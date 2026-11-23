package com.rikkaus.wealth.shared.deletion;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * One kind of stored record a delete reaches through a foreign key, and what happens to it.
 *
 * <p>{@code DELETED} mirrors {@code ON DELETE CASCADE} and {@code UNLINKED} mirrors {@code ON DELETE SET
 * NULL}, with {@code field} naming the API member that becomes {@code null}. Nothing here is a policy of
 * its own; the table decides what happens and this says so in advance.
 */
@Schema(description = "One kind of stored record the delete reaches, and what happens to it.")
public record LinkedRecordEffect(
        @Schema(description = "The kind of record affected.", example = "LIABILITY")
                DeletionImpact.RecordType recordType,
        @Schema(
                        description =
                                "DELETED: removed together with the record. UNLINKED: kept, with the field "
                                        + "named in field set to null.",
                        example = "UNLINKED")
                Effect effect,
        @Schema(
                        description =
                                "For UNLINKED, the response member that becomes null on each affected record. "
                                        + "Null for DELETED.",
                        example = "collateralAssetId",
                        nullable = true)
                String field,
        @Schema(description = "How many records are affected. Always at least 1.", example = "1")
                long count,
        @ArraySchema(
                        arraySchema =
                                @Schema(
                                        description =
                                                "The affected records, for UNLINKED. Empty for DELETED, "
                                                        + "where only the count is given."))
                List<LinkedRecordReference> records) {

    public LinkedRecordEffect {
        records = List.copyOf(records);
    }

    /** Records removed with the deleted one; only counted. */
    public static LinkedRecordEffect deleted(DeletionImpact.RecordType recordType, long count) {
        return new LinkedRecordEffect(recordType, Effect.DELETED, null, count, List.of());
    }

    /** Records kept with {@code field} cleared; listed so the user can see which. */
    public static LinkedRecordEffect unlinked(
            DeletionImpact.RecordType recordType, String field, List<LinkedRecordReference> records) {
        return new LinkedRecordEffect(recordType, Effect.UNLINKED, field, records.size(), records);
    }

    /** What the database does to an affected record. */
    public enum Effect {
        DELETED,
        UNLINKED
    }
}
