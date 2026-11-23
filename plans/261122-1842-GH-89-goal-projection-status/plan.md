---
title: "Issue #89 dự phóng mục tiêu minh bạch và trạng thái"
description: "Route đọc GET /api/v1/goal-projections và GET /api/v1/goal-projections/{goalId} trong slice goals: phần trăm hoàn thành, số còn lại, số tháng còn lại, dự phóng tại ngày đích theo công thức future value tách hai phần, mức đóng góp cần mỗi tháng, trạng thái ON_TRACK | AT_RISK | BEHIND theo ngưỡng cấu hình tập trung, sổ giả định và mô tả phương pháp trong response. Tính khi đọc, không migration."
status: completed
priority: P1
effort: "2d"
issue: 89
branch: thientrinhcoder/feat/issue-89-goal-projection-status
tags: [backend, api, goals, projection]
blockedBy: [86]
blocks: [88, 92]
created: 2026-11-22
---

# Issue #89 dự phóng mục tiêu minh bạch và trạng thái

## Outcome

Người dùng pilot hỏi được "với số đã để dành, mức góp mỗi tháng và lợi suất tôi tự giả định, mục tiêu này
có về đích đúng hạn không, và vì sao" qua `GET /api/v1/goal-projections/{goalId}` (một goal) và
`GET /api/v1/goal-projections` (mọi goal của mình, cùng thứ tự với danh sách goal). Mỗi dự phóng có năm
output của backbone (phần trăm hoàn thành, số còn lại, thời gian còn lại, mức đóng góp cần mỗi tháng,
trạng thái), dự phóng tại ngày đích tách hai phần như cột phân rã của design #87, sổ giả định ghi nguồn
từng giả định, mô tả công thức ngay trong response, và lý do khi một output không khả dụng. Đổi input bằng
`PUT /goals/{goalId}` làm dự phóng đổi ngay ở lần đọc sau vì không có gì được lưu hay cache. Đây là đầu vào
cho frontend #88 và Basic Advisor #92.

## Constraints

- Mọi file mới nằm trong `com.rikkaus.wealth.goals.projection` (`api`, `application`). Không sửa
  `GoalController`, `GoalService`, `GoalRepository` (đồng nghiệp #104 đang sửa). `GoalService.read` và
  `GoalService.listFor` đã `public`, tái dùng nguyên trạng, nên ownership vẫn đi qua `OwnershipGuard`.
- Tiền là `BigDecimal`; không `double`. Không migration, không bảng mới, không thêm `ProblemType`.
- Ngưỡng trạng thái là property `rikkaus.goals.projection.*` với mặc định trong `application.yml`.
- Không unit test/integration test mới (chỉ đạo Product Owner). Bù bằng scenario viết trước ở phase 1,
  chạy thật qua HTTP vào PostgreSQL Docker. Inventory `OpenApiContractIT` được mở rộng vì là CI gate.

## Non-goals

Basic Advisor (#92), insight rules, sửa CRUD goals, lưu dự phóng, lạm phát, thuế phí, kịch bản lợi suất
nhiều mức, liên kết tự động giữa đóng góp và dòng tiền, ngưỡng do người dùng tự đặt, tham số `asOf`.

## Quyết định đã chốt

Design đã duyệt: [`docs/transparent-goal-projection-status-review.html`](../../docs/transparent-goal-projection-status-review.html)
và plan design #87 (`plans/260920-1610-issue-87-transparent-goal-projection/`, ở checkout chính). Design nói
rõ công thức production, quy ước làm tròn và giá trị ngưỡng thuộc #89; các điểm được chốt như sau.

| # | Quyết định | Nguồn | Ví dụ |
|---|---|---|---|
| D1 | Route riêng `GET /api/v1/goal-projections` và `GET /api/v1/goal-projections/{goalId}`, controller riêng; không đặt dưới `/goals/...` để không chung không gian path với `GoalController` (`/goals/projections` sẽ đụng `GET /goals/{goalId}`) | brief: controller riêng; cùng kiểu với `/goal-templates` | `GET /goal-projections/9d4b…` |
| D2 | Ngày tham chiếu là hôm nay theo `Asia/Ho_Chi_Minh`, từ `Clock` bean. **Không** có tham số `asOf`: số đã phân bổ là số của hôm nay, nên tính "như thể ngày khác" là trộn hai thời điểm. Scenario tái lập bằng cách dời ngày đích theo ngày chạy | design quy tắc 1; brief cho phép tùy chọn | chạy 22/11/2026: tháng góp đầu tiên 12/2026 |
| D3 | `n` = số tháng lịch từ tháng sau ngày tham chiếu đến tháng của ngày đích, tính cả hai đầu | design quy tắc 1 | 22/11/2026 → 31/12/2030: `n = 49` |
| D4 | Ngày đích **trước** ngày tham chiếu → `timeline: EXPIRED`, kèm `targetReached` và `daysPastTargetDate`; không có mức cần, dự phóng, trạng thái. So bằng **ngày**, không bằng tháng: design tính theo tháng nên ngày đích 15/11 khi hôm nay 22/11 rơi vào "không còn tháng nào", trái với chính câu chữ "goal date has already passed" | design quy tắc 6 | 31/10/2026 → `EXPIRED`, quá 22 ngày |
| D5 | Ngày đích từ hôm nay đến cuối tháng này → `timeline: DUE_THIS_MONTH`, `n = 0`, mức cần, dự phóng, trạng thái đều `null` | design quy tắc 1 và 5 | 30/11/2026 |
| D6 | Dự phóng = `allocated × g` + `planned × (g − 1) / r`, `r = annual / 100 / 12`, `g = (1 + r)^n`; `r = 0` thì phần đóng góp là `planned × n`. Đóng góp cuối mỗi tháng, ghép lãi theo tháng | design phase 1 "Frozen decomposition" | `600.000.000 × 1,3297658588 = 797.859.515,30 ₫` |
| D7 | Mức cần mỗi tháng = `(target − allocated × g) × r / (g − 1)`, `r = 0` thì `(target − allocated) / n`; ≤ 0 thì 0; còn lại **làm tròn lên bội 1.000 ₫** | design quy tắc 3, ledger "làm tròn lên nghìn đồng" | `47.799.023,88 → 47.800.000 ₫` |
| D8 | Tỷ lệ bao phủ = `planned / required × 100`; required = 0 thì 100. Trạng thái so **chính xác** `planned × 100 ≥ ngưỡng × required` (required là số đã làm tròn lên, như design): ≥ 100 `ON_TRACK`, ≥ 80 `AT_RISK`, còn lại `BEHIND` | design quy tắc 4; fixture design dùng số đã làm tròn | `9.999.999 / 10.000.000` → `AT_RISK` |
| D9 | Ngưỡng 100 và 80 là property `rikkaus.goals.projection.on-track-min-coverage-percent` và `at-risk-min-coverage-percent`; app không khởi động nếu thiếu hoặc không thỏa `0 < at-risk < on-track` | brief; design "ngưỡng tham chiếu của hệ thống" | `application.yml` |
| D10 | Thiếu `plannedMonthlyContribution` → dự phóng, tỷ lệ, trạng thái `null`, lý do `PLANNED_MONTHLY_CONTRIBUTION_MISSING`; mức cần vẫn tính nếu có lợi suất | design quy tắc 5, goal g7 | `Quỹ học tiếp cho bản thân` cần `9.025.000 ₫` |
| D11 | Thiếu `expectedAnnualReturnPercent` → mức cần (trừ khi đã đủ), dự phóng, tỷ lệ, trạng thái `null`, lý do `EXPECTED_ANNUAL_RETURN_MISSING`; **không** coi là 0% | design quy tắc 5, "không bao giờ 0 thay cho không khả dụng" | `Du lịch Nhật Bản` |
| D12 | Đã phân bổ ≥ mục tiêu khi còn tháng góp: mức cần `0`, tỷ lệ `100`, trạng thái `ON_TRACK` nếu có `planned` | design code: `remaining = 0 → need = 0` | `Quỹ mua xe máy` |
| D13 | Làm tròn: phép trung gian `MathContext.DECIMAL128` (34 chữ số); hai phần dự phóng làm tròn `HALF_UP` về 2 số lẻ **rồi** mới cộng, nên `fromAllocated + fromContributions = total` đúng từng đồng; `gapToTarget = total − target`; phần trăm `scale 2` làm tròn **xuống** (không bao giờ báo dư tiến độ); `monthlyRatePercent` (`scale 6`) và `growthFactor` (`scale 10`) `HALF_UP` chỉ để hiển thị cách tính; lãi tháng ghi bằng phần trăm vì `BigDecimal` 0 với scale > 6 ra JSON dạng `0E-10` | design: "breakdown reconciles to the total" | `121,37 %`, design hiển thị `121,4 %` |
| D14 | `assumptions` là danh sách `{key, source, value}` với ba nguồn `USER_ENTERED`, `SYSTEM`, `NOT_MODELLED` theo ledger design; `value` là chuỗi chuẩn hoặc `null`; `disclaimer` khẳng định lợi suất không được đảm bảo; `method` mô tả công thức bằng chữ | design phase 1 "Frozen assumption ledger"; backbone §7 | `INFLATION`, `NOT_MODELLED` |
| D15 | Danh sách: cùng thứ tự mặc định của `GET /goals` (ngày đích gần nhất, rồi tên, rồi id); không tham số lọc | `GoalService.listFor` với `GoalFilter` mặc định | |
| D16 | Goal không tồn tại hoặc của người khác → `404 not-found` cùng `type`, `title`, `status`, `detail`, chỉ khác `instance` và `correlationId`; id sai dạng → `400 malformed-request` như `/goals/{goalId}` | `OwnershipGuard`, G58 của #86 | |

## Contract

```text
GET /api/v1/goal-projections            -> 200 [GoalProjectionResponse]
GET /api/v1/goal-projections/{goalId}   -> 200 GoalProjectionResponse

GoalProjectionResponse {
  goalId, goalName, currency: VND, asOfDate,
  timeline: ACTIVE | DUE_THIS_MONTH | EXPIRED, targetReached,
  completionPercent, remainingAmount, remainingMonths, daysPastTargetDate | null,
  requiredMonthlyContribution | null, contributionCoveragePercent | null,
  status: ON_TRACK | AT_RISK | BEHIND | null,
  projectedAtTargetDate { fromAllocated, fromContributions, total, gapToTarget } | null,
  calculation { monthlyRatePercent, growthFactor } | null,
  unavailableReasons [TARGET_DATE_PASSED | NO_CONTRIBUTION_MONTH |
                      PLANNED_MONTHLY_CONTRIBUTION_MISSING | EXPECTED_ANNUAL_RETURN_MISSING],
  assumptions [{key, source: USER_ENTERED | SYSTEM | NOT_MODELLED, value | null}],
  method { name, remainingMonths, monthlyRate, projectedAmount, requiredMonthlyContribution, status },
  disclaimer
}
```

Lỗi: `400 malformed-request` (id sai dạng), `401`, `404 not-found`, `405`, `406`.

## Phases

| Phase | Tên | Status | Kết quả |
|---|---|---|---|
| 1 | [Danh sách test scenario](./phase-01-test-scenarios.md) | Complete | case với kỳ vọng tính độc lập, viết trước khi code |
| 2 | [Service, properties và route](./phase-02-projection-service-and-http.md) | Complete | package `goals.projection` |
| 3 | [Contract và docs](./phase-03-contract-and-docs.md) | Complete | `openapi.json`, `OpenApiContractIT`, `api-contract-conventions.md` |
| 4 | [Xác minh thủ công](./phase-04-manual-verification.md) | Complete | guide, report evidence, commit, push, PR body |

## Acceptance criteria

- [x] Mọi case ở phase 1 đạt trên commit cuối, ghi status và body thật.
- [x] `clean verify` xanh không flag.
- [x] Sáu goal fixture #87 cho đúng mức cần mỗi tháng và trạng thái của design.
- [x] `PUT /goals/{goalId}` đổi input thì lần `GET` dự phóng kế tiếp đổi theo.
- [x] Không output nào trả 0 thay cho không khả dụng; lý do được nêu.
- [x] User B không thấy gì của A (404 cùng `type`, `title`, `status`, `detail`; chỉ khác `instance` và `correlationId`); 401 ba kiểu; 405; 406.
- [x] Hai lần gọi cùng input cho body giống từng byte.
- [x] `openapi.json`, `OpenApiContractIT`, `docs/api-contract-conventions.md` cập nhật.

## Verification gap

- Không automated test nào bảo vệ phép tính; regression chỉ bị phát hiện khi chạy lại
  `docs/manual-test-guide-goal-projection.md`. Kỳ vọng được tính bằng một máy tính Python `decimal` độc lập
  với code production, nhưng cùng tác giả, nên một hiểu sai chung về design sẽ không bị phát hiện. Sáu dòng
  fixture #87 khớp với số design công bố là kiểm chứng độc lập duy nhất.
- "Hôm nay" lấy từ đồng hồ hệ thống và không có `asOf`; lần chạy chỉ kiểm được ngày 23/11/2026. Biên đổi
  ngày lúc nửa đêm giờ Việt Nam (17:00 UTC), ngày cuối tháng và tháng 12 sang tháng 1 không được chạy thật.
- Goal có số đã phân bổ **lớn hơn** mục tiêu chỉ tạo được bằng SQL (API từ chối); không có case riêng.
  E4 (bằng mục tiêu) là case gần nhất.
- Biên lợi suất 30 % và số tiền 17 chữ số nguyên (giới hạn của `GoalRequest`) không có case; độ chính xác
  34 chữ số chỉ được suy ra, chưa đo với `n` lớn hơn 183 tháng.
- Hiệu năng của danh sách với nhiều goal không được đo; mỗi phần tử tính độc lập, không có truy vấn thêm.
- Ba goal quá hạn có ngày đích đặt bằng `UPDATE` trong database, vì API không cho tạo; đường ghi đó không
  phải đường người dùng đi.
- Lần chạy dùng script Python gửi cùng request với guide; các khối `bash` trong guide không được chạy
  nguyên văn.
- `apps/mobile/src/api/generated/*` chưa được sinh lại từ `openapi.json` mới: worktree không cài dependency
  của mobile và CI chỉ chạy `backend.yml`. Frontend #88 chạy `npm run api:generate` khi dùng route này.

## Câu hỏi cho Product Owner / Feature Contract #25

1. **Ngày đích đã qua nhưng còn trong tháng này** (ví dụ 15/11/2026 khi hôm nay là 23/11/2026): API trả
   `EXPIRED` vì so theo ngày (D4). Code của design #87 so theo tháng nên sẽ hiển thị "Không còn tháng đóng
   góp nào". Giữ cách của API?
2. **Không có tham số `asOf`** (D2): dự phóng luôn tính cho hôm nay vì số đã phân bổ là số của hôm nay.
   Có cần xem lại dự phóng "như ngày X" không?
3. **Hiển thị tỷ lệ**: API trả 2 số lẻ làm tròn **xuống** (121,37 %), design hiển thị 1 số lẻ làm tròn
   thường (121,4 %). Frontend #88 làm tròn lại 1 số lẻ thì ra đúng số design, nhưng có thể hiện 100,0 %
   cạnh `AT_RISK` khi tỷ lệ thật là 99,96 %. Chấp nhận, hay frontend cũng làm tròn xuống?
4. **Goal đã đủ tiền nhưng chưa nhập đóng góp**: theo design, trạng thái là `null` ("Không khả dụng") dù
   mức cần là 0 ₫. Có muốn coi goal đã đủ là `ON_TRACK` bất kể đóng góp không?
5. **Nhãn vòng đời quá hạn** ("Quá hạn" / "Đã đạt mục tiêu") là đề xuất của design #87, chưa có trong
   Feature Contract #25. API trả `timeline: EXPIRED` cùng `targetReached` để frontend chọn nhãn; cần PO
   chốt câu chữ.

## Risks and rollback

Rủi ro chính là đọc dự phóng như cam kết; response mang `disclaimer`, nguồn của từng giả định và các yếu tố
`NOT_MODELLED`. Rollback là xóa package `goals.projection`, khối `rikkaus.goals.projection` trong
`application.yml` và hai path trong contract; không có dữ liệu hay migration để hoàn tác.
