---
phase: 1
title: "Test harness and integration baseline"
status: done
priority: P1
effort: "12h"
issue: 39
dependencies: []
---

# Phase 1: Test harness and integration baseline

## Goal

Give `services/api` a working two-tier test harness — Surefire for database-free unit and slice
tests, Failsafe with Testcontainers PostgreSQL for integration tests — repair the missing Flyway
autoconfiguration this project has been shipping without, and prove both by booting the real
application context against a container.

## Overview

Nothing in this plan can be verified before this phase lands. `services/api/pom.xml` has no test
dependency of any kind and `services/api/src/test` does not exist, so there is no place to put an
assertion and no command that runs one.

Red-team review of the first draft of this phase found five load-bearing dependency facts that were
already false, every one of them a consequence of Spring Boot 4 modularizing autoconfiguration and
Testcontainers 2 renaming its modules. They are corrected below and each correction carries the
evidence that settled it. The lesson worth carrying into implementation: Maven Central was consulted
for *versions* and not for *coordinates, class locations, or module boundaries*, and that is exactly
where Boot 4 moved things.

The most consequential finding is not about this plan at all. **Flyway has never run in this
service.** `FlywayAutoConfiguration` lives in `org.springframework.boot:spring-boot-flyway`, which no
starter this project declares brings in, so `spring.flyway.*` in `application.yml` is inert
configuration. Nobody noticed because no `@Entity` exists, which makes `ddl-auto: validate` a no-op,
so the application starts cleanly while silently skipping migrations. Issue #35's own acceptance box
"Startup evidence shows Flyway successfully applied `V1`" is still unchecked, which is consistent
with it never having been verified. The Product Owner decided during planning that this phase repairs
it, because #39 cannot honestly claim a Flyway-and-Testcontainers integration baseline on top of a
Flyway that does not run.

This phase also adds ArchUnit rules for two rules the architecture document states and nothing
enforces. Writing them while only `shared/` exists means they pass trivially today and fail the first
time a later slice violates them, which is the point.

## Context links

- Issue: [#39](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/39)
- Feature-slice rule: `ARCHITECTURE_TECHNOLOGY_DECISIONS.md:135`
- Exact-decimal rule: `ARCHITECTURE_TECHNOLOGY_DECISIONS.md:141`
- Test stack decision: `ARCHITECTURE_TECHNOLOGY_DECISIONS.md:110-116`
- README claims this phase invalidates, corrected in Phase 6: `README.md:156-158` and `README.md:160-163`
- Research: `plans/reports/researcher-260927-1427-problem-details-correlation-tests.md`, section 3
- Existing state: `plans/reports/scout-260927-1427-issue-39-api-foundation.md`, section 7

## Corrected dependency facts

Each row was verified against Maven Central during red-team adjudication. The "first draft assumed"
column records what was wrong, so an implementer who finds a stale copy of this plan elsewhere knows
which version is authoritative.

| Need | Correct coordinate | First draft assumed | Evidence |
|---|---|---|---|
| Testcontainers PostgreSQL module | `org.testcontainers:testcontainers-postgresql` | `org.testcontainers:postgresql` | Bare `postgresql` tops out at **1.21.4** and has no 2.x release; `testcontainers-postgresql` is at **2.0.5**. `testcontainers-bom:2.0.5` contains **zero** entries for the bare name and one for the prefixed name, so the bare coordinate resolves to no managed version at all. |
| Testcontainers JUnit 5 module | `org.testcontainers:testcontainers-junit-jupiter` | `org.testcontainers:junit-jupiter` | Same rename; bare name stops at 1.21.4. |
| Flyway autoconfiguration | `org.springframework.boot:spring-boot-flyway` (compile scope) | assumed transitive | `spring-boot-autoconfigure-4.1.1.jar` has **zero** Flyway entries. `FlywayAutoConfiguration` and `FlywayContainerConnectionDetailsFactory` are in `spring-boot-flyway-4.1.1.jar`. None of `spring-boot-starter`, `-starter-jdbc`, `-starter-data-jpa`, `-starter-webmvc` or `-starter-actuator` mentions flyway. |
| `@WebMvcTest`, `MockMvc` | `org.springframework.boot:spring-boot-webmvc-test` (test scope) | assumed in `spring-boot-starter-test` | `spring-boot-starter-test-4.1.1.pom` declares only `spring-boot-test`, `spring-boot-test-autoconfigure`, assertj, junit-jupiter, mockito, jsonassert, spring-test, xmlunit, hamcrest, json-path. `@WebMvcTest` is at `org.springframework.boot.webmvc.test.autoconfigure`. |
| `TestRestTemplate` | `org.springframework.boot:spring-boot-resttestclient` (test scope) | assumed in `spring-boot-starter-test` | The class is at `org.springframework.boot.resttestclient.TestRestTemplate`, and the module also ships `AutoConfigureTestRestTemplate`, which `@SpringBootTest` no longer implies. |
| Everything else in the test stack | version-free, BOM-managed | correct | Boot 4.1.1 BOM: junit-jupiter 6.0.3, AssertJ 3.27.7, Mockito 5.23.0, Byte Buddy 1.18.11, Flyway 12.4.0, Testcontainers 2.0.5. |
| ArchUnit | `com.tngtech.archunit:archunit-junit5:1.5.1` (explicit version) | correct | Absent from the Boot BOM; 1.5.1 is Central's current release. |

## Prerequisite gate

A hard gate. The machine this plan was written on has no JDK, so no command below has been executed.

1. Install Eclipse Temurin JDK 25 and confirm `java -version` reports 25.
2. Confirm `docker version` exits 0. It did during planning (Docker Desktop 27.3.1, Engine 27.3.1).
3. Confirm the current build still works before changing anything:
   `./services/api/mvnw -f services/api/pom.xml clean package`.
4. **Record the Flyway defect before repairing it.** Start the database and the current application,
   then query for the history table:

   ```bash
   docker compose --env-file .env.example up -d --wait postgres
   ./services/api/mvnw -f services/api/pom.xml spring-boot:run   # in another shell
   docker compose --env-file .env.example exec -T postgres \
       psql -U rikkaus -d rikkaus -c "\dt flyway_schema_history"
   docker compose --env-file .env.example exec -T postgres \
       psql -U rikkaus -d rikkaus -c "\dn wealth"
   ```

   The expected result before the fix is that neither exists and the startup log contains no Flyway
   banner. Capture that output. It is the evidence that the repair in step 1 of the implementation is
   a real fix rather than a no-op, and Phase 6 cites it when correcting the README. If Flyway *does*
   run, stop: the premise of this correction is wrong and the phase must be re-planned.
5. After the dependency edits, confirm resolution: `./mvnw dependency:tree`.

If step 5 fails, report it. Do not relax a pinned version. Do check the coordinate against the table
above first, since a coordinate typo is the most likely cause and is not a version problem.

## Requirements

### Functional

- [x] Flyway autoconfiguration is present and `V1__baseline.sql` demonstrably applies, both at runtime
      and in tests.
- [x] `./mvnw test` runs unit and slice tests and requires neither Docker nor PostgreSQL.
- [x] `./mvnw verify` additionally runs `*IT` classes against a Testcontainers PostgreSQL.
- [x] One base class supplies the container and its datasource wiring, so no integration test
      duplicates container setup or hardcodes credentials.
- [x] The container image tag matches the Compose service tag.
- [x] Exactly one container is started for the whole `verify` run, and it is not stopped between
      `*IT` classes.
- [x] An integration test fails loudly if it is not actually talking to the container.
- [x] ArchUnit enforces feature-slice isolation, controller placement and the exact-decimal rule.

### Non-functional

- [x] Every test-stack version except ArchUnit comes from the Boot 4.1.1 BOM with no hand-pinned
      `<version>`.
- [x] No credential appears in any test resource file.
- [x] `application-test.yml` fails closed rather than falling back to a reachable local database.

## Architecture

```text
pom.xml                                        dependency and phase-binding declarations
support/AbstractPostgresIntegrationTest.java   the one place a container is defined
src/test/resources/application-test.yml        fail-closed profile overrides
```

Splitting Surefire from Failsafe by naming convention keeps the rule discoverable from the filename:
`FooIT.java` needs Docker, `FooTest.java` does not.

**The container is started manually in a static initializer, not by `@Testcontainers`.** This is a
correction from the first draft, which assumed a `static` field plus `@Testcontainers` starts the
container once per class hierarchy. It does not. The `@Testcontainers` extension registers each
container in the *per-test-class* JUnit store and closes it at that class's teardown, and
`GenericContainer.stop()` nulls the container id, so the next `*IT` class starts a genuinely new
container on a new ephemeral port. Spring's TestContext cache meanwhile reuses one
`ApplicationContext` across all subclasses with identical configuration, and its `DataSource` holds
the *first* container's port. The result would be that the first `*IT` passes and every later one
fails with connection refused — a failure that appears when Phase 3 adds the second integration
test, and looks like a Phase 3 regression while the defect is here. The documented singleton pattern
avoids it: start once in a static initializer and never stop, letting Docker reap the container when
the JVM exits.

**`application-test.yml` fails closed.** The first draft omitted datasource settings and claimed that
made tests safe. It does the opposite. `application.yml:4-7` supplies reachable defaults whose
credentials match `docker-compose.yml` byte for byte, so if `@ServiceConnection` silently fails to
contribute, Spring resolves those defaults and the test suite runs Flyway and Hibernate against the
developer's persistent Compose volume. Pointing the test profile at an unresolvable host means a
`@ServiceConnection` regression surfaces as an immediate connection error instead of as silent
corruption of real data.

## Files to create and modify

- Modify: `services/api/pom.xml`
- Create: `services/api/src/test/java/com/rikkaus/wealth/support/AbstractPostgresIntegrationTest.java`
- Create: `services/api/src/test/java/com/rikkaus/wealth/ApplicationContextIT.java`
- Create: `services/api/src/test/java/com/rikkaus/wealth/ArchitectureRulesTest.java`
- Create: `services/api/src/test/resources/application-test.yml`

## Implementation steps

1. **Repair Flyway autoconfiguration.** Add to `<dependencies>` at compile scope, version managed by
   the parent:

   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-flyway</artifactId>
   </dependency>
   ```

   Re-run the step-4 prerequisite check. `flyway_schema_history` and the `wealth` schema must now
   exist, and the startup log must show Flyway applying `V1`. Capture that output beside the
   before-state. This is a behavior change to the running application and Phase 6 records it.

2. **Add the test dependencies.** Note the corrected Testcontainers coordinates and the two Boot 4
   modules that `spring-boot-starter-test` does not bring.

   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-test</artifactId>
       <scope>test</scope>
   </dependency>
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-webmvc-test</artifactId>
       <scope>test</scope>
   </dependency>
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-resttestclient</artifactId>
       <scope>test</scope>
   </dependency>
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-testcontainers</artifactId>
       <scope>test</scope>
   </dependency>
   <dependency>
       <groupId>org.testcontainers</groupId>
       <artifactId>testcontainers-junit-jupiter</artifactId>
       <scope>test</scope>
   </dependency>
   <dependency>
       <groupId>org.testcontainers</groupId>
       <artifactId>testcontainers-postgresql</artifactId>
       <scope>test</scope>
   </dependency>
   <dependency>
       <groupId>com.tngtech.archunit</groupId>
       <artifactId>archunit-junit5</artifactId>
       <version>1.5.1</version>
       <scope>test</scope>
   </dependency>
   ```

   Then confirm with `./mvnw dependency:tree` that Testcontainers resolves to 2.0.5, junit-jupiter to
   6.0.3, Mockito to 5.23.0 and AssertJ to 3.27.7, none with an explicit version in this pom. Record
   the actual resolved versions. If any differs, stop and report it rather than adjusting the pom.

3. **Bind Failsafe so `*IT` classes run in `verify`.**

   ```xml
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-failsafe-plugin</artifactId>
       <executions>
           <execution>
               <goals>
                   <goal>integration-test</goal>
                   <goal>verify</goal>
               </goals>
           </execution>
       </executions>
   </plugin>
   ```

   Surefire's default includes are `Test*`, `*Test`, `*Tests` and `*TestCase`, which do not match
   `*IT`; Failsafe's defaults are `IT*`, `*IT` and `*ITCase`, which do. Verify that after step 6
   rather than trusting it.

   Note for #47: this makes `./mvnw verify` fail closed on any machine without a Docker daemon.
   Phase 6 documents it. The pipeline must provide Docker rather than reach for `-DskipITs`, which
   would silently disable Phase 5's contract drift gate.

4. **Write the integration base class.** Pin the image tag to the Compose tag from
   `docker-compose.yml:3` so tests and local development cannot drift onto different PostgreSQL
   versions. Start the container in a static initializer and do not annotate with `@Testcontainers`.

   ```java
   package com.rikkaus.wealth.support;

   import org.springframework.beans.factory.annotation.Autowired;
   import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
   import org.springframework.boot.test.context.SpringBootTest;
   import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
   import org.springframework.test.context.ActiveProfiles;
   import org.testcontainers.containers.PostgreSQLContainer;

   @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
   @AutoConfigureTestRestTemplate
   @ActiveProfiles("test")
   public abstract class AbstractPostgresIntegrationTest {

       @ServiceConnection
       static final PostgreSQLContainer<?> POSTGRES =
               new PostgreSQLContainer<>("postgres:18.6-alpine3.24");

       static {
           POSTGRES.start();
       }
   }
   ```

   `@AutoConfigureTestRestTemplate` is required in Boot 4; `@SpringBootTest` no longer contributes a
   `TestRestTemplate` bean on its own. Import it and `TestRestTemplate` from
   `org.springframework.boot.resttestclient`, not from the Boot 3 package.

   Resolve one uncertainty empirically before relying on it: `org.testcontainers.containers.PostgreSQLContainer`
   still exists in `testcontainers-postgresql-2.0.5.jar` but carries a deprecation marker, while a
   new non-generic `org.testcontainers.postgresql.PostgreSQLContainer` also ships. Compile against
   the legacy generic class first; if it fails or warns unacceptably, switch to the new package and
   drop the diamond (`static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(...)`).
   Record which one compiled.

5. **Write `application-test.yml` fail-closed.** No credentials, and a datasource URL that cannot
   accidentally succeed.

   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://service-connection-did-not-apply.invalid:5432/none
     flyway:
       enabled: true
     jpa:
       hibernate:
         ddl-auto: validate
   ```

   `@ServiceConnection` overrides all three datasource properties when it applies. If it does not,
   the suite fails immediately with an unresolvable host instead of connecting to the developer's
   real database.

6. **Write `ApplicationContextIT`.** Three assertions, each proving something the harness claims.

   ```java
   class ApplicationContextIT extends AbstractPostgresIntegrationTest {

       @Autowired DataSource dataSource;

       @Test
       void isConnectedToTheTestcontainersDatabaseAndNotALocalOne() throws Exception {
           try (var connection = dataSource.getConnection()) {
               assertThat(connection.getMetaData().getURL())
                       .as("@ServiceConnection did not apply; the suite is pointed at another database")
                       .contains(String.valueOf(POSTGRES.getMappedPort(5432)));
           }
       }

       @Test
       void flywayAppliedTheBaselineMigration() throws Exception {
           // assert flyway_schema_history has version '1' with success = true
       }

       @Test
       void baselineCreatesTheWealthSchemaAndNoDomainTables() throws Exception {
           // assert schema 'wealth' exists and holds no tables
       }
   }
   ```

   The first test is the guard the first draft lacked. Without it, a `@ServiceConnection` regression
   is silent. The second is the proof that step 1's Flyway repair works inside a container, not only
   at runtime.

7. **Write `ArchitectureRulesTest`.** Use
   `@AnalyzeClasses(packages = "com.rikkaus.wealth", importOptions = DoNotIncludeTests.class)`.

   - *Feature slices do not reach into each other.* Classes in `com.rikkaus.wealth.<feature>..` may
     depend only on their own feature, on `com.rikkaus.wealth.shared..`, and on third-party packages.
     Express with `slices().matching("com.rikkaus.wealth.(*)..").namingSlices("$1")
     .should().notDependOnEachOther()`, ignoring `shared`. Enforces
     `ARCHITECTURE_TECHNOLOGY_DECISIONS.md:135`.
   - *Controllers live in an `api` package.* Classes annotated `@RestController` must reside in a
     package matching `..api..`. Keeps Phase 2's convention enforceable.
   - *No binary floating point for money.* Scope this narrowly. A blanket ban on every `double` field
     in `com.rikkaus.wealth..` is broader than
     `ARCHITECTURE_TECHNOLOGY_DECISIONS.md:141`, which prohibits binary floating point for *monetary
     values and exchange rates* — the first legitimate non-monetary ratio or percentage field would
     trip a rule whose message talks about money, and the cheapest response would be to suppress it,
     taking the two rules that share the class down with it. Restrict the rule to fields whose name
     matches a monetary vocabulary (`amount`, `balance`, `price`, `value`, `rate`, `total`, `cost`)
     or whose declaring class sits in a package the money primitives own, and write the failure
     message so it explains the decimal requirement.

   Name each rule for the invariant it protects, not for this issue. This is a `*Test`: it reads
   bytecode and needs no database.

8. **Prove the rules are not vacuous.** For each of the three rules, temporarily add a violating
   class, observe the failure, then revert. A rule that has never been seen failing is not a rule,
   and the slice rule in particular passes trivially today because only `shared` exists. Record all
   three observations in the phase notes.

## Todo

- [x] Install JDK 25 and confirm `java -version`.
- [x] Confirm `docker version` exits 0.
- [x] Confirm `clean package` still passes before any edit.
- [x] Capture the before-state proving Flyway currently does not run.
- [x] Add `spring-boot-flyway` and re-capture, proving `V1` now applies.
- [x] Add the seven test dependencies with the corrected Testcontainers coordinates.
- [x] Run `dependency:tree` and record resolved versions of Testcontainers, JUnit, Mockito, AssertJ.
- [x] Add the Failsafe plugin execution.
- [x] Create `AbstractPostgresIntegrationTest` with the singleton container and `@AutoConfigureTestRestTemplate`.
- [x] Record which `PostgreSQLContainer` package compiled.
- [x] Create `application-test.yml` with the fail-closed datasource URL.
- [x] Create `ApplicationContextIT` with the container-identity, Flyway and no-domain-tables assertions.
- [x] Create `ArchitectureRulesTest` with the three rules, the money rule scoped by vocabulary.
- [x] Prove each of the three ArchUnit rules fails on a deliberate violation, then revert.
- [x] Confirm `./mvnw test` passes and does not run `ApplicationContextIT`.
- [x] Confirm `./mvnw verify` passes and does run it.
- [x] Confirm `docker ps` during `verify` shows exactly one container for the whole run.

## Verification

```bash
./services/api/mvnw -f services/api/pom.xml dependency:tree
./services/api/mvnw -f services/api/pom.xml test
./services/api/mvnw -f services/api/pom.xml verify
```

Pass conditions, each mechanically checkable:

- `dependency:tree` shows `org.testcontainers:testcontainers-postgresql:2.0.5` and
  `org.testcontainers:testcontainers-junit-jupiter:2.0.5`, neither with an explicit version in the
  pom, plus `org.springframework.boot:spring-boot-flyway`, `spring-boot-webmvc-test` and
  `spring-boot-resttestclient`.
- `test` completes with the ArchUnit test executed and `ApplicationContextIT` reported as not run.
- `verify` completes with all three `ApplicationContextIT` assertions passing.
- `docker ps` observed during `verify` shows exactly one `postgres:18.6-alpine3.24` container, and it
  is the same container id throughout the run.
- The captured before-and-after output shows `flyway_schema_history` absent before step 1 and present
  after.

## Success criteria

- [x] Flyway demonstrably runs, where before this phase it did not.
- [x] Both test tiers run from the documented commands.
- [x] An integration test is written by extending one base class with no container boilerplate.
- [x] A test cannot silently run against a database other than its container.
- [x] All three architecture rules have been observed failing on a deliberate violation.
- [x] No credential exists in any file added by this phase.

## Risk assessment

| Risk | Signal it broke | Response |
|---|---|---|
| The Flyway repair changes behavior on a database that already has hand-made state, so `V1` fails validation or `ddl-auto: validate` starts objecting. | Startup failure after step 1 on a developer machine with an old volume. | The baseline only creates a schema, so the realistic worst case is a developer whose volume predates it. Recreate the local volume with `docker compose down -v`, which is destructive and therefore an explicit developer decision, never a scripted step. |
| The legacy generic `PostgreSQLContainer` is deprecated in 2.0.5 and may be removed, or may not accept the diamond form. | Compilation failure or deprecation error in step 4. | Step 4 names the fallback: switch to `org.testcontainers.postgresql.PostgreSQLContainer` without the diamond. Record which compiled so later phases copy the right import. |
| The singleton container is never stopped, so a long `verify` leaves a container behind if the JVM is killed. | `docker ps` shows an orphan after an interrupted run. | Testcontainers' Ryuk reaper removes orphans on JVM exit, and an abandoned container is a local cleanup concern rather than a correctness one. Prefer an orphan over the per-class stop-and-restart defect this replaces. |
| Surefire and Failsafe include patterns do not behave as assumed. | `test` runs `ApplicationContextIT`, or `verify` skips it. | Add explicit `<includes>`/`<excludes>` to the respective plugin rather than renaming the test. |
| `verify` now requires Docker, and CI without a Docker socket fails every build. | Every pipeline run red once #47 wires it in. | Phase 6 documents the requirement. #47 must provide Docker; `-DskipITs` is forbidden because it also disables Phase 5's drift gate. |
| The money ArchUnit rule's name-based scoping misses a monetary field that does not match the vocabulary. | A `double` money field ships unflagged. | Accepted and stated: a name heuristic is weaker than a type. When the money primitives from #45 land, replace the heuristic with a rule that requires those types, which is the real enforcement point. |

## Security considerations

No credentials are introduced. `@ServiceConnection` generates per-run container credentials that
never touch a file, and `application-test.yml` carries an intentionally unresolvable host rather than
a working fallback, so a configuration regression cannot point a test at a real database. The
container binds to an ephemeral port chosen by Docker, so it cannot collide with the Compose service
on 5432 or be reached from off-host.

The Flyway repair is worth naming as a security-relevant fix rather than only a correctness one: a
service that silently skips migrations will also silently skip any future migration that adds a
constraint, an index backing an ownership check, or a `NOT NULL` that a validation rule depends on.

## Next steps

Phase 2 builds the `/api/v1` surface and its first slice test on this harness.

## Implementation notes — 2026-09-27

Executed on JDK 25.0.4.1 (Homebrew `openjdk@25`, not the Temurin build the prerequisite gate named;
any JDK 25 satisfies the pom's `java.version`) and Docker Engine 27.3.1.

### The Flyway defect was real

Captured before and after on the same fresh volume, changing only the one compile-scope dependency.
Before: zero Flyway lines in the startup log, no `flyway_schema_history` table, `\dn wealth` returned
zero rows — and the application still started cleanly in 1.149 seconds. After: `FlywayExecutor`
reports the database, creates the history table, and `SELECT version, success FROM
flyway_schema_history` returns `1 | baseline | SQL | t` with the `wealth` schema present. Issue #35's
unchecked acceptance box was accurate; the migration had never run.

### A sixth missing Boot 4 module, in the same category as the five red team found

`./mvnw verify` failed on its first run with `NoClassDefFoundError:
org/springframework/boot/restclient/RestTemplateBuilder`, raised while Spring evaluated
`@ConditionalOnMissingBean` on `TestRestTemplateTestAutoConfiguration.testRestTemplate`. Every
`@SpringBootTest` context carrying `@AutoConfigureTestRestTemplate` failed to refresh.

`spring-boot-resttestclient` declares `spring-boot-restclient` only as optional, so
`org.springframework.boot:spring-boot-restclient` must be declared explicitly at test scope.
Confirmed by `unzip -l spring-boot-restclient-4.1.1.jar`, which contains
`org/springframework/boot/restclient/RestTemplateBuilder.class`. This is the same failure mode the
red-team review catalogued — Boot 4 modularization moving a class out of the module that references
it — and it was not findable without a build, because the class is present at compile time through
the optional dependency and missing only at runtime. The plan's dependency count for this phase is
therefore eight test-scope artifacts, not seven.

### ArchUnit rules needed `allowEmptyShould(true)` on all three, not two

ArchUnit 1.5.1 defaults `archRule.failOnEmptyShould` to true, so the slice rule failed with "failed
to check any classes" as well: the pattern `com.rikkaus.wealth.(*)..` matches nothing while no
subpackage of `com.rikkaus.wealth` exists at all. Declared per rule rather than globally through
`archunit.properties`, so the allowance is visible at each rule and can be withdrawn individually
once slices exist.

### Resolved versions, none hand-pinned except ArchUnit

`spring-boot-flyway:4.1.1:compile`, `spring-boot-webmvc-test:4.1.1:test`,
`spring-boot-resttestclient:4.1.1:test`, `spring-boot-restclient:4.1.1:test`,
`spring-boot-testcontainers:4.1.1:test`, `testcontainers-junit-jupiter:2.0.5:test`,
`testcontainers-postgresql:2.0.5:test` (bringing `testcontainers-jdbc` and
`testcontainers-database-commons` at 2.0.5), `junit-jupiter:6.0.3`, `assertj-core:3.27.7`,
`mockito-core:5.23.0`, `archunit-junit5:1.5.1`. Every version matches the plan's prediction.

### Which `PostgreSQLContainer` compiled

The legacy generic `org.testcontainers.containers.PostgreSQLContainer<?>` with the diamond compiles
clean on 2.0.5 and emits no deprecation warning. Later phases copy that import. The jar also ships
`org.testcontainers.postgresql.PostgreSQLContainer`, which stays the fallback if the legacy class is
removed in a later 2.x release.

### Observed evidence for the pass conditions

`./mvnw test` ran three ArchUnit tests and reported no `ApplicationContextIT`, with no Docker
involvement. `./mvnw verify` ran both tiers and all three `ApplicationContextIT` assertions passed,
with `Successfully applied 1 migration to schema "public", now at version v1` logged against the
container. `docker ps --filter ancestor=postgres:18.6-alpine3.24` sampled twice during the run showed
the same single Testcontainers container id `36b23fe8e51c` on port 55408 throughout, alongside the
unrelated Compose service on 5432, confirming the singleton pattern holds.

All three architecture rules were observed failing on deliberate violations: two probe packages under
`com.rikkaus.wealth` where one returned and constructed the other's type, the probe controller
annotated `@RestController` outside any `api` package, and a `public double amount` field. Each rule
reported exactly one violation naming the offending element, and the probes were then deleted and
`clean test` returned to green.
