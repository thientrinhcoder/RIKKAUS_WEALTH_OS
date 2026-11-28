package com.rikkaus.wealth.advisor.application;

import com.rikkaus.wealth.advisor.application.AdvisorReport.ActionFamily;
import com.rikkaus.wealth.advisor.application.AdvisorReport.ActionId;
import com.rikkaus.wealth.advisor.application.AdvisorReport.AffectedRecord;
import com.rikkaus.wealth.advisor.application.AdvisorReport.Comparison;
import com.rikkaus.wealth.advisor.application.AdvisorReport.Finding;
import com.rikkaus.wealth.advisor.application.AdvisorReport.NoteId;
import com.rikkaus.wealth.advisor.application.AdvisorReport.Observed;
import com.rikkaus.wealth.advisor.application.AdvisorReport.RecordType;
import com.rikkaus.wealth.advisor.application.AdvisorReport.Source;
import com.rikkaus.wealth.advisor.application.AdvisorReport.SourceSummary;
import com.rikkaus.wealth.advisor.application.AdvisorReport.Status;
import com.rikkaus.wealth.advisor.application.AdvisorReport.Suggestion;
import com.rikkaus.wealth.advisor.application.AdvisorReport.Threshold;
import com.rikkaus.wealth.advisor.application.AdvisorReport.Unit;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.Calculation;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.Indicator;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.Role;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealth.Rule;
import com.rikkaus.wealth.cashflow.health.application.FinancialHealthService;
import com.rikkaus.wealth.goals.projection.application.GoalProjection;
import com.rikkaus.wealth.goals.projection.application.GoalProjection.Assumption;
import com.rikkaus.wealth.goals.projection.application.GoalProjection.AssumptionKey;
import com.rikkaus.wealth.goals.projection.application.GoalProjection.GoalProjectionStatus;
import com.rikkaus.wealth.goals.projection.application.GoalProjectionService;
import com.rikkaus.wealth.shared.api.ApiPaths;
import com.rikkaus.wealth.shared.money.Currency;
import com.rikkaus.wealth.snapshot.insights.application.SnapshotInsightService;
import com.rikkaus.wealth.snapshot.insights.application.SnapshotInsights;
import com.rikkaus.wealth.snapshot.insights.application.SnapshotInsights.Evidence;
import com.rikkaus.wealth.snapshot.insights.application.SnapshotInsights.Insight;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Composes the basic advisor report from the wealth-snapshot insights, the financial-health rules and the
 * goal projections, each read through its own application service.
 *
 * <p>The advisor composes; it never recalculates. Every observation and threshold is the upstream one,
 * and the only arithmetic here is the relative deviation the rank order needs. It defines no threshold of
 * its own and stores nothing: the same records on the same day under the same configuration give the
 * same report, byte for byte.
 *
 * <p>The three reads share one read-only transaction, so they see the same records. The financial-health
 * read comes first because it validates {@code month}; a malformed month is refused before anything else
 * is read, with the response {@code GET /financial-health} gives.
 */
@Service
public class AdvisorReportService {

    /** How many of the first suggestions carry the priority marker: the overview's top-three contract. */
    public static final int PRIORITY_MARKER_COUNT = 3;

    static final String DISCLAIMER =
            "This report is informational, based on the data you entered and the system's reference "
                    + "thresholds; it is not financial advice and does not guarantee any outcome.";

    /** The rule identifier a goal finding carries; goals have no upstream rule constant of their own. */
    static final String GOAL_RULE_ID = "GOAL_CONTRIBUTION_COVERAGE";

    private static final String GOAL_THRESHOLD_PROPERTY = "rikkaus.goals.projection.on-track-min-coverage-percent";
    private static final String HEALTH_PROPERTY_PREFIX = "rikkaus.cashflow.health.";
    private static final String TIMELINE_REASON_PREFIX = "TIMELINE_";
    private static final int DEVIATION_SCALE = 6;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /** The property each snapshot rule's threshold comes from; the insights report does not name them. */
    private static final Map<String, String> SNAPSHOT_PROPERTIES = Map.of(
            SnapshotInsights.RuleId.CATEGORY_CONCENTRATION.name(), "rikkaus.snapshot.insights.max-category-share",
            SnapshotInsights.RuleId.DEBT_TO_ASSET_RATIO.name(), "rikkaus.snapshot.insights.max-debt-to-asset-ratio",
            SnapshotInsights.RuleId.LOW_LIQUIDITY_SHARE.name(), "rikkaus.snapshot.insights.max-low-liquidity-share",
            SnapshotInsights.RuleId.UPCOMING_LIABILITY_DUE.name(), "rikkaus.snapshot.insights.due-window-days",
            SnapshotInsights.RuleId.STALE_VALUATION.name(), "rikkaus.snapshot.insights.max-valuation-age-days",
            SnapshotInsights.RuleId.STALE_EXCHANGE_RATE.name(), "rikkaus.snapshot.insights.max-exchange-rate-age-days");

    /** The priority tier and action family of every rule, design #90 section 04. */
    private static final Map<String, Placement> PLACEMENTS = Map.ofEntries(
            Map.entry(SnapshotInsights.RuleId.UPCOMING_LIABILITY_DUE.name(),
                    new Placement(1, ActionFamily.RECONCILE_CASH_WITH_OBLIGATIONS)),
            Map.entry(FinancialHealth.RuleId.LIQUIDITY_SHORT_FOR_UPCOMING_OBLIGATIONS.name(),
                    new Placement(1, ActionFamily.RECONCILE_CASH_WITH_OBLIGATIONS)),
            Map.entry(FinancialHealth.RuleId.FORECAST_CASH_NEGATIVE.name(),
                    new Placement(1, ActionFamily.RECONCILE_CASH_WITH_OBLIGATIONS)),
            Map.entry(FinancialHealth.RuleId.SPENDING_EXCEEDS_INCOME.name(),
                    new Placement(2, ActionFamily.REVIEW_PERIOD_SPENDING)),
            Map.entry(FinancialHealth.RuleId.HIGH_DEBT_PAYMENT_BURDEN.name(),
                    new Placement(2, ActionFamily.REVIEW_DEBT_PAYMENTS)),
            Map.entry(FinancialHealth.RuleId.RESERVE_BELOW_REFERENCE.name(),
                    new Placement(3, ActionFamily.RECONCILE_CASH_WITH_RESERVE)),
            Map.entry(SnapshotInsights.RuleId.LOW_LIQUIDITY_SHARE.name(),
                    new Placement(3, ActionFamily.RECONCILE_CASH_WITH_RESERVE)),
            Map.entry(FinancialHealth.RuleId.INCOME_CONCENTRATION.name(),
                    new Placement(3, ActionFamily.REVIEW_INCOME_SOURCES)),
            Map.entry(SnapshotInsights.RuleId.CATEGORY_CONCENTRATION.name(),
                    new Placement(3, ActionFamily.REVIEW_ASSET_CONCENTRATION)),
            Map.entry(SnapshotInsights.RuleId.DEBT_TO_ASSET_RATIO.name(),
                    new Placement(3, ActionFamily.REVIEW_DEBT_LEVEL)),
            Map.entry(GOAL_RULE_ID, new Placement(4, ActionFamily.REVIEW_GOAL_PLAN)),
            Map.entry(SnapshotInsights.RuleId.STALE_VALUATION.name(), new Placement(5, ActionFamily.REFRESH_RECORD)),
            Map.entry(SnapshotInsights.RuleId.STALE_EXCHANGE_RATE.name(),
                    new Placement(5, ActionFamily.REFRESH_RECORD)));

    /**
     * The action of each family, written in the frames design #90 allows ("may review / compare / check /
     * update / consider ... then review the result"): one step, no promised outcome, no urgency, no
     * product or institution named. The first variant names the first affected record.
     */
    private static final Map<ActionFamily, Action> ACTIONS = actions();

    /** Tier, then the nearest due date, then the largest deviation, then the identifier. */
    private static final Comparator<Finding> FINDING_ORDER = Comparator.comparing(Finding::tier)
            .thenComparing(Finding::dueDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Finding::relativeDeviation, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Finding::findingId);

    private static final Comparator<Suggestion> SUGGESTION_ORDER = Comparator.comparingInt(Suggestion::tier)
            .thenComparing(Suggestion::dueDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Suggestion::relativeDeviation, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Suggestion::suggestionId);

    private final SnapshotInsightService insights;
    private final FinancialHealthService health;
    private final GoalProjectionService goals;

    AdvisorReportService(SnapshotInsightService insights, FinancialHealthService health, GoalProjectionService goals) {
        this.insights = insights;
        this.health = health;
        this.goals = goals;
    }

    @Transactional(readOnly = true)
    public AdvisorReport reportFor(UUID ownerId, String monthText) {
        FinancialHealth healthReport = health.healthFor(ownerId, monthText);
        SnapshotInsights snapshot = insights.insightsFor(ownerId);
        List<GoalProjection> projections = goals.projectionsFor(ownerId);
        String month = healthReport.assumptions().month().toString();

        List<Finding> findings = new ArrayList<>();
        snapshot.insights().forEach(insight -> findings.add(snapshotFinding(insight)));
        Map<FinancialHealth.IndicatorId, Indicator> indicators = healthReport.indicators().stream()
                .collect(Collectors.toMap(Indicator::indicatorId, Function.identity()));
        healthReport.rules().forEach(rule -> findings.add(healthFinding(rule, indicators)));
        projections.forEach(projection -> findings.add(goalFinding(projection)));

        List<Finding> attention = withStatus(findings, Status.ATTENTION);
        return new AdvisorReport(
                healthReport.assumptions().asOfDate(),
                healthReport.assumptions().timeZone(),
                month,
                DISCLAIMER,
                PRIORITY_MARKER_COUNT,
                List.of(
                        new SourceSummary(
                                Source.WEALTH_SNAPSHOT, ApiPaths.V1 + "/wealth-snapshot/insights", snapshot.insights().size()),
                        new SourceSummary(
                                Source.FINANCIAL_HEALTH,
                                ApiPaths.V1 + "/financial-health?month=" + month,
                                healthReport.rules().size()),
                        new SourceSummary(Source.GOAL_PROJECTION, ApiPaths.V1 + "/goal-projections", projections.size())),
                withStatus(findings, Status.NOT_EVALUATED),
                withStatus(findings, Status.OK),
                attention,
                suggestions(attention));
    }

    private static Finding snapshotFinding(Insight insight) {
        String ruleId = insight.ruleId().name();
        Status status = switch (insight.status()) {
            case OK -> Status.OK;
            case ATTENTION -> Status.ATTENTION;
            case UNAVAILABLE -> Status.NOT_EVALUATED;
        };
        SnapshotInsights.Observed o = insight.observed();
        Observed observed = o == null
                ? null
                : new Observed(o.value(), unit(o.unit().name()), o.subject(), o.date(), o.numeratorVnd(), o.denominatorVnd());
        SnapshotInsights.Threshold t = insight.threshold();
        Threshold threshold = new Threshold(
                Comparison.valueOf(t.comparison().name()), t.value(), unit(t.unit().name()), SNAPSHOT_PROPERTIES.get(ruleId));
        List<AffectedRecord> records = insight.evidence().stream()
                .map(row -> record(RecordType.valueOf(row.sourceType().name()), row.sourceId(), row.name()))
                .toList();
        LocalDate dueDate = insight.ruleId() == SnapshotInsights.RuleId.UPCOMING_LIABILITY_DUE && o != null ? o.date() : null;
        boolean declaredRate = insight.evidence().stream().anyMatch(AdvisorReportService::isDeclaredRate);
        return finding(
                Source.WEALTH_SNAPSHOT,
                ruleId,
                Source.WEALTH_SNAPSHOT.name() + ":" + ruleId,
                status,
                observed,
                threshold,
                records,
                dueDate,
                deviation(observed, threshold),
                declaredRate ? NoteId.USER_DECLARED_EXCHANGE_RATE : NoteId.MANUAL_ENTRY_NOT_RECONCILED,
                insight.unavailableReason() == null ? null : insight.unavailableReason().name());
    }

    private static Finding healthFinding(Rule rule, Map<FinancialHealth.IndicatorId, Indicator> indicators) {
        String ruleId = rule.ruleId().name();
        Status status = switch (rule.status()) {
            case OK -> Status.OK;
            case ATTENTION -> Status.ATTENTION;
            case NOT_EVALUATED -> Status.NOT_EVALUATED;
        };
        FinancialHealth.Observed o = rule.observed();
        Calculation calculation = rule.dependsOn() == null ? null : indicators.get(rule.dependsOn()).calculation();
        Observed observed = o == null
                ? null
                : new Observed(
                        o.value(),
                        unit(o.unit().name()),
                        o.subject(),
                        o.date(),
                        calculation == null ? null : calculation.numeratorVnd(),
                        calculation == null ? null : calculation.denominatorVnd());
        FinancialHealth.Threshold t = rule.threshold();
        Threshold threshold = new Threshold(
                Comparison.valueOf(t.comparison().name()),
                t.value(),
                unit(t.unit().name()),
                t.property() == null ? null : HEALTH_PROPERTY_PREFIX + t.property());
        List<AffectedRecord> records = rule.evidence().stream()
                .map(row -> record(RecordType.valueOf(row.sourceType().name()), row.sourceId(), null))
                .toList();
        LocalDate dueDate = rule.ruleId() == FinancialHealth.RuleId.LIQUIDITY_SHORT_FOR_UPCOMING_OBLIGATIONS
                ? rule.evidence().stream()
                        .filter(row -> row.role() == Role.PAYMENT_DUE || row.role() == Role.MATURITY_DUE)
                        .map(FinancialHealth.Source::date)
                        .min(Comparator.naturalOrder())
                        .orElse(null)
                : null;
        NoteId note;
        if (rule.ruleId() == FinancialHealth.RuleId.FORECAST_CASH_NEGATIVE) {
            note = NoteId.FORECAST_FROM_ENTERED_SCHEDULES;
        } else if (rule.evidence().stream().anyMatch(row -> row.currency() == Currency.USD)) {
            note = NoteId.USER_DECLARED_EXCHANGE_RATE;
        } else {
            note = NoteId.MANUAL_ENTRY_NOT_RECONCILED;
        }
        return finding(
                Source.FINANCIAL_HEALTH,
                ruleId,
                Source.FINANCIAL_HEALTH.name() + ":" + ruleId,
                status,
                observed,
                threshold,
                records,
                dueDate,
                deviation(observed, threshold),
                note,
                rule.notEvaluatedReason() == null ? null : rule.notEvaluatedReason().name());
    }

    /**
     * A goal's coverage judged against the on-track percent its projection states. The planned and the
     * required contribution are the projection's own; the deviation is the share of the required
     * contribution not covered, negative when the plan covers more than it.
     */
    private static Finding goalFinding(GoalProjection projection) {
        Map<AssumptionKey, String> ledger = new EnumMap<>(AssumptionKey.class);
        for (Assumption assumption : projection.assumptions()) {
            if (assumption.value() != null) {
                ledger.put(assumption.key(), assumption.value());
            }
        }
        GoalProjectionStatus upstream = projection.status();
        Status status = upstream == null
                ? Status.NOT_EVALUATED
                : upstream == GoalProjectionStatus.ON_TRACK ? Status.OK : Status.ATTENTION;
        BigDecimal coverage = projection.contributionCoveragePercent();
        Observed observed = upstream == null
                ? null
                : new Observed(
                        coverage,
                        Unit.PERCENT,
                        null,
                        null,
                        decimal(ledger.get(AssumptionKey.PLANNED_MONTHLY_CONTRIBUTION)),
                        projection.requiredMonthlyContribution());
        Threshold threshold = new Threshold(
                Comparison.LESS_THAN,
                decimal(ledger.get(AssumptionKey.ON_TRACK_MIN_COVERAGE_PERCENT)),
                Unit.PERCENT,
                GOAL_THRESHOLD_PROPERTY);
        String target = ledger.get(AssumptionKey.TARGET_DATE);
        String reason = null;
        if (upstream == null) {
            reason = projection.unavailableReasons().isEmpty()
                    ? TIMELINE_REASON_PREFIX + projection.timeline().name()
                    : projection.unavailableReasons().getFirst().name();
        }
        return finding(
                Source.GOAL_PROJECTION,
                GOAL_RULE_ID,
                Source.GOAL_PROJECTION.name() + ":" + GOAL_RULE_ID + ":" + projection.goalId(),
                status,
                observed,
                threshold,
                List.of(record(RecordType.GOAL, projection.goalId(), projection.goalName())),
                target == null ? null : LocalDate.parse(target),
                observed == null || coverage == null
                        ? null
                        : HUNDRED.subtract(coverage).divide(HUNDRED, DEVIATION_SCALE, RoundingMode.HALF_UP),
                NoteId.USER_ASSUMED_RETURN,
                reason);
    }

    private static Finding finding(
            Source source,
            String ruleId,
            String findingId,
            Status status,
            Observed observed,
            Threshold threshold,
            List<AffectedRecord> records,
            LocalDate dueDate,
            BigDecimal deviation,
            NoteId note,
            String reason) {
        Placement placement = status == Status.ATTENTION ? Objects.requireNonNull(PLACEMENTS.get(ruleId), ruleId) : null;
        return new Finding(
                findingId,
                source,
                ruleId,
                status,
                placement == null ? null : placement.tier(),
                placement == null ? null : placement.family(),
                observed,
                threshold,
                distinct(records),
                dueDate,
                deviation,
                note,
                status == Status.NOT_EVALUATED ? reason : null);
    }

    /**
     * Merges the findings of one family that share a record, transitively, so the result does not depend
     * on the order the findings are visited in; then ranks the suggestions and marks the first ones.
     */
    private static List<Suggestion> suggestions(List<Finding> attention) {
        int size = attention.size();
        int[] parent = new int[size];
        for (int i = 0; i < size; i++) {
            parent[i] = i;
        }
        for (int i = 0; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                if (attention.get(i).actionFamily() == attention.get(j).actionFamily()
                        && shareARecord(attention.get(i), attention.get(j))) {
                    parent[root(parent, j)] = root(parent, i);
                }
            }
        }
        Map<Integer, List<Finding>> groups = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            groups.computeIfAbsent(root(parent, i), key -> new ArrayList<>()).add(attention.get(i));
        }
        List<Suggestion> unranked = groups.values().stream()
                .map(group -> suggestion(group.stream().sorted(FINDING_ORDER).toList()))
                .sorted(SUGGESTION_ORDER)
                .toList();
        List<Suggestion> ranked = new ArrayList<>(unranked.size());
        for (int i = 0; i < unranked.size(); i++) {
            Suggestion s = unranked.get(i);
            ranked.add(new Suggestion(
                    s.suggestionId(),
                    i + 1,
                    i < PRIORITY_MARKER_COUNT,
                    s.tier(),
                    s.actionFamily(),
                    s.actionId(),
                    s.action(),
                    s.findingIds(),
                    s.affectedRecords(),
                    s.dueDate(),
                    s.relativeDeviation(),
                    s.noteIds()));
        }
        return List.copyOf(ranked);
    }

    /** One suggestion over findings already in rank order; rank and priority are set by the caller. */
    private static Suggestion suggestion(List<Finding> findings) {
        Finding first = findings.getFirst();
        List<String> ids = findings.stream().map(Finding::findingId).toList();
        List<AffectedRecord> records =
                distinct(findings.stream().flatMap(f -> f.affectedRecords().stream()).toList());
        Action action = ACTIONS.get(first.actionFamily());
        String label = records.isEmpty() ? null : label(records.getFirst());
        return new Suggestion(
                String.join("+", ids),
                0,
                false,
                findings.stream().mapToInt(Finding::tier).min().orElseThrow(),
                first.actionFamily(),
                action.id(),
                label == null ? action.unnamed() : action.named().formatted(label),
                ids,
                records,
                findings.stream().map(Finding::dueDate).filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null),
                findings.stream()
                        .map(Finding::relativeDeviation)
                        .filter(Objects::nonNull)
                        .max(Comparator.naturalOrder())
                        .orElse(null),
                List.copyOf(findings.stream().map(Finding::noteId).collect(Collectors.toCollection(LinkedHashSet::new))));
    }

    private static boolean shareARecord(Finding a, Finding b) {
        return a.affectedRecords().stream()
                .anyMatch(left -> b.affectedRecords().stream().anyMatch(right -> sameRecord(left, right)));
    }

    private static boolean sameRecord(AffectedRecord a, AffectedRecord b) {
        return a.sourceType() == b.sourceType() && Objects.equals(a.sourceId(), b.sourceId());
    }

    private static int root(int[] parent, int i) {
        int r = i;
        while (parent[r] != r) {
            r = parent[r];
        }
        return r;
    }

    /** Each record once, by type and id, in first-seen order; a later name fills a missing one. */
    private static List<AffectedRecord> distinct(List<AffectedRecord> records) {
        Map<RecordKey, AffectedRecord> seen = new LinkedHashMap<>();
        for (AffectedRecord record : records) {
            seen.merge(
                    new RecordKey(record.sourceType(), record.sourceId()),
                    record,
                    (kept, later) -> kept.name() == null && later.name() != null ? later : kept);
        }
        return List.copyOf(seen.values());
    }

    /** {@code |observed − threshold| / threshold}; null when either is missing or the threshold is zero. */
    private static BigDecimal deviation(Observed observed, Threshold threshold) {
        if (observed == null || observed.value() == null || threshold.value() == null || threshold.value().signum() == 0) {
            return null;
        }
        return observed.value()
                .subtract(threshold.value())
                .abs()
                .divide(threshold.value().abs(), DEVIATION_SCALE, RoundingMode.HALF_UP);
    }

    private static boolean isDeclaredRate(Evidence row) {
        return "EXCHANGE_RATE".equals(row.sourceType().name()) || row.currency() == Currency.USD;
    }

    private static AffectedRecord record(RecordType type, UUID id, String name) {
        String route = switch (type) {
            case ASSET -> ApiPaths.V1 + "/assets/" + id;
            case LIABILITY -> ApiPaths.V1 + "/liabilities/" + id;
            case CASH_FLOW_ENTRY -> ApiPaths.V1 + "/cash-flow-entries/" + id;
            case GOAL -> ApiPaths.V1 + "/goals/" + id;
            case EXCHANGE_RATE -> ApiPaths.V1 + "/settings/exchange-rate";
        };
        return new AffectedRecord(type, id, name, route);
    }

    /** What an action calls a record: its name, or for the rate, which has none, what it is. */
    private static String label(AffectedRecord record) {
        if (record.name() != null) {
            return record.name();
        }
        return record.sourceType() == RecordType.EXCHANGE_RATE ? "the declared USD/VND exchange rate" : null;
    }

    private static List<Finding> withStatus(List<Finding> findings, Status status) {
        return findings.stream().filter(f -> f.status() == status).toList();
    }

    private static Unit unit(String name) {
        return Unit.valueOf(name);
    }

    private static BigDecimal decimal(String text) {
        return text == null ? null : new BigDecimal(text);
    }

    private static Map<ActionFamily, Action> actions() {
        Map<ActionFamily, Action> actions = new EnumMap<>(ActionFamily.class);
        actions.put(ActionFamily.RECONCILE_CASH_WITH_OBLIGATIONS, new Action(
                ActionId.COMPARE_CASH_WITH_UPCOMING_OBLIGATIONS,
                "You may compare your cash and bank balances with the upcoming payments entered for %s, then review the result.",
                "You may compare your cash and bank balances with the upcoming payments you entered, then review the result."));
        actions.put(ActionFamily.REVIEW_PERIOD_SPENDING, new Action(
                ActionId.REVIEW_LARGEST_PERIOD_EXPENSES,
                "You may review the expenses of the period, including %s, and check that the recurring entries still match"
                        + " reality, then review the result.",
                "You may review the largest expenses of the period and check that the recurring entries still match"
                        + " reality, then review the result."));
        actions.put(ActionFamily.REVIEW_DEBT_PAYMENTS, new Action(
                ActionId.CHECK_DEBT_PAYMENTS_AGAINST_INCOME,
                "You may check the debt payments entered for %s against the income of the period, then review the result.",
                "You may check the debt payments you entered against the income of the period, then review the result."));
        actions.put(ActionFamily.RECONCILE_CASH_WITH_RESERVE, new Action(
                ActionId.COMPARE_LIQUID_ASSETS_WITH_RESERVE,
                "You may compare your cash reserve with the assets that are slower to turn into cash, such as %s, then"
                        + " review the result.",
                "You may compare your cash reserve with the reference number of months of essential expenses, then"
                        + " review the result."));
        actions.put(ActionFamily.REVIEW_INCOME_SOURCES, new Action(
                ActionId.REVIEW_INCOME_SOURCE_MIX,
                "You may review how the income of the period is spread across its sources, including %s, then review the"
                        + " result.",
                "You may review how the income of the period is spread across its sources, then review the result."));
        actions.put(ActionFamily.REVIEW_ASSET_CONCENTRATION, new Action(
                ActionId.REVIEW_ASSET_CATEGORY_SHARE,
                "You may review the share of the largest asset category, including %s, alongside your expected liquidity"
                        + " needs, then review the result.",
                "You may review the share of the largest asset category alongside your expected liquidity needs, then"
                        + " review the result."));
        actions.put(ActionFamily.REVIEW_DEBT_LEVEL, new Action(
                ActionId.REVIEW_LIABILITIES_AGAINST_ASSETS,
                "You may review the liabilities you entered, including %s, against your total assets, then review the"
                        + " result.",
                "You may review the liabilities you entered against your total assets, then review the result."));
        actions.put(ActionFamily.REVIEW_GOAL_PLAN, new Action(
                ActionId.CONSIDER_GOAL_PLAN_OPTIONS,
                "You may consider raising the monthly contribution, moving the target date, or adjusting the target"
                        + " amount of %s, then review the result.",
                "You may consider raising the monthly contribution, moving the target date, or adjusting the target"
                        + " amount of the goal, then review the result."));
        actions.put(ActionFamily.REFRESH_RECORD, new Action(
                ActionId.UPDATE_STALE_RECORD,
                "You may update %s so the report reads a current value, then review the result.",
                "You may update the record so the report reads a current value, then review the result."));
        return Map.copyOf(actions);
    }

    private record Placement(int tier, ActionFamily family) {}

    private record Action(ActionId id, String named, String unnamed) {}

    private record RecordKey(RecordType type, UUID id) {}
}
