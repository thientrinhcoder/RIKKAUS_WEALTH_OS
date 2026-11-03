package com.rikkaus.wealth.assets.api;

import com.rikkaus.wealth.assets.domain.AssetLiquidity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The editable half of an asset.
 *
 * <p>What it leaves out is the point. There is no {@code currentValue}, {@code currency} or
 * {@code valuationDate}, because the accepted design for issue #51 makes the current value read-only
 * in the edit form — <em>Chỉ đọc. Cập nhật định giá thuộc #57.</em> — and a new value has to be
 * appended to the valuation history issue #57 introduces rather than overwrite the one on the row.
 * There is no {@code category} either: it is chosen once, in the create flow's picker.
 *
 * <p>Those members are absent rather than ignored-with-a-warning, so the contract never describes a
 * field this route accepts and then discards. A hand-written request that sends one anyway is
 * silently dropped by Jackson, which is Spring Boot's own default for an unknown property and is
 * documented in {@code docs/api-contract-conventions.md} so a client is not surprised by it.
 *
 * <p>This is a replacement, not a patch: an optional field omitted from the body is cleared. That is
 * what makes "remove my note" expressible without a sentinel value.
 */
@Schema(description = "Replace an asset's editable metadata. The valuation is not editable here.")
public record UpdateAssetMetadataRequest(
        @Schema(
                        description = "What the user calls this asset.",
                        example = "Tài khoản USD dự phòng",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotBlank(message = "must not be blank")
                @Size(max = 120, message = "must be at most 120 characters")
                String name,
        @Schema(
                        description = "How readily the user judges this asset can be turned into cash.",
                        example = "HIGH",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull(message = "must not be null")
                AssetLiquidity liquidity,
        @Schema(
                        description = "Purchase value or cost basis, in the asset's own currency. Omit to clear it.",
                        example = "19500.00")
                @DecimalMin(value = "0", message = "must not be negative")
                @Digits(integer = 17, fraction = 2, message = "must have at most 17 integer digits and 2 decimals")
                BigDecimal purchaseValue,
        @Schema(
                        description =
                                "When the asset was acquired. Must not be in the future and must not be later "
                                        + "than the asset's stored valuation date. Omit to clear it.",
                        example = "2024-03-18")
                LocalDate purchaseDate,
        @Schema(
                        description = "Units held. Must be greater than zero when present. Omit to clear it.",
                        example = "20000.000000")
                @DecimalMin(value = "0", inclusive = false, message = "must be greater than zero")
                @Digits(integer = 13, fraction = 6, message = "must have at most 13 integer digits and 6 decimals")
                BigDecimal quantity,
        @Schema(description = "Where the asset is held or stored. Omit to clear it.", example = "Ngân hàng An Tín")
                @Size(max = 160, message = "must be at most 160 characters")
                String institution,
        @Schema(description = "Anything the user wants to remember. Omit to clear it.", example = "Đã chuyển sang kỳ hạn 6 tháng.")
                @Size(max = 2000, message = "must be at most 2000 characters")
                String notes) {}
