# Xác minh thủ công — issue #89 dự phóng mục tiêu và trạng thái

**Kết quả: 44/44 case đạt** trên commit `6fc2c63`, và 44/44 lần nữa sau khi merge main thành `5c8718e` (merge `origin/main` `0eb1ec3` vào `a02b275`), chạy ngày
**23/11/2026** giờ Việt Nam qua HTTP vào PostgreSQL 18.6 Docker riêng. Không case nào fail trong lần chạy
chính thức. `clean verify` trên cùng commit xanh, không flag (50 integration test, gồm `OpenApiContractIT`
và `ApplicationContextIT`).

Danh sách case và kỳ vọng tính trước: [`phase-01-test-scenarios.md`](../261122-1842-GH-89-goal-projection-status/phase-01-test-scenarios.md).
Cách chạy lại: [`docs/manual-test-guide-goal-projection.md`](../../docs/manual-test-guide-goal-projection.md).

## Môi trường

| | |
|---|---|
| Commit | `6fc2c63`, nhánh `thientrinhcoder/feat/issue-89-goal-projection-status` |
| Jar | `rikkaus-wealth-api-0.0.1-SNAPSHOT.jar` đóng từ commit trên, sha256 bắt đầu `c9e7e586e1ff85be` |
| Database | container `rikkaus-pg-issue-89`, `postgres:18.6-alpine3.24`, `127.0.0.1:5489`, tạo mới cho lần chạy, xóa sau khi xong |
| API | `--spring.profiles.active=local --server.port=8089`, JDK 25; log có `Started RikkausWealthApplication` |
| Ngày tham chiếu | 23/11/2026 `Asia/Ho_Chi_Minh`, tháng góp đầu tiên 12/2026 |
| Token | A, B, hết hạn, ký sai khóa mint bằng secret phát triển của profile `local`; không ghi token vào tài liệu |
| Cách gửi request | script Python gửi đúng các request của guide và so với bảng phase 1; các khối `bash` của guide không được chạy nguyên văn |

## Sai khác so với phase 1, ghi trước khi chạy

- **Ngày chạy là 23/11/2026, không phải 22/11/2026.** Cùng tháng nên `n`, mọi số tiền, phần trăm và
  trạng thái giữ nguyên; `asOfDate` và mục `AS_OF_DATE` là `2026-11-23`, `daysPastTargetDate` của P17, P18,
  P19 là **23, 54, 8** thay cho 22, 53, 7.
- **P42 trong phase 1 kiểm E9, nhưng E9 đã bị xóa ở P27.** Đây là lỗi của kịch bản, không phải của code. Lần
  chạy thay E9 bằng F1 (bao phủ 121,37 %), phải giữ `ON_TRACK` dưới ngưỡng 95.

## Lịch sử lần chạy

- 22/11/2026, trên `a02b275` trước khi commit (chạy thử khi phát triển): P01–P41 và P44 đạt 42/42 ngay lần
  đầu; chưa chạy P42, P43. Sau lần đó chỉ đổi `operationId` tường minh và ví dụ OpenAPI, không đổi phép tính.
- 23/11/2026, trên `6fc2c63` (chính thức, ghi dưới đây): 44/44.
- 23/11/2026, chạy lại sau khi merge `origin/main` `4612f0e` (#104 deletion-impact và guard `linked-records`,
  #65 wealth snapshot) thành `5c8718e`: jar mới (sha256 bắt đầu `cca1ed24752e5c08`), database tạo mới,
  cùng script và kỳ vọng. **44/44 đạt**: P01–P41, P44 đạt 42/42; P42 (F1 `ON_TRACK` 121,37, F3 `BEHIND` 94,39,
  F4 `BEHIND` 91,91, ledger `95`) và P43 (thoát mã 1, log nêu property) đạt. P27 vẫn trả `204` rồi `404`:
  goal không có bản ghi liên kết nên guard của #104 không chặn. `clean verify -Dopenapi.update=true` không đổi
  `openapi.json`; `clean verify` không flag xanh. Body từng case giống lần chạy trên `6fc2c63` trừ `goalId`,
  nên phần dưới không in lại.

## Đối chiếu fixture design #87

| Goal | Mức cần design | Mức cần API | Trạng thái design | Trạng thái API | Tỷ lệ design | Tỷ lệ API |
|---|---:|---:|---|---|---:|---:|
| Quỹ dự phòng khẩn cấp | 12.358.000 | 12.358.000,00 | Đúng kế hoạch | `ON_TRACK` | 121,4 % | 121,37 |
| Mua nhà cho con | 21.371.000 | 21.371.000,00 | Đúng kế hoạch | `ON_TRACK` | 117,0 % | 116,98 |
| Học phí đại học cho con | 8.475.000 | 8.475.000,00 | Có rủi ro | `AT_RISK` | 94,4 % | 94,39 |
| Hưu trí | 21.760.000 | 21.760.000,00 | Có rủi ro | `AT_RISK` | 91,9 % | 91,91 |
| Tất toán vay mua nhà | 27.648.000 | 27.648.000,00 | Chậm kế hoạch | `BEHIND` | 36,2 % | 36,16 |
| Tăng trưởng tài sản ròng | 15.448.000 | 15.448.000,00 | Chậm kế hoạch | `BEHIND` | 64,7 % | 64,73 |

Tỷ lệ API làm tròn xuống 2 số lẻ; làm tròn lại 1 số lẻ ra đúng số design. Cột phân rã của "Mua nhà cho con"
làm tròn về đồng ra 1.594.581.665 / 1.644.090.271 / 3.238.671.937 ₫, đúng như design.

## Từng case

Body thật từ dây. Để report đọc được, `assumptions`, `method` và `disclaimer` được lược khỏi body (giống
nhau về cấu trúc ở mọi case) và chỉ in đầy đủ ở P40. P28 và P41 in bản tóm tắt do script tạo, vì body đầy
đủ là cả danh sách 18 phần tử.

#### P01 — STD Mua căn hộ — PASS

`GET /goal-projections/{STD}` → **200** `application/json`

```json
{"goalId":"c558283f-4d59-4745-8252-5f1b3056ea89","goalName":"Mua căn hộ","currency":"VND","asOfDate":"2026-11-23","timeline":"ACTIVE","targetReached":false,"completionPercent":17.14,"remainingAmount":2900000000.0,"remainingMonths":49,"daysPastTargetDate":null,"requiredMonthlyContribution":47800000.0,"contributionCoveragePercent":52.3,"status":"BEHIND","projectedAtTargetDate":{"fromAllocated":797859515.3,"fromContributions":1413282252.17,"total":2211141767.47,"gapToTarget":-1288858232.53},"calculation":{"monthlyRatePercent":0.583333,"growthFactor":1.3297658588},"unavailableReasons":[]}
```

#### P02 — F1 Quỹ dự phòng khẩn cấp — PASS

`GET /goal-projections/{F1}` → **200** `application/json`

```json
{"goalId":"ac53cccb-88ce-4d2a-b396-bcceca4abcd0","goalName":"Quỹ dự phòng khẩn cấp","currency":"VND","asOfDate":"2026-11-23","timeline":"ACTIVE","targetReached":false,"completionPercent":63.4,"remainingAmount":202000000.0,"remainingMonths":15,"daysPastTargetDate":null,"requiredMonthlyContribution":12358000.0,"contributionCoveragePercent":121.37,"status":"ON_TRACK","projectedAtTargetDate":{"fromAllocated":363357194.55,"fromContributions":228980477.94,"total":592337672.49,"gapToTarget":40337672.49},"calculation":{"monthlyRatePercent":0.25,"growthFactor":1.038163413},"unavailableReasons":[]}
```

#### P03 — F2 Mua nhà cho con — PASS

`GET /goal-projections/{F2}` → **200** `application/json`

```json
{"goalId":"fee2c073-3da6-4944-80d4-927462fae430","goalName":"Mua nhà cho con","currency":"VND","asOfDate":"2026-11-23","timeline":"ACTIVE","targetReached":false,"completionPercent":40.0,"remainingAmount":1800000000.0,"remainingMonths":57,"daysPastTargetDate":null,"requiredMonthlyContribution":21371000.0,"contributionCoveragePercent":116.98,"status":"ON_TRACK","projectedAtTargetDate":{"fromAllocated":1594581665.13,"fromContributions":1644090271.39,"total":3238671936.52,"gapToTarget":238671936.52},"calculation":{"monthlyRatePercent":0.5,"growthFactor":1.3288180543},"unavailableReasons":[]}
```

#### P04 — F3 Học phí đại học cho con — PASS

`GET /goal-projections/{F3}` → **200** `application/json`

```json
{"goalId":"ff45f6d9-9822-46cf-968f-0d45cc8c79e1","goalName":"Học phí đại học cho con","currency":"VND","asOfDate":"2026-11-23","timeline":"ACTIVE","targetReached":false,"completionPercent":16.66,"remainingAmount":750000000.0,"remainingMonths":71,"daysPastTargetDate":null,"requiredMonthlyContribution":8475000.0,"contributionCoveragePercent":94.39,"status":"AT_RISK","projectedAtTargetDate":{"fromAllocated":201513024.02,"fromContributions":659366707.5,"total":860879731.52,"gapToTarget":-39120268.48},"calculation":{"monthlyRatePercent":0.416667,"growthFactor":1.3434201602},"unavailableReasons":[]}
```

#### P05 — F4 Hưu trí — PASS

`GET /goal-projections/{F4}` → **200** `application/json`

```json
{"goalId":"cfb0a584-4a26-4d83-b9f3-f7457d830eea","goalName":"Hưu trí","currency":"VND","asOfDate":"2026-11-23","timeline":"ACTIVE","targetReached":false,"completionPercent":14.33,"remainingAmount":12850000000.0,"remainingMonths":183,"daysPastTargetDate":null,"requiredMonthlyContribution":21760000.0,"contributionCoveragePercent":91.91,"status":"AT_RISK","projectedAtTargetDate":{"fromAllocated":7253028890.75,"fromContributions":7120505428.95,"total":14373534319.7,"gapToTarget":-626465680.3},"calculation":{"monthlyRatePercent":0.666667,"growthFactor":3.3735018097},"unavailableReasons":[]}
```

#### P06 — F5 Tất toán vay mua nhà — PASS

`GET /goal-projections/{F5}` → **200** `application/json`

```json
{"goalId":"f52e5891-d689-4ab1-b3d9-a0117d6b9fd7","goalName":"Tất toán vay mua nhà","currency":"VND","asOfDate":"2026-11-23","timeline":"ACTIVE","targetReached":false,"completionPercent":6.0,"remainingAmount":1410000000.0,"remainingMonths":51,"daysPastTargetDate":null,"requiredMonthlyContribution":27648000.0,"contributionCoveragePercent":36.16,"status":"BEHIND","projectedAtTargetDate":{"fromAllocated":90000000.0,"fromContributions":510000000.0,"total":600000000.0,"gapToTarget":-900000000.0},"calculation":{"monthlyRatePercent":0.0,"growthFactor":1.0},"unavailableReasons":[]}
```

#### P07 — F6 Tăng trưởng tài sản ròng — PASS

`GET /goal-projections/{F6}` → **200** `application/json`

```json
{"goalId":"422c43c4-a17c-43a0-a1cb-02d94da76e9b","goalName":"Tăng trưởng tài sản ròng","currency":"VND","asOfDate":"2026-11-23","timeline":"ACTIVE","targetReached":false,"completionPercent":69.8,"remainingAmount":4530000000.0,"remainingMonths":51,"daysPastTargetDate":null,"requiredMonthlyContribution":15448000.0,"contributionCoveragePercent":64.73,"status":"BEHIND","projectedAtTargetDate":{"fromAllocated":14085553198.5,"fromContributions":591985787.72,"total":14677538986.22,"gapToTarget":-322461013.78},"calculation":{"monthlyRatePercent":0.583333,"growthFactor":1.3453250428},"unavailableReasons":[]}
```

#### P08 — F5 Tất toán vay mua nhà — PASS

`GET /goal-projections/{F5}` → **200** `application/json`

```json
{"goalId":"f52e5891-d689-4ab1-b3d9-a0117d6b9fd7","goalName":"Tất toán vay mua nhà","currency":"VND","asOfDate":"2026-11-23","timeline":"ACTIVE","targetReached":false,"completionPercent":6.0,"remainingAmount":1410000000.0,"remainingMonths":51,"daysPastTargetDate":null,"requiredMonthlyContribution":27648000.0,"contributionCoveragePercent":36.16,"status":"BEHIND","projectedAtTargetDate":{"fromAllocated":90000000.0,"fromContributions":510000000.0,"total":600000000.0,"gapToTarget":-900000000.0},"calculation":{"monthlyRatePercent":0.0,"growthFactor":1.0},"unavailableReasons":[]}
```

#### P09 — E3 Quỹ bảo dưỡng xe — PASS

`GET /goal-projections/{E3}` → **200** `application/json`

```json
{"goalId":"fb43cd00-90b7-4a82-a2b1-e8ec3902962b","goalName":"Quỹ bảo dưỡng xe","currency":"VND","asOfDate":"2026-11-23","timeline":"ACTIVE","targetReached":false,"completionPercent":20.0,"remainingAmount":120000000.0,"remainingMonths":19,"daysPastTargetDate":null,"requiredMonthlyContribution":6029000.0,"contributionCoveragePercent":0.0,"status":"BEHIND","projectedAtTargetDate":{"fromAllocated":31958091.17,"fromContributions":0.0,"total":31958091.17,"gapToTarget":-118041908.83},"calculation":{"monthlyRatePercent":0.333333,"growthFactor":1.0652697056},"unavailableReasons":[]}
```

#### P10 — E4 Quỹ mua xe máy — PASS

`GET /goal-projections/{E4}` → **200** `application/json`

```json
{"goalId":"b7b52d3b-a795-4496-838b-71386bfef4b3","goalName":"Quỹ mua xe máy","currency":"VND","asOfDate":"2026-11-23","timeline":"ACTIVE","targetReached":true,"completionPercent":100.0,"remainingAmount":0.0,"remainingMonths":7,"daysPastTargetDate":null,"requiredMonthlyContribution":0.0,"contributionCoveragePercent":100.0,"status":"ON_TRACK","projectedAtTargetDate":{"fromAllocated":80000000.0,"fromContributions":0.0,"total":80000000.0,"gapToTarget":0.0},"calculation":{"monthlyRatePercent":0.0,"growthFactor":1.0},"unavailableReasons":[]}
```

#### P11 — E10 Quỹ hưu sớm — PASS

`GET /goal-projections/{E10}` → **200** `application/json`

```json
{"goalId":"07ccb28a-693e-4a81-adda-fce0458bcae5","goalName":"Quỹ hưu sớm","currency":"VND","asOfDate":"2026-11-23","timeline":"ACTIVE","targetReached":false,"completionPercent":90.0,"remainingAmount":100000000.0,"remainingMonths":120,"daysPastTargetDate":null,"requiredMonthlyContribution":0.0,"contributionCoveragePercent":100.0,"status":"ON_TRACK","projectedAtTargetDate":{"fromAllocated":1637457060.63,"fromContributions":0.0,"total":1637457060.63,"gapToTarget":637457060.63},"calculation":{"monthlyRatePercent":0.5,"growthFactor":1.819396734},"unavailableReasons":[]}
```

#### P12 — E9 Quỹ sửa nhà — PASS

`GET /goal-projections/{E9}` → **200** `application/json`

```json
{"goalId":"3cdc7602-da18-44e5-8aad-1666ca758a1c","goalName":"Quỹ sửa nhà","currency":"VND","asOfDate":"2026-11-23","timeline":"ACTIVE","targetReached":false,"completionPercent":0.0,"remainingAmount":100000000.0,"remainingMonths":7,"daysPastTargetDate":null,"requiredMonthlyContribution":14286000.0,"contributionCoveragePercent":97.99,"status":"AT_RISK","projectedAtTargetDate":{"fromAllocated":0.0,"fromContributions":98000000.0,"total":98000000.0,"gapToTarget":-2000000.0},"calculation":{"monthlyRatePercent":0.0,"growthFactor":1.0},"unavailableReasons":[]}
```

#### P13 — E1 Quỹ học tiếp cho bản thân — PASS

`GET /goal-projections/{E1}` → **200** `application/json`

```json
{"goalId":"7f7a97ec-c7f4-4fc1-aa98-9d0b139c8650","goalName":"Quỹ học tiếp cho bản thân","currency":"VND","asOfDate":"2026-11-23","timeline":"ACTIVE","targetReached":false,"completionPercent":10.0,"remainingAmount":360000000.0,"remainingMonths":37,"daysPastTargetDate":null,"requiredMonthlyContribution":9025000.0,"contributionCoveragePercent":null,"status":null,"projectedAtTargetDate":null,"calculation":{"monthlyRatePercent":0.333333,"growthFactor":1.1310294474},"unavailableReasons":["PLANNED_MONTHLY_CONTRIBUTION_MISSING"]}
```

#### P14 — E2 Du lịch Nhật Bản — PASS

`GET /goal-projections/{E2}` → **200** `application/json`

```json
{"goalId":"0e52a912-b05b-4eae-96f6-16a169b32cfd","goalName":"Du lịch Nhật Bản","currency":"VND","asOfDate":"2026-11-23","timeline":"ACTIVE","targetReached":false,"completionPercent":0.0,"remainingAmount":120000000.0,"remainingMonths":16,"daysPastTargetDate":null,"requiredMonthlyContribution":null,"contributionCoveragePercent":null,"status":null,"projectedAtTargetDate":null,"calculation":null,"unavailableReasons":["EXPECTED_ANNUAL_RETURN_MISSING"]}
```

#### P15 — E11 Quỹ cưới — PASS

`GET /goal-projections/{E11}` → **200** `application/json`

```json
{"goalId":"cb763528-c3f9-45f3-af57-90efe4c9c607","goalName":"Quỹ cưới","currency":"VND","asOfDate":"2026-11-23","timeline":"ACTIVE","targetReached":false,"completionPercent":0.0,"remainingAmount":200000000.0,"remainingMonths":25,"daysPastTargetDate":null,"requiredMonthlyContribution":null,"contributionCoveragePercent":null,"status":null,"projectedAtTargetDate":null,"calculation":null,"unavailableReasons":["PLANNED_MONTHLY_CONTRIBUTION_MISSING","EXPECTED_ANNUAL_RETURN_MISSING"]}
```

#### P16 — E5 Quà kỷ niệm 20 năm — PASS

`GET /goal-projections/{E5}` → **200** `application/json`

```json
{"goalId":"b4dfdc51-b9ab-4a68-a85d-567d360efcc2","goalName":"Quà kỷ niệm 20 năm","currency":"VND","asOfDate":"2026-11-23","timeline":"DUE_THIS_MONTH","targetReached":false,"completionPercent":79.16,"remainingAmount":25000000.0,"remainingMonths":0,"daysPastTargetDate":null,"requiredMonthlyContribution":null,"contributionCoveragePercent":null,"status":null,"projectedAtTargetDate":null,"calculation":null,"unavailableReasons":["NO_CONTRIBUTION_MONTH"]}
```

#### P17 — E6 Đổi xe gia đình — PASS

`GET /goal-projections/{E6}` → **200** `application/json`

```json
{"goalId":"bad8c727-d41a-4eea-9987-2253f557f091","goalName":"Đổi xe gia đình","currency":"VND","asOfDate":"2026-11-23","timeline":"EXPIRED","targetReached":false,"completionPercent":77.5,"remainingAmount":180000000.0,"remainingMonths":0,"daysPastTargetDate":23,"requiredMonthlyContribution":null,"contributionCoveragePercent":null,"status":null,"projectedAtTargetDate":null,"calculation":null,"unavailableReasons":["TARGET_DATE_PASSED"]}
```

#### P18 — E7 Du lịch châu Âu — PASS

`GET /goal-projections/{E7}` → **200** `application/json`

```json
{"goalId":"0fac8fa1-93df-4cd6-b97b-a608766d9c83","goalName":"Du lịch châu Âu","currency":"VND","asOfDate":"2026-11-23","timeline":"EXPIRED","targetReached":true,"completionPercent":100.0,"remainingAmount":0.0,"remainingMonths":0,"daysPastTargetDate":54,"requiredMonthlyContribution":null,"contributionCoveragePercent":null,"status":null,"projectedAtTargetDate":null,"calculation":null,"unavailableReasons":["TARGET_DATE_PASSED"]}
```

#### P19 — E8 Quà sinh nhật mẹ — PASS

`GET /goal-projections/{E8}` → **200** `application/json`

```json
{"goalId":"189468c3-e158-4754-8fa0-4f39996724a1","goalName":"Quà sinh nhật mẹ","currency":"VND","asOfDate":"2026-11-23","timeline":"EXPIRED","targetReached":false,"completionPercent":40.0,"remainingAmount":18000000.0,"remainingMonths":0,"daysPastTargetDate":8,"requiredMonthlyContribution":null,"contributionCoveragePercent":null,"status":null,"projectedAtTargetDate":null,"calculation":null,"unavailableReasons":["TARGET_DATE_PASSED"]}
```

#### P20 — T Quỹ du học hè — PASS

`GET /goal-projections/{T}` → **200** `application/json`

```json
{"goalId":"359bfc72-aac2-4647-aba6-ca1343d3b299","goalName":"Quỹ du học hè","currency":"VND","asOfDate":"2026-11-23","timeline":"ACTIVE","targetReached":false,"completionPercent":0.0,"remainingAmount":120000000.0,"remainingMonths":12,"daysPastTargetDate":null,"requiredMonthlyContribution":10000000.0,"contributionCoveragePercent":100.0,"status":"ON_TRACK","projectedAtTargetDate":{"fromAllocated":0.0,"fromContributions":120000000.0,"total":120000000.0,"gapToTarget":0.0},"calculation":{"monthlyRatePercent":0.0,"growthFactor":1.0},"unavailableReasons":[]}
```

#### P21 — T góp 9999999.00 — PASS

`PUT /goals/{T} then GET /goal-projections/{T}` → **200** `application/json`

```json
{"goalId":"359bfc72-aac2-4647-aba6-ca1343d3b299","goalName":"Quỹ du học hè","currency":"VND","asOfDate":"2026-11-23","timeline":"ACTIVE","targetReached":false,"completionPercent":0.0,"remainingAmount":120000000.0,"remainingMonths":12,"daysPastTargetDate":null,"requiredMonthlyContribution":10000000.0,"contributionCoveragePercent":99.99,"status":"AT_RISK","projectedAtTargetDate":{"fromAllocated":0.0,"fromContributions":119999988.0,"total":119999988.0,"gapToTarget":-12.0},"calculation":{"monthlyRatePercent":0.0,"growthFactor":1.0},"unavailableReasons":[]}
```

#### P22 — T góp 8000000.00 — PASS

`PUT /goals/{T} then GET /goal-projections/{T}` → **200** `application/json`

```json
{"goalId":"359bfc72-aac2-4647-aba6-ca1343d3b299","goalName":"Quỹ du học hè","currency":"VND","asOfDate":"2026-11-23","timeline":"ACTIVE","targetReached":false,"completionPercent":0.0,"remainingAmount":120000000.0,"remainingMonths":12,"daysPastTargetDate":null,"requiredMonthlyContribution":10000000.0,"contributionCoveragePercent":80.0,"status":"AT_RISK","projectedAtTargetDate":{"fromAllocated":0.0,"fromContributions":96000000.0,"total":96000000.0,"gapToTarget":-24000000.0},"calculation":{"monthlyRatePercent":0.0,"growthFactor":1.0},"unavailableReasons":[]}
```

#### P23 — T góp 7999999.00 — PASS

`PUT /goals/{T} then GET /goal-projections/{T}` → **200** `application/json`

```json
{"goalId":"359bfc72-aac2-4647-aba6-ca1343d3b299","goalName":"Quỹ du học hè","currency":"VND","asOfDate":"2026-11-23","timeline":"ACTIVE","targetReached":false,"completionPercent":0.0,"remainingAmount":120000000.0,"remainingMonths":12,"daysPastTargetDate":null,"requiredMonthlyContribution":10000000.0,"contributionCoveragePercent":79.99,"status":"BEHIND","projectedAtTargetDate":{"fromAllocated":0.0,"fromContributions":95999988.0,"total":95999988.0,"gapToTarget":-24000012.0},"calculation":{"monthlyRatePercent":0.0,"growthFactor":1.0},"unavailableReasons":[]}
```

#### P24 — STD góp 40.000.000 — PASS

`PUT /goals/{STD} then GET /goal-projections/{STD}` → **200** `application/json`

```json
{"goalId":"c558283f-4d59-4745-8252-5f1b3056ea89","goalName":"Mua căn hộ","currency":"VND","asOfDate":"2026-11-23","timeline":"ACTIVE","targetReached":false,"completionPercent":17.14,"remainingAmount":2900000000.0,"remainingMonths":49,"daysPastTargetDate":null,"requiredMonthlyContribution":47800000.0,"contributionCoveragePercent":83.68,"status":"AT_RISK","projectedAtTargetDate":{"fromAllocated":797859515.3,"fromContributions":2261251603.47,"total":3059111118.77,"gapToTarget":-440888881.23},"calculation":{"monthlyRatePercent":0.583333,"growthFactor":1.3297658588},"unavailableReasons":[]}
```

#### P25 — STD bỏ lợi suất — PASS

`PUT /goals/{STD} then GET /goal-projections/{STD}` → **200** `application/json`

```json
{"goalId":"c558283f-4d59-4745-8252-5f1b3056ea89","goalName":"Mua căn hộ","currency":"VND","asOfDate":"2026-11-23","timeline":"ACTIVE","targetReached":false,"completionPercent":17.14,"remainingAmount":2900000000.0,"remainingMonths":49,"daysPastTargetDate":null,"requiredMonthlyContribution":null,"contributionCoveragePercent":null,"status":null,"projectedAtTargetDate":null,"calculation":null,"unavailableReasons":["EXPECTED_ANNUAL_RETURN_MISSING"]}
```

#### P26 — STD lợi suất 7, đích 31/12/2032 — PASS

`PUT /goals/{STD} then GET /goal-projections/{STD}` → **200** `application/json`

```json
{"goalId":"c558283f-4d59-4745-8252-5f1b3056ea89","goalName":"Mua căn hộ","currency":"VND","asOfDate":"2026-11-23","timeline":"ACTIVE","targetReached":false,"completionPercent":17.14,"remainingAmount":2900000000.0,"remainingMonths":73,"daysPastTargetDate":null,"requiredMonthlyContribution":28481000.0,"contributionCoveragePercent":140.44,"status":"ON_TRACK","projectedAtTargetDate":{"fromAllocated":917383671.82,"fromContributions":3627241963.64,"total":4544625635.46,"gapToTarget":1044625635.46},"calculation":{"monthlyRatePercent":0.583333,"growthFactor":1.5289727864},"unavailableReasons":[]}
```

#### P27 — xóa E9 rồi đọc dự phóng — PASS

`DELETE /goals/{E9}; GET /goal-projections/{E9}` → **404** `application/problem+json`

```json
{"detail":"Resource not found.","instance":"/api/v1/goal-projections/3cdc7602-da18-44e5-8aad-1666ca758a1c","status":404,"title":"Resource not found","type":"urn:rikkaus:problem:not-found","correlationId":"534398f1-c83c-46e2-bd43-770a5d8fa636"}
```

#### P28 — danh sách A (18 phần tử) cùng thứ tự GET /goals — PASS

`GET /goal-projections` → **200** `application/json`

```json
[{"goalId":"0fac8fa1-93df-4cd6-b97b-a608766d9c83","goalName":"Du lịch châu Âu","timeline":"EXPIRED","status":null},{"goalId":"bad8c727-d41a-4eea-9987-2253f557f091","goalName":"Đổi xe gia đình","timeline":"EXPIRED","status":null},{"goalId":"189468c3-e158-4754-8fa0-4f39996724a1","goalName":"Quà sinh nhật mẹ","timeline":"EXPIRED","status":null},{"goalId":"b4dfdc51-b9ab-4a68-a85d-567d360efcc2","goalName":"Quà kỷ niệm 20 năm","timeline":"DUE_THIS_MONTH","status":null},{"goalId":"b7b52d3b-a795-4496-838b-71386bfef4b3","goalName":"Quỹ mua xe máy","timeline":"ACTIVE","status":"ON_TRACK"},{"goalId":"359bfc72-aac2-4647-aba6-ca1343d3b299","goalName":"Quỹ du học hè","timeline":"ACTIVE","status":"BEHIND"},{"goalId":"ac53cccb-88ce-4d2a-b396-bcceca4abcd0","goalName":"Quỹ dự phòng khẩn cấp","timeline":"ACTIVE","status":"ON_TRACK"},{"goalId":"0e52a912-b05b-4eae-96f6-16a169b32cfd","goalName":"Du lịch Nhật Bản","timeline":"ACTIVE","status":null},{"goalId":"fb43cd00-90b7-4a82-a2b1-e8ec3902962b","goalName":"Quỹ bảo dưỡng xe","timeline":"ACTIVE","status":"BEHIND"},{"goalId":"cb763528-c3f9-45f3-af57-90efe4c9c607","goalName":"Quỹ cưới","timeline":"ACTIVE","status":null},{"goalId":"7f7a97ec-c7f4-4fc1-aa98-9d0b139c8650","goalName":"Quỹ học tiếp cho bản thân","timeline":"ACTIVE","status":null},{"goalId":"422c43c4-a17c-43a0-a1cb-02d94da76e9b","goalName":"Tăng trưởng tài sản ròng","timeline":"ACTIVE","status":"BEHIND"},{"goalId":"f52e5891-d689-4ab1-b3d9-a0117d6b9fd7","goalName":"Tất toán vay mua nhà","timeline":"ACTIVE","status":"BEHIND"},{"goalId":"fee2c073-3da6-4944-80d4-927462fae430","goalName":"Mua nhà cho con","timeline":"ACTIVE","status":"ON_TRACK"},{"goalId":"ff45f6d9-9822-46cf-968f-0d45cc8c79e1","goalName":"Học phí đại học cho con","timeline":"ACTIVE","status":"AT_RISK"},{"goalId":"c558283f-4d59-4745-8252-5f1b3056ea89","goalName":"Mua căn hộ","timeline":"ACTIVE","status":"ON_TRACK"},{"goalId":"07ccb28a-693e-4a81-adda-fce0458bcae5","goalName":"Quỹ hưu sớm","timeline":"ACTIVE","status":"ON_TRACK"},{"goalId":"cfb0a584-4a26-4d83-b9f3-f7457d830eea","goalName":"Hưu trí","timeline":"ACTIVE","status":"AT_RISK"}]
```

#### P29 — B: [] trước, một phần tử B1 sau — PASS

`GET /goal-projections (B)` → **200** `application/json`

```json
[{"goalId":"cdce2fa4-1f12-4ff2-8cb2-38471f7bdbcd","goalName":"Quỹ khởi nghiệp","currency":"VND","asOfDate":"2026-11-23","timeline":"ACTIVE","targetReached":false,"completionPercent":20.0,"remainingAmount":400000000.0,"remainingMonths":31,"daysPastTargetDate":null,"requiredMonthlyContribution":11698000.0,"contributionCoveragePercent":68.38,"status":"BEHIND","projectedAtTargetDate":{"fromAllocated":113757444.36,"fromContributions":264142931.68,"total":377900376.04,"gapToTarget":-122099623.96},"calculation":{"monthlyRatePercent":0.416667,"growthFactor":1.1375744436},"unavailableReasons":[]}]
```

#### P30 — B đọc goal của A — PASS

`GET /goal-projections/{STD} (B)` → **404** `application/problem+json`

```json
{"detail":"Resource not found.","instance":"/api/v1/goal-projections/c558283f-4d59-4745-8252-5f1b3056ea89","status":404,"title":"Resource not found","type":"urn:rikkaus:problem:not-found","correlationId":"287ac586-ea83-42e7-8add-8acc02f1479f"}
```

#### P31 — uuid ngẫu nhiên — PASS

`GET /goal-projections/{random}` → **404** `application/problem+json`

```json
{"detail":"Resource not found.","instance":"/api/v1/goal-projections/432a4b96-c645-4d94-9cb6-007276328008","status":404,"title":"Resource not found","type":"urn:rikkaus:problem:not-found","correlationId":"cbd130e4-40da-4dc7-9994-1fbbfb8fb66a"}
```

#### P32 — id sai dạng — PASS

`GET /goal-projections/khong-phai-uuid` → **400** `application/problem+json`

```json
{"detail":"Malformed request.","instance":"/api/v1/goal-projections/khong-phai-uuid","status":400,"title":"Malformed request","type":"urn:rikkaus:problem:malformed-request","correlationId":"14b619e7-78a6-4c97-962f-381e25026575"}
```

#### P33 — không token (hai route) — PASS

`GET /goal-projections/{F2}; GET /goal-projections` → **401** `application/problem+json`

```json
{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized","correlationId":"781e597c-3d8d-4fc4-8fac-544a60c44d51"}
```

#### P34 — token hết hạn (hai route) — PASS

`GET /goal-projections/{F2}; GET /goal-projections` → **401** `application/problem+json`

```json
{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized","correlationId":"3ef02ec6-23d0-4728-8934-4d118fcf080d"}
```

#### P35 — token ký sai khóa (hai route) — PASS

`GET /goal-projections/{F2}; GET /goal-projections` → **401** `application/problem+json`

```json
{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized","correlationId":"4cd1d5c8-0f98-4e7d-9ea6-90b724c8e88f"}
```

#### P36 — POST lên member — PASS

`POST /goal-projections/{STD}` → **405** `application/problem+json`

```json
{"detail":"Method 'POST' is not supported.","instance":"/api/v1/goal-projections/c558283f-4d59-4745-8252-5f1b3056ea89","status":405,"title":"Method not allowed","type":"urn:rikkaus:problem:method-not-allowed","correlationId":"11226685-52cb-4b9c-b1b0-eb75f7e76889"}
```

#### P37 — DELETE lên collection — PASS

`DELETE /goal-projections` → **405** `application/problem+json`

```json
{"detail":"Method 'DELETE' is not supported.","instance":"/api/v1/goal-projections","status":405,"title":"Method not allowed","type":"urn:rikkaus:problem:method-not-allowed","correlationId":"f9050c29-8822-4f80-b83e-f7a730faafe7"}
```

#### P38 — Accept: application/xml — PASS

`GET /goal-projections/{STD}` → **406** `application/problem+json`

```json
{"detail":"Acceptable representations: [application/json, application/*+json, application/yaml].","instance":"/api/v1/goal-projections/c558283f-4d59-4745-8252-5f1b3056ea89","status":406,"title":"Not acceptable","type":"urn:rikkaus:problem:not-acceptable","correlationId":"b88eabfe-90f2-4133-9def-37ac9ae420b2"}
```

#### P39 — hai lần gọi giống từng byte — PASS

`GET x2 member, GET x2 list` → **200** `application/json`

```json
{"member":["bbd48b8dcd8100017f7094e5db2e61cae866b740a5b10f553ff936e69021af85","bbd48b8dcd8100017f7094e5db2e61cae866b740a5b10f553ff936e69021af85"],"list":["818a236403f6ffa5c16b3214b8a36c6cd4d9ea1ac2db95b4945e61ad00805b73","818a236403f6ffa5c16b3214b8a36c6cd4d9ea1ac2db95b4945e61ad00805b73"]}
```

#### P40 — sổ giả định, phương pháp, disclaimer của F2 — PASS

`GET /goal-projections/{F2}` → **200** `application/json`

```json
{"goalId":"fee2c073-3da6-4944-80d4-927462fae430","goalName":"Mua nhà cho con","currency":"VND","asOfDate":"2026-11-23","timeline":"ACTIVE","targetReached":false,"completionPercent":40.00,"remainingAmount":1800000000.00,"remainingMonths":57,"daysPastTargetDate":null,"requiredMonthlyContribution":21371000.00,"contributionCoveragePercent":116.98,"status":"ON_TRACK","projectedAtTargetDate":{"fromAllocated":1594581665.13,"fromContributions":1644090271.39,"total":3238671936.52,"gapToTarget":238671936.52},"calculation":{"monthlyRatePercent":0.500000,"growthFactor":1.3288180543},"unavailableReasons":[],"assumptions":[{"key":"TARGET_AMOUNT","source":"USER_ENTERED","value":"3000000000.00"},{"key":"CURRENT_ALLOCATED_AMOUNT","source":"USER_ENTERED","value":"1200000000.00"},{"key":"TARGET_DATE","source":"USER_ENTERED","value":"2031-08-31"},{"key":"PLANNED_MONTHLY_CONTRIBUTION","source":"USER_ENTERED","value":"25000000.00"},{"key":"EXPECTED_ANNUAL_RETURN_PERCENT","source":"USER_ENTERED","value":"6.00"},{"key":"AS_OF_DATE","source":"SYSTEM","value":"2026-11-23"},{"key":"TIME_ZONE","source":"SYSTEM","value":"Asia/Ho_Chi_Minh"},{"key":"FIRST_CONTRIBUTION_MONTH","source":"SYSTEM","value":"2026-12"},{"key":"COMPOUNDING","source":"SYSTEM","value":"MONTHLY"},{"key":"CONTRIBUTION_TIMING","source":"SYSTEM","value":"END_OF_MONTH"},{"key":"REQUIRED_CONTRIBUTION_ROUNDING","source":"SYSTEM","value":"UP_TO_1000_VND"},{"key":"ON_TRACK_MIN_COVERAGE_PERCENT","source":"SYSTEM","value":"100"},{"key":"AT_RISK_MIN_COVERAGE_PERCENT","source":"SYSTEM","value":"80"},{"key":"INFLATION","source":"NOT_MODELLED","value":null},{"key":"TAXES_AND_FEES","source":"NOT_MODELLED","value":null},{"key":"RETURN_VARIABILITY","source":"NOT_MODELLED","value":null},{"key":"CASH_FLOW_LINK","source":"NOT_MODELLED","value":null}],"method":{"name":"FUTURE_VALUE_MONTHLY_COMPOUNDING","remainingMonths":"n = calendar months from the month after asOfDate through the month of targetDate, both included; contributions arrive at the end of each month. A target date before asOfDate is EXPIRED; a target date later this month leaves n = 0 and is DUE_THIS_MONTH.","monthlyRate":"r = expectedAnnualReturnPercent / 100 / 12, compounded monthly; g = (1 + r)^n.","projectedAmount":"fromAllocated = currentAllocatedAmount × g; fromContributions = plannedMonthlyContribution × (g − 1) / r, or plannedMonthlyContribution × n when r = 0; total = fromAllocated + fromContributions; gapToTarget = total − targetAmount.","requiredMonthlyContribution":"(targetAmount − currentAllocatedAmount × g) × r / (g − 1), or (targetAmount − currentAllocatedAmount) / n when r = 0; 0 when the goal is already funded or the allocated amount alone reaches the target; otherwise rounded up to the next 1,000 VND.","status":"contributionCoveragePercent = plannedMonthlyContribution / requiredMonthlyContribution × 100 (100 when nothing is required). ON_TRACK at or above 100, AT_RISK at or above 80, BEHIND below; these are system reference thresholds, compared before the percentage is rounded.","rounding":"Intermediate values carry 34 significant digits. Each part of the projected amount is rounded half up to 2 decimals before they are added, so the parts add up to the total. Percentages are rounded down to 2 decimals."},"disclaimer":"The expected annual return is the user's own assumption, not a guaranteed, promised or predicted return. Projected amounts illustrate what the entered figures would produce if that return were earned every month; actual results will differ."}
```

#### P41 — kiểm cộng dồn và null trên toàn danh sách A — PASS

`GET /goal-projections` → **200** `application/json`

```json
{"checked":18,"errors":[]}
```

#### P44 — đọc dự phóng không ghi gì — PASS

`GET /goals/{F2} trước và sau` → **200** `application/json`

```json
{"id":"fee2c073-3da6-4944-80d4-927462fae430","name":"Mua nhà cho con","type":"HOME_PURCHASE","currency":"VND","targetAmount":3000000000.0,"currentAllocatedAmount":1200000000.0,"targetDate":"2031-08-31","plannedMonthlyContribution":25000000.0,"expectedAnnualReturnPercent":6.0,"createdAt":"2026-11-23T08:39:18.130168Z","updatedAt":"2026-11-23T08:39:18.130168Z"}
```

#### P42 — F1 với ngưỡng at-risk 95 — PASS

`GET /goal-projections/{F1}` → **200**

```json
{"goalId":"ac53cccb-88ce-4d2a-b396-bcceca4abcd0","goalName":"Quỹ dự phòng khẩn cấp","currency":"VND","asOfDate":"2026-11-23","timeline":"ACTIVE","targetReached":false,"completionPercent":63.4,"remainingAmount":202000000.0,"remainingMonths":15,"daysPastTargetDate":null,"requiredMonthlyContribution":12358000.0,"contributionCoveragePercent":121.37,"status":"ON_TRACK","projectedAtTargetDate":{"fromAllocated":363357194.55,"fromContributions":228980477.94,"total":592337672.49,"gapToTarget":40337672.49},"calculation":{"monthlyRatePercent":0.25,"growthFactor":1.038163413},"unavailableReasons":[]}
```

#### P42 — F3 với ngưỡng at-risk 95 — PASS

`GET /goal-projections/{F3}` → **200**

```json
{"goalId":"ff45f6d9-9822-46cf-968f-0d45cc8c79e1","goalName":"Học phí đại học cho con","currency":"VND","asOfDate":"2026-11-23","timeline":"ACTIVE","targetReached":false,"completionPercent":16.66,"remainingAmount":750000000.0,"remainingMonths":71,"daysPastTargetDate":null,"requiredMonthlyContribution":8475000.0,"contributionCoveragePercent":94.39,"status":"BEHIND","projectedAtTargetDate":{"fromAllocated":201513024.02,"fromContributions":659366707.5,"total":860879731.52,"gapToTarget":-39120268.48},"calculation":{"monthlyRatePercent":0.416667,"growthFactor":1.3434201602},"unavailableReasons":[]}
```

#### P42 — F4 với ngưỡng at-risk 95 — PASS

`GET /goal-projections/{F4}` → **200**

```json
{"goalId":"cfb0a584-4a26-4d83-b9f3-f7457d830eea","goalName":"Hưu trí","currency":"VND","asOfDate":"2026-11-23","timeline":"ACTIVE","targetReached":false,"completionPercent":14.33,"remainingAmount":12850000000.0,"remainingMonths":183,"daysPastTargetDate":null,"requiredMonthlyContribution":21760000.0,"contributionCoveragePercent":91.91,"status":"BEHIND","projectedAtTargetDate":{"fromAllocated":7253028890.75,"fromContributions":7120505428.95,"total":14373534319.7,"gapToTarget":-626465680.3},"calculation":{"monthlyRatePercent":0.666667,"growthFactor":3.3735018097},"unavailableReasons":[]}
```

#### P43 — ngưỡng at-risk 120 — PASS

Tiến trình thoát mã **1**, không mở cổng 8089. Dòng log nêu property:

```text
Reason: java.lang.IllegalStateException: rikkaus.goals.projection.at-risk-min-coverage-percent must be less than rikkaus.goals.projection.on-track-min-coverage-percent.
```
