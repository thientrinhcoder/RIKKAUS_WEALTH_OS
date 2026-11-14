-- Financial goals: one row per goal a user is saving towards, across the seven templates the MVP
-- Backbone names. One generic shape for every template. A template is a value of the type column
-- plus a set of suggested defaults that live in application code, so there is no template table:
-- a table would be seven rows nobody edits, read on every form open, and a second place for the
-- same numbers to drift from the accepted design.
--
-- Every rule below exists in two places on purpose: bean validation refuses it at the API boundary
-- with a named field, and a check constraint refuses it for any caller that is not the API. What
-- the table deliberately does not check is the target date against the current day. A CHECK with
-- now() is not immutable, and it would also refuse every later edit of a goal whose date has passed;
-- that rule belongs to the service, which knows whether the date in the request is new.

CREATE TABLE wealth.goals (
    id                             UUID           PRIMARY KEY,
    user_id                        UUID           NOT NULL REFERENCES wealth.users (id) ON DELETE CASCADE,

    name                           TEXT           NOT NULL,
    type                           TEXT           NOT NULL,
    currency                       TEXT           NOT NULL DEFAULT 'VND',
    target_amount                  NUMERIC(19, 2) NOT NULL,
    current_allocated_amount       NUMERIC(19, 2) NOT NULL DEFAULT 0,
    target_date                    DATE           NOT NULL,
    planned_monthly_contribution   NUMERIC(19, 2),
    expected_annual_return_percent NUMERIC(5, 2),

    created_at                     TIMESTAMPTZ    NOT NULL,
    updated_at                     TIMESTAMPTZ    NOT NULL,

    -- The seven templates, enumerated here as well as in GoalType. An eighth is then a reviewed
    -- migration with a diff somebody reads rather than a typo that splits a user's goals in every
    -- later report.
    CONSTRAINT goals_type_is_known
        CHECK (type IN (
            'EMERGENCY_FUND',
            'HOME_PURCHASE',
            'EDUCATION',
            'RETIREMENT',
            'NET_WORTH_GROWTH',
            'DEBT_REDUCTION',
            'OTHER_GOAL'
        )),

    -- Every monetary field of a goal is in the reporting currency. The accepted design for issue #84
    -- states each amount as VND and lists multi-currency goals as out of scope, so the column is
    -- stored with a check rather than left implicit in code -- the shape user_settings.base_currency
    -- already uses -- and allowing a second currency later is a reviewed migration.
    CONSTRAINT goals_currency_is_vnd
        CHECK (currency = 'VND'),

    -- Strictly positive. A goal of zero has nothing to reach, and every ratio issue #89 builds on top
    -- of this column divides by it.
    CONSTRAINT goals_target_amount_is_positive
        CHECK (target_amount > 0),

    -- Zero is the honest value for "nothing set aside yet", which is why the column is NOT NULL with
    -- a default rather than nullable: issue #89's completion percentage must never meet a null here.
    CONSTRAINT goals_allocated_amount_is_not_negative
        CHECK (current_allocated_amount >= 0),

    -- The accepted design caps the allocated amount at the target. A completion percentage above one
    -- hundred would otherwise be a stored fact rather than a display rule.
    CONSTRAINT goals_allocated_amount_within_target
        CHECK (current_allocated_amount <= target_amount),

    -- Optional and distinct from zero. The design renders a goal with no planned contribution as
    -- "status unavailable", and a goal whose user plans to contribute nothing as a goal that is
    -- behind; collapsing the two into 0 would erase that distinction for issue #89.
    CONSTRAINT goals_contribution_is_not_negative
        CHECK (planned_monthly_contribution IS NULL OR planned_monthly_contribution >= 0),

    -- A user-entered assumption, held to two decimals of a percent, bounded as the accepted design
    -- states. Never a floating-point type: it is compounded monthly over as many as 180 months.
    CONSTRAINT goals_expected_return_within_bounds
        CHECK (expected_annual_return_percent IS NULL
               OR (expected_annual_return_percent >= 0 AND expected_annual_return_percent <= 30)),

    CONSTRAINT goals_name_is_present_and_bounded
        CHECK (btrim(name) <> '' AND char_length(name) <= 120)
);

COMMENT ON TABLE wealth.goals IS
    'One row per financial goal. One generic shape for all seven templates; the template is the type column.';

COMMENT ON COLUMN wealth.goals.user_id IS
    'Owner. Every query is scoped to it server-side; no request ever carries it.';

COMMENT ON COLUMN wealth.goals.type IS
    'Which of the seven templates the goal was created from. Editable, because no field depends on it.';

COMMENT ON COLUMN wealth.goals.currency IS
    'Fixed to VND. Stored so a second goal currency is a migration, not a code change.';

COMMENT ON COLUMN wealth.goals.target_amount IS
    'The amount the user wants to reach, in VND. Entered by the user; strictly positive.';

COMMENT ON COLUMN wealth.goals.current_allocated_amount IS
    'What the user has already set aside for this goal, in VND. Zero when nothing yet; never above target_amount.';

COMMENT ON COLUMN wealth.goals.target_date IS
    'The date the user wants to reach the goal by. Must be after the day of creation; an overdue goal keeps its date.';

COMMENT ON COLUMN wealth.goals.planned_monthly_contribution IS
    'What the user plans to add each month, in VND. NULL means not stated, which is not the same as zero.';

COMMENT ON COLUMN wealth.goals.expected_annual_return_percent IS
    'A user-entered assumption in percent per year, 0 to 30. NULL means no assumption. Never a promise.';

-- The order the list route uses: nearest target date first. user_id leads because every query is
-- owner-scoped first.
CREATE INDEX idx_goals_owner_target_date ON wealth.goals (user_id, target_date);
