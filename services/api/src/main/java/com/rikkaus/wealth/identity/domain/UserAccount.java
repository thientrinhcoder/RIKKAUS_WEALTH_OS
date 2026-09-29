package com.rikkaus.wealth.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A pilot user, identified by their Google Account.
 *
 * <p>There is no password field. Identity comes from Google's verified {@code sub} claim, per the
 * Product Owner decision recorded in the accepted design for issue #40; {@code V2__identity.sql}
 * carries the same reasoning at the schema level.
 *
 * <p>The identifier is assigned in application code rather than by the database, because a user is
 * created in the same unit of work as their first refresh token and the token needs the user id
 * before either row is flushed. A database sequence would force an extra round trip to learn it.
 */
@Entity
@Table(name = "users", schema = "wealth")
public class UserAccount {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "google_subject", nullable = false, updatable = false, unique = true)
    private String googleSubject;

    @Column(nullable = false)
    private String email;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Required by JPA. Not for application use. */
    protected UserAccount() {}

    private UserAccount(
            UUID id, String googleSubject, String email, String displayName, Instant now) {
        this.id = id;
        this.googleSubject = googleSubject;
        this.email = email;
        this.displayName = displayName;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static UserAccount createFromGoogle(
            String googleSubject, String email, String displayName, Instant now) {
        return new UserAccount(UUID.randomUUID(), googleSubject, email, displayName, now);
    }

    /**
     * Applies the current values from a fresh Google identity token.
     *
     * <p>The subject is deliberately not updatable: it is the identity key, and a row whose subject
     * changed would be a different person wearing the same record.
     */
    public void refreshProfileFromGoogle(String email, String displayName, Instant now) {
        this.email = email;
        this.displayName = displayName;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getGoogleSubject() {
        return googleSubject;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
