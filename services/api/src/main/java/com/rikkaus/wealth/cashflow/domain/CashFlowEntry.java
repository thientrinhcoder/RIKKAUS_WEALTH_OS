package com.rikkaus.wealth.cashflow.domain;

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
 * One income or expense a user records, one-time or recurring.
 *
 * <p>There is no setter. Creation goes through {@link #create} and an edit goes through {@link
 * #replace}, which takes the whole {@link CashFlowEntryDetails} at once, so the entity can never hold
 * half of one request and half of another.
 *
 * <p>The owner is a bare {@link UUID} and never a {@code UserAccount}, and the linked asset is a bare
 * {@link UUID} and never an {@code Asset}. The slice rule in {@code ArchitectureRulesTest} forbids
 * {@code cashflow} from depending on {@code identity} or {@code assets}, so both relationships live in
 * the foreign keys {@code V7__cash_flow_entries.sql} declares and nowhere in Java. Whether the linked
 * asset belongs to the same user is checked by {@code CashFlowEntryService} through a query on the
 * table, for the same reason.
 *
 * <p>The identifier is assigned in application code rather than by the database, as every entity here
 * does: the created row's identifier is needed to write a {@code Location} header, and a database
 * sequence would cost a round trip to learn it.
 *
 * <p>Enums are stored by name. {@code EnumType.ORDINAL} would mean that inserting a category anywhere
 * but the end of {@link CashFlowCategory} silently reinterpreted every row already written.
 */
@Entity
@Table(name = "cash_flow_entries", schema = "wealth")
public class CashFlowEntry implements UserOwned {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false)
    private CashFlowEntryType entryType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CashFlowCategory category;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CashFlowTiming timing;

    @Enumerated(EnumType.STRING)
    @Column private CashFlowRecurrence recurrence;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "linked_asset_id")
    private UUID linkedAssetId;

    @Column private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Required by JPA. Not for application use. */
    protected CashFlowEntry() {}

    private CashFlowEntry(UUID id, UUID userId, CashFlowEntryDetails details, Instant now) {
        this.id = id;
        this.userId = userId;
        this.createdAt = now;
        apply(details, now);
    }

    /** A new entry in the owner's catalog. */
    public static CashFlowEntry create(UUID ownerId, CashFlowEntryDetails details, Instant now) {
        return new CashFlowEntry(UUID.randomUUID(), ownerId, details, now);
    }

    /**
     * Replaces every user-stated field wholesale.
     *
     * <p>A replacement rather than a merge: a {@code null} optional field clears the stored value
     * instead of leaving the old one in place. That is the semantics {@code PUT} promises, and the
     * alternative would make "remove my note" or "unlink this asset" impossible to express without a
     * sentinel value.
     */
    public void replace(CashFlowEntryDetails details, Instant now) {
        apply(details, now);
    }

    private void apply(CashFlowEntryDetails details, Instant now) {
        this.entryType = details.entryType();
        this.category = details.category();
        this.amount = details.amount();
        this.currency = details.currency();
        this.timing = details.timing();
        this.recurrence = details.recurrence();
        this.effectiveDate = details.effectiveDate();
        this.endDate = details.endDate();
        this.linkedAssetId = details.linkedAssetId();
        this.notes = details.notes();
        this.updatedAt = now;
    }

    @Override
    public UUID ownerId() {
        return userId;
    }

    public UUID getId() {
        return id;
    }

    public CashFlowEntryType getEntryType() {
        return entryType;
    }

    public CashFlowCategory getCategory() {
        return category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public CashFlowTiming getTiming() {
        return timing;
    }

    public CashFlowRecurrence getRecurrence() {
        return recurrence;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public UUID getLinkedAssetId() {
        return linkedAssetId;
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
