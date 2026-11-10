# Manual verification — asset catalog and common CRUD (#53)

Date: 2026-11-03. First pass 22:47–22:49 ICT; second pass 23:03–23:10 ICT after code review. Branch
`thientrinhcoder/feat/issue-53-asset-catalog-common-crud`, cut from `main` at `e73ceb4`.

This is the behavioural evidence for issue #53. The Product Owner directed that the task ship
**without new unit or integration tests**, verified instead by exercising the running API and
recording what it answered — the same posture issue #50 shipped under. Every status and body below is
copied from a real run.

**Read this file in two parts.** The sections up to *Observations* are the first pass, which found
nothing wrong. Code review then raised three defects that the first pass had not looked for; all three
were probed, confirmed, fixed and the whole list replayed. *Second pass* records that, including one
case where my own first probe gave the wrong answer. The statuses in the first-pass sections were
re-confirmed unchanged on the replay.

The reproducible procedure is
[`docs/manual-test-guide-assets.md`](../../docs/manual-test-guide-assets.md). This file is what that
procedure returned on this run.

## How the run was set up

| | |
|---|---|
| Database | A disposable `postgres:18.6-alpine3.24` container on `127.0.0.1:5433`, created for this run and discarded after it, so the shared `rikkaus-postgres-data` volume was never touched |
| Application | The packaged jar on JDK 25.0.4.1, `--spring.profiles.active=local --server.port=8080`, started fresh against the empty database |
| Migrations | Flyway applied `V1 baseline`, `V2 identity`, `V3 settings and profile`, `V4 assets` in order: `Successfully applied 4 migrations to schema "public", now at version v4`. `spring.jpa.hibernate.ddl-auto: validate` then accepted the mappings and the context started in 2.729 s, which is what proves `V4__assets.sql` and the `Asset` entity agree on every column |
| Users | Two rows inserted directly: user A `0f2c8d14-…0c93` / `an.nguyen@example.com`, user B `7b41e590-…a842` / `bich.tran@example.com`. There is no sign-up route; accounts exist only through a Google sign-in |
| Tokens | HS256 access tokens minted with the `local` profile's committed development secret and the exact claim set `AccessTokenService` issues (`iss=rikkaus-wealth-api`, `sub`, `sid`, `iat`, `exp`), plus an expired one and one signed with a different secret. Minting rather than stubbing means every request still went through the real `BearerTokenAuthenticationFilter` and `AccessTokenService.verify` |
| Dates | Today was `2026-11-03` in `Asia/Ho_Chi_Minh`; the future date used for the refusal cases was `2026-12-01` |

**70 cases: 64 through HTTP and 6 straight into PostgreSQL.** Every one behaved as designed. The
defects review later found are not in this list — that is the point of recording them separately
rather than quietly widening the list and claiming a clean first pass.

| Status | Cases |
|---|---|
| `200` | 9 |
| `201` | 13 |
| `204` | 1 |
| `400` | 27 |
| `401` | 4 |
| `404` | 6 |
| `405` | 2 |
| `406` | 1 |
| `415` | 1 |

## All eleven categories

Cases 1–11. One create per category, each `201` with a `Location` of `/api/v1/assets/<id>`. No
category was refused and none was accepted that should not have been.

`CASH`, `BANK_ACCOUNT`, `TERM_DEPOSIT`, `LISTED_SECURITIES`, `BOND`, `INVESTMENT_FUND`, `REAL_ESTATE`,
`PRIVATE_BUSINESS_EQUITY`, `VEHICLE_OR_VALUABLE_ASSET`, `COLLECTIBLE`, `OTHER_ASSET`.

The first, with only the six required fields, shows what an absent optional field reads back as —
`null`, never `0` or `""`:

```
{"id":"7e1aab80-4332-4513-9757-bf19e598ab6b","name":"Tiền mặt dự phòng","category":"CASH","currentValue":148000000.00,"currency":"VND","valuationDate":"2026-11-03","liquidity":"HIGH","purchaseValue":null,"purchaseDate":null,"quantity":null,"institution":null,"notes":null,"createdAt":"2026-11-03T15:48:12.084476Z","updatedAt":"2026-11-03T15:48:12.084476Z"}
```

## The full optional set, and no conversion

| # | Request | Status | Result |
|---|---|---|---|
| 11 | `POST /assets`, a USD bank account with every optional field | `201` | echoed `20000.00` with `"currency":"USD"`, `quantity: 20000.000000`, `purchaseValue: 19500.00`, `purchaseDate: "2024-03-18"`, `institution` and `notes` as sent |

The value came back as the user entered it. **No VND figure appeared anywhere in the response**, which
is the boundary the slice rule enforces: the declared rate lives in `settings`, the assets slice may
not depend on it, and the client does the `502.000.000 ₫` arithmetic with the rate it already holds.

```
{"id":"c03d24de-1d84-489b-a8bd-9b74d3759f64","name":"Tài khoản USD","category":"BANK_ACCOUNT","currentValue":20000.00,"currency":"USD","valuationDate":"2026-09-12","liquidity":"HIGH","purchaseValue":19500.00,"purchaseDate":"2024-03-18","quantity":20000.000000,"institution":"Ngân hàng An Tín","notes":"Tiền gửi dự phòng bằng USD.","createdAt":"2026-11-03T15:48:12.199958Z","updatedAt":"2026-11-03T15:48:12.199958Z"}
```

## Listing and order

| # | Request | Status | Result |
|---|---|---|---|
| 12 | `GET /assets` as user A | `200` | 11 rows, newest `valuationDate` first and then by name |
| 13 | `GET /assets` as user B | `200` | `[]` — an empty catalog is an empty array, not a `404` |

The order as returned. The ten assets valued today sort by name, and the USD account dated
`2026-09-12` falls to the end:

```
2026-11-03  Căn hộ Thảo Điền
2026-11-03  Danh mục cổ phiếu HOSE
2026-11-03  Khoản đặt cọc mặt bằng
2026-11-03  Quỹ mở cổ phiếu Việt
2026-11-03  Tiền gửi 12 tháng
2026-11-03  Tiền mặt dự phòng
2026-11-03  Trái phiếu Chính phủ Việt Nam
2026-11-03  Vốn góp Công ty Mộc Việt
2026-11-03  Xe Volvo XC60
2026-11-03  Đồng hồ cơ sưu tầm
2026-09-12  Tài khoản USD
```

## Every required field, missing one at a time

| # | Request | Status | Result |
|---|---|---|---|
| 14 | no `name` | `400` | `validation-failed`, `{field: name, message: "must not be blank"}` |
| 15 | no `category` | `400` | `validation-failed`, `{field: category, message: "must not be null"}` |
| 16 | no `currentValue` | `400` | `validation-failed`, `{field: currentValue, message: "must not be null"}` |
| 17 | no `currency` | `400` | `validation-failed`, `{field: currency, message: "must not be null"}` |
| 18 | no `valuationDate` | `400` | `validation-failed`, `{field: valuationDate, message: "must not be null"}` |
| 19 | no `liquidity` | `400` | `validation-failed`, `{field: liquidity, message: "must not be null"}` |
| 20 | `"name":"   "` | `400` | `validation-failed`, `{field: name, message: "must not be blank"}` |

Each named exactly one field. None reported a second.

## Values, signs and lengths

| # | Request | Status | Result |
|---|---|---|---|
| 21 | `currentValue` of `-1.00` | `400` | `{field: currentValue, message: "must not be negative"}` |
| 22 | `currentValue` of `100.123` | `400` | `{field: currentValue, message: "must have at most 17 integer digits and 2 decimals"}` — refused before it could reach `NUMERIC(19, 2)` |
| 23 | `currentValue` of `0.00` | `201` | accepted, as intended: an emptied holding is a real state, and a negative asset is a liability with its own table |
| 24 | `quantity` of `0` | `400` | `{field: quantity, message: "must be greater than zero"}` |
| 25 | `quantity` of `-5` | `400` | `{field: quantity, message: "must be greater than zero"}` |
| 26 | `purchaseValue` of `-1.00` | `400` | `{field: purchaseValue, message: "must not be negative"}` |
| 27 | a 121-character `name` | `400` | `{field: name, message: "must be at most 120 characters"}` |
| 28 | a 2001-character `notes` | `400` | `{field: notes, message: "must be at most 2000 characters"}` |
| 29 | a 161-character `institution` | `400` | `{field: institution, message: "must be at most 160 characters"}` |

## An unrecognised enum constant

| # | Request | Status | Result |
|---|---|---|---|
| 30 | `"category":"CRYPTOCURRENCY"` | `400` | `urn:rikkaus:problem:malformed-request`, no `errors` array |
| 31 | `"currency":"EUR"` | `400` | `malformed-request` |
| 32 | `"liquidity":"VERY_HIGH"` | `400` | `malformed-request` |

`malformed-request` rather than `validation-failed`, because Jackson cannot construct the record at all
and the failure happens before bean validation runs. This is now written down in
`docs/api-contract-conventions.md` so the frontend is not surprised by it. The real client chooses from
a picker, so the case arises only from a hand-written request.

```
{"detail":"Malformed request.","instance":"/api/v1/assets","status":400,"title":"Malformed request","type":"urn:rikkaus:problem:malformed-request","correlationId":"4e210425-8062-4571-9d0a-5ab9cf7a8122"}
```

## Dates

| # | Request | Status | Result |
|---|---|---|---|
| 33 | `valuationDate` of `2026-12-01` | `400` | `{field: valuationDate, message: "must not be in the future"}` |
| 34 | `purchaseDate` of `2026-12-01` | `400` | `{field: purchaseDate, message: "must not be in the future"}` |
| 35 | valuation `2026-06-01`, purchase `2026-09-01` | `400` | `{field: purchaseDate, message: "must not be later than the valuation date"}` |
| 36 | both dates `2026-12-01` | `400` | **two** entries, `valuationDate` and `purchaseDate`, both `must not be in the future`, and no third about their order |

Case 36 is the one worth reading in full, because reporting one error at a time would have cost the
user two round trips:

```
{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be in the future","field":"valuationDate"},{"message":"must not be in the future","field":"purchaseDate"}],"correlationId":"51e0cbdf-bed6-409e-8654-324c219bf097"}
```

## Ownership

| # | Request | Status | Result |
|---|---|---|---|
| 37 | user A reads their own asset | `200` | the asset |
| 38 | user B reads user A's asset | `404` | `urn:rikkaus:problem:not-found` |
| 39 | user B reads an identifier that exists nowhere | `404` | `urn:rikkaus:problem:not-found` |
| 40 | user B `PUT`s user A's asset | `404` | `not-found`; user A's asset unchanged on re-read |
| 41 | user B `DELETE`s user A's asset | `404` | `not-found`; the row survived |

Cases 38 and 39 side by side. They agree on `type`, `title`, `status` and `detail`, and differ only in
`instance` — the request URI, which is RFC 9457's own definition and carries the identifier the caller
sent — and in `correlationId`, which differs on every response by design:

```
{"detail":"Resource not found.","instance":"/api/v1/assets/c03d24de-1d84-489b-a8bd-9b74d3759f64","status":404,"title":"Resource not found","type":"urn:rikkaus:problem:not-found","correlationId":"2d406ee5-478c-4445-b2f4-0eccfdcc3798"}
{"detail":"Resource not found.","instance":"/api/v1/assets/00000000-0000-4000-8000-000000000000","status":404,"title":"Resource not found","type":"urn:rikkaus:problem:not-found","correlationId":"bb6f1737-4d11-4b19-b18f-057be6ee3dcd"}
```

There is nothing in either body that distinguishes somebody else's record from one that was never
written, which is the property the `404`-not-`403` decision exists to produce.

## The metadata edit, and the frozen valuation

| # | Request | Status | Result |
|---|---|---|---|
| 42 | replace all seven editable members | `200` | all seven changed; `updatedAt` advanced to `…12.438861Z` while `createdAt` stayed `…12.199958Z` |
| 43 | a body also carrying `currentValue: 99999999.00`, `currency: VND`, `valuationDate: 2026-11-03`, `category: CASH` | `200` | **`currentValue` stayed `20000.00`, `currency` stayed `USD`, `valuationDate` stayed `2026-09-12`, `category` stayed `BANK_ACCOUNT`** |
| 44 | read it back | `200` | identical to 43 — the omitted optional members really were cleared in storage, not merely rendered as null |
| 45 | `institution` of `"   "`, `notes` of `"  Ghi chú có khoảng trắng.  "` | `200` | `institution: null`, `notes: "Ghi chú có khoảng trắng."` |
| 46 | no `name` | `400` | `{field: name, message: "must not be blank"}` |
| 47 | `"name":"  "` | `400` | `{field: name, message: "must not be blank"}` |
| 48 | `purchaseDate` of `2026-10-01`, after the stored valuation date of `2026-09-12` | `400` | `{field: purchaseDate, message: "must not be later than the valuation date"}` |

Case 43 is the acceptance criterion for the boundary with issue #57. The four frozen members are not
part of `UpdateAssetMetadataRequest`, so Jackson drops them — Spring Boot's default for an unknown
property — and `Asset` has no mutator that could apply them even if they arrived. The response:

```
{"id":"c03d24de-1d84-489b-a8bd-9b74d3759f64","name":"Tài khoản USD dự phòng","category":"BANK_ACCOUNT","currentValue":20000.00,"currency":"USD","valuationDate":"2026-09-12","liquidity":"MEDIUM","purchaseValue":null,"purchaseDate":null,"quantity":null,"institution":null,"notes":null,"createdAt":"2026-11-03T15:48:12.199958Z","updatedAt":"2026-11-03T15:48:12.448352Z"}
```

Case 48 also shows that the check is against the asset's **stored** valuation date, not one supplied
in the request — there is no way to supply one.

## Credentials

| # | Request | Status | Result |
|---|---|---|---|
| 49 | no `Authorization` header | `401` | `urn:rikkaus:problem:unauthorized` |
| 50 | an expired access token | `401` | `unauthorized`, body identical to 49 but for the correlation id |
| 51 | a token signed with another secret | `401` | `unauthorized`, body identical to 49 but for the correlation id |
| 52 | no credential on the member route | `401` | `unauthorized` — the member route does not leak existence to an anonymous caller either |

```
{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized","correlationId":"b8290e11-3b4c-43e6-9a69-1ba4bb69a095"}
```

## Protocol

| # | Request | Status | Result |
|---|---|---|---|
| 53 | `GET /assets/not-a-uuid` | `400` | `malformed-request` |
| 54 | `PATCH /assets/<id>` | `405` | `method-not-allowed`, `"Method 'PATCH' is not supported."` |
| 55 | `DELETE /assets` | `405` | `method-not-allowed` — no unintended collection handler |
| 56 | a `text/plain` body | `415` | `unsupported-media-type` |
| 57 | `{"name": ` — truncated JSON | `400` | `malformed-request` |
| 58 | `Accept: application/xml` | `406` | `not-acceptable`, listing `application/json, application/*+json, application/yaml` |

Every one was `application/problem+json`. Boot's default error shape appeared nowhere.

## Removal

| # | Request | Status | Result |
|---|---|---|---|
| 59 | `DELETE` an asset the viewer owns | `204` | no body |
| 60 | `GET` the deleted identifier | `404` | `not-found` |
| 61 | `DELETE` the same identifier again | `404` | `not-found` — a deleted identifier is treated exactly like one that never existed |
| 62 | `GET /assets` | `200` | one fewer row |

## The two catalogs stay apart

| # | Request | Status | Result |
|---|---|---|---|
| 63 | user B creates one asset | `201` | `Tiền mặt của Bích`, `5000000.00 VND` |
| 64 | user B lists | `200` | exactly one row, `["Tiền mặt của Bích"]` — none of user A's eleven |

Read straight from PostgreSQL afterwards:

```
assets_for_a | 11
assets_for_b | 1
```

## Exact decimals, in the database rather than in the JSON

The largest stored values, read with `psql` so no serializer is in the way:

```
Căn hộ Thảo Điền | 6100000000.00 | VND
Danh mục cổ phiếu HOSE | 2150000000.00 | VND
Tiền gửi 12 tháng | 1200000000.00 | VND
```

Summed by PostgreSQL: `11352000000.00`. Whole, unrounded, no exponent.

The applied column types, which are the other half of the requirement:

```
 current_value  | numeric | 19 | 2 | NO
 purchase_value | numeric | 19 | 2 | YES
 quantity       | numeric | 19 | 6 | YES
```

No `double precision` anywhere in the table. `ArchitectureRulesTest.monetaryFieldsUseExactDecimalTypes`
covers the Java side and passed; this covers the storage side, which no automated test asserts.

## The table refuses what the API refuses — six probes

Inserted with `psql`, bypassing the application entirely. Each raised the named check violation and
inserted nothing; `SELECT count(*)` was `0` afterwards.

| Probe | Constraint that refused it |
|---|---|
| `category` of `CRYPTOCURRENCY` | `assets_category_is_known` |
| `current_value` of `-1` | `assets_current_value_is_not_negative` |
| `currency` of `EUR` | `assets_currency_is_supported` |
| `quantity` of `0` | `assets_quantity_is_positive` |
| `purchase_date` `2026-09-01` with `valuation_date` `2026-06-01` | `assets_purchase_date_precedes_valuation` |
| `name` of `'   '` | `assets_name_is_present_and_bounded` |

This is the half of the verification that survives a future slice, a migration or a manual
correction, none of which go through bean validation.

## Second pass — three defects review found, measured and fixed

Code review of the first commit raised two should-fix findings and a nit. All three were probed against
a running instance before anything was changed, because a mechanism argument is not evidence. One
turned out worse than reported, one was initially contradicted by a badly designed probe and then
confirmed, and the nit's suggested fix would not have worked. Statuses below are from the reruns.

### Defect A — a concurrent delete answered 500, in nearly three quarters of attempts

Four simultaneous `DELETE`s of one asset, 25 rounds, 100 requests:

| | Before | After |
|---|---|---|
| `204` | 25 | 25 |
| `404` | 2 | 75 |
| **`500`** | **73** | **0** |

`PUT` racing `DELETE`, same shape, same counts. The application log named the cause:
`StaleStateException`, translated by Spring to `ObjectOptimisticLockingFailureException`, which is
neither an `ApiException` nor a Spring MVC exception and so reached the handler of last resort.

Both requests read the row, both passed the ownership guard, the first committed, and the second's
`UPDATE` or `DELETE` then matched zero rows. The review called this "low data risk" and it is — nothing
is corrupted and nothing crosses a user boundary — but it was not low frequency. **A double-tapped
delete button in issue #52 would have returned a server error almost every time.**

Fixed in `AssetService.flushTreatingAVanishedRowAsNotFound`: the write is flushed inside the
transaction and inside the method, where the failure can be caught, and a vanished row becomes the same
`not-found` the sequential second delete already produced. A row version was considered and rejected —
it would turn this into a retryable conflict, and there is no concurrent-edit feature to retry for.

### Defect B — a null character reached PostgreSQL as a 500

`U+0000` cannot be stored in a `text` column. A name of `Tiền\u0000mặt` is legal JSON, satisfies
`@NotBlank` and `@Size`, and would satisfy every check constraint on the table.

**The first probe of this wrongly came back clean, and the reason is worth recording.** It put the NUL
at the *end* of the value, where the then-current `trim()` silently removed it, so the request answered
`201` and stored a name the client had not sent. The reviewer's mechanism was right and the probe was
wrong. A NUL anywhere else was always a 500, which the rerun showed the moment the probe was corrected.

| Request | Before | After |
|---|---|---|
| `\u0000` at the end of `name` | `201`, stored as `Tiền mặt` with the character dropped | `400`, `name` / `must not contain a null character` |
| `\u0000` in the middle of `name` | `500` `internal-error` | `400`, `name` / `must not contain a null character` |
| `\u0000` in `institution` | `500` | `400`, `institution` / same message |
| `\u0000` in `notes`, on `PUT` | `500` | `400`, `notes` / same message |

Refused rather than stripped, because dropping a character and answering `201` stores something other
than what was asked for without saying so.

An unpaired surrogate was tested in the same pass and **is accepted**, `201`, stored and returned
intact by PostgreSQL. Nothing needed protecting, so no rule was invented for it.

### Defect C — a non-breaking space passed as a whole name

| `name` | Before | After |
|---|---|---|
| `"   "` (ASCII spaces) | `400`, `must not be blank` | `400`, unchanged |
| `"　"` (ideographic space) | `400`, `must not be blank` | `400`, unchanged |
| `" "` (non-breaking space) | **`201`**, an asset whose name renders as an empty row | `400`, `must not be blank` |

The review suggested `String.strip()`. **That would not have fixed it**, and the rerun is what showed
why: `strip()` removes what `Character.isWhitespace` accepts, and that method deliberately rejects the
non-breaking spaces because they are not line-breaking opportunities. The ideographic space was already
refused for the same reason — `U+3000` *is* whitespace to Java — so only the non-breaking family was
ever open. `AssetService.isBlankToAReader` is the union that closes it: Java whitespace plus every
Unicode space separator.

Switching from `trim()` to `strip()` is also what briefly turned defect B's trailing-NUL case from a
silent `201` into a 500, which is how the two came to be fixed together rather than separately.

### Everything else was rerun, and nothing else moved

The full 64-case list was replayed against the fixed application on a fresh database. The status
sequence is **identical to the first pass, case for case** — mechanically diffed after masking
identifiers, not eyeballed. `./services/api/mvnw -f services/api/pom.xml verify` passes with no update
flag: 81 unit and 50 integration tests.

### Three contract gaps closed in the same pass

- `POST /api/v1/assets` now declares the `Location` header in `openapi.json`, which the prose had
  promised and the document had not carried. One limitation the contract cannot express is recorded in
  the controller instead: `CorsConfiguration` exposes only `X-Correlation-Id`, so a browser client
  cannot read `Location` at all. The 201 body carries the same identifier, which is why that is noted
  rather than fixed by widening a CORS policy from inside this slice.
- The published error-response sets were inconsistent across the five routes — `GET` on the collection
  declared 406 but not 405, `POST` the reverse, and the three member routes neither. All five now
  declare every status they can actually return, which the manual guide already exercised.
- The guide's future-date cases hardcoded `2026-12-01`, which would have stopped being in the future
  and turned two passing rows into apparent failures. They now compute `$FUTURE`.

### Review findings deliberately not acted on

- **`AssetResponse` publishes money as a bare JSON `number`.** `NUMERIC(19,2)` permits 17 integer
  digits, beyond JavaScript's exact-integer range, so a client's `JSON.parse` would round a value above
  roughly `9.007e15`. Real pilot figures are five orders of magnitude below that, and the conventions
  document already records that no client generates code from this contract. Recorded, not changed.
- **`save` on an assigned identifier issues a `SELECT` before the `INSERT`.** One extra round trip per
  create, and the behaviour `UserSettings` already has. Changing it means `Persistable` or a different
  repository base across both slices, which is a wider change than this issue.
- **`AssetResponse` has no `required` array.** It matters to generated clients, and there are none.

## Observations that are not defects in this slice

**Bean-validation failures carry Spring's own `detail`.** Cases 14–29 and 46–47 answer with
`"detail": "Invalid request content."`, while the two rules the service raises itself (cases 33–36, 48)
answer with `"detail": "Request validation failed."`. Both are safe to show a user and both carry the
same `type`, `title` and `errors`, so a client switching on `type` — which the conventions document
already tells it to do — sees no difference. The cause is `ApiExceptionHandler.handleMethodArgumentNotValid`,
which reclassifies the problem and attaches `errors` but keeps the framework's text. It has behaved
this way for every route since issue #39 and is not specific to the asset resource, so it is recorded
here rather than changed under this issue: making the two agree would alter the published body of the
identity and settings routes as well.

**`docs/api-contract-conventions.md` points at a file that is not in the repository.** Its "What is not
here" section cites
`plans/reports/manual-verification-261101-1320-GH-50-profile-exchange-rate-settings.md`. That file
exists only in issue #50's own worktree: `.gitignore` ignores `plans/**/*` except an explicit
allowlist, and no entry was added for it, so it was never committed. This report and its guide are
allowlisted, so they will be. Restoring #50's file is that task's to do.

## What this run did not cover

- **A real Google sign-in.** No OAuth client is configured locally, so the tokens were minted. The
  filter and the verifier exercised are the production ones; the Google handoff is not.
- **Concurrency beyond the two races probed above.** Two overlapping `PUT`s both succeed and the last
  writer wins, with no conflict reported. There is no row version.
  only thing that catches a regression here. What the suite still guarantees is narrower and worth
  naming: `ApplicationContextIT` fails on a table no migration declares, `OpenApiContractIT` fails on a
  path that is published without being declared or declared without being published, and
  `ArchitectureRulesTest` fails on a controller binding an owner identifier from the request, on a
  monetary `double`, and on the assets slice reaching into another slice.
