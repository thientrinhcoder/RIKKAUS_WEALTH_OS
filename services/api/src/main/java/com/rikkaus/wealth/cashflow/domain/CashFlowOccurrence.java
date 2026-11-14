package com.rikkaus.wealth.cashflow.domain;

import java.time.LocalDate;

/**
 * One date on which an entry applies, derived from the entry and never stored.
 *
 * <p>The pair of {@code entry} and {@code date} is what identifies an occurrence. There is no
 * occurrence identifier of its own: an occurrence is a reading of its entry's rule, so it exists only
 * as long as that rule produces it, and giving it a key would suggest it could be edited apart from the
 * entry.
 *
 * @param entry the entry this occurrence was derived from
 * @param date the calendar date it falls on
 * @param sequence its position in the entry's series, 1 for the occurrence on the effective date
 * @param shiftedToMonthEnd whether the entry's day of month does not exist in this month, so the
 *     occurrence was moved back to the month's last day
 */
public record CashFlowOccurrence(
        CashFlowEntry entry, LocalDate date, int sequence, boolean shiftedToMonthEnd) {}
