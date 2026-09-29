package com.rikkaus.wealth.identity.application;

import com.rikkaus.wealth.identity.domain.UserAccount;
import com.rikkaus.wealth.identity.domain.UserAccountRepository;
import com.rikkaus.wealth.identity.google.GoogleAuthorizationCodeExchange;
import com.rikkaus.wealth.identity.google.GoogleAuthorizationCodeExchange.GoogleExchangeFailedException;
import com.rikkaus.wealth.identity.google.GoogleIdentity;
import com.rikkaus.wealth.identity.google.GoogleIdentityTokenVerifier;
import com.rikkaus.wealth.identity.google.GoogleIdentityTokenVerifier.GoogleIdentityTokenInvalidException;
import com.rikkaus.wealth.identity.google.GoogleOidcProperties;
import com.rikkaus.wealth.identity.session.AccessTokenService;
import com.rikkaus.wealth.identity.session.IssuedSession;
import com.rikkaus.wealth.identity.session.RefreshTokenService;
import com.rikkaus.wealth.identity.session.RefreshTokenService.Issued;
import com.rikkaus.wealth.identity.session.RefreshTokenService.RefreshTokenRejectedException;
import com.rikkaus.wealth.shared.error.ApiException;
import com.rikkaus.wealth.shared.error.ProblemType;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The identity slice's application service: signing in, renewing, signing out, and reading the
 * current user.
 *
 * <p>Every failure on this path becomes {@code urn:rikkaus:problem:unauthorized}, and the {@code detail}
 * is the taxonomy's own fixed text rather than a description of what went wrong. A caller cannot learn
 * whether their code expired, their token was replayed, or their account was never created — all of
 * which would help someone probing. The specific cause is logged server-side, which is where it is
 * useful.
 *
 * <p>Problem {@code detail} text stays English, matching the taxonomy established by the API foundation.
 * The Vietnamese user-facing copy for each of these states is specified by the accepted identity design
 * and is mapped from the {@code type} URN by the client, so the two never drift out of one place.
 */
@Service
public class IdentityService {

    private static final Logger log = LoggerFactory.getLogger(IdentityService.class);

    private final GoogleAuthorizationCodeExchange codeExchange;
    private final GoogleIdentityTokenVerifier tokenVerifier;
    private final GoogleOidcProperties googleProperties;
    private final UserAccountRepository users;
    private final AccessTokenService accessTokens;
    private final RefreshTokenService refreshTokens;
    private final Clock clock;

    IdentityService(
            GoogleAuthorizationCodeExchange codeExchange,
            GoogleIdentityTokenVerifier tokenVerifier,
            GoogleOidcProperties googleProperties,
            UserAccountRepository users,
            AccessTokenService accessTokens,
            RefreshTokenService refreshTokens,
            Clock clock) {
        this.codeExchange = codeExchange;
        this.tokenVerifier = tokenVerifier;
        this.googleProperties = googleProperties;
        this.users = users;
        this.accessTokens = accessTokens;
        this.refreshTokens = refreshTokens;
        this.clock = clock;
    }

    /**
     * Completes a Google sign-in. Creates the account on first login, resumes it afterwards.
     *
     * <p>The redirect URI is checked against the allowlist <em>before</em> the exchange, so a value the
     * operator never registered is never forwarded to Google. This is a 400 rather than a 401 because it
     * is a malformed request from the client, not a failed authentication, and the distinction tells a
     * frontend developer they have a configuration bug rather than a user with a stale code.
     */
    @Transactional
    public AuthenticatedSession signInWithGoogle(
            String authorizationCode, String codeVerifier, String redirectUri) {
        if (!googleProperties.permitsRedirectUri(redirectUri)) {
            log.warn("Sign-in attempted with a redirect URI that is not allowlisted");
            throw new ApiException(
                    ProblemType.VALIDATION_FAILED,
                    ProblemType.VALIDATION_FAILED.safeDetail(),
                    Map.of(
                            "errors",
                            java.util.List.of(
                                    Map.of("field", "redirectUri", "message", "is not an allowed redirect URI"))));
        }

        GoogleIdentity identity;
        try {
            identity =
                    tokenVerifier.verify(
                            codeExchange.exchangeForIdentityToken(authorizationCode, codeVerifier, redirectUri));
        } catch (GoogleExchangeFailedException | GoogleIdentityTokenInvalidException e) {
            log.warn("Google sign-in could not be completed: {}", e.getMessage());
            throw unauthorized();
        }

        if (!identity.emailVerified()) {
            // An unverified address must not become an account identity: anyone can claim an address they
            // do not control, and the email is what the user will later recognise the account by.
            log.warn("Google sign-in rejected because the email address is not verified");
            throw unauthorized();
        }

        UserAccount user =
                users.findByGoogleSubject(identity.subject())
                        .map(
                                existing -> {
                                    existing.refreshProfileFromGoogle(
                                            identity.email(), identity.displayName(), clock.instant());
                                    return users.save(existing);
                                })
                        .orElseGet(
                                () ->
                                        users.save(
                                                UserAccount.createFromGoogle(
                                                        identity.subject(),
                                                        identity.email(),
                                                        identity.displayName(),
                                                        clock.instant())));

        Issued issued = refreshTokens.issueNewChain(user.getId());
        return sessionFor(user, issued);
    }

    /** Renews a session by rotating its refresh token. */
    @Transactional
    public AuthenticatedSession renew(String refreshToken) {
        Issued issued;
        try {
            issued = refreshTokens.rotate(refreshToken);
        } catch (RefreshTokenRejectedException e) {
            log.warn("Session renewal refused: {}", e.getMessage());
            throw unauthorized();
        }
        // A token can outlive its user only if the account row was removed, which MVP 0 has no feature
        // for. Treated as unauthorized rather than as a 500, because the session is genuinely not usable.
        UserAccount user = users.findById(issued.userId()).orElseThrow(IdentityService::unauthorized);
        return sessionFor(user, issued);
    }

    /** Ends a session. Silent about whether the token was live, so it is not an oracle. */
    @Transactional
    public void signOut(String refreshToken) {
        refreshTokens.revokeChainOf(refreshToken);
    }

    /** The authenticated user, for the session-probe endpoint. */
    @Transactional(readOnly = true)
    public UserAccount requireUser(UUID userId) {
        return users.findById(userId).orElseThrow(IdentityService::unauthorized);
    }

    private AuthenticatedSession sessionFor(UserAccount user, Issued issued) {
        return new AuthenticatedSession(
                user,
                new IssuedSession(
                        user.getId(),
                        issued.sessionId(),
                        accessTokens.issue(user.getId(), issued.sessionId()),
                        accessTokens.accessTokenTtl(),
                        issued.rawToken()));
    }

    private static ApiException unauthorized() {
        return new ApiException(ProblemType.UNAUTHORIZED, ProblemType.UNAUTHORIZED.safeDetail());
    }

    /** A signed-in user together with the tokens just issued for them. */
    public record AuthenticatedSession(UserAccount user, IssuedSession session) {}
}
