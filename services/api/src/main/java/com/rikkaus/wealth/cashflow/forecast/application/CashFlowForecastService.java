package com.rikkaus.wealth.cashflow.forecast.application;

import com.rikkaus.wealth.cashflow.application.CashFlowOccurrenceService;
import com.rikkaus.wealth.cashflow.application.OccurrenceWindow;
import com.rikkaus.wealth.cashflow.domain.CashFlowEntry;
import com.rikkaus.wealth.cashflow.domain.CashFlowEntryRepository;
import com.rikkaus.wealth.cashflow.domain.CashFlowEntryType;
import com.rikkaus.wealth.cashflow.domain.CashFlowOccurrence;
import com.rikkaus.wealth.cashflow.domain.CashFlowTiming;
import com.rikkaus.wealth.cashflow.forecast.application.CashFlowForecast.Direction;
import com.rikkaus.wealth.cashflow.forecast.application.CashFlowForecast.Exclusion;
import com.rikkaus.wealth.cashflow.forecast.application.CashFlowForecast.ExclusionReason;
import com.rikkaus.wealth.cashflow.forecast.application.CashFlowForecast.Item;
import com.rikkaus.wealth.cashflow.forecast.application.CashFlowForecast.MinimumBalance;
import com.rikkaus.wealth.cashflow.forecast.application.CashFlowForecast.MinimumBalanceStatus;
import com.rikkaus.wealth.cashflow.forecast.application.CashFlowForecast.Month;
import com.rikkaus.wealth.cashflow.forecast.application.CashFlowForecast.OpeningBalance;
import com.rikkaus.wealth.cashflow.forecast.application.CashFlowForecast.OpeningSource;
import com.rikkaus.wealth.cashflow.forecast.application.CashFlowForecast.SourceType;
import com.rikkaus.wealth.cashflow.forecast.application.ForecastSourceReader.ScheduledLiability;
import com.rikkaus.wealth.cashflow.forecast.application.ForecastSourceReader.ValuedAsset;
import com.rikkaus.wealth.cashflow.forecast.application.LiabilityPaymentSchedule.Payment;
import com.rikkaus.wealth.shared.money.ConversionTrace;
import com.rikkaus.wealth.shared.money.Currency;
import com.rikkaus.wealth.shared.money.ExchangeRate;
import com.rikkaus.wealth.shared.money.Money;
import com.rikkaus.wealth.shared.time.TimeConventions;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Projects the owner's cash over the next three months from the records they can see.
 *
 * <p>Three kinds of record contribute, and nothing else: the occurrences of their cash-flow entries,
 * one-time and recurring, as {@link CashFlowOccurrenceService} expands them; the payments of their
 * liabilities, from each stated schedule; and the value of their cash and bank accounts, as the opening
 * balance. No growth, inflation, average or guess is added. A record that could have contributed and
 * does not is listed with its reason, so no obligation leaves the forecast silently.
 *
 * <p>Computed on every read and never stored, like the occurrences it is built from. Editing or
 * deleting a source changes the forecast on the next request.
 *
 * <p>The owner is the only user whose records are read, and the caller takes that value from {@code
 * CurrentUser}. Every query below filters on it.
 */
@Service
public class CashFlowForecastService {

    /** Same day, then entries before liabilities, then identifier as text: all fixed by the data. */
    private static final Comparator<Item> ITEM_ORDER =
            Comparator.comparing(Item::date)
                    .thenComparing(Item::sourceType)
                    .thenComparing(item -> item.sourceId().toString());

    private static final Comparator<Exclusion> EXCLUSION_ORDER =
            Comparator.comparing(Exclusion::sourceType)
                    .thenComparing(exclusion -> exclusion.sourceId().toString());

    /** The opening balance categories, in the order the response lists them. */
    public static final List<String> OPENING_BALANCE_CATEGORIES = List.of("BANK_ACCOUNT", "CASH");

    private final CashFlowOccurrenceService occurrences;
    private final CashFlowEntryRepository entries;
    private final ForecastSourceReader sources;
    private final Clock clock;

    CashFlowForecastService(
            CashFlowOccurrenceService occurrences,
            CashFlowEntryRepository entries,
            ForecastSourceReader sources,
            Clock clock) {
        this.occurrences = occurrences;
        this.entries = entries;
        this.sources = sources;
        this.clock = clock;
    }

    /**
     * The owner's forecast for the three months starting at {@code fromText}, or at next month when it
     * is absent.
     *
     * <p>Read-only and in one transaction, so the entries, liabilities, accounts and rate are read from
     * the same snapshot and a concurrent edit cannot leave the totals out of step with the items.
     */
    @Transactional(readOnly = true)
    public CashFlowForecast forecastFor(UUID ownerId, String fromText) {
        LocalDate today = TimeConventions.today(clock, TimeConventions.DEFAULT_DISPLAY_ZONE);
        ForecastHorizon horizon = ForecastHorizon.of(fromText, today);
        Optional<ExchangeRate> rate = sources.exchangeRateOf(ownerId);

        Accumulator acc = new Accumulator(rate);
        addEntries(ownerId, horizon, acc);
        addLiabilities(ownerId, horizon, acc);
        OpeningBalance opening = openingBalance(ownerId, horizon, acc);

        acc.items.sort(ITEM_ORDER);
        acc.exclusions.sort(EXCLUSION_ORDER);
        return project(horizon, opening, acc, rate.orElse(null));
    }

    private void addEntries(UUID ownerId, ForecastHorizon horizon, Accumulator acc) {
        List<CashFlowOccurrence> inHorizon =
                occurrences.occurrencesFor(ownerId, new OccurrenceWindow(horizon.from(), horizon.to()));

        Set<UUID> withOccurrence = new TreeSet<>();
        Map<UUID, Set<YearMonth>> unconverted = new TreeMap<>();
        for (CashFlowOccurrence occurrence : inHorizon) {
            CashFlowEntry entry = occurrence.entry();
            withOccurrence.add(entry.getId());
            Optional<Money> vnd = acc.toVnd(entry.getAmount(), entry.getCurrency());
            if (vnd.isEmpty()) {
                unconverted
                        .computeIfAbsent(entry.getId(), id -> new TreeSet<>())
                        .add(YearMonth.from(occurrence.date()));
                continue;
            }
            acc.items.add(new Item(
                    occurrence.date(),
                    entry.getEntryType() == CashFlowEntryType.INCOME ? Direction.INCOME : Direction.EXPENSE,
                    SourceType.CASH_FLOW_ENTRY,
                    entry.getId(),
                    entry.getCategory().name(),
                    entry.getAmount(),
                    entry.getCurrency(),
                    vnd.get().amount(),
                    entry.getRecurrence() == null ? null : entry.getRecurrence().name(),
                    occurrence.sequence(),
                    occurrence.shiftedToMonthEnd()));
        }
        unconverted.forEach((id, months) ->
                acc.exclude(SourceType.CASH_FLOW_ENTRY, id, ExclusionReason.EXCHANGE_RATE_MISSING, months));

        // The same coarse filter the occurrence service starts from, read again for the recurring
        // entries that overlap the horizon: those with no date in it are reported, and those linked to
        // a liability decide whether that liability's own schedule would count it a second time.
        for (CashFlowEntry entry : entries.findActiveBetween(ownerId, horizon.from(), horizon.to())) {
            if (entry.getTiming() != CashFlowTiming.RECURRING) {
                continue;
            }
            if (!withOccurrence.contains(entry.getId())) {
                acc.exclude(
                        SourceType.CASH_FLOW_ENTRY,
                        entry.getId(),
                        ExclusionReason.NO_OCCURRENCE_IN_HORIZON,
                        Set.of());
            }
            if (entry.getLinkedLiabilityId() != null) {
                acc.linkedEntries
                        .computeIfAbsent(entry.getLinkedLiabilityId(), id -> new TreeSet<>(Comparator.comparing(UUID::toString)))
                        .add(entry.getId());
            }
        }
    }

    private void addLiabilities(UUID ownerId, ForecastHorizon horizon, Accumulator acc) {
        for (ScheduledLiability liability : sources.liabilitiesOf(ownerId)) {
            Set<UUID> linked = acc.linkedEntries.get(liability.id());
            if (linked != null) {
                acc.exclusions.add(new Exclusion(
                        SourceType.LIABILITY,
                        liability.id(),
                        ExclusionReason.COVERED_BY_LINKED_ENTRIES,
                        List.of(),
                        List.copyOf(linked)));
                continue;
            }
            if (liability.paymentAmount() == null || liability.nextPaymentDate() == null) {
                // A liability that falls due inside the horizon without a schedule is an obligation the
                // forecast knows exists and cannot price, so the month it falls due in is partial.
                Set<YearMonth> affected = liability.maturityDate() != null && horizon.contains(liability.maturityDate())
                        ? Set.of(YearMonth.from(liability.maturityDate()))
                        : Set.of();
                acc.exclude(
                        SourceType.LIABILITY,
                        liability.id(),
                        liability.paymentAmount() == null
                                ? ExclusionReason.NO_PAYMENT_SCHEDULE
                                : ExclusionReason.NO_NEXT_PAYMENT_DATE,
                        affected);
                continue;
            }

            List<Payment> payments = LiabilityPaymentSchedule.paymentsOf(
                    liability.nextPaymentDate(),
                    LiabilityPaymentSchedule.monthsBetweenPayments(liability.frequency()),
                    liability.maturityDate(),
                    horizon.from(),
                    horizon.to());
            if (payments.isEmpty()) {
                acc.exclude(SourceType.LIABILITY, liability.id(), ExclusionReason.NO_PAYMENT_IN_HORIZON, Set.of());
                continue;
            }
            Optional<Money> vnd = acc.toVnd(liability.paymentAmount(), liability.currency());
            if (vnd.isEmpty()) {
                Set<YearMonth> affected = new TreeSet<>();
                payments.forEach(payment -> affected.add(YearMonth.from(payment.date())));
                acc.exclude(SourceType.LIABILITY, liability.id(), ExclusionReason.EXCHANGE_RATE_MISSING, affected);
                continue;
            }
            for (Payment payment : payments) {
                acc.items.add(new Item(
                        payment.date(),
                        Direction.EXPENSE,
                        SourceType.LIABILITY,
                        liability.id(),
                        liability.category(),
                        liability.paymentAmount(),
                        liability.currency(),
                        vnd.get().amount(),
                        liability.frequency(),
                        payment.sequence(),
                        payment.shiftedToMonthEnd()));
            }
        }
    }

    private OpeningBalance openingBalance(UUID ownerId, ForecastHorizon horizon, Accumulator acc) {
        List<OpeningSource> included = new ArrayList<>();
        Money total = Money.zero(Currency.VND);
        for (ValuedAsset account : sources.cashAccountsOf(ownerId)) {
            Optional<Money> vnd = acc.toVnd(account.value(), account.currency());
            if (vnd.isEmpty()) {
                acc.exclude(SourceType.ASSET, account.id(), ExclusionReason.EXCHANGE_RATE_MISSING, Set.of());
                acc.openingIncomplete = true;
                continue;
            }
            total = total.add(vnd.get());
            included.add(new OpeningSource(
                    account.id(),
                    account.category(),
                    account.valuationDate(),
                    account.value(),
                    account.currency(),
                    vnd.get().amount()));
        }
        included.sort(Comparator.comparing(source -> source.sourceId().toString()));
        return new OpeningBalance(total.amount(), horizon.from(), List.copyOf(included));
    }

    /**
     * Walks the horizon one landing date at a time.
     *
     * <p>The lowest position is judged on the opening balance and on the balance at the end of every
     * day an amount lands, never on month-end balances alone: a salary on the 5th can hide a shortfall
     * on the 3rd that a month-end figure would never show. Amounts landing on the same day are netted,
     * because the forecast has no time of day to order them by. On a tie the earliest date is kept.
     */
    private CashFlowForecast project(
            ForecastHorizon horizon, OpeningBalance opening, Accumulator acc, ExchangeRate rate) {
        Money balance = Money.of(opening.amount(), Currency.VND);
        Money lowest = balance;
        LocalDate lowestDate = horizon.from();
        Money totalExpense = Money.zero(Currency.VND);

        List<Month> months = new ArrayList<>();
        for (YearMonth month : horizon.months()) {
            List<Item> monthItems = acc.items.stream()
                    .filter(item -> YearMonth.from(item.date()).equals(month))
                    .toList();
            Money income = Money.zero(Currency.VND);
            Money expense = Money.zero(Currency.VND);
            TreeMap<LocalDate, Money> netByDay = new TreeMap<>();
            for (Item item : monthItems) {
                Money amount = Money.of(item.amountVnd(), Currency.VND);
                Money signed = item.direction() == Direction.INCOME ? amount : amount.negate();
                if (item.direction() == Direction.INCOME) {
                    income = income.add(amount);
                } else {
                    expense = expense.add(amount);
                }
                netByDay.merge(item.date(), signed, Money::add);
            }

            Money monthOpening = balance;
            Money monthLowest = balance;
            LocalDate monthLowestDate = month.atDay(1);
            for (Map.Entry<LocalDate, Money> day : netByDay.entrySet()) {
                balance = balance.add(day.getValue());
                if (balance.compareTo(monthLowest) < 0) {
                    monthLowest = balance;
                    monthLowestDate = day.getKey();
                }
            }
            if (monthLowest.compareTo(lowest) < 0) {
                lowest = monthLowest;
                lowestDate = monthLowestDate;
            }
            totalExpense = totalExpense.add(expense);
            months.add(new Month(
                    month,
                    income.amount(),
                    expense.amount(),
                    income.subtract(expense).amount(),
                    monthOpening.amount(),
                    balance.amount(),
                    monthLowest.amount(),
                    monthLowestDate,
                    acc.partialMonths.contains(month),
                    monthItems));
        }

        BigDecimal threshold = totalExpense
                .amount()
                .divide(BigDecimal.valueOf(ForecastHorizon.LENGTH), 2, RoundingMode.HALF_UP);
        MinimumBalance minimum = new MinimumBalance(
                lowestDate,
                YearMonth.from(lowestDate),
                lowest.amount(),
                lowest.isNegative(),
                threshold,
                statusOf(opening, lowest, threshold));

        return new CashFlowForecast(
                horizon,
                opening,
                List.copyOf(months),
                minimum,
                List.copyOf(acc.exclusions),
                !acc.partialMonths.isEmpty() || acc.openingIncomplete,
                rate);
    }

    /**
     * No cash or bank account counted means the projection starts from an unknown amount, so no reading
     * of it is offered. Otherwise below zero, below one month of projected expense, or neither.
     */
    private static MinimumBalanceStatus statusOf(OpeningBalance opening, Money lowest, BigDecimal threshold) {
        if (opening.sources().isEmpty()) {
            return MinimumBalanceStatus.INSUFFICIENT_DATA;
        }
        if (lowest.isNegative()) {
            return MinimumBalanceStatus.NEGATIVE;
        }
        if (lowest.amount().compareTo(threshold) < 0) {
            return MinimumBalanceStatus.THIN;
        }
        return MinimumBalanceStatus.ADEQUATE;
    }

    /** What one request collects before the months are walked. */
    private static final class Accumulator {

        private final Optional<ExchangeRate> rate;
        private final List<Item> items = new ArrayList<>();
        private final List<Exclusion> exclusions = new ArrayList<>();
        private final Set<YearMonth> partialMonths = new TreeSet<>();
        private final Map<UUID, Set<UUID>> linkedEntries = new TreeMap<>();
        private boolean openingIncomplete;

        private Accumulator(Optional<ExchangeRate> rate) {
            this.rate = rate;
        }

        /**
         * The amount in VND, rounded to whole đồng as {@code Currency.VND} declares, or empty when it is
         * in USD and the owner has declared no rate. Rounded here, per source amount, so the totals are
         * sums of the very numbers the items show.
         */
        private Optional<Money> toVnd(BigDecimal amount, Currency currency) {
            if (currency == Currency.VND) {
                return Optional.of(Money.of(amount, Currency.VND));
            }
            return rate.map(usdToVnd -> ConversionTrace.convert(Money.of(amount, currency), usdToVnd).converted());
        }

        /** Records an exclusion; the months it would have contributed to become partial. */
        private void exclude(SourceType type, UUID id, ExclusionReason reason, Set<YearMonth> affectedMonths) {
            List<YearMonth> months = List.copyOf(new TreeSet<>(affectedMonths));
            partialMonths.addAll(months);
            exclusions.add(new Exclusion(type, id, reason, months, List.of()));
        }
    }
}
