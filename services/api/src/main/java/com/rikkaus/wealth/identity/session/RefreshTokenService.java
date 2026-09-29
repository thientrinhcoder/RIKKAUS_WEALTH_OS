package com.rikkaus.wealth.identity.session;

import com.rikkaus.wealth.identity.domain.RefreshToken;
import com.rikkaus.wealth.identity.domain.RefreshToken.RevocationReason;
import com.rikkaus.wealth.identity.domain.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Issues, rotates and revokes refresh tokens.
 *
 * <p><strong>SHA-256, not BCrypt or Argon2, and that is deliberate.</strong> An adaptive password hash
 * exists to make brute force expensive against a low-entropy human secret. A refresh token here is
 * {@value #TOKEN_BYTES} bytes straight from {@link SecureRandom}, so there is no dictionary to run and
 * nothing for a cost factor to buy; what it would buy instead is latency on the hot path, because a
 * refresh happens every time an access token expires. The property that matters — a stolen database
 * yields no usable token — is given by any preimage-resistant hash. A later security review should read
 * this paragraph before "upgrading" it.
 *
 * <p>Rotation is single-use: exchanging a token revokes it. Presenting an already-revoked token is
 * treated as evidence the value leaked, and revokes the entire chain rather than just failing, because
 * by then an attacker may already hold the successor. The legitimate user is signed out and signs in
 * again, which is the correct trade against an active session hijack.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    /** 256 bits. Enough that guessing is not a threat model, which is what justifies the plain hash. */
    private static final int TOKEN_BYTES = 32;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final RefreshTokenRepository refreshTokens;
    private final Clock clock;
    private final Duration refreshTokenTtl;
    private final TransactionTemplate independentTransaction;

    RefreshTokenService(
            RefreshTokenRepository refreshTokens,
            Clock clock,
            SessionProperties properties,
            PlatformTransactionManager transactionManager) {
        this.refreshTokens = refreshTokens;
        this.clock = clock;
        this.refreshTokenTtl = properties.refreshTokenTtl();
        // Revoking a replayed chain must outlive the rejection of the request that revealed it. The
        // caller turns the rejection into an ApiException, and that runtime exception rolls back the
        // surrounding transaction — which, without a separate one here, would undo the revocation and
        // leave replay detection doing nothing at all while every unit test still passed. Self-invoking
        // an @Transactional(REQUIRES_NEW) method would not work either, because self-invocation bypasses
        // the proxy, so the new transaction is started explicitly.
        this.independentTransaction = new TransactionTemplate(transactionManager);
        this.independentTransaction.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Starts a new rotation chain, for a fresh sign-in.
     *
     * @return the raw token, which exists only in this return value from here on
     */
    @Transactional
    public Issued issueNewChain(UUID userId) {
        return issueInto(userId, UUID.randomUUID());
    }

    /**
     * Exchanges a token for its successor in the same chain.
     *
     * @throws RefreshTokenRejectedException when the token is unknown, expired, or a replay
     */
    @Transactional
    public Issued rotate(String rawToken) throws RefreshTokenRejectedException {
        Instant now = clock.instant();
        RefreshToken presented =
                refreshTokens
                        .findByTokenHash(hash(rawToken))
                        .orElseThrow(
                                () ->
                                        new RefreshTokenRejectedException(
                                                "Refresh token is not recognised"));

        if (presented.isRevoked()) {
            // Already exchanged, so the value reached someone after its single legitimate use. The
            // successor may already be in an attacker's hands, so the chain goes, not just this token.
            // Logged with identifiers only: the token and its hash are exactly what must never appear
            // in a log, and the user and session ids are enough to investigate.
            log.warn(
                    "Revoked refresh token presented again; revoking session {} for user {}",
                    presented.getSessionId(),
                    presented.getUserId());
            UUID compromisedSession = presented.getSessionId();
            independentTransaction.executeWithoutResult(
                    status -> revokeChain(compromisedSession, RevocationReason.REPLAYED, now));
            throw new RefreshTokenRejectedException("Refresh token was already used");
        }
        if (presented.hasExpiredAt(now)) {
            throw new RefreshTokenRejectedException("Refresh token has expired");
        }

        presented.revoke(RevocationReason.ROTATED, now);
        return issueInto(presented.getUserId(), presented.getSessionId());
    }

    /**
     * Ends the chain a token belongs to.
     *
     * <p>Reports nothing about whether the token was known or already dead. Signing out twice is not a
     * user error, and an endpoint that distinguished the cases would answer "is this token live?" for
     * anyone holding a guess.
     */
    @Transactional
    public void revokeChainOf(String rawToken) {
        Optional<RefreshToken> presented = refreshTokens.findByTokenHash(hash(rawToken));
        presented.ifPresent(
                token -> revokeChain(token.getSessionId(), RevocationReason.LOGGED_OUT, clock.instant()));
    }

    private Issued issueInto(UUID userId, UUID sessionId) {
        byte[] raw = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(raw);
        String rawToken = ENCODER.encodeToString(raw);
        Instant now = clock.instant();
        refreshTokens.save(
                RefreshToken.issue(userId, sessionId, hash(rawToken), now, now.plus(refreshTokenTtl)));
        return new Issued(userId, sessionId, rawToken);
    }

    private void revokeChain(UUID sessionId, RevocationReason reason, Instant when) {
        List<RefreshToken> chain = refreshTokens.findBySessionId(sessionId);
        chain.forEach(token -> token.revoke(reason, when));
        refreshTokens.saveAll(chain);
    }

    /**
     * Lowercase hex SHA-256.
     *
     * <p>Not constant-time, and it does not need to be: the comparison happens inside the database on
     * an indexed column, and the value being compared is a hash of an unguessable token, so there is no
     * secret an attacker could recover by measuring how long a lookup took.
     */
    static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the JDK specification, so this is unreachable.
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    /** A newly issued refresh token and the chain it belongs to. */
    public record Issued(UUID userId, UUID sessionId, String rawToken) {}

    /** Why a refresh attempt failed. The message is for the server log, never for the client. */
    public static class RefreshTokenRejectedException extends Exception {
        public RefreshTokenRejectedException(String message) {
            super(message);
        }
    }
}
