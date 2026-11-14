package com.rikkaus.wealth.goals.api;

import com.rikkaus.wealth.goals.domain.GoalType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A goal as the user enters it: the same seven fields for every template, on create and on replace.
 *
 * <p>One record for both routes because the accepted design gives every template the same form and
 * makes every field editable. {@code PUT} is a replacement, not a patch: an optional field omitted
 * from the body is cleared.
 *
 * <p>There is no owner field and no currency field. The goal belongs to whoever the access token
 * authenticates, and every amount is in VND by the accepted design — a {@code currency} member sent
 * anyway is an unknown property Jackson drops. There is no computed field either; completion,
 * remaining amount and status are issue #89's outputs, never inputs.
 *
 * <p>{@code Digits} mirrors the column precision rather than restating a business rule: the three
 * money columns are {@code NUMERIC(19, 2)} and the return is {@code NUMERIC(5, 2)}, so a value with
 * too many digits is a {@code validation-failed} naming the field rather than a 500 from PostgreSQL.
 *
 * <p>An unrecognised {@code type} is a {@code malformed-request} rather than a {@code
 * validation-failed}, because Jackson cannot construct the record at all. The real client chooses from
 * the seven templates.
 */
@Schema(description = "Create or replace a goal. The same fields for every template.")
public record GoalRequest(
        @Schema(
                        description = "What the user calls this goal.",
                        example = "Mua căn hộ",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotBlank(message = "must not be blank")
                @Size(max = 120, message = "must be at most 120 characters")
                String name,
        @Schema(
                        description = "Which of the seven templates this goal follows. Editable.",
                        example = "HOME_PURCHASE",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull(message = "must not be null")
                GoalType type,
        @Schema(
                        description = "The amount to reach, in VND. Must be greater than zero.",
                        example = "3500000000.00",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull(message = "must not be null")
                @DecimalMin(value = "0", inclusive = false, message = "must be greater than zero")
                @Digits(integer = 17, fraction = 2, message = "must have at most 17 integer digits and 2 decimals")
                BigDecimal targetAmount,
        @Schema(
                        description =
                                "What has already been set aside for this goal, in VND. Zero or more, and never "
                                        + "above the target amount. Omit for nothing set aside yet, which is stored "
                                        + "as 0.00.",
                        example = "1200000000.00")
                @DecimalMin(value = "0", message = "must not be negative")
                @Digits(integer = 17, fraction = 2, message = "must have at most 17 integer digits and 2 decimals")
                BigDecimal currentAllocatedAmount,
        @Schema(
                        description =
                                "The date to reach the goal by. When created, or when changed, it must be after "
                                        + "today as a Vietnamese civil date; keeping a stored date is always allowed.",
                        example = "2030-12-31",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull(message = "must not be null")
                LocalDate targetDate,
        @Schema(
                        description =
                                "What the user plans to add each month, in VND. Zero or more. Omit when not "
                                        + "stated; that is kept distinct from zero.",
                        example = "25000000.00")
                @DecimalMin(value = "0", message = "must not be negative")
                @Digits(integer = 17, fraction = 2, message = "must have at most 17 integer digits and 2 decimals")
                BigDecimal plannedMonthlyContribution,
        @Schema(
                        description =
                                "The user's own assumption for the annual return, in percent, from 0 to 30. Omit "
                                        + "for no assumption. Informational only; never a promise.",
                        example = "7.00")
                @DecimalMin(value = "0", message = "must be between 0 and 30")
                @DecimalMax(value = "30", message = "must be between 0 and 30")
                @Digits(integer = 3, fraction = 2, message = "must have at most 3 integer digits and 2 decimals")
                BigDecimal expectedAnnualReturnPercent) {}
