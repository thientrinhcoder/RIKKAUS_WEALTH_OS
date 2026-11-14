package com.rikkaus.wealth.onboarding.application;

import com.rikkaus.wealth.onboarding.domain.OnboardingProgress;
import com.rikkaus.wealth.onboarding.domain.OnboardingStatus;
import com.rikkaus.wealth.onboarding.domain.OnboardingStep;
import com.rikkaus.wealth.onboarding.domain.RecordedSteps;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * The checklist as the service answers it: stored progress merged with what the records say.
 *
 * <p>A user with no progress row reads as step one, in progress, nothing reviewed. That is the same
 * state a freshly inserted row holds, so the row's absence is never visible to a client.
 */
public record OnboardingView(
        OnboardingStatus status,
        int currentStep,
        List<StepState> steps,
        Instant dashboardReviewedAt,
        Instant completedAt,
        Instant dismissedAt) {

    /** One step and whether it is done right now. */
    public record StepState(OnboardingStep step, boolean done) {}

    static OnboardingView of(OnboardingProgress progress, RecordedSteps records) {
        Instant reviewedAt = progress == null ? null : progress.getDashboardReviewedAt();
        List<StepState> steps =
                Arrays.stream(OnboardingStep.values())
                        .map(step -> new StepState(step, isDone(step, records, reviewedAt)))
                        .toList();
        if (progress == null) {
            return new OnboardingView(
                    OnboardingStatus.IN_PROGRESS, OnboardingStep.FIRST_NUMBER, steps, null, null, null);
        }
        return new OnboardingView(
                progress.status(),
                progress.getCurrentStep(),
                steps,
                reviewedAt,
                progress.getCompletedAt(),
                progress.getDismissedAt());
    }

    private static boolean isDone(OnboardingStep step, RecordedSteps records, Instant reviewedAt) {
        return switch (step) {
            case EXCHANGE_RATE -> Boolean.TRUE.equals(records.getExchangeRateDeclared());
            case CASH_AND_BANK_ACCOUNTS -> Boolean.TRUE.equals(records.getCashOrBankAccountAdded());
            case MAJOR_ASSETS -> Boolean.TRUE.equals(records.getMajorAssetAdded());
            case LIABILITIES -> Boolean.TRUE.equals(records.getLiabilityAdded());
            case RECURRING_CASH_FLOWS -> Boolean.TRUE.equals(records.getRecurringCashFlowAdded());
            case DASHBOARD_REVIEW -> reviewedAt != null;
            case FIRST_GOAL -> Boolean.TRUE.equals(records.getGoalAdded());
        };
    }
}
