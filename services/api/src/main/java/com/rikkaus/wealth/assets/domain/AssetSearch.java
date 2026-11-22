package com.rikkaus.wealth.assets.domain;

import java.util.List;
import java.util.UUID;

/**
 * The catalog listing, narrowed and ordered as the client asked.
 *
 * <p>A repository fragment rather than a {@code JpaSpecificationExecutor}, for the reason {@link
 * AssetRepository} declares no {@code findAll}: a method that accepts an arbitrary specification would
 * return every user's assets the first time somebody passed one without the owner in it. Here the
 * owner is a required argument and the implementation makes it the first predicate, so no filter can
 * widen a listing beyond it.
 */
public interface AssetSearch {

    List<Asset> search(UUID ownerId, AssetFilter filter);
}
