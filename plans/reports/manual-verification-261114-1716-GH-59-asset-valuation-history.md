# Manual verification — append-only asset valuation history (#59)

Date: 2026-11-14. Final run 17:53 ICT, after code review, after the three fixes it led to, and after
`verify` had passed again on the fixed sources. Branch `thientrinhcoder/feat/issue-59-asset-valuation-history`,
cut from `main` at `6e57e92`.

This is the behavioural evidence for issue #59. The Product Owner directed that the task ship **without
new unit or integration tests**, verified instead by exercising the running API and recording what it
answered — the posture issues #50 and #53 shipped under. Every status and body below is copied from a
real run. The reproducible procedure is
[`docs/manual-test-guide-asset-valuations.md`](../../docs/manual-test-guide-asset-valuations.md); this
file is what that procedure returned.

**Read this file in two parts.** The sections up to *Second pass* describe the list as it stood before
code review, every one of which the first complete run (17:42) answered as designed. Code review then
found two defects and a rule gap that the list had not probed. All three were fixed, the list was
extended with cases that exercise them, and the whole list was replayed from an empty database against
the rebuilt jar. **Every status, body and figure below is from that final replay**, including the
pre-review sections, which were re-run unchanged; the earlier run is mentioned only where it differed.

The cases are driven by a script that sends each request and asserts on the body, so the whole list
replays in seconds. The very first replay stopped at case 72 on a mistake in the script's own assertion
— it counted the delete's `204` among the appends' statuses — and not on anything the API did.

## How the run was set up

| | |
|---|---|
| Database | A disposable `postgres:18.6-alpine3.24` container on `127.0.0.1:5433`, created for this run and discarded after it |
| Application | The packaged jar on JDK 25, `--spring.profiles.active=local --server.port=8080`, started fresh against the empty database |
| Migrations | Flyway applied `V1`, `V2`, `V3`, `V4` and `V6`: `Successfully applied 5 migrations to schema "public", now at version v6`. Five and not six because `V5` belongs to issue #56's unmerged branch. `ddl-auto: validate` then accepted the mappings and the context started in 4.175 s, which is what proves `V6__asset_valuations.sql` and the `AssetValuation` entity agree on every column |
| Users | Two rows inserted directly: user A `0f2c8d14-…0c93`, user B `7b41e590-…a842` |
| Tokens | HS256 access tokens minted with the `local` profile's committed development secret and the exact claim set `AccessTokenService` issues, plus an expired one and one signed with a different secret, so every request went through the real filter and verifier |
| Dates | Today was `2026-11-14` in `Asia/Ho_Chi_Minh`; the future date for the refusal cases was `2026-12-15` |

**96 cases: 79 single HTTP requests, 3 concurrency probes of 20, 30 and 20 requests, and 14 straight
into PostgreSQL.** Every one behaved as designed. The application log holds no `ERROR` line and no
`Unhandled exception`.

| Status | Single HTTP cases |
|---|---|
| `200` | 17 |
| `201` | 11 |
| `204` | 2 |
| `400` | 32 |
| `401` | 3 |
| `404` | 7 |
| `405` | 4 |
| `406` | 1 |
| `415` | 1 |

## The first record is written when the asset is

| # | Request | Status | Result |
|---|---|---|---|
| 1 | `POST /assets`, a USD bank account with `usdToVndRate` `25100.000000` and `rateEffectiveDate` `2026-09-12` | `201` | `currentValue: 20000.00`, `valuationDate: "2026-09-12"` |
| 2 | `GET /assets/{USD}/valuations` | `200` | **one** record, `current: true`, carrying the value, currency, date, rate and rate date, `recordedAt` equal to the asset's `createdAt` |
| 3–4 | a VND asset with no rate members, then its history | `201`, `200` | one record, `usdToVndRate: null`, `rateEffectiveDate: null` |
| 5–6 | a USD asset with no rate members, then its history | `201`, `200` | one record, both rate members `null` — the design's *partial* state |
| 7 | a VND asset **with** a rate and date | `400` | two `errors`: `usdToVndRate` and `rateEffectiveDate`, each `must be absent for a VND valuation` |
| 8 | a USD asset, rate without date | `400` | `rateEffectiveDate` / `must be given together with usdToVndRate` |
| 9 | a USD asset, date without rate | `400` | `usdToVndRate` / `must be given together with rateEffectiveDate` |
| 10 | a USD asset, `rateEffectiveDate` of `2026-12-15` | `400` | `rateEffectiveDate` / `must not be in the future` |

Case 2's body, the record the asset was born with:

```
[{"id":"9021a1cb-0828-454f-b354-f561e31125f8","assetId":"ab9a9dc0-fbbc-47da-91b5-c1dfd1a55ead","value":20000.00,"currency":"USD","effectiveDate":"2026-09-12","usdToVndRate":25100.000000,"rateEffectiveDate":"2026-09-12","note":null,"correctsValuationId":null,"current":true,"recordedAt":"2026-11-14T10:53:32.335018Z"}]
```

## A newer value moves the asset, in the same transaction

| # | Request | Status | Result |
|---|---|---|---|
| 11 | `POST /assets/{USD}/valuations`, `21250.00` on `2026-09-14` with rate `25100.000000` / `2026-09-12` and a note | `201` | `Location: /api/v1/assets/{USD}/valuations/a9cf7f92-…0db7`, `current: true` |
| 12 | `GET /assets/{USD}` | `200` | `currentValue: 21250.00`, `valuationDate: "2026-09-14"`, `updatedAt` advanced to `10:53:32.535019Z`, `createdAt` unchanged at `10:53:32.335018Z`; `purchaseValue`, `purchaseDate`, `institution` untouched |
| 13 | `GET /assets/{USD}/valuations` | `200` | two records, `2026-09-14` first and current, `2026-09-12` second and not |
| 14 | `GET /assets` | `200` | the catalog lists the asset under its new valuation date |

```
{"id":"a9cf7f92-1cff-4943-9de0-420499bb0db7","assetId":"ab9a9dc0-fbbc-47da-91b5-c1dfd1a55ead","value":21250.00,"currency":"USD","effectiveDate":"2026-09-14","usdToVndRate":25100.000000,"rateEffectiveDate":"2026-09-12","note":"Cập nhật số dư sau khi đối chiếu sao kê.","correctsValuationId":null,"current":true,"recordedAt":"2026-11-14T10:53:32.535019Z"}
```

The asset's `updatedAt` after case 12 is `2026-11-14T10:53:32.535019Z` — the same instant as the record's
`recordedAt`, because both were stamped from one `clock.instant()` inside one transaction.

## An older value is history, and moves nothing

| # | Request | Status | Result |
|---|---|---|---|
| 15 | `POST`, `18000.00` on `2026-05-31`, rate `25020.000000` / `2026-05-30`, note *Số dư cuối tháng được nhập ban đầu.* | `201` | `current: false` |
| 16 | `GET /assets/{USD}` | `200` | still `21250.00` / `2026-09-14`; `updatedAt` **identical** to case 12's `10:53:32.535019Z` — the asset row was not written |
| 17 | `GET /assets/{USD}/valuations` | `200` | three records, in the order `09-14`, `09-12`, `05-31` |

## A correction adds a record and keeps the original

| # | Request | Status | Result |
|---|---|---|---|
| 18 | `POST`, `18500.00` on `2026-05-31` with `correctsValuationId` of case 15's record and the reason *Điều chỉnh số dư cuối tháng sau khi đối chiếu sao kê.* | `201` | `current: false`, `correctsValuationId` echoed |
| 19 | `GET` case 15's record | `200` | still `18000.00`, every member identical to case 15's response |
| 20 | `GET /assets/{USD}/valuations` | `200` | four records; the two `05-31` records with the correction (recorded later) **before** the original |
| 21 | `POST`, `21300.00` on `2026-09-14` correcting case 11's record, note *Sửa số dư gõ nhầm.* | `201` | `current: true` — same date as the current record, later recorded, so it supersedes |
| 22 | `GET /assets/{USD}` | `200` | `currentValue: 21300.00` |
| 23 | `GET /assets/{USD}/valuations` | `200` | five records; the correction first and current, case 11's record second and `current: false`; exactly one `true` in the list |
| 24 | a correction with no `note` | `400` | `note` / `must be given when correcting a valuation` |
| 25 | a correction with `"note":"   "` | `400` | the same — a blank note is no note |
| 26 | `correctsValuationId` of a random UUID | `400` | `correctsValuationId` / `must identify a valuation of this asset` |
| 27 | `correctsValuationId` of a record of A's **other** asset | `400` | the same |
| 28–29 | B creates an asset and reads its history | `201`, `200` | B's first record, used next |
| 30 | `correctsValuationId` of **B's** record | `400` | the same message; the three bodies of cases 26, 27 and 30 are identical apart from `correlationId` |

Case 18's record and case 19's original, side by side — nothing on the original moved:

```
{"id":"9ab68337-5b7b-4bec-b884-dc7d6374eef9", ... "value":18500.00, "effectiveDate":"2026-05-31", "note":"Điều chỉnh số dư cuối tháng sau khi đối chiếu sao kê.", "correctsValuationId":"f260a3c0-2323-4552-b276-a8e08ce02a64", "current":false, "recordedAt":"2026-11-14T10:53:32.594609Z"}
{"id":"f260a3c0-2323-4552-b276-a8e08ce02a64", ... "value":18000.00, "effectiveDate":"2026-05-31", "note":"Số dư cuối tháng được nhập ban đầu.", "correctsValuationId":null, "current":false, "recordedAt":"2026-11-14T10:53:32.570609Z"}
```

## Every required field, missing one at a time

Cases 31–33: `value`, `currency`, `effectiveDate`. Each `400` `validation-failed` with exactly one
`errors` entry, naming that field, `must not be null`.

## Values, rates, lengths

| # | Sent | Status | `errors` |
|---|---|---|---|
| 34 | `"value":"-1.00"` | `400` | `value` / `must not be negative` |
| 35 | `"value":"100.123"` | `400` | `value` / `must have at most 17 integer digits and 2 decimals` |
| 36 | `"usdToVndRate":"0"` with a date | `400` | `usdToVndRate` / `must be greater than zero` |
| 37 | `"usdToVndRate":"25100.1234567"` | `400` | `usdToVndRate` / `must have at most 13 integer digits and 6 decimals` |
| 38 | a rate and no date | `400` | `rateEffectiveDate` / `must be given together with usdToVndRate` |
| 39 | a date and no rate | `400` | `usdToVndRate` / `must be given together with rateEffectiveDate` |
| 40 | against the VND asset, a rate and a date | `400` | two entries, `usdToVndRate` and `rateEffectiveDate`, each `must be absent for a VND valuation` |
| 41 | a 2001-character `note` | `400` | `note` / `must be at most 2000 characters` |
| 42 | `"note":"Ghi\u0000chú"` | `400` | `note` / `must not contain a null character` |
| 43 | `"note":"  Ghi chú.  "`, back-dated | `201` | `note` read back as `Ghi chú.`, `current: false` |
| 44 | `"note":"   "`, back-dated | `201` | `note` read back as `null` |
| 45 | `"currency":"EUR"` | `400` | `malformed-request`, no `errors` array |

## Dates

| # | Sent | Status | `errors` |
|---|---|---|---|
| 46 | `effectiveDate` of `2026-12-15` | `400` | `effectiveDate` / `must not be in the future` |
| 47 | `rateEffectiveDate` of `2026-12-15`, with a rate | `400` | `rateEffectiveDate` / `must not be in the future` |
| 48 | `effectiveDate` of `2024-01-01`, before the purchase date `2024-03-18` | `400` | `effectiveDate` / `must not be earlier than the purchase date` |
| 49 | `"currency":"VND"` on the USD asset, a future `effectiveDate`, a rate and a date | `400` | **four** entries in one response |

Case 49's body:

```
{"detail":"Request validation failed.","instance":"/api/v1/assets/ab9a9dc0-fbbc-47da-91b5-c1dfd1a55ead/valuations","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"currency","message":"must match the asset's currency"},{"field":"effectiveDate","message":"must not be in the future"},{"field":"usdToVndRate","message":"must be absent for a VND valuation"},{"field":"rateEffectiveDate","message":"must be absent for a VND valuation"}],"correlationId":"c89d08ed-1139-4ea8-94fd-433e6b1b49af"}
```

## The currency is the asset's

| # | Sent | Status | `errors` |
|---|---|---|---|
| 50 | `"currency":"VND"` on the USD asset | `400` | `currency` / `must match the asset's currency` |
| 51 | `"currency":"USD"` on the VND asset | `400` | the same |

## Ownership

| # | Request | Status |
|---|---|---|
| 52 | B lists A's history | `404` |
| 53 | B lists the history of `00000000-0000-4000-8000-000000000000` | `404` |
| 54 | B appends to A's asset, a valid body | `404` |
| 55 | B reads one of A's records | `404` |
| 56 | A reads one of the USD asset's records **through A's own VND asset** | `404` |
| 57 | A reads a record identifier that does not exist | `404` |
| 58 | A lists B's history | `404` |

The seven bodies match on `type`, `title`, `status` and `detail` and differ only in `instance` and
`correlationId`. After case 54 the USD asset's history still had 7 records and the asset was unchanged.
Cases 52 and 53, "someone else's" and "does not exist":

```
{"detail":"Resource not found.","instance":"/api/v1/assets/ab9a9dc0-fbbc-47da-91b5-c1dfd1a55ead/valuations","status":404,"title":"Resource not found","type":"urn:rikkaus:problem:not-found","correlationId":"23ae22f2-0ade-42e2-a73f-5c75644a4a4b"}
{"detail":"Resource not found.","instance":"/api/v1/assets/00000000-0000-4000-8000-000000000000/valuations","status":404,"title":"Resource not found","type":"urn:rikkaus:problem:not-found","correlationId":"c22f5048-2cba-44d1-9f95-fa3078cde9e8"}
```

## Credentials

Cases 59–61: no header, an expired token, a forged signature. Three `401` `unauthorized`, the bodies
identical apart from `correlationId`.

## Protocol — and the routes that must not exist

| # | Request | Status | Type |
|---|---|---|---|
| 62 | `PUT` a record | `405` | `method-not-allowed` |
| 63 | `PATCH` a record | `405` | `method-not-allowed` |
| 64 | `DELETE` a record | `405` | `method-not-allowed` |
| 65 | `DELETE` the history | `405` | `method-not-allowed` |
| 66 | `GET /assets/not-a-uuid/valuations` | `400` | `malformed-request` |
| 67 | `GET /assets/{USD}/valuations/not-a-uuid` | `400` | `malformed-request` |
| 68 | a `text/plain` body | `415` | `unsupported-media-type` |
| 69 | truncated JSON | `400` | `malformed-request` |
| 70 | `Accept: application/xml` | `406` | `not-acceptable` |

All nine bodies were `application/problem+json`. The record targeted by cases 62–64 read back unchanged.

## Two requests at once

**Case 71 — four concurrent appends to one asset, same date, different values, five rounds.** Every
request answered `201` (20 of 20). After each round, `GET /assets/{USD}` and the history were compared:
exactly one record was `current: true`, it was first in the list, and the asset's `currentValue` equalled
**its** value every time.

| Round | Statuses | Asset `currentValue` | First record's `value` | Agree |
|---|---|---|---|---|
| 1 | 201 201 201 201 | 103.00 | 103.00 | yes |
| 2 | 201 201 201 201 | 104.00 | 104.00 | yes |
| 3 | 201 201 201 201 | 102.00 | 102.00 | yes |
| 4 | 201 201 201 201 | 102.00 | 102.00 | yes |
| 5 | 201 201 201 201 | 102.00 | 102.00 | yes |

The winning value differs from round to round — it is whichever request the row lock admitted last — and
the snapshot follows it every time. Without `findAndLockById` the four would read the same current date,
all decide they supersede it, and the snapshot would equal whichever `UPDATE` ran last rather than the
record the order marks current.

**Case 72 — an append racing the asset's own deletion, ten rounds.** Each round created a throwaway
asset and fired one `DELETE /assets/{id}` and two `POST .../valuations` at once. The `DELETE` answered
`204` in all ten rounds; nineteen `POST`s answered `201` and one, in round 4, answered `404` — the other
permitted outcome, an append that arrived after the row was gone. **No `500` in any replay.**

## Exactness in the database

Case 83, column types:

```
 value                 | numeric                  |                19 |             2
 usd_to_vnd_rate       | numeric                  |                19 |             6
```

Case 84, the USD asset's history as stored — the rate exactly as sent, the corrections flagged, and
thirty same-day rows from cases 71 and 73 above these:

```
 21300.00 | USD      | 2026-09-14     |    25100.000000 | 2026-09-12          | t
 21250.00 | USD      | 2026-09-14     |    25100.000000 | 2026-09-12          | f
 20000.00 | USD      | 2026-09-12     |    25100.000000 | 2026-09-12          | f
 18500.00 | USD      | 2026-05-31     |    25020.000000 | 2026-05-30          | t
 18000.00 | USD      | 2026-05-31     |    25020.000000 | 2026-05-30          | f
 17000.00 | USD      | 2026-04-30     |                 |                     | f
 17100.00 | USD      | 2026-04-29     |                 |                     | f
```

## The snapshot agrees with the history, for every asset

Case 85: the join that finds any asset whose `current_value` or `valuation_date` differs from its first
record's returned **`(0 rows)`**, across every asset in the database after all of the above.

## The table refuses what the API has no route for — twelve probes

| # | Statement | Result |
|---|---|---|
| 86 | `UPDATE wealth.asset_valuations SET value = 1 WHERE id = …` | `ERROR: wealth.asset_valuations is append-only: a valuation is never updated. Record a new one.` |
| 87 | `DELETE FROM wealth.asset_valuations WHERE id = …` | `ERROR: wealth.asset_valuations is append-only: a valuation is never deleted while its asset exists.` |
| — | the targeted record, read back through the API | still `18000.00` |
| 88 | the VND asset's history count | `1` |
| 89 | `DELETE /assets/{VND}` through the API | `204` |
| 90 | the VND asset's history count | `0` — the cascade still works, because the trigger lets a delete through once the parent row is gone |
| 91 | `INSERT` a VND row carrying a rate | `violates check constraint "asset_valuations_rate_only_for_usd"` |
| 92 | `INSERT` a USD row with a rate and no date | `violates check constraint "asset_valuations_rate_is_complete"` |
| 93 | `INSERT` a negative `value` | `violates check constraint "asset_valuations_value_is_not_negative"` |
| 94 | `INSERT` a correction naming another asset's record | `violates foreign key constraint "asset_valuations_correction_names_a_record_of_this_asset"` |

The USD asset's history count was the same before and after the four refused inserts.

| # | Statement | Result |
|---|---|---|
| 95 | `DELETE /assets/{USD}` through the API — 37 records, two of them corrections pointing at other rows of the same cascade | `204` |
| 96 | the USD asset's history count | `0` — the composite foreign key on `corrects_valuation_id` is `NO ACTION`, checked at the end of the cascade, so a correction and its original go together |

Case 95 exists because review noticed the first cascade probe (case 89) deleted an asset with no
corrections, which left the self-referencing foreign key untested under a cascade.

## Second pass — what review found, and what the replay shows

Code review of the first complete run
([`code-reviewer-261114-1716-GH-59-asset-valuation-history.md`](code-reviewer-261114-1716-GH-59-asset-valuation-history.md))
raised two high findings, two medium and one low. The two high and one medium were defects in the
change; the other medium was the conventions document, which was being written while the review ran.
Each defect was fixed, each got cases of its own, and the whole list was replayed. The cases below are
numbered as they ran in the final replay.

### H1 — a metadata edit overlapping an append could roll the asset's value back

This diff had made the three snapshot columns updatable, and Hibernate writes every column of an entity
on `UPDATE`. `PUT /assets/{id}` loaded the row without a lock, so an edit that read the asset before a
concurrent append committed would write the old value, currency and date straight back over the new
ones, and the catalog would disagree with the history until the next append. The manual list had never
sent a `PUT` and a `POST` at once.

**Fix:** `AssetService.replaceMetadata` now loads the row with the same `findAndLockById` the append
uses, so the two are applied one after the other.

**Case 73 — one `PUT` and one `POST` fired together, ten rounds**, each append dated today so that it
supersedes the current record and the snapshot genuinely has to move:

| Round | `PUT` | `POST` | Asset `currentValue` after | First record's `value` | Note landed |
|---|---|---|---|---|---|
| 1 | 200 | 201 | 30000.00 | 30000.00 | yes |
| 2 | 200 | 201 | 30001.00 | 30001.00 | yes |
| 3 | 200 | 201 | 30002.00 | 30002.00 | yes |
| 4 | 200 | 201 | 30003.00 | 30003.00 | yes |
| 5 | 200 | 201 | 30004.00 | 30004.00 | yes |
| 6 | 200 | 201 | 30005.00 | 30005.00 | yes |
| 7 | 200 | 201 | 30006.00 | 30006.00 | yes |
| 8 | 200 | 201 | 30007.00 | 30007.00 | yes |
| 9 | 200 | 201 | 30008.00 | 30008.00 | yes |
| 10 | 200 | 201 | 30009.00 | 30009.00 | yes |

The snapshot followed the append and the edit's note landed in every round. One honest note on the
probe itself: its first version dated the appends in October, earlier than the day's existing records,
so none of them became current and the probe would have passed with or without the fix. It was
corrected to use today's date before the final replay, and the figures above are from that version.

### H2 — assets created before `V6` had no history

`V4` is on `main` and deployed. `V6` as first written created the table and nothing else, so an asset
created before it would answer an empty history — contradicting the contract's "at least one record" —
and the first back-dated record appended to it would become its only, and so its current, record while
the catalog kept showing the older snapshot.

**Fix:** `V6` now inserts one record for every asset that has none, from the asset's own snapshot, with
`recorded_at` equal to `created_at` and no rate — the design's partial state, not an invented figure.

| # | Step | Result |
|---|---|---|
| 77 | `INSERT` an asset straight into `wealth.assets` with no history row, as `V4` left them: *Căn hộ Thảo Điền*, `12002000000.00 VND`, valued `2026-10-01`, created `2026-10-01T03:00:00Z` | `INSERT 0 1` |
| 78 | `GET` its history | `200`, `[]` — the situation before the fix |
| 79 | replay `V6`'s backfill statement, character for character | `INSERT 0 1` — only the legacy asset lacked a record; every asset created through the API already had one |
| 80 | `GET` its history | one record, `value: 12002000000.00`, `effectiveDate: "2026-10-01"`, both rate members `null`, `current: true`, `recordedAt: "2026-10-01T03:00:00Z"` |
| 81 | append `11500000000.00` dated `2026-03-01` | `201`, `current: false`; the asset still reads `12002000000.00` |
| 82 | rows with `recorded_at = 2026-10-01T03:00:00Z` | `1` |

```
[{"id":"a08946ed-9c56-4e2e-96e2-3373caf19612","assetId":"3761355c-61d3-405c-89c4-d07b727fa6d9","value":12002000000.00,"currency":"VND","effectiveDate":"2026-10-01","usdToVndRate":null,"rateEffectiveDate":null,"note":null,"correctsValuationId":null,"current":true,"recordedAt":"2026-10-01T03:00:00Z"}]
```

This replays the statement against a running database; it does not run the migration itself against a
`V4` database, because the only way to do that is a second container at an older commit. The statement
is the whole of the change, so the replay is the evidence.

### M1 — an edited purchase date could land after records already in the history

`PUT` judged a new `purchaseDate` against the current valuation date only. With a back-dated record in
the history, a purchase date between that record and the current date was accepted, after which the
history said the asset was valued before it was owned and the back-dated record could not be corrected.

**Fix:** the purchase date is now judged against the earliest recorded valuation, under the message the
catalog has always used.

| # | `PUT /assets/{USD}` with `purchaseDate` | Status | `errors` |
|---|---|---|---|
| 74 | `2026-05-01` — after the `2026-04-29` record, before the current `2026-11-14` | `400` | `purchaseDate` / `must not be later than the valuation date` |
| 75 | `2026-04-29` — equal to the earliest record | `200` | |
| 76 | `2024-03-18` — back to the original | `200` | |

### Review findings deliberately not acted on

- **L1 — a null character in `note` is reported alone.** `InputRules.cleaned` refuses it before the
  other rules have run, so a request with a `\u0000` and, say, a bad date gets one error and then the
  other on the next attempt. The catalog's `name`, `institution` and `notes` behave the same way, and
  were accepted that way in issue #53's review; changing the note alone would make the slice inconsistent
  with itself. Recorded, not changed.

### Everything else was replayed, and nothing moved

Cases 1–72 and 83–94 answered the same statuses, the same `errors` and the same ordering as on the first
complete run. The twenty extra rows from case 73 appear at the top of case 84's dump, as expected.

## Observations that are not defects

- **Bean-validation failures carry Spring's own `detail`** (`Invalid request content.`) while the
  rules the service raises carry the taxonomy's (`Request validation failed.`). Same `type`, same
  `errors` shape; the conventions document already tells a client to switch on `type`. This has been
  true of every route since issue #39 and is not specific to this resource.
- **The append-versus-delete race rarely produces the `404` branch.** The `POST` usually wins the row
  lock because the `DELETE` arrives a few milliseconds later; two `404`s in thirty rounds across the
  three replays. Both branches are the designed answers and neither is a `500`.
- **Money is published as a bare JSON number**, as on the catalog, with the same note about the
  JavaScript exact-integer range. Realistic figures are orders of magnitude below it.

## What this run did not cover

- **A real Google sign-in.** The tokens were minted; the filter and verifier exercised are the
  production ones.
- **Running `V6` itself against a database that holds `V4` data.** The backfill statement was replayed
  by hand (cases 77–82); the migration as a whole was applied only to empty databases, in `verify` and in
  these runs.
- **Two appends with identical `effectiveDate` and identical `recordedAt`.** The row lock serialises
  appends to one asset, so two records of one asset cannot be stamped in the same transaction; the tie
  would need two instants equal to the microsecond across two serialised transactions, which the run
  did not and could not produce.
- **The frontend task's reconciliation** (issue #58) and the dashboard figure (issue #17), which read
  this resource and are theirs to verify.
