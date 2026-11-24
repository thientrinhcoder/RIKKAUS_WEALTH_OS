---
phase: 3
title: "Contract, docs, chạy ma trận trước/sau"
status: completed
priority: P1
effort: "0.5d"
dependencies: [2]
---

# Phase 3: Contract, docs, chạy ma trận trước/sau

## Các bước

1. Đóng gói jar từ `main` `7d1b1b8`, chạy `scenario-runner.sh` trên PostgreSQL 18.6 sạch: 98/121, 23 FAIL
   đúng các kịch bản `[FIX]` và P01–P03.
2. Sau phase 2, `./mvnw clean verify -Dopenapi.update=true`: `openapi.json` không đổi, nên
   `apps/mobile/src/api/generated/*` không cần tái tạo.
3. `./mvnw clean verify` không flag trên commit code; chạy lại ma trận bằng jar của lần verify đó trên
   database sạch: 121/121.
4. `docs/api-contract-conventions.md`: mục mới *Validation hardening — implemented*; sửa các câu cũ về
   profile (trim), định nghĩa blank của assets, message `from` của forecast, lỗi ngày của goals, danh sách
   report thủ công trong *What is not here*.
5. Report `plans/reports/manual-verification-261124-1003-GH-98-validation-hardening.md`.

## Rủi ro

- Client nào đang gửi ngày dạng số, mảng hoặc date-time, hay enum dạng số, giờ nhận `malformed-request`.
  `apps/mobile` gửi chuỗi `YYYY-MM-DD` và tên hằng, nên không bị ảnh hưởng.
- Tên hoặc ghi chú hiện có chỉ gồm `U+200B`/`U+2060`/`U+FEFF` vẫn nằm trong database; chỉ lần sửa tiếp theo
  mới bị từ chối.

## Rollback

Revert commit code; không có migration hay dữ liệu cần khôi phục.
