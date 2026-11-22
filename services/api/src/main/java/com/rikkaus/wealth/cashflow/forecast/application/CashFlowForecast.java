package com.rikkaus.wealth.cashflow.forecast.application;

import com.rikkaus.wealth.shared.money.Currency;
import com.rikkaus.wealth.shared.money.ExchangeRate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * A three-month projection of the owner's cash, built from the records they can see.
 *
 * <p>Every VND amount here has already been rounded to whole đồng, one source amount at a time, before
 * anything was added up. That is what lets a client add the items of a month and arrive at exactly the
 * month's total, which is the traceability the feature promises.
 *
 * @param horizon the months covered and the day the forecast was computed
 * @param openingBalance the cash the projection starts from
 * @param months the three months, earliest first
 * @param minimumBalance the lowest projected position in the horizon
 * @param excludedSources the records that could have contributed and did not, each with its reason
 * @param partial whether any month or the opening balance is missing a contribution the user can fix
 * @param exchangeRate the rate USD amounts were converted with, or null when none is declared
 */
public record CashFlowForecast(
        ForecastHorizon horizon,
        OpeningBalance openingBalance,
        List<Month> months,
        MinimumBalance minimumBalance,
        List<Exclusion> excludedSources,
        boolean partial,
        ExchangeRate exchangeRate) {

    /**
     * Which catalog a projected amount or an excluded record comes from. The order is the order items
     * on the same day are listed in, so it is part of the deterministic output.
     */
    public enum SourceType {
        CASH_FLOW_ENTRY,
        LIABILITY,
        ASSET
    }

    /** Whether an item raises or lowers the projected cash. */
    public enum Direction {
        INCOME,
        EXPENSE
    }

    /** Why a record that could have contributed does not. */
    public enum ExclusionReason {
        /** A recurring entry overlapping the horizon whose rule produces no date inside it. */
        NO_OCCURRENCE_IN_HORIZON,
        /** A liability without a payment amount and frequency. */
        NO_PAYMENT_SCHEDULE,
        /** A liability with a payment amount and frequency but no next payment date to count from. */
        NO_NEXT_PAYMENT_DATE,
        /** A liability whose schedule has no payment inside the horizon. */
        NO_PAYMENT_IN_HORIZON,
        /** A liability already counted through the recurring entries linked to it. */
        COVERED_BY_LINKED_ENTRIES,
        /** A USD record while the owner has not declared a USD/VND rate. */
        EXCHANGE_RATE_MISSING
    }

    /** How the lowest projected position reads against the reference threshold. */
    public enum MinimumBalanceStatus {
        ADEQUATE,
        THIN,
        NEGATIVE,
        INSUFFICIENT_DATA
    }

    /**
     * @param amount the sum of the included sources, in VND
     * @param appliedOn the first day of the horizon, where the projection starts
     * @param sources the cash and bank accounts that make it up
     */
    public record OpeningBalance(BigDecimal amount, LocalDate appliedOn, List<OpeningSource> sources) {}

    /**
     * @param sourceId the asset
     * @param category CASH or BANK_ACCOUNT
     * @param valuationDate the date the user states the value was true
     * @param amount the value in the asset's own currency
     * @param currency the asset's currency
     * @param amountVnd the value in VND
     */
    public record OpeningSource(
            UUID sourceId,
            String category,
            LocalDate valuationDate,
            BigDecimal amount,
            Currency currency,
            BigDecimal amountVnd) {}

    /**
     * @param month the calendar month
     * @param income the VND sum of the month's income items
     * @param expense the VND sum of the month's expense items
     * @param net income minus expense
     * @param openingBalance the projected cash at the start of the month's first day
     * @param closingBalance the projected cash at the end of the month's last day
     * @param lowestBalance the lowest of the opening balance and every end-of-day balance in the month
     * @param lowestBalanceDate the earliest date that lowest balance is reached
     * @param partial whether an excluded record would have contributed to this month
     * @param items the month's projected amounts, in output order
     */
    public record Month(
            YearMonth month,
            BigDecimal income,
            BigDecimal expense,
            BigDecimal net,
            BigDecimal openingBalance,
            BigDecimal closingBalance,
            BigDecimal lowestBalance,
            LocalDate lowestBalanceDate,
            boolean partial,
            List<Item> items) {}

    /**
     * @param date the day the amount lands
     * @param direction income or expense
     * @param sourceType the catalog the amount comes from
     * @param sourceId the record in that catalog
     * @param category the record's category
     * @param amount the amount in the record's own currency
     * @param currency the record's currency
     * @param amountVnd the amount in VND
     * @param cadence how often the record repeats, or null for a one-time entry
     * @param sequence the position of this date in the record's series, 1 for its first date
     * @param shiftedToMonthEnd whether the record's day of month does not exist in this month
     */
    public record Item(
            LocalDate date,
            Direction direction,
            SourceType sourceType,
            UUID sourceId,
            String category,
            BigDecimal amount,
            Currency currency,
            BigDecimal amountVnd,
            String cadence,
            int sequence,
            boolean shiftedToMonthEnd) {}

    /**
     * @param date the earliest date the lowest position is reached
     * @param month the month containing that date
     * @param amount the lowest position, in VND
     * @param negative whether it is below zero
     * @param referenceThreshold the average projected expense of one month of the horizon
     * @param status how the position reads against the threshold
     */
    public record MinimumBalance(
            LocalDate date,
            YearMonth month,
            BigDecimal amount,
            boolean negative,
            BigDecimal referenceThreshold,
            MinimumBalanceStatus status) {}

    /**
     * @param sourceType the catalog the record belongs to
     * @param sourceId the record
     * @param reason why it does not contribute
     * @param affectedMonths the horizon months it would have contributed to, when that is known
     * @param linkedEntryIds for a covered liability, the recurring entries that count it instead
     */
    public record Exclusion(
            SourceType sourceType,
            UUID sourceId,
            ExclusionReason reason,
            List<YearMonth> affectedMonths,
            List<UUID> linkedEntryIds) {}
}
