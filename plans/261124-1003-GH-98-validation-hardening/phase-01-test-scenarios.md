---
phase: 1
title: "Ma trận kịch bản negative-input"
status: completed
priority: P1
effort: "0.4d"
dependencies: []
---

# Phase 1: Ma trận kịch bản negative-input

## Mục tiêu

Viết toàn bộ kịch bản trước khi sửa dòng code nào (TDD theo nghĩa scenario-first đã chốt với Product
Owner). Ma trận được chạy hai lần bằng [`scenario-runner.sh`](scenario-runner.sh) (bash + `curl` +
`jq`) qua HTTP vào PostgreSQL 18.6 Docker: một lần trên jar của `main` `7d1b1b8` để ghi lại hành vi
trước khi sửa, một lần trên commit cuối. Cột *Mong đợi* là hành vi sau khi sửa; kịch bản có `[FIX]` là
những kịch bản `main` trả sai.

Quy ước: `A`, `B` là hai user đăng nhập qua `POST /api/v1/auth/dev-sign-in` (profile `local`). Mọi request
dùng token của A trừ khi ghi khác. `VF(field, message)` là `400 validation-failed` có entry
`{field, message}` trong `errors`. `MR` là `400 malformed-request` không có `errors`. "Không echo X" nghĩa
là body lỗi không chứa chuỗi X. Mọi body 4xx/5xx còn được kiểm tra tự động là không chứa
`com.rikkaus`, `Exception`, `SQL`, `jdbc`, `hibernate`, `tools.jackson`. `HÔM_NAY`, `NGÀY_MAI`,
`THÁNG_NÀY` tính ở `Asia/Ho_Chi_Minh` lúc chạy.

## Dữ liệu dùng xuyên suốt (tạo qua API, cũng là smoke test)

| Ký hiệu | Owner | Bản ghi |
|---|---|---|
| ASV | A | `Căn hộ Thảo Điền`, `REAL_ESTATE`, 4.200.000.000 ₫, định giá 15/11/2026, mua 15/06/2021 giá 3.100.000.000 ₫ |
| ASU | A | `Tài khoản USD Vietcombank`, `BANK_ACCOUNT`, 21.250,00 USD, định giá 14/09/2026, tỷ giá 25.100 ngày 12/09/2026 |
| LB1 | A | `Vay mua nhà Vietcombank`, `BANK_LOAN`, dư nợ 2.400.000.000 ₫, 15/06/2021–15/06/2041, trả 28.500.000 ₫/tháng từ 05/12/2026, thế chấp ASV |
| CF1 | A | Lương tháng, `INCOME`/`SALARY`, 45.000.000 ₫, `RECURRING`/`MONTHLY` từ 05/01/2026 |
| G1 | A | `Quỹ dự phòng 6 tháng`, `EMERGENCY_FUND`, mục tiêu 270.000.000 ₫ đến 31/12/2027, đã có 85.000.000 ₫, góp 12.000.000 ₫/tháng, lãi 4,5 %/năm |
| ASB, LBB, CFB, GB | B | `Nhà phố Thảo Điền` 9.500.000.000 ₫; `Vay kinh doanh ACB` 1.200.000.000 ₫; tiền thuê 35.000.000 ₫/tháng; `Mua nhà Quận 7` 6.500.000.000 ₫ |

## H — Smoke happy path, mỗi module một lần

| ID | Request | Mong đợi |
|---|---|---|
| H01–H05 | Tạo ASV, ASU, LB1, CF1, G1 như bảng trên | 201 |
| H06 | `PUT /settings/exchange-rate` `25100`, ngày 20/11/2026 | 200 |
| H07 | `POST /assets/ASU/valuations` 21.400,00 USD ngày HÔM_NAY, tỷ giá 25.100 | 201 |
| H08 | `PUT /assets/ASV` đổi ghi chú | 200 |
| H09 | `PUT /liabilities/LB1` dư nợ 2.380.000.000 ₫ | 200 |
| H10 | `PUT /cash-flow-entries/CF1` 46.000.000 ₫ | 200 |
| H11 | `PUT /goals/G1` góp 13.000.000 ₫ | 200 |
| H12 | `PUT /profile` `"  Trinh Nguyễn  "` | 200, `displayName` = `Trinh Nguyễn` |
| H13 | `PATCH /onboarding` `{"currentStep":3,"dashboardReviewed":true}` | 200 |
| H14 | `POST /feedback` `catalog/assets`, `Không thấy nút lưu trên điện thoại` | 201 |
| H15–H20 | `GET /wealth-snapshot`, `/cash-flow-occurrences?from=2026-12-01&to=2026-12-31`, `/cash-flow-reports/monthly?month=2026-11`, `/cash-flow-forecast`, `/goal-projections`, `/assets/ASV/deletion-impact` | 200 |
| H21 | Tạo rồi `DELETE` một khoản chi một lần | 204 |

## X — Giao thức và hình dạng body (trên `POST /liabilities` trừ khi ghi khác)

| ID | Input | Mong đợi |
|---|---|---|
| X01 | Body rỗng | MR |
| X02 | Body `null` | MR |
| X03 | Body `[]` | MR |
| X04 | JSON cụt `{"name":` | MR |
| X05 | `Content-Type: text/plain` | 415 `unsupported-media-type` |
| X06 | `outstandingBalance: "abc"` | MR, không echo `abc` |
| X07 | `outstandingBalance: true` | MR |
| X08 | `currency: "EUR"` | MR, không echo `EUR` |
| X09 `[FIX]` | `currency: 1` | MR (`main`: 201, khoản nợ **USD**) |
| X10 `[FIX]` | `category: 0` | MR (`main`: 201, `BANK_LOAN`) |
| X11 | Thêm `userId`, `id` của B | 201, record của A, id mới |
| X12 | `outstandingBalance: 1e2147483648` | MR |
| X13 | Số 1.201 chữ số | MR |
| X14 | `GET /liabilities/abc` | MR, không echo `abc` |
| X15 `[FIX]` | `PUT /settings/exchange-rate` `{"usdToVndRate":-1,"effectiveDate":null}` với `Accept-Language: de` | VF(`usdToVndRate`, `must be greater than zero`) và VF(`effectiveDate`, `must not be null`), tiếng Anh (`main`: `muss größer 0 sein`) |
| X16 | `name: 12345` (số) | 201, `name` = `"12345"` (coercion vô hướng của Jackson, giữ nguyên) |
| X17 | `outstandingBalance: "2400000000"` (chuỗi) | 201, `2400000000.00` |
| X18 | `DELETE /assets/ASV?confirm=abc` | MR, không xóa gì |

## N — Số

| ID | Endpoint, field, giá trị | Mong đợi |
|---|---|---|
| N01 | `POST /assets` `currentValue: -1` | VF(`currentValue`, `must not be negative`) |
| N02 | `POST /assets` `currentValue: 0` | 201 |
| N03 | `POST /assets` `currentValue` 18 chữ số nguyên | VF(`currentValue`, `must have at most 17 integer digits and 2 decimals`) |
| N04 | `POST /assets` `currentValue: 1250000.005` | VF như N03 |
| N05 | `POST /assets` `currentValue: 1e400` | VF như N03, không 500 |
| N06 | `POST /assets` `quantity: 0` | VF(`quantity`, `must be greater than zero`) |
| N07 | `POST /assets` `TERM_DEPOSIT` `interestRate: 100.0001` | VF(`interestRate`, `must be at most 100`) |
| N08 | `POST /assets/ASV/valuations` `value: -1` | VF(`value`, `must not be negative`) |
| N09 | `POST /liabilities` `outstandingBalance: -0.01` | VF(`outstandingBalance`, `must not be negative`) |
| N10 | `POST /liabilities` `recurringPaymentAmount: 0` | VF(`recurringPaymentAmount`, `must be greater than zero`) |
| N11 | `POST /liabilities` `interestRate: 1000` | VF(`interestRate`, `must have at most 3 integer digits and 4 decimals`) |
| N12 | `POST /cash-flow-entries` `amount: 0` | VF(`amount`, `must be greater than zero`) |
| N13 | `POST /cash-flow-entries` `amount: -45000000` | VF(`amount`, `must be greater than zero`) |
| N14 | `POST /goals` `targetAmount: 0` | VF(`targetAmount`, `must be greater than zero`) |
| N15 | `POST /goals` `expectedAnnualReturnPercent: 30.01` | VF(`expectedAnnualReturnPercent`, `must be between 0 and 30`) |
| N16 | `POST /goals` đã có 300.000.000 ₫ > mục tiêu 270.000.000 ₫ | VF(`currentAllocatedAmount`, `must not exceed the target amount`) |
| N17 `[FIX]` | `PUT /settings/exchange-rate` `usdToVndRate: 0` | VF(`usdToVndRate`, `must be greater than zero`) (`main`: `must be greater than 0`) |
| N18 `[FIX]` | `PUT /settings/exchange-rate` `usdToVndRate: 25100.1234567` | VF(`usdToVndRate`, `must have at most 13 integer digits and 6 decimals`) (`main`: `numeric value out of bounds (<13 digits>.<6 digits> expected)`) |
| N19 | `PATCH /onboarding` `currentStep: 8` | VF(`currentStep`, `must be between 1 and 7`) |
| N20 | `PATCH /onboarding` `currentStep: 2.5` | MR |

## D — Ngày

| ID | Endpoint, field, giá trị | Mong đợi |
|---|---|---|
| D01 | `POST /assets` `valuationDate: "2026-02-30"` | MR |
| D02 | `POST /assets` `valuationDate` NGÀY_MAI | VF(`valuationDate`, `must not be in the future`) |
| D03 | `POST /assets` mua 20/11/2026 sau định giá 15/11/2026 | VF(`purchaseDate`, `must not be later than the valuation date`) |
| D04 | `POST /assets/ASV/valuations` `effectiveDate` NGÀY_MAI | VF(`effectiveDate`, `must not be in the future`) |
| D05 | `POST /assets/ASV/valuations` `effectiveDate: "2020-01-01"` trước ngày mua | VF(`effectiveDate`, `must not be earlier than the purchase date`) |
| D06 | `POST /liabilities` đáo hạn 01/01/2020 trước bắt đầu 15/06/2021 | VF(`maturityDate`, `must not be before the start date`) |
| D07 | `POST /liabilities` kỳ trả 01/01/2042 sau đáo hạn 15/06/2041 | VF(`nextPaymentDate`, `must not be after the maturity date`) |
| D08 `[FIX]` | `POST /liabilities` `startDate: "-5000-01-01"` | MR (`main`: **500**) |
| D09 `[FIX]` | `POST /liabilities` `startDate: "+10000-01-01"` | MR (`main`: 201, lưu năm 10000) |
| D10 `[FIX]` | `POST /cash-flow-entries` `effectiveDate: "0000-01-31"` | MR (`main`: 201, đọc lại `0001-01-31`) |
| D11 `[FIX]` | `POST /cash-flow-entries` `effectiveDate: "2026-09-12T10:00:00"` | MR (`main`: 201, cắt giờ) |
| D12 `[FIX]` | `POST /cash-flow-entries` `effectiveDate: 20000` | MR (`main`: 201, `2024-10-04`) |
| D13 `[FIX]` | `POST /cash-flow-entries` `effectiveDate: [2026,9,12]` | MR (`main`: 201) |
| D14 | `POST /cash-flow-entries` kết thúc 01/01/2026 trước bắt đầu 05/01/2026 | VF(`endDate`, `must not be before the effective date`) |
| D15 | `POST /cash-flow-entries` một lần ngày 15/01/2027 (tương lai) | 201 |
| D16 | `POST /goals` `targetDate` HÔM_NAY | VF(`targetDate`, `must be after today`) |
| D17 `[FIX]` | `POST /goals` `targetDate: "+999999999-12-31"` | MR (`main`: **500**) |
| D18 | `PUT /settings/exchange-rate` `effectiveDate` NGÀY_MAI | VF(`effectiveDate`, `must not be in the future`) |
| D19 `[FIX]` | `PUT /settings/exchange-rate` `effectiveDate: "2026-09-12T20:00:00Z"` | MR (`main`: 200, lưu ngày UTC `2026-09-12`) |
| D20 | `POST /assets` USD `rateEffectiveDate` NGÀY_MAI | VF(`rateEffectiveDate`, `must not be in the future`) |
| D21 | `GET /cash-flow-occurrences?from=2026-02-29&to=2026-03-31` | VF(`from`, `must be a calendar date in the form YYYY-MM-DD`) |
| D22 | `GET /cash-flow-occurrences?from=2026-12-01&to=2026-11-01` | VF(`to`, `must not be before from`) |
| D23 | `GET /cash-flow-reports/monthly?month=2026-13` | VF(`month`, `must be a calendar month in the form YYYY-MM`) |
| D24 `[FIX]` | `GET /cash-flow-forecast?from=2026-9` | VF(`from`, `must be a calendar month in the form YYYY-MM`) (`main`: `must be a month in the form YYYY-MM`) |
| D25 | `GET /cash-flow-forecast?from=` THÁNG_NÀY | VF(`from`, `must not be before the month after today`) |
| D26 | `GET /cash-flow-entries?effectiveFrom=2026-02-30` | VF(`effectiveFrom`, `must be a calendar date in the form YYYY-MM-DD`) |
| D27 | `POST /liabilities` `startDate: ""` | 201, `startDate` = `null` |
| D28 | `POST /cash-flow-entries` `effectiveDate: ""` | VF(`effectiveDate`, `must not be null`) |

## C — Tiền tệ

| ID | Input | Mong đợi |
|---|---|---|
| C01 | `POST /assets` `currency: "EUR"` | MR, không echo `EUR` |
| C02 | `POST /assets/ASU/valuations` `currency: "VND"` trên tài sản USD | VF(`currency`, `must match the asset's currency`) |
| C03 | `POST /assets` VND kèm `usdToVndRate` | VF(`usdToVndRate`, `must be absent for a VND valuation`) |
| C04 | `POST /assets` USD có tỷ giá, thiếu ngày tỷ giá | VF(`rateEffectiveDate`, `must be given together with usdToVndRate`) |
| C05 | `POST /cash-flow-entries` `currency: "usd"` | MR |
| C06 | `GET /liabilities?currency=EUR` | VF(`currency`, `must be one of VND, USD`) |

## T — Chuỗi

| ID | Input | Mong đợi |
|---|---|---|
| T01 | `POST /assets` `name: " "` | VF(`name`, `must not be blank`) |
| T02 | `POST /assets` `name: "Tiền\u0000mặt"` | VF(`name`, `must not contain a null character`) |
| T03 | `POST /assets` tên 121 ký tự | VF(`name`, `must be at most 120 characters`) |
| T04 | `POST /liabilities` `notes: " 　"` | 201, `notes` = `null` |
| T05 | `POST /cash-flow-entries` `notes` chứa `U+0000` | VF(`notes`, `must not contain a null character`) |
| T06 | `POST /goals` `name: " "` | VF(`name`, `must not be blank`) |
| T07 | `POST /feedback` `message: " 　"` | VF(`message`, `must not be blank`) |
| T08 | `POST /feedback` `screen: "Dashboard"` | VF(`screen`, `must be a lowercase screen code such as dashboard or catalog/assets`) |
| T09 `[FIX]` | `PUT /profile` `displayName: " "` | VF(`displayName`, `must not be blank`) (`main`: 200, tên là một dấu cách không ngắt) |
| T10 `[FIX]` | `PUT /profile` `displayName: "An\u0000Nguyễn"` | VF(`displayName`, `must not contain a null character`) (`main`: **500**) |
| T11 `[FIX]` | `PUT /profile` 121 ký tự | VF(`displayName`, `must be at most 120 characters`) (`main`: `size must be between 0 and 120`) |
| T12 `[FIX]` | `POST /assets` `name: "​"` | VF(`name`, `must not be blank`) (`main`: 201, tên vô hình) |
| T13 `[FIX]` | `POST /assets/ASV/valuations` `note: "﻿"` | 201, `note` = `null` (`main`: lưu `U+FEFF`) |
| T14 `[FIX]` | `POST /auth/dev-sign-in` `displayName` chứa `U+0000` | VF(`displayName`, `must not contain a null character`) (`main`: **500**) |
| T15 | `GET /assets?q=` 121 ký tự | VF(`q`, `must be at most 120 characters`) |

## R — Quan hệ và quyền sở hữu

| ID | Input | Mong đợi |
|---|---|---|
| R01 | `GET /assets/ASB` so với `GET /assets/<uuid ngẫu nhiên>` | 404 cả hai, giống từng byte sau khi bỏ `instance`, `correlationId` |
| R02 | `PUT /liabilities/LBB` so với uuid ngẫu nhiên | như R01 |
| R03 | `DELETE /goals/GB` so với uuid ngẫu nhiên | như R01 |
| R04 | `PUT /cash-flow-entries/CFB` so với uuid ngẫu nhiên | như R01 |
| R05 | `GET /assets/ASB/valuations` so với uuid ngẫu nhiên | như R01 |
| R06 | `POST /liabilities` `collateralAssetId` = ASB so với uuid ngẫu nhiên | VF(`collateralAssetId`, `must identify an asset the signed-in user owns`), hai body giống nhau trừ `correlationId` |
| R07 | `POST /cash-flow-entries` `linkedAssetId` = ASB | VF(`linkedAssetId`, `must reference one of the signed-in user's assets`) |
| R08 | `POST /cash-flow-entries` có cả `linkedAssetId` và `linkedLiabilityId` | VF(`linkedLiabilityId`, `must not be combined with linkedAssetId`) |
| R09 | `POST /assets/ASV/valuations` `correctsValuationId` = valuation của ASU | VF(`correctsValuationId`, `must identify a valuation of this asset`) |

## M — Nhiều lỗi trong một response

| ID | Input | Mong đợi |
|---|---|---|
| M01 | `POST /cash-flow-entries` `EXPENSE`/`SALARY`, `ONE_TIME` kèm `recurrence` và `endDate` | 3 entry: `category`, `recurrence`, `endDate` |

## P — Quyền riêng tư (sau toàn bộ lần chạy)

| ID | Kiểm tra | Mong đợi |
|---|---|---|
| P01 | Không response nào là 500 | 0 |
| P02 | Log API của lần chạy: dòng `ERROR` | 0 |
| P03 | Log API chứa giá trị người dùng đã gửi (`Tiền`, `Căn hộ Thảo Điền`, `5001-01-01 BC`) | 0 dòng |
