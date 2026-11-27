package com.rikkaus.wealth.cashflow.health.application;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the financial-health thresholds.
 *
 * <p>A plain {@code @Configuration} rather than {@code @ConfigurationPropertiesScan}, for the reason
 * {@code IdentityConfiguration} records: it is excluded from a {@code @WebMvcTest} slice, so binding
 * happens only in a full application context.
 */
@Configuration
@EnableConfigurationProperties(FinancialHealthProperties.class)
class FinancialHealthConfiguration {}
