# Phase 3 — Contract và docs

Status: Complete

## Files

- `services/api/openapi/openapi.json`: regenerate bằng `-Dopenapi.update=true`.
- `services/api/src/test/java/com/rikkaus/wealth/shared/openapi/OpenApiContractIT.java`: thêm
  `/api/v1/goal-projections` và `/api/v1/goal-projections/{goalId}` vào inventory.
- `docs/api-contract-conventions.md`: mục "Goal projection" với công thức, làm tròn, ngưỡng, trạng thái không
  khả dụng (backbone: "method documented alongside the implementation").

## Validation

`clean verify` xanh không flag; diff `openapi.json` chỉ chứa hai path mới và schema của chúng.
