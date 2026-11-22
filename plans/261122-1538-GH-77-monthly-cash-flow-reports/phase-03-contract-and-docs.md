---
phase: 3
title: "Contract và docs"
status: completed
priority: P1
effort: "0.25d"
dependencies: [2]
---

# Phase 3: Contract và docs

- Regenerate `services/api/openapi/openapi.json` bằng `-Dopenapi.update=true` (chỉ local), rồi `clean verify`
  không flag.
- `OpenApiContractIT`: thêm `/api/v1/cash-flow-reports/monthly` vào danh sách path.
- `docs/api-contract-conventions.md`: mục "Cash-flow monthly report" ngay sau "Cash-flow occurrences" (công
  thức, quy đổi, tỷ trọng, drill-down, validation); sửa "What is not here" và danh sách báo cáo xác minh.
- `docs/manual-test-guide-cash-flow-reports.md`: lệnh chạy lại 40 case.
