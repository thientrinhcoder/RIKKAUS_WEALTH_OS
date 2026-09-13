# Rikkaus Wealth OS

Rikkaus Wealth OS is a mobile-first personal wealth application. This repository currently
contains the MVP 0 backend foundation: a Spring Boot modular monolith and local PostgreSQL.

## Prerequisites

- Java 25 when running the API directly on the host
- Docker with Docker Compose

Maven does not need to be installed globally; use the committed Maven Wrapper.

## Backend versions

- Java 25
- Spring Boot 4.1.1
- Maven Wrapper 3.3.4 with Maven 3.9.11
- PostgreSQL 18.6 (`postgres:18.6-alpine3.24`)

## Local configuration

The checked-in `.env.example` contains disposable local-development values. Copy it when you
need machine-specific overrides; real `.env` files are ignored by Git.

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

## Stop local services

Stop PostgreSQL while preserving its named data volume:

```bash
docker compose --env-file .env.example down
```

Deleting the volume with `docker compose --env-file .env.example down -v` permanently removes the
local database and should only be done intentionally.

## Current scope limits

This foundation deliberately contains no domain endpoints or tables, authentication, ownership
logic, full OpenAPI/RFC 9457 conventions, CI/CD pipeline, frontend, or Phase 2 behavior. Those are
owned by their dedicated delivery tasks.

To favor time-to-market, this slice also contains no unit, integration, Testcontainers, ArchUnit,
or UI tests. Therefore, issue #35's original automated-test acceptance criterion remains
intentionally unsatisfied; validation for this slice is limited to build and live-runtime checks.
