package com.rikkaus.wealth.goals.projection.api;

/**
 * OpenAPI examples for the goal projection routes.
 *
 * <p>Each is a body the running application returned on 22/11/2026 for a goal of the manual verification
 * guide, with only the identifier replaced, so the published contract shows real output rather than a
 * hand-written approximation of it.
 */
final class GoalProjectionExamples {

    /** "Mua nhà cho con": 3.000.000.000 ₫ by 31/08/2031, 1.200.000.000 ₫ allocated, 25.000.000 ₫ a month at 6%. */
    static final String ON_TRACK =
            """
            {
              "goalId": "9d4b1f68-2c7e-4a35-b8f0-6e1a5d3c7b92",
              "goalName": "Mua nhà cho con",
              "currency": "VND",
              "asOfDate": "2026-11-22",
              "timeline": "ACTIVE",
              "targetReached": false,
              "completionPercent": 40.00,
              "remainingAmount": 1800000000.00,
              "remainingMonths": 57,
              "daysPastTargetDate": null,
              "requiredMonthlyContribution": 21371000.00,
              "contributionCoveragePercent": 116.98,
              "status": "ON_TRACK",
              "projectedAtTargetDate": {
                "fromAllocated": 1594581665.13,
                "fromContributions": 1644090271.39,
                "total": 3238671936.52,
                "gapToTarget": 238671936.52
              },
              "calculation": {
                "monthlyRatePercent": 0.500000,
                "growthFactor": 1.3288180543
              },
              "unavailableReasons": [],
              "assumptions": [
                {
                  "key": "TARGET_AMOUNT",
                  "source": "USER_ENTERED",
                  "value": "3000000000.00"
                },
                {
                  "key": "CURRENT_ALLOCATED_AMOUNT",
                  "source": "USER_ENTERED",
                  "value": "1200000000.00"
                },
                {
                  "key": "TARGET_DATE",
                  "source": "USER_ENTERED",
                  "value": "2031-08-31"
                },
                {
                  "key": "PLANNED_MONTHLY_CONTRIBUTION",
                  "source": "USER_ENTERED",
                  "value": "25000000.00"
                },
                {
                  "key": "EXPECTED_ANNUAL_RETURN_PERCENT",
                  "source": "USER_ENTERED",
                  "value": "6.00"
                },
                {
                  "key": "AS_OF_DATE",
                  "source": "SYSTEM",
                  "value": "2026-11-22"
                },
                {
                  "key": "TIME_ZONE",
                  "source": "SYSTEM",
                  "value": "Asia/Ho_Chi_Minh"
                },
                {
                  "key": "FIRST_CONTRIBUTION_MONTH",
                  "source": "SYSTEM",
                  "value": "2026-12"
                },
                {
                  "key": "COMPOUNDING",
                  "source": "SYSTEM",
                  "value": "MONTHLY"
                },
                {
                  "key": "CONTRIBUTION_TIMING",
                  "source": "SYSTEM",
                  "value": "END_OF_MONTH"
                },
                {
                  "key": "REQUIRED_CONTRIBUTION_ROUNDING",
                  "source": "SYSTEM",
                  "value": "UP_TO_1000_VND"
                },
                {
                  "key": "ON_TRACK_MIN_COVERAGE_PERCENT",
                  "source": "SYSTEM",
                  "value": "100"
                },
                {
                  "key": "AT_RISK_MIN_COVERAGE_PERCENT",
                  "source": "SYSTEM",
                  "value": "80"
                },
                {
                  "key": "INFLATION",
                  "source": "NOT_MODELLED",
                  "value": null
                },
                {
                  "key": "TAXES_AND_FEES",
                  "source": "NOT_MODELLED",
                  "value": null
                },
                {
                  "key": "RETURN_VARIABILITY",
                  "source": "NOT_MODELLED",
                  "value": null
                },
                {
                  "key": "CASH_FLOW_LINK",
                  "source": "NOT_MODELLED",
                  "value": null
                }
              ],
              "method": {
                "name": "FUTURE_VALUE_MONTHLY_COMPOUNDING",
                "remainingMonths": "n = calendar months from the month after asOfDate through the month of targetDate, both included; contributions arrive at the end of each month. A target date before asOfDate is EXPIRED; a target date later this month leaves n = 0 and is DUE_THIS_MONTH.",
                "monthlyRate": "r = expectedAnnualReturnPercent / 100 / 12, compounded monthly; g = (1 + r)^n.",
                "projectedAmount": "fromAllocated = currentAllocatedAmount × g; fromContributions = plannedMonthlyContribution × (g − 1) / r, or plannedMonthlyContribution × n when r = 0; total = fromAllocated + fromContributions; gapToTarget = total − targetAmount.",
                "requiredMonthlyContribution": "(targetAmount − currentAllocatedAmount × g) × r / (g − 1), or (targetAmount − currentAllocatedAmount) / n when r = 0; 0 when the goal is already funded or the allocated amount alone reaches the target; otherwise rounded up to the next 1,000 VND.",
                "status": "contributionCoveragePercent = plannedMonthlyContribution / requiredMonthlyContribution × 100 (100 when nothing is required). ON_TRACK at or above 100, AT_RISK at or above 80, BEHIND below; these are system reference thresholds, compared before the percentage is rounded.",
                "rounding": "Intermediate values carry 34 significant digits. Each part of the projected amount is rounded half up to 2 decimals before they are added, so the parts add up to the total. Percentages are rounded down to 2 decimals."
              },
              "disclaimer": "The expected annual return is the user's own assumption, not a guaranteed, promised or predicted return. Projected amounts illustrate what the entered figures would produce if that return were earned every month; actual results will differ."
            }
            """;

    /** "Quỹ học tiếp cho bản thân": no planned monthly contribution, so no projection and no status. */
    static final String CONTRIBUTION_MISSING =
            """
            {
              "goalId": "4a7c2e91-5d08-4f3b-9c61-b2e8d0a5f714",
              "goalName": "Quỹ học tiếp cho bản thân",
              "currency": "VND",
              "asOfDate": "2026-11-22",
              "timeline": "ACTIVE",
              "targetReached": false,
              "completionPercent": 10.00,
              "remainingAmount": 360000000.00,
              "remainingMonths": 37,
              "daysPastTargetDate": null,
              "requiredMonthlyContribution": 9025000.00,
              "contributionCoveragePercent": null,
              "status": null,
              "projectedAtTargetDate": null,
              "calculation": {
                "monthlyRatePercent": 0.333333,
                "growthFactor": 1.1310294474
              },
              "unavailableReasons": ["PLANNED_MONTHLY_CONTRIBUTION_MISSING"],
              "assumptions": [
                {
                  "key": "TARGET_AMOUNT",
                  "source": "USER_ENTERED",
                  "value": "400000000.00"
                },
                {
                  "key": "CURRENT_ALLOCATED_AMOUNT",
                  "source": "USER_ENTERED",
                  "value": "40000000.00"
                },
                {
                  "key": "TARGET_DATE",
                  "source": "USER_ENTERED",
                  "value": "2029-12-31"
                },
                {
                  "key": "PLANNED_MONTHLY_CONTRIBUTION",
                  "source": "USER_ENTERED",
                  "value": null
                },
                {
                  "key": "EXPECTED_ANNUAL_RETURN_PERCENT",
                  "source": "USER_ENTERED",
                  "value": "4.00"
                },
                {
                  "key": "AS_OF_DATE",
                  "source": "SYSTEM",
                  "value": "2026-11-22"
                },
                {
                  "key": "TIME_ZONE",
                  "source": "SYSTEM",
                  "value": "Asia/Ho_Chi_Minh"
                },
                {
                  "key": "FIRST_CONTRIBUTION_MONTH",
                  "source": "SYSTEM",
                  "value": "2026-12"
                },
                {
                  "key": "COMPOUNDING",
                  "source": "SYSTEM",
                  "value": "MONTHLY"
                },
                {
                  "key": "CONTRIBUTION_TIMING",
                  "source": "SYSTEM",
                  "value": "END_OF_MONTH"
                },
                {
                  "key": "REQUIRED_CONTRIBUTION_ROUNDING",
                  "source": "SYSTEM",
                  "value": "UP_TO_1000_VND"
                },
                {
                  "key": "ON_TRACK_MIN_COVERAGE_PERCENT",
                  "source": "SYSTEM",
                  "value": "100"
                },
                {
                  "key": "AT_RISK_MIN_COVERAGE_PERCENT",
                  "source": "SYSTEM",
                  "value": "80"
                },
                {
                  "key": "INFLATION",
                  "source": "NOT_MODELLED",
                  "value": null
                },
                {
                  "key": "TAXES_AND_FEES",
                  "source": "NOT_MODELLED",
                  "value": null
                },
                {
                  "key": "RETURN_VARIABILITY",
                  "source": "NOT_MODELLED",
                  "value": null
                },
                {
                  "key": "CASH_FLOW_LINK",
                  "source": "NOT_MODELLED",
                  "value": null
                }
              ],
              "method": {
                "name": "FUTURE_VALUE_MONTHLY_COMPOUNDING",
                "remainingMonths": "n = calendar months from the month after asOfDate through the month of targetDate, both included; contributions arrive at the end of each month. A target date before asOfDate is EXPIRED; a target date later this month leaves n = 0 and is DUE_THIS_MONTH.",
                "monthlyRate": "r = expectedAnnualReturnPercent / 100 / 12, compounded monthly; g = (1 + r)^n.",
                "projectedAmount": "fromAllocated = currentAllocatedAmount × g; fromContributions = plannedMonthlyContribution × (g − 1) / r, or plannedMonthlyContribution × n when r = 0; total = fromAllocated + fromContributions; gapToTarget = total − targetAmount.",
                "requiredMonthlyContribution": "(targetAmount − currentAllocatedAmount × g) × r / (g − 1), or (targetAmount − currentAllocatedAmount) / n when r = 0; 0 when the goal is already funded or the allocated amount alone reaches the target; otherwise rounded up to the next 1,000 VND.",
                "status": "contributionCoveragePercent = plannedMonthlyContribution / requiredMonthlyContribution × 100 (100 when nothing is required). ON_TRACK at or above 100, AT_RISK at or above 80, BEHIND below; these are system reference thresholds, compared before the percentage is rounded.",
                "rounding": "Intermediate values carry 34 significant digits. Each part of the projected amount is rounded half up to 2 decimals before they are added, so the parts add up to the total. Percentages are rounded down to 2 decimals."
              },
              "disclaimer": "The expected annual return is the user's own assumption, not a guaranteed, promised or predicted return. Projected amounts illustrate what the entered figures would produce if that return were earned every month; actual results will differ."
            }
            """;

    /** "Đổi xe gia đình": target date 31/10/2026, 22 days before the reference day. */
    static final String EXPIRED_LIST =
            """
            [
              {
                "goalId": "c3e8a1f5-7b24-4d96-8e0a-1f5c9b3d6e27",
                "goalName": "Đổi xe gia đình",
                "currency": "VND",
                "asOfDate": "2026-11-22",
                "timeline": "EXPIRED",
                "targetReached": false,
                "completionPercent": 77.50,
                "remainingAmount": 180000000.00,
                "remainingMonths": 0,
                "daysPastTargetDate": 22,
                "requiredMonthlyContribution": null,
                "contributionCoveragePercent": null,
                "status": null,
                "projectedAtTargetDate": null,
                "calculation": null,
                "unavailableReasons": ["TARGET_DATE_PASSED"],
                "assumptions": [
                  {
                    "key": "TARGET_AMOUNT",
                    "source": "USER_ENTERED",
                    "value": "800000000.00"
                  },
                  {
                    "key": "CURRENT_ALLOCATED_AMOUNT",
                    "source": "USER_ENTERED",
                    "value": "620000000.00"
                  },
                  {
                    "key": "TARGET_DATE",
                    "source": "USER_ENTERED",
                    "value": "2026-10-31"
                  },
                  {
                    "key": "PLANNED_MONTHLY_CONTRIBUTION",
                    "source": "USER_ENTERED",
                    "value": "0.00"
                  },
                  {
                    "key": "EXPECTED_ANNUAL_RETURN_PERCENT",
                    "source": "USER_ENTERED",
                    "value": "4.00"
                  },
                  {
                    "key": "AS_OF_DATE",
                    "source": "SYSTEM",
                    "value": "2026-11-22"
                  },
                  {
                    "key": "TIME_ZONE",
                    "source": "SYSTEM",
                    "value": "Asia/Ho_Chi_Minh"
                  },
                  {
                    "key": "FIRST_CONTRIBUTION_MONTH",
                    "source": "SYSTEM",
                    "value": null
                  },
                  {
                    "key": "COMPOUNDING",
                    "source": "SYSTEM",
                    "value": "MONTHLY"
                  },
                  {
                    "key": "CONTRIBUTION_TIMING",
                    "source": "SYSTEM",
                    "value": "END_OF_MONTH"
                  },
                  {
                    "key": "REQUIRED_CONTRIBUTION_ROUNDING",
                    "source": "SYSTEM",
                    "value": "UP_TO_1000_VND"
                  },
                  {
                    "key": "ON_TRACK_MIN_COVERAGE_PERCENT",
                    "source": "SYSTEM",
                    "value": "100"
                  },
                  {
                    "key": "AT_RISK_MIN_COVERAGE_PERCENT",
                    "source": "SYSTEM",
                    "value": "80"
                  },
                  {
                    "key": "INFLATION",
                    "source": "NOT_MODELLED",
                    "value": null
                  },
                  {
                    "key": "TAXES_AND_FEES",
                    "source": "NOT_MODELLED",
                    "value": null
                  },
                  {
                    "key": "RETURN_VARIABILITY",
                    "source": "NOT_MODELLED",
                    "value": null
                  },
                  {
                    "key": "CASH_FLOW_LINK",
                    "source": "NOT_MODELLED",
                    "value": null
                  }
                ],
                "method": {
                  "name": "FUTURE_VALUE_MONTHLY_COMPOUNDING",
                  "remainingMonths": "n = calendar months from the month after asOfDate through the month of targetDate, both included; contributions arrive at the end of each month. A target date before asOfDate is EXPIRED; a target date later this month leaves n = 0 and is DUE_THIS_MONTH.",
                  "monthlyRate": "r = expectedAnnualReturnPercent / 100 / 12, compounded monthly; g = (1 + r)^n.",
                  "projectedAmount": "fromAllocated = currentAllocatedAmount × g; fromContributions = plannedMonthlyContribution × (g − 1) / r, or plannedMonthlyContribution × n when r = 0; total = fromAllocated + fromContributions; gapToTarget = total − targetAmount.",
                  "requiredMonthlyContribution": "(targetAmount − currentAllocatedAmount × g) × r / (g − 1), or (targetAmount − currentAllocatedAmount) / n when r = 0; 0 when the goal is already funded or the allocated amount alone reaches the target; otherwise rounded up to the next 1,000 VND.",
                  "status": "contributionCoveragePercent = plannedMonthlyContribution / requiredMonthlyContribution × 100 (100 when nothing is required). ON_TRACK at or above 100, AT_RISK at or above 80, BEHIND below; these are system reference thresholds, compared before the percentage is rounded.",
                  "rounding": "Intermediate values carry 34 significant digits. Each part of the projected amount is rounded half up to 2 decimals before they are added, so the parts add up to the total. Percentages are rounded down to 2 decimals."
                },
                "disclaimer": "The expected annual return is the user's own assumption, not a guaranteed, promised or predicted return. Projected amounts illustrate what the entered figures would produce if that return were earned every month; actual results will differ."
              }
            ]
            """;

    private GoalProjectionExamples() {}
}
