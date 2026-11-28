package com.rikkaus.wealth.advisor.api;

import com.rikkaus.wealth.advisor.application.AdvisorReport;
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
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * The basic advisor report as the API describes it. No Vietnamese text: the client maps every constant
 * to the accepted design's copy, and the English {@code action} and {@code disclaimer} sentences are the
 * reference wording for that copy.
 */
@Schema(
        description =
                "The signed-in user's rule-based advisor report: every finding of the wealth-snapshot insights, the"
                        + " financial-health rules and the goal projections, grouped, and the suggestions derived from"
                        + " the findings needing attention.")
public record AdvisorReportResponse(
        @Schema(description = "Today in timeZone, as GET /api/v1/financial-health states it.", example = "2026-11-28")
                LocalDate asOfDate,
        @Schema(example = "Asia/Ho_Chi_Minh") String timeZone,
        @Schema(description = "The month the FINANCIAL_HEALTH findings read, YYYY-MM.", example = "2026-11") String month,
        @Schema(
                        description =
                                "The report-level note, always shown and never dismissible: informational, based on"
                                        + " user-entered data and system reference thresholds, not financial advice,"
                                        + " no outcome guaranteed.")
                String disclaimer,
        @Schema(description = "How many of the first suggestions carry priority: true.", example = "3")
                int priorityMarkerCount,
        @ArraySchema(
                        arraySchema =
                                @Schema(
                                        description =
                                                "Always three, in the order WEALTH_SNAPSHOT, FINANCIAL_HEALTH,"
                                                        + " GOAL_PROJECTION."),
                        minItems = 3,
                        maxItems = 3)
                List<AdvisorSource> sources,
        AdvisorUnevaluated unevaluated,
        @ArraySchema(
                        arraySchema =
                                @Schema(
                                        description =
                                                "Findings judged and within their threshold: a snapshot or health rule"
                                                        + " OK, a goal ON_TRACK. Source order."))
                List<AdvisorFinding> positive,
        @ArraySchema(
                        arraySchema =
                                @Schema(
                                        description =
                                                "Findings judged and beyond their threshold: a snapshot or health rule"
                                                        + " ATTENTION, a goal AT_RISK or BEHIND. Source order. Merging"
                                                        + " suggestions never removes a finding from here."))
                List<AdvisorFinding> attention,
        @ArraySchema(
                        arraySchema =
                                @Schema(
                                        description =
                                                "Derived from attention only, in rank order: tier ascending, then"
                                                        + " dueDate nearest first (null last), then relativeDeviation"
                                                        + " descending (null last), then suggestionId ascending."))
                List<AdvisorSuggestion> suggestions) {

    static AdvisorReportResponse of(AdvisorReport report) {
        return new AdvisorReportResponse(
                report.asOfDate(),
                report.timeZone(),
                report.month(),
                report.disclaimer(),
                report.priorityMarkerCount(),
                report.sources().stream().map(AdvisorReportResponse::source).toList(),
                new AdvisorUnevaluated(
                        report.unevaluated().size(),
                        report.unevaluated().stream().map(AdvisorReportResponse::finding).toList()),
                report.positive().stream().map(AdvisorReportResponse::finding).toList(),
                report.attention().stream().map(AdvisorReportResponse::finding).toList(),
                report.suggestions().stream().map(AdvisorReportResponse::suggestion).toList());
    }

    private static AdvisorSource source(SourceSummary source) {
        return new AdvisorSource(source.source(), source.route(), source.findingCount());
    }

    private static AdvisorFinding finding(Finding f) {
        Observed o = f.observed();
        Threshold t = f.threshold();
        return new AdvisorFinding(
                f.findingId(),
                f.source(),
                f.ruleId(),
                f.status(),
                f.tier(),
                f.actionFamily(),
                o == null
                        ? null
                        : new AdvisorObserved(o.value(), o.unit(), o.subject(), o.date(), o.numeratorVnd(), o.denominatorVnd()),
                new AdvisorThreshold(t.comparison(), t.value(), t.unit(), t.property()),
                f.affectedRecords().stream().map(AdvisorReportResponse::record).toList(),
                f.dueDate(),
                f.relativeDeviation(),
                f.noteId(),
                f.reason());
    }

    private static AdvisorSuggestion suggestion(Suggestion s) {
        return new AdvisorSuggestion(
                s.suggestionId(),
                s.rank(),
                s.priority(),
                s.tier(),
                s.actionFamily(),
                s.actionId(),
                s.action(),
                s.findingIds(),
                s.affectedRecords().stream().map(AdvisorReportResponse::record).toList(),
                s.dueDate(),
                s.relativeDeviation(),
                s.noteIds());
    }

    private static AdvisorAffectedRecord record(AffectedRecord r) {
        return new AdvisorAffectedRecord(r.sourceType(), r.sourceId(), r.name(), r.route());
    }

    @Schema(description = "One upstream report the advisor read.")
    public record AdvisorSource(
            @Schema(example = "FINANCIAL_HEALTH") Source source,
            @Schema(
                            description = "The route that publishes the same findings for the same user and month.",
                            example = "/api/v1/financial-health?month=2026-11")
                    String route,
            @Schema(
                            description = "Findings taken from it: 6 for each rule report, one per goal for goals.",
                            example = "6")
                    int findingCount) {}

    @Schema(
            description =
                    "The \"not enough data\" block shown above the three groups: every snapshot insight UNAVAILABLE,"
                            + " every health rule NOT_EVALUATED and every goal without a status. None gives rise to a"
                            + " suggestion.")
    public record AdvisorUnevaluated(
            @Schema(example = "3") int count, List<AdvisorFinding> findings) {}

    @Schema(description = "One upstream result, with what it observed, the threshold it was judged against and its records.")
    public record AdvisorFinding(
            @Schema(
                            description =
                                    "The rule-code registry: WEALTH_SNAPSHOT:<ruleId>, FINANCIAL_HEALTH:<ruleId>, or"
                                            + " GOAL_PROJECTION:GOAL_CONTRIBUTION_COVERAGE:<goalId>.",
                            example = "WEALTH_SNAPSHOT:CATEGORY_CONCENTRATION")
                    String findingId,
            @Schema(example = "WEALTH_SNAPSHOT") Source source,
            @Schema(
                            description =
                                    "The upstream rule constant of GET /api/v1/wealth-snapshot/insights or"
                                            + " GET /api/v1/financial-health; GOAL_CONTRIBUTION_COVERAGE for a goal.",
                            example = "CATEGORY_CONCENTRATION")
                    String ruleId,
            @Schema(
                            description =
                                    "OK, ATTENTION or NOT_EVALUATED: snapshot UNAVAILABLE and a goal without a status"
                                            + " read as NOT_EVALUATED, ON_TRACK as OK, AT_RISK and BEHIND as ATTENTION.",
                            example = "ATTENTION")
                    Status status,
            @Schema(
                            description =
                                    "Priority tier 1 to 5 when ATTENTION, null otherwise: 1 upcoming obligations and"
                                            + " liquidity, 2 the period's cash flow, 3 reserve and structure, 4 goals,"
                                            + " 5 data freshness.",
                            example = "3")
                    Integer tier,
            @Schema(description = "Set exactly when ATTENTION.", example = "REVIEW_ASSET_CONCENTRATION")
                    ActionFamily actionFamily,
            @Schema(description = "The upstream observation, unchanged; null exactly when NOT_EVALUATED.")
                    AdvisorObserved observed,
            AdvisorThreshold threshold,
            @ArraySchema(
                            arraySchema =
                                    @Schema(
                                            description =
                                                    "The upstream evidence rows, each record once by (sourceType,"
                                                            + " sourceId), in upstream order."))
                    List<AdvisorAffectedRecord> affectedRecords,
            @Schema(
                            description =
                                    "UPCOMING_LIABILITY_DUE: the nearest due date. LIQUIDITY_SHORT_FOR_UPCOMING_OBLIGATIONS:"
                                            + " the earliest payment or maturity in the window. A goal: its target"
                                            + " date. null otherwise.",
                            example = "2026-11-30")
                    LocalDate dueDate,
            @Schema(
                            description =
                                    "|observed.value - threshold.value| / threshold.value, six decimals HALF_UP; for a"
                                            + " goal (100 - coverage) / 100, negative when the plan covers more than"
                                            + " required. null when the threshold is zero or a value is missing.",
                            example = "0.249792")
                    BigDecimal relativeDeviation,
            @Schema(
                            description =
                                    "The item-level note, the data limit of this finding: USER_ASSUMED_RETURN for a"
                                            + " goal, FORECAST_FROM_ENTERED_SCHEDULES for FORECAST_CASH_NEGATIVE,"
                                            + " USER_DECLARED_EXCHANGE_RATE when a record is the rate or in USD, else"
                                            + " MANUAL_ENTRY_NOT_RECONCILED.",
                            example = "MANUAL_ENTRY_NOT_RECONCILED")
                    NoteId noteId,
            @Schema(
                            description =
                                    "Set exactly when NOT_EVALUATED: the upstream unavailableReason or"
                                            + " notEvaluatedReason, or a goal's first unavailableReasons entry"
                                            + " (TIMELINE_<timeline> when it has none).",
                            example = "NO_LIABILITIES")
                    String reason) {}

    @Schema(description = "What the upstream rule measured.")
    public record AdvisorObserved(
            @Schema(description = "The upstream observed.value; for a goal contributionCoveragePercent.", example = "0.624896")
                    BigDecimal value,
            @Schema(example = "FRACTION") Unit unit,
            @Schema(description = "The upstream subject, such as the largest category; null otherwise.", example = "REAL_ESTATE")
                    String subject,
            @Schema(description = "The upstream observed date; null otherwise.") LocalDate date,
            @Schema(
                            description =
                                    "Snapshot: the upstream numerator. Health: the numerator of the indicator in"
                                            + " dependsOn. Goal: the planned monthly contribution. null otherwise.",
                            example = "7500000000.00")
                    BigDecimal numeratorVnd,
            @Schema(
                            description =
                                    "Snapshot: the upstream denominator. Health: the denominator of the indicator in"
                                            + " dependsOn. Goal: the required monthly contribution. null otherwise.",
                            example = "12002000000.00")
                    BigDecimal denominatorVnd) {}

    @Schema(description = "The upstream threshold, unchanged; the advisor defines none.")
    public record AdvisorThreshold(
            @Schema(
                            description =
                                    "GREATER_THAN: ATTENTION when observed > value. LESS_THAN: when <. AT_MOST: when"
                                            + " <=.",
                            example = "GREATER_THAN")
                    Comparison comparison,
            @Schema(description = "null only when the upstream threshold is itself an unconvertible sum.", example = "0.500000")
                    BigDecimal value,
            @Schema(example = "FRACTION") Unit unit,
            @Schema(
                            description = "The fully qualified configuration property the value comes from, when there is one.",
                            example = "rikkaus.snapshot.insights.max-category-share")
                    String property) {}

    @Schema(description = "A record behind a finding, openable from the report.")
    public record AdvisorAffectedRecord(
            @Schema(example = "ASSET") RecordType sourceType,
            @Schema(description = "null for EXCHANGE_RATE, which has no identifier.") UUID sourceId,
            @Schema(
                            description =
                                    "The record's name as the user entered it; null when the upstream evidence carries"
                                            + " none (financial-health rows, the exchange rate).",
                            example = "Căn hộ Thảo Điền")
                    String name,
            @Schema(
                            description = "The route that reads the record.",
                            example = "/api/v1/assets/6a1f3c9e-2b47-4d85-9e10-c7b4a2d8f531")
                    String route) {}

    @Schema(description = "One action to consider, from one or more findings of one family that share a record.")
    public record AdvisorSuggestion(
            @Schema(
                            description = "The findingIds joined by +.",
                            example = "WEALTH_SNAPSHOT:UPCOMING_LIABILITY_DUE+FINANCIAL_HEALTH:LIQUIDITY_SHORT_FOR_UPCOMING_OBLIGATIONS")
                    String suggestionId,
            @Schema(description = "1-based.", example = "1") int rank,
            @Schema(description = "true for the first priorityMarkerCount suggestions.", example = "true") boolean priority,
            @Schema(description = "The smallest tier of its findings.", example = "1") int tier,
            @Schema(example = "RECONCILE_CASH_WITH_OBLIGATIONS") ActionFamily actionFamily,
            @Schema(
                            description = "One per actionFamily; the constant the client maps to the approved action copy.",
                            example = "COMPARE_CASH_WITH_UPCOMING_OBLIGATIONS")
                    ActionId actionId,
            @Schema(
                            description =
                                    "Reference English wording: one step, in the frames \"You may review / compare /"
                                            + " check / update / consider ... then review the result.\", naming the first"
                                            + " affected record when it has a name. Never a promise, a prediction, an"
                                            + " order, urgency, or a product or institution.")
                    String action,
            @ArraySchema(
                            arraySchema =
                                    @Schema(
                                            description =
                                                    "One, or several when findings of the same actionFamily share a"
                                                            + " record (transitively); each also stays in attention."),
                            minItems = 1)
                    List<String> findingIds,
            @ArraySchema(arraySchema = @Schema(description = "The union of the findings' records, each once."))
                    List<AdvisorAffectedRecord> affectedRecords,
            @Schema(description = "The nearest of the findings' due dates.") LocalDate dueDate,
            @Schema(description = "The largest of the findings' relative deviations.") BigDecimal relativeDeviation,
            @ArraySchema(arraySchema = @Schema(description = "The findings' notes, each once, in finding order."))
                    List<NoteId> noteIds) {}
}
