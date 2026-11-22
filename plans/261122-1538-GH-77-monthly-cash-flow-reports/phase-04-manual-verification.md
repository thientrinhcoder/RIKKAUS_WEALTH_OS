---
phase: 4
title: "Xác minh thủ công"
status: pending
priority: P1
effort: "0.5d"
dependencies: [3]
---

# Phase 4: Xác minh thủ công

PostgreSQL `rikkaus-pg-issue-77` ở `127.0.0.1:5477`, API ở `8077` profile `local`. Chạy 40 case của phase 1
qua HTTP, ghi status và body thật vào
`plans/reports/manual-verification-261122-1538-GH-77-monthly-cash-flow-reports.md` kèm commit đang chạy và
số pass/fail. Case fail rồi fix ghi là đã fail. Dừng API, xóa container khi xong.
