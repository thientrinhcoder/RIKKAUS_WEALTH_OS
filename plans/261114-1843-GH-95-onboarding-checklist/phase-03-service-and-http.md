---
phase: 3
title: "Service và HTTP"
status: completed
priority: P1
effort: "0.3d"
dependencies: [2]
---

# Phase 3: Service và HTTP

## Files

- Tạo `.../onboarding/application/OnboardingService.java`, `OnboardingView.java`
- Tạo `.../onboarding/api/OnboardingController.java`, `OnboardingResponse.java`, `UpdateOnboardingRequest.java`

## Steps

1. `OnboardingService.checklistFor(userId)` read-only: đọc dòng (nếu có) + sáu cờ, ghép thành view.
2. `OnboardingService.update(userId, currentStep, dashboardReviewed, status)`: tất cả null → trả view, không
   ghi; ngược lại `ensureExists`, `findById`, `OwnershipGuard.requireOwned`, áp dụng, lưu, trả view.
3. Controller `GET`/`PATCH` dưới `ApiPaths.V1 + "/onboarding"`, `@Valid` với `@Min(1) @Max(7)`; ví dụ
   OpenAPI cho user mới và user đang ở bước 3.
4. Kiểm tra số thập phân cho `currentStep` (O27) — nếu Jackson cắt `2.5` thành `2`, từ chối ở boundary.

## Validation

Smoke `GET`/`PATCH` local trước phase 5.
