package com.rikkaus.wealth.shared.observability;

import com.rikkaus.wealth.shared.error.ProblemDetailWriter;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registers the correlation filter, deliberately not through {@code @Component}.
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
}
