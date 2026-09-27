package com.rikkaus.wealth.support;

import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for every integration test that needs a real PostgreSQL.
 *
 * <p>The container is started once in a static initializer and is deliberately never stopped, and
 * the class carries no {@code @Testcontainers} annotation. That extension registers each container
 * in the per-test-class JUnit store and closes it at that class's teardown, while Spring's
 * TestContext framework caches one {@code ApplicationContext} across every subclass with identical
 * configuration. The cached {@code DataSource} would then hold the first container's mapped port, so
 * the first integration test would pass and every later one would fail with connection refused.
 * Letting Docker reap the container when the JVM exits avoids that entirely.
 *
 * <p>The image tag is pinned to the tag in {@code docker-compose.yml} so tests and local
 * development cannot drift onto different PostgreSQL versions.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
public abstract class AbstractPostgresIntegrationTest {

    @ServiceConnection
    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18.6-alpine3.24");

    static {
        POSTGRES.start();
    }
}
