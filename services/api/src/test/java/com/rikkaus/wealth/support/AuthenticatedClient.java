package com.rikkaus.wealth.support;

import com.rikkaus.wealth.identity.application.IdentityService;
import com.rikkaus.wealth.identity.application.IdentityService.AuthenticatedSession;
import com.rikkaus.wealth.identity.google.GoogleIdentity;
import java.util.UUID;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Signs a fresh user in and hands back the headers to call a protected route as them.
 *
 * <p>This is the reusable piece every later slice's integration tests need: without it, each one would
 * reinvent stubbing Google, minting a token and attaching a header, and the first slice to get it subtly
 * wrong would be testing an unauthenticated request against a route it believed was protected.
 *
 * <p>It goes through the real {@link IdentityService}, so the account row, the refresh-token chain and the
 * signed access token are all genuine — only Google is stubbed, by {@link StubGoogleIdentityProvider}. A
 * shortcut that injected an {@code Authentication} directly would skip the filter and the token, which are
 * the parts most likely to break.
 *
 * <p>Each call creates a distinct user, which is what makes a cross-user ownership test a two-line setup.
 */
@Component
public class AuthenticatedClient {

    private final IdentityService identityService;
    private final StubGoogleIdentityProvider googleStub;

    AuthenticatedClient(IdentityService identityService, StubGoogleIdentityProvider googleStub) {
        this.identityService = identityService;
        this.googleStub = googleStub;
    }


    /** Signs in a brand-new user and returns their session. */
    public AuthenticatedSession signInNewUser() {
        String subject = "google-subject-" + UUID.randomUUID();
        googleStub.willReturn(
                new GoogleIdentity(subject, subject + "@example.com", true, "Người dùng thử nghiệm"));
        return identityService.signInWithGoogle(
                StubGoogleIdentityProvider.ANY_CODE,
                StubGoogleIdentityProvider.ANY_VERIFIER,
                StubGoogleIdentityProvider.ALLOWED_REDIRECT_URI);
    }

    /** Bearer headers for an existing session. */
    public HttpHeaders bearerHeadersFor(AuthenticatedSession session) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(session.session().accessToken());
        return headers;
    }

    /** An entity carrying only the bearer header, for a GET or a bodyless request. */
    public HttpEntity<Void> bearerEntityFor(AuthenticatedSession session) {
        return new HttpEntity<>(bearerHeadersFor(session));
    }

    /** An entity carrying the bearer header and a JSON body. */
    public <T> HttpEntity<T> bearerEntityFor(AuthenticatedSession session, T body) {
        HttpHeaders headers = bearerHeadersFor(session);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    /** Signs in a brand-new user and returns an entity carrying only their bearer header. */
    public HttpEntity<Void> newUserBearerEntity() {
        return bearerEntityFor(signInNewUser());
    }
}
