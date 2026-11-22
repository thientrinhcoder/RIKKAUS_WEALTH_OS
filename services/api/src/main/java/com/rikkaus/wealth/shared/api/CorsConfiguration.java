package com.rikkaus.wealth.shared.api;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Cross-origin access for the two surfaces a browser actually calls, from an explicit allowlist.
 *
 * <p>Fails closed. The backing property defaults to empty, and an empty list registers no mapping at
 * all rather than guessing an origin, so a deployment that configures nothing permits nothing.
 *
 * <p>This covers {@code /api/v1/**} only. {@code /actuator/health} is configured through {@code
 * management.endpoints.web.cors} in {@code application.yml}, because actuator endpoints are served by
 * their own handler mapping, which a {@code WebMvcConfigurer} cannot reach.
 *
 * <p><strong>Credentials are allowed on {@code /api/v1/auth/**} only.</strong> That is where the web
 * client's refresh token travels, in the HttpOnly {@code rikkaus_refresh} cookie, and a credentialed
 * cross-origin request is refused by the browser unless the response allows credentials for the exact
 * origin. Every other route authenticates with an {@code Authorization} header the client sets itself,
 * so it needs no cookie and keeps credentials off. Allowing credentials is safe only because the origin
 * list is explicit: the code takes configured values and never a wildcard, which Spring would in any case
 * reject in combination with credentials.
 *
 * <p>The auth mapping is registered first on purpose. The handler mapping returns the first pattern that
 * matches, so registering {@code /api/v1/**} first would shadow it and silently drop the credentials
 * flag the web session depends on.
 */
@Configuration
class CorsConfiguration implements WebMvcConfigurer {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    /** Declares a web-mode request; see {@code WebSessionTransport}. */
    private static final String CLIENT_HEADER = "X-Rikkaus-Client";

    private final List<String> allowedOrigins;

    CorsConfiguration(@Value(AllowedOrigins.PROPERTY) String allowedOrigins) {
        this.allowedOrigins = AllowedOrigins.parse(allowedOrigins);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (allowedOrigins.isEmpty()) {
            return;
        }
        String[] origins = allowedOrigins.toArray(String[]::new);
        registry.addMapping(ApiPaths.V1 + "/auth/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST")
                .allowedHeaders(
                        HttpHeaders.CONTENT_TYPE, HttpHeaders.AUTHORIZATION, CORRELATION_ID_HEADER, CLIENT_HEADER)
                .exposedHeaders(CORRELATION_ID_HEADER)
                .allowCredentials(true);
        registry.addMapping(ApiPaths.V1 + "/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE")
                // Authorization is listed because a browser client cannot send its bearer token
                // cross-origin otherwise: it is not a CORS-safelisted request header.
                .allowedHeaders(HttpHeaders.CONTENT_TYPE, HttpHeaders.AUTHORIZATION, CORRELATION_ID_HEADER)
                // The browser can only read a non-safelisted response header if it is exposed.
                .exposedHeaders(CORRELATION_ID_HEADER)
                .allowCredentials(false);
    }
}
