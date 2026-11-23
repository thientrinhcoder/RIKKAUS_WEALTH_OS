package com.rikkaus.wealth.goals.projection.api;

import com.rikkaus.wealth.goals.projection.application.GoalProjectionService;
import com.rikkaus.wealth.shared.api.ApiPaths;
import com.rikkaus.wealth.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Goal projections: one goal's, or every goal's of the signed-in user.
 *
 * <p>A read-only resource beside {@code /goals} rather than beneath it, so it shares no path with the
 * goals catalog: {@code /goals/projections} would sit in the same segment as {@code /goals/{goalId}}.
 * The member is addressed by the goal's own identifier and the collection by the access token; nothing
 * binds an owner from the request.
 *
 * <p>The two operations name their {@code operationId} explicitly. Springdoc otherwise derives it from
 * the method name and suffixes collisions ({@code list_6}, {@code read_7}), and a new {@code list} and
 * {@code read} would shift the suffixes of every operation registered after them.
 *
 * <p>Another user's goal answers {@code not-found} with the same {@code type}, {@code title}, {@code
 * status} and {@code detail} as a goal that does not exist, because the read goes through {@code
 * GoalService.read} and therefore through {@code OwnershipGuard}.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/goal-projections")
class GoalProjectionController {

    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;
    private static final String PROBLEM_SCHEMA_REF = "#/components/schemas/ProblemDetail";

    private static final String METHOD_DESCRIPTION =
            " Computed on every read from the goal as it is stored now and today's date in Asia/Ho_Chi_Minh, and"
                    + " never stored, so an edit to the goal shows in the next read and the same goal on the same"
                    + " day gives the same bytes. The projected amount at the target date is the future value of"
                    + " the allocated amount plus the future value of a level monthly contribution, compounded"
                    + " monthly at one twelfth of the user's expected annual return, with contributions at the"
                    + " end of each month; the required contribution solves the same equation for the target and"
                    + " is rounded up to a whole 1,000 VND; the status compares the planned contribution with it"
                    + " against the configured system reference thresholds. An output that cannot be derived is"
                    + " null, never 0, and its reason is in unavailableReasons. The expected return is the"
                    + " user's assumption and is never presented as guaranteed.";

    private final GoalProjectionService service;

    GoalProjectionController(GoalProjectionService service) {
        this.service = service;
    }

    @Operation(
            operationId = "listGoalProjections",
            summary = "Project every goal of the signed-in user",
            description =
                    "One projection per goal the viewer owns, in the goal list's default order: nearest target"
                            + " date first, then by name. An empty array when there are no goals."
                            + METHOD_DESCRIPTION)
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "The viewer's goal projections, possibly empty",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = GoalProjectionResponse.class)),
                            examples = {
                                @ExampleObject(name = "no goals yet", value = "[]"),
                                @ExampleObject(
                                        name = "one goal whose target date has passed",
                                        value = GoalProjectionExamples.EXPIRED_LIST)
                            }))
    @ApiResponse(
            responseCode = "401",
            description = "No access token, or it has expired or been revoked",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "405",
            description = "Wrong HTTP method",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "406",
            description = "The requested media type cannot be produced",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @GetMapping
    List<GoalProjectionResponse> list() {
        return service.projectionsFor(CurrentUser.require()).stream()
                .map(GoalProjectionResponse::of)
                .toList();
    }

    @Operation(
            operationId = "readGoalProjection",
            summary = "Project one of the signed-in user's goals",
            description = "The projection of one goal the viewer owns." + METHOD_DESCRIPTION)
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "The goal's projection",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GoalProjectionResponse.class),
                            examples = {
                                @ExampleObject(
                                        name = "an active goal on track",
                                        value = GoalProjectionExamples.ON_TRACK),
                                @ExampleObject(
                                        name = "no planned monthly contribution",
                                        value = GoalProjectionExamples.CONTRIBUTION_MISSING)
                            }))
    @ApiResponse(
            responseCode = "400",
            description = "The goal identifier is not a UUID",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "401",
            description = "No access token, or it has expired or been revoked",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "404",
            description =
                    "No goal with this identifier is visible to the viewer: it does not exist, or it belongs to"
                            + " someone else. The two are deliberately indistinguishable.",
            content =
                    @Content(
                            mediaType = PROBLEM_JSON,
                            schema = @Schema(ref = PROBLEM_SCHEMA_REF),
                            examples =
                                    @ExampleObject(
                                            name = "absent or another user's",
                                            value =
                                                    """
                                                    {
                                                      "type": "urn:rikkaus:problem:not-found",
                                                      "title": "Resource not found",
                                                      "status": 404,
                                                      "detail": "Resource not found.",
                                                      "instance": "/api/v1/goal-projections/9d4b1f68-2c7e-4a35-b8f0-6e1a5d3c7b92",
                                                      "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83"
                                                    }
                                                    """)))
    @ApiResponse(
            responseCode = "405",
            description = "Wrong HTTP method",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "406",
            description = "The requested media type cannot be produced",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @GetMapping("/{goalId}")
    GoalProjectionResponse read(
            @Parameter(description = "The goal's identifier.", example = "9d4b1f68-2c7e-4a35-b8f0-6e1a5d3c7b92")
                    @PathVariable UUID goalId) {
        return GoalProjectionResponse.of(service.projectionFor(CurrentUser.require(), goalId));
    }
}
