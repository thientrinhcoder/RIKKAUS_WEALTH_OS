# Manual verification — GH-92 Basic Advisor rule-based report

**Kết quả:** 26/26 kịch bản đạt; `cases.sh` 56/56 phép kiểm PASS, 0 FAIL, trong đó `recon.jq` chạy trên 6
body với 24 phép đối soát mỗi body (144/144 đúng); chạy ngày 28/11/2026 (10:59 giờ Việt Nam) qua HTTP vào API
thật trên PostgreSQL 18.6 trong Docker. `./mvnw clean verify` xanh. Không có unit test hay Testcontainers
test mới (chỉ đạo PO).

## Môi trường

| Mục | Giá trị |
|---|---|
| Nhánh | `claude/issue-92-basic-advisor-cc72b4`, cắt từ `main` `f4d01d4` |
| Bản jar | lần chạy chính dùng jar có toàn bộ mã service và route của ticket, với `AdvisorReportExamples` tạm `{}`; lần chạy lại dùng jar của `clean verify` có ví dụ thật (xem "Chạy lại trên jar cuối") |
| JDK | `openjdk@25` (Homebrew) |
| Cơ sở dữ liệu | container dùng một lần `rikkaus-pg-92`, `postgres:18.6-alpine3.24`, `127.0.0.1:5492`, không volume, mật khẩu sinh bằng `openssl rand -hex 24` và chỉ nằm trong scratchpad quyền 600; đã xoá sau khi chạy |
| API | `java -jar … --spring.profiles.active=local --server.port=8092`, `RIKKAUS_JWT_SECRET` sinh bằng `openssl rand -hex 48`; đăng nhập bằng `POST /api/v1/auth/dev-sign-in`; đã dừng |
| Ngày chạy | `D = 2026-11-28` (`Asia/Ho_Chi_Minh`); tháng mặc định `2026-11`; cửa sổ nghĩa vụ `2026-11-28…2026-12-28`; forecast `2026-12-01…2027-02-28` |
| Script | [`setup.sh`](../261128-1036-GH-92-basic-advisor/scripts/setup.sh), [`cases.sh`](../261128-1036-GH-92-basic-advisor/scripts/cases.sh), [`recon.jq`](../261128-1036-GH-92-basic-advisor/scripts/recon.jq) |

Không có token, secret hay mật khẩu nào được ghi vào repo: token lấy lại từ dev sign-in ở đầu `cases.sh` và
không bao giờ in ra; mật khẩu PostgreSQL và secret JWT chỉ nằm trong scratchpad. Container
`rikkaus_wealth_os-postgres-1` của người dùng không bị đụng tới.

## Cách chạy lại

```bash
docker run -d --name rikkaus-pg-92 -e POSTGRES_USER=rikkaus -e POSTGRES_PASSWORD="$(openssl rand -hex 24)" -e POSTGRES_DB=rikkaus -p 127.0.0.1:5492:5432 postgres:18.6-alpine3.24
```

Khởi động API với `DB_URL=jdbc:postgresql://127.0.0.1:5492/rikkaus`, `POSTGRES_USER`, `POSTGRES_PASSWORD`,
`RIKKAUS_JWT_SECRET`, `--spring.profiles.active=local --server.port=8092`, rồi:

```bash
OUT=/path/to/scratch bash plans/261128-1036-GH-92-basic-advisor/scripts/setup.sh
```

```bash
OUT=/path/to/scratch bash plans/261128-1036-GH-92-basic-advisor/scripts/cases.sh
```

Các ngày trong `setup.sh` là ngày tuyệt đối của lần chạy 28/11/2026; chạy vào ngày khác thì tuổi định giá,
cửa sổ nghĩa vụ, timeline mục tiêu và mức lệch dời theo hôm nay, nên các kỳ vọng phụ thuộc chúng phải tính lại.
`recon.jq` thì không phụ thuộc ngày.

## Từng kịch bản

Mọi request là `curl -s "$BASE/advisor-report[?…]" -H "Authorization: Bearer $T_<user>"`, trừ khi ghi khác.
Body đầy đủ của A01, A11 và A17 là ba ví dụ OpenAPI trong `AdvisorReportExamples`.

| # | Kịch bản | Body thật | Kết quả |
|---|---|---|---|
| A01 | B (Cơ sở), không `month` | `200`; `asOfDate 2026-11-28`, `Asia/Ho_Chi_Minh`, `month 2026-11`, `priorityMarkerCount 3`, `disclaimer` đúng câu; `sources` `[insights 6, financial-health?month=2026-11 6, goal-projections 6]` | PASS (2) |
| A02 | B, phân nhóm | `unevaluated` 3: `Quỹ cưới` `TARGET_DATE_PASSED`, `Du lịch Nhật Bản cuối năm` `NO_CONTRIBUTION_MONTH`, `Quỹ hưu trí` `PLANNED_MONTHLY_CONTRIBUTION_MISSING`; `positive` 7 (`DEBT_TO_ASSET_RATIO`, năm rule health, `Quỹ dự phòng 6 tháng`); `attention` 8 | PASS (3) |
| A03 | B, đối soát | 24/24 phép đúng: mọi `observed`, `threshold`, `status`, `reason`, `dueDate`, `noteId` và tập bản ghi bằng insights, financial-health tháng 11 và goal-projections đọc ngay sau; tỷ trọng bất động sản `0.624896` = `7500000000.00 / 12002000000.00` | PASS |
| A04 | B, thứ tự | 1 `UPCOMING_LIABILITY_DUE` (hạn `2026-11-30`, `0.933333`) · 2 `CATEGORY_CONCENTRATION` (`0.249792`) · 3 `INCOME_CONCENTRATION` (`0.222222`) · 4 `LOW_LIQUIDITY_SHARE` (`0.041493`) · 5 `Mua nhà phố Thủ Đức` (phủ `89.49`, cần `23466000.00`, góp `21000000.00`, hạn `2029-11-30`) · 6 `Học phí đại học cho con` (phủ `66.90`, `0.331000`, hạn `2034-08-31`) · 7 `STALE_EXCHANGE_RATE` (`14.400000`) · 8 `STALE_VALUATION` (`0.600000`); `priority` true ở 1–3; `Quỹ dự phòng 6 tháng` phủ `110.40`, lệch `-0.104000` | PASS (2) |
| A05 | B, cùng bản ghi khác họ | `Căn hộ Thảo Điền` có trong ba hành động riêng: `REVIEW_ASSET_CONCENTRATION`, `RECONCILE_CASH_WITH_RESERVE`, `REFRESH_RECORD` | PASS |
| A06 | B, lưu ý và câu hành động | tỷ giá → `USER_DECLARED_EXCHANGE_RATE`, hai mục tiêu → `USER_ASSUMED_RETURN`, tập trung → `MANUAL_ENTRY_NOT_RECONCILED`; tám câu đúng nguyên văn, ví dụ "You may update Căn hộ Thảo Điền so the report reads a current value, then review the result." | PASS (2) |
| A07–A09 | B, `?month=2026-11`; đọc lần hai; `?userId=<S>` | byte-identical với A01 | PASS (3) |
| A10 | B, `?month=2026-10` | `month 2026-10`, nguồn `/api/v1/financial-health?month=2026-10`; health tháng 10: `INCOME_CONCENTRATION` `0.890088`, chi vượt thu OK `89900000.00`, dự phòng `31.4`; finding snapshot và goal bằng A01; đối soát 24/24 với `/financial-health?month=2026-10` | PASS (3) |
| A11 | S (Áp lực) | `attention` 11 (năm snapshot, sáu health), `positive` 1 (`DEBT_TO_ASSET_RATIO`), `unevaluated` 0; đối soát 24/24 (chi vượt thu `-57250000.00`, forecast `-97050000.00`, dự phòng `0.7` = `42000000.00 / 61600000.00`, trả nợ `1.097973` = `16250000.00 / 14800000.00`, thanh khoản `42000000.00` vs `50750000.00`) | PASS (3) |
| A12 | S, gộp | hành động 1 = `UPCOMING_LIABILITY_DUE` + `LIQUIDITY_SHORT_FOR_UPCOMING_OBLIGATIONS` + `FORECAST_CASH_NEGATIVE`, tier 1, hạn `2026-11-30`; bản ghi: `Thẻ tín dụng`, `Khoản vay mua căn hộ`, hai tài khoản tiền, sáu dòng tiền dự báo; `noteIds` `[MANUAL_ENTRY_NOT_RECONCILED, FORECAST_FROM_ENTERED_SCHEDULES]`; hai finding đầu chung đúng hai liability; `attention` vẫn đủ 11 | PASS (2) |
| A13 | S, không gộp | `Căn hộ cho thuê Quận 2` trong ba hành động riêng; `RESERVE_BELOW_REFERENCE` và `LOW_LIQUIDITY_SHARE` cùng họ nhưng không chung bản ghi → hai hành động | PASS |
| A14 | S, thứ tự | 9 hành động: gộp tier 1 (`0.933333`), `HIGH_DEBT_PAYMENT_BURDEN` (`2.137066`), `SPENDING_EXCEEDS_INCOME` (`null`), `CATEGORY_CONCENTRATION` (`0.980654`), `RESERVE_BELOW_REFERENCE` (`0.766667`), `INCOME_CONCENTRATION` (`0.666667`), `LOW_LIQUIDITY_SHARE` (`0.650545`), `STALE_EXCHANGE_RATE` (`14.400000`), `STALE_VALUATION` (`0.733333`); ưu tiên ở 1–3 | PASS |
| A15 | Z (Một phần) | `unevaluated` 6 với lý do `NO_LIABILITIES`, `MISSING_EXCHANGE_RATE`, `INDICATOR_UNAVAILABLE` ×2, `NO_LIABILITIES`, `NO_INCOME`; `positive` 4; `attention` 2; hành động `SPENDING_EXCEEDS_INCOME` rồi `CATEGORY_CONCENTRATION`, không hành động nào từ finding chưa đánh giá; đối soát 24/24 | PASS (3) |
| A16 | U (USD không tỷ giá) | `unevaluated` 8: bốn snapshot `MISSING_EXCHANGE_RATE` và rule dự phòng `INDICATOR_PARTIAL` mang `USER_DECLARED_EXCHANGE_RATE`, forecast `INDICATOR_PARTIAL` mang `FORECAST_FROM_ENTERED_SCHEDULES`; `STALE_EXCHANGE_RATE` chỉ ra `Tài khoản USD`; 1 hành động `INCOME_CONCENTRATION`; đối soát 24/24 | PASS (3) |
| A17 | E (trống) | `200`; `unevaluated.count 12`, ba nhóm và `suggestions` rỗng, `findingCount` `[6, 6, 0]`; đối soát 24/24 | PASS (3) |
| A18 | Quyền sở hữu | body B không chứa id nào của S, Z, U; body S không chứa id nào của B | PASS (2) |
| A19 | `month` sai ×5 (`2026-13`, `11%2F2026`, `2026-9`, rỗng, hai `month`) | `400`; `type`, `title`, `status`, `detail`, `errors` bằng body `/financial-health` cùng query, `instance /api/v1/advisor-report`, không echo giá trị | PASS (10) |
| A20 | không token; token sai | `401`, `urn:rikkaus:problem:unauthorized` | PASS (3) |
| A21 | `POST` | `405` | PASS |
| A22 | `Accept: application/xml` | `406` | PASS |
| A23 | ngôn ngữ, sáu body | mọi `action` bắt đầu `You may `, kết thúc `then review the result.`, không chứa từ cấm hay `!` | PASS (6) |
| A24 | bất biến | trong `recon.jq` trên sáu body: một nhóm cho mỗi finding; tier/họ chỉ ở `ATTENTION`; mỗi finding attention ở đúng một hành động; gộp liên thông; không bỏ sót gộp; hạng, ưu tiên, thứ tự; trường tổng hợp của hành động; công thức mức lệch | PASS |
| A25 | kiến trúc | `ArchitectureRulesTest` 4/4 với rule đã tinh chỉnh; lớp thăm dò tạm tham chiếu `cashflow.domain.CashFlowCategory` làm rule đỏ ("gets field `CashFlowCategory.SALARY`"), đã xoá; `grep` import của slice `advisor`: chỉ `..application..` của `snapshot`, `cashflow`, `goals` và `shared` | PASS |
| A26 | build | `./mvnw verify -Dopenapi.update=true` rồi `./mvnw clean verify` xanh; `openapi.json` +2428 dòng, 0 dòng xoá; `npm run api:check` và `typecheck` xanh | PASS |

### Đối soát có bắt được sai lệch không

Một bản sao A01 sửa tay (ngưỡng `0.45`, đảo hai hành động đầu, câu "You will surely profit…", sai tier) cho
`false` ở `action_language`, `deviation_formula`, `rank_order`, `ranks`, `snapshot_echo`, `suggestion_fields`,
`tier_and_family_by_rule`. Một bản sao A11 thêm một finding lạ vào hành động gộp cho `false` ở
`suggestion_fields` và `suggestions_cover_attention_once`.

## Chạy lại trên jar cuối

API khởi động lại bằng jar của `./mvnw clean verify` (có ba ví dụ thật) trên cùng dữ liệu, 11:04: `cases.sh`
lại 56/56 PASS, 0 FAIL, và sáu body A01, A10, A11, A15, A16, A17 byte-identical với lần chạy 10:59. Ba ví dụ
`200` trong `openapi.json` bằng đúng JSON của A01, A11, A17. Tám schema mới: `AdvisorReportResponse`,
`AdvisorSource`, `AdvisorUnevaluated`, `AdvisorFinding`, `AdvisorObserved`, `AdvisorThreshold`,
`AdvisorAffectedRecord`, `AdvisorSuggestion`.

## Build

`JAVA_HOME=…openjdk@25… ./mvnw clean verify`: BUILD SUCCESS. Spotless sạch; Surefire 81/81, trong đó
`ArchitectureRulesTest` 4/4 với rule đã tinh chỉnh; Failsafe 50/50, trong đó `OpenApiContractIT` 4/4 (path
mới đã khai báo, contract không lệch) và `ApplicationContextIT` 3/3. `apps/mobile`: `npm run api:generate`,
`npm run api:check` ("matches the contract"), `npm run typecheck` xanh.

## Chưa chạy

- Không có kịch bản cho một rule upstream mới chưa có tier: route sẽ trả 500 nêu tên rule (xem rủi ro ở
  phase 2); hiện cả mười hai rule đều có tier.
- Mục tiêu bị `EXPECTED_ANNUAL_RETURN_MISSING` và nhánh `TIMELINE_<timeline>` không có trong fixture; nhánh
  thứ hai không xảy ra được với calculator hiện tại (một mục tiêu không có trạng thái luôn có ít nhất một lý do).
