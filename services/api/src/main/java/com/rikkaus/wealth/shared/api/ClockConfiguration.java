package com.rikkaus.wealth.shared.api;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Supplies the {@link Clock} that request handlers read the current time from.
 *
 * <p>This class must not implement {@code WebMvcConfigurer}. Boot's {@code WebMvcTypeExcludeFilter}
 * deliberately includes {@code WebMvcConfigurer} implementations in a {@code @WebMvcTest} slice, so
 * a configuration that both implemented that interface and declared this bean would be loaded into
 * slice tests and collide with the test's own clock. Bean-definition overriding is disabled by
 * default, so the collision aborts the context before any assertion runs.
 */
@Configuration
class ClockConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
