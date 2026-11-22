package com.rikkaus.wealth.identity.api;

import com.rikkaus.wealth.identity.application.LocalDevSignInService;
import com.rikkaus.wealth.shared.api.ApiPaths;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /api/v1/auth/dev-sign-in}: a real session for a seeded user, without Google.
 *
 * <p>Registered under the {@code local} profile only, and {@link Hidden} so it never appears in any
 * OpenAPI document — the committed contract is generated without that profile, and the local live
 * document should not advertise it either. It answers exactly like {@code /auth/google}, including the
 * web-mode cookie, so a client tested against it exercises the real session handling. The security
 * chain permits this path only when the {@code local} profile is active; elsewhere it is an unknown
 * path and denied by default. How to use it is in {@code docs/manual-test-guide-auth.md}.
 */
@Hidden
@Profile("local")
@RestController
@RequestMapping(ApiPaths.V1 + "/auth")
class LocalDevSignInController {

    private final LocalDevSignInService devSignIn;
    private final WebSessionTransport transport;

    LocalDevSignInController(LocalDevSignInService devSignIn, WebSessionTransport transport) {
        this.devSignIn = devSignIn;
        this.transport = transport;
    }

    @PostMapping("/dev-sign-in")
    ResponseEntity<SessionResponse> signIn(
            @RequestHeader(name = WebSessionTransport.CLIENT_HEADER, required = false) String client,
            @RequestHeader(name = HttpHeaders.ORIGIN, required = false) String origin,
            @Valid @RequestBody DevSignInRequest request) {
        boolean web = transport.isWebClient(client, origin);
        return transport.sessionResponse(devSignIn.signIn(request.email(), request.displayName()), web);
    }
}
