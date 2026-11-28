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
scenario trước, không unit test). Advisor không tính số mới, nên kỳ vọng gồm hai phần:

1. **Cấu trúc và phân nhóm**: finding nào vào nhóm nào, tier, họ hành động, gộp, thứ tự, dấu ưu tiên. Viết
   tay ở bảng dưới, tính bằng Python `decimal` (HALF_UP, 6 chữ số) cho mức lệch.
2. **Đối soát**: mỗi `observed`, `threshold`, `status` của từng finding phải bằng đúng route upstream đọc
   cho cùng user và cùng tháng ngay trong lần chạy (`scripts/recon.jq`). Số upstream đã được kiểm ở #68,
   #83, #89; ở đây chỉ kiểm advisor chép đúng.

## Ngày

`D = 28/11/2026` (`Asia/Ho_Chi_Minh`). Tháng mặc định `2026-11`; cửa sổ nghĩa vụ `2026-11-28 … 2026-12-28`;
forecast `2026-12-01 … 2027-02-28`. Tỷ giá `25.100` ngày `12/09/2026` (77 ngày tuổi, ngưỡng 5) cho B và S.

## Quy tắc advisor (hợp đồng cố định của brief)

| `ruleId` | Nguồn | tier | `actionFamily` | `actionId` |
|---|---|---|---|---|
| `UPCOMING_LIABILITY_DUE` | snapshot | 1 | `RECONCILE_CASH_WITH_OBLIGATIONS` | `COMPARE_CASH_WITH_UPCOMING_OBLIGATIONS` |
| `LIQUIDITY_SHORT_FOR_UPCOMING_OBLIGATIONS` | health | 1 | `RECONCILE_CASH_WITH_OBLIGATIONS` | như trên |
| `FORECAST_CASH_NEGATIVE` | health | 1 | `RECONCILE_CASH_WITH_OBLIGATIONS` | như trên |
| `SPENDING_EXCEEDS_INCOME` | health | 2 | `REVIEW_PERIOD_SPENDING` | `REVIEW_LARGEST_PERIOD_EXPENSES` |
| `HIGH_DEBT_PAYMENT_BURDEN` | health | 2 | `REVIEW_DEBT_PAYMENTS` | `CHECK_DEBT_PAYMENTS_AGAINST_INCOME` |
| `RESERVE_BELOW_REFERENCE` | health | 3 | `RECONCILE_CASH_WITH_RESERVE` | `COMPARE_LIQUID_ASSETS_WITH_RESERVE` |
| `LOW_LIQUIDITY_SHARE` | snapshot | 3 | `RECONCILE_CASH_WITH_RESERVE` | như trên |
| `INCOME_CONCENTRATION` | health | 3 | `REVIEW_INCOME_SOURCES` | `REVIEW_INCOME_SOURCE_MIX` |
| `CATEGORY_CONCENTRATION` | snapshot | 3 | `REVIEW_ASSET_CONCENTRATION` | `REVIEW_ASSET_CATEGORY_SHARE` |
| `DEBT_TO_ASSET_RATIO` | snapshot | 3 | `REVIEW_DEBT_LEVEL` | `REVIEW_LIABILITIES_AGAINST_ASSETS` |
| `GOAL_CONTRIBUTION_COVERAGE` | goal | 4 | `REVIEW_GOAL_PLAN` | `CONSIDER_GOAL_PLAN_OPTIONS` |
| `STALE_VALUATION`, `STALE_EXCHANGE_RATE` | snapshot | 5 | `REFRESH_RECORD` | `UPDATE_STALE_RECORD` |

Phân nhóm: snapshot `UNAVAILABLE`, health `NOT_EVALUATED`, goal `status: null` → `unevaluated` (status
`NOT_EVALUATED`); snapshot/health `OK`, goal `ON_TRACK` → `positive`; snapshot/health `ATTENTION`, goal
`AT_RISK`/`BEHIND` → `attention`. Mức lệch `|observed − threshold| / threshold` (6 chữ số HALF_UP), goal
`(100 − coverage) / 100`; `null` khi ngưỡng bằng 0 hoặc thiếu giá trị. Xếp hạng: tier tăng; `dueDate` gần
trước, `null` sau; mức lệch giảm, `null` sau; `findingId` tăng. Ba mục đầu `priority: true`.

## Dữ liệu (nạp bằng `scripts/setup.sh` qua API thật)

### B — Cơ sở (`adv.base@example.com`), fixture design #90 dời sang 11/2026

Tài sản (tổng **12.002.000.000 ₫**): `Tiền mặt dự phòng` CASH 148.000.000 ₫ (25/11) · `Tài khoản USD`
BANK_ACCOUNT 20.000 USD = 502.000.000 ₫ (24/11) · `Tiền gửi kỳ hạn 12 tháng` TERM_DEPOSIT 1.200.000.000 ₫ (21/11)
· `Danh mục cổ phiếu niêm yết` LISTED_SECURITIES 2.352.000.000 ₫ (26/11) · `Căn hộ Thảo Điền` REAL_ESTATE
7.500.000.000 ₫, LOW, định giá **11/10/2026 (48 ngày)** · `Chứng chỉ quỹ trái phiếu` INVESTMENT_FUND
300.000.000 ₫ (20/11). Nhóm thanh khoản 1.850.000.000 ₫.

Công nợ (tổng **1.532.000.000 ₫**): `Khoản vay mua căn hộ` BANK_LOAN 1.500.000.000 ₫, 18.750.000 ₫ MONTHLY,
kỳ tới 20/12/2026 · `Thẻ tín dụng` CREDIT_CARD_BALANCE 32.000.000 ₫, đáo hạn 30/11/2026.

Dòng tiền 11/2026 (thu **180.000.000 ₫**, chi **92.000.000 ₫**, như #83): `Lương Công ty ABC` SALARY
132.000.000 ₫ MONTHLY; kinh doanh 26.200.000 ₫; cho thuê 14.800.000 ₫ liên kết `Căn hộ Thảo Điền`; cổ tức
150 USD QUARTERLY; lãi tiền gửi 1.500.000 ₫; quỹ trái phiếu 1.500.000 ₫ QUARTERLY; khác 235.000 ₫; chi sinh
hoạt 23.400.000, nhà ở 18.750.000, học phí 12.800.000, khám 4.650.000, bảo hiểm 5.400.000 YEARLY, thuế
8.250.000 QUARTERLY, gốc 10.000.000, lãi 6.250.000, khác 2.500.000.

Sáu mục tiêu (lợi suất người dùng nhập):

| Khoá | Tên | Đích, ngày đích | Đã có, góp/tháng, lợi suất | Kỳ vọng |
|---|---|---|---|---|
| G1 | `Quỹ dự phòng 6 tháng` | 370.000.000 ₫, 30/11/2027 | 250.000.000 ₫, 10.200.000 ₫, 3% | `ON_TRACK`, cần 9.239.000 ₫, phủ `110.40` |
| G2 | `Mua nhà phố Thủ Đức` | 3.000.000.000 ₫, 30/11/2029 | 1.800.000.000 ₫, 21.000.000 ₫, 5% | `AT_RISK`, cần 23.466.000 ₫, phủ `89.49`, lệch `0.105100` |
| G3 | `Học phí đại học cho con` | 1.200.000.000 ₫, 31/08/2034 | 200.000.000 ₫, 5.000.000 ₫, 6% | `BEHIND`, cần 7.473.000 ₫, phủ `66.90`, lệch `0.331000` |
| G4 | `Du lịch Nhật Bản cuối năm` | 60.000.000 ₫, 30/11/2026 | 45.000.000 ₫, 5.000.000 ₫, 0% | `DUE_THIS_MONTH`, `NO_CONTRIBUTION_MONTH` |
| G5 | `Quỹ cưới` | 300.000.000 ₫, đổi bằng SQL sang 31/10/2026 | 240.000.000 ₫, 10.000.000 ₫, 4% | `EXPIRED`, `TARGET_DATE_PASSED` |
| G6 | `Quỹ hưu trí` | 5.000.000.000 ₫, 31/12/2046 | 600.000.000 ₫, không góp, 6% | `PLANNED_MONTHLY_CONTRIBUTION_MISSING` |

### S — Áp lực (`adv.stress@example.com`), biến thể #83 kích hoạt cả sáu rule health

`Tiền mặt` CASH 12.000.000 ₫ · `Tài khoản thanh toán` BANK_ACCOUNT 30.000.000 ₫ · `Căn hộ cho thuê Quận 2`
REAL_ESTATE 4.300.000.000 ₫ LOW, định giá 07/10/2026 (52 ngày). Cùng hai công nợ như B. Lương 132.000.000 ₫
kết thúc 31/10/2026; còn cho thuê 14.800.000 ₫; chi sinh hoạt, nhà ở, bảo hiểm, thuế, gốc, lãi như B.

### Z — Một phần (`adv.partial@example.com`): không có thu nhập trong tháng

`Tiền mặt` CASH 50.000.000 ₫; ba khoản chi một lần tháng 11 (23.400.000, 12.800.000, 2.500.000); không công
nợ, không tỷ giá.

### U — USD không tỷ giá (`adv.usd@example.com`)

`Tiền mặt` CASH 100.000.000 ₫, `Tài khoản USD` BANK_ACCOUNT 5.000 USD; `Lương Công ty ABC` 40.000.000 ₫ MONTHLY,
chi sinh hoạt 20.000.000 ₫ MONTHLY; không công nợ, **không tỷ giá**.

### E — Chưa có dữ liệu (`adv.empty@example.com`)

Không bản ghi nào.

## Ma trận

| # | Kịch bản | Kỳ vọng |
|---|---|---|
| A01 | B, không `month` | `200`; `month 2026-11`, `asOfDate 2026-11-28`, `timeZone Asia/Ho_Chi_Minh`, `priorityMarkerCount 3`, `disclaimer` một câu tiếng Anh; `sources` = `[WEALTH_SNAPSHOT /api/v1/wealth-snapshot/insights 6, FINANCIAL_HEALTH /api/v1/financial-health?month=2026-11 6, GOAL_PROJECTION /api/v1/goal-projections 6]` |
| A02 | B, phân nhóm | `unevaluated` 3 (G5 `TARGET_DATE_PASSED`, G4 `NO_CONTRIBUTION_MONTH`, G6 `PLANNED_MONTHLY_CONTRIBUTION_MISSING`); `positive` 7 (`DEBT_TO_ASSET_RATIO`, 5 rule health trừ `INCOME_CONCENTRATION`, G1); `attention` 8 (`CATEGORY_CONCENTRATION`, `LOW_LIQUIDITY_SHARE`, `UPCOMING_LIABILITY_DUE`, `STALE_VALUATION`, `STALE_EXCHANGE_RATE`, `INCOME_CONCENTRATION`, G2, G3) |
| A03 | B, đối soát `recon.jq` | mọi finding chép đúng `status`, `observed`, `threshold` của insights, financial-health tháng 11 và goal-projections đọc ngay sau; `affectedRecords` = evidence upstream bỏ trùng; số finding = 12 + 6 |
| A04 | B, hành động và thứ tự | 8 hành động, không gộp: 1 `UPCOMING_LIABILITY_DUE` (tier 1, hạn 30/11); 2 `CATEGORY_CONCENTRATION` (0.249792); 3 `INCOME_CONCENTRATION` (0.222222); 4 `LOW_LIQUIDITY_SHARE` (0.041493); 5 G2 (hạn 30/11/2029); 6 G3 (hạn 31/08/2034); 7 `STALE_EXCHANGE_RATE` (14.4); 8 `STALE_VALUATION` (0.6). `priority` true đúng ở 1–3 |
| A05 | B, cùng bản ghi khác họ | `Căn hộ Thảo Điền` nằm trong `CATEGORY_CONCENTRATION`, `LOW_LIQUIDITY_SHARE`, `STALE_VALUATION`: ba hành động riêng |
| A06 | B, lưu ý | `STALE_EXCHANGE_RATE` → `USER_DECLARED_EXCHANGE_RATE`; G2, G3 → `USER_ASSUMED_RETURN`; `CATEGORY_CONCENTRATION` → `MANUAL_ENTRY_NOT_RECONCILED` |
| A07 | B, `?month=2026-11` | byte-identical với A01 |
| A08 | B, đọc lần hai | byte-identical với A01 |
| A09 | B, `?userId=<S>` | byte-identical với A01 |
| A10 | B, `?month=2026-10` | `month 2026-10`, nguồn health `?month=2026-10`; finding health bằng `/financial-health?month=2026-10`; finding snapshot và goal như A01 |
| A11 | S | `attention` 11 (5 snapshot trừ `DEBT_TO_ASSET_RATIO`, 6 health); `positive` 1; `unevaluated` 0 |
| A12 | S, gộp | hành động 1 gộp `UPCOMING_LIABILITY_DUE` + `LIQUIDITY_SHORT_FOR_UPCOMING_OBLIGATIONS` (chung `Khoản vay mua căn hộ`, `Thẻ tín dụng`) + `FORECAST_CASH_NEGATIVE` (chung tiền mặt và tài khoản với rule thanh khoản), tier 1, hợp bản ghi, hạn 30/11/2026; `attention` vẫn đủ 11 |
| A13 | S, không gộp | `CATEGORY_CONCENTRATION`, `LOW_LIQUIDITY_SHARE`, `STALE_VALUATION` cùng `Căn hộ cho thuê Quận 2` mà ba hành động riêng; `RESERVE_BELOW_REFERENCE` cùng họ với `LOW_LIQUIDITY_SHARE` nhưng không chung bản ghi → riêng |
| A14 | S, thứ tự | 9 hành động: gộp tier 1; `HIGH_DEBT_PAYMENT_BURDEN` (2.137066); `SPENDING_EXCEEDS_INCOME` (lệch `null`); `CATEGORY_CONCENTRATION` (0.980654); `RESERVE_BELOW_REFERENCE` (0.766667); `INCOME_CONCENTRATION` (0.666667); `LOW_LIQUIDITY_SHARE` (0.650545); `STALE_EXCHANGE_RATE` (14.4); `STALE_VALUATION` (0.733333); `FORECAST_FROM_ENTERED_SCHEDULES` có trong `noteIds` của hành động gộp |
| A15 | Z (một phần) | `unevaluated` 6: `UPCOMING_LIABILITY_DUE` `NO_LIABILITIES`, `STALE_EXCHANGE_RATE` `MISSING_EXCHANGE_RATE`, `RESERVE_BELOW_REFERENCE` / `HIGH_DEBT_PAYMENT_BURDEN` `INDICATOR_UNAVAILABLE`, `LIQUIDITY_SHORT…` `NO_LIABILITIES`, `INCOME_CONCENTRATION` `NO_INCOME`; `positive` 4; `attention` 2 (`CATEGORY_CONCENTRATION`, `SPENDING_EXCEEDS_INCOME`); 2 hành động, không hành động nào từ finding chưa đánh giá |
| A16 | U (USD không tỷ giá) | `unevaluated` 8, lý do `MISSING_EXCHANGE_RATE` ở 4 finding snapshot và `INDICATOR_PARTIAL` ở 2 rule health; mọi finding có bản ghi USD mang `USER_DECLARED_EXCHANGE_RATE`; `attention` 1 (`INCOME_CONCENTRATION`), 1 hành động |
| A17 | E (trống) | `200`; `unevaluated.count 12` (6 + 6), `positive`, `attention`, `suggestions` rỗng; `sources[*].findingCount` `[6, 6, 0]` |
| A18 | Quyền sở hữu | body B không chứa id nào của S, U, Z; body S không chứa id nào của B |
| A19 | `month` sai: `2026-13`, `11%2F2026`, `2026-9`, rỗng, hai `month` | `400`, `type`, `title`, `status`, `detail`, `errors` bằng đúng body `/financial-health` cùng query; không echo giá trị |
| A20 | Không token / token sai | `401` `urn:rikkaus:problem:unauthorized` |
| A21 | `POST` | `405` |
| A22 | `Accept: application/xml` | `406` |
| A23 | Ngôn ngữ hành động (mọi body) | mỗi `action` bắt đầu `You may `, kết thúc `then review the result.`, không chứa `guarantee`, `will`, `ensure`, `must`, `should`, `urgent`, `immediately`, `!` |
| A24 | Bất biến (mọi body) | mỗi finding ở đúng một nhóm theo status; `tier`/`actionFamily` chỉ có ở `ATTENTION`; mỗi finding attention ở đúng một hành động; hai hành động cùng họ không chung bản ghi (không bỏ sót gộp); hạng 1…n theo bộ so sánh; `priority` đúng ở `min(3, n)` mục đầu; `dueDate` hành động = gần nhất, `relativeDeviation` = lớn nhất, `tier` = nhỏ nhất của các finding; `relativeDeviation` mỗi finding khớp công thức |
| A25 | Kiến trúc | `ArchitectureRulesTest` xanh với rule đã tinh chỉnh; advisor không import `..domain..` của slice khác (`grep`) |
| A26 | Build | `./mvnw clean verify` xanh; `OpenApiContractIT` có path mới; `npm run api:check` xanh |
