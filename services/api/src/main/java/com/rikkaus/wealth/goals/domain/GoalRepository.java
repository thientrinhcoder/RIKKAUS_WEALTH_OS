package com.rikkaus.wealth.goals.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/**
 * Goal persistence, declaring only the operations the slice uses.
 *
 * <p>Narrow for the same reason as {@code AssetRepository}: there is no {@code findAll}, because a
 * method that returns every user's goals would be one autocomplete away from a cross-user leak. The
 * only listing is owner-scoped and carries its order in its name — nearest target date first, then
 * name — so the list the client renders is the server's order rather than PostgreSQL's incidental one.
 *
 * <p>{@link #findById} takes no owner on purpose. A repository method that filtered by owner would
 * make "absent" and "someone else's" two different outcomes at this layer, and the single place that
 * decides they are indistinguishable is {@code OwnershipGuard}.
 *
 * <p>{@link #flush} exists so the service can catch a zero-row {@code UPDATE} or {@code DELETE} inside
 * the method, where it can become {@code not-found}, rather than at commit where it becomes a 500.
 */
public interface GoalRepository extends Repository<Goal, UUID> {

    List<Goal> findByUserIdOrderByTargetDateAscNameAsc(UUID userId);

    Optional<Goal> findById(UUID id);

    Goal save(Goal goal);

    void delete(Goal goal);

    void flush();
}
