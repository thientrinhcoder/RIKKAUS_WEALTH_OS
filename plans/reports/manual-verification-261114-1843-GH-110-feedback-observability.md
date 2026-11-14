# Manual verification — in-product feedback & privacy-safe observability (#110)

Ngày chạy: 14/11/2026, 18:59–19:00 ICT. Nhánh `thientrinhcoder/feat/issue-110-feedback-observability`,
cắt từ `main` tại `d9abcd8`. Jar đóng gói từ commit **`5958f95`** với working tree sạch, nên đây là lần
chạy trên commit cuối của phần code, không phải trên working tree.

Product Owner chỉ đạo task này ship **không có unit test hay integration test mới**. Danh sách scenario
được viết trước khi có dòng code nào
(`plans/261114-1843-GH-110-feedback-observability/phase-01-test-scenarios.md`), rồi chạy thật qua HTTP
vào PostgreSQL trong Docker; phần log được kiểm bằng cách grep **toàn bộ** stdout và stderr của tiến trình
API. Mọi status, body và dòng log dưới đây được sao từ lần chạy thật. Quy trình chạy lại:
[`docs/manual-test-guide-feedback-observability.md`](../../docs/manual-test-guide-feedback-observability.md).

## Kết quả

**64 case: 36 HTTP feedback (F01–F36), 8 PostgreSQL (P01–P08), 13 log `local` (O01–O13), 7 log JSON
(J01–J07). 64 pass, 0 fail.** Không case nào fail rồi phải sửa code trong lần chạy scenario. Hai lỗi thật
được phát hiện và sửa *trước* lần chạy scenario, trong lúc smoke test khi đang code, và được ghi ở mục
"Phát hiện trước lần chạy".

## Thiết lập

| | |
|---|---|
| Database | Container dùng một lần `rikkaus-pg-issue-110`, `postgres:18.6-alpine3.24`, bind `127.0.0.1:5410`, tạo mới ngay trước lần chạy; volume `rikkaus-postgres-data` không bị chạm. Đã `docker rm -f` khi xong |
| Lần 1 (`local`) | `java -jar api.jar --spring.profiles.active=local --server.port=8110`, `DB_URL=jdbc:postgresql://localhost:5410/rikkaus`; stdout → `api.stdout` (251 dòng), stderr → `api.stderr` (0 dòng) |
| Lần 2 (JSON) | cùng jar và DB, **không profile**, `RIKKAUS_JWT_SECRET` đặt qua env (secret riêng cho lần này); stdout 41 dòng, stderr 0 dòng |
| Migration | Lần 1: `Migrating schema "public" to version "12 - feedback entries"`, context lên sau 3,26 s (`ddl-auto: validate` chấp nhận entity). Lần 2: `Current version of schema "public": 12`, `No migration necessary` |
| Users | Insert thẳng: A `0f2c8d14-…0c93` / `an.nguyen@example.com`, B `7b41e590-…a842` / `bich.tran@example.com` |
| Tokens | HS256 mint đúng claim của `AccessTokenService` (A, B, hết hạn, sai chữ ký); lần 2 mint lại bằng secret của env. Request đi qua `BearerTokenAuthenticationFilter` thật |
| Gate | `./services/api/mvnw -f services/api/pom.xml clean verify` xanh không flag trên `5958f95`: 81 unit test, 50 integration test |

Lần chạy đầu của script bị dừng sau 6 request (F01–F04) vì lỗi của chính script (bash 3.2 của macOS coi
mảng rỗng là unbound dưới `set -u`), không phải lỗi API. Bảng `wealth.feedback_entries` được `TRUNCATE`
rồi chạy lại toàn bộ từ F01. Sáu request đó vẫn có trong log lần 1 (6 dòng `request completed`, không
chứa mồi nào) và được tính vào số dòng ở O13.

## F. Feedback — tạo và liệt kê

| # | Kết quả thật | |
|---|---|---|
| F01 | `201`, `{"id":"3e1c2635-…292e","screen":"catalog/assets","category":"BUG","message":"Màn hình danh mục tài sản không hiển thị tỷ giá cập nhật 12/09/2026 sau khi lưu.","createdAt":"2026-11-14T11:59:19.613056Z"}`; không header `Location`, không `userId` | Pass |
| F02 | `201`, `"category":null` | Pass |
| F03 | ba `201`, `IDEA` / `QUESTION` / `OTHER` | Pass |
| F04 | `201`, message `Nút Lưu bị che bởi bàn phím trên iPhone 13.` (hai đầu đã bỏ khoảng trắng) | Pass |
| F05 | `200`, 6 phần tử theo `createdAt` giảm dần: iPhone 13 → OTHER → QUESTION → IDEA → dashboard → F01; hai lần byte-identical | Pass |
| F06 | `200`, `[]` | Pass |
| F07 | B tạo `201`; B liệt kê 1 phần tử ("Biểu đồ tài sản ròng bị trống khi chưa có công nợ."); A vẫn 6 | Pass |
| F08 | `201`; P02 cho thấy A có 10, B có 1 — `userId` trong body bị bỏ qua | Pass |
| F09 | `201`, `id` `2b9559a6-…` (không phải `aaaaaaaa-…`), `createdAt` `2026-11-14T11:59:19.868925Z` (không phải 2020) | Pass |
| F10 | `201`, message 2000 ký tự | Pass |
| F11 | `201`, screen 64 ký tự | Pass |

## V. Feedback — validation

| # | `type` / `errors` thật | |
|---|---|---|
| F12 | `validation-failed`, `[{"field":"screen","message":"must not be blank"}]` | Pass |
| F13 | `[{"field":"message","message":"must not be blank"}]` | Pass |
| F14 | `[{"field":"message","message":"must not be blank"}]` | Pass |
| F15 | `[{"field":"message","message":"must not be blank"}]` | Pass |
| F16 | `[{"field":"message","message":"must not be blank"}]` (chỉ một `U+00A0`) | Pass |
| F17 | `[{"field":"message","message":"must be at most 2000 characters"}]` | Pass |
| F18 | `[{"field":"message","message":"must not contain a null character"}]`, không 500 | Pass |
| F19 | `[{"field":"screen","message":"must be a lowercase screen code such as dashboard or catalog/assets"}]` | Pass |
| F20 | `[{"field":"screen","message":"must be at most 64 characters"}]` | Pass |
| F21 | thông báo pattern; body không echo `dashboard?token=…` | Pass |
| F22 | thông báo pattern | Pass |
| F23 | `malformed-request`, `"detail":"Malformed request."`, không `errors` | Pass |
| F24 | `malformed-request` | Pass |
| F25 | hai phần tử: `screen` (pattern) và `message` (`must not be blank`) | Pass |

## H. Feedback — xác thực, method, định dạng

| # | Kết quả thật | |
|---|---|---|
| F26–F28 | ba `401`, bỏ `correlationId` thì cả ba là `{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized"}` | Pass |
| F29 | `401`, cùng body | Pass |
| F30 | `PUT`, `DELETE`, `PATCH` → `405 method-not-allowed`, `Allow: POST, GET` | Pass |
| F31 | `GET /feedback/3e1c2635-…` → `404 not-found`, `"detail":"Resource not found."` | Pass |
| F32 | `DELETE /feedback/3e1c2635-…` → `404`; P02b: row vẫn còn (`count = 1`) | Pass |
| F33 | `415 unsupported-media-type` | Pass |
| F34 | `400 malformed-request` | Pass |
| F35 | `400 malformed-request` | Pass |
| F36 | `406 not-acceptable` | Pass |

## P. PostgreSQL

| # | Kết quả thật | |
|---|---|---|
| P01 | `id uuid not null`, `user_id uuid not null`, `screen text not null`, `category text`, `message text not null`, `created_at timestamp with time zone not null`; index `idx_feedback_entries_owner_created_at (user_id, created_at DESC)`; 4 CHECK; FK `ON DELETE CASCADE` | Pass |
| P02 | A `10`, B `1` | Pass |
| P03 | `[Nút Lưu bị che bởi bàn phím trên iPhone 13.]` | Pass |
| P04 | `violates check constraint "feedback_entries_screen_is_a_code"` | Pass |
| P05 | `violates check constraint "feedback_entries_message_is_not_blank"` | Pass |
| P06 | `violates check constraint "feedback_entries_message_is_bounded"` | Pass |
| P07 | `violates check constraint "feedback_entries_category_is_known"` | Pass |
| P08 | `violates foreign key constraint "feedback_entries_user_id_fkey"` | Pass |

`psql` in kèm `DETAIL: Failing row contains (…)` cho P04–P07. Đó là đúng loại văn bản mà
`logServerErrorDetail=false` ngăn đi vào message exception của API; O05 bên dưới chứng minh nó không lọt
vào log.

## O. Log — lần 1, profile `local` (pattern)

| # | Response | Dòng log thật | |
|---|---|---|---|
| O01 | `201` | `INFO [issue110-local-o01] [0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93] c.r.w.s.o.RequestLoggingFilter - request completed POST /api/v1/assets 201 in 28 ms` | Pass |
| O02 | `400` | `… [issue110-local-o02] [0f2c8d14-…] … request completed POST /api/v1/assets 400 in 2 ms` | Pass |
| O03 | `404` | `… [issue110-local-o03] … request completed GET /api/v1/assets/{assetId} 404 in 5 ms` — template, không phải UUID | Pass |
| O04 | `201` | `… [issue110-local-o04] … request completed POST /api/v1/feedback 201 in 3 ms` | Pass |
| O05 | `500 internal-error`, body `{"detail":"Unexpected server error.",…,"correlationId":"issue110-local-o05"}` | xem trích đoạn dưới | Pass |
| O06 | `400 malformed-request` | `… request completed POST /api/v1/feedback 400 in 1 ms` | Pass |
| O07 | `401` | `INFO [issue110-local-o07] [no-user] … request completed GET unmatched 401 in 0 ms` | Pass |
| O08 | `200` | `… [issue110-local-o08] [0f2c8d14-…] … request completed GET /api/v1/feedback 200 in 3 ms` | Pass |
| O09 | `401` | `… [issue110-local-o09] [no-user] … GET unmatched 401 in 0 ms` | Pass |
| O10 | `200` | không có dòng nào cho health probe; chữ `actuator` chỉ xuất hiện 1 lần, ở dòng khởi động `Exposing 1 endpoint beneath base path '/actuator'` | Pass |
| O11 | `200`, header `X-Correlation-Id: d6df2cef-8658-4791-80ec-3fe00c18afe0` | dòng log dùng đúng UUID mới đó; chuỗi `bad id` có 0 dòng | Pass |

Trích đoạn O05 (`local`), nguyên văn trừ phần frame lặp được cắt:

```text
2026-11-14T18:59:20.934+07:00  WARN [issue110-local-o05] [0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93] org.hibernate.orm.jdbc.error - HHH000247: ErrorCode: 0, SQLState: 23514
2026-11-14T18:59:20.934+07:00  WARN [issue110-local-o05] [0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93] org.hibernate.orm.jdbc.error - ERROR: new row for relation "feedback_entries" violates check constraint "verification_rejects_thao_dien"
2026-11-14T18:59:20.937+07:00 ERROR [issue110-local-o05] [0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93] c.r.w.shared.error.ApiExceptionHandler - Unhandled exception while serving a request: org.springframework.dao.DataIntegrityViolationException
org.springframework.dao.DataIntegrityViolationException
	at org.springframework.orm.jpa.hibernate.HibernateExceptionTranslator.convertHibernateAccessException(HibernateExceptionTranslator.java:169)
	…
	... 100 more
Caused by: org.hibernate.exception.ConstraintViolationException
	…
	... 122 more
Caused by: org.postgresql.util.PSQLException
	…
	... 128 more
2026-11-14T18:59:20.940+07:00  INFO [issue110-local-o05] [0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93] c.r.w.s.o.RequestLoggingFilter - request completed POST /api/v1/feedback 500 in 9 ms
```

`verification_rejects_thao_dien` là tên constraint tạm do script đặt (ASCII), không phải dữ liệu của row.

**O12 — grep toàn bộ `api.stdout` + `api.stderr` (251 dòng):**

| Mồi | Số dòng |
|---|---|
| `ya29` / `fake-token-issue110` | 0 / 0 |
| access token thật của A (toàn chuỗi) / `eyJ` | 0 / 0 |
| `an.nguyen@example.com` / `bich.tran@example.com` / `@example.com` | 0 / 0 / 0 |
| `4.200.000.000` / `4200000000` | 0 / 0 |
| `Căn hộ Thảo Điền` / `Thảo Điền` | 0 / 0 |
| `session-cookie-issue110` / `Cookie` / `Bearer` | 0 / 0 / 0 |
| `Failing row` / `generated security password` | 0 / 0 |
| `[redacted]` | 0 — lưới redaction không phải kích hoạt lần nào: không có gì nhạy cảm tới được logger |

`Authorization` có 1 dòng: frame `at org.springframework.security.web.access.intercept.AuthorizationFilter.doFilter(…)`
trong stack trace O05 — tên lớp, không phải header. Pass.

**O13:** 59 dòng `request completed` = 6 (lần script bị dừng) + 43 (F01–F36) + 10 (O01–O09, O11); O10 không
có dòng. Mỗi dòng đủ correlationId, userId hoặc `no-user`, method, route, status, duration. Pass.

## J. Log — lần 2, không profile (JSON)

| # | Kết quả thật | |
|---|---|---|
| J01 | O01–O11 lặp lại: `201, 400, 404, 201, 500, 400, 401, 200, 401, 200 (health), 200` — giống lần 1 | Pass |
| J02 | `jq -c . api.stdout` không lỗi; 41 dòng file = 41 object parse được | Pass |
| J03 | 10 dòng `request completed`, mỗi dòng có `correlationId`, `httpMethod`, `httpRoute`, `httpStatus` (số), `durationMs` (số); 8 dòng có `userId`, 2 dòng 401 không có | Pass |
| J04 | `stack_trace` của O05 chỉ gồm `org.springframework.dao.DataIntegrityViolationException`, `Caused by: org.hibernate.exception.ConstraintViolationException`, `Caused by: org.postgresql.util.PSQLException` và các dòng `\tat …` / `... N more` | Pass |
| J05 | grep cùng bộ mồi trên stdout + stderr (kể cả dạng escape `Thảo`) và trên `parsed.jsonl` đã giải mã Unicode: tất cả 0; secret của env (`issue-110-json-run-only`) cũng 0 | Pass |
| J06 | O04 ở hai định dạng mang cùng thông tin (bên dưới) | Pass |
| J07 | dòng đầu stdout là object JSON `Starting RikkausWealthApplication …`, không banner; Flyway `Successfully validated 9 migrations`, `Current version of schema "public": 12` và `Started RikkausWealthApplication in 2.804 seconds` đều là object JSON | Pass |

J06, nguyên văn:

```text
2026-11-14T18:59:20.869+07:00  INFO [issue110-local-o04] [0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93] c.r.w.s.o.RequestLoggingFilter - request completed POST /api/v1/feedback 201 in 3 ms
```

```json
{"@timestamp":"2026-11-14T19:00:16.141973+07:00","@version":"1","message":"request completed POST /api/v1/feedback 201 in 12 ms","logger_name":"com.rikkaus.wealth.shared.observability.RequestLoggingFilter","thread_name":"http-nio-8110-exec-7","level":"INFO","level_value":20000,"userId":"0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93","correlationId":"issue110-json-o04","httpMethod":"POST","httpRoute":"/api/v1/feedback","httpStatus":201,"durationMs":12}
```

Tập field của 41 dòng JSON: 28 dòng khởi động và khởi tạo servlet (14 trong đó có thêm `tags:
["COMMONS-LOGGING"]`, marker Spring gắn cho log đi qua commons-logging), 10 dòng request, 2 dòng WARN của
Hibernate và 1 dòng ERROR — 13 dòng sau cùng mang `correlationId` và (trừ hai 401) `userId`.

## Phát hiện trước lần chạy

Cả hai được tìm thấy trong lúc smoke test khi đang code, đã sửa trong `5958f95`, và lần chạy scenario ở
trên xác nhận đã hết:

1. **Spring Security in mật khẩu sinh tự động ra log lúc khởi động.** `UserDetailsServiceAutoConfiguration`
   ghi `Using generated security password: …` ở WARN mỗi lần start — một credential trong log, có sẵn trên
   `main` từ trước task này. API không có đường xác thực bằng username/password nào, nên auto-configuration
   đó được exclude trong `application.yml`. O12/J05: `generated security password` 0 dòng.
2. **Thêm controller có method `list` làm lệch `operationId` của resource khác** (`list_2` → `list_3` …)
   trong `openapi.json`. Đổi tên handler thành `listFeedback`/`sendFeedback`; diff contract cuối chỉ còn phần
   thêm của feedback.

## Dọn dẹp

Cả hai tiến trình API đã dừng (`kill` đúng PID nghe cổng 8110 do session này khởi động), container
`rikkaus-pg-issue-110` đã `docker rm -f`.
