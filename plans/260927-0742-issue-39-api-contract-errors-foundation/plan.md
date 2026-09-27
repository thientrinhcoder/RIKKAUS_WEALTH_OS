---
title: "API contract, errors and typed client foundation (#39)"
description: "Establish the /api/v1 surface, a build-published OpenAPI contract, an RFC 9457 Problem Details taxonomy, correlation IDs in structured logs, and the contract/integration test conventions the rest of the backend will inherit."
status: done
priority: P1
effort: 50h
issue: 39
branch: thientrinhcoder/feat/issue-39-api-contract-errors-foundation
tags: [backend, api, openapi, errors, observability, testing, mvp-0]
blockedBy: []
blocks: []
created: 2026-09-27
---

# API contract, errors and typed client foundation (#39)

## Overview

Issue [#39](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/39) is the Backend
deliverable of Feature [#8](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/8). Its
feature-level blocker [#6](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/6) is
closed, so this work is unblocked. It establishes the five contracts every later backend slice
inherits: the versioned `/api/v1` surface, a published OpenAPI 3 contract, RFC 9457 Problem
Details, correlation IDs, and the contract/integration test conventions.

The starting point is genuinely empty. `services/api/src` contains four files —
`RikkausWealthApplication.java`, `package-info.java`, `application.yml` and
`db/migration/V1__baseline.sql` — and `V1` creates only the `wealth` schema namespace. There is no
`src/test` directory, no test dependency of any kind in `services/api/pom.xml`, and no controller.
A repo-wide search found zero occurrences of `ProblemDetail`, `problem+json`, `springdoc`,
`v3/api-docs`, `traceparent` or `X-Request-Id` in code; every one of those terms appears only in
`ARCHITECTURE_TECHNOLOGY_DECISIONS.md` as an unimplemented decision. Nothing here is being
rebuilt.

The Frontend sibling [#38](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/38) is
further along than the backend and constrains the shape of what this task publishes. It already has
a working consumer pattern in `apps/mobile/src/features/health/` — a `fetch` wrapper
(`health-client.ts`), a Zod runtime schema (`health-schema.ts`) and a TanStack Query hook
(`use-health-query.ts`), with three test files under `apps/mobile/__tests__/`. It reads
`EXPO_PUBLIC_API_BASE_URL` (default `http://localhost:8080`). So this task's job is not to invent a
client contract but to publish one that a hand-written fetch-plus-Zod client can consume and that
mock fixtures can be generated from.

## Outcome

`/api/v1` exists as a real, documented, versioned HTTP surface. Its OpenAPI 3 contract is generated
from the running application during `./mvnw verify` and committed at
`services/api/openapi/openapi.json`, so contract drift fails the build and contract changes are
visible in PR review. Every error response — framework-raised and domain-raised — is
`application/problem+json` conforming to RFC 9457, carrying a stable `type` URN and the same
correlation ID that appears in the server's structured logs. A Testcontainers-backed integration
test harness exists, and the feature-slice boundary rule is mechanically enforced rather than
merely documented.

## Decisions taken during planning

Four forks were resolved with the Product Owner before drafting. They are recorded here as the
authority for the phases.

| Decision | Choice | Why |
|---|---|---|
| Production surface under `/api/v1` | Add one real non-domain resource, `GET /api/v1/meta` | With no controller, `springdoc.paths-to-match: /api/v1/**` publishes an empty `paths` object, which gives #38 nothing to generate fixtures from until #42 lands. A meta resource returning API version, build version and server time is real data, not a placeholder. |
| RFC 9457 `type` URI scheme | `urn:rikkaus:problem:<code>` | The repository references no owned domain anywhere, so an `https://` type URI would invent a hostname that resolves to nothing. RFC 9457 does not require the URI to dereference. Changing this later is a breaking change for every client, so it is decided now. |
| Quality-gate tooling | ArchUnit in this task; JaCoCo and Spotless deferred to #47 | ArchUnit protects the feature-slice boundary rule *this* plan creates, so it is a test convention and belongs here. Coverage thresholds and formatting enforcement are CI gates and [#47](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/47) owns those. |
| Contract publication | Commit `services/api/openapi/openapi.json` and assert no drift | #39 requires contract changes to be updated with examples and announced to siblings before merge. That only works if the contract is reviewable in a PR diff. A `target/`-only artifact makes contract changes invisible in review. |

One further decision needed no escalation. Unit and slice tests run in Surefire with no database;
Testcontainers-backed tests are named `*IT` and run in Failsafe during `verify`. This keeps
`./mvnw test` fast and Docker-free while `./mvnw verify` exercises the real Flyway and PostgreSQL
path.

Three more decisions were taken after red-team review surfaced evidence the first draft did not have.

| Decision | Choice | Why |
|---|---|---|
| The missing Flyway autoconfiguration | Repair it in Phase 1 | `FlywayAutoConfiguration` lives in `spring-boot-flyway`, which no declared starter brings in, so `spring.flyway.*` is inert and **Flyway has never run in this service**. `spring-boot-autoconfigure-4.1.1.jar` has zero Flyway entries, and issue #35's "Flyway applied `V1`" acceptance box is still unchecked. #39 cannot honestly claim a Flyway-and-Testcontainers baseline on a Flyway that does not run. |
| #39's ownership acceptance criterion | Publish the contract shape here; enforcement in #42 | The criterion is requested scope and was silently dropped by the first draft. This plan now documents the reserved `unauthorized` and `forbidden` types and the not-found-rather-than-forbidden convention, and Phase 6 states which limbs of "API Ready" are met so #41 is not unblocked on a false signal. |
| CORS scope | Cover `/api/v1/**` and `/actuator/health`, fail-closed default | Not unrequested scope after all: `README.md:137` already documents CORS as a precondition and no implementation exists. The first draft's `/api/v1/**`-only mapping left the one shipped browser consumer, which calls `/actuator/health`, still broken. |

## Constraints

- Pin `springdoc-openapi-starter-webmvc-ui` **3.1.1**. Verified against Maven Central: it is the
  current `<release>`, published 2026-09-06, and its aggregator POM declares
  `spring-boot-starter-parent` **4.1.0** as its build parent, the same Boot 4.1 line this project
  uses. The `2.x` line (up to 2.9.1) targets Boot 3.x and must not be used.
- Take every test-stack version from the Boot 4.1.1 BOM rather than pinning by hand. Verified in
  `spring-boot-dependencies-4.1.1.pom`: junit-jupiter 6.0.3, AssertJ 3.27.7, Mockito 5.23.0,
  Byte Buddy 1.18.11, Flyway 12.4.0, and Testcontainers 2.0.5 imported as `testcontainers-bom`.
  ArchUnit is absent from the BOM and must be pinned explicitly at `archunit-junit5` **1.5.1**.
- Use the **Testcontainers 2.x module names**: `org.testcontainers:testcontainers-postgresql` and
  `org.testcontainers:testcontainers-junit-jupiter`. The 1.x names `postgresql` and `junit-jupiter`
  have no 2.x release and are absent from `testcontainers-bom:2.0.5`, so they resolve to no managed
  version at all.
- Declare the Boot 4 modules that Spring Boot's modularization split out and that no declared starter
  brings in: `org.springframework.boot:spring-boot-flyway` at compile scope, and
  `spring-boot-webmvc-test`, `spring-boot-resttestclient` plus `spring-boot-restclient` at test scope.
  `spring-boot-starter-test` provides neither `@WebMvcTest` nor `TestRestTemplate` in this line, and
  `spring-boot-resttestclient` declares `spring-boot-restclient` only as optional, so without it every
  `@SpringBootTest` context carrying `@AutoConfigureTestRestTemplate` fails to refresh with
  `NoClassDefFoundError: org/springframework/boot/restclient/RestTemplateBuilder`. Phase 1 carries the
  evidence table; the `spring-boot-restclient` requirement was found during Phase 1 implementation and
  is recorded in that phase's implementation notes.
- Do not set `spring.mvc.problemdetails.enabled`. Boot's
  `WebMvcAutoConfiguration$ProblemDetailsErrorHandlingConfiguration` is
  `@ConditionalOnMissingBean(ResponseEntityExceptionHandler.class)`, so the advice this plan registers
  makes Boot's handler back off and the property becomes dead configuration.
- Do not enable Spring Framework 7's native `spring.mvc.apiversion` header/path-segment versioning.
  springdoc issue [#3163](https://github.com/springdoc/springdoc-openapi/issues/3163) reports it
  returns HTTP 400 from both `/v3/api-docs` and `/swagger-ui.html` on Boot 4.x. This plan uses a
  static `/api/v1` path prefix, which is unaffected, and must stay that way.
- Do not use `springdoc-openapi-maven-plugin`. Its latest release (1.5, 2025-05-04) predates Boot 4
  GA, and it boots the packaged application out-of-process via `spring-boot-maven-plugin`
  `start`/`stop`, which needs a reachable PostgreSQL at build time with no JUnit or Testcontainers
  lifecycle to provide one.
- Keep money and exchange rates on exact decimal types. No `float` or `double` reaches a field that
  represents money, per `ARCHITECTURE_TECHNOLOGY_DECISIONS.md:141`.
- Never log passwords, tokens or financial payloads, per `ARCHITECTURE_TECHNOLOGY_DECISIONS.md:144`
  and `:239`. The correlation ID is the only request-scoped value added to the log context.
- CORS uses an explicit environment-specific allowlist with a fail-closed empty default, per
  `ARCHITECTURE_TECHNOLOGY_DECISIONS.md:235`.
- Flyway stays the only schema mechanism. This task adds no migration; `V1__baseline.sql` is
  untouched and `spring.jpa.hibernate.ddl-auto` stays `validate`. Phase 1 does add the missing
  `spring-boot-flyway` module, without which none of that is currently true — see below.
- Preserve unrelated work in the tree. Modify only the files in the inventory below.

## Non-goals

- Authentication, session handling and ownership enforcement. Those are
  [#42](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/42). This plan adds no Spring
  Security dependency. It does reserve the `401`, `403` and `404`-on-unowned problem types in the
  taxonomy so #42 extends the contract instead of redesigning it.
- Domain resources, entities, repositories and tables. Assets, liabilities, cash flow, goals and
  insights belong to MVP 1A onward.
- The Frontend typed client, interceptors, Zod schemas and mock fixtures. Those are #38. This task
  publishes the contract they consume and nothing more.
- CI/CD pipeline, JaCoCo coverage gates and Spotless formatting enforcement. Those are #47.
- Rate limiting, pagination conventions, idempotency keys and a distributed-tracing backend. None
  is named in #39 and none is needed by a single service talking directly to PostgreSQL.
- Swagger UI as a production surface. It is disabled outside local development.

## Acceptance criteria

These mirror #39 and its parent #8. They are verified in Phase 6 against real command output, not
assumed.

- [x] `./mvnw test` passes with unit and slice tests and requires no database or Docker.
- [x] `./mvnw verify` passes with Testcontainers PostgreSQL integration tests, applying
      `V1__baseline.sql` through Flyway in the container.
- [x] The OpenAPI contract is generated from the running application during the build and committed
      at `services/api/openapi/openapi.json`; an undocumented change to the API fails the build.
- [x] Every error response under `/api/v1/**`, including framework-raised 400, 404, 405 and 415 and
      domain-raised errors, is `application/problem+json` with a `type` of
      `urn:rikkaus:problem:<code>` and a `correlationId` extension member.
- [x] A request without `X-Correlation-Id` receives a generated one on the response; a request with
      a well-formed one has it echoed; a malformed or oversized one is rejected and replaced. The
      value appears in every log line for that request.
- [x] Invalid-input cases are covered by tests that assert the per-field `errors` extension member,
      not just the status code.
- [x] The ownership contract shape is published in the OpenAPI schema and documented for #42: the
      reserved `unauthorized` and `forbidden` types and the not-found-rather-than-forbidden convention.
      Enforcement is #42's and the acceptance report says so explicitly.
- [x] Flyway demonstrably applies `V1__baseline.sql`, both at runtime and inside the Testcontainers
      database, with before-and-after evidence captured.
- [x] The feature-slice boundary rule and the controller-placement rule are enforced by ArchUnit tests
      that fail on violation, each observed failing on a deliberate violation.
- [x] The published contract contains exactly the production paths, asserted by an allow-list so a test
      controller cannot leak into it.
- [x] Contract conventions are documented for sibling teams, and #38 is notified with the contract
      path and the error taxonomy before merge.
- [x] No Phase 2 behavior and no fabricated data were added to satisfy any check above.

## Phases

| # | Phase | Status | Depends on |
|---|-------|--------|------------|
| 1 | [Test harness and integration baseline](./phase-01-test-harness-and-integration-baseline.md) | Done | None |
| 2 | [Versioned API surface and meta resource](./phase-02-versioned-api-surface-and-meta-resource.md) | Done | 1 |
| 3 | [RFC 9457 Problem Details taxonomy](./phase-03-rfc-9457-problem-details-taxonomy.md) | Done | 1, 2 |
| 4 | [Correlation ID and structured logging](./phase-04-correlation-id-and-structured-logging.md) | Done | 1, 3 |
| 5 | [OpenAPI generation and build publication](./phase-05-openapi-generation-and-build-publication.md) | Done | 1, 2, 3, 4 |
| 6 | [Contract conventions and acceptance gate](./phase-06-contract-conventions-and-acceptance-gate.md) | Done | 1–5 |

## Dependency graph

Phase 1 blocks everything because nothing in this plan can be verified before a test harness
exists. Phase 3 needs Phase 2's controller to have a real endpoint whose error paths it can assert.
Phase 3 already writes the `correlationId` extension member by reading MDC, and Phase 4 is what
puts a value there, so Phase 4 follows Phase 3 rather than running beside it. Phase 5 needs both a
documented endpoint and a settled error taxonomy, otherwise the committed contract churns twice.

```text
Phase 1 (test harness + Flyway repair, blocking)
   └── Phase 2 (/api/v1 + meta resource + CORS)
         └── Phase 3 (Problem Details taxonomy)
               └── Phase 4 (correlation ID populates the member Phase 3 already writes)
                     └── Phase 5 (OpenAPI generation + committed contract)
                           └── Phase 6 (docs, sibling notification, acceptance evidence)
```

**This chain is strictly sequential.** The first draft claimed Phases 4 and 5 were parallel-safe because
their file ownership is disjoint, and red-team review showed that is false at the Spring context level
and at the contract level. Phase 5 documents the `correlationId` member and the `X-Correlation-Id`
header, both Phase 4 artifacts, and because Phase 5's `ProblemDetail` schema is hand-written rather than
inferred, the drift assertion compares generated against committed and never contract against behaviour
— so a Phase 5 that lands first would publish a contract promising a member the server does not send,
with every build green and #38's parser rejecting every error response. Phase 5's `dependencies` field
now names Phase 4, and Phase 5 additionally adds a contract-versus-behaviour assertion so the blind spot
is closed rather than merely scheduled around.

## File ownership map

Ownership is disjoint so the phases can merge without conflict. Two files are touched by more than
one phase and are arbitrated below.

| Phase | Owns |
|---|---|
| 1 | `services/api/src/test/java/com/rikkaus/wealth/support/**`, `services/api/src/test/java/com/rikkaus/wealth/ArchitectureRulesTest.java`, `services/api/src/test/java/com/rikkaus/wealth/ApplicationContextIT.java`, `services/api/src/test/resources/**` |
| 2 | `services/api/src/main/java/com/rikkaus/wealth/shared/api/**`, `services/api/src/test/java/com/rikkaus/wealth/shared/api/**`, `services/api/src/main/resources/application-local.yml`, `.env.example` |
| 3 | `services/api/src/main/java/com/rikkaus/wealth/shared/error/**`, `services/api/src/test/java/com/rikkaus/wealth/shared/error/**`, `services/api/src/test/java/com/rikkaus/testfixtures/**` |
| 4 | `services/api/src/main/java/com/rikkaus/wealth/shared/observability/**`, `services/api/src/test/java/com/rikkaus/wealth/shared/observability/**` |
| 5 | `services/api/src/main/java/com/rikkaus/wealth/shared/openapi/**`, `services/api/src/test/java/com/rikkaus/wealth/shared/openapi/**`, `services/api/openapi/openapi.json` |
| 6 | `README.md`, `docs/api-contract-conventions.md`, `.gitignore`, `plans/reports/pm-260927-issue-39-acceptance.md` |

Phase 2 owns the one README edit outside Phase 6 — adding `API_ALLOWED_ORIGINS` to the backend
environment section — because shipping an undocumented security-relevant variable for four phases is
worse than the ownership exception.

**Arbitration 1 — `services/api/pom.xml`.** Three phases add dependencies. Because the dependency
graph is strictly sequential, no two phases edit the file concurrently: Phase 1 adds
`spring-boot-flyway`, the seven test-scope dependencies and the Failsafe plugin; Phase 2 adds the
`build-info` execution; Phase 5 adds springdoc. Phases 3, 4 and 6 add nothing. Each phase appends only
its own block, and any phase that finds an unexpected diff stops and reconciles rather than overwriting.

**Arbitration 2 — `services/api/src/main/resources/application.yml`.** Phase 2 adds
`rikkaus.api.allowed-origins` and the Jackson date-format key, Phase 4 adds `logging.pattern`, Phase 5
adds `springdoc`. Phase 3 adds nothing, since `spring.mvc.problemdetails` is deliberately not set. These
are disjoint top-level keys and the phases are sequential, so each appends its own and touches no other.
`application-local.yml` follows the same rule: Phase 2 creates it with the CORS origin, Phase 5 appends
the springdoc overrides.

**Arbitration 3 — two files crossing a phase boundary by design.** Two edits deliberately live
outside their file's owning phase, each a single concern that would otherwise force two phases to
edit the same lines.

- `shared/api/MetaController.java` is created by Phase 2 and annotated by Phase 5. Phase 2
  deliberately adds no OpenAPI annotations, so Phase 5 owns every annotation on it and the two
  phases never touch the same lines. `application-local.yml` follows the same split: Phase 2 creates
  it, Phase 5 adds the Swagger UI override.
- `shared/error/ApiExceptionHandler.java` and `shared/error/ProblemDetailWriter.java` are owned by
  Phase 3, which writes the `correlationId` extension member in both using an inlined string key. Phase 4
  makes exactly one edit to each, replacing the literal with `CorrelationId.MDC_KEY`. No behavior changes,
  and the alternative — having Phase 4 add the extension member itself — would put the error body's shape
  in two phases.

**Ownership is not the same as independence.** Red-team review found that disjoint file ownership does
not imply the phases cannot break each other, because `@WebMvcTest` deliberately includes
`WebMvcConfigurer` and `jakarta.servlet.Filter` implementations from anywhere in the context. Phase 4's
filter would therefore have been pulled into Phase 2's and Phase 3's slice tests and dragged in an
unsatisfiable `ProblemDetailWriter` dependency, breaking tests in files Phase 4 never touches. Phase 4
now registers its filter through a `FilterRegistrationBean` in a non-web configuration, and carries an
explicit cross-phase regression gate requiring the earlier phases' tests to pass before it is done.

## Exact file inventory

| File | Action | Intended change |
|---|---|---|
| `services/api/pom.xml` | Modify | Add `spring-boot-flyway` at compile scope; add `spring-boot-starter-test`, `spring-boot-webmvc-test`, `spring-boot-resttestclient`, `spring-boot-restclient`, `spring-boot-testcontainers`, `org.testcontainers:testcontainers-junit-jupiter`, `org.testcontainers:testcontainers-postgresql` and `archunit-junit5:1.5.1` at test scope; add `maven-failsafe-plugin` bound to `integration-test`/`verify`; add `spring-boot-maven-plugin` `build-info` execution; add `springdoc-openapi-starter-webmvc-ui:3.1.1`. |
| `services/api/src/main/resources/application.yml` | Modify | Add the fail-closed `rikkaus.api.allowed-origins` key and the Jackson date-format key; set the console log pattern to include `%X{correlationId}`; add the `springdoc` block scoping the contract to `/api/v1/**` with both `api-docs` and Swagger UI disabled by default. |
| `services/api/src/main/resources/application-local.yml` | Create | Local-only profile: the CORS origin, plus `api-docs` and Swagger UI enabled. |
| `.env.example` | Modify | Document `API_ALLOWED_ORIGINS`. No secret value. |
| `.gitignore` | Modify | Allow-list this plan directory and the acceptance report, following the existing exception pattern, so acceptance evidence reaches review. |
| `.../shared/api/ApiPaths.java` | Create | The single `/api/v1` prefix constant. No string literal `"/api/v1"` appears anywhere else. |
| `.../shared/api/MetaController.java` | Create | `GET /api/v1/meta`; Phase 5 adds its OpenAPI annotations. |
| `.../shared/api/MetaResponse.java` | Create | Response record: `apiVersion`, `application`, `buildVersion`, `serverTime`. |
| `.../shared/api/ClockConfiguration.java` | Create | The `Clock` bean, deliberately not a `WebMvcConfigurer`. |
| `.../shared/api/CorsConfiguration.java` | Create | The environment-specific CORS allowlist over `/api/v1/**` and `/actuator/health`, registering nothing when the origin list is empty. |
| `.../shared/error/ProblemType.java` | Create | The `urn:rikkaus:problem:<code>` taxonomy as an enum carrying code, title and default status. |
| `.../shared/error/ApiException.java` | Create | Base domain exception carrying a `ProblemType` and optional extension members. |
| `.../shared/error/ApiExceptionHandler.java` | Create | `@RestControllerAdvice extends ResponseEntityExceptionHandler`; sets `type`/`title` from `ProblemType`, adds the per-field `errors` member, and adds `correlationId`. |
| `.../shared/error/ProblemDetailWriter.java` | Create | Renders a Problem Details body directly to the response for failures raised before `DispatcherServlet` runs. |
| `.../shared/observability/CorrelationId.java` | Create | Header name, MDC key and the allow-list validator. |
| `.../shared/observability/CorrelationIdFilter.java` | Create | `OncePerRequestFilter` that resolves, publishes, echoes and clears the correlation ID, running on ERROR dispatch too, with `chain.doFilter` outside any catch block. |
| `.../shared/observability/CorrelationIdFilterConfiguration.java` | Create | Registers the filter via `FilterRegistrationBean` for REQUEST and ERROR dispatch, so `@WebMvcTest` does not pull it into slice tests. |
| `.../shared/openapi/OpenApiConfiguration.java` | Create | The `OpenAPI` bean carrying `info` title, version and description, and the shared `ProblemDetail` schema and examples. |
| `services/api/openapi/openapi.json` | Create | The committed, generated contract. Regenerated deliberately, never by hand. |
| `services/api/src/test/java/.../support/AbstractPostgresIntegrationTest.java` | Create | `@SpringBootTest` base class with a `@ServiceConnection` `PostgreSQLContainer` pinned to the Compose image tag. |
| `services/api/src/test/java/com/rikkaus/testfixtures/ProblemFixtureController.java` | Create | Test-only controller that raises each error class. Deliberately outside the `com.rikkaus.wealth` scan root and mapped off `/api/v1`, so it can never be component-scanned into a context or published into the contract. |
| `services/api/src/test/java/.../ApplicationContextIT.java` | Create | Proves the context starts and Flyway applies `V1` against the container. |
| `services/api/src/test/java/.../ArchitectureRulesTest.java` | Create | ArchUnit rules for feature-slice isolation, controller placement and the exact-decimal rule. |
| `services/api/src/test/java/.../shared/api/MetaControllerTest.java` | Create | `@WebMvcTest` slice test for the meta resource. |
| `services/api/src/test/java/.../shared/error/ApiExceptionHandlerTest.java` | Create | `@WebMvcTest` assertions for 400, 404, 405 and 415 problem bodies and the per-field `errors` member. |
| `services/api/src/test/java/.../shared/error/ProblemDetailsContractIT.java` | Create | Full-stack assertions that real requests, not mocks, produce conforming bodies. |
| `services/api/src/test/java/.../shared/observability/CorrelationIdFilterTest.java` | Create | Generation, echo, rejection of malformed input, and MDC cleanup. |
| `services/api/src/test/java/.../shared/openapi/OpenApiContractIT.java` | Create | Generates the contract, compares it with the committed file, and fails on drift. |
| `services/api/src/test/resources/application-test.yml` | Create | Test profile; no credentials, and a deliberately unresolvable datasource host so a `@ServiceConnection` failure cannot silently fall back to a real database. |
| `README.md` | Modify | Replace both scope-limit paragraphs (`:156-158` and `:160-163`), record the Flyway repair, document `API_ALLOWED_ORIGINS`, and document contract regeneration. |
| `docs/api-contract-conventions.md` | Create | The contract siblings consume: path conventions, error taxonomy, correlation header, regeneration command. |
| `plans/reports/pm-260927-issue-39-acceptance.md` | Create | Recorded acceptance evidence. |

No file under `apps/mobile`, no Flyway migration, no CI configuration and no domain package is in
scope.

## Environment prerequisite

**Resolved during implementation (2026-09-27).** Homebrew `openjdk@25` was installed rather than the
Temurin build named below; any JDK 25 satisfies the pom's `java.version`. `./mvnw -q dependency:tree`
resolved every pinned coordinate exactly as predicted, and the full plan was then executed. The
paragraphs below are retained as the record of why the plan was written the way it was.

**There is no JDK on the machine this plan was written on.** `java -version` and
`/usr/libexec/java_home -V` both report no runtime found, so `./mvnw` cannot execute and no
command in this plan was run during planning. Docker is available and healthy (Docker Desktop
27.3.1, Engine 27.3.1), so Testcontainers will work once a JDK exists.

Every version and coordinate claim in this plan comes from reading published Maven Central metadata,
POMs and JAR listings directly, which is authoritative for what will resolve but is not a substitute
for a build. Before Phase 1 edits `pom.xml`, install Eclipse Temurin JDK 25 and confirm resolution:

```bash
cd "services/api" && ./mvnw -q dependency:tree
```

If resolution fails, that is a real blocker to report, not something to work around by relaxing the
pinned versions. Check the coordinate against Phase 1's evidence table first, since a renamed module is
a far more likely cause than a wrong version.

**A methodological note worth keeping, because it is why the first draft was unexecutable.** That draft
verified *versions* against Maven Central and did not verify *coordinates, class locations, or module
boundaries*. Spring Boot 4 split autoconfiguration into per-technology modules and Testcontainers 2
renamed every module, and all five of the false dependency assumptions red-team review found lived in
that gap. None of them needed a JDK to catch — each was findable with `curl` against Maven Central and
`unzip -l`. A resolution gate is the wrong instrument for a missing module that was never declared, so
when this plan pins a coordinate it now cites the artifact listing that proves the coordinate exists.

## Verification commands

All commands run from the repository root.

| Gate | Command |
|---|---|
| Compile | `./services/api/mvnw -f services/api/pom.xml clean compile` |
| Unit and slice tests, no Docker | `./services/api/mvnw -f services/api/pom.xml test` |
| Focused test | `./services/api/mvnw -f services/api/pom.xml test -Dtest=ApiExceptionHandlerTest` |
| Full build with integration tests | `./services/api/mvnw -f services/api/pom.xml verify` |
| Regenerate the contract deliberately | `./services/api/mvnw -f services/api/pom.xml verify -Dopenapi.update=true` |
| Live database for manual checks | `docker compose --env-file .env.example up -d --wait postgres` |
| Live contract check | `curl -s http://localhost:8080/v3/api-docs \| jq '.info, (.paths \| keys)'` |
| Live problem check | `curl -i -s http://localhost:8080/api/v1/does-not-exist` |

## Test strategy

Two tiers, split by whether a database is required.

Surefire runs `*Test` classes. These are unit tests and `@WebMvcTest` slice tests. They must not
require Docker or PostgreSQL, so `./mvnw test` stays fast enough to run on every save. Slice tests
are where error-shape assertions live, because MockMvc can raise each framework exception cheaply
against the test-only `ProblemFixtureController`.

Failsafe runs `*IT` classes during `verify`. These extend `AbstractPostgresIntegrationTest` and
boot the full context against a Testcontainers PostgreSQL with `@ServiceConnection`, so Flyway
applies `V1__baseline.sql` and Hibernate validates against the real schema. The contract snapshot
and the end-to-end problem-body assertions live here, because both need the real application, not a
slice.

Assertions target the contract, not the implementation: HTTP status, `Content-Type`, the RFC 9457
member names, the `type` URN and the presence of extension members. No test asserts a stack trace
or an internal message string, so the taxonomy can gain entries without breaking existing tests.

## Contract and blast radius

The new public surface is `GET /api/v1/meta`, the `X-Correlation-Id` request and response header,
`application/problem+json` as the error media type for `/api/v1/**`, CORS headers on `/api/v1/**` and
`/actuator/health` when an origin is configured, and `/v3/api-docs` under the local profile only.
`services/api/openapi/openapi.json` becomes a reviewed artifact.

**Two existing behaviors do change, correcting the first draft's claim that none did.**

Flyway starts running. Adding `spring-boot-flyway` means `V1__baseline.sql` is applied where previously
it was silently skipped, so the `wealth` schema and `flyway_schema_history` appear in databases that did
not have them. On a developer volume predating this change the effect is simply that the migration now
runs; there is no destructive step, because the baseline only creates a schema.

`/actuator/health` gains CORS response headers when an origin is configured. Its status code, body and
`show-details: never` setting are unchanged, so the frontend's existing `health-client.ts` keeps working
and now also works from a browser, which `README.md:137` already promised.

`V1__baseline.sql` and the Compose service are not modified.

The compatibility risks worth naming are the forward-pinned springdoc 3.1.1 against Boot 4.1.1, the
Testcontainers 2.x API surface behind the renamed modules, and the deprecated-but-present legacy
`PostgreSQLContainer` class. Each is addressed in the owning phase's risk section. The
`spring.mvc.problemdetails` interaction is no longer a risk: it is settled, and the property is not set.

## Risks

| Risk | Mitigation |
|---|---|
| No JDK exists, so nothing in this plan has been executed. Claims could be wrong in a way only a build reveals. | Phase 1 starts with the JDK install and `dependency:tree` resolution check as an explicit gate. Red-team review already caught five false dependency assumptions from Maven Central artifact listings alone, so the remaining uncertainty is narrower than it was — but a build is still the only proof. A resolution failure is reported as a blocker, never worked around by changing pinned versions. |
| Testcontainers 2.x is a major line whose API differs from the abundant 1.x documentation, and the legacy `PostgreSQLContainer` class is present but deprecated. | Compilation failure or deprecation error in Phase 1 step 4. | Phase 1 names the fallback: switch to `org.testcontainers.postgresql.PostgreSQLContainer` without the diamond, and record which compiled so later phases copy the right import. |
| The Flyway repair surfaces a migration problem that has been latent since #35, on a database that has drifted. | Startup failure after Phase 1 step 1. | The baseline creates only a schema, so the realistic worst case is a stale local volume. Recreating it is destructive and therefore an explicit developer decision, never a scripted step. |
| `./mvnw verify` now fails closed without Docker, and whoever wires CI reaches for `-DskipITs`. | Every pipeline run red, then a flag appears in the CI command. | Phase 6 documents that `-DskipITs` also disables the contract drift gate, so it is forbidden. #47 must provide a Docker-capable runner. |
| springdoc 3.1.1 is recent and this project is on Boot 4.1.1, one patch ahead of springdoc's 4.1.0 build parent. | Phase 5 begins with a resolution and smoke check before any annotation work. If `/v3/api-docs` does not serve, the fallback is `springdoc-openapi-starter-webmvc-api` without the UI webjars, and only then an upstream issue report. |
| A framework status bypasses the `createProblemDetail` funnel and keeps `about:blank` as its `type`. | Phase 3 asserts `$.type` per status code, so a bypass is visible rather than inferred. The property-versus-advice question is settled and not a risk: Boot's handler is `@ConditionalOnMissingBean(ResponseEntityExceptionHandler.class)`, so the advice is solely responsible. |
| Spring's own `detail` text reflects client input for some statuses, leaking probe values back to an attacker. | Phase 3 overrides `detail` with fixed taxonomy text for the reflecting statuses and asserts a marker string is absent from the body for every framework status, not only the last-resort path. |
| springdoc embeds a per-run ephemeral port in `servers`, so the drift assertion fails on every run. | Phase 5 sets an explicit relative `servers` entry, which removes the volatile value, and canonicalizes key order. The two-runs-no-diff check runs before the deliberate-break check so stability is proven first. |
| A test controller is component-scanned into the published contract, publishing endpoints that do not exist. | Phase 3 puts the fixture outside the `com.rikkaus.wealth` scan root and off `/api/v1`, and Phase 5 asserts the generated `paths` set equals an explicit allow-list so any future leak fails the build. |
| Errors raised before `DispatcherServlet` — including from the correlation filter itself — bypass the advice and return a non-conforming body. This is an open upstream inconsistency (spring-projects/spring-boot#48392). | Phase 4 catches only the filter's own work and renders through `ProblemDetailWriter`, leaving `chain.doFilter` unwrapped so the taxonomy survives. `ProblemDetailWriter` guards on `isCommitted` and calls `resetBuffer`, so it cannot append a second body to a partially-written response. |
| `correlationId` is absent from bodies rendered on the container's ERROR dispatch, which is exactly the class of error a user would report. | `OncePerRequestFilter` skips ERROR dispatch by default, so Phase 4 overrides `shouldNotFilterErrorDispatch()` and stashes the ID in a request attribute that survives across dispatches. A test asserts an ERROR-dispatch body still carries it. |
| Accepting a client-supplied correlation ID is a log-injection and correlation-spoofing vector. | Phase 4 validates against `^[A-Za-z0-9-]{8,64}$` before the value reaches MDC or the response, and falls back to a generated UUID rather than attempting to escape a malformed value. A test asserts rejection of CRLF and oversized input. |
| The committed contract goes stale if the drift assertion only runs in `verify` and nobody runs `verify`. | Phase 6 documents the command and #47 wires it into CI. Until CI exists, this is a documented manual gate, stated honestly rather than claimed as automated. |
| Enabling CORS widens the attack surface before authentication exists. | Phase 2 takes a configured origin list with a genuinely empty default, registers no mapping at all when that list is empty, refuses credentials, and uses no wildcard. Phase 6 flags `allowCredentials` as a decision #42 must make deliberately rather than inherit. |
| This filter sits outside the future Spring Security chain and would rewrite #42's 401 and 403 into 500. | Because `chain.doFilter` is not wrapped in a catch, auth exceptions propagate normally. Phase 4 records the constraint for #42: keep `ExceptionTranslationFilter` inside this filter and add a test asserting 401 and 403 survive. |
| #39's ownership criterion is treated as met because a contract was documented, unblocking #41 on a false signal. | Phase 6's acceptance report records "API Ready" limb by limb and states that the ownership limb is **not** met, and the #38 notification says the same. |

## Coordination

Phase 6 notifies #38 with the contract path, the error taxonomy and the correlation header name.
Per #39's own coordination rule, any later change to the published contract must be accompanied by
updated examples and a note on the sibling task before merge.

`.gitignore` ignores `plans/**/*` except a short allow-list, and `git ls-files plans/reports` returns
zero tracked files. Phase 6 therefore allow-lists this plan directory and the acceptance report, since
that report is the only evidence a reviewer has for #39's criteria and the #38 notification links it.
The repository already has this exception pattern at `.gitignore:71-75`.

## Red Team Review

### Session — 2026-09-27

Four reviewers ran with all four adversarial lenses at Full verification tier: Security Adversary
(Fact Checker), Failure Mode Analyst (Flow Tracer), Assumption Destroyer (Scope Auditor) and Scope &
Complexity Critic (Contract Verifier).

**Findings:** 22 after deduplication (21 accepted, 1 rejected).
**Severity breakdown:** 7 Critical, 9 High, 6 Medium.

Every load-bearing claim was independently re-verified by the orchestrator against Maven Central
metadata, POMs and JAR listings before acceptance, rather than taken on the reviewers' word.

| # | Finding | Severity | Disposition | Applied to |
|---|---------|----------|-------------|------------|
| 1 | Testcontainers 2.x renamed modules; bare `postgresql`/`junit-jupiter` have no 2.x and are absent from the BOM | Critical | Accept | Phase 1, plan constraints |
| 2 | `spring-boot-flyway` absent from the classpath — Flyway autoconfiguration has never loaded | Critical | Accept | Phase 1, plan decisions, blast radius |
| 3 | `@WebMvcTest`/`MockMvc` moved to `spring-boot-webmvc-test`, not in `spring-boot-starter-test` | Critical | Accept | Phase 1, Phase 2 |
| 4 | `TestRestTemplate` moved to `spring-boot-resttestclient` and needs `@AutoConfigureTestRestTemplate` | Critical | Accept | Phase 1, Phases 3–5 |
| 5 | Test fixture controller inside the scan root would publish three fake paths into the committed contract | Critical | Accept | Phase 3, Phase 5 |
| 6 | Static `@Container` stops per `*IT` class while Spring caches the context, leaving a dead port | Critical | Accept | Phase 1 |
| 7 | Phase 4's filter catch rule was undecidable and would swallow the taxonomy and #42's 401/403 | Critical | Accept | Phase 4 |
| 8 | No `servers` entry plus `RANDOM_PORT` makes the drift assertion fail every run | High | Accept | Phase 5 |
| 9 | `@WebMvcTest` includes `WebMvcConfigurer` and `Filter`, causing a duplicate `clock` bean and breaking Phases 2–3 tests | High | Accept | Phase 2, Phase 4, ownership map |
| 10 | `ProblemDetailWriter` lacked an `isCommitted` guard and `resetBuffer`, and omitted `correlationId` | High | Accept | Phase 3 |
| 11 | CORS default was non-empty in base config, violated `ARCH:235`, missed `/actuator/health`, env var undocumented | High | Accept | Phase 2, plan decisions |
| 12 | `/v3/api-docs` was public in every environment; `application-local.yml` was created by no phase | High | Accept | Phase 2, Phase 5 |
| 13 | `application-test.yml` omission let tests fall back to the developer's real database | High | Accept | Phase 1 |
| 14 | `spring.mvc.problemdetails.enabled` is shadowed by the advice, determinately — not an open question | High | Accept | Phase 3, plan constraints |
| 15 | `correlationId` absent on ERROR dispatch because `shouldNotFilterErrorDispatch()` defaults to true | High | Accept | Phase 4 |
| 16 | Phase 5 declared no dependency on Phase 4 while consuming its artifacts; hand-written schema hides contract-vs-behaviour drift | High | Accept | Phase 5, dependency graph |
| 17 | #39's ownership acceptance criterion was silently dropped | High | Accept | Plan acceptance criteria, Phase 6 |
| 18 | The acceptance report is git-ignored, so evidence never reaches review | Medium | Accept | Phase 6, coordination |
| 19 | Four wrong `path:line` citations (`:143`→`:144`, `:137`→`:135`, `README:157`→`:160-163`) | Medium | Accept | All files |
| 20 | ArchUnit exact-decimal rule exceeded its approved rationale and was made a #39 criterion | Medium | Accept | Phase 1 (scoped), acceptance criteria (removed) |
| 21 | `apps/mobile` has no OpenAPI codegen; the contract omits the one endpoint #38 calls | Medium | Accept | Phase 5, Phase 6 |
| 22 | Merge Phases 3 and 4 to remove arbitration ceremony | Medium | **Reject** | — |

**Rationale for the one rejection.** Phases 3 and 4 have genuinely distinct verification gates — the
error taxonomy is proven by per-status body assertions, the correlation ID by filter-lifecycle and
ERROR-dispatch tests — and merging them yields a single 16-hour phase whose failure would be harder to
localize. The arbitration is now three sentences and explicit. The concrete defect the finding
identified *was* accepted: the first draft's Phase 3 code sample used `CorrelationId.MDC_KEY` while its
prose said to inline the literal, which would not have compiled before Phase 4. Both now consistently
use the literal, and Phase 4 owns the one-line replacement in each of the two files.

### Whole-Plan Consistency Sweep

- Files reread: `plan.md`, `phase-01` through `phase-06`.
- Decision deltas checked: 21 accepted findings plus 3 new Product Owner decisions.
- Reconciled stale references: the `spring.mvc.problemdetails` key removed from constraints, inventory
  and Phase 3; Testcontainers coordinates corrected in constraints, inventory and Phase 1; the
  "nothing existing changes behavior" claim in blast radius replaced; the Phase 4/5 parallelism claim
  removed from the dependency graph and Phase 5's frontmatter; the fixture controller path corrected in
  the inventory, the ownership map and Phase 3; `ApiConfiguration` split into `ClockConfiguration` and
  `CorsConfiguration` across the inventory, ownership map and Phase 2; the exact-decimal rule removed
  from acceptance criteria; effort raised from 42h to 46h for Phase 1's added scope; the four wrong line
  citations corrected wherever they appeared.
- Unresolved contradictions: 0.

## Open questions

These do not block Phase 1. Each must be answered before the phase that consumes it.

1. Should `services/api/openapi/openapi.json` also be copied somewhere the frontend build imports
   directly, and does #38 want an OpenAPI codegen step at all? `apps/mobile` has no codegen dependency
   today, so the contract is a review artifact rather than a generation input. Phase 6 asks #38 in the
   notification; if codegen is wanted it belongs to #38's scope.
2. Boot's `BuildProperties` requires the `build-info` goal, which writes `build.time` on every build.
   Confirm that exposing a build timestamp on an unauthenticated `GET /api/v1/meta` is acceptable, or
   keep the response restricted to `buildVersion`, which is what Phase 2 does pending an answer.
3. Which exact origin does Expo web serve on in this team's setup? `apps/mobile/package.json:38` pins no
   port, so Phase 2 must read the real value rather than assume Expo's 8081 default.

The Flyway defect needed no separate issue: the Product Owner chose to repair it inside Phase 1, and
Phase 6 records it in the acceptance report and the README so the process gap stays visible without a
second ticket.

### Answers after implementation — 2026-09-27

1. **Copying the contract somewhere the frontend build imports, and whether #38 wants codegen.** Still
   open, and it is #38's call rather than this task's. Confirmed by inspection that `apps/mobile` has no
   OpenAPI codegen dependency and no generate script, so the contract is a review and documentation
   artifact today. Stated as such in `docs/api-contract-conventions.md` and carried into the sibling
   notification, so #38 decides with the facts rather than discovering them. If codegen is wanted it
   belongs to #38's scope.
2. **Exposing a build timestamp on an unauthenticated endpoint.** Not needed and therefore not asked.
   `MetaResponse` exposes `buildVersion` only, and `build.time` is not surfaced anywhere. The question
   can stay closed unless someone actively wants the timestamp; the `build-info` goal writes it to
   `META-INF/build-info.properties` regardless, so enabling it later is a one-line change.
3. **The real Expo web origin.** Answered empirically: `http://localhost:8081`. `npx expo start --web`
   printed `Waiting on http://localhost:8081`, confirmed by `lsof` and an HTTP 200. That value is in
   `application-local.yml` and `.env.example`. Expo's documented default and this project's actual
   origin agree, but the value is now evidence rather than an assumption.

### Corrections implementation forced on the plan

Recorded here so the plan describes what shipped rather than what was intended. Each is detailed in the
owning phase's implementation notes.

| Phase | The plan said | What is true |
|---|---|---|
| 1 | Seven test-scope dependencies | Eight. `spring-boot-resttestclient` declares `spring-boot-restclient` as optional, so without declaring it every `@SpringBootTest` carrying `@AutoConfigureTestRestTemplate` fails to refresh. Not findable without a build. |
| 1 | The legacy generic `PostgreSQLContainer` compiles cleanly | It compiles but is deprecated. Phase 3 took the documented fallback, `org.testcontainers.postgresql.PostgreSQLContainer` without the diamond. |
| 1 | Two ArchUnit rules need `allowEmptyShould` | All three. The slice pattern matches nothing while `com.rikkaus.wealth` has no subpackage at all. |
| 2 | `spring.jackson.serialization.write-dates-as-timestamps` | A Boot 3 key that aborts context startup on Boot 4. Jackson 3 moved the feature, so the key is `spring.jackson.datatype.datetime.write-dates-as-timestamps`. |
| 2 | A `CorsRegistry` mapping covers `/actuator/health` | It emits nothing. Actuator endpoints have their own handler mapping; `management.endpoints.web.cors` is the mechanism. The dead mapping was removed rather than left as false reassurance. |
| 3 | Overriding `createProblemDetail` classifies every framework status | It does not. `ErrorResponse` exceptions carry their own body and never call it, so 404, 405 and 415 kept `about:blank`. Classification moved to `handleExceptionInternal`, after the superclass call. |
| 5 | `@SpringBootTest(properties = ...)` on a subclass of the integration base | Replaces the base annotation rather than merging, silently dropping `webEnvironment = RANDOM_PORT`. It must be repeated. |

<!-- slug: issue-39-api-contract-errors-foundation -->
