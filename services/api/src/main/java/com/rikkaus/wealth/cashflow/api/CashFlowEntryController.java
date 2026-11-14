package com.rikkaus.wealth.cashflow.api;

import com.rikkaus.wealth.cashflow.application.CashFlowEntryService;
import com.rikkaus.wealth.shared.api.ApiPaths;
import com.rikkaus.wealth.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The cash-flow catalog: list, add, read, replace, remove.
 *
 * <p>The collection is addressed by the access token and the members by their own identifier. The only
 * path variable is the entry's id, never the owner's: an ArchUnit rule fails the compilation gate on a
 * controller method that binds a {@code userId}, {@code ownerId} or {@code accountId} from the request.
 *
 * <p>Another user's entry identifier answers {@code not-found}, and the body carries the same {@code
 * type}, {@code title}, {@code status} and {@code detail} a genuinely missing identifier produces. The
 * two differ only in {@code instance}, which is the request URI and therefore the identifier the
 * caller already sent, and in {@code correlationId}, which is fresh on every response. {@code
 * OwnershipGuard} is where that is decided, for every route here.
 *
 * <p>{@code GET} on the collection takes no query parameter. Search and filter belong to issue #101,
 * and pagination is listed as deliberately absent in the API conventions.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/cash-flow-entries")
class CashFlowEntryController {

    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;
    private static final String PROBLEM_SCHEMA_REF = "#/components/schemas/ProblemDetail";

    private final CashFlowEntryService service;

    CashFlowEntryController(CashFlowEntryService service) {
        this.service = service;
    }

    @Operation(
            summary = "List the signed-in user's cash-flow entries",
            description =
                    "Returns every income and expense entry the viewer owns, newest effective date first and "
                            + "then newest created first. An empty catalog is an empty array and a 200, not a "
                            + "404. Amounts are reported in each entry's own currency; converting a USD entry "
                            + "to VND is the client's, using the rate from GET /api/v1/settings/exchange-rate. "
                            + "A recurring entry is one row, not one row per month: the dates it falls on are "
                            + "GET /api/v1/cash-flow-occurrences.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "The viewer's catalog, possibly empty",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = CashFlowEntryResponse.class)),
                            examples = {
                                @ExampleObject(name = "an empty catalog", value = "[]"),
                                @ExampleObject(
                                        name = "a monthly salary, a quarterly tuition fee and a one-time USD dividend",
                                        value =
                                                """
                                                [
                                                  {
                                                    "id": "5c0e2b7a-91d4-4f3b-8a6e-2d7f1c9b4e30",
                                                    "entryType": "EXPENSE",
                                                    "category": "EDUCATION",
                                                    "amount": 12500000.00,
                                                    "currency": "VND",
                                                    "timing": "RECURRING",
                                                    "recurrence": "QUARTERLY",
                                                    "effectiveDate": "2026-09-05",
                                                    "endDate": "2029-06-30",
                                                    "linkedAssetId": null,
                                                    "linkedLiabilityId": null,
                                                    "notes": "Học phí",
                                                    "createdAt": "2026-11-14T10:42:11.503Z",
                                                    "updatedAt": "2026-11-14T10:42:11.503Z"
                                                  },
                                                  {
                                                    "id": "e2a7c4d1-3b8f-4e6a-9c05-7d1b2f8a6c43",
                                                    "entryType": "INCOME",
                                                    "category": "DIVIDEND",
                                                    "amount": 150.00,
                                                    "currency": "USD",
                                                    "timing": "ONE_TIME",
                                                    "recurrence": null,
                                                    "effectiveDate": "2026-09-12",
                                                    "endDate": null,
                                                    "linkedAssetId": "3f1c9a52-8d4b-4f0e-9c21-5b7a8e6d4f10",
                                                    "linkedLiabilityId": null,
                                                    "notes": null,
                                                    "createdAt": "2026-11-14T10:40:02.117Z",
                                                    "updatedAt": "2026-11-14T10:40:02.117Z"
                                                  },
                                                  {
                                                    "id": "9b4d6f12-7e3a-4c58-b1d9-0a2e5c7f8d61",
                                                    "entryType": "INCOME",
                                                    "category": "SALARY",
                                                    "amount": 45000000.00,
                                                    "currency": "VND",
                                                    "timing": "RECURRING",
                                                    "recurrence": "MONTHLY",
                                                    "effectiveDate": "2026-01-01",
                                                    "endDate": null,
                                                    "linkedAssetId": null,
                                                    "linkedLiabilityId": null,
                                                    "notes": "Lương tháng",
                                                    "createdAt": "2026-11-14T10:38:45.902Z",
                                                    "updatedAt": "2026-11-14T10:38:45.902Z"
                                                  }
                                                ]
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
    List<CashFlowEntryResponse> list() {
        return service.listFor(CurrentUser.require()).stream().map(CashFlowEntryResponse::of).toList();
    }

    @Operation(
            summary = "Add a cash-flow entry",
            description =
                    "Creates an income or expense entry owned by the signed-in user. The category must belong "
                            + "to the entry type. A one-time entry has an effective date and nothing else; a "
                            + "recurring one has a start date, a recurrence and an optional end date. Dates may "
                            + "be in the future: an expected entry is a legitimate one. An entry may link to one "
                            + "of the viewer's assets or one of their liabilities, never both; a linked record "
                            + "that is not the viewer's, or that does not exist, is refused with the same "
                            + "message either way.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "201",
            description = "The entry was created",
            // Declared rather than only described in prose. One caveat the contract cannot express:
            // CorsConfiguration exposes only X-Correlation-Id, so a browser client cannot read Location
            // off this response at all; the body carries the same identifier.
            headers =
                    @Header(
                            name = "Location",
                            description = "The new entry's URI, as /api/v1/cash-flow-entries/{entryId}.",
                            schema = @Schema(type = "string")),
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CashFlowEntryResponse.class)))
    @ApiResponse(
            responseCode = "400",
            description =
                    "A required field is missing, the amount is not positive or too precise, the category does "
                            + "not belong to the entry type, the recurrence or end date does not fit the timing, "
                            + "the end date precedes the effective date, a linked asset or liability is not "
                            + "the viewer's, or both links are sent together. Every problem is reported at once in the errors array. An unrecognised "
                            + "constant is a malformed-request instead, because the body cannot be parsed.",
            content =
                    @Content(
                            mediaType = PROBLEM_JSON,
                            schema = @Schema(ref = PROBLEM_SCHEMA_REF),
                            examples = {
                                @ExampleObject(
                                        name = "category of the wrong entry type",
                                        value =
                                                """
                                                {
                                                  "type": "urn:rikkaus:problem:validation-failed",
                                                  "title": "Request validation failed",
                                                  "status": 400,
                                                  "detail": "Request validation failed.",
                                                  "instance": "/api/v1/cash-flow-entries",
                                                  "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
                                                  "errors": [
                                                    {
                                                      "field": "category",
                                                      "message": "must belong to the entry type"
                                                    }
                                                  ]
                                                }
                                                """),
                                @ExampleObject(
                                        name = "a one-time entry carrying a recurrence and an end date",
                                        value =
                                                """
                                                {
                                                  "type": "urn:rikkaus:problem:validation-failed",
                                                  "title": "Request validation failed",
                                                  "status": 400,
                                                  "detail": "Request validation failed.",
                                                  "instance": "/api/v1/cash-flow-entries",
                                                  "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
                                                  "errors": [
                                                    {
                                                      "field": "recurrence",
                                                      "message": "must be omitted for a one-time entry"
                                                    },
                                                    {
                                                      "field": "endDate",
                                                      "message": "must be omitted for a one-time entry"
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
    ResponseEntity<CashFlowEntryResponse> create(@Valid @RequestBody CashFlowEntryRequest request) {
        CashFlowEntryResponse created =
                CashFlowEntryResponse.of(service.create(CurrentUser.require(), request.toDetails()));
        return ResponseEntity.created(URI.create(ApiPaths.V1 + "/cash-flow-entries/" + created.id()))
                .body(created);
    }

    @Operation(
            summary = "Read one cash-flow entry",
            description =
                    "Returns the entry when the signed-in user owns it. An identifier belonging to another "
                            + "user answers not-found with the same body a missing identifier produces, so the "
                            + "response cannot be used to discover which identifiers exist.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "The entry",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CashFlowEntryResponse.class)))
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
            description = "No such entry, or it belongs to another user",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "405",
            description = "Wrong HTTP method",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "406",
            description = "The requested media type cannot be produced",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @GetMapping("/{entryId}")
    CashFlowEntryResponse read(@PathVariable UUID entryId) {
        return CashFlowEntryResponse.of(service.read(CurrentUser.require(), entryId));
    }

    @Operation(
            summary = "Replace a cash-flow entry",
            description =
                    "Replaces every field of the entry with the body, which is the same shape as the create "
                            + "body. An omitted optional member — recurrence, endDate, linkedAssetId, linkedLiabilityId, "
                            + "notes — is "
                            + "cleared, because this is a replacement rather than a patch. Nothing is frozen "
                            + "after creation: the type, category and amount may all change.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "The entry as it now stands",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CashFlowEntryResponse.class)))
    @ApiResponse(
            responseCode = "400",
            description =
                    "The same refusals as on create, or the identifier in the path is not a UUID",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "401",
            description = "No access token, or it has expired or been revoked",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "404",
            description =
                    "No such entry, it belongs to another user, or a concurrent request removed it first",
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
    @PutMapping("/{entryId}")
    CashFlowEntryResponse replace(
            @PathVariable UUID entryId, @Valid @RequestBody CashFlowEntryRequest request) {
        return CashFlowEntryResponse.of(
                service.replace(CurrentUser.require(), entryId, request.toDetails()));
    }

    @Operation(
            summary = "Remove a cash-flow entry",
            description =
                    "Deletes the entry when the signed-in user owns it. This is the plain removal the generic "
                            + "destructive confirmation in the accepted design promises; undo and a retention "
                            + "policy belong to issue #104. A second delete of the same identifier answers "
                            + "not-found, the same answer any unknown identifier gets. Two deletes that overlap, "
                            + "which is what a double-tapped confirm button produces, give one 204 and not-found "
                            + "to the rest rather than a server error.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "204", description = "The entry no longer exists")
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
            description =
                    "No such entry, it belongs to another user, or a concurrent request removed it first",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "405",
            description = "Wrong HTTP method",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @DeleteMapping("/{entryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID entryId) {
        service.delete(CurrentUser.require(), entryId);
    }
}
