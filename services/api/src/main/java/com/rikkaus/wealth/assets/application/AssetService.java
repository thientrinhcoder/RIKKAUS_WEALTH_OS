package com.rikkaus.wealth.assets.application;

import com.rikkaus.wealth.assets.domain.Asset;
import com.rikkaus.wealth.assets.domain.AssetCategory;
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
            UUID ownerId, AssetCategory category, AssetValuation valuation, AssetMetadata metadata) {
        AssetMetadata normalized = normalize(metadata);
        rejectImpossibleDates(valuation.valuationDate(), normalized.purchaseDate());
        return assets.save(
                Asset.create(ownerId, category, valuation, normalized, clock.instant()));
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
     * that is the one it has to precede and the edit cannot change it.
     */
    @Transactional
    public Asset replaceMetadata(UUID ownerId, UUID assetId, AssetMetadata metadata) {
        Asset asset = OwnershipGuard.requireOwned(assets.findById(assetId), ownerId);
        AssetMetadata normalized = normalize(metadata);
        rejectImpossibleDates(asset.getValuationDate(), normalized.purchaseDate());
        asset.replaceMetadata(normalized, clock.instant());
        return assets.save(asset);
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
        LocalDate today = LocalDate.now(clock.withZone(ENTERED_DATE_ZONE));
        List<Map<String, String>> errors = new ArrayList<>();
        if (valuationDate.isAfter(today)) {
            errors.add(Map.of("field", "valuationDate", "message", "must not be in the future"));
        }
        if (purchaseDate != null && purchaseDate.isAfter(today)) {
            errors.add(Map.of("field", "purchaseDate", "message", "must not be in the future"));
        }
        // Only when neither is already rejected for being in the future. A request dated next year in
        // both fields is one mistake, and reporting a third error about their order would describe a
        // problem the user does not have.
        if (errors.isEmpty() && purchaseDate != null && purchaseDate.isAfter(valuationDate)) {
            errors.add(
                    Map.of("field", "purchaseDate", "message", "must not be later than the valuation date"));
        }
        if (!errors.isEmpty()) {
            throw new ApiException(
                    ProblemType.VALIDATION_FAILED,
                    ProblemType.VALIDATION_FAILED.safeDetail(),
                    Map.of("errors", List.copyOf(errors)));
        }
    }

    /**
     * Trims the free text and treats a blank optional string as absent.
     *
     * <p>An institution submitted as {@code "   "} is a user who typed nothing into an optional field,
     * not a user whose institution is three spaces. Storing it verbatim would make the field look
     * answered in the catalog and would make "has an institution" untrue of a row that has one.
     *
     * <p>The required name is only trimmed, never nulled: {@code @NotBlank} has already refused a blank
     * one at the boundary with the field named, which is a better answer than a {@code not-null}
     * violation from PostgreSQL.
     */
    private static AssetMetadata normalize(AssetMetadata metadata) {
        return new AssetMetadata(
                metadata.name().trim(),
                metadata.liquidity(),
                metadata.purchaseValue(),
                metadata.purchaseDate(),
                metadata.quantity(),
                blankToNull(metadata.institution()),
                blankToNull(metadata.notes()));
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
