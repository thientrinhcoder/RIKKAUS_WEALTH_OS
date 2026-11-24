package com.rikkaus.wealth.snapshot.insights.api;

import com.rikkaus.wealth.shared.money.Currency;
import com.rikkaus.wealth.shared.time.TimeConventions;
import com.rikkaus.wealth.snapshot.application.WealthSnapshot.SourceType;
import com.rikkaus.wealth.snapshot.insights.application.SnapshotInsights;
import com.rikkaus.wealth.snapshot.insights.application.SnapshotInsights.Comparison;
import com.rikkaus.wealth.snapshot.insights.application.SnapshotInsights.DateKind;
import com.rikkaus.wealth.snapshot.insights.application.SnapshotInsights.Evidence;
import com.rikkaus.wealth.snapshot.insights.application.SnapshotInsights.Insight;
import com.rikkaus.wealth.snapshot.insights.application.SnapshotInsights.RuleId;
import com.rikkaus.wealth.snapshot.insights.application.SnapshotInsights.Status;
import com.rikkaus.wealth.snapshot.insights.application.SnapshotInsights.Thresholds;
import com.rikkaus.wealth.snapshot.insights.application.SnapshotInsights.UnavailableReason;
import com.rikkaus.wealth.snapshot.insights.application.SnapshotInsights.Unit;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * The six insights, as the API describes them. No Vietnamese text: the client maps every constant to the
 * accepted design's copy.
 */
@Schema(description = "The six wealth snapshot insight rules judged for the signed-in user, with their evidence.")
public record SnapshotInsightsResponse(
        @Schema(description = "Today in timeZone; every age and due day counts from it.", example = "2026-11-24")
                LocalDate asOfDate,
        @Schema(description = "The zone asOfDate is read in.", example = "Asia/Ho_Chi_Minh") String timeZone,
        @Schema(
                        description =
                                "The wealth snapshot's own flag: false when a USD record could not be converted for"
                                        + " want of a declared rate.",
                        example = "true")
                boolean complete,
        SnapshotInsightThresholds thresholds,
        @ArraySchema(
                        arraySchema =
                                @Schema(
                                        description =
                                                "Always six, in the order CATEGORY_CONCENTRATION, DEBT_TO_ASSET_RATIO,"
                                                        + " LOW_LIQUIDITY_SHARE, UPCOMING_LIABILITY_DUE, STALE_VALUATION,"
                                                        + " STALE_EXCHANGE_RATE."),
                        minItems = 6,
                        maxItems = 6)
                List<SnapshotInsight> insights) {

    static SnapshotInsightsResponse of(SnapshotInsights insights) {
        Thresholds thresholds = insights.thresholds();
        return new SnapshotInsightsResponse(
                insights.asOfDate(),
                TimeConventions.DEFAULT_DISPLAY_ZONE.getId(),
                insights.complete(),
                new SnapshotInsightThresholds(
                        thresholds.maxCategoryShare(),
                        thresholds.maxDebtToAssetRatio(),
                        thresholds.maxLowLiquidityShare(),
                        thresholds.dueWindowDays(),
                        thresholds.maxValuationAgeDays(),
                        thresholds.maxExchangeRateAgeDays()),
                insights.insights().stream().map(SnapshotInsightsResponse::insight).toList());
    }

    private static SnapshotInsight insight(Insight insight) {
        return new SnapshotInsight(
                insight.ruleId(),
                insight.status(),
                insight.observed() == null
                        ? null
                        : new SnapshotInsightObserved(
                                insight.observed().value(),
                                insight.observed().unit(),
                                insight.observed().subject(),
                                insight.observed().numeratorVnd(),
                                insight.observed().denominatorVnd(),
                                insight.observed().date()),
                new SnapshotInsightThreshold(
                        insight.threshold().comparison(), insight.threshold().value(), insight.threshold().unit()),
                insight.evidence().stream().map(SnapshotInsightsResponse::evidence).toList(),
                insight.unavailableReason());
    }

    private static SnapshotInsightEvidence evidence(Evidence evidence) {
        return new SnapshotInsightEvidence(
                evidence.sourceType(),
                evidence.sourceId(),
                evidence.name(),
                evidence.category(),
                evidence.liquidity(),
                evidence.dateKind(),
                evidence.date(),
                evidence.ageDays(),
                evidence.daysUntilDue(),
                evidence.overdue(),
                evidence.amount(),
                evidence.currency(),
                evidence.amountVnd(),
                evidence.usdToVndRate());
    }

    @Schema(
            description =
                    "Every system reference threshold in force, configured centrally under"
                            + " rikkaus.snapshot.insights. Returned so a result can be reproduced after the"
                            + " configuration changes.")
    public record SnapshotInsightThresholds(
            @Schema(description = "CATEGORY_CONCENTRATION is ATTENTION above this share.", example = "0.500000")
                    BigDecimal maxCategoryShare,
            @Schema(description = "DEBT_TO_ASSET_RATIO is ATTENTION above this ratio.", example = "0.400000")
                    BigDecimal maxDebtToAssetRatio,
            @Schema(description = "LOW_LIQUIDITY_SHARE is ATTENTION above this share.", example = "0.600000")
                    BigDecimal maxLowLiquidityShare,
            @Schema(
                            description =
                                    "UPCOMING_LIABILITY_DUE is ATTENTION when the nearest due date is at most this"
                                            + " many days after asOfDate, an overdue one included.",
                            example = "30")
                    int dueWindowDays,
            @Schema(description = "STALE_VALUATION is ATTENTION above this age in days.", example = "30")
                    int maxValuationAgeDays,
            @Schema(description = "STALE_EXCHANGE_RATE is ATTENTION above this age in days.", example = "5")
                    int maxExchangeRateAgeDays) {}

    @Schema(description = "One rule's result.")
    public record SnapshotInsight(
            @Schema(description = "The rule. A stable constant the client maps to its copy.", example = "CATEGORY_CONCENTRATION")
                    RuleId ruleId,
            @Schema(
                            description =
                                    "ATTENTION when observed.value compares with threshold.value as"
                                            + " threshold.comparison says; OK when it does not; UNAVAILABLE when"
                                            + " nothing could be observed.",
                            example = "ATTENTION")
                    Status status,
            @Schema(description = "What the rule measured; null exactly when status is UNAVAILABLE.")
                    SnapshotInsightObserved observed,
            SnapshotInsightThreshold threshold,
            @ArraySchema(
                            arraySchema =
                                    @Schema(
                                            description =
                                                    "The source records behind the observation, for links to them."
                                                            + " Shares: the assets of the largest category, or the"
                                                            + " LOW ones. Debt: every liability. Due: every date within"
                                                            + " the window when ATTENTION, else the nearest one."
                                                            + " Valuation: every stale asset oldest first when"
                                                            + " ATTENTION, else the oldest. Rate: the rate, then every"
                                                            + " USD record it converts. Unavailable for want of a rate:"
                                                            + " the USD records that need one."))
                    List<SnapshotInsightEvidence> evidence,
            @Schema(description = "Why nothing could be observed; null unless status is UNAVAILABLE.")
                    UnavailableReason unavailableReason) {}

    @Schema(description = "The measured value and how it was obtained.")
    public record SnapshotInsightObserved(
            @Schema(
                            description =
                                    "A FRACTION with six decimals, HALF_UP (0.624896 is 62.4896 %), or whole DAYS"
                                            + " (negative for an overdue due date).",
                            example = "0.624896")
                    BigDecimal value,
            @Schema(example = "FRACTION") Unit unit,
            @Schema(
                            description = "The largest asset category for CATEGORY_CONCENTRATION; null otherwise.",
                            example = "REAL_ESTATE")
                    String subject,
            @Schema(description = "For a FRACTION, the VND amount divided; null for DAYS.", example = "7500000000.00")
                    BigDecimal numeratorVnd,
            @Schema(description = "For a FRACTION, the VND amount divided by; null for DAYS.", example = "12002000000.00")
                    BigDecimal denominatorVnd,
            @Schema(
                            description =
                                    "For DAYS, the date counted: the nearest due date, the oldest valuation date or the"
                                            + " rate's effective date; null for a FRACTION.")
                    LocalDate date) {}

    @Schema(description = "The system reference threshold the rule was judged against.")
    public record SnapshotInsightThreshold(
            @Schema(
                            description =
                                    "GREATER_THAN: ATTENTION when observed.value > value. AT_MOST: ATTENTION when"
                                            + " observed.value <= value.",
                            example = "GREATER_THAN")
                    Comparison comparison,
            @Schema(example = "0.500000") BigDecimal value,
            @Schema(example = "FRACTION") Unit unit) {}

    @Schema(description = "One source record and the figures the rule read from it.")
    public record SnapshotInsightEvidence(
            @Schema(example = "ASSET") SourceType sourceType,
            @Schema(
                            description =
                                    "GET /api/v1/assets/{id} or /api/v1/liabilities/{id} reads it; null for"
                                            + " EXCHANGE_RATE, which GET /api/v1/settings/exchange-rate reads.",
                            example = "6a1f3c9e-2b47-4d85-9e10-c7b4a2d8f531")
                    UUID sourceId,
            @Schema(description = "null for EXCHANGE_RATE.", example = "Căn hộ Thảo Điền") String name,
            @Schema(description = "The asset or liability category code; null for EXCHANGE_RATE.", example = "REAL_ESTATE")
                    String category,
            @Schema(description = "HIGH, MEDIUM or LOW for an asset; null otherwise.", example = "LOW")
                    String liquidity,
            @Schema(
                            description =
                                    "Which date this row carries: an asset's VALUATION_DATE, a liability's UPDATED_ON"
                                            + " (the day in timeZone it was last saved), NEXT_PAYMENT_DATE,"
                                            + " MATURITY_DATE, or the RATE_EFFECTIVE_DATE.",
                            example = "VALUATION_DATE")
                    DateKind dateKind,
            @Schema(example = "2026-10-07") LocalDate date,
            @Schema(
                            description =
                                    "asOfDate minus date, for VALUATION_DATE, UPDATED_ON and RATE_EFFECTIVE_DATE;"
                                            + " null otherwise.",
                            example = "48")
                    Integer ageDays,
            @Schema(
                            description =
                                    "date minus asOfDate, for NEXT_PAYMENT_DATE and MATURITY_DATE, negative when"
                                            + " overdue; null otherwise.")
                    Integer daysUntilDue,
            @Schema(description = "date is before asOfDate; set on due rows only.") Boolean overdue,
            @Schema(
                            description =
                                    "In the record's currency: an asset's value, a liability's outstanding balance, the"
                                            + " recurring payment for NEXT_PAYMENT_DATE (null when none is stated) or the"
                                            + " balance for MATURITY_DATE; null for EXCHANGE_RATE.",
                            example = "7500000000.00")
                    BigDecimal amount,
            @Schema(description = "null for EXCHANGE_RATE.", example = "VND") Currency currency,
            @Schema(
                            description = "amount in VND; null when amount is null or it is USD without a declared rate.",
                            example = "7500000000.00")
                    BigDecimal amountVnd,
            @Schema(description = "VND for one USD, on the EXCHANGE_RATE row only.")
                    BigDecimal usdToVndRate) {}
}
