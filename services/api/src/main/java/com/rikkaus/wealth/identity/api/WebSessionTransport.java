package com.rikkaus.wealth.identity.api;

import com.rikkaus.wealth.identity.application.IdentityService.AuthenticatedSession;
import com.rikkaus.wealth.identity.session.SessionProperties;
import com.rikkaus.wealth.shared.api.AllowedOrigins;
import com.rikkaus.wealth.shared.api.ApiPaths;
import com.rikkaus.wealth.shared.error.ApiException;
import com.rikkaus.wealth.shared.error.ProblemType;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * How a refresh token travels between the API and a client: in the JSON body for a native client, in an
 * HttpOnly cookie for a browser.
 *
 * <p><strong>Web mode</strong> is declared by the request header {@code X-Rikkaus-Client: web}. A browser
 * must not hold the refresh token where script can read it, so in web mode the token is set as the
 * {@code rikkaus_refresh} cookie and left out of the body. Without the header the request is native and
 * behaves exactly as before: the token is in the body and no cookie is set.
 *
 * <p><strong>CSRF.</strong> A cookie is attached by the browser on its own, so a cookie-authenticated
 * request is the one shape a forged cross-site request can take. Three controls stop it, each sufficient
 * against a plain cross-site form post:
 *
 * <ol>
 *   <li>The cookie is {@code SameSite=Strict}, so a browser never sends it on a cross-site request.
 *   <li>Using the cookie requires the {@code X-Rikkaus-Client} header. An HTML form cannot set a custom
 *       header, and a cross-origin script that sets one triggers a CORS preflight that only an allowlisted
 *       origin passes.
 *   <li>A web-mode request whose {@code Origin} is present and not on the allowlist is refused outright,
 *       before any token is read, so a refused request never consumes or revokes a session.
 * </ol>
 *
 * <p>A missing {@code Origin} is accepted: browsers send it on every cross-origin and every same-origin
 * {@code POST}, so its absence means a non-browser caller, which carries no ambient cookie to abuse.
 */
@Component
class WebSessionTransport {

    static final String CLIENT_HEADER = "X-Rikkaus-Client";
    static final String WEB_CLIENT = "web";
    static final String COOKIE_NAME = "rikkaus_refresh";
    static final String COOKIE_PATH = ApiPaths.V1 + "/auth";

    private static final Logger log = LoggerFactory.getLogger(WebSessionTransport.class);

    private final List<String> allowedOrigins;
    private final boolean secureCookie;
    private final long maxAgeSeconds;

    WebSessionTransport(
            @Value(AllowedOrigins.PROPERTY) String allowedOrigins,
            // Secure by default. Only the local profile turns it off, because a Secure cookie is not
            // stored from a plain-http response by every client used in local testing.
            @Value("${rikkaus.auth.refresh-cookie.secure:true}") boolean secureCookie,
            SessionProperties sessionProperties) {
        this.allowedOrigins = AllowedOrigins.parse(allowedOrigins);
        this.secureCookie = secureCookie;
        this.maxAgeSeconds = sessionProperties.refreshTokenTtl().toSeconds();
    }

    /**
     * Whether the request is from the web client, refusing a web-mode request from a foreign origin.
     *
     * <p>Only the exact value {@code web} selects web mode, so a typo is a native request rather than a
     * silently different contract.
     */
    boolean isWebClient(String clientHeader, String origin) {
        if (!WEB_CLIENT.equals(clientHeader)) {
            return false;
        }
        if (origin != null && !allowedOrigins.contains(origin)) {
            log.warn("Web session request refused: origin is not on the allowlist");
            throw forbidden();
        }
        return true;
    }

    /**
     * The refresh token to act on: the body's when present, otherwise the cookie's.
     *
     * <p>The body wins so a native client's behaviour cannot change because a stray cookie exists. The
     * cookie is honoured only in web mode, which is the CSRF control described on this class.
     */
    String resolveRefreshToken(String bodyToken, String cookieToken, boolean webClient) {
        if (bodyToken != null && !bodyToken.isBlank()) {
            return bodyToken;
        }
        if (cookieToken != null && !cookieToken.isBlank()) {
            if (!webClient) {
                log.warn("Refresh cookie presented without the web client header; refused as possible CSRF");
                throw forbidden();
            }
            return cookieToken;
        }
        throw new ApiException(
                ProblemType.VALIDATION_FAILED,
                ProblemType.VALIDATION_FAILED.safeDetail(),
                Map.of("errors", List.of(Map.of("field", "refreshToken", "message", "must not be blank"))));
    }

    /** {@code Set-Cookie} carrying a freshly issued refresh token. */
    private String issue(String refreshToken) {
        return cookie(refreshToken, maxAgeSeconds);
    }

    /** {@code Set-Cookie} that deletes the refresh cookie. */
    String cleared() {
        return cookie("", 0);
    }

    /**
     * The HTTP response for a new or renewed session: token in the cookie and out of the body for the
     * web client, in the body and no cookie for a native one.
     */
    ResponseEntity<SessionResponse> sessionResponse(AuthenticatedSession session, boolean webClient) {
        if (!webClient) {
            return ResponseEntity.ok(SessionResponse.of(session, true));
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, issue(session.session().refreshToken()))
                .body(SessionResponse.of(session, false));
    }

    private String cookie(String value, long maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build()
                .toString();
    }

    private static ApiException forbidden() {
        return new ApiException(ProblemType.FORBIDDEN, ProblemType.FORBIDDEN.safeDetail());
    }
}
