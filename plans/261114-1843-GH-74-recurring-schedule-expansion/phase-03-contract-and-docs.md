---
phase: 3
title: "Contract và docs"
status: completed
priority: P1
effort: "0.25d"
dependencies: [2]
---

# Phase 3: Contract và docs

## Steps

1. Thêm `/api/v1/cash-flow-occurrences` vào `OpenApiContractIT.publishesOnlyTheProductionApiSurface`.
2. Regenerate `services/api/openapi/openapi.json` bằng `clean verify -Dopenapi.update=true` (chỉ local).
3. `clean verify` không flag phải xanh.
4. `docs/api-contract-conventions.md`: mục "Cash-flow occurrences — implemented" với quy tắc biên; sửa câu
   "no expansion" ở mục cash-flow entries và mục "What is not here".
5. `docs/manual-test-guide-cash-flow-occurrences.md`: tái hiện 51 case.

`ApplicationContextIT` không đổi: không có bảng mới.

## Rollback

Revert commit; không có migration nên không có dữ liệu cần dọn.
