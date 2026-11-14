package com.rikkaus.wealth.cashflow.application;

import com.rikkaus.wealth.cashflow.domain.CashFlowEntryRepository;
import com.rikkaus.wealth.cashflow.domain.CashFlowOccurrence;
import com.rikkaus.wealth.cashflow.domain.CashFlowSchedule;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lists the dates the owner's entries fall on within a window.
 *
 * <p>Computed on every read and never stored. An occurrence is a reading of its entry's rule, so
 * editing or deleting the entry changes its occurrences on the next request with nothing to keep in
 * step. Splitting a series, or editing one occurrence on its own, are open questions for the feature
 * contract and are deliberately not anticipated by a table here.
 *
 * <p>The owner is the only user whose entries are read, and every caller gets that value from {@code
 * CurrentUser}. There is no identifier to guess: another user's entries are not filtered out, they
 * are never loaded.
 */
@Service
public class CashFlowOccurrenceService {

    /**
     * Earliest first, and entries on the same day by identifier as text. Both keys are fixed by the
     * data, so the same catalog and window always produce the same bytes, which is what lets two
     * reports over the same month agree.
     */
    private static final Comparator<CashFlowOccurrence> ORDER =
            Comparator.comparing(CashFlowOccurrence::date)
                    .thenComparing(occurrence -> occurrence.entry().getId().toString());

    private final CashFlowEntryRepository entries;

    CashFlowOccurrenceService(CashFlowEntryRepository entries) {
        this.entries = entries;
    }

    @Transactional(readOnly = true)
    public List<CashFlowOccurrence> occurrencesFor(UUID ownerId, OccurrenceWindow window) {
        return entries.findActiveBetween(ownerId, window.from(), window.to()).stream()
                .flatMap(entry -> CashFlowSchedule.occurrencesOf(entry, window.from(), window.to()).stream())
                .sorted(ORDER)
                .toList();
    }
}
