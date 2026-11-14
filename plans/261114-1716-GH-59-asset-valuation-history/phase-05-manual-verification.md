---
phase: 5
title: "Manual verification"
status: completed
priority: P1
effort: "1d"
dependencies: [4]
---

# Phase 5: Manual verification

## Goal

Replace the automated tests this task does not write with a guide anyone can rerun and a record of what
this run returned.

## Requirements

Cases cover, at minimum: the first record written at creation, with and without a rate; an append that
becomes current and moves the asset; a back-dated append that does not; a same-date correction that
does, with its pointer and note; every required field missing; a negative value, an over-precise value
and rate, a zero rate; a rate on a VND record; a rate without its date and a date without its rate; a
future effective date, a future rate date, an effective date before the purchase date; a currency
other than the asset's; a correction without a note, one naming another asset's record, one naming a
missing id; ownership of the asset and of the record, each `404` byte-compared with a missing id;
anonymous, expired and forged tokens; `PUT`, `PATCH` and `DELETE` on both paths; concurrent appends to
one asset with the snapshot checked afterwards; the trigger refusing an `UPDATE` and a direct `DELETE`
while an asset delete still cascades; exact decimals read back from the table.

## Files

- `docs/manual-test-guide-asset-valuations.md`
- `plans/reports/manual-verification-261114-1716-GH-59-asset-valuation-history.md`

## Validation

Every case in the guide has a verdict in the report and the counts agree.

## Risk and Rollback

Evidence drifting from code: the final run happens after the final change and the report names the
commit.
