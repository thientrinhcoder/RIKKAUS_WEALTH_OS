package com.rikkaus.wealth.identity.google;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Google OAuth client configuration.
 *
 * <p>{@code redirectUris} is an allowlist and <strong>empty permits nothing</strong>, the same
 * fail-closed posture {@code CorsConfiguration} takes with origins. A client-supplied redirect URI is
 * forwarded to Google as part of the exchange, and Google delivers the authorization code to whatever
 * it names, so accepting an arbitrary value is how codes end up at an attacker's endpoint. The list is
 * parsed from a raw comma-separated string rather than bound straight to a {@code List}, so an unset or
 * blank property is unambiguously an empty allowlist rather than a list holding one empty entry.
 *
 * <p>{@code clientSecret} is optional. A public client using PKCE alone needs none, and requiring one
 * would force a placeholder into a configuration where its absence is correct.
 */
@ConfigurationProperties("rikkaus.auth.google")
public record GoogleOidcProperties(
        String clientId,
        String clientSecret,
        String redirectUris,
        String tokenUri,
        String jwksUri,
        List<String> issuers) {

    public GoogleOidcProperties {
        if (tokenUri == null || tokenUri.isBlank()) {
            throw new IllegalStateException("rikkaus.auth.google.token-uri must be set.");
        }
        if (jwksUri == null || jwksUri.isBlank()) {
            throw new IllegalStateException("rikkaus.auth.google.jwks-uri must be set.");
        }
        if (issuers == null || issuers.isEmpty()) {
            throw new IllegalStateException("rikkaus.auth.google.issuers must list at least one issuer.");
        }
    }

    /** The parsed allowlist. Empty means no redirect URI is acceptable. */
    public List<String> allowedRedirectUris() {
        if (redirectUris == null) {
            return List.of();
        }
        return Arrays.stream(redirectUris.split(","))
                .map(String::trim)
                .filter(uri -> !uri.isEmpty())
                .toList();
    }

    /**
     * Exact string comparison, not URI normalisation or prefix matching. Google itself compares the
     * redirect URI exactly, and a looser check here would accept a value Google then rejects — or worse,
     * accept a path under an allowlisted origin that the operator never intended to register.
     */
    public boolean permitsRedirectUri(String candidate) {
        return candidate != null && allowedRedirectUris().contains(candidate);
    }

    public boolean hasClientSecret() {
        return clientSecret != null && !clientSecret.isBlank();
    }
}
