# Manual test guide — guided onboarding checklist

The onboarding resource (`/api/v1/onboarding`, issue #95) ships with no automated test, by Product
Owner direction. This guide is the replacement: rerunning it is the only way a regression in this slice
is caught. The recorded result of the run that accompanied the implementation is
[`plans/reports/manual-verification-261114-1843-GH-95-onboarding-checklist.md`](../plans/reports/manual-verification-261114-1843-GH-95-onboarding-checklist.md).
Case numbers are the ones phase 1 of the plan wrote before any code existed.

Rerun it whenever `user_settings`, `assets`, `liabilities`, `cash_flow_entries` or `goals` changes
shape: the checklist reads those tables by column name and category value, so a rename there is not a
compile error here.

Nothing here needs a Google account, a deployed environment or the mobile app.

## Set up the run

Everything below assumes a shell in the repository root and a Docker daemon.

### 1. A disposable database

```bash
docker run -d --name rikkaus-pg-issue-95 -p 127.0.0.1:5495:5432 -e POSTGRES_DB=rikkaus -e POSTGRES_USER=rikkaus -e POSTGRES_PASSWORD=rikkaus-local-only postgres:18.6-alpine3.24
```

```bash
docker exec rikkaus-pg-issue-95 pg_isready -U rikkaus
```

### 2. The packaged application

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./services/api/mvnw -f services/api/pom.xml -DskipTests package
```

```bash
DB_URL=jdbc:postgresql://localhost:5495/rikkaus /opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home/bin/java -jar services/api/target/rikkaus-wealth-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=local --server.port=8095
```

The startup log is case O54: Flyway must report `Migrating schema "public" to version "11 - onboarding
progress"`, and the context must then start. `ddl-auto: validate` is what proves the migration and the
`OnboardingProgress` entity agree about every column, `SMALLINT` included.

### 3. Four users

A and B carry the main flow, C is the concurrency case, D exists only so O57 can attempt an insert for
a user with no row yet.

```bash
docker exec -i rikkaus-pg-issue-95 psql -U rikkaus -d rikkaus -c "INSERT INTO wealth.users (id, google_subject, email, display_name, created_at, updated_at) VALUES ('0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93','google-sub-a-95','an.nguyen@example.com','An Nguyễn', now(), now()), ('7b41e590-3c82-4f16-b0d7-59e1c4f3a842','google-sub-b-95','bich.tran@example.com','Bích Trần', now(), now()), ('c3a9e1f0-4d2b-4b7a-8e65-1f0d9c2b7a51','google-sub-c-95','cuong.le@example.com','Cường Lê', now(), now()), ('d84f2b6e-9a13-4c5d-b7e0-2c6a1f9e3b72','google-sub-d-95','dung.pham@example.com','Dung Phạm', now(), now());"
```

### 4. Access tokens and a helper

Mint the tokens exactly as [`manual-test-guide-goals.md`](manual-test-guide-goals.md) step 4 does, adding
`C` with `mint("c3a9e1f0-4d2b-4b7a-8e65-1f0d9c2b7a51")`. Minting rather than stubbing keeps every
request on the real `BearerTokenAuthenticationFilter`.

```bash
export A=<token A> B=<token B> C=<token C> EXPIRED=<expired> FORGED=<forged> TODAY=$(TZ=Asia/Ho_Chi_Minh date +%F)
```

```bash
api() { curl -s -i -H "Authorization: Bearer $1" -H 'Content-Type: application/json' -X "$2" "http://127.0.0.1:8095/api/v1$3" ${4:+-d "$4"}; }
```

```bash
pg() { docker exec -i rikkaus-pg-issue-95 psql -U rikkaus -d rikkaus -tA -c "$1"; }
```

## The cases

Read the status line and the body of each. "Steps = TFT…" means the seven `steps[].done` values in
order. Every error body must be `application/problem+json`; for `validation-failed` the `errors` array is
the assertion.

### A. A new user (O01–O03)

```bash
api "$A" GET /onboarding
pg "SELECT count(*) FROM wealth.onboarding_progress WHERE user_id = '0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93'"
```

O01: `200`, `status: IN_PROGRESS`, `currentStep: 1`, seven steps numbered 1–7 with keys
`EXCHANGE_RATE`, `CASH_AND_BANK_ACCOUNTS`, `MAJOR_ASSETS`, `LIABILITIES`, `RECURRING_CASH_FLOWS`,
`DASHBOARD_REVIEW`, `FIRST_GOAL`, all `false`, three timestamps `null`. O02: `0` rows. O03: a second
`GET` is byte-identical.

### B. Steps follow real records (O04–O13)

Run `api "$A" GET /onboarding` after each request and compare the steps.

```bash
api "$A" GET /settings/exchange-rate                                                    # O04 steps FFFFFFF
api "$A" PUT /settings/exchange-rate '{"usdToVndRate":"25100.000000","effectiveDate":"'$TODAY'"}'   # O05 TFFFFFF
api "$A" POST /assets '{"name":"Căn hộ Thảo Điền","category":"REAL_ESTATE","currentValue":"4200000000.00","currency":"VND","valuationDate":"'$TODAY'","liquidity":"LOW"}'   # O06 TFTFFFF, keep APT
api "$A" POST /assets '{"name":"Tài khoản Vietcombank","category":"BANK_ACCOUNT","currentValue":"85000000.00","currency":"VND","valuationDate":"'$TODAY'","liquidity":"HIGH","institution":"Vietcombank"}'   # O07 TTTFFFF, keep BANK
api "$A" POST /liabilities '{"name":"Vay mua nhà Vietcombank","category":"BANK_LOAN","outstandingBalance":"2400000000.00","currency":"VND","interestRate":"8.5"}'   # O08 TTTTFFF, keep LOAN
api "$A" POST /cash-flow-entries '{"entryType":"INCOME","category":"BUSINESS_INCOME","amount":"26200000.00","currency":"VND","timing":"ONE_TIME","effectiveDate":"2026-09-10","notes":"Thưởng dự án tháng 9"}'   # O09 still TTTTFFF
api "$A" POST /cash-flow-entries '{"entryType":"INCOME","category":"SALARY","amount":"45000000.00","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-01","notes":"Lương tháng"}'   # O10 TTTTTFF, keep SALARY
api "$A" POST /goals '{"name":"Quỹ dự phòng 6 tháng","type":"EMERGENCY_FUND","targetAmount":"270000000.00","targetDate":"2027-06-30"}'   # O11 TTTTTFT, keep GOAL
```

O04 is the trap: reading the rate settings creates a settings row with a null rate, and step 1 must
stay `false`. O06 adds the apartment before any bank account, so step 3 turns `true` while step 2 stays
`false`. O12: the last checklist body contains none of `4200000000`, `85000000`, `2400000000`,
`45000000`, `Vietcombank`, `Thảo Điền`, `Lương`, `Quỹ`. O13: still `0` progress rows for A.

### C. Deleting the last record undoes the step (O14–O19)

```bash
api "$A" DELETE /assets/$BANK              # O14 TFTTTFT
api "$A" POST /assets '{"name":"Tiền mặt dự phòng","category":"CASH","currentValue":"12500000.00","currency":"VND","valuationDate":"'$TODAY'","liquidity":"HIGH"}'   # O15 TTTTTFT
api "$A" DELETE /liabilities/$LOAN         # O16 TTTFTFT
api "$A" DELETE /cash-flow-entries/$SALARY # O17 TTTFFFT — the one-time entry is still there
api "$A" DELETE /goals/$GOAL               # O18 TTTFFFF
api "$A" DELETE /assets/$APT               # O19 TTFFFFF
```

Step 1 has no undo case: the settings resource has no route that removes a declared rate.

### D. Recording progress (O20–O36)

```bash
api "$A" PATCH /onboarding '{"currentStep":3}'     # O20 200, currentStep 3
pg "SELECT current_step, dashboard_reviewed_at, completed_at, dismissed_at FROM wealth.onboarding_progress WHERE user_id = '0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93'"   # O21 one row, 3, nulls
api "$A" GET /onboarding                           # O22 currentStep 3
api "$A" PATCH /onboarding '{"currentStep":0}'     # O23 validation-failed currentStep
api "$A" PATCH /onboarding '{"currentStep":8}'     # O24
api "$A" PATCH /onboarding '{"currentStep":-1}'    # O25
api "$A" PATCH /onboarding '{"currentStep":"ba"}'  # O26 malformed-request
api "$A" PATCH /onboarding '{"currentStep":2.5}'   # O27 malformed-request, NOT stored as 2
```

O23–O25 must name `currentStep` with `must be between 1 and 7`, and the row must still say `3` after
each of O23–O27.

```bash
api "$A" PATCH /onboarding '{"dashboardReviewed":true}'    # O28 step 6 true, dashboardReviewedAt set
api "$A" PATCH /onboarding '{"dashboardReviewed":true}'    # O29 same dashboardReviewedAt as O28
api "$A" PATCH /onboarding '{"dashboardReviewed":false}'   # O30 step 6 false, null
api "$A" PATCH /onboarding '{"currentStep":7,"dashboardReviewed":true,"status":"COMPLETED"}'   # O31
api "$A" GET /onboarding                                   # O32 equals O31
api "$A" PATCH /onboarding '{"status":"COMPLETED"}'        # O33 same completedAt
api "$A" PATCH /onboarding '{"status":"DISMISSED"}'        # O34 dismissedAt set, completedAt null
api "$A" PATCH /onboarding '{"status":"FINISHED"}'         # O35 malformed-request, state unchanged
api "$A" PATCH /onboarding '{"currentStep":null}'          # O36 200, unchanged
```

O31 must apply all three members and still report steps `TTFFFTF`: completion does not force any step
to `true`.

### E. Empty body, isolation, dismiss and reopen (O37–O43)

```bash
api "$B" PATCH /onboarding '{}'      # O37 200 default state, and B has 0 rows
api "$B" GET /onboarding             # O38 default state, all false — nothing of A's
api "$B" POST /assets '{"name":"Tiền gửi có kỳ hạn ACB","category":"TERM_DEPOSIT","currentValue":"300000000.00","currency":"VND","valuationDate":"'$TODAY'","liquidity":"MEDIUM","institution":"ACB"}'   # O39 B steps FFTFFFF
api "$B" PATCH /onboarding '{"status":"DISMISSED"}'     # O40
api "$B" PATCH /onboarding '{"status":"IN_PROGRESS"}'   # O41 both timestamps null, currentStep 1
api "$B" PATCH /onboarding '{"userId":"0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93","currentStep":4}'   # O42 B is at 4; A's row unchanged
pg "SELECT user_id, current_step, completed_at IS NULL, dismissed_at IS NOT NULL FROM wealth.onboarding_progress ORDER BY current_step"   # O43
```

O43: B at 4, neither completed nor dismissed; A at 7, dismissed.

### F. Authentication and protocol (O44–O53)

```bash
curl -s -i http://127.0.0.1:8095/api/v1/onboarding                         # O44 401 unauthorized
api "$EXPIRED" GET /onboarding                                              # O45 401, same body but correlationId
api "$FORGED" GET /onboarding                                               # O46 401, same
curl -s -i -X PATCH -H 'Content-Type: application/json' -d '{"currentStep":9}' http://127.0.0.1:8095/api/v1/onboarding   # O47 401, not 400
api "$A" PUT /onboarding '{"currentStep":2}'                                # O48 405
api "$A" POST /onboarding '{"currentStep":2}'; api "$A" DELETE /onboarding  # O49 405 both
curl -s -i -X PATCH -H "Authorization: Bearer $A" -H 'Content-Type: text/plain' -d 'currentStep=2' http://127.0.0.1:8095/api/v1/onboarding   # O50 415
api "$A" PATCH /onboarding '{"currentStep":'                                # O51 malformed-request
curl -s -i -H "Authorization: Bearer $A" -H 'Accept: application/xml' http://127.0.0.1:8095/api/v1/onboarding   # O52 406
```

O53, eight simultaneous first writes for C, who has no row yet:

```bash
for i in 1 2 3 4 5 6 7 8; do curl -s -o /dev/null -w '%{http_code}\n' -X PATCH -H "Authorization: Bearer $C" -H 'Content-Type: application/json' -d '{"currentStep":2}' http://127.0.0.1:8095/api/v1/onboarding & done; wait
pg "SELECT count(*), max(current_step) FROM wealth.onboarding_progress WHERE user_id = 'c3a9e1f0-4d2b-4b7a-8e65-1f0d9c2b7a51'"
```

Eight `200`, never a `500`; exactly one row at step 2.

### G. PostgreSQL (O54–O60)

```bash
pg "SELECT column_name || ':' || data_type FROM information_schema.columns WHERE table_schema = 'wealth' AND table_name = 'onboarding_progress' ORDER BY ordinal_position"   # O55
pg "SELECT * FROM wealth.onboarding_progress"                                                     # O56
pg "INSERT INTO wealth.onboarding_progress (user_id, current_step, created_at, updated_at) VALUES ('d84f2b6e-9a13-4c5d-b7e0-2c6a1f9e3b72', 8, now(), now())"   # O57
pg "UPDATE wealth.onboarding_progress SET completed_at = now(), dismissed_at = now() WHERE user_id = '0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93'"   # O58
pg "DELETE FROM wealth.users WHERE id = 'c3a9e1f0-4d2b-4b7a-8e65-1f0d9c2b7a51'"                   # O59, then count C's rows: 0
pg "SELECT version, success FROM public.flyway_schema_history WHERE version = '11'"               # O60 11|t
```

O55: exactly seven columns — `user_id:uuid`, `current_step:smallint`, then five `timestamp with time
zone` — and no `numeric`, `text` or `character` column. O56: no amount, asset name or institution
anywhere. O57 fails on `onboarding_progress_current_step_is_known`, O58 on
`onboarding_progress_completed_or_dismissed`.

## Tear down

```bash
docker rm -f rikkaus-pg-issue-95
```

Stop the API with Ctrl-C in its terminal.
