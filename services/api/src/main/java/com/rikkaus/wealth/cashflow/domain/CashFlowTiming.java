package com.rikkaus.wealth.cashflow.domain;

/**
 * Whether an entry happens once or repeats.
 *
 * <p>The accepted design for issue #69 calls this <em>Thời điểm: Một lần / Định kỳ</em>. It decides
 * which of the other date fields may be present: a one-time entry has an effective date and nothing
 * else, a recurring one has a start date, a {@link CashFlowRecurrence} and an optional end date. That
 * rule is enforced in the service and in the table rather than inferred from which fields arrived, so
 * a request cannot be half of each.
 */
public enum CashFlowTiming {
    ONE_TIME,
    RECURRING
}
