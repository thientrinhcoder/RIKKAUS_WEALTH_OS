package com.rikkaus.wealth.cashflow.api;

import com.rikkaus.wealth.cashflow.domain.CashFlowCategory;
import com.rikkaus.wealth.cashflow.domain.CashFlowEntry;
import com.rikkaus.wealth.cashflow.domain.CashFlowEntryType;
import com.rikkaus.wealth.cashflow.domain.CashFlowRecurrence;
import com.rikkaus.wealth.cashflow.domain.CashFlowTiming;
import com.rikkaus.wealth.shared.money.Currency;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One cash-flow entry, as the API describes it.
 *
 * <p>There is no owner member. Every entry a caller can reach is their own, so publishing the owner
 * would add an identifier to the wire that tells a client nothing it does not already know.
 *
 * <p>There is no converted VND amount, no exchange rate, no monthly total and no list of the months a
 * recurring entry falls in. A USD dividend reports {@code 150.00} and {@code "USD"}; the client
 * converts with the rate it already read from {@code GET /api/v1/settings/exchange-rate}. The totals
 * belong to issue #77 and the expansion to issue #74.
 *
 * <p>{@code linkedAssetId} and {@code linkedLiabilityId} are identifiers and nothing more. The entry
 * does not carry the linked record's name, value or balance, because this slice may not read those
 * tables through Java and because a client that has the identifier already has the record from its
 * own catalog. At most one of the two is set.
 */
@Schema(description = "A cash-flow entry in the signed-in user's catalog.")
public record CashFlowEntryResponse(
        @Schema(description = "The entry's identifier.", example = "5c0e2b7a-91d4-4f3b-8a6e-2d7f1c9b4e30")
                UUID id,
        @Schema(description = "INCOME or EXPENSE.", example = "INCOME") CashFlowEntryType entryType,
        @Schema(description = "One of the sixteen categories, always of the entry's type.", example = "SALARY")
                CashFlowCategory category,
        @Schema(description = "The amount, in the entry's own currency. Always positive.", example = "45000000.00")
                BigDecimal amount,
        @Schema(description = "The currency the amount is expressed in.", example = "VND") Currency currency,
        @Schema(description = "ONE_TIME or RECURRING.", example = "RECURRING") CashFlowTiming timing,
        @Schema(description = "MONTHLY, QUARTERLY or YEARLY for a recurring entry. Null for a one-time one.", example = "MONTHLY")
                CashFlowRecurrence recurrence,
        @Schema(description = "The date a one-time entry applies, or the first date of a recurring one.", example = "2026-01-01")
                LocalDate effectiveDate,
        @Schema(description = "The last date a recurring entry applies. Null when open-ended or one-time.", example = "2029-06-30")
                LocalDate endDate,
        @Schema(description = "The user's asset this entry refers to. Null when unlinked.", example = "a81d4f27-6c3e-4b19-8f55-0d2e7c9a3b64")
                UUID linkedAssetId,
        @Schema(description = "The user's liability this entry refers to. Null when unlinked.", example = "7d3e9a14-2c6b-4f81-a0d5-6b8e1f4c2a97")
                UUID linkedLiabilityId,
        @Schema(description = "The user's own note. Null when not recorded.", example = "Lương tháng") String notes,
        @Schema(description = "When the server first stored this entry.", example = "2026-11-14T10:42:11.503Z")
                Instant createdAt,
        @Schema(description = "When the server last stored a change to it.", example = "2026-11-14T10:42:11.503Z")
                Instant updatedAt) {

    static CashFlowEntryResponse of(CashFlowEntry entry) {
        return new CashFlowEntryResponse(
                entry.getId(),
                entry.getEntryType(),
                entry.getCategory(),
                entry.getAmount(),
                entry.getCurrency(),
                entry.getTiming(),
                entry.getRecurrence(),
                entry.getEffectiveDate(),
                entry.getEndDate(),
                entry.getLinkedAssetId(),
                entry.getLinkedLiabilityId(),
                entry.getNotes(),
                entry.getCreatedAt(),
                entry.getUpdatedAt());
    }
}
