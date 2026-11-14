---
phase: 5
title: "Manual verification"
status: pending
priority: P1
effort: "0.4d"
dependencies: [4]
---

# Phase 5: Manual verification

## Files

- Tạo `docs/manual-test-guide-onboarding.md`
- Tạo `plans/reports/manual-verification-261114-1843-GH-95-onboarding-checklist.md`

## Steps

1. PostgreSQL `rikkaus-pg-issue-95` cổng 5495, API cổng 8095 profile `local`.
2. Ba user A/B/C insert trực tiếp, token mint theo `docs/manual-test-guide-goals.md`.
3. Chạy O01–O60 bằng script, lưu status/body thật; case fail rồi sửa ghi là đã fail.
4. Chạy lại trên commit cuối. Dừng API, xóa container.
