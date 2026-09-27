package com.rikkaus.wealth.shared.error;

import java.util.Map;

/**
 * The exception a feature slice throws to produce a conforming error response.
 *
 * <p>Slices throw this or a narrow subclass and never build a {@code ProblemDetail} themselves, so
 * the wire shape stays owned by {@link ApiExceptionHandler} alone.
 *
 * <p>The {@code detail} passed here is rendered to the client verbatim, so it must be user-safe: no
 * internal identifiers, no SQL, no exception messages from a lower layer.
 */
public class ApiException extends RuntimeException {

    private final ProblemType problemType;
    private final Map<String, Object> extensions;

    public ApiException(ProblemType problemType, String detail) {
        this(problemType, detail, Map.of());
    }

    public ApiException(ProblemType problemType, String detail, Map<String, Object> extensions) {
        super(detail);
        this.problemType = problemType;
        this.extensions = Map.copyOf(extensions);
    }

    public ProblemType getProblemType() {
        return problemType;
    }

    /** Additional RFC 9457 extension members to render alongside the standard ones. */
    public Map<String, Object> getExtensions() {
        return extensions;
    }
}
