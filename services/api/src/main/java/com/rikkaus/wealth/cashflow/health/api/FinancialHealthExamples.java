package com.rikkaus.wealth.cashflow.health.api;

/**
 * OpenAPI examples for the financial-health route.
 *
 * <p>Each is a body the running application returned on 27/11/2026 for a user of the manual verification
 * run in {@code plans/261127-1241-GH-83-financial-health}, unchanged apart from line breaks, so the
 * published contract shows real output rather than a hand-written approximation of it.
 */
final class FinancialHealthExamples {

    /**
     * The design #81 baseline moved to November 2026: income 180.000.000 ₫, expense 92.000.000 ₫, a liquid
     * group of 1.850.000.000 ₫ over 61.600.000 ₫ of recurring essential expense. One rule fires, income
     * concentration at 73,3 %.
     */
    static final String BASELINE =
            """
            {
              "assumptions": {"asOfDate":"2026-11-27","timeZone":"Asia/Ho_Chi_Minh","month":"2026-11","monthFrom":"2026-11-01","monthTo":"2026-11-30","forecastHorizon":{"from":"2026-12-01","to":"2027-02-28"},"obligationWindow":{"from":"2026-11-27","to":"2026-12-27"},"liquidAssetCategories":["CASH","BANK_ACCOUNT","TERM_DEPOSIT"],"cashAccountCategories":["CASH","BANK_ACCOUNT"],"essentialCategories":["LIVING_COSTS","HOUSING","HEALTHCARE","INSURANCE","TAX_AND_FEES","DEBT_PRINCIPAL_PAYMENT","INTEREST_PAYMENT"],"debtPaymentCategories":["DEBT_PRINCIPAL_PAYMENT","INTEREST_PAYMENT"],"exchangeRate":{"usdToVndRate":25100.000000,"effectiveDate":"2026-09-12"}},
              "thresholds": {"reserveOkMinMonths":6.0,"minReserveMonths":3.0,"debtBurdenOkMax":0.250000,"maxDebtPaymentBurden":0.350000,"passiveIncomeHighMin":0.500000,"passiveIncomeMediumMin":0.200000,"essentialShareOkMax":0.700000,"essentialShareWatchMax":0.800000,"dueWindowDays":30,"maxIncomeSourceShare":0.600000},
              "indicators": [
                {
                  "indicatorId": "EMERGENCY_RESERVE_MONTHS",
                  "availability": "COMPLETE",
                  "value": 30.0,
                  "unit": "MONTHS",
                  "band": "OK",
                  "bandNotDeterminedReason": null,
                  "unavailableReason": null,
                  "partialReason": null,
                  "calculation": {"formula":"LIQUID_ASSETS_OVER_MONTHLY_RECURRING_ESSENTIAL_EXPENSE","numeratorVnd":1850000000.00,"denominatorVnd":61600000.00,"referenceVnd":null,"observedOn":null,"sources":[
                    {"sourceType":"ASSET","sourceId":"cd3957a8-4df9-424f-815a-93d781e85d54","category":"BANK_ACCOUNT","role":"NUMERATOR","date":"2026-11-24","amount":20000.00,"currency":"USD","amountVnd":502000000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"ASSET","sourceId":"8debc358-2f2f-425f-a48f-bf92eca0e1bd","category":"CASH","role":"NUMERATOR","date":"2026-11-25","amount":148000000.00,"currency":"VND","amountVnd":148000000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"ASSET","sourceId":"1c9fa9d3-efc3-47fc-a52d-f88aaae8dd90","category":"TERM_DEPOSIT","role":"NUMERATOR","date":"2026-11-21","amount":1200000000.00,"currency":"VND","amountVnd":1200000000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"66567388-95b4-404a-98e7-e2d64b55cdd2","category":"HOUSING","role":"DENOMINATOR","date":"2026-01-20","amount":18750000.00,"currency":"VND","amountVnd":18750000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":18750000.00},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"8e6f74c9-33c4-47d4-862c-a2f4fb4ecf0e","category":"INSURANCE","role":"DENOMINATOR","date":"2025-11-16","amount":5400000.00,"currency":"VND","amountVnd":5400000.00,"cadence":"YEARLY","monthlyEquivalentVnd":450000.00},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"9a45d13b-bc47-499c-aad5-b673f7f4c3dc","category":"INTEREST_PAYMENT","role":"DENOMINATOR","date":"2026-01-20","amount":6250000.00,"currency":"VND","amountVnd":6250000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":6250000.00},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"9d2153bc-16e5-4aa0-9b8a-92d36aaf4b2c","category":"DEBT_PRINCIPAL_PAYMENT","role":"DENOMINATOR","date":"2026-01-20","amount":10000000.00,"currency":"VND","amountVnd":10000000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":10000000.00},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"c23ede47-04d1-46a2-a648-ebffb41d7b77","category":"LIVING_COSTS","role":"DENOMINATOR","date":"2026-01-03","amount":23400000.00,"currency":"VND","amountVnd":23400000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":23400000.00},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"f28b8730-a455-46a7-8a29-0a85c59e74aa","category":"TAX_AND_FEES","role":"DENOMINATOR","date":"2026-11-22","amount":8250000.00,"currency":"VND","amountVnd":8250000.00,"cadence":"QUARTERLY","monthlyEquivalentVnd":2750000.00}
                  ]}
                },
                {
                  "indicatorId": "DEBT_PAYMENT_BURDEN",
                  "availability": "COMPLETE",
                  "value": 0.090278,
                  "unit": "FRACTION",
                  "band": "OK",
                  "bandNotDeterminedReason": null,
                  "unavailableReason": null,
                  "partialReason": null,
                  "calculation": {"formula":"DEBT_PAYMENTS_OVER_INCOME","numeratorVnd":16250000.00,"denominatorVnd":180000000.00,"referenceVnd":null,"observedOn":null,"sources":[
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"9a45d13b-bc47-499c-aad5-b673f7f4c3dc","category":"INTEREST_PAYMENT","role":"NUMERATOR","date":"2026-11-20","amount":6250000.00,"currency":"VND","amountVnd":6250000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"9d2153bc-16e5-4aa0-9b8a-92d36aaf4b2c","category":"DEBT_PRINCIPAL_PAYMENT","role":"NUMERATOR","date":"2026-11-20","amount":10000000.00,"currency":"VND","amountVnd":10000000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"75d689de-b37c-4638-89ce-6117341d366b","category":"SALARY","role":"DENOMINATOR","date":"2026-11-05","amount":132000000.00,"currency":"VND","amountVnd":132000000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"0b9c97d9-2d94-4ff2-ab59-71e305e28b20","category":"RENTAL_INCOME","role":"DENOMINATOR","date":"2026-11-10","amount":14800000.00,"currency":"VND","amountVnd":14800000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"ede5fdb9-646c-4200-9c7f-32ef9b952198","category":"BUSINESS_INCOME","role":"DENOMINATOR","date":"2026-11-12","amount":26200000.00,"currency":"VND","amountVnd":26200000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"6374e3a7-1caa-421b-a51c-9849540b0aa6","category":"DIVIDEND","role":"DENOMINATOR","date":"2026-11-15","amount":150.00,"currency":"USD","amountVnd":3765000.00,"cadence":"QUARTERLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"cc5a0a53-59f4-4ac2-bd3f-ce62da9f038a","category":"OTHER_INCOME","role":"DENOMINATOR","date":"2026-11-18","amount":235000.00,"currency":"VND","amountVnd":235000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"36ebaf64-5e12-44ff-859f-40a06af95974","category":"BOND_OR_FUND_INCOME","role":"DENOMINATOR","date":"2026-11-20","amount":1500000.00,"currency":"VND","amountVnd":1500000.00,"cadence":"QUARTERLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"be494051-c2f7-4803-a9d1-932a14940145","category":"DEPOSIT_INTEREST","role":"DENOMINATOR","date":"2026-11-25","amount":1500000.00,"currency":"VND","amountVnd":1500000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null}
                  ]}
                },
                {
                  "indicatorId": "FORECAST_MINIMUM_CASH_BALANCE",
                  "availability": "COMPLETE",
                  "value": 626600000.00,
                  "unit": "VND",
                  "band": "OK",
                  "bandNotDeterminedReason": null,
                  "unavailableReason": null,
                  "partialReason": null,
                  "calculation": {"formula":"LOWEST_PROJECTED_CASH_BALANCE","numeratorVnd":null,"denominatorVnd":null,"referenceVnd":61600000.00,"observedOn":"2026-12-03","sources":[
                    {"sourceType":"ASSET","sourceId":"8debc358-2f2f-425f-a48f-bf92eca0e1bd","category":"CASH","role":"OPENING_BALANCE","date":"2026-11-25","amount":148000000.00,"currency":"VND","amountVnd":148000000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"ASSET","sourceId":"cd3957a8-4df9-424f-815a-93d781e85d54","category":"BANK_ACCOUNT","role":"OPENING_BALANCE","date":"2026-11-24","amount":20000.00,"currency":"USD","amountVnd":502000000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"c23ede47-04d1-46a2-a648-ebffb41d7b77","category":"LIVING_COSTS","role":"PROJECTED_EXPENSE","date":"2026-12-03","amount":23400000.00,"currency":"VND","amountVnd":23400000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null}
                  ]}
                },
                {
                  "indicatorId": "PASSIVE_INCOME_SHARE",
                  "availability": "COMPLETE",
                  "value": 0.119806,
                  "unit": "FRACTION",
                  "band": "LOW",
                  "bandNotDeterminedReason": null,
                  "unavailableReason": null,
                  "partialReason": null,
                  "calculation": {"formula":"ASSET_LINKED_INCOME_OVER_INCOME","numeratorVnd":21565000.00,"denominatorVnd":180000000.00,"referenceVnd":null,"observedOn":null,"sources":[
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"75d689de-b37c-4638-89ce-6117341d366b","category":"SALARY","role":"SHARE_REST","date":"2026-11-05","amount":132000000.00,"currency":"VND","amountVnd":132000000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"0b9c97d9-2d94-4ff2-ab59-71e305e28b20","category":"RENTAL_INCOME","role":"SHARE_PART","date":"2026-11-10","amount":14800000.00,"currency":"VND","amountVnd":14800000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"ede5fdb9-646c-4200-9c7f-32ef9b952198","category":"BUSINESS_INCOME","role":"SHARE_REST","date":"2026-11-12","amount":26200000.00,"currency":"VND","amountVnd":26200000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"6374e3a7-1caa-421b-a51c-9849540b0aa6","category":"DIVIDEND","role":"SHARE_PART","date":"2026-11-15","amount":150.00,"currency":"USD","amountVnd":3765000.00,"cadence":"QUARTERLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"cc5a0a53-59f4-4ac2-bd3f-ce62da9f038a","category":"OTHER_INCOME","role":"SHARE_REST","date":"2026-11-18","amount":235000.00,"currency":"VND","amountVnd":235000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"36ebaf64-5e12-44ff-859f-40a06af95974","category":"BOND_OR_FUND_INCOME","role":"SHARE_PART","date":"2026-11-20","amount":1500000.00,"currency":"VND","amountVnd":1500000.00,"cadence":"QUARTERLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"be494051-c2f7-4803-a9d1-932a14940145","category":"DEPOSIT_INTEREST","role":"SHARE_PART","date":"2026-11-25","amount":1500000.00,"currency":"VND","amountVnd":1500000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null}
                  ]}
                },
                {
                  "indicatorId": "ESSENTIAL_EXPENSE_SHARE",
                  "availability": "COMPLETE",
                  "value": 0.833696,
                  "unit": "FRACTION",
                  "band": "ATTENTION",
                  "bandNotDeterminedReason": null,
                  "unavailableReason": null,
                  "partialReason": null,
                  "calculation": {"formula":"ESSENTIAL_EXPENSE_OVER_EXPENSE","numeratorVnd":76700000.00,"denominatorVnd":92000000.00,"referenceVnd":null,"observedOn":null,"sources":[
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"c23ede47-04d1-46a2-a648-ebffb41d7b77","category":"LIVING_COSTS","role":"SHARE_PART","date":"2026-11-03","amount":23400000.00,"currency":"VND","amountVnd":23400000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"e96d4d74-aa20-445a-8159-c9a79b7ff349","category":"EDUCATION","role":"SHARE_REST","date":"2026-11-08","amount":12800000.00,"currency":"VND","amountVnd":12800000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"0e6907b4-0ac1-4000-873f-0e0eb4de448f","category":"HEALTHCARE","role":"SHARE_PART","date":"2026-11-14","amount":4650000.00,"currency":"VND","amountVnd":4650000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"8e6f74c9-33c4-47d4-862c-a2f4fb4ecf0e","category":"INSURANCE","role":"SHARE_PART","date":"2026-11-16","amount":5400000.00,"currency":"VND","amountVnd":5400000.00,"cadence":"YEARLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"66567388-95b4-404a-98e7-e2d64b55cdd2","category":"HOUSING","role":"SHARE_PART","date":"2026-11-20","amount":18750000.00,"currency":"VND","amountVnd":18750000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"9a45d13b-bc47-499c-aad5-b673f7f4c3dc","category":"INTEREST_PAYMENT","role":"SHARE_PART","date":"2026-11-20","amount":6250000.00,"currency":"VND","amountVnd":6250000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"9d2153bc-16e5-4aa0-9b8a-92d36aaf4b2c","category":"DEBT_PRINCIPAL_PAYMENT","role":"SHARE_PART","date":"2026-11-20","amount":10000000.00,"currency":"VND","amountVnd":10000000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"f28b8730-a455-46a7-8a29-0a85c59e74aa","category":"TAX_AND_FEES","role":"SHARE_PART","date":"2026-11-22","amount":8250000.00,"currency":"VND","amountVnd":8250000.00,"cadence":"QUARTERLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"76a0afa6-62e5-4887-85d3-d591e2abc42f","category":"OTHER_EXPENSE","role":"SHARE_REST","date":"2026-11-24","amount":2500000.00,"currency":"VND","amountVnd":2500000.00,"cadence":null,"monthlyEquivalentVnd":null}
                  ]}
                }
              ],
              "rules": [
                {
                  "ruleId": "SPENDING_EXCEEDS_INCOME",
                  "status": "OK",
                  "dependsOn": null,
                  "observed": {"value":88000000.00,"unit":"VND","subject":null,"date":null},
                  "threshold": {"comparison":"LESS_THAN","value":0.00,"unit":"VND","property":null},
                  "evidence": [
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"c23ede47-04d1-46a2-a648-ebffb41d7b77","category":"LIVING_COSTS","role":"EXPENSE","date":"2026-11-03","amount":23400000.00,"currency":"VND","amountVnd":23400000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"75d689de-b37c-4638-89ce-6117341d366b","category":"SALARY","role":"INCOME","date":"2026-11-05","amount":132000000.00,"currency":"VND","amountVnd":132000000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"e96d4d74-aa20-445a-8159-c9a79b7ff349","category":"EDUCATION","role":"EXPENSE","date":"2026-11-08","amount":12800000.00,"currency":"VND","amountVnd":12800000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"0b9c97d9-2d94-4ff2-ab59-71e305e28b20","category":"RENTAL_INCOME","role":"INCOME","date":"2026-11-10","amount":14800000.00,"currency":"VND","amountVnd":14800000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"ede5fdb9-646c-4200-9c7f-32ef9b952198","category":"BUSINESS_INCOME","role":"INCOME","date":"2026-11-12","amount":26200000.00,"currency":"VND","amountVnd":26200000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"0e6907b4-0ac1-4000-873f-0e0eb4de448f","category":"HEALTHCARE","role":"EXPENSE","date":"2026-11-14","amount":4650000.00,"currency":"VND","amountVnd":4650000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"6374e3a7-1caa-421b-a51c-9849540b0aa6","category":"DIVIDEND","role":"INCOME","date":"2026-11-15","amount":150.00,"currency":"USD","amountVnd":3765000.00,"cadence":"QUARTERLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"8e6f74c9-33c4-47d4-862c-a2f4fb4ecf0e","category":"INSURANCE","role":"EXPENSE","date":"2026-11-16","amount":5400000.00,"currency":"VND","amountVnd":5400000.00,"cadence":"YEARLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"cc5a0a53-59f4-4ac2-bd3f-ce62da9f038a","category":"OTHER_INCOME","role":"INCOME","date":"2026-11-18","amount":235000.00,"currency":"VND","amountVnd":235000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"36ebaf64-5e12-44ff-859f-40a06af95974","category":"BOND_OR_FUND_INCOME","role":"INCOME","date":"2026-11-20","amount":1500000.00,"currency":"VND","amountVnd":1500000.00,"cadence":"QUARTERLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"66567388-95b4-404a-98e7-e2d64b55cdd2","category":"HOUSING","role":"EXPENSE","date":"2026-11-20","amount":18750000.00,"currency":"VND","amountVnd":18750000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"9a45d13b-bc47-499c-aad5-b673f7f4c3dc","category":"INTEREST_PAYMENT","role":"EXPENSE","date":"2026-11-20","amount":6250000.00,"currency":"VND","amountVnd":6250000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"9d2153bc-16e5-4aa0-9b8a-92d36aaf4b2c","category":"DEBT_PRINCIPAL_PAYMENT","role":"EXPENSE","date":"2026-11-20","amount":10000000.00,"currency":"VND","amountVnd":10000000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"f28b8730-a455-46a7-8a29-0a85c59e74aa","category":"TAX_AND_FEES","role":"EXPENSE","date":"2026-11-22","amount":8250000.00,"currency":"VND","amountVnd":8250000.00,"cadence":"QUARTERLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"76a0afa6-62e5-4887-85d3-d591e2abc42f","category":"OTHER_EXPENSE","role":"EXPENSE","date":"2026-11-24","amount":2500000.00,"currency":"VND","amountVnd":2500000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"be494051-c2f7-4803-a9d1-932a14940145","category":"DEPOSIT_INTEREST","role":"INCOME","date":"2026-11-25","amount":1500000.00,"currency":"VND","amountVnd":1500000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null}
                  ],
                  "notEvaluatedReason": null
                },
                {
                  "ruleId": "FORECAST_CASH_NEGATIVE",
                  "status": "OK",
                  "dependsOn": "FORECAST_MINIMUM_CASH_BALANCE",
                  "observed": {"value":626600000.00,"unit":"VND","subject":null,"date":"2026-12-03"},
                  "threshold": {"comparison":"LESS_THAN","value":0.00,"unit":"VND","property":null},
                  "evidence": [
                    {"sourceType":"ASSET","sourceId":"8debc358-2f2f-425f-a48f-bf92eca0e1bd","category":"CASH","role":"OPENING_BALANCE","date":"2026-11-25","amount":148000000.00,"currency":"VND","amountVnd":148000000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"ASSET","sourceId":"cd3957a8-4df9-424f-815a-93d781e85d54","category":"BANK_ACCOUNT","role":"OPENING_BALANCE","date":"2026-11-24","amount":20000.00,"currency":"USD","amountVnd":502000000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"c23ede47-04d1-46a2-a648-ebffb41d7b77","category":"LIVING_COSTS","role":"PROJECTED_EXPENSE","date":"2026-12-03","amount":23400000.00,"currency":"VND","amountVnd":23400000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null}
                  ],
                  "notEvaluatedReason": null
                },
                {
                  "ruleId": "RESERVE_BELOW_REFERENCE",
                  "status": "OK",
                  "dependsOn": "EMERGENCY_RESERVE_MONTHS",
                  "observed": {"value":30.0,"unit":"MONTHS","subject":null,"date":null},
                  "threshold": {"comparison":"LESS_THAN","value":3.0,"unit":"MONTHS","property":"min-reserve-months"},
                  "evidence": [
                    {"sourceType":"ASSET","sourceId":"cd3957a8-4df9-424f-815a-93d781e85d54","category":"BANK_ACCOUNT","role":"NUMERATOR","date":"2026-11-24","amount":20000.00,"currency":"USD","amountVnd":502000000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"ASSET","sourceId":"8debc358-2f2f-425f-a48f-bf92eca0e1bd","category":"CASH","role":"NUMERATOR","date":"2026-11-25","amount":148000000.00,"currency":"VND","amountVnd":148000000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"ASSET","sourceId":"1c9fa9d3-efc3-47fc-a52d-f88aaae8dd90","category":"TERM_DEPOSIT","role":"NUMERATOR","date":"2026-11-21","amount":1200000000.00,"currency":"VND","amountVnd":1200000000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"66567388-95b4-404a-98e7-e2d64b55cdd2","category":"HOUSING","role":"DENOMINATOR","date":"2026-01-20","amount":18750000.00,"currency":"VND","amountVnd":18750000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":18750000.00},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"8e6f74c9-33c4-47d4-862c-a2f4fb4ecf0e","category":"INSURANCE","role":"DENOMINATOR","date":"2025-11-16","amount":5400000.00,"currency":"VND","amountVnd":5400000.00,"cadence":"YEARLY","monthlyEquivalentVnd":450000.00},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"9a45d13b-bc47-499c-aad5-b673f7f4c3dc","category":"INTEREST_PAYMENT","role":"DENOMINATOR","date":"2026-01-20","amount":6250000.00,"currency":"VND","amountVnd":6250000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":6250000.00},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"9d2153bc-16e5-4aa0-9b8a-92d36aaf4b2c","category":"DEBT_PRINCIPAL_PAYMENT","role":"DENOMINATOR","date":"2026-01-20","amount":10000000.00,"currency":"VND","amountVnd":10000000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":10000000.00},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"c23ede47-04d1-46a2-a648-ebffb41d7b77","category":"LIVING_COSTS","role":"DENOMINATOR","date":"2026-01-03","amount":23400000.00,"currency":"VND","amountVnd":23400000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":23400000.00},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"f28b8730-a455-46a7-8a29-0a85c59e74aa","category":"TAX_AND_FEES","role":"DENOMINATOR","date":"2026-11-22","amount":8250000.00,"currency":"VND","amountVnd":8250000.00,"cadence":"QUARTERLY","monthlyEquivalentVnd":2750000.00}
                  ],
                  "notEvaluatedReason": null
                },
                {
                  "ruleId": "HIGH_DEBT_PAYMENT_BURDEN",
                  "status": "OK",
                  "dependsOn": "DEBT_PAYMENT_BURDEN",
                  "observed": {"value":0.090278,"unit":"FRACTION","subject":null,"date":null},
                  "threshold": {"comparison":"GREATER_THAN","value":0.350000,"unit":"FRACTION","property":"max-debt-payment-burden"},
                  "evidence": [
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"9a45d13b-bc47-499c-aad5-b673f7f4c3dc","category":"INTEREST_PAYMENT","role":"NUMERATOR","date":"2026-11-20","amount":6250000.00,"currency":"VND","amountVnd":6250000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"9d2153bc-16e5-4aa0-9b8a-92d36aaf4b2c","category":"DEBT_PRINCIPAL_PAYMENT","role":"NUMERATOR","date":"2026-11-20","amount":10000000.00,"currency":"VND","amountVnd":10000000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"75d689de-b37c-4638-89ce-6117341d366b","category":"SALARY","role":"DENOMINATOR","date":"2026-11-05","amount":132000000.00,"currency":"VND","amountVnd":132000000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"0b9c97d9-2d94-4ff2-ab59-71e305e28b20","category":"RENTAL_INCOME","role":"DENOMINATOR","date":"2026-11-10","amount":14800000.00,"currency":"VND","amountVnd":14800000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"ede5fdb9-646c-4200-9c7f-32ef9b952198","category":"BUSINESS_INCOME","role":"DENOMINATOR","date":"2026-11-12","amount":26200000.00,"currency":"VND","amountVnd":26200000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"6374e3a7-1caa-421b-a51c-9849540b0aa6","category":"DIVIDEND","role":"DENOMINATOR","date":"2026-11-15","amount":150.00,"currency":"USD","amountVnd":3765000.00,"cadence":"QUARTERLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"cc5a0a53-59f4-4ac2-bd3f-ce62da9f038a","category":"OTHER_INCOME","role":"DENOMINATOR","date":"2026-11-18","amount":235000.00,"currency":"VND","amountVnd":235000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"36ebaf64-5e12-44ff-859f-40a06af95974","category":"BOND_OR_FUND_INCOME","role":"DENOMINATOR","date":"2026-11-20","amount":1500000.00,"currency":"VND","amountVnd":1500000.00,"cadence":"QUARTERLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"be494051-c2f7-4803-a9d1-932a14940145","category":"DEPOSIT_INTEREST","role":"DENOMINATOR","date":"2026-11-25","amount":1500000.00,"currency":"VND","amountVnd":1500000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null}
                  ],
                  "notEvaluatedReason": null
                },
                {
                  "ruleId": "LIQUIDITY_SHORT_FOR_UPCOMING_OBLIGATIONS",
                  "status": "OK",
                  "dependsOn": null,
                  "observed": {"value":650000000.00,"unit":"VND","subject":null,"date":null},
                  "threshold": {"comparison":"LESS_THAN","value":50750000.00,"unit":"VND","property":"due-window-days"},
                  "evidence": [
                    {"sourceType":"ASSET","sourceId":"cd3957a8-4df9-424f-815a-93d781e85d54","category":"BANK_ACCOUNT","role":"CASH_ACCOUNT","date":"2026-11-24","amount":20000.00,"currency":"USD","amountVnd":502000000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"ASSET","sourceId":"8debc358-2f2f-425f-a48f-bf92eca0e1bd","category":"CASH","role":"CASH_ACCOUNT","date":"2026-11-25","amount":148000000.00,"currency":"VND","amountVnd":148000000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"LIABILITY","sourceId":"2e75bdc4-0e13-4b82-853f-496f74177e7a","category":"CREDIT_CARD_BALANCE","role":"MATURITY_DUE","date":"2026-11-30","amount":32000000.00,"currency":"VND","amountVnd":32000000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"LIABILITY","sourceId":"96ae5201-288c-48f2-ac23-802349c89891","category":"BANK_LOAN","role":"PAYMENT_DUE","date":"2026-12-20","amount":18750000.00,"currency":"VND","amountVnd":18750000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null}
                  ],
                  "notEvaluatedReason": null
                },
                {
                  "ruleId": "INCOME_CONCENTRATION",
                  "status": "ATTENTION",
                  "dependsOn": null,
                  "observed": {"value":0.733333,"unit":"FRACTION","subject":"SALARY","date":null},
                  "threshold": {"comparison":"GREATER_THAN","value":0.600000,"unit":"FRACTION","property":"max-income-source-share"},
                  "evidence": [
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"75d689de-b37c-4638-89ce-6117341d366b","category":"SALARY","role":"INCOME","date":"2026-11-05","amount":132000000.00,"currency":"VND","amountVnd":132000000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null}
                  ],
                  "notEvaluatedReason": null
                }
              ]
            }
            """;

    /** The same records before any USD/VND rate is declared: four indicators partial, no rule judged. */
    static final String PARTIAL =
            """
            {
              "assumptions": {"asOfDate":"2026-11-27","timeZone":"Asia/Ho_Chi_Minh","month":"2026-11","monthFrom":"2026-11-01","monthTo":"2026-11-30","forecastHorizon":{"from":"2026-12-01","to":"2027-02-28"},"obligationWindow":{"from":"2026-11-27","to":"2026-12-27"},"liquidAssetCategories":["CASH","BANK_ACCOUNT","TERM_DEPOSIT"],"cashAccountCategories":["CASH","BANK_ACCOUNT"],"essentialCategories":["LIVING_COSTS","HOUSING","HEALTHCARE","INSURANCE","TAX_AND_FEES","DEBT_PRINCIPAL_PAYMENT","INTEREST_PAYMENT"],"debtPaymentCategories":["DEBT_PRINCIPAL_PAYMENT","INTEREST_PAYMENT"],"exchangeRate":null},
              "thresholds": {"reserveOkMinMonths":6.0,"minReserveMonths":3.0,"debtBurdenOkMax":0.250000,"maxDebtPaymentBurden":0.350000,"passiveIncomeHighMin":0.500000,"passiveIncomeMediumMin":0.200000,"essentialShareOkMax":0.700000,"essentialShareWatchMax":0.800000,"dueWindowDays":30,"maxIncomeSourceShare":0.600000},
              "indicators": [
                {
                  "indicatorId": "EMERGENCY_RESERVE_MONTHS",
                  "availability": "PARTIAL",
                  "value": 21.9,
                  "unit": "MONTHS",
                  "band": null,
                  "bandNotDeterminedReason": "PARTIAL_INPUTS",
                  "unavailableReason": null,
                  "partialReason": "MISSING_EXCHANGE_RATE",
                  "calculation": {"formula":"LIQUID_ASSETS_OVER_MONTHLY_RECURRING_ESSENTIAL_EXPENSE","numeratorVnd":1348000000.00,"denominatorVnd":61600000.00,"referenceVnd":null,"observedOn":null,"sources":[
                    {"sourceType":"ASSET","sourceId":"b740b482-947d-4c3b-9f69-3bcfeb60e142","category":"BANK_ACCOUNT","role":"NUMERATOR","date":"2026-11-24","amount":20000.00,"currency":"USD","amountVnd":null,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"ASSET","sourceId":"333980ae-ef8f-4545-b0dc-a3001110c92d","category":"CASH","role":"NUMERATOR","date":"2026-11-25","amount":148000000.00,"currency":"VND","amountVnd":148000000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"ASSET","sourceId":"74641b8a-9ed8-4d69-bae8-af112c315f6b","category":"TERM_DEPOSIT","role":"NUMERATOR","date":"2026-11-21","amount":1200000000.00,"currency":"VND","amountVnd":1200000000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"2ce3ce49-b26a-481f-bf23-e29713e4eca1","category":"DEBT_PRINCIPAL_PAYMENT","role":"DENOMINATOR","date":"2026-01-20","amount":10000000.00,"currency":"VND","amountVnd":10000000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":10000000.00},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"948f1453-fa5a-44f6-aa0f-a9c27b52a19f","category":"INTEREST_PAYMENT","role":"DENOMINATOR","date":"2026-01-20","amount":6250000.00,"currency":"VND","amountVnd":6250000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":6250000.00},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"a3b942ed-823e-4c7a-9eed-6acfaba4be66","category":"HOUSING","role":"DENOMINATOR","date":"2026-01-20","amount":18750000.00,"currency":"VND","amountVnd":18750000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":18750000.00},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"b30f9d43-7735-4328-a5f8-860482b533c2","category":"LIVING_COSTS","role":"DENOMINATOR","date":"2026-01-03","amount":23400000.00,"currency":"VND","amountVnd":23400000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":23400000.00},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"d10c48fd-2409-4fc5-8891-5de8fdfacacd","category":"INSURANCE","role":"DENOMINATOR","date":"2025-11-16","amount":5400000.00,"currency":"VND","amountVnd":5400000.00,"cadence":"YEARLY","monthlyEquivalentVnd":450000.00},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"e1d72ca1-1524-4a6c-b7d8-bc94ae214b00","category":"TAX_AND_FEES","role":"DENOMINATOR","date":"2026-11-22","amount":8250000.00,"currency":"VND","amountVnd":8250000.00,"cadence":"QUARTERLY","monthlyEquivalentVnd":2750000.00}
                  ]}
                },
                {
                  "indicatorId": "DEBT_PAYMENT_BURDEN",
                  "availability": "PARTIAL",
                  "value": 0.092206,
                  "unit": "FRACTION",
                  "band": null,
                  "bandNotDeterminedReason": "PARTIAL_INPUTS",
                  "unavailableReason": null,
                  "partialReason": "MISSING_EXCHANGE_RATE",
                  "calculation": {"formula":"DEBT_PAYMENTS_OVER_INCOME","numeratorVnd":16250000.00,"denominatorVnd":176235000.00,"referenceVnd":null,"observedOn":null,"sources":[
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"2ce3ce49-b26a-481f-bf23-e29713e4eca1","category":"DEBT_PRINCIPAL_PAYMENT","role":"NUMERATOR","date":"2026-11-20","amount":10000000.00,"currency":"VND","amountVnd":10000000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"948f1453-fa5a-44f6-aa0f-a9c27b52a19f","category":"INTEREST_PAYMENT","role":"NUMERATOR","date":"2026-11-20","amount":6250000.00,"currency":"VND","amountVnd":6250000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"bcb34916-f567-4e16-a905-4f27f9343066","category":"SALARY","role":"DENOMINATOR","date":"2026-11-05","amount":132000000.00,"currency":"VND","amountVnd":132000000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"b04b58b6-0a83-4f1d-826e-1adeaef71859","category":"RENTAL_INCOME","role":"DENOMINATOR","date":"2026-11-10","amount":14800000.00,"currency":"VND","amountVnd":14800000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"b6c15bf5-a5af-4119-8f47-3ea9e4807ab2","category":"BUSINESS_INCOME","role":"DENOMINATOR","date":"2026-11-12","amount":26200000.00,"currency":"VND","amountVnd":26200000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"be0a294d-8114-46b2-908a-230450f09508","category":"DIVIDEND","role":"DENOMINATOR","date":"2026-11-15","amount":150.00,"currency":"USD","amountVnd":null,"cadence":"QUARTERLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"639c4bc7-c1a2-4e89-9c92-90b9ae0e1b22","category":"OTHER_INCOME","role":"DENOMINATOR","date":"2026-11-18","amount":235000.00,"currency":"VND","amountVnd":235000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"0b07abb6-b9a6-4102-95ba-6967710c42d1","category":"BOND_OR_FUND_INCOME","role":"DENOMINATOR","date":"2026-11-20","amount":1500000.00,"currency":"VND","amountVnd":1500000.00,"cadence":"QUARTERLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"79eeed71-9864-45d3-9a7d-2fcaf44c7d31","category":"DEPOSIT_INTEREST","role":"DENOMINATOR","date":"2026-11-25","amount":1500000.00,"currency":"VND","amountVnd":1500000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null}
                  ]}
                },
                {
                  "indicatorId": "FORECAST_MINIMUM_CASH_BALANCE",
                  "availability": "PARTIAL",
                  "value": 124600000.00,
                  "unit": "VND",
                  "band": null,
                  "bandNotDeterminedReason": "PARTIAL_INPUTS",
                  "unavailableReason": null,
                  "partialReason": "MISSING_EXCHANGE_RATE",
                  "calculation": {"formula":"LOWEST_PROJECTED_CASH_BALANCE","numeratorVnd":null,"denominatorVnd":null,"referenceVnd":61600000.00,"observedOn":"2026-12-03","sources":[
                    {"sourceType":"ASSET","sourceId":"333980ae-ef8f-4545-b0dc-a3001110c92d","category":"CASH","role":"OPENING_BALANCE","date":"2026-11-25","amount":148000000.00,"currency":"VND","amountVnd":148000000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"ASSET","sourceId":"b740b482-947d-4c3b-9f69-3bcfeb60e142","category":"BANK_ACCOUNT","role":"OPENING_BALANCE","date":"2026-11-24","amount":20000.00,"currency":"USD","amountVnd":null,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"b30f9d43-7735-4328-a5f8-860482b533c2","category":"LIVING_COSTS","role":"PROJECTED_EXPENSE","date":"2026-12-03","amount":23400000.00,"currency":"VND","amountVnd":23400000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null}
                  ]}
                },
                {
                  "indicatorId": "PASSIVE_INCOME_SHARE",
                  "availability": "PARTIAL",
                  "value": 0.101002,
                  "unit": "FRACTION",
                  "band": null,
                  "bandNotDeterminedReason": "PARTIAL_INPUTS",
                  "unavailableReason": null,
                  "partialReason": "MISSING_EXCHANGE_RATE",
                  "calculation": {"formula":"ASSET_LINKED_INCOME_OVER_INCOME","numeratorVnd":17800000.00,"denominatorVnd":176235000.00,"referenceVnd":null,"observedOn":null,"sources":[
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"bcb34916-f567-4e16-a905-4f27f9343066","category":"SALARY","role":"SHARE_REST","date":"2026-11-05","amount":132000000.00,"currency":"VND","amountVnd":132000000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"b04b58b6-0a83-4f1d-826e-1adeaef71859","category":"RENTAL_INCOME","role":"SHARE_PART","date":"2026-11-10","amount":14800000.00,"currency":"VND","amountVnd":14800000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"b6c15bf5-a5af-4119-8f47-3ea9e4807ab2","category":"BUSINESS_INCOME","role":"SHARE_REST","date":"2026-11-12","amount":26200000.00,"currency":"VND","amountVnd":26200000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"be0a294d-8114-46b2-908a-230450f09508","category":"DIVIDEND","role":"SHARE_PART","date":"2026-11-15","amount":150.00,"currency":"USD","amountVnd":null,"cadence":"QUARTERLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"639c4bc7-c1a2-4e89-9c92-90b9ae0e1b22","category":"OTHER_INCOME","role":"SHARE_REST","date":"2026-11-18","amount":235000.00,"currency":"VND","amountVnd":235000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"0b07abb6-b9a6-4102-95ba-6967710c42d1","category":"BOND_OR_FUND_INCOME","role":"SHARE_PART","date":"2026-11-20","amount":1500000.00,"currency":"VND","amountVnd":1500000.00,"cadence":"QUARTERLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"79eeed71-9864-45d3-9a7d-2fcaf44c7d31","category":"DEPOSIT_INTEREST","role":"SHARE_PART","date":"2026-11-25","amount":1500000.00,"currency":"VND","amountVnd":1500000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null}
                  ]}
                },
                {
                  "indicatorId": "ESSENTIAL_EXPENSE_SHARE",
                  "availability": "COMPLETE",
                  "value": 0.833696,
                  "unit": "FRACTION",
                  "band": "ATTENTION",
                  "bandNotDeterminedReason": null,
                  "unavailableReason": null,
                  "partialReason": null,
                  "calculation": {"formula":"ESSENTIAL_EXPENSE_OVER_EXPENSE","numeratorVnd":76700000.00,"denominatorVnd":92000000.00,"referenceVnd":null,"observedOn":null,"sources":[
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"b30f9d43-7735-4328-a5f8-860482b533c2","category":"LIVING_COSTS","role":"SHARE_PART","date":"2026-11-03","amount":23400000.00,"currency":"VND","amountVnd":23400000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"0cde1661-f3d5-4acd-8a6f-021ac5e4e347","category":"EDUCATION","role":"SHARE_REST","date":"2026-11-08","amount":12800000.00,"currency":"VND","amountVnd":12800000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"16a8477b-572c-48c6-9ea4-678e29e60585","category":"HEALTHCARE","role":"SHARE_PART","date":"2026-11-14","amount":4650000.00,"currency":"VND","amountVnd":4650000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"d10c48fd-2409-4fc5-8891-5de8fdfacacd","category":"INSURANCE","role":"SHARE_PART","date":"2026-11-16","amount":5400000.00,"currency":"VND","amountVnd":5400000.00,"cadence":"YEARLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"2ce3ce49-b26a-481f-bf23-e29713e4eca1","category":"DEBT_PRINCIPAL_PAYMENT","role":"SHARE_PART","date":"2026-11-20","amount":10000000.00,"currency":"VND","amountVnd":10000000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"948f1453-fa5a-44f6-aa0f-a9c27b52a19f","category":"INTEREST_PAYMENT","role":"SHARE_PART","date":"2026-11-20","amount":6250000.00,"currency":"VND","amountVnd":6250000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"a3b942ed-823e-4c7a-9eed-6acfaba4be66","category":"HOUSING","role":"SHARE_PART","date":"2026-11-20","amount":18750000.00,"currency":"VND","amountVnd":18750000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"e1d72ca1-1524-4a6c-b7d8-bc94ae214b00","category":"TAX_AND_FEES","role":"SHARE_PART","date":"2026-11-22","amount":8250000.00,"currency":"VND","amountVnd":8250000.00,"cadence":"QUARTERLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"c08e3db8-bfe0-42c7-8937-276edebaf4ae","category":"OTHER_EXPENSE","role":"SHARE_REST","date":"2026-11-24","amount":2500000.00,"currency":"VND","amountVnd":2500000.00,"cadence":null,"monthlyEquivalentVnd":null}
                  ]}
                }
              ],
              "rules": [
                {
                  "ruleId": "SPENDING_EXCEEDS_INCOME",
                  "status": "NOT_EVALUATED",
                  "dependsOn": null,
                  "observed": null,
                  "threshold": {"comparison":"LESS_THAN","value":0.00,"unit":"VND","property":null},
                  "evidence": [
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"be0a294d-8114-46b2-908a-230450f09508","category":"DIVIDEND","role":"INCOME","date":"2026-11-15","amount":150.00,"currency":"USD","amountVnd":null,"cadence":"QUARTERLY","monthlyEquivalentVnd":null}
                  ],
                  "notEvaluatedReason": "MISSING_EXCHANGE_RATE"
                },
                {
                  "ruleId": "FORECAST_CASH_NEGATIVE",
                  "status": "NOT_EVALUATED",
                  "dependsOn": "FORECAST_MINIMUM_CASH_BALANCE",
                  "observed": null,
                  "threshold": {"comparison":"LESS_THAN","value":0.00,"unit":"VND","property":null},
                  "evidence": [
                    {"sourceType":"ASSET","sourceId":"333980ae-ef8f-4545-b0dc-a3001110c92d","category":"CASH","role":"OPENING_BALANCE","date":"2026-11-25","amount":148000000.00,"currency":"VND","amountVnd":148000000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"ASSET","sourceId":"b740b482-947d-4c3b-9f69-3bcfeb60e142","category":"BANK_ACCOUNT","role":"OPENING_BALANCE","date":"2026-11-24","amount":20000.00,"currency":"USD","amountVnd":null,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"b30f9d43-7735-4328-a5f8-860482b533c2","category":"LIVING_COSTS","role":"PROJECTED_EXPENSE","date":"2026-12-03","amount":23400000.00,"currency":"VND","amountVnd":23400000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null}
                  ],
                  "notEvaluatedReason": "INDICATOR_PARTIAL"
                },
                {
                  "ruleId": "RESERVE_BELOW_REFERENCE",
                  "status": "NOT_EVALUATED",
                  "dependsOn": "EMERGENCY_RESERVE_MONTHS",
                  "observed": null,
                  "threshold": {"comparison":"LESS_THAN","value":3.0,"unit":"MONTHS","property":"min-reserve-months"},
                  "evidence": [
                    {"sourceType":"ASSET","sourceId":"b740b482-947d-4c3b-9f69-3bcfeb60e142","category":"BANK_ACCOUNT","role":"NUMERATOR","date":"2026-11-24","amount":20000.00,"currency":"USD","amountVnd":null,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"ASSET","sourceId":"333980ae-ef8f-4545-b0dc-a3001110c92d","category":"CASH","role":"NUMERATOR","date":"2026-11-25","amount":148000000.00,"currency":"VND","amountVnd":148000000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"ASSET","sourceId":"74641b8a-9ed8-4d69-bae8-af112c315f6b","category":"TERM_DEPOSIT","role":"NUMERATOR","date":"2026-11-21","amount":1200000000.00,"currency":"VND","amountVnd":1200000000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"2ce3ce49-b26a-481f-bf23-e29713e4eca1","category":"DEBT_PRINCIPAL_PAYMENT","role":"DENOMINATOR","date":"2026-01-20","amount":10000000.00,"currency":"VND","amountVnd":10000000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":10000000.00},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"948f1453-fa5a-44f6-aa0f-a9c27b52a19f","category":"INTEREST_PAYMENT","role":"DENOMINATOR","date":"2026-01-20","amount":6250000.00,"currency":"VND","amountVnd":6250000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":6250000.00},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"a3b942ed-823e-4c7a-9eed-6acfaba4be66","category":"HOUSING","role":"DENOMINATOR","date":"2026-01-20","amount":18750000.00,"currency":"VND","amountVnd":18750000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":18750000.00},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"b30f9d43-7735-4328-a5f8-860482b533c2","category":"LIVING_COSTS","role":"DENOMINATOR","date":"2026-01-03","amount":23400000.00,"currency":"VND","amountVnd":23400000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":23400000.00},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"d10c48fd-2409-4fc5-8891-5de8fdfacacd","category":"INSURANCE","role":"DENOMINATOR","date":"2025-11-16","amount":5400000.00,"currency":"VND","amountVnd":5400000.00,"cadence":"YEARLY","monthlyEquivalentVnd":450000.00},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"e1d72ca1-1524-4a6c-b7d8-bc94ae214b00","category":"TAX_AND_FEES","role":"DENOMINATOR","date":"2026-11-22","amount":8250000.00,"currency":"VND","amountVnd":8250000.00,"cadence":"QUARTERLY","monthlyEquivalentVnd":2750000.00}
                  ],
                  "notEvaluatedReason": "INDICATOR_PARTIAL"
                },
                {
                  "ruleId": "HIGH_DEBT_PAYMENT_BURDEN",
                  "status": "NOT_EVALUATED",
                  "dependsOn": "DEBT_PAYMENT_BURDEN",
                  "observed": null,
                  "threshold": {"comparison":"GREATER_THAN","value":0.350000,"unit":"FRACTION","property":"max-debt-payment-burden"},
                  "evidence": [
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"2ce3ce49-b26a-481f-bf23-e29713e4eca1","category":"DEBT_PRINCIPAL_PAYMENT","role":"NUMERATOR","date":"2026-11-20","amount":10000000.00,"currency":"VND","amountVnd":10000000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"948f1453-fa5a-44f6-aa0f-a9c27b52a19f","category":"INTEREST_PAYMENT","role":"NUMERATOR","date":"2026-11-20","amount":6250000.00,"currency":"VND","amountVnd":6250000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"bcb34916-f567-4e16-a905-4f27f9343066","category":"SALARY","role":"DENOMINATOR","date":"2026-11-05","amount":132000000.00,"currency":"VND","amountVnd":132000000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"b04b58b6-0a83-4f1d-826e-1adeaef71859","category":"RENTAL_INCOME","role":"DENOMINATOR","date":"2026-11-10","amount":14800000.00,"currency":"VND","amountVnd":14800000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"b6c15bf5-a5af-4119-8f47-3ea9e4807ab2","category":"BUSINESS_INCOME","role":"DENOMINATOR","date":"2026-11-12","amount":26200000.00,"currency":"VND","amountVnd":26200000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"be0a294d-8114-46b2-908a-230450f09508","category":"DIVIDEND","role":"DENOMINATOR","date":"2026-11-15","amount":150.00,"currency":"USD","amountVnd":null,"cadence":"QUARTERLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"639c4bc7-c1a2-4e89-9c92-90b9ae0e1b22","category":"OTHER_INCOME","role":"DENOMINATOR","date":"2026-11-18","amount":235000.00,"currency":"VND","amountVnd":235000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"0b07abb6-b9a6-4102-95ba-6967710c42d1","category":"BOND_OR_FUND_INCOME","role":"DENOMINATOR","date":"2026-11-20","amount":1500000.00,"currency":"VND","amountVnd":1500000.00,"cadence":"QUARTERLY","monthlyEquivalentVnd":null},
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"79eeed71-9864-45d3-9a7d-2fcaf44c7d31","category":"DEPOSIT_INTEREST","role":"DENOMINATOR","date":"2026-11-25","amount":1500000.00,"currency":"VND","amountVnd":1500000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null}
                  ],
                  "notEvaluatedReason": "INDICATOR_PARTIAL"
                },
                {
                  "ruleId": "LIQUIDITY_SHORT_FOR_UPCOMING_OBLIGATIONS",
                  "status": "NOT_EVALUATED",
                  "dependsOn": null,
                  "observed": null,
                  "threshold": {"comparison":"LESS_THAN","value":50750000.00,"unit":"VND","property":"due-window-days"},
                  "evidence": [
                    {"sourceType":"ASSET","sourceId":"b740b482-947d-4c3b-9f69-3bcfeb60e142","category":"BANK_ACCOUNT","role":"CASH_ACCOUNT","date":"2026-11-24","amount":20000.00,"currency":"USD","amountVnd":null,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"ASSET","sourceId":"333980ae-ef8f-4545-b0dc-a3001110c92d","category":"CASH","role":"CASH_ACCOUNT","date":"2026-11-25","amount":148000000.00,"currency":"VND","amountVnd":148000000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"LIABILITY","sourceId":"06de4a20-b5e5-46f5-88ad-e6bef1666a6f","category":"CREDIT_CARD_BALANCE","role":"MATURITY_DUE","date":"2026-11-30","amount":32000000.00,"currency":"VND","amountVnd":32000000.00,"cadence":null,"monthlyEquivalentVnd":null},
                    {"sourceType":"LIABILITY","sourceId":"d6818358-a3e9-4bae-83e7-4b73b3dd2676","category":"BANK_LOAN","role":"PAYMENT_DUE","date":"2026-12-20","amount":18750000.00,"currency":"VND","amountVnd":18750000.00,"cadence":"MONTHLY","monthlyEquivalentVnd":null}
                  ],
                  "notEvaluatedReason": "MISSING_EXCHANGE_RATE"
                },
                {
                  "ruleId": "INCOME_CONCENTRATION",
                  "status": "NOT_EVALUATED",
                  "dependsOn": null,
                  "observed": null,
                  "threshold": {"comparison":"GREATER_THAN","value":0.600000,"unit":"FRACTION","property":"max-income-source-share"},
                  "evidence": [
                    {"sourceType":"CASH_FLOW_ENTRY","sourceId":"be0a294d-8114-46b2-908a-230450f09508","category":"DIVIDEND","role":"INCOME","date":"2026-11-15","amount":150.00,"currency":"USD","amountVnd":null,"cadence":"QUARTERLY","monthlyEquivalentVnd":null}
                  ],
                  "notEvaluatedReason": "MISSING_EXCHANGE_RATE"
                }
              ]
            }
            """;

    /** A user with no record yet: five unavailable indicators, six rules not evaluated. */
    static final String EMPTY =
            """
            {
              "assumptions": {"asOfDate":"2026-11-27","timeZone":"Asia/Ho_Chi_Minh","month":"2026-11","monthFrom":"2026-11-01","monthTo":"2026-11-30","forecastHorizon":{"from":"2026-12-01","to":"2027-02-28"},"obligationWindow":{"from":"2026-11-27","to":"2026-12-27"},"liquidAssetCategories":["CASH","BANK_ACCOUNT","TERM_DEPOSIT"],"cashAccountCategories":["CASH","BANK_ACCOUNT"],"essentialCategories":["LIVING_COSTS","HOUSING","HEALTHCARE","INSURANCE","TAX_AND_FEES","DEBT_PRINCIPAL_PAYMENT","INTEREST_PAYMENT"],"debtPaymentCategories":["DEBT_PRINCIPAL_PAYMENT","INTEREST_PAYMENT"],"exchangeRate":null},
              "thresholds": {"reserveOkMinMonths":6.0,"minReserveMonths":3.0,"debtBurdenOkMax":0.250000,"maxDebtPaymentBurden":0.350000,"passiveIncomeHighMin":0.500000,"passiveIncomeMediumMin":0.200000,"essentialShareOkMax":0.700000,"essentialShareWatchMax":0.800000,"dueWindowDays":30,"maxIncomeSourceShare":0.600000},
              "indicators": [
                {
                  "indicatorId": "EMERGENCY_RESERVE_MONTHS",
                  "availability": "UNAVAILABLE",
                  "value": null,
                  "unit": "MONTHS",
                  "band": null,
                  "bandNotDeterminedReason": "INDICATOR_UNAVAILABLE",
                  "unavailableReason": "NO_LIQUID_ASSETS",
                  "partialReason": null,
                  "calculation": {"formula":"LIQUID_ASSETS_OVER_MONTHLY_RECURRING_ESSENTIAL_EXPENSE","numeratorVnd":null,"denominatorVnd":null,"referenceVnd":null,"observedOn":null,"sources":[]}
                },
                {
                  "indicatorId": "DEBT_PAYMENT_BURDEN",
                  "availability": "UNAVAILABLE",
                  "value": null,
                  "unit": "FRACTION",
                  "band": null,
                  "bandNotDeterminedReason": "INDICATOR_UNAVAILABLE",
                  "unavailableReason": "NO_INCOME",
                  "partialReason": null,
                  "calculation": {"formula":"DEBT_PAYMENTS_OVER_INCOME","numeratorVnd":null,"denominatorVnd":null,"referenceVnd":null,"observedOn":null,"sources":[]}
                },
                {
                  "indicatorId": "FORECAST_MINIMUM_CASH_BALANCE",
                  "availability": "UNAVAILABLE",
                  "value": null,
                  "unit": "VND",
                  "band": null,
                  "bandNotDeterminedReason": "INDICATOR_UNAVAILABLE",
                  "unavailableReason": "NO_CASH_ACCOUNTS",
                  "partialReason": null,
                  "calculation": {"formula":"LOWEST_PROJECTED_CASH_BALANCE","numeratorVnd":null,"denominatorVnd":null,"referenceVnd":null,"observedOn":null,"sources":[]}
                },
                {
                  "indicatorId": "PASSIVE_INCOME_SHARE",
                  "availability": "UNAVAILABLE",
                  "value": null,
                  "unit": "FRACTION",
                  "band": null,
                  "bandNotDeterminedReason": "INDICATOR_UNAVAILABLE",
                  "unavailableReason": "NO_INCOME",
                  "partialReason": null,
                  "calculation": {"formula":"ASSET_LINKED_INCOME_OVER_INCOME","numeratorVnd":null,"denominatorVnd":null,"referenceVnd":null,"observedOn":null,"sources":[]}
                },
                {
                  "indicatorId": "ESSENTIAL_EXPENSE_SHARE",
                  "availability": "UNAVAILABLE",
                  "value": null,
                  "unit": "FRACTION",
                  "band": null,
                  "bandNotDeterminedReason": "INDICATOR_UNAVAILABLE",
                  "unavailableReason": "NO_EXPENSES",
                  "partialReason": null,
                  "calculation": {"formula":"ESSENTIAL_EXPENSE_OVER_EXPENSE","numeratorVnd":null,"denominatorVnd":null,"referenceVnd":null,"observedOn":null,"sources":[]}
                }
              ],
              "rules": [
                {
                  "ruleId": "SPENDING_EXCEEDS_INCOME",
                  "status": "NOT_EVALUATED",
                  "dependsOn": null,
                  "observed": null,
                  "threshold": {"comparison":"LESS_THAN","value":0.00,"unit":"VND","property":null},
                  "evidence": [],
                  "notEvaluatedReason": "NO_CASH_FLOW_IN_MONTH"
                },
                {
                  "ruleId": "FORECAST_CASH_NEGATIVE",
                  "status": "NOT_EVALUATED",
                  "dependsOn": "FORECAST_MINIMUM_CASH_BALANCE",
                  "observed": null,
                  "threshold": {"comparison":"LESS_THAN","value":0.00,"unit":"VND","property":null},
                  "evidence": [],
                  "notEvaluatedReason": "INDICATOR_UNAVAILABLE"
                },
                {
                  "ruleId": "RESERVE_BELOW_REFERENCE",
                  "status": "NOT_EVALUATED",
                  "dependsOn": "EMERGENCY_RESERVE_MONTHS",
                  "observed": null,
                  "threshold": {"comparison":"LESS_THAN","value":3.0,"unit":"MONTHS","property":"min-reserve-months"},
                  "evidence": [],
                  "notEvaluatedReason": "INDICATOR_UNAVAILABLE"
                },
                {
                  "ruleId": "HIGH_DEBT_PAYMENT_BURDEN",
                  "status": "NOT_EVALUATED",
                  "dependsOn": "DEBT_PAYMENT_BURDEN",
                  "observed": null,
                  "threshold": {"comparison":"GREATER_THAN","value":0.350000,"unit":"FRACTION","property":"max-debt-payment-burden"},
                  "evidence": [],
                  "notEvaluatedReason": "INDICATOR_UNAVAILABLE"
                },
                {
                  "ruleId": "LIQUIDITY_SHORT_FOR_UPCOMING_OBLIGATIONS",
                  "status": "NOT_EVALUATED",
                  "dependsOn": null,
                  "observed": null,
                  "threshold": {"comparison":"LESS_THAN","value":0.00,"unit":"VND","property":"due-window-days"},
                  "evidence": [],
                  "notEvaluatedReason": "NO_CASH_ACCOUNTS"
                },
                {
                  "ruleId": "INCOME_CONCENTRATION",
                  "status": "NOT_EVALUATED",
                  "dependsOn": null,
                  "observed": null,
                  "threshold": {"comparison":"GREATER_THAN","value":0.600000,"unit":"FRACTION","property":"max-income-source-share"},
                  "evidence": [],
                  "notEvaluatedReason": "NO_INCOME"
                }
              ]
            }
            """;

    private FinancialHealthExamples() {}
}
