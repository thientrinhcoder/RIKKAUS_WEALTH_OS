package com.rikkaus.wealth.cashflow.api;

import com.rikkaus.wealth.cashflow.application.CashFlowOccurrenceService;
import com.rikkaus.wealth.cashflow.application.OccurrenceWindow;
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
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The dates the signed-in user's cash-flow entries fall on within a window of days.
 *
 * <p>A read-only collection beside the catalog rather than a sub-resource of one entry, because the
 * consumers that exist — the monthly report and the three-month forecast — need every entry in a
 * window at once. A screen showing one entry's schedule filters by {@code sourceEntryId}.
 *
 * <p>The viewer comes from the access token and only their entries are read. There is no path
 * variable and no identifier to supply, so there is nothing for another user's entries to answer.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/cash-flow-occurrences")
class CashFlowOccurrenceController {

    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;
    private static final String PROBLEM_SCHEMA_REF = "#/components/schemas/ProblemDetail";

    private final CashFlowOccurrenceService service;

    CashFlowOccurrenceController(CashFlowOccurrenceService service) {
        this.service = service;
    }

    @Operation(
            summary = "List the occurrences of the signed-in user's cash-flow entries in a window",
            description =
                    "Expands every one-time and recurring entry the viewer owns into the dates it falls on from "
                            + "`from` to `to`, both included. A recurring entry's first occurrence is its "
                            + "effective date and its last is on or before its end date, both included. A day of "
                            + "month the month does not have moves back to that month's last day and is marked "
                            + "shiftedToMonthEnd; the next month returns to the entry's own day. Each occurrence "
                            + "belongs to the month containing its date, so two windows that meet without sharing "
                            + "a day count every occurrence exactly once. Dates are calendar dates with no time or "
                            + "zone, so the answer does not depend on when or where it is asked. Occurrences are "
                            + "computed on every read and never stored; editing or deleting an entry changes its "
                            + "occurrences immediately. Ordered by date, then by sourceEntryId. An empty window is "
                            + "an empty array.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "The occurrences in the window, possibly none",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = CashFlowOccurrenceResponse.class)),
                            examples = {
                                @ExampleObject(name = "a window with no occurrence", value = "[]"),
                                @ExampleObject(
                                        name = "February 2026: a salary anchored on the 31st and a quarterly fee anchored on the 30th",
                                        value =
                                                """
                                                [
                                                  {
                                                    "sourceEntryId": "5c0e2b7a-91d4-4f3b-8a6e-2d7f1c9b4e30",
                                                    "occurrenceDate": "2026-02-28",
                                                    "sequence": 2,
                                                    "shiftedToMonthEnd": true,
                                                    "entryType": "EXPENSE",
                                                    "category": "EDUCATION",
                                                    "amount": 12500000.00,
                                                    "currency": "VND",
                                                    "recurrence": "QUARTERLY"
                                                  },
                                                  {
                                                    "sourceEntryId": "9b4d6f12-7e3a-4c58-b1d9-0a2e5c7f8d61",
                                                    "occurrenceDate": "2026-02-28",
                                                    "sequence": 2,
                                                    "shiftedToMonthEnd": true,
                                                    "entryType": "INCOME",
                                                    "category": "SALARY",
                                                    "amount": 45000000.00,
                                                    "currency": "VND",
                                                    "recurrence": "MONTHLY"
                                                  }
                                                ]
                                                """)
                            }))
    @ApiResponse(
            responseCode = "400",
            description =
                    "from or to is missing, blank, not a calendar date in the form YYYY-MM-DD, to is before from, "
                            + "or the window is 24 months or longer. Every problem is reported at once in the "
                            + "errors array.",
            content =
                    @Content(
                            mediaType = PROBLEM_JSON,
                            schema = @Schema(ref = PROBLEM_SCHEMA_REF),
                            examples = {
                                @ExampleObject(
                                        name = "to before from",
                                        value =
                                                """
                                                {
                                                  "type": "urn:rikkaus:problem:validation-failed",
                                                  "title": "Request validation failed",
                                                  "status": 400,
                                                  "detail": "Request validation failed.",
                                                  "instance": "/api/v1/cash-flow-occurrences",
                                                  "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
                                                  "errors": [
                                                    {
                                                      "field": "to",
                                                      "message": "must not be before from"
                                                    }
                                                  ]
                                                }
                                                """),
                                @ExampleObject(
                                        name = "from not a date and to missing",
                                        value =
                                                """
                                                {
                                                  "type": "urn:rikkaus:problem:validation-failed",
                                                  "title": "Request validation failed",
                                                  "status": 400,
                                                  "detail": "Request validation failed.",
                                                  "instance": "/api/v1/cash-flow-occurrences",
                                                  "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
                                                  "errors": [
                                                    {
                                                      "field": "from",
                                                      "message": "must be a calendar date in the form YYYY-MM-DD"
                                                    },
                                                    {
                                                      "field": "to",
                                                      "message": "must be present"
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
    @GetMapping
    List<CashFlowOccurrenceResponse> list(
            // Bound as text and declared optional to Spring so a missing or malformed value reaches
            // OccurrenceWindow and is reported as validation-failed naming the field, like every other
            // rejected input. The contract still publishes both as required dates.
            @Parameter(
                            required = true,
                            description = "The first day of the window, included.",
                            schema = @Schema(type = "string", format = "date", example = "2026-01-01"))
                    @RequestParam(name = "from", required = false)
                    String from,
            @Parameter(
                            required = true,
                            description =
                                    "The last day of the window, included. Not before from, and less than 24 months after it.",
                            schema = @Schema(type = "string", format = "date", example = "2026-03-31"))
                    @RequestParam(name = "to", required = false)
                    String to) {
        OccurrenceWindow window = OccurrenceWindow.of(from, to);
        return service.occurrencesFor(CurrentUser.require(), window).stream()
                .map(CashFlowOccurrenceResponse::of)
                .toList();
    }
}
