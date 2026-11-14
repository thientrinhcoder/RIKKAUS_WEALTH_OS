package com.rikkaus.wealth.goals.domain;

import com.rikkaus.wealth.shared.money.Currency;
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
 * One financial goal a user is saving towards. The same shape for all seven templates.
 *
 * <p>There is no setter. Creation goes through {@link #create} and an edit through {@link #replace},
 * which takes the whole user-entered field set at once: a replacement rather than a merge, so an
 * optional field left out of a {@code PUT} is cleared.
 *
 * <p>The owner is a bare {@link UUID}, never a {@code UserAccount}. The slice rule in {@code
 * ArchitectureRulesTest} forbids {@code goals} from depending on {@code identity}, so the relationship
 * lives in the foreign key {@code V8__goals.sql} declares and nowhere in Java — the shape {@code Asset}
 * and {@code UserSettings} already use.
 *
 * <p>The currency is the shared {@link Currency} and is always {@link Currency#REPORTING}. It is a
 * column with a check constraint rather than an implicit assumption so that the response can state
 * it and a second currency later is a reviewed migration. Nothing in this slice converts anything.
 *
 * <p>No computed field lives here. Completion percentage, remaining amount, remaining months,
 * required contribution and status are issue #89's outputs; storing any of them would make a derived
 * figure a stored fact that goes stale the moment the clock moves.
 */
@Entity
@Table(name = "goals", schema = "wealth")
public class Goal implements UserOwned {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GoalType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Currency currency;

    @Column(name = "target_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal targetAmount;

    @Column(name = "current_allocated_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal currentAllocatedAmount;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "planned_monthly_contribution", precision = 19, scale = 2)
    private BigDecimal plannedMonthlyContribution;

    @Column(name = "expected_annual_return_percent", precision = 5, scale = 2)
    private BigDecimal expectedAnnualReturnPercent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Required by JPA. Not for application use. */
    protected Goal() {}

    private Goal(UUID id, UUID userId, GoalFields fields, Instant now) {
        this.id = id;
        this.userId = userId;
        this.currency = Currency.REPORTING;
        this.createdAt = now;
        apply(fields, now);
    }

    /**
     * A new goal owned by {@code ownerId}.
     *
     * <p>The identifier is assigned here rather than by the database, for the reason {@code Asset}
     * records: the created row's identifier is needed to write a {@code Location} header, and a
     * database sequence would cost a round trip to learn it.
     */
    public static Goal create(UUID ownerId, GoalFields fields, Instant now) {
        return new Goal(UUID.randomUUID(), ownerId, fields, now);
    }

    /** Replaces every user-entered field wholesale. A {@code null} optional field clears the stored one. */
    public void replace(GoalFields fields, Instant now) {
        apply(fields, now);
    }

    private void apply(GoalFields fields, Instant now) {
        this.name = fields.name();
        this.type = fields.type();
        this.targetAmount = fields.targetAmount();
        this.currentAllocatedAmount = fields.currentAllocatedAmount();
        this.targetDate = fields.targetDate();
        this.plannedMonthlyContribution = fields.plannedMonthlyContribution();
        this.expectedAnnualReturnPercent = fields.expectedAnnualReturnPercent();
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

    public GoalType getType() {
        return type;
    }

    public Currency getCurrency() {
        return currency;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public BigDecimal getCurrentAllocatedAmount() {
        return currentAllocatedAmount;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public BigDecimal getPlannedMonthlyContribution() {
        return plannedMonthlyContribution;
    }

    public BigDecimal getExpectedAnnualReturnPercent() {
        return expectedAnnualReturnPercent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
