package com.rikkaus.wealth.identity;

import com.rikkaus.wealth.identity.google.GoogleOidcProperties;
import com.rikkaus.wealth.identity.session.SessionProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the identity slice's configuration properties.
 *
 * <p>Declared here rather than as {@code @ConfigurationPropertiesScan} on the application class,
 * because that annotation binds in every context that finds the application class — including a
 * {@code @WebMvcTest} slice, which loads no profile and would therefore try to bind a signing secret
 * the base configuration deliberately leaves empty. Every slice test in this package would then fail at
 * context refresh with a message about a missing secret rather than about the controller under test.
 *
 * <p>A plain {@code @Configuration} is excluded from a {@code @WebMvcTest} slice by
 * {@code WebMvcTypeExcludeFilter} — the same mechanism {@code ClockConfiguration} relies on — so the
 * binding happens only in a full application context, which is the only place it is needed.
 */
@Configuration
@EnableConfigurationProperties({SessionProperties.class, GoogleOidcProperties.class})
class IdentityConfiguration {}
