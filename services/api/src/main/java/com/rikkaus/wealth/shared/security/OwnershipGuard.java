package com.rikkaus.wealth.shared.security;

import com.rikkaus.wealth.shared.error.ApiException;
import com.rikkaus.wealth.shared.error.ProblemType;
import java.util.Optional;
import java.util.UUID;

/**
 * Enforces that a user can only reach their own records.
 *
 * <p><strong>A record owned by someone else produces {@code not-found}, not {@code forbidden}</strong>,
 * and it produces the identical response a genuinely missing record produces. A 403 would confirm that
 * the record exists, which turns any list of identifiers into an enumeration oracle: an attacker learns
 * which asset, goal or liability identifiers are real without being able to read any of them. That shape
 * was agreed in {@code docs/api-contract-conventions.md} before authentication was built; this class is
 * the enforcement.
 *
 * <p>The detail text is taken from the taxonomy rather than accepted from the call site. That is the point
 * of the design: if callers supplied their own message, the first slice to write a helpful one — "Asset
 * belongs to another user" — would reintroduce the disclosure the status code was chosen to avoid, and no
 * test on that slice would notice.
 *
 * <p>The viewer's identifier is a parameter rather than read from the security context inside, so the guard
 * stays a pure function and the caller is the one place that decides whose view this is. Callers get that
 * identifier from {@link CurrentUser}, never from the request.
 */
public final class OwnershipGuard {

    private OwnershipGuard() {}

    /**
     * Returns the record when the viewer owns it.
     *
     * @throws ApiException {@code not-found} when the record is absent <em>or</em> owned by someone else —
     *     deliberately indistinguishable
     */
    public static <T extends UserOwned> T requireOwned(Optional<T> candidate, UUID viewerId) {
        return candidate
                .filter(record -> record.ownerId().equals(viewerId))
                .orElseThrow(OwnershipGuard::notFound);
    }

    /**
     * The same rule for a record already in hand, for a caller whose lookup returns the entity directly.
     *
     * <p>A {@code null} is treated as absent rather than rejected, so a repository method that returns null
     * cannot produce a 500 where a 404 is correct.
     */
    public static <T extends UserOwned> T requireOwned(T candidate, UUID viewerId) {
        return requireOwned(Optional.ofNullable(candidate), viewerId);
    }

    /** True when the viewer owns the record. For a caller that needs a boolean rather than an exception. */
    public static boolean isOwnedBy(UserOwned record, UUID viewerId) {
        return record != null && record.ownerId().equals(viewerId);
    }

    private static ApiException notFound() {
        return new ApiException(ProblemType.NOT_FOUND, ProblemType.NOT_FOUND.safeDetail());
    }
}
