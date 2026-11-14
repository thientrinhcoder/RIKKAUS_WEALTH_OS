---
title: "Issue #56 type-specific asset details"
description: "Extend the asset contract and persistence with the four optional instrument fields, and enforce the accepted design's category-to-field matrix server-side."
status: completed
priority: P1
effort: "2d"
issue: 56
branch: thientrinhcoder/feat/issue-56-type-specific-asset-details
tags: [backend, api, migration, assets]
blockedBy: [53]
blocks: [55]
created: 2026-11-10
---

# Issue #56 type-specific asset details

## Outcome

A pilot user can record a symbol, a cost per unit, a current price per unit and an interest or
expected-yield rate on the asset categories where those are useful, through the same
`/api/v1/assets` routes issue #53 built. Omitting all four stays valid for every category. A field
sent for a category the accepted design never shows it on is refused by name, both at the API
boundary and by a check constraint on the table. No value is derived from any other.

## Constraints

- This task extends issue #53's work, which is not on `main`. The branch is cut from
  `thientrinhcoder/feat/issue-53-asset-catalog-common-crud` (open PR #145), because `wealth.assets`,
  `Asset`, `AssetService` and `AssetController` exist only there.
- Spring Boot modular monolith, PostgreSQL with Flyway, exact decimals, `/api/v1` prefix, RFC 9457
  errors, ownership enforced server-side. The `assets` slice may depend only on `shared`.
- No new automated tests, by Product Owner direction, continuing the posture of issues #50 and #53.
  Verification is a recorded manual run against a disposable PostgreSQL.
- All prices are manually entered. No market-price integration, and `currentValue` is never computed
  from `quantity x currentPricePerUnit`.
- Every rule lands twice: bean validation or the service refuses it at the boundary with a named
  field, and a check constraint refuses it for any caller that is not the API.

## The field contract

The visibility matrix frozen in
[`plans/260914-1938-issue-54-type-specific-asset-details/phase-01-field-contract-and-fixtures.md`](../260914-1938-issue-54-type-specific-asset-details/phase-01-field-contract-and-fixtures.md)
is a design matrix for the form. This task reads it as the server's rule too, cell for cell.

| Category | `symbol` | `costPerUnit` | `currentPricePerUnit` | `interestRate` |
|---|---|---|---|---|
| `LISTED_SECURITIES` | accepted | accepted | accepted | refused |
| `BOND` | accepted | accepted | accepted | accepted |
| `INVESTMENT_FUND` | accepted | accepted | accepted | refused |
| `TERM_DEPOSIT` | refused | refused | refused | accepted |
| `OTHER_ASSET` | accepted | accepted | accepted | accepted |
| `CASH`, `BANK_ACCOUNT`, `REAL_ESTATE`, `PRIVATE_BUSINESS_EQUITY`, `VEHICLE_OR_VALUABLE_ASSET`, `COLLECTIBLE` | refused | refused | refused | refused |

Three cells deserve the reader's attention, because each is stricter or looser than a quick reading
of the matrix suggests:

- **`LISTED_SECURITIES` refuses `interestRate`.** The matrix cell is *Không*. A listed share has no
  rate, and accepting one would invite a user to record a dividend yield the MVP never calculates.
- **`INVESTMENT_FUND` refuses `interestRate`.** The matrix cell is *Ghi chú tùy chọn* — an optional
  note, not a rate field. A fund's expected return belongs in `notes`, where nothing can mistake it
  for a figure the server computed with.
- **`OTHER_ASSET` accepts all four.** The matrix cell is *Mặc định không hiển thị*, which is
  hidden by default and not forbidden. Enforcing it as a refusal would be a rule this task invented.

`quantity` is deliberately absent from that table. It is already a common optional field accepted on
every category by issue #53, and the backbone lists it in both the common-optional and the
instrument sets because it is one datum. Narrowing it now would be a breaking change to a contract
the frontend task is already reading.

## Non-goals

- A second quantity column, or any change to which categories accept the existing one.
- Any calculation. No derived current value, no accrued interest, no yield, no gain or loss, no
  performance figure. The design artifact states this four times and the service computes nothing.
- Market-price integration, live lookup, or any field whose value the server would have to fetch.
- Append-only valuation history and the value-update flow, still issue #57's. The edit route still
  cannot change `currentValue`, `currency` or `valuationDate`.
- Vietnamese field labels on the server. The contract stays English and the client owns the copy,
  the rule `AssetCategory` already states.

## Deliverables

- `services/api/src/main/resources/db/migration/V5__asset_instrument_details.sql`
- `services/api/src/main/java/com/rikkaus/wealth/assets/**`, extended
- `services/api/openapi/openapi.json`, regenerated
- `docs/api-contract-conventions.md`, with the instrument fields moved out of "not in the contract"
- `docs/manual-test-guide-asset-instrument-details.md`
- `plans/reports/manual-verification-261110-0844-GH-56-type-specific-asset-details.md`

## Phases

| Phase | Name | Status | Dependency | Outcome |
|---|---|---|---|---|
| 1 | [Schema and domain](./phase-01-schema-and-domain.md) | Completed | None | Four columns with their bounds and the matrix as check constraints; `AssetInstrumentDetails`, the two capability predicates on `AssetCategory`, and the entity mapping. |
| 2 | [Application service](./phase-02-application-service.md) | Completed | Phase 1 | Create and edit carry instrument details; the applicability rule and the symbol cleaning that no annotation can express. |
| 3 | [HTTP resource](./phase-03-http-resource.md) | Completed | Phase 2 | The four fields on the create request, the edit request and the response, with bean validation, schemas and examples. |
| 4 | [Contract and inventories](./phase-04-contract-and-inventories.md) | Completed | Phase 3 | `openapi.json` regenerated, the whole verification gate green, the conventions document corrected. |
| 5 | [Manual verification](./phase-05-manual-verification.md) | Completed | Phase 4 | A guide anyone can rerun, and the recorded result of this run. |

## Acceptance Criteria

- [x] All four instrument fields are optional on every category that accepts them, and a request
      carrying none of them succeeds for all eleven categories.
- [x] A field sent for a category that refuses it returns `validation-failed` naming that field, and
      the table's check constraint refuses the same row inserted directly.
- [x] `interestRate` outside `0` to `100`, a negative price, an over-precise decimal and an
      over-long symbol are each refused by name.
- [x] Exact decimals in Java and in PostgreSQL, verified by reading the stored rows back.
- [x] The edit route replaces instrument details wholesale: an omitted field is cleared.
- [x] `currentValue` is never derived. A request whose `quantity x currentPricePerUnit` disagrees
      with `currentValue` is stored exactly as sent.
- [x] Another user's asset identifier still answers `not-found` for the new fields' write paths.
- [x] `openapi.json` matches the running application and ships with examples.
- [x] The manual guide reproduces every case and the evidence report records each actual result.
- [x] No Phase 2 behaviour, no market integration, no seeded or fabricated data.

## Verification gap, stated plainly

No automated test exercises the instrument fields. That is the Product Owner's direction for this
task, the posture issues #50 and #53 shipped under, and it means a regression here is caught by
rerunning `docs/manual-test-guide-asset-instrument-details.md` and by nothing else. The existing
gate still catches two things: `ApplicationContextIT` fails on an undeclared table,
`OpenApiContractIT` fails when the published contract drifts from the running application, and
`spring.jpa.hibernate.ddl-auto: validate` fails when `V5` and the entity disagree on a column.

<!-- slug: issue-56-type-specific-asset-details -->

## Result

Delivered on 2026-11-10 on `thientrinhcoder/feat/issue-56-type-specific-asset-details`, cut from
issue #53's branch because the asset module exists only there.

The whole verification gate passes with no update flag —
`./services/api/mvnw -f services/api/pom.xml verify`, 81 unit and 50 integration tests — which means
the committed `openapi.json` matches the running application, `ApplicationContextIT` still agrees
with the schema, and `OpenApiContractIT` still finds no leaked path. Neither inventory needed
extending: this task adds no route and no table. The contract diff is 150 insertions, all of them the
four fields on the two request schemas and the response plus the new examples.

Behaviour is recorded in
[`plans/reports/manual-verification-261110-0844-GH-56-type-specific-asset-details.md`](../reports/manual-verification-261110-0844-GH-56-type-specific-asset-details.md):
84 cases, 73 through HTTP and 11 straight into PostgreSQL, every one as designed and none rerun after
a fix. Flyway reported `Successfully applied 5 migrations ... now at version v5` and
`ddl-auto: validate` then accepted the mappings, which is what proves `V5` and the four new fields on
`Asset` agree on every column name, type and scale.

Two things are worth a reader's attention rather than being buried. A `symbol` sent as a JSON number
is coerced to a string and accepted, which is Jackson's default for every string field in this API
and not something this task introduced; the report records it as a finding with the mapper-wide fix it
would need. And `interestRate` is bounded to `0` through `100`, which refuses the negative yields real
bonds can carry — a deliberate simplification for a Vietnamese pilot user, named in the report's
unresolved questions so the Product Owner can widen it with a one-line change to the annotation and
the check constraint.

One refinement to the accepted option is worth stating plainly, because the question the Product
Owner answered was looser than what shipped. The agreed option described the three instrument-like
categories as accepting any subset of the four fields. The frozen matrix is more specific:
`LISTED_SECURITIES` and `INVESTMENT_FUND` refuse `interestRate`, because their matrix cells are
*Không* and *Ghi chú tùy chọn* respectively. The implementation follows the matrix, since matching it
cell for cell is what the option was chosen on. Loosening those two cells is a one-line change to
`AssetCategory.allowsRate()` and one check constraint.
