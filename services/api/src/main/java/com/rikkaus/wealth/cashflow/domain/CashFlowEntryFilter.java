package com.rikkaus.wealth.cashflow.domain;

import com.rikkaus.wealth.shared.money.Currency;
import com.rikkaus.wealth.shared.search.QueryParameters;
import com.rikkaus.wealth.shared.search.SearchText;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What a listing of the cash-flow catalog is narrowed to and how it is ordered.
 *
 * <p>The date range is on the stored effective date, both ends included. It is not a question about
 * occurrences: a monthly salary that started in January has an effective date in January and is not in
 * a September range, even though it falls in September. Which entries occur in a month is what {@code
 * GET /api/v1/cash-flow-occurrences} answers.
 *
 * @param text the search, or {@code null} for none; matched against the notes, the one free-text field
 * @param entryTypes the types to keep, or empty for both
 * @param categories the categories to keep, or empty for all
 * @param timings one-time, recurring, or empty for both
 * @param recurrences the recurrences to keep, or empty for all; a one-time entry has none and is
 *     dropped by any value here
 * @param currencies the currencies to keep, or empty for all
 * @param effectiveFrom the earliest effective date kept, or {@code null}
 * @param effectiveTo the latest effective date kept, or {@code null}
 * @param sort the order
 */
public record CashFlowEntryFilter(
        String text,
        Set<CashFlowEntryType> entryTypes,
        Set<CashFlowCategory> categories,
        Set<CashFlowTiming> timings,
        Set<CashFlowRecurrence> recurrences,
        Set<Currency> currencies,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        CashFlowEntrySort sort) {

    public CashFlowEntryFilter {
        entryTypes = Set.copyOf(entryTypes);
        categories = Set.copyOf(categories);
        timings = Set.copyOf(timings);
        recurrences = Set.copyOf(recurrences);
        currencies = Set.copyOf(currencies);
    }

    /** Reads the query parameters, or throws one {@code validation-failed} naming every bad one. */
    public static CashFlowEntryFilter of(
            String q,
            List<String> entryType,
            List<String> category,
            List<String> timing,
            List<String> recurrence,
            List<String> currency,
            String effectiveFrom,
            String effectiveTo,
            String sort) {
        List<Map<String, String>> errors = new ArrayList<>();
        String text = SearchText.parse(q, errors);
        Set<CashFlowEntryType> entryTypes =
                QueryParameters.enumValues("entryType", entryType, CashFlowEntryType.class, errors);
        Set<CashFlowCategory> categories =
                QueryParameters.enumValues("category", category, CashFlowCategory.class, errors);
        Set<CashFlowTiming> timings =
                QueryParameters.enumValues("timing", timing, CashFlowTiming.class, errors);
        Set<CashFlowRecurrence> recurrences =
                QueryParameters.enumValues("recurrence", recurrence, CashFlowRecurrence.class, errors);
        Set<Currency> currencies =
                QueryParameters.enumValues("currency", currency, Currency.class, errors);
        LocalDate from = QueryParameters.date("effectiveFrom", effectiveFrom, errors);
        LocalDate to = QueryParameters.date("effectiveTo", effectiveTo, errors);
        QueryParameters.requireOrdered("effectiveFrom", from, "effectiveTo", to, errors);
        CashFlowEntrySort order =
                QueryParameters.sort(
                        sort, CashFlowEntrySort.class, CashFlowEntrySort.EFFECTIVE_DATE_DESC, errors);
        QueryParameters.rejectIfAny(errors);
        return new CashFlowEntryFilter(
                text, entryTypes, categories, timings, recurrences, currencies, from, to, order);
    }
}
