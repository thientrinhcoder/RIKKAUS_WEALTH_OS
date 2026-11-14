package com.rikkaus.wealth.assets.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * What a user declares when they state what an asset is worth.
 *
 * <p>Seven things, and they arrive together because none of them means anything alone: a value
 * without its currency cannot be summed, a value without its date cannot be judged stale, and a USD
 * value without the rate that was used cannot be reconciled later. This is the input to both places a
 * valuation is born — the first one when an asset is created, every later one through the history
 * resource — so the two cannot drift into accepting different fields.
 *
 * <p>It is the input, not the record. {@link AssetValuation} is what the server stored, with the
 * identifier, the asset and the server's own timestamp added; this carries only what the user said.
 *
 * <p>{@code usdToVndRate} and {@code rateEffectiveDate} are the snapshot the accepted design for issue
 * #57 calls <em>Tỷ giá USD/VND đã dùng</em>: the rate the user applied for this valuation, not the one
 * currently declared in settings. Both may be {@code null} together, which is the design's
 * <em>partial</em> state rather than an error. {@code note} and {@code correctsValuationId} are
 * optional; a correction names the record it corrects and carries its reason in the note.
 *
 * <p>{@link BigDecimal} and never a floating-point type: the value is summed across a portfolio and
 * the rate is multiplied into it.
 */
public record DeclaredValuation(
        BigDecimal value,
        AssetCurrency currency,
        LocalDate effectiveDate,
        BigDecimal usdToVndRate,
        LocalDate rateEffectiveDate,
        String note,
        UUID correctsValuationId) {

    /** The declaration an asset is created with: value, currency, date and the optional rate snapshot. */
    public static DeclaredValuation initial(
            BigDecimal value,
            AssetCurrency currency,
            LocalDate effectiveDate,
            BigDecimal usdToVndRate,
            LocalDate rateEffectiveDate) {
        return new DeclaredValuation(
                value, currency, effectiveDate, usdToVndRate, rateEffectiveDate, null, null);
    }

    /** The same declaration with its note replaced, for the service that cleans free text. */
    public DeclaredValuation withNote(String cleanedNote) {
        return new DeclaredValuation(
                value, currency, effectiveDate, usdToVndRate, rateEffectiveDate, cleanedNote, correctsValuationId);
    }
}
