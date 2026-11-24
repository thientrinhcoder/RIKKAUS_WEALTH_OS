# Phase 2 — Script fixture và hướng dẫn

Status: completed.

## Files

- `services/api/fixtures/lib.sh` — sign-in qua `POST /auth/dev-sign-in`, `api` dừng script ở mọi status
  ngoài 2xx, `day N` theo `Asia/Ho_Chi_Minh` (GNU và BSD `date`), `delete_all_records` (`confirm=true`).
- `services/api/fixtures/reset.sh`, `load-baseline.sh`, `load-long-lists.sh`.
- `docs/local-fixtures-guide.md`.

## Quyết định

- Mỗi script load bắt đầu bằng reset của chính tài khoản đó, nên chạy lại cho cùng dữ liệu (id mới).
- Baseline và danh sách dài dùng hai tài khoản khác nhau (`an.nguyen@example.com`, mặc định của nút
  **Đăng nhập phát triển** trong app; `long.list@example.com`), để frontend đổi bộ dữ liệu bằng cách đổi email.
- Mục tiêu `AT_RISK` không hard-code khoản góp: script đọc `requiredMonthlyContribution` từ
  `GET /goal-projections/{id}` rồi đặt 90 %, nên trạng thái đúng vào bất kỳ ngày nào.
- Feedback không xoá được: `load-long-lists.sh` chỉ bổ sung cho đủ 20.
- Token chỉ ở file header `0600` trong thư mục tạm `0700`, xoá bằng `trap EXIT`; truyền bằng `-H @file`
  nên không lộ trên `ps`.

## Validation

F01–F14 trong `phase-01-test-scenarios.md`; kết quả ở evidence.
