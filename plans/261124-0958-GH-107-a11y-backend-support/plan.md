---
title: "Issue #107 backend support for responsive and accessibility work"
description: "Fixture scripts that load the design baseline and long lists through the public API, plus a measured audit of list size, order, ignored paging parameters and error shape, so the frontend needs no layout-specific workaround."
status: completed
priority: P2
effort: "1d"
issue: 107
branch: claude/issue-107-a11y-backend-support-bf11ee
tags: [backend, api, fixtures, accessibility, audit]
blockedBy: []
blocks: []
created: 2026-11-24
---

# Issue #107 backend support for responsive and accessibility work

## Outcome

Frontend chạy `reset.sh` → `load-baseline.sh` trên API local và thấy ngay dashboard của fixture 16.3:
tổng tài sản `12.002.000.000 ₫`, công nợ `1.532.000.000 ₫`, tài sản ròng `10.470.000.000 ₫`, tỷ giá
`1 USD = 25.100 VND cập nhật 12/09/2026`, một định giá cũ, một kỳ trả nợ trong 30 ngày, một mục tiêu
`AT_RISK`. `load-long-lists.sh` nạp 60 tài sản, 40 khoản nợ, 120 dòng tiền, 15 mục tiêu với tên tiếng
Việt dài có dấu để kiểm tra cuộn, xuống dòng và thứ tự ở 360 px và 1440 px. Một audit đo trên API thật
xác nhận list trả đủ, thứ tự ổn định, tham số phân trang lạ bị bỏ qua và mọi lỗi có đủ trường để UI
hiển thị inline, nên UI không cần workaround theo layout.

## Constraints

- Backend chỉ tham gia khi flow responsive/accessibility bị chặn bởi hành vi API (#31). Không có scope UI.
- Không thêm mã production cho fixture: script bash `curl` + `jq` gọi API công khai, nên mọi bản ghi đi
  qua đúng validation và ownership. Chỉ dùng `POST /api/v1/auth/dev-sign-in` (profile `local`).
- Không unit test mới (chỉ đạo PO). Verification là chạy tay trên API thật cổng 8107 với PostgreSQL
  18.6 Docker cổng 5407. `OpenApiContractIT`, `ApplicationContextIT`, `ArchitectureRulesTest` vẫn xanh.
- Không sửa request record, service, `shared/error`, `shared/search`, slice `snapshot` (thuộc #68, #98).
  Thiếu sót validation ghi vào "Phát hiện chuyển cho #98".
- Không tự thêm pagination. Nếu số đo cho thấy cần, ghi thành câu hỏi PO kèm con số.
- Script không in token, không chứa secret; secret local sinh bằng `openssl rand -hex 32`.

## Non-goals

- Thay đổi contract, schema, migration.
- Endpoint xoá hàng loạt, xoá tỷ giá, xoá feedback hay reset onboarding (không có route; script ghi rõ).
- Fixture cho môi trường không phải `local`.

## Phases

| Phase | File | Status |
|---|---|---|
| 1. Kịch bản viết trước | [phase-01-test-scenarios.md](phase-01-test-scenarios.md) | completed |
| 2. Script fixture và hướng dẫn | [phase-02-fixture-scripts.md](phase-02-fixture-scripts.md) | completed |
| 3. Audit, docs và evidence | [phase-03-audit-and-docs.md](phase-03-audit-and-docs.md) | completed |

## Acceptance criteria

- `reset.sh` → `load-baseline.sh` → `GET /wealth-snapshot` có `totalAssets.amountVnd = 12002000000.00`,
  `totalLiabilities.amountVnd = 1532000000.00`; chạy lại cho cùng kết quả.
- `load-long-lists.sh` → các list trả đủ số bản ghi đã nạp; chạy lại không nhân đôi.
- Audit đủ mọi list endpoint và mọi nhóm lỗi trong phase 1, có bảng đo trong evidence.
- `docs/api-contract-conventions.md` có mục "Pilot list bounds, error shape and local fixtures —
  implemented"; `docs/local-fixtures-guide.md` hướng dẫn chạy.
- `./mvnw clean verify` xanh; `bash -n` sạch trên mọi script.

## Evidence

`plans/reports/manual-verification-261124-0958-GH-107-a11y-backend-support.md`.
