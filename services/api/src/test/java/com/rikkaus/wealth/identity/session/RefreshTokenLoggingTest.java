package com.rikkaus.wealth.identity.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.rikkaus.wealth.identity.domain.RefreshToken;
import com.rikkaus.wealth.identity.domain.RefreshTokenRepository;
import com.rikkaus.wealth.identity.session.RefreshTokenService.Issued;
import com.rikkaus.wealth.identity.session.RefreshTokenService.RefreshTokenRejectedException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

/**
 * Asserts the one thing a code review is worst at catching: that no secret reaches a log.
 *
 * <p>{@code ARCHITECTURE_TECHNOLOGY_DECISIONS.md} forbids logging passwords, tokens and financial payloads,
 * and issue #42 makes it an acceptance criterion. Grepping for {@code log.} proves it for today's code and
 * for nobody's future edit; this fails the build instead.
 *
 * <p>The replay path is the one tested because it is where the temptation is strongest — a developer
 * investigating a stolen token wants the offending value in the log, and that value is a live credential.
 */
class RefreshTokenLoggingTest {

    private static final Instant NOW = Instant.parse("2026-09-27T14:36:00Z");

    private final InMemoryRepository repository = new InMemoryRepository();
    private final RefreshTokenService service =
            new RefreshTokenService(
                    repository,
                    Clock.fixed(NOW, ZoneOffset.UTC),
                    new SessionProperties(
                            new SessionProperties.Jwt(
                                    "a-test-signing-secret-of-sufficient-length", Duration.ofMinutes(15)),
                            Duration.ofDays(30)),
                    new NoOpTransactionManager());

    private Logger serviceLogger;
    private ListAppender<ILoggingEvent> captured;

    @BeforeEach
    void captureLogs() {
        serviceLogger = (Logger) LoggerFactory.getLogger(RefreshTokenService.class);
        captured = new ListAppender<>();
        captured.start();
        serviceLogger.addAppender(captured);
        serviceLogger.setLevel(Level.TRACE);
    }

    @AfterEach
    void releaseLogs() {
        serviceLogger.detachAppender(captured);
        captured.stop();
    }

    @Test
    void replayDetectionLogsTheSessionButNeverTheToken() throws Exception {
        Issued first = service.issueNewChain(UUID.randomUUID());
        service.rotate(first.rawToken());

        assertThatThrownBy(() -> service.rotate(first.rawToken()))
                .isInstanceOf(RefreshTokenRejectedException.class);

        String logged = renderedLog();
        assertThat(logged)
                .as("a replay must be visible in the log at all, or it cannot be investigated")
                .contains(first.sessionId().toString());
        assertThat(logged)
                .as("the raw refresh token is a live credential and must never be logged")
                .doesNotContain(first.rawToken());
        assertThat(logged)
                .as("the stored hash must not be logged either; it is enough to look a token up by")
                .doesNotContain(RefreshTokenService.hash(first.rawToken()));
    }

    @Test
    void theOrdinaryRotationPathLogsNothingAtAll() throws Exception {
        Issued first = service.issueNewChain(UUID.randomUUID());

        service.rotate(first.rawToken());

        // Rotation happens on every access-token expiry. A log line per rotation would be noise, and the
        // temptation to make it useful by including the token is exactly what this guards.
        assertThat(captured.list).isEmpty();
    }

    private String renderedLog() {
        return captured.list.stream()
                .map(event -> event.getFormattedMessage())
                .reduce("", (a, b) -> a + "\n" + b);
    }

    /** Four methods, because the repository declares four. */
    private static final class InMemoryRepository implements RefreshTokenRepository {

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
            List<RefreshToken> all = new ArrayList<>();
            tokens.forEach(all::add);
            return all;
        }

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

    private static final class NoOpTransactionManager extends AbstractPlatformTransactionManager {
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
    }
}
