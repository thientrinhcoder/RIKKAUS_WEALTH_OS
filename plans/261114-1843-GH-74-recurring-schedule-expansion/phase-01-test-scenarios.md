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

Viết ra toàn bộ hành vi phải kiểm chứng **trước** khi có dòng code nào, theo cách hiểu TDD đã chốt với
Product Owner: scenario trước, không unit test. Mỗi scenario trở thành một case trong
`docs/manual-test-guide-cash-flow-occurrences.md` và một dòng có status/body thật trong evidence report.
Số thứ tự ở đây là số case trong hai tài liệu đó.

Các ngày kỳ vọng dưới đây tính tay từ năm quy tắc biên của design #72 trước khi code; khi chạy, kết quả API
được so với bảng này chứ không phải ngược lại.

## Dữ liệu dùng chung

User A `0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93` (An Nguyễn) và user B `7b41e590-3c82-4f16-b0d7-59e1c4f3a842`
(Bích Trần), chèn thẳng vào `wealth.users`; token HS256 mint bằng secret của profile `local`.

Entry của A, tạo qua `POST /api/v1/cash-flow-entries` (không chèn thẳng):

| Tên | Loại / danh mục | Số tiền | Lịch | Kiểm chứng điều gì |
|---|---|---|---|---|
| E1 `Lương` | `INCOME` `SALARY` | `45.000.000 ₫` | `MONTHLY` từ 31/01/2026, không kết thúc | ngày 31 qua tháng 30 ngày, tháng 2 nhuận và không nhuận |
| E2 `Tiền thuê nhà` | `EXPENSE` `HOUSING` | `18.000.000 ₫` | `MONTHLY` từ 30/12/2025 đến 30/06/2026 | `endDate` trùng ngày kỳ; ngày 30 qua tháng 2 |
| E3 `Phí gửi xe chung cư` | `EXPENSE` `LIVING_COSTS` | `1.200.000 ₫` | `MONTHLY` từ 15/01/2026 đến 14/06/2026 | `endDate` ngay trước ngày kỳ |
| E4 `Học phí` | `EXPENSE` `EDUCATION` | `12.500.000 ₫` | `QUARTERLY` từ 30/11/2025 | quý neo ngày 30 qua tháng 2 |
| E5 `Thưởng Tết` | `INCOME` `SALARY` | `60.000.000 ₫` | `YEARLY` từ 13/02/2026 | năm thường |
| E6 `Phí bảo hiểm nhân thọ` | `EXPENSE` `INSURANCE` | `24.000.000 ₫` | `YEARLY` từ 29/02/2028 | 29/02 qua năm không nhuận rồi quay lại |
| E7 `Phí bảo trì chung cư` | `EXPENSE` `TAX_AND_FEES` | `3.200.000 ₫` | `QUARTERLY` từ 15/03/2026 đến 15/03/2026 | chuỗi một kỳ |
| E8 `Khám sức khỏe định kỳ` | `EXPENSE` `HEALTHCARE` | `4.650.000 ₫` | `ONE_TIME` 31/03/2026 | một lần, nằm đúng `to` của quý 1 |
| E9 `Cổ tức VNM` | `INCOME` `DIVIDEND` | `150 USD` | `ONE_TIME` 01/04/2026 | một lần, nằm đúng `from` của quý 2; USD không quy đổi |

Entry của B: EB `Lương của Bích`, `SALARY`, `30.000.000 ₫`, `MONTHLY` từ 31/01/2026.

Đáp án tổng của A cho cả năm 2026 (`2026-01-01..2026-12-31`), tính trước: E1 12 kỳ, E2 6, E3 5, E4 4, E5 1,
E6 0, E7 1, E8 1, E9 1 → **31 kỳ**. Quý 1 là 13 kỳ, quý 2 là 10 kỳ, nửa đầu năm là 23.

Ký hiệu kỳ: `ngày sN` là kỳ thứ N (`sequence`), thêm `dời` khi `shiftedToMonthEnd: true`.

## A. Ngày cuối tháng, hàng tháng (case 1–4)

| # | Request (user A) | Kỳ vọng cho E1 |
|---|---|---|
| 1 | `from=2026-01-01&to=2026-04-30` | `200`; đúng 4 kỳ: `2026-01-31 s1`, `2026-02-28 s2 dời`, `2026-03-31 s3`, `2026-04-30 s4 dời`. Tháng 3 quay về 31, không dính 28 |
| 2 | `from=2027-12-01&to=2028-03-31` (năm nhuận) | `2027-12-31 s24`, `2028-01-31 s25`, `2028-02-29 s26 dời`, `2028-03-31 s27` |
| 3 | `from=2026-02-01&to=2026-02-28` | đúng một kỳ `2026-02-28 s2 dời`: kỳ dời nằm trong tháng 2, không sang tháng 3 |
| 4 | `from=2026-03-01&to=2026-03-31` | đúng một kỳ `2026-03-31 s3`; không có kỳ nào ngày 01/03–03/03 |

## B. Hàng quý và hàng năm (case 5–9)

| # | Request (user A) | Kỳ vọng |
|---|---|---|
| 5 | `from=2025-11-01&to=2026-11-30`, E4 | `2025-11-30 s1`, `2026-02-28 s2 dời`, `2026-05-30 s3`, `2026-08-30 s4`, `2026-11-30 s5` |
| 6 | `from=2026-12-01&to=2027-05-31`, E4 | `2027-02-28 s6 dời`, `2027-05-30 s7` |
| 7 | `from=2026-01-01&to=2027-12-31`, E5 | `2026-02-13 s1`, `2027-02-13 s2`. Đây cũng là khoảng dài nhất được phép (24 tháng): `200` |
| 8 | `from=2028-02-01&to=2030-01-31`, E6 | `2028-02-29 s1`, `2029-02-28 s2 dời` |
| 9 | `from=2031-03-01&to=2033-02-28`, E6 | `2032-02-29 s5` (không dời: năm nhuận quay lại ngày neo), `2033-02-28 s6 dời` |

## C. `endDate` (case 10–14)

| # | Request (user A) | Kỳ vọng |
|---|---|---|
| 10 | `from=2026-01-01&to=2026-12-31`, E2 | 6 kỳ: `01-30 s2`, `02-28 s3 dời`, `03-30 s4`, `04-30 s5`, `05-30 s6`, `06-30 s7`. Kỳ trùng `endDate` có mặt, không kỳ nào sau đó |
| 11 | `from=2026-06-30&to=2026-07-31`, E2 | đúng một kỳ `2026-06-30 s7` |
| 12 | `from=2026-01-01&to=2026-12-31`, E3 | 5 kỳ `01-15 s1` … `05-15 s5`; **không** có `06-15` vì `endDate` 14/06 |
| 13 | `from=2026-01-01&to=2026-12-31`, E7 | đúng một kỳ `2026-03-15 s1`, không có `06-15`, `09-15` |
| 14 | `from=2027-01-01&to=2028-02-28`, E6 | không có kỳ nào: `effectiveDate` 29/02/2028 sau `to` |

## D. Biên của khoảng truy vấn (case 15–21)

| # | Request (user A) | Kỳ vọng |
|---|---|---|
| 15 | `from=2026-01-31&to=2026-02-27` (`from` = `effectiveDate` E1) | E1 có đúng `2026-01-31 s1`: `from` tính vào; `02-28` ngoài `to` |
| 16 | `from=2026-01-01&to=2026-03-31` (`to` = kỳ cuối) | E1 có `03-31 s3` là kỳ cuối: `to` tính vào; tổng 13 kỳ |
| 17 | `from=2026-02-01&to=2026-03-30` | E1 chỉ có `2026-02-28 s2 dời`: `01-31` trước `from`, `03-31` sau `to` |
| 18 | quý 1 `2026-01-01..2026-03-31` và quý 2 `2026-04-01..2026-06-30`, so với `2026-01-01..2026-06-30` | 13 + 10 = 23; hợp hai tập (`sourceEntryId`, `occurrenceDate`) bằng đúng tập nửa năm; giao rỗng. E8 (31/03) chỉ ở quý 1, E9 (01/04) chỉ ở quý 2 |
| 19 | 12 request tháng `2026-MM-01..cuối tháng` so với `2026-01-01..2026-12-31` | tổng 12 tháng = 31 = cả năm; không cặp (`sourceEntryId`, `occurrenceDate`) nào xuất hiện ở hai tháng; mỗi kỳ nằm trong tháng của chính ngày nó |
| 20 | `from=2026-02-28&to=2026-02-28` (một ngày) | đúng 3 kỳ: E1 `s2 dời`, E2 `s3 dời`, E4 `s2 dời` |
| 21 | `from=2020-01-01&to=2020-12-31` | `200 []` |

## E. Một lần (case 22–23)

| # | Request (user A) | Kỳ vọng |
|---|---|---|
| 22 | `from=2026-03-01&to=2026-03-31` | E8 có đúng một kỳ `2026-03-31 s1`, `shiftedToMonthEnd: false`, `recurrence: null` |
| 23 | `from=2026-04-01&to=2026-04-30` | E8 vắng mặt; E9 có `2026-04-01 s1` với `amount: 150.00`, `currency: USD`, không có trường VND nào |

## F. Hình dạng, truy nguồn, xác định (case 24–27)

| # | Request (user A) | Kỳ vọng |
|---|---|---|
| 24 | `from=2026-01-01&to=2026-01-31`, kỳ của E1 | đúng 9 member: `sourceEntryId` = id E1, `occurrenceDate: 2026-01-31`, `sequence: 1`, `shiftedToMonthEnd: false`, `entryType: INCOME`, `category: SALARY`, `amount: 45000000.00`, `currency: VND`, `recurrence: MONTHLY`. Không có `userId`, `notes`, id kỳ |
| 25 | `GET /cash-flow-entries/{sourceEntryId}` lấy từ case 24 | `200`, cùng `amount`, `category`, `entryType`: kỳ truy ngược được về entry gốc |
| 26 | cả năm 2026 | mảng sắp theo `occurrenceDate` tăng dần, cùng ngày thì `sourceEntryId` tăng dần |
| 27 | cả năm 2026 gọi hai lần | body giống hệt từng byte (so sha256) |

## G. Ownership (case 28–30)

| # | Request | Kỳ vọng |
|---|---|---|
| 28 | B, cả năm 2026, khi B chưa có entry | `200 []` dù A có 31 kỳ trong khoảng đó |
| 29 | B tạo EB rồi gọi cả năm 2026 | 12 kỳ, mọi kỳ `sourceEntryId` = EB, `amount: 30000000.00`; không id nào của A |
| 30 | A gọi lại cả năm 2026 | vẫn 31 kỳ, không kỳ nào mang id EB |

## H. Tính khi đọc (case 31–32)

| # | Thao tác | Kỳ vọng |
|---|---|---|
| 31 | `PUT` E2 với `endDate: 2026-04-30` rồi gọi cả năm 2026 | E2 còn 4 kỳ, kỳ cuối `04-30 s5`; tổng 29 |
| 32 | `DELETE` E3 rồi gọi cả năm 2026 | không còn kỳ nào của E3; tổng 24 |

## I. Input sai (case 33–43)

Mọi case là `400` với `type: urn:rikkaus:problem:validation-failed` và `errors` đúng như cột kỳ vọng; body
không chứa lại giá trị đã gửi.

| # | Query | `errors` kỳ vọng |
|---|---|---|
| 33 | `to=2026-03-31` (thiếu `from`) | `[{from, "must be present"}]` |
| 34 | `from=2026-01-01` (thiếu `to`) | `[{to, "must be present"}]` |
| 35 | không có query | hai entry, `from` rồi `to`, cùng `must be present` |
| 36 | `from=&to=2026-03-31` | `[{from, "must be present"}]` |
| 37 | `from=2026-04-01&to=2026-03-31` (đảo) | `[{to, "must not be before from"}]` |
| 38 | `from=31/01/2026&to=2026-03-31` | `[{from, "must be a calendar date in the form YYYY-MM-DD"}]`; body không chứa `31/01/2026` |
| 39 | `from=2026-01-01&to=2026-02-29` (2026 không nhuận) | `[{to, "must be a calendar date in the form YYYY-MM-DD"}]` |
| 40 | `from=2026-1-5&to=2026-03-31` | lỗi định dạng trên `from` |
| 41 | `from=2026-01-01T00:00:00%2B07:00&to=2026-03-31` | lỗi định dạng trên `from`: tham số là ngày, không nhận giờ hay múi giờ |
| 42 | `from=2026-01-01&to=2028-01-01` (24 tháng + 1 ngày) | `[{to, "must be less than 24 months after from"}]` |
| 43 | `from=abc&to=` | hai entry: `from` định dạng, `to` `must be present`; không có lỗi thứ tự |

## J. Xác thực, method, media type (case 44–48)

| # | Request | Kỳ vọng |
|---|---|---|
| 44 | không header `Authorization` | `401` `unauthorized` |
| 45 | token hết hạn | `401`, body giống 44 trừ `correlationId` |
| 46 | token ký bằng secret khác | `401`, như 45 |
| 47 | `POST` và `DELETE /cash-flow-occurrences` | `405` `method-not-allowed`, `Allow: GET` |
| 48 | `Accept: application/xml` | `406` `not-acceptable` |

## K. Múi giờ, contract, schema (case 49–51)

| # | Kiểm tra | Kỳ vọng |
|---|---|---|
| 49 | Ghi sha256 body cả năm 2026 của A; khởi động lại API với `-Duser.timezone=Pacific/Pago_Pago` (UTC−11); gọi lại | sha256 giống hệt: kết quả không phụ thuộc múi giờ máy chủ |
| 50 | `services/api/openapi/openapi.json` | path `/api/v1/cash-flow-occurrences` chỉ có `get`, `from`/`to` `required: true`, `format: date`, có ví dụ |
| 51 | `\dt wealth.*` và `flyway_schema_history` | không bảng mới, version cao nhất vẫn `9` |

Tổng: **51 case**.
