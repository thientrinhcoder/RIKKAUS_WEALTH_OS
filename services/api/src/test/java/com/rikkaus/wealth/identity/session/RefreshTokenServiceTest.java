package com.rikkaus.wealth.identity.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rikkaus.wealth.identity.domain.RefreshToken;
import com.rikkaus.wealth.identity.domain.RefreshToken.RevocationReason;
import com.rikkaus.wealth.identity.domain.RefreshTokenRepository;
import com.rikkaus.wealth.identity.session.RefreshTokenService.Issued;
import com.rikkaus.wealth.identity.session.RefreshTokenService.RefreshTokenRejectedException;
import java.time.Clock;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Rotation, replay detection and revocation, against an in-memory repository.
 *
 * <p>A hand-written fake rather than Mockito: every test here depends on the repository actually
 * remembering what was saved, because rotation is a read-then-write cycle. Stubbing
 * {@code findByTokenHash} per call would encode the expected sequence into the test and stop it from
 * catching the bug that matters — a token that stays usable after being exchanged.
 */
class RefreshTokenServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-09-27T14:36:00Z");
    private static final Duration REFRESH_TTL = Duration.ofDays(30);

    private final InMemoryRefreshTokenRepository repository = new InMemoryRefreshTokenRepository();
    private final MutableClock clock = new MutableClock(FIXED_NOW);
    private final RefreshTokenService service =
            new RefreshTokenService(
                    repository,
                    clock,
                    new SessionProperties(
                            new SessionProperties.Jwt(
                                    "a-test-signing-secret-of-sufficient-length", Duration.ofMinutes(15)),
                            REFRESH_TTL),
                    // A no-op manager. The in-memory repository has no transaction semantics to model, so
                    // these tests cannot prove that the replay revocation survives the caller's rollback —
                    // that is exactly the gap AuthenticationFlowIT closes, and it is the bug that made this
                    // manager necessary in the first place.
                    new org.springframework.transaction.support.AbstractPlatformTransactionManager() {
                        @Override
                        protected Object doGetTransaction() {
                            return new Object();
                        }

                        @Override
                        protected void doBegin(Object transaction, TransactionDefinition definition) {}

                        @Override
                        protected void doCommit(DefaultTransactionStatus status) {}

                        @Override
                        protected void doRollback(DefaultTransactionStatus status) {}
                    });

    @Test
    void anIssuedTokenIsStoredOnlyAsAHash() {
        Issued issued = service.issueNewChain(UUID.randomUUID());

        assertThat(repository.saved).hasSize(1);
        assertThat(repository.saved.getFirst().getId()).isNotNull();
        // The stored value must not be the token, and must be the token's hash. Asserting both
        // directions is what makes this a real check rather than a restatement of the implementation.
        assertThat(repository.findByTokenHash(issued.rawToken())).isEmpty();
        assertThat(repository.findByTokenHash(RefreshTokenService.hash(issued.rawToken()))).isPresent();
    }

    @Test
    void rotationReturnsANewTokenAndKillsThePresentedOne() throws Exception {
        UUID userId = UUID.randomUUID();
        Issued first = service.issueNewChain(userId);

        Issued second = service.rotate(first.rawToken());

        assertThat(second.rawToken()).isNotEqualTo(first.rawToken());
        assertThat(second.userId()).isEqualTo(userId);
        // Same chain, so a later replay can still be traced to every descendant.
        assertThat(second.sessionId()).isEqualTo(first.sessionId());
        assertThat(tokenFor(first).getRevokedReason()).isEqualTo(RevocationReason.ROTATED.wireValue());
    }

    @Test
    void thePresentedTokenCannotBeUsedTwice() throws Exception {
        Issued first = service.issueNewChain(UUID.randomUUID());
        service.rotate(first.rawToken());

        assertThatThrownBy(() -> service.rotate(first.rawToken()))
                .isInstanceOf(RefreshTokenRejectedException.class);
    }

    @Test
    void replayingARotatedTokenRevokesEveryTokenInTheChain() throws Exception {
        // The property that actually defends a hijacked session: by the time a rotated token is
        // replayed, the successor may already be in someone else's hands, so it must die too.
        Issued first = service.issueNewChain(UUID.randomUUID());
        Issued second = service.rotate(first.rawToken());

        assertThatThrownBy(() -> service.rotate(first.rawToken()))
                .isInstanceOf(RefreshTokenRejectedException.class);

        assertThat(tokenFor(second).isRevoked()).isTrue();
        assertThat(tokenFor(second).getRevokedReason()).isEqualTo(RevocationReason.REPLAYED.wireValue());
        assertThatThrownBy(() -> service.rotate(second.rawToken()))
                .isInstanceOf(RefreshTokenRejectedException.class);
    }

    @Test
    void replayRevocationKeepsItsReasonWhenTheChainIsLaterSignedOut() throws Exception {
        Issued first = service.issueNewChain(UUID.randomUUID());
        Issued second = service.rotate(first.rawToken());
        assertThatThrownBy(() -> service.rotate(first.rawToken()))
                .isInstanceOf(RefreshTokenRejectedException.class);

        service.revokeChainOf(second.rawToken());

        // A security-relevant cause must not be overwritten by a routine one, or the evidence that a
        // token leaked disappears from the record.
        assertThat(tokenFor(second).getRevokedReason()).isEqualTo(RevocationReason.REPLAYED.wireValue());
    }

    @Test
    void anUnknownTokenIsRejected() {
        assertThatThrownBy(() -> service.rotate("a-token-that-was-never-issued"))
                .isInstanceOf(RefreshTokenRejectedException.class);
    }

    @Test
    void anExpiredTokenIsRejected() {
        Issued issued = service.issueNewChain(UUID.randomUUID());

        clock.advanceBy(REFRESH_TTL.plusSeconds(1));

        assertThatThrownBy(() -> service.rotate(issued.rawToken()))
                .isInstanceOf(RefreshTokenRejectedException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void aTokenOneSecondBeforeExpiryStillRotates() throws Exception {
        Issued issued = service.issueNewChain(UUID.randomUUID());

        clock.advanceBy(REFRESH_TTL.minusSeconds(1));

        assertThat(service.rotate(issued.rawToken())).isNotNull();
    }

    @Test
    void signingOutRevokesTheWholeChain() throws Exception {
        Issued first = service.issueNewChain(UUID.randomUUID());
        Issued second = service.rotate(first.rawToken());

        service.revokeChainOf(second.rawToken());

        assertThat(tokenFor(second).getRevokedReason())
                .isEqualTo(RevocationReason.LOGGED_OUT.wireValue());
        assertThatThrownBy(() -> service.rotate(second.rawToken()))
                .isInstanceOf(RefreshTokenRejectedException.class);
    }

    @Test
    void signingOutTwiceIsNotAnError() {
        Issued issued = service.issueNewChain(UUID.randomUUID());

        service.revokeChainOf(issued.rawToken());

        // Not an exception: a second sign-out is a normal client retry, and an endpoint that failed
        // here would also answer "is this token live?" for anyone holding a guess.
        service.revokeChainOf(issued.rawToken());
        service.revokeChainOf("a-token-that-was-never-issued");
    }

    @Test
    void twoTokensIssuedInARowAreDifferent() {
        // Guards against a refactor that reuses a buffer or seeds the generator per call.
        Issued first = service.issueNewChain(UUID.randomUUID());
        Issued second = service.issueNewChain(UUID.randomUUID());

        assertThat(first.rawToken()).isNotEqualTo(second.rawToken());
        assertThat(first.sessionId()).isNotEqualTo(second.sessionId());
    }

    private RefreshToken tokenFor(Issued issued) {
        return repository.findByTokenHash(RefreshTokenService.hash(issued.rawToken())).orElseThrow();
    }

    /**
     * Remembers what was saved, which is what rotation semantics need to be provable.
     *
     * <p>Four methods, because {@link RefreshTokenRepository} declares four. That is the payoff of the
     * repository not extending {@code JpaRepository}.
     */
    private static final class InMemoryRefreshTokenRepository implements RefreshTokenRepository {

        private final List<RefreshToken> saved = new ArrayList<>();

        @Override
        public Optional<RefreshToken> findByTokenHash(String tokenHash) {
            return saved.stream().filter(token -> hashOf(token).equals(tokenHash)).findFirst();
        }

        @Override
        public List<RefreshToken> findBySessionId(UUID sessionId) {
            return saved.stream().filter(token -> token.getSessionId().equals(sessionId)).toList();
        }

        @Override
        public RefreshToken save(RefreshToken token) {
            saved.add(token);
            return token;
        }

        @Override
        public List<RefreshToken> saveAll(Iterable<RefreshToken> tokens) {
            // The instances are already in `saved`; mutating them is visible without re-adding.
            List<RefreshToken> all = new ArrayList<>();
            tokens.forEach(all::add);
            return all;
        }

        /**
         * Reads the private hash field. The entity deliberately exposes no getter for it: nothing in
         * production has a reason to read a stored token hash back out, and adding one to satisfy a
         * test would widen a security-sensitive class for no production benefit.
         */
        private static String hashOf(RefreshToken token) {
            try {
                var field = RefreshToken.class.getDeclaredField("tokenHash");
                field.setAccessible(true);
                return (String) field.get(token);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("RefreshToken.tokenHash was renamed", e);
            }
        }
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
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
