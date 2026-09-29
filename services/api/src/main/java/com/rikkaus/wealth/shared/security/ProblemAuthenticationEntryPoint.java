package com.rikkaus.wealth.shared.security;

import com.rikkaus.wealth.shared.error.ProblemDetailWriter;
import com.rikkaus.wealth.shared.error.ProblemType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Renders a missing or invalid credential as {@code urn:rikkaus:problem:unauthorized}.
 *
 * <p>Written through {@link ProblemDetailWriter} rather than by throwing, because Spring Security's
 * handlers run inside the filter chain and an exception raised there never reaches
 * {@code ApiExceptionHandler} — the exact upstream gap that writer exists for. Reusing it is what keeps
 * the {@code correlationId} member, the {@code application/problem+json} content type and the body shape
 * identical to every other error on the service.
 *
 * <p>No {@code WWW-Authenticate} header is sent. The browser's response to one on an XHR is to do
 * nothing useful, and on a same-origin document it can trigger a native credential dialog that has no
 * meaning for a Google-only sign-in. The client distinguishes states from the {@code type} URN.
 */
class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ProblemDetailWriter problemDetailWriter;

    ProblemAuthenticationEntryPoint(ProblemDetailWriter problemDetailWriter) {
        this.problemDetailWriter = problemDetailWriter;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) {
        // The exception message is deliberately unused: it can name the token or the reason it failed,
        // and the client is told only that authentication is required.
        problemDetailWriter.write(
                response, ProblemType.UNAUTHORIZED, ProblemType.UNAUTHORIZED.safeDetail());
    }
}
