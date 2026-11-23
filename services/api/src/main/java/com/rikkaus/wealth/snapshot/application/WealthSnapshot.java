package com.rikkaus.wealth.snapshot.application;

import com.rikkaus.wealth.shared.money.Currency;
import com.rikkaus.wealth.shared.money.ExchangeRate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The owner's wealth as of one day: the six dashboard KPIs, the groupings the charts draw, and every
 * asset and liability line they are summed from.
 *
 * <p>Every VND amount is either a real figure or {@code null}. A figure that a missing exchange rate
 * makes unknowable is {@code null}, never zero, and the {@link Aggregate} that holds it still says how
 * much of it is known and how many records were left out.
 *
 * @param asOfDate today in {@code Asia/Ho_Chi_Minh}, the day the due-date window is counted from
 * @param exchangeRate the declared USD/VND rate, empty when none is declared
 * @param complete false when at least one asset or liability line could not be converted
 */
public record WealthSnapshot(
        LocalDate asOfDate,
        Optional<ExchangeRate> exchangeRate,
        boolean complete,
        Kpis kpis,
        List<Group> assetsByCategory,
        List<Group> assetsByLiquidity,
        LargestAssets largestAssets,
        UpcomingDues upcomingDues,
        List<SourceDate> sourceDates,
        List<AssetLine> assets,
        List<LiabilityLine> liabilities) {

    /**
     * A sum over a set of lines that may contain unconverted ones.
     *
     * @param amountVnd the sum, or null when any line in the set is unconverted
     * @param knownAmountVnd the sum of the lines that were converted
     * @param recordCount every line in the set
     * @param excludedRecordCount the lines left out because they could not be converted
     */
    public record Aggregate(
            BigDecimal amountVnd, BigDecimal knownAmountVnd, int recordCount, int excludedRecordCount) {}

    /** One slice of a chart: a category or a liquidity level, and the aggregate of its assets. */
    public record Group(String key, Aggregate aggregate) {}

    /**
     * The six KPIs of the backbone's dashboard.
     *
     * @param netWorth total assets minus total liabilities; null when either is null
     * @param debtToAssetRatio liabilities over assets as a fraction with six decimals; null when either
     *     total is null or total assets are zero
     * @param lastDataUpdate the newest contributing source date; null when there is no source
     */
    public record Kpis(
            Aggregate totalAssets,
            Aggregate totalLiabilities,
            BigDecimal netWorth,
            BigDecimal debtToAssetRatio,
            Aggregate liquidAssets,
            LocalDate lastDataUpdate) {}

    public record AssetLine(
            UUID id,
            String name,
            String category,
            String liquidity,
            BigDecimal value,
            Currency currency,
            BigDecimal valueVnd,
            LocalDate valuationDate) {}

    public record LiabilityLine(
            UUID id,
            String name,
            String category,
            BigDecimal outstandingBalance,
            Currency currency,
            BigDecimal outstandingBalanceVnd,
            LocalDate nextPaymentDate,
            LocalDate maturityDate,
            BigDecimal recurringPaymentAmount,
            String recurringPaymentFrequency,
            LocalDate updatedOn) {}

    /** At most five ranked assets, and how many could not be ranked for want of a rate. */
    public record LargestAssets(List<AssetLine> items, int excludedRecordCount) {}

    public record UpcomingDues(int windowDays, LocalDate windowEnd, List<Due> items) {}

    /**
     * One date a liability falls due.
     *
     * @param amount the recurring payment for a {@code PAYMENT}, the outstanding balance for a {@code
     *     MATURITY}; null when the liability states no recurring payment
     */
    public record Due(
            UUID liabilityId,
            String name,
            String category,
            DueKind dueKind,
            LocalDate dueDate,
            boolean overdue,
            BigDecimal amount,
            Currency currency,
            BigDecimal amountVnd) {}

    /** Declared in the order due items on the same date are listed. */
    public enum DueKind {
        PAYMENT,
        MATURITY
    }

    /** Declared in the order source dates on the same day are listed. */
    public enum SourceType {
        ASSET,
        LIABILITY,
        EXCHANGE_RATE
    }

    /** The date one source contributes; the rate has no id and no name. */
    public record SourceDate(SourceType sourceType, UUID sourceId, String name, LocalDate date) {}
}
