# Phase 4 — Xác minh thủ công

Status: Pending

## Files

- `docs/manual-test-guide-goal-projection.md`: cách dựng môi trường và chạy P01–P44.
- `plans/reports/manual-verification-261122-1842-GH-89-goal-projection-status.md`: status và body thật từng case,
  commit, số pass/fail; case fail rồi sửa ghi là đã fail.

## Môi trường

PostgreSQL `rikkaus-pg-issue-89` cổng `127.0.0.1:5489`; API `--spring.profiles.active=local --server.port=8089`.
Dừng và xóa container, tắt API khi xong.

## Steps

1. Package jar, chạy container và API; chèn hai user; mint token A, B, hết hạn, ký sai khóa.
2. Tạo dữ liệu phase 1; ba goal quá hạn đổi ngày bằng SQL.
3. Chạy P01–P41, P44 trên cùng một lần khởi động; P42, P43 bằng khởi động lại với property khác.
4. Ghi report; điền "Verification gap" và câu hỏi PO vào `plan.md`.
5. Commit, push, PR (hoặc `pr-body.md` ở scratchpad nếu `gh pr create` bị chặn).
