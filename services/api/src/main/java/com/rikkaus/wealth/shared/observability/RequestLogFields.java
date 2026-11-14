package com.rikkaus.wealth.shared.observability;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.slf4j.MDC;

/**
 * The names of every field a log line may carry about a request, and the one way to attach the user.
 *
 * <p>This class is the allowlist. A request log line carries the correlation identifier, the user's
 * UUID, the HTTP method, the route <em>template</em>, the status and the duration — and nothing else.
 * No header value, no query string, no body, no email, no amount and no record name is ever given a
 * field, because there is no field to give it.
 *
 * <p>The user's identifier lives in two places because the security filter that learns it clears its
 * own context when it returns, before the outermost filter writes the request line. MDC carries it to
 * every line logged while the request is being served; the request attribute carries it back out to
 * {@link RequestLoggingFilter}.
 */
public final class RequestLogFields {

    public static final String USER_ID = "userId";
    public static final String HTTP_METHOD = "httpMethod";
    public static final String HTTP_ROUTE = "httpRoute";
    public static final String HTTP_STATUS = "httpStatus";
    public static final String DURATION_MS = "durationMs";

    static final String USER_ID_ATTRIBUTE = RequestLogFields.class.getName() + ".userId";

    /** Called once the access token is verified. The UUID only; never an email or a display name. */
    public static void bindUser(HttpServletRequest request, UUID userId) {
        request.setAttribute(USER_ID_ATTRIBUTE, userId.toString());
        MDC.put(USER_ID, userId.toString());
    }

    /** Called by whoever bound the user, in a {@code finally}, because Tomcat reuses threads. */
    public static void unbindUser() {
        MDC.remove(USER_ID);
    }

    static String boundUser(HttpServletRequest request) {
        return (String) request.getAttribute(USER_ID_ATTRIBUTE);
    }

    private RequestLogFields() {}
}
