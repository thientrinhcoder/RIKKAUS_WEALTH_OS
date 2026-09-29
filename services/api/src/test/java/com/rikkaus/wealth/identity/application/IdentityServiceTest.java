package com.rikkaus.wealth.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rikkaus.wealth.identity.application.IdentityService.AuthenticatedSession;
import com.rikkaus.wealth.identity.domain.UserAccount;
import com.rikkaus.wealth.identity.domain.UserAccountRepository;
import com.rikkaus.wealth.identity.google.GoogleAuthorizationCodeExchange;
import com.rikkaus.wealth.identity.google.GoogleAuthorizationCodeExchange.GoogleExchangeFailedException;
import com.rikkaus.wealth.identity.google.GoogleIdentity;
import com.rikkaus.wealth.identity.google.GoogleIdentityTokenVerifier;
import com.rikkaus.wealth.identity.google.GoogleIdentityTokenVerifier.GoogleIdentityTokenInvalidException;
import com.rikkaus.wealth.identity.google.GoogleOidcProperties;
import com.rikkaus.wealth.identity.session.AccessTokenService;
import com.rikkaus.wealth.identity.session.RefreshTokenService;
import com.rikkaus.wealth.identity.session.RefreshTokenService.Issued;
import com.rikkaus.wealth.identity.session.RefreshTokenService.RefreshTokenRejectedException;
import com.rikkaus.wealth.shared.error.ApiException;
import com.rikkaus.wealth.shared.error.ProblemType;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Sign-in, first-login account creation and the failure paths, with Google replaced by stubs.
 *
 * <p>Nothing here reaches the network. The two Google ports exist so this whole path is provable
 * offline, which is what keeps {@code mvnw test} runnable without credentials.
 */
class IdentityServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-27T14:36:00Z");
    private static final String ALLOWED_REDIRECT = "http://localhost:8081/auth/callback";
    private static final String CODE = "4/0AbUR2VPlaceholderAuthorizationCode";
    private static final String VERIFIER = "dBjftJeZ4CVPmB92K27uhbUJU1p1r-wW1gFWFOEjXk";
    private static final String ID_TOKEN = "a.google.id-token";
    private static final GoogleIdentity AN_NGUYEN =
            new GoogleIdentity("google-subject-102938", "an.nguyen@example.com", true, "An Nguyễn");

    private final GoogleAuthorizationCodeExchange codeExchange =
            mock(GoogleAuthorizationCodeExchange.class);
    private final GoogleIdentityTokenVerifier tokenVerifier = mock(GoogleIdentityTokenVerifier.class);
    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final AccessTokenService accessTokens = mock(AccessTokenService.class);
    private final RefreshTokenService refreshTokens = mock(RefreshTokenService.class);

    private IdentityService service;

    @BeforeEach
    void setUp() throws Exception {
        service =
                new IdentityService(
                        codeExchange,
                        tokenVerifier,
                        properties(ALLOWED_REDIRECT),
                        users,
                        accessTokens,
                        refreshTokens,
                        Clock.fixed(NOW, ZoneOffset.UTC));
        when(codeExchange.exchangeForIdentityToken(CODE, VERIFIER, ALLOWED_REDIRECT)).thenReturn(ID_TOKEN);
        when(tokenVerifier.verify(ID_TOKEN)).thenReturn(AN_NGUYEN);
        when(users.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accessTokens.issue(any(), any())).thenReturn("an.access.token");
        when(accessTokens.accessTokenTtl()).thenReturn(Duration.ofMinutes(15));
        when(refreshTokens.issueNewChain(any()))
                .thenAnswer(
                        invocation ->
                                new Issued(invocation.getArgument(0), UUID.randomUUID(), "a-refresh-token"));
    }

    @Test
    void aFirstSignInCreatesTheAccountFromTheVerifiedClaims() {
        when(users.findByGoogleSubject(AN_NGUYEN.subject())).thenReturn(Optional.empty());

        AuthenticatedSession session = service.signInWithGoogle(CODE, VERIFIER, ALLOWED_REDIRECT);

        assertThat(session.user().getGoogleSubject()).isEqualTo("google-subject-102938");
        assertThat(session.user().getEmail()).isEqualTo("an.nguyen@example.com");
        assertThat(session.user().getDisplayName()).isEqualTo("An Nguyễn");
        assertThat(session.user().getCreatedAt()).isEqualTo(NOW);
        assertThat(session.session().refreshToken()).isEqualTo("a-refresh-token");
        assertThat(session.session().accessToken()).isEqualTo("an.access.token");
    }

    @Test
    void aReturningSignInResumesTheSameAccountAndRefreshesTheProfile() {
        UserAccount existing =
                UserAccount.createFromGoogle(
                        AN_NGUYEN.subject(), "old.address@example.com", "Old Name", NOW.minusSeconds(86400));
        when(users.findByGoogleSubject(AN_NGUYEN.subject())).thenReturn(Optional.of(existing));

        AuthenticatedSession session = service.signInWithGoogle(CODE, VERIFIER, ALLOWED_REDIRECT);

        // Same row, so the user's data follows them; profile fields track Google, which is the reason
        // email is stored at all.
        assertThat(session.user().getId()).isEqualTo(existing.getId());
        assertThat(session.user().getEmail()).isEqualTo("an.nguyen@example.com");
        assertThat(session.user().getDisplayName()).isEqualTo("An Nguyễn");
        assertThat(session.user().getCreatedAt()).isEqualTo(NOW.minusSeconds(86400));
        assertThat(session.user().getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void aRedirectUriOutsideTheAllowlistIsRejectedBeforeGoogleIsCalled() throws Exception {
        ApiException thrown =
                catchThrowableOfType(
                        ApiException.class,
                        () -> service.signInWithGoogle(CODE, VERIFIER, "http://evil.example.com/callback"));

        assertThat(thrown.getProblemType()).isEqualTo(ProblemType.VALIDATION_FAILED);
        assertThat(thrown.getExtensions())
                .containsEntry(
                        "errors",
                        List.of(Map.of("field", "redirectUri", "message", "is not an allowed redirect URI")));
        // The assertion that matters: an unregistered redirect URI must never be forwarded, because
        // Google would deliver the authorization code to it.
        verifyNoInteractions(codeExchange);
    }

    @Test
    void anUnverifiedEmailAddressCannotBecomeAnAccount() throws Exception {
        when(tokenVerifier.verify(ID_TOKEN))
                .thenReturn(new GoogleIdentity("google-subject-777", "unverified@example.com", false, "X"));

        assertUnauthorized(() -> service.signInWithGoogle(CODE, VERIFIER, ALLOWED_REDIRECT));
        verify(users, never()).save(any());
    }

    @Test
    void aRejectedAuthorizationCodeIsAnUnauthorizedNotAServerError() throws Exception {
        when(codeExchange.exchangeForIdentityToken(anyString(), anyString(), anyString()))
                .thenThrow(new GoogleExchangeFailedException("code already redeemed"));

        // A reused or expired code is the ordinary "callback failure" state, so it must not surface as a
        // 500 that reads like an outage.
        assertUnauthorized(() -> service.signInWithGoogle(CODE, VERIFIER, ALLOWED_REDIRECT));
    }

    @Test
    void aForgedIdentityTokenIsUnauthorized() throws Exception {
        when(tokenVerifier.verify(ID_TOKEN))
                .thenThrow(new GoogleIdentityTokenInvalidException("bad signature"));

        assertUnauthorized(() -> service.signInWithGoogle(CODE, VERIFIER, ALLOWED_REDIRECT));
        verify(users, never()).save(any());
    }

    @Test
    void renewalRotatesTheTokenAndReturnsTheSameUser() throws Exception {
        UserAccount user =
                UserAccount.createFromGoogle("google-subject-102938", "an.nguyen@example.com", "An Nguyễn", NOW);
        // The rotated chain belongs to this user, so the identifier the service looks up and the
        // identifier it stamps into the new access token are the same one.
        UUID userId = user.getId();
        UUID sessionId = UUID.randomUUID();
        when(refreshTokens.rotate("old-token")).thenReturn(new Issued(userId, sessionId, "new-token"));
        when(users.findById(userId)).thenReturn(Optional.of(user));

        AuthenticatedSession renewed = service.renew("old-token");

        assertThat(renewed.session().refreshToken()).isEqualTo("new-token");
        assertThat(renewed.user().getEmail()).isEqualTo("an.nguyen@example.com");
        verify(accessTokens).issue(eq(userId), eq(sessionId));
    }

    @Test
    void aRejectedRefreshTokenIsUnauthorized() throws Exception {
        when(refreshTokens.rotate("replayed")).thenThrow(new RefreshTokenRejectedException("replay"));

        assertUnauthorized(() -> service.renew("replayed"));
    }

    @Test
    void signingOutDelegatesToChainRevocationAndSaysNothingBack() {
        service.signOut("a-token");

        verify(refreshTokens).revokeChainOf("a-token");
    }

    /**
     * Asserts the rejection and, just as importantly, that it says nothing about which check failed.
     *
     * <p>Every caller below arranges a different internal cause — a redeemed code, a forged token, an
     * unverified address, a replayed refresh token — and each must produce the same type and the same
     * fixed detail text. Asserting the detail here rather than per test is what makes it impossible to
     * add a sign-in failure path that quietly explains itself to the caller.
     */
    private static void assertUnauthorized(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOf(ApiException.class)
                .hasMessage(ProblemType.UNAUTHORIZED.safeDetail())
                .satisfies(
                        thrown ->
                                assertThat(((ApiException) thrown).getProblemType())
                                        .isEqualTo(ProblemType.UNAUTHORIZED));
    }

    private static GoogleOidcProperties properties(String redirectUris) {
        return new GoogleOidcProperties(
                "test-google-oauth-client-id",
                null,
                redirectUris,
                "https://oauth2.googleapis.com/token",
                "https://www.googleapis.com/oauth2/v3/certs",
                List.of("https://accounts.google.com", "accounts.google.com"));
    }
}
