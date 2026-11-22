---
title: "Issue #65 tổng quan tài sản ròng (wealth snapshot dashboard)"
description: "Route đọc GET /api/v1/wealth-snapshot trong slice mới snapshot: sáu KPI theo công thức canonical, phân bổ theo danh mục và thanh khoản, năm tài sản lớn nhất, nghĩa vụ công nợ sắp đến hạn, ngày nguồn để dựng màn độ mới, và mọi dòng nguồn để đối soát. Quy đổi USD bằng tỷ giá người dùng khai, partial state không bao giờ trả 0. Không migration."
status: in-progress
priority: P1
effort: "2d"
issue: 65
branch: thientrinhcoder/feat/issue-65-wealth-snapshot-dashboard
tags: [backend, api, dashboard, reporting]
blockedBy: [53, 59, 62, 50]
blocks: [64, 68, 83, 113]
created: 2026-11-22
---

# Issue #65 tổng quan tài sản ròng (wealth snapshot dashboard)

## Outcome

Người dùng pilot mở màn Tổng quan và thấy "tôi có bao nhiêu, nợ bao nhiêu, ròng còn bao nhiêu, bao nhiêu
là tiền dễ rút, dữ liệu mới tới ngày nào, sắp phải trả gì" bằng một request `GET /api/v1/wealth-snapshot`.
Mỗi tổng trong response cộng lại được từ các dòng tài sản và công nợ trả về trong chính response đó, mỗi
dòng mang `id` để truy ngược về catalog. Đây là đầu vào của màn dashboard Frontend #64, của insight #68,
của chỉ số sức khỏe tài chính #83 và của #113.

## Quy tắc đã chốt

Sáu quyết định đã duyệt của design #63 (`plans/260919-0748-issue-63-wealth-snapshot-dashboard-design/plan.md`
ở checkout chính, artifact [`docs/wealth-snapshot-dashboard-review.html`](../../docs/wealth-snapshot-dashboard-review.html))
được giữ nguyên; các điểm design để lại cho Backend được chốt như sau.

| Quy tắc | Nguồn | Ví dụ (fixture design #63) |
|---|---|---|
| `total_assets_vnd` = tổng `current_value` mới nhất của mọi tài sản, đã quy đổi VND. `wealth.assets` là bản sao giá trị của bản ghi định giá hiện hành (#59 giữ chúng đồng bộ trong cùng transaction) | backbone §7 | `12.002.000.000 ₫` từ 11 tài sản |
| `total_liabilities_vnd` = tổng `outstanding_balance` đã quy đổi | backbone §7 | `1.500.000.000 + 32.000.000 = 1.532.000.000 ₫` |
| `net_worth_vnd = total_assets_vnd − total_liabilities_vnd`, được phép âm | backbone §7 | `10.470.000.000 ₫` |
| `debt_to_asset_ratio = liabilities / assets`, là **tỷ số** (không phải phần trăm), 6 chữ số thập phân `HALF_UP`; `null` khi tổng tài sản bằng 0 hoặc một trong hai tổng không khả dụng. Client nhân 100 và hiện một chữ số | backbone §7, design "12,7645…% → 12,8%" | `0.127645` |
| `liquid_asset_value_vnd` = tổng tài sản có `liquidity = HIGH` | design #63 quyết định 1 | `148.000.000 + 502.000.000 + 2.150.000.000 + 380.000.000 = 3.180.000.000 ₫` |
| **Quy đổi**: VND giữ nguyên; USD × tỷ giá người dùng đang khai (`wealth.user_settings`) qua `ConversionTrace`, làm tròn một lần về đồng `HALF_UP` **từng dòng**, rồi cộng các dòng đã làm tròn. Không dùng tỷ giá chụp trên từng bản ghi định giá | backbone "Currency conversion", cách #77/#80 đã làm | `20.000 USD × 25.100 = 502.000.000 ₫` |
| **Partial**: có dòng USD mà chưa khai tỷ giá thì dòng đó `valueVnd: null`; mọi tổng chứa nó có `amountVnd: null` ("Không khả dụng") kèm `knownAmountVnd` (tổng phần đã quy đổi) và `excludedRecordCount`. KPI phái sinh (`netWorth`, `debtToAssetRatio`) là `null`. Không bao giờ trả 0 thay cho không khả dụng. `complete: false` | design #63 quyết định 2 | `Tài khoản USD` chưa có tỷ giá: tổng tài sản `null`, đã biết `148.000.000 ₫`, loại 1 |
| **Phân bổ theo danh mục**: một dòng cho mỗi category có ít nhất một tài sản (kể cả giá trị 0), sắp `knownAmountVnd` giảm dần rồi mã category. Không gộp `Khác` ở server: "4 nhóm lớn nhất + Khác" là quyết định hiển thị của client | design #63 quyết định 4 | `REAL_ESTATE 6.100.000.000`, … `COLLECTIBLE 110.000.000` |
| **Phân bổ theo thanh khoản**: một dòng cho mỗi mức có tài sản, thứ tự cố định `HIGH`, `MEDIUM`, `LOW` | backbone "Donut chart: assets by liquidity level" | `3.180.000.000 / 1.842.000.000 / 6.980.000.000` |
| **So sánh tài sản vs công nợ**: dùng thẳng `kpis.totalAssets` và `kpis.totalLiabilities`, không lặp một khối riêng | backbone "Comparison bar" | — |
| **Năm tài sản lớn nhất**: sắp `valueVnd` giảm dần, hòa thì theo tên với **collation tiếng Việt** (`java.text.Collator` locale `vi-VN`), rồi `id`. Tài sản chưa quy đổi được không xếp hạng được: không vào danh sách, đếm ở `excludedRecordCount` | design #63 quyết định 5 | hòa `420.000.000 ₫`: `Trái phiếu Chính phủ Việt Nam` đứng thứ 5, `Xe Volvo XC60` thứ 6 |
| **Nghĩa vụ sắp đến hạn**: một mục cho mỗi `next_payment_date` (`PAYMENT`, số tiền là `recurring_payment_amount`, có thể `null`) và mỗi `maturity_date` (`MATURITY`, số tiền là dư nợ) **không muộn hơn hôm nay + 30 ngày**. Ngày đã qua vẫn hiện, gắn `overdue: true` (design #60: "overdue payment is a state the design shows"). Sắp theo ngày, `PAYMENT` trước `MATURITY`, tên, `id` | brief #65 "ví dụ 30 ngày tới", design #60 | `Khoản vay mua nhà` `18.750.000 ₫` |
| **Ngày dữ liệu** `lastDataUpdate` = ngày nguồn đóng góp mới nhất: `valuation_date` của tài sản, ngày (theo `Asia/Ho_Chi_Minh`) của `updated_at` công nợ, `effectiveDate` tỷ giá **chỉ khi tỷ giá đã quy đổi ít nhất một dòng USD**. `sourceDates` liệt kê từng ngày nguồn cho màn độ mới. Không nguồn nào thì `null` | design #63 quyết định 6 | — |
| **Không phán xét độ cũ**: không ngưỡng N ngày, không cờ "cũ" — đó là insight #68 | design #63 "factual cues only", conventions "No stale-rate rule" | — |
| User mới: `200` với đủ cấu trúc, tổng `0.00`, mảng rỗng, `debtToAssetRatio` và `lastDataUpdate` `null`; không `404` | brief #65 | — |
| "Hôm nay" đọc một lần theo `Asia/Ho_Chi_Minh`, trả ở `asOfDate`. Hai request cùng ngày, cùng dữ liệu cho body giống từng byte | conventions "Time zone", #80 | — |

Vì sao `GET /api/v1/wealth-snapshot` (danh từ số ít, không id): mỗi user có đúng một bức tranh tài sản hiện
tại, giống `/settings/exchange-rate` và `/onboarding`; người xem lấy từ access token nên không có gì để
trỏ vào dữ liệu người khác. Tên theo tiêu đề design "Wealth snapshot dashboard" nhưng bỏ chữ `dashboard`
vì đó là màn hình của client, còn resource là bức tranh tài sản; #68 và #83 đọc cùng resource mà không vẽ
dashboard.

## Contract

`GET /api/v1/wealth-snapshot`, `bearerAuth`, không tham số, owner-scoped bằng token.

| Trường | Ý nghĩa |
|---|---|
| `asOfDate`, `timeZone`, `currency` | ngày "hôm nay" đã dùng, `Asia/Ho_Chi_Minh`, luôn `VND` |
| `exchangeRate` | `{usdToVndRate, effectiveDate}` đang khai, `null` nếu chưa khai |
| `complete` | `false` khi có ít nhất một dòng USD không quy đổi được |
| `kpis` | `totalAssets`, `totalLiabilities`, `liquidAssets` (mỗi cái là `{amountVnd, knownAmountVnd, recordCount, excludedRecordCount}`), `netWorth`, `debtToAssetRatio`, `lastDataUpdate` |
| `assetsByCategory[]` | `{category, amountVnd, knownAmountVnd, recordCount, excludedRecordCount}` |
| `assetsByLiquidity[]` | `{liquidity, amountVnd, knownAmountVnd, recordCount, excludedRecordCount}` |
| `largestAssets` | `{items[{assetId, name, category, liquidity, valueVnd, valuationDate}], excludedRecordCount}`, tối đa 5 |
| `upcomingLiabilityDues` | `{windowDays: 30, windowEnd, items[{liabilityId, name, category, dueKind, dueDate, overdue, amount, currency, amountVnd}]}` |
| `sourceDates[]` | `{sourceType: ASSET/LIABILITY/EXCHANGE_RATE, sourceId, name, date}`, sắp ngày giảm dần, loại nguồn, tên, id |
| `assets[]` | `{id, name, category, liquidity, value, currency, valueVnd, valuationDate}`, sắp `valueVnd` giảm dần (`null` cuối), tên tiếng Việt, `id` |
| `liabilities[]` | `{id, name, category, outstandingBalance, currency, outstandingBalanceVnd, nextPaymentDate, maturityDate, recurringPaymentAmount, recurringPaymentFrequency, updatedOn}`, cùng cách sắp |

Lỗi: không token, token hết hạn, token sai chữ ký → `401`. Method khác `GET` → `405`. `Accept` không phải
JSON → `406`. Không có input nên không có `400`.

## Constraints

- Package mới `com.rikkaus.wealth.snapshot` (`api`, `application`), chỉ phụ thuộc `shared`
  (`ArchitectureRulesTest`). Đọc `wealth.assets`, `wealth.liabilities`, `wealth.user_settings` bằng
  `JdbcClient` trong `SnapshotSourceReader` của slice, như `cashflow/forecast/ForecastSourceReader`. Query
  tỷ giá trùng nhỏ với `cashflow/reporting` và `cashflow/forecast`, coordinator gộp sau.
- `BigDecimal` cho mọi phép tính. Không migration, không cache, không ghi gì.
- Không viết unit test hay integration test mới (chỉ đạo Product Owner). Scenario viết trước ở
  [phase 1](./phase-01-test-scenarios.md), chạy thật qua HTTP vào PostgreSQL Docker, ghi evidence.
  `OpenApiContractIT` mở rộng path mới; `ApplicationContextIT` không đổi vì không có bảng mới.

## Non-goals

Insight và ngưỡng (#68), cờ dữ liệu cũ, tỷ giá lịch sử hoặc tỷ giá chụp theo từng định giá, export, cache,
gộp `Khác`, tỷ trọng phần trăm ở server (client chia từ số tiền), lịch sử tài sản ròng theo thời gian.

## Phases

| Phase | Tên | Trạng thái | Kết quả |
|---|---|---|---|
| 1 | [Danh sách test scenario](./phase-01-test-scenarios.md) | Hoàn tất | 32 case viết trước khi code |
| 2 | [Slice snapshot và HTTP](./phase-02-snapshot-slice-and-http.md) | Hoàn tất | reader, service, controller |
| 3 | [Contract và docs](./phase-03-contract-and-docs.md) | Đang làm | `openapi.json`, `OpenApiContractIT`, conventions, guide |
| 4 | [Xác minh thủ công](./phase-04-manual-verification.md) | Chưa làm | chạy 32 case, ghi evidence |

## Acceptance Criteria

- [ ] Sáu KPI đúng công thức canonical; fixture design #63 tái dựng đúng `12.002.000.000`, `1.532.000.000`,
      `10.470.000.000`, `0.127645`, `3.180.000.000`.
- [ ] USD quy đổi bằng tỷ giá đang khai, response mang tỷ giá và ngày hiệu lực; thiếu tỷ giá thì partial,
      không quy về 0.
- [ ] Phân bổ danh mục, thanh khoản, năm tài sản lớn nhất và nghĩa vụ sắp đến hạn khớp các dòng nguồn.
- [ ] Hòa giá trị sắp theo tên tiếng Việt; ghi định giá mới làm KPI và `lastDataUpdate` đổi, lịch sử giữ nguyên.
- [ ] User mới đủ cấu trúc; không thấy dữ liệu user khác; 401 ba kiểu token; 405; 406.
- [ ] Reconciliation kiểm bằng `jq` trên body thật; hai lần gọi cho body giống từng byte.
- [ ] `openapi.json` regenerate, `clean verify` xanh không flag.

## Verification gap

Không có automated test nào gọi vào route này hay vào phép tổng hợp, quy đổi và xếp hạng. Đó là chỉ đạo
của Product Owner, cùng tư thế #53, #59, #62, #77, #80 đã ship. Hậu quả cụ thể: một regression trong
`WealthSnapshotService` — ví dụ ai đó đổi `null` thành `0` cho tổng có dòng USD chưa quy đổi, làm tài sản
ròng của user chưa khai tỷ giá hiện `148.000.000 ₫` như thể là con số thật, hoặc thay collation tiếng Việt
bằng so sánh chuỗi thường làm `Vàng SJC` chen trước `Ô tô Kia Morning` — chỉ bị phát hiện khi chạy lại
`docs/manual-test-guide-wealth-snapshot.md`. Đây là loại logic mà một unit test bảng (quy đổi × partial ×
hòa giá trị) bảo vệ rẻ nhất; nên cân nhắc khi chỉ đạo thay đổi. Cửa sổ 30 ngày phụ thuộc ngày chạy: guide
tính mọi ngày tương đối với hôm nay, nên lần chạy lại không cần sửa dữ liệu, nhưng case biên giữa đêm (chạy
lúc 23:59 và 00:00 giờ Việt Nam) không được chạy. `OpenApiContractIT` vẫn bắt path bị lộ hoặc thiếu,
`ArchitectureRulesTest` vẫn bắt import chéo slice.

## Câu hỏi cho Product Owner

1. Nghĩa vụ đã quá hạn (`Vay margin SSI` hạn `17/11/2026`, hôm nay `22/11/2026`) có hiện trong danh sách
   "Nghĩa vụ sắp tới" kèm nhãn `Quá hạn` (đã làm), hay chỉ hiện các hạn từ hôm nay tới `22/12/2026`?
2. Cửa sổ "sắp đến hạn" 30 ngày (đã làm, trả ở `windowDays`) có đúng ý, hay cần cho người dùng chọn?
3. Ngày dữ liệu tính cả ngày sửa công nợ (`updated_at`): công nợ nhập hôm nay làm cả dashboard hiện
   `22/11/2026` dù định giá mới nhất là `19/09/2026`. Giữ như brief, hay chỉ tính ngày định giá và ngày tỷ giá?
4. Tỷ giá không quy đổi dòng nào (user toàn VND) thì không tính vào ngày dữ liệu (đã làm). Đồng ý?

<!-- slug: issue-65-wealth-snapshot-dashboard -->
