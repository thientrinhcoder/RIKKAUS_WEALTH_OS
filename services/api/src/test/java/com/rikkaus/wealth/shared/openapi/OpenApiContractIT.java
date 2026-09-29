package com.rikkaus.wealth.shared.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.rikkaus.wealth.support.AbstractPostgresIntegrationTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Publishes the contract and enforces that it matches the running application.
 *
 * <p>A test rather than a Maven plugin. The springdoc Maven plugin predates Boot 4 and boots the
 * packaged application out of process, which needs a reachable PostgreSQL it has no lifecycle hook to
 * provide. A {@code *IT} extending the shared base class already has that database.
 *
 * <p>The property is set locally rather than by activating the {@code local} profile, so the dependency
 * is stated here and the rest of that profile is not dragged in.
 *
 * <p>{@code webEnvironment} has to be repeated. Declaring {@code @SpringBootTest} here replaces the
 * base class's annotation rather than merging with it, so omitting it silently reverts to the mock
 * environment and every test fails with "No local test web server available".
 */
@SpringBootTest(
        properties = "springdoc.api-docs.enabled=true",
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiContractIT extends AbstractPostgresIntegrationTest {

    private static final Path CONTRACT = Path.of("openapi", "openapi.json");

    /** Canonicalizing mapper. Sorting keys is what makes the comparison stable. */
    private static final ObjectMapper CANONICAL =
            JsonMapper.builder()
                    .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .build();

    @Autowired private TestRestTemplate restTemplate;

    @BeforeEach
    void contractPathResolvesInsideThisModule() {
        // Failsafe's working directory is the module directory. Asserted rather than trusted, so a
        // future build change surfaces here instead of as a file mysteriously written to the repo root.
        assertThat(CONTRACT.toAbsolutePath().getParent().getParent())
                .as("the contract path must resolve inside services/api")
                .hasFileName("api");
    }

    @Test
    void publishesOnlyTheProductionApiSurface() {
        JsonNode paths = servedDocument().get("paths");

        assertThat(paths.propertyNames())
                .as(
                        "A non-production path leaked into the contract. A test controller inside the "
                                + "component-scan root is the usual cause.")
                // In any order, not containsExactly: the served document's key order is springdoc's own,
                // and the canonicalizing mapper sorts keys on the way out rather than in the parsed tree.
                // The set is still exact, which is what catches a leaked path.
                .containsExactlyInAnyOrder(
                        "/api/v1/auth/google",
                        "/api/v1/auth/logout",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/session",
                        "/api/v1/meta");
    }

    @Test
    void publishedContractMatchesTheRunningApplication() throws IOException {
        String generated = canonicalize(servedDocument());

        if (Boolean.getBoolean("openapi.update")) {
            Files.createDirectories(CONTRACT.getParent());
            Files.writeString(CONTRACT, generated);
            return;
        }

        assertThat(CONTRACT)
                .as(
                        "Committed OpenAPI contract is missing. Run "
                                + "./services/api/mvnw -f services/api/pom.xml verify -Dopenapi.update=true "
                                + "and review the diff.")
                .exists();
        assertThat(canonicalize(CANONICAL.readTree(Files.readString(CONTRACT))))
                .as(
                        "The API changed but %s was not regenerated. Run "
                                + "./services/api/mvnw -f services/api/pom.xml verify -Dopenapi.update=true, "
                                + "review the diff, and notify the frontend task before merging.",
                        CONTRACT)
                .isEqualTo(generated);
    }

    @Test
    void carriesNoValueThatChangesBetweenRuns() {
        JsonNode document = servedDocument();

        assertThat(document.get("servers").get(0).get("url").asString())
                .as("an absolute server URL embeds this run's ephemeral port and breaks drift detection")
                .isEqualTo("/");
        assertThat(canonicalize(document))
                .doesNotContain("localhost")
                .doesNotContain(String.valueOf(POSTGRES.getMappedPort(5432)));
    }

    @Test
    void documentsTheErrorContractTheFrontendConsumes() {
        JsonNode problem =
                servedDocument().get("components").get("schemas").get("ProblemDetail").get("properties");

        assertThat(problem.propertyNames())
                .contains("type", "title", "status", "detail", "instance", "correlationId", "errors");
    }

    private JsonNode servedDocument() {
        return CANONICAL.readTree(restTemplate.getForObject("/v3/api-docs", String.class));
    }

    private static String canonicalize(JsonNode document) {
        return CANONICAL.writeValueAsString(document) + System.lineSeparator();
    }
}
