# Rikkaus Wealth OS

Rikkaus Wealth OS is a mobile-first personal wealth application. The MVP 0 repository includes
an Expo frontend foundation, a Spring Boot modular monolith, and local PostgreSQL.

## Prerequisites

- Node.js 22.13 or newer with npm 10 or newer
- Java 25 when running the API directly on the host
- Docker with Docker Compose

Maven does not need to be installed globally; use the committed Maven Wrapper.

## Run the whole application locally

These steps start PostgreSQL, the API and the Expo web app together, then sign you in with a local
development account. No Google OAuth client is needed. Run each long-running command in its own
terminal, from the repository root.

1. **Start PostgreSQL** (Docker):

   ```bash
   docker compose --env-file .env.example up -d --wait postgres
   ```

2. **Start the API with the `local` profile.** The profile supplies a fixed development JWT secret,
   allows the Expo web origin `http://localhost:8081`, and enables the development sign-in endpoint.
   Without it the API refuses to start, because `RIKKAUS_JWT_SECRET` has no default.

   ```bash
   ./services/api/mvnw -f services/api/pom.xml spring-boot:run -Dspring-boot.run.profiles=local
   ```

   If your default `java` is older than 25, point `JAVA_HOME` at a JDK 25 for this command, for
   example `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home` with Homebrew.
   Flyway applies every migration on startup. The API is ready when
   `curl http://localhost:8080/actuator/health` returns `{"status":"UP"}`.

3. **Configure the web app** once:

   ```bash
   cp apps/mobile/.env.example apps/mobile/.env.local
   ```

   Then set these two values in `apps/mobile/.env.local`:

   ```dotenv
   EXPO_PUBLIC_API_BASE_URL=http://localhost:8080
   EXPO_PUBLIC_DEV_SIGN_IN=local
   ```

4. **Install dependencies and start Expo web.** Run `npm ci` again after every pull that changes
   `apps/mobile/package-lock.json`; a stale install fails at startup with
   `PluginError: Failed to resolve plugin for module ...`.

   ```bash
   npm --prefix apps/mobile ci
   npm --prefix apps/mobile run web
   ```

   Keep the default port 8081. The API's CORS allowlist and the Google redirect URI are both set for
   `http://localhost:8081`, and the refresh cookie is only accepted from that origin.

5. **Sign in.** Open `http://localhost:8081`. The app redirects to `/sign-in`. Scroll to
   **Đăng nhập phát triển**, keep or change the prefilled email and display name, and choose
   **Đăng nhập phát triển**. A new email creates a new account; the same email signs back into it.
   Choose **Mở Tổng quan** to enter the app.

What you can use after signing in:

| URL | What it shows |
|---|---|
| `http://localhost:8081/` | The app shell with the five destinations: Tổng quan, Tài sản & Nợ, Dòng tiền, Mục tiêu, Nhận định. Domain screens are still placeholders; they are built by later epics. |
| `http://localhost:8081/settings/exchange-rate` | The destination of the first-sign-in step. Only the title exists today; the rate form is a later task. Declare a rate through the API (`PUT /api/v1/settings/exchange-rate`) until then. |
| `http://localhost:8081/profile` | The signed-in account and sign-out. |
| `http://localhost:8081/catalog/money` | The design-system catalog (also `shell`, `forms`, `data`, `states`, `overlays`). |
| `http://localhost:8081/diagnostics` | API connectivity check. |
| `http://localhost:8080/swagger-ui/index.html` | Swagger UI for every backend endpoint (`local` profile only). |

The development sign-in exists only when the API runs under `local` and the web bundle is a
development build with `EXPO_PUBLIC_DEV_SIGN_IN=local`; `npm --prefix apps/mobile run build:web`
never includes it. To sign in with a real Google Account instead, see
[Signing in locally](#signing-in-locally). Step-by-step API checks with `curl` are in
[`docs/manual-test-guide-auth.md`](docs/manual-test-guide-auth.md).

To stop, press `Ctrl+C` in the Expo and API terminals, then run
`docker compose --env-file .env.example down` (the database volume is kept).

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
| `RIKKAUS_JWT_SECRET` | none | Signing key for the short-lived JWT access tokens, at least 32 bytes. **The application refuses to start without it**, because a guessable key lets anyone mint a token for any user. Generate one with `openssl rand -base64 48`. The `local` profile carries a fixed development value, so only deployed environments need to set this. |
| `GOOGLE_OAUTH_CLIENT_ID` | empty | The Google OAuth client the backend redeems authorization codes against. |
| `GOOGLE_OAUTH_CLIENT_SECRET` | empty | Optional. Leave empty for a public client using PKCE alone. |
| `GOOGLE_OAUTH_REDIRECT_URIS` | empty (`local`: `http://localhost:8081/auth/callback`) | Comma-separated allowlist of redirect URIs, compared exactly. **Empty permits none**, which is the intended default: the value is forwarded to Google, which delivers the authorization code to whatever it names. Every entry must also be registered on the Google OAuth client. |

## Signing in locally

Identity is **Google Account OIDC only**. There is no password, no sign-up form and no app-managed
credential — that is the accepted product direction, not a gap.

To exercise sign-in against real Google you need a Google Cloud OAuth 2.0 client:

1. In Google Cloud Console, create an **OAuth client ID** of type *Web application*.
2. Add `http://localhost:8081/auth/callback` as an authorised redirect URI.
3. Put the client id in `GOOGLE_OAUTH_CLIENT_ID`, and the secret in `GOOGLE_OAUTH_CLIENT_SECRET` if
   the client is confidential.

None of this is needed to build or test. The Google token exchange and identity-token verification sit
behind interfaces that the test suite replaces, so `./services/api/mvnw verify` runs offline with no
credentials and no live Google account.

The session endpoints are described in
[`docs/api-contract-conventions.md`](docs/api-contract-conventions.md).

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
./services/api/mvnw -f services/api/pom.xml spring-boot:run -Dspring-boot.run.profiles=local
```

Without the `local` profile the API needs `RIKKAUS_JWT_SECRET` and refuses to start when it is
unset. To run without the profile, export a copied `.env` before starting the API:

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
error responses, correlation identifiers, a two-tier test harness, and Google OIDC sign-in with
rotating refresh tokens and server-side ownership enforcement. See
[`docs/api-contract-conventions.md`](docs/api-contract-conventions.md) for what that means for a
client.


It still deliberately contains no domain endpoints or tables, no CI/CD pipeline and no domain UI.
Those are owned by their dedicated delivery tasks. The security chain now **denies by default**, so an
unauthenticated request to anything outside the documented public routes returns 401.

It still deliberately contains no domain endpoints or tables, no authentication, no ownership
enforcement and no domain UI. Those are owned by their dedicated delivery tasks.

The backend now has a pipeline and a deployable AWS preview environment, described in
[`docs/backend-deployment.md`](docs/backend-deployment.md). Every pull request runs formatting,
unit, integration and coverage gates; a push to the integration branch publishes an immutable image
and rolls an ECS Fargate service that answers on a public URL, so a reviewer can check the running
API without a checkout.

That preview is served over HTTPS by a CloudFront distribution using its own `*.cloudfront.net`
certificate, and the load balancer behind it accepts connections from CloudFront alone, so there is
no way to reach the API in clear text. The address is therefore a generated hostname rather than a
name of ours; adopting a domain is a follow-up, not a prerequisite.

The earlier warning that an unauthenticated API must not leave local development still applies to
anything with data behind it. This environment is safe today because there is nothing behind it:
the only reachable endpoints are `/actuator/health`, `/api/v1/meta` and an error surface, all
anonymous by design and disclosing no user data.

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
