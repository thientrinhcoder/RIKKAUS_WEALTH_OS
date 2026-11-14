package com.rikkaus.wealth.cashflow.application;

import com.rikkaus.wealth.shared.error.ApiException;
import com.rikkaus.wealth.shared.error.ProblemType;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The days an occurrence query covers, from {@code from} to {@code to}, both included.
 *
 * <p>Built from the raw query text rather than bound by Spring as {@code LocalDate}. Spring's own
 * binding answers a missing or unparseable parameter with {@code malformed-request} and no field
 * name, while every other resource reports a rejected input as {@code validation-failed} with an
 * {@code errors} entry naming the field. Parsing here keeps one error shape for the client and lets a
 * request with two mistakes be told about both at once.
 *
 * @param from the first day included
 * @param to the last day included
 */
public record OccurrenceWindow(LocalDate from, LocalDate to) {

    /**
     * The longest window, in calendar months. {@code to} must fall before {@code from} plus this many
     * months, so {@code 2026-01-01..2027-12-31} is accepted and {@code 2026-01-01..2028-01-01} is not.
     *
     * <p>Twenty-four because the accepted design's longest horizon is twenty-four monthly occurrences.
     * The known consumers need one month (the monthly report) and three (the forecast).
     */
    public static final int MAX_MONTHS = 24;

    /**
     * Four-digit year, two-digit month and day, nothing else. {@code LocalDate.parse} alone accepts a
     * signed year with more than four digits, and the window arithmetic is only safe within the years
     * a person can mean.
     */
    private static final Pattern ISO_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    /**
     * Validates both parameters and returns the window, or throws one {@code validation-failed} naming
     * every problem.
     *
     * <p>Order and length are judged only when both dates are valid. A request with a malformed
     * {@code from} has one mistake, and reporting that an unparseable value is also out of order would
     * describe a problem the user does not have. The messages never repeat the value sent, so nothing
     * the caller typed is reflected into the response.
     */
    public static OccurrenceWindow of(String fromText, String toText) {
        List<Map<String, String>> errors = new ArrayList<>();
        LocalDate from = parse("from", fromText, errors);
        LocalDate to = parse("to", toText, errors);

        if (from != null && to != null) {
            if (to.isBefore(from)) {
                errors.add(error("to", "must not be before from"));
            } else if (!to.isBefore(from.plusMonths(MAX_MONTHS))) {
                errors.add(error("to", "must be less than " + MAX_MONTHS + " months after from"));
            }
        }

        if (!errors.isEmpty()) {
            throw new ApiException(
                    ProblemType.VALIDATION_FAILED,
                    ProblemType.VALIDATION_FAILED.safeDetail(),
                    Map.of("errors", List.copyOf(errors)));
        }
        return new OccurrenceWindow(from, to);
    }

    private static LocalDate parse(String field, String text, List<Map<String, String>> errors) {
        if (text == null || text.isBlank()) {
            errors.add(error(field, "must be present"));
            return null;
        }
        if (ISO_DATE.matcher(text).matches()) {
            try {
                // ISO_LOCAL_DATE resolves strictly, so 2026-02-29 and 2026-04-31 are refused rather
                // than quietly moved to the month's last day.
                return LocalDate.parse(text);
            } catch (DateTimeParseException notACalendarDate) {
                // Reported below with the same message as a malformed value.
            }
        }
        errors.add(error(field, "must be a calendar date in the form YYYY-MM-DD"));
        return null;
    }

    private static Map<String, String> error(String field, String message) {
        return Map.of("field", field, "message", message);
    }
}
