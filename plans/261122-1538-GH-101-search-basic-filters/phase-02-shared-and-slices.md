---
phase: 2
title: "shared/search và bốn slice"
status: completed
priority: P1
effort: "0.5d"
dependencies: [1]
---

# Phase 2: `shared/search` và bốn slice

## Files

Tạo trong `services/api/src/main/java/com/rikkaus/wealth/`:

- `shared/search/SearchText.java`: đọc `q` (strip, rỗng = vắng, ≤ 120 code point, cấm U+0000), escape
  `\ % _`, dựng predicate `lower(translate(col, FROM, TO)) LIKE lower(translate(:pattern, FROM, TO))`
  OR qua các cột. Bảng `FROM`/`TO` sinh lúc nạp class từ 12 nguyên âm gốc × 5 dấu thanh, cộng `đ`,
  chữ hoa tương ứng và 8 dấu tổ hợp (bị xóa).
- `shared/search/QueryParameters.java`: parse enum nhiều giá trị, ngày `YYYY-MM-DD` chặt, `sort`,
  gom lỗi và ném một `validation-failed`.
- `shared/search/SortKey.java`: interface `token()` cho enum sort của slice.
- Mỗi slice, trong `domain/`: `<X>Filter` (record + factory `of(...)` từ chuỗi thô), `<X>Sort` (enum),
  `<X>Search` (fragment interface) và `<X>SearchImpl` (Criteria API, `user_id = :owner` luôn là
  predicate đầu tiên).

Sửa: `<X>Repository` thêm `extends <X>Search`; `<X>Service.listFor(owner, filter)`; controller list bind
tham số thô (`String`, `List<String>`) với `@Parameter` có mô tả và ví dụ, cập nhật javadoc "takes no
query parameter". Method repository cũ chỉ phục vụ list của cả bốn slice bị xóa vì không còn caller.
`CashFlowEntryRepository.findByUserIdOrderByEffectiveDateDescCreatedAtDesc` được giữ ở commit đầu vì #77,
#80 đang làm cùng slice; sau khi merge `main` (`becca58`) xác nhận không slice nào gọi nên đã xóa.

## Validation

`clean verify` xanh (ArchUnit: slice chỉ phụ thuộc `shared`, controller không bind `userId`).

## Risk / rollback

Đổi đường truy vấn list cho cả bốn endpoint: X01 so byte với jar `main`. Rollback: revert commit.
