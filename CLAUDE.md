# Travel Planner — instruções para o Claude

Sistema web mobile-first para planejar viagens em família/grupo: o usuário monta a viagem e o sistema **aponta lacunas** (transfer, documentos, malas) por regras determinísticas, com IA só para enriquecer.
Monorepo: backend Spring Boot (Java 21) + frontend React/TypeScript/Vite, deploy em Cloud Run com Cloud SQL MySQL.
Leia antes de qualquer fase: [`docs/especificacao.md`](docs/especificacao.md) (ESP) e [`docs/plano-implementacao.md`](docs/plano-implementacao.md) (plano).

## Comandos

| O quê | Comando | Disponível |
|---|---|---|
| Subir MySQL local | `docker compose up -d` (porta **3307** no host; `MYSQL_PORT` sobrescreve) | já |
| Conectar no MySQL | `docker compose exec mysql mysql -utravel -ptravel travel_planner` | já |
| Parar MySQL | `docker compose down` (`-v` apaga os dados) | já |
| Rodar backend | `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local` | a partir da Fase 1 |
| Testes backend | `cd backend && ./mvnw verify` | a partir da Fase 1 |
| Formatar código Java | `cd backend && ./mvnw fmt:format` (google-java-format; o build falha se o código não estiver formatado) | a partir da Fase 1 |
| Rodar frontend | `cd frontend && npm run dev` | a partir da Fase 2 |
| Testes frontend | `cd frontend && npm run test -- --run && npm run lint && npm run build` | a partir da Fase 2 |
| Gerar cliente da API | `cd frontend && npm run gen:api` (backend rodando localmente) | a partir da Fase 2 |

O frontend usa **Node 22** (`frontend/.nvmrc`; rode `nvm use` em `frontend/`) e sobe em `http://localhost:5173`, com proxy de `/api`, `/oauth2`, `/login`, `/logout` e `/dev` para o backend em `:8082`. A página `/dev/ui` (só em dev) mostra os componentes e as cores nos dois temas.

O MySQL local usa a porta **3307** porque a 3306 da máquina de desenvolvimento já está ocupada por outro MySQL. Pelo mesmo motivo, o backend no perfil `local` sobe na porta **8082** (8080/8081 ocupadas; `PORT` sobrescreve): `http://localhost:8082/actuator/health`. Datasource local: `jdbc:mysql://localhost:3307/travel_planner`, usuário/senha `travel`/`travel`.

## Convenções (resumo)

Detalhes na seção **"Convenções gerais"** do plano — ela prevalece sobre este resumo.

- **Estrutura:** `backend/`, `frontend/`, `infra/`, `docs/`, `Dockerfile`, `docker-compose.yml`, `.github/workflows/`.
- **Backend:** pacotes por domínio (`auth`, `trip`, `itinerary`, …, `shared`) com `api`/`domain`/`infra`; REST em `/api/v1`, DTOs `record`, entidades JPA nunca saem do controller; Lombok para boilerplate (`@Getter`, `@Slf4j`, `@RequiredArgsConstructor`); código no google-java-format; erros como `ProblemDetail`; `@Version` para concorrência otimista (409); `Clock` injetado; migrations Flyway nunca editadas depois de commitadas; segredos só por variável de ambiente.
- **Frontend:** React 19 + TS estrito, Tailwind v4 + shadcn/ui, React Router, TanStack Query (hooks por domínio em `features/<módulo>/api.ts`), `openapi-fetch` com tipos gerados (não editar `src/api/schema.d.ts`), `react-hook-form` + `zod`, textos só em `src/lib/i18n/pt-BR.json`, mobile-first (360 px).
- **Testes:** JUnit 5 + AssertJ, Testcontainers MySQL, WireMock para APIs externas; Vitest + Testing Library + MSW no frontend.
- **Git:** branch `fase-NN-<slug>`, commit/PR `Fase N: <título>`.

## Regras de trabalho

- Implemente **uma fase por vez**, seguindo as tarefas e o aceite descritos no plano.
- Ao concluir uma fase, **atualize a tabela de progresso do plano** (marcar ☑) e registre desvios em "Notas de implementação".
- Se uma decisão do plano se mostrar errada, atualize o plano e a ESP no mesmo PR, em vez de divergir em silêncio.
- Nunca commite segredos nem arquivos `.env` (só `.env.example`).
