package com.rikkaus.wealth.cashflow.application;

import com.rikkaus.wealth.cashflow.domain.CashFlowEntry;
import com.rikkaus.wealth.cashflow.domain.CashFlowEntryDetails;
import com.rikkaus.wealth.cashflow.domain.CashFlowEntryRepository;
import com.rikkaus.wealth.cashflow.domain.CashFlowTiming;
import com.rikkaus.wealth.shared.error.ApiException;
import com.rikkaus.wealth.shared.error.ProblemType;
import com.rikkaus.wealth.shared.security.OwnershipGuard;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The cash-flow slice's application service: list, create, read, replace, delete.
 *
 * <p>Every method takes the owner as its first argument and every caller gets that value from {@code
 * CurrentUser}, never from the request. There is no method that can be asked about a user other than
 * the one named, which is what makes another user's catalog unaddressable rather than protected by a
 * check somebody has to remember.
 *
 * <p>No method converts a USD amount into VND, sums a month, or expands a recurring entry into the
 * months it falls in. The rate belongs to {@code settings}, the totals to issue #77 and the expansion
 * to {@link CashFlowOccurrenceService}. An entry is the user's statement of an amount and when it applies, and that is all
 * this class stores.
 *
 * <p>No date here is compared with today. An expected salary starting next January is a legitimate
 * entry — it is exactly the input the three-month forecast needs — so, unlike a valuation date, an
 * effective date in the future is not a mistake to refuse.
 */
@Service
public class CashFlowEntryService {

    private final CashFlowEntryRepository entries;
    private final Clock clock;

    CashFlowEntryService(CashFlowEntryRepository entries, Clock clock) {
        this.entries = entries;
        this.clock = clock;
    }

    /**
     * The owner's catalog, newest effective date first and then newest created first.
     *
     * <p>Ordered by the repository rather than by the client, because an unordered result from
     * PostgreSQL is not stable between runs and the catalog is rendered in arrival order.
     *
     * <p>Unpaginated, deliberately. The API conventions list pagination as absent, a pilot user's
     * catalog is tens of rows, and inventing a page shape here would commit the frontend to one before
     * anybody agreed it.
     */
    @Transactional(readOnly = true)
    public List<CashFlowEntry> listFor(UUID ownerId) {
        return entries.findByUserIdOrderByEffectiveDateDescCreatedAtDesc(ownerId);
    }

    /** One of the owner's entries, or {@code not-found} if it is absent or somebody else's. */
    @Transactional(readOnly = true)
    public CashFlowEntry read(UUID ownerId, UUID entryId) {
        return OwnershipGuard.requireOwned(entries.findById(entryId), ownerId);
    }

    /** Adds an entry to the owner's catalog. */
    @Transactional
    public CashFlowEntry create(UUID ownerId, CashFlowEntryDetails details) {
        CashFlowEntryDetails accepted = validated(ownerId, details);
        return entries.save(CashFlowEntry.create(ownerId, accepted, clock.instant()));
    }

    /**
     * Replaces every user-stated field of one of the owner's entries.
     *
     * <p>Ownership is resolved before the body is examined, so a non-owner learns {@code not-found}
     * and nothing about which of their values would have been refused.
     */
    @Transactional
    public CashFlowEntry replace(UUID ownerId, UUID entryId, CashFlowEntryDetails details) {
        CashFlowEntry entry = OwnershipGuard.requireOwned(entries.findById(entryId), ownerId);
        CashFlowEntryDetails accepted = validated(ownerId, details);
        entry.replace(accepted, clock.instant());
        CashFlowEntry saved = entries.save(entry);
        flushTreatingAVanishedRowAsNotFound();
        return saved;
    }

    /**
     * Removes one of the owner's entries.
     *
     * <p>A plain delete. Undo and a retention policy belong to issue #104; what this does is exactly
     * what the generic destructive confirmation in the approved design promises, and nothing more.
     *
     * <p>Ownership is resolved before anything is removed, so a non-owner's request returns {@code
     * not-found} with the row untouched rather than deleting it and then failing a check.
     */
    @Transactional
    public void delete(UUID ownerId, UUID entryId) {
        entries.delete(OwnershipGuard.requireOwned(entries.findById(entryId), ownerId));
        flushTreatingAVanishedRowAsNotFound();
    }

    /**
     * Sends the pending write now, and answers {@code not-found} if the row has gone.
     *
     * <p>Two requests can both read an entry, both pass the ownership guard, and then both try to
     * write it. The first commits; the second's {@code UPDATE} or {@code DELETE} matches zero rows,
     * Hibernate notices the row count it did not expect, and the failure arrives as an
     * {@link OptimisticLockingFailureException}. That is not an {@code ApiException} and not a Spring
     * MVC exception, so without this it falls through to the handler of last resort and the caller
     * gets a 500 for having double-tapped a confirm button. The asset slice measured that at 73 of 100
     * requests before it added the same guard.
     *
     * <p>{@code not-found} is the honest answer, and it is the same one the sequential case already
     * gives: by the time this request's write ran, the entry was gone. The flush has to happen here,
     * inside the transaction and inside the method, because at commit time the proxy has already
     * returned and nothing in this class could catch it.
     */
    private void flushTreatingAVanishedRowAsNotFound() {
        try {
            entries.flush();
        } catch (OptimisticLockingFailureException concurrentlyRemoved) {
            throw new ApiException(ProblemType.NOT_FOUND, ProblemType.NOT_FOUND.safeDetail());
        }
    }

    /**
     * Applies every rule that is not a single-field annotation, and returns the details as stored.
     *
     * <p>The rules are collected rather than thrown one at a time, so a request with three problems is
     * one response naming all three fields instead of three round trips. They produce the same {@code
     * errors} extension shape bean validation produces, so a client has one error body to parse.
     *
     * <ul>
     *   <li>The category must belong to the entry type. {@code SALARY} under {@code EXPENSE} is not a
     *       negative salary; it is a mismatched pair, and the table refuses it too.
     *   <li>A one-time entry has no recurrence and no end date; a recurring one must have a recurrence.
     *       Both halves are stated, so a request cannot be half of each.
     *   <li>An end date may equal the effective date — a recurrence of one occurrence — but may not
     *       precede it.
     *   <li>A linked asset or liability must exist and belong to the owner. The two failures are one
     *       error with one message on purpose: distinguishing them would let a caller confirm which
     *       identifiers exist, which is the enumeration oracle {@code OwnershipGuard} closes on every
     *       other route.
     *   <li>At most one link. The backbone says an entry may reference one asset or one liability.
     * </ul>
     */
    private CashFlowEntryDetails validated(UUID ownerId, CashFlowEntryDetails details) {
        List<Map<String, String>> errors = new ArrayList<>();

        if (details.category().entryType() != details.entryType()) {
            errors.add(error("category", "must belong to the entry type"));
        }

        if (details.timing() == CashFlowTiming.ONE_TIME) {
            if (details.recurrence() != null) {
                errors.add(error("recurrence", "must be omitted for a one-time entry"));
            }
            if (details.endDate() != null) {
                errors.add(error("endDate", "must be omitted for a one-time entry"));
            }
        } else if (details.recurrence() == null) {
            errors.add(error("recurrence", "must be present for a recurring entry"));
        }

        // Only judged when the end date is allowed to exist at all. A one-time entry carrying an end
        // date is one mistake, and reporting a second error about its order would describe a problem
        // the user does not have.
        if (details.timing() == CashFlowTiming.RECURRING
                && details.endDate() != null
                && details.endDate().isBefore(details.effectiveDate())) {
            errors.add(error("endDate", "must not be before the effective date"));
        }

        if (details.linkedAssetId() != null
                && !entries.assetBelongsTo(details.linkedAssetId(), ownerId)) {
            errors.add(error("linkedAssetId", "must reference one of the signed-in user's assets"));
        }
        if (details.linkedLiabilityId() != null) {
            if (details.linkedAssetId() != null) {
                // Judged before ownership, and the ownership query is not run: an entry that names two
                // records is one mistake, and reporting whether the second one is the user's would
                // describe a problem the user does not have.
                errors.add(error("linkedLiabilityId", "must not be combined with linkedAssetId"));
            } else if (!entries.liabilityBelongsTo(details.linkedLiabilityId(), ownerId)) {
                errors.add(
                        error("linkedLiabilityId", "must reference one of the signed-in user's liabilities"));
            }
        }

        String notes = details.notes();
        if (notes != null && notes.indexOf('\u0000') >= 0) {
            errors.add(error("notes", "must not contain a null character"));
        } else {
            notes = stripped(notes);
        }

        if (!errors.isEmpty()) {
            throw new ApiException(
                    ProblemType.VALIDATION_FAILED,
                    ProblemType.VALIDATION_FAILED.safeDetail(),
                    Map.of("errors", List.copyOf(errors)));
        }

        // The column is NUMERIC(19, 2), so a value sent as 45000000 reads back as 45000000.00 on the
        // next GET. Setting the scale here makes the create response say what every later read will
        // say, instead of echoing the caller's own formatting once and the database's forever after.
        // @Digits has already refused anything with more than two decimals, so nothing is rounded.
        return new CashFlowEntryDetails(
                details.entryType(),
                details.category(),
                details.amount().setScale(2),
                details.currency(),
                details.timing(),
                details.recurrence(),
                details.effectiveDate(),
                details.endDate(),
                details.linkedAssetId(),
                details.linkedLiabilityId(),
                notes);
    }

    /**
     * One free-text value with surrounding blank space removed, and a blank one reported as absent.
     *
     * <p>The same rule {@code AssetService} applies to its three text fields. What counts as blank is
     * wider than {@code trim()} or {@code strip()} know: a note of a single non-breaking space, which a
     * Vietnamese input method emits with nothing visibly unusual about it, would otherwise be stored
     * as a note that renders as nothing. Java whitespace plus every Unicode space separator is the
     * union that closes it.
     *
     * <p>The null character is refused by the caller before this runs, for the reason the asset slice
     * recorded: PostgreSQL cannot store {@code U+0000} in a {@code text} column, so a note carrying one
     * — legal JSON that satisfies {@code @Size} — would fail inside the driver and reach the client as
     * {@code internal-error}. It is refused rather than silently stripped, because dropping a character
     * the client sent and answering 201 would store something other than what was asked for.
     */
    private static String stripped(String value) {
        if (value == null) {
            return null;
        }
        int start = 0;
        int end = value.length();
        while (start < end && isBlankToAReader(value.codePointAt(start))) {
            start += Character.charCount(value.codePointAt(start));
        }
        while (end > start && isBlankToAReader(value.codePointBefore(end))) {
            end -= Character.charCount(value.codePointBefore(end));
        }
        String stripped = value.substring(start, end);
        return stripped.isEmpty() ? null : stripped;
    }

    /** Whitespace as Java defines it, plus the space separators it deliberately excludes. */
    private static boolean isBlankToAReader(int codePoint) {
        return Character.isWhitespace(codePoint)
                || Character.getType(codePoint) == Character.SPACE_SEPARATOR;
    }

    private static Map<String, String> error(String field, String message) {
        return Map.of("field", field, "message", message);
    }
}
