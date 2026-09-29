# API contract conventions

What you need to build against the Rikkaus Wealth OS backend. Everything here is implemented and
verified, not planned. Where something is deliberately absent, this document says so rather than
leaving you to discover it.

## Base path and versioning

Every resource lives under `/api/v1`, defined once as `ApiPaths.V1` in
[`ApiPaths.java`](../services/api/src/main/java/com/rikkaus/wealth/shared/api/ApiPaths.java). No other
file contains the literal. A new major version gets a new static prefix.

Spring Framework 7's native `spring.mvc.apiversion` header and path-segment versioning is deliberately
unused. springdoc [issue #3163](https://github.com/springdoc/springdoc-openapi/issues/3163) reports
that enabling it makes both `/v3/api-docs` and `/swagger-ui.html` return HTTP 400 on Boot 4.x. A static
path prefix is unaffected and must stay that way.

## The contract

| | |
|---|---|
| Location | [`services/api/openapi/openapi.json`](../services/api/openapi/openapi.json) |
| How it is produced | Generated from the running application by `OpenApiContractIT` during `verify` |
| How it is enforced | The same test fails the build when the generated and committed documents differ |
| Regenerate | `./services/api/mvnw -f services/api/pom.xml verify -Dopenapi.update=true` |

`-Dopenapi.update=true` is a deliberate local action. **It must never appear in a CI command**, because
it turns the drift check into a silent rewrite and the contract would then change without anyone
reading the diff. For the same reason `-DskipITs` is forbidden in CI: it disables the drift gate
entirely along with every other integration test.

Regenerating is not the end of the job. Any contract change must ship with updated examples and a note
on the affected sibling task before merge.

Live, the document is served at `/v3/api-docs` and browsable at `/swagger-ui/index.html`, **but only
under the `local` Spring profile**. Both return 404 otherwise. That gate is on the document endpoint,
not just the UI, because the contract describes every route and the whole error taxonomy on a service
that has no authentication yet.

## Errors

Every error under `/api/v1/**` is `application/problem+json` conforming to
[RFC 9457](https://www.rfc-editor.org/rfc/rfc9457), including framework-raised 400, 404, 405, 406 and
415. There is no status that returns Boot's default error shape.

The taxonomy is [`ProblemType`](../services/api/src/main/java/com/rikkaus/wealth/shared/error/ProblemType.java).
The `type` is a URN rather than a URL because this project owns no domain and an `https://` type would
name a host that resolves to nothing; RFC 9457 does not require the type to dereference.

| Code | `type` | Title | Status |
|---|---|---|---|
| `validation-failed` | `urn:rikkaus:problem:validation-failed` | Request validation failed | 400 |
| `malformed-request` | `urn:rikkaus:problem:malformed-request` | Malformed request | 400 |
| `not-found` | `urn:rikkaus:problem:not-found` | Resource not found | 404 |
| `method-not-allowed` | `urn:rikkaus:problem:method-not-allowed` | Method not allowed | 405 |
| `not-acceptable` | `urn:rikkaus:problem:not-acceptable` | Not acceptable | 406 |
| `unsupported-media-type` | `urn:rikkaus:problem:unsupported-media-type` | Unsupported media type | 415 |
| `unauthorized` | `urn:rikkaus:problem:unauthorized` | Authentication required | 401 |
| `forbidden` | `urn:rikkaus:problem:forbidden` | Access denied | 403 |
| `internal-error` | `urn:rikkaus:problem:internal-error` | Unexpected server error | 500 |

`unauthorized` is now reachable: it is what a missing, expired, revoked or malformed credential returns —
see *Authentication* below. `forbidden` is declared and remains unreachable in practice, because MVP 0 has a
single role and an ownership failure deliberately returns `not-found` rather than `forbidden`. Keep handling
it: it is wired up, so a framework-level denial renders as a conforming problem document rather than as
Boot's default body.

A real response body, copied from live output rather than invented:

```json
{
  "detail": "Resource not found.",
  "instance": "/api/v1/does-not-exist",
  "status": 404,
  "title": "Resource not found",
  "type": "urn:rikkaus:problem:not-found",
  "correlationId": "trace-me-0001"
}
```

Two extension members beyond the RFC's standard ones:

- **`correlationId`** — a string, present on every problem body. Same value as the
  `X-Correlation-Id` response header and the server's log lines for that request.
- **`errors`** — an array, present on validation failures only. Each entry is
  `{"field": "...", "message": "..."}`, one per rejected field. Assert against this rather than
  against the status code alone.

`detail` is always safe to show a user. It never carries a stack trace, an internal class name, a SQL
fragment or a credential, and for the statuses where Spring's own text would echo your input it is
replaced with the taxonomy's fixed text. `instance` is the request URI, so it does contain the path you
sent — that is the RFC's definition, not a leak.

## Correlation

Send `X-Correlation-Id` if you want to choose the value. It is echoed when it matches
`^[A-Za-z0-9-]{8,64}$`, and otherwise replaced with a generated UUID. **A rejected value never fails
your request** — you get a working response with a generated identifier instead.

The header is returned on every response, including errors, and it is listed in
`Access-Control-Expose-Headers` on `/api/v1/**` so browser JavaScript can read it. On
`/actuator/health` the header is set but deliberately not exposed to script, because that mapping
allows no custom or exposed headers.

## CORS

`API_ALLOWED_ORIGINS` is a comma-separated allowlist covering `/api/v1/**` and `/actuator/health`.
**Empty permits nothing**: no CORS mapping is registered at all rather than a guessed origin being
allowed, and that is the default in every environment. The `local` profile sets
`http://localhost:8081`, which is the origin Expo web actually serves on.

Credentials are refused. That is the correct posture only because no credential exists yet; enabling
them is a real decision that belongs with the authentication design and must not be inherited from here
as already settled.

## Testing conventions

| Naming | Runner | Needs Docker | What belongs there |
|---|---|---|---|
| `*Test` | Surefire, `mvnw test` | No | Unit tests and `@WebMvcTest` slice tests |
| `*IT` | Failsafe, `mvnw verify` | Yes | Anything needing the real application or database |

An integration test extends
[`AbstractPostgresIntegrationTest`](../services/api/src/test/java/com/rikkaus/wealth/support/AbstractPostgresIntegrationTest.java)
and writes no container boilerplate. The container image tag is pinned to the tag in
`docker-compose.yml`, one container is started for the whole run, and the test profile points at an
unresolvable datasource host so a wiring regression fails loudly instead of running against a
developer's real database.

`verify` therefore fails closed without a Docker daemon. That is intentional. CI must provide Docker
rather than reach for `-DskipITs`.

## Authentication

Identity is **Google Account OIDC only**. There is no password, no sign-up endpoint and no app-managed
credential. That is the accepted product direction (the Product Owner decision recorded in the design for
issue #40), not an unfinished piece.

### The four routes

| Route | Token needed | Body | Success |
|---|---|---|---|
| `POST /api/v1/auth/google` | none | `authorizationCode`, `codeVerifier`, `redirectUri` | 200, a session |
| `POST /api/v1/auth/refresh` | none | `refreshToken` | 200, a new session |
| `POST /api/v1/auth/logout` | none | `refreshToken` | 204 |
| `GET /api/v1/auth/session` | **bearer** | — | 200, the signed-in user |

The client performs the Google handoff and sends the **authorization code**, not an identity token. The
backend redeems it with PKCE, so the Google client secret never reaches the browser and no Google token is
ever held by the client. `redirectUri` must appear in the server's exact-match allowlist or the request is
a 400 with an `errors` entry for `redirectUri`; an unregistered value is never forwarded to Google.

Refresh and logout are deliberately **unauthenticated**. Their credential is the refresh token in the body.
Requiring a live access token would make an expired session impossible to recover from.

A session response:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresInSeconds": 900,
  "refreshToken": "1mQ7rVx0Yb3TkPzFhLdW8sNcJgA6eRuq2ZoXiB4ySvE",
  "user": { "id": "0f2c8d14-...", "email": "an.nguyen@example.com", "displayName": "An Nguyễn" }
}
```

Send the access token as `Authorization: Bearer <accessToken>`. The published contract declares this as the
`bearerAuth` security scheme, applied per route rather than globally, so sign-in and renewal are not
described as needing a token they exist to provide.

### Session lifetimes and rotation

- The access token is a signed JWT valid for **15 minutes**. It carries the user id and session id and
  nothing else — no email, no name.
- The refresh token is valid for **30 days** and is **single-use**. Every renewal returns a new one and
  retires the presented one.
- **Presenting an already-retired refresh token revokes the entire session.** It is treated as evidence
  the value leaked, and by then the successor may already be in someone else's hands. The legitimate user
  is signed out and signs in again.
- Refresh tokens exist in the database only as a SHA-256 hash. An adaptive password hash is deliberately
  not used: the token is 256 random bits, so there is no low-entropy secret for a cost factor to protect.

`GET /api/v1/auth/session` is how a client distinguishes a live session from an expired or revoked one
without inferring it from a failed domain call.

**One limitation, stated plainly:** signing out revokes the ability to renew, not the access token already
issued. The access token is self-contained and verified without a database lookup, so it stays valid for the
remainder of its 15 minutes. `AuthenticationFlowIT` asserts this so it cannot change unnoticed.

### Deny by default — a change from the previous contract

The security chain now denies every route that is not explicitly permitted. Two consequences differ from
the pre-authentication behaviour:

| Request | Was | Is now |
|---|---|---|
| Unauthenticated request to an unknown path under `/api/v1` | 404 `not-found` | **401 `unauthorized`** |
| Any request carrying a malformed or expired bearer token, even to a public route | (not possible) | **401 `unauthorized`** |

The first is deliberate: an unauthenticated caller must not be able to map which paths exist. An
*authenticated* request to an unknown path still returns 404 `not-found`. The second is also deliberate — a
bad token is rejected rather than silently treated as anonymous, so clock skew or a rotated key surfaces as
one clear rejection instead of a confusing success.

The publicly reachable routes are exactly: `GET /api/v1/meta`, the three `POST /api/v1/auth/*` routes above,
`GET /actuator/health`, and — under the `local` profile only — `/v3/api-docs` and `/swagger-ui/**`.
`SecurityConfigurationIT` asserts each one individually, so widening the list requires editing a test.

`unauthorized` and `forbidden` are no longer reserved; `unauthorized` is now reachable. `forbidden` remains
unreachable in practice because MVP 0 has one role and ownership failures return `not-found` — see below.

### Error detail language

Problem `detail` text is English, matching the rest of the taxonomy. The Vietnamese user-facing copy for
each state — expired session, revoked session, cancelled Google handoff, cross-user denial — is specified by
the accepted identity design and is mapped by the client from the `type` URN. Do not parse or display
`detail` as user copy; switch on `type`.

### CORS and credentials

`allowCredentials(false)` stays correct and is not an oversight now that authentication exists. The access
token travels in an `Authorization` header the client sets explicitly, not in a cookie, so no credential is
attached by the browser and none needs to be allowed. That is also why CSRF protection is disabled: there is
no cookie-borne credential to forge. If a cookie session is ever introduced, both decisions must be revisited
together.

## Ownership enforcement — implemented

Every user-owned query and mutation enforces ownership server-side, and the viewer's identity comes only
from the verified access token.

- A missing or invalid principal returns `urn:rikkaus:problem:unauthorized` (401).
- An authenticated principal without rights returns `urn:rikkaus:problem:forbidden` (403). Unreachable in
  MVP 0, which has a single role.
- **A resource owned by another user returns `not-found`, not `forbidden`** — and the response body is
  byte-identical to that of a record that genuinely does not exist. A 403 would confirm the record exists,
  turning any list of identifiers into an enumeration oracle.

Two mechanisms keep this true as slices are added:

- `OwnershipGuard` is the single enforcement point. It takes the detail text from the taxonomy rather than
  from the caller, so no slice can write a helpful-sounding message that reintroduces the disclosure.
- An ArchUnit rule fails the build if any controller method binds a user identifier from a `@PathVariable`
  or `@RequestParam`. `ARCHITECTURE_TECHNOLOGY_DECISIONS.md` requires never trusting a client-supplied
  `userId`; this makes that a compile-time guarantee rather than a review habit.

A later slice inherits the whole rule set by extending `OwnershipContract`, which asserts owner-reads-200,
non-owner-reads-404, non-owner-writes-404, anonymous-401, and the byte-identical-body property.

**What no static rule can guarantee** is that a slice calls `OwnershipGuard` at all. The contract test makes
doing so cheap; it cannot make omitting it impossible.

## What is not here

Named explicitly, so nothing above is read as a guarantee it does not make.

- CI pipeline, coverage thresholds, formatting enforcement, and JSON log encoding. The console log
  pattern carries the correlation identifier but is readable text, not structured JSON.
- Pagination conventions, rate limiting and idempotency keys.
- Domain resources. Assets, liabilities, cash flow, goals and insights are later work.
- Account deletion, profile editing, a second identity provider, household or shared access, and roles.
- **`/actuator/health` is absent from the published contract**, by design, because the contract is
  scoped to `/api/v1/**`. The endpoint works and is CORS-enabled; it simply is not described there.
- **Contract consumption is hand-written this iteration.** `apps/mobile` has no OpenAPI codegen
  dependency and no generate script, so this document and `openapi.json` are review and documentation
  artifacts, not a generation input. If codegen is wanted, it belongs to the frontend task's scope.
