package com.rikkaus.wealth.liabilities.application;

import com.rikkaus.wealth.liabilities.domain.Liability;
import com.rikkaus.wealth.liabilities.domain.LiabilityCategory;
import com.rikkaus.wealth.liabilities.domain.LiabilityDetails;
import com.rikkaus.wealth.liabilities.domain.LiabilityRepository;
import com.rikkaus.wealth.liabilities.domain.LiabilityTerms;
import com.rikkaus.wealth.liabilities.domain.PaymentSchedule;
import com.rikkaus.wealth.shared.error.ApiException;
import com.rikkaus.wealth.shared.error.ProblemType;
import com.rikkaus.wealth.shared.security.OwnershipGuard;
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
 * The liability slice's application service: list, create, read, replace, delete.
 *
 * <p>Every method takes the owner as its first argument and every caller gets that value from {@code
 * CurrentUser}, never from the request, so another user's catalog is unaddressable rather than
 * protected by a check somebody has to remember.
 *
 * <p>Nothing here computes from the terms. There is no amortisation, no interest accrual and no
 * expansion of the recurring payment into dated cash flows: the accepted design for issue #60 records
 * what the user states, and the projection that would consume it belongs to issues #74 and #77. Nor
 * does anything convert a USD balance to VND; the declared rate lives in {@code settings}, which the
 * slice rule forbids this one from reaching.
 */
@Service
public class LiabilityService {

    private static final String COLLATERAL_FIELD = "collateralAssetId";

    private final LiabilityRepository liabilities;
    private final Clock clock;

    LiabilityService(LiabilityRepository liabilities, Clock clock) {
        this.liabilities = liabilities;
        this.clock = clock;
    }

    /** The owner's catalog, soonest payment first, undated ones last, then by name. Unpaginated. */
    @Transactional(readOnly = true)
    public List<Liability> listFor(UUID ownerId) {
        return liabilities.findByOwnerOrderBySoonestPayment(ownerId);
    }

    /** One of the owner's liabilities, or {@code not-found} if it is absent or somebody else's. */
    @Transactional(readOnly = true)
    public Liability read(UUID ownerId, UUID liabilityId) {
        return OwnershipGuard.requireOwned(liabilities.findById(liabilityId), ownerId);
    }

    /** Adds a liability to the owner's catalog. */
    @Transactional
    public Liability create(UUID ownerId, LiabilityCategory category, LiabilityDetails details) {
        LiabilityDetails accepted = validated(ownerId, details);
        return liabilities.save(Liability.create(ownerId, category, accepted, clock.instant()));
    }

    /**
     * Replaces every editable member of one of the owner's liabilities.
     *
     * <p>The collateral is re-checked against the viewer on every replacement, not only on the first
     * link, because the edit is where a user would substitute an identifier they do not own.
     */
    @Transactional
    public Liability replace(UUID ownerId, UUID liabilityId, LiabilityDetails details) {
        Liability liability = OwnershipGuard.requireOwned(liabilities.findById(liabilityId), ownerId);
        liability.replace(validated(ownerId, details), clock.instant());
        Liability saved = liabilities.save(liability);
        flushTreatingAVanishedRowAsNotFound();
        return saved;
    }

    /**
     * Removes one of the owner's liabilities. A plain delete; undo and a retention policy are issue
     * #104's. Ownership is resolved before anything is removed.
     */
    @Transactional
    public void delete(UUID ownerId, UUID liabilityId) {
        liabilities.delete(OwnershipGuard.requireOwned(liabilities.findById(liabilityId), ownerId));
        flushTreatingAVanishedRowAsNotFound();
    }

    /**
     * Sends the pending write now, and answers {@code not-found} if the row has gone, for the reason
     * {@code AssetService} measured: two overlapping writes to one row otherwise reach the client as a
     * 500 for having double-tapped a confirm button.
     */
    private void flushTreatingAVanishedRowAsNotFound() {
        try {
            liabilities.flush();
        } catch (OptimisticLockingFailureException concurrentlyRemoved) {
            throw new ApiException(ProblemType.NOT_FOUND, ProblemType.NOT_FOUND.safeDetail());
        }
    }

    /**
     * Cleans the free text, then refuses everything bean validation cannot see: the relationships
     * between fields, and the collateral's owner.
     *
     * <p>Every problem is collected into one {@code errors} array, so a request with a maturity before
     * its start <em>and</em> a collateral it does not own is one response naming both rather than two
     * round trips. The shape is the one the identity, settings and assets slices already produce.
     */
    private LiabilityDetails validated(UUID ownerId, LiabilityDetails details) {
        LiabilityDetails normalized =
                new LiabilityDetails(
                        requireName(details.name()),
                        details.outstandingBalance(),
                        details.currency(),
                        details.terms(),
                        details.schedule(),
                        details.collateralAssetId(),
                        cleaned(details.notes(), "notes"));

        List<Map<String, String>> errors = new ArrayList<>();
        rejectInconsistentTerms(normalized.terms(), normalized.schedule(), errors);
        rejectHalfASchedule(normalized.schedule(), errors);
        rejectCollateralTheViewerDoesNotOwn(ownerId, normalized.collateralAssetId(), errors);
        if (!errors.isEmpty()) {
            throw new ApiException(
                    ProblemType.VALIDATION_FAILED,
                    ProblemType.VALIDATION_FAILED.safeDetail(),
                    Map.of("errors", List.copyOf(errors)));
        }
        return normalized;
    }

    /**
     * The three date rules: maturity not before start, next payment not before start, next payment not
     * after maturity.
     *
     * <p>The third is skipped when the second-to-last already fired. A maturity that precedes the start
     * is one mistake, and a next payment that falls outside that inverted window would be reported as
     * a second problem the user does not have once the first is fixed.
     */
    private static void rejectInconsistentTerms(
            LiabilityTerms terms, PaymentSchedule schedule, List<Map<String, String>> errors) {
        LocalDate start = terms.startDate();
        LocalDate maturity = terms.maturityDate();
        LocalDate next = schedule.nextPaymentDate();
        boolean maturityInverted = start != null && maturity != null && maturity.isBefore(start);
        if (maturityInverted) {
            errors.add(Map.of("field", "maturityDate", "message", "must not be before the start date"));
        }
        if (next != null && start != null && next.isBefore(start)) {
            errors.add(Map.of("field", "nextPaymentDate", "message", "must not be before the start date"));
        } else if (next != null && maturity != null && !maturityInverted && next.isAfter(maturity)) {
            errors.add(Map.of("field", "nextPaymentDate", "message", "must not be after the maturity date"));
        }
    }

    /** The amount and the frequency come as a pair; a half-stated schedule names the missing half. */
    private static void rejectHalfASchedule(PaymentSchedule schedule, List<Map<String, String>> errors) {
        boolean hasAmount = schedule.recurringPaymentAmount() != null;
        boolean hasFrequency = schedule.recurringPaymentFrequency() != null;
        if (hasAmount && !hasFrequency) {
            errors.add(
                    Map.of(
                            "field",
                            "recurringPaymentFrequency",
                            "message",
                            "must be present when a recurring payment amount is given"));
        }
        if (hasFrequency && !hasAmount) {
            errors.add(
                    Map.of(
                            "field",
                            "recurringPaymentAmount",
                            "message",
                            "must be present when a recurring payment frequency is given"));
        }
    }

    /**
     * A collateral identifier must name an asset the viewer owns.
     *
     * <p>One message for both "no such asset" and "somebody else's asset", on purpose. Telling them
     * apart would turn this field into an oracle for which asset identifiers exist — the same
     * disclosure {@code OwnershipGuard} refuses on the path, refused here on the body. It is a {@code
     * validation-failed} naming the field rather than a {@code not-found}, because the resource the
     * request addresses is the liability, which is fine; it is one value in the body that is wrong.
     */
    private void rejectCollateralTheViewerDoesNotOwn(
            UUID ownerId, UUID collateralAssetId, List<Map<String, String>> errors) {
        if (collateralAssetId != null && !liabilities.ownsAsset(collateralAssetId, ownerId)) {
            errors.add(
                    Map.of(
                            "field",
                            COLLATERAL_FIELD,
                            "message",
                            "must identify an asset the signed-in user owns"));
        }
    }

    /** The name with surrounding blank space removed, refusing one that was nothing else. */
    private static String requireName(String name) {
        String value = cleaned(name, "name");
        if (value == null) {
            throw new ApiException(
                    ProblemType.VALIDATION_FAILED,
                    ProblemType.VALIDATION_FAILED.safeDetail(),
                    Map.of("errors", List.of(Map.of("field", "name", "message", "must not be blank"))));
        }
        return value;
    }

    /**
     * One free-text value, stripped, with a blank one reported as absent and a null character refused.
     *
     * <p>The same rule {@code AssetService} applies, for the same two measured defects: PostgreSQL
     * cannot store {@code U+0000} in a {@code text} column, so it is refused with the field named
     * rather than reaching the client as a 500; and a name of a single non-breaking space passes
     * {@code @NotBlank}, {@code trim()}, {@code strip()} and {@code btrim} alike, so blank here is the
     * union of Java whitespace and every Unicode space separator. Repeated in this slice rather than
     * lifted into {@code shared}, because two slices is not yet the pattern that justifies a shared
     * text module; the third one should lift it.
     */
    private static String cleaned(String value, String field) {
        if (value == null) {
            return null;
        }
        if (value.indexOf('\u0000') >= 0) {
            throw new ApiException(
                    ProblemType.VALIDATION_FAILED,
                    ProblemType.VALIDATION_FAILED.safeDetail(),
                    Map.of(
                            "errors",
                            List.of(Map.of("field", field, "message", "must not contain a null character"))));
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

    private static boolean isBlankToAReader(int codePoint) {
        return Character.isWhitespace(codePoint)
                || Character.getType(codePoint) == Character.SPACE_SEPARATOR;
    }
}
