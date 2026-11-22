package com.rikkaus.wealth.goals.domain;

import java.util.List;
import java.util.UUID;

/**
 * The goal listing, narrowed and ordered as the client asked, with the owner as a required argument so
 * no filter can widen it beyond the owner's own goals.
 */
public interface GoalSearch {

    List<Goal> search(UUID ownerId, GoalFilter filter);
}
