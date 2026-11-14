# Manual test guide — financial goals and templates

The goals resource (`/api/v1/goals` and `/api/v1/goal-templates`, issue #86) ships with no automated
test, by Product Owner direction. This guide is the replacement: it is the whole of the verification,
and rerunning it is the only way a regression in this slice is caught. The recorded result of the run
that accompanied the implementation is
[`plans/reports/manual-verification-261114-1740-GH-86-generic-financial-goals-templates.md`](../plans/reports/manual-verification-261114-1740-GH-86-generic-financial-goals-templates.md).
The case numbers below are the ones that report uses, and they are the ones phase 1 of the plan wrote
before any code existed.

Nothing here needs a Google account, a deployed environment or the mobile app.

## Set up the run

Everything below assumes a shell in the repository root and a Docker daemon.

### 1. A disposable database

Not the shared `rikkaus-postgres-data` volume, and not port 5432, so a run cannot touch whatever your
own development database holds.

```bash
docker run -d --name rikkaus-pg-issue-86 -p 127.0.0.1:5486:5432 -e POSTGRES_DB=rikkaus -e POSTGRES_USER=rikkaus -e POSTGRES_PASSWORD=rikkaus-local-only postgres:18.6-alpine3.24
```

```bash
docker exec rikkaus-pg-issue-86 pg_isready -U rikkaus
```

### 2. The packaged application

The API needs JDK 25; the system default is often 21 and fails on `release 25`.

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./services/api/mvnw -f services/api/pom.xml -DskipTests package
```

```bash
DB_URL=jdbc:postgresql://localhost:5486/rikkaus JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home /opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home/bin/java -jar services/api/target/rikkaus-wealth-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=local --server.port=8086
```

Two lines of the startup log are part of the verification. Flyway must report that it applied the
goals migration and is now at the version that includes it, and the context must then start:
`spring.jpa.hibernate.ddl-auto: validate` is what proves the migration and the `Goal` entity agree
about every column, and a mismatch stops the application instead of corrupting a row later.

### 3. Two users

The resource is owner-scoped, so the cross-user cases need two rows. There is no sign-up endpoint —
accounts are created by a Google sign-in — so insert them directly.

```bash
docker exec -i rikkaus-pg-issue-86 psql -U rikkaus -d rikkaus -c "INSERT INTO wealth.users (id, google_subject, email, display_name, created_at, updated_at) VALUES ('0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93','google-sub-a','an.nguyen@example.com','An Nguyen', now(), now()), ('7b41e590-3c82-4f16-b0d7-59e1c4f3a842','google-sub-b','bich.tran@example.com','Bich Tran', now(), now());"
```

### 4. Access tokens

Mint them with the `local` profile's committed development secret and the exact claim set
`AccessTokenService` issues. Minting rather than stubbing matters: the requests still travel through
the real `BearerTokenAuthenticationFilter` and the real `AccessTokenService.verify`.

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

Export them, and set the three dates as the application judges them — Vietnamese civil dates, not
UTC:

```bash
export A=<token A> B=<token B> EXPIRED=<expired> FORGED=<forged> TODAY=$(TZ=Asia/Ho_Chi_Minh date +%F) YESTERDAY=$(TZ=Asia/Ho_Chi_Minh date -v-1d +%F) TOMORROW=$(TZ=Asia/Ho_Chi_Minh date -v+1d +%F)
```

A short helper keeps every case below to one line:

```bash
api() { curl -s -i -H "Authorization: Bearer $1" -H 'Content-Type: application/json' -X "$2" "http://127.0.0.1:8086/api/v1$3" ${4:+-d "$4"}; }
```

## The cases

Read the status line and the body of each. Where a case expects `validation-failed`, the `errors`
array and the field it names are the assertion — not the 400 on its own, which several unrelated
mistakes also produce. Every error body must be `application/problem+json`.

### A. All seven templates are accepted (G01–G07)

Seven creates, one per type. Each must answer `201` with a `Location` of `/api/v1/goals/<id>` and a
body carrying `"currency":"VND"`. Keep the identifiers; later cases need them.

```bash
api "$A" POST /goals '{"name":"Quỹ dự phòng 6 tháng","type":"EMERGENCY_FUND","targetAmount":"270000000.00","currentAllocatedAmount":"90000000.00","targetDate":"2027-06-30","plannedMonthlyContribution":"15000000.00","expectedAnnualReturnPercent":"3.00"}'
```

| # | `type` | `name` | amounts |
|---|---|---|---|
| G01 | `EMERGENCY_FUND` | Quỹ dự phòng 6 tháng | 270.000.000 / 90.000.000 / 30/06/2027 / 15.000.000 / 3,00 |
| G02 | `HOME_PURCHASE` | Mua căn hộ | 3.500.000.000 / 1.200.000.000 / 31/12/2030 / 25.000.000 / 7,00 |
| G03 | `EDUCATION` | Học phí đại học cho con | 900.000.000 / 150.000.000 / 31/08/2032 / 8.000.000 / 5,00 |
| G04 | `RETIREMENT` | Hưu trí | 15.000.000.000 / 2.150.000.000 / 31/12/2041 / 20.000.000 / 8,00 |
| G05 | `NET_WORTH_GROWTH` | Tăng trưởng tài sản ròng | 15.000.000.000 / 10.470.000.000 / 31/12/2030 / 10.000.000 / 7,00 |
| G06 | `DEBT_REDUCTION` | Tất toán vay mua nhà | 1.500.000.000 / 90.000.000 / 31/12/2030 / 10.000.000 / **0,00** |
| G07 | `OTHER_GOAL` | Du lịch Nhật Bản | 120.000.000 / — / 31/03/2028 / — / — |

G06 must echo `"expectedAnnualReturnPercent":0.00`, not `null`: zero is an answer. G07 is the
optional-fields-absent case and must come back `"currentAllocatedAmount":0.00`,
`"plannedMonthlyContribution":null`, `"expectedAnnualReturnPercent":null`.

```bash
api "$A" POST /goals '{"name":"Du lịch Nhật Bản","type":"OTHER_GOAL","targetAmount":"120000000.00","targetDate":"2028-03-31"}'
```

### B. Listing (G08–G09)

```bash
api "$A" GET /goals
api "$B" GET /goals
```

User A's list must be in `targetDate` order and then by name: Quỹ dự phòng 6 tháng (2027-06-30),
Du lịch Nhật Bản (2028-03-31), then the three dated 2030-12-31 alphabetically — Mua căn hộ, Tăng
trưởng tài sản ròng, Tất toán vay mua nhà — then Học phí đại học cho con (2032-08-31) and Hưu trí
(2041-12-31). Run it twice and compare. User B's must be `[]` with a `200`, never a `404`.

### C. Required fields and the name (G10–G17)

Remove one member at a time from the G01 body. Each must answer `400` `validation-failed` with an
`errors` entry naming **that** field and no other.

| # | Send | Expect |
|---|---|---|
| G10 | no `name` | `name` / `must not be blank` |
| G11 | no `type` | `type` / `must not be null` |
| G12 | no `targetAmount` | `targetAmount` / `must not be null` |
| G13 | no `targetDate` | `targetDate` / `must not be null` |
| G14 | `"name":"   "` | `name` / `must not be blank` |
| G15 | a 121-character `name` | `name` / `must be at most 120 characters` |
| G16 | `"name":" "` (one non-breaking space) | `name` / `must not be blank` |
| G17 | `"name":"Quỹ\u0000dự phòng"` | `name` / `must not contain a null character` |

G16 and G17 exist because review of the asset slice found them: a non-breaking space is not whitespace
to `@NotBlank`, and a null character cannot be stored in a PostgreSQL `text` column. Put the NUL in
the **middle** of the value.

### D. Amounts and the return (G18–G31)

| # | Send | Expect |
|---|---|---|
| G18 | `"targetAmount":"0"` | `targetAmount` / `must be greater than zero` |
| G19 | `"targetAmount":"-1000000"` | `targetAmount` / `must be greater than zero` |
| G20 | `"targetAmount":"270000000.123"` | `targetAmount` / `must have at most 17 integer digits and 2 decimals` |
| G21 | `"targetAmount":"100000000000000000.00"` (18 integer digits) | the same message — refused before it can reach `NUMERIC(19, 2)` |
| G22 | `"currentAllocatedAmount":"-1"` | `currentAllocatedAmount` / `must not be negative` |
| G23 | allocated `300000000.00` against a target of `270000000.00` | `currentAllocatedAmount` / `must not exceed the target amount` |
| G24 | allocated equal to the target | `201` — a reached goal is a real state |
| G25 | `"plannedMonthlyContribution":"-5000000"` | `plannedMonthlyContribution` / `must not be negative` |
| G26 | `"plannedMonthlyContribution":"0.00"` | `201`, echoed as `0.00` |
| G27 | `"expectedAnnualReturnPercent":"-0.5"` | `expectedAnnualReturnPercent` / `must be between 0 and 30` |
| G28 | `"expectedAnnualReturnPercent":"30.01"` | `expectedAnnualReturnPercent` / `must be between 0 and 30` |
| G29 | `"expectedAnnualReturnPercent":"30.00"` | `201` |
| G30 | `"expectedAnnualReturnPercent":"7.125"` | `expectedAnnualReturnPercent` / `must have at most 3 integer digits and 2 decimals` |
| G31 | `"targetAmount":"-1"` **and** `"currentAllocatedAmount":"-1"` | **two** `errors` entries, one per field, and no third about exceeding the target |

### E. The target date (G32–G35)

Use `$YESTERDAY`, `$TODAY` and `$TOMORROW` rather than literals. A hardcoded date stops meaning what it
meant, and then a pass reads as a failure.

| # | Send | Expect |
|---|---|---|
| G32 | `"targetDate":"'$YESTERDAY'"` | `targetDate` / `must be after today` |
| G33 | `"targetDate":"'$TODAY'"` | `targetDate` / `must be after today` — the design says *after* the reference day |
| G34 | `"targetDate":"'$TOMORROW'"` | `201` |
| G35 | `"targetDate":"31/12/2030"` | `400` `malformed-request` — not ISO 8601, so the body cannot be parsed |

The date is judged in `Asia/Ho_Chi_Minh`. To see that this is real, run G34 late in a Vietnamese
evening: it must be accepted even though UTC still calls tomorrow "today".

### F. Unknown constants and members the resource does not accept (G36–G38)

```bash
api "$A" POST /goals '{"name":"Lạ","type":"CRYPTO","targetAmount":"1000000.00","targetDate":"2030-12-31"}'
```

| # | Send | Expect |
|---|---|---|
| G36 | `"type":"CRYPTO"` | `400` `malformed-request`, no `errors` array — Jackson cannot construct the record |
| G37 | the G01 body plus `"currency":"USD"` | `201`, and the response says `"currency":"VND"` — the member is an unknown property and is dropped |
| G38 | the G01 body plus `"completionPercent":50` | `201`, and no `completionPercent` anywhere in the response — issue #89's outputs are never inputs |

### G. Replacing a goal (G39–G46)

```bash
api "$A" PUT /goals/<G01's id> '{"name":"Quỹ dự phòng 9 tháng","type":"OTHER_GOAL","targetAmount":"405000000.00","currentAllocatedAmount":"120000000.00","targetDate":"2027-12-31","plannedMonthlyContribution":"18000000.00","expectedAnnualReturnPercent":"2.50"}'
```

| # | Send | Expect |
|---|---|---|
| G39 | every field changed, the `type` included | `200`, all seven changed, `updatedAt` advanced, `createdAt` and `id` unchanged |
| G40 | only the four required fields | `200`, `currentAllocatedAmount: 0.00`, `plannedMonthlyContribution: null`, `expectedAnnualReturnPercent: null`; `GET` it back to confirm it was stored so and not merely rendered so |
| G41 | no `name` | `name` / `must not be blank` |
| G42 | allocated above the target | `currentAllocatedAmount` / `must not exceed the target amount` |
| G43 | `"targetDate":"'$YESTERDAY'"` | `targetDate` / `must be after today` |
| G44 | see below | `200` |
| G45 | see below | `targetDate` / `must be after today` |
| G46 | `"type":"CRYPTO"` | `400` `malformed-request` |

G44 and G45 are the overdue-goal pair. The API refuses to *create* a goal in the past, so make one
overdue by hand:

```bash
docker exec -i rikkaus-pg-issue-86 psql -U rikkaus -d rikkaus -c "UPDATE wealth.goals SET target_date = '2026-01-31' WHERE name = 'Du lịch Nhật Bản';"
```

G44 then sends a `PUT` that changes the name but **keeps** `"targetDate":"2026-01-31"`: it must be
`200`, because a stored date is always allowed to stay. G45 sends `"targetDate":"2026-02-28"` — a
different past date — and must be refused. A `400` on G44 means an overdue goal can never be edited
again, not even to fix a typo.

### H. Ownership (G47–G52)

```bash
api "$A" GET /goals/<A's goal>     # 200
api "$B" GET /goals/<A's goal>     # 404
api "$B" GET /goals/00000000-0000-4000-8000-000000000000   # 404
api "$B" PUT /goals/<A's goal> '{"name":"Chiếm dụng","type":"OTHER_GOAL","targetAmount":"1.00","targetDate":"2030-12-31"}'   # 404
api "$B" DELETE /goals/<A's goal>  # 404
```

Compare the second and third bodies. They must match on `type`, `title`, `status` and `detail`, and
differ only in `instance` and `correlationId`. Any other difference is an enumeration oracle, which is
the whole reason this answers `404` rather than `403`. Afterwards read A's goal as A: the refused
write must have changed nothing, and the refused delete must have left the row.

G52: user B creates one goal — `Tiền mặt dự phòng của Bích`, 50.000.000 ₫ — and lists. Exactly one
row, none of A's.

### I. Credentials (G53–G57)

```bash
curl -s -i http://127.0.0.1:8086/api/v1/goals                              # 401
api "$EXPIRED" GET /goals                                                  # 401
api "$FORGED" GET /goals                                                   # 401
curl -s -i http://127.0.0.1:8086/api/v1/goals/<A's goal>                   # 401
curl -s -i http://127.0.0.1:8086/api/v1/goal-templates                     # 401
```

All five must be `401` `unauthorized`, and the first three bodies must be indistinguishable apart from
the correlation id. The templates are the same for everyone and are still behind the token: the
security chain denies by default, and nothing here justifies widening the public list.

### J. Protocol (G58–G63)

| # | Send | Expect |
|---|---|---|
| G58 | `GET /goals/not-a-uuid` | `400` `malformed-request` |
| G59 | `PATCH /goals/<id>` | `405` `method-not-allowed` |
| G60 | `DELETE /goals` | `405` `method-not-allowed` |
| G61 | a `text/plain` body on `POST /goals` | `415` `unsupported-media-type` |
| G62 | `{"name": ` — truncated JSON | `400` `malformed-request` |
| G63 | `Accept: application/xml` | `406` `not-acceptable` |

Every one of these must be `application/problem+json`. Boot's default error shape appearing anywhere
is a defect.

### K. Removal (G64–G68)

```bash
api "$A" DELETE /goals/<id>   # 204, no body
api "$A" GET /goals/<id>      # 404
api "$A" DELETE /goals/<id>   # 404
api "$A" GET /goals           # 200, one fewer row
```

Then the race, which is what a double-tapped confirm button produces:

```bash
ID=<a goal the viewer owns>
for i in 1 2 3 4; do api "$A" DELETE "/goals/$ID" -o /dev/null -w "%{http_code}\n" & done; wait
```

Expect exactly one `204` and three `404`. **Any `500` is a regression.** Repeat it ten times, not once:
it is a race, and the asset slice returned 500 for 73 of 100 requests before it flushed inside the
transaction.

### L. Templates (G69–G70)

```bash
api "$A" GET /goal-templates
api "$A" POST /goal-templates '{}'
```

Seven entries, in this order and with these values, which are the accepted design's table "Bảy mẫu và
thứ chúng điền sẵn":

| `type` | `defaultExpectedAnnualReturnPercent` | `defaultHorizonMonths` | `amountPrefill` |
|---|---|---|---|
| `EMERGENCY_FUND` | `3.00` | `18` | `TARGET_FROM_SIX_MONTHS_OF_EXPENSES` |
| `HOME_PURCHASE` | `6.00` | `60` | `NONE` |
| `EDUCATION` | `5.00` | `72` | `NONE` |
| `RETIREMENT` | `8.00` | `180` | `NONE` |
| `NET_WORTH_GROWTH` | `7.00` | `48` | `ALLOCATED_FROM_CURRENT_NET_WORTH` |
| `DEBT_REDUCTION` | `0.00` | `48` | `TARGET_FROM_TOTAL_LIABILITIES` |
| `OTHER_GOAL` | `0.00` | `24` | `NONE` |

The `POST` must be `405`: the templates are read-only.

### M. Straight into PostgreSQL (D01–D11)

The API's JSON is not the last word; read the stored value and the stored types.

```bash
docker exec -i rikkaus-pg-issue-86 psql -U rikkaus -d rikkaus -c "SELECT column_name, data_type, numeric_precision, numeric_scale FROM information_schema.columns WHERE table_schema='wealth' AND table_name='goals' ORDER BY ordinal_position;"
```

D01: the three money columns must be `numeric(19,2)` and `expected_annual_return_percent`
`numeric(5,2)`. A `double precision` anywhere is a defect no API-level case would reveal.

```bash
docker exec -i rikkaus-pg-issue-86 psql -U rikkaus -d rikkaus -c "SELECT name, target_amount FROM wealth.goals ORDER BY target_amount DESC LIMIT 2;"
```

D02: `15000000000.00`, whole and unrounded — not `1.5e+10`.

D03–D10 prove the table refuses what the API refuses, bypassing the application entirely. Each must
raise the named check violation and insert nothing:

```bash
docker exec -i rikkaus-pg-issue-86 psql -U rikkaus -d rikkaus -c "INSERT INTO wealth.goals (id,user_id,name,type,target_amount,current_allocated_amount,target_date,created_at,updated_at) VALUES (gen_random_uuid(),'0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93','Probe','CRYPTO',1,0,'2030-12-31',now(),now());"
```

| # | Probe | Constraint that must refuse it |
|---|---|---|
| D03 | `type` of `CRYPTO` | `goals_type_is_known` |
| D04 | `target_amount` of `0` | `goals_target_amount_is_positive` |
| D05 | `current_allocated_amount` of `-1` | `goals_allocated_amount_is_not_negative` |
| D06 | allocated `2` against a target of `1` | `goals_allocated_amount_within_target` |
| D07 | `expected_annual_return_percent` of `31` | `goals_expected_return_within_bounds` |
| D08 | `currency` of `USD` | `goals_currency_is_vnd` |
| D09 | `name` of `'   '` | `goals_name_is_present_and_bounded` |
| D10 | `planned_monthly_contribution` of `-1` | `goals_contribution_is_not_negative` |

D11: `SELECT user_id, count(*) FROM wealth.goals GROUP BY user_id` must agree with what each user's
`GET /goals` returned, and `SELECT count(*)` must not have moved after D03–D10.

## Tear down

Leaving these running holds port 8086 and port 5486 and a container, which the next run then has to
work around.

```bash
pkill -f 'rikkaus-wealth-api-0.0.1-SNAPSHOT.jar.*8086'
```

```bash
docker rm -f rikkaus-pg-issue-86
```

## What this guide does not cover

- A real Google sign-in. The tokens are minted; the filter and the verifier under test are the
  production ones, the Google handoff is not.
- Concurrency beyond the delete race. Two simultaneous `PUT`s of one goal both succeed and the last
  writer wins, with no conflict reported — there is no row version, which is a product decision about
  edit conflicts rather than an oversight.
- Any projection. Completion, remaining amount, remaining months, required contribution and status are
  issue #89's and have no route yet.
- The mobile client. This is the API's behaviour only; the frontend task is issue #85.
