package com.rikkaus.wealth.goals.api;

import com.rikkaus.wealth.goals.domain.GoalAmountPrefill;
import com.rikkaus.wealth.goals.domain.GoalType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * One of the seven templates: its type and what the form should suggest for it.
 *
 * <p>Published so the client does not hard-code the accepted design's numbers. Every value is a
 * suggestion the user may change before saving, and the amount basis is named rather than computed,
 * because the figures it refers to belong to other slices the client already reads.
 */
@Schema(description = "A goal template: a type and the suggested defaults for a new goal of that type.")
public record GoalTemplateResponse(
        @Schema(description = "The type a goal created from this template carries.", example = "EMERGENCY_FUND")
                GoalType type,
        @Schema(description = "The suggested expected annual return, in percent. A suggestion, not a promise.", example = "3.00")
                BigDecimal defaultExpectedAnnualReturnPercent,
        @Schema(description = "How many months from today to suggest as the target date.", example = "18")
                int defaultHorizonMonths,
        @Schema(
                        description =
                                "Which amount the form should suggest, and from what figure the client already holds.",
                        example = "TARGET_FROM_SIX_MONTHS_OF_EXPENSES")
                GoalAmountPrefill amountPrefill) {

    static GoalTemplateResponse of(GoalType type) {
        return new GoalTemplateResponse(
                type, type.defaultExpectedAnnualReturnPercent(), type.defaultHorizonMonths(), type.amountPrefill());
    }
}
