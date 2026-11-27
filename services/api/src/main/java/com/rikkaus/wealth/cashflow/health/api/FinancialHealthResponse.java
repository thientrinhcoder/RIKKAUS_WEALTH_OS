package com.rikkaus.wealth.cashflow.health.api;

import com.rikkaus.wealth.cashflow.domain.CashFlowCategory;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.Assumptions;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.Availability;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.Band;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.BandNotDeterminedReason;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.Calculation;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.Comparison;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.Formula;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.Indicator;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.IndicatorId;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.NotEvaluatedReason;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.Reason;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.Role;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.Rule;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.RuleId;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.Source;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.SourceType;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.Status;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.Thresholds;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.Unit;
import com.rikkaus.wealth.shared.money.Currency;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * The five indicators and six rules, as the API describes them. No Vietnamese text: the client maps every
 * constant to the accepted design's copy.
 */
@Schema(description = "The signed-in user's five financial-health indicators and six insight rules for one month.")
public record FinancialHealthResponse(
        FinancialHealthAssumptions assumptions,
        FinancialHealthThresholds thresholds,
        @ArraySchema(
                        arraySchema =
                                @Schema(
                                        description =
                                                "Always five, in the order EMERGENCY_RESERVE_MONTHS, DEBT_PAYMENT_BURDEN,"
                                                        + " FORECAST_MINIMUM_CASH_BALANCE, PASSIVE_INCOME_SHARE,"
                                                        + " ESSENTIAL_EXPENSE_SHARE."),
                        minItems = 5,
                        maxItems = 5)
                List<FinancialHealthIndicator> indicators,
        @ArraySchema(
                        arraySchema =
                                @Schema(
                                        description =
                                                "Always six, in the order SPENDING_EXCEEDS_INCOME, FORECAST_CASH_NEGATIVE,"
                                                        + " RESERVE_BELOW_REFERENCE, HIGH_DEBT_PAYMENT_BURDEN,"
                                                        + " LIQUIDITY_SHORT_FOR_UPCOMING_OBLIGATIONS,"
                                                        + " INCOME_CONCENTRATION."),
                        minItems = 6,
                        maxItems = 6)
                List<FinancialHealthRule> rules) {

    static FinancialHealthResponse of(FinancialHealth health) {
        Assumptions a = health.assumptions();
        Thresholds t = health.thresholds();
        return new FinancialHealthResponse(
                new FinancialHealthAssumptions(
                        a.asOfDate(),
                        a.timeZone(),
                        a.month().toString(),
                        a.month().atDay(1),
                        a.month().atEndOfMonth(),
                        new DateRange(a.forecastFrom(), a.forecastTo()),
                        new DateRange(a.obligationWindowFrom(), a.obligationWindowTo()),
                        a.liquidAssetCategories(),
                        a.cashAccountCategories(),
                        a.essentialCategories(),
                        a.debtPaymentCategories(),
                        a.exchangeRate() == null
                                ? null
                                : new FinancialHealthExchangeRate(a.exchangeRate().rate(), a.exchangeRate().effectiveDate())),
                new FinancialHealthThresholds(
                        t.reserveOkMinMonths(),
                        t.minReserveMonths(),
                        t.debtBurdenOkMax(),
                        t.maxDebtPaymentBurden(),
                        t.passiveIncomeHighMin(),
                        t.passiveIncomeMediumMin(),
                        t.essentialShareOkMax(),
                        t.essentialShareWatchMax(),
                        t.dueWindowDays(),
                        t.maxIncomeSourceShare()),
                health.indicators().stream().map(FinancialHealthResponse::indicator).toList(),
                health.rules().stream().map(FinancialHealthResponse::rule).toList());
    }

    private static FinancialHealthIndicator indicator(Indicator indicator) {
        Calculation c = indicator.calculation();
        return new FinancialHealthIndicator(
                indicator.indicatorId(),
                indicator.availability(),
                indicator.value(),
                indicator.unit(),
                indicator.band(),
                indicator.bandNotDeterminedReason(),
                indicator.unavailableReason(),
                indicator.partialReason(),
                new FinancialHealthCalculation(
                        c.formula(),
                        c.numeratorVnd(),
                        c.denominatorVnd(),
                        c.referenceVnd(),
                        c.observedOn(),
                        c.sources().stream().map(FinancialHealthResponse::source).toList()));
    }

    private static FinancialHealthRule rule(Rule rule) {
        return new FinancialHealthRule(
                rule.ruleId(),
                rule.status(),
                rule.dependsOn(),
                rule.observed() == null
                        ? null
                        : new FinancialHealthObserved(
                                rule.observed().value(),
                                rule.observed().unit(),
                                rule.observed().subject(),
                                rule.observed().date()),
                new FinancialHealthThreshold(
                        rule.threshold().comparison(),
                        rule.threshold().value(),
                        rule.threshold().unit(),
                        rule.threshold().property()),
                rule.evidence().stream().map(FinancialHealthResponse::source).toList(),
                rule.notEvaluatedReason());
    }

    private static FinancialHealthSource source(Source source) {
        return new FinancialHealthSource(
                source.sourceType(),
                source.sourceId(),
                source.category(),
                source.role(),
                source.date(),
                source.amount(),
                source.currency(),
                source.amountVnd(),
                source.cadence(),
                source.monthlyEquivalentVnd());
    }

    @Schema(
            description =
                    "Every input a figure depends on that is not a record, so the result can be reproduced: the"
                            + " day, the month, the forecast horizon, the obligation window, the category groups and"
                            + " the declared rate.")
    public record FinancialHealthAssumptions(
            @Schema(description = "Today in timeZone; the default month and both windows count from it.", example = "2026-11-27")
                    LocalDate asOfDate,
            @Schema(example = "Asia/Ho_Chi_Minh") String timeZone,
            @Schema(
                            description = "The month the cash-flow indicators read, YYYY-MM: the query's month or the current one.",
                            example = "2026-11")
                    String month,
            @Schema(example = "2026-11-01") LocalDate monthFrom,
            @Schema(example = "2026-11-30") LocalDate monthTo,
            @Schema(
                            description =
                                    "The forecast FORECAST_MINIMUM_CASH_BALANCE reads: always the three months after the"
                                            + " current one, as GET /api/v1/cash-flow-forecast without from, whatever"
                                            + " month is asked for.")
                    DateRange forecastHorizon,
            @Schema(
                            description =
                                    "asOfDate to asOfDate plus thresholds.dueWindowDays, both included: where"
                                            + " LIQUIDITY_SHORT_FOR_UPCOMING_OBLIGATIONS looks for obligations.")
                    DateRange obligationWindow,
            @Schema(
                            description = "The asset categories of the liquid group, the reserve's numerator.",
                            example = "[\"CASH\", \"BANK_ACCOUNT\", \"TERM_DEPOSIT\"]")
                    List<String> liquidAssetCategories,
            @Schema(
                            description =
                                    "The asset categories the forecast starts from and the liquidity rule compares"
                                            + " with what falls due.",
                            example = "[\"CASH\", \"BANK_ACCOUNT\"]")
                    List<String> cashAccountCategories,
            @Schema(
                            description =
                                    "The expense categories counted as essential, configured under"
                                            + " rikkaus.cashflow.health.essential-categories.")
                    List<CashFlowCategory> essentialCategories,
            @Schema(
                            description =
                                    "The only categories counted as debt payments; a HOUSING entry linked to the same"
                                            + " loan is not added.")
                    List<CashFlowCategory> debtPaymentCategories,
            @Schema(description = "The declared USD/VND rate every USD record was converted with; null when none is declared.")
                    FinancialHealthExchangeRate exchangeRate) {}

    @Schema(description = "Two days, both included.")
    public record DateRange(
            @Schema(example = "2026-12-01") LocalDate from, @Schema(example = "2027-02-28") LocalDate to) {}

    @Schema(description = "The declared rate: VND for one USD, and the day the user says it was true.")
    public record FinancialHealthExchangeRate(
            @Schema(example = "25100.000000") BigDecimal usdToVndRate,
            @Schema(example = "2026-09-12") LocalDate effectiveDate) {}

    @Schema(
            description =
                    "Every system reference value in force, configured centrally under rikkaus.cashflow.health."
                            + " The review fixture of design #81, not an approved business configuration. A value"
                            + " equal to a limit is not beyond it.")
    public record FinancialHealthThresholds(
            @Schema(description = "EMERGENCY_RESERVE_MONTHS is OK at and above this.", example = "6.0")
                    BigDecimal reserveOkMinMonths,
            @Schema(
                            description =
                                    "EMERGENCY_RESERVE_MONTHS is ATTENTION below this and WATCH from here to"
                                            + " reserveOkMinMonths; RESERVE_BELOW_REFERENCE fires below it.",
                            example = "3.0")
                    BigDecimal minReserveMonths,
            @Schema(description = "DEBT_PAYMENT_BURDEN is OK at and below this.", example = "0.250000")
                    BigDecimal debtBurdenOkMax,
            @Schema(
                            description =
                                    "DEBT_PAYMENT_BURDEN is WATCH up to this and ATTENTION above it;"
                                            + " HIGH_DEBT_PAYMENT_BURDEN fires above it.",
                            example = "0.350000")
                    BigDecimal maxDebtPaymentBurden,
            @Schema(description = "PASSIVE_INCOME_SHARE is HIGH at and above this.", example = "0.500000")
                    BigDecimal passiveIncomeHighMin,
            @Schema(description = "PASSIVE_INCOME_SHARE is MEDIUM at and above this, LOW below.", example = "0.200000")
                    BigDecimal passiveIncomeMediumMin,
            @Schema(description = "ESSENTIAL_EXPENSE_SHARE is OK at and below this.", example = "0.700000")
                    BigDecimal essentialShareOkMax,
            @Schema(
                            description = "ESSENTIAL_EXPENSE_SHARE is WATCH up to this and ATTENTION above it.",
                            example = "0.800000")
                    BigDecimal essentialShareWatchMax,
            @Schema(description = "The days after asOfDate an obligation still counts as upcoming.", example = "30")
                    int dueWindowDays,
            @Schema(description = "INCOME_CONCENTRATION fires above this share.", example = "0.600000")
                    BigDecimal maxIncomeSourceShare) {}

    @Schema(description = "One indicator.")
    public record FinancialHealthIndicator(
            @Schema(description = "A stable constant the client maps to its copy.", example = "EMERGENCY_RESERVE_MONTHS")
                    IndicatorId indicatorId,
            @Schema(
                            description =
                                    "COMPLETE: every input read and converted. PARTIAL: computed from what could be"
                                            + " converted, a USD record left out for want of a rate (or, for the minimum"
                                            + " balance, a forecast record the user can fix). UNAVAILABLE: nothing could"
                                            + " be computed.",
                            example = "COMPLETE")
                    Availability availability,
            @Schema(
                            description =
                                    "MONTHS at one decimal, a FRACTION at six (HALF_UP), or VND at two; null exactly"
                                            + " when UNAVAILABLE. Never 0 in place of a missing denominator.",
                            example = "30.0")
                    BigDecimal value,
            @Schema(example = "MONTHS") Unit unit,
            @Schema(
                            description =
                                    "OK, WATCH or ATTENTION against thresholds (the minimum balance against"
                                            + " calculation.referenceVnd and zero); HIGH, MEDIUM or LOW for"
                                            + " PASSIVE_INCOME_SHARE, informational only. null when not determined.",
                            example = "OK")
                    Band band,
            @Schema(description = "Why band is null; null when band is set.") BandNotDeterminedReason bandNotDeterminedReason,
            @Schema(description = "Why nothing could be computed; set exactly when UNAVAILABLE.") Reason unavailableReason,
            @Schema(description = "What the value leaves out; set exactly when PARTIAL.") Reason partialReason,
            FinancialHealthCalculation calculation) {}

    @Schema(description = "How the value was obtained and the records behind it.")
    public record FinancialHealthCalculation(
            @Schema(example = "LIQUID_ASSETS_OVER_MONTHLY_RECURRING_ESSENTIAL_EXPENSE") Formula formula,
            @Schema(
                            description =
                                    "The VND amount divided: the sum of the NUMERATOR or SHARE_PART sources with an"
                                            + " amountVnd. null for the minimum balance and when UNAVAILABLE.",
                            example = "1850000000.00")
                    BigDecimal numeratorVnd,
            @Schema(
                            description =
                                    "The VND amount divided by: the sum of the DENOMINATOR sources' monthly"
                                            + " equivalents for the reserve, the month's total income or expense for a"
                                            + " share or the burden. null for the minimum balance and when UNAVAILABLE.",
                            example = "61600000.00")
                    BigDecimal denominatorVnd,
            @Schema(
                            description =
                                    "For FORECAST_MINIMUM_CASH_BALANCE: the month of recurring essential expense the"
                                            + " value is banded against (OK at and above it, WATCH from zero, ATTENTION"
                                            + " below zero); null otherwise or when it cannot be known.")
                    BigDecimal referenceVnd,
            @Schema(description = "For FORECAST_MINIMUM_CASH_BALANCE: the earliest date the lowest balance is reached.")
                    LocalDate observedOn,
            @ArraySchema(
                            arraySchema =
                                    @Schema(
                                            description =
                                                    "The records behind the figure, a USD one without a declared rate with"
                                                            + " amountVnd null. Reserve: liquid assets (NUMERATOR) and"
                                                            + " recurring essential expenses (DENOMINATOR). Burden: debt"
                                                            + " payments (NUMERATOR) and income occurrences (DENOMINATOR)."
                                                            + " Shares: the month's income or expense occurrences,"
                                                            + " SHARE_PART or SHARE_REST. Minimum balance: OPENING_BALANCE"
                                                            + " accounts, then every PROJECTED_INCOME and"
                                                            + " PROJECTED_EXPENSE amount up to observedOn, so the opening"
                                                            + " balance plus them is value."))
                    List<FinancialHealthSource> sources) {}

    @Schema(description = "One rule's result.")
    public record FinancialHealthRule(
            @Schema(example = "INCOME_CONCENTRATION") RuleId ruleId,
            @Schema(
                            description =
                                    "ATTENTION when observed.value compares with threshold.value as"
                                            + " threshold.comparison says; OK when it does not; NOT_EVALUATED when it"
                                            + " could not be judged. Never OK over a partial or unavailable input.",
                            example = "ATTENTION")
                    Status status,
            @Schema(description = "The indicator whose value the rule judges; null when it reads the records itself.")
                    IndicatorId dependsOn,
            @Schema(description = "What the rule measured; null exactly when NOT_EVALUATED.") FinancialHealthObserved observed,
            FinancialHealthThreshold threshold,
            @ArraySchema(
                            arraySchema =
                                    @Schema(
                                            description =
                                                    "The records behind the judgement. A rule on an indicator: that"
                                                            + " indicator's sources. Spending: the month's occurrences."
                                                            + " Liquidity: the CASH_ACCOUNT assets, then every"
                                                            + " PAYMENT_DUE and MATURITY_DUE in the window by date."
                                                            + " Concentration: the occurrences of the largest category."))
                    List<FinancialHealthSource> evidence,
            @Schema(description = "Why the rule was not judged; set exactly when NOT_EVALUATED.")
                    NotEvaluatedReason notEvaluatedReason) {}

    @Schema(description = "The measured value.")
    public record FinancialHealthObserved(
            @Schema(
                            description =
                                    "Spending: income minus expense, VND. Forecast: the lowest projected balance, VND."
                                            + " Reserve, burden: the indicator's value. Liquidity: cash and bank accounts,"
                                            + " VND. Concentration: the largest category's share, FRACTION.",
                            example = "0.733333")
                    BigDecimal value,
            @Schema(example = "FRACTION") Unit unit,
            @Schema(description = "The largest income category for INCOME_CONCENTRATION; null otherwise.", example = "SALARY")
                    String subject,
            @Schema(description = "The date of the lowest balance for FORECAST_CASH_NEGATIVE; null otherwise.")
                    LocalDate date) {}

    @Schema(description = "What the rule was judged against.")
    public record FinancialHealthThreshold(
            @Schema(
                            description = "LESS_THAN: ATTENTION when observed.value < value. GREATER_THAN: when >.",
                            example = "GREATER_THAN")
                    Comparison comparison,
            @Schema(
                            description =
                                    "Zero for spending and forecast, a configured value, or for liquidity the VND sum of"
                                            + " the obligations in assumptions.obligationWindow; null only when that sum"
                                            + " holds a USD amount with no declared rate.",
                            example = "0.600000")
                    BigDecimal value,
            @Schema(example = "FRACTION") Unit unit,
            @Schema(
                            description =
                                    "The rikkaus.cashflow.health property the value comes from (for liquidity, the"
                                            + " window it is summed over); null for a threshold fixed by the rule.",
                            example = "max-income-source-share")
                    String property) {}

    @Schema(description = "One source record and the figures read from it.")
    public record FinancialHealthSource(
            @Schema(example = "CASH_FLOW_ENTRY") SourceType sourceType,
            @Schema(
                            description =
                                    "GET /api/v1/cash-flow-entries/{id}, /api/v1/assets/{id} or /api/v1/liabilities/{id}"
                                            + " reads it.",
                            example = "6a1f3c9e-2b47-4d85-9e10-c7b4a2d8f531")
                    UUID sourceId,
            @Schema(description = "The record's category code.", example = "SALARY") String category,
            @Schema(example = "SHARE_REST") Role role,
            @Schema(
                            description =
                                    "The occurrence date of a month's line, the effective date of a recurring essential"
                                            + " expense, an asset's valuation date, a projected date, or a due date.",
                            example = "2026-11-05")
                    LocalDate date,
            @Schema(description = "In the record's own currency.", example = "132000000.00") BigDecimal amount,
            @Schema(example = "VND") Currency currency,
            @Schema(description = "amount in VND; null when it is USD and no rate is declared.", example = "132000000.00")
                    BigDecimal amountVnd,
            @Schema(
                            description =
                                    "MONTHLY, QUARTERLY or YEARLY for a recurring entry, a liability's payment"
                                            + " frequency; null for a one-time entry or an asset.",
                            example = "MONTHLY")
                    String cadence,
            @Schema(
                            description =
                                    "For a reserve DENOMINATOR row: amountVnd divided by the months its cadence spans"
                                            + " (1, 3 or 12), HALF_UP to two decimals; null otherwise.")
                    BigDecimal monthlyEquivalentVnd) {}
}
