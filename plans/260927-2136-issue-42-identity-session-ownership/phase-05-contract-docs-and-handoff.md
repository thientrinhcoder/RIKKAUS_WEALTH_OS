---
phase: 5
title: "Contract, docs and sibling handoff"
status: completed
priority: P1
effort: "6h"
dependencies: [4]
---

# Phase 5: Contract, docs and sibling handoff

## Objective

Publish the contract change, reconcile the two durable documents the OIDC decision supersedes, and
notify the siblings — the issue's third acceptance criterion.

## Requirements

### OpenAPI

- Annotate the four auth routes so the published document carries request schemas, response schemas
  and real examples, following `MetaController`'s style: document only the statuses each route can
  actually return.
- `POST /api/v1/auth/google` gives the contract its **first request-accepting endpoint**, so it is
  also the first chance to publish a populated `errors` sample for a 400 `validation-failed`.
  `docs/api-contract-conventions.md` currently names that absence as a known gap; this phase closes
  it.
- Declare an OpenAPI `bearerAuth` security scheme and apply it to the protected routes, so a consumer
  can see which routes need a token.
- Update `OpenApiContractIT.publishesOnlyTheProductionApiSurface` to the new expected path set.
- Regenerate: `./services/api/mvnw -f services/api/pom.xml verify -Dopenapi.update=true`, read the
  diff, then re-run `verify` **without** the flag and confirm the drift gate passes.

### Documents

| File | Change |
|---|---|
| `ARCHITECTURE_TECHNOLOGY_DECISIONS.md:125-126` | Name Google OIDC as the sole identity provider. |
| `ARCHITECTURE_TECHNOLOGY_DECISIONS.md:233` | Replace the password-hashing line with: identity is Google OIDC only; refresh tokens are stored as a SHA-256 hash; the JWT signing key is an external secret. |
| `RIKKAUS_WEALTH_OS_MVP_BACKBONE.md:488` | Replace the password-hashing control with the OIDC and refresh-token-hashing control. |
| `docs/api-contract-conventions.md` | Rewrite "Ownership contract — shape agreed, enforcement not yet built" as implemented. Move authentication out of "What is not here". Record the deny-by-default change and the new 401 on an unauthenticated unknown path. Document the four routes, the bearer scheme, and that `detail` stays English while Vietnamese copy is mapped from the `type` URN by the client. Re-enable the credentials question: CORS `allowCredentials(false)` stays correct because the token is not a cookie. |
| `README.md` | Add the four new environment variables and how to obtain a Google OAuth client. |
| `.env.example` | Add `GOOGLE_OAUTH_CLIENT_ID`, `GOOGLE_OAUTH_CLIENT_SECRET`, `GOOGLE_OAUTH_REDIRECT_URIS`, `RIKKAUS_JWT_SECRET`, with a local-only note matching the existing file's tone. |

The two durable-document edits are the supersession the user approved. Each edited line gets a short
note naming the #40 decision, so a later reader sees why the password direction disappeared rather
than assuming it was dropped by accident.

### Handoff

- Comment on #42 recording: the password direction superseded by #40's PO decision, the two document
  lines changed, and the deny-by-default contract change.
- Comment on #41 with the contract change it must handle: the four routes, the bearer header, the
  401-on-unknown-path change, and the `type` URNs its error classifier now needs to map to the
  Vietnamese copy the #40 artifact already specifies.
- Write the acceptance report to
  `plans/reports/pm-260927-2136-issue-42-acceptance.md`, in the shape of
  `plans/reports/pm-260927-issue-39-acceptance.md`.

## Steps

1. Annotate the routes, add the security scheme, update the path-set assertion.
2. Regenerate the contract, review the diff line by line, re-verify without the flag.
3. Edit the five documents.
4. `mvnw verify` one final time.
5. Post the two issue comments and write the report.

## Validation

- `OpenApiContractIT` passes with no `-Dopenapi.update=true`.
- Every claim written into `docs/api-contract-conventions.md` is checked against a passing test or the
  generated document, not against intent. That document's opening promise is "everything here is
  implemented and verified", and it must stay true.
- `git diff` on the two durable documents touches only the identity lines.

## Risk

| Risk | Mitigation |
|---|---|
| The conventions document overstates what is built. | Each statement traced to a named test before it is written. |
| A contract change reaches `main` before #41 knows. | The issue comment is a step in this phase, not an afterthought, and the acceptance criterion requires it before merge. |
| The document edits quietly widen beyond the approved supersession. | Diff review limited to the identity lines; anything else is reverted. |

## Rollback

Revert the document edits and restore `openapi.json` from `main`. The code from Phases 1–4 is
unaffected.
