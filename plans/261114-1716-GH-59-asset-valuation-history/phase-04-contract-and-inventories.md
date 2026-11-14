---
phase: 4
title: "Contract and inventories"
status: completed
priority: P1
effort: "0.5d"
dependencies: [3]
---

# Phase 4: Contract and inventories

## Goal

Make the committed contract match the application again and keep the inventories exact.

## Requirements

- `openapi.json` regenerated with `verify -Dopenapi.update=true` and the diff read.
- `ApplicationContextIT` lists `asset_valuations`.
- `OpenApiContractIT` lists the two new paths.
- `docs/api-contract-conventions.md` gains the valuation resource and drops the statements that the
  value cannot be changed and that there is no history.

## Files

- `services/api/openapi/openapi.json`
- `services/api/src/test/java/com/rikkaus/wealth/ApplicationContextIT.java`
- `services/api/src/test/java/com/rikkaus/wealth/shared/openapi/OpenApiContractIT.java`
- `docs/api-contract-conventions.md`

## Validation

`./services/api/mvnw -f services/api/pom.xml verify` passes with no update flag.

## Risk and Rollback

`git checkout` of the four files.
