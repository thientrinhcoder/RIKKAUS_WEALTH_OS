# Xác minh thủ công — issue #65 wealth snapshot dashboard

**Kết quả: 32/32 case đạt, 51/51 phép kiểm `PASS`, 0 `FAIL`** trên jar đóng gói từ commit `a74afb8`
(nhánh `thientrinhcoder/feat/issue-65-wealth-snapshot-dashboard`, đã merge `origin/main` `0eb1ec3`), chạy
23/11/2026 15:40 giờ Việt Nam vào PostgreSQL `postgres:18.6-alpine3.24` mới tinh (container
`rikkaus-pg-issue-65`, cổng `5465`), API profile `local` cổng `8065`. Log khởi động có `now at version v14`:
không migration mới.

- Scenario (viết trước khi code):
  [`plans/261122-1842-GH-65-wealth-snapshot-dashboard/phase-01-test-scenarios.md`](../261122-1842-GH-65-wealth-snapshot-dashboard/phase-01-test-scenarios.md)
- Script đã chạy: đúng từng byte ba khối `setup.sh`, `recon.jq`, `cases.sh` của
  [`docs/manual-test-guide-wealth-snapshot.md`](../../docs/manual-test-guide-wealth-snapshot.md) (đã so bằng
  script trước khi chạy).
- `clean verify` không flag trên cùng cây: xanh, 81 unit test, 50 integration test; `openapi.json` không drift.

## Ngày của lần chạy

Phase 1 ghi ngày mẫu cho `D = 22/11/2026`. Lần chạy chính thức là `D = 23/11/2026`, nên mọi ngày tương đối
dịch một ngày: `D−5 = 18/11`, `D−1 = 22/11`, `D+7 = 30/11`, `D+13 = 06/12`, `D+20 = 13/12`, `D+28 = 21/12`,
`D+30 = windowEnd = 23/12/2026`, `D+31 = 24/12`, `D+40 = 02/01/2027`. Số tiền không phụ thuộc ngày.

## Lịch sử chạy

| Lần | Commit | Ngày | Kết quả |
|---|---|---|---|
| 1, chạy thử | cây làm việc trên `da67680` trước commit `4eb5dde`, `spring-boot:run` | 22/11/2026 18:55 | 51/51 `PASS`; không tính là evidence vì code chưa commit |
| 2, chính thức | `a74afb8`, jar | 23/11/2026 15:40 | 51/51 `PASS` |

Không case nào fail ở cả hai lần, nên không có lỗi nào phải sửa giữa hai lần chạy.

## Body thật theo case

Số tiền trên wire có hai chữ số thập phân (`12002000000.00`); dưới đây rút gọn bằng `jq -c`.

### A — fixture design #63

- **S01** `200`. `asOfDate "2026-11-23"`, `exchangeRate {"usdToVndRate":25100.000000,"effectiveDate":"2026-09-12"}`,
  `complete true`.
- **S02** `kpis`: `totalAssets {"amountVnd":12002000000.00,"knownAmountVnd":12002000000.00,"recordCount":11,"excludedRecordCount":0}`,
  `totalLiabilities {"amountVnd":1532000000.00,…,"recordCount":2}`, `netWorth 10470000000.00`,
  `debtToAssetRatio 0.127645`, `liquidAssets {"amountVnd":3180000000.00,…,"recordCount":4}`,
  `lastDataUpdate "2026-11-23"`. Khớp `12.002.000.000 ₫`, `1.532.000.000 ₫`, `10.470.000.000 ₫`, `12,8%`,
  `3.180.000.000 ₫` của design.
- **S03** `["Tài khoản USD",20000.00,"USD",502000000.00]` — `20.000 × 25.100 = 502.000.000 ₫`.
- **S04** `[["REAL_ESTATE",6100000000.00],["LISTED_SECURITIES",2150000000.00],["TERM_DEPOSIT",1200000000.00],["BANK_ACCOUNT",502000000.00],["BOND",420000000.00],["VEHICLE_OR_VALUABLE_ASSET",420000000.00],["INVESTMENT_FUND",380000000.00],["PRIVATE_BUSINESS_EQUITY",350000000.00],["OTHER_ASSET",222000000.00],["CASH",148000000.00],["COLLECTIBLE",110000000.00]]`;
  bảy dòng sau cộng `2.050.000.000 ₫` = nhóm `Khác`.
- **S05** `[["HIGH",3180000000.00,4],["MEDIUM",1842000000.00,3],["LOW",6980000000.00,4]]`.
- **S06** top 5 `Căn hộ Thảo Điền 6100000000`, `Danh mục cổ phiếu HOSE 2150000000`, `Tiền gửi 12 tháng 1200000000`,
  `Tài khoản USD 502000000`, `Trái phiếu Chính phủ Việt Nam 420000000`; `assets[4:6]` là
  `["Trái phiếu Chính phủ Việt Nam","Xe Volvo XC60"]` — hòa `420.000.000 ₫` xếp theo tên.
- **S07** `{"windowEnd":"2026-12-23","items":[["Khoản vay mua nhà","PAYMENT","2026-12-21",18750000.00,false]]}`;
  `Thẻ tín dụng An Tín` (`02/01/2027`) và đáo hạn 2041 không có.
- **S08** 14 `sourceDates`, sáu dòng đầu
  `[["LIABILITY","Khoản vay mua nhà","2026-11-23"],["LIABILITY","Thẻ tín dụng An Tín","2026-11-23"],["ASSET","Danh mục cổ phiếu HOSE","2026-09-19"],["ASSET","Tài khoản USD","2026-09-12"],["ASSET","Tiền mặt dự phòng","2026-09-12"],["EXCHANGE_RATE",null,"2026-09-12"]]`.
- **S09** `recon.jq`: 28/28 phép đối soát `true`.
- **S10** gọi lần hai: `cmp` giống từng byte (8.554 byte cả hai).

### B — partial state (C)

- **S11** `200`. `complete false`, `exchangeRate null`, `totalAssets {"amountVnd":null,"knownAmountVnd":148000000.00,"recordCount":2,"excludedRecordCount":1}`,
  `totalLiabilities {"amountVnd":null,"knownAmountVnd":0.00,"recordCount":1,"excludedRecordCount":1}`,
  `netWorth null`, `debtToAssetRatio null`, `liquidAssets {"amountVnd":null,"knownAmountVnd":148000000.00,…}`;
  danh mục `[["CASH",148000000.00,148000000.00,0],["BANK_ACCOUNT",null,0.00,1]]`; top 5 chỉ
  `Tiền mặt dự phòng`, `excludedRecordCount 1`; dòng USD `valueVnd null`, công nợ USD `outstandingBalanceVnd null`.
- **S12** sáu figure không khả dụng đều `null`, không cái nào là `0`; `sourceDates` không có `EXCHANGE_RATE`.
- **S13** `recon.jq` 28/28 `true`.
- **S14** sau `PUT /settings/exchange-rate` `25100` hiệu lực `2026-11-20`: `complete true`, tài sản `650000000.00`,
  công nợ `125500000.00`, ròng `524500000.00`, nợ/tài sản `0.193077`; danh mục
  `[["BANK_ACCOUNT",502000000.00],["CASH",148000000.00]]`; `sourceDates`
  `[["LIABILITY","2026-11-23"],["EXCHANGE_RATE","2026-11-20"],["ASSET","2026-11-15"],["ASSET","2026-11-15"]]`.
- **S15** `recon.jq` 28/28 `true`.

### C — biên

- **S16** `200` (không `404`), body đầy đủ:
  `{"asOfDate":"2026-11-23",…,"exchangeRate":null,"complete":true,"kpis":{"totalAssets":{"amountVnd":0.00,"knownAmountVnd":0.00,"recordCount":0,"excludedRecordCount":0},…,"netWorth":0.00,"debtToAssetRatio":null,…,"lastDataUpdate":null},"assetsByCategory":[],"assetsByLiquidity":[],"largestAssets":{"items":[],"excludedRecordCount":0},"upcomingLiabilityDues":{"windowDays":30,"windowEnd":"2026-12-23","items":[]},"sourceDates":[],"assets":[],"liabilities":[]}`.
- **S17** `recon.jq` 28/28 `true`.
- **S18** tài sản `{"amountVnd":0.00,"knownAmountVnd":0.00,"recordCount":1,"excludedRecordCount":0}`, công nợ
  `50000000.00`, ròng `-50000000.00`, nợ/tài sản `null`, top 5 `[["Xe máy Honda SH đã bán",0.00]]`.

### D — top 5, hòa giá trị, ghi định giá (T)

- **S19** tổng `4000000000.00`, nợ/tài sản `0.000000`, thanh khoản cao `2100000000.00`, `lastDataUpdate "2026-10-20"`;
  top 5 `Cổ phiếu FPT`, `Tiền gửi BIDV 6 tháng`, `Quỹ mở VCBF`, `Trái phiếu Vingroup`, `Ô tô Kia Morning`;
  `assets` kết thúc bằng `"Ô tô Kia Morning","Vàng SJC"` — collation tiếng Việt, không phải code point.
- **S20** `POST /assets/{T6}/valuations` `650000000` hiệu lực `2026-11-22` → `201`; snapshot: tổng `4150000000.00`,
  thanh khoản cao `2250000000.00`, `lastDataUpdate "2026-11-22"`, top 5
  `FPT 900000000`, `BIDV 800000000`, `VCBF 700000000`, `Vàng SJC 650000000`, `Trái phiếu Vingroup 600000000`.
- **S21** lịch sử định giá của `Vàng SJC`: 2 bản ghi.
- **S22** định giá lùi ngày `480000000` hiệu lực `2026-08-01` → `201`; snapshot giống từng byte với S20 (4.635 byte);
  lịch sử 3 bản ghi.
- **S23** `recon.jq` 28/28 `true`.

### E — cửa sổ nghĩa vụ (G)

- **S24** `[["Vay margin SSI","PAYMENT","2026-11-18",null,true],["Vay mua xe VPBank","PAYMENT","2026-11-30",8500000.00,false],["Vay mua xe VPBank","MATURITY","2026-11-30",8500000.00,false],["Vay mua nhà Vietcombank","PAYMENT","2026-12-06",25000000.00,false],["Vay cầm cố sổ tiết kiệm","MATURITY","2026-12-13",300000000.00,false],["Thẻ tín dụng Techcombank","PAYMENT","2026-12-23",null,false]]`;
  `Vay tiêu dùng HD Saison` (`24/12`, ngoài biên) và `Nợ mượn anh Hai` (không lịch) không có.
- **S25** công nợ `2933000000.00`, ròng `1267000000.00`, nợ/tài sản `0.698333`.
- **S26** `recon.jq` 28/28 `true`.

### F — ownership và HTTP

- **S27** B: `{"assets":["Sổ tiết kiệm Agribank"],"a":300000000.00}`; số id của A trong body B: `0`; số id của B
  trong body A: `0`.
- **S28** `?userId=<id của A>` bằng token B: giống từng byte body S27 (1.503 byte).
- **S29** không token / hết hạn / ký sai khóa: cả ba `401`,
  `{"type":"urn:rikkaus:problem:unauthorized","title":"Authentication required","status":401}`,
  `Content-Type: application/problem+json`.
- **S30** `POST` và `DELETE`: `405`, `{"type":"urn:rikkaus:problem:method-not-allowed","status":405}`.
- **S31** `Accept: text/csv`: `406`.
- **S32** `/v3/api-docs`: `{"methods":["get"],"security":[{"bearerAuth":[]}]}`.

## Toàn bộ dòng in của `cases.sh`

```text
commit a74afb8 2026-11-23 15:40 +07 today=2026-11-23
    S01 PASS status 200
    S01 PASS header, rate, complete
    S02 PASS kpis
    S03 PASS A02 20000 USD -> 502000000
    S04 PASS assetsByCategory order and amounts
    S05 PASS assetsByLiquidity
    S06 PASS largest five, tie 420M -> Trái phiếu before Xe Volvo
    S07 PASS one upcoming due: L1 payment D+28
    S08 PASS lastDataUpdate D, 14 source dates
    S09 PASS reconciliation of S01 (all true)
    S10 PASS second read byte-identical
    S11 PASS status 200
    S11 PASS partial aggregates
    S12 PASS no zero stands in for unavailable; no rate source
    S13 PASS reconciliation of S11 (all true)
    S14 PASS after declaring the rate
    S15 PASS reconciliation of S14 (all true)
    S16 PASS status 200, not 404
    S16 PASS full empty structure
    S17 PASS reconciliation of S16 (all true)
    S18 PASS zero assets: ratio null, net worth negative
    S19 PASS top five, Ô tô before Vàng
    S20 PASS valuation recorded 201
    S20 PASS kpis and ranking follow the new valuation
    S21 PASS history kept: 2 records
    S22 PASS back-dated valuation recorded 201
    S22 PASS back-dated valuation leaves the snapshot byte-identical
    S22 PASS history kept: 3 records
    S23 PASS reconciliation of S22 (all true)
    S24 PASS six dues in order
    S25 PASS G totals
    S26 PASS reconciliation of S24 (all true)
    S27 PASS B sees only B
    S27 PASS no id of A in B's body
    S27 PASS no id of B in A's body
    S28 PASS ?userId=<A> with B's token is B's body
    S29 PASS none token -> 401
    S29 PASS none token -> unauthorized problem
    S29 PASS none token -> problem+json
    S29 PASS expired token -> 401
    S29 PASS expired token -> unauthorized problem
    S29 PASS expired token -> problem+json
    S29 PASS forged token -> 401
    S29 PASS forged token -> unauthorized problem
    S29 PASS forged token -> problem+json
    S30 PASS POST -> 405
    S30 PASS POST -> problem
    S30 PASS DELETE -> 405
    S30 PASS DELETE -> problem
    S31 PASS Accept text/csv -> 406
    S32 PASS contract publishes GET only, bearerAuth
TOTAL pass=51 fail=0
```

## Dọn dẹp

API (jar) đã dừng, container `rikkaus-pg-issue-65` đã xóa. Token, id và body thật chỉ nằm trong thư mục
nháp ngoài repo.

## Verification gap

Không có automated test nào gọi vào route này; xem mục "Verification gap" của
[plan](../261122-1842-GH-65-wealth-snapshot-dashboard/plan.md). Case biên nửa đêm không chạy. Collation tiếng
Việt chỉ được kiểm trên một cặp tên (`Ô` trước `V`) và cặp `Trái phiếu…`/`Xe Volvo…`.
