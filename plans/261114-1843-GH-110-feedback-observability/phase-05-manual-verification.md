---
phase: 5
title: "Manual verification"
status: completed
priority: P1
effort: "0.5d"
dependencies: [4]
---

# Phase 5: Manual verification

## Yêu cầu

- PostgreSQL dùng một lần `rikkaus-pg-issue-110` trên `127.0.0.1:5410`; API `--server.port=8110`,
  `DB_URL=jdbc:postgresql://localhost:5410/rikkaus`. Không chạm volume `rikkaus-postgres-data`.
- Lần 1: profile `local` (token mint bằng secret dev). Lần 2: không profile, `RIKKAUS_JWT_SECRET` qua env
  (token mint lại bằng secret đó). stdout và stderr của mỗi lần ghi vào file riêng.
- Chạy F01–F36, P01–P08, O01–O13, J01–J07 theo thứ tự trên commit cuối. Ghi status và body thật, trích
  đoạn log thật (đã grep sạch mồi).
- Case fail rồi fix: ghi là đã fail, ghi thay đổi, chạy lại toàn bộ.
- Dừng API, `docker rm -f rikkaus-pg-issue-110` khi xong.

## File tạo

- `docs/manual-test-guide-feedback-observability.md`
- `plans/reports/manual-verification-261114-1843-GH-110-feedback-observability.md`
