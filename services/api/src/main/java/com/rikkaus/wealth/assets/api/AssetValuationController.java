package com.rikkaus.wealth.assets.api;

import com.rikkaus.wealth.assets.application.AssetValuationService;
import com.rikkaus.wealth.assets.domain.DeclaredValuation;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * An asset's valuation history: list it, record a new valuation, read one record.
 *
 * <p>Three routes and no fourth. There is no {@code PUT}, {@code PATCH} or {@code DELETE} on a record
 * because the history is append-only: a wrong value is corrected by recording the right one with a
 * pointer to the wrong one and a reason, and both stay readable. A client that tries anyway receives
 * {@code method-not-allowed}, which is the framework's answer and the honest one.
 *
 * <p>Both path variables name records — the asset, then the valuation — and neither names an owner. The
 * ArchUnit rule that fails the compilation gate on a bound {@code userId} applies here as it does to
 * every controller. Ownership is decided on the asset by {@code OwnershipGuard}, so another user's
 * asset identifier answers {@code not-found} with the same body a missing identifier produces, and a
 * valuation identifier of any other asset answers the same.
 *
 * <p>Recording a valuation is the one route in the slice that changes an asset's current value, and it
 * does so in the same transaction as the insert, so {@code GET /api/v1/assets/{assetId}} after a
 * {@code 201} here reflects the new record with no window in between.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/assets/{assetId}/valuations")
class AssetValuationController {

    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;
    private static final String PROBLEM_SCHEMA_REF = "#/components/schemas/ProblemDetail";

    private final AssetValuationService valuationService;

    AssetValuationController(AssetValuationService valuationService) {
        this.valuationService = valuationService;
    }

    @Operation(
            summary = "List an asset's valuation history",
            description =
                    "Returns every valuation ever recorded for the asset, newest first: latest effective date, "
                            + "and between two records on one date the later recorded. Exactly one record is "
                            + "marked current, and it is the one the asset's currentValue, currency and "
                            + "valuationDate are taken from. Values are in the asset's own currency with the "
                            + "rate the user applied; converting to VND is the client's. An asset always has "
                            + "at least one record, written when it was created.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "The history, current record first",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = AssetValuationResponse.class)),
                            examples =
                                    @ExampleObject(
                                            name = "a USD account with a correction in its history",
                                            value =
                                                    """
                                                    [
                                                      {
                                                        "id": "9c4e2b7a-1d35-4f68-a0c2-7e5b3d9f1a24",
                                                        "assetId": "3f1c9a52-8d4b-4f0e-9c21-5b7a8e6d4f10",
                                                        "value": 20000.00,
                                                        "currency": "USD",
                                                        "effectiveDate": "2026-09-12",
                                                        "usdToVndRate": 25100.000000,
                                                        "rateEffectiveDate": "2026-09-12",
                                                        "note": null,
                                                        "correctsValuationId": null,
                                                        "current": true,
                                                        "recordedAt": "2026-09-12T02:42:11.503Z"
                                                      },
                                                      {
                                                        "id": "e17b5c90-2f4a-4d83-b6e1-9c0d3f7a8b52",
                                                        "assetId": "3f1c9a52-8d4b-4f0e-9c21-5b7a8e6d4f10",
                                                        "value": 18500.00,
                                                        "currency": "USD",
                                                        "effectiveDate": "2026-05-31",
                                                        "usdToVndRate": 25020.000000,
                                                        "rateEffectiveDate": "2026-05-30",
                                                        "note": "Điều chỉnh số dư cuối tháng sau khi đối chiếu sao kê.",
                                                        "correctsValuationId": "6d2f8a41-3b9c-4e07-8f15-2a7c4e9b0d63",
                                                        "current": false,
                                                        "recordedAt": "2026-06-03T08:20:05.117Z"
                                                      },
                                                      {
                                                        "id": "6d2f8a41-3b9c-4e07-8f15-2a7c4e9b0d63",
                                                        "assetId": "3f1c9a52-8d4b-4f0e-9c21-5b7a8e6d4f10",
                                                        "value": 18000.00,
                                                        "currency": "USD",
                                                        "effectiveDate": "2026-05-31",
                                                        "usdToVndRate": 25020.000000,
                                                        "rateEffectiveDate": "2026-05-30",
                                                        "note": "Số dư cuối tháng được nhập ban đầu.",
                                                        "correctsValuationId": null,
                                                        "current": false,
                                                        "recordedAt": "2026-05-31T13:02:48.330Z"
                                                      }
                                                    ]
                                                    """)))
    @ApiResponse(
            responseCode = "400",
            description = "The asset identifier in the path is not a UUID",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "401",
            description = "No access token, or it has expired or been revoked",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "404",
            description = "No such asset, or it belongs to another user",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "405",
            description = "Wrong HTTP method. The history cannot be edited or deleted",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "406",
            description = "The requested media type cannot be produced",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @GetMapping
    List<AssetValuationResponse> list(@PathVariable UUID assetId) {
        return valuationService.listFor(CurrentUser.require(), assetId).stream()
                .map(AssetValuationResponse::of)
                .toList();
    }

    @Operation(
            summary = "Record a valuation",
            description =
                    "Appends a record to the asset's history. Nothing already recorded changes. When the new "
                            + "record's effective date is on or after the current record's, the asset's "
                            + "currentValue, currency and valuationDate become this record's in the same "
                            + "transaction and the response marks it current; a record dated earlier is kept "
                            + "as history and marks nothing. Two records on one date are a correction and its "
                            + "original, and the later recorded is current. To correct an earlier record, "
                            + "send its identifier in correctsValuationId with the reason in note; the "
                            + "original stays readable. The currency must be the asset's. A USD record may "
                            + "carry the USD/VND rate the user applied and its date, both or neither; a VND "
                            + "record carries neither.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "201",
            description = "The record was appended",
            headers =
                    @Header(
                            name = "Location",
                            description = "The new record's URI, as /api/v1/assets/{assetId}/valuations/{valuationId}.",
                            schema = @Schema(type = "string")),
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AssetValuationResponse.class),
                            examples = {
                                @ExampleObject(
                                        name = "a newer value, now current",
                                        value =
                                                """
                                                {
                                                  "id": "4b8e1f36-7c2d-4a95-b0e3-1d6f9a2c5e78",
                                                  "assetId": "3f1c9a52-8d4b-4f0e-9c21-5b7a8e6d4f10",
                                                  "value": 21250.00,
                                                  "currency": "USD",
                                                  "effectiveDate": "2026-09-14",
                                                  "usdToVndRate": 25100.000000,
                                                  "rateEffectiveDate": "2026-09-12",
                                                  "note": "Cập nhật số dư sau khi đối chiếu sao kê.",
                                                  "correctsValuationId": null,
                                                  "current": true,
                                                  "recordedAt": "2026-09-14T03:11:47.902Z"
                                                }
                                                """),
                                @ExampleObject(
                                        name = "a correction of an older record, kept as history",
                                        value =
                                                """
                                                {
                                                  "id": "e17b5c90-2f4a-4d83-b6e1-9c0d3f7a8b52",
                                                  "assetId": "3f1c9a52-8d4b-4f0e-9c21-5b7a8e6d4f10",
                                                  "value": 18500.00,
                                                  "currency": "USD",
                                                  "effectiveDate": "2026-05-31",
                                                  "usdToVndRate": 25020.000000,
                                                  "rateEffectiveDate": "2026-05-30",
                                                  "note": "Điều chỉnh số dư cuối tháng sau khi đối chiếu sao kê.",
                                                  "correctsValuationId": "6d2f8a41-3b9c-4e07-8f15-2a7c4e9b0d63",
                                                  "current": false,
                                                  "recordedAt": "2026-06-03T08:20:05.117Z"
                                                }
                                                """)
                            }))
    @ApiResponse(
            responseCode = "400",
            description =
                    "A required field is missing, the value is negative or too precise, the currency is not "
                            + "the asset's, a date is in the future or before the purchase date, the rate "
                            + "snapshot is incomplete or on a VND record, or a correction names a record that "
                            + "is not this asset's or carries no note. An unrecognised currency is a "
                            + "malformed-request instead, because the body cannot be parsed.",
            content =
                    @Content(
                            mediaType = PROBLEM_JSON,
                            schema = @Schema(ref = PROBLEM_SCHEMA_REF),
                            examples = {
                                @ExampleObject(
                                        name = "rate without its date",
                                        value =
                                                """
                                                {
                                                  "type": "urn:rikkaus:problem:validation-failed",
                                                  "title": "Request validation failed",
                                                  "status": 400,
                                                  "detail": "Request validation failed.",
                                                  "instance": "/api/v1/assets/3f1c9a52-8d4b-4f0e-9c21-5b7a8e6d4f10/valuations",
                                                  "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
                                                  "errors": [
                                                    {
                                                      "field": "rateEffectiveDate",
                                                      "message": "must be given together with usdToVndRate"
                                                    }
                                                  ]
                                                }
                                                """),
                                @ExampleObject(
                                        name = "correction without a reason",
                                        value =
                                                """
                                                {
                                                  "type": "urn:rikkaus:problem:validation-failed",
                                                  "title": "Request validation failed",
                                                  "status": 400,
                                                  "detail": "Request validation failed.",
                                                  "instance": "/api/v1/assets/3f1c9a52-8d4b-4f0e-9c21-5b7a8e6d4f10/valuations",
                                                  "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
                                                  "errors": [
                                                    {
                                                      "field": "note",
                                                      "message": "must be given when correcting a valuation"
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
            responseCode = "404",
            description = "No such asset, it belongs to another user, or a concurrent request removed it first",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "405",
            description = "Wrong HTTP method. The history cannot be edited or deleted",
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
    ResponseEntity<AssetValuationResponse> record(
            @PathVariable UUID assetId, @Valid @RequestBody RecordValuationRequest request) {
        AssetValuationResponse recorded =
                AssetValuationResponse.of(
                        valuationService.record(
                                CurrentUser.require(),
                                assetId,
                                new DeclaredValuation(
                                        request.value(),
                                        request.currency(),
                                        request.effectiveDate(),
                                        request.usdToVndRate(),
                                        request.rateEffectiveDate(),
                                        request.note(),
                                        request.correctsValuationId())));
        return ResponseEntity.created(
                        URI.create(
                                ApiPaths.V1 + "/assets/" + assetId + "/valuations/" + recorded.id()))
                .body(recorded);
    }

    @Operation(
            summary = "Read one valuation record",
            description =
                    "Returns the record when the signed-in user owns the asset and the record belongs to it. "
                            + "An asset of another user, or a record of any other asset, answers not-found "
                            + "with the same body a missing identifier produces.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "The record",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AssetValuationResponse.class)))
    @ApiResponse(
            responseCode = "400",
            description = "An identifier in the path is not a UUID",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "401",
            description = "No access token, or it has expired or been revoked",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "404",
            description = "No such asset or record, or the asset belongs to another user",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "405",
            description = "Wrong HTTP method. A record is never edited or deleted",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "406",
            description = "The requested media type cannot be produced",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @GetMapping("/{valuationId}")
    AssetValuationResponse read(@PathVariable UUID assetId, @PathVariable UUID valuationId) {
        return AssetValuationResponse.of(
                valuationService.read(CurrentUser.require(), assetId, valuationId));
    }
}
