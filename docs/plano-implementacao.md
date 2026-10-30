# Travel Planner — Plano de implementação

> Plano executável, em fases pequenas, derivado de [`especificacao.md`](./especificacao.md) (abreviada aqui como **ESP**). Cada fase foi pensada para caber em **uma sessão de trabalho / um PR**, terminar com algo testável e não depender de fases futuras.

---

## Como usar este plano

1. Em uma sessão nova, peça algo como:
   > Leia `CLAUDE.md`, `docs/especificacao.md` e `docs/plano-implementacao.md`. Implemente a **Fase N**. Siga as convenções da seção "Convenções gerais" e os critérios de aceite da fase.
2. Ao terminar uma fase: rodar os testes, marcar o checkbox na tabela abaixo, commitar (`Fase N: <título>`) e registrar no fim da fase qualquer desvio do plano em **"Notas de implementação"**.
3. Se uma decisão do plano se mostrar errada durante a implementação, **atualize este arquivo e a ESP** no mesmo PR, em vez de divergir em silêncio.
4. Cada fase pode virar uma issue no GitHub Project (compatível com a skill `/pick-task`): título = `Fase N: <título>`, corpo = o conteúdo da fase.

### Progresso

| Fase | Título | Bloco | Status |
|---|---|---|---|
| 0 | Repositório, ferramentas e `CLAUDE.md` | Fundação | ☑ |
| 1 | Esqueleto do backend | Fundação | ☑ |
| 2 | Esqueleto do frontend | Fundação | ☑ |
| 3 | Autenticação Google | Fundação | ☑ |
| 4 | Infraestrutura GCP e deploy contínuo | Fundação | ☐ |
| 5 | Viagens (CRUD) e autorização | MVP | ☐ |
| 6 | Viajantes (S1) | MVP | ☐ |
| 7 | Cliente Places + paradas com autocomplete de cidade | MVP | ☐ |
| 8 | Transporte | MVP | ☐ |
| 9 | Hospedagem (cadastro manual) | MVP | ☐ |
| 10 | Timeline dia a dia (leitura) (S2) | MVP | ☐ |
| 11 | Partilha: convites, papéis e link | MVP | ☐ |
| 12 | Checklists manuais | MVP | ☐ |
| 13 | Templates de checklist (S4) | MVP | ☐ |
| 14 | Motor de lacunas: framework + regras de logística (S3) | MVP | ☐ |
| 15 | Dashboard | MVP | ☐ |
| 16 | Busca de hotéis com faixa de preço | Descoberta | ☐ |
| 17 | Restaurantes, atrações e favoritos | Descoberta | ☐ |
| 18 | Atividades na timeline + drag-and-drop | Descoberta | ☐ |
| 19 | Base de requisitos de documentos | Descoberta | ☐ |
| 20 | Lembretes por e-mail + job diário (S5) | Lembretes | ☐ |
| 21 | Web Push (S5) | Lembretes | ☐ |
| 22 | Anexos (S7) | Lembretes | ☐ |
| 23 | Integração de IA (`LlmClient` + Gemini) | Inteligência | ☐ |
| 24 | Malas: regra + clima + IA | Inteligência | ☐ |
| 25 | IA: ranking de restaurantes/atrações e estimativa de preço | Inteligência | ☐ |
| 26 | IA: rascunho de requisitos de documentos | Inteligência | ☐ |
| 27 | PWA offline: leitura | Offline | ☐ |
| 28 | PWA offline: escrita e sincronização | Offline | ☐ |
| 29 | Endurecimento: segurança, privacidade, observabilidade | Produção | ☐ |
| 30 | Cold start: medição e imagem nativa (opcional) | Produção | ☐ |

As fases 0–15 formam o **MVP** (ESP §10, Fases 0 e 1).

---

## Convenções gerais

Valem para todas as fases. Na dúvida, siga o que já existe no código.

### Estrutura do repositório (monorepo)

```
/
├─ backend/                 Spring Boot (Maven, wrapper ./mvnw)
│  ├─ pom.xml
│  └─ src/
│     ├─ main/java/com/travelplanner/<módulo>/...
│     ├─ main/resources/
│     │  ├─ application.yml, application-local.yml, application-prod.yml
│     │  └─ db/migration/   (Flyway: V<n>__<descricao>.sql)
│     └─ test/java/...
├─ frontend/                React + TypeScript + Vite
│  ├─ src/
│  │  ├─ app/               (router, providers, layout)
│  │  ├─ features/<módulo>/ (páginas, componentes, hooks de API por domínio)
│  │  ├─ components/ui/     (componentes base, shadcn/ui)
│  │  ├─ lib/               (api client, i18n, utilidades de data)
│  │  └─ api/schema.d.ts    (gerado do OpenAPI; não editar à mão)
│  └─ package.json
├─ infra/                   scripts gcloud e arquivos de política (ver Fase 4)
├─ docs/
├─ Dockerfile               multi-stage: node → maven → JRE
├─ docker-compose.yml       MySQL 8 local
├─ .github/workflows/       ci.yml, deploy.yml
└─ CLAUDE.md
```

### Backend

- **Java 21**, **Spring Boot** na última versão estável (4.x). No Spring Boot 4 a auto-configuração é modularizada: use os starters específicos (ex.: `spring-boot-starter-flyway`) e confira a documentação ao adicionar dependências. Se alguma biblioteca necessária não for compatível, use 3.5.x e registre a decisão.
- **Lombok** para eliminar boilerplate (getters, loggers via `@Slf4j`, construtores via `@RequiredArgsConstructor`). DTOs continuam sendo `record`; evitar `@Data`/`@EqualsAndHashCode` em entidades JPA.
- **Formatação:** google-java-format via `fmt-maven-plugin` (`com.spotify.fmt`). O goal `check` roda no build e falha se houver código fora do padrão; corrigir com `./mvnw fmt:format`.
- **Pacotes por domínio** (ESP §7.3): `auth`, `trip`, `itinerary`, `checklist`, `suggestion`, `packing`, `places`, `requirements`, `ai`, `attachment`, `notification`, `weather`, `shared`. Dentro de cada um: `api` (controllers + DTOs), `domain` (entidades, serviços, regras), `infra` (repositórios, clientes externos), quando o tamanho justificar.
- **API REST** em `/api/v1/...`. JSON em camelCase. DTOs como `record`. Entidades JPA **nunca** saem do controller.
- **Validação** com Jakarta Validation nos DTOs de entrada (`@Valid`).
- **Erros** no formato `ProblemDetail` (RFC 9457) via `@RestControllerAdvice` em `shared`. Códigos: 400 validação, 401 não autenticado, 403 sem permissão, 404 não encontrado (também quando o usuário não tem acesso à viagem, para não revelar que ela existe), 409 conflito de versão.
- **Concorrência otimista:** entidades editáveis têm `@Version long version`. O DTO de atualização carrega `version`; divergência → 409.
- **IDs:** `BIGINT AUTO_INCREMENT`. A autorização é sempre verificada (ver Fase 5), então expor IDs numéricos é aceitável.
- **Datas:** `LocalDate` para dias; para horários de transporte, `LocalDateTime` + `ZoneId` (coluna `VARCHAR(64)`) por ponta. Hora atual sempre via um bean `Clock` injetado (testável).
- **Auditoria:** todas as tabelas têm `created_at` e `updated_at` (`DATETIME(6)`, UTC), preenchidos por `@CreatedDate`/`@LastModifiedDate` ou `@PrePersist`/`@PreUpdate`.
- **Migrations Flyway:** nunca editar uma migration já commitada; criar uma nova. Charset `utf8mb4`, collation `utf8mb4_0900_ai_ci`, engine InnoDB, FKs explícitas com `ON DELETE CASCADE` quando o filho não faz sentido sem o pai. Os números `V<n>` sugeridos nas fases são indicativos: use o próximo disponível.
- **Configuração** via `application.yml` com propriedades tipadas (`@ConfigurationProperties`, prefixo `app.`). Segredos **só** por variável de ambiente (em produção vêm do Secret Manager, ver Fase 4).
- **Clientes HTTP externos** com `RestClient`, timeouts explícitos (connect 3 s, read 10 s) e uma interface por integração (ex.: `PlacesClient`), para permitir fakes em teste.
- **OpenAPI** com `springdoc-openapi` (versão compatível com o Spring Boot). Disponível em `/v3/api-docs` só nos perfis `local` e `test`.

### Frontend

- **Node 22 LTS**, **React 19**, **TypeScript** estrito, **Vite**.
- **UI:** Tailwind CSS v4 (`@tailwindcss/vite`) + **shadcn/ui** (componentes copiados para `components/ui`) + ícones `lucide-react`. **Mobile-first**: projetar para 360 px e expandir com breakpoints `md:`/`lg:`.
- **Roteamento:** React Router (modo data/`createBrowserRouter`).
- **Dados do servidor:** TanStack Query v5. Um arquivo de hooks por domínio (ex.: `features/trips/api.ts` com `useTrips`, `useCreateTrip`).
- **Cliente de API tipado:** `openapi-typescript` gera `src/api/schema.d.ts` a partir de `/v3/api-docs`; `openapi-fetch` faz as chamadas. Script `npm run gen:api` (backend rodando localmente).
- **Formulários:** `react-hook-form` + `zod`.
- **Datas:** `date-fns` + `@date-fns/tz`. Exibir horários de transporte **no fuso local do evento**, com o fuso indicado.
- **i18n:** `react-i18next`, só `pt-BR` por enquanto (`src/lib/i18n/pt-BR.json`). Nenhum texto de interface fixo no JSX.
- **CSRF:** o cliente lê o cookie `XSRF-TOKEN` e envia o header `X-XSRF-TOKEN` em métodos não-GET (configurado uma vez no `openapi-fetch` middleware).
- **401:** um middleware global redireciona para `/login`.

### Testes

- **Backend:**
  - Unitários (JUnit 5 + AssertJ) para regras e serviços puros.
  - Integração com `@SpringBootTest` + **Testcontainers MySQL 8** (container compartilhado entre testes via `@ServiceConnection`).
  - **WireMock** para APIs externas (Places, Open-Meteo, Gemini).
  - Testes de controller com MockMvc e usuário autenticado simulado (`oidcLogin()` do `spring-security-test`).
- **Frontend:** Vitest + Testing Library + **MSW** para mockar a API. Testar componentes com lógica (formulários, listas com estado), não markup trivial.
- **E2E (opcional, a partir da Fase 15):** Playwright usando o login de desenvolvimento (Fase 3).
- Cada fase só está pronta com testes passando: `./mvnw verify` e `npm run test -- --run && npm run lint && npm run build`.

### Git

- Branch por fase: `fase-NN-<slug>`. Commit/PR com título `Fase N: <título>`.
- CI (Fase 4) precisa estar verde antes do merge.

### Definição de pronto (vale para toda fase)

- Critérios de aceite da fase atendidos.
- Testes novos cobrindo o comportamento principal; suíte inteira verde.
- Textos de interface no arquivo de i18n.
- Telas utilizáveis em 360 px de largura.
- Sem segredos commitados.
- Checkbox marcado na tabela de progresso; desvios anotados em "Notas de implementação".

---

# Bloco Fundação

## Fase 0 — Repositório, ferramentas e `CLAUDE.md`

**Objetivo:** estrutura do monorepo e instruções para sessões futuras.

**Tarefas**
1. Criar a estrutura de pastas da seção "Convenções gerais" (pastas vazias com `.gitkeep` onde necessário).
2. `.gitignore` cobrindo Java/Maven (`target/`), Node (`node_modules/`, `dist/`), IDEs, `.env*` (exceto `.env.example`).
3. `.editorconfig` (UTF-8, LF, 4 espaços Java, 2 espaços TS/JSON/YAML).
4. `docker-compose.yml` com MySQL 8.0:
   - banco `travel_planner`, usuário `travel`/`travel`, porta `3307` no host (`${MYSQL_PORT:-3307}:3306`; a 3306 local já está ocupada), volume nomeado;
   - `command: --character-set-server=utf8mb4 --collation-server=utf8mb4_0900_ai_ci`.
5. `.env.example` listando todas as variáveis de ambiente previstas (preencher ao longo das fases): `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `PLACES_API_KEY`, `GEMINI_API_KEY`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `VAPID_PUBLIC_KEY`, `VAPID_PRIVATE_KEY`, `GCS_BUCKET`, `APP_ADMIN_EMAILS`.
6. `README.md` curto: o que é, como rodar localmente (aponta para `CLAUDE.md`).
7. `CLAUDE.md` com:
   - Visão em 3 linhas e links para `docs/especificacao.md` e `docs/plano-implementacao.md`.
   - Comandos: subir MySQL (`docker compose up -d`), backend (`cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`), frontend (`cd frontend && npm run dev`), testes, gerar cliente da API.
   - Resumo das convenções (apontando para a seção do plano).
   - Regra: "ao concluir uma fase, atualizar a tabela de progresso do plano".
   - Os comandos que ainda não existirem são marcados "(a partir da Fase N)".

**Aceite:** `docker compose up -d` sobe o MySQL e aceita conexão; `CLAUDE.md` e `README.md` existem.

---

## Fase 1 — Esqueleto do backend

**Objetivo:** aplicação Spring Boot que sobe, conecta no MySQL, roda migrations e tem a base de erros/testes.

**Referências:** ESP §7.2, §7.3.

**Tarefas**
1. Gerar projeto (Spring Initializr ou à mão) em `backend/`: Maven, Java 21, `groupId com.travelplanner`, `artifactId travel-planner`.
   Dependências: web, validation, data-jpa, flyway (+ `flyway-mysql`), `mysql-connector-j`, actuator, security (já incluída, liberando tudo por enquanto), `springdoc-openapi`, `spring-boot-starter-test`, Testcontainers (`mysql`, `junit-jupiter`), `spring-boot-testcontainers`.
2. `application.yml`:
   - `server.port: ${PORT:8080}` (Cloud Run injeta `PORT`);
   - `server.forward-headers-strategy: framework` (Cloud Run fica atrás de proxy HTTPS; necessário para os redirects do OAuth);
   - `spring.jpa.open-in-view: false`, `spring.jpa.hibernate.ddl-auto: validate`;
   - Hikari `maximum-pool-size: 5`;
   - Actuator expondo só `health`.
3. `application-local.yml`: `server.port: ${PORT:8082}` (8080/8081 ocupadas na máquina de desenvolvimento); datasource `jdbc:mysql://localhost:${MYSQL_PORT:3307}/travel_planner`, usuário `travel`.
4. `shared/web/GlobalExceptionHandler` (`@RestControllerAdvice`) produzindo `ProblemDetail` para: `MethodArgumentNotValidException` (400, com lista de campos), `NotFoundException` própria (404), `ForbiddenException` (403), `OptimisticLockingFailureException` (409), genérico (500 sem stack trace).
5. `shared/time/ClockConfig`: bean `Clock.systemUTC()`.
6. `shared/persistence/BaseEntity` (`@MappedSuperclass`) com `id`, `createdAt`, `updatedAt`, `version`.
7. Migration `V1__baseline.sql` vazia (só comentário), para validar o Flyway.
8. `SecurityConfig` provisório: tudo liberado (`permitAll`), CSRF desligado. **Será substituído na Fase 3.**
9. Testes:
   - `ApplicationIT` (`@SpringBootTest` + Testcontainers) que sobe o contexto e confere `/actuator/health` = UP.
   - Classe base `IntegrationTest` com o container compartilhado, para as próximas fases reutilizarem.
   - Teste do `GlobalExceptionHandler` com um controller de teste.

**Aceite:** `./mvnw verify` verde; `./mvnw spring-boot:run -Dspring-boot.run.profiles=local` sobe e `GET /actuator/health` retorna `UP`; tabela `flyway_schema_history` criada.

---

## Fase 2 — Esqueleto do frontend

**Objetivo:** SPA React com layout mobile, roteamento, i18n e cliente de API, rodando contra o backend local.

**Referências:** ESP §6.7 (dashboard), §11 (mobile-first).

**Tarefas**
1. `npm create vite@latest frontend -- --template react-ts`. TypeScript `strict: true`. ESLint + Prettier.
2. Instalar e configurar: Tailwind v4, shadcn/ui (`button`, `card`, `input`, `label`, `dialog`, `sheet`, `dropdown-menu`, `badge`, `checkbox`, `tabs`, `sonner` para toasts), `lucide-react`, React Router, TanStack Query (+ devtools em dev), `react-hook-form`, `zod`, `@hookform/resolvers`, `date-fns`, `@date-fns/tz`, `react-i18next`, `i18next`, `openapi-fetch`, `openapi-typescript` (dev).
3. `vite.config.ts`: proxy de `/api`, `/oauth2`, `/login`, `/logout`, `/dev` para `http://localhost:8082`.
4. Layout (`app/AppLayout.tsx`):
   - **Celular:** barra superior com título e menu do usuário; **barra de navegação inferior** com "Início", "Viagens", "Perfil".
   - **Desktop (`lg:`):** navegação lateral.
   - Área de conteúdo com `max-w-3xl` centralizada.
5. Rotas iniciais: `/login`, `/` (dashboard, placeholder), `/trips` (placeholder), `/profile` (placeholder), `*` (404).
6. `lib/api.ts`: instância `openapi-fetch` com middlewares de CSRF e de 401 (convenções). Script `gen:api` em `package.json`.
7. `lib/i18n`: inicialização com `pt-BR.json`.
8. Tema conforme **ESP §13 (Identidade visual)**:
   - variáveis CSS dos temas claro e escuro em `src/index.css` (`:root` e `.dark`), incluindo os tokens próprios `--highlight`, `--warning`, `--info`, `--success` (e as variantes `-soft`/`-foreground`), registrados no `@theme` do Tailwind para gerar classes como `bg-highlight` e `text-warning`;
   - fontes `@fontsource-variable/inter` e `@fontsource-variable/plus-jakarta-sans` (auto-hospedadas);
   - `--radius: 0.875rem`;
   - modo escuro: seguir o sistema, com override manual (claro/escuro/sistema) guardado em `localStorage` e aplicado como classe `dark` no `<html>`;
   - página de desenvolvimento `/dev/ui` (só em dev) mostrando botões, cards, badges de severidade, `PriceTier` e as cores, nos dois temas, para conferir o visual.
9. Integração com o backend para produção:
   - `npm run build` gera `frontend/dist`;
   - backend: `shared/web/SpaForwardController` que encaminha para `index.html` qualquer GET que não seja `/api/**`, `/oauth2/**`, `/login/**` (sub-caminhos: a página `/login` em si é da SPA), `/actuator/**` nem arquivo com extensão.
10. Testes: Vitest + Testing Library + MSW configurados; um teste do layout renderizando a navegação.

**Aceite:** `npm run dev` mostra o layout responsivo (conferir em 360 px e em desktop); navegação entre rotas funciona; `npm run test -- --run`, `lint` e `build` verdes.

---

## Fase 3 — Autenticação Google

**Objetivo:** login com Google, sessão persistida no MySQL, usuário provisionado, CSRF para SPA e um login de desenvolvimento.

**Referências:** ESP §6.1, §11.

**Pré-requisito manual (documentar no `CLAUDE.md`):** no Console GCP → APIs e serviços:
- configurar a **tela de consentimento OAuth** (tipo externo, escopos `openid email profile`, seu e-mail como usuário de teste ou app publicado);
- criar **ID do cliente OAuth (Aplicativo da Web)** com redirect `http://localhost:8082/login/oauth2/code/google` (o de produção é adicionado na Fase 4).

Em dev, o frontend roda em `:5173`, mas o fluxo OAuth passa pelo backend em `:8082` via proxy. Configurar o redirect pós-login para `http://localhost:5173/` no perfil `local`.

**Migrations**
- `V2__app_user.sql` — tabela `app_user`:
  - `id`, `google_sub VARCHAR(255) UNIQUE NOT NULL`, `email VARCHAR(320) NOT NULL` (índice);
  - `name`, `picture_url VARCHAR(1024)`, `locale VARCHAR(16) DEFAULT 'pt-BR'`;
  - `nationality CHAR(2) DEFAULT 'BR'`, `home_city_name`, `home_city_place_id`, `home_country_code CHAR(2)`, `home_timezone` (preenchidos no perfil; usados pelas regras de transfer e de viagem internacional);
  - auditoria, `version`.
- `V3__spring_session.sql` — copiar o `schema-mysql.sql` do `spring-session-jdbc` (tabelas `SPRING_SESSION` e `SPRING_SESSION_ATTRIBUTES`). Configurar `spring.session.jdbc.initialize-schema: never`.

**Backend**
1. Dependências: `spring-boot-starter-oauth2-client`, `spring-session-jdbc`, `spring-security-test` (teste).
2. `application.yml`: registro `google` com `client-id`/`client-secret` de env; escopos `openid,email,profile`. Cookie de sessão `HttpOnly`, `SameSite=Lax`, `Secure` em prod; timeout de sessão de 30 dias.
3. `auth/AppUserService.upsertFromOidc(OidcUser)`: cria ou atualiza `app_user` por `google_sub` (atualiza nome, foto, e-mail).
4. `auth/AppUserPrincipal`: implementação de `OidcUser` que carrega também `userId`. Registrada via `OidcUserService` customizado que chama o upsert. Controllers recebem `@AuthenticationPrincipal AppUserPrincipal`.
5. `SecurityConfig` definitivo:
   - `/api/**` exige autenticação; não autenticado → **401 JSON** (`HttpStatusEntryPoint`), sem redirect.
   - `/actuator/health`, `/oauth2/**`, `/login/**`, assets estáticos e rotas da SPA: públicos.
   - `oauth2Login` com `defaultSuccessUrl` configurável (`app.auth.post-login-redirect`).
   - `logout` em `POST /logout` retornando 204.
   - **CSRF para SPA:** cookie `XSRF-TOKEN` legível pelo JS + header `X-XSRF-TOKEN`. Usar a configuração para SPA da versão do Spring Security em uso (`csrf.spa()` nas versões recentes; senão `CookieCsrfTokenRepository.withHttpOnlyFalse()` + o `SpaCsrfTokenRequestHandler` da documentação oficial).
   - Cabeçalhos de segurança padrão + `Referrer-Policy: strict-origin-when-cross-origin`.
6. Endpoints `auth/api/MeController`:
   - `GET /api/v1/me` → `{id, email, name, pictureUrl, nationality, homeCity:{name, placeId, countryCode, timezone}, isAdmin}`.
   - `PATCH /api/v1/me` → atualiza `nationality` e cidade de origem (o autocomplete de cidade chega na Fase 7; até lá, campos de texto).
7. **Admin:** `app.auth.admin-emails` (lista, de `APP_ADMIN_EMAILS`); `isAdmin` = e-mail na lista. Usado na Fase 19.
8. **Login de desenvolvimento** (`auth/dev/DevLoginController`, `@Profile("local | test")`):
   - `POST /dev/login?email=...` cria/usa o usuário e autentica a sessão sem Google.
   - Guarda de segurança: um `ApplicationListener` que **falha na inicialização** se o bean existir com o perfil `prod` ativo.
   - Serve para testes manuais, E2E e sessões de IA sem acesso ao Google.

**Frontend**
1. Página `/login`: logo/nome, botão "Entrar com Google" (link para `/oauth2/authorization/google`); em dev, campo de e-mail + botão "Login dev".
2. `useMe()` (TanStack Query). Rotas protegidas com um loader/guard: sem usuário → `/login`.
3. Menu do usuário com foto, nome e "Sair" (`POST /logout` → limpa o cache → `/login`).
4. Página `/profile`: nacionalidade (select, padrão Brasil) e cidade de origem (texto por enquanto).

**Testes**
- Integração: `GET /api/v1/me` sem sessão → 401; com `oidcLogin()` → 200 e usuário criado no banco.
- `POST` sem CSRF → 403; com CSRF → OK.
- Upsert: segundo login com nome diferente atualiza o registro, sem duplicar.
- Teste que garante que o `DevLoginController` não existe no perfil `prod`.

**Aceite:** login real com Google funciona localmente; a sessão sobrevive a um restart do backend (porque está no MySQL); logout funciona; o login dev funciona no perfil `local`.

---

## Fase 4 — Infraestrutura GCP e deploy contínuo

**Objetivo:** a aplicação rodando no Cloud Run com o Cloud SQL existente, com deploy automático a partir da `main` e proteções de custo ativas. Fazer cedo garante que surpresas de infraestrutura apareçam logo e permite medir o cold start.

**Referências:** ESP §7.4, §8.

**Arquivos**
- `Dockerfile` (raiz), multi-stage:
  1. `node:22-alpine`: `npm ci && npm run build` em `frontend/`.
  2. `maven:3.9-eclipse-temurin-21`: copia `backend/`, copia `frontend/dist` para `backend/src/main/resources/static`, `./mvnw -B -DskipTests package`.
  3. `eclipse-temurin:21-jre`: só o JAR. `ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:TieredStopAtLevel=1"` (JVM leve para 1 vCPU). Usuário não root. `ENTRYPOINT ["java","-jar","/app/app.jar"]`.
  - `.dockerignore` excluindo `node_modules`, `target`, `.git`, `docs`.
- `infra/setup.sh`: script **idempotente e comentado** com os comandos `gcloud` abaixo, parametrizado por `PROJECT_ID`, `REGION` (a mesma do Cloud SQL) e `SQL_INSTANCE`. Rodar manualmente, uma vez.
- `infra/artifact-cleanup-policy.json`: manter só as 2 imagens mais recentes.
- `application-prod.yml`.
- `.github/workflows/ci.yml` e `.github/workflows/deploy.yml`.

**Passos de infraestrutura (`infra/setup.sh`)**
1. Habilitar APIs: `run`, `sqladmin`, `artifactregistry`, `secretmanager`, `iamcredentials`, `cloudscheduler`, `places` (Places API New), `storage`.
2. **Banco:** no Cloud SQL existente, criar o database `travel_planner` (utf8mb4) e o usuário `travel_planner` com senha forte e permissões só nesse database.
3. **Conta de serviço** `travel-planner-run@...` com:
   - `roles/cloudsql.client`;
   - `roles/secretmanager.secretAccessor` (só nos segredos do app);
   - permissões no bucket (Fase 22) adicionadas depois.
4. **Artifact Registry:** repositório Docker `travel-planner` na região, com a política de limpeza.
5. **Secret Manager:** segredos `db-password`, `google-client-id`, `google-client-secret`. Os demais (`places-api-key`, `gemini-api-key`, SMTP, VAPID) são criados nas fases correspondentes.
6. **Cloud Run** (primeiro deploy manual, para obter a URL):
   - `--service-account travel-planner-run@...`, `--add-cloudsql-instances PROJECT:REGION:INSTANCE`;
   - `--cpu 1 --memory 1Gi --min-instances 0 --max-instances 2 --cpu-boost --concurrency 40 --allow-unauthenticated`;
   - `--set-secrets` mapeando cada segredo para uma env var;
   - `--set-env-vars SPRING_PROFILES_ACTIVE=prod,...`.
7. Adicionar `https://<url-cloud-run>/login/oauth2/code/google` aos redirects do cliente OAuth.
8. **Orçamento:** budget de US$ 1 com alertas em 50/90/100% (`gcloud billing budgets create`).
9. **Deploy pelo GitHub sem chave:** Workload Identity Federation (pool + provider OIDC do GitHub restrito ao repositório) e conta de serviço `github-deployer@...` com `roles/run.developer` (basta para publicar revisões; ver notas), `roles/artifactregistry.writer` e `roles/iam.serviceAccountUser` sobre a conta do Cloud Run.

**`application-prod.yml`**
- Datasource via Cloud SQL Java Connector: dependência `com.google.cloud.sql:mysql-socket-factory-connector-j-8`, URL `jdbc:mysql:///travel_planner?cloudSqlInstance=${CLOUD_SQL_INSTANCE}&socketFactory=com.google.cloud.sql.mysql.SocketFactory`.
- Hikari `maximum-pool-size: 3`, `minimum-idle: 1`.
- `spring.main.lazy-initialization: true` (reduz o cold start; validar que nada quebra).
- Cookies `Secure`; `app.auth.post-login-redirect: /`.
- Logs em JSON estruturado (Cloud Logging entende): o Boot 4.1 não tem formato `gcp`, então `logging.structured.format.console` aponta para um formatter próprio (`shared/logging/GcpLogFormatter`).

**GitHub Actions**
- `ci.yml` (em PR e push):
  - job `backend`: Temurin 21, cache Maven, `./mvnw -B verify` (o runner do GitHub tem Docker para o Testcontainers);
  - job `frontend`: Node 22, `npm ci`, `lint`, `test -- --run`, `build`.
- `deploy.yml` (push na `main`, depois do CI):
  1. `google-github-actions/auth` (WIF);
  2. `docker build` e push para `REGION-docker.pkg.dev/PROJECT/travel-planner/app:${{ github.sha }}`;
  3. `gcloud run deploy --image ...`, mantendo flags e segredos.
- Variáveis do repositório (não secretas): `GCP_PROJECT_ID`, `GCP_REGION`, `WIF_PROVIDER`, `DEPLOYER_SA`.

**Medição:** depois do deploy, registrar em "Notas de implementação" o **tempo de cold start** (primeira requisição após 15+ minutos parado, medido com `curl -w '%{time_total}'`) e o tempo de inicialização no log do Spring. Esses números alimentam a Fase 30.

**Aceite:** push na `main` faz deploy automático; login Google funciona na URL do Cloud Run; o Flyway roda no banco de produção; budget criado; nenhum segredo no repositório ou nos logs.

---

# Bloco MVP

## Fase 5 — Viagens (CRUD) e autorização

**Objetivo:** criar, listar, editar e excluir viagens, com o mecanismo de autorização por viagem usado por todas as fases seguintes.

**Referências:** ESP §6.2, §9.

**Migration `V4__trip.sql`**
- `trip`:
  - `id`, `name VARCHAR(120) NOT NULL`;
  - `start_date DATE NULL`, `end_date DATE NULL` (manuais por enquanto; na Fase 8 passam a ser derivadas quando houver trechos);
  - `status ENUM('PLANNING','CONFIRMED','IN_PROGRESS','DONE') NOT NULL DEFAULT 'PLANNING'`;
  - `cover_color VARCHAR(16)` (a capa por imagem fica fora do escopo por agora);
  - `owner_id FK app_user`, auditoria, `version`.
- `trip_member`:
  - `id`, `trip_id FK CASCADE`, `user_id FK NULL`, `invite_email VARCHAR(320) NULL`;
  - `role ENUM('OWNER','EDITOR','VIEWER')`, `accepted_at DATETIME NULL`, auditoria;
  - `UNIQUE(trip_id, user_id)`, `UNIQUE(trip_id, invite_email)`.

**Backend**
1. Entidades `Trip`, `TripMember`; repositórios.
2. **`trip/domain/TripAccessService`** (bean `tripAccess`):
   - `requireRole(tripId, userId, minRole)`, com hierarquia `VIEWER < EDITOR < OWNER`;
   - lança `NotFoundException` se o usuário não for membro e `ForbiddenException` se for membro com papel insuficiente;
   - todos os serviços de recursos filhos (paradas, transporte, etc.) chamam isso **no serviço**, não só no controller.
3. Status `IN_PROGRESS`/`DONE`: calculado na leitura a partir das datas e do `Clock` (não persistir mudanças automáticas). O usuário só define `PLANNING`/`CONFIRMED`.
4. Endpoints:

   | Método | Rota | Papel mínimo | Notas |
   |---|---|---|---|
   | GET | `/api/v1/trips` | membro | Query `?scope=upcoming\|past\|all`. Retorna resumo: id, nome, datas, status efetivo, papel do usuário, contagem de destinos. |
   | POST | `/api/v1/trips` | autenticado | Cria a viagem e o `trip_member` OWNER. |
   | GET | `/api/v1/trips/{id}` | VIEWER | Detalhe (por enquanto só a viagem; cresce nas próximas fases). |
   | PUT | `/api/v1/trips/{id}` | EDITOR | Com `version`. |
   | DELETE | `/api/v1/trips/{id}` | OWNER | Remove em cascata. |

5. Evento de domínio **`TripChangedEvent(tripId)`**, publicado por qualquer serviço que altere dados da viagem. Ainda sem ouvintes; o motor de lacunas (Fase 14) vai escutar com `@TransactionalEventListener(phase = AFTER_COMMIT)`.

**Frontend**
1. `/trips`: lista em cards (nome, datas, status como badge, papel), abas "Próximas" / "Passadas", botão flutuante "+ Nova viagem".
2. `/trips/new` e edição: formulário (nome, datas opcionais com validação fim ≥ início, status).
3. `/trips/:tripId`: **página da viagem com abas**, que serão preenchidas nas próximas fases: "Roteiro" (paradas/transporte/hospedagem), "Timeline", "Checklist", "Mala", "Sugestões", "Anexos", "Pessoas". Por enquanto só um cabeçalho com nome, datas e menu (editar, excluir com confirmação).

**Testes**
- `TripAccessService`: matriz papel × operação.
- Usuário B não vê nem edita a viagem do usuário A (404).
- Conflito de versão → 409.
- Exclusão remove os membros.

**Aceite:** CRUD completo no celular; acesso de outro usuário bloqueado; teste de autorização cobrindo a matriz.

---

## Fase 6 — Viajantes (S1)

**Objetivo:** cadastro de viajantes reutilizáveis e associação às viagens.

**Referências:** ESP §6.2 (Viajantes), §9 (`TRAVELER`, `TRIP_TRAVELER`).

**Migration `V5__traveler.sql`**
- `traveler`:
  - `id`, `owner_user_id FK app_user`, `linked_user_id FK app_user NULL`, `name VARCHAR(120)`;
  - `age_group ENUM('BABY','CHILD','TEEN','ADULT')`, `birth_year SMALLINT NULL` (opcional, refina a faixa);
  - `nationalities VARCHAR(64) NOT NULL DEFAULT 'BR'` (códigos ISO separados por vírgula);
  - `passport_expiry DATE NULL`, auditoria, `version`;
  - **sem número de passaporte**.
- `trip_traveler`: `trip_id FK CASCADE`, `traveler_id FK CASCADE`, PK composta.

**Backend**
- Ao criar o usuário (Fase 3), criar automaticamente um `traveler` "eu" com `linked_user_id` = o próprio usuário. Fazer o mesmo, via migration de dados, para usuários já existentes.
- Endpoints:
  - `GET/POST /api/v1/travelers`, `PUT/DELETE /api/v1/travelers/{id}`: só os do `owner_user_id`.
  - `GET /api/v1/trips/{tripId}/travelers` (VIEWER).
  - `PUT /api/v1/trips/{tripId}/travelers` (EDITOR): substitui a lista de IDs. Só aceita viajantes do próprio usuário ou já presentes na viagem, porque um editor convidado pode trazer os seus.
- Ao criar uma viagem, adicionar o viajante "eu" do criador por padrão.
- Publicar `TripChangedEvent` ao alterar viajantes da viagem.

**Frontend**
- `/profile/travelers`: lista e formulário (nome, faixa etária, nacionalidades, validade do passaporte, com aviso visual se vencer em menos de 6 meses).
- Aba "Pessoas" da viagem: seção "Quem vai", com seleção múltipla dos viajantes e atalho "novo viajante".

**Testes:** não é possível associar viajante de outro usuário; o viajante "eu" é criado no primeiro login.

**Aceite:** família cadastrada uma vez e reutilizada em viagens diferentes.

---

## Fase 7 — Cliente Places + paradas com autocomplete de cidade

**Objetivo:** primeira integração com o Google Places (autocomplete de cidades) e o roteiro de paradas da viagem.

**Referências:** ESP §3.1 (termos de uso), §6.2 (Destinos), §8 (Places).

**Pré-requisito manual:** criar a chave de API no GCP **restrita à "Places API (New)"**, guardar no Secret Manager como `places-api-key` e mapear no Cloud Run como `PLACES_API_KEY`. Configurar **quotas diárias** no console (APIs → Places API (New) → Cotas): começar com ~100/dia para Autocomplete e ~50/dia para Place Details.

**Backend: módulo `places`**
1. `PlacesClient` (interface) + `GooglePlacesClient` (`RestClient`, base `https://places.googleapis.com/v1`, headers `X-Goog-Api-Key` e **sempre** `X-Goog-FieldMask`):
   - `autocompleteCities(input, sessionToken, languageCode)` → `POST /places:autocomplete` com `includedPrimaryTypes: ["locality","administrative_area_level_3"]`. Retorna `placeId` + texto.
   - `getCity(placeId, sessionToken)` → `GET /places/{id}` com field mask **só** `id,displayName,formattedAddress,location,addressComponents` (SKU barato). O `sessionToken` encerra a sessão de autocomplete.
2. Fuso horário a partir de lat/lng **sem custo**: biblioteca `net.iakovlev:timeshape` (dados offline de fusos). Encapsular em `places/TimeZoneResolver`. Ela carrega dados em memória (~dezenas de MB): inicializar de forma lazy e conferir o consumo de memória no Cloud Run.
3. País: extrair `countryCode` (ISO-2) dos `addressComponents` (tipo `country`, `shortText`).
4. Cache Caffeine (`spring-boot-starter-cache`): autocomplete por 10 min (chave = input normalizado); detalhes da cidade por 24 h (chave = placeId).
5. Endpoints (autenticado):
   - `GET /api/v1/places/cities/autocomplete?q=...&session=...` (mínimo 3 caracteres; o frontend faz debounce de 300 ms).
   - `GET /api/v1/places/cities/{placeId}?session=...` → `{placeId, name, countryCode, lat, lng, timezone}`.
6. Tratamento de erro: falha ou quota esgotada no Places → 503 com `ProblemDetail` "busca de lugares indisponível". O frontend permite **digitar a cidade manualmente** (nome + país em select), com fuso escolhido de uma lista.

**Migration `V6__stop.sql`**
- `stop`:
  - `id`, `trip_id FK CASCADE`, `seq INT NOT NULL`;
  - `city_place_id VARCHAR(255) NULL`, `city_name VARCHAR(160) NOT NULL`, `country_code CHAR(2) NOT NULL`;
  - `lat DECIMAL(9,6) NULL`, `lng DECIMAL(9,6) NULL`, `timezone VARCHAR(64) NOT NULL`;
  - `arrival_date DATE NOT NULL`, `departure_date DATE NOT NULL`, `notes TEXT`, auditoria, `version`;
  - `UNIQUE(trip_id, seq)`.
  - Guardar nome, país, coordenadas e fuso de uma cidade é aceitável: são dados do roteiro do usuário, não conteúdo do Places exibido como catálogo. Se houver dúvida nos termos, manter só `place_id` + o que o usuário confirmou.

**Backend: paradas**
- `GET/POST /api/v1/trips/{tripId}/stops`, `PUT/DELETE .../stops/{stopId}`, `PUT .../stops/order` (lista de IDs).
- Validação: `departure_date ≥ arrival_date`; paradas não podem se sobrepor por mais de 1 dia (o dia de troca de cidade é compartilhado).
- Se a viagem não tiver datas manuais, `start_date`/`end_date` passam a ser o mínimo/máximo das paradas (recalcular ao salvar).
- `TripChangedEvent` em toda alteração.
- O perfil (Fase 3) passa a usar o autocomplete para a cidade de origem, preenchendo `home_*`.

**Frontend**
- Componente reutilizável `CityAutocomplete`: gera um `sessionToken` (UUID) por interação e tem fallback manual.
- Aba "Roteiro": lista ordenada de paradas (cidade, país com bandeira emoji, datas, nº de noites), adicionar/editar em `Sheet` (painel inferior no celular), reordenar com botões ↑/↓ (drag-and-drop fica para a Fase 18).
- Perfil: cidade de origem com o autocomplete.

**Testes**
- WireMock para o Places: confere que o `X-Goog-FieldMask` é sempre enviado, e o mapeamento de país e fuso.
- Fallback manual quando o Places retorna 429.
- Cálculo das datas da viagem a partir das paradas.

**Aceite:** criar uma viagem "Lisboa 10–14/03 → Porto 14–17/03" pelo celular, com fuso correto em cada parada.

---

## Fase 8 — Transporte

**Objetivo:** cadastrar os trechos de ida, volta, entre cidades e locais, ligados às paradas.

**Referências:** ESP §6.2 (Transporte), §6.4 (regras que dependem disso).

**Migration `V7__transport_segment.sql`**
- `transport_segment`:
  - `id`, `trip_id FK CASCADE`;
  - `type ENUM('FLIGHT','BUS','TRAIN','CAR','CAR_RENTAL','FERRY','TRANSFER','TAXI_RIDE','OTHER')`;
  - `origin_stop_id FK stop NULL`: **NULL = cidade de origem do usuário (casa)**;
  - `destination_stop_id FK stop NULL`: **NULL = casa**;
  - `local_stop_id FK stop NULL`: preenchido para trechos **locais** dentro de uma parada (transfer aeroporto→hotel, aluguel de carro); nesse caso origin/destination ficam NULL;
  - `origin_label VARCHAR(160)`, `destination_label VARCHAR(160)` (ex.: "GRU", "Aeroporto de Punta Cana", "Hotel X");
  - `depart_at DATETIME NOT NULL`, `depart_tz VARCHAR(64) NOT NULL`, `arrive_at DATETIME NULL`, `arrive_tz VARCHAR(64) NULL`;
  - `carrier VARCHAR(120)`, `code VARCHAR(40)` (nº do voo), `booking_ref VARCHAR(80)`, `notes TEXT`, auditoria, `version`.
- Os fusos são sugeridos a partir das paradas (casa = `home_timezone` do usuário) e podem ser editados.

**Backend**
- CRUD em `/api/v1/trips/{tripId}/segments` (VIEWER lê, EDITOR escreve).
- Validações:
  - um trecho é **de deslocamento** (origin/destination) **ou local** (`local_stop_id`), nunca os dois;
  - `arrive_at` em instante posterior a `depart_at` (comparar como `ZonedDateTime`).
- DTO de saída inclui os instantes em ISO-8601 com offset, além dos campos locais.
- `TripChangedEvent`.
- Datas derivadas da viagem: considerar também o primeiro `depart_at` e o último `arrive_at`.

**Frontend**
- Na aba "Roteiro", intercalar os trechos entre as paradas: casa → ✈ → Lisboa → 🚆 → Porto → ✈ → casa.
- Formulário com tipo (ícones), seleção de origem/destino entre "Casa" e as paradas, ou "trecho local em <parada>". Data/hora com o fuso exibido.
- Horários mostrados no fuso de cada ponta (ex.: "10/03 22:40 (GMT-3) → 11/03 11:15 (GMT+0)").

**Testes:** validação de fusos (voo que "chega antes" no relógio local mas depois em UTC deve ser aceito); trecho local vs. deslocamento.

**Aceite:** roteiro completo de ida/volta com voo noturno exibido corretamente.

---

## Fase 9 — Hospedagem (cadastro manual)

**Objetivo:** cadastrar hospedagens por parada. A busca no Places chega na Fase 16; aqui é texto livre.

**Referências:** ESP §6.2 (Hospedagem).

**Migration `V8__lodging.sql`**
- `lodging`:
  - `id`, `stop_id FK CASCADE`, `place_id VARCHAR(255) NULL`, `name VARCHAR(160) NOT NULL`, `address VARCHAR(300)`;
  - `check_in DATE NOT NULL`, `check_out DATE NOT NULL`;
  - `board_type ENUM('ROOM_ONLY','BREAKFAST','HALF_BOARD','FULL_BOARD','ALL_INCLUSIVE') NULL`;
  - `price_tier TINYINT NULL` (1–4) e `price_tier_source ENUM('PLACES','HEURISTIC','AI','USER') NULL`;
  - `booking_ref VARCHAR(80)`, `notes TEXT`, auditoria, `version`.

**Backend**
- CRUD em `/api/v1/trips/{tripId}/stops/{stopId}/lodgings`.
- Validação: `check_out > check_in`, dentro (ou no limite) das datas da parada.
- `TripChangedEvent`.

**Frontend**
- Na aba "Roteiro", dentro de cada parada: hospedagens (nome, datas, regime, `$`–`$$$$` se houver).
- Formulário com o seletor de faixa de preço (`$`, `$$`, `$$$`, `$$$$`, "não sei").
- Componente reutilizável `PriceTier` (exibe e edita).

**Aceite:** parada com uma ou mais hospedagens (ex.: dois hotéis na mesma cidade).

---

## Fase 10 — Timeline dia a dia (leitura) (S2)

**Objetivo:** visão por dia gerada a partir de paradas, trechos e hospedagens.

**Referências:** ESP §6.3.

**Backend: módulo `itinerary`**
1. `ItineraryService.build(tripId)` → lista de dias de `start_date` a `end_date`. Cada dia contém:
   - `date`, `stops` (paradas em que se está nesse dia, até 2 em dia de troca);
   - `events` ordenados por horário local: partidas/chegadas de trechos, check-in/check-out de hospedagem (atividades entram na Fase 18);
   - `overnight`: onde se dorme (hospedagem, ou "em trânsito" se houver trecho cruzando a noite, ou `null` = lacuna).
2. Lógica pura, sem efeitos colaterais. Recebe um **`TripSnapshot`** (record imutável com viagem, paradas, trechos, hospedagens, viajantes, usuário dono). Criar `TripSnapshotLoader` no módulo `trip`: o **mesmo snapshot será usado pelo motor de lacunas** (Fase 14).
3. `GET /api/v1/trips/{tripId}/itinerary` (VIEWER).

**Frontend**
- Aba "Timeline": lista vertical de dias (cabeçalho "Seg, 10/03 · Lisboa"), eventos com ícone e horário local, linha "Dormir: Hotel X" ou **destaque de lacuna** ("sem hospedagem") em cor de alerta.
- Durante a viagem, rolar automaticamente até "hoje".

**Testes:** unitários do `ItineraryService` com snapshots montados em código: voo noturno ("em trânsito"), dia de troca de cidade, noite sem hospedagem.

**Aceite:** a timeline reflete o roteiro das fases 7–9 e mostra noites descobertas.

---

## Fase 11 — Partilha: convites, papéis e link

**Objetivo:** convidar pessoas por e-mail ou link, com papéis.

**Referências:** ESP §6.2 (Partilhar).

**Migration `V9__trip_share_link.sql`**
- `trip_share_link`: `id`, `trip_id FK CASCADE`, `token CHAR(43) UNIQUE` (32 bytes aleatórios em base64url), `role ENUM('EDITOR','VIEWER')`, `created_by FK`, `revoked_at DATETIME NULL`, auditoria.

**Backend**
1. Convite por e-mail:
   - `POST /api/v1/trips/{tripId}/members` `{email, role}` (OWNER) cria um `trip_member` com `invite_email` e `user_id NULL`. Se o e-mail já pertencer a um usuário, vincula direto.
   - **Ao fazer login** (upsert da Fase 3), vincular convites pendentes com o mesmo e-mail (`user_id` + `accepted_at`).
   - O envio do e-mail de convite chega na Fase 20; até lá o dono avisa a pessoa por fora.
2. Gestão:
   - `GET .../members` (VIEWER);
   - `PUT .../members/{memberId}` (OWNER, muda papel; não pode rebaixar o último OWNER);
   - `DELETE .../members/{memberId}` (OWNER; ou o próprio membro, para sair da viagem).
3. Link:
   - `POST .../share-links` (OWNER) `{role}` → token;
   - `DELETE .../share-links/{id}` (revoga);
   - `POST /api/v1/share-links/{token}/join` (autenticado): adiciona o usuário como membro com o papel do link (se já for membro com papel maior, mantém).
   - **O link exige login**, para não haver dados expostos publicamente.
4. A listagem `GET /api/v1/trips` passa a incluir as viagens em que o usuário é membro.

**Frontend**
- Aba "Pessoas": membros (foto, nome/e-mail, papel, "pendente"), convidar por e-mail, criar/copiar/revogar link (usar `navigator.share` no celular, se disponível).
- Rota `/join/:token`: se não estiver logado, vai para o login e volta; depois chama o `join` e redireciona para a viagem.
- Esconder ações de edição para VIEWER (o backend é a fonte de verdade).

**Testes:** convite pendente vinculado no login; VIEWER não edita; último OWNER não pode ser removido; link revogado → 404.

**Aceite:** um segundo usuário (usar login dev com outro e-mail) entra por convite e por link, com os papéis respeitados.

---

## Fase 12 — Checklists manuais

**Objetivo:** itens de checklist por viagem.

**Referências:** ESP §6.5.

**Migration `V10__checklist_item.sql`**
- `checklist_item`:
  - `id`, `trip_id FK CASCADE`;
  - `category ENUM('DOCUMENTS','BOOKINGS','PACKING','HOME','HEALTH','MONEY','OTHER')`;
  - `title VARCHAR(200)`, `notes TEXT`, `due_date DATE NULL`;
  - `status ENUM('OPEN','DONE') DEFAULT 'OPEN'`, `done_at DATETIME NULL`, `done_by FK app_user NULL`;
  - `assignee_user_id FK app_user NULL`;
  - `source ENUM('MANUAL','RULE','AI','TEMPLATE')`;
  - `rule_code VARCHAR(64) NULL`, `rule_key VARCHAR(160) NULL` (para itens criados por regra, Fase 14), `UNIQUE(trip_id, rule_code, rule_key)`;
  - `sort_order INT`, auditoria, `version`.

**Backend**
- CRUD em `/api/v1/trips/{tripId}/checklist`, mais:
  - `PATCH .../checklist/{id}/status` `{status}`: operação rápida e idempotente; importante para o offline (Fase 28);
  - `PUT .../checklist/order`.
- Filtros: `?category=&status=&assignee=me`.

**Frontend**
- Aba "Checklist": agrupada por categoria, checkbox grande (toque fácil), data limite com cor (vencido/em breve), responsável (avatar), progresso no topo (`7/12`).
- Adição rápida: input fixo no rodapé, Enter adiciona na categoria selecionada.
- Filtro "só meus" e "esconder concluídos".

**Aceite:** checklist usável com uma mão no celular; progresso atualiza na hora (atualização otimista no TanStack Query).

---

## Fase 13 — Templates de checklist (S4)

**Objetivo:** templates do sistema e do usuário, aplicáveis a viagens.

**Referências:** ESP §6.5.

**Migration `V11__checklist_template.sql`**
- `checklist_template`: `id`, `owner_user_id FK NULL` (NULL = template do sistema), `name VARCHAR(120)`, `description`, `icon VARCHAR(32)`, auditoria.
- `template_item`:
  - `id`, `template_id FK CASCADE`, `category` (mesmo enum), `title`;
  - `due_offset_days INT NULL` (relativo à partida; ex.: −7);
  - `sort_order`.
- `V12__seed_system_templates.sql` com templates do sistema, cada um com 8–20 itens realistas em pt-BR:
  - **Praia**, **Neve**, **Cidade**, **Internacional**, **Com bebê**, **Com pet**;
  - **Antes de sair de casa**: desligar gás, regar plantas/pedir a alguém, tirar o lixo, avisar o banco/liberar o cartão para uso internacional, ativar roaming/comprar eSIM, deixar a chave com alguém, programar luzes etc.
  - Exemplo, Internacional: "Conferir validade do passaporte" (−60), "Contratar seguro viagem" (−14), "Comprar moeda/cartão internacional" (−7), "Avisar banco" (−3).

**Backend**
- `GET /api/v1/checklist-templates` (do sistema + do usuário).
- CRUD dos templates do usuário: `POST/PUT/DELETE /api/v1/checklist-templates/{id}`.
- `POST /api/v1/trips/{tripId}/checklist/apply-template` `{templateId}` (EDITOR):
  - cria itens com `source=TEMPLATE`;
  - `due_date = data de partida + due_offset_days` (sem data de partida → sem `due_date`);
  - **não duplica** itens com o mesmo título e categoria já presentes.
- `POST /api/v1/trips/{tripId}/checklist/save-as-template` `{name}`: cria um template do usuário a partir dos itens atuais, convertendo `due_date` em offset.

**Frontend**
- Na criação da viagem (passo opcional) e na aba "Checklist" ("Aplicar template"): grade de templates com ícone e nº de itens, prévia, aplicar.
- `/profile/templates`: gerenciar templates próprios.

**Testes:** offset de datas; deduplicação; template do sistema não editável pelo usuário.

**Aceite:** criar uma viagem internacional de praia e aplicar 2 templates sem duplicar itens.

---

## Fase 14 — Motor de lacunas: framework + regras de logística (S3)

**Objetivo:** o coração do produto. Um framework de regras testáveis e as regras que dependem só dos dados já existentes.

**Referências:** ESP §6.4 (tabela de regras), §1 (princípio 2).

**Migration `V13__suggestion.sql`**
- `suggestion`:
  - `id`, `trip_id FK CASCADE`, `rule_code VARCHAR(64)`, `logical_key VARCHAR(160)`;
  - `severity ENUM('INFO','WARNING','CRITICAL')`;
  - `status ENUM('PENDING','ACCEPTED','SNOOZED','DISMISSED')`, `snoozed_until DATE NULL`;
  - `payload_json JSON` (parâmetros para montar a mensagem e as ações), auditoria;
  - `UNIQUE(trip_id, rule_code, logical_key)`.

**Backend: módulo `suggestion`**
1. Contrato:
   ```java
   public interface GapRule {
       String code();
       List<SuggestionDraft> evaluate(TripSnapshot snapshot, Clock clock);
   }
   record SuggestionDraft(String logicalKey, Severity severity, Map<String,Object> params,
                          Optional<ChecklistDraft> autoChecklist) {}
   ```
   Regras são beans Spring (`@Component`), descobertas automaticamente; puras (sem acesso a banco).
   - Se uma regra precisar de dados externos (requisitos de documentos, Fase 19), o dado entra no `TripSnapshot` ou num contexto passado junto.
2. `SuggestionEngine.run(tripId)` faz a **reconciliação**:
   - carrega o snapshot;
   - executa todas as regras;
   - cria os drafts novos como `PENDING`;
   - drafts existentes em `ACCEPTED`/`DISMISSED` mantêm o estado; `SNOOZED` volta a `PENDING` se `snoozed_until` passou;
   - sugestões `PENDING`/`SNOOZED` que **não foram mais produzidas** são apagadas (a lacuna foi resolvida);
   - drafts com `autoChecklist` criam/atualizam o `checklist_item` correspondente (`source=RULE`, `rule_code`/`rule_key`), idempotente.
3. Disparo: `@TransactionalEventListener(AFTER_COMMIT)` em `TripChangedEvent`, executado de forma síncrona numa nova transação (o volume é pequeno). Debounce não é necessário.
4. Mensagens: o backend devolve `ruleCode` + `params`; **o texto é montado no frontend via i18n** (`suggestions.TRANSFER_CHEGADA.title`, com interpolação). Assim o backend não depende de idioma.
5. Endpoints:
   - `GET /api/v1/trips/{tripId}/suggestions?status=PENDING`;
   - `PATCH .../suggestions/{id}` `{status, snoozedUntil?}` (EDITOR). Aceitar uma sugestão com checklist sugerido cria o item.
6. **Regras desta fase** (cada uma em uma classe, com teste unitário próprio). "Trecho de chão" = qualquer tipo ≠ `FLIGHT`.

   | Código | Condição exata | logicalKey | Severidade | Checklist automático |
   |---|---|---|---|---|
   | `TRANSFER_CHEGADA` | Para cada `FLIGHT` cujo destino é uma parada: não existe trecho de chão com `local_stop_id` = essa parada, ou que saia do mesmo destino, com `depart_at` entre a chegada do voo e +6 h. | `segmentId` | WARNING | não |
   | `TRANSFER_SAIDA` | Para cada `FLIGHT`: não existe trecho de chão chegando ao aeroporto de origem com `arrive_at` entre −8 h e a partida (para origem = casa, considerar trechos locais sem parada, ou aceitar o "dispensar"). | `segmentId` | WARNING | não |
   | `NOITE_SEM_HOSPEDAGEM` | Para cada noite `d` em `[start, end)`: nenhuma `lodging` com `check_in ≤ d < check_out` **e** nenhum trecho em andamento entre `d 23:59` e `d+1 05:00` (hora local). Ignorar noites em que se está em casa (antes da ida/depois da volta). | `yyyy-MM-dd` | WARNING | não |
   | `DESTINO_SEM_TRANSPORTE` | Para cada par consecutivo (casa→parada 1, parada i→i+1, última→casa): não existe trecho com esse `origin_stop_id`/`destination_stop_id`. | `"home-<id>"`, `"<id>-<id>"`, `"<id>-home"` | WARNING | não |
   | `TRANSPORTE_LOCAL` | Parada com ≥ 2 noites sem trecho local (`local_stop_id`) do tipo `CAR_RENTAL`. Sugere decidir como se locomover; dispensar resolve. | `stopId` | INFO | não |
   | `CHECKIN_ONLINE` | `FLIGHT` com partida nos próximos 3 dias (pelo `Clock`). | `segmentId` | INFO | sim: "Check-in online <voo>", `due_date` = dia da partida − 1 |
   | `PASSAPORTE_VALIDADE` | Viagem internacional (algum `country_code` ≠ país de casa) e, para cada viajante: `passport_expiry` nulo → INFO "informe a validade"; `< end_date + 6 meses` → CRITICAL. Pular se **todos** os países aceitarem RG brasileiro (lista fixa por enquanto: AR, UY, PY, CL, BO, PE, CO, EC, VE; será substituída pela base na Fase 19). | `travelerId` | INFO / CRITICAL | sim (CRITICAL): "Renovar passaporte de <nome>" |
   | `SEGURO_VIAGEM` | Viagem internacional sem item de checklist aberto ou concluído com `rule_code = SEGURO_VIAGEM`. Severidade WARNING se algum país for Schengen (lista fixa por enquanto), senão INFO. | `trip` | INFO / WARNING | ao aceitar |

   - As regras de restaurantes, atrações, resort e documentos entram nas Fases 17 e 19.
   - Usar "agora" sempre do `Clock`. Para comparar horários de fusos diferentes, converter para `Instant`.

**Frontend**
- Aba "Sugestões": cards por severidade (CRITICAL vermelho, WARNING âmbar, INFO azul), com título e descrição montados via i18n e ações:
  - "Resolver": link contextual, ex. abrir o formulário de trecho já preenchido como transfer local da parada;
  - "Adiar" (escolher data);
  - "Dispensar".
  - Exemplos de links externos: Uber (`https://m.uber.com/`), busca de transfer no Booking (`https://www.booking.com/taxi/`).
- Badge com o número de sugestões pendentes na aba e no card da viagem.
- Na timeline, os dias com lacuna linkam para a sugestão.

**Testes**
- Um arquivo de teste por regra, com snapshots construídos por um **builder de teste** (`TripSnapshotFixtures`); cobrir casos positivos, negativos e fusos.
- Teste de integração da reconciliação: criar lacuna → PENDING; resolver (adicionar hospedagem) → sugestão some; dispensar → não volta; snooze vencido → volta.

**Aceite:** com o roteiro de exemplo, o sistema aponta exatamente as lacunas esperadas e elas somem quando o dado é cadastrado.

---

## Fase 15 — Dashboard

**Objetivo:** tela inicial com o que importa agora.

**Referências:** ESP §6.7.

**Backend**
- `GET /api/v1/dashboard`:
  - `upcomingTrips`: id, nome, datas, dias até a partida, % do checklist concluído, nº de sugestões pendentes por severidade;
  - `alerts`: top 10 entre itens de checklist abertos com `due_date` nos próximos 14 dias ou vencidos, e sugestões CRITICAL, ordenados por (vencido, severidade, data);
  - `pastTrips`: últimas 5.
- Consultas agregadas eficientes (evitar N+1: uma query por tipo de dado, agrupando por `trip_id`).

**Frontend**
- `/`:
  - Bloco "Alertas" no topo (cada um linka para a viagem/aba);
  - Cards de próximas viagens com contagem regressiva ("em 12 dias"), barra de progresso e badges;
  - "Viagens passadas" recolhível;
  - Estado vazio convidativo ("Planeje sua primeira viagem").

**Aceite:** o dashboard reflete as mudanças feitas nas viagens; testado com 0, 1 e várias viagens. **Fim do MVP:** fazer deploy e usar numa viagem real antes de seguir.

---

# Bloco Descoberta

## Fase 16 — Busca de hotéis com faixa de preço

**Objetivo:** buscar hotéis/resorts de uma parada no Places, com filtros e faixa `$`–`$$$$`, e transformar um resultado em hospedagem.

**Referências:** ESP §3.1, §8 (SKUs e quotas).

**Backend: `places`**
1. `searchLodging(stop, filters, pageToken)` → `POST /places:searchText`:
   - `textQuery` = "hotels in <cidade>, <país>" (ou "resorts in ..." se o filtro for resort);
   - `includedType: "lodging"`, `locationBias` circular (lat/lng da parada, raio de 15 km), `languageCode: "pt-BR"`, `pageSize: 20`;
   - `minRating` e `priceLevels` quando filtrados.
   - **Field mask:** `places.id,places.displayName,places.formattedAddress,places.location,places.rating,places.userRatingCount,places.priceLevel,places.primaryType,places.photos,places.websiteUri,places.googleMapsUri,nextPageToken`. Esses campos caem no SKU **Enterprise** (~1 mil/mês grátis): ajustar a quota diária no console (ex.: 30/dia).
2. Mapeamento de faixa (`places/PriceTierMapper`):
   - `priceLevel`: `INEXPENSIVE`→1, `MODERATE`→2, `EXPENSIVE`→3, `VERY_EXPENSIVE`→4 (`FREE` → 1), fonte `PLACES`.
   - Sem `priceLevel`: heurística por `primaryType` → `hostel`/`motel`/`campground` = 1, `bed_and_breakfast`/`guest_house` = 2, `resort_hotel` = 3, outros = `null`. Fonte `HEURISTIC`, exibida como "estimado".
   - A estimativa por IA chega na Fase 25.
   - O `priceRange` do Places não é usado (ver ESP §3.1).
3. Fotos: `GET /api/v1/places/photos?name=<photo.name>&w=400` faz proxy para `GET /v1/{name}/media?maxWidthPx=400&skipHttpRedirect=true` e responde com redirect para o `photoUri` retornado, **sem expor a chave**. Cache de 1 h. Mostrar no máximo 1 foto por resultado na lista (cada foto é cobrada).
4. Cache de buscas por 1 h (chave = parada + filtros + página). **Não persistir** os resultados no banco.
5. Endpoint: `GET /api/v1/trips/{tripId}/stops/{stopId}/lodging-search?type=hotel|resort|any&minRating=&tiers=1,2&page=` (VIEWER).
6. Ordenação: padrão do Google (relevância); opções no cliente: avaliação, nº de avaliações, preço.
7. "Adicionar como hospedagem": `POST .../lodgings` aceita `placeId`. O backend busca nome/endereço com o field mask mínimo (`id,displayName,formattedAddress`) e grava com `price_tier` + fonte.

**Frontend**
- Na parada: botão "Buscar hotéis" → tela de resultados.
  - Filtros em chips: tipo, avaliação ≥ 4/4,5, faixa.
  - Cards: foto, nome, ★ nota (nº de avaliações), `$$`, categoria, distância do centro da parada (Haversine no cliente).
  - Ações: "Ver no Maps", "Ver preços" (deep link `https://www.booking.com/searchresults.html?ss=<nome+cidade>&checkin=YYYY-MM-DD&checkout=YYYY-MM-DD&group_adults=<adultos>&group_children=<crianças>`), "Adicionar à viagem".
- A sugestão `NOITE_SEM_HOSPEDAGEM` passa a ter a ação "Buscar hotéis" apontando para essa tela.
- Texto fixo no rodapé: "Faixa de preço indicativa".

**Testes:** WireMock confere o field mask e o mapeamento de tier; o proxy de fotos não vaza a chave.

**Aceite:** buscar hotéis em Punta Cana, filtrar por resort e `$$$`, adicionar um à viagem com check-in/out da parada.

---

## Fase 17 — Restaurantes, atrações e favoritos

**Objetivo:** descobrir e salvar restaurantes/atrações por parada, e as regras correspondentes.

**Referências:** ESP §3.2, §6.4 (`RESTAURANTES`, `ATRACOES`, `RESORT_OPCOES`), §9 (`SAVED_PLACE`).

**Migration `V14__saved_place.sql`**
- `saved_place`:
  - `id`, `stop_id FK CASCADE`, `place_id VARCHAR(255) NOT NULL`, `name VARCHAR(160)` (nome no momento em que foi salvo, para exibição offline e listagem);
  - `kind ENUM('RESTAURANT','ATTRACTION','OTHER')`;
  - `price_tier TINYINT NULL`, `price_tier_source`;
  - `notes TEXT`, `visited BOOLEAN DEFAULT FALSE`, auditoria;
  - `UNIQUE(stop_id, place_id)`.

**Backend**
1. `searchNearby(stop, kind, filters)`:
   - `POST /places:searchNearby` com `includedTypes`: restaurantes = `restaurant`; atrações = `tourist_attraction`, `museum`, `park`, `beach` (conferir se `beach` é aceito no Nearby), `historical_landmark`;
   - centro = hospedagem da parada (se tiver lat/lng) ou centro da cidade; raio de 3–10 km;
   - `rankPreference: POPULARITY`;
   - mesmo field mask da Fase 16 (sem `websiteUri`, se não for usado).
   - Para restaurantes, filtro de tier (sem filtro "aberto agora", irrelevante para planejamento).
2. `GET /api/v1/trips/{tripId}/stops/{stopId}/places-search?kind=restaurant|attraction&tiers=&minRating=`.
3. CRUD de favoritos: `GET/POST/PATCH/DELETE /api/v1/trips/{tripId}/stops/{stopId}/saved-places`.
4. `TripSnapshot` passa a incluir os favoritos.
5. Novas regras:

   | Código | Condição | logicalKey | Severidade |
   |---|---|---|---|
   | `RESTAURANTES` | Parada com ≥ 2 noites e nenhum favorito `RESTAURANT` | `stopId` | INFO |
   | `ATRACOES` | Parada com ≥ 1 noite e nenhum favorito `ATTRACTION` | `stopId` | INFO |
   | `RESORT_OPCOES` | Hospedagem `ALL_INCLUSIVE` (ou nome contendo "resort") | `lodgingId` | INFO, com checklist ao aceitar: "Ver restaurantes à la carte/reservas do resort" e link para o `websiteUri` se houver `place_id` |

**Frontend**
- Por parada, seção "Lugares" com abas Restaurantes / Atrações: favoritos salvos (nome, tier, nota, ✓ visitado) e botão "Descobrir" → resultados com ★, `$$`, distância e "Salvar".
- As sugestões `RESTAURANTES`/`ATRACOES` levam direto a "Descobrir".

**Aceite:** salvar 3 restaurantes e 2 atrações em Lisboa; as sugestões correspondentes somem.

---

## Fase 18 — Atividades na timeline + drag-and-drop

**Objetivo:** planejar o que fazer em cada dia.

**Referências:** ESP §6.3, §9 (`ACTIVITY`).

**Migration `V15__activity.sql`**
- `activity`:
  - `id`, `trip_id FK CASCADE`, `day DATE NOT NULL`, `start_time TIME NULL`, `end_time TIME NULL`;
  - `title VARCHAR(200)`, `notes TEXT`, `saved_place_id FK saved_place NULL ON DELETE SET NULL`;
  - `sort_order INT`, auditoria, `version`.

**Backend**
- CRUD em `/api/v1/trips/{tripId}/activities`, mais `PUT .../activities/reorder` `{day, ids[]}`.
- Validação: `day` dentro das datas da viagem.
- `ItineraryService` inclui atividades nos eventos do dia (sem horário → depois dos eventos com horário, por `sort_order`).

**Frontend**
- Timeline com "+ atividade" por dia; atividade com horário opcional e vínculo a um favorito.
- **Drag-and-drop** com `@dnd-kit` (sensor de toque com delay de 200 ms para não conflitar com a rolagem): reordenar dentro do dia, mover entre dias, arrastar um favorito da lista "Lugares" para um dia.
- Reordenar paradas no "Roteiro" também por drag-and-drop (substitui os botões ↑/↓ da Fase 7).

**Aceite:** montar um dia com 3 atividades, uma delas a partir de um restaurante favorito, só com o toque no celular.

---

## Fase 19 — Base de requisitos de documentos

**Objetivo:** base curada de requisitos por país (nacionalidade brasileira), regras de documentos e a tela de administração.

**Referências:** ESP §3.4, §6.4 (`DOCUMENTO_DESTINO`, `DESTINO_SEM_REQUISITOS`, `REQUISITO_DESATUALIZADO`).

**Migrations**
- `V16__country.sql` — `country`:
  - `code CHAR(2) PK`, `name_pt VARCHAR(80)`, `flag_emoji VARCHAR(8)`;
  - `plug_types VARCHAR(32)` (ex.: "C,F"), `voltage VARCHAR(16)`, `currency CHAR(3)`;
  - `schengen BOOLEAN`, `br_id_card_accepted BOOLEAN` (RG brasileiro aceito).
  - Seed com todos os países: gerar o SQL a partir de uma fonte aberta (ex.: dados ISO 3166 + tomadas/voltagem de fontes públicas), revisar os campos `schengen` e `br_id_card_accepted`.
- `V17__destination_requirement.sql` — `destination_requirement`:
  - `id`, `country_code FK`, `nationality CHAR(2) NOT NULL DEFAULT 'BR'`;
  - `type ENUM('VISA','ETA','ENTRY_FORM','VACCINE','INSURANCE','PASSPORT_VALIDITY','ID_CARD','OTHER')`;
  - `title VARCHAR(200)`, `description TEXT`;
  - `lead_days INT NULL` (antecedência máxima para fazer; ex.: e-Ticket = 3);
  - `official_url VARCHAR(500) NOT NULL`;
  - `status ENUM('DRAFT','PUBLISHED') DEFAULT 'DRAFT'`, `verified_at DATE NULL`, `verified_by FK app_user NULL`;
  - `source ENUM('MANUAL','AI')`, auditoria, `version`.
- **Seed de requisitos:** **não** inserir via migration como `PUBLISHED`. O seed (lista da ESP §3.4) entra como `DRAFT`, para que o admin confirme cada um na fonte oficial e publique pela interface.

**Backend: módulo `requirements`**
1. Admin (`isAdmin`, Fase 3): CRUD em `/api/v1/admin/requirements`, mais `POST .../{id}/publish` (define `verified_at = hoje`, `verified_by`).
2. Leitura: `GET /api/v1/trips/{tripId}/requirements` → por país da viagem × nacionalidades dos viajantes, só `PUBLISHED`, com indicação de "sem dados".
3. O `TripSnapshot` (ou contexto das regras) recebe os requisitos publicados e o cadastro `country` dos países da viagem.
4. Novas regras:

   | Código | Condição | logicalKey | Severidade | Checklist automático |
   |---|---|---|---|---|
   | `DOCUMENTO_DESTINO` | Para cada requisito publicado de país da viagem × nacionalidade de algum viajante | `requirementId` | WARNING (CRITICAL se `VISA`) | sim: título do requisito, `due_date` = (data de chegada no país − `lead_days`), ou chegada − 30 dias para visto, ou sem data; nota com o `official_url` |
   | `DESTINO_SEM_REQUISITOS` | País da viagem (≠ casa) sem nenhum requisito publicado para a nacionalidade | `countryCode` | WARNING | não. Ação "gerar rascunho com IA" (Fase 26); até lá, "avisar admin" / link para a tela admin se o usuário for admin |
   | `REQUISITO_DESATUALIZADO` | Requisito usado na viagem com `verified_at` há mais de 6 meses | `requirementId` | INFO | não. Visível só para admin |

5. `PASSAPORTE_VALIDADE` e `SEGURO_VIAGEM` (Fase 14) passam a usar `country.br_id_card_accepted` e `country.schengen` em vez das listas fixas.
6. Mostrar o adaptador de tomada no item de mala (Fase 24) a partir de `plug_types`.

**Frontend**
- Seção "Documentos" na aba "Checklist" (ou aba própria): requisitos por país com link oficial, "verificado em dd/mm/aaaa" e o aviso fixo "informação indicativa, confirme na fonte oficial".
- `/admin/requirements` (só admin): lista por país e status, formulário, botão "Publicar".

**Testes:** cálculo de `due_date` por `lead_days`; requisito em DRAFT não gera sugestão; país sem dados gera `DESTINO_SEM_REQUISITOS`.

**Aceite:** publicar o e-Ticket da República Dominicana; uma viagem a Punta Cana gera o item com data limite e link oficial.

---

# Bloco Lembretes

## Fase 20 — Lembretes por e-mail + job diário (S5)

**Objetivo:** job diário que envia lembretes por e-mail, com preferências e sem duplicidade.

**Referências:** ESP §6.8, §8 (e-mail, Cloud Scheduler).

**Pré-requisito manual:** conta Gmail com verificação em 2 etapas + **senha de app** (ou conta Brevo). Segredos `smtp-username` / `smtp-password`.

**Migration `V18__notification.sql`**
- `notification_preference`:
  - `user_id PK FK`, `email_enabled BOOLEAN DEFAULT TRUE`, `push_enabled BOOLEAN DEFAULT FALSE`;
  - `daily_digest BOOLEAN DEFAULT TRUE` (um e-mail diário consolidado vs. alertas individuais);
  - `lead_days_default INT DEFAULT 3`.
- `notification_log`:
  - `id`, `user_id FK`, `channel ENUM('EMAIL','PUSH')`, `kind VARCHAR(64)`, `ref_key VARCHAR(200)` (ex.: `checklist:123:due-3`), `sent_at`;
  - `UNIQUE(user_id, channel, kind, ref_key)`.

**Backend: módulo `notification`**
1. `spring-boot-starter-mail`, SMTP do Gmail (`smtp.gmail.com:587`, STARTTLS). Templates HTML simples com Thymeleaf **só para e-mail** (`templates/mail/*.html`), em pt-BR.
2. Endpoint interno **`POST /internal/jobs/daily`**, protegido por **OIDC do Cloud Scheduler**:
   - `oauth2-resource-server` só para `/internal/**`, emissor `https://accounts.google.com`;
   - audiência = URL do serviço;
   - validar a claim `email` = conta de serviço do Scheduler (configurável). Rejeitar todo o resto. CSRF desligado só nessa rota.
3. `DailyJob`:
   1. Roda `SuggestionEngine.run` para viagens ativas (não concluídas), para as regras baseadas em tempo (`CHECKIN_ONLINE`, `REQUISITO_DESATUALIZADO`, snoozes vencidos).
   2. Coleta, para cada membro de viagens futuras:
      - itens de checklist abertos com `due_date` em {hoje + lead_days, amanhã, hoje} ou vencidos há 1 dia;
      - sugestões CRITICAL novas;
      - "viagem em 7 dias: N itens pendentes";
      - "viagem amanhã".
   3. Filtra pelo `notification_log` (não reenviar o mesmo `ref_key`).
   4. Envia (digest ou individual) e grava no log.
4. E-mail de **convite** (pendência da Fase 11): ao convidar alguém, enviar e-mail com link para o app.
5. Preferências: `GET/PUT /api/v1/me/notifications`.
6. `infra/`: comando para criar o job do Cloud Scheduler (diário às 08:00, fuso America/Sao_Paulo, `POST` com `--oidc-service-account-email` e `--oidc-token-audience`) e a conta de serviço `scheduler-invoker@...`.
7. Em `local`: `POST /dev/jobs/daily` dispara o job sem OIDC (mesma guarda de perfil do login dev).

**Frontend:** `/profile/notifications`: toggles e antecedência.

**Testes:**
- Job com `Clock` fixo: gera os e-mails certos e não reenvia na segunda execução.
- `/internal/jobs/daily` sem token ou com token de outra conta → 401/403.
- Mail com GreenMail ou mock do `JavaMailSender`.

**Aceite:** com o Scheduler em produção, o e-mail diário chega com os prazos corretos; executar duas vezes no mesmo dia não duplica.

---

## Fase 21 — Web Push (S5)

**Objetivo:** notificações push no celular/desktop via PWA.

**Referências:** ESP §6.8.

**Pré-requisitos**
- Gerar o par de chaves **VAPID** (ex.: `npx web-push generate-vapid-keys`) e guardar nos segredos `vapid-public-key` / `vapid-private-key`.
- O PWA precisa estar instalável: o manifest + service worker mínimo podem ser antecipados da Fase 27, se ainda não existirem.

**Migration `V19__push_subscription.sql`**
- `push_subscription`: `id`, `user_id FK CASCADE`, `endpoint VARCHAR(1000) UNIQUE`, `p256dh VARCHAR(255)`, `auth VARCHAR(255)`, `user_agent VARCHAR(255)`, auditoria.

**Backend**
- Biblioteca Java de Web Push (ex.: `nl.martijndwars:web-push` + BouncyCastle; conferir manutenção e, se estiver abandonada, usar uma alternativa ativa ou implementar com `java.net.http` + criptografia ECE).
- `POST /api/v1/me/push-subscriptions` (registra), `DELETE .../push-subscriptions` `{endpoint}`.
- `GET /api/v1/push/vapid-public-key`.
- `PushSender`: payload JSON `{title, body, url}`. Resposta 404/410 do serviço de push → apagar a assinatura.
- `DailyJob` (Fase 20) envia também por push quando `push_enabled`, usando o mesmo `notification_log` (canal `PUSH`).

**Frontend**
- No service worker: handler de `push` (mostra a notificação) e `notificationclick` (abre/foca `url`).
- `/profile/notifications`: "Ativar notificações neste dispositivo" → pede permissão → `pushManager.subscribe` com a chave VAPID → envia ao backend.
- No iOS, push só funciona com o PWA **instalado na tela inicial**: mostrar essa instrução quando for Safari iOS e não estiver em modo standalone.

**Aceite:** um lembrete do job diário chega como notificação no celular Android e no iPhone (com o PWA instalado).

---

## Fase 22 — Anexos (S7)

**Objetivo:** upload e download de PDFs/imagens ligados à viagem.

**Referências:** ESP §6.10, §8 (Cloud Storage).

**Pré-requisito manual (`infra/setup.sh`):**
- bucket `travel-planner-attachments-<project>` em **`us-central1`** (região do nível gratuito), classe Standard, acesso uniforme, **sem acesso público**;
- `roles/storage.objectAdmin` **no bucket** para a conta do Cloud Run;
- env `GCS_BUCKET`.

**Migration `V20__attachment.sql`**
- `attachment`:
  - `id`, `trip_id FK CASCADE`;
  - `linked_type ENUM('TRIP','SEGMENT','LODGING','CHECKLIST_ITEM') DEFAULT 'TRIP'`, `linked_id BIGINT NULL`;
  - `gcs_object VARCHAR(300)`, `filename VARCHAR(255)`, `content_type VARCHAR(100)`, `size_bytes INT`;
  - `uploaded_by FK`, auditoria.

**Backend: módulo `attachment`**
- `google-cloud-storage` (credenciais automáticas da conta de serviço no Cloud Run; em local, `gcloud auth application-default login` ou implementação `LocalFileStorage` no perfil `local`).
- Interface `FileStorage` com implementações GCS e local (pasta `./.data/attachments`).
- `POST /api/v1/trips/{tripId}/attachments` (multipart, EDITOR):
  - máximo 10 MB (`spring.servlet.multipart.max-file-size`);
  - tipos permitidos: `application/pdf`, `image/jpeg`, `image/png`, `image/heic`, validados pelos **bytes iniciais** (magic number), não só pelo header;
  - objeto em `trips/{tripId}/{uuid}`.
- `GET .../attachments`, `DELETE .../attachments/{id}` (EDITOR; apaga também no storage).
- **Download via backend** (`GET .../attachments/{id}/content`, VIEWER), com `Content-Disposition` e `Cache-Control: private, max-age=86400`.
  - Passar pelo backend (em vez de URL assinada) mantém a mesma origem, o que permite o cache offline pelo service worker (Fase 27) e dispensa a permissão de assinatura (`iam.serviceAccountTokenCreator`). Ver ESP §6.10.
- Excluir a viagem → apagar os objetos (listener no evento de exclusão).

**Frontend**
- Aba "Anexos": lista (ícone por tipo, nome, tamanho, vínculo), upload por botão (no celular abre câmera/arquivos), visualizar (PDF em nova aba), excluir.
- Nos formulários de trecho/hospedagem/item: "anexar comprovante".

**Testes:** rejeitar arquivo grande ou com tipo falsificado; VIEWER não faz upload; usuário sem acesso recebe 404 no download.

**Aceite:** anexar o PDF da reserva do hotel à hospedagem e abri-lo pelo celular.

---

# Bloco Inteligência

## Fase 23 — Integração de IA (`LlmClient` + Gemini)

**Objetivo:** infraestrutura de IA reutilizável, com cache, limites e privacidade. Ainda sem funcionalidade de usuário.

**Referências:** ESP §3.3, §12 (decisão Gemini), §11 (privacidade).

**Pré-requisito manual:** chave de API no **Google AI Studio** (nível gratuito), segredo `gemini-api-key`. Confirmar que o nível gratuito está disponível na sua conta/país e quais modelos Flash ele inclui.

**Migration `V21__ai_generation.sql`**
- `ai_generation`:
  - `id`, `trip_id FK CASCADE NULL`, `kind VARCHAR(64)`, `input_hash CHAR(64)` (SHA-256 da entrada normalizada);
  - `model VARCHAR(64)`, `output_json JSON`, `created_at`;
  - `UNIQUE(kind, input_hash)`.

**Backend: módulo `ai`**
1. Interface:
   ```java
   public interface LlmClient {
       <T> T generateStructured(LlmRequest request, Class<T> responseType); // JSON schema derivado do tipo
       boolean isAvailable();
   }
   ```
   `LlmRequest`: `systemInstruction`, `userPrompt`, `temperature`, `useWebSearch` (Fase 26).
2. `GeminiLlmClient` com o SDK oficial **`com.google.genai:google-genai`**, que também suporta Vertex AI: trocar depois é configuração (`app.ai.provider=gemini-api|vertex`).
   - Modelo configurável (`app.ai.model`, usar o Flash mais recente disponível no nível gratuito).
   - `responseMimeType: application/json` + schema de resposta.
   - Timeout de 30 s.
3. `NoopLlmClient` quando `app.ai.enabled=false` ou sem chave: `isAvailable() = false`.
4. `AiService` (fachada usada pelas features):
   - **cache**: antes de chamar, procura `ai_generation` por `(kind, input_hash)`;
   - **limite próprio**: no máximo N chamadas/dia por usuário (configurável, ex.: 20), contado em `ai_generation` + memória, para não esgotar a cota gratuita;
   - **erros**: 429/5xx/timeout → exceção `AiUnavailableException`. As features tratam mostrando "sugestões inteligentes indisponíveis agora" e seguem sem IA.
5. **Privacidade (obrigatório):** `AiPromptSanitizer` + revisão manual de cada prompt. Enviar só cidade/país, datas, nº de dias, faixas etárias, clima, tipo de viagem, nomes de lugares públicos. **Nunca** nomes de pessoas, e-mails, validade de passaporte, códigos de reserva ou notas livres do usuário.
6. Prompts em arquivos `src/main/resources/prompts/<kind>.md`, com placeholders, versionados (o nome do arquivo entra no `input_hash`).
7. Endpoint: `GET /api/v1/ai/status` → `{available}` (o frontend esconde botões de IA quando indisponível).

**Testes:** WireMock simulando a API do Gemini (sucesso, 429, JSON inválido); cache evita a segunda chamada; limite diário; teste do sanitizer.

**Aceite:** chamada de teste (endpoint dev `POST /dev/ai/ping`) funciona com a chave real em `local`; com a chave ausente, o app sobe e `available=false`.

---

## Fase 24 — Malas: regra + clima + IA

**Objetivo:** lista de mala por viajante, gerada por regra a partir do clima e ajustada opcionalmente pela IA.

**Referências:** ESP §6.6, §9 (`PACKING_ITEM`).

**Migration `V22__packing.sql`**
- `trip` ganha `trip_style SET('BEACH','CITY','SNOW','NATURE','BUSINESS')` e `laundry_available BOOLEAN DEFAULT FALSE`.
- `packing_item`:
  - `id`, `trip_id FK CASCADE`, `traveler_id FK CASCADE NULL` (NULL = item compartilhado, ex.: adaptador);
  - `category ENUM('CLOTHES','SHOES','TOILETRIES','HEALTH','ELECTRONICS','DOCUMENTS','BABY','OTHER')`;
  - `name VARCHAR(160)`, `qty INT DEFAULT 1`, `packed BOOLEAN DEFAULT FALSE`;
  - `source ENUM('RULE','AI','USER','TEMPLATE')`, `sort_order`, auditoria, `version`.

**Backend: módulo `weather`**
- `OpenMeteoClient`:
  - viagem a até 16 dias: `GET https://api.open-meteo.com/v1/forecast?latitude=&longitude=&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max,precipitation_sum&timezone=auto&start_date=&end_date=`;
  - mais distante: **média dos últimos 3 anos** nas mesmas datas via `https://archive-api.open-meteo.com/v1/archive?...&daily=temperature_2m_max,temperature_2m_min,precipitation_sum`;
  - cache de 6 h (forecast) / 30 dias (histórico) por (lat, lng, datas).
- Resumo por parada: `{minTemp, maxTemp, rainyDays, source: FORECAST|HISTORICAL}`.
- `GET /api/v1/trips/{tripId}/weather`.

**Backend: módulo `packing`**
1. `PackingRuleEngine` (puro, testável). Entrada: viajantes (faixa etária), nº de dias `D`, estilo, clima por parada, lavanderia, `country.plug_types` vs. tomadas do Brasil (N/C). Regras base (documentar no código):
   - `L = laundry ? min(D, 7) : D`;
   - roupa íntima e meias = `L + 1`; camisetas = `L`; calças/bermudas = `ceil(L / 2)`;
   - pijama = `ceil(D / 4)`, mínimo 1;
   - `maxTemp ≥ 25 °C` → +bermudas, roupa de banho (2), chinelo, protetor solar, óculos de sol, chapéu;
   - `minTemp ≤ 12 °C` → casaco, blusa (2), calça extra; `≤ 0 °C` → segunda pele, luvas, gorro, bota;
   - `rainyDays ≥ 2` → guarda-chuva/capa;
   - `BEBÊ` → fraldas = `6 × D`, lenços, mamadeira etc.; `CRIANÇA` → +1 muda extra a cada 3 dias;
   - compartilhados: adaptador de tomada (se necessário), carregadores, kit de remédios, documentos;
   - `BUSINESS` → roupa social.
2. `POST /api/v1/trips/{tripId}/packing/generate` (EDITOR):
   - gera/regenera **apenas itens `source=RULE` e `AI` não marcados como `packed`**;
   - itens `USER` e itens já marcados são preservados;
   - quantidades de itens `RULE` existentes são atualizadas.
3. `POST .../packing/ai-refine` (EDITOR, se a IA estiver disponível):
   - envia ao `AiService` (`kind=packing`) a lista base + clima + estilo + faixas etárias (sem nomes; viajantes como "adulto 1", "criança 1");
   - recebe JSON `{add:[{traveler, category, name, qty, reason}], adjust:[{itemKey, qty, reason}]}`;
   - aplica como `source=AI`; o `reason` aparece como dica na interface.
4. CRUD: `GET .../packing?traveler=`, `POST`, `PATCH .../packing/{id}` (`packed`, `qty`), `DELETE`.
5. "Salvar como template de mala": reutiliza `checklist_template` com categoria `PACKING`, ou fica fora desta fase; decidir e anotar.

**Frontend**
- Configuração da viagem: estilo (chips) e lavanderia.
- Aba "Mala":
  - resumo do clima por parada ("Lisboa: 12–19 °C, 2 dias de chuva (previsão)");
  - seletor de viajante (tabs com avatar/nome; "Compartilhado");
  - lista por categoria com checkbox grande e quantidade editável (− / +);
  - progresso por viajante;
  - botões "Gerar lista" e "Melhorar com IA" (escondido se indisponível); itens de IA com ícone ✨ e dica.

**Testes:** `PackingRuleEngine` com tabela de casos (praia 7 dias 2 adultos + 1 bebê; neve 5 dias; cidade 10 dias com lavanderia); regenerar preserva os itens do usuário; WireMock do Open-Meteo.

**Aceite:** viagem de praia de 7 dias gera uma lista coerente por viajante; regenerar após mudar as datas ajusta as quantidades sem perder o que foi marcado.

---

## Fase 25 — IA: ranking de restaurantes/atrações e estimativa de preço

**Objetivo:** usar a IA para ordenar e explicar resultados do Places, e estimar a faixa de preço quando faltar.

**Referências:** ESP §3.2, §3.1 (fallback de preço).

**Backend**
1. `POST /api/v1/trips/{tripId}/stops/{stopId}/places-search/ai-rank` `{kind, placeIds[]}`:
   - envia ao `AiService` (`kind=place-rank`) **apenas dados públicos** dos lugares (nome, tipo, nota, nº de avaliações, tier) + perfil anônimo do grupo (faixas etárias, estilo da viagem);
   - recebe `[{placeId, rank, reason}]` (máximo 5 destaques);
   - a IA **só pode escolher entre os `placeIds` enviados**: descartar qualquer id desconhecido.
2. Estimativa de faixa para hospedagens sem tier (`kind=price-tier`): lote de até 20 hotéis (nome, cidade, tipo, nota) → `[{placeId, tier, confidence}]`; aplicar só se `confidence ≥ 0.6`, com fonte `AI` ("estimado por IA"). Disparo manual: botão "Estimar faixas" na busca de hotéis.
3. Cache via `ai_generation` (entrada = lista ordenada de ids + perfil).

**Frontend**
- Nos resultados de restaurantes/atrações: botão "✨ Destaques para o grupo" → seção no topo com os 5 escolhidos e o motivo de cada um.
- Na busca de hotéis: tiers estimados com estilo visual diferente e o tooltip "estimado".

**Testes:** id inventado pela IA é descartado; baixa confiança não aplica tier.

**Aceite:** os destaques fazem sentido para "família com criança" vs. "casal"; nenhum lugar fora da lista aparece.

---

## Fase 26 — IA: rascunho de requisitos de documentos

**Objetivo:** gerar rascunhos de requisitos com fontes, para revisão do admin.

**Referências:** ESP §3.4 (alimentação sob demanda), Fase 19.

**Backend**
1. `POST /api/v1/admin/requirements/draft` `{countryCode, nationality: "BR"}` (admin), também acionável pela sugestão `DESTINO_SEM_REQUISITOS` por um admin.
2. `AiService` (`kind=requirements-draft`, `useWebSearch=true`), com **grounding na Busca Google** do Gemini.
   - Conferir se o nível gratuito permite grounding no modelo escolhido e qual é a cota.
   - Se o modelo não permitir combinar grounding com saída estruturada na mesma chamada, fazer **duas chamadas**: (1) pesquisa com grounding retornando texto + fontes; (2) estruturação do texto em JSON sem grounding.
3. Saída esperada: `[{type, title, description, leadDays, officialUrl, sources[]}]`.
   - Validar que `officialUrl` é uma URL `https`.
   - Preferir domínios governamentais (marcar `needsReview` quando não for `.gov`/`.gob`/`.gouv`/`gov.uk`/`europa.eu`/domínio oficial conhecido).
4. Gravar como `destination_requirement` `status=DRAFT`, `source=AI`. **Nunca publicar automaticamente.**
5. Notificar os admins (e-mail/push, reutilizando a Fase 20/21): "rascunho de requisitos para <país> pronto para revisão".

**Frontend**
- Admin: botão "Gerar rascunho com IA" por país, com indicador de progresso. Rascunhos com fontes clicáveis; editar → publicar.
- Usuário comum, ao ver `DESTINO_SEM_REQUISITOS`: texto explicando que o admin foi avisado (enviar a notificação ao admin na primeira ocorrência por país).

**Aceite:** gerar o rascunho para "MX", revisar, publicar, e ver o item aparecer numa viagem ao México.

---

# Bloco Offline

## Fase 27 — PWA offline: leitura

**Objetivo:** app instalável e com leitura offline completa das viagens próximas/em andamento.

**Referências:** ESP §6.9.

**Frontend**
1. `vite-plugin-pwa` com estratégia `injectManifest` (service worker próprio em `src/sw.ts`, necessário para push e para regras de cache próprias).
   - Manifest: nome, ícones 192/512 + maskable, `display: standalone`, `theme_color`, `start_url: /`.
   - Pré-cache do app shell (build do Vite).
   - Runtime caching:
     - `/api/v1/**` GET → **network-first** com fallback para cache, timeout de 3 s;
     - fotos do Places → cache-first com expiração de 7 dias e limite de 100 entradas;
     - anexos marcados como offline → cache dedicado.
   - Atualização do SW: aviso "nova versão disponível — atualizar".
2. Persistência do TanStack Query em **IndexedDB** (`@tanstack/query-async-storage-persister` + `idb-keyval`), com `gcTime` de 30 dias para as queries de viagem.
3. **"Disponível offline"** por viagem:
   - ao ativar (padrão: ativo automaticamente para viagens com início em até 7 dias ou em andamento), pré-buscar detalhe, roteiro, timeline, checklist, mala, sugestões, requisitos e clima;
   - anexos só com marcação individual ("salvar offline").
4. Indicador de conexão ("offline — mostrando dados de dd/mm hh:mm") e ações de escrita desabilitadas offline (habilitadas na Fase 28).

**Backend**
- Servir `sw.js` e `manifest.webmanifest` com `Cache-Control: no-cache`; assets com hash com `max-age=31536000, immutable`; `index.html` com `no-cache`.

**Testes:** testes manuais documentados (modo avião no celular com o PWA instalado; DevTools → offline). Opcional: Playwright com `context.setOffline(true)`.

**Aceite:** com o PWA instalado e em modo avião, abrir a viagem e navegar por roteiro, timeline, checklist, mala e anexos salvos.

---

## Fase 28 — PWA offline: escrita e sincronização

**Objetivo:** marcar checklist e mala (e editar notas simples) offline, sincronizando ao reconectar.

**Referências:** ESP §6.9 (última alteração vence por item).

**Escopo offline (escrita):**
- marcar/desmarcar item de checklist;
- marcar item da mala e alterar quantidade;
- criar item de checklist/mala simples;
- marcar favorito como visitado.

**Edição de roteiro, transporte e hospedagem continua só online** (conflitos complexos demais para o ganho).

**Frontend**
1. **Outbox** em IndexedDB (Dexie): `{id, tripId, method, url, body, clientTimestamp, attempts}`.
2. Mutações offline: aplicam atualização otimista no cache do TanStack Query e gravam na outbox.
3. Sincronização: no evento `online`, ao abrir o app e periodicamente, enviar em ordem. Para itens criados offline, usar `clientId` (UUID) para idempotência.
4. Erros:
   - 4xx (exceto 409) → descartar a operação e avisar;
   - 409 → buscar o estado do servidor e reaplicar se a alteração local for mais recente (por `clientTimestamp`);
   - 5xx/rede → retry com backoff.
5. Indicador "N alterações pendentes".

**Backend**
1. Endpoints de alteração rápida aceitam o header `X-Client-Timestamp` (ISO-8601). Para os campos `packed`/`status`/`qty`, aplicar **last-write-wins por campo**: só sobrescrever se `clientTimestamp ≥ field_updated_at` (adicionar colunas `status_updated_at` / `packed_updated_at` às tabelas `checklist_item` e `packing_item` via migration).
   - Nesses endpoints, o `version` é ignorado (a LWW por campo substitui a concorrência otimista).
2. Criação idempotente: coluna `client_id CHAR(36) UNIQUE NULL` em `checklist_item` e `packing_item`. `POST` com `clientId` já existente → retorna o existente.

**Testes:** backend (timestamps fora de ordem; `clientId` repetido); frontend (outbox com MSW simulando offline → online).

**Aceite:** dois celulares offline marcam itens diferentes da mala; ao reconectar, ambos convergem para o mesmo estado.

---

# Bloco Produção

## Fase 29 — Endurecimento: segurança, privacidade, observabilidade

**Objetivo:** deixar o sistema pronto para uso contínuo por família e amigos.

**Tarefas**
1. **Segurança**
   - **Content-Security-Policy:** `default-src 'self'`; `img-src 'self' data: https://*.googleusercontent.com https://lh3.googleusercontent.com https://*.ggpht.com` (fotos do Google/Places); `connect-src 'self'`; sem `unsafe-inline` em scripts. Testar que nada quebra.
   - HSTS (Cloud Run já é HTTPS).
   - **Rate limit simples** por usuário (Bucket4j em memória) em endpoints caros: buscas do Places e IA.
   - Revisar autorização: teste automatizado que percorre **todos** os endpoints `/api/v1/trips/{tripId}/**` (lista do OpenAPI) com um usuário não membro e espera 404.
   - Dependências: Dependabot (Maven, npm, Actions) e `npm audit` / OWASP dependency-check no CI (não bloqueante no início).
2. **Privacidade**
   - **Excluir conta:** `DELETE /api/v1/me` apaga as viagens das quais o usuário é o único OWNER (transferir para outro OWNER, se existir), memberships, viajantes, assinaturas push, preferências, logs e anexos no storage. Confirmação forte na interface (digitar "EXCLUIR").
   - **Exportar dados:** `GET /api/v1/me/export` (JSON com as viagens do usuário).
   - Página "Privacidade" simples, explicando o uso de dados e da IA (sem dados pessoais enviados).
3. **Observabilidade**
   - Logs estruturados com `traceId` (Micrometer Tracing) e `userId` no MDC.
   - **Error Reporting:** exceções 500 logadas como ERROR com stack trace (o Cloud Logging agrupa automaticamente).
   - Health check de dependências (DB) no actuator, sem expor detalhes publicamente.
   - Log de uso de APIs externas (Places por SKU, Gemini) para comparar com as cotas gratuitas.
4. **Backup:** confirmar backups automáticos do Cloud SQL (já existentes na instância) e documentar a restauração do schema `travel_planner` no `CLAUDE.md`.
5. **Revisão de custos:** conferir no Billing que o mês ficou em ~US$ 0 adicional; ajustar quotas.

**Aceite:** checklist de segurança verde; excluir conta remove tudo (teste de integração); budget sem alertas.

---

## Fase 30 — Cold start: medição e imagem nativa (opcional)

**Objetivo:** só se o cold start medido na Fase 4 (e reavaliado aqui) incomodar no uso real.

**Referências:** ESP §7.4.

**Passos**
1. Medir de novo (a aplicação cresceu): tempo até o primeiro byte após 15 min ocioso, 5 amostras.
2. Otimizações baratas primeiro:
   - `spring.main.lazy-initialization` (já ativo), revisar beans pesados no startup (`timeshape` lazy, clientes HTTP lazy);
   - **Class Data Sharing / AOT cache da JVM** (`-XX:SharedArchiveFile` gerado no build, ou o AOT cache das versões novas do JDK com Spring AOT);
   - conferir o `--cpu-boost`.
3. Se ainda for ruim (> 5 s): **imagem nativa GraalVM** (`spring-boot-maven-plugin` + `native-maven-plugin`, perfil `native`).
   - Rodar os testes em modo nativo (`./mvnw -PnativeTest test`).
   - Pontos de atenção: reflexão em bibliotecas (Places/Gemini SDK, web-push, timeshape, Flyway). Adicionar `RuntimeHints` quando necessário.
   - Build no GitHub Actions (mais lento; usar cache).
   - Dockerfile alternativo `Dockerfile.native` (estágio de build com GraalVM, runtime `distroless`/`ubi-micro`).
4. Última alternativa: `--min-instances 1`, aceitando o custo (estimar antes no Pricing Calculator e registrar a decisão na ESP §12).

**Aceite:** cold start < 2 s com imagem nativa, ou decisão documentada de manter a JVM.

---

## Apêndice A — Mapa de migrations

| Migration | Fase | Conteúdo |
|---|---|---|
| V1 | 1 | baseline |
| V2 | 3 | `app_user` |
| V3 | 3 | Spring Session |
| V4 | 5 | `trip`, `trip_member` |
| V5 | 6 | `traveler`, `trip_traveler` |
| V6 | 7 | `stop` |
| V7 | 8 | `transport_segment` |
| V8 | 9 | `lodging` |
| V9 | 11 | `trip_share_link` |
| V10 | 12 | `checklist_item` |
| V11–V12 | 13 | templates + seed |
| V13 | 14 | `suggestion` |
| V14 | 17 | `saved_place` |
| V15 | 18 | `activity` |
| V16–V17 | 19 | `country`, `destination_requirement` |
| V18 | 20 | `notification_preference`, `notification_log` |
| V19 | 21 | `push_subscription` |
| V20 | 22 | `attachment` |
| V21 | 23 | `ai_generation` |
| V22 | 24 | estilo da viagem, `packing_item` |
| V23+ | 28 | colunas de sincronização offline |

Fases feitas fora de ordem: usar o próximo número disponível e atualizar esta tabela.

## Apêndice B — Mapa de regras do motor de lacunas

| Regra | Fase |
|---|---|
| `TRANSFER_CHEGADA`, `TRANSFER_SAIDA`, `NOITE_SEM_HOSPEDAGEM`, `DESTINO_SEM_TRANSPORTE`, `TRANSPORTE_LOCAL`, `CHECKIN_ONLINE`, `PASSAPORTE_VALIDADE`, `SEGURO_VIAGEM` | 14 |
| `RESTAURANTES`, `ATRACOES`, `RESORT_OPCOES` | 17 |
| `DOCUMENTO_DESTINO`, `DESTINO_SEM_REQUISITOS`, `REQUISITO_DESATUALIZADO` | 19 |

## Apêndice C — Segredos e variáveis de ambiente

| Variável | Segredo (Secret Manager) | Fase |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | — | 4 |
| `CLOUD_SQL_INSTANCE` | — | 4 |
| `DB_USERNAME` / `DB_PASSWORD` | `db-password` | 4 |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | `google-client-id`, `google-client-secret` | 3/4 |
| `APP_ADMIN_EMAILS` | — | 3 |
| `PLACES_API_KEY` | `places-api-key` | 7 |
| `SMTP_USERNAME` / `SMTP_PASSWORD` | `smtp-username`, `smtp-password` | 20 |
| `SCHEDULER_SA_EMAIL` | — | 20 |
| `VAPID_PUBLIC_KEY` / `VAPID_PRIVATE_KEY` | `vapid-public-key`, `vapid-private-key` | 21 |
| `GCS_BUCKET` | — | 22 |
| `GEMINI_API_KEY` | `gemini-api-key` | 23 |

---

## Notas de implementação

> Registrar aqui, por fase, desvios do plano, decisões tomadas durante a implementação e medições (ex.: cold start da Fase 4).

### Fase 0
- **Porta do MySQL local: 3307** (e não 3306), porque a 3306 da máquina de desenvolvimento já está ocupada por outro container MySQL. O `docker-compose.yml` publica `${MYSQL_PORT:-3307}:3306`; a Fase 1 deve usar `jdbc:mysql://localhost:${MYSQL_PORT:3307}/travel_planner` no `application-local.yml` (já ajustado na tarefa 3 da Fase 1).
- `docker-compose.yml` ganhou `healthcheck` (`mysqladmin ping`) e `MYSQL_ROOT_PASSWORD=root` (obrigatório na imagem oficial).
- `.env.example` inclui também as variáveis do Apêndice C que não estavam na lista da Fase 0 (`MYSQL_PORT`, `SPRING_PROFILES_ACTIVE`, `CLOUD_SQL_INSTANCE`, `DB_USERNAME`, `DB_PASSWORD`, `SCHEDULER_SA_EMAIL`).

### Fase 1
- **Spring Boot 4.1.2** (última estável) com Hibernate 7.4 e **springdoc-openapi 3.2.0** (`springdoc-openapi-starter-webmvc-api`, só o JSON, sem Swagger UI). Projeto gerado pelo Spring Initializr, que já usa os starters modulares do Boot 4 (`spring-boot-starter-webmvc`, `-flyway`, e os `-test` correspondentes).
- **Porta local: 8082.** A 8080 e a 8081 da máquina de desenvolvimento já estão ocupadas, então o perfil `local` usa `server.port: ${PORT:8082}`; o `application.yml` continua com `${PORT:8080}` (Cloud Run injeta `PORT`). As Fases 2 e 3 foram ajustadas (proxy do Vite e redirect OAuth em `:8082`).
- OpenAPI desligado por padrão (`springdoc.api-docs.enabled: false`) e ligado em `application-local.yml` e `src/test/resources/application-test.yml`.
- Exceções e handler ficam em `shared/web` (`NotFoundException`, `ForbiddenException`, `GlobalExceptionHandler`). O handler estende `ResponseEntityExceptionHandler`, então as exceções padrão do Spring MVC (rota inexistente, 405, JSON inválido) também saem como `ProblemDetail`. Erros de validação trazem `errors: [{field, message}]`.
- Auditoria via Spring Data (`@EnableJpaAuditing` em `shared/persistence/JpaAuditingConfig`) com um `DateTimeProvider` que lê o `Clock` injetado; `createdAt`/`updatedAt` são `Instant` e `hibernate.jdbc.time_zone=UTC`.
- `SecurityConfig` provisório em `shared/security` (a Fase 3 substitui). O Boot ainda loga a senha gerada do usuário em memória; some na Fase 3.
- Testes: `*Test` rodam no Surefire e `*IT` no Failsafe (plugin adicionado ao `pom.xml`), ambos no `./mvnw verify`. `IntegrationTest` usa um container estático `mysql:8.0` com `@ServiceConnection`, iniciado uma vez e compartilhado entre todas as subclasses. O teste do `GlobalExceptionHandler` usa MockMvc standalone (sem contexto Spring).
- **Lombok e google-java-format** (pedido na validação da fase): Lombok configurado como `annotationProcessorPaths` no compilador e excluído do jar pelo `spring-boot-maven-plugin`; `fmt-maven-plugin` 2.29 com o goal `check` no build. O `.editorconfig` passou a usar 2 espaços para Java, como o google-java-format.

### Fase 2
- **Node 22 LTS** (`frontend/.nvmrc`). A máquina de desenvolvimento tinha Node 18 como padrão; usar `nvm use` dentro de `frontend/`.
- **TypeScript 5.9** em vez da versão mais recente: o template atual do Vite traz TypeScript 7 e oxlint, mas o plano pede ESLint, e o `typescript-eslint` só suporta TS < 6.1; o `openapi-typescript` 7 exige TS 5.x. Ferramentas: Vite 8, Vitest 5, ESLint 10 (flat config) + Prettier (com `prettier-plugin-tailwindcss`), React Router 8 (modo data), MSW 3 (a opção `onUnhandledRequest` virou `onUnhandledFrame`).
- **shadcn/ui** com base Radix, estilo `radix-nova`. Os componentes usam o pacote `cn` (do próprio shadcn) no lugar de `clsx` + `tailwind-merge`. Ajustes locais nos componentes copiados: `button`/`input` com altura de 44 px (ESP §13.1), `badge` com variantes `highlight`/`critical`/`warning`/`info`/`success`, `sonner` usando o tema do app em vez do `next-themes`. Também foram adicionados `tooltip` (usado pelo `PriceTier` estimado) e `separator`.
- **Raios** (ESP §13.6): `--radius-xl` = `--radius` (cards/sheets) e `--radius-lg` = `--radius - 4px` (botões/campos, que no shadcn usam `rounded-lg`).
- **Tema:** script inline no `index.html` aplica a classe `dark` antes do primeiro paint (a CSP da Fase 29 precisa liberar esse script por hash). A variante `dark:` ignora trechos sob `.light`, e `.light` redefine os tokens claros, para a `/dev/ui` mostrar os dois temas lado a lado. A ESP não define `--highlight-soft` no escuro: derivado do coral com 16% de opacidade.
- **Proxy do Vite:** `/login` e `/dev/ui` também são páginas da SPA; um `bypass` serve o `index.html` para `GET` nesses caminhos exatos e encaminha o resto (`/login/oauth2/code/*`, `POST /dev/login`) para o backend.
- **`SpaForwardController`:** o `PathPatternParser` do Spring 7 só aceita `**` no início ou no fim do padrão, então o mapeamento é `/**/{last:[^.]*}` (caminhos sem extensão) e os prefixos do backend são conferidos no método (404 em `ProblemDetail`). A página `/login` é encaminhada para a SPA; só `/login/**` fica com o backend. Teste em `SpaForwardControllerIT` com um `static/index.html` de teste.
- **401:** o middleware chama um handler configurável (`setUnauthorizedHandler`), que o `main.tsx` liga ao `router.navigate('/login')`.
- `/dev/ui` é registrada só com `import.meta.env.DEV` (import dinâmico; não entra no build de produção). As devtools do TanStack Query também ficam só em dev, no canto superior esquerdo para não cobrir o menu do usuário.
- O build avisa que o chunk principal passa de 500 kB (~180 kB gzip). Code splitting por rota fica para quando houver telas reais (Fases 27/29).

### Fase 3
- **Dependências do Boot 4:** `spring-boot-starter-security-oauth2-client` (o `spring-boot-starter-oauth2-client` antigo está obsoleto) e `spring-boot-starter-session-jdbc`, mais os `-test` correspondentes. `V3__spring_session.sql` é a cópia do `schema-mysql.sql` do `spring-session-jdbc` 4.1.1.
- **Pacotes:** `auth/domain` (`AppUser`, `AppUserService`, `AppUserPrincipal`, `AppOidcUserService`, `AuthProperties`), `auth/api` (`MeController` + DTOs), `auth/infra` (repositório), `auth/dev` (login dev + guarda). O `SecurityConfig` continua em `shared/security`.
- **Propriedades:** `app.auth.post-login-redirect`, `app.auth.login-error-redirect` (novo: falha no Google volta para `/login?error`, que mostra um aviso) e `app.auth.admin-emails` (o plano falava em `app.admin-emails`; ficou tudo sob `app.auth`).
- **Segredos do Google:** o `application.yml` usa `${GOOGLE_CLIENT_ID}` sem padrão (produção não sobe sem eles). O perfil `local` usa um valor provisório, para subir só com o login dev, e importa o `.env` da raiz (`spring.config.import: optional:file:../.env[.properties]`), então não é preciso exportar variáveis.
- **Redirect OAuth em dev:** o perfil `local` fixa `redirect-uri: http://localhost:8082/login/oauth2/code/google`. Pelo proxy do Vite o backend veria o host `localhost:5173`; com o URI fixo o Google volta direto ao backend, e o cookie de sessão (de `localhost`, sem distinção de porta) vale para o Vite.
- **`AppUserPrincipal`** estende `DefaultOidcUser` (serializável, vai para a sessão JDBC) e `getName()` devolve o id do usuário, que fica na coluna `PRINCIPAL_NAME` da sessão. O e-mail/nome do Google são atualizados a cada login; `locale` não (fica `pt-BR`). A nacionalidade e a cidade só mudam pelo perfil.
- **CSRF:** `csrf.spa()` do Spring Security 7 adia o token, então o cookie `XSRF-TOKEN` não era escrito até o primeiro `POST`. Um `CsrfCookieFilter` carrega o token em toda requisição (como na documentação oficial).
- **Request cache desligado:** depois do login vamos sempre para `post-login-redirect`, e sem o cache um 401 anônimo não cria linha em `SPRING_SESSION`.
- **Cookie de sessão:** `HttpOnly`, `SameSite=Lax`, `Max-Age` e timeout de 30 dias (`Secure` em `application-prod.yml`). Com `Max-Age` a sessão sobrevive ao fechamento do navegador no celular; o cookie não é renovado a cada acesso, então o usuário entra de novo a cada 30 dias.
- **`PATCH /api/v1/me`** substitui nacionalidade e cidade de origem (`homeCity` ausente limpa a cidade) e exige `version` (divergente → 409). Fuso validado com uma constraint `@TimeZoneId` (`shared/validation`). O `GET` devolve também `version`.
- **Login dev:** `POST /dev/login?email=` cria o usuário com `google_sub = dev:<email>`, troca o id da sessão e grava o contexto; exige CSRF. O `DevLoginGuard` derruba a inicialização se o controller existir com o perfil `prod` (ex.: `prod,local` por engano); testado com `ApplicationContextRunner`.
- **Testes:** no MockMvc o `spring-security-test` troca o repositório de CSRF por um baseado em sessão, então os cookies (`XSRF-TOKEN`, `SESSION`) são testados por HTTP de verdade em `CookiesOverHttpIT` (`RANDOM_PORT`). `TestUsers` (teste) provisiona o usuário e devolve o `oidcLogin()` com `AppUserPrincipal`, para as próximas fases.
- **OpenAPI:** `springdoc.default-produces-media-type: application/json`, para o `openapi-fetch` tipar as respostas (o padrão era `*/*`). Métodos nomeados `getMe`/`updateMe`/`devLogin` para dar `operationId` legíveis.
- **Frontend:** guarda `RequireAuth` (componente) em volta do `AppLayout`, em vez de loader, porque o `QueryClient` vive no React. `useMe()` devolve `null` em 401. O `openapi-fetch` passou a resolver o `fetch` a cada chamada (senão o MSW não intercepta o cliente criado no import). Nacionalidade e país usam um `NativeSelect` (seletor nativo no celular) com nomes do `Intl.DisplayNames` e bandeiras em emoji; a lista de códigos ISO veio do JDK. O formulário dev de login só existe com `import.meta.env.DEV` (não entra no build de produção).
- **`./dev.sh`** (pedido na validação): sobe MySQL (`docker compose up -d --wait`), backend (`spring-boot:run`, que compila) e Vite com um comando, com logs prefixados (`[api]`/`[web]`). Confere as portas antes (se a porta estiver com uma instância antiga deste projeto, pergunta se pode encerrá-la), instala as dependências do frontend se o `package-lock.json` mudou e encerra os dois processos (incluindo o java do Maven) no Ctrl+C. Para desenvolvimento ficou um script, e não o Docker, para manter o hot reload; a imagem Docker é da Fase 4.

### Fase 4
- **Projeto e banco:** o Cloud SQL existente é MySQL **8.4** (o connector `mysql-socket-factory-connector-j-8` 1.30.2 funciona igual). Database `travel_planner` e usuário `travel_planner` na mesma instância de outros apps. A região do Cloud Run é a do Cloud SQL. O `setup.sh` aceita o Cloud Run num **projeto próprio** (`SQL_PROJECT_ID` = projeto do banco): a conta do Cloud Run recebe `cloudsql.client` no projeto do banco, e o orçamento passa a cobrir o projeto inteiro, sem filtro por serviço.
- **Permissões do usuário do banco:** o `gcloud sql users create` dá o papel `cloudsqlsuperuser` e não há como rodar SQL pelo `gcloud`; o `setup.sh` imprime o `REVOKE`/`GRANT` para rodar uma vez como root no Cloud SQL Studio. A senha é gerada pelo script, fica só no segredo `db-password` e é reaplicada no usuário a cada execução. `DB_USERNAME` é env var comum (não segredo).
- **Config do Cloud Run no `setup.sh`:** o primeiro deploy usa a imagem `cloudrun/container/hello` (só para ter a URL); rodadas seguintes fazem `services update`, sem trocar a imagem. O `deploy.yml` só troca a imagem; env vars, segredos, conta de serviço, Cloud SQL e limites persistem entre revisões. Fases futuras acrescentam env vars/segredos no script.
- **`deploy.yml`** dispara por `workflow_run` quando o CI termina com sucesso num push na `main` (e por `workflow_dispatch`), faz checkout do `head_sha` do CI, build com cache do GitHub Actions (buildx `type=gha`), deploy e um *smoke test* em `/actuator/health`.
- **WIF:** provider OIDC restrito a `assertion.repository == '<repo>' && assertion.ref == 'refs/heads/main'` (PRs não conseguem credencial). O deployer tem `roles/run.developer` no projeto (no lugar de `run.admin`: o deploy não mexe em IAM), `artifactregistry.writer` só no repositório e `iam.serviceAccountUser` só na conta do Cloud Run. O `allUsers → run.invoker` é aplicado pelo `setup.sh`. As variáveis do repositório são gravadas pelo script via `gh`.
- **Orçamento:** US$ 1 com alertas em 50/90/100%, filtrado pelo projeto **e** pelos serviços do app (Cloud Run, Artifact Registry, Secret Manager, Places API/New, Cloud Scheduler; IDs do catálogo do Cloud Billing no script), porque o projeto tem outros custos (o Cloud SQL, outros apps). Cloud Storage fica de fora pelo mesmo motivo; reavaliar na Fase 22.
- **Dockerfile:** o estágio Maven usa o `mvn` da imagem `maven:3.9` (não o `./mvnw`, que baixaria o Maven de novo) e uma camada de `dependency:go-offline` para cache (o `package` final não pode ser `-o`: plugins resolvem dependências que o `go-offline` não baixa). Usuário `app` (não root). JAR de ~129 MB; imagem de ~450 MB.
- **Log:** `GcpLogFormatter` escreve uma linha JSON com `time`, `severity` (WARN → `WARNING`), `message` (com a pilha da exceção, para o Error Reporting), `logger`, `thread`.
- **Testes:** `ProdProfileIT` sobe o perfil `prod` (com `lazy-initialization`) contra o Testcontainers: Flyway aplicado, health `UP`, sem `DevLoginController`, e o `/oauth2/authorization/google` atrás de `X-Forwarded-Proto: https` gera `redirect_uri` `https://...` e cookie `SESSION` com `Secure`. A imagem foi testada localmente no perfil `prod` contra o MySQL do compose (subida em ~10,5 s emulando amd64 num Mac ARM; ~390 MB de RAM).

