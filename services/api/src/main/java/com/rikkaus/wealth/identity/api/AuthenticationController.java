package com.rikkaus.wealth.identity.api;

import com.rikkaus.wealth.identity.application.IdentityService;
import com.rikkaus.wealth.shared.api.ApiPaths;
import com.rikkaus.wealth.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The session lifecycle: sign in with Google, renew, sign out.
 *
 * <p>Renewal and sign-out are deliberately unauthenticated. Their credential is the refresh token — in the
 * body for a native client, in the HttpOnly {@code rikkaus_refresh} cookie for the web client — and
 * requiring a live access token would make it impossible to renew an expired session — which is
 * precisely the state the accepted identity design expects a client to recover from.
 *
 * <p>There is no registration route. First-login account creation happens inside the Google sign-in path,
 * because a separate route would be a second way to create an account and the product has exactly one.
 * (The {@code local}-profile-only {@code dev-sign-in} route is a development tool that is never
 * registered outside that profile; see {@code LocalDevSignInController}.)
 *
 * <p>How the token travels, and the CSRF controls on the cookie, are described on {@link
 * WebSessionTransport}.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/auth")
class AuthenticationController {

    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;
    private static final String PROBLEM_SCHEMA_REF = "#/components/schemas/ProblemDetail";

    private static final String CLIENT_HEADER_DESCRIPTION =
            "Send `web` from the browser client. The refresh token is then set as the HttpOnly, "
                    + "SameSite=Strict cookie `rikkaus_refresh` (Path=/api/v1/auth) and omitted from the body. "
                    + "Omit the header from a native client to keep the token in the body.";

    private final IdentityService identityService;
    private final WebSessionTransport transport;

    AuthenticationController(IdentityService identityService, WebSessionTransport transport) {
        this.identityService = identityService;
        this.transport = transport;
    }

    @Operation(
            summary = "Complete a Google sign-in and start a session",
            description =
                    "Exchanges an authorization code with Google using PKCE, creates the account on first "
                            + "sign-in, and returns an access token with a rotating refresh token. Google is the "
                            + "only identity provider; there is no password.")
    @ApiResponse(
            responseCode = "200",
            description = "The session was established",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SessionResponse.class)))
    @ApiResponse(
            responseCode = "400",
            description = "A field is missing, or the redirect URI is not allowlisted",
            content =
                    @Content(
                            mediaType = PROBLEM_JSON,
                            schema = @Schema(ref = PROBLEM_SCHEMA_REF),
                            examples =
                                    @ExampleObject(
                                            name = "redirect URI not allowlisted",
                                            value =
                                                    """
                                                    {
                                                      "type": "urn:rikkaus:problem:validation-failed",
                                                      "title": "Request validation failed",
                                                      "status": 400,
                                                      "detail": "Request validation failed.",
                                                      "instance": "/api/v1/auth/google",
                                                      "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
                                                      "errors": [
                                                        {
                                                          "field": "redirectUri",
                                                          "message": "is not an allowed redirect URI"
                                                        }
                                                      ]
                                                    }
                                                    """)))
    @ApiResponse(
            responseCode = "401",
            description = "Google rejected the code, or the identity could not be verified",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "405",
            description = "Wrong HTTP method",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "415",
            description = "The request body is not JSON",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "403",
            description = "`X-Rikkaus-Client: web` was sent from an origin that is not allowlisted",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @PostMapping("/google")
    ResponseEntity<SessionResponse> signInWithGoogle(
            @Parameter(description = CLIENT_HEADER_DESCRIPTION, example = "web")
                    @RequestHeader(name = WebSessionTransport.CLIENT_HEADER, required = false)
                    String client,
            @Parameter(hidden = true) @RequestHeader(name = HttpHeaders.ORIGIN, required = false) String origin,
            @Valid @RequestBody GoogleSignInRequest request) {
        boolean web = transport.isWebClient(client, origin);
        return transport.sessionResponse(
                identityService.signInWithGoogle(
                        request.authorizationCode(), request.codeVerifier(), request.redirectUri()),
                web);
    }

    @Operation(
            summary = "Renew a session",
            description =
                    "Exchanges a refresh token for a new access token and a new refresh token. The presented "
                            + "token is single-use: presenting it again is treated as evidence that it leaked and "
                            + "revokes the whole session. A native client sends the token in the body. The web "
                            + "client sends no body token, the `rikkaus_refresh` cookie, and `X-Rikkaus-Client: "
                            + "web`; the rotated token comes back as a new cookie.")
    @ApiResponse(
            responseCode = "200",
            description = "The session was renewed",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SessionResponse.class)))
    @ApiResponse(
            responseCode = "400",
            description = "No refresh token in the body or in an honoured cookie",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "401",
            description = "The refresh token is unknown, expired, already used, or revoked",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "405",
            description = "Wrong HTTP method",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "415",
            description = "The request body is not JSON",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "403",
            description =
                    "The refresh cookie was sent without `X-Rikkaus-Client: web`, or from an origin that is not "
                            + "allowlisted. Refused before the token is read, so the session is untouched.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @PostMapping("/refresh")
    ResponseEntity<SessionResponse> renew(
            @Parameter(description = CLIENT_HEADER_DESCRIPTION, example = "web")
                    @RequestHeader(name = WebSessionTransport.CLIENT_HEADER, required = false)
                    String client,
            @Parameter(hidden = true) @RequestHeader(name = HttpHeaders.ORIGIN, required = false) String origin,
            @Parameter(description = "The web client's refresh token, set by a previous sign-in or renewal.")
                    @CookieValue(name = WebSessionTransport.COOKIE_NAME, required = false)
                    String cookieToken,
            @RequestBody(required = false) RefreshSessionRequest request) {
        boolean web = transport.isWebClient(client, origin);
        String token = transport.resolveRefreshToken(bodyToken(request), cookieToken, web);
        return transport.sessionResponse(identityService.renew(token), web);
    }

    @Operation(
            summary = "Sign out",
            description =
                    "Revokes the session the refresh token belongs to. Returns 204 whether or not the token "
                            + "was still live, so the endpoint cannot be used to test whether a token is valid. "
                            + "The token comes from the body, or for the web client from the `rikkaus_refresh` "
                            + "cookie with `X-Rikkaus-Client: web`. Every 204 also expires the cookie.")
    @ApiResponse(responseCode = "204", description = "The session is no longer usable and the cookie is cleared")
    @ApiResponse(
            responseCode = "400",
            description = "No refresh token in the body or in an honoured cookie",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "405",
            description = "Wrong HTTP method",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "415",
            description = "The request body is not JSON",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "403",
            description =
                    "The refresh cookie was sent without `X-Rikkaus-Client: web`, or from an origin that is not "
                            + "allowlisted",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @PostMapping("/logout")
    ResponseEntity<Void> signOut(
            @Parameter(description = CLIENT_HEADER_DESCRIPTION, example = "web")
                    @RequestHeader(name = WebSessionTransport.CLIENT_HEADER, required = false)
                    String client,
            @Parameter(hidden = true) @RequestHeader(name = HttpHeaders.ORIGIN, required = false) String origin,
            @Parameter(description = "The web client's refresh token, set by a previous sign-in or renewal.")
                    @CookieValue(name = WebSessionTransport.COOKIE_NAME, required = false)
                    String cookieToken,
            @RequestBody(required = false) RefreshSessionRequest request) {
        boolean web = transport.isWebClient(client, origin);
        identityService.signOut(transport.resolveRefreshToken(bodyToken(request), cookieToken, web));
        // Cleared unconditionally: a native client has no cookie and ignores the header, and a web client
        // must lose the cookie even if its token had already been revoked.
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, transport.cleared()).build();
    }

    @Operation(
            summary = "Read the signed-in user",
            description =
                    "Confirms the access token is still valid and returns who it belongs to. This is how a "
                            + "client tells a live session from an expired or revoked one without having to "
                            + "infer it from a failed domain request.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "The session is live",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthenticatedUserResponse.class)))
    @ApiResponse(
            responseCode = "401",
            description = "No access token, or it has expired or been revoked",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "405",
            description = "Wrong HTTP method",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "406",
            description = "The requested media type cannot be produced",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @GetMapping("/session")
    AuthenticatedUserResponse currentSession() {
        // The identifier comes from the verified token through CurrentUser, never from the request, so
        // there is no parameter a caller could use to ask about somebody else.
        return AuthenticatedUserResponse.of(identityService.requireUser(CurrentUser.require()));
    }

    private static String bodyToken(RefreshSessionRequest request) {
        return request == null ? null : request.refreshToken();
    }
}
