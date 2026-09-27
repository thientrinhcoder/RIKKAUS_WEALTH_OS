package com.rikkaus.wealth.shared.api;

/**
 * The one definition of the versioned API prefix.
 *
 * <p>Keeping it here rather than repeating a string literal is what makes introducing {@code
 * /api/v2} a single edit instead of a grep across every controller. Versioning is a static path
 * prefix on purpose: Spring Framework 7's native {@code spring.mvc.apiversion} feature is
 * deliberately unused because springdoc returns HTTP 400 from {@code /v3/api-docs} when it is
 * enabled on Boot 4.x (springdoc-openapi issue 3163).
 */
public final class ApiPaths {

    public static final String V1 = "/api/v1";

    private ApiPaths() {}
}
