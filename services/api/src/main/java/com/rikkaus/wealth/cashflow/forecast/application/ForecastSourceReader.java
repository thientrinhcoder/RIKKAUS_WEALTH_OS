package com.rikkaus.wealth.cashflow.forecast.application;

import com.rikkaus.wealth.shared.money.Currency;
import com.rikkaus.wealth.shared.money.ExchangeRate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Reads the rows of other slices' tables that a forecast needs, scoped to one owner.
 *
 * <p>Native SQL rather than a call into {@code settings}, {@code assets} or {@code liabilities},
 * because the slice rule in {@code ArchitectureRulesTest} forbids {@code cashflow} from importing them
 * — the same reason {@code CashFlowEntryRepository} and {@code OnboardingProgressRepository} read those
 * tables directly. Every column read here is one the owning migration constrains ({@code V3}, {@code
 * V4}, {@code V7}), so a change to it is a schema change that would surface here.
 *
 * <p>{@link JdbcClient} rather than a Spring Data native query, because these are projections with no
 * entity of this slice behind them, and the explicit row mapping keeps each {@code DATE} a {@code
 * LocalDate} and each {@code NUMERIC} a {@code BigDecimal} without relying on projection conversion.
 *
 * <p>Every statement filters on {@code user_id} with the value the caller took from {@code
 * CurrentUser}. Another user's rows are never loaded, so there is nothing to filter out afterwards.
 */
@Component
public class ForecastSourceReader {

    private final JdbcClient jdbc;

    ForecastSourceReader(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** The USD/VND rate the owner has declared, or empty when they have not declared one. */
    public Optional<ExchangeRate> exchangeRateOf(UUID ownerId) {
        return jdbc.sql(
                        """
                        SELECT usd_to_vnd_rate, rate_effective_date
                          FROM wealth.user_settings
                         WHERE user_id = :ownerId AND usd_to_vnd_rate IS NOT NULL
                        """)
                .param("ownerId", ownerId)
                .query((rs, row) -> new ExchangeRate(
                        Currency.USD,
                        Currency.VND,
                        rs.getBigDecimal("usd_to_vnd_rate"),
                        rs.getObject("rate_effective_date", LocalDate.class)))
                .optional();
    }

    /**
     * The owner's cash and bank accounts, the only assets the opening balance is made of.
     *
     * <p>By category and not by liquidity: liquidity is the user's judgement of how quickly an asset
     * could be sold, while the opening balance is the money payments are actually made from. A term
     * deposit the user marks HIGH is still not spendable on the first of next month.
     */
    List<ValuedAsset> cashAccountsOf(UUID ownerId) {
        return assetsIn(ownerId, CashFlowForecastService.OPENING_BALANCE_CATEGORIES);
    }

    /**
     * The owner's assets in the given categories, as stored, ordered by category and then identifier so
     * every caller lists them the same way. Financial-health reads its liquid group through it.
     */
    public List<ValuedAsset> assetsIn(UUID ownerId, Collection<String> categories) {
        return jdbc.sql(
                        """
                        SELECT id, category, current_value, currency, valuation_date
                          FROM wealth.assets
                         WHERE user_id = :ownerId AND category IN (:categories)
                         ORDER BY category, id::text
                        """)
                .param("ownerId", ownerId)
                .param("categories", List.copyOf(categories))
                .query((rs, row) -> new ValuedAsset(
                        rs.getObject("id", UUID.class),
                        rs.getString("category"),
                        rs.getBigDecimal("current_value"),
                        Currency.valueOf(rs.getString("currency")),
                        rs.getObject("valuation_date", LocalDate.class)))
                .list();
    }

    /** Every liability the owner has, with the schedule fields as the user stated them. */
    public List<ScheduledLiability> liabilitiesOf(UUID ownerId) {
        return jdbc.sql(
                        """
                        SELECT id, category, currency, outstanding_balance, recurring_payment_amount,
                               recurring_payment_frequency, next_payment_date, maturity_date
                          FROM wealth.liabilities
                         WHERE user_id = :ownerId
                         ORDER BY id::text
                        """)
                .param("ownerId", ownerId)
                .query((rs, row) -> new ScheduledLiability(
                        rs.getObject("id", UUID.class),
                        rs.getString("category"),
                        Currency.valueOf(rs.getString("currency")),
                        rs.getBigDecimal("outstanding_balance"),
                        rs.getBigDecimal("recurring_payment_amount"),
                        rs.getString("recurring_payment_frequency"),
                        rs.getObject("next_payment_date", LocalDate.class),
                        rs.getObject("maturity_date", LocalDate.class)))
                .list();
    }

    /** An asset's stored value and the date the user states it was true. */
    public record ValuedAsset(UUID id, String category, BigDecimal value, Currency currency, LocalDate valuationDate) {}

    /**
     * A liability's schedule as stored. {@code paymentAmount} and {@code frequency} are null together,
     * which {@code liabilities_recurring_payment_is_complete} guarantees.
     */
    public record ScheduledLiability(
            UUID id,
            String category,
            Currency currency,
            BigDecimal outstandingBalance,
            BigDecimal paymentAmount,
            String frequency,
            LocalDate nextPaymentDate,
            LocalDate maturityDate) {}
}
