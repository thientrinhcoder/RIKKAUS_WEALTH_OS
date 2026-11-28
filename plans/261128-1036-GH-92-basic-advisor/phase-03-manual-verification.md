---
phase: 3
title: "Chạy ma trận trên API thật"
status: completed
priority: P1
effort: "0.25d"
dependencies: [2]
---

# Phase 3: Chạy ma trận trên API thật

## Môi trường

Container dùng một lần `rikkaus-pg-92` (`postgres:18.6-alpine3.24`, `127.0.0.1:5492`, không volume, mật khẩu
`openssl rand -hex 24` chỉ nằm trong scratchpad quyền 600), API `java -jar … --spring.profiles.active=local
--server.port=8092`, `RIKKAUS_JWT_SECRET` sinh bằng `openssl rand -hex 48` vào biến môi trường. Container
`rikkaus_wealth_os-postgres-1` của người dùng không bị đụng tới.

## Thứ tự chạy

1. `OUT=<scratch> bash scripts/setup.sh` — nạp năm user qua API thật; một mục tiêu được dời ngày đích về
   31/10/2026 bằng SQL trong container dùng một lần.
2. `OUT=<scratch> bash scripts/cases.sh` — A01–A23; `recon.jq` chạy trên sáu body.
3. A24 nằm trong `recon.jq`; A25 là `ArchitectureRulesTest` cộng `grep` import; A26 là
   `./mvnw verify -Dopenapi.update=true`, `npm run api:generate`, `npm run api:check`, rồi
   `./mvnw clean verify`.
4. Khởi động lại API bằng jar của `clean verify` và chạy lại `cases.sh` trên cùng dữ liệu: body phải
   byte-identical với lần đầu.
5. Dừng API, xoá container.

Kết quả: [`manual-verification-261128-1036-GH-92-basic-advisor.md`](../reports/manual-verification-261128-1036-GH-92-basic-advisor.md).
