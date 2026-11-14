package com.rikkaus.wealth.cashflow.api;

import com.rikkaus.wealth.cashflow.domain.CashFlowCategory;
import com.rikkaus.wealth.cashflow.domain.CashFlowEntryDetails;
import com.rikkaus.wealth.cashflow.domain.CashFlowEntryType;
import com.rikkaus.wealth.cashflow.domain.CashFlowRecurrence;
import com.rikkaus.wealth.cashflow.domain.CashFlowTiming;
import com.rikkaus.wealth.shared.money.Currency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A cash-flow entry as the user states it: the body of both {@code POST} and {@code PUT}.
 *
 * <p>One record for both routes, because nothing about an entry is frozen after creation — there is no
 * append-only history that owns any of its values, unlike an asset's valuation — so the two bodies
 * would otherwise be the same record declared twice. {@code PUT} is a replacement: an optional member
 * omitted from the body is cleared.
 *
 * <p>There is no owner field. The entry belongs to whoever the access token authenticates, so an owner
 * member would be a value the server had to refuse — and an ArchUnit rule already fails the
 * compilation gate on a controller that binds one from the request.
 *
 * <p>There is no name. The MVP Backbone lists nine attributes for an entry and a name is not one of
 * them, and the accepted design's quick-entry form has no field for one; {@code notes} is where "Lương
 * tháng" or "Tiền thuê căn hộ Thảo Điền" goes.
 *
 * <p>{@code Digits} mirrors the column precision rather than restating a business rule. {@code amount}
 * is {@code NUMERIC(19, 2)}, so without it a value with too many digits would reach PostgreSQL and
 * fail there — a 500 for what is a client input error.
 *
 * <p>Only the single-field rules are annotations. Whether the category fits the entry type, whether a
 * recurrence may be present and whether an end date is in order are decided in {@code
 * CashFlowEntryService}, which reports them in the same {@code errors} shape, all at once.
 *
 * <p>An unrecognised constant in any of the enumerated members is a {@code malformed-request} rather
 * than a {@code validation-failed}, because Jackson cannot construct the record at all and the failure
 * happens before bean validation runs. The real client chooses from a picker, so the case arises only
 * from a hand-written request.
 */
@Schema(description = "Create or replace a cash-flow entry. The same body serves both.")
public record CashFlowEntryRequest(
        @Schema(
                        description = "Whether the money comes in or goes out. Decides the sign; the amount is always positive.",
                        example = "INCOME",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull(message = "must not be null")
                CashFlowEntryType entryType,
        @Schema(
                        description =
                                "One of the seven income or nine expense categories. Must belong to the entry "
                                        + "type given.",
                        example = "SALARY",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull(message = "must not be null")
                CashFlowCategory category,
        @Schema(
                        description = "The amount, in this entry's currency. Greater than zero.",
                        example = "45000000.00",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull(message = "must not be null")
                @DecimalMin(value = "0", inclusive = false, message = "must be greater than zero")
                @Digits(integer = 17, fraction = 2, message = "must have at most 17 integer digits and 2 decimals")
                BigDecimal amount,
        @Schema(
                        description =
                                "The currency the amount is expressed in. No conversion is stored; the client "
                                        + "converts with the rate from the settings resource.",
                        example = "VND",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull(message = "must not be null")
                Currency currency,
        @Schema(
                        description = "ONE_TIME or RECURRING. Decides whether recurrence and endDate may be present.",
                        example = "RECURRING",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull(message = "must not be null")
                CashFlowTiming timing,
        @Schema(
                        description =
                                "How often a recurring entry repeats. Required when timing is RECURRING and must "
                                        + "be omitted when it is ONE_TIME.",
                        example = "MONTHLY")
                CashFlowRecurrence recurrence,
        @Schema(
                        description =
                                "The date a one-time entry applies, or the first date of a recurring one. May be "
                                        + "in the future.",
                        example = "2026-01-01",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull(message = "must not be null")
                LocalDate effectiveDate,
        @Schema(
                        description =
                                "The last date a recurring entry applies. Only for RECURRING; omit for open-ended. "
                                        + "Must not be before effectiveDate.",
                        example = "2029-06-30")
                LocalDate endDate,
        @Schema(
                        description =
                                "An asset of the signed-in user this entry refers to, such as the apartment a rent "
                                        + "comes from. Omit to leave it unlinked, or to clear it on PUT.",
                        example = "a81d4f27-6c3e-4b19-8f55-0d2e7c9a3b64")
                UUID linkedAssetId,
        @Schema(description = "Anything the user wants to remember about it. Omit to clear it on PUT.", example = "Lương tháng")
                @Size(max = 2000, message = "must be at most 2000 characters")
                String notes) {

    CashFlowEntryDetails toDetails() {
        return new CashFlowEntryDetails(
                entryType, category, amount, currency, timing, recurrence, effectiveDate, endDate, linkedAssetId, notes);
    }
}
