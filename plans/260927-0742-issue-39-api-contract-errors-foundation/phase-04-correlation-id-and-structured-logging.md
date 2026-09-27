---
phase: 4
title: "Correlation ID and structured logging"
status: done
priority: P1
effort: "8h"
issue: 39
dependencies: [1, 3]
---

# Phase 4: Correlation ID and structured logging

## Goal

Give every request a correlation ID that appears in the response header, in every log line for that
request, and in every Problem Details body, with inbound client-supplied values validated before they
are trusted.

## Overview

`ARCHITECTURE_TECHNOLOGY_DECISIONS.md:239` requires that API logs be structured and include
correlation IDs without financial payloads. Nothing implements that today; a repo-wide search found
zero occurrences of `correlation`, `traceparent` or `X-Request-Id` in code.

There were two credible designs and this plan chose the smaller one. Micrometer Tracing, added through
a tracer bridge, would put `traceId` and `spanId` into MDC automatically and needs no filter code for
the log line itself. But it brings a tracing dependency, forces a propagation-format decision between
B3 and W3C `traceparent`, and — with no Zipkin, Tempo or Jaeger backend deployed — delivers nothing
beyond a random identifier in a log file. Paying a tracing library's weight for a random ID is the
wrong trade for a single service talking directly to PostgreSQL. A hand-written `OncePerRequestFilter`
needs no new dependency, since `OncePerRequestFilter` already arrives with
`spring-boot-starter-webmvc`.

The decision worth stating plainly is that accepting a client-supplied correlation ID is a real
security concern, not a theoretical one. An unvalidated header value that reaches a log line can carry
CRLF sequences to forge log entries, or be long enough to bloat storage, and SLF4J and Logback do not
sanitize MDC values. This phase validates against a strict allow-list and falls back to a generated
UUID rather than attempting to escape a malformed value — rejecting is safer than sanitizing because
there is no legitimate reason for a client to send anything outside the allowed character set.

This phase also closes the pre-dispatch gap Phase 3 identified. The filter itself runs before
`DispatcherServlet`, so an exception thrown inside it bypasses `ApiExceptionHandler` entirely and lands
on the container's default `/error` handling — the known upstream inconsistency
spring-projects/spring-boot#48392. The filter therefore catches its own failures and renders a
conforming body through Phase 3's `ProblemDetailWriter`.

## Context links

- Issue: [#39](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/39)
- Project rules: `ARCHITECTURE_TECHNOLOGY_DECISIONS.md:239` (structured logs with correlation IDs) and
  `:144` (never log passwords, tokens or financial payloads)
- Research: `plans/reports/researcher-260927-1427-problem-details-correlation-tests.md`, section 2
- Pre-dispatch gap: https://github.com/spring-projects/spring-boot/issues/48392
- Phase 3's writer: `services/api/src/main/java/com/rikkaus/wealth/shared/error/ProblemDetailWriter.java`
- Header exposed to the browser by Phase 2: `shared/api/CorsConfiguration.java`

## Requirements

### Functional

- [x] A request with no `X-Correlation-Id` receives a generated UUID, returned on the response.
- [x] A request with a well-formed `X-Correlation-Id` has that value echoed and used.
- [x] A malformed, empty, oversized or control-character-bearing inbound value is rejected and
      replaced with a generated one; the request still succeeds.
- [x] The correlation ID appears in every log line emitted during the request.
- [x] The correlation ID appears as the `correlationId` extension member on every Problem Details
      body, which Phase 3 already reads from MDC — including bodies rendered on the container's ERROR
      dispatch and bodies rendered by the filter itself.
- [x] MDC is cleared after every request, including when the handler throws.
- [x] An exception raised inside the filter returns `application/problem+json`, not the container
      default.
- [x] An exception raised **downstream** keeps the status and `type` Phase 3 assigned it; the filter
      never rewrites it.

### Non-functional

- [x] No new runtime dependency is added.
- [x] No password, token, credential or financial payload is written to the log context.
- [x] The validator rejects rather than escapes; no inbound value is transformed and then trusted.
- [x] The filter adds no measurable per-request allocation beyond one UUID and one MDC entry.

## Architecture

```text
shared/observability/CorrelationId.java                    header, MDC key, request attribute, validator
shared/observability/CorrelationIdFilter.java              resolve, publish, echo, clear
shared/observability/CorrelationIdFilterConfiguration.java FilterRegistrationBean, REQUEST + ERROR
application.yml                                            logging.pattern.console with %X{correlationId}
```

Constants and the validator are separated from the filter so tests can exercise validation directly
without constructing a servlet request, and so Phase 3's classes can reference `MDC_KEY` rather than a
duplicated literal.

The filter runs at the highest precedence, because a correlation ID that starts after some other filter
has already logged is useless for the lines it missed. Precedence has two consequences the design must
answer for. First, the filter's own failure must not take down the request, so its own work is wrapped
and rendered through `ProblemDetailWriter`. Second — and this is the part the first draft got wrong —
running outermost means `chain.doFilter` must **not** be wrapped in a catch, because everything
downstream, including the whole Phase 3 taxonomy and every future Spring Security exception, propagates
through this frame on its way to the advice.

It is registered through a `FilterRegistrationBean` rather than `@Component` so that `@WebMvcTest`,
which deliberately includes `jakarta.servlet.Filter` implementations, does not pull it into the slice
tests Phases 2 and 3 wrote.

The correlation ID lives in two places, not one. MDC is what the log pattern reads, and a request
attribute is what survives the container's ERROR dispatch after MDC has been cleared. Without the
attribute, an error routed to `/error` would be formatted with an empty MDC and the `correlationId`
member would vanish from precisely the responses a user would quote in a bug report.

MDC cleanup in a `finally` block is not optional hygiene. Tomcat pools request-handling threads, so a
leaked MDC entry is not merely stale — it attaches one request's correlation ID to an unrelated later
request on the same thread, silently corrupting the log correlation this phase exists to provide. Be
precise about the evidence, though: the unit tests prove cleanup on a single thread, not the absence of
cross-request bleed under real pooling.

## Files to create and modify

- Create: `services/api/src/main/java/com/rikkaus/wealth/shared/observability/CorrelationId.java`
- Create: `services/api/src/main/java/com/rikkaus/wealth/shared/observability/CorrelationIdFilter.java`
- Create: `services/api/src/main/java/com/rikkaus/wealth/shared/observability/CorrelationIdFilterConfiguration.java`
- Create: `services/api/src/test/java/com/rikkaus/wealth/shared/observability/CorrelationIdFilterTest.java`
- Create: `services/api/src/test/java/com/rikkaus/wealth/shared/observability/CorrelationIdContractIT.java`
- Modify: `services/api/src/main/resources/application.yml` — add the `logging.pattern` key only
- Modify: `services/api/src/main/java/com/rikkaus/wealth/shared/error/ApiExceptionHandler.java` — replace
  the inlined `"correlationId"` literal with `CorrelationId.MDC_KEY`, one line, no behavior change
- Modify: `services/api/src/main/java/com/rikkaus/wealth/shared/error/ProblemDetailWriter.java` — same
  one-line literal replacement

## Implementation steps

1. **Define the constants and the validator.**

   ```java
   package com.rikkaus.wealth.shared.observability;

   import java.util.UUID;
   import java.util.regex.Pattern;

   public final class CorrelationId {

       public static final String HEADER = "X-Correlation-Id";
       public static final String MDC_KEY = "correlationId";
       public static final String REQUEST_ATTRIBUTE = CorrelationId.class.getName() + ".id";

       // ASCII letters, digits and hyphen only, 8 to 64 characters. Rejects CR, LF, tabs,
       // control characters, spaces, and anything long enough to bloat a log line.
       private static final Pattern ALLOWED = Pattern.compile("^[A-Za-z0-9-]{8,64}$");

       public static String resolve(String inbound) {
           return isAcceptable(inbound) ? inbound : UUID.randomUUID().toString();
       }

       public static boolean isAcceptable(String candidate) {
           return candidate != null && ALLOWED.matcher(candidate).matches();
       }

       private CorrelationId() {}
   }
   ```

   The lower bound of 8 characters is deliberate: it rejects trivially short values that would collide
   across clients and make correlation useless. The upper bound of 64 accommodates a UUID with room to
   spare while capping log-line growth.

2. **Write the filter.** The structure below is prescriptive, and the placement of the `try` boundaries
   is the whole point of the design.

   ```java
   public class CorrelationIdFilter extends OncePerRequestFilter {

       private final ProblemDetailWriter problemDetailWriter;

       @Override
       protected boolean shouldNotFilterErrorDispatch() {
           return false;   // see step 3: the default of true is what loses the ID on /error
       }

       @Override
       protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
               FilterChain chain) throws ServletException, IOException {
           String correlationId;
           try {
               correlationId = resolveAndPublish(request, response);
           } catch (RuntimeException e) {
               // The filter's OWN work failed. Nothing downstream has run, so no MVC handler
               // will ever format this. Render it here and stop.
               logger.error("Correlation filter failed before dispatch", e);
               problemDetailWriter.write(response, ProblemType.INTERNAL_ERROR,
                       "The request could not be processed.");
               return;
           }
           try {
               chain.doFilter(request, response);   // NOT wrapped in a catch
           } finally {
               MDC.remove(CorrelationId.MDC_KEY);
           }
       }

       private String resolveAndPublish(HttpServletRequest request, HttpServletResponse response) {
           String existing = (String) request.getAttribute(CorrelationId.REQUEST_ATTRIBUTE);
           String correlationId = existing != null
                   ? existing
                   : CorrelationId.resolve(request.getHeader(CorrelationId.HEADER));
           request.setAttribute(CorrelationId.REQUEST_ATTRIBUTE, correlationId);
           MDC.put(CorrelationId.MDC_KEY, correlationId);
           response.setHeader(CorrelationId.HEADER, correlationId);
           return correlationId;
       }
   }
   ```

   Two things are deliberate. `chain.doFilter` carries **no** catch block, so every application
   exception propagates to `ApiExceptionHandler` exactly as Phase 3 designed. And the response header is
   set before the chain runs, because once the response commits — which happens as soon as the handler
   starts writing — headers can no longer be added.

   **This replaces a first-draft design that red-team review found unimplementable.** The first draft
   said to "catch `Exception` from `chain.doFilter` and re-throw unless the exception is not one the MVC
   stack will handle". That predicate cannot be evaluated: by the time an exception propagates out of
   `chain.doFilter`, `DispatcherServlet` has already had its chance and declined, so the answer is
   always "MVC will not handle it". An implementer writing the literal instruction would have caught and
   flattened every escaping exception into `INTERNAL_ERROR`, destroying Phase 3's taxonomy. Worse, this
   filter runs at highest precedence — **outside** the Spring Security chain #42 will add — so it would
   have rewritten #42's `AuthenticationException` and `AccessDeniedException` into 500s, reporting auth
   denials as server faults. Catching only the filter's own work removes the whole class of failure.

3. **Handle the ERROR dispatch, and stash the ID as a request attribute.** `OncePerRequestFilter`
   defaults `shouldNotFilterErrorDispatch()` to `true`, so the filter does **not** run when the
   container routes a failure to `/error` — which is precisely the post-commit and pre-dispatch class
   this phase exists to cover. Phase 3's `apply(...)` sets `correlationId` only when MDC is non-null, so
   the member would be silently absent from exactly the error bodies a user most needs an ID for, and
   the plan would have read that omission as "valid RFC 9457" rather than as a broken criterion.

   Overriding to `false` makes the filter run on the ERROR dispatch too. Because `MDC.remove` already
   fired on the original dispatch, the ID must survive somewhere else — hence
   `CorrelationId.REQUEST_ATTRIBUTE`. The attribute persists across dispatches on the same request, so
   `resolveAndPublish` re-reads it and re-populates MDC rather than minting a second ID for the same
   request. That is why `resolveAndPublish` checks the attribute before the header.

4. **Register the filter so it does not leak into slice tests.** Do **not** annotate it `@Component`.
   Red-team review found that `@WebMvcTest` deliberately includes `jakarta.servlet.Filter`
   implementations, so a `@Component` filter would be pulled into every slice test in the module and
   would drag in `ProblemDetailWriter`, which is a plain `@Component` and therefore *not* included —
   producing `NoSuchBeanDefinitionException` in Phase 2's and Phase 3's previously-green tests the
   moment this phase landed.

   Register it through a `FilterRegistrationBean` in an ordinary `@Configuration` instead:

   ```java
   @Configuration
   class CorrelationIdFilterConfiguration {
       @Bean
       FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter(ProblemDetailWriter writer) {
           var registration = new FilterRegistrationBean<>(new CorrelationIdFilter(writer));
           registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
           registration.addUrlPatterns("/*");
           registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ERROR);
           return registration;
       }
   }
   ```

   `setDispatcherTypes` must include `ERROR` for step 3's override to have anything to act on. The
   configuration class is not a `WebMvcConfigurer`, so `@WebMvcTest` does not include it.

   Note for #42: this filter sits at highest precedence, outside the Spring Security chain. #42 must
   register its security filters so that `ExceptionTranslationFilter` stays *inside* this one, and must
   add a test asserting a 401 and a 403 are not rewritten to 500.

5. **Set the log pattern.** Add to `application.yml`:

   ```yaml
   logging:
     pattern:
       console: "%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX} %5p [%X{correlationId:-no-correlation-id}] %logger{40} - %msg%n"
   ```

   The `:-no-correlation-id` default makes a missing ID visible rather than rendering an empty bracket,
   which distinguishes "outside a request" from "filter did not run".

   Note this is a readable console pattern, not JSON. `ARCHITECTURE_TECHNOLOGY_DECISIONS.md:239` calls
   for structured logs; a full JSON encoder is a deployment concern that belongs with #47, which owns
   the pipeline that would ship those logs. Phase 6 records that explicitly rather than letting the
   acceptance criterion be quietly read as satisfied.

6. **Replace the literals in Phase 3's classes.** Change the inlined `"correlationId"` string to
   `CorrelationId.MDC_KEY` in both `ApiExceptionHandler.apply(...)` and `ProblemDetailWriter.write(...)`.
   These are the only edits this phase makes outside its own package, they change no behavior, and they
   exist so the key is defined once.

7. **Write the unit tests.** `CorrelationIdFilterTest` uses `MockHttpServletRequest`,
   `MockHttpServletResponse` and `MockFilterChain`, so it needs no Spring context and runs in Surefire.

   Cover each case as its own test:
   - No inbound header: response header is present and matches the allow-list.
   - Well-formed inbound header: the exact value is echoed.
   - Inbound value containing `\r\n`: rejected, response header differs from the input.
   - Inbound value of 200 characters: rejected.
   - Inbound value of 3 characters: rejected.
   - Empty inbound value: rejected.
   - MDC contains the ID inside the chain, asserted from a custom `FilterChain` that reads MDC.
   - MDC is empty after the filter returns, both on success and when the chain throws.
   - **A `ProblemType.NOT_FOUND` raised downstream keeps its own 404 and its own `type` URN with the
     filter installed.** This is the taxonomy-regression guard. The first draft left it as an unchecked
     todo line while calling the risk the highest in the phase.
   - **A failure inside the filter's own work returns `application/problem+json`.** Force it by stubbing
     `ProblemDetailWriter` and making `response.setHeader` throw, or by injecting a resolver that throws.
   - **An ERROR dispatch still carries the correlation ID**, proving step 3's override and the request
     attribute work together.

   The CRLF test is the security-regression test. Name it so its purpose survives refactoring, for
   example `rejectsInboundValueContainingLineBreaksToPreventLogForging`.

8. **Run the cross-phase regression gate.** Run the full `./mvnw test` and confirm that Phase 2's
   `MetaControllerTest` and Phase 3's `ApiExceptionHandlerTest` still pass. This phase changes the bean
   graph that slice tests see, so "my own tests pass" is not sufficient evidence. The first draft had no
   cross-phase gate at all, which is how the `@WebMvcTest` filter-inclusion defect would have surfaced
   as "Phase 4 broke Phase 2" during integration.

9. **Write `CorrelationIdContractIT`.** Extend `AbstractPostgresIntegrationTest` and assert against a
   real running server that a successful `/api/v1/meta` call returns the header, that an error response
   carries the same value in both the header and the `correlationId` body member, and that a response
   which has already committed does not produce two concatenated JSON bodies. Asserting header and body
   come from one request is the point — it is what closes the debug loop the frontend needs.

## Todo

- [x] Create `CorrelationId` with the header name, MDC key, request attribute, allow-list and `resolve`.
- [x] Create `CorrelationIdFilter` with `chain.doFilter` **outside** any catch block.
- [x] Override `shouldNotFilterErrorDispatch()` to return `false`.
- [x] Read the request attribute before the header, so an ERROR dispatch reuses the same ID.
- [x] Set the response header before calling the chain, not after.
- [x] Register via `FilterRegistrationBean` with `DispatcherType.REQUEST` and `ERROR`; do **not** use
      `@Component`.
- [x] Implement the filter-own-failure rendering path via `ProblemDetailWriter`.
- [x] Add the `logging.pattern.console` key with the `no-correlation-id` default.
- [x] Replace the `"correlationId"` literal in both `ApiExceptionHandler` and `ProblemDetailWriter`.
- [x] Create the eleven `CorrelationIdFilterTest` cases, including the CRLF regression test, the
      taxonomy-preservation test, the filter-own-failure test and the ERROR-dispatch test.
- [x] Run the cross-phase regression gate: confirm Phase 2 and Phase 3 slice tests still pass.
- [x] Create `CorrelationIdContractIT` asserting header and body carry the same value and that a
      committed response does not yield two concatenated bodies.
- [x] Inspect real log output and confirm the ID appears on every line of one request.
- [x] Confirm no log line contains a credential, token or financial value.

## Verification

```bash
./services/api/mvnw -f services/api/pom.xml test -Dtest=CorrelationIdFilterTest
./services/api/mvnw -f services/api/pom.xml verify
docker compose --env-file .env.example up -d --wait postgres
./services/api/mvnw -f services/api/pom.xml spring-boot:run
curl -s -D - -o /dev/null http://localhost:8080/api/v1/meta | grep -i x-correlation-id
curl -s -D - -o /dev/null -H 'X-Correlation-Id: abcdef12-3456-7890' http://localhost:8080/api/v1/meta | grep -i x-correlation-id
curl -s -D - -o /dev/null -H 'X-Correlation-Id: short' http://localhost:8080/api/v1/meta | grep -i x-correlation-id
curl -s http://localhost:8080/api/v1/does-not-exist | jq -r '.correlationId'
```

Pass conditions:

- The first call returns a generated UUID in the header.
- The second echoes `abcdef12-3456-7890` exactly.
- The third returns a generated value, not `short`.
- The fourth prints a non-null correlation ID from the Problem Details body.
- The application log shows the same ID on every line produced by a single request.
- All eight filter unit tests pass, including the CRLF case.

## Success criteria

- [x] Every response and every log line for a request share one correlation ID.
- [x] Every Problem Details body carries that same ID, including bodies rendered on an ERROR dispatch
      and bodies rendered by the filter itself.
- [x] A hostile inbound header value cannot reach the log context or the response.
- [x] MDC cleanup happens in a `finally` and is verified on a single thread. Cross-request isolation
      under real thread pooling is argued from construction, not claimed as tested.
- [x] Phase 2's and Phase 3's slice tests still pass after this phase lands.
- [x] Phase 3's error taxonomy is unchanged: every status still returns its own code, not a 500.
- [x] No new dependency was added.

## Risk assessment

| Risk | Signal it broke | Response |
|---|---|---|
| The filter intercepts downstream application exceptions and flattens Phase 3's taxonomy into generic 500s. | Phase 3's 404 and 405 assertions start returning 500. | Structurally prevented: `chain.doFilter` carries no catch block at all. The step-7 taxonomy-preservation test is the alarm. Treat a taxonomy regression as blocking. |
| MDC leaks across pooled Tomcat threads, attaching one request's ID to another. | Two concurrent requests share an ID in logs. | The `finally` block is the mitigation. Be precise about what the unit test proves: `MockFilterChain` runs on the JUnit thread and can only show cleanup on one thread, not the absence of cross-request bleed. The first draft claimed the test proved pooled-thread safety; it does not. Real evidence would be a concurrent load check, which is out of scope here — so the honest claim is "cleanup is correct by construction and verified on one thread". |
| This filter runs outside the future Spring Security chain and rewrites #42's auth denials. | A 401 or 403 returns as 500 once #42 lands. | Step 4 records the constraint for #42: keep `ExceptionTranslationFilter` inside this filter, and add a test asserting 401 and 403 survive. Since `chain.doFilter` is not wrapped, the current design already propagates them correctly. |
| `FilterRegistrationBean` omits `DispatcherType.ERROR`, so the `shouldNotFilterErrorDispatch` override has no effect. | The ERROR-dispatch test fails and error bodies lose `correlationId`. | Both halves are required and both are in the todo. The test is what proves they were done together. |
| Setting the response header after the chain silently does nothing once the response is committed. | The header is absent on successful responses but present on errors. | Set it before `chain.doFilter`, as step 2 specifies. |
| The correlation ID is lost across `@Async` or a manually created executor, so background work logs without it. | Log lines from async work show `no-correlation-id`. | No async endpoint exists yet, so this is a forward-looking note rather than a present defect. When async arrives, wrap the executor with a `TaskDecorator` that copies `MDC.getCopyOfContextMap()` and clears it afterwards. Recorded here so the next author does not rediscover it. |
| A readable console pattern is mistaken for the structured JSON logging the architecture document calls for. | Acceptance review marks the logging criterion satisfied when it is only partly so. | Phase 6 records the distinction explicitly and attributes JSON encoding to #47. Do not claim full structured logging in this task. |
| Accepting any inbound ID lets a caller deliberately reuse another request's ID to confuse log analysis. | Duplicate IDs across unrelated requests in logs. | Accepted, and narrower than it looks: the value is opaque and carries no authority, so the worst case is self-inflicted log noise for that caller. Revisit if correlation IDs ever gain security meaning, which they must not. |

## Security considerations

The inbound header is untrusted input that reaches two sinks: the log file and the response header.
Both are protected by the same allow-list, applied before either write. Rejection rather than
escaping is the deliberate choice, because there is no legitimate client need for characters outside
`[A-Za-z0-9-]` and a rejected value costs the caller nothing — they still get a working request with a
generated ID.

The 64-character cap bounds log-line growth from a hostile caller. The 8-character floor prevents
useless collisions.

Nothing else is added to the log context. Per `ARCHITECTURE_TECHNOLOGY_DECISIONS.md:144`, passwords,
tokens and financial payloads must never be logged, and this phase deliberately does not introduce a
request-body or header-dump logger, which would be the easiest way to violate that rule by accident.

## Next steps

Phase 5 documents this header and the `correlationId` extension member in the published OpenAPI
contract, so the frontend's generated fixtures include it.

## Implementation notes — 2026-09-27

Implemented as designed. No correction to the phase's own design was needed, which is worth stating
plainly given how much of it was itself a correction of an earlier draft: `chain.doFilter` carries no
catch, the response header is set before the chain, `shouldNotFilterErrorDispatch()` returns false, the
identifier is stashed in a request attribute, and the filter is registered through a
`FilterRegistrationBean` rather than `@Component`.

### The cross-phase regression gate passed

This is the gate the phase exists to enforce, because the change alters the bean graph that slice tests
see rather than only adding files. With the filter registered, `MetaControllerTest` still reports 4 green
and `ApiExceptionHandlerTest` still reports 9. Neither was touched. The `FilterRegistrationBean`
registration is what keeps `@WebMvcTest` from pulling the filter — and with it the unsatisfiable
`ProblemDetailWriter` dependency — into those slices.

### Live evidence

A request with no inbound header received `X-Correlation-Id: 62741faa-7ea2-4e5e-83c0-6a5f7a195624`. A
request sending `abcdef12-3456-7890` had exactly that echoed. A request sending `short` received
`3d563a6d-041b-4551-9359-15699a2024cc` instead and still succeeded. A 404 with
`X-Correlation-Id: trace-me-0001` returned:

```json
{"detail":"Resource not found.","instance":"/api/v1/does-not-exist","status":404,
 "title":"Resource not found","type":"urn:rikkaus:problem:not-found","correlationId":"trace-me-0001"}
```

The log pattern is in effect and the `no-correlation-id` default does its job: startup lines render as
`INFO [no-correlation-id] c.r.wealth.RikkausWealthApplication - Starting ...`, which distinguishes
"outside a request" from "the filter did not run".

Proving the identifier reaches *every* line of a request needed a request that actually logs, since
nothing in this service logs at INFO per request. Re-running with
`--logging.level.org.springframework.web=DEBUG` and two tagged requests produced complete, unbroken
coverage: `trace-me-0001` appears on all five lines from `DispatcherServlet - GET "/api/v1/meta"` through
`Completed 200 OK`, and `trace-me-0002` on all eight lines of the 404 including
`ExceptionHandlerExceptionResolver - Using @ExceptionHandler ... ApiExceptionHandler` and
`Writing [ProblemDetail[type='urn:rikkaus:problem:not-found' ...]`.

**One line before the first request-scoped line carries no identifier**, and it is recorded rather than
glossed: `19:00:42.114 INFO [no-correlation-id] o.s.web.servlet.DispatcherServlet - Completed
initialization in 1 ms`. That is Spring's lazy servlet initialization, triggered by the first request but
emitted by container bootstrap outside the filter chain. It is not a per-request application line, and
every line after it at `19:00:42.147` onward is tagged. No log line contains a credential: grep for the
Compose password returns zero, as does a case-insensitive grep for password, secret or token.

### Browser verification of header readability

Repeated from Phase 2 now that the header exists. From the real Expo page at `http://localhost:8081`,
`fetch('http://localhost:8080/api/v1/meta')` returns 200 and JavaScript reads
`X-Correlation-Id: c7a562bb-28c1-420f-af5a-91ea2438043c`, so the CORS `exposedHeaders` entry works. A
cross-origin 404 returns header and body ids that are byte-identical
(`c0a4ebae-5f4e-4ed9-8d5e-a7336029fe1a`) together with `type: urn:rikkaus:problem:not-found`, which is
the debug loop the frontend needs, proven end to end rather than inferred.

`/actuator/health` succeeds cross-origin but its `X-Correlation-Id` reads as `null` from JavaScript. That
is the deliberate posture, not a defect: the actuator CORS mapping exposes no response headers. The
header is present on the wire; it is simply not readable by script on that surface. Recorded so a later
reader does not treat it as a regression.

### What is honestly not delivered

The console pattern is readable text, not JSON. The structured-logging requirement is therefore only
partly met and a JSON encoder belongs with the CI/CD task, which owns the pipeline that would ship those
logs. The acceptance report records this as partial.

MDC cleanup is verified on a single thread only. `MockFilterChain` runs on the JUnit thread, so the tests
show that the `finally` block clears the context, not that no bleed occurs across pooled Tomcat threads.
The honest claim is that cleanup is correct by construction and verified on one thread.
