package com.rikkaus.wealth.liabilities.api;

import com.rikkaus.wealth.liabilities.domain.Liability;
import com.rikkaus.wealth.liabilities.domain.LiabilityCategory;
import com.rikkaus.wealth.liabilities.domain.PaymentFrequency;
import com.rikkaus.wealth.shared.money.Currency;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One liability, as the API describes it.
 *
 * <p>There is no owner member: every liability a caller can reach is their own. There is no converted
 * VND amount, no exchange rate, no amortisation and no "amount paid so far". A USD guarantee reports
 * {@code 10000.00} and {@code "USD"}; the client multiplies by the rate it already read from the
 * settings resource and shows the audit trail the approved design specifies.
 *
 * <p>{@code collateralAssetId} is an identifier the client resolves against its own copy of the asset
 * catalog. It is {@code null} when no asset was pledged, and also after the pledged asset was deleted,
 * because the foreign key is {@code ON DELETE SET NULL}; the two states are the same to the client.
 */
@Schema(description = "A liability in the signed-in user's catalog.")
public record LiabilityResponse(
        @Schema(description = "The liability's identifier.", example = "5c0e2b7a-91d4-4f3e-8a6b-7d2c1e9f4a30")
                UUID id,
        @Schema(description = "What the user calls this liability.", example = "Vay mua nhà Vietcombank")
                String name,
        @Schema(description = "One of the six liability categories.", example = "BANK_LOAN")
                LiabilityCategory category,
        @Schema(description = "What is still owed, in its own currency.", example = "2400000000.00")
                BigDecimal outstandingBalance,
        @Schema(description = "The currency the amounts are expressed in.", example = "VND") Currency currency,
        @Schema(description = "Nominal annual rate in percent. Null when not recorded.", example = "8.5000")
                BigDecimal interestRate,
        @Schema(description = "When the liability began. Null when not recorded.", example = "2024-03-15")
                LocalDate startDate,
        @Schema(description = "When it is due to be settled. Null when not recorded.", example = "2044-03-15")
                LocalDate maturityDate,
        @Schema(description = "The total due each period. Null when there is no recurring payment.", example = "23500000.00")
                BigDecimal recurringPaymentAmount,
        @Schema(description = "How often it falls due. Null when there is no recurring payment.", example = "MONTHLY")
                PaymentFrequency recurringPaymentFrequency,
        @Schema(description = "The next date a payment is due. Null when not recorded.", example = "2026-12-05")
                LocalDate nextPaymentDate,
        @Schema(
                        description = "The pledged asset, owned by the same user. Null when none, or when that asset was deleted.",
                        example = "a81d4f27-6c3e-4b19-8f55-0d2e7c9a3b64")
                UUID collateralAssetId,
        @Schema(description = "The user's own note. Null when not recorded.", example = "Thế chấp căn hộ Thảo Điền.")
                String notes,
        @Schema(description = "When the server first stored this liability.", example = "2026-11-14T10:42:11.503Z")
                Instant createdAt,
        @Schema(description = "When the server last stored a change to it.", example = "2026-11-14T10:42:11.503Z")
                Instant updatedAt) {

    static LiabilityResponse of(Liability liability) {
        return new LiabilityResponse(
                liability.getId(),
                liability.getName(),
                liability.getCategory(),
                liability.getOutstandingBalance(),
                liability.getCurrency(),
                liability.getInterestRate(),
                liability.getStartDate(),
                liability.getMaturityDate(),
                liability.getRecurringPaymentAmount(),
                liability.getRecurringPaymentFrequency(),
                liability.getNextPaymentDate(),
                liability.getCollateralAssetId(),
                liability.getNotes(),
                liability.getCreatedAt(),
                liability.getUpdatedAt());
    }
}
