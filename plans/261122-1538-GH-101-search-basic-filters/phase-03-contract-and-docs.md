---
phase: 3
title: "Contract, inventory, docs"
status: completed
priority: P1
effort: "0.2d"
dependencies: [2]
---

# Phase 3: Contract, inventory, docs

- Regenerate `services/api/openapi.json` bằng `-Dopenapi.update=true` (chỉ local); kiểm tra mỗi tham số
  mới có `description`, `example`, enum đầy đủ cho mảng.
- Inventory: không thêm path, không thêm bảng (không có migration) nên `OpenApiContractIT`,
  `ApplicationContextIT` không đổi; vẫn chạy trong `clean verify`.
- `docs/api-contract-conventions.md`: mục "Search & basic filters — implemented"; sửa các câu "takes no
  query parameter" của bốn resource.
- `docs/manual-test-guide-search-filters.md`: hướng dẫn chạy lại 96 scenario.
