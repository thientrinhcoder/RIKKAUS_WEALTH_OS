-- The valuation history: one row every time a user states what an asset is worth, and never fewer.
--
-- The MVP Backbone requires that every meaningful change to an asset's current value creates a new
-- record (RIKKAUS_WEALTH_OS_MVP_BACKBONE.md:148-159), so that the history is auditable and a trend can be
-- drawn without a price feed. The three valuation columns on wealth.assets stay where issue #53 put
-- them and become a denormalised snapshot of the current record, kept in step by the application in
-- the same transaction that inserts the record. Reading the catalog therefore costs no join, and the
-- history is the audit trail the snapshot is read from.
--
-- This file is numbered V6 and not V5 because issue #56's branch, unmerged while this was written,
-- already carries V5__asset_instrument_details.sql. Flyway refuses two migrations with one version
-- and, with out-of-order disabled, refuses a lower version arriving after a higher one has run in an
-- environment. Whichever of the two merges second has to look at this.
--
-- Every rule below exists in two places on purpose, as in V4: bean validation refuses it at the API
-- boundary with a named field, and the table refuses it for any caller that is not the API.

CREATE TABLE wealth.asset_valuations (
    id                    UUID           PRIMARY KEY,
    asset_id              UUID           NOT NULL REFERENCES wealth.assets (id) ON DELETE CASCADE,

    -- What the user said, exactly as they said it.
    value                 NUMERIC(19, 2) NOT NULL,
    currency              TEXT           NOT NULL,
    effective_date        DATE           NOT NULL,

    -- The rate snapshot the accepted design for issue #57 calls "Tỷ giá USD/VND đã dùng". Present
    -- only for a USD record, and then both columns or neither.
    usd_to_vnd_rate       NUMERIC(19, 6),
    rate_effective_date   DATE,

    note                  TEXT,

    -- The record this one corrects, when it is a correction. The original row is never touched.
    corrects_valuation_id UUID,

    -- The server's clock, never the user's. Distinct from effective_date for the same reason
    -- user_settings keeps rate_updated_at apart from rate_effective_date: "the value I entered today
    -- for last Friday" has to stay representable.
    recorded_at           TIMESTAMPTZ    NOT NULL,

    -- A correction must name a record of the same asset. A plain foreign key on the id alone would
    -- accept a record of another asset, or of another user's asset, and nothing in the table would
    -- notice. Referencing the (id, asset_id) pair makes "same asset" something the table checks.
    CONSTRAINT asset_valuations_id_and_asset_are_unique
        UNIQUE (id, asset_id),
    CONSTRAINT asset_valuations_correction_names_a_record_of_this_asset
        FOREIGN KEY (corrects_valuation_id, asset_id)
        REFERENCES wealth.asset_valuations (id, asset_id),
    CONSTRAINT asset_valuations_correction_is_not_itself
        CHECK (corrects_valuation_id IS NULL OR corrects_valuation_id <> id),

    -- Zero is allowed and negative is not, as on wealth.assets and for the same reason.
    CONSTRAINT asset_valuations_value_is_not_negative
        CHECK (value >= 0),

    CONSTRAINT asset_valuations_currency_is_supported
        CHECK (currency IN ('VND', 'USD')),

    -- An exact decimal cannot protect itself from a zero.
    CONSTRAINT asset_valuations_rate_is_positive
        CHECK (usd_to_vnd_rate IS NULL OR usd_to_vnd_rate > 0),

    -- Both or neither. A rate with no date cannot be reconciled later, and a date with no rate
    -- describes nothing.
    CONSTRAINT asset_valuations_rate_is_complete
        CHECK (
            (usd_to_vnd_rate IS NULL AND rate_effective_date IS NULL)
            OR (usd_to_vnd_rate IS NOT NULL AND rate_effective_date IS NOT NULL)
        ),

    -- A VND record carries no rate: there is nothing to convert. Permitting one would store a figure
    -- that no reader could apply to anything, and that a later report might apply anyway.
    CONSTRAINT asset_valuations_rate_only_for_usd
        CHECK (currency = 'USD' OR usd_to_vnd_rate IS NULL),

    CONSTRAINT asset_valuations_note_is_bounded
        CHECK (note IS NULL OR char_length(note) <= 2000)
);

COMMENT ON TABLE wealth.asset_valuations IS
    'One row per valuation ever recorded for an asset. Append-only: rows are never updated, and deleted only with their asset.';

COMMENT ON COLUMN wealth.asset_valuations.value IS
    'The value the user entered, in this row''s currency. Manually entered; there is no price feed.';

COMMENT ON COLUMN wealth.asset_valuations.currency IS
    'VND or USD. Stored on every row as the backbone requires, although the application holds it equal to the asset''s.';

COMMENT ON COLUMN wealth.asset_valuations.effective_date IS
    'The date the user states the value was true. Theirs, not the server''s. Decides which record is current.';

COMMENT ON COLUMN wealth.asset_valuations.usd_to_vnd_rate IS
    'How many VND one USD bought when this value was recorded, as the user used it. NULL on a VND row, and NULL when the user did not capture it.';

COMMENT ON COLUMN wealth.asset_valuations.rate_effective_date IS
    'The date the user states that rate took effect. Moves with usd_to_vnd_rate: both or neither.';

COMMENT ON COLUMN wealth.asset_valuations.corrects_valuation_id IS
    'The record this one corrects, of the same asset. The original stays readable; this row carries the reason in note.';

COMMENT ON COLUMN wealth.asset_valuations.recorded_at IS
    'When the server stored this row. Breaks the tie between two records with one effective_date: the later recorded wins.';

-- The listing order and the current-record lookup are the same query, so one index serves both.
-- The current record is the first row of a history ordered this way: latest effective date, and
-- among those the latest recorded. asset_id leads because every query is scoped to one asset.
CREATE INDEX idx_asset_valuations_history
    ON wealth.asset_valuations (asset_id, effective_date DESC, recorded_at DESC);

-- Every asset that already exists gets the first record of its history, now, from its own snapshot.
--
-- V4 is on main and deployed, so there are assets with a value and no history. Without this an asset
-- created before today would answer an empty history, contradicting the contract's "at least one
-- record"; worse, the first back-dated record appended to it would become its only record and so its
-- current one, while the catalog kept showing the older snapshot. The rate columns stay NULL because
-- the snapshot never held a rate -- that is the accepted design's partial state, and inventing one here
-- would be fabricated data. recorded_at is the asset's created_at, which is when that value was in fact
-- recorded. The NOT EXISTS guard is what makes this statement say exactly what it means: a first record
-- for an asset that has none, and nothing for one that has.
INSERT INTO wealth.asset_valuations (id, asset_id, value, currency, effective_date, recorded_at)
SELECT gen_random_uuid(), a.id, a.current_value, a.currency, a.valuation_date, a.created_at
  FROM wealth.assets a
 WHERE NOT EXISTS (SELECT 1 FROM wealth.asset_valuations v WHERE v.asset_id = a.id);

-- Append-only, enforced by the table and not only by the absence of a route.
--
-- The entity maps every column updatable = false and the repository declares no delete, so the
-- application cannot change a row by accident. This trigger is for everything that is not the
-- application: a migration, a manual correction in psql, the next slice. An UPDATE is refused
-- outright. A DELETE is refused while the parent asset still exists, which is what distinguishes
-- somebody deleting history from the cascade that runs when the asset itself is removed: by the time
-- ON DELETE CASCADE reaches these rows the asset row is already gone, so the lookup below finds
-- nothing and the delete proceeds. Hardening asset deletion is issue #102's; this keeps the history
-- exactly as durable as the asset it describes, and not one row less.
CREATE FUNCTION wealth.refuse_asset_valuation_change() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        RAISE EXCEPTION 'wealth.asset_valuations is append-only: a valuation is never updated. Record a new one.'
            USING ERRCODE = 'restrict_violation';
    END IF;
    IF EXISTS (SELECT 1 FROM wealth.assets WHERE id = OLD.asset_id) THEN
        RAISE EXCEPTION 'wealth.asset_valuations is append-only: a valuation is never deleted while its asset exists.'
            USING ERRCODE = 'restrict_violation';
    END IF;
    RETURN OLD;
END;
$$;

CREATE TRIGGER asset_valuations_are_append_only
    BEFORE UPDATE OR DELETE ON wealth.asset_valuations
    FOR EACH ROW EXECUTE FUNCTION wealth.refuse_asset_valuation_change();
