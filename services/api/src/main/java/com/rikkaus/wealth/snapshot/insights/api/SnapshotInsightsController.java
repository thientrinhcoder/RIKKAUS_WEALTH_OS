package com.rikkaus.wealth.snapshot.insights.api;

import com.rikkaus.wealth.shared.api.ApiPaths;
import com.rikkaus.wealth.shared.security.CurrentUser;
import com.rikkaus.wealth.snapshot.insights.application.SnapshotInsightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The signed-in user's wealth snapshot insights: the six rules of the backbone, each with what it
 * observed, the threshold it was judged against and the records behind it.
 *
 * <p>A sub-resource of the singular {@code /wealth-snapshot}, with no identifier: the viewer comes from
 * the access token and only their records are read, so there is nothing for another user's data to
 * answer. The {@code operationId} is explicit so springdoc's derived names of other operations do not
 * shift.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/wealth-snapshot/insights")
class SnapshotInsightsController {

    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;
    private static final String PROBLEM_SCHEMA_REF = "#/components/schemas/ProblemDetail";

    private final SnapshotInsightService service;

    SnapshotInsightsController(SnapshotInsightService service) {
        this.service = service;
    }

    @Operation(
            operationId = "readWealthSnapshotInsights",
            summary = "Judge the six insight rules over the signed-in user's wealth snapshot",
            description =
                    "Always six insights, in a fixed order. CATEGORY_CONCENTRATION is the largest asset category"
                            + " over total assets; DEBT_TO_ASSET_RATIO is total liabilities over total assets;"
                            + " LOW_LIQUIDITY_SHARE is the assets marked LOW over total assets, each a six-decimal"
                            + " fraction rounded HALF_UP from the same VND totals GET /api/v1/wealth-snapshot returns."
                            + " UPCOMING_LIABILITY_DUE is the days from asOfDate to the nearest next payment or"
                            + " maturity date, negative when overdue. STALE_VALUATION is the age in days of the"
                            + " oldest asset valuation; STALE_EXCHANGE_RATE the age of the declared rate's effective"
                            + " date. An insight is ATTENTION when its observed value compares with its threshold as"
                            + " threshold.comparison says, a value equal to a maximum being OK. Thresholds are system"
                            + " reference values configured centrally and returned in thresholds, so a result can be"
                            + " reproduced. An insight that cannot be observed — no asset, no liability, no due date,"
                            + " no declared rate where one is needed, total assets of zero — is UNAVAILABLE with"
                            + " observed null and unavailableReason set; nothing unavailable is reported as zero. A"
                            + " user with no record gets six UNAVAILABLE insights. Computed on every read; nothing is"
                            + " stored. No Vietnamese text is published: the client maps every constant to its copy.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "The six insights, possibly all unavailable for a user with no record yet",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SnapshotInsightsResponse.class),
                            examples = {
                                @ExampleObject(
                                        name = "the design fixture: 12,002,000,000 VND of assets, a stale rate",
                                        value = SnapshotInsightsExamples.FIXTURE),
                                @ExampleObject(
                                        name = "a USD account and a USD loan before any rate is declared",
                                        value = SnapshotInsightsExamples.MISSING_RATE),
                                @ExampleObject(
                                        name = "a user with no record yet",
                                        value = SnapshotInsightsExamples.EMPTY)
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
    SnapshotInsightsResponse insights() {
        return SnapshotInsightsResponse.of(service.insightsFor(CurrentUser.require()));
    }
}
