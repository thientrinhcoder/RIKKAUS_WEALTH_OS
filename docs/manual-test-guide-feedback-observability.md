# Manual test guide — feedback and privacy-safe logging

The feedback resource (`/api/v1/feedback`) and the structured request logging of issue #110 ship with no
automated test, by Product Owner direction. This guide is the replacement and the only way a regression
in either is caught. The case numbers are the ones written before any code existed, in
`plans/261114-1843-GH-110-feedback-observability/phase-01-test-scenarios.md`; the recorded result of the
implementing run is
[`plans/reports/manual-verification-261114-1843-GH-110-feedback-observability.md`](../plans/reports/manual-verification-261114-1843-GH-110-feedback-observability.md).

There are two parts. Part one is the resource: create, list, validation, authentication and PostgreSQL
(F01–F36, P01–P08). Part two runs the API twice — once with the `local` profile and once with no profile
— sends requests carrying planted secrets, and then searches **the whole output of the process** for them
(O01–O13, J01–J07). Nothing needs a Google account or a deployed environment.

## Set up the run

Everything assumes a shell in the repository root and a Docker daemon.

```bash
docker run -d --name rikkaus-pg-issue-110 -p 127.0.0.1:5410:5432 -e POSTGRES_DB=rikkaus -e POSTGRES_USER=rikkaus -e POSTGRES_PASSWORD=rikkaus-local-only postgres:18.6-alpine3.24
```

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./services/api/mvnw -f services/api/pom.xml -DskipTests package
```

Start the API with the `local` profile, **capturing stdout and stderr into files**: those files are what
part two searches.

```bash
mkdir -p /tmp/issue-110/run-local && DB_URL=jdbc:postgresql://localhost:5410/rikkaus /opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home/bin/java -jar services/api/target/rikkaus-wealth-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=local --server.port=8110 > /tmp/issue-110/run-local/api.stdout 2> /tmp/issue-110/run-local/api.stderr
```

The startup log must show Flyway migrating to `12 - feedback entries` and the context starting;
`ddl-auto: validate` is what proves the migration and `FeedbackEntry` agree.

Insert two users (there is no sign-up endpoint) and mint their tokens exactly as
[`manual-test-guide-goals.md`](manual-test-guide-goals.md) does, with the `local` secret
`local-development-only-jwt-signing-secret`: `A` for `0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93`
(`an.nguyen@example.com`), `B` for `7b41e590-3c82-4f16-b0d7-59e1c4f3a842` (`bich.tran@example.com`), plus
`EXPIRED` and `FORGED`. A helper:

```bash
api() { curl -s -i -H "Authorization: Bearer $1" -H 'Content-Type: application/json' -X "$2" "http://127.0.0.1:8110/api/v1$3" ${4:+--data-binary "$4"} "${@:5}"; }
```

## Part one — the resource

Every error must be `application/problem+json`. Where `validation-failed` is expected, the `errors`
array and the field it names are the assertion, not the 400 alone.

### Create and list (F01–F11)

```bash
api "$A" POST /feedback '{"screen":"catalog/assets","category":"BUG","message":"Màn hình danh mục tài sản không hiển thị tỷ giá cập nhật 12/09/2026 sau khi lưu."}'
```

| # | Send | Expect |
|---|---|---|
| F01 | the request above | `201`, body `id`, `screen`, `category`, `message`, `createdAt`; **no `Location`**, no `userId` |
| F02 | `dashboard`, no category, "Mong có biểu đồ dòng tiền theo quý." | `201`, `"category":null` |
| F03 | `goals/new` `IDEA`; `settings/exchange-rate` `QUESTION`; `catalog/liabilities` `OTHER` | three `201` |
| F04 | message `"  Nút Lưu bị che bởi bàn phím trên iPhone 13.  "` | `201`, message returned without the surrounding spaces |
| F05 | `GET /feedback` as A, twice | `200`, six entries, newest first, identical both times |
| F06 | `GET /feedback` as B | `200`, `[]` |
| F07 | B sends one, then both list | B sees one, A still six |
| F08 | A sends with `"userId":"7b41e590-…"` in the body | `201`; it is A's (A's count goes up, B's does not) |
| F09 | A sends with `"createdAt":"2020-01-01T00:00:00Z"` and an `id` | `201`; server time and a fresh id |
| F10 | a 2000-character Vietnamese message | `201` |
| F11 | a 64-character screen code (`catalog/` + 56 × `a`) | `201` |

### Validation (F12–F25)

| # | Body | Expect |
|---|---|---|
| F12 | no `screen` | `screen` / `must not be blank` |
| F13 | no `message` | `message` / `must not be blank` |
| F14–F16 | `message` `""`, `"   "`, `" "` | `message` / `must not be blank` |
| F17 | 2001 characters | `message` / `must be at most 2000 characters` |
| F18 | `"Lỗi\u0000màn hình"` | `message` / `must not contain a null character` — not a 500 |
| F19 | `screen` `"Màn hình tài sản"` | `screen` / `must be a lowercase screen code such as dashboard or catalog/assets` |
| F20 | 65-character screen | `screen` / `must be at most 64 characters` |
| F21 | `screen` `"dashboard?token=ya29.fake-token-issue110"` | the pattern message; the body does not echo the value |
| F22 | `screen` `"Dashboard"` | the pattern message |
| F23–F24 | `category` `"PRAISE"`, `"bug"` | `malformed-request`, no `errors` |
| F25 | bad screen and empty message together | both errors in one response |

### Authentication, method, format (F26–F36)

| # | Request | Expect |
|---|---|---|
| F26–F28 | `POST` with no token, `EXPIRED`, `FORGED` | `401 unauthorized`, bodies identical but for `correlationId` |
| F29 | `GET` with no token | `401` |
| F30 | `PUT`, `DELETE`, `PATCH` on `/feedback` | `405`, `Allow: POST, GET` |
| F31–F32 | `GET` and `DELETE` on `/feedback/<F01 id>` | `404 not-found`; the row is still there |
| F33 | `Content-Type: text/plain` | `415` |
| F34–F35 | truncated JSON, `[]` | `400 malformed-request` |
| F36 | `Accept: application/xml` | `406` |

### PostgreSQL (P01–P08)

```bash
docker exec -i rikkaus-pg-issue-110 psql -U rikkaus -d rikkaus -c '\d wealth.feedback_entries'
```

P01 the columns, the `(user_id, created_at DESC)` index, four check constraints and the cascading foreign
key. P02 counts per user (A ten, B one at that point). P03 the F04 message stored without its spaces.
P04–P07 a direct `INSERT` with screen `'Màn hình'`, message `'   '`, a 2001-character message and
category `'PRAISE'` is refused by `feedback_entries_screen_is_a_code`, `…_message_is_not_blank`,
`…_message_is_bounded` and `…_category_is_known`. P08 an unknown `user_id` is a foreign-key violation.

## Part two — the logs

### The planted secrets

| Marker | Value | Sent as |
|---|---|---|
| M1 | `ya29.fake-token-issue110` | asset notes, feedback message, a bogus bearer token |
| M2 | A's real access token (`eyJ…`) | the `Authorization` header of every request by A |
| M3 | `an.nguyen@example.com` | a feedback message (and it is in the users table) |
| M4 | `4.200.000.000`, `4200000000` | an asset value, a feedback message |
| M5 | `Căn hộ Thảo Điền` | an asset name, a feedback message |
| M6 | `session-cookie-issue110` | a `Cookie` header |

Send each request with `X-Correlation-Id: issue110-local-oNN` so its line can be found.

| # | Request | Response |
|---|---|---|
| O01 | A creates asset `Căn hộ Thảo Điền`, `REAL_ESTATE`, `4200000000.00` VND, notes `ya29.fake-token-issue110` | `201` |
| O02 | the same with a negative value | `400` |
| O03 | `GET /assets/3f1c9a52-8d4b-4f0e-9c21-5b7a8e6d4f10` | `404` |
| O04 | A sends feedback "Căn hộ Thảo Điền trị giá 4.200.000.000 ₫, token ya29.fake-token-issue110, email an.nguyen@example.com" | `201` |
| O05 | a controlled 500, below | `500 internal-error` |
| O06 | truncated JSON containing M1 and M5 | `400` |
| O07 | `GET /feedback` with `Authorization: Bearer ya29.fake-token-issue110` | `401` |
| O08 | `GET /feedback` as A with `Cookie: session=session-cookie-issue110` | `200` |
| O09 | `GET /feedback` without a token | `401` |
| O10 | `GET /actuator/health` | `200`, and no `INFO` line |
| O11 | `GET /feedback` as A with `X-Correlation-Id: bad id` | `200`, a generated UUID in the header |

The controlled 500 makes PostgreSQL refuse a row that contains the secrets, which is exactly the path on
which the driver would otherwise quote the row back as `Failing row contains (…)`:

```bash
docker exec -i rikkaus-pg-issue-110 psql -U rikkaus -d rikkaus -c "ALTER TABLE wealth.feedback_entries ADD CONSTRAINT verification_rejects_thao_dien CHECK (message NOT LIKE '%Thảo Điền%') NOT VALID;"
```

Resend O04, expect `500`, then drop the constraint:

```bash
docker exec -i rikkaus-pg-issue-110 psql -U rikkaus -d rikkaus -c "ALTER TABLE wealth.feedback_entries DROP CONSTRAINT verification_rejects_thao_dien;"
```

### Search the whole process output (O12–O13)

```bash
cd /tmp/issue-110/run-local && cat api.stdout api.stderr > all.log && for m in ya29 fake-token-issue110 "$A" eyJ @example.com 4.200.000.000 4200000000 'Thảo Điền' session-cookie-issue110 'Failing row' 'generated security password' Bearer Cookie; do printf '%-30.30s %s\n' "$m" "$(grep -cF -- "$m" all.log)"; done
```

Every count must be `0`. Then `grep 'request completed' all.log` must show exactly one line per request,
each with the correlation identifier, the user's UUID (or `no-user`), the method, the route **template**
(`/api/v1/assets/{assetId}`, never the UUID; `unmatched` for a 401 from the security chain) and the
status and duration. The `ERROR` line of O05 must show exception class names and `\tat` frames only.

### The JSON run (J01–J07)

Stop the API, then start it with **no profile** and a separate signing secret from the environment, and
mint A's and B's tokens again with that secret:

```bash
mkdir -p /tmp/issue-110/run-json && RIKKAUS_JWT_SECRET=issue-110-json-run-only-signing-secret-not-for-reuse DB_URL=jdbc:postgresql://localhost:5410/rikkaus /opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home/bin/java -jar services/api/target/rikkaus-wealth-api-0.0.1-SNAPSHOT.jar --server.port=8110 > /tmp/issue-110/run-json/api.stdout 2> /tmp/issue-110/run-json/api.stderr
```

Repeat O01–O11 with `issue110-json-oNN` identifiers (J01), then:

```bash
cd /tmp/issue-110/run-json && jq -c . api.stdout > parsed.jsonl && wc -l < api.stdout && wc -l < parsed.jsonl
```

J02 both counts equal and `jq` reports no error. J03 every `request completed` object has
`correlationId`, `httpMethod`, `httpRoute`, `httpStatus`, `durationMs`, and `userId` when authenticated:

```bash
jq -c 'select(.message|startswith("request completed")) | {correlationId,userId,httpMethod,httpRoute,httpStatus,durationMs}' parsed.jsonl
```

J04 the O05 object's `stack_trace` holds class names and frames only. J05 the marker search above, on
this run's output, gives `0` everywhere. J06 the O04 line carries the same information in both formats.
J07 no banner on stdout, and Flyway's and the application's startup lines are JSON objects.

Finally stop the API and remove the database:

```bash
docker rm -f rikkaus-pg-issue-110
```
