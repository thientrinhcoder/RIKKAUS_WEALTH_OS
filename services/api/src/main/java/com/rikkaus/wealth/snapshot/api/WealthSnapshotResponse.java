package com.rikkaus.wealth.snapshot.api;

import com.rikkaus.wealth.shared.money.Currency;
import com.rikkaus.wealth.shared.money.ExchangeRate;
import com.rikkaus.wealth.shared.time.TimeConventions;
import com.rikkaus.wealth.snapshot.application.WealthSnapshot;
import com.rikkaus.wealth.snapshot.application.WealthSnapshot.Aggregate;
import com.rikkaus.wealth.snapshot.application.WealthSnapshot.AssetLine;
import com.rikkaus.wealth.snapshot.application.WealthSnapshot.DueKind;
import com.rikkaus.wealth.snapshot.application.WealthSnapshot.Group;
import com.rikkaus.wealth.snapshot.application.WealthSnapshot.SourceType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * The wealth snapshot, as the API describes it.
 *
 * <p>Every VND amount carries two decimals on the wire, like every amount in the catalog. A VND figure
 * that a missing exchange rate makes unknowable is {@code null}, never {@code 0.00}.
 */
@Schema(description = "The signed-in user's assets, liabilities and net worth in VND, with every line they are summed from.")
public record WealthSnapshotResponse(
        @Schema(description = "Today in timeZone; the day the due-date window starts.", example = "2026-11-22")
                LocalDate asOfDate,
        @Schema(description = "The zone asOfDate is read in.", example = "Asia/Ho_Chi_Minh") String timeZone,
        @Schema(description = "The reporting currency. Always VND.", example = "VND") Currency currency,
        @Schema(description = "The declared USD/VND rate every USD line is converted with; null when none is declared.")
                WealthSnapshotExchangeRate exchangeRate,
        @Schema(
                        description =
                                "False when at least one USD asset or liability could not be converted because no"
                                        + " rate is declared. The affected totals are then null.",
                        example = "true")
                boolean complete,
        WealthSnapshotKpis kpis,
        @ArraySchema(
                        arraySchema =
                                @Schema(
                                        description =
                                                "One row per category that has an asset, largest knownAmountVnd first,"
                                                        + " then by category. Not grouped into an other slice."))
                List<WealthSnapshotCategory> assetsByCategory,
        @ArraySchema(
                        arraySchema =
                                @Schema(
                                        description =
                                                "One row per liquidity level that has an asset, in the order HIGH,"
                                                        + " MEDIUM, LOW."))
                List<WealthSnapshotLiquidity> assetsByLiquidity,
        WealthSnapshotLargestAssets largestAssets,
        WealthSnapshotUpcomingDues upcomingLiabilityDues,
        @ArraySchema(
                        arraySchema =
                                @Schema(
                                        description =
                                                "The date every source contributes, for the freshness screen. Newest"
                                                        + " first; on one day ASSET, LIABILITY, EXCHANGE_RATE, then by"
                                                        + " Vietnamese name and id."))
                List<WealthSnapshotSourceDate> sourceDates,
        @ArraySchema(
                        arraySchema =
                                @Schema(
                                        description =
                                                "Every asset, largest valueVnd first (unconverted last), then by"
                                                        + " Vietnamese name, then id. Every asset total sums these."))
                List<WealthSnapshotAsset> assets,
        @ArraySchema(
                        arraySchema =
                                @Schema(
                                        description =
                                                "Every liability, largest outstandingBalanceVnd first (unconverted"
                                                        + " last), then by Vietnamese name, then id."))
                List<WealthSnapshotLiability> liabilities) {

    static WealthSnapshotResponse of(WealthSnapshot snapshot) {
        WealthSnapshot.Kpis kpis = snapshot.kpis();
        return new WealthSnapshotResponse(
                snapshot.asOfDate(),
                TimeConventions.DEFAULT_DISPLAY_ZONE.getId(),
                Currency.VND,
                snapshot.exchangeRate().map(WealthSnapshotResponse::rate).orElse(null),
                snapshot.complete(),
                new WealthSnapshotKpis(
                        total(kpis.totalAssets()),
                        total(kpis.totalLiabilities()),
                        kpis.netWorth(),
                        kpis.debtToAssetRatio(),
                        total(kpis.liquidAssets()),
                        kpis.lastDataUpdate()),
                snapshot.assetsByCategory().stream()
                        .map(group -> new WealthSnapshotCategory(
                                group.key(),
                                group.aggregate().amountVnd(),
                                group.aggregate().knownAmountVnd(),
                                group.aggregate().recordCount(),
                                group.aggregate().excludedRecordCount()))
                        .toList(),
                snapshot.assetsByLiquidity().stream().map(WealthSnapshotResponse::liquidity).toList(),
                new WealthSnapshotLargestAssets(
                        snapshot.largestAssets().items().stream()
                                .map(line -> new WealthSnapshotRankedAsset(
                                        line.id(),
                                        line.name(),
                                        line.category(),
                                        line.liquidity(),
                                        line.valueVnd(),
                                        line.valuationDate()))
                                .toList(),
                        snapshot.largestAssets().excludedRecordCount()),
                new WealthSnapshotUpcomingDues(
                        snapshot.upcomingDues().windowDays(),
                        snapshot.upcomingDues().windowEnd(),
                        snapshot.upcomingDues().items().stream()
                                .map(due -> new WealthSnapshotDue(
                                        due.liabilityId(),
                                        due.name(),
                                        due.category(),
                                        due.dueKind(),
                                        due.dueDate(),
                                        due.overdue(),
                                        due.amount(),
                                        due.currency(),
                                        due.amountVnd()))
                                .toList()),
                snapshot.sourceDates().stream()
                        .map(source -> new WealthSnapshotSourceDate(
                                source.sourceType(), source.sourceId(), source.name(), source.date()))
                        .toList(),
                snapshot.assets().stream().map(WealthSnapshotResponse::asset).toList(),
                snapshot.liabilities().stream()
                        .map(line -> new WealthSnapshotLiability(
                                line.id(),
                                line.name(),
                                line.category(),
                                line.outstandingBalance(),
                                line.currency(),
                                line.outstandingBalanceVnd(),
                                line.nextPaymentDate(),
                                line.maturityDate(),
                                line.recurringPaymentAmount(),
                                line.recurringPaymentFrequency(),
                                line.updatedOn()))
                        .toList());
    }

    private static WealthSnapshotExchangeRate rate(ExchangeRate rate) {
        return new WealthSnapshotExchangeRate(rate.rate(), rate.effectiveDate());
    }

    private static WealthSnapshotTotal total(Aggregate aggregate) {
        return new WealthSnapshotTotal(
                aggregate.amountVnd(),
                aggregate.knownAmountVnd(),
                aggregate.recordCount(),
                aggregate.excludedRecordCount());
    }

    private static WealthSnapshotLiquidity liquidity(Group group) {
        return new WealthSnapshotLiquidity(
                group.key(),
                group.aggregate().amountVnd(),
                group.aggregate().knownAmountVnd(),
                group.aggregate().recordCount(),
                group.aggregate().excludedRecordCount());
    }

    private static WealthSnapshotAsset asset(AssetLine line) {
        return new WealthSnapshotAsset(
                line.id(),
                line.name(),
                line.category(),
                line.liquidity(),
                line.value(),
                line.currency(),
                line.valueVnd(),
                line.valuationDate());
    }

    @Schema(description = "The declared rate, as used for every conversion in this snapshot.")
    public record WealthSnapshotExchangeRate(
            @Schema(description = "VND for one USD.", example = "25100.000000") BigDecimal usdToVndRate,
            @Schema(description = "The date the user states the rate took effect.", example = "2026-09-12")
                    LocalDate effectiveDate) {}

    @Schema(description = "The six KPIs of the dashboard.")
    public record WealthSnapshotKpis(
            @Schema(description = "Every asset's latest value, converted to VND.") WealthSnapshotTotal totalAssets,
            @Schema(description = "Every liability's outstanding balance, converted to VND.")
                    WealthSnapshotTotal totalLiabilities,
            @Schema(
                            description =
                                    "totalAssets minus totalLiabilities; may be negative. Null when either amountVnd is"
                                            + " null.",
                            example = "10470000000.00")
                    BigDecimal netWorth,
            @Schema(
                            description =
                                    "totalLiabilities / totalAssets as a fraction, six decimals, HALF_UP (0.127645 is"
                                            + " 12.7645 %). Null when total assets are zero or either amountVnd is null.",
                            example = "0.127645")
                    BigDecimal debtToAssetRatio,
            @Schema(description = "Assets whose liquidity is HIGH, converted to VND.") WealthSnapshotTotal liquidAssets,
            @Schema(
                            description = "The newest date in sourceDates; null when there is no source.",
                            example = "2026-09-19")
                    LocalDate lastDataUpdate) {}

    @Schema(
            description =
                    "A sum over records that may include unconverted USD ones. amountVnd is null when any record is"
                            + " excluded; knownAmountVnd is always the sum of the converted records.")
    public record WealthSnapshotTotal(
            @Schema(description = "The total, or null when excludedRecordCount > 0.", example = "12002000000.00")
                    BigDecimal amountVnd,
            @Schema(description = "The sum of the records that were converted.", example = "12002000000.00")
                    BigDecimal knownAmountVnd,
            @Schema(description = "Every record in the set.", example = "11") int recordCount,
            @Schema(description = "Records left out for want of a declared rate.", example = "0") int excludedRecordCount) {}

    @Schema(description = "One asset category and the sum of its assets.")
    public record WealthSnapshotCategory(
            @Schema(description = "The asset category code.", example = "REAL_ESTATE") String category,
            @Schema(description = "The sum, or null when a record is excluded.", example = "6100000000.00")
                    BigDecimal amountVnd,
            @Schema(description = "The sum of the converted records.", example = "6100000000.00")
                    BigDecimal knownAmountVnd,
            @Schema(example = "1") int recordCount,
            @Schema(example = "0") int excludedRecordCount) {}

    @Schema(description = "One liquidity level and the sum of its assets.")
    public record WealthSnapshotLiquidity(
            @Schema(description = "HIGH, MEDIUM or LOW.", example = "HIGH") String liquidity,
            @Schema(description = "The sum, or null when a record is excluded.", example = "3180000000.00")
                    BigDecimal amountVnd,
            @Schema(description = "The sum of the converted records.", example = "3180000000.00")
                    BigDecimal knownAmountVnd,
            @Schema(example = "4") int recordCount,
            @Schema(example = "0") int excludedRecordCount) {}

    @Schema(description = "The five largest assets by VND value.")
    public record WealthSnapshotLargestAssets(
            @ArraySchema(
                            arraySchema =
                                    @Schema(
                                            description =
                                                    "At most five, in the order of assets: largest valueVnd, then"
                                                            + " Vietnamese name, then id."),
                            maxItems = 5)
                    List<WealthSnapshotRankedAsset> items,
            @Schema(description = "Assets that could not be ranked because they could not be converted.", example = "0")
                    int excludedRecordCount) {}

    @Schema(description = "A ranked asset; the same figures as its row in assets.")
    public record WealthSnapshotRankedAsset(
            @Schema(description = "GET /api/v1/assets/{id} reads it.", example = "6a1f3c9e-2b47-4d85-9e10-c7b4a2d8f531")
                    UUID assetId,
            @Schema(example = "Căn hộ Thảo Điền") String name,
            @Schema(example = "REAL_ESTATE") String category,
            @Schema(example = "LOW") String liquidity,
            @Schema(example = "6100000000.00") BigDecimal valueVnd,
            @Schema(example = "2026-08-02") LocalDate valuationDate) {}

    @Schema(description = "Liability dates on or before asOfDate + windowDays, past ones included.")
    public record WealthSnapshotUpcomingDues(
            @Schema(description = "How many days ahead of asOfDate count as upcoming.", example = "30") int windowDays,
            @Schema(description = "asOfDate + windowDays, included.", example = "2026-12-22") LocalDate windowEnd,
            @ArraySchema(
                            arraySchema =
                                    @Schema(
                                            description =
                                                    "By dueDate, PAYMENT before MATURITY, then Vietnamese name, then"
                                                            + " liabilityId."))
                    List<WealthSnapshotDue> items) {}

    @Schema(description = "One date a liability falls due.")
    public record WealthSnapshotDue(
            @Schema(description = "GET /api/v1/liabilities/{id} reads it.", example = "9c2e7b14-5d38-4a61-b0f9-3e8d1a6c2b57")
                    UUID liabilityId,
            @Schema(example = "Khoản vay mua nhà") String name,
            @Schema(example = "BANK_LOAN") String category,
            @Schema(description = "PAYMENT is nextPaymentDate; MATURITY is maturityDate.", example = "PAYMENT")
                    DueKind dueKind,
            @Schema(example = "2026-12-20") LocalDate dueDate,
            @Schema(description = "dueDate is before asOfDate.", example = "false") boolean overdue,
            @Schema(
                            description =
                                    "The recurring payment for PAYMENT, the outstanding balance for MATURITY, in the"
                                            + " liability's currency. Null when no recurring payment is stated.",
                            example = "18750000.00")
                    BigDecimal amount,
            @Schema(example = "VND") Currency currency,
            @Schema(
                            description = "amount in VND; null when amount is null or it is USD without a declared rate.",
                            example = "18750000.00")
                    BigDecimal amountVnd) {}

    @Schema(description = "The date one source contributes to the snapshot.")
    public record WealthSnapshotSourceDate(
            @Schema(example = "ASSET") SourceType sourceType,
            @Schema(
                            description = "The asset or liability; null for EXCHANGE_RATE.",
                            example = "6a1f3c9e-2b47-4d85-9e10-c7b4a2d8f531")
                    UUID sourceId,
            @Schema(description = "Its name; null for EXCHANGE_RATE.", example = "Danh mục cổ phiếu HOSE")
                    String name,
            @Schema(
                            description =
                                    "An asset's valuationDate, the day (in timeZone) a liability was last saved, or the"
                                            + " rate's effectiveDate. The rate is listed only when it converted a line.",
                            example = "2026-09-19")
                    LocalDate date) {}

    @Schema(description = "An asset as the snapshot counts it.")
    public record WealthSnapshotAsset(
            @Schema(example = "4b8d2e61-7a03-4f59-8c1e-d6a9b3f0e742") UUID id,
            @Schema(example = "Tài khoản USD") String name,
            @Schema(example = "BANK_ACCOUNT") String category,
            @Schema(example = "HIGH") String liquidity,
            @Schema(description = "The current value in the asset's own currency.", example = "20000.00") BigDecimal value,
            @Schema(example = "USD") Currency currency,
            @Schema(
                            description = "value in VND; null when USD and no rate is declared.",
                            example = "502000000.00")
                    BigDecimal valueVnd,
            @Schema(example = "2026-09-12") LocalDate valuationDate) {}

    @Schema(description = "A liability as the snapshot counts it.")
    public record WealthSnapshotLiability(
            @Schema(example = "9c2e7b14-5d38-4a61-b0f9-3e8d1a6c2b57") UUID id,
            @Schema(example = "Khoản vay mua nhà") String name,
            @Schema(example = "BANK_LOAN") String category,
            @Schema(description = "In the liability's own currency.", example = "1500000000.00")
                    BigDecimal outstandingBalance,
            @Schema(example = "VND") Currency currency,
            @Schema(
                            description = "outstandingBalance in VND; null when USD and no rate is declared.",
                            example = "1500000000.00")
                    BigDecimal outstandingBalanceVnd,
            @Schema(example = "2026-12-20") LocalDate nextPaymentDate,
            @Schema(example = "2041-09-20") LocalDate maturityDate,
            @Schema(example = "18750000.00") BigDecimal recurringPaymentAmount,
            @Schema(example = "MONTHLY") String recurringPaymentFrequency,
            @Schema(description = "The day, in timeZone, the liability was last saved.", example = "2026-11-22")
                    LocalDate updatedOn) {}
}
