package com.rikkaus.wealth.cashflow.reporting.application;

import com.rikkaus.wealth.cashflow.application.CashFlowOccurrenceService;
import com.rikkaus.wealth.cashflow.domain.CashFlowCategory;
import com.rikkaus.wealth.cashflow.domain.CashFlowEntry;
import com.rikkaus.wealth.cashflow.domain.CashFlowEntryType;
import com.rikkaus.wealth.cashflow.reporting.application.MonthlyCashFlowReport.AssetIncome;
import com.rikkaus.wealth.cashflow.reporting.application.MonthlyCashFlowReport.CategoryShare;
import com.rikkaus.wealth.cashflow.reporting.application.MonthlyCashFlowReport.Line;
import com.rikkaus.wealth.cashflow.reporting.application.MonthlyCashFlowReport.PassiveIncome;
import com.rikkaus.wealth.cashflow.reporting.application.MonthlyCashFlowReport.Totals;
import com.rikkaus.wealth.cashflow.reporting.application.MonthlyCashFlowReport.Unassigned;
import com.rikkaus.wealth.cashflow.reporting.application.MonthlyCashFlowReport.Unconverted;
import com.rikkaus.wealth.shared.money.ConversionTrace;
import com.rikkaus.wealth.shared.money.Currency;
import com.rikkaus.wealth.shared.money.ExchangeRate;
import com.rikkaus.wealth.shared.money.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sums one month of the owner's cash flow from the occurrences #74 expands.
 *
 * <p>Computed on every read and never stored, like the occurrences it is built from: editing an entry
 * or declaring a rate changes the next report with nothing to keep in step.
 *
 * <p>The occurrences come from {@link CashFlowOccurrenceService} unchanged, so the report and the
 * occurrence route can never disagree about which dates fall in a month. Every figure is then a sum
 * over those lines, which is what makes each total reconcile with the lines the response carries.
 */
@Service
public class MonthlyCashFlowReportService {

    /**
     * The income categories that describe money an asset produces.
     *
     * <p>Used for one thing only: an income in one of these with no asset link is shown as
     * unassigned, so a deposit interest the user forgot to link is visible rather than silently
     * counted as active income. It never makes an income passive — only an asset link does, because
     * the accepted design states that the system does not infer passive income from a category.
     */
    static final Set<CashFlowCategory> PASSIVE_KIND =
            EnumSet.of(
                    CashFlowCategory.RENTAL_INCOME,
                    CashFlowCategory.DIVIDEND,
                    CashFlowCategory.DEPOSIT_INTEREST,
                    CashFlowCategory.BOND_OR_FUND_INCOME);

    /** Amounts in the report carry the two decimals the entries are stored with. */
    private static final int AMOUNT_SCALE = 2;

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(AMOUNT_SCALE);
    private static final BigDecimal TENTHS_OF_A_PERCENT = BigDecimal.valueOf(1000);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final CashFlowOccurrenceService occurrences;
    private final DeclaredExchangeRateRepository rates;

    MonthlyCashFlowReportService(
            CashFlowOccurrenceService occurrences, DeclaredExchangeRateRepository rates) {
        this.occurrences = occurrences;
        this.rates = rates;
    }

    @Transactional(readOnly = true)
    public MonthlyCashFlowReport reportFor(UUID ownerId, ReportMonth month) {
        Optional<ExchangeRate> rate =
                rates.findDeclaredRate(ownerId)
                        .map(
                                declared ->
                                        new ExchangeRate(
                                                Currency.USD,
                                                Currency.VND,
                                                declared.getUsdToVndRate(),
                                                declared.getEffectiveDate()));

        List<Line> lines =
                occurrences.occurrencesFor(ownerId, month.window()).stream()
                        .map(occurrence -> new Line(occurrence, inVnd(occurrence.entry(), rate)))
                        .toList();

        List<Line> income = linesOf(lines, CashFlowEntryType.INCOME);
        List<Line> expense = linesOf(lines, CashFlowEntryType.EXPENSE);
        BigDecimal incomeTotal = sum(income);
        BigDecimal expenseTotal = sum(expense);

        return new MonthlyCashFlowReport(
                month,
                rate,
                new Totals(incomeTotal, expenseTotal, incomeTotal.subtract(expenseTotal)),
                unconverted(income, expense),
                composition(income, incomeTotal),
                composition(expense, expenseTotal),
                passiveIncome(income, incomeTotal),
                lines);
    }

    /**
     * The entry's amount in VND, or {@code null} when it is in USD and no rate is declared.
     *
     * <p>A VND amount is kept exactly as stored. A USD amount goes through {@link ConversionTrace},
     * which rounds once to whole dong, and is then written at the report's two decimals — an exact
     * step, since whole dong has no fraction to lose. Each occurrence is converted on its own, so the
     * total of a month is the sum of the amounts its lines show.
     *
     * <p>Never zero for a missing rate. A zero would read as a real amount and understate the month;
     * {@code null} is what lets the report say which lines it could not count.
     */
    private static BigDecimal inVnd(CashFlowEntry entry, Optional<ExchangeRate> rate) {
        return switch (entry.getCurrency()) {
            case VND -> entry.getAmount().setScale(AMOUNT_SCALE);
            case USD ->
                    rate.map(
                                    usdToVnd ->
                                            ConversionTrace.convert(Money.of(entry.getAmount(), Currency.USD), usdToVnd)
                                                    .converted()
                                                    .amount()
                                                    .setScale(AMOUNT_SCALE))
                            .orElse(null);
        };
    }

    private static List<Line> linesOf(List<Line> lines, CashFlowEntryType type) {
        return lines.stream().filter(line -> line.occurrence().entry().getEntryType() == type).toList();
    }

    /** The converted amounts of {@code lines}; an unconverted line adds nothing. */
    private static BigDecimal sum(List<Line> lines) {
        return lines.stream()
                .map(Line::amount)
                .filter(Objects::nonNull)
                .reduce(ZERO, BigDecimal::add);
    }

    private static boolean anyUnconverted(List<Line> lines) {
        return lines.stream().anyMatch(line -> line.amount() == null);
    }

    private static Unconverted unconverted(List<Line> income, List<Line> expense) {
        List<Line> unconvertedIncome = income.stream().filter(line -> line.amount() == null).toList();
        List<Line> unconvertedExpense = expense.stream().filter(line -> line.amount() == null).toList();
        return new Unconverted(
                unconvertedIncome.size() + unconvertedExpense.size(),
                originalSum(unconvertedIncome),
                originalSum(unconvertedExpense));
    }

    private static BigDecimal originalSum(List<Line> lines) {
        return lines.stream()
                .map(line -> line.occurrence().entry().getAmount())
                .reduce(ZERO, BigDecimal::add)
                .setScale(AMOUNT_SCALE);
    }

    /**
     * One row per category present in the month, largest amount first and then by category name, so
     * the order is fixed by the data. A category whose every line is unconverted still has a row, with
     * a zero amount and {@code partial}, so the client can name what is missing.
     */
    private static List<CategoryShare> composition(List<Line> lines, BigDecimal total) {
        Map<CashFlowCategory, List<Line>> byCategory =
                groupBy(lines, line -> line.occurrence().entry().getCategory());
        List<Map.Entry<CashFlowCategory, BigDecimal>> amounts =
                byCategory.entrySet().stream()
                        .map(group -> Map.entry(group.getKey(), sum(group.getValue())))
                        .sorted(
                                Map.Entry.<CashFlowCategory, BigDecimal>comparingByValue()
                                        .reversed()
                                        .thenComparing(group -> group.getKey().name()))
                        .toList();

        List<BigDecimal> shares = apportion(amounts.stream().map(Map.Entry::getValue).toList(), total);
        List<CategoryShare> rows = new ArrayList<>(amounts.size());
        for (int i = 0; i < amounts.size(); i++) {
            CashFlowCategory category = amounts.get(i).getKey();
            List<Line> group = byCategory.get(category);
            rows.add(
                    new CategoryShare(
                            category, amounts.get(i).getValue(), shares.get(i), group.size(), anyUnconverted(group)));
        }
        return List.copyOf(rows);
    }

    private static PassiveIncome passiveIncome(List<Line> income, BigDecimal incomeTotal) {
        Map<UUID, List<Line>> byAsset =
                groupBy(
                        income.stream()
                                .filter(line -> line.occurrence().entry().getLinkedAssetId() != null)
                                .toList(),
                        line -> line.occurrence().entry().getLinkedAssetId());
        List<AssetIncome> assets =
                byAsset.entrySet().stream()
                        .map(
                                group -> {
                                    BigDecimal amount = sum(group.getValue());
                                    return new AssetIncome(
                                            group.getKey(),
                                            amount,
                                            percentOf(amount, incomeTotal),
                                            group.getValue().size(),
                                            anyUnconverted(group.getValue()));
                                })
                        .sorted(
                                Comparator.comparing(AssetIncome::amount)
                                        .reversed()
                                        .thenComparing(asset -> asset.assetId().toString()))
                        .toList();
        BigDecimal total = assets.stream().map(AssetIncome::amount).reduce(ZERO, BigDecimal::add);

        Predicate<Line> unassignedPassiveKind =
                line ->
                        line.occurrence().entry().getLinkedAssetId() == null
                                && PASSIVE_KIND.contains(line.occurrence().entry().getCategory());
        List<Line> unassigned = income.stream().filter(unassignedPassiveKind).toList();

        return new PassiveIncome(
                total,
                percentOf(total, incomeTotal),
                assets,
                new Unassigned(sum(unassigned), unassigned.size(), anyUnconverted(unassigned)));
    }

    /**
     * Percent shares at one decimal that sum to exactly 100.0, by the largest-remainder method.
     *
     * <p>Each row first gets the whole tenths of a percent it has earned, {@code floor(amount * 1000 /
     * total)}. The tenths still missing from 1000 go one each to the rows with the largest remainder,
     * ties to the earlier row. The remainders share one denominator, so they are compared exactly as
     * {@code BigDecimal} with no intermediate rounding. Rounding each row on its own would let a column
     * add up to 100.1 or 99.9, which the accepted design forbids for the totals it shows.
     *
     * <p>The amounts are not negative and sum to {@code total}, so the missing tenths are fewer than
     * the rows with a remainder and a row with a zero amount never receives one. When the total is
     * zero there is nothing to divide by and every share is {@code null}.
     */
    static List<BigDecimal> apportion(List<BigDecimal> amounts, BigDecimal total) {
        if (total.signum() == 0) {
            List<BigDecimal> unavailable = new ArrayList<>(amounts.size());
            amounts.forEach(amount -> unavailable.add(null));
            return unavailable;
        }
        long[] tenths = new long[amounts.size()];
        BigDecimal[] remainders = new BigDecimal[amounts.size()];
        long assigned = 0;
        for (int i = 0; i < amounts.size(); i++) {
            BigDecimal[] division = amounts.get(i).multiply(TENTHS_OF_A_PERCENT).divideAndRemainder(total);
            tenths[i] = division[0].longValueExact();
            remainders[i] = division[1];
            assigned += tenths[i];
        }
        List<Integer> byRemainder = new ArrayList<>(amounts.size());
        for (int i = 0; i < amounts.size(); i++) {
            byRemainder.add(i);
        }
        // List.sort is stable, so equal remainders keep row order and the earlier row wins the tie.
        byRemainder.sort(Comparator.comparing((Integer i) -> remainders[i]).reversed());
        long missing = TENTHS_OF_A_PERCENT.longValueExact() - assigned;
        for (int k = 0; k < missing; k++) {
            tenths[byRemainder.get(k)]++;
        }

        List<BigDecimal> shares = new ArrayList<>(amounts.size());
        for (long share : tenths) {
            shares.add(BigDecimal.valueOf(share, 1));
        }
        return shares;
    }

    /** {@code part} as a percent of {@code whole} at one decimal, or {@code null} when whole is zero. */
    private static BigDecimal percentOf(BigDecimal part, BigDecimal whole) {
        if (whole.signum() == 0) {
            return null;
        }
        return part.multiply(HUNDRED).divide(whole, 1, RoundingMode.HALF_UP);
    }

    /** Groups in first-seen order; callers sort the groups themselves. */
    private static <K> Map<K, List<Line>> groupBy(List<Line> lines, Function<Line, K> key) {
        Map<K, List<Line>> groups = new LinkedHashMap<>();
        for (Line line : lines) {
            groups.computeIfAbsent(key.apply(line), ignored -> new ArrayList<>()).add(line);
        }
        return groups;
    }
}
