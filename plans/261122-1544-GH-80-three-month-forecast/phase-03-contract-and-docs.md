# Phase 3 — Contract và docs

Status: Complete · Depends on: phase 2

## Steps

1. Thêm `/api/v1/cash-flow-forecast` vào inventory `OpenApiContractIT.publishesOnlyTheProductionApiSurface`.
2. Regenerate `services/api/openapi.json` bằng `-Dopenapi.update=true`, đọc diff, rồi `clean verify` không flag.
3. Thêm mục "Cash-flow forecast" vào `docs/api-contract-conventions.md`: nguồn, công thức số dư, quy đổi,
   assumptions, lý do loại nguồn.

## Validation

`clean verify` xanh không flag; diff `openapi.json` chỉ thêm path và schema forecast.
