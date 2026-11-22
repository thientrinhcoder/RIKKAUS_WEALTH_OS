# Hướng dẫn test thủ công — dự báo dòng tiền ba tháng

Route `GET /api/v1/cash-flow-forecast` (issue #80) ship **không có automated test**, theo chỉ đạo của
Product Owner. Tài liệu này là phần thay thế: chạy lại nó là cách duy nhất một regression trong phép
chiếu bị phát hiện. Kết quả lần chạy đi kèm implementation ở
[`plans/reports/manual-verification-261122-1544-GH-80-three-month-forecast.md`](../plans/reports/manual-verification-261122-1544-GH-80-three-month-forecast.md).
Danh sách scenario viết trước khi code, cùng **bảng số dư tính tay**, ở
[`phase-01-test-scenarios.md`](../plans/261122-1544-GH-80-three-month-forecast/phase-01-test-scenarios.md);
số case ở đây là số case ở đó. Quy tắc đang được kiểm chứng ở mục "Cash-flow forecast" của
[`api-contract-conventions.md`](./api-contract-conventions.md).

**Ngày chạy quan trọng.** Kỳ vọng của phase 1 tính cho ngày **22/11/2026** (giờ Việt Nam): horizon mặc
định 12/2026–02/2027, khoảng không chiếu 23–30/11/2026. Chạy vào ngày khác thì horizon dịch theo; các
case 1, 3, 4, 46–48 cần đổi tháng, còn mọi case khác vẫn đúng nếu gọi với `from=2026-12` khi tháng đó
còn hợp lệ.

## Chuẩn bị

Mọi lệnh chạy từ thư mục gốc repo, có Docker daemon, `curl`, `jq`, `python3`.

### 1. PostgreSQL dùng một lần

```bash
docker run -d --name rikkaus-pg-issue-80 -p 127.0.0.1:5480:5432 -e POSTGRES_DB=rikkaus -e POSTGRES_USER=rikkaus -e POSTGRES_PASSWORD=rikkaus-local-only postgres:18.6-alpine3.24
```

### 2. API

```bash
DB_URL=jdbc:postgresql://localhost:5480/rikkaus JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./services/api/mvnw -f services/api/pom.xml spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local --server.port=8180"
```

Log phải có `now at version v12`: #80 không thêm migration.

### 3. Ba người dùng và token

```bash
docker exec -i rikkaus-pg-issue-80 psql -U rikkaus -d rikkaus -c "INSERT INTO wealth.users (id, google_subject, email, display_name, created_at, updated_at) VALUES ('0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93','google-sub-a','an.nguyen@example.com','An Nguyễn', now(), now()), ('7b41e590-3c82-4f16-b0d7-59e1c4f3a842','google-sub-b','bich.tran@example.com','Bích Trần', now(), now()), ('a3e9c5d2-4b17-4f86-9c20-6d8e1f7b3a55','google-sub-c','chi.le@example.com','Chi Lê', now(), now());"
```

Mint token A, B, token hết hạn và token ký sai bằng script ở mục 4 của
[`manual-test-guide-cash-flow-entries.md`](./manual-test-guide-cash-flow-entries.md) (cùng secret của
profile `local`, cùng bộ claim), thêm một dòng cho C với `sub` `a3e9c5d2-4b17-4f86-9c20-6d8e1f7b3a55`.
Không chép token vào file nào.

```bash
export A=<token A> B=<token B> C=<token C> EXPIRED=<expired> FORGED=<forged>
```

```bash
api() { curl -s -H "Authorization: Bearer $1" -H 'Content-Type: application/json' -X "$2" "http://127.0.0.1:8180/api/v1$3" ${4:+-d "$4"}; }
fc() { curl -s "http://127.0.0.1:8180/api/v1/cash-flow-forecast$2" -H "Authorization: Bearer $1"; }
items() { jq -c --arg id "$1" '[.months[].items[] | select(.sourceId == $id) | [.date, .sequence, .shiftedToMonthEnd]]'; }
excl() { jq -c --arg id "$1" '[.excludedSources[] | select(.sourceId == $id) | [.reason, .affectedMonths, .linkedEntryIds]]'; }
month() { jq -c --arg m "$1" '.months[] | select(.month == $m) | [.income, .expense, .net, .openingBalance, .closingBalance, .lowestBalance, .lowestBalanceDate, .partial]'; }
```

### 4. Dữ liệu

Mã bản ghi (AS1, E1, L1…) là mã ở bảng dữ liệu của phase 1.

```bash
api "$A" PUT /settings/exchange-rate '{"usdToVndRate":"25100","effectiveDate":"2026-09-12"}'
AS1=$(api "$A" POST /assets '{"name":"Tài khoản Vietcombank","category":"BANK_ACCOUNT","currentValue":"85000000","currency":"VND","valuationDate":"2026-11-20","liquidity":"HIGH"}' | jq -r .id)
AS2=$(api "$A" POST /assets '{"name":"Tiền mặt","category":"CASH","currentValue":"12000000","currency":"VND","valuationDate":"2026-11-20","liquidity":"HIGH"}' | jq -r .id)
AS3=$(api "$A" POST /assets '{"name":"Tài khoản USD","category":"BANK_ACCOUNT","currentValue":"2000","currency":"USD","valuationDate":"2026-11-20","liquidity":"HIGH"}' | jq -r .id)
AS4=$(api "$A" POST /assets '{"name":"Tiết kiệm kỳ hạn 6 tháng","category":"TERM_DEPOSIT","currentValue":"500000000","currency":"VND","valuationDate":"2026-11-20","liquidity":"HIGH"}' | jq -r .id)
L1=$(api "$A" POST /liabilities '{"name":"Vay mua nhà Vietcombank","category":"BANK_LOAN","outstandingBalance":"2150000000","currency":"VND","recurringPaymentAmount":"18500000","recurringPaymentFrequency":"MONTHLY","nextPaymentDate":"2026-12-15","maturityDate":"2041-06-15"}' | jq -r .id)
L2=$(api "$A" POST /liabilities '{"name":"Thẻ tín dụng Techcombank","category":"CREDIT_CARD_BALANCE","outstandingBalance":"9800000","currency":"VND","recurringPaymentAmount":"9800000","recurringPaymentFrequency":"MONTHLY","nextPaymentDate":"2027-01-25"}' | jq -r .id)
L3=$(api "$A" POST /liabilities '{"name":"Vay mua xe VPBank","category":"BANK_LOAN","outstandingBalance":"12400000","currency":"VND","recurringPaymentAmount":"6200000","recurringPaymentFrequency":"MONTHLY","nextPaymentDate":"2026-12-10","maturityDate":"2027-01-10"}' | jq -r .id)
L4=$(api "$A" POST /liabilities '{"name":"Vay người thân","category":"PERSONAL_LOAN","outstandingBalance":"50000000","currency":"VND","maturityDate":"2027-01-31"}' | jq -r .id)
L5=$(api "$A" POST /liabilities '{"name":"Vay sửa nhà BIDV","category":"BANK_LOAN","outstandingBalance":"168000000","currency":"VND","recurringPaymentAmount":"7000000","recurringPaymentFrequency":"MONTHLY","nextPaymentDate":"2026-12-20"}' | jq -r .id)
L6=$(api "$A" POST /liabilities '{"name":"Vay ký quỹ SSI","category":"MARGIN_LOAN","outstandingBalance":"80000000","currency":"VND"}' | jq -r .id)
L7=$(api "$A" POST /liabilities '{"name":"Vay mua đất Agribank","category":"BANK_LOAN","outstandingBalance":"900000000","currency":"VND","recurringPaymentAmount":"30000000","recurringPaymentFrequency":"QUARTERLY","nextPaymentDate":"2027-04-01"}' | jq -r .id)
E1=$(api "$A" POST /cash-flow-entries '{"entryType":"INCOME","category":"SALARY","amount":"45000000","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-05","notes":"Lương"}' | jq -r .id)
E2=$(api "$A" POST /cash-flow-entries '{"entryType":"EXPENSE","category":"LIVING_COSTS","amount":"25000000","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-01","notes":"Chi phí sinh hoạt"}' | jq -r .id)
E3=$(api "$A" POST /cash-flow-entries '{"entryType":"EXPENSE","category":"HOUSING","amount":"15000000","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-03-05","endDate":"2027-01-05","notes":"Tiền thuê nhà"}' | jq -r .id)
E4=$(api "$A" POST /cash-flow-entries '{"entryType":"EXPENSE","category":"EDUCATION","amount":"12500000","currency":"VND","timing":"RECURRING","recurrence":"QUARTERLY","effectiveDate":"2026-08-31","notes":"Học phí"}' | jq -r .id)
E5=$(api "$A" POST /cash-flow-entries '{"entryType":"INCOME","category":"SALARY","amount":"60000000","currency":"VND","timing":"RECURRING","recurrence":"YEARLY","effectiveDate":"2026-01-25","notes":"Thưởng Tết"}' | jq -r .id)
E6=$(api "$A" POST /cash-flow-entries '{"entryType":"EXPENSE","category":"INSURANCE","amount":"24000000","currency":"VND","timing":"RECURRING","recurrence":"YEARLY","effectiveDate":"2026-06-15","notes":"Phí bảo hiểm nhân thọ"}' | jq -r .id)
E7=$(api "$A" POST /cash-flow-entries '{"entryType":"INCOME","category":"DIVIDEND","amount":"150","currency":"USD","timing":"RECURRING","recurrence":"QUARTERLY","effectiveDate":"2026-12-15","notes":"Cổ tức"}' | jq -r .id)
E8=$(api "$A" POST /cash-flow-entries '{"entryType":"EXPENSE","category":"OTHER_EXPENSE","amount":"8500000","currency":"VND","timing":"ONE_TIME","effectiveDate":"2027-01-18","notes":"Sửa xe"}' | jq -r .id)
E9=$(api "$A" POST /cash-flow-entries "{\"entryType\":\"EXPENSE\",\"category\":\"DEBT_PRINCIPAL_PAYMENT\",\"amount\":\"7000000\",\"currency\":\"VND\",\"timing\":\"RECURRING\",\"recurrence\":\"MONTHLY\",\"effectiveDate\":\"2026-05-20\",\"linkedLiabilityId\":\"$L5\",\"notes\":\"Trả nợ vay sửa nhà\"}" | jq -r .id)
E10=$(api "$A" POST /cash-flow-entries '{"entryType":"EXPENSE","category":"OTHER_EXPENSE","amount":"32000000","currency":"VND","timing":"ONE_TIME","effectiveDate":"2026-10-02","notes":"Mua laptop"}' | jq -r .id)
BS1=$(api "$B" POST /assets '{"name":"Tài khoản ACB","category":"BANK_ACCOUNT","currentValue":"8000000","currency":"VND","valuationDate":"2026-11-20","liquidity":"HIGH"}' | jq -r .id)
api "$B" POST /cash-flow-entries '{"entryType":"INCOME","category":"SALARY","amount":"30000000","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-10","notes":"Lương của Bích"}'
api "$B" POST /cash-flow-entries '{"entryType":"EXPENSE","category":"HOUSING","amount":"12000000","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-01","notes":"Tiền thuê nhà"}'
api "$B" POST /cash-flow-entries '{"entryType":"EXPENSE","category":"LIVING_COSTS","amount":"14000000","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-03","notes":"Chi phí sinh hoạt"}'
api "$B" POST /cash-flow-entries '{"entryType":"EXPENSE","category":"EDUCATION","amount":"45000000","currency":"VND","timing":"ONE_TIME","effectiveDate":"2027-01-08","notes":"Học phí khóa MBA"}'
CS1=$(api "$C" POST /assets '{"name":"Tài khoản USD HSBC","category":"BANK_ACCOUNT","currentValue":"1200","currency":"USD","valuationDate":"2026-11-20","liquidity":"HIGH"}' | jq -r .id)
api "$C" POST /cash-flow-entries '{"entryType":"INCOME","category":"SALARY","amount":"25000000","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-05","notes":"Lương của Chi"}'
CE2=$(api "$C" POST /cash-flow-entries '{"entryType":"INCOME","category":"DIVIDEND","amount":"150","currency":"USD","timing":"RECURRING","recurrence":"QUARTERLY","effectiveDate":"2026-12-15","notes":"Cổ tức FPT"}' | jq -r .id)
CL1=$(api "$C" POST /liabilities '{"name":"Vay tín chấp Shinhan","category":"PERSONAL_LOAN","outstandingBalance":"38700000","currency":"VND","recurringPaymentAmount":"4300000","recurringPaymentFrequency":"MONTHLY","nextPaymentDate":"2026-10-28"}' | jq -r .id)
```

## Các case

Chạy theo thứ tự: case 31–32 xóa dữ liệu, case 35 khai tỷ giá cho C. So từng kết quả với cột kỳ vọng của
phase 1.

### A. Horizon (1–4)

```bash
fc "$A" | jq -c '[.horizon, .assumptions.asOfDate, .assumptions.notProjected]'      # 1
cmp <(fc "$A") <(fc "$A" '?from=2026-12') && echo giống                            # 2
fc "$A" '?from=2027-01' | jq -c '[.horizon, .assumptions.notProjected, .openingBalance.amount, .openingBalance.appliedOn]'  # 3
fc "$A" '?from=2027-11' | jq -c .horizon                                           # 4
```

### B–C. Entry và công nợ (5–20)

```bash
fc "$A" > a.json
for x in E1 E4 E5 E3 E6 E7 E8 E10 E2 L1 L2 L3 L4 L6 L5 E9 L7; do echo "$x $(items "${!x}" < a.json) $(excl "${!x}" < a.json)"; done
jq -c --arg id "$E7" '.months[].items[] | select(.sourceId == $id) | [.amount, .currency, .amountVnd]' a.json   # 10
jq -c '[.months[].partial]' a.json                                                                                 # 17
```

### D. Tổng và tín hiệu của A (21–29)

```bash
jq -c '.openingBalance' a.json                                                     # 21
for m in 2026-12 2027-01 2027-02; do month "$m" < a.json; done                     # 22–24
jq -c '[.minimumBalance, .partial]' a.json                                         # 25
jq -c '[.months[] | [(.items | length), ([.items[] | select(.direction=="INCOME") | .amountVnd] | add) == .income, ([.items[] | select(.direction=="EXPENSE") | .amountVnd] | add) == .expense]]' a.json  # 26
jq -c '[.months[].items[] | select(.date == "2026-12-15" or .date == "2027-01-25") | [.date, .sourceType]]' a.json  # 27
jq -c '[.excludedSources[] | [.sourceType, .sourceId, .reason]]' a.json            # 28
jq -c '.assumptions' a.json                                                        # 29
```

### E. Truy vết và dữ liệu sống (30–32)

```bash
jq -r '.months[].items[] | "\(.sourceType) \(.sourceId) \(.amount)"' a.json | while read t id amt; do p=$([ "$t" = LIABILITY ] && echo liabilities || echo cash-flow-entries); api "$A" GET "/$p/$id" | jq -r --arg a "$amt" '"\(.id) \((.amount // .recurringPaymentAmount) == ($a|tonumber))"'; done   # 30
jq -r '.openingBalance.sources[] | .sourceId' a.json | while read id; do api "$A" GET "/assets/$id" | jq -c '[.id, .currentValue, .currency]'; done
api "$A" DELETE "/cash-flow-entries/$E5"; fc "$A" | tee a31.json | items "$E5"; month 2027-01 < a31.json; month 2027-02 < a31.json; jq -c .minimumBalance a31.json   # 31
api "$A" DELETE "/cash-flow-entries/$E9"; fc "$A" | tee a32.json | jq -c --arg id "$L5" '[.months[].items[] | select(.sourceId == $id) | [.date, .sequence, .sourceType]]'; excl "$L5" < a32.json   # 32
```

### F. Chưa khai tỷ giá (33–35)

```bash
fc "$C" | jq -c '[.assumptions.exchangeRate, .openingBalance, .excludedSources, .months[0].partial, .months[0].income, .months[0].expense, .minimumBalance.status, .partial]'  # 33
fc "$C" | items "$CL1"                                                             # 34
api "$C" PUT /settings/exchange-rate '{"usdToVndRate":"25300","effectiveDate":"2026-11-20"}'
fc "$C" | jq -c '[.openingBalance.amount, [.months[].items[] | select(.currency=="USD") | .amountVnd], .excludedSources, .partial, [.months[].closingBalance], .minimumBalance]'  # 35
```

### G. Tháng âm và cô lập (36–41)

```bash
fc "$B" > b.json; for m in 2026-12 2027-01 2027-02; do month "$m" < b.json; done   # 36–38
jq -c .minimumBalance b.json                                                       # 39
jq -c '[[.months[].items[].sourceId] + [.openingBalance.sources[].sourceId] | unique[], .openingBalance.sources]' b.json  # 40: chỉ id của B
curl -s -o /dev/null -w '%{http_code}\n' "http://127.0.0.1:8180/api/v1/cash-flow-entries/$E1" -H "Authorization: Bearer $B"  # 41: 404
```

### H–I. Tham số, chứng thực, method (42–53)

```bash
for f in 2026-13 2026-12-01 abc '' 2026-11 2026-10 2027-12; do echo "$f → $(fc "$A" "?from=$f" | jq -c '[.status, .errors]')"; done   # 42–48
curl -s http://127.0.0.1:8180/api/v1/cash-flow-forecast | jq -c '[.status, .type]'                                  # 49
for t in "$EXPIRED" "$FORGED"; do fc "$t" | jq -c '[.status, .type]'; done                                          # 50–51
curl -s -X POST http://127.0.0.1:8180/api/v1/cash-flow-forecast -H "Authorization: Bearer $A" | jq -c '[.status, .type]'  # 52
curl -s -o /dev/null -w '%{http_code}\n' -H 'Accept: application/xml' http://127.0.0.1:8180/api/v1/cash-flow-forecast -H "Authorization: Bearer $A"  # 53: 406
```

### J. Xác định (54–55)

```bash
fc "$A" | shasum -a 256; fc "$A" | shasum -a 256                                   # 54: hai dòng giống
grep -o -E '"amount":45000000\.00|"usdToVndRate":25100\.000000|"referenceThreshold":78166666\.67' a.json | sort -u   # 55: ba chuỗi
```

## Dọn

Dừng API (Ctrl-C) rồi:

```bash
docker rm -f rikkaus-pg-issue-80
```
