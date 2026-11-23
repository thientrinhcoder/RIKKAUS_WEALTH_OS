package com.rikkaus.wealth.liabilities.domain;

import com.rikkaus.wealth.shared.deletion.LinkedRecordRow;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Liability persistence, declaring only the operations the slice uses.
 *
 * <p>Narrow for the reason {@code AssetRepository} gives: there is no {@code findAll}, because a
 * method that returns every user's liabilities is one autocomplete away from a cross-user leak, and
 * the only legitimate listing is owner-scoped. {@link #findById} deliberately takes no owner, so
 * "absent" and "someone else's" are the same outcome at this layer and {@code OwnershipGuard} is the
 * single place that decides they are indistinguishable.
 */
public interface LiabilityRepository extends Repository<Liability, UUID>, LiabilitySearch {

    Optional<Liability> findById(UUID id);

    /**
     * The liability, with its row locked until the transaction ends.
     *
     * <p>Declared for the delete, which reads what would be unlinked and then acts on it. Locking first
     * serialises two overlapping deletes: the second waits, finds the row gone and answers {@code
     * not-found}, instead of both judging the same impact. A JPQL query rather than a derived {@code
     * findById} with {@code @Lock}, as in {@code AssetRepository}, so the lock cannot be dropped by a
     * rename.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Liability l where l.id = :id")
    Optional<Liability> findAndLockById(@Param("id") UUID id);

    Liability save(Liability liability);

    void delete(Liability liability);

    /**
     * The owner's cash-flow entries linked to this liability, which {@code ON DELETE SET NULL} unlinks.
     *
     * <p>Native SQL on the cash-flow table for the reason {@link #ownsAsset} gives: the foreign key
     * already couples the two tables, and this slice may not import the other.
     */
    @Query(
            value =
                    "SELECT id, notes AS label, category FROM wealth.cash_flow_entries"
                            + " WHERE linked_liability_id = :liabilityId AND user_id = :ownerId"
                            + " ORDER BY created_at, id",
            nativeQuery = true)
    List<LinkedRecordRow> cashFlowEntriesLinkedTo(
            @Param("liabilityId") UUID liabilityId, @Param("ownerId") UUID ownerId);

    /**
     * Whether {@code assetId} names an asset that {@code userId} owns.
     *
     * <p>This is how the slice checks the collateral without depending on {@code assets}. The
     * ArchUnit slice rule forbids importing {@code Asset} or {@code AssetRepository}; a port in {@code
     * shared} would have made {@code shared} know what an asset is and required a change inside the
     * {@code assets} slice to implement it. The foreign key {@code V7__liabilities.sql} declares
     * already couples the two tables at the schema level, so a read of that table scoped by owner
     * adds no coupling that does not exist.
     *
     * <p>A single boolean, deliberately. Returning the asset's row would hand this slice data it has
     * no business reading, and distinguishing "absent" from "somebody else's" here would let the
     * service leak which identifiers exist. The caller turns {@code false} into one message for both.
     */
    @Query(
            value = "select exists (select 1 from wealth.assets where id = :assetId and user_id = :userId)",
            nativeQuery = true)
    boolean ownsAsset(UUID assetId, UUID userId);

    /**
     * Sends the pending statements now, instead of at commit, for the reason {@code AssetRepository}
     * records: a concurrently deleted row raises its stale-state failure during the flush, and only a
     * flush inside the service method lets the slice answer {@code not-found} instead of a 500.
     */
    void flush();
}
