package com.rikkaus.wealth.onboarding.api;

import com.rikkaus.wealth.onboarding.application.OnboardingService;
import com.rikkaus.wealth.shared.api.ApiPaths;
import com.rikkaus.wealth.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The guided-onboarding resource: read the checklist, record progress through it.
 *
 * <p>A singleton addressed by the access token, like the exchange-rate settings. There is no
 * identifier in the path and no owner in the body, so there is nothing a caller could substitute to
 * reach another user's progress.
 *
 * <p>{@code PATCH} rather than {@code PUT} because the client records one fact at a time — the step it
 * is showing, then later the dashboard review, then completion — and should not have to restate the
 * others to do it.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/onboarding")
class OnboardingController {

    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;
    private static final String PROBLEM_SCHEMA_REF = "#/components/schemas/ProblemDetail";

    private final OnboardingService onboardingService;

    OnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @Operation(
            summary = "Read the guided-onboarding checklist",
            description =
                    "Returns the seven onboarding steps in backbone order, each done or not according to the "
                            + "user's own records at this moment, together with the step the user last viewed "
                            + "and whether they completed or dismissed the flow. A user who has recorded no "
                            + "progress reads as step 1, in progress; reading never creates progress.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "The checklist of the signed-in user",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = OnboardingResponse.class),
                            examples = {
                                @ExampleObject(
                                        name = "a new user",
                                        value =
                                                """
                                                {
                                                  "status": "IN_PROGRESS",
                                                  "currentStep": 1,
                                                  "steps": [
                                                    {"step": 1, "key": "EXCHANGE_RATE", "done": false},
                                                    {"step": 2, "key": "CASH_AND_BANK_ACCOUNTS", "done": false},
                                                    {"step": 3, "key": "MAJOR_ASSETS", "done": false},
                                                    {"step": 4, "key": "LIABILITIES", "done": false},
                                                    {"step": 5, "key": "RECURRING_CASH_FLOWS", "done": false},
                                                    {"step": 6, "key": "DASHBOARD_REVIEW", "done": false},
                                                    {"step": 7, "key": "FIRST_GOAL", "done": false}
                                                  ],
                                                  "dashboardReviewedAt": null,
                                                  "completedAt": null,
                                                  "dismissedAt": null
                                                }
                                                """),
                                @ExampleObject(
                                        name = "rate, bank account and apartment added, viewing step 4",
                                        value =
                                                """
                                                {
                                                  "status": "IN_PROGRESS",
                                                  "currentStep": 4,
                                                  "steps": [
                                                    {"step": 1, "key": "EXCHANGE_RATE", "done": true},
                                                    {"step": 2, "key": "CASH_AND_BANK_ACCOUNTS", "done": true},
                                                    {"step": 3, "key": "MAJOR_ASSETS", "done": true},
                                                    {"step": 4, "key": "LIABILITIES", "done": false},
                                                    {"step": 5, "key": "RECURRING_CASH_FLOWS", "done": false},
                                                    {"step": 6, "key": "DASHBOARD_REVIEW", "done": false},
                                                    {"step": 7, "key": "FIRST_GOAL", "done": false}
                                                  ],
                                                  "dashboardReviewedAt": null,
                                                  "completedAt": null,
                                                  "dismissedAt": null
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
    @GetMapping
    OnboardingResponse read() {
        return OnboardingResponse.of(onboardingService.checklistFor(CurrentUser.require()));
    }

    @Operation(
            summary = "Record progress through the guided onboarding",
            description =
                    "Records any of: the step the user is viewing, that they reviewed their first dashboard, "
                            + "and that they completed, dismissed or reopened the flow. Members left out or "
                            + "null are unchanged; an empty body changes nothing and returns the checklist. "
                            + "Whether the other six steps are done cannot be written: it is read from the "
                            + "user's records.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "The checklist after recording the progress",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = OnboardingResponse.class)))
    @ApiResponse(
            responseCode = "400",
            description =
                    "currentStep is outside 1 to 7 (validation-failed), or the body is not valid JSON, has an "
                            + "unknown status or a currentStep that is not a whole number (malformed-request)",
            content =
                    @Content(
                            mediaType = PROBLEM_JSON,
                            schema = @Schema(ref = PROBLEM_SCHEMA_REF),
                            examples =
                                    @ExampleObject(
                                            name = "step outside the checklist",
                                            value =
                                                    """
                                                    {
                                                      "type": "urn:rikkaus:problem:validation-failed",
                                                      "title": "Request validation failed",
                                                      "status": 400,
                                                      "detail": "Request validation failed.",
                                                      "instance": "/api/v1/onboarding",
                                                      "correlationId": "3d9a61c4-0b7e-4f28-9c53-e1a84f6b2d07",
                                                      "errors": [
                                                        {
                                                          "field": "currentStep",
                                                          "message": "must be between 1 and 7"
                                                        }
                                                      ]
                                                    }
                                                    """)))
    @ApiResponse(
            responseCode = "401",
            description = "No access token, or it has expired or been revoked",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "405",
            description = "Wrong HTTP method",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "415",
            description = "The request body is not JSON",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @PatchMapping
    OnboardingResponse recordProgress(@Valid @RequestBody UpdateOnboardingRequest request) {
        return OnboardingResponse.of(
                onboardingService.recordProgress(
                        CurrentUser.require(),
                        request.currentStep(),
                        request.dashboardReviewed(),
                        request.status()));
    }
}
