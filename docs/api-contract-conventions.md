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

`unauthorized` and `forbidden` are **reserved and currently unreachable**. They are declared now so the
authentication work can throw them without renegotiating this contract, and so your error classifier
can handle them before that lands.

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

## Ownership contract — shape agreed, enforcement not yet built

This section exists so the authentication work implements against an agreed interface instead of
inventing one. **No ownership enforcement exists today.**

- A missing or invalid principal returns `urn:rikkaus:problem:unauthorized` (401).
- An authenticated principal without rights returns `urn:rikkaus:problem:forbidden` (403).
- **A resource owned by another user returns `not-found`, not `forbidden`**, so the API does not
  disclose that another user's record exists.

`ARCHITECTURE_TECHNOLOGY_DECISIONS.md:140` requires that every user-owned query and mutation enforce
ownership on the server and never trust a client-supplied `userId`. That requirement is satisfied by
the authentication task, not by this foundation.

Because authentication is absent, **this API must not be exposed beyond local development** until that
work lands.

## What is not here

Named explicitly, so nothing above is read as a guarantee it does not make.

- Authentication, session handling and ownership enforcement.
- CI pipeline, coverage thresholds, formatting enforcement, and JSON log encoding. The console log
  pattern carries the correlation identifier but is readable text, not structured JSON.
- Pagination conventions, rate limiting and idempotency keys.
- Domain resources. Assets, liabilities, cash flow, goals and insights are later work.
- **`/actuator/health` is absent from the published contract**, by design, because the contract is
  scoped to `/api/v1/**`. The endpoint works and is CORS-enabled; it simply is not described there.
- **The contract contains no request-accepting endpoint**, so it carries no 400 validation example and
  no populated `errors` sample. That is the cost of keeping test scaffolding out of a published
  contract. The `errors` member is still documented in the schema, so its shape is agreed before the
  first real request body arrives.
- **Contract consumption is hand-written this iteration.** `apps/mobile` has no OpenAPI codegen
  dependency and no generate script, so this document and `openapi.json` are review and documentation
  artifacts, not a generation input. If codegen is wanted, it belongs to the frontend task's scope.
