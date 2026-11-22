package com.rikkaus.wealth.cashflow.reporting.application;

import com.rikkaus.wealth.cashflow.application.OccurrenceWindow;
import com.rikkaus.wealth.shared.error.ApiException;
import com.rikkaus.wealth.shared.error.ProblemType;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The calendar month a report covers, from its first day to its last, both included.
 *
 * <p>Built from the raw query text for the same reason as {@link OccurrenceWindow}: Spring's own
 * binding answers a missing or unparseable value with {@code malformed-request} and no field name,
 * while every other rejected input in the API is a {@code validation-failed} naming the field.
 *
 * <p>No comparison with today. A report is a reading of the catalog for a month, so a past, current
 * and future month are answered the same way, and the answer does not depend on when it is asked.
 *
 * @param value the month
 */
public record ReportMonth(YearMonth value) {

    /**
     * Four-digit year, a hyphen and a two-digit month, nothing else. {@code YearMonth.parse} alone
     * accepts a signed year with more than four digits.
     */
    private static final Pattern YEAR_MONTH = Pattern.compile("\\d{4}-\\d{2}");

    /**
     * Validates the parameter and returns the month, or throws one {@code validation-failed} naming
     * {@code month}. The message never repeats the value sent, so nothing the caller typed is
     * reflected into the response.
     */
    public static ReportMonth of(String text) {
        if (text == null || text.isBlank()) {
            throw invalid("must be present");
        }
        if (YEAR_MONTH.matcher(text).matches()) {
            try {
                // Strict: 2026-13 and 2026-00 are refused rather than rolled into another year.
                return new ReportMonth(YearMonth.parse(text));
            } catch (DateTimeParseException notACalendarMonth) {
                // Reported below with the same message as a malformed value.
            }
        }
        throw invalid("must be a calendar month in the form YYYY-MM");
    }

    /** The first day of the month. */
    public LocalDate first() {
        return value.atDay(1);
    }

    /** The last day of the month: the 28th, 29th, 30th or 31st. */
    public LocalDate last() {
        return value.atEndOfMonth();
    }

    /** The occurrence window of #74 that covers exactly this month. */
    OccurrenceWindow window() {
        return new OccurrenceWindow(first(), last());
    }

    private static ApiException invalid(String message) {
        return new ApiException(
                ProblemType.VALIDATION_FAILED,
                ProblemType.VALIDATION_FAILED.safeDetail(),
                Map.of("errors", List.of(Map.of("field", "month", "message", message))));
    }
}
