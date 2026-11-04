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

Give the asset slice a table whose constraints hold the backbone's rules without trusting application
code, and the Java vocabulary the rest of the slice is written against.

## Requirements

- Eleven categories exactly: cash, bank account, term deposit, listed securities, bond, investment
  fund, real estate, private business equity, vehicle or valuable asset, collectible, other asset.
- Three liquidity levels: high, medium, low. Required, with no default, because the design states the
  form offers no preselected answer and a database default would quietly supply one.
- Required columns: name, category, current value, currency, valuation date, liquidity, owner.
- Optional columns: purchase value, purchase date, quantity, institution or storage location, notes.
- Currency is VND or USD only, enforced by a check constraint as well as by the enum.
- Money is `NUMERIC(19, 2)` and quantity is `NUMERIC(19, 6)`. No floating-point type anywhere.
- A value may not be negative. A purchase value may not be negative. A quantity must be positive when
  present. These are checks in the table, not only annotations at the boundary.
- The owner is a bare `UUID` column with a foreign key to `wealth.users`, cascading on delete, and is
  never a `UserAccount` association: the slice rule forbids `assets` depending on `identity`.

## Files to Create

- `services/api/src/main/resources/db/migration/V4__assets.sql`
- `services/api/src/main/java/com/rikkaus/wealth/assets/domain/Asset.java`
- `services/api/src/main/java/com/rikkaus/wealth/assets/domain/AssetCategory.java`
- `services/api/src/main/java/com/rikkaus/wealth/assets/domain/AssetLiquidity.java`
- `services/api/src/main/java/com/rikkaus/wealth/assets/domain/AssetCurrency.java`
- `services/api/src/main/java/com/rikkaus/wealth/assets/domain/AssetRepository.java`

## Steps

1. Write the migration with a column comment for every decision a reader would otherwise have to
   reconstruct, in the voice `V2` and `V3` already use.
2. Add an index on `(user_id, valuation_date DESC)`, the order the catalog lists in.
3. Write `Asset` implementing `UserOwned`, with a static factory for creation and one mutator for the
   metadata edit, so no setter can reach the valuation columns.
4. Store the enums as `TEXT` via `@Enumerated(EnumType.STRING)`; an ordinal would make inserting a
   twelfth category in the middle silently rewrite existing rows.
5. Declare the repository with only the four operations the slice uses.

## Validation

- `V4__assets.sql` applies against a fresh container with no manual step.
- Attempting a negative value, an unknown currency and a zero quantity through `psql` is refused by
  the table itself, independently of the API.

## Risk and Rollback

The migration is additive and creates one table. Rollback is dropping `wealth.assets` and deleting the
migration file; nothing else references it yet.
