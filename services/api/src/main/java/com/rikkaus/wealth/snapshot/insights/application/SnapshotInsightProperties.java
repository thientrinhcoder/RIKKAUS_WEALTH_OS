package com.rikkaus.wealth.snapshot.insights.application;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The system reference thresholds the six wealth snapshot insight rules are judged against.
 *
 * <p>A share or ratio above its maximum, a valuation or rate older than its maximum age, and a liability
 * due within the window are what turn an insight to {@code ATTENTION}. A value equal to its threshold is
 * not above it. The accepted design for issue #66 shows these values as a review fixture, not as an
 * approved business configuration, which is why they live in configuration in one place: changing one is
 * a deployment decision, not a code change, and every response states the values it was judged with, so
 * a result can be reproduced after the configuration changes.
 *
 * <p>There is no default in this record. The values are in {@code application.yml}, and a missing or
 * meaningless value stops the application at start-up with the property named, because an insight judged
 * against a threshold nobody chose would be a silent product decision.
 *
 * @param maxCategoryShare the largest asset category's share of total assets above which it is reported
 * @param maxDebtToAssetRatio total liabilities over total assets above which the ratio is reported
 * @param maxLowLiquidityShare the share of assets marked {@code LOW} above which it is reported
 * @param dueWindowDays how many days after today a payment or maturity date still counts as upcoming
 * @param maxValuationAgeDays the age in days above which an asset's latest valuation is stale
 * @param maxExchangeRateAgeDays the age in days above which the declared USD/VND rate is stale
 */
@ConfigurationProperties("rikkaus.snapshot.insights")
public record SnapshotInsightProperties(
        BigDecimal maxCategoryShare,
        BigDecimal maxDebtToAssetRatio,
        BigDecimal maxLowLiquidityShare,
        Integer dueWindowDays,
        Integer maxValuationAgeDays,
        Integer maxExchangeRateAgeDays) {

    private static final String PREFIX = "rikkaus.snapshot.insights.";

    /** Observed fractions carry six decimals; a threshold with more could not be told apart from them. */
    static final int FRACTION_SCALE = 6;

    /** Ten years. A longer window or age is not a threshold anyone would choose, only a typo. */
    private static final int MAX_DAYS = 3650;

    public SnapshotInsightProperties {
        share(maxCategoryShare, "max-category-share");
        positiveFraction(maxDebtToAssetRatio, "max-debt-to-asset-ratio");
        share(maxLowLiquidityShare, "max-low-liquidity-share");
        days(dueWindowDays, "due-window-days");
        days(maxValuationAgeDays, "max-valuation-age-days");
        days(maxExchangeRateAgeDays, "max-exchange-rate-age-days");
    }

    /** A share of total assets can never exceed one, so a maximum above one would never be reached. */
    private static void share(BigDecimal value, String name) {
        positiveFraction(value, name);
        if (value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalStateException(PREFIX + name + " must not be greater than 1.");
        }
    }

    /** Debt may exceed assets, so the ratio has no upper bound; it still has to be a positive fraction. */
    private static void positiveFraction(BigDecimal value, String name) {
        if (value == null) {
            throw new IllegalStateException(PREFIX + name + " is not set.");
        }
        if (value.signum() <= 0) {
            throw new IllegalStateException(PREFIX + name + " must be greater than zero.");
        }
        if (value.stripTrailingZeros().scale() > FRACTION_SCALE) {
            throw new IllegalStateException(
                    PREFIX + name + " must have at most " + FRACTION_SCALE + " decimal places.");
        }
    }

    private static void days(Integer value, String name) {
        if (value == null) {
            throw new IllegalStateException(PREFIX + name + " is not set.");
        }
        if (value < 0 || value > MAX_DAYS) {
            throw new IllegalStateException(PREFIX + name + " must be between 0 and " + MAX_DAYS + " days.");
        }
    }
}
