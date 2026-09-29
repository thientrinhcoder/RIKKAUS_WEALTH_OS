package com.rikkaus.wealth.shared.security;

import java.util.UUID;

/**
 * Turns an access token into the identity it authenticates.
 *
 * <p>Declared in {@code shared} and implemented by the {@code identity} slice, rather than the security
 * filter depending on that slice directly. That direction is required, not stylistic: the ArchUnit rule
 * {@code featureSlicesDoNotDependOnEachOther} permits any slice to depend on {@code shared} and forbids
 * the reverse, so a filter importing {@code identity.session.AccessTokenService} fails the build — and
 * rightly, because the request-authentication mechanism has no business knowing which feature mints
 * tokens.
 */
public interface AccessTokenVerifier {

    VerifiedAccessToken verify(String token) throws InvalidAccessTokenException;

    /** Who a verified access token authenticates, and which rotation chain it belongs to. */
    record VerifiedAccessToken(UUID userId, UUID sessionId) {}

    /**
     * A checked exception on purpose. Verifying a credential is the one place a caller must not be able to
     * forget the failure path, and an unchecked exception here would let a filter treat an invalid token as
     * an absent one.
     */
    class InvalidAccessTokenException extends Exception {
        public InvalidAccessTokenException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
