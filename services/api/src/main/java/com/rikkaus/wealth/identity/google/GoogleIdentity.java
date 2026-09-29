package com.rikkaus.wealth.identity.google;

/**
 * The claims this service is prepared to trust from a verified Google identity token.
 *
 * <p>Deliberately narrow. Google returns considerably more — picture, locale, hosted domain, profile
 * URL — and every field carried past this boundary is a field that ends up stored, logged or
 * serialized somewhere. MVP 0 needs an identity key, an address to show, and a name to greet the user
 * with.
 */
public record GoogleIdentity(
        String subject, String email, boolean emailVerified, String displayName) {}
