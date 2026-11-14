package com.rikkaus.wealth.shared.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Writes exactly one operational log line per request, after the response status is known.
 *
 * <p>The line is built from an allowlist, {@link RequestLogFields}, and from nothing the client sent
 * verbatim. In particular the path is the Spring MVC route <em>template</em> — {@code
 * /api/v1/assets/{assetId}}, never the identifier in it — and a request that never reached a handler
 * (refused by authentication, or matching no route at all) is logged as {@value #UNMATCHED_ROUTE}
 * rather than with its raw URI, which an anonymous caller fully controls. No header, query string or
 * body is read here, so no {@code Authorization} value, cookie, token, email, amount or record name can
 * reach this line.
 *
 * <p>It runs immediately inside {@code CorrelationIdFilter}, so the correlation identifier is already
 * in MDC, and outside Spring Security, so it also sees the 401s the security chain writes itself. The
 * user identifier comes back out through a request attribute because the security filter clears its
 * own MDC entry before this one resumes.
 *
 * <p>Liveness probes are logged at {@code DEBUG}: the load balancer calls {@code /actuator/health}
 * every few seconds, and an {@code INFO} line for each would bury every real request in CloudWatch.
 *
 * <p>Registered for the {@code REQUEST} dispatch only, so an error forwarded to {@code /error} does not
 * produce a second line for the same request.
 */
public class RequestLoggingFilter extends OncePerRequestFilter {

    static final String UNMATCHED_ROUTE = "unmatched";

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String PROBE_PREFIX = "/actuator/";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long started = System.nanoTime();
        boolean escaped = true;
        try {
            chain.doFilter(request, response);
            escaped = false;
        } finally {
            // An exception that escaped the chain becomes a 500 when the container renders it, after
            // this frame has returned, so the status on the response object is not yet the real one.
            int status = escaped ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR : response.getStatus();
            write(request, status, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
        }
    }

    private static void write(HttpServletRequest request, int status, long durationMs) {
        String method = request.getMethod();
        String route = routeOf(request);
        String userId = RequestLogFields.boundUser(request);
        LoggingEventBuilder line =
                request.getRequestURI().startsWith(PROBE_PREFIX) ? log.atDebug() : log.atInfo();
        if (userId != null) {
            MDC.put(RequestLogFields.USER_ID, userId);
        }
        try {
            line.addKeyValue(RequestLogFields.HTTP_METHOD, method)
                    .addKeyValue(RequestLogFields.HTTP_ROUTE, route)
                    .addKeyValue(RequestLogFields.HTTP_STATUS, status)
                    .addKeyValue(RequestLogFields.DURATION_MS, durationMs)
                    .log("request completed {} {} {} in {} ms", method, route, status, durationMs);
        } finally {
            MDC.remove(RequestLogFields.USER_ID);
        }
    }

    private static String routeOf(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pattern instanceof String template ? template : UNMATCHED_ROUTE;
    }
}
