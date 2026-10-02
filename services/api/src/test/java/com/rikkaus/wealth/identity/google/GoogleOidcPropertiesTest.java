package com.rikkaus.wealth.identity.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** The redirect-URI allowlist, which is the open-redirect defence. */
class GoogleOidcPropertiesTest {

    private static final String ALLOWED = "http://localhost:8081/auth/callback";

    @Test
    void anEmptyAllowlistPermitsNothing() {
        // The default in every environment. A deployment that configures no redirect URI must reject
        // every candidate rather than fall back to guessing one.
        GoogleOidcProperties properties = propertiesWith("");

        assertThat(properties.allowedRedirectUris()).isEmpty();
        assertThat(properties.permitsRedirectUri(ALLOWED)).isFalse();
    }

    @Test
    void aNullAllowlistPermitsNothing() {
        assertThat(propertiesWith(null).permitsRedirectUri(ALLOWED)).isFalse();
    }

    @Test
    void blankEntriesAreNotTreatedAsAnAllowedValue() {
        // A trailing comma is the realistic configuration slip. It must not produce a list holding one
        // empty string, because an empty candidate would then be permitted.
        GoogleOidcProperties properties = propertiesWith(ALLOWED + ", ,");

        assertThat(properties.allowedRedirectUris()).containsExactly(ALLOWED);
        assertThat(properties.permitsRedirectUri("")).isFalse();
    }

    @Test
    void anAllowlistedUriIsPermitted() {
        assertThat(propertiesWith(" " + ALLOWED + " ").permitsRedirectUri(ALLOWED)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "http://localhost:8081/auth/callback/evil",
                "http://localhost:8081/auth/callbackX",
                "http://localhost:8081",
                "https://localhost:8081/auth/callback",
                "http://localhost:8082/auth/callback",
                "http://evil.example.com/auth/callback",
                "http://localhost:8081/auth/callback?next=http://evil.example.com"
            })
    void aUriThatIsMerelySimilarIsRejected(String candidate) {
        // Exact comparison, not prefix or origin matching. Each of these would be accepted by some
        // looser rule, and each one delivers the authorization code somewhere the operator never
        // registered.
        assertThat(propertiesWith(ALLOWED).permitsRedirectUri(candidate)).isFalse();
    }

    @Test
    void aMissingTokenUriIsRefusedAtStartup() {
        assertThatThrownBy(
                        () ->
                                new GoogleOidcProperties(
                                        "client", null, ALLOWED, " ", "https://jwks", List.of("https://issuer")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("token-uri");
    }

    @Test
    void anEmptyIssuerListIsRefusedAtStartup() {
        assertThatThrownBy(
                        () ->
                                new GoogleOidcProperties(
                                        "client", null, ALLOWED, "https://token", "https://jwks", List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("issuers");
    }

    @Test
    void aBlankClientSecretCountsAsAbsent() {
        // A public PKCE client legitimately has none, and an empty environment variable must not be
        // forwarded to Google as an empty client_secret parameter.
        assertThat(propertiesWith(ALLOWED).hasClientSecret()).isFalse();
        assertThat(
                        new GoogleOidcProperties(
                                        "client", "   ", ALLOWED, "https://token", "https://jwks", List.of("iss"))
                                .hasClientSecret())
                .isFalse();
    }

    private static GoogleOidcProperties propertiesWith(String redirectUris) {
        return new GoogleOidcProperties(
                "test-google-oauth-client-id",
                null,
                redirectUris,
                "https://oauth2.googleapis.com/token",
                "https://www.googleapis.com/oauth2/v3/certs",
                List.of("https://accounts.google.com", "accounts.google.com"));
    }
}
