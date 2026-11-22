package com.rikkaus.wealth.liabilities.domain;

import com.rikkaus.wealth.shared.search.SortKey;

/**
 * The orders the liability catalog can be listed in. The first is the default.
 *
 * <p>A liability with no next payment date sorts last in both date orders, because "no date" is not
 * earlier or later than a date; it is a different kind of liability.
 */
public enum LiabilitySort implements SortKey {
    /** Soonest payment first, undated last, then by name: the order when no sort is asked for. */
    NEXT_PAYMENT_DATE_ASC("nextPaymentDate"),
    NEXT_PAYMENT_DATE_DESC("-nextPaymentDate"),
    NAME_ASC("name"),
    NAME_DESC("-name");

    private final String token;

    LiabilitySort(String token) {
        this.token = token;
    }

    @Override
    public String token() {
        return token;
    }
}
