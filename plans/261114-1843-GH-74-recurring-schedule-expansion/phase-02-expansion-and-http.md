---
phase: 2
title: "Thuật toán và HTTP"
status: pending
priority: P1
effort: "0.5d"
dependencies: [1]
---

# Phase 2: Thuật toán và HTTP

## Files

| File | Vai trò |
|---|---|
| `cashflow/domain/CashFlowRecurrence.java` | thêm số tháng của mỗi bước (1, 3, 12) |
| `cashflow/domain/CashFlowOccurrence.java` (mới) | record một kỳ |
| `cashflow/domain/CashFlowSchedule.java` (mới) | hàm thuần mở rộng một entry trong `[from, to]` |
| `cashflow/domain/CashFlowEntryRepository.java` | thêm query entry của owner có thể chạm khoảng |
| `cashflow/application/OccurrenceWindow.java` (mới) | parse và kiểm tra `from`/`to`, gom lỗi |
| `cashflow/application/CashFlowOccurrenceService.java` (mới) | đọc entry của owner, mở rộng, sắp xếp |
| `cashflow/api/CashFlowOccurrenceResponse.java`, `CashFlowOccurrenceController.java` (mới) | route `GET /api/v1/cash-flow-occurrences` |

## Thuật toán

Kỳ thứ `n` (bắt đầu từ 0) là `effectiveDate.plusMonths(n × bước)`. Luôn cộng **từ ngày neo**, không cộng
dồn từ kỳ trước: `LocalDate.plusMonths` kẹp ngày về cuối tháng khi ngày neo không tồn tại, và vì phép cộng
xuất phát lại từ ngày neo mỗi lần, tháng 3 sau một tháng 2 bị dời vẫn là ngày 31. Kỳ bị dời khi ngày của
nó khác ngày neo. Dừng khi kỳ vượt `to` hoặc vượt `endDate`. Bỏ qua phần đầu bằng số tháng giữa
`effectiveDate` và `from`, lùi một bước cho an toàn, nên một entry bắt đầu năm 1990 không phải duyệt từ
đầu. Entry một lần là một kỳ tại `effectiveDate` nếu nằm trong khoảng.

Query chỉ lấy entry của owner có `effectiveDate ≤ to` và (`endDate` rỗng hoặc `≥ from`).

## Validation

`from`/`to` nhận dạng chuỗi rồi parse trong `OccurrenceWindow` (không dùng `@RequestParam LocalDate`) vì
binding của Spring trả `malformed-request` cho thiếu tham số và sai định dạng, trong khi contract cần một
`validation-failed` có `errors` theo trường như mọi resource khác. Định dạng chặt `YYYY-MM-DD` (regex bốn
chữ số năm rồi `LocalDate.parse` ở chế độ STRICT), nên `2026-02-29`, `2026-1-5`, năm có dấu `+` và chuỗi có
giờ đều bị từ chối. Thứ tự và độ dài chỉ được xét khi cả hai ngày hợp lệ.

## Validation của phase

Compile và `ArchitectureRulesTest` xanh; case 1–51 chạy ở phase 4.
