import "dotenv/config";
import express from "express";
import helmet from "helmet";
import cors from "cors";
import { randomUUID } from "node:crypto";
import { WebSocketServer } from "ws";
import { createServer } from "node:http";
import { z } from "zod";
import {
  db, pairingByCode, pairingById, insertPairing, completePairing, expirePairing,
  revokePairing, insertDiagnostic, listDiagnostics, type Pairing
} from "./store.js";
import { hash, randomToken, newCode, bearer, safeEqual, seal, unseal } from "./security.js";

const app = express();
const server = createServer(app);
const port = Number(process.env.PORT ?? 8080);
const clients = new Map<string, Set<import("ws").WebSocket>>();
const pairingInput = z.object({
  deviceId: z.string().min(8).max(128),
  deviceLabel: z.string().trim().min(1).max(80)
});
const diagnosticInput = z.object({
  kind: z.enum(["status","log","crash","screen","feature_snapshot"]),
  payload: z.unknown()
});
const MAX_PAYLOAD = 1_000_000;
app.disable("x-powered-by");
if (process.env.TRUST_PROXY === "true") app.set("trust proxy", 1);
app.use(helmet({ crossOriginResourcePolicy: { policy: "same-site" } }));
const origins = (process.env.CORS_ORIGINS ?? "").split(",").map(v => v.trim()).filter(Boolean);
app.use(cors({ origin(origin, callback) {
  if (!origin || origins.includes(origin)) return callback(null, true);
  return callback(new Error("Origin is not allowed"));
}}));
app.use(express.json({ limit: "1mb", strict: true }));
app.get("/health", (_req,res) => res.json({ ok:true, service:"nexcraft-connector-relay", api:"v1" }));

type Authenticated = express.Request & { pairing: Pairing; role: "device" | "assistant" };
function authenticate(role: "device" | "assistant"): express.RequestHandler {
  return (req,res,next) => {
    const token = bearer(req.header("authorization"));
    if (!token) return res.status(401).json({ error:"Bearer token required" });
    const digest = hash(token);
    const rows = db.prepare("SELECT * FROM pairings WHERE status='paired'").all() as Pairing[];
    const pairing = rows.find(p => safeEqual(role === "device" ? p.device_token_hash : (p.assistant_token_hash ?? ""), digest));
    if (!pairing) return res.status(401).json({ error:"Invalid or revoked connector token" });
    (req as Authenticated).pairing = pairing;
    (req as Authenticated).role = role;
    next();
  };
}
function publish(pairingId: string, event: unknown) {
  const set = clients.get(pairingId);
  if (!set) return;
  const data = JSON.stringify(event);
  for (const ws of set) if (ws.readyState === ws.OPEN) ws.send(data);
}

app.post("/v1/pairings", (req,res) => {
  const parsed = pairingInput.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error:"Invalid device details" });
  expirePairing.run(Date.now());
  const code = newCode(), deviceSecret = randomToken(), deviceToken = randomToken();
  const id = randomUUID(), now = Date.now(), expires = now + 5 * 60_000;
  insertPairing.run({
    id, device_id:parsed.data.deviceId, device_label:parsed.data.deviceLabel,
    code_hash:hash(code), device_secret_hash:hash(deviceSecret),
    device_token_hash:hash(deviceToken), device_token_cipher:seal(deviceToken), created_at:now, expires_at:expires
  });
  res.status(201).json({ pairingId:id, pairingCode:code, deviceSecret, expiresAt:new Date(expires).toISOString(), pollAfterSeconds:2 });
});

app.get("/v1/pairings/status", (req,res) => {
  const id = String(req.query.pairingId ?? "");
  const secret = String(req.header("x-device-pairing-secret") ?? "");
  const p = pairingById.get(id) as Pairing | undefined;
  if (!p || !secret || !safeEqual(p.device_secret_hash, hash(secret))) return res.status(404).json({ error:"Pairing not found" });
  if (p.status === "pending" && p.expires_at < Date.now()) { expirePairing.run(Date.now()); return res.status(410).json({ status:"expired" }); }
  if (p.status === "paired") {
    if (!p.device_token_cipher) return res.json({ status:"paired", pairingId:id, tokenDelivered:true });
    const deviceToken = unseal(p.device_token_cipher);
    const consumed = db.prepare("UPDATE pairings SET device_token_cipher=NULL WHERE id=? AND device_token_cipher=?").run(id,p.device_token_cipher);
    if (consumed.changes === 1) return res.json({ status:"paired", pairingId:id, deviceToken });
    return res.json({ status:"paired", pairingId:id, tokenDelivered:true });
  }
  return res.json({ status:p.status, expiresAt:new Date(p.expires_at).toISOString() });
});

app.post("/v1/pairings/complete", (req,res) => {
  const parsed = z.object({ pairingCode:z.string().regex(/^[A-F0-9]{10}$/) }).safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error:"Invalid pairing code" });
  expirePairing.run(Date.now());
  const p = pairingByCode.get(hash(parsed.data.pairingCode)) as Pairing | undefined;
  if (!p || p.status !== "pending" || p.expires_at < Date.now()) return res.status(404).json({ error:"Pairing code expired or not found" });
  const assistantToken = randomToken();
  const changed = completePairing.run(hash(assistantToken), Date.now(), p.id);
  if (changed.changes !== 1) return res.status(409).json({ error:"Pairing was already completed" });
  publish(p.id,{ type:"paired", at:Date.now() });
  res.json({ pairingId:p.id, assistantToken, deviceLabel:p.device_label, permissions:["read_diagnostics","read_screen_stream"], note:"Store this token securely. It is shown only once." });
});

app.post("/v1/pairings/:id/device-token", (req,res) => {
  const secret = String(req.header("x-device-pairing-secret") ?? "");
  const p = pairingById.get(req.params.id) as Pairing | undefined;
  if (!p || !secret || !safeEqual(p.device_secret_hash,hash(secret))) return res.status(404).json({ error:"Pairing not found" });
  if (p.status !== "paired") return res.status(409).json({ error:"Pairing is not approved yet" });
  if (!p.device_token_cipher) return res.status(410).json({ error:"Device token was already delivered" });
  const deviceToken = unseal(p.device_token_cipher);
  const consumed = db.prepare("UPDATE pairings SET device_token_cipher=NULL WHERE id=? AND device_token_cipher=?").run(p.id,p.device_token_cipher);
  if (consumed.changes !== 1) return res.status(409).json({ error:"Device token handoff already consumed" });
  return res.json({ deviceToken });
});

app.post("/v1/diagnostics", authenticate("device"), (req,res) => {
  const parsed = diagnosticInput.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error:"Invalid diagnostic payload" });
  const payload = JSON.stringify(parsed.data.payload);
  if (Buffer.byteLength(payload) > MAX_PAYLOAD) return res.status(413).json({ error:"Diagnostic payload too large" });
  const auth = req as Authenticated;
  const result = insertDiagnostic.run(auth.pairing.id, parsed.data.kind, payload, Date.now());
  publish(auth.pairing.id,{ type:"diagnostic", id:Number(result.lastInsertRowid), kind:parsed.data.kind, createdAt:Date.now(), payload:parsed.data.payload });
  res.status(202).json({ accepted:true, id:Number(result.lastInsertRowid) });
});
app.get("/v1/diagnostics", authenticate("assistant"), (req,res) => {
  const auth = req as Authenticated;
  const limit = Math.min(100,Math.max(1,Number(req.query.limit ?? 30)));
  const rows = listDiagnostics.all(auth.pairing.id,limit) as {id:number;kind:string;payload:string;created_at:number}[];
  res.json({ pairingId:auth.pairing.id, device:auth.pairing.device_label, items:rows.map(r=>({id:r.id,kind:r.kind,createdAt:new Date(r.created_at).toISOString(),payload:JSON.parse(r.payload)})) });
});
app.post("/v1/pairings/:id/revoke", authenticate("assistant"), (req,res) => {
  const auth = req as Authenticated;
  if (req.params.id !== auth.pairing.id) return res.status(404).json({error:"Pairing not found"});
  revokePairing.run(auth.pairing.id);
  publish(auth.pairing.id,{type:"revoked",at:Date.now()});
  res.json({revoked:true});
});

const wss = new WebSocketServer({ noServer:true, maxPayload:64*1024 });
server.on("upgrade",(request,socket,head) => {
  if (request.url !== "/v1/stream") { socket.destroy(); return; }
  wss.handleUpgrade(request,socket,head,ws => wss.emit("connection",ws,request));
});
wss.on("connection",ws => {
  let pairingId: string | null = null;
  const timer = setTimeout(()=>ws.close(4401,"Authenticate within 10 seconds"),10_000);
  ws.once("message",raw => {
    try {
      const msg = JSON.parse(raw.toString()) as { token?:string };
      const token = msg.token ?? "";
      const digest = hash(token);
      const rows = db.prepare("SELECT * FROM pairings WHERE status='paired'").all() as Pairing[];
      const p = rows.find(row=>safeEqual(row.assistant_token_hash ?? "",digest));
      if (!p) { ws.close(4401,"Invalid token"); return; }
      pairingId = p.id; clearTimeout(timer);
      if (!clients.has(p.id)) clients.set(p.id,new Set());
      clients.get(p.id)!.add(ws);
      ws.send(JSON.stringify({type:"ready",pairingId:p.id}));
    } catch { ws.close(4400,"Invalid authentication message"); }
  });
  ws.on("close",()=>{ clearTimeout(timer); if(pairingId){clients.get(pairingId)?.delete(ws); if(clients.get(pairingId)?.size===0) clients.delete(pairingId);} });
});

server.listen(port,"0.0.0.0",()=>console.log(`NEXCRAFT Connector Relay listening on :${port}`));
