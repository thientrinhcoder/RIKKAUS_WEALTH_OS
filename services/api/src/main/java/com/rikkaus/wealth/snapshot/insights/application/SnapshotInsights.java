package com.rikkaus.wealth.snapshot.insights.application;

import com.rikkaus.wealth.shared.money.Currency;
import com.rikkaus.wealth.snapshot.application.WealthSnapshot.SourceType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * The six insight rules of the backbone, judged over one wealth snapshot.
 *
 * <p>Every insight carries what it observed, the threshold it was judged against and the source records
 * behind the observation, so the result can be explained to the user and reproduced from the response
 * alone. An observation that cannot be made is {@code null} with a reason, never zero.
 *
 * @param asOfDate the snapshot's day in {@code Asia/Ho_Chi_Minh}; every age and due day counts from it
 * @param complete the snapshot's own flag: false when a USD record could not be converted
 * @param thresholds every threshold in force, whichever rule it belongs to
 * @param insights one per {@link RuleId}, in the order the enum declares
 */
public record SnapshotInsights(
        LocalDate asOfDate, boolean complete, Thresholds thresholds, List<Insight> insights) {

    /** Declared in the order the response lists the insights. */
    public enum RuleId {
        CATEGORY_CONCENTRATION,
        DEBT_TO_ASSET_RATIO,
        LOW_LIQUIDITY_SHARE,
        UPCOMING_LIABILITY_DUE,
        STALE_VALUATION,
        STALE_EXCHANGE_RATE
    }

    public enum Status {
        /** The observation is beyond the threshold. */
        ATTENTION,
        /** The observation was made and is within the threshold. */
        OK,
        /** The observation could not be made; {@code unavailableReason} says why. */
        UNAVAILABLE
    }

    public enum UnavailableReason {
        /** The user has no asset, so there is no total, no share and no valuation to judge. */
        NO_ASSETS,
        /** The user has no liability, so nothing can fall due. */
        NO_LIABILITIES,
        /** The user's liabilities state neither a next payment date nor a maturity date. */
        NO_DUE_DATES,
        /** No USD/VND rate is declared, and the rule needs it: a USD record to convert, or its date. */
        MISSING_EXCHANGE_RATE,
        /** Every asset is valued at zero, so a share of total assets would divide by zero. */
        ZERO_TOTAL_ASSETS
    }

    public enum Unit {
        /** A share or ratio as a fraction with six decimals: 0.624896 is 62.4896 %. */
        FRACTION,
        /** Whole calendar days in {@code Asia/Ho_Chi_Minh}. */
        DAYS
    }

    public enum Comparison {
        /** {@code ATTENTION} when the observed value is strictly greater than the threshold. */
        GREATER_THAN,
        /** {@code ATTENTION} when the observed value is less than or equal to the threshold. */
        AT_MOST
    }

    /** The date an evidence row carries, and therefore how its day count reads. */
    public enum DateKind {
        VALUATION_DATE,
        UPDATED_ON,
        NEXT_PAYMENT_DATE,
        MATURITY_DATE,
        RATE_EFFECTIVE_DATE
    }

    /** The thresholds in force. Fractions carry six decimals. */
    public record Thresholds(
            BigDecimal maxCategoryShare,
            BigDecimal maxDebtToAssetRatio,
            BigDecimal maxLowLiquidityShare,
            int dueWindowDays,
            int maxValuationAgeDays,
            int maxExchangeRateAgeDays) {}

    /**
     * One rule's result.
     *
     * @param observed null exactly when {@code status} is {@code UNAVAILABLE}
     * @param unavailableReason set exactly when {@code status} is {@code UNAVAILABLE}
     */
    public record Insight(
            RuleId ruleId,
            Status status,
            Observed observed,
            Threshold threshold,
            List<Evidence> evidence,
            UnavailableReason unavailableReason) {}

    /**
     * What the rule measured.
     *
     * @param subject the category code the concentration rule found largest; null for every other rule
     * @param numeratorVnd for a {@code FRACTION}, the VND amount divided; null for {@code DAYS}
     * @param denominatorVnd for a {@code FRACTION}, the VND amount divided by; null for {@code DAYS}
     * @param date for {@code DAYS}, the date counted from or to; null for {@code FRACTION}
     */
    public record Observed(
            BigDecimal value,
            Unit unit,
            String subject,
            BigDecimal numeratorVnd,
            BigDecimal denominatorVnd,
            LocalDate date) {}

    public record Threshold(Comparison comparison, BigDecimal value, Unit unit) {}

    /**
     * One source record behind an insight, with the figures the rule read from it.
     *
     * @param ageDays {@code asOfDate} minus {@code date}, for a valuation, update or rate date
     * @param daysUntilDue {@code date} minus {@code asOfDate}, for a payment or maturity date; negative
     *     when overdue
     * @param amount the record's amount in its own currency: an asset's value, a liability's balance, the
     *     recurring payment of a {@code NEXT_PAYMENT_DATE} or the balance of a {@code MATURITY_DATE}
     * @param amountVnd {@code amount} in VND, null when it is USD and no rate is declared
     * @param usdToVndRate the declared rate, on the {@code EXCHANGE_RATE} row only
     */
    public record Evidence(
            SourceType sourceType,
            UUID sourceId,
            String name,
            String category,
            String liquidity,
            DateKind dateKind,
            LocalDate date,
            Integer ageDays,
            Integer daysUntilDue,
            Boolean overdue,
            BigDecimal amount,
            Currency currency,
            BigDecimal amountVnd,
            BigDecimal usdToVndRate) {}
}
