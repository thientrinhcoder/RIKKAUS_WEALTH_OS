package com.rikkaus.wealth.cashflow.forecast.application;

import com.rikkaus.wealth.shared.error.ApiException;
import com.rikkaus.wealth.shared.error.ProblemType;
import com.rikkaus.wealth.shared.validation.EnteredDates;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The three whole calendar months a forecast covers, and the day it is computed on.
 *
 * <p>Always three months and always whole months: the accepted design for issue #78 shows the next
 * three complete months and nothing shorter or longer, so the length is not a parameter.
 *
 * <p>The first month is never the current one or an earlier one. The opening balance is the cash the
 * user has recorded today, which already reflects whatever has happened this month, so projecting the
 * current month again would count its payments twice. The days between today and the first month are
 * not projected at all; {@link #notProjectedFrom()} and {@link #notProjectedTo()} name them so the
 * response can say so instead of hiding it.
 *
 * @param first the first month of the horizon
 * @param asOfDate the day the forecast is computed on, in the display zone
 */
public record ForecastHorizon(YearMonth first, LocalDate asOfDate) {

    /** How many months a forecast covers. */
    public static final int LENGTH = 3;

    /**
     * The furthest first month, counted from the current month. Twelve keeps a request from asking for
     * a forecast whose unprojected gap is longer than the forecast itself many times over.
     */
    public static final int MAX_MONTHS_AHEAD = 12;

    /** Four-digit year and two-digit month, nothing else, for the reason {@code OccurrenceWindow} gives. */
    private static final Pattern ISO_MONTH = Pattern.compile("\\d{4}-\\d{2}");

    /**
     * Validates {@code from} against today and returns the horizon, or throws one {@code
     * validation-failed} naming the field.
     *
     * <p>Absent means the month after today. Present but blank is a mistake, not a request for the
     * default, because a client that sends {@code from=} meant to send something. The messages never
     * repeat the value sent and never name a date, so the body does not change from one day to the
     * next for the same mistake.
     */
    public static ForecastHorizon of(String fromText, LocalDate today) {
        YearMonth current = YearMonth.from(today);
        YearMonth next = current.plusMonths(1);
        if (fromText == null) {
            return new ForecastHorizon(next, today);
        }

        YearMonth first = parse(fromText);
        if (first == null) {
            throw invalid(EnteredDates.CALENDAR_MONTH);
        }
        if (first.isBefore(next)) {
            throw invalid("must not be before the month after today");
        }
        if (first.isAfter(current.plusMonths(MAX_MONTHS_AHEAD))) {
            throw invalid("must be at most " + MAX_MONTHS_AHEAD + " months after the current month");
        }
        return new ForecastHorizon(first, today);
    }

    /** The first day of the horizon, included. */
    public LocalDate from() {
        return first.atDay(1);
    }

    /** The last day of the horizon, included. */
    public LocalDate to() {
        return last().atEndOfMonth();
    }

    public YearMonth last() {
        return first.plusMonths(LENGTH - 1L);
    }

    /** The months of the horizon, earliest first. */
    public List<YearMonth> months() {
        return List.of(first, first.plusMonths(1), first.plusMonths(2));
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(from()) && !date.isAfter(to());
    }

    /** The first day after today that the forecast does not project, or null when there is none. */
    public LocalDate notProjectedFrom() {
        LocalDate start = asOfDate.plusDays(1);
        return start.isBefore(from()) ? start : null;
    }

    /** The last day before the horizon that the forecast does not project, or null when there is none. */
    public LocalDate notProjectedTo() {
        return notProjectedFrom() == null ? null : from().minusDays(1);
    }

    private static YearMonth parse(String text) {
        if (text.isBlank() || !ISO_MONTH.matcher(text).matches()) {
            return null;
        }
        try {
            return YearMonth.parse(text);
        } catch (DateTimeParseException notACalendarMonth) {
            return null;
        }
    }

    private static ApiException invalid(String message) {
        return new ApiException(
                ProblemType.VALIDATION_FAILED,
                ProblemType.VALIDATION_FAILED.safeDetail(),
                Map.of("errors", List.of(Map.of("field", "from", "message", message))));
    }
}
