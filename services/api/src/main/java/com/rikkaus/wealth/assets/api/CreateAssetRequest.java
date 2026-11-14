package com.rikkaus.wealth.assets.api;

import com.rikkaus.wealth.assets.domain.AssetCategory;
import com.rikkaus.wealth.assets.domain.AssetCurrency;
import com.rikkaus.wealth.assets.domain.AssetLiquidity;
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
 * A new asset: the six common required fields and the five optional ones.
 *
 * <p>There is no owner field. The asset belongs to whoever the access token authenticates, so an
 * owner member would be a value the server had to refuse — and an ArchUnit rule already fails the
 * compilation gate on a controller that binds one from the request.
 *
 * <p>The four instrument fields are optional on the categories that accept them and refused on the
 * categories the accepted design never shows them on, with {@code validation-failed} naming the
 * field. Which category accepts what is recorded on {@code AssetCategory} and enforced a second time
 * by a check constraint in {@code V5__asset_instrument_details.sql}. Sending none of the four is the
 * ordinary case and is valid for all eleven categories.
 *
 * <p>{@code Digits} mirrors the column precision rather than restating a business rule.
 * {@code current_value} is {@code NUMERIC(19, 2)} and {@code quantity} is {@code NUMERIC(19, 6)}, so
 * without these a value with too many digits would reach PostgreSQL and fail there — a 500 for what is
 * a client input error. Refusing it here makes it a {@code validation-failed} naming the field.
 *
 * <p>An unrecognised {@code category}, {@code currency} or {@code liquidity} is a
 * {@code malformed-request} rather than a {@code validation-failed}, because Jackson cannot construct
 * the record at all and the failure happens before bean validation runs. The real client chooses from
 * a picker, so the case arises only from a hand-written request.
 */
@Schema(description = "Create an asset with the fields every category shares.")
public record CreateAssetRequest(
        @Schema(
                        description = "What the user calls this asset.",
                        example = "Tài khoản USD",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotBlank(message = "must not be blank")
                @Size(max = 120, message = "must be at most 120 characters")
                String name,
        @Schema(
                        description = "One of the eleven asset categories.",
                        example = "BANK_ACCOUNT",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull(message = "must not be null")
                AssetCategory category,
        @Schema(
                        description =
                                "What the asset is worth, in this asset's currency. Entered by the user; there "
                                        + "is no price feed. Zero is allowed, negative is not.",
                        example = "20000.00",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull(message = "must not be null")
                @DecimalMin(value = "0", message = "must not be negative")
                @Digits(integer = 17, fraction = 2, message = "must have at most 17 integer digits and 2 decimals")
                BigDecimal currentValue,
        @Schema(
                        description =
                                "The currency the values are expressed in. The reporting currency is separately "
                                        + "fixed to VND and is read from the settings resource.",
                        example = "USD",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull(message = "must not be null")
                AssetCurrency currency,
        @Schema(
                        description =
                                "The date the user states this value was accurate. Must not be in the future, "
                                        + "judged as a Vietnamese civil date.",
                        example = "2026-09-12",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull(message = "must not be null")
                LocalDate valuationDate,
        @Schema(
                        description = "How readily the user judges this asset can be turned into cash.",
                        example = "HIGH",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull(message = "must not be null")
                AssetLiquidity liquidity,
        @Schema(description = "Purchase value or cost basis, in the same currency.", example = "19500.00")
                @DecimalMin(value = "0", message = "must not be negative")
                @Digits(integer = 17, fraction = 2, message = "must have at most 17 integer digits and 2 decimals")
                BigDecimal purchaseValue,
        @Schema(
                        description =
                                "When the asset was acquired. Must not be in the future and must not be later "
                                        + "than the valuation date.",
                        example = "2024-03-18")
                LocalDate purchaseDate,
        @Schema(description = "Units held, where that is meaningful. Must be greater than zero.", example = "20000.000000")
                @DecimalMin(value = "0", inclusive = false, message = "must be greater than zero")
                @Digits(integer = 13, fraction = 6, message = "must have at most 13 integer digits and 6 decimals")
                BigDecimal quantity,
        @Schema(description = "Where the asset is held or stored.", example = "Ngân hàng An Tín")
                @Size(max = 160, message = "must be at most 160 characters")
                String institution,
        @Schema(description = "Anything the user wants to remember about it.", example = "Tiền gửi dự phòng bằng USD.")
                @Size(max = 2000, message = "must be at most 2000 characters")
                String notes,
        @Schema(
                        description =
                                "Ticker, ISIN or fund code, as the user types it. Nothing resolves it against a "
                                        + "market. Accepted for LISTED_SECURITIES, BOND, INVESTMENT_FUND and "
                                        + "OTHER_ASSET; any other category refuses it.",
                        example = "VCB")
                @Size(max = 32, message = "must be at most 32 characters")
                String symbol,
        @Schema(
                        description =
                                "What one unit cost, in this asset's own currency. Entered by the user and never "
                                        + "multiplied by the quantity. Accepted for the same four categories as "
                                        + "the symbol.",
                        example = "21800.000000")
                @DecimalMin(value = "0", message = "must not be negative")
                @Digits(integer = 13, fraction = 6, message = "must have at most 13 integer digits and 6 decimals")
                BigDecimal costPerUnit,
        @Schema(
                        description =
                                "What one unit is worth now, in this asset's own currency. Entered by the user; "
                                        + "currentValue is a separate field and is never derived from this one. "
                                        + "Accepted for the same four categories as the symbol.",
                        example = "24150.000000")
                @DecimalMin(value = "0", message = "must not be negative")
                @Digits(integer = 13, fraction = 6, message = "must have at most 13 integer digits and 6 decimals")
                BigDecimal currentPricePerUnit,
        @Schema(
                        description =
                                "Interest rate or expected yield as a percent per year, so 6.2500 is 6.25%. No "
                                        + "interest is accrued and no yield projected from it. Accepted for BOND, "
                                        + "TERM_DEPOSIT and OTHER_ASSET only: a listed security has no rate, and a "
                                        + "fund's expected return belongs in the notes.",
                        example = "6.2500")
                @DecimalMin(value = "0", message = "must not be negative")
                @DecimalMax(value = "100", message = "must be at most 100")
                @Digits(integer = 3, fraction = 4, message = "must have at most 3 integer digits and 4 decimals")
                BigDecimal interestRate) {}
