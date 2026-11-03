---
phase: 3
title: "HTTP resource"
status: completed
priority: P1
effort: "1.5d"
dependencies: [2]
---

# Phase 3: HTTP resource

## Goal

Publish the five routes the frontend task needs, with request validation that names the offending
field and documented responses for every status a client can actually receive.

## Requirements

- `GET /api/v1/assets` lists the viewer's assets. No query parameter: filtering and search are #99's.
- `POST /api/v1/assets` creates one and answers `201` with a `Location` header.
- `GET /api/v1/assets/{assetId}` reads one.
- `PUT /api/v1/assets/{assetId}` replaces the editable metadata.
- `DELETE /api/v1/assets/{assetId}` answers `204`.
- The path variable is the asset identifier and never an owner identifier. `ArchitectureRulesTest`
  fails the compilation gate on a bound `userId`, `ownerId` or `accountId`, which is the mechanism
  rather than the convention.
- Bean validation covers presence, length, the decimal shape and the sign. `@Digits` mirrors the
  column precision so an over-long value is a `validation-failed` naming the field instead of a 500
  raised by PostgreSQL.
- Every operation declares `bearerAuth` and documents 400, 401, 404, 405 and 415 as they apply, with
  at least one real example body per request shape.
- Examples use Vietnamese market values and no environment-specific host, because `OpenApiContractIT`
  forbids `localhost` anywhere in the published document.

## Files to Create

- `services/api/src/main/java/com/rikkaus/wealth/assets/api/AssetController.java`
- `services/api/src/main/java/com/rikkaus/wealth/assets/api/CreateAssetRequest.java`
- `services/api/src/main/java/com/rikkaus/wealth/assets/api/UpdateAssetMetadataRequest.java`
- `services/api/src/main/java/com/rikkaus/wealth/assets/api/AssetResponse.java`

## Steps

1. Write the request records with `@Schema` descriptions and examples drawn from the approved design
   fixture, so the contract reads as the product rather than as placeholders.
2. Write `AssetResponse` with a package-private `of(Asset)` factory, matching `ExchangeRateResponse`.
3. Write the controller, reading the viewer from `CurrentUser.require()` on every route.
4. Keep the controller class and its methods package-private, as the two existing controllers are.

## Validation

- `./mvnw spotless:check` passes, since the formatting gate runs before anything is compiled.
- The five routes appear in the served document under exactly those paths.

## Risk and Rollback

Deleting the four files removes the routes; the slice below them is inert without a controller.
