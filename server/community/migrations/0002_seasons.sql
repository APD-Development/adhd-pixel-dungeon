-- Community seasons: Month 1 submissions, Month 2 voting to a top 16, Month 3 weekly elimination tournament.
-- Phase boundaries are stored, so a test season can run on minutes instead of months.
CREATE TABLE seasons (
  id TEXT PRIMARY KEY,
  title TEXT NOT NULL,
  starts_at INTEGER NOT NULL,       -- submissions open
  voting_at INTEGER NOT NULL,       -- submissions close, voting opens
  tournament_at INTEGER NOT NULL,   -- voting closes, the top 16 enter the tournament
  ends_at INTEGER NOT NULL,         -- tournament over
  created_at INTEGER NOT NULL
);
ALTER TABLE submissions ADD COLUMN season_id TEXT REFERENCES seasons(id);
CREATE INDEX submissions_by_season ON submissions(season_id, created_at);
