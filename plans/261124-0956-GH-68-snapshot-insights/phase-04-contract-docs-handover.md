---
phase: 4
title: "Contract, docs, bàn giao"
status: completed
priority: P1
effort: "0.25d"
dependencies: [3]
---

# Phase 4: Contract, docs, bàn giao

- `./mvnw clean verify -Dopenapi.update=true` sinh lại `services/api/openapi/openapi.json`: chỉ thêm path
  `/api/v1/wealth-snapshot/insights` và các schema `SnapshotInsight*`; không dòng nào của contract cũ đổi.
- `./mvnw clean verify` không flag: Spotless, Surefire, Failsafe/Testcontainers và drift check của
  `OpenApiContractIT` xanh.
- `docs/api-contract-conventions.md`: mục mới cuối file "Wealth snapshot insights — implemented" (bảng rule →
  metric → ngưỡng → evidence, lý do không tính được, ngưỡng, biến môi trường).
- Rà tay: không có so sánh `int` với `long` trong điều kiện vòng lặp; không secret, token hay JWT trong plan,
  report, docs hay script.
- Nội dung PR do coordinator mở; Frontend #67 chạy `npm run api:generate` để lấy schema mới.
