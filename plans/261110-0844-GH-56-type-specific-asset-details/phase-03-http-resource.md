---
phase: 3
title: "HTTP resource"
status: done
priority: P1
effort: "0.5d"
dependencies: [2]
---

# Phase 3: HTTP resource

## Goal

Publish the four fields on the two request bodies and the response, with the bounds bean validation
can express and the documentation a frontend reader needs.

## Files

- `services/api/src/main/java/com/rikkaus/wealth/assets/api/CreateAssetRequest.java`
- `services/api/src/main/java/com/rikkaus/wealth/assets/api/UpdateAssetMetadataRequest.java`
- `services/api/src/main/java/com/rikkaus/wealth/assets/api/AssetResponse.java`
- `services/api/src/main/java/com/rikkaus/wealth/assets/api/AssetController.java`

## Steps

1. Both requests gain the four optional members with `@Size(max = 32)` on `symbol`,
   `@DecimalMin("0")` and `@Digits(integer = 13, fraction = 6)` on the two prices, and
   `@DecimalMin("0")`, `@DecimalMax("100")` and `@Digits(integer = 3, fraction = 4)` on
   `interestRate`. `Digits` mirrors the column precision so an over-precise value is a
   `validation-failed` naming the field rather than a 500 from the driver.
2. `AssetResponse` gains the four members, each documented as null when not recorded, and
   `AssetResponse.of` passes them through.
3. The controller builds an `AssetInstrumentDetails` from each request and passes it to the service.
4. `@Operation` descriptions and `@ExampleObject`s gain the instrument case: a listed-securities
   holding with a symbol and both per-unit prices whose `currentValue` is plainly not their product,
   a term deposit with a rate alone, and the 400 a refused cell produces.
5. One new `400` example on both write routes, so a frontend reader sees the exact body:
   `{"field": "symbol", "message": "must not be set for this asset category"}`.

## Verification

- `./services/api/mvnw -f services/api/pom.xml verify`, then read the regenerated contract diff.
- Phase 5 sends every documented example verbatim and records what came back.

## Risks

- **An example that would not actually succeed.** Mitigation: Phase 5 sends each one and the
  evidence report records its status, so a wrong example is a recorded failure rather than a
  plausible-looking line in the contract.
- **`@DecimalMax` on a percent read as a business cap.** Mitigation: the schema description says the
  field is a percent per year and that `6.2500` is six and a quarter percent.

## Rollback

Revert the commit and regenerate `openapi.json`. Phases 1 and 2 remain, unreachable through HTTP.
