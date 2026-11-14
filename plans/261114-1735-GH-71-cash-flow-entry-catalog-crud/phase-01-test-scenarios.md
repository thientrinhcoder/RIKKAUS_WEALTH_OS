---
phase: 1
title: "Danh sách test scenario"
status: completed
priority: P1
effort: "0.5d"
dependencies: []
---

# Phase 1: Danh sách test scenario

## Mục tiêu

Viết ra toàn bộ hành vi phải kiểm chứng **trước** khi có dòng code nào, theo cách hiểu TDD đã chốt
với Product Owner: scenario trước, không unit test. Mỗi scenario dưới đây trở thành một case `curl`
trong `docs/manual-test-guide-cash-flow-entries.md` và một dòng có status/body thật trong evidence
report. Số thứ tự ở đây là số case trong hai tài liệu đó.

## Dữ liệu dùng chung

- User A `0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93` (An Nguyễn), user B `7b41e590-3c82-4f16-b0d7-59e1c4f3a842`
  (Bích Trần), chèn thẳng vào `wealth.users`. Token HS256 mint bằng secret của profile `local`.
- Một tài sản của A: `Căn hộ Thảo Điền`, `REAL_ESTATE`, `6.100.000.000 ₫`; một tài sản của B:
  `Tiền mặt của Bích`, `5.000.000 ₫`. Tạo qua `POST /api/v1/assets` để không chạm vào bảng của slice khác.
- Entry mẫu: `Lương tháng 45.000.000 ₫`, `RECURRING` `MONTHLY` từ `2026-01-01`, không ngày kết thúc;
  `Tiền thuê căn hộ Thảo Điền 18.000.000 ₫`, `RENTAL_INCOME`, `MONTHLY` từ `2026-02-01`, liên kết căn hộ;
  `Học phí 12.500.000 ₫`, `EDUCATION`, `QUARTERLY` từ `2026-09-05` đến `2029-06-30`;
  `Cổ tức 150 USD`, `DIVIDEND`, `ONE_TIME` ngày `2026-09-12`.

## A. Đủ 16 danh mục, đúng loại (case 1–16)

| # | `entryType` | `category` | Body ví dụ | Kỳ vọng |
|---|---|---|---|---|
| 1 | `INCOME` | `SALARY` | `45000000.00 VND`, `RECURRING MONTHLY` từ `2026-01-01`, notes `Lương tháng` | `201`, `Location: /api/v1/cash-flow-entries/<id>` |
| 2 | `INCOME` | `BUSINESS_INCOME` | `26200000.00 VND`, `ONE_TIME` `2026-09-10` | `201` |
| 3 | `INCOME` | `RENTAL_INCOME` | `18000000.00 VND`, `MONTHLY` từ `2026-02-01`, notes `Tiền thuê căn hộ Thảo Điền` | `201` |
| 4 | `INCOME` | `DIVIDEND` | `150.00 USD`, `ONE_TIME` `2026-09-12` | `201`, echo `150.00` và `"currency":"USD"`, **không** có số VND |
| 5 | `INCOME` | `DEPOSIT_INTEREST` | `1500000.00 VND`, `QUARTERLY` từ `2026-03-31` | `201` |
| 6 | `INCOME` | `BOND_OR_FUND_INCOME` | `1500000.00 VND`, `YEARLY` từ `2026-06-30` | `201` |
| 7 | `INCOME` | `OTHER_INCOME` | `235000.00 VND`, `ONE_TIME` `2026-09-20` | `201` |
| 8 | `EXPENSE` | `LIVING_COSTS` | `23400000.00 VND`, `MONTHLY` từ `2026-01-01` | `201` |
| 9 | `EXPENSE` | `HOUSING` | `18750000.00 VND`, `MONTHLY` từ `2026-01-01` | `201` |
| 10 | `EXPENSE` | `EDUCATION` | `12500000.00 VND`, `QUARTERLY` từ `2026-09-05` đến `2029-06-30`, notes `Học phí` | `201`, `endDate` echo đúng |
| 11 | `EXPENSE` | `HEALTHCARE` | `4650000.00 VND`, `ONE_TIME` `2026-09-18` | `201` |
| 12 | `EXPENSE` | `INSURANCE` | `5400000.00 VND`, `YEARLY` từ `2026-04-01` | `201` |
| 13 | `EXPENSE` | `TAX_AND_FEES` | `8250000.00 VND`, `QUARTERLY` từ `2026-01-31` | `201` |
| 14 | `EXPENSE` | `DEBT_PRINCIPAL_PAYMENT` | `10000000.00 VND`, `MONTHLY` từ `2026-01-15` đến `2031-01-15` | `201` |
| 15 | `EXPENSE` | `INTEREST_PAYMENT` | `6250000.00 VND`, `MONTHLY` từ `2026-01-15` | `201` |
| 16 | `EXPENSE` | `OTHER_EXPENSE` | `2500000.00 VND`, `ONE_TIME` `2026-09-25` | `201` |

Case 1 cũng là case "mọi trường optional vắng": `recurrence` có, còn `endDate`, `linkedAssetId`,
`notes` (nếu bỏ) phải đọc lại là `null`, không phải `""` hay `0`.

## B. Danh mục sai loại (case 17–18)

| # | Gửi | Kỳ vọng |
|---|---|---|
| 17 | `entryType: EXPENSE`, `category: SALARY` | `400` `validation-failed`, đúng một `errors` entry `{field: category, message: "must belong to the entry type"}` |
| 18 | `entryType: INCOME`, `category: HOUSING` | như trên |

## C. Thiếu từng trường bắt buộc (case 19–24)

Bỏ lần lượt một member khỏi body tối thiểu (case 2). Mỗi case `400` `validation-failed` với đúng một
entry nêu trường đó và `must not be null`.

| # | Thiếu |
|---|---|
| 19 | `entryType` |
| 20 | `category` |
| 21 | `amount` |
| 22 | `currency` |
| 23 | `timing` |
| 24 | `effectiveDate` |

## D. Số tiền (case 25–28)

| # | `amount` | Kỳ vọng |
|---|---|---|
| 25 | `0` | `400`, `amount` / `must be greater than zero` |
| 26 | `-1000000.00` | `400`, `amount` / `must be greater than zero` |
| 27 | `1000000.123` | `400`, `amount` / `must have at most 17 integer digits and 2 decimals` |
| 28 | `45000000` (không số lẻ) | `201`, body trả về và lần `GET` sau đều là `45000000.00` |

## E. Hằng số lạ (case 29–33)

Mỗi case `400` `malformed-request`, **không** có `errors`, vì Jackson không dựng được record trước khi
bean validation chạy. Hành vi này đã ghi trong conventions doc cho slice `assets`.

| # | Gửi |
|---|---|
| 29 | `entryType: TRANSFER` |
| 30 | `category: CRYPTO_INCOME` |
| 31 | `currency: EUR` |
| 32 | `timing: WEEKLY` |
| 33 | `recurrence: DAILY` |

## F. Một lần, định kỳ, ngày (case 34–42)

| # | Gửi | Kỳ vọng |
|---|---|---|
| 34 | `ONE_TIME` kèm `recurrence: MONTHLY` | `400`, `recurrence` / `must be omitted for a one-time entry` |
| 35 | `RECURRING` không có `recurrence` | `400`, `recurrence` / `must be present for a recurring entry` |
| 36 | `ONE_TIME` kèm `endDate` | `400`, `endDate` / `must be omitted for a one-time entry` |
| 37 | `RECURRING MONTHLY`, `effectiveDate 2026-09-05`, `endDate 2026-09-04` | `400`, `endDate` / `must not be before the effective date` |
| 38 | `RECURRING MONTHLY`, `endDate` bằng `effectiveDate` | `201` |
| 39 | `RECURRING MONTHLY`, không `endDate` | `201`, `endDate: null` (định kỳ không giới hạn) |
| 40 | `effectiveDate 2027-01-01` (tương lai) | `201`, khoản dự kiến là hợp lệ |
| 41 | `ONE_TIME` kèm cả `recurrence: YEARLY` và `endDate` | `400`, **hai** `errors` entry, `recurrence` và `endDate`, trong một lần trả về |
| 42 | `effectiveDate: "01/01/2026"` (sai định dạng) | `400` `malformed-request` |

## G. Ghi chú (case 43–46)

| # | `notes` | Kỳ vọng |
|---|---|---|
| 43 | 2001 ký tự | `400`, `notes` / `must be at most 2000 characters` |
| 44 | `"   "` | `201`, `notes: null` |
| 45 | `"  Lương tháng 11.  "` | `201`, `notes: "Lương tháng 11."` |
| 46 | `"Lương\u0000tháng"` (NUL ở giữa) | `400`, `notes` / `must not contain a null character` |

## H. Liên kết tài sản (case 47–53)

| # | Gửi | Kỳ vọng |
|---|---|---|
| 47 | A tạo `RENTAL_INCOME` với `linkedAssetId` = căn hộ của A | `201`, echo `linkedAssetId` |
| 48 | A tạo với `linkedAssetId` = tài sản của B | `400`, `linkedAssetId` / `must reference one of the signed-in user's assets` |
| 49 | A tạo với `linkedAssetId` = `00000000-0000-4000-8000-000000000000` | `400`, body **giống case 48** ngoài `correlationId` |
| 50 | `linkedAssetId: "not-a-uuid"` | `400` `malformed-request` |
| 51 | A `PUT` entry 47 đổi `linkedAssetId` sang tài sản của B | `400` như case 48; đọc lại entry 47 không đổi |
| 52 | A `PUT` entry 47 bỏ `linkedAssetId` | `200`, `linkedAssetId: null` |
| 53 | A liên kết lại căn hộ rồi `DELETE /api/v1/assets/<căn hộ>`, đọc lại entry | `204` rồi `200`, entry còn và `linkedAssetId: null` (FK `ON DELETE SET NULL`) |

Các case liên kết công nợ ở mục P, thêm ở [phase 6](./phase-06-liability-link.md) sau khi #62 merge.

## I. Liệt kê (case 54–56)

| # | Request | Kỳ vọng |
|---|---|---|
| 54 | `GET /cash-flow-entries` của A | `200`, `effectiveDate` giảm dần rồi `createdAt` giảm dần |
| 55 | gọi lại case 54 | thứ tự y hệt |
| 56 | `GET /cash-flow-entries` của B | `200`, `[]`, không phải `404` |

## J. Ownership (case 57–61)

| # | Request | Kỳ vọng |
|---|---|---|
| 57 | A `GET` entry của A | `200` |
| 58 | B `GET` entry của A | `404` `not-found` |
| 59 | B `GET` `00000000-0000-4000-8000-000000000000` | `404`, body giống case 58 ngoài `instance` và `correlationId` |
| 60 | B `PUT` entry của A | `404`; A đọc lại không đổi |
| 61 | B `DELETE` entry của A | `404`; row còn |

## K. Sửa (case 62–66)

| # | Request | Kỳ vọng |
|---|---|---|
| 62 | A `PUT` entry 1: `amount 48000000.00`, notes `Lương tháng sau tăng lương` | `200`, `updatedAt` tiến, `createdAt` giữ nguyên |
| 63 | A `PUT` đổi `entryType`+`category` từ `INCOME/SALARY` sang `EXPENSE/LIVING_COSTS` | `200`, cả hai đổi |
| 64 | A `PUT` bỏ `notes` | `200`, `notes: null` (thay thế, không patch) |
| 65 | A `PUT` với `category` sai loại | `400`, `category` / `must belong to the entry type` |
| 66 | A `PUT` body có thêm `"id"` và `"userId"` lạ | `200`, hai member bị bỏ qua, `id` không đổi |

## L. Chứng thực (case 67–70)

| # | Request | Kỳ vọng |
|---|---|---|
| 67 | không header `Authorization`, `GET` collection | `401` `unauthorized` |
| 68 | token hết hạn | `401`, body giống 67 ngoài `correlationId` |
| 69 | token sai chữ ký | `401`, như trên |
| 70 | không token, `GET` member route | `401` |

## M. Giao thức (case 71–76)

| # | Request | Kỳ vọng |
|---|---|---|
| 71 | `GET /cash-flow-entries/not-a-uuid` | `400` `malformed-request` |
| 72 | `PATCH /cash-flow-entries/<id>` | `405` `method-not-allowed` |
| 73 | `DELETE /cash-flow-entries` | `405` |
| 74 | body `text/plain` | `415` `unsupported-media-type` |
| 75 | JSON cụt `{"entryType": ` | `400` `malformed-request` |
| 76 | `Accept: application/xml` | `406` `not-acceptable` |

Mọi case đều phải là `application/problem+json`.

## N. Xóa (case 77–81)

| # | Request | Kỳ vọng |
|---|---|---|
| 77 | A `DELETE` entry của A | `204`, không body |
| 78 | A `GET` id vừa xóa | `404` |
| 79 | A `DELETE` lại | `404` |
| 80 | A `GET` collection | `200`, ít đi một dòng |
| 81 | 4 `DELETE` đồng thời một id, lặp 10 vòng | mỗi vòng đúng một `204` và ba `404`, **không** `500` |

## O. Kiểm tra thẳng trong PostgreSQL (case 82–92)

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 82 | `information_schema.columns` của `wealth.cash_flow_entries` | `amount` là `numeric(19,2)`, không `double precision` |
| 83 | `SELECT amount` ba dòng lớn nhất và `SUM` | nguyên, không mũ, không làm tròn |
| 84 | `count(*)` theo `user_id` | khớp số entry mỗi user đã tạo |
| 85 | `INSERT` `entry_type = 'TRANSFER'` | bị từ chối; PostgreSQL báo `cash_flow_entries_category_matches_entry_type`, vì mọi category đều gắn với `INCOME` hoặc `EXPENSE` nên cặp sai luôn vi phạm cả constraint này, và nó được kiểm tra trước theo thứ tự tên |
| 86 | `INSERT` `INCOME` + `HOUSING` | `cash_flow_entries_category_matches_entry_type` |
| 87 | `INSERT` `amount = 0` | `cash_flow_entries_amount_is_positive` |
| 88 | `INSERT` `currency = 'EUR'` | `cash_flow_entries_currency_is_supported` |
| 89 | `INSERT` `ONE_TIME` kèm `recurrence` | `cash_flow_entries_recurrence_matches_timing` |
| 90 | `INSERT` `RECURRING` không `recurrence` | `cash_flow_entries_recurrence_matches_timing` |
| 91 | `INSERT` `end_date < effective_date` | `cash_flow_entries_end_date_not_before_effective_date` |
| 92 | `INSERT` `linked_asset_id` không tồn tại | vi phạm FK `cash_flow_entries_linked_asset_id_fkey` |

Sau mỗi probe `count(*)` không đổi. Hai probe 99–100 của mục P cũng thuộc nhóm này.

## P. Liên kết công nợ (case 93–100, thêm ở phase 6)

Dữ liệu: công nợ của A `Vay mua nhà Vietcombank`, `BANK_LOAN`, dư nợ `2.400.000.000 ₫`, trả `23.500.000 ₫`
hàng tháng; công nợ của B `Vay tiêu dùng FE Credit`, `PERSONAL_LOAN`, `120.000.000 ₫`. Tạo qua
`POST /api/v1/liabilities`. Một tài sản thứ hai của A, `Xe Volvo XC60`, cho case 96–97.

| # | Gửi | Kỳ vọng |
|---|---|---|
| 93 | A tạo `EXPENSE/INTEREST_PAYMENT` `17.000.000 ₫` `MONTHLY` với `linkedLiabilityId` = khoản vay của A, notes `Lãi vay mua nhà` | `201`, echo `linkedLiabilityId`, `linkedAssetId: null` |
| 94 | A tạo với `linkedLiabilityId` = khoản vay của B | `400`, `linkedLiabilityId` / `must reference one of the signed-in user's liabilities` |
| 95 | A tạo với `linkedLiabilityId` = `00000000-0000-4000-8000-000000000000` | `400`, body **giống case 94** ngoài `correlationId` |
| 96 | A tạo với cả `linkedAssetId` (xe) và `linkedLiabilityId` (khoản vay của A) | `400`, đúng một entry `linkedLiabilityId` / `must not be combined with linkedAssetId` |
| 97 | A `PUT` entry 93 đổi liên kết từ khoản vay sang xe | `200`, `linkedAssetId` là xe, `linkedLiabilityId: null` |
| 98 | A liên kết lại entry 93 với khoản vay, rồi `DELETE /api/v1/liabilities/<khoản vay>` và đọc lại entry | `204` rồi `200`, entry còn và `linkedLiabilityId: null` (FK `ON DELETE SET NULL`) |
| 99 | `INSERT` có cả `linked_asset_id` và `linked_liability_id` | `cash_flow_entries_links_at_most_one_record` |
| 100 | `INSERT` `linked_liability_id` không tồn tại | `cash_flow_entries_linked_liability_id_fkey` |

## Validation

Mọi case ở trên có một dòng status/body thật trong evidence report, và tổng số case trong report bằng
tổng số ở đây (100 sau phase 6). Case nào fail rồi fix phải ghi là đã fail.
