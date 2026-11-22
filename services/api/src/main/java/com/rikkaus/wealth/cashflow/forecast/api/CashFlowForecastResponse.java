package com.rikkaus.wealth.cashflow.forecast.api;

import com.rikkaus.wealth.cashflow.forecast.application.CashFlowForecast;
import com.rikkaus.wealth.cashflow.forecast.application.CashFlowForecast.Direction;
import com.rikkaus.wealth.cashflow.forecast.application.CashFlowForecast.ExclusionReason;
import com.rikkaus.wealth.cashflow.forecast.application.CashFlowForecast.MinimumBalanceStatus;
import com.rikkaus.wealth.cashflow.forecast.application.CashFlowForecast.SourceType;
import com.rikkaus.wealth.cashflow.forecast.application.CashFlowForecastService;
import com.rikkaus.wealth.cashflow.forecast.application.ForecastHorizon;
import com.rikkaus.wealth.shared.money.Currency;
import com.rikkaus.wealth.shared.money.ExchangeRate;
import com.rikkaus.wealth.shared.time.TimeConventions;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * The three-month forecast, as the API describes it.
 *
 * <p>Every VND amount carries two decimals on the wire, like every amount in the catalog, although the
 * forecast rounds each one to whole đồng first. Months are {@code YYYY-MM} text rather than a
 * structured type, because that is how the request names them.
 */
@Schema(description = "A three-month projection of the signed-in user's cash from the records they can see.")
public record CashFlowForecastResponse(
        CashFlowForecastHorizon horizon,
        CashFlowForecastOpeningBalance openingBalance,
        @ArraySchema(schema = @Schema(implementation = CashFlowForecastMonth.class), minItems = 3, maxItems = 3)
                List<CashFlowForecastMonth> months,
        CashFlowForecastMinimumBalance minimumBalance,
        @ArraySchema(
                        arraySchema =
                                @Schema(
                                        description =
                                                "Records that could have contributed and do not, each with its reason."
                                                        + " Ordered by sourceType, then sourceId."))
                List<CashFlowForecastExclusion> excludedSources,
        @Schema(
                        description =
                                "True when any month is partial or a cash or bank account could not be counted in"
                                        + " the opening balance.",
                        example = "true")
                boolean partial,
        CashFlowForecastAssumptions assumptions) {

    static CashFlowForecastResponse of(CashFlowForecast forecast) {
        ForecastHorizon horizon = forecast.horizon();
        return new CashFlowForecastResponse(
                new CashFlowForecastHorizon(
                        horizon.from(),
                        horizon.to(),
                        horizon.months().stream().map(YearMonth::toString).toList()),
                new CashFlowForecastOpeningBalance(
                        vnd(forecast.openingBalance().amount()),
                        forecast.openingBalance().appliedOn(),
                        forecast.openingBalance().sources().stream()
                                .map(source -> new CashFlowForecastOpeningSource(
                                        SourceType.ASSET,
                                        source.sourceId(),
                                        source.category(),
                                        source.valuationDate(),
                                        source.amount(),
                                        source.currency(),
                                        vnd(source.amountVnd())))
                                .toList()),
                forecast.months().stream().map(CashFlowForecastResponse::month).toList(),
                new CashFlowForecastMinimumBalance(
                        forecast.minimumBalance().date(),
                        forecast.minimumBalance().month().toString(),
                        vnd(forecast.minimumBalance().amount()),
                        forecast.minimumBalance().negative(),
                        forecast.minimumBalance().referenceThreshold(),
                        forecast.minimumBalance().status()),
                forecast.excludedSources().stream()
                        .map(exclusion -> new CashFlowForecastExclusion(
                                exclusion.sourceType(),
                                exclusion.sourceId(),
                                exclusion.reason(),
                                exclusion.affectedMonths().stream()
                                        .map(YearMonth::toString)
                                        .toList(),
                                exclusion.linkedEntryIds()))
                        .toList(),
                forecast.partial(),
                new CashFlowForecastAssumptions(
                        horizon.asOfDate(),
                        TimeConventions.DEFAULT_DISPLAY_ZONE.getId(),
                        Currency.VND,
                        rate(forecast.exchangeRate()),
                        CashFlowForecastService.OPENING_BALANCE_CATEGORIES,
                        horizon.notProjectedFrom() == null
                                ? null
                                : new CashFlowForecastDateRange(horizon.notProjectedFrom(), horizon.notProjectedTo())));
    }

    private static CashFlowForecastMonth month(CashFlowForecast.Month month) {
        return new CashFlowForecastMonth(
                month.month().toString(),
                vnd(month.income()),
                vnd(month.expense()),
                vnd(month.net()),
                vnd(month.openingBalance()),
                vnd(month.closingBalance()),
                vnd(month.lowestBalance()),
                month.lowestBalanceDate(),
                month.partial(),
                month.items().stream()
                        .map(item -> new CashFlowForecastItem(
                                item.date(),
                                item.direction(),
                                item.sourceType(),
                                item.sourceId(),
                                item.category(),
                                item.amount(),
                                item.currency(),
                                vnd(item.amountVnd()),
                                item.cadence(),
                                item.sequence(),
                                item.shiftedToMonthEnd()))
                        .toList());
    }

    private static CashFlowForecastExchangeRate rate(ExchangeRate rate) {
        return rate == null ? null : new CashFlowForecastExchangeRate(rate.rate(), rate.effectiveDate());
    }

    /** Whole đồng shown with the two decimals every catalog amount carries. */
    private static BigDecimal vnd(BigDecimal amount) {
        return amount.setScale(2);
    }

    @Schema(description = "The three whole calendar months the forecast covers.")
    public record CashFlowForecastHorizon(
            @Schema(description = "The first day, included.", example = "2026-12-01") LocalDate from,
            @Schema(description = "The last day, included.", example = "2027-02-28") LocalDate to,
            @ArraySchema(
                            schema = @Schema(example = "2026-12", pattern = "^\\d{4}-\\d{2}$"),
                            minItems = 3,
                            maxItems = 3)
                    List<String> months) {}

    @Schema(description = "The cash the projection starts from, applied at the start of the horizon's first day.")
    public record CashFlowForecastOpeningBalance(
            @Schema(description = "The sum of the counted sources, in VND.", example = "147200000.00")
                    BigDecimal amount,
            @Schema(description = "The first day of the horizon.", example = "2026-12-01") LocalDate appliedOn,
            @ArraySchema(
                            arraySchema =
                                    @Schema(
                                            description =
                                                    "The cash and bank accounts counted, ordered by sourceId. A USD account"
                                                            + " without a declared rate is in excludedSources instead."))
                    List<CashFlowForecastOpeningSource> sources) {}

    @Schema(description = "A cash or bank account counted in the opening balance.")
    public record CashFlowForecastOpeningSource(
            @Schema(description = "Always ASSET.", example = "ASSET") SourceType sourceType,
            @Schema(
                            description = "The asset; GET /api/v1/assets/{id} reads it.",
                            example = "3d7a51c2-8f04-4b6e-9a21-c5e0f8b2d419")
                    UUID sourceId,
            @Schema(description = "CASH or BANK_ACCOUNT.", example = "BANK_ACCOUNT") String category,
            @Schema(description = "The date the user states the value was true.", example = "2026-11-20")
                    LocalDate valuationDate,
            @Schema(description = "The value in the asset's own currency.", example = "2000.00") BigDecimal amount,
            @Schema(description = "The asset's currency.", example = "USD") Currency currency,
            @Schema(description = "The value in VND.", example = "50200000.00") BigDecimal amountVnd) {}

    @Schema(description = "One calendar month of the horizon.")
    public record CashFlowForecastMonth(
            @Schema(example = "2027-01", pattern = "^\\d{4}-\\d{2}$") String month,
            @Schema(description = "The VND sum of the month's INCOME items.", example = "105000000.00")
                    BigDecimal income,
            @Schema(description = "The VND sum of the month's EXPENSE items.", example = "90000000.00")
                    BigDecimal expense,
            @Schema(description = "income minus expense.", example = "15000000.00") BigDecimal net,
            @Schema(description = "Projected cash at the start of the month's first day.", example = "124265000.00")
                    BigDecimal openingBalance,
            @Schema(description = "Projected cash at the end of the month's last day.", example = "139265000.00")
                    BigDecimal closingBalance,
            @Schema(
                            description =
                                    "The lowest of the month's opening balance and every end-of-day balance on a day an"
                                            + " amount lands.",
                            example = "89065000.00")
                    BigDecimal lowestBalance,
            @Schema(description = "The earliest date the lowest balance is reached.", example = "2027-01-20")
                    LocalDate lowestBalanceDate,
            @Schema(
                            description =
                                    "True when an excluded record would have contributed to this month: a liability"
                                            + " falling due in it without a schedule, or a USD amount without a rate.",
                            example = "true")
                    boolean partial,
            @ArraySchema(
                            arraySchema =
                                    @Schema(
                                            description =
                                                    "Ordered by date, then CASH_FLOW_ENTRY before LIABILITY, then sourceId."))
                    List<CashFlowForecastItem> items) {}

    @Schema(description = "One projected amount and the record it comes from.")
    public record CashFlowForecastItem(
            @Schema(description = "The day the amount lands.", example = "2027-01-15") LocalDate date,
            @Schema(description = "INCOME raises the projected cash, EXPENSE lowers it.", example = "EXPENSE")
                    Direction direction,
            @Schema(description = "CASH_FLOW_ENTRY or LIABILITY.", example = "LIABILITY") SourceType sourceType,
            @Schema(
                            description =
                                    "The record; GET /api/v1/cash-flow-entries/{id} or /api/v1/liabilities/{id} reads it.",
                            example = "8e2f4a90-1c37-4d5b-b6a8-0f9d3e7c2b14")
                    UUID sourceId,
            @Schema(description = "The record's category.", example = "BANK_LOAN") String category,
            @Schema(description = "The amount in the record's own currency.", example = "18500000.00")
                    BigDecimal amount,
            @Schema(description = "The record's currency.", example = "VND") Currency currency,
            @Schema(description = "The amount in VND, rounded to whole đồng.", example = "18500000.00")
                    BigDecimal amountVnd,
            @Schema(
                            description =
                                    "MONTHLY, QUARTERLY or YEARLY for an entry; MONTHLY, QUARTERLY, SEMIANNUAL or YEARLY"
                                            + " for a liability; null for a one-time entry.",
                            example = "MONTHLY",
                            nullable = true)
                    String cadence,
            @Schema(
                            description =
                                    "The date's position in the record's series: counted from an entry's effective date,"
                                            + " or from a liability's next payment date.",
                            example = "2")
                    int sequence,
            @Schema(
                            description = "True when the record's day of month does not exist in this month.",
                            example = "false")
                    boolean shiftedToMonthEnd) {}

    @Schema(description = "The lowest projected cash position in the horizon and how it reads.")
    public record CashFlowForecastMinimumBalance(
            @Schema(description = "The earliest date the lowest position is reached.", example = "2027-01-20")
                    LocalDate date,
            @Schema(example = "2027-01", pattern = "^\\d{4}-\\d{2}$") String month,
            @Schema(description = "The lowest position, in VND.", example = "89065000.00") BigDecimal amount,
            @Schema(description = "True when the lowest position is below zero.", example = "false")
                    boolean negative,
            @Schema(
                            description = "One month of projected expense: the horizon's expense divided by three.",
                            example = "78166666.67")
                    BigDecimal referenceThreshold,
            @Schema(
                            description =
                                    "INSUFFICIENT_DATA when no cash or bank account was counted; else NEGATIVE below"
                                            + " zero, THIN below the threshold, ADEQUATE otherwise.",
                            example = "ADEQUATE")
                    MinimumBalanceStatus status) {}

    @Schema(description = "A record that could have contributed to the forecast and does not.")
    public record CashFlowForecastExclusion(
            @Schema(example = "LIABILITY") SourceType sourceType,
            @Schema(example = "c41b7e2d-6a95-4f08-8d3c-27e1b9f05a6e") UUID sourceId,
            @Schema(example = "NO_PAYMENT_SCHEDULE") ExclusionReason reason,
            @ArraySchema(
                            arraySchema =
                                    @Schema(
                                            description =
                                                    "The horizon months it would have contributed to, when known. Each is"
                                                            + " marked partial."),
                            schema = @Schema(example = "2027-01", pattern = "^\\d{4}-\\d{2}$"))
                    List<String> affectedMonths,
            @ArraySchema(
                            arraySchema =
                                    @Schema(
                                            description =
                                                    "For COVERED_BY_LINKED_ENTRIES, the recurring entries linked to the"
                                                            + " liability that count it instead. Empty otherwise."))
                    List<UUID> linkedEntryIds) {}

    @Schema(description = "The inputs the projection rests on.")
    public record CashFlowForecastAssumptions(
            @Schema(description = "The day the forecast was computed, in timeZone.", example = "2026-11-22")
                    LocalDate asOfDate,
            @Schema(description = "The zone today is read in.", example = "Asia/Ho_Chi_Minh") String timeZone,
            @Schema(description = "The currency every total is expressed in.", example = "VND") Currency currency,
            @Schema(
                            description = "The rate USD amounts were converted with. Null when the user has declared none.",
                            nullable = true)
                    CashFlowForecastExchangeRate exchangeRate,
            @ArraySchema(
                            arraySchema = @Schema(description = "The asset categories the opening balance is made of."),
                            schema = @Schema(example = "BANK_ACCOUNT"))
                    List<String> openingBalanceCategories,
            @Schema(
                            description =
                                    "The days between today and the horizon that are not projected. Null when there are"
                                            + " none.",
                            nullable = true)
                    CashFlowForecastDateRange notProjected) {}

    @Schema(description = "The user-declared USD/VND rate the forecast used.")
    public record CashFlowForecastExchangeRate(
            @Schema(description = "How many VND one USD buys.", example = "25100.000000") BigDecimal usdToVndRate,
            @Schema(description = "The date the user declares the rate took effect.", example = "2026-09-12")
                    LocalDate effectiveDate) {}

    @Schema(description = "A range of calendar days, both included.")
    public record CashFlowForecastDateRange(
            @Schema(example = "2026-11-23") LocalDate from, @Schema(example = "2026-11-30") LocalDate to) {}
}
