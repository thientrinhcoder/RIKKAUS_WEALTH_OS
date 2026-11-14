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
Owner). Mỗi scenario là một request hoặc một lệnh cụ thể, dữ liệu Việt Nam thật, kết quả mong đợi đủ rõ
để người khác chạy và chấm được. Phase 5 chạy đúng danh sách này và ghi kết quả thật.

Quy ước: `A` (An Nguyen, `an.nguyen@example.com`) và `B` (Bich Tran, `bich.tran@example.com`) là hai
user insert thẳng vào `wealth.users`. Mọi lỗi là `application/problem+json`; với `validation-failed`,
assertion là mảng `errors` và field nó nêu, không phải mã 400 đơn thuần.

## Dữ liệu nhạy cảm dùng làm "mồi" cho phần observability

Những chuỗi này được cố ý gửi vào API qua header và body. Phần O kết thúc bằng việc grep **toàn bộ**
stdout + stderr của tiến trình API cho từng chuỗi; mỗi chuỗi phải cho 0 dòng.

| Mồi | Giá trị | Đi vào qua |
|---|---|---|
| M1 | `ya29.fake-token-issue110` | body (notes tài sản, message feedback), header `Authorization: Bearer`, cookie |
| M2 | JWT access token thật của A (chuỗi `eyJ...`) | header `Authorization` mọi request của A |
| M3 | `an.nguyen@example.com` | cột `email` trong DB, body feedback |
| M4 | `4.200.000.000` và `4200000000` | body tài sản và feedback |
| M5 | `Căn hộ Thảo Điền` (và `Thảo Điền`) | tên tài sản, message feedback |
| M6 | `session-cookie-issue110` | header `Cookie` |

## Dữ liệu feedback xuyên suốt

| # | `screen` | `category` | `message` |
|---|---|---|---|
| D1 | `catalog/assets` | `BUG` | Màn hình danh mục tài sản không hiển thị tỷ giá cập nhật 12/09/2026 sau khi lưu. |
| D2 | `dashboard` | (bỏ trống) | Mong có biểu đồ dòng tiền theo quý. |
| D3 | `goals/new` | `IDEA` | Nên gợi ý số tiền đóng góp hằng tháng khi chọn mẫu Mua căn hộ. |
| D4 | `settings/exchange-rate` | `QUESTION` | Tỷ giá 1 USD = 25.100 VND lấy từ đâu? |
| D5 | `catalog/liabilities` | `OTHER` | Ứng dụng dễ dùng, cảm ơn đội phát triển. |

## F. Feedback — tạo và liệt kê

| # | Request | Mong đợi |
|---|---|---|
| F01 | `POST /feedback` D1 as A | `201`, body có `id` (UUID), `screen`, `category: "BUG"`, `message` nguyên văn, `createdAt` ISO-8601; **không** có `userId`, **không** có header `Location` |
| F02 | `POST /feedback` D2 as A (không gửi `category`) | `201`, `category: null` |
| F03 | `POST /feedback` D3, D4, D5 as A | ba lần `201`, category `IDEA`, `QUESTION`, `OTHER` |
| F04 | `POST /feedback` as A, message `"  Nút Lưu bị che bởi bàn phím trên iPhone 13.  "` | `201`, message trả về đã bỏ khoảng trắng hai đầu |
| F05 | `GET /feedback` as A, chạy hai lần | `200`, 6 phần tử, mới nhất trước (F04 → D5 → D4 → D3 → D2 → D1), hai lần giống nhau |
| F06 | `GET /feedback` as B | `200`, `[]` (không phải 404) |
| F07 | `POST /feedback` as B (`dashboard`, `BUG`, "Biểu đồ tài sản ròng bị trống khi chưa có công nợ."), rồi `GET` as A và as B | B thấy đúng 1 phần tử của mình; A vẫn 6 phần tử, không có feedback của B |
| F08 | `POST /feedback` as A kèm `"userId": "<id của B>"` trong body | `201`; feedback thuộc A (A có 7, B vẫn 1) — client không thể gán owner |
| F09 | `POST /feedback` as A kèm `"createdAt": "2020-01-01T00:00:00Z"` và `"id": "<uuid tự chọn>"` | `201`; `createdAt` là giờ server, `id` khác uuid gửi lên |
| F10 | message dài đúng 2000 ký tự (chuỗi tiếng Việt có dấu) | `201` (biên trên) |
| F11 | screen dài đúng 64 ký tự hợp lệ | `201` (biên trên) |

## V. Feedback — validation

| # | Request | Mong đợi |
|---|---|---|
| F12 | thiếu `screen` | `400 validation-failed`, `errors` đúng một phần tử `screen` / `must not be blank` |
| F13 | thiếu `message` | `message` / `must not be blank` |
| F14 | `message: ""` | `message` / `must not be blank` |
| F15 | `message: "   "` | `message` / `must not be blank` |
| F16 | `message` chỉ gồm `U+00A0` (NBSP) | `message` / `must not be blank` |
| F17 | `message` 2001 ký tự | `message` / `must be at most 2000 characters` |
| F18 | `message: "Lỗi\u0000màn hình"` | `400 validation-failed`, `message` / `must not contain a null character`; **không** 500 |
| F19 | `screen: "Màn hình tài sản"` (có dấu, khoảng trắng) | `screen` / thông báo pattern |
| F20 | `screen` 65 ký tự | `screen` / thông báo pattern hoặc độ dài |
| F21 | `screen: "dashboard?token=ya29.fake-token-issue110"` | `screen` / thông báo pattern; body lỗi **không** echo giá trị |
| F22 | `screen: "Dashboard"` (chữ hoa) | `screen` / thông báo pattern |
| F23 | `category: "PRAISE"` | `400 malformed-request` (Jackson không dựng được enum) |
| F24 | `category: "bug"` (chữ thường) | `400 malformed-request` |
| F25 | `screen` sai và `message` rỗng cùng lúc | `400`, `errors` có đúng 2 phần tử, `screen` và `message` |

## H. Feedback — xác thực, method, định dạng

| # | Request | Mong đợi |
|---|---|---|
| F26 | `POST /feedback` không header `Authorization` | `401 unauthorized` |
| F27 | token hết hạn | `401`, body giống F26 trừ `correlationId` |
| F28 | token sai chữ ký | `401`, body giống F26 trừ `correlationId` |
| F29 | `GET /feedback` không header | `401` |
| F30 | `PUT /feedback`, `DELETE /feedback`, `PATCH /feedback` as A | `405 method-not-allowed`, header `Allow: GET,POST` |
| F31 | `GET /feedback/<id của F01>` as A | `404 not-found` (không có route đọc/sửa/xóa từng feedback) |
| F32 | `DELETE /feedback/<id của F01>` as A | `404 not-found`; row vẫn còn |
| F33 | `POST` `Content-Type: text/plain` | `415 unsupported-media-type` |
| F34 | `POST` JSON cụt `{"screen":"dashboard","message":"Lỗi` | `400 malformed-request` |
| F35 | `POST` body `[]` | `400 malformed-request` |
| F36 | `GET` `Accept: application/xml` | `406 not-acceptable` |

## P. Feedback — kiểm tra trực tiếp PostgreSQL

| # | Lệnh | Mong đợi |
|---|---|---|
| P01 | `\d wealth.feedback_entries` | cột `id uuid`, `user_id uuid not null` FK `wealth.users` `ON DELETE CASCADE`, `screen text not null`, `category text`, `message text not null`, `created_at timestamptz not null`; index `(user_id, created_at DESC)` |
| P02 | `SELECT user_id, count(*) ... GROUP BY user_id` | A = 9 (F01–F04 sáu, F08, F09, F10, F11 → đếm chính xác khi chạy), B = 1 |
| P03 | message F04 trong DB | đã bỏ khoảng trắng hai đầu |
| P04 | `INSERT` screen `'Màn hình'` | `check_violation` `feedback_entries_screen_is_a_code` |
| P05 | `INSERT` message `'   '` | `check_violation` `feedback_entries_message_is_not_blank` |
| P06 | `INSERT` message 2001 ký tự | `check_violation` `feedback_entries_message_is_bounded` |
| P07 | `INSERT` category `'PRAISE'` | `check_violation` `feedback_entries_category_is_known` |
| P08 | `INSERT` user_id không tồn tại | `foreign_key_violation` |

## O. Observability — chạy với profile `local` (log console pattern)

API chạy `--spring.profiles.active=local`, stdout+stderr ghi vào một file. Mỗi request gửi header
`X-Correlation-Id` riêng (`issue110-local-oNN`) để tìm dòng log tương ứng.

| # | Request | Mong đợi ở response | Mong đợi ở log |
|---|---|---|---|
| O01 | `POST /assets` as A, tên `Căn hộ Thảo Điền`, giá trị `4200000000`, notes `ya29.fake-token-issue110` | `201` | đúng 1 dòng `request completed` có correlationId, `userId` = UUID của A, `POST /api/v1/assets 201`, thời gian ms |
| O02 | `POST /assets` as A cùng tên nhưng giá trị âm | `400 validation-failed` | dòng request `400`; không giá trị nào của body |
| O03 | `GET /assets/<uuid ngẫu nhiên>` as A | `404 not-found` | route ghi là template `/api/v1/assets/{assetId}`, **không** phải UUID thật |
| O04 | `POST /feedback` as A, message "Căn hộ Thảo Điền trị giá 4.200.000.000 ₫, token ya29.fake-token-issue110, email an.nguyen@example.com" | `201` | dòng request `201`; không mồi nào |
| O05 | 500 có kiểm soát: thêm tạm CHECK `NOT VALID` trên `wealth.feedback_entries` từ chối message chứa `Thảo Điền`, rồi gửi lại O04 | `500 internal-error`, body không lộ gì | một dòng `ERROR` có tên lớp exception và stack frame, **không** có `Failing row contains`, không giá trị nào của row; dòng request `500`. Gỡ constraint sau đó |
| O06 | `POST /feedback` JSON cụt chứa `ya29.fake-token-issue110` | `400 malformed-request` | không mồi |
| O07 | `GET /feedback` với `Authorization: Bearer ya29.fake-token-issue110` | `401` | dòng request `401`, không token, không userId |
| O08 | `GET /feedback` as A với `Cookie: session=session-cookie-issue110` | `200` | không giá trị cookie |
| O09 | `GET /feedback` không token | `401` | dòng request không có userId (hiển thị dạng vắng mặt) |
| O10 | `GET /actuator/health` | `200` | không có dòng INFO nào cho health probe (chỉ DEBUG) |
| O11 | `GET /feedback` với `X-Correlation-Id: bad id` (có khoảng trắng) | `200`, header trả về là UUID mới | semantics correlation id không đổi; log dùng UUID mới |
| O12 | `grep -c` toàn bộ log cho M1–M6 | — | mỗi mồi **0** dòng |
| O13 | `grep` dòng `request completed` | — | mỗi request O01–O11 (trừ O10) có đúng 1 dòng, đủ correlationId, userId (khi có), method, route, status, duration |

## J. Observability — chạy không profile `local` (log JSON)

API chạy không profile, `RIKKAUS_JWT_SECRET` đặt qua env (một secret riêng cho lần chạy này, token A/B
mint lại bằng nó), cùng database. Gửi lại O01–O11 với correlation id `issue110-json-oNN`.

| # | Kiểm tra | Mong đợi |
|---|---|---|
| J01 | O01–O11 lặp lại | cùng status như phần O |
| J02 | `jq -c . log.stdout` trên toàn bộ stdout | mọi dòng parse được; số dòng parse = số dòng file |
| J03 | dòng `request completed` | field `correlationId`, `userId`, `httpMethod`, `httpRoute`, `httpStatus`, `durationMs`, `level`, `logger_name`, `@timestamp` |
| J04 | dòng `ERROR` của 500 có kiểm soát | `stack_trace` chỉ có tên lớp và frame, không message exception, không `Failing row contains` |
| J05 | grep M1–M6 trên stdout + stderr | 0 dòng mỗi mồi |
| J06 | so sánh một request giữa hai lần chạy | cùng thông tin (correlationId, userId, method, route, status, duration) ở hai định dạng |
| J07 | khởi động | không banner ASCII trên stdout làm hỏng JSON; Flyway và context start có trong log JSON |
