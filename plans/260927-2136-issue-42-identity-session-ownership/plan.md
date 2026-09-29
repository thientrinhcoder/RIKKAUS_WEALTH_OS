---
title: "Identity, session and ownership boundary (#42)"
description: "Implement Google OIDC sign-in, short-lived JWT access tokens, rotating hashed refresh tokens, logout invalidation, and reusable server-side ownership enforcement on top of the #39 API foundation."
status: completed
priority: P1
effort: 40h
issue: 42
branch: thientrinhcoder/feat/issue-42-identity-session-ownership
tags: [backend, identity, security, oidc, sessions, ownership, mvp-0]
blockedBy: []
blocks: []
created: 2026-09-27
---

# Identity, session and ownership boundary (#42)

## Outcome

A pilot user signs in with their Google Account, receives a short-lived access token and a rotating
refresh token, can sign out so the session cannot be resumed, and can never read or write another
user's record. Every later backend slice inherits one ownership helper and one authenticated-request
test helper instead of reinventing them.

## Resolved contract conflict

Issue #42's body asks for **password hashing**. The Design sibling
[#40](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/40) is closed and PO-accepted, and
narrowed identity to **Google Account OIDC only** — no password field, no app-managed signup, no
credential storage. Its own artifact, `docs/identity-session-ownership-review.html`, states the new
PO decision replaces the password direction, and
`plans/260913-issue-40-google-oidc-identity-design/phase-01-contract-state-mapping.md` deliberately
routed the reconciliation to this task rather than editing the durable docs itself.

The user confirmed **Google OIDC only** on 2026-09-27. Reconciling the two durable documents is
therefore in scope here, not an unauthorised reversal:

| Source | Current text | Action |
|---|---|---|
| `ARCHITECTURE_TECHNOLOGY_DECISIONS.md:233` | "Passwords are hashed using Spring Security's supported adaptive password encoder." | Replace with the OIDC + refresh-token-hashing posture. |
| `RIKKAUS_WEALTH_OS_MVP_BACKBONE.md:488` | "Hash passwords with an established password-hashing algorithm." | Replace with the same. |
| Issue #42 body | "password hashing" | Comment on the issue recording the supersession. |

Everything else the issue asks for — short-lived JWT, rotating hashed refresh tokens, logout
invalidation, reusable ownership enforcement tests — is unaffected by the decision and is delivered
in full.

## Inherited contracts this task must honour

From `docs/api-contract-conventions.md`, all of it already implemented and verified:

- `urn:rikkaus:problem:unauthorized` (401) and `urn:rikkaus:problem:forbidden` (403) are already
  declared in `ProblemType` and documented as reserved for this task. Do not add taxonomy entries.
- **A resource owned by another user returns `not-found`, not `forbidden`**, so the API never
  discloses that another user's record exists. This is the agreed shape; this task builds the
  enforcement.
- `*Test` = Surefire, no Docker. `*IT` = Failsafe, extends `AbstractPostgresIntegrationTest`.
- `OpenApiContractIT` fails the build on OpenAPI drift. Regenerate with
  `-Dopenapi.update=true` locally, never in CI.
- Problem `detail` stays English, matching the existing taxonomy. Vietnamese user-facing copy is
  mapped from the `type` URN by the Frontend sibling #41, per the #40 state matrix.
- A test-only controller must live outside `com.rikkaus.wealth` (see
  `com.rikkaus.testfixtures.ProblemFixtureController`) or it leaks into the published contract.

## Deliberate contract change: deny by default

Adding authentication makes the security chain deny every unmatched route. Two existing assertions
change as a direct consequence, and both changes are intentional:

| Request | Before | After | Why |
|---|---|---|---|
| Unauthenticated `GET /api/v1/no-such-resource` | 404 `not-found` | 401 `unauthorized` | An unauthenticated caller must not learn which paths exist. The original intent is preserved by a new assertion that an **authenticated** request to an unknown path still returns 404 `not-found`. |
| Unauthenticated `/test-fixtures/**` | 200/4xx per fixture | 401 `unauthorized` | Test scaffolding must not be allowlisted in production security config. The error-taxonomy ITs authenticate instead, through the reusable helper this task delivers. |

Both are contract changes and must be recorded in `docs/api-contract-conventions.md` and notified to
#41 before merge, as the issue's third acceptance criterion requires.

## Non-goals

- No password field, app-managed signup, password reset, magic link, OTP, passkey, or second
  identity provider.
- No household, shared access, role editor, or advisor permission. MVP 0 has one owner per record.
- No domain resource. Assets, liabilities, cash flow and goals stay later work; the ownership helper
  ships with a test-only owned resource proving it.
- No CI pipeline, coverage threshold, JSON log encoder, rate limiting, or idempotency key.
- No OpenAPI client codegen in `apps/mobile`; that is #41's scope per the inherited document.

## Phases

| Phase | Name | Status | Dependency |
|---|---|---|---|
| 1 | [Session primitives and persistence](./phase-01-session-primitives-and-persistence.md) | Completed | None |
| 2 | [Google OIDC boundary and first login](./phase-02-google-oidc-boundary.md) | Completed | Phase 1 |
| 3 | [Security chain and session endpoints](./phase-03-security-chain-and-session-endpoints.md) | Completed | Phase 2 |
| 4 | [Reusable ownership enforcement](./phase-04-reusable-ownership-enforcement.md) | Completed | Phase 3 |
| 5 | [Contract, docs and sibling handoff](./phase-05-contract-docs-and-handoff.md) | Completed | Phase 4 |

## Observable acceptance criteria

- [x] `POST /api/v1/auth/google` exchanges a Google authorization code with PKCE, creates an account
      on first login, and resumes the existing account on a later login, keyed on the Google subject.
- [x] An access token is a signed JWT that expires; an expired or tampered token returns 401
      `urn:rikkaus:problem:unauthorized`.
- [x] `POST /api/v1/auth/refresh` rotates the refresh token: the presented token stops working and a
      new one is returned. Replaying a rotated token revokes the whole session chain.
- [x] `POST /api/v1/auth/logout` invalidates the session immediately; a later refresh returns 401.
- [x] Refresh tokens exist in PostgreSQL only as a hash. No raw token, access token, or Google
      identity token is written to any log.
- [x] Reading or writing another user's record returns 404 `not-found`, byte-identical to a record
      that does not exist.
- [x] `services/api/openapi/openapi.json` is regenerated, carries the four auth routes with
      examples, and `OpenApiContractIT` passes without `-Dopenapi.update=true`.
- [x] `mvnw verify` is green: unit, slice and Testcontainers integration tests.
- [x] `ARCHITECTURE_TECHNOLOGY_DECISIONS.md`, `RIKKAUS_WEALTH_OS_MVP_BACKBONE.md` and
      `docs/api-contract-conventions.md` reflect the OIDC decision and the deny-by-default change;
      #41 and #42 are notified.
- [x] No Phase 2 behaviour and no fake data added to pass a check.

## Setup the user must perform

Implementation and the whole test suite run offline: the Google token exchange and identity-token
verification sit behind ports that tests stub. Manual end-to-end verification against real Google
needs a Google Cloud OAuth 2.0 client ID, its redirect URIs, and these environment variables, which
are documented in Phase 5 and `.env.example`:

- `GOOGLE_OAUTH_CLIENT_ID`
- `GOOGLE_OAUTH_CLIENT_SECRET` (omit for a public PKCE-only client)
- `GOOGLE_OAUTH_REDIRECT_URIS`
- `RIKKAUS_JWT_SECRET` (at least 32 bytes; the application fails to start without it)
