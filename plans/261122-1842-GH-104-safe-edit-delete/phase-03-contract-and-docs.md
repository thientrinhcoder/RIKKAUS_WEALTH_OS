---
phase: 3
title: "Contract, inventory, docs"
status: completed
priority: P1
effort: "0.25d"
dependencies: [2]
---

# Phase 3: Contract, inventory, docs

## Steps

1. `OpenApiConfiguration.problemDetailSchema`: thêm property `impact` (`$ref DeletionImpact`).
2. `OpenApiContractIT`: thêm bốn path `deletion-impact` vào inventory; assert `ProblemDetail` có
   `impact` và enum `type` có `urn:rikkaus:problem:linked-records`.
3. Regenerate `services/api/openapi.json` bằng `clean verify -Dopenapi.update=true`, rồi `clean verify`
   không flag.
4. `docs/api-contract-conventions.md`: dòng `linked-records` trong bảng lỗi, mục "Deletion impact and
   linked-record confirmation — implemented", sửa các câu "issue #104's question" ở liabilities và
   cash-flow, cập nhật mô tả delete trong controller.
5. `docs/manual-test-guide-safe-delete.md`.
