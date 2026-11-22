---
phase: 5
title: "Manual verification"
status: done
priority: P1
effort: "0.5d"
dependencies: [4]
---

# Phase 5: Manual verification

## Goal

Because no automated test covers this slice, produce a guide anyone can rerun and a record of what
this run actually returned, case by case.

## Files

- `docs/manual-test-guide-asset-instrument-details.md` (new)
- `plans/reports/manual-verification-261110-0844-GH-56-type-specific-asset-details.md` (new)

## Steps

1. The guide follows `docs/manual-test-guide-assets.md` so a reader who has run that one recognises
   the setup: a disposable `postgres:18.6-alpine3.24` on a port the shared volume does not use, the
   packaged jar on JDK 25 with `--spring.profiles.active=local`, two users inserted directly, and
   access tokens minted with the `local` profile's committed development secret so every request
   still passes through the real bearer-token filter.
2. Cases, each with its request, its expected status and what it proves:
   - every accepted cell of the matrix, one request per category that accepts a field group.
   - every refused cell, one request per field per refusing category, checking the field name in the
     `errors` array.
   - all four fields omitted, for all eleven categories, which is the criterion that missing
     optional data stays valid.
   - bounds: `interestRate` of `-0.01` and `100.01`, a negative price, a price with seven decimals,
     a 33-character symbol, a symbol of only blank space, a symbol containing a null character.
   - exactness: a fund holding with `costPerUnit` of `24150.123456` read back out of PostgreSQL.
   - no derivation: `quantity` 500, `currentPricePerUnit` 24150.000000 and `currentValue`
     12000000.00, which is not their product, stored and echoed exactly as sent.
   - the edit route clearing an instrument field by omitting it, and refusing one the stored
     category does not accept.
   - ownership: user B's identifier on a write carrying instrument fields answers `not-found`.
   - the table directly: an `INSERT` with `symbol` on a `CASH` row, and one with `interest_rate` of
     `150`, both refused by name by the check constraint.
3. The report records the actual status and body of every case, a status tally, and any finding
   worth a reader's attention rather than burying it.
4. No case is rerun after a fix without saying so in the report.

## Verification

The report is the verification. A case whose recorded result differs from the guide's expectation is
a defect to repair in the owning phase and then rerun, with the rerun stated.

## Risks

- **A guide that drifts from the code.** Mitigation: it is written against the regenerated contract
  from Phase 4, not from memory, and every example body is sent verbatim.
- **Evidence that records intent rather than output.** Mitigation: each row carries the status and
  the field name the server actually returned.

## Rollback

Documentation only. Reverting removes the only verification this slice has, which is why it ships in
the same change as the code.
