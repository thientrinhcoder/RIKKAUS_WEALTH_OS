---
phase: 4
title: "Chạy scenario thật, ghi evidence"
status: completed
priority: P1
effort: "0.4d"
dependencies: [3]
---

# Phase 4: Chạy scenario thật, ghi evidence

- PostgreSQL `rikkaus-pg-issue-101` (127.0.0.1:5401, `postgres:18.6-alpine3.24`), API port 8101,
  profile `local`.
- X01: chạy jar `main` `abadb96` trước trên database đã seed, lưu body bốn endpoint cho A và B; dừng,
  chạy jar nhánh này trên cùng database, so `sha256`.
- Token A/B/hết hạn/giả mạo sinh lúc chạy theo `docs/manual-test-guide-assets.md`, chỉ giữ trong biến
  môi trường; report không chứa token.
- Ghi status và body thật từng case vào
  `plans/reports/manual-verification-261122-1538-GH-101-search-basic-filters.md`, commit đang chạy,
  số pass/fail. Case fail rồi sửa ghi là đã fail.
- Dọn: dừng API, `docker rm -f rikkaus-pg-issue-101`.
