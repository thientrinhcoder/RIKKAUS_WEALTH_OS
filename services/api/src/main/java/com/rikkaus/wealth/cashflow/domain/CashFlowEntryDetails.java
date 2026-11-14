package com.rikkaus.wealth.cashflow.domain;

import com.rikkaus.wealth.shared.money.Currency;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Everything a user states about one cash-flow entry.
 *
 * <p>One type used by both create and replace, so the editable set is defined once and the two paths
 * cannot drift into accepting different fields. Unlike an asset, nothing here is frozen after
 * creation: there is no append-only history that owns any of these values, so an edit may change the
 * type, the category and the amount as freely as the note.
 *
 * <p>{@code recurrence}, {@code endDate}, the two links and {@code notes} may be {@code null}.
 * For the first two a {@code null} is a rule, not merely an absence — a one-time entry must have
 * neither — and {@code CashFlowEntryService} is where that rule is applied. For the links and the
 * note a {@code null} is the absence of an answer, and a replacement carrying {@code null} clears the
 * stored value, which is what makes {@code PUT} a replacement and not a patch. At most one of the two
 * links may be set.
 */
public record CashFlowEntryDetails(
        CashFlowEntryType entryType,
        CashFlowCategory category,
        BigDecimal amount,
        Currency currency,
        CashFlowTiming timing,
        CashFlowRecurrence recurrence,
        LocalDate effectiveDate,
        LocalDate endDate,
        UUID linkedAssetId,
        UUID linkedLiabilityId,
        String notes) {}
