package com.rikkaus.wealth.shared.deletion;

import com.rikkaus.wealth.shared.error.ApiException;
import com.rikkaus.wealth.shared.error.ProblemType;
import java.util.Map;

/**
 * Refuses a delete that would change other records until the client says it knows.
 *
 * <p>The refusal carries the same {@link DeletionImpact} the preview route returns, computed inside the
 * delete's own transaction, so a client that skipped the preview or showed a stale one still gets the
 * current consequences in the 409 and can show them before repeating the request with {@code
 * confirm=true}.
 */
public final class DeletionGuard {

    /** The extension member the impact travels in on a {@code linked-records} problem. */
    public static final String IMPACT_MEMBER = "impact";

    private static final String DETAIL =
            "Deleting this record changes other records. Review the impact and repeat the request with "
                    + "confirm=true.";

    private DeletionGuard() {}

    public static void requireConfirmed(DeletionImpact impact, boolean confirmed) {
        if (impact.confirmationRequired() && !confirmed) {
            throw new ApiException(ProblemType.LINKED_RECORDS, DETAIL, Map.of(IMPACT_MEMBER, impact));
        }
    }
}
