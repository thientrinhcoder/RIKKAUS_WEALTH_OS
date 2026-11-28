---
title: "GH-92 Basic Advisor rule-based report"
status: completed
issue: 92
parent: 26
branch: claude/issue-92-basic-advisor-cc72b4
created: 2026-11-28
---

# GH-92 — Basic Advisor rule-based report

## Outcome

`GET /api/v1/advisor-report?month=YYYY-MM` (month optional, default the current month in
`Asia/Ho_Chi_Minh`) returns, for the signed-in user, one rule-based report composed from three merged
routes: the six wealth-snapshot insights (`GET /wealth-snapshot/insights`, #68), the six financial-health
rules (`GET /financial-health?month=`, #83) and the goal projections (`GET /goal-projections`, #89). Every
finding keeps the upstream observation and threshold, every suggestion names the findings, rule, records,
action and note behind it. The advisor composes; it never recalculates a figure.

Accepted design: `docs/basic-advisor-rule-based-report-review.html` (issue #90), sections 04 and 06.
Product boundary: `RIKKAUS_WEALTH_OS_MVP_BACKBONE.md:331-359`. The issue-90 plan directory named in the
brief is not in this checkout; the design HTML is the reference used.

## Constraints

- New slice `com.rikkaus.wealth.advisor` (`api`, `application`). It calls
  `SnapshotInsightService.insightsFor`, `FinancialHealthService.healthFor` and
  `GoalProjectionService.projectionsFor`, all public `@Service` classes in an `..application` package.
- `ArchitectureRulesTest.featureSlicesDoNotDependOnEachOther` is refined to ignore a dependency whose
  target resides in another slice's `..application..` package, as `ARCHITECTURE_TECHNOLOGY_DECISIONS.md`
  allows ("explicit application interfaces"); `domain`, `persistence`, `infrastructure` and `api` stay
  forbidden. The advisor imports no `..domain..` type of another slice.
- Nothing stored, no migration (V14 stays the highest), no `@ConfigurationProperties`, no threshold of its
  own: each finding echoes its upstream threshold. `priorityMarkerCount = 3` is a code constant.
- English constants only; the mobile client maps them to the approved copy.
- No unit test and no Testcontainers test (Product Owner direction). The matrix of phase 1 is written
  before the code and run by hand against the real API on PostgreSQL in Docker.

## Decisions taken in this plan

| # | Decision | Why |
|---|---|---|
| D1 | Merge is the connected components of "same `actionFamily` and at least one shared `(sourceType, sourceId)`", so three tier-1 findings that chain through a shared liability and a shared cash account become one suggestion | the pairwise rule of design §04 applied deterministically; a greedy pairwise pass would depend on order |
| D2 | `threshold.property` is the fully qualified property: `rikkaus.snapshot.insights.<name>` per snapshot rule, `rikkaus.cashflow.health.` + the upstream short name, `rikkaus.goals.projection.on-track-min-coverage-percent` | one registry readable without knowing which slice abbreviates |
| D3 | Health findings take `observed.numeratorVnd/denominatorVnd` from the indicator in `dependsOn` (reserve, burden); null otherwise | echoes the upstream calculation, computes nothing |
| D4 | Goal findings: `observed.value` = `contributionCoveragePercent`, `numeratorVnd` = the planned contribution of the assumption ledger, `denominatorVnd` = `requiredMonthlyContribution`; `threshold` = `LESS_THAN` the on-track percent | the coverage is already planned ÷ required |
| D5 | `dueDate`: `UPCOMING_LIABILITY_DUE` → `observed.date`; `LIQUIDITY_SHORT_FOR_UPCOMING_OBLIGATIONS` → earliest `PAYMENT_DUE`/`MATURITY_DUE` evidence date; goal → `TARGET_DATE`; else null | "due-type" findings only, as the brief lists |
| D6 | `noteId` precedence: goal → `USER_ASSUMED_RETURN`; `FORECAST_CASH_NEGATIVE` → `FORECAST_FROM_ENTERED_SCHEDULES`; an `EXCHANGE_RATE` or USD evidence row → `USER_DECLARED_EXCHANGE_RATE`; else `MANUAL_ENTRY_NOT_RECONCILED` | one note per finding, the most specific limit first |
| D7 | Group lists keep source order (snapshot rules, health rules, goals in the goal list's order); only `suggestions` is ranked | each group stays traceable to its upstream list |
| D8 | Health evidence carries no name, so its records have `name: null`; inside one suggestion a later record of the same key fills a null name | no lookup the upstream did not do (PO question 2) |

## Non-goals

Stored or dismissed findings, history, a cap on suggestions, per-user thresholds, Vietnamese copy, chat,
AI or LLM, a new metric, an `asOf` parameter.

## Acceptance criteria

1. One route, three groups plus `unevaluated`, `suggestions` derived from `attention` only; every finding
   belongs to exactly one group and every attention finding to exactly one suggestion.
2. Every finding's `status`, `observed` and `threshold` equal the upstream route's for the same user and
   month (checked by `scripts/recon.jq` in the same run).
3. Merge, tier, family, rank and the priority marker follow the brief; same data gives the same bytes.
4. No `action` contains a banned word or an exclamation mark.
5. `month` malformed → the same `400 validation-failed` as `/financial-health`; no token → 401; 405; 406.
6. `./mvnw clean verify` green with the refined ArchUnit rule and the regenerated `openapi.json`;
   `npm run api:check` green.

## Phases

| Phase | File | Status |
|---|---|---|
| 1 | [phase-01-test-scenarios.md](phase-01-test-scenarios.md) — scenario matrix, written first | completed |
| 2 | [phase-02-advisor-slice-and-http.md](phase-02-advisor-slice-and-http.md) — slice, ArchUnit, route | completed |
| 3 | [phase-03-manual-verification.md](phase-03-manual-verification.md) — run the matrix on the real API | completed |
| 4 | [phase-04-contract-docs-handover.md](phase-04-contract-docs-handover.md) — contract, docs, PR body | completed |

Evidence: [`plans/reports/manual-verification-261128-1036-GH-92-basic-advisor.md`](../reports/manual-verification-261128-1036-GH-92-basic-advisor.md).
