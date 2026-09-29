package com.rikkaus.wealth.identity.api;

import com.rikkaus.wealth.identity.application.IdentityService.AuthenticatedSession;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A newly established or renewed session.
 *
 * <p>The refresh token appears in this body and nowhere else: it is not stored server-side in recoverable
 * form and is never logged. The access token's remaining life is published as a duration in seconds rather
 * than an absolute timestamp, so a client with a skewed clock still schedules its renewal correctly.
 */
@Schema(description = "A signed-in session: an access token, its lifetime, and the refresh token.")
public record SessionResponse(
        @Schema(
                        description = "Signed JWT to send as `Authorization: Bearer`.",
                        example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIwZjJjOGQxNCJ9.placeholder-signature")
                String accessToken,
        @Schema(example = "Bearer") String tokenType,
        @Schema(description = "Seconds until the access token expires.", example = "900")
                long expiresInSeconds,
        @Schema(
                        description = "Single-use token for renewing the session. Rotated on every renewal.",
                        example = "1mQ7rVx0Yb3TkPzFhLdW8sNcJgA6eRuq2ZoXiB4ySvE")
                String refreshToken,
        AuthenticatedUserResponse user) {

    static SessionResponse of(AuthenticatedSession session) {
        return new SessionResponse(
                session.session().accessToken(),
                "Bearer",
                session.session().accessTokenTtl().toSeconds(),
                session.session().refreshToken(),
                AuthenticatedUserResponse.of(session.user()));
    }
}
