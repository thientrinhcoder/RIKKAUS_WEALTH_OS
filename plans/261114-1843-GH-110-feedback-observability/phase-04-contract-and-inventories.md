---
phase: 4
title: "Contract và inventories"
status: pending
priority: P1
effort: "0.25d"
dependencies: [2, 3]
---

# Phase 4: Contract và inventories

## Bước

1. `ApplicationContextIT` thêm bảng `feedback_entries`; `OpenApiContractIT` thêm `/api/v1/feedback`.
2. Regenerate `services/api/openapi/openapi.json` bằng `-Dopenapi.update=true` (chỉ local).
3. `docs/api-contract-conventions.md` thêm mục feedback; `docs/backend-deployment.md` thêm mục định dạng
   log và những gì không bao giờ được ghi.
4. `JAVA_HOME=<jdk25> ./services/api/mvnw -f services/api/pom.xml clean verify` xanh không flag.

## Validation

`clean verify` exit 0; diff `openapi.json` chỉ gồm path và schema feedback.
