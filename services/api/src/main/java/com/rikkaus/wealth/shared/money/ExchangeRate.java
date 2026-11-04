package com.rikkaus.wealth.shared.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * How many units of {@code to} one unit of {@code from} buys, and the date that rate takes effect.
 *
 * <p>The effective date is part of the rate rather than metadata beside it. The product backbone
 * requires a conversion to retain the rate and the date used, and a rate whose date can be dropped on
 * the way through a method signature is a rate that will be. Carrying them together means a stored
 * conversion can always be reconciled later, even after the user has entered a newer rate.
 *
 * <p>The date is a {@link LocalDate}, not an instant. "Effective 12/09/2026" is a business fact that
 * does not move with a timezone; storing it as a timestamp would make the same rate appear to take
 * effect on different days depending on where it is read.
 *
 * <p>The rate is normalized to {@link #RATE_SCALE} for the same reason {@link Money} normalizes its
 * amount: so two instances of the same rate are equal. It is not rounded to a currency's scale, which
 * would be wrong in both directions here — a VND-per-USD rate is not an amount of dong, and flooring
 * it to whole units would discard a rate like {@code 25100.5}.
 *
 * <p>A non-positive rate and a rate from a currency to itself are both rejected. A zero or negative
 * rate produces a zero or sign-flipped total that reads as real data, and a self-conversion is either
 * a no-op dressed up as a traceable business fact or a sign the caller has the arguments crossed.
 */
public record ExchangeRate(
        Currency from, Currency to, BigDecimal rate, LocalDate effectiveDate) {

    /**
     * Decimal places a rate is held to.
     *
     * <p>Six is well beyond what a hand-entered VND/USD rate needs and costs nothing, which is the
     * point: the scale exists to make equality total, not to bound precision. A rate entered with
     * more precision than this is rounded once, here, rather than at an unpredictable point inside a
     * later calculation.
     */
    public static final int RATE_SCALE = 6;

    public ExchangeRate {
        Objects.requireNonNull(from, "from must not be null");
        Objects.requireNonNull(to, "to must not be null");
        Objects.requireNonNull(rate, "rate must not be null");
        Objects.requireNonNull(effectiveDate, "effectiveDate must not be null");
        if (from == to) {
            throw new IllegalArgumentException(
                    "an exchange rate needs two different currencies, got %s twice"
                            .formatted(from.code()));
        }
        if (rate.signum() <= 0) {
            throw new IllegalArgumentException(
                    "exchange rate must be greater than zero, got %s".formatted(rate.toPlainString()));
        }
        rate = rate.setScale(RATE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Parses a decimal literal. Takes a {@link String} and never a {@code double}, so the entered
     * rate is the rate that is stored.
     */
    public static ExchangeRate of(
            Currency from, Currency to, String rate, LocalDate effectiveDate) {
        Objects.requireNonNull(rate, "rate must not be null");
        return new ExchangeRate(from, to, new BigDecimal(rate), effectiveDate);
    }

    /** Diagnostic form, for logs and exception messages rather than for a user. */
    @Override
    public String toString() {
        return "%s %s/%s effective %s"
                .formatted(rate.toPlainString(), to.code(), from.code(), effectiveDate);
    }
}
