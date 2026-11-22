---
phase: 1
title: "Danh sách test scenario"
status: completed
priority: P1
effort: "0.5d"
dependencies: []
---

# Phase 1: Danh sách test scenario

## Mục tiêu

Viết ra toàn bộ hành vi phải kiểm chứng **trước** khi có dòng code nào, theo cách hiểu TDD đã chốt với
Product Owner: scenario trước, không unit test. Mỗi scenario là một case trong
`docs/manual-test-guide-wealth-snapshot.md` và một dòng có status/body thật trong evidence report.

Mọi số kỳ vọng dưới đây tính tay từ fixture và quy tắc ở [plan.md](./plan.md) trước khi code; khi chạy,
body API được so với bảng này chứ không phải ngược lại.

## Ngày

`D` là hôm nay theo `Asia/Ho_Chi_Minh`. Lần chạy kèm implementation là `D = 22/11/2026`; mọi ngày tương đối
được tính lúc chạy, nên guide chạy lại được ngày khác mà không sửa dữ liệu.

| Ký hiệu | Lần chạy 22/11/2026 |
|---|---|
| `D−5` | `17/11/2026` |
| `D−1` | `21/11/2026` |
| `D+7` | `29/11/2026` |
| `D+13` | `05/12/2026` |
| `D+20` | `12/12/2026` |
| `D+28` | `20/12/2026` |
| `D+30` (biên, vẫn tính) | `22/12/2026` |
| `D+31` (ngoài) | `23/12/2026` |
| `D+40` (ngoài) | `01/01/2027` |

## Dữ liệu

Bảy user chèn thẳng vào `wealth.users` (không có endpoint đăng ký), token HS256 mint bằng secret của profile
`local` như các guide trước. Mọi tài sản, công nợ, định giá, tỷ giá tạo **qua API**.

| User | Vai trò |
|---|---|
| A An Nguyễn | fixture design #63, VND + USD có tỷ giá |
| B Bích Trần | ownership |
| C Cường Lê | USD chưa có tỷ giá → partial, rồi khai tỷ giá |
| N Nam Phạm | user mới, không có gì |
| Z Zung Võ | tổng tài sản bằng 0 |
| T Thảo Đặng | 6 tài sản chỉ VND, top-5 và hòa giá trị, ghi định giá |
| G Giang Hồ | cửa sổ nghĩa vụ sắp đến hạn |

### A — fixture design #63

Tỷ giá `1 USD = 25.100 ₫`, hiệu lực `12/09/2026`.

| # | Tên | Category | Giá trị gốc | Ngày định giá | Thanh khoản | VND |
|---|---|---|---|---|---|---|
| A01 | `Tiền mặt dự phòng` | `CASH` | `148.000.000 ₫` | 12/09/2026 | `HIGH` | `148.000.000` |
| A02 | `Tài khoản USD` | `BANK_ACCOUNT` | `20.000 USD` | 12/09/2026 | `HIGH` | `20.000 × 25.100 = 502.000.000` |
| A03 | `Tiền gửi 12 tháng` | `TERM_DEPOSIT` | `1.200.000.000 ₫` | 08/09/2026 | `MEDIUM` | `1.200.000.000` |
| A04 | `Danh mục cổ phiếu HOSE` | `LISTED_SECURITIES` | `2.150.000.000 ₫` | 19/09/2026 | `HIGH` | `2.150.000.000` |
| A05 | `Trái phiếu Chính phủ Việt Nam` | `BOND` | `420.000.000 ₫` | 31/08/2026 | `LOW` | `420.000.000` |
| A06 | `Quỹ mở cổ phiếu Việt` | `INVESTMENT_FUND` | `380.000.000 ₫` | 10/09/2026 | `HIGH` | `380.000.000` |
| A07 | `Căn hộ Thảo Điền` | `REAL_ESTATE` | `6.100.000.000 ₫` | 02/08/2026 | `LOW` | `6.100.000.000` |
| A08 | `Vốn góp Công ty Mộc Việt` | `PRIVATE_BUSINESS_EQUITY` | `350.000.000 ₫` | 30/06/2026 | `LOW` | `350.000.000` |
| A09 | `Xe Volvo XC60` | `VEHICLE_OR_VALUABLE_ASSET` | `420.000.000 ₫` | 01/09/2026 | `MEDIUM` | `420.000.000` |
| A10 | `Đồng hồ cơ sưu tầm` | `COLLECTIBLE` | `110.000.000 ₫` | 15/07/2026 | `LOW` | `110.000.000` |
| A11 | `Khoản đặt cọc mặt bằng` | `OTHER_ASSET` | `222.000.000 ₫` | 05/09/2026 | `MEDIUM` | `222.000.000` |

| # | Tên | Category | Dư nợ | Lịch |
|---|---|---|---|---|
| L1 | `Khoản vay mua nhà` | `BANK_LOAN` | `1.500.000.000 ₫` | `18.750.000 ₫` `MONTHLY`, kỳ tới `D+28`, đáo hạn 20/09/2041, thế chấp A07 |
| L2 | `Thẻ tín dụng An Tín` | `CREDIT_CARD_BALANCE` | `32.000.000 ₫` | kỳ tới `D+40` |

Tính tay:

- Tổng tài sản `148.000.000 + 502.000.000 + 1.200.000.000 + 2.150.000.000 + 420.000.000 + 380.000.000 +
  6.100.000.000 + 350.000.000 + 420.000.000 + 110.000.000 + 222.000.000 = 12.002.000.000 ₫`.
- Tổng công nợ `1.532.000.000 ₫`; ròng `10.470.000.000 ₫`.
- Nợ/tài sản `1.532.000.000 / 12.002.000.000 = 0,1276453924…` → `0.127645`.
- Thanh khoản cao (A01, A02, A04, A06) `3.180.000.000 ₫`; trung bình (A03, A09, A11) `1.842.000.000 ₫`;
  thấp (A05, A07, A08, A10) `6.980.000.000 ₫`. Ba mức cộng `12.002.000.000 ₫`.
- Danh mục theo thứ tự: `REAL_ESTATE 6.100.000.000`, `LISTED_SECURITIES 2.150.000.000`,
  `TERM_DEPOSIT 1.200.000.000`, `BANK_ACCOUNT 502.000.000`, `BOND 420.000.000`,
  `VEHICLE_OR_VALUABLE_ASSET 420.000.000` (hòa với `BOND`, sắp theo mã), `INVESTMENT_FUND 380.000.000`,
  `PRIVATE_BUSINESS_EQUITY 350.000.000`, `OTHER_ASSET 222.000.000`, `CASH 148.000.000`,
  `COLLECTIBLE 110.000.000`. Bảy dòng sau bốn dòng đầu cộng `2.050.000.000 ₫` = nhóm `Khác` của design.
- Top 5: A07, A04, A03, A02, rồi hòa `420.000.000 ₫` giữa `Trái phiếu Chính phủ Việt Nam` và
  `Xe Volvo XC60` → `Trái phiếu…` (T trước X).
- Nghĩa vụ trong cửa sổ: chỉ L1 `PAYMENT` `D+28` `18.750.000 ₫`. L1 đáo hạn 2041 và L2 `D+40` nằm ngoài.
- Ngày dữ liệu: L1, L2 tạo hôm nay → `D`. `sourceDates` có 14 dòng: 11 `ASSET`, 2 `LIABILITY` (`D`),
  1 `EXCHANGE_RATE` (12/09/2026, vì A02 đã được quy đổi).

### B, N, Z

- B: `Sổ tiết kiệm Agribank` `TERM_DEPOSIT` `300.000.000 ₫` `MEDIUM` 01/11/2026.
- N: không có gì.
- Z: `Xe máy Honda SH đã bán` `VEHICLE_OR_VALUABLE_ASSET` `0 ₫` `LOW` 10/11/2026; công nợ
  `Vay tiêu dùng FE Credit` `PERSONAL_LOAN` `50.000.000 ₫`, không lịch. Tổng tài sản `0`, ròng
  `−50.000.000 ₫`, nợ/tài sản `null`.

### C — USD chưa có tỷ giá

`Tiền mặt dự phòng` `CASH` `148.000.000 ₫` `HIGH` 15/11/2026; `Tài khoản USD` `BANK_ACCOUNT` `20.000 USD`
`HIGH` 15/11/2026; công nợ `Khoản vay USD từ người thân` `PERSONAL_LOAN` `5.000 USD`, không lịch.

- Trước khai tỷ giá: tài sản `{amountVnd: null, knownAmountVnd: 148.000.000, recordCount: 2,
  excludedRecordCount: 1}`; công nợ `{null, 0, 1, 1}`; ròng `null`; nợ/tài sản `null`; thanh khoản cao
  `{null, 148.000.000, 2, 1}`; danh mục `CASH {148.000.000, 148.000.000, 1, 0}` rồi
  `BANK_ACCOUNT {null, 0, 1, 1}`; top 5 chỉ `Tiền mặt dự phòng`, `excludedRecordCount 1`; `complete false`;
  không có `EXCHANGE_RATE` trong `sourceDates`.
- Sau khi khai `25.100`, hiệu lực 20/11/2026: tài sản `148.000.000 + 502.000.000 = 650.000.000 ₫`; công nợ
  `5.000 × 25.100 = 125.500.000 ₫`; ròng `524.500.000 ₫`; nợ/tài sản `125.500.000 / 650.000.000 =
  0,193076923…` → `0.193077`; `BANK_ACCOUNT 502.000.000` lên trước `CASH`; `sourceDates` có
  `EXCHANGE_RATE` 20/11/2026.

### T — 6 tài sản chỉ VND

| # | Tên | Category | Giá trị | Ngày | Thanh khoản |
|---|---|---|---|---|---|
| T1 | `Cổ phiếu FPT` | `LISTED_SECURITIES` | `900.000.000 ₫` | 01/10/2026 | `HIGH` |
| T2 | `Tiền gửi BIDV 6 tháng` | `TERM_DEPOSIT` | `800.000.000 ₫` | 05/10/2026 | `MEDIUM` |
| T3 | `Quỹ mở VCBF` | `INVESTMENT_FUND` | `700.000.000 ₫` | 10/10/2026 | `HIGH` |
| T4 | `Trái phiếu Vingroup` | `BOND` | `600.000.000 ₫` | 30/09/2026 | `LOW` |
| T5 | `Ô tô Kia Morning` | `VEHICLE_OR_VALUABLE_ASSET` | `500.000.000 ₫` | 15/09/2026 | `MEDIUM` |
| T6 | `Vàng SJC` | `VEHICLE_OR_VALUABLE_ASSET` | `500.000.000 ₫` | 20/10/2026 | `HIGH` |

- Tổng `4.000.000.000 ₫`, không công nợ → nợ/tài sản `0.000000` (khác `null`: tổng tài sản không bằng 0).
- Top 5: T1, T2, T3, T4, T5. T5 và T6 hòa `500.000.000 ₫`; collation tiếng Việt xếp `Ô` trước `V`, so sánh
  code point thì ngược lại (`V` U+0056 < `Ô` U+00D4). Đây là case phân biệt được hai cách.
- Thanh khoản cao `900 + 700 + 500 = 2.100.000.000 ₫`; ngày dữ liệu 20/10/2026.
- Ghi định giá `Vàng SJC` `650.000.000 ₫` hiệu lực `D−1`: tổng `4.150.000.000 ₫`, top 5
  T1, T2, T3, T6, T4 (T5 rơi ra), thanh khoản cao `2.250.000.000 ₫`, ngày dữ liệu `D−1`, lịch sử định giá
  của T6 có 2 bản ghi.
- Ghi tiếp một định giá lùi ngày `480.000.000 ₫` hiệu lực 01/08/2026: bản ghi hiện hành vẫn là `D−1`, nên
  snapshot giống từng byte với sau bước trước; lịch sử có 3 bản ghi.

### G — cửa sổ nghĩa vụ

Tài sản `Căn hộ Thảo Điền` `REAL_ESTATE` `4.200.000.000 ₫` `LOW` 01/10/2026.

| # | Tên | Category | Dư nợ | Lịch | Mục kỳ vọng |
|---|---|---|---|---|---|
| G1 | `Vay mua nhà Vietcombank` | `BANK_LOAN` | `2.400.000.000 ₫` | `25.000.000 ₫` `MONTHLY`, kỳ tới `D+13`, đáo hạn 15/11/2046 | `PAYMENT D+13 25.000.000` |
| G2 | `Thẻ tín dụng Techcombank` | `CREDIT_CARD_BALANCE` | `18.500.000 ₫` | kỳ tới `D+30`, không số tiền định kỳ | `PAYMENT D+30`, `amount null` |
| G3 | `Vay tiêu dùng HD Saison` | `PERSONAL_LOAN` | `36.000.000 ₫` | `3.000.000 ₫` `MONTHLY`, kỳ tới `D+31` | không có |
| G4 | `Vay cầm cố sổ tiết kiệm` | `BANK_LOAN` | `300.000.000 ₫` | đáo hạn `D+20` | `MATURITY D+20 300.000.000` |
| G5 | `Nợ mượn anh Hai` | `PERSONAL_LOAN` | `50.000.000 ₫` | không lịch | không có |
| G6 | `Vay margin SSI` | `MARGIN_LOAN` | `120.000.000 ₫` | kỳ tới `D−5` | `PAYMENT D−5 overdue`, `amount null` |
| G7 | `Vay mua xe VPBank` | `BANK_LOAN` | `8.500.000 ₫` | `8.500.000 ₫` `MONTHLY`, kỳ tới `D+7`, đáo hạn `D+7` | `PAYMENT D+7` rồi `MATURITY D+7` |

Thứ tự mục: G6, G7 `PAYMENT`, G7 `MATURITY`, G1, G4, G2 (6 mục). Tổng công nợ `2.400.000.000 + 18.500.000 +
36.000.000 + 300.000.000 + 50.000.000 + 120.000.000 + 8.500.000 = 2.933.000.000 ₫`; ròng
`1.267.000.000 ₫`; nợ/tài sản `2.933.000.000 / 4.200.000.000 = 0,698333…` → `0.698333`.

## Case

| Case | Nhóm | Hành động | Kỳ vọng |
|---|---|---|---|
| S01 | A | `GET` snapshot của A | `200`; `asOfDate = D`, `timeZone Asia/Ho_Chi_Minh`, `currency VND`, `exchangeRate {25100, 2026-09-12}`, `complete true` |
| S02 | A | như trên | `kpis`: tài sản `{12002000000, 12002000000, 11, 0}`, công nợ `{1532000000, 1532000000, 2, 0}`, ròng `10470000000`, nợ/tài sản `0.127645`, thanh khoản cao `{3180000000, 3180000000, 4, 0}` |
| S03 | A | như trên | dòng A02 `value 20000`, `currency USD`, `valueVnd 502000000` |
| S04 | A | như trên | `assetsByCategory` đúng 11 dòng, thứ tự và số ở trên |
| S05 | A | như trên | `assetsByLiquidity` `HIGH 3180000000/4`, `MEDIUM 1842000000/3`, `LOW 6980000000/4` |
| S06 | A | như trên | `largestAssets.items` theo tên: A07, A04, A03, A02, A05; `Xe Volvo XC60` không có; `excludedRecordCount 0` |
| S07 | A | như trên | `upcomingLiabilityDues`: `windowDays 30`, `windowEnd D+30`, đúng 1 mục L1 `PAYMENT D+28 18750000 VND 18750000 overdue false` |
| S08 | A | như trên | `kpis.lastDataUpdate = D`; `sourceDates` 14 dòng (11/2/1), dòng đầu là hai công nợ ngày `D`, có `EXCHANGE_RATE 2026-09-12`, sắp ngày giảm dần |
| S09 | A | `recon.jq` trên body A | mọi phép đối soát `true` |
| S10 | A | gọi lần hai | `cmp` hai body: giống từng byte |
| S11 | C | `GET` snapshot của C chưa có tỷ giá | `200`; `complete false`, `exchangeRate null`, các aggregate partial như trên, `netWorth null`, `debtToAssetRatio null`, dòng USD `valueVnd null` |
| S12 | C | như trên | không có số `0` thay cho không khả dụng: `kpis.totalAssets.amountVnd`, `totalLiabilities.amountVnd`, `liquidAssets.amountVnd`, `BANK_ACCOUNT.amountVnd` đều `null`; không có `EXCHANGE_RATE` trong `sourceDates` |
| S13 | C | `recon.jq` trên body partial | mọi phép `true` |
| S14 | C | `PUT /settings/exchange-rate` `25100` hiệu lực 20/11/2026, rồi `GET` | `complete true`; tài sản `650000000`, công nợ `125500000`, ròng `524500000`, nợ/tài sản `0.193077`; `BANK_ACCOUNT` đứng đầu danh mục; `EXCHANGE_RATE 2026-11-20` có trong `sourceDates` |
| S15 | C | `recon.jq` trên body sau khai | mọi phép `true` |
| S16 | N | `GET` của user mới | `200` (không `404`); ba aggregate `{0, 0, 0, 0}`, ròng `0`, nợ/tài sản `null`, `lastDataUpdate null`, mọi mảng rỗng, `largestAssets {items: [], excludedRecordCount: 0}`, `complete true`, `exchangeRate null` |
| S17 | N | `recon.jq` | mọi phép `true` |
| S18 | Z | `GET` của Z | tài sản `{0, 0, 1, 0}`, công nợ `50000000`, ròng `-50000000`, nợ/tài sản `null`; danh mục `VEHICLE_OR_VALUABLE_ASSET {0, 0, 1, 0}`; top 5 có xe máy `valueVnd 0` |
| S19 | T | `GET` của T | tổng `4000000000`, nợ/tài sản `0`, top 5 T1, T2, T3, T4, T5; trong `assets` `Ô tô Kia Morning` đứng ngay trước `Vàng SJC`; thanh khoản cao `2100000000`; `lastDataUpdate 2026-10-20` |
| S20 | T | `POST /assets/{T6}/valuations` `650000000` hiệu lực `D−1`, rồi `GET` | `201`; tổng `4150000000`; top 5 T1, T2, T3, T6, T4; thanh khoản cao `2250000000`; `lastDataUpdate D−1`; dòng T6 `valuationDate D−1` |
| S21 | T | `GET /assets/{T6}/valuations` | 2 bản ghi: lịch sử không mất |
| S22 | T | `POST` định giá lùi ngày `480000000` hiệu lực 01/08/2026, rồi `GET` | `201`; snapshot giống từng byte với S20; lịch sử 3 bản ghi |
| S23 | T | `recon.jq` | mọi phép `true` |
| S24 | G | `GET` của G | `upcomingLiabilityDues.items` đúng 6 mục theo thứ tự G6, G7 `PAYMENT`, G7 `MATURITY`, G1, G4, G2 với ngày, `dueKind`, `amount`, `overdue` như bảng; G3, G5 không có |
| S25 | G | như trên | công nợ `2933000000`, ròng `1267000000`, nợ/tài sản `0.698333` |
| S26 | G | `recon.jq` | mọi phép `true` |
| S27 | B | `GET` của B | chỉ một tài sản `Sổ tiết kiệm Agribank` `300000000`; không id nào của A xuất hiện trong body B và ngược lại |
| S28 | B | `GET ?userId=<id của A>` bằng token B | body giống từng byte với S27: route không nhận owner từ client |
| S29 | HTTP | không token / token hết hạn / token ký sai khóa | ba lần `401`, `application/problem+json`, `type urn:rikkaus:problem:unauthorized` |
| S30 | HTTP | `POST` và `DELETE` vào route | `405` problem |
| S31 | HTTP | `Accept: text/csv` | `406` |
| S32 | Contract | `GET /v3/api-docs` | có path `/api/v1/wealth-snapshot` chỉ với `get`, `security bearerAuth` |

## Đối soát (`recon.jq`)

Chạy trên mọi body `200` của snapshot; mọi giá trị phải `true`:

- Mỗi dòng tài sản: VND thì `valueVnd == value`; USD có tỷ giá thì `valueVnd == round(value × rate)`;
  USD không tỷ giá thì `valueVnd == null`. Tương tự dòng công nợ.
- `kpis.totalAssets`: `knownAmountVnd` = tổng `valueVnd` khác `null`; `recordCount` = số dòng;
  `excludedRecordCount` = số dòng `null`; `amountVnd` = `knownAmountVnd` khi không loại dòng nào, ngược lại
  `null`. Tương tự `totalLiabilities`, `liquidAssets` (chỉ dòng `HIGH`).
- `netWorth` = tài sản − công nợ, hoặc `null` khi một trong hai `null`.
- `debtToAssetRatio`: `null` khi tài sản `null`/`0` hoặc công nợ `null`; ngược lại lệch `công nợ / tài sản`
  dưới `0.0000005`.
- Mỗi dòng danh mục và thanh khoản bằng aggregate của các dòng tài sản cùng nhóm; nhóm không trùng; tổng
  `recordCount` các nhóm bằng số dòng tài sản; tổng `knownAmountVnd` các nhóm bằng tổng tài sản đã biết;
  thứ tự danh mục đúng quy tắc; thanh khoản theo `HIGH`, `MEDIUM`, `LOW`.
- `largestAssets.items` = 5 dòng đầu (theo thứ tự `assets`) có `valueVnd` khác `null`, cùng giá trị; số loại
  = số dòng tài sản `null`.
- Mỗi mục nghĩa vụ trỏ tới một dòng công nợ có đúng ngày đó ở `nextPaymentDate` hoặc `maturityDate`;
  `dueDate <= windowEnd`; `overdue == (dueDate < asOfDate)`; số tiền đúng loại mục; mọi ngày công nợ trong
  cửa sổ đều có mục.
- `lastDataUpdate` = ngày lớn nhất của `sourceDates`; `sourceDates` có đúng một dòng cho mỗi tài sản, mỗi
  công nợ, và một dòng tỷ giá khi và chỉ khi có dòng USD đã quy đổi.
- `complete` = không dòng nào `null`.

## Todo

- [x] Fixture và số kỳ vọng tính tay.
- [x] 32 case, mỗi case có hành động và kỳ vọng kiểm được bằng `jq`.
- [x] Các phép đối soát dùng chung.
