package com.rikkaus.wealth.snapshot.application;

import com.rikkaus.wealth.shared.money.Currency;
import com.rikkaus.wealth.shared.money.ExchangeRate;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Reads the rows of other slices' tables that a wealth snapshot is summed from, scoped to one owner.
 *
 * <p>Native SQL rather than a call into {@code assets}, {@code liabilities} or {@code settings}, because
 * the slice rule in {@code ArchitectureRulesTest} forbids {@code snapshot} from importing them — the
 * approach {@code cashflow/forecast} already takes. Every column read here is one the owning migration
 * constrains ({@code V3}, {@code V4}, {@code V7}), so a change to it is a schema change that surfaces here.
 *
 * <p>The asset row is read from {@code wealth.assets} and not from the valuation history: the catalog
 * row is the denormalised copy of the current valuation record, kept in step in the same transaction
 * that appends a record, so it is the "latest asset valuation" the backbone sums.
 *
 * <p>Every statement filters on {@code user_id} with the value the caller took from {@code
 * CurrentUser}. Another user's rows are never loaded, so there is nothing to filter out afterwards.
 */
@Component
class SnapshotSourceReader {

    private final JdbcClient jdbc;

    SnapshotSourceReader(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** The USD/VND rate the owner has declared, or empty when they have not declared one. */
    Optional<ExchangeRate> exchangeRateOf(UUID ownerId) {
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

    /** Every asset the owner has, with its current value as stored. */
    List<AssetRow> assetsOf(UUID ownerId) {
        return jdbc.sql(
                        """
                        SELECT id, name, category, liquidity, current_value, currency, valuation_date
                          FROM wealth.assets
                         WHERE user_id = :ownerId
                        """)
                .param("ownerId", ownerId)
                .query((rs, row) -> new AssetRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getString("liquidity"),
                        rs.getBigDecimal("current_value"),
                        Currency.valueOf(rs.getString("currency")),
                        rs.getObject("valuation_date", LocalDate.class)))
                .list();
    }

    /** Every liability the owner has, with the balance and the dates the user stated. */
    List<LiabilityRow> liabilitiesOf(UUID ownerId) {
        return jdbc.sql(
                        """
                        SELECT id, name, category, outstanding_balance, currency, next_payment_date, maturity_date,
                               recurring_payment_amount, recurring_payment_frequency, updated_at
                          FROM wealth.liabilities
                         WHERE user_id = :ownerId
                        """)
                .param("ownerId", ownerId)
                .query((rs, row) -> new LiabilityRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getBigDecimal("outstanding_balance"),
                        Currency.valueOf(rs.getString("currency")),
                        rs.getObject("next_payment_date", LocalDate.class),
                        rs.getObject("maturity_date", LocalDate.class),
                        rs.getBigDecimal("recurring_payment_amount"),
                        rs.getString("recurring_payment_frequency"),
                        rs.getObject("updated_at", OffsetDateTime.class).toInstant()))
                .list();
    }

    record AssetRow(
            UUID id,
            String name,
            String category,
            String liquidity,
            BigDecimal value,
            Currency currency,
            LocalDate valuationDate) {}

    /**
     * A liability as stored. {@code recurringPaymentAmount} and {@code recurringPaymentFrequency} are
     * null together, which {@code liabilities_recurring_payment_is_complete} guarantees.
     */
    record LiabilityRow(
            UUID id,
            String name,
            String category,
            BigDecimal outstandingBalance,
            Currency currency,
            LocalDate nextPaymentDate,
            LocalDate maturityDate,
            BigDecimal recurringPaymentAmount,
            String recurringPaymentFrequency,
            Instant updatedAt) {}
}
