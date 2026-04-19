-- User identities: each row corresponds to one Apple Sign-In identity.
-- `apple_sub` is the stable user identifier from Apple's ID token.
CREATE TABLE users (
    id            UUID PRIMARY KEY,
    apple_sub     TEXT NOT NULL UNIQUE,
    email         TEXT,
    display_name  TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Saved player roster, scoped per user.
CREATE TABLE players (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        TEXT NOT NULL,
    email       TEXT,
    notes       TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_players_user_id ON players (user_id, name);

-- Every record now belongs to a user. There is no prior prod data so the FK is
-- added as NOT NULL directly; existing dev rows (if any) are cleared.
DELETE FROM game_records;
ALTER TABLE game_records
    ADD COLUMN user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE;
CREATE INDEX idx_game_records_user_id ON game_records (user_id, created_at DESC);
