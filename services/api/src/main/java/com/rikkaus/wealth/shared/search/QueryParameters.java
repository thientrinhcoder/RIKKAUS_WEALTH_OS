package com.rikkaus.wealth.shared.search;

import com.rikkaus.wealth.shared.error.ApiException;
import com.rikkaus.wealth.shared.error.ProblemType;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Reads the filter and sort parameters of a list route from the text Spring bound.
 *
 * <p>Every parameter arrives as a {@code String} or a list of them rather than as an enum or a date,
 * because Spring's own conversion answers a value it cannot convert with {@code malformed-request} and
 * no field name, while every other rejected input in the API is a {@code validation-failed} whose
 * {@code errors} entry names the field. Parsing here keeps that one shape, lets one response report
 * every mistake in a request, and keeps the caller's value out of the message.
 *
 * <p>Each method appends to a caller-owned list instead of throwing, and {@link #rejectIfAny} throws
 * once at the end.
 */
public final class QueryParameters {

    /** What every multi-valued filter's documentation says about repetition and rejection. */
    public static final String MULTI_VALUE_RULES =
            " Repeat the parameter or separate values with commas; a record matching any of them is "
                    + "kept, and different parameters must all match. Values are the exact constant names. "
                    + "An empty value is ignored; any other unknown value is rejected as validation-failed "
                    + "naming this parameter.";

    /** What every {@code sort} parameter's documentation says after its own keys. */
    public static final String SORT_RULES =
            " One key; a leading - sorts descending. Ties are broken by the default order and then by "
                    + "id, so the order is stable between requests. Any other key, or more than one, is "
                    + "rejected as validation-failed naming sort.";

    /** Four-digit year, two-digit month and day. {@code LocalDate.parse} alone accepts more. */
    private static final Pattern ISO_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    private QueryParameters() {}

    /**
     * The constants named by {@code raw}, or an empty set when the filter is absent.
     *
     * <p>Values must be the exact constant name, so {@code cash} is refused rather than guessed. An
     * empty value is skipped, so {@code category=} reads as a filter that was cleared.
     */
    public static <E extends Enum<E>> Set<E> enumValues(
            String field, List<String> raw, Class<E> type, List<Map<String, String>> errors) {
        Set<E> values = EnumSet.noneOf(type);
        if (raw == null) {
            return values;
        }
        for (String value : raw) {
            if (value == null || value.isBlank()) {
                continue;
            }
            E constant = constantNamed(type, value.strip());
            if (constant == null) {
                errors.add(error(field, "must be one of " + names(type.getEnumConstants())));
                return EnumSet.noneOf(type);
            }
            values.add(constant);
        }
        return values;
    }

    /** The date in {@code raw}, {@code null} when absent, or an error when it is not a date. */
    public static LocalDate date(String field, String raw, List<Map<String, String>> errors) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        if (ISO_DATE.matcher(raw).matches()) {
            try {
                // ISO_LOCAL_DATE resolves strictly, so 2026-02-30 is refused rather than moved.
                return LocalDate.parse(raw);
            } catch (DateTimeParseException notACalendarDate) {
                // Reported below with the same message as a malformed value.
            }
        }
        errors.add(error(field, "must be a calendar date in the form YYYY-MM-DD"));
        return null;
    }

    /**
     * Rejects a range whose end is before its start. Only called when both ends parsed, because a
     * request with an unparseable start has one mistake, not two.
     */
    public static void requireOrdered(
            String fromField,
            LocalDate from,
            String toField,
            LocalDate to,
            List<Map<String, String>> errors) {
        if (from != null && to != null && to.isBefore(from)) {
            errors.add(error(toField, "must not be before " + fromField));
        }
    }

    /** The sort named by {@code raw}, {@code defaultSort} when absent, or an error. */
    public static <S extends Enum<S> & SortKey> S sort(
            String raw, Class<S> type, S defaultSort, List<Map<String, String>> errors) {
        if (raw == null || raw.isBlank()) {
            return defaultSort;
        }
        String token = raw.strip();
        for (S key : type.getEnumConstants()) {
            if (key.token().equals(token)) {
                return key;
            }
        }
        errors.add(
                error(
                        "sort",
                        "must be one of "
                                + Arrays.stream(type.getEnumConstants())
                                        .map(SortKey::token)
                                        .collect(Collectors.joining(", "))));
        return defaultSort;
    }

    /** Throws one {@code validation-failed} listing every entry, when there is any. */
    public static void rejectIfAny(List<Map<String, String>> errors) {
        if (!errors.isEmpty()) {
            throw new ApiException(
                    ProblemType.VALIDATION_FAILED,
                    ProblemType.VALIDATION_FAILED.safeDetail(),
                    Map.of("errors", List.copyOf(errors)));
        }
    }

    static Map<String, String> error(String field, String message) {
        return Map.of("field", field, "message", message);
    }

    private static <E extends Enum<E>> E constantNamed(Class<E> type, String name) {
        for (E constant : type.getEnumConstants()) {
            if (constant.name().equals(name)) {
                return constant;
            }
        }
        return null;
    }

    private static String names(Enum<?>[] constants) {
        return Arrays.stream(constants).map(Enum::name).collect(Collectors.joining(", "));
    }
}
