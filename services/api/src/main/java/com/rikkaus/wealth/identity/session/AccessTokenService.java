package com.rikkaus.wealth.identity.session;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.rikkaus.wealth.shared.security.AccessTokenVerifier;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

/**
 * Issues and verifies the short-lived access token.
 *
 * <p>HS256 with a shared secret rather than an asymmetric key, because one application both issues
 * and verifies: there is no third party that needs a public key, and a symmetric key removes a
 * keypair and its rotation from the MVP's operational surface. The decision record already treats the
 * signing key as an external secret, which is the property that actually matters.
 *
 * <p>The token carries the user id and the session id and nothing else. An access token travels in a
 * header, through proxies and into client storage, so every additional claim is data that leaks with
 * it; the email and display name are available from {@code GET /api/v1/auth/session} to anyone who
 * already holds the token, which is the same audience, without being stamped into a bearer credential.
 *
 * <p>The session id claim is what lets a later request be traced to a rotation chain, so revoking a
 * chain can be reasoned about without a database lookup on every authenticated request.
 */
@Service
public class AccessTokenService implements AccessTokenVerifier {

    /** The {@code iss} claim. Fixed, and asserted on decode so a token from elsewhere is rejected. */
    static final String ISSUER = "rikkaus-wealth-api";

    /** The session-id claim name. Short, because it is repeated in every token. */
    static final String SESSION_ID_CLAIM = "sid";

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Clock clock;
    private final Duration accessTokenTtl;

    AccessTokenService(SessionProperties properties, Clock clock) {
        SecretKeySpec key =
                new SecretKeySpec(
                        properties.jwt().secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        NimbusJwtDecoder nimbusDecoder =
                NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        // The decoder is reduced to signature verification alone, and the claim checks move to
        // verify() below. NimbusJwtDecoder otherwise installs a default JwtTimestampValidator that
        // reads the *system* clock, not the Clock bean injected here, so an expiry assertion written
        // against a test clock would be decided by wall-clock time instead — it fails or passes
        // depending on when the suite runs. Replacing the validator is what makes the injected clock
        // the single source of "now" for this class, as verify() documents.
        nimbusDecoder.setJwtValidator(token -> OAuth2TokenValidatorResult.success());
        this.decoder = nimbusDecoder;
        this.clock = clock;
        this.accessTokenTtl = properties.jwt().accessTokenTtl();
    }

    public String issue(UUID userId, UUID sessionId) {
        Instant now = clock.instant();
        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer(ISSUER)
                        .subject(userId.toString())
                        .claim(SESSION_ID_CLAIM, sessionId.toString())
                        .issuedAt(now)
                        .expiresAt(now.plus(accessTokenTtl))
                        .build();
        return encoder
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    /**
     * Verifies a token and returns who it authenticates.
     *
     * <p>Expiry and issuer are checked here against the injected {@link Clock} rather than by the
     * decoder's default validators, which read the system clock — see the constructor. A test can
     * therefore advance time instead of sleeping, and a token with a missing {@code exp} is a rejection
     * rather than a token that never expires.
     *
     * <p>Every failure — bad signature, wrong issuer, expired, unparseable subject — returns the same
     * empty result. The caller turns that into one 401; distinguishing the causes on the wire would
     * tell an attacker which part of a forged token to fix next. The cause is still available in the
     * exception for a caller that wants to log it server-side.
     */
    @Override
    public VerifiedAccessToken verify(String token) throws InvalidAccessTokenException {
        Jwt jwt;
        try {
            jwt = decoder.decode(token);
        } catch (JwtException | IllegalArgumentException e) {
            // IllegalArgumentException as well as JwtException, because Nimbus's default claim-set
            // converter throws the former for a claim it cannot coerce. Either way the token is
            // unusable, and both must produce one rejection rather than escaping as a 500.
            throw new InvalidAccessTokenException("Access token could not be decoded", e);
        }
        // Read as a string, not through Jwt#getIssuer, which coerces `iss` to a java.net.URL and
        // therefore rejects any issuer that is not an HTTP URL. This project owns no domain — the same
        // reason ProblemType uses a URN rather than an https: type — so the issuer is a stable plain
        // identifier and must be compared as one.
        if (!ISSUER.equals(jwt.getClaimAsString("iss"))) {
            throw new InvalidAccessTokenException("Access token was issued by another party", null);
        }
        Instant expiresAt = jwt.getExpiresAt();
        if (expiresAt == null || !clock.instant().isBefore(expiresAt)) {
            throw new InvalidAccessTokenException("Access token has expired", null);
        }
        try {
            return new VerifiedAccessToken(
                    UUID.fromString(jwt.getSubject()),
                    UUID.fromString(jwt.getClaimAsString(SESSION_ID_CLAIM)));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidAccessTokenException("Access token identifiers are not well formed", e);
        }
    }

    public Duration accessTokenTtl() {
        return accessTokenTtl;
    }
}
