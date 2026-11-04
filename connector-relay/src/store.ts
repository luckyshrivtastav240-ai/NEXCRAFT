import Database from "better-sqlite3";
import { mkdirSync } from "node:fs";
import { dirname } from "node:path";

const path = process.env.DATABASE_PATH ?? "./data/nexcraft-connector.sqlite";
mkdirSync(dirname(path), { recursive: true });
export const db = new Database(path);
db.pragma("journal_mode = WAL");
db.pragma("foreign_keys = ON");
db.exec(`
CREATE TABLE IF NOT EXISTS pairings (
  id TEXT PRIMARY KEY,
  device_id TEXT NOT NULL,
  device_label TEXT NOT NULL,
  code_hash TEXT NOT NULL UNIQUE,
  device_secret_hash TEXT NOT NULL,
  device_token_hash TEXT NOT NULL,
  assistant_token_hash TEXT,
  status TEXT NOT NULL CHECK(status IN ('pending','paired','revoked','expired')),
  created_at INTEGER NOT NULL,
  expires_at INTEGER NOT NULL,
  paired_at INTEGER
);
CREATE TABLE IF NOT EXISTS diagnostics (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  pairing_id TEXT NOT NULL REFERENCES pairings(id) ON DELETE CASCADE,
  kind TEXT NOT NULL,
  payload TEXT NOT NULL,
  created_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS diagnostics_pairing_created ON diagnostics(pairing_id, created_at DESC);
`);
export type Pairing = {
  id: string; device_id: string; device_label: string; code_hash: string;
  device_secret_hash: string; device_token_hash: string; assistant_token_hash: string | null;
  status: "pending" | "paired" | "revoked" | "expired";
  created_at: number; expires_at: number; paired_at: number | null;
};
export const pairingByCode = db.prepare("SELECT * FROM pairings WHERE code_hash = ?");
export const pairingById = db.prepare("SELECT * FROM pairings WHERE id = ?");
export const insertPairing = db.prepare(`INSERT INTO pairings
(id,device_id,device_label,code_hash,device_secret_hash,device_token_hash,status,created_at,expires_at)
VALUES (@id,@device_id,@device_label,@code_hash,@device_secret_hash,@device_token_hash,'pending',@created_at,@expires_at)`);
export const completePairing = db.prepare("UPDATE pairings SET status='paired', assistant_token_hash=?, paired_at=? WHERE id=? AND status='pending'");
export const expirePairing = db.prepare("UPDATE pairings SET status='expired' WHERE status='pending' AND expires_at < ?");
export const revokePairing = db.prepare("UPDATE pairings SET status='revoked' WHERE id=?");
export const insertDiagnostic = db.prepare("INSERT INTO diagnostics(pairing_id,kind,payload,created_at) VALUES (?,?,?,?)");
export const listDiagnostics = db.prepare("SELECT id,kind,payload,created_at FROM diagnostics WHERE pairing_id=? ORDER BY created_at DESC LIMIT ?");
