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

Hold every asset operation behind one service whose every method names the owner as its first
argument, so no path exists that reads or writes an asset without saying whose view it is.

## Requirements

- `listFor(owner)` returns only that owner's assets, newest valuation first then name, so the catalog
  order is the server's and not the database's incidental one.
- `create(owner, …)` assigns the identifier in application code and stamps both timestamps.
- `read(owner, assetId)` goes through `OwnershipGuard.requireOwned`, so another user's identifier is
  `not-found` and indistinguishable from a missing one.
- `updateMetadata(owner, assetId, …)` changes name, liquidity, purchase value, purchase date,
  quantity, institution and notes. It cannot change `currentValue`, `currency`, `valuationDate` or
  `category`, because the approved design makes the value read-only in the edit form and defers the
  update path to #57, and because the category is chosen once in the create flow.
- `delete(owner, assetId)` resolves ownership first and then removes the row. A non-owner gets
  `not-found` and the row is untouched.
- A valuation date or purchase date in the future is refused as `validation-failed` with a populated
  `errors` entry, judged as a Vietnamese civil date against `Asia/Ho_Chi_Minh` for the reason
  `SettingsService` already records: the UTC clock would refuse a user entering today's date in the
  evening.
- A purchase date later than the valuation date is refused the same way. The two dates describe one
  history and that order cannot be reversed.

## Files to Create

- `services/api/src/main/java/com/rikkaus/wealth/assets/application/AssetService.java`

## Steps

1. Write the service with `@Transactional` on each mutation and on the read that the guard protects.
2. Put the date comparisons here rather than in annotations, since they depend on the injected `Clock`.
3. Reuse the `Map.of("errors", List.of(Map.of("field", …, "message", …)))` extension shape the identity
   and settings slices already produce, so the client has one error body to parse.
4. Keep the omitted-optional-field rule explicit: a `PUT` carrying no `notes` clears the notes, because
   the request represents the whole metadata state.

## Validation

Exercised through the HTTP routes in Phase 5; the service has no separate entry point.

## Risk and Rollback

Delete the file. Nothing outside the slice refers to it.
