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
Owner). Mỗi case là một request cụ thể với dữ liệu Việt Nam thật và kết quả mong đợi đủ rõ để người khác
chạy và chấm được. Phase 5 chạy đúng danh sách này và ghi kết quả thật.

Quy ước: `A`, `B`, `C` là ba user mới; `TODAY` là ngày theo `Asia/Ho_Chi_Minh`. "Bước n = x" nghĩa là
phần tử `steps[n-1].done` trong `GET /onboarding`. Mọi lỗi là `application/problem+json`; với
`validation-failed` assertion là mảng `errors` và field nó nêu.

## Dữ liệu dùng xuyên suốt

| Record | Slice | Payload chính |
|---|---|---|
| Tỷ giá | settings | `1 USD = 25.100 VND`, hiệu lực `TODAY` |
| Tài khoản Vietcombank | assets `BANK_ACCOUNT` | 85.000.000 ₫, thanh khoản `HIGH` |
| Tiền mặt dự phòng | assets `CASH` | 12.500.000 ₫ |
| Căn hộ Thảo Điền | assets `REAL_ESTATE` | 4.200.000.000 ₫, thanh khoản `LOW` |
| Tiền gửi có kỳ hạn ACB (user B) | assets `TERM_DEPOSIT` | 300.000.000 ₫ |
| Vay mua nhà Vietcombank | liabilities `BANK_LOAN` | dư nợ 2.400.000.000 ₫, lãi 8,5% |
| Thưởng dự án tháng 9 | cash-flow `INCOME`/`BUSINESS_INCOME` `ONE_TIME` | 26.200.000 ₫ |
| Lương tháng | cash-flow `INCOME`/`SALARY` `RECURRING` `MONTHLY` | 45.000.000 ₫ |
| Quỹ dự phòng 6 tháng | goals `EMERGENCY_FUND` | 270.000.000 ₫, hạn 30/06/2027 |

## A. User mới (A)

| # | Request | Mong đợi |
|---|---|---|
| O01 | `GET /onboarding` as A, chưa có record nào | `200`, `status: IN_PROGRESS`, `currentStep: 1`, 7 phần tử `step` 1..7 với `key` đúng thứ tự backbone, tất cả `done: false`, ba timestamp `null` |
| O02 | `psql`: `SELECT count(*) FROM wealth.onboarding_progress WHERE user_id = A` | `0` — `GET` không tạo dòng |
| O03 | `GET /onboarding` as A lần hai | body byte-identical với O01 |

## B. Bước chuyển sang xong theo record thật (A)

Sau mỗi request tạo record, `GET /onboarding` và kiểm tra đúng bước đó đổi, các bước khác giữ nguyên.

| # | Request | Mong đợi |
|---|---|---|
| O04 | `GET /settings/exchange-rate` (tạo dòng settings với rate `null`), rồi `GET /onboarding` | bước 1 = `false` — dòng settings tồn tại chưa phải là đã khai báo |
| O05 | `PUT /settings/exchange-rate` `25100.000000`, `TODAY` | `200`; bước 1 = `true` |
| O06 | `POST /assets` Căn hộ Thảo Điền `REAL_ESTATE` 4.200.000.000 ₫ | `201`; bước 3 = `true`, bước 2 vẫn `false` (thứ tự không quan trọng, category tách đúng) |
| O07 | `POST /assets` Tài khoản Vietcombank `BANK_ACCOUNT` 85.000.000 ₫ | `201`; bước 2 = `true` |
| O08 | `POST /liabilities` Vay mua nhà Vietcombank `BANK_LOAN` 2.400.000.000 ₫ | `201`; bước 4 = `true` |
| O09 | `POST /cash-flow-entries` Thưởng dự án `ONE_TIME` 26.200.000 ₫ | `201`; bước 5 vẫn `false` |
| O10 | `POST /cash-flow-entries` Lương tháng `RECURRING` `MONTHLY` 45.000.000 ₫ | `201`; bước 5 = `true` |
| O11 | `POST /goals` Quỹ dự phòng 6 tháng | `201`; bước 7 = `true`; bước 6 vẫn `false` (không có record nào làm bước 6 xong) |
| O12 | Body của `GET /onboarding` lúc này | không chứa `4200000000`, `85000000`, `2400000000`, `Vietcombank`, `Thảo Điền`, `Lương` — không có số tiền hay tên |
| O13 | `psql` count dòng onboarding của A | vẫn `0` — tạo record ở slice khác không ghi tiến độ |

## C. Xóa record làm bước quay lại chưa xong (A)

| # | Request | Mong đợi |
|---|---|---|
| O14 | `DELETE /assets/{Vietcombank}` | `204`; bước 2 = `false`, bước 3 vẫn `true` |
| O15 | `POST /assets` Tiền mặt dự phòng `CASH` 12.500.000 ₫ | `201`; bước 2 = `true` (CASH cũng tính) |
| O16 | `DELETE /liabilities/{vay}` | `204`; bước 4 = `false` |
| O17 | `DELETE /cash-flow-entries/{lương}` (khoản `ONE_TIME` còn lại) | `204`; bước 5 = `false` |
| O18 | `DELETE /goals/{quỹ}` | `204`; bước 7 = `false` |
| O19 | `DELETE /assets/{Thảo Điền}` | `204`; bước 3 = `false`, bước 2 vẫn `true` |

Bước 1 không có case quay lại: settings không có route xóa tỷ giá.

## D. Ghi tiến độ (A)

| # | Request | Mong đợi |
|---|---|---|
| O20 | `PATCH /onboarding` `{"currentStep":3}` | `200`, `currentStep: 3`, `status: IN_PROGRESS`, `steps` vẫn suy ra từ record |
| O21 | `psql` dòng của A | đúng 1 dòng, `current_step = 3`, ba timestamp tiến độ `NULL` |
| O22 | `GET /onboarding` | `currentStep: 3` — resume đúng bước |
| O23 | `PATCH` `{"currentStep":0}` | `400 validation-failed`, `errors` đúng một phần tử `currentStep`; `psql` vẫn `3` |
| O24 | `PATCH` `{"currentStep":8}` | như O23 |
| O25 | `PATCH` `{"currentStep":-1}` | như O23 |
| O26 | `PATCH` `{"currentStep":"ba"}` | `400 malformed-request`, không có `errors` |
| O27 | `PATCH` `{"currentStep":2.5}` | `400` (không được âm thầm lưu thành `2`); `psql` vẫn `3` |
| O28 | `PATCH` `{"dashboardReviewed":true}` | `200`, bước 6 = `true`, `dashboardReviewedAt` có giá trị |
| O29 | `PATCH` `{"dashboardReviewed":true}` lần hai | `dashboardReviewedAt` giữ nguyên giá trị O28 |
| O30 | `PATCH` `{"dashboardReviewed":false}` | bước 6 = `false`, `dashboardReviewedAt: null` |
| O31 | `PATCH` `{"currentStep":7,"dashboardReviewed":true,"status":"COMPLETED"}` | `200`, cả ba áp dụng; `status: COMPLETED`, `completedAt` có giá trị, `dismissedAt: null`; bước 2 vẫn `true`, các bước khác theo record (không bị ép `true`) |
| O32 | `GET /onboarding` | giống O31 |
| O33 | `PATCH` `{"status":"COMPLETED"}` lần hai | `completedAt` giữ nguyên |
| O34 | `PATCH` `{"status":"DISMISSED"}` | `status: DISMISSED`, `dismissedAt` có giá trị, `completedAt: null` |
| O35 | `PATCH` `{"status":"FINISHED"}` | `400 malformed-request`; trạng thái không đổi |
| O36 | `PATCH` `{"currentStep":null}` | `200`, không đổi gì (`null` = giữ nguyên) |

## E. Bỏ qua rồi quay lại, body rỗng (B)

| # | Request | Mong đợi |
|---|---|---|
| O37 | `PATCH /onboarding` as B `{}` | `200`, trạng thái mặc định như O01; `psql` B có `0` dòng |
| O38 | `GET /onboarding` as B sau khi A đã có tiến độ và record | B: `currentStep: 1`, `IN_PROGRESS`, mọi bước `false` trừ những gì B tự tạo — không thấy gì của A |
| O39 | `POST /assets` as B Tiền gửi có kỳ hạn ACB `TERM_DEPOSIT` 300.000.000 ₫ | B: bước 3 = `true`, bước 2 = `false` (quyết định sổ tiết kiệm thuộc tài sản lớn) |
| O40 | `PATCH` as B `{"status":"DISMISSED"}` | `DISMISSED`, `dismissedAt` có giá trị |
| O41 | `PATCH` as B `{"status":"IN_PROGRESS"}` | `IN_PROGRESS`, `completedAt` và `dismissedAt` đều `null`, `currentStep` giữ nguyên |
| O42 | `PATCH` as B `{"userId":"<A>","currentStep":4}` | `200`, B có `currentStep: 4`; `psql` A không đổi — field lạ bị bỏ qua, body không chọn được owner |
| O43 | `psql` hai dòng A và B | mỗi user đúng giá trị của mình |

## F. Xác thực và giao thức

| # | Request | Mong đợi |
|---|---|---|
| O44 | `GET /onboarding` không header | `401 unauthorized` |
| O45 | `GET` với token hết hạn | `401`, body giống O44 trừ `correlationId` |
| O46 | `GET` với token sai chữ ký | `401`, như O45 |
| O47 | `PATCH` không header, body `{"currentStep":9}` | `401`, không phải `400` |
| O48 | `PUT /onboarding` as A | `405 method-not-allowed` |
| O49 | `POST` và `DELETE /onboarding` as A | `405` cả hai |
| O50 | `PATCH` `Content-Type: text/plain` | `415` problem+json |
| O51 | `PATCH` body cụt `{"currentStep":` | `400 malformed-request` |
| O52 | `GET` với `Accept: application/xml` | `406 not-acceptable` |
| O53 | 8 `PATCH {"currentStep":2}` song song as C (chưa có dòng) | tất cả `200`, không có 500; `psql` C đúng 1 dòng `current_step = 2` |

## G. PostgreSQL

| # | Lệnh | Mong đợi |
|---|---|---|
| O54 | Log khởi động | Flyway áp dụng `11 - onboarding progress`; context lên (`ddl-auto: validate` khớp entity) |
| O55 | `\d wealth.onboarding_progress` | đúng 7 cột: `user_id uuid`, `current_step smallint`, `dashboard_reviewed_at`, `completed_at`, `dismissed_at`, `created_at`, `updated_at` (timestamptz); **không có cột `numeric` hay `text`** |
| O56 | `SELECT * FROM wealth.onboarding_progress` | không có số tiền, tên tài sản hay ghi chú nào |
| O57 | `INSERT` trực tiếp `current_step = 8` | lỗi CHECK `onboarding_progress_current_step_is_known` |
| O58 | `UPDATE` đặt cả `completed_at` và `dismissed_at` | lỗi CHECK `onboarding_progress_completed_or_dismissed` |
| O59 | `DELETE FROM wealth.users WHERE id = C` | dòng onboarding của C bị xóa theo (`ON DELETE CASCADE`) |
| O60 | `SELECT version, success FROM flyway_schema_history` | có `11`, `success = true` |
