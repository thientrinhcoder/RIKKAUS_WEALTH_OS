# Manual test guide — the liability catalog

The liability catalog resource (`/api/v1/liabilities`, issue #62) ships with no automated test, by
Product Owner direction. This guide is the whole of the verification, and rerunning it is the only way
a regression in this slice is caught. The recorded result of the run that accompanied the
implementation is
[`plans/reports/manual-verification-261114-1735-GH-62-liability-catalog-schedules-collateral.md`](../plans/reports/manual-verification-261114-1735-GH-62-liability-catalog-schedules-collateral.md).

Every case below was written in
[`plans/261114-1735-GH-62-liability-catalog-schedules-collateral/phase-01-test-scenarios.md`](../plans/261114-1735-GH-62-liability-catalog-schedules-collateral/phase-01-test-scenarios.md)
before the slice had a line of code; the case codes here are that file's. Nothing here needs a Google
account, a deployed environment or the mobile app.

## Set up the run

Everything below assumes a shell in the repository root and a Docker daemon.

### 1. A disposable database

Not the shared `rikkaus-postgres-data` volume and not port 5432, so a run cannot touch your own
development database.

```bash
docker run -d --name rikkaus-pg-issue-62 -p 127.0.0.1:5462:5432 -e POSTGRES_DB=rikkaus -e POSTGRES_USER=rikkaus -e POSTGRES_PASSWORD=rikkaus-local-only postgres:18.6-alpine3.24
```

```bash
docker exec rikkaus-pg-issue-62 pg_isready -U rikkaus
```

### 2. The packaged application

The API needs JDK 25; the system default is often 21 and fails on `release 25`.

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./services/api/mvnw -f services/api/pom.xml package -DskipTests -DskipITs
```

```bash
DB_URL=jdbc:postgresql://localhost:5462/rikkaus /opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home/bin/java -jar services/api/target/rikkaus-wealth-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=local --server.port=8062
```

Two lines in the startup log are part of the verification (case S01). Flyway must report `now at
version v7`, and the context must then start: `spring.jpa.hibernate.ddl-auto: validate` is what proves
`V7__liabilities.sql` and the `Liability` entity agree about every column.

### 3. Two users and three assets

The catalog is owner-scoped and the collateral rule needs an asset on each side, so insert two users
directly (there is no sign-up route) and create assets through the real API.

```bash
docker exec -i rikkaus-pg-issue-62 psql -U rikkaus -d rikkaus -c "INSERT INTO wealth.users (id, google_subject, email, display_name, created_at, updated_at) VALUES ('0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93','google-sub-a','an.nguyen@example.com','An Nguyen', now(), now()), ('7b41e590-3c82-4f16-b0d7-59e1c4f3a842','google-sub-b','bich.tran@example.com','Bich Tran', now(), now());"
```

### 4. Access tokens

Mint them with the `local` profile's committed development secret and the exact claim set
`AccessTokenService` issues, so every request still travels through the real
`BearerTokenAuthenticationFilter`:

```bash
python3 - <<'PY'
import base64, hashlib, hmac, json, time
secret = b"local-development-only-jwt-signing-secret"
def b64(raw): return base64.urlsafe_b64encode(raw).rstrip(b"=").decode()
def mint(sub, ttl=3600, key=secret):
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
export A=<token A> B=<token B> EXPIRED=<expired> FORGED=<forged>
```

A helper keeps every case to one line. The fifth argument is the body, `''` when there is none, so a
header can follow it:

```bash
api() { local t="$1" m="$2" p="$3" b="${4:-}"; shift 4; curl -s -i -H "Content-Type: ${CT:-application/json}" ${t:+-H "Authorization: Bearer $t"} "$@" -X "$m" "http://127.0.0.1:8062/api/v1$p" ${b:+--data-binary "$b"}; }
```

Now the collateral assets. Keep the three identifiers from the `id` member of each response:

```bash
api "$A" POST /assets '{"name":"Căn hộ Thảo Điền","category":"REAL_ESTATE","currentValue":6100000000.00,"currency":"VND","valuationDate":"2026-11-01","liquidity":"LOW"}'
api "$A" POST /assets '{"name":"Xe Volvo XC60","category":"VEHICLE_OR_VALUABLE_ASSET","currentValue":1850000000.00,"currency":"VND","valuationDate":"2026-11-01","liquidity":"MEDIUM"}'
api "$B" POST /assets '{"name":"Nhà phố Gò Vấp","category":"REAL_ESTATE","currentValue":4200000000.00,"currency":"VND","valuationDate":"2026-11-01","liquidity":"LOW"}'
export ASSET_A=<first id> ASSET_A2=<second id> ASSET_B=<third id>
```

## The cases

Read the status line and the body of each. Where a case expects `validation-failed`, the `errors`
array and the field it names are the assertion, not the 400 on its own. Every error body must be
`application/problem+json`.

### C — every category and every frequency

Six creates, one per category, each `201` with `Location: /api/v1/liabilities/<id>`. Keep the ids.

```bash
api "$A" POST /liabilities "{\"name\":\"Vay mua nhà Vietcombank\",\"category\":\"BANK_LOAN\",\"outstandingBalance\":2400000000.00,\"currency\":\"VND\",\"interestRate\":8.5,\"startDate\":\"2024-03-15\",\"maturityDate\":\"2044-03-15\",\"recurringPaymentAmount\":23500000.00,\"recurringPaymentFrequency\":\"MONTHLY\",\"nextPaymentDate\":\"2026-12-05\",\"collateralAssetId\":\"$ASSET_A\",\"notes\":\"Thế chấp căn hộ Thảo Điền.\"}"   # C01, keep as L_BANK
api "$A" POST /liabilities '{"name":"Vay tiêu dùng FE Credit","category":"PERSONAL_LOAN","outstandingBalance":120000000.00,"currency":"VND","interestRate":18,"startDate":"2026-01-10","maturityDate":"2028-01-10","recurringPaymentAmount":5800000.00,"recurringPaymentFrequency":"MONTHLY","nextPaymentDate":"2026-12-10"}'   # C02, L_PERSONAL
api "$A" POST /liabilities '{"name":"Dư nợ ký quỹ SSI","category":"MARGIN_LOAN","outstandingBalance":350000000.00,"currency":"VND","interestRate":12.5}'   # C03, L_MARGIN
api "$A" POST /liabilities '{"name":"Thẻ tín dụng Techcombank","category":"CREDIT_CARD_BALANCE","outstandingBalance":38500000.00,"currency":"VND","nextPaymentDate":"2026-11-25"}'   # C04
api "$A" POST /liabilities '{"name":"Bảo lãnh hợp đồng thiết bị","category":"GUARANTEE_OBLIGATION","outstandingBalance":10000.00,"currency":"USD","startDate":"2026-06-01","maturityDate":"2027-06-01"}'   # C05
api "$A" POST /liabilities '{"name":"Nợ người thân mua xe","category":"OTHER_LIABILITY","outstandingBalance":150000000.00,"currency":"VND","recurringPaymentAmount":50000000.00,"recurringPaymentFrequency":"YEARLY","nextPaymentDate":"2027-01-15"}'   # C06, L_OTHER
```

C01 must echo `"interestRate":8.5000` and `"collateralAssetId":"<ASSET_A>"`. C03 shows what an absent
optional field reads back as: `null`, never `0` or `""`. C05 must carry no VND figure anywhere.

| Case | Request | Expect |
|---|---|---|
| C07 | only `name`, `category`, `outstandingBalance`, `currency` | `201`, nine `null` members |
| C08 | `"recurringPaymentFrequency":"QUARTERLY"` with an amount | `201` |
| C09 | `"SEMIANNUAL"` with an amount | `201` |
| C10 | `"YEARLY"` with an amount | `201` |
| C11 | `"outstandingBalance":1250.50,"currency":"USD"` | `201`, no converted figure |

```bash
api "$A" POST /liabilities '{"name":"Khoản phải trả đối tác","category":"OTHER_LIABILITY","outstandingBalance":65000000.00,"currency":"VND"}'
api "$A" POST /liabilities '{"name":"Vay vốn lưu động BIDV","category":"BANK_LOAN","outstandingBalance":500000000.00,"currency":"VND","recurringPaymentAmount":45000000.00,"recurringPaymentFrequency":"QUARTERLY","nextPaymentDate":"2026-12-31"}'
api "$A" POST /liabilities '{"name":"Vay mua xe VPBank","category":"BANK_LOAN","outstandingBalance":420000000.00,"currency":"VND","recurringPaymentAmount":78000000.00,"recurringPaymentFrequency":"SEMIANNUAL","nextPaymentDate":"2027-03-01"}'
api "$A" POST /liabilities '{"name":"Vay học phí người thân","category":"OTHER_LIABILITY","outstandingBalance":90000000.00,"currency":"VND","recurringPaymentAmount":30000000.00,"recurringPaymentFrequency":"YEARLY","nextPaymentDate":"2027-09-01"}'
api "$A" POST /liabilities '{"name":"Thẻ tín dụng USD Citibank","category":"CREDIT_CARD_BALANCE","outstandingBalance":1250.50,"currency":"USD","nextPaymentDate":"2026-12-01"}'
```

### R — every required field, missing one at a time

Each must be `400` `validation-failed` with exactly one `errors` entry naming the field.

```bash
api "$A" POST /liabilities '{"category":"BANK_LOAN","outstandingBalance":1000000.00,"currency":"VND"}'                      # R01 name: must not be blank
api "$A" POST /liabilities '{"name":"Vay thử","outstandingBalance":1000000.00,"currency":"VND"}'                            # R02 category: must not be null
api "$A" POST /liabilities '{"name":"Vay thử","category":"BANK_LOAN","currency":"VND"}'                                      # R03 outstandingBalance: must not be null
api "$A" POST /liabilities '{"name":"Vay thử","category":"BANK_LOAN","outstandingBalance":1000000.00}'                      # R04 currency: must not be null
api "$A" POST /liabilities '{"name":"   ","category":"BANK_LOAN","outstandingBalance":1000000.00,"currency":"VND"}'          # R05 name: must not be blank
api "$A" POST /liabilities '{"name":" ","category":"BANK_LOAN","outstandingBalance":1000000.00,"currency":"VND"}'       # R06 name: must not be blank (a non-breaking space)
```

### V — values

| Case | Send | Expect |
|---|---|---|
| V01 | `"outstandingBalance":-1.00` | `400`, `outstandingBalance: must not be negative` |
| V02 | `"outstandingBalance":100.123` | `400`, `must have at most 17 integer digits and 2 decimals` |
| V03 | `"outstandingBalance":0.00` | `201` — a paid-down balance is a real state |
| V04 | `"interestRate":-0.5` | `400`, `interestRate: must not be negative` |
| V05 | `"interestRate":8.12345` | `400`, `must have at most 3 integer digits and 4 decimals` |
| V06 | `"interestRate":1000` | `400`, same message as V05 |
| V07 | `"interestRate":0` | `201`, reads back `0.0000` |
| V08 | `"recurringPaymentAmount":0` with `MONTHLY` | `400`, `recurringPaymentAmount: must be greater than zero` |
| V09 | an amount and no frequency | `400`, `recurringPaymentFrequency: must be present when a recurring payment amount is given` |
| V10 | `MONTHLY` and no amount | `400`, `recurringPaymentAmount: must be present when a recurring payment frequency is given` |
| V11 | a 121-character `name` | `400`, `must be at most 120 characters` |
| V12 | a 2001-character `notes` | `400`, `must be at most 2000 characters` |
| V13 | `"name":"Vay\u0000nhà"` | `400`, `name: must not contain a null character` |
| V14 | `"notes":"  Ghi chú có khoảng trắng.  "` | `201`, `notes` read back stripped |

```bash
api "$A" POST /liabilities '{"name":"Vay thử","category":"BANK_LOAN","outstandingBalance":-1.00,"currency":"VND"}'
api "$A" POST /liabilities '{"name":"Vay thử","category":"BANK_LOAN","outstandingBalance":100.123,"currency":"VND"}'
api "$A" POST /liabilities '{"name":"Thẻ tín dụng đã tất toán","category":"CREDIT_CARD_BALANCE","outstandingBalance":0.00,"currency":"VND"}'
api "$A" POST /liabilities '{"name":"Vay thử","category":"BANK_LOAN","outstandingBalance":1000000.00,"currency":"VND","interestRate":-0.5}'
api "$A" POST /liabilities '{"name":"Vay thử","category":"BANK_LOAN","outstandingBalance":1000000.00,"currency":"VND","interestRate":8.12345}'
api "$A" POST /liabilities '{"name":"Vay thử","category":"BANK_LOAN","outstandingBalance":1000000.00,"currency":"VND","interestRate":1000}'
api "$A" POST /liabilities '{"name":"Vay không lãi từ cha mẹ","category":"OTHER_LIABILITY","outstandingBalance":200000000.00,"currency":"VND","interestRate":0}'
api "$A" POST /liabilities '{"name":"Vay thử","category":"BANK_LOAN","outstandingBalance":1000000.00,"currency":"VND","recurringPaymentAmount":0,"recurringPaymentFrequency":"MONTHLY"}'
api "$A" POST /liabilities '{"name":"Vay thử","category":"BANK_LOAN","outstandingBalance":1000000.00,"currency":"VND","recurringPaymentAmount":5800000.00}'
api "$A" POST /liabilities '{"name":"Vay thử","category":"BANK_LOAN","outstandingBalance":1000000.00,"currency":"VND","recurringPaymentFrequency":"MONTHLY"}'
api "$A" POST /liabilities "{\"name\":\"$(python3 -c "print('V'*121)")\",\"category\":\"BANK_LOAN\",\"outstandingBalance\":1000000.00,\"currency\":\"VND\"}"
api "$A" POST /liabilities "{\"name\":\"Vay thử\",\"category\":\"BANK_LOAN\",\"outstandingBalance\":1000000.00,\"currency\":\"VND\",\"notes\":\"$(python3 -c "print('g'*2001)")\"}"
api "$A" POST /liabilities '{"name":"Vay\u0000nhà","category":"BANK_LOAN","outstandingBalance":1000000.00,"currency":"VND"}'
api "$A" POST /liabilities '{"name":"Vay sửa nhà","category":"PERSONAL_LOAN","outstandingBalance":74000000.00,"currency":"VND","notes":"  Ghi chú có khoảng trắng.  "}'
```

### E — an unrecognised enum constant

All three are `400` **`malformed-request`** with no `errors` array, because Jackson cannot construct
the record. Switch on `type`, not on the status.

```bash
api "$A" POST /liabilities '{"name":"Vay thử","category":"CRYPTO_LOAN","outstandingBalance":1000000.00,"currency":"VND"}'                                           # E01
api "$A" POST /liabilities '{"name":"Vay thử","category":"BANK_LOAN","outstandingBalance":1000000.00,"currency":"EUR"}'                                              # E02
api "$A" POST /liabilities '{"name":"Vay thử","category":"BANK_LOAN","outstandingBalance":1000000.00,"currency":"VND","recurringPaymentAmount":100.00,"recurringPaymentFrequency":"WEEKLY"}'   # E03
```

### D — dates

| Case | Send | Expect |
|---|---|---|
| D01 | start `2025-01-01`, maturity `2024-01-01` | `400`, `maturityDate: must not be before the start date` |
| D02 | start `2026-01-10`, next `2025-12-10` | `400`, `nextPaymentDate: must not be before the start date` |
| D03 | maturity `2028-01-10`, next `2028-02-10` | `400`, `nextPaymentDate: must not be after the maturity date` |
| D04 | start and maturity both `2026-06-01` | `201` |
| D05 | only `nextPaymentDate` | `201` |
| D06 | start `2025-01-01`, maturity `2024-01-01`, next `2023-01-01` | `400`, **two** entries: `maturityDate` and `nextPaymentDate`, no third |
| D07 | next `2026-01-05` inside the term, in the past | `201` — overdue is a state, not an error |
| D08 | `"startDate":"15/03/2024"` | `400` `malformed-request` |

```bash
api "$A" POST /liabilities '{"name":"Vay thử","category":"BANK_LOAN","outstandingBalance":1000000.00,"currency":"VND","startDate":"2025-01-01","maturityDate":"2024-01-01"}'
api "$A" POST /liabilities '{"name":"Vay thử","category":"BANK_LOAN","outstandingBalance":1000000.00,"currency":"VND","startDate":"2026-01-10","nextPaymentDate":"2025-12-10"}'
api "$A" POST /liabilities '{"name":"Vay thử","category":"BANK_LOAN","outstandingBalance":1000000.00,"currency":"VND","maturityDate":"2028-01-10","nextPaymentDate":"2028-02-10"}'
api "$A" POST /liabilities '{"name":"Bảo lãnh một ngày","category":"GUARANTEE_OBLIGATION","outstandingBalance":5000.00,"currency":"USD","startDate":"2026-06-01","maturityDate":"2026-06-01"}'
api "$A" POST /liabilities '{"name":"Thẻ tín dụng VIB","category":"CREDIT_CARD_BALANCE","outstandingBalance":12000000.00,"currency":"VND","nextPaymentDate":"2026-11-25"}'
api "$A" POST /liabilities '{"name":"Vay thử","category":"BANK_LOAN","outstandingBalance":1000000.00,"currency":"VND","startDate":"2025-01-01","maturityDate":"2024-01-01","nextPaymentDate":"2023-01-01"}'
api "$A" POST /liabilities '{"name":"Vay quá hạn Agribank","category":"BANK_LOAN","outstandingBalance":80000000.00,"currency":"VND","startDate":"2025-01-05","maturityDate":"2027-01-05","nextPaymentDate":"2026-01-05"}'
api "$A" POST /liabilities '{"name":"Vay thử","category":"BANK_LOAN","outstandingBalance":1000000.00,"currency":"VND","startDate":"15/03/2024"}'
```

### K — the collateral

| Case | Send | Expect |
|---|---|---|
| K01 | A pledges `$ASSET_A2` | `201`, `collateralAssetId` echoed |
| K02 | A pledges `$ASSET_B` (user B's) | `400`, `collateralAssetId: must identify an asset the signed-in user owns` |
| K03 | A pledges `00000000-0000-4000-8000-000000000000` | `400`, same body as K02 apart from `correlationId` |
| K04 | `"collateralAssetId":"khong-phai-uuid"` | `400` `malformed-request` |

```bash
api "$A" POST /liabilities "{\"name\":\"Vay thế chấp xe\",\"category\":\"BANK_LOAN\",\"outstandingBalance\":900000000.00,\"currency\":\"VND\",\"collateralAssetId\":\"$ASSET_A2\"}"
api "$A" POST /liabilities "{\"name\":\"Vay thử\",\"category\":\"BANK_LOAN\",\"outstandingBalance\":1000000.00,\"currency\":\"VND\",\"collateralAssetId\":\"$ASSET_B\"}"
api "$A" POST /liabilities '{"name":"Vay thử","category":"BANK_LOAN","outstandingBalance":1000000.00,"currency":"VND","collateralAssetId":"00000000-0000-4000-8000-000000000000"}'
api "$A" POST /liabilities '{"name":"Vay thử","category":"BANK_LOAN","outstandingBalance":1000000.00,"currency":"VND","collateralAssetId":"khong-phai-uuid"}'
```

K02 and K03 side by side must agree on `type`, `title`, `status`, `detail` and `errors`. Nothing in
either body says whether the asset exists.

The remaining collateral cases use `PUT` and the asset's deletion, after the ownership cases below:

```bash
api "$A" PUT "/liabilities/$L_MARGIN" "{\"name\":\"Dư nợ ký quỹ SSI\",\"outstandingBalance\":350000000.00,\"currency\":\"VND\",\"interestRate\":12.5,\"collateralAssetId\":\"$ASSET_A2\"}"   # K06: 200, linked
api "$A" PUT "/liabilities/$L_MARGIN" '{"name":"Dư nợ ký quỹ SSI","outstandingBalance":350000000.00,"currency":"VND","interestRate":12.5}'                                               # K07: 200, collateralAssetId null
api "$A" PUT "/liabilities/$L_MARGIN" "{\"name\":\"Dư nợ ký quỹ SSI\",\"outstandingBalance\":350000000.00,\"currency\":\"VND\",\"interestRate\":12.5,\"collateralAssetId\":\"$ASSET_B\"}"    # K08: 400, same body as K02
api "$A" DELETE "/assets/$ASSET_A" ''                                                                                                                                                  # K05a: 204
api "$A" GET "/liabilities/$L_BANK" ''                                                                                                                                                 # K05b: 200, collateralAssetId null, everything else unchanged
docker exec -i rikkaus-pg-issue-62 psql -U rikkaus -d rikkaus -Atc "select coalesce(collateral_asset_id::text,'NULL') from wealth.liabilities where id='$L_BANK'"                      # K09: NULL
```

The liability survives the asset's deletion with its collateral cleared. **A `409` or a `500` on the
asset's `DELETE` is a defect**: the foreign key is `ON DELETE SET NULL`, and the asset resource's
behaviour must not change because a liability points at it.

### L — the catalog

```bash
api "$A" GET /liabilities '' | tail -1 | jq -r '.[] | "\(.nextPaymentDate // "null")  \(.name)"'   # L01
api "$B" GET /liabilities ''                                                                        # L02: 200, []
```

L01 must list soonest `nextPaymentDate` first, every `null` after all dated rows, and ties by `name`.
An empty catalog is `[]` with `200`, not a `404`.

### O — ownership

```bash
api "$A" GET "/liabilities/$L_BANK" ''                                                            # O01: 200
api "$B" GET "/liabilities/$L_BANK" ''                                                            # O02: 404 not-found
api "$B" GET /liabilities/00000000-0000-4000-8000-000000000000 ''                                 # O03: 404, same body as O02 apart from instance and correlationId
api "$B" PUT "/liabilities/$L_BANK" '{"name":"Đã bị sửa","outstandingBalance":1.00,"currency":"VND"}'   # O04: 404
api "$A" GET "/liabilities/$L_BANK" ''                                                            # O04b: unchanged
api "$B" DELETE "/liabilities/$L_BANK" ''                                                         # O05: 404; the row survives
```

### U — the replacement

| Case | Send | Expect |
|---|---|---|
| U01 | every field, balance `96000000.00`, `interestRate` `17.5`, collateral `$ASSET_A2` | `200`, all changed, `updatedAt` advanced, `createdAt` kept |
| U02 | a body also carrying `"category":"OTHER_LIABILITY"` | `200`, `category` still `PERSONAL_LOAN` |
| U03 | only `name`, balance, currency, start, maturity | `200`, rate, schedule, collateral and notes all `null`; re-read agrees |
| U04 | no `name` | `400`, `name: must not be blank` |
| U05 | maturity before start | `400`, `maturityDate: must not be before the start date` |
| U06 | `"outstandingBalance":4000.00,"currency":"USD"` | `200`, both changed |

```bash
api "$A" PUT "/liabilities/$L_PERSONAL" "{\"name\":\"Vay tiêu dùng FE Credit (đã trả 4 kỳ)\",\"outstandingBalance\":96000000.00,\"currency\":\"VND\",\"interestRate\":17.5,\"startDate\":\"2026-01-10\",\"maturityDate\":\"2028-01-10\",\"recurringPaymentAmount\":5800000.00,\"recurringPaymentFrequency\":\"MONTHLY\",\"nextPaymentDate\":\"2027-01-10\",\"collateralAssetId\":\"$ASSET_A2\",\"notes\":\"Đã trả trước 4 kỳ.\"}"
api "$A" PUT "/liabilities/$L_PERSONAL" '{"name":"Vay tiêu dùng FE Credit","category":"OTHER_LIABILITY","outstandingBalance":96000000.00,"currency":"VND","interestRate":17.5,"notes":"Thử đổi category."}'
api "$A" PUT "/liabilities/$L_PERSONAL" '{"name":"Vay tiêu dùng FE Credit","outstandingBalance":96000000.00,"currency":"VND","startDate":"2026-01-10","maturityDate":"2028-01-10"}'
api "$A" GET "/liabilities/$L_PERSONAL" ''
api "$A" PUT "/liabilities/$L_PERSONAL" '{"outstandingBalance":96000000.00,"currency":"VND"}'
api "$A" PUT "/liabilities/$L_PERSONAL" '{"name":"Vay tiêu dùng FE Credit","outstandingBalance":96000000.00,"currency":"VND","startDate":"2026-01-10","maturityDate":"2025-01-10"}'
api "$A" PUT "/liabilities/$L_PERSONAL" '{"name":"Vay tiêu dùng FE Credit","outstandingBalance":4000.00,"currency":"USD"}'
```

### A — credentials

```bash
curl -s -i http://127.0.0.1:8062/api/v1/liabilities      # A01: 401
api "$EXPIRED" GET /liabilities ''                       # A02: 401
api "$FORGED" GET /liabilities ''                        # A03: 401
```

All three `unauthorized`, and the three bodies indistinguishable apart from `correlationId`.

### P — protocol

| Case | Send | Expect |
|---|---|---|
| P01 | `GET /liabilities/not-a-uuid` | `400` `malformed-request` |
| P02 | `PATCH /liabilities/<id>` | `405` `method-not-allowed` |
| P03 | `DELETE /liabilities` | `405` |
| P04 | a `text/plain` body | `415` `unsupported-media-type` |
| P05 | `{"name": ` | `400` `malformed-request` |
| P06 | `Accept: application/xml` | `406` `not-acceptable` |

```bash
api "$A" GET /liabilities/not-a-uuid ''
api "$A" PATCH "/liabilities/$L_BANK" '{"name":"x"}'
api "$A" DELETE /liabilities ''
CT='text/plain' api "$A" POST /liabilities 'name=Vay'
api "$A" POST /liabilities '{"name": '
api "$A" GET /liabilities '' -H 'Accept: application/xml'
```

Every one must be `application/problem+json`.

### X — removal

```bash
api "$A" DELETE "/liabilities/$L_OTHER" ''   # X01: 204, no body
api "$A" GET "/liabilities/$L_OTHER" ''      # X02: 404
api "$A" DELETE "/liabilities/$L_OTHER" ''   # X03: 404
api "$A" GET /liabilities ''                 # X04: one fewer row
```

X05, the one concurrency case worth running, because it is what a double-tapped confirm button
produces. Repeat ten times, not once; it is a race:

```bash
ID=$(api "$A" POST /liabilities '{"name":"Nợ xóa đồng thời","category":"OTHER_LIABILITY","outstandingBalance":1000000.00,"currency":"VND"}' | tail -1 | jq -r .id); for i in 1 2 3 4; do curl -s -o /dev/null -w "%{http_code}\n" -H "Authorization: Bearer $A" -X DELETE "http://127.0.0.1:8062/api/v1/liabilities/$ID" & done; wait
```

Expect exactly one `204` and three `404` each round. **Any `500` is a regression.**

### S — the table, straight into PostgreSQL

```bash
docker exec -i rikkaus-pg-issue-62 psql -U rikkaus -d rikkaus -c "SELECT column_name, data_type, numeric_precision, numeric_scale, is_nullable FROM information_schema.columns WHERE table_schema='wealth' AND table_name='liabilities' ORDER BY ordinal_position;"   # S02
```

`outstanding_balance` and `recurring_payment_amount` must be `numeric(19,2)`, `interest_rate`
`numeric(7,4)`, the four date columns `date`. A `double precision` anywhere is a defect no HTTP case
reveals.

```bash
docker exec -i rikkaus-pg-issue-62 psql -U rikkaus -d rikkaus -c "SELECT conname, contype, confrelid::regclass, confdeltype FROM pg_constraint WHERE conrelid='wealth.liabilities'::regclass ORDER BY conname;"   # S03, S04
```

The eleven named `CHECK`s from `V7__liabilities.sql` must all appear, `liabilities_collateral_asset_id_fkey`
must point at `wealth.assets` with `confdeltype = n` (SET NULL), and `liabilities_user_id_fkey` at
`wealth.users` with `c` (CASCADE).

Then prove the table refuses what the API refuses, bypassing the application. Each must raise a named
check violation and insert nothing:

```bash
docker exec -i rikkaus-pg-issue-62 psql -U rikkaus -d rikkaus -c "INSERT INTO wealth.liabilities (id,user_id,name,category,outstanding_balance,currency,created_at,updated_at) VALUES (gen_random_uuid(),'0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93','Probe','CRYPTO_LOAN',1,'VND',now(),now());"
```

Repeat with `outstanding_balance` of `-1` (S06), `currency` of `'EUR'` (S07), `start_date`
`'2025-01-01'` with `maturity_date` `'2024-01-01'` (S08), `recurring_payment_frequency` `'MONTHLY'`
with no amount (S09), `interest_rate` of `-0.5` (S10) and a `name` of `'   '` (S11). Expect
`liabilities_category_is_known`, `liabilities_outstanding_balance_is_not_negative`,
`liabilities_currency_is_supported`, `liabilities_maturity_not_before_start`,
`liabilities_recurring_payment_is_complete`, `liabilities_interest_rate_is_not_negative` and
`liabilities_name_is_present_and_bounded` in turn, and no `Probe` row afterwards.

Finally read the stored values (S12):

```bash
docker exec -i rikkaus-pg-issue-62 psql -U rikkaus -d rikkaus -c "SELECT name, outstanding_balance, interest_rate, recurring_payment_amount FROM wealth.liabilities WHERE id='$L_BANK';"
```

`2400000000.00`, `8.5000` and `23500000.00`, whole and unrounded.

## Tear down

```bash
pkill -f rikkaus-wealth-api-0.0.1-SNAPSHOT.jar
```

```bash
docker rm -f rikkaus-pg-issue-62
```

## What this guide does not cover

- A real Google sign-in. The tokens are minted; the filter and verifier under test are the production
  ones, the Google handoff is not exercised.
- Two simultaneous *edits* of one liability. The last writer wins with no conflict reported; there is
  no row version, which is a product decision rather than an oversight.
- The mobile client (issue #61), the liabilities total (#65) and the schedule expansion (#74, #77).
- Anything the `assets`, `settings` or `identity` routes own beyond the three asset creates and one
  asset delete this run needs. Those have their own guides.
