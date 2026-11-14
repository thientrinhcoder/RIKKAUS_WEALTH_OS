package com.rikkaus.wealth.assets.application;

import com.rikkaus.wealth.assets.domain.Asset;
import com.rikkaus.wealth.assets.domain.AssetCategory;
import com.rikkaus.wealth.assets.domain.AssetInstrumentDetails;
import com.rikkaus.wealth.assets.domain.AssetMetadata;
import com.rikkaus.wealth.assets.domain.AssetRepository;
import com.rikkaus.wealth.assets.domain.AssetValuation;
import com.rikkaus.wealth.shared.error.ApiException;
import com.rikkaus.wealth.shared.error.ProblemType;
import com.rikkaus.wealth.shared.security.OwnershipGuard;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The asset slice's application service: list, create, read, edit the metadata, delete.
 *
 * <p>Every method takes the owner as its first argument and every caller gets that value from {@code
 * CurrentUser}, never from the request. There is no method that can be asked about a user other than
 * the one named, which is what makes another user's catalog unaddressable rather than protected by a
 * check somebody has to remember.
 *
 * <p>No method converts a USD value into VND. The user-declared rate belongs to the {@code settings}
 * slice, which the slice rule in {@code ArchitectureRulesTest} forbids this one from depending on, and
 * the client already reads the rate from {@code GET /api/v1/settings/exchange-rate}. Doing the
 * arithmetic here would also mean a stored asset carried a conversion made with whichever rate
 * happened to be current when it was written.
 */
@Service
public class AssetService {

    /**
     * The zone every user-entered date is judged in.
     *
     * <p>The {@link Clock} bean is UTC and a pilot user in Vietnam is seven hours ahead of it. Judging
     * "is this date in the future" in UTC would refuse a user who records today's valuation at nine in
     * the evening, because UTC still calls it yesterday and their date therefore looks like tomorrow.
     * {@code SettingsService} judges the declared exchange-rate date the same way and for the same
     * reason; the two must agree or the same calendar day would be valid on one resource and refused
     * on the other.
     */
    private static final ZoneId ENTERED_DATE_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final AssetRepository assets;
    private final Clock clock;

    AssetService(AssetRepository assets, Clock clock) {
        this.assets = assets;
        this.clock = clock;
    }

    /**
     * The owner's catalog, newest valuation first and then by name.
     *
     * <p>Ordered by the repository rather than by the client, because an unordered result from
     * PostgreSQL is not stable between runs and the catalog is rendered in arrival order. The name
     * breaks the tie so two assets valued on the same day do not swap places between requests.
     *
     * <p>Unpaginated, deliberately. The API conventions list pagination as absent, a pilot user's
     * catalog is tens of rows, and inventing a page shape here would commit the frontend to one before
     * anybody agreed it.
     */
    @Transactional(readOnly = true)
    public List<Asset> listFor(UUID ownerId) {
        return assets.findByUserIdOrderByValuationDateDescNameAsc(ownerId);
    }

    /** One of the owner's assets, or {@code not-found} if it is absent or somebody else's. */
    @Transactional(readOnly = true)
    public Asset read(UUID ownerId, UUID assetId) {
        return OwnershipGuard.requireOwned(assets.findById(assetId), ownerId);
    }

    /**
     * Adds an asset to the owner's catalog.
     *
     * <p>The dates are checked together rather than one at a time, so a request with two date problems
     * is one response naming both fields instead of two round trips.
     */
    @Transactional
    public Asset create(
            UUID ownerId,
            AssetCategory category,
            AssetValuation valuation,
            AssetMetadata metadata,
            AssetInstrumentDetails instrumentDetails) {
        AssetMetadata normalized = normalize(metadata);
        AssetInstrumentDetails normalizedDetails = normalize(instrumentDetails);
        rejectWhatTheAnnotationsCannotJudge(
                category, valuation.valuationDate(), normalized.purchaseDate(), normalizedDetails);
        return assets.save(
                Asset.create(
                        ownerId, category, valuation, normalized, normalizedDetails, clock.instant()));
    }

    /**
     * Replaces the editable metadata of one of the owner's assets.
     *
     * <p>The valuation is not a parameter. The accepted design for issue #51 makes the current value
     * read-only in the edit form and defers its update to issue #57's append-only history, so there is
     * no argument here a caller could use to overwrite it — and {@link Asset} has no mutator for it
     * either.
     *
     * <p>The purchase date is re-checked against the asset's <em>stored</em> valuation date, because
     * that is the one it has to precede and the edit cannot change it. The instrument fields are
     * checked against the asset's <em>stored</em> category for the same reason: the category is chosen
     * once in the create flow and no edit carries one, so a request cannot move an asset into a
     * category that would accept the field it is sending.
     */
    @Transactional
    public Asset replaceMetadata(
            UUID ownerId,
            UUID assetId,
            AssetMetadata metadata,
            AssetInstrumentDetails instrumentDetails) {
        Asset asset = OwnershipGuard.requireOwned(assets.findById(assetId), ownerId);
        AssetMetadata normalized = normalize(metadata);
        AssetInstrumentDetails normalizedDetails = normalize(instrumentDetails);
        rejectWhatTheAnnotationsCannotJudge(
                asset.getCategory(), asset.getValuationDate(), normalized.purchaseDate(), normalizedDetails);
        asset.replaceMetadata(normalized, normalizedDetails, clock.instant());
        Asset saved = assets.save(asset);
        flushTreatingAVanishedRowAsNotFound();
        return saved;
    }

    /**
     * Removes one of the owner's assets.
     *
     * <p>A plain delete. Undo, linked-record consequences and a retention policy belong to issue #102;
     * what this does is exactly what the generic destructive confirmation in the approved design
     * promises, and nothing more.
     *
     * <p>Ownership is resolved before anything is removed, so a non-owner's request returns {@code
     * not-found} with the row untouched rather than deleting it and then failing a check.
     */
    @Transactional
    public void delete(UUID ownerId, UUID assetId) {
        assets.delete(OwnershipGuard.requireOwned(assets.findById(assetId), ownerId));
        flushTreatingAVanishedRowAsNotFound();
    }

    /**
     * Sends the pending write now, and answers {@code not-found} if the row has gone.
     *
     * <p>Two requests can both read an asset, both pass the ownership guard, and then both try to
     * write it. The first commits; the second's {@code UPDATE} or {@code DELETE} matches zero rows,
     * Hibernate notices the row count it did not expect, and the failure arrives as an
     * {@link OptimisticLockingFailureException}. That is not an {@code ApiException} and not a Spring
     * MVC exception, so without this it falls through to the handler of last resort and the caller
     * gets a 500 for having double-tapped a confirm button.
     *
     * <p>Measured rather than assumed: four concurrent deletes of one asset, repeated 25 times,
     * returned 500 for 73 of the 100 requests before this existed. The run is recorded in
     * {@code plans/reports/manual-verification-261103-2231-GH-53-asset-catalog-common-crud.md}.
     *
     * <p>{@code not-found} is the honest answer, and it is the same one the sequential case already
     * gives: by the time this request's write ran, the asset was gone. The flush has to happen here,
     * inside the transaction and inside the method, because at commit time the proxy has already
     * returned and nothing in this class could catch it.
     *
     * <p>A row version would turn this into a different answer — a conflict the client could retry —
     * but there is no concurrent-edit feature to retry for: one pilot user on one device, and the
     * races that actually happen are a repeated submission of the same intent. Adding {@code @Version}
     * is a product decision about edit conflicts, not a fix for this.
     */
    private void flushTreatingAVanishedRowAsNotFound() {
        try {
            assets.flush();
        } catch (OptimisticLockingFailureException concurrentlyRemoved) {
            throw new ApiException(ProblemType.NOT_FOUND, ProblemType.NOT_FOUND.safeDetail());
        }
    }

    /**
     * Refuses the three rules that cannot be a bean-validation annotation, in one response.
     *
     * <p>None of them is expressible as an annotation. Whether a date is in the future depends on the
     * injected clock. Whether a purchase date precedes a valuation date is a comparison between two
     * fields that, for an edit, do not even arrive in the same request. Whether an instrument field
     * suits the asset's category is a relationship between a field and the category, which for an
     * edit is read from the stored row rather than from the body.
     *
     * <p>They are collected and thrown once rather than checked one at a time, so a request that got
     * three things wrong is one response naming all three instead of three round trips. The resulting
     * {@code errors} extension has the same shape the identity and settings slices produce, so a
     * client has one error body to parse.
     */
    private void rejectWhatTheAnnotationsCannotJudge(
            AssetCategory category,
            LocalDate valuationDate,
            LocalDate purchaseDate,
            AssetInstrumentDetails instrumentDetails) {
        List<Map<String, String>> errors = new ArrayList<>();
        addDateProblems(errors, valuationDate, purchaseDate);
        addCategoryApplicabilityProblems(errors, category, instrumentDetails);
        if (!errors.isEmpty()) {
            throw new ApiException(
                    ProblemType.VALIDATION_FAILED,
                    ProblemType.VALIDATION_FAILED.safeDetail(),
                    Map.of("errors", List.copyOf(errors)));
        }
    }

    /** A date the calendar cannot support. */
    private void addDateProblems(
            List<Map<String, String>> errors, LocalDate valuationDate, LocalDate purchaseDate) {
        LocalDate today = LocalDate.now(clock.withZone(ENTERED_DATE_ZONE));
        boolean anyDateIsInTheFuture = false;
        if (valuationDate.isAfter(today)) {
            errors.add(Map.of("field", "valuationDate", "message", "must not be in the future"));
            anyDateIsInTheFuture = true;
        }
        if (purchaseDate != null && purchaseDate.isAfter(today)) {
            errors.add(Map.of("field", "purchaseDate", "message", "must not be in the future"));
            anyDateIsInTheFuture = true;
        }
        // Only when neither is already rejected for being in the future. A request dated next year in
        // both fields is one mistake, and reporting a third error about their order would describe a
        // problem the user does not have. Judged on these two dates alone rather than on whether the
        // error list is empty, so an unrelated instrument problem cannot suppress this one.
        if (!anyDateIsInTheFuture && purchaseDate != null && purchaseDate.isAfter(valuationDate)) {
            errors.add(
                    Map.of("field", "purchaseDate", "message", "must not be later than the valuation date"));
        }
    }

    /**
     * An instrument field sent for a category the accepted design never shows it on.
     *
     * <p>The matrix lives on {@link AssetCategory} and is enforced again by
     * {@code assets_instrument_pricing_suits_the_category} and
     * {@code assets_interest_rate_suits_the_category} in {@code V5__asset_instrument_details.sql}.
     * This is the half that names the field, which is the difference between a user being told
     * <em>symbol must not be set for this asset category</em> and a 500 from a check constraint.
     *
     * <p>Refused rather than quietly dropped. The two are not equally honest: storing nothing and
     * answering 201 would discard a value the client sent without saying so, and a frontend bug would
     * then be invisible until somebody wondered where the ticker went.
     *
     * <p>It runs after the symbol has been cleaned, so a symbol submitted as {@code "   "} is a user
     * who typed nothing into an optional field and not a user sending a field their category refuses.
     *
     * <p>Fields are reported in the order the contract lists them, so two requests that are wrong in
     * the same way produce the same body.
     */
    private static void addCategoryApplicabilityProblems(
            List<Map<String, String>> errors,
            AssetCategory category,
            AssetInstrumentDetails instrumentDetails) {
        if (!category.allowsInstrumentPricing()) {
            refuseIfPresent(errors, "symbol", instrumentDetails.symbol());
            refuseIfPresent(errors, "costPerUnit", instrumentDetails.costPerUnit());
            refuseIfPresent(errors, "currentPricePerUnit", instrumentDetails.currentPricePerUnit());
        }
        if (!category.allowsRate()) {
            refuseIfPresent(errors, "interestRate", instrumentDetails.interestRate());
        }
    }

    private static void refuseIfPresent(
            List<Map<String, String>> errors, String field, Object value) {
        if (value != null) {
            errors.add(Map.of("field", field, "message", "must not be set for this asset category"));
        }
    }

    /**
     * Cleans the three free-text fields, and refuses the two things neither bean validation nor the
     * table can catch.
     *
     * <p>An institution submitted as {@code "   "} is a user who typed nothing into an optional field,
     * not a user whose institution is three spaces. Storing it verbatim would make the field look
     * answered in the catalog and would make "has an institution" untrue of a row that has one. So
     * surrounding blank space is removed and an optional field left with nothing becomes {@code null}.
     *
     * <p>What counts as blank is wider than any of the three obvious answers, which is why this is
     * written out rather than delegated. {@code trim()} removes only code points at or below
     * {@code U+0020}. {@code strip()} removes what {@code Character.isWhitespace} accepts, and that
     * method deliberately rejects the non-breaking spaces — {@code U+00A0} among them — because they
     * are not line-breaking opportunities. {@code btrim} in the table's check constraint removes the
     * ASCII space alone. A name of a single {@code U+00A0}, which a Vietnamese or CJK input method
     * emits with nothing visibly unusual about it, therefore passes {@code @NotBlank}, survives either
     * standard trim and satisfies the check constraint, and creates an asset that renders as an empty
     * row in the catalog. {@link #isBlankToAReader} is the union: whitespace as Java defines it, plus
     * every Unicode space separator.
     */
    private static AssetMetadata normalize(AssetMetadata metadata) {
        return new AssetMetadata(
                requireName(metadata.name()),
                metadata.liquidity(),
                metadata.purchaseValue(),
                metadata.purchaseDate(),
                metadata.quantity(),
                cleaned(metadata.institution(), "institution"),
                cleaned(metadata.notes(), "notes"));
    }

    /**
     * Cleans the one free-text instrument field.
     *
     * <p>{@code symbol} goes through the same {@link #cleaned} helper as the institution and the
     * notes, for the same two reasons: a value of only blank space is an optional field the user left
     * alone rather than a ticker made of spaces, and a null character cannot be stored in a
     * PostgreSQL {@code text} column at all and would otherwise reach the client as a 500.
     *
     * <p>The three numeric fields are not touched. Their bounds are annotations on the request and
     * check constraints on the table, and there is nothing about a {@link java.math.BigDecimal} to
     * clean: a scale the column cannot hold is refused by {@code @Digits} before it arrives here,
     * never rounded, because rounding a figure the user typed would store something other than what
     * they said.
     */
    private static AssetInstrumentDetails normalize(AssetInstrumentDetails instrumentDetails) {
        return new AssetInstrumentDetails(
                cleaned(instrumentDetails.symbol(), "symbol"),
                instrumentDetails.costPerUnit(),
                instrumentDetails.currentPricePerUnit(),
                instrumentDetails.interestRate());
    }

    /** The name with surrounding blank space removed, refusing one that was nothing else. */
    private static String requireName(String name) {
        String value = cleaned(name, "name");
        if (value == null) {
            throw validationFailed("name", "must not be blank");
        }
        return value;
    }

    /**
     * One free-text value, stripped, with a blank one reported as absent, and a null character
     * refused.
     *
     * <p><strong>The null character is the reason this method rejects rather than only cleans.</strong> PostgreSQL
     * cannot store {@code U+0000} in a {@code text} column at all, so a name of
     * {@code "Tiền\u0000mặt"} — which is legal JSON, satisfies {@code @NotBlank} and {@code @Size},
     * and would satisfy every check constraint on the table — fails inside the driver and reaches the
     * client as {@code internal-error} with a stack trace in the log. That contradicts this slice's
     * own stated contract, which is that a client input error is a named {@code validation-failed} and
     * not a 500 from PostgreSQL, and it hands an authenticated caller a one-line way to fill the error
     * log.
     *
     * <p>It is refused rather than silently stripped because the two are not equally honest: dropping
     * a character the client sent and answering 201 would store something other than what was asked
     * for, without saying so. Refusing names the field.
     *
     * <p>This was found by review and then measured. Before the fix, the trailing-null case answered
     * 201 because {@code trim()} happened to remove it, which is exactly why the first probe of it was
     * inconclusive; a null character anywhere else was always a 500.
     *
     * <p>An unpaired surrogate is deliberately not refused. It was tested and PostgreSQL stores and
     * returns it without complaint, so there is nothing here to protect against; refusing it would be
     * a rule invented for its own sake.
     */
    private static String cleaned(String value, String field) {
        if (value == null) {
            return null;
        }
        if (value.indexOf('\u0000') >= 0) {
            throw validationFailed(field, "must not contain a null character");
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

    private static ApiException validationFailed(String field, String message) {
        return new ApiException(
                ProblemType.VALIDATION_FAILED,
                ProblemType.VALIDATION_FAILED.safeDetail(),
                Map.of("errors", List.of(Map.of("field", field, "message", message))));
    }
}
