package com.rikkaus.wealth.liabilities.api;

import com.rikkaus.wealth.liabilities.domain.PaymentFrequency;
import com.rikkaus.wealth.shared.money.Currency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * The editable whole of a liability: every field of the create request except the category.
 *
 * <p>The balance is editable, unlike an asset's value. The accepted design's edit screen offers
 * <em>Dư nợ hiện tại</em> directly, and no append-only balance history exists for liabilities the way
 * issue #57 introduces one for valuations. The category is absent rather than ignored-with-a-warning:
 * it is chosen once in the create flow's picker, and a hand-written request that sends one anyway is
 * silently dropped by Jackson, Spring Boot's own default for an unknown property.
 *
 * <p>This is a replacement, not a patch. An optional field omitted from the body is cleared, which is
 * what makes "unlink the collateral" and "remove my note" expressible without a sentinel value.
 */
@Schema(description = "Replace a liability's editable fields. The category is not editable.")
public record UpdateLiabilityRequest(
        @Schema(
                        description = "What the user calls this liability.",
                        example = "Vay mua nhà Vietcombank",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotBlank(message = "must not be blank")
                @Size(max = 120, message = "must be at most 120 characters")
                String name,
        @Schema(
                        description = "What is still owed, as the user now states it. Zero is allowed, negative is not.",
                        example = "2376500000.00",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull(message = "must not be null")
                @DecimalMin(value = "0", message = "must not be negative")
                @Digits(integer = 17, fraction = 2, message = "must have at most 17 integer digits and 2 decimals")
                BigDecimal outstandingBalance,
        @Schema(
                        description = "The currency the balance and the recurring payment are expressed in.",
                        example = "VND",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull(message = "must not be null")
                Currency currency,
        @Schema(description = "Nominal annual interest rate in percent. Omit to clear it.", example = "8.5")
                @DecimalMin(value = "0", message = "must not be negative")
                @Digits(integer = 3, fraction = 4, message = "must have at most 3 integer digits and 4 decimals")
                BigDecimal interestRate,
        @Schema(description = "When the liability began. Omit to clear it.", example = "2024-03-15")
                LocalDate startDate,
        @Schema(
                        description = "When it is due to be settled. Must not be before the start date. Omit to clear it.",
                        example = "2044-03-15")
                LocalDate maturityDate,
        @Schema(
                        description = "The total due each period. Must be greater than zero and come with a frequency. Omit to clear the schedule.",
                        example = "23500000.00")
                @DecimalMin(value = "0", inclusive = false, message = "must be greater than zero")
                @Digits(integer = 17, fraction = 2, message = "must have at most 17 integer digits and 2 decimals")
                BigDecimal recurringPaymentAmount,
        @Schema(description = "How often the recurring payment falls due. Must come with an amount.", example = "MONTHLY")
                PaymentFrequency recurringPaymentFrequency,
        @Schema(
                        description = "The next date a payment is due, inside the term when the term is given. Omit to clear it.",
                        example = "2027-01-05")
                LocalDate nextPaymentDate,
        @Schema(
                        description = "The asset pledged against this liability; must be the signed-in user's. Omit to unlink.",
                        example = "a81d4f27-6c3e-4b19-8f55-0d2e7c9a3b64")
                UUID collateralAssetId,
        @Schema(description = "Anything the user wants to remember. Omit to clear it.", example = "Đã trả trước 23.500.000 ₫ tháng 12.")
                @Size(max = 2000, message = "must be at most 2000 characters")
                String notes) {}
