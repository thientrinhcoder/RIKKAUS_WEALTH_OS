package com.rikkaus.wealth.liabilities.domain;

/**
 * The six liability categories the MVP Backbone names, and no others.
 *
 * <p>An enum rather than free text, and the same six are a check constraint in {@code
 * V7__liabilities.sql}: the enum stops the API, the constraint stops everything else. Persisted by
 * name, never by ordinal, so inserting a seventh in the middle of this list cannot reinterpret a row
 * already stored.
 *
 * <p>No Vietnamese label lives here. The client maps each constant to the copy the accepted design
 * specifies — {@code CREDIT_CARD_BALANCE} renders as <em>Dư nợ thẻ tín dụng</em> — and a second
 * mapping on the server would be product copy that nothing keeps in step with the first.
 *
 * <p>Nothing about the category drives validation on the server. The accepted design lets the form
 * offer "không áp dụng" for the schedule and the maturity only on a credit card balance and a
 * guarantee obligation, but that is a rule about what the form shows, and it says so: <em>Không tự
 * suy đoán theo danh mục</em>. Every term and schedule field is optional here for every category.
 */
public enum LiabilityCategory {
    BANK_LOAN,
    PERSONAL_LOAN,
    MARGIN_LOAN,
    CREDIT_CARD_BALANCE,
    GUARANTEE_OBLIGATION,
    OTHER_LIABILITY
}
