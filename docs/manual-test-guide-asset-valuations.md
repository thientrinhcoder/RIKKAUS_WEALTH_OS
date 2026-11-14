# Manual test guide — the asset valuation history

The valuation history (`/api/v1/assets/{assetId}/valuations`, issue #59) ships with no automated test,
by Product Owner direction, as the asset catalog did. This guide is the whole of the verification, and
rerunning it is the only way a regression in this slice is caught. The recorded result of the run that
accompanied the implementation is
[`plans/reports/manual-verification-261114-1716-GH-59-asset-valuation-history.md`](../plans/reports/manual-verification-261114-1716-GH-59-asset-valuation-history.md).

The setup is the one [`manual-test-guide-assets.md`](manual-test-guide-assets.md) describes, with a
container of its own so the two runs cannot collide. It is repeated here in full so this guide can be
run on its own.

## Set up the run

Everything below assumes a shell in the repository root and a Docker daemon.

### 1. A disposable database

```bash
docker run -d --name rikkaus-valuations-check -e POSTGRES_USER=rikkaus -e POSTGRES_PASSWORD=rikkaus-local-only -e POSTGRES_DB=rikkaus -p 5433:5432 postgres:18.6-alpine3.24
```

```bash
docker exec rikkaus-valuations-check pg_isready -U rikkaus
```

### 2. The packaged application

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@25 ./services/api/mvnw -f services/api/pom.xml package
```

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@25 java -jar services/api/target/rikkaus-wealth-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=local --server.port=8080 --spring.datasource.url=jdbc:postgresql://127.0.0.1:5433/rikkaus
```

Two lines of the startup log are part of the verification. Flyway must report
`Successfully applied 5 migrations ... now at version v6` — five, not six, because `V5` belongs to
issue #56's branch and is absent here — and the context must then start, which is `ddl-auto: validate`
accepting that `V6__asset_valuations.sql` and the `AssetValuation` entity agree on every column.

### 3. Two users

```bash
docker exec -i rikkaus-valuations-check psql -U rikkaus -d rikkaus -c "INSERT INTO wealth.users (id, google_subject, email, display_name, created_at, updated_at) VALUES ('0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93','google-sub-a','an.nguyen@example.com','An Nguyen', now(), now()), ('7b41e590-3c82-4f16-b0d7-59e1c4f3a842','google-sub-b','bich.tran@example.com','Bich Tran', now(), now());"
```

### 4. Access tokens

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

```bash
export A=<token A> B=<token B> EXPIRED=<expired> FORGED=<forged> TODAY=$(TZ=Asia/Ho_Chi_Minh date +%F) FUTURE=$(TZ=Asia/Ho_Chi_Minh date -v+1m +%F)
```

```bash
api() { curl -s -i -H "Authorization: Bearer $1" -H 'Content-Type: application/json' -X "$2" "http://127.0.0.1:8080/api/v1$3" ${4:+-d "$4"}; }
```

## The cases

Where a case expects `validation-failed`, the `errors` array and the field it names are the assertion,
not the 400 on its own. Where a case expects a change to the asset, read the asset back with `GET
/assets/<id>` rather than trusting the valuation response.

### The first record is written when the asset is

```bash
api "$A" POST /assets '{"name":"Tài khoản USD","category":"BANK_ACCOUNT","currentValue":"20000.00","currency":"USD","valuationDate":"2026-09-12","usdToVndRate":"25100.000000","rateEffectiveDate":"2026-09-12","liquidity":"HIGH","purchaseValue":"19500.00","purchaseDate":"2024-03-18","institution":"Ngân hàng An Tín"}'
```

`201`. Keep the identifier as `USD`. Then:

```bash
api "$A" GET /assets/$USD/valuations
```

`200` with **one** record: `value: 20000.00`, `currency: "USD"`, `effectiveDate: "2026-09-12"`,
`usdToVndRate: 25100.000000`, `rateEffectiveDate: "2026-09-12"`, `note: null`,
`correctsValuationId: null`, `current: true`, and a `recordedAt` equal to the asset's `createdAt`. The
history starts at creation; an asset with no record is a defect.

| Create | Expect |
|---|---|
| a VND asset with no rate members (`148000000.00`, `$TODAY`) — keep it as `VND` | `201`; its one record has `usdToVndRate: null`, `rateEffectiveDate: null`, `current: true` |
| a USD asset with no rate members | `201`; its one record has both rate members `null` — the design's *partial* state, not an error |
| a VND asset with `usdToVndRate` and `rateEffectiveDate` | `400`, two `errors`: `usdToVndRate` and `rateEffectiveDate`, each `must be absent for a VND valuation` |
| a USD asset with `usdToVndRate` and no `rateEffectiveDate` | `400`, `rateEffectiveDate` / `must be given together with usdToVndRate` |
| a USD asset with `rateEffectiveDate` and no `usdToVndRate` | `400`, `usdToVndRate` / `must be given together with rateEffectiveDate` |
| a USD asset with `rateEffectiveDate` of `$FUTURE` | `400`, `rateEffectiveDate` / `must not be in the future` |

### A newer value moves the asset

```bash
api "$A" POST /assets/$USD/valuations '{"value":"21250.00","currency":"USD","effectiveDate":"2026-09-14","usdToVndRate":"25100.000000","rateEffectiveDate":"2026-09-12","note":"Cập nhật số dư sau khi đối chiếu sao kê."}'
```

`201`, a `Location` of `/api/v1/assets/<USD>/valuations/<id>`, and `current: true`. Keep the identifier
as `CUR`. Then:

```bash
api "$A" GET /assets/$USD
api "$A" GET /assets/$USD/valuations
api "$A" GET /assets
```

The asset must now show `currentValue: 21250.00`, `valuationDate: "2026-09-14"`, an `updatedAt` later
than before and a `createdAt` that has not moved; `purchaseValue`, `purchaseDate`, `institution` and
`notes` unchanged. The history has **two** records, `2026-09-14` first and marked current, `2026-09-12`
second and not. The catalog lists the asset under its new valuation date.

### An older value is history and moves nothing

```bash
api "$A" POST /assets/$USD/valuations '{"value":"18000.00","currency":"USD","effectiveDate":"2026-05-31","usdToVndRate":"25020.000000","rateEffectiveDate":"2026-05-30","note":"Số dư cuối tháng được nhập ban đầu."}'
```

`201` with `current: false`. Keep the identifier as `OLD`. `GET /assets/$USD` must still answer
`21250.00` / `2026-09-14`, with `updatedAt` **identical** to the previous read — the asset row was not
written. The history has three records in the order `09-14`, `09-12`, `05-31`.

### A correction adds a record and keeps the original

```bash
api "$A" POST /assets/$USD/valuations '{"value":"18500.00","currency":"USD","effectiveDate":"2026-05-31","usdToVndRate":"25020.000000","rateEffectiveDate":"2026-05-30","note":"Điều chỉnh số dư cuối tháng sau khi đối chiếu sao kê.","correctsValuationId":"'$OLD'"}'
```

`201`, `current: false`, `correctsValuationId` echoed. Then:

```bash
api "$A" GET /assets/$USD/valuations/$OLD
api "$A" GET /assets/$USD/valuations
```

The original must still read `18000.00`, unchanged in every member. The history has **four** records:
`09-14`, `09-12`, then the two `05-31` records with the correction (`18500.00`, recorded later)
**before** the original. Nothing was deleted and nothing was edited.

Now correct the current record itself, on the same date:

```bash
api "$A" POST /assets/$USD/valuations '{"value":"21300.00","currency":"USD","effectiveDate":"2026-09-14","usdToVndRate":"25100.000000","rateEffectiveDate":"2026-09-12","note":"Sửa số dư gõ nhầm.","correctsValuationId":"'$CUR'"}'
```

`201` with `current: true`, and `GET /assets/$USD` now answers `21300.00`. The history has five
records and the correction is first; `$CUR` is second, still readable, no longer current. Two records on
one date: the later recorded wins.

| Send | Expect |
|---|---|
| a correction with no `note` | `400`, `note` / `must be given when correcting a valuation` |
| a correction with `"note":"   "` | the same — a blank note is no note |
| `correctsValuationId` of a random UUID | `400`, `correctsValuationId` / `must identify a valuation of this asset` |
| `correctsValuationId` of a record of the viewer's **other** asset (`$VND`'s first record) | the same |
| `correctsValuationId` of another user's record (create one as B first) | the same, byte-compared with the previous two apart from `correlationId` |

### Each required field missing, one case each

Remove one member at a time from the minimal body `{"value":"1.00","currency":"USD","effectiveDate":"$TODAY"}`:
`value`, `currency`, `effectiveDate`. Each must answer `400` `validation-failed` with an `errors`
entry naming **that** field and no other.

### Values, rates, lengths

Against `$USD` unless stated.

| Send | Expect |
|---|---|
| `"value":"-1.00"` | `value` / `must not be negative` |
| `"value":"100.123"` | `value` / `must have at most 17 integer digits and 2 decimals` |
| `"usdToVndRate":"0"` with a date | `usdToVndRate` / `must be greater than zero` |
| `"usdToVndRate":"25100.1234567"` with a date | `usdToVndRate` / `must have at most 13 integer digits and 6 decimals` |
| a rate and no date | `rateEffectiveDate` / `must be given together with usdToVndRate` |
| a date and no rate | `usdToVndRate` / `must be given together with rateEffectiveDate` |
| against `$VND`: a rate and a date | **two** `errors`, `usdToVndRate` and `rateEffectiveDate`, each `must be absent for a VND valuation` |
| a 2001-character `note` | `note` / `must be at most 2000 characters` |
| `"note":"Ghi\u0000chú"` | `note` / `must not contain a null character` |
| `"note":"  Ghi chú.  "` | `201`, `note` read back as `Ghi chú.` |
| `"note":"   "` | `201`, `note` read back as `null` |
| `"currency":"EUR"` | `400` `malformed-request`, no `errors` array |
| against `$VND`: `"value":"0.00"`, `$TODAY` | `201`, `current: true`; `GET /assets/$VND` answers `0.00` — an emptied holding is a real state. Run this one last in the section: it changes the VND asset |

### Dates

| Send | Expect |
|---|---|
| `effectiveDate` of `$FUTURE` | `effectiveDate` / `must not be in the future` |
| `rateEffectiveDate` of `$FUTURE`, with a rate | `rateEffectiveDate` / `must not be in the future` |
| `effectiveDate` of `2024-01-01` — before the asset's `purchaseDate` of `2024-03-18` | `effectiveDate` / `must not be earlier than the purchase date` |
| `"currency":"VND"`, `effectiveDate` of `$FUTURE`, a rate and a date | **four** `errors` in one response: `currency`, `effectiveDate`, `usdToVndRate`, `rateEffectiveDate` |

### The currency is the asset's

| Send | Expect |
|---|---|
| against `$USD`: `"currency":"VND"`, no rate | `currency` / `must match the asset's currency` |
| against `$VND`: `"currency":"USD"`, no rate | the same |

### Ownership

```bash
api "$B" GET /assets/$USD/valuations                                  # 404
api "$B" GET /assets/00000000-0000-4000-8000-000000000000/valuations  # 404
api "$B" POST /assets/$USD/valuations '{"value":"1.00","currency":"USD","effectiveDate":"'$TODAY'"}'  # 404
api "$B" GET /assets/$USD/valuations/$OLD                             # 404
api "$A" GET /assets/$VND/valuations/$OLD                             # 404 — a record of another of A's own assets
api "$A" GET /assets/$USD/valuations/00000000-0000-4000-8000-000000000000   # 404
```

Compare the bodies pairwise. They must match on `type`, `title`, `status` and `detail`, differing only
in `instance` and `correlationId`. After B's refused `POST`, read `$USD` as A: value and history count
unchanged.

Create an asset as B and read its history as A: `404`.

### Credentials

```bash
curl -s -i http://127.0.0.1:8080/api/v1/assets/$USD/valuations   # 401
api "$EXPIRED" GET /assets/$USD/valuations                       # 401
api "$FORGED" GET /assets/$USD/valuations                        # 401
```

All three `unauthorized`, indistinguishable apart from the correlation id.

### Protocol — and the routes that must not exist

| Send | Expect |
|---|---|
| `PUT /assets/$USD/valuations/$OLD` with a valid body | `405` `method-not-allowed` |
| `PATCH /assets/$USD/valuations/$OLD` | `405` |
| `DELETE /assets/$USD/valuations/$OLD` | `405` |
| `DELETE /assets/$USD/valuations` | `405` |
| `GET /assets/not-a-uuid/valuations` | `400` `malformed-request` |
| `GET /assets/$USD/valuations/not-a-uuid` | `400` `malformed-request` |
| a `text/plain` body on `POST` | `415` `unsupported-media-type` |
| `{"value": ` — truncated JSON | `400` `malformed-request` |
| `Accept: application/xml` | `406` `not-acceptable` |

After the three `405`s, `GET /assets/$USD/valuations/$OLD` must be unchanged. Every one of these must
be `application/problem+json`.

### Two requests at once

Four appends to one asset on the same date, each with a different value:

```bash
for v in 101.00 102.00 103.00 104.00; do api "$A" POST /assets/$USD/valuations '{"value":"'$v'","currency":"USD","effectiveDate":"'$TODAY'"}' -o /dev/null -w "%{http_code}\n" & done; wait
```

Four `201`s, never a `500`. Then `GET /assets/$USD` and `GET /assets/$USD/valuations`: exactly **one**
record is `current: true`, it is the first in the list, and the asset's `currentValue` equals **its**
value — not whichever request happened to finish last. Repeat a few times. The row lock in
`AssetRepository.findAndLockById` is what makes this hold; a snapshot that disagrees with the list is
the regression it exists to prevent.

An append racing the asset's own deletion:

```bash
T=$(api "$A" POST /assets '{"name":"Tạm","category":"CASH","currentValue":"1.00","currency":"VND","valuationDate":"'$TODAY'","liquidity":"HIGH"}' | tail -1 | python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])')
(api "$A" DELETE /assets/$T -o /dev/null -w "DELETE %{http_code}\n" &); for i in 1 2; do api "$A" POST /assets/$T/valuations '{"value":"2.00","currency":"VND","effectiveDate":"'$TODAY'"}' -o /dev/null -w "POST %{http_code}\n" & done; wait
```

The `DELETE` is `204`; each `POST` is `201` if it ran first or `404` if it ran second. **Any `500` is a
regression.**

### An edit racing an append

The edit route writes every column of the asset row, the three snapshot columns included, so an edit
that read the asset before a concurrent append committed would write the old value straight back. Both
routes take the same row lock to prevent it. Fire one of each at once, ten rounds:

```bash
for i in 1 2 3 4 5 6 7 8 9 10; do (api "$A" PUT /assets/$USD '{"name":"Tài khoản USD","liquidity":"HIGH","purchaseValue":"19500.00","purchaseDate":"2024-03-18","notes":"Sửa lần '$i'"}' -o /dev/null -w "PUT %{http_code}\n" & api "$A" POST /assets/$USD/valuations '{"value":"3000'$i'.00","currency":"USD","effectiveDate":"'$TODAY'"}' -o /dev/null -w "POST %{http_code}\n" & wait); api "$A" GET /assets/$USD | tail -1 | python3 -c 'import json,sys; a=json.load(sys.stdin); print(a["currentValue"], a["valuationDate"], a["notes"])'; done
```

Every `PUT` is `200` and every `POST` is `201`. Each append is dated today, so it supersedes the current
record; after each round the asset must show **that round's** value (`30001.00`, then `30002.00`, …)
and the **new** note. The edit landed and the snapshot was not rolled back. A round where the value
reverts to the previous one is the regression this exists to catch — and the probe only tests it when
the append actually changes the snapshot, which is why the date is today and not in the past.

### The purchase date and the whole history

The asset has a record dated `2026-04-29` (from the note cases) and a current record dated later.

| Send | Expect |
|---|---|
| `PUT /assets/$USD` with `purchaseDate` `2026-05-01` — after the earliest record, before the current date | `400`, `purchaseDate` / `must not be later than the valuation date` |
| the same with `purchaseDate` `2026-04-29` — equal to the earliest record | `200` |
| the same with `purchaseDate` back to `2024-03-18` | `200` |

The comparison is against the **earliest recorded** valuation, not only the current one, so the
history can never say the asset was valued before it was owned.

### An asset that existed before the history did

`V6` backfills one record for every asset that has none, from the asset's own snapshot. Reproduce the
situation and the statement:

```bash
LEGACY=$(uuidgen | tr A-Z a-z)
docker exec -i rikkaus-valuations-check psql -U rikkaus -d rikkaus -c "INSERT INTO wealth.assets (id, user_id, name, category, current_value, currency, valuation_date, liquidity, created_at, updated_at) VALUES ('$LEGACY', '0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93', 'Căn hộ Thảo Điền', 'REAL_ESTATE', 12002000000.00, 'VND', '2026-10-01', 'LOW', '2026-10-01T03:00:00Z', '2026-10-01T03:00:00Z');"
api "$A" GET /assets/$LEGACY/valuations      # 200, []
docker exec -i rikkaus-valuations-check psql -U rikkaus -d rikkaus -c "INSERT INTO wealth.asset_valuations (id, asset_id, value, currency, effective_date, recorded_at) SELECT gen_random_uuid(), a.id, a.current_value, a.currency, a.valuation_date, a.created_at FROM wealth.assets a WHERE NOT EXISTS (SELECT 1 FROM wealth.asset_valuations v WHERE v.asset_id = a.id);"
api "$A" GET /assets/$LEGACY/valuations      # 200, one record
```

The statement is the one in `V6__asset_valuations.sql`, character for character. It must report
`INSERT 0 1` — only the legacy asset lacked a record — and the record must carry `12002000000.00`,
`2026-10-01`, `current: true`, both rate members `null`, and `recordedAt` equal to the asset's
`createdAt`. A back-dated record appended afterwards (`2026-03-01`) must answer `current: false` and
leave the asset at `12002000000.00`.

### Exactness in the database

```bash
docker exec -i rikkaus-valuations-check psql -U rikkaus -d rikkaus -c "SELECT column_name, data_type, numeric_precision, numeric_scale FROM information_schema.columns WHERE table_schema='wealth' AND table_name='asset_valuations' ORDER BY ordinal_position;"
```

`value` must be `numeric(19,2)` and `usd_to_vnd_rate` `numeric(19,6)`. Then the stored figures:

```bash
docker exec -i rikkaus-valuations-check psql -U rikkaus -d rikkaus -c "SELECT value, currency, effective_date, usd_to_vnd_rate, rate_effective_date, recorded_at FROM wealth.asset_valuations WHERE asset_id = '$USD' ORDER BY effective_date DESC, recorded_at DESC;"
```

`25100.000000` and `25020.000000` exactly; no `2.51e+04`.

### The snapshot agrees with the history, for every asset

```bash
docker exec -i rikkaus-valuations-check psql -U rikkaus -d rikkaus -c "SELECT a.id, a.current_value, v.value, a.valuation_date, v.effective_date FROM wealth.assets a JOIN LATERAL (SELECT value, effective_date FROM wealth.asset_valuations WHERE asset_id = a.id ORDER BY effective_date DESC, recorded_at DESC LIMIT 1) v ON true WHERE a.current_value <> v.value OR a.valuation_date <> v.effective_date;"
```

**Zero rows.** A row here is an asset whose catalog value is not its current record's.

### The table refuses what the API has no route for

```bash
docker exec -i rikkaus-valuations-check psql -U rikkaus -d rikkaus -c "UPDATE wealth.asset_valuations SET value = 1 WHERE id = '$OLD';"
docker exec -i rikkaus-valuations-check psql -U rikkaus -d rikkaus -c "DELETE FROM wealth.asset_valuations WHERE id = '$OLD';"
```

Both refused by the trigger, each naming `wealth.asset_valuations is append-only`, and `$OLD` still
reads `18000.00` afterwards. Then the cascade that must still work:

```bash
docker exec -i rikkaus-valuations-check psql -U rikkaus -d rikkaus -c "SELECT count(*) FROM wealth.asset_valuations WHERE asset_id = '$VND';"
api "$A" DELETE /assets/$VND
docker exec -i rikkaus-valuations-check psql -U rikkaus -d rikkaus -c "SELECT count(*) FROM wealth.asset_valuations WHERE asset_id = '$VND';"
```

`204`, and the count goes from its previous value to `0`: deleting an asset still removes its history,
because the trigger lets a delete through once the parent row is gone. Then delete `$USD` the same way:
its history holds two corrections, each pointing at another row of the same cascade, and the composite
foreign key must let the whole set go together. `204`, and its count is `0`.

Finally the checks, bypassing the application:

```bash
docker exec -i rikkaus-valuations-check psql -U rikkaus -d rikkaus -c "INSERT INTO wealth.asset_valuations (id, asset_id, value, currency, effective_date, usd_to_vnd_rate, rate_effective_date, recorded_at) VALUES (gen_random_uuid(), '$USD', 1, 'VND', '2026-01-01', 25000, '2026-01-01', now());"
```

Repeat with: a USD row carrying a rate and no date; a negative `value`; a `corrects_valuation_id` naming
a record of another asset. Expect `asset_valuations_rate_only_for_usd`, `asset_valuations_rate_is_complete`,
`asset_valuations_value_is_not_negative` and `asset_valuations_correction_names_a_record_of_this_asset`
in turn, with the history count unchanged afterwards.

## Tear down

```bash
pkill -f rikkaus-wealth-api-0.0.1-SNAPSHOT.jar
```

```bash
docker rm -f rikkaus-valuations-check
```

## What this guide does not cover

- A real Google sign-in. The tokens are minted; the filter and verifier under test are the production
  ones.
- The mobile client. This is the API's behaviour only; the frontend task is issue #58.
- Converting a USD record to VND. The API publishes the value and the rate; the arithmetic is the
  client's, and the dashboard figure is issue #17's.
- Hardened asset deletion. Deleting an asset still removes its history by cascade; issue #102 owns
  undo, retention and linked records.
