---
phase: 3
title: "Contract và docs"
status: completed
priority: P1
effort: "0.25d"
dependencies: [2]
---

# Phase 3: Contract và docs

## Bước

1. Thêm `/api/v1/wealth-snapshot` vào `OpenApiContractIT.publishesOnlyTheProductionApiSurface`.
2. Regenerate `services/api/openapi/openapi.json` bằng `-Dopenapi.update=true` (chỉ local), xem diff.
3. `docs/api-contract-conventions.md`: mục "Wealth snapshot — implemented" (công thức, partial, quy đổi,
   thứ tự, cửa sổ, ngày nguồn, verified by hand) và cập nhật "What is not here".
4. `docs/manual-test-guide-wealth-snapshot.md`: chuẩn bị, `setup.sh`, `recon.jq`, `cases.sh`.
5. `clean verify` không flag phải xanh.
