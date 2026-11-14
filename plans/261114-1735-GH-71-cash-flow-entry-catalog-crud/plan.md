---
title: "Issue #71 danh mục dòng tiền và CRUD"
description: "Xây slice cashflow: bảng wealth.cash_flow_entries, resource /api/v1/cash-flow-entries cho 7 loại thu nhập và 9 loại chi phí, một lần hoặc định kỳ, liên kết tùy chọn tới tài sản/công nợ, ownership server-side."
status: in-progress
priority: P1
effort: "4d"
issue: 71
branch: thientrinhcoder/feat/issue-71-cash-flow-entry-catalog-crud
tags: [backend, api, migration, cash-flow]
blockedBy: [62]
blocks: [70, 74, 77, 80]
created: 2026-11-14
---

# Issue #71 danh mục dòng tiền và CRUD

## Outcome

Người dùng pilot tạo, liệt kê, đọc, sửa và xóa các khoản thu nhập và chi phí của chính mình qua
`/api/v1/cash-flow-entries`, với đủ 16 danh mục của MVP Backbone, một lần hoặc định kỳ
tháng/quý/năm, tiền là số thập phân chính xác, liên kết tùy chọn tới một tài sản (và sau khi #62 merge,
một công nợ) do chính họ sở hữu, lỗi RFC 9457, và contract OpenAPI đã publish để #70 (frontend) làm theo.

## Constraints

- Spring Boot modular monolith, PostgreSQL + Flyway, `/api/v1` qua `ApiPaths.V1`, lỗi theo
  `ProblemType`, ownership qua `CurrentUser` + `OwnershipGuard` + `UserOwned`. Không route nào nhận
  user id từ client.
- Slice `cashflow` chỉ được phụ thuộc `shared`. `ArchitectureRulesTest` fail compile nếu import class
  của `assets` hay `liabilities`, nên việc kiểm tra "tài sản này có thuộc người dùng không" làm bằng
  **một native query trong repository của slice** (`SELECT EXISTS ... FROM wealth.assets WHERE id = ?
  AND user_id = ?`). Lý do chọn cách này thay vì một port trong `shared`: quan hệ giữa hai bảng vốn đã
  là một foreign key ở tầng DB, nên câu SQL chỉ lặp lại đúng sự phụ thuộc đã có; một interface trong
  `shared` sẽ cần một bean do `assets` cung cấp, tức là `shared` phải biết đến `assets`, vi phạm chiều
  phụ thuộc mà rule bảo vệ.
- Không viết unit test hay integration test mới (chỉ đạo của Product Owner). Thay bằng danh sách
  scenario viết trước ở [phase 1](./phase-01-test-scenarios.md), chạy thật qua HTTP vào PostgreSQL
  Docker, ghi evidence. Hai inventory test sẵn có (`OpenApiContractIT`, `ApplicationContextIT`) vẫn
  phải mở rộng để `./mvnw verify` xanh.
- Tiền là `BigDecimal` / `NUMERIC(19, 2)`, currency chỉ `VND`/`USD` (dùng `shared.money.Currency`),
  không quy đổi tỷ giá trong slice này.
- Migration là `V7__cash_flow_entries.sql` (V5 thuộc PR #147, V6 thuộc #62). Khi merge `origin/main`
  thì đánh số lại thành `max(main) + 1` nếu cần.

## Non-goals

- Expansion định kỳ thành occurrence theo tháng (#74), báo cáo tháng (#77), forecast 3 tháng (#80),
  search/filter/sort (#101), safe delete và undo (#104).
- Quy đổi USD sang VND, tổng thu/chi, dòng tiền ròng: client tính với tỷ giá từ
  `GET /api/v1/settings/exchange-rate`.
- Nhãn tiếng Việt cho danh mục: client ánh xạ từ hằng số, như slice `assets`.

## Contract đã chốt (từ Backbone §MVP 1B, design #69 và hình mẫu slice `assets`)

| Trường | Kiểu | Bắt buộc | Quy tắc |
|---|---|---|---|
| `entryType` | `INCOME` \| `EXPENSE` | có | |
| `category` | 16 hằng số bên dưới | có | phải thuộc `entryType`; sai cặp là `validation-failed` trên `category` |
| `amount` | decimal | có | `> 0`, tối đa 17 số nguyên và 2 số lẻ |
| `currency` | `VND` \| `USD` | có | |
| `timing` | `ONE_TIME` \| `RECURRING` | có | thiết kế #69 gọi là *Thời điểm: Một lần / Định kỳ* |
| `recurrence` | `MONTHLY` \| `QUARTERLY` \| `YEARLY` | khi `RECURRING` | phải bỏ trống khi `ONE_TIME` |
| `effectiveDate` | date | có | ngày hiệu lực (một lần) hoặc ngày bắt đầu (định kỳ); **được phép ở tương lai** vì khoản dự kiến là đầu vào của forecast #80 |
| `endDate` | date | không | chỉ với `RECURRING`; không trước `effectiveDate`; bằng `effectiveDate` được chấp nhận |
| `linkedAssetId` | uuid | không | phải là tài sản của chính người dùng; của người khác và không tồn tại trả **cùng một** `validation-failed` |
| `linkedLiabilityId` | uuid | không | **phase 6, sau khi #62 merge**; không được gửi cùng `linkedAssetId` |
| `notes` | text ≤ 2000 | không | strip khoảng trắng, chuỗi trống thành `null`, ký tự NUL bị từ chối |

Danh mục thu nhập: `SALARY`, `BUSINESS_INCOME`, `RENTAL_INCOME`, `DIVIDEND`, `DEPOSIT_INTEREST`,
`BOND_OR_FUND_INCOME`, `OTHER_INCOME`. Danh mục chi phí: `LIVING_COSTS`, `HOUSING`, `EDUCATION`,
`HEALTHCARE`, `INSURANCE`, `TAX_AND_FEES`, `DEBT_PRINCIPAL_PAYMENT`, `INTEREST_PAYMENT`, `OTHER_EXPENSE`.

Quyết định đáng chú ý, ghi để reviewer không phải suy lại:

- **Không có trường `name`.** Backbone liệt kê đúng 9 thuộc tính cho một entry và không có tên; form
  quick-entry trong design #69 cũng không có ô tên. `notes` là nơi ghi "Lương tháng" hay "Tiền thuê căn
  hộ Thảo Điền".
- **`PUT` thay thế toàn bộ entry**, cùng body với `POST`, kể cả `entryType`, `category`, `amount`. Khác
  slice `assets`, ở đây không có trường nào bị đóng băng vì không có lịch sử append-only nào sở hữu nó.
- **Lỗi chéo trường được gom thành một `errors` array** trong một lần trả về, đúng cách slice `assets`
  gom hai lỗi ngày.
- **FK `linked_asset_id` là `ON DELETE SET NULL`.** `RESTRICT` sẽ làm `DELETE /assets/{id}` của slice
  khác trả 500 vì một bảng nó không biết; `CASCADE` xóa dữ liệu tài chính của người dùng không hỏi. Hậu
  quả về sản phẩm của việc xóa bản ghi có liên kết là phạm vi của #104.
- **Thứ tự liệt kê**: `effectiveDate` giảm dần, rồi `createdAt` giảm dần, để kết quả ổn định giữa hai lần
  gọi.

## Deliverables

- `services/api/src/main/resources/db/migration/V7__cash_flow_entries.sql`
- `services/api/src/main/java/com/rikkaus/wealth/cashflow/**`
- `services/api/openapi/openapi.json` regenerate
- `OpenApiContractIT` và `ApplicationContextIT` mở rộng
- `docs/api-contract-conventions.md` thêm mục cash-flow entries
- `docs/manual-test-guide-cash-flow-entries.md`
- `plans/reports/manual-verification-261114-1735-GH-71-cash-flow-entry-catalog-crud.md`

## Phases

| Phase | Tên | Trạng thái | Phụ thuộc | Kết quả |
|---|---|---|---|---|
| 1 | [Danh sách test scenario](./phase-01-test-scenarios.md) | Hoàn tất | Không | Mọi hành vi cần kiểm chứng được viết ra trước khi có dòng code nào. |
| 2 | [Schema và domain](./phase-02-schema-and-domain.md) | Đang làm | Phase 1 | `wealth.cash_flow_entries` với đủ check constraint; entity, 5 enum, repository hẹp. |
| 3 | [Service và HTTP resource](./phase-03-service-and-http.md) | Chờ | Phase 2 | Năm route dưới `/api/v1/cash-flow-entries`, validation chéo trường, kiểm tra liên kết. |
| 4 | [Contract và inventory](./phase-04-contract-and-inventories.md) | Chờ | Phase 3 | `openapi.json` regenerate, hai inventory test mở rộng, conventions doc cập nhật, `verify` xanh. |
| 5 | [Xác minh thủ công](./phase-05-manual-verification.md) | Chờ | Phase 4 | Chạy hết scenario phase 1 qua HTTP, ghi evidence. |
| 6 | [Liên kết công nợ](./phase-06-liability-link.md) | Chờ #62 merge | Phase 5 | `linkedLiabilityId` cùng cách kiểm tra, chạy lại scenario, cập nhật PR. |

## Acceptance Criteria

- [ ] Đủ 16 danh mục được chấp nhận đúng loại; sai cặp bị từ chối với `errors` nêu `category`.
- [ ] Một lần và định kỳ tháng/quý/năm đều tạo được; `recurrence` thừa hoặc thiếu bị từ chối, `endDate`
      trước ngày bắt đầu bị từ chối.
- [ ] `amount` là số thập phân chính xác, `> 0`, lưu `NUMERIC(19, 2)`; giá trị quá precision là
      `validation-failed` chứ không phải 500.
- [ ] Liên kết tới tài sản của chính mình được lưu; của người khác và không tồn tại trả cùng một body.
- [ ] Bản ghi của người khác trả `not-found` byte-identical với bản ghi không tồn tại, cho đọc và ghi.
- [ ] Không có token, token hết hạn, token sai chữ ký đều `unauthorized` với body giống nhau.
- [ ] `openapi.json` khớp ứng dụng đang chạy, có ví dụ; `./mvnw verify` xanh không dùng update flag.
- [ ] Guide tái hiện được mọi case; evidence ghi status/body thật từng case và commit đang chạy.
- [ ] Không có hành vi của #74/#77/#80/#101/#104 và không có dữ liệu giả.

## Verification gap

Không có automated test nào gọi vào resource này. Đó là chỉ đạo của Product Owner cho task này, cùng
tư thế mà #50 và #53 đã ship. Hậu quả: một regression trong slice chỉ bị phát hiện bằng cách chạy lại
`docs/manual-test-guide-cash-flow-entries.md`. Hai inventory test được mở rộng vẫn bắt được một bảng
không khai báo và một path chưa publish hoặc bị lộ; `ArchitectureRulesTest` vẫn bắt được import chéo slice
và trường tiền kiểu float.

<!-- slug: issue-71-cash-flow-entry-catalog-crud -->
