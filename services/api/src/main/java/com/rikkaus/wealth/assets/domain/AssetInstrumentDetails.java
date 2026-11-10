package com.rikkaus.wealth.assets.domain;

import java.math.BigDecimal;

/**
 * The optional instrument half of an asset: what the backbone calls the type-specific fields.
 *
 * <p>Separate from {@link AssetMetadata} because the two obey different rules. Every field in that
 * record is accepted on all eleven categories; every field in this one is accepted only on the
 * categories the accepted design's visibility matrix shows it on, which is what
 * {@link AssetCategory#allowsInstrumentPricing()} and {@link AssetCategory#allowsRate()} decide.
 * Keeping them apart means the applicability rule has one argument to inspect rather than having to
 * pick four members out of eleven.
 *
 * <p>{@code quantity} is deliberately not here. It is already a common optional field on
 * {@link AssetMetadata}, accepted on every category since issue #53, and the backbone lists it in
 * both the common-optional and the instrument sets because it is one datum. A second quantity would
 * make "how many units" a question with two answers.
 *
 * <p>Every field may be {@code null}, and all four being {@code null} is the ordinary case: the
 * accepted design's own criterion is that an asset missing every optional detail still saves. As with
 * {@link AssetMetadata}, this is a replacement and not a patch — a {@code null} arriving in an edit
 * clears the stored value, which is what makes "I recorded the wrong ticker, remove it" expressible
 * without a sentinel.
 *
 * <p>Nothing in this record is derived and nothing derived from it is stored. An asset's current
 * value is the user's own figure; it is never {@code quantity} multiplied by
 * {@code currentPricePerUnit}. The accepted design states this four times, and the absence of any
 * arithmetic in this slice is how it is kept true.
 *
 * @param symbol ticker, ISIN or fund code, as the user typed it. Nothing resolves it against a
 *     market.
 * @param costPerUnit what one unit cost, in the asset's own currency.
 * @param currentPricePerUnit what one unit is worth now, in the asset's own currency.
 * @param interestRate interest rate or expected yield as a percent per year, so {@code 6.2500} is
 *     6.25%. A percent rather than a fraction because it is what the user typed, and storing {@code
 *     0.0625} would mean the figure in the database never matched the figure on the form. No interest
 *     is accrued and no yield projected from it anywhere in MVP 1A.
 */
public record AssetInstrumentDetails(
        String symbol, BigDecimal costPerUnit, BigDecimal currentPricePerUnit, BigDecimal interestRate) {

    /** The absence of every instrument field: what an asset in a category without them carries. */
    public static AssetInstrumentDetails none() {
        return new AssetInstrumentDetails(null, null, null, null);
    }
}
