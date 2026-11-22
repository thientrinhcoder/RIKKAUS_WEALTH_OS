package com.rikkaus.wealth.cashflow.domain;

import java.util.List;
import java.util.UUID;

/**
 * The catalog listing, narrowed and ordered as the client asked.
 *
 * <p>A fragment with the owner as a required argument, not a {@code JpaSpecificationExecutor}: an
 * arbitrary specification is one missing predicate away from every user's entries.
 */
public interface CashFlowEntrySearch {

    List<CashFlowEntry> search(UUID ownerId, CashFlowEntryFilter filter);
}
