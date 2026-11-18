-- Month 2: upvotes (one per account per submission, never your own). Tournament: week 1 picks (up to 8 per account)
-- and weeks 2-4 one vote per account per matchup. All changeable until the phase/week ends.
CREATE TABLE upvotes (
  account_id TEXT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
  submission_id TEXT NOT NULL REFERENCES submissions(id) ON DELETE CASCADE,
  created_at INTEGER NOT NULL,
  PRIMARY KEY (account_id, submission_id)
);
CREATE TABLE tournament_votes (
  season_id TEXT NOT NULL REFERENCES seasons(id) ON DELETE CASCADE,
  week INTEGER NOT NULL,            -- 1..4
  slot INTEGER NOT NULL,            -- week 1: pick number 0..7; weeks 2-4: matchup index
  account_id TEXT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
  submission_id TEXT NOT NULL REFERENCES submissions(id) ON DELETE CASCADE,
  created_at INTEGER NOT NULL,
  PRIMARY KEY (season_id, week, slot, account_id)
);
