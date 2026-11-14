package com.rikkaus.wealth.liabilities.domain;

/**
 * How often a recurring payment falls due.
 *
 * <p>The four cadences the accepted design for issue #60 offers — <em>Hằng tháng, Hằng quý, Mỗi 6
 * tháng, Hằng năm</em> — and no finer one. A weekly or daily cadence is not something the design's
 * picker shows, so accepting it here would publish a value the client cannot render.
 *
 * <p>A cadence and nothing more. There is no "expand into dated payments" here, because the
 * cash-flow projection that needs that belongs to issues #74 and #77, and a method on this enum
 * would be the first line of it.
 */
public enum PaymentFrequency {
    MONTHLY,
    QUARTERLY,
    SEMIANNUAL,
    YEARLY
}
