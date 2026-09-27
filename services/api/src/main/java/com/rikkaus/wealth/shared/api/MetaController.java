package com.rikkaus.wealth.shared.api;

import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reports which build of the API a client is talking to.
 *
 * <p>This is real runtime data rather than a placeholder. It exists because the published OpenAPI
 * contract is scoped to {@code /api/v1/**}, and with no controller at all that contract would carry
 * an empty {@code paths} object, leaving the frontend nothing to build fixtures against until the
 * first domain slice lands.
 *
 * <p>It carries no OpenAPI annotations on purpose; documentation annotations are owned by the
 * OpenAPI configuration phase so that two concerns never edit the same lines.
 */
@RestController
@RequestMapping(ApiPaths.V1)
class MetaController {

    private static final String UNKNOWN_BUILD_VERSION = "unknown";

    private final Clock clock;
    private final String applicationName;
    private final String buildVersion;

    MetaController(
            Clock clock,
            @Value("${spring.application.name}") String applicationName,
            ObjectProvider<BuildProperties> buildProperties) {
        this.clock = clock;
        this.applicationName = applicationName;
        // BuildProperties exists only when the Maven build-info goal has written
        // META-INF/build-info.properties. Running from an IDE without it is a normal development
        // path and must not fail context startup, hence the provider and the fallback.
        BuildProperties build = buildProperties.getIfAvailable();
        this.buildVersion = build != null ? build.getVersion() : UNKNOWN_BUILD_VERSION;
    }

    @GetMapping("/meta")
    MetaResponse meta() {
        return new MetaResponse(applicationName, "v1", buildVersion, clock.instant());
    }
}
