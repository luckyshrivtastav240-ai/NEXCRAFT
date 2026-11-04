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
