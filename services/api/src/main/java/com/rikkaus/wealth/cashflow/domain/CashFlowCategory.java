package com.rikkaus.wealth.cashflow.domain;

/**
 * The seven income and nine expense categories the MVP Backbone names, and no others.
 *
 * <p>Each constant carries the entry type it belongs to, so "does this category fit this type" is a
 * lookup on the enum rather than a table somebody has to keep in step with it. The same pairing is a
 * check constraint in {@code V7__cash_flow_entries.sql}: the enum stops the API, the constraint stops
 * everything else.
 *
 * <p>Persisted by name, never by ordinal. Inserting a category in the middle of this list would
 * otherwise rewrite the meaning of every row already stored, silently and with no migration.
 *
 * <p>No Vietnamese label lives here. The published contract is English, the client maps each constant
 * to the copy the accepted design specifies — {@code SALARY} renders as <em>Lương</em> — and
 * duplicating that mapping on the server would create a second source of product copy that nothing
 * keeps in step with the first.
 */
public enum CashFlowCategory {
    SALARY(CashFlowEntryType.INCOME),
    BUSINESS_INCOME(CashFlowEntryType.INCOME),
    RENTAL_INCOME(CashFlowEntryType.INCOME),
    DIVIDEND(CashFlowEntryType.INCOME),
    DEPOSIT_INTEREST(CashFlowEntryType.INCOME),
    BOND_OR_FUND_INCOME(CashFlowEntryType.INCOME),
    OTHER_INCOME(CashFlowEntryType.INCOME),

    LIVING_COSTS(CashFlowEntryType.EXPENSE),
    HOUSING(CashFlowEntryType.EXPENSE),
    EDUCATION(CashFlowEntryType.EXPENSE),
    HEALTHCARE(CashFlowEntryType.EXPENSE),
    INSURANCE(CashFlowEntryType.EXPENSE),
    TAX_AND_FEES(CashFlowEntryType.EXPENSE),
    DEBT_PRINCIPAL_PAYMENT(CashFlowEntryType.EXPENSE),
    INTEREST_PAYMENT(CashFlowEntryType.EXPENSE),
    OTHER_EXPENSE(CashFlowEntryType.EXPENSE);

    private final CashFlowEntryType entryType;

    CashFlowCategory(CashFlowEntryType entryType) {
        this.entryType = entryType;
    }

    /** The one entry type this category may be filed under. */
    public CashFlowEntryType entryType() {
        return entryType;
    }
}
