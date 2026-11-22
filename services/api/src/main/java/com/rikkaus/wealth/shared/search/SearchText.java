package com.rikkaus.wealth.shared.search;

import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Predicate;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The free-text {@code q} parameter every list route accepts: how it is read, and how it matches.
 *
 * <p>A match is a substring match that ignores letter case and Vietnamese diacritics, so {@code thao
 * dien}, {@code THẢO ĐIỀN} and {@code Thảo Điền} all find {@code Căn hộ Thảo Điền}. Pilot users type on
 * phones, often without tone marks, and a search that demanded the exact marks would find nothing for
 * most of what they type.
 *
 * <p>The folding is PostgreSQL's {@code translate}, applied to the column and to the pattern alike,
 * with a table built here from the Vietnamese alphabet. Not the {@code unaccent} extension: that needs
 * {@code CREATE EXTENSION} on whatever host runs the database, is not immutable, and maps a wider set of
 * letters than the product has asked for. {@code translate} is a core function, so the query runs on
 * any PostgreSQL the application already runs on. The table covers each letter in its precomposed form
 * and, by deleting the eight combining marks Vietnamese uses, in its decomposed form too, so a name an
 * input method stored as NFD still matches a query typed as NFC. Letters outside the Vietnamese
 * alphabet ({@code ñ}, {@code ü}) are not folded and match only as typed.
 *
 * <p>The text the user typed reaches the database as a bound parameter, never as part of the SQL
 * text. {@code %}, {@code _} and the
 * escape character are escaped first, so {@code q=%} finds the records that contain a percent sign
 * rather than every record.
 */
public final class SearchText {

    /** The parameter's name, which is also the field a rejected value is reported under. */
    public static final String PARAMETER = "q";

    /** The longest search, in characters. The same bound as a record's name. */
    public static final int MAX_LENGTH = 120;

    /** What every list route's {@code q} documentation says about matching, after its field list. */
    public static final String MATCHING_RULES =
            " Substring match that ignores letter case and Vietnamese diacritics, so \"thao dien\" finds "
                    + "\"Căn hộ Thảo Điền\". Surrounding spaces are ignored and an empty value means no "
                    + "search. % and _ are literal characters, not wildcards. At most 120 characters; a "
                    + "longer value or one containing U+0000 is rejected as validation-failed naming q.";

    private static final char ESCAPE = '\\';

    /** The eight combining marks Vietnamese uses: five tones, circumflex, breve and horn. */
    private static final String COMBINING_MARKS = "̛̣̀́̃̉̂̆";

    private static final String FOLD_FROM;
    private static final String FOLD_TO;

    static {
        String[] bases = {"a", "ă", "â", "e", "ê", "i", "o", "ô", "ơ", "u", "ư", "y"};
        String plain = "aaaeeiooouuy";
        String[] tones = {"", "̀", "́", "̉", "̃", "̣"};
        StringBuilder from = new StringBuilder();
        StringBuilder to = new StringBuilder();
        for (int i = 0; i < bases.length; i++) {
            for (String tone : tones) {
                String letter = Normalizer.normalize(bases[i] + tone, Normalizer.Form.NFC);
                if (letter.length() != 1) {
                    throw new IllegalStateException("A Vietnamese letter has no precomposed form");
                }
                if (letter.charAt(0) < 0x80) {
                    continue;
                }
                from.append(letter).append(letter.toUpperCase(Locale.ROOT));
                to.append(plain.charAt(i)).append(plain.charAt(i));
            }
        }
        from.append("đĐ");
        to.append("dd");
        // Characters of FOLD_FROM with no counterpart in FOLD_TO are deleted by translate, which is
        // what turns a decomposed letter into its base.
        from.append(COMBINING_MARKS);
        FOLD_FROM = from.toString();
        FOLD_TO = to.toString();
    }

    private SearchText() {}

    /**
     * The search the caller asked for, or {@code null} when there is none.
     *
     * <p>Surrounding white space is removed, and a value with nothing left is no search at all: a
     * cleared search box sends {@code q=}, and that has to show the whole list again. A rejected value
     * adds an entry to {@code errors} and returns {@code null}; the message never repeats the value.
     */
    public static String parse(String raw, List<Map<String, String>> errors) {
        if (raw == null) {
            return null;
        }
        if (raw.indexOf('\u0000') >= 0) {
            errors.add(QueryParameters.error(PARAMETER, "must not contain a null character"));
            return null;
        }
        String stripped = raw.strip();
        if (stripped.isEmpty()) {
            return null;
        }
        if (stripped.codePointCount(0, stripped.length()) > MAX_LENGTH) {
            errors.add(
                    QueryParameters.error(PARAMETER, "must be at most " + MAX_LENGTH + " characters"));
            return null;
        }
        return stripped;
    }

    /**
     * A predicate that holds when any of {@code columns} contains {@code text}, folded as the class
     * describes, together with the parameter the caller must bind before running the query.
     *
     * <p>The pattern is a {@link ParameterExpression}, not {@code cb.literal}. Hibernate renders an
     * explicit literal into the SQL text — escaped, but still part of the statement — which was seen
     * in the statement log while this was written: {@code like lower(translate('%thao dien%', ...))}.
     * A parameter keeps what the user typed out of the SQL entirely. The two folding tables stay
     * literals, because they are constants of this class, not input.
     */
    public static TextMatch matchesAny(
            CriteriaBuilder cb, String text, List<Expression<String>> columns) {
        ParameterExpression<String> pattern = cb.parameter(String.class, "searchPattern");
        Expression<String> foldedPattern = fold(cb, pattern);
        Predicate predicate =
                cb.or(
                        columns.stream()
                                .map(column -> cb.like(fold(cb, column), foldedPattern, ESCAPE))
                                .toArray(Predicate[]::new));
        return new TextMatch(predicate, pattern, "%" + escape(text) + "%");
    }

    /**
     * A search predicate and the bound value it needs.
     *
     * @param predicate the condition to add to the query's restrictions
     * @param parameter the pattern's parameter
     * @param pattern the escaped {@code LIKE} pattern, with the user's text between two {@code %}
     */
    public record TextMatch(
            Predicate predicate, ParameterExpression<String> parameter, String pattern) {

        /** Binds the pattern on the query built from the criteria this predicate belongs to. */
        public <T> TypedQuery<T> bindTo(TypedQuery<T> query) {
            return query.setParameter(parameter, pattern);
        }
    }

    private static Expression<String> fold(CriteriaBuilder cb, Expression<String> value) {
        return cb.lower(
                cb.function(
                        "translate", String.class, value, cb.literal(FOLD_FROM), cb.literal(FOLD_TO)));
    }

    private static String escape(String text) {
        StringBuilder escaped = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            if (c == ESCAPE || c == '%' || c == '_') {
                escaped.append(ESCAPE);
            }
            escaped.append(c);
        }
        return escaped.toString();
    }
}
