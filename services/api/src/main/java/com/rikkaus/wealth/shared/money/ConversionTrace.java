package com.rikkaus.wealth.shared.money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A completed conversion together with everything needed to reconcile it: the amount that was
 * entered, the rate and effective date applied, and the result.
 *
 * <p>This type exists because the product backbone requires that a conversion retain the rate and
 * date used, and the accepted design for issue #43 shows the user the whole chain behind a "Cách
 * tính" disclosure. A method that returned only the converted {@link Money} would satisfy the
 * arithmetic and lose the audit trail, leaving the caller to carry the rate alongside the result by
 * convention. Returning the chain as one value makes that impossible to forget.
 *
 * <p>{@link #convert} rounds exactly once. The product of an exact amount and an exact rate is
 * itself exact, and only the final {@link Money} construction rounds it to the target currency's
 * scale. Converting in two steps — rounding to a currency and then adjusting — would round twice and
 * drift from what the user is shown.
 *
 * <p>Worked against the specimen the design froze: {@code 20000.00 USD} times a rate of {@code
 * 25100.000000} is {@code 502000000.00000000}, which normalizes to {@code 502000000 VND} because VND
 * has no subunit. Nothing is lost, because the product is exact before the single rounding step.
 */
public record ConversionTrace(Money original, ExchangeRate rate, Money converted) {

    public ConversionTrace {
        Objects.requireNonNull(original, "original must not be null");
        Objects.requireNonNull(rate, "rate must not be null");
        Objects.requireNonNull(converted, "converted must not be null");
        if (original.currency() != rate.from()) {
            throw new IllegalArgumentException(
                    "rate converts from %s but the original amount is in %s"
                            .formatted(rate.from().code(), original.currency().code()));
        }
        if (converted.currency() != rate.to()) {
            throw new IllegalArgumentException(
                    "rate converts to %s but the converted amount is in %s"
                            .formatted(rate.to().code(), converted.currency().code()));
        }
    }

    /**
     * Applies {@code rate} to {@code original} and keeps the whole chain.
     *
     * @throws IllegalArgumentException if the rate does not convert from the amount's currency,
     *     which means the caller has the wrong rate rather than an amount worth converting
     */
    public static ConversionTrace convert(Money original, ExchangeRate rate) {
        Objects.requireNonNull(original, "original must not be null");
        Objects.requireNonNull(rate, "rate must not be null");
        if (original.currency() != rate.from()) {
            throw new IllegalArgumentException(
                    "cannot convert %s with a rate from %s"
                            .formatted(original.currency().code(), rate.from().code()));
        }
        BigDecimal exactProduct = original.amount().multiply(rate.rate());
        return new ConversionTrace(original, rate, Money.of(exactProduct, rate.to()));
    }

    /** The date the applied rate took effect. Surfaced here so a caller need not reach through. */
    public LocalDate effectiveDate() {
        return rate.effectiveDate();
    }
}
