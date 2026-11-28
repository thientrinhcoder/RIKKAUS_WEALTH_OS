---
phase: 2
title: "Slice advisor và route HTTP"
status: completed
priority: P1
effort: "0.5d"
dependencies: [1]
---

# Phase 2: Slice advisor và route HTTP

## Files

| File | Vai trò |
|---|---|
| `services/api/src/main/java/com/rikkaus/wealth/advisor/application/AdvisorReport.java` | record báo cáo, finding, suggestion và các enum hằng số (nguồn, trạng thái, họ hành động, `actionId`, `noteId`, loại bản ghi, đơn vị, phép so sánh) |
| `services/api/src/main/java/com/rikkaus/wealth/advisor/application/AdvisorReportService.java` | đọc ba service upstream trong một transaction read-only; chép quan sát và ngưỡng; phân nhóm; tier và họ; gộp theo thành phần liên thông; xếp hạng; dấu ưu tiên |
| `services/api/src/main/java/com/rikkaus/wealth/advisor/api/AdvisorReportController.java` | `GET /api/v1/advisor-report`, `operationId readAdvisorReport`, `month` truyền nguyên văn cho financial-health |
| `services/api/src/main/java/com/rikkaus/wealth/advisor/api/AdvisorReportResponse.java` | record response có `@Schema` theo kiểu `FinancialHealthResponse` |
| `services/api/src/main/java/com/rikkaus/wealth/advisor/api/AdvisorReportExamples.java` | ba body thật của lần chạy phase 3 |
| `services/api/src/test/java/com/rikkaus/wealth/ArchitectureRulesTest.java` | tinh chỉnh `featureSlicesDoNotDependOnEachOther`: bỏ qua phụ thuộc có đích trong `..application..` |
| `services/api/src/test/java/com/rikkaus/wealth/shared/openapi/OpenApiContractIT.java` | thêm `/api/v1/advisor-report` vào danh sách path |

## Steps

1. Record và enum hằng số tiếng Anh; không có chuỗi tiếng Việt nào do API sinh ra.
2. Service: health trước (kiểm `month`), rồi snapshot insights, rồi goal projections; không có
   `@ConfigurationProperties`, `PRIORITY_MARKER_COUNT = 3` là hằng số code.
3. Controller và response; ví dụ tạm `{}` cho tới khi phase 3 có body thật.
4. ArchUnit: thêm `ignoreDependency(alwaysTrue(), resideInAPackage("com.rikkaus.wealth..application.."))`,
   cập nhật `because` và Javadoc. Kiểm ngược bằng một lớp thăm dò tạm tham chiếu
   `cashflow.domain.CashFlowCategory`: rule đỏ đúng chỗ, đã xoá lớp thăm dò.

## Validation

`./mvnw spotless:apply`, `ArchitectureRulesTest` 4/4, `./mvnw clean verify`.

## Risk

Một rule upstream mới mà chưa có tier trong `PLACEMENTS` sẽ làm route trả 500 khi rule đó `ATTENTION`
(`Objects.requireNonNull` nêu tên rule), thay vì âm thầm bỏ qua một nhận định đã kích hoạt.
