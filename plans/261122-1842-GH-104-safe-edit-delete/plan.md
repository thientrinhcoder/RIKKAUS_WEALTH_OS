---
title: "Issue #104 safe edit & delete flows"
description: "Thêm deletion-impact preview cho bốn domain và guard 409 linked-records khi xóa asset/liability còn record khác trỏ tới; giữ nguyên ngữ nghĩa PUT thay thế và hành vi xóa đồng thời."
status: completed
priority: P1
effort: "1.5d"
issue: 104
branch: thientrinhcoder/feat/issue-104-safe-edit-delete
tags: [backend, api, delete, integrity]
blockedBy: []
blocks: [103]
created: 2026-11-22
---

# Issue #104 safe edit & delete flows

## Outcome

Trước khi xóa `Căn hộ Thảo Điền 4.200.000.000 ₫`, app hỏi backend và hiển thị được: "3 bản ghi định giá
sẽ bị xóa theo; khoản vay `Vay mua nhà Vietcombank` sẽ mất tài sản bảo đảm; 2 khoản dòng tiền
(`Cho thuê căn hộ Thảo Điền 18.000.000 ₫/tháng`, `Phí quản lý căn hộ Thảo Điền`) sẽ mất liên kết".
Nếu client bấm xóa mà chưa xác nhận các hệ quả đó, backend trả `409` kèm đúng payload ấy thay vì
âm thầm `SET NULL`. Record không có liên kết vẫn xóa `204` như trước; xóa đồng thời vẫn `204` rồi `404`.

## Constraints

- Spring Boot modular monolith, prefix `/api/v1`, lỗi RFC 9457 theo `ProblemType`, owner chỉ từ
  `CurrentUser`; record của user khác trả `not-found` giống từng byte (trừ `instance`, `correlationId`).
- Slice chỉ phụ thuộc `shared`. Kiểu dùng chung (payload impact, guard) ở `shared/deletion`; truy vấn
  đếm/liệt kê liên kết là native query trong repository của từng slice.
- Theo chỉ đạo Product Owner: **không viết unit test/integration test mới**. Phase 1 là danh sách
  scenario viết trước khi code; verification là chạy thật qua HTTP vào PostgreSQL 18.6 Docker.
  Inventory `OpenApiContractIT`, `ApplicationContextIT` và drift check `openapi.json` vẫn phải xanh.
- Chỉ sửa phần delete/impact của controller/service/repository bốn slice; không đụng
  `cashflow/reporting`, `cashflow/forecast`, `goals/projection`, slice `snapshot`. Không migration.
- Design #102 chưa được PO duyệt; các review HTML chỉ ghi "Hệ quả liên kết, hoàn tác và chính sách xóa
  thuộc #102". Contract dưới đây chốt tối thiểu từ backbone (`Safe edit/delete confirmation flows`) và
  FK thật trên `main`.

## Non-goals

- Soft delete, hoàn tác, thùng rác, thời gian lưu giữ.
- Audit log, bulk delete, đổi FK (`SET NULL`/`CASCADE` giữ nguyên).
- Optimistic locking (`If-Match`, `ETag`, cột `version`): không review HTML nào đòi hỏi; một user pilot
  trên một thiết bị. Xem câu hỏi PO #3.
- Đổi ngữ nghĩa `PUT` từ thay thế sang patch.
- Liệt kê tác động phái sinh (dashboard, forecast, báo cáo) khi xóa asset/liability. Xem câu hỏi PO #2.

## Quyết định contract

| Quyết định | Giá trị | Căn cứ |
|---|---|---|
| Route preview | `GET /api/v1/assets/{assetId}/deletion-impact`, `/liabilities/{liabilityId}/deletion-impact`, `/cash-flow-entries/{entryId}/deletion-impact`, `/goals/{goalId}/deletion-impact` | Sub-resource chỉ đọc, cùng ownership như `GET` record |
| Payload `DeletionImpact` | `recordType`, `recordId`, `confirmationRequired`, `linkedEffects[]`, `derivedEffects[]` | Một schema cho cả bốn domain; client render một hộp thoại |
| `linkedEffects[]` | `{recordType, effect, field, count, records[]}`; `effect` = `DELETED` (xóa theo, CASCADE) hoặc `UNLINKED` (`field` bị đặt `null`, SET NULL); chỉ có phần tử khi `count > 0` | Tính từ FK thật: `asset_valuations` CASCADE; `liabilities.collateral_asset_id`, `cash_flow_entries.linked_asset_id`/`linked_liability_id` SET NULL |
| `records[]` | `{id, label, category}`; `label` = `name` của liability, `notes` của cash-flow (có thể `null`); valuation không liệt kê (`[]`), chỉ đếm | Cash-flow entry không có `name`; `category` để client dịch nhãn tiếng Việt |
| `derivedEffects[]` | Cash-flow entry: `CASH_FLOW_OCCURRENCES`, `MONTHLY_CASH_FLOW_REPORTS`, `CASH_FLOW_FORECAST` (mô tả, không đếm). Ba domain còn lại: `[]` | Task: "occurrence/report/forecast không còn khoản đó" |
| Khi nào cần xác nhận | `confirmationRequired = true` khi có ít nhất một effect `UNLINKED`. Lịch sử định giá (`DELETED`) là một phần của chính asset, không bắt xác nhận | Asset nào cũng có ≥ 1 valuation; nếu tính, mọi asset đều 409 và "record không liên kết xóa như cũ" không còn đúng. Câu hỏi PO #1 |
| Guard | `DELETE` khi `confirmationRequired` mà chưa xác nhận → `409` `urn:rikkaus:problem:linked-records`, extension `impact` = đúng payload preview tại thời điểm đó; không có gì bị xóa | Feature criteria "Không xóa ngoài ý muốn" |
| Cơ chế xác nhận | Query `confirm=true` trên `DELETE /assets/{id}` và `DELETE /liabilities/{id}`. Thiếu hoặc `false` = chưa xác nhận; giá trị không phải boolean → `400 malformed-request`. Cash-flow/goals không có liên kết nên không khai báo tham số; Spring bỏ qua tham số lạ nên client gửi `confirm=true` đồng loạt vẫn được | Header tùy biến phải thêm vào CORS allowlist (`CorsConfiguration` chỉ cho `Content-Type`, `X-Correlation-Id`) và gây preflight; query string không bao giờ được log (`RequestLoggingFilter`) |
| Đồng thời | `DELETE` asset/liability khóa dòng (`PESSIMISTIC_WRITE`) rồi mới tính impact; request đến sau chờ, thấy dòng đã mất → `404`. Cash-flow/goals giữ cơ chế flush hiện có | Bốn delete đồng thời: một `204`, còn lại `404`, không `500` |
| Safe edit | Giữ nguyên `PUT` là thay thế (contract #53/#62/#71): gửi lại `collateralAssetId`/`linkedAssetId` thì giữ liên kết; bỏ field thì liên kết bị xóa — đã ghi trong mô tả `PUT` và conventions. `PUT` asset không chạm các record trỏ tới nó | "Không đổi hành vi đã có nếu không bắt buộc" |
| Problem type mới | `linked-records`, title `Linked records would change`, status 409 | Thêm vào `ProblemType`, enum `type` trong `ProblemDetail` của OpenAPI, bảng trong conventions |
| Migration | Không | FK và index hiện có đủ |

## Phases

| # | Phase | File | Status |
|---|---|---|---|
| 1 | Danh sách test scenario | [phase-01-test-scenarios.md](phase-01-test-scenarios.md) | completed |
| 2 | `shared/deletion` và bốn slice | [phase-02-shared-and-slices.md](phase-02-shared-and-slices.md) | completed |
| 3 | Contract, inventory, docs | [phase-03-contract-and-docs.md](phase-03-contract-and-docs.md) | completed |
| 4 | Chạy scenario thật, ghi evidence | [phase-04-manual-verification.md](phase-04-manual-verification.md) | completed |

## Acceptance criteria

- Mọi scenario ở phase 1 pass trên commit cuối, chạy qua HTTP vào PostgreSQL 18.6 Docker; case fail
  rồi sửa thì ghi là đã fail.
- `clean verify` xanh không flag; `openapi.json` regenerate; `OpenApiContractIT` liệt kê bốn path mới và
  extension `impact`.
- `docs/api-contract-conventions.md` có mục deletion impact và guard, bảng problem type có
  `linked-records`; `docs/manual-test-guide-safe-delete.md` chạy lại được; report ở `plans/reports/`.

## Verification gap

- Không có automated test nào chạm route preview hay guard (chỉ đạo PO). Regression chỉ bị bắt khi chạy
  lại `docs/manual-test-guide-safe-delete.md`. `OpenApiContractIT` chỉ giữ inventory path, enum problem
  type và property `impact`, không kiểm hành vi.
- Race giữa "tính impact" và "xóa" chỉ được đóng cho hai delete chồng nhau. `PESSIMISTIC_WRITE` của
  Hibernate trên PostgreSQL là `SELECT … FOR NO KEY UPDATE`, không xung đột với khóa `FOR KEY SHARE` mà
  một `INSERT` liability/cash-flow mới trỏ tới asset đó lấy. Một record được link vào đúng khoảnh khắc
  giữa lúc đọc impact và lúc `DELETE` vẫn bị `SET NULL` mà không có 409. Chưa tái hiện được bằng HTTP;
  với một user trên một thiết bị, khả năng xảy ra rất thấp.
- Chạy đồng thời chỉ ở mức 4 request một lần cho mỗi case (C01–C03), không lặp nhiều vòng như lần đo
  của #53.
- Chỉ chạy trên PostgreSQL 18.6 Alpine trong Docker; chưa chạy trên AWS RDS.
- Chưa chạy cùng app mobile (#103) và chưa đối chiếu design #102 (chưa duyệt): nhãn tiếng Việt, cách
  hiển thị danh sách bị ảnh hưởng và luồng hai bước là việc của frontend/design.
- Thứ tự `records[]` là `created_at, id`; chưa có scenario hai record tạo cùng micro giây.

## Kết quả

45/45 scenario pass trên commit `c8dbeab` (lần 3, database sạch). Lần 1 44/45: U01 fail và U04 pass sai
do lỗi script với bash 3.2; lần 2 45/45 nhưng G06 kiểm sai tên trường (`entryId` thay vì
`sourceEntryId`) nên pass rỗng; cả hai đã sửa trong script, không đổi code. `clean verify` không flag
xanh (131 test, 0 fail). Sau khi merge `origin/main` `0eb1ec3` (commit `a13a7d6`): verify xanh và
45/45 scenario pass lại trên jar đã merge. Evidence:
[`plans/reports/manual-verification-261122-1842-GH-104-safe-edit-delete.md`](../reports/manual-verification-261122-1842-GH-104-safe-edit-delete.md).

## Câu hỏi cho PO / design #102

1. **Lịch sử định giá có cần xác nhận không?** Hiện xóa `Căn hộ Thảo Điền` chỉ có 3 bản ghi định giá
   (không có khoản vay hay dòng tiền nào trỏ tới) vẫn xóa ngay `204`; preview vẫn báo "3 bản ghi định
   giá sẽ bị xóa". Nếu muốn bắt xác nhận khi có hơn 1 bản ghi định giá, đổi là một điều kiện trong
   `DeletionImpact.of`, nhưng khi đó gần như mọi tài sản đã cập nhật giá đều thành xóa hai bước.
2. **Tác động phái sinh khi xóa tài sản/khoản nợ.** Preview chỉ liệt kê view phái sinh cho dòng tiền.
   Xóa `Tài khoản Vietcombank 85.000.000 ₫` làm số dư đầu kỳ của dự báo 3 tháng giảm, xóa căn hộ làm
   dòng `Cho thuê căn hộ Thảo Điền 18.000.000 ₫/tháng` rời khỏi `passiveIncome.byAsset`, xóa khoản vay
   có lịch trả làm dự báo bỏ khoản trả `28.500.000 ₫/tháng`. Có cần hộp thoại nói các điều này không?
   Nếu có, backend sẽ thêm `CASH_FLOW_FORECAST`/`MONTHLY_CASH_FLOW_REPORTS` vào `derivedEffects` theo
   đúng quy tắc của #77/#80 (và `WEALTH_SNAPSHOT` sau khi #65 merge).
3. **Optimistic locking.** Không thêm `If-Match`/`version` vì không review HTML nào yêu cầu. Hai thiết bị
   cùng sửa `Vay mua nhà Vietcombank` thì bản lưu sau thắng. Có cần màn hình xung đột không?
4. **`PUT` bỏ trống liên kết.** Giữ ngữ nghĩa thay thế đã chốt ở #62/#71: `PUT` khoản dòng tiền không gửi
   `linkedAssetId` sẽ gỡ liên kết với `Căn hộ Thảo Điền`. Frontend (#103) phải luôn gửi lại liên kết
   hiện có. Có muốn đổi sang "không gửi = giữ nguyên" không? Đó là thay đổi contract của ba resource.
5. **Hoàn tác.** Review goals ghi "Hủy, hoặc xóa kèm hoàn tác ngắn"; backend chưa có undo/soft delete
   (non-goal của ticket này). Nếu design #102 chốt hoàn tác, cần một ticket riêng.
