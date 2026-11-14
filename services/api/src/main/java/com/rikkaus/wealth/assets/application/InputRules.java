package com.rikkaus.wealth.assets.application;

import com.rikkaus.wealth.assets.domain.AssetCurrency;
import com.rikkaus.wealth.assets.domain.DeclaredValuation;
import com.rikkaus.wealth.shared.error.ApiException;
import com.rikkaus.wealth.shared.error.ProblemType;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The rules this slice applies to user input that cannot be bean-validation annotations, shared by
 * the two services so the asset and its history are judged by one definition of each.
 *
 * <p>Three kinds of rule live here. The zone a user-entered date is judged in. The cleaning of free
 * text, which has to be written out rather than delegated. And the rules about the exchange-rate
 * snapshot, which apply equally to the valuation an asset is created with and to every one recorded
 * later. The error shape is the {@code errors} extension the identity and settings slices already
 * produce, so a client has one error body to parse.
 */
final class InputRules {

    /**
     * The zone every user-entered date is judged in.
     *
     * <p>The {@link Clock} bean is UTC and a pilot user in Vietnam is seven hours ahead of it. Judging
     * "is this date in the future" in UTC would refuse a user who records today's valuation at nine in
     * the evening, because UTC still calls it yesterday and their date therefore looks like tomorrow.
     * {@code SettingsService} judges the declared exchange-rate date the same way and for the same
     * reason; the two must agree or the same calendar day would be valid on one resource and refused
     * on the other.
     */
    static final ZoneId ENTERED_DATE_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private InputRules() {}

    /** Today, as a Vietnamese civil date. */
    static LocalDate today(Clock clock) {
        return LocalDate.now(clock.withZone(ENTERED_DATE_ZONE));
    }

    /** The name with surrounding blank space removed, refusing one that was nothing else. */
    static String requireName(String name) {
        String value = cleaned(name, "name");
        if (value == null) {
            throw validationFailed("name", "must not be blank");
        }
        return value;
    }

    /**
     * One free-text value, stripped, with a blank one reported as absent, and a null character
     * refused.
     *
     * <p>A value submitted as {@code "   "} is a user who typed nothing into an optional field, not a
     * user whose answer is three spaces. Storing it verbatim would make the field look answered in the
     * catalog. So surrounding blank space is removed and a field left with nothing becomes {@code
     * null}.
     *
     * <p>What counts as blank is wider than any of the three obvious answers, which is why this is
     * written out rather than delegated. {@code trim()} removes only code points at or below
     * {@code U+0020}. {@code strip()} removes what {@code Character.isWhitespace} accepts, and that
     * method deliberately rejects the non-breaking spaces — {@code U+00A0} among them — because they
     * are not line-breaking opportunities. {@code btrim} in the table's check constraint removes the
     * ASCII space alone. A name of a single {@code U+00A0}, which a Vietnamese or CJK input method
     * emits with nothing visibly unusual about it, therefore passes {@code @NotBlank}, survives either
     * standard trim and satisfies the check constraint, and creates an asset that renders as an empty
     * row in the catalog. {@link #isBlankToAReader} is the union: whitespace as Java defines it, plus
     * every Unicode space separator.
     *
     * <p><strong>The null character is the reason this method rejects rather than only cleans.</strong>
     * PostgreSQL cannot store {@code U+0000} in a {@code text} column at all, so a name of
     * {@code "Tiền\u0000mặt"} — which is legal JSON, satisfies {@code @NotBlank} and {@code @Size},
     * and would satisfy every check constraint on the table — fails inside the driver and reaches the
     * client as {@code internal-error} with a stack trace in the log. That contradicts this slice's
     * own stated contract, which is that a client input error is a named {@code validation-failed} and
     * not a 500 from PostgreSQL, and it hands an authenticated caller a one-line way to fill the error
     * log. It is refused rather than silently stripped because the two are not equally honest:
     * dropping a character the client sent and answering 201 would store something other than what
     * was asked for, without saying so.
     *
     * <p>This was found by review of issue #53 and then measured. Before the fix, the trailing-null
     * case answered 201 because {@code trim()} happened to remove it; a null character anywhere else
     * was always a 500. An unpaired surrogate is deliberately not refused: PostgreSQL stores and
     * returns it without complaint, so there is nothing here to protect against.
     */
    static String cleaned(String value, String field) {
        if (value == null) {
            return null;
        }
        if (value.indexOf('\u0000') >= 0) {
            throw validationFailed(field, "must not contain a null character");
        }
        int start = 0;
        int end = value.length();
        while (start < end && isBlankToAReader(value.codePointAt(start))) {
            start += Character.charCount(value.codePointAt(start));
        }
        while (end > start && isBlankToAReader(value.codePointBefore(end))) {
            end -= Character.charCount(value.codePointBefore(end));
        }
        String stripped = value.substring(start, end);
        return stripped.isEmpty() ? null : stripped;
    }

    /** Whitespace as Java defines it, plus the space separators it deliberately excludes. */
    private static boolean isBlankToAReader(int codePoint) {
        return Character.isWhitespace(codePoint)
                || Character.getType(codePoint) == Character.SPACE_SEPARATOR;
    }

    /**
     * What is wrong with the exchange-rate snapshot a declaration carries, if anything.
     *
     * <p>The two rate fields travel together or not at all, because a rate with no date cannot be
     * reconciled later and a date with no rate describes nothing; the table refuses a half-snapshot for
     * the same reason. A VND declaration carries neither, since there is nothing to convert and a rate
     * stored beside a VND value is a figure some later report might apply anyway. A USD declaration may
     * carry neither — that is the accepted design's <em>partial</em> state, and the state every asset
     * created before the history existed is in — but when it carries them, the date is judged like
     * every other user-entered date.
     *
     * <p>Returned as a list rather than thrown, so a caller can report these alongside the date rules
     * in one response instead of making the user fix them one round trip at a time.
     */
    static List<Map<String, String>> rateSnapshotErrors(DeclaredValuation declared, LocalDate today) {
        List<Map<String, String>> errors = new ArrayList<>();
        boolean hasRate = declared.usdToVndRate() != null;
        boolean hasRateDate = declared.rateEffectiveDate() != null;
        if (declared.currency() == AssetCurrency.VND) {
            if (hasRate) {
                errors.add(error("usdToVndRate", "must be absent for a VND valuation"));
            }
            if (hasRateDate) {
                errors.add(error("rateEffectiveDate", "must be absent for a VND valuation"));
            }
            return errors;
        }
        if (hasRate && !hasRateDate) {
            errors.add(error("rateEffectiveDate", "must be given together with usdToVndRate"));
        }
        if (hasRateDate && !hasRate) {
            errors.add(error("usdToVndRate", "must be given together with rateEffectiveDate"));
        }
        if (hasRateDate && declared.rateEffectiveDate().isAfter(today)) {
            errors.add(error("rateEffectiveDate", "must not be in the future"));
        }
        return errors;
    }

    static Map<String, String> error(String field, String message) {
        return Map.of("field", field, "message", message);
    }

    static ApiException validationFailed(String field, String message) {
        return validationFailed(List.of(error(field, message)));
    }

    static ApiException validationFailed(List<Map<String, String>> errors) {
        return new ApiException(
                ProblemType.VALIDATION_FAILED,
                ProblemType.VALIDATION_FAILED.safeDetail(),
                Map.of("errors", List.copyOf(errors)));
    }
}
