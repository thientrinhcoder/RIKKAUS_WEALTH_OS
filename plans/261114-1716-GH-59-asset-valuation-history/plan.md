---
title: "Issue #59 append-only asset valuation history"
description: "Add the AssetValuation history table and /api/v1/assets/{assetId}/valuations resource, with the asset's current value updated atomically from the newest record and never overwritten in place."
status: completed
priority: P1
effort: "5d"
issue: 59
branch: thientrinhcoder/feat/issue-59-asset-valuation-history
tags: [backend, api, migration, assets, valuations]
blockedBy: [53]
blocks: [58, 17]
created: 2026-11-14
---

# Issue #59 append-only asset valuation history

## Outcome

A pilot user can record a new valuation for one of their assets, read every valuation ever recorded for
it, and see which record the asset's current value comes from. Recording a valuation never changes or
removes an earlier one. Each record keeps the value, its currency, the date it was true, the USD/VND
rate and rate date the user used when the asset is in USD, an optional note, and an optional pointer to
the record it corrects. The asset's `currentValue`, `currency` and `valuationDate` move in the same
transaction as the new record, so a reader of the catalog and a reader of the history can never
disagree.

## Constraints

- Spring Boot modular monolith, PostgreSQL with Flyway, exact decimals, `/api/v1` prefix, RFC 9457
  errors, ownership enforced server-side. No client-supplied owner identifier anywhere.
- The `assets` slice may depend only on `shared`. The rate a record carries is therefore the one the
  client sends — the snapshot the approved design calls *Tỷ giá USD/VND đã dùng* — and is never read
  from `settings`.
- No new automated tests, by Product Owner direction. Verification is a recorded manual run against a
  disposable PostgreSQL. The two existing inventory assertions still have to be extended.
- Issue #56's unmerged branch already uses migration `V5`. This task uses `V6` so the two file names
  cannot collide; whichever merges second still has to confirm Flyway sees the versions in order.
- No market-price integration, no fabricated data, no Phase 2 behaviour.

## Decisions the shared contract left open, settled here

The approved design for issue #57 lists three questions as *đang chờ shared contract*. This task owns
the API, so it answers them:

1. **Which record is current.** The record with the latest `effectiveDate`; among records sharing a
   date, the one recorded last. A back-dated record is history and leaves the asset's current value
   alone. A correction carrying the same date as the record it corrects therefore becomes current if
   the original was, which is what a correction is for.
2. **How a correction points at the original.** An optional `correctsValuationId` on the new record,
   which must name a valuation of the same asset. A correction requires a `note`, because the design
   makes *Lý do điều chỉnh* mandatory. The original is untouched.
3. **How the rate snapshot is stored.** `usdToVndRate` and `rateEffectiveDate` on the record itself,
   both present or both absent, permitted only when the record is in USD. Absent is the design's
   *partial* state, not an error, because the first record of an asset created before this task shipped
   has no rate to carry.

Two further rules this task adds and the design did not need to state:

- A record's `currency` must equal the asset's. A holding's currency is a property of the holding, as
  `V4__assets.sql` already records, and `purchaseValue` is denominated in it. The record still stores the
  currency, as the backbone requires, so relaxing this later is a code change and not a migration.
- A record's `effectiveDate` may not precede the asset's `purchaseDate`, for the reason the catalog
  already refuses a purchase after the valuation.

## Non-goals

- Converting a USD record to VND, or any portfolio total. The client multiplies; issue #17 owns the
  dashboard figure.
- Editing or deleting a valuation. There is no such route, and the table refuses it.
- Hardened asset deletion. Deleting an asset still removes it, and its history goes with it by cascade,
  exactly as the plain delete issue #53 shipped and issue #102 will harden.
- Freshness judgement. The API reports dates; the client decides what is stale.
- Pagination of the history. A pilot user's history is tens of rows.

## Deliverables

- `services/api/src/main/resources/db/migration/V6__asset_valuations.sql`
- `services/api/src/main/java/com/rikkaus/wealth/assets/**`, extended
- `services/api/openapi/openapi.json`, regenerated
- `docs/api-contract-conventions.md`, extended with the valuation resource
- `docs/manual-test-guide-asset-valuations.md`, the reproducible manual run
- `plans/reports/manual-verification-261114-1716-GH-59-asset-valuation-history.md`, its recorded result

## Phases

| Phase | Name | Status | Dependency | Outcome |
|---|---|---|---|---|
| 1 | [Schema and domain](./phase-01-schema-and-domain.md) | Completed | None | `wealth.asset_valuations` with its constraints and immutability trigger; the entity, the declared-valuation input, the narrow repository; `Asset` gains its one valuation mutator. |
| 2 | [Application service](./phase-02-application-service.md) | Completed | Phase 1 | Owner-scoped list, read and append under a row lock; the first record written at asset creation; the rules that cannot be annotations. |
| 3 | [HTTP resource](./phase-03-http-resource.md) | Completed | Phase 2 | Three routes under `/api/v1/assets/{assetId}/valuations`; the create-asset request gains the optional rate snapshot. |
| 4 | [Contract and inventories](./phase-04-contract-and-inventories.md) | Completed | Phase 3 | `openapi.json` regenerated, both inventories extended, the conventions document updated. |
| 5 | [Manual verification](./phase-05-manual-verification.md) | Completed | Phase 4 | A guide anyone can rerun, and the recorded evidence of this run. |

## Acceptance Criteria

- [x] Recording a valuation adds a row and changes no existing row; the table itself refuses an
      `UPDATE` or a direct `DELETE` on the history.
- [x] Every record stores value, currency, effective date, the rate and rate date used when in USD,
      and an optional note.
- [x] The asset's `currentValue`, `currency` and `valuationDate` equal the current record's after every
      append, in the same transaction, including under concurrent appends to one asset.
- [x] A back-dated record is kept and does not change the asset's current value.
- [x] A correction keeps the original readable and points at it.
- [x] Another user's asset or valuation identifier answers `not-found`, byte-identical to a missing one.
- [x] Invalid input answers `validation-failed` naming the field; a missing credential answers
      `unauthorized`.
- [x] `openapi.json` matches the running application and ships with examples; the inventories pass.
- [x] The manual guide reproduces every case and the report records the actual result of each.

## Verification gap, stated plainly

No automated test exercises the valuation resource, by the same Product Owner direction issues #50 and
#53 shipped under. A regression here is caught by rerunning `docs/manual-test-guide-asset-valuations.md`
and by nothing else, apart from the inventory and architecture assertions that still guard the schema,
the published paths and the slice boundaries.

<!-- slug: issue-59-asset-valuation-history -->

## Result

Delivered on 2026-11-14. `./services/api/mvnw -f services/api/pom.xml verify` passes with no update flag —
81 unit and 50 integration tests — so the committed `openapi.json` matches the running application and
both extended inventories agree with the schema and the published surface.

Behaviour is recorded in
[`plans/reports/manual-verification-261114-1716-GH-59-asset-valuation-history.md`](../reports/manual-verification-261114-1716-GH-59-asset-valuation-history.md):
96 cases on the final run, 79 through HTTP, 3 concurrency probes and 14 straight into PostgreSQL, every
one as designed and none rerun after a fix.

Code review of the first complete run
([`code-reviewer-261114-1716-GH-59-asset-valuation-history.md`](../reports/code-reviewer-261114-1716-GH-59-asset-valuation-history.md))
found two defects the manual run had not looked for, and both were fixed before the final run: a metadata
edit overlapping an append could write the old snapshot back over the new record, because the edit
route did not take the row lock; and assets created before `V6` had no first record, so the migration now
backfills one from each asset's own snapshot. A third finding, that an edited purchase date was judged
only against the current valuation and not against back-dated history, was fixed in the same pass. The
review's one low finding — a null character in `note` reports alone rather than alongside the other
errors — was left as it is, because the catalog's `name` and `notes` behave the same way and changing one
without the others would make the slice inconsistent.

Two contract changes shipped with examples, both additive: the three new routes, and two optional
members on `POST /api/v1/assets` for the first record's rate snapshot. Issue #58 (frontend) and #17
(dashboard) consume them.
