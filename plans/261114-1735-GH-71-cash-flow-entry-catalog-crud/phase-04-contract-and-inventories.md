---
phase: 4
title: "Contract và inventory"
status: pending
priority: P1
effort: "0.5d"
dependencies: [3]
---

# Phase 4: Contract và inventory

## Mục tiêu

`./services/api/mvnw -f services/api/pom.xml verify` xanh mà không dùng update flag, và contract đã
publish cho #70.

## Bước

1. `OpenApiContractIT.publishesOnlyTheProductionApiSurface` thêm `/api/v1/cash-flow-entries` và
   `/api/v1/cash-flow-entries/{entryId}`.
2. `ApplicationContextIT.theMigrationsCreateExactlyTheTablesTheSlicesDeclare` thêm `cash_flow_entries`.
3. `verify -Dopenapi.update=true` một lần, đọc diff của `openapi.json`, rồi `verify` lần nữa không flag.
4. `docs/api-contract-conventions.md` thêm mục *Cash-flow entries — implemented*: route, 16 danh mục,
   bảng trường, quy tắc timing/recurrence, liên kết và lý do `validation-failed` không phân biệt "của người
   khác" với "không tồn tại", `PUT` là thay thế toàn bộ.
5. `.gitignore` thêm negation cho thư mục plan và evidence report này (pattern `plans/**/*` đang ignore
   mọi plan chưa được liệt kê).

## Validation

`verify` xanh; `git status` cho thấy plan, report và code đều được track.
