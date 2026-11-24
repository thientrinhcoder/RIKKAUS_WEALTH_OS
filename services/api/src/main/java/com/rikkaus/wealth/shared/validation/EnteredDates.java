package com.rikkaus.wealth.shared.validation;

import com.rikkaus.wealth.shared.time.TimeConventions;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * The calendar a user-entered date belongs to: which day "today" is, and which text is a date at all.
 *
 * <p><strong>Today is a Vietnamese civil date.</strong> The {@link Clock} bean is UTC and a pilot user
 * in Vietnam is seven hours ahead of it. Judging "is this date in the future" in UTC would refuse a user
 * who records today's valuation at nine in the evening, because UTC still calls it yesterday. Every
 * slice that compares an entered date with today reads it here, so one calendar day cannot be valid on
 * one resource and refused on another.
 *
 * <p><strong>A date is exactly {@code YYYY-MM-DD}</strong>, a four-digit year from {@code 0001}, a real
 * month and a real day. That is the grammar the query parameters already enforce, and it is what keeps
 * a JSON body from reaching PostgreSQL with a date it cannot hold or will not hand back unchanged:
 * {@code -5000-01-01} is outside PostgreSQL's range and failed as a 500, and {@code 0000-01-31} is
 * stored as {@code 0001-01-31 BC} and read back by the driver as {@code 0001-01-31}, a different day.
 */
public final class EnteredDates {

    /** The zone an entered date is judged in. */
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public static final String CALENDAR_DATE = "must be a calendar date in the form YYYY-MM-DD";
    public static final String CALENDAR_MONTH = "must be a calendar month in the form YYYY-MM";
    public static final String NOT_IN_FUTURE = "must not be in the future";

    /** Four-digit year, two-digit month and day. {@code LocalDate.parse} alone accepts signed years. */
    private static final Pattern ISO_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    private static final LocalDate EARLIEST = LocalDate.of(1, 1, 1);

    private EnteredDates() {}

    /** Today, as a Vietnamese civil date. */
    public static LocalDate today(Clock clock) {
        return TimeConventions.today(clock, ZONE);
    }

    /**
     * The date {@code text} names, or {@code null} when it is not a calendar date in the form {@code
     * YYYY-MM-DD} from {@code 0001-01-01} on. The value is never echoed by any caller.
     */
    public static LocalDate parse(String text) {
        if (text == null || !ISO_DATE.matcher(text).matches()) {
            return null;
        }
        try {
            // ISO_LOCAL_DATE resolves strictly, so 2026-02-30 is refused rather than moved.
            LocalDate date = LocalDate.parse(text);
            return date.isBefore(EARLIEST) ? null : date;
        } catch (DateTimeParseException notACalendarDate) {
            return null;
        }
    }
}
