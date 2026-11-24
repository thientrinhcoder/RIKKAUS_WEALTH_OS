---
title: "Issue #68 nhận định Wealth Snapshot có giải thích (explainable insights)"
description: "Route đọc GET /api/v1/wealth-snapshot/insights trong package snapshot/insights: sáu rule cố định của backbone, ngưỡng tham chiếu cấu hình tập trung trong application.yml, mỗi kết quả mang metric quan sát, ngưỡng áp dụng, bằng chứng nguồn và lý do không tính được. Không lưu gì, không migration."
status: completed
priority: P1
effort: "1.5d"
issue: 68
branch: claude/issue-68-snapshot-insights-99a45a
tags: [backend, api, insights, snapshot]
blockedBy: [65, 66]
blocks: [67]
created: 2026-11-24
---

# Issue #68 nhận định Wealth Snapshot có giải thích

## Outcome

Client gọi `GET /api/v1/wealth-snapshot/insights` và nhận đúng sáu nhận định theo thứ tự cố định. Mỗi nhận
định cho biết: quy tắc nào (`ruleId`), kết quả (`ATTENTION` / `OK` / `UNAVAILABLE`), con số quan sát được
và cách tính (tử số, mẫu số hoặc ngày tham chiếu), ngưỡng tham chiếu của hệ thống đã áp dụng, các bản ghi
nguồn để UI link tới, và lý do khi không tính được. Response trả lại toàn bộ khối ngưỡng đang áp dụng, nên
đổi cấu hình rồi gọi lại cho kết quả tái lập được. Đây là đầu vào của Frontend #67.

## Nguồn sự thật

- Ticket #68, feature cha #18; design đã duyệt #66 (`plans/260919-issue-66-explainable-wealth-snapshot-insights/`
  ở checkout chính, artifact [`docs/explainable-wealth-snapshot-insights-review.html`](../../docs/explainable-wealth-snapshot-insights-review.html)).
- `RIKKAUS_WEALTH_OS_MVP_BACKBONE.md:200-216`: đúng sáu rule, ngưỡng cấu hình tập trung, UI gọi là "ngưỡng
  tham chiếu của hệ thống".
- `docs/design-guidelines.md:59-65` (mọi kết quả phải giải thích được), `:873-892` (fixture 16.3).
- Mã tái dùng: `snapshot/application/WealthSnapshotService` (mọi phép quy đổi USD→VND và các tổng);
  mẫu ngưỡng cấu hình: `goals/projection/application/GoalProjectionProperties`.

## Quyết định đã chốt (với coordinator, không mở lại)

1. Package `com.rikkaus.wealth.snapshot.insights` (`api` + `application`) trong slice `snapshot`.
2. Route `GET /api/v1/wealth-snapshot/insights`, không tham số. Không lưu, không cache, không migration.
3. Ngưỡng là record `@ConfigurationProperties("rikkaus.snapshot.insights")`, không default trong record, giá
   trị ở `application.yml`, ứng dụng không khởi động khi thiếu hoặc vô lý (thông báo nêu tên property).
4. Response: `asOfDate`, `timeZone`, `complete`, `thresholds`, `insights[6]`. Không xuất chuỗi tiếng Việt;
   client map hằng sang copy.
5. Không bao giờ trả 0 cho thứ không tính được. User chưa có bản ghi nhận đủ sáu insight, không 404.

## Quyết định thiết kế của Backend trong ticket này

| Điểm | Chốt |
|---|---|
| So sánh | `ATTENTION` khi giá trị quan sát **vượt** ngưỡng (`>`), riêng hạn nợ là `≤ cửa sổ`. Giá trị bằng ngưỡng là `OK`, khớp chữ "Trên 50%", "Quá 30 ngày" của artifact. |
| Phân số | Tỷ lệ 6 chữ số thập phân, `HALF_UP`, như `debtToAssetRatio` của snapshot. Status so trên chính giá trị đã làm tròn trả ra, để client tự tái lập status từ response. Ngưỡng phân số tối đa 6 chữ số thập phân. |
| Tập trung | Nhóm lớn nhất của `assetsByCategory` (đã xếp theo số tiền rồi mã nhóm). Bằng chứng: các tài sản của nhóm đó. |
| Thanh khoản thấp | Tỷ trọng tài sản `liquidity = LOW` trên tổng tài sản (backbone: "Low-liquidity assets represent a high share"). Bằng chứng: các tài sản `LOW`. |
| Hạn nợ | Mọi ngày trả kỳ tới và ngày đáo hạn (kể cả đã quá hạn) qua cùng hàm `dues` của snapshot. Quan sát: số ngày tới hạn gần nhất (âm khi quá hạn). `ATTENTION`: bằng chứng là mọi hạn trong cửa sổ; `OK`: hạn gần nhất ngoài cửa sổ. Với cửa sổ 30, bằng chứng trùng `upcomingLiabilityDues.items`. |
| Định giá cũ | Quan sát: tuổi (ngày) của định giá cũ nhất. `ATTENTION`: mọi tài sản quá ngưỡng, cũ nhất trước; `OK`: tài sản cũ nhất. |
| Tỷ giá cũ | Quan sát: tuổi của `effectiveDate` tỷ giá đã khai. Bằng chứng: dòng tỷ giá và mọi bản ghi USD nó quy đổi. Được đánh giá bất cứ khi nào có tỷ giá. |
| Không tính được | `NO_ASSETS`, `NO_LIABILITIES`, `NO_DUE_DATES`, `MISSING_EXCHANGE_RATE`, `ZERO_TOTAL_ASSETS`. Khi thiếu tỷ giá, bằng chứng là các bản ghi USD chưa quy đổi được, để UI chỉ ra cần khai tỷ giá cho bản ghi nào. |
| Ngưỡng khởi đầu | Giá trị fixture của artifact #66 (chưa phải cấu hình nghiệp vụ được PO duyệt, xem câu hỏi cho PO): nhóm `> 0.5`, nợ/tài sản `> 0.4`, thanh khoản thấp `> 0.6`, hạn nợ `≤ 30` ngày, định giá `> 30` ngày, tỷ giá `> 5` ngày. |

## Phases

| Phase | Nội dung | Status |
|---|---|---|
| 1 | [Ma trận test scenario](./phase-01-test-scenarios.md) | Completed |
| 2 | [Package insights, cấu hình, HTTP](./phase-02-insights-slice-and-http.md) | Completed |
| 3 | [Chạy kịch bản trên API thật](./phase-03-manual-verification.md) | Completed |
| 4 | [Contract, docs, bàn giao](./phase-04-contract-docs-handover.md) | Completed |

## Acceptance criteria

- [x] Sáu insight, thứ tự cố định, mỗi insight có rule, metric, ngưỡng áp dụng, nguồn.
- [x] Mỗi rule có kịch bản `ATTENTION` và `OK` chạy thật; biên ngưỡng (bằng ngưỡng là `OK`, vượt một đơn vị là `ATTENTION`).
- [x] Thiếu tỷ giá: ba rule tỷ lệ và rule tỷ giá `UNAVAILABLE`, hạn nợ và định giá vẫn tính.
- [x] User rỗng nhận sáu insight `UNAVAILABLE` với lý do, `200`.
- [x] Đổi ngưỡng bằng biến môi trường: `thresholds` đổi theo, status đổi theo, trả về cấu hình cũ cho đúng body cũ.
- [x] Cấu hình vô lý: ứng dụng không khởi động, thông báo nêu tên property.
- [x] Số liệu khớp `GET /wealth-snapshot` của cùng user; chỉ có bản ghi của chính user.
- [x] `401` thiếu/sai token; `405`; `406`.
- [x] `./mvnw clean verify` xanh; `openapi.json` sinh lại; `OpenApiContractIT` có path mới.
- [x] `docs/api-contract-conventions.md` có mục mới; evidence report.

## Ngoài phạm vi

Lưu hoặc lịch sử nhận định, ngưỡng do user tự đặt, thông báo đẩy, Basic Advisor, chỉ số sức khỏe tài chính
(#83), câu chữ tiếng Việt, tham số `asOf`, unit test (chỉ đạo PO).

## Rủi ro

- Hai mục của "What is not here" trong `docs/api-contract-conventions.md` ("No stale-rate rule", "Insights
  are still later work") trở nên sai. Brief chỉ cho phép thêm mục mới ở cuối, nên mục mới ghi rõ nó thay
  thế hai dòng đó; coordinator sửa hai dòng khi merge để tránh xung đột với session #107.
- `OpenApiContractIT` (ngoài slice) phải thêm một path vào inventory; chỉ đổi đúng một dòng.

## Rollback

Xóa package `snapshot/insights`, khối `rikkaus.snapshot.insights`, path trong `openapi.json` và inventory,
hoàn nguyên refactor nhỏ `dues` trong `WealthSnapshotService`. Không có dữ liệu cần dọn.
