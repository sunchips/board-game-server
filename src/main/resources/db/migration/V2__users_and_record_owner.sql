CREATE TABLE users (
    id          UUID PRIMARY KEY,
    apple_sub   TEXT NOT NULL UNIQUE,
    email       TEXT,
    name        TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE game_records
    ADD COLUMN user_id UUID REFERENCES users (id);

CREATE INDEX idx_game_records_user_id ON game_records (user_id);
