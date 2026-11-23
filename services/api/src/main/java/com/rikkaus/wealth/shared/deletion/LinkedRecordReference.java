package com.rikkaus.wealth.shared.deletion;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

/**
 * Enough of an affected record for a confirmation dialog to name it.
 *
 * <p>Only the identifier, the user's own label and the category: no amount, no balance. The label is a
 * liability's {@code name} or a cash-flow entry's {@code notes}, which may be absent because an entry
 * has no name; the category is there so the client can still show "Thu nhập cho thuê" for an entry with
 * no notes.
 */
@Schema(description = "An affected record, identified for display.")
public record LinkedRecordReference(
        @Schema(description = "The affected record's identifier.") UUID id,
        @Schema(
                        description =
                                "The user's label for it: a liability's name, a cash-flow entry's notes. Null "
                                        + "for an entry recorded without notes.",
                        example = "Vay mua nhà Vietcombank",
                        nullable = true)
                String label,
        @Schema(description = "The affected record's category constant.", example = "BANK_LOAN")
                String category) {

    /** Converts the rows a slice's native query returns. */
    public static List<LinkedRecordReference> of(List<? extends LinkedRecordRow> rows) {
        return rows.stream()
                .map(row -> new LinkedRecordReference(row.getId(), row.getLabel(), row.getCategory()))
                .toList();
    }
}
