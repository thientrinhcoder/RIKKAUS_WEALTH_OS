package com.rikkaus.wealth.shared.security;

import com.rikkaus.wealth.shared.error.ApiException;
import com.rikkaus.wealth.shared.error.ProblemType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The single way to learn who is making the current request.
 *
 * <p>Its existence is the mechanism behind the decision-record rule that the server must never trust a
 * client-supplied {@code userId}. A feature slice that needs the viewer's identity asks here and gets it
 * from the verified access token; there is no supported path that reads it from a path variable, a query
 * parameter or a request body, so "forgot to check" cannot look like ordinary code.
 *
 * <p>{@link #require()} throws {@code unauthorized} rather than returning null, so a slice that uses it
 * outside an authenticated request fails as a 401 instead of a {@code NullPointerException} rendered as a
 * 500.
 */
public final class CurrentUser {

    private CurrentUser() {}

    public static UUID require() {
        return find()
                .orElseThrow(
                        () -> new ApiException(ProblemType.UNAUTHORIZED, ProblemType.UNAUTHORIZED.safeDetail()));
    }

    public static Optional<UUID> find() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        if (authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return Optional.of(user.userId());
        }
        return Optional.empty();
    }

    /**
     * The principal a verified access token produces.
     *
     * <p>Carries the session id as well as the user id so a slice can attribute an action to one
     * rotation chain without a database lookup.
     */
    public record AuthenticatedUser(UUID userId, UUID sessionId) {}
}
