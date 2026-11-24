# Manual verification — issue #107 backend support for responsive and accessibility work

**Kết quả: 14/14 kịch bản fixture (F), 7/7 kịch bản danh sách (L), 11/11 nhóm lỗi (E) và 4/4 kịch bản
OpenAPI (O) đã chạy trên API thật; không có lỗi chặn UI, nên không sửa Java.** Mọi list trả đủ bản ghi,
ổn định từng byte, bỏ qua `page`/`limit`/`offset`/`size`; không request nào trả 5xx; 404 của id người
khác giống từng byte 404 của id không tồn tại trên 15 route. Bốn phát hiện không chặn UI được ghi ở
*Phát hiện* (ba chuyển cho #98, một câu hỏi PO về collation). `shellcheck` không có trên máy nên chỉ chạy
`bash -n` (xem *Khoảng trống*).

## Môi trường

| Mục | Giá trị |
|---|---|
| Ngày chạy | 24/11/2026, giờ `Asia/Saigon` |
| Code | `main` `7d1b1b8` (nhánh không đổi Java), jar đóng gói `-DskipTests` rồi chép ra scratchpad |
| Database | Container `rikkaus-pg-107`, `postgres:18.6-alpine3.24` (PostgreSQL 18.6 aarch64), `127.0.0.1:5407`, không volume, mật khẩu sinh bằng `openssl rand -hex 32` và chỉ truyền qua biến môi trường |
| API | `--spring.profiles.active=local`, `SERVER_PORT=8107`, `DB_URL=jdbc:postgresql://127.0.0.1:5407/rikkaus`; Flyway `Successfully applied 11 migrations ... now at version v14` |
| Tài khoản | `an.nguyen@example.com` (baseline), `long.list@example.com` (danh sách dài), `bich.tran@example.com` (ownership), `reset.probe@example.com`, `probe.f14@example.com`; tất cả qua `POST /auth/dev-sign-in` |
| Token | Chỉ nằm trong file header `0600` trong thư mục tạm `0700`, xoá khi script thoát; không có trong report, log script hay log API (`grep eyJ` = 0) |
| Lệnh | `services/api/fixtures/*.sh`, `plans/261124-0958-GH-107-a11y-backend-support/audit-lists.sh`, `audit-errors.sh` |

## F — Script fixture

| ID | Lệnh | Kết quả | Pass |
|---|---|---|---|
| F01 | `bash -n lib.sh reset.sh load-baseline.sh load-long-lists.sh audit-lists.sh audit-errors.sh` | Không lỗi. `shellcheck`: không cài trên máy | PASS (một phần) |
| F02 | `./reset.sh` | `assets 0, liabilities 0, cash-flow entries 0, goals 0`, exit 0, 0,8 s | PASS |
| F03 | `./load-baseline.sh` | `total assets 12002000000.00, total liabilities 1532000000.00, net worth 10470000000.00`, `complete: true`, tỷ giá `25100.000000` ngày `2026-09-12`, exit 0, 1,1 s | PASS |
| F04 | `GET /assets`, `valuationDate < hôm nay − 30` | Đúng 1: `Căn hộ Thảo Điền` `2026-09-10` | PASS |
| F05 | `upcomingLiabilityDues.items` | Đúng 1: `Vay mua nhà Vietcombank`, `PAYMENT`, `2026-12-06`, `overdue: false`, `18750000.00` | PASS |
| F06 | `GET /goal-projections` | `Quỹ du học cho con` `AT_RISK`, coverage `89.86`; `Quỹ dự phòng 6 tháng chi tiêu` `ON_TRACK`, `129.67` | PASS |
| F07 | `GET /cash-flow-reports/monthly?month=2026-11` | `income 180000000.00`, `expense 92000000.00`, `net 88000000.00`, `complete: true` | PASS |
| F08 | `./load-baseline.sh` lần hai, so kết quả kiểm tra | Giống hệt lần một (bỏ qua id) | PASS |
| F09 | `./load-long-lists.sh` | `assets 60, valuations 24, liabilities 40, cash-flow entries 120, goals 15, feedback 20`, exit 0, 6 s | PASS |
| F10 | `./load-long-lists.sh` lần hai | Cùng số đếm; feedback vẫn 20 | PASS |
| F11 | `./load-long-lists.sh reset.probe@example.com && ./reset.sh reset.probe@example.com` | Bốn catalog về 0; script in rõ tỷ giá, onboarding, feedback được giữ | PASS |
| F12 | `grep eyJ` trên mọi output; `TMPDIR` riêng rồi đếm thư mục còn lại | 0 lần; 0 thư mục còn lại sau khi chạy thành công và sau khi lỗi | PASS |
| F13 | `API_BASE_URL=http://localhost:8199 ./reset.sh` | Exit 1, `Development sign-in at http://localhost:8199/api/v1/auth/dev-sign-in answered HTTP 000.` và nhắc profile `local` | PASS |
| F14 | `api POST /assets` với `currentValue: -5` | Exit 1, in `POST /assets answered HTTP 400:` và body `validation-failed` `currentValue` (không token) | PASS |

Baseline đọc lại sau khi chạy (F03–F07):

```text
{"asOfDate":"2026-11-24","complete":true,"totalAssets":12002000000.00,"totalLiabilities":1532000000.00,"netWorth":10470000000.00,"liquid":3302000000.00,"rate":{"usdToVndRate":25100.000000,"effectiveDate":"2026-09-12"},"dues":[{"name":"Vay mua nhà Vietcombank","dueKind":"PAYMENT","dueDate":"2026-12-06","overdue":false,"amountVnd":18750000.00}],"byCategory":[{"category":"REAL_ESTATE","amountVnd":7500000000.00},{"category":"LISTED_SECURITIES","amountVnd":2150000000.00},{"category":"TERM_DEPOSIT","amountVnd":1200000000.00},{"category":"BANK_ACCOUNT","amountVnd":1152000000.00}]}
{"assets":5,"stale":[{"name":"Căn hộ Thảo Điền","valuationDate":"2026-09-10"}]}
[{"goalName":"Quỹ dự phòng 6 tháng chi tiêu","status":"ON_TRACK","requiredMonthlyContribution":16889000.00,"contributionCoveragePercent":129.67},{"goalName":"Quỹ du học cho con","status":"AT_RISK","requiredMonthlyContribution":37057000.00,"contributionCoveragePercent":89.86}]
{"month":"2026-11","from":"2026-11-01","to":"2026-11-30","currency":"VND","exchangeRate":{"usdToVndRate":25100.000000,"effectiveDate":"2026-09-12"},"complete":true,"totals":{"income":180000000.00,"expense":92000000.00,"net":88000000.00},"unconverted":{"occurrenceCount":0,"income":0.00,"expense":0.00},"passiveIncome":{"total":0.00,"sharePercentOfIncome":0.0,"byAsset":[],"unassigned":{"amount":0.00,"occurrenceCount":0,"partial":false}}}

{"status":"COMPLETED","currentStep":7,"steps":[true,true,true,true,true,true,true]}
counts: assets 5 liabilities 2 entries 4 goals 2
```

## L — Danh sách (bộ danh sách dài, `audit-lists.sh`)

Tất cả PASS: L01 đủ số bản ghi, L02 mọi route dưới 25 ms, L03 ổn định hai lần, L04 tham số phân trang bị bỏ qua, L05 mọi `sort` ổn định, L06 thứ tự mặc định đúng docs, L07 tên 120 ký tự trả nguyên vẹn. Output nguyên văn:

| Route | Status | Elements | Bytes | Median s (5) | Max s (5) | Stable x2 | page/limit/offset/size ignored |
|---|---|---|---|---|---|---|---|
| `/assets` | 200 | 60 | 32454 | 0.005079 | 0.006185 | yes | yes |
| `/assets/{id\}/valuations` | 200 | 24 | 8562 | 0.004684 | 0.006811 | yes | yes |
| `/liabilities` | 200 | 40 | 20408 | 0.004337 | 0.005462 | yes | yes |
| `/cash-flow-entries` | 200 | 120 | 52394 | 0.004705 | 0.006050 | yes | yes |
| `/cash-flow-occurrences?from=2026-11-01&to=2027-11-24` | 200 | 367 | 86113 | 0.005446 | 0.008969 | yes | yes |
| `/goals` | 200 | 15 | 6435 | 0.003976 | 0.005626 | yes | yes |
| `/goal-projections` | 200 | 15 | 53953 | 0.005268 | 0.006273 | yes | yes |
| `/feedback` | 200 | 20 | 6554 | 0.004010 | 0.005232 | yes | yes |
| `/wealth-snapshot` | 200 | "object" | 63844 | 0.006272 | 0.010695 | yes | yes |
| `/cash-flow-forecast` | 200 | "object" | 36023 | 0.007648 | 0.020513 | yes | yes |
| `/cash-flow-reports/monthly?month=2026-11` | 200 | "object" | 14811 | 0.005233 | 0.006275 | yes | yes |

| Sort | Elements | Stable x2 | First three names |
|---|---|---|---|
| `/assets?sort=-valuationDate` | 60 | yes | Tiền mặt dự phòng để ở két s / Căn hộ chung cư cao cấp tầng / Tiền mặt |
| `/assets?sort=valuationDate` | 60 | yes | Nhà phố ba tầng mặt tiền đườ / Chứng chỉ quỹ mở cổ phiếu tă / Trái phiếu Chính phủ Việt Na |
| `/assets?sort=name` | 60 | yes | Bộ sưu tập tranh sơn mài và  / Bộ sưu tập tranh sơn mài và  / Bộ sưu tập tranh sơn mài và  |
| `/assets?sort=-name` | 60 | yes | Đất nền Long An / Ô tô Kia Morning / Xe ô tô Toyota Corolla Cross |
| `/liabilities?sort=nextPaymentDate` | 40 | yes | Tiền hụi tháng còn phải đóng / Vay mua nhà Ngân hàng TMCP Đ / Khoản mượn anh Hai để sửa nh |
| `/liabilities?sort=-nextPaymentDate` | 40 | yes | Khoản mượn anh Hai để sửa nh / Vay ký quỹ Chứng khoán VNDir / Dư nợ thẻ tín dụng Techcomba |
| `/liabilities?sort=name` | 40 | yes | Bảo lãnh khoản vay kinh doan / Bảo lãnh khoản vay kinh doan / Bảo lãnh khoản vay kinh doan |
| `/liabilities?sort=-name` | 40 | yes | Vay mua nhà Ngân hàng TMCP Đ / Vay mua nhà Ngân hàng TMCP Đ / Vay mua nhà Ngân hàng TMCP Đ |
| `/cash-flow-entries?sort=-effectiveDate` | 120 | yes | — / — / Lương tháng và phụ cấp trách |
| `/cash-flow-entries?sort=effectiveDate` | 120 | yes | Chi tiêu đi chợ, điện nước,  / Phí bảo hiểm sức khỏe cao cấ / Học phí học kỳ một và tiền x |
| `/goals?sort=targetDate` | 15 | yes | Quỹ dự phòng sáu tháng chi t / Mua căn hộ ba phòng ngủ gần  / Quỹ du học thạc sĩ ngành kỹ  |
| `/goals?sort=-targetDate` | 15 | yes | Quỹ dự phòng sáu tháng chi t / Chuyến du lịch Nhật Bản mùa  / Trả hết khoản vay mua nhà tr |
| `/goals?sort=name` | 15 | yes | Chuyến du lịch Nhật Bản mùa  / Chuyến du lịch Nhật Bản mùa  / Mua căn hộ ba phòng ngủ gần  |
| `/goals?sort=-name` | 15 | yes | Tăng tài sản ròng lên hai mư / Tăng tài sản ròng lên hai mư / Trả hết khoản vay mua nhà tr |


```text
Ties (identical name and date, ordered by id):
[{"name":"Tiền mặt","valuationDate":"2026-11-19","id":"b7e1e4c4-2902-48a0-aedf-55eae4e28ac4"},{"name":"Tiền mặt","valuationDate":"2026-11-19","id":"f9277736-e212-4178-bb0b-aafff9e38bd9"},{"name":"Vàng miếng SJC 1 lượng","valuationDate":"2026-11-19","id":"c21fc546-56e5-41d0-9227-47878640703b"},{"name":"Vàng miếng SJC 1 lượng","valuationDate":"2026-11-19","id":"eb37db2e-3dd2-499e-8c45-2c88dfbc2860"}]
Default orders:
{"sortedDesc":true}
{"nullsLast":true,"datesAscending":true}
{"sortedDesc":true}
{"sortedAsc":true}
{"newestFirst":true}
Names around D and Đ under sort=name:
["Danh mục chứng chỉ quỹ mở cân bằng – lịch sử 24 tháng","Danh mục cổ phiếu ngành ngân hàng và bất động sản niêm yết trên sàn HOSE – hồ sơ số 15","Danh mục cổ phiếu ngành ngân hàng và bất động sản niêm yết trên sàn HOSE – hồ sơ số 26","Danh mục cổ phiếu ngành ngân hàng và bất động sản niêm yết trên sàn HOSE – hồ sơ số 37","Danh mục cổ phiếu ngành ngân hàng và bất động sản niêm yết trên sàn HOSE – hồ sơ số 4","Danh mục cổ phiếu ngành ngân hàng và bất động sản niêm yết trên sàn HOSE – hồ sơ số 48","Dat nen Long An khong dau","Đất nền Long An"]
Longest name returned (characters, NFC as sent):
120
```

## E — Lỗi (`audit-errors.sh`, sau `load-baseline.sh`)

Tất cả PASS theo nghĩa của kịch bản: mọi body là `application/problem+json`, đủ `type`/`title`/`status`/`detail`/`instance`/`correlationId` (trừ 401 thiếu `instance`, phát hiện 4), `errors[].field` đúng tên trường request, 0 response 5xx (E11). E05 ghi nhận `malformed-request` không có `field` (phát hiện 2). Cột thứ năm: `all5` = có đủ năm trường chuẩn và `correlationId`.

| ID | Request | Status | Content-Type | type \| type,title,status,detail,instance,correlationId \| errors[].field \| extra |
|---|---|---|---|---|
| E01 | `POST /assets` | 400 | application/problem+json | validation-failed | all5 | name,valuationDate,liquidity,currency,currentValue,category |  |
| E02 | `POST /assets` | 400 | application/problem+json | validation-failed | all5 | quantity |  |
| E03a | `POST /liabilities` | 400 | application/problem+json | validation-failed | all5 | maturityDate,recurringPaymentFrequency |  |
| E03b | `POST /cash-flow-entries` | 400 | application/problem+json | validation-failed | all5 | amount |  |
| E03c | `POST /goals` | 400 | application/problem+json | validation-failed | all5 | expectedAnnualReturnPercent |  |
| E03d | `POST /assets/34a99cf4-d3ae-4467-b4ad-1f17fd46aae2/valuations` | 400 | application/problem+json | validation-failed | all5 | value |  |
| E03e | `POST /feedback` | 400 | application/problem+json | validation-failed | all5 | screen,message |  |
| E03f | `PUT /settings/exchange-rate` | 400 | application/problem+json | validation-failed | all5 | usdToVndRate |  |
| E03g | `PUT /profile` | 400 | application/problem+json | validation-failed | all5 | displayName |  |
| E04a | `POST /assets` | 400 | application/problem+json | validation-failed | all5 | name,notes,currentValue |  |
| E04b | `POST /assets` | 400 | application/problem+json | validation-failed | all5 | currentValue |  |
| E05a | `POST /assets` | 400 | application/problem+json | malformed-request | all5 | - |  |
| E05b | `POST /assets` | 400 | application/problem+json | malformed-request | all5 | - |  |
| E05c | `POST /assets` | 400 | application/problem+json | malformed-request | all5 | - |  |
| E05d | `POST /assets` | 400 | application/problem+json | malformed-request | all5 | - |  |
| E05e | `PATCH /onboarding` | 400 | application/problem+json | malformed-request | all5 | - |  |
| E06a | `GET /assets?sort=bogus&category=cash&liquidity=high` | 400 | application/problem+json | validation-failed | all5 | category,liquidity,sort |  |
| E06b | `GET /assets?q=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa` | 400 | application/problem+json | validation-failed | all5 | q |  |
| E06c | `GET /cash-flow-entries?effectiveFrom=2026-13-01&effectiveTo=2026-01-01` | 400 | application/problem+json | validation-failed | all5 | effectiveFrom |  |
| E06d | `GET /cash-flow-entries?effectiveFrom=2026-06-01&effectiveTo=2026-01-01` | 400 | application/problem+json | validation-failed | all5 | effectiveTo |  |
| E06e | `GET /cash-flow-occurrences?from=2026-12-01&to=2026-01-01` | 400 | application/problem+json | validation-failed | all5 | to |  |
| E06f | `GET /cash-flow-reports/monthly?month=2026-13` | 400 | application/problem+json | validation-failed | all5 | month |  |
| E06g | `GET /cash-flow-forecast?from=hom-nay` | 400 | application/problem+json | validation-failed | all5 | from |  |
| E06h | `DELETE /assets/34a99cf4-d3ae-4467-b4ad-1f17fd46aae2?confirm=maybe` | 400 | application/problem+json | malformed-request | all5 | - |  |
| E06i | `GET /cash-flow-occurrences` | 400 | application/problem+json | validation-failed | all5 | from,to |  |
| E08 | `DELETE /assets/34a99cf4-d3ae-4467-b4ad-1f17fd46aae2` | 409 | application/problem+json | linked-records | all5 | - | impact.confirmationRequired=true |
| E09a | `GET /assets` | 401 | application/problem+json | unauthorized | MISSING | - |  |
| E09b | `PATCH /assets` | 405 | application/problem+json | method-not-allowed | all5 | - |  |
| E09c | `GET /assets` | 406 | application/problem+json | not-acceptable | all5 | - |  |
| E09d | `POST /assets` | 400 | application/problem+json | malformed-request | all5 | - |  |
| E09e | `GET /khong-ton-tai` | 404 | application/problem+json | not-found | all5 | - |  |
| E09f | `GET /assets/khong-phai-uuid` | 400 | application/problem+json | malformed-request | all5 | - |  |

E07 — another user's id against an id that exists nowhere (instance id replaced by {id}):

| Request | Other user's | Missing | Identical |
|---|---|---|---|
| `GET /assets/{id}` | 404 | 404 | yes, 212 bytes |
| `PUT /assets/{id}` | 404 | 404 | yes, 212 bytes |
| `DELETE /assets/{id}?confirm=true` | 404 | 404 | yes, 212 bytes |
| `GET /assets/{id}/deletion-impact` | 404 | 404 | yes, 228 bytes |
| `GET /assets/{id}/valuations` | 404 | 404 | yes, 223 bytes |
| `POST /assets/{id}/valuations` | 404 | 404 | yes, 223 bytes |
| `GET /liabilities/{id}` | 404 | 404 | yes, 217 bytes |
| `PUT /liabilities/{id}` | 404 | 404 | yes, 217 bytes |
| `DELETE /liabilities/{id}?confirm=true` | 404 | 404 | yes, 217 bytes |
| `GET /liabilities/{id}/deletion-impact` | 404 | 404 | yes, 233 bytes |
| `GET /cash-flow-entries/{id}` | 404 | 404 | yes, 223 bytes |
| `DELETE /cash-flow-entries/{id}` | 404 | 404 | yes, 223 bytes |
| `GET /goals/{id}` | 404 | 404 | yes, 211 bytes |
| `DELETE /goals/{id}` | 404 | 404 | yes, 211 bytes |
| `GET /goal-projections/{id}` | 404 | 404 | yes, 222 bytes |

E10 — correlation and CORS from the Expo web origin (PASS):

```text
HTTP/1.1 404 
X-Correlation-Id: audit-gh107-cors
Access-Control-Allow-Origin: http://localhost:8081
Access-Control-Expose-Headers: X-Correlation-Id
Content-Type: application/problem+json
{"correlationId":"audit-gh107-cors"}
Preflight:
HTTP/1.1 200 
Access-Control-Allow-Origin: http://localhost:8081
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: authorization, content-type, x-correlation-id
```

Lỗi liên trường, body không đọc được và 401 (phát hiện 1, 2 và 4; `"instance":null` là cách `jq` hiển thị trường vắng mặt, body thật không có khoá `instance`):

```text
Phase 2 only (every field-level constraint satisfied):
POST /cash-flow-entries -> {"status":400,"type":"validation-failed","errors":[{"message":"must belong to the entry type","field":"category"},{"message":"must be omitted for a one-time entry","field":"recurrence"},{"message":"must be omitted for a one-time entry","field":"endDate"},{"message":"must not be combined with linkedAssetId","field":"linkedLiabilityId"}]}
POST /goals -> {"status":400,"type":"validation-failed","errors":[{"message":"must not exceed the target amount","field":"currentAllocatedAmount"},{"message":"must be after today","field":"targetDate"}]}
POST /assets/34a99cf4-d3ae-4467-b4ad-1f17fd46aae2/valuations -> {"status":400,"type":"validation-failed","errors":[{"message":"must match the asset's currency","field":"currency"},{"message":"must not be in the future","field":"effectiveDate"},{"message":"must be given together with usdToVndRate","field":"rateEffectiveDate"}]}
POST /assets -> {"status":400,"type":"validation-failed","errors":[{"message":"must be given together with usdToVndRate","field":"rateEffectiveDate"},{"message":"must not be set for this asset category","field":"symbol"}]}
Mixed (one field-level error plus the same cross-field errors):
POST /goals -> {"status":400,"type":"validation-failed","errors":[{"message":"must be between 0 and 30","field":"expectedAnnualReturnPercent"}]}
Malformed body details:
{"status":400,"type":"urn:rikkaus:problem:malformed-request","detail":"Malformed request.","errors":null}
{"status":400,"type":"urn:rikkaus:problem:malformed-request","detail":"Malformed request.","errors":null}
401 without a token:
{"status":401,"type":"urn:rikkaus:problem:unauthorized","title":"Authentication required","detail":"Authentication required.","instance":null,"correlationId":"audit-gh107-anon"}
HTTP/1.1 401 
Content-Type: application/problem+json
```

## O — OpenAPI cho thuộc tính form

| ID | Kiểm tra | Kết quả | Pass |
|---|---|---|---|
| O01 | 85 thuộc tính của 11 request schema (trừ Google/refresh) | Mọi chuỗi có `@Size(max)` đều có `maxLength` (120 tên, 160 `institution`, 32 `symbol`, 2000 ghi chú, 64 `screen`) | PASS |
| O02 | 27 thuộc tính số | Cả 27 có `minimum` hoặc `exclusiveMinimum`; `interestRate` tài sản `maximum 100`, `expectedAnnualReturnPercent` `maximum 30`, `currentStep` 1–7 | PASS |
| O03 | Cái không được công bố | 26 `@Digits` không có trong schema; 7 trường bắt buộc không trống công bố `minLength: 0`; quy tắc ngày (không ở tương lai, sau hôm nay) và liên trường chỉ có trong `description` | Ghi nhận (phát hiện 3) |
| O04 | Tham số truy vấn | `q` có `maxLength: 120`; mọi filter enum có `items.enum`; `sort` có enum khóa | PASS |

## P — Kết luận phân trang

**Pilot không cần phân trang.** Danh sách lớn nhất đo được là 367 occurrence một năm, 86 KB, trung vị
5 ms, chậm nhất 9 ms; `wealth-snapshot` với 60 tài sản và 40 khoản nợ là 64 KB, chậm nhất 11 ms. Đó là
gấp nhiều lần quy mô pilot ("hàng chục bản ghi mỗi người"). Không thêm pagination; tham số phân trang
lạ bị bỏ qua đúng như `docs/api-contract-conventions.md` nói. Ngưỡng nên xem lại nằm trong câu hỏi PO.

## Phát hiện

Không phát hiện nào chặn flow responsive/accessibility, nên ticket này không sửa Java.

1. **Lỗi theo trường che lỗi liên trường (chuyển #98).** Khi request vừa vi phạm ràng buộc một trường
   vừa vi phạm quy tắc liên trường, chỉ lỗi một trường được trả. `POST /goals` với
   `expectedAnnualReturnPercent: 31`, `currentAllocatedAmount > targetAmount`, `targetDate` quá khứ trả
   một lỗi `expectedAnnualReturnPercent`; bỏ lỗi đó thì trả hai lỗi `currentAllocatedAmount`,
   `targetDate`. Tương tự cash-flow (`amount: 0` che bốn lỗi `category`, `recurrence`, `endDate`,
   `linkedLiabilityId`), valuation (`value: -1` che `currency`, `effectiveDate`, `rateEffectiveDate`) và
   asset (`quantity: 0` che `symbol`, `rateEffectiveDate`). Form cần hai lượt gửi để đánh dấu đủ lỗi.
2. **`malformed-request` không có `errors[].field` (chuyển #98).** Ngày `2026-13-45` hoặc `12/09/2026`,
   enum `cash`, `currentValue: "một triệu"` trong body đều trả `malformed-request` không chỉ ra trường.
   Form dùng date picker, select và ô số không gặp; ô ngày gõ tay sẽ gặp và chỉ hiện được lỗi chung.
3. **OpenAPI thiếu gợi ý cho form (chuyển #98).** 26 annotation `@Digits` trong 8 request record không
   xuất hiện trong schema (UI không biết `currentValue` nhận 2 chữ số thập phân, `usdToVndRate` 6); 7
   trường bắt buộc không được trống (`name` ở bốn request, `displayName`, `screen`, `message`) công bố
   `minLength: 0`. `maxLength`, `minimum`/`exclusiveMinimum`/`maximum`, `enum`, `required`, `pattern`
   đều đủ.
4. **401 không có `instance` (chuyển #98, phần `shared/error`/security entry point).** Body 401 có
   `type`, `title`, `status`, `detail`, `correlationId` nhưng thiếu `instance`; mọi status khác có đủ.
   Client hiện không dùng `instance`, nên không chặn UI.
5. **Thứ tự `sort=name` theo collation của database (câu hỏi PO).** Trên image `postgres:18.6-alpine3.24`
   (musl, `datcollate = en_US.utf8`, provider `libc`) thứ tự là code point: `Dat < Oanh < Xe < Zeta <
   Ô tô < Đất`. Collation ICU `vi-x-icu` có sẵn và cho `Dat < Đất < Oanh < Ô tô < Xe < Zeta`. Snapshot
   lại sắp theo `Collator` `vi-VN`, nên hai màn hình có thể xếp khác nhau. Ổn định từng byte, không chặn
   accessibility; sửa nằm ở vùng sort của #101, không thuộc ticket này.

## Khoảng trống

- `shellcheck` không cài trên máy; script chỉ qua `bash -n`. Không tự cài vì đó là tải phần mềm mới.
- Trường hợp "API chạy nhưng không có profile `local`" (F13) chỉ kiểm bằng API không chạy; theo
  `manual-test-guide-auth.md` route khi đó trả 401, và script coi mọi mã khác 200 là lỗi với cùng thông
  báo.
- Thời gian đo trên máy local (Mac mini, cùng máy với database), không phải môi trường AWS.

## Kiểm tra hồi quy

`JAVA_HOME=…openjdk@25… ./mvnw clean verify` trên nhánh (không đổi Java): `BUILD SUCCESS`, 81 unit
test và 50 integration test, 0 fail, gồm `OpenApiContractIT` (4), `ApplicationContextIT` (3),
`ArchitectureRulesTest` (4). `openapi.json` không đổi.
