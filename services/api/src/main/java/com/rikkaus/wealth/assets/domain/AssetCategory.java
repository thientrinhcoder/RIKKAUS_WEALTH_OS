package com.rikkaus.wealth.assets.domain;

/**
 * The eleven asset categories the MVP Backbone names, and no others.
 *
 * <p>An enum rather than free text, so a category that nobody agreed to cannot be created by a typo.
 * The same list is a check constraint in {@code V4__assets.sql}: the enum stops the API, the
 * constraint stops everything else.
 *
 * <p>Persisted by name, never by ordinal. Inserting a twelfth category in the middle of this list
 * would otherwise rewrite the meaning of every row already stored, silently and with no migration.
 *
 * <p>No Vietnamese label lives here. The published contract is English, the client maps each constant
 * to the copy the accepted design specifies — {@code BANK_ACCOUNT} renders as
 * <em>Tài khoản ngân hàng</em> — and duplicating that mapping on the server would create a second
 * source of product copy that nothing keeps in step with the first.
 */
public enum AssetCategory {
    CASH,
    BANK_ACCOUNT,
    TERM_DEPOSIT,
    LISTED_SECURITIES,
    BOND,
    INVESTMENT_FUND,
    REAL_ESTATE,
    PRIVATE_BUSINESS_EQUITY,
    VEHICLE_OR_VALUABLE_ASSET,
    COLLECTIBLE,
    OTHER_ASSET;

    /**
     * Whether a symbol, a cost per unit and a current price per unit are meaningful for this
     * category.
     *
     * <p>The accepted design's visibility matrix decides this, and the matrix is here rather than in
     * the service so that adding a twelfth category is a compilation problem somebody has to answer
     * rather than a silently permissive default. The same four constants appear in
     * {@code assets_instrument_pricing_suits_the_category} in
     * {@code V5__asset_instrument_details.sql}: this stops the API, the constraint stops everything
     * else.
     *
     * <p>{@link #OTHER_ASSET} is included because its matrix cell is <em>Mặc định không hiển thị</em> —
     * hidden by default, which is a statement about the form and not a prohibition. Refusing it would
     * be a rule this slice invented.
     */
    public boolean allowsInstrumentPricing() {
        return this == LISTED_SECURITIES || this == BOND || this == INVESTMENT_FUND || this == OTHER_ASSET;
    }

    /**
     * Whether an interest rate or expected yield is meaningful for this category.
     *
     * <p>Narrower than {@link #allowsInstrumentPricing()}, and the two places it is narrower are the
     * matrix's own answers rather than a simplification:
     *
     * <ul>
     *   <li>{@link #LISTED_SECURITIES} is <em>Không</em>. A listed share has no rate, and a field here
     *       would invite a dividend yield that nothing in MVP 1A calculates.
     *   <li>{@link #INVESTMENT_FUND} is <em>Ghi chú tùy chọn</em> — an optional note. A fund's expected
     *       return goes in {@code notes}, where no reader can mistake it for a computed figure.
     * </ul>
     *
     * <p>{@link #TERM_DEPOSIT} is the mirror image: a rate is the one instrument field that suits a
     * deposit, and a unit price is the one that does not.
     */
    public boolean allowsRate() {
        return this == BOND || this == TERM_DEPOSIT || this == OTHER_ASSET;
    }
}
