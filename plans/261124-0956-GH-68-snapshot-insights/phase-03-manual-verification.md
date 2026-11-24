---
phase: 3
title: "Chạy kịch bản trên API thật"
status: completed
priority: P1
effort: "0.25d"
dependencies: [2]
---

# Phase 3: Chạy kịch bản trên API thật

Script trong [`scripts/`](./scripts/): `setup.sh` tạo tám user qua dev sign-in và mọi bản ghi qua API;
`cases.sh main | custom | restored | resolved` chạy các kịch bản của phase 1; `recon.jq` đối soát mỗi body
insights với `GET /wealth-snapshot` của cùng user.

Thứ tự chạy: API cấu hình mặc định → `setup.sh` → `cases.sh main` (I01–I22) → khởi động lại với sáu biến
`RIKKAUS_SNAPSHOT_INSIGHTS_*` → `cases.sh custom` (I23) → ba lần khởi động với cấu hình vô lý (I25–I27) →
khởi động lại mặc định → `cases.sh restored` (I24) → `cases.sh resolved` (I28).

Kết quả: 28/28 kịch bản, 46/46 phép kiểm PASS, ghi ở
[`manual-verification-261124-1010-GH-68-snapshot-insights.md`](../reports/manual-verification-261124-1010-GH-68-snapshot-insights.md).
