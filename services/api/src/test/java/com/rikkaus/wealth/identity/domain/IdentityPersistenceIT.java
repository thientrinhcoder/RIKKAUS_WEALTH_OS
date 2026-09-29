package com.rikkaus.wealth.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rikkaus.wealth.support.AbstractPostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.exception.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * Proves the identity entities and {@code V2__identity.sql} agree, against a real PostgreSQL.
 *
 * <p>The unit tests cover rotation rules with an in-memory repository, which by construction cannot
 * catch a column name that does not exist, a nullability mismatch, or a constraint that was never
 * created. Context refresh alone catches part of it, because {@code ddl-auto: validate} compares the
 * mapping to the live schema; the assertions below cover what validation does not check —
 * that the constraints actually behave.
 */
class IdentityPersistenceIT extends AbstractPostgresIntegrationTest {

    @Autowired private UserAccountRepository users;

    @Autowired private RefreshTokenRepository refreshTokens;

    @Autowired private DataSource dataSource;

    @PersistenceContext private EntityManager entityManager;

    @Test
    @Transactional
    void aUserRoundTripsThroughTheRealSchema() {
        Instant now = Instant.parse("2026-09-27T14:36:00Z");
        UserAccount saved =
                users.save(
                        UserAccount.createFromGoogle(
                                "google-subject-" + UUID.randomUUID(), "an.nguyen@example.com", "An Nguyễn", now));

        UserAccount found = users.findById(saved.getId()).orElseThrow();

        assertThat(found.getEmail()).isEqualTo("an.nguyen@example.com");
        // A non-ASCII display name is the realistic case for this product, and it is the one that
        // exposes a client or column encoding mismatch.
        assertThat(found.getDisplayName()).isEqualTo("An Nguyễn");
        assertThat(found.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @Transactional
    void twoAccountsCannotShareAGoogleSubject() {
        Instant now = Instant.parse("2026-09-27T14:36:00Z");
        String subject = "google-subject-" + UUID.randomUUID();
        users.save(UserAccount.createFromGoogle(subject, "first@example.com", "First", now));

        // The constraint that makes the Google subject an identity rather than an attribute. Without
        // it a race between two concurrent first logins would create two accounts for one person.
        assertThatThrownBy(
                        () ->
                                flush(
                                        () ->
                                                users.save(
                                                        UserAccount.createFromGoogle(
                                                                subject, "second@example.com", "Second", now))))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @Transactional
    void twoAccountsMayShareAnEmailAddress() {
        // Asserted, not merely permitted: V2__identity.sql leaves email non-unique on purpose so a
        // legitimate Google email change cannot fail a sign-in, and a future migration that "tidies" a
        // unique index onto the column must fail here rather than in production.
        Instant now = Instant.parse("2026-09-27T14:36:00Z");
        String email = "shared@example.com";
        users.save(
                UserAccount.createFromGoogle("google-subject-" + UUID.randomUUID(), email, "One", now));

        UserAccount second =
                users.save(
                        UserAccount.createFromGoogle("google-subject-" + UUID.randomUUID(), email, "Two", now));

        assertThat(users.findById(second.getId())).isPresent();
    }

    @Test
    @Transactional
    void aRefreshTokenRoundTripsAndIsFoundByItsHash() {
        Instant now = Instant.parse("2026-09-27T14:36:00Z");
        UserAccount user =
                users.save(
                        UserAccount.createFromGoogle(
                                "google-subject-" + UUID.randomUUID(), "an.nguyen@example.com", "An Nguyễn", now));
        UUID sessionId = UUID.randomUUID();
        String hash = "0".repeat(63) + "1";

        refreshTokens.save(
                RefreshToken.issue(user.getId(), sessionId, hash, now, now.plusSeconds(3600)));

        RefreshToken found = refreshTokens.findByTokenHash(hash).orElseThrow();
        assertThat(found.getUserId()).isEqualTo(user.getId());
        assertThat(found.getSessionId()).isEqualTo(sessionId);
        assertThat(found.isRevoked()).isFalse();
        assertThat(refreshTokens.findBySessionId(sessionId)).hasSize(1);
    }

    @Test
    @Transactional
    void aTokenCannotReferenceAUserThatDoesNotExist() {
        Instant now = Instant.parse("2026-09-27T14:36:00Z");

        // The foreign key exists only in the migration, because the entity maps userId as a plain
        // column rather than an association. This is the test that proves it is really there.
        assertThatThrownBy(
                        () ->
                                flush(
                                        () ->
                                                refreshTokens.save(
                                                        RefreshToken.issue(
                                                                UUID.randomUUID(),
                                                                UUID.randomUUID(),
                                                                "f".repeat(64),
                                                                now,
                                                                now.plusSeconds(3600)))))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void deletingAUserRemovesTheirTokens() {
        // Not @Transactional: the cascade is a database behaviour, so it is exercised through committed
        // SQL rather than through a rolled-back persistence context. The rows are cleaned up by the
        // delete itself, so this leaves nothing behind for the other tests.
        Instant now = Instant.parse("2026-09-27T14:36:00Z");
        UserAccount user =
                users.save(
                        UserAccount.createFromGoogle(
                                "google-subject-" + UUID.randomUUID(), "cascade@example.com", "Cascade", now));
        UUID sessionId = UUID.randomUUID();
        refreshTokens.save(
                RefreshToken.issue(user.getId(), sessionId, "a".repeat(64), now, now.plusSeconds(3600)));

        JdbcClient jdbc = JdbcClient.create(dataSource);
        jdbc.sql("DELETE FROM wealth.users WHERE id = ?").param(user.getId()).update();

        Long remaining =
                jdbc.sql("SELECT count(*) FROM wealth.refresh_tokens WHERE session_id = ?")
                        .param(sessionId)
                        .query(Long.class)
                        .single();
        assertThat(remaining).isZero();
    }

    /**
     * Runs an action and forces the flush, so a constraint violation surfaces inside the assertion
     * rather than at an unrelated commit later. Spring Data's {@code save} only schedules the insert
     * while a transaction is open, so without this a violated constraint would go unnoticed here.
     *
     * <p>Flushing the {@code EntityManager} directly raises Hibernate's {@code
     * ConstraintViolationException} rather than Spring's {@code DataIntegrityViolationException},
     * because exception translation happens at the repository proxy boundary and this call goes around
     * it. Asserting the type that is actually thrown keeps the test honest about what it exercises.
     */
    private void flush(Runnable action) {
        action.run();
        entityManager.flush();
    }
}
