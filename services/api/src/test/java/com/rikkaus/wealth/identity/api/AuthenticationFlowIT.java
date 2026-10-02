package com.rikkaus.wealth.identity.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.rikkaus.wealth.identity.google.GoogleIdentity;
import com.rikkaus.wealth.support.AbstractPostgresIntegrationTest;
import com.rikkaus.wealth.support.StubGoogleIdentityProvider;
import java.util.UUID;
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
 * The whole session lifecycle over real HTTP, against a real database.
 *
 * <p>These are the transitions the accepted identity design describes as reviewable states — first login,
 * returning user, renewal, sign-out, expired and revoked session — and the ones issue #42 lists as
 * acceptance criteria. The unit tests prove each rule in isolation; this proves they still hold once the
 * security chain, the message converters, Flyway and the transaction boundaries are all in play.
 *
 * <p>Only Google is stubbed. Everything the client would touch is real.
 */
class AuthenticationFlowIT extends AbstractPostgresIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private StubGoogleIdentityProvider googleStub;

    @Autowired private ObjectMapper objectMapper;

    @Test
    void aFirstSignInCreatesTheAccountAndReturnsAUsableSession() {
        String subject = "google-subject-" + UUID.randomUUID();
        googleStub.willReturn(new GoogleIdentity(subject, "an.nguyen@example.com", true, "An Nguyễn"));

        JsonNode session = bodyOf(signIn());

        assertThat(session.get("tokenType").asString()).isEqualTo("Bearer");
        assertThat(session.get("expiresInSeconds").asLong()).isEqualTo(900L);
        assertThat(session.get("user").get("email").asString()).isEqualTo("an.nguyen@example.com");
        assertThat(session.get("user").get("displayName").asString()).isEqualTo("An Nguyễn");
        // The user object must not carry the provider subject: it is an internal identity key, not
        // something a client has any use for.
        assertThat(session.get("user").propertyNames())
                .containsExactlyInAnyOrder("id", "email", "displayName");

        assertThat(readSessionWith(session.get("accessToken").asString()).get("email").asString())
                .isEqualTo("an.nguyen@example.com");
    }

    @Test
    void aReturningSignInResumesTheSameAccount() {
        String subject = "google-subject-" + UUID.randomUUID();
        googleStub.willReturn(new GoogleIdentity(subject, "an.nguyen@example.com", true, "An Nguyễn"));
        String firstId = bodyOf(signIn()).get("user").get("id").asString();

        // Same Google subject, changed email: the account must follow the subject, and the stored profile
        // must track what Google now reports.
        googleStub.willReturn(new GoogleIdentity(subject, "an.nguyen.moi@example.com", true, "An Nguyễn"));
        JsonNode second = bodyOf(signIn());

        assertThat(second.get("user").get("id").asString()).isEqualTo(firstId);
        assertThat(second.get("user").get("email").asString()).isEqualTo("an.nguyen.moi@example.com");
    }

    @Test
    void twoDifferentGoogleAccountsGetSeparateUsers() {
        googleStub.willReturn(
                new GoogleIdentity("google-subject-" + UUID.randomUUID(), "mot@example.com", true, "Một"));
        String firstId = bodyOf(signIn()).get("user").get("id").asString();

        googleStub.willReturn(
                new GoogleIdentity("google-subject-" + UUID.randomUUID(), "hai@example.com", true, "Hai"));
        String secondId = bodyOf(signIn()).get("user").get("id").asString();

        assertThat(secondId).isNotEqualTo(firstId);
    }

    @Test
    void anUnverifiedGoogleEmailCannotSignIn() {
        googleStub.willReturn(
                new GoogleIdentity(
                        "google-subject-" + UUID.randomUUID(),
                        "chua.xac.thuc@example.com",
                        false,
                        "Chưa xác thực"));

        assertProblem(signIn(), 401, "unauthorized");
    }

    @Test
    void aRedirectUriOutsideTheAllowlistIsRejected() {
        googleStub.willReturn(
                new GoogleIdentity("google-subject-" + UUID.randomUUID(), "an@example.com", true, "An"));

        ResponseEntity<String> response =
                restTemplate.postForEntity(
                        "/api/v1/auth/google",
                        json(
                                """
                                {"authorizationCode":"%s","codeVerifier":"%s",\
                                "redirectUri":"https://evil.example.com/callback"}
                                """
                                        .formatted(
                                                StubGoogleIdentityProvider.ANY_CODE,
                                                StubGoogleIdentityProvider.ANY_VERIFIER)),
                        String.class);

        assertProblem(response, 400, "validation-failed");
        assertThat(bodyOf(response).get("errors").get(0).get("field").asString())
                .isEqualTo("redirectUri");
    }

    @Test
    void renewalReturnsANewPairAndRetiresThePresentedRefreshToken() {
        googleStub.willReturn(
                new GoogleIdentity("google-subject-" + UUID.randomUUID(), "an@example.com", true, "An"));
        JsonNode first = bodyOf(signIn());
        String firstRefresh = first.get("refreshToken").asString();

        JsonNode renewed = bodyOf(refreshWith(firstRefresh));

        assertThat(renewed.get("refreshToken").asString()).isNotEqualTo(firstRefresh);
        assertThat(renewed.get("user").get("id").asString())
                .isEqualTo(first.get("user").get("id").asString());
        // The new access token works...
        assertThat(readSessionWith(renewed.get("accessToken").asString()).get("email").asString())
                .isEqualTo("an@example.com");
        // ...and the presented refresh token is dead.
        assertProblem(refreshWith(firstRefresh), 401, "unauthorized");
    }

    @Test
    void replayingARetiredRefreshTokenRevokesTheWholeSession() {
        googleStub.willReturn(
                new GoogleIdentity("google-subject-" + UUID.randomUUID(), "an@example.com", true, "An"));
        String firstRefresh = bodyOf(signIn()).get("refreshToken").asString();
        String secondRefresh = bodyOf(refreshWith(firstRefresh)).get("refreshToken").asString();

        // Replay the retired token. By now an attacker holding it may also hold the successor, so the
        // successor must die with it rather than the replay merely failing.
        assertProblem(refreshWith(firstRefresh), 401, "unauthorized");

        assertProblem(refreshWith(secondRefresh), 401, "unauthorized");
    }

    @Test
    void signingOutMakesTheSessionUnrenewable() {
        googleStub.willReturn(
                new GoogleIdentity("google-subject-" + UUID.randomUUID(), "an@example.com", true, "An"));
        String refreshToken = bodyOf(signIn()).get("refreshToken").asString();

        ResponseEntity<String> logout = logoutWith(refreshToken);

        assertThat(logout.getStatusCode().value()).isEqualTo(204);
        assertProblem(refreshWith(refreshToken), 401, "unauthorized");
    }

    @Test
    void signingOutTwiceIsStillNoContent() {
        googleStub.willReturn(
                new GoogleIdentity("google-subject-" + UUID.randomUUID(), "an@example.com", true, "An"));
        String refreshToken = bodyOf(signIn()).get("refreshToken").asString();
        logoutWith(refreshToken);

        // Not an error, and not distinguishable from the first call. An endpoint that answered differently
        // would tell any caller whether a guessed token was live.
        assertThat(logoutWith(refreshToken).getStatusCode().value()).isEqualTo(204);
        assertThat(logoutWith("a-token-that-was-never-issued").getStatusCode().value()).isEqualTo(204);
    }

    @Test
    void anAccessTokenIssuedBeforeSignOutRemainsValidUntilItExpires() {
        googleStub.willReturn(
                new GoogleIdentity("google-subject-" + UUID.randomUUID(), "an@example.com", true, "An"));
        JsonNode session = bodyOf(signIn());
        String accessToken = session.get("accessToken").asString();
        assertThat(readSessionWith(accessToken).get("email").asString()).isEqualTo("an@example.com");

        logoutWith(session.get("refreshToken").asString());

        // Recorded as behaviour, not claimed as a goal. The access token is a self-contained signed
        // credential with no per-request database lookup, so signing out ends the ability to *renew* and
        // the remaining exposure is the access token's own 15-minute lifetime. This test exists so nobody
        // reads "logout invalidation" as instant revocation of every issued access token, and so that if
        // that stronger property is ever wanted there is a test here to change rather than a surprise to
        // discover. The acceptance report states the same limitation.
        assertThat(sessionResponseFor(accessToken).getStatusCode().value()).isEqualTo(200);
    }

    private ResponseEntity<String> signIn() {
        return restTemplate.postForEntity(
                "/api/v1/auth/google",
                json(
                        """
                        {"authorizationCode":"%s","codeVerifier":"%s","redirectUri":"%s"}
                        """
                                .formatted(
                                        StubGoogleIdentityProvider.ANY_CODE,
                                        StubGoogleIdentityProvider.ANY_VERIFIER,
                                        StubGoogleIdentityProvider.ALLOWED_REDIRECT_URI)),
                String.class);
    }

    private ResponseEntity<String> refreshWith(String refreshToken) {
        return restTemplate.postForEntity(
                "/api/v1/auth/refresh",
                json("{\"refreshToken\":\"%s\"}".formatted(refreshToken)),
                String.class);
    }

    private ResponseEntity<String> logoutWith(String refreshToken) {
        return restTemplate.postForEntity(
                "/api/v1/auth/logout",
                json("{\"refreshToken\":\"%s\"}".formatted(refreshToken)),
                String.class);
    }

    private JsonNode readSessionWith(String accessToken) {
        ResponseEntity<String> response = sessionResponseFor(accessToken);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        return bodyOf(response);
    }

    private ResponseEntity<String> sessionResponseFor(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return restTemplate.exchange(
                "/api/v1/auth/session", HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private static HttpEntity<String> json(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private void assertProblem(ResponseEntity<String> response, int status, String code) {
        assertThat(response.getStatusCode().value()).isEqualTo(status);
        assertThat(bodyOf(response).get("type").asString()).isEqualTo("urn:rikkaus:problem:" + code);
    }

    private JsonNode bodyOf(ResponseEntity<String> response) {
        return objectMapper.readTree(response.getBody());
    }
}
