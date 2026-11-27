package com.rikkaus.wealth.cashflow.health.application;

import com.rikkaus.wealth.cashflow.domain.CashFlowCategory;
import com.rikkaus.wealth.cashflow.domain.CashFlowEntry;
import com.rikkaus.wealth.cashflow.domain.CashFlowEntryRepository;
import com.rikkaus.wealth.cashflow.domain.CashFlowEntryType;
import com.rikkaus.wealth.cashflow.domain.CashFlowOccurrence;
import com.rikkaus.wealth.cashflow.domain.CashFlowTiming;
import com.rikkaus.wealth.cashflow.forecast.application.CashFlowForecast;
import com.rikkaus.wealth.cashflow.forecast.application.CashFlowForecast.Direction;
import com.rikkaus.wealth.cashflow.forecast.application.CashFlowForecast.Exclusion;
import com.rikkaus.wealth.cashflow.forecast.application.CashFlowForecast.ExclusionReason;
import com.rikkaus.wealth.cashflow.forecast.application.CashFlowForecast.Item;
import com.rikkaus.wealth.cashflow.forecast.application.CashFlowForecast.MinimumBalance;
import com.rikkaus.wealth.cashflow.forecast.application.CashFlowForecast.OpeningSource;
import com.rikkaus.wealth.cashflow.forecast.application.CashFlowForecastService;
import com.rikkaus.wealth.cashflow.forecast.application.ForecastSourceReader;
import com.rikkaus.wealth.cashflow.forecast.application.ForecastSourceReader.ScheduledLiability;
import com.rikkaus.wealth.cashflow.forecast.application.ForecastSourceReader.ValuedAsset;
import com.rikkaus.wealth.cashflow.forecast.application.LiabilityPaymentSchedule;
import com.rikkaus.wealth.cashflow.forecast.application.LiabilityPaymentSchedule.Payment;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.Assumptions;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.Availability;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.Band;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.BandNotDeterminedReason;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.Calculation;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.Comparison;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.Formula;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.Indicator;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.IndicatorId;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.NotEvaluatedReason;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.Observed;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.Reason;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.Role;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.Rule;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.RuleId;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.Source;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.SourceType;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.Status;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.Threshold;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.Thresholds;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.Unit;
import com.rikkaus.wealth.cashflow.reporting.application.MonthlyCashFlowReport;
import com.rikkaus.wealth.cashflow.reporting.application.MonthlyCashFlowReport.CategoryShare;
import com.rikkaus.wealth.cashflow.reporting.application.MonthlyCashFlowReport.Line;
import com.rikkaus.wealth.cashflow.reporting.application.MonthlyCashFlowReportService;
import com.rikkaus.wealth.cashflow.reporting.application.ReportMonth;
import com.rikkaus.wealth.shared.error.ApiException;
import com.rikkaus.wealth.shared.error.ProblemType;
import com.rikkaus.wealth.shared.money.ConversionTrace;
import com.rikkaus.wealth.shared.money.Currency;
import com.rikkaus.wealth.shared.money.ExchangeRate;
import com.rikkaus.wealth.shared.money.Money;
import com.rikkaus.wealth.shared.time.TimeConventions;
import com.rikkaus.wealth.shared.validation.EnteredDates;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Computes the five financial-health indicators and judges the six insight rules for one owner and one
 * month.
 *
 * <p>Every cash-flow figure is the monthly report's own ({@link MonthlyCashFlowReportService}) and the
 * minimum balance is the forecast's own ({@link CashFlowForecastService}), so an indicator always agrees
 * with the report and the forecast the user can open beside it. Assets and liabilities are read through
 * {@link ForecastSourceReader}, the native queries this slice already uses, because the slice rule
 * forbids reaching into {@code assets}, {@code liabilities} or {@code snapshot}. Nothing is stored; the
 * same records on the same day under the same configuration give the same result.
 *
 * <p>USD amounts are converted exactly as the report converts them: one record at a time with the
 * declared rate, rounded half up to whole đồng. A USD record with no declared rate is left out of every
 * sum and listed with {@code amountVnd} null, and the indicator it would have entered is {@code PARTIAL}
 * — or {@code UNAVAILABLE} when nothing else is left to compute from. A rule never judges a partial or
 * unavailable value: it is {@code NOT_EVALUATED}, because an {@code OK} on incomplete data would be a
 * false reassurance.
 *
 * <p>Shares are rounded half up to six decimals and the reserve to one decimal, and every band and
 * status compares that rounded value, so a client can re-derive each one from the response alone.
 */
@Service
public class FinancialHealthService {

    /** The liquid group of design #81: cash, bank accounts and term deposits, by category. */
    public static final List<String> LIQUID_ASSET_CATEGORIES = List.of("CASH", "BANK_ACCOUNT", "TERM_DEPOSIT");

    /** The money payments are made from, as the forecast's opening balance counts it. */
    public static final List<String> CASH_ACCOUNT_CATEGORIES = List.of("CASH", "BANK_ACCOUNT");

    /**
     * The only categories counted as debt payments. A {@code HOUSING} entry linked to the same loan is not
     * added: design #81's rule against counting one payment twice.
     */
    public static final List<CashFlowCategory> DEBT_PAYMENT_CATEGORIES =
            List.of(CashFlowCategory.DEBT_PRINCIPAL_PAYMENT, CashFlowCategory.INTEREST_PAYMENT);

    private static final int AMOUNT_SCALE = 2;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(AMOUNT_SCALE);

    private final MonthlyCashFlowReportService reports;
    private final CashFlowForecastService forecasts;
    private final CashFlowEntryRepository entries;
    private final ForecastSourceReader sources;
    private final Thresholds thresholds;
    private final List<CashFlowCategory> essentialCategories;
    private final Set<CashFlowCategory> essential;

    FinancialHealthService(
            MonthlyCashFlowReportService reports,
            CashFlowForecastService forecasts,
            CashFlowEntryRepository entries,
            ForecastSourceReader sources,
            FinancialHealthProperties properties) {
        this.reports = reports;
        this.forecasts = forecasts;
        this.entries = entries;
        this.sources = sources;
        this.thresholds = new Thresholds(
                months(properties.reserveOkMinMonths()),
                months(properties.minReserveMonths()),
                fraction(properties.debtBurdenOkMax()),
                fraction(properties.maxDebtPaymentBurden()),
                fraction(properties.passiveIncomeHighMin()),
                fraction(properties.passiveIncomeMediumMin()),
                fraction(properties.essentialShareOkMax()),
                fraction(properties.essentialShareWatchMax()),
                properties.dueWindowDays(),
                fraction(properties.maxIncomeSourceShare()));
        this.essentialCategories = properties.essentialCategories();
        this.essential = EnumSet.copyOf(properties.essentialCategories());
    }

    /**
     * The owner's financial health for {@code monthText}, or for the current month in {@code
     * Asia/Ho_Chi_Minh} when it is absent.
     *
     * <p>Read-only and in one transaction, so the report, the forecast, the assets, the liabilities and
     * the rate are read from the same snapshot.
     */
    @Transactional(readOnly = true)
    public FinancialHealth healthFor(UUID ownerId, String monthText) {
        ReportMonth requested = monthText == null ? null : monthOf(monthText);
        CashFlowForecast forecast = forecasts.forecastFor(ownerId, null);
        LocalDate today = forecast.horizon().asOfDate();
        ReportMonth month = requested != null ? requested : new ReportMonth(YearMonth.from(today));

        MonthlyCashFlowReport report = reports.reportFor(ownerId, month);
        Optional<ExchangeRate> rate = report.exchangeRate();
        List<ValuedAsset> liquid = sources.assetsIn(ownerId, LIQUID_ASSET_CATEGORIES);
        List<ScheduledLiability> liabilities = sources.liabilitiesOf(ownerId);
        List<CashFlowEntry> recurringEssential = entries.findActiveBetween(ownerId, month.first(), month.last())
                .stream()
                .filter(entry -> entry.getTiming() == CashFlowTiming.RECURRING
                        && entry.getEntryType() == CashFlowEntryType.EXPENSE
                        && essential.contains(entry.getCategory()))
                .sorted(Comparator.comparing(entry -> entry.getId().toString()))
                .toList();

        Request request = new Request(today, report, forecast, rate, liquid, liabilities, recurringEssential);
        EssentialMonth essentialMonth = essentialMonth(request);

        Indicator reserve = reserve(request, essentialMonth);
        Indicator debt = debtBurden(request);
        Indicator minimum = minimumBalance(request, essentialMonth);
        Indicator passive = passiveShare(request);
        Indicator essentialShare = essentialShare(request);

        LocalDate windowTo = today.plusDays(thresholds.dueWindowDays());
        Assumptions assumptions = new Assumptions(
                today,
                TimeConventions.DEFAULT_DISPLAY_ZONE.getId(),
                month.value(),
                forecast.horizon().from(),
                forecast.horizon().to(),
                today,
                windowTo,
                LIQUID_ASSET_CATEGORIES,
                CASH_ACCOUNT_CATEGORIES,
                essentialCategories,
                DEBT_PAYMENT_CATEGORIES,
                rate.orElse(null));

        return new FinancialHealth(
                assumptions,
                thresholds,
                List.of(reserve, debt, minimum, passive, essentialShare),
                List.of(
                        spendingExceedsIncome(request),
                        forecastCashNegative(minimum),
                        reserveBelowReference(reserve),
                        highDebtPaymentBurden(debt),
                        liquidityShort(request, windowTo),
                        incomeConcentration(request)));
    }

    // ---------------------------------------------------------------- indicators

    /** Liquid assets over a month of recurring essential expense, in months at one decimal. */
    private Indicator reserve(Request request, EssentialMonth denominator) {
        List<Source> rows = new ArrayList<>();
        BigDecimal liquidVnd = ZERO;
        int converted = 0;
        boolean missing = false;
        for (ValuedAsset asset : request.liquid()) {
            BigDecimal vnd = toVnd(asset.value(), asset.currency(), request.rate());
            rows.add(assetSource(asset, Role.NUMERATOR, vnd));
            if (vnd == null) {
                missing = true;
            } else {
                liquidVnd = liquidVnd.add(vnd);
                converted++;
            }
        }
        rows.addAll(denominator.rows());

        IndicatorId id = IndicatorId.EMERGENCY_RESERVE_MONTHS;
        Formula formula = Formula.LIQUID_ASSETS_OVER_MONTHLY_RECURRING_ESSENTIAL_EXPENSE;
        if (request.liquid().isEmpty()) {
            return unavailable(id, Unit.MONTHS, formula, Reason.NO_LIQUID_ASSETS, rows);
        }
        if (converted == 0) {
            return unavailable(id, Unit.MONTHS, formula, Reason.MISSING_EXCHANGE_RATE, rows);
        }
        Optional<Reason> noDenominator = denominator.unavailableReason();
        if (noDenominator.isPresent()) {
            return unavailable(id, Unit.MONTHS, formula, noDenominator.get(), rows);
        }
        BigDecimal value = liquidVnd.divide(
                denominator.amount(), FinancialHealthProperties.MONTHS_SCALE, RoundingMode.HALF_UP);
        boolean partial = missing || denominator.missing();
        Band band = value.compareTo(thresholds.reserveOkMinMonths()) >= 0
                ? Band.OK
                : value.compareTo(thresholds.minReserveMonths()) >= 0 ? Band.WATCH : Band.ATTENTION;
        return computed(
                id, value, Unit.MONTHS, partial, band,
                new Calculation(formula, liquidVnd, denominator.amount(), null, null, rows));
    }

    /** Debt-principal and interest payments of the month over the month's income. */
    private Indicator debtBurden(Request request) {
        List<Line> income = request.lines(CashFlowEntryType.INCOME);
        List<Line> debt = request.lines(CashFlowEntryType.EXPENSE).stream()
                .filter(line -> DEBT_PAYMENT_CATEGORIES.contains(category(line)))
                .toList();
        List<Source> rows = new ArrayList<>();
        debt.forEach(line -> rows.add(lineSource(line, Role.NUMERATOR)));
        income.forEach(line -> rows.add(lineSource(line, Role.DENOMINATOR)));

        IndicatorId id = IndicatorId.DEBT_PAYMENT_BURDEN;
        Formula formula = Formula.DEBT_PAYMENTS_OVER_INCOME;
        BigDecimal incomeVnd = request.report().totals().income();
        Optional<Reason> noIncome = noTotal(income, incomeVnd, Reason.NO_INCOME);
        if (noIncome.isPresent()) {
            return unavailable(id, Unit.FRACTION, formula, noIncome.get(), rows);
        }
        BigDecimal debtVnd = sum(debt);
        BigDecimal value = share(debtVnd, incomeVnd);
        Band band = value.compareTo(thresholds.debtBurdenOkMax()) <= 0
                ? Band.OK
                : value.compareTo(thresholds.maxDebtPaymentBurden()) <= 0 ? Band.WATCH : Band.ATTENTION;
        return computed(
                id, value, Unit.FRACTION, anyUnconverted(income) || anyUnconverted(debt), band,
                new Calculation(formula, debtVnd, incomeVnd, null, null, rows));
    }

    /**
     * The forecast's lowest projected position, banded against one month of recurring essential expense.
     * The sources are the accounts the projection starts from and every amount landing up to the day the
     * lowest balance is reached, so the opening balance plus those amounts is the value.
     */
    private Indicator minimumBalance(Request request, EssentialMonth reference) {
        CashFlowForecast forecast = request.forecast();
        IndicatorId id = IndicatorId.FORECAST_MINIMUM_CASH_BALANCE;
        Formula formula = Formula.LOWEST_PROJECTED_CASH_BALANCE;
        List<Source> rows = new ArrayList<>();
        for (OpeningSource opening : forecast.openingBalance().sources()) {
            rows.add(new Source(
                    SourceType.ASSET,
                    opening.sourceId(),
                    opening.category(),
                    Role.OPENING_BALANCE,
                    opening.valuationDate(),
                    opening.amount(),
                    opening.currency(),
                    vnd(opening.amountVnd()),
                    null,
                    null));
        }
        request.cashAccounts().stream()
                .filter(asset -> toVnd(asset.value(), asset.currency(), request.rate()) == null)
                .forEach(asset -> rows.add(assetSource(asset, Role.OPENING_BALANCE, null)));

        if (forecast.openingBalance().sources().isEmpty()) {
            Reason reason = forecast.excludedSources().stream()
                            .anyMatch(exclusion -> exclusion.sourceType() == CashFlowForecast.SourceType.ASSET
                                    && exclusion.reason() == ExclusionReason.EXCHANGE_RATE_MISSING)
                    ? Reason.MISSING_EXCHANGE_RATE
                    : Reason.NO_CASH_ACCOUNTS;
            return unavailable(id, Unit.VND, formula, reason, rows);
        }

        MinimumBalance lowest = forecast.minimumBalance();
        boolean atOpening = lowest.date().equals(forecast.horizon().from())
                && lowest.amount().compareTo(forecast.openingBalance().amount()) == 0;
        if (!atOpening) {
            forecast.months().stream()
                    .flatMap(month -> month.items().stream())
                    .filter(item -> !item.date().isAfter(lowest.date()))
                    .forEach(item -> rows.add(itemSource(item)));
        }

        BigDecimal value = vnd(lowest.amount());
        boolean partial = forecast.partial();
        BigDecimal referenceVnd = reference.unavailableReason().isEmpty() && !reference.missing()
                ? reference.amount()
                : null;
        Calculation calculation = new Calculation(formula, null, null, referenceVnd, lowest.date(), rows);
        Reason partialReason = forecast.excludedSources().stream()
                        .map(Exclusion::reason)
                        .anyMatch(ExclusionReason.EXCHANGE_RATE_MISSING::equals)
                ? Reason.MISSING_EXCHANGE_RATE
                : Reason.FORECAST_INCOMPLETE;
        if (partial) {
            return new Indicator(
                    id, Availability.PARTIAL, value, Unit.VND, null, BandNotDeterminedReason.PARTIAL_INPUTS,
                    null, partialReason, calculation);
        }
        if (referenceVnd == null) {
            BandNotDeterminedReason why = reference.missing()
                    ? BandNotDeterminedReason.PARTIAL_INPUTS
                    : BandNotDeterminedReason.NO_RECURRING_ESSENTIAL_EXPENSES;
            return new Indicator(
                    id, Availability.COMPLETE, value, Unit.VND, null, why, null, null, calculation);
        }
        Band band = value.compareTo(referenceVnd) >= 0
                ? Band.OK
                : value.signum() >= 0 ? Band.WATCH : Band.ATTENTION;
        return new Indicator(id, Availability.COMPLETE, value, Unit.VND, band, null, null, null, calculation);
    }

    /** Income linked to an asset over the month's income, as the report's passive income counts it. */
    private Indicator passiveShare(Request request) {
        List<Line> income = request.lines(CashFlowEntryType.INCOME);
        List<Source> rows = income.stream()
                .map(line -> lineSource(
                        line,
                        line.occurrence().entry().getLinkedAssetId() != null ? Role.SHARE_PART : Role.SHARE_REST))
                .toList();
        IndicatorId id = IndicatorId.PASSIVE_INCOME_SHARE;
        Formula formula = Formula.ASSET_LINKED_INCOME_OVER_INCOME;
        BigDecimal incomeVnd = request.report().totals().income();
        Optional<Reason> noIncome = noTotal(income, incomeVnd, Reason.NO_INCOME);
        if (noIncome.isPresent()) {
            return unavailable(id, Unit.FRACTION, formula, noIncome.get(), rows);
        }
        BigDecimal passiveVnd = request.report().passiveIncome().total();
        BigDecimal value = share(passiveVnd, incomeVnd);
        Band band = value.compareTo(thresholds.passiveIncomeHighMin()) >= 0
                ? Band.HIGH
                : value.compareTo(thresholds.passiveIncomeMediumMin()) >= 0 ? Band.MEDIUM : Band.LOW;
        return computed(
                id, value, Unit.FRACTION, anyUnconverted(income), band,
                new Calculation(formula, passiveVnd, incomeVnd, null, null, rows));
    }

    /** Expense in an essential category over the month's expense. */
    private Indicator essentialShare(Request request) {
        List<Line> expense = request.lines(CashFlowEntryType.EXPENSE);
        List<Source> rows = expense.stream()
                .map(line -> lineSource(line, essential.contains(category(line)) ? Role.SHARE_PART : Role.SHARE_REST))
                .toList();
        IndicatorId id = IndicatorId.ESSENTIAL_EXPENSE_SHARE;
        Formula formula = Formula.ESSENTIAL_EXPENSE_OVER_EXPENSE;
        BigDecimal expenseVnd = request.report().totals().expense();
        Optional<Reason> noExpense = noTotal(expense, expenseVnd, Reason.NO_EXPENSES);
        if (noExpense.isPresent()) {
            return unavailable(id, Unit.FRACTION, formula, noExpense.get(), rows);
        }
        BigDecimal essentialVnd = sum(expense.stream().filter(line -> essential.contains(category(line))).toList());
        BigDecimal value = share(essentialVnd, expenseVnd);
        Band band = value.compareTo(thresholds.essentialShareOkMax()) <= 0
                ? Band.OK
                : value.compareTo(thresholds.essentialShareWatchMax()) <= 0 ? Band.WATCH : Band.ATTENTION;
        return computed(
                id, value, Unit.FRACTION, anyUnconverted(expense), band,
                new Calculation(formula, essentialVnd, expenseVnd, null, null, rows));
    }

    /**
     * Every recurring essential expense in effect in the month, at its monthly equivalent: a monthly
     * amount once, a quarterly one divided by three, a yearly one by twelve, each rounded half up to two
     * decimals before the sum. A one-time expense never enters it, so a single large bill cannot move the
     * reserve. In effect means started no later than the month's last day and not ended before its first,
     * whether or not the entry falls due inside the month.
     */
    private EssentialMonth essentialMonth(Request request) {
        List<Source> rows = new ArrayList<>();
        BigDecimal total = ZERO;
        boolean missing = false;
        for (CashFlowEntry entry : request.recurringEssential()) {
            BigDecimal vnd = toVnd(entry.getAmount(), entry.getCurrency(), request.rate());
            BigDecimal monthly = vnd == null
                    ? null
                    : vnd.divide(BigDecimal.valueOf(entry.getRecurrence().months()), AMOUNT_SCALE, RoundingMode.HALF_UP);
            rows.add(new Source(
                    SourceType.CASH_FLOW_ENTRY,
                    entry.getId(),
                    entry.getCategory().name(),
                    Role.DENOMINATOR,
                    entry.getEffectiveDate(),
                    entry.getAmount(),
                    entry.getCurrency(),
                    vnd,
                    entry.getRecurrence().name(),
                    monthly));
            if (monthly == null) {
                missing = true;
            } else {
                total = total.add(monthly);
            }
        }
        return new EssentialMonth(total, missing, request.recurringEssential().isEmpty(), List.copyOf(rows));
    }

    // ---------------------------------------------------------------- rules

    /** Net cash flow of the month below zero. Judged on the report's own totals. */
    private Rule spendingExceedsIncome(Request request) {
        RuleId id = RuleId.SPENDING_EXCEEDS_INCOME;
        Threshold threshold = new Threshold(Comparison.LESS_THAN, ZERO, Unit.VND, null);
        MonthlyCashFlowReport report = request.report();
        List<Source> evidence = report.lines().stream()
                .map(line -> lineSource(line, type(line) == CashFlowEntryType.INCOME ? Role.INCOME : Role.EXPENSE))
                .toList();
        if (report.lines().isEmpty()) {
            return notEvaluated(id, null, threshold, evidence, NotEvaluatedReason.NO_CASH_FLOW_IN_MONTH);
        }
        if (!report.complete()) {
            return notEvaluated(
                    id, null, threshold,
                    evidence.stream().filter(row -> row.amountVnd() == null).toList(),
                    NotEvaluatedReason.MISSING_EXCHANGE_RATE);
        }
        BigDecimal net = report.totals().net();
        return judged(id, null, net.signum() < 0, new Observed(net, Unit.VND, null, null), threshold, evidence);
    }

    private Rule forecastCashNegative(Indicator minimum) {
        Threshold threshold = new Threshold(Comparison.LESS_THAN, ZERO, Unit.VND, null);
        return onIndicator(
                RuleId.FORECAST_CASH_NEGATIVE,
                minimum,
                threshold,
                new Observed(minimum.value(), Unit.VND, null, minimum.calculation().observedOn()));
    }

    private Rule reserveBelowReference(Indicator reserve) {
        Threshold threshold =
                new Threshold(Comparison.LESS_THAN, thresholds.minReserveMonths(), Unit.MONTHS, "min-reserve-months");
        return onIndicator(
                RuleId.RESERVE_BELOW_REFERENCE, reserve, threshold, new Observed(reserve.value(), Unit.MONTHS, null, null));
    }

    private Rule highDebtPaymentBurden(Indicator debt) {
        Threshold threshold = new Threshold(
                Comparison.GREATER_THAN, thresholds.maxDebtPaymentBurden(), Unit.FRACTION, "max-debt-payment-burden");
        return onIndicator(
                RuleId.HIGH_DEBT_PAYMENT_BURDEN, debt, threshold, new Observed(debt.value(), Unit.FRACTION, null, null));
    }

    /**
     * Cash and bank accounts today against what falls due from today to the end of the window.
     *
     * <p>A liability maturing inside the window contributes its whole outstanding balance, which already
     * contains every payment left on it, so its scheduled payments are not added a second time. Any other
     * liability contributes each payment its stated schedule places in the window. A payment the schedule
     * places before today is not counted: it is a fact about today's balance, as in the forecast.
     */
    private Rule liquidityShort(Request request, LocalDate windowTo) {
        RuleId id = RuleId.LIQUIDITY_SHORT_FOR_UPCOMING_OBLIGATIONS;
        LocalDate today = request.today();
        List<Source> cash = request.cashAccounts().stream()
                .map(asset -> assetSource(asset, Role.CASH_ACCOUNT, toVnd(asset.value(), asset.currency(), request.rate())))
                .toList();
        List<Source> due = new ArrayList<>();
        for (ScheduledLiability liability : request.liabilities()) {
            LocalDate maturity = liability.maturityDate();
            if (maturity != null && !maturity.isBefore(today) && !maturity.isAfter(windowTo)) {
                due.add(liabilitySource(
                        liability, Role.MATURITY_DUE, maturity, liability.outstandingBalance(), request.rate()));
                continue;
            }
            if (liability.paymentAmount() == null || liability.nextPaymentDate() == null) {
                continue;
            }
            for (Payment payment : LiabilityPaymentSchedule.paymentsOf(
                    liability.nextPaymentDate(),
                    LiabilityPaymentSchedule.monthsBetweenPayments(liability.frequency()),
                    maturity,
                    today,
                    windowTo)) {
                due.add(liabilitySource(
                        liability, Role.PAYMENT_DUE, payment.date(), liability.paymentAmount(), request.rate()));
            }
        }
        due.sort(Comparator.comparing(Source::date).thenComparing(row -> row.sourceId().toString()));
        List<Source> evidence = new ArrayList<>(cash);
        evidence.addAll(due);

        boolean dueMissing = due.stream().anyMatch(row -> row.amountVnd() == null);
        BigDecimal dueVnd = dueMissing ? null : sumRows(due);
        Threshold threshold = new Threshold(Comparison.LESS_THAN, dueVnd, Unit.VND, "due-window-days");
        if (cash.isEmpty()) {
            return notEvaluated(id, null, threshold, evidence, NotEvaluatedReason.NO_CASH_ACCOUNTS);
        }
        if (request.liabilities().isEmpty()) {
            return notEvaluated(id, null, threshold, evidence, NotEvaluatedReason.NO_LIABILITIES);
        }
        if (dueMissing || cash.stream().anyMatch(row -> row.amountVnd() == null)) {
            return notEvaluated(id, null, threshold, evidence, NotEvaluatedReason.MISSING_EXCHANGE_RATE);
        }
        BigDecimal cashVnd = sumRows(cash);
        return judged(
                id, null, cashVnd.compareTo(dueVnd) < 0, new Observed(cashVnd, Unit.VND, null, null), threshold, evidence);
    }

    /**
     * The largest income category's share of the month's income. A source is a category, as the report's
     * income composition groups it; the evidence is that category's occurrences.
     */
    private Rule incomeConcentration(Request request) {
        RuleId id = RuleId.INCOME_CONCENTRATION;
        Threshold threshold = new Threshold(
                Comparison.GREATER_THAN, thresholds.maxIncomeSourceShare(), Unit.FRACTION, "max-income-source-share");
        List<Line> income = request.lines(CashFlowEntryType.INCOME);
        BigDecimal incomeVnd = request.report().totals().income();
        if (income.isEmpty()) {
            return notEvaluated(id, null, threshold, List.of(), NotEvaluatedReason.NO_INCOME);
        }
        if (anyUnconverted(income) || incomeVnd.signum() == 0) {
            return notEvaluated(
                    id, null, threshold,
                    income.stream().filter(line -> line.amount() == null).map(line -> lineSource(line, Role.INCOME)).toList(),
                    NotEvaluatedReason.MISSING_EXCHANGE_RATE);
        }
        CategoryShare largest = request.report().incomeComposition().getFirst();
        BigDecimal value = share(largest.amount(), incomeVnd);
        return judged(
                id,
                null,
                value.compareTo(threshold.value()) > 0,
                new Observed(value, Unit.FRACTION, largest.category().name(), null),
                threshold,
                income.stream()
                        .filter(line -> category(line) == largest.category())
                        .map(line -> lineSource(line, Role.INCOME))
                        .toList());
    }

    /**
     * A rule that judges one indicator's value. A partial or unavailable indicator is not judged: a value
     * missing part of its inputs could read as within the threshold when the whole value is not.
     */
    private static Rule onIndicator(RuleId id, Indicator indicator, Threshold threshold, Observed observed) {
        List<Source> evidence = indicator.calculation().sources();
        return switch (indicator.availability()) {
            case UNAVAILABLE -> notEvaluated(
                    id, indicator.indicatorId(), threshold, evidence, NotEvaluatedReason.INDICATOR_UNAVAILABLE);
            case PARTIAL -> notEvaluated(
                    id, indicator.indicatorId(), threshold, evidence, NotEvaluatedReason.INDICATOR_PARTIAL);
            case COMPLETE -> {
                int comparison = indicator.value().compareTo(threshold.value());
                boolean attention = threshold.comparison() == Comparison.LESS_THAN ? comparison < 0 : comparison > 0;
                yield judged(id, indicator.indicatorId(), attention, observed, threshold, evidence);
            }
        };
    }

    // ---------------------------------------------------------------- helpers

    private static Rule judged(
            RuleId id, IndicatorId dependsOn, boolean attention, Observed observed, Threshold threshold, List<Source> evidence) {
        return new Rule(id, attention ? Status.ATTENTION : Status.OK, dependsOn, observed, threshold, evidence, null);
    }

    private static Rule notEvaluated(
            RuleId id, IndicatorId dependsOn, Threshold threshold, List<Source> evidence, NotEvaluatedReason reason) {
        return new Rule(id, Status.NOT_EVALUATED, dependsOn, null, threshold, evidence, reason);
    }

    private static Indicator unavailable(
            IndicatorId id, Unit unit, Formula formula, Reason reason, List<Source> rows) {
        return new Indicator(
                id,
                Availability.UNAVAILABLE,
                null,
                unit,
                null,
                BandNotDeterminedReason.INDICATOR_UNAVAILABLE,
                reason,
                null,
                new Calculation(formula, null, null, null, null, List.copyOf(rows)));
    }

    /** A computed value; a partial one has no band, because the whole value may fall in another. */
    private static Indicator computed(
            IndicatorId id, BigDecimal value, Unit unit, boolean partial, Band band, Calculation calculation) {
        Calculation frozen = new Calculation(
                calculation.formula(),
                calculation.numeratorVnd(),
                calculation.denominatorVnd(),
                calculation.referenceVnd(),
                calculation.observedOn(),
                List.copyOf(calculation.sources()));
        if (partial) {
            return new Indicator(
                    id, Availability.PARTIAL, value, unit, null, BandNotDeterminedReason.PARTIAL_INPUTS,
                    null, Reason.MISSING_EXCHANGE_RATE, frozen);
        }
        return new Indicator(id, Availability.COMPLETE, value, unit, band, null, null, null, frozen);
    }

    /**
     * Why a total cannot be divided by: there is no line at all, or every line is USD with no rate. A
     * total of zero over converted lines cannot happen, since an entry's amount is positive.
     */
    private static Optional<Reason> noTotal(List<Line> lines, BigDecimal total, Reason none) {
        if (lines.isEmpty()) {
            return Optional.of(none);
        }
        if (total.signum() == 0) {
            return Optional.of(anyUnconverted(lines) ? Reason.MISSING_EXCHANGE_RATE : none);
        }
        return Optional.empty();
    }

    private static Source lineSource(Line line, Role role) {
        CashFlowOccurrence occurrence = line.occurrence();
        CashFlowEntry entry = occurrence.entry();
        return new Source(
                SourceType.CASH_FLOW_ENTRY,
                entry.getId(),
                entry.getCategory().name(),
                role,
                occurrence.date(),
                entry.getAmount(),
                entry.getCurrency(),
                line.amount(),
                entry.getRecurrence() == null ? null : entry.getRecurrence().name(),
                null);
    }

    private static Source assetSource(ValuedAsset asset, Role role, BigDecimal amountVnd) {
        return new Source(
                SourceType.ASSET,
                asset.id(),
                asset.category(),
                role,
                asset.valuationDate(),
                asset.value(),
                asset.currency(),
                amountVnd,
                null,
                null);
    }

    private static Source liabilitySource(
            ScheduledLiability liability, Role role, LocalDate date, BigDecimal amount, Optional<ExchangeRate> rate) {
        return new Source(
                SourceType.LIABILITY,
                liability.id(),
                liability.category(),
                role,
                date,
                amount,
                liability.currency(),
                toVnd(amount, liability.currency(), rate),
                liability.frequency(),
                null);
    }

    private static Source itemSource(Item item) {
        return new Source(
                SourceType.valueOf(item.sourceType().name()),
                item.sourceId(),
                item.category(),
                item.direction() == Direction.INCOME ? Role.PROJECTED_INCOME : Role.PROJECTED_EXPENSE,
                item.date(),
                item.amount(),
                item.currency(),
                vnd(item.amountVnd()),
                item.cadence(),
                null);
    }

    /**
     * The amount in VND at two decimals, converted as the report converts a line, or {@code null} when it
     * is USD and no rate is declared. Never zero for a missing rate.
     */
    private static BigDecimal toVnd(BigDecimal amount, Currency currency, Optional<ExchangeRate> rate) {
        return switch (currency) {
            case VND -> amount.setScale(AMOUNT_SCALE);
            case USD -> rate.map(usdToVnd -> ConversionTrace.convert(Money.of(amount, Currency.USD), usdToVnd)
                            .converted()
                            .amount()
                            .setScale(AMOUNT_SCALE))
                    .orElse(null);
        };
    }

    /** A forecast amount, whole đồng, written at the two decimals every VND figure here carries. */
    private static BigDecimal vnd(BigDecimal amount) {
        return amount.setScale(AMOUNT_SCALE, RoundingMode.UNNECESSARY);
    }

    private static CashFlowCategory category(Line line) {
        return line.occurrence().entry().getCategory();
    }

    private static CashFlowEntryType type(Line line) {
        return line.occurrence().entry().getEntryType();
    }

    private static boolean anyUnconverted(List<Line> lines) {
        return lines.stream().anyMatch(line -> line.amount() == null);
    }

    private static BigDecimal sum(List<Line> lines) {
        return lines.stream().map(Line::amount).filter(Objects::nonNull).reduce(ZERO, BigDecimal::add);
    }

    private static BigDecimal sumRows(List<Source> rows) {
        return rows.stream().map(Source::amountVnd).filter(Objects::nonNull).reduce(ZERO, BigDecimal::add);
    }

    private static BigDecimal share(BigDecimal part, BigDecimal whole) {
        return part.divide(whole, FinancialHealthProperties.FRACTION_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal fraction(BigDecimal value) {
        return value.setScale(FinancialHealthProperties.FRACTION_SCALE, RoundingMode.UNNECESSARY);
    }

    private static BigDecimal months(BigDecimal value) {
        return value.setScale(FinancialHealthProperties.MONTHS_SCALE, RoundingMode.UNNECESSARY);
    }

    /**
     * The month asked for. A blank value is a mistake rather than a request for the default, because a
     * client that sends {@code month=} meant to send something; it gets the same sentence as a malformed
     * value. The value sent is never repeated.
     */
    private static ReportMonth monthOf(String text) {
        if (text.isBlank()) {
            throw new ApiException(
                    ProblemType.VALIDATION_FAILED,
                    ProblemType.VALIDATION_FAILED.safeDetail(),
                    Map.of("errors", List.of(Map.of("field", "month", "message", EnteredDates.CALENDAR_MONTH))));
        }
        return ReportMonth.of(text);
    }

    /** What one request reads before anything is computed. */
    private record Request(
            LocalDate today,
            MonthlyCashFlowReport report,
            CashFlowForecast forecast,
            Optional<ExchangeRate> rate,
            List<ValuedAsset> liquid,
            List<ScheduledLiability> liabilities,
            List<CashFlowEntry> recurringEssential) {

        List<Line> lines(CashFlowEntryType type) {
            Predicate<Line> ofType = line -> line.occurrence().entry().getEntryType() == type;
            return report.lines().stream().filter(ofType).toList();
        }

        List<ValuedAsset> cashAccounts() {
            return liquid.stream().filter(asset -> CASH_ACCOUNT_CATEGORIES.contains(asset.category())).toList();
        }
    }

    /**
     * A month of recurring essential expense.
     *
     * @param amount the sum of the converted monthly equivalents
     * @param missing whether a USD entry could not be converted
     * @param none whether no recurring essential expense is in effect at all
     * @param rows one denominator row per entry
     */
    private record EssentialMonth(BigDecimal amount, boolean missing, boolean none, List<Source> rows) {

        /** Why a month of it cannot divide anything, if it cannot. */
        Optional<Reason> unavailableReason() {
            if (none) {
                return Optional.of(Reason.NO_RECURRING_ESSENTIAL_EXPENSES);
            }
            if (amount.signum() == 0) {
                return Optional.of(missing ? Reason.MISSING_EXCHANGE_RATE : Reason.NO_RECURRING_ESSENTIAL_EXPENSES);
            }
            return Optional.empty();
        }
    }
}
