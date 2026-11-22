package com.rikkaus.wealth.liabilities.domain;

import com.rikkaus.wealth.shared.search.SearchText;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Nulls;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Builds the listing as a Criteria query with every value bound.
 *
 * <p>The date orders state {@code NULLS LAST} explicitly in both directions: PostgreSQL's default for
 * {@code DESC} is nulls first, which would put a margin loan with no payment date above the loan due
 * tomorrow. Every order ends with the id so a full tie is still stable.
 */
class LiabilitySearchImpl implements LiabilitySearch {

    private final EntityManager entityManager;

    LiabilitySearchImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<Liability> search(UUID ownerId, LiabilityFilter filter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Liability> query = cb.createQuery(Liability.class);
        Root<Liability> liability = query.from(Liability.class);

        List<Predicate> where = new ArrayList<>();
        where.add(cb.equal(liability.get("userId"), ownerId));
        SearchText.TextMatch match =
                filter.text() == null
                        ? null
                        : SearchText.matchesAny(
                                cb,
                                filter.text(),
                                List.of(
                                        liability.<String>get("name"),
                                        liability.<String>get("notes")));
        if (match != null) {
            where.add(match.predicate());
        }
        if (!filter.categories().isEmpty()) {
            where.add(liability.get("category").in(filter.categories()));
        }
        if (!filter.currencies().isEmpty()) {
            where.add(liability.get("currency").in(filter.currencies()));
        }

        query.select(liability)
                .where(where.toArray(Predicate[]::new))
                .orderBy(order(cb, liability, filter.sort()));
        TypedQuery<Liability> typed = entityManager.createQuery(query);
        return (match == null ? typed : match.bindTo(typed)).getResultList();
    }

    private static List<Order> order(
            CriteriaBuilder cb, Root<Liability> liability, LiabilitySort sort) {
        Order soonestFirst = cb.asc(liability.get("nextPaymentDate"), Nulls.LAST);
        Order byName = cb.asc(liability.get("name"));
        Order byId = cb.asc(liability.get("id"));
        return switch (sort) {
            case NEXT_PAYMENT_DATE_ASC -> List.of(soonestFirst, byName, byId);
            case NEXT_PAYMENT_DATE_DESC ->
                    List.of(cb.desc(liability.get("nextPaymentDate"), Nulls.LAST), byName, byId);
            case NAME_ASC -> List.of(byName, soonestFirst, byId);
            case NAME_DESC -> List.of(cb.desc(liability.get("name")), soonestFirst, byId);
        };
    }
}
