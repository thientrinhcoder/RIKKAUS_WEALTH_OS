---
phase: 2
title: "Báo cáo và HTTP"
status: in-progress
priority: P1
effort: "0.5d"
dependencies: [1]
---

# Phase 2: Báo cáo và HTTP

## Files

Tất cả mới, dưới `services/api/src/main/java/com/rikkaus/wealth/cashflow/reporting/`. Không file hiện có
nào của `cashflow` bị sửa.

| File | Vai trò |
|---|---|
| `application/ReportMonth.java` | parse và kiểm tra `month`, trả ngày đầu và ngày cuối tháng |
| `application/DeclaredExchangeRateRepository.java` | native query tỷ giá đang khai trên `wealth.user_settings` |
| `application/MonthlyCashFlowReport.java` | kết quả tổng hợp, không phụ thuộc HTTP |
| `application/MonthlyCashFlowReportService.java` | đọc kỳ qua `CashFlowOccurrenceService`, quy đổi, cộng, chia tỷ trọng |
| `api/MonthlyCashFlowReportResponse.java` | hình dạng JSON và schema OpenAPI |
| `api/MonthlyCashFlowReportController.java` | `GET /api/v1/cash-flow-reports/monthly` |

## Thuật toán

1. `ReportMonth.of(text)` → `[ngày 1, ngày cuối]`; `OccurrenceWindow` của #74 nhận đúng khoảng đó.
2. Mỗi kỳ thành một dòng: VND giữ nguyên `amount` của entry; USD có tỷ giá đi qua
   `ConversionTrace.convert`, làm tròn một lần về đồng; USD không tỷ giá có `amount` `null`.
3. Mọi tổng là `Σ amount` của các dòng có `amount`; composition nhóm theo category; thụ động nhóm theo
   `linkedAssetId`; nhóm chưa gán là bốn category thụ động không link.
4. Tỷ trọng composition: phần mười phần trăm nguyên của mỗi dòng là `⌊amount × 1000 / tổng⌋`, phần thiếu so
   với `1000` chia cho các dòng có phần dư lớn nhất, hòa thì theo thứ tự dòng. Phần dư so trực tiếp bằng
   `BigDecimal` (cùng mẫu số), không làm tròn trung gian.

## Validation

`clean verify` xanh (biên dịch, `ArchitectureRulesTest`); case của phase 1 chạy ở phase 4.
