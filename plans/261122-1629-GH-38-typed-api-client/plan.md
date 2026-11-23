# #38 — API contract, errors & typed client foundation

**Status:** implemented and verified. See
[`manual-verification-261122-1629-GH-38-typed-api-client.md`](../reports/manual-verification-261122-1629-GH-38-typed-api-client.md).

## Outcome

`apps/mobile` reaches the backend only through `src/api`. That module provides a typed fetch client
over the published contract, Zod validation of responses, an RFC 9457 classifier keyed on the `type`
URN with Vietnamese copy, `X-Correlation-Id` handling, a pluggable auth seam, a TanStack Query retry
policy, and mock fixtures built from the contract's own examples. `src/features/health` is migrated
to it.

## Non-goals

Token storage, sign-in UI, session renewal and any backend change are out of scope; they belong to
#41. No Phase 2 behaviour is included.

## Decisions

| Decision | Choice | Why |
|---|---|---|
| OpenAPI codegen | **Yes, types only** (`openapi-typescript` 7.13, `npm run api:generate`, enforced by `npm run api:check`) | Keeps paths, parameters, bodies and response shapes in sync with the contract at compile time. A generated runtime client (e.g. `openapi-fetch`) was not added: the client must own the parts the contract cannot describe — the problem classification, correlation, the 401 retry seam and the operational route for `/actuator/health` — and a thin fetch is smaller than wrapping one |
| TypeScript 6 vs the codegen peer range (`typescript@^5`) | An `overrides` entry pins `openapi-typescript`'s TypeScript to the app's | Codegen output verified identical in shape. `npm ci` is clean. Remove the override when `openapi-typescript` declares TS 6 |
| Runtime validation | Hand-written Zod per endpoint, constrained to the generated type | The contract marks most fields optional. The client can require what the server always sends, and a drifting schema still fails typecheck (TC-29) |
| Unknown problem `type` | Parsed as a string. Copy falls back by status | A newer server's URN must not break an older client |
| `not-found` / `forbidden` copy | Both use `Không thể mở mục này` | design-guidelines §11.1 requires non-enumerating copy |
| `unauthorized` copy | The accepted identity design's expired-session text | Source: `docs/identity-session-ownership-review.html` |
| Automatic retries | At most one, for network, 5xx and `internal-error` only | Resending a 4xx or a schema mismatch cannot change the answer |

## Acceptance

- Every test case in [`test-cases.md`](test-cases.md) is executed, with evidence.
- `npm run lint`, `typecheck`, `api:check` and `build:web` all pass.
- No fake data is wired into the running app. Fixtures are reached only through an explicit
  `fetchImpl`.
