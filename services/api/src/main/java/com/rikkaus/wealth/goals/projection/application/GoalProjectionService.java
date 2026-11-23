package com.rikkaus.wealth.goals.projection.application;

import com.rikkaus.wealth.goals.application.GoalService;
import com.rikkaus.wealth.goals.domain.GoalFilter;
import com.rikkaus.wealth.goals.domain.GoalSort;
import com.rikkaus.wealth.shared.time.TimeConventions;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Projects the owner's goals on today's date.
 *
 * <p>Reads goals only through {@link GoalService}, so the ownership rule is the one the goals catalog
 * already enforces: another user's goal is {@code not-found}, indistinguishable from a missing one.
 *
 * <p>Computed on every read and never stored. A projection depends on the clock as much as on the goal,
 * so a stored one would be stale the next day; and an edit to the goal is visible in the very next read
 * because there is nothing to invalidate.
 */
@Service
public class GoalProjectionService {

    /** The goal catalog's default order: nearest target date first, then name. */
    private static final GoalFilter DEFAULT_ORDER = new GoalFilter(null, Set.of(), GoalSort.TARGET_DATE_ASC);

    private final GoalService goals;
    private final GoalProjectionCalculator calculator;
    private final Clock clock;

    GoalProjectionService(GoalService goals, GoalProjectionProperties thresholds, Clock clock) {
        this.goals = goals;
        this.calculator = new GoalProjectionCalculator(thresholds);
        this.clock = clock;
    }

    /** One of the owner's goals, projected; {@code not-found} if it is absent or somebody else's. */
    public GoalProjection projectionFor(UUID ownerId, UUID goalId) {
        return calculator.project(goals.read(ownerId, goalId), today());
    }

    /** Every goal of the owner, projected, in the goal list's default order. */
    public List<GoalProjection> projectionsFor(UUID ownerId) {
        LocalDate today = today();
        return goals.listFor(ownerId, DEFAULT_ORDER).stream()
                .map(goal -> calculator.project(goal, today))
                .toList();
    }

    private LocalDate today() {
        return TimeConventions.today(clock, GoalProjectionCalculator.REFERENCE_ZONE);
    }
}
