# Hướng dẫn test thủ công — danh mục dòng tiền

Resource `/api/v1/cash-flow-entries` (issue #71) ship **không có automated test**, theo chỉ đạo của
Product Owner. Tài liệu này là phần thay thế: nó là toàn bộ việc kiểm chứng, và chạy lại nó là cách
duy nhất một regression trong slice này bị phát hiện. Kết quả của lần chạy đi kèm implementation ghi ở
[`plans/reports/manual-verification-261114-1735-GH-71-cash-flow-entry-catalog-crud.md`](../plans/reports/manual-verification-261114-1735-GH-71-cash-flow-entry-catalog-crud.md).
Danh sách scenario gốc, viết trước khi code, ở
[`plans/261114-1735-GH-71-cash-flow-entry-catalog-crud/phase-01-test-scenarios.md`](../plans/261114-1735-GH-71-cash-flow-entry-catalog-crud/phase-01-test-scenarios.md);
số case ở đây là số case ở đó.

Không cần tài khoản Google, môi trường deploy hay app mobile.

## Chuẩn bị

Mọi lệnh chạy từ thư mục gốc repo, có Docker daemon.

### 1. Một PostgreSQL dùng một lần

Không dùng volume `rikkaus-postgres-data` và không dùng cổng 5432, để lần chạy không chạm vào database
phát triển của bạn.

```bash
docker run -d --name rikkaus-pg-issue-71 -p 127.0.0.1:5471:5432 -e POSTGRES_DB=rikkaus -e POSTGRES_USER=rikkaus -e POSTGRES_PASSWORD=rikkaus-local-only postgres:18.6-alpine3.24
```

```bash
docker exec rikkaus-pg-issue-71 pg_isready -U rikkaus
```

### 2. Ứng dụng đã đóng gói

API cần JDK 25; JDK mặc định của máy thường là 21 và fail ở `release 25`.

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./services/api/mvnw -f services/api/pom.xml package -DskipTests
```

```bash
DB_URL=jdbc:postgresql://localhost:5471/rikkaus JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home /opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home/bin/java -jar services/api/target/rikkaus-wealth-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=local --server.port=8071
```

Hai dòng trong log khởi động là một phần của việc kiểm chứng. Flyway phải báo đã apply tới
`V8__cash_flow_entries.sql` (`now at version v8`; số có thể cao hơn nếu migration sau này được thêm), và
context phải khởi động: `spring.jpa.hibernate.ddl-auto: validate` là
thứ chứng minh migration và entity `CashFlowEntry` khớp nhau từng cột.

### 3. Hai người dùng

Catalog là owner-scoped nên các case liên người dùng cần hai row. Không có endpoint đăng ký — tài khoản
sinh ra từ đăng nhập Google — nên chèn thẳng.

```bash
docker exec -i rikkaus-pg-issue-71 psql -U rikkaus -d rikkaus -c "INSERT INTO wealth.users (id, google_subject, email, display_name, created_at, updated_at) VALUES ('0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93','google-sub-a','an.nguyen@example.com','An Nguyễn', now(), now()), ('7b41e590-3c82-4f16-b0d7-59e1c4f3a842','google-sub-b','bich.tran@example.com','Bích Trần', now(), now());"
```

### 4. Access token

Mint bằng secret phát triển đã commit của profile `local` và đúng bộ claim `AccessTokenService` phát
hành. Mint thay vì stub có ý nghĩa: request vẫn đi qua `BearerTokenAuthenticationFilter` và
`AccessTokenService.verify` thật.

```bash
python3 - <<'PY'
import base64, hashlib, hmac, json, time
secret = b"local-development-only-jwt-signing-secret"
def b64(raw): return base64.urlsafe_b64encode(raw).rstrip(b"=").decode()
def mint(sub, ttl=900, key=secret):
    now = int(time.time())
    h = b64(json.dumps({"alg": "HS256"}, separators=(",", ":")).encode())
    p = b64(json.dumps({"iss": "rikkaus-wealth-api", "sub": sub,
                        "sid": "11111111-2222-3333-4444-555555555555",
                        "iat": now, "exp": now + ttl}, separators=(",", ":")).encode())
    return f"{h}.{p}." + b64(hmac.new(key, f"{h}.{p}".encode(), hashlib.sha256).digest())
print("A        ", mint("0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93"))
print("B        ", mint("7b41e590-3c82-4f16-b0d7-59e1c4f3a842"))
print("expired  ", mint("0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93", ttl=-60))
print("forged   ", mint("0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93", key=b"a-different-secret-entirely"))
PY
```

```bash
export A=<token A> B=<token B> EXPIRED=<expired> FORGED=<forged>
```

Một helper giữ mỗi case dưới đây trong một dòng:

```bash
api() { curl -s -i -H "Authorization: Bearer $1" -H 'Content-Type: application/json' -X "$2" "http://127.0.0.1:8071/api/v1$3" ${4:+-d "$4"}; }
```

### 5. Tài sản và công nợ để liên kết

Tạo qua resource của slice `assets` và `liabilities`, không chèn thẳng, để không phụ thuộc vào cột của
bảng khác.

```bash
api "$A" POST /assets '{"name":"Căn hộ Thảo Điền","category":"REAL_ESTATE","currentValue":"6100000000.00","currency":"VND","valuationDate":"2026-08-02","liquidity":"LOW"}'
api "$A" POST /assets '{"name":"Xe Volvo XC60","category":"VEHICLE_OR_VALUABLE_ASSET","currentValue":"1850000000.00","currency":"VND","valuationDate":"2026-09-01","liquidity":"MEDIUM"}'
api "$B" POST /assets '{"name":"Tiền mặt của Bích","category":"CASH","currentValue":"5000000.00","currency":"VND","valuationDate":"2026-09-01","liquidity":"HIGH"}'
api "$A" POST /liabilities '{"name":"Vay mua nhà Vietcombank","category":"BANK_LOAN","outstandingBalance":"2400000000.00","currency":"VND","recurringPaymentAmount":"23500000.00","recurringPaymentFrequency":"MONTHLY"}'
api "$B" POST /liabilities '{"name":"Vay tiêu dùng FE Credit","category":"PERSONAL_LOAN","outstandingBalance":"120000000.00","currency":"VND"}'
```

Giữ các `id`, gọi là `$ASSET_A` (căn hộ), `$CAR_A` (xe), `$ASSET_B`, `$LOAN_A` và `$LOAN_B`.

## Các case

Đọc dòng status và body của mỗi case. Nơi nào kỳ vọng `validation-failed`, mảng `errors` và trường nó
nêu mới là assertion — không phải riêng con số 400, vì vài lỗi không liên quan cũng cho 400.

### A. Đủ 16 danh mục, đúng loại (case 1–16)

Mười sáu lần tạo, một lần mỗi danh mục. Mỗi lần phải `201` với `Location: /api/v1/cash-flow-entries/<id>`.

```bash
api "$A" POST /cash-flow-entries '{"entryType":"INCOME","category":"SALARY","amount":"45000000.00","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-01","notes":"Lương tháng"}'
api "$A" POST /cash-flow-entries '{"entryType":"INCOME","category":"BUSINESS_INCOME","amount":"26200000.00","currency":"VND","timing":"ONE_TIME","effectiveDate":"2026-09-10"}'
api "$A" POST /cash-flow-entries '{"entryType":"INCOME","category":"RENTAL_INCOME","amount":"18000000.00","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-02-01","notes":"Tiền thuê căn hộ Thảo Điền"}'
api "$A" POST /cash-flow-entries '{"entryType":"INCOME","category":"DIVIDEND","amount":"150.00","currency":"USD","timing":"ONE_TIME","effectiveDate":"2026-09-12"}'
api "$A" POST /cash-flow-entries '{"entryType":"INCOME","category":"DEPOSIT_INTEREST","amount":"1500000.00","currency":"VND","timing":"RECURRING","recurrence":"QUARTERLY","effectiveDate":"2026-03-31"}'
api "$A" POST /cash-flow-entries '{"entryType":"INCOME","category":"BOND_OR_FUND_INCOME","amount":"1500000.00","currency":"VND","timing":"RECURRING","recurrence":"YEARLY","effectiveDate":"2026-06-30"}'
api "$A" POST /cash-flow-entries '{"entryType":"INCOME","category":"OTHER_INCOME","amount":"235000.00","currency":"VND","timing":"ONE_TIME","effectiveDate":"2026-09-20"}'
api "$A" POST /cash-flow-entries '{"entryType":"EXPENSE","category":"LIVING_COSTS","amount":"23400000.00","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-01"}'
api "$A" POST /cash-flow-entries '{"entryType":"EXPENSE","category":"HOUSING","amount":"18750000.00","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-01"}'
api "$A" POST /cash-flow-entries '{"entryType":"EXPENSE","category":"EDUCATION","amount":"12500000.00","currency":"VND","timing":"RECURRING","recurrence":"QUARTERLY","effectiveDate":"2026-09-05","endDate":"2029-06-30","notes":"Học phí"}'
api "$A" POST /cash-flow-entries '{"entryType":"EXPENSE","category":"HEALTHCARE","amount":"4650000.00","currency":"VND","timing":"ONE_TIME","effectiveDate":"2026-09-18"}'
api "$A" POST /cash-flow-entries '{"entryType":"EXPENSE","category":"INSURANCE","amount":"5400000.00","currency":"VND","timing":"RECURRING","recurrence":"YEARLY","effectiveDate":"2026-04-01"}'
api "$A" POST /cash-flow-entries '{"entryType":"EXPENSE","category":"TAX_AND_FEES","amount":"8250000.00","currency":"VND","timing":"RECURRING","recurrence":"QUARTERLY","effectiveDate":"2026-01-31"}'
api "$A" POST /cash-flow-entries '{"entryType":"EXPENSE","category":"DEBT_PRINCIPAL_PAYMENT","amount":"10000000.00","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-15","endDate":"2031-01-15"}'
api "$A" POST /cash-flow-entries '{"entryType":"EXPENSE","category":"INTEREST_PAYMENT","amount":"6250000.00","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-15"}'
api "$A" POST /cash-flow-entries '{"entryType":"EXPENSE","category":"OTHER_EXPENSE","amount":"2500000.00","currency":"VND","timing":"ONE_TIME","effectiveDate":"2026-09-25"}'
```

Case 4 phải echo `150.00` với `"currency":"USD"` — **không** có số VND nào. Slice không quy đổi; client
nhân với tỷ giá từ `GET /api/v1/settings/exchange-rate`. Case 1 cũng là case "trường optional vắng":
`endDate`, `linkedAssetId` phải đọc lại là `null`, không phải `""` hay `0`. Giữ `id` của case 1 (gọi là
`$SALARY`) và case 3 (`$RENT`); các case sau cần.

### B. Danh mục sai loại (case 17–18)

```bash
api "$A" POST /cash-flow-entries '{"entryType":"EXPENSE","category":"SALARY","amount":"45000000.00","currency":"VND","timing":"ONE_TIME","effectiveDate":"2026-09-10"}'
api "$A" POST /cash-flow-entries '{"entryType":"INCOME","category":"HOUSING","amount":"18750000.00","currency":"VND","timing":"ONE_TIME","effectiveDate":"2026-09-10"}'
```

Cả hai `400` `validation-failed` với đúng một entry `{field: category, message: "must belong to the entry type"}`.

### C. Thiếu từng trường bắt buộc (case 19–24)

Bỏ lần lượt một member khỏi body tối thiểu: `entryType`, `category`, `amount`, `currency`, `timing`,
`effectiveDate`. Mỗi case `400` `validation-failed` với một entry nêu **đúng trường đó** và `must not be null`.

```bash
api "$A" POST /cash-flow-entries '{"category":"BUSINESS_INCOME","amount":"26200000.00","currency":"VND","timing":"ONE_TIME","effectiveDate":"2026-09-10"}'
```

### D. Số tiền (case 25–28)

| Gửi | Kỳ vọng |
|---|---|
| `"amount":"0"` | `400`, `amount` / `must be greater than zero` |
| `"amount":"-1000000.00"` | `400`, `amount` / `must be greater than zero` |
| `"amount":"1000000.123"` | `400`, `amount` / `must have at most 17 integer digits and 2 decimals` |
| `"amount":"45000000"` | `201`, body trả về **và** lần `GET` sau đều là `45000000.00` |

Giá trị quá precision quan trọng nhất: phải bị từ chối ở đây với tên trường, thay vì đến `NUMERIC(19, 2)`
và fail thành 500.

### E. Hằng số lạ (case 29–33)

```bash
api "$A" POST /cash-flow-entries '{"entryType":"TRANSFER","category":"SALARY","amount":"1.00","currency":"VND","timing":"ONE_TIME","effectiveDate":"2026-09-10"}'
```

`400` `malformed-request`, không `validation-failed`, và **không** có mảng `errors` — Jackson không dựng
được record nên lỗi xảy ra trước bean validation. Lặp cho `category: CRYPTO_INCOME`, `currency: EUR`,
`timing: WEEKLY`, `recurrence: DAILY`. Đây là hành vi đã ghi trong conventions doc, không phải lỗi;
client thật chọn từ picker.

### F. Một lần, định kỳ, ngày (case 34–42)

| Gửi | Kỳ vọng |
|---|---|
| `ONE_TIME` kèm `"recurrence":"MONTHLY"` | `400`, `recurrence` / `must be omitted for a one-time entry` |
| `RECURRING` không có `recurrence` | `400`, `recurrence` / `must be present for a recurring entry` |
| `ONE_TIME` kèm `"endDate":"2026-12-31"` | `400`, `endDate` / `must be omitted for a one-time entry` |
| `RECURRING MONTHLY`, `effectiveDate 2026-09-05`, `endDate 2026-09-04` | `400`, `endDate` / `must not be before the effective date` |
| `RECURRING MONTHLY`, `endDate` bằng `effectiveDate` | `201` |
| `RECURRING MONTHLY`, không `endDate` | `201`, `endDate: null` |
| `effectiveDate 2027-01-01` | `201` — khoản dự kiến hợp lệ, không có luật "không ở tương lai" |
| `ONE_TIME` kèm cả `recurrence: YEARLY` và `endDate` | `400`, **hai** entry `recurrence` và `endDate`, một lần trả về |
| `"effectiveDate":"01/01/2026"` | `400` `malformed-request` |

```bash
api "$A" POST /cash-flow-entries '{"entryType":"INCOME","category":"OTHER_INCOME","amount":"235000.00","currency":"VND","timing":"ONE_TIME","recurrence":"YEARLY","effectiveDate":"2026-09-20","endDate":"2026-12-31"}'
```

### G. Ghi chú (case 43–46)

| `notes` | Kỳ vọng |
|---|---|
| 2001 ký tự | `400`, `notes` / `must be at most 2000 characters` |
| `"   "` | `201`, `notes: null` |
| `"  Lương tháng 11.  "` | `201`, `notes: "Lương tháng 11."` |
| `"Lương\u0000tháng"` | `400`, `notes` / `must not contain a null character` |

Đặt NUL ở **giữa** chuỗi. Một `201` ở dòng cuối nghĩa là PostgreSQL sắp nhận một byte nó không lưu được
vào cột `text`, và dòng kế trong log là một 500.

### H. Liên kết tài sản (case 47–53)

```bash
api "$A" POST /cash-flow-entries '{"entryType":"INCOME","category":"RENTAL_INCOME","amount":"18000000.00","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-02-01","linkedAssetId":"'$ASSET_A'","notes":"Tiền thuê căn hộ Thảo Điền"}'   # 201, echo linkedAssetId; giữ id là $LINKED
api "$A" POST /cash-flow-entries '{"entryType":"INCOME","category":"RENTAL_INCOME","amount":"18000000.00","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-02-01","linkedAssetId":"'$ASSET_B'"}'   # 400
api "$A" POST /cash-flow-entries '{"entryType":"INCOME","category":"RENTAL_INCOME","amount":"18000000.00","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-02-01","linkedAssetId":"00000000-0000-4000-8000-000000000000"}'   # 400
api "$A" POST /cash-flow-entries '{"entryType":"INCOME","category":"RENTAL_INCOME","amount":"18000000.00","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-02-01","linkedAssetId":"not-a-uuid"}'   # 400 malformed-request
```

Case 48 và 49 phải `validation-failed` với `{field: linkedAssetId, message: "must reference one of the
signed-in user's assets"}`, và hai body phải **giống nhau** ngoài `correlationId`. Bất kỳ khác biệt nào giữa
"của người khác" và "không tồn tại" là một oracle đếm tài sản.

```bash
api "$A" PUT /cash-flow-entries/$LINKED '{"entryType":"INCOME","category":"RENTAL_INCOME","amount":"18000000.00","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-02-01","linkedAssetId":"'$ASSET_B'"}'   # 400; đọc lại $LINKED không đổi
api "$A" PUT /cash-flow-entries/$LINKED '{"entryType":"INCOME","category":"RENTAL_INCOME","amount":"18000000.00","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-02-01"}'   # 200, linkedAssetId: null
api "$A" PUT /cash-flow-entries/$LINKED '{"entryType":"INCOME","category":"RENTAL_INCOME","amount":"18000000.00","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-02-01","linkedAssetId":"'$ASSET_A'"}'   # 200, liên kết lại
api "$A" DELETE /assets/$ASSET_A      # 204
api "$A" GET /cash-flow-entries/$LINKED   # 200, entry còn, linkedAssetId: null
```

Dòng cuối là FK `ON DELETE SET NULL` đang làm việc: xóa tài sản không xóa khoản thu nhập ghi từ nó, chỉ
bỏ liên kết. Hậu quả về sản phẩm thuộc #104.

### P. Liên kết công nợ (case 93–98)

Chạy ngay sau mục H. Số case theo thứ tự thêm vào plan, không theo thứ tự chạy.

```bash
api "$A" POST /cash-flow-entries '{"entryType":"EXPENSE","category":"INTEREST_PAYMENT","amount":"17000000.00","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-05","linkedLiabilityId":"'$LOAN_A'","notes":"Lãi vay mua nhà"}'   # 93: 201, echo linkedLiabilityId, linkedAssetId null; giữ id là $INTEREST
api "$A" POST /cash-flow-entries '{"entryType":"EXPENSE","category":"INTEREST_PAYMENT","amount":"17000000.00","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-05","linkedLiabilityId":"'$LOAN_B'"}'   # 94: 400
api "$A" POST /cash-flow-entries '{"entryType":"EXPENSE","category":"INTEREST_PAYMENT","amount":"17000000.00","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-05","linkedLiabilityId":"00000000-0000-4000-8000-000000000000"}'   # 95: 400, giống 94
api "$A" POST /cash-flow-entries '{"entryType":"EXPENSE","category":"INTEREST_PAYMENT","amount":"17000000.00","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-05","linkedAssetId":"'$CAR_A'","linkedLiabilityId":"'$LOAN_A'"}'   # 96: 400
```

Case 94 và 95 phải `validation-failed` với `{field: linkedLiabilityId, message: "must reference one of the
signed-in user's liabilities"}`, hai body giống nhau ngoài `correlationId`. Case 96 phải có **đúng một**
entry, `linkedLiabilityId` / `must not be combined with linkedAssetId`, không kèm lỗi ownership nào.

```bash
api "$A" PUT /cash-flow-entries/$INTEREST '{"entryType":"EXPENSE","category":"INTEREST_PAYMENT","amount":"17000000.00","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-05","linkedAssetId":"'$CAR_A'"}'   # 97: 200, linkedAssetId = xe, linkedLiabilityId null
api "$A" PUT /cash-flow-entries/$INTEREST '{"entryType":"EXPENSE","category":"INTEREST_PAYMENT","amount":"17000000.00","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-05","linkedLiabilityId":"'$LOAN_A'"}'   # liên kết lại
api "$A" DELETE /liabilities/$LOAN_A          # 204
api "$A" GET /cash-flow-entries/$INTEREST     # 98: 200, entry còn, linkedLiabilityId null
```

### I. Liệt kê (case 54–56)

```bash
api "$A" GET /cash-flow-entries
api "$A" GET /cash-flow-entries
api "$B" GET /cash-flow-entries
```

Danh sách của A phải theo `effectiveDate` giảm dần rồi `createdAt` giảm dần; chạy hai lần và so, thứ tự
phải y hệt. Của B phải `[]` với `200`, không bao giờ `404`.

### J. Ownership (case 57–61)

```bash
api "$A" GET /cash-flow-entries/$SALARY     # 200
api "$B" GET /cash-flow-entries/$SALARY     # 404
api "$B" GET /cash-flow-entries/00000000-0000-4000-8000-000000000000   # 404
api "$B" PUT /cash-flow-entries/$SALARY '{"entryType":"EXPENSE","category":"OTHER_EXPENSE","amount":"1.00","currency":"VND","timing":"ONE_TIME","effectiveDate":"2026-09-10"}'   # 404
api "$B" DELETE /cash-flow-entries/$SALARY  # 404
```

So case 58 và 59: giống nhau ở `type`, `title`, `status`, `detail`, khác chỉ ở `instance` — request URI,
theo định nghĩa của RFC 9457 — và `correlationId`. Sau đó A đọc lại `$SALARY`: write bị từ chối không đổi gì.

### K. Sửa (case 62–66)

```bash
api "$A" PUT /cash-flow-entries/$SALARY '{"entryType":"INCOME","category":"SALARY","amount":"48000000.00","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-01","notes":"Lương tháng sau tăng lương"}'   # 200, updatedAt tiến, createdAt giữ
api "$A" PUT /cash-flow-entries/$SALARY '{"entryType":"EXPENSE","category":"LIVING_COSTS","amount":"48000000.00","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-01","notes":"Đổi loại"}'   # 200, cả entryType và category đổi
api "$A" PUT /cash-flow-entries/$SALARY '{"entryType":"INCOME","category":"SALARY","amount":"48000000.00","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-01"}'   # 200, notes: null
api "$A" PUT /cash-flow-entries/$SALARY '{"entryType":"INCOME","category":"HOUSING","amount":"48000000.00","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-01"}'   # 400 category
api "$A" PUT /cash-flow-entries/$SALARY '{"id":"11111111-1111-4111-8111-111111111111","userId":"'7b41e590-3c82-4f16-b0d7-59e1c4f3a842'","entryType":"INCOME","category":"SALARY","amount":"48000000.00","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-01"}'   # 200, id không đổi, hai member lạ bị bỏ qua
```

`PUT` là thay thế, không phải patch: member optional bỏ đi bị xóa. Member lạ bị Jackson bỏ qua lặng lẽ —
mặc định của Spring Boot — nên client không được đọc `200` như xác nhận giá trị nó gửi đã được lưu.

### L. Chứng thực (case 67–70)

```bash
curl -s -i http://127.0.0.1:8071/api/v1/cash-flow-entries           # 401
api "$EXPIRED" GET /cash-flow-entries                                 # 401
api "$FORGED" GET /cash-flow-entries                                  # 401
curl -s -i http://127.0.0.1:8071/api/v1/cash-flow-entries/$SALARY   # 401
```

Cả bốn `401` `unauthorized`, body không phân biệt được ngoài `correlationId`.

### M. Giao thức (case 71–76)

| Gửi | Kỳ vọng |
|---|---|
| `GET /cash-flow-entries/not-a-uuid` | `400` `malformed-request` |
| `PATCH /cash-flow-entries/$SALARY` | `405` `method-not-allowed` |
| `DELETE /cash-flow-entries` | `405` |
| body `text/plain` | `415` `unsupported-media-type` |
| `{"entryType": ` — JSON cụt | `400` `malformed-request` |
| `Accept: application/xml` | `406` `not-acceptable` |

Mọi case phải là `application/problem+json`.

### N. Xóa (case 77–81)

```bash
api "$A" DELETE /cash-flow-entries/$SALARY   # 204, không body
api "$A" GET /cash-flow-entries/$SALARY      # 404
api "$A" DELETE /cash-flow-entries/$SALARY   # 404
api "$A" GET /cash-flow-entries              # 200, ít đi một dòng
```

Case đồng thời, vì đó là thứ một nút xác nhận bị nhấn đôi tạo ra:

```bash
ID=<một entry A sở hữu>
for i in 1 2 3 4; do api "$A" DELETE "/cash-flow-entries/$ID" -o /dev/null -w "%{http_code}\n" & done; wait
```

Đúng một `204` và ba `404`. **Bất kỳ `500` nào là regression.** Lặp mười vòng, không phải một: đây là race.

### O. Trong PostgreSQL (case 82–92)

```bash
docker exec -i rikkaus-pg-issue-71 psql -U rikkaus -d rikkaus -c "SELECT column_name, data_type, numeric_precision, numeric_scale FROM information_schema.columns WHERE table_schema='wealth' AND table_name='cash_flow_entries' ORDER BY ordinal_position;"
docker exec -i rikkaus-pg-issue-71 psql -U rikkaus -d rikkaus -c "SELECT category, amount, currency FROM wealth.cash_flow_entries ORDER BY amount DESC LIMIT 3;"
docker exec -i rikkaus-pg-issue-71 psql -U rikkaus -d rikkaus -c "SELECT user_id, count(*) FROM wealth.cash_flow_entries GROUP BY user_id;"
```

`amount` phải là `numeric(19,2)`; số VND lớn phải nguyên, không mũ, không làm tròn.

Bảng phải từ chối điều API từ chối, bỏ qua ứng dụng hoàn toàn. Mỗi probe phải raise đúng tên constraint
và không chèn gì:

```bash
docker exec -i rikkaus-pg-issue-71 psql -U rikkaus -d rikkaus -c "INSERT INTO wealth.cash_flow_entries (id,user_id,entry_type,category,amount,currency,timing,effective_date,created_at,updated_at) VALUES (gen_random_uuid(),'0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93','TRANSFER','SALARY',1,'VND','ONE_TIME','2026-09-10',now(),now());"
```

| Probe | Constraint |
|---|---|
| `entry_type = 'TRANSFER'` | `cash_flow_entries_category_matches_entry_type` — mọi category đều thuộc `INCOME` hoặc `EXPENSE`, nên một loại lạ luôn vi phạm cả constraint ghép cặp, và PostgreSQL báo constraint đó trước theo thứ tự tên; `cash_flow_entries_entry_type_is_known` vẫn có mặt như phát biểu tường minh |
| `INCOME` + `HOUSING` | `cash_flow_entries_category_matches_entry_type` |
| `amount = 0` | `cash_flow_entries_amount_is_positive` |
| `currency = 'EUR'` | `cash_flow_entries_currency_is_supported` |
| `ONE_TIME` kèm `recurrence = 'MONTHLY'` | `cash_flow_entries_recurrence_matches_timing` |
| `RECURRING` không `recurrence` | `cash_flow_entries_recurrence_matches_timing` |
| `end_date` `2026-09-04` với `effective_date` `2026-09-05` | `cash_flow_entries_end_date_not_before_effective_date` |
| `linked_asset_id` không tồn tại | `cash_flow_entries_linked_asset_id_fkey` |
| 99: cả `linked_asset_id` và `linked_liability_id` có giá trị | `cash_flow_entries_links_at_most_one_record` |
| 100: `linked_liability_id` không tồn tại | `cash_flow_entries_linked_liability_id_fkey` |

`SELECT count(*)` sau cùng phải bằng trước đó.

## Dọn dẹp

```bash
pkill -f rikkaus-wealth-api-0.0.1-SNAPSHOT.jar
```

```bash
docker rm -f rikkaus-pg-issue-71
```

## Tài liệu này không bao gồm

- Đăng nhập Google thật. Token được mint; filter và verifier là bản production, nhưng handoff Google không
  được chạy.
- Hai lần sửa đồng thời cùng một entry: người ghi sau thắng, không báo xung đột — không có row version,
  là quyết định sản phẩm về xung đột sửa chứ không phải thiếu sót.
- Client mobile (#70), expansion định kỳ (#74), báo cáo tháng (#77), forecast (#80).
