package com.rikkaus.wealth.identity.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Carries a refresh token, for renewing or ending a session.
 *
 * <p>The field is optional because the web client sends its token in the {@code rikkaus_refresh} cookie
 * instead. When neither the body nor an honoured cookie supplies one, the request is a 400 with an
 * {@code errors} entry for {@code refreshToken}, the same answer a blank field always produced; that
 * check lives in {@link WebSessionTransport#resolveRefreshToken} because only there are both sources
 * visible.
 */
@Schema(description = "Carries the refresh token identifying the session to renew or end.")
public record RefreshSessionRequest(
        @Schema(
                        description =
                                "The refresh token returned by the most recent sign-in or renewal. Required for "
                                        + "native clients. The web client omits it (or the whole body) and sends "
                                        + "the `rikkaus_refresh` cookie with `X-Rikkaus-Client: web` instead.",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                        example = "1mQ7rVx0Yb3TkPzFhLdW8sNcJgA6eRuq2ZoXiB4ySvE")
                String refreshToken) {}
