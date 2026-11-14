package com.rikkaus.wealth.assets.application;

import com.rikkaus.wealth.assets.domain.Asset;
import com.rikkaus.wealth.assets.domain.AssetCategory;
import com.rikkaus.wealth.assets.domain.AssetMetadata;
import com.rikkaus.wealth.assets.domain.AssetRepository;
import com.rikkaus.wealth.assets.domain.AssetValuation;
import com.rikkaus.wealth.assets.domain.AssetValuationRepository;
import com.rikkaus.wealth.assets.domain.DeclaredValuation;
import com.rikkaus.wealth.shared.error.ApiException;
import com.rikkaus.wealth.shared.error.ProblemType;
import com.rikkaus.wealth.shared.security.OwnershipGuard;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
 *
 * <p>No method changes an asset's value. Creating one writes the first valuation record, and every
 * later value is appended by {@code AssetValuationService}; this class never touches the snapshot
 * after creation.
 */
@Service
public class AssetService {

    private final AssetRepository assets;
    private final AssetValuationRepository valuations;
    private final Clock clock;

    AssetService(AssetRepository assets, AssetValuationRepository valuations, Clock clock) {
        this.assets = assets;
        this.valuations = valuations;
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
     * Adds an asset to the owner's catalog, together with the first record of its valuation history.
     *
     * <p>Two rows in one transaction: the asset, whose three valuation columns are a snapshot, and the
     * {@link AssetValuation} they are a snapshot of. Writing the record here rather than leaving the
     * history to start at the first later update is what makes the history complete — the design's
     * <em>initial</em> state is "only the first valuation", and a trend that omits the value an asset
     * was created with starts one point short.
     *
     * <p>The dates and the rate snapshot are checked together rather than one at a time, so a request
     * with two problems is one response naming both fields instead of two round trips.
     */
    @Transactional
    public Asset create(
            UUID ownerId, AssetCategory category, DeclaredValuation valuation, AssetMetadata metadata) {
        AssetMetadata normalized = normalize(metadata);
        List<Map<String, String>> errors =
                new ArrayList<>(dateErrors(valuation.effectiveDate(), normalized.purchaseDate()));
        errors.addAll(InputRules.rateSnapshotErrors(valuation, InputRules.today(clock)));
        if (!errors.isEmpty()) {
            throw InputRules.validationFailed(errors);
        }
        Instant now = clock.instant();
        Asset asset = assets.save(Asset.create(ownerId, category, valuation, normalized, now));
        valuations.save(AssetValuation.recordedFor(asset.getId(), valuation, now));
        return asset;
    }

    /**
     * Replaces the editable metadata of one of the owner's assets.
     *
     * <p>The valuation is not a parameter. The accepted design for issue #51 makes the current value
     * read-only in the edit form; a new value is appended to the history by {@code
     * AssetValuationService}, so there is no argument here a caller could use to overwrite it.
     *
     * <p>The row is locked for the whole method, with the same lock an append takes. Hibernate's
     * {@code UPDATE} writes every column of the entity, the three snapshot columns among them, so an
     * edit that read the asset before a concurrent append committed would write the old value, currency
     * and date straight back over the new ones — and the catalog would then disagree with the history
     * until the next append. Found by review, not by the manual run, which never sent an edit and an
     * append at once. Taking the lock makes the edit wait for the append and read what it wrote.
     *
     * <p>The purchase date is checked against the <em>earliest</em> valuation ever recorded, not only
     * the current one. A back-dated record may sit before the current valuation date, and a purchase
     * dated after it would make the history say the asset was valued before it was owned — and would
     * then refuse the correction of that very record. Reported under the message the catalog has always
     * used, because for an asset with no back-dated history the earliest record <em>is</em> the stored
     * valuation date.
     */
    @Transactional
    public Asset replaceMetadata(UUID ownerId, UUID assetId, AssetMetadata metadata) {
        Asset asset = OwnershipGuard.requireOwned(assets.findAndLockById(assetId), ownerId);
        AssetMetadata normalized = normalize(metadata);
        LocalDate earliestValuation =
                valuations
                        .findFirstByAssetIdOrderByEffectiveDateAsc(asset.getId())
                        .map(AssetValuation::getEffectiveDate)
                        .orElse(asset.getValuationDate());
        rejectImpossibleDates(earliestValuation, normalized.purchaseDate());
        asset.replaceMetadata(normalized, clock.instant());
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
     * Refuses a date the calendar cannot support.
     *
     * <p>These two rules cannot be bean-validation annotations. The first depends on the injected
     * clock, and the second is a comparison between two fields that for an edit do not even arrive in
     * the same request. They produce the same {@code errors} extension shape the identity and settings
     * slices produce, so a client has one error body to parse.
     */
    private void rejectImpossibleDates(LocalDate valuationDate, LocalDate purchaseDate) {
        List<Map<String, String>> errors = dateErrors(valuationDate, purchaseDate);
        if (!errors.isEmpty()) {
            throw InputRules.validationFailed(errors);
        }
    }

    private List<Map<String, String>> dateErrors(LocalDate valuationDate, LocalDate purchaseDate) {
        LocalDate today = InputRules.today(clock);
        List<Map<String, String>> errors = new ArrayList<>();
        if (valuationDate.isAfter(today)) {
            errors.add(InputRules.error("valuationDate", "must not be in the future"));
        }
        if (purchaseDate != null && purchaseDate.isAfter(today)) {
            errors.add(InputRules.error("purchaseDate", "must not be in the future"));
        }
        // Only when neither is already rejected for being in the future. A request dated next year in
        // both fields is one mistake, and reporting a third error about their order would describe a
        // problem the user does not have.
        if (errors.isEmpty() && purchaseDate != null && purchaseDate.isAfter(valuationDate)) {
            errors.add(
                    InputRules.error("purchaseDate", "must not be later than the valuation date"));
        }
        return errors;
    }

    /**
     * Cleans the three free-text fields.
     *
     * <p>What "clean" means — surrounding blank space in the widest sense removed, an optional field
     * left with nothing stored as {@code null}, a null character refused — is defined once in {@link
     * InputRules#cleaned}, because the valuation history has a free-text note that has to be judged by
     * the same rule.
     */
    private static AssetMetadata normalize(AssetMetadata metadata) {
        return new AssetMetadata(
                InputRules.requireName(metadata.name()),
                metadata.liquidity(),
                metadata.purchaseValue(),
                metadata.purchaseDate(),
                metadata.quantity(),
                InputRules.cleaned(metadata.institution(), "institution"),
                InputRules.cleaned(metadata.notes(), "notes"));
    }
}
