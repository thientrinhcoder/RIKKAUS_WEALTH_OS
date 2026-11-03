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

Make the published contract match the running application again, and extend the two assertions that
enumerate the schema and the API surface so they keep failing on anything nobody declared.

## Requirements

- `services/api/openapi/openapi.json` is regenerated with
  `./services/api/mvnw -f services/api/pom.xml verify -Dopenapi.update=true` and the diff is read, not
  merely produced.
- `ApplicationContextIT.theMigrationsCreateExactlyTheTablesTheSlicesDeclare` lists `assets` alongside
  `refresh_tokens`, `user_settings` and `users`.
- `OpenApiContractIT.publishesOnlyTheProductionApiSurface` lists the two new paths.
- `docs/api-contract-conventions.md` gains an asset-resource section covering the routes, the eleven
  categories, the required and optional fields, the frozen valuation triple, and the explicit absence
  of conversion, filtering and pagination.
- The conventions document's "What is not here" list is reconciled: domain resources are no longer
  entirely absent, and the asset resource's own omissions are named in their place.

## Files to Modify

- `services/api/openapi/openapi.json`
- `services/api/src/test/java/com/rikkaus/wealth/ApplicationContextIT.java`
- `services/api/src/test/java/com/rikkaus/wealth/shared/openapi/OpenApiContractIT.java`
- `docs/api-contract-conventions.md`

## Steps

1. Run `verify` once without the update flag first, to see the drift gate fail for the right reason.
2. Regenerate, then read the diff for an accidental `localhost`, an absolute server URL or a leaked
   test-fixture path.
3. Extend the two inventories in the same commit as the code, never afterwards.
4. Write the conventions section in the document's existing voice, describing only what is implemented.

## Validation

`./services/api/mvnw -f services/api/pom.xml verify` passes with no update flag, which proves the
committed contract matches the application and that both inventories agree with reality.

## Risk and Rollback

The only risk is regenerating the contract without reading it, which turns the drift gate into a silent
rewrite. Mitigated by step 1 and step 2. Rollback is `git checkout` of the four files.
