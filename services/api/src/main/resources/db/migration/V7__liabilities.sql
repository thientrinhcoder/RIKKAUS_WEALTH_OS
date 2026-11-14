-- The liability catalog: one row per debt or obligation a user carries, across the six categories
-- the MVP Backbone names. The row records the terms the user states -- balance, rate, dates, the
-- recurring payment -- and nothing derived from them: there is no amortisation table, no principal
-- and interest split and no interest accrual, because the accepted design for issue #60 states the
-- schedule is recorded, not computed, and the cash-flow expansion belongs to issues #74 and #77.
--
-- As with wealth.assets, every rule exists in two places on purpose: bean validation refuses it at
-- the API boundary with a named field, and a check constraint refuses it for any caller that is not
-- the API. The second is the one that still holds during a migration or a manual correction.

CREATE TABLE wealth.liabilities (
    id                          UUID           PRIMARY KEY,
    user_id                     UUID           NOT NULL REFERENCES wealth.users (id) ON DELETE CASCADE,

    -- The four required fields.
    name                        TEXT           NOT NULL,
    category                    TEXT           NOT NULL,
    outstanding_balance         NUMERIC(19, 2) NOT NULL,
    currency                    TEXT           NOT NULL,

    -- The terms. All optional: a margin loan has no maturity and a debt to a relative has no rate.
    interest_rate               NUMERIC(7, 4),
    start_date                  DATE,
    maturity_date               DATE,

    -- The recurring payment, which travels as a pair, and the next date a payment is due, which
    -- stands alone: a credit card has a due date without a fixed recurring amount.
    recurring_payment_amount    NUMERIC(19, 2),
    recurring_payment_frequency TEXT,
    next_payment_date           DATE,

    -- The optional collateral. ON DELETE SET NULL rather than RESTRICT: refusing the asset's
    -- deletion would change the assets resource's behaviour from inside this slice -- its DELETE
    -- declares no 409 and would answer 500 -- and the linked-record consequences of a delete are
    -- issue #104's. The liability survives with no collateral, which the accepted design renders as
    -- an ordinary unlinked state.
    collateral_asset_id         UUID           REFERENCES wealth.assets (id) ON DELETE SET NULL,

    notes                       TEXT,

    created_at                  TIMESTAMPTZ    NOT NULL,
    updated_at                  TIMESTAMPTZ    NOT NULL,

    -- The six categories, enumerated here as well as in LiabilityCategory, so a seventh is a
    -- reviewed migration and a typo cannot split a user's debts across every report built on this.
    CONSTRAINT liabilities_category_is_known
        CHECK (category IN (
            'BANK_LOAN',
            'PERSONAL_LOAN',
            'MARGIN_LOAN',
            'CREDIT_CARD_BALANCE',
            'GUARANTEE_OBLIGATION',
            'OTHER_LIABILITY'
        )),

    CONSTRAINT liabilities_currency_is_supported
        CHECK (currency IN ('VND', 'USD')),

    -- Zero is allowed and negative is not. A card paid down to nothing this month is a real state
    -- the user may keep in the catalog; a negative debt is an asset and has its own table.
    CONSTRAINT liabilities_outstanding_balance_is_not_negative
        CHECK (outstanding_balance >= 0),

    -- A nominal annual percentage as the user states it. Zero is a real rate -- an interest-free
    -- loan from family -- and negative is not something a lender charges.
    CONSTRAINT liabilities_interest_rate_is_not_negative
        CHECK (interest_rate IS NULL OR interest_rate >= 0),

    -- Strictly positive when present. A recurring payment of nothing describes no obligation.
    CONSTRAINT liabilities_recurring_payment_amount_is_positive
        CHECK (recurring_payment_amount IS NULL OR recurring_payment_amount > 0),

    -- The four frequencies the accepted design offers: monthly, quarterly, every six months, yearly.
    CONSTRAINT liabilities_recurring_payment_frequency_is_known
        CHECK (recurring_payment_frequency IS NULL OR recurring_payment_frequency IN (
            'MONTHLY',
            'QUARTERLY',
            'SEMIANNUAL',
            'YEARLY'
        )),

    -- The amount and the frequency move together or not at all. An amount with no cadence, or a
    -- cadence with no amount, is a schedule the client has no way to render and the design has no
    -- copy for -- the same shape V3 chose for the three exchange-rate columns.
    CONSTRAINT liabilities_recurring_payment_is_complete
        CHECK (
            (recurring_payment_amount IS NULL AND recurring_payment_frequency IS NULL)
            OR (recurring_payment_amount IS NOT NULL AND recurring_payment_frequency IS NOT NULL)
        ),

    -- A loan cannot mature before it starts. Equal is allowed: a one-day bridging obligation is odd
    -- but not impossible, and refusing it would be a rule nobody asked for.
    CONSTRAINT liabilities_maturity_not_before_start
        CHECK (start_date IS NULL OR maturity_date IS NULL OR maturity_date >= start_date),

    -- The next payment falls inside the term when the term is known. It may be in the past: an
    -- overdue payment is a state the accepted design shows, not an input error.
    CONSTRAINT liabilities_next_payment_within_term
        CHECK (
            next_payment_date IS NULL
            OR (
                (start_date IS NULL OR next_payment_date >= start_date)
                AND (maturity_date IS NULL OR next_payment_date <= maturity_date)
            )
        ),

    -- Bounded text, as TEXT with a length check rather than VARCHAR(n), for the reason V4 gives.
    CONSTRAINT liabilities_name_is_present_and_bounded
        CHECK (btrim(name) <> '' AND char_length(name) <= 120),

    CONSTRAINT liabilities_notes_are_bounded
        CHECK (notes IS NULL OR char_length(notes) <= 2000)
);

COMMENT ON TABLE wealth.liabilities IS
    'One row per debt or obligation a user carries. Terms and schedule as stated; nothing is computed from them.';

-- The owner is a plain column with a foreign key and no Java association, for the reason V4 gives:
-- ArchitectureRulesTest forbids this slice from depending on identity.
COMMENT ON COLUMN wealth.liabilities.user_id IS
    'Owner. Every query is scoped to it server-side; no request ever carries it.';

-- NUMERIC, never a floating-point type. This column is summed into a total-liabilities figure and
-- subtracted from net worth, so a binary rounding error here moves a reported number.
COMMENT ON COLUMN wealth.liabilities.outstanding_balance IS
    'What is still owed, in this row''s currency, as the user entered it. Zero is a paid-down balance.';

COMMENT ON COLUMN wealth.liabilities.currency IS
    'VND or USD, the currency outstanding_balance and recurring_payment_amount are expressed in.';

-- A percentage per year, not a fraction: 8.5 means 8.5 % per annum. Four decimals hold a rate
-- quoted to the basis point and finer; three integer digits hold any rate a Vietnamese lender quotes.
COMMENT ON COLUMN wealth.liabilities.interest_rate IS
    'Nominal annual rate in percent, as the user states it. Recorded only; nothing here accrues interest from it.';

COMMENT ON COLUMN wealth.liabilities.recurring_payment_amount IS
    'The total due each period, in this row''s currency. Not split into principal and interest.';

COMMENT ON COLUMN wealth.liabilities.recurring_payment_frequency IS
    'MONTHLY, QUARTERLY, SEMIANNUAL or YEARLY. Present exactly when recurring_payment_amount is.';

COMMENT ON COLUMN wealth.liabilities.next_payment_date IS
    'The next date a payment is due, as the user states it. May be in the past; that is an overdue payment.';

-- Points at another slice's table through the schema alone. The liabilities slice never imports an
-- assets class; ownership of the collateral is checked with a query against this table's target
-- scoped by user_id, so a user can only pledge what they own.
COMMENT ON COLUMN wealth.liabilities.collateral_asset_id IS
    'The asset pledged against this liability, owned by the same user. NULL when none, or when the asset was since deleted.';

-- The order the catalog lists in: soonest payment first, so the index serves the only query the
-- slice makes. user_id leads because every query is owner-scoped first.
CREATE INDEX idx_liabilities_owner_next_payment ON wealth.liabilities (user_id, next_payment_date);
