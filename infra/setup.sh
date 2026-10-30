#!/usr/bin/env bash
# Infraestrutura do Travel Planner no GCP (Fase 4 do plano). Idempotente: rodar de novo só cria o
# que falta e reaplica a configuração do Cloud Run, sem trocar a imagem em produção.
#
#   PROJECT_ID=meu-projeto REGION=europe-west6 SQL_INSTANCE=minha-instancia ./infra/setup.sh
#
# Variáveis:
#   PROJECT_ID, REGION, SQL_INSTANCE  obrigatórias (REGION = a mesma do Cloud SQL)
#   SQL_PROJECT_ID                    projeto do Cloud SQL, se for outro (padrão: PROJECT_ID). O
#                                     PROJECT_ID precisa existir e ter conta de faturamento.
#   GITHUB_REPO                       dono/repo que pode fazer deploy (padrão: o remoto `origin`)
#   GOOGLE_CLIENT_ID/_SECRET          cliente OAuth; se ausentes, lidos do .env da raiz
#   APP_ADMIN_EMAILS                  administradores (vírgulas); se ausente, lido do .env
#   BUDGET_AMOUNT                     orçamento mensal, na moeda da conta de faturamento (padrão: 1)
#
# Pré-requisitos: gcloud autenticado como dono do projeto; `gh` autenticado (opcional, para gravar
# as variáveis do repositório). Nenhum segredo é impresso.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
: "${PROJECT_ID:?defina PROJECT_ID}"
: "${REGION:?defina REGION (a mesma do Cloud SQL)}"
: "${SQL_INSTANCE:?defina SQL_INSTANCE}"
GITHUB_REPO="${GITHUB_REPO:-$(git -C "$ROOT" remote get-url origin | sed -E 's#(.*github\.com[:/])##; s#\.git$##')}"
SQL_PROJECT_ID="${SQL_PROJECT_ID:-$PROJECT_ID}"
BUDGET_AMOUNT="${BUDGET_AMOUNT:-1}"

SERVICE=travel-planner
REPOSITORY=travel-planner
DB_NAME=travel_planner
DB_USER=travel_planner
RUN_SA="travel-planner-run@$PROJECT_ID.iam.gserviceaccount.com"
DEPLOYER_SA="github-deployer@$PROJECT_ID.iam.gserviceaccount.com"
WIF_POOL=github
WIF_PROVIDER_ID=travel-planner
# Imagem provisória do primeiro deploy (só para obter a URL); o GitHub Actions publica a real.
PLACEHOLDER_IMAGE=us-docker.pkg.dev/cloudrun/container/hello

log() { printf '\033[1;36m[setup]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[setup]\033[0m %s\n' "$*"; }

# Contas de serviço recém-criadas demoram alguns segundos para aparecer no IAM.
retry() {
  local attempt
  for attempt in 1 2 3 4 5 6; do
    "$@" && return
    warn "Falhou (tentativa $attempt); tentando de novo em 10 s..."
    sleep 10
  done
  "$@"
}

# Só para este script: não muda o projeto padrão do gcloud.
export CLOUDSDK_CORE_PROJECT="$PROJECT_ID"
PROJECT_NUMBER="$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')"
CONNECTION_NAME="$SQL_PROJECT_ID:$REGION:$SQL_INSTANCE"
# Comandos do Cloud SQL rodam no projeto do banco.
sql() { gcloud sql "$@" --project="$SQL_PROJECT_ID"; }

# Lê uma variável do .env da raiz, se ela não veio do ambiente.
from_dotenv() {
  local name=$1
  [[ -n "${!name:-}" ]] && return
  [[ -f "$ROOT/.env" ]] || return 0
  local value
  value="$(grep -E "^$name=" "$ROOT/.env" | tail -1 | cut -d= -f2- || true)"
  value="${value%\"}"
  value="${value#\"}"
  printf -v "$name" '%s' "$value"
}
from_dotenv GOOGLE_CLIENT_ID
from_dotenv GOOGLE_CLIENT_SECRET
from_dotenv APP_ADMIN_EMAILS

# --- 1. APIs ------------------------------------------------------------------------------------
log "Habilitando APIs..."
gcloud services enable \
  run.googleapis.com sqladmin.googleapis.com artifactregistry.googleapis.com \
  secretmanager.googleapis.com iam.googleapis.com iamcredentials.googleapis.com \
  sts.googleapis.com cloudscheduler.googleapis.com places.googleapis.com \
  storage.googleapis.com billingbudgets.googleapis.com cloudresourcemanager.googleapis.com

# --- 2. Segredos (antes do banco: a senha do usuário vem daqui) ---------------------------------
secret_exists() { gcloud secrets describe "$1" >/dev/null 2>&1; }

# Cria o segredo, ou adiciona uma versão se o valor mudou. O valor vem do stdin.
put_secret() {
  local name=$1 value
  value="$(cat)"
  if ! secret_exists "$name"; then
    gcloud secrets create "$name" --replication-policy=automatic --labels=app=travel-planner >/dev/null
  elif [[ "$(gcloud secrets versions access latest --secret="$name" 2>/dev/null)" == "$value" ]]; then
    return
  fi
  printf '%s' "$value" | gcloud secrets versions add "$name" --data-file=- >/dev/null
  log "Segredo $name gravado."
}

log "Segredos no Secret Manager..."
if ! secret_exists db-password; then
  openssl rand -base64 32 | tr -d '/+=\n' | put_secret db-password
fi
if [[ -z "${GOOGLE_CLIENT_ID:-}" || -z "${GOOGLE_CLIENT_SECRET:-}" ]]; then
  if secret_exists google-client-id && secret_exists google-client-secret; then
    warn "GOOGLE_CLIENT_ID/SECRET não informados: mantendo os segredos existentes."
  else
    echo "Defina GOOGLE_CLIENT_ID e GOOGLE_CLIENT_SECRET (ambiente ou .env)." >&2
    exit 1
  fi
else
  printf '%s' "$GOOGLE_CLIENT_ID" | put_secret google-client-id
  printf '%s' "$GOOGLE_CLIENT_SECRET" | put_secret google-client-secret
fi
APP_SECRETS=(db-password google-client-id google-client-secret)

# --- 3. Banco -----------------------------------------------------------------------------------
log "Database $DB_NAME e usuário $DB_USER no Cloud SQL $SQL_INSTANCE..."
if ! sql databases describe "$DB_NAME" --instance="$SQL_INSTANCE" >/dev/null 2>&1; then
  sql databases create "$DB_NAME" --instance="$SQL_INSTANCE" \
    --charset=utf8mb4 --collation=utf8mb4_0900_ai_ci
fi
DB_PASSWORD="$(gcloud secrets versions access latest --secret=db-password)"
if sql users list --instance="$SQL_INSTANCE" --format='value(name)' | grep -qx "$DB_USER"; then
  # Mantém a senha do banco igual à do segredo.
  sql users set-password "$DB_USER" --host=% --instance="$SQL_INSTANCE" \
    --password="$DB_PASSWORD" >/dev/null
else
  sql users create "$DB_USER" --host=% --instance="$SQL_INSTANCE" --password="$DB_PASSWORD"
fi
unset DB_PASSWORD

# --- 4. Contas de serviço -----------------------------------------------------------------------
ensure_sa() {
  local email=$1 name=${1%%@*} display=$2
  gcloud iam service-accounts describe "$email" >/dev/null 2>&1 ||
    gcloud iam service-accounts create "$name" --display-name="$display"
}
log "Contas de serviço..."
ensure_sa "$RUN_SA" "Travel Planner (Cloud Run)"
ensure_sa "$DEPLOYER_SA" "Travel Planner (deploy pelo GitHub)"

retry gcloud projects add-iam-policy-binding "$SQL_PROJECT_ID" --condition=None \
  --member="serviceAccount:$RUN_SA" --role=roles/cloudsql.client >/dev/null
for secret in "${APP_SECRETS[@]}"; do
  retry gcloud secrets add-iam-policy-binding "$secret" \
    --member="serviceAccount:$RUN_SA" --role=roles/secretmanager.secretAccessor >/dev/null
done

# --- 5. Artifact Registry -----------------------------------------------------------------------
log "Artifact Registry ($REPOSITORY, mantém só as 2 imagens mais recentes)..."
gcloud artifacts repositories describe "$REPOSITORY" --location="$REGION" >/dev/null 2>&1 ||
  gcloud artifacts repositories create "$REPOSITORY" --repository-format=docker \
    --location="$REGION" --labels=app=travel-planner --description="Imagens do Travel Planner"
gcloud artifacts repositories set-cleanup-policies "$REPOSITORY" --location="$REGION" \
  --policy="$ROOT/infra/artifact-cleanup-policy.json" --no-dry-run >/dev/null

# --- 6. Cloud Run -------------------------------------------------------------------------------
# A configuração do serviço mora aqui; o deploy do GitHub só troca a imagem. Fases futuras
# acrescentam env vars e segredos nestas listas e rodam o script de novo.
ENV_VARS="SPRING_PROFILES_ACTIVE=prod;CLOUD_SQL_INSTANCE=$CONNECTION_NAME;DB_USERNAME=$DB_USER;APP_ADMIN_EMAILS=${APP_ADMIN_EMAILS:-}"
SECRETS="DB_PASSWORD=db-password:latest,GOOGLE_CLIENT_ID=google-client-id:latest,GOOGLE_CLIENT_SECRET=google-client-secret:latest"
RUN_FLAGS=(
  --region="$REGION"
  --service-account="$RUN_SA"
  --set-cloudsql-instances="$CONNECTION_NAME"
  --cpu=1 --memory=1Gi --cpu-boost
  --min-instances=0 --max-instances=2 --concurrency=40
  # ^;^ troca o separador, porque APP_ADMIN_EMAILS tem vírgulas.
  --set-env-vars="^;^$ENV_VARS"
  --set-secrets="$SECRETS"
  --labels=app=travel-planner
)
if gcloud run services describe "$SERVICE" --region="$REGION" >/dev/null 2>&1; then
  log "Atualizando a configuração do Cloud Run (a imagem continua a mesma)..."
  gcloud run services update "$SERVICE" "${RUN_FLAGS[@]}" --quiet
else
  log "Primeiro deploy do Cloud Run com a imagem provisória (para obter a URL)..."
  gcloud run deploy "$SERVICE" --image="$PLACEHOLDER_IMAGE" --port=8080 "${RUN_FLAGS[@]}" --quiet
fi
gcloud run services add-iam-policy-binding "$SERVICE" --region="$REGION" \
  --member=allUsers --role=roles/run.invoker >/dev/null
SERVICE_URL="$(gcloud run services describe "$SERVICE" --region="$REGION" --format='value(status.url)')"

# --- 7. Orçamento -------------------------------------------------------------------------------
# Projeto só do app: o orçamento cobre tudo nele. Projeto compartilhado com o Cloud SQL (já pago)
# ou outros apps: só os serviços do app. IDs do catálogo do Cloud Billing: Cloud Run, Artifact
# Registry, Secret Manager, Places API (New), Places API, Cloud Scheduler.
BUDGET_FILTER=(--filter-projects="projects/$PROJECT_ID")
if [[ "$SQL_PROJECT_ID" == "$PROJECT_ID" ]]; then
  BUDGET_FILTER+=(--filter-services=services/152E-C115-5142,services/149C-F9EC-3994,services/EE82-7A5E-871C,services/0776-5E2E-55E3,services/213C-9623-1402,services/1F14-4801-0E16)
fi
BILLING_ACCOUNT="$(gcloud billing projects describe "$PROJECT_ID" --format='value(billingAccountName)')"
BILLING_ACCOUNT="${BILLING_ACCOUNT#billingAccounts/}"
# O orçamento tem de estar na moeda da conta (ex.: EUR), senão a API responde INVALID_ARGUMENT.
CURRENCY="$(gcloud billing accounts describe "$BILLING_ACCOUNT" --format='value(currencyCode)')"
log "Orçamento de $BUDGET_AMOUNT $CURRENCY/mês (alertas em 50/90/100%)..."
if gcloud billing budgets list --billing-account="$BILLING_ACCOUNT" --format='value(displayName)' |
  grep -qx travel-planner; then
  log "Orçamento travel-planner já existe."
else
  gcloud billing budgets create --billing-account="$BILLING_ACCOUNT" \
    --display-name=travel-planner --budget-amount="$BUDGET_AMOUNT$CURRENCY" \
    "${BUDGET_FILTER[@]}" \
    --threshold-rule=percent=0.5 --threshold-rule=percent=0.9 --threshold-rule=percent=1.0
fi

# --- 8. Deploy pelo GitHub (Workload Identity Federation, sem chave) ----------------------------
log "Workload Identity Federation para $GITHUB_REPO (só a branch main)..."
gcloud iam workload-identity-pools describe "$WIF_POOL" --location=global >/dev/null 2>&1 ||
  gcloud iam workload-identity-pools create "$WIF_POOL" --location=global \
    --display-name="GitHub Actions"
gcloud iam workload-identity-pools providers describe "$WIF_PROVIDER_ID" --location=global \
  --workload-identity-pool="$WIF_POOL" >/dev/null 2>&1 ||
  gcloud iam workload-identity-pools providers create-oidc "$WIF_PROVIDER_ID" --location=global \
    --workload-identity-pool="$WIF_POOL" --display-name="Travel Planner" \
    --issuer-uri=https://token.actions.githubusercontent.com \
    --attribute-mapping=google.subject=assertion.sub,attribute.repository=assertion.repository,attribute.ref=assertion.ref \
    --attribute-condition="assertion.repository == '$GITHUB_REPO' && assertion.ref == 'refs/heads/main'"
WIF_PROVIDER="projects/$PROJECT_NUMBER/locations/global/workloadIdentityPools/$WIF_POOL/providers/$WIF_PROVIDER_ID"

retry gcloud iam service-accounts add-iam-policy-binding "$DEPLOYER_SA" --role=roles/iam.workloadIdentityUser \
  --member="principalSet://iam.googleapis.com/projects/$PROJECT_NUMBER/locations/global/workloadIdentityPools/$WIF_POOL/attribute.repository/$GITHUB_REPO" \
  >/dev/null
# Publicar revisões (sem mexer em IAM), enviar imagens só para este repositório e rodar o serviço
# com a conta do Cloud Run.
retry gcloud projects add-iam-policy-binding "$PROJECT_ID" --condition=None \
  --member="serviceAccount:$DEPLOYER_SA" --role=roles/run.developer >/dev/null
retry gcloud artifacts repositories add-iam-policy-binding "$REPOSITORY" --location="$REGION" \
  --member="serviceAccount:$DEPLOYER_SA" --role=roles/artifactregistry.writer >/dev/null
retry gcloud iam service-accounts add-iam-policy-binding "$RUN_SA" \
  --member="serviceAccount:$DEPLOYER_SA" --role=roles/iam.serviceAccountUser >/dev/null

if command -v gh >/dev/null && gh auth status >/dev/null 2>&1; then
  log "Variáveis do repositório no GitHub..."
  gh variable set GCP_PROJECT_ID --repo "$GITHUB_REPO" --body "$PROJECT_ID"
  gh variable set GCP_REGION --repo "$GITHUB_REPO" --body "$REGION"
  gh variable set WIF_PROVIDER --repo "$GITHUB_REPO" --body "$WIF_PROVIDER"
  gh variable set DEPLOYER_SA --repo "$GITHUB_REPO" --body "$DEPLOYER_SA"
else
  warn "gh indisponível: crie as variáveis do repositório à mão (Settings → Secrets and variables → Actions → Variables):"
  printf '  GCP_PROJECT_ID=%s\n  GCP_REGION=%s\n  WIF_PROVIDER=%s\n  DEPLOYER_SA=%s\n' \
    "$PROJECT_ID" "$REGION" "$WIF_PROVIDER" "$DEPLOYER_SA"
fi

# --- Passos manuais -----------------------------------------------------------------------------
cat <<EOF

$(log "Pronto. Serviço: $SERVICE_URL")

Passos manuais (uma vez):

1. Permissões do usuário do banco. O Cloud SQL cria usuários com o papel cloudsqlsuperuser; para
   restringir ao database $DB_NAME, rode como root no Cloud SQL Studio (Console → SQL →
   $SQL_INSTANCE no projeto $SQL_PROJECT_ID → Cloud SQL Studio):

     REVOKE IF EXISTS cloudsqlsuperuser FROM '$DB_USER'@'%';
     SET DEFAULT ROLE NONE TO '$DB_USER'@'%';
     GRANT ALL PRIVILEGES ON $DB_NAME.* TO '$DB_USER'@'%';

2. No cliente OAuth (Console → APIs e serviços → Credenciais), adicione o redirect:
     $SERVICE_URL/login/oauth2/code/google

3. Faça push na main (ou rode Actions → Deploy) para publicar a imagem real.
EOF
