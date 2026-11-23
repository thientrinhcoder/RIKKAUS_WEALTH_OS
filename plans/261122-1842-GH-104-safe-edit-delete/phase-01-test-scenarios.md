---
phase: 1
title: "Danh sách test scenario"
status: completed
priority: P1
effort: "0.3d"
dependencies: []
---

# Phase 1: Danh sách test scenario

## Mục tiêu

Viết toàn bộ scenario trước khi có dòng code nào (TDD theo nghĩa scenario-first đã chốt với Product
Owner). Phase 4 chạy đúng danh sách này qua HTTP vào PostgreSQL 18.6 Docker và ghi status/body thật.

Quy ước: `A`, `B` là hai user; mọi request dùng token của A trừ khi ghi khác. Mọi lỗi là
`application/problem+json`. "psql:" là câu truy vấn chạy trực tiếp trên container để kiểm tra dữ liệu
thật sau request, không qua API. "Giống từng byte" nghĩa là hai body sau khi bỏ `instance` và
`correlationId` (hai member RFC cho phép khác) so bằng `diff` không ra dòng nào.

## Dữ liệu dùng xuyên suốt

### Assets

| Ký hiệu | Owner | name | category | giá trị hiện tại | Lịch sử định giá |
|---|---|---|---|---|---|
| AS1 | A | `Căn hộ Thảo Điền` | `REAL_ESTATE` | 4.200.000.000 ₫ | 3 bản ghi: 4.050.000.000 ₫ (10/05/2026, khi tạo), 4.120.000.000 ₫ (02/08/2026), 4.200.000.000 ₫ (15/11/2026) |
| AS2 | A | `Tài khoản Vietcombank` | `BANK_ACCOUNT` | 85.000.000 ₫ | 1 bản ghi khi tạo |
| AS3 | A | `Sổ tiết kiệm BIDV 12 tháng` | `TERM_DEPOSIT` | 300.000.000 ₫ | 1 |
| AS4 | A | `Xe Honda SH 160i` | `VEHICLE_OR_VALUABLE_ASSET` | 95.000.000 ₫ | 1 |
| AS5 | A | `Vàng SJC 5 lượng` | `VEHICLE_OR_VALUABLE_ASSET` | 425.000.000 ₫ | 1 |
| ASB1 | B | `Nhà phố Thảo Điền` | `REAL_ESTATE` | 9.500.000.000 ₫ | 1 |

### Liabilities

| Ký hiệu | Owner | name | category | dư nợ | collateral | trả định kỳ |
|---|---|---|---|---|---|---|
| LB1 | A | `Vay mua nhà Vietcombank` | `BANK_LOAN` | 2.400.000.000 ₫ | AS1 | 28.500.000 ₫/tháng |
| LB2 | A | `Thẻ tín dụng Techcombank Visa` | `CREDIT_CARD_BALANCE` | 18.700.000 ₫ | — | — |
| LBB1 | B | `Vay mua nhà Vietcombank` | `BANK_LOAN` | 1.800.000.000 ₫ | ASB1 | — |

### Cash-flow entries

| Ký hiệu | Owner | type / category | số tiền | timing | link | notes |
|---|---|---|---|---|---|---|
| CF1 | A | `INCOME` / `RENTAL_INCOME` | 18.000.000 ₫ | `RECURRING` `MONTHLY` từ 01/01/2026 | `linkedAssetId` AS1 | `Cho thuê căn hộ Thảo Điền` |
| CF2 | A | `EXPENSE` / `HOUSING` | 2.300.000 ₫ | `RECURRING` `MONTHLY` từ 01/01/2026 | `linkedAssetId` AS1 | `Phí quản lý căn hộ Thảo Điền` |
| CF3 | A | `EXPENSE` / `DEBT_PRINCIPAL_PAYMENT` | 28.500.000 ₫ | `RECURRING` `MONTHLY` từ 05/01/2026 | `linkedLiabilityId` LB1 | `Trả góp vay mua nhà Vietcombank` |
| CF4 | A | `INCOME` / `DEPOSIT_INTEREST` | 14.400.000 ₫ | `ONE_TIME` 15/12/2026 | `linkedAssetId` AS3 | `Lãi sổ tiết kiệm BIDV` |
| CF5 | A | `EXPENSE` / `EDUCATION` | 12.500.000 ₫ | `ONE_TIME` 03/09/2026 | — | `Học phí học kỳ 1 trường Quốc tế Á Châu` |
| CF6 | A | `EXPENSE` / `INSURANCE` | 3.200.000 ₫ | `RECURRING` `YEARLY` từ 01/03/2026 | `linkedAssetId` AS4 | `Bảo hiểm xe Honda SH` |
| CF7 | A | `INCOME` / `OTHER_INCOME` | 1.500.000 ₫ | `ONE_TIME` 20/11/2026 | `linkedAssetId` AS5 | `Cho mượn vàng lấy lãi` |
| CFB1 | B | `INCOME` / `RENTAL_INCOME` | 35.000.000 ₫ | `RECURRING` `MONTHLY` | `linkedAssetId` ASB1 | `Cho thuê nhà phố Thảo Điền` |

### Goals

| Ký hiệu | Owner | name | type | mục tiêu |
|---|---|---|---|---|
| G1 | A | `Quỹ học đại học cho con` | `EDUCATION` | 1.200.000.000 ₫ trước 31/08/2034 |
| G2 | A | `Du lịch Nhật Bản` | `OTHER_GOAL` | 120.000.000 ₫ trước 31/03/2028 |
| GB1 | B | `Quỹ dự phòng 6 tháng` | `EMERGENCY_FUND` | 270.000.000 ₫ trước 30/06/2027 |

## Scenario

### I — Deletion-impact preview

| ID | Request | Mong đợi |
|---|---|---|
| I01 | `GET /assets/AS2/deletion-impact` | 200; `recordType` `ASSET`, `recordId` AS2, `confirmationRequired` `false`, `linkedEffects` = một phần tử `{recordType: ASSET_VALUATION, effect: DELETED, field: null, count: 1, records: []}`, `derivedEffects` `[]` |
| I02 | `GET /assets/AS1/deletion-impact` | 200; `confirmationRequired` `true`; `linkedEffects` theo thứ tự: `ASSET_VALUATION`/`DELETED`/count 3/`records []`; `LIABILITY`/`UNLINKED`/`field collateralAssetId`/count 1/`[{id LB1, label "Vay mua nhà Vietcombank", category BANK_LOAN}]`; `CASH_FLOW_ENTRY`/`UNLINKED`/`field linkedAssetId`/count 2/`[CF1 "Cho thuê căn hộ Thảo Điền" RENTAL_INCOME, CF2 "Phí quản lý căn hộ Thảo Điền" HOUSING]`; `derivedEffects` `[]` |
| I03 | `GET /liabilities/LB1/deletion-impact` | 200; `confirmationRequired` `true`; một effect `CASH_FLOW_ENTRY`/`UNLINKED`/`field linkedLiabilityId`/count 1/`[CF3 "Trả góp vay mua nhà Vietcombank" DEBT_PRINCIPAL_PAYMENT]` |
| I04 | `GET /liabilities/LB2/deletion-impact` | 200; `confirmationRequired` `false`, `linkedEffects` `[]`, `derivedEffects` `[]` |
| I05 | `GET /cash-flow-entries/CF1/deletion-impact` (định kỳ, có link tới AS1) | 200; `confirmationRequired` `false`, `linkedEffects` `[]` (AS1 không bị đụng), `derivedEffects` `[CASH_FLOW_OCCURRENCES, MONTHLY_CASH_FLOW_REPORTS, CASH_FLOW_FORECAST]` |
| I06 | `GET /cash-flow-entries/CF5/deletion-impact` (một lần, không link) | 200; giống I05 về hình dạng |
| I07 | `GET /goals/G1/deletion-impact` | 200; `confirmationRequired` `false`, hai mảng rỗng |
| I08 | Sau I01–I07: psql đếm `asset_valuations` của AS1 | Vẫn 3; preview không ghi gì |

### O — Ownership

| ID | Request | Mong đợi |
|---|---|---|
| O01 | A: `GET /assets/ASB1/deletion-impact` so với A: `GET /assets/<UUID ngẫu nhiên>/deletion-impact` | Cả hai 404 `not-found`; giống từng byte |
| O02 | Như O01 với `/liabilities/LBB1/deletion-impact` | 404, giống từng byte với UUID ngẫu nhiên |
| O03 | Như O01 với `/cash-flow-entries/CFB1/deletion-impact` | 404, giống từng byte |
| O04 | Như O01 với `/goals/GB1/deletion-impact` | 404, giống từng byte |
| O05 | A: `DELETE /assets/ASB1?confirm=true` | 404 `not-found`; psql: ASB1 còn, LBB1 vẫn có `collateral_asset_id` = ASB1, CFB1 vẫn link ASB1 |
| O06 | A: `DELETE /liabilities/LBB1` (không confirm) | 404 `not-found`, không phải 409 (không lộ là record có liên kết) |

### U — Authentication

| ID | Request | Mong đợi |
|---|---|---|
| U01 | `GET /assets/AS1/deletion-impact` không có `Authorization` | 401 `unauthorized` |
| U02 | Như U01 với token hết hạn | 401 `unauthorized` |
| U03 | Như U01 với token ký bằng secret khác | 401 `unauthorized` |
| U04 | `DELETE /assets/AS1?confirm=true` không token | 401; psql: AS1 còn |

### M — Method và input

| ID | Request | Mong đợi |
|---|---|---|
| M01 | `POST /assets/AS1/deletion-impact` | 405 `method-not-allowed` |
| M02 | `DELETE /goals/G1/deletion-impact` | 405 `method-not-allowed`; G1 còn |
| M03 | `GET /assets/khong-phai-uuid/deletion-impact` | 400 `malformed-request` |
| M04 | `GET /liabilities/<UUID ngẫu nhiên>/deletion-impact` | 404 `not-found` |
| M05 | `DELETE /assets/AS1?confirm=co-le` | 400 `malformed-request`; psql: AS1 còn |

### E — Safe edit (chạy trước G)

| ID | Request | Mong đợi |
|---|---|---|
| E01 | `PUT /liabilities/LB1` gửi lại đủ field, `collateralAssetId` AS1, đổi tên thành `Vay mua nhà Vietcombank (gói ưu đãi 2026)` | 200, `collateralAssetId` AS1; I02 lặp lại liệt kê label mới |
| E02 | `PUT /cash-flow-entries/CF1` gửi lại `linkedAssetId` AS1, đổi `amount` 19.000.000 | 200, `linkedAssetId` AS1 |
| E03 | `PUT /assets/AS1` đổi tên `Căn hộ Thảo Điền (Masteri)` | 200; psql: LB1 vẫn collateral AS1, CF1/CF2 vẫn link AS1; I02 lặp lại vẫn count 3/1/2 |
| E04 | `PUT /cash-flow-entries/CF6` không gửi `linkedAssetId` | 200, `linkedAssetId` `null` (thay thế, đúng contract #71 đã ghi); impact AS4 không còn effect `CASH_FLOW_ENTRY` |

### G — Guard xác nhận và xóa

| ID | Request | Mong đợi |
|---|---|---|
| G01 | `DELETE /assets/AS1` (không confirm) | 409 `urn:rikkaus:problem:linked-records`, title `Linked records would change`; member `impact` bằng đúng body I02 (so bằng `jq`); psql: AS1 còn, 3 valuation còn, LB1/CF1/CF2 còn link |
| G02 | `DELETE /assets/AS1?confirm=false` | 409 như G01 |
| G03 | `DELETE /liabilities/LB1` (không confirm) | 409; `impact` bằng body I03; LB1 còn |
| G04 | `DELETE /assets/AS2` (không confirm, chỉ có lịch sử định giá của chính nó) | 204; psql: AS2 mất, 0 valuation của AS2 |
| G05 | `DELETE /liabilities/LB2` (không confirm) | 204 |
| G06 | `DELETE /cash-flow-entries/CF5` (không confirm) | 204; `GET /cash-flow-occurrences?from=2026-09-01&to=2026-09-30` không còn CF5 |
| G07 | `DELETE /goals/G1` (không confirm) | 204 |
| G08 | `DELETE /assets/AS4` (không confirm, sau E04 không còn link) | 204 |
| G09 | `DELETE /assets/AS1?confirm=true` | 204; psql: AS1 mất; 0 valuation của AS1 (CASCADE); LB1 còn, `collateral_asset_id` NULL; CF1, CF2 còn, `linked_asset_id` NULL; `GET /liabilities/LB1` → `collateralAssetId` `null` |
| G10 | `DELETE /assets/AS1?confirm=true` lần nữa | 404 `not-found` |
| G11 | `GET /liabilities/LB1/deletion-impact` sau G09 | Vẫn effect CF3 (liên kết liability không đổi) |
| G12 | `DELETE /liabilities/LB1?confirm=true` | 204; psql: CF3 còn, `linked_liability_id` NULL |
| G13 | `DELETE /cash-flow-entries/CF1?confirm=true` (định kỳ, tham số lạ bị bỏ qua) | 204 |

### C — Đồng thời

| ID | Request | Mong đợi |
|---|---|---|
| C01 | 4 × `DELETE /assets/AS3?confirm=true` song song (AS3 có CF4 link) | Đúng một 204, ba 404 `not-found`, không 500; psql: CF4 còn, `linked_asset_id` NULL |
| C02 | 4 × `DELETE /assets/AS5` song song, không confirm (AS5 có CF7 link) | Bốn 409, không 500; psql: AS5 còn |
| C03 | 4 × `DELETE /goals/G2` song song | Đúng một 204, ba 404, không 500 |

### K — Contract

| ID | Kiểm tra | Mong đợi |
|---|---|---|
| K01 | `GET /v3/api-docs` | Có bốn path `.../deletion-impact`; `DELETE /api/v1/assets/{assetId}` và `/liabilities/{liabilityId}` có tham số `confirm` và response `409`; enum `type` của `ProblemDetail` có `urn:rikkaus:problem:linked-records`; `ProblemDetail` có property `impact` |
| K02 | Log ứng dụng sau toàn bộ lượt chạy | Không có dòng nào chứa `Căn hộ Thảo Điền`, `Vay mua nhà` hay `confirm=` |
