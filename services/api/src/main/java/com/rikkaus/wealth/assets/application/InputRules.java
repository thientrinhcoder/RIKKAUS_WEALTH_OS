package com.rikkaus.wealth.assets.application;

import static com.rikkaus.wealth.shared.validation.FieldErrors.error;

import com.rikkaus.wealth.assets.domain.AssetCurrency;
import com.rikkaus.wealth.assets.domain.DeclaredValuation;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The input rules that belong to this slice alone, shared by the two services so an asset and its
 * history are judged by one definition.
 *
 * <p>What every slice applies the same way — cleaning free text, refusing {@code U+0000}, reading
 * today as a Vietnamese civil date, the shape of the {@code errors} body — lives in {@code
 * shared/validation}. What is left here is the exchange-rate snapshot, which applies equally to the
 * valuation an asset is created with and to every one recorded later.
 */
final class InputRules {

    private InputRules() {}

    /**
     * What is wrong with the exchange-rate snapshot a declaration carries, if anything.
     *
     * <p>The two rate fields travel together or not at all, because a rate with no date cannot be
     * reconciled later and a date with no rate describes nothing; the table refuses a half-snapshot for
     * the same reason. A VND declaration carries neither, since there is nothing to convert and a rate
     * stored beside a VND value is a figure some later report might apply anyway. A USD declaration may
     * carry neither — that is the accepted design's <em>partial</em> state, and the state every asset
     * created before the history existed is in — but when it carries them, the date is judged like
     * every other user-entered date.
     *
     * <p>Returned as a list rather than thrown, so a caller can report these alongside the date rules
     * in one response instead of making the user fix them one round trip at a time.
     */
    static List<Map<String, String>> rateSnapshotErrors(DeclaredValuation declared, LocalDate today) {
        List<Map<String, String>> errors = new ArrayList<>();
        boolean hasRate = declared.usdToVndRate() != null;
        boolean hasRateDate = declared.rateEffectiveDate() != null;
        if (declared.currency() == AssetCurrency.VND) {
            if (hasRate) {
                errors.add(error("usdToVndRate", "must be absent for a VND valuation"));
            }
            if (hasRateDate) {
                errors.add(error("rateEffectiveDate", "must be absent for a VND valuation"));
            }
            return errors;
        }
        if (hasRate && !hasRateDate) {
            errors.add(error("rateEffectiveDate", "must be given together with usdToVndRate"));
        }
        if (hasRateDate && !hasRate) {
            errors.add(error("usdToVndRate", "must be given together with rateEffectiveDate"));
        }
        if (hasRateDate && declared.rateEffectiveDate().isAfter(today)) {
            errors.add(error("rateEffectiveDate", "must not be in the future"));
        }
        return errors;
    }
}
