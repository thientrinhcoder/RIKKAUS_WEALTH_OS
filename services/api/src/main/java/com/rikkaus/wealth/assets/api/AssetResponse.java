package com.rikkaus.wealth.assets.api;

import com.rikkaus.wealth.assets.domain.Asset;
import com.rikkaus.wealth.assets.domain.AssetCategory;
import com.rikkaus.wealth.assets.domain.AssetCurrency;
import com.rikkaus.wealth.assets.domain.AssetLiquidity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One asset, as the API describes it.
 *
 * <p>There is no owner member. Every asset a caller can reach is their own, so publishing the owner
 * would add an identifier to the wire that tells a client nothing it does not already know.
 *
 * <p>There is no converted VND amount and no exchange rate. A USD asset reports {@code 20000.00} and
 * {@code "USD"}; the client multiplies by the rate it already read from {@code
 * GET /api/v1/settings/exchange-rate} and shows the audit trail the approved design specifies —
 * original amount, rate, rate date, reported value. Converting here would mean storing or recomputing
 * a figure whose rate nobody could see, and the slice rule forbids this package from reading the rate
 * at all.
 *
 * <p>{@code createdAt} and {@code updatedAt} are the server's own timestamps and are deliberately
 * distinct from {@code valuationDate}, which is the date the <em>user</em> states the value was
 * accurate. The catalog's stale-valuation warning is driven by the user's date, not by the row's.
 */
@Schema(description = "An asset in the signed-in user's catalog.")
public record AssetResponse(
        @Schema(description = "The asset's identifier.", example = "3f1c9a52-8d4b-4f0e-9c21-5b7a8e6d4f10")
                UUID id,
        @Schema(description = "What the user calls this asset.", example = "Tài khoản USD") String name,
        @Schema(description = "One of the eleven asset categories.", example = "BANK_ACCOUNT")
                AssetCategory category,
        @Schema(
                        description =
                                "What the asset is worth, in its own currency: the value of the current record "
                                        + "in its valuation history.",
                        example = "20000.00")
                BigDecimal currentValue,
        @Schema(description = "The currency the values are expressed in.", example = "USD")
                AssetCurrency currency,
        @Schema(
                        description =
                                "The date the user states the value was accurate: the effective date of the "
                                        + "current valuation record.",
                        example = "2026-09-12")
                LocalDate valuationDate,
        @Schema(description = "How readily the user judges it can be turned into cash.", example = "HIGH")
                AssetLiquidity liquidity,
        @Schema(description = "Purchase value or cost basis. Null when not recorded.", example = "19500.00")
                BigDecimal purchaseValue,
        @Schema(description = "When the asset was acquired. Null when not recorded.", example = "2024-03-18")
                LocalDate purchaseDate,
        @Schema(description = "Units held. Null when not recorded.", example = "20000.000000")
                BigDecimal quantity,
        @Schema(description = "Where it is held or stored. Null when not recorded.", example = "Ngân hàng An Tín")
                String institution,
        @Schema(description = "The user's own note. Null when not recorded.", example = "Tiền gửi dự phòng bằng USD.")
                String notes,
        @Schema(description = "When the server first stored this asset.", example = "2026-09-12T02:42:11.503Z")
                Instant createdAt,
        @Schema(description = "When the server last stored a change to it.", example = "2026-09-12T02:42:11.503Z")
                Instant updatedAt) {

    static AssetResponse of(Asset asset) {
        return new AssetResponse(
                asset.getId(),
                asset.getName(),
                asset.getCategory(),
                asset.getCurrentValue(),
                asset.getCurrency(),
                asset.getValuationDate(),
                asset.getLiquidity(),
                asset.getPurchaseValue(),
                asset.getPurchaseDate(),
                asset.getQuantity(),
                asset.getInstitution(),
                asset.getNotes(),
                asset.getCreatedAt(),
                asset.getUpdatedAt());
    }
}
