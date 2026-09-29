package com.rikkaus.wealth.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.rikkaus.wealth.support.AuthenticatedClient;
import com.rikkaus.wealth.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * The permit list and the shape of a denial, against the assembled application.
 *
 * <p>Every permitted route is asserted individually rather than as a set, so widening the list requires
 * editing a test and stating which route became public and why. That is the only real protection against
 * a domain route being quietly added to {@code permitAll}.
 */
class SecurityConfigurationIT extends AbstractPostgresIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private AuthenticatedClient authenticatedClient;

    @Autowired private ObjectMapper objectMapper;

    @Test
    void theMetaEndpointStaysPublic() {
        // It reports the build version and nothing about any user. The frontend calls it before sign-in.
        assertThat(restTemplate.getForEntity("/api/v1/meta", String.class).getStatusCode().value())
                .isEqualTo(200);
    }

    @Test
    void theHealthEndpointStaysPublic() {
        // A liveness probe cannot hold a credential.
        assertThat(restTemplate.getForEntity("/actuator/health", String.class).getStatusCode().value())
                .isEqualTo(200);
    }

    @Test
    void theGoogleSignInRouteIsReachableWithoutAToken() {
        // Reachable, not successful: an empty body is a 400 from validation, which proves authorization
        // let the request through to the controller rather than rejecting it at the chain.
        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/v1/auth/google", emptyJsonBody(), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertProblem(response, 400, "validation-failed");
    }

    @Test
    void theRenewalRouteIsReachableWithoutAToken() {
        // The route whose entire purpose is recovering from an expired access token. If this ever
        // required one, the "expired session" state in the accepted identity design would be a dead end.
        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/v1/auth/refresh", emptyJsonBody(), String.class);

        assertProblem(response, 400, "validation-failed");
    }

    @Test
    void theSignOutRouteIsReachableWithoutAToken() {
        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/v1/auth/logout", emptyJsonBody(), String.class);

        assertProblem(response, 400, "validation-failed");
    }

    @Test
    void aProtectedRouteWithoutATokenIsAConformingUnauthorized() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/v1/auth/session", String.class);

        // This is the point at which urn:rikkaus:problem:unauthorized stops being a reserved code. The
        // body must be a problem document like every other error, not Spring Security's own output.
        assertProblem(response, 401, "unauthorized");
    }

    @Test
    void anUnauthorizedBodyCarriesTheCorrelationIdentifier() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Correlation-Id", "security-it-0001");

        ResponseEntity<String> response =
                restTemplate.exchange(
                        "/api/v1/auth/session", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        // The entry point runs inside the filter chain, where an exception never reaches the exception
        // handler. Without ProblemDetailWriter this member would be missing on exactly the errors a
        // client most needs to report.
        assertThat(response.getHeaders().getFirst("X-Correlation-Id")).isEqualTo("security-it-0001");
        assertThat(bodyOf(response).get("correlationId").asString()).isEqualTo("security-it-0001");
    }

    @Test
    void anUnknownPathIsUnauthorizedRatherThanNotFoundWhenAnonymous() {
        // A deliberate change from the pre-authentication contract, where this returned 404. An
        // unauthenticated caller must not be able to map which paths exist.
        assertProblem(restTemplate.getForEntity("/api/v1/no-such-resource", String.class), 401, "unauthorized");
    }

    @Test
    void anUnknownPathIsStillNotFoundForAnAuthenticatedCaller() {
        // The original intent of the assertion this replaces: a real 404 still looks like a 404 once the
        // caller has proved who they are.
        ResponseEntity<String> response =
                restTemplate.exchange(
                        "/api/v1/no-such-resource",
                        HttpMethod.GET,
                        authenticatedClient.newUserBearerEntity(),
                        String.class);

        assertProblem(response, 404, "not-found");
    }

    @Test
    void aMalformedBearerTokenIsRejectedRatherThanTreatedAsAnonymous() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("this-is-not-a-jwt");

        ResponseEntity<String> response =
                restTemplate.exchange(
                        "/api/v1/meta", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        // Sent to a *public* route on purpose. Treating a bad token as absent would return 200 here and
        // hide a rotated signing key or a truncated header until it broke something else.
        assertProblem(response, 401, "unauthorized");
    }

    @Test
    void anAuthenticatedCallerReachesTheProtectedRoute() {
        ResponseEntity<String> response =
                restTemplate.exchange(
                        "/api/v1/auth/session",
                        HttpMethod.GET,
                        authenticatedClient.newUserBearerEntity(),
                        String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(bodyOf(response).get("email").asString()).contains("@example.com");
    }

    @Test
    void aBrowserPreflightIsNotBlockedByTheChain() {
        // Spring Security runs before MVC, so without .cors() the preflight — which carries no credential
        // — would be refused and the whole CORS allowlist would be dead in a browser.
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ORIGIN, "http://localhost:8081");
        headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST");

        ResponseEntity<String> response =
                restTemplate.exchange(
                        "/api/v1/auth/google", HttpMethod.OPTIONS, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("preflight was rejected: %s", response.getStatusCode())
                .isTrue();
    }

    private static HttpEntity<String> emptyJsonBody() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>("{}", headers);
    }

    private void assertProblem(ResponseEntity<String> response, int status, String code) {
        assertThat(response.getStatusCode().value()).isEqualTo(status);
        assertThat(response.getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        JsonNode body = bodyOf(response);
        assertThat(body.get("type").asString()).isEqualTo("urn:rikkaus:problem:" + code);
        assertThat(body.get("status").asInt()).isEqualTo(status);
    }

    private JsonNode bodyOf(ResponseEntity<String> response) {
        return objectMapper.readTree(response.getBody());
    }
}
