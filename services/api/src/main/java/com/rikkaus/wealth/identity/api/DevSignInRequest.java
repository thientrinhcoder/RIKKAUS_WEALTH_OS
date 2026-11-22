package com.rikkaus.wealth.identity.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Who to sign in as through the {@code local}-profile development sign-in. */
public record DevSignInRequest(
        @NotBlank(message = "must not be blank") @Email(message = "must be an email address") String email,
        @Size(max = 200, message = "must be at most 200 characters") String displayName) {}
