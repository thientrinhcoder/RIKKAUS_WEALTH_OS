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

Viết toàn bộ scenario trước khi có dòng code nào (TDD theo nghĩa scenario-first đã chốt với Product
Owner). Mỗi scenario là một request cụ thể với dữ liệu Việt Nam thật và kết quả mong đợi đủ rõ để một
người khác chạy và chấm được. Phase 5 chạy đúng danh sách này và ghi kết quả thật.

Quy ước: `A`, `B` là hai user; `TODAY`, `YESTERDAY`, `TOMORROW` tính theo `Asia/Ho_Chi_Minh`. Mọi lỗi
là `application/problem+json`; với `validation-failed`, assertion là mảng `errors` và field nó nêu,
không phải mã 400 đơn thuần.

## Dữ liệu dùng xuyên suốt

| Mục tiêu | `type` | `targetAmount` | `currentAllocatedAmount` | `targetDate` | `plannedMonthlyContribution` | `expectedAnnualReturnPercent` |
|---|---|---|---|---|---|---|
| Quỹ dự phòng 6 tháng | `EMERGENCY_FUND` | 270.000.000 ₫ | 90.000.000 ₫ | 30/06/2027 | 15.000.000 ₫ | 3,00 |
| Mua căn hộ | `HOME_PURCHASE` | 3.500.000.000 ₫ | 1.200.000.000 ₫ | 31/12/2030 | 25.000.000 ₫ | 7,00 |
| Học phí đại học cho con | `EDUCATION` | 900.000.000 ₫ | 150.000.000 ₫ | 31/08/2032 | 8.000.000 ₫ | 5,00 |
| Hưu trí | `RETIREMENT` | 15.000.000.000 ₫ | 2.150.000.000 ₫ | 31/12/2041 | 20.000.000 ₫ | 8,00 |
| Tăng trưởng tài sản ròng | `NET_WORTH_GROWTH` | 15.000.000.000 ₫ | 10.470.000.000 ₫ | 31/12/2030 | 10.000.000 ₫ | 7,00 |
| Tất toán vay mua nhà | `DEBT_REDUCTION` | 1.500.000.000 ₫ | 90.000.000 ₫ | 31/12/2030 | 10.000.000 ₫ | 0,00 |
| Du lịch Nhật Bản | `OTHER_GOAL` | 120.000.000 ₫ | (bỏ trống) | 31/03/2028 | (bỏ trống) | (bỏ trống) |

## A. Bảy type tạo được

| # | Request | Mong đợi |
|---|---|---|
| G01 | `POST /goals` Quỹ dự phòng 6 tháng (`EMERGENCY_FUND`) | `201`, `Location: /api/v1/goals/<id>`, body echo đủ 7 field, `currency: "VND"`, `createdAt == updatedAt` |
| G02 | `POST /goals` Mua căn hộ (`HOME_PURCHASE`) | `201` |
| G03 | `POST /goals` Học phí đại học cho con (`EDUCATION`) | `201` |
| G04 | `POST /goals` Hưu trí (`RETIREMENT`) | `201`, `targetAmount` đọc lại `15000000000.00` |
| G05 | `POST /goals` Tăng trưởng tài sản ròng (`NET_WORTH_GROWTH`) | `201` |
| G06 | `POST /goals` Tất toán vay mua nhà (`DEBT_REDUCTION`), return `0.00` | `201`, `expectedAnnualReturnPercent: 0.00` (không phải null) |
| G07 | `POST /goals` Du lịch Nhật Bản (`OTHER_GOAL`), chỉ 4 field bắt buộc | `201`, `currentAllocatedAmount: 0.00`, `plannedMonthlyContribution: null`, `expectedAnnualReturnPercent: null` |

## B. Liệt kê

| # | Request | Mong đợi |
|---|---|---|
| G08 | `GET /goals` as A, chạy hai lần | `200`, 7 phần tử, thứ tự `targetDate` tăng dần rồi `name`: 2027-06-30 Quỹ dự phòng → 2028-03-31 Du lịch → 2030-12-31 Mua căn hộ → 2030-12-31 Tăng trưởng TSR → 2030-12-31 Tất toán → 2032-08-31 Học phí → 2041-12-31 Hưu trí; hai lần giống nhau |
| G09 | `GET /goals` as B | `200`, `[]` — không phải 404 |

## C. Field bắt buộc thiếu, tên không hợp lệ

| # | Request | Mong đợi |
|---|---|---|
| G10 | bỏ `name` | `400 validation-failed`, `errors` đúng một phần tử `name` / `must not be blank` |
| G11 | bỏ `type` | `type` / `must not be null` |
| G12 | bỏ `targetAmount` | `targetAmount` / `must not be null` |
| G13 | bỏ `targetDate` | `targetDate` / `must not be null` |
| G14 | `"name":"   "` | `name` / `must not be blank` |
| G15 | `name` 121 ký tự | `name` / `must be at most 120 characters` |
| G16 | `name` là một ký tự U+00A0 (non-breaking space) | `name` / `must not be blank` |
| G17 | `"name":"Quỹ\u0000dự phòng"` | `name` / `must not contain a null character` |

## D. Giá trị số

| # | Request | Mong đợi |
|---|---|---|
| G18 | `targetAmount: 0` | `targetAmount` / `must be greater than zero` |
| G19 | `targetAmount: -1000000` | `targetAmount` / `must be greater than zero` |
| G20 | `targetAmount: 270000000.123` | `targetAmount` / `must have at most 17 integer digits and 2 decimals` |
| G21 | `targetAmount: 100000000000000000.00` (18 chữ số nguyên) | `targetAmount` / `must have at most 17 integer digits and 2 decimals` — bị chặn trước khi tới `NUMERIC(19,2)` |
| G22 | `currentAllocatedAmount: -1` | `currentAllocatedAmount` / `must not be negative` |
| G23 | `currentAllocatedAmount: 300000000` với `targetAmount: 270000000` | `currentAllocatedAmount` / `must not exceed the target amount` |
| G24 | `currentAllocatedAmount == targetAmount` | `201` — đã đạt mục tiêu là trạng thái hợp lệ |
| G25 | `plannedMonthlyContribution: -5000000` | `plannedMonthlyContribution` / `must not be negative` |
| G26 | `plannedMonthlyContribution: 0` | `201`, echo `0.00` |
| G27 | `expectedAnnualReturnPercent: -0.5` | `expectedAnnualReturnPercent` / `must be between 0 and 30` |
| G28 | `expectedAnnualReturnPercent: 30.01` | `expectedAnnualReturnPercent` / `must be between 0 and 30` |
| G29 | `expectedAnnualReturnPercent: 30.00` | `201` |
| G30 | `expectedAnnualReturnPercent: 7.125` | `expectedAnnualReturnPercent` / `must have at most 3 integer digits and 2 decimals` |
| G31 | `targetAmount: -1` **và** `currentAllocatedAmount: -1` | hai phần tử `errors`, một cho mỗi field; không có lỗi thứ ba về "vượt target" |

## E. Ngày đích

| # | Request | Mong đợi |
|---|---|---|
| G32 | `targetDate: YESTERDAY` | `targetDate` / `must be after today` |
| G33 | `targetDate: TODAY` | `targetDate` / `must be after today` — design yêu cầu *sau* ngày tham chiếu |
| G34 | `targetDate: TOMORROW` | `201` |
| G35 | `"targetDate":"31/12/2030"` (sai định dạng ISO) | `400 malformed-request` — Jackson không parse được |

## F. Enum lạ và field không nhận

| # | Request | Mong đợi |
|---|---|---|
| G36 | `"type":"CRYPTO"` | `400 malformed-request`, không có `errors` |
| G37 | body có thêm `"currency":"USD"` | `201`, response `currency: "VND"` — field lạ bị Jackson bỏ qua, không đổi được loại tiền |
| G38 | body có thêm `"completionPercent": 50` | `201`, response không có field đó — output của #89 không bị nhận vào |

## G. Sửa (PUT thay thế toàn bộ)

| # | Request | Mong đợi |
|---|---|---|
| G39 | `PUT` Quỹ dự phòng: đổi `name` → "Quỹ dự phòng 9 tháng", `type` → `OTHER_GOAL`, `targetAmount` 405.000.000, allocated 120.000.000, `targetDate` 2027-12-31, contribution 18.000.000, return 2,50 | `200`, cả 7 field đổi, `updatedAt` tăng, `createdAt` giữ nguyên, `id` giữ nguyên |
| G40 | `PUT` chỉ 4 field bắt buộc | `200`, `currentAllocatedAmount: 0.00`, contribution `null`, return `null` — thay thế, không phải patch; `GET` lại xác nhận lưu đúng thế |
| G41 | `PUT` bỏ `name` | `name` / `must not be blank` |
| G42 | `PUT` allocated > target | `currentAllocatedAmount` / `must not exceed the target amount` |
| G43 | `PUT` `targetDate: YESTERDAY` (khác ngày đã lưu) | `targetDate` / `must be after today` |
| G44 | Dùng `psql` đặt `target_date` của một mục tiêu về `2026-01-31` (giả lập quá hạn); `PUT` đổi `name` nhưng giữ `targetDate: 2026-01-31` | `200` — giữ nguyên ngày đích đã lưu luôn được phép |
| G45 | Cùng mục tiêu quá hạn, `PUT` với `targetDate: 2026-02-28` (quá khứ khác) | `targetDate` / `must be after today` |
| G46 | `PUT` `"type":"CRYPTO"` | `400 malformed-request` |

## H. Ownership

| # | Request | Mong đợi |
|---|---|---|
| G47 | A `GET /goals/<A's>` | `200` |
| G48 | B `GET /goals/<A's>` | `404 not-found` |
| G49 | B `GET /goals/00000000-0000-4000-8000-000000000000` | `404 not-found`; body so với G48 chỉ khác `instance` và `correlationId` |
| G50 | B `PUT /goals/<A's>` body hợp lệ | `404`; A đọc lại thấy không đổi |
| G51 | B `DELETE /goals/<A's>` | `404`; row còn |
| G52 | B tạo "Tiền mặt dự phòng của Bích" 50.000.000 ₫ rồi `GET /goals` | `200`, đúng một phần tử, không thấy mục tiêu nào của A |

## I. Xác thực

| # | Request | Mong đợi |
|---|---|---|
| G53 | `GET /goals` không header | `401 unauthorized` |
| G54 | token hết hạn | `401`, body giống G53 trừ `correlationId` |
| G55 | token ký bằng secret khác | `401`, body giống G53 trừ `correlationId` |
| G56 | `GET /goals/<id>` không header | `401` — route thành phần không lộ tồn tại cho người vô danh |
| G57 | `GET /goal-templates` không header | `401` |

## J. Giao thức

| # | Request | Mong đợi |
|---|---|---|
| G58 | `GET /goals/not-a-uuid` | `400 malformed-request` |
| G59 | `PATCH /goals/<id>` | `405 method-not-allowed` |
| G60 | `DELETE /goals` | `405 method-not-allowed` |
| G61 | `POST /goals` với `Content-Type: text/plain` | `415 unsupported-media-type` |
| G62 | `{"name": ` (JSON cụt) | `400 malformed-request` |
| G63 | `Accept: application/xml` | `406 not-acceptable` |

## K. Xóa

| # | Request | Mong đợi |
|---|---|---|
| G64 | A `DELETE /goals/<id>` | `204`, không body |
| G65 | A `GET /goals/<id>` | `404` |
| G66 | A `DELETE /goals/<id>` lần hai | `404` |
| G67 | A `GET /goals` | `200`, ít đi một phần tử |
| G68 | 4 `DELETE` đồng thời cùng một id, lặp 10 vòng | mỗi vòng đúng 1×`204`, 3×`404`; **không có 500** |

## L. Templates

| # | Request | Mong đợi |
|---|---|---|
| G69 | `GET /goal-templates` as A | `200`, đúng 7 phần tử theo thứ tự `EMERGENCY_FUND`(3.00, 18, `TARGET_FROM_SIX_MONTHS_OF_EXPENSES`), `HOME_PURCHASE`(6.00, 60, `NONE`), `EDUCATION`(5.00, 72, `NONE`), `RETIREMENT`(8.00, 180, `NONE`), `NET_WORTH_GROWTH`(7.00, 48, `ALLOCATED_FROM_CURRENT_NET_WORTH`), `DEBT_REDUCTION`(0.00, 48, `TARGET_FROM_TOTAL_LIABILITIES`), `OTHER_GOAL`(0.00, 24, `NONE`) |
| G70 | `POST /goal-templates` | `405 method-not-allowed` — chỉ đọc |

## M. Kiểm tra trực tiếp trong PostgreSQL

| # | Lệnh | Mong đợi |
|---|---|---|
| D01 | `information_schema.columns` của `wealth.goals` | `target_amount`, `current_allocated_amount`, `planned_monthly_contribution` là `numeric(19,2)`; `expected_annual_return_percent` là `numeric(5,2)`; không có `double precision` |
| D02 | `SELECT name, target_amount FROM wealth.goals ORDER BY target_amount DESC LIMIT 2` | `15000000000.00` nguyên vẹn, không exponent, không làm tròn |
| D03 | INSERT `type = 'CRYPTO'` | `goals_type_is_known` |
| D04 | INSERT `target_amount = 0` | `goals_target_amount_is_positive` |
| D05 | INSERT `current_allocated_amount = -1` | `goals_allocated_amount_is_not_negative` |
| D06 | INSERT allocated > target | `goals_allocated_amount_within_target` |
| D07 | INSERT `expected_annual_return_percent = 31` | `goals_expected_return_within_bounds` |
| D08 | INSERT `currency = 'USD'` | `goals_currency_is_vnd` |
| D09 | INSERT `name = '   '` | `goals_name_is_present_and_bounded` |
| D10 | INSERT `planned_monthly_contribution = -1` | `goals_contribution_is_not_negative` |
| D11 | `SELECT user_id, count(*) GROUP BY user_id` | số dòng mỗi user khớp với danh sách qua API; `SELECT count(*)` không tăng sau D03–D10 |

## Tổng

70 case HTTP (G01–G70) và 11 case PostgreSQL (D01–D11). Phase 5 phải ghi status và body thật cho từng
case; case nào fail rồi fix phải ghi là đã fail, không được lặng lẽ sửa danh sách.
