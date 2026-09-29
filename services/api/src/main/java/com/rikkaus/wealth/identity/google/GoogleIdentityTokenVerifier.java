package com.rikkaus.wealth.identity.google;

/**
 * Verifies a Google identity token's signature, issuer, audience and expiry, and reads its claims.
 *
 * <p>Separate from the code exchange because they fail for different reasons and are stubbed
 * independently in tests: an exchange fails because a code was reused, while verification fails
 * because a token was forged. Collapsing them would hide which of the two a test is exercising.
 */
public interface GoogleIdentityTokenVerifier {

    GoogleIdentity verify(String idToken) throws GoogleIdentityTokenInvalidException;

    class GoogleIdentityTokenInvalidException extends Exception {
        public GoogleIdentityTokenInvalidException(String message, Throwable cause) {
            super(message, cause);
        }

        public GoogleIdentityTokenInvalidException(String message) {
            super(message, null);
        }
    }
}
