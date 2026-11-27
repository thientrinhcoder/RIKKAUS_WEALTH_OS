---
phase: 2
title: "Package cashflow/health và route HTTP"
status: completed
priority: P1
effort: "0.5d"
dependencies: [1]
---

# Phase 2: Package `cashflow/health` và route HTTP

## Tệp

| Tệp | Vai trò |
|---|---|
| `cashflow/health/application/FinancialHealthProperties.java` | `@ConfigurationProperties("rikkaus.cashflow.health")`, không default, fail-fast (dương, số chữ số thập phân, share ≤ 1, hai biên một dải không chéo, `0..3650` ngày, danh mục thiết yếu chỉ EXPENSE, không trùng, không rỗng) |
| `cashflow/health/application/FinancialHealthConfiguration.java` | `@EnableConfigurationProperties`, như `SnapshotInsightConfiguration` |
| `cashflow/health/application/FinancialHealth.java` | record kết quả và mọi hằng (`IndicatorId`, `RuleId`, `Availability`, `Reason`, `NotEvaluatedReason`, `BandNotDeterminedReason`, `Band`, `Unit`, `Formula`, `Status`, `Comparison`, `SourceType`, `Role`) |
| `cashflow/health/application/FinancialHealthService.java` | đọc report tháng, forecast, tài sản thanh khoản, công nợ, entry định kỳ thiết yếu còn hiệu lực trong một transaction chỉ đọc; tính năm chỉ số, xét sáu rule |
| `cashflow/health/api/FinancialHealthController.java`, `FinancialHealthResponse.java`, `FinancialHealthExamples.java` | `GET /api/v1/financial-health?month=`, schema OpenAPI, ba ví dụ là body thật của lần chạy phase 3 |
| `cashflow/forecast/application/ForecastSourceReader.java` | thành `public`; thêm `assetsIn(owner, categories)` (có `ORDER BY`), `ScheduledLiability.outstandingBalance`; `CashAccount` đổi tên `ValuedAsset` |
| `cashflow/forecast/application/LiabilityPaymentSchedule.java` | thành `public` để rule thanh khoản dùng đúng lịch trả của forecast |
| `src/main/resources/application.yml` | khối `rikkaus.cashflow.health` |
| `src/test/.../OpenApiContractIT.java` | thêm `/api/v1/financial-health` vào tập path đã khai báo (không thêm test mới) |

## Quyết định

- Tái dùng `MonthlyCashFlowReportService.reportFor` và `CashFlowForecastService.forecastFor(owner, null)` nguyên
  vẹn, nên tử/mẫu số luôn trùng report và forecast; `asOfDate` lấy từ horizon của forecast để mặc định tháng và
  cửa sổ nghĩa vụ dùng cùng một "hôm nay".
- Quy đổi USD giống report: từng bản ghi, `ConversionTrace` làm tròn đồng, viết ở hai chữ số thập phân.
- Mẫu số dự phòng: entry `RECURRING` + `EXPENSE` + danh mục thiết yếu, `findActiveBetween(tháng)` (bắt đầu
  không sau ngày cuối tháng, chưa kết thúc trước ngày đầu tháng), quy tháng rồi làm tròn từng dòng trước khi
  cộng.
- Rule thanh khoản: liability đáo hạn trong cửa sổ góp nguyên `outstandingBalance` và không cộng thêm kỳ trả của
  nó; liability khác góp từng kỳ trả theo `LiabilityPaymentSchedule` trong `[hôm nay, hôm nay + due-window-days]`.
- `PARTIAL` không có `band` (`PARTIAL_INPUTS`); rule trên chỉ số không `COMPLETE` là `NOT_EVALUATED`.

## Kiểm chứng

Spotless, compile; route chạy được trên API thật (phase 3).
