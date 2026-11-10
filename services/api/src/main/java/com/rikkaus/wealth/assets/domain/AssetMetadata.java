package com.rikkaus.wealth.assets.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Everything about an asset that an edit may change.
 *
 * <p>One type used by both create and edit, so the editable set is defined once and the two paths
 * cannot drift into accepting different fields. What it leaves out is as deliberate as what it holds:
 * the category is chosen once in the create flow, and the valuation triple is {@link AssetValuation},
 * read-only after creation and owned by issue #57.
 *
 * <p>{@code liquidity} sits here rather than with the valuation because it is the user's standing
 * judgement about the holding, not a measurement taken on the valuation date, and the accepted design
 * for issue #51 lets an edit change it.
 *
 * <p>Every field except {@code name} and {@code liquidity} may be {@code null}: these are the
 * backbone's optional common fields, and a {@code null} is the absence of an answer rather than a
 * missing one. A replacement carrying {@code null} therefore clears the stored value, which is what
 * makes this a replacement and not a patch.
 */
public record AssetMetadata(
        String name,
        AssetLiquidity liquidity,
        BigDecimal purchaseValue,
        LocalDate purchaseDate,
        BigDecimal quantity,
        String institution,
        String notes) {}
