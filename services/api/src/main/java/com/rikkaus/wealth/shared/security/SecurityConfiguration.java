package com.rikkaus.wealth.shared.security;

import com.rikkaus.wealth.shared.api.ApiPaths;
import com.rikkaus.wealth.shared.error.ProblemDetailWriter;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * The application's one security filter chain: stateless, bearer-token, deny by default.
 *
 * <p>This bean is mandatory rather than optional. Spring Boot's own default chain secures every request
 * with HTTP Basic and a generated login page, so without an explicit chain the API would answer JSON
 * clients with an HTML form.
 *
 * <p><strong>Deny by default is a contract change, made deliberately.</strong> Before authentication
 * existed, an unknown path under {@code /api/v1} returned 404 {@code not-found}. It now returns 401
 * {@code unauthorized}, because an unauthenticated caller must not be able to map which paths exist. The
 * 404 behaviour is preserved for authenticated callers, and both are asserted in
 * {@code SecurityConfigurationIT}.
 */
@Configuration
class SecurityConfiguration {

    private final BearerTokenAuthenticationFilter bearerTokenFilter;
    private final ProblemAuthenticationEntryPoint authenticationEntryPoint;
    private final ProblemAccessDeniedHandler accessDeniedHandler;
    private final String healthPath;

    /**
     * The three collaborators are constructed here rather than scanned as components. A scanned {@code
     * Filter} is pulled into every {@code @WebMvcTest} slice by {@code WebMvcTypeExcludeFilter}, where
     * the {@link AccessTokenVerifier} it needs is absent and the context fails to refresh. Building them
     * inside this configuration — which a slice excludes — keeps the whole chain out of slice tests.
     */
    SecurityConfiguration(
            AccessTokenVerifier accessTokenVerifier,
            ProblemDetailWriter problemDetailWriter,
            WebEndpointProperties webEndpointProperties) {
        this.bearerTokenFilter =
                new BearerTokenAuthenticationFilter(accessTokenVerifier, problemDetailWriter);
        this.authenticationEntryPoint = new ProblemAuthenticationEntryPoint(problemDetailWriter);
        this.accessDeniedHandler = new ProblemAccessDeniedHandler(problemDetailWriter);
        // Read from the actuator's own property rather than hard-coding "/actuator", so relocating the
        // base path in configuration cannot silently lock the liveness probe out.
        this.healthPath = webEndpointProperties.getBasePath() + "/health";
    }

    @Bean
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                // Honours the existing MVC CORS configuration. Spring Security falls back to the
                // HandlerMappingIntrospector when no CorsConfigurationSource bean exists, which is the
                // case here because CORS is declared through WebMvcConfigurer. Without this, a browser
                // preflight — which carries no credential — would be refused before it reached MVC, and
                // the whole allowlist in CorsConfiguration would be dead.
                .cors(Customizer.withDefaults())
                // No cookie-borne credential exists to forge: the access token travels in an
                // Authorization header the browser never attaches on its own, so a cross-site form post
                // cannot authenticate. If a cookie session is ever introduced this must be revisited,
                // and that is the only condition under which this line is wrong.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Both would otherwise be enabled by default and would answer an unauthenticated request
                // with a browser credential prompt or an HTML form instead of a problem document.
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .authorizeHttpRequests(
                        requests ->
                                requests
                                        // Matched by path rather than by method, deliberately.
                                        //
                                        // Narrowing each of these to its one real method looks tighter but
                                        // buys nothing: none of them has a handler for any other method, so
                                        // the only thing method scoping changes is that a wrong-method
                                        // request falls through to anyRequest() and answers 401 instead of
                                        // the 405 that the published contract documents for these routes.
                                        // That would make the contract untrue for every unauthenticated
                                        // caller while protecting nothing, since there is no second handler
                                        // to protect.
                                        //
                                        // /meta reports the build version and nothing about any user.
                                        .requestMatchers(ApiPaths.V1 + "/meta")
                                        .permitAll()
                                        // Sign-in: by definition there is no token yet. Renewal and sign-out
                                        // authenticate with the refresh token in the body — requiring a live
                                        // access token would make an expired session impossible to recover
                                        // from, which is the state the accepted identity design expects a
                                        // client to handle.
                                        .requestMatchers(
                                                ApiPaths.V1 + "/auth/google",
                                                ApiPaths.V1 + "/auth/refresh",
                                                ApiPaths.V1 + "/auth/logout")
                                        .permitAll()
                                        // A liveness probe cannot carry a credential.
                                        .requestMatchers(healthPath)
                                        .permitAll()
                                        // Already gated to the local profile by springdoc.api-docs.enabled,
                                        // which returns 404 everywhere else. Permitted so that gate stays
                                        // the single control, rather than two half-controls that disagree.
                                        .requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui/**")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .exceptionHandling(
                        handling ->
                                handling
                                        .authenticationEntryPoint(authenticationEntryPoint)
                                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(bearerTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
