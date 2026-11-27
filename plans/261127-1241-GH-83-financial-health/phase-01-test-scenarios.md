---
phase: 1
title: "Ma trận test scenario"
status: completed
priority: P1
effort: "0.25d"
dependencies: []
---

# Phase 1: Ma trận test scenario

## Mục tiêu

Viết toàn bộ hành vi phải kiểm chứng **trước** khi có dòng code nào (TDD theo nghĩa đã chốt với PO:
scenario trước, không unit test). Mọi số kỳ vọng tính bằng Python `decimal` (HALF_UP) từ dữ liệu dưới đây
và quy tắc của [plan.md](./plan.md); khi chạy, body API được so với bảng này chứ không phải ngược lại.

## Ngày

`D = 27/11/2026` (`Asia/Ho_Chi_Minh`). Tháng mặc định là `2026-11`; forecast luôn là ba tháng kể từ tháng
sau hôm nay: `2026-12-01 … 2027-02-28`; cửa sổ nghĩa vụ là `D … D+30 = 2026-11-27 … 2026-12-27`.
Kịch bản Cơ sở 09/2026 của design #81 được dời sang `11/2026` để forecast có dữ liệu; mọi số tiền giữ
nguyên.

## Quy tắc tính (tóm tắt, chi tiết ở phase 2)

| # | `indicatorId` | Công thức | Đơn vị, làm tròn | Dải |
|---|---|---|---|---|
| 1 | `EMERGENCY_RESERVE_MONTHS` | tài sản `CASH`+`BANK_ACCOUNT`+`TERM_DEPOSIT` (VND) ÷ Σ chi định kỳ thiết yếu còn hiệu lực trong tháng quy về tháng (MONTHLY ×1, QUARTERLY ÷3, YEARLY ÷12) | `MONTHS`, 1 chữ số | OK ≥ 6, WATCH 3–<6, ATTENTION < 3 |
| 2 | `DEBT_PAYMENT_BURDEN` | chi `DEBT_PRINCIPAL_PAYMENT`+`INTEREST_PAYMENT` của tháng ÷ `totals.income` | `FRACTION`, 6 | OK ≤ 0,25, WATCH ≤ 0,35, ATTENTION > 0,35 |
| 3 | `FORECAST_MINIMUM_CASH_BALANCE` | `minimumBalance.amount` của `GET /cash-flow-forecast` | `VND` | OK ≥ mẫu số chỉ số 1, WATCH 0–<, ATTENTION < 0 |
| 4 | `PASSIVE_INCOME_SHARE` | `passiveIncome.total` ÷ `totals.income` | `FRACTION`, 6 | HIGH ≥ 0,5, MEDIUM ≥ 0,2, LOW < 0,2 |
| 5 | `ESSENTIAL_EXPENSE_SHARE` | chi thuộc nhóm thiết yếu ÷ `totals.expense` | `FRACTION`, 6 | OK ≤ 0,7, WATCH ≤ 0,8, ATTENTION > 0,8 |

| # | `ruleId` | `ATTENTION` khi | Phụ thuộc |
|---|---|---|---|
| 1 | `SPENDING_EXCEEDS_INCOME` | `totals.income − totals.expense < 0` | report |
| 2 | `FORECAST_CASH_NEGATIVE` | chỉ số 3 `< 0` | chỉ số 3 |
| 3 | `RESERVE_BELOW_REFERENCE` | chỉ số 1 `< min-reserve-months` (3) | chỉ số 1 |
| 4 | `HIGH_DEBT_PAYMENT_BURDEN` | chỉ số 2 `> max-debt-payment-burden` (0,35) | chỉ số 2 |
| 5 | `LIQUIDITY_SHORT_FOR_UPCOMING_OBLIGATIONS` | `CASH`+`BANK_ACCOUNT` `<` nghĩa vụ đến hạn trong `due-window-days` (30) | tài sản, công nợ |
| 6 | `INCOME_CONCENTRATION` | danh mục thu lớn nhất ÷ `totals.income` `> max-income-source-share` (0,6) | report |

Nghĩa vụ đến hạn: liability đáo hạn trong cửa sổ đóng góp `outstandingBalance` (đã bao gồm mọi kỳ trả còn
lại, nên kỳ trả của nó không cộng thêm); liability khác đóng góp từng kỳ trả theo lịch trong cửa sổ.

## Dữ liệu

Tỷ giá `25.100` ngày `12/09/2026` cho mọi user trừ `P`, `E`, `Z`, `I`.

### B — Cơ sở (`fh.base@example.com`), fixture design #81

Tài sản: `Tiền mặt dự phòng` CASH 148.000.000 ₫ · `Tài khoản USD HSBC` BANK_ACCOUNT 20.000 USD (= 502.000.000 ₫)
· `Tiền gửi kỳ hạn 12 tháng` TERM_DEPOSIT 1.200.000.000 ₫ · `Danh mục cổ phiếu HOSE` LISTED_SECURITIES
2.150.000.000 ₫ · `Căn hộ cho thuê Quận 2` REAL_ESTATE 4.300.000.000 ₫ · `Quỹ trái phiếu An Tâm`
INVESTMENT_FUND 300.000.000 ₫.

Công nợ: `Khoản vay mua căn hộ` BANK_LOAN 1.500.000.000 ₫, 18.750.000 ₫ MONTHLY, kỳ tới `2026-12-20`, đáo hạn
`2041-09-20` · `Thẻ tín dụng Techcombank` CREDIT_CARD_BALANCE 32.000.000 ₫, không lịch trả, đáo hạn `2026-11-30` (trong cửa sổ nghĩa vụ nhưng trước
forecast; một khoản nợ không lịch trả đáo hạn trong forecast sẽ làm forecast `partial`).

Thu (7, tổng tháng 11 = 180.000.000 ₫): Lương 132.000.000 MONTHLY từ `2026-01-05` · Thu nhập kinh doanh
26.200.000 một lần `2026-11-12` · Thu nhập cho thuê 14.800.000 MONTHLY từ `2026-01-10` (liên kết căn hộ cho
thuê) · Cổ tức 150 USD QUARTERLY từ `2026-11-15` (liên kết cổ phiếu, = 3.765.000 ₫) · Lãi tiền gửi
1.500.000 MONTHLY từ `2026-01-25` (liên kết tiền gửi) · Thu nhập trái phiếu hoặc quỹ 1.500.000 QUARTERLY từ
`2026-11-20` (liên kết quỹ) · Thu nhập khác 235.000 một lần `2026-11-18`.

Chi (9, tổng 92.000.000 ₫, thiết yếu 76.700.000 ₫): Chi phí sinh hoạt 23.400.000 MONTHLY từ `2026-01-03` ·
Nhà ở 18.750.000 MONTHLY từ `2026-01-20` (liên kết khoản vay) · Giáo dục 12.800.000 một lần `2026-11-08` ·
Y tế 4.650.000 một lần `2026-11-14` · Bảo hiểm 5.400.000 YEARLY từ `2025-11-16` · Thuế và phí 8.250.000
QUARTERLY từ `2026-11-22` · Thanh toán gốc 10.000.000 MONTHLY từ `2026-01-20` (liên kết khoản vay) · Thanh
toán lãi 6.250.000 MONTHLY từ `2026-01-20` (liên kết khoản vay) · Chi phí khác 2.500.000 một lần
`2026-11-24`.

Chi thiết yếu định kỳ quy tháng = 23.400.000 + 18.750.000 + 5.400.000/12 + 8.250.000/3 + 10.000.000 +
6.250.000 = **61.600.000 ₫**.

### W — Theo dõi (`fh.watch@example.com`)

Tài sản: Tiền mặt CASH 40.000.000 · Tiền gửi kỳ hạn 6 tháng TERM_DEPOSIT 160.000.000 · Căn hộ dịch vụ Bình
Thạnh REAL_ESTATE 2.800.000.000. Công nợ: Vay mua nhà BIDV BANK_LOAN 900.000.000, 16.250.000 MONTHLY, kỳ tới
`2026-12-20`. Thu: Lương 45.000.000 MONTHLY từ `2026-01-01` · Thu nhập cho thuê 12.000.000 MONTHLY từ
`2026-01-01` (liên kết căn hộ). Chi: Chi phí sinh hoạt 26.150.000 MONTHLY từ `2026-01-03` · Thanh toán gốc
10.000.000 và Thanh toán lãi 6.250.000 MONTHLY từ `2026-01-20` (liên kết khoản vay) · Giáo dục 14.000.000 một
lần `2026-11-08`. Thiết yếu định kỳ = 42.400.000 ₫; chi tháng = 56.400.000 ₫; thu = 57.000.000 ₫.

### S — Áp lực (`fh.stress@example.com`)

Như B nhưng: tài sản chỉ có Tiền mặt CASH 12.000.000 · Tài khoản thanh toán BANK_ACCOUNT 30.000.000 · Căn hộ
cho thuê Quận 2; không tiền gửi, không USD. Lương 132.000.000 MONTHLY từ `2026-01-05` **kết thúc
`2026-10-31`** (đổi việc). Thu chỉ còn Thu nhập cho thuê 14.800.000. Chi: Chi phí sinh hoạt, Nhà ở, Thanh toán
gốc, Thanh toán lãi (như B), Bảo hiểm YEARLY từ `2025-11-16`, Thuế và phí QUARTERLY từ `2026-11-22`; không
khoản một lần nào. Công nợ như B. Chi tháng = 72.050.000 ₫.

### P — Một phần (`fh.partial@example.com`)

Đúng dữ liệu B nhưng **không khai báo tỷ giá**: tài khoản 20.000 USD và cổ tức 150 USD không quy đổi được.

### Z — Mẫu số 0 (`fh.zero@example.com`)

Tài sản: Tiền mặt CASH 50.000.000. Không công nợ, không thu. Chi một lần: Chi phí sinh hoạt 23.400.000
`2026-11-10` · Giáo dục 12.800.000 `2026-11-15` · Chi phí khác 2.500.000 `2026-11-20`.

### I — Chỉ có thu (`fh.income@example.com`)

Không tài sản, không công nợ. Lương 30.000.000 một lần `2026-11-05`.

### E — Chưa có dữ liệu (`fh.empty@example.com`)

Đăng nhập lần đầu, không bản ghi nào.

## Kỳ vọng — chỉ số

`C` = COMPLETE, `P` = PARTIAL, `U` = UNAVAILABLE.

| User | 1 Dự phòng | 2 Trả nợ | 3 Số dư thấp nhất | 4 Thụ động | 5 Thiết yếu |
|---|---|---|---|---|---|
| B | C `30.0` OK (1.850.000.000 / 61.600.000) | C `0.090278` OK (16.250.000 / 180.000.000) | C `626600000.00` ngày `2026-12-03`, OK (tham chiếu 61.600.000) | C `0.119806` LOW (21.565.000 / 180.000.000) | C `0.833696` ATTENTION (76.700.000 / 92.000.000) |
| W | C `4.7` WATCH (200.000.000 / 42.400.000) | C `0.285088` WATCH | C `40000000.00` ngày `2026-12-01`, WATCH | C `0.210526` MEDIUM | C `0.751773` WATCH (42.400.000 / 56.400.000) |
| S | C `0.7` ATTENTION (42.000.000 / 61.600.000) | C `1.097973` ATTENTION (16.250.000 / 14.800.000) | C `-97050000.00` ngày `2027-02-22`, ATTENTION | C `1.000000` HIGH | C `1.000000` ATTENTION |
| P | P `21.9`, band `null` (`PARTIAL_INPUTS`), `MISSING_EXCHANGE_RATE` (1.348.000.000 / 61.600.000) | P `0.092206` (16.250.000 / 176.235.000) | P `124600000.00` ngày `2026-12-03` | P `0.101002` (17.800.000 / 176.235.000) | C `0.833696` ATTENTION |
| Z | U `NO_RECURRING_ESSENTIAL_EXPENSES` | U `NO_INCOME` | C `50000000.00` ngày `2026-12-01`, band `null` (`NO_RECURRING_ESSENTIAL_EXPENSES`) | U `NO_INCOME` | C `0.604651` OK (23.400.000 / 38.700.000) |
| I | U `NO_LIQUID_ASSETS` | C `0.000000` OK (số 0 thật: có thu, không trả nợ) | U `NO_CASH_ACCOUNTS` | C `0.000000` LOW | U `NO_EXPENSES` |
| E | U `NO_LIQUID_ASSETS` | U `NO_INCOME` | U `NO_CASH_ACCOUNTS` | U `NO_INCOME` | U `NO_EXPENSES` |

Mỗi dải xuất hiện ít nhất một lần cho mỗi chỉ số: 1 OK/WATCH/ATTENTION = B/W/S; 2 = B/W/S; 3 = B/W/S;
4 LOW/MEDIUM/HIGH = B/W/S; 5 OK/WATCH/ATTENTION = Z/W/B.

## Kỳ vọng — rule

`A` = ATTENTION, `O` = OK, `N(x)` = NOT_EVALUATED với lý do `x`.

| User | 1 Chi vượt thu | 2 Forecast âm | 3 Dự phòng | 4 Trả nợ | 5 Thanh khoản | 6 Tập trung thu |
|---|---|---|---|---|---|---|
| B | O (`88000000.00`) | O | O (`30.0` vs `3.0`) | O | O (650.000.000 vs 50.750.000) | A `0.733333` `SALARY` |
| W | O (`600000.00`) | O | O (`4.7`) | O | O (40.000.000 vs 16.250.000) | A `0.789474` `SALARY` |
| S | A (`-57250000.00`) | A (`-97050000.00`) | A (`0.7`) | A (`1.097973`) | A (42.000.000 vs 50.750.000) | A `1.000000` `RENTAL_INCOME` |
| P | N(`MISSING_EXCHANGE_RATE`) | N(`INDICATOR_PARTIAL`) | N(`INDICATOR_PARTIAL`) | N(`INDICATOR_PARTIAL`) | N(`MISSING_EXCHANGE_RATE`) | N(`MISSING_EXCHANGE_RATE`) |
| Z | A (`-38700000.00`) | O (`50000000.00`) | N(`INDICATOR_UNAVAILABLE`) | N(`INDICATOR_UNAVAILABLE`) | N(`NO_LIABILITIES`) | N(`NO_INCOME`) |
| I | O (`30000000.00`) | N(`INDICATOR_UNAVAILABLE`) | N(`INDICATOR_UNAVAILABLE`) | O (`0.000000`) | N(`NO_CASH_ACCOUNTS`) | A `1.000000` `SALARY` |
| E | N(`NO_CASH_FLOW_IN_MONTH`) | N(`INDICATOR_UNAVAILABLE`) | N(`INDICATOR_UNAVAILABLE`) | N(`INDICATOR_UNAVAILABLE`) | N(`NO_CASH_ACCOUNTS`) | N(`NO_INCOME`) |

Cơ sở kích hoạt đúng một rule (tập trung thu 73,3%), như design #81; Áp lực kích hoạt cả sáu.

## Kịch bản

| # | Kịch bản | Kỳ vọng |
|---|---|---|
| H01 | B, `GET /financial-health` (không `month`) | 200; `assumptions` `{asOfDate 2026-11-27, timeZone Asia/Ho_Chi_Minh, month 2026-11, forecastHorizon 2026-12-01…2027-02-28, obligationWindow 2026-11-27…2026-12-27, liquidAssetCategories [CASH, BANK_ACCOUNT, TERM_DEPOSIT], essentialCategories (7), exchangeRate 25100.000000 / 2026-09-12}`; `thresholds` 10 giá trị mặc định; 5 chỉ số và 6 rule đúng thứ tự |
| H02 | B, năm chỉ số | bảng trên, gồm tử số, mẫu số, formula, `sources` đúng vai trò |
| H03 | B, sáu rule | bảng trên, gồm `threshold.property`, `dependsOn`, evidence |
| H04 | B, đối soát `jq` | tử/mẫu số 2, 4, 5 = report tháng 11; chỉ số 3 = `minimumBalance` của forecast; nguồn tiền 650.000.000 = `openingBalance.amount`; tổng `sources` = tử/mẫu số |
| H05 | B, `month=2026-11` và đọc lại | byte-identical với H01 |
| H06 | B, `month=2026-10` | thu 148.300.000, chi 58.400.000; thiết yếu định kỳ 58.850.000 (Thuế và phí chưa có hiệu lực) → dự phòng `31.4`; trả nợ `0.109575`; thụ động `0.109912`; thiết yếu `1.000000`; tập trung `0.890088`; forecast giữ nguyên H01 |
| H07 | B, `?userId=<id của S>` | byte-identical với H01 |
| H08 | W | bảng trên; đối soát `jq` |
| H09 | S | bảng trên; cả sáu rule `ATTENTION`; đối soát `jq` |
| H10 | P | bảng trên; `assumptions.exchangeRate null`; không rule nào `OK` hay `ATTENTION`; nguồn USD có `amountVnd null` |
| H11 | Z | bảng trên; không `value` nào là `0` cho chỉ số `UNAVAILABLE` |
| H12 | I | bảng trên; `0.000000` thật ở 2 và 4 |
| H13 | E | năm `UNAVAILABLE` `value null`, sáu `NOT_EVALUATED`, `numeratorVnd`/`denominatorVnd` đều `null` |
| H14 | `month=2026-13`, `month=11/2026`, `month=2026-9`, `month=` | 400 `validation-failed`, `errors [{field month, message "must be a calendar month in the form YYYY-MM"}]`, không echo giá trị |
| H15 | không token; token sai | 401 `unauthorized` |
| H16 | `POST /financial-health` | 405 |
| H17 | Khởi động lại với `RIKKAUS_CASHFLOW_HEALTH_MINRESERVEMONTHS=5`, `…_MAXINCOMESOURCESHARE=0.8`, `…_ESSENTIALCATEGORIES=LIVING_COSTS,HOUSING,EDUCATION,HEALTHCARE,INSURANCE,TAX_AND_FEES,DEBT_PRINCIPAL_PAYMENT,INTEREST_PAYMENT` | `thresholds.minReserveMonths 5.0`, `maxIncomeSourceShare 0.800000`; W rule 3 `ATTENTION` (4.7 < 5), dải dự phòng W `ATTENTION`; B và W rule 6 `OK`; B thiết yếu `0.972826` (89.500.000 / 92.000.000), `assumptions.essentialCategories` có `EDUCATION` |
| H18 | Khởi động lại không biến nào | B trở lại H01 byte-identical |
| H19 | Khởi động với cấu hình vô lý: `ESSENTIALCATEGORIES=SALARY`; `MINRESERVEMONTHS=7` (> ok 6); `PASSIVEINCOMEMEDIUMMIN=0.6` (> high 0.5) | ứng dụng tự thoát, log nêu tên property |
| H20 | `OpenApiContractIT`, `ApplicationContextIT`, `ArchitectureRulesTest` | xanh trong `./mvnw clean verify` |
