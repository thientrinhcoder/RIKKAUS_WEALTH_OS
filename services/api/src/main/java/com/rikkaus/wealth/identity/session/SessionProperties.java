package com.rikkaus.wealth.identity.session;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Token lifetimes and the access-token signing secret.
 *
 * <p>The secret has <strong>no default</strong>, and a value shorter than {@link #MINIMUM_SECRET_BYTES}
 * aborts context refresh. A guessable signing key is not a partial weakness: anyone who can guess it
 * mints a valid token for any user id, which defeats every other control in this slice at once. So
 * the application must refuse to start rather than run with a placeholder, and there is deliberately
 * no fallback that would let a deployment miss the variable and still boot.
 *
 * <p>The minimum is 32 bytes because that is HS256's key length; a shorter key is also rejected
 * outright by Nimbus, but failing here names the property instead of surfacing as a signing error on
 * the first sign-in attempt.
 */
@ConfigurationProperties("rikkaus.auth")
public record SessionProperties(Jwt jwt, Duration refreshTokenTtl) {

    static final int MINIMUM_SECRET_BYTES = 32;

    public SessionProperties {
        if (jwt == null || jwt.secret() == null || jwt.secret().isBlank()) {
            throw new IllegalStateException(
                    "rikkaus.auth.jwt.secret is not set. Supply at least "
                            + MINIMUM_SECRET_BYTES
                            + " bytes, for example through the RIKKAUS_JWT_SECRET environment variable.");
        }
        int length = jwt.secret().getBytes(StandardCharsets.UTF_8).length;
        if (length < MINIMUM_SECRET_BYTES) {
            throw new IllegalStateException(
                    "rikkaus.auth.jwt.secret is "
                            + length
                            + " bytes; HS256 requires at least "
                            + MINIMUM_SECRET_BYTES
                            + ".");
        }
        if (refreshTokenTtl == null || refreshTokenTtl.isZero() || refreshTokenTtl.isNegative()) {
            throw new IllegalStateException("rikkaus.auth.refresh-token-ttl must be a positive duration.");
        }
    }

    public record Jwt(String secret, Duration accessTokenTtl) {

        public Jwt {
            if (accessTokenTtl == null || accessTokenTtl.isZero() || accessTokenTtl.isNegative()) {
                throw new IllegalStateException(
                        "rikkaus.auth.jwt.access-token-ttl must be a positive duration.");
            }
        }
    }
}
