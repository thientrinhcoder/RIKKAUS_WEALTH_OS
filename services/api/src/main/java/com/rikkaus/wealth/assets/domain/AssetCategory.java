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
    OTHER_ASSET
}
