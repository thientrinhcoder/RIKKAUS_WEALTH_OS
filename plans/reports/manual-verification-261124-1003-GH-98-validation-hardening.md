# Manual verification — issue #98 validation, empty, loading & error-state hardening (Backend)

**Kết quả: 121/121 kịch bản PASS trên commit `d948229`** (lần chạy *sau*, database sạch). Cùng ma trận
chạy trên `main` `7d1b1b8` (lần chạy *trước*, database sạch) cho **98 PASS / 23 FAIL**: 20 kịch bản `[FIX]`
và ba kiểm tra quyền riêng tư P01–P03, tức đúng các lỗ hổng phase 1 đã dự đoán, không hơn không kém.
`clean verify` không flag xanh trên `d948229`: 131 test (81 unit, 50 integration), 0 fail, gồm drift check `openapi.json`, `OpenApiContractIT`, `ApplicationContextIT`. `clean verify -Dopenapi.update=true` không
đổi `services/api/openapi/openapi.json`, nên client mobile không cần tái tạo.

## Môi trường

| Mục | Giá trị |
|---|---|
| Ngày chạy | 24/11/2026, giờ `Asia/Saigon` |
| Database | Container dùng một lần `rikkaus-pg-98`, `postgres:18.6-alpine3.24`, `127.0.0.1:5498`, không volume, tạo mới cho mỗi lần chạy; mật khẩu sinh bằng `openssl rand -hex 32`, chỉ nằm trong biến môi trường |
| API | `java -jar … --spring.profiles.active=local --server.port=8098`, `DB_URL=jdbc:postgresql://localhost:5498/rikkaus`. *Trước*: jar đóng gói từ `7d1b1b8`. *Sau*: jar do `clean verify` không flag đóng gói từ `d948229` |
| Token | Hai user (A `an.nguyen@example.com`, B `binh.tran@example.com`) đăng nhập qua `POST /api/v1/auth/dev-sign-in`; token chỉ nằm trong biến shell, report không chứa token |
| Cách chấm | [`scenario-runner.sh`](../261124-1003-GH-98-validation-hardening/scenario-runner.sh) (bash 3.2 + `curl` + `jq`). Mỗi kịch bản so status và một biểu thức `jq` trên body; mọi body 4xx/5xx còn bị kiểm tra tự động là không chứa `com.rikkaus`, `Exception`, `SQL`, `jdbc`, `hibernate`, `tools.jackson`. "Không echo X" bỏ qua `instance` vì RFC 9457 định nghĩa nó là path của request |
| Lệnh | `OUT=<thư mục> LOG=<log API> BASE=http://localhost:8098/api/v1 ./scenario-runner.sh` |

## Các lần chạy

| Lần | Code | Kết quả | Ghi chú |
|---|---|---|---|
| 1 | `main` `7d1b1b8` | 97/121 | X14 fail do script: `noecho abc` đọc cả `instance` (`/api/v1/liabilities/abc`). Sửa script để bỏ `instance` khi kiểm tra echo, chạy lại trên database mới |
| 2 (*trước*) | `main` `7d1b1b8`, database mới | **98/121** | 23 FAIL ở bảng *Lỗ hổng đã sửa* và P01–P03 |
| 3 | Working tree trước commit | 121/121 | Lần chạy đầu sau khi sửa |
| 4 (*sau*) | `d948229`, jar của `clean verify`, database mới | **121/121** | Kết quả ghi bên dưới |

## Lỗ hổng đã sửa — response trước và sau

Body rút gọn: `type` bỏ tiền tố `urn:rikkaus:problem:`, với 2xx chỉ giữ member liên quan.

| ID | Input | *Trước* (`main`) | *Sau* (`d948229`) |
|---|---|---|---|
| X09 | `POST /liabilities` `"currency": 1` | **201**, `{"currency":"USD"}` — số được hiểu là thứ tự enum | 400 `malformed-request` |
| X10 | `POST /liabilities` `"category": 0` | **201**, `{"category":"BANK_LOAN"}` | 400 `malformed-request` |
| X15 | `PUT /settings/exchange-rate` `{"usdToVndRate":-1,"effectiveDate":null}`, `Accept-Language: de` | 400, `muss größer 0 sein`, `darf nicht null sein` | 400, `must be greater than zero`, `must not be null` |
| N17 | `usdToVndRate: 0` | 400, `must be greater than 0` | 400, `must be greater than zero` |
| N18 | `usdToVndRate: 25100.1234567` | 400, `numeric value out of bounds (<13 digits>.<6 digits> expected)` | 400, `must have at most 13 integer digits and 6 decimals` |
| D08 | `POST /liabilities` `startDate: "-5000-01-01"` | **500** `internal-error`; log có `ERROR: date out of range: "5001-01-01 BC +07"` | 400 `malformed-request` |
| D09 | `startDate: "+10000-01-01"` | **201**, lưu năm 10000 | 400 `malformed-request` |
| D10 | `POST /cash-flow-entries` `effectiveDate: "0000-01-31"` | **201**; PostgreSQL lưu `0001-01-31 BC`, `GET` đọc lại `0001-01-31` (lệch một năm, đo ở lần dò trước khi viết ma trận) | 400 `malformed-request` |
| D11 | `effectiveDate: "2026-09-12T10:00:00"` | **201**, cắt giờ thành `2026-09-12` | 400 `malformed-request` |
| D12 | `effectiveDate: 20000` | **201**, thành `2024-10-04` (đếm ngày từ 1970) | 400 `malformed-request` |
| D13 | `effectiveDate: [2026,9,12]` | **201**, `2026-09-12` | 400 `malformed-request` |
| D17 | `POST /goals` `targetDate: "+999999999-12-31"` | **500** `internal-error` | 400 `malformed-request` |
| D19 | `PUT /settings/exchange-rate` `effectiveDate: "2026-09-12T20:00:00Z"` | **200**, lưu ngày UTC `2026-09-12`; ở Việt Nam thời điểm đó đã là 13/09 | 400 `malformed-request` |
| D24 | `GET /cash-flow-forecast?from=2026-9` | 400, `must be a month in the form YYYY-MM` | 400, `must be a calendar month in the form YYYY-MM` (cùng câu với report tháng) |
| T09 | `PUT /profile` `displayName: " "` | **200**, `{"displayName":" "}` | 400, `displayName` / `must not be blank` |
| T10 | `PUT /profile` `displayName: "An\u0000Nguyễn"` | **500** `internal-error` | 400, `displayName` / `must not contain a null character` |
| T11 | `PUT /profile` 121 ký tự | 400, `size must be between 0 and 120` | 400, `must be at most 120 characters` |
| T12 | `POST /assets` `name: "​"` | **201**, tên vô hình | 400, `name` / `must not be blank` |
| T13 | `POST /assets/ASV/valuations` `note: "﻿"` | 201, `note` = `U+FEFF` | 201, `note` = `null` |
| T14 | `POST /auth/dev-sign-in` `displayName` chứa `U+0000` (chỉ profile `local`) | **500** `internal-error` | 400, `displayName` / `must not contain a null character` |
| P01 | Số response 500 trong lần chạy | 4 (D08, D17, T10, T14) | 0 |
| P02 | Dòng `ERROR` trong log API | 4 | 0 |
| P03 | Log chứa giá trị người dùng gửi | 1 dòng: Hibernate log nguyên văn lỗi PostgreSQL `date out of range: "5001-01-01 BC +07"` | 0 |

Log của lần *trước* dài 715 dòng (stack trace của bốn 500), lần *sau* 193 dòng, không dòng `ERROR`; hai
dòng `WARN` duy nhất là cảnh báo cố ý của dev sign-in.

## Toàn bộ ma trận, trước và sau

Cột *Mong đợi* là status sau khi sửa; P01–P03 là kiểm tra trên cả lần chạy. Input chi tiết của từng ID ở
[phase-01-test-scenarios.md](../261124-1003-GH-98-validation-hardening/phase-01-test-scenarios.md).

| ID | Kịch bản | Mong đợi | *Trước* | *Sau* |
|---|---|---|---|---|
| H01 | POST /assets Căn hộ Thảo Điền 4.200.000.000 VND | 201 | 201 PASS | 201 PASS |
| H02 | POST /assets Tài khoản USD Vietcombank 21.250 USD | 201 | 201 PASS | 201 PASS |
| H03 | POST /liabilities Vay mua nhà Vietcombank | 201 | 201 PASS | 201 PASS |
| H04 | POST /cash-flow-entries lương 45.000.000 VND/tháng | 201 | 201 PASS | 201 PASS |
| H05 | POST /goals Quỹ dự phòng 6 tháng | 201 | 201 PASS | 201 PASS |
| H06 | PUT /settings/exchange-rate 25.100 | 200 | 200 PASS | 200 PASS |
| H07 | POST valuation 21.400 USD hôm nay | 201 | 201 PASS | 201 PASS |
| H08 | PUT /assets đổi ghi chú | 200 | 200 PASS | 200 PASS |
| H09 | PUT /liabilities dư nợ 2.380.000.000 | 200 | 200 PASS | 200 PASS |
| H10 | PUT /cash-flow-entries 46.000.000 | 200 | 200 PASS | 200 PASS |
| H11 | PUT /goals góp 13.000.000 | 200 | 200 PASS | 200 PASS |
| H12 | PUT /profile "  Trinh Nguyễn  " | 200 | 200 PASS | 200 PASS |
| H13 | PATCH /onboarding bước 3 | 200 | 200 PASS | 200 PASS |
| H14 | POST /feedback | 201 | 201 PASS | 201 PASS |
| H15 | GET /wealth-snapshot | 200 | 200 PASS | 200 PASS |
| H16 | GET /cash-flow-occurrences tháng 12/2026 | 200 | 200 PASS | 200 PASS |
| H17 | GET /cash-flow-reports/monthly 2026-11 | 200 | 200 PASS | 200 PASS |
| H18 | GET /cash-flow-forecast | 200 | 200 PASS | 200 PASS |
| H19 | GET /goal-projections | 200 | 200 PASS | 200 PASS |
| H20 | GET /assets/ASV/deletion-impact | 200 | 200 PASS | 200 PASS |
| H21 | DELETE khoản học phí một lần | 204 | 204 PASS | 204 PASS |
| X01 | body rỗng | 400 | 400 PASS | 400 PASS |
| X02 | body null | 400 | 400 PASS | 400 PASS |
| X03 | body [] | 400 | 400 PASS | 400 PASS |
| X04 | JSON cụt | 400 | 400 PASS | 400 PASS |
| X05 | Content-Type text/plain | 415 | 415 PASS | 415 PASS |
| X06 | outstandingBalance "abc" | 400 | 400 PASS | 400 PASS |
| X07 | outstandingBalance true | 400 | 400 PASS | 400 PASS |
| X08 | currency "EUR" | 400 | 400 PASS | 400 PASS |
| X09 | currency 1 (số) | 400 | 201 FAIL | 400 PASS |
| X10 | category 0 (số) | 400 | 201 FAIL | 400 PASS |
| X11 | trường thừa userId, id | 201 | 201 PASS | 201 PASS |
| X12 | outstandingBalance 1e2147483648 | 400 | 400 PASS | 400 PASS |
| X13 | số 1.201 chữ số | 400 | 400 PASS | 400 PASS |
| X14 | GET /liabilities/abc | 400 | 400 PASS | 400 PASS |
| X15 | settings sai, Accept-Language de | 400 | 400 FAIL | 400 PASS |
| X16 | name là số 12345 | 201 | 201 PASS | 201 PASS |
| X17 | outstandingBalance là chuỗi | 201 | 201 PASS | 201 PASS |
| X18 | DELETE confirm=abc | 400 | 400 PASS | 400 PASS |
| N01 | currentValue -1 | 400 | 400 PASS | 400 PASS |
| N02 | currentValue 0 | 201 | 201 PASS | 201 PASS |
| N03 | currentValue 18 chữ số nguyên | 400 | 400 PASS | 400 PASS |
| N04 | currentValue 3 chữ số thập phân | 400 | 400 PASS | 400 PASS |
| N05 | currentValue 1e400 | 400 | 400 PASS | 400 PASS |
| N06 | quantity 0 | 400 | 400 PASS | 400 PASS |
| N07 | interestRate 100.0001 | 400 | 400 PASS | 400 PASS |
| N08 | valuation value -1 | 400 | 400 PASS | 400 PASS |
| N09 | outstandingBalance -0.01 | 400 | 400 PASS | 400 PASS |
| N10 | recurringPaymentAmount 0 | 400 | 400 PASS | 400 PASS |
| N11 | liability interestRate 1000 | 400 | 400 PASS | 400 PASS |
| N12 | amount 0 | 400 | 400 PASS | 400 PASS |
| N13 | amount -45.000.000 | 400 | 400 PASS | 400 PASS |
| N14 | targetAmount 0 | 400 | 400 PASS | 400 PASS |
| N15 | expectedAnnualReturnPercent 30.01 | 400 | 400 PASS | 400 PASS |
| N16 | đã có > mục tiêu | 400 | 400 PASS | 400 PASS |
| N17 | usdToVndRate 0 | 400 | 400 FAIL | 400 PASS |
| N18 | usdToVndRate 7 chữ số thập phân | 400 | 400 FAIL | 400 PASS |
| N19 | currentStep 8 | 400 | 400 PASS | 400 PASS |
| N20 | currentStep 2.5 | 400 | 400 PASS | 400 PASS |
| D01 | valuationDate 2026-02-30 | 400 | 400 PASS | 400 PASS |
| D02 | valuationDate ngày mai | 400 | 400 PASS | 400 PASS |
| D03 | mua sau ngày định giá | 400 | 400 PASS | 400 PASS |
| D04 | valuation ngày mai | 400 | 400 PASS | 400 PASS |
| D05 | valuation trước ngày mua | 400 | 400 PASS | 400 PASS |
| D06 | đáo hạn trước ngày bắt đầu | 400 | 400 PASS | 400 PASS |
| D07 | kỳ trả sau đáo hạn | 400 | 400 PASS | 400 PASS |
| D08 | startDate -5000-01-01 | 400 | 500 FAIL | 400 PASS |
| D09 | startDate +10000-01-01 | 400 | 201 FAIL | 400 PASS |
| D10 | effectiveDate 0000-01-31 | 400 | 201 FAIL | 400 PASS |
| D11 | effectiveDate date-time | 400 | 201 FAIL | 400 PASS |
| D12 | effectiveDate 20000 (số) | 400 | 201 FAIL | 400 PASS |
| D13 | effectiveDate [2026,9,12] | 400 | 201 FAIL | 400 PASS |
| D14 | endDate trước effectiveDate | 400 | 400 PASS | 400 PASS |
| D15 | effectiveDate tương lai 15/01/2027 | 201 | 201 PASS | 201 PASS |
| D16 | targetDate hôm nay | 400 | 400 PASS | 400 PASS |
| D17 | targetDate +999999999-12-31 | 400 | 500 FAIL | 400 PASS |
| D18 | effectiveDate ngày mai | 400 | 400 PASS | 400 PASS |
| D19 | effectiveDate date-time UTC | 400 | 200 FAIL | 400 PASS |
| D20 | rateEffectiveDate ngày mai | 400 | 400 PASS | 400 PASS |
| D21 | occurrences from=2026-02-29 | 400 | 400 PASS | 400 PASS |
| D22 | occurrences to < from | 400 | 400 PASS | 400 PASS |
| D23 | report month=2026-13 | 400 | 400 PASS | 400 PASS |
| D24 | forecast from=2026-9 | 400 | 400 FAIL | 400 PASS |
| D25 | forecast from=tháng này | 400 | 400 PASS | 400 PASS |
| D26 | effectiveFrom=2026-02-30 | 400 | 400 PASS | 400 PASS |
| D27 | startDate "" | 201 | 201 PASS | 201 PASS |
| D28 | effectiveDate "" | 400 | 400 PASS | 400 PASS |
| C01 | asset currency EUR | 400 | 400 PASS | 400 PASS |
| C02 | valuation VND trên tài sản USD | 400 | 400 PASS | 400 PASS |
| C03 | VND kèm tỷ giá | 400 | 400 PASS | 400 PASS |
| C04 | USD có tỷ giá thiếu ngày | 400 | 400 PASS | 400 PASS |
| C05 | currency "usd" | 400 | 400 PASS | 400 PASS |
| C06 | list currency=EUR | 400 | 400 PASS | 400 PASS |
| T01 | name U+00A0 | 400 | 400 PASS | 400 PASS |
| T02 | name chứa U+0000 | 400 | 400 PASS | 400 PASS |
| T03 | name 121 ký tự | 400 | 400 PASS | 400 PASS |
| T04 | notes chỉ khoảng trắng Unicode | 201 | 201 PASS | 201 PASS |
| T05 | notes chứa U+0000 | 400 | 400 PASS | 400 PASS |
| T06 | goal name U+00A0 | 400 | 400 PASS | 400 PASS |
| T07 | feedback message trắng | 400 | 400 PASS | 400 PASS |
| T08 | feedback screen hoa | 400 | 400 PASS | 400 PASS |
| T09 | displayName U+00A0 | 400 | 200 FAIL | 400 PASS |
| T10 | displayName chứa U+0000 | 400 | 500 FAIL | 400 PASS |
| T11 | displayName 121 ký tự | 400 | 400 FAIL | 400 PASS |
| T12 | name U+200B | 400 | 201 FAIL | 400 PASS |
| T13 | valuation note U+FEFF | 201 | 201 FAIL | 201 PASS |
| T14 | dev-sign-in displayName U+0000 | 400 | 500 FAIL | 400 PASS |
| T15 | q 121 ký tự | 400 | 400 PASS | 400 PASS |
| R01 | GET asset của B so với id không tồn tại | — | PASS | PASS |
| R02 | PUT liability của B so với id không tồn tại | — | PASS | PASS |
| R03 | DELETE goal của B so với id không tồn tại | — | PASS | PASS |
| R04 | PUT cash-flow của B so với id không tồn tại | — | PASS | PASS |
| R05 | GET valuations asset của B so với id không tồn tại | — | PASS | PASS |
| R06 | collateral là asset của B so với id không tồn tại | — | PASS | PASS |
| R07 | linkedAssetId của B | 400 | 400 PASS | 400 PASS |
| R08 | link cả asset và liability | 400 | 400 PASS | 400 PASS |
| R09 | correctsValuationId của tài sản khác | 400 | 400 PASS | 400 PASS |
| M01 | ba lỗi trong một body | 400 | 400 PASS | 400 PASS |
| P01 | Số response 500 trong lần chạy | — | FAIL | PASS |
| P02 | Dòng ERROR trong log API | — | FAIL | PASS |
| P03 | Log API chứa giá trị người dùng gửi | — | FAIL | PASS |

## Dò thêm ngoài ma trận (lần chạy 3)

| Input | Kết quả |
|---|---|
| `POST /auth/dev-sign-in` `email` chứa `U+0000` | 400, `email` / `must be an email address` (không cần sửa) |
| `PUT /settings/exchange-rate` `effectiveDate: "0001-01-01"` | 200, đọc lại `0001-01-01`: ngày sớm nhất được nhận và giữ nguyên |
| `effectiveDate: " 2026-11-20"` (khoảng trắng đầu) | 400 `malformed-request` |
| `GET /goal-projections/{id}` của mục tiêu `targetDate` 31/12/9999, 99.999.999.999.999.999,99 ₫, lãi 30 %/năm (đo trên `main`) | 200 trong 12 ms, nhưng `fromAllocated` là một số khoảng 1.046 chữ số. Không phải 500; xem câu hỏi PO #2 |

## Kiểm tra khác

- **`clean verify`** không flag trên `d948229`: 131 test (81 unit, 50 integration), 0 fail, gồm drift check `openapi.json`, `OpenApiContractIT`, `ApplicationContextIT`. `ArchitectureRulesTest` xanh: `shared/validation`
  chỉ phụ thuộc `shared`, các slice chỉ thêm phụ thuộc vào `shared`.
- **Contract**: `clean verify -Dopenapi.update=true` không đổi `openapi.json`; message lỗi và cách Jackson
  đọc ngày/enum không nằm trong tài liệu OpenAPI. `apps/mobile/src/api/generated/*` không cần tái tạo.
- **CodeQL `java/comparison-with-wider-type`**: mã mới chỉ so `int` với `int` (`start < end` trên chỉ số
  chuỗi), không có vòng lặp nào so biến hẹp với giới hạn `long`.
- **Gitleaks**: plan, report, script và docs không chứa token, JWT hay mật khẩu; mật khẩu PostgreSQL sinh
  lúc chạy và chỉ nằm trong file môi trường ở scratchpad, không in ra.
- **Không migration**, không đổi `GET` nào, không đụng slice `snapshot`, `QueryParameters` hay `SearchText`.

## Câu hỏi cho PO

Design #96 chưa được duyệt; các giả định dưới đây là của backend.

1. **Lỗi enum và ngày trong body vẫn là `malformed-request` không có tên trường.** Ví dụ form khoản nợ gửi
   `"currency": "EUR"` hay `"startDate": "15/06/2021"` thì client chỉ biết "yêu cầu sai dạng", không biết
   ô nào. Docs đã công bố hành vi này và client đã map theo `type`; form dùng picker nên hiếm khi xảy ra.
   Giữ nguyên (đề xuất), hay chuyển thành `validation-failed` kèm `field` (đổi contract, frontend phải map lại)?
2. **`targetDate` của mục tiêu không có giới hạn trên.** `Mua nhà Quận 7`, 6.500.000.000 ₫, ngày 31/12/9999
   được nhận; projection trả số hàng nghìn chữ số mà JavaScript đọc thành `Infinity`. Có muốn giới hạn,
   ví dụ tối đa 100 năm sau hôm nay (31/12/2126)?
3. **Lãi suất khoản nợ được tới 999,9999 %/năm, của tài sản chỉ tới 100 %/năm.** Thẻ tín dụng Techcombank
   36 %/năm là thật; gõ nhầm `625` thay cho `6,25` thì khoản nợ vẫn lưu. Có áp trần 100 %/năm như tài sản?
4. **Ngày sớm nhất là 01/01/0001.** Không có cận dưới nghiệp vụ nào, ví dụ ngày mua `Căn hộ Thảo Điền`
   01/01/1900 hay 01/01/0001 đều được nhận. Có muốn cận dưới (ví dụ 01/01/1900)?
5. **Ép kiểu vô hướng của Jackson được giữ.** `"name": 12345` lưu thành `"12345"`, `"outstandingBalance":
   "2400000000"` (chuỗi) được nhận như số. Vô hại với dữ liệu; giữ nguyên?
6. **Ký tự vô hình giờ là khoảng trắng.** Tên chỉ gồm `U+200B`, `U+2060` hoặc `U+FEFF` bị từ chối như
   `U+00A0`; ghi chú chỉ gồm chúng thành `null`. Đây là thay đổi hành vi nhỏ duy nhất với dữ liệu hợp lệ
   về mặt kỹ thuật; xác nhận?
