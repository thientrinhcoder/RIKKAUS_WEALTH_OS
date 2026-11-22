---
title: "Issue #77 báo cáo dòng tiền tháng và cơ cấu thu chi"
description: "Route đọc GET /api/v1/cash-flow-reports/monthly trong slice cashflow: tổng thu, tổng chi, net, cơ cấu theo danh mục, thu nhập thụ động theo tài sản liên kết và danh sách kỳ nguồn để truy ngược, quy đổi USD bằng tỷ giá người dùng khai, mọi tổng cộng lại từ các dòng trong cùng response. Không migration."
status: completed
priority: P1
effort: "2d"
issue: 77
branch: thientrinhcoder/feat/issue-77-monthly-cash-flow-reports
tags: [backend, api, cash-flow, reporting]
blockedBy: [74]
blocks: [76, 83, 89]
created: 2026-11-22
---

# Issue #77 báo cáo dòng tiền tháng và cơ cấu thu chi

## Outcome

Người dùng pilot hỏi được "tháng 09/2026 tôi thu bao nhiêu, chi bao nhiêu, còn lại bao nhiêu, tiền đến từ
đâu và đi về đâu, phần nào là thu nhập thụ động của tài sản nào" bằng một request
`GET /api/v1/cash-flow-reports/monthly?month=2026-09`. Mỗi con số trong response cộng lại được từ danh
sách kỳ nguồn trả về trong chính response đó, và mỗi kỳ nguồn truy ngược về entry qua `sourceEntryId` và
`occurrenceDate`. Đây là đầu vào của màn hình báo cáo Frontend #76 và của các chỉ số #83.

## Quy tắc đã chốt

Sáu quy tắc trình bày của design đã duyệt
[`docs/monthly-cash-flow-reports-composition-review.html`](../../docs/monthly-cash-flow-reports-composition-review.html)
(issue #75, mục "Quy tắc") được giữ nguyên; các điểm design để lại cho Backend được chốt như sau.

| Quy tắc | Nguồn | Ví dụ |
|---|---|---|
| Một kỳ thuộc đúng tháng dương lịch chứa ngày của nó; tháng là khoảng đóng ngày 1 → ngày cuối tháng, đi qua service mở rộng của #74 | design quy tắc 01, #74 | `Chi phí sinh hoạt` neo 31/01 rơi 30/09/2026, dời cuối tháng |
| Net = tổng thu − tổng chi của tháng, không điều chỉnh gì khác | backbone "Monthly net cash flow", design 02 | `180.000.000 − 92.000.000 = 88.000.000 ₫` |
| Tổng một danh mục = tổng các kỳ cùng danh mục; tổng các danh mục = tổng loại. Không gộp "Khác" ở server: ngưỡng gộp là quyết định hiển thị của client | design 03 | `HOUSING` `18.750.000 ₫` |
| **Tỷ trọng composition**: phần trăm một chữ số thập phân, chia theo **phương pháp phần dư lớn nhất** để tổng luôn đúng `100.0` | quyết định của #77 | `HEALTHCARE` `5.0` thay vì `5.1`, nên chín danh mục chi cộng đúng `100.0` |
| **Thu nhập thụ động** = khoản thu có `linkedAssetId`, nhóm theo tài sản. Server không suy ra thụ động từ tên danh mục | design 04 | `Cho thuê căn hộ Thảo Điền` `18.000.000 ₫` dưới AS1 |
| **Nhóm "Chưa gán tài sản"** = khoản thu thuộc bốn danh mục mang tính thụ động (`RENTAL_INCOME`, `DIVIDEND`, `DEPOSIT_INTEREST`, `BOND_OR_FUND_INCOME`) nhưng không link tài sản; hiện riêng, không cộng vào thụ động | design 04 + trạng thái 08, brief #77 | `Lãi tiền gửi USD` `30.120.000 ₫` |
| Tỷ trọng thụ động (tổng và từng tài sản) là phần trăm trên **tổng thu**, làm tròn riêng `HALF_UP` một chữ số; không ép cộng thành con số nào | design màn "Thu nhập thụ động" | `22.500.000 / 180.000.000 = 12.5` |
| **Tiền tệ**: báo cáo bằng VND. Entry USD nhân tỷ giá người dùng đang khai (`wealth.user_settings`) qua `ConversionTrace`, làm tròn một lần về đồng (`HALF_UP`) **từng kỳ**; tổng cộng các kỳ đã làm tròn | backbone "Currency conversion", `shared/money` | `1.200 × 25.100 = 30.120.000 ₫` |
| **Thiếu tỷ giá**: không lỗi, không quy về 0. Dòng USD giữ `originalAmount`, `amount` là `null`; tổng chỉ cộng phần đã quy đổi; `complete` là `false`; `unconverted` nêu số kỳ và số USD thu/chi bị loại; dòng composition hay tài sản chứa kỳ đó có `partial` `true` | design quy tắc 05 + trạng thái 03 | C: thu `40.000.000 ₫`, `unconverted.income` `150.00` USD |
| Tháng rỗng trả đủ cấu trúc với `0.00` và mảng rỗng; tỷ trọng không tính được là `null` ("Không khả dụng"), không phải `0` | design 06, brief | `month=2025-06` |
| Tháng tương lai được phép: báo cáo là hàm thuần của catalog và tháng, không so với "hôm nay" | quyết định của #77, theo #74 | `month=2026-12` có 9 kỳ |
| Tỷ giá áp dụng là tỷ giá đang khai, không tra theo ngày kỳ: settings chỉ lưu một tỷ giá. Response mang `exchangeRate{usdToVndRate, effectiveDate}` để truy vết | backbone "retain the rate and date used" | `25100.000000`, `2026-09-12` |

Vì sao `?month=` thay vì `/cash-flow-reports/2026-09`: cùng kiểu với `?from&to` của #74, và một giá trị sai
được báo là `validation-failed` có `field: "month"` như mọi input khác thay vì một 404 không tên trường.
`/monthly` để chỗ cho báo cáo khác cùng collection mà không đổi path này.

Vì sao phần dư lớn nhất: design yêu cầu "tổng các nhóm hiển thị phải cộng lại đúng bằng tổng tháng", và một
cột tỷ trọng cộng ra `100.1%` là thứ người dùng thấy ngay. Phương pháp này chỉ lệch tối đa `0.1` so với làm
tròn riêng từng dòng, và chỉ ở dòng có phần dư nhỏ nhất. Số tiền vẫn là nguồn sự thật; tỷ trọng là phái sinh.

## Contract

`GET /api/v1/cash-flow-reports/monthly?month=YYYY-MM`, `bearerAuth`, owner-scoped bằng token.

| Trường | Ý nghĩa |
|---|---|
| `month`, `from`, `to`, `currency` | tháng hỏi, ngày đầu và cuối (đều tính vào), luôn `VND` |
| `exchangeRate` | `{usdToVndRate, effectiveDate}` đang khai, `null` nếu chưa khai |
| `complete` | `false` khi có kỳ USD không quy đổi được |
| `totals` | `{income, expense, net}` bằng VND |
| `unconverted` | `{occurrenceCount, income, expense}`, hai số sau bằng USD |
| `incomeComposition`, `expenseComposition` | `[{category, amount, sharePercent, occurrenceCount, partial}]`, sắp `amount` giảm dần rồi tên category |
| `passiveIncome` | `{total, sharePercentOfIncome, byAsset[{assetId, amount, sharePercentOfIncome, occurrenceCount, partial}], unassigned{amount, occurrenceCount, partial}}` |
| `occurrences` | `[{sourceEntryId, occurrenceDate, sequence, shiftedToMonthEnd, entryType, category, recurrence, linkedAssetId, notes, originalAmount, originalCurrency, amount}]`, sắp theo ngày rồi `sourceEntryId` như #74 |

Lỗi: `month` thiếu, rỗng, sai định dạng hoặc tháng không tồn tại → `400 validation-failed` với
`errors[{field: "month"}]`. Không token, token hết hạn, token sai chữ ký → `401`. Method khác `GET` → `405`.

`assetId` không kèm tên tài sản: client đã có danh sách `GET /api/v1/assets`, và slice `cashflow` không đọc
cột nào của `wealth.assets` ngoài khóa ngoại nó vốn có.

## Constraints

- Package mới `com.rikkaus.wealth.cashflow.reporting` (`api`, `application`), chỉ phụ thuộc `shared` và chính
  slice `cashflow`. Tái dùng `CashFlowOccurrenceService` và `OccurrenceWindow` của #74 nguyên trạng; **không
  sửa file hiện có của `cashflow`** (#101 đang sửa controller/service list, #80 làm `cashflow/forecast`).
- Đọc tỷ giá bằng native query trên `wealth.user_settings` trong repository của package mới, cách
  `onboarding` đã làm, vì slice không được import `settings`.
- `BigDecimal` cho mọi phép tính, không float. Không migration.
- Không viết unit test hay integration test mới (chỉ đạo Product Owner). Scenario viết trước ở
  [phase 1](./phase-01-test-scenarios.md), chạy thật qua HTTP vào PostgreSQL Docker, ghi evidence.
  `OpenApiContractIT` mở rộng path mới; `ApplicationContextIT` không đổi vì không có bảng mới.

## Non-goals

Forecast ba tháng (#80), chỉ số sức khỏe tài chính (#83), insight, export, so sánh nhiều tháng (client gọi
route này nhiều lần), gộp nhóm nhỏ thành "Khác", tỷ giá theo lịch sử, tên tài sản trong response.

## Phases

| Phase | Tên | Trạng thái | Kết quả |
|---|---|---|---|
| 1 | [Danh sách test scenario](./phase-01-test-scenarios.md) | Hoàn tất | 40 case viết trước khi code |
| 2 | [Báo cáo và HTTP](./phase-02-report-service-and-http.md) | Hoàn tất | `ReportMonth`, service tổng hợp, controller |
| 3 | [Contract và docs](./phase-03-contract-and-docs.md) | Hoàn tất | `openapi.json`, `OpenApiContractIT`, conventions, guide; `clean verify` xanh |
| 4 | [Xác minh thủ công](./phase-04-manual-verification.md) | Hoàn tất | Chạy 40 case, ghi evidence |

## Acceptance Criteria

- [x] Tổng thu, tổng chi, net của tháng khớp các kỳ nguồn; net = thu − chi.
- [x] Composition thu và chi theo category, tỷ trọng cộng đúng `100.0`.
- [x] Thu nhập thụ động nhóm đúng theo tài sản liên kết; khoản thụ động chưa link hiện ở nhóm riêng.
- [x] Mỗi kỳ nguồn truy ngược về entry qua `sourceEntryId` và `occurrenceDate`.
- [x] USD quy đổi bằng tỷ giá đang khai, response mang tỷ giá và ngày hiệu lực; thiếu tỷ giá thì đánh dấu một
      phần, không quy về 0.
- [x] Tháng rỗng trả đủ cấu trúc; không thấy dữ liệu user khác; input sai là `validation-failed`; 401 ba
      kiểu token; 405 method khác.
- [x] Reconciliation kiểm bằng `jq` trên body thật cho mọi tổng.
- [x] `openapi.json` regenerate, `clean verify` xanh không flag.

## Verification gap

Không có automated test nào gọi vào route này hay vào phép tổng hợp, chia tỷ trọng và quy đổi. Đó là chỉ đạo
của Product Owner, cùng tư thế #50, #53, #71, #74, #86 đã ship. Hậu quả cụ thể: một regression trong
`MonthlyCashFlowReportService` — ví dụ ai đó đổi phép chia phần dư lớn nhất sang làm tròn riêng, làm cột chi
của tháng 09/2026 cộng ra `100.1`, hay cộng USD chưa quy đổi như thể là đồng — chỉ bị phát hiện khi chạy lại
`docs/manual-test-guide-cash-flow-reports.md`. Đây là loại logic mà một unit test bảng (tổng × tỷ trọng ×
thiếu tỷ giá) bảo vệ rẻ nhất; nên cân nhắc bổ sung khi chỉ đạo thay đổi. Case múi giờ JVM của #74 không chạy
lại: báo cáo không đọc đồng hồ nào, chỉ dùng ngày lịch qua cùng service. `OpenApiContractIT` vẫn bắt path bị
lộ hoặc thiếu, `ArchitectureRulesTest` vẫn bắt import chéo slice.

## Câu hỏi cho Product Owner

1. Tỷ trọng chia theo phần dư lớn nhất (cột luôn cộng `100,0%`, `Y tế` hiện `5,0%`) hay làm tròn riêng từng
   dòng như prototype (`Y tế` `5,1%`, cột cộng `100,1%`)?
2. Nhóm "Chưa gán tài sản" chỉ gồm bốn danh mục thụ động không link (`Lãi tiền gửi USD` `30.120.000 ₫`), hay
   mọi khoản thu không link (thêm cả `Lương tháng` `95.000.000 ₫`)?
3. Thiếu tỷ giá: giữ trạng thái một phần như design đề xuất (đã làm), hay từ chối cả báo cáo cho tới khi khai?
4. Tháng tương lai: cho xem như hiện tại (`12/2026` hiện `117.500.000 ₫` thu theo lịch định kỳ), hay chặn để
   không lẫn với forecast #80?

<!-- slug: issue-77-monthly-cash-flow-reports -->

## Kết quả

Hoàn tất ngày 22/11/2026 tại commit `7683dee` (code, contract, plan) và commit kế tiếp (docs, guide,
evidence). `clean verify` xanh không dùng update flag: 81 unit test, 50 integration test. 40 case (60 phép
kiểm) chạy thật qua HTTP vào PostgreSQL Docker. Lần chạy đầu đạt 58/60: hai phép kiểm fail vì lỗi của script
kiểm (`jq` cộng tỷ trọng bằng số thực; danh sách bảng kỳ vọng viết sai), không phải của API; sửa script, chạy
lại trên database mới, **60/60 đạt**. Fixture tháng 09/2026 tái dựng đúng `180.000.000 ₫` thu,
`92.000.000 ₫` chi, `88.000.000 ₫` net của design #75. Evidence ở
[`manual-verification-261122-1538-GH-77-monthly-cash-flow-reports.md`](../reports/manual-verification-261122-1538-GH-77-monthly-cash-flow-reports.md).
