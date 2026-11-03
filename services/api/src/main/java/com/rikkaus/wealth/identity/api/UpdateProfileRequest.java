package com.rikkaus.wealth.identity.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A change to the personal profile.
 *
 * <p>The display name is the only editable field, per the accepted design for issue #48. The email
 * comes from the verified Google identity token and accepting it here would offer an edit the server
 * would have to ignore.
 *
 * <p>The 120-character bound exists because {@code display_name} is {@code TEXT} and has no limit of
 * its own, so without it the API would accept a megabyte of text into a field the design renders on
 * one line. {@code NotBlank} rather than {@code NotNull}: a name of spaces is not a name.
 */
@Schema(description = "Change the display name. The only editable profile field.")
public record UpdateProfileRequest(
        @Schema(
                        description = "The name to show. Trimmed before storing.",
                        example = "An Nguyễn",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotBlank
                @Size(max = 120)
                String displayName) {}
