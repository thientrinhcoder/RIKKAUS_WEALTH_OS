package com.rikkaus.wealth.shared.search;

/**
 * One value a list route's {@code sort} parameter accepts.
 *
 * <p>Implemented by an enum in each slice, so the set of accepted orders is closed and each constant
 * says in one place what the client sends. A leading {@code -} means descending, as in {@code
 * -valuationDate}.
 */
public interface SortKey {

    /** The exact text of the {@code sort} parameter that selects this order. */
    String token();
}
