package com.rikkaus.wealth.goals.domain;

import java.math.BigDecimal;

/**
 * The seven goal templates the MVP Backbone names, and no others.
 *
 * <p>A template is nothing more than a value of this enum plus the suggestions it carries. The
 * accepted design for issue #84 is explicit that every template uses the same form and the same
 * field set, and only fills in suggested values the user may change — so a template table would be
 * seven rows nobody edits, and a separate {@code GoalTemplate} type would be this enum under another
 * name. Keeping the defaults here is what lets {@code GET /api/v1/goal-templates} publish them from
 * one source rather than the client hard-coding product numbers.
 *
 * <p>The same seven constants are a check constraint in {@code V9__goals.sql}: the enum stops the API,
 * the constraint stops everything else. Persisted by name, never by ordinal, so inserting an eighth
 * template anywhere but the end cannot reinterpret rows already written.
 *
 * <p>The suggested amounts the design describes — six months of expenses, the current net worth, the
 * total tracked debt — are deliberately <em>named</em> here and not computed. Computing them means
 * reading the cash-flow, asset and liability slices, which the slice rule in {@code
 * ArchitectureRulesTest} forbids; the client already holds those figures and does the arithmetic.
 *
 * <p>No Vietnamese label lives here. The client maps each constant to the copy the accepted design
 * specifies — {@code EMERGENCY_FUND} renders as <em>Quỹ dự phòng</em> — and a second mapping on the
 * server would be product copy nothing keeps in step with the first.
 */
public enum GoalType {
    EMERGENCY_FUND("3.00", 18, GoalAmountPrefill.TARGET_FROM_SIX_MONTHS_OF_EXPENSES),
    HOME_PURCHASE("6.00", 60, GoalAmountPrefill.NONE),
    EDUCATION("5.00", 72, GoalAmountPrefill.NONE),
    RETIREMENT("8.00", 180, GoalAmountPrefill.NONE),
    NET_WORTH_GROWTH("7.00", 48, GoalAmountPrefill.ALLOCATED_FROM_CURRENT_NET_WORTH),
    DEBT_REDUCTION("0.00", 48, GoalAmountPrefill.TARGET_FROM_TOTAL_LIABILITIES),
    OTHER_GOAL("0.00", 24, GoalAmountPrefill.NONE);

    private final BigDecimal defaultExpectedAnnualReturnPercent;
    private final int defaultHorizonMonths;
    private final GoalAmountPrefill amountPrefill;

    GoalType(
            String defaultExpectedAnnualReturnPercent,
            int defaultHorizonMonths,
            GoalAmountPrefill amountPrefill) {
        this.defaultExpectedAnnualReturnPercent = new BigDecimal(defaultExpectedAnnualReturnPercent);
        this.defaultHorizonMonths = defaultHorizonMonths;
        this.amountPrefill = amountPrefill;
    }

    /** The expected annual return the form suggests, in percent. A suggestion, never a promise. */
    public BigDecimal defaultExpectedAnnualReturnPercent() {
        return defaultExpectedAnnualReturnPercent;
    }

    /** How many months from today the form suggests as the target date. */
    public int defaultHorizonMonths() {
        return defaultHorizonMonths;
    }

    /** Which amount, if any, the form should suggest from figures the client already holds. */
    public GoalAmountPrefill amountPrefill() {
        return amountPrefill;
    }
}
