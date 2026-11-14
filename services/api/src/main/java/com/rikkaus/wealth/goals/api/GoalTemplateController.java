package com.rikkaus.wealth.goals.api;

import com.rikkaus.wealth.goals.domain.GoalType;
import com.rikkaus.wealth.shared.api.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The seven goal templates and their suggested defaults. Read-only.
 *
 * <p>A separate path rather than {@code /goals/templates}, so the literal never competes with the
 * {@code /goals/{goalId}} pattern and a goal identifier can never be shadowed by a route name.
 *
 * <p>Behind {@code bearerAuth} like every other route under {@code /api/v1}, even though the content
 * is the same for everyone: the security chain denies by default and widening the public list is an
 * edit to {@code SecurityConfigurationIT} that nothing here justifies.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/goal-templates")
class GoalTemplateController {

    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;
    private static final String PROBLEM_SCHEMA_REF = "#/components/schemas/ProblemDetail";

    @Operation(
            summary = "List the seven goal templates",
            description =
                    "Returns every template with the defaults the accepted design suggests for a new goal of "
                            + "that type: an expected annual return, a horizon in months, and which amount the "
                            + "form should prefill from figures the client already holds. Every value is a "
                            + "suggestion the user may change. The list is fixed and identical for every user.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "The seven templates, in the backbone's order",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = GoalTemplateResponse.class))))
    @ApiResponse(
            responseCode = "401",
            description = "No access token, or it has expired or been revoked",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "405",
            description = "Wrong HTTP method; the templates are read-only",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "406",
            description = "The requested media type cannot be produced",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @GetMapping
    List<GoalTemplateResponse> list() {
        return Arrays.stream(GoalType.values()).map(GoalTemplateResponse::of).toList();
    }
}
