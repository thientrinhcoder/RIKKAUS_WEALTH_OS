package com.rikkaus.wealth.identity.google;

/**
 * Exchanges an authorization code for a Google identity token.
 *
 * <p>A port, so the whole sign-in path is testable without reaching Google. That is not only a test
 * convenience: it is what keeps the suite deterministic and runnable offline, which the two-tier test
 * convention in {@code docs/api-contract-conventions.md} depends on.
 */
public interface GoogleAuthorizationCodeExchange {

    /**
     * @return the raw, still-unverified {@code id_token}
     * @throws GoogleExchangeFailedException if Google rejects the code or returns no identity token
     */
    String exchangeForIdentityToken(String authorizationCode, String codeVerifier, String redirectUri)
            throws GoogleExchangeFailedException;

    /**
     * A rejected or expired code is an ordinary user outcome — the "callback failure" state in the
     * accepted identity design — not a server fault, so it is a checked exception the caller must turn
     * into a 401 rather than an unchecked one that would surface as a 500.
     */
    class GoogleExchangeFailedException extends Exception {
        public GoogleExchangeFailedException(String message, Throwable cause) {
            super(message, cause);
        }

        public GoogleExchangeFailedException(String message) {
            super(message, null);
        }
    }
}
