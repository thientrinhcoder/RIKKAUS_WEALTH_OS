# Hướng dẫn test thủ công — lịch kỳ của dòng tiền

Route `GET /api/v1/cash-flow-occurrences` (issue #74) ship **không có automated test**, theo chỉ đạo của
Product Owner. Tài liệu này là phần thay thế: chạy lại nó là cách duy nhất một regression trong thuật toán
mở rộng lịch bị phát hiện. Kết quả của lần chạy đi kèm implementation ghi ở
[`plans/reports/manual-verification-261114-1843-GH-74-recurring-schedule-expansion.md`](../plans/reports/manual-verification-261114-1843-GH-74-recurring-schedule-expansion.md).
Danh sách scenario gốc, viết trước khi code, ở
[`plans/261114-1843-GH-74-recurring-schedule-expansion/phase-01-test-scenarios.md`](../plans/261114-1843-GH-74-recurring-schedule-expansion/phase-01-test-scenarios.md);
số case ở đây là số case ở đó, và **ngày kỳ vọng của từng case nằm ở bảng đó**.

Quy tắc biên đang được kiểm chứng ở mục "Cash-flow occurrences" của
[`api-contract-conventions.md`](./api-contract-conventions.md).

## Chuẩn bị

Mọi lệnh chạy từ thư mục gốc repo, có Docker daemon, `curl`, `jq`.

### 1. PostgreSQL dùng một lần

```bash
docker run -d --name rikkaus-pg-issue-74 -p 127.0.0.1:5474:5432 -e POSTGRES_DB=rikkaus -e POSTGRES_USER=rikkaus -e POSTGRES_PASSWORD=rikkaus-local-only postgres:18.6-alpine3.24
```

### 2. API

JDK 25 bắt buộc. `spring-boot:run` thay cho jar đóng gói, cùng mã nguồn và classpath.

```bash
DB_URL=jdbc:postgresql://localhost:5474/rikkaus JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./services/api/mvnw -f services/api/pom.xml spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local --server.port=8074"
```

Log phải có `now at version v9` (hoặc cao hơn nếu sau này có migration mới): #74 không thêm migration.

### 3. Hai người dùng và token

Chèn user A và B, mint token A, B, hết hạn và sai chữ ký đúng như mục 3–4 của
[`manual-test-guide-cash-flow-entries.md`](./manual-test-guide-cash-flow-entries.md), thay tên container
thành `rikkaus-pg-issue-74`. Rồi:

```bash
export A=<token A> B=<token B> EXPIRED=<expired> FORGED=<forged>
```

```bash
api() { curl -s -H "Authorization: Bearer $1" -H 'Content-Type: application/json' -X "$2" "http://127.0.0.1:8074/api/v1$3" ${4:+-d "$4"}; }
occ() { curl -s "http://127.0.0.1:8074/api/v1/cash-flow-occurrences?from=$2&to=$3" -H "Authorization: Bearer $1"; }
pick() { jq -c --arg id "$1" '[.[] | select(.sourceEntryId == $id) | [.occurrenceDate, .sequence, .shiftedToMonthEnd]]'; }
```

`pick` in mỗi kỳ của một entry thành `[ngày, sequence, shiftedToMonthEnd]`.

### 4. Chín entry của A

```bash
E1=$(api "$A" POST /cash-flow-entries '{"entryType":"INCOME","category":"SALARY","amount":"45000000","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-31","notes":"Lương"}' | jq -r .id)
E2=$(api "$A" POST /cash-flow-entries '{"entryType":"EXPENSE","category":"HOUSING","amount":"18000000","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2025-12-30","endDate":"2026-06-30","notes":"Tiền thuê nhà"}' | jq -r .id)
E3=$(api "$A" POST /cash-flow-entries '{"entryType":"EXPENSE","category":"LIVING_COSTS","amount":"1200000","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-15","endDate":"2026-06-14","notes":"Phí gửi xe chung cư"}' | jq -r .id)
E4=$(api "$A" POST /cash-flow-entries '{"entryType":"EXPENSE","category":"EDUCATION","amount":"12500000","currency":"VND","timing":"RECURRING","recurrence":"QUARTERLY","effectiveDate":"2025-11-30","notes":"Học phí"}' | jq -r .id)
E5=$(api "$A" POST /cash-flow-entries '{"entryType":"INCOME","category":"SALARY","amount":"60000000","currency":"VND","timing":"RECURRING","recurrence":"YEARLY","effectiveDate":"2026-02-13","notes":"Thưởng Tết"}' | jq -r .id)
E6=$(api "$A" POST /cash-flow-entries '{"entryType":"EXPENSE","category":"INSURANCE","amount":"24000000","currency":"VND","timing":"RECURRING","recurrence":"YEARLY","effectiveDate":"2028-02-29","notes":"Phí bảo hiểm nhân thọ"}' | jq -r .id)
E7=$(api "$A" POST /cash-flow-entries '{"entryType":"EXPENSE","category":"TAX_AND_FEES","amount":"3200000","currency":"VND","timing":"RECURRING","recurrence":"QUARTERLY","effectiveDate":"2026-03-15","endDate":"2026-03-15","notes":"Phí bảo trì chung cư"}' | jq -r .id)
E8=$(api "$A" POST /cash-flow-entries '{"entryType":"EXPENSE","category":"HEALTHCARE","amount":"4650000","currency":"VND","timing":"ONE_TIME","effectiveDate":"2026-03-31","notes":"Khám sức khỏe định kỳ"}' | jq -r .id)
E9=$(api "$A" POST /cash-flow-entries '{"entryType":"INCOME","category":"DIVIDEND","amount":"150","currency":"USD","timing":"ONE_TIME","effectiveDate":"2026-04-01","notes":"Cổ tức VNM"}' | jq -r .id)
```

## Các case

Chạy theo thứ tự: case 31–32 sửa và xóa dữ liệu mà các case trước dựa vào.

### A–C. Ngày kỳ của từng entry (case 1–14)

Mỗi case là một lệnh `occ … | pick …`; so mảng in ra với cột kỳ vọng của phase 1.

```bash
occ "$A" 2026-01-01 2026-04-30 | pick "$E1"   # 1
occ "$A" 2027-12-01 2028-03-31 | pick "$E1"   # 2
occ "$A" 2026-02-01 2026-02-28 | pick "$E1"   # 3
occ "$A" 2026-03-01 2026-03-31 | pick "$E1"   # 4
occ "$A" 2025-11-01 2026-11-30 | pick "$E4"   # 5
occ "$A" 2026-12-01 2027-05-31 | pick "$E4"   # 6
occ "$A" 2026-01-01 2027-12-31 | pick "$E5"   # 7
occ "$A" 2028-02-01 2030-01-31 | pick "$E6"   # 8
occ "$A" 2031-03-01 2033-02-28 | pick "$E6"   # 9
occ "$A" 2026-01-01 2026-12-31 | pick "$E2"   # 10
occ "$A" 2026-06-30 2026-07-31 | pick "$E2"   # 11
occ "$A" 2026-01-01 2026-12-31 | pick "$E3"   # 12
occ "$A" 2026-01-01 2026-12-31 | pick "$E7"   # 13
occ "$A" 2027-01-01 2028-02-28 | pick "$E6"   # 14
```

Ví dụ case 1 đúng: `[["2026-01-31",1,false],["2026-02-28",2,true],["2026-03-31",3,false],["2026-04-30",4,true]]`.

### D. Biên của khoảng (case 15–21)

```bash
occ "$A" 2026-01-31 2026-02-27 | pick "$E1"                 # 15: chỉ 2026-01-31
occ "$A" 2026-01-01 2026-03-31 | jq 'length'                # 16: 13
occ "$A" 2026-02-01 2026-03-30 | pick "$E1"                 # 17: chỉ 2026-02-28
```

Case 18, hai quý so với nửa năm — ba số phải là `13`, `10`, `23`, và hai dòng cuối phải là `0` và `true`:

```bash
occ "$A" 2026-01-01 2026-03-31 > q1.json; occ "$A" 2026-04-01 2026-06-30 > q2.json; occ "$A" 2026-01-01 2026-06-30 > h1.json
jq length q1.json q2.json h1.json
jq -n --slurpfile a q1.json --slurpfile b q2.json '[$a[0][], $b[0][]] | map([.sourceEntryId,.occurrenceDate]) | length - (unique | length)'
jq -n --slurpfile a q1.json --slurpfile b q2.json --slurpfile h h1.json '([$a[0][],$b[0][]] | map([.sourceEntryId,.occurrenceDate]) | sort) == ($h[0] | map([.sourceEntryId,.occurrenceDate]) | sort)'
```

Case 19, mười hai tháng so với cả năm — tổng phải là `31`, bằng `occ "$A" 2026-01-01 2026-12-31 | jq length`,
và không kỳ nào có tháng khác tháng của khoảng:

```bash
for m in 01 02 03 04 05 06 07 08 09 10 11 12; do last=$(date -j -v+1m -v-1d -f %Y-%m-%d 2026-$m-01 +%d); occ "$A" 2026-$m-01 2026-$m-$last | jq --arg m "$m" '[length, ([.[] | select(.occurrenceDate[5:7] != $m)] | length)]' -c; done
```

```bash
occ "$A" 2026-02-28 2026-02-28 | jq -c '[.[] | [.sourceEntryId, .sequence, .shiftedToMonthEnd]]'   # 20: E1, E2, E4, cả ba dời
occ "$A" 2020-01-01 2020-12-31                                                                     # 21: []
```

### E–F. Một lần, hình dạng, truy nguồn, xác định (case 22–27)

```bash
occ "$A" 2026-03-01 2026-03-31 | jq -c --arg id "$E8" '.[] | select(.sourceEntryId == $id)'   # 22
occ "$A" 2026-04-01 2026-04-30                                                                 # 23: E9 có "amount":150.00,"currency":"USD"; không có E8
occ "$A" 2026-01-01 2026-01-31 | jq -c --arg id "$E1" '.[] | select(.sourceEntryId == $id)'   # 24: đúng 9 member
api "$A" GET /cash-flow-entries/$E1 | jq -c '{id, amount, category, entryType}'                # 25
occ "$A" 2026-01-01 2026-12-31 | jq '[.[] | [.occurrenceDate, .sourceEntryId]] | . == sort'    # 26: true
occ "$A" 2026-01-01 2026-12-31 | shasum -a 256; occ "$A" 2026-01-01 2026-12-31 | shasum -a 256 # 27: hai dòng giống nhau
```

### G–H. Ownership và tính khi đọc (case 28–32)

```bash
occ "$B" 2026-01-01 2026-12-31                                                                  # 28: []
EB=$(api "$B" POST /cash-flow-entries '{"entryType":"INCOME","category":"SALARY","amount":"30000000","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-31","notes":"Lương của Bích"}' | jq -r .id)
occ "$B" 2026-01-01 2026-12-31 | jq --arg id "$EB" '[length, all(.sourceEntryId == $id)]' -c   # 29: [12,true]
occ "$A" 2026-01-01 2026-12-31 | jq --arg id "$EB" '[length, any(.sourceEntryId == $id)]' -c   # 30: [31,false]
api "$A" PUT /cash-flow-entries/$E2 '{"entryType":"EXPENSE","category":"HOUSING","amount":"18000000","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2025-12-30","endDate":"2026-04-30","notes":"Tiền thuê nhà"}' > /dev/null
occ "$A" 2026-01-01 2026-12-31 | pick "$E2"                                                      # 31: 4 kỳ, cuối 2026-04-30 s5
api "$A" DELETE /cash-flow-entries/$E3
occ "$A" 2026-01-01 2026-12-31 | jq length                                                       # 32: 24
```

### I. Input sai (case 33–43)

```bash
bad() { curl -s "http://127.0.0.1:8074/api/v1/cash-flow-occurrences$1" -H "Authorization: Bearer $A" | jq -c '[.status, .type, .errors]'; }
```

```bash
bad '?to=2026-03-31'                                    # 33
bad '?from=2026-01-01'                                  # 34
bad ''                                                  # 35
bad '?from=&to=2026-03-31'                              # 36
bad '?from=2026-04-01&to=2026-03-31'                    # 37
bad '?from=31/01/2026&to=2026-03-31'                    # 38
bad '?from=2026-01-01&to=2026-02-29'                    # 39
bad '?from=2026-1-5&to=2026-03-31'                      # 40
bad '?from=2026-01-01T00:00:00%2B07:00&to=2026-03-31'   # 41
bad '?from=2026-01-01&from=2026-02-01&to=2026-03-31'    # 41b
bad '?from=2026-01-01&to=2028-01-01'                    # 42
bad '?from=abc&to='                                     # 43
occ "$A" 2026-01-01 2027-12-31 | jq length              # 42b: một con số, tức 200 với mảng kỳ
```

Mỗi case phải in `400`, `urn:rikkaus:problem:validation-failed` và đúng `errors` của phase 1. Với case 38 và 39,
body đầy đủ không được chứa lại `31/01/2026` hay `2026-02-29`.

### J. Xác thực, method, media type (case 44–48)

```bash
Q='http://127.0.0.1:8074/api/v1/cash-flow-occurrences?from=2026-01-01&to=2026-03-31'
curl -s "$Q" | jq -c 'del(.correlationId)'                                      # 44
curl -s "$Q" -H "Authorization: Bearer $EXPIRED" | jq -c 'del(.correlationId)'  # 45: giống 44
curl -s "$Q" -H "Authorization: Bearer $FORGED" | jq -c 'del(.correlationId)'   # 46: giống 44
curl -s -i -X POST "$Q" -H "Authorization: Bearer $A" | grep -iE '^HTTP|^allow'  # 47: 405, Allow: GET
curl -s -i -X DELETE "$Q" -H "Authorization: Bearer $A" | grep -iE '^HTTP|^allow'
curl -s -i "$Q" -H "Authorization: Bearer $A" -H 'Accept: application/xml' | head -1   # 48: 406
```

### K. Múi giờ, contract, schema (case 49–51)

Case 49: ghi `occ "$A" 2026-01-01 2026-12-31 | shasum -a 256`, dừng API, khởi động lại với
`-Dspring-boot.run.jvmArguments="-Duser.timezone=Pacific/Pago_Pago"` thêm vào lệnh ở mục 2 (log sẽ in giờ
`-11:00`), gọi lại: hai hash phải giống nhau.

```bash
jq -c '.paths["/api/v1/cash-flow-occurrences"] | [keys, [.get.parameters[] | [.name, .required, .schema.format]]]' services/api/openapi/openapi.json   # 50
docker exec rikkaus-pg-issue-74 psql -U rikkaus -d rikkaus -tAc "SELECT string_agg(tablename, ',' ORDER BY tablename) FROM pg_tables WHERE schemaname='wealth'; SELECT max(version::int) FROM flyway_schema_history WHERE success;"   # 51
```

## Dọn dẹp

Dừng API (Ctrl+C), rồi:

```bash
docker rm -f rikkaus-pg-issue-74
```
