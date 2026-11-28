package com.rikkaus.wealth.advisor.api;

import com.rikkaus.wealth.advisor.application.AdvisorReportService;
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
 * The signed-in user's basic advisor report: the wealth-snapshot insights, the financial-health rules and
 * the goal projections, grouped, with the suggestions the findings needing attention give rise to.
 *
 * <p>A singular resource with an optional {@code month}, like {@code /financial-health}: the viewer comes
 * from the access token and the three upstream services read only their records. The {@code operationId}
 * is explicit so springdoc's derived names of other operations do not shift.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/advisor-report")
class AdvisorReportController {

    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;
    private static final String PROBLEM_SCHEMA_REF = "#/components/schemas/ProblemDetail";

    private final AdvisorReportService service;

    AdvisorReportController(AdvisorReportService service) {
        this.service = service;
    }

    @Operation(
            operationId = "readAdvisorReport",
            summary = "Compose the signed-in user's rule-based advisor report from the insights, health rules and goals",
            description =
                    "Composes, never recalculates: every finding repeats the observed value and threshold that GET"
                            + " /api/v1/wealth-snapshot/insights, GET /api/v1/financial-health?month= and GET"
                            + " /api/v1/goal-projections return for the same user and month, and names the records"
                            + " behind it with the route that reads each. Findings are grouped by status: unevaluated"
                            + " (UNAVAILABLE, NOT_EVALUATED, a goal without a status, with the upstream reason),"
                            + " positive (OK, ON_TRACK) and attention (ATTENTION, AT_RISK, BEHIND). Suggestions are"
                            + " derived from attention only: each ATTENTION finding has a tier (1 upcoming obligations"
                            + " and liquidity, 2 the period's cash flow, 3 reserve and structure, 4 goals, 5 data"
                            + " freshness) and an action family; findings of one family that share a record"
                            + " (sourceType and sourceId) merge into one suggestion, which keeps the smallest tier and"
                            + " lists every finding. Suggestions are ranked by tier, then the nearest due date, then"
                            + " the largest relative deviation from the threshold, then the identifier, and the first"
                            + " priorityMarkerCount (3) carry priority. The advisor defines no threshold and stores"
                            + " nothing; the same records on the same day give the same bytes. A user with no record"
                            + " gets twelve unevaluated findings and empty groups, never 404. No Vietnamese text is"
                            + " published: the client maps every constant to its copy; action and disclaimer are"
                            + " reference English wording.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "The report, possibly with every finding unevaluated for a user with no record yet",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AdvisorReportResponse.class),
                            examples = {
                                @ExampleObject(
                                        name = "the design #90 baseline: eight findings need attention, eight suggestions",
                                        value = AdvisorReportExamples.BASELINE),
                                @ExampleObject(
                                        name = "a stressed month: three tier-1 findings merge into one suggestion",
                                        value = AdvisorReportExamples.STRESS),
                                @ExampleObject(name = "a user with no record yet", value = AdvisorReportExamples.EMPTY)
                            }))
    @ApiResponse(
            responseCode = "400",
            description =
                    "month is present but blank or not a calendar month in the form YYYY-MM; the same answer GET"
                            + " /api/v1/financial-health gives.",
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
                                                      "instance": "/api/v1/advisor-report",
                                                      "correlationId": "8d2b6e14-5c3a-4f97-b1e0-73a9c4d2f605",
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
    AdvisorReportResponse advisorReport(
            // Bound as text and handed to the financial-health service unchanged, so a malformed value is
            // refused exactly as GET /financial-health refuses it.
            @Parameter(
                            description =
                                    "The calendar month the FINANCIAL_HEALTH findings read, YYYY-MM. Absent means the"
                                            + " current month in Asia/Ho_Chi_Minh. The snapshot and goal findings are"
                                            + " always as of today.",
                            schema = @Schema(type = "string", pattern = "^\\d{4}-\\d{2}$", example = "2026-11"))
                    @RequestParam(name = "month", required = false)
                    String month) {
        return AdvisorReportResponse.of(service.reportFor(CurrentUser.require(), month));
    }
}
