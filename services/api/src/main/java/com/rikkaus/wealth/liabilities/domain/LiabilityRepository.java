package com.rikkaus.wealth.liabilities.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Liability persistence, declaring only the operations the slice uses.
 *
 * <p>Narrow for the reason {@code AssetRepository} gives: there is no {@code findAll}, because a
 * method that returns every user's liabilities is one autocomplete away from a cross-user leak, and
 * the only legitimate listing is owner-scoped. {@link #findById} deliberately takes no owner, so
 * "absent" and "someone else's" are the same outcome at this layer and {@code OwnershipGuard} is the
 * single place that decides they are indistinguishable.
 */
public interface LiabilityRepository extends Repository<Liability, UUID> {

    /**
     * The owner's catalog, soonest payment first.
     *
     * <p>A JPQL query rather than a derived method name because the order needs {@code NULLS LAST}: a
     * liability with no next payment date — a margin loan, a guarantee — belongs after the ones with
     * a date, not before them, and PostgreSQL's default for {@code ASC} is nulls last but a derived
     * query cannot state it. The name breaks the tie so two liabilities due the same day do not swap
     * places between requests.
     */
    @Query(
            "select l from Liability l where l.userId = :userId "
                    + "order by l.nextPaymentDate asc nulls last, l.name asc")
    List<Liability> findByOwnerOrderBySoonestPayment(UUID userId);

    Optional<Liability> findById(UUID id);

    Liability save(Liability liability);

    void delete(Liability liability);

    /**
     * Whether {@code assetId} names an asset that {@code userId} owns.
     *
     * <p>This is how the slice checks the collateral without depending on {@code assets}. The
     * ArchUnit slice rule forbids importing {@code Asset} or {@code AssetRepository}; a port in {@code
     * shared} would have made {@code shared} know what an asset is and required a change inside the
     * {@code assets} slice to implement it. The foreign key {@code V6__liabilities.sql} declares
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
