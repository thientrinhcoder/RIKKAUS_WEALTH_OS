-- Where a user stands in the guided onboarding, and nothing else.
--
-- Whether each checklist step is done is not stored here. It is read from the records the step asks
-- for -- the declared rate, the assets, liabilities, recurring cash-flow entries and goals -- every time
-- the checklist is requested, so it can never disagree with them and deleting a record undoes its step.
-- That also keeps every financial figure and every name out of this table: a row is a step number and
-- three timestamps.
--
-- The one step with no record of its own is reviewing the first dashboard, so the client reports it and
-- the moment it did so is kept in dashboard_reviewed_at.
--
-- A row exists only once the user has written progress. Reading the checklist never inserts one,
-- because the default state -- step 1, nothing reviewed, neither completed nor dismissed -- is fully
-- derivable without it.

CREATE TABLE wealth.onboarding_progress (
    user_id               UUID        PRIMARY KEY REFERENCES wealth.users (id) ON DELETE CASCADE,
    current_step          SMALLINT    NOT NULL DEFAULT 1,
    dashboard_reviewed_at TIMESTAMPTZ,
    completed_at          TIMESTAMPTZ,
    dismissed_at          TIMESTAMPTZ,
    created_at            TIMESTAMPTZ NOT NULL,
    updated_at            TIMESTAMPTZ NOT NULL,

    -- The backbone's onboarding sequence has seven steps. Bean validation refuses anything else at the
    -- API boundary; this refuses it for any caller that is not the API.
    CONSTRAINT onboarding_progress_current_step_is_known
        CHECK (current_step BETWEEN 1 AND 7),

    -- Completing the onboarding and dismissing it are two ends of the same flow. A row that claims both
    -- has no status the client could render, so it cannot be written.
    CONSTRAINT onboarding_progress_completed_or_dismissed
        CHECK (completed_at IS NULL OR dismissed_at IS NULL)
);
