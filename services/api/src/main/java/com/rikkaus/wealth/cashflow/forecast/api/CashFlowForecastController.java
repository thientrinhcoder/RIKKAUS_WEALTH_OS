package com.rikkaus.wealth.cashflow.forecast.api;

import com.rikkaus.wealth.cashflow.forecast.application.CashFlowForecastService;
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
 * The signed-in user's projected cash for the next three months.
 *
 * <p>A read-only singleton beside the catalog, like {@code /cash-flow-occurrences} it is built from.
 * The viewer comes from the access token and only their records are read; there is no path variable
 * and no identifier to supply.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/cash-flow-forecast")
class CashFlowForecastController {

    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;
    private static final String PROBLEM_SCHEMA_REF = "#/components/schemas/ProblemDetail";

    private final CashFlowForecastService service;

    CashFlowForecastController(CashFlowForecastService service) {
        this.service = service;
    }

    @Operation(
            summary = "Project the signed-in user's cash over three whole months",
            description =
                    "Three calendar months starting at `from`, or at the month after today in Asia/Ho_Chi_Minh"
                            + " when it is absent. Only records the user can see contribute: the occurrences of"
                            + " their one-time and recurring cash-flow entries, expanded with the rules of"
                            + " /cash-flow-occurrences; the payments of their liabilities, counted from"
                            + " nextPaymentDate every recurringPaymentFrequency and never after maturityDate; and"
                            + " their CASH and BANK_ACCOUNT assets as the opening balance, applied at the start of"
                            + " the first day. A liability with a recurring entry linked to it that overlaps the"
                            + " horizon is counted through that entry only. USD amounts are converted with the"
                            + " user's declared rate and each amount is rounded to whole đồng before it is added,"
                            + " so a month's items add up to its totals exactly. The lowest position is judged on"
                            + " the opening balance and at the end of every day an amount lands. Every record"
                            + " that could have contributed and does not is listed in excludedSources with its"
                            + " reason; without a declared rate, USD records are excluded and the months they"
                            + " fall in are partial rather than the request failing. The days between today and"
                            + " the horizon are not projected and are named in assumptions.notProjected."
                            + " Computed on every read and never stored. The same records, day and `from` give"
                            + " the same bytes.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "The forecast. Always three months, possibly with no items.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CashFlowForecastResponse.class),
                            examples =
                                    @ExampleObject(
                                            name = "a salary, a mortgage and a loan from family without a schedule",
                                            value =
                                                    """
                                                    {
                                                      "horizon": {
                                                        "from": "2026-12-01",
                                                        "to": "2027-02-28",
                                                        "months": ["2026-12", "2027-01", "2027-02"]
                                                      },
                                                      "openingBalance": {
                                                        "amount": 85000000.00,
                                                        "appliedOn": "2026-12-01",
                                                        "sources": [
                                                          {
                                                            "sourceType": "ASSET",
                                                            "sourceId": "3d7a51c2-8f04-4b6e-9a21-c5e0f8b2d419",
                                                            "category": "BANK_ACCOUNT",
                                                            "valuationDate": "2026-11-20",
                                                            "amount": 85000000.00,
                                                            "currency": "VND",
                                                            "amountVnd": 85000000.00
                                                          }
                                                        ]
                                                      },
                                                      "months": [
                                                        {
                                                          "month": "2026-12",
                                                          "income": 45000000.00,
                                                          "expense": 18500000.00,
                                                          "net": 26500000.00,
                                                          "openingBalance": 85000000.00,
                                                          "closingBalance": 111500000.00,
                                                          "lowestBalance": 85000000.00,
                                                          "lowestBalanceDate": "2026-12-01",
                                                          "partial": false,
                                                          "items": [
                                                            {
                                                              "date": "2026-12-05",
                                                              "direction": "INCOME",
                                                              "sourceType": "CASH_FLOW_ENTRY",
                                                              "sourceId": "9b4d6f12-7e3a-4c58-b1d9-0a2e5c7f8d61",
                                                              "category": "SALARY",
                                                              "amount": 45000000.00,
                                                              "currency": "VND",
                                                              "amountVnd": 45000000.00,
                                                              "cadence": "MONTHLY",
                                                              "sequence": 12,
                                                              "shiftedToMonthEnd": false
                                                            },
                                                            {
                                                              "date": "2026-12-15",
                                                              "direction": "EXPENSE",
                                                              "sourceType": "LIABILITY",
                                                              "sourceId": "8e2f4a90-1c37-4d5b-b6a8-0f9d3e7c2b14",
                                                              "category": "BANK_LOAN",
                                                              "amount": 18500000.00,
                                                              "currency": "VND",
                                                              "amountVnd": 18500000.00,
                                                              "cadence": "MONTHLY",
                                                              "sequence": 1,
                                                              "shiftedToMonthEnd": false
                                                            }
                                                          ]
                                                        },
                                                        {
                                                          "month": "2027-01",
                                                          "income": 45000000.00,
                                                          "expense": 18500000.00,
                                                          "net": 26500000.00,
                                                          "openingBalance": 111500000.00,
                                                          "closingBalance": 138000000.00,
                                                          "lowestBalance": 111500000.00,
                                                          "lowestBalanceDate": "2027-01-01",
                                                          "partial": true,
                                                          "items": [
                                                            {
                                                              "date": "2027-01-05",
                                                              "direction": "INCOME",
                                                              "sourceType": "CASH_FLOW_ENTRY",
                                                              "sourceId": "9b4d6f12-7e3a-4c58-b1d9-0a2e5c7f8d61",
                                                              "category": "SALARY",
                                                              "amount": 45000000.00,
                                                              "currency": "VND",
                                                              "amountVnd": 45000000.00,
                                                              "cadence": "MONTHLY",
                                                              "sequence": 13,
                                                              "shiftedToMonthEnd": false
                                                            },
                                                            {
                                                              "date": "2027-01-15",
                                                              "direction": "EXPENSE",
                                                              "sourceType": "LIABILITY",
                                                              "sourceId": "8e2f4a90-1c37-4d5b-b6a8-0f9d3e7c2b14",
                                                              "category": "BANK_LOAN",
                                                              "amount": 18500000.00,
                                                              "currency": "VND",
                                                              "amountVnd": 18500000.00,
                                                              "cadence": "MONTHLY",
                                                              "sequence": 2,
                                                              "shiftedToMonthEnd": false
                                                            }
                                                          ]
                                                        },
                                                        {
                                                          "month": "2027-02",
                                                          "income": 45000000.00,
                                                          "expense": 18500000.00,
                                                          "net": 26500000.00,
                                                          "openingBalance": 138000000.00,
                                                          "closingBalance": 164500000.00,
                                                          "lowestBalance": 138000000.00,
                                                          "lowestBalanceDate": "2027-02-01",
                                                          "partial": false,
                                                          "items": [
                                                            {
                                                              "date": "2027-02-05",
                                                              "direction": "INCOME",
                                                              "sourceType": "CASH_FLOW_ENTRY",
                                                              "sourceId": "9b4d6f12-7e3a-4c58-b1d9-0a2e5c7f8d61",
                                                              "category": "SALARY",
                                                              "amount": 45000000.00,
                                                              "currency": "VND",
                                                              "amountVnd": 45000000.00,
                                                              "cadence": "MONTHLY",
                                                              "sequence": 14,
                                                              "shiftedToMonthEnd": false
                                                            },
                                                            {
                                                              "date": "2027-02-15",
                                                              "direction": "EXPENSE",
                                                              "sourceType": "LIABILITY",
                                                              "sourceId": "8e2f4a90-1c37-4d5b-b6a8-0f9d3e7c2b14",
                                                              "category": "BANK_LOAN",
                                                              "amount": 18500000.00,
                                                              "currency": "VND",
                                                              "amountVnd": 18500000.00,
                                                              "cadence": "MONTHLY",
                                                              "sequence": 3,
                                                              "shiftedToMonthEnd": false
                                                            }
                                                          ]
                                                        }
                                                      ],
                                                      "minimumBalance": {
                                                        "date": "2026-12-01",
                                                        "month": "2026-12",
                                                        "amount": 85000000.00,
                                                        "negative": false,
                                                        "referenceThreshold": 18500000.00,
                                                        "status": "ADEQUATE"
                                                      },
                                                      "excludedSources": [
                                                        {
                                                          "sourceType": "LIABILITY",
                                                          "sourceId": "c41b7e2d-6a95-4f08-8d3c-27e1b9f05a6e",
                                                          "reason": "NO_PAYMENT_SCHEDULE",
                                                          "affectedMonths": ["2027-01"],
                                                          "linkedEntryIds": []
                                                        }
                                                      ],
                                                      "partial": true,
                                                      "assumptions": {
                                                        "asOfDate": "2026-11-22",
                                                        "timeZone": "Asia/Ho_Chi_Minh",
                                                        "currency": "VND",
                                                        "exchangeRate": {
                                                          "usdToVndRate": 25100.000000,
                                                          "effectiveDate": "2026-09-12"
                                                        },
                                                        "openingBalanceCategories": ["BANK_ACCOUNT", "CASH"],
                                                        "notProjected": {
                                                          "from": "2026-11-23",
                                                          "to": "2026-11-30"
                                                        }
                                                      }
                                                    }
                                                    """)))
    @ApiResponse(
            responseCode = "400",
            description =
                    "from is not a month in the form YYYY-MM, is before the month after today, or is more than 12"
                            + " months after the current month.",
            content =
                    @Content(
                            mediaType = PROBLEM_JSON,
                            schema = @Schema(ref = PROBLEM_SCHEMA_REF),
                            examples =
                                    @ExampleObject(
                                            name = "the current month",
                                            value =
                                                    """
                                                    {
                                                      "type": "urn:rikkaus:problem:validation-failed",
                                                      "title": "Request validation failed",
                                                      "status": 400,
                                                      "detail": "Request validation failed.",
                                                      "instance": "/api/v1/cash-flow-forecast",
                                                      "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
                                                      "errors": [
                                                        {
                                                          "field": "from",
                                                          "message": "must not be before the month after today"
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
    CashFlowForecastResponse forecast(
            // Bound as text and optional to Spring, for the reason CashFlowOccurrenceController gives: a
            // malformed value reaches ForecastHorizon and is reported as validation-failed naming the field.
            @Parameter(
                            description =
                                    "The first month, YYYY-MM. Defaults to the month after today; from that month to 12"
                                            + " months after the current one.",
                            schema = @Schema(type = "string", pattern = "^\\d{4}-\\d{2}$", example = "2026-12"))
                    @RequestParam(name = "from", required = false)
                    String from) {
        return CashFlowForecastResponse.of(service.forecastFor(CurrentUser.require(), from));
    }
}
