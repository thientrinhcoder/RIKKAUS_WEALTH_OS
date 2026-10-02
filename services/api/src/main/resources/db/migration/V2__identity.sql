-- Identity and session state for Google Account OIDC sign-in.
--
-- There is no password column, and that is a decision rather than an omission. Issue #42's original
-- text asked for password hashing; the Product Owner decision recorded in the accepted design for
-- issue #40 replaced app-managed credentials with Google Account OIDC as the only entry method, and
-- ARCHITECTURE_TECHNOLOGY_DECISIONS.md and RIKKAUS_WEALTH_OS_MVP_BACKBONE.md were reconciled with it.
-- Adding a password column later is a migration; adding one now would be dead schema inviting a
-- second, unreviewed login path.

CREATE TABLE wealth.users (
    id             UUID        PRIMARY KEY,
    google_subject TEXT        NOT NULL UNIQUE,
    email          TEXT        NOT NULL,
    display_name   TEXT,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL
);

COMMENT ON TABLE wealth.users IS
    'One row per pilot user. Created on first successful Google sign-in.';

-- The identity key is the provider subject, not the email address. Google''s `sub` claim is stable
-- for the lifetime of the account, while the email can change. Keying on email would silently
-- attach an existing account''s data to whoever inherited that address.
COMMENT ON COLUMN wealth.users.google_subject IS
    'Google OIDC `sub` claim. The stable identity key.';

-- Deliberately NOT unique. It is refreshed from the verified claim on every sign-in so the displayed
-- address stays current. A unique constraint here would reject a legitimate Google email change with
-- an error the accepted identity design has no user-facing copy for, which is a worse outcome than
-- the duplicate it would prevent -- and a duplicate cannot arise anyway, because google_subject is
-- unique and one Google account has one subject.
COMMENT ON COLUMN wealth.users.email IS
    'Verified email from the Google identity token, refreshed on each sign-in. For display only.';

CREATE TABLE wealth.refresh_tokens (
    id             UUID        PRIMARY KEY,
    user_id        UUID        NOT NULL REFERENCES wealth.users (id) ON DELETE CASCADE,
    session_id     UUID        NOT NULL,
    token_hash     TEXT        NOT NULL UNIQUE,
    issued_at      TIMESTAMPTZ NOT NULL,
    expires_at     TIMESTAMPTZ NOT NULL,
    revoked_at     TIMESTAMPTZ,
    revoked_reason TEXT
);

COMMENT ON TABLE wealth.refresh_tokens IS
    'Rotating refresh tokens. One row per token ever issued, including rotated and revoked ones.';

-- Groups every token in one rotation chain, so replay of a stolen token can revoke the whole chain
-- rather than only the token presented. Without it, an attacker who replayed a rotated token would
-- lose that one token while the successor they had already obtained kept working.
COMMENT ON COLUMN wealth.refresh_tokens.session_id IS
    'Identifies the rotation chain a token belongs to. Survives rotation; changes only on a new sign-in.';

-- Only ever a hash. SHA-256 rather than an adaptive password hash: the raw token is 256 bits from
-- SecureRandom, so it is not guessable and there is nothing for an adaptive cost factor to defend
-- against, while paying that cost on every token refresh would be latency for no security.
COMMENT ON COLUMN wealth.refresh_tokens.token_hash IS
    'Lowercase hex SHA-256 of the raw token. The raw value is never stored or logged.';

-- Rows are revoked, never deleted. A deleted row is indistinguishable from a token that never
-- existed, which is exactly the distinction replay detection depends on.
COMMENT ON COLUMN wealth.refresh_tokens.revoked_reason IS
    'Why the token stopped being usable: rotated, logged-out, or replayed.';

CREATE INDEX idx_refresh_tokens_session ON wealth.refresh_tokens (session_id);
CREATE INDEX idx_refresh_tokens_user ON wealth.refresh_tokens (user_id);
