-- Failed sign-ins and sign-ups, counted per key (an email or a network address) over a short window.
CREATE TABLE throttle (
  key TEXT NOT NULL,
  at INTEGER NOT NULL
);
CREATE INDEX throttle_by_key ON throttle(key, at);
-- Stands in for deleted accounts so other players' replies under a deleted comment stay in place.
INSERT INTO accounts (id, display_name, role, banned, created_at) VALUES ('deleted', '[deleted]', 'player', 0, 0);
