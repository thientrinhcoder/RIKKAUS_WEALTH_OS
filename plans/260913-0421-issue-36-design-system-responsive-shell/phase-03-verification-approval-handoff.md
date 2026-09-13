---
phase: 3
title: "Verify, obtain approval, and hand off"
status: pending
priority: P1
effort: "1d"
dependencies: [1, 2]
---

# Phase 3: Verify, obtain approval, and hand off

## Objective

Prove the review board against the design contract, record the Product Owner decision, reconcile approved changes, and hand frontend issue #37 an explicit implementation contract.

## Requirements

- Verify every issue #36 criterion with a traceable result. Split parent #7 criteria into #36-owned Design gates, #37 implementation gates, #40 identity/ownership gates, and feature-level API/calculation/end-to-end gates; do not claim parent completion.
- Record PO approval, requested changes, or rejection by contract area and responsive shell view.
- Apply only approved durable decisions to `docs/design-guidelines.md`; keep review history in the stateful report.
- Notify #37 of approved mappings, breakpoints, state anatomy, known implementation discrepancies, and unresolved decisions.
- Classify current `apps/mobile/app.json` automatic appearance and disabled predictive-back configuration as blocking #37 conformance items. #37 must force the approved light baseline and enable compliant platform back behavior, or obtain a separately approved exception; #36 does not change production config.
- Do not mark Design Approved until all blocking review comments are resolved or explicitly accepted by the Product Owner.
- Define the approval record with PO identity/role, date, artifact path and revision/hash, approved areas, requested changes, rejected areas, and explicit issue status/label/comment evidence.
- Before any GitHub write, verify authentication and issue access, prepare the exact outbound text, remove local absolute paths, machine/environment details, private PO notes, screenshots with local paths, and any personal/financial data, then obtain explicit user/PO approval to publish.

## Related Code Files

| Action | Absolute path | Purpose |
|---|---|---|
| Verify / modify approved deltas only | `/Volumes/MacMini_Data 3/Projects/PetProjects/RIKKAUS_WEALTH_OS/docs/design-guidelines.md` | Evergreen approved contract. |
| Verify | `/Volumes/MacMini_Data 3/Projects/PetProjects/RIKKAUS_WEALTH_OS/docs/design-system-responsive-shell-review.html` | Review evidence. |
| Create | `/Volumes/MacMini_Data 3/Projects/PetProjects/RIKKAUS_WEALTH_OS/plans/reports/design-260913-issue-36-po-review.md` | Stateful PO decision and handoff record. |
| Read only | `/Volumes/MacMini_Data 3/Projects/PetProjects/RIKKAUS_WEALTH_OS/apps/mobile/app.json` | #37 configuration discrepancy evidence. |

## Verification Matrix

| Gate | Evidence | Pass condition |
|---|---|---|
| Contract traceability | Issue-to-guideline/review-board table | Every criterion has a source and visible evidence. |
| Contrast | Numeric foreground/background matrix | Normal text ≥4.5:1; large text and meaningful UI ≥3:1. |
| Responsive | Evidence-table row and capture/reference for 360, 375, 768, 1024, and 1440 | No overflow; correct navigation mode; stable labels and order. |
| Accessibility | Timestamped evidence rows for keyboard path, focus return, text scale, reduced motion, and offline mode | All paths work without pointer or motion; focus is never lost. |
| States | Component-by-state matrix plus deterministic samples | All applicable required states have anatomy and recovery. |
| Scope | Unstaged, staged, and untracked file inventories | No production frontend, backend, API, auth, calculation, or Phase 2 implementation. |
| Offline | Direct file open and request inspection | No required remote asset or runtime service. |

## Implementation Steps

1. Run responsive, keyboard, reduced-motion, contrast, copy, and offline gates; record evidence and failures.
2. Sweep `docs/design-guidelines.md`, #36, #7, and #37; separate task ownership and correct contradictions before PO review.
3. Present the proposed board and record decisions by tokens, components, shell-level flow/spec, responsive behavior, accessibility, and states.
4. Apply approved durable deltas to `docs/design-guidelines.md`, regenerate or recheck every review-board citation, then obtain final PO confirmation that the updated authority matches the approved decisions.
5. Write a durable evidence table with viewport, navigation mode, capture/reference, keyboard result, focus-return result, text-scale result, reduced-motion result, offline/network result, reviewer, and timestamp.
6. Record the approval template, final artifact revision/hash, section-16 exclusion inventory, #7 ownership split, and #37 blocking/assessment items in the stateful report.
7. Preflight GitHub authentication and access. If unavailable, save the redacted proposed comment text in the report, keep the plan blocked, and do not claim Design Approved.
8. After explicit publication approval, post redacted evidence and the decision to #36, then notify #37 with the mapping, blocking configuration gaps, and follow-ups.
9. Keep #36 open or in PO Review if any blocking decision remains; close only after all issue-level acceptance criteria pass.

## Todo

- [ ] Complete all mechanical and manual review gates.
- [ ] Record PO decision and resolve blocking comments.
- [ ] Reconcile approved deltas and run a whole-contract sweep.
- [ ] Attach evidence to #36 and notify #37.

## Risk Assessment

| Risk | Signal | Response |
|---|---|---|
| Approval is inferred from baseline status. | No explicit #36 PO decision exists. | Require a dated issue-tied decision record. |
| #37 starts from stale assumptions. | It references old colors, breakpoints, or states. | Post an approved-delta list and stable artifact link before merge. |
| Accessibility is treated as polish. | Contrast, focus, scaling, or overflow fails. | Keep #36 unapproved and correct the design artifact first. |
| GitHub is unavailable or outbound text is unsafe. | Auth/access preflight fails or redaction detects local/private content. | Preserve the exact redacted draft in the report, stop external publication, and keep Design Approved blocked. |

## Security and Privacy

Review output contains no customer data, credentials, environment values, or private logs. Links point only to repository artifacts and public GitHub issues.

## Rollback

Revert unapproved guideline deltas and mark the review record “changes requested.” Retain a rejected board only when clearly labeled, otherwise replace it with a versioned correction.

## Success Criteria

- [ ] PO decision is explicit, dated, and traceable to evidence.
- [ ] Issue #36 criteria pass or remain visibly unresolved without false closure.
- [ ] #37 receives a stable contract while production code remains untouched.
