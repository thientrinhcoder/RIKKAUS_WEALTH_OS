package com.rikkaus.wealth.settings.domain;

import com.rikkaus.wealth.shared.security.UserOwned;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One user's settings: the fixed reporting currency and the exchange rate they declared.
 *
 * <p>The owner is the primary key rather than a column beside a surrogate id, because the resource is
 * a singleton per user. A second row for the same user is not something a check has to catch; it
 * cannot be written.
 *
 * <p>The rate is a {@link BigDecimal} mapped to {@code NUMERIC(19, 6)}. Nothing here may become a
 * {@code double}: the rate multiplies into every USD holding a user owns, so a binary rounding error
 * would move a reported net-worth figure rather than a displayed decimal place.
 *
 * <p>This class holds the owner as a bare {@link UUID} and never a {@code UserAccount}. The slice rule
 * in {@code ArchitectureRulesTest} forbids {@code settings} from depending on {@code identity}, so the
 * relationship between the two tables lives in the foreign key declared by {@code
 * V3__settings_and_profile.sql} and nowhere in Java.
 */
@Entity
@Table(name = "user_settings", schema = "wealth")
public class UserSettings implements UserOwned {

    /** The only reporting currency MVP 1A has. A database check refuses anything else. */
    public static final String BASE_CURRENCY_VND = "VND";

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "base_currency", nullable = false)
    private String baseCurrency;

    @Column(name = "usd_to_vnd_rate", precision = 19, scale = 6)
    private BigDecimal usdToVndRate;

    @Column(name = "rate_effective_date")
    private LocalDate rateEffectiveDate;

    @Column(name = "rate_updated_at")
    private Instant rateUpdatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Required by JPA. Not for application use. */
    protected UserSettings() {}

    private UserSettings(UUID userId, Instant now) {
        this.userId = userId;
        this.baseCurrency = BASE_CURRENCY_VND;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * A user's settings before they have declared anything: VND reporting and no exchange rate.
     *
     * <p>The three rate fields stay null together, which is the state the accepted design for issue #48
     * renders as <em>Tỷ giá cần được cập nhật</em>. It is a designed state, not a missing row.
     */
    public static UserSettings defaultsFor(UUID userId, Instant now) {
        return new UserSettings(userId, now);
    }

    /**
     * Records the rate the user declared and the date they say it took effect.
     *
     * <p>All three rate fields are written in this one call so a half-saved rate — a value with no
     * declared date, or a date with no value — is not expressible here any more than it is in the
     * table. {@code rateUpdatedAt} is the server's own record of when this happened and is never taken
     * from the caller's input.
     */
    public void declareExchangeRate(BigDecimal usdToVndRate, LocalDate effectiveDate, Instant now) {
        this.usdToVndRate = usdToVndRate;
        this.rateEffectiveDate = effectiveDate;
        this.rateUpdatedAt = now;
        this.updatedAt = now;
    }

    @Override
    public UUID ownerId() {
        return userId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public BigDecimal getUsdToVndRate() {
        return usdToVndRate;
    }

    public LocalDate getRateEffectiveDate() {
        return rateEffectiveDate;
    }

    public Instant getRateUpdatedAt() {
        return rateUpdatedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
