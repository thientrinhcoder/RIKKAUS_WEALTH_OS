---
phase: 1
title: "Danh sách test scenario"
status: completed
priority: P1
effort: "0.3d"
dependencies: []
---

# Phase 1: Danh sách test scenario

## Mục tiêu

Viết toàn bộ scenario trước khi có dòng code nào (TDD theo nghĩa scenario-first đã chốt với Product
Owner). Mỗi case là một request cụ thể với dữ liệu Việt Nam thật và kết quả mong đợi đủ rõ để người
khác chạy và chấm được. Phase 4 chạy đúng danh sách này và ghi kết quả thật.

Quy ước: `A`, `B` là hai user; mọi request dùng token của A trừ khi ghi khác. "→ [X, Y]" là danh sách
`name` (assets, liabilities, goals) hoặc `notes` (cash-flow, ký hiệu `CFn`) theo đúng thứ tự trả về.
Mọi lỗi là `application/problem+json`; với `validation-failed`, assertion là mảng `errors` (field và
message), không chỉ status 400. "Thứ tự collation" nghĩa là thứ tự mà
`SELECT name FROM wealth.<bảng> WHERE user_id = :A ORDER BY name, id` trả về trên cùng database: đó là
oracle cho sort theo tên, vì vị trí của `Đ` so với `D` phụ thuộc collation.

## Dữ liệu dùng xuyên suốt

### Assets

| Ký hiệu | Owner | name | category | currency, giá trị | liquidity | valuationDate | institution | notes |
|---|---|---|---|---|---|---|---|---|
| AS1 | A | `Tài khoản Vietcombank` | `BANK_ACCOUNT` | VND 85.000.000 | `HIGH` | 2026-11-20 | `Vietcombank` | — |
| AS2 | A | `Tiền mặt dự phòng` | `CASH` | VND 12.500.000 | `HIGH` | 2026-11-20 | — | `Cất trong két sắt ở nhà` |
| AS3 | A | `Tiền gửi 12 tháng Techcombank` | `TERM_DEPOSIT` | VND 500.000.000 | `MEDIUM` | 2026-10-15 | `Techcombank` | `Đáo hạn 15/10/2027, lãi 4,8%/năm` |
| AS4 | A | `Tài khoản USD` | `BANK_ACCOUNT` | USD 20.000,00 | `HIGH` | 2026-09-12 | `HSBC Việt Nam` | — |
| AS5 | A | `Căn hộ Thảo Điền` | `REAL_ESTATE` | VND 6.100.000.000 | `LOW` | 2026-08-02 | `Sổ hồng cá nhân` | — |
| AS6 | A | `Chứng chỉ quỹ VN30 (phí 0,6%)` | `INVESTMENT_FUND` | VND 150.000.000 | `MEDIUM` | 2026-07-31 | `SSIAM` | — |
| AS7 | A | `Nhà phố Đà Lạt` gửi ở dạng **NFD** (dấu tổ hợp tách rời) | `REAL_ESTATE` | VND 3.800.000.000 | `LOW` | 2026-06-30 | — | — |
| ASB1 | B | `Nhà phố Thảo Điền` | `REAL_ESTATE` | VND 9.500.000.000 | `LOW` | 2026-10-01 | — | — |
| ASB2 | B | `Tài khoản Vietcombank` | `BANK_ACCOUNT` | VND 40.000.000 | `HIGH` | 2026-11-01 | `Vietcombank` | — |

### Liabilities

| Ký hiệu | Owner | name | category | currency, dư nợ | nextPaymentDate | notes |
|---|---|---|---|---|---|---|
| LB1 | A | `Vay mua nhà Vietcombank` | `BANK_LOAN` | VND 2.400.000.000, lãi 8,5% | 2026-12-05 | `Thế chấp căn hộ Thảo Điền.` |
| LB2 | A | `Thẻ tín dụng Techcombank Visa` | `CREDIT_CARD_BALANCE` | VND 18.700.000 | 2026-12-01 | — |
| LB3 | A | `Dư nợ ký quỹ SSI` | `MARGIN_LOAN` | VND 350.000.000 | — (null) | — |
| LB4 | A | `Vay du học USD` | `PERSONAL_LOAN` | USD 12.000,00 | 2027-01-10 | `Vay người thân, không lãi` |
| LBB1 | B | `Vay mua nhà Vietcombank` | `BANK_LOAN` | VND 1.800.000.000 | 2026-12-15 | — |

### Cash-flow entries

| Ký hiệu | Owner | entryType / category | currency, số tiền | timing / recurrence | effectiveDate | notes |
|---|---|---|---|---|---|---|
| CF1 | A | `INCOME` / `SALARY` | VND 45.000.000 | `RECURRING` / `MONTHLY` | 2026-01-05 | `Lương tháng công ty Sao Mai` |
| CF2 | A | `EXPENSE` / `EDUCATION` | VND 12.500.000 | `ONE_TIME` | 2026-09-03 | `Học phí học kỳ 1 trường Quốc tế Á Châu` |
| CF3 | A | `EXPENSE` / `HOUSING` | VND 15.000.000 | `RECURRING` / `MONTHLY` | 2026-03-01 | `Tiền thuê căn hộ Thảo Điền` |
| CF4 | A | `INCOME` / `DIVIDEND` | USD 350,00 | `ONE_TIME` | 2026-06-20 | `Cổ tức quỹ ETF Mỹ` |
| CF5 | A | `EXPENSE` / `INSURANCE` | VND 24.000.000 | `RECURRING` / `YEARLY` | 2026-04-15 | `Bảo hiểm nhân thọ` |
| CF6 | A | `INCOME` / `BUSINESS_INCOME` | VND 26.200.000 | `ONE_TIME` | 2026-09-10 | — (null) |
| CFB1 | B | `INCOME` / `SALARY` | VND 30.000.000 | `RECURRING` / `MONTHLY` | 2026-02-01 | `Lương tháng` |

### Goals

| Ký hiệu | Owner | name | type | targetAmount | targetDate |
|---|---|---|---|---|---|
| GL1 | A | `Quỹ dự phòng 6 tháng` | `EMERGENCY_FUND` | 270.000.000 ₫ | 2027-06-30 |
| GL2 | A | `Mua nhà Thủ Đức` | `HOME_PURCHASE` | 3.500.000.000 ₫ | 2031-12-31 |
| GL3 | A | `Học phí đại học cho con` | `EDUCATION` | 1.200.000.000 ₫ | 2034-08-31 |
| GLB1 | B | `Quỹ dự phòng 6 tháng` | `EMERGENCY_FUND` | 180.000.000 ₫ | 2027-12-31 |

## X. Chung cho cả bốn endpoint

| # | Request | Mong đợi |
|---|---|---|
| X01 | `GET /assets`, `/liabilities`, `/cash-flow-entries`, `/goals` không tham số, as A và as B, **jar `main` `abadb96`** rồi **jar nhánh này** trên cùng database | 8 cặp body giống từng byte (so `sha256`), status `200` cả hai |
| X02 | Bốn endpoint `?page=2&size=1&foo=bar` | Giống X01 (tham số không biết bị bỏ qua) |
| X03 | Bốn endpoint `?q=thao` không header `Authorization` | `401`, `urn:rikkaus:problem:unauthorized` |
| X04 | Như X03 với token hết hạn | `401` cùng type |
| X05 | Như X03 với token ký bằng secret khác | `401` cùng type |
| X06 | Bốn endpoint `?q=` và `?q=%20%20` (rỗng, toàn khoảng trắng) | Giống X01 (reset bộ lọc) |
| X07 | Bốn endpoint `?q=<121 ký tự a>` | `400 validation-failed`, `errors=[{field:q, message:"must be at most 120 characters"}]` |
| X08 | Bốn endpoint `?q=<120 ký tự a>` | `200 []` |
| X09 | Bốn endpoint `?q=<120 ký tự "ệ">` (120 code point, 360 byte UTF-8) | `200 []` (đếm ký tự, không đếm byte) |
| X10 | Bốn endpoint `?q=vay%00mua` | `400 validation-failed`, field `q`, message `must not contain a null character` |
| X11 | Bốn endpoint `?sort=amount` | `400 validation-failed`, field `sort`, message liệt kê các khóa hợp lệ của endpoint đó |
| X12 | Bốn endpoint `?sort=name&sort=-name` (cash-flow: `effectiveDate` hai lần) | `400 validation-failed`, field `sort` |

> Sửa sau lần chạy 1: X03–X05 ban đầu ghi type `unauthenticated`, sai so với `ProblemType.UNAUTHORIZED`
> (`urn:rikkaus:problem:unauthorized`) mà contract đã có từ trước. Lỗi nằm ở scenario, không ở code.

## AS. Assets

| # | Request | Mong đợi |
|---|---|---|
| AS01 | `?q=Thảo Điền` | `[Căn hộ Thảo Điền]` |
| AS02 | `?q=thao dien` | `[Căn hộ Thảo Điền]` (bỏ dấu) |
| AS03 | `?q=THẢO ĐIỀN` | `[Căn hộ Thảo Điền]` (khác hoa thường) |
| AS04 | `?q=điền` và `?q=DIEN` | `[Căn hộ Thảo Điền]` (chuỗi con cuối tên) |
| AS05 | `?q=khoản` | `[Tài khoản Vietcombank, Tài khoản USD]` theo thứ tự mặc định (valuationDate giảm dần) |
| AS06 | `?q=hsbc` | `[Tài khoản USD]` (chỉ khớp `institution`) |
| AS07 | `?q=két sắt` và `?q=ket sat` | `[Tiền mặt dự phòng]` (chỉ khớp `notes`) |
| AS08 | `?q=%20%20Vietcombank%20%20` | `[Tài khoản Vietcombank]` (strip đầu cuối) |
| AS09 | `?q=ngân hàng sao hỏa` | `200 []` |
| AS10 | `?q=%25` (ký tự `%`) | `[Tiền gửi 12 tháng Techcombank, Chứng chỉ quỹ VN30 (phí 0,6%)]` (notes `4,8%/năm` và tên); **không** phải cả 7 |
| AS11 | `?q=_` | `200 []` (`_` là chữ, không phải wildcard) |
| AS12 | `?q=da lat` và `?q=Đà Lạt` (NFC) | `[Nhà phố Đà Lạt]`; name trả về đúng byte NFD đã lưu |
| AS13 | `?category=BANK_ACCOUNT` | `[Tài khoản Vietcombank, Tài khoản USD]` |
| AS14 | `?category=CASH&category=BANK_ACCOUNT` | AS1, AS2, AS4 theo thứ tự mặc định |
| AS15 | `?category=CASH,BANK_ACCOUNT` | Giống AS14 |
| AS16 | `?currency=USD` | `[Tài khoản USD]` |
| AS17 | `?liquidity=MEDIUM&liquidity=LOW` | AS3, AS5, AS6, AS7 theo thứ tự mặc định |
| AS18 | `?q=vietcombank&category=BANK_ACCOUNT&currency=VND&liquidity=HIGH` | `[Tài khoản Vietcombank]` |
| AS19 | `?q=thao&category=CASH` | `200 []` |
| AS20 | `?category=` | Giống không tham số |
| AS21 | `?category=GOLD` | `400 validation-failed`, `errors=[{field:category, message:"must be one of CASH, BANK_ACCOUNT, TERM_DEPOSIT, LISTED_SECURITIES, BOND, INVESTMENT_FUND, REAL_ESTATE, PRIVATE_BUSINESS_EQUITY, VEHICLE_OR_VALUABLE_ASSET, COLLECTIBLE, OTHER_ASSET"}]`; body không chứa `GOLD` |
| AS22 | `?category=bank_account` | `400`, field `category` (phân biệt hoa thường) |
| AS23 | `?currency=EUR` | `400`, field `currency`, message `must be one of VND, USD` |
| AS24 | `?liquidity=VERY_HIGH` | `400`, field `liquidity`, message `must be one of HIGH, MEDIUM, LOW` |
| AS25 | `?category=GOLD&currency=EUR&sort=price&q=<121 ký tự>` | `400`, bốn phần tử `errors`: `q`, `category`, `currency`, `sort` |
| AS26 | `?sort=-valuationDate` | Giống không tham số |
| AS27 | `?sort=valuationDate` | AS7, AS6, AS5, AS4, AS3, rồi hai bản ghi 2026-11-20 (AS1, AS2) theo thứ tự collation |
| AS28 | `?sort=name` | Bảy tên theo thứ tự collation |
| AS29 | `?sort=-name` | Đảo ngược AS28 |
| AS30 | `?q=thao dien` as B | `[Nhà phố Thảo Điền]`; không có `Căn hộ Thảo Điền` |
| AS31 | `?q=vietcombank` as B | Một phần tử, id = ASB2 (không phải AS1 dù trùng tên) |

## LB. Liabilities

| # | Request | Mong đợi |
|---|---|---|
| LB01 | Không tham số | `[Thẻ tín dụng Techcombank Visa, Vay mua nhà Vietcombank, Vay du học USD, Dư nợ ký quỹ SSI]` (null cuối) |
| LB02 | `?q=vietcombank` | `[Vay mua nhà Vietcombank]` |
| LB03 | `?q=thao dien` | `[Vay mua nhà Vietcombank]` (khớp `notes`) |
| LB04 | `?q=VAY` | `[Vay mua nhà Vietcombank, Vay du học USD]` (notes LB4 cũng có "Vay") |
| LB05 | `?q=du no ky quy` | `[Dư nợ ký quỹ SSI]` (`đ`/`ư`/`ỹ` bỏ dấu) |
| LB06 | `?category=BANK_LOAN&category=MARGIN_LOAN` | `[Vay mua nhà Vietcombank, Dư nợ ký quỹ SSI]` |
| LB07 | `?currency=USD` | `[Vay du học USD]` |
| LB08 | `?q=ssi&currency=VND&category=MARGIN_LOAN` | `[Dư nợ ký quỹ SSI]` |
| LB09 | `?sort=-nextPaymentDate` | `[Vay du học USD, Vay mua nhà Vietcombank, Thẻ tín dụng Techcombank Visa, Dư nợ ký quỹ SSI]` (null vẫn cuối) |
| LB10 | `?sort=name` / `?sort=-name` | Thứ tự collation / đảo ngược |
| LB11 | `?sort=nextPaymentDate` | Giống LB01 |
| LB12 | `?category=MORTGAGE` | `400`, field `category`, liệt kê 6 giá trị |
| LB13 | `?liquidity=HIGH` | Giống LB01 (không phải tham số của liabilities, bị bỏ qua) |
| LB14 | `?q=vietcombank` as B | Một phần tử, id = LBB1 |

## CF. Cash-flow entries

| # | Request | Mong đợi |
|---|---|---|
| CF01 | Không tham số | `[CF6, CF2, CF4, CF5, CF3, CF1]` |
| CF02 | `?q=lương` và `?q=luong thang` | `[CF1]` |
| CF03 | `?q=thảo điền` | `[CF3]` |
| CF04 | `?q=học phí` và `?q=hoc phi` | `[CF2]` |
| CF05 | `?entryType=INCOME` | `[CF6, CF4, CF1]` |
| CF06 | `?entryType=EXPENSE&category=EDUCATION` | `[CF2]` |
| CF07 | `?category=SALARY&category=DIVIDEND` | `[CF4, CF1]` |
| CF08 | `?entryType=EXPENSE&category=SALARY` | `200 []` (tổ hợp hợp lệ, không khớp; không phải lỗi) |
| CF09 | `?timing=RECURRING` | `[CF5, CF3, CF1]` |
| CF10 | `?timing=ONE_TIME` | `[CF6, CF2, CF4]` |
| CF11 | `?recurrence=MONTHLY` và `?recurrence=YEARLY` | `[CF3, CF1]` và `[CF5]` |
| CF12 | `?currency=USD` | `[CF4]` |
| CF13 | `?effectiveFrom=2026-09-01&effectiveTo=2026-09-30` | `[CF6, CF2]` |
| CF14 | `?effectiveFrom=2026-09-03&effectiveTo=2026-09-03` | `[CF2]` (bao gồm hai đầu) |
| CF15 | `?effectiveFrom=2026-06-20` | `[CF6, CF2, CF4]` |
| CF16 | `?effectiveTo=2026-03-01` | `[CF3, CF1]` |
| CF17 | `?q=hoc phi&entryType=EXPENSE&effectiveFrom=2026-09-01&effectiveTo=2026-09-30` | `[CF2]` |
| CF18 | `?q=luong&effectiveFrom=2026-09-01` | `200 []` (CF1 định kỳ nhưng effectiveDate lưu là 2026-01-05) |
| CF19 | `?effectiveFrom=2026-09-30&effectiveTo=2026-09-01` | `400`, `errors=[{field:effectiveTo, message:"must not be before effectiveFrom"}]` |
| CF20 | `?effectiveFrom=2026-02-30` | `400`, field `effectiveFrom`, `must be a calendar date in the form YYYY-MM-DD` |
| CF21 | `?effectiveTo=30/09/2026` | `400`, field `effectiveTo`, cùng message |
| CF22 | `?entryType=TRANSFER` | `400`, field `entryType`, `must be one of INCOME, EXPENSE` |
| CF23 | `?recurrence=WEEKLY` và `?timing=DAILY` | `400`, field `recurrence` / `timing` |
| CF24 | `?category=GROCERIES` | `400`, field `category`, liệt kê 16 giá trị |
| CF25 | `?sort=effectiveDate` | `[CF1, CF3, CF5, CF4, CF2, CF6]` |
| CF26 | `?sort=-effectiveDate` | Giống CF01 |
| CF27 | `?q=sao mai` → `[CF1]`; `?category=BUSINESS_INCOME` → `[CF6]` | CF6 notes null không khớp `q` nào nhưng vẫn ra theo filter enum |
| CF28 | `?q=luong` as B | `[CFB1]` |

## GL. Goals

| # | Request | Mong đợi |
|---|---|---|
| GL01 | Không tham số | `[Quỹ dự phòng 6 tháng, Mua nhà Thủ Đức, Học phí đại học cho con]` |
| GL02 | `?q=quỹ dự phòng` và `?q=quy du phong` | `[Quỹ dự phòng 6 tháng]` của A |
| GL03 | `?q=THỦ ĐỨC` và `?q=thu duc` | `[Mua nhà Thủ Đức]` |
| GL04 | `?type=EDUCATION` | `[Học phí đại học cho con]` |
| GL05 | `?type=EMERGENCY_FUND&type=HOME_PURCHASE` | `[Quỹ dự phòng 6 tháng, Mua nhà Thủ Đức]` |
| GL06 | `?q=nha&type=HOME_PURCHASE` | `[Mua nhà Thủ Đức]` |
| GL07 | `?q=nha&type=EDUCATION` | `200 []` |
| GL08 | `?type=VACATION` | `400`, field `type`, liệt kê 7 giá trị |
| GL09 | `?sort=-targetDate` | Đảo ngược GL01 |
| GL10 | `?sort=name` / `?sort=-name` | Thứ tự collation / đảo ngược |
| GL11 | `?q=quy du phong` as B | Một phần tử, id = GLB1, `targetAmount` 180000000.00 |

Tổng: 12 + 31 + 14 + 28 + 11 = **96 scenario**.
