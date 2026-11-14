package com.rikkaus.wealth.onboarding.domain;

/**
 * Which of the record-backed steps the user's own records currently satisfy.
 *
 * <p>A projection of one native query, so each getter name matches a quoted column alias in {@link
 * OnboardingProgressRepository#recordedStepsOf}. The dashboard review has no getter: no record proves
 * it, so it comes from {@link OnboardingProgress} instead.
 */
public interface RecordedSteps {

    Boolean getExchangeRateDeclared();

    Boolean getCashOrBankAccountAdded();

    Boolean getMajorAssetAdded();

    Boolean getLiabilityAdded();

    Boolean getRecurringCashFlowAdded();

    Boolean getGoalAdded();
}
