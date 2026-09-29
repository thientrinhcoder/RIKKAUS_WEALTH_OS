package com.rikkaus.wealth.identity.session;

import java.time.Duration;
import java.util.UUID;

/**
 * A freshly issued pair of tokens.
 *
 * <p>This is the only place the raw refresh token exists after issuing; the database holds a hash and
 * nothing recovers the original from it. It is carried to the HTTP response and then discarded, and
 * is deliberately not exposed by any getter that a log statement might reach for.
 */
public record IssuedSession(
        UUID userId,
        UUID sessionId,
        String accessToken,
        Duration accessTokenTtl,
        String refreshToken) {}
