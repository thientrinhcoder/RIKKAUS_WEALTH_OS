package com.rikkaus.wealth.assets.domain;

import com.rikkaus.wealth.shared.search.SortKey;

/** The orders the asset catalog can be listed in. The first is the default. */
public enum AssetSort implements SortKey {
    /** Newest valuation first, then by name: the catalog's order when no sort is asked for. */
    VALUATION_DATE_DESC("-valuationDate"),
    VALUATION_DATE_ASC("valuationDate"),
    NAME_ASC("name"),
    NAME_DESC("-name");

    private final String token;

    AssetSort(String token) {
        this.token = token;
    }

    @Override
    public String token() {
        return token;
    }
}
