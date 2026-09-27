package com.rikkaus.wealth.shared.error;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * The error taxonomy every response under {@code /api/v1/**} is classified by.
 *
 * <p>An enum rather than a set of string constants, because a taxonomy entry is three coupled values
 * — wire code, human-readable title and default HTTP status — and an enum makes it impossible to
 * supply one without the others. The {@code urn:rikkaus:problem:} prefix appears exactly once.
 *
 * <p>A URN rather than an {@code https://} URI because this project owns no domain, so an HTTP type
 * URI would name a host that resolves to nothing. RFC 9457 does not require the type to dereference.
 * Changing the scheme later would break every client, so it is settled here.
 *
 * <p>{@code detail} text is fixed per entry and is used in place of the framework's own text for the
 * statuses where Spring reflects client input back into the message.
 */
public enum ProblemType {
    VALIDATION_FAILED("validation-failed", "Request validation failed", HttpStatus.BAD_REQUEST),
    MALFORMED_REQUEST("malformed-request", "Malformed request", HttpStatus.BAD_REQUEST),
    NOT_FOUND("not-found", "Resource not found", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED("method-not-allowed", "Method not allowed", HttpStatus.METHOD_NOT_ALLOWED),
    UNSUPPORTED_MEDIA_TYPE(
            "unsupported-media-type", "Unsupported media type", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    NOT_ACCEPTABLE("not-acceptable", "Not acceptable", HttpStatus.NOT_ACCEPTABLE),
    /**
     * Declared but unreachable here. Reserved so the authentication task can throw it without
     * editing this enum or renegotiating the contract with the frontend.
     */
    UNAUTHORIZED("unauthorized", "Authentication required", HttpStatus.UNAUTHORIZED),
    /** Reserved for the same reason as {@link #UNAUTHORIZED}. */
    FORBIDDEN("forbidden", "Access denied", HttpStatus.FORBIDDEN),
    INTERNAL_ERROR("internal-error", "Unexpected server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private static final String URN_PREFIX = "urn:rikkaus:problem:";

    private final String code;
    private final String title;
    private final HttpStatus status;

    ProblemType(String code, String title, HttpStatus status) {
        this.code = code;
        this.title = title;
        this.status = status;
    }

    public String code() {
        return code;
    }

    public URI type() {
        return URI.create(URN_PREFIX + code);
    }

    public String title() {
        return title;
    }

    public HttpStatus status() {
        return status;
    }

    /** The generic, input-free detail text used where the framework's own text is unsafe to echo. */
    public String safeDetail() {
        return title + ".";
    }

    /**
     * Classifies a framework-raised status. Anything unmapped becomes {@link #INTERNAL_ERROR} rather
     * than falling back to {@code about:blank}, so no response can escape the taxonomy.
     *
     * <p>400 maps to {@link #MALFORMED_REQUEST} because the handler overrides bean-validation
     * failures separately with {@link #VALIDATION_FAILED}; a 400 arriving here is a parse, binding or
     * type-conversion failure rather than a field-level rejection.
     */
    public static ProblemType forStatus(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> MALFORMED_REQUEST;
            case 401 -> UNAUTHORIZED;
            case 403 -> FORBIDDEN;
            case 404 -> NOT_FOUND;
            case 405 -> METHOD_NOT_ALLOWED;
            case 406 -> NOT_ACCEPTABLE;
            case 415 -> UNSUPPORTED_MEDIA_TYPE;
            default -> INTERNAL_ERROR;
        };
    }
}
