package com.rikkaus.wealth.snapshot.api;

import com.rikkaus.wealth.shared.api.ApiPaths;
import com.rikkaus.wealth.shared.security.CurrentUser;
import com.rikkaus.wealth.snapshot.application.WealthSnapshotService;
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
 * The signed-in user's wealth snapshot: the dashboard's KPIs, charts and lists, and every asset and
 * liability line they are summed from.
 *
 * <p>A singular resource with no identifier, like {@code /settings/exchange-rate}: each user has exactly
 * one current picture of their wealth. The viewer comes from the access token and only their records
 * and their declared rate are read, so there is nothing for another user's data to answer.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/wealth-snapshot")
class WealthSnapshotController {

    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;
    private static final String PROBLEM_SCHEMA_REF = "#/components/schemas/ProblemDetail";

    private final WealthSnapshotService service;

    WealthSnapshotController(WealthSnapshotService service) {
        this.service = service;
    }

    @Operation(
            summary = "Summarise the signed-in user's assets, liabilities and net worth",
            description =
                    "Every amount is in VND. A USD asset or liability is converted with the rate the user has"
                            + " declared, one line at a time, rounded half-up to whole dong; the rate and its"
                            + " effective date are returned in exchangeRate. totalAssets sums every asset's latest"
                            + " value, totalLiabilities every outstanding balance, netWorth is their difference,"
                            + " debtToAssetRatio is liabilities over assets as a six-decimal fraction, liquidAssets"
                            + " sums the assets marked HIGH. Without a declared rate a USD line keeps its original"
                            + " amount and its VND figure is null; every total containing it has a null amountVnd,"
                            + " the sum of what is known in knownAmountVnd and the count left out in"
                            + " excludedRecordCount; netWorth and debtToAssetRatio are null and complete is false."
                            + " Nothing unavailable is ever reported as zero. debtToAssetRatio is also null when"
                            + " total assets are zero. assetsByCategory and assetsByLiquidity group the asset lines;"
                            + " largestAssets is the first five converted asset lines. upcomingLiabilityDues lists"
                            + " every next payment and maturity date on or before asOfDate + 30 days, past ones"
                            + " marked overdue. lastDataUpdate is the newest of sourceDates. Every total equals the"
                            + " sum of the lines returned. A user with no record gets the same structure with zeros"
                            + " and empty arrays. Nothing is stored.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "The snapshot, possibly of a user with no record yet",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = WealthSnapshotResponse.class),
                            examples = {
                                @ExampleObject(
                                        name = "a USD account and a USD loan after declaring 1 USD = 25,100 VND",
                                        value =
                                                """
                                                {
                                                  "asOfDate": "2026-11-22",
                                                  "timeZone": "Asia/Ho_Chi_Minh",
                                                  "currency": "VND",
                                                  "exchangeRate": { "usdToVndRate": 25100.000000, "effectiveDate": "2026-11-20" },
                                                  "complete": true,
                                                  "kpis": {
                                                    "totalAssets": { "amountVnd": 650000000.00, "knownAmountVnd": 650000000.00, "recordCount": 2, "excludedRecordCount": 0 },
                                                    "totalLiabilities": { "amountVnd": 125500000.00, "knownAmountVnd": 125500000.00, "recordCount": 1, "excludedRecordCount": 0 },
                                                    "netWorth": 524500000.00,
                                                    "debtToAssetRatio": 0.193077,
                                                    "liquidAssets": { "amountVnd": 650000000.00, "knownAmountVnd": 650000000.00, "recordCount": 2, "excludedRecordCount": 0 },
                                                    "lastDataUpdate": "2026-11-22"
                                                  },
                                                  "assetsByCategory": [
                                                    { "category": "BANK_ACCOUNT", "amountVnd": 502000000.00, "knownAmountVnd": 502000000.00, "recordCount": 1, "excludedRecordCount": 0 },
                                                    { "category": "CASH", "amountVnd": 148000000.00, "knownAmountVnd": 148000000.00, "recordCount": 1, "excludedRecordCount": 0 }
                                                  ],
                                                  "assetsByLiquidity": [
                                                    { "liquidity": "HIGH", "amountVnd": 650000000.00, "knownAmountVnd": 650000000.00, "recordCount": 2, "excludedRecordCount": 0 }
                                                  ],
                                                  "largestAssets": {
                                                    "items": [
                                                      { "assetId": "4b8d2e61-7a03-4f59-8c1e-d6a9b3f0e742", "name": "Tài khoản USD", "category": "BANK_ACCOUNT", "liquidity": "HIGH", "valueVnd": 502000000.00, "valuationDate": "2026-11-15" },
                                                      { "assetId": "e3c17a9d-0f52-4b68-a4d1-9b6e2c8f5a03", "name": "Tiền mặt dự phòng", "category": "CASH", "liquidity": "HIGH", "valueVnd": 148000000.00, "valuationDate": "2026-11-15" }
                                                    ],
                                                    "excludedRecordCount": 0
                                                  },
                                                  "upcomingLiabilityDues": { "windowDays": 30, "windowEnd": "2026-12-22", "items": [] },
                                                  "sourceDates": [
                                                    { "sourceType": "LIABILITY", "sourceId": "71d5b0e8-3c29-4a6f-8e17-2f9c4d6b1a85", "name": "Khoản vay USD từ người thân", "date": "2026-11-22" },
                                                    { "sourceType": "EXCHANGE_RATE", "sourceId": null, "name": null, "date": "2026-11-20" },
                                                    { "sourceType": "ASSET", "sourceId": "4b8d2e61-7a03-4f59-8c1e-d6a9b3f0e742", "name": "Tài khoản USD", "date": "2026-11-15" },
                                                    { "sourceType": "ASSET", "sourceId": "e3c17a9d-0f52-4b68-a4d1-9b6e2c8f5a03", "name": "Tiền mặt dự phòng", "date": "2026-11-15" }
                                                  ],
                                                  "assets": [
                                                    { "id": "4b8d2e61-7a03-4f59-8c1e-d6a9b3f0e742", "name": "Tài khoản USD", "category": "BANK_ACCOUNT", "liquidity": "HIGH", "value": 20000.00, "currency": "USD", "valueVnd": 502000000.00, "valuationDate": "2026-11-15" },
                                                    { "id": "e3c17a9d-0f52-4b68-a4d1-9b6e2c8f5a03", "name": "Tiền mặt dự phòng", "category": "CASH", "liquidity": "HIGH", "value": 148000000.00, "currency": "VND", "valueVnd": 148000000.00, "valuationDate": "2026-11-15" }
                                                  ],
                                                  "liabilities": [
                                                    { "id": "71d5b0e8-3c29-4a6f-8e17-2f9c4d6b1a85", "name": "Khoản vay USD từ người thân", "category": "PERSONAL_LOAN", "outstandingBalance": 5000.00, "currency": "USD", "outstandingBalanceVnd": 125500000.00, "nextPaymentDate": null, "maturityDate": null, "recurringPaymentAmount": null, "recurringPaymentFrequency": null, "updatedOn": "2026-11-22" }
                                                  ]
                                                }
                                                """),
                                @ExampleObject(
                                        name = "the same records before any rate is declared",
                                        value =
                                                """
                                                {
                                                  "asOfDate": "2026-11-22",
                                                  "timeZone": "Asia/Ho_Chi_Minh",
                                                  "currency": "VND",
                                                  "exchangeRate": null,
                                                  "complete": false,
                                                  "kpis": {
                                                    "totalAssets": { "amountVnd": null, "knownAmountVnd": 148000000.00, "recordCount": 2, "excludedRecordCount": 1 },
                                                    "totalLiabilities": { "amountVnd": null, "knownAmountVnd": 0.00, "recordCount": 1, "excludedRecordCount": 1 },
                                                    "netWorth": null,
                                                    "debtToAssetRatio": null,
                                                    "liquidAssets": { "amountVnd": null, "knownAmountVnd": 148000000.00, "recordCount": 2, "excludedRecordCount": 1 },
                                                    "lastDataUpdate": "2026-11-22"
                                                  },
                                                  "assetsByCategory": [
                                                    { "category": "CASH", "amountVnd": 148000000.00, "knownAmountVnd": 148000000.00, "recordCount": 1, "excludedRecordCount": 0 },
                                                    { "category": "BANK_ACCOUNT", "amountVnd": null, "knownAmountVnd": 0.00, "recordCount": 1, "excludedRecordCount": 1 }
                                                  ],
                                                  "assetsByLiquidity": [
                                                    { "liquidity": "HIGH", "amountVnd": null, "knownAmountVnd": 148000000.00, "recordCount": 2, "excludedRecordCount": 1 }
                                                  ],
                                                  "largestAssets": {
                                                    "items": [
                                                      { "assetId": "e3c17a9d-0f52-4b68-a4d1-9b6e2c8f5a03", "name": "Tiền mặt dự phòng", "category": "CASH", "liquidity": "HIGH", "valueVnd": 148000000.00, "valuationDate": "2026-11-15" }
                                                    ],
                                                    "excludedRecordCount": 1
                                                  },
                                                  "upcomingLiabilityDues": { "windowDays": 30, "windowEnd": "2026-12-22", "items": [] },
                                                  "sourceDates": [
                                                    { "sourceType": "LIABILITY", "sourceId": "71d5b0e8-3c29-4a6f-8e17-2f9c4d6b1a85", "name": "Khoản vay USD từ người thân", "date": "2026-11-22" },
                                                    { "sourceType": "ASSET", "sourceId": "4b8d2e61-7a03-4f59-8c1e-d6a9b3f0e742", "name": "Tài khoản USD", "date": "2026-11-15" },
                                                    { "sourceType": "ASSET", "sourceId": "e3c17a9d-0f52-4b68-a4d1-9b6e2c8f5a03", "name": "Tiền mặt dự phòng", "date": "2026-11-15" }
                                                  ],
                                                  "assets": [
                                                    { "id": "e3c17a9d-0f52-4b68-a4d1-9b6e2c8f5a03", "name": "Tiền mặt dự phòng", "category": "CASH", "liquidity": "HIGH", "value": 148000000.00, "currency": "VND", "valueVnd": 148000000.00, "valuationDate": "2026-11-15" },
                                                    { "id": "4b8d2e61-7a03-4f59-8c1e-d6a9b3f0e742", "name": "Tài khoản USD", "category": "BANK_ACCOUNT", "liquidity": "HIGH", "value": 20000.00, "currency": "USD", "valueVnd": null, "valuationDate": "2026-11-15" }
                                                  ],
                                                  "liabilities": [
                                                    { "id": "71d5b0e8-3c29-4a6f-8e17-2f9c4d6b1a85", "name": "Khoản vay USD từ người thân", "category": "PERSONAL_LOAN", "outstandingBalance": 5000.00, "currency": "USD", "outstandingBalanceVnd": null, "nextPaymentDate": null, "maturityDate": null, "recurringPaymentAmount": null, "recurringPaymentFrequency": null, "updatedOn": "2026-11-22" }
                                                  ]
                                                }
                                                """),
                                @ExampleObject(
                                        name = "a user with no record yet",
                                        value =
                                                """
                                                {
                                                  "asOfDate": "2026-11-22",
                                                  "timeZone": "Asia/Ho_Chi_Minh",
                                                  "currency": "VND",
                                                  "exchangeRate": null,
                                                  "complete": true,
                                                  "kpis": {
                                                    "totalAssets": { "amountVnd": 0.00, "knownAmountVnd": 0.00, "recordCount": 0, "excludedRecordCount": 0 },
                                                    "totalLiabilities": { "amountVnd": 0.00, "knownAmountVnd": 0.00, "recordCount": 0, "excludedRecordCount": 0 },
                                                    "netWorth": 0.00,
                                                    "debtToAssetRatio": null,
                                                    "liquidAssets": { "amountVnd": 0.00, "knownAmountVnd": 0.00, "recordCount": 0, "excludedRecordCount": 0 },
                                                    "lastDataUpdate": null
                                                  },
                                                  "assetsByCategory": [],
                                                  "assetsByLiquidity": [],
                                                  "largestAssets": { "items": [], "excludedRecordCount": 0 },
                                                  "upcomingLiabilityDues": { "windowDays": 30, "windowEnd": "2026-12-22", "items": [] },
                                                  "sourceDates": [],
                                                  "assets": [],
                                                  "liabilities": []
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
    WealthSnapshotResponse snapshot() {
        return WealthSnapshotResponse.of(service.snapshotFor(CurrentUser.require()));
    }
}
