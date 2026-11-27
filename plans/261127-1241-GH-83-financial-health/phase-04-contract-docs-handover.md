---
phase: 4
title: "Contract, tài liệu, bàn giao"
status: completed
priority: P1
effort: "0.25d"
dependencies: [3]
---

# Phase 4: Contract, tài liệu, bàn giao

- `services/api/openapi/openapi.json` tái tạo bằng `-Dopenapi.update=true`; `clean verify` không cờ thấy không
  lệch.
- `apps/mobile`: `npm run api:generate` cập nhật `src/api/generated/openapi.ts` và `contract-examples.ts`;
  `npm run api:check` và `npm run typecheck` xanh.
- `docs/api-contract-conventions.md`: mục mới "Financial-health indicators and insights — implemented" ở cuối
  file; sửa các câu đã lỗi thời nhắc #83 trong "Cash-flow monthly report / Not here", "Cash-flow forecast / Not
  here", "Wealth snapshot", "Wealth snapshot insights / Not here" và "What is not here" (gạch đầu dòng domain
  resources, danh sách route và report manual-verification).
- `.gitignore`: ngoại lệ cho thư mục plan này và báo cáo evidence.
- Nội dung PR viết vào `pr-body.md` trong scratchpad; coordinator mở PR.
