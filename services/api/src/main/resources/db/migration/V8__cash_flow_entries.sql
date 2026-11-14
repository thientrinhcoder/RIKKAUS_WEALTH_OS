-- The cash-flow catalog: one row per income or expense a user records, one-time or recurring, across
-- the seven income and nine expense categories the MVP Backbone names. A row is the user's statement
-- of an amount and when it applies; it is not an occurrence. Expanding a monthly salary into the
-- months it falls in belongs to issue #74, reporting a month's totals to issue #77, and neither reads
-- anything this table does not already hold.
--
-- Every rule below exists in two places on purpose: bean validation refuses it at the API boundary
-- with a named field, and a check constraint refuses it for any caller that is not the API. The
-- second is the one that still holds during a migration, a manual correction, or the next slice.

CREATE TABLE wealth.cash_flow_entries (
    id              UUID           PRIMARY KEY,
    user_id         UUID           NOT NULL REFERENCES wealth.users (id) ON DELETE CASCADE,

    -- What the money is.
    entry_type      TEXT           NOT NULL,
    category        TEXT           NOT NULL,
    amount          NUMERIC(19, 2) NOT NULL,
    currency        TEXT           NOT NULL,

    -- When it applies. effective_date is the one date a one-time entry has and the start date of a
    -- recurring one; end_date exists only for a recurring entry and is optional even then.
    timing          TEXT           NOT NULL,
    recurrence      TEXT,
    effective_date  DATE           NOT NULL,
    end_date        DATE,

    -- Optional reference to the one record this money comes from or goes to: rent from an apartment,
    -- dividends from a holding, the interest on a mortgage. A reference and nothing more -- nothing
    -- here schedules, forecasts or revalues the asset, and nothing derives the amount from the
    -- liability's own recurring payment.
    linked_asset_id     UUID       REFERENCES wealth.assets (id) ON DELETE SET NULL,
    linked_liability_id UUID       REFERENCES wealth.liabilities (id) ON DELETE SET NULL,

    notes           TEXT,

    created_at      TIMESTAMPTZ    NOT NULL,
    updated_at      TIMESTAMPTZ    NOT NULL,

    CONSTRAINT cash_flow_entries_entry_type_is_known
        CHECK (entry_type IN ('INCOME', 'EXPENSE')),

    -- The sixteen categories, each tied to the one entry type it belongs to. A free-text column would
    -- let a typo invent a category; a category check alone would let a salary be filed as an expense
    -- and silently flip the sign of every monthly total built on this table.
    CONSTRAINT cash_flow_entries_category_matches_entry_type
        CHECK (
            (entry_type = 'INCOME' AND category IN (
                'SALARY',
                'BUSINESS_INCOME',
                'RENTAL_INCOME',
                'DIVIDEND',
                'DEPOSIT_INTEREST',
                'BOND_OR_FUND_INCOME',
                'OTHER_INCOME'
            ))
            OR
            (entry_type = 'EXPENSE' AND category IN (
                'LIVING_COSTS',
                'HOUSING',
                'EDUCATION',
                'HEALTHCARE',
                'INSURANCE',
                'TAX_AND_FEES',
                'DEBT_PRINCIPAL_PAYMENT',
                'INTEREST_PAYMENT',
                'OTHER_EXPENSE'
            ))
        ),

    -- Strictly positive. The entry type carries the direction, so a negative or zero amount would be
    -- either a second way of saying "expense" or an entry that moves nothing.
    CONSTRAINT cash_flow_entries_amount_is_positive
        CHECK (amount > 0),

    -- VND or USD, the two currencies a user may record an amount in. Distinct from the single
    -- reporting currency in user_settings; converting is the client's, with the rate it already holds.
    CONSTRAINT cash_flow_entries_currency_is_supported
        CHECK (currency IN ('VND', 'USD')),

    CONSTRAINT cash_flow_entries_timing_is_known
        CHECK (timing IN ('ONE_TIME', 'RECURRING')),

    CONSTRAINT cash_flow_entries_recurrence_is_known
        CHECK (recurrence IS NULL OR recurrence IN ('MONTHLY', 'QUARTERLY', 'YEARLY')),

    -- A one-time entry has no cadence and no end; a recurring one must say how often. Held as one
    -- constraint because the two columns describe one fact, and a row that satisfied one half without
    -- the other would be a schedule issue #74 could not expand.
    CONSTRAINT cash_flow_entries_recurrence_matches_timing
        CHECK (
            (timing = 'ONE_TIME' AND recurrence IS NULL AND end_date IS NULL)
            OR
            (timing = 'RECURRING' AND recurrence IS NOT NULL)
        ),

    -- An end on the same day as the start is a recurrence of one occurrence and is allowed; an end
    -- before the start describes nothing.
    CONSTRAINT cash_flow_entries_end_date_not_before_effective_date
        CHECK (end_date IS NULL OR end_date >= effective_date),

    -- At most one link. The backbone says an entry "may reference one asset or one liability", and
    -- an entry pointing at both would be counted against two records by every report that follows
    -- the link.
    CONSTRAINT cash_flow_entries_links_at_most_one_record
        CHECK (linked_asset_id IS NULL OR linked_liability_id IS NULL),

    -- Bounded text. TEXT with a length check rather than VARCHAR(n), because widening a check is a
    -- one-line migration while widening a VARCHAR rewrites the column's type.
    CONSTRAINT cash_flow_entries_notes_are_bounded
        CHECK (notes IS NULL OR char_length(notes) <= 2000)
);

COMMENT ON TABLE wealth.cash_flow_entries IS
    'One row per income or expense a user records, one-time or recurring. Not an occurrence; expansion is issue #74.';

-- The owner is a plain column with a foreign key and no Java association. ArchitectureRulesTest
-- forbids the cashflow slice from depending on identity, so the relationship between the two tables
-- is expressed here and nowhere in application code -- the same shape V3, V4 and V7 chose.
COMMENT ON COLUMN wealth.cash_flow_entries.user_id IS
    'Owner. Every query is scoped to it server-side; no request ever carries it.';

COMMENT ON COLUMN wealth.cash_flow_entries.entry_type IS
    'INCOME or EXPENSE. Carries the direction of the money, so amount is always positive.';

COMMENT ON COLUMN wealth.cash_flow_entries.category IS
    'One of the seven income or nine expense categories, constrained to match entry_type.';

-- NUMERIC, never a floating-point type. ARCHITECTURE_TECHNOLOGY_DECISIONS.md:140 requires exact
-- decimals for money, and this column is summed into monthly totals and multiplied by a user-declared
-- exchange rate. Scale 2 serves both currencies: USD has cents, and VND amounts are whole numbers.
COMMENT ON COLUMN wealth.cash_flow_entries.amount IS
    'The amount the user entered, in this row''s currency. Always positive; entry_type gives the sign.';

COMMENT ON COLUMN wealth.cash_flow_entries.currency IS
    'VND or USD, the currency amount is expressed in. No conversion is stored.';

COMMENT ON COLUMN wealth.cash_flow_entries.timing IS
    'ONE_TIME or RECURRING. Decides whether recurrence and end_date may be present.';

COMMENT ON COLUMN wealth.cash_flow_entries.recurrence IS
    'MONTHLY, QUARTERLY or YEARLY for a recurring entry; NULL for a one-time one.';

-- The user's date, never the server's. It may be in the future: an expected salary from next January
-- is exactly the input the three-month forecast in issue #80 needs.
COMMENT ON COLUMN wealth.cash_flow_entries.effective_date IS
    'The date a one-time entry applies, or the first date of a recurring one. May be in the future.';

COMMENT ON COLUMN wealth.cash_flow_entries.end_date IS
    'The last date a recurring entry applies. NULL means open-ended. Always NULL for a one-time entry.';

-- SET NULL rather than RESTRICT or CASCADE. RESTRICT would make DELETE /api/v1/assets fail with a
-- server error over a table that slice does not know exists; CASCADE would erase a user's recorded
-- income because they removed the asset it came from. Losing the link alone is the least surprising
-- outcome, and what the user should see when that happens is issue #104's question.
COMMENT ON COLUMN wealth.cash_flow_entries.linked_asset_id IS
    'The asset this entry refers to, when any. Must belong to the same user; the API enforces that.';

-- SET NULL for the same reasons as linked_asset_id, and the same choice V7 made for a liability's
-- collateral asset.
COMMENT ON COLUMN wealth.cash_flow_entries.linked_liability_id IS
    'The liability this entry refers to, when any. Must belong to the same user; never set together with linked_asset_id.';

COMMENT ON COLUMN wealth.cash_flow_entries.notes IS
    'The user''s own note, such as "Lương tháng" or "Tiền thuê căn hộ Thảo Điền". Free text, bounded.';

-- The order the catalog lists in, so the index serves the only query the slice makes rather than
-- being a guess at a future one. user_id leads because every query is owner-scoped first.
CREATE INDEX idx_cash_flow_entries_owner_effective_date
    ON wealth.cash_flow_entries (user_id, effective_date DESC);

-- The foreign key columns, so removing an asset or a liability does not scan the whole catalog to
-- find the rows to null out.
CREATE INDEX idx_cash_flow_entries_linked_asset ON wealth.cash_flow_entries (linked_asset_id);
CREATE INDEX idx_cash_flow_entries_linked_liability ON wealth.cash_flow_entries (linked_liability_id);
