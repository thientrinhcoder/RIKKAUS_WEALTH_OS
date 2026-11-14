package com.rikkaus.wealth.cashflow.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Expands an entry into the dates it falls on within a window of days.
 *
 * <p>A pure function of the entry and the window: no clock, no zone, no database. Every value is a
 * calendar date, so the result is the same whatever zone the server runs in and whatever day it is
 * asked on, which is what lets a monthly report and a forecast agree about the same month.
 *
 * <p>The rules are the five the accepted design for issue #72 sets out:
 *
 * <ul>
 *   <li>The effective date is included; the first occurrence falls on it.
 *   <li>The end date is included; an occurrence that falls on it is produced.
 *   <li>A day of month the month does not have is moved back to that month's last day, and never into
 *       the next month. The entry's own day is kept, so the month after a shortened February is back
 *       on the 31st.
 *   <li>Each occurrence belongs to the month that contains its date, which follows from the rule above.
 *   <li>The window is closed: both {@code from} and {@code to} are included. Two windows that meet
 *       without sharing a day — the first quarter and the second — therefore count every occurrence
 *       exactly once between them.
 * </ul>
 */
public final class CashFlowSchedule {

    private CashFlowSchedule() {}

    /**
     * The entry's occurrences from {@code from} to {@code to}, both included, earliest first.
     *
     * <p>Occurrence {@code n} is the effective date plus {@code n} steps, always counted from the
     * effective date and never from the previous occurrence. That is the whole of the end-of-month
     * rule: {@code plusMonths} clamps a day the month lacks to its last day, and because each addition
     * starts again from the 31st, the clamp in February is not carried into March.
     */
    public static List<CashFlowOccurrence> occurrencesOf(
            CashFlowEntry entry, LocalDate from, LocalDate to) {
        Objects.requireNonNull(entry, "entry must not be null");
        Objects.requireNonNull(from, "from must not be null");
        Objects.requireNonNull(to, "to must not be null");

        LocalDate anchor = entry.getEffectiveDate();
        if (entry.getTiming() == CashFlowTiming.ONE_TIME) {
            return inWindow(anchor, from, to)
                    ? List.of(new CashFlowOccurrence(entry, anchor, 1, false))
                    : List.of();
        }

        int step = entry.getRecurrence().months();
        LocalDate endDate = entry.getEndDate();
        LocalDate last = endDate == null || endDate.isAfter(to) ? to : endDate;
        List<CashFlowOccurrence> occurrences = new ArrayList<>();
        for (long n = firstCandidate(anchor, from, step); ; n++) {
            LocalDate date = anchor.plusMonths(n * step);
            if (date.isAfter(last)) {
                return occurrences;
            }
            if (!date.isBefore(from)) {
                occurrences.add(
                        new CashFlowOccurrence(
                                entry,
                                date,
                                Math.toIntExact(n + 1),
                                date.getDayOfMonth() != anchor.getDayOfMonth()));
            }
        }
    }

    /**
     * An index no later than the first occurrence on or after {@code from}.
     *
     * <p>Skips the occurrences that are certainly before the window, so an entry that started decades
     * ago is not walked from its first month. {@code MONTHS.between} never overstates the distance, and
     * one more step is taken back so a clamped month-end cannot put the first candidate inside the
     * window. Occurrences are strictly increasing, so nothing before the index can be in the window.
     */
    private static long firstCandidate(LocalDate anchor, LocalDate from, int step) {
        long months = ChronoUnit.MONTHS.between(anchor, from);
        return Math.max(0, months / step - 1);
    }

    private static boolean inWindow(LocalDate date, LocalDate from, LocalDate to) {
        return !date.isBefore(from) && !date.isAfter(to);
    }
}
