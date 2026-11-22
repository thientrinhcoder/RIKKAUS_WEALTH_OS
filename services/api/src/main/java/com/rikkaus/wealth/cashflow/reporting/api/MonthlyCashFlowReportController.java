package com.rikkaus.wealth.cashflow.reporting.api;

import com.rikkaus.wealth.cashflow.reporting.application.MonthlyCashFlowReportService;
import com.rikkaus.wealth.cashflow.reporting.application.ReportMonth;
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
 * The signed-in user's cash flow for one calendar month: totals, composition, passive income and the
 * occurrences every figure is summed from.
 *
 * <p>The month is a query parameter rather than a path segment, like {@code from} and {@code to} on
 * the occurrence route, so a malformed month is a {@code validation-failed} naming the field instead
 * of an unnamed 404. {@code /monthly} leaves room for another report beside it under the same
 * collection.
 *
 * <p>The viewer comes from the access token and only their entries and their declared rate are read.
 * There is no identifier to supply, so there is nothing for another user's data to answer.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/cash-flow-reports")
class MonthlyCashFlowReportController {

    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;
    private static final String PROBLEM_SCHEMA_REF = "#/components/schemas/ProblemDetail";

    private final MonthlyCashFlowReportService service;

    MonthlyCashFlowReportController(MonthlyCashFlowReportService service) {
        this.service = service;
    }

    @Operation(
            summary = "Report the signed-in user's cash flow for one calendar month",
            description =
                    "Expands the viewer's entries into the month exactly as GET /api/v1/cash-flow-occurrences does"
                            + " for the window from the first to the last day of the month, and sums those"
                            + " occurrences. Every amount is in VND. A USD entry is converted with the rate the"
                            + " user has declared, one occurrence at a time, rounded half-up to whole dong; the"
                            + " rate and its effective date are returned in exchangeRate. Without a declared rate a"
                            + " USD occurrence keeps its originalAmount, its amount is null, it is left out of every"
                            + " total, complete is false and unconverted says how much was left out. net is"
                            + " income minus expense. Each composition lists the categories present, largest"
                            + " first, with shares apportioned by largest remainder so they sum to exactly 100.0."
                            + " Passive income is income linked to an asset, grouped by asset; income in a"
                            + " passive-kind category with no link is reported as unassigned and not counted as"
                            + " passive. Every total equals the sum of the occurrences returned. A month with no"
                            + " occurrence is the same structure with zeros and empty arrays, and a share that"
                            + " cannot be computed is null. Past, current and future months are answered alike;"
                            + " nothing is compared with today and nothing is stored.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "The report, possibly of an empty month",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MonthlyCashFlowReportResponse.class),
                            examples = {
                                @ExampleObject(
                                        name = "September 2026 after declaring 1 USD = 25,100 VND",
                                        value =
                                                """
                                                {
                                                  "month": "2026-09",
                                                  "from": "2026-09-01",
                                                  "to": "2026-09-30",
                                                  "currency": "VND",
                                                  "exchangeRate": { "usdToVndRate": 25100.000000, "effectiveDate": "2026-09-12" },
                                                  "complete": true,
                                                  "totals": { "income": 43765000.00, "expense": 20020000.00, "net": 23745000.00 },
                                                  "unconverted": { "occurrenceCount": 0, "income": 0.00, "expense": 0.00 },
                                                  "incomeComposition": [
                                                    { "category": "SALARY", "amount": 40000000.00, "sharePercent": 91.4, "occurrenceCount": 1, "partial": false },
                                                    { "category": "DIVIDEND", "amount": 3765000.00, "sharePercent": 8.6, "occurrenceCount": 1, "partial": false }
                                                  ],
                                                  "expenseComposition": [
                                                    { "category": "HOUSING", "amount": 15000000.00, "sharePercent": 74.9, "occurrenceCount": 1, "partial": false },
                                                    { "category": "LIVING_COSTS", "amount": 5020000.00, "sharePercent": 25.1, "occurrenceCount": 1, "partial": false }
                                                  ],
                                                  "passiveIncome": {
                                                    "total": 0.00,
                                                    "sharePercentOfIncome": 0.0,
                                                    "byAsset": [],
                                                    "unassigned": { "amount": 3765000.00, "occurrenceCount": 1, "partial": false }
                                                  },
                                                  "occurrences": [
                                                    { "sourceEntryId": "2e7c9a41-5b3d-4f86-a0c2-8d1e6b9f3a57", "occurrenceDate": "2026-09-01", "sequence": 1, "shiftedToMonthEnd": false, "entryType": "EXPENSE", "category": "HOUSING", "recurrence": null, "linkedAssetId": null, "notes": "Tiền nhà", "originalAmount": 15000000.00, "originalCurrency": "VND", "amount": 15000000.00 },
                                                    { "sourceEntryId": "8f14b6d2-3a9e-4c71-b5d8-0e2a7c4f9b13", "occurrenceDate": "2026-09-05", "sequence": 1, "shiftedToMonthEnd": false, "entryType": "INCOME", "category": "SALARY", "recurrence": null, "linkedAssetId": null, "notes": "Lương", "originalAmount": 40000000.00, "originalCurrency": "VND", "amount": 40000000.00 },
                                                    { "sourceEntryId": "c3a85e17-9d2b-4f40-8e6a-5b1f0d7c2e94", "occurrenceDate": "2026-09-10", "sequence": 1, "shiftedToMonthEnd": false, "entryType": "EXPENSE", "category": "LIVING_COSTS", "recurrence": null, "linkedAssetId": null, "notes": "Phần mềm làm việc", "originalAmount": 200.00, "originalCurrency": "USD", "amount": 5020000.00 },
                                                    { "sourceEntryId": "5d9e2f73-1c6a-4b85-9f30-a7e4c8b1d026", "occurrenceDate": "2026-09-30", "sequence": 1, "shiftedToMonthEnd": false, "entryType": "INCOME", "category": "DIVIDEND", "recurrence": null, "linkedAssetId": null, "notes": "Cổ tức ETF Mỹ", "originalAmount": 150.00, "originalCurrency": "USD", "amount": 3765000.00 }
                                                  ]
                                                }
                                                """),
                                @ExampleObject(
                                        name = "the same month before any rate is declared",
                                        value =
                                                """
                                                {
                                                  "month": "2026-09",
                                                  "from": "2026-09-01",
                                                  "to": "2026-09-30",
                                                  "currency": "VND",
                                                  "exchangeRate": null,
                                                  "complete": false,
                                                  "totals": { "income": 40000000.00, "expense": 15000000.00, "net": 25000000.00 },
                                                  "unconverted": { "occurrenceCount": 2, "income": 150.00, "expense": 200.00 },
                                                  "incomeComposition": [
                                                    { "category": "SALARY", "amount": 40000000.00, "sharePercent": 100.0, "occurrenceCount": 1, "partial": false },
                                                    { "category": "DIVIDEND", "amount": 0.00, "sharePercent": 0.0, "occurrenceCount": 1, "partial": true }
                                                  ],
                                                  "expenseComposition": [
                                                    { "category": "HOUSING", "amount": 15000000.00, "sharePercent": 100.0, "occurrenceCount": 1, "partial": false },
                                                    { "category": "LIVING_COSTS", "amount": 0.00, "sharePercent": 0.0, "occurrenceCount": 1, "partial": true }
                                                  ],
                                                  "passiveIncome": {
                                                    "total": 0.00,
                                                    "sharePercentOfIncome": 0.0,
                                                    "byAsset": [],
                                                    "unassigned": { "amount": 0.00, "occurrenceCount": 1, "partial": true }
                                                  },
                                                  "occurrences": [
                                                    { "sourceEntryId": "2e7c9a41-5b3d-4f86-a0c2-8d1e6b9f3a57", "occurrenceDate": "2026-09-01", "sequence": 1, "shiftedToMonthEnd": false, "entryType": "EXPENSE", "category": "HOUSING", "recurrence": null, "linkedAssetId": null, "notes": "Tiền nhà", "originalAmount": 15000000.00, "originalCurrency": "VND", "amount": 15000000.00 },
                                                    { "sourceEntryId": "8f14b6d2-3a9e-4c71-b5d8-0e2a7c4f9b13", "occurrenceDate": "2026-09-05", "sequence": 1, "shiftedToMonthEnd": false, "entryType": "INCOME", "category": "SALARY", "recurrence": null, "linkedAssetId": null, "notes": "Lương", "originalAmount": 40000000.00, "originalCurrency": "VND", "amount": 40000000.00 },
                                                    { "sourceEntryId": "c3a85e17-9d2b-4f40-8e6a-5b1f0d7c2e94", "occurrenceDate": "2026-09-10", "sequence": 1, "shiftedToMonthEnd": false, "entryType": "EXPENSE", "category": "LIVING_COSTS", "recurrence": null, "linkedAssetId": null, "notes": "Phần mềm làm việc", "originalAmount": 200.00, "originalCurrency": "USD", "amount": null },
                                                    { "sourceEntryId": "5d9e2f73-1c6a-4b85-9f30-a7e4c8b1d026", "occurrenceDate": "2026-09-30", "sequence": 1, "shiftedToMonthEnd": false, "entryType": "INCOME", "category": "DIVIDEND", "recurrence": null, "linkedAssetId": null, "notes": "Cổ tức ETF Mỹ", "originalAmount": 150.00, "originalCurrency": "USD", "amount": null }
                                                  ]
                                                }
                                                """),
                                @ExampleObject(
                                        name = "a month with no occurrence",
                                        value =
                                                """
                                                {
                                                  "month": "2025-06",
                                                  "from": "2025-06-01",
                                                  "to": "2025-06-30",
                                                  "currency": "VND",
                                                  "exchangeRate": { "usdToVndRate": 25100.000000, "effectiveDate": "2026-09-12" },
                                                  "complete": true,
                                                  "totals": { "income": 0.00, "expense": 0.00, "net": 0.00 },
                                                  "unconverted": { "occurrenceCount": 0, "income": 0.00, "expense": 0.00 },
                                                  "incomeComposition": [],
                                                  "expenseComposition": [],
                                                  "passiveIncome": {
                                                    "total": 0.00,
                                                    "sharePercentOfIncome": null,
                                                    "byAsset": [],
                                                    "unassigned": { "amount": 0.00, "occurrenceCount": 0, "partial": false }
                                                  },
                                                  "occurrences": []
                                                }
                                                """)
                            }))
    @ApiResponse(
            responseCode = "400",
            description = "month is missing, blank, or not a calendar month in the form YYYY-MM.",
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
                                                      "instance": "/api/v1/cash-flow-reports/monthly",
                                                      "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
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
    @GetMapping("/monthly")
    MonthlyCashFlowReportResponse monthly(
            // Bound as text and declared optional to Spring so a missing or malformed value reaches
            // ReportMonth and is reported as validation-failed naming the field. The contract still
            // publishes it as required.
            @Parameter(
                            required = true,
                            description = "The calendar month to report, YYYY-MM.",
                            schema = @Schema(type = "string", pattern = "^\\d{4}-\\d{2}$", example = "2026-09"))
                    @RequestParam(name = "month", required = false)
                    String month) {
        ReportMonth reportMonth = ReportMonth.of(month);
        return MonthlyCashFlowReportResponse.of(service.reportFor(CurrentUser.require(), reportMonth));
    }
}
