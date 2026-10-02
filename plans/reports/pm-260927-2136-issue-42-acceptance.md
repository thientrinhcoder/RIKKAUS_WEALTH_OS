# Issue #42 acceptance — Identity, session and ownership boundary

- **Issue:** [#42](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/42)
- **Plan:** [260927-2136-issue-42-identity-session-ownership](../260927-2136-issue-42-identity-session-ownership/plan.md)
- **Branch:** `thientrinhcoder/feat/issue-42-identity-session-ownership`
- **Date:** 2026-09-27
- **Verification:** `./services/api/mvnw -f services/api/pom.xml clean verify` — **131 tests, 0 failures**
  (81 Surefire, 50 Failsafe with Testcontainers PostgreSQL)

## Outcome

A pilot user signs in with their Google Account, receives a 15-minute JWT access token and a rotating
single-use refresh token, can sign out so the session cannot be renewed, and cannot read or write another
user's record. Every later backend slice inherits one ownership helper, one authenticated-request test
helper, and one inheritable ownership test suite.

## The contract conflict this task resolved

Issue #42's body asked for **password hashing**. The Design sibling
[#40](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/40) is closed and PO-accepted, and had
narrowed identity to **Google Account OIDC only** — no password field, no app-managed signup, no credential
storage. Its delivered artifact `docs/identity-session-ownership-review.html` states the new PO decision
replaces the password direction, and
`plans/260913-issue-40-google-oidc-identity-design/phase-01-contract-state-mapping.md` deliberately routed
the reconciliation to this task rather than editing the durable documents itself.

The user confirmed **Google OIDC only** before implementation began. Two durable documents were reconciled
as part of that decision, each edit annotated with the superseding decision:

| File | Change |
|---|---|
| `ARCHITECTURE_TECHNOLOGY_DECISIONS.md` | Security baseline: password hashing replaced by Google-OIDC-only identity plus SHA-256 refresh-token hashing. Backend stack table now names Google OIDC as the sole provider. |
| `RIKKAUS_WEALTH_OS_MVP_BACKBONE.md` | Safety baseline: password hashing replaced by the same, plus rotate-on-every-use. |

Everything else the issue asked for was unaffected by the decision and is delivered in full.

## Acceptance criteria

| Criterion | Status | Evidence |
|---|---|---|
| Google sign-in, account on first login, resume afterwards | Met | `AuthenticationFlowIT.aFirstSignInCreatesTheAccountAndReturnsAUsableSession`, `aReturningSignInResumesTheSameAccount`, `twoDifferentGoogleAccountsGetSeparateUsers` |
| Short-lived signed JWT; expired or tampered token rejected | Met | `AccessTokenServiceTest` (7 tests: expiry against an injected clock, foreign secret, tampered signature, garbage) |
| Refresh rotation; presented token retired | Met | `AuthenticationFlowIT.renewalReturnsANewPairAndRetiresThePresentedRefreshToken`, `RefreshTokenServiceTest.rotationReturnsANewTokenAndKillsThePresentedOne` |
| Replay revokes the whole chain | Met | `AuthenticationFlowIT.replayingARetiredRefreshTokenRevokesTheWholeSession` |
| Logout invalidation | Met (one limitation, below) | `AuthenticationFlowIT.signingOutMakesTheSessionUnrenewable`, `signingOutTwiceIsStillNoContent` |
| Refresh tokens stored only as a hash | Met | `RefreshTokenServiceTest.anIssuedTokenIsStoredOnlyAsAHash` asserts both directions |
| No password, token or financial payload logged | Met | `RefreshTokenLoggingTest` captures the appender and asserts the session id is present while the raw token and its hash are absent; all seven log statements in the slice were audited |
| Cross-user read/write returns 404, identical to a missing record | Met | `OwnershipGuardTest.theTwoRejectionsAreIndistinguishable`, `OwnershipContract.theDenialIsIndistinguishableFromAMissingRecord` (byte-identical body comparison) |
| Reusable ownership enforcement tests | Met | `OwnershipContract` (5 inheritable assertions), `AuthenticatedClient`, `StubGoogleIdentityProvider`; `OwnershipEnforcementIT` is the worked example and supplies only two hooks |
| Never trust a client-supplied `userId` | Met | ArchUnit rule `controllersNeverAcceptAUserIdentifierFromTheClient`; verified to fire by temporarily introducing both an implicit `@PathVariable UUID userId` and an explicit `@RequestParam("owner_id")` |
| OpenAPI regenerated with examples; drift gate passes | Met | `services/api/openapi/openapi.json` +357 lines, 5 paths, `bearerAuth` scheme; `OpenApiContractIT` passes **without** `-Dopenapi.update=true` |
| Documents reconciled; siblings notified | Met | See above and *Handoff* below |
| No Phase 2 behaviour, no fake data to pass a check | Met | No stubs in production code. The only test double is `StubGoogleIdentityProvider`, which replaces the two Google ports so the suite runs offline; the database, tokens, security chain and HTTP layer are all real |

## Deliberate contract change — deny by default

Adding authentication made the security chain deny every unmatched route. Two behaviours changed:

| Request | Was | Is now |
|---|---|---|
| Unauthenticated request to an unknown path under `/api/v1` | 404 `not-found` | **401 `unauthorized`** |
| Any request with a malformed or expired bearer token, even to a public route | not possible | **401 `unauthorized`** |

The first prevents an unauthenticated caller from mapping which paths exist; an *authenticated* request to an
unknown path still returns 404, asserted by
`SecurityConfigurationIT.anUnknownPathIsStillNotFoundForAnAuthenticatedCaller`. The second means a bad token
is rejected rather than silently treated as anonymous.

Both are recorded in `docs/api-contract-conventions.md` and notified to #41.

## Bugs found during implementation

Two are worth recording because they would not have been caught by review.

**Replay detection was silently rolled back.** `RefreshTokenService.rotate` revoked the compromised chain and
then threw a checked exception; `IdentityService.renew` caught it and threw an `ApiException`, whose rollback
undid the revocation in the same transaction. The unit tests passed — an in-memory repository has no
transaction semantics to model — so in production a replayed token would have revoked nothing. Found by
`AuthenticationFlowIT`, fixed by performing the revocation in a `PROPAGATION_REQUIRES_NEW` transaction so a
security-relevant fact survives the rejection of the request that revealed it.

**Access-token expiry was being judged by the wrong clock.** `NimbusJwtDecoder` installs a default timestamp
validator that reads the system clock, ignoring the injected `Clock`, so an expiry assertion would pass or
fail depending on when the suite ran. The decoder is now reduced to signature verification and the claim
checks moved into `verify`, making the injected clock the single source of "now".

A third, smaller one: the original tampered-token test flipped the final base64url character of the
signature, which encodes mostly padding bits and can decode to identical bytes — a genuinely flaky test,
now tampering a character that carries full bits.

## Known limitations

1. **Sign-out does not revoke an already-issued access token.** It revokes the ability to renew. The access
   token is self-contained and verified without a database lookup, so it remains valid for the remainder of
   its 15 minutes. This is asserted by
   `AuthenticationFlowIT.anAccessTokenIssuedBeforeSignOutRemainsValidUntilItExpires` so it cannot change
   unnoticed, and is documented in `docs/api-contract-conventions.md`. Tightening it would require a
   per-request session lookup, which was not in scope.
2. **No static rule can force a slice to call `OwnershipGuard`.** The ArchUnit rule catches a
   client-supplied `userId`, and `OwnershipContract` makes the test cheap to inherit, but a slice that
   simply omits the check will not fail the build. This is a residual gap, not a solved problem.
3. **Manual end-to-end verification against real Google was not performed.** It requires a Google Cloud
   OAuth client, which is the user's to create. The two Google ports are stubbed in tests, so the adapters
   `HttpGoogleAuthorizationCodeExchange` and `NimbusGoogleIdentityTokenVerifier` are covered by unit-level
   reasoning and the contract they implement, not by a live exchange. Setup steps are in `README.md`.
4. **Structured JSON logging, CI, rate limiting and coverage thresholds remain absent**, unchanged from the
   #39 foundation and owned by their own tasks.

## Setup the user must perform

Before a deployed environment or a live Google sign-in:

- `RIKKAUS_JWT_SECRET` — at least 32 bytes. **The application refuses to start without it.**
- `GOOGLE_OAUTH_CLIENT_ID`, optionally `GOOGLE_OAUTH_CLIENT_SECRET`
- `GOOGLE_OAUTH_REDIRECT_URIS` — exact-match allowlist, also registered on the Google OAuth client

All four are documented in `.env.example` and `README.md`. The `local` profile supplies a development JWT
secret so nothing is needed to build, test or run locally.

## Handoff

- **#42** — comment recording the superseded password direction, the two document edits, and the
  deny-by-default change.
- **#41 (Frontend)** — comment with the four routes, the bearer header, the 401-on-unknown-path change, and
  the `type` URNs its error classifier must map to the Vietnamese copy the #40 artifact already specifies.

## Unresolved questions

1. Should sign-out revoke issued access tokens immediately? That needs a per-request session lookup, trading
   a database round trip on every authenticated request for closing a 15-minute window. Recorded as
   limitation 1 rather than decided unilaterally.
2. The `local` profile's committed JWT secret follows the same convention as the committed PostgreSQL
   password in `docker-compose.yml`. Confirm that convention is acceptable for a signing key, or move it to
   an uncommitted `.env`.
