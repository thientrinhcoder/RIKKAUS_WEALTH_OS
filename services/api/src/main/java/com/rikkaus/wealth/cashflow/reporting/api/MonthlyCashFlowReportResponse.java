package com.rikkaus.wealth.cashflow.reporting.api;

import com.rikkaus.wealth.cashflow.domain.CashFlowCategory;
import com.rikkaus.wealth.cashflow.domain.CashFlowEntry;
import com.rikkaus.wealth.cashflow.domain.CashFlowEntryType;
import com.rikkaus.wealth.cashflow.domain.CashFlowRecurrence;
import com.rikkaus.wealth.cashflow.reporting.application.MonthlyCashFlowReport;
import com.rikkaus.wealth.shared.money.Currency;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * One month of the signed-in user's cash flow, as the API describes it.
 *
 * <p>Every VND amount is a sum over {@code occurrences}, so a client — or a reviewer with {@code jq} —
 * can add the lines up and arrive at each figure. That is the reconciliation the feature requires,
 * carried in the shape of the response rather than promised beside it.
 */
@Schema(
        name = "MonthlyCashFlowReport",
        description = "Income, expense, net, composition and passive income of one calendar month, in VND.")
public record MonthlyCashFlowReportResponse(
        @Schema(description = "The month reported, YYYY-MM.", example = "2026-09") String month,
        @Schema(description = "The first day of the month, included.", example = "2026-09-01") LocalDate from,
        @Schema(description = "The last day of the month, included.", example = "2026-09-30") LocalDate to,
        @Schema(description = "The currency every amount except originalAmount and unconverted is in.", example = "VND")
                Currency currency,
        @Schema(
                        description =
                                "The USD/VND rate the user has declared, applied to every USD line. Null when none is"
                                        + " declared, in which case no USD line is converted.")
                Rate exchangeRate,
        @Schema(
                        description =
                                "False when a USD line could not be converted for lack of a rate. The totals then"
                                        + " cover only the converted lines and unconverted says what was left out.",
                        example = "true")
                boolean complete,
        Totals totals,
        Unconverted unconverted,
        @Schema(description = "Income by category, largest amount first, then by category name.")
                List<CategoryShare> incomeComposition,
        @Schema(description = "Expense by category, largest amount first, then by category name.")
                List<CategoryShare> expenseComposition,
        PassiveIncome passiveIncome,
        @Schema(
                        description =
                                "Every occurrence in the month, ordered by date and then by sourceEntryId, as"
                                        + " GET /api/v1/cash-flow-occurrences orders them.")
                List<Line> occurrences) {

    static MonthlyCashFlowReportResponse of(MonthlyCashFlowReport report) {
        return new MonthlyCashFlowReportResponse(
                report.month().value().toString(),
                report.month().first(),
                report.month().last(),
                Currency.REPORTING,
                report.exchangeRate()
                        .map(rate -> new Rate(rate.rate(), rate.effectiveDate()))
                        .orElse(null),
                report.complete(),
                new Totals(report.totals().income(), report.totals().expense(), report.totals().net()),
                new Unconverted(
                        report.unconverted().occurrenceCount(),
                        report.unconverted().income(),
                        report.unconverted().expense()),
                report.incomeComposition().stream().map(CategoryShare::of).toList(),
                report.expenseComposition().stream().map(CategoryShare::of).toList(),
                PassiveIncome.of(report.passiveIncome()),
                report.lines().stream().map(Line::of).toList());
    }

    @Schema(name = "MonthlyCashFlowReportRate", description = "The declared rate the report applied.")
    public record Rate(
            @Schema(description = "How many VND one USD buys.", example = "25100.000000") BigDecimal usdToVndRate,
            @Schema(description = "The date the user declared the rate took effect.", example = "2026-09-12")
                    LocalDate effectiveDate) {}

    @Schema(name = "MonthlyCashFlowReportTotals", description = "The month's totals over converted lines.")
    public record Totals(
            @Schema(description = "Sum of the INCOME lines' amount.", example = "180000000.00") BigDecimal income,
            @Schema(description = "Sum of the EXPENSE lines' amount.", example = "92000000.00") BigDecimal expense,
            @Schema(description = "income minus expense, and nothing else. Negative when spending exceeds income.", example = "88000000.00")
                    BigDecimal net) {}

    @Schema(
            name = "MonthlyCashFlowReportUnconverted",
            description = "The USD lines left out of every total because no rate is declared.")
    public record Unconverted(
            @Schema(description = "How many lines have a null amount.", example = "0") int occurrenceCount,
            @Schema(description = "Sum of the unconverted INCOME lines' originalAmount, in USD.", example = "0.00")
                    BigDecimal income,
            @Schema(description = "Sum of the unconverted EXPENSE lines' originalAmount, in USD.", example = "0.00")
                    BigDecimal expense) {}

    @Schema(name = "MonthlyCashFlowReportCategoryShare", description = "One category's part of its type's total.")
    public record CategoryShare(
            @Schema(example = "HOUSING") CashFlowCategory category,
            @Schema(description = "Sum of the category's converted lines.", example = "18750000.00") BigDecimal amount,
            @Schema(
                            description =
                                    "Percent of the type's total at one decimal. The rows of one composition are"
                                            + " apportioned by largest remainder so they sum to exactly 100.0. Null"
                                            + " when the type's total is zero.",
                            example = "20.4")
                    BigDecimal sharePercent,
            @Schema(description = "How many lines of the month are in this category.", example = "1")
                    int occurrenceCount,
            @Schema(
                            description = "True when a line of this category is unconverted and left out of amount.",
                            example = "false")
                    boolean partial) {

        static CategoryShare of(MonthlyCashFlowReport.CategoryShare share) {
            return new CategoryShare(
                    share.category(), share.amount(), share.sharePercent(), share.occurrenceCount(), share.partial());
        }
    }

    @Schema(
            name = "MonthlyCashFlowReportPassiveIncome",
            description =
                    "Income linked to an asset, grouped by that asset. Passive income is decided by the link the user"
                            + " entered, never inferred from a category.")
    public record PassiveIncome(
            @Schema(description = "Sum of byAsset.", example = "22500000.00") BigDecimal total,
            @Schema(
                            description = "total as a percent of total income, one decimal. Null when there is no income.",
                            example = "12.5")
                    BigDecimal sharePercentOfIncome,
            @Schema(description = "One row per linked asset, largest amount first, then by assetId.")
                    List<AssetIncome> byAsset,
            Unassigned unassigned) {

        static PassiveIncome of(MonthlyCashFlowReport.PassiveIncome passive) {
            return new PassiveIncome(
                    passive.total(),
                    passive.sharePercentOfIncome(),
                    passive.byAsset().stream()
                            .map(asset -> new AssetIncome(
                                    asset.assetId(),
                                    asset.amount(),
                                    asset.sharePercentOfIncome(),
                                    asset.occurrenceCount(),
                                    asset.partial()))
                            .toList(),
                    new Unassigned(
                            passive.unassigned().amount(),
                            passive.unassigned().occurrenceCount(),
                            passive.unassigned().partial()));
        }
    }

    @Schema(name = "MonthlyCashFlowReportAssetIncome", description = "One asset's linked income in the month.")
    public record AssetIncome(
            @Schema(
                            description = "The asset; GET /api/v1/assets/{assetId} reads its name.",
                            example = "6a1f3c2e-8b47-4d09-9e5a-1c7b2d4f8e60")
                    UUID assetId,
            @Schema(description = "Sum of the lines linked to this asset.", example = "18000000.00") BigDecimal amount,
            @Schema(
                            description = "amount as a percent of total income, one decimal, rounded on its own.",
                            example = "10.0")
                    BigDecimal sharePercentOfIncome,
            @Schema(example = "1") int occurrenceCount,
            @Schema(example = "false") boolean partial) {}

    @Schema(
            name = "MonthlyCashFlowReportUnassigned",
            description =
                    "Income in RENTAL_INCOME, DIVIDEND, DEPOSIT_INTEREST or BOND_OR_FUND_INCOME with no asset link."
                            + " Shown so it can be linked; not counted in passive income.")
    public record Unassigned(
            @Schema(example = "30120000.00") BigDecimal amount,
            @Schema(example = "1") int occurrenceCount,
            @Schema(example = "false") boolean partial) {}

    @Schema(
            name = "MonthlyCashFlowReportLine",
            description = "One occurrence in the month, traceable to its entry by sourceEntryId and occurrenceDate.")
    public record Line(
            @Schema(
                            description = "The entry; GET /api/v1/cash-flow-entries/{id} reads it.",
                            example = "9b4d6f12-7e3a-4c58-b1d9-0a2e5c7f8d61")
                    UUID sourceEntryId,
            @Schema(example = "2026-09-15") LocalDate occurrenceDate,
            @Schema(description = "Position in the entry's series, 1 for a one-time entry.", example = "2") int sequence,
            @Schema(example = "false") boolean shiftedToMonthEnd,
            @Schema(example = "INCOME") CashFlowEntryType entryType,
            @Schema(example = "DEPOSIT_INTEREST") CashFlowCategory category,
            @Schema(description = "Null for a one-time entry.", example = "YEARLY")
                    CashFlowRecurrence recurrence,
            @Schema(description = "The asset the entry is linked to. Null when it has none.") UUID linkedAssetId,
            @Schema(description = "The entry's note. Null when it has none.", example = "Lãi tiền gửi USD") String notes,
            @Schema(description = "The entry's amount in its own currency.", example = "1200.00")
                    BigDecimal originalAmount,
            @Schema(example = "USD") Currency originalCurrency,
            @Schema(
                            description =
                                    "The amount in VND: the original for a VND entry, originalAmount times"
                                            + " exchangeRate.usdToVndRate rounded half-up to whole dong for a USD one."
                                            + " Null for a USD entry when no rate is declared.",
                            example = "30120000.00")
                    BigDecimal amount) {

        static Line of(MonthlyCashFlowReport.Line line) {
            CashFlowEntry entry = line.occurrence().entry();
            return new Line(
                    entry.getId(),
                    line.occurrence().date(),
                    line.occurrence().sequence(),
                    line.occurrence().shiftedToMonthEnd(),
                    entry.getEntryType(),
                    entry.getCategory(),
                    entry.getRecurrence(),
                    entry.getLinkedAssetId(),
                    entry.getNotes(),
                    entry.getAmount(),
                    entry.getCurrency(),
                    line.amount());
        }
    }
}
