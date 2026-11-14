# Manual verification — issue #95 guided onboarding checklist

## Kết quả

**60/60 case pass trên commit `bf72d18`** (`feat(api): add the guided onboarding checklist`), jar build từ
chính commit đó bởi `./mvnw clean verify` không flag (81 unit test, 50 integration test, `BUILD SUCCESS`).
Một case (O27) **đã fail** ở lần chạy đầu trên working tree và là một defect thật; nó được sửa trước khi
commit và pass ở cả hai lần chạy sau.

| Lần chạy | Code | Database | Kết quả |
|---|---|---|---|
| 1 | working tree trước khi sửa (base `d9abcd8`) | PostgreSQL mới, migrate tới V11 | 59 pass, **1 fail (O27)** |
| 2 | working tree sau khi sửa O27 | PostgreSQL mới | 60 pass |
| 3 | commit `bf72d18`, tree sạch | PostgreSQL mới | 60 pass |

Lần 2 và lần 3 cho cùng status và cùng body ở cả 60 case sau khi che id và timestamp; khác biệt duy nhất
là dòng log của O54 (thời điểm và thời gian chạy migration).

## Môi trường

- Ngày chạy: 14/11/2026, `TODAY = 2026-11-14` theo `Asia/Ho_Chi_Minh`.
- PostgreSQL `postgres:18.6-alpine3.24`, container `rikkaus-pg-issue-95`, `127.0.0.1:5495`, tạo mới cho
  mỗi lần chạy và xóa sau khi xong.
- API: `java -jar` profile `local`, cổng `8095`, JDK 25.
- User: A `0f2c8d14-…` (An Nguyễn), B `7b41e590-…` (Bích Trần), C `c3a9e1f0-…` (Cường Lê),
  D `d84f2b6e-…` (Dung Phạm), insert trực tiếp; access token mint bằng secret dev của profile `local`
  và đi qua `BearerTokenAuthenticationFilter` thật.
- Mỗi case được chạy bởi một script Python gửi HTTP thật và `docker exec psql`, tự chấm theo kết quả mong
  đợi của [phase 1](../261114-1843-GH-95-onboarding-checklist/phase-01-test-scenarios.md). Lệnh tay
  tương đương nằm trong [`docs/manual-test-guide-onboarding.md`](../../docs/manual-test-guide-onboarding.md).

Log khởi động lần 3 (O54):

```text
Migrating schema "public" to version "11 - onboarding progress"
Successfully applied 9 migrations to schema "public", now at version v11 (execution time 00:00.099s)
```

## Case đã fail: O27

Lần 1, `PATCH /api/v1/onboarding` với `{"currentStep":2.5}` trả **`200`** và lưu
`current_step = 2` (`db=2`):

```json
{"status":"IN_PROGRESS","currentStep":2,"steps":[{"step":1,"key":"EXCHANGE_RATE","done":true},{"step":2,"key":"CASH_AND_BANK_ACCOUNTS","done":true},{"step":3,"key":"MAJOR_ASSETS","done":false},{"step":4,"key":"LIABILITIES","done":false},{"step":5,"key":"RECURRING_CASH_FLOWS","done":false},{"step":6,"key":"DASHBOARD_REVIEW","done":false},{"step":7,"key":"FIRST_GOAL","done":false}],"dashboardReviewedAt":null,"completedAt":null,"dismissedAt":null}
```

Mong đợi là `400` và dòng giữ nguyên bước 3. Nguyên nhân: Jackson mặc định chấp nhận số thực cho một
`Integer` và cắt phần thập phân, nên client gửi một bước không tồn tại mà vẫn nhận `200`. Sửa bằng
`WholeNumberDeserializer` gắn riêng cho `currentStep` trong `UpdateOnboardingRequest`: chỉ nhận token số
nguyên JSON, mọi thứ khác là `malformed-request`. Không đổi cấu hình mapper dùng chung, vì đổi cách ép
kiểu cho mọi slice không phải quyết định của resource này. Assertion của O27 được siết từ "400" thành
"400 `malformed-request`" cho lần 2 và 3.

## Từng case (lần 3, commit `bf72d18`)

Cột body rút gọn: checklist được viết thành `steps=` bảy ký tự `T`/`F` theo thứ tự bước 1–7; lỗi được
viết thành status + `type` + `errors`; body của slice khác được cắt ở 150 ký tự. Hai status `a/b` là
request tạo/xóa record rồi `GET /onboarding`.

| # | Request | Status | Body (rút gọn) | Kết quả |
|---|---|---|---|---|
| O01 | GET /onboarding as A (new) | 200 | status=IN_PROGRESS currentStep=1 steps=FFFFFFF reviewed=null completed=null dismissed=null | PASS |
| O02 | psql count A | psql | 0 | PASS |
| O03 | GET /onboarding as A again | 200 | status=IN_PROGRESS currentStep=1 steps=FFFFFFF reviewed=null completed=null dismissed=null | PASS |
| O04 | GET /settings/exchange-rate then GET /onboarding | 200/200 | {"baseCurrency":"VND","usdToVndRate":null,"effectiveDate":null,"updatedAt":null} / status=IN_PROGRESS currentStep=1 steps=FFFFFFF reviewed=null completed=null dismissed=null | PASS |
| O05 | PUT /settings/exchange-rate 25100.000000 | 200/200 | {"baseCurrency":"VND","usdToVndRate":25100.000000,"effectiveDate":"2026-11-14","updatedAt":"2026-11-14T11:57:01.665958Z"} / status=IN_PROGRESS currentStep=1 steps=TFFFFFF reviewed=null completed=null dismissed=null | PASS |
| O06 | POST /assets Căn hộ Thảo Điền REAL_ESTATE 4.200.000.000 | 201/200 | {"id":"ea8bae96-99fe-4fd3-bb8a-53895f916c24","name":"Căn hộ Thảo Điền","category":"REAL_ESTATE","currentValue":4200000000.00,"currency":"VND","valuati… / status=IN_PROGRESS currentStep=1 steps=TFTFFFF reviewed=null completed=null dismissed=null | PASS |
| O07 | POST /assets Tài khoản Vietcombank BANK_ACCOUNT 85.000.000 | 201/200 | {"id":"97e37ab4-6cb0-411a-9424-c3fba689105c","name":"Tài khoản Vietcombank","category":"BANK_ACCOUNT","currentValue":85000000.00,"currency":"VND","val… / status=IN_PROGRESS currentStep=1 steps=TTTFFFF reviewed=null completed=null dismissed=null | PASS |
| O08 | POST /liabilities Vay mua nhà Vietcombank 2.400.000.000 | 201/200 | {"id":"13fd0049-e55a-4da0-a73f-1742538a5e5f","name":"Vay mua nhà Vietcombank","category":"BANK_LOAN","outstandingBalance":2400000000.00,"currency":"VN… / status=IN_PROGRESS currentStep=1 steps=TTTTFFF reviewed=null completed=null dismissed=null | PASS |
| O09 | POST /cash-flow-entries ONE_TIME 26.200.000 | 201/200 | {"id":"dc603f87-8a98-46de-bb86-0d0e6afaa2a5","entryType":"INCOME","category":"BUSINESS_INCOME","amount":26200000.00,"currency":"VND","timing":"ONE_TIM… / status=IN_PROGRESS currentStep=1 steps=TTTTFFF reviewed=null completed=null dismissed=null | PASS |
| O10 | POST /cash-flow-entries RECURRING MONTHLY 45.000.000 | 201/200 | {"id":"d3259290-c87b-44f3-9ef9-6f025d464658","entryType":"INCOME","category":"SALARY","amount":45000000.00,"currency":"VND","timing":"RECURRING","recu… / status=IN_PROGRESS currentStep=1 steps=TTTTTFF reviewed=null completed=null dismissed=null | PASS |
| O11 | POST /goals Quỹ dự phòng 6 tháng | 201/200 | {"id":"117a438d-01e8-40b5-9e15-34cd4a7c2665","name":"Quỹ dự phòng 6 tháng","type":"EMERGENCY_FUND","currency":"VND","targetAmount":270000000.00,"curre… / status=IN_PROGRESS currentStep=1 steps=TTTTTFT reviewed=null completed=null dismissed=null | PASS |
| O12 | GET /onboarding body has no amounts or names | 200 | status=IN_PROGRESS currentStep=1 steps=TTTTTFT reviewed=null completed=null dismissed=null | PASS |
| O13 | psql count A after records | psql | 0 | PASS |
| O14 | DELETE /assets/{Vietcombank} | 204/200 | status=IN_PROGRESS currentStep=1 steps=TFTTTFT reviewed=null completed=null dismissed=null | PASS |
| O15 | POST /assets Tiền mặt dự phòng CASH 12.500.000 | 201/200 | {"id":"616e30c1-824d-465d-9e43-fdbd5cbad528","name":"Tiền mặt dự phòng","category":"CASH","currentValue":12500000.00,"currency":"VND","valuationDate":… / status=IN_PROGRESS currentStep=1 steps=TTTTTFT reviewed=null completed=null dismissed=null | PASS |
| O16 | DELETE /liabilities/{vay} | 204/200 | status=IN_PROGRESS currentStep=1 steps=TTTFTFT reviewed=null completed=null dismissed=null | PASS |
| O17 | DELETE /cash-flow-entries/{lương} | 204/200 | status=IN_PROGRESS currentStep=1 steps=TTTFFFT reviewed=null completed=null dismissed=null | PASS |
| O18 | DELETE /goals/{quỹ} | 204/200 | status=IN_PROGRESS currentStep=1 steps=TTTFFFF reviewed=null completed=null dismissed=null | PASS |
| O19 | DELETE /assets/{Thảo Điền} | 204/200 | status=IN_PROGRESS currentStep=1 steps=TTFFFFF reviewed=null completed=null dismissed=null | PASS |
| O20 | PATCH {"currentStep":3} | 200 | status=IN_PROGRESS currentStep=3 steps=TTFFFFF reviewed=null completed=null dismissed=null | PASS |
| O21 | psql row of A | psql | 1\|3\|t | PASS |
| O22 | GET /onboarding resumes at step 3 | 200 | status=IN_PROGRESS currentStep=3 steps=TTFFFFF reviewed=null completed=null dismissed=null | PASS |
| O23 | PATCH {"currentStep":0} | 400 | 400 urn:rikkaus:problem:validation-failed errors=[{"field": "currentStep", "message": "must be between 1 and 7"}] | PASS |
| O24 | PATCH {"currentStep":8} | 400 | 400 urn:rikkaus:problem:validation-failed errors=[{"field": "currentStep", "message": "must be between 1 and 7"}] | PASS |
| O25 | PATCH {"currentStep":-1} | 400 | 400 urn:rikkaus:problem:validation-failed errors=[{"field": "currentStep", "message": "must be between 1 and 7"}] | PASS |
| O26 | PATCH {"currentStep":"ba"} | 400 | 400 urn:rikkaus:problem:malformed-request | PASS |
| O27 | PATCH {"currentStep":2.5} | 400 | 400 urn:rikkaus:problem:malformed-request | PASS |
| O28 | PATCH {"dashboardReviewed":true} | 200 | status=IN_PROGRESS currentStep=3 steps=TTFFFTF reviewed=2026-11-14T11:57:02.392679Z completed=null dismissed=null | PASS |
| O29 | PATCH {"dashboardReviewed":true} again | 200 | status=IN_PROGRESS currentStep=3 steps=TTFFFTF reviewed=2026-11-14T11:57:02.392679Z completed=null dismissed=null | PASS |
| O30 | PATCH {"dashboardReviewed":false} | 200 | status=IN_PROGRESS currentStep=3 steps=TTFFFFF reviewed=null completed=null dismissed=null | PASS |
| O31 | PATCH {"currentStep":7,"dashboardReviewed":true,"status":"COMPLETED"} | 200 | status=COMPLETED currentStep=7 steps=TTFFFTF reviewed=2026-11-14T11:57:03.527622Z completed=2026-11-14T11:57:03.527622Z dismissed=null | PASS |
| O32 | GET after completion | 200 | status=COMPLETED currentStep=7 steps=TTFFFTF reviewed=2026-11-14T11:57:03.527622Z completed=2026-11-14T11:57:03.527622Z dismissed=null | PASS |
| O33 | PATCH {"status":"COMPLETED"} again | 200 | status=COMPLETED currentStep=7 steps=TTFFFTF reviewed=2026-11-14T11:57:03.527622Z completed=2026-11-14T11:57:03.527622Z dismissed=null | PASS |
| O34 | PATCH {"status":"DISMISSED"} | 200 | status=DISMISSED currentStep=7 steps=TTFFFTF reviewed=2026-11-14T11:57:03.527622Z completed=null dismissed=2026-11-14T11:57:04.681008Z | PASS |
| O35 | PATCH {"status":"FINISHED"} | 400 | 400 urn:rikkaus:problem:malformed-request | PASS |
| O36 | PATCH {"currentStep":null} | 200 | status=DISMISSED currentStep=7 steps=TTFFFTF reviewed=2026-11-14T11:57:03.527622Z completed=null dismissed=2026-11-14T11:57:04.681008Z | PASS |
| O37 | PATCH {} as B | 200 | status=IN_PROGRESS currentStep=1 steps=FFFFFFF reviewed=null completed=null dismissed=null | PASS |
| O38 | GET /onboarding as B | 200 | status=IN_PROGRESS currentStep=1 steps=FFFFFFF reviewed=null completed=null dismissed=null | PASS |
| O39 | POST /assets as B Tiền gửi có kỳ hạn ACB TERM_DEPOSIT 300.000.000 | 201/200 | {"id":"a6da7487-35a9-475b-98e6-bc87dd76dea8","name":"Tiền gửi có kỳ hạn ACB","category":"TERM_DEPOSIT","currentValue":300000000.00,"currency":"VND","v… / status=IN_PROGRESS currentStep=1 steps=FFTFFFF reviewed=null completed=null dismissed=null | PASS |
| O40 | PATCH {"status":"DISMISSED"} as B | 200 | status=DISMISSED currentStep=1 steps=FFTFFFF reviewed=null completed=null dismissed=2026-11-14T11:57:04.792964Z | PASS |
| O41 | PATCH {"status":"IN_PROGRESS"} as B | 200 | status=IN_PROGRESS currentStep=1 steps=FFTFFFF reviewed=null completed=null dismissed=null | PASS |
| O42 | PATCH {"userId":"<A>","currentStep":4} as B | 200 | status=IN_PROGRESS currentStep=4 steps=FFTFFFF reviewed=null completed=null dismissed=null | PASS |
| O43 | psql rows of A and B | psql | 7b41e590-3c82-4f16-b0d7-59e1c4f3a842\|4\|t\|f / 0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93\|7\|t\|t | PASS |
| O44 | GET without Authorization | 401 | 401 urn:rikkaus:problem:unauthorized | PASS |
| O45 | GET with expired token | 401 | 401 urn:rikkaus:problem:unauthorized | PASS |
| O46 | GET with forged token | 401 | 401 urn:rikkaus:problem:unauthorized | PASS |
| O47 | PATCH {"currentStep":9} without Authorization | 401 | 401 urn:rikkaus:problem:unauthorized | PASS |
| O48 | PUT /onboarding | 405 | 405 urn:rikkaus:problem:method-not-allowed | PASS |
| O49 | POST and DELETE /onboarding | 405/405 | 405 urn:rikkaus:problem:method-not-allowed / 405 urn:rikkaus:problem:method-not-allowed | PASS |
| O50 | PATCH Content-Type text/plain | 415 | 415 urn:rikkaus:problem:unsupported-media-type | PASS |
| O51 | PATCH truncated {"currentStep": | 400 | 400 urn:rikkaus:problem:malformed-request | PASS |
| O52 | GET Accept: application/xml | 406 | 406 urn:rikkaus:problem:not-acceptable | PASS |
| O53 | 8 concurrent first PATCH as C | [200, 200, 200, 200, 200, 200, 200, 200] | 1\|2 | PASS |
| O54 | startup log: V11 applied, context started | log | 2026-11-14T18:56:58.382+07:00  INFO [no-correlation-id] o.f.core.internal.command.DbMigrate - Migrating schema "public" to version "11 - onboarding progress" / 2026-11-14T18:56:58.399+07:00  INFO [no-correlation-id] o.f.core.internal.command.DbMigrate - Successfully applied 9 migrations to schema "public", now at version v11 (execution time 00:00.099s) / 2026-11-14T18:57:00.873+07:00  INFO [no-correlation-id] c.r.wealth.RikkausWealthApplication - Started RikkausWealthApplication in 4.895 seconds (process running for 5.252) | PASS |
| O55 | columns of wealth.onboarding_progress | psql | user_id:uuid / current_step:smallint / dashboard_reviewed_at:timestamp with time zone / completed_at:timestamp with time zone / dismissed_at:timestamp with time zone / created_at:timestamp with time zone / updated_at:timestamp with time zone | PASS |
| O56 | SELECT * FROM wealth.onboarding_progress | psql | c3a9e1f0-4d2b-4b7a-8e65-1f0d9c2b7a51\|2\|\|\|\|2026-11-14 11:57:04.956068+00\|2026-11-14 11:57:04.958058+00 / 7b41e590-3c82-4f16-b0d7-59e1c4f3a842\|4\|\|\|\|2026-11-14 11:57:04.792964+00\|2026-11-14 11:57:04.845413+00 / 0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93\|7\|2026-11-14 11:57:03.527622+00\|\|2026-11-14 11:57:04.681008+00\|2026-11-14 11:57:02.047275+00\|2026-11-14 11:57:04.681008+00 | PASS |
| O57 | INSERT current_step = 8 | psql | ERROR:  new row for relation "onboarding_progress" violates check constraint "onboarding_progress_current_step_is_known" / DETAIL:  Failing row contains (d84f2b6e-9a13-4c5d-b7e0-2c6a1f9e3b72, 8, null, null, null, 2026-11-14 11:57:05.137363+00, 2026-11-14 11:57:05.137363+00). | PASS |
| O58 | UPDATE completed_at and dismissed_at together | psql | ERROR:  new row for relation "onboarding_progress" violates check constraint "onboarding_progress_completed_or_dismissed" / DETAIL:  Failing row contains (0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93, 7, 2026-11-14 11:57:03.527622+00, 2026-11-14 11:57:05.177026+00, 2026-11-14 11:57:05.177026+00, 2026-11-14 11:57:02.047275+00, 2026-11-14 11:57:04.681008+00). | PASS |
| O59 | DELETE user C cascades | psql | DELETE 1 / rows=0 | PASS |
| O60 | flyway_schema_history V11 | psql | 11\|t | PASS |

## Những gì không được kiểm chứng ở đây

- Không có automated test nào chạm `/api/v1/onboarding` (chỉ đạo PO). Regression chỉ bị bắt khi chạy lại
  guide.
- Bước 1 không có case quay lại "chưa xong" vì settings không có route xóa tỷ giá.
- Chưa chạy trên môi trường deploy hay với app mobile; design #93 chưa duyệt nên copy và luồng client
  chưa được đối chiếu.
- Native query đọc tên cột và giá trị category của bốn slice khác; đổi tên ở các migration đó không gây
  compile error. Lần chạy này chỉ chứng minh khớp với schema V1–V9 hiện có trên `main` `d9abcd8`.
