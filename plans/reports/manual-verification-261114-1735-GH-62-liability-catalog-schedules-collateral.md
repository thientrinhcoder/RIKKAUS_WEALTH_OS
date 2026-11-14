# Manual verification — liability catalog, schedules & collateral (#62)

Ngày 14/11/2026. Lượt 1 lúc 17:53 ICT trên working tree chưa commit; lượt 2 lúc 18:00 ICT trên commit
`f26f36b` của nhánh `thientrinhcoder/feat/issue-62-liability-catalog-schedules-collateral`, cắt từ
`main` tại `6e57e92`.

Đây là evidence hành vi của issue #62. Product Owner chỉ đạo ticket ship **không có unit/integration
test mới**; thay vào đó danh sách scenario được viết trước khi code
(`plans/261114-1735-GH-62-liability-catalog-schedules-collateral/phase-01-test-scenarios.md`), rồi chạy
thật qua HTTP vào PostgreSQL Docker. Mọi status và body dưới đây copy từ output thật của lượt 2; quy
trình tái hiện là [`docs/manual-test-guide-liabilities.md`](../../docs/manual-test-guide-liabilities.md).

**Đọc file này thành hai phần.** Lượt 1 có 3 chỗ không như mong đợi: 2 do harness chạy sai, 1 là một
điểm bất nhất thật của API đã được sửa. Toàn bộ danh sách được chạy lại trên commit cuối và lượt 2 là
phần ghi kết quả chính thức. Lượt 1 được ghi lại ở cuối thay vì lặng lẽ nới danh sách.

## Môi trường

| | |
|---|---|
| Database | Container `rikkaus-pg-issue-62`, `postgres:18.6-alpine3.24`, `127.0.0.1:5462`, tạo mới cho lượt 2 và xóa sau khi xong; volume `rikkaus-postgres-data` không bị chạm |
| Application | Jar đóng gói từ `f26f36b`, JDK 25, `--spring.profiles.active=local --server.port=8062`, `DB_URL=jdbc:postgresql://localhost:5462/rikkaus` |
| Migration | Flyway: `Successfully applied 5 migrations to schema "public", now at version v6` (V1–V4 và V6; V5 thuộc PR #147 chưa merge). `spring.jpa.hibernate.ddl-auto: validate` chấp nhận entity, context start sau 4,153 s — đây là bằng chứng `V6__liabilities.sql` và `Liability` khớp từng cột (S01) |
| Users | Hai row insert trực tiếp: A `0f2c8d14-…0c93` / `an.nguyen@example.com`, B `7b41e590-…a842` / `bich.tran@example.com` |
| Assets | Qua `POST /assets` thật: A có `Căn hộ Thảo Điền` (6.100.000.000 ₫) và `Xe Volvo XC60` (1.850.000.000 ₫); B có `Nhà phố Gò Vấp` (4.200.000.000 ₫) |
| Tokens | HS256 mint bằng secret dev của profile `local`, đúng claim set của `AccessTokenService`; thêm một token hết hạn và một token ký sai secret. Mọi request đi qua `BearerTokenAuthenticationFilter` thật |
| Build gate | `./services/api/mvnw -f services/api/pom.xml verify` xanh **không** dùng `-Dopenapi.update`: 50 integration test, `BUILD SUCCESS` |

## Kết quả lượt 2 — 95/95 case đúng như thiết kế

| Nhóm | Số case | Kết quả |
|---|---|---|
| HTTP (C, R, V, E, D, K, L, O, U, A, P, X) | 82 request + X05 (40 request đồng thời trong 10 vòng) | tất cả đúng |
| PostgreSQL trực tiếp (S01–S12) | 12 | tất cả đúng |

Phân bố status của 82 request HTTP:

| Status | Số request |
|---|---|
| `200` | 13 |
| `201` | 21 |
| `204` | 2 |
| `400` | 33 |
| `401` | 3 |
| `404` | 6 |
| `405` | 2 |
| `406` | 1 |
| `415` | 1 |

Không có `500` nào trong 122 request HTTP của lượt 2.

## C — 6 category, 4 tần suất

C01–C06 mỗi category một `201` với `Location: /api/v1/liabilities/<id>`. C01, vay mua nhà có đủ mọi
trường và tài sản bảo đảm; lãi gửi `8.5` đọc về `8.5000` ngay ở 201:

```
{"id":"83e795cf-c0b3-45b3-a194-32997f9f159f","name":"Vay mua nhà Vietcombank","category":"BANK_LOAN","outstandingBalance":2400000000.00,"currency":"VND","interestRate":8.5000,"startDate":"2024-03-15","maturityDate":"2044-03-15","recurringPaymentAmount":23500000.00,"recurringPaymentFrequency":"MONTHLY","nextPaymentDate":"2026-12-05","collateralAssetId":"7b5a7720-649d-4059-87c9-742198e15df1","notes":"Thế chấp căn hộ Thảo Điền.","createdAt":"2026-11-14T11:00:54.961575Z","updatedAt":"2026-11-14T11:00:54.961575Z"}
```

C03, vay ký quỹ không có đáo hạn và lịch — trường thiếu đọc về là `null`, không phải `0` hay `""`:

```
{"id":"639d474a-fbca-484d-88fe-743eeaa971d4","name":"Dư nợ ký quỹ SSI","category":"MARGIN_LOAN","outstandingBalance":350000000.00,"currency":"VND","interestRate":12.5000,"startDate":null,"maturityDate":null,"recurringPaymentAmount":null,"recurringPaymentFrequency":null,"nextPaymentDate":null,"collateralAssetId":null,"notes":null,"createdAt":"2026-11-14T11:00:55.011691Z","updatedAt":"2026-11-14T11:00:55.011691Z"}
```

| # | Request | Status | Kết quả |
|---|---|---|---|
| C04 | thẻ tín dụng Techcombank 38.500.000 ₫, chỉ `nextPaymentDate` | `201` | lịch định kỳ `null`, ngày trả `2026-11-25` |
| C05 | bảo lãnh 10.000 USD | `201` | `"currency":"USD"`, **không có số VND quy đổi nào trong body** |
| C06 | nợ người thân, `YEARLY` 50.000.000 ₫ | `201` | |
| C07 | chỉ 4 trường bắt buộc | `201` | 9 trường optional đều `null` |
| C08 / C09 / C10 | `QUARTERLY` / `SEMIANNUAL` / `YEARLY` | `201` | cả 4 giá trị tần suất đều nhận |
| C11 | `1250.50` USD | `201` | |

## R — thiếu từng trường bắt buộc

| # | Request | Status | `errors` |
|---|---|---|---|
| R01 | không `name` | `400` | `{field: name, message: "must not be blank"}` |
| R02 | không `category` | `400` | `{field: category, message: "must not be null"}` |
| R03 | không `outstandingBalance` | `400` | `{field: outstandingBalance, message: "must not be null"}` |
| R04 | không `currency` | `400` | `{field: currency, message: "must not be null"}` |
| R05 | `"name":"   "` | `400` | `{field: name, message: "must not be blank"}` |
| R06 | `"name":" "` | `400` | `{field: name, message: "must not be blank"}` — một non-breaking space bị từ chối, không tạo ra dòng rỗng |

Mỗi case đúng một entry.

## V — giá trị

| # | Request | Status | Kết quả |
|---|---|---|---|
| V01 | `outstandingBalance: -1.00` | `400` | `must not be negative` |
| V02 | `outstandingBalance: 100.123` | `400` | `must have at most 17 integer digits and 2 decimals` |
| V03 | `outstandingBalance: 0.00` | `201` | thẻ đã tất toán là bản ghi thật |
| V04 | `interestRate: -0.5` | `400` | `must not be negative` |
| V05 | `interestRate: 8.12345` | `400` | `must have at most 3 integer digits and 4 decimals` |
| V06 | `interestRate: 1000` | `400` | cùng thông điệp V05 |
| V07 | `interestRate: 0` | `201` | đọc về `"interestRate":0.0000` |
| V08 | `recurringPaymentAmount: 0` | `400` | `must be greater than zero` |
| V09 | amount không có frequency | `400` | `{field: recurringPaymentFrequency, message: "must be present when a recurring payment amount is given"}` |
| V10 | frequency không có amount | `400` | `{field: recurringPaymentAmount, message: "must be present when a recurring payment frequency is given"}` |
| V11 | `name` 121 ký tự | `400` | `must be at most 120 characters` |
| V12 | `notes` 2001 ký tự | `400` | `must be at most 2000 characters` |
| V13 | `"name":"Vay\u0000nhà"` | `400` | `{field: name, message: "must not contain a null character"}` |
| V14 | `notes` có khoảng trắng hai đầu | `201` | `"notes":"Ghi chú có khoảng trắng."` |

## E — enum lạ

E01 `CRYPTO_LOAN`, E02 `EUR`, E03 `WEEKLY`: cả ba `400` `urn:rikkaus:problem:malformed-request`, không có
mảng `errors`, cùng lý do đã ghi cho assets: Jackson không dựng được record nên bean validation chưa
chạy.

## D — ngày tháng

| # | Request | Status | Kết quả |
|---|---|---|---|
| D01 | start `2025-01-01`, maturity `2024-01-01` | `400` | `{field: maturityDate, message: "must not be before the start date"}` |
| D02 | start `2026-01-10`, next `2025-12-10` | `400` | `{field: nextPaymentDate, message: "must not be before the start date"}` |
| D03 | maturity `2028-01-10`, next `2028-02-10` | `400` | `{field: nextPaymentDate, message: "must not be after the maturity date"}` |
| D04 | start = maturity | `201` | |
| D05 | chỉ `nextPaymentDate` | `201` | |
| D06 | maturity trước start **và** next trước start | `400` | **hai** entry, không có entry thứ ba về next-sau-maturity |
| D07 | next `2026-01-05` trong khoảng, đã qua | `201` | quá hạn là trạng thái hợp lệ |
| D08 | `"startDate":"15/03/2024"` | `400` | `malformed-request` |

D06 nguyên văn:

```
{"detail":"Request validation failed.","instance":"/api/v1/liabilities","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be before the start date","field":"maturityDate"},{"message":"must not be before the start date","field":"nextPaymentDate"}],"correlationId":"bc616937-1d0c-4e04-b411-a9771bf16aa5"}
```

## K — tài sản bảo đảm

| # | Request | Status | Kết quả |
|---|---|---|---|
| K01 | A gắn `Xe Volvo XC60` của A | `201` | `collateralAssetId` echo đúng |
| K02 | A gắn `Nhà phố Gò Vấp` của B | `400` | `{field: collateralAssetId, message: "must identify an asset the signed-in user owns"}` |
| K03 | A gắn UUID không tồn tại | `400` | cùng body K02 |
| K04 | `"collateralAssetId":"khong-phai-uuid"` | `400` | `malformed-request` |
| K06 | `PUT` gắn asset của A vào vay ký quỹ | `200` | `collateralAssetId` set |
| K07 | `PUT` bỏ `collateralAssetId` | `200` | `collateralAssetId: null` |
| K08 | `PUT` gắn asset của B | `400` | cùng body K02 |
| K05a | A `DELETE` `Căn hộ Thảo Điền` đang là collateral của vay mua nhà | `204` | asset resource không đổi hành vi |
| K05b | A `GET` vay mua nhà | `200` | `collateralAssetId: null`, mọi trường khác giữ nguyên, `updatedAt` không đổi |
| K09 | `SELECT collateral_asset_id` | | `NULL` trong bảng |

K02 và K03 cạnh nhau — giống nhau ở `type`, `title`, `status`, `detail`, `errors`, chỉ khác
`correlationId`. Không có gì trong body nói asset đó tồn tại hay không:

```
{"detail":"Request validation failed.","instance":"/api/v1/liabilities","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must identify an asset the signed-in user owns","field":"collateralAssetId"}],"correlationId":"7dd6cbfe-80f0-4343-869f-1cd12364d158"}
{"detail":"Request validation failed.","instance":"/api/v1/liabilities","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must identify an asset the signed-in user owns","field":"collateralAssetId"}],"correlationId":"7588f5d7-450e-43ea-aded-9cb1dafdf84d"}
```

K05b, công nợ sống sót sau khi tài sản bảo đảm bị xóa:

```
{"id":"83e795cf-c0b3-45b3-a194-32997f9f159f","name":"Vay mua nhà Vietcombank","category":"BANK_LOAN","outstandingBalance":2400000000.00,"currency":"VND","interestRate":8.5000,"startDate":"2024-03-15","maturityDate":"2044-03-15","recurringPaymentAmount":23500000.00,"recurringPaymentFrequency":"MONTHLY","nextPaymentDate":"2026-12-05","collateralAssetId":null,"notes":"Thế chấp căn hộ Thảo Điền.","createdAt":"2026-11-14T11:00:54.961575Z","updatedAt":"2026-11-14T11:00:54.961575Z"}
```

## L — liệt kê

L01, 18 dòng của A, ngày trả gần nhất lên đầu, `null` xếp cuối, cùng ngày thì theo tên:

```
2026-01-05  Vay quá hạn Agribank
2026-11-25  Thẻ tín dụng Techcombank
2026-11-25  Thẻ tín dụng VIB
2026-12-01  Thẻ tín dụng USD Citibank
2026-12-05  Vay mua nhà Vietcombank
2026-12-10  Vay tiêu dùng FE Credit
2026-12-31  Vay vốn lưu động BIDV
2027-01-15  Nợ người thân mua xe
2027-03-01  Vay mua xe VPBank
2027-09-01  Vay học phí người thân
null  Bảo lãnh hợp đồng thiết bị
null  Bảo lãnh một ngày
null  Dư nợ ký quỹ SSI
null  Khoản phải trả đối tác
null  Thẻ tín dụng đã tất toán
null  Vay không lãi từ cha mẹ
null  Vay sửa nhà
null  Vay thế chấp xe
```

L02, B: `200` `[]`.

## O — ownership

| # | Request | Status | Kết quả |
|---|---|---|---|
| O01 | A đọc công nợ của A | `200` | |
| O02 | B đọc công nợ của A | `404` | `not-found` |
| O03 | B đọc `00000000-0000-4000-8000-000000000000` | `404` | `not-found`, body giống O02 trừ `instance` và `correlationId` |
| O04 | B `PUT` công nợ của A | `404` | A đọc lại (O04b) không đổi |
| O05 | B `DELETE` công nợ của A | `404` | `count(*)` của row = 1 |

```
{"detail":"Resource not found.","instance":"/api/v1/liabilities/83e795cf-c0b3-45b3-a194-32997f9f159f","status":404,"title":"Resource not found","type":"urn:rikkaus:problem:not-found","correlationId":"a3db389a-4a0a-4641-9afa-b3c3291391f5"}
{"detail":"Resource not found.","instance":"/api/v1/liabilities/00000000-0000-4000-8000-000000000000","status":404,"title":"Resource not found","type":"urn:rikkaus:problem:not-found","correlationId":"213235f7-ed89-4cfe-8484-8707c7550089"}
```

## U — sửa

| # | Request | Status | Kết quả |
|---|---|---|---|
| U01 | thay toàn bộ trường, dư nợ còn 96.000.000 ₫, lãi `17.5`, gắn collateral | `200` | mọi trường đổi, `interestRate: 17.5000`, `updatedAt` tiến, `createdAt` giữ |
| U02 | body có `"category":"OTHER_LIABILITY"` | `200` | `category` vẫn `PERSONAL_LOAN` |
| U03 | bỏ lãi, lịch, collateral, notes | `200` | bốn nhóm thành `null`; U03b đọc lại giống hệt — xóa thật trong storage |
| U04 | thiếu `name` | `400` | `{field: name, message: "must not be blank"}` |
| U05 | maturity trước start | `400` | `{field: maturityDate, message: "must not be before the start date"}` |
| U06 | đổi sang `4000.00` USD | `200` | `currency` và `outstandingBalance` đổi |

## A — xác thực

A01 không header, A02 hết hạn, A03 sai chữ ký: cả ba `401` `urn:rikkaus:problem:unauthorized`, body chỉ
khác `correlationId`:

```
{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized","correlationId":"18b99415-8a92-4884-8eea-f502cd1c95b7"}
```

## P — protocol

| # | Request | Status | `type` |
|---|---|---|---|
| P01 | `GET /liabilities/not-a-uuid` | `400` | `malformed-request` |
| P02 | `PATCH /liabilities/<id>` | `405` | `method-not-allowed` |
| P03 | `DELETE /liabilities` | `405` | `method-not-allowed` |
| P04 | body `text/plain` | `415` | `unsupported-media-type` |
| P05 | `{"name": ` | `400` | `malformed-request` |
| P06 | `Accept: application/xml` | `406` | `not-acceptable` |

Cả sáu là `application/problem+json`.

## X — xóa

| # | Request | Status |
|---|---|---|
| X01 | `DELETE` nợ người thân | `204`, không body |
| X02 | `GET` lại | `404` |
| X03 | `DELETE` lần hai | `404` |
| X04 | `GET /liabilities` | `200`, 17 dòng (18 − 1) |

X05, bốn `DELETE` đồng thời cùng một id, mười vòng, mỗi vòng một bản ghi mới:

```
round 1: 204 404 404 404
round 2: 204 404 404 404
round 3: 204 404 404 404
round 4: 204 404 404 404
round 5: 204 404 404 404
round 6: 204 404 404 404
round 7: 204 404 404 404
round 8: 204 404 404 404
round 9: 204 404 404 404
round 10: 204 404 404 404
```

40 request, 10 `204`, 30 `404`, không `500`.

## S — đọc thẳng PostgreSQL

S02, kiểu cột:

```
outstanding_balance         | numeric | 19 | 2 | NO
interest_rate               | numeric | 7  | 4 | YES
recurring_payment_amount    | numeric | 19 | 2 | YES
start_date / maturity_date / next_payment_date | date
collateral_asset_id         | uuid | YES
```

Không có `double precision`.

S03/S04, constraint — đủ 11 CHECK có tên, FK collateral trỏ `wealth.assets` với `confdeltype = n`
(SET NULL), FK owner trỏ `wealth.users` với `c` (CASCADE):

```
liabilities_category_is_known | c
liabilities_collateral_asset_id_fkey | f | wealth.assets | n
liabilities_currency_is_supported | c
liabilities_interest_rate_is_not_negative | c
liabilities_maturity_not_before_start | c
liabilities_name_is_present_and_bounded | c
liabilities_next_payment_within_term | c
liabilities_notes_are_bounded | c
liabilities_outstanding_balance_is_not_negative | c
liabilities_recurring_payment_amount_is_positive | c
liabilities_recurring_payment_frequency_is_known | c
liabilities_recurring_payment_is_complete | c
liabilities_user_id_fkey | f | wealth.users | c
```

S05–S11, bảy `INSERT` bỏ qua API, mỗi lệnh bị từ chối đúng tên constraint, `count(*)` của dòng probe =
0:

```
violates check constraint "liabilities_category_is_known"
violates check constraint "liabilities_outstanding_balance_is_not_negative"
violates check constraint "liabilities_currency_is_supported"
violates check constraint "liabilities_maturity_not_before_start"
violates check constraint "liabilities_recurring_payment_is_complete"
violates check constraint "liabilities_interest_rate_is_not_negative"
violates check constraint "liabilities_name_is_present_and_bounded"
```

S12, giá trị lưu nguyên vẹn:

```
Vay mua nhà Vietcombank | 2400000000.00 | 8.5000 | 23500000.00
```

## Lượt 1 — những gì không như mong đợi và đã làm gì

Lượt 1 chạy lúc 17:53 ICT trên working tree trước commit `f26f36b`, cùng danh sách case. 79/82 request
HTTP và 12/12 case PostgreSQL đúng như thiết kế. Ba điểm còn lại:

| Case | Lượt 1 trả | Nguyên nhân | Xử lý |
|---|---|---|---|
| P04 `text/plain` | `400` `malformed-request` thay vì `415` | **Lỗi harness**: helper gửi hai header `Content-Type` (JSON mặc định rồi `text/plain`), server đọc JSON. Gọi tay với một header duy nhất trả `415` ngay trên build lượt 1, cả `/liabilities` và `/assets` | Sửa helper (biến `CT` ghi đè); lượt 2 `415` |
| P06 `Accept: application/xml` | `200` | **Lỗi harness**: helper nhận `-H` làm body nên header Accept không được gửi. Gọi tay trả `406` trên build lượt 1 | Sửa helper (body `''` tường minh); lượt 2 `406` |
| C01, C02, U01, K06 — `interestRate` | `201` echo `8.5`, `18`, `17.5`, `12.5`; `GET` cùng bản ghi trả `8.5000`… | **Bất nhất thật của API**: response của POST/PUT phản chiếu giá trị trong bộ nhớ, GET phản chiếu `NUMERIC(7, 4)`. Hai số bằng nhau về giá trị, nhưng hai document không bằng nhau. Slice `assets` có cùng hành vi với `currentValue` (echo như gửi) | `Liability.apply` đưa `outstandingBalance`, `interestRate`, `recurringPaymentAmount` về scale cột (`setScale`, `HALF_UP` — không bao giờ làm tròn vì `@Digits` đã từ chối giá trị mịn hơn). Lượt 2: `8.5000` ở cả 201 và GET |

Không case nào trong lượt 1 trả `500`. API không có defect hành vi ở lượt 1 ngoài điểm bất nhất scale;
hai case protocol là harness.

## Quan sát (không phải defect, ghi để người đọc biết)

- `detail` của lỗi bean-validation là `"Invalid request content."` (text của Spring) trong khi lỗi do
  service ném là `"Request validation failed."` (text của taxonomy). Đã ghi nhận từ #39 và trong report
  của #53; frontend switch theo `type`, không theo `detail`.
- `detail` của `405`/`406`/`415` phản chiếu method/media type của client (`Method 'PATCH' is not
  supported.`). Đây là hành vi sẵn có của `ApiExceptionHandler` cho mọi resource, không riêng slice này.
- Lượt 1 và 2 chạy khi migration còn là `V6__liabilities.sql`; Flyway báo `applied 5 migrations ... now at
  version v6`. Khi merge, `main` đã có `V6__asset_valuations.sql` (#59) và không có V5, nên migration được
  đánh số lại thành `V7__liabilities.sql` và chạy lại lượt 3 (dưới đây).
- `assets` echo create body như gửi (`20000.00` nếu gửi `20000.00`, `20000` nếu gửi `20000`), còn
  `liabilities` echo ở scale cột. Đồng nhất hóa `assets` là follow-up ngoài phạm vi #62.

## Lượt 3 — sau khi merge `main` (`74829ee`, có #59 asset valuation history)

Chạy lúc 18:15 ICT trên tree đã merge, migration đổi thành `V7__liabilities.sql`, DB mới, cùng script.

| | |
|---|---|
| Migration | `Successfully applied 6 migrations to schema "public", now at version v7`; `ddl-auto: validate` chấp nhận, context start 3,625 s |
| `verify` | `clean verify` không update flag: 81 unit + 50 IT, `BUILD SUCCESS` |
| HTTP | chuỗi status của 82 request **giống hệt lượt 2, từng case** |
| X05 | 10/10 vòng `204 404 404 404` |
| K05/K09 | xóa asset (giờ cascade sang `asset_valuations`) vẫn `204`; công nợ còn, `collateral_asset_id` = `NULL` |
| S05–S11 | 0 dòng probe; S12 `2400000000.00 | 8.5000 | 23500000.00` |
| `500` | 0 |

Lần `verify` đầu sau merge fail 50/50 IT với `Found more than one migration with version 6`: file
`V6__liabilities.sql` cũ còn trong `target/classes` vì build không `clean`. Không phải lỗi code; `clean
verify` xanh.

## Verification gap

Không automated test nào gọi 5 route của resource này. `verify` xanh (50 IT) chứng minh: contract khớp
ứng dụng, inventory bảng và path đúng, ArchUnit không thấy import chéo slice hay field tiền kiểu float,
context khởi động với `ddl-auto: validate`. Hành vi của resource chỉ được chứng minh bằng file này và
chỉ được bảo vệ khỏi regression bằng việc chạy lại guide.

## Dọn dẹp

API (pid ghi trong scratchpad) đã tắt và container `rikkaus-pg-issue-62` đã xóa sau khi file này được
viết; port 8062 và 5462 được trả lại.
