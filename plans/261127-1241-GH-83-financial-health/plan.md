---
title: "GH-83 Financial-health indicators and insights"
status: completed
issue: 83
parent: 23
branch: claude/issue-83-financial-health-587f90
created: 2026-11-27
---

# GH-83 — Financial-health indicators and insights

## Outcome

`GET /api/v1/financial-health?month=YYYY-MM` returns, for the signed-in user, the five financial-health
indicators of the backbone (`RIKKAUS_WEALTH_OS_MVP_BACKBONE.md:273-288`) and the six insight rules, every
figure traceable to its source records and every threshold echoed, so the result can be reproduced from
the response alone. Design #81 (approved, `plans/260920-1437-issue-81-financial-health-indicators/` in the
main checkout and `docs/financial-health-indicators-insights-review.html`) fixes the formulas, the
essential-expense grouping, the bands, the rules and the anti-double-count rule.

## Constraints

- Package `cashflow/health` (api + application) in the `cashflow` slice, beside `cashflow/reporting` and
  `cashflow/forecast`. It reuses `MonthlyCashFlowReportService` and `CashFlowForecastService` unchanged
  and reads assets and liabilities only through `ForecastSourceReader` (native SQL), never through the
  `assets`, `liabilities` or `snapshot` slices.
- Nothing stored, no migration (V14 stays the highest).
- Thresholds in `application.yml` under `rikkaus.cashflow.health`, bound by a record with no default and
  checked at start-up, as `SnapshotInsightProperties` does. They are the design #81 review fixture, not
  an approved business configuration.
- No Vietnamese string in the API; every constant is stable English.
- No unit test (Product Owner direction). The scenario matrix of phase 1 is written before the code and
  run by hand against the real API on PostgreSQL in Docker.

## Non-goals

Stored or historical indicators, per-user thresholds, a user-editable essential mapping, notifications,
Vietnamese copy, a forecast horizon other than the forecast route's own, an `asOf` parameter.

## Acceptance criteria

1. Five indicators in a fixed order, each `COMPLETE`, `PARTIAL` or `UNAVAILABLE`; `value` is `null`
   exactly when `UNAVAILABLE`; no zero stands in for a missing denominator.
2. Six rules in a fixed order, `ATTENTION`, `OK` or `NOT_EVALUATED`; a rule over a `PARTIAL` or
   `UNAVAILABLE` indicator is `NOT_EVALUATED`, never `OK`.
3. Numerators and denominators reconcile with `GET /cash-flow-reports/monthly` and
   `GET /cash-flow-forecast` read for the same user and month.
4. Thresholds and assumptions are echoed; changing a threshold through its environment variable changes
   the result; a meaningless configuration stops the application at start-up.
5. `month` malformed → `400 validation-failed` with the report's sentence; no token → `401`.
6. `./mvnw clean verify` green with the regenerated `openapi.json`; the mobile client regenerated.

## Phases

| Phase | File | Status |
|---|---|---|
| 1 | [phase-01-test-scenarios.md](phase-01-test-scenarios.md) — scenario matrix, written first | completed |
| 2 | [phase-02-health-slice-and-http.md](phase-02-health-slice-and-http.md) — properties, service, route | completed |
| 3 | [phase-03-manual-verification.md](phase-03-manual-verification.md) — run the matrix on the real API | completed |
| 4 | [phase-04-contract-docs-handover.md](phase-04-contract-docs-handover.md) — contract, docs, PR body | completed |

Evidence: [`plans/reports/manual-verification-261127-1241-GH-83-financial-health.md`](../reports/manual-verification-261127-1241-GH-83-financial-health.md).
