package com.rikkaus.wealth.assets.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/**
 * Asset persistence, declaring only the operations the slice uses.
 *
 * <p>Narrow for the same reason as {@code UserSettingsRepository}. There is no {@code findAll}: a
 * method that returns every user's assets would be one autocomplete away from a cross-user leak, and
 * the only legitimate listing is owner-scoped, so that is the only listing declared.
 *
 * <p>{@link #findByUserIdOrderByValuationDateDescNameAsc} carries the catalog's order in its name
 * rather than leaving it to the caller, because an unordered list from PostgreSQL is not stable
 * between runs and the client renders it in the order it arrives.
 *
 * <p>{@link #findById} returns an {@link Optional} that the caller hands to {@code OwnershipGuard}.
 * It deliberately does <em>not</em> take an owner, because a repository method that filtered by owner
 * would make "absent" and "someone else's" two different outcomes at this layer, and the single place
 * that decides they are indistinguishable is the guard.
 */
public interface AssetRepository extends Repository<Asset, UUID> {

    List<Asset> findByUserIdOrderByValuationDateDescNameAsc(UUID userId);

    Optional<Asset> findById(UUID id);

    Asset save(Asset asset);

    void delete(Asset asset);
}
