package com.rikkaus.wealth.settings.api;

import com.rikkaus.wealth.settings.application.SettingsService;
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
 * The exchange-rate settings resource: read the rate in use, declare a new one.
 *
 * <p>A singleton addressed by the access token. There is no identifier in the path and no request
 * parameter, so there is nothing a caller could substitute to reach another user's settings — the
 * mechanism the decision record's "never trust a client-supplied userId" rule asks for, rather than a
 * check that could be forgotten.
 *
 * <p>{@code PUT} rather than {@code PATCH} because the request carries the whole declaration: a rate
 * without its effective date is not a state this resource has, so there is no partial update to
 * express.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/settings/exchange-rate")
class ExchangeRateSettingsController {

    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;
    private static final String PROBLEM_SCHEMA_REF = "#/components/schemas/ProblemDetail";

    private final SettingsService settingsService;

    ExchangeRateSettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Operation(
            summary = "Read the reporting currency and the declared USD/VND rate",
            description =
                    "Returns VND as the fixed reporting currency together with the rate the user declared, "
                            + "the date they declared it effective, and when the server stored that "
                            + "declaration. The three rate fields are null until a rate has been declared; "
                            + "that is the rate-needed state, not an error.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "The settings of the signed-in user",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ExchangeRateResponse.class),
                            examples = {
                                @ExampleObject(
                                        name = "no rate declared yet",
                                        value =
                                                """
                                                {
                                                  "baseCurrency": "VND",
                                                  "usdToVndRate": null,
                                                  "effectiveDate": null,
                                                  "updatedAt": null
                                                }
                                                """),
                                @ExampleObject(
                                        name = "a declared rate",
                                        value =
                                                """
                                                {
                                                  "baseCurrency": "VND",
                                                  "usdToVndRate": 25100.000000,
                                                  "effectiveDate": "2026-09-12",
                                                  "updatedAt": "2026-09-12T02:42:11.503Z"
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
    ExchangeRateResponse read() {
        return ExchangeRateResponse.of(settingsService.settingsFor(CurrentUser.require()));
    }

    @Operation(
            summary = "Declare the USD/VND exchange rate",
            description =
                    "Stores the rate and the date the user says it took effect, and stamps the server's own "
                            + "record of when it was stored. The effective date must not be in the future; it "
                            + "is judged as a Vietnamese civil date, not a UTC one.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "The rate was stored",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ExchangeRateResponse.class)))
    @ApiResponse(
            responseCode = "400",
            description = "The rate is missing or not positive, or the effective date is missing or in the future",
            content =
                    @Content(
                            mediaType = PROBLEM_JSON,
                            schema = @Schema(ref = PROBLEM_SCHEMA_REF),
                            examples =
                                    @ExampleObject(
                                            name = "effective date in the future",
                                            value =
                                                    """
                                                    {
                                                      "type": "urn:rikkaus:problem:validation-failed",
                                                      "title": "Request validation failed",
                                                      "status": 400,
                                                      "detail": "Request validation failed.",
                                                      "instance": "/api/v1/settings/exchange-rate",
                                                      "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
                                                      "errors": [
                                                        {
                                                          "field": "effectiveDate",
                                                          "message": "must not be in the future"
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
    ExchangeRateResponse declare(@Valid @RequestBody DeclareExchangeRateRequest request) {
        return ExchangeRateResponse.of(
                settingsService.declareExchangeRate(
                        CurrentUser.require(), request.usdToVndRate(), request.effectiveDate()));
    }
}
