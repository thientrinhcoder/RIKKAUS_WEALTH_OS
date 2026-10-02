package com.rikkaus.wealth.shared.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.rikkaus.wealth.support.AbstractPostgresIntegrationTest;
import com.rikkaus.wealth.support.AuthenticatedClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.ObjectMapper;

/**
 * Asserts against a real running server that the header and the body carry one identifier.
 *
 * <p>That single fact is the point: it is what closes the debug loop, letting a reported
 * {@code X-Correlation-Id} be found in the logs for the request that produced the body.
 */
class CorrelationIdContractIT extends AbstractPostgresIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private AuthenticatedClient authenticatedClient;

    @Autowired private ObjectMapper objectMapper;

    @Test
    void aSuccessfulResponseCarriesAGeneratedIdentifier() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/meta", String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(CorrelationId.isAcceptable(response.getHeaders().getFirst(CorrelationId.HEADER)))
                .isTrue();
    }

    @Test
    void anErrorCarriesTheSameIdentifierInTheHeaderAndTheBody() {
        // Authenticated so this stays a 404 from the exception advice. Unauthenticated it would be a 401
        // written by the security entry point, which also carries the member — the test would still pass
        // while quietly covering a different code path. SecurityConfigurationIT covers that one.
        ResponseEntity<String> response =
                restTemplate.exchange(
                        "/api/v1/no-such-resource",
                        HttpMethod.GET,
                        authenticatedClient.newUserBearerEntity(),
                        String.class);

        String fromHeader = response.getHeaders().getFirst(CorrelationId.HEADER);
        assertThat(fromHeader).isNotNull();

        var body = objectMapper.readTree(response.getBody());
        assertThat(body.has(CorrelationId.MDC_KEY))
                .as("the problem body lost its correlation member, so an error cannot be traced")
                .isTrue();
        assertThat(body.get(CorrelationId.MDC_KEY).asString()).isEqualTo(fromHeader);
    }

    @Test
    void anInboundIdentifierIsEchoedOnBothTheHeaderAndTheProblemBody() {
        HttpHeaders headers =
                authenticatedClient.bearerHeadersFor(authenticatedClient.signInNewUser());
        headers.set(CorrelationId.HEADER, "abcdef12-3456-7890");

        ResponseEntity<String> response =
                restTemplate.exchange(
                        "/api/v1/no-such-resource",
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        String.class);

        assertThat(response.getHeaders().getFirst(CorrelationId.HEADER))
                .isEqualTo("abcdef12-3456-7890");
        assertThat(objectMapper.readTree(response.getBody()).get(CorrelationId.MDC_KEY).asString())
                .isEqualTo("abcdef12-3456-7890");
    }

    @Test
    void aMalformedInboundIdentifierIsReplacedAndTheRequestStillSucceeds() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(CorrelationId.HEADER, "short");

        ResponseEntity<String> response =
                restTemplate.exchange(
                        "/api/v1/meta", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("rejecting a malformed identifier must not fail the caller's request")
                .isTrue();
        assertThat(response.getHeaders().getFirst(CorrelationId.HEADER)).isNotEqualTo("short");
    }

    @Test
    void anErrorResponseIsExactlyOneJsonDocument() {
        ResponseEntity<String> response =
                restTemplate.exchange(
                        "/api/v1/no-such-resource",
                        HttpMethod.GET,
                        authenticatedClient.newUserBearerEntity(),
                        String.class);

        // A second body appended to a partially written one is the failure mode ProblemDetailWriter's
        // isCommitted guard exists to prevent, and a client would report it as malformed data rather
        // than as a server error. Strict parsing fails on trailing content, so one document is proof.
        try (var parser = objectMapper.createParser(response.getBody())) {
            var tree = objectMapper.readTree(parser);
            assertThat(tree.isObject()).isTrue();
            assertThat(parser.nextToken())
                    .as("the response carried content after the first JSON document")
                    .isNull();
        }
    }
}
