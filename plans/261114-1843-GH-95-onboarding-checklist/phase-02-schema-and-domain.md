---
phase: 2
title: "Schema và domain"
status: completed
priority: P1
effort: "0.3d"
dependencies: [1]
---

# Phase 2: Schema và domain

## Files

- Tạo `services/api/src/main/resources/db/migration/V11__onboarding_progress.sql`
- Tạo `services/api/src/main/java/com/rikkaus/wealth/onboarding/domain/OnboardingProgress.java`
- Tạo `.../onboarding/domain/OnboardingStep.java`, `OnboardingStatus.java`, `RecordedSteps.java`
- Tạo `.../onboarding/domain/OnboardingProgressRepository.java`

## Steps

1. Bảng `wealth.onboarding_progress`: `user_id UUID PK REFERENCES wealth.users ON DELETE CASCADE`,
   `current_step SMALLINT NOT NULL DEFAULT 1`, `dashboard_reviewed_at`, `completed_at`, `dismissed_at`
   (`TIMESTAMPTZ` nullable), `created_at`, `updated_at`. CHECK `current_step BETWEEN 1 AND 7`; CHECK không
   đồng thời `completed_at` và `dismissed_at`.
2. Entity `OnboardingProgress implements UserOwned`, owner là khóa chính; mutator `viewStep`,
   `recordDashboardReview(boolean, now)`, `changeStatus(status, now)`.
3. `OnboardingStep` enum bảy bước, số thứ tự 1–7.
4. Repository hẹp: `findById`, native `ensureExists` (`INSERT ... ON CONFLICT DO NOTHING`), và một native
   query trả sáu cờ `EXISTS` qua projection `RecordedSteps`.

## Validation

`clean verify` xanh (`ddl-auto: validate` trong `ApplicationContextIT` sau khi thêm bảng vào inventory).

## Risk

Native query chạm bảng slice khác: tên cột/category đổi sẽ không có compile error. Ghi trong Verification gap.
