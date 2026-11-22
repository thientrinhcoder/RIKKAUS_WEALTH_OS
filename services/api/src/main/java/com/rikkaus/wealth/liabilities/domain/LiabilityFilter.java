package com.rikkaus.wealth.liabilities.domain;

import com.rikkaus.wealth.shared.money.Currency;
import com.rikkaus.wealth.shared.search.QueryParameters;
import com.rikkaus.wealth.shared.search.SearchText;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What a listing of the liability catalog is narrowed to and how it is ordered.
 *
 * <p>An empty set is "no filter on this field", never "match nothing".
 *
 * @param text the search, or {@code null} for none; matched against name and notes
 * @param categories the categories to keep, or empty for all
 * @param currencies the currencies to keep, or empty for all
 * @param sort the order
 */
public record LiabilityFilter(
        String text, Set<LiabilityCategory> categories, Set<Currency> currencies, LiabilitySort sort) {

    public LiabilityFilter {
        categories = Set.copyOf(categories);
        currencies = Set.copyOf(currencies);
    }

    /** Reads the query parameters, or throws one {@code validation-failed} naming every bad one. */
    public static LiabilityFilter of(
            String q, List<String> category, List<String> currency, String sort) {
        List<Map<String, String>> errors = new ArrayList<>();
        LiabilityFilter filter =
                new LiabilityFilter(
                        SearchText.parse(q, errors),
                        QueryParameters.enumValues(
                                "category", category, LiabilityCategory.class, errors),
                        QueryParameters.enumValues("currency", currency, Currency.class, errors),
                        QueryParameters.sort(
                                sort,
                                LiabilitySort.class,
                                LiabilitySort.NEXT_PAYMENT_DATE_ASC,
                                errors));
        QueryParameters.rejectIfAny(errors);
        return filter;
    }
}
