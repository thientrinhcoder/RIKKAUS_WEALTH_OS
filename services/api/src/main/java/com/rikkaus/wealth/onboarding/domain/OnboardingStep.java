package com.rikkaus.wealth.onboarding.domain;

/**
 * The seven steps of the guided onboarding, in the order the MVP backbone suggests them.
 *
 * <p>The order is the contract: {@link #number()} is the declaration position plus one, and a client
 * resumes the user at a step by that number. Adding, removing or reordering a constant therefore
 * changes what a stored {@code current_step} means, and the table's check constraint pins the range to
 * one through seven so that cannot happen without a migration.
 *
 * <p>No Vietnamese label lives here. The client maps each constant to the accepted design's copy, the
 * same way asset categories and goal types are published.
 */
public enum OnboardingStep {
    /** Declare the USD/VND exchange rate. Done once a rate has been declared, not merely read. */
    EXCHANGE_RATE,
    /** Add cash and bank accounts. Done while an asset of category CASH or BANK_ACCOUNT exists. */
    CASH_AND_BANK_ACCOUNTS,
    /** Add major assets. Done while an asset of any other category exists. */
    MAJOR_ASSETS,
    /** Add liabilities. Done while any liability exists. */
    LIABILITIES,
    /** Add recurring income and expenses. Done while any recurring cash-flow entry exists. */
    RECURRING_CASH_FLOWS,
    /** Review the first dashboard. The only step the client reports rather than a record proving. */
    DASHBOARD_REVIEW,
    /** Create the first financial goal. Done while any goal exists. */
    FIRST_GOAL;

    public static final int FIRST_NUMBER = 1;
    public static final int LAST_NUMBER = 7;

    public int number() {
        return ordinal() + 1;
    }
}
