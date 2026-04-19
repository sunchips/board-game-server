CREATE TABLE game_records (
    id              UUID PRIMARY KEY,
    game            TEXT NOT NULL,
    year_published  INTEGER,
    variants        JSONB NOT NULL DEFAULT '[]'::jsonb,
    date            DATE NOT NULL,
    player_count    INTEGER NOT NULL,
    winners         JSONB NOT NULL,
    notes           TEXT,
    players         JSONB NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_game_records_game ON game_records (game);
CREATE INDEX idx_game_records_date ON game_records (date DESC);
CREATE INDEX idx_game_records_created_at ON game_records (created_at DESC);
