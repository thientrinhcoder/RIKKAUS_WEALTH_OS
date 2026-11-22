# Manual verification — type-specific asset details (#56)

**Outcome: 84 cases, 73 through HTTP and 11 against PostgreSQL directly, every one as designed. No
case was rerun after a fix.**

This is the whole of the verification for the four instrument fields. No automated test exercises
them, by Product Owner direction, which is the posture issues #50 and #53 shipped under. The
reproducible procedure is
[`docs/manual-test-guide-asset-instrument-details.md`](../../docs/manual-test-guide-asset-instrument-details.md);
this file is what it returned on 2026-11-10.

One observation is recorded in **Findings** below rather than left for a reader to notice: a `symbol`
sent as a JSON number is coerced to a string and accepted. It is Jackson's default for every string
field in this API, not something this task introduced.

## How the run was set up

| | |
|---|---|
| Database | A disposable `postgres:18.6-alpine3.24` container on `127.0.0.1:5433`, created for this run and removed after it, so the shared `rikkaus-postgres-data` volume was never touched |
| Application | The packaged jar on JDK 25.0.4.1, `--spring.profiles.active=local --server.port=8080`, started fresh against the empty database |
| Migrations | Flyway applied all five in order: `Successfully applied 5 migrations to schema "public", now at version v5 (execution time 00:00.043s)`. `spring.jpa.hibernate.ddl-auto: validate` then accepted the mappings and Tomcat started, which is what proves `V5__asset_instrument_details.sql` and the four new fields on `Asset` agree on every column name, type and scale |
| Users | Two rows inserted directly: user A `0f2c8d14-…0c93` / `an.nguyen@example.com`, user B `7b41e590-…a842` / `bich.tran@example.com`. There is no sign-up route |
| Tokens | HS256 access tokens minted with the `local` profile's committed development secret and the exact claim set `AccessTokenService` issues, so every request still went through the real `BearerTokenAuthenticationFilter` and `AccessTokenService.verify` |
| Dates | Today was `2026-11-10` in `Asia/Ho_Chi_Minh`; the future date used for the combined-error case was `2026-12-01` |

The automated gate was green before the run and is quoted in the plan's Result section:
`./services/api/mvnw -f services/api/pom.xml verify` with no update flag, 81 unit and 50 integration
tests, which is what proves the committed `openapi.json` matches the running application.

## Status tally

| Status | Cases |
|---|---|
| `201` | 25 |
| `200` | 4 |
| `400` | 43 |
| `401` | 1 |
| `404` | 3 |
| Refused by a check constraint | 9 |
| Accepted by the table | 1 |

## Every accepted cell of the matrix

| # | Request | Status | Result |
|---|---|---|---|
| 1 | `LISTED_SECURITIES` with `symbol`, `costPerUnit`, `currentPricePerUnit` | `201` | echoed `VCB`, `21800.000000`, `24150.000000`, `interestRate: null` |
| 2 | `BOND` with all four | `201` | echoed `ATB2029`, `1000000.000000`, `1012500.000000`, `8.5000` |
| 3 | `INVESTMENT_FUND` with the three pricing fields | `201` | echoed `VFMVF1`, `22400.500000`, `24150.123456` |
| 4 | `TERM_DEPOSIT` with `interestRate` alone | `201` | echoed `6.2500`, the other three `null` |
| 5 | `OTHER_ASSET` with all four | `201` | echoed `KHAC-01`, `500000.000000`, `750000.000000`, `12.0000` |

## Every refused cell

Twenty-four cases: each of the six categories the matrix marks as having no instrument section, sent
each of the four fields on its own.

| # | Categories | Field | Status | Result |
|---|---|---|---|---|
| 6–29 | `CASH`, `BANK_ACCOUNT`, `REAL_ESTATE`, `PRIVATE_BUSINESS_EQUITY`, `VEHICLE_OR_VALUABLE_ASSET`, `COLLECTIBLE` | each of `symbol`, `costPerUnit`, `currentPricePerUnit`, `interestRate` | `400` ×24 | every one `validation-failed` with a single `errors` entry naming the field sent: `must not be set for this asset category` |

Then the four narrower refusals, which are the cells most likely to be implemented as "whatever the
pricing group allows" and therefore the ones that prove the matrix was read cell by cell:

| # | Request | Status | Result |
|---|---|---|---|
| 30 | `LISTED_SECURITIES` with `interestRate` | `400` | `{field: interestRate, message: "must not be set for this asset category"}` — the matrix cell is *Không* |
| 31 | `INVESTMENT_FUND` with `interestRate` | `400` | same — the matrix cell is *Ghi chú tùy chọn*, a note rather than a rate field |
| 32 | `TERM_DEPOSIT` with `symbol` | `400` | `{field: symbol, …}` |
| 33 | `TERM_DEPOSIT` with `currentPricePerUnit` | `400` | `{field: currentPricePerUnit, …}` |

| # | Request | Status | Result |
|---|---|---|---|
| 34 | `CASH` carrying all four fields | `400` | **four** `errors` entries in contract order — `symbol`, `costPerUnit`, `currentPricePerUnit`, `interestRate` — one response rather than four round trips |

## Omitting all four, for all eleven categories

| # | Request | Status | Result |
|---|---|---|---|
| 35–45 | one `POST` per category with no instrument field at all | `201` ×11 | every one accepted, all four fields `null` in the response. This is the acceptance criterion that data missing the optional fields stays valid, and it holds for the six categories that refuse them as much as for the five that accept them |

## Bounds

| # | Request | Status | Result |
|---|---|---|---|
| 46 | `interestRate` of `-0.0100` | `400` | `{field: interestRate, message: "must not be negative"}` |
| 47 | `interestRate` of `100.0100` | `400` | `{field: interestRate, message: "must be at most 100"}` |
| 48 | `interestRate` of `100.0000` | `201` | accepted — the bound is inclusive, and the stored value read back as `100.0000` |
| 49 | `interestRate` of `6.25001` | `400` | `must have at most 3 integer digits and 4 decimals`, refused before it could reach `NUMERIC(7, 4)` |
| 50 | `interestRate` of `1000.0000` | `400` | **two** entries, both on `interestRate`: `must be at most 100` and the digits message. Two annotations each have something true to say about one value, and both are reported |
| 51 | `costPerUnit` of `-1.000000` | `400` | `{field: costPerUnit, message: "must not be negative"}` |
| 52 | `currentPricePerUnit` of `0.000000` | `201` | accepted, as intended: a unit currently worth nothing is a real state |
| 53 | `currentPricePerUnit` of `1.0000001` | `400` | `must have at most 13 integer digits and 6 decimals` |
| 54 | a 33-character `symbol` | `400` | `{field: symbol, message: "must be at most 32 characters"}` |
| 55 | a 32-character `symbol` | `201` | accepted and stored whole |

## A symbol that is not a symbol

| # | Request | Status | Result |
|---|---|---|---|
| 56 | `"symbol":"   "` on `LISTED_SECURITIES` | `201` | stored as `null`. A field of only blank space is one the user left alone |
| 57 | `"symbol":"   "` on `CASH` | `201` | stored as `null` **and not refused**. This is the ordering that matters: the symbol is cleaned before the category rule runs, so a user who typed nothing is never told their category refuses a field they did not send |
| 58 | `"symbol":"VC\u0000B"` | `400` | `{field: symbol, message: "must not contain a null character"}`. Refused here rather than failing inside the PostgreSQL driver, which cannot store `U+0000` in a `text` column and would have returned `internal-error` |
| 59 | `"symbol":"  VCB  "` | `201` | stored as `VCB` |
| 60 | `"symbol":" "`, one non-breaking space | `201` | stored as `null`. This is the case that needed the wider definition of blank: `U+00A0` passes `@NotBlank`, survives `strip()` and satisfies `btrim`, so a narrower helper would have stored a symbol that renders as an empty field |

## Nothing is derived

| # | Request | Status | Result |
|---|---|---|---|
| 61 | `quantity` `500`, `currentPricePerUnit` `24150.000000`, `currentValue` `12000000.00` | `201` | stored and echoed `12000000.00`. Their product is `12,075,000`, and the server neither computed it, checked it, nor complained about the difference. The asset is worth what the user said it is worth |

## An instrument problem and a date problem together

| # | Request | Status | Result |
|---|---|---|---|
| 62 | `CASH` with a `symbol` and a `valuationDate` of `2026-12-01` | `400` | **two** entries in one body: `{valuationDate, "must not be in the future"}` and `{symbol, "must not be set for this asset category"}`. The two rules are collected and thrown once, so a request wrong in two unrelated ways is one round trip |

## The edit route

| # | Request | Status | Result |
|---|---|---|---|
| 63 | `PUT` the fund with a new `currentPricePerUnit` | `200` | `25000.000000`, with the symbol and cost unchanged |
| 64 | `PUT` the same fund with all four fields omitted | `200` | all four `null`. This is a replacement and not a patch, which is how a ticker recorded by mistake is removed |
| 65 | `PUT` the fund with `interestRate` | `400` | `{field: interestRate, …}`, judged against the asset's **stored** category, which the body does not carry |
| 66 | `PUT` a `CASH` asset with a `symbol` | `400` | `{field: symbol, …}` |
| 67 | `PUT` the fund with `symbol` alone | `200` | symbol restored, the two prices cleared — the replacement semantics again |

## Ownership

| # | Request | Status | Result |
|---|---|---|---|
| 68 | user B `PUT`s user A's fund, carrying a `symbol` | `404` | `not-found`, `detail: "Resource not found."` |
| 69 | user B `PUT`s it carrying an `interestRate` the stored category refuses | `404` | `not-found`, the **same** body. Ownership is resolved before the instrument fields are judged, so which refusal a request would have got cannot be used to learn a stored asset's category |
| 70 | user B `GET`s it | `404` | identical body |

## Credentials and malformed bodies

| # | Request | Status | Result |
|---|---|---|---|
| 71 | `POST` with instrument fields and no token | `401` | `unauthorized`, before any validation runs |
| 72 | `"symbol": 123`, a JSON number | `201` | stored as the string `123`. See **Findings** |
| 73 | `"interestRate": "sáu phẩy hai lăm"`, a JSON string | `400` | `malformed-request`, with no `errors` array, because Jackson cannot build the record at all and the failure happens before bean validation |

## Exactness in the database

| # | Check | Result |
|---|---|---|
| 84 | a fund created with `quantity` `2000.123456`, `costPerUnit` `22400.654321`, `currentPricePerUnit` `24150.123456` | PostgreSQL returned `2000.123456`, `22400.654321` and `24150.123456`, and so did the API. Digit for digit in both directions, which a value that had been near a `double` would not be |
| — | column types, from `information_schema.columns` | `cost_per_unit numeric(19,6)`, `current_price_per_unit numeric(19,6)`, `interest_rate numeric(7,4)` and `symbol text` — exactly what `V5` declares and what the `@Column` precision and scale on `Asset` state |

Eleven assets carrying at least one instrument field were read back in bulk and every stored value
matched what had been sent, including `6.2500`, `8.5000`, `12.0000` and `100.0000`.

## The table refuses what the API refuses

Inserted straight into PostgreSQL, bypassing the API entirely. This is the half of each rule that
still holds during a migration, a manual correction, or the next slice.

| # | `INSERT` | Result |
|---|---|---|
| 74 | `CASH` with a `symbol` | refused — `assets_instrument_pricing_suits_the_category` |
| 75 | `LISTED_SECURITIES` with an `interest_rate` | refused — `assets_interest_rate_suits_the_category` |
| 76 | `INVESTMENT_FUND` with an `interest_rate` | refused — `assets_interest_rate_suits_the_category` |
| 77 | `TERM_DEPOSIT` with a `current_price_per_unit` | refused — `assets_instrument_pricing_suits_the_category` |
| 78 | `interest_rate` of `150.0000` | refused — `assets_interest_rate_is_a_percent` |
| 79 | `interest_rate` of `-1.0000` | refused — `assets_interest_rate_is_a_percent` |
| 80 | `symbol` of three spaces | refused — `assets_symbol_is_present_and_bounded` |
| 81 | `symbol` of 33 characters | refused — `assets_symbol_is_present_and_bounded` |
| 82 | `cost_per_unit` of `-1.000000` | refused — `assets_cost_per_unit_is_not_negative` |
| 83 | `BOND` with all four fields, all valid | `INSERT 0 1`. The constraints refuse what they should and nothing more |

Cases 74 to 77 are the ones that matter most: the API's refusal and the table's refusal are two
statements of one rule, and a divergence between `AssetCategory`'s predicates and these two
constraints would be invisible through HTTP alone.

## Findings

**A `symbol` sent as a JSON number is accepted and stored as a string.** Case 72 sent
`"symbol": 123` and got `201` with `"symbol": "123"`. Jackson coerces a scalar to a string by
default, and this is true of every string field in the API — `name`, `institution` and `notes` have
behaved this way since issue #39 — so it is not something this task introduced and not a rule the
instrument fields could fix on their own. It is recorded because a frontend reading the contract
would reasonably expect a type error, and because the fix, if the Product Owner wants one, is a
mapper-wide coercion setting rather than an annotation on these four fields.

**Two annotations can both fire on one value.** Case 50's `interestRate` of `1000.0000` returned two
`errors` entries naming the same field. That is bean validation reporting every constraint that
failed rather than the first, it is the behaviour every numeric field in the API already has, and a
client rendering the first message per field shows the right one.

## What this run did not cover

- The common fields, the five routes, the protocol cases and ownership on reads. Those are issue
  #53's, verified in
  `plans/reports/manual-verification-261103-2231-GH-53-asset-catalog-common-crud.md`, and a full
  regression run is both guides.
- Any calculation over these fields, because there is none to test: no accrued interest, no yield, no
  gain or loss, no derived current value, and no market-price lookup anywhere in MVP 1A.
- The valuation history and the value-update flow, which are issue #57's.
- The frontend's conditional form. Which fields the form offers is issue #55's; this run tested what
  the server accepts.

## Unresolved questions

- **Should a JSON number in a string field be a type error?** Finding one above. It is a
  mapper-wide decision affecting every existing route, so it is the Product Owner's rather than this
  task's.
- **Is `0` to `100` the right bound for an expected yield?** It refuses a negative yield, which real
  bonds can carry but a Vietnamese pilot user is unlikely to record. Widening it is a one-line change
  to the annotation and the check constraint if the Product Owner disagrees.
