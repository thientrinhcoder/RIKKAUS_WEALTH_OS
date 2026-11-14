package com.rikkaus.wealth.assets.application;

import com.rikkaus.wealth.assets.domain.Asset;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The valuation history: list it, read one record, append one.
 *
 * <p>There is no method that changes or removes a record. That is the backbone's rule — a change of
 * value creates a record — expressed as an absence rather than a check: the repository declares no
 * delete, the entity has no mutator, and this class has no method to call either from.
 *
 * <p>Every method takes the owner as its first argument and every caller gets that value from {@code
 * CurrentUser}, never from the request. Ownership is decided on the <em>asset</em>, once, by {@code
 * OwnershipGuard}; a record is then reachable only through an asset the viewer owns, and a valuation
 * identifier that belongs to another asset is {@code not-found} exactly as another user's asset is.
 *
 * <p>No method converts a USD value into VND. The rate a record carries is the one the user applied,
 * sent by the client as the snapshot the accepted design calls <em>Tỷ giá USD/VND đã dùng</em>, and is
 * never read from the {@code settings} slice — which the slice rule in {@code ArchitectureRulesTest}
 * forbids this one from reaching. The client multiplies, as it already does for the catalog.
 */
@Service
public class AssetValuationService {

    private final AssetRepository assets;
    private final AssetValuationRepository valuations;
    private final Clock clock;

    AssetValuationService(AssetRepository assets, AssetValuationRepository valuations, Clock clock) {
        this.assets = assets;
        this.valuations = valuations;
        this.clock = clock;
    }

    /**
     * Every valuation of one of the owner's assets, current record first.
     *
     * <p>The order is the definition of "current": latest effective date, and among records sharing a
     * date the one recorded last. The first entry is therefore the current one by construction, and
     * the flag on it is derived from the position rather than looked up, so the two cannot disagree.
     */
    @Transactional(readOnly = true)
    public List<ValuationEntry> listFor(UUID ownerId, UUID assetId) {
        Asset asset = OwnershipGuard.requireOwned(assets.findById(assetId), ownerId);
        List<AssetValuation> history =
                valuations.findByAssetIdOrderByEffectiveDateDescRecordedAtDesc(asset.getId());
        List<ValuationEntry> entries = new ArrayList<>(history.size());
        for (int position = 0; position < history.size(); position++) {
            entries.add(new ValuationEntry(history.get(position), position == 0));
        }
        return List.copyOf(entries);
    }

    /**
     * One valuation of one of the owner's assets.
     *
     * <p>Two lookups and both have to succeed: the asset must be the viewer's, and the record must be
     * that asset's. A record identifier from another asset — the viewer's own or anybody else's — is
     * absent at this layer, which is what keeps a valuation identifier from being a way around the
     * ownership of the asset it belongs to.
     */
    @Transactional(readOnly = true)
    public ValuationEntry read(UUID ownerId, UUID assetId, UUID valuationId) {
        Asset asset = OwnershipGuard.requireOwned(assets.findById(assetId), ownerId);
        AssetValuation valuation =
                valuations
                        .findByIdAndAssetId(valuationId, asset.getId())
                        .orElseThrow(AssetValuationService::notFound);
        boolean current =
                valuations
                        .findFirstByAssetIdOrderByEffectiveDateDescRecordedAtDesc(asset.getId())
                        .map(latest -> latest.getId().equals(valuation.getId()))
                        .orElse(false);
        return new ValuationEntry(valuation, current);
    }

    /**
     * Appends a valuation to one of the owner's assets and, when it is the newest, makes it the
     * asset's current value — in one transaction.
     *
     * <p>The asset row is locked for the whole method. The decision "does this record supersede the
     * current one" is a read of the asset's current date followed by a write of its snapshot, and two
     * appends that interleave between the two would both decide yes and leave the snapshot equal to
     * whichever {@code UPDATE} ran last rather than to the record the order says is current. With the
     * lock the second append waits for the first to commit, reads the date it wrote, and decides
     * against it. The same lock makes a concurrent delete wait or fail first, so a record is never
     * written for an asset that is gone.
     *
     * <p>A record dated earlier than the current one is appended and changes nothing else. It is
     * history — a user filling in last year's values for a trend — and the catalog keeps showing the
     * newest. A record dated the same day is a correction and does supersede, because the later
     * recorded of two same-day records is the one the user means. {@link
     * AssetValuation#supersedesARecordDated} is where that line is drawn.
     */
    @Transactional
    public ValuationEntry record(UUID ownerId, UUID assetId, DeclaredValuation declared) {
        Asset asset = OwnershipGuard.requireOwned(assets.findAndLockById(assetId), ownerId);
        DeclaredValuation accepted = accept(asset, declared);
        Instant now = clock.instant();
        AssetValuation valuation =
                valuations.save(AssetValuation.recordedFor(asset.getId(), accepted, now));
        boolean current = valuation.supersedesARecordDated(asset.getValuationDate());
        if (current) {
            asset.adoptValuation(valuation, now);
            assets.save(asset);
        }
        return new ValuationEntry(valuation, current);
    }

    /**
     * Applies every rule that depends on the asset, the clock or the history, and returns the
     * declaration with its note cleaned.
     *
     * <p>All of it is collected into one response. A user who got the currency and the rate wrong at
     * once is told both, not the first and then the second a round trip later.
     *
     * <p>The currency must be the asset's. A holding's currency is a property of the holding — {@code
     * V4__assets.sql} says so, and {@code purchaseValue} is denominated in it — so a record in the
     * other currency would make the asset's own fields disagree about what unit they are in. The record
     * still stores the currency, as the backbone requires each one to, so relaxing this later is a rule
     * removed here and not a migration.
     *
     * <p>The effective date may not precede the purchase date, which is the rule the catalog already
     * applies from the other side when it refuses a purchase after the valuation. Reported against
     * {@code effectiveDate} because that is the field this request carries.
     *
     * <p>A correction must name a record of this asset — the table's composite foreign key refuses any
     * other — and must say why, because the accepted design makes <em>Lý do điều chỉnh</em> mandatory
     * and a correction with no reason is a second value with nothing to tell a reader which to trust.
     * The message for a record that is not this asset's is the same whether the identifier is missing,
     * another asset's or another user's; distinguishing them would be a small enumeration oracle.
     */
    private DeclaredValuation accept(Asset asset, DeclaredValuation declared) {
        LocalDate today = InputRules.today(clock);
        List<Map<String, String>> errors = new ArrayList<>();
        if (declared.currency() != asset.getCurrency()) {
            errors.add(InputRules.error("currency", "must match the asset's currency"));
        }
        if (declared.effectiveDate().isAfter(today)) {
            errors.add(InputRules.error("effectiveDate", "must not be in the future"));
        } else if (asset.getPurchaseDate() != null
                && declared.effectiveDate().isBefore(asset.getPurchaseDate())) {
            errors.add(
                    InputRules.error("effectiveDate", "must not be earlier than the purchase date"));
        }
        errors.addAll(InputRules.rateSnapshotErrors(declared, today));
        String note = InputRules.cleaned(declared.note(), "note");
        if (declared.correctsValuationId() != null) {
            if (valuations.findByIdAndAssetId(declared.correctsValuationId(), asset.getId()).isEmpty()) {
                errors.add(
                        InputRules.error(
                                "correctsValuationId", "must identify a valuation of this asset"));
            }
            if (note == null) {
                errors.add(InputRules.error("note", "must be given when correcting a valuation"));
            }
        }
        if (!errors.isEmpty()) {
            throw InputRules.validationFailed(errors);
        }
        return declared.withNote(note);
    }

    private static ApiException notFound() {
        return new ApiException(ProblemType.NOT_FOUND, ProblemType.NOT_FOUND.safeDetail());
    }
}
