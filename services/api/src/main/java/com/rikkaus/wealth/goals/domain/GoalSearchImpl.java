package com.rikkaus.wealth.goals.domain;

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

/** Builds the listing as a Criteria query with every value bound; every order ends with the id. */
class GoalSearchImpl implements GoalSearch {

    private final EntityManager entityManager;

    GoalSearchImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<Goal> search(UUID ownerId, GoalFilter filter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Goal> query = cb.createQuery(Goal.class);
        Root<Goal> goal = query.from(Goal.class);

        List<Predicate> where = new ArrayList<>();
        where.add(cb.equal(goal.get("userId"), ownerId));
        SearchText.TextMatch match =
                filter.text() == null
                        ? null
                        : SearchText.matchesAny(cb, filter.text(), List.of(goal.<String>get("name")));
        if (match != null) {
            where.add(match.predicate());
        }
        if (!filter.types().isEmpty()) {
            where.add(goal.get("type").in(filter.types()));
        }

        query.select(goal)
                .where(where.toArray(Predicate[]::new))
                .orderBy(order(cb, goal, filter.sort()));
        TypedQuery<Goal> typed = entityManager.createQuery(query);
        return (match == null ? typed : match.bindTo(typed)).getResultList();
    }

    private static List<Order> order(CriteriaBuilder cb, Root<Goal> goal, GoalSort sort) {
        Order nearestFirst = cb.asc(goal.get("targetDate"));
        Order byName = cb.asc(goal.get("name"));
        Order byId = cb.asc(goal.get("id"));
        return switch (sort) {
            case TARGET_DATE_ASC -> List.of(nearestFirst, byName, byId);
            case TARGET_DATE_DESC -> List.of(cb.desc(goal.get("targetDate")), byName, byId);
            case NAME_ASC -> List.of(byName, nearestFirst, byId);
            case NAME_DESC -> List.of(cb.desc(goal.get("name")), nearestFirst, byId);
        };
    }
}
