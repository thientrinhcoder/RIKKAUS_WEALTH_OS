---
phase: 5
title: "OpenAPI generation and build publication"
status: done
priority: P1
effort: "8h"
issue: 39
dependencies: [1, 2, 3, 4]
---

# Phase 5: OpenAPI generation and build publication

## Goal

Generate the OpenAPI 3 contract from the running application during `./mvnw verify`, commit it at
`services/api/openapi/openapi.json`, and fail the build when the code and the committed contract
disagree.

## Overview

This phase satisfies the acceptance criterion both #39 and its parent
[#8](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/8) state as "OpenAPI contract được
publish từ build" — published from the build, not merely served at runtime.

The dependency is pinned and was verified independently during planning.
`org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1` is the current Maven Central `<release>`,
published 2026-09-06, and its aggregator POM declares `spring-boot-starter-parent` **4.1.0** as its
build parent — the same Boot 4.1 line this project runs. The widely documented `2.x` line, which tops
out at 2.9.1, targets Boot 3.x and will not work here.

The publication mechanism is a test, not a Maven plugin, and that was a considered choice.
`springdoc-openapi-maven-plugin`'s latest release is 1.5 from 2025-05-04, months before Boot 4 GA, with
no Boot 4 compatibility statement. More importantly it boots the packaged application out-of-process
through `spring-boot-maven-plugin`'s `start` and `stop` goals, and this application has JPA and Flyway,
so startup needs a reachable PostgreSQL that the plugin has no lifecycle hook to provide. A `*IT` class
extending Phase 1's `AbstractPostgresIntegrationTest` already has that database, runs on the JUnit
lifecycle the team is adopting anyway, and adds no second application-boot mechanism.

The drift assertion is the part that makes this worth doing. #39 requires that a contract change be
accompanied by updated examples and a note to sibling tasks before merge. That is only enforceable if
changing the API without regenerating the contract fails the build.

## Context links

- Issue: [#39](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/39)
- Parent feature criterion: [#8](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/8)
- Project decision: `ARCHITECTURE_TECHNOLOGY_DECISIONS.md:110`, `:139`
- Research: `plans/reports/researcher-260927-1427-springdoc-openapi-boot4.md`
- Version evidence: https://repo1.maven.org/maven2/org/springdoc/springdoc-openapi/3.1.1/springdoc-openapi-3.1.1.pom
- Known conflict to avoid: https://github.com/springdoc/springdoc-openapi/issues/3163
- Consumer: [#38](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/38)

## Requirements

### Functional

- [x] `/v3/api-docs` serves a valid OpenAPI 3 document under the `local` profile and returns 404
      otherwise.
- [x] The document contains only `/api/v1/**` paths; actuator and any future `/api/v2` are excluded.
- [x] `info` carries a stable title, version and description with no build timestamp.
- [x] `GET /api/v1/meta` is documented with its success schema and at least one example.
- [x] The Problem Details schema is documented once and referenced by every error response, including
      the `correlationId` extension member from Phase 4.
- [x] `X-Correlation-Id` is documented as both a request and a response header.
- [x] `./mvnw verify` writes or verifies `services/api/openapi/openapi.json`.
- [x] An API change without a contract regeneration fails the build with a readable diff.
- [x] `./mvnw verify -Dopenapi.update=true` regenerates the committed file.
- [x] Swagger UI is disabled by default and available only under the local profile.

### Non-functional

- [x] The generated JSON is byte-stable across runs on the same code, so drift detection is not flaky.
- [x] The committed file is human-readable in a PR diff: sorted keys, two-space indentation, trailing
      newline.
- [x] Spring Framework 7's native `spring.mvc.apiversion` feature stays disabled.

## Architecture

```text
shared/openapi/OpenApiConfiguration.java   the OpenAPI bean: info, ProblemDetail schema, examples
shared/api/MetaController.java             gains operation annotations (Phase 2 created it)
openapi/openapi.json                       the committed, reviewed contract
src/test/.../OpenApiContractIT.java        generates, canonicalizes, compares or updates
application.yml                            springdoc block
```

`info` title and version are set through an `OpenAPI` bean rather than properties, because springdoc
exposes no `application.yml` property for them in any release line. Only path and enablement toggles
live in configuration.

Byte-stability is the hinge of the whole design. springdoc does not guarantee key ordering, and any
value that changes per build — a timestamp, a random port, a host — would make the committed file churn
on every run and train the team to regenerate blindly. The test therefore canonicalizes before
comparing: parse, sort all object keys recursively, serialize with fixed indentation. And `info` is
kept free of build metadata for the same reason, which is why Phase 2's `build-info` timestamp stays out
of the contract even though the meta endpoint's schema is in it.

## Files to create and modify

- Create: `services/api/src/main/java/com/rikkaus/wealth/shared/openapi/OpenApiConfiguration.java`
- Create: `services/api/src/test/java/com/rikkaus/wealth/shared/openapi/OpenApiContractIT.java`
- Create: `services/api/openapi/openapi.json`
- Modify: `services/api/pom.xml` — add the springdoc dependency only
- Modify: `services/api/src/main/resources/application.yml` — add the `springdoc` key only
- Modify: `services/api/src/main/resources/application-local.yml` — re-enable Swagger UI
- Modify: `services/api/src/main/java/com/rikkaus/wealth/shared/api/MetaController.java` — add operation
  annotations. This is the one cross-phase edit; Phase 2 deliberately left annotations out so that only
  this phase touches them.

## Implementation steps

1. **Add and smoke-test the dependency before any annotation work.** This ordering is deliberate: if
   springdoc 3.1.1 does not serve on Boot 4.1.1, everything downstream is wasted effort.

   ```xml
   <dependency>
       <groupId>org.springdoc</groupId>
       <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
       <version>3.1.1</version>
   </dependency>
   ```

   Then resolve and smoke-test:

   ```bash
   ./services/api/mvnw -f services/api/pom.xml dependency:get \
       -Dartifact=org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1
   docker compose --env-file .env.example up -d --wait postgres
   ./services/api/mvnw -f services/api/pom.xml spring-boot:run
   curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/v3/api-docs
   ```

   A 200 means proceed. If it fails, the fallback order is: try
   `springdoc-openapi-starter-webmvc-api` without the UI webjars, since the UI is disabled in production
   anyway; then check whether any `spring.mvc.apiversion` property crept into configuration, which
   springdoc issue #3163 reports causes exactly a 400 on this endpoint; then report upstream and raise it
   as a blocker rather than downgrading to the Boot-3 `2.x` line.

2. **Configure springdoc.** Add to `application.yml`:

   ```yaml
   springdoc:
     api-docs:
       enabled: false          # the contract endpoint itself is gated, not only the UI
       path: /v3/api-docs
     paths-to-match: /api/v1/**
     swagger-ui:
       enabled: false
     show-actuator: false
   ```

   And appended to `application-local.yml`, which Phase 2 creates:

   ```yaml
   springdoc:
     api-docs:
       enabled: true
     swagger-ui:
       enabled: true
   ```

   **Gating `api-docs` and not only `swagger-ui` is a correction from the first draft.** Red-team review
   pointed out that `swagger-ui.enabled: false` disables only the browsable UI while `/v3/api-docs`
   stayed anonymously readable in every environment — publishing every route, every schema and the whole
   error taxonomy including the reserved `unauthorized` and `forbidden` codes to any caller, on a service
   with no authentication until #42. The committed `openapi.json` is the artifact siblings consume, so
   nothing needs the live endpoint outside local development and the build.

   `OpenApiContractIT` must therefore activate the profile explicitly rather than relying on the default.
   Add `@ActiveProfiles({"test", "local"})` to that test class, or set
   `@SpringBootTest(properties = "springdoc.api-docs.enabled=true")` on it. Prefer the properties form,
   since it states the dependency locally and does not drag the rest of the local profile in.

   `paths-to-match` keeps `/actuator/**` and any future version out of the v1 contract without
   annotating anything with `@Hidden`. Note the consequence, which Phase 6 discloses to #38: the
   published contract deliberately omits `/actuator/health`, which is currently the only endpoint the
   frontend calls.

3. **Write the `OpenAPI` bean.** It owns `info` and the shared error components.

   ```java
   @Configuration
   class OpenApiConfiguration {

       @Bean
       OpenAPI rikkausWealthApi() {
           return new OpenAPI()
                   .servers(List.of(new Server().url("/")))   // REQUIRED — see note below
                   .info(new Info()
                           .title("Rikkaus Wealth OS API")
                           .version("v1")
                           .description("Versioned REST contract for the Rikkaus Wealth OS backend."))
                   .components(new Components()
                           .addSchemas("ProblemDetail", problemDetailSchema())
                           .addHeaders("X-Correlation-Id", correlationIdHeader()));
       }
   }
   ```

   **The explicit `servers` entry is load-bearing, not cosmetic.** When no server is declared, springdoc
   synthesises one from the incoming request's scheme, host and port. `OpenApiContractIT` runs against a
   `RANDOM_PORT` server, so the generated document would embed a different ephemeral port on every run —
   the committed file would say `http://localhost:51234` and the next run would generate
   `http://localhost:49807`. Canonicalization sorts keys; it does not normalise values. The drift
   assertion would therefore fail on every single run with the message "the API changed", which is a lie,
   and the documented escape hatch would both silence it and train the team to run
   `-Dopenapi.update=true` reflexively — the one thing this phase forbids. Red-team review caught this;
   the first draft would have shipped a gate that fails 100% of the time.

   A relative `/` server URL is also the right answer for #38, which resolves paths against its own
   configured base URL rather than a host baked into the contract.

   Define the `ProblemDetail` schema explicitly rather than letting springdoc infer it from Spring's
   class. The inferred version would omit the project's extension members, and `correlationId` and
   `errors` are exactly the parts #38's error classifier needs. Document all of `type`, `title`,
   `status`, `detail`, `instance`, `correlationId` and `errors`, with `type` described as the
   `urn:rikkaus:problem:<code>` taxonomy and an enumeration of the codes Phase 3 defined.

   **A hand-written schema creates a blind spot the drift assertion cannot see, so close it here.** The
   drift check compares generated against committed, and both derive from this bean — so if the schema
   claims a member the server never sends, every build passes while #38's Zod parser rejects every real
   error response. That is the exact failure the parent feature is graded on. Red-team review found this,
   and it is also why this phase now declares a dependency on Phase 4 rather than claiming to be parallel
   with it.

   Add a contract-versus-behaviour assertion to `ProblemDetailsContractIT` (Phase 3's file, extended here
   by agreement since Phase 3 has already merged by the time this phase runs):

   ```java
   @Test
   void everyDocumentedRequiredProblemMemberIsActuallySent() throws Exception {
       Set<String> documented = requiredMembersOf(committedContract(), "ProblemDetail");
       Set<String> actual = memberNamesOf(realErrorResponseBody());
       assertThat(actual).as("The published contract documents a member the server does not send")
               .containsAll(documented);
   }
   ```

   Enumerate the `type` codes from `ProblemType` itself rather than by hand, so adding a taxonomy entry
   without documenting it shows up as a contract diff.

4. **Annotate the meta operation.** In `MetaController`, add `@Operation` with a summary and
   `@ApiResponse` entries for 200 and for the problem responses the endpoint can actually produce — 405
   and 406. Do not document statuses the endpoint cannot return; an over-broad contract is worse than a
   narrow one because #38 will generate fixtures for responses that never occur.

   Attach a concrete example to the 200 response so the frontend has a real payload to build a fixture
   from, and one problem example showing the `correlationId` member populated.

   Be explicit about what the contract does **not** carry this iteration. Because Phase 3 moved the test
   fixture controller off `/api/v1` to keep it out of the published document, no request-accepting
   endpoint exists, so there is no 400 validation example and no `errors` array sample. Phase 6 states
   this in the #38 notification rather than letting #38 find out. The `errors` member is still documented
   in the schema so its shape is agreed before #42 adds the first real request body.

5. **Write `OpenApiContractIT`.** This is the mechanism that publishes and enforces.

   ```java
   @SpringBootTest(properties = "springdoc.api-docs.enabled=true")
   class OpenApiContractIT extends AbstractPostgresIntegrationTest {

       private static final Path CONTRACT = Path.of("openapi", "openapi.json");

       // org.springframework.boot.resttestclient.TestRestTemplate — Boot 4 package.
       // Available because AbstractPostgresIntegrationTest carries @AutoConfigureTestRestTemplate.
       @Autowired TestRestTemplate restTemplate;

       @Test
       void publishesOnlyTheProductionApiSurface() throws Exception {
           JsonNode doc = objectMapper.readTree(restTemplate.getForObject("/v3/api-docs", String.class));
           assertThat(doc.get("paths").fieldNames()).toIterable()
                   .as("A non-production path leaked into the contract. A test controller inside the "
                       + "component-scan root is the usual cause.")
                   .containsExactly("/api/v1/meta");
       }

       @Test
       void publishedContractMatchesTheRunningApplication() throws Exception {
           String generated = canonicalize(restTemplate.getForObject("/v3/api-docs", String.class));

           if (Boolean.getBoolean("openapi.update")) {
               Files.createDirectories(CONTRACT.getParent());
               Files.writeString(CONTRACT, generated);
               return;
           }

           assertThat(CONTRACT).as(
                   "Committed OpenAPI contract is missing. Run "
                   + "./mvnw verify -Dopenapi.update=true and review the diff.").exists();
           assertThat(canonicalize(Files.readString(CONTRACT)))
                   .as("The API changed but %s was not regenerated. Run "
                       + "./mvnw verify -Dopenapi.update=true, review the diff, and notify "
                       + "issue #38 before merging.", CONTRACT)
                   .isEqualTo(generated);
       }
   }
   ```

   `canonicalize` parses with Jackson, enables `ORDER_MAP_ENTRIES_BY_KEYS` and `INDENT_OUTPUT` with a
   two-space `DefaultPrettyPrinter`, and appends a trailing newline. Sorting is what makes the
   comparison stable against springdoc's ordering.

   Resolve `CONTRACT` relative to the Maven module directory, since Failsafe's working directory is
   `services/api`. Assert that assumption in the test setup rather than trusting it, so a future build
   change surfaces as a clear failure instead of a mysterious file written to the repository root.

   The failure message matters as much as the assertion. It is the only place a contributor learns both
   the regeneration command and the obligation to notify #38.

6. **Generate and review the first contract.** Run
   `./mvnw verify -Dopenapi.update=true`, then read the generated file before committing it. Confirm it
   contains exactly one path, that no actuator path appears, that `info.version` is `v1`, that the
   `ProblemDetail` schema includes `correlationId` and `errors`, and that nothing resembling a timestamp,
   port or hostname is present.

7. **Prove the drift assertion works.** Temporarily add a field to `MetaResponse`, run `./mvnw verify`,
   and confirm it fails with the regeneration message. Then revert. A drift check that has never been
   observed failing is not a check. Record the observed failure in the phase notes.

8. **Confirm the contract file is not git-ignored.** `.gitignore` ignores `**/target/` and a bare
   `build`, but nothing matching `services/api/openapi/`. Verify with
   `git check-ignore -v services/api/openapi/openapi.json`, which must report no match.

## Todo

- [x] Add springdoc 3.1.1 and confirm `/v3/api-docs` returns 200 before annotating anything.
- [x] Add the `springdoc` block to `application.yml` with `api-docs.enabled: false` and the `/api/v1/**`
      scope.
- [x] Append the `api-docs` and `swagger-ui` overrides to `application-local.yml`.
- [x] Create `OpenApiConfiguration` with the explicit `servers`, `info`, and `ProblemDetail` schema.
- [x] Enumerate the `type` codes from `ProblemType` rather than by hand.
- [x] Document `X-Correlation-Id` as a request and response header.
- [x] Annotate `MetaController` with 200, 405 and 406 responses plus concrete examples.
- [x] Create `OpenApiContractIT` with the property override, canonicalization and the actionable failure
      message.
- [x] Add the `paths` allow-list assertion so a leaked test route fails the build.
- [x] Add the contract-versus-behaviour assertion to `ProblemDetailsContractIT`.
- [x] Assert the resolved contract path is inside `services/api`.
- [x] Generate the contract, read it, and confirm it contains no port, host or timestamp.
- [x] Run `verify` twice with no code change and confirm zero diff, **before** the break check.
- [x] Deliberately break the API, observe the drift failure, revert, and record it.
- [x] Confirm `git check-ignore` reports no match for the contract path.
- [x] Confirm `/v3/api-docs` and Swagger UI both return 404 without the local profile.

## Verification

```bash
./services/api/mvnw -f services/api/pom.xml verify -Dopenapi.update=true
git diff --stat services/api/openapi/openapi.json
./services/api/mvnw -f services/api/pom.xml verify
./services/api/mvnw -f services/api/pom.xml verify
git check-ignore -v services/api/openapi/openapi.json
jq '.info, (.paths | keys), (.components.schemas.ProblemDetail.properties | keys)' services/api/openapi/openapi.json
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/swagger-ui/index.html
```

Pass conditions:

- The first `verify` writes the contract; the second and third pass with no diff, proving byte-stability
  across runs.
- `git check-ignore` exits non-zero with no output, meaning the file is tracked.
- `jq` shows `info.version` of `v1`, exactly one path `/api/v1/meta`, and `ProblemDetail` properties
  including `correlationId` and `errors`.
- Swagger UI returns 404 without the local profile and 200 with it.
- The deliberate-break check fails with the regeneration message and passes again after revert.

## Success criteria

- [x] The contract is generated from the running application, not hand-written.
- [x] It is committed and reviewable in a PR diff.
- [x] Changing the API without regenerating fails the build, with a message naming both the command and
      the obligation to notify #38.
- [x] The error schema #38 needs is documented, including the extension members.
- [x] No production surface exposes Swagger UI.

## Risk assessment

| Risk | Signal it broke | Response |
|---|---|---|
| springdoc 3.1.1 does not work on Boot 4.1.1 despite being built against 4.1.0. | `/v3/api-docs` returns a non-200 in step 1. | Step 1 exists to find this before any other work. Follow its documented fallback order. Do not downgrade to the `2.x` Boot-3 line; report it as a blocker instead. |
| springdoc output is not byte-stable, making the drift assertion flaky and eroding trust in it. | The second `verify` in the verification block fails with no code change. | The explicit `servers` entry removes the known cause, the per-run ephemeral port. Canonicalization handles ordering. If instability survives both, find the volatile field with `diff` and either exclude it from the comparison deliberately, documenting why, or remove it from the contract. Never paper over it by running with `-Dopenapi.update=true` in CI. The two-runs-no-diff check runs *before* the deliberate-break check so stability is established first. |
| The contract documents a member the server never sends, and the drift check cannot see it because both sides come from the hand-written bean. | #38 reports that every error response fails schema validation. | The contract-versus-behaviour assertion in step 3 is the guard, and this phase now depends on Phase 4 so `correlationId` is real before it is documented. |
| A test controller added in some future phase leaks into the contract. | The `paths` allow-list assertion fails. | The assertion names the likely cause in its message. Update the allow-list only when a genuinely new production endpoint is added. |
| The contract file is written to the wrong directory because Failsafe's working directory differs from assumption. | `openapi.json` appears at the repository root. | Step 5 asserts the resolved path is inside `services/api` as part of the test, so this fails loudly. |
| Someone adds `-Dopenapi.update=true` to the CI command to stop build failures, silently disabling the whole check. | The contract changes in CI commits without review. | Phase 6 documents that the flag is a local, deliberate action. #47 owns the pipeline and must not use it. |
| Generated documentation drifts from the hand-written `ProblemDetail` schema, so the contract claims members the code does not send. | `ProblemDetailsContractIT` passes but a frontend fixture has fields the server omits. | Keep the hand-written schema and Phase 3's taxonomy aligned by enumerating the codes in the schema from `ProblemType`, so adding an enum entry without documenting it shows up as a contract diff. |
| Swagger UI reaches production and exposes the full API surface to anonymous users. | `swagger-ui/index.html` returns 200 in a deployed environment. | Disabled by default, enabled only in the `local` profile. The verification block checks both states. |

## Security considerations

A published contract is a deliberate disclosure. It describes only `/api/v1/**`, which is intentional
and is the artifact the frontend needs, and `show-actuator: false` keeps the management surface out of
it. Because the contract is committed to the repository, it must never accumulate example values that
resemble real credentials, tokens or personal data; examples in step 4 use obviously synthetic values.

Swagger UI is disabled outside local development. It is a convenience for a developer, not an
endpoint a deployment should expose, and shipping it enabled would advertise every route to anonymous
callers.

`spring.mvc.apiversion` must stay unused, both because springdoc #3163 breaks on it and because the
static `/api/v1` prefix is the documented project convention.

## Rollback order

This phase touches four surfaces and a failure at step 1 leaves the branch partly changed, so reverse
in this order: delete `services/api/openapi/`, then the `springdoc` key from `application.yml` and the
overrides from `application-local.yml`, then `OpenApiConfiguration` and the `MetaController`
annotations, then the springdoc dependency from `pom.xml`. Reverting the dependency first would leave
uncompilable annotation imports behind.

Phases 1 through 4 are independently useful and are not rolled back with this phase: the test harness,
the Flyway repair, the `/api/v1` surface, the error taxonomy and the correlation ID all stand without a
published contract.

## Next steps

Phase 6 documents these conventions for the sibling teams, notifies #38 with the contract path, and
records the acceptance evidence.

## Implementation notes — 2026-09-27

### springdoc 3.1.1 works on Boot 4.1.1

The step-1 gate passed on the first attempt, so none of the documented fallbacks were needed.
`/v3/api-docs` returned 200 and Swagger UI returned 200 under the `local` profile, with `/api/v1/meta`
already documented and `serverTime` typed as `string`/`date-time`.

The smoke run also confirmed the `servers` risk was real rather than theoretical. Before the explicit
entry, the served document carried
`"servers":[{"url":"http://localhost:8080","description":"Generated server url"}]` — exactly the
per-run host and port that would have made the drift assertion fail on every single run while claiming
the API had changed.

### One correction to the phase's code sample

`@SpringBootTest(properties = "springdoc.api-docs.enabled=true")` on a subclass **replaces** the base
class's `@SpringBootTest` rather than merging with it, so it silently dropped
`webEnvironment = RANDOM_PORT`. Every test in the class failed to refresh its context with
`BeanInstantiationException ... No local test web server available`. `webEnvironment` has to be
repeated in the subclass annotation. Sibling annotations on the base class — `@AutoConfigureTestRestTemplate`
and `@ActiveProfiles` — are inherited normally; only the repeated annotation type is overridden.

### Observed results

The generated contract is 153 lines. `info` reads
`{"title":"Rikkaus Wealth OS API","version":"v1"}` with no build metadata, `servers` is the single
relative entry `{"url":"/"}`, and `paths` contains exactly `["/api/v1/meta"]` — so the test fixture
controller did not leak, confirming the out-of-scan-root placement works. The documented responses for
that operation are `200`, `405` and `406`, and no others, because the endpoint takes no request body or
parameters and therefore cannot produce 400 or 415.

`components.schemas.ProblemDetail.properties` carries all seven members — `type`, `title`, `status`,
`detail`, `instance`, `correlationId`, `errors` — with `["status","title","type"]` required, and the
`type` enumeration lists all nine taxonomy URNs, generated from `ProblemType.values()` rather than by
hand. `git check-ignore` exits non-zero with no output, so the file is trackable.

The only match for a volatile-looking pattern is the word "timestamp" inside the operation's own
description prose, not a value.

**Byte stability was established before the break check, in that order.** Two consecutive `verify` runs
with no code change left `git diff --stat services/api/openapi/openapi.json` empty both times.

**The drift gate was then observed failing.** Adding a `driftProbe` field to `MetaResponse` made
`verify` fail with exactly the intended message: "The API changed but openapi/openapi.json was not
regenerated. Run ./services/api/mvnw -f services/api/pom.xml verify -Dopenapi.update=true, review the
diff, and notify the frontend task before merging." Reverting the field returned the build to green.

Production gating was verified on a run with no profile active: `/v3/api-docs` returns 404 and
`/swagger-ui/index.html` returns 404, while `/api/v1/meta` still returns 200. Under the `local` profile
both return 200.

The contract-versus-behaviour assertion added to `ProblemDetailsContractIT` reads the required members
out of the committed document and asserts a real error response actually carries each one, so a schema
that documented a member the server never sends can no longer pass on both sides of the drift
comparison.

### What the contract deliberately does not carry

It omits `/actuator/health`, which is currently the only endpoint the frontend calls, because
`paths-to-match` is scoped to `/api/v1/**`. It contains no request-accepting endpoint, so there is no
400 validation example and no populated `errors` sample — the consequence of keeping the test fixture
off `/api/v1`. The `errors` member is still documented in the schema so its shape is agreed before the
first real request body arrives. All three are stated in the acceptance report and the sibling
notification rather than left to be discovered.
