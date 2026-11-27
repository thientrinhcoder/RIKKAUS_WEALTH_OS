# Manual verification — GH-83 Financial-health indicators and insights

**Kết quả:** 20/20 kịch bản đạt, 59/59 phép kiểm PASS, 0 FAIL, chạy ngày 27/11/2026 (12:59–13:06 giờ Việt
Nam) qua HTTP vào API thật trên PostgreSQL 18.6 trong Docker; `./mvnw clean verify` xanh. Không có unit test
(chỉ đạo PO).

## Môi trường

| Mục | Giá trị |
|---|---|
| Nhánh | `claude/issue-83-financial-health-587f90`, cắt từ `main` `69f586e` |
| Bản jar | lần chạy chính dùng jar có toàn bộ mã của ticket với `FinancialHealthExamples` tạm; lần chạy lại cuối (13:06) dùng jar của `clean verify` có ví dụ thật, 49/49 PASS và body H01 byte-identical với lần đầu; sau đó chỉ cách kiểm danh mục thiết yếu trùng trong `FinancialHealthProperties` đổi (`EnumSet.size()` thay `count()`, cùng hành vi) và `clean verify` chạy lại xanh |
| JDK | `openjdk@25` (Homebrew) |
| Cơ sở dữ liệu | container dùng một lần `rikkaus-pg-83`, `postgres:18.6-alpine3.24`, `127.0.0.1:5483`, không volume, mật khẩu sinh bằng `openssl rand -hex 24` và chỉ nằm trong scratchpad quyền 600; đã xoá sau khi chạy |
| API | `java -jar … --spring.profiles.active=local --server.port=8083`, đăng nhập bằng `POST /api/v1/auth/dev-sign-in`; đã dừng |
| Ngày chạy | `D = 2026-11-27` (`Asia/Ho_Chi_Minh`); tháng mặc định `2026-11`, forecast `2026-12-01…2027-02-28`, cửa sổ nghĩa vụ `2026-11-27…2026-12-27` |
| Script | [`setup.sh`](../261127-1241-GH-83-financial-health/scripts/setup.sh), [`cases.sh`](../261127-1241-GH-83-financial-health/scripts/cases.sh), [`recon.jq`](../261127-1241-GH-83-financial-health/scripts/recon.jq) |

Không có token, secret hay mật khẩu nào được ghi vào repo: token lấy lại từ dev sign-in ở đầu mỗi lần chạy
`cases.sh` và không bao giờ in ra; mật khẩu PostgreSQL chỉ nằm trong scratchpad.

## Cách chạy lại

```bash
docker run -d --name rikkaus-pg-83 -e POSTGRES_USER=rikkaus -e POSTGRES_PASSWORD="$(openssl rand -hex 24)" -e POSTGRES_DB=rikkaus -p 127.0.0.1:5483:5432 postgres:18.6-alpine3.24
```

Khởi động API với `DB_URL=jdbc:postgresql://127.0.0.1:5483/rikkaus`, `POSTGRES_USER`, `POSTGRES_PASSWORD`,
`--spring.profiles.active=local --server.port=8083`, rồi:

```bash
OUT=/path/to/scratch bash plans/261127-1241-GH-83-financial-health/scripts/setup.sh
```

```bash
OUT=/path/to/scratch bash plans/261127-1241-GH-83-financial-health/scripts/cases.sh main
```

Dừng API, khởi động lại với `RIKKAUS_CASHFLOW_HEALTH_MINRESERVEMONTHS=5`,
`RIKKAUS_CASHFLOW_HEALTH_MAXINCOMESOURCESHARE=0.8` và
`RIKKAUS_CASHFLOW_HEALTH_ESSENTIALCATEGORIES=LIVING_COSTS,HOUSING,EDUCATION,HEALTHCARE,INSURANCE,TAX_AND_FEES,DEBT_PRINCIPAL_PAYMENT,INTEREST_PAYMENT`,
chạy `cases.sh custom`; dừng, khởi động lại không biến nào, chạy `cases.sh restored`. Các ngày trong
`setup.sh` là ngày tuyệt đối của lần chạy 27/11/2026; chạy vào ngày khác thì forecast và cửa sổ nghĩa vụ dời
theo hôm nay, nên các kỳ vọng phụ thuộc hai thứ đó phải tính lại.

## Từng kịch bản

Mọi request là `curl -s "$BASE/financial-health[?…]" -H "Authorization: Bearer $T_<user>"`, trừ khi ghi khác.
Body đầy đủ của H01, H10 và H13 là ba ví dụ OpenAPI trong `FinancialHealthExamples`.

| # | Kịch bản | Body thật | Kết quả |
|---|---|---|---|
| H01 | B (Cơ sở), không `month` | `200`; `assumptions` `{2026-11-27, Asia/Ho_Chi_Minh, 2026-11, forecast 2026-12-01…2027-02-28, obligationWindow 2026-11-27…2026-12-27, [CASH, BANK_ACCOUNT, TERM_DEPOSIT], 7 danh mục thiết yếu, rate 25100.000000 / 2026-09-12}`; `thresholds {6.0, 3.0, 0.250000, 0.350000, 0.500000, 0.200000, 0.700000, 0.800000, 30, 0.600000}` | PASS (2) |
| H02 | B, năm chỉ số | dự phòng `30.0` OK (`1850000000.00 / 61600000.00`; mẫu số: Bảo hiểm YEARLY `5400000 → 450000.00`, Thuế QUARTERLY `8250000 → 2750000.00`); trả nợ `0.090278` OK (`16250000.00 / 180000000.00`, không có dòng Nhà ở); số dư thấp nhất `626600000.00` ngày `2026-12-03` OK (tham chiếu `61600000.00`; nguồn: tiền mặt 148.000.000, USD 502.000.000, Chi phí sinh hoạt 23.400.000 ngày 03/12); thụ động `0.119806` LOW (`21565000.00 / 180000000.00`, Cổ tức `150 USD → 3765000.00`); thiết yếu `0.833696` ATTENTION (`76700000.00 / 92000000.00`, `SHARE_REST` = Giáo dục, Chi phí khác) | PASS (7) |
| H03 | B, sáu rule | chi vượt thu OK (`88000000.00`); forecast âm OK; dự phòng OK (`30.0` vs `3.0`, `min-reserve-months`); trả nợ OK (`0.090278` vs `0.350000`); thanh khoản OK (`650000000.00` vs `50750000.00` = thẻ đáo hạn 30/11 `32000000.00` + kỳ trả vay 20/12 `18750000.00`); tập trung thu **ATTENTION** `0.733333` `SALARY`, evidence = Lương 05/11 | PASS (5) |
| H04 | B, đối soát `recon.jq` với report tháng 11 và forecast đọc ngay sau | 27 phép đối soát đều `true` (tử/mẫu số 2, 4, 5 = `totals`, `passiveIncome.total`, `expenseComposition`; chỉ số 3 = `minimumBalance`; opening + đường đi = giá trị; tiền mặt rule 5 = `openingBalance.amount`; mọi nguồn dòng tiền là một occurrence của report) | PASS |
| H05 | B, `month=2026-11` và đọc lần hai | byte-identical với H01 | PASS (2) |
| H06 | B, `month=2026-10` | dự phòng `31.4` (mẫu số `58850000.00`, không có Thuế và phí vì bắt đầu 22/11); trả nợ `0.109575`; thụ động `0.109912`; thiết yếu `1.000000`; tập trung `0.890088`; chi vượt thu OK `89900000.00`; forecast giữ `626600000.00`; đối soát đúng | PASS (3) |
| H07 | B, `?userId=<id của S>` | byte-identical với H01 | PASS |
| H08 | W (Theo dõi) | dự phòng `4.7` WATCH; trả nợ `0.285088` WATCH; số dư thấp nhất `40000000.00` ngày `2026-12-01` WATCH (tham chiếu `42400000.00`, nguồn chỉ có số dư mở đầu); thụ động `0.210526` MEDIUM; thiết yếu `0.751773` WATCH; rule: OK ×5, tập trung ATTENTION `0.789474`; đối soát đúng | PASS (2) |
| H09 | S (Áp lực) | dự phòng `0.7` ATTENTION; trả nợ `1.097973` ATTENTION; số dư thấp nhất `-97050000.00` ngày `2027-02-22` ATTENTION; thụ động `1.000000` HIGH; thiết yếu `1.000000` ATTENTION; **cả sáu rule ATTENTION**: `-57250000.00`, `-97050000.00`, `0.7`, `1.097973`, `42000000.00 < 50750000.00`, `1.000000 RENTAL_INCOME`; Lương đã kết thúc 31/10 không xuất hiện ở đâu; đối soát đúng | PASS (2) |
| H10 | P (Một phần, không tỷ giá) | `exchangeRate null`; dự phòng PARTIAL `21.9` (`1348000000.00`), trả nợ PARTIAL `0.092206`, số dư thấp nhất PARTIAL `124600000.00`, thụ động PARTIAL `0.101002`, đều `band null` `PARTIAL_INPUTS` `MISSING_EXCHANGE_RATE`; thiết yếu COMPLETE `0.833696` ATTENTION; sáu rule NOT_EVALUATED (`MISSING_EXCHANGE_RATE`, `INDICATOR_PARTIAL` ×3, `MISSING_EXCHANGE_RATE` ×2); mọi dòng USD `amountVnd null`; đối soát đúng | PASS (3) |
| H11 | Z (Mẫu số 0, chỉ có chi một lần) | dự phòng UNAVAILABLE `NO_RECURRING_ESSENTIAL_EXPENSES` (nguồn vẫn liệt kê tiền mặt 50.000.000); trả nợ, thụ động UNAVAILABLE `NO_INCOME`; số dư thấp nhất COMPLETE `50000000.00`, `band null` `NO_RECURRING_ESSENTIAL_EXPENSES`; thiết yếu `0.604651` OK; rule: chi vượt thu ATTENTION `-38700000.00`, forecast OK, dự phòng/trả nợ `INDICATOR_UNAVAILABLE`, thanh khoản `NO_LIABILITIES`, tập trung `NO_INCOME`; không `value` nào là `0` thay mẫu số thiếu | PASS (2) |
| H12 | I (chỉ có thu) | trả nợ `0.000000` OK và thụ động `0.000000` LOW là số 0 thật; dự phòng `NO_LIQUID_ASSETS`, số dư thấp nhất `NO_CASH_ACCOUNTS`, thiết yếu `NO_EXPENSES`; tập trung ATTENTION `1.000000 SALARY` | PASS (2) |
| H13 | E (chưa có dữ liệu) | `200`; năm UNAVAILABLE `value null`, `numeratorVnd`/`denominatorVnd null`, `sources []`; sáu NOT_EVALUATED (`NO_CASH_FLOW_IN_MONTH`, `INDICATOR_UNAVAILABLE` ×3, `NO_CASH_ACCOUNTS`, `NO_INCOME`) | PASS (3) |
| H14 | `month=2026-13`, `11%2F2026`, `2026-9`, `month=`, hai `month` | `400` `urn:rikkaus:problem:validation-failed`, `errors [{month, "must be a calendar month in the form YYYY-MM"}]`, giá trị gửi không bị echo | PASS (10) |
| H15 | không token; `Bearer not-a-token` | `401` `urn:rikkaus:problem:unauthorized` | PASS (3) |
| H16 | `POST`; `Accept: text/csv` | `405`; `406` `urn:rikkaus:problem:not-acceptable` | PASS (2) |
| H17 | ngưỡng qua biến môi trường | `thresholds.minReserveMonths 5.0`, `maxIncomeSourceShare 0.800000`, `essentialCategories` có `EDUCATION`; B thiết yếu `0.972826` (`89500000.00`), tập trung B OK `0.733333 ≤ 0.8`; W dự phòng `4.7` dải ATTENTION và rule dự phòng **ATTENTION** (`4.7 < 5.0`), tập trung W OK; đối soát B đúng | PASS (4) |
| H18 | khởi động lại không biến | B byte-identical với H01 | PASS |
| H19 | cấu hình vô lý | `ESSENTIALCATEGORIES=SALARY` → thoát, `rikkaus.cashflow.health.essential-categories must list expense categories only; SALARY is an income category.`; `MINRESERVEMONTHS=7` → `min-reserve-months must not be greater than … reserve-ok-min-months.`; `PASSIVEINCOMEMEDIUMMIN=0.6` → `passive-income-medium-min must not be greater than … passive-income-high-min.` | PASS (3) |
| H20 | `./mvnw clean verify` | Spotless check sạch; Surefire 81/81 (`ArchitectureRulesTest` 4/4); Failsafe 50/50 (`OpenApiContractIT` 4/4 không lệch contract, `ApplicationContextIT` 3/3); `BUILD SUCCESS` | PASS |

## Ngoài ma trận

- `apps/mobile`: `npm run api:generate` cập nhật `src/api/generated/openapi.ts` (+420 dòng) và
  `contract-examples.ts` (ba ví dụ mới); `npm run api:check` và `npm run typecheck` xanh.
- CodeQL `java/comparison-with-wider-type`: không có so sánh `int` với `long` trong mã mới (biên ngày là
  `Integer` so với hằng `int`; vòng lặp lịch trả nằm trong `LiabilityPaymentSchedule` có sẵn).
- Gitleaks: plan, report, script và docs không chứa token, JWT, mật khẩu hay khoá nào.

## Khoảng trống còn lại

- Không có test tự động cho route (chỉ đạo PO); ma trận này là bằng chứng duy nhất.
- Chưa chạy trên tháng có USD ở **chi** định kỳ thiết yếu không tỷ giá (đường `PARTIAL` của mẫu số dự phòng
  và `band null PARTIAL_INPUTS` của số dư thấp nhất); đường mã giống nhánh tài sản USD đã chạy ở H10.
- Chưa chạy liability có lịch trả bằng USD trong cửa sổ nghĩa vụ khi không có tỷ giá (rule thanh khoản
  `threshold.value null`).
