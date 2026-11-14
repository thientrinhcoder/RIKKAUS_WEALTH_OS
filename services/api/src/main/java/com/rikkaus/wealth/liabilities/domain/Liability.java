package com.rikkaus.wealth.liabilities.domain;

import com.rikkaus.wealth.shared.money.Currency;
import com.rikkaus.wealth.shared.security.UserOwned;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One debt or obligation a user carries: its balance, the terms stated, the schedule stated, and the
 * asset pledged against it if any.
 *
 * <p>There is no setter. Creation goes through {@link #create} and an edit through {@link #replace},
 * which takes the whole editable set at once; the category has no mutator and is mapped {@code
 * updatable = false}, so Hibernate omits it from every {@code UPDATE} even if a future assignment
 * inside this class tried.
 *
 * <p>The owner and the collateral are bare {@link UUID}s, never a {@code UserAccount} or an {@code
 * Asset}. The slice rule in {@code ArchitectureRulesTest} forbids {@code liabilities} from depending
 * on {@code identity} or {@code assets}, so both relationships live in the foreign keys {@code
 * V6__liabilities.sql} declares and nowhere in Java — the shape {@code Asset} already uses for its
 * owner.
 *
 * <p>The identifier is assigned in application code so the {@code Location} header can be written
 * without a second round trip. Enums are stored by name, never by ordinal.
 */
@Entity
@Table(name = "liabilities", schema = "wealth")
public class Liability implements UserOwned {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private LiabilityCategory category;

    @Column(name = "outstanding_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency;

    @Column(name = "interest_rate", precision = 7, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    @Column(name = "recurring_payment_amount", precision = 19, scale = 2)
    private BigDecimal recurringPaymentAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurring_payment_frequency")
    private PaymentFrequency recurringPaymentFrequency;

    @Column(name = "next_payment_date")
    private LocalDate nextPaymentDate;

    @Column(name = "collateral_asset_id")
    private UUID collateralAssetId;

    @Column private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Required by JPA. Not for application use. */
    protected Liability() {}

    private Liability(
            UUID id, UUID userId, LiabilityCategory category, LiabilityDetails details, Instant now) {
        this.id = id;
        this.userId = userId;
        this.category = category;
        this.createdAt = now;
        apply(details, now);
    }

    /** A new liability in the owner's catalog. */
    public static Liability create(
            UUID ownerId, LiabilityCategory category, LiabilityDetails details, Instant now) {
        return new Liability(UUID.randomUUID(), ownerId, category, details, now);
    }

    /**
     * Replaces every editable member wholesale.
     *
     * <p>A replacement rather than a merge: a {@code null} optional member clears the stored value
     * instead of leaving the old one in place. That is what {@code PUT} promises, and it is what makes
     * "unlink the collateral" expressible by omitting the field rather than by a sentinel.
     */
    public void replace(LiabilityDetails details, Instant now) {
        apply(details, now);
    }

    private void apply(LiabilityDetails details, Instant now) {
        this.name = details.name();
        this.outstandingBalance = atColumnScale(details.outstandingBalance(), 2);
        this.currency = details.currency();
        this.interestRate = atColumnScale(details.terms().interestRate(), 4);
        this.startDate = details.terms().startDate();
        this.maturityDate = details.terms().maturityDate();
        this.recurringPaymentAmount = atColumnScale(details.schedule().recurringPaymentAmount(), 2);
        this.recurringPaymentFrequency = details.schedule().recurringPaymentFrequency();
        this.nextPaymentDate = details.schedule().nextPaymentDate();
        this.collateralAssetId = details.collateralAssetId();
        this.notes = details.notes();
        this.updatedAt = now;
    }

    /**
     * The value at the scale its column holds it, so the response to a create reads exactly as the
     * later read does.
     *
     * <p>Without this a rate sent as {@code 8.5} is echoed as {@code 8.5} from the 201 and as {@code
     * 8.5000} from every GET, because the first is the in-memory value and the second is what {@code
     * NUMERIC(7, 4)} handed back. The two are equal and a client parsing numbers would not notice, but
     * a client comparing the two documents would, and "what you read back is what you were told" is
     * worth one line. Never rounds in practice: the request records' {@code @Digits} bounds already
     * refuse a finer value, so {@code HALF_UP} is the mode that cannot throw rather than one that is
     * ever exercised.
     */
    private static BigDecimal atColumnScale(BigDecimal value, int scale) {
        return value == null ? null : value.setScale(scale, RoundingMode.HALF_UP);
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

    public LiabilityCategory getCategory() {
        return category;
    }

    public BigDecimal getOutstandingBalance() {
        return outstandingBalance;
    }

    public Currency getCurrency() {
        return currency;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getMaturityDate() {
        return maturityDate;
    }

    public BigDecimal getRecurringPaymentAmount() {
        return recurringPaymentAmount;
    }

    public PaymentFrequency getRecurringPaymentFrequency() {
        return recurringPaymentFrequency;
    }

    public LocalDate getNextPaymentDate() {
        return nextPaymentDate;
    }

    public UUID getCollateralAssetId() {
        return collateralAssetId;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
