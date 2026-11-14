package com.rikkaus.wealth.liabilities.domain;

import com.rikkaus.wealth.shared.money.Currency;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Everything about a liability that an edit may change — which is everything but the category.
 *
 * <p>One type used by both create and edit, so the editable set is defined once and the two paths
 * cannot drift. Unlike an asset, the balance is editable: the accepted design's edit screen offers
 * <em>Dư nợ hiện tại</em>, and there is no append-only balance history for liabilities the way issue
 * #57 introduces one for asset valuations. The category is chosen once, in the create flow's picker,
 * and is the one member absent here.
 *
 * <p>{@code collateralAssetId} is a bare identifier and never an asset entity. The slice rule in
 * {@code ArchitectureRulesTest} forbids this package from importing anything from {@code assets}; the
 * link is a foreign key in the schema and an ownership query in {@link LiabilityRepository}.
 *
 * <p>Every member but {@code name}, {@code outstandingBalance} and {@code currency} may be {@code
 * null}. A replacement carrying {@code null} clears the stored value, which is what makes {@code PUT}
 * a replacement and not a patch.
 */
public record LiabilityDetails(
        String name,
        BigDecimal outstandingBalance,
        Currency currency,
        LiabilityTerms terms,
        PaymentSchedule schedule,
        UUID collateralAssetId,
        String notes) {}
