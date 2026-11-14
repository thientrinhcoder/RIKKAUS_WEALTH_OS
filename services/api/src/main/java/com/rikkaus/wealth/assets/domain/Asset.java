package com.rikkaus.wealth.assets.domain;

import com.rikkaus.wealth.shared.security.UserOwned;
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
 * One asset a user owns: the common fields every category shares.
 *
 * <p>There is no setter. Creation goes through {@link #create}, an edit goes through {@link
 * #replaceMetadata}, and there is deliberately no third way in — so the valuation columns have no
 * mutator at all. That is the enforcement of the accepted design's boundary with issue #57: a new
 * value must be appended to a history rather than overwriting the current one, and this class makes
 * overwriting it unexpressible rather than merely discouraged.
 *
 * <p>The owner is a bare {@link UUID} and never a {@code UserAccount}. The slice rule in {@code
 * ArchitectureRulesTest} forbids {@code assets} from depending on {@code identity}, so the
 * relationship between the two tables lives in the foreign key {@code V4__assets.sql} declares and
 * nowhere in Java — the shape {@code UserSettings} already uses.
 *
 * <p>The identifier is assigned in application code rather than by the database, for the reason
 * {@code UserAccount} records: the created row's identifier is needed to write a {@code Location}
 * header, and a database sequence would cost a round trip to learn it.
 *
 * <p>Enums are stored by name. {@code EnumType.ORDINAL} would mean that inserting a twelfth category
 * anywhere but the end of {@link AssetCategory} silently reinterpreted every row already written.
 */
@Entity
@Table(name = "assets", schema = "wealth")
public class Asset implements UserOwned {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private AssetCategory category;

    @Column(name = "current_value", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal currentValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private AssetCurrency currency;

    @Column(name = "valuation_date", nullable = false, updatable = false)
    private LocalDate valuationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetLiquidity liquidity;

    @Column(name = "purchase_value", precision = 19, scale = 2)
    private BigDecimal purchaseValue;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(precision = 19, scale = 6)
    private BigDecimal quantity;

    @Column private String institution;

    @Column private String notes;

    @Column(length = 32)
    private String symbol;

    @Column(name = "cost_per_unit", precision = 19, scale = 6)
    private BigDecimal costPerUnit;

    @Column(name = "current_price_per_unit", precision = 19, scale = 6)
    private BigDecimal currentPricePerUnit;

    @Column(name = "interest_rate", precision = 7, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Required by JPA. Not for application use. */
    protected Asset() {}

    private Asset(
            UUID id,
            UUID userId,
            AssetCategory category,
            AssetValuation valuation,
            AssetMetadata metadata,
            AssetInstrumentDetails instrumentDetails,
            Instant now) {
        this.id = id;
        this.userId = userId;
        this.category = category;
        this.currentValue = valuation.currentValue();
        this.currency = valuation.currency();
        this.valuationDate = valuation.valuationDate();
        this.createdAt = now;
        applyMetadata(metadata, instrumentDetails, now);
    }

    /**
     * A new asset in the owner's catalog.
     *
     * <p>{@code updatable = false} on the category and the three valuation columns is not decoration:
     * Hibernate omits them from every {@code UPDATE} it generates, so even a future field assignment
     * inside this class could not reach the database with them. The table itself is the last line, and
     * the absence of a mutator is the first.
     */
    public static Asset create(
            UUID ownerId,
            AssetCategory category,
            AssetValuation valuation,
            AssetMetadata metadata,
            AssetInstrumentDetails instrumentDetails,
            Instant now) {
        return new Asset(UUID.randomUUID(), ownerId, category, valuation, metadata, instrumentDetails, now);
    }

    /**
     * Replaces the editable metadata wholesale.
     *
     * <p>A replacement rather than a merge: a {@code null} optional field clears the stored value
     * instead of leaving the old one in place. That is the semantics {@code PUT} promises, and the
     * alternative would make "remove my note" impossible to express without a sentinel value.
     */
    public void replaceMetadata(
            AssetMetadata metadata, AssetInstrumentDetails instrumentDetails, Instant now) {
        applyMetadata(metadata, instrumentDetails, now);
    }

    /**
     * The one write path, used by creation and by an edit alike.
     *
     * <p>Both halves are applied here rather than in two methods, so there is no way to replace the
     * metadata and leave stale instrument details behind. The instrument fields are a replacement for
     * the same reason the optional common fields are: an omitted {@code symbol} clears the stored one,
     * which is what {@code PUT} promises and the only way to express removing a ticker.
     *
     * <p>Whether the category accepts these four fields at all is decided before this is reached, in
     * {@code AssetService}, with {@code assets_instrument_pricing_suits_the_category} and {@code
     * assets_interest_rate_suits_the_category} in {@code V5__asset_instrument_details.sql} as the
     * backstop for any caller that is not the API. This method stores what it is given.
     */
    private void applyMetadata(
            AssetMetadata metadata, AssetInstrumentDetails instrumentDetails, Instant now) {
        this.name = metadata.name();
        this.liquidity = metadata.liquidity();
        this.purchaseValue = metadata.purchaseValue();
        this.purchaseDate = metadata.purchaseDate();
        this.quantity = metadata.quantity();
        this.institution = metadata.institution();
        this.notes = metadata.notes();
        this.symbol = instrumentDetails.symbol();
        this.costPerUnit = instrumentDetails.costPerUnit();
        this.currentPricePerUnit = instrumentDetails.currentPricePerUnit();
        this.interestRate = instrumentDetails.interestRate();
        this.updatedAt = now;
    }

    @Override
    public UUID ownerId() {
        return userId;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public AssetCategory getCategory() {
        return category;
    }

    public BigDecimal getCurrentValue() {
        return currentValue;
    }

    public AssetCurrency getCurrency() {
        return currency;
    }

    public LocalDate getValuationDate() {
        return valuationDate;
    }

    public AssetLiquidity getLiquidity() {
        return liquidity;
    }

    public BigDecimal getPurchaseValue() {
        return purchaseValue;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getInstitution() {
        return institution;
    }

    public String getNotes() {
        return notes;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getCostPerUnit() {
        return costPerUnit;
    }

    public BigDecimal getCurrentPricePerUnit() {
        return currentPricePerUnit;
    }

    /** Interest rate or expected yield, as a percent per year: {@code 6.2500} is 6.25%. */
    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
