package com.rikkaus.wealth.advisor.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * The basic advisor report: every finding of the three upstream reports, grouped, and the suggestions the
 * findings needing attention give rise to.
 *
 * <p>Nothing here is a new figure. Each finding repeats the observation and threshold its upstream report
 * published, and names the records behind it, so a suggestion is traceable to the metric, the rule, the
 * records and the note it rests on.
 *
 * @param asOfDate today in {@code timeZone}, as the financial-health report states it
 * @param month the month the financial-health findings read
 * @param priorityMarkerCount how many of the first suggestions carry the priority marker
 * @param sources one entry per upstream report, in a fixed order
 * @param unevaluated the findings that could not be judged, with why
 * @param positive the findings judged and within their threshold
 * @param attention the findings judged and beyond their threshold
 * @param suggestions derived from {@code attention} only, in rank order
 */
public record AdvisorReport(
        LocalDate asOfDate,
        String timeZone,
        String month,
        String disclaimer,
        int priorityMarkerCount,
        List<SourceSummary> sources,
        List<Finding> unevaluated,
        List<Finding> positive,
        List<Finding> attention,
        List<Suggestion> suggestions) {

    /** The upstream report a finding comes from, in the order the report lists them. */
    public enum Source {
        WEALTH_SNAPSHOT,
        FINANCIAL_HEALTH,
        GOAL_PROJECTION
    }

    public enum Status {
        /** Judged and within the threshold. */
        OK,
        /** Judged and beyond the threshold, or a goal at risk or behind. */
        ATTENTION,
        /** Not judged: an input is missing; {@code reason} says which. */
        NOT_EVALUATED
    }

    /** What kind of step a suggestion is about; two findings merge only within one family. */
    public enum ActionFamily {
        RECONCILE_CASH_WITH_OBLIGATIONS,
        REVIEW_PERIOD_SPENDING,
        REVIEW_DEBT_PAYMENTS,
        RECONCILE_CASH_WITH_RESERVE,
        REVIEW_INCOME_SOURCES,
        REVIEW_ASSET_CONCENTRATION,
        REVIEW_DEBT_LEVEL,
        REVIEW_GOAL_PLAN,
        REFRESH_RECORD
    }

    /** One per action family: the constant the client maps to the approved action copy. */
    public enum ActionId {
        COMPARE_CASH_WITH_UPCOMING_OBLIGATIONS,
        REVIEW_LARGEST_PERIOD_EXPENSES,
        CHECK_DEBT_PAYMENTS_AGAINST_INCOME,
        COMPARE_LIQUID_ASSETS_WITH_RESERVE,
        REVIEW_INCOME_SOURCE_MIX,
        REVIEW_ASSET_CATEGORY_SHARE,
        REVIEW_LIABILITIES_AGAINST_ASSETS,
        CONSIDER_GOAL_PLAN_OPTIONS,
        UPDATE_STALE_RECORD
    }

    /** The item-level note: the data limit of the finding itself. */
    public enum NoteId {
        /** The figures are what the user entered; the system confirms no transaction. */
        MANUAL_ENTRY_NOT_RECONCILED,
        /** A USD amount or the USD/VND rate is the user's own declaration. */
        USER_DECLARED_EXCHANGE_RATE,
        /** A goal projection rests on the expected return the user entered. */
        USER_ASSUMED_RETURN,
        /** The forecast projects only the schedules the user entered. */
        FORECAST_FROM_ENTERED_SCHEDULES
    }

    public enum RecordType {
        ASSET,
        LIABILITY,
        CASH_FLOW_ENTRY,
        EXCHANGE_RATE,
        GOAL
    }

    public enum Unit {
        FRACTION,
        DAYS,
        MONTHS,
        VND,
        PERCENT
    }

    public enum Comparison {
        /** {@code ATTENTION} when the observed value is strictly greater than the threshold. */
        GREATER_THAN,
        /** {@code ATTENTION} when the observed value is strictly less than the threshold. */
        LESS_THAN,
        /** {@code ATTENTION} when the observed value is less than or equal to the threshold. */
        AT_MOST
    }

    /**
     * @param route the upstream route that publishes the same findings for the same user
     * @param findingCount how many findings this report took from it
     */
    public record SourceSummary(Source source, String route, int findingCount) {}

    /**
     * One upstream result.
     *
     * @param findingId {@code <source>:<ruleId>}, or for a goal {@code GOAL_PROJECTION:GOAL_CONTRIBUTION_COVERAGE:<goalId>}
     * @param ruleId the upstream rule constant, {@code GOAL_CONTRIBUTION_COVERAGE} for a goal
     * @param tier 1 to 5, set exactly when {@code ATTENTION}
     * @param actionFamily set exactly when {@code ATTENTION}
     * @param observed null exactly when {@code NOT_EVALUATED}
     * @param dueDate the nearest due date of a due-type finding, the target date of a goal, else null
     * @param relativeDeviation how far the observation is from the threshold, relative to the threshold
     * @param reason the upstream reason, set exactly when {@code NOT_EVALUATED}
     */
    public record Finding(
            String findingId,
            Source source,
            String ruleId,
            Status status,
            Integer tier,
            ActionFamily actionFamily,
            Observed observed,
            Threshold threshold,
            List<AffectedRecord> affectedRecords,
            LocalDate dueDate,
            BigDecimal relativeDeviation,
            NoteId noteId,
            String reason) {}

    public record Observed(
            BigDecimal value,
            Unit unit,
            String subject,
            LocalDate date,
            BigDecimal numeratorVnd,
            BigDecimal denominatorVnd) {}

    /** @param property the fully qualified configuration property the value comes from, when there is one */
    public record Threshold(Comparison comparison, BigDecimal value, Unit unit, String property) {}

    /** @param name null when the upstream evidence carries none */
    public record AffectedRecord(RecordType sourceType, UUID sourceId, String name, String route) {}

    /**
     * One action to consider, from one or more findings of one family that share a record.
     *
     * @param suggestionId the finding identifiers joined by {@code +}, in the order {@code findingIds} lists them
     * @param priority true for the first {@code priorityMarkerCount} suggestions
     * @param tier the smallest tier among the findings
     * @param action one fixed English sentence per {@code actionId}, naming the first record when it has a name
     * @param dueDate the nearest of the findings' due dates
     * @param relativeDeviation the largest of the findings' deviations
     */
    public record Suggestion(
            String suggestionId,
            int rank,
            boolean priority,
            int tier,
            ActionFamily actionFamily,
            ActionId actionId,
            String action,
            List<String> findingIds,
            List<AffectedRecord> affectedRecords,
            LocalDate dueDate,
            BigDecimal relativeDeviation,
            List<NoteId> noteIds) {}
}
