-- The optional instrument half of an asset: the five type-specific fields the MVP Backbone names for
-- securities, funds, bonds and deposits. Four of them are columns here; the fifth, quantity, is
-- already a common optional column in V4 and stays one, because the backbone lists it in both sets
-- and it is one datum rather than two.
--
-- Every value is entered by the user. There is no price feed in MVP 1A, and no column here is
-- derived from any other: current_value is the user's own figure and is never
-- quantity * current_price_per_unit. That is stated in a comment because it cannot be stated in a
-- constraint, and a reader looking at a quantity and a unit price together will otherwise assume the
-- product was checked.
--
-- As in V4, every rule lands twice: bean validation refuses it at the API boundary with a named
-- field, and a check constraint refuses it for any caller that is not the API -- a migration, a
-- manual correction, or the next slice.

ALTER TABLE wealth.assets
    ADD COLUMN symbol                 TEXT,
    ADD COLUMN cost_per_unit          NUMERIC(19, 6),
    ADD COLUMN current_price_per_unit NUMERIC(19, 6),
    ADD COLUMN interest_rate          NUMERIC(7, 4);

-- Bounded and non-blank when present, the shape V4 chose for name. 32 characters holds every ticker,
-- ISIN and fund code a pilot user will type, and a symbol of only blank space is a user who typed
-- nothing into an optional field rather than a user whose symbol is three spaces.
ALTER TABLE wealth.assets
    ADD CONSTRAINT assets_symbol_is_present_and_bounded
        CHECK (symbol IS NULL OR (btrim(symbol) <> '' AND char_length(symbol) <= 32));

-- Zero is allowed and negative is not, for V4's reason: a unit that is currently worth nothing is a
-- real state a user may want to record, while a negative price describes nothing that can be held.
ALTER TABLE wealth.assets
    ADD CONSTRAINT assets_cost_per_unit_is_not_negative
        CHECK (cost_per_unit IS NULL OR cost_per_unit >= 0),
    ADD CONSTRAINT assets_current_price_per_unit_is_not_negative
        CHECK (current_price_per_unit IS NULL OR current_price_per_unit >= 0);

-- A percent per year, not a fraction: 6.2500 is six and a quarter percent. Bounded at 100 because a
-- rate above it is a data-entry error -- a user who meant 6.25 and typed 625 -- and the bound is what
-- turns that into a named refusal rather than a figure a later report presents as real. Negative is
-- refused: a negative expected yield is not something this product asks a pilot user to record.
ALTER TABLE wealth.assets
    ADD CONSTRAINT assets_interest_rate_is_a_percent
        CHECK (interest_rate IS NULL OR (interest_rate >= 0 AND interest_rate <= 100));

-- The accepted design's visibility matrix, enforced rather than only drawn. The matrix is in
-- plans/260914-1938-issue-54-type-specific-asset-details/phase-01-field-contract-and-fixtures.md and
-- the cells are reproduced in this task's plan.md. Three of them read oddly enough to say out loud:
--
--   * LISTED_SECURITIES refuses interest_rate. The matrix cell is "Khong". A listed share has no
--     rate, and accepting one would invite a dividend yield the MVP never calculates.
--   * INVESTMENT_FUND refuses interest_rate. The matrix cell is "Ghi chu tuy chon" -- an optional
--     note. A fund's expected return belongs in notes, where nothing mistakes it for a computed one.
--   * OTHER_ASSET accepts everything. The matrix cell is "Mac dinh khong hien thi", which is hidden
--     by default and not forbidden; refusing it would be a rule nobody agreed to.
--
-- category is updatable = false on the entity and in V4, so a row that satisfies this today cannot be
-- moved to a category that refuses its fields tomorrow.
ALTER TABLE wealth.assets
    ADD CONSTRAINT assets_instrument_pricing_suits_the_category
        CHECK (
            (symbol IS NULL AND cost_per_unit IS NULL AND current_price_per_unit IS NULL)
            OR category IN ('LISTED_SECURITIES', 'BOND', 'INVESTMENT_FUND', 'OTHER_ASSET')
        ),
    ADD CONSTRAINT assets_interest_rate_suits_the_category
        CHECK (
            interest_rate IS NULL
            OR category IN ('BOND', 'TERM_DEPOSIT', 'OTHER_ASSET')
        );

COMMENT ON COLUMN wealth.assets.symbol IS
    'Ticker, ISIN or fund code, as the user typed it. Free text; nothing resolves it against a market.';

COMMENT ON COLUMN wealth.assets.cost_per_unit IS
    'What one unit cost, in this row''s currency. User-entered. Never multiplied by quantity here.';

COMMENT ON COLUMN wealth.assets.current_price_per_unit IS
    'What one unit is worth now, in this row''s currency. User-entered; current_value is separate and is not derived from it.';

COMMENT ON COLUMN wealth.assets.interest_rate IS
    'Interest rate or expected yield as a percent per year: 6.2500 is 6.25%. User-entered; no interest is accrued or projected.';

COMMENT ON TABLE wealth.assets IS
    'One row per asset a user owns. Common fields for all eleven categories, plus the optional instrument fields the visibility matrix allows per category.';
