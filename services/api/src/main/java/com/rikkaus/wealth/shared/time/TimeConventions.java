package com.rikkaus.wealth.shared.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Where time comes from and which zone it is interpreted in.
 *
 * <p>Every accessor takes a {@link Clock} rather than calling {@code Instant.now()}. The application
 * already publishes a {@code Clock} bean, and routing through it is what lets a slice be exercised at
 * a fixed moment instead of at whatever the wall clock says when the suite runs.
 *
 * <p>Every accessor that produces a date takes an explicit {@link ZoneId} and never falls back to the
 * JVM default. That is the actual bug this class prevents: the API runs in UTC, so {@code
 * LocalDate.now()} on the server returns yesterday's date for a user in Ho Chi Minh City for the
 * first seven hours of every local day. A valuation dated that way is off by one for the user, and
 * the error is invisible in any test that runs during European office hours.
 *
 * <p>A static holder rather than an injected service, because there is no state and no dependency to
 * substitute: the two zones are decisions, and the clock arrives as an argument. Making it a bean
 * would add wiring without adding a seam.
 */
public final class TimeConventions {

    /**
     * The zone every stored timestamp is in.
     *
     * <p>UTC, as the architecture decisions require. A single storage zone is what makes two records
     * written in different places orderable at all, and it does not shift under a daylight-saving
     * rule change the way a named civil zone can.
     */
    public static final ZoneId STORAGE_ZONE = ZoneOffset.UTC;

    /**
     * The zone a timestamp is rendered in when the user has not configured one.
     *
     * <p>A default, not a constant to build on. The architecture decisions require display in the
     * user's configured timezone, and that setting is owned by the personal-profile slice; this value
     * is what a reader falls back to before any preference exists. A named zone rather than a fixed
     * {@code +07:00} offset, so a future rule change for Vietnam arrives with the JDK's tz database
     * instead of needing a code change.
     */
    public static final ZoneId DEFAULT_DISPLAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private TimeConventions() {}

    /** The current moment, as it would be stored. */
    public static Instant now(Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        return clock.instant();
    }

    /**
     * The calendar date it currently is in {@code zone}.
     *
     * <p>Use this wherever a business date is defaulted to "today" — a valuation date, a rate's
     * effective date — so the date recorded is the user's day rather than the server's.
     */
    public static LocalDate today(Clock clock, ZoneId zone) {
        Objects.requireNonNull(clock, "clock must not be null");
        Objects.requireNonNull(zone, "zone must not be null");
        return LocalDate.ofInstant(clock.instant(), zone);
    }

    /**
     * A stored moment as it reads in {@code zone}, carrying the offset.
     *
     * <p>The result keeps both the instant and the zone, which is what the audit rule in the accepted
     * design needs: the reader is shown the local absolute time while the UTC source stays
     * recoverable from the same value.
     */
    public static ZonedDateTime inZone(Instant instant, ZoneId zone) {
        Objects.requireNonNull(instant, "instant must not be null");
        Objects.requireNonNull(zone, "zone must not be null");
        return instant.atZone(zone);
    }
}
