package com.rikkaus.wealth.shared.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rikkaus.testfixtures.ProblemFixtureController;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Enumerates what the advice covers, one assertion group per status.
 *
 * <p>Every group checks the media type and that {@code type} is a project URN rather than {@code
 * about:blank}, because a framework status that bypasses the {@code createProblemDetail} funnel would
 * otherwise be invisible. The marker assertions run per status, not only for the last-resort handler,
 * since Spring's own {@code detail} text is not uniformly opaque.
 */
// Scoped to the fixture controller. A bare @WebMvcTest scans every controller, which drags in
// MetaController and its Clock — a bean the slice deliberately excludes, so the context would fail to
// refresh for a reason unrelated to error handling. Both classes need @Import because the advice is
// not a controller and the fixture lives outside the component-scan root.
@WebMvcTest(ProblemFixtureController.class)
@Import({ProblemFixtureController.class, ApiExceptionHandler.class})
class ApiExceptionHandlerTest {

    /** Sent as the offending input so its absence from the body can be asserted. */
    private static final String MARKER = "REFLECTED-MARKER-9457";

    private static final String URN_PREFIX = "urn:rikkaus:problem:";

    @Autowired private MockMvc mockMvc;

    @Test
    void validationFailureNamesEveryFailingField() throws Exception {
        conforming(
                        mockMvc.perform(
                                post("/test-fixtures/validated")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"name\":\"\"}")),
                        400,
                        "validation-failed")
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("must not be blank"));
    }

    @Test
    void malformedJsonIsABadRequest() throws Exception {
        conforming(
                mockMvc.perform(
                        post("/test-fixtures/validated")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\": " + MARKER)),
                400,
                "malformed-request");
    }

    @Test
    void typeMismatchIsABadRequestAndDoesNotEchoTheInput() throws Exception {
        conforming(
                mockMvc.perform(get("/test-fixtures/typed/{id}", MARKER)), 400, "malformed-request");
    }

    @Test
    void unknownPathUnderTheApiPrefixIsNotFound() throws Exception {
        conforming(mockMvc.perform(get("/api/v1/" + MARKER)), 404, "not-found");
    }

    @Test
    void wrongMethodIsMethodNotAllowed() throws Exception {
        conforming(mockMvc.perform(post("/test-fixtures/domain")), 405, "method-not-allowed");
    }

    @Test
    void wrongContentTypeIsUnsupportedMediaType() throws Exception {
        conforming(
                mockMvc.perform(
                        post("/test-fixtures/validated")
                                .contentType(MediaType.TEXT_PLAIN)
                                .content(MARKER)),
                415,
                "unsupported-media-type");
    }

    @Test
    void impossibleAcceptIsNotAcceptable() throws Exception {
        // Against an endpoint that would otherwise succeed, so the 406 comes from content
        // negotiation rather than from the endpoint throwing first.
        conforming(
                mockMvc.perform(get("/test-fixtures/typed/1").accept(MediaType.APPLICATION_PDF)),
                406,
                "not-acceptable");
    }

    @Test
    void aDomainExceptionKeepsTheDetailItsThrowerChose() throws Exception {
        conforming(mockMvc.perform(get("/test-fixtures/domain")), 404, "not-found")
                // A domain detail is deliberately chosen by the thrower, so unlike the
                // framework-supplied 404 text it must survive rather than be replaced.
                .andExpect(jsonPath("$.detail").value("Fixture not found"));
    }

    @Test
    void anUnexpectedFailureLeaksNothingFromItsMessage() throws Exception {
        String body =
                conforming(mockMvc.perform(get("/test-fixtures/boom")), 500, "internal-error")
                        .andReturn()
                        .getResponse()
                        .getContentAsString(StandardCharsets.UTF_8);

        assertThat(body).doesNotContain("internal detail that must not leak");
        assertThat(body).doesNotContain("IllegalStateException");
    }

    /**
     * Asserts the members every error response owes a client, and that the human-readable text does
     * not echo the caller's input.
     *
     * <p>{@code detail} and {@code title} are checked rather than the whole body, because RFC 9457
     * defines {@code instance} as the request URI and a request for {@code /api/v1/<marker>} therefore
     * carries the marker there by design. Echoing the caller's own path back is not a disclosure;
     * repeating a probe value inside a message, where a conversion failure can also name an internal
     * target type, is.
     */
    private ResultActions conforming(ResultActions result, int expectedStatus, String expectedCode)
            throws Exception {
        return result.andExpect(status().is(expectedStatus))
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.type").value(URN_PREFIX + expectedCode))
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.instance").isNotEmpty())
                .andExpect(jsonPath("$.detail", not(containsString(MARKER))))
                .andExpect(jsonPath("$.title", not(containsString(MARKER))));
    }
}
