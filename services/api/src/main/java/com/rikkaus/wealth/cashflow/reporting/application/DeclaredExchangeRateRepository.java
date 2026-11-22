package com.rikkaus.wealth.cashflow.reporting.application;

import com.rikkaus.wealth.cashflow.domain.CashFlowEntry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Reads the USD/VND rate the owner has declared, for converting USD entries in a report.
 *
 * <p>A native query against {@code wealth.user_settings} rather than a call into the {@code settings}
 * slice, because the slice rule in {@code ArchitectureRulesTest} forbids {@code cashflow} from
 * importing it — the approach {@code onboarding} already takes to see whether a rate is declared. It
 * reads the two columns the backbone requires a conversion to retain, the rate and its effective
 * date, and nothing else.
 *
 * <p>Typed on {@link CashFlowEntry} only because a Spring Data repository needs a domain type; it
 * declares no method that reads or writes an entry.
 */
public interface DeclaredExchangeRateRepository extends Repository<CashFlowEntry, UUID> {

    /** The declared rate, as stored. */
    interface DeclaredRate {
        BigDecimal getUsdToVndRate();

        LocalDate getEffectiveDate();
    }

    /**
     * The owner's declared rate, or empty when they have none.
     *
     * <p>Reading the settings resource creates the row with every rate column null, and the table's
     * check keeps the three rate columns null or set together, so "no row" and "a row with no rate"
     * both mean "not declared" and both are empty here. The aliases are quoted so PostgreSQL keeps
     * their case and they match the getters of {@link DeclaredRate}.
     */
    @Query(
            value =
                    "SELECT s.usd_to_vnd_rate AS \"usdToVndRate\", s.rate_effective_date AS \"effectiveDate\""
                            + " FROM wealth.user_settings s"
                            + " WHERE s.user_id = :ownerId AND s.usd_to_vnd_rate IS NOT NULL",
            nativeQuery = true)
    Optional<DeclaredRate> findDeclaredRate(@Param("ownerId") UUID ownerId);
}
