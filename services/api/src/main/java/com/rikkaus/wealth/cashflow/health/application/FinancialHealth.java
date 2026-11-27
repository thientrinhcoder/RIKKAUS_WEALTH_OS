package com.rikkaus.wealth.cashflow.health.application;

import com.rikkaus.wealth.cashflow.domain.CashFlowCategory;
import com.rikkaus.wealth.shared.money.Currency;
import com.rikkaus.wealth.shared.money.ExchangeRate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * The five financial-health indicators and the six insight rules of the backbone, for one owner and one
 * month.
 *
 * <p>Every indicator carries its numerator, denominator and the source records behind both, and every
 * rule the threshold it was judged against, so the result can be explained to the user and reproduced
 * from the response alone. A figure that cannot be computed is {@code null} with a reason, never zero.
 *
 * @param assumptions the inputs every figure depends on that are not records: dates, categories, rate
 * @param thresholds every threshold in force
 * @param indicators one per {@link IndicatorId}, in the order the enum declares
 * @param rules one per {@link RuleId}, in the order the enum declares
 */
public record FinancialHealth(
        Assumptions assumptions, Thresholds thresholds, List<Indicator> indicators, List<Rule> rules) {

    /** Declared in the order the response lists the indicators. */
    public enum IndicatorId {
        EMERGENCY_RESERVE_MONTHS,
        DEBT_PAYMENT_BURDEN,
        FORECAST_MINIMUM_CASH_BALANCE,
        PASSIVE_INCOME_SHARE,
        ESSENTIAL_EXPENSE_SHARE
    }

    /** Declared in the order the response lists the rules. */
    public enum RuleId {
        SPENDING_EXCEEDS_INCOME,
        FORECAST_CASH_NEGATIVE,
        RESERVE_BELOW_REFERENCE,
        HIGH_DEBT_PAYMENT_BURDEN,
        LIQUIDITY_SHORT_FOR_UPCOMING_OBLIGATIONS,
        INCOME_CONCENTRATION
    }

    public enum Availability {
        /** Every input was read and converted. */
        COMPLETE,
        /** Computed from the inputs that could be converted; a USD record is left out for want of a rate. */
        PARTIAL,
        /** Nothing could be computed; {@code unavailableReason} says why. */
        UNAVAILABLE
    }

    /** Why an indicator is unavailable or partial, or why a rule could not be judged on its own inputs. */
    public enum Reason {
        /** No asset in a liquid category. */
        NO_LIQUID_ASSETS,
        /** No recurring essential expense is in effect in the month, so a month of it is unknown. */
        NO_RECURRING_ESSENTIAL_EXPENSES,
        /** No income in the month. */
        NO_INCOME,
        /** No expense in the month. */
        NO_EXPENSES,
        /** No cash or bank account, so the forecast has no balance to start from. */
        NO_CASH_ACCOUNTS,
        /** No liability, so nothing can fall due. */
        NO_LIABILITIES,
        /** No occurrence of any entry in the month. */
        NO_CASH_FLOW_IN_MONTH,
        /** A USD record cannot be converted because no USD/VND rate is declared. */
        MISSING_EXCHANGE_RATE,
        /** The forecast leaves out a record the user can fix, for a reason other than a missing rate. */
        FORECAST_INCOMPLETE
    }

    /** Why a rule was not judged. */
    public enum NotEvaluatedReason {
        /** The indicator the rule reads is {@code PARTIAL}: judging a partial value could report a false OK. */
        INDICATOR_PARTIAL,
        /** The indicator the rule reads is {@code UNAVAILABLE}. */
        INDICATOR_UNAVAILABLE,
        NO_CASH_FLOW_IN_MONTH,
        NO_INCOME,
        NO_CASH_ACCOUNTS,
        NO_LIABILITIES,
        MISSING_EXCHANGE_RATE
    }

    /** Why an indicator has no band. */
    public enum BandNotDeterminedReason {
        /** The indicator is unavailable. */
        INDICATOR_UNAVAILABLE,
        /** The value is partial, so the band it falls in may not be the one the whole value would. */
        PARTIAL_INPUTS,
        /** The minimum balance is banded against a month of recurring essential expense, and there is none. */
        NO_RECURRING_ESSENTIAL_EXPENSES
    }

    public enum Band {
        OK,
        WATCH,
        ATTENTION,
        /** Passive income only: an informational band, never a judgement. */
        HIGH,
        MEDIUM,
        LOW
    }

    public enum Unit {
        /** Months at one decimal. */
        MONTHS,
        /** A share or ratio with six decimals: 0.833696 is 83.3696 %. */
        FRACTION,
        /** Đồng, at two decimals. */
        VND
    }

    /** How each indicator's value is obtained. Stable constants the client maps to its explanation. */
    public enum Formula {
        LIQUID_ASSETS_OVER_MONTHLY_RECURRING_ESSENTIAL_EXPENSE,
        DEBT_PAYMENTS_OVER_INCOME,
        LOWEST_PROJECTED_CASH_BALANCE,
        ASSET_LINKED_INCOME_OVER_INCOME,
        ESSENTIAL_EXPENSE_OVER_EXPENSE
    }

    public enum Status {
        /** The observation is beyond the threshold. */
        ATTENTION,
        /** The observation was made and is within the threshold. */
        OK,
        /** The rule could not be judged; {@code notEvaluatedReason} says why. */
        NOT_EVALUATED
    }

    public enum Comparison {
        /** {@code ATTENTION} when the observed value is strictly less than the threshold. */
        LESS_THAN,
        /** {@code ATTENTION} when the observed value is strictly greater than the threshold. */
        GREATER_THAN
    }

    public enum SourceType {
        CASH_FLOW_ENTRY,
        ASSET,
        LIABILITY
    }

    /** What a source record contributes. */
    public enum Role {
        /** Counted in the numerator of a ratio of two different groups. */
        NUMERATOR,
        /** Counted in the denominator of a ratio of two different groups. */
        DENOMINATOR,
        /** Of a share: counted in the numerator and in the denominator. */
        SHARE_PART,
        /** Of a share: counted in the denominator only. */
        SHARE_REST,
        /** A cash or bank account the forecast starts from. */
        OPENING_BALANCE,
        /** A forecast amount landing on or before the date of the lowest balance. */
        PROJECTED_INCOME,
        PROJECTED_EXPENSE,
        /** An occurrence of the month. */
        INCOME,
        EXPENSE,
        /** A cash or bank account compared with the obligations due. */
        CASH_ACCOUNT,
        /** A scheduled payment due within the window. */
        PAYMENT_DUE,
        /** A liability whose maturity date is within the window; its whole balance is due. */
        MATURITY_DUE
    }

    /**
     * @param asOfDate today in {@code timeZone}
     * @param month the month the indicators read
     * @param forecastFrom the first day the forecast projects, always the first of next month
     * @param forecastTo the last day it projects
     * @param obligationWindowFrom today, the first day an obligation counts as upcoming
     * @param obligationWindowTo today plus {@code due-window-days}, included
     * @param exchangeRate the declared USD/VND rate, or {@code null} when there is none
     */
    public record Assumptions(
            LocalDate asOfDate,
            String timeZone,
            YearMonth month,
            LocalDate forecastFrom,
            LocalDate forecastTo,
            LocalDate obligationWindowFrom,
            LocalDate obligationWindowTo,
            List<String> liquidAssetCategories,
            List<String> cashAccountCategories,
            List<CashFlowCategory> essentialCategories,
            List<CashFlowCategory> debtPaymentCategories,
            ExchangeRate exchangeRate) {}

    /** The thresholds in force. Fractions carry six decimals, months one. */
    public record Thresholds(
            BigDecimal reserveOkMinMonths,
            BigDecimal minReserveMonths,
            BigDecimal debtBurdenOkMax,
            BigDecimal maxDebtPaymentBurden,
            BigDecimal passiveIncomeHighMin,
            BigDecimal passiveIncomeMediumMin,
            BigDecimal essentialShareOkMax,
            BigDecimal essentialShareWatchMax,
            int dueWindowDays,
            BigDecimal maxIncomeSourceShare) {}

    /**
     * @param value null exactly when {@code availability} is {@code UNAVAILABLE}
     * @param band null when it cannot be determined, with {@code bandNotDeterminedReason} set
     * @param unavailableReason set exactly when {@code UNAVAILABLE}
     * @param partialReason set exactly when {@code PARTIAL}
     */
    public record Indicator(
            IndicatorId indicatorId,
            Availability availability,
            BigDecimal value,
            Unit unit,
            Band band,
            BandNotDeterminedReason bandNotDeterminedReason,
            Reason unavailableReason,
            Reason partialReason,
            Calculation calculation) {}

    /**
     * @param numeratorVnd the VND amount divided, null for the minimum balance and when unavailable
     * @param denominatorVnd the VND amount divided by, null for the minimum balance and when unavailable
     * @param referenceVnd for the minimum balance, the month of recurring essential expense it is banded
     *     against; null otherwise
     * @param observedOn for the minimum balance, the earliest date it is reached; null otherwise
     */
    public record Calculation(
            Formula formula,
            BigDecimal numeratorVnd,
            BigDecimal denominatorVnd,
            BigDecimal referenceVnd,
            LocalDate observedOn,
            List<Source> sources) {}

    /**
     * One record behind a figure.
     *
     * @param date the occurrence date, the valuation date, the projected date or the due date
     * @param amount in the record's own currency
     * @param amountVnd {@code amount} in VND, null when it is USD and no rate is declared
     * @param cadence MONTHLY, QUARTERLY or YEARLY for a recurring entry, a liability's payment frequency,
     *     null otherwise
     * @param monthlyEquivalentVnd for a recurring essential expense of the reserve denominator, its
     *     amount in VND divided by the months its cadence spans; null otherwise
     */
    public record Source(
            SourceType sourceType,
            UUID sourceId,
            String category,
            Role role,
            LocalDate date,
            BigDecimal amount,
            Currency currency,
            BigDecimal amountVnd,
            String cadence,
            BigDecimal monthlyEquivalentVnd) {}

    /**
     * One rule's result.
     *
     * @param dependsOn the indicator whose value the rule judges, null when it reads the records itself
     * @param observed null exactly when {@code status} is {@code NOT_EVALUATED}
     * @param notEvaluatedReason set exactly when {@code status} is {@code NOT_EVALUATED}
     */
    public record Rule(
            RuleId ruleId,
            Status status,
            IndicatorId dependsOn,
            Observed observed,
            Threshold threshold,
            List<Source> evidence,
            NotEvaluatedReason notEvaluatedReason) {}

    /**
     * @param subject the income category the concentration rule found largest; null otherwise
     * @param date the date of the lowest projected balance for the forecast rule; null otherwise
     */
    public record Observed(BigDecimal value, Unit unit, String subject, LocalDate date) {}

    /**
     * @param value null only when the threshold is itself a sum that could not be converted
     * @param property the configuration property the value comes from, or for the liquidity rule the
     *     window it is summed over; null for a threshold fixed by the rule's definition
     */
    public record Threshold(Comparison comparison, BigDecimal value, Unit unit, String property) {}
}
