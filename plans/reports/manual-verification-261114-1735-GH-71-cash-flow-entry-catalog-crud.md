# Xác minh thủ công — danh mục dòng tiền và CRUD (#71)

Ngày: 2026-11-14. Ba lần chạy trên nhánh `thientrinhcoder/feat/issue-71-cash-flow-entry-catalog-crud`:

| Lần | Giờ (ICT) | Mã nguồn | Số case | Đạt |
|---|---|---|---|---|
| 1 | 17:56 | working tree trước commit đầu, cắt từ `main` `6e57e92` | 92 | 89 |
| 2 | 18:00 | `b6f4c9c` | 92 | 92 |
| 3 | 18:23 | `ef42b3f` — merge `main` `f7e5fdb` (#59, #62), migration đổi thành `V8`, thêm `linkedLiabilityId` | 100 | **100** |

Lần chạy 3 là bằng chứng cho trạng thái hiện tại. Không có thay đổi code nào giữa lần chạy 3 và commit
`ef42b3f`; commit chứa file này chỉ thêm tài liệu.

Đây là bằng chứng hành vi cho issue #71. Product Owner chỉ đạo task này ship **không có unit test hay
integration test mới**; thay vào đó toàn bộ scenario được viết trước ở
[`phase-01-test-scenarios.md`](../261114-1735-GH-71-cash-flow-entry-catalog-crud/phase-01-test-scenarios.md),
rồi chạy thật qua HTTP vào PostgreSQL trong Docker và ghi lại đúng điều API trả về. Mọi status và body
dưới đây được chép từ lần chạy thật, không viết tay. Quy trình tái hiện ở
[`docs/manual-test-guide-cash-flow-entries.md`](../../docs/manual-test-guide-cash-flow-entries.md).

## Môi trường của lần chạy 3

| | |
|---|---|
| Database | Container `postgres:18.6-alpine3.24` dùng một lần, tên `rikkaus-pg-issue-71`, cổng `127.0.0.1:5471`, tạo mới cho lần chạy và xóa sau đó. Volume `rikkaus-postgres-data` không bị chạm |
| Ứng dụng | Mã đã merge, chạy qua `spring-boot:run` trên JDK 25, `--spring.profiles.active=local --server.port=8071`, `DB_URL=jdbc:postgresql://localhost:5471/rikkaus`. Không chạy bằng jar đóng gói vì hook của máy chặn truy cập thư mục `target/`; mã nguồn và classpath là một |
| Migration | Flyway: `Successfully applied 7 migrations to schema "public", now at version v8` (V1, V2, V3, V4, V6, V7 liabilities, V8 cash-flow entries). `ddl-auto: validate` chấp nhận mapping, context khởi động trong 2,2 giây: đó là bằng chứng `V8__cash_flow_entries.sql` và entity `CashFlowEntry` khớp từng cột, kể cả `linked_liability_id` |
| Người dùng | Hai row chèn thẳng: user A `0f2c8d14-…0c93` (An Nguyễn), user B `7b41e590-…a842` (Bích Trần) |
| Token | HS256 mint bằng secret phát triển của profile `local`, đúng bộ claim `AccessTokenService` phát hành; thêm một token hết hạn và một token ký bằng secret khác. Request đi qua `BearerTokenAuthenticationFilter` và `AccessTokenService.verify` thật |
| Bản ghi để liên kết | Tạo qua API của slice khác: tài sản `Căn hộ Thảo Điền` (`6.100.000.000 ₫`) và `Xe Volvo XC60` (`1.850.000.000 ₫`) của A, `Tiền mặt của Bích` (`5.000.000 ₫`) của B; công nợ `Vay mua nhà Vietcombank` (`2.400.000.000 ₫`, trả `23.500.000 ₫`/tháng) của A, `Vay tiêu dùng FE Credit` (`120.000.000 ₫`) của B |
| Gate build | `./services/api/mvnw -f services/api/pom.xml clean verify` xanh **không** dùng update flag sau khi regenerate contract: 81 unit test, 50 integration test |

## Lần chạy 1 — 89/92, ba case không đạt

Chạy trên working tree trước commit. Ba case không đạt, mỗi case được xem lại để phân biệt lỗi code với
lỗi của kịch bản kiểm tra.

| # | Điều xảy ra | Phân loại | Xử lý |
|---|---|---|---|
| 28 | `amount: "45000000"` trả `201` nhưng body echo `"amount":45000000` (scale 0), trong khi `GET` sau đó đọc từ `NUMERIC(19, 2)` ra `45000000.00`. Cùng một entry, hai cách viết tùy vào request nào đọc nó | **Lỗi code**, nhỏ nhưng thật | `CashFlowEntryService.validated` đặt `setScale(2)` trước khi lưu; `@Digits` đã chặn quá hai số lẻ nên không có gì bị làm tròn. Lần 2: `201` với `45000000.00` ở cả `POST` và `GET` |
| 83 | Kịch bản kiểm tra tìm `48000000.00` trong ba dòng lớn nhất, nhưng entry lương đã bị xóa ở case 77 trước đó | Lỗi của kịch bản kiểm tra | Assertion đổi thành "không có mũ, mọi số đều hai số lẻ". Dữ liệu trong DB vốn đã đúng |
| 85 | Probe `entry_type = 'TRANSFER'` bị bảng từ chối, nhưng constraint được báo là `cash_flow_entries_category_matches_entry_type`, không phải `cash_flow_entries_entry_type_is_known` như kịch bản kỳ vọng | Kỳ vọng viết sai | Mọi category đều gắn với `INCOME` hoặc `EXPENSE`, nên loại lạ luôn vi phạm cả constraint ghép cặp, và PostgreSQL kiểm tra theo thứ tự tên. Bảng vẫn từ chối; phase 1 và guide đã sửa kỳ vọng. Constraint `entry_type_is_known` giữ lại như phát biểu tường minh |

89 case còn lại đạt ở lần 1 và đạt lại ở lần 2 với cùng status.

## Lần chạy 2 — 92/92 trên `b6f4c9c`

Chạy lại toàn bộ 92 case trên commit đầu sau khi sửa lỗi scale của lần 1. Mọi case đạt, cùng status với
lần chạy 3 cho 92 case chung. Bảng chi tiết dưới đây lấy từ lần chạy 3, vì đó là mã nguồn hiện tại.

## Lần chạy 3 — 100/100 trên `ef42b3f`, sau khi merge `main`

**100 case: 86 qua HTTP (case 81 gồm 40 request đồng thời) và 14 thẳng vào PostgreSQL.** Mọi case đúng
thiết kế ngay lần đầu chạy trên mã đã merge. 92 case cũ cho cùng status như lần chạy 2; 8 case mới của
liên kết công nợ (93–100) đạt ngay.

| Status | Số case |
|---|---|
| `200` | 13 |
| `201` | 24 |
| `204` | 1 |
| `400` | 34 |
| `401` | 4 |
| `404` | 6 |
| `405` | 2 |
| `406` | 1 |
| `415` | 1 |

### Liên kết công nợ, nguyên văn

Case 93, khoản lãi vay liên kết với khoản vay của chính A:

```
{"id":"fd2faff7-cf62-44e6-a6a8-5e7f8476aa19","entryType":"EXPENSE","category":"INTEREST_PAYMENT","amount":17000000.00,"currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-05","endDate":null,"linkedAssetId":null,"linkedLiabilityId":"7b3d874d-a50c-4e7c-84cb-0acf89403e5e","notes":"Lãi vay mua nhà","createdAt":"2026-11-14T11:23:26.076840Z","updatedAt":"2026-11-14T11:23:26.076840Z"}
```

Case 94 (khoản vay của B) và 95 (khoản vay không tồn tại), cạnh nhau. Khác nhau **chỉ** ở `correlationId`:

```
{"detail":"Request validation failed.","instance":"/api/v1/cash-flow-entries","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"linkedLiabilityId","message":"must reference one of the signed-in user's liabilities"}],"correlationId":"73c8b835-cb1b-446a-94b0-f47dc70b28fc"}
{"detail":"Request validation failed.","instance":"/api/v1/cash-flow-entries","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"linkedLiabilityId","message":"must reference one of the signed-in user's liabilities"}],"correlationId":"5335c917-9193-466d-b659-58a8398b4d04"}
```

Case 96, gửi cả hai liên kết: **đúng một** lỗi, không kèm lỗi ownership nào dù cả hai id đều là của A:

```
{"detail":"Request validation failed.","instance":"/api/v1/cash-flow-entries","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"linkedLiabilityId","message":"must not be combined with linkedAssetId"}],"correlationId":"eee642f8-9be4-4bba-b623-585bae6360b4"}
```

Case 97, `PUT` đổi liên kết từ khoản vay sang xe; `linkedLiabilityId` thành `null`:

```
{"id":"fd2faff7-cf62-44e6-a6a8-5e7f8476aa19","entryType":"EXPENSE","category":"INTEREST_PAYMENT","amount":17000000.00,"currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-05","endDate":null,"linkedAssetId":"5e55df0a-c471-48ce-a716-f3c008ff119a","linkedLiabilityId":null,"notes":null,"createdAt":"2026-11-14T11:23:26.076840Z","updatedAt":"2026-11-14T11:23:26.093835Z"}
```

Case 98, xóa khoản vay rồi đọc lại entry: entry còn, `linkedLiabilityId` thành `null`:

```
DELETE liability -> 204
{"id":"fd2faff7-cf62-44e6-a6a8-5e7f8476aa19","entryType":"EXPENSE","category":"INTEREST_PAYMENT","amount":17000000.00,"currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-05","endDate":null,"linkedAssetId":null,"linkedLiabilityId":null,"notes":null,"createdAt":"2026-11-14T11:23:26.076840Z","updatedAt":"2026-11-14T11:23:26.100010Z"}
```

Case 99 và 100, bảng tự từ chối không qua API:

```
ERROR:  new row for relation "cash_flow_entries" violates check constraint "cash_flow_entries_links_at_most_one_record"
ERROR:  insert or update on table "cash_flow_entries" violates foreign key constraint "cash_flow_entries_linked_liability_id_fkey"
```

### Các body khác đáng đọc nguyên văn

Case 1, lương tháng, và cách trường optional vắng đọc lại là `null` — kể cả `linkedLiabilityId` mới:

```
{"id":"d9b1a8f7-2179-46eb-8b68-fccc0a8519e6","entryType":"INCOME","category":"SALARY","amount":45000000.00,"currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-01","endDate":null,"linkedAssetId":null,"linkedLiabilityId":null,"notes":"Lương tháng","createdAt":"2026-11-14T11:23:25.822399Z","updatedAt":"2026-11-14T11:23:25.822399Z"}
```

Case 4, cổ tức 150 USD, **không có số VND nào** trong body — slice không quy đổi:

```
{"id":"e328f524-3332-4ef5-bcca-b1ec7708e141","entryType":"INCOME","category":"DIVIDEND","amount":150.00,"currency":"USD","timing":"ONE_TIME","recurrence":null,"effectiveDate":"2026-09-12","endDate":null,"linkedAssetId":null,"linkedLiabilityId":null,"notes":null,"createdAt":"2026-11-14T11:23:25.848186Z","updatedAt":"2026-11-14T11:23:25.848186Z"}
```

Case 41, một lần kèm cả `recurrence` và `endDate`: hai lỗi trong một lần trả về:

```
{"detail":"Request validation failed.","instance":"/api/v1/cash-flow-entries","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"recurrence","message":"must be omitted for a one-time entry"},{"field":"endDate","message":"must be omitted for a one-time entry"}],"correlationId":"c374d5cb-a431-4afc-ab32-5124b28d8255"}
```

Case 48 (tài sản của B) và 49 (tài sản không tồn tại). Khác nhau chỉ ở `correlationId`:

```
{"detail":"Request validation failed.","instance":"/api/v1/cash-flow-entries","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"linkedAssetId","message":"must reference one of the signed-in user's assets"}],"correlationId":"7c313b81-2e49-4fe1-9241-5842856d843f"}
{"detail":"Request validation failed.","instance":"/api/v1/cash-flow-entries","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"linkedAssetId","message":"must reference one of the signed-in user's assets"}],"correlationId":"d396c9b0-fd03-4eea-baf0-6d680aa0c0d0"}
```

Case 58 (B đọc entry của A) và 59 (B đọc id không tồn tại). Giống nhau ở `type`, `title`, `status`,
`detail`; khác ở `instance` — request URI theo RFC 9457 — và `correlationId`:

```
{"detail":"Resource not found.","instance":"/api/v1/cash-flow-entries/d9b1a8f7-2179-46eb-8b68-fccc0a8519e6","status":404,"title":"Resource not found","type":"urn:rikkaus:problem:not-found","correlationId":"f934a328-2136-4dac-a4f6-423b4785fa86"}
{"detail":"Resource not found.","instance":"/api/v1/cash-flow-entries/00000000-0000-4000-8000-000000000000","status":404,"title":"Resource not found","type":"urn:rikkaus:problem:not-found","correlationId":"6385a6f6-50f7-44d8-a14b-ca1f32d13795"}
```

Case 53, xóa căn hộ rồi đọc lại khoản tiền thuê — FK `ON DELETE SET NULL`:

```
DELETE asset -> 204
{"id":"f4af5728-1720-43b5-bffd-a51ff4dd7eea","entryType":"INCOME","category":"RENTAL_INCOME","amount":18000000.00,"currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-02-01","endDate":null,"linkedAssetId":null,"linkedLiabilityId":null,"notes":null,"createdAt":"2026-11-14T11:23:26.031114Z","updatedAt":"2026-11-14T11:23:26.058075Z"}
```

Case 67, không có token:

```
{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized","correlationId":"5f02557e-74dc-4cc8-b3a0-33fa5b703b46"}
```

Case 81, bốn `DELETE` đồng thời một id, mười vòng — không có `500`:

```
{"0": [204, 404, 404, 404], "1": [204, 404, 404, 404], "2": [204, 404, 404, 404], "3": [204, 404, 404, 404], "4": [204, 404, 404, 404], "5": [204, 404, 404, 404], "6": [204, 404, 404, 404], "7": [204, 404, 404, 404], "8": [204, 404, 404, 404], "9": [204, 404, 404, 404]}
```

Case 82, kiểu cột — `amount` là `numeric(19,2)`, `linked_liability_id` là `uuid`, không có `double precision`:

```
amount|numeric|19|2
linked_asset_id|uuid||
linked_liability_id|uuid||
```

Case 83, ba số lớn nhất và tổng VND do PostgreSQL cộng — nguyên, không mũ:

```
BUSINESS_INCOME|45000000.00|VND
BUSINESS_INCOME|26200000.00|VND
BUSINESS_INCOME|26200000.00|VND
SUM 350135000.00
```

`count(*)` trước và sau mười probe: `23` và `23`.

## Toàn bộ 100 case (lần chạy 3)

### A. Đủ 16 danh mục, đúng loại

| # | Request | Status | Kết quả | Verdict |
|---|---|---|---|---|
| 1 | POST INCOME/SALARY | `201` | entryType=`INCOME`, category=`SALARY`, amount=`45000000.00`, currency=`VND`, timing=`RECURRING`, recurrence=`MONTHLY`, effectiveDate=`2026-01-01`, notes=`Lương tháng` | đúng thiết kế |
| 2 | POST INCOME/BUSINESS_INCOME | `201` | entryType=`INCOME`, category=`BUSINESS_INCOME`, amount=`26200000.00`, currency=`VND`, timing=`ONE_TIME`, effectiveDate=`2026-09-10` | đúng thiết kế |
| 3 | POST INCOME/RENTAL_INCOME | `201` | entryType=`INCOME`, category=`RENTAL_INCOME`, amount=`18000000.00`, currency=`VND`, timing=`RECURRING`, recurrence=`MONTHLY`, effectiveDate=`2026-02-01`, notes=`Tiền thuê căn hộ Thảo Điền` | đúng thiết kế |
| 4 | POST INCOME/DIVIDEND | `201` | entryType=`INCOME`, category=`DIVIDEND`, amount=`150.00`, currency=`USD`, timing=`ONE_TIME`, effectiveDate=`2026-09-12` | đúng thiết kế |
| 5 | POST INCOME/DEPOSIT_INTEREST | `201` | entryType=`INCOME`, category=`DEPOSIT_INTEREST`, amount=`1500000.00`, currency=`VND`, timing=`RECURRING`, recurrence=`QUARTERLY`, effectiveDate=`2026-03-31` | đúng thiết kế |
| 6 | POST INCOME/BOND_OR_FUND_INCOME | `201` | entryType=`INCOME`, category=`BOND_OR_FUND_INCOME`, amount=`1500000.00`, currency=`VND`, timing=`RECURRING`, recurrence=`YEARLY`, effectiveDate=`2026-06-30` | đúng thiết kế |
| 7 | POST INCOME/OTHER_INCOME | `201` | entryType=`INCOME`, category=`OTHER_INCOME`, amount=`235000.00`, currency=`VND`, timing=`ONE_TIME`, effectiveDate=`2026-09-20` | đúng thiết kế |
| 8 | POST EXPENSE/LIVING_COSTS | `201` | entryType=`EXPENSE`, category=`LIVING_COSTS`, amount=`23400000.00`, currency=`VND`, timing=`RECURRING`, recurrence=`MONTHLY`, effectiveDate=`2026-01-01` | đúng thiết kế |
| 9 | POST EXPENSE/HOUSING | `201` | entryType=`EXPENSE`, category=`HOUSING`, amount=`18750000.00`, currency=`VND`, timing=`RECURRING`, recurrence=`MONTHLY`, effectiveDate=`2026-01-01` | đúng thiết kế |
| 10 | POST EXPENSE/EDUCATION | `201` | entryType=`EXPENSE`, category=`EDUCATION`, amount=`12500000.00`, currency=`VND`, timing=`RECURRING`, recurrence=`QUARTERLY`, effectiveDate=`2026-09-05`, endDate=`2029-06-30`, notes=`Học phí` | đúng thiết kế |
| 11 | POST EXPENSE/HEALTHCARE | `201` | entryType=`EXPENSE`, category=`HEALTHCARE`, amount=`4650000.00`, currency=`VND`, timing=`ONE_TIME`, effectiveDate=`2026-09-18` | đúng thiết kế |
| 12 | POST EXPENSE/INSURANCE | `201` | entryType=`EXPENSE`, category=`INSURANCE`, amount=`5400000.00`, currency=`VND`, timing=`RECURRING`, recurrence=`YEARLY`, effectiveDate=`2026-04-01` | đúng thiết kế |
| 13 | POST EXPENSE/TAX_AND_FEES | `201` | entryType=`EXPENSE`, category=`TAX_AND_FEES`, amount=`8250000.00`, currency=`VND`, timing=`RECURRING`, recurrence=`QUARTERLY`, effectiveDate=`2026-01-31` | đúng thiết kế |
| 14 | POST EXPENSE/DEBT_PRINCIPAL_PAYMENT | `201` | entryType=`EXPENSE`, category=`DEBT_PRINCIPAL_PAYMENT`, amount=`10000000.00`, currency=`VND`, timing=`RECURRING`, recurrence=`MONTHLY`, effectiveDate=`2026-01-15`, endDate=`2031-01-15` | đúng thiết kế |
| 15 | POST EXPENSE/INTEREST_PAYMENT | `201` | entryType=`EXPENSE`, category=`INTEREST_PAYMENT`, amount=`6250000.00`, currency=`VND`, timing=`RECURRING`, recurrence=`MONTHLY`, effectiveDate=`2026-01-15` | đúng thiết kế |
| 16 | POST EXPENSE/OTHER_EXPENSE | `201` | entryType=`EXPENSE`, category=`OTHER_EXPENSE`, amount=`2500000.00`, currency=`VND`, timing=`ONE_TIME`, effectiveDate=`2026-09-25` | đúng thiết kế |

### B. Danh mục sai loại

| # | Request | Status | Kết quả | Verdict |
|---|---|---|---|---|
| 17 | category SALARY under EXPENSE | `400` | `validation-failed`, errors [{"field": "category", "message": "must belong to the entry type"}] | đúng thiết kế |
| 18 | category HOUSING under INCOME | `400` | `validation-failed`, errors [{"field": "category", "message": "must belong to the entry type"}] | đúng thiết kế |

### C. Thiếu từng trường bắt buộc

| # | Request | Status | Kết quả | Verdict |
|---|---|---|---|---|
| 19 | missing entryType | `400` | `validation-failed`, errors [{"field": "entryType", "message": "must not be null"}] | đúng thiết kế |
| 20 | missing category | `400` | `validation-failed`, errors [{"field": "category", "message": "must not be null"}] | đúng thiết kế |
| 21 | missing amount | `400` | `validation-failed`, errors [{"field": "amount", "message": "must not be null"}] | đúng thiết kế |
| 22 | missing currency | `400` | `validation-failed`, errors [{"field": "currency", "message": "must not be null"}] | đúng thiết kế |
| 23 | missing timing | `400` | `validation-failed`, errors [{"field": "timing", "message": "must not be null"}] | đúng thiết kế |
| 24 | missing effectiveDate | `400` | `validation-failed`, errors [{"field": "effectiveDate", "message": "must not be null"}] | đúng thiết kế |

### D. Số tiền

| # | Request | Status | Kết quả | Verdict |
|---|---|---|---|---|
| 25 | amount 0 | `400` | `validation-failed`, errors [{"field": "amount", "message": "must be greater than zero"}] | đúng thiết kế |
| 26 | amount -1000000.00 | `400` | `validation-failed`, errors [{"field": "amount", "message": "must be greater than zero"}] | đúng thiết kế |
| 27 | amount 1000000.123 | `400` | `validation-failed`, errors [{"field": "amount", "message": "must have at most 17 integer digits and 2 decimals"}] | đúng thiết kế |
| 28 | amount 45000000 (no decimals) | `201` | entryType=`INCOME`, category=`BUSINESS_INCOME`, amount=`45000000.00`, currency=`VND`, timing=`ONE_TIME`, effectiveDate=`2026-09-10` | đúng thiết kế |

### E. Hằng số lạ

| # | Request | Status | Kết quả | Verdict |
|---|---|---|---|---|
| 29 | unknown constant ['entryType'] | `400` | `malformed-request` | đúng thiết kế |
| 30 | unknown constant ['category'] | `400` | `malformed-request` | đúng thiết kế |
| 31 | unknown constant ['currency'] | `400` | `malformed-request` | đúng thiết kế |
| 32 | unknown constant ['timing'] | `400` | `malformed-request` | đúng thiết kế |
| 33 | unknown constant ['timing', 'recurrence'] | `400` | `malformed-request` | đúng thiết kế |

### F. Một lần, định kỳ, ngày

| # | Request | Status | Kết quả | Verdict |
|---|---|---|---|---|
| 34 | timing/date case 34 | `400` | `validation-failed`, errors [{"field": "recurrence", "message": "must be omitted for a one-time entry"}] | đúng thiết kế |
| 35 | timing/date case 35 | `400` | `validation-failed`, errors [{"field": "recurrence", "message": "must be present for a recurring entry"}] | đúng thiết kế |
| 36 | timing/date case 36 | `400` | `validation-failed`, errors [{"field": "endDate", "message": "must be omitted for a one-time entry"}] | đúng thiết kế |
| 37 | timing/date case 37 | `400` | `validation-failed`, errors [{"field": "endDate", "message": "must not be before the effective date"}] | đúng thiết kế |
| 38 | timing/date case 38 | `201` | entryType=`INCOME`, category=`BUSINESS_INCOME`, amount=`26200000.00`, currency=`VND`, timing=`RECURRING`, recurrence=`MONTHLY`, effectiveDate=`2026-09-05`, endDate=`2026-09-05` | đúng thiết kế |
| 39 | timing/date case 39 | `201` | entryType=`INCOME`, category=`BUSINESS_INCOME`, amount=`26200000.00`, currency=`VND`, timing=`RECURRING`, recurrence=`MONTHLY`, effectiveDate=`2026-09-05` | đúng thiết kế |
| 40 | timing/date case 40 | `201` | entryType=`INCOME`, category=`BUSINESS_INCOME`, amount=`26200000.00`, currency=`VND`, timing=`ONE_TIME`, effectiveDate=`2027-01-01` | đúng thiết kế |
| 41 | timing/date case 41 | `400` | `validation-failed`, errors [{"field": "recurrence", "message": "must be omitted for a one-time entry"}, {"field": "endDate", "message": "must be omitted for a one-time entry"}] | đúng thiết kế |
| 42 | effectiveDate 01/01/2026 | `400` | `malformed-request` | đúng thiết kế |

### G. Ghi chú

| # | Request | Status | Kết quả | Verdict |
|---|---|---|---|---|
| 43 | notes 2001 chars | `400` | `validation-failed`, errors [{"field": "notes", "message": "must be at most 2000 characters"}] | đúng thiết kế |
| 44 | notes blank | `201` | entryType=`INCOME`, category=`BUSINESS_INCOME`, amount=`26200000.00`, currency=`VND`, timing=`ONE_TIME`, effectiveDate=`2026-09-10` | đúng thiết kế |
| 45 | notes trimmed | `201` | entryType=`INCOME`, category=`BUSINESS_INCOME`, amount=`26200000.00`, currency=`VND`, timing=`ONE_TIME`, effectiveDate=`2026-09-10`, notes=`Lương tháng 11.` | đúng thiết kế |
| 46 | notes NUL | `400` | `validation-failed`, errors [{"field": "notes", "message": "must not contain a null character"}] | đúng thiết kế |

### H. Liên kết tài sản

| # | Request | Status | Kết quả | Verdict |
|---|---|---|---|---|
| 47 | link own asset | `201` | entryType=`INCOME`, category=`RENTAL_INCOME`, amount=`18000000.00`, currency=`VND`, timing=`RECURRING`, recurrence=`MONTHLY`, effectiveDate=`2026-02-01`, linkedAssetId=`55f087aa-afb6-4bbc-846c-89ce15ac5261`, notes=`Tiền thuê căn hộ Thảo Điền` | đúng thiết kế |
| 48 | link other user's asset | `400` | `validation-failed`, errors [{"field": "linkedAssetId", "message": "must reference one of the signed-in user's assets"}] | đúng thiết kế |
| 49 | link nonexistent asset | `400` | `validation-failed`, errors [{"field": "linkedAssetId", "message": "must reference one of the signed-in user's assets"}] | đúng thiết kế |
| 50 | linkedAssetId not a uuid | `400` | `malformed-request` | đúng thiết kế |
| 51 | PUT relink to other user's asset | `400` | `validation-failed`, errors [{"field": "linkedAssetId", "message": "must reference one of the signed-in user's assets"}] | đúng thiết kế |
| 52 | PUT omit linkedAssetId | `200` | entryType=`INCOME`, category=`RENTAL_INCOME`, amount=`18000000.00`, currency=`VND`, timing=`RECURRING`, recurrence=`MONTHLY`, effectiveDate=`2026-02-01` | đúng thiết kế |
| 53 | delete linked asset then read entry | `200` | `DELETE asset -> 204 ⏎ {"id":"f4af5728-1720-43b5-bffd-a51ff4dd7eea","entryType":"INCOME","category":"RENTAL_INCOME","amount":18000000.00,"currency":"VND","timing…` | đúng thiết kế |

### I. Liệt kê

| # | Request | Status | Kết quả | Verdict |
|---|---|---|---|---|
| 54 | GET list A ordered | `200` | mảng 24 phần tử | đúng thiết kế |
| 55 | GET list A again | `200` | body byte-identical với case 54 | đúng thiết kế |
| 56 | GET list B | `200` | mảng 0 phần tử | đúng thiết kế |

### J. Ownership

| # | Request | Status | Kết quả | Verdict |
|---|---|---|---|---|
| 57 | A reads own | `200` | entryType=`INCOME`, category=`SALARY`, amount=`45000000.00`, currency=`VND`, timing=`RECURRING`, recurrence=`MONTHLY`, effectiveDate=`2026-01-01`, notes=`Lương tháng` | đúng thiết kế |
| 58 | B reads A's | `404` | `not-found` | đúng thiết kế |
| 59 | B reads nonexistent | `404` | `not-found` | đúng thiết kế |
| 60 | B PUTs A's | `404` | `not-found` | đúng thiết kế |
| 61 | B DELETEs A's | `404` | `not-found` | đúng thiết kế |

### K. Sửa

| # | Request | Status | Kết quả | Verdict |
|---|---|---|---|---|
| 62 | PUT amount+notes | `200` | entryType=`INCOME`, category=`SALARY`, amount=`48000000.00`, currency=`VND`, timing=`RECURRING`, recurrence=`MONTHLY`, effectiveDate=`2026-01-01`, notes=`Lương tháng sau tăng lương` | đúng thiết kế |
| 63 | PUT change type+category | `200` | entryType=`EXPENSE`, category=`LIVING_COSTS`, amount=`48000000.00`, currency=`VND`, timing=`RECURRING`, recurrence=`MONTHLY`, effectiveDate=`2026-01-01`, notes=`Đổi loại` | đúng thiết kế |
| 64 | PUT omit notes | `200` | entryType=`INCOME`, category=`SALARY`, amount=`48000000.00`, currency=`VND`, timing=`RECURRING`, recurrence=`MONTHLY`, effectiveDate=`2026-01-01` | đúng thiết kế |
| 65 | PUT wrong category | `400` | `validation-failed`, errors [{"field": "category", "message": "must belong to the entry type"}] | đúng thiết kế |
| 66 | PUT with unknown id/userId members | `200` | entryType=`INCOME`, category=`SALARY`, amount=`48000000.00`, currency=`VND`, timing=`RECURRING`, recurrence=`MONTHLY`, effectiveDate=`2026-01-01` | đúng thiết kế |

### L. Chứng thực

| # | Request | Status | Kết quả | Verdict |
|---|---|---|---|---|
| 67 | no Authorization | `401` | `unauthorized` | đúng thiết kế |
| 68 | expired token | `401` | `unauthorized` | đúng thiết kế |
| 69 | forged token | `401` | `unauthorized` | đúng thiết kế |
| 70 | no token on member route | `401` | `unauthorized` | đúng thiết kế |

### M. Giao thức

| # | Request | Status | Kết quả | Verdict |
|---|---|---|---|---|
| 71 | GET not-a-uuid | `400` | `malformed-request` | đúng thiết kế |
| 72 | PATCH member | `405` | `method-not-allowed` | đúng thiết kế |
| 73 | DELETE collection | `405` | `method-not-allowed` | đúng thiết kế |
| 74 | text/plain body | `415` | `unsupported-media-type` | đúng thiết kế |
| 75 | truncated JSON | `400` | `malformed-request` | đúng thiết kế |
| 76 | Accept xml | `406` | `not-acceptable` | đúng thiết kế |

### N. Xóa

| # | Request | Status | Kết quả | Verdict |
|---|---|---|---|---|
| 77 | DELETE own | `204` | không body | đúng thiết kế |
| 78 | GET deleted | `404` | `not-found` | đúng thiết kế |
| 79 | DELETE again | `404` | `not-found` | đúng thiết kế |
| 80 | GET list one fewer | `200` | `24 -> 23` | đúng thiết kế |
| 81 | 4 `DELETE` đồng thời một id, 10 vòng | `204`×1 + `404`×3 mỗi vòng | `{"0": [204, 404, 404, 404], "1": [204, 404, 404, 404], "2": [204, 404, 404, 404], "3": [204, 404, 404, 404], "4": [204, 404, 404, 404], "5": [204, 404, 404, 404…` | đúng thiết kế |

### O. Trong PostgreSQL

| # | Request | Status | Kết quả | Verdict |
|---|---|---|---|---|
| 82 | column types | `psql` | `id|uuid|| ⏎ user_id|uuid|| ⏎ entry_type|text|| ⏎ category|text|| ⏎ amount|numeric|19|2 ⏎ currency|text|| ⏎ timing|text|| ⏎ recurrence|text|| ⏎ effective_date|da…` | đúng thiết kế |
| 83 | largest amounts and sum | `psql` | `BUSINESS_INCOME|45000000.00|VND ⏎ BUSINESS_INCOME|26200000.00|VND ⏎ BUSINESS_INCOME|26200000.00|VND ⏎ SUM 350135000.00` | đúng thiết kế |
| 84 | count per user | `psql` | `0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93|23` | đúng thiết kế |
| 85 | probe cash_flow_entries_category_matches_entry_type | `psql` | `ERROR:  new row for relation "cash_flow_entries" violates check constraint "cash_flow_entries_category_matches_entry_type" ⏎ DETAIL:  Failing row contains (0148…` | đúng thiết kế |
| 86 | probe cash_flow_entries_category_matches_entry_type | `psql` | `ERROR:  new row for relation "cash_flow_entries" violates check constraint "cash_flow_entries_category_matches_entry_type" ⏎ DETAIL:  Failing row contains (7227…` | đúng thiết kế |
| 87 | probe cash_flow_entries_amount_is_positive | `psql` | `ERROR:  new row for relation "cash_flow_entries" violates check constraint "cash_flow_entries_amount_is_positive" ⏎ DETAIL:  Failing row contains (5e17e8fd-712e…` | đúng thiết kế |
| 88 | probe cash_flow_entries_currency_is_supported | `psql` | `ERROR:  new row for relation "cash_flow_entries" violates check constraint "cash_flow_entries_currency_is_supported" ⏎ DETAIL:  Failing row contains (cc5e5d81-5…` | đúng thiết kế |
| 89 | probe cash_flow_entries_recurrence_matches_timing | `psql` | `ERROR:  new row for relation "cash_flow_entries" violates check constraint "cash_flow_entries_recurrence_matches_timing" ⏎ DETAIL:  Failing row contains (72c3cc…` | đúng thiết kế |
| 90 | probe cash_flow_entries_recurrence_matches_timing | `psql` | `ERROR:  new row for relation "cash_flow_entries" violates check constraint "cash_flow_entries_recurrence_matches_timing" ⏎ DETAIL:  Failing row contains (8c9bb7…` | đúng thiết kế |
| 91 | probe cash_flow_entries_end_date_not_before_effective_date | `psql` | `ERROR:  new row for relation "cash_flow_entries" violates check constraint "cash_flow_entries_end_date_not_before_effective_date" ⏎ DETAIL:  Failing row contain…` | đúng thiết kế |
| 92 | probe cash_flow_entries_linked_asset_id_fkey | `psql` | `ERROR:  insert or update on table "cash_flow_entries" violates foreign key constraint "cash_flow_entries_linked_asset_id_fkey" ⏎ DETAIL:  Key (linked_asset_id)=…` | đúng thiết kế |
| 99 | probe cash_flow_entries_links_at_most_one_record | `psql` | `ERROR:  new row for relation "cash_flow_entries" violates check constraint "cash_flow_entries_links_at_most_one_record" ⏎ DETAIL:  Failing row contains (e1cd4d8…` | đúng thiết kế |
| 100 | probe cash_flow_entries_linked_liability_id_fkey | `psql` | `ERROR:  insert or update on table "cash_flow_entries" violates foreign key constraint "cash_flow_entries_linked_liability_id_fkey" ⏎ DETAIL:  Key (linked_liabil…` | đúng thiết kế |

### P. Liên kết công nợ

| # | Request | Status | Kết quả | Verdict |
|---|---|---|---|---|
| 93 | link own liability | `201` | entryType=`EXPENSE`, category=`INTEREST_PAYMENT`, amount=`17000000.00`, currency=`VND`, timing=`RECURRING`, recurrence=`MONTHLY`, effectiveDate=`2026-01-05`, notes=`Lãi vay mua nhà` | đúng thiết kế |
| 94 | link other user's liability | `400` | `validation-failed`, errors [{"field": "linkedLiabilityId", "message": "must reference one of the signed-in user's liabilities"}] | đúng thiết kế |
| 95 | link nonexistent liability | `400` | `validation-failed`, errors [{"field": "linkedLiabilityId", "message": "must reference one of the signed-in user's liabilities"}] | đúng thiết kế |
| 96 | both links at once | `400` | `validation-failed`, errors [{"field": "linkedLiabilityId", "message": "must not be combined with linkedAssetId"}] | đúng thiết kế |
| 97 | PUT switch link liability -> asset | `200` | entryType=`EXPENSE`, category=`INTEREST_PAYMENT`, amount=`17000000.00`, currency=`VND`, timing=`RECURRING`, recurrence=`MONTHLY`, effectiveDate=`2026-01-05`, linkedAssetId=`5e55df0a-c471-48ce-a716-f3c008ff119a` | đúng thiết kế |
| 98 | delete linked liability then read entry | `200` | `DELETE liability -> 204 ⏎ {"id":"fd2faff7-cf62-44e6-a6a8-5e7f8476aa19","entryType":"EXPENSE","category":"INTEREST_PAYMENT","amount":17000000.00,"currency":"VND"…` | đúng thiết kế |

## Quan sát không phải lỗi của slice này

- **Jackson và hằng số lạ.** Một `entryType`, `category`, `currency`, `timing` hay `recurrence` không
  tồn tại là `malformed-request` không có `errors`, vì record không dựng được trước khi bean validation
  chạy. Cùng hành vi với slice `assets`, đã ghi trong `docs/api-contract-conventions.md`.
- **`detail` của lỗi bean validation vs lỗi service.** Cả hai đều là `Request validation failed.` ở đây
  vì `ApiExceptionHandler` đặt `safeDetail()` cho `MethodArgumentNotValidException`; khác với quan sát
  ở #53. Không có khác biệt giữa hai đường.
- **`operationId` trong `openapi.json` của slice `assets` bị springdoc đánh số lại** (`read_2`, `delete`,
  `list`, `create` thành tên khác) khi thêm controller mới cùng tên method. Không client nào consume
  `operationId` (conventions doc ghi không có codegen), nhưng #70 nên biết.
- **Liên kết là tham chiếu, không suy ra số tiền.** Case 93 liên kết khoản lãi `17.000.000 ₫` với khoản vay
  có `recurringPaymentAmount` `23.500.000 ₫`; API không so sánh hay dẫn xuất hai con số. Đối soát thuộc
  #74 và #77.


## Verification gap

Không có automated test nào gọi vào `/api/v1/cash-flow-entries`. `ApplicationContextIT` và
`OpenApiContractIT` đã mở rộng (hợp với bảng và route của #59, #62) vẫn bắt được bảng chưa khai báo và
path chưa publish hoặc bị lộ; `ArchitectureRulesTest` vẫn bắt được import chéo slice (`cashflow` không
import `assets` hay `liabilities`: kiểm tra liên kết bằng native query) và trường tiền kiểu float. Hành vi
của resource chỉ được bảo vệ bởi việc chạy lại guide.
