package com.rikkaus.wealth.liabilities.api;

import com.rikkaus.wealth.liabilities.domain.LiabilityCategory;
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
 * A new liability: the four required fields and the nine optional ones.
 *
 * <p>There is no owner field, for the reason every owned resource gives: the liability belongs to
 * whoever the access token authenticates, and an ArchUnit rule fails the build on a controller that
 * binds an owner from the request.
 *
 * <p>{@code Digits} mirrors the column precision. {@code outstanding_balance} and {@code
 * recurring_payment_amount} are {@code NUMERIC(19, 2)} and {@code interest_rate} is {@code NUMERIC(7,
 * 4)}, so a value with too many digits is a {@code validation-failed} naming the field rather than a
 * 500 from PostgreSQL.
 *
 * <p>Three rules are not here because they relate two fields or need the database: maturity not
 * before start, next payment inside the term, the recurring amount and frequency as a pair, and the
 * collateral belonging to the viewer. {@code LiabilityService} applies them and reports them in the
 * same {@code errors} shape.
 *
 * <p>An unrecognised {@code category}, {@code currency} or {@code recurringPaymentFrequency} is a
 * {@code malformed-request} rather than a {@code validation-failed}, because Jackson cannot construct
 * the record at all. The real client chooses from a picker.
 */
@Schema(description = "Create a liability. The category is chosen here and cannot be changed later.")
public record CreateLiabilityRequest(
        @Schema(
                        description = "What the user calls this liability.",
                        example = "Vay mua nhà Vietcombank",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotBlank(message = "must not be blank")
                @Size(max = 120, message = "must be at most 120 characters")
                String name,
        @Schema(
                        description = "One of the six liability categories.",
                        example = "BANK_LOAN",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull(message = "must not be null")
                LiabilityCategory category,
        @Schema(
                        description =
                                "What is still owed, in this liability's currency, as the user states it. Zero "
                                        + "is allowed, negative is not.",
                        example = "2400000000.00",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull(message = "must not be null")
                @DecimalMin(value = "0", message = "must not be negative")
                @Digits(integer = 17, fraction = 2, message = "must have at most 17 integer digits and 2 decimals")
                BigDecimal outstandingBalance,
        @Schema(
                        description =
                                "The currency the balance and the recurring payment are expressed in. No "
                                        + "conversion happens here; the client holds the declared rate.",
                        example = "VND",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull(message = "must not be null")
                Currency currency,
        @Schema(
                        description =
                                "Nominal annual interest rate in percent, as the user states it: 8.5 means "
                                        + "8.5 % per year. Recorded only; nothing accrues from it.",
                        example = "8.5")
                @DecimalMin(value = "0", message = "must not be negative")
                @Digits(integer = 3, fraction = 4, message = "must have at most 3 integer digits and 4 decimals")
                BigDecimal interestRate,
        @Schema(description = "When the liability began.", example = "2024-03-15") LocalDate startDate,
        @Schema(
                        description = "When it is due to be settled. Must not be before the start date.",
                        example = "2044-03-15")
                LocalDate maturityDate,
        @Schema(
                        description =
                                "The total due each period, in the liability's currency. Must be greater than "
                                        + "zero, and must come with a frequency.",
                        example = "23500000.00")
                @DecimalMin(value = "0", inclusive = false, message = "must be greater than zero")
                @Digits(integer = 17, fraction = 2, message = "must have at most 17 integer digits and 2 decimals")
                BigDecimal recurringPaymentAmount,
        @Schema(
                        description = "How often the recurring payment falls due. Must come with an amount.",
                        example = "MONTHLY")
                PaymentFrequency recurringPaymentFrequency,
        @Schema(
                        description =
                                "The next date a payment is due. Must fall between the start and maturity "
                                        + "dates when those are given. May be in the past.",
                        example = "2026-12-05")
                LocalDate nextPaymentDate,
        @Schema(
                        description =
                                "The asset pledged against this liability. Must identify an asset the signed-in "
                                        + "user owns; any other identifier is refused with the field named.",
                        example = "a81d4f27-6c3e-4b19-8f55-0d2e7c9a3b64")
                UUID collateralAssetId,
        @Schema(description = "Anything the user wants to remember about it.", example = "Thế chấp căn hộ Thảo Điền.")
                @Size(max = 2000, message = "must be at most 2000 characters")
                String notes) {}
