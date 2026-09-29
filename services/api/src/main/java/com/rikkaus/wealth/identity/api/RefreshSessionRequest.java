package com.rikkaus.wealth.identity.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Carries a refresh token, for renewing or ending a session. */
@Schema(description = "Carries the refresh token identifying the session to renew or end.")
public record RefreshSessionRequest(
        @Schema(
                        description = "The refresh token returned by the most recent sign-in or renewal.",
                        example = "1mQ7rVx0Yb3TkPzFhLdW8sNcJgA6eRuq2ZoXiB4ySvE")
                @NotBlank(message = "must not be blank")
                String refreshToken) {}
