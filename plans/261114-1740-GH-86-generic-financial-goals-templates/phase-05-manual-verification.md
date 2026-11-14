---
phase: 5
title: "Manual verification"
status: pending
priority: P1
effort: "0.5d"
dependencies: [4]
---

# Phase 5: Manual verification

## Mục tiêu

Thay cho automated test mà task này không viết: một guide ai cũng chạy lại được và một bản ghi kết quả
thật của lần chạy này.

## Yêu cầu

- PostgreSQL dùng một lần `rikkaus-pg-issue-86` trên `127.0.0.1:5486`; API `--server.port=8086`,
  profile `local`, `DB_URL=jdbc:postgresql://localhost:5486/rikkaus`. Không chạm volume
  `rikkaus-postgres-data`.
- Hai user insert thẳng vào `wealth.users`; token HS256 mint bằng secret dev của profile `local` (A, B,
  hết hạn, sai chữ ký), như `docs/manual-test-guide-assets.md`.
- Chạy đúng 70 case G01–G70 và 11 case D01–D11 của phase 1, theo thứ tự, trên commit cuối cùng của
  slice. Mỗi case ghi status thật và phần body có ý nghĩa.
- Case fail rồi fix: ghi là đã fail, ghi cái gì đổi, chạy lại toàn bộ danh sách sau lần sửa cuối.
- Dừng API, `docker rm -f rikkaus-pg-issue-86` khi xong.

## File tạo

- `docs/manual-test-guide-goals.md`
- `plans/reports/manual-verification-261114-1740-GH-86-generic-financial-goals-templates.md`

## Validation

Report có verdict cho mọi case trong guide; số pass/fail khớp; report nêu commit đã chạy.
