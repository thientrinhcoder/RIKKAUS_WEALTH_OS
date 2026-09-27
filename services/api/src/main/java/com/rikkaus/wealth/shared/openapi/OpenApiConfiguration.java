package com.rikkaus.wealth.shared.openapi;

import com.rikkaus.wealth.shared.error.ProblemType;
import com.rikkaus.wealth.shared.observability.CorrelationId;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.servers.Server;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Owns the contract's {@code info}, its {@code servers} entry and the shared error components.
 *
 * <p>{@code info} is set through a bean because springdoc exposes no configuration property for the
 * title or version in any release line. Only the path and enablement toggles live in {@code
 * application.yml}.
 */
@Configuration
class OpenApiConfiguration {

    static final String PROBLEM_DETAIL_SCHEMA = "ProblemDetail";

    @Bean
    OpenAPI rikkausWealthApi() {
        return new OpenAPI()
                .servers(List.of(relativeServer()))
                .info(
                        new Info()
                                .title("Rikkaus Wealth OS API")
                                .version("v1")
                                .description(
                                        "Versioned REST contract for the Rikkaus Wealth OS backend. "
                                                + "Errors follow RFC 9457 with a urn:rikkaus:problem: type."))
                .components(
                        new Components()
                                .addSchemas(PROBLEM_DETAIL_SCHEMA, problemDetailSchema())
                                .addHeaders(CorrelationId.HEADER, correlationIdHeader()));
    }

    /**
     * A relative server URL, and it is load-bearing rather than cosmetic.
     *
     * <p>With no server declared, springdoc synthesises one from the incoming request — the contract
     * test runs against a random port, so the document would embed a different ephemeral port on every
     * run. Canonicalization sorts keys; it does not normalise values, so the drift assertion would fail
     * on every single run claiming "the API changed", and the documented escape hatch would train the
     * team to regenerate reflexively. A relative URL is also the right answer for a client that
     * resolves paths against its own configured base URL.
     */
    private static Server relativeServer() {
        return new Server().url("/").description("Relative to the deployment's own base URL");
    }

    /**
     * Written by hand rather than inferred from Spring's {@code ProblemDetail} class, because the
     * inferred version omits this project's extension members — and {@code correlationId} and {@code
     * errors} are exactly the parts a client's error classifier needs.
     */
    private static Schema<?> problemDetailSchema() {
        return new ObjectSchema()
                .description("RFC 9457 problem details. Returned for every error under /api/v1/**.")
                .addProperty(
                        "type",
                        new StringSchema()
                                .description(
                                        "Stable taxonomy identifier. A URN rather than a URL because "
                                                + "the project owns no domain; RFC 9457 does not require "
                                                + "it to dereference.")
                                ._enum(problemTypeUrns()))
                .addProperty("title", new StringSchema().description("Short, human-readable summary."))
                .addProperty("status", new IntegerSchema().description("The HTTP status code."))
                .addProperty(
                        "detail",
                        new StringSchema()
                                .description(
                                        "Human-readable explanation, safe to display. Never contains a "
                                                + "stack trace, an internal class name or a credential."))
                .addProperty(
                        "instance",
                        new StringSchema().description("The request URI the problem occurred for."))
                .addProperty(
                        CorrelationId.MDC_KEY,
                        new StringSchema()
                                .description(
                                        "Extension member. The same value as the "
                                                + CorrelationId.HEADER
                                                + " response header and the server's log lines for this "
                                                + "request."))
                .addProperty(
                        "errors",
                        new ArraySchema()
                                .description(
                                        "Extension member, present on validation failures only. One "
                                                + "entry per rejected field.")
                                .items(
                                        new ObjectSchema()
                                                .addProperty("field", new StringSchema())
                                                .addProperty("message", new StringSchema())))
                .required(List.of("type", "title", "status"));
    }

    /**
     * Enumerated from the taxonomy itself rather than by hand, so adding an entry without regenerating
     * the contract shows up as a diff instead of going unnoticed.
     */
    private static List<String> problemTypeUrns() {
        return Arrays.stream(ProblemType.values()).map(type -> type.type().toString()).sorted().toList();
    }

    private static Header correlationIdHeader() {
        return new Header()
                .description(
                        "Correlation identifier. Send one matching ^[A-Za-z0-9-]{8,64}$ to have it "
                                + "echoed; anything else is replaced with a generated value and the "
                                + "request still succeeds. Always returned.")
                .schema(new StringSchema().pattern("^[A-Za-z0-9-]{8,64}$"));
    }
}
