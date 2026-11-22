# Phase 4 — Xác minh thủ công

Status: Pending · Depends on: phases 2–3

## Steps

1. PostgreSQL `rikkaus-pg-issue-80` trên `127.0.0.1:5480`, API `--server.port=8180`.
2. Chèn user A, B, C; mint token lúc chạy (secret đọc từ profile `local`, không chép vào docs).
3. Tạo dữ liệu phase 1 qua API, chạy 55 case bằng script so kỳ vọng, ghi status và body thật.
4. Viết `docs/manual-test-guide-cash-flow-forecast.md` và
   `plans/reports/manual-verification-261122-1544-GH-80-three-month-forecast.md`.
5. Dừng API, xóa container.

## Validation

Mọi case đạt trên commit cuối; case fail rồi sửa được ghi lại là đã fail.
