package com.rikkaus.wealth.goals.domain;

/**
 * Which amount a template suggests, and from what, as the accepted design's table "Bảy mẫu và thứ
 * chúng điền sẵn" states it.
 *
 * <p>Each constant names both the field that is suggested and the basis for it, because the design
 * suggests a <em>target</em> for the emergency fund and the debt-reduction goal but an
 * <em>allocated</em> amount for net-worth growth. One enum rather than two keeps a template from
 * claiming to prefill both at once, which no template does.
 *
 * <p>The basis is named, never computed. The figures live in the cash-flow, asset and liability
 * slices, which this slice may not read, and the client already has them on screen.
 */
public enum GoalAmountPrefill {
    /** The form suggests no amount. */
    NONE,
    /** Suggest the target as six months of the user's monthly expenses. */
    TARGET_FROM_SIX_MONTHS_OF_EXPENSES,
    /** Suggest the allocated amount as the user's current net worth. */
    ALLOCATED_FROM_CURRENT_NET_WORTH,
    /** Suggest the target as the total of the user's tracked liabilities. */
    TARGET_FROM_TOTAL_LIABILITIES
}
