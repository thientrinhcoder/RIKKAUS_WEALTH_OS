package com.rikkaus.wealth.goals.domain;

import com.rikkaus.wealth.shared.search.QueryParameters;
import com.rikkaus.wealth.shared.search.SearchText;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What a listing of the goals is narrowed to and how it is ordered.
 *
 * <p>A goal has no notes, so the search reads the name alone. An empty set means every type.
 *
 * @param text the search, or {@code null} for none
 * @param types the templates to keep, or empty for all
 * @param sort the order
 */
public record GoalFilter(String text, Set<GoalType> types, GoalSort sort) {

    public GoalFilter {
        types = Set.copyOf(types);
    }

    /** Reads the query parameters, or throws one {@code validation-failed} naming every bad one. */
    public static GoalFilter of(String q, List<String> type, String sort) {
        List<Map<String, String>> errors = new ArrayList<>();
        GoalFilter filter =
                new GoalFilter(
                        SearchText.parse(q, errors),
                        QueryParameters.enumValues("type", type, GoalType.class, errors),
                        QueryParameters.sort(sort, GoalSort.class, GoalSort.TARGET_DATE_ASC, errors));
        QueryParameters.rejectIfAny(errors);
        return filter;
    }
}
