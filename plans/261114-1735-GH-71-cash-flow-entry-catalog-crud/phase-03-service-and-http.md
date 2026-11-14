---
phase: 3
title: "Service và HTTP resource"
status: pending
priority: P1
effort: "1d"
dependencies: [2]
---

# Phase 3: Service và HTTP resource

## Mục tiêu

Năm route dưới `/api/v1/cash-flow-entries`, mọi quy tắc chéo trường ở một chỗ, ownership qua
`OwnershipGuard`, và kiểm tra liên kết tài sản không import class của slice `assets`.

## Yêu cầu

- `CashFlowEntryService`: `listFor`, `read`, `create`, `replace`, `delete`, mọi method nhận owner làm
  tham số đầu, caller lấy từ `CurrentUser`.
- Quy tắc chéo trường gom vào một `errors` array (case 41): category ↔ entry type, recurrence ↔ timing,
  endDate ↔ timing, endDate ↔ effectiveDate, linkedAssetId ↔ owner.
- `notes` làm sạch như `AssetService.cleaned`: NUL bị từ chối, khoảng trắng Unicode được strip, chuỗi
  trống thành `null`.
- Flush trong transaction để xóa đồng thời trả `not-found` thay vì 500 (case 81), như `AssetService`.
- `CashFlowEntryController` với `@Operation`/`@ApiResponse` đủ mọi status mỗi route có thể trả, ví dụ
  bằng dữ liệu Việt Nam, `Location` header khai báo trên `POST`.
- Một request record `CashFlowEntryRequest` dùng cho cả `POST` và `PUT`; `CashFlowEntryResponse` không
  có owner, không có số VND quy đổi.

## Files

- `services/api/src/main/java/com/rikkaus/wealth/cashflow/application/CashFlowEntryService.java`
- `.../api/CashFlowEntryController.java`, `CashFlowEntryRequest.java`, `CashFlowEntryResponse.java`

## Validation

Ứng dụng khởi động với profile `local`; `GET /v3/api-docs` liệt kê hai path mới.
