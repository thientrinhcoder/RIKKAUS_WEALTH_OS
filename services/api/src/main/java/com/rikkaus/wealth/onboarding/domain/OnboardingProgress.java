package com.rikkaus.wealth.onboarding.domain;

import com.rikkaus.wealth.shared.security.UserOwned;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * One user's position in the guided onboarding: the step they last viewed and three moments.
 *
 * <p>Deliberately not a copy of anything financial. Which steps are done is never stored; it is read
 * from the records themselves on every request, so this row holds only what no record can tell — the
 * step the user was looking at, when they reviewed their first dashboard, and whether they completed or
 * dismissed the flow.
 *
 * <p>The owner is the primary key, as for {@code UserSettings}: the resource is a singleton per user, so
 * a second row is not something a check has to catch. Rows are created by {@link
 * OnboardingProgressRepository#insertIfAbsent}, never by this class, so there is no factory.
 *
 * <p>The owner is a bare {@link UUID}. The slice rule forbids {@code onboarding} from depending on
 * {@code identity}; the relationship lives in the foreign key {@code V11__onboarding_progress.sql}
 * declares.
 */
@Entity
@Table(name = "onboarding_progress", schema = "wealth")
public class OnboardingProgress implements UserOwned {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    /** {@code short} because the column is {@code SMALLINT} and schema validation compares the types. */
    @Column(name = "current_step", nullable = false)
    private short currentStep;

    @Column(name = "dashboard_reviewed_at")
    private Instant dashboardReviewedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "dismissed_at")
    private Instant dismissedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Required by JPA. Not for application use. */
    protected OnboardingProgress() {}

    /** Records the step the user is now looking at. The range is refused at the API boundary. */
    public void viewStep(int step, Instant now) {
        this.currentStep = (short) step;
        this.updatedAt = now;
    }

    /**
     * Records or withdraws the first-dashboard review.
     *
     * <p>Reporting it again keeps the original moment: the step is about the <em>first</em> review, so a
     * later one is not news. Withdrawing it clears the moment, so the step reads as not done.
     */
    public void recordDashboardReview(boolean reviewed, Instant now) {
        if (!reviewed) {
            this.dashboardReviewedAt = null;
        } else if (this.dashboardReviewedAt == null) {
            this.dashboardReviewedAt = now;
        }
        this.updatedAt = now;
    }

    /**
     * Moves the flow to the given status.
     *
     * <p>Completing clears a dismissal and dismissing clears a completion, so the row always has one
     * status; the table refuses both at once. Moving to the status the flow already has keeps its
     * original moment. Back to {@code IN_PROGRESS} clears both, which is how a dismissed onboarding is
     * reopened.
     */
    public void changeStatus(OnboardingStatus status, Instant now) {
        switch (status) {
            case IN_PROGRESS -> {
                this.completedAt = null;
                this.dismissedAt = null;
            }
            case COMPLETED -> {
                this.dismissedAt = null;
                if (this.completedAt == null) {
                    this.completedAt = now;
                }
            }
            case DISMISSED -> {
                this.completedAt = null;
                if (this.dismissedAt == null) {
                    this.dismissedAt = now;
                }
            }
        }
        this.updatedAt = now;
    }

    public OnboardingStatus status() {
        if (completedAt != null) {
            return OnboardingStatus.COMPLETED;
        }
        if (dismissedAt != null) {
            return OnboardingStatus.DISMISSED;
        }
        return OnboardingStatus.IN_PROGRESS;
    }

    @Override
    public UUID ownerId() {
        return userId;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public Instant getDashboardReviewedAt() {
        return dashboardReviewedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getDismissedAt() {
        return dismissedAt;
    }
}
