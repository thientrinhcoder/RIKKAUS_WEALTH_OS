# Manual verification — GH-68 Wealth snapshot insights

**Kết quả:** 28/28 kịch bản đạt, 46/46 phép kiểm PASS, 0 FAIL, chạy ngày 24/11/2026 (10:08–10:10 giờ
Việt Nam) qua HTTP vào API thật trên PostgreSQL 18.6 trong Docker. Không có unit test (chỉ đạo PO).

## Môi trường

| Mục | Giá trị |
|---|---|
| Nhánh | `claude/issue-68-snapshot-insights-99a45a`, cắt từ `main` `7d1b1b8` |
| Bản jar | đóng từ working tree có toàn bộ mã của ticket; sau lần chạy chỉ `SnapshotInsightsExamples` (văn bản ví dụ OpenAPI) đổi |
| JDK | `openjdk@25` (Homebrew) |
| Cơ sở dữ liệu | container dùng một lần `rikkaus-pg-68`, `postgres:18.6-alpine3.24`, `127.0.0.1:5468`, không volume, mật khẩu sinh bằng `openssl rand` và chỉ nằm trong scratchpad |
| API | `java -jar … --server.port=8068`, profile `local`, đăng nhập bằng `POST /api/v1/auth/dev-sign-in` |
| Ngày chạy | `D = 2026-11-24` (`Asia/Ho_Chi_Minh`) |
| Script | [`setup.sh`](../261124-0956-GH-68-snapshot-insights/scripts/setup.sh), [`cases.sh`](../261124-0956-GH-68-snapshot-insights/scripts/cases.sh), [`recon.jq`](../261124-0956-GH-68-snapshot-insights/scripts/recon.jq) |

Không có token, secret hay mật khẩu nào được ghi vào repo: token lấy lại từ dev sign-in ở đầu mỗi lần chạy
`cases.sh`, mật khẩu PostgreSQL chỉ nằm trong file scratchpad quyền 600.

## Cách chạy lại

```bash
docker run -d --name rikkaus-pg-68 -e POSTGRES_USER=rikkaus -e POSTGRES_PASSWORD="$(openssl rand -hex 24)" -e POSTGRES_DB=rikkaus -p 127.0.0.1:5468:5432 postgres:18.6-alpine3.24
```

Khởi động API với `SPRING_PROFILES_ACTIVE=local`, `DB_URL=jdbc:postgresql://127.0.0.1:5468/rikkaus`,
`POSTGRES_USER`, `POSTGRES_PASSWORD` và `--server.port=8068`, rồi theo thứ tự:

```bash
OUT=/path/to/scratch bash plans/261124-0956-GH-68-snapshot-insights/scripts/setup.sh
```

```bash
OUT=/path/to/scratch bash plans/261124-0956-GH-68-snapshot-insights/scripts/cases.sh main
```

Dừng API, khởi động lại với sáu biến `RIKKAUS_SNAPSHOT_INSIGHTS_MAXCATEGORYSHARE=0.7`,
`…_MAXDEBTTOASSETRATIO=0.1`, `…_MAXLOWLIQUIDITYSHARE=0.65`, `…_DUEWINDOWDAYS=7`,
`…_MAXVALUATIONAGEDAYS=60`, `…_MAXEXCHANGERATEAGEDAYS=90`, chạy `cases.sh custom`; dừng, khởi động lại
không có biến nào, chạy `cases.sh restored` rồi `cases.sh resolved`. I25–I27 là ba lần khởi động với cấu hình
vô lý, mỗi lần phải tự thoát.

## Từng kịch bản

Mọi request là `curl -s "$BASE/wealth-snapshot/insights" -H "Authorization: Bearer $T_<user>"`, trừ khi ghi
khác. Cột "Body thật" trích từ body đã lưu.

| # | Kịch bản | Lệnh | Body thật | Kết quả |
|---|---|---|---|---|
| I01 | A, đầu response | `GET` với token A | `200`, `asOfDate 2026-11-24`, `complete true`, `thresholds {0.500000, 0.400000, 0.600000, 30, 30, 5}`, sáu `ruleId` đúng thứ tự | PASS (2) |
| I02 | A, tập trung | như I01 | `ATTENTION`, `0.624896` `REAL_ESTATE`, `7500000000.00 / 12002000000.00`, evidence `[Căn hộ Thảo Điền]` | PASS |
| I03 | A, nợ/tài sản | như I01 | `OK`, `0.127645`, `1532000000.00 / 12002000000.00`, evidence vay mua căn hộ rồi thẻ tín dụng | PASS |
| I04 | A, thanh khoản thấp | như I01 | `ATTENTION`, `0.624896`, evidence `[Căn hộ Thảo Điền]` | PASS |
| I05 | A, hạn nợ | như I01 | `ATTENTION`, `12` ngày tới `2026-12-06`, evidence `NEXT_PAYMENT_DATE 18750000.00 VND, daysUntilDue 12`; thẻ tín dụng D+40 không có | PASS (2) |
| I06 | A, định giá cũ | như I01 | `ATTENTION`, `48` ngày từ `2026-10-07`, evidence căn hộ `ageDays 48` | PASS |
| I07 | A, tỷ giá cũ | như I01 | `ATTENTION`, `73` ngày từ `2026-09-12`; evidence dòng tỷ giá `25100.000000` rồi Tiền gửi USD `20000.00 USD → 502000000.00` | PASS |
| I08 | A, đối soát | `GET /wealth-snapshot` ngay sau I01, `recon.jq` | 14 phép đối soát đều `true` (tử/mẫu số, tỷ lệ nợ, cửa sổ hạn nợ = `upcomingLiabilityDues.items`, định giá cũ nhất, ngày tỷ giá, số tiền evidence, mọi `sourceId` thuộc A) | PASS |
| I09 | A, đọc lần hai | như I01 | byte-identical với I01 | PASS |
| I10 | O, cả sáu `OK` | token O | `OK` ×6: `0.380590`, `0.126863`, `0.380590`, `45`, `20`, `2`; evidence vay ô tô D+45, Nhà phố Gò Vấp, tỷ giá 25.300 + USD `10000.00 → 253000000.00`; đối soát đúng | PASS (2) |
| I11 | E0, bằng ngưỡng | token E0 | `OK` ×6: `0.500000` `BOND`, `0.400000`, `0.600000`, `31`, `30`, `5`; đối soát đúng | PASS (2) |
| I12 | E1, vượt một đơn vị | token E1 | `ATTENTION` ×6: `0.501000`, `0.401000`, `0.601000`, `-5`, `31`, `6`; evidence hạn nợ margin SSI D−5 `overdue true` rồi HD Saison D+30; định giá chỉ trái phiếu; đối soát đúng | PASS (2) |
| I13 | C, thiếu tỷ giá | token C | `complete false`; `UNAVAILABLE MISSING_EXCHANGE_RATE` cho tập trung, nợ, thanh khoản, tỷ giá với evidence là bản ghi USD `amountVnd null`; hạn nợ `ATTENTION 20` (đáo hạn 5.000 USD, `amountVnd null`); định giá `OK 3`; đối soát đúng | PASS (2) |
| I14 | C khai tỷ giá | `PUT /settings/exchange-rate {"usdToVndRate":"25100","effectiveDate":"2026-11-24"}` rồi `GET` | `complete true`; `ATTENTION 0.772308 BANK_ACCOUNT` (502.000.000 / 650.000.000), `OK 0.193077`, `OK 0.000000` evidence rỗng, `ATTENTION 20` `amountVnd 125500000.00`, `OK 3`, `OK 0` | PASS (2) |
| I15 | H, chỉ VND | token H | `ATTENTION 0.533333 TERM_DEPOSIT`, `OK 0.000000` (0 / 1.500.000.000), `OK 0.000000`, `UNAVAILABLE NO_LIABILITIES`, `OK 2`, `UNAVAILABLE MISSING_EXCHANGE_RATE` evidence rỗng | PASS (2) |
| I16 | Z, tổng tài sản 0 | token Z | ba rule tỷ lệ `UNAVAILABLE ZERO_TOTAL_ASSETS` evidence `[Xe máy 0.00]`, `UNAVAILABLE NO_DUE_DATES`, `OK 1`, `UNAVAILABLE MISSING_EXCHANGE_RATE` | PASS (2) |
| I17 | N, chưa có bản ghi | token N | `200`; `UNAVAILABLE` ×6: `NO_ASSETS, NO_ASSETS, NO_ASSETS, NO_LIABILITIES, NO_ASSETS, MISSING_EXCHANGE_RATE`; observed `null`, evidence rỗng, threshold vẫn có | PASS (3) |
| I18 | Ownership | `GET …?userId=<id của O>` với token A | evidence của A không có id nào của O; body byte-identical với I01 | PASS (2) |
| I19 | Không token | `curl -s "$BASE/wealth-snapshot/insights"` | `401`, `urn:rikkaus:problem:unauthorized` | PASS (2) |
| I20 | Token sai | `-H "Authorization: Bearer not-a-real-token"` | `401` | PASS |
| I21 | Sai method | `curl -X POST …` | `405`, `urn:rikkaus:problem:method-not-allowed` | PASS |
| I22 | Accept không phải JSON | `-H 'Accept: application/xml'` | `406`, `urn:rikkaus:problem:not-acceptable` | PASS |
| I23 | Ngưỡng mới qua biến môi trường | khởi động lại với `0.7 / 0.1 / 0.65 / 7 / 60 / 90` | `thresholds` đúng giá trị mới; status `OK, ATTENTION, OK, OK, OK, OK`; observed giữ nguyên `0.624896, 0.127645, 0.624896, 12, 48, 73`; hạn nợ `OK` evidence là hạn gần nhất; `GET /wealth-snapshot` vẫn `windowDays 30`; đối soát đúng | PASS (4) |
| I24 | Trả về cấu hình mặc định | khởi động lại không biến | body A byte-identical với I01: kết quả tái lập được | PASS |
| I25 | `MAXCATEGORYSHARE=1.5` | khởi động | thoát mã 1, không có dòng `Started`; log: `rikkaus.snapshot.insights.max-category-share must not be greater than 1.` | PASS |
| I26 | `DUEWINDOWDAYS=-1` | khởi động | thoát mã 1; `rikkaus.snapshot.insights.due-window-days must be between 0 and 3650 days.` | PASS |
| I27 | `MAXVALUATIONAGEDAYS=` (rỗng) | khởi động | thoát mã 1; `rikkaus.snapshot.insights.max-valuation-age-days is not set.` | PASS |
| I28 | A cập nhật định giá rồi tỷ giá | `POST /assets/{căn hộ}/valuations {"value":"7500000000","currency":"VND","effectiveDate":"2026-11-24"}`, sau đó `PUT /settings/exchange-rate` ngày D | định giá `OK 10` (evidence Tiền gửi 12 tháng); rồi tỷ giá `OK 0`; ba tỷ lệ giữ `0.624896, 0.127645, 0.624896`; đối soát đúng | PASS (3) |

## Bộ kiểm không "pass rỗng"

- `jq -e '.insights[0].observed.value == 0.624897'` trên body I01 trả `false`, mã thoát 1.
- Sửa `DEBT_TO_ASSET_RATIO.observed.value` của body I01 thành `0.2` rồi chạy `recon.jq`: đúng một phép
  `debt_ratio_eq_snapshot_kpi` thành `false`.

## Log tóm tắt

```text
phase main      2026-11-24 10:08 +07  TOTAL pass=35 fail=0
phase custom    2026-11-24 10:09 +07  TOTAL pass=4 fail=0
fail-fast       I25 exit=1, I26 exit=1, I27 exit=1, started=0 each   (3 PASS)
phase restored  2026-11-24 10:09 +07  TOTAL pass=1 fail=0
phase resolved  2026-11-24 10:09 +07  TOTAL pass=3 fail=0
```

## Verification gap

- Không có automated test cho route này; regression chỉ bị bắt khi chạy lại script.
- Chỉ chạy ngày 24/11/2026. Biên nửa đêm giờ Việt Nam và chuyển năm chưa chạy thật (mọi ngày đi qua
  `asOfDate` của snapshot, đã kiểm ở #65).
- Ngưỡng phân số có 6 chữ số thập phân (ví dụ `0.500001`) chưa có kịch bản riêng; biên được kiểm bằng giá
  trị đúng bằng ngưỡng và vượt `0.001`.
- Hiệu năng với danh mục lớn chưa đo; một request đọc đúng ba truy vấn của snapshot.
