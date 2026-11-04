package com.rikkaus.wealth.shared.money;

/**
 * The closed set of currencies this product handles, each carrying the number of decimal places an
 * amount in it is held to.
 *
 * <p>An enum rather than {@code java.util.Currency}, because the accepted design for issue #43
 * limits the product to VND reporting with USD input, and {@code Currency.getInstance("JPY")}
 * returns a perfectly valid instance for which no reporting path exists. Keeping the set closed makes
 * such an amount impossible to construct, instead of making it a runtime check every service has to
 * remember. It also means a {@code switch} over currencies needs no default branch, so adding a third
 * currency later fails the build at every site that has to decide something, rather than quietly
 * taking an existing branch.
 *
 * <p>The scale is the currency's real minor unit, not a storage convenience: VND has no circulating
 * subunit, so an amount in dong is a whole number, while USD is held to cents.
 */
public enum Currency {
    /** Vietnamese dong. No circulating subunit, so amounts are whole. */
    VND("VND", 0),
    /** United States dollar, held to cents. An input currency here, never a reporting one. */
    USD("USD", 2);

    /**
     * The currency every total, net worth and ratio is reported in.
     *
     * <p>Named once here so no slice hard-codes {@link #VND} as "the" currency inside its own
     * arithmetic. A later product decision that makes the reporting currency a per-user setting
     * changes this constant's users, which is a far smaller surface than every call site.
     */
    public static final Currency REPORTING = VND;

    private final String code;
    private final int scale;

    Currency(String code, int scale) {
        this.code = code;
        this.scale = scale;
    }

    /** The ISO 4217 alphabetic code, which is also the form that appears on the wire. */
    public String code() {
        return code;
    }

    /** The number of decimal places an amount in this currency is held to. */
    public int scale() {
        return scale;
    }
}
