package com.rikkaus.wealth.assets.domain;

import com.rikkaus.wealth.shared.search.QueryParameters;
import com.rikkaus.wealth.shared.search.SearchText;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What a listing of the catalog is narrowed to and how it is ordered.
 *
 * <p>An empty set is "no filter on this field", never "match nothing": a request without the parameter
 * and a request that cleared it both mean the whole catalog.
 *
 * @param text the search, or {@code null} for none; matched against name, institution and notes
 * @param categories the categories to keep, or empty for all
 * @param currencies the currencies to keep, or empty for all
 * @param liquidities the liquidity levels to keep, or empty for all
 * @param sort the order
 */
public record AssetFilter(
        String text,
        Set<AssetCategory> categories,
        Set<AssetCurrency> currencies,
        Set<AssetLiquidity> liquidities,
        AssetSort sort) {

    public AssetFilter {
        categories = Set.copyOf(categories);
        currencies = Set.copyOf(currencies);
        liquidities = Set.copyOf(liquidities);
    }

    /** Reads the query parameters, or throws one {@code validation-failed} naming every bad one. */
    public static AssetFilter of(
            String q,
            List<String> category,
            List<String> currency,
            List<String> liquidity,
            String sort) {
        List<Map<String, String>> errors = new ArrayList<>();
        AssetFilter filter =
                new AssetFilter(
                        SearchText.parse(q, errors),
                        QueryParameters.enumValues("category", category, AssetCategory.class, errors),
                        QueryParameters.enumValues("currency", currency, AssetCurrency.class, errors),
                        QueryParameters.enumValues(
                                "liquidity", liquidity, AssetLiquidity.class, errors),
                        QueryParameters.sort(
                                sort, AssetSort.class, AssetSort.VALUATION_DATE_DESC, errors));
        QueryParameters.rejectIfAny(errors);
        return filter;
    }
}
