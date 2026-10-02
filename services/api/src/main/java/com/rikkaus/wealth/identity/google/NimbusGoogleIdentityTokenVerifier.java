package com.rikkaus.wealth.identity.google;

import java.util.List;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

/**
 * Verifies a Google identity token against Google's published signing keys.
 *
 * <p>The decoder is built from Google's JWKS URI, so key rotation needs no deployment: Nimbus fetches
 * and caches the current keys. Signature, expiry and {@code nbf} are checked by the decoder's default
 * validators, which is correct here — unlike our own access tokens, these timestamps are Google's and
 * must be judged against real time rather than against an injectable clock.
 *
 * <p>Issuer and audience are checked explicitly rather than through {@code withIssuerLocation}, because
 * Google issues both {@code https://accounts.google.com} and the bare {@code accounts.google.com} and a
 * single-issuer validator rejects one of them.
 *
 * <p>The audience check is the one that stops token substitution: without it, an identity token minted
 * for an entirely different Google OAuth client would verify here and sign that person in.
 */
@Component
class NimbusGoogleIdentityTokenVerifier implements GoogleIdentityTokenVerifier {

    private final JwtDecoder decoder;
    private final String clientId;
    private final List<String> acceptedIssuers;

    NimbusGoogleIdentityTokenVerifier(GoogleOidcProperties properties) {
        this.decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwksUri()).build();
        this.clientId = properties.clientId();
        this.acceptedIssuers = List.copyOf(properties.issuers());
    }

    @Override
    public GoogleIdentity verify(String idToken) throws GoogleIdentityTokenInvalidException {
        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new GoogleIdentityTokenInvalidException("Google identity token is not valid", e);
        }

        if (!acceptedIssuers.contains(jwt.getClaimAsString("iss"))) {
            throw new GoogleIdentityTokenInvalidException("Google identity token has an unexpected issuer");
        }
        if (!jwt.getAudience().contains(clientId)) {
            throw new GoogleIdentityTokenInvalidException(
                    "Google identity token was issued for another client");
        }
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new GoogleIdentityTokenInvalidException("Google identity token carries no subject");
        }

        return new GoogleIdentity(
                subject,
                jwt.getClaimAsString("email"),
                Boolean.TRUE.equals(jwt.getClaim("email_verified")),
                jwt.getClaimAsString("name"));
    }
}
