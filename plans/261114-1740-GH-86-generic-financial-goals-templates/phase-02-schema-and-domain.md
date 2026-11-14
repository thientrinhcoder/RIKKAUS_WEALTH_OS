---
phase: 2
title: "Schema và domain"
status: completed
priority: P1
effort: "0.5d"
dependencies: [1]
---

# Phase 2: Schema và domain

## Mục tiêu

Một bảng mà ràng buộc của nó giữ đúng quy tắc của design mà không tin vào application code, và từ vựng
Java cho phần còn lại của slice.

## Yêu cầu

- `wealth.goals`: `id`, `user_id` (FK `wealth.users`, cascade), `name`, `type`, `currency` (CHECK =
  `'VND'`), `target_amount NUMERIC(19,2) > 0`, `current_allocated_amount NUMERIC(19,2) >= 0` và
  `<= target_amount`, `target_date DATE NOT NULL`, `planned_monthly_contribution NUMERIC(19,2)` nullable
  `>= 0`, `expected_annual_return_percent NUMERIC(5,2)` nullable trong `[0, 30]`, `created_at`,
  `updated_at`.
- Không CHECK về `target_date` so với ngày hiện tại: `now()` trong CHECK không ổn định và sẽ chặn mọi
  UPDATE của mục tiêu đã quá hạn; quy tắc này sống ở service.
- Bảy type là CHECK, như `assets_category_is_known`.
- Index `(user_id, target_date)` theo thứ tự list.
- `Goal implements UserOwned`, static factory `create`, một mutator `replace` cho toàn bộ field người
  dùng sửa được; owner là `UUID` thuần.
- `GoalType` enum bảy giá trị, mỗi giá trị mang luôn mặc định của template (`defaultExpectedAnnualReturnPercent`,
  `defaultHorizonMonths`, `amountPrefill` kiểu `GoalAmountPrefill`) — không có class `GoalTemplate` riêng vì
  template chính là type; `GoalRepository` hẹp như
  `AssetRepository` (list theo owner, `findById`, `save`, `delete`, `flush`).

## File tạo

- `services/api/src/main/resources/db/migration/V9__goals.sql`
- `services/api/src/main/java/com/rikkaus/wealth/goals/domain/Goal.java`
- `.../goals/domain/GoalType.java`, `GoalAmountPrefill.java`, `GoalFields.java` (record các field người
  dùng nhập), `GoalRepository.java`

## Validation

Migration apply sạch trên container mới; D03–D10 bị từ chối bởi bảng.

## Rủi ro và rollback

Thêm một bảng. Rollback là xóa `wealth.goals` và file migration; chưa ai tham chiếu.
