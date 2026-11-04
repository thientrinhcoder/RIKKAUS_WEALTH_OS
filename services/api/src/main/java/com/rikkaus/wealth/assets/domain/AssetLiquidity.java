package com.rikkaus.wealth.assets.domain;

/**
 * How readily the user judges an asset can be turned into cash.
 *
 * <p>Three qualitative levels and no time thresholds. The accepted design for issue #51 states the
 * guidance is qualitative, and inventing "within 7 days" here would publish a rule the Product Owner
 * never agreed to through a contract the frontend would then render as fact.
 *
 * <p>It is the user's judgement, not a derived property of the category: a listed holding in a thin
 * market and one in a liquid market are the same category and not the same liquidity, which is why
 * this is stored per record rather than looked up from {@link AssetCategory}.
 */
public enum AssetLiquidity {
    HIGH,
    MEDIUM,
    LOW
}
