package com.rikkaus.wealth.assets.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/**
 * Valuation persistence, declaring only the operations the slice uses — and so declaring no way to
 * change or remove a record.
 *
 * <p>There is no {@code delete} and no way to update, which is the first line of the append-only rule;
 * {@link AssetValuation} mapping every column {@code updatable = false} is the second, and the trigger
 * in {@code V6__asset_valuations.sql} is the last. There is no {@code findAll} either, for the reason
 * {@code AssetRepository} gives: every legitimate query is scoped to one asset.
 *
 * <p>Every lookup takes the asset's identifier, not the owner's. Ownership is decided once, on the
 * asset, by {@code OwnershipGuard}; a record is then reachable only through an asset the viewer owns.
 * {@link #findByIdAndAssetId} carries the asset for that reason: a valuation identifier that belongs to
 * another asset — anybody's — is absent, not merely somebody else's.
 *
 * <p>The two ordered queries share one order, latest effective date first and then latest recorded,
 * because that order is the definition of which record is current. The history is listed in it and
 * its first row is the current one. {@link #findFirstByAssetIdOrderByEffectiveDateAsc} is the other
 * end of the same history: the earliest value ever recorded, which a purchase date may not follow.
 */
public interface AssetValuationRepository extends Repository<AssetValuation, UUID> {

    List<AssetValuation> findByAssetIdOrderByEffectiveDateDescRecordedAtDesc(UUID assetId);

    Optional<AssetValuation> findFirstByAssetIdOrderByEffectiveDateDescRecordedAtDesc(UUID assetId);

    Optional<AssetValuation> findFirstByAssetIdOrderByEffectiveDateAsc(UUID assetId);

    Optional<AssetValuation> findByIdAndAssetId(UUID id, UUID assetId);

    AssetValuation save(AssetValuation valuation);
}
