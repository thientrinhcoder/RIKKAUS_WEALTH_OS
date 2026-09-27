# Acceptance evidence — issue #39, API contract, errors and typed client foundation

Recorded 2026-09-27. Every row below names the command that was run and what it actually returned. A
criterion is marked satisfied only when its own command passed. Three are recorded as **partial** with
reasons, and one is recorded as **not met**.

Environment: JDK 25.0.4.1 (Homebrew `openjdk@25`), Maven 3.9.11, Docker Engine 27.3.1, PostgreSQL
18.6 via `postgres:18.6-alpine3.24`. Branch `thientrinhcoder/feat/issue-39-api-contract-errors-foundation`.

## Test and build gates

| Criterion | Command | Observed |
|---|---|---|
| `test` passes without a database or Docker | `./services/api/mvnw -f services/api/pom.xml clean test` | Exit 0. `Tests run: 29, Failures: 0, Errors: 0, Skipped: 0`. No `*IT` class ran and no container was started. |
| `verify` passes with Testcontainers PostgreSQL, applying `V1` through Flyway | `./services/api/mvnw -f services/api/pom.xml clean verify` | Exit 0. Surefire `Tests run: 29`, Failsafe `Tests run: 17`, both with zero failures. The log records `Successfully applied 1 migration to schema "public", now at version v1` against the container. |
| Exactly one container for the whole run | `docker ps --filter ancestor=postgres:18.6-alpine3.24` sampled twice during `verify` | The same container id throughout, alongside the unrelated Compose service on 5432. The singleton pattern holds. |

## The published contract

| Criterion | Command | Observed |
|---|---|---|
| Generated from the running application during the build and committed | `./services/api/mvnw -f services/api/pom.xml verify -Dopenapi.update=true` | Wrote `services/api/openapi/openapi.json`, 153 lines. `info` is `{"title":"Rikkaus Wealth OS API","version":"v1"}` with no build metadata. |
| An undocumented API change fails the build | Added a `driftProbe` field to `MetaResponse`, ran `verify` | **Observed failing**, with the intended message: "The API changed but openapi/openapi.json was not regenerated. Run ./services/api/mvnw -f services/api/pom.xml verify -Dopenapi.update=true, review the diff, and notify the frontend task before merging." Reverting returned the build to green. |
| The generated document is byte-stable | Two consecutive `verify` runs, no code change | `git diff --stat services/api/openapi/openapi.json` empty after both. Stability was established **before** the deliberate-break check. |
| Contains exactly the production paths | `OpenApiContractIT.publishesOnlyTheProductionApiSurface` | `paths` equals `["/api/v1/meta"]`, asserted against an explicit allow-list. The test fixture controller, deliberately outside the component-scan root and off `/api/v1`, did not leak. |
| The contract is reviewable, not git-ignored | `git check-ignore -v services/api/openapi/openapi.json` | Exit 1 with no output, so the file is tracked. |

## Error contract

| Criterion | Command | Observed |
|---|---|---|
| Every error under `/api/v1/**` is `application/problem+json` with a `urn:rikkaus:problem:` type and a `correlationId` | `curl -i -s http://localhost:8080/api/v1/does-not-exist` | `{"detail":"Resource not found.","instance":"/api/v1/does-not-exist","status":404,"title":"Resource not found","type":"urn:rikkaus:problem:not-found","correlationId":"01e360f3-b213-4583-9b9e-d4a2883810ec"}` |
| Framework-raised statuses too | `curl -i -s -X POST http://localhost:8080/api/v1/meta` | `{"detail":"Method 'POST' is not supported.","instance":"/api/v1/meta","status":405,"title":"Method not allowed","type":"urn:rikkaus:problem:method-not-allowed","correlationId":"abfcdd2f-..."}` |
| 400, 404, 405, 406, 415 and the domain and last-resort paths | `ApiExceptionHandlerTest` | 9 assertion groups pass. Each checks the media type, a project URN type, a non-empty `title`, `detail` and `instance`, and that a marker sent as the offending input does not appear in `detail` or `title`. |
| Invalid input is covered by tests asserting the per-field `errors` member, not just the status | `ApiExceptionHandlerTest.validationFailureNamesEveryFailingField` and `ProblemDetailsContractIT.aValidationFailureCarriesThePerFieldErrorsMember` | `$.errors[0].field` is `name` and `$.errors[0].message` is `must not be blank`, asserted in both the slice and the full stack. |
| No error body leaks internals | `ApiExceptionHandlerTest.anUnexpectedFailureLeaksNothingFromItsMessage` | The 500 body contains neither the seeded message `internal detail that must not leak` nor the string `IllegalStateException`. Verified in the full stack as well. |
| The published error schema matches actual behaviour | `ProblemDetailsContractIT.everyRequiredMemberThePublishedContractDocumentsIsActuallySent` | Every member the committed schema marks required is present in a real error response. This is a separate check because the drift comparison derives both sides from the same hand-written schema and could not catch that class of defect. |

## Correlation identifier

| Criterion | Command | Observed |
|---|---|---|
| A request with no header receives a generated one | `curl -s -D - -o /dev/null http://localhost:8080/api/v1/meta` | `X-Correlation-Id: 62741faa-7ea2-4e5e-83c0-6a5f7a195624` |
| A well-formed value is echoed | same with `-H 'X-Correlation-Id: abcdef12-3456-7890'` | `X-Correlation-Id: abcdef12-3456-7890` |
| A malformed value is rejected and replaced, and the request still succeeds | same with `-H 'X-Correlation-Id: bad value with spaces'` | HTTP 200 and `X-Correlation-Id: 59120566-d021-46a2-8981-4db1bda0c08e` |
| The value appears in every log line for that request | Re-ran with `--logging.level.org.springframework.web=DEBUG` and two tagged requests | `trace-me-0001` appears on all five lines from `DispatcherServlet - GET "/api/v1/meta"` to `Completed 200 OK`; `trace-me-0002` on all eight lines of a 404 including the handler selection and the `ProblemDetail` write. One line carries no identifier and is named below. |
| CRLF and oversized input are rejected | `CorrelationIdFilterTest` | 13 tests pass, including `rejectsInboundValueContainingLineBreaksToPreventLogForging`, the oversized and too-short cases, MDC cleanup on success and on throw, the ERROR-dispatch reuse case, and the taxonomy-preservation guard. |
| No credential reaches the log | grep over the captured run log | Zero matches for the Compose password; zero case-insensitive matches for password, secret or token. |

**One log line legitimately carries no identifier**, recorded rather than glossed:
`19:00:42.114 INFO [no-correlation-id] o.s.web.servlet.DispatcherServlet - Completed initialization in
1 ms`. That is Spring's lazy servlet initialization, triggered by the first request but emitted by
container bootstrap outside the filter chain. Every application line from `19:00:42.147` onward is
tagged.

## Cross-origin access, verified in a real browser

A passing slice test cannot prove browser behaviour, so this was done from the actual Expo web page.

The Expo origin was **read, not assumed**: `npx expo start --web` in `apps/mobile` printed
`Waiting on http://localhost:8081`, confirmed by `lsof -nP -iTCP:8081 -sTCP:LISTEN`.
`apps/mobile/package.json` pins no port, so the value is evidence rather than Expo's documented default.

From `http://localhost:8081` with the API on the `local` profile:

- `fetch('http://localhost:8080/api/v1/meta')` returned 200, and JavaScript read
  `X-Correlation-Id: c7a562bb-28c1-420f-af5a-91ea2438043c`.
- `fetch('http://localhost:8080/actuator/health')` returned 200 with `{"status":"UP"}`, so the
  already-shipped health screen works from a browser.
- A cross-origin 404 returned a header id and a body `correlationId` that were byte-identical
  (`c0a4ebae-5f4e-4ed9-8d5e-a7336029fe1a`) together with `type: urn:rikkaus:problem:not-found`.

With no `API_ALLOWED_ORIGINS` and no profile, **neither** endpoint returns any `Access-Control-*`
header, and both still answer ordinary requests with 200. A request from `https://evil.example` is
rejected with HTTP 403.

`/actuator/health` does not expose `X-Correlation-Id` to script. That is the deliberate narrower
posture for that mapping, not a defect.

## Contract endpoint gating

| Command | With `local` profile | Without it |
|---|---|---|
| `curl -o /dev/null -w '%{http_code}' /v3/api-docs` | 200 | 404 |
| `curl -o /dev/null -w '%{http_code}' /swagger-ui/index.html` | 200 | 404 |
| `curl -o /dev/null -w '%{http_code}' /api/v1/meta` | 200 | 200 |

## Architecture rules

| Criterion | Observed |
|---|---|
| Feature-slice isolation, controller placement and exact-decimal rules fail on violation | **All three observed failing.** Two probe packages under `com.rikkaus.wealth` where one returned and constructed the other's type, a `@RestController` outside any `api` package, and a `public double amount` field. Each rule reported exactly one violation naming the offending element. Probes deleted; `clean test` green again. |

## Flyway — a defect in previously shipped code, now repaired

Recorded as its own entry because it changes runtime behaviour and because issue #35 closed with its
Flyway acceptance box unchecked. A future reader should not have to re-derive why.

`FlywayAutoConfiguration` lives in `org.springframework.boot:spring-boot-flyway`, a module Spring Boot 4
split out and which no declared starter brings in. Every `spring.flyway.*` setting was therefore inert
and **Flyway had never run in this service**. Nobody noticed because with no `@Entity` in the codebase
`ddl-auto: validate` had nothing to check, so the application started cleanly while silently skipping
the migration.

Before, on a fresh volume with the unmodified pom:

```
$ grep -ci flyway <startup log>          → 0
$ psql -c "\dt flyway_schema_history"    → Did not find any tables named "flyway_schema_history".
$ psql -c "\dn wealth"                   → (0 rows)
```

Startup nonetheless reported `Started RikkausWealthApplication in 1.149 seconds`.

After adding that one compile-scope dependency, same volume, same application:

```
$ psql -c "SELECT installed_rank, version, description, type, success FROM flyway_schema_history"
 installed_rank | version | description | type | success
              1 | 1       | baseline    | SQL  | t
$ psql -c "\dn wealth"                   → wealth | rikkaus
```

`V1__baseline.sql` and the Compose service were not modified. The baseline only creates a schema, so
there is no destructive step; on a developer volume predating the change the migration simply applies.

A related finding, also only discoverable by building: `spring-boot-resttestclient` declares
`spring-boot-restclient` as optional, so without declaring the latter every `@SpringBootTest` context
carrying `@AutoConfigureTestRestTemplate` failed to refresh with `NoClassDefFoundError:
org/springframework/boot/restclient/RestTemplateBuilder`.

## Sibling notification

Posted to issue #38 on the Product Owner's explicit go-ahead, having been held back initially because it
is a public comment on another task's issue:
https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/38#issuecomment-5856688886

It carries the contract path, the regeneration command with the CI prohibitions, the full taxonomy table
including the two reserved codes, the `ProblemDetail` shape with both extension members, the observed
correlation-header behaviour, and `API_ALLOWED_ORIGINS`. The three limitations are stated up front rather
than buried: the contract omits `/actuator/health`, it has no request-accepting endpoint and therefore no
validation example, and `apps/mobile` has no codegen so the contract is documentation rather than a
generation input. It asks #38 directly whether it wants a codegen step, and records that the answer
belongs to #38's scope. It also states that the ownership limb of "API Ready" is unmet, so #38 is
unblocked but #41 is not.

Per this issue's own coordination rule, any later change to the published contract must be accompanied by
updated examples and a further comment on that issue before merge.

## Criteria not fully satisfied

### Structured logging — partial

The correlation identifier appears on every log line for a request, verified above. But the pattern is
readable console text, not JSON. `ARCHITECTURE_TECHNOLOGY_DECISIONS.md:239` calls for structured logs,
and a JSON encoder is a deployment concern belonging to the CI/CD task, which owns the pipeline that
would ship them. **Do not read this criterion as fully met.**

### Data ownership — shape published, enforcement not built

Delivered as a documented contract shape with enforcement deferred to the identity task by explicit
Product Owner decision during planning, not silently dropped.
[`docs/api-contract-conventions.md`](../../docs/api-contract-conventions.md) documents the reserved
`urn:rikkaus:problem:unauthorized` and `urn:rikkaus:problem:forbidden` types, and the convention that a
resource owned by another user returns `not-found` rather than `forbidden` so the API does not disclose
that another user's record exists. Both codes are in the published contract's `type` enumeration.

`ARCHITECTURE_TECHNOLOGY_DECISIONS.md:140` — every user-owned query and mutation must enforce ownership
on the server, never trusting a client-supplied `userId` — is satisfied by the identity task, **not
here**.

### "API Ready" — three limbs of four

`RIKKAUS_WEALTH_OS_MVP_DELIVERY_BACKLOG.md:30` defines API Ready as: OpenAPI/schema, error contract,
`quyền sở hữu dữ liệu`, and a working fixture or endpoint.

| Limb | Status |
|---|---|
| OpenAPI/schema chốt | **Met.** Generated from the application, committed, drift-enforced. |
| Error contract | **Met.** RFC 9457 across every status, with the taxonomy documented and tested. |
| Quyền sở hữu dữ liệu | **NOT MET.** Contract shape only. No enforcement exists. |
| Fixture hoặc endpoint chạy được | **Met.** `GET /api/v1/meta` returns real runtime data. |

This matters beyond bookkeeping. `RIKKAUS_WEALTH_OS_MVP_DELIVERY_BACKLOG.md:56` gates issues #38, #41
and #44 on "#39 API Ready", and **#41 is the frontend identity and ownership task**. It must not start
building an ownership UX against a backend that has none. #38, the typed client, is genuinely unblocked.

## Deliberately not built

So a later reader does not mistake absence for oversight: no authentication, no session handling, no
ownership enforcement, no domain resources or tables, no CI pipeline, no coverage or formatting gates,
no JSON log encoding, no pagination, rate limiting or idempotency keys, no Swagger UI outside local
development, and **no fabricated data anywhere**. `GET /api/v1/meta` returns real runtime values; the
error-probe endpoints are test-only, outside the component-scan root and off `/api/v1`.

## Note for whoever wires CI

`verify` fails closed without a Docker daemon, by design. The pipeline must provide Docker.
**`-DskipITs` is forbidden** because it disables the contract drift gate along with the integration
tests, and **`-Dopenapi.update=true` must never appear in a CI command** because it converts that gate
into a silent rewrite. Until CI exists, the drift gate is an honest manual gate, stated as such rather
than claimed as automated.
