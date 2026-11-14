---
title: "Issue #95 guided onboarding & financial-profile checklist"
description: "Xây slice onboarding: bảng wealth.onboarding_progress lưu tối thiểu tiến độ, resource /api/v1/onboarding trả checklist bảy bước suy ra từ record thật, ghi bước đang xem, đã xem dashboard, hoàn tất hoặc bỏ qua."
status: completed
priority: P1
effort: "1.5d"
issue: 95
branch: thientrinhcoder/feat/issue-95-onboarding-checklist
tags: [backend, api, migration, onboarding]
blockedBy: []
blocks: [94]
created: 2026-11-14
---

# Issue #95 guided onboarding & financial-profile checklist

## Outcome

Người dùng pilot mở app và thấy checklist bảy bước của backbone (`RIKKAUS_WEALTH_OS_MVP_BACKBONE.md:379-387`),
mỗi bước `done` khi và chỉ khi record thật tương ứng đang tồn tại; quay lại app thì tiếp tục đúng bước
đã xem cuối; có thể đánh dấu đã xem dashboard, hoàn tất, bỏ qua và mở lại. Backend chỉ lưu tiến độ,
không sao chép số tiền, tên tài sản hay bất kỳ dữ liệu tài chính nào.

## Constraints

- Spring Boot modular monolith, PostgreSQL + Flyway, prefix `/api/v1`, lỗi RFC 9457 theo `ProblemType`,
  owner lấy từ access token qua `CurrentUser`, entity cài `UserOwned` và đi qua `OwnershipGuard`.
- Slice `onboarding` chỉ phụ thuộc `shared` (`ArchitectureRulesTest`). Trạng thái bước được đọc bằng
  một native query `EXISTS` trên bảng của slice khác, như `cashflow` và `liabilities` đã làm cho liên kết.
- Theo chỉ đạo Product Owner: **không viết unit test/integration test mới**. Phase 1 là danh sách test
  scenario viết trước khi code; verification là chạy thật qua HTTP vào PostgreSQL Docker và ghi
  evidence. Hai inventory (`ApplicationContextIT`, `OpenApiContractIT`) vẫn được mở rộng vì CI gate.
- Migration `V11__onboarding_progress.sql`. `V10` dành cho #74 nếu cần; khi merge sẽ đánh số lại thành
  `max(main)+1`.
- Design #93 chưa được PO duyệt. Contract chốt từ backbone, giữ nhỏ; mọi giả định ở mục cuối.

## Non-goals

- Dữ liệu mẫu, empty states, nội dung hướng dẫn (copy), nhãn tiếng Việt cho bước: client ánh xạ từ `key`.
- Search/filter, mọi tính toán tài chính (tổng tài sản, tài sản ròng, dòng tiền ròng).
- Slice dashboard: bước 6 chỉ là cờ client ghi nhận.
- Cổng chặn "hoàn tất" khi còn bước chưa xong (xem quyết định bên dưới).
- Chống ghi đè đồng thời (row version): hai `PATCH` chồng nhau thì người ghi sau thắng.

## Quyết định contract

| Quyết định | Giá trị | Căn cứ |
|---|---|---|
| Route | `GET /api/v1/onboarding`, `PATCH /api/v1/onboarding`. Singleton theo token, không có id trong path | Cùng kiểu `/settings/exchange-rate`: không có gì để thay thế mà chạm tới user khác |
| Tạo dòng | **Lazy khi ghi**: `GET` không bao giờ ghi; dòng được tạo ở `PATCH` đầu tiên có ít nhất một field | Không tạo dòng thừa cho user chỉ xem; khác `user_settings` (GET tạo) vì ở đây trạng thái mặc định suy ra hoàn toàn được |
| Bảy bước, `key` | `EXCHANGE_RATE`, `CASH_AND_BANK_ACCOUNTS`, `MAJOR_ASSETS`, `LIABILITIES`, `RECURRING_CASH_FLOWS`, `DASHBOARD_REVIEW`, `FIRST_GOAL` | Thứ tự backbone 1–7 |
| Bước 1 xong | `wealth.user_settings.usd_to_vnd_rate IS NOT NULL` | Đọc `GET /settings/exchange-rate` tạo dòng settings với rate null; tồn tại dòng ≠ đã khai báo |
| Bước 2 xong | có asset `category IN ('CASH','BANK_ACCOUNT')` | Chỉ đạo ticket |
| Bước 3 xong | có asset `category NOT IN ('CASH','BANK_ACCOUNT')`, gồm cả `TERM_DEPOSIT` | "các category khác"; câu hỏi PO #1 |
| Bước 4 xong | có bất kỳ dòng `wealth.liabilities` | |
| Bước 5 xong | có `wealth.cash_flow_entries` với `timing = 'RECURRING'` (thu hoặc chi, kể cả đã có `end_date`) | Câu hỏi PO #2 |
| Bước 6 xong | `dashboard_reviewed_at IS NOT NULL` do client ghi | Chưa có slice dashboard |
| Bước 7 xong | có bất kỳ dòng `wealth.goals` | |
| `currentStep` | số nguyên 1–7, mặc định 1, do client ghi; server không tự nhảy bước | "User resume đúng bước" = bước đã xem cuối |
| `status` | `IN_PROGRESS` / `COMPLETED` / `DISMISSED`, suy ra từ `completed_at`/`dismissed_at` (không bao giờ cả hai) | Một trạng thái duy nhất, không có tổ hợp mâu thuẫn |
| Hoàn tất không bị chặn | `COMPLETED` nhận được dù còn bước chưa xong | User không có nợ không bao giờ xong bước 4; chặn sẽ khóa họ. Câu hỏi PO #3 |
| `PATCH` | field vắng hoặc `null` = giữ nguyên; `{}` → 200 trả trạng thái hiện tại và không tạo dòng | Partial update, body nhỏ |
| `dashboardReviewed` | `true` ghi thời điểm lần đầu (gửi lại giữ nguyên thời điểm cũ); `false` xóa | Desired-state, idempotent |
| `status` khi ghi | `COMPLETED` đặt `completedAt` (giữ nếu đã có), xóa `dismissedAt`; `DISMISSED` ngược lại; `IN_PROGRESS` xóa cả hai | Mở lại sau khi bỏ qua |
| Tạo dòng đồng thời | `INSERT ... ON CONFLICT (user_id) DO NOTHING` rồi đọc lại | `PATCH` đầu tiên song song không được ra 500 |

Response mẫu:

```json
{
  "status": "IN_PROGRESS",
  "currentStep": 3,
  "steps": [
    {"step": 1, "key": "EXCHANGE_RATE", "done": true},
    {"step": 2, "key": "CASH_AND_BANK_ACCOUNTS", "done": true},
    {"step": 3, "key": "MAJOR_ASSETS", "done": false},
    {"step": 4, "key": "LIABILITIES", "done": false},
    {"step": 5, "key": "RECURRING_CASH_FLOWS", "done": false},
    {"step": 6, "key": "DASHBOARD_REVIEW", "done": false},
    {"step": 7, "key": "FIRST_GOAL", "done": false}
  ],
  "dashboardReviewedAt": null,
  "completedAt": null,
  "dismissedAt": null
}
```

## Deliverables

- `services/api/src/main/resources/db/migration/V11__onboarding_progress.sql`
- `services/api/src/main/java/com/rikkaus/wealth/onboarding/**`
- `services/api/openapi/openapi.json` regenerate
- `ApplicationContextIT` thêm `onboarding_progress`; `OpenApiContractIT` thêm `/api/v1/onboarding`
- `docs/api-contract-conventions.md` thêm mục onboarding
- `docs/manual-test-guide-onboarding.md`
- `plans/reports/manual-verification-261114-1843-GH-95-onboarding-checklist.md`
- `.gitignore` thêm ngoại lệ cho thư mục plan này và report

## Phases

| Phase | Tên | Trạng thái | Phụ thuộc | Kết quả |
|---|---|---|---|---|
| 1 | [Danh sách test scenario](./phase-01-test-scenarios.md) | Completed | — | 60 case viết trước khi có dòng code nào |
| 2 | [Schema và domain](./phase-02-schema-and-domain.md) | Completed | 1 | `V11`, `OnboardingProgress`, `OnboardingStep`, repository với native query |
| 3 | [Service và HTTP](./phase-03-service-and-http.md) | Completed | 2 | `GET`/`PATCH /api/v1/onboarding` |
| 4 | [Contract và inventories](./phase-04-contract-and-inventories.md) | Completed | 3 | `openapi.json`, hai inventory, conventions doc; `verify` xanh |
| 5 | [Manual verification](./phase-05-manual-verification.md) | Completed | 4 | Guide chạy lại được và evidence |

## Acceptance Criteria

- [x] User mới `GET` được 7 bước chưa xong, không lỗi, không có dòng nào trong `onboarding_progress`.
- [x] Mỗi bước 1, 2, 3, 4, 5, 7 chuyển sang `done` sau khi tạo record thật qua API của slice tương ứng,
      và quay về `done: false` khi record cuối cùng của loại đó bị xóa.
- [x] Bước 6, `currentStep`, hoàn tất, bỏ qua, mở lại ghi được và đọc lại đúng (resume đúng bước).
- [x] `currentStep` ngoài 1–7 → `validation-failed` nêu `currentStep`; kiểu sai → `malformed-request`.
- [x] User B không thấy và không đổi được tiến độ của A; record của A không làm bước của B `done`.
- [x] 401 giống nhau cho không header / hết hạn / sai chữ ký; 405, 415, 406, JSON cụt đều problem+json.
- [x] `psql`: bảng chỉ có cột tiến độ (không `numeric`, không `text`), CHECK từ chối bước ngoài 1–7 và
      hoàn tất + bỏ qua cùng lúc.
- [x] `./mvnw verify` xanh không flag.

## Verification gap

Không có automated test nào chạm vào `/api/v1/onboarding`. Đó là chỉ đạo của Product Owner, cùng tư thế
#53, #59, #62, #71, #86. Regression chỉ bị phát hiện khi chạy lại `docs/manual-test-guide-onboarding.md`.
Đặc biệt: native query đọc trực tiếp bảng của bốn slice khác. Khi một slice đổi tên cột hoặc giá trị
category, không có compile error nào; chỉ `GET /api/v1/onboarding` lỗi lúc chạy (cột sai) hoặc trả sai
`done` âm thầm (giá trị category đổi). Bộ test sẵn có vẫn bắt được: bảng không migration nào khai báo
(`ApplicationContextIT`), path công bố lệch (`OpenApiContractIT`), slice import slice khác
(`ArchitectureRulesTest`), và `ddl-auto: validate` khi khởi động bắt entity lệch cột.

## Câu hỏi cho Product Owner / Design #93

1. `TERM_DEPOSIT` (tiền gửi có kỳ hạn) đang tính cho bước 3 "tài sản lớn", không phải bước 2 "tiền mặt
   và tài khoản ngân hàng". Nếu design #93 coi sổ tiết kiệm là tài khoản ngân hàng, đổi một hằng số.
2. Bước 5 xong khi có **một** khoản thu hoặc chi định kỳ. Backbone viết "recurring income and expenses";
   nếu PO muốn cần cả thu lẫn chi, đổi query thành hai `EXISTS`.
3. Hoàn tất không bị chặn khi còn bước chưa xong (user không có nợ hoặc không có tài sản lớn). Nếu design
   muốn chặn, cần quy tắc "bước nào được bỏ qua", đó là quyết định sản phẩm.
4. Server không tự chuyển `currentStep` khi một bước vừa xong; client quyết định bước tiếp theo
   (ví dụ bước đầu tiên `done: false`). Nếu design muốn server gợi ý, thêm field suy ra `nextStep`.
5. Không có nhãn tiếng Việt trong contract; client ánh xạ `key` sang copy, ví dụ `EXCHANGE_RATE` →
   `Khai báo tỷ giá USD/VND`.

<!-- slug: issue-95-onboarding-checklist -->

## Kết quả

Giao ngày 14/11/2026 tại commit `bf72d18`. `./services/api/mvnw -f services/api/pom.xml clean verify`
xanh không dùng flag cập nhật: 81 unit test và 50 integration test. Hành vi được ghi trong
[`plans/reports/manual-verification-261114-1843-GH-95-onboarding-checklist.md`](../reports/manual-verification-261114-1843-GH-95-onboarding-checklist.md):
60 case (HTTP và PostgreSQL). Lần chạy đầu trên working tree có **1 case fail (O27)**: Jackson cắt
`currentStep: 2.5` thành 2 và trả 200; đã sửa bằng `WholeNumberDeserializer` trước khi commit. Lần chạy
thứ hai trên working tree và lần thứ ba trên commit đều 60/60. Sau khi merge `main` `3bd5f9d` (#74, không có migration) tại `26120c5`, `clean verify` xanh và 60/60
case pass lại trên jar đã merge. Năm câu hỏi sản phẩm ở mục phía trên
chờ Product Owner và design #93.
