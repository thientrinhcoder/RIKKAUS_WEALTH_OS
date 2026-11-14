package com.rikkaus.wealth.goals.api;

import com.rikkaus.wealth.goals.domain.Goal;
import com.rikkaus.wealth.goals.domain.GoalType;
import com.rikkaus.wealth.shared.money.Currency;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One goal, as the API describes it: what the user entered, and nothing derived from it.
 *
 * <p>There is no owner member, because every goal a caller can reach is their own. There is no
 * completion percentage, remaining amount, remaining time, required contribution or status: those are
 * issue #89's outputs, and publishing them here would fix a projection method in this contract before
 * that task has documented one. The client renders the inputs; the projection arrives with #89.
 *
 * <p>{@code currency} is always {@code VND}. It is stated rather than implied so a client never has to
 * assume it, and so a later second currency changes a value rather than adding a member.
 */
@Schema(description = "A financial goal owned by the signed-in user. Inputs only; projections are issue #89.")
public record GoalResponse(
        @Schema(description = "The goal's identifier.", example = "5c2e7d31-9a4f-4b8e-a1d6-3f7b2c9e8a40")
                UUID id,
        @Schema(description = "What the user calls this goal.", example = "Mua căn hộ") String name,
        @Schema(description = "Which of the seven templates this goal follows.", example = "HOME_PURCHASE")
                GoalType type,
        @Schema(description = "The currency every amount is in. Always VND.", example = "VND")
                Currency currency,
        @Schema(description = "The amount to reach.", example = "3500000000.00") BigDecimal targetAmount,
        @Schema(description = "What has been set aside so far. 0.00 when nothing yet.", example = "1200000000.00")
                BigDecimal currentAllocatedAmount,
        @Schema(description = "The date to reach the goal by.", example = "2030-12-31") LocalDate targetDate,
        @Schema(description = "Planned monthly contribution. Null when not stated.", example = "25000000.00")
                BigDecimal plannedMonthlyContribution,
        @Schema(
                        description = "The user's assumed annual return in percent. Null when no assumption.",
                        example = "7.00")
                BigDecimal expectedAnnualReturnPercent,
        @Schema(description = "When the server first stored this goal.", example = "2026-11-14T10:35:40.512Z")
                Instant createdAt,
        @Schema(description = "When the server last stored a change to it.", example = "2026-11-14T10:35:40.512Z")
                Instant updatedAt) {

    static GoalResponse of(Goal goal) {
        return new GoalResponse(
                goal.getId(),
                goal.getName(),
                goal.getType(),
                goal.getCurrency(),
                goal.getTargetAmount(),
                goal.getCurrentAllocatedAmount(),
                goal.getTargetDate(),
                goal.getPlannedMonthlyContribution(),
                goal.getExpectedAnnualReturnPercent(),
                goal.getCreatedAt(),
                goal.getUpdatedAt());
    }
}
