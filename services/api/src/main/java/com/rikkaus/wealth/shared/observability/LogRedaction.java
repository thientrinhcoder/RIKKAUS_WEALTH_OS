package com.rikkaus.wealth.shared.observability;

import java.io.IOException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The two rules every log output in this application applies, whatever its format.
 *
 * <p><strong>Exceptions are printed without their messages.</strong> An exception message is the one
 * place where data the code never chose to log reaches a log line anyway: a JSON parse error quotes
 * the body it failed on, a constraint violation quotes the row, a conversion failure quotes the value.
 * Only the class name of each exception in the cause chain and its stack frames are printed. That is
 * enough to find the failing line from a correlation identifier, and it holds no user data by
 * construction rather than by pattern-matching.
 *
 * <p><strong>Credential- and email-shaped strings are masked</strong> in every message as a safety net
 * for code that logs something it should not. This is pattern-based and so only catches what has a
 * recognisable shape: a JWT, a Google OAuth token, a {@code Bearer} credential, an email address.
 * Amounts and the names of assets, liabilities or goals have no such shape; they are kept out of the
 * log at the source — no body, header or query string is ever logged, and exception messages are
 * dropped above — and this class does not pretend otherwise.
 *
 * <p>Used by both outputs: {@link RedactedMessageConverter} and {@link MessageFreeThrowableConverter} for
 * the readable {@code local} console pattern, {@link RedactingJsonMembersCustomizer} and {@link
 * MessageFreeStackTracePrinter} for the JSON written everywhere else.
 */
public final class LogRedaction {

    static final String REDACTED = "[redacted]";

    private static final Pattern SENSITIVE =
            Pattern.compile(
                    String.join(
                            "|",
                            // An Authorization header value, wherever it was copied.
                            "(?i:bearer)\\s+[A-Za-z0-9._~+/=-]+",
                            // A JWT: this API's own access tokens and Google's ID tokens alike.
                            "eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]*",
                            // A Google OAuth access token and a Google refresh token.
                            "ya29\\.[A-Za-z0-9._-]+",
                            "1//[A-Za-z0-9_-]{10,}",
                            // An email address. The users table holds one per user.
                            "[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}"));

    /** Causes printed per exception; the rest are summarised. Bounds a pathological chain. */
    private static final int MAX_CAUSES = 8;

    /** Frames printed per exception. The failing frame is at the top; the tail is the container. */
    private static final int MAX_FRAMES = 40;

    /** The value with every credential- or email-shaped substring replaced by {@value #REDACTED}. */
    public static String redact(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return SENSITIVE.matcher(value).replaceAll(REDACTED);
    }

    /**
     * Prints the cause chain as class names and stack frames only, in the layout of {@link
     * Throwable#printStackTrace()} so existing tooling still recognises it, but with no message.
     */
    public static void appendMessageFree(Throwable throwable, Appendable out) throws IOException {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = throwable;
        String prefix = "";
        int depth = 0;
        while (current != null && seen.add(current)) {
            if (depth == MAX_CAUSES) {
                out.append("\t... further causes omitted").append(System.lineSeparator());
                return;
            }
            out.append(prefix).append(current.getClass().getName()).append(System.lineSeparator());
            StackTraceElement[] frames = current.getStackTrace();
            int shown = Math.min(frames.length, MAX_FRAMES);
            for (int i = 0; i < shown; i++) {
                out.append("\tat ").append(frames[i].toString()).append(System.lineSeparator());
            }
            if (frames.length > shown) {
                out.append("\t... ")
                        .append(String.valueOf(frames.length - shown))
                        .append(" more")
                        .append(System.lineSeparator());
            }
            current = current.getCause();
            prefix = "Caused by: ";
            depth++;
        }
    }

    private LogRedaction() {}
}
