package com.rikkaus.wealth.snapshot.application;

import com.rikkaus.wealth.shared.money.ConversionTrace;
import com.rikkaus.wealth.shared.money.Currency;
import com.rikkaus.wealth.shared.money.ExchangeRate;
import com.rikkaus.wealth.shared.money.Money;
import com.rikkaus.wealth.shared.time.TimeConventions;
import com.rikkaus.wealth.snapshot.application.SnapshotSourceReader.AssetRow;
import com.rikkaus.wealth.snapshot.application.SnapshotSourceReader.LiabilityRow;
import com.rikkaus.wealth.snapshot.application.WealthSnapshot.Aggregate;
import com.rikkaus.wealth.snapshot.application.WealthSnapshot.AssetLine;
import com.rikkaus.wealth.snapshot.application.WealthSnapshot.Due;
import com.rikkaus.wealth.snapshot.application.WealthSnapshot.DueKind;
import com.rikkaus.wealth.snapshot.application.WealthSnapshot.Group;
import com.rikkaus.wealth.snapshot.application.WealthSnapshot.Kpis;
import com.rikkaus.wealth.snapshot.application.WealthSnapshot.LargestAssets;
import com.rikkaus.wealth.snapshot.application.WealthSnapshot.LiabilityLine;
import com.rikkaus.wealth.snapshot.application.WealthSnapshot.SourceDate;
import com.rikkaus.wealth.snapshot.application.WealthSnapshot.SourceType;
import com.rikkaus.wealth.snapshot.application.WealthSnapshot.UpcomingDues;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Collator;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sums the owner's assets and liabilities into the dashboard snapshot.
 *
 * <p>Computed on every read and never stored: recording a valuation, editing a liability or declaring a
 * rate changes the next snapshot with nothing to keep in step. Every figure is a sum over the lines the
 * snapshot returns, which is what makes each total reconcile with them.
 *
 * <p>The clock is read once, for the due-date window. Nothing else depends on today, so two reads on
 * the same day over the same records give the same snapshot.
 */
@Service
public class WealthSnapshotService {

    /** How far ahead a due date counts as upcoming, today included at the start. */
    static final int DUE_WINDOW_DAYS = 30;

    /** Amounts carry the two decimals the catalog stores them with. */
    private static final int AMOUNT_SCALE = 2;

    /** The debt-to-asset ratio is a fraction; six decimals keep a tenth of a percent exact to display. */
    private static final int RATIO_SCALE = 6;

    private static final int LARGEST_ASSET_COUNT = 5;

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(AMOUNT_SCALE);

    /** The levels the liquidity chart lists, in its fixed order. */
    private static final List<String> LIQUIDITY_ORDER = List.of("HIGH", "MEDIUM", "LOW");

    private static final String HIGH_LIQUIDITY = "HIGH";

    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");

    private final SnapshotSourceReader reader;
    private final Clock clock;

    WealthSnapshotService(SnapshotSourceReader reader, Clock clock) {
        this.reader = reader;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public WealthSnapshot snapshotFor(UUID ownerId) {
        LocalDate today = TimeConventions.today(clock, TimeConventions.DEFAULT_DISPLAY_ZONE);
        Optional<ExchangeRate> rate = reader.exchangeRateOf(ownerId);
        // A Collator is not safe to share between threads, so each snapshot has its own.
        Collator collator = Collator.getInstance(VIETNAMESE);

        List<AssetLine> assets =
                reader.assetsOf(ownerId).stream()
                        .map(row -> assetLine(row, rate))
                        .sorted(Comparator.comparing(AssetLine::valueVnd, Comparator.nullsLast(Comparator.<BigDecimal>reverseOrder()))
                                .thenComparing(AssetLine::name, collator)
                                .thenComparing(line -> line.id().toString()))
                        .toList();
        List<LiabilityLine> liabilities =
                reader.liabilitiesOf(ownerId).stream()
                        .map(row -> liabilityLine(row, rate))
                        .sorted(Comparator.comparing(
                                        LiabilityLine::outstandingBalanceVnd,
                                        Comparator.nullsLast(Comparator.<BigDecimal>reverseOrder()))
                                .thenComparing(LiabilityLine::name, collator)
                                .thenComparing(line -> line.id().toString()))
                        .toList();

        Aggregate totalAssets = aggregate(assets, AssetLine::valueVnd);
        Aggregate totalLiabilities = aggregate(liabilities, LiabilityLine::outstandingBalanceVnd);
        Aggregate liquidAssets =
                aggregate(
                        assets.stream().filter(line -> HIGH_LIQUIDITY.equals(line.liquidity())).toList(),
                        AssetLine::valueVnd);

        boolean anyUsdConverted =
                assets.stream().anyMatch(line -> line.currency() == Currency.USD && line.valueVnd() != null)
                        || liabilities.stream()
                                .anyMatch(line -> line.currency() == Currency.USD
                                        && line.outstandingBalanceVnd() != null);
        List<SourceDate> sourceDates = sourceDates(assets, liabilities, anyUsdConverted ? rate : Optional.empty(), collator);
        LocalDate lastDataUpdate =
                sourceDates.stream().map(SourceDate::date).max(Comparator.naturalOrder()).orElse(null);

        boolean complete = totalAssets.excludedRecordCount() == 0 && totalLiabilities.excludedRecordCount() == 0;

        return new WealthSnapshot(
                today,
                rate,
                complete,
                new Kpis(
                        totalAssets,
                        totalLiabilities,
                        netWorth(totalAssets, totalLiabilities),
                        debtToAssetRatio(totalAssets, totalLiabilities),
                        liquidAssets,
                        lastDataUpdate),
                byCategory(assets),
                byLiquidity(assets),
                largest(assets),
                upcomingDues(liabilities, today, rate, collator),
                sourceDates,
                assets,
                liabilities);
    }

    private static AssetLine assetLine(AssetRow row, Optional<ExchangeRate> rate) {
        return new AssetLine(
                row.id(),
                row.name(),
                row.category(),
                row.liquidity(),
                row.value(),
                row.currency(),
                inVnd(row.value(), row.currency(), rate),
                row.valuationDate());
    }

    private static LiabilityLine liabilityLine(LiabilityRow row, Optional<ExchangeRate> rate) {
        return new LiabilityLine(
                row.id(),
                row.name(),
                row.category(),
                row.outstandingBalance(),
                row.currency(),
                inVnd(row.outstandingBalance(), row.currency(), rate),
                row.nextPaymentDate(),
                row.maturityDate(),
                row.recurringPaymentAmount(),
                row.recurringPaymentFrequency(),
                TimeConventions.inZone(row.updatedAt(), TimeConventions.DEFAULT_DISPLAY_ZONE).toLocalDate());
    }

    /**
     * {@code amount} in VND, or {@code null} when it is in USD and no rate is declared, or when there is
     * no amount at all.
     *
     * <p>A VND amount is kept exactly as stored. A USD amount goes through {@link ConversionTrace}, which
     * rounds once to whole đồng, and is then written with the catalog's two decimals — an exact step,
     * since whole đồng has no fraction to lose. Each line is converted on its own, so a total is the sum
     * of the figures its lines show.
     *
     * <p>Never zero for a missing rate: a zero would read as a real amount and understate the owner's
     * wealth. {@code null} is what lets the snapshot say which lines it could not count.
     */
    private static BigDecimal inVnd(BigDecimal amount, Currency currency, Optional<ExchangeRate> rate) {
        if (amount == null) {
            return null;
        }
        return switch (currency) {
            case VND -> amount.setScale(AMOUNT_SCALE);
            case USD ->
                    rate.map(usdToVnd -> ConversionTrace.convert(Money.of(amount, Currency.USD), usdToVnd)
                                    .converted()
                                    .amount()
                                    .setScale(AMOUNT_SCALE))
                            .orElse(null);
        };
    }

    private static <T> Aggregate aggregate(List<T> lines, Function<T, BigDecimal> vnd) {
        BigDecimal known = lines.stream().map(vnd).filter(Objects::nonNull).reduce(ZERO, BigDecimal::add);
        int excluded = (int) lines.stream().map(vnd).filter(Objects::isNull).count();
        return new Aggregate(excluded == 0 ? known : null, known, lines.size(), excluded);
    }

    private static BigDecimal netWorth(Aggregate assets, Aggregate liabilities) {
        if (assets.amountVnd() == null || liabilities.amountVnd() == null) {
            return null;
        }
        return assets.amountVnd().subtract(liabilities.amountVnd());
    }

    /** {@code null} rather than a division by zero, and rather than a ratio of partial totals. */
    private static BigDecimal debtToAssetRatio(Aggregate assets, Aggregate liabilities) {
        if (assets.amountVnd() == null || liabilities.amountVnd() == null || assets.amountVnd().signum() == 0) {
            return null;
        }
        return liabilities.amountVnd().divide(assets.amountVnd(), RATIO_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * One group per category that has an asset, a zero-valued one included; largest known amount first,
     * then by category code so equal amounts keep a fixed order. No "other" group: how many slices the
     * donut shows is the client's decision.
     */
    private static List<Group> byCategory(List<AssetLine> assets) {
        return groupBy(assets, AssetLine::category).entrySet().stream()
                .map(group -> new Group(group.getKey(), aggregate(group.getValue(), AssetLine::valueVnd)))
                .sorted(Comparator.comparing((Group group) -> group.aggregate().knownAmountVnd())
                        .reversed()
                        .thenComparing(Group::key))
                .toList();
    }

    /** One group per liquidity level that has an asset, always HIGH, MEDIUM, LOW. */
    private static List<Group> byLiquidity(List<AssetLine> assets) {
        Map<String, List<AssetLine>> groups = groupBy(assets, AssetLine::liquidity);
        return LIQUIDITY_ORDER.stream()
                .filter(groups::containsKey)
                .map(level -> new Group(level, aggregate(groups.get(level), AssetLine::valueVnd)))
                .toList();
    }

    /**
     * The first five converted lines. {@code assets} is already in ranking order — value, then the
     * Vietnamese name, then id — so the ranking and the list can never disagree. An unconverted line has
     * no value to rank by and is counted instead.
     */
    private static LargestAssets largest(List<AssetLine> assets) {
        List<AssetLine> items =
                assets.stream().filter(line -> line.valueVnd() != null).limit(LARGEST_ASSET_COUNT).toList();
        int excluded = (int) assets.stream().filter(line -> line.valueVnd() == null).count();
        return new LargestAssets(items, excluded);
    }

    /**
     * Every next payment date and maturity date on or before today plus the window. A date already past
     * is kept and marked overdue: an unpaid obligation does not stop being one because its day went by.
     */
    private static UpcomingDues upcomingDues(
            List<LiabilityLine> liabilities, LocalDate today, Optional<ExchangeRate> rate, Collator collator) {
        LocalDate windowEnd = today.plusDays(DUE_WINDOW_DAYS);
        List<Due> items = new ArrayList<>();
        for (LiabilityLine line : liabilities) {
            if (line.nextPaymentDate() != null && !line.nextPaymentDate().isAfter(windowEnd)) {
                items.add(due(line, DueKind.PAYMENT, line.nextPaymentDate(), line.recurringPaymentAmount(), today, rate));
            }
            if (line.maturityDate() != null && !line.maturityDate().isAfter(windowEnd)) {
                items.add(due(line, DueKind.MATURITY, line.maturityDate(), line.outstandingBalance(), today, rate));
            }
        }
        items.sort(Comparator.comparing(Due::dueDate)
                .thenComparing(Due::dueKind)
                .thenComparing(Due::name, collator)
                .thenComparing(item -> item.liabilityId().toString()));
        return new UpcomingDues(DUE_WINDOW_DAYS, windowEnd, List.copyOf(items));
    }

    private static Due due(
            LiabilityLine line,
            DueKind kind,
            LocalDate date,
            BigDecimal amount,
            LocalDate today,
            Optional<ExchangeRate> rate) {
        return new Due(
                line.id(),
                line.name(),
                line.category(),
                kind,
                date,
                date.isBefore(today),
                amount,
                line.currency(),
                inVnd(amount, line.currency(), rate));
    }

    /**
     * The date each source contributes: an asset's valuation date, the day a liability was last saved,
     * and the rate's effective date when the rate converted at least one line. Newest first; on one day,
     * assets before liabilities before the rate, then by Vietnamese name and id.
     */
    private static List<SourceDate> sourceDates(
            List<AssetLine> assets,
            List<LiabilityLine> liabilities,
            Optional<ExchangeRate> usedRate,
            Collator collator) {
        List<SourceDate> dates = new ArrayList<>(assets.size() + liabilities.size() + 1);
        assets.forEach(line -> dates.add(new SourceDate(SourceType.ASSET, line.id(), line.name(), line.valuationDate())));
        liabilities.forEach(
                line -> dates.add(new SourceDate(SourceType.LIABILITY, line.id(), line.name(), line.updatedOn())));
        usedRate.ifPresent(rate -> dates.add(new SourceDate(SourceType.EXCHANGE_RATE, null, null, rate.effectiveDate())));
        dates.sort(Comparator.comparing(SourceDate::date)
                .reversed()
                .thenComparing(SourceDate::sourceType)
                .thenComparing(SourceDate::name, Comparator.nullsFirst(collator))
                .thenComparing(date -> date.sourceId() == null ? "" : date.sourceId().toString()));
        return List.copyOf(dates);
    }

    /** Groups in first-seen order; callers sort the groups themselves. */
    private static <K> Map<K, List<AssetLine>> groupBy(List<AssetLine> lines, Function<AssetLine, K> key) {
        Map<K, List<AssetLine>> groups = new LinkedHashMap<>();
        for (AssetLine line : lines) {
            groups.computeIfAbsent(key.apply(line), ignored -> new ArrayList<>()).add(line);
        }
        return groups;
    }
}
