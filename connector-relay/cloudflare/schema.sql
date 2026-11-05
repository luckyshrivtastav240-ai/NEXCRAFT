CREATE TABLE IF NOT EXISTS pairings (
  id TEXT PRIMARY KEY,
  device_id TEXT NOT NULL,
  device_label TEXT NOT NULL,
  code_hash TEXT NOT NULL UNIQUE,
  device_secret_hash TEXT NOT NULL,
  device_token_hash TEXT NOT NULL,
  device_token_cipher TEXT,
  assistant_token_hash TEXT,
  status TEXT NOT NULL CHECK(status IN ('pending','paired','revoked','expired')),
  created_at INTEGER NOT NULL,
  expires_at INTEGER NOT NULL,
  paired_at INTEGER
);
CREATE INDEX IF NOT EXISTS pairings_code_hash ON pairings(code_hash);
CREATE TABLE IF NOT EXISTS diagnostics (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  pairing_id TEXT NOT NULL REFERENCES pairings(id) ON DELETE CASCADE,
  kind TEXT NOT NULL,
  payload TEXT NOT NULL,
  created_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS diagnostics_pairing_created ON diagnostics(pairing_id,created_at DESC);
CREATE TABLE IF NOT EXISTS rate_limits (
  bucket TEXT PRIMARY KEY,
  window_id INTEGER NOT NULL,
  hits INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS dev_projects (
  id TEXT PRIMARY KEY,
  pairing_id TEXT NOT NULL REFERENCES pairings(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  project_type TEXT NOT NULL,
  brief TEXT NOT NULL,
  status TEXT NOT NULL CHECK(status IN ('planned','analyzing','building','testing','blocked','completed','failed')),
  progress INTEGER NOT NULL DEFAULT 0 CHECK(progress BETWEEN 0 AND 100),
  current_stage TEXT NOT NULL DEFAULT 'Queued',
  summary TEXT NOT NULL DEFAULT '',
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS dev_projects_pairing_updated ON dev_projects(pairing_id,updated_at DESC);
CREATE TABLE IF NOT EXISTS dev_project_events (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  project_id TEXT NOT NULL REFERENCES dev_projects(id) ON DELETE CASCADE,
  event_type TEXT NOT NULL,
  message TEXT NOT NULL,
  details TEXT NOT NULL DEFAULT '{}',
  created_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS dev_events_project_created ON dev_project_events(project_id,created_at DESC);
