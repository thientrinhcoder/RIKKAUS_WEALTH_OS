package com.rikkaus.wealth.shared.security;

import com.rikkaus.wealth.shared.error.ProblemDetailWriter;
import com.rikkaus.wealth.shared.error.ProblemType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Renders an authenticated-but-not-permitted request as {@code urn:rikkaus:problem:forbidden}.
 *
 * <p>MVP 0 has one role and one owner per record, so this is currently reachable only through a
 * framework-level denial such as a rejected CSRF or firewall check. It is wired up all the same, because
 * the alternative is Boot's default body appearing on the one path nobody tested.
 *
 * <p><strong>Ownership failures do not come here.</strong> A record belonging to another user returns
 * {@code not-found}, not {@code forbidden}, so the API does not disclose that the record exists — see
 * {@link OwnershipGuard}. This handler covers denials that are not about ownership.
 */
class ProblemAccessDeniedHandler implements AccessDeniedHandler {

    private final ProblemDetailWriter problemDetailWriter;

    ProblemAccessDeniedHandler(ProblemDetailWriter problemDetailWriter) {
        this.problemDetailWriter = problemDetailWriter;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) {
        problemDetailWriter.write(response, ProblemType.FORBIDDEN, ProblemType.FORBIDDEN.safeDetail());
    }
}
