---
title: "Issue #74 mở rộng lịch kỳ của dòng tiền định kỳ"
description: "Service mở rộng xác định trong slice cashflow: từ entry một lần hoặc định kỳ tháng/quý/năm sinh các kỳ trong một khoảng ngày, qua GET /api/v1/cash-flow-occurrences, tính khi đọc, không lưu, không migration."
status: in-progress
priority: P1
effort: "2d"
issue: 74
branch: thientrinhcoder/feat/issue-74-recurring-schedule-expansion
tags: [backend, api, cash-flow, schedule]
blockedBy: []
blocks: [73, 77, 80]
created: 2026-11-14
---

# Issue #74 mở rộng lịch kỳ của dòng tiền định kỳ

## Outcome

Người dùng pilot hỏi được "trong khoảng ngày này, các khoản thu chi của tôi rơi vào những ngày nào" qua
`GET /api/v1/cash-flow-occurrences?from=…&to=…`. Mỗi kỳ truy ngược được về entry gốc qua
`sourceEntryId`, mang số tiền, tiền tệ, loại, danh mục và ngày diễn ra. Kết quả xác định: cùng dữ liệu và
cùng khoảng luôn cho cùng body, không phụ thuộc giờ máy chủ hay múi giờ JVM, và không kỳ nào bị đếm hai
lần khi ghép các khoảng kề nhau. Đây là đầu vào cho báo cáo tháng #77, forecast #80 và màn hình lịch kỳ
của frontend #73.

## Quy tắc đã chốt

Năm quy tắc biên của design đã duyệt
[`docs/recurring-cash-flow-schedule-expansion-review.html`](../../docs/recurring-cash-flow-schedule-expansion-review.html)
(issue #72, mục 04) được giữ nguyên, cộng ba quyết định design để lại cho #74:

| Quy tắc | Nguồn | Ví dụ |
|---|---|---|
| `effectiveDate` được tính vào: kỳ đầu rơi đúng ngày bắt đầu | design, quy tắc 1 | `Lương` từ 31/01/2026 có kỳ 1 ngày 31/01/2026 |
| `endDate` được tính vào: kỳ rơi đúng ngày kết thúc vẫn sinh ra | design, quy tắc 2 | `Tiền thuê nhà` đến 30/06/2026 có kỳ 30/06/2026 |
| Ngày neo không có trong tháng thì dời về cuối tháng, đánh dấu `shiftedToMonthEnd`, không bao giờ sang tháng khác; ngày neo giữ nguyên nên tháng sau quay lại | design, quy tắc 3 | 31/01 → 28/02/2026 (dời) → 31/03/2026 |
| Mỗi kỳ thuộc đúng một kỳ báo cáo là tháng chứa ngày diễn ra | design, quy tắc 4 | kỳ 28/02/2026 thuộc 02/2026 |
| **Khoảng truy vấn là khoảng đóng `[from, to]`, cả hai đầu tính vào** | quyết định của #74 | `2026-01-01..2026-03-31` và `2026-04-01..2026-06-30` không chung ngày nào và không hở ngày nào |
| **Khoảng tối đa 24 tháng**: `to` phải trước `from + 24 tháng` | quyết định của #74, theo tầm nhìn dài nhất của design (24 kỳ tháng) | `2026-01-01..2027-12-31` được; `..2028-01-01` bị từ chối |
| **Không múi giờ**: mọi ngày là ngày lịch (`LocalDate`), không có giờ, không so với "hôm nay" | quyết định của #74, trả lời câu hỏi 5 của design | kết quả giống hệt khi JVM chạy ở `Pacific/Pago_Pago` (UTC−11) |
| **Định danh kỳ** là cặp (`sourceEntryId`, `occurrenceDate`), cộng `sequence` là số thứ tự kỳ tính từ `effectiveDate` | quyết định của #74, trả lời câu hỏi 4 của design | kỳ 28/02/2026 của `Lương` là `sequence: 2` |

Vì sao khoảng đóng thay vì nửa mở: ngày không có thành phần giờ, nên `[01/01, 31/03]` và `[01/04, 30/06]`
đã chia lịch thành hai phần rời nhau mà không cần quy ước "đến trước ngày". Khoảng đóng cũng khớp cách
design và entry viết ngày (ngày bắt đầu và ngày kết thúc đều tính vào), và khớp cách báo cáo tháng gọi:
`from` là ngày 1, `to` là ngày cuối tháng.

Vì sao 24 tháng: tầm nhìn dài nhất của prototype là 24 kỳ hàng tháng; #77 cần 1 tháng, #80 cần 3 tháng. Giới
hạn chặn một request vô tình xin hàng chục năm kỳ, và vẫn rộng gấp tám lần nhu cầu lớn nhất đã biết.

## Contract

`GET /api/v1/cash-flow-occurrences?from=YYYY-MM-DD&to=YYYY-MM-DD`, `bearerAuth`, owner-scoped bằng token.
`200` với mảng kỳ, sắp theo `occurrenceDate` tăng dần rồi `sourceEntryId` tăng dần theo chuỗi; khoảng
không có kỳ nào là `[]`.

| Trường | Kiểu | Ý nghĩa |
|---|---|---|
| `sourceEntryId` | uuid | entry gốc; `GET /api/v1/cash-flow-entries/{sourceEntryId}` đọc được nó |
| `occurrenceDate` | date | ngày diễn ra |
| `sequence` | integer ≥ 1 | kỳ thứ mấy tính từ `effectiveDate`; entry một lần luôn là `1` |
| `shiftedToMonthEnd` | boolean | ngày neo không có trong tháng nên kỳ bị dời về cuối tháng |
| `entryType`, `category`, `amount`, `currency`, `recurrence` | như entry | chép từ entry tại thời điểm đọc; `recurrence` là `null` với entry một lần |

Lỗi: `from`/`to` thiếu, rỗng, sai định dạng, không phải ngày có thật, `from > to`, khoảng ≥ 24 tháng → `400
validation-failed` với `errors` gom hết trong một lần. Không token, token hết hạn, token sai chữ ký → `401
unauthorized`. Method khác `GET` → `405`.

Tên route chọn `cash-flow-occurrences` (collection riêng) thay vì `/cash-flow-entries/{id}/occurrences` vì
hai consumer có thật (#77, #80) cần mọi entry trong một khoảng; màn hình lịch kỳ một entry của #73 lọc
theo `sourceEntryId`. Nhánh per-entry không được thêm để không mở hai đường cho cùng một phép tính.

## Constraints

- Chỉ slice `cashflow`, chỉ phụ thuộc `shared`. Không migration: kỳ tính khi đọc, không lưu.
- Không viết unit test hay integration test mới (chỉ đạo Product Owner). Scenario viết trước ở
  [phase 1](./phase-01-test-scenarios.md), chạy thật qua HTTP vào PostgreSQL Docker, ghi evidence.
  `OpenApiContractIT` mở rộng path mới; `ApplicationContextIT` không đổi vì không có bảng mới.
- Không quy đổi tỷ giá, không cộng tổng.

## Non-goals

Báo cáo tháng và composition (#77), forecast ba tháng (#80), lịch trả nợ của liabilities, tỷ giá, sửa
riêng một kỳ hay tách chuỗi (câu hỏi 1–3 của design, thuộc Feature Contract #20), preview một quy tắc chưa
lưu (#73).

## Phases

| Phase | Tên | Trạng thái | Kết quả |
|---|---|---|---|
| 1 | [Danh sách test scenario](./phase-01-test-scenarios.md) | Hoàn tất | 51 case viết trước khi code |
| 2 | [Thuật toán và HTTP](./phase-02-expansion-and-http.md) | Chưa bắt đầu | `CashFlowSchedule`, `OccurrenceWindow`, controller |
| 3 | [Contract và docs](./phase-03-contract-and-docs.md) | Chưa bắt đầu | `openapi.json`, `OpenApiContractIT`, conventions, guide; `clean verify` xanh |
| 4 | [Xác minh thủ công](./phase-04-manual-verification.md) | Chưa bắt đầu | Chạy 51 case, ghi evidence |

## Acceptance Criteria

- [ ] Monthly/quarterly/yearly sinh đúng ngày, kể cả 31 → 28/29/30 và 29/02 → 28/02, và quay lại ngày neo.
- [ ] `effectiveDate`, `endDate`, `from`, `to` đều tính vào; hai khoảng kề nhau không trùng, không hụt kỳ.
- [ ] Entry một lần có đúng một kỳ tại `effectiveDate`.
- [ ] Mỗi kỳ mang `sourceEntryId` đọc lại được, cùng `amount`, `currency`, `entryType`, `category`.
- [ ] Không thấy entry của user khác.
- [ ] Mọi input sai là `validation-failed` có `errors`; 401 ba kiểu token; 405 method khác.
- [ ] Kết quả không đổi khi múi giờ JVM đổi.
- [ ] `openapi.json` regenerate, `clean verify` xanh không flag.

## Verification gap

Không có automated test nào gọi vào route này hay vào thuật toán mở rộng. Đó là chỉ đạo của Product Owner,
cùng tư thế #50, #53, #71, #86 đã ship. Hậu quả cụ thể: một regression trong `CashFlowSchedule` — ví dụ ai
đó đổi phép cộng tháng từ "tính từ ngày neo" sang "cộng dồn từ kỳ trước", làm 31/03 thành 28/03 sau tháng
2 — chỉ bị phát hiện khi chạy lại `docs/manual-test-guide-cash-flow-occurrences.md`. Đây là loại logic mà
một unit test bảng (ngày neo × chu kỳ × năm nhuận) bảo vệ rẻ nhất; nên cân nhắc bổ sung khi chỉ đạo thay
đổi. `OpenApiContractIT` vẫn bắt path bị lộ hoặc thiếu, `ArchitectureRulesTest` vẫn bắt import chéo slice.

<!-- slug: issue-74-recurring-schedule-expansion -->
