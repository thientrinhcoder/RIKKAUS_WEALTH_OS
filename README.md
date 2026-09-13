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

This foundation deliberately contains no domain endpoints or tables, authentication, ownership
logic, full OpenAPI/RFC 9457 conventions, CI/CD pipeline, domain UI, or Phase 2 behavior. Those
are owned by their dedicated delivery tasks.

To favor time-to-market, the backend slice contains no unit, integration, Testcontainers, or
ArchUnit tests. Therefore, issue #35's original automated-test acceptance criterion remains
intentionally unsatisfied; backend validation for that slice is limited to build and live-runtime
checks. The frontend foundation has its own unit and component test harness.
