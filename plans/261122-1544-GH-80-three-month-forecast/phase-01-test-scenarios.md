# Phase 1 — Danh sách test scenario (viết trước khi code)

Status: Complete · Viết ngày 22/11/2026 trước khi có dòng code nào của `cashflow.forecast`.

Product Owner chỉ đạo không viết unit test hay integration test mới. Bảng dưới là phần thay thế: mỗi case
có kỳ vọng **tính tay** từ dữ liệu cố định, và phase 4 chạy đúng các case này qua HTTP vào PostgreSQL
Docker. Số case ở đây là số case trong guide và report.

## Ngày chạy và horizon

Ngày tính là **22/11/2026** theo `Asia/Ho_Chi_Minh`. Không truyền `from` thì horizon là 12/2026, 01/2027,
02/2027 (01/12/2026–28/02/2027). Khoảng không chiếu là 23/11/2026–30/11/2026.

## Dữ liệu

Tạo qua API của từng slice, không chèn thẳng (trừ bảng `wealth.users`, vì tài khoản sinh ra từ Google).
Dữ liệu khớp fixture design #72 và #60 về tên và mức tiền.

### User A — An Nguyễn

Tỷ giá: `PUT /settings/exchange-rate` `25100` hiệu lực `2026-09-12`.

| Mã | Bản ghi | Loại | Số tiền | Lịch |
|---|---|---|---:|---|
| AS1 | Tài khoản Vietcombank | `BANK_ACCOUNT`, `HIGH` | 85.000.000 ₫ | định giá 20/11/2026 |
| AS2 | Tiền mặt | `CASH`, `HIGH` | 12.000.000 ₫ | định giá 20/11/2026 |
| AS3 | Tài khoản USD | `BANK_ACCOUNT`, `HIGH` | 2.000 USD | định giá 20/11/2026 |
| AS4 | Tiết kiệm kỳ hạn 6 tháng | `TERM_DEPOSIT`, `HIGH` | 500.000.000 ₫ | định giá 20/11/2026 |
| E1 | Lương | thu `SALARY` | 45.000.000 ₫ | tháng từ 05/01/2026 |
| E2 | Chi phí sinh hoạt | chi `LIVING_COSTS` | 25.000.000 ₫ | tháng từ 01/01/2026 |
| E3 | Tiền thuê nhà | chi `HOUSING` | 15.000.000 ₫ | tháng 05/03/2026–05/01/2027 |
| E4 | Học phí | chi `EDUCATION` | 12.500.000 ₫ | quý từ 31/08/2026 |
| E5 | Thưởng Tết | thu `SALARY` | 60.000.000 ₫ | năm từ 25/01/2026 |
| E6 | Phí bảo hiểm nhân thọ | chi `INSURANCE` | 24.000.000 ₫ | năm từ 15/06/2026 |
| E7 | Cổ tức | thu `DIVIDEND` | 150 USD | quý từ 15/12/2026 |
| E8 | Sửa xe | chi `OTHER_EXPENSE` | 8.500.000 ₫ | một lần 18/01/2027 |
| E9 | Trả nợ vay sửa nhà | chi `DEBT_PRINCIPAL_PAYMENT`, liên kết L5 | 7.000.000 ₫ | tháng từ 20/05/2026 |
| E10 | Mua laptop | chi `OTHER_EXPENSE` | 32.000.000 ₫ | một lần 02/10/2026 |
| L1 | Vay mua nhà Vietcombank | `BANK_LOAN`, dư nợ 2.150.000.000 ₫ | 18.500.000 ₫ | tháng, kế 15/12/2026, đáo hạn 15/06/2041 |
| L2 | Thẻ tín dụng Techcombank | `CREDIT_CARD_BALANCE`, dư nợ 9.800.000 ₫ | 9.800.000 ₫ | tháng, kế 25/01/2027 |
| L3 | Vay mua xe VPBank | `BANK_LOAN`, dư nợ 12.400.000 ₫ | 6.200.000 ₫ | tháng, kế 10/12/2026, đáo hạn 10/01/2027 |
| L4 | Vay người thân | `PERSONAL_LOAN`, dư nợ 50.000.000 ₫ | — | không lịch, đáo hạn 31/01/2027 |
| L5 | Vay sửa nhà BIDV | `BANK_LOAN`, dư nợ 168.000.000 ₫ | 7.000.000 ₫ | tháng, kế 20/12/2026 |
| L6 | Vay ký quỹ SSI | `MARGIN_LOAN`, dư nợ 80.000.000 ₫ | — | không lịch, không đáo hạn |
| L7 | Vay mua đất Agribank | `BANK_LOAN`, dư nợ 900.000.000 ₫ | 30.000.000 ₫ | quý, kế 01/04/2027 |

### User B — Bích Trần (tháng âm)

| Mã | Bản ghi | Số tiền | Lịch |
|---|---|---:|---|
| BS1 | Tài khoản ACB (`BANK_ACCOUNT`) | 8.000.000 ₫ | định giá 20/11/2026 |
| BE1 | Lương của Bích (thu) | 30.000.000 ₫ | tháng từ 10/01/2026 |
| BE2 | Tiền thuê nhà (chi) | 12.000.000 ₫ | tháng từ 01/01/2026 |
| BE3 | Chi phí sinh hoạt (chi) | 14.000.000 ₫ | tháng từ 03/01/2026 |
| BE4 | Học phí khóa MBA (chi, một lần) | 45.000.000 ₫ | 08/01/2027 |

### User C — Chi Lê (chưa khai tỷ giá)

| Mã | Bản ghi | Số tiền | Lịch |
|---|---|---:|---|
| CS1 | Tài khoản USD HSBC (`BANK_ACCOUNT`) | 1.200 USD | định giá 20/11/2026 |
| CE1 | Lương của Chi (thu) | 25.000.000 ₫ | tháng từ 05/01/2026 |
| CE2 | Cổ tức FPT (thu) | 150 USD | quý từ 15/12/2026 |
| CL1 | Vay tín chấp Shinhan (`PERSONAL_LOAN`) | 4.300.000 ₫ | tháng, kế 28/10/2026 (đã qua) |

## Kỳ vọng tính tay cho A (trước case 31)

Số dư mở đầu: 85.000.000 + 12.000.000 + 2.000 × 25.100 = **147.200.000 ₫** (AS4 không tính).

| Ngày | Khoản | Số dư cuối ngày |
|---|---|---:|
| 01/12/2026 | điểm mở đầu | 147.200.000 |
| 01/12 | −25.000.000 E2 | 122.200.000 |
| 05/12 | +45.000.000 E1, −15.000.000 E3 | 152.200.000 |
| 10/12 | −6.200.000 L3 | 146.000.000 |
| 15/12 | +3.765.000 E7, −18.500.000 L1 | 131.265.000 |
| 20/12 | −7.000.000 E9 | 124.265.000 |
| 01/01/2027 | −25.000.000 E2 | 99.265.000 |
| 05/01 | +45.000.000 E1, −15.000.000 E3 | 129.265.000 |
| 10/01 | −6.200.000 L3 | 123.065.000 |
| 15/01 | −18.500.000 L1 | 104.565.000 |
| 18/01 | −8.500.000 E8 | 96.065.000 |
| 20/01 | −7.000.000 E9 | **89.065.000** (thấp nhất) |
| 25/01 | +60.000.000 E5, −9.800.000 L2 | 139.265.000 |
| 01/02 | −25.000.000 E2 | 114.265.000 |
| 05/02 | +45.000.000 E1 | 159.265.000 |
| 15/02 | −18.500.000 L1 | 140.765.000 |
| 20/02 | −7.000.000 E9 | 133.765.000 |
| 25/02 | −9.800.000 L2 | 123.965.000 |
| 28/02 | −12.500.000 E4 (dời) | 111.465.000 |

| Tháng | Thu | Chi | Net | Đầu tháng | Cuối tháng | Thấp nhất | partial |
|---|---:|---:|---:|---:|---:|---|---|
| 2026-12 | 48.765.000 | 71.700.000 | −22.935.000 | 147.200.000 | 124.265.000 | 122.200.000 ngày 01/12 | false |
| 2027-01 | 105.000.000 | 90.000.000 | +15.000.000 | 124.265.000 | 139.265.000 | 89.065.000 ngày 20/01 | true (L4) |
| 2027-02 | 45.000.000 | 72.800.000 | −27.800.000 | 139.265.000 | 111.465.000 | 111.465.000 ngày 28/02 | false |

Ngưỡng tham chiếu: (71.700.000 + 90.000.000 + 72.800.000) / 3 = **78.166.666,67 ₫**. Thấp nhất 89.065.000 ≥
ngưỡng → `ADEQUATE`.

## Các case

### A. Horizon và tham số (1–4)

| # | Request | Kỳ vọng |
|---|---|---|
| 1 | A, không `from` | `200`; `horizon` 2026-12-01..2027-02-28, `months` `[2026-12, 2027-01, 2027-02]`; `assumptions.asOfDate` 2026-11-22, `timeZone` `Asia/Ho_Chi_Minh`, `notProjected` 2026-11-23..2026-11-30 |
| 2 | A, `from=2026-12` | body giống case 1 từng byte |
| 3 | A, `from=2027-01` | `200`; horizon 2027-01-01..2027-03-31; `notProjected` 2026-11-23..2026-12-31; số dư mở đầu vẫn 147.200.000 áp ngày 2027-01-01 |
| 4 | A, `from=2027-11` (xa nhất) | `200`; horizon 2027-11-01..2028-01-31 |

### B. Entry định kỳ và một lần (5–13)

| # | Nguồn | Kỳ vọng trong `items` (ngày, sequence, shiftedToMonthEnd) |
|---|---|---|
| 5 | E1 tháng | 2026-12-05 s12, 2027-01-05 s13, 2027-02-05 s14; `INCOME`, `SALARY`, `45000000.00`, `cadence: MONTHLY` |
| 6 | E4 quý | chỉ 2027-02-28 s3 dời (`true`) |
| 7 | E5 năm | chỉ 2027-01-25 s2 |
| 8 | E3 `endDate` 05/01/2027 giữa horizon | 2026-12-05 s10, 2027-01-05 s11; không có 02/2027 |
| 9 | E6 năm, kỳ kế 15/06/2027 | không có trong `items`; `excludedSources` có `CASH_FLOW_ENTRY` E6 `NO_OCCURRENCE_IN_HORIZON` |
| 10 | E7 USD quý | 2026-12-15 s1; `amount` `150.00` `USD`, `amountVnd` `3765000.00` |
| 11 | E8 một lần tương lai | 2027-01-18 s1, `cadence: null` |
| 12 | E10 một lần quá khứ | không có trong `items`, không có trong `excludedSources` |
| 13 | E2 tháng | 2026-12-01 s12, 2027-01-01 s13, 2027-02-01 s14 |

### C. Công nợ (14–20)

| # | Nguồn | Kỳ vọng |
|---|---|---|
| 14 | L1 tháng đến 2041 | `LIABILITY` 2026-12-15 s1, 2027-01-15 s2, 2027-02-15 s3; `EXPENSE`, `BANK_LOAN`, `18500000.00`, `cadence: MONTHLY` |
| 15 | L2 `nextPaymentDate` trong tháng 2 của horizon | 2027-01-25 s1, 2027-02-25 s2; không có 12/2026 |
| 16 | L3 `maturityDate` trước kỳ thứ ba | 2026-12-10 s1, 2027-01-10 s2; không có 02/2027 |
| 17 | L4 không lịch, đáo hạn trong horizon | không có trong `items`; loại `NO_PAYMENT_SCHEDULE`, `affectedMonths: ["2027-01"]`; tháng 2027-01 `partial: true`, hai tháng kia `false` |
| 18 | L6 không lịch, không đáo hạn | loại `NO_PAYMENT_SCHEDULE`, `affectedMonths: []` |
| 19 | L5 có entry định kỳ liên kết E9 | không có `LIABILITY` L5 trong `items`; loại `COVERED_BY_LINKED_ENTRIES`, `linkedEntryIds: [E9]`; E9 có 2026-12-20 s8, 2027-01-20 s9, 2027-02-20 s10 |
| 20 | L7 quý, kỳ kế sau horizon | loại `NO_PAYMENT_IN_HORIZON` |

### D. Tổng, số dư và tín hiệu của A (21–29)

| # | Kiểm | Kỳ vọng |
|---|---|---|
| 21 | `openingBalance` | `amount` `147200000.00`, `appliedOn` 2026-12-01; đúng ba `sources` AS1 85.000.000, AS2 12.000.000, AS3 `2000.00 USD` → `50200000.00`; AS4 không có |
| 22 | tháng 2026-12 | bảng trên |
| 23 | tháng 2027-01 | bảng trên, `partial: true` |
| 24 | tháng 2027-02 | bảng trên |
| 25 | `minimumBalance` | `date` 2027-01-20, `month` 2027-01, `amount` `89065000.00`, `negative: false`, `referenceThreshold` `78166666.67`, `status: ADEQUATE`; `partial: true` ở gốc |
| 26 | số khoản mục và tổng | 7, 9, 6 khoản; tổng `amountVnd` theo `direction` của từng tháng bằng `income`/`expense` |
| 27 | thứ tự | trong ngày 15/12, E7 (`CASH_FLOW_ENTRY`) đứng trước L1 (`LIABILITY`); ngày 25/01, E5 trước L2; cùng ngày cùng loại theo `sourceId` |
| 28 | `excludedSources` | đúng năm mục: E6, L4, L5, L6, L7 với lý do ở case 9, 17–20; sắp `CASH_FLOW_ENTRY` trước `LIABILITY`, rồi `sourceId` |
| 29 | `assumptions` | `exchangeRate` `{usdToVndRate: 25100.000000, effectiveDate: 2026-09-12}`, `currency: VND`, `openingBalanceCategories: [BANK_ACCOUNT, CASH]` |

### E. Truy vết và dữ liệu sống (30–32)

| # | Kiểm | Kỳ vọng |
|---|---|---|
| 30 | mọi `sourceId` trong `items` và `openingBalance.sources` | `GET /cash-flow-entries/{id}`, `/liabilities/{id}`, `/assets/{id}` đều `200`; `amount`/`recurringPaymentAmount`/`currentValue` và `currency` trùng khoản mục |
| 31 | xóa E5 rồi gọi lại | không còn khoản E5; 2027-01 thu 45.000.000, net −45.000.000, cuối 79.265.000, thấp nhất 79.265.000 ngày 25/01; 2027-02 cuối 51.465.000; `minimumBalance` 51.465.000 ngày 2027-02-28, `status: THIN`, ngưỡng vẫn 78.166.666,67 |
| 32 | xóa E9 rồi gọi lại | L5 vào `items` (2026-12-20 s1, 2027-01-20 s2, 2027-02-20 s3, `LIABILITY`); L5 không còn trong `excludedSources`; tổng tháng không đổi so với case 31 |

### F. Chưa khai tỷ giá — user C (33–35)

| # | Kiểm | Kỳ vọng |
|---|---|---|
| 33 | C trước khi khai tỷ giá | `200`; `assumptions.exchangeRate: null`; `openingBalance.amount` `0.00`, `sources: []`; loại `CASH_FLOW_ENTRY` CE2 `EXCHANGE_RATE_MISSING` `["2026-12"]` và `ASSET` CS1 `EXCHANGE_RATE_MISSING` `[]`; 2026-12 `partial: true`, thu 25.000.000, chi 4.300.000; `status: INSUFFICIENT_DATA`; `partial: true` |
| 34 | CL1 neo quá khứ 28/10/2026 | 2026-12-28 s3, 2027-01-28 s4, 2027-02-28 s5, không dời |
| 35 | C khai `25300` hiệu lực 2026-11-20 rồi gọi lại | mở đầu `30360000.00` (CS1); CE2 `amountVnd` `3795000.00`; `excludedSources: []`; `partial: false`; cuối tháng 54.855.000, 75.555.000, 96.255.000; thấp nhất 30.360.000 ngày 2026-12-01, ngưỡng `4300000.00`, `ADEQUATE` |

### G. Tháng âm — user B (36–41)

| # | Kiểm | Kỳ vọng |
|---|---|---|
| 36 | 2026-12 | thu 30.000.000, chi 26.000.000, net +4.000.000, cuối 12.000.000, thấp nhất −18.000.000 ngày 03/12 |
| 37 | 2027-01 | thu 30.000.000, chi 71.000.000, net −41.000.000, cuối −29.000.000, thấp nhất −59.000.000 ngày 08/01 |
| 38 | 2027-02 | thu 30.000.000, chi 26.000.000, net +4.000.000, cuối −25.000.000, thấp nhất −55.000.000 ngày 03/02 |
| 39 | `minimumBalance` | 2027-01-08, `month` 2027-01, `-59000000.00`, `negative: true`, `status: NEGATIVE`, ngưỡng `41000000.00` |
| 40 | B không thấy A | mọi `sourceId` trong body của B thuộc BS1, BE1–BE4; mở đầu chỉ BS1 8.000.000 |
| 41 | B đọc source của A | `GET /cash-flow-entries/{E1}` bằng token B → `404` |

### H. Tham số sai (42–48)

Mọi case: `400`, `type` `urn:rikkaus:problem:validation-failed`, `errors` đúng một mục `field: from`;
message không lặp lại giá trị gửi lên.

| # | `from` | message |
|---|---|---|
| 42 | `2026-13` | `must be a month in the form YYYY-MM` |
| 43 | `2026-12-01` | như trên |
| 44 | `abc` | như trên |
| 45 | rỗng (`from=`) | như trên |
| 46 | `2026-11` (tháng hiện tại) | `must not be before the month after today` |
| 47 | `2026-10` | như trên |
| 48 | `2027-12` | `must be at most 12 months after the current month` |

### I. Chứng thực, method, media type (49–53)

| # | Request | Kỳ vọng |
|---|---|---|
| 49 | không token | `401` `urn:rikkaus:problem:unauthorized` |
| 50 | token hết hạn | `401` |
| 51 | token ký sai secret | `401` |
| 52 | `POST` | `405` `method-not-allowed` |
| 53 | `Accept: application/xml` | `406` |

### J. Xác định (54–55)

| # | Kiểm | Kỳ vọng |
|---|---|---|
| 54 | A gọi hai lần liên tiếp | sha256 hai body bằng nhau |
| 55 | body thô | chứa chuỗi `"amount":45000000.00`, `"usdToVndRate":25100.000000`, `"referenceThreshold":78166666.67` |
