---
phase: 4
title: "Xác minh thủ công"
status: pending
priority: P1
effort: "0.5d"
dependencies: [3]
---

# Phase 4: Xác minh thủ công

## Bước

1. PostgreSQL `rikkaus-pg-issue-65` cổng `5465`, API cổng `8065` profile `local`.
2. Chạy `setup.sh` rồi `cases.sh` của guide trên commit cuối; giữ body thật.
3. Ghi `plans/reports/manual-verification-261122-1842-GH-65-wealth-snapshot-dashboard.md`: commit, từng
   case với status/body rút gọn, số pass/fail; case fail rồi sửa ghi rõ đã fail.
4. Dừng API, xóa container.
