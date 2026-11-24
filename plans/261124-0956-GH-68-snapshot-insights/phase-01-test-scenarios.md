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
scenario trước, không unit test). Mọi số kỳ vọng tính bằng Python `decimal` từ fixture và quy tắc của
[plan.md](./plan.md); khi chạy, body API được so với bảng này chứ không phải ngược lại.

## Ngày

`D` là hôm nay theo `Asia/Ho_Chi_Minh`; script tính mọi ngày tương đối lúc chạy. Lần chạy kèm implementation
là `D = 24/11/2026`.

| Ký hiệu | 24/11/2026 | Ký hiệu | 24/11/2026 |
|---|---|---|---|
| `D−73` (ngày tỷ giá cố định 12/09/2026) | 12/09/2026 | `D+12` | 06/12/2026 |
| `D−48` | 07/10/2026 | `D+20` | 14/12/2026 |
| `D−31` | 24/10/2026 | `D+30` (biên, vẫn trong cửa sổ) | 24/12/2026 |
| `D−30` | 25/10/2026 | `D+31` | 25/12/2026 |
| `D−6`, `D−5`, `D−2` | 18/11, 19/11, 22/11/2026 | `D+40`, `D+45` | 03/01/2027, 08/01/2027 |

Riêng user A giữ ngày tỷ giá `12/09/2026` của fixture 16.3, nên tuổi tỷ giá là `D − 12/09/2026` (73 ngày
khi chạy ngày 24/11/2026).

## Ngưỡng mặc định

`maxCategoryShare 0.500000`, `maxDebtToAssetRatio 0.400000`, `maxLowLiquidityShare 0.600000`,
`dueWindowDays 30`, `maxValuationAgeDays 30`, `maxExchangeRateAgeDays 5`.

## Dữ liệu

Tám user tạo qua `POST /api/v1/auth/dev-sign-in` (profile `local`), mọi bản ghi tạo qua API.

| User | Mục đích | Tài sản (giá trị, nhóm, thanh khoản, ngày định giá) | Công nợ | Tỷ giá |
|---|---|---|---|---|
| A An Nguyễn | Fixture 16.3 | Tài khoản ngân hàng 650.000.000 ₫ `BANK_ACCOUNT` HIGH D−3; Tiền gửi 12 tháng 1.200.000.000 ₫ `TERM_DEPOSIT` MEDIUM D−10; Danh mục cổ phiếu HOSE 2.150.000.000 ₫ `LISTED_SECURITIES` HIGH D−1; Căn hộ Thảo Điền 7.500.000.000 ₫ `REAL_ESTATE` LOW D−48; Tiền gửi USD 20.000 USD `BANK_ACCOUNT` HIGH D−5 | Khoản vay mua căn hộ 1.500.000.000 ₫, trả 18.750.000 ₫/tháng, kỳ tới D+12; Dư nợ thẻ tín dụng 32.000.000 ₫, kỳ tới D+40 | 25.100, 12/09/2026 |
| O Bình Trần | Cả sáu `OK` | Cổ phiếu FPT 900.000.000 ₫ HIGH D−2; Tiền gửi BIDV 12 tháng 800.000.000 ₫ MEDIUM D−7; Nhà phố Gò Vấp 1.200.000.000 ₫ LOW D−20; Tài khoản USD Vietcombank 10.000 USD HIGH D−1 | Vay mua ô tô VPBank 400.000.000 ₫, 12.500.000 ₫/tháng, kỳ tới D+45 | 25.300, D−2 |
| E0 Cường Lê | Đúng bằng ngưỡng → `OK` | Tiền mặt 400.000.000 ₫ `CASH` HIGH D−30; Trái phiếu Chính phủ 500.000.000 ₫ `BOND` LOW D−30; Tranh sơn mài 100.000.000 ₫ `COLLECTIBLE` LOW D−30 | Vay tiêu dùng 400.000.000 ₫, kỳ tới D+31 | 25.150, D−5 |
| E1 Dung Võ | Vượt ngưỡng một đơn vị → `ATTENTION` | Tiền mặt 399.000.000 ₫ HIGH D−29; Trái phiếu Chính phủ 501.000.000 ₫ LOW D−31; Tranh sơn mài 100.000.000 ₫ LOW D−10 | Vay margin SSI 300.000.000 ₫, kỳ tới D−5 (quá hạn); Vay tiêu dùng HD Saison 101.000.000 ₫, 3.000.000 ₫/tháng, kỳ tới D+30 | 25.150, D−6 |
| C Giang Hồ | Thiếu tỷ giá | Tiền mặt dự phòng 148.000.000 ₫ `CASH` HIGH D−3; Tài khoản USD 20.000 USD `BANK_ACCOUNT` HIGH D−3 | Khoản vay USD từ người thân 5.000 USD, đáo hạn D+20 | chưa khai |
| H Hà Phạm | Chỉ VND, không nợ, không tỷ giá | Tiền gửi ACB 800.000.000 ₫ `TERM_DEPOSIT` MEDIUM D−2; Cổ phiếu VNM 700.000.000 ₫ `LISTED_SECURITIES` HIGH D−2 | không | chưa khai |
| Z Khoa Đỗ | Tổng tài sản bằng 0 | Xe máy Honda SH đã bán 0 ₫ `VEHICLE_OR_VALUABLE_ASSET` LOW D−1 | Vay tiêu dùng FE Credit 50.000.000 ₫, không có ngày | chưa khai |
| N Nam Phạm | Chưa có bản ghi | không | không | chưa khai |

## Ma trận

Kết quả viết tắt: `ATT` = `ATTENTION`, `UNA` = `UNAVAILABLE`. Phân số in 6 chữ số như API trả.

| # | Kịch bản | Kỳ vọng |
|---|---|---|
| I01 | A, đầu response | `200`; `asOfDate = D`, `timeZone = Asia/Ho_Chi_Minh`, `complete = true`; `thresholds` đúng sáu giá trị mặc định; `ruleId` theo thứ tự `CATEGORY_CONCENTRATION, DEBT_TO_ASSET_RATIO, LOW_LIQUIDITY_SHARE, UPCOMING_LIABILITY_DUE, STALE_VALUATION, STALE_EXCHANGE_RATE` |
| I02 | A, tập trung | `ATT`; observed `0.624896` FRACTION, subject `REAL_ESTATE`, 7.500.000.000 / 12.002.000.000; threshold `GREATER_THAN 0.500000`; evidence = [Căn hộ Thảo Điền] |
| I03 | A, nợ/tài sản | `OK`; `0.127645`, 1.532.000.000 / 12.002.000.000; evidence = hai khoản nợ, vay mua căn hộ trước |
| I04 | A, thanh khoản thấp | `ATT`; `0.624896`, 7.500.000.000 / 12.002.000.000; evidence = [Căn hộ Thảo Điền] |
| I05 | A, hạn nợ | `ATT`; observed `12` DAYS, date D+12; threshold `AT_MOST 30`; evidence = [vay mua căn hộ, `NEXT_PAYMENT_DATE`, 18.750.000 ₫, `daysUntilDue 12`, `overdue false`]; thẻ tín dụng D+40 không có |
| I06 | A, định giá cũ | `ATT`; `48` DAYS, date D−48; evidence = [Căn hộ Thảo Điền, `ageDays 48`] |
| I07 | A, tỷ giá cũ | `ATT`; `73` DAYS, date 12/09/2026; evidence = [dòng tỷ giá 25.100, Tiền gửi USD 20.000 USD → 502.000.000 ₫] |
| I08 | A, đối soát với `GET /wealth-snapshot` | Tử/mẫu số = các tổng của snapshot; tỷ lệ nợ = `kpis.debtToAssetRatio`; evidence hạn nợ = `upcomingLiabilityDues.items`; ngày định giá cũ nhất = min `assets[].valuationDate`; ngày tỷ giá = `exchangeRate.effectiveDate`; mọi `sourceId` có trong snapshot |
| I09 | A, đọc lần hai | byte-identical với I01 |
| I10 | O, cả sáu `OK` | tập trung `0.380590` REAL_ESTATE; nợ `0.126863`; thanh khoản thấp `0.380590`; hạn nợ `45`, evidence = [vay ô tô D+45]; định giá `20`, evidence = [Nhà phố Gò Vấp]; tỷ giá `2`, evidence = [tỷ giá 25.300, tài khoản USD 10.000 USD → 253.000.000 ₫] |
| I11 | E0, bằng ngưỡng | cả sáu `OK`: `0.500000` BOND, `0.400000`, `0.600000`, `31`, `30`, `5` |
| I12 | E1, vượt một đơn vị | cả sáu `ATT`: `0.501000` BOND, `0.401000`, `0.601000`, `−5` (evidence hai hạn: margin D−5 `overdue true` rồi HD Saison D+30), `31` (evidence chỉ trái phiếu), `6` |
| I13 | C, thiếu tỷ giá | `complete false`; tập trung, nợ, thanh khoản thấp, tỷ giá đều `UNA` `MISSING_EXCHANGE_RATE`, `observed null`, evidence = các bản ghi USD với `amountVnd null`; hạn nợ `ATT` `20` (đáo hạn, 5.000 USD, `amountVnd null`); định giá `OK` `3` |
| I14 | C khai tỷ giá 25.100 ngày D | `complete true`; tập trung `ATT` `0.772308` BANK_ACCOUNT; nợ `OK` `0.193077`; thanh khoản thấp `OK` `0.000000`, evidence rỗng; hạn nợ `amountVnd 125.500.000`; tỷ giá `OK` `0` |
| I15 | H | tập trung `ATT` `0.533333` TERM_DEPOSIT; nợ `OK` `0.000000` (0 / 1.500.000.000); thanh khoản thấp `OK` `0.000000`; hạn nợ `UNA` `NO_LIABILITIES`; định giá `OK` `2`; tỷ giá `UNA` `MISSING_EXCHANGE_RATE`, evidence rỗng |
| I16 | Z | ba rule tỷ lệ `UNA` `ZERO_TOTAL_ASSETS`, evidence = [Xe máy 0 ₫]; hạn nợ `UNA` `NO_DUE_DATES`; định giá `OK` `1`; tỷ giá `UNA` |
| I17 | N, chưa có bản ghi | `200`; sáu `UNA`: `NO_ASSETS` ×4 (tập trung, nợ, thanh khoản, định giá), `NO_LIABILITIES`, `MISSING_EXCHANGE_RATE`; mọi evidence rỗng, mọi observed null |
| I18 | Ownership | evidence của A chỉ chứa id của A, không id nào của O; `?userId=<id của O>` cho body giống I01 |
| I19 | Không token | `401 unauthorized` Problem Details |
| I20 | Token sai | `401` |
| I21 | `POST` | `405` |
| I22 | `Accept: application/xml` | `406` |
| I23 | Khởi động lại với ngưỡng mới qua biến môi trường (`0.7`, `0.1`, `0.65`, `7`, `60`, `90`) | `thresholds` phản ánh giá trị mới; A: tập trung `OK`, nợ `ATT`, thanh khoản `OK`, hạn nợ `OK` (12 > 7, evidence = hạn gần nhất), định giá `OK`, tỷ giá `OK`; observed không đổi; `GET /wealth-snapshot` vẫn cửa sổ 30 |
| I24 | Khởi động lại với cấu hình mặc định | body A byte-identical với I01 (tái lập) |
| I25 | `maxCategoryShare = 1.5` | ứng dụng không khởi động, log nêu `rikkaus.snapshot.insights.max-category-share` |
| I26 | `dueWindowDays = -1` | không khởi động, nêu `due-window-days` |
| I27 | `maxValuationAgeDays` rỗng | không khởi động, nêu `max-valuation-age-days` |
| I28 | A ghi định giá mới cho căn hộ ngày D, rồi khai tỷ giá 25.100 ngày D | định giá `OK` `10` (evidence = Tiền gửi 12 tháng); tỷ giá `OK` `0`; các tỷ lệ không đổi |
