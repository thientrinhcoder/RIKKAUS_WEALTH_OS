package com.rikkaus.wealth.shared.error;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Renders every error as an RFC 9457 {@code application/problem+json} body.
 *
 * <p>Extending {@code ResponseEntityExceptionHandler} covers every built-in Spring MVC exception,
 * because all of them implement {@code ErrorResponse}. Registering this bean also makes Boot's own
 * autoconfigured handler back off, since {@code ProblemDetailsErrorHandlingConfiguration} is
 * {@code @ConditionalOnMissingBean(ResponseEntityExceptionHandler.class)}. That is why
 * {@code spring.mvc.problemdetails.enabled} is deliberately not set anywhere: with this advice
 * registered the property is dead configuration, and setting it would mislead a later maintainer into
 * thinking it was load-bearing.
 *
 * <p><strong>{@code handleExceptionInternal} is the funnel, not {@code createProblemDetail}.</strong>
 * Overriding the latter covers only the statuses Spring builds a body for on the spot. Every {@code
 * ErrorResponse} exception — which is what 404, 405, 406, 415 and type-mismatch 400 all are — carries
 * its own pre-built {@code ProblemDetail} and never calls {@code createProblemDetail} at all. Those
 * responses kept {@code about:blank} and echoed caller input until the classification moved here.
 * Every path, framework and domain alike, passes through this one method.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** What {@code ProblemDetail} uses until something classifies the response. */
    private static final URI UNCLASSIFIED_TYPE = URI.create("about:blank");

    static final String CORRELATION_ID_MEMBER = "correlationId";

    /**
     * Statuses whose framework-supplied {@code detail} reflects attacker-controlled input and must be
     * replaced with the taxonomy's own fixed text.
     *
     * <p>{@code TypeMismatchException} produces text of the form {@code Failed to convert 'x' with
     * value: '<client input>'} and can name internal target types, and {@code NoResourceFoundException}
     * produces {@code No static resource <client path>.} Both echo a probe value straight back.
     */
    private static final Set<Integer> STATUSES_THAT_REFLECT_INPUT = Set.of(400, 404);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Object> handleApiException(ApiException ex, WebRequest request) {
        ProblemType type = ex.getProblemType();
        ProblemDetail body = ProblemDetail.forStatusAndDetail(type.status(), ex.getMessage());
        // Classified here so the detail the thrower chose survives the funnel below, which only
        // rewrites detail for responses it had to classify itself.
        classify(body, type);
        ex.getExtensions().forEach(body::setProperty);
        return handleExceptionInternal(ex, body, new HttpHeaders(), type.status(), request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ProblemDetail body = ex.getBody();
        // Classified as a validation failure rather than the generic 400 the funnel would assign.
        classify(body, ProblemType.VALIDATION_FAILED);
        body.setProperty("errors", fieldErrorsOf(ex));
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    /**
     * Last resort. Deliberately never inspects the exception message, so nothing internal can reach
     * the body; the real cause is recoverable from the log alone.
     *
     * <p>It must stay last and must never catch {@link ApiException} or a Spring MVC exception, or it
     * would mask a specific status as a 500.
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpectedException(Exception ex, WebRequest request) {
        log.error("Unhandled exception while serving a request", ex);
        ProblemType type = ProblemType.INTERNAL_ERROR;
        ProblemDetail body = ProblemDetail.forStatusAndDetail(type.status(), type.safeDetail());
        classify(body, type);
        return handleExceptionInternal(ex, body, new HttpHeaders(), type.status(), request);
    }

    /**
     * The one place every error body is guaranteed to pass through.
     *
     * <p>Classification happens <em>after</em> the superclass call, not before, because most of the
     * built-in handlers pass a {@code null} body and let the base implementation derive the {@code
     * ProblemDetail} from the {@code ErrorResponse} itself. Inspecting the incoming argument therefore
     * sees nothing for exactly the statuses that matter — 404, 405 and 415 all kept {@code about:blank}
     * that way. {@code ProblemDetail} is mutable and the response holds the same instance, so
     * classifying the returned body is what actually reaches the client.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            @Nullable Object body,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ResponseEntity<Object> response =
                super.handleExceptionInternal(ex, body, headers, status, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem) {
            if (isUnclassified(problem)) {
                ProblemType type = ProblemType.forStatus(status);
                classify(problem, type);
                // Only a body this method had to classify gets its detail replaced. A handler above
                // that set its own type chose its own detail deliberately, and it must survive.
                if (STATUSES_THAT_REFLECT_INPUT.contains(status.value())) {
                    problem.setDetail(type.safeDetail());
                }
            }
            addCorrelationId(problem);
        }
        return response;
    }

    private static boolean isUnclassified(ProblemDetail problem) {
        return problem.getType() == null || UNCLASSIFIED_TYPE.equals(problem.getType());
    }

    private static void classify(ProblemDetail body, ProblemType type) {
        body.setType(type.type());
        body.setTitle(type.title());
    }

    private static void addCorrelationId(ProblemDetail body) {
        // The MDC key is inlined rather than referenced from the observability package, because the
        // correlation filter does not exist yet; that package replaces this literal with its own
        // constant. Until then MDC is empty and the member is simply absent, which RFC 9457 permits
        // because extension members are optional.
        String correlationId = MDC.get(CORRELATION_ID_MEMBER);
        if (correlationId != null) {
            body.setProperty(CORRELATION_ID_MEMBER, correlationId);
        }
    }

    private static List<Map<String, String>> fieldErrorsOf(MethodArgumentNotValidException ex) {
        return ex.getBindingResult().getFieldErrors().stream()
                .map(
                        fieldError ->
                                Map.of(
                                        "field",
                                        fieldError.getField(),
                                        "message",
                                        Objects.toString(fieldError.getDefaultMessage(), "invalid")))
                .toList();
    }
}
