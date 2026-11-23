package com.rikkaus.wealth.shared.deletion;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

/**
 * What deleting one record would do, computed from the database as it stands.
 *
 * <p>{@code confirmationRequired} is derived rather than chosen by each slice, so the rule that decides
 * whether a {@code DELETE} is refused cannot drift between assets and liabilities: it is true exactly
 * when some other record would lose a reference to this one. A history that belongs to the record
 * itself and goes with it, such as an asset's valuations, is reported but never on its own a reason to
 * stop, because every asset has at least the valuation it was created with and a record with no links
 * would otherwise never delete without a second request.
 */
@Schema(
        description =
                "What deleting this record does to other stored records and to computed views, read from "
                        + "the database at the moment of the request.")
public record DeletionImpact(
        @Schema(description = "The kind of record being deleted.", example = "ASSET")
                RecordType recordType,
        @Schema(description = "The record being deleted.") UUID recordId,
        @Schema(
                        description =
                                "True when at least one linkedEffects entry is UNLINKED. A DELETE of this "
                                        + "record then answers 409 linked-records unless it carries confirm=true.",
                        example = "true")
                boolean confirmationRequired,
        @ArraySchema(
                        arraySchema =
                                @Schema(
                                        description =
                                                "Stored records the delete removes or changes. Only kinds "
                                                        + "with at least one affected record appear."))
                List<LinkedRecordEffect> linkedEffects,
        @ArraySchema(
                        arraySchema =
                                @Schema(
                                        description =
                                                "Computed views that will no longer include this record. "
                                                        + "Descriptive: nothing is stored for them and "
                                                        + "nothing is counted."))
                List<DerivedEffect> derivedEffects) {

    public DeletionImpact {
        linkedEffects = List.copyOf(linkedEffects);
        derivedEffects = List.copyOf(derivedEffects);
    }

    /** The impact, with {@code confirmationRequired} worked out from the effects. */
    public static DeletionImpact of(
            RecordType recordType,
            UUID recordId,
            List<LinkedRecordEffect> linkedEffects,
            List<DerivedEffect> derivedEffects) {
        boolean unlinksSomething =
                linkedEffects.stream().anyMatch(effect -> effect.effect() == LinkedRecordEffect.Effect.UNLINKED);
        return new DeletionImpact(recordType, recordId, unlinksSomething, linkedEffects, derivedEffects);
    }

    /** The record kinds a deletion impact can name, as the deleted record or as an affected one. */
    public enum RecordType {
        ASSET,
        ASSET_VALUATION,
        LIABILITY,
        CASH_FLOW_ENTRY,
        GOAL
    }

    /** A computed view that reads the deleted record and will simply stop including it. */
    public enum DerivedEffect {
        CASH_FLOW_OCCURRENCES,
        MONTHLY_CASH_FLOW_REPORTS,
        CASH_FLOW_FORECAST
    }
}
