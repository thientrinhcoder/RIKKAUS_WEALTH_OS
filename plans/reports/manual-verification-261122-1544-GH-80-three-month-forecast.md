# Xác minh thủ công — dự báo dòng tiền ba tháng (#80)

Ngày: 2026-11-22. Nhánh `thientrinhcoder/feat/issue-80-three-month-forecast`, mã nguồn là commit `08a0ba4`
(cắt từ `main` `abadb96`). Commit chứa file này chỉ thêm tài liệu, không đổi code.

| Lần | Giờ (ICT) | Mã nguồn | Database | Số case | Đạt |
|---|---|---|---|---|---|
| 1 | 15:55 | cây làm việc chưa commit, cùng nội dung với `08a0ba4` | container mới | 55 | **55** |
| 2 | 15:57 | `08a0ba4` | container tạo lại từ đầu | 55 | **55** |

Không case nào fail; không có case fail rồi được sửa. Hai lần cho cùng kết quả từng case. Kỳ vọng là bảng
tính tay viết trước khi code ở
[`phase-01-test-scenarios.md`](../261122-1544-GH-80-three-month-forecast/phase-01-test-scenarios.md);
quy trình tái hiện ở [`docs/manual-test-guide-cash-flow-forecast.md`](../../docs/manual-test-guide-cash-flow-forecast.md).

Một lỗi contract được bắt **trước** lần chạy 1, khi đọc diff `openapi.json` sau lần regenerate đầu: record
lồng tên `ExchangeRateResponse` trùng tên schema của slice `settings`, nên springdoc ghi đè schema đó, và
method tên `read` làm dịch `operationId` của một route khác (`read_7`). Đã đổi mọi record lồng sang tiền tố
`CashFlowForecast…` và method thành `forecast`; diff `openapi.json` sau đó chỉ còn dòng thêm (663 dòng, 0
dòng xóa).

Product Owner chỉ đạo task này ship **không có unit test hay integration test mới**. Status và bằng chứng
dưới đây sinh từ lần chạy 2, không viết tay.

## Môi trường

| | |
|---|---|
| Database | `postgres:18.6-alpine3.24` dùng một lần, `rikkaus-pg-issue-80`, `127.0.0.1:5480`; tạo mới cho mỗi lần chạy, đã xóa sau lần 2 |
| Ứng dụng | `spring-boot:run` trên JDK 25, `--spring.profiles.active=local --server.port=8180`, `DB_URL=jdbc:postgresql://localhost:5480/rikkaus`; đã tắt |
| Migration | Flyway `Successfully applied 10 migrations`, `now at version v12`. #80 không thêm migration |
| Người dùng, token | A `0f2c8d14-…0c93` (An Nguyễn), B `7b41e590-…a842` (Bích Trần), C `a3e9c5d2-…3a55` (Chi Lê) chèn thẳng; token HS256 mint lúc chạy bằng secret của profile `local` đọc từ `application-local.yml`, cộng một token hết hạn và một token ký bằng secret khác; request đi qua `BearerTokenAuthenticationFilter` thật. Không token nào được ghi ra file |
| Dữ liệu | đúng bảng dữ liệu phase 1, tạo qua `POST /assets`, `/liabilities`, `/cash-flow-entries` và `PUT /settings/exchange-rate` |
| Cách chạy | một script Python gửi đúng các request của guide, so kết quả với kỳ vọng phase 1 và in bảng dưới; so số bằng `Decimal`, không float |
| Gate build | `./services/api/mvnw -f services/api/pom.xml clean verify` xanh **không** update flag trên cây của `08a0ba4`: 81 unit test, 50 integration test, `BUILD SUCCESS` |

## Kết quả từng case

Mã E1, L1, AS1… là mã ở bảng dữ liệu phase 1. Số tiền in từ body đã parse; byte thật trên dây có dạng
`45000000.00`, xem case 55 và mục body thô.

| # | Request | Status | Bằng chứng | Kết quả |
|---|---|---|---|---|
| 1 | A, không `from` | `200` | horizon 2026-12-01..2027-02-28 ['2026-12', '2027-01', '2027-02']; asOf 2026-11-22; notProjected {'from': '2026-11-23', 'to': '2026-11-30'} | đạt |
| 2 | A, `from=2026-12` | `200` | sha256 5c47c7217e20e267 = 5c47c7217e20e267 | đạt |
| 3 | A, `from=2027-01` | `200` | horizon 2027-01-01..2027-03-31; notProjected {'from': '2026-11-23', 'to': '2026-12-31'}; mở đầu 147200000.00 áp 2027-01-01 | đạt |
| 4 | A, `from=2027-11` | `200` | horizon 2027-11-01..2028-01-31 | đạt |
| 5 | E1 tháng | `200` | 2026-12-05 s12; 2027-01-05 s13; 2027-02-05 s14 | đạt |
| 6 | E4 quý | `200` | 2027-02-28 s3 dời | đạt |
| 7 | E5 năm | `200` | 2027-01-25 s2 | đạt |
| 8 | E3 endDate giữa horizon | `200` | 2026-12-05 s10; 2027-01-05 s11 | đạt |
| 9 | E6 năm ngoài horizon | `200` | items 0; loại [('CASH_FLOW_ENTRY', 'NO_OCCURRENCE_IN_HORIZON')] | đạt |
| 10 | E7 USD quý | `200` | 2026-12-15 s1 150.00 USD → 3765000.00 | đạt |
| 11 | E8 một lần tương lai | `200` | 2027-01-18 s1 cadence None | đạt |
| 12 | E10 một lần quá khứ | `200` | items 0, loại 0 | đạt |
| 13 | E2 tháng | `200` | 2026-12-01 s12; 2027-01-01 s13; 2027-02-01 s14 | đạt |
| 14 | L1 tháng đến 2041 | `200` | 2026-12-15 s1; 2027-01-15 s2; 2027-02-15 s3 | đạt |
| 15 | L2 kế 25/01/2027 | `200` | 2027-01-25 s1; 2027-02-25 s2 | đạt |
| 16 | L3 đáo hạn 10/01/2027 | `200` | 2026-12-10 s1; 2027-01-10 s2 | đạt |
| 17 | L4 không lịch, đáo hạn 31/01/2027 | `200` | items 0; loại [('NO_PAYMENT_SCHEDULE', ['2027-01'])]; partial [False, True, False] | đạt |
| 18 | L6 không lịch, không đáo hạn | `200` | loại [('NO_PAYMENT_SCHEDULE', [])] | đạt |
| 19 | L5 có E9 liên kết | `200` | items L5 0; loại [('COVERED_BY_LINKED_ENTRIES', ['E9'])]; E9 [('2026-12-20', 8, False), ('2027-01-20', 9, False), ('2027-02-20', 10, False)] | đạt |
| 20 | L7 quý, kế 01/04/2027 | `200` | loại ['NO_PAYMENT_IN_HORIZON'] | đạt |
| 21 | `openingBalance` A | `200` | 147200000.00 áp 2026-12-01; [('AS1', 85000000.00, 'VND', 85000000.00), ('AS2', 12000000.00, 'VND', 12000000.00), ('AS3', 2000.00, 'USD', 50200000.00)] | đạt |
| 22 | tháng 2026-12 của A | `200` | thu/chi/net/đầu/cuối/thấp nhất/ngày/partial = 48765000.00 / 71700000.00 / -22935000.00 / 147200000.00 / 124265000.00 / 122200000.00 / 2026-12-01 / False | đạt |
| 23 | tháng 2027-01 của A | `200` | thu/chi/net/đầu/cuối/thấp nhất/ngày/partial = 105000000.00 / 90000000.00 / 15000000.00 / 124265000.00 / 139265000.00 / 89065000.00 / 2027-01-20 / True | đạt |
| 24 | tháng 2027-02 của A | `200` | thu/chi/net/đầu/cuối/thấp nhất/ngày/partial = 45000000.00 / 72800000.00 / -27800000.00 / 139265000.00 / 111465000.00 / 111465000.00 / 2027-02-28 / False | đạt |
| 25 | `minimumBalance` A | `200` | {'date': '2027-01-20', 'month': '2027-01', 'amount': 89065000.00, 'negative': False, 'referenceThreshold': 78166666.67, 'status': 'ADEQUATE'}; partial gốc True | đạt |
| 26 | số khoản và tổng | `200` | số khoản [7, 9, 6]; tổng khoản = tổng tháng: True | đạt |
| 27 | thứ tự | `200` | 15/12 ['E7', 'L1']; 25/01 ['E5', 'L2']; 05/12 theo id ['E1', 'E3']; cả body đúng thứ tự: True | đạt |
| 28 | `excludedSources` A | `200` | [('CASH_FLOW_ENTRY', 'E6', 'NO_OCCURRENCE_IN_HORIZON'), ('LIABILITY', 'L5', 'COVERED_BY_LINKED_ENTRIES'), ('LIABILITY', 'L4', 'NO_PAYMENT_SCHEDULE'), ('LIABILITY', 'L6', 'NO_PAYMENT_SCHEDULE'), ('LIABILITY', 'L7', 'NO_PAYMENT_IN_HORIZON')] | đạt |
| 29 | `assumptions` A | `200` | exchangeRate {'usdToVndRate': 25100.000000, 'effectiveDate': '2026-09-12'}; currency VND; categories ['BANK_ACCOUNT', 'CASH'] | đạt |
| 30 | GET từng source | `200` | 25 lần GET (22 khoản + 3 nguồn mở đầu), tất cả 200 và trùng số tiền: True | đạt |
| 31 | xóa E5 rồi gọi lại | `DELETE 204, GET 200` | E5 còn 0 khoản; 01/2027 thu 45000000.00 net -45000000.00 cuối 79265000.00 thấp nhất 79265000.00 2027-01-25; 02/2027 cuối 51465000.00; min 51465000.00 2027-02-28 THIN | đạt |
| 32 | xóa E9 rồi gọi lại | `DELETE 204, GET 200` | L5 [('2026-12-20', 1, 'LIABILITY'), ('2027-01-20', 2, 'LIABILITY'), ('2027-02-20', 3, 'LIABILITY')]; L5 trong loại: False; tổng tháng không đổi: True | đạt |
| 33 | C chưa khai tỷ giá | `200` | exchangeRate None; mở đầu 0.00 []; loại [('CASH_FLOW_ENTRY', 'CE2', 'EXCHANGE_RATE_MISSING', ['2026-12']), ('ASSET', 'CS1', 'EXCHANGE_RATE_MISSING', [])]; 12/2026 partial True thu 25000000.00 chi 4300000.00; status INSUFFICIENT_DATA; partial True | đạt |
| 34 | CL1 neo 28/10/2026 | `200` | [('2026-12-28', 3, False), ('2027-01-28', 4, False), ('2027-02-28', 5, False)] | đạt |
| 35 | C khai 25.300 rồi gọi lại | `PUT 200, GET 200` | mở đầu 30360000.00; CE2 [3795000.00]; loại []; partial False; cuối tháng [54855000.00, 75555000.00, 96255000.00]; min 30360000.00 2026-12-01 ngưỡng 4300000.00 ADEQUATE | đạt |
| 36 | tháng 2026-12 của B | `200` | thu/chi/net/đầu/cuối/thấp nhất/ngày/partial = 30000000.00 / 26000000.00 / 4000000.00 / 8000000.00 / 12000000.00 / -18000000.00 / 2026-12-03 / False | đạt |
| 37 | tháng 2027-01 của B | `200` | thu/chi/net/đầu/cuối/thấp nhất/ngày/partial = 30000000.00 / 71000000.00 / -41000000.00 / 12000000.00 / -29000000.00 / -59000000.00 / 2027-01-08 / False | đạt |
| 38 | tháng 2027-02 của B | `200` | thu/chi/net/đầu/cuối/thấp nhất/ngày/partial = 30000000.00 / 26000000.00 / 4000000.00 / -29000000.00 / -25000000.00 / -55000000.00 / 2027-02-03 / False | đạt |
| 39 | `minimumBalance` B | `200` | {'date': '2027-01-08', 'month': '2027-01', 'amount': -59000000.00, 'negative': True, 'referenceThreshold': 41000000.00, 'status': 'NEGATIVE'} | đạt |
| 40 | B không thấy A | `200` | 5 sourceId, đều của B: True; mở đầu [('BS1', 8000000.00)] | đạt |
| 41 | B đọc E1 của A | `404` | urn:rikkaus:problem:not-found | đạt |
| 42 | `from=2026-13` | `400` | urn:rikkaus:problem:validation-failed [{'message': 'must be a month in the form YYYY-MM', 'field': 'from'}] | đạt |
| 43 | `from=2026-12-01` | `400` | urn:rikkaus:problem:validation-failed [{'message': 'must be a month in the form YYYY-MM', 'field': 'from'}] | đạt |
| 44 | `from=abc` | `400` | urn:rikkaus:problem:validation-failed [{'message': 'must be a month in the form YYYY-MM', 'field': 'from'}] | đạt |
| 45 | `from=` | `400` | urn:rikkaus:problem:validation-failed [{'message': 'must be a month in the form YYYY-MM', 'field': 'from'}] | đạt |
| 46 | `from=2026-11` | `400` | urn:rikkaus:problem:validation-failed [{'message': 'must not be before the month after today', 'field': 'from'}] | đạt |
| 47 | `from=2026-10` | `400` | urn:rikkaus:problem:validation-failed [{'message': 'must not be before the month after today', 'field': 'from'}] | đạt |
| 48 | `from=2027-12` | `400` | urn:rikkaus:problem:validation-failed [{'message': 'must be at most 12 months after the current month', 'field': 'from'}] | đạt |
| 49 | không token | `401` | urn:rikkaus:problem:unauthorized | đạt |
| 50 | token hết hạn | `401` | urn:rikkaus:problem:unauthorized | đạt |
| 51 | token ký sai | `401` | urn:rikkaus:problem:unauthorized | đạt |
| 52 | `POST` | `405` | urn:rikkaus:problem:method-not-allowed | đạt |
| 53 | `Accept: application/xml` | `406` | {"detail":"Acceptable representations: [application/json, application/*+json, ap | đạt |
| 54 | A gọi hai lần | `200, 200` | sha256 4f47365bcbbcf7f2 / 4f47365bcbbcf7f2 | đạt |
| 55 | body thô (case 1) | `200` | {'"amount":45000000.00': True, '"usdToVndRate":25100.000000': True, '"referenceThreshold":78166666.67': True} | đạt |

## Body thô

Đầu body case 1 (A, không `from`), nguyên byte:

```json
{"horizon":{"from":"2026-12-01","to":"2027-02-28","months":["2026-12","2027-01","2027-02"]},"openingBalance":{"amount":147200000.00,"appliedOn":"2026-12-01","sources":[{"sourceType":"ASSET","sourceId":"52f78056-f7a7-46a8-a28f-6da53486c9fa","category":"BANK_ACCOUNT","valuationDate":"2026-11-20","amou …
```

Phần tín hiệu của C trước khi khai tỷ giá (case 33):

```json
{
  "openingBalance": {
    "amount": "0.00",
    "appliedOn": "2026-12-01",
    "sources": []
  },
  "excludedSources": [
    {
      "sourceType": "CASH_FLOW_ENTRY",
      "sourceId": "29477357-0a24-45db-8d69-7619a1f519bd",
      "reason": "EXCHANGE_RATE_MISSING",
      "affectedMonths": [
        "2026-12"
      ],
      "linkedEntryIds": []
    },
    {
      "sourceType": "ASSET",
      "sourceId": "2f675117-1cd2-4c2b-a75d-54169517d863",
      "reason": "EXCHANGE_RATE_MISSING",
      "affectedMonths": [],
      "linkedEntryIds": []
    }
  ],
  "minimumBalance": {
    "date": "2026-12-01",
    "month": "2026-12",
    "amount": "0.00",
    "negative": false,
    "referenceThreshold": "4300000.00",
    "status": "INSUFFICIENT_DATA"
  },
  "partial": true
}
```

`sourceId` là UUID sinh trong lần chạy và mất cùng container.

## Verification gap

- Không automated test nào bảo vệ phép chiếu; regression chỉ bị bắt khi chạy lại guide.
- Lần chạy dùng script Python gửi cùng request với guide; các khối lệnh `bash` trong guide không được chạy
  nguyên văn.
- "Hôm nay" là đồng hồ hệ thống: chỉ ngày 22/11/2026 được chạy. Ngày cuối tháng (`notProjected: null`) và
  khoảnh khắc đổi tháng theo giờ Việt Nam không được chạy.
- Tần suất `SEMIANNUAL` và `YEARLY` của công nợ, công nợ bằng USD, và `NO_NEXT_PAYMENT_DATE` không có case
  riêng; chúng đi qua cùng vòng lặp và cùng đường quy đổi đã được chạy.
- Hiệu năng với catalog lớn không được đo.
