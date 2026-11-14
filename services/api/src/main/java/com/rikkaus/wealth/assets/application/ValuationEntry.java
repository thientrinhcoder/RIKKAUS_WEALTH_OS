package com.rikkaus.wealth.assets.application;

import com.rikkaus.wealth.assets.domain.AssetValuation;

/**
 * A stored valuation together with whether it is the one the asset's current value comes from.
 *
 * <p>The flag is not a column — {@link AssetValuation} explains why — so it has to be decided by
 * whoever has the history's order in hand, which is the service. Handing the record and the answer
 * back as one value means the API layer never has to work it out from a list position, and never has
 * to make a second call to learn it.
 */
public record ValuationEntry(AssetValuation valuation, boolean current) {}
