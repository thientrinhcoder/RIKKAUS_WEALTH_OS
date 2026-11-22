package com.rikkaus.wealth.cashflow.domain;

import com.rikkaus.wealth.shared.search.SortKey;

/**
 * The orders the cash-flow catalog can be listed in. The first is the default.
 *
 * <p>No amount order: the catalog mixes VND and USD, and comparing the bare numbers would rank 350 USD
 * below 12.500.000 VND. Conversion is the client's, with the user's declared rate.
 */
public enum CashFlowEntrySort implements SortKey {
    /** Newest effective date first, then newest created: the order when no sort is asked for. */
    EFFECTIVE_DATE_DESC("-effectiveDate"),
    EFFECTIVE_DATE_ASC("effectiveDate");

    private final String token;

    CashFlowEntrySort(String token) {
        this.token = token;
    }

    @Override
    public String token() {
        return token;
    }
}
