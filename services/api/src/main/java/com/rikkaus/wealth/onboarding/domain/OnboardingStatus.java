package com.rikkaus.wealth.onboarding.domain;

/**
 * Where the user stands in the onboarding flow as a whole.
 *
 * <p>Not stored as a column. It is read from which of {@code completed_at} and {@code dismissed_at} is
 * set, and the table refuses a row with both, so there is exactly one status per row and no combination
 * a client could not render.
 */
public enum OnboardingStatus {
    /** Neither completed nor dismissed. The state of every user who has not decided. */
    IN_PROGRESS,
    /** The user finished the guided flow. Not gated on every step being done; see the service. */
    COMPLETED,
    /** The user chose to skip the guided flow. Reopened by setting the status back to IN_PROGRESS. */
    DISMISSED
}
