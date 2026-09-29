package com.rikkaus.wealth.identity.api;

import com.rikkaus.wealth.identity.domain.UserAccount;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * The user, as the API describes them.
 *
 * <p>A separate record rather than serializing {@link UserAccount} directly, so the Google subject and
 * the timestamps stay out of every response by construction. Adding a column to the entity cannot widen
 * the API.
 */
@Schema(description = "The signed-in user.")
public record AuthenticatedUserResponse(
        @Schema(example = "0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93") UUID id,
        @Schema(example = "an.nguyen@example.com") String email,
        @Schema(example = "An Nguyễn") String displayName) {

    static AuthenticatedUserResponse of(UserAccount user) {
        return new AuthenticatedUserResponse(user.getId(), user.getEmail(), user.getDisplayName());
    }
}
