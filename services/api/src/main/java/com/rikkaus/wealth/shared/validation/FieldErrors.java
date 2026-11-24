package com.rikkaus.wealth.shared.validation;

import com.rikkaus.wealth.shared.error.ApiException;
import com.rikkaus.wealth.shared.error.ProblemType;
import java.util.List;
import java.util.Map;

/**
 * The {@code validation-failed} body a service-level rule produces: the taxonomy's fixed detail and
 * one {@code {field, message}} entry per problem in the {@code errors} extension, the same shape bean
 * validation produces through {@code ApiExceptionHandler}.
 */
public final class FieldErrors {

    private FieldErrors() {}

    /** One entry of the {@code errors} array. */
    public static Map<String, String> error(String field, String message) {
        return Map.of("field", field, "message", message);
    }

    /** A refusal naming one field. */
    public static ApiException validationFailed(String field, String message) {
        return validationFailed(List.of(error(field, message)));
    }

    /** A refusal naming every entry of {@code errors}, so the client can mark them all at once. */
    public static ApiException validationFailed(List<Map<String, String>> errors) {
        return new ApiException(
                ProblemType.VALIDATION_FAILED,
                ProblemType.VALIDATION_FAILED.safeDetail(),
                Map.of("errors", List.copyOf(errors)));
    }

    /** Throws one refusal listing every collected entry, when there is any. */
    public static void rejectIfAny(List<Map<String, String>> errors) {
        if (!errors.isEmpty()) {
            throw validationFailed(errors);
        }
    }
}
