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

Container dùng một lần `rikkaus-pg-83` (`postgres:18.6-alpine3.24`, `127.0.0.1:5483`, không volume, mật khẩu
`openssl rand -hex 24` chỉ nằm trong scratchpad), API `java -jar … --spring.profiles.active=local
--server.port=8083`.

## Thứ tự chạy

1. `OUT=<scratch> bash scripts/setup.sh` — nạp chín user qua API thật.
2. `OUT=<scratch> bash scripts/cases.sh main` — H01–H16; `cases.sh usd` — H21–H22 (hai nhánh USD không tỷ giá).
3. Dừng API; khởi động lại với `RIKKAUS_CASHFLOW_HEALTH_MINRESERVEMONTHS=5`,
   `RIKKAUS_CASHFLOW_HEALTH_MAXINCOMESOURCESHARE=0.8`,
   `RIKKAUS_CASHFLOW_HEALTH_ESSENTIALCATEGORIES=LIVING_COSTS,HOUSING,EDUCATION,HEALTHCARE,INSURANCE,TAX_AND_FEES,DEBT_PRINCIPAL_PAYMENT,INTEREST_PAYMENT`;
   `cases.sh custom` — H17.
4. Ba lần khởi động với cấu hình vô lý, mỗi lần phải tự thoát — H19.
5. Khởi động lại không biến nào; `cases.sh restored` — H18.
6. `./mvnw verify -Dopenapi.update=true`, rồi `./mvnw clean verify` — H20.

Kết quả: [`manual-verification-261127-1241-GH-83-financial-health.md`](../reports/manual-verification-261127-1241-GH-83-financial-health.md).
