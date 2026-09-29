package com.rikkaus.wealth.support;

import com.rikkaus.wealth.identity.google.GoogleAuthorizationCodeExchange;
import com.rikkaus.wealth.identity.google.GoogleIdentity;
import com.rikkaus.wealth.identity.google.GoogleIdentityTokenVerifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Stands in for Google in every integration test.
 *
 * <p>The real adapters reach {@code oauth2.googleapis.com}, which would make the suite need network
 * access, live OAuth credentials and a real person clicking consent — so integration tests could run
 * neither offline nor in CI. Replacing the two ports is the entire reason they exist as interfaces.
 *
 * <p>What is <em>not</em> stubbed is everything that matters: the account row, the refresh-token chain,
 * the signed access token, the security filter chain and the HTTP layer are all real. Only the identity
 * claim is supplied rather than fetched, which is the one part a test cannot legitimately obtain.
 *
 * <p>A single {@code @Primary @Component} rather than a {@code @TestConfiguration} each test imports.
 * An {@code @Import} changes a test's context cache key, so every importing class would build its own
 * application context; one unconditional bean on the test classpath keeps the whole suite on one cached
 * context. It is {@code @Primary} rather than a bean-definition override because overriding is disabled
 * in this project by default, and enabling it globally would let a genuinely accidental duplicate pass
 * unnoticed.
 *
 * <p>This class lives inside the component-scan root on purpose, unlike
 * {@code com.rikkaus.testfixtures.ProblemFixtureController}. It declares no request mapping, so it
 * cannot leak a route into the published contract — which is the specific hazard that keeps the fixture
 * controller outside.
 */
@Component
@Primary
public class StubGoogleIdentityProvider
        implements GoogleAuthorizationCodeExchange, GoogleIdentityTokenVerifier {

    public static final String ANY_CODE = "a-test-authorization-code";
    public static final String ANY_VERIFIER = "a-test-pkce-code-verifier";

    /** Must match {@code rikkaus.auth.google.redirect-uris} in {@code application-test.yml}. */
    public static final String ALLOWED_REDIRECT_URI = "http://localhost:8081/auth/callback";

    private static final String STUBBED_ID_TOKEN = "a-stubbed-google-id-token";

    /** Volatile because the container serves requests on other threads than the test's. */
    private volatile GoogleIdentity next =
            new GoogleIdentity("google-subject-default", "default@example.com", true, "Mặc định");

    public void willReturn(GoogleIdentity identity) {
        this.next = identity;
    }

    @Override
    public String exchangeForIdentityToken(
            String authorizationCode, String codeVerifier, String redirectUri) {
        // The returned value is never parsed, because verify() below is stubbed too. The two ports stay
        // separate precisely so neither has to produce input the other can read.
        return STUBBED_ID_TOKEN;
    }

    @Override
    public GoogleIdentity verify(String idToken) {
        return next;
    }
}
