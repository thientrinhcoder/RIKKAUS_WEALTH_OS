package com.rikkaus.wealth.identity.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * What the client sends to complete a Google sign-in.
 *
 * <p>The authorization code is exchanged on the server, so the Google client secret never reaches the
 * browser and no Google token is ever held by the client. PKCE's {@code code_verifier} travels with it
 * because the client generated the challenge.
 */
@Schema(description = "Completes a Google sign-in started by the client.")
public record GoogleSignInRequest(
        @Schema(
                        description = "The authorization code Google returned to the client's redirect URI.",
                        example = "4/0AbUR2VPlaceholderAuthorizationCode")
                @NotBlank(message = "must not be blank")
                String authorizationCode,
        @Schema(
                        description = "The PKCE code verifier for the challenge sent when the flow started.",
                        example = "dBjftJeZ4CVPmB92K27uhbUJU1p1r-wW1gFWFOEjXk")
                @NotBlank(message = "must not be blank")
                String codeVerifier,
        @Schema(
                        description =
                                "The redirect URI used to obtain the code. Must appear in the server's allowlist.",
                        // Not a localhost URI. OpenApiContractIT forbids an environment-specific host
                        // anywhere in the published contract, and rightly so: the committed document is
                        // consumed by the frontend and by QA, neither of whose redirect URI is a
                        // developer's own origin.
                        example = "https://app.rikkaus.example/auth/callback")
                @NotBlank(message = "must not be blank")
                String redirectUri) {}
