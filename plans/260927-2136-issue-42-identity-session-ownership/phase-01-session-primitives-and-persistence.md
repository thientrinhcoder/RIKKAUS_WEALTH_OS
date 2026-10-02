---
phase: 1
title: "Session primitives and persistence"
status: completed
priority: P1
effort: "10h"
dependencies: []
---

# Phase 1: Session primitives and persistence

## Objective

Land the `identity` feature slice's persistence and the two token services it owns, with no HTTP
surface yet. After this phase a signed access token can be issued and verified, and a refresh token
can be issued, rotated, replayed-and-caught, and revoked — all provable by unit tests and one
repository integration test.

## Context

- `services/api/pom.xml` has no Spring Security dependency yet and `wealth` contains no table but the
  schema itself (`V1__baseline.sql`).
- `ARCHITECTURE_TECHNOLOGY_DECISIONS.md:125-126` fixes the mechanism: signed short-lived JWT access
  tokens, rotating refresh tokens stored hashed in PostgreSQL.
- `ARCHITECTURE_TECHNOLOGY_DECISIONS.md:135` makes Flyway the only schema mechanism and
  `spring.jpa.hibernate.ddl-auto: validate` enforces that the entities match the migration.
- `ArchitectureRulesTest.featureSlicesDoNotDependOnEachOther` starts matching real slices the moment
  `com.rikkaus.wealth.identity` exists, so the slice may depend on `shared` and nothing else.

## Requirements

### Dependencies

Add to `services/api/pom.xml`, matching the existing pinning and commenting style:

- `spring-security-oauth2-jose` — supplies `NimbusJwtEncoder`/`NimbusJwtDecoder` for the access
  tokens issued here and the JWKS-backed decoder Phase 2 needs for Google identity tokens. The
  version stays governed by the Boot BOM.

**Deviation found during step 3.T, and the reason for it.** The phase originally called for
`spring-boot-starter-security` here. Adding it made 12 of the 17 existing integration tests fail
at once: the starter installs Boot's default filter chain, which answers every request with the
generated HTML login page, so each JSON assertion failed with `Unexpected character ('<')`. Those
tests are this task's only regression detector, and leaving them red across Phases 1 and 2 would
discard it.

`spring-security-oauth2-jose` pulls in `spring-security-core` but neither `spring-security-config`
nor `spring-security-web`, so no web-security autoconfiguration activates and no chain is
installed. Phase 1 gets the JWT primitives it needs with the suite still green, and
`spring-boot-starter-security`, the chain, and the deliberate updates to those integration tests all
land together in Phase 3, where they can be reviewed as one change. `spring-security-test` moves to
Phase 3 for the same reason.

### Migration `V2__identity.sql`

```sql
CREATE TABLE wealth.users (
    id              UUID        PRIMARY KEY,
    google_subject  TEXT        NOT NULL UNIQUE,
    email           TEXT        NOT NULL,
    display_name    TEXT,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL
);

CREATE TABLE wealth.refresh_tokens (
    id             UUID        PRIMARY KEY,
    user_id        UUID        NOT NULL REFERENCES wealth.users (id) ON DELETE CASCADE,
    session_id     UUID        NOT NULL,
    token_hash     TEXT        NOT NULL UNIQUE,
    issued_at      TIMESTAMPTZ NOT NULL,
    expires_at     TIMESTAMPTZ NOT NULL,
    revoked_at     TIMESTAMPTZ,
    revoked_reason TEXT
);

CREATE INDEX idx_refresh_tokens_session ON wealth.refresh_tokens (session_id);
CREATE INDEX idx_refresh_tokens_user ON wealth.refresh_tokens (user_id);
```

Decisions to record as SQL comments:

- `google_subject` is the identity key, not `email`. Google's `sub` is stable; a user can change
  their Google email. `email` is stored and refreshed on each login for display only, and is
  deliberately **not** unique so a legitimate email change cannot fail a login with an error state
  the accepted design has no copy for.
- `session_id` groups a rotation chain. It is what makes replay detection able to revoke every
  descendant of a stolen token, not just the one presented.
- `revoked_at` plus `revoked_reason` rather than a delete, so replay of a rotated token is
  distinguishable from a token that never existed.

### `AccessTokenService`

- HS256 via `NimbusJwtEncoder` over a `SecretKeySpec` built from `rikkaus.auth.jwt.secret`.
- **Fails closed at startup**: no default value, and a secret shorter than 32 bytes aborts context
  refresh with a message naming the property. A weak signing key is a silent total compromise, so it
  must not be possible to boot without a real one. HS256 rather than RS256 because one monolith both
  issues and verifies; the decision record already treats the signing key as an external secret.
- Claims: `sub` = user id, `sid` = session id, `iat`, `exp`, `iss` = `rikkaus-wealth-api`. No email,
  no display name, no Google claim — an access token travels in headers and logs, so it carries the
  minimum needed to authorise.
- TTL from `rikkaus.auth.jwt.access-token-ttl`, default `PT15M`.
- Reads the injected `Clock` bean, never `Instant.now()`, so expiry is testable without sleeping.

### `RefreshTokenService`

- Raw token: 32 bytes from `SecureRandom`, base64url without padding.
- Stored as **SHA-256 hex of the raw token**. Deliberately not BCrypt/Argon2: those are designed to
  slow brute force against low-entropy human passwords. A 256-bit random token is not guessable, and
  an adaptive hash on every refresh would only add latency. Record this reasoning in the class
  javadoc so a later security review does not "fix" it.
- `issue(userId, sessionId)` returns the raw token once and never again.
- `rotate(rawToken)`:
  1. Unknown hash, or expired → `unauthorized`.
  2. Hash already revoked → **replay**: revoke every unrevoked token sharing that `session_id` with
     reason `replayed`, then `unauthorized`.
  3. Otherwise revoke the presented token with reason `rotated` and issue a successor in the same
     `session_id`.
- `revokeSession(rawToken)` for logout, reason `logged-out`; revoking an already-revoked token is a
  no-op that still reports success, so signing out twice is not an error.
- TTL from `rikkaus.auth.refresh-token-ttl`, default `P30D`.

### Logging boundary

`ARCHITECTURE_TECHNOLOGY_DECISIONS.md:144` forbids logging passwords, tokens and financial payloads.
No raw token, hash, or JWT may appear in any log statement in this slice. Log the user id and session
id only. A unit test asserts the negative for the replay path, which is the one place a developer is
most tempted to log the offending value.

## Files

| Action | Path |
|---|---|
| Modify | `services/api/pom.xml` |
| Create | `services/api/src/main/resources/db/migration/V2__identity.sql` |
| Create | `services/api/src/main/java/com/rikkaus/wealth/identity/domain/UserAccount.java` |
| Create | `services/api/src/main/java/com/rikkaus/wealth/identity/domain/RefreshToken.java` |
| Create | `services/api/src/main/java/com/rikkaus/wealth/identity/domain/UserAccountRepository.java` |
| Create | `services/api/src/main/java/com/rikkaus/wealth/identity/domain/RefreshTokenRepository.java` |
| Create | `services/api/src/main/java/com/rikkaus/wealth/identity/session/AccessTokenService.java` |
| Create | `services/api/src/main/java/com/rikkaus/wealth/identity/session/RefreshTokenService.java` |
| Create | `services/api/src/main/java/com/rikkaus/wealth/identity/session/IssuedSession.java` |
| Create | `services/api/src/main/java/com/rikkaus/wealth/identity/session/SessionProperties.java` |
| Modify | `services/api/src/main/resources/application.yml` |
| Modify | `services/api/src/main/resources/application-local.yml` |
| Modify | `services/api/src/test/resources/application-test.yml` |
| Create | `services/api/src/test/java/com/rikkaus/wealth/identity/session/AccessTokenServiceTest.java` |
| Create | `services/api/src/test/java/com/rikkaus/wealth/identity/session/RefreshTokenServiceTest.java` |
| Create | `services/api/src/test/java/com/rikkaus/wealth/identity/domain/IdentityPersistenceIT.java` |

## Steps

1. **3.T — characterize.** Run `mvnw test` and record the 29 passing tests as the baseline. Adding
   `spring-boot-starter-security` puts a default filter chain on the classpath, which changes
   `@WebMvcTest` and `@SpringBootTest` behaviour; the existing suite is the detector for that.
2. Add the dependencies. Re-run `mvnw test` immediately and deal with whatever the default security
   autoconfiguration breaks before writing any new code.
3. Write `V2__identity.sql`.
4. Write the failing unit tests for `AccessTokenService` and `RefreshTokenService` against a fixed
   `Clock`, then implement until green.
5. Write `IdentityPersistenceIT` proving the entities match the migration under `ddl-auto: validate`,
   the unique constraint on `google_subject` holds, and the cascade from `users` removes tokens.
6. **3.V — verify.** `mvnw verify`. Every test from step 1 must still pass.

## Validation

- `mvnw test` — new unit tests green, all 29 pre-existing tests still green.
- `mvnw verify` — `IdentityPersistenceIT` green, no Flyway validation error.
- `grep` the slice for `log.` and confirm no statement interpolates a token, hash or JWT.

## Risk

| Risk | Mitigation |
|---|---|
| `spring-boot-starter-security` auto-secures everything and breaks the 4 existing ITs before the chain is designed in Phase 3. | Expected, and step 2 surfaces it immediately rather than at the end. Phase 3 owns the real chain; Phase 1 only needs the suite honest. |
| A weak or absent JWT secret reaches a deployed environment. | No default value, minimum length enforced at startup, verified by a context-failure test. |
| Hibernate and Flyway drift. | `ddl-auto: validate` plus `IdentityPersistenceIT`. |

## Rollback

`git checkout -- services/api` restores the phase. `V2__identity.sql` has not been applied to any
shared environment, so dropping the file is safe; `clean-disabled: true` means no accidental wipe is
possible either way.
