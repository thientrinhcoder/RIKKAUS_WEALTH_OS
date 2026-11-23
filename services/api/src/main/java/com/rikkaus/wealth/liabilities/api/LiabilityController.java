package com.rikkaus.wealth.liabilities.api;

import com.rikkaus.wealth.liabilities.application.LiabilityService;
import com.rikkaus.wealth.liabilities.domain.LiabilityCategory;
import com.rikkaus.wealth.liabilities.domain.LiabilityDetails;
import com.rikkaus.wealth.liabilities.domain.LiabilityFilter;
import com.rikkaus.wealth.liabilities.domain.LiabilityTerms;
import com.rikkaus.wealth.liabilities.domain.PaymentSchedule;
import com.rikkaus.wealth.shared.api.ApiPaths;
import com.rikkaus.wealth.shared.deletion.DeletionImpact;
import com.rikkaus.wealth.shared.money.Currency;
import com.rikkaus.wealth.shared.search.QueryParameters;
import com.rikkaus.wealth.shared.search.SearchText;
import com.rikkaus.wealth.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The liability catalog: list, add, read, replace, preview a deletion, remove.
 *
 * <p>The collection is addressed by the access token and the members by their own identifier. The only
 * path variable is the liability's id, never the owner's. Another user's identifier answers {@code
 * not-found} with the body a missing identifier produces, and a collateral the viewer does not own
 * answers {@code validation-failed} with the body a non-existent asset produces; neither response says
 * which of the two it was. {@code GET} on the collection takes optional search, filter and sort
 * parameters, read as text and checked by {@code LiabilityFilter} so a rejected value is a {@code
 * validation-failed} naming the parameter.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/liabilities")
class LiabilityController {

    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;
    private static final String PROBLEM_SCHEMA_REF = "#/components/schemas/ProblemDetail";

    private final LiabilityService liabilityService;

    LiabilityController(LiabilityService liabilityService) {
        this.liabilityService = liabilityService;
    }

    @Operation(
            summary = "List the signed-in user's liabilities",
            description =
                    "Returns every liability the viewer owns, soonest next payment first, those with no next "
                            + "payment date after them, and then by name. An empty catalog is an empty array "
                            + "and a 200, not a 404. Amounts are reported in each liability's own currency; "
                            + "converting a USD balance to VND is the client's, using the rate from "
                            + "GET /api/v1/settings/exchange-rate. The optional q, category, currency and "
                            + "sort parameters narrow and reorder the catalog; without them the response is "
                            + "the whole catalog in the default order.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "The viewer's catalog, possibly empty",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array =
                                    @ArraySchema(schema = @Schema(implementation = LiabilityResponse.class)),
                            examples = {
                                @ExampleObject(
                                        name = "an empty catalog, or nothing matches the search",
                                        value = "[]"),
                                @ExampleObject(
                                        name = "a mortgage with collateral and a card balance",
                                        value =
                                                """
                                                [
                                                  {
                                                    "id": "9d2f4c81-0a6e-4b73-b5c2-3e8f1d7a6c45",
                                                    "name": "Thẻ tín dụng Techcombank",
                                                    "category": "CREDIT_CARD_BALANCE",
                                                    "outstandingBalance": 38500000.00,
                                                    "currency": "VND",
                                                    "interestRate": null,
                                                    "startDate": null,
                                                    "maturityDate": null,
                                                    "recurringPaymentAmount": null,
                                                    "recurringPaymentFrequency": null,
                                                    "nextPaymentDate": "2026-11-25",
                                                    "collateralAssetId": null,
                                                    "notes": null,
                                                    "createdAt": "2026-11-14T10:42:11.503Z",
                                                    "updatedAt": "2026-11-14T10:42:11.503Z"
                                                  },
                                                  {
                                                    "id": "5c0e2b7a-91d4-4f3e-8a6b-7d2c1e9f4a30",
                                                    "name": "Vay mua nhà Vietcombank",
                                                    "category": "BANK_LOAN",
                                                    "outstandingBalance": 2400000000.00,
                                                    "currency": "VND",
                                                    "interestRate": 8.5000,
                                                    "startDate": "2024-03-15",
                                                    "maturityDate": "2044-03-15",
                                                    "recurringPaymentAmount": 23500000.00,
                                                    "recurringPaymentFrequency": "MONTHLY",
                                                    "nextPaymentDate": "2026-12-05",
                                                    "collateralAssetId": "a81d4f27-6c3e-4b19-8f55-0d2e7c9a3b64",
                                                    "notes": "Thế chấp căn hộ Thảo Điền.",
                                                    "createdAt": "2026-11-14T10:40:02.117Z",
                                                    "updatedAt": "2026-11-14T10:40:02.117Z"
                                                  }
                                                ]
                                                """)
                            }))
    @ApiResponse(
            responseCode = "400",
            description =
                    "A search longer than 120 characters or containing U+0000, an unknown category or currency, or an unknown sort key. Every rejected parameter is named in errors, and the value sent is never "
                            + "repeated.",
            content =
                    @Content(
                            mediaType = PROBLEM_JSON,
                            schema = @Schema(ref = PROBLEM_SCHEMA_REF),
                            examples =
                                    @ExampleObject(
                                            name = "an unknown category",
                                            value =
                                                    """
                                                    {
                                                      "type": "urn:rikkaus:problem:validation-failed",
                                                      "title": "Request validation failed",
                                                      "status": 400,
                                                      "detail": "Request validation failed.",
                                                      "instance": "/api/v1/liabilities",
                                                      "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
                                                      "errors": [
                                                        {
                                                          "field": "category",
                                                          "message": "must be one of BANK_LOAN, PERSONAL_LOAN, MARGIN_LOAN, CREDIT_CARD_BALANCE, GUARANTEE_OBLIGATION, OTHER_LIABILITY"
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
    List<LiabilityResponse> list(
            @Parameter(
                            description = "Searches the name and notes." + SearchText.MATCHING_RULES,
                            schema = @Schema(type = "string", maxLength = SearchText.MAX_LENGTH),
                            example = "vietcombank")
                    @RequestParam(name = "q", required = false)
                    String q,
            @Parameter(
                            description =
                                    "Keeps the liabilities in these categories."
                                            + QueryParameters.MULTI_VALUE_RULES,
                            array =
                                    @ArraySchema(schema = @Schema(implementation = LiabilityCategory.class)),
                            example = "BANK_LOAN")
                    @RequestParam(name = "category", required = false)
                    List<String> category,
            @Parameter(
                            description =
                                    "Keeps the liabilities in these currencies."
                                            + QueryParameters.MULTI_VALUE_RULES,
                            array = @ArraySchema(schema = @Schema(implementation = Currency.class)),
                            example = "VND")
                    @RequestParam(name = "currency", required = false)
                    List<String> currency,
            @Parameter(
                            description =
                                    "nextPaymentDate (the default) lists the soonest payment "
                                            + "first; -nextPaymentDate the latest first; name and "
                                            + "-name by name. A liability with no next payment date is "
                                            + "last in both date orders."
                                            + QueryParameters.SORT_RULES,
                            schema =
                                    @Schema(
                                            type = "string",
                                            allowableValues = {
                                                    "nextPaymentDate",
                                                    "-nextPaymentDate",
                                                    "name",
                                                    "-name"
                                            },
                                            defaultValue = "nextPaymentDate"),
                            example = "name")
                    @RequestParam(name = "sort", required = false)
                    String sort) {
        LiabilityFilter filter = LiabilityFilter.of(q, category, currency, sort);
        return liabilityService.listFor(CurrentUser.require(), filter).stream()
                .map(LiabilityResponse::of)
                .toList();
    }

    @Operation(
            summary = "Add a liability to the catalog",
            description =
                    "Creates a liability owned by the signed-in user. Terms and schedule are recorded as stated "
                            + "and nothing is computed from them. The maturity date must not be before the "
                            + "start date, the next payment date must fall inside the term when the term is "
                            + "given, the recurring amount and frequency come as a pair, and a collateral "
                            + "must be an asset the signed-in user owns.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "201",
            description = "The liability was created",
            headers =
                    @Header(
                            name = "Location",
                            description =
                                    "The new liability's URI, as "
                                            + "/api/v1/liabilities/{liabilityId}.",
                            schema = @Schema(type = "string")),
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LiabilityResponse.class)))
    @ApiResponse(
            responseCode = "400",
            description =
                    "A required field is missing, an amount is negative or too precise, a recurring payment is "
                            + "not positive or is half-stated, a date is inconsistent, or the collateral is "
                            + "not an asset the signed-in user owns. An unrecognised category, currency or "
                            + "frequency is a malformed-request instead, because the body cannot be parsed.",
            content =
                    @Content(
                            mediaType = PROBLEM_JSON,
                            schema = @Schema(ref = PROBLEM_SCHEMA_REF),
                            examples = {
                                @ExampleObject(
                                        name = "collateral the viewer does not own",
                                        value =
                                                """
                                                {
                                                  "type": "urn:rikkaus:problem:validation-failed",
                                                  "title": "Request validation failed",
                                                  "status": 400,
                                                  "detail": "Request validation failed.",
                                                  "instance": "/api/v1/liabilities",
                                                  "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
                                                  "errors": [
                                                    {
                                                      "field": "collateralAssetId",
                                                      "message": "must identify an asset the signed-in user owns"
                                                    }
                                                  ]
                                                }
                                                """),
                                @ExampleObject(
                                        name = "maturity before the start date",
                                        value =
                                                """
                                                {
                                                  "type": "urn:rikkaus:problem:validation-failed",
                                                  "title": "Request validation failed",
                                                  "status": 400,
                                                  "detail": "Request validation failed.",
                                                  "instance": "/api/v1/liabilities",
                                                  "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
                                                  "errors": [
                                                    {
                                                      "field": "maturityDate",
                                                      "message": "must not be before the start date"
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
    ResponseEntity<LiabilityResponse> create(@Valid @RequestBody CreateLiabilityRequest request) {
        LiabilityResponse created =
                LiabilityResponse.of(
                        liabilityService.create(
                                CurrentUser.require(),
                                request.category(),
                                new LiabilityDetails(
                                        request.name(),
                                        request.outstandingBalance(),
                                        request.currency(),
                                        new LiabilityTerms(
                                                request.interestRate(), request.startDate(), request.maturityDate()),
                                        new PaymentSchedule(
                                                request.recurringPaymentAmount(),
                                                request.recurringPaymentFrequency(),
                                                request.nextPaymentDate()),
                                        request.collateralAssetId(),
                                        request.notes())));
        return ResponseEntity.created(URI.create(ApiPaths.V1 + "/liabilities/" + created.id()))
                .body(created);
    }

    @Operation(
            summary = "Read one liability",
            description =
                    "Returns the liability when the signed-in user owns it. An identifier belonging to another "
                            + "user answers not-found with the same body a missing identifier produces.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "The liability",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LiabilityResponse.class)))
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
            description = "No such liability, or it belongs to another user",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "405",
            description = "Wrong HTTP method",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "406",
            description = "The requested media type cannot be produced",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @GetMapping("/{liabilityId}")
    LiabilityResponse read(@PathVariable UUID liabilityId) {
        return LiabilityResponse.of(liabilityService.read(CurrentUser.require(), liabilityId));
    }

    @Operation(
            summary = "Replace a liability's editable fields",
            description =
                    "Replaces every field except the category, which is chosen once when the liability is "
                            + "created. An omitted optional field is cleared, because this is a replacement "
                            + "rather than a patch: omitting collateralAssetId unlinks the collateral. The "
                            + "same rules as creation apply, and the collateral is re-checked against the "
                            + "signed-in user.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "The liability as it now stands",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LiabilityResponse.class)))
    @ApiResponse(
            responseCode = "400",
            description =
                    "A required field is missing, an amount is negative or too precise, a recurring payment is "
                            + "not positive or is half-stated, a date is inconsistent, or the collateral is "
                            + "not an asset the signed-in user owns",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "401",
            description = "No access token, or it has expired or been revoked",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "404",
            description =
                    "No such liability, it belongs to another user, or a concurrent request removed it first",
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
    @PutMapping("/{liabilityId}")
    LiabilityResponse replace(
            @PathVariable UUID liabilityId, @Valid @RequestBody UpdateLiabilityRequest request) {
        return LiabilityResponse.of(
                liabilityService.replace(
                        CurrentUser.require(),
                        liabilityId,
                        new LiabilityDetails(
                                request.name(),
                                request.outstandingBalance(),
                                request.currency(),
                                new LiabilityTerms(
                                        request.interestRate(), request.startDate(), request.maturityDate()),
                                new PaymentSchedule(
                                        request.recurringPaymentAmount(),
                                        request.recurringPaymentFrequency(),
                                        request.nextPaymentDate()),
                                request.collateralAssetId(),
                                request.notes())));
    }

    @Operation(
            summary = "Preview what deleting a liability would do",
            description =
                    "Reads, without changing anything, what DELETE /api/v1/liabilities/{liabilityId} would do "
                            + "now: the cash-flow entries that would lose their link to it (listed). "
                            + "confirmationRequired is true when there is at least one. The pledged asset is "
                            + "never affected. The same not-found as GET answers another user's liability.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "What the delete would do",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DeletionImpact.class),
                            examples =
                                    @ExampleObject(
                                            name = "Mortgage with its monthly instalment linked",
                                            value =
                                                    "{\"recordType\":\"LIABILITY\","
                                                            + "\"recordId\":\"9d1e7c42-5b6a-4f3e-8d2c-1b0a9f8e7d6c\","
                                                            + "\"confirmationRequired\":true,"
                                                            + "\"linkedEffects\":["
                                                            + "{\"recordType\":\"CASH_FLOW_ENTRY\",\"effect\":\"UNLINKED\","
                                                            + "\"field\":\"linkedLiabilityId\",\"count\":1,\"records\":["
                                                            + "{\"id\":\"5e7a9c1b-3d4f-4a6b-8c0d-2e4f6a8b0c1d\","
                                                            + "\"label\":\"Trả góp vay mua nhà Vietcombank\","
                                                            + "\"category\":\"DEBT_PRINCIPAL_PAYMENT\"}]}],"
                                                            + "\"derivedEffects\":[]}")))
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
            description = "No such liability, or it belongs to another user",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "405",
            description = "Wrong HTTP method",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "406",
            description = "The requested media type cannot be produced",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @GetMapping("/{liabilityId}/deletion-impact")
    DeletionImpact deletionImpact(@PathVariable UUID liabilityId) {
        return liabilityService.deletionImpact(CurrentUser.require(), liabilityId);
    }

    @Operation(
            summary = "Remove a liability",
            description =
                    "Deletes the liability when the signed-in user owns it. Cash-flow entries linked to it "
                            + "are kept with linkedLiabilityId cleared, and while there is at least one the "
                            + "delete is refused with 409 linked-records, carrying the same impact GET "
                            + ".../deletion-impact returns, unless the request says confirm=true. A liability "
                            + "nothing refers to deletes without it. Nothing is recoverable afterwards. A "
                            + "second delete of the same identifier answers not-found, the same answer any "
                            + "unknown identifier gets. Two deletes that overlap give one 204 and not-found "
                            + "to the rest rather than a server error. The pledged asset, if any, is untouched.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "204", description = "The liability no longer exists")
    @ApiResponse(
            responseCode = "400",
            description = "The identifier in the path is not a UUID, or confirm is not a boolean",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "401",
            description = "No access token, or it has expired or been revoked",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "404",
            description =
                    "No such liability, it belongs to another user, or a concurrent request removed it first",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "405",
            description = "Wrong HTTP method",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "409",
            description =
                    "linked-records: cash-flow entries would be unlinked and confirm=true was not sent. "
                            + "Nothing was deleted; the impact member says what the confirmed delete would do",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @DeleteMapping("/{liabilityId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(
            @PathVariable UUID liabilityId,
            @Parameter(
                            description =
                                    "true once the user has seen and accepted the linked-record effects. "
                                            + "Absent or false: a liability with linked entries is not deleted.",
                            example = "true")
                    @RequestParam(name = "confirm", defaultValue = "false")
                    boolean confirm) {
        liabilityService.delete(CurrentUser.require(), liabilityId, confirm);
    }
}
