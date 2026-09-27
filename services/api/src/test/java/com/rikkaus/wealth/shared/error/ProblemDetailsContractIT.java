package com.rikkaus.wealth.shared.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.rikkaus.testfixtures.ProblemFixtureController;
import com.rikkaus.wealth.support.AbstractPostgresIntegrationTest;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.ObjectMapper;

/**
 * The slice tests prove the handler; this proves the assembled application.
 *
 * <p>It catches the class of failure where a filter, message converter or content-negotiation setting
 * reshapes the body in the full stack but not in a slice — which is precisely where a published
 * contract and real behaviour drift apart.
 */
@Import(ProblemFixtureController.class)
class ProblemDetailsContractIT extends AbstractPostgresIntegrationTest {

    private static final String URN_PREFIX = "urn:rikkaus:problem:";

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private ObjectMapper objectMapper;

    @Test
    void anUnknownPathUnderTheApiPrefixIsAConformingNotFound() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/v1/no-such-resource", String.class);

        assertConforming(response, 404, "not-found");
    }

    @Test
    void theWrongMethodOnARealEndpointIsAConformingMethodNotAllowed() {
        ResponseEntity<String> response =
                restTemplate.exchange("/api/v1/meta", HttpMethod.DELETE, null, String.class);

        assertConforming(response, 405, "method-not-allowed");
    }

    @Test
    void aValidationFailureCarriesThePerFieldErrorsMember() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response =
                restTemplate.postForEntity(
                        "/test-fixtures/validated",
                        new HttpEntity<>("{\"name\":\"\"}", headers),
                        String.class);

        assertConforming(response, 400, "validation-failed");
        assertThat(response.getBody())
                .contains("\"errors\"")
                .contains("\"field\":\"name\"")
                .contains("must not be blank");
    }

    @Test
    void anUnexpectedFailureLeaksNothingInTheFullStackEither() {
        ResponseEntity<String> response = restTemplate.getForEntity("/test-fixtures/boom", String.class);

        assertConforming(response, 500, "internal-error");
        assertThat(response.getBody()).doesNotContain("internal detail that must not leak");
    }

    /**
     * Closes the blind spot a hand-written schema creates.
     *
     * <p>The drift check compares the generated contract against the committed one, and both derive
     * from the same bean — so a schema claiming a member the server never sends would pass every build
     * while a consumer's parser rejected every real error response. This compares the published
     * contract against actual behaviour instead.
     */
    @Test
    void everyRequiredMemberThePublishedContractDocumentsIsActuallySent() throws Exception {
        var contract = objectMapper.readTree(Files.readString(Path.of("openapi", "openapi.json")));
        var required =
                contract.get("components").get("schemas").get("ProblemDetail").get("required");
        var realErrorBody =
                objectMapper.readTree(
                        restTemplate.getForEntity("/api/v1/no-such-resource", String.class).getBody());

        assertThat(required).isNotEmpty();
        for (var member : required) {
            assertThat(realErrorBody.has(member.asString()))
                    .as(
                            "the published contract documents '%s' as required, but the server does not "
                                    + "send it",
                            member.asString())
                    .isTrue();
        }
    }

    private void assertConforming(
            ResponseEntity<String> response, int expectedStatus, String expectedCode) {
        assertThat(response.getStatusCode().value()).isEqualTo(expectedStatus);
        assertThat(response.getHeaders().getContentType())
                .as("errors must be application/problem+json in the full stack, not only in a slice")
                .isNotNull()
                .satisfies(
                        contentType ->
                                assertThat(contentType.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                                        .isTrue());
        assertThat(response.getBody()).contains("\"" + URN_PREFIX + expectedCode + "\"");
    }
}
