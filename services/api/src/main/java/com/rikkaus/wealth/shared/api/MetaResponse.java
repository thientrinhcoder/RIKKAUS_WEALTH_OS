package com.rikkaus.wealth.shared.api;

import java.time.Instant;

/**
 * What {@code GET /api/v1/meta} returns.
 *
 * <p>No binary floating point appears here, in keeping with the exact-decimal rule the architecture
 * rules enforce across this package. {@code serverTime} serializes as an ISO-8601 string rather than
 * an epoch number, which {@code MetaControllerTest} pins with an exact assertion.
 *
 * <p>The build timestamp is deliberately absent. Only the build version is exposed, because a build
 * time on an unauthenticated endpoint helps an attacker correlate a deployment with a known CVE
 * window, and whether to expose it is an open question on the plan index.
 */
public record MetaResponse(
        String application, String apiVersion, String buildVersion, Instant serverTime) {}
