---
title: "Issue #62 liability catalog, schedules & collateral"
description: "Xây slice liabilities: bảng wealth.liabilities, 6 category, lịch trả nợ, liên kết tài sản bảo đảm cùng chủ, resource /api/v1/liabilities theo hình mẫu slice assets."
status: in-progress
priority: P1
effort: "3d"
issue: 62
branch: thientrinhcoder/feat/issue-62-liability-catalog-schedules-collateral
tags: [backend, api, migration, liabilities]
blockedBy: []
blocks: [61, 65, 74, 80]
created: 2026-11-14
---

# Issue #62 liability catalog, schedules & collateral

## Outcome

Người dùng pilot tạo, liệt kê, đọc, sửa và xóa công nợ của chính mình qua `/api/v1/liabilities`
với đủ 6 category của MVP Backbone, dư nợ là số thập phân chính xác, điều khoản (lãi suất, ngày bắt
đầu, ngày đáo hạn), lịch thanh toán định kỳ (khoản trả, tần suất, ngày trả tiếp theo), một tài sản bảo
đảm tùy chọn phải thuộc cùng chủ, ownership kiểm tra server-side, lỗi RFC 9457 và `openapi.json` đã
publish cho frontend (#61).

## Constraints

- Spring Boot modular monolith, PostgreSQL + Flyway, `BigDecimal`/`NUMERIC`, prefix `/api/v1`
  (`ApiPaths.V1`), lỗi theo `ProblemType`, ownership qua `CurrentUser` + `OwnershipGuard` +
  `UserOwned`; không route nào nhận user id từ client.
- Slice `liabilities` chỉ được phụ thuộc `shared`. `ArchitectureRulesTest` fail nếu import class của
  `assets`, nên kiểm tra sở hữu tài sản bảo đảm bằng native query trên `wealth.assets` ngay trong
  repository của slice (xem Quyết định).
- Theo chỉ đạo Product Owner: **không viết unit/integration test mới**. Phase 1 là danh sách scenario
  viết trước khi code; phase 5 chạy thật qua HTTP vào PostgreSQL Docker và ghi evidence. Hai inventory
  test sẵn có (`ApplicationContextIT` liệt kê bảng, `OpenApiContractIT` liệt kê path) phải mở rộng để
  `verify` xanh.
- Migration là `V6__liabilities.sql` (V5 thuộc PR #147). Khi merge đánh số lại thành `max(main)+1`.
- Tiền chỉ VND/USD, không chuyển đổi tỷ giá trong slice.

## Non-goals

- Expansion lịch trả nợ thành dòng tiền (#74/#77), aggregation nợ/net worth (#65), search/filter
  (#101), safe delete/undo (#104), pagination, tỷ giá.
- Sinh bảng khấu hao, tách gốc/lãi, tính lãi thực tế: thiết kế đã duyệt ghi rõ chỉ lưu điều khoản.

## Quyết định kỹ thuật (ghi lý do để review)

| # | Quyết định | Lý do |
|---|---|---|
| 1 | **Tần suất có 4 giá trị** `MONTHLY`, `QUARTERLY`, `SEMIANNUAL`, `YEARLY` | Thiết kế PO đã duyệt (`docs/liability-catalog-schedules-collateral-review.html`, mục 04) liệt kê "tháng, quý, 6 tháng, năm". Brief của coordinator chỉ nêu 3; thiết kế đã duyệt là nguồn chốt enum, nên lấy 4 và nêu ở báo cáo. |
| 2 | **`outstandingBalance >= 0`**, zero được chấp nhận | Backbone và brief nói "không âm"; gợi ý form trong thiết kế ghi "lớn hơn 0". Thẻ tín dụng đã thanh toán hết là trạng thái thật. Nêu để PO chốt; đổi thành `> 0` là một dòng ở cả `@DecimalMin` và CHECK. |
| 3 | **Mọi trường lịch và đáo hạn đều optional với mọi category** | Thiết kế nói "Không áp dụng chỉ chọn được cho thẻ tín dụng và bảo lãnh" là quy tắc biểu mẫu (`Không tự suy đoán theo danh mục`). Server không ép bank loan phải có lịch: vay ký quỹ thường không có ngày đáo hạn. Nêu là giả định. |
| 4 | **Khoản trả định kỳ và tần suất đi cùng nhau** (cả hai hoặc không) và khoản trả `> 0` | Lịch nửa vời không render được; khoản trả 0 không mô tả nghĩa vụ nào. CHECK `liabilities_recurring_payment_is_complete`. |
| 5 | **`nextPaymentDate` nằm trong `[startDate, maturityDate]`** khi các mốc đó có; được ở quá khứ | "Quá hạn" là trạng thái thiết kế hỗ trợ, nên ngày trả tiếp theo trong quá khứ hợp lệ; ngoài khoảng vay thì không. |
| 6 | **`maturityDate >= startDate`**, bằng nhau hợp lệ | Brief: "không trước start date". |
| 7 | **`interestRate` là `NUMERIC(7, 4)`**, %/năm, `>= 0`, `@Digits(integer = 3, fraction = 4)` | Lãi suất danh nghĩa như `8.5000`; 4 chữ số lẻ đủ cho lãi theo ngày quy năm; không bao giờ `double` (ArchUnit cấm field tên `*rate*` kiểu float). |
| 8 | **Kiểm tra sở hữu collateral bằng native query** `SELECT EXISTS (SELECT 1 FROM wealth.assets WHERE id = ? AND user_id = ?)` trong `LiabilityRepository` | Không import `assets` (ArchUnit). Một port trong `shared` sẽ bắt `shared` biết về khái niệm asset và cần `assets` implement, tức sửa slice khác trong PR này. FK đã ràng buộc hai bảng ở tầng schema, nên truy vấn đọc một bảng khác trong cùng DB là cùng tầng coupling, không thêm tầng mới. |
| 9 | **Collateral không thuộc mình hoặc không tồn tại → `validation-failed`**, `errors[{field: collateralAssetId, message: "must identify an asset the signed-in user owns"}]`, byte-identical hai trường hợp | `not-found` nói về URL resource; đây là một giá trị trong body. Một thông điệp cho cả hai để không lộ asset id nào tồn tại (cùng nguyên tắc `OwnershipGuard`). |
| 10 | **FK `collateral_asset_id` `ON DELETE SET NULL`** | Chặn xóa sẽ đổi hành vi slice `assets` (DELETE thành 409 mà `AssetController` chưa khai báo → 500). Safe delete và hậu quả bản ghi liên kết là #104. Công nợ vẫn tồn tại, chỉ mất liên kết; thiết kế có nút "Bỏ liên kết" nên trạng thái không-collateral là hợp lệ. |
| 11 | **`PUT` thay thế mọi trường trừ `category`**; dư nợ và tiền tệ sửa được | Màn hình Sửa trong thiết kế cho sửa "Dư nợ hiện tại"; không có issue lịch sử dư nợ tương đương #57. Category chọn một lần ở bước chọn danh mục, giống assets. |
| 12 | **Thứ tự list**: `nextPaymentDate ASC NULLS LAST`, rồi `name ASC` | Sắp đến hạn lên đầu phục vụ trạng thái "Sắp đến hạn/Quá hạn"; JPQL `NULLS LAST` qua `@Query`. |
| 13 | **Dùng `shared.money.Currency`** làm enum tiền tệ thay vì tạo `LiabilityCurrency` | Đã có trong `shared`, đúng hai giá trị, lưu bằng tên. |

## Deliverables

- `services/api/src/main/resources/db/migration/V6__liabilities.sql`
- `services/api/src/main/java/com/rikkaus/wealth/liabilities/{api,application,domain}/**`
- `services/api/openapi/openapi.json` regenerate; `OpenApiContractIT`, `ApplicationContextIT` mở rộng
- `docs/api-contract-conventions.md` thêm mục Liability catalog
- `docs/manual-test-guide-liabilities.md`
- `plans/reports/manual-verification-261114-HHMM-GH-62-liability-catalog-schedules-collateral.md`

## Phases

| Phase | Tên | Trạng thái | Phụ thuộc | Kết quả |
|---|---|---|---|---|
| 1 | [Danh sách test scenario](./phase-01-test-scenarios.md) | Completed | — | Mọi case viết trước khi code, có dữ liệu Việt Nam và kết quả mong đợi. |
| 2 | [Schema và domain](./phase-02-schema-and-domain.md) | Completed | 1 | `wealth.liabilities` với constraint; entity, enum, repository hẹp có native query sở hữu asset. |
| 3 | [Service và HTTP](./phase-03-service-and-http.md) | Completed | 2 | 5 route, request/response record, validation không-annotation, collateral check. |
| 4 | [Contract, inventory, docs](./phase-04-contract-inventories-docs.md) | Completed | 3 | `openapi.json`, hai inventory test, conventions doc; `verify` xanh không update flag. |
| 5 | [Chạy scenario và evidence](./phase-05-manual-verification.md) | Pending | 4 | Guide curl + report ghi status/body thật. |

## Acceptance criteria

- [ ] Đủ 6 category tạo được và nằm trong CHECK + enum + contract.
- [ ] Name, category, outstandingBalance, currency bắt buộc; mọi trường còn lại optional và đọc lại là `null`.
- [ ] Giá trị âm, quá precision, enum lạ, maturity trước start, next payment ngoài khoảng, lịch nửa vời bị từ chối đúng `ProblemType` và field.
- [ ] Collateral của chính mình 201; của người khác hoặc không tồn tại là `validation-failed` byte-identical; xóa asset thì công nợ còn, `collateralAssetId` thành `null`.
- [ ] Read/update/delete bản ghi người khác trả `not-found` byte-identical bản ghi không tồn tại.
- [ ] Anonymous/expired/forged token → 401 không phân biệt được.
- [ ] `openapi.json` khớp ứng dụng đang chạy, kèm ví dụ; `verify` xanh không `-Dopenapi.update`.
- [ ] Guide tái hiện mọi case, report ghi kết quả thật từng case, case fail rồi fix ghi là đã fail.

## Verification gap

Không có automated test nào gọi resource liabilities. Đây là chỉ đạo của Product Owner, không phải sơ
suất, và là posture #50/#53 đã ship. Hậu quả: regression trong slice chỉ bị phát hiện bằng chạy lại
`docs/manual-test-guide-liabilities.md`. Hai inventory test được mở rộng vẫn bắt được bảng chưa khai
báo và path lộ/thiếu trong contract; `ArchitectureRulesTest` vẫn bắt import chéo slice và field tiền
kiểu float.

<!-- slug: issue-62-liability-catalog-schedules-collateral -->
