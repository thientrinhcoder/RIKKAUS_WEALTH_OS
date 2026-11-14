---
phase: 4
title: "Contract và inventories"
status: completed
priority: P1
effort: "0.5d"
dependencies: [3]
---

# Phase 4: Contract và inventories

## Mục tiêu

`./mvnw verify` xanh không dùng flag cập nhật, contract đã commit khớp ứng dụng đang chạy.

## Bước

1. `ApplicationContextIT.theMigrationsCreateExactlyTheTablesTheSlicesDeclare` thêm `goals` (thứ tự
   alphabet: `assets`, `goals`, `refresh_tokens`, `user_settings`, `users`).
2. `OpenApiContractIT.publishesOnlyTheProductionApiSurface` thêm `/api/v1/goal-templates`,
   `/api/v1/goals`, `/api/v1/goals/{goalId}`.
3. `verify -Dopenapi.update=true` một lần, đọc diff `openapi.json`, rồi `verify` không flag.
4. `docs/api-contract-conventions.md`: mục "Financial goals — implemented" và cập nhật "What is not
   here".
5. `.gitignore`: ngoại lệ cho `plans/261114-1740-GH-86-generic-financial-goals-templates/` và report.

## Validation

`verify` xanh; `git status` cho thấy toàn bộ file slice được track (không bị `.gitignore` nuốt).
