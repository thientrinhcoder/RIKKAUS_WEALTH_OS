package com.rikkaus.wealth.cashflow.domain;

/**
 * How often a recurring entry repeats.
 *
 * <p>Three cadences, because the MVP Backbone names three. No weekly, no custom rule, no day-of-month
 * override: the schedule expansion in issue #74 is written against exactly this set, and a cadence
 * added here without that slice knowing would be a row it cannot expand.
 */
public enum CashFlowRecurrence {
    MONTHLY,
    QUARTERLY,
    YEARLY
}
