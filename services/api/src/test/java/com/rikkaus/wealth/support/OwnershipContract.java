package com.rikkaus.wealth.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.rikkaus.wealth.identity.application.IdentityService.AuthenticatedSession;
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
 * The ownership rules every user-owned resource must satisfy, as an inheritable set of tests.
 *
 * <p>This is the reusable half of issue #42's ownership deliverable. A later slice — assets, liabilities,
 * cash flow, goals — extends this class, implements the two hooks, and inherits five assertions it would
 * otherwise have to write, get subtly wrong, or skip. The one that is easiest to omit by hand and hardest
 * to notice missing is {@link #theDenialIsIndistinguishableFromAMissingRecord()}.
 *
 * <p>It extends {@link AbstractPostgresIntegrationTest} itself rather than leaving that to subclasses,
 * because Java has single inheritance and a subclass cannot extend both. Every user-owned resource needs a
 * running application and a database anyway, and the container is shared statically across the suite, so
 * this costs nothing. A subclass must still be named {@code *IT}, because that naming is what puts it in
 * the Failsafe run.
 */
public abstract class OwnershipContract extends AbstractPostgresIntegrationTest {

    @Autowired protected TestRestTemplate restTemplate;

    @Autowired protected AuthenticatedClient authenticatedClient;

    @Autowired protected ObjectMapper objectMapper;

    /**
     * Creates a record owned by the signed-in user and returns the path that reads it back.
     *
     * @param owner the session the record must belong to
     */
    protected abstract String createRecordOwnedBy(AuthenticatedSession owner);

    /**
     * A path of the same shape that no record exists at, used to prove a denial and a genuine miss are
     * identical. It must be well-formed — a path that fails to bind produces a 400 and proves nothing.
     */
    protected abstract String pathOfARecordThatDoesNotExist();

    /**
     * A body for a write attempt against the resource, as JSON.
     *
     * <p>Default is a trivial object. Override when the resource requires specific fields, so the write is
     * refused by the ownership check rather than by validation — a 400 would pass a naive "not 200" check
     * while proving nothing about ownership.
     */
    protected String writeBody() {
        return "{\"label\":\"Cập nhật không được phép\"}";
    }

    @Test
    void theOwnerCanReadTheirOwnRecord() {
        AuthenticatedSession owner = authenticatedClient.signInNewUser();
        String path = createRecordOwnedBy(owner);

        ResponseEntity<String> response = get(path, owner);

        assertThat(response.getStatusCode().value())
                .as("the owner must be able to read their own record")
                .isEqualTo(200);
    }

    @Test
    void anotherUserReadingTheRecordGetsNotFound() {
        AuthenticatedSession owner = authenticatedClient.signInNewUser();
        String path = createRecordOwnedBy(owner);
        AuthenticatedSession someoneElse = authenticatedClient.signInNewUser();

        ResponseEntity<String> response = get(path, someoneElse);

        // Not 403. A 403 confirms the record exists, which turns a list of identifiers into an enumeration
        // oracle even though none of them can be read.
        assertProblem(response, 404, "not-found");
    }

    @Test
    void anotherUserWritingToTheRecordGetsNotFound() {
        AuthenticatedSession owner = authenticatedClient.signInNewUser();
        String path = createRecordOwnedBy(owner);
        AuthenticatedSession someoneElse = authenticatedClient.signInNewUser();

        ResponseEntity<String> response =
                restTemplate.exchange(
                        path,
                        HttpMethod.PUT,
                        authenticatedClient.bearerEntityFor(someoneElse, writeBody()),
                        String.class);

        // Guarding reads and forgetting writes is the usual way this gets half-done.
        assertProblem(response, 404, "not-found");
    }

    @Test
    void anAnonymousCallerGetsUnauthorized() {
        AuthenticatedSession owner = authenticatedClient.signInNewUser();
        String path = createRecordOwnedBy(owner);

        ResponseEntity<String> response = restTemplate.getForEntity(path, String.class);

        // 401, not 404: the caller has not identified themselves, which is a different failure from asking
        // for something that is not theirs.
        assertProblem(response, 401, "unauthorized");
    }

    @Test
    void theDenialIsIndistinguishableFromAMissingRecord() {
        AuthenticatedSession owner = authenticatedClient.signInNewUser();
        String othersRecord = createRecordOwnedBy(owner);
        AuthenticatedSession someoneElse = authenticatedClient.signInNewUser();

        JsonNode denied = bodyOf(get(othersRecord, someoneElse));
        JsonNode missing = bodyOf(get(pathOfARecordThatDoesNotExist(), someoneElse));

        // The assertion that actually protects the property. Comparing status codes alone would pass while
        // `detail`, `title` or an extension member quietly revealed which of the two had happened.
        assertThat(withoutRequestSpecificMembers(denied))
                .as("a record owned by someone else must look exactly like one that does not exist")
                .isEqualTo(withoutRequestSpecificMembers(missing));
    }

    private ResponseEntity<String> get(String path, AuthenticatedSession as) {
        return restTemplate.exchange(
                path, HttpMethod.GET, authenticatedClient.bearerEntityFor(as), String.class);
    }

    /**
     * Drops the two members that legitimately differ between any two requests.
     *
     * <p>{@code instance} is the request URI and {@code correlationId} is per-request, so both differ by
     * design. Everything else must match byte for byte.
     */
    private JsonNode withoutRequestSpecificMembers(JsonNode problem) {
        return ((tools.jackson.databind.node.ObjectNode) problem.deepCopy())
                .without(java.util.List.of("instance", "correlationId"));
    }

    private void assertProblem(ResponseEntity<String> response, int status, String code) {
        assertThat(response.getStatusCode().value()).isEqualTo(status);
        assertThat(response.getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(bodyOf(response).get("type").asString()).isEqualTo("urn:rikkaus:problem:" + code);
    }

    private JsonNode bodyOf(ResponseEntity<String> response) {
        return objectMapper.readTree(response.getBody());
    }

    /** Bearer headers with a JSON content type, for a subclass that needs to build its own request. */
    protected HttpEntity<String> jsonAs(AuthenticatedSession session, String body) {
        HttpHeaders headers = authenticatedClient.bearerHeadersFor(session);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
