package com.rikkaus.wealth.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * One refresh token that was issued, stored only as a hash.
 *
 * <p>Rows are revoked, never deleted. Deleting a rotated token would make it indistinguishable from
 * a token that never existed, and that distinction is the whole of replay detection: presenting a
 * token that was already rotated is evidence the value leaked, while presenting an unknown value is
 * just a bad request.
 *
 * <p>{@code userId} is a plain column rather than a {@code @ManyToOne} association. Verifying a
 * refresh token needs the owner's identifier and nothing else about them, and an association would
 * load the whole user row on every refresh. The foreign key and its cascade still live in the
 * migration, so referential integrity is the database's job either way.
 */
@Entity
@Table(name = "refresh_tokens", schema = "wealth")
public class RefreshToken {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;

    @Column(name = "token_hash", nullable = false, updatable = false, unique = true)
    private String tokenHash;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_reason")
    private String revokedReason;

    /** Required by JPA. Not for application use. */
    protected RefreshToken() {}

    private RefreshToken(
            UUID userId, UUID sessionId, String tokenHash, Instant issuedAt, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.sessionId = sessionId;
        this.tokenHash = tokenHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    public static RefreshToken issue(
            UUID userId, UUID sessionId, String tokenHash, Instant issuedAt, Instant expiresAt) {
        return new RefreshToken(userId, sessionId, tokenHash, issuedAt, expiresAt);
    }

    /**
     * Records why this token stopped being usable. Revoking an already-revoked token keeps the first
     * reason, so a chain revoked as {@code replayed} is not overwritten by a later {@code logged-out}
     * and the security-relevant cause survives.
     *
     * <p>Public because {@code RefreshTokenService} owns the rotation rules from the sibling {@code
     * session} package. It is slice-internal all the same: nothing outside {@code
     * com.rikkaus.wealth.identity} should reach a token entity, and the ArchUnit slice rule enforces
     * that no other feature can.
     */
    public void revoke(RevocationReason reason, Instant when) {
        if (revokedAt != null) {
            return;
        }
        this.revokedAt = when;
        this.revokedReason = reason.wireValue();
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean hasExpiredAt(Instant when) {
        return !when.isBefore(expiresAt);
    }

    /** True when the token can still be exchanged: neither revoked nor past its expiry. */
    public boolean isUsableAt(Instant when) {
        return !isRevoked() && !hasExpiredAt(when);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public String getRevokedReason() {
        return revokedReason;
    }

    /** Why a token was revoked. An enum so the three values cannot drift into free text. */
    public enum RevocationReason {
        /** Exchanged for a successor in the same chain. The ordinary path. */
        ROTATED("rotated"),
        /** The user signed out. */
        LOGGED_OUT("logged-out"),
        /** An already-rotated token was presented again, so the whole chain was revoked. */
        REPLAYED("replayed");

        private final String wireValue;

        RevocationReason(String wireValue) {
            this.wireValue = wireValue;
        }

        public String wireValue() {
            return wireValue;
        }
    }
}
