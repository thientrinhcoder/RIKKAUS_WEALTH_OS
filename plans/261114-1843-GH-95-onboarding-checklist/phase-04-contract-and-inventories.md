---
phase: 4
title: "Contract và inventories"
status: completed
priority: P1
effort: "0.2d"
dependencies: [3]
---

# Phase 4: Contract và inventories

## Files

- Sửa `services/api/src/test/java/com/rikkaus/wealth/ApplicationContextIT.java` (thêm `onboarding_progress`)
- Sửa `services/api/src/test/java/com/rikkaus/wealth/shared/openapi/OpenApiContractIT.java` (thêm `/api/v1/onboarding`)
- Regenerate `services/api/openapi/openapi.json` bằng `-Dopenapi.update=true`
- Sửa `docs/api-contract-conventions.md`: mục "Guided onboarding — implemented" trước "What is not here"
- Sửa `.gitignore`: ngoại lệ cho plan dir và report

## Validation

`clean verify` xanh **không** flag cập nhật.
