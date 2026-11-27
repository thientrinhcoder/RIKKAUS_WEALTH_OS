package com.rikkaus.wealth.cashflow.health.api;

import com.rikkaus.wealth.cashflow.health.application.FinancialHealthService;
import com.rikkaus.wealth.shared.api.ApiPaths;
import com.rikkaus.wealth.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The signed-in user's financial health for one month: the five indicators of the backbone, each with
 * its calculation and source records, and the six insight rules judged over them.
 *
 * <p>A singular resource with an optional {@code month}: the viewer comes from the access token and only
 * their records are read, so there is no identifier to supply and nothing for another user's data to
 * answer. The {@code operationId} is explicit so springdoc's derived names of other operations do not
 * shift.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/financial-health")
class FinancialHealthController {

    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;
    private static final String PROBLEM_SCHEMA_REF = "#/components/schemas/ProblemDetail";

    private final FinancialHealthService service;

    FinancialHealthController(FinancialHealthService service) {
        this.service = service;
    }

    @Operation(
            operationId = "readFinancialHealth",
            summary = "Compute the signed-in user's five financial-health indicators and judge the six insight rules",
            description =
                    "Always five indicators and six rules, in a fixed order. EMERGENCY_RESERVE_MONTHS is the CASH,"
                            + " BANK_ACCOUNT and TERM_DEPOSIT assets over the month's recurring essential expenses at"
                            + " their monthly equivalent (monthly x1, quarterly /3, yearly /12; one-time expenses never"
                            + " count), in months at one decimal. DEBT_PAYMENT_BURDEN is the month's"
                            + " DEBT_PRINCIPAL_PAYMENT and INTEREST_PAYMENT expense over its income. PASSIVE_INCOME_SHARE"
                            + " is passiveIncome.total over income and ESSENTIAL_EXPENSE_SHARE the essential-category"
                            + " expense over expense, each a six-decimal fraction rounded HALF_UP from the same VND"
                            + " totals GET /api/v1/cash-flow-reports/monthly returns for the month."
                            + " FORECAST_MINIMUM_CASH_BALANCE is minimumBalance.amount of GET /api/v1/cash-flow-forecast"
                            + " (the three months after the current one, whatever month is asked for). Bands and rule"
                            + " statuses compare the rounded value with thresholds; a value equal to a limit is not"
                            + " beyond it. Thresholds are system reference values configured centrally and returned in"
                            + " thresholds, and assumptions returns the day, windows, category groups and rate, so a"
                            + " result can be reproduced. A USD record with no declared rate is left out with amountVnd"
                            + " null and makes the indicator PARTIAL, or UNAVAILABLE when nothing else is left; a"
                            + " missing denominator makes it UNAVAILABLE with value null, never 0. A rule over a PARTIAL"
                            + " or UNAVAILABLE input is NOT_EVALUATED, never OK. A user with no record gets five"
                            + " UNAVAILABLE indicators and six NOT_EVALUATED rules. Computed on every read; nothing is"
                            + " stored. No Vietnamese text is published: the client maps every constant to its copy.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "The five indicators and six rules, possibly all unavailable for a user with no record yet",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FinancialHealthResponse.class),
                            examples = {
                                @ExampleObject(
                                        name = "the design #81 baseline: one rule fires, income concentration",
                                        value = FinancialHealthExamples.BASELINE),
                                @ExampleObject(
                                        name = "the same records before any rate is declared: partial, no rule judged",
                                        value = FinancialHealthExamples.PARTIAL),
                                @ExampleObject(
                                        name = "a user with no record yet",
                                        value = FinancialHealthExamples.EMPTY)
                            }))
    @ApiResponse(
            responseCode = "400",
            description = "month is present but blank or not a calendar month in the form YYYY-MM.",
            content =
                    @Content(
                            mediaType = PROBLEM_JSON,
                            schema = @Schema(ref = PROBLEM_SCHEMA_REF),
                            examples =
                                    @ExampleObject(
                                            name = "month not in the form YYYY-MM",
                                            value =
                                                    """
                                                    {
                                                      "type": "urn:rikkaus:problem:validation-failed",
                                                      "title": "Request validation failed",
                                                      "status": 400,
                                                      "detail": "Request validation failed.",
                                                      "instance": "/api/v1/financial-health",
                                                      "correlationId": "3c8e1f52-7a04-4b9d-9e61-d2a75b0c4f18",
                                                      "errors": [
                                                        {
                                                          "field": "month",
                                                          "message": "must be a calendar month in the form YYYY-MM"
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
            responseCode = "406",
            description = "The requested media type cannot be produced",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @GetMapping
    FinancialHealthResponse financialHealth(
            // Bound as text so a malformed value reaches the service and is reported as validation-failed
            // naming the field, as the monthly report does.
            @Parameter(
                            description =
                                    "The calendar month the cash-flow indicators read, YYYY-MM. Absent means the current"
                                            + " month in Asia/Ho_Chi_Minh. Past and future months are accepted.",
                            schema = @Schema(type = "string", pattern = "^\\d{4}-\\d{2}$", example = "2026-11"))
                    @RequestParam(name = "month", required = false)
                    String month) {
        return FinancialHealthResponse.of(service.healthFor(CurrentUser.require(), month));
    }
}
