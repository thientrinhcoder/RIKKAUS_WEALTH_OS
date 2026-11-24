package com.rikkaus.wealth.snapshot.insights.application;

import static java.time.temporal.ChronoUnit.DAYS;

import com.rikkaus.wealth.shared.money.Currency;
import com.rikkaus.wealth.shared.money.ExchangeRate;
import com.rikkaus.wealth.snapshot.application.WealthSnapshot;
import com.rikkaus.wealth.snapshot.application.WealthSnapshot.Aggregate;
import com.rikkaus.wealth.snapshot.application.WealthSnapshot.AssetLine;
import com.rikkaus.wealth.snapshot.application.WealthSnapshot.Due;
import com.rikkaus.wealth.snapshot.application.WealthSnapshot.DueKind;
import com.rikkaus.wealth.snapshot.application.WealthSnapshot.Group;
import com.rikkaus.wealth.snapshot.application.WealthSnapshot.LiabilityLine;
import com.rikkaus.wealth.snapshot.application.WealthSnapshot.SourceType;
import com.rikkaus.wealth.snapshot.application.WealthSnapshotService;
import com.rikkaus.wealth.snapshot.insights.application.SnapshotInsights.Comparison;
import com.rikkaus.wealth.snapshot.insights.application.SnapshotInsights.DateKind;
import com.rikkaus.wealth.snapshot.insights.application.SnapshotInsights.Evidence;
import com.rikkaus.wealth.snapshot.insights.application.SnapshotInsights.Insight;
import com.rikkaus.wealth.snapshot.insights.application.SnapshotInsights.Observed;
import com.rikkaus.wealth.snapshot.insights.application.SnapshotInsights.RuleId;
import com.rikkaus.wealth.snapshot.insights.application.SnapshotInsights.Status;
import com.rikkaus.wealth.snapshot.insights.application.SnapshotInsights.Threshold;
import com.rikkaus.wealth.snapshot.insights.application.SnapshotInsights.Thresholds;
import com.rikkaus.wealth.snapshot.insights.application.SnapshotInsights.UnavailableReason;
import com.rikkaus.wealth.snapshot.insights.application.SnapshotInsights.Unit;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Collator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Judges the six insight rules over the owner's wealth snapshot.
 *
 * <p>Every figure is read from {@link WealthSnapshotService}: the totals, the USD conversions and the due
 * dates are the snapshot's own, so an insight always agrees with the dashboard it sits on. Nothing is
 * stored; the same records on the same day under the same thresholds give the same result.
 *
 * <p>A share is rounded half up to six decimals, like the snapshot's debt-to-asset ratio, and the status
 * compares that rounded value with the threshold, so a client can re-derive every status from the
 * response it was given.
 */
@Service
public class SnapshotInsightService {

    private static final String LOW_LIQUIDITY = "LOW";

    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");

    private final WealthSnapshotService snapshots;
    private final Thresholds thresholds;

    SnapshotInsightService(WealthSnapshotService snapshots, SnapshotInsightProperties properties) {
        this.snapshots = snapshots;
        this.thresholds =
                new Thresholds(
                        fraction(properties.maxCategoryShare()),
                        fraction(properties.maxDebtToAssetRatio()),
                        fraction(properties.maxLowLiquidityShare()),
                        properties.dueWindowDays(),
                        properties.maxValuationAgeDays(),
                        properties.maxExchangeRateAgeDays());
    }

    @Transactional(readOnly = true)
    public SnapshotInsights insightsFor(UUID ownerId) {
        WealthSnapshot snapshot = snapshots.snapshotFor(ownerId);
        List<Due> allDues = snapshots.duesUntil(snapshot, LocalDate.MAX);
        return new SnapshotInsights(
                snapshot.asOfDate(),
                snapshot.complete(),
                thresholds,
                List.of(
                        categoryConcentration(snapshot),
                        debtToAssetRatio(snapshot),
                        lowLiquidityShare(snapshot),
                        upcomingLiabilityDue(snapshot, allDues),
                        staleValuation(snapshot),
                        staleExchangeRate(snapshot)));
    }

    /** The largest category's share of total assets; the evidence is that category's assets. */
    private Insight categoryConcentration(WealthSnapshot snapshot) {
        Threshold threshold = new Threshold(Comparison.GREATER_THAN, thresholds.maxCategoryShare(), Unit.FRACTION);
        Optional<Insight> unavailable = shareUnavailable(RuleId.CATEGORY_CONCENTRATION, threshold, snapshot);
        if (unavailable.isPresent()) {
            return unavailable.get();
        }
        // Totals are complete here, so every category has an amount; the first is the largest.
        Group largest = snapshot.assetsByCategory().getFirst();
        BigDecimal total = snapshot.kpis().totalAssets().amountVnd();
        BigDecimal share = share(largest.aggregate().amountVnd(), total);
        return judged(
                RuleId.CATEGORY_CONCENTRATION,
                share.compareTo(threshold.value()) > 0,
                new Observed(share, Unit.FRACTION, largest.key(), largest.aggregate().amountVnd(), total, null),
                threshold,
                assetEvidence(snapshot, snapshot.assets().stream()
                        .filter(line -> line.category().equals(largest.key()))));
    }

    /** Total liabilities over total assets, as the snapshot computes it; the evidence is every liability. */
    private Insight debtToAssetRatio(WealthSnapshot snapshot) {
        Threshold threshold =
                new Threshold(Comparison.GREATER_THAN, thresholds.maxDebtToAssetRatio(), Unit.FRACTION);
        RuleId rule = RuleId.DEBT_TO_ASSET_RATIO;
        Aggregate assets = snapshot.kpis().totalAssets();
        Aggregate liabilities = snapshot.kpis().totalLiabilities();
        if (snapshot.assets().isEmpty()) {
            return unavailable(rule, threshold, UnavailableReason.NO_ASSETS, List.of());
        }
        if (assets.amountVnd() == null || liabilities.amountVnd() == null) {
            return unavailable(
                    rule,
                    threshold,
                    UnavailableReason.MISSING_EXCHANGE_RATE,
                    Stream.concat(
                                    assetEvidence(snapshot, snapshot.assets().stream()
                                                    .filter(line -> line.valueVnd() == null))
                                            .stream(),
                                    liabilityEvidence(snapshot, snapshot.liabilities().stream()
                                                    .filter(line -> line.outstandingBalanceVnd() == null))
                                            .stream())
                            .toList());
        }
        if (assets.amountVnd().signum() == 0) {
            return unavailable(
                    rule, threshold, UnavailableReason.ZERO_TOTAL_ASSETS, assetEvidence(snapshot, snapshot.assets().stream()));
        }
        BigDecimal ratio = snapshot.kpis().debtToAssetRatio();
        return judged(
                rule,
                ratio.compareTo(threshold.value()) > 0,
                new Observed(ratio, Unit.FRACTION, null, liabilities.amountVnd(), assets.amountVnd(), null),
                threshold,
                liabilityEvidence(snapshot, snapshot.liabilities().stream()));
    }

    /** The share of total assets marked {@code LOW}; the evidence is those assets, possibly none. */
    private Insight lowLiquidityShare(WealthSnapshot snapshot) {
        Threshold threshold =
                new Threshold(Comparison.GREATER_THAN, thresholds.maxLowLiquidityShare(), Unit.FRACTION);
        Optional<Insight> unavailable = shareUnavailable(RuleId.LOW_LIQUIDITY_SHARE, threshold, snapshot);
        if (unavailable.isPresent()) {
            return unavailable.get();
        }
        BigDecimal total = snapshot.kpis().totalAssets().amountVnd();
        // No LOW asset is a real zero: the total is known and none of it is of low liquidity.
        BigDecimal low = snapshot.assetsByLiquidity().stream()
                .filter(group -> LOW_LIQUIDITY.equals(group.key()))
                .map(group -> group.aggregate().amountVnd())
                .findFirst()
                .orElse(BigDecimal.ZERO.setScale(total.scale()));
        BigDecimal share = share(low, total);
        return judged(
                RuleId.LOW_LIQUIDITY_SHARE,
                share.compareTo(threshold.value()) > 0,
                new Observed(share, Unit.FRACTION, null, low, total, null),
                threshold,
                assetEvidence(snapshot, snapshot.assets().stream()
                        .filter(line -> LOW_LIQUIDITY.equals(line.liquidity()))));
    }

    /**
     * Days from today to the nearest payment or maturity date, negative when it is overdue. Within the
     * window the evidence is every date in the window, the snapshot's upcoming list when the window is
     * the snapshot's 30 days; beyond it, the nearest date alone.
     */
    private Insight upcomingLiabilityDue(WealthSnapshot snapshot, List<Due> allDues) {
        Threshold threshold =
                new Threshold(Comparison.AT_MOST, BigDecimal.valueOf(thresholds.dueWindowDays()), Unit.DAYS);
        RuleId rule = RuleId.UPCOMING_LIABILITY_DUE;
        if (snapshot.liabilities().isEmpty()) {
            return unavailable(rule, threshold, UnavailableReason.NO_LIABILITIES, List.of());
        }
        if (allDues.isEmpty()) {
            return unavailable(rule, threshold, UnavailableReason.NO_DUE_DATES, List.of());
        }
        LocalDate asOf = snapshot.asOfDate();
        Due nearest = allDues.getFirst();
        long daysUntil = DAYS.between(asOf, nearest.dueDate());
        long window = thresholds.dueWindowDays();
        boolean attention = daysUntil <= window;
        LocalDate windowEnd = asOf.plusDays(thresholds.dueWindowDays());
        List<Evidence> evidence = (attention
                        ? allDues.stream().filter(due -> !due.dueDate().isAfter(windowEnd))
                        : Stream.of(nearest))
                .map(due -> dueEvidence(due, asOf))
                .toList();
        return judged(
                rule,
                attention,
                new Observed(BigDecimal.valueOf(daysUntil), Unit.DAYS, null, null, null, nearest.dueDate()),
                threshold,
                evidence);
    }

    /** The age of the oldest asset valuation; the evidence is every stale asset, or the oldest one. */
    private Insight staleValuation(WealthSnapshot snapshot) {
        Threshold threshold =
                new Threshold(Comparison.GREATER_THAN, BigDecimal.valueOf(thresholds.maxValuationAgeDays()), Unit.DAYS);
        if (snapshot.assets().isEmpty()) {
            return unavailable(RuleId.STALE_VALUATION, threshold, UnavailableReason.NO_ASSETS, List.of());
        }
        LocalDate asOf = snapshot.asOfDate();
        Collator collator = Collator.getInstance(VIETNAMESE);
        List<AssetLine> oldestFirst = snapshot.assets().stream()
                .sorted(Comparator.comparing(AssetLine::valuationDate)
                        .thenComparing(AssetLine::name, collator)
                        .thenComparing(line -> line.id().toString()))
                .toList();
        AssetLine oldest = oldestFirst.getFirst();
        long age = DAYS.between(oldest.valuationDate(), asOf);
        long maxAge = thresholds.maxValuationAgeDays();
        boolean attention = age > maxAge;
        Stream<AssetLine> evidence = attention
                ? oldestFirst.stream().filter(line -> DAYS.between(line.valuationDate(), asOf) > maxAge)
                : Stream.of(oldest);
        return judged(
                RuleId.STALE_VALUATION,
                attention,
                new Observed(BigDecimal.valueOf(age), Unit.DAYS, null, null, null, oldest.valuationDate()),
                threshold,
                assetEvidence(snapshot, evidence));
    }

    /**
     * The age of the declared rate's effective date; the evidence is the rate and every USD record it
     * converts. Without a rate the evidence is the USD records waiting for one.
     */
    private Insight staleExchangeRate(WealthSnapshot snapshot) {
        Threshold threshold = new Threshold(
                Comparison.GREATER_THAN, BigDecimal.valueOf(thresholds.maxExchangeRateAgeDays()), Unit.DAYS);
        List<Evidence> usdRecords = Stream.concat(
                        assetEvidence(snapshot, snapshot.assets().stream()
                                        .filter(line -> line.currency() == Currency.USD))
                                .stream(),
                        liabilityEvidence(snapshot, snapshot.liabilities().stream()
                                        .filter(line -> line.currency() == Currency.USD))
                                .stream())
                .toList();
        if (snapshot.exchangeRate().isEmpty()) {
            return unavailable(
                    RuleId.STALE_EXCHANGE_RATE, threshold, UnavailableReason.MISSING_EXCHANGE_RATE, usdRecords);
        }
        ExchangeRate rate = snapshot.exchangeRate().get();
        long age = DAYS.between(rate.effectiveDate(), snapshot.asOfDate());
        long maxAge = thresholds.maxExchangeRateAgeDays();
        List<Evidence> evidence = new ArrayList<>(usdRecords.size() + 1);
        evidence.add(new Evidence(
                SourceType.EXCHANGE_RATE,
                null,
                null,
                null,
                null,
                DateKind.RATE_EFFECTIVE_DATE,
                rate.effectiveDate(),
                Math.toIntExact(age),
                null,
                null,
                null,
                null,
                null,
                rate.rate()));
        evidence.addAll(usdRecords);
        return judged(
                RuleId.STALE_EXCHANGE_RATE,
                age > maxAge,
                new Observed(BigDecimal.valueOf(age), Unit.DAYS, null, null, null, rate.effectiveDate()),
                threshold,
                List.copyOf(evidence));
    }

    /**
     * Why a share of total assets cannot be computed, if it cannot: no asset, an unconverted USD asset
     * (the evidence names each), or a total of zero (the evidence is the zero-valued assets).
     */
    private static Optional<Insight> shareUnavailable(RuleId rule, Threshold threshold, WealthSnapshot snapshot) {
        BigDecimal total = snapshot.kpis().totalAssets().amountVnd();
        if (snapshot.assets().isEmpty()) {
            return Optional.of(unavailable(rule, threshold, UnavailableReason.NO_ASSETS, List.of()));
        }
        if (total == null) {
            return Optional.of(unavailable(
                    rule,
                    threshold,
                    UnavailableReason.MISSING_EXCHANGE_RATE,
                    assetEvidence(snapshot, snapshot.assets().stream().filter(line -> line.valueVnd() == null))));
        }
        if (total.signum() == 0) {
            return Optional.of(unavailable(
                    rule, threshold, UnavailableReason.ZERO_TOTAL_ASSETS, assetEvidence(snapshot, snapshot.assets().stream())));
        }
        return Optional.empty();
    }

    private static Insight judged(
            RuleId rule, boolean attention, Observed observed, Threshold threshold, List<Evidence> evidence) {
        return new Insight(rule, attention ? Status.ATTENTION : Status.OK, observed, threshold, evidence, null);
    }

    private static Insight unavailable(
            RuleId rule, Threshold threshold, UnavailableReason reason, List<Evidence> evidence) {
        return new Insight(rule, Status.UNAVAILABLE, null, threshold, evidence, reason);
    }

    /** Assets keep the order the stream gives them: the snapshot's order unless the rule sorted them. */
    private static List<Evidence> assetEvidence(WealthSnapshot snapshot, Stream<AssetLine> lines) {
        LocalDate asOf = snapshot.asOfDate();
        return lines.map(line -> new Evidence(
                        SourceType.ASSET,
                        line.id(),
                        line.name(),
                        line.category(),
                        line.liquidity(),
                        DateKind.VALUATION_DATE,
                        line.valuationDate(),
                        Math.toIntExact(DAYS.between(line.valuationDate(), asOf)),
                        null,
                        null,
                        line.value(),
                        line.currency(),
                        line.valueVnd(),
                        null))
                .toList();
    }

    private static List<Evidence> liabilityEvidence(WealthSnapshot snapshot, Stream<LiabilityLine> lines) {
        LocalDate asOf = snapshot.asOfDate();
        return lines.map(line -> new Evidence(
                        SourceType.LIABILITY,
                        line.id(),
                        line.name(),
                        line.category(),
                        null,
                        DateKind.UPDATED_ON,
                        line.updatedOn(),
                        Math.toIntExact(DAYS.between(line.updatedOn(), asOf)),
                        null,
                        null,
                        line.outstandingBalance(),
                        line.currency(),
                        line.outstandingBalanceVnd(),
                        null))
                .toList();
    }

    private static Evidence dueEvidence(Due due, LocalDate asOf) {
        return new Evidence(
                SourceType.LIABILITY,
                due.liabilityId(),
                due.name(),
                due.category(),
                null,
                due.dueKind() == DueKind.PAYMENT ? DateKind.NEXT_PAYMENT_DATE : DateKind.MATURITY_DATE,
                due.dueDate(),
                null,
                Math.toIntExact(DAYS.between(asOf, due.dueDate())),
                due.overdue(),
                due.amount(),
                due.currency(),
                due.amountVnd(),
                null);
    }

    private static BigDecimal share(BigDecimal part, BigDecimal total) {
        return Objects.requireNonNull(part).divide(total, SnapshotInsightProperties.FRACTION_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal fraction(BigDecimal value) {
        return value.setScale(SnapshotInsightProperties.FRACTION_SCALE, RoundingMode.UNNECESSARY);
    }
}
