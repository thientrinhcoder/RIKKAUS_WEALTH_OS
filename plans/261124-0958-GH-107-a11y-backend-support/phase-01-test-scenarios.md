# Phase 1 — Kịch bản viết trước

Viết trước khi có script và trước khi đo. Mỗi kịch bản chạy bằng tay trên API thật (jar của `main`
`7d1b1b8`, profile `local`, cổng 8107, PostgreSQL 18.6 container `rikkaus-pg-107` cổng 5407). Kết quả
ghi ở `plans/reports/manual-verification-261124-0958-GH-107-a11y-backend-support.md`.

Người dùng fixture: baseline `an.nguyen@example.com` (`An Nguyễn`), danh sách dài
`long.list@example.com` (`Lê Thị Danh Sách Dài`), người thứ hai cho ownership `bich.tran@example.com`.

## F — Script fixture

| ID | Kịch bản | Kỳ vọng |
|---|---|---|
| F01 | `bash -n` trên `lib.sh`, `reset.sh`, `load-baseline.sh`, `load-long-lists.sh`; `shellcheck` nếu có | Không lỗi |
| F02 | `reset.sh` với email chưa từng dùng | Exit 0; bốn catalog list `[]` |
| F03 | `load-baseline.sh` | Exit 0; `GET /wealth-snapshot`: `totalAssets.amountVnd = 12002000000.00`, `totalLiabilities.amountVnd = 1532000000.00`, `netWorth.amountVnd = 10470000000.00`, `complete = true`; `exchangeRate` `25100.000000` ngày `2026-09-12` |
| F04 | Baseline có đúng một định giá cũ hơn 30 ngày | Đúng 1 tài sản có `valuationDate` < hôm nay − 30 ngày (`Căn hộ Thảo Điền`) |
| F05 | Baseline có đúng một kỳ trả nợ trong 30 ngày | `upcomingLiabilityDues` có đúng 1 phần tử `PAYMENT` của `Vay mua nhà Vietcombank`, không `overdue` |
| F06 | Baseline có một mục tiêu `AT_RISK` | `GET /goal-projections` có đúng 1 `AT_RISK` (`Quỹ du học cho con`), coverage trong [80, 100) |
| F07 | Dòng tiền baseline | `GET /cash-flow-reports/monthly?month=<tháng này>`: thu 180.000.000, chi 92.000.000, ròng 88.000.000 |
| F08 | Chạy `load-baseline.sh` lần hai | Cùng số bản ghi, cùng tổng snapshot (idempotent) |
| F09 | `load-long-lists.sh` | Exit 0; assets 60, liabilities 40, cash-flow entries 120, goals 15, feedback 20, valuations của tài sản lịch sử 24 |
| F10 | Chạy `load-long-lists.sh` lần hai | Cùng số đếm, không nhân đôi feedback |
| F11 | `reset.sh` cho user danh sách dài | Bốn catalog list `[]`; tỷ giá, onboarding, feedback còn lại (không có route xoá) và script nói rõ |
| F12 | Token không lộ | Stdout/stderr của cả ba script không chứa chuỗi `eyJ`; thư mục tạm bị xoá khi thoát, kể cả khi lỗi |
| F13 | API không chạy hoặc không có profile `local` | Exit khác 0, thông báo chỉ ra `API_BASE_URL` và profile `local` |
| F14 | Bản ghi đi qua validation thật | Một POST lỗi trong script làm script dừng và in body problem (không token) |

## L — Danh sách dưới góc nhìn UI (trên bộ danh sách dài)

Áp dụng cho `GET /assets`, `/assets/{id}/valuations`, `/liabilities`, `/cash-flow-entries`,
`/cash-flow-occurrences?from&to`, `/goals`, `/goal-projections`, `/feedback`, và `GET /wealth-snapshot`.

| ID | Kịch bản | Kỳ vọng |
|---|---|---|
| L01 | Đếm phần tử | Bằng số đã nạp; không bị cắt |
| L02 | Kích thước response (byte) và thời gian (`time_total`, trung vị 5 lần) | Ghi số; dưới 1 s trên máy local |
| L03 | Gọi hai lần liên tiếp | Byte-identical |
| L04 | `?page=2`, `?limit=5`, `?offset=10`, `?size=5`, `?page=2&limit=5&offset=10` | Byte-identical với lời gọi không tham số |
| L05 | Mỗi giá trị `sort` gọi hai lần | Byte-identical; tên trùng nhau giữ thứ tự theo `id` |
| L06 | Thứ tự mặc định đúng như docs | Assets `-valuationDate`, liabilities `nextPaymentDate` (null cuối), entries `-effectiveDate`, goals `targetDate`, feedback mới nhất trước |
| L07 | Tên dài 120 ký tự có dấu | Trả về nguyên vẹn, NFC, không cắt |

## E — Lỗi dưới góc nhìn UI

| ID | Kịch bản | Kỳ vọng |
|---|---|---|
| E01 | `POST /assets` thiếu nhiều trường | 400 `application/problem+json`; có `type`, `title`, `status`, `detail`, `instance`, `correlationId`; `errors[].field` đúng tên trường request (`name`, `category`, `currentValue`, `currency`, `valuationDate`, `liquidity`) |
| E02 | Trường "lồng" như `instrument.quantity` | Request asset phẳng: `field` là `quantity`, khớp tên trong request (ghi rõ không có object lồng) |
| E03 | Lỗi liên trường (liability, cash-flow, goal) | Mọi lỗi trong một `errors`, mỗi lỗi đúng tên trường |
| E04 | Quá độ dài (`name` 121 ký tự, `notes` 2001) và quá độ chính xác (`currentValue` 3 chữ số thập phân) | `validation-failed`, `field` đúng |
| E05 | JSON hỏng, ngày sai định dạng, enum lạ trong body | Ghi lại: `malformed-request` có hay không có `field` |
| E06 | Tham số truy vấn sai (`sort=bogus`, `category=cash`, `q` 121 ký tự, `effectiveFrom=2026-13-01`) | `validation-failed`, `field` = tên tham số |
| E07 | 404 id của người khác so với id không tồn tại (GET, PUT, DELETE, deletion-impact, valuations) | Body giống từng byte sau khi thay id trong `instance` và cùng `X-Correlation-Id` |
| E08 | `DELETE /assets/{id}` có liên kết, không `confirm` | 409 `linked-records` có `impact` với `confirmationRequired = true` |
| E09 | 401 không token, 405, 406, 415 | `application/problem+json` đủ trường, có `correlationId` |
| E10 | `X-Correlation-Id` gửi lên | Header và `correlationId` trong body trùng; CORS từ `http://localhost:8081` expose header |
| E11 | Không có 500 | Không lời gọi nào trong E01–E10 trả 5xx |

## O — OpenAPI cho thuộc tính form

| ID | Kịch bản | Kỳ vọng |
|---|---|---|
| O01 | Mọi trường chuỗi có `@Size(max)` | `maxLength` có trong schema request |
| O02 | Mọi số có `@DecimalMin/@DecimalMax` | `minimum`/`exclusiveMinimum`/`maximum` có trong schema |
| O03 | `@Digits`, `@NotBlank`, quy tắc ngày, quy tắc liên trường | Liệt kê cái gì không được công bố |
| O04 | Tham số truy vấn | `q` có `maxLength`, enum có `enum` |

## P — Kết luận phân trang

Từ L02 trên bộ danh sách dài: nếu mọi list dưới 1 s và dưới vài trăm KB, pilot không cần phân trang; nếu
không, ghi đề xuất kèm số đo vào "Câu hỏi cho PO". Không thêm pagination trong ticket này.
