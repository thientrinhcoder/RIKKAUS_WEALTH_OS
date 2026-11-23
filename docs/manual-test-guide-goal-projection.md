# Hướng dẫn test thủ công — dự phóng mục tiêu và trạng thái

Hai route `GET /api/v1/goal-projections` và `GET /api/v1/goal-projections/{goalId}` (issue #89) ship
**không có automated test**, theo chỉ đạo của Product Owner. Tài liệu này là phần thay thế: chạy lại nó
là cách duy nhất một regression trong phương pháp dự phóng bị phát hiện. Kết quả lần chạy đi kèm
implementation ở
[`plans/reports/manual-verification-261123-1537-GH-89-goal-projection-status.md`](../plans/reports/manual-verification-261123-1537-GH-89-goal-projection-status.md).
Danh sách scenario viết trước khi code, cùng **bảng kỳ vọng tính trước** cho từng goal, ở
[`phase-01-test-scenarios.md`](../plans/261122-1842-GH-89-goal-projection-status/phase-01-test-scenarios.md);
số case ở đây là số case ở đó. Phương pháp đang được kiểm chứng ở mục "Goal projection" của
[`api-contract-conventions.md`](./api-contract-conventions.md).

**Ngày chạy quan trọng.** Kỳ vọng của phase 1 tính cho ngày tham chiếu **22/11/2026** (giờ Việt Nam),
tháng góp đầu tiên 12/2026. Không có tham số `asOf`. Chạy vào ngày khác thì dời **mọi ngày đích** đi đúng
số tháng chênh (ví dụ chạy trong tháng 01/2027 thì cộng hai tháng): `n`, mọi số tiền và trạng thái giữ
nguyên; riêng `asOfDate`, `FIRST_CONTRIBUTION_MONTH` và `daysPastTargetDate` (P17–P19) đổi theo ngày.

## Chuẩn bị

Mọi lệnh chạy từ thư mục gốc repo, có Docker daemon, `curl`, `jq`, `python3`.

### 1. PostgreSQL dùng một lần

Không dùng volume `rikkaus-postgres-data` dùng chung, không dùng cổng 5432.

```bash
docker run -d --name rikkaus-pg-issue-89 -p 127.0.0.1:5489:5432 -e POSTGRES_DB=rikkaus -e POSTGRES_USER=rikkaus -e POSTGRES_PASSWORD=rikkaus-local-only postgres:18.6-alpine3.24
```

### 2. API

API cần JDK 25; JDK mặc định của máy thường là 21.

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./services/api/mvnw -f services/api/pom.xml -DskipTests package
```

```bash
DB_URL=jdbc:postgresql://localhost:5489/rikkaus /opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home/bin/java -jar services/api/target/rikkaus-wealth-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=local --server.port=8089
```

Log khởi động phải có `Started RikkausWealthApplication`. Ngưỡng trạng thái được bind lúc khởi động;
thiếu hoặc sai thứ tự thì app dừng ngay (P43).

### 3. Hai người dùng và token

Tài khoản sinh từ Google sign-in nên chèn thẳng:

```bash
docker exec -i rikkaus-pg-issue-89 psql -U rikkaus -d rikkaus -c "INSERT INTO wealth.users (id, google_subject, email, display_name, created_at, updated_at) VALUES ('0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93','google-sub-a','an.nguyen@example.com','An Nguyen', now(), now()), ('7b41e590-3c82-4f16-b0d7-59e1c4f3a842','google-sub-b','bich.tran@example.com','Bich Tran', now(), now());"
```

Mint token A, B, hết hạn và ký sai khóa bằng đoạn Python ở mục "4. Access tokens" của
[`manual-test-guide-goals.md`](./manual-test-guide-goals.md) (cùng secret phát triển của profile `local`,
cùng claim `AccessTokenService` phát hành), rồi export vào biến, không in token ra log hay tài liệu:

```bash
export A=<token A> B=<token B> EXPIRED=<token hết hạn> FORGED=<token ký sai khóa>
```

```bash
api() { curl -s -i -H "Authorization: Bearer $1" -H 'Content-Type: application/json' -X "$2" "http://127.0.0.1:8089/api/v1$3" ${4:+-d "$4"}; }
```

### 4. Dữ liệu

Tạo 19 goal của A và (ở P29) một goal của B qua `POST /goals`, đúng bảng "Dữ liệu" của phase 1. Một dòng
mẫu; giữ lại `id` của từng goal theo mã (`STD`, `F1`…):

```bash
api "$A" POST /goals '{"name":"Mua căn hộ","type":"HOME_PURCHASE","targetAmount":"3500000000.00","currentAllocatedAmount":"600000000.00","targetDate":"2030-12-31","plannedMonthlyContribution":"25000000.00","expectedAnnualReturnPercent":"7.00"}'
```

Trường ghi `—` trong bảng thì **bỏ hẳn** khỏi body (không gửi `0`): `null` và `0` là hai case khác nhau.

API từ chối ngày đích không sau hôm nay, nên E6, E7, E8 tạo với ngày tương lai (31/10/2027, 30/09/2027,
15/11/2027) rồi đổi ngày trong database:

```bash
docker exec -i rikkaus-pg-issue-89 psql -U rikkaus -d rikkaus -c "UPDATE wealth.goals SET target_date = '2026-10-31' WHERE id = '<E6>'; UPDATE wealth.goals SET target_date = '2026-09-30' WHERE id = '<E7>'; UPDATE wealth.goals SET target_date = '2026-11-15' WHERE id = '<E8>';"
```

## Các case

Đọc status line và body. So mọi số với bảng "Kỳ vọng tính trước" của phase 1 **theo giá trị thập phân**,
và với các case về số 0 thì theo **chữ trên dây** (`0.00`, không phải `null`, và ngược lại). Mọi body lỗi
phải là `application/problem+json`.

```bash
api "$A" GET /goal-projections/<F2> | tail -1 | jq '{timeline, remainingMonths, completionPercent, requiredMonthlyContribution, contributionCoveragePercent, status, projectedAtTargetDate, calculation, unavailableReasons}'
```

### A. Fixture và goal chuẩn (P01–P08)

`GET /goal-projections/{id}` cho STD, F1–F6. F1–F6 phải cho đúng mức cần 12.358.000 / 21.371.000 /
8.475.000 / 21.760.000 / 27.648.000 / 15.448.000 ₫ và trạng thái `ON_TRACK`, `ON_TRACK`, `AT_RISK`,
`AT_RISK`, `BEHIND`, `BEHIND` của design #87. P08 đọc lại F5: `"monthlyRatePercent":0.000000` và phần đóng
góp bằng `10.000.000 × 51`.

### B. Đầu vào đặc biệt (P09–P15)

E3 góp 0 (số 0 là thật), E4 đã đủ, E10 lãi tự đủ (mức cần `0.00`), E9 làm tròn lên 1.000 ₫, E1 thiếu góp,
E2 thiếu lợi suất, E11 thiếu cả hai. Với E1, E2, E11 kiểm `unavailableReasons` đúng thứ tự trong phase 1
và các trường phụ thuộc là `null`.

### C. Mốc thời gian (P16–P19)

E5 → `DUE_THIS_MONTH`; E6, E7, E8 → `EXPIRED` với `daysPastTargetDate` 22, 53, 7 và `targetReached`
`false`, `true`, `false`. E8 (ngày đích đã qua nhưng cùng tháng) phải là `EXPIRED`, không phải
`DUE_THIS_MONTH`.

### D. Ngưỡng ở biên và đổi input (P20–P27)

```bash
api "$A" PUT /goals/<T> '{"name":"Quỹ du học hè","type":"EDUCATION","targetAmount":"120000000.00","targetDate":"2027-11-30","plannedMonthlyContribution":"9999999.00","expectedAnnualReturnPercent":"0.00"}'
```

Sau mỗi `PUT` gọi lại `GET /goal-projections/<T>`: 10.000.000 → `ON_TRACK` `100.00`; 9.999.999 →
`AT_RISK` `99.99`; 8.000.000 → `AT_RISK` `80.00`; 7.999.999 → `BEHIND` `79.99`. Với STD: góp 40.000.000
(P24), bỏ lợi suất (P25), lợi suất 7,00 và ngày đích 31/12/2032 (P26); mỗi lần `GET` ngay sau `PUT` phải
ra số mới. P27: `DELETE /goals/<E9>` rồi `GET /goal-projections/<E9>` → `404`.

### E. Danh sách và cô lập (P28–P32)

`GET /goal-projections` của A có đúng các id, đúng thứ tự của `GET /goals`, mỗi phần tử bằng body đọc
lẻ. B trước khi có goal: `[]`; sau khi tạo B1: một phần tử. B đọc `STD` của A → `404` có `type`, `title`,
`status`, `detail` giống một UUID ngẫu nhiên. `khong-phai-uuid` → `400 malformed-request`.

### F. Chứng thực và method (P33–P38)

Không token, `$EXPIRED`, `$FORGED` trên cả hai route → `401`. `POST /goal-projections/<STD>` và
`DELETE /goal-projections` → `405`. `-H 'Accept: application/xml'` → `406`.

### G. Xác định, sổ giả định, không ghi (P39–P41, P44)

Gọi F2 và danh sách hai lần, so `sha256sum`. Kiểm 17 mục `assumptions` của F2 theo phase 1, `disclaimer`
và `method.status` nêu 100 và 80. Trên toàn danh sách A: `fromAllocated + fromContributions = total`,
`total − targetAmount = gapToTarget`. `updatedAt` của `GET /goals/<F2>` không đổi sau các lần đọc dự phóng.

### H. Cấu hình ngưỡng (P42–P43)

Dừng API, khởi động lại với ngưỡng 95:

```bash
DB_URL=jdbc:postgresql://localhost:5489/rikkaus /opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home/bin/java -jar services/api/target/rikkaus-wealth-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=local --server.port=8089 --rikkaus.goals.projection.at-risk-min-coverage-percent=95
```

F3 (94,39) và F4 (91,91) thành `BEHIND`, F1 (121,37) vẫn `ON_TRACK` (E9 đã bị xóa ở P27), mục
`AT_RISK_MIN_COVERAGE_PERCENT` là `95`. Rồi khởi động với `=120`: app không lên và log có
`rikkaus.goals.projection.at-risk-min-coverage-percent must be less than`.

## Dọn

```bash
docker rm -f rikkaus-pg-issue-89
```

Dừng tiến trình `java` của API (Ctrl-C ở terminal chạy nó).
