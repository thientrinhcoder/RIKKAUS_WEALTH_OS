---
phase: 2
title: "Versioned API surface and meta resource"
status: done
priority: P1
effort: "7h"
issue: 39
dependencies: [1]
---

# Phase 2: Versioned API surface and meta resource

## Goal

Make `/api/v1` a real HTTP surface with one genuinely useful non-domain resource, `GET /api/v1/meta`,
and establish the conventions every later feature slice will copy.

## Overview

`/api/v1` is currently a string in a decision document and nothing more. This phase turns it into
code: a single prefix constant, a controller placed where the architecture document says controllers
belong, and a response record using exact types.

The meta resource exists because of a decision the Product Owner took during planning. With no
controller at all, `springdoc.paths-to-match: /api/v1/**` would publish a contract whose `paths`
object is empty, and the Frontend sibling
[#38](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/38) would have nothing to generate
mock fixtures from until #42 lands. A resource reporting API version, build version and server time
is real data the frontend can genuinely use to confirm which build it is talking to, and it is not a
placeholder or a fixture.

This phase also adds CORS. Red-team review settled what the first draft had left as an open
question: `README.md:137` **already documents** "backend CORS permits the browser origin" as a
precondition for the health screen, and no CORS configuration exists anywhere in `services/api`. So
CORS is not unrequested scope — it is a documented expectation that shipped code does not satisfy.

Review also found the first draft's mapping was too narrow to deliver on its own justification. It
covered `/api/v1/**` only, while the single existing browser consumer calls `/actuator/health`
(`apps/mobile/src/features/health/health-client.ts:40`). The Product Owner decided the mapping covers
both, with a fail-closed default. The first draft additionally contradicted itself: its prose promised
an empty default while its YAML shipped `http://localhost:8081`, which also violates
`ARCHITECTURE_TECHNOLOGY_DECISIONS.md:235` ("CORS uses an explicit environment-specific allowlist") —
a rule the first draft cited nowhere. Both are corrected below.

## Context links

- Issue: [#39](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/39)
- Path convention: `ARCHITECTURE_TECHNOLOGY_DECISIONS.md:47`, `:139`
- Feature-slice layout: `ARCHITECTURE_TECHNOLOGY_DECISIONS.md:118-137`
- Exact-decimal rule: `ARCHITECTURE_TECHNOLOGY_DECISIONS.md:141`
- Frontend consumer that will read this: `apps/mobile/src/features/health/health-client.ts`
- Frontend base URL: `apps/mobile/.env.example:1`

## Requirements

### Functional

- [x] `GET /api/v1/meta` returns HTTP 200 with `application/json` and the documented fields.
- [x] The `/api/v1` prefix exists as exactly one constant; no other file contains the literal.
- [x] The response reports the running application name, the API version, the build version and the
      current server time in UTC ISO-8601.
- [x] Server time comes from an injected `Clock`, so a test can assert an exact value.
- [x] A browser on the configured local web origin can call both `/api/v1/**` and `/actuator/health`
      successfully, and reads `X-Correlation-Id` from the former.
- [x] With no origin configured, no CORS header is emitted on either surface.
- [x] The controller sits in a package that satisfies Phase 1's controller-placement ArchUnit rule.

### Non-functional

- [x] The response record uses no `float` or `double`, satisfying Phase 1's exact-decimal rule.
- [x] CORS allows a configured origin list with no wildcard and no credentials.
- [x] The resource is unauthenticated by design, and exposes nothing beyond application name,
      version and time.

## Architecture

```text
shared/api/ApiPaths.java          the single source of "/api/v1"
shared/api/MetaController.java    GET /api/v1/meta
shared/api/MetaResponse.java      immutable response record
shared/api/CorsConfiguration.java implements WebMvcConfigurer; CORS only
shared/api/ClockConfiguration.java  the Clock bean, deliberately NOT a WebMvcConfigurer
```

**Why the `Clock` bean is in its own class.** Red-team review found that `@WebMvcTest` is not the
narrow slice the first draft assumed: Boot's `WebMvcTypeExcludeFilter` deliberately *includes*
`WebMvcConfigurer` and `jakarta.servlet.Filter` implementations. A single `ApiConfiguration
implements WebMvcConfigurer` carrying `@Bean Clock clock()` would therefore be loaded into
`MetaControllerTest`, colliding with the test's own `clock` bean — and since bean-definition
overriding is disabled by default, the test would abort with `BeanDefinitionOverrideException` before
a single assertion ran. Splitting the two configurations means the slice test loads the CORS
configurer (harmless) and supplies its own clock without a conflict.

The meta resource lives in `shared/api` rather than in a feature slice because it describes the API
itself, not a business capability. `ARCHITECTURE_TECHNOLOGY_DECISIONS.md:118-137` reserves `shared/`
for exactly this kind of cross-cutting concern, and putting it in a feature package would imply a
business owner it does not have.

`ApiPaths` matters more than its size suggests. Once thirty controllers exist, a single constant is
the difference between introducing `/api/v2` with one edit and grepping for a string literal across
the codebase.

The `Clock` bean is not ceremony. Without it the only way to test a timestamp is to assert it is
"close to now", which is a flaky test. With it, `MetaControllerTest` stubs a fixed instant through
`@MockitoBean` and asserts an exact serialized value, which also pins the serialization format itself.

## Files to create and modify

- Create: `services/api/src/main/java/com/rikkaus/wealth/shared/api/ApiPaths.java`
- Create: `services/api/src/main/java/com/rikkaus/wealth/shared/api/MetaController.java`
- Create: `services/api/src/main/java/com/rikkaus/wealth/shared/api/MetaResponse.java`
- Create: `services/api/src/main/java/com/rikkaus/wealth/shared/api/CorsConfiguration.java`
- Create: `services/api/src/main/java/com/rikkaus/wealth/shared/api/ClockConfiguration.java`
- Create: `services/api/src/test/java/com/rikkaus/wealth/shared/api/MetaControllerTest.java`
- Create: `services/api/src/main/resources/application-local.yml` — created here, with the local CORS
  origin; Phase 5 later appends the Swagger UI override
- Modify: `services/api/pom.xml` — add the `build-info` execution only
- Modify: `services/api/src/main/resources/application.yml` — add the CORS origin property only
- Modify: `.env.example` — document `API_ALLOWED_ORIGINS`
- Modify: `README.md` — add `API_ALLOWED_ORIGINS` to the backend environment section near
  `README.md:49-54`. This is the one README edit outside Phase 6, because an undocumented
  security-relevant variable shipping for a whole phase is worse than the ownership exception.

## Implementation steps

1. **Define the prefix constant.**

   ```java
   package com.rikkaus.wealth.shared.api;

   public final class ApiPaths {
       public static final String V1 = "/api/v1";
       private ApiPaths() {}
   }
   ```

2. **Enable build information.** `BuildProperties` is only available if the Maven plugin writes it.
   Add the execution to the existing `spring-boot-maven-plugin` in `services/api/pom.xml`:

   ```xml
   <executions>
       <execution>
           <goals><goal>build-info</goal></goals>
       </execution>
   </executions>
   ```

   This writes `META-INF/build-info.properties` including `build.time`. Open question 2 on the plan
   index asks whether exposing a build timestamp on an unauthenticated endpoint is acceptable. Until
   it is answered, expose `buildVersion` only and do not surface `build.time`.

3. **Define the response record.** Use `Instant` for time and `String` for versions. No numeric
   money field exists here, but the record must still not introduce `double` anywhere, because
   Phase 1's rule scans the whole package.

   ```java
   package com.rikkaus.wealth.shared.api;

   import java.time.Instant;

   public record MetaResponse(
           String application,
           String apiVersion,
           String buildVersion,
           Instant serverTime) {}
   ```

   Confirm Jackson serializes `Instant` as an ISO-8601 string rather than an epoch number. Spring
   Boot's default `spring.jackson.serialization.write-dates-as-timestamps=false` should give that,
   but the slice test in step 6 asserts the exact string so the behavior is pinned rather than
   assumed.

4. **Write the controller.** Keep it thin; it reads three values and returns them.

   ```java
   @RestController
   @RequestMapping(ApiPaths.V1)
   class MetaController {

       private final Clock clock;
       private final String applicationName;
       private final String buildVersion;

       // constructor injects Clock, spring.application.name, and BuildProperties
       // (ObjectProvider<BuildProperties>, since build-info may be absent in an IDE run)

       @GetMapping("/meta")
       MetaResponse meta() {
           return new MetaResponse(applicationName, "v1", buildVersion, clock.instant());
       }
   }
   ```

   Inject `BuildProperties` through `ObjectProvider` and fall back to `"unknown"` when absent.
   Running the application from an IDE without the Maven `build-info` goal is a normal development
   path and must not fail context startup.

   Do not add OpenAPI annotations yet. Phase 5 owns documentation annotations, and adding them here
   would mean two phases editing the same file.

5. **Configure the `Clock` in its own non-web configuration.**

   ```java
   @Configuration
   class ClockConfiguration {
       @Bean
       Clock clock() {
           return Clock.systemUTC();
       }
   }
   ```

   Keep this class free of `WebMvcConfigurer`, or `@WebMvcTest` will load it and collide with the
   slice test's clock.

6. **Configure CORS, fail-closed, covering both surfaces.**

   ```java
   @Configuration
   class CorsConfiguration implements WebMvcConfigurer {

       private final List<String> allowedOrigins;   // bound from configuration; empty by default

       @Override
       public void addCorsMappings(CorsRegistry registry) {
           if (allowedOrigins.isEmpty()) {
               return;   // fail closed: configure nothing rather than guess an origin
           }
           String[] origins = allowedOrigins.toArray(String[]::new);
           registry.addMapping(ApiPaths.V1 + "/**")
                   .allowedOrigins(origins)
                   .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE")
                   .allowedHeaders("Content-Type", "X-Correlation-Id")
                   .exposedHeaders("X-Correlation-Id")
                   .allowCredentials(false);
           registry.addMapping("/actuator/health")
                   .allowedOrigins(origins)
                   .allowedMethods("GET")
                   .allowCredentials(false);
       }
   }
   ```

   The `/actuator/health` mapping exists because that is the only endpoint the shipped browser client
   calls (`apps/mobile/src/features/health/health-client.ts:40`) and because `README.md:137` already
   documents CORS as a precondition for it. Omitting it would leave a documented, shipped feature
   broken while Phase 6 recorded the client criterion as satisfied.

   Add to `application.yml` with a genuinely empty default:

   ```yaml
   rikkaus:
     api:
       allowed-origins: ${API_ALLOWED_ORIGINS:}
   ```

   And to the new `application-local.yml`:

   ```yaml
   rikkaus:
     api:
       allowed-origins: http://localhost:8081
   ```

   A deployment that sets nothing now permits nothing, which is what
   `ARCHITECTURE_TECHNOLOGY_DECISIONS.md:235` requires of an environment-specific allowlist. Document
   `API_ALLOWED_ORIGINS` in `.env.example` and in the README's backend environment section, since an
   operator reading either must be able to discover the knob.

   Confirm the actual Expo web port rather than assuming it. `apps/mobile/package.json:38` is a bare
   `expo start --web` with no pinned port, so `8081` is Expo's default, not a guarantee. Start the web
   app, read the origin it prints, and use that value.

   `allowCredentials(false)` is deliberate. #42 introduces tokens, and whether credentials cross
   origins belongs with the authentication design. Exposing `X-Correlation-Id` is required for the
   browser client to read the value Phase 4 sets.

7. **Write the slice test.** Supply the clock with `@MockitoBean` rather than a competing `@Bean`, so
   no bean-definition conflict is possible regardless of what `@WebMvcTest` decides to include.

   ```java
   @WebMvcTest(MetaController.class)
   class MetaControllerTest {

       @Autowired MockMvc mockMvc;
       @MockitoBean Clock clock;

       @BeforeEach
       void fixTheClock() {
           given(clock.instant()).willReturn(Instant.parse("2026-09-27T07:42:00Z"));
       }

       @Test
       void returnsApiMetadata() throws Exception {
           mockMvc.perform(get("/api/v1/meta"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                   .andExpect(jsonPath("$.apiVersion").value("v1"))
                   .andExpect(jsonPath("$.application").value("rikkaus-wealth-api"))
                   .andExpect(jsonPath("$.serverTime").value("2026-09-27T07:42:00Z"));
       }
   }
   ```

   Import `@WebMvcTest` from `org.springframework.boot.webmvc.test.autoconfigure`, not the Boot 3
   package — Phase 1 adds the `spring-boot-webmvc-test` module that provides it. Confirm
   `@MockitoBean` against the `spring-boot-test` 4.1.1 Javadoc; `@MockBean` is removed in the Boot 4
   line.

   Add a second test asserting the path is exactly `/api/v1/meta` and that `/meta` without the
   prefix returns 404, which pins the prefix wiring rather than the controller method alone.

## Todo

- [x] Create `ApiPaths` with the single `V1` constant.
- [x] Add the `build-info` execution to `spring-boot-maven-plugin`.
- [x] Create `MetaResponse` using `Instant` and `String` only.
- [x] Set `spring.jackson.serialization.write-dates-as-timestamps: false` explicitly.
- [x] Create `MetaController` with `ObjectProvider<BuildProperties>` and the fallback.
- [x] Create `ClockConfiguration` that does **not** implement `WebMvcConfigurer`.
- [x] Create `CorsConfiguration` covering `/api/v1/**` and `/actuator/health`, returning early when
      the origin list is empty.
- [x] Add `rikkaus.api.allowed-origins` to `application.yml` with an **empty** default.
- [x] Create `application-local.yml` carrying the local origin.
- [x] Confirm the real Expo web origin rather than assuming port 8081.
- [x] Document `API_ALLOWED_ORIGINS` in `.env.example` and the README environment section.
- [x] Create `MetaControllerTest` using `@MockitoBean Clock` and the exact-timestamp assertion.
- [x] Add the prefix-wiring test asserting `/meta` alone is 404.
- [x] Confirm the ArchUnit controller-placement rule from Phase 1 still passes.
- [x] Confirm `./mvnw test` passes, proving no duplicate `clock` bean in the slice context.
- [x] Verify CORS end to end from the running Expo web app for **both** `/api/v1/meta` and
      `/actuator/health`, not only from a unit test.
- [x] Confirm that with no `API_ALLOWED_ORIGINS` set, no `Access-Control-Allow-Origin` is returned.

## Verification

```bash
./services/api/mvnw -f services/api/pom.xml test
./services/api/mvnw -f services/api/pom.xml verify
docker compose --env-file .env.example up -d --wait postgres
./services/api/mvnw -f services/api/pom.xml spring-boot:run
curl -s http://localhost:8080/api/v1/meta | jq .
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/meta
curl -s -D - -o /dev/null -H 'Origin: http://localhost:8081' http://localhost:8080/api/v1/meta | grep -i access-control
curl -s -D - -o /dev/null -H 'Origin: http://localhost:8081' http://localhost:8080/actuator/health | grep -i access-control
```

Run the CORS checks twice: once with the `local` profile active, and once with no
`API_ALLOWED_ORIGINS` set and no `local` profile.

Pass conditions:

- `/api/v1/meta` returns 200 with all four fields populated and `serverTime` as an ISO-8601 string
  ending in `Z`.
- `/meta` returns 404.
- With the local profile, both `/api/v1/meta` and `/actuator/health` carry
  `Access-Control-Allow-Origin` for the real Expo origin, and `/api/v1/meta` also carries
  `Access-Control-Expose-Headers` including `X-Correlation-Id`.
- With no origin configured, **neither** endpoint returns any `Access-Control-*` header.
- `buildVersion` reports `0.0.1-SNAPSHOT` when run from the Maven build, and `unknown` when run
  without `build-info`.
- The ArchUnit controller-placement rule passes, and `./mvnw test` shows `MetaControllerTest`
  executing its assertions rather than failing at context load.

## Success criteria

- [x] `/api/v1` is a real, reachable surface with exactly one prefix definition.
- [x] The meta resource returns real runtime data, with no hardcoded or fabricated value.
- [x] A browser on the configured origin can read the resource and its correlation header.
- [x] The timestamp format is pinned by an exact assertion rather than a tolerance.

## Risk assessment

| Risk | Signal it broke | Response |
|---|---|---|
| `BuildProperties` is absent when the app runs outside the Maven build, failing context startup. | `NoSuchBeanDefinitionException` on IDE run. | The `ObjectProvider` plus `"unknown"` fallback is the mitigation. Test both paths, not just the Maven one. |
| Jackson serializes `Instant` as an epoch number, breaking the frontend's Zod schema. | The exact-timestamp assertion in step 6 fails. | Set `spring.jackson.serialization.write-dates-as-timestamps: false` explicitly rather than relying on the default. |
| CORS is read as outside #39's stated scope. | Review comment on the PR. | `README.md:137` already documents CORS as a precondition and no implementation exists, so this closes a documented gap rather than adding scope. The Product Owner confirmed the decision during red-team adjudication. It remains isolated to one configuration class and one property. |
| A wildcard origin creeps in during local debugging and reaches production. | `allowedOrigins("*")` appears in a diff. | The property defaults to empty, the code takes a configured list and never a literal wildcard, and an empty list registers no mapping at all. Phase 6 documents that production origins are #42's decision. |
| The `Clock` bean and a slice test's clock collide because `@WebMvcTest` includes `WebMvcConfigurer` implementations. | `MetaControllerTest` fails at context load with `BeanDefinitionOverrideException`. | The split into `ClockConfiguration` and `CorsConfiguration` plus `@MockitoBean Clock` in the test removes both halves of the collision. This was a confirmed red-team finding, not a hypothetical. |
| Expo web does not serve on 8081, so the configured origin is wrong and the browser check fails for a reason unrelated to the server. | Preflight returns no matching origin despite correct configuration. | Step 6 requires reading the real origin from the running web app rather than assuming Expo's default. |
| The meta resource is read as unrequested scope. | Review comment. | It was an explicit Product Owner decision during planning, recorded in the plan index decision table. Point at that rather than re-arguing it. |

## Security considerations

The resource is unauthenticated because #39 adds no authentication and the data is not sensitive:
application name, an API version string, a build version and the current time. It exposes no schema,
no user data and no configuration. The build timestamp is deliberately withheld pending open
question 3, since build times can help an attacker correlate a deployment with a known CVE window.

CORS is the one real widening here, and it is narrowed four ways: the origin list is configured with
a genuinely empty default, an empty list registers no mapping at all rather than guessing, credentials
are refused, and allowed headers are restricted to the two the client needs. This satisfies
`ARCHITECTURE_TECHNOLOGY_DECISIONS.md:235`, which requires an explicit environment-specific allowlist.

The `/actuator/health` mapping is narrower than the `/api/v1/**` one on purpose: `GET` only, no
custom headers, no exposed headers. `application.yml:22-31` already restricts actuator exposure to
`health` with `show-details: never`, so the response body carries a status and nothing else.

Note for #42: `allowCredentials(false)` is the current posture only because no credential exists yet.
Turning it on is a real decision with real consequences, and it must be made with the authentication
design rather than inherited from here as already settled.

## Next steps

Phase 3 gives this surface its error contract. The meta resource's own error paths — wrong method,
unacceptable media type, unknown sub-path — become the first real assertions of RFC 9457 conformance.

## Implementation notes — 2026-09-27

### Two Boot 4 corrections the plan's code samples predate

**The Jackson property key moved.** The step-3 instruction to set
`spring.jackson.serialization.write-dates-as-timestamps: false` is a Boot 3 key. Setting it on Boot
4.1.1 does not merely fail to take effect — it aborts context startup, because
`JacksonProperties.getSerialization()` binds to `Map<tools.jackson.databind.SerializationFeature,
Boolean>` and Jackson 3 moved `WRITE_DATES_AS_TIMESTAMPS` out of `SerializationFeature` into
`tools.jackson.databind.cfg.DateTimeFeature`. Every `MetaControllerTest` case failed with "Could not
bind properties to 'JacksonProperties' : prefix=spring.jackson" until the key became
`spring.jackson.datatype.datetime.write-dates-as-timestamps`, confirmed against
`JacksonProperties$Datatype.getDatetime()` and the `DateTimeFeature` constant list in
`jackson-databind-3.1.5.jar`.

**A `WebMvcConfigurer` cannot give `/actuator/health` CORS headers.** The step-6 code registers an
`/actuator/health` mapping on the `CorsRegistry`, and a live request proved it emits nothing: with
the `local` profile active and `/api/v1/meta` correctly returning
`Access-Control-Allow-Origin: http://localhost:8081`, the health endpoint returned no
`Access-Control-*` header at all. Actuator endpoints are served by their own handler mapping with its
own CORS configuration, which the MVC registry never reaches.

This is exactly the failure the red-team review predicted — a browser step that exercises only
`/api/v1/meta` passing while the one consumer that actually exists stays broken — so it is recorded
rather than quietly patched. The dead registry mapping was removed instead of left in place, because a
mapping that provably emits nothing is worse than none: a later reader would take it as proof the
surface is covered. `management.endpoints.web.cors.allowed-origins` now reads
`${rikkaus.api.allowed-origins}`, so one variable still drives both surfaces and the health mapping
stays narrower — `GET` only, no custom request headers, no exposed headers. Boot registers no actuator
CORS configuration when that list is empty, so the fail-closed default holds on both surfaces by
construction.

### The Expo origin was read, not assumed

`npx expo start --web` in `apps/mobile` printed `Waiting on http://localhost:8081`, confirmed by
`lsof -nP -iTCP:8081 -sTCP:LISTEN` and an HTTP 200. That is the value in `application-local.yml` and
`.env.example`. Expo's documented default and this project's actual origin happen to agree, but the
value is now evidence rather than an assumption.

### Observed results

With the `local` profile: `/api/v1/meta` returns 200 and
`{"application":"rikkaus-wealth-api","apiVersion":"v1","buildVersion":"0.0.1-SNAPSHOT","serverTime":"2026-09-27T11:44:09.956700Z"}`,
so `build-info` works and `Instant` serializes as an ISO-8601 string ending in `Z`. `/meta` returns
404, pinning the prefix wiring. Both surfaces carry `Access-Control-Allow-Origin:
http://localhost:8081`, `/api/v1/meta` additionally carries `Access-Control-Expose-Headers:
X-Correlation-Id`, and the health preflight returns `Access-Control-Allow-Methods: GET`. A request
from `https://evil.example` is rejected with HTTP 403.

With no profile and no `API_ALLOWED_ORIGINS`: neither endpoint emits any `Access-Control-*` header,
and both still answer ordinary non-CORS requests with 200. This also confirms that an empty property
value binds to an empty list on both the hand-parsed API allowlist and Boot's actuator property,
rather than to a list holding one empty entry.

The browser check was done for real, not inferred from a slice test. With the API on the `local`
profile and Expo web served at `http://localhost:8081`, a `fetch` from that page returned 200 and a
parsed body for **both** `/api/v1/meta` and `/actuator/health`. Reading `X-Correlation-Id` from
JavaScript returned `null`, which is correct at this point because Phase 4 is what sets the header;
the exposure header is already in place, and the read is repeated once Phase 4 lands.

`./mvnw test` reports seven tests green across `MetaControllerTest` and `ArchitectureRulesTest`, so no
duplicate `clock` bean appears in the slice context and the controller-placement rule — which had no
class to check in Phase 1 — now passes against a real controller.

### Scope note

The `BuildProperties` fallback is covered by a unit test that builds the controller with a genuinely
empty `ObjectProvider` from a bare `DefaultListableBeanFactory`, rather than by trying to suppress the
`build-info` goal in a live run. The goal binds to `generate-resources`, so `spring-boot:run` always
regenerates the file and the live path cannot exercise the absent-bean branch at all.
