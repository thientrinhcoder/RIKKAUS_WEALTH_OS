-- Per-user settings, and the display name a user chose for themselves.
--
-- The exchange rate is user-declared and carries two different dates. Keeping them in separate
-- columns rather than deriving one from the other is the whole point of the audit requirement: the
-- user says when they consider the rate effective, and the server says when it stored that
-- statement. A single timestamp would make "the rate I set yesterday for last Friday" unrepresentable.

CREATE TABLE wealth.user_settings (
    user_id             UUID        PRIMARY KEY REFERENCES wealth.users (id) ON DELETE CASCADE,
    base_currency       TEXT        NOT NULL DEFAULT 'VND',
    usd_to_vnd_rate     NUMERIC(19, 6),
    rate_effective_date DATE,
    rate_updated_at     TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL,

    -- VND is the reporting currency for MVP 1A and is not a user choice. Storing it with a check
    -- rather than leaving it implicit in code means allowing a second reporting currency is a
    -- reviewed migration, and that nothing can set one in the meantime.
    CONSTRAINT user_settings_base_currency_is_vnd
        CHECK (base_currency = 'VND'),

    -- An exact decimal cannot protect itself from a zero. Bean validation refuses one at the API
    -- boundary; this refuses it for any caller that is not the API.
    CONSTRAINT user_settings_rate_is_positive
        CHECK (usd_to_vnd_rate IS NULL OR usd_to_vnd_rate > 0),

    -- All three rate columns move together or not at all. A half-saved rate -- a value with no
    -- declared date, or a date with no value -- is a state the client has no way to render, and the
    -- accepted design for issue #48 has no copy for it.
    CONSTRAINT user_settings_rate_is_complete
        CHECK (
            (usd_to_vnd_rate IS NULL AND rate_effective_date IS NULL AND rate_updated_at IS NULL)
            OR (usd_to_vnd_rate IS NOT NULL AND rate_effective_date IS NOT NULL AND rate_updated_at IS NOT NULL)
        )
);

COMMENT ON TABLE wealth.user_settings IS
    'One row per user. Created on first read of the settings resource, not at sign-up.';

-- The primary key is the owner. There is no separate surrogate id, because the resource is a
-- singleton per user: a second row for the same user would be a bug, and this makes it unexpressible
-- rather than something a uniqueness check has to catch.
COMMENT ON COLUMN wealth.user_settings.user_id IS
    'Owner and primary key. The settings resource is a singleton addressed by the access token.';

COMMENT ON COLUMN wealth.user_settings.base_currency IS
    'Fixed to VND for MVP 1A. Stored so a future reporting currency is a migration, not a code change.';

-- NUMERIC, never a floating-point type. ARCHITECTURE_TECHNOLOGY_DECISIONS.md:140 requires exact
-- decimals for money and rates, and a rate is multiplied into every USD holding, so a binary
-- rounding error here would move a reported net-worth figure.
COMMENT ON COLUMN wealth.user_settings.usd_to_vnd_rate IS
    'How many VND one USD buys, as declared by the user. NULL until they declare one.';

COMMENT ON COLUMN wealth.user_settings.rate_effective_date IS
    'The date the user states the rate took effect. Theirs, not the server''s.';

COMMENT ON COLUMN wealth.user_settings.rate_updated_at IS
    'When the server stored the declaration. Deliberately separate from rate_effective_date.';

-- The user's own display name, kept apart from the one Google reports.
--
-- users.display_name stays the value from the verified Google identity token and is still refreshed
-- on every sign-in, which is why the edit needs somewhere else to live: a single column would be
-- overwritten the next time the user signed in, and the edit would vanish with no error anywhere.
-- Keeping Google's value also means a later "use my Google name again" feature needs no migration.
ALTER TABLE wealth.users
    ADD COLUMN preferred_display_name TEXT;

COMMENT ON COLUMN wealth.users.preferred_display_name IS
    'The display name the user set themselves. Wins over display_name when present; NULL means use Google''s.';
