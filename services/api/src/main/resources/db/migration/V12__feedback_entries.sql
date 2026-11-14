-- In-product feedback: one row per message a pilot user sends from a screen of the app. The MVP 1D
-- acceptance criterion is that feedback "can be associated with a user and screen without recording
-- sensitive financial payloads", so the row carries the owner, a screen code and the user's own words,
-- and nothing the client did not type into the feedback form -- no snapshot of the screen's data, no
-- device fingerprint, no copy of the request.
--
-- The message is stored verbatim because it is the product data this table exists for, and the user
-- reads it back. It is never written to an application log; that rule lives in the logging
-- configuration and docs/backend-deployment.md, not here.
--
-- As in every other slice, each rule exists twice: bean validation refuses it at the API boundary with
-- a named field, and a check constraint refuses it for any caller that is not the API.

CREATE TABLE wealth.feedback_entries (
    id          UUID        PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES wealth.users (id) ON DELETE CASCADE,
    screen      TEXT        NOT NULL,
    category    TEXT,
    message     TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,

    -- A route-like code such as 'dashboard', 'catalog/assets' or 'goals/new': lowercase ASCII letters
    -- and digits in segments joined by '-' or '/'. Narrow on purpose. A screen code that admitted
    -- spaces, diacritics, '.', '?' or '=' could carry a sentence, an amount such as 4.200.000.000 or a
    -- query string with a token in it, and this column is meant to be safe to group and count by.
    CONSTRAINT feedback_entries_screen_is_a_code
        CHECK (screen ~ '^[a-z0-9]+([-/][a-z0-9]+)*$' AND char_length(screen) <= 64),

    -- Optional. NULL is "the user did not say", which is different from OTHER.
    CONSTRAINT feedback_entries_category_is_known
        CHECK (category IS NULL OR category IN ('BUG', 'IDEA', 'QUESTION', 'OTHER')),

    -- btrim of the ASCII space only; the API applies the wider Unicode notion of blank before a row
    -- ever reaches this check, so this is the floor for other callers rather than the whole rule.
    CONSTRAINT feedback_entries_message_is_not_blank
        CHECK (btrim(message) <> ''),

    -- TEXT with a length check rather than VARCHAR(n): widening a check is a one-line migration.
    CONSTRAINT feedback_entries_message_is_bounded
        CHECK (char_length(message) <= 2000)
);

COMMENT ON TABLE wealth.feedback_entries IS
    'Feedback a pilot user sent from a screen of the app. Readable back by its author only; never logged.';

COMMENT ON COLUMN wealth.feedback_entries.user_id IS
    'Author, taken from the access token. No request ever carries it.';

COMMENT ON COLUMN wealth.feedback_entries.screen IS
    'Route-like screen code such as catalog/assets, at most 64 characters.';

COMMENT ON COLUMN wealth.feedback_entries.category IS
    'BUG, IDEA, QUESTION or OTHER; NULL when the user did not choose one.';

COMMENT ON COLUMN wealth.feedback_entries.message IS
    'The user''s own words, stripped of surrounding blank space, at most 2000 characters.';

-- The only query the slice makes: one author's feedback, newest first.
CREATE INDEX idx_feedback_entries_owner_created_at
    ON wealth.feedback_entries (user_id, created_at DESC);
