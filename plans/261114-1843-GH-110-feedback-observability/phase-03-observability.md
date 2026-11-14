---
phase: 3
title: "Observability"
status: completed
priority: P1
effort: "0.5d"
dependencies: [1]
---

# Phase 3: Observability

## Mục tiêu

Một dòng log có cấu trúc cho mỗi request; JSON ngoài `local`; không đường log nào mang dữ liệu nhạy cảm.

## File tạo / sửa

- Tạo `shared/observability/RequestLoggingFilter.java`, `RequestLogFields.java`, `LogRedaction.java`,
  `RedactedMessageConverter.java`, `MessageFreeThrowableConverter.java`,
  `MessageFreeStackTracePrinter.java`, `RedactingJsonMembersCustomizer.java`
- Sửa `shared/observability/CorrelationIdFilterConfiguration.java` (đăng ký filter mới),
  `shared/security/BearerTokenAuthenticationFilter.java` (gắn userId),
  `shared/error/ApiExceptionHandler.java` (ghi tên lớp exception, không message)
- Tạo `services/api/src/main/resources/logback-spring.xml`
- Sửa `application.yml` (pattern, structured format, stack trace printer, customizer,
  `logServerErrorDetail=false`, banner), `application-local.yml` (banner)

## Bước

1. `RequestLogFields`: MDC key `userId`, attribute giữ userId qua các filter.
2. `RequestLoggingFilter`: đo thời gian, đọc `BEST_MATCHING_PATTERN_ATTRIBUTE`, status; health ở DEBUG.
3. Redaction: một nơi định nghĩa pattern che (JWT, `ya29.`, `Bearer`, email) và cách in throwable không
   message; ba adapter (converter pattern, printer JSON, customizer JSON) dùng chung.
4. `logback-spring.xml` chọn appender theo profile.

## Validation

Phase 5 phần O và J: grep log thật hai lần chạy, `jq` parse từng dòng.

## Rủi ro

`logback-spring.xml` thay cấu hình mặc định của Boot: phải include `defaults.xml` để giữ converter và
level mặc định. Test profile cũng ra JSON (không phải `local`) — chấp nhận, CI đọc được.
