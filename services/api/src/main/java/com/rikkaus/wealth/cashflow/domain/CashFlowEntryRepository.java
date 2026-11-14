package com.rikkaus.wealth.cashflow.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Cash-flow entry persistence, declaring only the operations the slice uses.
 *
 * <p>Narrow for the same reason as {@code AssetRepository}. There is no {@code findAll}: a method that
 * returns every user's entries would be one autocomplete away from a cross-user leak, and the only
 * legitimate listing is owner-scoped, so that is the only listing declared.
 *
 * <p>{@link #findByUserIdOrderByEffectiveDateDescCreatedAtDesc} carries the catalog's order in its
 * name rather than leaving it to the caller, because an unordered list from PostgreSQL is not stable
 * between runs and the client renders it in the order it arrives. {@code createdAt} breaks the tie so
 * two entries dated the same day do not swap places between requests.
 *
 * <p>{@link #findById} returns an {@link Optional} that the caller hands to {@code OwnershipGuard}.
 * It deliberately does <em>not</em> take an owner, because a repository method that filtered by owner
 * would make "absent" and "someone else's" two different outcomes at this layer, and the single place
 * that decides they are indistinguishable is the guard.
 */
public interface CashFlowEntryRepository extends Repository<CashFlowEntry, UUID> {

    List<CashFlowEntry> findByUserIdOrderByEffectiveDateDescCreatedAtDesc(UUID userId);

    /**
     * The owner's entries that can have an occurrence between {@code from} and {@code to}, both
     * included: those that start no later than {@code to} and have not ended before {@code from}.
     *
     * <p>A coarse filter, not the answer. A quarterly entry can overlap the window and still fall in
     * none of its days; {@code CashFlowSchedule} decides that. The filter only keeps a request for one
     * month from loading entries that ended years ago or start next decade. A one-time entry has no end
     * date, so it passes on its effective date alone and the schedule drops it if it is before
     * {@code from}.
     */
    @Query(
            "SELECT e FROM CashFlowEntry e WHERE e.userId = :ownerId AND e.effectiveDate <= :to"
                    + " AND (e.endDate IS NULL OR e.endDate >= :from)")
    List<CashFlowEntry> findActiveBetween(
            @Param("ownerId") UUID ownerId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    Optional<CashFlowEntry> findById(UUID id);

    CashFlowEntry save(CashFlowEntry entry);

    void delete(CashFlowEntry entry);

    /**
     * Sends the pending statements now, instead of at commit.
     *
     * <p>Declared for one reason: Hibernate checks the affected row count of an {@code UPDATE} or
     * {@code DELETE} and raises a stale-state failure when it is zero, and that happens during the
     * flush. Without a flush inside the service method the failure surfaces while the transaction
     * commits — after the method has returned, where no {@code catch} in the slice can see it, and it
     * reaches the client as a 500. Flushing inside the method is what lets the slice answer the
     * {@code not-found} that a concurrently deleted row deserves.
     */
    void flush();

    /**
     * Whether an asset exists and belongs to the given owner.
     *
     * <p>A native query against {@code wealth.assets} rather than a call into the {@code assets}
     * slice, because the slice rule in {@code ArchitectureRulesTest} forbids {@code cashflow} from
     * importing anything that slice declares. The relationship between the two tables is already a
     * foreign key in {@code V8__cash_flow_entries.sql}; this query reads the same two columns that key
     * depends on and nothing else, so it adds no coupling the schema does not already carry.
     *
     * <p>The alternative — a port in {@code shared} implemented by {@code assets} — would make {@code
     * shared} name a bean only one slice can supply, which inverts the direction the rule protects.
     *
     * <p>One answer for "absent" and "someone else's", deliberately. A caller that could tell them
     * apart would be able to confirm which asset identifiers exist, which is the enumeration oracle
     * {@code OwnershipGuard} closes on every other route.
     */
    @Query(
            value =
                    "SELECT EXISTS (SELECT 1 FROM wealth.assets WHERE id = :assetId AND user_id = :ownerId)",
            nativeQuery = true)
    boolean assetBelongsTo(@Param("assetId") UUID assetId, @Param("ownerId") UUID ownerId);

    /**
     * Whether a liability exists and belongs to the given owner.
     *
     * <p>The same query against {@code wealth.liabilities}, for the same reasons as {@link
     * #assetBelongsTo}: the slice may not import {@code liabilities}, the foreign key already ties the
     * two tables, and "absent" and "someone else's" are deliberately one answer.
     */
    @Query(
            value =
                    "SELECT EXISTS (SELECT 1 FROM wealth.liabilities WHERE id = :liabilityId AND user_id = :ownerId)",
            nativeQuery = true)
    boolean liabilityBelongsTo(@Param("liabilityId") UUID liabilityId, @Param("ownerId") UUID ownerId);
}
