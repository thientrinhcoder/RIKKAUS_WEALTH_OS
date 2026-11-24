package com.rikkaus.wealth.goals.application;

import com.rikkaus.wealth.goals.domain.Goal;
import com.rikkaus.wealth.goals.domain.GoalFields;
import com.rikkaus.wealth.goals.domain.GoalFilter;
import com.rikkaus.wealth.goals.domain.GoalRepository;
import com.rikkaus.wealth.shared.deletion.DeletionImpact;
import com.rikkaus.wealth.shared.deletion.DeletionImpact.RecordType;
import com.rikkaus.wealth.shared.error.ApiException;
import com.rikkaus.wealth.shared.error.ProblemType;
import com.rikkaus.wealth.shared.security.OwnershipGuard;
import com.rikkaus.wealth.shared.validation.EnteredDates;
import com.rikkaus.wealth.shared.validation.FieldErrors;
import com.rikkaus.wealth.shared.validation.TextInput;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The goals slice's application service: list, create, read, replace, delete.
 *
 * <p>Every method takes the owner as its first argument and every caller gets that value from {@code
 * CurrentUser}, never from the request. There is no method that can be asked about a user other than
 * the one named, which is what makes another user's goals unaddressable rather than protected by a
 * check somebody has to remember.
 *
 * <p>Nothing here computes a projection. Completion percentage, remaining amount, remaining months,
 * required monthly contribution and the on-track status are issue #89's, and the response of this
 * slice carries only what the user entered.
 */
@Service
public class GoalService {

    /** What an omitted allocated amount means: nothing set aside yet, at the column's scale. */
    private static final BigDecimal NOTHING_ALLOCATED = new BigDecimal("0.00");

    private final GoalRepository goals;
    private final Clock clock;

    GoalService(GoalRepository goals, Clock clock) {
        this.goals = goals;
        this.clock = clock;
    }

    /**
     * The owner's goals, narrowed and ordered by {@code filter}; by default nearest target date first
     * and then by name. The owner is applied before any filter, inside the repository fragment.
     *
     * <p>Unpaginated, deliberately, as the asset catalog is: a pilot user has a handful of goals and the
     * API conventions list pagination as absent.
     */
    @Transactional(readOnly = true)
    public List<Goal> listFor(UUID ownerId, GoalFilter filter) {
        return goals.search(ownerId, filter);
    }

    /** One of the owner's goals, or {@code not-found} if it is absent or somebody else's. */
    @Transactional(readOnly = true)
    public Goal read(UUID ownerId, UUID goalId) {
        return OwnershipGuard.requireOwned(goals.findById(goalId), ownerId);
    }

    /**
     * Adds a goal for the owner.
     *
     * <p>The target date is new by definition here, so it must be after today.
     */
    @Transactional
    public Goal create(UUID ownerId, GoalFields fields) {
        GoalFields normalized = normalize(fields);
        rejectInconsistentFields(normalized, null);
        return goals.save(Goal.create(ownerId, normalized, clock.instant()));
    }

    /**
     * Replaces every user-entered field of one of the owner's goals.
     *
     * <p>The stored target date is passed to the consistency check so that keeping it is always
     * allowed. Without that, a goal whose date has passed could never be edited again — not even to fix
     * its name — because every {@code PUT} carries the date and the date would be "in the past". Only a
     * <em>changed</em> date has to be after today.
     */
    @Transactional
    public Goal replace(UUID ownerId, UUID goalId, GoalFields fields) {
        Goal goal = OwnershipGuard.requireOwned(goals.findById(goalId), ownerId);
        GoalFields normalized = normalize(fields);
        rejectInconsistentFields(normalized, goal.getTargetDate());
        goal.replace(normalized, clock.instant());
        Goal saved = goals.save(goal);
        flushTreatingAVanishedRowAsNotFound();
        return saved;
    }

    /**
     * What deleting one of the owner's goals would do, without doing it: nothing beyond the goal.
     *
     * <p>No table refers to a goal and a goal refers to no record, which is what the accepted design's
     * dialog says ("Các bản ghi tài sản, công nợ và dòng tiền không thay đổi"). The route exists so a
     * client asks the same question of every record type and gets an answer from the database rather
     * than from an assumption that could go stale when a goal gains a link.
     */
    @Transactional(readOnly = true)
    public DeletionImpact deletionImpact(UUID ownerId, UUID goalId) {
        Goal goal = OwnershipGuard.requireOwned(goals.findById(goalId), ownerId);
        return DeletionImpact.of(RecordType.GOAL, goal.getId(), List.of(), List.of());
    }

    /**
     * Removes one of the owner's goals.
     *
     * <p>Nothing refers to a goal, so there is no linked-record confirmation; no undo and no retention
     * window. Ownership is resolved before anything is removed, so a non-owner's request returns {@code
     * not-found} with the row untouched.
     */
    @Transactional
    public void delete(UUID ownerId, UUID goalId) {
        goals.delete(OwnershipGuard.requireOwned(goals.findById(goalId), ownerId));
        flushTreatingAVanishedRowAsNotFound();
    }

    /**
     * Sends the pending write now, and answers {@code not-found} if the row has gone.
     *
     * <p>Two requests can both read a goal, both pass the ownership guard, and both try to write it.
     * The first commits; the second's {@code UPDATE} or {@code DELETE} matches zero rows and Hibernate
     * raises a stale-state failure, which is neither an {@code ApiException} nor a Spring MVC exception
     * and would otherwise reach the handler of last resort as a 500. The asset slice measured that at
     * 73 of 100 concurrent deletes before it flushed inside the method; this slice inherits the fix
     * rather than the defect. A row version was considered there and rejected, because there is no
     * concurrent-edit feature for a conflict response to serve.
     */
    private void flushTreatingAVanishedRowAsNotFound() {
        try {
            goals.flush();
        } catch (OptimisticLockingFailureException concurrentlyRemoved) {
            throw new ApiException(ProblemType.NOT_FOUND, ProblemType.NOT_FOUND.safeDetail());
        }
    }

    /**
     * The two rules that cannot be bean-validation annotations, reported together.
     *
     * <p>The allocated amount is compared with the target in the same request, and the target date is
     * compared with the injected clock. Both produce the same {@code errors} extension shape the other
     * slices produce, so a client has one error body to parse. They run only after bean validation has
     * accepted the shape of each field, which is why a negative allocated amount is never also reported
     * as exceeding the target: the request never reaches this method.
     */
    private void rejectInconsistentFields(GoalFields fields, LocalDate storedTargetDate) {
        List<Map<String, String>> errors = new ArrayList<>();
        if (fields.currentAllocatedAmount().compareTo(fields.targetAmount()) > 0) {
            errors.add(
                    Map.of("field", "currentAllocatedAmount", "message", "must not exceed the target amount"));
        }
        boolean dateIsNew = storedTargetDate == null || !fields.targetDate().equals(storedTargetDate);
        if (dateIsNew && !fields.targetDate().isAfter(EnteredDates.today(clock))) {
            errors.add(Map.of("field", "targetDate", "message", "must be after today"));
        }
        FieldErrors.rejectIfAny(errors);
    }

    /**
     * Cleans the name and supplies the one default this slice has.
     *
     * <p>An omitted allocated amount becomes {@code 0.00}: a goal nothing has been set aside for has
     * zero allocated, and the column is {@code NOT NULL} so issue #89's completion percentage never
     * meets a null. The two other optional fields stay {@code null} when omitted, because for them the
     * design distinguishes "not stated" from "zero".
     */
    private static GoalFields normalize(GoalFields fields) {
        return new GoalFields(
                TextInput.required(fields.name(), "name"),
                fields.type(),
                fields.targetAmount(),
                fields.currentAllocatedAmount() == null ? NOTHING_ALLOCATED : fields.currentAllocatedAmount(),
                fields.targetDate(),
                fields.plannedMonthlyContribution(),
                fields.expectedAnnualReturnPercent());
    }
}
