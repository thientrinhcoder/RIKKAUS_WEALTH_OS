package com.rikkaus.wealth.shared.error;

import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Renders a Problem Details body straight to the response, for failures raised before {@code
 * DispatcherServlet} runs.
 *
 * <p>{@link ApiExceptionHandler} never sees those: an exception thrown inside a servlet filter
 * bypasses the advice entirely and lands on the container's default error handling, which is a known
 * upstream inconsistency (spring-projects/spring-boot#48392). Without this class such a failure would
 * return the container's HTML or an empty body, and the frontend's error classifier would see a
 * response it cannot classify.
 */
@Component
public class ProblemDetailWriter {

    private static final Logger log = LoggerFactory.getLogger(ProblemDetailWriter.class);

    private final ObjectMapper objectMapper;

    /**
     * Takes the autoconfigured mapper rather than constructing one. Boot registers the Problem
     * Details mixin on it, and that is what makes extension members and the RFC member names
     * serialize the same way the advice's bodies do. A hand-built mapper would produce a differently
     * shaped body on this path than on every other.
     */
    public ProblemDetailWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response, ProblemType type, String detail) {
        if (response.isCommitted()) {
            // setStatus and setContentType are silent no-ops on a committed response, so the JSON
            // would be appended to whatever body was already streaming: the client would get a 200
            // with two concatenated objects, which a schema parser reports as malformed data rather
            // than as a server error. Once committed no correct HTTP outcome remains, so the log line
            // is the only recovery path.
            log.warn("Response already committed; cannot render a problem body for {}", type);
            return;
        }
        try {
            response.resetBuffer();
            response.setStatus(type.status().value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            ProblemDetail body = ProblemDetail.forStatusAndDetail(type.status(), detail);
            body.setType(type.type());
            body.setTitle(type.title());
            // Same inlined key, and for the same reason, as ApiExceptionHandler: the observability
            // package does not exist yet and replaces both occurrences together.
            String correlationId = MDC.get("correlationId");
            if (correlationId != null) {
                body.setProperty("correlationId", correlationId);
            }
            objectMapper.writeValue(response.getOutputStream(), body);
        } catch (IOException e) {
            log.error("Failed to write problem response for {}", type, e);
        }
    }
}
