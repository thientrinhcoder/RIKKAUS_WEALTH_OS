package com.rikkaus.wealth.assets.domain;

import com.rikkaus.wealth.shared.search.SearchText;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Builds the listing as a Criteria query with every value bound, never concatenated into SQL.
 *
 * <p>Every order ends with the id, so two assets that agree on every sort column still come back in
 * the same order on every request.
 */
class AssetSearchImpl implements AssetSearch {

    private final EntityManager entityManager;

    AssetSearchImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<Asset> search(UUID ownerId, AssetFilter filter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Asset> query = cb.createQuery(Asset.class);
        Root<Asset> asset = query.from(Asset.class);

        List<Predicate> where = new ArrayList<>();
        where.add(cb.equal(asset.get("userId"), ownerId));
        SearchText.TextMatch match =
                filter.text() == null
                        ? null
                        : SearchText.matchesAny(
                                cb,
                                filter.text(),
                                List.of(
                                        asset.<String>get("name"),
                                        asset.<String>get("institution"),
                                        asset.<String>get("notes")));
        if (match != null) {
            where.add(match.predicate());
        }
        if (!filter.categories().isEmpty()) {
            where.add(asset.get("category").in(filter.categories()));
        }
        if (!filter.currencies().isEmpty()) {
            where.add(asset.get("currency").in(filter.currencies()));
        }
        if (!filter.liquidities().isEmpty()) {
            where.add(asset.get("liquidity").in(filter.liquidities()));
        }

        query.select(asset)
                .where(where.toArray(Predicate[]::new))
                .orderBy(order(cb, asset, filter.sort()));
        TypedQuery<Asset> typed = entityManager.createQuery(query);
        return (match == null ? typed : match.bindTo(typed)).getResultList();
    }

    private static List<Order> order(CriteriaBuilder cb, Root<Asset> asset, AssetSort sort) {
        Order newestFirst = cb.desc(asset.get("valuationDate"));
        Order byName = cb.asc(asset.get("name"));
        Order byId = cb.asc(asset.get("id"));
        return switch (sort) {
            case VALUATION_DATE_DESC -> List.of(newestFirst, byName, byId);
            case VALUATION_DATE_ASC -> List.of(cb.asc(asset.get("valuationDate")), byName, byId);
            case NAME_ASC -> List.of(byName, newestFirst, byId);
            case NAME_DESC -> List.of(cb.desc(asset.get("name")), newestFirst, byId);
        };
    }
}
