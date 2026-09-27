package com.rikkaus.wealth.shared.observability;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * The correlation identifier's names and its inbound validation rule.
 *
 * <p>Separated from the filter so the validator can be exercised without constructing a servlet
 * request, and so the error package can reference {@link #MDC_KEY} rather than duplicate a literal.
 *
 * <p>An inbound value is untrusted input reaching two sinks — the log file and the response header —
 * and neither SLF4J nor Logback sanitizes a value put into MDC. A CRLF sequence could forge log
 * entries and an unbounded value could bloat storage, so the rule rejects rather than escapes: there
 * is no legitimate reason for a client to send anything outside the allowed set, and rejection costs
 * the caller nothing because the request still succeeds with a generated identifier.
 */
public final class CorrelationId {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    /**
     * Where the identifier survives after MDC is cleared. A request attribute persists across the
     * container's dispatches, so an error routed to {@code /error} can recover the same value instead
     * of minting a second one for the same request.
     */
    public static final String REQUEST_ATTRIBUTE = CorrelationId.class.getName() + ".id";

    /**
     * ASCII letters, digits and hyphen only, 8 to 64 characters. Rejects carriage return, line feed,
     * tabs, other control characters and spaces. The lower bound rejects trivially short values that
     * would collide across clients and make correlation useless; the upper bound accommodates a UUID
     * with room to spare while capping how much a hostile caller can add to every log line.
     */
    private static final Pattern ALLOWED = Pattern.compile("^[A-Za-z0-9-]{8,64}$");

    /** The inbound value when it is acceptable, otherwise a freshly generated one. */
    public static String resolve(String inbound) {
        return isAcceptable(inbound) ? inbound : UUID.randomUUID().toString();
    }

    public static boolean isAcceptable(String candidate) {
        return candidate != null && ALLOWED.matcher(candidate).matches();
    }

    private CorrelationId() {}
}
