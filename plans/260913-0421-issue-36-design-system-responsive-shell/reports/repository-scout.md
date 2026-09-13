# Repository and frontend boundary scout

- `apps/mobile/src/ui/theme.ts:1-5` is the default MD3 light theme.
- `apps/mobile/src/ui/responsive.ts:3-18` has a 720px cap, one 768px breakpoint, and 16/24px padding.
- `apps/mobile/src/app/_layout.tsx:1-12` is a headerless stack with no shell navigation.
- Existing responsive tests cover only 375 and 1024px (`apps/mobile/__tests__/responsive.test.ts:3-10`).
- Issue #34 explicitly reserved tokens, navigation, and shell implementation for #37 (`plans/260913-issue-34-frontend-foundation/plan.md:39-44`).

#36 owns specification, review evidence, state matrix, responsive decisions, and PO approval. #37 owns production Paper theme, primitives, Expo Router shell, configuration, and automated tests. #40 owns identity/session/ownership flows.

Handoff items for #37: the light-only baseline differs from automatic appearance in `apps/mobile/app.json`, and the platform-back contract differs from its disabled Android predictive-back setting. #36 records expected behavior but does not edit configuration.
