---
phase: 2
title: "shared/validation và các slice"
status: completed
priority: P1
effort: "0.6d"
dependencies: [1]
---

# Phase 2: `shared/validation` và các slice

## Mục tiêu

Một định nghĩa cho mỗi rule dùng chung, và đóng các lỗ hổng `[FIX]` của phase 1 mà không đổi `field`,
rule nghiệp vụ hay contract đọc.

## Thay đổi

| File | Thay đổi |
|---|---|
| `shared/validation/TextInput.java` (mới) | `optional`, `required`, `strip`: blank = Java whitespace + mọi `Zs` + `U+200B`, `U+2060`, `U+FEFF`; `U+0000` → `must not contain a null character` |
| `shared/validation/EnteredDates.java` (mới) | `today(clock)` ở `Asia/Ho_Chi_Minh`; `parse` chỉ nhận `YYYY-MM-DD` năm 0001–9999; hằng `CALENDAR_DATE`, `CALENDAR_MONTH`, `NOT_IN_FUTURE` |
| `shared/validation/FieldErrors.java` (mới) | `error`, `validationFailed`, `rejectIfAny` |
| `shared/validation/StrictDateConfiguration.java` (mới) | `JacksonModule` đọc mọi `LocalDate` trong body bằng `EnteredDates.parse`; chuỗi rỗng vẫn là `null` |
| `application.yml` | `spring.jackson.datatype.enum.fail-on-numbers-for-enums: true`; `spring.web.locale: en`, `locale-resolver: fixed` |
| `assets/application/InputRules.java` | Chỉ còn `rateSnapshotErrors`; `AssetService`, `AssetValuationService` gọi `shared/validation` |
| `liabilities`, `cashflow`, `goals`, `feedback`, `settings` service | Bỏ bản sao vòng lặp làm sạch chuỗi, `ZoneId` và builder lỗi; gọi `shared/validation` |
| `identity` | `UpdateProfileRequest` có message tường minh; `IdentityService.updatePreferredDisplayName` và `LocalDevSignInService` làm sạch tên bằng `TextInput` |
| `settings/api/DeclareExchangeRateRequest.java` | Message tường minh cho `@NotNull`, `@DecimalMin`, `@Digits` |
| `cashflow/forecast/application/ForecastHorizon.java`, `cashflow/reporting/application/ReportMonth.java` | Dùng chung `EnteredDates.CALENDAR_MONTH` |

Không đụng: `QueryParameters`, `SearchText`, `OccurrenceWindow` (list và query đang được #107 audit;
các lỗi của chúng đã đúng hình dạng), slice `snapshot`, migration.

## Validation

`./mvnw clean verify` không flag xanh; `ArchitectureRulesTest` xác nhận slice chỉ phụ thuộc `shared`.
