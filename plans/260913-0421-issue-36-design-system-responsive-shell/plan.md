---
title: "Issue #36 design system and responsive shell"
description: "Turn the approved Quiet Heritage baseline into a reviewable design contract, responsive shell board, state matrix, and PO-approved handoff for frontend issue #37."
status: in-progress
priority: P1
effort: "3d"
issue: 36
branch: main
tags: [feature, design, frontend, accessibility]
blockedBy: []
blocks: []
created: 2026-09-13
---

# Issue #36 design system and responsive shell

## Outcome

Package the already-approved design baseline into one annotated, self-contained review board that makes semantic tokens, component states, responsive shell behavior, accessibility, and the frontend handoff explicit. Obtain Product Owner approval and notify sibling task #37 without implementing production Expo UI.

## Scope Challenge

- **Existing code and contract:** `docs/design-guidelines.md` already owns Quiet Heritage, semantic tokens, navigation, responsive behavior, states, accessibility, and review rules. The Expo app has only a neutral Paper theme and a one-breakpoint foundation.
- **Requested scope:** deliver the full Design-owned contract for issue #36 and parent feature #7. Do not absorb frontend issue #37 or later domain design tasks.
- **Complexity:** three sequential phases, two durable design artifacts, one stateful review record, and no new production services or classes.
- **Selected scope:** HOLD. Auto-detected Hard mode because approval, accessibility, responsive behavior, and cross-team handoff are load-bearing.

## Deliverables

- [`docs/design-guidelines.md`](../../docs/design-guidelines.md): remains the evergreen authority; update only approved missing decisions and add the final review-artifact reference.
- `docs/design-system-responsive-shell-review.html`: non-normative, design-only, offline review evidence. It is not the full-MVP clickable mockup governed by section 16.
- `plans/reports/design-260913-issue-36-po-review.md`: stateful PO decision and sibling-handoff evidence.

## Phases

| Phase | Name | Status | Dependency | Outcome |
|---|---|---|---|---|
| 1 | [Freeze contract and mappings](./phase-01-contract-mappings.md) | Pending | None | Close semantic, responsive, elevation, and state-anatomy gaps. |
| 2 | [Build annotated review surface](./phase-02-annotated-review-surface.md) | Pending | Phase 1 | Produce the reviewable offline shell and state board. |
| 3 | [Verify, approve, and hand off](./phase-03-verification-approval-handoff.md) | Pending | Phases 1-2 | Record evidence, PO decision, and the #37 handoff. |

## Acceptance Criteria

- [ ] Semantic token names, MD3 handoff mappings, and intended foreground/background pairs are explicit and meet WCAG 2.2 AA.
- [ ] Phone, tablet, and desktop shell behavior is shown at 360, 375, 768, 1024, and 1440px with unchanged destination names, order, and hierarchy.
- [ ] The review board covers the required component families and applicable interaction, loading, data-quality, error, recovery, and success states.
- [ ] Keyboard, focus, touch target, text scaling, reduced motion, safe-area, overflow, and back-path expectations are annotated and verified.
- [ ] Production files under `apps/mobile/**`, backend code, API contracts, calculations, authentication, and Phase 2 product capabilities remain unchanged; labeled design simulations and fictional specimens are permitted.
- [ ] PO approval or requested changes are recorded; approved decisions and contract changes are linked from issue #36 and announced to #37 before merge/approval.

## Sources

- [Issue #36](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/36)
- [Parent feature #7](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/7)
- [Sibling frontend task #37](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/37)
- [`docs/design-guidelines.md`](../../docs/design-guidelines.md)
- [`ARCHITECTURE_TECHNOLOGY_DECISIONS.md`](../../ARCHITECTURE_TECHNOLOGY_DECISIONS.md)
- [`RIKKAUS_WEALTH_OS_MVP_BACKBONE.md`](../../RIKKAUS_WEALTH_OS_MVP_BACKBONE.md)

## Open Questions

None for planning. Phase 1 presents `768px` for bottom navigation to rail and `1024px` for rail to sidebar as proposed decisions, not facts; PO approval is the execution gate. Ownership-case behavior stays with identity design issue #40, while #36 defines only non-enumerating generic shell/state hooks. Issue #36 Design Approved does not satisfy parent #7's later API, calculation, ownership, implementation, or end-to-end acceptance gates.

## Research Reports

- [Design contract research](./reports/design-contract-research.md)
- [Repository and frontend boundary scout](./reports/repository-scout.md)

## Validation Log

### Verification Results

- **Tier:** Standard (3 phases; Fact Checker + Contract Verifier)
- **Claims checked:** 30
- **Verified:** 30 | **Failed:** 0 | **Unverified:** 0
- Verified file ownership, current MD3 theme state, responsive helper state, app configuration discrepancies, issue/backlog relationships, approved review widths, component/state requirements, and accessibility thresholds against repository evidence and live GitHub issue bodies.
- No implementation-changing user question remains. Breakpoint values are explicitly proposed pending PO approval, not presented as current contract facts.

## Red Team Review

### Session — 2026-09-13

**Findings:** 25 raw findings, deduplicated to 14 material findings (14 accepted, 0 rejected).  
**Severity breakdown after deduplication:** 5 High, 9 Medium.

| # | Finding | Severity | Disposition | Applied To |
|---|---|---|---|---|
| 1 | Shell-level flow/spec and explicit section-16 exclusion inventory were missing | High | Accept | Phases 2-3 |
| 2 | Design plan over-claimed parent #7 acceptance | High | Accept | Plan, Phase 3 |
| 3 | PO approval record and sequence were underspecified | High | Accept | Phase 3 |
| 4 | External GitHub publication lacked auth, redaction, and approval gates | High | Accept | Phase 3 |
| 5 | Review board was incorrectly called authoritative | High | Accept | Plan, Phase 2 |
| 6 | Breakpoints were stated as facts before approval | Medium | Accept | Plan, Phase 1 |
| 7 | State ownership could absorb later task scope | Medium | Accept | Phase 1 |
| 8 | Generic unavailable copy could reveal ownership/record existence | Medium | Accept | Phase 1 |
| 9 | Phase 2/fake-data wording conflicted with labeled design simulation | Medium | Accept | Plan, Phase 2 |
| 10 | Offline typography could not prove bundled production fonts | Medium | Accept | Phase 2 |
| 11 | Light mode and predictive back were treated as optional assessments | Medium | Accept | Phase 3 |
| 12 | Scope diff missed staged and untracked files | Medium | Accept | Phases 1, 3 |
| 13 | Responsive/accessibility evidence format was not durable | Medium | Accept | Phase 3 |
| 14 | Print/export could leak review notes | Medium | Accept | Phase 2 |

### Whole-Plan Consistency Sweep

- Files reread: `plan.md`, all three phase files, and both reports.
- Decision deltas checked: 14.
- Reconciled stale references: 11.
- Unresolved contradictions: 0.

<!-- slug: issue-36-design-system-responsive-shell -->
