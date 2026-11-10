# Manual test guide — type-specific asset details

The four instrument fields on `/api/v1/assets` (issue #56) ship with no automated test, by Product
Owner direction, which is the posture issues #50 and #53 shipped under. This guide is the whole of
the verification: rerunning it is the only way a regression in these fields is caught. The recorded
result of the run that accompanied the implementation is
[`plans/reports/manual-verification-261110-0844-GH-56-type-specific-asset-details.md`](../plans/reports/manual-verification-261110-0844-GH-56-type-specific-asset-details.md).

It covers `symbol`, `costPerUnit`, `currentPricePerUnit` and `interestRate`. It does not re-cover the
common fields, the five routes, ownership on reads, or the protocol cases:
[`docs/manual-test-guide-assets.md`](manual-test-guide-assets.md) owns those, and a full regression
run is both guides.

Nothing here needs a Google account, a deployed environment or the mobile app.

## Set up the run

Follow sections 1 to 4 of [`docs/manual-test-guide-assets.md`](manual-test-guide-assets.md) — the
disposable database on port 5433, the packaged jar on JDK 25, the two users inserted directly, the
minted access tokens, and the `api` helper. Everything below uses that helper and those variables.

Two differences from that guide, both part of this verification:

- Flyway must now report **five** migrations: `Successfully applied 5 migrations ... now at version
  v5`. Run against a database that already has `v4`, it applies `V5` alone and reports one.
- The context must then start. `spring.jpa.hibernate.ddl-auto: validate` is what proves
  `V5__asset_instrument_details.sql` and the four new fields on the `Asset` entity agree about every
  column name, type and scale. A mismatch stops the application rather than corrupting a row later.

Use a container name of its own so a run cannot collide with the other guide's:

```bash
docker run -d --name rikkaus-instrument-check -e POSTGRES_USER=rikkaus -e POSTGRES_PASSWORD=rikkaus-local-only -e POSTGRES_DB=rikkaus -p 5433:5432 postgres:18.6-alpine3.24
```

## The contract under test

The accepted design's visibility matrix, enforced server-side. Every cell below is a case.

| Category | `symbol` | `costPerUnit` | `currentPricePerUnit` | `interestRate` |
|---|---|---|---|---|
| `LISTED_SECURITIES` | accepted | accepted | accepted | refused |
| `BOND` | accepted | accepted | accepted | accepted |
| `INVESTMENT_FUND` | accepted | accepted | accepted | refused |
| `TERM_DEPOSIT` | refused | refused | refused | accepted |
| `OTHER_ASSET` | accepted | accepted | accepted | accepted |
| `CASH`, `BANK_ACCOUNT`, `REAL_ESTATE`, `PRIVATE_BUSINESS_EQUITY`, `VEHICLE_OR_VALUABLE_ASSET`, `COLLECTIBLE` | refused | refused | refused | refused |

A refusal is `400` with `type` `urn:rikkaus:problem:validation-failed` and an `errors` entry
`{"field": "<the field>", "message": "must not be set for this asset category"}`. The field name in
that array is the assertion, not the 400 on its own.

## The cases

### 1. Every accepted cell

```bash
api "$A" POST /assets '{"name":"Cổ phiếu VCB","category":"LISTED_SECURITIES","currentValue":12000000.00,"currency":"VND","valuationDate":"'"$TODAY"'","liquidity":"HIGH","quantity":500,"symbol":"VCB","costPerUnit":21800.000000,"currentPricePerUnit":24150.000000}'
api "$A" POST /assets '{"name":"Trái phiếu An Tín 2029","category":"BOND","currentValue":200000000.00,"currency":"VND","valuationDate":"'"$TODAY"'","liquidity":"LOW","quantity":200,"symbol":"ATB2029","costPerUnit":1000000.000000,"currentPricePerUnit":1012500.000000,"interestRate":8.5000}'
api "$A" POST /assets '{"name":"Quỹ VFMVF1","category":"INVESTMENT_FUND","currentValue":48300246.00,"currency":"VND","valuationDate":"'"$TODAY"'","liquidity":"MEDIUM","quantity":2000,"symbol":"VFMVF1","costPerUnit":22400.500000,"currentPricePerUnit":24150.123456}'
api "$A" POST /assets '{"name":"Tiền gửi 12 tháng An Tín","category":"TERM_DEPOSIT","currentValue":300000000.00,"currency":"VND","valuationDate":"'"$TODAY"'","liquidity":"LOW","interestRate":6.2500,"institution":"Ngân hàng An Tín"}'
api "$A" POST /assets '{"name":"Hợp đồng góp vốn khác","category":"OTHER_ASSET","currentValue":75000000.00,"currency":"VND","valuationDate":"'"$TODAY"'","liquidity":"LOW","symbol":"KHAC-01","costPerUnit":500000.000000,"currentPricePerUnit":750000.000000,"interestRate":12.0000}'
```

All five are `201`. Each response must echo exactly the instrument values sent, and the ones not
sent must be `null` rather than absent.

### 2. Every refused cell

One request per refused field per refusing category. For each of `CASH`, `BANK_ACCOUNT`,
`REAL_ESTATE`, `PRIVATE_BUSINESS_EQUITY`, `VEHICLE_OR_VALUABLE_ASSET` and `COLLECTIBLE`, send each of
the four fields alone:

```bash
for c in CASH BANK_ACCOUNT REAL_ESTATE PRIVATE_BUSINESS_EQUITY VEHICLE_OR_VALUABLE_ASSET COLLECTIBLE; do
  for f in '"symbol":"VCB"' '"costPerUnit":1000.000000' '"currentPricePerUnit":1000.000000' '"interestRate":6.2500'; do
    api "$A" POST /assets '{"name":"Trường hợp từ chối","category":"'"$c"'","currentValue":1000000.00,"currency":"VND","valuationDate":"'"$TODAY"'","liquidity":"LOW",'"$f"'}' | tail -1
  done
done
```

24 cases, every one `400`, each naming the field it was sent.

Then the three narrower refusals:

```bash
api "$A" POST /assets '{"name":"Lãi suất trên chứng khoán","category":"LISTED_SECURITIES","currentValue":1000000.00,"currency":"VND","valuationDate":"'"$TODAY"'","liquidity":"HIGH","interestRate":6.2500}'
api "$A" POST /assets '{"name":"Lãi suất trên quỹ","category":"INVESTMENT_FUND","currentValue":1000000.00,"currency":"VND","valuationDate":"'"$TODAY"'","liquidity":"MEDIUM","interestRate":6.2500}'
api "$A" POST /assets '{"name":"Mã trên tiền gửi","category":"TERM_DEPOSIT","currentValue":1000000.00,"currency":"VND","valuationDate":"'"$TODAY"'","liquidity":"LOW","symbol":"ATB"}'
```

Each is `400` naming `interestRate`, `interestRate` and `symbol` respectively. These three are the
cells most likely to be implemented as "whatever the pricing group allows", so they are the ones that
prove the matrix was read cell by cell rather than grouped.

### 3. All four fields reported at once

```bash
api "$A" POST /assets '{"name":"Tiền mặt sai mọi trường","category":"CASH","currentValue":1000000.00,"currency":"VND","valuationDate":"'"$TODAY"'","liquidity":"HIGH","symbol":"VCB","costPerUnit":1000.000000,"currentPricePerUnit":1000.000000,"interestRate":6.2500}'
```

One `400` with **four** `errors` entries, in contract order — `symbol`, `costPerUnit`,
`currentPricePerUnit`, `interestRate` — rather than one error per round trip.

### 4. Omitting all four, for all eleven categories

The acceptance criterion that missing optional data stays valid.

```bash
for c in CASH BANK_ACCOUNT TERM_DEPOSIT LISTED_SECURITIES BOND INVESTMENT_FUND REAL_ESTATE PRIVATE_BUSINESS_EQUITY VEHICLE_OR_VALUABLE_ASSET COLLECTIBLE OTHER_ASSET; do
  api "$A" POST /assets '{"name":"Không có chi tiết '"$c"'","category":"'"$c"'","currentValue":1000000.00,"currency":"VND","valuationDate":"'"$TODAY"'","liquidity":"LOW"}' | tail -1
done
```

Eleven `201`s, each with all four instrument fields `null`.

### 5. Bounds

```bash
api "$A" POST /assets '{"name":"Lãi suất âm","category":"TERM_DEPOSIT","currentValue":1000000.00,"currency":"VND","valuationDate":"'"$TODAY"'","liquidity":"LOW","interestRate":-0.0100}'
api "$A" POST /assets '{"name":"Lãi suất quá lớn","category":"TERM_DEPOSIT","currentValue":1000000.00,"currency":"VND","valuationDate":"'"$TODAY"'","liquidity":"LOW","interestRate":100.0100}'
api "$A" POST /assets '{"name":"Lãi suất đúng biên","category":"TERM_DEPOSIT","currentValue":1000000.00,"currency":"VND","valuationDate":"'"$TODAY"'","liquidity":"LOW","interestRate":100.0000}'
api "$A" POST /assets '{"name":"Lãi suất quá nhiều chữ số","category":"TERM_DEPOSIT","currentValue":1000000.00,"currency":"VND","valuationDate":"'"$TODAY"'","liquidity":"LOW","interestRate":6.25001}'
api "$A" POST /assets '{"name":"Giá vốn âm","category":"LISTED_SECURITIES","currentValue":1000000.00,"currency":"VND","valuationDate":"'"$TODAY"'","liquidity":"HIGH","costPerUnit":-1.000000}'
api "$A" POST /assets '{"name":"Giá bằng không","category":"LISTED_SECURITIES","currentValue":0.00,"currency":"VND","valuationDate":"'"$TODAY"'","liquidity":"HIGH","currentPricePerUnit":0.000000}'
api "$A" POST /assets '{"name":"Giá quá nhiều chữ số thập phân","category":"LISTED_SECURITIES","currentValue":1000000.00,"currency":"VND","valuationDate":"'"$TODAY"'","liquidity":"HIGH","currentPricePerUnit":1.0000001}'
api "$A" POST /assets '{"name":"Lãi suất bốn chữ số nguyên","category":"TERM_DEPOSIT","currentValue":1000000.00,"currency":"VND","valuationDate":"'"$TODAY"'","liquidity":"LOW","interestRate":1000.0000}'
LONG=$(python3 -c 'print("V"*33)')
api "$A" POST /assets '{"name":"Mã quá dài","category":"LISTED_SECURITIES","currentValue":1000000.00,"currency":"VND","valuationDate":"'"$TODAY"'","liquidity":"HIGH","symbol":"'"$LONG"'"}'
api "$A" POST /assets '{"name":"Mã đúng biên","category":"LISTED_SECURITIES","currentValue":1000000.00,"currency":"VND","valuationDate":"'"$TODAY"'","liquidity":"HIGH","symbol":"'"${LONG:0:32}"'"}'
```

In order: `400` `must not be negative`; `400` `must be at most 100`; `201`, because the bound is
inclusive; `400` `must have at most 3 integer digits and 4 decimals`; `400` `must not be negative`;
`201`, because a unit currently worth nothing is a real state; `400`
`must have at most 13 integer digits and 6 decimals`; `400` with **two** entries on `interestRate`,
the bound and the digits message, because two annotations each have something true to say about
`1000.0000`; `400` `must be at most 32 characters`; `201` for a symbol of exactly 32.

### 6. A symbol that is not a symbol

```bash
api "$A" POST /assets '{"name":"Mã toàn khoảng trắng","category":"LISTED_SECURITIES","currentValue":1000000.00,"currency":"VND","valuationDate":"'"$TODAY"'","liquidity":"HIGH","symbol":"   "}'
api "$A" POST /assets '{"name":"Mã khoảng trắng trên loại từ chối","category":"CASH","currentValue":1000000.00,"currency":"VND","valuationDate":"'"$TODAY"'","liquidity":"HIGH","symbol":"   "}'
api "$A" POST /assets '{"name":"Mã có ký tự null","category":"LISTED_SECURITIES","currentValue":1000000.00,"currency":"VND","valuationDate":"'"$TODAY"'","liquidity":"HIGH","symbol":"VC\u0000B"}'
api "$A" POST /assets '{"name":"Mã có khoảng trắng hai đầu","category":"LISTED_SECURITIES","currentValue":1000000.00,"currency":"VND","valuationDate":"'"$TODAY"'","liquidity":"HIGH","symbol":"  VCB  "}'
api "$A" POST /assets '{"name":"Mã là khoảng trắng không ngắt","category":"LISTED_SECURITIES","currentValue":1000000.00,"currency":"VND","valuationDate":"'"$TODAY"'","liquidity":"HIGH","symbol":"\u00a0"}'
```

`201` with `"symbol": null`, because a field of only blank space is one the user left alone; `201`
with `"symbol": null` **and no refusal**, which is the ordering that matters — the symbol is cleaned
before the category rule is applied, so a user who typed nothing is not told their category refuses
a field; `400` `must not contain a null character`, refused here rather than reaching PostgreSQL as a
500; `201` with `"symbol": "VCB"`; and `201` with `"symbol": null` for the single non-breaking space,
which is the case the wider definition of blank exists for — `U+00A0` passes `@NotBlank`, survives
`strip()` and satisfies `btrim`, so a narrower helper would store a symbol that renders as an empty
field.

### 7. Nothing is derived

```bash
api "$A" POST /assets '{"name":"Giá trị không phải tích số","category":"LISTED_SECURITIES","currentValue":12000000.00,"currency":"VND","valuationDate":"'"$TODAY"'","liquidity":"HIGH","quantity":500,"currentPricePerUnit":24150.000000}'
```

`201`, storing `12000000.00` and not `12075000.00`. The product of the quantity and the unit price is
`12,075,000` and the asset is worth what the user said it is worth. This is the case that proves the
server does no arithmetic, and it is the one most likely to regress if somebody later adds a
"helpful" calculation.

### 7b. An instrument problem and a date problem together

```bash
api "$A" POST /assets '{"name":"Hai lỗi cùng lúc","category":"CASH","currentValue":1000000.00,"currency":"VND","valuationDate":"'"$FUTURE"'","liquidity":"HIGH","symbol":"VCB"}'
```

One `400` with two entries: `valuationDate` `must not be in the future` and `symbol` `must not be set
for this asset category`. The two rules are collected and thrown once, so a request wrong in two
unrelated ways is one round trip.

### 8. Exactness in the database

```bash
docker exec -i rikkaus-instrument-check psql -U rikkaus -d rikkaus -c "SELECT name, symbol, cost_per_unit, current_price_per_unit, interest_rate FROM wealth.assets WHERE symbol IS NOT NULL OR interest_rate IS NOT NULL ORDER BY name;"
```

`24150.123456` and `6.2500` must come back digit for digit. A value that has been near a `double`
does not.

### 9. The edit route

Create a fund holding, keep its id, then:

```bash
api "$A" PUT "/assets/$ID" '{"name":"Quỹ VFMVF1","liquidity":"MEDIUM","quantity":2000,"symbol":"VFMVF1","costPerUnit":22400.500000,"currentPricePerUnit":25000.000000}'
api "$A" PUT "/assets/$ID" '{"name":"Quỹ VFMVF1","liquidity":"MEDIUM"}'
api "$A" PUT "/assets/$ID" '{"name":"Quỹ VFMVF1","liquidity":"MEDIUM","interestRate":6.2500}'
```

`200` with the new price; `200` with all four instrument fields `null`, because this is a replacement
and an omitted field is cleared; `400` naming `interestRate`, judged against the asset's **stored**
category, which the body does not and cannot carry.

### 10. Ownership on a write carrying instrument fields

```bash
api "$B" PUT "/assets/$ID" '{"name":"Chiếm quyền","liquidity":"HIGH","symbol":"VCB"}'
```

`404`, with a body identical to a genuinely missing identifier except for `instance` and
`correlationId`. Ownership is resolved before the instrument fields are judged, so a non-owner cannot
learn a stored asset's category from which refusal they get.

### 10b. Credentials and malformed bodies

```bash
curl -s -o /dev/null -w '%{http_code}\n' -H 'Content-Type: application/json' -X POST http://127.0.0.1:8080/api/v1/assets -d '{"name":"X","category":"BOND","currentValue":1,"currency":"VND","valuationDate":"'"$TODAY"'","liquidity":"LOW","symbol":"X"}'
api "$A" POST /assets '{"name":"Mã là số","category":"LISTED_SECURITIES","currentValue":1000000.00,"currency":"VND","valuationDate":"'"$TODAY"'","liquidity":"HIGH","symbol":123}'
api "$A" POST /assets '{"name":"Lãi suất là chuỗi","category":"TERM_DEPOSIT","currentValue":1000000.00,"currency":"VND","valuationDate":"'"$TODAY"'","liquidity":"LOW","interestRate":"sáu phẩy hai lăm"}'
```

`401`, before any validation runs; `201` storing the string `123`, because Jackson coerces a scalar
to a string by default for every string field in this API and not only these — recorded as a finding
in the evidence report rather than treated as correct by design; and `400` `malformed-request` with no
`errors` array, because Jackson cannot build the record at all and the failure precedes bean
validation.

### 11. The table refuses what the API refuses

The constraints are the half that still holds for a caller that is not the API.

```bash
docker exec -i rikkaus-instrument-check psql -U rikkaus -d rikkaus -c "INSERT INTO wealth.assets (id, user_id, name, category, current_value, currency, valuation_date, liquidity, symbol, created_at, updated_at) VALUES (gen_random_uuid(), '0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93', 'Tiền mặt có mã', 'CASH', 1000000.00, 'VND', CURRENT_DATE, 'HIGH', 'VCB', now(), now());"
docker exec -i rikkaus-instrument-check psql -U rikkaus -d rikkaus -c "INSERT INTO wealth.assets (id, user_id, name, category, current_value, currency, valuation_date, liquidity, interest_rate, created_at, updated_at) VALUES (gen_random_uuid(), '0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93', 'Chứng khoán có lãi suất', 'LISTED_SECURITIES', 1000000.00, 'VND', CURRENT_DATE, 'HIGH', 6.2500, now(), now());"
docker exec -i rikkaus-instrument-check psql -U rikkaus -d rikkaus -c "INSERT INTO wealth.assets (id, user_id, name, category, current_value, currency, valuation_date, liquidity, interest_rate, created_at, updated_at) VALUES (gen_random_uuid(), '0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93', 'Lãi suất 150', 'TERM_DEPOSIT', 1000000.00, 'VND', CURRENT_DATE, 'LOW', 150.0000, now(), now());"
docker exec -i rikkaus-instrument-check psql -U rikkaus -d rikkaus -c "INSERT INTO wealth.assets (id, user_id, name, category, current_value, currency, valuation_date, liquidity, symbol, created_at, updated_at) VALUES (gen_random_uuid(), '0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93', 'Mã rỗng', 'BOND', 1000000.00, 'VND', CURRENT_DATE, 'LOW', '   ', now(), now());"
```

Four failures, naming `assets_instrument_pricing_suits_the_category`,
`assets_interest_rate_suits_the_category`, `assets_interest_rate_is_a_percent` and
`assets_symbol_is_present_and_bounded`.

Then the rest of the constraint set, including one row that must be **accepted** — a constraint that
refuses everything proves nothing:

```bash
U=0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93
ins() { docker exec -i rikkaus-instrument-check psql -U rikkaus -d rikkaus -c "$1"; }
ins "INSERT INTO wealth.assets (id,user_id,name,category,current_value,currency,valuation_date,liquidity,interest_rate,created_at,updated_at) VALUES (gen_random_uuid(),'$U','Quỹ có lãi suất','INVESTMENT_FUND',1000000.00,'VND',CURRENT_DATE,'MEDIUM',6.2500,now(),now());"
ins "INSERT INTO wealth.assets (id,user_id,name,category,current_value,currency,valuation_date,liquidity,current_price_per_unit,created_at,updated_at) VALUES (gen_random_uuid(),'$U','Tiền gửi có đơn giá','TERM_DEPOSIT',1000000.00,'VND',CURRENT_DATE,'LOW',1000.000000,now(),now());"
ins "INSERT INTO wealth.assets (id,user_id,name,category,current_value,currency,valuation_date,liquidity,interest_rate,created_at,updated_at) VALUES (gen_random_uuid(),'$U','Lãi suất âm','TERM_DEPOSIT',1000000.00,'VND',CURRENT_DATE,'LOW',-1.0000,now(),now());"
ins "INSERT INTO wealth.assets (id,user_id,name,category,current_value,currency,valuation_date,liquidity,cost_per_unit,created_at,updated_at) VALUES (gen_random_uuid(),'$U','Giá vốn âm','BOND',1000000.00,'VND',CURRENT_DATE,'LOW',-1.000000,now(),now());"
ins "INSERT INTO wealth.assets (id,user_id,name,category,current_value,currency,valuation_date,liquidity,symbol,cost_per_unit,current_price_per_unit,interest_rate,created_at,updated_at) VALUES (gen_random_uuid(),'$U','Trái phiếu hợp lệ','BOND',1000000.00,'VND',CURRENT_DATE,'LOW','OK',1.000000,2.000000,8.0000,now(),now());"
```

`assets_interest_rate_suits_the_category`, `assets_instrument_pricing_suits_the_category`,
`assets_interest_rate_is_a_percent`, `assets_cost_per_unit_is_not_negative`, and then `INSERT 0 1`.

The four category cases are the ones that matter most: the API's refusal and the table's refusal are
two statements of one rule, and a divergence between `AssetCategory`'s predicates and these two
constraints would be invisible through HTTP alone.

## Tear down

```bash
docker rm -f rikkaus-instrument-check
```

The shared `rikkaus-postgres-data` volume was never touched, because nothing above used port 5432.

## What this guide does not cover

- The common fields, the five routes, the protocol cases and ownership on reads. Those are
  [`docs/manual-test-guide-assets.md`](manual-test-guide-assets.md), and a full regression run is
  both.
- Any calculation on these fields, because there is none: no accrued interest, no yield, no gain or
  loss, no derived current value, and no market-price lookup anywhere in MVP 1A.
- The valuation history and the value-update flow, which are issue #57's.
- The frontend's conditional form. Which fields the form shows is issue #55's, and this guide tests
  what the server accepts rather than what the form offers.
