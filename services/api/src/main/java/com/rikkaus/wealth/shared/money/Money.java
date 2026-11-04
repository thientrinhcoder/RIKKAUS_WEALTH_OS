package com.rikkaus.wealth.shared.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * An amount bound to the currency it is denominated in.
 *
 * <p>{@link BigDecimal} rather than any binary floating-point type, because the architecture
 * decisions forbid floating point for stored monetary values: {@code 0.1 + 0.2} is not {@code 0.3}
 * in a {@code double}, and the error compounds through a sum of valuations until a displayed total is
 * visibly wrong.
 *
 * <p>An amount is normalized to its currency's scale on construction, rounding {@link
 * RoundingMode#HALF_UP}. That is what makes equality usable: {@code BigDecimal.equals} compares scale
 * as well as value, so without normalization {@code 380000000} and {@code 380000000.0} would be two
 * unequal instances of the same amount, and every comparison in the codebase would have to remember
 * to call {@code compareTo}. The cost is that construction is the rounding point, so a calculation
 * that must not round twice has to reach its final currency in a single step. {@link ConversionTrace}
 * does exactly that.
 *
 * <p>Arithmetic refuses to mix currencies. Adding dong to dollars has no answer without a rate, and
 * the rate is a traceable business fact rather than something arithmetic may invent, so the mismatch
 * is a programming error and is reported as one. These types are framework-free, so that is a plain
 * {@link IllegalArgumentException}; turning rejected user input into a problem response stays the API
 * layer's job.
 */
public record Money(BigDecimal amount, Currency currency) implements Comparable<Money> {

    public Money {
        Objects.requireNonNull(currency, "currency must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        amount = amount.setScale(currency.scale(), RoundingMode.HALF_UP);
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    /**
     * Parses a decimal literal. Takes a {@link String} and never a {@code double}, so no caller can
     * introduce a binary rounding error on the way in.
     */
    public static Money of(String amount, Currency currency) {
        Objects.requireNonNull(amount, "amount must not be null");
        return new Money(new BigDecimal(amount), currency);
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    public Money negate() {
        return new Money(amount.negate(), currency);
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    /**
     * Orders two amounts in the same currency.
     *
     * <p>Comparing across currencies throws rather than returning an order, for the same reason
     * adding across them does. This narrows {@link Comparable} to amounts that are genuinely
     * comparable, so a mixed-currency collection cannot be sorted into a meaningless sequence.
     */
    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount);
    }

    /** Diagnostic form, for logs and exception messages rather than for a user. */
    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency.code();
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other must not be null");
        if (currency != other.currency) {
            throw new IllegalArgumentException(
                    "cannot combine %s with %s without an exchange rate"
                            .formatted(currency.code(), other.currency.code()));
        }
    }
}
