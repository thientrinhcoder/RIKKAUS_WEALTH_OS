package com.rikkaus.wealth.identity.api;

import com.rikkaus.wealth.identity.application.IdentityService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The personal profile resource: read it, change the display name.
 *
 * <p>It lives in the identity slice rather than a settings one even though the product presents it
 * under <em>Hồ sơ &amp; cài đặt</em>, because the display name is a column on {@code wealth.users} and
 * the slice rule in {@code ArchitectureRulesTest} forbids another slice from reaching it. Splitting
 * the product screen across two slices is the cost of keeping the user row owned in one place.
 *
 * <p>A singleton addressed by the access token. No path variable, no request parameter, nothing a
 * caller could substitute to read or rename somebody else.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/profile")
class ProfileController {

    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;
    private static final String PROBLEM_SCHEMA_REF = "#/components/schemas/ProblemDetail";

    private final IdentityService identityService;

    ProfileController(IdentityService identityService) {
        this.identityService = identityService;
    }

    @Operation(
            summary = "Read the personal profile",
            description =
                    "Returns the signed-in user's identifier, their verified Google email, and the display "
                            + "name they chose — or the Google one if they never changed it.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "The profile of the signed-in user",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProfileResponse.class)))
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
    ProfileResponse read() {
        return ProfileResponse.of(identityService.requireUser(CurrentUser.require()));
    }

    @Operation(
            summary = "Change the display name",
            description =
                    "Stores the name the user chose. It is kept apart from the name Google reports, so a "
                            + "later sign-in refreshes the Google value without reverting this one. The email "
                            + "is not editable: it comes from the verified identity token.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "The profile after the change",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProfileResponse.class)))
    @ApiResponse(
            responseCode = "400",
            description = "The display name is blank or longer than 120 characters",
            content =
                    @Content(
                            mediaType = PROBLEM_JSON,
                            schema = @Schema(ref = PROBLEM_SCHEMA_REF),
                            examples =
                                    @ExampleObject(
                                            name = "blank display name",
                                            value =
                                                    """
                                                    {
                                                      "type": "urn:rikkaus:problem:validation-failed",
                                                      "title": "Request validation failed",
                                                      "status": 400,
                                                      "detail": "Request validation failed.",
                                                      "instance": "/api/v1/profile",
                                                      "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
                                                      "errors": [
                                                        {
                                                          "field": "displayName",
                                                          "message": "must not be blank"
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
    @PutMapping
    ProfileResponse update(@Valid @RequestBody UpdateProfileRequest request) {
        return ProfileResponse.of(
                identityService.updatePreferredDisplayName(
                        CurrentUser.require(), request.displayName()));
    }
}
