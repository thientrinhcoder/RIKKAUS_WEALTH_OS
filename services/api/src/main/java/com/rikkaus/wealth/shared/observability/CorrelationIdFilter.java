package com.rikkaus.wealth.shared.observability;

import com.rikkaus.wealth.shared.error.ProblemDetailWriter;
import com.rikkaus.wealth.shared.error.ProblemType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Publishes one correlation identifier per request to the log context and the response header.
 *
 * <p>It runs at the highest precedence, because an identifier that starts after another filter has
 * already logged is useless for the lines it missed. Two consequences follow, and the placement of the
 * {@code try} boundaries below is the whole design.
 *
 * <p><strong>{@code chain.doFilter} carries no catch block.</strong> Running outermost means everything
 * downstream propagates through this frame — the entire Problem Details taxonomy, and every future
 * Spring Security exception, since this filter sits outside that chain. Catching there would flatten a
 * 404 or a 405 into a 500, and would later report authentication denials as server faults. Only the
 * filter's own work is wrapped, because nothing downstream has run at that point and no MVC handler
 * will ever format it.
 *
 * <p>Note for the authentication task: keep {@code ExceptionTranslationFilter} inside this filter, and
 * add a test asserting a 401 and a 403 are not rewritten.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    private final ProblemDetailWriter problemDetailWriter;

    public CorrelationIdFilter(ProblemDetailWriter problemDetailWriter) {
        this.problemDetailWriter = problemDetailWriter;
    }

    /**
     * {@code OncePerRequestFilter} defaults this to {@code true}, which is what would drop the
     * identifier from bodies rendered on the container's ERROR dispatch — exactly the errors a user
     * quotes in a bug report. The advice sets the {@code correlationId} member only when MDC holds a
     * value, so the member would be silently absent and read as valid RFC 9457 rather than as a broken
     * criterion.
     */
    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            resolveAndPublish(request, response);
        } catch (RuntimeException e) {
            logger.error("Correlation filter failed before dispatch", e);
            problemDetailWriter.write(
                    response, ProblemType.INTERNAL_ERROR, "The request could not be processed.");
            return;
        }
        try {
            chain.doFilter(request, response);
        } finally {
            // Tomcat pools request-handling threads, so a leaked entry does not merely go stale: it
            // attaches this request's identifier to an unrelated later request on the same thread,
            // corrupting the very correlation this filter exists to provide.
            MDC.remove(CorrelationId.MDC_KEY);
        }
    }

    private void resolveAndPublish(HttpServletRequest request, HttpServletResponse response) {
        // The attribute is read before the header so an ERROR dispatch reuses the identifier from the
        // original dispatch instead of minting a second one for the same request.
        String existing = (String) request.getAttribute(CorrelationId.REQUEST_ATTRIBUTE);
        String correlationId =
                existing != null
                        ? existing
                        : CorrelationId.resolve(request.getHeader(CorrelationId.HEADER));
        request.setAttribute(CorrelationId.REQUEST_ATTRIBUTE, correlationId);
        MDC.put(CorrelationId.MDC_KEY, correlationId);
        // Set before the chain runs: the response commits as soon as the handler starts writing, and
        // headers can no longer be added after that.
        response.setHeader(CorrelationId.HEADER, correlationId);
    }
}
