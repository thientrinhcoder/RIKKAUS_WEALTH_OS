package com.rikkaus.wealth.cashflow.api;

import com.rikkaus.wealth.cashflow.domain.CashFlowCategory;
import com.rikkaus.wealth.cashflow.domain.CashFlowEntry;
import com.rikkaus.wealth.cashflow.domain.CashFlowEntryType;
import com.rikkaus.wealth.cashflow.domain.CashFlowOccurrence;
import com.rikkaus.wealth.cashflow.domain.CashFlowRecurrence;
import com.rikkaus.wealth.shared.money.Currency;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One date an entry falls on, as the API describes it.
 *
 * <p>The entry's money and classification are copied onto the occurrence so a monthly report can sum
 * a window without a second request per entry. Nothing else is: the note, the links and the dates of
 * the rule stay on the entry, which {@code sourceEntryId} reads back.
 *
 * <p>No converted VND amount. A USD dividend's occurrence reports {@code 150.00} and {@code "USD"}, as
 * the entry does.
 */
@Schema(description = "A date on which one of the signed-in user's cash-flow entries applies.")
public record CashFlowOccurrenceResponse(
        @Schema(
                        description = "The entry this occurrence comes from; GET /api/v1/cash-flow-entries/{id} reads it.",
                        example = "9b4d6f12-7e3a-4c58-b1d9-0a2e5c7f8d61")
                UUID sourceEntryId,
        @Schema(description = "The calendar date the occurrence falls on.", example = "2026-02-28")
                LocalDate occurrenceDate,
        @Schema(
                        description =
                                "The occurrence's position in its entry's series, counted from the effective date."
                                        + " Always 1 for a one-time entry.",
                        example = "2")
                int sequence,
        @Schema(
                        description =
                                "True when the entry's day of month does not exist in this month, so the occurrence"
                                        + " was moved back to the month's last day.",
                        example = "true")
                boolean shiftedToMonthEnd,
        @Schema(description = "INCOME or EXPENSE, as on the entry.", example = "INCOME") CashFlowEntryType entryType,
        @Schema(description = "The entry's category.", example = "SALARY") CashFlowCategory category,
        @Schema(description = "The entry's amount, in its own currency. Always positive.", example = "45000000.00")
                BigDecimal amount,
        @Schema(description = "The currency the amount is expressed in.", example = "VND") Currency currency,
        @Schema(
                        description = "MONTHLY, QUARTERLY or YEARLY for a recurring entry. Null for a one-time one.",
                        example = "MONTHLY")
                CashFlowRecurrence recurrence) {

    static CashFlowOccurrenceResponse of(CashFlowOccurrence occurrence) {
        CashFlowEntry entry = occurrence.entry();
        return new CashFlowOccurrenceResponse(
                entry.getId(),
                occurrence.date(),
                occurrence.sequence(),
                occurrence.shiftedToMonthEnd(),
                entry.getEntryType(),
                entry.getCategory(),
                entry.getAmount(),
                entry.getCurrency(),
                entry.getRecurrence());
    }
}
