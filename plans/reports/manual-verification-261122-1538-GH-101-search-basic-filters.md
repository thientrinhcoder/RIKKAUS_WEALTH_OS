# Manual verification — issue #101 search & basic filters

**Kết quả: 96 scenario (152 lượt kiểm tra) PASS 152/152 trên commit `ab1af04`, và lại 152/152 sau khi merge `main` `becca58`.** Lần chạy đầu trên
working tree có 12 lượt fail, đều do scenario ghi sai type 401 (chi tiết bên dưới); không lượt nào fail vì
code. `clean verify` xanh không flag trên `ab1af04`: 81 unit test, 50 integration test, spotless, drift
check `openapi.json`.

## Môi trường

| Mục | Giá trị |
|---|---|
| Ngày chạy | 22/11/2026, giờ `Asia/Saigon` |
| Database | Container `rikkaus-pg-issue-101`, `postgres:18.6-alpine3.24` (PostgreSQL 18.6, aarch64), `127.0.0.1:5401`, collation `en_US.utf8`, provider libc |
| API | Jar đóng gói từ commit, `--spring.profiles.active=local --server.port=8101`, `DB_URL=jdbc:postgresql://localhost:5401/rikkaus`, log `org.hibernate.SQL=DEBUG` và `org.hibernate.orm.jdbc.bind=TRACE` |
| Baseline | Jar đóng gói từ `main` `abadb96`, cùng database, chạy trước |
| Token | Bốn token (A, B, hết hạn, ký bằng key ngẫu nhiên) sinh lúc chạy, chỉ giữ trong file `0600` ngoài repo; report không chứa token |
| Dữ liệu | Đúng bảng dữ liệu trong `phase-01-test-scenarios.md`, tạo qua API thật; `Nhà phố Đà Lạt` gửi dạng NFD |
| Cách chấm | Script so status, `Content-Type`, mảng `errors` (field + message, đúng thứ tự) và danh sách id trả về với kết quả mong đợi. Thứ tự theo tên lấy oracle là `ORDER BY` tương ứng chạy trên cùng database |

"96 scenario, 152 lượt": nhóm X chạy trên cả bốn endpoint, và vài case có hai biến thể (`?q=lương` và
`?q=luong thang`), mỗi biến thể một lượt.

## Các lần chạy

| Lần | Code | Kết quả | Ghi chú |
|---|---|---|---|
| Smoke test | Working tree trước khi sửa binding | 8/8 body không tham số giống `main`; `q=thao dien` trả `Căn hộ Thảo Điền` | Log SQL lộ lỗi bên dưới (phát hiện 1) |
| 1 | Working tree, đã sửa binding | **140/152**, 12 fail | X03, X04, X05 × 4 endpoint: scenario mong đợi type sai (phát hiện 2) |
| 2 | Working tree, scenario đã sửa | 152/152 | |
| 3 | Commit `ab1af04` (jar từ `clean verify` không flag) | **152/152** | Giống lần 2 trừ thứ tự key trong `errors` (phát hiện 4) |
| 4 | Merge `origin/main` `becca58` (#77, #80, #56) vào nhánh, jar từ `clean verify` không flag | **152/152** | X01 so với jar `main` `becca58` (body có thêm trường instrument của #56) |

## Phát hiện

1. **Mẫu tìm kiếm bị render vào câu SQL (đã sửa trước lần 1).** Bản đầu dùng `cb.literal("%…%")`;
   Hibernate 7 render literal thành chuỗi trong câu lệnh: `like lower(translate('%thao dien%', …))`.
   Hibernate có escape, nhưng giá trị người dùng vẫn nằm trong SQL. Enum và ngày thì đã được bind
   (`category in (?,?)`, `effective_date>=?`). Sửa: `SearchText.matchesAny` trả `TextMatch` gồm predicate
   và một `ParameterExpression` mà fragment bind lên `TypedQuery`. Bằng chứng trên lần 3: 0 dòng
   `org.hibernate.SQL` chứa `thao`, `vietcombank`, `hoc phi`, `luong` hay `quy du`; 49 dòng
   `binding parameter (2:VARCHAR) <- [%…%]`, ví dụ `binding parameter (2:VARCHAR) <- [%thao dien%]`.
   Câu lệnh liabilities khi có `q`:
   `where l1_0.user_id=? and (lower(translate(l1_0.name,<FROM>,<TO>)) like lower(translate(?,<FROM>,<TO>)) escape '\' or lower(translate(l1_0.notes,<FROM>,<TO>)) like lower(translate(?,<FROM>,<TO>)) escape '\') order by l1_0.next_payment_date,l1_0.name,l1_0.id`
   (`<FROM>`/`<TO>` là hai bảng chữ hằng của `SearchText`).
2. **Scenario X03–X05 viết sai, code đúng (fail ở lần 1).** Bản viết trước code mong đợi
   `urn:rikkaus:problem:unauthenticated`; API trả `urn:rikkaus:problem:unauthorized`, đúng
   `ProblemType.UNAUTHORIZED` có từ trước và đúng guide của các slice khác. Body thật lần 1:
   `{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized"}`.
   Đã sửa scenario (có ghi chú trong phase 1), không đổi code.
3. **Seed script lần đầu không tạo được `Vay mua nhà Vietcombank`** (400 `malformed-request`) vì script
   gửi `collateralAssetId` trước khi id của `Căn hộ Thảo Điền` được nạp vào biến. Tạo lại bằng tay với
   đúng payload thì `201`. Đây là lỗi của script chuẩn bị dữ liệu, không phải scenario; mọi record còn
   lại tạo `201` ngay.
4. **Thứ tự key trong phần tử `errors` đổi giữa hai lần khởi động JVM** (`{"field","message"}` hay
   `{"message","field"}`), vì phần tử được dựng bằng `Map.of`, có thứ tự lặp ngẫu nhiên theo từng JVM.
   Mọi slice hiện có (`InputRules.error`, `OccurrenceWindow`) làm giống hệt; thứ tự key JSON không mang
   nghĩa và client đọc theo tên. Không sửa trong ticket này; nêu làm follow-up nếu muốn body ổn định
   từng byte.

## Kết quả lần 3 (commit `ab1af04`)

Nhãn `AS1`…`GLB1` là record trong bảng dữ liệu của phase 1; sau nhãn là `name` (hoặc `notes` với
cash-flow) đúng như API trả về. Body lỗi in nguyên văn, chỉ bỏ `correlationId`. X01 so `sha256` body
của jar `main` và jar nhánh trên cùng database.

| # | Request | Status | Body (nhãn record và `name`/`notes` như API trả về; lỗi bỏ `correlationId`) | Kết quả |
|---|---|---|---|---|
| X01 | `GET /assets` as A, jar `main` abadb96 so với jar nhánh | 200 | sha256 main `562086a873101a7b…`, nhánh `562086a873101a7b…`, 2729 byte | PASS |
| X01 | `GET /liabilities` as A, jar `main` abadb96 so với jar nhánh | 200 | sha256 main `89d52c9e22279474…`, nhánh `89d52c9e22279474…`, 1865 byte | PASS |
| X01 | `GET /cash-flow-entries` as A, jar `main` abadb96 so với jar nhánh | 200 | sha256 main `22664d0ceb469fd9…`, nhánh `22664d0ceb469fd9…`, 2295 byte | PASS |
| X01 | `GET /goals` as A, jar `main` abadb96 so với jar nhánh | 200 | sha256 main `88a9051f9352c124…`, nhánh `88a9051f9352c124…`, 1077 byte | PASS |
| X01 | `GET /assets` as B, jar `main` abadb96 so với jar nhánh | 200 | sha256 main `27e400315203ccde…`, nhánh `27e400315203ccde…`, 753 byte | PASS |
| X01 | `GET /liabilities` as B, jar `main` abadb96 so với jar nhánh | 200 | sha256 main `96171e8935fa438b…`, nhánh `96171e8935fa438b…`, 443 byte | PASS |
| X01 | `GET /cash-flow-entries` as B, jar `main` abadb96 so với jar nhánh | 200 | sha256 main `6e389ecb78bd8550…`, nhánh `6e389ecb78bd8550…`, 369 byte | PASS |
| X01 | `GET /goals` as B, jar `main` abadb96 so với jar nhánh | 200 | sha256 main `54300871e938b197…`, nhánh `54300871e938b197…`, 361 byte | PASS |
| X02 | `GET /assets?page=2&size=1&foo=bar` | 200 | [AS2 `Tiền mặt dự phòng`, AS1 `Tài khoản Vietcombank`, AS3 `Tiền gửi 12 tháng Techcombank`, AS4 `Tài khoản USD`, AS5 `Căn hộ Thảo Điền`, AS6 `Chứng chỉ quỹ VN30 (phí 0,6%)`, AS7 `Nhà phố Đà Lạt`] | PASS — body giống từng byte với không tham số |
| X02 | `GET /liabilities?page=2&size=1&foo=bar` | 200 | [LB2 `Thẻ tín dụng Techcombank Visa`, LB1 `Vay mua nhà Vietcombank`, LB4 `Vay du học USD`, LB3 `Dư nợ ký quỹ SSI`] | PASS — body giống từng byte với không tham số |
| X02 | `GET /cash-flow-entries?page=2&size=1&foo=bar` | 200 | [CF6 `None`, CF2 `Học phí học kỳ 1 trường Quốc tế Á Châu`, CF4 `Cổ tức quỹ ETF Mỹ`, CF5 `Bảo hiểm nhân thọ`, CF3 `Tiền thuê căn hộ Thảo Điền`, CF1 `Lương tháng công ty Sao Mai`] | PASS — body giống từng byte với không tham số |
| X02 | `GET /goals?page=2&size=1&foo=bar` | 200 | [GL1 `Quỹ dự phòng 6 tháng`, GL2 `Mua nhà Thủ Đức`, GL3 `Học phí đại học cho con`] | PASS — body giống từng byte với không tham số |
| X03 | `GET /assets?q=thao (không header)` | 401 | `{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized"}` | PASS |
| X04 | `GET /assets?q=thao (token hết hạn)` | 401 | `{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized"}` | PASS |
| X05 | `GET /assets?q=thao (token sai chữ ký)` | 401 | `{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized"}` | PASS |
| X03 | `GET /liabilities?q=thao (không header)` | 401 | `{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized"}` | PASS |
| X04 | `GET /liabilities?q=thao (token hết hạn)` | 401 | `{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized"}` | PASS |
| X05 | `GET /liabilities?q=thao (token sai chữ ký)` | 401 | `{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized"}` | PASS |
| X03 | `GET /cash-flow-entries?q=thao (không header)` | 401 | `{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized"}` | PASS |
| X04 | `GET /cash-flow-entries?q=thao (token hết hạn)` | 401 | `{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized"}` | PASS |
| X05 | `GET /cash-flow-entries?q=thao (token sai chữ ký)` | 401 | `{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized"}` | PASS |
| X03 | `GET /goals?q=thao (không header)` | 401 | `{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized"}` | PASS |
| X04 | `GET /goals?q=thao (token hết hạn)` | 401 | `{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized"}` | PASS |
| X05 | `GET /goals?q=thao (token sai chữ ký)` | 401 | `{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized"}` | PASS |
| X06 | `GET /assets?q=` | 200 | [AS2 `Tiền mặt dự phòng`, AS1 `Tài khoản Vietcombank`, AS3 `Tiền gửi 12 tháng Techcombank`, AS4 `Tài khoản USD`, AS5 `Căn hộ Thảo Điền`, AS6 `Chứng chỉ quỹ VN30 (phí 0,6%)`, AS7 `Nhà phố Đà Lạt`] | PASS — body giống từng byte với không tham số |
| X06 | `GET /assets?q=++` | 200 | [AS2 `Tiền mặt dự phòng`, AS1 `Tài khoản Vietcombank`, AS3 `Tiền gửi 12 tháng Techcombank`, AS4 `Tài khoản USD`, AS5 `Căn hộ Thảo Điền`, AS6 `Chứng chỉ quỹ VN30 (phí 0,6%)`, AS7 `Nhà phố Đà Lạt`] | PASS — body giống từng byte với không tham số |
| X06 | `GET /liabilities?q=` | 200 | [LB2 `Thẻ tín dụng Techcombank Visa`, LB1 `Vay mua nhà Vietcombank`, LB4 `Vay du học USD`, LB3 `Dư nợ ký quỹ SSI`] | PASS — body giống từng byte với không tham số |
| X06 | `GET /liabilities?q=++` | 200 | [LB2 `Thẻ tín dụng Techcombank Visa`, LB1 `Vay mua nhà Vietcombank`, LB4 `Vay du học USD`, LB3 `Dư nợ ký quỹ SSI`] | PASS — body giống từng byte với không tham số |
| X06 | `GET /cash-flow-entries?q=` | 200 | [CF6 `None`, CF2 `Học phí học kỳ 1 trường Quốc tế Á Châu`, CF4 `Cổ tức quỹ ETF Mỹ`, CF5 `Bảo hiểm nhân thọ`, CF3 `Tiền thuê căn hộ Thảo Điền`, CF1 `Lương tháng công ty Sao Mai`] | PASS — body giống từng byte với không tham số |
| X06 | `GET /cash-flow-entries?q=++` | 200 | [CF6 `None`, CF2 `Học phí học kỳ 1 trường Quốc tế Á Châu`, CF4 `Cổ tức quỹ ETF Mỹ`, CF5 `Bảo hiểm nhân thọ`, CF3 `Tiền thuê căn hộ Thảo Điền`, CF1 `Lương tháng công ty Sao Mai`] | PASS — body giống từng byte với không tham số |
| X06 | `GET /goals?q=` | 200 | [GL1 `Quỹ dự phòng 6 tháng`, GL2 `Mua nhà Thủ Đức`, GL3 `Học phí đại học cho con`] | PASS — body giống từng byte với không tham số |
| X06 | `GET /goals?q=++` | 200 | [GL1 `Quỹ dự phòng 6 tháng`, GL2 `Mua nhà Thủ Đức`, GL3 `Học phí đại học cho con`] | PASS — body giống từng byte với không tham số |
| X07 | `GET /assets?q=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa…(123 ký tự)` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"q","message":"must be at most 120 characters"}]}` | PASS |
| X07 | `GET /liabilities?q=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa…(123 ký tự)` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/liabilities","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"q","message":"must be at most 120 characters"}]}` | PASS |
| X07 | `GET /cash-flow-entries?q=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa…(123 ký tự)` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/cash-flow-entries","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"q","message":"must be at most 120 characters"}]}` | PASS |
| X07 | `GET /goals?q=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa…(123 ký tự)` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/goals","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"q","message":"must be at most 120 characters"}]}` | PASS |
| X08 | `GET /assets?q=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa…(122 ký tự)` | 200 | [] | PASS |
| X08 | `GET /liabilities?q=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa…(122 ký tự)` | 200 | [] | PASS |
| X08 | `GET /cash-flow-entries?q=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa…(122 ký tự)` | 200 | [] | PASS |
| X08 | `GET /goals?q=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa…(122 ký tự)` | 200 | [] | PASS |
| X09 | `GET /assets?q=ệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệ…(122 ký tự)` | 200 | [] | PASS |
| X09 | `GET /liabilities?q=ệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệ…(122 ký tự)` | 200 | [] | PASS |
| X09 | `GET /cash-flow-entries?q=ệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệ…(122 ký tự)` | 200 | [] | PASS |
| X09 | `GET /goals?q=ệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệ…(122 ký tự)` | 200 | [] | PASS |
| X10 | `GET /assets?q=vay%00mua` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"q","message":"must not contain a null character"}]}` | PASS |
| X10 | `GET /liabilities?q=vay%00mua` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/liabilities","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"q","message":"must not contain a null character"}]}` | PASS |
| X10 | `GET /cash-flow-entries?q=vay%00mua` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/cash-flow-entries","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"q","message":"must not contain a null character"}]}` | PASS |
| X10 | `GET /goals?q=vay%00mua` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/goals","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"q","message":"must not contain a null character"}]}` | PASS |
| X11 | `GET /assets?sort=amount` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"sort","message":"must be one of -valuationDate, valuationDate, name, -name"}]}` | PASS |
| X11 | `GET /liabilities?sort=amount` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/liabilities","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"sort","message":"must be one of nextPaymentDate, -nextPaymentDate, name, -name"}]}` | PASS |
| X11 | `GET /cash-flow-entries?sort=amount` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/cash-flow-entries","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"sort","message":"must be one of -effectiveDate, effectiveDate"}]}` | PASS |
| X11 | `GET /goals?sort=amount` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/goals","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"sort","message":"must be one of targetDate, -targetDate, name, -name"}]}` | PASS |
| X12 | `GET /assets?sort=name&sort=-name` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"sort","message":"must be one of -valuationDate, valuationDate, name, -name"}]}` | PASS |
| X12 | `GET /liabilities?sort=name&sort=-name` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/liabilities","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"sort","message":"must be one of nextPaymentDate, -nextPaymentDate, name, -name"}]}` | PASS |
| X12 | `GET /cash-flow-entries?sort=effectiveDate&sort=effectiveDate` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/cash-flow-entries","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"sort","message":"must be one of -effectiveDate, effectiveDate"}]}` | PASS |
| X12 | `GET /goals?sort=name&sort=-name` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/goals","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"sort","message":"must be one of targetDate, -targetDate, name, -name"}]}` | PASS |
| AS01 | `GET /assets?q=Thảo+Điền` | 200 | [AS5 `Căn hộ Thảo Điền`] | PASS |
| AS02 | `GET /assets?q=thao+dien` | 200 | [AS5 `Căn hộ Thảo Điền`] | PASS |
| AS03 | `GET /assets?q=THẢO+ĐIỀN` | 200 | [AS5 `Căn hộ Thảo Điền`] | PASS |
| AS04 | `GET /assets?q=điền` | 200 | [AS5 `Căn hộ Thảo Điền`] | PASS |
| AS04 | `GET /assets?q=DIEN` | 200 | [AS5 `Căn hộ Thảo Điền`] | PASS |
| AS05 | `GET /assets?q=khoản` | 200 | [AS1 `Tài khoản Vietcombank`, AS4 `Tài khoản USD`] | PASS |
| AS06 | `GET /assets?q=hsbc` | 200 | [AS4 `Tài khoản USD`] | PASS |
| AS07 | `GET /assets?q=két+sắt` | 200 | [AS2 `Tiền mặt dự phòng`] | PASS |
| AS07 | `GET /assets?q=ket+sat` | 200 | [AS2 `Tiền mặt dự phòng`] | PASS |
| AS08 | `GET /assets?q=++Vietcombank++` | 200 | [AS1 `Tài khoản Vietcombank`] | PASS |
| AS09 | `GET /assets?q=ngân+hàng+sao+hỏa` | 200 | [] | PASS |
| AS10 | `GET /assets?q=%` | 200 | [AS3 `Tiền gửi 12 tháng Techcombank`, AS6 `Chứng chỉ quỹ VN30 (phí 0,6%)`] | PASS |
| AS11 | `GET /assets?q=_` | 200 | [] | PASS |
| AS12 | `GET /assets?q=da+lat` | 200 | [AS7 `Nhà phố Đà Lạt`] | PASS |
| AS12 | `GET /assets?q=Đà+Lạt` | 200 | [AS7 `Nhà phố Đà Lạt`] | PASS |
| AS13 | `GET /assets?category=BANK_ACCOUNT` | 200 | [AS1 `Tài khoản Vietcombank`, AS4 `Tài khoản USD`] | PASS |
| AS14 | `GET /assets?category=CASH&category=BANK_ACCOUNT` | 200 | [AS2 `Tiền mặt dự phòng`, AS1 `Tài khoản Vietcombank`, AS4 `Tài khoản USD`] | PASS |
| AS15 | `GET /assets?category=CASH,BANK_ACCOUNT` | 200 | [AS2 `Tiền mặt dự phòng`, AS1 `Tài khoản Vietcombank`, AS4 `Tài khoản USD`] | PASS |
| AS16 | `GET /assets?currency=USD` | 200 | [AS4 `Tài khoản USD`] | PASS |
| AS17 | `GET /assets?liquidity=MEDIUM&liquidity=LOW` | 200 | [AS3 `Tiền gửi 12 tháng Techcombank`, AS5 `Căn hộ Thảo Điền`, AS6 `Chứng chỉ quỹ VN30 (phí 0,6%)`, AS7 `Nhà phố Đà Lạt`] | PASS |
| AS18 | `GET /assets?q=vietcombank&category=BANK_ACCOUNT&currency=VND&liquidity=HIGH` | 200 | [AS1 `Tài khoản Vietcombank`] | PASS |
| AS19 | `GET /assets?q=thao&category=CASH` | 200 | [] | PASS |
| AS20 | `GET /assets?category=` | 200 | [AS2 `Tiền mặt dự phòng`, AS1 `Tài khoản Vietcombank`, AS3 `Tiền gửi 12 tháng Techcombank`, AS4 `Tài khoản USD`, AS5 `Căn hộ Thảo Điền`, AS6 `Chứng chỉ quỹ VN30 (phí 0,6%)`, AS7 `Nhà phố Đà Lạt`] | PASS — body giống từng byte với không tham số |
| AS21 | `GET /assets?category=GOLD` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"category","message":"must be one of CASH, BANK_ACCOUNT, TERM_DEPOSIT, LISTED_SECURITIES, BOND, INVESTMENT_FUND, REAL_ESTATE, PRIVATE_BUSINESS_EQUITY, VEHICLE_OR_VALUABLE_ASSET, COLLECTIBLE, OTHER_ASSET"}]}` | PASS |
| AS22 | `GET /assets?category=bank_account` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"category","message":"must be one of CASH, BANK_ACCOUNT, TERM_DEPOSIT, LISTED_SECURITIES, BOND, INVESTMENT_FUND, REAL_ESTATE, PRIVATE_BUSINESS_EQUITY, VEHICLE_OR_VALUABLE_ASSET, COLLECTIBLE, OTHER_ASSET"}]}` | PASS |
| AS23 | `GET /assets?currency=EUR` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"currency","message":"must be one of VND, USD"}]}` | PASS |
| AS24 | `GET /assets?liquidity=VERY_HIGH` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"liquidity","message":"must be one of HIGH, MEDIUM, LOW"}]}` | PASS |
| AS25 | `GET /assets?category=GOLD&currency=EUR&sort=price&q=aaaaaaaaaaaaaaaaaaaa…(161 ký tự)` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"q","message":"must be at most 120 characters"},{"field":"category","message":"must be one of CASH, BANK_ACCOUNT, TERM_DEPOSIT, LISTED_SECURITIES, BOND, INVESTMENT_FUND, REAL_ESTATE, PRIVATE_BUSINESS_EQUITY, VEHICLE_OR_VALUABLE_ASSET, COLLECTIBLE, OTHER_ASSET"},{"field":"currency","message":"must be one of VND, USD"},{"field":"sort","message":"must be one of -valuationDate, valuationDate, name, -name"}]}` | PASS |
| AS26 | `GET /assets?sort=-valuationDate` | 200 | [AS2 `Tiền mặt dự phòng`, AS1 `Tài khoản Vietcombank`, AS3 `Tiền gửi 12 tháng Techcombank`, AS4 `Tài khoản USD`, AS5 `Căn hộ Thảo Điền`, AS6 `Chứng chỉ quỹ VN30 (phí 0,6%)`, AS7 `Nhà phố Đà Lạt`] | PASS — body giống từng byte với không tham số |
| AS27 | `GET /assets?sort=valuationDate` | 200 | [AS7 `Nhà phố Đà Lạt`, AS6 `Chứng chỉ quỹ VN30 (phí 0,6%)`, AS5 `Căn hộ Thảo Điền`, AS4 `Tài khoản USD`, AS3 `Tiền gửi 12 tháng Techcombank`, AS2 `Tiền mặt dự phòng`, AS1 `Tài khoản Vietcombank`] | PASS |
| AS28 | `GET /assets?sort=name` | 200 | [AS6 `Chứng chỉ quỹ VN30 (phí 0,6%)`, AS5 `Căn hộ Thảo Điền`, AS7 `Nhà phố Đà Lạt`, AS3 `Tiền gửi 12 tháng Techcombank`, AS2 `Tiền mặt dự phòng`, AS4 `Tài khoản USD`, AS1 `Tài khoản Vietcombank`] | PASS |
| AS29 | `GET /assets?sort=-name` | 200 | [AS1 `Tài khoản Vietcombank`, AS4 `Tài khoản USD`, AS2 `Tiền mặt dự phòng`, AS3 `Tiền gửi 12 tháng Techcombank`, AS7 `Nhà phố Đà Lạt`, AS5 `Căn hộ Thảo Điền`, AS6 `Chứng chỉ quỹ VN30 (phí 0,6%)`] | PASS |
| AS30 | `GET /assets?q=thao+dien as B` | 200 | [ASB1 `Nhà phố Thảo Điền`] | PASS |
| AS31 | `GET /assets?q=vietcombank as B` | 200 | [ASB2 `Tài khoản Vietcombank`] | PASS |
| LB01 | `GET /liabilities` | 200 | [LB2 `Thẻ tín dụng Techcombank Visa`, LB1 `Vay mua nhà Vietcombank`, LB4 `Vay du học USD`, LB3 `Dư nợ ký quỹ SSI`] | PASS |
| LB02 | `GET /liabilities?q=vietcombank` | 200 | [LB1 `Vay mua nhà Vietcombank`] | PASS |
| LB03 | `GET /liabilities?q=thao+dien` | 200 | [LB1 `Vay mua nhà Vietcombank`] | PASS |
| LB04 | `GET /liabilities?q=VAY` | 200 | [LB1 `Vay mua nhà Vietcombank`, LB4 `Vay du học USD`] | PASS |
| LB05 | `GET /liabilities?q=du+no+ky+quy` | 200 | [LB3 `Dư nợ ký quỹ SSI`] | PASS |
| LB06 | `GET /liabilities?category=BANK_LOAN&category=MARGIN_LOAN` | 200 | [LB1 `Vay mua nhà Vietcombank`, LB3 `Dư nợ ký quỹ SSI`] | PASS |
| LB07 | `GET /liabilities?currency=USD` | 200 | [LB4 `Vay du học USD`] | PASS |
| LB08 | `GET /liabilities?q=ssi&currency=VND&category=MARGIN_LOAN` | 200 | [LB3 `Dư nợ ký quỹ SSI`] | PASS |
| LB09 | `GET /liabilities?sort=-nextPaymentDate` | 200 | [LB4 `Vay du học USD`, LB1 `Vay mua nhà Vietcombank`, LB2 `Thẻ tín dụng Techcombank Visa`, LB3 `Dư nợ ký quỹ SSI`] | PASS |
| LB10 | `GET /liabilities?sort=name` | 200 | [LB3 `Dư nợ ký quỹ SSI`, LB2 `Thẻ tín dụng Techcombank Visa`, LB4 `Vay du học USD`, LB1 `Vay mua nhà Vietcombank`] | PASS |
| LB10 | `GET /liabilities?sort=-name` | 200 | [LB1 `Vay mua nhà Vietcombank`, LB4 `Vay du học USD`, LB2 `Thẻ tín dụng Techcombank Visa`, LB3 `Dư nợ ký quỹ SSI`] | PASS |
| LB11 | `GET /liabilities?sort=nextPaymentDate` | 200 | [LB2 `Thẻ tín dụng Techcombank Visa`, LB1 `Vay mua nhà Vietcombank`, LB4 `Vay du học USD`, LB3 `Dư nợ ký quỹ SSI`] | PASS — body giống từng byte với không tham số |
| LB12 | `GET /liabilities?category=MORTGAGE` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/liabilities","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"category","message":"must be one of BANK_LOAN, PERSONAL_LOAN, MARGIN_LOAN, CREDIT_CARD_BALANCE, GUARANTEE_OBLIGATION, OTHER_LIABILITY"}]}` | PASS |
| LB13 | `GET /liabilities?liquidity=HIGH` | 200 | [LB2 `Thẻ tín dụng Techcombank Visa`, LB1 `Vay mua nhà Vietcombank`, LB4 `Vay du học USD`, LB3 `Dư nợ ký quỹ SSI`] | PASS — body giống từng byte với không tham số |
| LB14 | `GET /liabilities?q=vietcombank as B` | 200 | [LBB1 `Vay mua nhà Vietcombank`] | PASS |
| CF01 | `GET /cash-flow-entries` | 200 | [CF6 `None`, CF2 `Học phí học kỳ 1 trường Quốc tế Á Châu`, CF4 `Cổ tức quỹ ETF Mỹ`, CF5 `Bảo hiểm nhân thọ`, CF3 `Tiền thuê căn hộ Thảo Điền`, CF1 `Lương tháng công ty Sao Mai`] | PASS |
| CF02 | `GET /cash-flow-entries?q=lương` | 200 | [CF1 `Lương tháng công ty Sao Mai`] | PASS |
| CF02 | `GET /cash-flow-entries?q=luong+thang` | 200 | [CF1 `Lương tháng công ty Sao Mai`] | PASS |
| CF03 | `GET /cash-flow-entries?q=thảo+điền` | 200 | [CF3 `Tiền thuê căn hộ Thảo Điền`] | PASS |
| CF04 | `GET /cash-flow-entries?q=học+phí` | 200 | [CF2 `Học phí học kỳ 1 trường Quốc tế Á Châu`] | PASS |
| CF04 | `GET /cash-flow-entries?q=hoc+phi` | 200 | [CF2 `Học phí học kỳ 1 trường Quốc tế Á Châu`] | PASS |
| CF05 | `GET /cash-flow-entries?entryType=INCOME` | 200 | [CF6 `None`, CF4 `Cổ tức quỹ ETF Mỹ`, CF1 `Lương tháng công ty Sao Mai`] | PASS |
| CF06 | `GET /cash-flow-entries?entryType=EXPENSE&category=EDUCATION` | 200 | [CF2 `Học phí học kỳ 1 trường Quốc tế Á Châu`] | PASS |
| CF07 | `GET /cash-flow-entries?category=SALARY&category=DIVIDEND` | 200 | [CF4 `Cổ tức quỹ ETF Mỹ`, CF1 `Lương tháng công ty Sao Mai`] | PASS |
| CF08 | `GET /cash-flow-entries?entryType=EXPENSE&category=SALARY` | 200 | [] | PASS |
| CF09 | `GET /cash-flow-entries?timing=RECURRING` | 200 | [CF5 `Bảo hiểm nhân thọ`, CF3 `Tiền thuê căn hộ Thảo Điền`, CF1 `Lương tháng công ty Sao Mai`] | PASS |
| CF10 | `GET /cash-flow-entries?timing=ONE_TIME` | 200 | [CF6 `None`, CF2 `Học phí học kỳ 1 trường Quốc tế Á Châu`, CF4 `Cổ tức quỹ ETF Mỹ`] | PASS |
| CF11 | `GET /cash-flow-entries?recurrence=MONTHLY` | 200 | [CF3 `Tiền thuê căn hộ Thảo Điền`, CF1 `Lương tháng công ty Sao Mai`] | PASS |
| CF11 | `GET /cash-flow-entries?recurrence=YEARLY` | 200 | [CF5 `Bảo hiểm nhân thọ`] | PASS |
| CF12 | `GET /cash-flow-entries?currency=USD` | 200 | [CF4 `Cổ tức quỹ ETF Mỹ`] | PASS |
| CF13 | `GET /cash-flow-entries?effectiveFrom=2026-09-01&effectiveTo=2026-09-30` | 200 | [CF6 `None`, CF2 `Học phí học kỳ 1 trường Quốc tế Á Châu`] | PASS |
| CF14 | `GET /cash-flow-entries?effectiveFrom=2026-09-03&effectiveTo=2026-09-03` | 200 | [CF2 `Học phí học kỳ 1 trường Quốc tế Á Châu`] | PASS |
| CF15 | `GET /cash-flow-entries?effectiveFrom=2026-06-20` | 200 | [CF6 `None`, CF2 `Học phí học kỳ 1 trường Quốc tế Á Châu`, CF4 `Cổ tức quỹ ETF Mỹ`] | PASS |
| CF16 | `GET /cash-flow-entries?effectiveTo=2026-03-01` | 200 | [CF3 `Tiền thuê căn hộ Thảo Điền`, CF1 `Lương tháng công ty Sao Mai`] | PASS |
| CF17 | `GET /cash-flow-entries?q=hoc+phi&entryType=EXPENSE&effectiveFrom=2026-09-01&effectiveTo=2026-09-30` | 200 | [CF2 `Học phí học kỳ 1 trường Quốc tế Á Châu`] | PASS |
| CF18 | `GET /cash-flow-entries?q=luong&effectiveFrom=2026-09-01` | 200 | [] | PASS |
| CF19 | `GET /cash-flow-entries?effectiveFrom=2026-09-30&effectiveTo=2026-09-01` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/cash-flow-entries","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"effectiveTo","message":"must not be before effectiveFrom"}]}` | PASS |
| CF20 | `GET /cash-flow-entries?effectiveFrom=2026-02-30` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/cash-flow-entries","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"effectiveFrom","message":"must be a calendar date in the form YYYY-MM-DD"}]}` | PASS |
| CF21 | `GET /cash-flow-entries?effectiveTo=30/09/2026` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/cash-flow-entries","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"effectiveTo","message":"must be a calendar date in the form YYYY-MM-DD"}]}` | PASS |
| CF22 | `GET /cash-flow-entries?entryType=TRANSFER` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/cash-flow-entries","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"entryType","message":"must be one of INCOME, EXPENSE"}]}` | PASS |
| CF23 | `GET /cash-flow-entries?recurrence=WEEKLY` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/cash-flow-entries","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"recurrence","message":"must be one of MONTHLY, QUARTERLY, YEARLY"}]}` | PASS |
| CF23 | `GET /cash-flow-entries?timing=DAILY` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/cash-flow-entries","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"timing","message":"must be one of ONE_TIME, RECURRING"}]}` | PASS |
| CF24 | `GET /cash-flow-entries?category=GROCERIES` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/cash-flow-entries","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"category","message":"must be one of SALARY, BUSINESS_INCOME, RENTAL_INCOME, DIVIDEND, DEPOSIT_INTEREST, BOND_OR_FUND_INCOME, OTHER_INCOME, LIVING_COSTS, HOUSING, EDUCATION, HEALTHCARE, INSURANCE, TAX_AND_FEES, DEBT_PRINCIPAL_PAYMENT, INTEREST_PAYMENT, OTHER_EXPENSE"}]}` | PASS |
| CF25 | `GET /cash-flow-entries?sort=effectiveDate` | 200 | [CF1 `Lương tháng công ty Sao Mai`, CF3 `Tiền thuê căn hộ Thảo Điền`, CF5 `Bảo hiểm nhân thọ`, CF4 `Cổ tức quỹ ETF Mỹ`, CF2 `Học phí học kỳ 1 trường Quốc tế Á Châu`, CF6 `None`] | PASS |
| CF26 | `GET /cash-flow-entries?sort=-effectiveDate` | 200 | [CF6 `None`, CF2 `Học phí học kỳ 1 trường Quốc tế Á Châu`, CF4 `Cổ tức quỹ ETF Mỹ`, CF5 `Bảo hiểm nhân thọ`, CF3 `Tiền thuê căn hộ Thảo Điền`, CF1 `Lương tháng công ty Sao Mai`] | PASS — body giống từng byte với không tham số |
| CF27 | `GET /cash-flow-entries?q=sao+mai` | 200 | [CF1 `Lương tháng công ty Sao Mai`] | PASS |
| CF27 | `GET /cash-flow-entries?category=BUSINESS_INCOME` | 200 | [CF6 `None`] | PASS |
| CF28 | `GET /cash-flow-entries?q=luong as B` | 200 | [CFB1 `Lương tháng`] | PASS |
| GL01 | `GET /goals` | 200 | [GL1 `Quỹ dự phòng 6 tháng`, GL2 `Mua nhà Thủ Đức`, GL3 `Học phí đại học cho con`] | PASS |
| GL02 | `GET /goals?q=quỹ+dự+phòng` | 200 | [GL1 `Quỹ dự phòng 6 tháng`] | PASS |
| GL02 | `GET /goals?q=quy+du+phong` | 200 | [GL1 `Quỹ dự phòng 6 tháng`] | PASS |
| GL03 | `GET /goals?q=THỦ+ĐỨC` | 200 | [GL2 `Mua nhà Thủ Đức`] | PASS |
| GL03 | `GET /goals?q=thu+duc` | 200 | [GL2 `Mua nhà Thủ Đức`] | PASS |
| GL04 | `GET /goals?type=EDUCATION` | 200 | [GL3 `Học phí đại học cho con`] | PASS |
| GL05 | `GET /goals?type=EMERGENCY_FUND&type=HOME_PURCHASE` | 200 | [GL1 `Quỹ dự phòng 6 tháng`, GL2 `Mua nhà Thủ Đức`] | PASS |
| GL06 | `GET /goals?q=nha&type=HOME_PURCHASE` | 200 | [GL2 `Mua nhà Thủ Đức`] | PASS |
| GL07 | `GET /goals?q=nha&type=EDUCATION` | 200 | [] | PASS |
| GL08 | `GET /goals?type=VACATION` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/goals","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"field":"type","message":"must be one of EMERGENCY_FUND, HOME_PURCHASE, EDUCATION, RETIREMENT, NET_WORTH_GROWTH, DEBT_REDUCTION, OTHER_GOAL"}]}` | PASS |
| GL09 | `GET /goals?sort=-targetDate` | 200 | [GL3 `Học phí đại học cho con`, GL2 `Mua nhà Thủ Đức`, GL1 `Quỹ dự phòng 6 tháng`] | PASS |
| GL10 | `GET /goals?sort=name` | 200 | [GL3 `Học phí đại học cho con`, GL2 `Mua nhà Thủ Đức`, GL1 `Quỹ dự phòng 6 tháng`] | PASS |
| GL10 | `GET /goals?sort=-name` | 200 | [GL1 `Quỹ dự phòng 6 tháng`, GL2 `Mua nhà Thủ Đức`, GL3 `Học phí đại học cho con`] | PASS |
| GL11 | `GET /goals?q=quy+du+phong as B` | 200 | [GLB1 `Quỹ dự phòng 6 tháng`] | PASS |

## Lần 4 — sau khi merge `main` `becca58`

`main` lúc merge đã có #77 (`cashflow/reporting`), #80 (`cashflow/forecast`) và #56 (trường instrument
của tài sản, `V14__asset_instrument_details.sql`). Conflict ở `AssetController` và `AssetService` chỉ là
import, ở `.gitignore` và `docs/api-contract-conventions.md` là hai bên cùng thêm dòng; giữ cả hai. Sau
merge không còn caller nào của `CashFlowEntryRepository.findByUserIdOrderByEffectiveDateDescCreatedAtDesc`
nên method này bị xóa như ba method list cũ của các slice kia.

Database mới (cùng container, cổng, dữ liệu như trên; Flyway lên `v14`). Baseline X01 lấy từ jar đóng gói
ở `becca58` chạy trước trên cùng database. Jar nhánh đóng gói từ working tree đã merge bằng
`clean verify -Dopenapi.update=true` rồi `clean verify` không flag (81 unit, 50 IT, xanh);
regenerate không đổi `openapi.json` so với bản git tự merge. Log: 0 dòng `org.hibernate.SQL` chứa chữ
được tìm, 0 dòng `ERROR`. Seed script lại không tạo được `Vay mua nhà Vietcombank` ở lần gửi đầu (như
phát hiện 3); tạo lại bằng tay `201`.

| # | Request | Status | Body (nhãn record và `name`/`notes` như API trả về; lỗi bỏ `correlationId`) | Kết quả |
| # | Request | Status | Body (nhãn record và `name`/`notes` như API trả về; lỗi bỏ `correlationId`) | Kết quả |
|---|---|---|---|---|
| X01 | `GET /assets` as A, jar `main` becca58 so với jar đã merge | 200 | sha256 main `6e30c0ad15e40dc0…`, nhánh `6e30c0ad15e40dc0…`, 3289 byte | PASS |
| X01 | `GET /liabilities` as A, jar `main` becca58 so với jar đã merge | 200 | sha256 main `9c0fb8296bbc6657…`, nhánh `9c0fb8296bbc6657…`, 1865 byte | PASS |
| X01 | `GET /cash-flow-entries` as A, jar `main` becca58 so với jar đã merge | 200 | sha256 main `3b650d78711053ee…`, nhánh `3b650d78711053ee…`, 2295 byte | PASS |
| X01 | `GET /goals` as A, jar `main` becca58 so với jar đã merge | 200 | sha256 main `1342416fe8b2c7dd…`, nhánh `1342416fe8b2c7dd…`, 1077 byte | PASS |
| X01 | `GET /assets` as B, jar `main` becca58 so với jar đã merge | 200 | sha256 main `629e7bdaa2b246e2…`, nhánh `629e7bdaa2b246e2…`, 913 byte | PASS |
| X01 | `GET /liabilities` as B, jar `main` becca58 so với jar đã merge | 200 | sha256 main `a8c091a1be7202fe…`, nhánh `a8c091a1be7202fe…`, 443 byte | PASS |
| X01 | `GET /cash-flow-entries` as B, jar `main` becca58 so với jar đã merge | 200 | sha256 main `fb178667a9902086…`, nhánh `fb178667a9902086…`, 369 byte | PASS |
| X01 | `GET /goals` as B, jar `main` becca58 so với jar đã merge | 200 | sha256 main `bec5a25b0a62257f…`, nhánh `bec5a25b0a62257f…`, 361 byte | PASS |
| X02 | `GET /assets?page=2&size=1&foo=bar` | 200 | [AS2 `Tiền mặt dự phòng`, AS1 `Tài khoản Vietcombank`, AS3 `Tiền gửi 12 tháng Techcombank`, AS4 `Tài khoản USD`, AS5 `Căn hộ Thảo Điền`, AS6 `Chứng chỉ quỹ VN30 (phí 0,6%)`, AS7 `Nhà phố Đà Lạt`] | PASS — body giống từng byte với không tham số |
| X02 | `GET /liabilities?page=2&size=1&foo=bar` | 200 | [LB2 `Thẻ tín dụng Techcombank Visa`, LB1 `Vay mua nhà Vietcombank`, LB4 `Vay du học USD`, LB3 `Dư nợ ký quỹ SSI`] | PASS — body giống từng byte với không tham số |
| X02 | `GET /cash-flow-entries?page=2&size=1&foo=bar` | 200 | [CF6 `None`, CF2 `Học phí học kỳ 1 trường Quốc tế Á Châu`, CF4 `Cổ tức quỹ ETF Mỹ`, CF5 `Bảo hiểm nhân thọ`, CF3 `Tiền thuê căn hộ Thảo Điền`, CF1 `Lương tháng công ty Sao Mai`] | PASS — body giống từng byte với không tham số |
| X02 | `GET /goals?page=2&size=1&foo=bar` | 200 | [GL1 `Quỹ dự phòng 6 tháng`, GL2 `Mua nhà Thủ Đức`, GL3 `Học phí đại học cho con`] | PASS — body giống từng byte với không tham số |
| X03 | `GET /assets?q=thao (không header)` | 401 | `{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized"}` | PASS |
| X04 | `GET /assets?q=thao (token hết hạn)` | 401 | `{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized"}` | PASS |
| X05 | `GET /assets?q=thao (token sai chữ ký)` | 401 | `{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized"}` | PASS |
| X03 | `GET /liabilities?q=thao (không header)` | 401 | `{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized"}` | PASS |
| X04 | `GET /liabilities?q=thao (token hết hạn)` | 401 | `{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized"}` | PASS |
| X05 | `GET /liabilities?q=thao (token sai chữ ký)` | 401 | `{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized"}` | PASS |
| X03 | `GET /cash-flow-entries?q=thao (không header)` | 401 | `{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized"}` | PASS |
| X04 | `GET /cash-flow-entries?q=thao (token hết hạn)` | 401 | `{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized"}` | PASS |
| X05 | `GET /cash-flow-entries?q=thao (token sai chữ ký)` | 401 | `{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized"}` | PASS |
| X03 | `GET /goals?q=thao (không header)` | 401 | `{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized"}` | PASS |
| X04 | `GET /goals?q=thao (token hết hạn)` | 401 | `{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized"}` | PASS |
| X05 | `GET /goals?q=thao (token sai chữ ký)` | 401 | `{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized"}` | PASS |
| X06 | `GET /assets?q=` | 200 | [AS2 `Tiền mặt dự phòng`, AS1 `Tài khoản Vietcombank`, AS3 `Tiền gửi 12 tháng Techcombank`, AS4 `Tài khoản USD`, AS5 `Căn hộ Thảo Điền`, AS6 `Chứng chỉ quỹ VN30 (phí 0,6%)`, AS7 `Nhà phố Đà Lạt`] | PASS — body giống từng byte với không tham số |
| X06 | `GET /assets?q=++` | 200 | [AS2 `Tiền mặt dự phòng`, AS1 `Tài khoản Vietcombank`, AS3 `Tiền gửi 12 tháng Techcombank`, AS4 `Tài khoản USD`, AS5 `Căn hộ Thảo Điền`, AS6 `Chứng chỉ quỹ VN30 (phí 0,6%)`, AS7 `Nhà phố Đà Lạt`] | PASS — body giống từng byte với không tham số |
| X06 | `GET /liabilities?q=` | 200 | [LB2 `Thẻ tín dụng Techcombank Visa`, LB1 `Vay mua nhà Vietcombank`, LB4 `Vay du học USD`, LB3 `Dư nợ ký quỹ SSI`] | PASS — body giống từng byte với không tham số |
| X06 | `GET /liabilities?q=++` | 200 | [LB2 `Thẻ tín dụng Techcombank Visa`, LB1 `Vay mua nhà Vietcombank`, LB4 `Vay du học USD`, LB3 `Dư nợ ký quỹ SSI`] | PASS — body giống từng byte với không tham số |
| X06 | `GET /cash-flow-entries?q=` | 200 | [CF6 `None`, CF2 `Học phí học kỳ 1 trường Quốc tế Á Châu`, CF4 `Cổ tức quỹ ETF Mỹ`, CF5 `Bảo hiểm nhân thọ`, CF3 `Tiền thuê căn hộ Thảo Điền`, CF1 `Lương tháng công ty Sao Mai`] | PASS — body giống từng byte với không tham số |
| X06 | `GET /cash-flow-entries?q=++` | 200 | [CF6 `None`, CF2 `Học phí học kỳ 1 trường Quốc tế Á Châu`, CF4 `Cổ tức quỹ ETF Mỹ`, CF5 `Bảo hiểm nhân thọ`, CF3 `Tiền thuê căn hộ Thảo Điền`, CF1 `Lương tháng công ty Sao Mai`] | PASS — body giống từng byte với không tham số |
| X06 | `GET /goals?q=` | 200 | [GL1 `Quỹ dự phòng 6 tháng`, GL2 `Mua nhà Thủ Đức`, GL3 `Học phí đại học cho con`] | PASS — body giống từng byte với không tham số |
| X06 | `GET /goals?q=++` | 200 | [GL1 `Quỹ dự phòng 6 tháng`, GL2 `Mua nhà Thủ Đức`, GL3 `Học phí đại học cho con`] | PASS — body giống từng byte với không tham số |
| X07 | `GET /assets?q=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa…(123 ký tự)` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must be at most 120 characters","field":"q"}]}` | PASS |
| X07 | `GET /liabilities?q=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa…(123 ký tự)` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/liabilities","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must be at most 120 characters","field":"q"}]}` | PASS |
| X07 | `GET /cash-flow-entries?q=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa…(123 ký tự)` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/cash-flow-entries","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must be at most 120 characters","field":"q"}]}` | PASS |
| X07 | `GET /goals?q=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa…(123 ký tự)` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/goals","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must be at most 120 characters","field":"q"}]}` | PASS |
| X08 | `GET /assets?q=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa…(122 ký tự)` | 200 | [] | PASS |
| X08 | `GET /liabilities?q=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa…(122 ký tự)` | 200 | [] | PASS |
| X08 | `GET /cash-flow-entries?q=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa…(122 ký tự)` | 200 | [] | PASS |
| X08 | `GET /goals?q=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa…(122 ký tự)` | 200 | [] | PASS |
| X09 | `GET /assets?q=ệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệ…(122 ký tự)` | 200 | [] | PASS |
| X09 | `GET /liabilities?q=ệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệ…(122 ký tự)` | 200 | [] | PASS |
| X09 | `GET /cash-flow-entries?q=ệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệ…(122 ký tự)` | 200 | [] | PASS |
| X09 | `GET /goals?q=ệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệệ…(122 ký tự)` | 200 | [] | PASS |
| X10 | `GET /assets?q=vay%00mua` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not contain a null character","field":"q"}]}` | PASS |
| X10 | `GET /liabilities?q=vay%00mua` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/liabilities","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not contain a null character","field":"q"}]}` | PASS |
| X10 | `GET /cash-flow-entries?q=vay%00mua` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/cash-flow-entries","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not contain a null character","field":"q"}]}` | PASS |
| X10 | `GET /goals?q=vay%00mua` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/goals","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not contain a null character","field":"q"}]}` | PASS |
| X11 | `GET /assets?sort=amount` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must be one of -valuationDate, valuationDate, name, -name","field":"sort"}]}` | PASS |
| X11 | `GET /liabilities?sort=amount` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/liabilities","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must be one of nextPaymentDate, -nextPaymentDate, name, -name","field":"sort"}]}` | PASS |
| X11 | `GET /cash-flow-entries?sort=amount` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/cash-flow-entries","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must be one of -effectiveDate, effectiveDate","field":"sort"}]}` | PASS |
| X11 | `GET /goals?sort=amount` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/goals","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must be one of targetDate, -targetDate, name, -name","field":"sort"}]}` | PASS |
| X12 | `GET /assets?sort=name&sort=-name` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must be one of -valuationDate, valuationDate, name, -name","field":"sort"}]}` | PASS |
| X12 | `GET /liabilities?sort=name&sort=-name` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/liabilities","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must be one of nextPaymentDate, -nextPaymentDate, name, -name","field":"sort"}]}` | PASS |
| X12 | `GET /cash-flow-entries?sort=effectiveDate&sort=effectiveDate` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/cash-flow-entries","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must be one of -effectiveDate, effectiveDate","field":"sort"}]}` | PASS |
| X12 | `GET /goals?sort=name&sort=-name` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/goals","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must be one of targetDate, -targetDate, name, -name","field":"sort"}]}` | PASS |
| AS01 | `GET /assets?q=Thảo+Điền` | 200 | [AS5 `Căn hộ Thảo Điền`] | PASS |
| AS02 | `GET /assets?q=thao+dien` | 200 | [AS5 `Căn hộ Thảo Điền`] | PASS |
| AS03 | `GET /assets?q=THẢO+ĐIỀN` | 200 | [AS5 `Căn hộ Thảo Điền`] | PASS |
| AS04 | `GET /assets?q=điền` | 200 | [AS5 `Căn hộ Thảo Điền`] | PASS |
| AS04 | `GET /assets?q=DIEN` | 200 | [AS5 `Căn hộ Thảo Điền`] | PASS |
| AS05 | `GET /assets?q=khoản` | 200 | [AS1 `Tài khoản Vietcombank`, AS4 `Tài khoản USD`] | PASS |
| AS06 | `GET /assets?q=hsbc` | 200 | [AS4 `Tài khoản USD`] | PASS |
| AS07 | `GET /assets?q=két+sắt` | 200 | [AS2 `Tiền mặt dự phòng`] | PASS |
| AS07 | `GET /assets?q=ket+sat` | 200 | [AS2 `Tiền mặt dự phòng`] | PASS |
| AS08 | `GET /assets?q=++Vietcombank++` | 200 | [AS1 `Tài khoản Vietcombank`] | PASS |
| AS09 | `GET /assets?q=ngân+hàng+sao+hỏa` | 200 | [] | PASS |
| AS10 | `GET /assets?q=%` | 200 | [AS3 `Tiền gửi 12 tháng Techcombank`, AS6 `Chứng chỉ quỹ VN30 (phí 0,6%)`] | PASS |
| AS11 | `GET /assets?q=_` | 200 | [] | PASS |
| AS12 | `GET /assets?q=da+lat` | 200 | [AS7 `Nhà phố Đà Lạt`] | PASS |
| AS12 | `GET /assets?q=Đà+Lạt` | 200 | [AS7 `Nhà phố Đà Lạt`] | PASS |
| AS13 | `GET /assets?category=BANK_ACCOUNT` | 200 | [AS1 `Tài khoản Vietcombank`, AS4 `Tài khoản USD`] | PASS |
| AS14 | `GET /assets?category=CASH&category=BANK_ACCOUNT` | 200 | [AS2 `Tiền mặt dự phòng`, AS1 `Tài khoản Vietcombank`, AS4 `Tài khoản USD`] | PASS |
| AS15 | `GET /assets?category=CASH,BANK_ACCOUNT` | 200 | [AS2 `Tiền mặt dự phòng`, AS1 `Tài khoản Vietcombank`, AS4 `Tài khoản USD`] | PASS |
| AS16 | `GET /assets?currency=USD` | 200 | [AS4 `Tài khoản USD`] | PASS |
| AS17 | `GET /assets?liquidity=MEDIUM&liquidity=LOW` | 200 | [AS3 `Tiền gửi 12 tháng Techcombank`, AS5 `Căn hộ Thảo Điền`, AS6 `Chứng chỉ quỹ VN30 (phí 0,6%)`, AS7 `Nhà phố Đà Lạt`] | PASS |
| AS18 | `GET /assets?q=vietcombank&category=BANK_ACCOUNT&currency=VND&liquidity=HIGH` | 200 | [AS1 `Tài khoản Vietcombank`] | PASS |
| AS19 | `GET /assets?q=thao&category=CASH` | 200 | [] | PASS |
| AS20 | `GET /assets?category=` | 200 | [AS2 `Tiền mặt dự phòng`, AS1 `Tài khoản Vietcombank`, AS3 `Tiền gửi 12 tháng Techcombank`, AS4 `Tài khoản USD`, AS5 `Căn hộ Thảo Điền`, AS6 `Chứng chỉ quỹ VN30 (phí 0,6%)`, AS7 `Nhà phố Đà Lạt`] | PASS — body giống từng byte với không tham số |
| AS21 | `GET /assets?category=GOLD` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must be one of CASH, BANK_ACCOUNT, TERM_DEPOSIT, LISTED_SECURITIES, BOND, INVESTMENT_FUND, REAL_ESTATE, PRIVATE_BUSINESS_EQUITY, VEHICLE_OR_VALUABLE_ASSET, COLLECTIBLE, OTHER_ASSET","field":"category"}]}` | PASS |
| AS22 | `GET /assets?category=bank_account` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must be one of CASH, BANK_ACCOUNT, TERM_DEPOSIT, LISTED_SECURITIES, BOND, INVESTMENT_FUND, REAL_ESTATE, PRIVATE_BUSINESS_EQUITY, VEHICLE_OR_VALUABLE_ASSET, COLLECTIBLE, OTHER_ASSET","field":"category"}]}` | PASS |
| AS23 | `GET /assets?currency=EUR` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must be one of VND, USD","field":"currency"}]}` | PASS |
| AS24 | `GET /assets?liquidity=VERY_HIGH` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must be one of HIGH, MEDIUM, LOW","field":"liquidity"}]}` | PASS |
| AS25 | `GET /assets?category=GOLD&currency=EUR&sort=price&q=aaaaaaaaaaaaaaaaaaaa…(161 ký tự)` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must be at most 120 characters","field":"q"},{"message":"must be one of CASH, BANK_ACCOUNT, TERM_DEPOSIT, LISTED_SECURITIES, BOND, INVESTMENT_FUND, REAL_ESTATE, PRIVATE_BUSINESS_EQUITY, VEHICLE_OR_VALUABLE_ASSET, COLLECTIBLE, OTHER_ASSET","field":"category"},{"message":"must be one of VND, USD","field":"currency"},{"message":"must be one of -valuationDate, valuationDate, name, -name","field":"sort"}]}` | PASS |
| AS26 | `GET /assets?sort=-valuationDate` | 200 | [AS2 `Tiền mặt dự phòng`, AS1 `Tài khoản Vietcombank`, AS3 `Tiền gửi 12 tháng Techcombank`, AS4 `Tài khoản USD`, AS5 `Căn hộ Thảo Điền`, AS6 `Chứng chỉ quỹ VN30 (phí 0,6%)`, AS7 `Nhà phố Đà Lạt`] | PASS — body giống từng byte với không tham số |
| AS27 | `GET /assets?sort=valuationDate` | 200 | [AS7 `Nhà phố Đà Lạt`, AS6 `Chứng chỉ quỹ VN30 (phí 0,6%)`, AS5 `Căn hộ Thảo Điền`, AS4 `Tài khoản USD`, AS3 `Tiền gửi 12 tháng Techcombank`, AS2 `Tiền mặt dự phòng`, AS1 `Tài khoản Vietcombank`] | PASS |
| AS28 | `GET /assets?sort=name` | 200 | [AS6 `Chứng chỉ quỹ VN30 (phí 0,6%)`, AS5 `Căn hộ Thảo Điền`, AS7 `Nhà phố Đà Lạt`, AS3 `Tiền gửi 12 tháng Techcombank`, AS2 `Tiền mặt dự phòng`, AS4 `Tài khoản USD`, AS1 `Tài khoản Vietcombank`] | PASS |
| AS29 | `GET /assets?sort=-name` | 200 | [AS1 `Tài khoản Vietcombank`, AS4 `Tài khoản USD`, AS2 `Tiền mặt dự phòng`, AS3 `Tiền gửi 12 tháng Techcombank`, AS7 `Nhà phố Đà Lạt`, AS5 `Căn hộ Thảo Điền`, AS6 `Chứng chỉ quỹ VN30 (phí 0,6%)`] | PASS |
| AS30 | `GET /assets?q=thao+dien as B` | 200 | [ASB1 `Nhà phố Thảo Điền`] | PASS |
| AS31 | `GET /assets?q=vietcombank as B` | 200 | [ASB2 `Tài khoản Vietcombank`] | PASS |
| LB01 | `GET /liabilities` | 200 | [LB2 `Thẻ tín dụng Techcombank Visa`, LB1 `Vay mua nhà Vietcombank`, LB4 `Vay du học USD`, LB3 `Dư nợ ký quỹ SSI`] | PASS |
| LB02 | `GET /liabilities?q=vietcombank` | 200 | [LB1 `Vay mua nhà Vietcombank`] | PASS |
| LB03 | `GET /liabilities?q=thao+dien` | 200 | [LB1 `Vay mua nhà Vietcombank`] | PASS |
| LB04 | `GET /liabilities?q=VAY` | 200 | [LB1 `Vay mua nhà Vietcombank`, LB4 `Vay du học USD`] | PASS |
| LB05 | `GET /liabilities?q=du+no+ky+quy` | 200 | [LB3 `Dư nợ ký quỹ SSI`] | PASS |
| LB06 | `GET /liabilities?category=BANK_LOAN&category=MARGIN_LOAN` | 200 | [LB1 `Vay mua nhà Vietcombank`, LB3 `Dư nợ ký quỹ SSI`] | PASS |
| LB07 | `GET /liabilities?currency=USD` | 200 | [LB4 `Vay du học USD`] | PASS |
| LB08 | `GET /liabilities?q=ssi&currency=VND&category=MARGIN_LOAN` | 200 | [LB3 `Dư nợ ký quỹ SSI`] | PASS |
| LB09 | `GET /liabilities?sort=-nextPaymentDate` | 200 | [LB4 `Vay du học USD`, LB1 `Vay mua nhà Vietcombank`, LB2 `Thẻ tín dụng Techcombank Visa`, LB3 `Dư nợ ký quỹ SSI`] | PASS |
| LB10 | `GET /liabilities?sort=name` | 200 | [LB3 `Dư nợ ký quỹ SSI`, LB2 `Thẻ tín dụng Techcombank Visa`, LB4 `Vay du học USD`, LB1 `Vay mua nhà Vietcombank`] | PASS |
| LB10 | `GET /liabilities?sort=-name` | 200 | [LB1 `Vay mua nhà Vietcombank`, LB4 `Vay du học USD`, LB2 `Thẻ tín dụng Techcombank Visa`, LB3 `Dư nợ ký quỹ SSI`] | PASS |
| LB11 | `GET /liabilities?sort=nextPaymentDate` | 200 | [LB2 `Thẻ tín dụng Techcombank Visa`, LB1 `Vay mua nhà Vietcombank`, LB4 `Vay du học USD`, LB3 `Dư nợ ký quỹ SSI`] | PASS — body giống từng byte với không tham số |
| LB12 | `GET /liabilities?category=MORTGAGE` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/liabilities","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must be one of BANK_LOAN, PERSONAL_LOAN, MARGIN_LOAN, CREDIT_CARD_BALANCE, GUARANTEE_OBLIGATION, OTHER_LIABILITY","field":"category"}]}` | PASS |
| LB13 | `GET /liabilities?liquidity=HIGH` | 200 | [LB2 `Thẻ tín dụng Techcombank Visa`, LB1 `Vay mua nhà Vietcombank`, LB4 `Vay du học USD`, LB3 `Dư nợ ký quỹ SSI`] | PASS — body giống từng byte với không tham số |
| LB14 | `GET /liabilities?q=vietcombank as B` | 200 | [LBB1 `Vay mua nhà Vietcombank`] | PASS |
| CF01 | `GET /cash-flow-entries` | 200 | [CF6 `None`, CF2 `Học phí học kỳ 1 trường Quốc tế Á Châu`, CF4 `Cổ tức quỹ ETF Mỹ`, CF5 `Bảo hiểm nhân thọ`, CF3 `Tiền thuê căn hộ Thảo Điền`, CF1 `Lương tháng công ty Sao Mai`] | PASS |
| CF02 | `GET /cash-flow-entries?q=lương` | 200 | [CF1 `Lương tháng công ty Sao Mai`] | PASS |
| CF02 | `GET /cash-flow-entries?q=luong+thang` | 200 | [CF1 `Lương tháng công ty Sao Mai`] | PASS |
| CF03 | `GET /cash-flow-entries?q=thảo+điền` | 200 | [CF3 `Tiền thuê căn hộ Thảo Điền`] | PASS |
| CF04 | `GET /cash-flow-entries?q=học+phí` | 200 | [CF2 `Học phí học kỳ 1 trường Quốc tế Á Châu`] | PASS |
| CF04 | `GET /cash-flow-entries?q=hoc+phi` | 200 | [CF2 `Học phí học kỳ 1 trường Quốc tế Á Châu`] | PASS |
| CF05 | `GET /cash-flow-entries?entryType=INCOME` | 200 | [CF6 `None`, CF4 `Cổ tức quỹ ETF Mỹ`, CF1 `Lương tháng công ty Sao Mai`] | PASS |
| CF06 | `GET /cash-flow-entries?entryType=EXPENSE&category=EDUCATION` | 200 | [CF2 `Học phí học kỳ 1 trường Quốc tế Á Châu`] | PASS |
| CF07 | `GET /cash-flow-entries?category=SALARY&category=DIVIDEND` | 200 | [CF4 `Cổ tức quỹ ETF Mỹ`, CF1 `Lương tháng công ty Sao Mai`] | PASS |
| CF08 | `GET /cash-flow-entries?entryType=EXPENSE&category=SALARY` | 200 | [] | PASS |
| CF09 | `GET /cash-flow-entries?timing=RECURRING` | 200 | [CF5 `Bảo hiểm nhân thọ`, CF3 `Tiền thuê căn hộ Thảo Điền`, CF1 `Lương tháng công ty Sao Mai`] | PASS |
| CF10 | `GET /cash-flow-entries?timing=ONE_TIME` | 200 | [CF6 `None`, CF2 `Học phí học kỳ 1 trường Quốc tế Á Châu`, CF4 `Cổ tức quỹ ETF Mỹ`] | PASS |
| CF11 | `GET /cash-flow-entries?recurrence=MONTHLY` | 200 | [CF3 `Tiền thuê căn hộ Thảo Điền`, CF1 `Lương tháng công ty Sao Mai`] | PASS |
| CF11 | `GET /cash-flow-entries?recurrence=YEARLY` | 200 | [CF5 `Bảo hiểm nhân thọ`] | PASS |
| CF12 | `GET /cash-flow-entries?currency=USD` | 200 | [CF4 `Cổ tức quỹ ETF Mỹ`] | PASS |
| CF13 | `GET /cash-flow-entries?effectiveFrom=2026-09-01&effectiveTo=2026-09-30` | 200 | [CF6 `None`, CF2 `Học phí học kỳ 1 trường Quốc tế Á Châu`] | PASS |
| CF14 | `GET /cash-flow-entries?effectiveFrom=2026-09-03&effectiveTo=2026-09-03` | 200 | [CF2 `Học phí học kỳ 1 trường Quốc tế Á Châu`] | PASS |
| CF15 | `GET /cash-flow-entries?effectiveFrom=2026-06-20` | 200 | [CF6 `None`, CF2 `Học phí học kỳ 1 trường Quốc tế Á Châu`, CF4 `Cổ tức quỹ ETF Mỹ`] | PASS |
| CF16 | `GET /cash-flow-entries?effectiveTo=2026-03-01` | 200 | [CF3 `Tiền thuê căn hộ Thảo Điền`, CF1 `Lương tháng công ty Sao Mai`] | PASS |
| CF17 | `GET /cash-flow-entries?q=hoc+phi&entryType=EXPENSE&effectiveFrom=2026-09-01&effectiveTo=2026-09-30` | 200 | [CF2 `Học phí học kỳ 1 trường Quốc tế Á Châu`] | PASS |
| CF18 | `GET /cash-flow-entries?q=luong&effectiveFrom=2026-09-01` | 200 | [] | PASS |
| CF19 | `GET /cash-flow-entries?effectiveFrom=2026-09-30&effectiveTo=2026-09-01` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/cash-flow-entries","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be before effectiveFrom","field":"effectiveTo"}]}` | PASS |
| CF20 | `GET /cash-flow-entries?effectiveFrom=2026-02-30` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/cash-flow-entries","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must be a calendar date in the form YYYY-MM-DD","field":"effectiveFrom"}]}` | PASS |
| CF21 | `GET /cash-flow-entries?effectiveTo=30/09/2026` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/cash-flow-entries","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must be a calendar date in the form YYYY-MM-DD","field":"effectiveTo"}]}` | PASS |
| CF22 | `GET /cash-flow-entries?entryType=TRANSFER` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/cash-flow-entries","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must be one of INCOME, EXPENSE","field":"entryType"}]}` | PASS |
| CF23 | `GET /cash-flow-entries?recurrence=WEEKLY` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/cash-flow-entries","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must be one of MONTHLY, QUARTERLY, YEARLY","field":"recurrence"}]}` | PASS |
| CF23 | `GET /cash-flow-entries?timing=DAILY` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/cash-flow-entries","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must be one of ONE_TIME, RECURRING","field":"timing"}]}` | PASS |
| CF24 | `GET /cash-flow-entries?category=GROCERIES` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/cash-flow-entries","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must be one of SALARY, BUSINESS_INCOME, RENTAL_INCOME, DIVIDEND, DEPOSIT_INTEREST, BOND_OR_FUND_INCOME, OTHER_INCOME, LIVING_COSTS, HOUSING, EDUCATION, HEALTHCARE, INSURANCE, TAX_AND_FEES, DEBT_PRINCIPAL_PAYMENT, INTEREST_PAYMENT, OTHER_EXPENSE","field":"category"}]}` | PASS |
| CF25 | `GET /cash-flow-entries?sort=effectiveDate` | 200 | [CF1 `Lương tháng công ty Sao Mai`, CF3 `Tiền thuê căn hộ Thảo Điền`, CF5 `Bảo hiểm nhân thọ`, CF4 `Cổ tức quỹ ETF Mỹ`, CF2 `Học phí học kỳ 1 trường Quốc tế Á Châu`, CF6 `None`] | PASS |
| CF26 | `GET /cash-flow-entries?sort=-effectiveDate` | 200 | [CF6 `None`, CF2 `Học phí học kỳ 1 trường Quốc tế Á Châu`, CF4 `Cổ tức quỹ ETF Mỹ`, CF5 `Bảo hiểm nhân thọ`, CF3 `Tiền thuê căn hộ Thảo Điền`, CF1 `Lương tháng công ty Sao Mai`] | PASS — body giống từng byte với không tham số |
| CF27 | `GET /cash-flow-entries?q=sao+mai` | 200 | [CF1 `Lương tháng công ty Sao Mai`] | PASS |
| CF27 | `GET /cash-flow-entries?category=BUSINESS_INCOME` | 200 | [CF6 `None`] | PASS |
| CF28 | `GET /cash-flow-entries?q=luong as B` | 200 | [CFB1 `Lương tháng`] | PASS |
| GL01 | `GET /goals` | 200 | [GL1 `Quỹ dự phòng 6 tháng`, GL2 `Mua nhà Thủ Đức`, GL3 `Học phí đại học cho con`] | PASS |
| GL02 | `GET /goals?q=quỹ+dự+phòng` | 200 | [GL1 `Quỹ dự phòng 6 tháng`] | PASS |
| GL02 | `GET /goals?q=quy+du+phong` | 200 | [GL1 `Quỹ dự phòng 6 tháng`] | PASS |
| GL03 | `GET /goals?q=THỦ+ĐỨC` | 200 | [GL2 `Mua nhà Thủ Đức`] | PASS |
| GL03 | `GET /goals?q=thu+duc` | 200 | [GL2 `Mua nhà Thủ Đức`] | PASS |
| GL04 | `GET /goals?type=EDUCATION` | 200 | [GL3 `Học phí đại học cho con`] | PASS |
| GL05 | `GET /goals?type=EMERGENCY_FUND&type=HOME_PURCHASE` | 200 | [GL1 `Quỹ dự phòng 6 tháng`, GL2 `Mua nhà Thủ Đức`] | PASS |
| GL06 | `GET /goals?q=nha&type=HOME_PURCHASE` | 200 | [GL2 `Mua nhà Thủ Đức`] | PASS |
| GL07 | `GET /goals?q=nha&type=EDUCATION` | 200 | [] | PASS |
| GL08 | `GET /goals?type=VACATION` | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/goals","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must be one of EMERGENCY_FUND, HOME_PURCHASE, EDUCATION, RETIREMENT, NET_WORTH_GROWTH, DEBT_REDUCTION, OTHER_GOAL","field":"type"}]}` | PASS |
| GL09 | `GET /goals?sort=-targetDate` | 200 | [GL3 `Học phí đại học cho con`, GL2 `Mua nhà Thủ Đức`, GL1 `Quỹ dự phòng 6 tháng`] | PASS |
| GL10 | `GET /goals?sort=name` | 200 | [GL3 `Học phí đại học cho con`, GL2 `Mua nhà Thủ Đức`, GL1 `Quỹ dự phòng 6 tháng`] | PASS |
| GL10 | `GET /goals?sort=-name` | 200 | [GL1 `Quỹ dự phòng 6 tháng`, GL2 `Mua nhà Thủ Đức`, GL3 `Học phí đại học cho con`] | PASS |
| GL11 | `GET /goals?q=quy+du+phong as B` | 200 | [GLB1 `Quỹ dự phòng 6 tháng`] | PASS |
