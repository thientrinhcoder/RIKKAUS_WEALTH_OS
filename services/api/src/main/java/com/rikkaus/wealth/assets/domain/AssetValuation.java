package com.rikkaus.wealth.assets.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One valuation ever recorded for an asset: what it was worth, in which currency, as of when, at
 * which rate, and why.
 *
 * <p>There is no setter and no method that changes a field after construction, and every column is
 * mapped {@code updatable = false} so that even a future field assignment inside this class could not
 * reach the database. That is the backbone's rule that a change of value <em>creates</em> a record
 * rather than editing one, made unexpressible rather than discouraged. The table enforces the same
 * rule for callers that are not this application, with the trigger {@code V6__asset_valuations.sql}
 * declares.
 *
 * <p>The asset is a bare {@link UUID} rather than an association, the shape {@link Asset} already uses
 * for its owner. The foreign key and its cascade live in the migration; nothing here could load an
 * asset, so nothing here could reach another slice through one.
 *
 * <p>Which record is current is not a column. It is the first row of the history ordered by effective
 * date and then by {@code recordedAt}, both descending, and {@code Asset} carries a snapshot of that
 * row's value, currency and date. Storing a flag here would create a second source of truth that an
 * append has to keep in step with the first; deriving it from the order means it cannot be wrong.
 */
@Entity
@Table(name = "asset_valuations", schema = "wealth")
public class AssetValuation {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "asset_id", nullable = false, updatable = false)
    private UUID assetId;

    @Column(nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal value;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private AssetCurrency currency;

    @Column(name = "effective_date", nullable = false, updatable = false)
    private LocalDate effectiveDate;

    @Column(name = "usd_to_vnd_rate", updatable = false, precision = 19, scale = 6)
    private BigDecimal usdToVndRate;

    @Column(name = "rate_effective_date", updatable = false)
    private LocalDate rateEffectiveDate;

    @Column(updatable = false)
    private String note;

    @Column(name = "corrects_valuation_id", updatable = false)
    private UUID correctsValuationId;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    /** Required by JPA. Not for application use. */
    protected AssetValuation() {}

    private AssetValuation(UUID assetId, DeclaredValuation declared, Instant now) {
        this.id = UUID.randomUUID();
        this.assetId = assetId;
        this.value = declared.value();
        this.currency = declared.currency();
        this.effectiveDate = declared.effectiveDate();
        this.usdToVndRate = declared.usdToVndRate();
        this.rateEffectiveDate = declared.rateEffectiveDate();
        this.note = declared.note();
        this.correctsValuationId = declared.correctsValuationId();
        this.recordedAt = now;
    }

    /**
     * A new record of what the user declared, stamped with the server's clock.
     *
     * <p>The identifier is assigned here rather than by the database, for the reason {@link Asset}
     * records: the created row's identifier is needed to write a {@code Location} header.
     */
    public static AssetValuation recordedFor(UUID assetId, DeclaredValuation declared, Instant now) {
        return new AssetValuation(assetId, declared, now);
    }

    /**
     * True when this record outranks a current record dated {@code currentValuationDate}.
     *
     * <p>On or after, not strictly after. Two records on one date are a correction and its original, and
     * the later-recorded one — this one, since it is being appended now — is the one the user means.
     * A record dated earlier is history: it is kept, and the asset's current value is left alone.
     */
    public boolean supersedesARecordDated(LocalDate currentValuationDate) {
        return !effectiveDate.isBefore(currentValuationDate);
    }

    public UUID getId() {
        return id;
    }

    public UUID getAssetId() {
        return assetId;
    }

    public BigDecimal getValue() {
        return value;
    }

    public AssetCurrency getCurrency() {
        return currency;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public BigDecimal getUsdToVndRate() {
        return usdToVndRate;
    }

    public LocalDate getRateEffectiveDate() {
        return rateEffectiveDate;
    }

    public String getNote() {
        return note;
    }

    public UUID getCorrectsValuationId() {
        return correctsValuationId;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
