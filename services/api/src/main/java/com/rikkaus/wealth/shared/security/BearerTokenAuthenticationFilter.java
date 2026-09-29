package com.rikkaus.wealth.shared.security;

import com.rikkaus.wealth.shared.error.ProblemDetailWriter;
import com.rikkaus.wealth.shared.error.ProblemType;
import com.rikkaus.wealth.shared.security.AccessTokenVerifier.InvalidAccessTokenException;
import com.rikkaus.wealth.shared.security.AccessTokenVerifier.VerifiedAccessToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Turns a valid {@code Authorization: Bearer} header into an authenticated security context.
 *
 * <p>An <em>absent</em> header is left alone: the request continues unauthenticated and the authorization
 * rules decide, which is what lets the permitted routes stay reachable. A <em>present but invalid</em>
 * header is rejected here and now, rather than being treated as anonymous. The difference matters: a
 * silently-ignored bad token turns clock skew, a rotated signing key or a truncated header into a
 * confusing 401-on-a-public-route or an unexpected 200, instead of one clear rejection at the point of
 * failure.
 *
 * <p>Not a {@code @Component}, and that is load-bearing. {@code WebMvcTypeExcludeFilter} deliberately
 * includes {@code Filter} beans in a {@code @WebMvcTest} slice, so a scanned security filter would be
 * instantiated in every slice test and fail the context looking for an {@link AccessTokenVerifier} that
 * the slice does not load. It is constructed by {@link SecurityConfiguration} instead — the same reason
 * {@code ClockConfiguration} must not implement {@code WebMvcConfigurer}.
 */
class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(BearerTokenAuthenticationFilter.class);

    private static final String BEARER_PREFIX = "Bearer ";

    private final AccessTokenVerifier accessTokens;
    private final ProblemDetailWriter problemDetailWriter;

    BearerTokenAuthenticationFilter(
            AccessTokenVerifier accessTokens, ProblemDetailWriter problemDetailWriter) {
        this.accessTokens = accessTokens;
        this.problemDetailWriter = problemDetailWriter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        VerifiedAccessToken principal;
        try {
            principal = accessTokens.verify(token);
        } catch (InvalidAccessTokenException e) {
            // The reason is logged, never returned. The token itself is not logged: it is a live
            // credential until it expires, and a log is the wrong place for one.
            log.debug("Rejected a bearer token: {}", e.getMessage());
            problemDetailWriter.write(
                    response, ProblemType.UNAUTHORIZED, ProblemType.UNAUTHORIZED.safeDetail());
            return;
        }

        // No authorities. MVP 0 has a single kind of user, and inventing a role now would put an
        // unenforced authorization vocabulary into the codebase that later readers would assume means
        // something. Authorization here is ownership, which OwnershipGuard enforces per record.
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        new CurrentUser.AuthenticatedUser(principal.userId(), principal.sessionId()),
                        null,
                        List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            filterChain.doFilter(request, response);
        } finally {
            // The chain is stateless and Tomcat reuses threads, so a context left behind would leak one
            // request's identity into the next request served by the same thread.
            SecurityContextHolder.clearContext();
        }
    }
}
