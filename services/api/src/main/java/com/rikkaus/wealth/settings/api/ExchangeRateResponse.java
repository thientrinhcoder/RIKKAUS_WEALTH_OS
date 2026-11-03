package com.rikkaus.wealth.settings.api;

import com.rikkaus.wealth.settings.domain.UserSettings;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * The exchange-rate settings, as the API describes them.
 *
 * <p>The three rate members are {@code null} together until the user has declared a rate. That is the
 * state the accepted design renders as <em>Tỷ giá cần được cập nhật</em>; answering 404 instead would
 * make a user who has never saved indistinguishable from one who does not exist.
 *
 * <p>{@code effectiveDate} and {@code updatedAt} are both present on purpose. The first is what the
 * user declared and the second is when the server stored it, and the audit requirement in issue #50 is
 * precisely that the two are separately visible.
 */
@Schema(description = "The reporting currency and the user-declared USD/VND exchange rate.")
public record ExchangeRateResponse(
        @Schema(description = "The reporting currency. Fixed to VND for MVP 1A.", example = "VND")
                String baseCurrency,
        @Schema(
                        description = "How many VND one USD buys, as declared by the user. Null until declared.",
                        example = "25100.000000")
                BigDecimal usdToVndRate,
        @Schema(
                        description =
                                "The date the user declares the rate took effect. Not the time it was stored.",
                        example = "2026-09-12")
                LocalDate effectiveDate,
        @Schema(
                        description = "When the server stored the declaration.",
                        example = "2026-09-12T02:42:00Z")
                Instant updatedAt) {

    static ExchangeRateResponse of(UserSettings settings) {
        return new ExchangeRateResponse(
                settings.getBaseCurrency(),
                settings.getUsdToVndRate(),
                settings.getRateEffectiveDate(),
                settings.getRateUpdatedAt());
    }
}
