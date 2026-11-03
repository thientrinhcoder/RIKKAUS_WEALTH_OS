package com.rikkaus.wealth.identity.api;

import com.rikkaus.wealth.identity.domain.UserAccount;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * The personal profile, as the API describes it.
 *
 * <p>A separate record from {@link AuthenticatedUserResponse} even though the shape is identical
 * today. That one answers "is this session still live and whose is it"; this one is a profile
 * resource that the user edits. Sharing a record would couple two contracts that are free to diverge
 * — the profile may grow a field the session probe must not carry.
 *
 * <p>{@code displayName} is the effective name, so a user who renamed themselves sees their own name
 * and not the one Google reports. The Google subject and the timestamps stay out of the response by
 * construction.
 */
@Schema(description = "The signed-in user's personal profile.")
public record ProfileResponse(
        @Schema(example = "0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93") UUID id,
        @Schema(
                        description = "From the verified Google identity token. Read-only here.",
                        example = "an.nguyen@example.com")
                String email,
        @Schema(
                        description = "The name the user chose, or the Google name if they never chose one.",
                        example = "An Nguyễn")
                String displayName) {

    static ProfileResponse of(UserAccount user) {
        return new ProfileResponse(user.getId(), user.getEmail(), user.effectiveDisplayName());
    }
}
