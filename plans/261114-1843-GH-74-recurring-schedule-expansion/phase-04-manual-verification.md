---
phase: 4
title: "Xác minh thủ công"
status: pending
priority: P1
effort: "0.5d"
dependencies: [3]
---

# Phase 4: Xác minh thủ công

## Môi trường

- PostgreSQL dùng một lần `rikkaus-pg-issue-74`, cổng `127.0.0.1:5474`, xóa khi xong.
- API chạy bằng `spring-boot:run` trên JDK 25, `--spring.profiles.active=local --server.port=8074`,
  `DB_URL=jdbc:postgresql://localhost:5474/rikkaus`. Case 49 khởi động lại với
  `-Duser.timezone=Pacific/Pago_Pago`.

## Steps

1. Chạy case 1–51 bằng script trong scratchpad, ghi status và body thật.
2. Case không đạt: phân loại lỗi code hay kỳ vọng viết sai, sửa, chạy lại toàn bộ; ghi cả lần fail.
3. Ghi `plans/reports/manual-verification-261114-1843-GH-74-recurring-schedule-expansion.md` với commit đang
   chạy và số pass/fail.
4. Tắt API, xóa container.
