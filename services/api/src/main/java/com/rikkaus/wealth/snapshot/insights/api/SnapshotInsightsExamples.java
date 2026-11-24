package com.rikkaus.wealth.snapshot.insights.api;

/**
 * OpenAPI examples for the insights route.
 *
 * <p>Each is a body the running application returned on 24/11/2026 for a user of the manual verification
 * run in {@code plans/261124-0956-GH-68-snapshot-insights}, unchanged, so the published contract shows
 * real output rather than a hand-written approximation of it.
 */
final class SnapshotInsightsExamples {

    /**
     * The design guideline fixture: 12.002.000.000 ₫ of assets, 7.500.000.000 ₫ of it one apartment valued
     * 48 days ago, 1.532.000.000 ₫ of debt, a mortgage payment in 12 days and a rate declared on 12/09/2026.
     */
    static final String FIXTURE =
            """
            {
              "asOfDate": "2026-11-24",
              "timeZone": "Asia/Ho_Chi_Minh",
              "complete": true,
              "thresholds": {"maxCategoryShare":0.500000,"maxDebtToAssetRatio":0.400000,"maxLowLiquidityShare":0.600000,"dueWindowDays":30,"maxValuationAgeDays":30,"maxExchangeRateAgeDays":5},
              "insights": [
                {
                  "ruleId": "CATEGORY_CONCENTRATION", "status": "ATTENTION",
                  "observed": {"value":0.624896,"unit":"FRACTION","subject":"REAL_ESTATE","numeratorVnd":7500000000.00,"denominatorVnd":12002000000.00,"date":null},
                  "threshold": {"comparison":"GREATER_THAN","value":0.500000,"unit":"FRACTION"},
                  "evidence": [
                    {"sourceType":"ASSET","sourceId":"d714c0bd-c311-4264-a7e6-453cb5254135","name":"Căn hộ Thảo Điền","category":"REAL_ESTATE","liquidity":"LOW","dateKind":"VALUATION_DATE","date":"2026-10-07","ageDays":48,"daysUntilDue":null,"overdue":null,"amount":7500000000.00,"currency":"VND","amountVnd":7500000000.00,"usdToVndRate":null}
                  ],
                  "unavailableReason": null
                },
                {
                  "ruleId": "DEBT_TO_ASSET_RATIO", "status": "OK",
                  "observed": {"value":0.127645,"unit":"FRACTION","subject":null,"numeratorVnd":1532000000.00,"denominatorVnd":12002000000.00,"date":null},
                  "threshold": {"comparison":"GREATER_THAN","value":0.400000,"unit":"FRACTION"},
                  "evidence": [
                    {"sourceType":"LIABILITY","sourceId":"a16f71f4-416f-44d0-94ef-19d56449426d","name":"Khoản vay mua căn hộ","category":"BANK_LOAN","liquidity":null,"dateKind":"UPDATED_ON","date":"2026-11-24","ageDays":0,"daysUntilDue":null,"overdue":null,"amount":1500000000.00,"currency":"VND","amountVnd":1500000000.00,"usdToVndRate":null},
                    {"sourceType":"LIABILITY","sourceId":"ecdd66f2-ddd6-432f-8a88-79e6e39bc943","name":"Dư nợ thẻ tín dụng","category":"CREDIT_CARD_BALANCE","liquidity":null,"dateKind":"UPDATED_ON","date":"2026-11-24","ageDays":0,"daysUntilDue":null,"overdue":null,"amount":32000000.00,"currency":"VND","amountVnd":32000000.00,"usdToVndRate":null}
                  ],
                  "unavailableReason": null
                },
                {
                  "ruleId": "LOW_LIQUIDITY_SHARE", "status": "ATTENTION",
                  "observed": {"value":0.624896,"unit":"FRACTION","subject":null,"numeratorVnd":7500000000.00,"denominatorVnd":12002000000.00,"date":null},
                  "threshold": {"comparison":"GREATER_THAN","value":0.600000,"unit":"FRACTION"},
                  "evidence": [
                    {"sourceType":"ASSET","sourceId":"d714c0bd-c311-4264-a7e6-453cb5254135","name":"Căn hộ Thảo Điền","category":"REAL_ESTATE","liquidity":"LOW","dateKind":"VALUATION_DATE","date":"2026-10-07","ageDays":48,"daysUntilDue":null,"overdue":null,"amount":7500000000.00,"currency":"VND","amountVnd":7500000000.00,"usdToVndRate":null}
                  ],
                  "unavailableReason": null
                },
                {
                  "ruleId": "UPCOMING_LIABILITY_DUE", "status": "ATTENTION",
                  "observed": {"value":12,"unit":"DAYS","subject":null,"numeratorVnd":null,"denominatorVnd":null,"date":"2026-12-06"},
                  "threshold": {"comparison":"AT_MOST","value":30,"unit":"DAYS"},
                  "evidence": [
                    {"sourceType":"LIABILITY","sourceId":"a16f71f4-416f-44d0-94ef-19d56449426d","name":"Khoản vay mua căn hộ","category":"BANK_LOAN","liquidity":null,"dateKind":"NEXT_PAYMENT_DATE","date":"2026-12-06","ageDays":null,"daysUntilDue":12,"overdue":false,"amount":18750000.00,"currency":"VND","amountVnd":18750000.00,"usdToVndRate":null}
                  ],
                  "unavailableReason": null
                },
                {
                  "ruleId": "STALE_VALUATION", "status": "ATTENTION",
                  "observed": {"value":48,"unit":"DAYS","subject":null,"numeratorVnd":null,"denominatorVnd":null,"date":"2026-10-07"},
                  "threshold": {"comparison":"GREATER_THAN","value":30,"unit":"DAYS"},
                  "evidence": [
                    {"sourceType":"ASSET","sourceId":"d714c0bd-c311-4264-a7e6-453cb5254135","name":"Căn hộ Thảo Điền","category":"REAL_ESTATE","liquidity":"LOW","dateKind":"VALUATION_DATE","date":"2026-10-07","ageDays":48,"daysUntilDue":null,"overdue":null,"amount":7500000000.00,"currency":"VND","amountVnd":7500000000.00,"usdToVndRate":null}
                  ],
                  "unavailableReason": null
                },
                {
                  "ruleId": "STALE_EXCHANGE_RATE", "status": "ATTENTION",
                  "observed": {"value":73,"unit":"DAYS","subject":null,"numeratorVnd":null,"denominatorVnd":null,"date":"2026-09-12"},
                  "threshold": {"comparison":"GREATER_THAN","value":5,"unit":"DAYS"},
                  "evidence": [
                    {"sourceType":"EXCHANGE_RATE","sourceId":null,"name":null,"category":null,"liquidity":null,"dateKind":"RATE_EFFECTIVE_DATE","date":"2026-09-12","ageDays":73,"daysUntilDue":null,"overdue":null,"amount":null,"currency":null,"amountVnd":null,"usdToVndRate":25100.000000},
                    {"sourceType":"ASSET","sourceId":"5590ab36-5bf7-4248-8ddc-b9abb41a11f7","name":"Tiền gửi USD","category":"BANK_ACCOUNT","liquidity":"HIGH","dateKind":"VALUATION_DATE","date":"2026-11-19","ageDays":5,"daysUntilDue":null,"overdue":null,"amount":20000.00,"currency":"USD","amountVnd":502000000.00,"usdToVndRate":null}
                  ],
                  "unavailableReason": null
                }
              ]
            }
            """;

    /** 148.000.000 ₫ in cash, a 20.000 USD account and a 5.000 USD loan, before any rate is declared. */
    static final String MISSING_RATE =
            """
            {
              "asOfDate": "2026-11-24",
              "timeZone": "Asia/Ho_Chi_Minh",
              "complete": false,
              "thresholds": {"maxCategoryShare":0.500000,"maxDebtToAssetRatio":0.400000,"maxLowLiquidityShare":0.600000,"dueWindowDays":30,"maxValuationAgeDays":30,"maxExchangeRateAgeDays":5},
              "insights": [
                {
                  "ruleId": "CATEGORY_CONCENTRATION", "status": "UNAVAILABLE",
                  "observed": null,
                  "threshold": {"comparison":"GREATER_THAN","value":0.500000,"unit":"FRACTION"},
                  "evidence": [
                    {"sourceType":"ASSET","sourceId":"3897dd94-10c9-467d-9998-53dbb6429778","name":"Tài khoản USD","category":"BANK_ACCOUNT","liquidity":"HIGH","dateKind":"VALUATION_DATE","date":"2026-11-21","ageDays":3,"daysUntilDue":null,"overdue":null,"amount":20000.00,"currency":"USD","amountVnd":null,"usdToVndRate":null}
                  ],
                  "unavailableReason": "MISSING_EXCHANGE_RATE"
                },
                {
                  "ruleId": "DEBT_TO_ASSET_RATIO", "status": "UNAVAILABLE",
                  "observed": null,
                  "threshold": {"comparison":"GREATER_THAN","value":0.400000,"unit":"FRACTION"},
                  "evidence": [
                    {"sourceType":"ASSET","sourceId":"3897dd94-10c9-467d-9998-53dbb6429778","name":"Tài khoản USD","category":"BANK_ACCOUNT","liquidity":"HIGH","dateKind":"VALUATION_DATE","date":"2026-11-21","ageDays":3,"daysUntilDue":null,"overdue":null,"amount":20000.00,"currency":"USD","amountVnd":null,"usdToVndRate":null},
                    {"sourceType":"LIABILITY","sourceId":"5d55a3cc-4211-4e0c-bcb0-e9b018f99e91","name":"Khoản vay USD từ người thân","category":"PERSONAL_LOAN","liquidity":null,"dateKind":"UPDATED_ON","date":"2026-11-24","ageDays":0,"daysUntilDue":null,"overdue":null,"amount":5000.00,"currency":"USD","amountVnd":null,"usdToVndRate":null}
                  ],
                  "unavailableReason": "MISSING_EXCHANGE_RATE"
                },
                {
                  "ruleId": "LOW_LIQUIDITY_SHARE", "status": "UNAVAILABLE",
                  "observed": null,
                  "threshold": {"comparison":"GREATER_THAN","value":0.600000,"unit":"FRACTION"},
                  "evidence": [
                    {"sourceType":"ASSET","sourceId":"3897dd94-10c9-467d-9998-53dbb6429778","name":"Tài khoản USD","category":"BANK_ACCOUNT","liquidity":"HIGH","dateKind":"VALUATION_DATE","date":"2026-11-21","ageDays":3,"daysUntilDue":null,"overdue":null,"amount":20000.00,"currency":"USD","amountVnd":null,"usdToVndRate":null}
                  ],
                  "unavailableReason": "MISSING_EXCHANGE_RATE"
                },
                {
                  "ruleId": "UPCOMING_LIABILITY_DUE", "status": "ATTENTION",
                  "observed": {"value":20,"unit":"DAYS","subject":null,"numeratorVnd":null,"denominatorVnd":null,"date":"2026-12-14"},
                  "threshold": {"comparison":"AT_MOST","value":30,"unit":"DAYS"},
                  "evidence": [
                    {"sourceType":"LIABILITY","sourceId":"5d55a3cc-4211-4e0c-bcb0-e9b018f99e91","name":"Khoản vay USD từ người thân","category":"PERSONAL_LOAN","liquidity":null,"dateKind":"MATURITY_DATE","date":"2026-12-14","ageDays":null,"daysUntilDue":20,"overdue":false,"amount":5000.00,"currency":"USD","amountVnd":null,"usdToVndRate":null}
                  ],
                  "unavailableReason": null
                },
                {
                  "ruleId": "STALE_VALUATION", "status": "OK",
                  "observed": {"value":3,"unit":"DAYS","subject":null,"numeratorVnd":null,"denominatorVnd":null,"date":"2026-11-21"},
                  "threshold": {"comparison":"GREATER_THAN","value":30,"unit":"DAYS"},
                  "evidence": [
                    {"sourceType":"ASSET","sourceId":"3897dd94-10c9-467d-9998-53dbb6429778","name":"Tài khoản USD","category":"BANK_ACCOUNT","liquidity":"HIGH","dateKind":"VALUATION_DATE","date":"2026-11-21","ageDays":3,"daysUntilDue":null,"overdue":null,"amount":20000.00,"currency":"USD","amountVnd":null,"usdToVndRate":null}
                  ],
                  "unavailableReason": null
                },
                {
                  "ruleId": "STALE_EXCHANGE_RATE", "status": "UNAVAILABLE",
                  "observed": null,
                  "threshold": {"comparison":"GREATER_THAN","value":5,"unit":"DAYS"},
                  "evidence": [
                    {"sourceType":"ASSET","sourceId":"3897dd94-10c9-467d-9998-53dbb6429778","name":"Tài khoản USD","category":"BANK_ACCOUNT","liquidity":"HIGH","dateKind":"VALUATION_DATE","date":"2026-11-21","ageDays":3,"daysUntilDue":null,"overdue":null,"amount":20000.00,"currency":"USD","amountVnd":null,"usdToVndRate":null},
                    {"sourceType":"LIABILITY","sourceId":"5d55a3cc-4211-4e0c-bcb0-e9b018f99e91","name":"Khoản vay USD từ người thân","category":"PERSONAL_LOAN","liquidity":null,"dateKind":"UPDATED_ON","date":"2026-11-24","ageDays":0,"daysUntilDue":null,"overdue":null,"amount":5000.00,"currency":"USD","amountVnd":null,"usdToVndRate":null}
                  ],
                  "unavailableReason": "MISSING_EXCHANGE_RATE"
                }
              ]
            }
            """;

    /** A user who has recorded nothing yet. */
    static final String EMPTY =
            """
            {
              "asOfDate": "2026-11-24",
              "timeZone": "Asia/Ho_Chi_Minh",
              "complete": true,
              "thresholds": {"maxCategoryShare":0.500000,"maxDebtToAssetRatio":0.400000,"maxLowLiquidityShare":0.600000,"dueWindowDays":30,"maxValuationAgeDays":30,"maxExchangeRateAgeDays":5},
              "insights": [
                {
                  "ruleId": "CATEGORY_CONCENTRATION", "status": "UNAVAILABLE",
                  "observed": null,
                  "threshold": {"comparison":"GREATER_THAN","value":0.500000,"unit":"FRACTION"},
                  "evidence": [],
                  "unavailableReason": "NO_ASSETS"
                },
                {
                  "ruleId": "DEBT_TO_ASSET_RATIO", "status": "UNAVAILABLE",
                  "observed": null,
                  "threshold": {"comparison":"GREATER_THAN","value":0.400000,"unit":"FRACTION"},
                  "evidence": [],
                  "unavailableReason": "NO_ASSETS"
                },
                {
                  "ruleId": "LOW_LIQUIDITY_SHARE", "status": "UNAVAILABLE",
                  "observed": null,
                  "threshold": {"comparison":"GREATER_THAN","value":0.600000,"unit":"FRACTION"},
                  "evidence": [],
                  "unavailableReason": "NO_ASSETS"
                },
                {
                  "ruleId": "UPCOMING_LIABILITY_DUE", "status": "UNAVAILABLE",
                  "observed": null,
                  "threshold": {"comparison":"AT_MOST","value":30,"unit":"DAYS"},
                  "evidence": [],
                  "unavailableReason": "NO_LIABILITIES"
                },
                {
                  "ruleId": "STALE_VALUATION", "status": "UNAVAILABLE",
                  "observed": null,
                  "threshold": {"comparison":"GREATER_THAN","value":30,"unit":"DAYS"},
                  "evidence": [],
                  "unavailableReason": "NO_ASSETS"
                },
                {
                  "ruleId": "STALE_EXCHANGE_RATE", "status": "UNAVAILABLE",
                  "observed": null,
                  "threshold": {"comparison":"GREATER_THAN","value":5,"unit":"DAYS"},
                  "evidence": [],
                  "unavailableReason": "MISSING_EXCHANGE_RATE"
                }
              ]
            }
            """;

    private SnapshotInsightsExamples() {}
}
