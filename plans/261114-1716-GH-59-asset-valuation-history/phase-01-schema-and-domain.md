---
phase: 1
title: "Schema and domain"
status: completed
priority: P1
effort: "1d"
dependencies: []
---

# Phase 1: Schema and domain

## Goal

Give the history a table that cannot be edited, and give the asset exactly one way for its current
value to change.

## Requirements

- `wealth.asset_valuations`: id, asset_id (FK, cascade), value `NUMERIC(19, 2)` ≥ 0, currency VND or
  USD, effective_date, usd_to_vnd_rate `NUMERIC(19, 6)` > 0, rate_effective_date, note ≤ 2000,
  corrects_valuation_id (self FK), recorded_at.
- Rate and rate date are both null or both present, and present only when currency is USD.
- A trigger refuses every `UPDATE`, and refuses a `DELETE` whose parent asset still exists, so the
  cascade from deleting an asset keeps working while a direct delete of history does not.
- Index on `(asset_id, effective_date DESC, recorded_at DESC)`: the listing order and the current-record
  lookup are the same query.
- `AssetValuation` entity with no setter and `updatable = false` on every column.
- `DeclaredValuation` record: what the user declared, replacing the value-triple record of the same role.
- `AssetValuationRepository`: ordered list by asset, first by asset, find by id and asset, save, flush.
  No delete, no update.
- `Asset` loses `updatable = false` on the three valuation columns and gains `adoptValuation`, the one
  mutator, with the Javadoc rewritten to say so.
- `AssetRepository.findAndLockById` with `PESSIMISTIC_WRITE`, so appends to one asset serialize.

## Files

- `services/api/src/main/resources/db/migration/V6__asset_valuations.sql` (create)
- `services/api/src/main/java/com/rikkaus/wealth/assets/domain/AssetValuation.java` (rewrite)
- `services/api/src/main/java/com/rikkaus/wealth/assets/domain/DeclaredValuation.java` (create)
- `services/api/src/main/java/com/rikkaus/wealth/assets/domain/AssetValuationRepository.java` (create)
- `services/api/src/main/java/com/rikkaus/wealth/assets/domain/Asset.java` (modify)
- `services/api/src/main/java/com/rikkaus/wealth/assets/domain/AssetRepository.java` (modify)

## Validation

`V6` applies against a fresh container; `ddl-auto: validate` accepts the entity; an `UPDATE` and a
direct `DELETE` through `psql` are refused by name while `DELETE FROM wealth.assets` still cascades.

## Risk and Rollback

Additive migration. Rollback is dropping the table and trigger and deleting the file.
