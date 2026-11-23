package com.rikkaus.wealth.identity.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rikkaus.wealth.identity.application.IdentityService.AuthenticatedSession;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A newly established or renewed session.
 *
 * <p>For a native client the refresh token appears in this body and nowhere else. For the web client
 * ({@code X-Rikkaus-Client: web}) it is omitted here and set as the HttpOnly {@code rikkaus_refresh}
 * cookie instead, so script in the page can never read it. Either way it is not stored server-side in
 * recoverable form and is never logged. The access token's remaining life is published as a duration in seconds rather
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
                        description =
                                "Single-use token for renewing the session. Rotated on every renewal. Present "
                                        + "for native clients only; a request sent with `X-Rikkaus-Client: web` "
                                        + "receives it in the HttpOnly `rikkaus_refresh` cookie and this field "
                                        + "is omitted.",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                        example = "1mQ7rVx0Yb3TkPzFhLdW8sNcJgA6eRuq2ZoXiB4ySvE")
                @JsonInclude(JsonInclude.Include.NON_NULL)
                String refreshToken,
        AuthenticatedUserResponse user) {

    static SessionResponse of(AuthenticatedSession session, boolean includeRefreshToken) {
        return new SessionResponse(
                session.session().accessToken(),
                "Bearer",
                session.session().accessTokenTtl().toSeconds(),
                includeRefreshToken ? session.session().refreshToken() : null,
                AuthenticatedUserResponse.of(session.user()));
    }
}
