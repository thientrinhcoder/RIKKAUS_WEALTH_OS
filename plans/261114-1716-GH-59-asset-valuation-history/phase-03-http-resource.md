---
phase: 3
title: "HTTP resource"
status: completed
priority: P1
effort: "1d"
dependencies: [2]
---

# Phase 3: HTTP resource

## Goal

Publish the three routes the frontend task needs and nothing that could edit history.

## Requirements

- `GET /api/v1/assets/{assetId}/valuations` lists newest first, each entry flagged `current`.
- `POST /api/v1/assets/{assetId}/valuations` answers `201` with `Location` and the record.
- `GET /api/v1/assets/{assetId}/valuations/{valuationId}` reads one record.
- No `PUT`, `PATCH` or `DELETE` on either path; they answer `method-not-allowed`.
- `CreateAssetRequest` gains optional `usdToVndRate` and `rateEffectiveDate` for the first record.
- Bean validation covers presence, sign, decimal shape and length; examples use the design's fixture
  (20.000 USD × 25.100 VND on 12/09/2026, and the 18.000 → 18.500 USD correction).
- Every operation declares `bearerAuth` and documents the statuses a client can receive.

## Files

- `services/api/src/main/java/com/rikkaus/wealth/assets/api/AssetValuationController.java` (create)
- `services/api/src/main/java/com/rikkaus/wealth/assets/api/RecordValuationRequest.java` (create)
- `services/api/src/main/java/com/rikkaus/wealth/assets/api/AssetValuationResponse.java` (create)
- `services/api/src/main/java/com/rikkaus/wealth/assets/api/CreateAssetRequest.java` (modify)
- `services/api/src/main/java/com/rikkaus/wealth/assets/api/AssetController.java` (modify)

## Validation

`spotless:check` passes and the routes appear in the served document under exactly those paths.

## Risk and Rollback

Delete the three new files and revert the two edits.
