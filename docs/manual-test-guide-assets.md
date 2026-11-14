# Manual test guide — the asset catalog

The asset catalog resource (`/api/v1/assets`, issue #53) ships with no automated test, by Product
Owner direction. This guide is the replacement: it is the whole of the verification, and rerunning it
is the only way a regression in this slice is caught. The recorded result of the run that accompanied
the implementation is
[`plans/reports/manual-verification-261103-2231-GH-53-asset-catalog-common-crud.md`](../plans/reports/manual-verification-261103-2231-GH-53-asset-catalog-common-crud.md).

Nothing here needs a Google account, a deployed environment or the mobile app.

## Set up the run

Everything below assumes a shell in the repository root and a Docker daemon.

### 1. A disposable database

Not the shared `rikkaus-postgres-data` volume, and not port 5432, so a run cannot touch whatever your
own development database holds.

```bash
docker run -d --name rikkaus-assets-check -e POSTGRES_USER=rikkaus -e POSTGRES_PASSWORD=rikkaus-local-only -e POSTGRES_DB=rikkaus -p 5433:5432 postgres:18.6-alpine3.24
```

The image tag is the one `docker-compose.yml` pins. Wait for it:

```bash
docker exec rikkaus-assets-check pg_isready -U rikkaus
```

### 2. The packaged application

The API needs JDK 25; the system default is often 21 and fails on `release 25`.

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@25 ./services/api/mvnw -f services/api/pom.xml package
```

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@25 java -jar services/api/target/rikkaus-wealth-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=local --server.port=8080 --spring.datasource.url=jdbc:postgresql://127.0.0.1:5433/rikkaus
```

Two things in the startup log are part of the verification and worth reading rather than scrolling
past. Flyway must report `Successfully applied 5 migrations ... now at version v6` — `V6` is the
valuation history, and `V5` belongs to issue #56's branch — and the context must then start:
`spring.jpa.hibernate.ddl-auto: validate` is what proves `V4__assets.sql` and the `Asset` entity agree
about every column, and a mismatch stops the application instead of corrupting a row later.

### 3. Two users

The catalog is owner-scoped, so the cross-user cases need two rows. There is no sign-up endpoint —
accounts are created by a Google sign-in — so insert them directly.

```bash
docker exec -i rikkaus-assets-check psql -U rikkaus -d rikkaus -c "INSERT INTO wealth.users (id, google_subject, email, display_name, created_at, updated_at) VALUES ('0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93','google-sub-a','an.nguyen@example.com','An Nguyen', now(), now()), ('7b41e590-3c82-4f16-b0d7-59e1c4f3a842','google-sub-b','bich.tran@example.com','Bich Tran', now(), now());"
```

### 4. Access tokens

Mint them with the `local` profile's committed development secret and the exact claim set
`AccessTokenService` issues. Minting rather than stubbing matters: the requests still travel through
the real `BearerTokenAuthenticationFilter` and the real `AccessTokenService.verify`, so the
authentication path under test is the production one.

```bash
python3 - <<'PY'
import base64, hashlib, hmac, json, time
secret = b"local-development-only-jwt-signing-secret"
def b64(raw): return base64.urlsafe_b64encode(raw).rstrip(b"=").decode()
def mint(sub, ttl=900, key=secret):
    now = int(time.time())
    h = b64(json.dumps({"alg": "HS256"}, separators=(",", ":")).encode())
    p = b64(json.dumps({"iss": "rikkaus-wealth-api", "sub": sub,
                        "sid": "11111111-2222-3333-4444-555555555555",
                        "iat": now, "exp": now + ttl}, separators=(",", ":")).encode())
    return f"{h}.{p}." + b64(hmac.new(key, f"{h}.{p}".encode(), hashlib.sha256).digest())
print("A        ", mint("0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93"))
print("B        ", mint("7b41e590-3c82-4f16-b0d7-59e1c4f3a842"))
print("expired  ", mint("0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93", ttl=-60))
print("forged   ", mint("0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93", key=b"a-different-secret-entirely"))
PY
```

Export them, and set today's date as the application judges it — a Vietnamese civil date, not UTC:

```bash
export A=<token A> B=<token B> EXPIRED=<expired> FORGED=<forged> TODAY=$(TZ=Asia/Ho_Chi_Minh date +%F) FUTURE=$(TZ=Asia/Ho_Chi_Minh date -v+1m +%F)
```

A short helper keeps every case below to one line:

```bash
api() { curl -s -i -H "Authorization: Bearer $1" -H 'Content-Type: application/json' -X "$2" "http://127.0.0.1:8080/api/v1$3" ${4:+-d "$4"}; }
```

## The cases

Read the status line and the body of each. Where a case expects `validation-failed`, the `errors`
array and the field it names are the assertion — not the 400 on its own, which several unrelated
mistakes also produce.

### Every category is accepted

Eleven creates, one per category. Substitute each constant in turn: `CASH`, `BANK_ACCOUNT`,
`TERM_DEPOSIT`, `LISTED_SECURITIES`, `BOND`, `INVESTMENT_FUND`, `REAL_ESTATE`,
`PRIVATE_BUSINESS_EQUITY`, `VEHICLE_OR_VALUABLE_ASSET`, `COLLECTIBLE`, `OTHER_ASSET`.

```bash
api "$A" POST /assets '{"name":"Tiền mặt dự phòng","category":"CASH","currentValue":"148000000.00","currency":"VND","valuationDate":"'$TODAY'","liquidity":"HIGH"}'
```

Each must answer `201` with a `Location` of `/api/v1/assets/<id>`. Keep one identifier; the later
cases need it. Expect `11` creates to succeed and nothing to be rejected for its category alone.

### The full optional field set, and the absent one

```bash
api "$A" POST /assets '{"name":"Tài khoản USD","category":"BANK_ACCOUNT","currentValue":"20000.00","currency":"USD","valuationDate":"2026-09-12","liquidity":"HIGH","purchaseValue":"19500.00","purchaseDate":"2024-03-18","quantity":"20000.000000","institution":"Ngân hàng An Tín","notes":"Tiền gửi dự phòng bằng USD."}'
```

`201`, and the response must echo `20000.00` with `"currency":"USD"` — **not** a VND figure. The
catalog never converts; the client multiplies by the rate from
`GET /api/v1/settings/exchange-rate`. A converted amount appearing here is a defect.

The minimal creates above are the optional-fields-absent case: every optional member must come back
`null` rather than `0` or `""`.

### Listing and order

```bash
api "$A" GET /assets
api "$B" GET /assets
```

User A's list must be newest `valuationDate` first and then by name; user B's must be `[]` with a
`200`, never a `404`. Run the first one twice and compare — the order must be identical, because an
unordered result is not stable between requests.

### Each required field missing, one case each

Remove one member at a time from the minimal body: `name`, `category`, `currentValue`, `currency`,
`valuationDate`, `liquidity`. Each must answer `400` `validation-failed` with an `errors` entry
naming **that** field and no other.

```bash
api "$A" POST /assets '{"category":"CASH","currentValue":"1000000.00","currency":"VND","valuationDate":"'$TODAY'","liquidity":"HIGH"}'
```

A name of `"   "` must also be refused, as `name` / `must not be blank`.

### Values and lengths

| Send | Expect |
|---|---|
| `"currentValue":"-1.00"` | `currentValue` / `must not be negative` |
| `"currentValue":"100.123"` | `currentValue` / `must have at most 17 integer digits and 2 decimals` |
| `"currentValue":"0.00"` | `201` — zero is a real state, an emptied holding |
| `"quantity":"0"` | `quantity` / `must be greater than zero` |
| `"quantity":"-5"` | `quantity` / `must be greater than zero` |
| `"purchaseValue":"-1.00"` | `purchaseValue` / `must not be negative` |
| a 121-character `name` | `name` / `must be at most 120 characters` |
| a 161-character `institution` | `institution` / `must be at most 160 characters` |
| a 2001-character `notes` | `notes` / `must be at most 2000 characters` |

The over-precise value matters most: it has to be refused here, with the field named, rather than
reaching `NUMERIC(19, 2)` and failing as a 500.

### An unrecognised constant

```bash
api "$A" POST /assets '{"name":"Lạ","category":"CRYPTOCURRENCY","currentValue":"1.00","currency":"VND","valuationDate":"'$TODAY'","liquidity":"HIGH"}'
```

`400` `malformed-request`, not `validation-failed`, and with no `errors` array — Jackson cannot
construct the record at all, so the failure happens before bean validation runs. Repeat for an unknown
`currency` and an unknown `liquidity`. This is documented behaviour, not a defect; the real client
chooses from a picker.

### Impossible dates

Use `$FUTURE` rather than a literal date. A hardcoded one stops being in the future, and then these
two rows answer `201` and the operator reads a pass as a failure.

| Send | Expect |
|---|---|
| `valuationDate` of `$FUTURE` | `valuationDate` / `must not be in the future` |
| `purchaseDate` of `$FUTURE` | `purchaseDate` / `must not be in the future` |
| `valuationDate` `2026-06-01` with `purchaseDate` `2026-09-01` | `purchaseDate` / `must not be later than the valuation date` |
| both dates `$FUTURE` | **two** `errors` entries, one per field, and no third about their order |

Date handling is judged in `Asia/Ho_Chi_Minh`. To see that this is real rather than incidental, run
the first case again with today's date late in a Vietnamese evening: it must be accepted, even though
UTC still calls it tomorrow.

### Ownership

```bash
api "$A" GET /assets/<A's asset>     # 200
api "$B" GET /assets/<A's asset>     # 404
api "$B" GET /assets/00000000-0000-4000-8000-000000000000   # 404
api "$B" PUT /assets/<A's asset> '{"name":"Chiếm dụng","liquidity":"LOW"}'   # 404
api "$B" DELETE /assets/<A's asset>  # 404
```

Compare the second and third bodies. They must match on `type`, `title`, `status` and `detail`, and
differ only in `instance` — the request URI, which RFC 9457 defines that way — and in
`correlationId`. Any other difference between "someone else's" and "does not exist" is an enumeration
oracle, which is the whole reason this answers `404` rather than `403`.

Afterwards, read user A's asset again as user A: the refused write must have changed nothing.

### The metadata edit

```bash
api "$A" PUT /assets/<id> '{"name":"Tài khoản USD dự phòng","liquidity":"MEDIUM","purchaseValue":"19800.00","purchaseDate":"2024-04-01","quantity":"20500.000000","institution":"Ngân hàng An Tín","notes":"Đã chuyển sang kỳ hạn 6 tháng."}'
```

`200`, with the seven members changed and `updatedAt` advanced while `createdAt` stands still.

Now the case that matters most in this slice:

```bash
api "$A" PUT /assets/<id> '{"name":"Tài khoản USD dự phòng","liquidity":"MEDIUM","currentValue":"99999999.00","currency":"VND","valuationDate":"'$TODAY'","category":"CASH"}'
```

`200`, and `currentValue`, `currency`, `valuationDate` and `category` must be **unchanged**. They are
not members of the request record, so Jackson drops them — Spring Boot's own default for an unknown
property. A value that moved here means the valuation has become overwritable through the edit route,
and the only legitimate way to change it — `POST /api/v1/assets/{assetId}/valuations`, covered by
[`manual-test-guide-asset-valuations.md`](manual-test-guide-asset-valuations.md) — has been bypassed.

The same response also demonstrates the replacement semantics: the four optional members left out of
that body are now `null`. Read it back with a `GET` to confirm it was stored that way and not merely
rendered so.

Then the remaining edit cases:

| Send | Expect |
|---|---|
| `"institution":"   "`, `"notes":"  Ghi chú.  "` | `institution` stored as `null`; `notes` trimmed |
| no `name` | `name` / `must not be blank` |
| `"name":"  "` | `name` / `must not be blank` |
| a `purchaseDate` after the asset's stored `valuationDate` | `purchaseDate` / `must not be later than the valuation date` |

The last one is checked against the **stored** valuation date, which the edit cannot change.

### Characters that only fail further down

These three exist because review found them and a run confirmed them. Each is legal JSON that
satisfies every annotation and every check constraint, so nothing upstream of these cases catches them.

```bash
api "$A" POST /assets '{"name":"Tiền\u0000mặt","category":"CASH","currentValue":"1000000.00","currency":"VND","valuationDate":"'$TODAY'","liquidity":"HIGH"}'
```

| Send | Expect |
|---|---|
| a `\u0000` anywhere in `name`, `institution` or `notes`, on `POST` or on `PUT` | `400`, that field / `must not contain a null character` |
| a `name` of a single ` ` (non-breaking space) | `400`, `name` / `must not be blank` |
| a `name` of a single `　` (ideographic space) | `400`, `name` / `must not be blank` |

A `201` on the first row means PostgreSQL is about to be handed a byte it cannot store in a `text`
column, and the next thing in the log is a 500 with a stack trace. A `201` on the other two means an
asset was created whose name renders as an empty row in the catalog. Put the NUL in the **middle** of
the value: a trailing one was removed by `trim()` in an earlier revision, which is exactly why an
earlier probe of this wrongly came back clean.

An unpaired surrogate — `"notes":"xin ch\ud800o"` — is **expected to be accepted** with a `201`.
PostgreSQL stores and returns it without complaint, so there is nothing to refuse.

### Two requests at once

The one concurrency case that is worth testing, because it is what a double-tapped confirm button
produces and because it used to be a 500.

```bash
ID=<an asset the viewer owns>
for i in 1 2 3 4; do api "$A" DELETE "/assets/$ID" -o /dev/null -w "%{http_code}\n" & done; wait
```

Expect exactly one `204` and three `404`. **Any `500` is a regression** — the race returned
`internal-error` for 73 of 100 requests before `AssetService.flushTreatingAVanishedRowAsNotFound`
existed. Repeat it a dozen times, not once: it is a race, so a single clean round proves little.

The same applies to a `PUT` racing a `DELETE`. Run two of each against one identifier and expect
`204` once and `404` for the rest, never a 500.

### Credentials

```bash
curl -s -i http://127.0.0.1:8080/api/v1/assets                            # 401
api "$EXPIRED" GET /assets                                                # 401
api "$FORGED" GET /assets                                                 # 401
```

All three must be `401` `unauthorized`, and the three bodies must be indistinguishable apart from the
correlation id. A forged signature that produced a different answer from a missing header would tell
an attacker which half of the token to work on.

### Protocol

| Send | Expect |
|---|---|
| `GET /assets/not-a-uuid` | `400` `malformed-request` |
| `PATCH /assets/<id>` | `405` `method-not-allowed` |
| `DELETE /assets` | `405` `method-not-allowed` |
| a `text/plain` body | `415` `unsupported-media-type` |
| `{"name": ` — truncated JSON | `400` `malformed-request` |
| `Accept: application/xml` | `406` `not-acceptable` |

Every one of these must be `application/problem+json`. Boot's default error shape appearing anywhere
is a defect.

### Removal

```bash
api "$A" DELETE /assets/<id>   # 204, no body
api "$A" GET /assets/<id>      # 404
api "$A" DELETE /assets/<id>   # 404
api "$A" GET /assets           # 200, one fewer row
```

The second delete answering `404` rather than `204` is deliberate: a deleted identifier is treated
exactly like one that never existed.

### Exactness in the database

The API's JSON is not the last word; read the stored value.

```bash
docker exec -i rikkaus-assets-check psql -U rikkaus -d rikkaus -x -c "SELECT name, current_value, currency, quantity FROM wealth.assets ORDER BY current_value DESC LIMIT 3;"
```

Large VND figures must come back whole and unrounded — `6100000000.00`, not `6.1e+09` and not
`6100000000.0000001`. The column types are the other half:

```bash
docker exec -i rikkaus-assets-check psql -U rikkaus -d rikkaus -c "SELECT column_name, data_type, numeric_precision, numeric_scale FROM information_schema.columns WHERE table_schema='wealth' AND table_name='assets' ORDER BY ordinal_position;"
```

`current_value` and `purchase_value` must be `numeric(19,2)` and `quantity` `numeric(19,6)`. A
`double precision` anywhere in that output is a defect that no API-level case would reveal.

### The table refuses what the API refuses

The checks exist so a caller that is not the API cannot write a row the API would reject. Prove it by
bypassing the application entirely — each of these must raise a named check violation and insert
nothing.

```bash
docker exec -i rikkaus-assets-check psql -U rikkaus -d rikkaus -c "INSERT INTO wealth.assets (id,user_id,name,category,current_value,currency,valuation_date,liquidity,created_at,updated_at) VALUES (gen_random_uuid(),'0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93','Probe','CRYPTOCURRENCY',1,'VND','$TODAY','HIGH',now(),now());"
```

Repeat with a negative `current_value`, a `currency` of `EUR`, a `quantity` of `0`, a `purchase_date`
later than the `valuation_date`, and a `name` of `'   '`. Expect
`assets_category_is_known`, `assets_current_value_is_not_negative`, `assets_currency_is_supported`,
`assets_quantity_is_positive`, `assets_purchase_date_precedes_valuation` and
`assets_name_is_present_and_bounded` in turn, and `SELECT count(*)` unchanged afterwards.

## Tear down

Leaving these running holds port 8080 and port 5433 and a container, which the next run then has to
work around.

```bash
pkill -f rikkaus-wealth-api-0.0.1-SNAPSHOT.jar
```

```bash
docker rm -f rikkaus-assets-check
```

## What this guide does not cover

- A real Google sign-in. There is no OAuth client configured locally, so the tokens are minted. The
  filter and the verifier under test are the production ones; the Google handoff is not exercised.
- Concurrency beyond the delete and edit-versus-delete races above. Two simultaneous *edits* of one
  asset are not tested, and the last writer wins with no conflict reported — there is no row version,
  which is a product decision about edit conflicts rather than an oversight.
- The mobile client. This is the API's behaviour only; the frontend task is issue #52.
- Anything the `settings` or `identity` routes own, or the valuation history. The history has its own
  guide, `manual-test-guide-asset-valuations.md`.
