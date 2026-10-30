#!/usr/bin/env bash
# Sobe o ambiente de desenvolvimento com um comando: MySQL (Docker), backend (compila e roda no
# perfil local, :8082) e frontend (Vite, :5173). Ctrl+C encerra tudo.
# PORT e FRONTEND_PORT trocam as portas (o login com Google sempre volta para :5173).
#
#   ./dev.sh              sobe tudo
#   ./dev.sh --gen-api    idem, e regenera src/api/schema.d.ts quando o backend estiver no ar
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
BACKEND_PORT="${PORT:-8082}"
FRONTEND_PORT="${FRONTEND_PORT:-5173}"
GEN_API=false
[[ "${1:-}" == "--gen-api" ]] && GEN_API=true

# Cada processo em segundo plano ganha o próprio grupo, para o Ctrl+C derrubar também o java
# que o Maven cria.
set -m
PIDS=()

log() { printf '\033[1;36m[dev]\033[0m %s\n' "$*"; }
fail() { printf '\033[1;31m[dev]\033[0m %s\n' "$*" >&2; exit 1; }

cleanup() {
  trap - EXIT INT TERM
  log "Encerrando..."
  for pid in ${PIDS[@]+"${PIDS[@]}"}; do kill -- "-$pid" 2>/dev/null || true; done
  wait 2>/dev/null || true
}
trap cleanup EXIT INT TERM

listening_pid() { lsof -tiTCP:"$1" -sTCP:LISTEN 2>/dev/null | head -1 || true; }

# Porta ocupada por uma instância antiga deste projeto (backend ou Vite esquecidos): oferece
# encerrar, junto com o processo pai (o Maven ou o npm). Qualquer outro processo: só avisa.
check_port_free() {
  local port=$1 name=$2 pid command parent parent_command answer
  pid="$(listening_pid "$port")"
  [[ -z "$pid" ]] && return
  command="$(ps -o command= -p "$pid")"
  if [[ "$command" == *"$ROOT"* && -t 0 ]]; then
    printf '\033[1;33m[dev]\033[0m A porta %s (%s) está com uma instância antiga deste projeto (processo %s):\n      %s\n' \
      "$port" "$name" "$pid" "$(cut -c1-120 <<<"$command")"
    read -r -p "      Encerrar e continuar? [s/N] " answer
    if [[ "$answer" == [sS]* ]]; then
      parent="$(ps -o ppid= -p "$pid" | tr -d ' ')"
      parent_command="$(ps -o command= -p "$parent" 2>/dev/null || true)"
      kill "$pid" 2>/dev/null || true
      if [[ "$parent_command" == *"$ROOT"* || "$parent_command" == *maven* || "$parent_command" == *"npm exec"* ]]; then
        kill "$parent" 2>/dev/null || true
      fi
      for _ in $(seq 1 15); do [[ -z "$(listening_pid "$port")" ]] && return; sleep 1; done
      fail "O processo $pid não liberou a porta $port."
    fi
  fi
  fail "A porta $port ($name) já está em uso pelo processo $pid: $(cut -c1-120 <<<"$command")
      Encerre com: kill $pid"
}

# Executa um comando com as linhas prefixadas (ex.: [api]) e guarda o PID do grupo.
run_prefixed() {
  local prefix=$1 color=$2; shift 2
  ( "$@" 2>&1 | sed -u "s/^/$(printf '\033[%sm[%s]\033[0m ' "$color" "$prefix")/" ) &
  PIDS+=($!)
}

# --- Pré-requisitos -----------------------------------------------------------------------------
command -v docker >/dev/null || fail "Docker não encontrado."
command -v java >/dev/null || fail "Java 21 não encontrado."
check_port_free "$BACKEND_PORT" "backend"
check_port_free "$FRONTEND_PORT" "frontend"

export NVM_DIR="${NVM_DIR:-$HOME/.nvm}"
if [[ -s "$NVM_DIR/nvm.sh" ]]; then
  # shellcheck disable=SC1091
  source "$NVM_DIR/nvm.sh"
  node_version="$(cat "$ROOT/frontend/.nvmrc")"
  nvm use "$node_version" >/dev/null 2>&1 || nvm install "$node_version" >/dev/null
fi
[[ "$(node -v 2>/dev/null)" == v22* ]] || fail "O frontend precisa do Node 22 (encontrado: $(node -v 2>/dev/null || echo nenhum))."

# --- MySQL --------------------------------------------------------------------------------------
log "Subindo o MySQL (docker compose)..."
(cd "$ROOT" && docker compose up -d --wait)

# --- Dependências do frontend ------------------------------------------------------------------
if [[ ! -d "$ROOT/frontend/node_modules" || "$ROOT/frontend/package-lock.json" -nt "$ROOT/frontend/node_modules/.package-lock.json" ]]; then
  log "Instalando dependências do frontend (npm install)..."
  (cd "$ROOT/frontend" && npm install)
fi

# --- Backend e frontend -------------------------------------------------------------------------
log "Compilando e subindo o backend em :$BACKEND_PORT..."
run_prefixed api "1;35" bash -c "cd '$ROOT/backend' && PORT=$BACKEND_PORT ./mvnw -q spring-boot:run -Dspring-boot.run.profiles=local"

log "Subindo o frontend em :$FRONTEND_PORT..."
run_prefixed web "1;32" bash -c "cd '$ROOT/frontend' && BACKEND_URL=http://localhost:$BACKEND_PORT npm run dev -- --port $FRONTEND_PORT --strictPort"

# Espera o backend ficar no ar (a primeira compilação pode demorar).
for _ in $(seq 1 120); do
  curl -sf "http://localhost:$BACKEND_PORT/actuator/health" >/dev/null && break
  for pid in ${PIDS[@]+"${PIDS[@]}"}; do kill -0 "$pid" 2>/dev/null || fail "Um dos processos parou; veja o log acima."; done
  sleep 2
done
curl -sf "http://localhost:$BACKEND_PORT/actuator/health" >/dev/null || fail "O backend não respondeu em 4 minutos."

if $GEN_API; then
  log "Regenerando o cliente da API..."
  (cd "$ROOT/frontend" && npm run gen:api)
fi

log "Pronto: http://localhost:$FRONTEND_PORT  (backend em http://localhost:$BACKEND_PORT). Ctrl+C encerra."
wait
