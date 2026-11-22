---
phase: 2
title: "Slice snapshot và HTTP"
status: completed
priority: P1
effort: "0.75d"
dependencies: [1]
---

# Phase 2: Slice snapshot và HTTP

## Mục tiêu

Hiện thực các quy tắc ở [plan.md](./plan.md) để mọi case ở [phase 1](./phase-01-test-scenarios.md) pass.

## File

| File | Vai trò |
|---|---|
| `services/api/src/main/java/com/rikkaus/wealth/snapshot/application/SnapshotSourceReader.java` | `JdbcClient`, ba truy vấn owner-scoped trên `wealth.assets`, `wealth.liabilities`, `wealth.user_settings` |
| `services/api/src/main/java/com/rikkaus/wealth/snapshot/application/WealthSnapshot.java` | mô hình kết quả (record lồng nhau), không annotation HTTP |
| `services/api/src/main/java/com/rikkaus/wealth/snapshot/application/WealthSnapshotService.java` | quy đổi, aggregate, xếp hạng, cửa sổ nghĩa vụ, ngày nguồn |
| `services/api/src/main/java/com/rikkaus/wealth/snapshot/api/WealthSnapshotController.java` | `GET /api/v1/wealth-snapshot`, ví dụ OpenAPI |
| `services/api/src/main/java/com/rikkaus/wealth/snapshot/api/WealthSnapshotResponse.java` | DTO có `@Schema`, ánh xạ từ `WealthSnapshot` |

## Bước

1. Reader đọc đúng cột cần; mọi câu `WHERE user_id = :ownerId`.
2. Service: một lần đọc đồng hồ (`TimeConventions.today`, `Asia/Ho_Chi_Minh`); quy đổi từng dòng qua
   `ConversionTrace`, làm tròn đồng rồi viết hai chữ số; aggregate partial; `Collator` `vi-VN` tạo mỗi
   request (không chia sẻ giữa thread).
3. Controller không tham số, trả `WealthSnapshotResponse.of(...)`.

## Kiểm tra

`clean verify` (sau phase 3 cập nhật contract) và các case phase 1 ở phase 4.

## Rủi ro

`Collator` của JDK cần dữ liệu locale `vi`; nếu thiếu, collator gốc vẫn xếp `Ô` trước `V`, và `id` là khóa
phụ cuối cùng nên thứ tự luôn xác định.
