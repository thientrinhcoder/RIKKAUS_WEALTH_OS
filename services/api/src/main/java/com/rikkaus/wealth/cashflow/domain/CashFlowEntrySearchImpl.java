package com.rikkaus.wealth.cashflow.domain;

import com.rikkaus.wealth.shared.search.SearchText;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Builds the listing as a Criteria query with every value bound. Both orders end with the id, in the
 * same direction as the date, so two entries created in the same instant still have a fixed order.
 */
class CashFlowEntrySearchImpl implements CashFlowEntrySearch {

    private final EntityManager entityManager;

    CashFlowEntrySearchImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<CashFlowEntry> search(UUID ownerId, CashFlowEntryFilter filter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<CashFlowEntry> query = cb.createQuery(CashFlowEntry.class);
        Root<CashFlowEntry> entry = query.from(CashFlowEntry.class);

        List<Predicate> where = new ArrayList<>();
        where.add(cb.equal(entry.get("userId"), ownerId));
        SearchText.TextMatch match =
                filter.text() == null
                        ? null
                        : SearchText.matchesAny(cb, filter.text(), List.of(entry.<String>get("notes")));
        if (match != null) {
            where.add(match.predicate());
        }
        if (!filter.entryTypes().isEmpty()) {
            where.add(entry.get("entryType").in(filter.entryTypes()));
        }
        if (!filter.categories().isEmpty()) {
            where.add(entry.get("category").in(filter.categories()));
        }
        if (!filter.timings().isEmpty()) {
            where.add(entry.get("timing").in(filter.timings()));
        }
        if (!filter.recurrences().isEmpty()) {
            where.add(entry.get("recurrence").in(filter.recurrences()));
        }
        if (!filter.currencies().isEmpty()) {
            where.add(entry.get("currency").in(filter.currencies()));
        }
        if (filter.effectiveFrom() != null) {
            where.add(
                    cb.greaterThanOrEqualTo(
                            entry.<LocalDate>get("effectiveDate"), filter.effectiveFrom()));
        }
        if (filter.effectiveTo() != null) {
            where.add(
                    cb.lessThanOrEqualTo(
                            entry.<LocalDate>get("effectiveDate"), filter.effectiveTo()));
        }

        query.select(entry)
                .where(where.toArray(Predicate[]::new))
                .orderBy(order(cb, entry, filter.sort()));
        TypedQuery<CashFlowEntry> typed = entityManager.createQuery(query);
        return (match == null ? typed : match.bindTo(typed)).getResultList();
    }

    private static List<Order> order(
            CriteriaBuilder cb, Root<CashFlowEntry> entry, CashFlowEntrySort sort) {
        return switch (sort) {
            case EFFECTIVE_DATE_DESC ->
                    List.of(
                            cb.desc(entry.get("effectiveDate")),
                            cb.desc(entry.get("createdAt")),
                            cb.desc(entry.get("id")));
            case EFFECTIVE_DATE_ASC ->
                    List.of(
                            cb.asc(entry.get("effectiveDate")),
                            cb.asc(entry.get("createdAt")),
                            cb.asc(entry.get("id")));
        };
    }
}
