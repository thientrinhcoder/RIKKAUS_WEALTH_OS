package com.rikkaus.wealth.assets.api;

import com.rikkaus.wealth.assets.application.AssetService;
import com.rikkaus.wealth.assets.domain.AssetMetadata;
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
 * The asset catalog: list, add, read, edit the metadata, remove.
 *
 * <p>The collection is addressed by the access token and the members by their own identifier. The only
 * path variable is the asset's id, never the owner's: an ArchUnit rule fails the compilation gate on a
 * controller method that binds a {@code userId}, {@code ownerId} or {@code accountId} from the request,
 * which is what turns "never trust a client-supplied owner" from a review habit into a guarantee.
 *
 * <p>Another user's asset identifier answers {@code not-found}, and the body carries the same {@code
 * type}, {@code title}, {@code status} and {@code detail} a genuinely missing identifier produces. The
 * two differ only in {@code instance}, which is the request URI and therefore the identifier the
 * caller already sent, and in {@code correlationId}, which is fresh on every response. Nothing in
 * either body distinguishes somebody else's record from one that was never written. A 403 would, and
 * would turn a list of identifiers into an enumeration oracle; {@code OwnershipGuard} is where that is
 * decided, for every route here.
 *
 * <p>{@code GET} on the collection takes no query parameter. Search, filter and sort belong to issue
 * #99, and pagination is listed as deliberately absent in the API conventions, so accepting a
 * parameter now would publish a shape nobody has agreed to.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/assets")
class AssetController {

    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;
    private static final String PROBLEM_SCHEMA_REF = "#/components/schemas/ProblemDetail";

    private final AssetService assetService;

    AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @Operation(
            summary = "List the signed-in user's assets",
            description =
                    "Returns every asset the viewer owns, newest valuation date first and then by name, so "
                            + "the catalog's order is the server's rather than the database's incidental one. "
                            + "An empty catalog is an empty array and a 200, not a 404. Values are reported in "
                            + "each asset's own currency; converting a USD holding to VND is the client's, "
                            + "using the rate from GET /api/v1/settings/exchange-rate.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "The viewer's catalog, possibly empty",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = AssetResponse.class)),
                            examples = {
                                @ExampleObject(
                                        name = "an empty catalog",
                                        value = "[]"),
                                @ExampleObject(
                                        name = "two assets, one in USD",
                                        value =
                                                """
                                                [
                                                  {
                                                    "id": "3f1c9a52-8d4b-4f0e-9c21-5b7a8e6d4f10",
                                                    "name": "Tài khoản USD",
                                                    "category": "BANK_ACCOUNT",
                                                    "currentValue": 20000.00,
                                                    "currency": "USD",
                                                    "valuationDate": "2026-09-12",
                                                    "liquidity": "HIGH",
                                                    "purchaseValue": null,
                                                    "purchaseDate": null,
                                                    "quantity": 20000.000000,
                                                    "institution": "Ngân hàng An Tín",
                                                    "notes": "Tiền gửi dự phòng bằng USD.",
                                                    "createdAt": "2026-09-12T02:42:11.503Z",
                                                    "updatedAt": "2026-09-12T02:42:11.503Z"
                                                  },
                                                  {
                                                    "id": "a81d4f27-6c3e-4b19-8f55-0d2e7c9a3b64",
                                                    "name": "Căn hộ Thảo Điền",
                                                    "category": "REAL_ESTATE",
                                                    "currentValue": 6100000000.00,
                                                    "currency": "VND",
                                                    "valuationDate": "2026-08-02",
                                                    "liquidity": "LOW",
                                                    "purchaseValue": 4800000000.00,
                                                    "purchaseDate": "2021-11-05",
                                                    "quantity": null,
                                                    "institution": "Sổ hồng cá nhân",
                                                    "notes": null,
                                                    "createdAt": "2026-08-02T01:15:03.221Z",
                                                    "updatedAt": "2026-08-02T01:15:03.221Z"
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
    List<AssetResponse> list() {
        return assetService.listFor(CurrentUser.require()).stream().map(AssetResponse::of).toList();
    }

    @Operation(
            summary = "Add an asset to the catalog",
            description =
                    "Creates an asset owned by the signed-in user, and the first record of its valuation "
                            + "history with the same value, currency, date and optional rate snapshot. All "
                            + "prices are manually entered; there is no market-price integration in MVP 1A. "
                            + "The valuation date must not be in the future and a purchase date must not be "
                            + "later than it, both judged as Vietnamese civil dates. A later value is recorded "
                            + "through POST /api/v1/assets/{assetId}/valuations, never by editing this one.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "201",
            description = "The asset was created",
            // Declared rather than only described in prose, so a reader of the contract alone knows the
            // header is there. One caveat the contract cannot express: CorsConfiguration exposes only
            // X-Correlation-Id, so a browser client cannot read Location off this response at all. The
            // body carries the same identifier, which is why that is a documented limitation here
            // rather than a reason to widen the CORS policy from inside this slice.
            headers =
                    @Header(
                            name = "Location",
                            description = "The new asset's URI, as /api/v1/assets/{assetId}.",
                            schema = @Schema(type = "string")),
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AssetResponse.class)))
    @ApiResponse(
            responseCode = "400",
            description =
                    "A required field is missing, a value is negative or too precise, a quantity is not "
                            + "positive, a date is impossible, or the rate snapshot is incomplete or given for "
                            + "a VND asset. An unrecognised category, currency or liquidity is a "
                            + "malformed-request instead, because the body cannot be parsed.",
            content =
                    @Content(
                            mediaType = PROBLEM_JSON,
                            schema = @Schema(ref = PROBLEM_SCHEMA_REF),
                            examples = {
                                @ExampleObject(
                                        name = "liquidity missing",
                                        value =
                                                """
                                                {
                                                  "type": "urn:rikkaus:problem:validation-failed",
                                                  "title": "Request validation failed",
                                                  "status": 400,
                                                  "detail": "Request validation failed.",
                                                  "instance": "/api/v1/assets",
                                                  "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
                                                  "errors": [
                                                    {
                                                      "field": "liquidity",
                                                      "message": "must not be null"
                                                    }
                                                  ]
                                                }
                                                """),
                                @ExampleObject(
                                        name = "purchase date after the valuation date",
                                        value =
                                                """
                                                {
                                                  "type": "urn:rikkaus:problem:validation-failed",
                                                  "title": "Request validation failed",
                                                  "status": 400,
                                                  "detail": "Request validation failed.",
                                                  "instance": "/api/v1/assets",
                                                  "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
                                                  "errors": [
                                                    {
                                                      "field": "purchaseDate",
                                                      "message": "must not be later than the valuation date"
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
    ResponseEntity<AssetResponse> create(@Valid @RequestBody CreateAssetRequest request) {
        AssetResponse created =
                AssetResponse.of(
                        assetService.create(
                                CurrentUser.require(),
                                request.category(),
                                DeclaredValuation.initial(
                                        request.currentValue(),
                                        request.currency(),
                                        request.valuationDate(),
                                        request.usdToVndRate(),
                                        request.rateEffectiveDate()),
                                new AssetMetadata(
                                        request.name(),
                                        request.liquidity(),
                                        request.purchaseValue(),
                                        request.purchaseDate(),
                                        request.quantity(),
                                        request.institution(),
                                        request.notes())));
        return ResponseEntity.created(URI.create(ApiPaths.V1 + "/assets/" + created.id())).body(created);
    }

    @Operation(
            summary = "Read one asset",
            description =
                    "Returns the asset when the signed-in user owns it. An identifier belonging to another "
                            + "user answers not-found with the same body a missing identifier produces, so the "
                            + "response cannot be used to discover which identifiers exist.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "The asset",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AssetResponse.class)))
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
            description = "No such asset, or it belongs to another user",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "405",
            description = "Wrong HTTP method",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "406",
            description = "The requested media type cannot be produced",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @GetMapping("/{assetId}")
    AssetResponse read(@PathVariable UUID assetId) {
        return AssetResponse.of(assetService.read(CurrentUser.require(), assetId));
    }

    @Operation(
            summary = "Replace an asset's editable metadata",
            description =
                    "Replaces the name, liquidity and the optional common fields. An omitted optional field "
                            + "is cleared, because this is a replacement rather than a patch. The current "
                            + "value, its currency, the valuation date and the category are not editable "
                            + "here: a new value is recorded through POST /api/v1/assets/{assetId}/valuations "
                            + "and the asset follows it, and the category is chosen once when the asset is "
                            + "created.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "The asset as it now stands",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AssetResponse.class)))
    @ApiResponse(
            responseCode = "400",
            description =
                    "The name or liquidity is missing, a value is negative or too precise, a quantity is "
                            + "not positive, or the purchase date is in the future or later than the asset's "
                            + "stored valuation date",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "401",
            description = "No access token, or it has expired or been revoked",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "404",
            description =
                    "No such asset, it belongs to another user, or a concurrent request removed it first",
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
    @PutMapping("/{assetId}")
    AssetResponse replaceMetadata(
            @PathVariable UUID assetId, @Valid @RequestBody UpdateAssetMetadataRequest request) {
        return AssetResponse.of(
                assetService.replaceMetadata(
                        CurrentUser.require(),
                        assetId,
                        new AssetMetadata(
                                request.name(),
                                request.liquidity(),
                                request.purchaseValue(),
                                request.purchaseDate(),
                                request.quantity(),
                                request.institution(),
                                request.notes())));
    }

    @Operation(
            summary = "Remove an asset",
            description =
                    "Deletes the asset when the signed-in user owns it. This is the plain removal the generic "
                            + "destructive confirmation in the accepted design promises; undo, linked-record "
                            + "consequences and a retention policy belong to issue #102. A second delete of the "
                            + "same identifier answers not-found, so the response is not idempotent and that is "
                            + "deliberate: it is the same answer any unknown identifier gets. Two deletes that "
                            + "overlap, which is what a double-tapped confirm button produces, give one 204 and "
                            + "not-found to the rest rather than a server error.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "204", description = "The asset no longer exists")
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
                    "No such asset, it belongs to another user, or a concurrent request removed it first",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "405",
            description = "Wrong HTTP method",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @DeleteMapping("/{assetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID assetId) {
        assetService.delete(CurrentUser.require(), assetId);
    }
}
