package com.rikkaus.wealth.cashflow.domain;

/**
 * How often a recurring entry repeats.
 *
 * <p>Three cadences, because the MVP Backbone names three. No weekly, no custom rule, no day-of-month
 * override: {@link CashFlowSchedule} is written against exactly this set, and every cadence is a whole
 * number of calendar months so the end-of-month rule is the same for all of them.
 */
public enum CashFlowRecurrence {
    MONTHLY(1),
    QUARTERLY(3),
    YEARLY(12);

    private final int months;

    CashFlowRecurrence(int months) {
        this.months = months;
    }

    /** The calendar months between two consecutive occurrences. */
    public int months() {
        return months;
    }
}
