-- The asset catalog: one row per thing a user owns, across the eleven categories the MVP Backbone
-- names. Common fields only. The instrument-specific fields for securities, funds and bonds belong
-- to issue #54 and are deliberately absent, because a nullable column nobody writes is schema that
-- reads as a promise.
--
-- Every rule below exists in two places on purpose: bean validation refuses it at the API boundary
-- with a named field, and a check constraint refuses it for any caller that is not the API. The
-- second is the one that still holds during a migration, a manual correction, or the next slice.

CREATE TABLE wealth.assets (
    id             UUID           PRIMARY KEY,
    user_id        UUID           NOT NULL REFERENCES wealth.users (id) ON DELETE CASCADE,

    -- The six common required fields.
    name           TEXT           NOT NULL,
    category       TEXT           NOT NULL,
    current_value  NUMERIC(19, 2) NOT NULL,
    currency       TEXT           NOT NULL,
    valuation_date DATE           NOT NULL,
    liquidity      TEXT           NOT NULL,

    -- The five common optional fields.
    purchase_value NUMERIC(19, 2),
    purchase_date  DATE,
    quantity       NUMERIC(19, 6),
    institution    TEXT,
    notes          TEXT,

    created_at     TIMESTAMPTZ    NOT NULL,
    updated_at     TIMESTAMPTZ    NOT NULL,

    -- The eleven categories, enumerated rather than left to application code. Adding a twelfth is
    -- then a reviewed migration with a diff somebody reads, which is the point: a free-text column
    -- would let a typo create a category that silently splits a user's portfolio in every later
    -- report built on top of this table.
    CONSTRAINT assets_category_is_known
        CHECK (category IN (
            'CASH',
            'BANK_ACCOUNT',
            'TERM_DEPOSIT',
            'LISTED_SECURITIES',
            'BOND',
            'INVESTMENT_FUND',
            'REAL_ESTATE',
            'PRIVATE_BUSINESS_EQUITY',
            'VEHICLE_OR_VALUABLE_ASSET',
            'COLLECTIBLE',
            'OTHER_ASSET'
        )),

    -- VND or USD, the two currencies a user may hold a value in. Distinct from
    -- user_settings.base_currency, which is the single reporting currency and is fixed to VND: a user
    -- may own a USD account while still being reported in VND.
    CONSTRAINT assets_currency_is_supported
        CHECK (currency IN ('VND', 'USD')),

    -- Required, with no default. The accepted design for issue #51 states the form offers no
    -- preselected answer, so a DEFAULT here would quietly answer a question the user was asked.
    CONSTRAINT assets_liquidity_is_known
        CHECK (liquidity IN ('HIGH', 'MEDIUM', 'LOW')),

    -- Zero is allowed and negative is not. A fully depreciated or emptied holding is a real state a
    -- user may want to keep in the catalog; a negative asset is a liability, which issue #16 owns and
    -- which has its own table rather than a sign convention on this one.
    CONSTRAINT assets_current_value_is_not_negative
        CHECK (current_value >= 0),

    CONSTRAINT assets_purchase_value_is_not_negative
        CHECK (purchase_value IS NULL OR purchase_value >= 0),

    -- Strictly positive when present. A quantity of zero describes nothing that can be held, and it
    -- would make a later cost-per-unit calculation in issue #54 a division by zero rather than an
    -- input error anyone could see.
    CONSTRAINT assets_quantity_is_positive
        CHECK (quantity IS NULL OR quantity > 0),

    -- A purchase cannot happen after the valuation it precedes. The two dates describe one history,
    -- and reversing them makes every holding-period figure built on them wrong in a way no later
    -- report could detect.
    CONSTRAINT assets_purchase_date_precedes_valuation
        CHECK (purchase_date IS NULL OR purchase_date <= valuation_date),

    -- Bounded text. Left as TEXT with a length check rather than VARCHAR(n) because widening a check
    -- is a one-line migration while widening a VARCHAR rewrites the column's type; the bound itself
    -- is here so a client cannot store an unbounded document in a notes field.
    CONSTRAINT assets_name_is_present_and_bounded
        CHECK (btrim(name) <> '' AND char_length(name) <= 120),

    CONSTRAINT assets_institution_is_bounded
        CHECK (institution IS NULL OR char_length(institution) <= 160),

    CONSTRAINT assets_notes_are_bounded
        CHECK (notes IS NULL OR char_length(notes) <= 2000)
);

COMMENT ON TABLE wealth.assets IS
    'One row per asset a user owns. Common fields only; type-specific instrument fields are issue #54.';

-- The owner is a plain column with a foreign key and no Java association. ArchitectureRulesTest
-- forbids the assets slice from depending on identity, so the relationship between the two tables is
-- expressed here and nowhere in application code -- the same shape V3 chose for user_settings.
COMMENT ON COLUMN wealth.assets.user_id IS
    'Owner. Every query is scoped to it server-side; no request ever carries it.';

-- NUMERIC, never a floating-point type. ARCHITECTURE_TECHNOLOGY_DECISIONS.md:140 requires exact
-- decimals for money, and this column is summed across a portfolio and multiplied by a user-declared
-- exchange rate, so a binary rounding error here moves a reported net-worth figure rather than a
-- displayed decimal place. Scale 2 serves both currencies: USD has cents, and VND amounts are whole
-- numbers that fit without loss.
COMMENT ON COLUMN wealth.assets.current_value IS
    'The value the user entered, in this row''s currency. Manually entered; there is no price feed.';

-- Stored beside the value rather than derived from the user's reporting currency, because a holding's
-- currency is a property of the holding. Converting to VND is not done here at all: the declared rate
-- lives in the settings slice, which this slice may not depend on, and the client already reads it.
COMMENT ON COLUMN wealth.assets.currency IS
    'VND or USD, the currency current_value and purchase_value are expressed in.';

-- The date the user says the value was true, not the row's timestamp. It is what makes a stale
-- valuation visible in the catalog, and it is never taken from the server's clock.
COMMENT ON COLUMN wealth.assets.valuation_date IS
    'The date the user states current_value was accurate. Theirs, not the server''s.';

COMMENT ON COLUMN wealth.assets.liquidity IS
    'HIGH, MEDIUM or LOW, as judged by the user. Required, and deliberately without a default.';

COMMENT ON COLUMN wealth.assets.quantity IS
    'Units held, for an asset where that is meaningful. NUMERIC(19, 6) because fund units are fractional.';

COMMENT ON COLUMN wealth.assets.institution IS
    'Where the asset is held or stored: a bank, a broker, a safe. Free text, by design.';

-- The order the catalog lists in, so the index serves the only query the slice makes rather than
-- being a guess at a future one. user_id leads because every query is owner-scoped first.
CREATE INDEX idx_assets_owner_valuation ON wealth.assets (user_id, valuation_date DESC);
