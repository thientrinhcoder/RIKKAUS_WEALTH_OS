---
phase: 2
title: "Schema và domain"
status: completed
priority: P1
effort: "1d"
dependencies: [1]
---

# Phase 2: Schema và domain

## Mục tiêu

Một bảng giữ được quy tắc của Backbone mà không cần tin vào code ứng dụng, và bộ từ vựng Java phần
còn lại của slice viết dựa trên.

## Yêu cầu

- Bảng `wealth.cash_flow_entries`: `id`, `user_id` FK `wealth.users` cascade, `entry_type`, `category`,
  `amount NUMERIC(19, 2)`, `currency`, `timing`, `recurrence`, `effective_date`, `end_date`,
  `linked_asset_id` FK `wealth.assets(id) ON DELETE SET NULL`, `notes`, `created_at`, `updated_at`.
- Check constraint cho: 2 entry type, 16 category **gắn với đúng entry type**, `amount > 0`, 2 currency,
  2 timing, 3 recurrence, cặp timing/recurrence (`ONE_TIME` thì `recurrence` và `end_date` đều NULL;
  `RECURRING` thì `recurrence` NOT NULL), `end_date >= effective_date`, `notes` ≤ 2000.
- Index `(user_id, effective_date DESC)` theo thứ tự liệt kê.
- Owner là `UUID` thuần, không association; enum lưu bằng tên.

## Files

- `services/api/src/main/resources/db/migration/V7__cash_flow_entries.sql`
- `services/api/src/main/java/com/rikkaus/wealth/cashflow/domain/CashFlowEntry.java`
- `.../domain/CashFlowEntryType.java`, `CashFlowCategory.java` (mỗi hằng số mang entry type của nó),
  `CashFlowTiming.java`, `CashFlowRecurrence.java`
- `.../domain/CashFlowEntryDetails.java` (record mọi trường người dùng nhập, dùng chung cho tạo và sửa)
- `.../domain/CashFlowEntryRepository.java` (list theo owner, `findById`, `save`, `delete`, `flush`, và
  native query `assetBelongsTo`)

Currency dùng `com.rikkaus.wealth.shared.money.Currency` thay vì enum riêng: đã có trong `shared`, đúng
hai giá trị, serialize bằng tên.

## Validation

- `V7__cash_flow_entries.sql` apply vào container sạch mà không cần bước tay; `ddl-auto: validate` chấp
  nhận entity.
- Các probe case 85–92 bị bảng từ chối độc lập với API.

## Risk và rollback

Migration chỉ thêm một bảng. Rollback là drop `wealth.cash_flow_entries` và xóa file migration.
