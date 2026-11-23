# Phase 1 — Danh sách test scenario (viết trước khi code)

Status: Complete · Viết ngày 22/11/2026 trước khi có dòng code nào của `goals.projection`.

Product Owner chỉ đạo không viết unit test hay integration test mới. Bảng dưới là phần thay thế: mỗi case
có kỳ vọng tính trước từ dữ liệu cố định, và phase 4 chạy đúng các case này qua HTTP vào PostgreSQL Docker.
Số case ở đây là số case trong guide và report.

Kỳ vọng được tính bằng một máy tính độc lập (Python `decimal`, độ chính xác 50 chữ số, viết riêng theo các
quyết định D3–D13 của `plan.md`, không dùng code production). Sáu dòng fixture design #87 cho đúng mức cần
mỗi tháng mà design công bố, nên máy tính và design xác nhận lẫn nhau.

## Ngày chạy

Ngày tham chiếu là **22/11/2026** theo `Asia/Ho_Chi_Minh`; tháng góp đầu tiên là 12/2026;
`n = (năm đích − 2026) × 12 + (tháng đích − 11)`.

Design #87 dùng ngày tham chiếu 20/09/2026 (tháng góp đầu 10/2026). Không có tham số `asOf` (D2), nên
fixture được tái dựng bằng cách **dời ngày đích thêm hai tháng**: cùng `n`, cùng mọi số.

## Dữ liệu

Tạo qua `POST /api/v1/goals` của #86 (không chèn thẳng), trừ hai bảng: `wealth.users` (tài khoản sinh từ
Google) và ba goal quá hạn, vì API từ chối ngày đích không sau hôm nay; ba goal đó được tạo với ngày tương
lai rồi `UPDATE wealth.goals SET target_date = ...` trong container.

### User A — An Nguyễn

| Mã | Tên | Loại | Mục tiêu | Đã phân bổ | Ngày đích | Góp/tháng | Lợi suất |
|---|---|---|---:|---:|---|---:|---:|
| STD | Mua căn hộ | `HOME_PURCHASE` | 3.500.000.000 | 600.000.000 | 31/12/2030 | 25.000.000 | 7,00 |
| F1 | Quỹ dự phòng khẩn cấp | `EMERGENCY_FUND` | 552.000.000 | 350.000.000 | 29/02/2028 | 15.000.000 | 3,00 |
| F2 | Mua nhà cho con | `HOME_PURCHASE` | 3.000.000.000 | 1.200.000.000 | 31/08/2031 | 25.000.000 | 6,00 |
| F3 | Học phí đại học cho con | `EDUCATION` | 900.000.000 | 150.000.000 | 31/10/2032 | 8.000.000 | 5,00 |
| F4 | Hưu trí | `RETIREMENT` | 15.000.000.000 | 2.150.000.000 | 28/02/2042 | 20.000.000 | 8,00 |
| F5 | Tất toán vay mua nhà | `DEBT_REDUCTION` | 1.500.000.000 | 90.000.000 | 28/02/2031 | 10.000.000 | 0,00 |
| F6 | Tăng trưởng tài sản ròng | `NET_WORTH_GROWTH` | 15.000.000.000 | 10.470.000.000 | 28/02/2031 | 10.000.000 | 7,00 |
| E1 | Quỹ học tiếp cho bản thân | `EDUCATION` | 400.000.000 | 40.000.000 | 31/12/2029 | — | 4,00 |
| E2 | Du lịch Nhật Bản | `OTHER_GOAL` | 120.000.000 | — | 31/03/2028 | 5.000.000 | — |
| E3 | Quỹ bảo dưỡng xe | `OTHER_GOAL` | 150.000.000 | 30.000.000 | 30/06/2028 | 0 | 4,00 |
| E4 | Quỹ mua xe máy | `OTHER_GOAL` | 80.000.000 | 80.000.000 | 30/06/2027 | 0 | 0,00 |
| E5 | Quà kỷ niệm 20 năm | `OTHER_GOAL` | 120.000.000 | 95.000.000 | 30/11/2026 | 0 | 0,00 |
| E6 | Đổi xe gia đình | `OTHER_GOAL` | 800.000.000 | 620.000.000 | 31/10/2026 (SQL) | 0 | 4,00 |
| E7 | Du lịch châu Âu | `OTHER_GOAL` | 250.000.000 | 250.000.000 | 30/09/2026 (SQL) | 0 | 0,00 |
| E8 | Quà sinh nhật mẹ | `OTHER_GOAL` | 30.000.000 | 12.000.000 | 15/11/2026 (SQL) | 3.000.000 | 0,00 |
| E9 | Quỹ sửa nhà | `OTHER_GOAL` | 100.000.000 | — | 30/06/2027 | 14.000.000 | 0,00 |
| E10 | Quỹ hưu sớm | `RETIREMENT` | 1.000.000.000 | 900.000.000 | 30/11/2036 | 0 | 6,00 |
| E11 | Quỹ cưới | `OTHER_GOAL` | 200.000.000 | — | 31/12/2028 | — | — |
| T | Quỹ du học hè | `EDUCATION` | 120.000.000 | — | 30/11/2027 | 10.000.000 | 0,00 |

F1–F6 là sáu goal của design #87 với ngày đích dời hai tháng. Tổng góp F1–F6 = 88.000.000 ₫ như design.

### User B — Bích Trần

| Mã | Tên | Loại | Mục tiêu | Đã phân bổ | Ngày đích | Góp/tháng | Lợi suất |
|---|---|---|---:|---:|---|---:|---:|
| B1 | Quỹ khởi nghiệp | `OTHER_GOAL` | 500.000.000 | 100.000.000 | 30/06/2029 | 8.000.000 | 5,00 |

## Kỳ vọng tính trước

Tiền theo đồng, hai số lẻ khi khác 0. `cần` là `requiredMonthlyContribution`, `bao phủ` là
`contributionCoveragePercent`, `từ phân bổ` / `từ đóng góp` / `tổng` / `chênh` là bốn trường của
`projectedAtTargetDate`.

| Mã | `n` | Hoàn thành | Còn lại | Cần | Bao phủ | Trạng thái | Từ phân bổ | Từ đóng góp | Tổng | Chênh |
|---|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|
| STD | 49 | 17,14 | 2.900.000.000 | 47.800.000 | 52,30 | `BEHIND` | 797.859.515,30 | 1.413.282.252,17 | 2.211.141.767,47 | −1.288.858.232,53 |
| F1 | 15 | 63,40 | 202.000.000 | 12.358.000 | 121,37 | `ON_TRACK` | 363.357.194,55 | 228.980.477,94 | 592.337.672,49 | 40.337.672,49 |
| F2 | 57 | 40,00 | 1.800.000.000 | 21.371.000 | 116,98 | `ON_TRACK` | 1.594.581.665,13 | 1.644.090.271,39 | 3.238.671.936,52 | 238.671.936,52 |
| F3 | 71 | 16,66 | 750.000.000 | 8.475.000 | 94,39 | `AT_RISK` | 201.513.024,02 | 659.366.707,50 | 860.879.731,52 | −39.120.268,48 |
| F4 | 183 | 14,33 | 12.850.000.000 | 21.760.000 | 91,91 | `AT_RISK` | 7.253.028.890,75 | 7.120.505.428,95 | 14.373.534.319,70 | −626.465.680,30 |
| F5 | 51 | 6,00 | 1.410.000.000 | 27.648.000 | 36,16 | `BEHIND` | 90.000.000 | 510.000.000 | 600.000.000 | −900.000.000 |
| F6 | 51 | 69,80 | 4.530.000.000 | 15.448.000 | 64,73 | `BEHIND` | 14.085.553.198,50 | 591.985.787,72 | 14.677.538.986,22 | −322.461.013,78 |
| E1 | 37 | 10,00 | 360.000.000 | 9.025.000 | `null` | `null` | `null` | | | |
| E2 | 16 | 0,00 | 120.000.000 | `null` | `null` | `null` | `null` | | | |
| E3 | 19 | 20,00 | 120.000.000 | 6.029.000 | 0,00 | `BEHIND` | 31.958.091,17 | 0 | 31.958.091,17 | −118.041.908,83 |
| E4 | 7 | 100,00 | 0 | 0 | 100,00 | `ON_TRACK` | 80.000.000 | 0 | 80.000.000 | 0 |
| E9 | 7 | 0,00 | 100.000.000 | 14.286.000 | 97,99 | `AT_RISK` | 0 | 98.000.000 | 98.000.000 | −2.000.000 |
| E10 | 120 | 90,00 | 100.000.000 | 0 | 100,00 | `ON_TRACK` | 1.637.457.060,63 | 0 | 1.637.457.060,63 | 637.457.060,63 |
| E11 | 25 | 0,00 | 200.000.000 | `null` | `null` | `null` | `null` | | | |
| T | 12 | 0,00 | 120.000.000 | 10.000.000 | 100,00 | `ON_TRACK` | 0 | 120.000.000 | 120.000.000 | 0 |
| B1 | 31 | 20,00 | 400.000.000 | 11.698.000 | 68,38 | `BEHIND` | 113.757.444,36 | 264.142.931,68 | 377.900.376,04 | −122.099.623,96 |

`calculation` (`monthlyRatePercent` 6 số lẻ / `growthFactor` 10 số lẻ): STD và F6 `0.583333` /
`1.3297658588` và `1.3453250428`; F1 `0.250000` / `1.0381634130`; F2 `0.500000` / `1.3288180543`; F3
`0.416667` / `1.3434201602`; F4 `0.666667` / `3.3735018097`; F5, E4, E9, T `0.000000` / `1.0000000000`;
E1 `0.333333` / `1.1310294474`; E3 `0.333333` / `1.0652697056`; E10 `0.500000` / `1.8193967340`; B1
`0.416667` / `1.1375744436`; E2, E11 `null`. (Bản đầu của phase này ghi lãi tháng dạng tỷ lệ 10 số lẻ;
đổi sang phần trăm 6 số lẻ khi viết code vì lý do nêu ở D13, giá trị không đổi.)

Đối chiếu design #87 (ngày đích gốc): mức cần 12.358.000 / 21.371.000 / 8.475.000 / 21.760.000 /
27.648.000 / 15.448.000 và trạng thái Đúng kế hoạch ×2, Có rủi ro ×2, Chậm kế hoạch ×2 phải khớp tuyệt đối.
Tỷ lệ design hiển thị một số lẻ làm tròn (121,4 / 117,0 / 94,4 / 91,9 / 36,2 / 64,7); API trả hai số lẻ làm
tròn xuống (D13), làm tròn lại một số lẻ phải ra đúng số design. F2 làm tròn về đồng phải ra 1.594.581.665 /
1.644.090.271 / 3.238.671.937 như design.

## Case

| # | Request | Kỳ vọng |
|---|---|---|
| P01 | A `GET /goal-projections/{STD}` | `200`; dòng STD; `timeline: ACTIVE`, `targetReached: false`, `remainingMonths: 49`, `daysPastTargetDate: null`, `unavailableReasons: []`, `asOfDate: 2026-11-22`, `currency: VND`, `goalName: Mua căn hộ` |
| P02–P07 | A `GET /goal-projections/{F1..F6}` | `200`; đúng dòng F1–F6; khớp design như trên |
| P08 | F5 (`r = 0`) | `fromContributions = 10.000.000 × 51 = 510.000.000.00`; `fromAllocated = 90.000.000.00`; `monthlyRatePercent 0.000000` |
| P09 | E3 (góp 0) | `fromContributions: 0.00` và `contributionCoveragePercent: 0.00` là số thật, không `null`; `BEHIND` |
| P10 | E4 (đã đủ, còn tháng) | `targetReached: true`, `completionPercent: 100.00`, `remainingAmount: 0.00`, `required: 0.00`, `ON_TRACK`, `gapToTarget: 0.00` |
| P11 | E10 (lãi tự đủ) | công thức thô ra −3.889.794,98 → `required: 0.00`, `coverage: 100.00`, `ON_TRACK` |
| P12 | E9 (làm tròn lên) | `100.000.000 / 7 = 14.285.714,29` → `required: 14286000.00`; `coverage 97.99`; `AT_RISK` |
| P13 | E1 (thiếu góp) | `required: 9025000.00`; `coverage`, `status`, `projectedAtTargetDate` `null`; `unavailableReasons: [PLANNED_MONTHLY_CONTRIBUTION_MISSING]` |
| P14 | E2 (thiếu lợi suất) | `required`, `coverage`, `status`, `projectedAtTargetDate`, `calculation` `null`; `[EXPECTED_ANNUAL_RETURN_MISSING]`; `completionPercent 0.00` |
| P15 | E11 (thiếu cả hai) | như P14 và `unavailableReasons: [PLANNED_MONTHLY_CONTRIBUTION_MISSING, EXPECTED_ANNUAL_RETURN_MISSING]` |
| P16 | E5 (đích 30/11/2026) | `timeline: DUE_THIS_MONTH`, `remainingMonths: 0`, `completionPercent 79.16`, `remainingAmount 25000000.00`; `required`, `coverage`, `status`, `projectedAtTargetDate`, `calculation` `null`; `[NO_CONTRIBUTION_MONTH]` |
| P17 | E6 (đích 31/10/2026) | `timeline: EXPIRED`, `targetReached: false`, `daysPastTargetDate: 22`, `remainingAmount 180000000.00`, `completionPercent 77.50`, `remainingMonths 0`; các output dự phóng `null`; `[TARGET_DATE_PASSED]` |
| P18 | E7 (đích 30/09/2026) | `EXPIRED`, `targetReached: true`, `daysPastTargetDate: 53`, `completionPercent 100.00`, `remainingAmount 0.00` |
| P19 | E8 (đích 15/11/2026, cùng tháng) | `EXPIRED` (không phải `DUE_THIS_MONTH`, D4), `daysPastTargetDate: 7`, `remainingAmount 18000000.00` |
| P20 | T góp 10.000.000 | `required 10000000.00`, `coverage 100.00`, `ON_TRACK` (biên trên) |
| P21 | `PUT /goals/{T}` góp 9.999.999 rồi `GET` | `coverage 99.99`, `AT_RISK`, `total 119999988.00`, `gapToTarget -12.00` |
| P22 | `PUT` góp 8.000.000 rồi `GET` | `coverage 80.00`, `AT_RISK` (biên dưới) |
| P23 | `PUT` góp 7.999.999 rồi `GET` | `coverage 79.99`, `BEHIND` |
| P24 | `PUT /goals/{STD}` góp 40.000.000 rồi `GET` | `fromContributions 2261251603.47`, `total 3059111118.77`, `gapToTarget -440888881.23`, `coverage 83.68`, `AT_RISK`; `required` vẫn `47800000.00` |
| P25 | `PUT /goals/{STD}` bỏ `expectedAnnualReturnPercent` rồi `GET` | `required`, `status`, `projectedAtTargetDate`, `calculation` `null`; `[EXPECTED_ANNUAL_RETURN_MISSING]`; ledger `EXPECTED_ANNUAL_RETURN_PERCENT` có `value: null` |
| P26 | `PUT /goals/{STD}` lợi suất 7,00, đích 31/12/2032 rồi `GET` | `remainingMonths 73`, `required 28481000.00`, `coverage 140.44`, `ON_TRACK`, `fromAllocated 917383671.82`, `fromContributions 3627241963.64`, `total 4544625635.46`, `growthFactor 1.5289727864` |
| P27 | `DELETE /goals/{E9}` rồi `GET /goal-projections/{E9}` | `204` rồi `404 not-found` |
| P28 | A `GET /goal-projections` | `200`; mảng có đúng các id và đúng thứ tự của A `GET /goals`; mỗi phần tử giống từng byte body `GET /goal-projections/{id}` tương ứng |
| P29 | B `GET /goal-projections` trước khi tạo B1, rồi sau | `200 []`; sau đó đúng một phần tử B1 với số như bảng |
| P30 | B `GET /goal-projections/{STD}` | `404 not-found`; `type`, `title`, `status`, `detail` giống P31; không lộ gì của A |
| P31 | A `GET /goal-projections/{uuid ngẫu nhiên}` | `404 not-found` |
| P32 | A `GET /goal-projections/khong-phai-uuid` | `400 malformed-request` |
| P33–P35 | không token / token hết hạn / token ký sai khóa, trên cả hai route | `401`, `application/problem+json` |
| P36 | A `POST /goal-projections/{STD}` | `405` |
| P37 | A `DELETE /goal-projections` | `405` |
| P38 | A `GET /goal-projections/{STD}` với `Accept: application/xml` | `406` |
| P39 | A gọi `GET /goal-projections/{F2}` hai lần, `GET /goal-projections` hai lần | sha256 hai lần giống nhau |
| P40 | Sổ giả định và phương pháp của F2 | 17 mục theo thứ tự: 5 `USER_ENTERED` (`TARGET_AMOUNT 3000000000.00`, `CURRENT_ALLOCATED_AMOUNT 1200000000.00`, `TARGET_DATE 2031-08-31`, `PLANNED_MONTHLY_CONTRIBUTION 25000000.00`, `EXPECTED_ANNUAL_RETURN_PERCENT 6.00`), 8 `SYSTEM` (`AS_OF_DATE 2026-11-22`, `TIME_ZONE Asia/Ho_Chi_Minh`, `FIRST_CONTRIBUTION_MONTH 2026-12`, `COMPOUNDING MONTHLY`, `CONTRIBUTION_TIMING END_OF_MONTH`, `REQUIRED_CONTRIBUTION_ROUNDING UP_TO_1000_VND`, `ON_TRACK_MIN_COVERAGE_PERCENT 100`, `AT_RISK_MIN_COVERAGE_PERCENT 80`), 4 `NOT_MODELLED` với `value: null` (`INFLATION`, `TAXES_AND_FEES`, `RETURN_VARIABILITY`, `CASH_FLOW_LINK`); `disclaimer` nói lợi suất không được đảm bảo; `method.status` nêu 100 và 80 |
| P41 | Kiểm toàn bộ danh sách A | mọi phần tử `ACTIVE` có dự phóng thỏa `fromAllocated + fromContributions = total` và `total − target = gapToTarget`; không trường nào trong `unavailableReasons` đi kèm giá trị `0` thay cho `null` |
| P42 | Khởi động lại với `--rikkaus.goals.projection.at-risk-min-coverage-percent=95` | F3 (94,39) và F4 (91,91) thành `BEHIND`; ~~E9 (97,99) vẫn `AT_RISK`~~ E9 đã bị xóa ở P27, lần chạy dùng F1 (121,37) vẫn `ON_TRACK`; ledger `AT_RISK_MIN_COVERAGE_PERCENT` là `95` |
| P43 | Khởi động với `at-risk-min-coverage-percent=120` (lớn hơn on-track 100) | ứng dụng không khởi động; log nêu tên property |
| P44 | `GET /goals/{STD}` sau mọi `GET` dự phóng | `updatedAt` không đổi vì đọc dự phóng không ghi gì |
