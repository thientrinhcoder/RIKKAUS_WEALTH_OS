---
phase: 2
title: "Package insights, cấu hình, HTTP"
status: completed
priority: P1
effort: "0.5d"
dependencies: [1]
---

# Phase 2: Package insights, cấu hình, HTTP

## File

| File | Thay đổi |
|---|---|
| `snapshot/application/WealthSnapshotService.java` | Tách vòng lặp hạn nợ thành `dues(...)` và thêm `public duesUntil(snapshot, until)`. Danh sách 30 ngày của snapshot vẫn gọi cùng hàm, nên `GET /wealth-snapshot` không đổi (contract không đổi một byte). |
| `snapshot/insights/application/SnapshotInsightProperties.java` | `@ConfigurationProperties("rikkaus.snapshot.insights")`, sáu giá trị, không default, fail-fast nêu tên property. |
| `snapshot/insights/application/SnapshotInsightConfiguration.java` | `@EnableConfigurationProperties`, theo mẫu `GoalProjectionConfiguration`. |
| `snapshot/insights/application/SnapshotInsights.java` | Model: `RuleId`, `Status`, `UnavailableReason`, `Unit`, `Comparison`, `DateKind`, `Observed`, `Threshold`, `Evidence`. |
| `snapshot/insights/application/SnapshotInsightService.java` | Đánh giá sáu rule trên một `WealthSnapshot`; không tự quy đổi tiền. |
| `snapshot/insights/api/SnapshotInsightsController.java` | `GET /api/v1/wealth-snapshot/insights`, `operationId` tường minh `readWealthSnapshotInsights`. |
| `snapshot/insights/api/SnapshotInsightsResponse.java` | Record response có `@Schema`. |
| `snapshot/insights/api/SnapshotInsightsExamples.java` | Ba body thật của lần chạy (A, C, N) làm ví dụ OpenAPI. |
| `src/main/resources/application.yml` | Khối `rikkaus.snapshot.insights`. |
| `shared/openapi/OpenApiContractIT.java` (test) | Thêm path mới vào inventory, một dòng. |

## Ghi chú

- So sánh ngày dùng `long` cả hai vế (CodeQL `java/comparison-with-wider-type`): `long window`, `long maxAge`.
- `Math.toIntExact` cho số ngày trong evidence: không bao giờ tràn với ngày hợp lệ.
- Không migration, không bảng mới. `ArchitectureRulesTest` vẫn pass: `snapshot.insights` chỉ phụ thuộc `snapshot` và `shared`.
