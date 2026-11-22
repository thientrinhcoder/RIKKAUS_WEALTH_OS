---
phase: 4
title: "Chạy scenario thật, ghi evidence"
status: pending
priority: P1
effort: "0.25d"
dependencies: [3]
---

# Phase 4: Chạy scenario thật, ghi evidence

## Môi trường

- `docker run -d --name rikkaus-pg-issue-104 -p 127.0.0.1:5404:5432 ... postgres:18.6-alpine3.24`
- API từ jar của commit đang kiểm, `--spring.profiles.active=local --server.port=8104`,
  `DB_URL=jdbc:postgresql://localhost:5404/rikkaus`.
- Hai user A/B chèn trực tiếp, token mint theo `docs/manual-test-guide-assets.md` bước 4.

## Steps

1. Chạy script scenario (lưu ở scratchpad, không commit) theo thứ tự I → O → U → M → E → G → C → K.
2. Ghi status, body thật và kết quả psql từng case vào
   `plans/reports/manual-verification-261122-1842-GH-104-safe-edit-delete.md`, kèm commit và số pass/fail.
3. Dừng/xóa container, tắt API.
