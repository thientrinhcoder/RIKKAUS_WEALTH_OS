package com.rikkaus.wealth.onboarding.application;

import com.rikkaus.wealth.onboarding.domain.OnboardingProgress;
import com.rikkaus.wealth.onboarding.domain.OnboardingProgressRepository;
import com.rikkaus.wealth.onboarding.domain.OnboardingStatus;
import com.rikkaus.wealth.shared.security.OwnershipGuard;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The onboarding slice's application service: read the checklist, record progress through it.
 *
 * <p>Every method takes the owner as its first argument and every caller gets it from {@code
 * CurrentUser}, never from the request, so the resource cannot be addressed as somebody else.
 *
 * <p>Completing the onboarding is not gated on every step being done. A user with no debt can never
 * add a liability and a user with nothing but savings may have no major asset, so refusing completion
 * until all seven are done would lock them in the flow. Which steps may be skipped is a product
 * decision the accepted design has not made yet; until it does, completion is the user's statement and
 * the checklist keeps reporting the records as they are.
 */
@Service
public class OnboardingService {

    private final OnboardingProgressRepository progress;
    private final Clock clock;

    OnboardingService(OnboardingProgressRepository progress, Clock clock) {
        this.progress = progress;
        this.clock = clock;
    }

    /** The viewer's checklist. Never writes: a user who has recorded nothing has no row. */
    @Transactional(readOnly = true)
    public OnboardingView checklistFor(UUID userId) {
        OnboardingProgress stored =
                progress.findById(userId)
                        .map(existing -> OwnershipGuard.requireOwned(existing, userId))
                        .orElse(null);
        return OnboardingView.of(stored, progress.recordedStepsOf(userId));
    }

    /**
     * Records whichever of the three progress facts the caller sent; a null argument leaves that fact
     * as it is. With nothing to record, it answers the checklist without creating a row.
     */
    @Transactional
    public OnboardingView recordProgress(
            UUID userId, Integer currentStep, Boolean dashboardReviewed, OnboardingStatus status) {
        if (currentStep == null && dashboardReviewed == null && status == null) {
            return checklistFor(userId);
        }

        Instant now = clock.instant();
        progress.insertIfAbsent(userId, now);
        OnboardingProgress current = OwnershipGuard.requireOwned(progress.findById(userId), userId);
        if (currentStep != null) {
            current.viewStep(currentStep, now);
        }
        if (dashboardReviewed != null) {
            current.recordDashboardReview(dashboardReviewed, now);
        }
        if (status != null) {
            current.changeStatus(status, now);
        }
        OnboardingProgress saved = progress.save(current);
        return OnboardingView.of(saved, progress.recordedStepsOf(userId));
    }
}
