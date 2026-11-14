---
phase: 1
title: "Schema and domain"
status: done
priority: P1
effort: "0.5d"
dependencies: []
---

# Phase 1: Schema and domain

## Goal

Give the four instrument fields a column, a bound and a category rule in PostgreSQL, and a type in
the domain that makes the rule expressible in one place.

## Files

- `services/api/src/main/resources/db/migration/V5__asset_instrument_details.sql` (new)
- `services/api/src/main/java/com/rikkaus/wealth/assets/domain/AssetInstrumentDetails.java` (new)
- `services/api/src/main/java/com/rikkaus/wealth/assets/domain/AssetCategory.java`
- `services/api/src/main/java/com/rikkaus/wealth/assets/domain/Asset.java`

## Steps

1. `V5__asset_instrument_details.sql` adds `symbol TEXT`, `cost_per_unit NUMERIC(19, 6)`,
   `current_price_per_unit NUMERIC(19, 6)` and `interest_rate NUMERIC(7, 4)`, all nullable, with:
   - `symbol` bounded at 32 characters and refused when blank, matching how `V4` bounds `name`.
   - both per-unit prices refused when negative. Zero is allowed, for the same reason `V4` allows a
     `current_value` of zero: a worthless unit is a real state.
   - `interest_rate` between `0` and `100` inclusive, because the column holds a percent and
     `6.2500` is six and a quarter percent per year, not a fraction.
   - one check constraint per field group encoding the matrix: the pricing fields are null unless
     the category is `LISTED_SECURITIES`, `BOND`, `INVESTMENT_FUND` or `OTHER_ASSET`, and
     `interest_rate` is null unless the category is `BOND`, `TERM_DEPOSIT` or `OTHER_ASSET`.
   - column comments recording that every value is user-entered and that no column is derived.
2. `AssetInstrumentDetails`, a record of the four values, documented as the optional instrument half
   of an asset and as a replacement rather than a patch.
3. Two predicates on `AssetCategory`: `allowsInstrumentPricing()` and `allowsRate()`. The matrix
   lives on the enum rather than in the service, so the enum and the check constraints are the two
   statements of one rule and a twelfth category cannot be added without answering both questions.
4. `Asset` gains the four columns and applies them through the existing single write path, so create
   and edit cannot drift. The category stays `updatable = false`, which is what makes the stored
   combination of category and instrument fields permanently consistent with the constraint.

## Verification

- `./services/api/mvnw -f services/api/pom.xml verify` starts the context, so Flyway applies `V5`
  and `ddl-auto: validate` confirms the entity and the table agree on all four columns.
- Read `V5` against the matrix table in `plan.md`, cell by cell.

## Risks

- **A check constraint that contradicts the enum.** Mitigation: the predicates and the constraints
  are written from the same table in `plan.md`, and Phase 5 inserts rows directly to prove the
  constraint refuses what the API refuses.
- **Scale chosen for display rather than for arithmetic.** Mitigation: `NUMERIC(19, 6)` matches
  `quantity`, so a fund's fractional NAV per unit and a whole-dong share price both store exactly.

## Rollback

`V5` is additive and every column is nullable. Reverting the commit before the migration has run
anywhere leaves nothing behind; after it has run, a `V6` dropping the four columns restores `V4`'s
shape without touching a row's common fields.
