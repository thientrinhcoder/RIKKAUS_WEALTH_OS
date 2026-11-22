---
phase: 2
title: "shared/deletion và bốn slice"
status: completed
priority: P1
effort: "0.5d"
dependencies: [1]
---

# Phase 2: `shared/deletion` và bốn slice

## Files

- Mới: `services/api/src/main/java/com/rikkaus/wealth/shared/deletion/` — `DeletionImpact`,
  `LinkedRecordEffect`, `LinkedRecordReference`, `LinkedRecordRow` (interface projection cho native
  query), `DeletionGuard` (ném `ApiException(LINKED_RECORDS)` với extension `impact`), `package-info`.
- Sửa: `shared/error/ProblemType` (thêm `LINKED_RECORDS`).
- Assets: `AssetRepository` (đếm valuation, liệt kê liability/cash-flow trỏ tới), `AssetService`
  (`deletionImpact`, `delete(owner, id, confirmed)` dùng `findAndLockById`), `AssetController`.
- Liabilities: `LiabilityRepository` (`findAndLockById`, liệt kê cash-flow trỏ tới), `LiabilityService`,
  `LiabilityController`.
- Cash-flow: `CashFlowEntryService.deletionImpact`, `CashFlowEntryController` (chỉ thêm route preview).
- Goals: `GoalService.deletionImpact`, `GoalController` (chỉ thêm route preview).

## Steps

1. `shared/deletion` + `ProblemType.LINKED_RECORDS`.
2. Từng slice: query trong repository, service đọc impact sau `OwnershipGuard`, route `GET
   .../deletion-impact`, `DELETE` asset/liability nhận `confirm` và gọi `DeletionGuard`.
3. `clean verify` (ArchUnit: slice chỉ phụ thuộc `shared`).

## Risk

- Native query trả `uuid` qua interface projection: nếu Hibernate trả kiểu khác, ép `id::text` rồi
  `UUID.fromString` — kiểm tra ở phase 4 bằng I02.
- `PESSIMISTIC_WRITE` của Hibernate trên PostgreSQL là `FOR NO KEY UPDATE`, không chặn một insert
  liability/cash-flow mới trỏ tới asset chạy xen giữa lúc tính impact và lúc `DELETE`. Ghi vào
  verification gap.

## Rollback

Revert commit; không có migration nên database không cần đụng.
