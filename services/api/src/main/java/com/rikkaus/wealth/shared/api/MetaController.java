package com.rikkaus.wealth.shared.api;

import com.rikkaus.wealth.shared.observability.CorrelationId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.MediaType;
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
    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;
    private static final String PROBLEM_SCHEMA_REF = "#/components/schemas/ProblemDetail";

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

    @Operation(
            summary = "Report which build of the API the caller is talking to",
            description =
                    "Unauthenticated by design. Exposes the application name, the API version, the "
                            + "build version and the current server time in UTC, and nothing else. The "
                            + "build timestamp is deliberately withheld.")
    @ApiResponse(
            responseCode = "200",
            description = "API metadata",
            headers =
                    @Header(
                            name = CorrelationId.HEADER,
                            description = "Correlation identifier for this request",
                            schema = @Schema(type = "string")),
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MetaResponse.class),
                            examples =
                                    @ExampleObject(
                                            name = "current build",
                                            value =
                                                    """
                                                    {
                                                      "application": "rikkaus-wealth-api",
                                                      "apiVersion": "v1",
                                                      "buildVersion": "0.0.1-SNAPSHOT",
                                                      "serverTime": "2026-09-27T07:42:00Z"
                                                    }
                                                    """)))
    // Only the statuses this endpoint can actually return are documented. An over-broad contract is
    // worse than a narrow one, because a consumer would generate fixtures for responses that never
    // occur. It takes no request body and no parameters, so it cannot produce 400 or 415.
    @ApiResponse(
            responseCode = "405",
            description = "Wrong HTTP method",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "406",
            description = "The requested media type cannot be produced",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @GetMapping("/meta")
    MetaResponse meta() {
        return new MetaResponse(applicationName, "v1", buildVersion, clock.instant());
    }
}
