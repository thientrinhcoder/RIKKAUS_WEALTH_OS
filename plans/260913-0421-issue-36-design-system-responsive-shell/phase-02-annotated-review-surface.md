---
phase: 2
title: "Build the annotated design review surface"
status: pending
priority: P1
effort: "1d"
dependencies: [1]
---

# Phase 2: Build the annotated design review surface

## Objective

Create one self-contained, keyboard-accessible HTML review board that lets the Product Owner inspect the token contract, responsive shell transformations, navigation, component states, and recovery behavior without running the Expo application.

## Requirements

- Identify the artifact as a simulated design-contract review board, not production frontend and not the full-MVP clickable mockup from section 16.
- Provide visible sections for contract traceability, semantic tokens, typography/elevation, responsive shell, component-state matrix, and accessibility/approval.
- Add a shell-level UX specification with entry points, primary/secondary navigation, avatar/settings path, **Thêm** sheet boundary, modal boundaries, screen hierarchy assumptions, system/back paths, and safe return behavior.
- Add a master section-16 coverage table. Mark later domain screens and eight end-to-end flows “intentionally out of #36 scope,” and mark only shell/component specimens as covered. Do not imply full-MVP mockup approval.
- Show phone (360/375), tablet (768), and desktop (1024/1440) compositions.
- Preserve the five destinations and order: Tổng quan, Tài sản & Nợ, Dòng tiền, Mục tiêu, Nhận định.
- Demonstrate avatar/settings, the lower-thumb **Thêm** action and sheet boundary, selected/focus states, safe-area reservation, and safe return paths.
- Show bottom navigation transforming to rail, then sidebar, without changing labels, order, or hierarchy.
- Cover buttons, inputs, cards, lists, tabs, bottom navigation, dialogs, bottom sheets, status indicators, and feedback across all applicable states.
- Include deterministic state switching and recovery without domain workflows or fake production API behavior.
- Label deterministic interaction and fictional financial specimens as design-review simulations. They may not represent real integrations, persistence, authentication, calculations, or Phase 2 product capabilities.
- Use semantic HTML, visible focus, keyboard operation, accessible dialog/focus return, 44px touch targets, 16px inputs, text/icon status reinforcement, and reduced motion.
- Inline CSS and JavaScript; use no CDN, remote font, iframe, analytics, or network-required asset.
- Treat typography as a stack and fallback specimen only unless approved local font files are embedded. Production font loading, platform rendering, and final diacritic verification remain #37 responsibilities.

## Design Direction

Quiet Heritage review editorial: warm canvas, white and subtle-paper specimens, deep ink, restrained teal interaction, rare gold rules, Noto Serif Display only for display moments, IBM Plex Sans for functional content, hairline separation, and no gradients or decorative wealth clichés. The memorable element is a single responsive shell spine that transforms across navigation modes.

## Related Code Files

| Action | Absolute path | Purpose |
|---|---|---|
| Create | `/Volumes/MacMini_Data 3/Projects/PetProjects/RIKKAUS_WEALTH_OS/docs/design-system-responsive-shell-review.html` | Non-normative issue #36 review evidence. |
| Read | `/Volumes/MacMini_Data 3/Projects/PetProjects/RIKKAUS_WEALTH_OS/docs/design-guidelines.md` | Normative source for visual and interaction rules. |
| Do not modify | `/Volumes/MacMini_Data 3/Projects/PetProjects/RIKKAUS_WEALTH_OS/apps/mobile/src/ui/theme.ts` | #37-owned production mapping. |
| Do not modify | `/Volumes/MacMini_Data 3/Projects/PetProjects/RIKKAUS_WEALTH_OS/apps/mobile/src/app` | #37-owned route and shell. |

## Implementation Steps

1. Define CSS tokens first from approved semantic roles; every color, spacing, type size, radius, and motion value traces to a token.
2. Compose an asymmetric editorial shell with a fixed reading path and visible issue/feature metadata.
3. Render token and typography specimens with contrast results, Vietnamese diacritics, tabular numbers, wrapping, and large-text examples.
4. Build the responsive shell spine with selectable phone/tablet/desktop views and annotations.
5. Render the component-state matrix and deterministic samples, including error recovery and focus behavior.
6. Add the approval checklist. If print is offered, it exports static artifact content only; PO notes and decisions remain in the stateful report.
7. Test with JavaScript disabled so factual content remains visible; interactions enhance but do not gate it.

## Todo

- [x] Build token, typography, elevation, shell, and state sections.
- [x] Add deterministic responsive and state controls.
- [x] Add citations, annotations, accessibility semantics, and approval checklist.
- [x] Verify direct-file use with no required network assets.

## Verification

- Open directly via `file://` and confirm zero required network requests.
- Keyboard-only review reaches every control, exposes focus, closes dialogs with Escape, and returns focus.
- At 360, 375, 768, 1024, and 1440px there is no horizontal scroll, clipped label, obscured action, or broken hierarchy.
- Under reduced motion, no comprehension depends on animation.
- Visible copy contains no generic placeholders, promotional claims, emoji icons, or em dashes.
- Every displayed token, breakpoint, or state row cites `docs/design-guidelines.md` or is visibly marked “proposed pending approval,” and the guideline last-updated date is shown for drift detection.

## Risk Assessment

| Risk | Signal | Response |
|---|---|---|
| The review board becomes an alternate source of truth. | Values differ from `docs/design-guidelines.md`. | Block approval until citations and authority are reconciled. |
| Interactivity is mistaken for production readiness. | Review asks for API/auth/persistence behavior. | Keep the simulation label persistent and route requests to owning tasks. |
| Dense state coverage harms mobile review. | The matrix scrolls horizontally at 360px. | Stack component groups and use disclosure while keeping content in the DOM. |

## Security and Privacy

No telemetry, storage, user input persistence, remote assets, or production credentials. Any financial specimen is fictional and visibly labeled.

## Rollback

Delete the issue-scoped HTML review board and remove its reference. No runtime behavior or build is affected.

## Success Criteria

- [x] The Product Owner can review every issue #36 criterion from one offline file.
- [x] #37 can translate it into Paper/theme/shell code without inventing intent.
- [x] The board remains a design artifact and does not absorb later feature flows.
