---
phase: 2
title: "Application service"
status: completed
priority: P1
effort: "1d"
dependencies: [1]
---

# Phase 2: Application service

## Goal

Hold every valuation operation behind a service whose every method names the owner first, and make the
append and the snapshot update one transaction under one row lock.

## Requirements

- `listFor(owner, assetId)`: ownership through the asset, then the ordered history.
- `read(owner, assetId, valuationId)`: ownership through the asset, then the record by id and asset, so
  a valuation of another asset is `not-found`.
- `record(owner, assetId, declared)`: lock the asset row, check ownership, apply the rules, insert the
  record, and when its effective date is on or after the asset's current valuation date adopt it as the
  asset's current value. A vanished asset is `not-found`.
- `AssetService.create` writes the first record alongside the asset.
- Rules that cannot be annotations: effective date not in the future and not before the purchase date,
  judged in `Asia/Ho_Chi_Minh`; rate date not in the future; rate and rate date together; no rate on a
  VND record; currency equal to the asset's; a correction names a record of this asset and carries a
  note; the note cleaned the way `notes` already is.
- The text-cleaning and error-shaping helpers move to a package-private `InputRules` so both services
  share one definition.

## Files

- `services/api/src/main/java/com/rikkaus/wealth/assets/application/AssetValuationService.java` (create)
- `services/api/src/main/java/com/rikkaus/wealth/assets/application/InputRules.java` (create)
- `services/api/src/main/java/com/rikkaus/wealth/assets/application/AssetService.java` (modify)

## Validation

Exercised through the HTTP routes in Phase 5.

## Risk and Rollback

Delete the two new files and restore `AssetService`.
