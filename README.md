# Rikkaus Wealth OS

Rikkaus Wealth OS is a mobile-first personal wealth application. The MVP 0 repository includes
an Expo frontend foundation, a Spring Boot modular monolith, and local PostgreSQL.

## Prerequisites

- Node.js 22.13 or newer with npm 10 or newer
- Java 25 when running the API directly on the host
- Docker with Docker Compose

Maven does not need to be installed globally; use the committed Maven Wrapper.

## Backend versions

- Java 25
- Spring Boot 4.1.1
- Maven Wrapper 3.3.4 with Maven 3.9.11
- PostgreSQL 18.6 (`postgres:18.6-alpine3.24`)

## Local configuration

The checked-in root `.env.example` contains disposable backend local-development values. Copy it
when you need machine-specific overrides; real `.env` files are ignored by Git.

```bash
cp .env.example .env
```

Backend environment variables:

| Variable | Default | Purpose |
|---|---|---|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT` | see `.env.example` | Local PostgreSQL container credentials and port. |
| `DB_URL` | `jdbc:postgresql://localhost:5432/rikkaus` | JDBC URL the API connects with. |
| `API_ALLOWED_ORIGINS` | empty | Comma-separated browser origins allowed to call `/api/v1/**` and `/actuator/health`. **Empty permits nothing** and registers no CORS mapping at all, which is the intended default outside local development. The `local` Spring profile sets `http://localhost:8081`, the origin Expo web serves on. |

## Start PostgreSQL

```bash
docker compose --env-file .env.example up -d --wait postgres
docker compose --env-file .env.example ps
```

PostgreSQL is exposed on `localhost:5432` by default. Change both `POSTGRES_PORT` and `DB_URL`
together if that port is already reserved by another service.

## Build and run the API

From the repository root:

```bash
./services/api/mvnw -f services/api/pom.xml clean package
./services/api/mvnw -f services/api/pom.xml spring-boot:run
```

The API uses the values from `application.yml` by default. To use a copied `.env`, export it
before starting the API:

```bash
set -a
source .env
set +a
./services/api/mvnw -f services/api/pom.xml spring-boot:run
```

Verify the running service:

```bash
curl --fail --silent http://localhost:8080/actuator/health
```

The expected response has status `200` and reports `{"status":"UP"}`. Only the Actuator health
endpoint is exposed by this foundation.

## Verify Flyway ownership

Flyway is the only schema-change mechanism. Hibernate is configured with `ddl-auto: validate`,
and Spring SQL initialization is disabled. Migration `V1__baseline.sql` creates the empty
`wealth` schema namespace; it does not create domain tables.

```bash
docker compose --env-file .env.example exec -T postgres \
  psql -U rikkaus -d rikkaus -c 'TABLE flyway_schema_history;'

docker compose --env-file .env.example exec -T postgres \
  psql -U rikkaus -d rikkaus \
  -c "SELECT nspname FROM pg_namespace WHERE nspname = 'wealth';"

docker compose --env-file .env.example exec -T postgres \
  psql -U rikkaus -d rikkaus \
  -c "SELECT schemaname, tablename FROM pg_tables WHERE schemaname = 'wealth';"
```

The Flyway history must show version `1` as successful, the namespace query must return `wealth`,
and the table query must return no domain tables.

## Frontend foundation

The Expo application lives in `apps/mobile` and supports browser, iOS, and Android development
from one strict TypeScript codebase.

Install its locked dependencies:

```bash
npm --prefix apps/mobile ci
```

Copy the frontend environment example and point it at the local API:

```bash
cp apps/mobile/.env.example apps/mobile/.env.local
```

`EXPO_PUBLIC_API_BASE_URL` is public client configuration and must never contain credentials or
secrets. The frontend requests `GET /actuator/health`; a healthy response must contain at least
`{"status":"UP"}`. Additional Actuator response fields are allowed.

Run the application:

```bash
npm --prefix apps/mobile run web
npm --prefix apps/mobile run ios
npm --prefix apps/mobile run android
```

The browser command is the primary MVP preview path. iOS and Android commands require their
respective simulator, emulator, or device tooling.

Verify the frontend:

```bash
npm --prefix apps/mobile run lint
npm --prefix apps/mobile run typecheck
npm --prefix apps/mobile test
npm --prefix apps/mobile run expo:check
npm --prefix apps/mobile run build:web
```

Frontend health-state troubleshooting:

- **API endpoint is not configured:** set `EXPO_PUBLIC_API_BASE_URL` in
  `apps/mobile/.env.local`, then restart Expo.
- **API health check failed:** confirm the backend is running, the configured URL is reachable
  from the selected platform, and backend CORS permits the browser origin.
- **Expo dependency mismatch:** run `npx --prefix apps/mobile expo install --check` and install
  Expo-coupled packages through `npx expo install` from `apps/mobile`.

The frontend does not substitute a mock or fallback response when the backend is unavailable.

## Stop local services

Stop PostgreSQL while preserving its named data volume:

```bash
docker compose --env-file .env.example down
```

Deleting the volume with `docker compose --env-file .env.example down -v` permanently removes the
local database and should only be done intentionally.

## Current scope limits

The backend now has a versioned `/api/v1` surface, a build-published OpenAPI contract, RFC 9457
error responses, correlation identifiers, and a two-tier test harness. See
[`docs/api-contract-conventions.md`](docs/api-contract-conventions.md) for what that means for a
client.

It still deliberately contains no domain endpoints or tables, no authentication, no ownership
enforcement, no CI/CD pipeline and no domain UI. Those are owned by their dedicated delivery tasks.
Because authentication is absent, this API must not be exposed beyond local development yet.

The backend slice now has unit, integration, Testcontainers and ArchUnit tests, so issue #35's
original automated-test acceptance criterion is satisfiable: it was intentionally unsatisfied because
no harness existed, and the harness is what this foundation added. The frontend foundation has its own
unit and component test harness.

### Flyway was repaired, not merely configured

Until this foundation, **Flyway had never run in this service.** `FlywayAutoConfiguration` lives in
`org.springframework.boot:spring-boot-flyway`, a module Spring Boot 4 split out and which no declared
starter brings in, so every `spring.flyway.*` setting in `application.yml` was inert. `V1__baseline.sql`
was silently skipped and startup still succeeded, because with no `@Entity` in the codebase
`ddl-auto: validate` had nothing to check. Issue #35's unchecked "Flyway applied `V1`" acceptance box
was accurate.

Adding that module makes migrations actually run. On a database volume predating the change the
migration simply applies now; the baseline only creates a schema, so there is no destructive step.
Before-and-after evidence is recorded in
[`plans/reports/pm-260927-issue-39-acceptance.md`](plans/reports/pm-260927-issue-39-acceptance.md).

## Verify the backend

```bash
./services/api/mvnw -f services/api/pom.xml test
```

Fast tier: unit and `@WebMvcTest` slice tests. Needs neither Docker nor PostgreSQL.

```bash
./services/api/mvnw -f services/api/pom.xml verify
```

Full tier: additionally runs `*IT` classes against a Testcontainers PostgreSQL, and fails the build if
the committed OpenAPI contract no longer matches the application. **This requires a running Docker
daemon.** Do not work around that with `-DskipITs`, which also disables the contract drift gate.

```bash
./services/api/mvnw -f services/api/pom.xml verify -Dopenapi.update=true
```

Regenerates `services/api/openapi/openapi.json` after an intended API change. Review the resulting diff
and notify affected sibling tasks before merging. **This flag is a deliberate local action and must
never appear in a CI command**, because it converts the drift check into a silent rewrite.
