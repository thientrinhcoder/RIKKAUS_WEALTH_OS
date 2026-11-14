## Code Review Summary — issue #59, append-only asset valuation history

### Scope
- Files: `V6__asset_valuations.sql`; `assets/api/{AssetController, AssetResponse, CreateAssetRequest, AssetValuationController, AssetValuationResponse, RecordValuationRequest}.java`; `assets/application/{AssetService, AssetValuationService, InputRules, ValuationEntry}.java`; `assets/domain/{Asset, AssetRepository, AssetValuation, AssetValuationRepository, DeclaredValuation}.java`; `ApplicationContextIT.java`, `OpenApiContractIT.java`; `openapi.json`; `.gitignore`; `docs/manual-test-guide-asset-valuations.md`
- Focus: uncommitted diff against `main` plus untracked files
- Scout findings: `AssetService.replaceMetadata` is an unlocked writer of the same `wealth.assets` row that `record` locks. `V6` adds no backfill for assets created under `V4`. `docs/api-contract-conventions.md` was not changed even though it is a listed deliverable.

### Overall Assessment
The append path itself is correct. `record` takes `SELECT ... FOR UPDATE` before it reads the snapshot date, the history has no repository or entity path that updates or deletes a row, the trigger and the cascade can run together, and ownership answers the same way on all three routes. Two defects break the plan's central acceptance criterion: "the asset's `currentValue`, `currency` and `valuationDate` equal the current record's after every append". One is a lost update this diff introduces. The other is a data-migration gap. Neither shows up in the manual run, because that run starts on an empty database and never runs `PUT` at the same time as `POST`.

### Critical Issues
None.

### High Priority

**H1. A concurrent `PUT /assets/{id}` reverts the valuation snapshot, so the catalog and the history disagree permanently.**
- `Asset.java:57-68`: this diff removed `updatable = false` from `current_value`, `currency` and `valuation_date`. The entity has no `@DynamicUpdate` (grep finds no match in `src/main`), so every Hibernate `UPDATE` of `Asset` now writes all three columns.
- `AssetService.java` `replaceMetadata` loads the row with the unlocked `assets.findById(assetId)`, then `save` + `flush`.
- Failure sequence under PostgreSQL READ COMMITTED:
  1. `PUT` loads the asset, with snapshot `18000.00 USD / 2026-05-31`.
  2. `POST /valuations` takes `findAndLockById`, appends `21250.00 / 2026-09-14`, calls `adoptValuation` and commits.
  3. `PUT`'s `UPDATE wealth.assets SET current_value=18000.00, valuation_date='2026-05-31', ... WHERE id=?` was waiting on the row lock. It now re-evaluates against the new row version and commits the stale values.
  4. Result: `GET /assets/{id}` shows `18000.00 / 2026-05-31`, while `GET /valuations` marks the `21250.00 / 2026-09-14` record `current: true`. Nothing repairs the divergence until another record dated on or after 2026-05-31 is appended. That later append compares against the reverted snapshot date, so even a record dated before 2026-09-14 would be adopted over the real current one.
- Before this diff the columns were `updatable = false`, so this race could not happen. The diff removed that protection without locking the other writer.
- Trigger: one user with two tabs or two devices, editing the name in one and recording a value in the other.
- Fix (smallest first):
  - (a) `replaceMetadata` uses `assets.findAndLockById(assetId)` instead of `findById`, so both writers serialise on the same lock that already protects appends.
  - (b) Annotate `Asset` with `@org.hibernate.annotations.DynamicUpdate`, so a metadata edit never writes the snapshot columns.
  - (a) alone is enough. (b) also protects any future writer.

**H2. Assets that already exist get no history row, so the "exactly one current record" contract is false for them.**
- `V6__asset_valuations.sql` creates the table and the trigger but has no `INSERT ... SELECT` from `wealth.assets`.
- `V4__assets.sql` is on `main` (PR #145). Any database that ran `main` with assets keeps the snapshot with no matching record.
- The plan expects such assets ("the first record of an asset created before this task shipped has no rate to carry", `plan.md:54`), but nothing creates that first record.
- Failure scenario, for an asset created before V6 with `12.002.000.000 ₫` valued on 2026-10-01:
  - `GET /assets/{id}/valuations` returns `[]`. This contradicts the published description "An asset always has at least one record, written when it was created" (`AssetValuationController.java`, list `@Operation`, and the same text in `openapi.json`).
  - `POST` a back-dated record for 2026-03-01: `supersedesARecordDated(2026-10-01)` is false, so the response says `current: false` and the snapshot stays the same.
  - `GET /valuations` then marks that same record `current: true`. `listFor` flags position 0, and `read` uses `findFirst...` with the same result. The catalog still shows `12.002.000.000 ₫`. The POST response and the GET answer disagree about one record, and the snapshot comes from no record at all.
- Fix: add a backfill to `V6` (before or after the trigger; the trigger does not fire on INSERT):
  ```sql
  INSERT INTO wealth.asset_valuations (id, asset_id, value, currency, effective_date, recorded_at)
  SELECT gen_random_uuid(), id, current_value, currency, valuation_date, created_at FROM wealth.assets;
  ```
  `recorded_at = created_at` matches what `AssetService.create` writes and what the manual guide (line 97) expects. Before V6 the valuation columns were never updatable, so `created_at` is the true time of that value.
- If no environment holds V4 data, this drops to Medium, but the migration should still be total. See Unresolved Questions.

### Medium Priority

**M1. A metadata edit can move `purchaseDate` past records already in the history, which breaks the rule `record` enforces.**
- `AssetValuationService.accept` refuses `effectiveDate < purchaseDate`.
- `AssetService.replaceMetadata` checks the new `purchaseDate` only against the snapshot `asset.getValuationDate()` (`rejectImpossibleDates(asset.getValuationDate(), ...)`). The V4 CHECK `assets_purchase_date_precedes_valuation` also looks only at the snapshot.
- Scenario:
  1. Asset bought 2025-01-01, current value dated 2026-10-01.
  2. A back-dated record for 2025-06-01 is appended (allowed).
  3. `PUT` sets `purchaseDate` to 2026-01-01. This is accepted because 2026-01-01 ≤ 2026-10-01.
  4. The history now holds a value from five months before the purchase.
  5. A later correction of that record with `effectiveDate` 2025-06-01 is refused with `effectiveDate must not be earlier than the purchase date`, so the user cannot correct it.
- Fix: in `replaceMetadata`, also compare against the earliest `effective_date` in the history. A repository method such as `findFirstByAssetIdOrderByEffectiveDateAsc` would do it. Report the error against `purchaseDate`. The alternative is to drop the purchase-date rule from `record`; that is a product call.

**M2. `docs/api-contract-conventions.md` is a listed deliverable, was not updated, and now states the opposite of the code.**
- Lines 375-386 say the valuation columns "are mapped `updatable = false` so Hibernate omits them from every `UPDATE`" and that "until #57 ships there is no way to correct a mistyped value".
- Line 463 says "There is no history table and no way to change an asset's current value."
- All three are false after this diff, and the first describes exactly the protection whose removal causes H1.
- `plan.md` Deliverables lists this file. `git diff main --stat -- docs` shows no change to it.

### Low Priority

**L1. A NUL character in `note` throws immediately and discards the errors already collected.**
- `AssetValuationService.accept` calls `InputRules.cleaned(declared.note(), "note")` after it has gathered the currency, date and rate errors. `cleaned` *throws* on `\u0000`.
- Effect: a request with a wrong currency and a NUL in the note reports only `note`. That contradicts the method Javadoc ("All of it is collected into one response").
- It still returns 400, not 500, so this is cosmetic.

### Items checked and found correct (risk calibration only)
- **Append-only, application side.** `AssetValuationRepository` extends `Repository` and declares only three finders and `save`. Every `AssetValuation` column is `updatable = false`, and no code path loads a valuation and then changes it. `save` on an assigned-id entity goes through `merge`: one extra SELECT, then INSERT, the same pattern `Asset` uses. It never produces an UPDATE.
- **Trigger versus cascade.** The ON DELETE CASCADE runs in the AFTER-DELETE RI trigger on `wealth.assets`. When it runs, the parent row is already deleted, and the plpgsql `EXISTS` (volatile, fresh snapshot) does not find it, so the cascade proceeds. The direct-DELETE and UPDATE refusals are confirmed by the recorded psql probes.
- **Self-referencing composite FK during a cascade.** It is NO ACTION, not RESTRICT, so the check is deferred to the end of the cascade's DELETE statement. By then the correction rows that reference the deleted rows are gone too. *Gap:* the manual cascade probe (guide lines 314-319) deletes `$VND`, which has no corrections. The corrections are all on `$USD`. Reasoning says this works, but deleting an asset that has a correction has not been run. One extra psql probe (`DELETE` the `$USD` asset) would close it. Without it, a failure would show up as a 500 and an asset that cannot be deleted.
- **Locking.**
  - `@Lock(PESSIMISTIC_WRITE)` plus `@Query` on a plain `Repository` interface is supported: `AbstractJpaQuery` applies the lock mode to `@Query` methods. The 4-way concurrent append runs confirm it.
  - No deadlock cycle: append takes the asset row lock, then the FK `KEY SHARE` on the same row in the same transaction. Delete takes the asset row lock and then cascades.
  - Locking another user's row before `OwnershipGuard` runs is not a material risk. The identifier is a random UUID, and the lock is released when the guard's exception rolls the transaction back. The 404 body does not depend on the lock.
- **Ownership and enumeration.**
  - All three routes run `OwnershipGuard.requireOwned` on the asset first.
  - `read` scopes the record lookup with `findByIdAndAssetId`.
  - `correctsValuationId` returns one message whether the record is missing, belongs to another asset or belongs to another user.
  - Bean validation runs before ownership, but it runs that way for a missing asset and for another user's asset alike, so no oracle results.
- **Current-record rule.** `listFor` (position 0), `read` (`findFirst...`) and the index all use `effective_date DESC, recorded_at DESC`. `record` uses `>=` on the snapshot date. That matches the order because `now` is taken after the lock, so it is later than every existing `recorded_at`. The rule holds only while the snapshot equals the first row of the history, which is exactly what H1 and H2 break.
- **Validation reaching PostgreSQL.**
  - `@Digits(17,2)` / `@Digits(13,6)` match `NUMERIC(19,2)` / `NUMERIC(19,6)`.
  - `@Size(max=2000)` counts UTF-16 units, which are always at least `char_length` code points, so the limit is safe.
  - NUL is refused before it reaches PostgreSQL.
  - Rate completeness, VND-has-no-rate and currency equality are all checked in the service.
  - The correction FK cannot race, because the referenced row cannot be deleted while its asset exists and the asset is locked.
  - The V4 CHECK `purchase_date <= valuation_date` cannot fail at commit, because `adoptValuation` only ever moves `valuation_date` forward or keeps it the same.
  - No path to a 500 was found.
- **Mapping versus migration.** Column names, nullability, `NUMERIC` precision and scale, `TEXT` for the enum and the note, `DATE` and `TIMESTAMPTZ` all match. `ddl-auto: validate` passed in `verify`.
- **Transactions.** Reads are `readOnly = true`. There are no associations, so nothing loads lazily, and `open-in-view: false` does not matter here.
- **OpenAPI.** The 201/400/404/405/406/415 responses match the behaviour, and the `Location` header matches the code. The exception is the "always at least one record" sentence and the "exactly one record is marked current ... the one the asset's currentValue ... are taken from" claim, which are untrue for the cases in H1 and H2.

### Recommended Actions
1. H1: `replaceMetadata` → `findAndLockById` (and optionally add `@DynamicUpdate` to `Asset`).
2. H2: add the backfill `INSERT ... SELECT` to `V6` before it lands on any shared database. Once applied, V6 cannot be edited.
3. M1: check a `purchaseDate` edit against the earliest effective date in the history.
4. M2: update `docs/api-contract-conventions.md` §"The valuation is read-only after creation" and the deferred-work bullet.
5. Run one psql probe: delete the `$USD` asset, which has corrections, and confirm the cascade succeeds.
6. L1: collect the NUL error from `note` instead of throwing it (optional).

### Plan Follow-ups
- Phases 1-3: implemented. H1, H2 and M1 are still open against the acceptance criteria "snapshot equals current record after every append" and "exactly one current".
- Phase 4: `openapi.json` is regenerated and the inventories are extended, but the conventions document is not done (M2).
- Phase 5: the guide exists. `plans/reports/manual-verification-261114-1716-GH-59-asset-valuation-history.md` is not present in `plans/reports/` yet, although `.gitignore` already whitelists it.

### Metrics
- Type coverage: Java, strongly typed. No raw types, no suppressions.
- Test coverage: none for this slice, by Product Owner direction (noted, not recommended).
- Linting issues: none observed. Maven was not run, by instruction.

### Unresolved Questions
- Does any environment (pilot, staging, a developer database that must be kept) already hold `wealth.assets` rows from V4? This decides whether H2 is High or Medium. The backfill is recommended either way.
- Is the purchase-date rule meant to cover the whole history (M1), or only the snapshot? This is a product call.

Status: DONE_WITH_CONCERNS
Summary: The append path, locking, trigger and ownership are sound. However, a concurrent metadata PUT can revert the valuation snapshot (H1, introduced by removing `updatable = false`), and V6 creates no history row for assets that already exist (H2). Both break the "snapshot equals current record" acceptance criterion.
Concerns/Blockers: H1 and H2 should be fixed before merge. H2 must be fixed before V6 runs on any shared database. The conventions doc deliverable and the manual-verification report are still missing.
