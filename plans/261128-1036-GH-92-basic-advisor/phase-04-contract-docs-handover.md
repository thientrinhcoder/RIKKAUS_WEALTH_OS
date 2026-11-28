---
phase: 4
title: "Contract, tài liệu, bàn giao"
status: completed
priority: P2
effort: "0.25d"
dependencies: [3]
---

# Phase 4: Contract, tài liệu, bàn giao

## Files

- `services/api/openapi/openapi.json` — tái tạo bằng `-Dopenapi.update=true`; chỉ thêm, không xoá dòng nào.
- `apps/mobile/src/api/generated/{openapi.ts,contract-examples.ts}` — `npm run api:generate`; `api:check` và
  `typecheck` xanh.
- `docs/api-contract-conventions.md` — mục "Basic Advisor report — implemented" (kèm sổ đăng ký mã quy tắc,
  trả lời câu hỏi mở 1 của design #90); sửa hai câu cũ nhắc #92 là chưa có ở mục goal projection và
  financial-health.
- `pr-body.md` trong scratchpad — tiêu đề, contract, ghi chú ArchUnit cho reviewer, số kịch bản và phép
  kiểm, thông báo cho sibling #91, câu hỏi PO. Coordinator mở PR.

## Steps

1. Tái tạo contract và client, chạy `api:check`.
2. Viết mục tài liệu; kiểm liên kết neo tới các mục cùng file.
3. Tự rà diff: không token, JWT, mật khẩu; không so sánh `int` với `long` (CodeQL
   `java/comparison-with-wider-type`); không import `..domain..` của slice khác.
4. Commit theo conventional commit, push nhánh, không force-push.
