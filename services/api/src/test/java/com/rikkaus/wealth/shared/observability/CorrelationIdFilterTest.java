package com.rikkaus.wealth.shared.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.rikkaus.wealth.shared.error.ApiException;
import com.rikkaus.wealth.shared.error.ProblemDetailWriter;
import com.rikkaus.wealth.shared.error.ProblemType;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Filter lifecycle, inbound validation and the two regressions this filter could cause.
 *
 * <p>Needs no Spring context, so it runs in the fast tier.
 */
class CorrelationIdFilterTest {

    private ProblemDetailWriter problemDetailWriter;
    private CorrelationIdFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        problemDetailWriter = mock(ProblemDetailWriter.class);
        filter = new CorrelationIdFilter(problemDetailWriter);
        request = new MockHttpServletRequest("GET", "/api/v1/meta");
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void generatesAnIdentifierWhenTheRequestSendsNone() throws Exception {
        filter.doFilter(request, response, new MockFilterChain());

        assertThat(CorrelationId.isAcceptable(response.getHeader(CorrelationId.HEADER))).isTrue();
    }

    @Test
    void echoesAWellFormedInboundIdentifierExactly() throws Exception {
        request.addHeader(CorrelationId.HEADER, "abcdef12-3456-7890");

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(CorrelationId.HEADER)).isEqualTo("abcdef12-3456-7890");
    }

    /**
     * The security regression test. Neither SLF4J nor Logback sanitizes an MDC value, so a CRLF
     * sequence reaching the log context can forge entries. Rejecting is safer than escaping because
     * nothing legitimate needs those characters.
     */
    @Test
    void rejectsInboundValueContainingLineBreaksToPreventLogForging() throws Exception {
        request.addHeader(CorrelationId.HEADER, "abcdefgh\r\nFORGED-LOG-LINE");

        filter.doFilter(request, response, new MockFilterChain());

        String published = response.getHeader(CorrelationId.HEADER);
        assertThat(published).doesNotContain("FORGED-LOG-LINE").doesNotContain("\r").doesNotContain("\n");
        assertThat(CorrelationId.isAcceptable(published)).isTrue();
    }

    @Test
    void rejectsAnOversizedInboundIdentifier() throws Exception {
        request.addHeader(CorrelationId.HEADER, "a".repeat(200));

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(CorrelationId.HEADER)).hasSizeLessThanOrEqualTo(64);
    }

    @Test
    void rejectsATooShortInboundIdentifier() throws Exception {
        request.addHeader(CorrelationId.HEADER, "abc");

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(CorrelationId.HEADER)).isNotEqualTo("abc");
    }

    @Test
    void rejectsAnEmptyInboundIdentifier() throws Exception {
        request.addHeader(CorrelationId.HEADER, "");

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(CorrelationId.isAcceptable(response.getHeader(CorrelationId.HEADER))).isTrue();
    }

    @Test
    void publishesTheIdentifierToTheLogContextForTheDurationOfTheChain() throws Exception {
        request.addHeader(CorrelationId.HEADER, "abcdef12-3456-7890");
        List<String> seenInsideChain = new ArrayList<>();

        filter.doFilter(
                request,
                response,
                (req, res) -> seenInsideChain.add(MDC.get(CorrelationId.MDC_KEY)));

        assertThat(seenInsideChain).containsExactly("abcdef12-3456-7890");
    }

    @Test
    void clearsTheLogContextAfterASuccessfulRequest() throws Exception {
        filter.doFilter(request, response, new MockFilterChain());

        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }

    @Test
    void clearsTheLogContextEvenWhenTheChainThrows() {
        FilterChain throwing =
                (req, res) -> {
                    throw new IllegalStateException("downstream failure");
                };

        assertThatThrownBy(() -> filter.doFilter(request, response, throwing))
                .isInstanceOf(IllegalStateException.class);

        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }

    /**
     * The taxonomy regression guard. Because {@code chain.doFilter} is not wrapped in a catch, a
     * downstream exception must leave this frame untouched so the advice can classify it. Were it
     * caught, the whole Problem Details taxonomy would collapse into 500s — and so would the
     * authentication task's 401 and 403, since this filter sits outside that chain.
     */
    @Test
    void letsADownstreamExceptionPropagateInsteadOfRewritingIt() {
        ApiException downstream = new ApiException(ProblemType.NOT_FOUND, "Resource not found");
        FilterChain throwing =
                (req, res) -> {
                    throw downstream;
                };

        assertThatThrownBy(() -> filter.doFilter(request, response, throwing)).isSameAs(downstream);

        verify(problemDetailWriter, org.mockito.Mockito.never())
                .write(any(), any(), any());
    }

    @Test
    void rendersAConformingProblemWhenTheFilterItsOwnWorkFails() throws Exception {
        HttpServletResponse failing =
                new MockHttpServletResponse() {
                    @Override
                    public void setHeader(String name, String value) {
                        throw new IllegalStateException("header store unavailable");
                    }
                };

        filter.doFilter(request, failing, new MockFilterChain());

        verify(problemDetailWriter)
                .write(eq(failing), eq(ProblemType.INTERNAL_ERROR), any(String.class));
    }

    /**
     * Proves the {@code shouldNotFilterErrorDispatch} override and the request attribute work together:
     * the container's ERROR dispatch re-publishes the <em>same</em> identifier, rather than being
     * skipped or minting a second one for one request.
     */
    @Test
    void reusesTheSameIdentifierOnTheContainersErrorDispatch() throws Exception {
        request.addHeader(CorrelationId.HEADER, "abcdef12-3456-7890");
        filter.doFilter(request, response, new MockFilterChain());
        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();

        request.setDispatcherType(DispatcherType.ERROR);
        List<String> seenOnErrorDispatch = new ArrayList<>();
        filter.doFilter(
                request,
                response,
                (req, res) -> seenOnErrorDispatch.add(MDC.get(CorrelationId.MDC_KEY)));

        assertThat(seenOnErrorDispatch).containsExactly("abcdef12-3456-7890");
    }

    @Test
    void neverAcceptsAnIdentifierOutsideTheAllowedCharacterSet() throws IOException, ServletException {
        request.addHeader(CorrelationId.HEADER, "has spaces and/slashes");

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(CorrelationId.HEADER))
                .doesNotContain(" ")
                .doesNotContain("/");
    }
}
