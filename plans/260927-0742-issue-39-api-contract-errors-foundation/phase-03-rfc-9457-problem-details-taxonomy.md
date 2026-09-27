---
phase: 3
title: "RFC 9457 Problem Details taxonomy"
status: done
priority: P1
effort: "10h"
issue: 39
dependencies: [1, 2]
---

# Phase 3: RFC 9457 Problem Details taxonomy

## Goal

Make every error response under `/api/v1/**` an `application/problem+json` body conforming to
RFC 9457, with a stable `urn:rikkaus:problem:<code>` type, a per-field `errors` member for validation
failures, and coverage for failures raised before `DispatcherServlet` ever runs.

## Overview

Spring Framework 7's `ResponseEntityExceptionHandler` gives a large part of this, and the value of
this phase is knowing exactly where it stops.

A `@RestControllerAdvice extends ResponseEntityExceptionHandler` renders `ProblemDetail` bodies for
every built-in Spring MVC exception, because all of them implement `ErrorResponse`. That covers 400
for bean validation and malformed JSON and missing parameters and type mismatch, 404 for no handler
and no resource, 405 for unsupported method, 415 and 406 for media types, 500 for conversion and
write failures, and 408 for async timeout. `instance` is filled from the request path automatically.

**Correction from the first draft on `spring.mvc.problemdetails.enabled`.** The first draft attributed
that coverage to the property and listed the property-versus-advice interaction as an open question to
settle by test. Red-team review settled it from bytecode instead, and the answer is determinate:
Boot's `WebMvcAutoConfiguration$ProblemDetailsErrorHandlingConfiguration` carries both
`@ConditionalOnBooleanProperty("spring.mvc.problemdetails.enabled")` **and**
`@ConditionalOnMissingBean(ResponseEntityExceptionHandler.class)`. Registering our own
`ResponseEntityExceptionHandler` makes Boot's handler back off entirely, so the property becomes dead
configuration and one hundred percent of the coverage comes from our advice. The property is therefore
**not** set by this phase. Setting it would be misleading configuration that a later maintainer could
reasonably believe was load-bearing.

What this phase must write by hand: `type` and `title`, which otherwise resolve through a
`MessageSource` lookup and default to `about:blank` and the bare HTTP reason phrase; domain exception
mapping, which is never automatic; field-level validation detail, since Spring returns only a generic
`detail` string; and a path for exceptions thrown before dispatch, which bypass the advice entirely and
land on the container's `/error` mapping — a known upstream inconsistency
(spring-projects/spring-boot#48392). That last one matters because Phase 4 adds a filter.

The per-status tests still matter, but their framing changes: they enumerate what the advice covers,
rather than discovering whether two mechanisms compose.

## Context links

- Issue: [#39](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/39)
- Project rule: `ARCHITECTURE_TECHNOLOGY_DECISIONS.md:143`
- Research: `plans/reports/researcher-260927-1427-problem-details-correlation-tests.md`, section 1
- RFC 9457: https://www.rfc-editor.org/rfc/rfc9457
- Spring error responses: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html
- Upstream gap: https://github.com/spring-projects/spring-boot/issues/48392

## Requirements

### Functional

- [x] Every error under `/api/v1/**` returns `Content-Type: application/problem+json`.
- [x] Every body carries `type`, `title`, `status`, `detail` and `instance`.
- [x] `type` is always a `urn:rikkaus:problem:<code>` URN, never `about:blank`.
- [x] Bean validation failures carry an `errors` array naming each failing field and its message.
- [x] A domain exception type exists that any later feature slice can throw to produce a conforming
      body without touching the handler.
- [x] A failure raised inside a servlet filter, before dispatch, still returns a conforming body.
- [x] The taxonomy reserves `unauthorized`, `forbidden` and `not-found` so #42 extends it rather than
      redesigning it.

### Non-functional

- [x] No response body leaks a stack trace, an internal class name, a SQL fragment or a credential.
- [x] `detail` text is safe to show a user, or is a fixed generic string when it cannot be.
- [x] Adding a taxonomy entry requires no change to existing tests.

## Architecture

```text
shared/error/ProblemType.java           the taxonomy: code, title, default status
shared/error/ApiException.java          base domain exception carrying a ProblemType
shared/error/ApiExceptionHandler.java   @RestControllerAdvice extends ResponseEntityExceptionHandler
shared/error/ProblemDetailWriter.java   renders a body directly, for pre-dispatch failures
```

The taxonomy is an enum rather than a set of string constants because it must carry three coupled
values — code, human-readable title and default HTTP status — and an enum makes it impossible to
supply a code without the other two. The URN is derived from the code in one place, so the
`urn:rikkaus:problem:` prefix appears exactly once in the codebase.

`type` and `title` are set explicitly in the handler rather than through a `MessageSource` bundle.
Both mechanisms work. Explicit setting was chosen because a message bundle keyed by fully-qualified
exception class name couples the wire contract to Java class names, so renaming an exception class
would silently change the published contract. With an enum, the wire code is a deliberate, visible
value.

`ProblemDetailWriter` exists solely for the pre-dispatch gap. It serializes a `ProblemDetail` straight
to the `HttpServletResponse` with the right status and content type. Phase 4's filter calls it from its
own catch block. Without it, a filter failure returns the container's default HTML or an empty body,
and the frontend's error classifier — the thing the parent feature is graded on — sees an
unclassifiable response.

## Files to create and modify

- Create: `services/api/src/main/java/com/rikkaus/wealth/shared/error/ProblemType.java`
- Create: `services/api/src/main/java/com/rikkaus/wealth/shared/error/ApiException.java`
- Create: `services/api/src/main/java/com/rikkaus/wealth/shared/error/ApiExceptionHandler.java`
- Create: `services/api/src/main/java/com/rikkaus/wealth/shared/error/ProblemDetailWriter.java`
- Create: `services/api/src/test/java/com/rikkaus/testfixtures/ProblemFixtureController.java` — note the
  package is deliberately outside `com.rikkaus.wealth` so component scan cannot reach it
- Create: `services/api/src/test/java/com/rikkaus/wealth/shared/error/ApiExceptionHandlerTest.java`
- Create: `services/api/src/test/java/com/rikkaus/wealth/shared/error/ProblemDetailsContractIT.java`
This phase modifies no configuration file. The first draft added
`spring.mvc.problemdetails.enabled: true`; as established in the Overview, that key is dead
configuration once this advice registers, so it is deliberately not added.

## Implementation steps

1. **Record the baseline, without setting the property.** Call
   `curl -i http://localhost:8080/api/v1/does-not-exist` against the Phase 2 application and record
   the actual status, content type and body. This is the genuine before-state: no advice, no property,
   so expect the container's default error representation rather than Problem Details.

   Do **not** set `spring.mvc.problemdetails.enabled`. As established in the Overview, the advice this
   phase registers makes Boot's autoconfigured handler back off via
   `@ConditionalOnMissingBean(ResponseEntityExceptionHandler.class)`, so the property would be dead
   configuration. The first draft's plan to measure a property-only baseline would have recorded a
   state that never coexists with the delivered code.

2. **Define the taxonomy.** Start with the entries this MVP can actually produce, plus the three
   reserved for #42. Do not invent domain-specific entries for resources that do not exist.

   ```java
   package com.rikkaus.wealth.shared.error;

   import org.springframework.http.HttpStatus;
   import java.net.URI;

   public enum ProblemType {
       VALIDATION_FAILED("validation-failed", "Request validation failed", HttpStatus.BAD_REQUEST),
       MALFORMED_REQUEST("malformed-request", "Malformed request", HttpStatus.BAD_REQUEST),
       NOT_FOUND("not-found", "Resource not found", HttpStatus.NOT_FOUND),
       METHOD_NOT_ALLOWED("method-not-allowed", "Method not allowed", HttpStatus.METHOD_NOT_ALLOWED),
       UNSUPPORTED_MEDIA_TYPE("unsupported-media-type", "Unsupported media type", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
       NOT_ACCEPTABLE("not-acceptable", "Not acceptable", HttpStatus.NOT_ACCEPTABLE),
       UNAUTHORIZED("unauthorized", "Authentication required", HttpStatus.UNAUTHORIZED),
       FORBIDDEN("forbidden", "Access denied", HttpStatus.FORBIDDEN),
       INTERNAL_ERROR("internal-error", "Unexpected server error", HttpStatus.INTERNAL_SERVER_ERROR);

       private static final String URN_PREFIX = "urn:rikkaus:problem:";
       // fields, constructor, accessors; type() returns URI.create(URN_PREFIX + code)
   }
   ```

   `UNAUTHORIZED` and `FORBIDDEN` are declared but unreachable in this task. That is deliberate: #42
   must be able to throw them without editing this enum or renegotiating the contract with #38.

3. **Define the domain exception base.**

   ```java
   public class ApiException extends RuntimeException {
       private final ProblemType problemType;
       private final Map<String, Object> extensions;
       // constructors: (ProblemType, String detail), (ProblemType, String detail, Map extensions)
   }
   ```

   Feature slices throw either `ApiException` directly or a narrow subclass. They never build a
   `ProblemDetail` themselves, so the wire shape stays owned by one file.

4. **Write the handler.** Three responsibilities: map `ApiException`, enrich the framework-mapped
   bodies with a real `type` and `title`, and add the per-field `errors` member.

   ```java
   @RestControllerAdvice
   public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

       @ExceptionHandler(ApiException.class)
       ResponseEntity<Object> handleApiException(ApiException ex, WebRequest request) {
           ProblemDetail body = problem(ex.getProblemType(), ex.getMessage());
           ex.getExtensions().forEach(body::setProperty);
           return handleExceptionInternal(ex, body, new HttpHeaders(),
                   ex.getProblemType().status(), request);
       }

       @Override
       protected ResponseEntity<Object> handleMethodArgumentNotValid(
               MethodArgumentNotValidException ex, HttpHeaders headers,
               HttpStatusCode status, WebRequest request) {
           ProblemDetail body = ex.getBody();
           apply(body, ProblemType.VALIDATION_FAILED);
           body.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                   .map(fe -> Map.of("field", fe.getField(),
                                     "message", Objects.toString(fe.getDefaultMessage(), "invalid")))
                   .toList());
           return handleExceptionInternal(ex, body, headers, status, request);
       }

       @Override
       protected ProblemDetail createProblemDetail(Exception ex, HttpStatusCode status,
               String defaultDetail, String detailMessageCode, Object[] detailMessageArguments,
               WebRequest request) {
           ProblemDetail body = super.createProblemDetail(ex, status, defaultDetail,
                   detailMessageCode, detailMessageArguments, request);
           apply(body, ProblemType.forStatus(status));
           return body;
       }
   }
   ```

   Overriding `createProblemDetail` is what makes every framework-raised status carry a real `type`
   without writing one handler per exception. Add `ProblemType.forStatus(HttpStatusCode)` mapping
   status to taxonomy entry, defaulting to `INTERNAL_ERROR`.

   Also add a last-resort `@ExceptionHandler(Exception.class)` returning `INTERNAL_ERROR` with a fixed
   generic `detail`. It must log the real exception at error level and put nothing from it in the body.

   Add the `correlationId` extension member here, inside the shared `apply(...)` helper, reading it
   from MDC and setting it **only when non-null**. Use the string literal, not a constant — Phase 4's
   `CorrelationId.MDC_KEY` does not exist yet, so referencing it here would not compile:

   ```java
   private void apply(ProblemDetail body, ProblemType type) {
       body.setType(type.type());
       body.setTitle(type.title());
       String correlationId = MDC.get("correlationId");
       if (correlationId != null) {
           body.setProperty("correlationId", correlationId);
       }
   }
   ```

   This keeps `shared/error` owned entirely by this phase. Phase 4 populates MDC and makes exactly one
   edit here, replacing the literal with `CorrelationId.MDC_KEY`, which is recorded in the Phase 4
   todo. Until Phase 4 lands, MDC is empty and the member is simply absent, which is valid RFC 9457
   since extension members are optional.

   Use the same literal in `ProblemDetailWriter` (step 5) for the same reason, and Phase 4 replaces
   both occurrences together.

5. **Write `ProblemDetailWriter`.** Small, dependency-light, and usable from a filter that has no
   access to the MVC machinery. Two guards and one enrichment that the first draft omitted.

   ```java
   @Component
   public class ProblemDetailWriter {

       private static final Logger log = LoggerFactory.getLogger(ProblemDetailWriter.class);
       private final ObjectMapper objectMapper;

       public void write(HttpServletResponse response, ProblemType type, String detail) {
           if (response.isCommitted()) {
               log.warn("Response already committed; cannot render a problem body for {}", type);
               return;
           }
           try {
               response.resetBuffer();
               response.setStatus(type.status().value());
               response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
               ProblemDetail body = ProblemDetail.forStatusAndDetail(type.status(), detail);
               body.setType(type.type());
               body.setTitle(type.title());
               String correlationId = MDC.get("correlationId");
               if (correlationId != null) {
                   body.setProperty("correlationId", correlationId);
               }
               objectMapper.writeValue(response.getOutputStream(), body);
           } catch (IOException e) {
               log.error("Failed to write problem response for {}", type, e);
           }
       }
   }
   ```

   The `isCommitted` early return matters because `setStatus` and `setContentType` are silent no-ops
   on a committed response, and the JSON would then be *appended* to whatever body was already
   streaming. Without it, a failure after a 200 had begun writing would hand the client HTTP 200,
   `Content-Type: application/json`, and two concatenated JSON objects — which #38's Zod parser would
   report as malformed data rather than as a server error, with no correlation ID to trace. The
   `resetBuffer` call discards any buffered-but-uncommitted output for the same reason.

   Reading `correlationId` here keeps the filter-stage body consistent with the advice's bodies. It is
   the same read the advice does in step 4, so the member is present or absent identically on both
   paths.

   Inject the autoconfigured `ObjectMapper` rather than constructing one. Boot registers
   `ProblemDetailJacksonMixin` on it, and that is what makes extension members and RFC member names
   serialize correctly. A hand-built mapper would produce a body shaped differently from the advice's.

6. **Write the test fixture controller outside the component-scan root.** Production code carries no
   error-probe endpoint, per the Product Owner decision recorded on the plan index. But `src/test`
   placement alone does not achieve that, which was a Critical red-team finding.

   `RikkausWealthApplication` is `@SpringBootApplication` in package `com.rikkaus.wealth`, so its
   component scan covers `com.rikkaus.wealth.**` across the whole classpath — including
   `target/test-classes`. A `@RestController` at `com.rikkaus.wealth.support` would therefore be a live
   bean in **every** `@SpringBootTest` context, including Phase 5's `OpenApiContractIT`. With
   `springdoc.paths-to-match: /api/v1/**`, its three routes would be serialized into the committed
   `services/api/openapi/openapi.json`, publishing endpoints to #38 that do not exist in production —
   and the drift assertion could not detect it, because the fixture would appear in both the generated
   and the committed document.

   Two changes prevent it. Put the fixture in a package **outside** the scan root, and move its routes
   off `/api/v1`:

   ```java
   package com.rikkaus.testfixtures;   // NOT com.rikkaus.wealth.**

   @RestController
   @RequestMapping("/test-fixtures")   // NOT under /api/v1
   public class ProblemFixtureController {
       // POST /validated  -> takes a @Valid record with a @NotBlank field
       // GET  /domain     -> throws new ApiException(ProblemType.NOT_FOUND, "Fixture not found")
       // GET  /boom       -> throws new IllegalStateException("internal detail that must not leak")
   }
   ```

   Register it explicitly where it is needed, with `@Import(ProblemFixtureController.class)` on the
   slice tests and on `ProblemDetailsContractIT`. Being outside the scan root means it can never be
   picked up implicitly by any other context.

   The path change has a consequence worth stating: the published contract will document no
   request-accepting endpoint, so it carries no validation example for #38 this iteration. That is the
   honest outcome of keeping test scaffolding out of a published contract, and Phase 6 says so in the
   #38 notification rather than letting #38 discover it.

   Phase 5 additionally asserts that the generated `paths` set equals exactly `["/api/v1/meta"]`, so
   any future test controller that leaks in fails the build loudly instead of being committed.

7. **Write the slice tests.** One assertion group per status, because step 4's central question —
   whether Boot's autoconfiguration and this advice compose — is answered by observation, not by
   reasoning.

   Cover: 400 from bean validation including the `errors` array contents; 400 from malformed JSON;
   400 from a type mismatch on a path variable; 404 from an unknown `/api/v1` path; 405 from the wrong
   method on `/api/v1/meta`; 415 from a wrong `Content-Type`; 406 from an impossible `Accept`; the
   domain `ApiException` path; and the last-resort handler, asserting the body contains the generic
   detail and does **not** contain the string `internal detail that must not leak`.

   Every assertion group checks `Content-Type` is `application/problem+json` and `$.type` starts with
   `urn:rikkaus:problem:`.

   **Add a reflection test for every framework status, not only the last-resort path.** Red-team review
   found that Spring's own `detail` strings are not uniformly opaque: `TypeMismatchException` produces
   text of the form `Failed to convert 'x' with value: '<client input>'`, and `NoResourceFoundException`
   produces `No static resource <client path>.` — both reflect attacker-controlled input verbatim, and
   conversion failures can surface internal target type names. The first draft asserted "`detail` for
   framework errors comes from Spring's own safe defaults", which is not true.

   So for the statuses where Spring reflects input — 400 type-mismatch and 404 no-resource — override
   `detail` in `apply(...)` with the taxonomy entry's own fixed text rather than passing Spring's
   through. Then send a distinctive marker string (for example `REFLECTED-MARKER-9457`) as the offending
   input for each framework status and assert it does not appear anywhere in the response body. One
   test per status, not one test for the last-resort handler.

8. **Write `ProblemDetailsContractIT`.** The slice tests prove the handler; this proves the assembled
   application. Extend `AbstractPostgresIntegrationTest`, use `TestRestTemplate`, and assert the same
   shapes against a real running server for at least the 404, 405 and validation cases. This catches
   the class of failure where a filter, converter or content-negotiation setting changes the body in
   the full stack but not in a slice.

## Todo

- [x] Record the genuine baseline output without setting `spring.mvc.problemdetails`.
- [x] Create `ProblemType` with the nine entries and the single URN prefix.
- [x] Add `ProblemType.forStatus` with an `INTERNAL_ERROR` default.
- [x] Create `ApiException` with the extensions map.
- [x] Create `ApiExceptionHandler` with the `ApiException` handler, the validation override, the
      `createProblemDetail` override and the last-resort handler.
- [x] Override `detail` with fixed taxonomy text for the statuses where Spring reflects client input.
- [x] Confirm the last-resort handler logs the cause and leaks nothing into the body.
- [x] Create `ProblemDetailWriter` with the `isCommitted` guard, `resetBuffer`, the MDC read, and the
      autoconfigured `ObjectMapper`.
- [x] Create `ProblemFixtureController` in `com.rikkaus.testfixtures`, mapped off `/api/v1`.
- [x] Confirm the fixture package is outside the `@SpringBootApplication` scan root.
- [x] Register the fixture with `@Import` on the tests that need it.
- [x] Create `ApiExceptionHandlerTest` covering 400 validation, 400 malformed, 400 type-mismatch, 404,
      405, 415, 406, domain and last-resort.
- [x] Add a marker-reflection assertion for every framework status, not just last-resort.
- [x] Create `ProblemDetailsContractIT` covering 404, 405 and validation in the full stack.
- [x] Confirm no production file contains a test-fixture route.
- [x] Confirm `spring.mvc.problemdetails.enabled` appears nowhere in the repository.

## Verification

```bash
./services/api/mvnw -f services/api/pom.xml test -Dtest=ApiExceptionHandlerTest
./services/api/mvnw -f services/api/pom.xml test
./services/api/mvnw -f services/api/pom.xml verify
curl -i -s http://localhost:8080/api/v1/does-not-exist
curl -i -s -X POST http://localhost:8080/api/v1/meta
curl -i -s -H 'Accept: application/xml' http://localhost:8080/api/v1/meta
```

Pass conditions:

- Every error response carries `Content-Type: application/problem+json`.
- No error response has `"type": "about:blank"`.
- The validation case returns an `errors` array whose entries each have `field` and `message`.
- The last-resort case returns 500 with a generic detail and no occurrence of the seeded internal
  string anywhere in the body.
- `ProblemDetailsContractIT` passes against the Testcontainers PostgreSQL.

## Success criteria

- [x] A feature slice can throw one exception type and get a conforming, documented error response.
- [x] Every status the framework raises carries a project-owned `type` URN.
- [x] Validation failures are actionable per field, not just "400 Bad Request".
- [x] A pre-dispatch failure path exists and is ready for Phase 4's filter to use.
- [x] Whether Boot's autoconfiguration composes with this advice is settled by test output recorded
      in the phase notes, not by assumption.

## Risk assessment

| Risk | Signal it broke | Response |
|---|---|---|
| `createProblemDetail` is not the single funnel every framework status passes through, so some status keeps `about:blank`. | The 404, 405, 415 or 406 slice assertion on `$.type` fails while the domain-exception assertion passes. | Step 7's per-status tests find this. Add a targeted `@ExceptionHandler` override for whichever status bypasses the funnel, rather than assuming the funnel covers everything. Note this is no longer about the property shadowing the advice — that question is settled in the Overview. |
| `detail` still reflects client input on a status not covered by the override. | A marker-reflection assertion fails for a status the override missed. | Extend the override list. The test exists per status precisely so a missed one is visible rather than inferred. |
| `ProblemDetailWriter`'s `isCommitted` guard silently swallows a real error, so a failure returns a partial 200 body with no problem at all. | A post-commit failure produces a truncated success body and a warn log. | Accepted and logged at warn: once the response is committed, no correct HTTP outcome remains available. The log line is the recovery path, and Phase 4's test asserts the client receives a single well-formed body rather than two concatenated ones. |
| Spring Framework 7 moved or renamed MVC exception classes relative to Boot 3 examples, so the overrides do not compile. | Compilation failure on the override signatures. | Check the method signatures against the Spring Framework 7 `ResponseEntityExceptionHandler` Javadoc for the resolved version rather than adapting a Boot 3 snippet. |
| The last-resort `Exception` handler swallows something that should have produced a specific status, masking bugs as 500s. | A test that expected 404 gets 500. | Keep it strictly last, never catch `ApiException` or Spring MVC exceptions in it, and always log at error level with the stack trace so the real cause is recoverable from logs. |
| `detail` text from a domain exception leaks internal information because a developer passes an exception message straight through. | Review, or a test asserting absence of a seeded internal string. | The last-resort handler never uses the exception message. For `ApiException`, the `detail` is supplied deliberately by the thrower, and Phase 6 documents that it must be user-safe. |
| `ProblemDetailWriter` constructs its own `ObjectMapper` and produces a body that differs from the advice's. | The Phase 4 filter-failure test finds different member names or missing extensions. | Inject the autoconfigured `ObjectMapper` rather than constructing one, and assert the filter-stage body shape in Phase 4 rather than trusting it. |
| The fixture controller leaks into a Spring context anyway, because someone moves it back under `com.rikkaus.wealth` for tidiness. | Phase 5's `paths` allow-list assertion fails. | The package placement is the mechanism and Phase 5's assertion is the alarm. Say in the file's own class comment why the package is unusual, so the next author does not "fix" it. |
| `ProblemDetailWriter` is a plain `@Component`, so a `@WebMvcTest` that pulls in Phase 4's filter cannot satisfy its dependency. | Every slice test fails with `NoSuchBeanDefinitionException` after Phase 4 lands. | Phase 4 owns the fix by registering its filter so `@WebMvcTest` does not pick it up. Recorded here because the dependency originates in this phase. |

## Security considerations

Error bodies are an information-disclosure surface. Three rules are enforced here rather than left to
discipline: the last-resort handler never copies an exception message into the response, `detail` for
framework errors comes from Spring's own safe defaults, and stack traces are never serialized. Spring
Boot's `server.error.include-stacktrace` defaults to `never`, which this phase does not change.

The `errors` array reflects field names and validation messages, both of which the client supplied or
the project authored, so neither leaks server internals. Reserving `UNAUTHORIZED` and `FORBIDDEN`
without implementing them ensures #42 does not have to widen the contract under time pressure.

## Next steps

Phase 4 adds the correlation ID to every one of these bodies and to the logs, and uses
`ProblemDetailWriter` to close the pre-dispatch gap this phase identified.

## Implementation notes — 2026-09-27

### `createProblemDetail` is not the funnel; `handleExceptionInternal` is

The step-4 design overrides `createProblemDetail` to give every framework status a real `type`. On
Spring Framework 7.0.9 that covers only part of the surface, and the per-status tests the phase
insisted on are what exposed it rather than reasoning. Observed bodies with only that override in
place:

```
405  {"detail":"Method 'POST' is not supported.","instance":"...","status":405,"title":"Method Not Allowed"}
404  {"detail":"No static resource api/v1/MARKER.","instance":"...","status":404,"title":"Not Found"}
415  {"detail":"Content-Type 'text/plain;charset=UTF-8' is not supported.","status":415,"title":"Unsupported Media Type"}
400  {"detail":"Malformed request.","status":400,"title":"Malformed request","type":"urn:rikkaus:problem:malformed-request"}
```

Only the type-mismatch 400 was classified. `type` is absent from the other three because it was still
`about:blank`, which the Problem Details serializer omits — so the failure presented as a missing
member rather than as a wrong one.

The cause is that most built-in handlers call `handleExceptionInternal(ex, null, ...)` and let the base
implementation derive the `ProblemDetail` from the `ErrorResponse` itself. Any classification that
inspects the incoming `body` argument therefore sees `null` for exactly the statuses that matter.
Classification now happens in `handleExceptionInternal` **after** the superclass call, mutating the
returned body, which every path — framework and domain alike — genuinely passes through. The phase's
risk table anticipated a bypass and prescribed a targeted per-status override; moving to the real funnel
is the same fix without nine handlers to keep in sync.

### The reflecting-input override had to be scoped, or it ate domain detail

Applying the fixed taxonomy text for statuses 400 and 404 unconditionally also overwrote the `detail`
of a domain `ApiException(NOT_FOUND, "Fixture not found")`, which is the one `detail` a thrower chooses
on purpose. Handlers that classify a body themselves are now left alone, and only a body the funnel had
to classify gets its `detail` replaced. A test asserts the domain detail survives, so the two rules
cannot quietly merge again.

### `instance` legitimately contains caller input

The marker-reflection assertion originally scanned the whole body and failed on the 404 case, because
RFC 9457 defines `instance` as the request URI and a request for `/api/v1/REFLECTED-MARKER-9457` carries
the marker there by construction. Echoing a caller's own path back is not a disclosure. The assertion
now targets `detail` and `title`, which is where a probe value or an internal target type would actually
leak, and `instance` is asserted present rather than scanned.

### Two test-harness corrections

A bare `@WebMvcTest` scans every controller, so it pulled in `MetaController` and failed to refresh for
want of a `Clock` — a bean the slice deliberately excludes. The test is scoped to
`@WebMvcTest(ProblemFixtureController.class)`, with both the fixture and the advice supplied by
`@Import`.

The 406 fixture had to stop returning a `String`. `StringHttpMessageConverter` advertises `*/*`, so
`GET /test-fixtures/typed/1` with `Accept: application/pdf` returned HTTP 200 with
`Content-Type: application/pdf` and a body of `1`. The endpoint now returns a record, so only the JSON
converter applies and content negotiation genuinely fails.

### The deprecated container class was switched, and it did warn

Phase 1 recorded the legacy generic `org.testcontainers.containers.PostgreSQLContainer` as compiling
without complaint. Compiling the wider test set surfaced `uses or overrides a deprecated API` against
`AbstractPostgresIntegrationTest`, so this phase took Phase 1's documented fallback: the non-generic
`org.testcontainers.postgresql.PostgreSQLContainer`, with the diamond dropped. The warning is gone and
that import is what later phases should copy.

### Observed results

`spring.mvc.problemdetails` appears nowhere in the repository, confirmed by grep, and no configuration
file was modified by this phase. The pre-advice baseline is recorded separately: `application/json` with
Boot's `timestamp`/`status`/`error`/`path` shape, no `type`, no `title`, and a completely empty body on
the 406 case.

After the advice: nine slice assertions pass covering validation 400 with the per-field `errors` array,
malformed-JSON 400, type-mismatch 400, 404, 405, 415, 406, the domain path and the last-resort handler.
Every one carries `application/problem+json`, a `urn:rikkaus:problem:` type, a non-empty `title`,
`detail` and `instance`, and none echoes the marker into `detail` or `title`. The last-resort body
contains neither the seeded internal message nor the exception class name. Four full-stack assertions in
`ProblemDetailsContractIT` confirm the same shapes against a real server on the Testcontainers database.
The whole suite is 16 Surefire and 7 Failsafe tests, all green.
