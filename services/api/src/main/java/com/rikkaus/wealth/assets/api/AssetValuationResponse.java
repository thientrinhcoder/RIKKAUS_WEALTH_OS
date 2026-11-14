package com.rikkaus.wealth.assets.api;

import com.rikkaus.wealth.assets.application.ValuationEntry;
import com.rikkaus.wealth.assets.domain.AssetCurrency;
import com.rikkaus.wealth.assets.domain.AssetValuation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One valuation record, as the API describes it.
 *
 * <p>{@code current} is the mark the accepted design says the client must not infer from list position
 * — <em>Dấu "Hiện tại" do shared contract cung cấp</em>. It is here on every record so the history, a
 * single record and the record just created all say the same thing without the client comparing dates.
 *
 * <p>There is no converted VND amount. A USD record reports {@code 21250.00}, {@code "USD"} and the
 * rate the user applied; the client multiplies and shows the audit chain the design specifies —
 * original amount, rate, rate date, reported value. Converting here would mean publishing a figure
 * computed from a rate the client cannot see was used, and the slice rule forbids this package from
 * reading the settings rate at all.
 *
 * <p>{@code effectiveDate} is the user's statement of when the value was true; {@code recordedAt} is
 * the server's record of when it was stored. Both are present because the audit requirement is that
 * they are separately visible.
 */
@Schema(description = "One record in an asset's valuation history.")
public record AssetValuationResponse(
        @Schema(description = "The record's identifier.", example = "9c4e2b7a-1d35-4f68-a0c2-7e5b3d9f1a24")
                UUID id,
        @Schema(description = "The asset this record belongs to.", example = "3f1c9a52-8d4b-4f0e-9c21-5b7a8e6d4f10")
                UUID assetId,
        @Schema(description = "What the asset was worth, in its own currency.", example = "21250.00")
                BigDecimal value,
        @Schema(description = "The currency of the value.", example = "USD") AssetCurrency currency,
        @Schema(description = "The date the user states the value was true.", example = "2026-09-14")
                LocalDate effectiveDate,
        @Schema(
                        description =
                                "The USD/VND rate the user applied when recording this value. Null on a VND "
                                        + "record, and null when the user did not capture it.",
                        example = "25100.000000")
                BigDecimal usdToVndRate,
        @Schema(
                        description = "The date the user states that rate took effect. Null whenever usdToVndRate is.",
                        example = "2026-09-12")
                LocalDate rateEffectiveDate,
        @Schema(description = "The user's note, or the reason for a correction. Null when not given.", example = "Cập nhật số dư sau khi đối chiếu sao kê.")
                String note,
        @Schema(
                        description = "The record this one corrects, when it is a correction. Null otherwise.",
                        example = "6d2f8a41-3b9c-4e07-8f15-2a7c4e9b0d63")
                UUID correctsValuationId,
        @Schema(
                        description =
                                "True on exactly one record of an asset: the one its current value, currency "
                                        + "and valuation date are taken from. Latest effective date wins; "
                                        + "between two on the same date, the later recorded.",
                        example = "true")
                boolean current,
        @Schema(description = "When the server stored this record.", example = "2026-09-14T03:11:47.902Z")
                Instant recordedAt) {

    static AssetValuationResponse of(ValuationEntry entry) {
        AssetValuation valuation = entry.valuation();
        return new AssetValuationResponse(
                valuation.getId(),
                valuation.getAssetId(),
                valuation.getValue(),
                valuation.getCurrency(),
                valuation.getEffectiveDate(),
                valuation.getUsdToVndRate(),
                valuation.getRateEffectiveDate(),
                valuation.getNote(),
                valuation.getCorrectsValuationId(),
                entry.current(),
                valuation.getRecordedAt());
    }
}
