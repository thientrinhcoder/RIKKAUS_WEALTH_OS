---
title: "Issue #53 asset catalog and common CRUD"
description: "Build the Asset model, migration and /api/v1/assets resource for the eleven categories, the common required and optional fields, ownership enforcement and validation."
status: completed
priority: P1
effort: "5d"
issue: 53
branch: thientrinhcoder/feat/issue-53-asset-catalog-common-crud
tags: [backend, api, migration, assets]
blockedBy: []
blocks: [52, 54, 57]
created: 2026-11-03
---

# Issue #53 asset catalog and common CRUD

## Outcome

A pilot user can create, list, read, edit and delete their own assets across all eleven backbone
categories through `/api/v1/assets`, with exact decimal money, server-side ownership, RFC 9457
validation failures, and a published OpenAPI contract the frontend task (#52) can work against.

## Constraints

- Spring Boot modular monolith, PostgreSQL with Flyway, exact decimals, `/api/v1` prefix,
  RFC 9457 errors, ownership enforced server-side. No client-supplied owner identifier anywhere.
- The `assets` slice may depend only on `shared`. `ArchitectureRulesTest` fails the compilation gate
  otherwise, which is why no exchange-rate conversion happens here: the rate belongs to `settings`.
- No new automated tests, by Product Owner direction. Verification is a recorded manual run against
  a disposable PostgreSQL. The two existing inventory assertions still have to be extended, because
  they enumerate the schema's tables and the contract's paths and would otherwise fail.
- No floating point for money, no market-price integration, no fabricated data to satisfy a check.

## Non-goals

- Type-specific instrument fields — symbol, cost per unit, price per unit, interest rate — owned by #54.
- Append-only valuation history and the value-update flow, owned by #57. This is why the edit route
  cannot change `currentValue`, `currency` or `valuationDate`.
- Active search, filter and sort parameters, owned by #99. Hardened delete, undo and linked-record
  consequences, owned by #102. Pagination, which the API conventions list as deliberately absent.
- VND conversion of USD holdings, and any computed portfolio total. The client already reads the rate
  from `GET /api/v1/settings/exchange-rate`.

## Deliverables

- `services/api/src/main/resources/db/migration/V4__assets.sql`
- `services/api/src/main/java/com/rikkaus/wealth/assets/**`
- `services/api/openapi/openapi.json`, regenerated
- `docs/api-contract-conventions.md`, extended with the asset resource
- `docs/manual-test-guide-assets.md`, the reproducible manual run
- `plans/reports/manual-verification-261103-2231-GH-53-asset-catalog-common-crud.md`, the recorded
  result of that run

## Phases

| Phase | Name | Status | Dependency | Outcome |
|---|---|---|---|---|
| 1 | [Schema and domain](./phase-01-schema-and-domain.md) | Completed | None | `wealth.assets` exists with its constraints; the entity, the category and liquidity vocabulary, and the narrow repository. |
| 2 | [Application service](./phase-02-application-service.md) | Completed | Phase 1 | Owner-scoped list, create, read, metadata edit and delete, with the validation that cannot be an annotation. |
| 3 | [HTTP resource](./phase-03-http-resource.md) | Completed | Phase 2 | Five routes under `/api/v1/assets` with request and response records, bean validation and documented responses. |
| 4 | [Contract and inventories](./phase-04-contract-and-inventories.md) | Completed | Phase 3 | `openapi.json` regenerated, the schema and path inventories extended, the conventions document updated. |
| 5 | [Manual verification](./phase-05-manual-verification.md) | Completed | Phase 4 | A guide anyone can rerun, and the recorded evidence of this run. |

## Acceptance Criteria

- [x] All eleven backbone categories are accepted and published in the contract.
- [x] Every common required field is required and every common optional field is optional, matching
      `RIKKAUS_WEALTH_OS_MVP_BACKBONE.md:105-147`.
- [x] Current value is always stored and returned with its currency and valuation date.
- [x] A request carrying another user's asset identifier returns `not-found` with a body byte-identical
      to a genuinely missing record, for reads and for writes.
- [x] An anonymous request returns `unauthorized`; no route accepts an owner identifier from the client.
- [x] Invalid input returns `validation-failed` with a populated `errors` array naming the field.
- [x] Money and quantity are exact decimals in Java and in PostgreSQL.
- [x] `openapi.json` matches the running application and ships with examples.
- [x] The manual guide reproduces every case, and the evidence report records the actual result of each.
- [x] No later-phase behaviour and no seeded or fabricated data.

## Verification gap, stated plainly

No automated test exercises the asset resource. That is the Product Owner's direction for this task,
not an oversight, and it is the posture issue #50 shipped under. The consequence is that a regression
in this slice is caught by rerunning `docs/manual-test-guide-assets.md` and by nothing else. The two
inventory assertions that were extended will still catch an undeclared table and an unpublished or
leaked path.

<!-- slug: issue-53-asset-catalog-common-crud -->

## Result

Delivered on 2026-11-03. The whole verification gate passes with no update flag —
`./services/api/mvnw -f services/api/pom.xml verify`, 81 unit and 50 integration tests, which means the
committed `openapi.json` matches the running application and both extended inventories agree with the
schema and the published surface.

Behaviour is recorded in
[`plans/reports/manual-verification-261103-2231-GH-53-asset-catalog-common-crud.md`](../reports/manual-verification-261103-2231-GH-53-asset-catalog-common-crud.md):
70 cases, 64 through HTTP and 6 straight into PostgreSQL, every one as designed and none rerun after a
fix. Two findings worth a reader's attention are recorded there rather than buried: bean-validation
failures carry Spring's own `detail` text while service-raised ones carry the taxonomy's, which has
been true of every route since issue #39; and `docs/api-contract-conventions.md` cites issue #50's
manual verification report, which `.gitignore` kept out of the repository.

One change outside the slice was necessary. `.gitignore` carried a bare `assets/` rule for marketing
imagery, which matched `services/api/.../wealth/assets/` and made the entire new package invisible to
git. Without the negation added here the migration and the contract would have committed while none of
the code did.
