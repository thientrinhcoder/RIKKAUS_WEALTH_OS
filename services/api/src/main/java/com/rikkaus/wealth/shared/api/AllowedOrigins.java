package com.rikkaus.wealth.shared.api;

import java.util.Arrays;
import java.util.List;

/**
 * Parses the {@code rikkaus.api.allowed-origins} allowlist.
 *
 * <p>One parser for every reader of the property, so the CORS mapping and the web-session origin check
 * can never disagree about which origins are allowed. Parsed from a raw string rather than bound to a
 * List, so an unset or blank property is unambiguously an empty allowlist rather than a list holding one
 * empty entry.
 */
public final class AllowedOrigins {

    public static final String PROPERTY = "${rikkaus.api.allowed-origins:}";

    private AllowedOrigins() {}

    public static List<String> parse(String commaSeparated) {
        if (commaSeparated == null) {
            return List.of();
        }
        return Arrays.stream(commaSeparated.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }
}
