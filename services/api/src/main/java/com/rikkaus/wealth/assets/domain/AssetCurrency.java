package com.rikkaus.wealth.assets.domain;

/**
 * The currencies a holding's value may be expressed in.
 *
 * <p>Two, because the MVP Backbone says two. Distinct from {@code UserSettings.BASE_CURRENCY_VND},
 * which is the single <em>reporting</em> currency: a pilot user may own a USD account and still be
 * reported in VND, so the two concepts are not the same field with a different name.
 *
 * <p>Nothing in this slice converts between them. The user-declared rate lives in the {@code
 * settings} slice, which the slice rule in {@code ArchitectureRulesTest} forbids this one from
 * reaching, and the client already holds that rate from {@code GET /api/v1/settings/exchange-rate}.
 * An asset therefore reports the amount the user entered and the currency they entered it in, and the
 * conversion happens where the rate already is.
 */
public enum AssetCurrency {
    VND,
    USD
}
