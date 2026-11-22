---
phase: 1
title: "Danh sách test scenario"
status: completed
priority: P1
effort: "0.5d"
dependencies: []
---

# Phase 1: Danh sách test scenario

## Mục tiêu

Viết ra toàn bộ hành vi phải kiểm chứng **trước** khi có dòng code nào, theo cách hiểu TDD đã chốt với
Product Owner: scenario trước, không unit test. Mỗi scenario là một case trong
`docs/manual-test-guide-cash-flow-reports.md` và một dòng có status/body thật trong evidence report. Số
case ở đây là số case ở hai tài liệu đó.

Mọi số kỳ vọng dưới đây tính tay từ fixture và từ các quy tắc ở [plan.md](./plan.md) trước khi code; khi
chạy, body API được so với bảng này chứ không phải ngược lại.

## Dữ liệu dùng chung

Ba user chèn thẳng vào `wealth.users` (không có endpoint đăng ký), token HS256 mint bằng secret của profile
`local` theo mục 4 của `docs/manual-test-guide-cash-flow-entries.md`:

| User | Id | Tỷ giá | Vai trò |
|---|---|---|---|
| A An Nguyễn | `0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93` | `1 USD = 25.100 ₫`, hiệu lực `12/09/2026` | fixture chính |
| B Bích Trần | `7b41e590-3c82-4f16-b0d7-59e1c4f3a842` | chưa khai | ownership, làm tròn tỷ trọng |
| C Cường Lê | `3d9a7c25-48e1-4b6f-a2c3-6e8f0b1d5a74` | chưa khai, khai ở case M17 | USD thiếu tỷ giá |

Tài sản của A tạo qua `POST /api/v1/assets`: **AS1** `Căn hộ Thảo Điền` (`REAL_ESTATE`), **AS2** `Cổ phiếu
VNM` (`LISTED_SECURITIES`).

### Entry của A (tạo qua `POST /api/v1/cash-flow-entries`)

Rơi vào tháng 09/2026:

| Tên | Loại / danh mục | Số tiền | Lịch | Kỳ trong 09/2026 | Link |
|---|---|---|---|---|---|
| R1 `Lương tháng` | `INCOME` `SALARY` | `95.000.000 ₫` | `MONTHLY` từ 05/01/2026 | 05/09, kỳ 9 | — |
| R2 `Doanh thu tư vấn quý 3` | `INCOME` `BUSINESS_INCOME` | `26.200.000 ₫` | `ONE_TIME` 28/09/2026 | 28/09, kỳ 1 | — |
| R3 `Cho thuê căn hộ Thảo Điền` | `INCOME` `RENTAL_INCOME` | `18.000.000 ₫` | `MONTHLY` từ 10/01/2026 | 10/09, kỳ 9 | AS1 |
| R4 `Cổ tức VNM` | `INCOME` `DIVIDEND` | `4.500.000 ₫` | `QUARTERLY` từ 30/03/2026 | 30/09, kỳ 3 | AS2 |
| R5 `Lãi tiền gửi USD` | `INCOME` `DEPOSIT_INTEREST` | `1.200 USD` | `YEARLY` từ 15/09/2025 | 15/09, kỳ 2 | — |
| R6 `Hoàn thuế TNCN 2025` | `INCOME` `OTHER_INCOME` | `6.180.000 ₫` | `ONE_TIME` 12/09/2026 | 12/09, kỳ 1 | — |
| X1 `Chi phí sinh hoạt gia đình` | `EXPENSE` `LIVING_COSTS` | `23.400.000 ₫` | `MONTHLY` từ 31/01/2026 | 30/09, kỳ 9, **dời cuối tháng** | — |
| X2 `Điện nước, phí quản lý, internet` | `EXPENSE` `HOUSING` | `18.750.000 ₫` | `MONTHLY` từ 05/01/2026 | 05/09, kỳ 9 | — |
| X3 `Học phí` | `EXPENSE` `EDUCATION` | `12.800.000 ₫` | `QUARTERLY` từ 08/03/2026 | 08/09, kỳ 3 | — |
| X4 `Khám sức khỏe định kỳ` | `EXPENSE` `HEALTHCARE` | `4.650.000 ₫` | `ONE_TIME` 18/09/2026 | 18/09, kỳ 1 | — |
| X5 `Phí bảo hiểm sức khỏe gia đình` | `EXPENSE` `INSURANCE` | `5.400.000 ₫` | `YEARLY` từ 15/09/2025 | 15/09, kỳ 2 | — |
| X6 `Thuế TNCN tạm nộp` | `EXPENSE` `TAX_AND_FEES` | `8.250.000 ₫` | `ONE_TIME` 25/09/2026 | 25/09, kỳ 1 | — |
| X7 `Trả gốc vay mua nhà` | `EXPENSE` `DEBT_PRINCIPAL_PAYMENT` | `10.000.000 ₫` | `MONTHLY` từ 28/01/2026 | 28/09, kỳ 9 | — |
| X8 `Lãi vay mua nhà` | `EXPENSE` `INTEREST_PAYMENT` | `6.250.000 ₫` | `MONTHLY` từ 28/01/2026 | 28/09, kỳ 9 | — |
| X9 `Chi khác` | `EXPENSE` `OTHER_EXPENSE` | `2.500.000 ₫` | `ONE_TIME` 22/09/2026 | 22/09, kỳ 1 | — |

Thu `95.000.000 + 26.200.000 + 18.000.000 + 4.500.000 + 30.120.000 + 6.180.000 = 180.000.000 ₫` (R5 là
`1.200 × 25.100 = 30.120.000 ₫`). Chi `23.400.000 + 18.750.000 + 12.800.000 + 4.650.000 + 5.400.000 +
8.250.000 + 10.000.000 + 6.250.000 + 2.500.000 = 92.000.000 ₫`. Net `88.000.000 ₫`, khớp fixture tháng
09/2026 của design #75.

Không rơi vào tháng 09/2026 (nhiễu có chủ đích):

| Tên | Loại / danh mục | Số tiền | Lịch | Vì sao không rơi |
|---|---|---|---|---|
| N1 `Sửa xe` | `EXPENSE` `OTHER_EXPENSE` | `3.000.000 ₫` | `ONE_TIME` 31/08/2026 | ngày trước tháng |
| N2 `Thưởng dự án` | `INCOME` `OTHER_INCOME` | `2.000.000 ₫` | `ONE_TIME` 01/10/2026 | ngày sau tháng |
| N3 `Lãi chứng chỉ quỹ` | `INCOME` `BOND_OR_FUND_INCOME` | `1.500.000 ₫` | `QUARTERLY` từ 20/01/2026 | quý rơi 01, 04, 07, 10 |
| N4 `Thuê kho` | `EXPENSE` `HOUSING` | `2.000.000 ₫` | `MONTHLY` từ 15/01/2026 đến 15/08/2026 | `endDate` trước tháng |
| N5 `Phí thường niên thẻ tín dụng` | `EXPENSE` `TAX_AND_FEES` | `1.200.000 ₫` | `YEARLY` từ 31/03/2026 | năm rơi tháng 03 |
| N6 `Gửi xe tháng` | `EXPENSE` `LIVING_COSTS` | `5.000.000 ₫` | `MONTHLY` từ 05/10/2026 | bắt đầu sau tháng |

### Entry của B và C

B (không tỷ giá, chỉ VND): `Lương` `SALARY` `10.000.000 ₫` một lần 05/09/2026; `Doanh thu bán hàng online`
`BUSINESS_INCOME` `10.000.000 ₫` một lần 15/09/2026; `Tiền lì xì cưới` `OTHER_INCOME` `10.000.000 ₫` một
lần 20/09/2026; `Chi tiêu gia đình` `LIVING_COSTS` `12.000.000 ₫` một lần 10/09/2026.

C (chưa khai tỷ giá): `Lương` `SALARY` `40.000.000 ₫` một lần 05/09/2026; `Cổ tức ETF Mỹ` `DIVIDEND`
`150 USD` một lần 30/09/2026, không link; `Tiền nhà` `HOUSING` `15.000.000 ₫` một lần 01/09/2026; `Phần mềm
làm việc` `LIVING_COSTS` `200 USD` một lần 10/09/2026.

## Scenario

Route `GET /api/v1/cash-flow-reports/monthly?month=YYYY-MM`. Số tiền trên wire có hai chữ số thập phân;
bảng dưới viết theo kiểu Việt Nam cho dễ đọc.

### A. Tháng 09/2026 của A: kỳ nào rơi, kỳ nào không (M01–M07)

| # | Scenario | Kỳ vọng |
|---|---|---|
| M01 | A đọc `month=2026-09` | `200`; `month` `2026-09`, `from` `2026-09-01`, `to` `2026-09-30`, `currency` `VND`, `exchangeRate` `{25100.000000, 2026-09-12}`, `complete` `true`; `totals` thu `180.000.000 ₫`, chi `92.000.000 ₫`, net `88.000.000 ₫`; 15 dòng `occurrences` |
| M02 | Tập `sourceEntryId` của 15 dòng | đúng bằng {R1..R6, X1..X9}, mỗi entry một dòng, ngày và kỳ như bảng fixture |
| M03 | N1..N6 | không entry nào xuất hiện |
| M04 | Định kỳ tháng, quý, năm rơi đúng | R1 kỳ 9, R4 kỳ 3 (quý), R5 kỳ 2 (năm), X3 kỳ 3 (quý), X5 kỳ 2 (năm) |
| M05 | Dời cuối tháng | X1 `2026-09-30`, `shiftedToMonthEnd` `true` |
| M06 | Dòng USD có tỷ giá | R5 `originalAmount` `1200.00`, `originalCurrency` `USD`, `amount` `30120000.00` |
| M07 | Tháng kề hai phía | `2026-08` có N1 (31/08) và N4 (15/08, `endDate` tính vào); `2026-10` có N2, N3, N6; số dòng 08 + 09 + 10 bằng số kỳ của `/cash-flow-occurrences?from=2026-08-01&to=2026-10-31` |

### B. Composition và tỷ trọng (M08–M11)

| # | Scenario | Kỳ vọng |
|---|---|---|
| M08 | `incomeComposition` của A tháng 09 | theo thứ tự: `SALARY` `95.000.000 ₫` `52.8`; `DEPOSIT_INTEREST` `30.120.000 ₫` `16.7`; `BUSINESS_INCOME` `26.200.000 ₫` `14.6`; `RENTAL_INCOME` `18.000.000 ₫` `10.0`; `OTHER_INCOME` `6.180.000 ₫` `3.4`; `DIVIDEND` `4.500.000 ₫` `2.5`; mỗi dòng `occurrenceCount` 1, `partial` `false` |
| M09 | `expenseComposition` của A tháng 09 | `LIVING_COSTS` `25.4`; `HOUSING` `20.4`; `EDUCATION` `13.9`; `DEBT_PRINCIPAL_PAYMENT` `10.9`; `TAX_AND_FEES` `9.0`; `INTEREST_PAYMENT` `6.8`; `INSURANCE` `5.9`; `HEALTHCARE` **`5.0`** (làm tròn riêng sẽ là `5.1` và tổng thành `100.1`); `OTHER_EXPENSE` `2.7` |
| M10 | Tổng `sharePercent` | đúng `100.0` cho cả hai composition |
| M11 | B: ba khoản thu bằng nhau `10.000.000 ₫` | `BUSINESS_INCOME` `33.4`, `OTHER_INCOME` `33.3`, `SALARY` `33.3`; tổng `100.0` (làm tròn riêng sẽ là `99.9`) |

### C. Thu nhập thụ động (M12–M14)

| # | Scenario | Kỳ vọng |
|---|---|---|
| M12 | `passiveIncome` của A tháng 09 | `total` `22.500.000 ₫`, `sharePercentOfIncome` `12.5`; `byAsset` = AS1 `18.000.000 ₫` `10.0` 1 kỳ, AS2 `4.500.000 ₫` `2.5` 1 kỳ; `unassigned` `30.120.000 ₫`, 1 kỳ |
| M13 | Hai tài sản tách riêng, khoản không link | R3 chỉ ở AS1, R4 chỉ ở AS2; R5 (`DEPOSIT_INTEREST` không link) ở `unassigned`; R1, R2, R6 không ở đâu trong `passiveIncome` |
| M14 | Xóa AS2 rồi đọc lại (chạy gần cuối) | `byAsset` chỉ còn AS1; `total` `18.000.000 ₫` `10.0`; `unassigned` `34.620.000 ₫`, 2 kỳ; R4 có `linkedAssetId` `null` |

### D. Reconciliation bằng `jq` (M15)

| # | Scenario | Kỳ vọng |
|---|---|---|
| M15 | Script `recon` chạy trên body thật của M01, M11, M16, M17, M20 | mọi phép so là `true`: thu = Σ dòng `INCOME` có `amount`; chi = Σ dòng `EXPENSE`; net = thu − chi; Σ composition = tổng loại; mỗi dòng composition = Σ và đếm các dòng cùng category; Σ `sharePercent` = 100 khi tổng loại > 0; `passiveIncome.total` = Σ `byAsset` = Σ dòng thu có `linkedAssetId`; mỗi tài sản = Σ dòng của nó; `unassigned` = Σ dòng thu bốn category thụ động không link; `unconverted` = Σ `originalAmount` các dòng `amount` `null`; dòng USD = `originalAmount × usdToVndRate` làm tròn đồng |

### E. Tiền tệ (M16–M18)

| # | Scenario | Kỳ vọng |
|---|---|---|
| M16 | C tháng 09, chưa khai tỷ giá | `200`; `exchangeRate` `null`, `complete` `false`; `totals` `40.000.000 ₫` / `15.000.000 ₫` / `25.000.000 ₫`; `unconverted` `{occurrenceCount 2, income 150.00, expense 200.00}` (USD); hai dòng USD `amount` `null`; `DIVIDEND` và `LIVING_COSTS` có mặt trong composition với `amount` `0.00`, `partial` `true`; `SALARY` và `HOUSING` `100.0`; `unassigned` `{0.00, 1, partial true}` |
| M17 | C khai `25.100` hiệu lực `12/09/2026`, đọc lại | `complete` `true`; `totals` `43.765.000 ₫` / `20.020.000 ₫` / `23.745.000 ₫`; `DIVIDEND` `3.765.000 ₫` `8.6`, `SALARY` `91.4`; `HOUSING` `74.9`, `LIVING_COSTS` `5.020.000 ₫` `25.1`; `unconverted` `{0, 0.00, 0.00}`; `unassigned` `3.765.000 ₫` |
| M18 | B chưa khai tỷ giá nhưng chỉ có VND | `exchangeRate` `null`, `complete` `true` |

### F. Tháng rỗng, tháng tương lai (M19–M20)

| # | Scenario | Kỳ vọng |
|---|---|---|
| M19 | A `month=2025-06` (trước mọi entry) | `200`, không 404; `totals` ba số `0.00`; `incomeComposition`, `expenseComposition`, `byAsset`, `occurrences` đều `[]`; `passiveIncome.total` `0.00`, `sharePercentOfIncome` `null`; `unassigned` `{0.00, 0, false}`; `complete` `true`; `exchangeRate` vẫn có |
| M20 | A `month=2026-12` (sau hôm nay 22/11/2026) | `200`; 9 dòng (R1, R3, R4 30/12, X1 31/12 không dời, X2, X3, X7, X8, N6); thu `117.500.000 ₫`, chi `76.200.000 ₫`, net `41.300.000 ₫` |

### G. Truy nguồn và xác định (M21–M22)

| # | Scenario | Kỳ vọng |
|---|---|---|
| M21 | Mỗi dòng của M01 | `GET /cash-flow-entries/{sourceEntryId}` là `200` với cùng `entryType`, `category`, `amount` = `originalAmount`, `currency` = `originalCurrency`, `linkedAssetId`; bộ (`sourceEntryId`, `occurrenceDate`, `sequence`, `shiftedToMonthEnd`) bằng đúng `/cash-flow-occurrences?from=2026-09-01&to=2026-09-30` |
| M22 | Thứ tự và tính xác định | `occurrences` sắp theo ngày rồi `sourceEntryId`; hai lần gọi cho cùng `sha256` |

### H. Ownership (M23–M24)

| # | Scenario | Kỳ vọng |
|---|---|---|
| M23 | B tháng 09 | 4 dòng, đều là entry của B; thu `30.000.000 ₫`, chi `12.000.000 ₫`, net `18.000.000 ₫` |
| M24 | A tháng 09 | không dòng nào có `sourceEntryId` của B hay C; vẫn 15 dòng |

### I. Input sai (M25–M34)

Mỗi case là `400` `urn:rikkaus:problem:validation-failed` với đúng một lỗi `{field: "month", message}`; body
không lặp lại giá trị đã gửi.

| # | Query | `message` |
|---|---|---|
| M25 | không có `month` | `must be present` |
| M26 | `month=` | `must be present` |
| M27 | `month=2026-9` | `must be a calendar month in the form YYYY-MM` |
| M28 | `month=2026-13` | như trên |
| M29 | `month=2026-00` | như trên |
| M30 | `month=09/2026` | như trên; body không chứa `09/2026` |
| M31 | `month=2026-09-01` | như trên |
| M32 | `month=2026-09&month=2026-10` | như trên |
| M33 | `month=abc` | như trên |
| M34 | `month=%2B2026-09` | như trên |

### J. Xác thực, method, media type, contract (M35–M40)

| # | Scenario | Kỳ vọng |
|---|---|---|
| M35 | không token | `401` `urn:rikkaus:problem:unauthorized` |
| M36 | token hết hạn | giống M35 trừ `correlationId` |
| M37 | token sai chữ ký | giống M35 trừ `correlationId` |
| M38 | `POST`, `PUT`, `DELETE` | `405`, header `Allow: GET` |
| M39 | `Accept: application/xml` | `406` |
| M40 | `openapi.json` và schema | path chỉ có `get`, tham số `month` `required` `true`; bảng `wealth` không đổi, Flyway tối đa `12` |
