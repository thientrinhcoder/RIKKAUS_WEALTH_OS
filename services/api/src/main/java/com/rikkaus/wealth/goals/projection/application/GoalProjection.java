package com.rikkaus.wealth.goals.projection.application;

import com.rikkaus.wealth.shared.money.Currency;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * One goal's projection on one reference day: the five outputs the backbone names, the projected
 * amount at the target date split into its two parts, and every assumption behind them.
 *
 * <p>An output that cannot be derived is {@code null}, never zero, and the reason is listed in {@link
 * #unavailableReasons}. Zero is always a computed answer: a contribution of zero, a required
 * contribution of zero because the goal is already funded, a gap of zero because the projection lands
 * exactly on the target.
 *
 * @param remainingMonths contribution months left, from the month after the reference day through the
 *     target month; 0 when none remains
 * @param daysPastTargetDate how long ago the target date was, only when {@link Timeline#EXPIRED}
 * @param requiredMonthlyContribution the level monthly contribution that reaches the target, rounded up
 *     to a whole thousand đồng
 * @param contributionCoveragePercent the planned contribution as a percentage of the required one
 */
public record GoalProjection(
        UUID goalId,
        String goalName,
        Currency currency,
        LocalDate asOfDate,
        Timeline timeline,
        boolean targetReached,
        BigDecimal completionPercent,
        BigDecimal remainingAmount,
        int remainingMonths,
        Integer daysPastTargetDate,
        BigDecimal requiredMonthlyContribution,
        BigDecimal contributionCoveragePercent,
        GoalProjectionStatus status,
        Breakdown projectedAtTargetDate,
        Calculation calculation,
        List<UnavailableReason> unavailableReasons,
        List<Assumption> assumptions,
        Method method,
        String disclaimer) {

    public GoalProjection {
        unavailableReasons = List.copyOf(unavailableReasons);
        assumptions = List.copyOf(assumptions);
    }

    /** Where the goal stands in time relative to the reference day. */
    public enum Timeline {
        /** At least one contribution month remains before the target date. */
        ACTIVE,
        /** The target date is today or later this month, so no contribution month remains. */
        DUE_THIS_MONTH,
        /** The target date is before the reference day. */
        EXPIRED
    }

    /** How the planned contribution compares with the required one, against the reference thresholds. */
    public enum GoalProjectionStatus {
        ON_TRACK,
        AT_RISK,
        BEHIND
    }

    /** Why some outputs are {@code null}. */
    public enum UnavailableReason {
        /** The target date has passed; there is nothing left to project. */
        TARGET_DATE_PASSED,
        /** The target date falls in the current month; no contribution month remains. */
        NO_CONTRIBUTION_MONTH,
        /** No planned monthly contribution was entered. */
        PLANNED_MONTHLY_CONTRIBUTION_MISSING,
        /** No expected annual return was entered; it is never assumed to be zero. */
        EXPECTED_ANNUAL_RETURN_MISSING
    }

    /** Who supplied an assumption. */
    public enum AssumptionSource {
        /** Entered by the user on the goal. */
        USER_ENTERED,
        /** A convention the product chose. */
        SYSTEM,
        /** A factor the projection deliberately does not model. */
        NOT_MODELLED
    }

    /** Every assumption the ledger lists, in the order it lists them. */
    public enum AssumptionKey {
        TARGET_AMOUNT(AssumptionSource.USER_ENTERED),
        CURRENT_ALLOCATED_AMOUNT(AssumptionSource.USER_ENTERED),
        TARGET_DATE(AssumptionSource.USER_ENTERED),
        PLANNED_MONTHLY_CONTRIBUTION(AssumptionSource.USER_ENTERED),
        EXPECTED_ANNUAL_RETURN_PERCENT(AssumptionSource.USER_ENTERED),
        AS_OF_DATE(AssumptionSource.SYSTEM),
        TIME_ZONE(AssumptionSource.SYSTEM),
        FIRST_CONTRIBUTION_MONTH(AssumptionSource.SYSTEM),
        COMPOUNDING(AssumptionSource.SYSTEM),
        CONTRIBUTION_TIMING(AssumptionSource.SYSTEM),
        REQUIRED_CONTRIBUTION_ROUNDING(AssumptionSource.SYSTEM),
        ON_TRACK_MIN_COVERAGE_PERCENT(AssumptionSource.SYSTEM),
        AT_RISK_MIN_COVERAGE_PERCENT(AssumptionSource.SYSTEM),
        INFLATION(AssumptionSource.NOT_MODELLED),
        TAXES_AND_FEES(AssumptionSource.NOT_MODELLED),
        RETURN_VARIABILITY(AssumptionSource.NOT_MODELLED),
        CASH_FLOW_LINK(AssumptionSource.NOT_MODELLED);

        private final AssumptionSource source;

        AssumptionKey(AssumptionSource source) {
            this.source = source;
        }

        public AssumptionSource source() {
            return source;
        }
    }

    /**
     * One line of the assumption ledger.
     *
     * @param value the canonical text of the value: a plain decimal, an ISO date or month, an identifier;
     *     {@code null} when the user did not state it or the factor is not modelled
     */
    public record Assumption(AssumptionKey key, AssumptionSource source, String value) {}

    /**
     * The projected amount at the target date as its two parts.
     *
     * <p>Each part is rounded to two decimals before they are added, so the parts always add up to the
     * total exactly. {@code gapToTarget} is the total minus the target: positive is a surplus, negative a
     * shortfall.
     */
    public record Breakdown(
            BigDecimal fromAllocated, BigDecimal fromContributions, BigDecimal total, BigDecimal gapToTarget) {}

    /**
     * The two intermediate figures a reader needs to redo the arithmetic, rounded for display only.
     *
     * @param monthlyRatePercent the expected annual return divided by twelve, in percent, six decimals
     * @param growthFactor {@code (1 + r)^n}, ten decimals
     */
    public record Calculation(BigDecimal monthlyRatePercent, BigDecimal growthFactor) {}

    /** The method, in words, as the response states it. */
    public record Method(
            String name,
            String remainingMonths,
            String monthlyRate,
            String projectedAmount,
            String requiredMonthlyContribution,
            String status,
            String rounding) {}
}
