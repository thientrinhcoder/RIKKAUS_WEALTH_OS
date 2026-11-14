# Manual verification — generic financial goals & templates (#86)

Ngày chạy: 14/11/2026, 17:55 ICT. Nhánh `thientrinhcoder/feat/issue-86-generic-financial-goals-templates`,
cắt từ `main` tại `6e57e92`. Lần chạy đầu thực hiện trên working tree chứa toàn bộ thay đổi của slice
trước khi commit; mục *Chạy lại trên commit cuối* ở cuối file ghi lần chạy xác nhận trên commit đã tạo.

Đây là bằng chứng hành vi cho issue #86. Product Owner chỉ đạo task này ship **không có unit test hay
integration test mới**; thay vào đó danh sách scenario được viết trước khi có dòng code nào
(`plans/261114-1740-GH-86-generic-financial-goals-templates/phase-01-test-scenarios.md`), rồi chạy thật
qua HTTP vào PostgreSQL trong Docker và ghi lại. Mọi status và body dưới đây được sao từ lần chạy thật;
số tiền trong body là JSON number đúng scale cột (`3500000000.00`), bảng tóm tắt viết gọn hơn. G68 là 40 request đua, đếm riêng ở mục K.

Quy trình chạy lại được là [`docs/manual-test-guide-goals.md`](../../docs/manual-test-guide-goals.md).

## Thiết lập

| | |
|---|---|
| Database | Container dùng một lần `rikkaus-pg-issue-86`, `postgres:18.6-alpine3.24`, bind `127.0.0.1:5486`; volume `rikkaus-postgres-data` không bị chạm |
| Application | Jar đóng gói bởi `verify`, JDK 25, `--spring.profiles.active=local --server.port=8086`, `DB_URL=jdbc:postgresql://localhost:5486/rikkaus` |
| Migrations | `Successfully applied 5 migrations to schema "public", now at version v8` (V1, V2, V3, V4, V8 — V5/V6/V7 thuộc các PR đang mở). `ddl-auto: validate` chấp nhận mapping, context lên sau 3,197 s: đó là bằng chứng `V8__goals.sql` và entity `Goal` khớp từng cột |
| Users | Hai dòng insert thẳng: A `0f2c8d14-…0c93` / `an.nguyen@example.com`, B `7b41e590-…a842` / `bich.tran@example.com` |
| Tokens | HS256 mint bằng secret dev của profile `local`, claim đúng như `AccessTokenService` phát; thêm một token hết hạn và một token ký bằng secret khác. Request vẫn đi qua `BearerTokenAuthenticationFilter` và `AccessTokenService.verify` thật |
| Ngày | `TODAY=2026-11-14`, `YESTERDAY=2026-11-13`, `TOMORROW=2026-11-15` theo `Asia/Ho_Chi_Minh` |
| Gate | `./services/api/mvnw -f services/api/pom.xml verify -Dopenapi.update=true` xanh (50 integration test), rồi `verify` không flag xanh trên cùng cây nguồn |

**81 case: 70 qua HTTP (G01–G70) và 11 thẳng vào PostgreSQL (D01–D11). Tất cả đúng như thiết kế.
Không case nào fail rồi phải sửa code.** Phân bố status HTTP (không tính các request đọc-lại phụ
và 10 request seed cho case đua):

| Status | Số case |
|---|---|
| `200` | 8 |
| `201` | 14 |
| `204` | 1 |
| `400` | 30 |
| `401` | 5 |
| `404` | 6 |
| `405` | 3 |
| `406` | 1 |
| `415` | 1 |

## A. Bảy template (G01–G07)

| # | `type` | Status | Kết quả |
|---|---|---|---|
| G01 | `EMERGENCY_FUND` | `201` | `Location: /api/v1/goals/cb13feb8-…`, `currency: VND`, `createdAt == updatedAt` |
| G02 | `HOME_PURCHASE` | `201` | `targetAmount: 3500000000.00`, `plannedMonthlyContribution: 25000000.00`, `expectedAnnualReturnPercent: 7.00` |
| G03 | `EDUCATION` | `201` | |
| G04 | `RETIREMENT` | `201` | `targetAmount: 15000000000.00` nguyên vẹn |
| G05 | `NET_WORTH_GROWTH` | `201` | `currentAllocatedAmount: 10470000000.00` |
| G06 | `DEBT_REDUCTION` | `201` | `expectedAnnualReturnPercent: 0.00` — là 0, không phải `null` |
| G07 | `OTHER_GOAL`, chỉ 4 field bắt buộc | `201` | `currentAllocatedAmount: 0.00`, `plannedMonthlyContribution: null`, `expectedAnnualReturnPercent: null` |

Body G02 đọc lại qua `GET` (nguyên văn):

```
{"id":"ce86498a-a4b7-4b85-bf7e-45be1b1f1fc1","name":"Mua căn hộ","type":"HOME_PURCHASE","currency":"VND","targetAmount":3500000000.00,"currentAllocatedAmount":1200000000.00,"targetDate":"2030-12-31","plannedMonthlyContribution":25000000.00,"expectedAnnualReturnPercent":7.00,"createdAt":"2026-11-14T10:55:14.302176Z","updatedAt":"2026-11-14T10:55:14.302176Z"}
```

Không có field tính toán nào (phần trăm hoàn thành, số còn lại, trạng thái) xuất hiện: ranh giới với
#89 được giữ.

## B. Liệt kê (G08–G09)

| # | Request | Status | Kết quả |
|---|---|---|---|
| G08 | A `GET /goals`, hai lần | `200` | 7 phần tử, thứ tự y hệt ở cả hai lần |
| G09 | B `GET /goals` | `200` | `[]` |

Thứ tự trả về — `targetDate` tăng dần, cùng ngày thì theo tên:

```
2027-06-30  Quỹ dự phòng 6 tháng
2028-03-31  Du lịch Nhật Bản
2030-12-31  Mua căn hộ
2030-12-31  Tăng trưởng tài sản ròng
2030-12-31  Tất toán vay mua nhà
2032-08-31  Học phí đại học cho con
2041-12-31  Hưu trí
```

## C. Field bắt buộc và tên (G10–G17)

| # | Request | Status | `errors` |
|---|---|---|---|
| G10 | bỏ `name` | `400` | `name` / `must not be blank` |
| G11 | bỏ `type` | `400` | `type` / `must not be null` |
| G12 | bỏ `targetAmount` | `400` | `targetAmount` / `must not be null` |
| G13 | bỏ `targetDate` | `400` | `targetDate` / `must not be null` |
| G14 | `"name":"   "` | `400` | `name` / `must not be blank` |
| G15 | `name` 121 ký tự | `400` | `name` / `must be at most 120 characters` |
| G16 | `name` = một U+00A0 | `400` | `name` / `must not be blank` |
| G17 | `"Quỹ\u0000dự phòng"` | `400` | `name` / `must not contain a null character` |

Mỗi case nêu đúng một field. G16 và G17 là hai lỗ review đã tìm thấy ở slice assets; slice goals kế
thừa cách chặn và đều trả 400 có tên field thay vì 500 từ driver PostgreSQL hoặc một dòng rỗng trong
danh sách.

## D. Số tiền và lợi suất (G18–G31)

| # | Request | Status | `errors` |
|---|---|---|---|
| G18 | `targetAmount: 0` | `400` | `targetAmount` / `must be greater than zero` |
| G19 | `targetAmount: -1000000` | `400` | `targetAmount` / `must be greater than zero` |
| G20 | `targetAmount: 270000000.123` | `400` | `targetAmount` / `must have at most 17 integer digits and 2 decimals` |
| G21 | `targetAmount` 18 chữ số nguyên | `400` | cùng thông điệp — chặn trước khi tới `NUMERIC(19,2)` |
| G22 | `currentAllocatedAmount: -1` | `400` | `currentAllocatedAmount` / `must not be negative` |
| G23 | allocated 300.000.000 > target 270.000.000 | `400` | `currentAllocatedAmount` / `must not exceed the target amount` |
| G24 | allocated == target | `201` | mục tiêu đã đạt là trạng thái hợp lệ |
| G25 | `plannedMonthlyContribution: -5000000` | `400` | `plannedMonthlyContribution` / `must not be negative` |
| G26 | `plannedMonthlyContribution: 0.00` | `201` | echo `0.00` |
| G27 | `expectedAnnualReturnPercent: -0.5` | `400` | `expectedAnnualReturnPercent` / `must be between 0 and 30` |
| G28 | `expectedAnnualReturnPercent: 30.01` | `400` | `expectedAnnualReturnPercent` / `must be between 0 and 30` |
| G29 | `expectedAnnualReturnPercent: 30.00` | `201` | |
| G30 | `expectedAnnualReturnPercent: 7.125` | `400` | `expectedAnnualReturnPercent` / `must have at most 3 integer digits and 2 decimals` |
| G31 | `targetAmount: -1` và `currentAllocatedAmount: -1` | `400` | **hai** phần tử: `targetAmount` / `must be greater than zero`, `currentAllocatedAmount` / `must not be negative`; không có lỗi thứ ba về vượt target |

Body G23 nguyên văn (quy tắc do service kiểm, không phải annotation):

```
{"detail":"Request validation failed.","instance":"/api/v1/goals","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"currentAllocatedAmount","message":"must not exceed the target amount"}],"correlationId":"a9841bfa-d85b-437d-9f1b-0b316aff534f"}
```

## E. Ngày đích (G32–G35)

| # | Request | Status | Kết quả |
|---|---|---|---|
| G32 | `targetDate: 2026-11-13` (hôm qua) | `400` | `targetDate` / `must be after today` |
| G33 | `targetDate: 2026-11-14` (hôm nay) | `400` | `targetDate` / `must be after today` |
| G34 | `targetDate: 2026-11-15` (mai) | `201` | |
| G35 | `"targetDate":"31/12/2030"` | `400` | `malformed-request`, không có `errors` |

## F. Enum lạ và field không nhận (G36–G38)

| # | Request | Status | Kết quả |
|---|---|---|---|
| G36 | `"type":"CRYPTO"` | `400` | `urn:rikkaus:problem:malformed-request`, không có `errors` |
| G37 | body kèm `"currency":"USD"` | `201` | response `"currency":"VND"` |
| G38 | body kèm `"completionPercent":50` | `201` | response không có `completionPercent` |

## G. Sửa — thay thế toàn bộ (G39–G46)

| # | Request | Status | Kết quả |
|---|---|---|---|
| G39 | đổi cả 7 field, `type` `EMERGENCY_FUND` → `OTHER_GOAL` | `200` | cả 7 đổi; `id` và `createdAt` (`…14.232573Z`) giữ nguyên, `updatedAt` → `…14.662397Z` |
| G40 | chỉ 4 field bắt buộc | `200` | `currentAllocatedAmount: 0.00`, hai field kia `null` |
| G40b | `GET` lại | `200` | body **giống hệt** G40 — đã lưu thế, không chỉ render thế |
| G41 | bỏ `name` | `400` | `name` / `must not be blank` |
| G42 | allocated 500.000.000 > target 405.000.000 | `400` | `currentAllocatedAmount` / `must not exceed the target amount` |
| G43 | `targetDate` = hôm qua | `400` | `targetDate` / `must be after today` |
| G44 | mục tiêu bị `psql` đặt `target_date = 2026-01-31` (quá hạn); `PUT` đổi tên, giữ `2026-01-31` | `200` | tên đổi, ngày giữ — mục tiêu quá hạn vẫn sửa được |
| G45 | cùng mục tiêu, `PUT` `targetDate: 2026-02-28` (quá khứ khác) | `400` | `targetDate` / `must be after today` |
| G46 | `PUT` `"type":"CRYPTO"` | `400` | `malformed-request` |

Body G39:

```
{"id":"cb13feb8-d7aa-410d-8396-c2c185271cd0","name":"Quỹ dự phòng 9 tháng","type":"OTHER_GOAL","currency":"VND","targetAmount":405000000.00,"currentAllocatedAmount":120000000.00,"targetDate":"2027-12-31","plannedMonthlyContribution":18000000.00,"expectedAnnualReturnPercent":2.50,"createdAt":"2026-11-14T10:55:14.232573Z","updatedAt":"2026-11-14T10:55:14.662397Z"}
```

## H. Ownership (G47–G52)

| # | Request | Status | Kết quả |
|---|---|---|---|
| G47 | A đọc mục tiêu của A | `200` | |
| G48 | B đọc mục tiêu của A | `404` | `not-found` |
| G49 | B đọc id không tồn tại | `404` | `not-found` |
| G50 | B `PUT` mục tiêu của A | `404` | A đọc lại: body **giống hệt** G47 |
| G51 | B `DELETE` mục tiêu của A | `404` | A đọc lại: `200`, row còn |
| G52 | B tạo "Tiền mặt dự phòng của Bích" 50.000.000 ₫ rồi `GET /goals` | `201` / `200` | danh sách của B đúng một phần tử, không có mục tiêu nào của A |

G48 và G49 sau khi bỏ `instance` và `correlationId` **bằng nhau từng byte**:
`{"detail":"Resource not found.","status":404,"title":"Resource not found","type":"urn:rikkaus:problem:not-found"}`.
Nguyên văn G49:

```
{"detail":"Resource not found.","instance":"/api/v1/goals/00000000-0000-4000-8000-000000000000","status":404,"title":"Resource not found","type":"urn:rikkaus:problem:not-found","correlationId":"68a279f3-3116-443f-bbea-2a0ddd45ae96"}
```

## I. Xác thực (G53–G57)

| # | Request | Status |
|---|---|---|
| G53 | không header | `401` |
| G54 | token hết hạn | `401` |
| G55 | token ký secret khác | `401` |
| G56 | `GET /goals/<id>` không header | `401` |
| G57 | `GET /goal-templates` không header | `401` |

G53, G54, G55 sau khi bỏ `correlationId` **bằng nhau từng byte**. Nguyên văn:

```
{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized","correlationId":"e5f55356-f4f1-4287-b49a-243e17e9bfff"}
```

## J. Giao thức (G58–G63)

| # | Request | Status | `type` |
|---|---|---|---|
| G58 | `GET /goals/not-a-uuid` | `400` | `malformed-request` |
| G59 | `PATCH /goals/<id>` | `405` | `method-not-allowed`, `"Method 'PATCH' is not supported."` |
| G60 | `DELETE /goals` | `405` | `method-not-allowed` |
| G61 | body `text/plain` | `415` | `unsupported-media-type` |
| G62 | `{"name": ` | `400` | `malformed-request` |
| G63 | `Accept: application/xml` | `406` | `not-acceptable` |

Tất cả là `application/problem+json`. Không có body mặc định của Boot ở đâu.

## K. Xóa (G64–G68)

| # | Request | Status | Kết quả |
|---|---|---|---|
| G64 | A `DELETE` Học phí đại học cho con | `204` | không body |
| G65 | `GET` id vừa xóa | `404` | |
| G66 | `DELETE` lần hai | `404` | |
| G67 | `GET /goals` | `200` | 12 phần tử (7 ban đầu + 6 tạo thêm ở D/E/F − 1 xóa) |
| G68 | 4 `DELETE` đồng thời, 10 vòng | — | **mỗi vòng `[204, 404, 404, 404]`; 0 lần 500 trên 40 request** |

## L. Templates (G69–G70)

| # | Request | Status |
|---|---|---|
| G69 | `GET /goal-templates` | `200`, 7 phần tử |
| G70 | `POST /goal-templates` | `405` |

Nguyên văn G69, khớp từng ô với bảng "Bảy mẫu và thứ chúng điền sẵn" của design đã duyệt:

```
[{"type":"EMERGENCY_FUND","defaultExpectedAnnualReturnPercent":3.00,"defaultHorizonMonths":18,"amountPrefill":"TARGET_FROM_SIX_MONTHS_OF_EXPENSES"},{"type":"HOME_PURCHASE","defaultExpectedAnnualReturnPercent":6.00,"defaultHorizonMonths":60,"amountPrefill":"NONE"},{"type":"EDUCATION","defaultExpectedAnnualReturnPercent":5.00,"defaultHorizonMonths":72,"amountPrefill":"NONE"},{"type":"RETIREMENT","defaultExpectedAnnualReturnPercent":8.00,"defaultHorizonMonths":180,"amountPrefill":"NONE"},{"type":"NET_WORTH_GROWTH","defaultExpectedAnnualReturnPercent":7.00,"defaultHorizonMonths":48,"amountPrefill":"ALLOCATED_FROM_CURRENT_NET_WORTH"},{"type":"DEBT_REDUCTION","defaultExpectedAnnualReturnPercent":0.00,"defaultHorizonMonths":48,"amountPrefill":"TARGET_FROM_TOTAL_LIABILITIES"},{"type":"OTHER_GOAL","defaultExpectedAnnualReturnPercent":0.00,"defaultHorizonMonths":24,"amountPrefill":"NONE"}]
```

## M. Thẳng vào PostgreSQL (D01–D11)

D01 — kiểu cột, đọc từ `information_schema.columns`:

```
target_amount                  | numeric | 19 | 2
current_allocated_amount       | numeric | 19 | 2
planned_monthly_contribution   | numeric | 19 | 2
expected_annual_return_percent | numeric |  5 | 2
```

Không có `double precision`. D02 — hai giá trị lớn nhất, đọc bằng `psql` nên không có serializer nào
ở giữa: `Hưu trí | 15000000000.00`, `Tăng trưởng tài sản ròng | 15000000000.00`.

D03–D10 — insert trực tiếp, bỏ qua ứng dụng. Mỗi probe bị đúng constraint có tên từ chối và không
insert gì; `count(*)` là 13 trước và 13 sau:

| # | Probe | Constraint đã từ chối |
|---|---|---|
| D03 | `type = 'CRYPTO'` | `goals_type_is_known` |
| D04 | `target_amount = 0` | `goals_target_amount_is_positive` |
| D05 | `current_allocated_amount = -1` | `goals_allocated_amount_is_not_negative` |
| D06 | allocated 2 > target 1 | `goals_allocated_amount_within_target` |
| D07 | `expected_annual_return_percent = 31` | `goals_expected_return_within_bounds` |
| D08 | `currency = 'USD'` | `goals_currency_is_vnd` |
| D09 | `name = '   '` | `goals_name_is_present_and_bounded` |
| D10 | `planned_monthly_contribution = -1` | `goals_contribution_is_not_negative` |

D11 — `SELECT user_id, count(*) ... GROUP BY user_id`: A có 12, B có 1, khớp với G67 và G52b.

## Chạy lại trên commit cuối

Sau commit `056873f` (`feat(api): add generic financial goals and the seven templates`), container
PostgreSQL được xóa và tạo lại rỗng, API được khởi động từ jar mà `verify` đóng gói từ đúng cây nguồn
của commit đó (18:02 ICT, Flyway `now at version v8`, context lên sau 2,932 s), và toàn bộ 81 case được
chạy lại theo cùng thứ tự.

- Chuỗi status của 84 request HTTP (70 case + các request đọc-lại phụ) **trùng khớp từng vị trí** với
  lần 1, so bằng máy sau khi bỏ id.
- G68: 10 vòng, mỗi vòng `[204, 404, 404, 404]`, 0 lần 500 trên 40 request.
- G48/G49 và G53/G54/G55 vẫn bằng nhau từng byte sau khi bỏ `instance` và `correlationId`.
- D03–D10 bị đúng 8 constraint có tên từ chối; `count(*)` 13 trước và 13 sau; D11 A có 12, B có 1.

`./services/api/mvnw -f services/api/pom.xml verify` không flag trên cùng commit: 81 unit test và 50
integration test xanh, nghĩa là `openapi.json` đã commit khớp ứng dụng đang chạy và hai inventory khớp
schema và surface đã công bố.

## Quan sát không phải defect của slice này

**Lỗi bean-validation mang `detail` của Spring, lỗi service mang `detail` của taxonomy.** G10–G15,
G18–G22, G25, G27–G31, G41 trả `"detail":"Invalid request content."`; G16, G17, G23, G32, G33, G42, G43,
G45 trả `"detail":"Request validation failed."`. Cùng `type`, `title`, `errors`; client switch theo
`type` như conventions đã dặn nên không thấy khác. Hành vi này có từ #39 trên mọi route và đã được
ghi ở report #53; không sửa dưới issue này.

**`operationId` của asset đổi tên trong `openapi.json`.** springdoc đặt `operationId` theo tên method
và thêm hậu tố khi trùng; thêm controller goals có `list`/`create`/`read`/`delete` làm các operation
của assets đổi thành `list_1`, `create_1`, `read_3`, `delete_1` Không client nào sinh code từ contract nên không
gì tiêu thụ các id này; đã ghi vào `docs/api-contract-conventions.md`.

## Lần chạy này không bao phủ

- Đăng nhập Google thật: token được mint, filter và verifier là bản production, Google handoff thì
  không.
- Hai `PUT` chồng nhau: cả hai thành công, người ghi sau thắng, không báo xung đột (không có row
  version — quyết định sản phẩm về sửa đồng thời).
- Mọi phép chiếu của #89, chưa có route.
- Mobile client (#85).

## Verification gap

Không có automated test nào chạm `/api/v1/goals` hay `/api/v1/goal-templates`; regression ở slice này
chỉ bị bắt khi chạy lại guide. Bộ test sẵn có vẫn bắt: bảng không khai báo (`ApplicationContextIT`),
path công bố/khai báo lệch nhau (`OpenApiContractIT`), controller nhận owner id từ client, field tiền
`double`, slice chạm slice khác (`ArchitectureRulesTest`).
