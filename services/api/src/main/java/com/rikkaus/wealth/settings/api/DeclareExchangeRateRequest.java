package com.rikkaus.wealth.settings.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A declaration of the USD/VND rate and the date it took effect.
 *
 * <p>{@code Digits} is not decoration. The column is {@code NUMERIC(19, 6)}, so a value with more
 * integer digits or a finer scale would reach PostgreSQL and fail there — a 500 for what is a client
 * input error. Refusing it here makes it a {@code validation-failed} with the field named.
 *
 * <p>There is no {@code baseCurrency} member. VND is fixed for MVP 1A and accepting the field would
 * imply a choice that does not exist; the database check would refuse any other value anyway.
 */
@Schema(description = "Declare the USD/VND exchange rate and the date it took effect.")
public record DeclareExchangeRateRequest(
        @Schema(
                        description = "How many VND one USD buys. Must be greater than zero.",
                        example = "25100.000000",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull
                @DecimalMin(value = "0", inclusive = false)
                @Digits(integer = 13, fraction = 6)
                BigDecimal usdToVndRate,
        @Schema(
                        description =
                                "The date you confirm this rate took effect. Must not be in the future.",
                        example = "2026-09-12",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull
                LocalDate effectiveDate) {}
