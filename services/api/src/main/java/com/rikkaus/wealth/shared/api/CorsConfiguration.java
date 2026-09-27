package com.rikkaus.wealth.shared.api;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Cross-origin access for the two surfaces a browser actually calls, from an explicit allowlist.
 *
 * <p>Fails closed. The backing property defaults to empty, and an empty list registers no mapping at
 * all rather than guessing an origin, so a deployment that configures nothing permits nothing.
 *
 * <p>This covers {@code /api/v1/**} only. {@code /actuator/health} matters just as much, because it
 * is the only endpoint the shipped browser client calls and the README has documented CORS as a
 * precondition for that screen since before any CORS configuration existed — but a {@code
 * WebMvcConfigurer} cannot reach it. Actuator endpoints are served by their own handler mapping with
 * its own CORS configuration, so a mapping registered here for {@code /actuator/health} emits no
 * header at all. That surface is configured through {@code management.endpoints.web.cors} in {@code
 * application.yml}, reading the same allowlist property, and is verified by live request rather than
 * by a slice test for exactly this reason.
 *
 * <p>Credentials are refused. That is the correct posture only because no credential exists yet;
 * turning it on belongs with the authentication design rather than being inherited from here as
 * already settled. No wildcard origin is possible, because the code takes a configured list and
 * never a literal.
 */
@Configuration
class CorsConfiguration implements WebMvcConfigurer {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    private final List<String> allowedOrigins;

    CorsConfiguration(@Value("${rikkaus.api.allowed-origins:}") String allowedOrigins) {
        // Parsed from a raw string rather than bound straight to a List, so that an unset or blank
        // property is unambiguously an empty allowlist rather than a list holding one empty entry.
        this.allowedOrigins =
                Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .filter(origin -> !origin.isEmpty())
                        .toList();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (allowedOrigins.isEmpty()) {
            return;
        }
        String[] origins = allowedOrigins.toArray(String[]::new);
        registry.addMapping(ApiPaths.V1 + "/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE")
                .allowedHeaders("Content-Type", CORRELATION_ID_HEADER)
                // The browser can only read a non-safelisted response header if it is exposed.
                .exposedHeaders(CORRELATION_ID_HEADER)
                .allowCredentials(false);
    }
}
