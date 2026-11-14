---
phase: 2
title: "Slice feedback"
status: completed
priority: P1
effort: "0.5d"
dependencies: [1]
---

# Phase 2: Slice feedback

## Mục tiêu

`POST`/`GET /api/v1/feedback` đúng contract ở `plan.md`, dữ liệu có CHECK ở PostgreSQL.

## File tạo

- `services/api/src/main/resources/db/migration/V12__feedback_entries.sql`
- `services/api/src/main/java/com/rikkaus/wealth/feedback/domain/{FeedbackEntry,FeedbackCategory,FeedbackEntryRepository}.java`
- `services/api/src/main/java/com/rikkaus/wealth/feedback/application/FeedbackService.java`
- `services/api/src/main/java/com/rikkaus/wealth/feedback/api/{FeedbackController,FeedbackRequest,FeedbackResponse}.java`

## Bước

1. Migration: bảng `wealth.feedback_entries` với FK `wealth.users ON DELETE CASCADE`, CHECK cho screen
   (pattern + độ dài), category, message (không blank, ≤ 2000), index `(user_id, created_at DESC)`.
2. Entity bất biến (không setter, `create` factory), `UserOwned`.
3. Service: làm sạch message (strip, blank theo Unicode, từ chối NUL), báo lỗi theo `errors` cùng lúc.
4. Controller: annotation OpenAPI với ví dụ tiếng Việt, 201 không `Location`.

## Validation

Context khởi động (Hibernate `validate` khớp migration); phase 5 chạy F01–F36, P01–P08.

## Rủi ro

Đánh số migration trùng V10/V11 của #74/#95 — đã chọn V12; coordinator đánh số lại khi merge.
