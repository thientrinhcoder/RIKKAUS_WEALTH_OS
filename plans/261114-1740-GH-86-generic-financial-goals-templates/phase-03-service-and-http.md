---
phase: 3
title: "Service và HTTP"
status: completed
priority: P1
effort: "1d"
dependencies: [2]
---

# Phase 3: Service và HTTP

## Mục tiêu

Năm route của resource goals và một route templates, validation nêu tên field, mọi status có thể trả
đều được khai báo trong contract.

## Yêu cầu

- `GoalService`: `listFor`, `read`, `create`, `replace`, `delete`; owner là tham số đầu, lấy từ
  `CurrentUser`; `OwnershipGuard.requireOwned` trên mọi truy cập theo id; flush trong transaction để
  row biến mất đồng thời thành `not-found` (kế thừa `AssetService`).
- Quy tắc không thể là annotation, gom lỗi vào một mảng: `currentAllocatedAmount <= targetAmount`;
  `targetDate` sau hôm nay (`Asia/Ho_Chi_Minh`) khi tạo hoặc khi khác giá trị đã lưu. Không báo "vượt
  target" khi một trong hai số đã bị từ chối vì âm.
- Làm sạch `name` như assets (NUL bị từ chối; blank gồm mọi space separator).
- `GoalRequest` dùng chung cho POST và PUT (bộ field giống nhau, PUT là thay thế toàn bộ).
  `@Digits(17,2)` cho ba field tiền, `@Digits(3,2)` + `@DecimalMin(0)` + `@DecimalMax(30)` cho return.
- `GoalResponse`: `id`, `name`, `type`, `currency`, `targetAmount`, `currentAllocatedAmount`,
  `targetDate`, `plannedMonthlyContribution`, `expectedAnnualReturnPercent`, `createdAt`, `updatedAt`.
  Không có field tính toán.
- `GoalController` tại `ApiPaths.V1 + "/goals"`; `GoalTemplateController` tại
  `ApiPaths.V1 + "/goal-templates"` chỉ `GET`, trả `List<GoalTemplateResponse>`.
- Mọi operation khai `bearerAuth`, ví dụ dùng số liệu Việt Nam, không có `localhost`.

## File tạo

- `.../goals/application/GoalService.java`
- `.../goals/api/GoalController.java`, `GoalRequest.java`, `GoalResponse.java`,
  `GoalTemplateController.java`, `GoalTemplateResponse.java`

## Validation

`spotless:check` xanh; sáu path xuất hiện trong `/v3/api-docs`.
