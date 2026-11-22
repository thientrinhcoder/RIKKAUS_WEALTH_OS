---
phase: 4
title: "Contract and inventories"
status: done
priority: P1
effort: "0.25d"
dependencies: [3]
---

# Phase 4: Contract and inventories

## Goal

Publish the regenerated contract, prove the whole gate is green without an update flag, and correct
the conventions document where it currently says these fields do not exist.

## Files

- `services/api/openapi/openapi.json`
- `docs/api-contract-conventions.md`

## Steps

1. Regenerate with
   `./services/api/mvnw -f services/api/pom.xml verify -Dopenapi.update=true`, read the diff, then
   rerun `verify` with no flag so `OpenApiContractIT` proves the committed document matches the
   running application.
2. Neither inventory needs extending, and that is worth recording rather than assuming: this task
   adds no path, so `OpenApiContractIT`'s path set is unchanged, and no table, so
   `ApplicationContextIT`'s table list is unchanged. Both still run and both still have to pass.
3. `docs/api-contract-conventions.md`:
   - the asset resource rows gain the four fields.
   - the matrix is stated once, as the server's rule, with the three cells that read oddly explained
     as `plan.md` explains them.
   - **Type-specific asset fields** is removed from "not in the contract", because that section
     currently says the table has no column for any of them and it now does.
   - the manual guide and evidence report for this task are cited beside issue #53's.
4. `plan.md` and every phase file move to completed, with the actual commands and counts.

## Verification

- `./services/api/mvnw -f services/api/pom.xml verify` with no flag, recorded with its test counts.
- Every statement added to the conventions document traced to a line of source or a recorded case.

## Risks

- **A contract regenerated from a stale build.** Mitigation: the flagless rerun is the gate, and its
  output is quoted in the plan's Result section.

## Rollback

Revert the commit. The committed contract then disagrees with the code, which `OpenApiContractIT`
fails on, so this cannot be reverted alone and that is intentional.
