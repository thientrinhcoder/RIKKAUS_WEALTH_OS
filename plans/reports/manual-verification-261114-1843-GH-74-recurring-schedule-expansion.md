# Xác minh thủ công — mở rộng lịch kỳ của dòng tiền định kỳ (#74)

Ngày: 2026-11-14. Một lần chạy trên nhánh `thientrinhcoder/feat/issue-74-recurring-schedule-expansion`, mã
nguồn là commit `13a23d8` (cắt từ `main` `d9abcd8`). Commit chứa file này chỉ thêm tài liệu, không đổi code.

| Lần | Giờ (ICT) | Mã nguồn | Số dòng kết quả | Đạt |
|---|---|---|---|---|
| 1 | 18:54–18:55 | `13a23d8` | 53 (51 case + 41b, 42b) | **53** |

Mọi case đạt ngay lần chạy đầu; không có case nào fail rồi được sửa. Kỳ vọng của từng case là bảng ngày
tính tay trước khi code ở
[`phase-01-test-scenarios.md`](../261114-1843-GH-74-recurring-schedule-expansion/phase-01-test-scenarios.md),
và script chạy so kết quả API với đúng bảng đó. Quy trình tái hiện ở
[`docs/manual-test-guide-cash-flow-occurrences.md`](../../docs/manual-test-guide-cash-flow-occurrences.md).

Product Owner chỉ đạo task này ship **không có unit test hay integration test mới**. Status và body dưới đây
lấy từ lần chạy thật, không viết tay.

## Môi trường

| | |
|---|---|
| Database | Container `postgres:18.6-alpine3.24` dùng một lần, tên `rikkaus-pg-issue-74`, cổng `127.0.0.1:5474`, tạo mới cho lần chạy và đã xóa sau đó. Volume `rikkaus-postgres-data` không bị chạm |
| Ứng dụng | `spring-boot:run` trên JDK 25, `--spring.profiles.active=local --server.port=8074`, `DB_URL=jdbc:postgresql://localhost:5474/rikkaus`. Case 49 khởi động lại cùng mã với `-Duser.timezone=Pacific/Pago_Pago`. Đã tắt sau lần chạy |
| Migration | Flyway: `Successfully applied 8 migrations to schema "public", now at version v9`. Không migration mới: #74 tính khi đọc, không lưu |
| Người dùng, token | User A `0f2c8d14-…0c93` (An Nguyễn), user B `7b41e590-…a842` (Bích Trần) chèn thẳng; token HS256 mint bằng secret của profile `local`, thêm một token hết hạn và một token ký bằng secret khác; request đi qua `BearerTokenAuthenticationFilter` thật |
| Dữ liệu | Chín entry của A và một của B tạo qua `POST /api/v1/cash-flow-entries`: `Lương 45.000.000 ₫` tháng từ 31/01/2026, `Tiền thuê nhà 18.000.000 ₫` tháng 30/12/2025–30/06/2026, `Phí gửi xe chung cư 1.200.000 ₫` tháng 15/01–14/06/2026, `Học phí 12.500.000 ₫` quý từ 30/11/2025, `Thưởng Tết 60.000.000 ₫` năm từ 13/02/2026, `Phí bảo hiểm nhân thọ 24.000.000 ₫` năm từ 29/02/2028, `Phí bảo trì chung cư 3.200.000 ₫` quý một kỳ 15/03/2026, `Khám sức khỏe định kỳ 4.650.000 ₫` một lần 31/03/2026, `Cổ tức VNM 150 USD` một lần 01/04/2026; B: `Lương của Bích 30.000.000 ₫` tháng từ 31/01/2026 |
| Gate build | `./services/api/mvnw -f services/api/pom.xml clean verify` xanh **không** update flag trên `13a23d8`: 81 unit test, 50 integration test, `BUILD SUCCESS` |

## Kết quả từng case

Ký hiệu kỳ: `ngày sN` là `sequence: N`, `dời` là `shiftedToMonthEnd: true`. E1–E9 là các entry ở bảng trên
theo thứ tự. Cột bằng chứng in lại body đã parse, nên số tiền hiện dạng `45000000.0`; byte thật trên dây
là `45000000.00` và `150.00` — case 23 và 24 assert đúng chuỗi đó trong body thô, xem mục body thô bên dưới.

| # | Request | Status | Bằng chứng | Kết quả |
|---|---|---|---|---|
| 1 | `from=2026-01-01&to=2026-04-30`, E1 | `200` | 2026-01-31 s1, 2026-02-28 s2 dời, 2026-03-31 s3, 2026-04-30 s4 dời | đạt |
| 2 | `from=2027-12-01&to=2028-03-31`, E1 | `200` | 2027-12-31 s24, 2028-01-31 s25, 2028-02-29 s26 dời, 2028-03-31 s27 | đạt |
| 3 | `from=2026-02-01&to=2026-02-28`, E1 | `200` | 2026-02-28 s2 dời | đạt |
| 4 | `from=2026-03-01&to=2026-03-31`, E1 | `200` | 2026-03-31 s3 | đạt |
| 5 | `from=2025-11-01&to=2026-11-30`, E4 | `200` | 2025-11-30 s1, 2026-02-28 s2 dời, 2026-05-30 s3, 2026-08-30 s4, 2026-11-30 s5 | đạt |
| 6 | `from=2026-12-01&to=2027-05-31`, E4 | `200` | 2027-02-28 s6 dời, 2027-05-30 s7 | đạt |
| 7 | `from=2026-01-01&to=2027-12-31`, E5 | `200` | 2026-02-13 s1, 2027-02-13 s2 | đạt |
| 8 | `from=2028-02-01&to=2030-01-31`, E6 | `200` | 2028-02-29 s1, 2029-02-28 s2 dời | đạt |
| 9 | `from=2031-03-01&to=2033-02-28`, E6 | `200` | 2032-02-29 s5, 2033-02-28 s6 dời | đạt |
| 10 | `from=2026-01-01&to=2026-12-31`, E2 | `200` | 2026-01-30 s2, 2026-02-28 s3 dời, 2026-03-30 s4, 2026-04-30 s5, 2026-05-30 s6, 2026-06-30 s7 | đạt |
| 11 | `from=2026-06-30&to=2026-07-31`, E2 | `200` | 2026-06-30 s7 | đạt |
| 12 | `from=2026-01-01&to=2026-12-31`, E3 | `200` | 2026-01-15 s1, 2026-02-15 s2, 2026-03-15 s3, 2026-04-15 s4, 2026-05-15 s5 | đạt |
| 13 | `from=2026-01-01&to=2026-12-31`, E7 | `200` | 2026-03-15 s1 | đạt |
| 14 | `from=2027-01-01&to=2028-02-28`, E6 | `200` | (không có) | đạt |
| 15 | `from=2026-01-31&to=2026-02-27`, E1 | `200` | 2026-01-31 s1 | đạt |
| 16 | `from=2026-01-01&to=2026-03-31`, E1 và tổng | `200` | E1: 2026-01-31 s1, 2026-02-28 s2 dời, 2026-03-31 s3; tổng 13 kỳ | đạt |
| 17 | `from=2026-02-01&to=2026-03-30`, E1 | `200` | 2026-02-28 s2 dời | đạt |
| 18 | quý 1 + quý 2 so với nửa năm | `200/200/200` | 13 + 10 = 23, nửa năm 23; giao 0; hợp = nửa năm: True; E8 chỉ ở quý 1: True; E9 chỉ ở quý 2: True | đạt |
| 19 | 12 khoảng tháng của 2026 so với cả năm | `200` | tổng 12 tháng 31, cả năm 31, trùng 0, tập bằng nhau: True, kỳ lệch tháng 0 | đạt |
| 20 | `from=2026-02-28&to=2026-02-28` | `200` | E1 s2 dời, E2 s3 dời, E4 s2 dời | đạt |
| 21 | `from=2020-01-01&to=2020-12-31` | `200` | body `[]` | đạt |
| 22 | `from=2026-03-01&to=2026-03-31`, E8 | `200` | 1 kỳ: `{"sourceEntryId": "4e4a76d8-112e-460c-aa14-79e09f59d53e", "occurrenceDate": "2026-03-31", "sequence": 1, "shiftedToMonthEnd": false, "entryType": "EXPENSE", "category": "HEALTHCARE", "amount": 4650000.0, "currency": "VND", "recurrence": null}` | đạt |
| 23 | `from=2026-04-01&to=2026-04-30`, E8 và E9 | `200` | E8: 0 kỳ; E9: `{"sourceEntryId": "1dd995d2-d191-4e7b-a5f2-024c39d47f0e", "occurrenceDate": "2026-04-01", "sequence": 1, "shiftedToMonthEnd": false, "entryType": "INCOME", "category": "DIVIDEND", "amount": 150.0, "currency": "USD", "recurrence": null}` | đạt |
| 24 | `from=2026-01-01&to=2026-01-31`, kỳ của E1 | `200` | `{"sourceEntryId": "67928f6f-7c25-4f3d-9dbb-f8167dda5978", "occurrenceDate": "2026-01-31", "sequence": 1, "shiftedToMonthEnd": false, "entryType": "INCOME", "category": "SALARY", "amount": 45000000.0, "currency": "VND", "recurrence": "MONTHLY"}` | đạt |
| 25 | `GET /cash-flow-entries/{sourceEntryId}` của case 24 | `200` | `SALARY` `45000000.0` `VND` notes `Lương` | đạt |
| 26 | cả năm 2026, thứ tự | `200` | 31 kỳ, sắp theo (ngày, `sourceEntryId`): True | đạt |
| 27 | cả năm 2026 gọi hai lần | `200` | `b77ec945b130375d…` và `b77ec945b130375d…` | đạt |
| 28 | B, cả năm 2026, chưa có entry | `200` | body `[]` | đạt |
| 29 | B tạo EB, cả năm 2026 | `200` | 12 kỳ, nguồn ['EB'], amount [30000000.0] | đạt |
| 30 | A, cả năm 2026 sau khi B có entry | `200` | 31 kỳ, kỳ mang id EB: 0 | đạt |
| 31 | `PUT` E2 `endDate: 2026-04-30`, cả năm 2026 | `200/200` | E2: 2026-01-30 s2, 2026-02-28 s3 dời, 2026-03-30 s4, 2026-04-30 s5; tổng 29 | đạt |
| 32 | `DELETE` E3, cả năm 2026 | `204/200` | E3: 0 kỳ; tổng 24 | đạt |
| 33 | `?to=2026-03-31` | `400` | `from`: must be present | đạt |
| 34 | `?from=2026-01-01` | `400` | `to`: must be present | đạt |
| 35 | `(không query)` | `400` | `from`: must be present; `to`: must be present | đạt |
| 36 | `?from=&to=2026-03-31` | `400` | `from`: must be present | đạt |
| 37 | `?from=2026-04-01&to=2026-03-31` | `400` | `to`: must not be before from | đạt |
| 38 | `?from=31/01/2026&to=2026-03-31` | `400` | `from`: must be a calendar date in the form YYYY-MM-DD | đạt |
| 39 | `?from=2026-01-01&to=2026-02-29` | `400` | `to`: must be a calendar date in the form YYYY-MM-DD | đạt |
| 40 | `?from=2026-1-5&to=2026-03-31` | `400` | `from`: must be a calendar date in the form YYYY-MM-DD | đạt |
| 41 | `?from=2026-01-01T00:00:00%2B07:00&to=2026-03-31` | `400` | `from`: must be a calendar date in the form YYYY-MM-DD | đạt |
| 41b | `?from=2026-01-01&from=2026-02-01&to=2026-03-31` | `400` | `from`: must be a calendar date in the form YYYY-MM-DD | đạt |
| 42 | `?from=2026-01-01&to=2028-01-01` | `400` | `to`: must be less than 24 months after from | đạt |
| 42b | `?from=2026-01-01&to=2027-12-31` (đúng 24 tháng trừ một ngày, đối chứng) | `200` | được chấp nhận | đạt |
| 43 | `?from=abc&to=` | `400` | `from`: must be a calendar date in the form YYYY-MM-DD; `to`: must be present | đạt |
| 44 | không `Authorization` | `401` | `urn:rikkaus:problem:unauthorized` | đạt |
| 45 | token hết hạn | `401` | giống 44 trừ `correlationId`: True | đạt |
| 46 | token ký bằng secret khác | `401` | giống 44 trừ `correlationId`: True | đạt |
| 47 | `POST` và `DELETE` | `405/405` | `urn:rikkaus:problem:method-not-allowed`, `Allow: GET` / `Allow: GET` | đạt |
| 48 | `Accept: application/xml` | `406` | `urn:rikkaus:problem:not-acceptable` | đạt |
| 49 | cả năm 2026 sau khi khởi động lại với `-Duser.timezone=Pacific/Pago_Pago` | `200` | sha256 trước `9cab73204337b53d…`, sau `9cab73204337b53d…`; case 1 lặp lại: 2026-01-31 s1, 2026-02-28 s2 dời, 2026-03-31 s3, 2026-04-30 s4 dời | đạt |
| 50 | `openapi.json` | `—` | method ['get'], from/to required [True, True], format ['date', 'date'], 2 ví dụ 200 | đạt |
| 51 | `pg_tables` của `wealth` và `flyway_schema_history` | `—` | bảng `asset_valuations,assets,cash_flow_entries,goals,liabilities,refresh_tokens,user_settings,users`; version cao nhất `9` | đạt |

| Status | Số dòng |
|---|---|
| `200` (một hoặc nhiều request, kể cả 18, 31 và 49) | 33 |
| `204` / `200` (case 32) | 1 |
| `400` `validation-failed` | 12 |
| `401` | 3 |
| `405` (hai request) | 1 |
| `406` | 1 |
| Không HTTP (50 đọc `openapi.json`, 51 đọc PostgreSQL) | 2 |

Case 49 ghi chú: log của lần khởi động thứ hai in giờ `2026-11-14T00:54:38-11:00`, tức JVM thực sự chạy ở
UTC−11 trong khi lần đầu chạy ở `+07:00`. Ở thời điểm chạy, hai múi giờ vẫn cùng ngày lịch 14/11; điều case
này chứng minh là kết quả không đổi khi múi giờ và giờ địa phương của máy chủ đổi, còn việc không có phép
tính nào dùng "hôm nay" thì đến từ mã nguồn (`CashFlowSchedule` không nhận `Clock`).

## Body thô chọn lọc

Case 20 — một ngày `2026-02-28`, ba kỳ đều dời, sắp theo `sourceEntryId` dạng chuỗi:

```json
[{"sourceEntryId":"67928f6f-7c25-4f3d-9dbb-f8167dda5978","occurrenceDate":"2026-02-28","sequence":2,"shiftedToMonthEnd":true,"entryType":"INCOME","category":"SALARY","amount":45000000.00,"currency":"VND","recurrence":"MONTHLY"},{"sourceEntryId":"75306014-25c9-4c2a-a0c3-994471ce16f2","occurrenceDate":"2026-02-28","sequence":2,"shiftedToMonthEnd":true,"entryType":"EXPENSE","category":"EDUCATION","amount":12500000.00,"currency":"VND","recurrence":"QUARTERLY"},{"sourceEntryId":"c290aea6-82c0-489f-aad2-e8eba6c7f06a","occurrenceDate":"2026-02-28","sequence":3,"shiftedToMonthEnd":true,"entryType":"EXPENSE","category":"HOUSING","amount":18000000.00,"currency":"VND","recurrence":"MONTHLY"}]
```

Case 23 — tháng 4/2026, cổ tức USD giữ `150.00` và `"USD"`, không có số VND:

```json
[{"sourceEntryId":"1dd995d2-d191-4e7b-a5f2-024c39d47f0e","occurrenceDate":"2026-04-01","sequence":1,"shiftedToMonthEnd":false,"entryType":"INCOME","category":"DIVIDEND","amount":150.00,"currency":"USD","recurrence":null},{"sourceEntryId":"67928f6f-7c25-4f3d-9dbb-f8167dda5978","occurrenceDate":"2026-04-30","sequence":4,"shiftedToMonthEnd":true,"entryType":"INCOME","category":"SALARY","amount":45000000.00,"currency":"VND","recurrence":"MONTHLY"},{"sourceEntryId":"c290aea6-82c0-489f-aad2-e8eba6c7f06a","occurrenceDate":"2026-04-30","sequence":5,"shiftedToMonthEnd":false,"entryType":"EXPENSE","category":"HOUSING","amount":18000000.00,"currency":"VND","recurrence":"MONTHLY"}]
```

Case 38 — định dạng sai, body không lặp lại `31/01/2026`:

```json
{"detail":"Request validation failed.","instance":"/api/v1/cash-flow-occurrences","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"from","message":"must be a calendar date in the form YYYY-MM-DD"}],"correlationId":"d9173737-40df-4453-9e6e-a20db913ceb7"}
```

Case 43 — hai lỗi trong một lần trả về:

```json
{"detail":"Request validation failed.","instance":"/api/v1/cash-flow-occurrences","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"from","message":"must be a calendar date in the form YYYY-MM-DD"},{"field":"to","message":"must be present"}],"correlationId":"480a5fed-3a64-4e8d-9315-dafbc3ee33a5"}
```

Case 44 — không token:

```json
{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized","correlationId":"dfba4fe1-152e-4022-9395-6a2accdf363f"}
```

Case 47 — `POST`, `Allow: GET`:

```json
{"detail":"Method 'POST' is not supported.","instance":"/api/v1/cash-flow-occurrences","status":405,"title":"Method not allowed","type":"urn:rikkaus:problem:method-not-allowed","correlationId":"589472c8-40fb-485c-85ae-d470e4c602cd"}
```

Các body thô này được lấy ngay sau lần chạy chính, trên cùng database, trước khi khởi động lại cho case 49.
Vì case 31–32 đã sửa E2 và xóa E3, case 23 ở đây không có kỳ tháng 5–6 của E2; điều đó khớp case 31.

## Điều lần chạy này không chứng minh

- Không có automated test: một regression sau này chỉ bị bắt khi chạy lại guide (xem mục "Verification gap"
  của plan).
- Hiệu năng với catalog lớn không được đo. Một user pilot có hàng chục entry; khoảng tối đa 24 tháng giới hạn
  mỗi entry tháng ở 24 kỳ.
- CI trên GitHub chưa chạy cho nhánh tại thời điểm viết; gate được chạy cục bộ.
