package com.rikkaus.wealth.identity.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.rikkaus.wealth.shared.security.AccessTokenVerifier.InvalidAccessTokenException;
import com.rikkaus.wealth.shared.security.AccessTokenVerifier.VerifiedAccessToken;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Unit tests for access-token issuing and verification. No database, no Spring context. */
class AccessTokenServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-09-27T14:36:00Z");
    private static final String SECRET = "a-test-signing-secret-of-sufficient-length";
    private static final Duration TTL = Duration.ofMinutes(15);

    private final MutableClock clock = new MutableClock(FIXED_NOW);
    private final AccessTokenService service = serviceWith(SECRET, clock);

    @Test
    void aFreshTokenVerifiesToTheUserAndSessionItWasIssuedFor() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        VerifiedAccessToken principal = service.verify(service.issue(userId, sessionId));

        assertThat(principal).isEqualTo(new VerifiedAccessToken(userId, sessionId));
    }

    @Test
    void aTokenIsRejectedOnceItsLifetimeHasElapsed() {
        String token = service.issue(UUID.randomUUID(), UUID.randomUUID());

        clock.advanceBy(TTL.plusSeconds(1));

        assertThatThrownBy(() -> service.verify(token))
                .isInstanceOf(InvalidAccessTokenException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void aTokenIsStillValidOneSecondBeforeItExpires() throws Exception {
        String token = service.issue(UUID.randomUUID(), UUID.randomUUID());

        clock.advanceBy(TTL.minusSeconds(1));

        assertThat(service.verify(token)).isNotNull();
    }

    @Test
    void aTokenSignedWithAnotherSecretIsRejected() {
        // The case that matters most: a forged token must not authenticate anyone. Two services with
        // different secrets stand in for an attacker who guessed the algorithm but not the key.
        String foreign =
                serviceWith("an-entirely-different-secret-of-good-length", clock)
                        .issue(UUID.randomUUID(), UUID.randomUUID());

        assertThatThrownBy(() -> service.verify(foreign))
                .isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    void aTamperedTokenIsRejected() {
        String token = service.issue(UUID.randomUUID(), UUID.randomUUID());
        String[] segments = token.split("\\.");
        // The FIRST character of the signature, not the last. An HS256 signature is 32 bytes encoded as 43
        // base64url characters, so the final character carries only two significant bits and four bits of
        // padding: several distinct characters there decode to identical bytes, and flipping it is a no-op
        // often enough to make the test flaky. The first character carries a full six bits, so changing it
        // always changes the signature.
        char first = segments[2].charAt(0);
        String tamperedSignature = (first == 'A' ? 'B' : 'A') + segments[2].substring(1);
        String tampered = segments[0] + "." + segments[1] + "." + tamperedSignature;

        assertThat(tampered).isNotEqualTo(token);
        assertThatThrownBy(() -> service.verify(tampered))
                .isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    void garbageIsRejectedWithoutLeakingAParserError() {
        InvalidAccessTokenException thrown =
                catchThrowableOfType(
                        InvalidAccessTokenException.class, () -> service.verify("not-a-jwt-at-all"));

        assertThat(thrown).isNotNull();
        assertThat(thrown.getMessage()).isEqualTo("Access token could not be decoded");
    }

    @Test
    void theTokenCarriesNoPersonalDataInItsPayload() {
        // An access token travels in headers, proxies and client storage. This asserts the payload is
        // limited to identifiers, so a leaked token discloses nothing about the person it belongs to.
        String payload =
                new String(
                        java.util.Base64.getUrlDecoder()
                                .decode(service.issue(UUID.randomUUID(), UUID.randomUUID()).split("\\.")[1]),
                        java.nio.charset.StandardCharsets.UTF_8);

        assertThat(payload).doesNotContain("email").doesNotContain("name").doesNotContain("@");
    }

    private static AccessTokenService serviceWith(String secret, Clock clock) {
        return new AccessTokenService(
                new SessionProperties(new SessionProperties.Jwt(secret, TTL), Duration.ofDays(30)),
                clock);
    }

    /** A clock a test can move, so expiry is provable without sleeping. */
    private static final class MutableClock extends Clock {
        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        void advanceBy(Duration amount) {
            now = now.plus(amount);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }
}
