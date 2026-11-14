package com.rikkaus.wealth.shared.observability;

import com.rikkaus.wealth.shared.error.ProblemDetailWriter;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registers the correlation filter and the request log filter, deliberately not as components.
 *
 * <p>{@code @WebMvcTest} includes {@code jakarta.servlet.Filter} implementations in its slice. A
 * {@code @Component} filter would therefore be pulled into every slice test in this module and would
 * drag in {@link ProblemDetailWriter}, which is a plain {@code @Component} and so is *not* included —
 * breaking previously-green tests in files this package never touches. A {@code FilterRegistrationBean}
 * in an ordinary configuration is invisible to that slice.
 *
 * <p>This class must not implement {@code WebMvcConfigurer} for the same reason.
 */
@Configuration
class CorrelationIdFilterConfiguration {

    @Bean
    FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter(ProblemDetailWriter writer) {
        var registration = new FilterRegistrationBean<>(new CorrelationIdFilter(writer));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        // ERROR is required for the filter's shouldNotFilterErrorDispatch override to have anything to
        // act on. Both halves are needed and neither works alone.
        registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ERROR);
        return registration;
    }

    /**
     * The request log line, registered for the same reason and in the same way as the filter above.
     *
     * <p>One step inside the correlation filter, so the identifier is already in MDC when the line is
     * written, and still outside Spring Security, so the 401s that chain writes on its own are logged
     * too. {@code REQUEST} dispatch only: an {@code ERROR} dispatch is the same request, and a second
     * line for it would double-count every failure.
     */
    @Bean
    FilterRegistrationBean<RequestLoggingFilter> requestLoggingFilter() {
        var registration = new FilterRegistrationBean<>(new RequestLoggingFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        registration.addUrlPatterns("/*");
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        return registration;
    }
}
