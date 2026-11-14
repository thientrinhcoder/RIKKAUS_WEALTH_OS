---
title: "Issue #110 in-product feedback & privacy-safe observability"
description: "Slice feedback (POST/GET /api/v1/feedback, bảng wealth.feedback_entries) và log vận hành có cấu trúc, không bao giờ ghi body, token, email, số tiền hay tên bản ghi tài chính."
status: in-progress
priority: P1
effort: "2d"
issue: 110
branch: thientrinhcoder/feat/issue-110-feedback-observability
tags: [backend, api, migration, feedback, observability, logging]
blockedBy: []
blocks: [109]
created: 2026-11-14
---

# Issue #110 in-product feedback & privacy-safe observability

## Outcome

Người dùng pilot gửi góp ý từ bất kỳ màn hình nào qua `POST /api/v1/feedback`; góp ý gắn với user
hiện tại (từ access token) và mã màn hình, đọc lại được qua `GET /api/v1/feedback` (chỉ của chính mình).
API ghi một dòng log vận hành cho mỗi request với `correlationId`, `userId` (UUID), method, route
template, status và thời gian xử lý; ở mọi môi trường không phải `local` log là JSON một dòng một sự
kiện để CloudWatch đọc được, ở `local` vẫn là pattern dễ đọc. Không đường log nào chứa body, header
`Authorization`, cookie, token, email, số tiền hay tên tài sản/công nợ/mục tiêu.

## Constraints

- Modular monolith Spring Boot 4.1 / Java 25, PostgreSQL 18 + Flyway, prefix `/api/v1`, lỗi RFC 9457 theo
  `ProblemType`, owner chỉ lấy từ `CurrentUser`. Slice `feedback` chỉ phụ thuộc `shared`.
- Theo chỉ đạo Product Owner: **không viết unit test/integration test mới**. Phase 1 là danh sách test
  scenario viết trước khi code; verification là chạy thật qua HTTP vào PostgreSQL Docker, grep log thật
  và ghi evidence. Hai inventory sẵn có (`ApplicationContextIT`, `OpenApiContractIT`) vẫn phải mở rộng.
- `shared/observability` và `shared/error` được mở rộng, không viết lại. Semantics của
  `X-Correlation-Id` (header, MDC key `correlationId`, rule 8–64 ký tự `[A-Za-z0-9-]`) giữ nguyên.
- Migration `V12__feedback_entries.sql` (V10 dự kiến của #74, V11 của #95; đánh số lại `max(main)+1`
  khi merge nếu cần).
- Design #108 chưa được PO duyệt: contract chốt từ backbone (`RIKKAUS_WEALTH_OS_MVP_BACKBONE.md:376,393,486`)
  và issue; giả định ghi ở bảng dưới và mục câu hỏi.

## Non-goals

Dashboard giám sát, alerting, metrics endpoint mới ngoài `health`, ảnh chụp màn hình đính kèm, gửi
email, route admin, route sửa/xóa feedback, rate limit, đổi hạ tầng AWS (chỉ ghi docs rằng log JSON đi
vào log group CloudWatch sẵn có).

## Quyết định contract feedback (giả định, chờ design #108)

| Quyết định | Giá trị | Căn cứ |
|---|---|---|
| Route | `POST /api/v1/feedback` (201, body là bản ghi), `GET /api/v1/feedback` (200, mảng của chính user) | Issue; không route đọc từng bản ghi nên 201 **không** có `Location` |
| `screen` | bắt buộc, mã màn hình dạng route: `^[a-z0-9]+(?:[-/][a-z0-9]+)*$`, ≤ 64 ký tự, ví dụ `dashboard`, `catalog/assets`, `goals/new` | Free-text có ràng buộc thay vì allowlist: route của app (#109) còn thay đổi và allowlist cứng buộc backend deploy mỗi lần thêm màn hình. Pattern chặn khoảng trắng, dấu, `?`, `=`, `.`, nên không chở được câu văn, số tiền `4.200.000.000` hay query chứa token |
| `message` | bắt buộc, bỏ khoảng trắng hai đầu, không blank theo nghĩa Unicode (kể cả `U+00A0`), ≤ 2000 ký tự, từ chối `U+0000` | Cùng giới hạn và cách làm sạch như `notes` của cashflow; PostgreSQL không lưu được `U+0000` |
| `category` | tùy chọn: `BUG`, `IDEA`, `QUESTION`, `OTHER`; vắng mặt lưu `NULL`; giá trị lạ → `malformed-request` | Issue; không bịa giá trị mặc định |
| `createdAt` | do server đặt (UTC, ISO-8601) | Issue |
| Response | `id`, `screen`, `category`, `message`, `createdAt`; không `userId` | Client đã biết mình là ai; không lộ id nội bộ thừa |
| Thứ tự `GET` | `createdAt` giảm dần, rồi `id` | Mới nhất trước, ổn định |
| Nội dung `message` | lưu nguyên văn trong DB (người dùng tự viết, đọc lại được), **không bao giờ** ghi vào log | Backbone: không ghi financial payload vào log; nội dung góp ý là dữ liệu sản phẩm |

## Quyết định observability

| Quyết định | Giá trị |
|---|---|
| Định dạng | `logback-spring.xml`: profile `local` → appender pattern; mọi profile khác → `StructuredLogEncoder` format `logstash` (Spring Boot 4.1 hỗ trợ sẵn `ecs`/`logstash`/`gelf`). Chọn `logstash` vì nó không có field `error.message` riêng; stack trace đi qua printer tự viết |
| Dòng log request | filter mới `RequestLoggingFilter` (ngay trong `CorrelationIdFilter`), một dòng `INFO` `request completed` mỗi request với key-value `httpMethod`, `httpRoute` (template Spring MVC, `unmatched` khi chưa tới MVC), `httpStatus`, `durationMs`; health probe ghi ở `DEBUG` |
| `userId` | `BearerTokenAuthenticationFilter` đặt vào MDC + request attribute sau khi xác thực thành công; dòng request lấy lại từ attribute |
| Exception | Mọi throwable in ra **không có message**: `%safeEx` cho pattern, `MessageFreeStackTracePrinter` cho JSON. Chỉ tên lớp, chuỗi `Caused by` và frame |
| Redaction lưới an toàn | `%redactedMsg` cho pattern và `RedactingJsonMembersCustomizer` cho JSON che JWT, token Google `ya29.*`, `Bearer ...`, email trong mọi message/giá trị chuỗi |
| PostgreSQL | `logServerErrorDetail=false` trên driver để `Detail: Failing row contains (...)` không bao giờ vào message exception |
| Banner | tắt ở profile không phải `local` để stdout toàn JSON |

## Phases

| Phase | Tên | Trạng thái | Phụ thuộc | Kết quả |
|---|---|---|---|---|
| 1 | [Danh sách test scenario](./phase-01-test-scenarios.md) | Completed | — | 47 case feedback (F/P), 13 case log local (O), 7 case log JSON (J) |
| 2 | [Slice feedback](./phase-02-feedback-slice.md) | Pending | 1 | V12, entity, repository, service, controller |
| 3 | [Observability](./phase-03-observability.md) | Pending | 1 | Filter, MDC userId, redaction, logback-spring.xml, config |
| 4 | [Contract và inventories](./phase-04-contract-and-inventories.md) | Pending | 2, 3 | `openapi.json`, inventories, docs; `clean verify` xanh |
| 5 | [Manual verification](./phase-05-manual-verification.md) | Pending | 4 | Guide + report evidence hai lần chạy |

## Acceptance Criteria

- [ ] Feedback gắn user (từ token) và screen; body không gán được owner; B không thấy feedback của A.
- [ ] Mọi input sai (F12–F25) trả 400 có tên field hoặc `malformed-request`; `U+0000` không thành 500.
- [ ] 401 ba kiểu token giống nhau trừ `correlationId`; 405/404/415/400/406 đều problem+json.
- [ ] CHECK trong PostgreSQL từ chối dữ liệu sai cho mọi caller.
- [ ] Mỗi request có đúng một dòng log với correlationId, userId (khi có), method, route template, status, duration.
- [ ] Grep toàn bộ log hai lần chạy: 0 dòng cho mọi mồi M1–M6, kể cả trong 500 có kiểm soát.
- [ ] Log không `local` là JSON parse được từng dòng bằng `jq`; `local` vẫn là pattern đọc được.
- [ ] `X-Correlation-Id` giữ nguyên semantics.
- [ ] `./mvnw clean verify` xanh không flag.

## Verification gap

Theo chỉ đạo Product Owner, không có automated test nào chạm `/api/v1/feedback`, `RequestLoggingFilter`
hay các lớp redaction. Hậu quả: regression (ví dụ ai đó thêm `log.info("{}", request)` hoặc đổi pattern
log bỏ `%safeEx`) chỉ bị phát hiện khi chạy lại `docs/manual-test-guide-feedback-observability.md`. Bộ test
sẵn có vẫn bắt được bảng không được migration khai báo, path công bố lệch contract, slice phụ thuộc
slice khác. Redaction theo pattern chỉ nhận ra thứ có hình dạng (JWT, `ya29.`, `Bearer`, email); tên tài
sản và số tiền được bảo vệ bằng kỷ luật ở nguồn (không ghi body, exception không có message, driver không
kèm detail), không phải bằng pattern — một `log.info` mới in thẳng một field tài chính sẽ không bị chặn.
Các dòng JVM ghi thẳng ra stderr (cảnh báo của JVM) không đi qua Logback nên không phải JSON.

## Câu hỏi cho Product Owner / Design #108

1. `screen` là mã dạng route có ràng buộc thay vì allowlist cố định. Nếu design muốn allowlist (ví dụ
   đúng 9 màn hình MVP), đây là thay đổi một CHECK và một annotation.
2. `category` tùy chọn và lưu `NULL` khi bỏ trống; form có bắt buộc chọn loại góp ý không?
3. Nội dung `message` được lưu nguyên văn kể cả khi người dùng tự gõ số tiền hay tên tài sản. Design có
   cần dòng hướng dẫn "đừng nhập số liệu tài chính" (consent/content guidance của #108) không?
4. Không có rate limit cho feedback trong MVP; một tài khoản pilot có thể gửi không giới hạn.
5. Ai đọc feedback? Không có route admin; hiện chỉ đọc được bằng truy vấn trực tiếp PostgreSQL.

<!-- slug: issue-110-feedback-observability -->
