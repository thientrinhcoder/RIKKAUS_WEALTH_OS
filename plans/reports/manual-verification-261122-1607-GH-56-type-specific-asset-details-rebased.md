# Manual verification — GH-56 type-specific asset details, rebased onto `main`

Ngày chạy: 22/11/2026, 16:07 (Asia/Ho_Chi_Minh). Người chạy: session BE dev phụ trách #56.

## Kết quả

**118 case, 118 PASS, 0 FAIL.** 103 case đi qua HTTP vào API thật, 15 case chạy thẳng vào PostgreSQL
bằng `psql`. Không case nào fail rồi phải sửa: lượt chạy đầu tiên đã xanh toàn bộ, và log API của cả
lượt chạy không có dòng `ERROR` nào (không có 500).

| Nhóm | Case | Pass |
|---|---|---|
| Guide #56, mục 1–11 (`docs/manual-test-guide-asset-instrument-details.md`) | 78 | 78 |
| Tương tác instrument × valuation history (mục 12 mới của guide #56) | 9 | 9 |
| Smoke #59 (`docs/manual-test-guide-asset-valuations.md`) | 31 | 31 |
| **Tổng** | **118** | **118** |

## Vì sao có lượt chạy này

Code #56 đã được kiểm chứng ngày 10/11/2026 trong commit `1bb2756`
([`manual-verification-261110-0844-GH-56-type-specific-asset-details.md`](manual-verification-261110-0844-GH-56-type-specific-asset-details.md)),
nhưng PR #147 bị merge vào nhánh của #53 chứ không vào `main`. Trong lúc đó `main` có thêm #59
(valuation history) sửa cùng `Asset`, `AssetService`, `AssetController`. Commit này cherry-pick
`1bb2756` lên `main` (`abadb96`), giải conflict, đổi migration `V5` thành `V14`. Lượt chạy này chứng
minh hành vi của #56 còn nguyên trên nền mới và #59 không hồi quy.

## Đối tượng được kiểm

- Nhánh `thientrinhcoder/feat/issue-56-type-specific-asset-details-rebased`, commit
  `feat(api): record the type-specific details of an instrument asset on main` (commit duy nhất trên
  `abadb96`). Jar được đóng gói từ chính working tree của commit đó trước khi ghi báo cáo này; phần
  `services/api` của working tree lúc đóng gói có tree hash `efc872476fc4b93867f9bc599ea36f0368b4d507`, trùng với
  `git rev-parse HEAD:services/api` của commit. Các file khác nhau giữa lúc chạy và lúc commit chỉ là
  docs và plan.
- Jar: `rikkaus-wealth-api-0.0.1-SNAPSHOT.jar`, SHA-256 bắt đầu bằng `fec0903fcf93ffb8`, chạy trên
  JDK 25.0.4.1.
- PostgreSQL dùng một lần: `docker run -d --name rikkaus-pg-issue-56 -p 127.0.0.1:5456:5432 ...
  postgres:18.6-alpine3.24`, database rỗng.
- API: `--spring.profiles.active=local --server.port=8056`, `DB_URL=jdbc:postgresql://localhost:5456/rikkaus`.
- Hai user `An Nguyen` (A) và `Bich Tran` (B) chèn thẳng vào `wealth.users`. Access token được mint
  trong bộ nhớ của runner từ secret phát triển đã commit của profile `local`; không token nào được in
  ra hay ghi vào file.

## Bằng chứng khởi động

```text
o.f.core.internal.command.DbValidate - Successfully validated 11 migrations
o.f.core.internal.command.DbMigrate - Migrating schema "public" to version "12 - feedback entries"
o.f.core.internal.command.DbMigrate - Migrating schema "public" to version "14 - asset instrument details"
o.f.core.internal.command.DbMigrate - Successfully applied 11 migrations to schema "public", now at version v14
o.s.boot.tomcat.TomcatWebServer - Tomcat started on port 8056 (http) with context path '/'
c.r.wealth.RikkausWealthApplication - Started RikkausWealthApplication in 3.151 seconds
```

`spring.jpa.hibernate.ddl-auto: validate` chấp nhận mapping: `V14__asset_instrument_details.sql` và
bốn field mới trên `Asset` khớp tên cột, kiểu và scale, đứng cạnh các cột snapshot mà #59 đã đổi.

## Gate CI

- `./services/api/mvnw -f services/api/pom.xml clean verify -Dopenapi.update=true`: BUILD SUCCESS;
  `openapi.json` đổi đúng 150 dòng thêm, 4 dòng bớt — bốn field trên hai request schema và response,
  cùng các example mới.
- `./services/api/mvnw -f services/api/pom.xml clean verify` không flag: BUILD SUCCESS, 81 unit test
  (gồm `ArchitectureRulesTest`) và 50 integration test, 0 failure. Drift check `openapi.json` khớp,
  `ApplicationContextIT` và `OpenApiContractIT` xanh mà không cần sửa: #56 không thêm route và không
  thêm bảng.

## Cách chạy

Runner Python gửi đúng các request của guide qua HTTP, đọc status và body thật, so với kỳ vọng ghi
trong guide; các case database chạy `psql` qua `docker exec`. Hai khác biệt nhỏ so với lệnh `curl`
trong guide, không đổi ngữ nghĩa:

- Ở mục 1, 3, 7, 9 và 12 một số số thập phân được gửi dưới dạng biểu diễn ngắn nhất của JSON
  (`6.25` thay cho `6.2500`, `21800.0` thay cho `21800.000000`). Cùng một giá trị JSON number; mục 5 và 6,
  nơi số chữ số là điều được kiểm, gửi đúng chuỗi literal của guide.
- `$FUTURE` là hôm nay cộng 31 ngày (`2026-12-23`), tương đương `date -v+1m` của guide.

## Kết quả từng case

### 56/1 accepted cells

| # | Case | Kỳ vọng | Status | Body thật (rút gọn) | Kết quả |
|---|---|---|---|---|---|
| 1 | LISTED_SECURITIES with its accepted fields | 201, sent values echoed, unsent null | 201 | `{"id":"71cddc98-10e5-40dc-9ad9-ac2b79d1a185","name":"Cổ phiếu VCB","category":"LISTED_SECURITIES","currentValue":12000000.0,"currency":"VND","valuationDate":"2026-11-22","liquidity":"HIGH","purchaseValue":null,"purchaseD …` | PASS |
| 2 | BOND with its accepted fields | 201, sent values echoed, unsent null | 201 | `{"id":"d89ef795-5fd0-4746-acbc-363f8bdd8173","name":"Trái phiếu An Tín 2029","category":"BOND","currentValue":200000000.0,"currency":"VND","valuationDate":"2026-11-22","liquidity":"LOW","purchaseValue":null,"purchaseDate …` | PASS |
| 3 | INVESTMENT_FUND with its accepted fields | 201, sent values echoed, unsent null | 201 | `{"id":"9ae8fb51-3c03-414e-b0ec-4b9595056556","name":"Quỹ VFMVF1","category":"INVESTMENT_FUND","currentValue":48300246.0,"currency":"VND","valuationDate":"2026-11-22","liquidity":"MEDIUM","purchaseValue":null,"purchaseDat …` | PASS |
| 4 | TERM_DEPOSIT with its accepted fields | 201, sent values echoed, unsent null | 201 | `{"id":"f41bf3d5-f8ae-4c2a-9140-8ae787739aa1","name":"Tiền gửi 12 tháng An Tín","category":"TERM_DEPOSIT","currentValue":300000000.0,"currency":"VND","valuationDate":"2026-11-22","liquidity":"LOW","purchaseValue":null,"pu …` | PASS |
| 5 | OTHER_ASSET with its accepted fields | 201, sent values echoed, unsent null | 201 | `{"id":"4fd0a057-093f-4b3f-bb02-19b978d3028f","name":"Hợp đồng góp vốn khác","category":"OTHER_ASSET","currentValue":75000000.0,"currency":"VND","valuationDate":"2026-11-22","liquidity":"LOW","purchaseValue":null,"purchas …` | PASS |

### 56/2 refused cells

| # | Case | Kỳ vọng | Status | Body thật (rút gọn) | Kết quả |
|---|---|---|---|---|---|
| 6 | CASH + symbol | 400 naming symbol | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be set for this asset cate …` | PASS |
| 7 | CASH + costPerUnit | 400 naming costPerUnit | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be set for this asset cate …` | PASS |
| 8 | CASH + currentPricePerUnit | 400 naming currentPricePerUnit | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be set for this asset cate …` | PASS |
| 9 | CASH + interestRate | 400 naming interestRate | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be set for this asset cate …` | PASS |
| 10 | BANK_ACCOUNT + symbol | 400 naming symbol | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be set for this asset cate …` | PASS |
| 11 | BANK_ACCOUNT + costPerUnit | 400 naming costPerUnit | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be set for this asset cate …` | PASS |
| 12 | BANK_ACCOUNT + currentPricePerUnit | 400 naming currentPricePerUnit | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be set for this asset cate …` | PASS |
| 13 | BANK_ACCOUNT + interestRate | 400 naming interestRate | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be set for this asset cate …` | PASS |
| 14 | REAL_ESTATE + symbol | 400 naming symbol | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be set for this asset cate …` | PASS |
| 15 | REAL_ESTATE + costPerUnit | 400 naming costPerUnit | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be set for this asset cate …` | PASS |
| 16 | REAL_ESTATE + currentPricePerUnit | 400 naming currentPricePerUnit | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be set for this asset cate …` | PASS |
| 17 | REAL_ESTATE + interestRate | 400 naming interestRate | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be set for this asset cate …` | PASS |
| 18 | PRIVATE_BUSINESS_EQUITY + symbol | 400 naming symbol | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be set for this asset cate …` | PASS |
| 19 | PRIVATE_BUSINESS_EQUITY + costPerUnit | 400 naming costPerUnit | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be set for this asset cate …` | PASS |
| 20 | PRIVATE_BUSINESS_EQUITY + currentPricePerUnit | 400 naming currentPricePerUnit | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be set for this asset cate …` | PASS |
| 21 | PRIVATE_BUSINESS_EQUITY + interestRate | 400 naming interestRate | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be set for this asset cate …` | PASS |
| 22 | VEHICLE_OR_VALUABLE_ASSET + symbol | 400 naming symbol | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be set for this asset cate …` | PASS |
| 23 | VEHICLE_OR_VALUABLE_ASSET + costPerUnit | 400 naming costPerUnit | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be set for this asset cate …` | PASS |
| 24 | VEHICLE_OR_VALUABLE_ASSET + currentPricePerUnit | 400 naming currentPricePerUnit | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be set for this asset cate …` | PASS |
| 25 | VEHICLE_OR_VALUABLE_ASSET + interestRate | 400 naming interestRate | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be set for this asset cate …` | PASS |
| 26 | COLLECTIBLE + symbol | 400 naming symbol | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be set for this asset cate …` | PASS |
| 27 | COLLECTIBLE + costPerUnit | 400 naming costPerUnit | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be set for this asset cate …` | PASS |
| 28 | COLLECTIBLE + currentPricePerUnit | 400 naming currentPricePerUnit | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be set for this asset cate …` | PASS |
| 29 | COLLECTIBLE + interestRate | 400 naming interestRate | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be set for this asset cate …` | PASS |
| 30 | LISTED_SECURITIES + interestRate (narrow cell) | 400 naming interestRate | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be set for this asset cate …` | PASS |
| 31 | INVESTMENT_FUND + interestRate (narrow cell) | 400 naming interestRate | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be set for this asset cate …` | PASS |
| 32 | TERM_DEPOSIT + symbol (narrow cell) | 400 naming symbol | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be set for this asset cate …` | PASS |

### 56/3 all four at once

| # | Case | Kỳ vọng | Status | Body thật (rút gọn) | Kết quả |
|---|---|---|---|---|---|
| 33 | CASH with all four | 400, four errors in contract order | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be set for this asset cate …` | PASS |

### 56/4 none of the four

| # | Case | Kỳ vọng | Status | Body thật (rút gọn) | Kết quả |
|---|---|---|---|---|---|
| 34 | CASH | 201, four fields present and null | 201 | `{"id":"af1d4bca-d952-4ac4-b598-e31433f8e028","name":"Không có chi tiết CASH","category":"CASH","currentValue":1000000.00,"currency":"VND","valuationDate":"2026-11-22","liquidity":"LOW","purchaseValue":null,"purchaseDate" …` | PASS |
| 35 | BANK_ACCOUNT | 201, four fields present and null | 201 | `{"id":"5c892f46-56a9-456d-902b-fd9fc219af63","name":"Không có chi tiết BANK_ACCOUNT","category":"BANK_ACCOUNT","currentValue":1000000.00,"currency":"VND","valuationDate":"2026-11-22","liquidity":"LOW","purchaseValue":nul …` | PASS |
| 36 | TERM_DEPOSIT | 201, four fields present and null | 201 | `{"id":"31cf412f-9056-47e3-97b4-b5b304bb1d7c","name":"Không có chi tiết TERM_DEPOSIT","category":"TERM_DEPOSIT","currentValue":1000000.00,"currency":"VND","valuationDate":"2026-11-22","liquidity":"LOW","purchaseValue":nul …` | PASS |
| 37 | LISTED_SECURITIES | 201, four fields present and null | 201 | `{"id":"e2f0adcc-8d97-4924-95a6-143d0f145def","name":"Không có chi tiết LISTED_SECURITIES","category":"LISTED_SECURITIES","currentValue":1000000.00,"currency":"VND","valuationDate":"2026-11-22","liquidity":"LOW","purchase …` | PASS |
| 38 | BOND | 201, four fields present and null | 201 | `{"id":"92c5e707-3b99-408d-9dca-4fa150007be3","name":"Không có chi tiết BOND","category":"BOND","currentValue":1000000.00,"currency":"VND","valuationDate":"2026-11-22","liquidity":"LOW","purchaseValue":null,"purchaseDate" …` | PASS |
| 39 | INVESTMENT_FUND | 201, four fields present and null | 201 | `{"id":"00d11414-4b15-4239-96c8-b9f6f3fef33c","name":"Không có chi tiết INVESTMENT_FUND","category":"INVESTMENT_FUND","currentValue":1000000.00,"currency":"VND","valuationDate":"2026-11-22","liquidity":"LOW","purchaseValu …` | PASS |
| 40 | REAL_ESTATE | 201, four fields present and null | 201 | `{"id":"5d696dd8-f84f-4685-ac59-d7dd90728274","name":"Không có chi tiết REAL_ESTATE","category":"REAL_ESTATE","currentValue":1000000.00,"currency":"VND","valuationDate":"2026-11-22","liquidity":"LOW","purchaseValue":null, …` | PASS |
| 41 | PRIVATE_BUSINESS_EQUITY | 201, four fields present and null | 201 | `{"id":"c09defc4-d657-48cd-9721-fe5f69275de5","name":"Không có chi tiết PRIVATE_BUSINESS_EQUITY","category":"PRIVATE_BUSINESS_EQUITY","currentValue":1000000.00,"currency":"VND","valuationDate":"2026-11-22","liquidity":"LO …` | PASS |
| 42 | VEHICLE_OR_VALUABLE_ASSET | 201, four fields present and null | 201 | `{"id":"a58903b0-ff1b-4a76-a96c-213f075ca4f2","name":"Không có chi tiết VEHICLE_OR_VALUABLE_ASSET","category":"VEHICLE_OR_VALUABLE_ASSET","currentValue":1000000.00,"currency":"VND","valuationDate":"2026-11-22","liquidity" …` | PASS |
| 43 | COLLECTIBLE | 201, four fields present and null | 201 | `{"id":"e183a512-6330-44fc-a621-c89e9a159b73","name":"Không có chi tiết COLLECTIBLE","category":"COLLECTIBLE","currentValue":1000000.00,"currency":"VND","valuationDate":"2026-11-22","liquidity":"LOW","purchaseValue":null, …` | PASS |
| 44 | OTHER_ASSET | 201, four fields present and null | 201 | `{"id":"9aed2df0-35e7-4c4e-a0b1-91526662cb54","name":"Không có chi tiết OTHER_ASSET","category":"OTHER_ASSET","currentValue":1000000.00,"currency":"VND","valuationDate":"2026-11-22","liquidity":"LOW","purchaseValue":null, …` | PASS |

### 56/5 bounds

| # | Case | Kỳ vọng | Status | Body thật (rút gọn) | Kết quả |
|---|---|---|---|---|---|
| 45 | Lãi suất âm | 400 [('interestRate', 'must not be negative')] | 400 | `{"detail":"Invalid request content.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be negative","field":"intere …` | PASS |
| 46 | Lãi suất quá lớn | 400 [('interestRate', 'must be at most 100')] | 400 | `{"detail":"Invalid request content.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must be at most 100","field":"interes …` | PASS |
| 47 | Lãi suất đúng biên | 201  | 201 | `{"id":"e951b499-6f44-4fbc-bbbd-b79011ba201a","name":"Lãi suất đúng biên","category":"TERM_DEPOSIT","currentValue":1000000.00,"currency":"VND","valuationDate":"2026-11-22","liquidity":"HIGH","purchaseValue":null,"purchase …` | PASS |
| 48 | Lãi suất quá nhiều chữ số | 400 [('interestRate', 'must have at most 3 integer digits and 4 decimals')] | 400 | `{"detail":"Invalid request content.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must have at most 3 integer digits an …` | PASS |
| 49 | Giá vốn âm | 400 [('costPerUnit', 'must not be negative')] | 400 | `{"detail":"Invalid request content.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be negative","field":"costPe …` | PASS |
| 50 | Giá bằng không | 201  | 201 | `{"id":"905fb1d5-f476-4e67-8b53-c3cc26fc6afe","name":"Giá bằng không","category":"LISTED_SECURITIES","currentValue":0.00,"currency":"VND","valuationDate":"2026-11-22","liquidity":"HIGH","purchaseValue":null,"purchaseDate" …` | PASS |
| 51 | Giá quá nhiều chữ số thập phân | 400 [('currentPricePerUnit', 'must have at most 13 integer digits and 6 decimals')] | 400 | `{"detail":"Invalid request content.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must have at most 13 integer digits a …` | PASS |
| 52 | Lãi suất bốn chữ số nguyên | 400 two-on-interestRate | 400 | `{"detail":"Invalid request content.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must have at most 3 integer digits an …` | PASS |
| 53 | Mã quá dài | 400 [('symbol', 'must be at most 32 characters')] | 400 | `{"detail":"Invalid request content.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must be at most 32 characters","field …` | PASS |
| 54 | Mã đúng biên | 201  | 201 | `{"id":"5d75cb98-fdf6-41b1-bd4a-95c1897e8043","name":"Mã đúng biên","category":"LISTED_SECURITIES","currentValue":1000000.00,"currency":"VND","valuationDate":"2026-11-22","liquidity":"HIGH","purchaseValue":null,"purchaseD …` | PASS |

### 56/6 symbol cleaning

| # | Case | Kỳ vọng | Status | Body thật (rút gọn) | Kết quả |
|---|---|---|---|---|---|
| 55 | Mã toàn khoảng trắng | 201 symbol=None | 201 | `{"id":"dfdaa786-8664-4781-a20a-784eca22f015","name":"Mã toàn khoảng trắng","category":"LISTED_SECURITIES","currentValue":1000000.00,"currency":"VND","valuationDate":"2026-11-22","liquidity":"HIGH","purchaseValue":null,"p …` | PASS |
| 56 | Mã khoảng trắng trên loại từ chối | 201 symbol=None | 201 | `{"id":"d1d0012a-f5a3-4b74-bc4c-5d7822b8a373","name":"Mã khoảng trắng trên loại từ chối","category":"CASH","currentValue":1000000.00,"currency":"VND","valuationDate":"2026-11-22","liquidity":"HIGH","purchaseValue":null,"p …` | PASS |
| 57 | Mã có ký tự null | 400 symbol=null | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not contain a null character", …` | PASS |
| 58 | Mã có khoảng trắng hai đầu | 201 symbol=VCB | 201 | `{"id":"266b0f6f-441d-4d7e-a962-5a869412eba2","name":"Mã có khoảng trắng hai đầu","category":"LISTED_SECURITIES","currentValue":1000000.00,"currency":"VND","valuationDate":"2026-11-22","liquidity":"HIGH","purchaseValue":n …` | PASS |
| 59 | Mã là khoảng trắng không ngắt | 201 symbol=None | 201 | `{"id":"02ede475-a6c2-4ae0-9dff-e3fc97524565","name":"Mã là khoảng trắng không ngắt","category":"LISTED_SECURITIES","currentValue":1000000.00,"currency":"VND","valuationDate":"2026-11-22","liquidity":"HIGH","purchaseValue …` | PASS |

### 56/7 nothing derived

| # | Case | Kỳ vọng | Status | Body thật (rút gọn) | Kết quả |
|---|---|---|---|---|---|
| 60 | currentValue stored as sent, not 500 x 24150 | 201, 12000000.00 | 201 | `{"id":"09e157b2-f08c-4045-bb98-c937add4afa3","name":"Giá trị không phải tích số","category":"LISTED_SECURITIES","currentValue":12000000.0,"currency":"VND","valuationDate":"2026-11-22","liquidity":"HIGH","purchaseValue":n …` | PASS |

### 56/7b two problems at once

| # | Case | Kỳ vọng | Status | Body thật (rút gọn) | Kết quả |
|---|---|---|---|---|---|
| 61 | future date + refused symbol | 400, two errors | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be in the future","field": …` | PASS |

### 56/8 exactness in PostgreSQL

| # | Case | Kỳ vọng | Status | Body thật (rút gọn) | Kết quả |
|---|---|---|---|---|---|
| 62 | 24150.123456 and 6.2500 digit for digit | exact decimals | psql | `Quỹ VFMVF1\|VFMVF1\|22400.500000\|24150.123456\| Tiền gửi 12 tháng An Tín\|\|\|\|6.2500` | PASS |

### 56/9 edit route

| # | Case | Kỳ vọng | Status | Body thật (rút gọn) | Kết quả |
|---|---|---|---|---|---|
| 63 | PUT new unit price | 200, price 25000, currentValue unchanged | 200 | `{"id":"9ae8fb51-3c03-414e-b0ec-4b9595056556","name":"Quỹ VFMVF1","category":"INVESTMENT_FUND","currentValue":48300246.00,"currency":"VND","valuationDate":"2026-11-22","liquidity":"MEDIUM","purchaseValue":null,"purchaseDa …` | PASS |
| 64 | PUT omitting all four clears them | 200, four null | 200 | `{"id":"9ae8fb51-3c03-414e-b0ec-4b9595056556","name":"Quỹ VFMVF1","category":"INVESTMENT_FUND","currentValue":48300246.00,"currency":"VND","valuationDate":"2026-11-22","liquidity":"MEDIUM","purchaseValue":null,"purchaseDa …` | PASS |
| 65 | PUT rate on stored INVESTMENT_FUND | 400 naming interestRate | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets/9ae8fb51-3c03-414e-b0ec-4b9595056556","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message" …` | PASS |

### 56/10 ownership

| # | Case | Kỳ vọng | Status | Body thật (rút gọn) | Kết quả |
|---|---|---|---|---|---|
| 66 | B edits A's fund with a symbol | 404, same as a missing id apart from instance/correlationId | 404 | `{"detail":"Resource not found.","instance":"/api/v1/assets/9ae8fb51-3c03-414e-b0ec-4b9595056556","status":404,"title":"Resource not found","type":"urn:rikkaus:problem:not-found","correlationId":"b76a598d-fdfc-4720-ab96-8 …` | PASS |

### 56/10b credentials and malformed

| # | Case | Kỳ vọng | Status | Body thật (rút gọn) | Kết quả |
|---|---|---|---|---|---|
| 67 | no token | 401 | 401 | `{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized","correlationId":"9fd81712-8246-4dfc-a1ba-fe04e8a5f3b7"}` | PASS |
| 68 | symbol as JSON number (known coercion finding) | 201, symbol '123' | 201 | `{"id":"e327ef67-105c-45e9-b329-4c86866f0a6a","name":"Mã là số","category":"LISTED_SECURITIES","currentValue":1000000.00,"currency":"VND","valuationDate":"2026-11-22","liquidity":"HIGH","purchaseValue":null,"purchaseDate" …` | PASS |
| 69 | interestRate as text | 400 malformed-request, no errors | 400 | `{"detail":"Malformed request.","instance":"/api/v1/assets","status":400,"title":"Malformed request","type":"urn:rikkaus:problem:malformed-request","correlationId":"35124654-7b59-4921-9a6f-c4dda194436c"}` | PASS |

### 56/11 table constraints

| # | Case | Kỳ vọng | Status | Body thật (rút gọn) | Kết quả |
|---|---|---|---|---|---|
| 70 | Tiền mặt có mã refused | assets_instrument_pricing_suits_the_category | psql | `ERROR:  new row for relation "assets" violates check constraint "assets_instrument_pricing_suits_the_category" DETAIL:  Failing row contains (4bd2c3ce-4cfb-40b9-92de-c993c4e0c2cf, 0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93, Ti …` | PASS |
| 71 | Chứng khoán có lãi suất refused | assets_interest_rate_suits_the_category | psql | `ERROR:  new row for relation "assets" violates check constraint "assets_interest_rate_suits_the_category" DETAIL:  Failing row contains (53603d5b-1f61-4b9b-80ff-b3bcc1f2a8a7, 0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93, Chứng k …` | PASS |
| 72 | Lãi suất 150 refused | assets_interest_rate_is_a_percent | psql | `ERROR:  new row for relation "assets" violates check constraint "assets_interest_rate_is_a_percent" DETAIL:  Failing row contains (9274a741-107b-4a80-8b32-ff0b918ea902, 0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93, Lãi suất 150, …` | PASS |
| 73 | Mã rỗng refused | assets_symbol_is_present_and_bounded | psql | `ERROR:  new row for relation "assets" violates check constraint "assets_symbol_is_present_and_bounded" DETAIL:  Failing row contains (6a0ee061-ccc9-4e1c-8f36-586a73616123, 0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93, Mã rỗng, B …` | PASS |
| 74 | Quỹ có lãi suất refused | assets_interest_rate_suits_the_category | psql | `ERROR:  new row for relation "assets" violates check constraint "assets_interest_rate_suits_the_category" DETAIL:  Failing row contains (a9f8dd5e-5349-4310-96dd-031dee6fb36a, 0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93, Quỹ có  …` | PASS |
| 75 | Tiền gửi có đơn giá refused | assets_instrument_pricing_suits_the_category | psql | `ERROR:  new row for relation "assets" violates check constraint "assets_instrument_pricing_suits_the_category" DETAIL:  Failing row contains (642e3d64-d228-42a7-9c79-d029194833cc, 0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93, Ti …` | PASS |
| 76 | Lãi suất âm refused | assets_interest_rate_is_a_percent | psql | `ERROR:  new row for relation "assets" violates check constraint "assets_interest_rate_is_a_percent" DETAIL:  Failing row contains (92fd9f3d-9156-4ae8-ab83-3cbf9a4b750b, 0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93, Lãi suất âm,  …` | PASS |
| 77 | Giá vốn âm refused | assets_cost_per_unit_is_not_negative | psql | `ERROR:  new row for relation "assets" violates check constraint "assets_cost_per_unit_is_not_negative" DETAIL:  Failing row contains (4d62d87b-f8e1-4997-a289-54f53470105a, 0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93, Giá vốn âm …` | PASS |
| 78 | valid BOND row accepted | INSERT 0 1 | psql | `INSERT 0 1` | PASS |

### 56x59 interaction

| # | Case | Kỳ vọng | Status | Body thật (rút gọn) | Kết quả |
|---|---|---|---|---|---|
| 79 | create with instrument fields writes exactly one first record | 200, one current record 12000000.00, no instrument members | 200 | `[{"id":"779212b0-9a29-466c-a1f2-6ad61e788730","assetId":"71cddc98-10e5-40dc-9ad9-ac2b79d1a185","value":12000000.00,"currency":"VND","effectiveDate":"2026-11-22","usdToVndRate":null,"rateEffectiveDate":null,"note":null,"c …` | PASS |
| 80 | append a newer valuation | 201 current | 201 | `{"id":"e1e0b0a7-8f56-40f1-88b0-38e319f05c15","assetId":"71cddc98-10e5-40dc-9ad9-ac2b79d1a185","value":12075000.00,"currency":"VND","effectiveDate":"2026-11-22","usdToVndRate":null,"rateEffectiveDate":null,"note":"Cập nhậ …` | PASS |
| 81 | asset follows the valuation, instrument fields untouched | currentValue 12075000.00, symbol/prices/quantity unchanged | 200 | `{"id":"71cddc98-10e5-40dc-9ad9-ac2b79d1a185","name":"Cổ phiếu VCB","category":"LISTED_SECURITIES","currentValue":12075000.00,"currency":"VND","valuationDate":"2026-11-22","liquidity":"HIGH","purchaseValue":null,"purchase …` | PASS |
| 82 | PUT a new unit price records no valuation and moves no value | 200, currentValue 12075000.00, history stays 2 (was 2, now 2) | 200 | `{"id":"71cddc98-10e5-40dc-9ad9-ac2b79d1a185","name":"Cổ phiếu VCB","category":"LISTED_SECURITIES","currentValue":12075000.00,"currency":"VND","valuationDate":"2026-11-22","liquidity":"HIGH","purchaseValue":null,"purchase …` | PASS |
| 83 | USD bond with rate snapshot and instrument fields | 201 | 201 | `{"id":"1bfda8fa-a211-4f52-9f5a-474f2baafbdf","name":"Trái phiếu USD","category":"BOND","currentValue":15000.00,"currency":"USD","valuationDate":"2026-11-22","liquidity":"LOW","purchaseValue":null,"purchaseDate":null,"qua …` | PASS |
| 84 | VND rate snapshot + refused symbol in one response | 400, three errors | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must be absent for a VND valuation" …` | PASS |
| 85 | PUT future purchaseDate + refused rate in one response | 400, both named | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets/71cddc98-10e5-40dc-9ad9-ac2b79d1a185","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message" …` | PASS |
| 86 | snapshot agrees with history for every asset | 0 disagreeing rows | psql | `0` | PASS |
| 87 | every API-created asset has a history | 0 (the one psql-inserted row excepted) | psql | `0` | PASS |

### 59 smoke

| # | Case | Kỳ vọng | Status | Body thật (rút gọn) | Kết quả |
|---|---|---|---|---|---|
| 88 | create USD asset with rate snapshot | 201, instrument fields null | 201 | `{"id":"15ca7d7d-6756-4ba2-89b9-f678e7929335","name":"Tài khoản USD","category":"BANK_ACCOUNT","currentValue":20000.00,"currency":"USD","valuationDate":"2026-09-12","liquidity":"HIGH","purchaseValue":19500.00,"purchaseDat …` | PASS |
| 89 | first record written at creation | one current record 20000.00 @25100 | 200 | `[{"id":"320a5c0f-4b20-41c8-9e5f-1af8abe152e0","assetId":"15ca7d7d-6756-4ba2-89b9-f678e7929335","value":20000.00,"currency":"USD","effectiveDate":"2026-09-12","usdToVndRate":25100.000000,"rateEffectiveDate":"2026-09-12"," …` | PASS |
| 90 | create VND asset with no rate | 201 | 201 | `{"id":"867b0815-a201-47ca-92e0-16fbc0137e81","name":"Tiền mặt dự phòng","category":"CASH","currentValue":148000000.00,"currency":"VND","valuationDate":"2026-11-22","liquidity":"HIGH","purchaseValue":null,"purchaseDate":n …` | PASS |
| 91 | VND asset with rate members | 400 two errors | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must be absent for a VND valuation" …` | PASS |
| 92 | USD rate without date | 400 | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must be given together with usdToVn …` | PASS |
| 93 | newer value current | 201 current | 201 | `{"id":"b90bbb07-c1f6-41c2-ae02-74891afd96c7","assetId":"15ca7d7d-6756-4ba2-89b9-f678e7929335","value":21250.00,"currency":"USD","effectiveDate":"2026-09-14","usdToVndRate":25100.000000,"rateEffectiveDate":"2026-09-12","n …` | PASS |
| 94 | asset moves to newer value | 21250.00 / 2026-09-14 | 200 | `{"id":"15ca7d7d-6756-4ba2-89b9-f678e7929335","name":"Tài khoản USD","category":"BANK_ACCOUNT","currentValue":21250.00,"currency":"USD","valuationDate":"2026-09-14","liquidity":"HIGH","purchaseValue":19500.00,"purchaseDat …` | PASS |
| 95 | older value moves nothing | 201 current=false, asset unchanged | 201 | `{"id":"5d528cec-75e7-4e9d-b150-07f1cdf1ba06","assetId":"15ca7d7d-6756-4ba2-89b9-f678e7929335","value":18000.00,"currency":"USD","effectiveDate":"2026-05-31","usdToVndRate":25020.000000,"rateEffectiveDate":"2026-05-30","n …` | PASS |
| 96 | correction adds a record, original intact | 201, original still 18000.00 | 201 | `{"id":"07d79403-8f2d-47e9-a3a1-29a1d814efd5","assetId":"15ca7d7d-6756-4ba2-89b9-f678e7929335","value":18500.00,"currency":"USD","effectiveDate":"2026-05-31","usdToVndRate":25020.000000,"rateEffectiveDate":"2026-05-30","n …` | PASS |
| 97 | correcting the current record | 201 current, five records, asset 21300.00 | 201 | `{"id":"56e7ca8e-cc2e-428a-92ce-5f7ac0d663ab","assetId":"15ca7d7d-6756-4ba2-89b9-f678e7929335","value":21300.00,"currency":"USD","effectiveDate":"2026-09-14","usdToVndRate":25100.000000,"rateEffectiveDate":"2026-09-12","n …` | PASS |
| 98 | correction without note | 400 note | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets/15ca7d7d-6756-4ba2-89b9-f678e7929335/valuations","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors": …` | PASS |
| 99 | currency must match the asset | 400 currency | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets/15ca7d7d-6756-4ba2-89b9-f678e7929335/valuations","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors": …` | PASS |
| 100 | effectiveDate before purchaseDate | 400 effectiveDate | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets/15ca7d7d-6756-4ba2-89b9-f678e7929335/valuations","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors": …` | PASS |
| 101 | effectiveDate in the future | 400 | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets/15ca7d7d-6756-4ba2-89b9-f678e7929335/valuations","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors": …` | PASS |
| 102 | B list A's history | 404 | 404 | `{"detail":"Resource not found.","instance":"/api/v1/assets/15ca7d7d-6756-4ba2-89b9-f678e7929335/valuations","status":404,"title":"Resource not found","type":"urn:rikkaus:problem:not-found","correlationId":"20483f36-3345- …` | PASS |
| 103 | B reads A's record | 404 | 404 | `{"detail":"Resource not found.","instance":"/api/v1/assets/15ca7d7d-6756-4ba2-89b9-f678e7929335/valuations/5d528cec-75e7-4e9d-b150-07f1cdf1ba06","status":404,"title":"Resource not found","type":"urn:rikkaus:problem:not-f …` | PASS |
| 104 | B appends to A's asset | 404 | 404 | `{"detail":"Resource not found.","instance":"/api/v1/assets/15ca7d7d-6756-4ba2-89b9-f678e7929335/valuations","status":404,"title":"Resource not found","type":"urn:rikkaus:problem:not-found","correlationId":"7cb61c13-7a39- …` | PASS |
| 105 | record of another of A's assets | 404 | 404 | `{"detail":"Resource not found.","instance":"/api/v1/assets/867b0815-a201-47ca-92e0-16fbc0137e81/valuations/5d528cec-75e7-4e9d-b150-07f1cdf1ba06","status":404,"title":"Resource not found","type":"urn:rikkaus:problem:not-f …` | PASS |
| 106 | credentials: no token | 401 | 401 | `{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized","correlationId":"6c605daf-110f-4762-93d2-583441de25af"}` | PASS |
| 107 | credentials: expired | 401 | 401 | `{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized","correlationId":"8b7e4654-fdfb-4f8e-80ee-434e248f5f6f"}` | PASS |
| 108 | credentials: forged | 401 | 401 | `{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized","correlationId":"8d784510-7db7-459b-b2ba-32d82320b8bc"}` | PASS |
| 109 | PUT on a valuation record | 405 | 405 | `{"detail":"Method 'PUT' is not supported.","instance":"/api/v1/assets/15ca7d7d-6756-4ba2-89b9-f678e7929335/valuations/5d528cec-75e7-4e9d-b150-07f1cdf1ba06","status":405,"title":"Method not allowed","type":"urn:rikkaus:pr …` | PASS |
| 110 | PATCH on a valuation record | 405 | 405 | `{"detail":"Method 'PATCH' is not supported.","instance":"/api/v1/assets/15ca7d7d-6756-4ba2-89b9-f678e7929335/valuations/5d528cec-75e7-4e9d-b150-07f1cdf1ba06","status":405,"title":"Method not allowed","type":"urn:rikkaus: …` | PASS |
| 111 | DELETE on a valuation record | 405 | 405 | `{"detail":"Method 'DELETE' is not supported.","instance":"/api/v1/assets/15ca7d7d-6756-4ba2-89b9-f678e7929335/valuations/5d528cec-75e7-4e9d-b150-07f1cdf1ba06","status":405,"title":"Method not allowed","type":"urn:rikkaus …` | PASS |
| 112 | record unchanged after the 405s | 18000.00 | 200 | `{"id":"5d528cec-75e7-4e9d-b150-07f1cdf1ba06","assetId":"15ca7d7d-6756-4ba2-89b9-f678e7929335","value":18000.00,"currency":"USD","effectiveDate":"2026-05-31","usdToVndRate":25020.000000,"rateEffectiveDate":"2026-05-30","n …` | PASS |
| 113 | PUT purchaseDate after the earliest history record | 400 purchaseDate | 400 | `{"detail":"Request validation failed.","instance":"/api/v1/assets/15ca7d7d-6756-4ba2-89b9-f678e7929335","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message" …` | PASS |
| 114 | trigger refuses UPDATE | error from asset_valuations_are_append_only | psql | `ERROR:  wealth.asset_valuations is append-only: a valuation is never updated. Record a new one. CONTEXT:  PL/pgSQL function wealth.refuse_asset_valuation_change() line 4 at RAISE` | PASS |
| 115 | trigger refuses DELETE | error | psql | `ERROR:  wealth.asset_valuations is append-only: a valuation is never deleted while its asset exists. CONTEXT:  PL/pgSQL function wealth.refuse_asset_valuation_change() line 8 at RAISE` | PASS |
| 116 | deleting an asset removes its history | 204, 1 -> 0 | 204 | `before=1 after=0` | PASS |
| 117 | deleting an instrument asset (fund) with its history | 204 | 204 | `` | PASS |
| 118 | snapshot agrees with history at the end | 0 | psql | `0` | PASS |

## Phát hiện

Không có phát hiện mới làm đổi hành vi. Những điểm dưới đây được ghi lại để người đọc không phải tự
tìm ra:

- **`symbol` gửi dưới dạng JSON number vẫn được nhận** (case "symbol as JSON number"): `123` được lưu
  thành chuỗi `"123"`. Đây là mặc định của Jackson cho mọi field chuỗi trong API, đã ghi ở báo cáo
  10/11, không phải do rebase.
- **`interestRate` bị chặn trong khoảng 0–100**, nên lợi suất âm của trái phiếu thật bị từ chối. Quyết
  định của plan #56, vẫn còn là câu hỏi mở cho Product Owner.
- **Thông điệp `detail` khác nhau giữa hai nguồn lỗi 400**: lỗi từ bean validation trả
  `"detail":"Invalid request content."`, lỗi từ service (category, date, rate snapshot) trả
  `"detail":"Request validation failed."`; cả hai cùng `type` `validation-failed` và cùng mảng
  `errors`. Có từ trước #56 (#53), client switch theo `type` nên không ảnh hưởng.
- **Response của `POST`/`PUT` trả lại số với scale người dùng gửi** (ví dụ `"currentValue":12000000.0`
  khi gửi `12000000.0`), còn `GET` sau đó trả theo scale cột (`12000000.00`). Có từ #53; giá trị bằng
  nhau, chỉ khác cách viết.

## Không được chạy lại trong lượt này

- Các case đồng thời của guide #59 ("Two requests at once", "An edit racing an append") và nhóm
  protocol `406`/`415`/JSON cụt trên route valuation. Rebase không đổi `AssetValuationService`,
  `AssetValuationController` hay `findAndLockById`; `replaceMetadata` vẫn lấy cùng row lock trước khi
  đọc, chỉ thêm việc kiểm bốn field instrument. Nguy cơ hồi quy ở đây thấp nhưng chưa được đo lại.
- Guide `docs/manual-test-guide-assets.md` (#53) không được chạy lại toàn bộ; các case chung mà guide #56
  và smoke #59 chạm tới (tạo, đọc, sửa, xoá, ownership, credential) đều xanh.
- Frontend (#55) chưa tiêu thụ contract này.

## Dọn dẹp

API (PID của lượt chạy) đã dừng, container `rikkaus-pg-issue-56` đã xoá bằng `docker rm -f`; cổng 8056
và 5456 không còn tiến trình nào giữ.

## Lượt chạy lại sau khi merge `main` có #77 và #80 — 22/11/2026, 16:26

`main` đi tới `c025079`, có thêm `cashflow/reporting` (#77) và `cashflow/forecast` (#80); không có
migration mới. Nhánh merge `origin/main` vào (merge commit, không rebase vì nhánh đã có trên origin và
PR #160). Conflict duy nhất nằm ở mục *What is not here* của `docs/api-contract-conventions.md`: `main`
đã bỏ dòng "Monthly totals and the forecast" vì hai tính năng đó đã có, nhánh này thay dòng
"Type-specific asset fields" bằng dòng "Any calculation over the instrument fields". Kết quả giữ dòng
của #56 và bỏ cả hai dòng đã lỗi thời. `OpenApiContractIT`, `ApplicationContextIT` và `.gitignore`
merge tự động vì nhánh này không sửa chúng; `openapi.json` được regenerate.

- Rà CodeQL `java/comparison-with-wider-type`: phần Java #56 thêm vào không có vòng lặp hay phép so
  sánh số nguyên nào, nên không có gì phải sửa.
- `clean verify -Dopenapi.update=true` rồi `clean verify` không flag: BUILD SUCCESS, 81 unit + 50 IT,
  drift check khớp. Diff `openapi.json` so với `main` vẫn đúng +150/−4, chỉ là bốn field và example.
- Jar đã merge (SHA-256 bắt đầu bằng `66b55d29f30e419b`) trên PostgreSQL 18.6 dùng một lần cổng
  5456, API cổng 8056: Flyway `Successfully applied 11 migrations ... now at version v14`,
  `ddl-auto: validate` chấp nhận mapping.
- Cùng runner, cùng 118 case như bảng trên: **118 PASS, 0 FAIL** (103 HTTP, 15 PostgreSQL), từng nhóm
  giống hệt lượt 16:07; log API không có dòng `ERROR`.
- API đã dừng, container `rikkaus-pg-issue-56` đã xoá, cổng 8056 và 5456 trống.
