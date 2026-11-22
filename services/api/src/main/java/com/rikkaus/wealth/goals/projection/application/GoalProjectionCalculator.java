package com.rikkaus.wealth.goals.projection.application;

import com.rikkaus.wealth.goals.domain.Goal;
import com.rikkaus.wealth.goals.projection.application.GoalProjection.Assumption;
import com.rikkaus.wealth.goals.projection.application.GoalProjection.AssumptionKey;
import com.rikkaus.wealth.goals.projection.application.GoalProjection.Breakdown;
import com.rikkaus.wealth.goals.projection.application.GoalProjection.Calculation;
import com.rikkaus.wealth.goals.projection.application.GoalProjection.GoalProjectionStatus;
import com.rikkaus.wealth.goals.projection.application.GoalProjection.Method;
import com.rikkaus.wealth.goals.projection.application.GoalProjection.Timeline;
import com.rikkaus.wealth.goals.projection.application.GoalProjection.UnavailableReason;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The projection method, as one pure function of a goal, a reference day and the thresholds.
 *
 * <p>The method is the one the accepted design for issue #87 froze and {@code
 * docs/api-contract-conventions.md} documents under "Goal projection":
 *
 * <ul>
 *   <li>{@code n} is the number of calendar months from the month after the reference day through the
 *       target month, both included. Contributions arrive at the end of each of those months.
 *   <li>{@code r} is the user's expected annual return divided by 100 and by 12, compounded monthly, and
 *       {@code g = (1 + r)^n}.
 *   <li>The projected amount is {@code allocated × g} plus {@code planned × (g − 1) / r}; with {@code r =
 *       0} the second part is {@code planned × n}.
 *   <li>The required monthly contribution solves the same equation for the contribution that lands
 *       exactly on the target, is zero when the allocated amount already grows to the target on its
 *       own, and is otherwise rounded up to a whole thousand đồng.
 *   <li>The status compares the planned contribution with that rounded required contribution, exactly,
 *       against the configured thresholds.
 * </ul>
 *
 * <p>Every intermediate value is a {@link BigDecimal} at {@link MathContext#DECIMAL128}, 34 significant
 * digits, so the result is the same on every machine and every run; nothing passes through {@code
 * double}. Money is rounded once, at the end, half up to two decimals. Percentages are rounded
 * <em>down</em> to two decimals, so a coverage of 99.996% reads 99.99% beside its {@code AT_RISK} status
 * rather than 100.00%, and a goal is never shown as further along than it is.
 *
 * <p>No input is ever assumed. A missing contribution or return makes the outputs that depend on it
 * {@code null} and names the reason, as the design requires: "Không khả dụng", never zero.
 */
public final class GoalProjectionCalculator {

    /** The zone "today" is judged in, as every other slice judges a user-entered date. */
    public static final ZoneId REFERENCE_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    static final String DISCLAIMER =
            "The expected annual return is the user's own assumption, not a guaranteed, promised or predicted "
                    + "return. Projected amounts illustrate what the entered figures would produce if that return "
                    + "were earned every month; actual results will differ.";

    private static final MathContext MC = MathContext.DECIMAL128;
    private static final int MONEY_SCALE = 2;
    private static final int PERCENT_SCALE = 2;
    private static final int RATE_PERCENT_SCALE = 6;
    private static final int FACTOR_SCALE = 10;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);
    private static final BigDecimal FULL_COVERAGE = new BigDecimal("100.00");
    private static final BigDecimal ZERO_MONEY = BigDecimal.ZERO.setScale(MONEY_SCALE);
    private static final int ROUNDING_STEP_DIGITS = 3;

    private final GoalProjectionProperties thresholds;
    private final Method method;

    public GoalProjectionCalculator(GoalProjectionProperties thresholds) {
        this.thresholds = thresholds;
        this.method = method(thresholds);
    }

    /** Projects {@code goal} as it stands on {@code asOfDate}. */
    public GoalProjection project(Goal goal, LocalDate asOfDate) {
        BigDecimal target = goal.getTargetAmount();
        BigDecimal allocated = goal.getCurrentAllocatedAmount();
        BigDecimal planned = goal.getPlannedMonthlyContribution();
        BigDecimal annualPercent = goal.getExpectedAnnualReturnPercent();
        LocalDate targetDate = goal.getTargetDate();

        BigDecimal remaining = target.subtract(allocated).max(BigDecimal.ZERO).setScale(MONEY_SCALE);
        BigDecimal completion =
                allocated.multiply(HUNDRED).divide(target, PERCENT_SCALE, RoundingMode.DOWN).min(FULL_COVERAGE);
        boolean reached = allocated.compareTo(target) >= 0;

        Outputs out = new Outputs();
        if (targetDate.isBefore(asOfDate)) {
            out.timeline = Timeline.EXPIRED;
            out.daysPastTargetDate = Math.toIntExact(ChronoUnit.DAYS.between(targetDate, asOfDate));
            out.reasons.add(UnavailableReason.TARGET_DATE_PASSED);
        } else {
            out.months =
                    Math.toIntExact(
                            ChronoUnit.MONTHS.between(YearMonth.from(asOfDate), YearMonth.from(targetDate)));
            if (out.months == 0) {
                out.timeline = Timeline.DUE_THIS_MONTH;
                out.reasons.add(UnavailableReason.NO_CONTRIBUTION_MONTH);
            } else {
                out.timeline = Timeline.ACTIVE;
                projectActive(out, target, allocated, remaining, planned, annualPercent);
            }
        }

        return new GoalProjection(
                goal.getId(),
                goal.getName(),
                goal.getCurrency(),
                asOfDate,
                out.timeline,
                reached,
                completion,
                remaining,
                out.months,
                out.daysPastTargetDate,
                out.required,
                out.coverage,
                out.status,
                out.breakdown,
                out.calculation,
                out.reasons,
                assumptions(goal, asOfDate, out.timeline),
                method,
                DISCLAIMER);
    }

    private void projectActive(
            Outputs out,
            BigDecimal target,
            BigDecimal allocated,
            BigDecimal remaining,
            BigDecimal planned,
            BigDecimal annualPercent) {
        if (planned == null) {
            out.reasons.add(UnavailableReason.PLANNED_MONTHLY_CONTRIBUTION_MISSING);
        }
        if (annualPercent == null) {
            out.reasons.add(UnavailableReason.EXPECTED_ANNUAL_RETURN_MISSING);
        }

        BigDecimal months = BigDecimal.valueOf(out.months);
        BigDecimal rate = null;
        BigDecimal growth = null;
        if (annualPercent != null) {
            rate = annualPercent.divide(HUNDRED, MC).divide(MONTHS_PER_YEAR, MC);
            growth = BigDecimal.ONE.add(rate, MC).pow(out.months, MC);
            out.calculation =
                    new Calculation(
                            annualPercent.divide(MONTHS_PER_YEAR, RATE_PERCENT_SCALE, RoundingMode.HALF_UP),
                            growth.setScale(FACTOR_SCALE, RoundingMode.HALF_UP));
        }

        if (remaining.signum() == 0) {
            // Nothing left to save, whatever the return: the allocated amount already meets the target.
            out.required = ZERO_MONEY;
        } else if (rate != null) {
            out.required = required(target, allocated, remaining, rate, growth, months);
        }

        if (planned != null && rate != null) {
            BigDecimal fromAllocated = money(allocated.multiply(growth, MC));
            BigDecimal fromContributions =
                    rate.signum() == 0
                            ? money(planned.multiply(months, MC))
                            : money(planned.multiply(growth.subtract(BigDecimal.ONE, MC), MC).divide(rate, MC));
            BigDecimal total = fromAllocated.add(fromContributions);
            out.breakdown = new Breakdown(fromAllocated, fromContributions, total, total.subtract(target));
        }

        if (planned != null && out.required != null) {
            out.coverage =
                    out.required.signum() == 0
                            ? FULL_COVERAGE
                            : planned.multiply(HUNDRED).divide(out.required, PERCENT_SCALE, RoundingMode.DOWN);
            out.status = status(planned, out.required);
        }
    }

    /**
     * The level monthly contribution that lands on the target, rounded up to a whole thousand đồng.
     *
     * <p>Rounded up rather than to the nearest, so that contributing the published figure is always
     * enough; zero when the allocated amount grows past the target with no contribution at all.
     */
    private static BigDecimal required(
            BigDecimal target,
            BigDecimal allocated,
            BigDecimal remaining,
            BigDecimal rate,
            BigDecimal growth,
            BigDecimal months) {
        BigDecimal exact =
                rate.signum() == 0
                        ? remaining.divide(months, MC)
                        : target.subtract(allocated.multiply(growth, MC), MC)
                                .multiply(rate, MC)
                                .divide(growth.subtract(BigDecimal.ONE, MC), MC);
        if (exact.signum() <= 0) {
            return ZERO_MONEY;
        }
        return exact.movePointLeft(ROUNDING_STEP_DIGITS)
                .setScale(0, RoundingMode.CEILING)
                .movePointRight(ROUNDING_STEP_DIGITS)
                .setScale(MONEY_SCALE);
    }

    /** Compared exactly, without the rounding the published coverage percentage carries. */
    private GoalProjectionStatus status(BigDecimal planned, BigDecimal required) {
        BigDecimal plannedTimesHundred = planned.multiply(HUNDRED);
        if (plannedTimesHundred.compareTo(thresholds.onTrackMinCoveragePercent().multiply(required)) >= 0) {
            return GoalProjectionStatus.ON_TRACK;
        }
        if (plannedTimesHundred.compareTo(thresholds.atRiskMinCoveragePercent().multiply(required)) >= 0) {
            return GoalProjectionStatus.AT_RISK;
        }
        return GoalProjectionStatus.BEHIND;
    }

    private List<Assumption> assumptions(Goal goal, LocalDate asOfDate, Timeline timeline) {
        Map<AssumptionKey, String> values = new EnumMap<>(AssumptionKey.class);
        values.put(AssumptionKey.TARGET_AMOUNT, plain(goal.getTargetAmount()));
        values.put(AssumptionKey.CURRENT_ALLOCATED_AMOUNT, plain(goal.getCurrentAllocatedAmount()));
        values.put(AssumptionKey.TARGET_DATE, goal.getTargetDate().toString());
        values.put(AssumptionKey.PLANNED_MONTHLY_CONTRIBUTION, plain(goal.getPlannedMonthlyContribution()));
        values.put(AssumptionKey.EXPECTED_ANNUAL_RETURN_PERCENT, plain(goal.getExpectedAnnualReturnPercent()));
        values.put(AssumptionKey.AS_OF_DATE, asOfDate.toString());
        values.put(AssumptionKey.TIME_ZONE, REFERENCE_ZONE.getId());
        values.put(
                AssumptionKey.FIRST_CONTRIBUTION_MONTH,
                timeline == Timeline.ACTIVE ? YearMonth.from(asOfDate).plusMonths(1).toString() : null);
        values.put(AssumptionKey.COMPOUNDING, "MONTHLY");
        values.put(AssumptionKey.CONTRIBUTION_TIMING, "END_OF_MONTH");
        values.put(AssumptionKey.REQUIRED_CONTRIBUTION_ROUNDING, "UP_TO_1000_VND");
        values.put(AssumptionKey.ON_TRACK_MIN_COVERAGE_PERCENT, plain(thresholds.onTrackMinCoveragePercent()));
        values.put(AssumptionKey.AT_RISK_MIN_COVERAGE_PERCENT, plain(thresholds.atRiskMinCoveragePercent()));

        List<Assumption> ledger = new ArrayList<>();
        for (AssumptionKey key : AssumptionKey.values()) {
            ledger.add(new Assumption(key, key.source(), values.get(key)));
        }
        return ledger;
    }

    private static Method method(GoalProjectionProperties thresholds) {
        String onTrack = plain(thresholds.onTrackMinCoveragePercent());
        String atRisk = plain(thresholds.atRiskMinCoveragePercent());
        return new Method(
                "FUTURE_VALUE_MONTHLY_COMPOUNDING",
                "n = calendar months from the month after asOfDate through the month of targetDate, both included;"
                        + " contributions arrive at the end of each month. A target date before asOfDate is EXPIRED;"
                        + " a target date later this month leaves n = 0 and is DUE_THIS_MONTH.",
                "r = expectedAnnualReturnPercent / 100 / 12, compounded monthly; g = (1 + r)^n.",
                "fromAllocated = currentAllocatedAmount × g; fromContributions = plannedMonthlyContribution ×"
                        + " (g − 1) / r, or plannedMonthlyContribution × n when r = 0; total = fromAllocated +"
                        + " fromContributions; gapToTarget = total − targetAmount.",
                "(targetAmount − currentAllocatedAmount × g) × r / (g − 1), or (targetAmount −"
                        + " currentAllocatedAmount) / n when r = 0; 0 when the goal is already funded or the"
                        + " allocated amount alone reaches the target; otherwise rounded up to the next 1,000 VND.",
                "contributionCoveragePercent = plannedMonthlyContribution / requiredMonthlyContribution × 100"
                        + " (100 when nothing is required). ON_TRACK at or above "
                        + onTrack
                        + ", AT_RISK at or above "
                        + atRisk
                        + ", BEHIND below; these are system reference thresholds, compared before the percentage"
                        + " is rounded.",
                "Intermediate values carry 34 significant digits. Each part of the projected amount is rounded half"
                        + " up to 2 decimals before they are added, so the parts add up to the total. Percentages"
                        + " are rounded down to 2 decimals.");
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static String plain(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    /** The outputs that depend on the timeline, filled in as far as the inputs allow. */
    private static final class Outputs {
        private Timeline timeline;
        private int months;
        private Integer daysPastTargetDate;
        private BigDecimal required;
        private BigDecimal coverage;
        private GoalProjectionStatus status;
        private Breakdown breakdown;
        private Calculation calculation;
        private final List<UnavailableReason> reasons = new ArrayList<>();
    }
}
