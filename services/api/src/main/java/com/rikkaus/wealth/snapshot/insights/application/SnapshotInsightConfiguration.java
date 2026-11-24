package com.rikkaus.wealth.snapshot.insights.application;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the insight thresholds.
 *
 * <p>A plain {@code @Configuration} rather than {@code @ConfigurationPropertiesScan}, for the reason
 * {@code IdentityConfiguration} records: it is excluded from a {@code @WebMvcTest} slice, so binding
 * happens only in a full application context.
 */
@Configuration
@EnableConfigurationProperties(SnapshotInsightProperties.class)
class SnapshotInsightConfiguration {}
