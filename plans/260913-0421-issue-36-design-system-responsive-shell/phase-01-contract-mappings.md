---
phase: 1
title: "Freeze contract and implementation mappings"
status: pending
priority: P1
effort: "1d"
dependencies: []
---

# Phase 1: Freeze contract and implementation mappings

## Objective

Convert the approved design baseline into precise decisions that frontend issue #37 can implement without inventing token mappings, breakpoints, elevation, or state anatomy.

## Context Links

- `docs/design-guidelines.md:112-240` owns tokens, typography, spacing, navigation, and responsive behavior.
- `docs/design-guidelines.md:337-433` owns component states, accessibility, review, and governance.
- `ARCHITECTURE_TECHNOLOGY_DECISIONS.md:61-91` selects Expo, React Native Web, React Native Paper, and one shared design-system layer.
- `apps/mobile/src/ui/theme.ts:1-5` is an unchanged MD3 light theme; `apps/mobile/src/ui/responsive.ts:3-18` has only a 768px padding breakpoint.

## Requirements

- Treat `docs/design-guidelines.md` as the only evergreen source of design truth; do not fork token values into another normative document.
- Define proposed MD3 mappings for the approved semantic roles, retaining project extensions where no faithful MD3 role exists.
- Map the approved type roles to implementable Paper typography roles, preserving IBM Plex Sans, restricted Noto Serif Display, Vietnamese diacritics, and tabular figures.
- Present shell thresholds as a PO decision with alternatives. The recommended proposal is bottom navigation below 768px, navigation rail from 768px through 1023px, and stable sidebar at 1024px and above; it is not implementation-ready until approved.
- Specify flat, raised-card, and modal elevation through observable border and surface behavior.
- Define state anatomy for empty, loading, partial, stale, validation error, request error, offline, success, disabled, destructive confirmation, and unsaved changes.
- Allocate identity and ownership behavior to issue #40; #36 owns only global component anatomy and non-enumerating generic copy such as “Không thể mở mục này” plus a safe recovery path. It must not confirm record existence, another user, or missing permission.
- Add a state ownership table: #36 owns global anatomy/specimens, #40 owns identity/session/ownership, later validation/error hardening owns cross-domain error semantics, and later safe-edit/delete work owns domain destructive flows.
- Include numeric contrast evidence and preserve portability across React Native, React Native Web, iOS, iPadOS, and Android.
- Introduce no production component API, route, package, runtime state, API fixture, or business calculation.

## Architecture and Data Flow

`docs/design-guidelines.md` remains the authority. The issue #36 review board cites it and visualizes approved decisions. Frontend issue #37 maps the approved semantic contract into `appTheme`, responsive helpers, shell routes, and tests. Design does not write those consumers.

## Related Code Files

| Action | Absolute path | Purpose |
|---|---|---|
| Read / modify only for approved durable deltas | `/Volumes/MacMini_Data 3/Projects/PetProjects/RIKKAUS_WEALTH_OS/docs/design-guidelines.md` | Preserve the normative contract and record only approved missing decisions. |
| Read only | `/Volumes/MacMini_Data 3/Projects/PetProjects/RIKKAUS_WEALTH_OS/apps/mobile/src/ui/theme.ts` | Identify the #37 theme injection point. |
| Read only | `/Volumes/MacMini_Data 3/Projects/PetProjects/RIKKAUS_WEALTH_OS/apps/mobile/src/ui/responsive.ts` | Identify current breakpoint/padding behavior. |
| Read only | `/Volumes/MacMini_Data 3/Projects/PetProjects/RIKKAUS_WEALTH_OS/apps/mobile/app.json` | Record light-mode and predictive-back discrepancies for #37. |

## Implementation Steps

1. Build a requirement-to-guideline traceability table for every issue #36 acceptance criterion.
2. Audit the semantic palette against intended text, icon, border, focus, and status pairings; retain calculated ratios as review evidence.
3. Draft the MD3 and typography handoff mappings, labeling direct mappings and project extensions.
4. Draft the responsive decision table for 360/375 phone, 768 tablet, 1024 transition, 1440 bounded desktop, landscape, safe-area, keyboard, and back behavior.
5. Draft the component-by-state matrix and anatomy; distinguish global shell behavior from later domain behavior.
6. Present proposed breakpoint and mapping decisions as approval items in the Phase 2 review board.
7. Update `docs/design-guidelines.md` only after approval and only for a genuine durable gap.

## Todo

- [x] Trace all issue and parent-feature criteria to current design guidance.
- [x] Calculate and record required contrast pairs.
- [x] Define MD3, typography, elevation, breakpoint, and state-anatomy mappings.
- [x] Record the issue #40 ownership boundary and #37 consumer boundary.

## Verification

- Every mapped token and rule resolves to a current guideline section or is labeled as a proposed decision.
- No duplicated token authority is introduced.
- Combined unstaged, staged, and untracked inventories contain no `apps/mobile/**`, backend, migration, API, or runtime configuration changes: inspect `git diff --name-only`, `git diff --cached --name-only`, and `git ls-files --others --exclude-standard`.
- Every proposed contrast pairing includes a numeric ratio and threshold result.

## Risk Assessment

| Risk | Signal | Response |
|---|---|---|
| Baseline status is mistaken for issue-level PO sign-off. | Issue #36 remains open with no approval evidence. | Keep baseline status and issue acceptance separate; require Phase 3 PO decision. |
| A focused review board is treated as the full section-16 MVP mockup. | Review asks for all screens and eight domain flows. | Label it “design contract review board” and keep later domain flows out of scope. |
| Breakpoints conflict with #34 runtime helpers. | The approved threshold differs from 768 or 1024. | Record the decision; #37 owns code changes and migration. |
| Generic unavailable copy leaks ownership or record existence. | Specimen copy names another account, permission, or hidden record. | Reject the copy and use a non-enumerating message with a safe return path. |

## Security and Privacy

Use no real personal or financial data. Generic specimens may use the fictional guideline fixture, clearly labeled as design-review content. Do not imply authentication or authorization behavior.

## Rollback

Revert only unapproved changes to `docs/design-guidelines.md`. Draft mappings remain in the issue-scoped review artifact until resolved.

## Success Criteria

- [x] Frontend can map every required semantic role without guessing.
- [x] Responsive transformations and state anatomy are explicit and reviewable.
- [x] All proposed contract deltas are distinguished from already-approved guidance.
