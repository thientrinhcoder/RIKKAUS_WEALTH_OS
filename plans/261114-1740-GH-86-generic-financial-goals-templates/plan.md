---
title: "Issue #86 generic financial goals & templates"
description: "Xây slice goals: bảng wealth.goals, một model chung cho bảy template, resource /api/v1/goals và /api/v1/goal-templates, ownership server-side, validation RFC 9457, contract OpenAPI."
status: completed
priority: P1
effort: "3d"
issue: 86
branch: thientrinhcoder/feat/issue-86-generic-financial-goals-templates
tags: [backend, api, migration, goals]
blockedBy: []
blocks: [85, 89, 92, 101, 104]
created: 2026-11-14
---

# Issue #86 generic financial goals & templates

## Outcome

Người dùng pilot tạo, liệt kê, đọc, sửa và xóa mục tiêu tài chính của chính mình qua
`/api/v1/goals`, với bảy template dùng chung một model, tiền là số thập phân chính xác, ownership
kiểm tra server-side, lỗi RFC 9457 có `errors` nêu tên field, và contract OpenAPI để Frontend #85
làm việc. Bảy template cùng giá trị gợi ý mặc định của design đã duyệt được công bố qua
`/api/v1/goal-templates` để client không phải hard-code số liệu sản phẩm.

## Constraints

- Spring Boot modular monolith, PostgreSQL + Flyway, `BigDecimal`/`NUMERIC`, prefix `/api/v1`,
  lỗi RFC 9457 theo `ProblemType`, ownership qua `CurrentUser` + `OwnershipGuard`, record của user khác
  trả `not-found` byte-identical với record không tồn tại.
- Slice `goals` chỉ phụ thuộc `shared`. `ArchitectureRulesTest` fail nếu vi phạm, nên không có tỷ giá,
  không đọc tài sản/công nợ/dòng tiền để điền sẵn số tiền; backend chỉ nêu *cơ sở* gợi ý.
- Theo chỉ đạo Product Owner: **không viết unit test/integration test mới**. Phase 1 là danh sách test
  scenario viết trước khi code; verification là chạy thật qua HTTP vào PostgreSQL Docker và ghi
  evidence. Hai inventory sẵn có (`ApplicationContextIT` bảng, `OpenApiContractIT` path) vẫn phải mở
  rộng vì chúng liệt kê chính xác và sẽ fail.
- Migration là `V9__goals.sql`. Ban đầu là `V8` (lúc đó `main` dừng ở V4 và V5–V7 đã dành cho các PR
  khác); khi merge sau #59 (`V6`), #62 (`V7`) và #71 (`V8`) được đánh số lại thành `max(main)+1 = V9`.

## Non-goals

- Mọi output tính toán — phần trăm hoàn thành, số còn lại, thời gian còn lại, mức đóng góp cần thiết,
  trạng thái đúng kế hoạch/có rủi ro/chậm kế hoạch — thuộc #89. Response không có field tính toán.
- Nhận định theo quy tắc (#92), search/filter (#101), xóa an toàn/undo (#104), tỷ giá và chuyển đổi.
- Liên kết đóng góp vào dòng tiền, mục tiêu nhiều loại tiền, mốc con, chia sẻ mục tiêu: design đã duyệt
  ghi ngoài phạm vi.
- Bảng template riêng. Template là giá trị `type` cộng bộ gợi ý mặc định cố định trong code.

## Quyết định thiết kế chốt từ design đã duyệt

Nguồn: `docs/generic-financial-goals-templates-review.html` mục 04 ("Bộ trường chung cho mọi loại
mục tiêu", "Bảy mẫu và thứ chúng điền sẵn") và `RIKKAUS_WEALTH_OS_MVP_BACKBONE.md:302-330`.

| Quyết định | Giá trị | Căn cứ |
|---|---|---|
| Enum `GoalType` | `EMERGENCY_FUND`, `HOME_PURCHASE`, `EDUCATION`, `RETIREMENT`, `NET_WORTH_GROWTH`, `DEBT_REDUCTION`, `OTHER_GOAL` | Bảy mẫu của backbone, đặt tên theo kiểu `OTHER_ASSET` của slice assets |
| `name` | bắt buộc, ≤ 120 ký tự, làm sạch như assets (NUL bị từ chối, mọi space separator Unicode là blank) | Design "Không để trống"; giới hạn và cách làm sạch kế thừa #53 |
| `type` | bắt buộc; **được đổi khi PUT** | Design: template chỉ điền sẵn giá trị, không thêm bớt trường; không có trường riêng theo type nên đổi type vô hại. Khác assets (category đóng băng vì #54 có trường riêng theo category) |
| `targetAmount` | bắt buộc, `> 0`, `NUMERIC(19, 2)` | Design "Lớn hơn 0" |
| `currentAllocatedAmount` | tùy chọn, `≥ 0`, `≤ targetAmount`; **vắng mặt lưu `0.00`** | Design "Từ 0 trở lên, không vượt số tiền mục tiêu". Không có phân bổ nghĩa là chưa dành ra đồng nào, nên 0 là sự thật, không phải dữ liệu bịa; giữ cột NOT NULL để #89 tính phần trăm hoàn thành không gặp null |
| `targetDate` | bắt buộc, **sau hôm nay** theo `Asia/Ho_Chi_Minh` khi tạo; khi PUT chỉ kiểm tra nếu khác giá trị đã lưu | Design "Phải sau ngày tham chiếu"; ticket nói "không trong quá khứ" — chọn nghiêm hơn theo design. Giữ ngày cũ khi sửa được phép để người dùng vẫn sửa được mục tiêu đã quá hạn (design ghi "Quá hạn" là câu hỏi mở của Feature Integrator) |
| `plannedMonthlyContribution` | tùy chọn, `≥ 0`, `NUMERIC(19, 2)`; vắng mặt lưu `NULL` | Design "Từ 0 trở lên; thiếu thì trạng thái là Không khả dụng" — null phải phân biệt được với 0 cho #89 |
| `expectedAnnualReturnPercent` | tùy chọn, `0.00`–`30.00`, `NUMERIC(5, 2)`; vắng mặt lưu `NULL` | Design "Từ 0% đến 30%", "Để trống hoặc 0 nếu không muốn giả định lợi suất" |
| `currency` | cố định `VND`, lưu trong bảng với CHECK, trả về trong response, **không nhận từ request** | Design: mọi trường tiền là "Tiền VND", "Mục tiêu nhiều loại tiền" ngoài phạm vi; backbone base currency VND. Lưu như `user_settings.base_currency` để loại tiền thứ hai là một migration có review |
| Thứ tự list | `targetDate` tăng dần, rồi `name` | Mục tiêu gần hạn nhất lên trước; ổn định giữa hai request |
| `PUT` | thay thế toàn bộ: field tùy chọn bỏ qua bị xóa | Cùng semantics với assets |
| Templates | `GET /api/v1/goal-templates` trả 7 phần tử: `type`, `defaultExpectedAnnualReturnPercent`, `defaultHorizonMonths`, `amountPrefill` | Bảng "Bảy mẫu và thứ chúng điền sẵn": 3%/18, 6%/60, 5%/72, 8%/180, 7%/48, 0%/48, 0%/24. Cơ sở số tiền (6 tháng chi phí, tài sản ròng, tổng dư nợ) chỉ được *nêu tên* vì slice không được đọc slice khác |
| Nhãn tiếng Việt | không có trong contract | Như assets: client ánh xạ từ constant sang copy của design |

`amountPrefill` nhận một trong: `NONE`, `TARGET_FROM_SIX_MONTHS_OF_EXPENSES` (quỹ dự phòng),
`ALLOCATED_FROM_CURRENT_NET_WORTH` (tăng trưởng tài sản ròng), `TARGET_FROM_TOTAL_LIABILITIES` (giảm nợ).

## Deliverables

- `services/api/src/main/resources/db/migration/V9__goals.sql`
- `services/api/src/main/java/com/rikkaus/wealth/goals/**`
- `services/api/openapi/openapi.json` regenerate
- `ApplicationContextIT` thêm bảng `goals`; `OpenApiContractIT` thêm ba path
- `docs/api-contract-conventions.md` thêm mục goals
- `docs/manual-test-guide-goals.md`
- `plans/reports/manual-verification-261114-1740-GH-86-generic-financial-goals-templates.md`
- `.gitignore` thêm ngoại lệ cho thư mục plan này và report

## Phases

| Phase | Tên | Trạng thái | Phụ thuộc | Kết quả |
|---|---|---|---|---|
| 1 | [Danh sách test scenario](./phase-01-test-scenarios.md) | Completed | — | Mọi case viết trước khi có dòng code nào, với dữ liệu Việt Nam thật và kết quả mong đợi |
| 2 | [Schema và domain](./phase-02-schema-and-domain.md) | Completed | 1 | `wealth.goals` với CHECK; `Goal`, `GoalType` (mang mặc định template), `GoalAmountPrefill`, `GoalFields`, `GoalRepository` |
| 3 | [Service và HTTP](./phase-03-service-and-http.md) | Completed | 2 | Năm route goals + một route templates, validation có tên field |
| 4 | [Contract và inventories](./phase-04-contract-and-inventories.md) | Completed | 3 | `openapi.json`, hai inventory, conventions doc; `verify` xanh |
| 5 | [Manual verification](./phase-05-manual-verification.md) | Completed | 4 | Guide chạy lại được và evidence của lần chạy này |

## Acceptance Criteria

- [x] Bảy type tạo được và có trong contract; không có type thứ tám qua API hay qua `psql`.
- [x] Từng field bắt buộc thiếu trả `validation-failed` nêu đúng field đó.
- [x] `targetAmount` 0/âm/quá precision, `currentAllocatedAmount` âm hoặc vượt target, contribution âm,
      return âm hoặc > 30, `targetDate` không sau hôm nay: mỗi cái 400 nêu tên field.
- [x] Record của user khác: read/update/delete trả `not-found` byte-identical (trừ `instance`,
      `correlationId`) với id không tồn tại; row không đổi.
- [x] Không header / token hết hạn / token sai chữ ký: 401 giống nhau trừ `correlationId`.
- [x] UUID sai, method sai, body không JSON, JSON cụt, Accept lạ: đều `application/problem+json`.
- [x] Xóa rồi đọc → 404; xóa lần hai → 404; 4 delete đồng thời → 1×204, còn lại 404, không 500.
- [x] `psql` xác nhận kiểu cột `numeric(19,2)`/`numeric(5,2)`, giá trị `15000000000.00` nguyên vẹn, và
      từng CHECK từ chối insert trực tiếp.
- [x] `GET /api/v1/goal-templates` trả đúng 7 template với giá trị mặc định khớp bảng design.
- [x] `./mvnw verify` xanh không dùng `-Dopenapi.update`.
- [x] Không có field tính toán nào của #89 trong response.

## Verification gap

Không có automated test nào chạm vào `/api/v1/goals` hay `/api/v1/goal-templates`. Đó là chỉ đạo của
Product Owner cho task này, cùng tư thế #50 và #53 đã ship. Hậu quả: regression trong slice này chỉ bị
phát hiện khi chạy lại `docs/manual-test-guide-goals.md`. Bộ test sẵn có vẫn bắt được: một bảng không
migration nào khai báo (`ApplicationContextIT`), một path công bố mà không khai báo hoặc ngược lại
(`OpenApiContractIT`), controller nhận owner id từ client, field tiền kiểu `double`, slice chạm slice
khác (`ArchitectureRulesTest`).

## Câu hỏi còn mở cho Product Owner / Feature Integrator

1. Đổi `type` khi sửa mục tiêu được phép (quyết định ở đây). Nếu PO muốn đóng băng như category của
   assets, đây là một dòng thay đổi trong `GoalRequest`/`Goal`.
2. `currentAllocatedAmount` vắng mặt được lưu `0.00` thay vì `NULL`. Nếu #89 cần phân biệt "chưa nhập"
   với "0", cần đổi cột thành nullable.
3. Mục tiêu đã quá hạn vẫn sửa được miễn không đổi `targetDate` sang một ngày quá khứ khác. Hành vi
   quá hạn đầy đủ thuộc Feature Integrator (design ghi là câu hỏi mở).

<!-- slug: issue-86-generic-financial-goals-templates -->

## Kết quả

Giao ngày 14/11/2026 tại commit `056873f`. `./services/api/mvnw -f services/api/pom.xml verify` xanh
không dùng flag cập nhật: 81 unit test và 50 integration test. Hành vi được ghi trong
[`plans/reports/manual-verification-261114-1740-GH-86-generic-financial-goals-templates.md`](../reports/manual-verification-261114-1740-GH-86-generic-financial-goals-templates.md):
81 case (70 HTTP, 11 PostgreSQL), tất cả đúng như thiết kế ở lần chạy đầu trên working tree và ở lần
chạy lại trên commit, không case nào phải sửa code. Ba quyết định sản phẩm nằm ngoài design đã duyệt
được ghi ở mục "Câu hỏi còn mở" phía trên để Product Owner xác nhận.
