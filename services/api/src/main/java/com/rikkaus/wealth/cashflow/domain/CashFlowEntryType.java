package com.rikkaus.wealth.cashflow.domain;

/**
 * Which way the money moves.
 *
 * <p>The type carries the sign so the amount never has to. An amount is always positive, and a later
 * monthly total adds the incomes and subtracts the expenses by looking here rather than at the sign
 * of a number a user might have entered either way.
 *
 * <p>Each {@link CashFlowCategory} belongs to exactly one of these, and the pairing is checked in the
 * service and in the table, so a salary cannot be filed as an expense by a mismatched pair of fields.
 */
public enum CashFlowEntryType {
    INCOME,
    EXPENSE
}
