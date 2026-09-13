---
title: "Phase 2: Providers, health contract, and route UI"
status: completed
---

# Phase 2: Providers, health contract, and route UI

## Objective

Wire the app-level providers and implement the single index route that exercises the first real frontend-to-backend contract: a typed health check against `GET /actuator/health`.

## Context Links

- TanStack Query, Paper, React Hook Form, Zod, and fetch are accepted frontend standards (`ARCHITECTURE_TECHNOLOGY_DECISIONS.md:70-76`).
- Business calculations must stay on the backend and remote data must live in TanStack Query (`ARCHITECTURE_TECHNOLOGY_DECISIONS.md:86-90`).
- Backend health and metrics are part of the backend standard, with administrative Actuator endpoints not public (`ARCHITECTURE_TECHNOLOGY_DECISIONS.md:111`, `ARCHITECTURE_TECHNOLOGY_DECISIONS.md:238`).

## Requirements

- Root layout wraps routes in `SafeAreaProvider`, `PaperProvider`, and `QueryClientProvider`.
- The runtime `QueryClient` may use normal retry defaults, but tests must create their own retry-off client in Phase 3.
- Health client reads `EXPO_PUBLIC_API_BASE_URL`; a missing or blank value becomes the explicit empty/configuration state, while an invalid configured URL becomes an error. Neither case may produce a fake success.
- Health schema requires `status: "UP"` and uses Zod `.passthrough()` or equivalent to tolerate additional Actuator fields.
- The index route renders a centered card with max width 720, 16px mobile padding, 24px padding at `>=768`, accessible heading, and live status text.
- Retry control appears only in the error state.
- No branding, tabs, drawer, auth flow, dashboard cards, charts, or business placeholders.

## File Inventory

| File or path | Action | Notes |
|---|---|---|
| `apps/mobile/src/app/_layout.tsx` | Modify | App providers and router slot/stack. |
| `apps/mobile/src/app/index.tsx` | Modify | Health route UI and responsive layout. |
| `apps/mobile/src/providers/app-providers.tsx` | Create | Provider composition and runtime QueryClient factory outside Expo Router's route directory. |
| `apps/mobile/src/features/health/health-schema.ts` | Create | Zod schema and exported TypeScript type. |
| `apps/mobile/src/features/health/health-client.ts` | Create | Fetch wrapper and base URL handling. |
| `apps/mobile/src/features/health/use-health-query.ts` | Create | TanStack Query hook. |
| `apps/mobile/src/ui/theme.ts` | Create | Minimal Paper theme only; no #37 design-system tokens. |
| `apps/mobile/src/ui/responsive.ts` | Create if useful | Shared layout constants for max width and padding. |

Do not modify backend `services/api`, Flyway migrations, root Docker files, CI, or domain feature folders.

## Dependency Graph

- Depends on Phase 1 package installation and route scaffold.
- Blocks Phase 3 tests because tests need stable providers, health schema, and UI states.
- Does not depend on #37 design approval or #38 OpenAPI typed client because #34 owns only a minimal hand-written health contract.

## Data Flow

1. `EXPO_PUBLIC_API_BASE_URL` enters through Expo public environment handling.
2. `health-client.ts` normalizes the base URL and requests `/actuator/health`.
3. The raw JSON response is parsed through `health-schema.ts`.
4. `use-health-query.ts` stores the parsed result in TanStack Query cache.
5. `src/app/index.tsx` maps query state to empty, loading, error, or success UI.
6. The user sees only operational status and can retry after errors.

## Implementation Steps

- [x] Create provider composition with Paper, SafeArea, and Query providers in one reusable module.
- [x] Define `HealthResponseSchema = z.object({ status: z.literal("UP") }).passthrough()`.
- [x] Implement base URL handling so empty `EXPO_PUBLIC_API_BASE_URL` disables the query and produces the explicit empty/configuration state.
- [x] Implement fetch error handling for non-2xx responses and invalid JSON without swallowing the original status/message.
- [x] Add `useHealthQuery` with a stable query key such as `["health"]`.
- [x] Replace scaffold route content with the accessible health card and four observable states:
  - Empty/configuration state when `EXPO_PUBLIC_API_BASE_URL` is missing or blank, without calling fetch.
  - Loading/progress state with live region.
  - Error state with retry button.
  - Success state showing backend status.
- [x] Use React Hook Form only if a tiny local config/debug form is genuinely needed; otherwise installing it is enough for #34 and avoids unnecessary UI.

## Verification Commands

```bash
npm --prefix apps/mobile run typecheck
npm --prefix apps/mobile run lint
npm --prefix apps/mobile run web
```

Manual pass conditions: with no `EXPO_PUBLIC_API_BASE_URL`, the route renders the empty/configuration state without making a request; with a valid backend base URL and `GET /actuator/health` returning `{ "status": "UP" }`, it renders success; additional health fields do not break rendering.

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Missing backend blocks UI validation. | Medium | Medium | Validate the error state without backend and success state against a real backend when available; do not fake runtime success. |
| Unknown Actuator fields break parsing. | Medium | High | Use strict requirement only for `status: "UP"` and tolerate extras. |
| Provider state leaks across tests. | Medium | Medium | Phase 3 test utilities must create fresh QueryClient instances. |
| UI drifts into #37 design scope. | Medium | Medium | Keep neutral Paper defaults, minimal theme, and no brand/design-system layer. |

## Rollback

Revert the files in this phase inventory. If Phase 3 has already added tests against these modules, revert those tests with the same feature rollback.

## Success Criteria

- [x] App providers wrap the index route without runtime crashes.
- [x] Health schema accepts `{ status: "UP", details: {} }` and rejects non-UP or missing status.
- [x] UI exposes empty/loading/error/success states and retry only in error.
- [x] Layout meets max width and padding requirements at mobile and tablet/desktop widths.

## Completion Evidence

- `RootLayout` composes Expo Router with SafeArea, TanStack Query, and Paper providers; the live Expo server rendered without a runtime crash.
- Source inspection and passing tests confirm environment handling, HTTP/JSON/schema failures, the passthrough `UP` contract, stable query keys, four observable states, and error-only retry behavior.
- Live inspection confirmed centered content, a 720px maximum width, 16px mobile padding at 375px, and 24px wide padding at 1024px.
