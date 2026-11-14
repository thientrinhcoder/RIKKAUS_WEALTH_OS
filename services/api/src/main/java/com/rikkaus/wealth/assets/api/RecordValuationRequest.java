package com.rikkaus.wealth.assets.api;

import com.rikkaus.wealth.assets.domain.AssetCurrency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A new valuation of an asset: what it is worth now, as of when, and at which rate.
 *
 * <p>There is no asset member and no owner member. The asset is in the path and the owner is whoever
 * the access token authenticates, so either would be a value the server had to refuse.
 *
 * <p>{@code Digits} mirrors the column precision rather than restating a business rule. {@code value}
 * is {@code NUMERIC(19, 2)} and {@code usd_to_vnd_rate} is {@code NUMERIC(19, 6)}, so without these a
 * value with too many digits would reach PostgreSQL and fail there — a 500 for what is a client input
 * error. Refusing it here makes it a {@code validation-failed} naming the field.
 *
 * <p>What the annotations cannot say lives in {@code AssetValuationService}: the currency must be the
 * asset's, the dates may not be in the future or precede the purchase, the two rate members come
 * together or not at all and only on a USD record, and a correction must name a record of this asset
 * and carry a note.
 */
@Schema(
        description =
                "Record what an asset is worth. Appended to the history; the asset's current value follows "
                        + "when this is the newest record.")
public record RecordValuationRequest(
        @Schema(
                        description =
                                "What the asset is worth, in the asset's own currency. The original amount, "
                                        + "never a converted one. Zero is allowed, negative is not.",
                        example = "21250.00",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull(message = "must not be null")
                @DecimalMin(value = "0", message = "must not be negative")
                @Digits(integer = 17, fraction = 2, message = "must have at most 17 integer digits and 2 decimals")
                BigDecimal value,
        @Schema(
                        description = "The currency of the value. Must be the asset's currency.",
                        example = "USD",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull(message = "must not be null")
                AssetCurrency currency,
        @Schema(
                        description =
                                "The date the user states this value was true. Must not be in the future and "
                                        + "not before the asset's purchase date, judged as a Vietnamese civil "
                                        + "date. Decides whether this record becomes the current one.",
                        example = "2026-09-14",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull(message = "must not be null")
                LocalDate effectiveDate,
        @Schema(
                        description =
                                "The USD/VND rate the user applied to this value: a snapshot kept with the "
                                        + "record, not a reference to the settings rate. Only on a USD "
                                        + "record, and only together with rateEffectiveDate.",
                        example = "25100.000000")
                @DecimalMin(value = "0", inclusive = false, message = "must be greater than zero")
                @Digits(integer = 13, fraction = 6, message = "must have at most 13 integer digits and 6 decimals")
                BigDecimal usdToVndRate,
        @Schema(
                        description =
                                "The date the user states that rate took effect. Must not be in the future. "
                                        + "Only together with usdToVndRate.",
                        example = "2026-09-12")
                LocalDate rateEffectiveDate,
        @Schema(
                        description =
                                "Why this record exists. Required when correcting an earlier record; otherwise "
                                        + "optional.",
                        example = "Cập nhật số dư sau khi đối chiếu sao kê.")
                @Size(max = 2000, message = "must be at most 2000 characters")
                String note,
        @Schema(
                        description =
                                "The earlier record of this asset that this one corrects. The original is kept "
                                        + "unchanged and stays readable; this record carries the reason in "
                                        + "note.",
                        example = "6d2f8a41-3b9c-4e07-8f15-2a7c4e9b0d63")
                UUID correctsValuationId) {}
