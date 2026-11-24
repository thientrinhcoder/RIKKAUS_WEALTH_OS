package com.rikkaus.wealth.shared.validation;

import java.util.List;
import java.util.Map;

/**
 * Free text as the user meant it: surrounding blank space removed, a field left with nothing treated as
 * not answered, and the one character PostgreSQL cannot store refused by name.
 *
 * <p><strong>What counts as blank</strong> is wider than any standard answer, which is why it is
 * written out. {@code trim()} removes only code points at or below {@code U+0020}. {@code strip()} and
 * {@code @NotBlank} use {@code Character.isWhitespace}, which deliberately rejects the non-breaking
 * spaces, {@code U+00A0} among them, because they are not line-breaking opportunities. A Vietnamese or
 * CJK input method emits {@code U+00A0} or {@code U+3000} with nothing visibly unusual about it, and a
 * name made only of them passed {@code @NotBlank} and rendered as an empty row. Blank here is Java
 * whitespace, every Unicode space separator ({@code Zs}), and the three zero-width characters that
 * render as nothing at all: {@code U+200B} zero width space, {@code U+2060} word joiner and {@code
 * U+FEFF} zero width no-break space, which is also what a byte-order mark pasted from a file becomes.
 *
 * <p><strong>{@code U+0000} is refused, not stripped.</strong> PostgreSQL cannot hold it in a {@code
 * text} column, so it failed inside the driver and reached the client as a 500. Dropping it and
 * answering 2xx would store something other than what was sent without saying so.
 *
 * <p>An unpaired surrogate is deliberately accepted: PostgreSQL stores and returns it without
 * complaint, so there is nothing to protect against.
 */
public final class TextInput {

    public static final String BLANK = "must not be blank";
    public static final String NULL_CHARACTER = "must not contain a null character";

    private TextInput() {}

    /**
     * An optional text field: stripped, {@code null} when nothing is left.
     *
     * @throws com.rikkaus.wealth.shared.error.ApiException {@code validation-failed} naming {@code
     *     field} when the value contains {@code U+0000}
     */
    public static String optional(String value, String field) {
        if (containsNullCharacter(value)) {
            throw FieldErrors.validationFailed(field, NULL_CHARACTER);
        }
        return strippedOrNull(value);
    }

    /**
     * An optional text field, reporting a {@code U+0000} into {@code errors} instead of throwing, for a
     * caller that reports every problem of a request in one response. Returns {@code null} when the
     * value was refused.
     */
    public static String optional(String value, String field, List<Map<String, String>> errors) {
        if (containsNullCharacter(value)) {
            errors.add(FieldErrors.error(field, NULL_CHARACTER));
            return null;
        }
        return strippedOrNull(value);
    }

    /**
     * A required text field: stripped, and refused as {@code must not be blank} when absent or when
     * nothing is left.
     *
     * @throws com.rikkaus.wealth.shared.error.ApiException {@code validation-failed} naming {@code field}
     */
    public static String required(String value, String field) {
        String cleaned = optional(value, field);
        if (cleaned == null) {
            throw FieldErrors.validationFailed(field, BLANK);
        }
        return cleaned;
    }

    /** The value without leading and trailing blank space as this class defines it. */
    public static String strip(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && isBlankToAReader(value.codePointAt(start))) {
            start += Character.charCount(value.codePointAt(start));
        }
        while (end > start && isBlankToAReader(value.codePointBefore(end))) {
            end -= Character.charCount(value.codePointBefore(end));
        }
        return value.substring(start, end);
    }

    /** Whether {@code value} contains {@code U+0000}. */
    public static boolean containsNullCharacter(String value) {
        return value != null && value.indexOf('\u0000') >= 0;
    }

    /** Whitespace as Java defines it, every space separator, and the three zero-width blanks. */
    public static boolean isBlankToAReader(int codePoint) {
        return Character.isWhitespace(codePoint)
                || Character.getType(codePoint) == Character.SPACE_SEPARATOR
                || codePoint == '​'
                || codePoint == '⁠'
                || codePoint == '﻿';
    }

    private static String strippedOrNull(String value) {
        if (value == null) {
            return null;
        }
        String stripped = strip(value);
        return stripped.isEmpty() ? null : stripped;
    }
}
