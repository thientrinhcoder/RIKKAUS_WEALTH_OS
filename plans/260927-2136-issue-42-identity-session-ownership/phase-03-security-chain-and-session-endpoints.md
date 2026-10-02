---
phase: 3
title: "Security chain and session endpoints"
status: completed
priority: P1
effort: "8h"
dependencies: [2]
---

# Phase 3: Security chain and session endpoints

## Objective

Make the access token actually authorise requests, make 401 and 403 conform to RFC 9457, and expose
refresh, logout and session-probe endpoints so the #40 session states are reachable end to end.

## Requirements

### Filter chain

Stateless, deny by default:

- `SessionCreationPolicy.STATELESS` — no servlet session, matching
  `ARCHITECTURE_TECHNOLOGY_DECISIONS.md:146` ("stateless except for PostgreSQL-backed data and
  refresh-token state").
- CSRF disabled, and the reason recorded: there is no cookie-borne credential to forge, because the
  access token travels in an `Authorization` header the browser never attaches automatically. If a
  cookie-based session is ever introduced, this must be reconsidered.
- Permitted without authentication: `GET /api/v1/meta`, `POST /api/v1/auth/google`,
  `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout`, `/actuator/health`, and — only because
  `springdoc.api-docs.enabled` already gates them to the `local` profile — `/v3/api-docs**` and
  `/swagger-ui/**`.
- `anyRequest().authenticated()`.

`/auth/refresh` and `/auth/logout` are unauthenticated on purpose: their credential is the refresh
token in the body, and requiring a live access token would make it impossible to renew an expired
session — which is the entire point of the "expired session" state in the #40 matrix.

### Bearer authentication

A `OncePerRequestFilter` reading `Authorization: Bearer`, decoding with the `JwtDecoder` built from
the same secret as the encoder, and setting an authentication whose principal is the user id. An
absent header leaves the context empty and lets the entry point handle it; a present-but-invalid token
is rejected immediately, because silently treating a bad token as anonymous hides clock skew and key
rotation bugs.

### 401 and 403 in the taxonomy

`AuthenticationEntryPoint` → `ProblemType.UNAUTHORIZED`, `AccessDeniedHandler` →
`ProblemType.FORBIDDEN`, both rendered through the existing `ProblemDetailWriter`. That class exists
precisely because a failure raised inside a filter never reaches `ApiExceptionHandler`, and Spring
Security's handlers run in the filter chain. Reusing it is what keeps the `correlationId` member and
the `application/problem+json` content type identical to every other error.

This is the point at which the two reserved taxonomy entries become reachable, closing the gap
`docs/api-contract-conventions.md` documents.

### Endpoints

| Route | Auth | Body | Success |
|---|---|---|---|
| `POST /api/v1/auth/refresh` | none | `{ "refreshToken": "..." }` | 200, same shape as sign-in |
| `POST /api/v1/auth/logout` | none | `{ "refreshToken": "..." }` | 204 |
| `GET /api/v1/auth/session` | Bearer | — | 200 `{ id, email, displayName }` |

`logout` returns 204 for an unknown or already-revoked token as well. Distinguishing them would turn
the endpoint into an oracle for whether a token is live, and signing out twice is not a user error.

`GET /api/v1/auth/session` is what lets the client distinguish the #40 "expired session" and "revoked
session" states from a working one without guessing from a domain call.

## Files

| Action | Path |
|---|---|
| Create | `.../shared/security/SecurityConfiguration.java` |
| Create | `.../shared/security/BearerTokenAuthenticationFilter.java` |
| Create | `.../shared/security/ProblemAuthenticationEntryPoint.java` |
| Create | `.../shared/security/ProblemAccessDeniedHandler.java` |
| Create | `.../shared/security/CurrentUser.java` |
| Modify | `.../identity/api/AuthenticationController.java` |
| Create | `.../identity/api/RefreshSessionRequest.java` |
| Modify | `.../identity/application/IdentityService.java` |
| Create | `.../shared/security/SecurityConfigurationIT.java` |
| Create | `.../identity/api/AuthenticationFlowIT.java` |
| Modify | `.../shared/error/ProblemDetailsContractIT.java` |
| Modify | `.../shared/observability/CorrelationIdContractIT.java` |
| Create | `.../support/AuthenticatedClient.java` |

## Steps

1. **3.T** — before adding the chain, add to `ProblemDetailsContractIT` an authenticated-request
   assertion for the unknown-path 404 so the original intent is captured as a test *before* the
   unauthenticated assertion changes to 401.
2. Build `AuthenticatedClient` test support: sign in a fresh test user through the real service with
   stubbed Google ports, and hand back a `TestRestTemplate` carrying the bearer header. This is the
   reusable helper the issue asks for and every later slice's ITs will use.
3. Implement the chain, filter, entry point and denied handler.
4. Update the two existing ITs: unauthenticated unknown path now 401; the fixture-controller calls now
   authenticate.
5. Implement refresh, logout and session endpoints with `AuthenticationFlowIT` covering: sign in,
   call a protected route, refresh and confirm the old token is dead, replay the old token and confirm
   the chain is revoked, log out and confirm refresh fails, expired access token rejected.
6. **3.V** — `mvnw verify` fully green.

## Validation

- `SecurityConfigurationIT`: every permitted route reachable anonymously; a protected route returns
  401 `urn:rikkaus:problem:unauthorized` as `application/problem+json` with a `correlationId`.
- `AuthenticationFlowIT`: the five session transitions above.
- Manual check that no log line carries a bearer token.

## Risk

| Risk | Mitigation |
|---|---|
| The permit list drifts and a domain route is accidentally public. | `anyRequest().authenticated()` is the default; `SecurityConfigurationIT` asserts the permit list explicitly so adding a route to it requires editing a test. |
| 401 bodies fall back to Boot's default shape. | Rendered through `ProblemDetailWriter`, asserted in the IT against the same helper the other taxonomy ITs use. |
| Changing the unknown-path status silently breaks #41. | Recorded as a contract change in Phase 5 and notified before merge. |

## Rollback

Remove `shared/security` and revert the two IT edits. Phases 1 and 2 remain usable without a chain.
