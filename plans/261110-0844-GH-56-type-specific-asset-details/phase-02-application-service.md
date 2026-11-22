---
phase: 2
title: "Application service"
status: done
priority: P1
effort: "0.5d"
dependencies: [1]
---

# Phase 2: Application service

## Goal

Carry instrument details through create and edit, and refuse the two things neither bean validation
nor the table can catch with a named field.

## Files

- `services/api/src/main/java/com/rikkaus/wealth/assets/application/AssetService.java`

## Steps

1. `create` and `replaceMetadata` take an `AssetInstrumentDetails` beside the existing
   `AssetMetadata`. The edit replaces it wholesale, so an omitted field is cleared — the semantics
   `PUT` already promises for the common optional fields.
2. `rejectFieldsNotApplicableTo(category, details)` collects one `errors` entry per field the
   category refuses, in field order, so a request carrying three inapplicable fields is one response
   naming all three rather than three round trips. The message is
   `must not be set for this asset category`.
3. The edit re-checks against the asset's **stored** category, because the category is not editable
   and the request does not carry one. This is the shape `rejectImpossibleDates` already uses for
   the purchase date against the stored valuation date.
4. `symbol` goes through the existing `cleaned` helper: surrounding blank space removed with the
   wider definition of blank that method documents, a blank-to-a-reader value reported as absent,
   and a null character refused by name rather than reaching PostgreSQL as a 500.
5. Nothing is computed. There is no code path that reads `quantity` and a per-unit price together.

## Verification

- Read the method against the matrix in `plan.md`.
- Phase 5 exercises every refused cell and every accepted cell through HTTP.

## Risks

- **An inapplicable field silently dropped instead of refused.** Mitigation: the collector runs
  before the entity is touched and throws, so there is no path that stores a cleaned-away value and
  answers 201.
- **Clearing on edit read as data loss.** Mitigation: documented on the method and in the contract,
  identical to how the common optional fields already behave.

## Rollback

Revert the commit. Phase 1's columns become unwritten nullable columns, which is `V4`'s situation
before this task.
