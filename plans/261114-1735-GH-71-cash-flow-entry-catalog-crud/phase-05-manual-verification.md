---
phase: 5
title: "Xác minh thủ công"
status: pending
priority: P1
effort: "1d"
dependencies: [4]
---

# Phase 5: Xác minh thủ công

## Mục tiêu

Chạy thật toàn bộ scenario ở [phase 1](./phase-01-test-scenarios.md) qua HTTP vào PostgreSQL trong
Docker, và để lại hai thứ: một guide ai cũng chạy lại được, và một bản ghi điều lần chạy này thực sự
trả về.

## Môi trường

- `docker run -d --name rikkaus-pg-issue-71 -p 127.0.0.1:5471:5432 -e POSTGRES_DB=rikkaus
  -e POSTGRES_USER=rikkaus -e POSTGRES_PASSWORD=rikkaus-local-only postgres:18.6-alpine3.24`
- Jar đóng gói, `--spring.profiles.active=local --server.port=8071`,
  `DB_URL=jdbc:postgresql://localhost:5471/rikkaus`. Không chạm volume `rikkaus-postgres-data`.
- Hai user chèn thẳng; token mint như `docs/manual-test-guide-assets.md`.

## Bước

1. Viết `docs/manual-test-guide-cash-flow-entries.md`: mỗi case là một `curl` hoàn chỉnh với status và
   phần body kỳ vọng. Không case nào nói "kiểm tra nó chạy".
2. Chạy case theo thứ tự, dán status/body thật vào
   `plans/reports/manual-verification-261114-1735-GH-71-cash-flow-entry-catalog-crud.md`.
3. Case fail: sửa code, ghi rõ đã fail và đã sửa gì, rồi **chạy lại toàn bộ** trên commit cuối để evidence
   mô tả trạng thái commit chứ không phải trạng thái giữa chừng.
4. Tắt API, `docker rm -f rikkaus-pg-issue-71`.

## Validation

Report có verdict cho mọi case trong guide, tổng số khớp, và nêu commit đã chạy.
