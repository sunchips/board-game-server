ALTER TABLE players
    ADD COLUMN is_self BOOLEAN NOT NULL DEFAULT FALSE;

-- At most one self player per user. Partial index so non-self rows are unconstrained.
CREATE UNIQUE INDEX idx_players_user_self ON players (user_id) WHERE is_self;
