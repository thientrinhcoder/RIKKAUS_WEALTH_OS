# Phase 1 — Danh sách test scenario (viết trước khi code)

Mọi case dưới đây được viết trước khi có một dòng Java nào của slice, và là định nghĩa "xong" của
phase 2–4. Phase 5 chạy từng case thật qua HTTP vào PostgreSQL Docker và ghi status/body thật vào
report. Không có unit test hay integration test mới theo chỉ đạo Product Owner.

Quy ước: `A`, `B` là hai user; `api` là helper curl trong guide; mọi lỗi phải là
`application/problem+json` với `type` đúng `ProblemType`. Với `validation-failed`, assertion là mảng
`errors` và tên field, không chỉ status 400.

## Dữ liệu mẫu (user A)

| Mã | Category | Body tạo |
|---|---|---|
| L-BANK | `BANK_LOAN` | `Vay mua nhà Vietcombank`, `2400000000.00` VND, lãi `8.5`, start `2024-03-15`, maturity `2044-03-15`, trả `23500000.00` `MONTHLY`, next `2026-12-05`, collateral = asset `Căn hộ Thảo Điền` của A, notes `Thế chấp căn hộ Thảo Điền.` |
| L-PERSONAL | `PERSONAL_LOAN` | `Vay tiêu dùng FE Credit`, `120000000.00` VND, lãi `18`, start `2026-01-10`, maturity `2028-01-10`, trả `5800000.00` `MONTHLY`, next `2026-12-10` |
| L-MARGIN | `MARGIN_LOAN` | `Dư nợ ký quỹ SSI`, `350000000.00` VND, lãi `12.5`, không start/maturity/lịch |
| L-CARD | `CREDIT_CARD_BALANCE` | `Thẻ tín dụng Techcombank`, `38500000.00` VND, next `2026-11-25`, không lịch định kỳ, không đáo hạn |
| L-GUARANTEE | `GUARANTEE_OBLIGATION` | `Bảo lãnh hợp đồng thiết bị`, `10000.00` USD, start `2026-06-01`, maturity `2027-06-01`, không lịch |
| L-OTHER | `OTHER_LIABILITY` | `Nợ người thân mua xe`, `150000000.00` VND, không lãi, trả `50000000.00` `YEARLY`, next `2027-01-15` |

Asset bảo đảm tạo trước qua `POST /assets`: A có `Căn hộ Thảo Điền` (`REAL_ESTATE`, `6100000000.00`
VND); B có `Nhà phố Gò Vấp` (`REAL_ESTATE`, `4200000000.00` VND).

## S — Schema, đọc trực tiếp PostgreSQL

| # | Case | Mong đợi |
|---|---|---|
| S01 | Khởi động API trên DB trống | Flyway `... now at version v7`; `ddl-auto: validate` chấp nhận entity, context start |
| S02 | `information_schema.columns` của `wealth.liabilities` | `outstanding_balance numeric(19,2) NOT NULL`, `recurring_payment_amount numeric(19,2)`, `interest_rate numeric(7,4)`, các cột `*_date` là `date`, không có `double precision` |
| S03 | `pg_constraint` của bảng | Có đúng các CHECK: `liabilities_category_is_known`, `liabilities_currency_is_supported`, `liabilities_outstanding_balance_is_not_negative`, `liabilities_interest_rate_is_not_negative`, `liabilities_recurring_payment_amount_is_positive`, `liabilities_recurring_payment_frequency_is_known`, `liabilities_recurring_payment_is_complete`, `liabilities_maturity_not_before_start`, `liabilities_next_payment_within_term`, `liabilities_name_is_present_and_bounded`, `liabilities_notes_are_bounded` |
| S04 | FK `collateral_asset_id` | `confrelid = 'wealth.assets'`, `confdeltype = 'n'` (SET NULL); FK `user_id` `confdeltype = 'c'` |
| S05 | INSERT trực tiếp category `CRYPTO_LOAN` | `liabilities_category_is_known` violation, `count(*)` không đổi |
| S06 | INSERT `outstanding_balance = -1` | `liabilities_outstanding_balance_is_not_negative` |
| S07 | INSERT `currency = 'EUR'` | `liabilities_currency_is_supported` |
| S08 | INSERT maturity `2024-01-01` với start `2025-01-01` | `liabilities_maturity_not_before_start` |
| S09 | INSERT frequency `MONTHLY` không có amount | `liabilities_recurring_payment_is_complete` |
| S10 | INSERT `interest_rate = -0.5` | `liabilities_interest_rate_is_not_negative` |
| S11 | INSERT `name = '   '` | `liabilities_name_is_present_and_bounded` |
| S12 | SELECT dư nợ, lãi sau khi tạo L-BANK | `2400000000.00` và `8.5000` nguyên vẹn, không `2.4e+09` |

## C — Tạo, đủ 6 category và 4 tần suất

| # | Case | Mong đợi |
|---|---|---|
| C01–C06 | `POST /liabilities` lần lượt L-BANK … L-OTHER | `201`, `Location: /api/v1/liabilities/<id>`, body echo đúng mọi trường, trường không gửi là `null` |
| C07 | Body tối thiểu: chỉ `name`, `category`, `outstandingBalance`, `currency` | `201`, 9 trường optional đều `null` |
| C08 | `recurringPaymentFrequency: QUARTERLY` kèm amount | `201` |
| C09 | `SEMIANNUAL` kèm amount | `201` |
| C10 | `YEARLY` kèm amount (đã có trong C06) | `201` |
| C11 | USD với dư nợ `10000.00` | `201`, body không có bất kỳ số VND quy đổi nào |

## R — Thiếu từng trường bắt buộc

| # | Case | Mong đợi |
|---|---|---|
| R01 | Không `name` | `400` `validation-failed`, `{field: name, message: "must not be blank"}` |
| R02 | Không `category` | `{field: category, message: "must not be null"}` |
| R03 | Không `outstandingBalance` | `{field: outstandingBalance, message: "must not be null"}` |
| R04 | Không `currency` | `{field: currency, message: "must not be null"}` |
| R05 | `name: "   "` | `{field: name, message: "must not be blank"}` |
| R06 | `name: " "` (một non-breaking space) | `400` `{field: name, message: "must not be blank"}` |

Mỗi case đúng một entry trong `errors`.

## V — Giá trị: âm, quá precision, độ dài, ký tự null

| # | Case | Mong đợi |
|---|---|---|
| V01 | `outstandingBalance: -1.00` | `{field: outstandingBalance, message: "must not be negative"}` |
| V02 | `outstandingBalance: 100.123` | `{field: outstandingBalance, message: "must have at most 17 integer digits and 2 decimals"}` |
| V03 | `outstandingBalance: 0.00` | `201` (đã trả hết vẫn là bản ghi thật) |
| V04 | `interestRate: -0.5` | `{field: interestRate, message: "must not be negative"}` |
| V05 | `interestRate: 8.12345` | `{field: interestRate, message: "must have at most 3 integer digits and 4 decimals"}` |
| V06 | `interestRate: 1000` | cùng thông điệp V05 |
| V07 | `interestRate: 0` | `201` (vay không lãi) |
| V08 | `recurringPaymentAmount: 0` kèm frequency | `{field: recurringPaymentAmount, message: "must be greater than zero"}` |
| V09 | `recurringPaymentAmount: 5800000.00` không có frequency | `{field: recurringPaymentFrequency, message: "must be present when a recurring payment amount is given"}` |
| V10 | `recurringPaymentFrequency: MONTHLY` không có amount | `{field: recurringPaymentAmount, message: "must be present when a recurring payment frequency is given"}` |
| V11 | `name` 121 ký tự | `{field: name, message: "must be at most 120 characters"}` |
| V12 | `notes` 2001 ký tự | `{field: notes, message: "must be at most 2000 characters"}` |
| V13 | `name: "Vay\u0000nhà"` | `{field: name, message: "must not contain a null character"}` |
| V14 | `notes: "  Ghi chú có khoảng trắng.  "` | `201`, `notes` đọc lại đã strip |

## E — Enum lạ

| # | Case | Mong đợi |
|---|---|---|
| E01 | `category: "CRYPTO_LOAN"` | `400` `malformed-request`, không có `errors` |
| E02 | `currency: "EUR"` | `malformed-request` |
| E03 | `recurringPaymentFrequency: "WEEKLY"` | `malformed-request` |

## D — Ngày tháng

| # | Case | Mong đợi |
|---|---|---|
| D01 | start `2025-01-01`, maturity `2024-01-01` | `{field: maturityDate, message: "must not be before the start date"}` |
| D02 | start `2026-01-10`, next `2025-12-10` | `{field: nextPaymentDate, message: "must not be before the start date"}` |
| D03 | maturity `2028-01-10`, next `2028-02-10` | `{field: nextPaymentDate, message: "must not be after the maturity date"}` |
| D04 | start = maturity = `2026-06-01` | `201` |
| D05 | Chỉ `nextPaymentDate: 2026-11-25`, không start/maturity | `201` |
| D06 | start `2025-01-01`, maturity `2024-01-01`, next `2023-01-01` | **hai** entry: `maturityDate` và `nextPaymentDate` trong một response |
| D07 | `nextPaymentDate: 2026-01-05` (quá khứ, trong khoảng) | `201` — quá hạn là trạng thái hợp lệ |
| D08 | `startDate: "15/03/2024"` | `400` `malformed-request` |

## K — Tài sản bảo đảm

| # | Case | Mong đợi |
|---|---|---|
| K01 | A tạo với `collateralAssetId` = asset của A | `201`, `collateralAssetId` echo đúng |
| K02 | A tạo với asset của B | `400` `validation-failed`, `{field: collateralAssetId, message: "must identify an asset the signed-in user owns"}` |
| K03 | A tạo với UUID không tồn tại | `400`, body giống K02 trừ `correlationId` |
| K04 | `collateralAssetId: "khong-phai-uuid"` | `400` `malformed-request` |
| K05 | A xóa asset đang là collateral của L-BANK, rồi `GET` L-BANK | asset `204`; liability `200`, `collateralAssetId: null`, mọi trường khác giữ nguyên |
| K06 | `PUT` L-MARGIN gắn asset của A | `200`, `collateralAssetId` set |
| K07 | `PUT` L-MARGIN bỏ `collateralAssetId` | `200`, `collateralAssetId: null` |
| K08 | `PUT` L-MARGIN gắn asset của B | `400`, cùng body K02 |
| K09 | SELECT trực tiếp `collateral_asset_id` của L-BANK sau K05 | `NULL` trong bảng, không chỉ trong JSON |

## L — Liệt kê

| # | Case | Mong đợi |
|---|---|---|
| L01 | `GET /liabilities` as A | `200`, thứ tự `nextPaymentDate` tăng dần, `null` cuối, cùng ngày thì theo `name` |
| L02 | `GET /liabilities` as B | `200`, `[]` |

## O — Ownership

| # | Case | Mong đợi |
|---|---|---|
| O01 | A `GET` công nợ của A | `200` |
| O02 | B `GET` công nợ của A | `404` `not-found` |
| O03 | B `GET` `00000000-0000-4000-8000-000000000000` | `404`, body giống O02 trừ `instance`, `correlationId` |
| O04 | B `PUT` công nợ của A | `404`; A đọc lại không đổi |
| O05 | B `DELETE` công nợ của A | `404`; row còn |

## U — Sửa

| # | Case | Mong đợi |
|---|---|---|
| U01 | `PUT` L-PERSONAL đổi toàn bộ trường, dư nợ còn `96000000.00` | `200`, mọi trường đổi, `updatedAt` tiến, `createdAt` giữ |
| U02 | Body có thêm `"category":"OTHER_LIABILITY"` | `200`, `category` vẫn `PERSONAL_LOAN` |
| U03 | Bỏ `interestRate`, `notes`, `collateralAssetId` khỏi body | `200`, ba trường thành `null` (replacement, không patch) |
| U04 | `PUT` thiếu `name` | `400` `{field: name}` |
| U05 | `PUT` maturity trước start | `400` `{field: maturityDate}` |
| U06 | `PUT` đổi `currency` VND→USD và dư nợ `4000.00` | `200`, hai trường đổi |

## A — Xác thực

| # | Case | Mong đợi |
|---|---|---|
| A01 | Không header | `401` `unauthorized` |
| A02 | Token hết hạn | `401` |
| A03 | Token sai chữ ký | `401`; ba body chỉ khác `correlationId` |

## P — Protocol

| # | Case | Mong đợi |
|---|---|---|
| P01 | `GET /liabilities/not-a-uuid` | `400` `malformed-request` |
| P02 | `PATCH /liabilities/<id>` | `405` `method-not-allowed` |
| P03 | `DELETE /liabilities` | `405` |
| P04 | Body `text/plain` | `415` `unsupported-media-type` |
| P05 | JSON cụt `{"name": ` | `400` `malformed-request` |
| P06 | `Accept: application/xml` | `406` `not-acceptable` |

## X — Xóa

| # | Case | Mong đợi |
|---|---|---|
| X01 | `DELETE` L-OTHER | `204`, không body |
| X02 | `GET` L-OTHER | `404` |
| X03 | `DELETE` L-OTHER lần hai | `404` |
| X04 | `GET /liabilities` | ít hơn một dòng |
| X05 | 4 `DELETE` đồng thời một id, lặp 10 vòng | mỗi vòng đúng một `204`, ba `404`, **không `500`** |

## Tổng

12 case đọc PostgreSQL, 83 case HTTP (tính C01–C06 là 6, X05 là 1). Mọi case phải có trong
`docs/manual-test-guide-liabilities.md` dạng lệnh curl hoàn chỉnh, và trong report với status/body thật.
