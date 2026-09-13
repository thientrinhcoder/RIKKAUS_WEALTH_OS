---
title: "Phase 3: Tests, responsive verification, and docs"
status: completed
---

# Phase 3: Tests, responsive verification, and docs

## Objective

Prove the foundation works through focused tests, type/lint/build checks, manual responsive verification, and README onboarding. This phase makes #34 executable by the next frontend tasks.

## Context Links

- CI expectations include frontend `npm ci`, formatting/linting, type-checking, unit tests, and web build (`ARCHITECTURE_TECHNOLOGY_DECISIONS.md:207-218`).
- Wave rules require frontend tasks to use typed clients and verify responsive/error states before closure (`RIKKAUS_WEALTH_OS_MVP_DELIVERY_BACKLOG.md:124-132`).
- Guardrails require sessions to run appropriate test/lint/typecheck evidence before claiming Done (`RIKKAUS_WEALTH_OS_MVP_DELIVERY_BACKLOG.md:288-295`).

## Requirements

- Configure Jest with `jest-expo` and React Native Testing Library.
- Keep tests outside `apps/mobile/src/app` because route files under `src/app/` are Expo Router routes.
- Do not add `react-test-renderer`; React Native Testing Library replaces it for this stack.
- Test Query with a fresh `QueryClient` per test and retries disabled.
- Mock `fetch` only inside tests. Runtime code must use real fetch and never fake a healthy backend.
- Document exact commands and `EXPO_PUBLIC_API_BASE_URL` usage in root README.

## File Inventory

| File or path | Action | Notes |
|---|---|---|
| `apps/mobile/package.json` | Modify | Jest config/script if not already complete. |
| `apps/mobile/jest.config.*` or package Jest field | Create/modify | Use `jest-expo` preset. |
| `apps/mobile/jest.setup.ts` | Create if needed | Test globals/fetch helpers only. |
| `apps/mobile/__tests__/health-schema.test.ts` | Create | Zod contract tests. |
| `apps/mobile/__tests__/health-client.test.ts` | Create | URL, request, HTTP failure, and validation boundary tests. |
| `apps/mobile/__tests__/health-screen.test.tsx` | Create | Route state tests with RNTL and retry-off QueryClient. |
| `apps/mobile/__tests__/responsive.test.ts` | Create | Verify the real 375px and 1024px breakpoint mapping. |
| `apps/mobile/test/test-utils.tsx` | Create | Provider helper and fresh QueryClient factory outside Jest suite discovery. |
| `README.md` | Modify | Frontend setup, env, commands, health check, troubleshooting. |

Do not create tests under `apps/mobile/src/app`, Playwright E2E tests, CI pipeline files, backend fixtures, or fake runtime data files.

## Dependency Graph

- Depends on Phase 1 scripts/config and Phase 2 health/provider modules.
- Blocks #37, #38, and #46 handoff readiness because those tasks depend on a verified frontend foundation.
- No later phase exists in this plan.

## Test Matrix

| Test | File | Pass condition |
|---|---|---|
| Health schema accepts minimal success | `__tests__/health-schema.test.ts` | `{ status: "UP" }` parses. |
| Health schema tolerates extras | `__tests__/health-schema.test.ts` | Unknown fields remain allowed. |
| Health schema rejects invalid status | `__tests__/health-schema.test.ts` | Missing or non-UP status throws. |
| Missing API configuration | `__tests__/health-screen.test.tsx` | Empty/configuration copy renders and `fetch` is not called. |
| Loading state | `__tests__/health-screen.test.tsx` | Live loading/progress text renders while fetch is pending. |
| Error state and retry | `__tests__/health-screen.test.tsx` | Error text and retry button render; pressing retry calls refetch/fetch again. |
| Success state | `__tests__/health-screen.test.tsx` | `UP` status renders from parsed response. |
| Provider isolation | `__tests__/test-utils.tsx` | Each test gets a fresh retry-off QueryClient. |

## Implementation Steps

- [x] Add Jest config using `jest-expo`.
- [x] Add React Native Testing Library tests outside `src/app/`.
- [x] Add tests for missing configuration plus fetch mocks for pending, failing, and successful health responses.
- [x] Add README sections:
  - Prerequisites: Node `>=22.13`, npm, backend optional for success-state verification.
  - Install: `npm --prefix apps/mobile ci`.
  - Environment: `EXPO_PUBLIC_API_BASE_URL=http://localhost:8080`.
  - Commands: web, build, typecheck, lint, test, Expo dependency check.
  - Health contract: `GET /actuator/health`, minimal `{ "status": "UP" }`, extra fields tolerated.
  - Troubleshooting: missing env, backend down, dependency mismatch.
- [x] Manually verify responsive layout at 375px and 1024px widths and record evidence in the implementation summary, not as a permanent docs artifact unless the repo later standardizes visual evidence storage.
- [x] Run all verification commands.

## Verification Commands

```bash
npm --prefix apps/mobile ci
npm --prefix apps/mobile run typecheck
npm --prefix apps/mobile run lint
npm --prefix apps/mobile test
npm --prefix apps/mobile run expo:check
npm --prefix apps/mobile run build:web
npm --prefix apps/mobile run web
```

Pass conditions: all non-interactive commands pass; the web app starts; manual responsive checks at 375px and 1024px show centered max-width 720 content, 16px mobile padding, 24px `>=768` padding, visible heading/live regions, and no text overlap.

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Jest transforms fail on Expo/RN packages. | Medium | Medium | Use `jest-expo` preset and only add transform ignore overrides if a real failure requires it. |
| Tests accidentally rely on app-route filesystem behavior. | Medium | Medium | Keep tests outside `app/` and use Expo Router testing utilities only if route integration needs them. |
| README over-documents future domain behavior. | Low | Medium | Document only setup, env, commands, and health foundation for #34. |
| Manual responsive checks are skipped. | Medium | Medium | Treat 375px and 1024px evidence as an acceptance gate before closing. |

## Rollback

Revert only this phase's test/config/README changes. If rolling back the whole feature, remove `apps/mobile` after preserving any unrelated user-created files under that path.

## Success Criteria

- [x] Typecheck, lint, test, Expo dependency check, and web build pass.
- [x] README lets a new developer install, configure, run, test, and troubleshoot the frontend.
- [x] Responsive and accessible health states are verified at 375px and 1024px.
- [x] No backend, database, Flyway, auth, design system, CI, or Phase 2 scope was added.

## Completion Evidence

- The independent final test run passed all four suites and all 17 tests with fresh retry-disabled query clients.
- Locked installation, typecheck, lint, and Expo dependency checks passed; Expo reported only an offline local-map warning.
- Static web export passed for `/`, `/_sitemap`, and `/+not-found`; the live web app rendered successfully.
- Live measurements confirmed 16px padding and 343px usable content at 375px, and 24px padding with a 720px content cap at 1024px.
- Root README now covers prerequisites, locked install, environment setup, run and verification commands, health behavior, and troubleshooting while preserving backend #35 onboarding.
