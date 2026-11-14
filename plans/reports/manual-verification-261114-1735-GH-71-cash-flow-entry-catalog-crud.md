# Xác minh thủ công — danh mục dòng tiền và CRUD (#71)

Ngày: 2026-11-14. Lần chạy 1 lúc 17:56 ICT trên working tree trước khi commit; lần chạy 2 lúc 18:00 ICT
trên đúng mã nguồn của commit `b6f4c9c` (`feat(api): add the cash-flow entry catalog and its CRUD`), nhánh
`thientrinhcoder/feat/issue-71-cash-flow-entry-catalog-crud`, cắt từ `main` tại `6e57e92`. Không có thay
đổi code nào giữa lần chạy 2 và commit đó.

Đây là bằng chứng hành vi cho issue #71. Product Owner chỉ đạo task này ship **không có unit test hay
integration test mới**; thay vào đó toàn bộ scenario được viết trước ở
[`phase-01-test-scenarios.md`](../261114-1735-GH-71-cash-flow-entry-catalog-crud/phase-01-test-scenarios.md),
rồi chạy thật qua HTTP vào PostgreSQL trong Docker và ghi lại đúng điều API trả về. Mọi status và body
dưới đây được chép từ lần chạy thật, không viết tay. Quy trình tái hiện ở
[`docs/manual-test-guide-cash-flow-entries.md`](../../docs/manual-test-guide-cash-flow-entries.md).

**Đọc file này theo hai phần.** Phần *Lần chạy 1* ghi ba case không đạt và vì sao. Phần *Lần chạy 2* là
toàn bộ 92 case trên commit cuối, mọi case đúng thiết kế.

## Môi trường

| | |
|---|---|
| Database | Container `postgres:18.6-alpine3.24` dùng một lần, tên `rikkaus-pg-issue-71`, cổng `127.0.0.1:5471`, tạo mới cho mỗi lần chạy và xóa sau đó. Volume `rikkaus-postgres-data` không bị chạm |
| Ứng dụng | Chạy qua `spring-boot:run` trên JDK 25, `--spring.profiles.active=local --server.port=8071`, `DB_URL=jdbc:postgresql://localhost:5471/rikkaus` |
| Migration | Flyway: `Successfully applied 5 migrations to schema "public", now at version v7` (V1, V2, V3, V4, V7 — V5 thuộc PR #147 và V6 thuộc #62, chưa có trên nhánh này). `ddl-auto: validate` chấp nhận mapping, context khởi động trong 2,6 giây: đó là bằng chứng `V7__cash_flow_entries.sql` và entity `CashFlowEntry` khớp từng cột |
| Người dùng | Hai row chèn thẳng: user A `0f2c8d14-…0c93` (An Nguyễn), user B `7b41e590-…a842` (Bích Trần) |
| Token | HS256 mint bằng secret phát triển của profile `local`, đúng bộ claim `AccessTokenService` phát hành; thêm một token hết hạn và một token ký bằng secret khác. Request đi qua `BearerTokenAuthenticationFilter` và `AccessTokenService.verify` thật |
| Tài sản để liên kết | Tạo qua `POST /api/v1/assets`: `Căn hộ Thảo Điền` (`6.100.000.000 ₫`) của A và `Tiền mặt của Bích` (`5.000.000 ₫`) của B |
| Gate build | `./services/api/mvnw -f services/api/pom.xml verify` xanh không dùng update flag: 81 unit test, 50 integration test, `openapi.json` đã commit khớp ứng dụng |

## Lần chạy 1 — 89/92, ba case không đạt

Chạy trên working tree trước commit. Ba case không đạt, mỗi case được xem lại để phân biệt lỗi code với
lỗi của kịch bản kiểm tra.

| # | Điều xảy ra | Phân loại | Xử lý |
|---|---|---|---|
| 28 | `amount: "45000000"` trả `201` nhưng body echo `"amount":45000000` (scale 0), trong khi `GET` sau đó đọc từ `NUMERIC(19, 2)` ra `45000000.00`. Cùng một entry, hai cách viết tùy vào request nào đọc nó | **Lỗi code**, nhỏ nhưng thật | `CashFlowEntryService.validated` đặt `setScale(2)` trước khi lưu; `@Digits` đã chặn quá hai số lẻ nên không có gì bị làm tròn. Lần 2: `201` với `45000000.00` ở cả `POST` và `GET` |
| 83 | Kịch bản kiểm tra tìm `48000000.00` trong ba dòng lớn nhất, nhưng entry lương đã bị xóa ở case 77 trước đó | Lỗi của kịch bản kiểm tra | Assertion đổi thành "không có mũ, mọi số đều hai số lẻ". Dữ liệu trong DB vốn đã đúng |
| 85 | Probe `entry_type = 'TRANSFER'` bị bảng từ chối, nhưng constraint được báo là `cash_flow_entries_category_matches_entry_type`, không phải `cash_flow_entries_entry_type_is_known` như kịch bản kỳ vọng | Kỳ vọng viết sai | Mọi category đều gắn với `INCOME` hoặc `EXPENSE`, nên loại lạ luôn vi phạm cả constraint ghép cặp, và PostgreSQL kiểm tra theo thứ tự tên. Bảng vẫn từ chối; phase 1 và guide đã sửa kỳ vọng. Constraint `entry_type_is_known` giữ lại như phát biểu tường minh |

89 case còn lại đạt ở lần 1 và đạt lại ở lần 2 với cùng status.

## Lần chạy 2 — 92/92 trên commit `b6f4c9c`

**92 case: 81 qua HTTP (case 81 là 40 request đồng thời) và 11 thẳng vào PostgreSQL.** Mọi case đúng
thiết kế. Phân bố status của 80 request HTTP đơn lẻ:

| Status | Số case |
|---|---|
| `200` | 11 |
| `201` | 23 |
| `204` | 1 |
| `400` | 31 |
| `401` | 4 |
| `404` | 6 |
| `405` | 2 |
| `406` | 1 |
| `415` | 1 |

Body đáng đọc nguyên văn, trước khi vào bảng:

Case 1, lương tháng, và cách trường optional vắng đọc lại là `null`:

```
{"id":"abcf5fd8-8731-49b5-8ec0-21d777d4456c","entryType":"INCOME","category":"SALARY","amount":45000000.00,"currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-01","endDate":null,"linkedAssetId":null,"notes":"Lương tháng","createdAt":"2026-11-14T11:00:18.308523Z","updatedAt":"2026-11-14T11:00:18.308523Z"}
```

Case 4, cổ tức 150 USD, **không có số VND nào** trong body — slice không quy đổi:

```
{"id":"a5bd0419-8992-4652-af28-c9a0a700d4d7","entryType":"INCOME","category":"DIVIDEND","amount":150.00,"currency":"USD","timing":"ONE_TIME","recurrence":null,"effectiveDate":"2026-09-12","endDate":null,"linkedAssetId":null,"notes":null,"createdAt":"2026-11-14T11:00:18.325269Z","updatedAt":"2026-11-14T11:00:18.325269Z"}
```

Case 41, một lần kèm cả `recurrence` và `endDate`: hai lỗi trong một lần trả về, không có lỗi thứ ba về
thứ tự ngày:

```
{"detail":"Request validation failed.","instance":"/api/v1/cash-flow-entries","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must be omitted for a one-time entry","field":"recurrence"},{"message":"must be omitted for a one-time entry","field":"endDate"}],"correlationId":"7e18796c-83a9-4aef-9ecb-db4c81b2b57d"}
```

Case 48 (tài sản của B) và 49 (tài sản không tồn tại), cạnh nhau. Khác nhau **chỉ** ở `correlationId`:

```
{"detail":"Request validation failed.","instance":"/api/v1/cash-flow-entries","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must reference one of the signed-in user's assets","field":"linkedAssetId"}],"correlationId":"2d2bd02a-bcbd-42e1-8dc8-e179db9a90d3"}
{"detail":"Request validation failed.","instance":"/api/v1/cash-flow-entries","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must reference one of the signed-in user's assets","field":"linkedAssetId"}],"correlationId":"9ae208fd-6473-4659-9a3f-744498138263"}
```

Case 58 (B đọc entry của A) và 59 (B đọc id không tồn tại). Giống nhau ở `type`, `title`, `status`,
`detail`; khác ở `instance` — request URI theo định nghĩa RFC 9457 — và `correlationId`:

```
{"detail":"Resource not found.","instance":"/api/v1/cash-flow-entries/abcf5fd8-8731-49b5-8ec0-21d777d4456c","status":404,"title":"Resource not found","type":"urn:rikkaus:problem:not-found","correlationId":"36afe766-efd0-403f-914a-9b36d56c6bc9"}
{"detail":"Resource not found.","instance":"/api/v1/cash-flow-entries/00000000-0000-4000-8000-000000000000","status":404,"title":"Resource not found","type":"urn:rikkaus:problem:not-found","correlationId":"91efc7d5-b8b2-4ade-b951-d10e459a8be7"}
```

Case 53, xóa căn hộ rồi đọc lại khoản tiền thuê: `DELETE /assets` trả `204`, entry còn và
`linkedAssetId` thành `null` (`ON DELETE SET NULL`), `updatedAt` không đổi vì không có request nào sửa
entry:

```
{"id":"2b79f712-28d4-441a-9957-78bdad9eb6b8","entryType":"INCOME","category":"RENTAL_INCOME","amount":18000000.00,"currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-02-01","endDate":null,"linkedAssetId":null,"notes":null,"createdAt":"2026-11-14T11:00:18.461385Z","updatedAt":"2026-11-14T11:00:18.490249Z"}
```

Case 67, không có token:

```
{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized","correlationId":"ae4327a5-cabf-4cf6-b5dc-d15960fb3f6a"}
```

Case 81, bốn `DELETE` đồng thời một id, mười vòng — mỗi vòng đúng một `204` và ba `404`, không `500`:

```
{"0": [204, 404, 404, 404], "1": [204, 404, 404, 404], "2": [204, 404, 404, 404], "3": [204, 404, 404, 404], "4": [204, 404, 404, 404], "5": [204, 404, 404, 404], "6": [204, 404, 404, 404], "7": [204, 404, 404, 404], "8": [204, 404, 404, 404], "9": [204, 404, 404, 404]}
```

Case 82, kiểu cột đọc từ `information_schema.columns` — `amount` là `numeric(19,2)`, không có
`double precision`:

```
amount|numeric|19|2
effective_date|date||
end_date|date||
linked_asset_id|uuid||
```

Case 83, ba số lớn nhất và tổng VND do PostgreSQL cộng — nguyên, không mũ:

```
BUSINESS_INCOME|45000000.00|VND
BUSINESS_INCOME|26200000.00|VND
BUSINESS_INCOME|26200000.00|VND
SUM 333135000.00
```

Case 86, bảng tự từ chối cặp sai không qua API:

```
ERROR:  new row for relation "cash_flow_entries" violates check constraint "cash_flow_entries_category_matches_entry_type"
```

`count(*)` trước và sau tám probe: `22` và `22`.

## Toàn bộ 92 case

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
| 17 | category SALARY under EXPENSE | `400` | `validation-failed`, errors [{"message": "must belong to the entry type", "field": "category"}] | đúng thiết kế |
| 18 | category HOUSING under INCOME | `400` | `validation-failed`, errors [{"message": "must belong to the entry type", "field": "category"}] | đúng thiết kế |

### C. Thiếu từng trường bắt buộc

| # | Request | Status | Kết quả | Verdict |
|---|---|---|---|---|
| 19 | missing entryType | `400` | `validation-failed`, errors [{"message": "must not be null", "field": "entryType"}] | đúng thiết kế |
| 20 | missing category | `400` | `validation-failed`, errors [{"message": "must not be null", "field": "category"}] | đúng thiết kế |
| 21 | missing amount | `400` | `validation-failed`, errors [{"message": "must not be null", "field": "amount"}] | đúng thiết kế |
| 22 | missing currency | `400` | `validation-failed`, errors [{"message": "must not be null", "field": "currency"}] | đúng thiết kế |
| 23 | missing timing | `400` | `validation-failed`, errors [{"message": "must not be null", "field": "timing"}] | đúng thiết kế |
| 24 | missing effectiveDate | `400` | `validation-failed`, errors [{"message": "must not be null", "field": "effectiveDate"}] | đúng thiết kế |

### D. Số tiền

| # | Request | Status | Kết quả | Verdict |
|---|---|---|---|---|
| 25 | amount 0 | `400` | `validation-failed`, errors [{"message": "must be greater than zero", "field": "amount"}] | đúng thiết kế |
| 26 | amount -1000000.00 | `400` | `validation-failed`, errors [{"message": "must be greater than zero", "field": "amount"}] | đúng thiết kế |
| 27 | amount 1000000.123 | `400` | `validation-failed`, errors [{"message": "must have at most 17 integer digits and 2 decimals", "field": "amount"}] | đúng thiết kế |
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
| 34 | timing/date case 34 | `400` | `validation-failed`, errors [{"message": "must be omitted for a one-time entry", "field": "recurrence"}] | đúng thiết kế |
| 35 | timing/date case 35 | `400` | `validation-failed`, errors [{"message": "must be present for a recurring entry", "field": "recurrence"}] | đúng thiết kế |
| 36 | timing/date case 36 | `400` | `validation-failed`, errors [{"message": "must be omitted for a one-time entry", "field": "endDate"}] | đúng thiết kế |
| 37 | timing/date case 37 | `400` | `validation-failed`, errors [{"message": "must not be before the effective date", "field": "endDate"}] | đúng thiết kế |
| 38 | timing/date case 38 | `201` | entryType=`INCOME`, category=`BUSINESS_INCOME`, amount=`26200000.00`, currency=`VND`, timing=`RECURRING`, recurrence=`MONTHLY`, effectiveDate=`2026-09-05`, endDate=`2026-09-05` | đúng thiết kế |
| 39 | timing/date case 39 | `201` | entryType=`INCOME`, category=`BUSINESS_INCOME`, amount=`26200000.00`, currency=`VND`, timing=`RECURRING`, recurrence=`MONTHLY`, effectiveDate=`2026-09-05` | đúng thiết kế |
| 40 | timing/date case 40 | `201` | entryType=`INCOME`, category=`BUSINESS_INCOME`, amount=`26200000.00`, currency=`VND`, timing=`ONE_TIME`, effectiveDate=`2027-01-01` | đúng thiết kế |
| 41 | timing/date case 41 | `400` | `validation-failed`, errors [{"message": "must be omitted for a one-time entry", "field": "recurrence"}, {"message": "must be omitted for a one-time entry", "field": "endDate"}] | đúng thiết kế |
| 42 | effectiveDate 01/01/2026 | `400` | `malformed-request` | đúng thiết kế |

### G. Ghi chú

| # | Request | Status | Kết quả | Verdict |
|---|---|---|---|---|
| 43 | notes 2001 chars | `400` | `validation-failed`, errors [{"message": "must be at most 2000 characters", "field": "notes"}] | đúng thiết kế |
| 44 | notes blank | `201` | entryType=`INCOME`, category=`BUSINESS_INCOME`, amount=`26200000.00`, currency=`VND`, timing=`ONE_TIME`, effectiveDate=`2026-09-10` | đúng thiết kế |
| 45 | notes trimmed | `201` | entryType=`INCOME`, category=`BUSINESS_INCOME`, amount=`26200000.00`, currency=`VND`, timing=`ONE_TIME`, effectiveDate=`2026-09-10`, notes=`Lương tháng 11.` | đúng thiết kế |
| 46 | notes NUL | `400` | `validation-failed`, errors [{"message": "must not contain a null character", "field": "notes"}] | đúng thiết kế |

### H. Liên kết tài sản

| # | Request | Status | Kết quả | Verdict |
|---|---|---|---|---|
| 47 | link own asset | `201` | entryType=`INCOME`, category=`RENTAL_INCOME`, amount=`18000000.00`, currency=`VND`, timing=`RECURRING`, recurrence=`MONTHLY`, effectiveDate=`2026-02-01`, linkedAssetId=`3c2b5eae-be76-44a1-87ed-e3541ad2a419`, notes=`Tiền thuê căn hộ Thảo Điền` | đúng thiết kế |
| 48 | link other user's asset | `400` | `validation-failed`, errors [{"message": "must reference one of the signed-in user's assets", "field": "linkedAssetId"}] | đúng thiết kế |
| 49 | link nonexistent asset | `400` | `validation-failed`, errors [{"message": "must reference one of the signed-in user's assets", "field": "linkedAssetId"}] | đúng thiết kế |
| 50 | linkedAssetId not a uuid | `400` | `malformed-request` | đúng thiết kế |
| 51 | PUT relink to other user's asset | `400` | `validation-failed`, errors [{"message": "must reference one of the signed-in user's assets", "field": "linkedAssetId"}] | đúng thiết kế |
| 52 | PUT omit linkedAssetId | `200` | entryType=`INCOME`, category=`RENTAL_INCOME`, amount=`18000000.00`, currency=`VND`, timing=`RECURRING`, recurrence=`MONTHLY`, effectiveDate=`2026-02-01` | đúng thiết kế |
| 53 | delete linked asset then read entry | `200` | `DELETE asset -> 204 ⏎ {"id":"2b79f712-28d4-441a-9957-78bdad9eb6b8","entryType":"INCOME","category":"RENTAL_INCOME","amount":18000000.00,"currency":"VND","timing…` | đúng thiết kế |

### I. Liệt kê

| # | Request | Status | Kết quả | Verdict |
|---|---|---|---|---|
| 54 | GET list A ordered | `200` | mảng 23 phần tử | đúng thiết kế |
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
| 65 | PUT wrong category | `400` | `validation-failed`, errors [{"message": "must belong to the entry type", "field": "category"}] | đúng thiết kế |
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
| 80 | GET list one fewer | `200` | `23 -> 22` | đúng thiết kế |
| 81 | 4 `DELETE` đồng thời một id, 10 vòng | `204`×1 + `404`×3 mỗi vòng | `{"0": [204, 404, 404, 404], "1": [204, 404, 404, 404], "2": [204, 404, 404, 404], "3": [204, 404, 404, 404], "4": [204, 404, 404, 404], "5": [204, 404, 404, 404…` | đúng thiết kế |

### O. Trong PostgreSQL

| # | Request | Status | Kết quả | Verdict |
|---|---|---|---|---|
| 82 | column types | `psql` | `id|uuid|| ⏎ user_id|uuid|| ⏎ entry_type|text|| ⏎ category|text|| ⏎ amount|numeric|19|2 ⏎ currency|text|| ⏎ timing|text|| ⏎ recurrence|text|| ⏎ effective_date|da…` | đúng thiết kế |
| 83 | largest amounts and sum | `psql` | `BUSINESS_INCOME|45000000.00|VND ⏎ BUSINESS_INCOME|26200000.00|VND ⏎ BUSINESS_INCOME|26200000.00|VND ⏎ SUM 333135000.00` | đúng thiết kế |
| 84 | count per user | `psql` | `0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93|22` | đúng thiết kế |
| 85 | probe cash_flow_entries_category_matches_entry_type | `psql` | `ERROR:  new row for relation "cash_flow_entries" violates check constraint "cash_flow_entries_category_matches_entry_type" ⏎ DETAIL:  Failing row contains (5836…` | đúng thiết kế |
| 86 | probe cash_flow_entries_category_matches_entry_type | `psql` | `ERROR:  new row for relation "cash_flow_entries" violates check constraint "cash_flow_entries_category_matches_entry_type" ⏎ DETAIL:  Failing row contains (4853…` | đúng thiết kế |
| 87 | probe cash_flow_entries_amount_is_positive | `psql` | `ERROR:  new row for relation "cash_flow_entries" violates check constraint "cash_flow_entries_amount_is_positive" ⏎ DETAIL:  Failing row contains (cbda1bf6-8f9b…` | đúng thiết kế |
| 88 | probe cash_flow_entries_currency_is_supported | `psql` | `ERROR:  new row for relation "cash_flow_entries" violates check constraint "cash_flow_entries_currency_is_supported" ⏎ DETAIL:  Failing row contains (3b0a0ad7-0…` | đúng thiết kế |
| 89 | probe cash_flow_entries_recurrence_matches_timing | `psql` | `ERROR:  new row for relation "cash_flow_entries" violates check constraint "cash_flow_entries_recurrence_matches_timing" ⏎ DETAIL:  Failing row contains (e46db6…` | đúng thiết kế |
| 90 | probe cash_flow_entries_recurrence_matches_timing | `psql` | `ERROR:  new row for relation "cash_flow_entries" violates check constraint "cash_flow_entries_recurrence_matches_timing" ⏎ DETAIL:  Failing row contains (28ac92…` | đúng thiết kế |
| 91 | probe cash_flow_entries_end_date_not_before_effective_date | `psql` | `ERROR:  new row for relation "cash_flow_entries" violates check constraint "cash_flow_entries_end_date_not_before_effective_date" ⏎ DETAIL:  Failing row contain…` | đúng thiết kế |
| 92 | probe cash_flow_entries_linked_asset_id_fkey | `psql` | `ERROR:  insert or update on table "cash_flow_entries" violates foreign key constraint "cash_flow_entries_linked_asset_id_fkey" ⏎ DETAIL:  Key (linked_asset_id)=…` | đúng thiết kế |

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
- **Liên kết công nợ chưa có.** `linkedLiabilityId` chờ bảng `wealth.liabilities` của #62; phase 6 của plan
  ghi các scenario bổ sung sẽ chạy khi merge.

## Verification gap

Không có automated test nào gọi vào `/api/v1/cash-flow-entries`. `ApplicationContextIT` và
`OpenApiContractIT` đã mở rộng vẫn bắt được bảng chưa khai báo và path chưa publish hoặc bị lộ;
`ArchitectureRulesTest` vẫn bắt được import chéo slice (`cashflow` không import `assets`: kiểm tra
liên kết bằng native query) và trường tiền kiểu float. Hành vi của resource chỉ được bảo vệ bởi việc
chạy lại guide này.
