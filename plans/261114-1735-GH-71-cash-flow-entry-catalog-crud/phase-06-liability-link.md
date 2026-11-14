---
phase: 6
title: "Liên kết công nợ"
status: pending
priority: P1
effort: "0.5d"
dependencies: [5]
---

# Phase 6: Liên kết công nợ (sau khi #62 merge)

## Mục tiêu

Hoàn tất nửa còn lại của "optional link to an asset or liability" khi bảng `wealth.liabilities` của #62
đã có trên `main`.

## Bước

1. `git fetch origin && git merge origin/main`; đánh số lại migration thành `max(main) + 1`.
2. Trong migration của slice: thêm `linked_liability_id UUID REFERENCES wealth.liabilities (id) ON DELETE
   SET NULL` và `CHECK (linked_asset_id IS NULL OR linked_liability_id IS NULL)`.
3. `CashFlowEntryRepository.liabilityBelongsTo` bằng native query cùng kiểu `assetBelongsTo`.
4. `linkedLiabilityId` vào request, response, entity, details; service từ chối khi không thuộc owner và khi
   gửi cùng `linkedAssetId` (`linkedLiabilityId` / `must not be combined with linkedAssetId`).
5. Scenario bổ sung: liên kết công nợ của mình (201), của người khác (400), không tồn tại (400 cùng body),
   cả hai liên kết cùng lúc (400), xóa công nợ thì entry còn với `linkedLiabilityId: null`, probe
   `INSERT` cả hai cột không NULL bị `cash_flow_entries_links_at_most_one_record` từ chối.
6. Regenerate `openapi.json`, `verify`, chạy lại **toàn bộ** scenario, cập nhật guide, report, conventions
   doc, PR.

## Trạng thái

Chờ coordinator báo #62 đã merge. Nếu mọi phần khác xong trước: commit, mở PR draft nêu rõ phần thiếu này.
