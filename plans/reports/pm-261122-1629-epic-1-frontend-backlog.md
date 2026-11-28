# Epic #1 (MVP 0 Foundation) — Frontend backlog status

Epic #1 still has **4 Frontend tasks in `Todo`**: #38, #41, #44, #46. Every Backend and Design sibling
of these tasks is closed. The Frontend app (`apps/mobile`) has only the shell, design system and a
health-check client; no typed API client, auth, money primitives or Frontend CI exist yet.

## Tickets

| Ticket | Size | Parent | BE sibling | API ready? | Contract gap |
|---|---|---|---|---|---|
| #38 API contract, errors & typed client | M / 3d | #8 (closed) | #39 closed | Yes — `services/api/openapi/openapi.json`, 21 paths, RFC 9457 problems, `X-Correlation-Id` | None. FE owns optional OpenAPI codegen. |
| #44 Money, currency, rate & time primitives | M / 3d | #10 | #45 closed | Yes — `NUMERIC` columns, `/settings/exchange-rate` | Money is published as bare JSON numbers (documented BE decision, `docs/api-contract-conventions.md:450`). Safe for realistic VND figures; FE must not do arithmetic on them. Accepted, no BE change. |
| #41 Identity, session & ownership | L / 5d | #9 | #42 closed | Yes — `/auth/google`, `/auth/refresh`, `/auth/logout`, `/auth/session` | **Gap 1:** refresh token returned in JSON body only, CORS `allowCredentials(false)`; ticket forbids web tokens in `localStorage`. **Gap 2:** no Google OAuth client configured locally and no local sign-in path, so no agent can obtain a real session for real-data testing. |
| #46 CI/CD quality gates & AWS preview | L / 5d | #11 | #47 closed | n/a | Only `.github/workflows/backend.yml` exists. `infra/aws/cdn.tf` has the API distribution only; no S3/CloudFront for the Expo web build. Applying infra needs the owner's AWS credentials. |

GitHub records no `blockedBy` links between these tasks. Dependencies below come from the issue bodies
and `RIKKAUS_WEALTH_OS_MVP_DELIVERY_BACKLOG.md:122`.

## Implementation order

1. **Wave A (parallel, separate worktrees):** #38, #44, #46 — no shared files expected
   (`src/api/**`, `src/money/**` + formatters, `.github/workflows/frontend.yml` + `infra/aws`).
2. **Backend change request** for #41 gaps, in parallel with Wave A.
3. **Wave B:** #41 after #38 merges (needs the typed client and auth interceptor hook) and after the
   Backend change lands.

## Unresolved questions

- Web refresh-token storage strategy (HttpOnly cookie vs memory-only).
- Local sign-in path for real-data testing (Google client credentials vs `local`-only dev sign-in).
- Whether #46 should provision AWS web preview now (owner runs `tofu apply`) or ship CI only.
