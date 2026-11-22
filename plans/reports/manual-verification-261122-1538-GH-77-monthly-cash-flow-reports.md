# Xác minh thủ công — #77 báo cáo dòng tiền tháng

**Kết quả: 40/40 case đạt (60/60 phép kiểm) ở lần chạy thứ hai trên commit `7683dee`.** Lần chạy đầu trên
cùng commit đạt 58/60; hai phép kiểm fail là lỗi của chính script kiểm, không phải của API, được ghi ở
mục "Lần chạy 1" bên dưới. Không dòng code nào đổi giữa hai lần chạy.

| | |
|---|---|
| Commit chạy | `7683dee feat(api): add the monthly cash-flow report` |
| Ngày | 22/11/2026, 15:56 và 15:57 (+07) |
| Môi trường | PostgreSQL `postgres:18.6-alpine3.24` container `rikkaus-pg-issue-77` ở `127.0.0.1:5477`, tạo mới cho mỗi lần chạy; API `spring-boot:run` profile `local` cổng `8077`, JDK 25; Flyway `v12` |
| Dữ liệu | ba user A, B, C chèn thẳng `wealth.users`; tài sản, tỷ giá và 33 entry tạo qua API (fixture ở phase 1) |
| Cách chạy | `setup.sh`, `recon.jq`, `cases.sh` đúng như trong `docs/manual-test-guide-cash-flow-reports.md`; token và id chỉ nằm trong thư mục nháp ngoài repo |
| Log API | 0 dòng `ERROR` trong cả lần chạy 2 |
| `clean verify` | xanh không flag trên `7683dee`: 81 unit test, 50 integration test |

## Lần chạy 1 — 58/60, hai phép kiểm fail ở script

```text
M15 FAIL reconciliation of M11 (all true) :: {..."income_shares_sum_100":false,...}
M40 FAIL schema unchanged, Flyway max 12 :: {"tables":"asset_valuations,assets,cash_flow_entries,feedback_entries,goals,liabilities,onboarding_progress,refresh_tokens,user_settings,users","flyway":12}
```

1. **M15 trên body của B.** Body đúng: `sharePercent` là `33.4`, `33.3`, `33.3` (M11 đạt, cộng theo phần
   mười ra `1000`). Phép so trong `recon.jq` cộng số thực, và trong `jq` `33.4 + 33.3 + 33.3` là
   `99.99999999999999`. Sửa: `recon.jq` cộng tỷ trọng theo phần mười (`× 10 | round`) và so với `1000`, như M10.
2. **M40 danh sách bảng.** Danh sách kỳ vọng tôi viết tay sai: có bảng `sessions` không tồn tại và sai thứ
   tự chữ cái. Danh sách thật trùng khít danh sách `ApplicationContextIT` trên `main`, và Flyway vẫn `12`,
   nên schema không đổi. Sửa: kỳ vọng thành danh sách thật.

Lần chạy 2 dùng database mới tinh, chạy lại toàn bộ `setup.sh` và `cases.sh` sau khi sửa hai dòng trên.

## Lần chạy 2 — 60/60

### A. Tháng 09/2026 của A (M01–M07): đạt

M01 `200`, phần đầu body thật:

```json
{"month":"2026-09","from":"2026-09-01","to":"2026-09-30","currency":"VND","exchangeRate":{"usdToVndRate":25100.000000,"effectiveDate":"2026-09-12"},"complete":true,"totals":{"income":180000000.00,"expense":92000000.00,"net":88000000.00},"unconverted":{"occurrenceCount":0,"income":0.00,"expense":0.00}}
```

Mười lăm dòng `occurrences` thật (M02 tập id đúng R1..R6, X1..X9; M03 không có N1..N6; M04 kỳ tháng, quý,
năm; M05 dời cuối tháng; M06 USD quy đổi):

| Ngày | Ghi chú | Loại / danh mục | Kỳ | Dời | Gốc | VND |
|---|---|---|---|---|---|---|
| 2026-09-05 | Lương tháng | `INCOME` `SALARY` | 9 | false | 95000000.00 VND | 95000000.00 |
| 2026-09-05 | Điện nước, phí quản lý, internet | `EXPENSE` `HOUSING` | 9 | false | 18750000.00 VND | 18750000.00 |
| 2026-09-08 | Học phí | `EXPENSE` `EDUCATION` | 3 | false | 12800000.00 VND | 12800000.00 |
| 2026-09-10 | Cho thuê căn hộ Thảo Điền | `INCOME` `RENTAL_INCOME` | 9 | false | 18000000.00 VND | 18000000.00 |
| 2026-09-12 | Hoàn thuế TNCN 2025 | `INCOME` `OTHER_INCOME` | 1 | false | 6180000.00 VND | 6180000.00 |
| 2026-09-15 | Lãi tiền gửi USD | `INCOME` `DEPOSIT_INTEREST` | 2 | false | 1200.00 USD | 30120000.00 |
| 2026-09-15 | Phí bảo hiểm sức khỏe gia đình | `EXPENSE` `INSURANCE` | 2 | false | 5400000.00 VND | 5400000.00 |
| 2026-09-18 | Khám sức khỏe định kỳ | `EXPENSE` `HEALTHCARE` | 1 | false | 4650000.00 VND | 4650000.00 |
| 2026-09-22 | Chi khác | `EXPENSE` `OTHER_EXPENSE` | 1 | false | 2500000.00 VND | 2500000.00 |
| 2026-09-25 | Thuế TNCN tạm nộp | `EXPENSE` `TAX_AND_FEES` | 1 | false | 8250000.00 VND | 8250000.00 |
| 2026-09-28 | Lãi vay mua nhà | `EXPENSE` `INTEREST_PAYMENT` | 9 | false | 6250000.00 VND | 6250000.00 |
| 2026-09-28 | Trả gốc vay mua nhà | `EXPENSE` `DEBT_PRINCIPAL_PAYMENT` | 9 | false | 10000000.00 VND | 10000000.00 |
| 2026-09-28 | Doanh thu tư vấn quý 3 | `INCOME` `BUSINESS_INCOME` | 1 | false | 26200000.00 VND | 26200000.00 |
| 2026-09-30 | Chi phí sinh hoạt gia đình | `EXPENSE` `LIVING_COSTS` | 9 | true | 23400000.00 VND | 23400000.00 |
| 2026-09-30 | Cổ tức VNM | `INCOME` `DIVIDEND` | 3 | false | 4500000.00 VND | 4500000.00 |

M07 tháng kề: `2026-08` có 8 dòng, gồm N4 `2026-08-15` (`endDate` tính vào) và N1 `2026-08-31`, thu
`113.000.000 ₫`, chi `63.400.000 ₫`; `2026-10` có 9 dòng, gồm N2 `2026-10-01`, N6 `2026-10-05`, N3 `2026-10-20`.
Tập (`sourceEntryId`, `occurrenceDate`) của ba tháng ghép lại bằng đúng
`/cash-flow-occurrences?from=2026-08-01&to=2026-10-31`, không trùng, không hụt.

### B. Composition (M08–M11): đạt

```json
"incomeComposition":[{"category":"SALARY","amount":95000000.00,"sharePercent":52.8,...},{"category":"DEPOSIT_INTEREST","amount":30120000.00,"sharePercent":16.7,...},{"category":"BUSINESS_INCOME","amount":26200000.00,"sharePercent":14.6,...},{"category":"RENTAL_INCOME","amount":18000000.00,"sharePercent":10.0,...},{"category":"OTHER_INCOME","amount":6180000.00,"sharePercent":3.4,...},{"category":"DIVIDEND","amount":4500000.00,"sharePercent":2.5,...}]
"expenseComposition":[LIVING_COSTS 25.4, HOUSING 20.4, EDUCATION 13.9, DEBT_PRINCIPAL_PAYMENT 10.9, TAX_AND_FEES 9.0, INTEREST_PAYMENT 6.8, INSURANCE 5.9, HEALTHCARE 5.0, OTHER_EXPENSE 2.7]
```

`HEALTHCARE` là `5.0` (làm tròn riêng sẽ là `5.1`, cột cộng `100.1`); hai cột cộng đúng `100.0`. M11, body
thật của B:

```json
[{"category":"BUSINESS_INCOME","amount":10000000.00,"sharePercent":33.4,"occurrenceCount":1,"partial":false},{"category":"OTHER_INCOME","amount":10000000.00,"sharePercent":33.3,"occurrenceCount":1,"partial":false},{"category":"SALARY","amount":10000000.00,"sharePercent":33.3,"occurrenceCount":1,"partial":false}]
```

### C. Thu nhập thụ động (M12–M14): đạt

M12: `{"total":22500000.00,"sharePercentOfIncome":12.5,"byAsset":[AS1 18000000.00 10.0, AS2 4500000.00 2.5],"unassigned":{"amount":30120000.00,"occurrenceCount":1,"partial":false}}`.
M13: R3 dưới AS1, R4 dưới AS2, R5 không link nên ở `unassigned`; R1, R2, R6 không ở đâu trong `passiveIncome`.
M14: `DELETE /assets/{AS2}` → `204`, đọc lại:

```json
{"total":18000000.00,"sharePercentOfIncome":10.0,"byAsset":[{"assetId":"de3e174f-6522-49b6-9cb3-fc52e437397e","amount":18000000.00,"sharePercentOfIncome":10.0,"occurrenceCount":1,"partial":false}],"unassigned":{"amount":34620000.00,"occurrenceCount":2,"partial":false}}
```

R4 có `linkedAssetId` `null` (khóa ngoại `ON DELETE SET NULL`), tổng thu vẫn `180.000.000 ₫`.

### D. Reconciliation bằng `jq` (M15): đạt trên 9 body

`recon.jq` chạy trên body thật của M01, M11, M16, M20, M07a, M07b, M19, M14 và M17; mọi body cho đủ 20 phép so
`true`. Ví dụ M01:

```json
{"income_eq_sum_income_lines":true,"expense_eq_sum_expense_lines":true,"net_eq_income_minus_expense":true,"income_eq_sum_income_composition":true,"expense_eq_sum_expense_composition":true,"each_income_row_eq_its_lines":true,"each_expense_row_eq_its_lines":true,"composition_covers_every_line":true,"income_shares_sum_100":true,"expense_shares_sum_100":true,"passive_total_eq_sum_by_asset":true,"passive_total_eq_linked_income_lines":true,"each_asset_eq_its_lines":true,"unassigned_eq_its_lines":true,"unconverted_count_eq_null_lines":true,"unconverted_income_eq_original_usd":true,"unconverted_expense_eq_original_usd":true,"complete_iff_nothing_unconverted":true,"each_line_amount_is_its_conversion":true,"lines_ordered_by_date_then_id":true}
```

### E. Tiền tệ (M16–M18): đạt

M16, C chưa khai tỷ giá, body thật (bỏ `occurrences`):

```json
{"month":"2026-09","from":"2026-09-01","to":"2026-09-30","currency":"VND","exchangeRate":null,"complete":false,"totals":{"income":40000000.00,"expense":15000000.00,"net":25000000.00},"unconverted":{"occurrenceCount":2,"income":150.00,"expense":200.00},"incomeComposition":[{"category":"SALARY","amount":40000000.00,"sharePercent":100.0,"occurrenceCount":1,"partial":false},{"category":"DIVIDEND","amount":0.00,"sharePercent":0.0,"occurrenceCount":1,"partial":true}],"expenseComposition":[{"category":"HOUSING","amount":15000000.00,"sharePercent":100.0,"occurrenceCount":1,"partial":false},{"category":"LIVING_COSTS","amount":0.00,"sharePercent":0.0,"occurrenceCount":1,"partial":true}],"passiveIncome":{"total":0.00,"sharePercentOfIncome":0.0,"byAsset":[],"unassigned":{"amount":0.00,"occurrenceCount":1,"partial":true}}}
```

Hai dòng USD: `["2026-09-10","LIVING_COSTS",200.00,"USD",null]`, `["2026-09-30","DIVIDEND",150.00,"USD",null]`.
M17, sau `PUT /settings/exchange-rate` `25100.000000` hiệu lực `2026-09-12`:

```json
{"totals":{"income":43765000.00,"expense":20020000.00,"net":23745000.00},"unconverted":{"occurrenceCount":0,"income":0.00,"expense":0.00},"incomeComposition":[{"category":"SALARY","amount":40000000.00,"sharePercent":91.4,...},{"category":"DIVIDEND","amount":3765000.00,"sharePercent":8.6,...}],"expenseComposition":[{"category":"HOUSING","amount":15000000.00,"sharePercent":74.9,...},{"category":"LIVING_COSTS","amount":5020000.00,"sharePercent":25.1,...}],"unassigned":{"amount":3765000.00,"occurrenceCount":1,"partial":false}}
```

M18: B không khai tỷ giá, chỉ có VND → `exchangeRate` `null`, `complete` `true`.

### F. Tháng rỗng, tháng tương lai (M19–M20): đạt

M19 `200`:

```json
{"month":"2025-06","from":"2025-06-01","to":"2025-06-30","currency":"VND","exchangeRate":{"usdToVndRate":25100.000000,"effectiveDate":"2026-09-12"},"complete":true,"totals":{"income":0.00,"expense":0.00,"net":0.00},"unconverted":{"occurrenceCount":0,"income":0.00,"expense":0.00},"incomeComposition":[],"expenseComposition":[],"passiveIncome":{"total":0.00,"sharePercentOfIncome":null,"byAsset":[],"unassigned":{"amount":0.00,"occurrenceCount":0,"partial":false}},"occurrences":[]}
```

M20 `2026-12`: 9 dòng, `{"income":117500000.00,"expense":76200000.00,"net":41300000.00}`; X1 rơi `2026-12-31`
không dời, R4 rơi `2026-12-30`.

### G. Truy nguồn và xác định (M21–M22): đạt

Cả 15 dòng của M01 đọc lại bằng `GET /cash-flow-entries/{sourceEntryId}` `200` với cùng loại, danh mục, số
tiền gốc, tiền tệ, `linkedAssetId`, ghi chú. Bộ (`sourceEntryId`, `occurrenceDate`, `sequence`,
`shiftedToMonthEnd`) bằng đúng và cùng thứ tự với `/cash-flow-occurrences?from=2026-09-01&to=2026-09-30`.
Hai lần đọc cho cùng `sha256` `e6ff6088…bcbdc7cb3`.

### H. Ownership (M23–M24): đạt

B thấy đúng 4 entry của mình, thu `30.000.000 ₫`, chi `12.000.000 ₫`, net `18.000.000 ₫`. A vẫn 15 dòng,
không dòng nào của B hay C.

### I. Input sai (M25–M34): đạt

| # | Query | Status | `errors` |
|---|---|---|---|
| M25 | không `month` | 400 | `[{"message":"must be present","field":"month"}]` |
| M26 | `month=` | 400 | như M25 |
| M27 | `2026-9` | 400 | `[{"message":"must be a calendar month in the form YYYY-MM","field":"month"}]` |
| M28 | `2026-13` | 400 | như M27 |
| M29 | `2026-00` | 400 | như M27 |
| M30 | `09/2026` | 400 | như M27; body không chứa `09/2026` |
| M31 | `2026-09-01` | 400 | như M27 |
| M32 | `month=2026-09&month=2026-10` | 400 | như M27 |
| M33 | `abc` | 400 | như M27 |
| M34 | `%2B2026-09` | 400 | như M27 |

Mọi body có `type` `urn:rikkaus:problem:validation-failed`.

### J. Xác thực, method, media type, contract (M35–M40): đạt

M35 không token: `{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized"}`
(bỏ `correlationId`); M36 token hết hạn và M37 token ký bằng khóa khác cho body giống hệt. M38 `POST`,
`PUT`, `DELETE` → `405` `Allow: GET`. M39 `Accept: application/xml` → `406`. M40 `openapi.json`:
`{"methods":["get"],"params":[["month",true,"query"]]}`; bảng `wealth` là
`asset_valuations,assets,cash_flow_entries,feedback_entries,goals,liabilities,onboarding_progress,refresh_tokens,user_settings,users`,
Flyway `12`.

## Lần chạy 3 — 60/60 sau khi sửa cảnh báo CodeQL

CodeQL trên PR #158 báo `java/comparison-with-wider-type` ở vòng lặp chia phần dư lớn nhất: chỉ số `int` so
với số phần mười còn thiếu kiểu `long`. Commit `9001eda` đổi mọi bộ đếm phần mười sang `int`, đọc thương bằng
`intValueExact`. Vì phép chia tỷ trọng bị chạm, toàn bộ 40 case được chạy lại trên jar đóng gói từ
`clean verify` của `9001eda` (81 unit, 50 IT xanh), database mới, ngày 22/11/2026 16:16 (+07):
**60/60 đạt**, gồm M09 (`HEALTHCARE` `5.0`), M10, M11 (`33.4`/`33.3`/`33.3`), M17 và reconciliation trên 9
body; log API không có dòng `ERROR`.

## Không chạy

Case múi giờ JVM của #74 không chạy lại: báo cáo không đọc đồng hồ, chỉ dùng ngày lịch qua cùng service đã
được kiểm ở `Pacific/Pago_Pago` trong lần chạy của #74.
