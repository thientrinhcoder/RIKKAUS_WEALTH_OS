package com.rikkaus.wealth.cashflow.reporting.application;

import com.rikkaus.wealth.cashflow.domain.CashFlowCategory;
import com.rikkaus.wealth.cashflow.domain.CashFlowOccurrence;
import com.rikkaus.wealth.shared.money.ExchangeRate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * One month of the owner's cash flow, summed from the occurrences it lists.
 *
 * <p>Every amount here is in VND at two decimals, and every one of them is a sum over {@link #lines}:
 * nothing is computed from a figure that is not also in the report. That is the reconciliation the
 * feature requires — a reader can add the lines up and arrive at each total — and it is why a line's
 * converted amount is rounded before it is summed rather than after.
 *
 * @param month the month covered
 * @param exchangeRate the declared USD/VND rate applied, empty when the owner has none
 * @param totals income, expense and net over the converted lines
 * @param unconverted the USD lines left out of every sum because no rate is declared
 * @param incomeComposition income by category, largest first
 * @param expenseComposition expense by category, largest first
 * @param passiveIncome income linked to an asset, by asset
 * @param lines every occurrence in the month, in the order of #74
 */
public record MonthlyCashFlowReport(
        ReportMonth month,
        Optional<ExchangeRate> exchangeRate,
        Totals totals,
        Unconverted unconverted,
        List<CategoryShare> incomeComposition,
        List<CategoryShare> expenseComposition,
        PassiveIncome passiveIncome,
        List<Line> lines) {

    /** Whether every line was converted, so the totals cover the whole month. */
    public boolean complete() {
        return unconverted.occurrenceCount() == 0;
    }

    /**
     * One occurrence and its amount in VND.
     *
     * @param occurrence the occurrence of #74, carrying its entry
     * @param amount the amount in VND at two decimals, or {@code null} for a USD entry with no rate
     */
    public record Line(CashFlowOccurrence occurrence, BigDecimal amount) {}

    /** {@code net} is {@code income - expense} and nothing else. */
    public record Totals(BigDecimal income, BigDecimal expense, BigDecimal net) {}

    /**
     * The lines no rate could convert. The two amounts are in USD, the only currency that can be
     * unconverted.
     */
    public record Unconverted(int occurrenceCount, BigDecimal income, BigDecimal expense) {}

    /**
     * One category's part of its type's total.
     *
     * @param sharePercent percent of the type's total at one decimal, apportioned so the column sums
     *     to exactly 100.0; {@code null} when the type's total is zero
     * @param partial whether an unconverted line of this category is left out of {@code amount}
     */
    public record CategoryShare(
            CashFlowCategory category,
            BigDecimal amount,
            BigDecimal sharePercent,
            int occurrenceCount,
            boolean partial) {}

    /**
     * Income linked to an asset, and the passive-kind income that is not.
     *
     * @param total the sum of {@code byAsset}
     * @param sharePercentOfIncome {@code total} as a percent of total income, {@code null} when there
     *     is no income
     * @param byAsset one row per linked asset, largest first
     * @param unassigned income in a passive-kind category with no asset link, kept out of {@code total}
     */
    public record PassiveIncome(
            BigDecimal total,
            BigDecimal sharePercentOfIncome,
            List<AssetIncome> byAsset,
            Unassigned unassigned) {}

    /** One asset's linked income. The share is of total income, rounded on its own. */
    public record AssetIncome(
            UUID assetId,
            BigDecimal amount,
            BigDecimal sharePercentOfIncome,
            int occurrenceCount,
            boolean partial) {}

    /** Passive-kind income with no asset link. */
    public record Unassigned(BigDecimal amount, int occurrenceCount, boolean partial) {}
}
