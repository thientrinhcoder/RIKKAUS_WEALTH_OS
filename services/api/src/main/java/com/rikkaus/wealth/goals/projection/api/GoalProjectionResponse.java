package com.rikkaus.wealth.goals.projection.api;

import com.rikkaus.wealth.goals.projection.application.GoalProjection;
import com.rikkaus.wealth.goals.projection.application.GoalProjection.AssumptionKey;
import com.rikkaus.wealth.goals.projection.application.GoalProjection.AssumptionSource;
import com.rikkaus.wealth.goals.projection.application.GoalProjection.GoalProjectionStatus;
import com.rikkaus.wealth.goals.projection.application.GoalProjection.Timeline;
import com.rikkaus.wealth.goals.projection.application.GoalProjection.UnavailableReason;
import com.rikkaus.wealth.shared.money.Currency;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * One goal's projection as the API describes it.
 *
 * <p>Separate from {@code GoalResponse} on purpose: that resource is what the user entered, and this one
 * is what the server derives from it on a given day. An output that cannot be derived is {@code null} and
 * the reason is in {@code unavailableReasons}; zero is always a computed answer.
 */
@Schema(
        name = "GoalProjection",
        description =
                "A goal's projection on today's date in Asia/Ho_Chi_Minh. Computed on every read and never stored. "
                        + "The method is stated in `method` and in docs/api-contract-conventions.md.")
record GoalProjectionResponse(
        @Schema(description = "The projected goal's identifier.", example = "9d4b1f68-2c7e-4a35-b8f0-6e1a5d3c7b92")
                UUID goalId,
        @Schema(description = "The goal's name.", example = "Mua nhà cho con") String goalName,
        @Schema(description = "The currency every amount is in. Always VND.", example = "VND") Currency currency,
        @Schema(description = "The reference day: today in Asia/Ho_Chi_Minh.", example = "2026-11-22")
                LocalDate asOfDate,
        @Schema(
                        description =
                                "ACTIVE when at least one contribution month remains; DUE_THIS_MONTH when the target"
                                        + " date is today or later this month; EXPIRED when it is before asOfDate.",
                        example = "ACTIVE")
                Timeline timeline,
        @Schema(description = "Whether the allocated amount already meets the target amount.", example = "false")
                boolean targetReached,
        @Schema(
                        description =
                                "currentAllocatedAmount / targetAmount × 100, rounded down to 2 decimals, at most 100.",
                        example = "40.00")
                BigDecimal completionPercent,
        @Schema(description = "targetAmount − currentAllocatedAmount, never below 0.", example = "1800000000.00")
                BigDecimal remainingAmount,
        @Schema(
                        description =
                                "Contribution months from the month after asOfDate through the target month,"
                                        + " both included. 0 when DUE_THIS_MONTH or EXPIRED.",
                        example = "57")
                int remainingMonths,
        @Schema(
                        description = "Days since the target date. Null unless EXPIRED.",
                        example = "22")
                Integer daysPastTargetDate,
        @Schema(
                        description =
                                "The level monthly contribution that reaches the target, rounded up to a whole 1,000"
                                        + " VND; 0.00 when nothing more is needed. Null when not ACTIVE, or when the"
                                        + " expected return is missing and the goal is not yet funded.",
                        example = "21371000.00")
                BigDecimal requiredMonthlyContribution,
        @Schema(
                        description =
                                "plannedMonthlyContribution / requiredMonthlyContribution × 100, rounded down to 2"
                                        + " decimals; 100.00 when nothing is required. Null when either is unavailable.",
                        example = "116.98")
                BigDecimal contributionCoveragePercent,
        @Schema(
                        description =
                                "Coverage against the system reference thresholds in the assumptions. Null whenever"
                                        + " the coverage is unavailable, including every DUE_THIS_MONTH and EXPIRED goal.",
                        example = "ON_TRACK")
                GoalProjectionStatus status,
        @Schema(
                        description =
                                "The projected amount at the target date as its two parts. Null unless ACTIVE with both"
                                        + " a planned contribution and an expected return.")
                BreakdownResponse projectedAtTargetDate,
        @Schema(
                        description = "Intermediate figures for redoing the arithmetic. Null unless ACTIVE with an expected return.")
                CalculationResponse calculation,
        @Schema(description = "Why each null output is null. Empty when every output is available.")
                List<UnavailableReason> unavailableReasons,
        @Schema(description = "Every assumption behind the projection, in a fixed order, with who supplied it.")
                List<AssumptionResponse> assumptions,
        @Schema(description = "The projection method in words.") MethodResponse method,
        @Schema(
                        description = "States that the expected return is the user's assumption and is not guaranteed.",
                        example =
                                "The expected annual return is the user's own assumption, not a guaranteed, promised or"
                                        + " predicted return. Projected amounts illustrate what the entered figures"
                                        + " would produce if that return were earned every month; actual results will"
                                        + " differ.")
                String disclaimer) {

    static GoalProjectionResponse of(GoalProjection projection) {
        GoalProjection.Breakdown breakdown = projection.projectedAtTargetDate();
        GoalProjection.Calculation calculation = projection.calculation();
        GoalProjection.Method method = projection.method();
        return new GoalProjectionResponse(
                projection.goalId(),
                projection.goalName(),
                projection.currency(),
                projection.asOfDate(),
                projection.timeline(),
                projection.targetReached(),
                projection.completionPercent(),
                projection.remainingAmount(),
                projection.remainingMonths(),
                projection.daysPastTargetDate(),
                projection.requiredMonthlyContribution(),
                projection.contributionCoveragePercent(),
                projection.status(),
                breakdown == null
                        ? null
                        : new BreakdownResponse(
                                breakdown.fromAllocated(),
                                breakdown.fromContributions(),
                                breakdown.total(),
                                breakdown.gapToTarget()),
                calculation == null
                        ? null
                        : new CalculationResponse(calculation.monthlyRatePercent(), calculation.growthFactor()),
                projection.unavailableReasons(),
                projection.assumptions().stream()
                        .map(a -> new AssumptionResponse(a.key(), a.source(), a.value()))
                        .toList(),
                new MethodResponse(
                        method.name(),
                        method.remainingMonths(),
                        method.monthlyRate(),
                        method.projectedAmount(),
                        method.requiredMonthlyContribution(),
                        method.status(),
                        method.rounding()),
                projection.disclaimer());
    }

    @Schema(name = "GoalProjectionBreakdown", description = "The projected amount at the target date, in two parts.")
    record BreakdownResponse(
            @Schema(description = "currentAllocatedAmount × g, rounded half up to 2 decimals.", example = "1594581665.13")
                    BigDecimal fromAllocated,
            @Schema(
                            description =
                                    "plannedMonthlyContribution × (g − 1) / r, or × n when r = 0, rounded half up to 2"
                                            + " decimals.",
                            example = "1644090271.39")
                    BigDecimal fromContributions,
            @Schema(description = "fromAllocated + fromContributions, exactly.", example = "3238671936.52")
                    BigDecimal total,
            @Schema(
                            description = "total − targetAmount. Positive is a surplus, negative a shortfall.",
                            example = "238671936.52")
                    BigDecimal gapToTarget) {}

    @Schema(name = "GoalProjectionCalculation", description = "Intermediate figures, rounded for display only.")
    record CalculationResponse(
            @Schema(
                            description = "expectedAnnualReturnPercent / 12, in percent, rounded half up to 6 decimals.",
                            example = "0.500000")
                    BigDecimal monthlyRatePercent,
            @Schema(description = "g = (1 + r)^n, rounded half up to 10 decimals.", example = "1.3288180543")
                    BigDecimal growthFactor) {}

    @Schema(name = "GoalProjectionAssumption", description = "One line of the assumption ledger.")
    record AssumptionResponse(
            @Schema(description = "Which assumption.", example = "EXPECTED_ANNUAL_RETURN_PERCENT") AssumptionKey key,
            @Schema(
                            description =
                                    "USER_ENTERED on the goal, a SYSTEM convention, or NOT_MODELLED by the projection.",
                            example = "USER_ENTERED")
                    AssumptionSource source,
            @Schema(
                            description =
                                    "The value as canonical text: a plain decimal, an ISO date or month, or an"
                                            + " identifier. Null when the user did not state it or it is NOT_MODELLED.",
                            example = "6.00")
                    String value) {}

    @Schema(name = "GoalProjectionMethod", description = "The projection method in words, as implemented.")
    record MethodResponse(
            @Schema(example = "FUTURE_VALUE_MONTHLY_COMPOUNDING") String name,
            String remainingMonths,
            String monthlyRate,
            String projectedAmount,
            String requiredMonthlyContribution,
            String status,
            String rounding) {}
}
