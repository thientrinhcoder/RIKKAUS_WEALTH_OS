package com.rikkaus.wealth.goals.api;

import com.rikkaus.wealth.goals.application.GoalService;
import com.rikkaus.wealth.goals.domain.GoalFields;
import com.rikkaus.wealth.shared.api.ApiPaths;
import com.rikkaus.wealth.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Financial goals: list, create, read, replace, remove.
 *
 * <p>The collection is addressed by the access token and the members by their own identifier. The only
 * path variable is the goal's id, never the owner's: an ArchUnit rule fails the compilation gate on a
 * controller method that binds a {@code userId}, {@code ownerId} or {@code accountId} from the request.
 *
 * <p>Another user's goal identifier answers {@code not-found}, with the same {@code type}, {@code
 * title}, {@code status} and {@code detail} a genuinely missing identifier produces; the two differ
 * only in {@code instance} and {@code correlationId}. A 403 would turn a list of identifiers into an
 * enumeration oracle, and {@code OwnershipGuard} is where that is decided for every route here.
 *
 * <p>{@code GET} on the collection takes no query parameter. Search and filter belong to issue #101,
 * and pagination is listed as deliberately absent in the API conventions.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/goals")
class GoalController {

    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;
    private static final String PROBLEM_SCHEMA_REF = "#/components/schemas/ProblemDetail";

    private final GoalService goalService;

    GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @Operation(
            summary = "List the signed-in user's goals",
            description =
                    "Returns every goal the viewer owns, nearest target date first and then by name. An empty "
                            + "list is an empty array and a 200, not a 404. Every amount is in VND. No "
                            + "projection is included: completion, remaining amount, remaining time, required "
                            + "contribution and status arrive with issue #89.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "The viewer's goals, possibly empty",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = GoalResponse.class)),
                            examples = {
                                @ExampleObject(name = "no goals yet", value = "[]"),
                                @ExampleObject(
                                        name = "two goals",
                                        value =
                                                """
                                                [
                                                  {
                                                    "id": "5c2e7d31-9a4f-4b8e-a1d6-3f7b2c9e8a40",
                                                    "name": "Quỹ dự phòng 6 tháng",
                                                    "type": "EMERGENCY_FUND",
                                                    "currency": "VND",
                                                    "targetAmount": 270000000.00,
                                                    "currentAllocatedAmount": 90000000.00,
                                                    "targetDate": "2027-06-30",
                                                    "plannedMonthlyContribution": 15000000.00,
                                                    "expectedAnnualReturnPercent": 3.00,
                                                    "createdAt": "2026-11-14T10:35:40.512Z",
                                                    "updatedAt": "2026-11-14T10:35:40.512Z"
                                                  },
                                                  {
                                                    "id": "9d4b1f68-2c7e-4a35-b8f0-6e1a5d3c7b92",
                                                    "name": "Mua căn hộ",
                                                    "type": "HOME_PURCHASE",
                                                    "currency": "VND",
                                                    "targetAmount": 3500000000.00,
                                                    "currentAllocatedAmount": 1200000000.00,
                                                    "targetDate": "2030-12-31",
                                                    "plannedMonthlyContribution": 25000000.00,
                                                    "expectedAnnualReturnPercent": 7.00,
                                                    "createdAt": "2026-11-14T10:36:02.118Z",
                                                    "updatedAt": "2026-11-14T10:36:02.118Z"
                                                  }
                                                ]
                                                """)
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
    List<GoalResponse> list() {
        return goalService.listFor(CurrentUser.require()).stream().map(GoalResponse::of).toList();
    }

    @Operation(
            summary = "Create a goal",
            description =
                    "Creates a goal owned by the signed-in user from any of the seven templates. The target "
                            + "amount must be greater than zero, the allocated amount may not exceed it, and the "
                            + "target date must be after today as a Vietnamese civil date. The expected return "
                            + "is the user's own assumption, 0 to 30 percent, and is informational only.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "201",
            description = "The goal was created",
            // Declared so a reader of the contract alone knows the header is there. CorsConfiguration
            // exposes only X-Correlation-Id, so a browser client cannot read Location; the body carries the
            // same identifier, which is why that is a documented limitation rather than a reason to widen a
            // CORS policy from inside this slice.
            headers =
                    @Header(
                            name = "Location",
                            description = "The new goal's URI, as /api/v1/goals/{goalId}.",
                            schema = @Schema(type = "string")),
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GoalResponse.class)))
    @ApiResponse(
            responseCode = "400",
            description =
                    "A required field is missing, an amount is negative, zero where it must be positive or too "
                            + "precise, the allocated amount exceeds the target, the return is outside 0 to 30, "
                            + "or the target date is not after today. An unrecognised type is a "
                            + "malformed-request instead, because the body cannot be parsed.",
            content =
                    @Content(
                            mediaType = PROBLEM_JSON,
                            schema = @Schema(ref = PROBLEM_SCHEMA_REF),
                            examples = {
                                @ExampleObject(
                                        name = "target amount of zero",
                                        value =
                                                """
                                                {
                                                  "type": "urn:rikkaus:problem:validation-failed",
                                                  "title": "Request validation failed",
                                                  "status": 400,
                                                  "detail": "Request validation failed.",
                                                  "instance": "/api/v1/goals",
                                                  "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
                                                  "errors": [
                                                    {
                                                      "field": "targetAmount",
                                                      "message": "must be greater than zero"
                                                    }
                                                  ]
                                                }
                                                """),
                                @ExampleObject(
                                        name = "allocated more than the target",
                                        value =
                                                """
                                                {
                                                  "type": "urn:rikkaus:problem:validation-failed",
                                                  "title": "Request validation failed",
                                                  "status": 400,
                                                  "detail": "Request validation failed.",
                                                  "instance": "/api/v1/goals",
                                                  "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
                                                  "errors": [
                                                    {
                                                      "field": "currentAllocatedAmount",
                                                      "message": "must not exceed the target amount"
                                                    }
                                                  ]
                                                }
                                                """),
                                @ExampleObject(
                                        name = "target date not after today",
                                        value =
                                                """
                                                {
                                                  "type": "urn:rikkaus:problem:validation-failed",
                                                  "title": "Request validation failed",
                                                  "status": 400,
                                                  "detail": "Request validation failed.",
                                                  "instance": "/api/v1/goals",
                                                  "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
                                                  "errors": [
                                                    {
                                                      "field": "targetDate",
                                                      "message": "must be after today"
                                                    }
                                                  ]
                                                }
                                                """)
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
    @ApiResponse(
            responseCode = "415",
            description = "The request body is not JSON",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @PostMapping
    ResponseEntity<GoalResponse> create(@Valid @RequestBody GoalRequest request) {
        GoalResponse created = GoalResponse.of(goalService.create(CurrentUser.require(), fieldsOf(request)));
        return ResponseEntity.created(URI.create(ApiPaths.V1 + "/goals/" + created.id())).body(created);
    }

    @Operation(
            summary = "Read one goal",
            description =
                    "Returns the goal when the signed-in user owns it. An identifier belonging to another user "
                            + "answers not-found with the same body a missing identifier produces, so the "
                            + "response cannot be used to discover which identifiers exist.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "The goal",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GoalResponse.class)))
    @ApiResponse(
            responseCode = "400",
            description = "The identifier in the path is not a UUID",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "401",
            description = "No access token, or it has expired or been revoked",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "404",
            description = "No such goal, or it belongs to another user",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "405",
            description = "Wrong HTTP method",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "406",
            description = "The requested media type cannot be produced",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @GetMapping("/{goalId}")
    GoalResponse read(@PathVariable UUID goalId) {
        return GoalResponse.of(goalService.read(CurrentUser.require(), goalId));
    }

    @Operation(
            summary = "Replace a goal",
            description =
                    "Replaces every user-entered field of the goal, the type included. An omitted optional "
                            + "field is cleared, because this is a replacement rather than a patch; an omitted "
                            + "allocated amount becomes 0.00. A target date that differs from the stored one "
                            + "must be after today; keeping the stored date is always allowed, so a goal whose "
                            + "date has passed can still be edited.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "The goal as it now stands",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GoalResponse.class)))
    @ApiResponse(
            responseCode = "400",
            description =
                    "A required field is missing, an amount is negative, zero where it must be positive or too "
                            + "precise, the allocated amount exceeds the target, the return is outside 0 to 30, "
                            + "or a changed target date is not after today",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "401",
            description = "No access token, or it has expired or been revoked",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "404",
            description = "No such goal, it belongs to another user, or a concurrent request removed it first",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "405",
            description = "Wrong HTTP method",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "406",
            description = "The requested media type cannot be produced",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "415",
            description = "The request body is not JSON",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @PutMapping("/{goalId}")
    GoalResponse replace(@PathVariable UUID goalId, @Valid @RequestBody GoalRequest request) {
        return GoalResponse.of(goalService.replace(CurrentUser.require(), goalId, fieldsOf(request)));
    }

    @Operation(
            summary = "Remove a goal",
            description =
                    "Deletes the goal when the signed-in user owns it. Asset, liability and cash-flow records "
                            + "are untouched, as the accepted design's confirmation dialog states. Undo and a "
                            + "retention window belong to issue #104. A second delete of the same identifier "
                            + "answers not-found, the same answer any unknown identifier gets; two deletes that "
                            + "overlap give one 204 and not-found to the rest rather than a server error.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "204", description = "The goal no longer exists")
    @ApiResponse(
            responseCode = "400",
            description = "The identifier in the path is not a UUID",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "401",
            description = "No access token, or it has expired or been revoked",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "404",
            description = "No such goal, it belongs to another user, or a concurrent request removed it first",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "405",
            description = "Wrong HTTP method",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @DeleteMapping("/{goalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID goalId) {
        goalService.delete(CurrentUser.require(), goalId);
    }

    private static GoalFields fieldsOf(GoalRequest request) {
        return new GoalFields(
                request.name(),
                request.type(),
                request.targetAmount(),
                request.currentAllocatedAmount(),
                request.targetDate(),
                request.plannedMonthlyContribution(),
                request.expectedAnnualReturnPercent());
    }
}
