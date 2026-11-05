import { handleMcp } from "./mcp";

interface D1Statement {
  bind(...values: unknown[]): D1Statement;
  first<T = Record<string, unknown>>(): Promise<T | null>;
  all<T = Record<string, unknown>>(): Promise<{ results: T[] }>;
  run(): Promise<{ meta: { changes: number; last_row_id: number } }>;
}
interface D1Database { prepare(query: string): D1Statement; }

export interface Env {
  DB: D1Database;
  PAIRING_SIGNING_SECRET: string;
}

type Pairing = {
  id: string; device_id: string; device_label: string; code_hash: string;
  device_secret_hash: string; device_token_hash: string; device_token_cipher: string | null;
  assistant_token_hash: string | null; status: string; created_at: number; expires_at: number;
};

const headers = { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" };
const json = (data: unknown, status = 200) => new Response(JSON.stringify(data), { status, headers });
const bytesToHex = (b: ArrayBuffer) => [...new Uint8Array(b)].map(x => x.toString(16).padStart(2, "0")).join("");
const hexToBytes = (s: string) => new Uint8Array((s.match(/.{2}/g) ?? []).map(x => parseInt(x, 16)));
const b64url = (b: Uint8Array) => btoa(String.fromCharCode(...b)).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
const fromB64url = (s: string) => Uint8Array.from(atob(s.replace(/-/g, "+").replace(/_/g, "/") + "===".slice((s.length + 3) % 4)), c => c.charCodeAt(0));
const random = (n = 32) => { const b = new Uint8Array(n); crypto.getRandomValues(b); return b; };
const secretKey = async (env: Env) => crypto.subtle.importKey("raw", await crypto.subtle.digest("SHA-256", new TextEncoder().encode(env.PAIRING_SIGNING_SECRET)), { name: "AES-GCM" }, false, ["encrypt", "decrypt"]);
const digest = async (env: Env, value: string) => bytesToHex(await crypto.subtle.sign("HMAC", await crypto.subtle.importKey("raw", new TextEncoder().encode(env.PAIRING_SIGNING_SECRET), { name: "HMAC", hash: "SHA-256" }, false, ["sign"]), new TextEncoder().encode(value)));
const seal = async (env: Env, value: string) => {
  const iv = random(12);
  const cipher = await crypto.subtle.encrypt({ name: "AES-GCM", iv }, await secretKey(env), new TextEncoder().encode(value));
  return [b64url(iv), b64url(new Uint8Array(cipher))].join(".");
};
const unseal = async (env: Env, value: string) => {
  const [iv, cipher] = value.split(".");
  const plain = await crypto.subtle.decrypt({ name: "AES-GCM", iv: fromB64url(iv) }, await secretKey(env), fromB64url(cipher));
  return new TextDecoder().decode(plain);
};
const uuid = () => crypto.randomUUID();
const safeEqual = (a: string, b: string) => {
  if (a.length !== b.length) return false;
  let mismatch = 0;
  for (let i = 0; i < a.length; i++) mismatch |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return mismatch === 0;
};
const authToken = (r: Request) => r.headers.get("authorization")?.match(/^Bearer ([A-Za-z0-9._~-]{20,200})$/)?.[1] ?? null;
async function bodyJson(r: Request) {
  const raw = await r.text();
  if (raw.length > 700_000) throw new Error("Request body too large");
  return JSON.parse(raw);
}
async function getPairing(env: Env, id: string) {
  return env.DB.prepare("SELECT * FROM pairings WHERE id=?").bind(id).first<Pairing>();
}
async function authPairing(env: Env, r: Request, role: "device" | "assistant") {
  const token = role === "assistant"
    ? (r.headers.get("X-Assistant-Token") ?? authToken(r))
    : authToken(r);
  if (!token) return null;
  const tokenHash = await digest(env, token);
  const field = role === "device" ? "device_token_hash" : "assistant_token_hash";
  return env.DB.prepare(`SELECT * FROM pairings WHERE status='paired' AND ${field}=?`).bind(tokenHash).first<Pairing>();
}
async function rateLimited(env: Env, r: Request) {
  const ip = r.headers.get("CF-Connecting-IP") ?? "unknown";
  const key = await digest(env, ip);
  const now = Date.now();
  const window = Math.floor(now / 60_000);
  await env.DB.prepare("DELETE FROM rate_limits WHERE window_id < ?").bind(window - 10).run();
  await env.DB.prepare(`INSERT INTO rate_limits (bucket, window_id, hits) VALUES (?, ?, 1)
    ON CONFLICT(bucket) DO UPDATE SET window_id=excluded.window_id, hits=CASE WHEN rate_limits.window_id=excluded.window_id THEN rate_limits.hits+1 ELSE 1 END`).bind(key, window).run();
  const row = await env.DB.prepare("SELECT hits FROM rate_limits WHERE bucket=? AND window_id=?").bind(key, window).first<{ hits: number }>();
  return (row?.hits ?? 0) > 8;
}
async function prune(env: Env, pairingId: string, kind: string, keep: number) {
  await env.DB.prepare(`DELETE FROM diagnostics WHERE pairing_id=? AND kind=? AND id NOT IN
    (SELECT id FROM diagnostics WHERE pairing_id=? AND kind=? ORDER BY created_at DESC LIMIT ?)`)
    .bind(pairingId, kind, pairingId, kind, keep).run();
}
function unauthorized() { return json({ error: "Invalid or revoked connector token" }, 401); }

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    try {
      if (!env.PAIRING_SIGNING_SECRET || env.PAIRING_SIGNING_SECRET.length < 32) return json({ error: "Relay secret is not configured" }, 503);
      const url = new URL(request.url);
      const path = url.pathname;
      if (path === "/mcp") return handleMcp(request);
      if (request.method === "GET" && path === "/health") {
        const dbCheck = await env.DB.prepare("SELECT 1 AS ok").first<{ ok: number }>();
        if (dbCheck?.ok !== 1) return json({ ok: false, service: "nexcraft-connector-relay", api: "v1", storage: "D1", error: "Database health check failed" }, 503);
        return json({ ok: true, service: "nexcraft-connector-relay", api: "v1", storage: "D1", database: "connected" });
      }

      if (request.method === "POST" && (path === "/v1/pairings" || path === "/v1/pairings/complete")) {
        if (await rateLimited(env, request)) return json({ error: "Too many pairing attempts. Try again shortly." }, 429);
        const input = await bodyJson(request);
        if (path === "/v1/pairings") {
          if (typeof input.deviceId !== "string" || input.deviceId.length < 8 || input.deviceId.length > 128 ||
              typeof input.deviceLabel !== "string" || !input.deviceLabel.trim() || input.deviceLabel.length > 80) return json({ error: "Invalid device details" }, 400);
          const code = [...random(5)].map(x => x.toString(16).padStart(2, "0")).join("").toUpperCase();
          const deviceSecret = b64url(random()), deviceToken = b64url(random());
          const id = uuid(), now = Date.now(), expires = now + 300_000;
          await env.DB.prepare(`INSERT INTO pairings
            (id,device_id,device_label,code_hash,device_secret_hash,device_token_hash,device_token_cipher,status,created_at,expires_at)
            VALUES (?,?,?,?,?,?,?,'pending',?,?)`).bind(id,input.deviceId,input.deviceLabel.trim(),await digest(env,code),await digest(env,deviceSecret),await digest(env,deviceToken),await seal(env,deviceToken),now,expires).run();
          return json({ pairingId:id, pairingCode:code, deviceSecret, expiresAt:new Date(expires).toISOString(), pollAfterSeconds:2 },201);
        }
        if (typeof input.pairingCode !== "string" || !/^[A-F0-9]{10}$/.test(input.pairingCode)) return json({ error: "Invalid pairing code" },400);
        const p = await env.DB.prepare("SELECT * FROM pairings WHERE code_hash=?").bind(await digest(env,input.pairingCode)).first<Pairing>();
        if (!p || p.status !== "pending" || p.expires_at < Date.now()) return json({ error:"Pairing code expired or not found" },404);
        const assistantToken = b64url(random());
        const result = await env.DB.prepare("UPDATE pairings SET status='paired',assistant_token_hash=?,paired_at=? WHERE id=? AND status='pending'")
          .bind(await digest(env,assistantToken),Date.now(),p.id).run();
        if (!result.meta.changes) return json({ error:"Pairing was already completed" },409);
        return json({ pairingId:p.id, assistantToken, deviceLabel:p.device_label, permissions:["read_diagnostics","read_screen_stream"] });
      }

      if (request.method === "GET" && path === "/v1/pairings/status") {
        const id = url.searchParams.get("pairingId") ?? "";
        const secret = request.headers.get("X-Device-Pairing-Secret") ?? "";
        const p = await getPairing(env,id);
        if (!p || !secret || !safeEqual(p.device_secret_hash, await digest(env,secret))) return json({ error:"Pairing not found" },404);
        if (p.status === "pending" && p.expires_at < Date.now()) {
          await env.DB.prepare("UPDATE pairings SET status='expired' WHERE id=? AND status='pending'").bind(id).run();
          return json({ status:"expired" },410);
        }
        if (p.status === "paired") return json({ status:"paired", pairingId:id, ...(p.device_token_cipher ? { deviceToken: await unseal(env,p.device_token_cipher) } : { tokenDelivered:true }) });
        return json({ status:p.status, expiresAt:new Date(p.expires_at).toISOString() });
      }

      const ack = path.match(/^\/v1\/pairings\/([^/]+)\/device-token\/ack$/);
      if (request.method === "POST" && ack) {
        const p = await getPairing(env,ack[1]);
        const secret = request.headers.get("X-Device-Pairing-Secret") ?? "";
        if (!p || !secret || !safeEqual(p.device_secret_hash, await digest(env,secret))) return json({ error:"Pairing not found" },404);
        if (p.status !== "paired") return json({ error:"Pairing is not approved yet" },409);
        await env.DB.prepare("UPDATE pairings SET device_token_cipher=NULL WHERE id=?").bind(p.id).run();
        return json({ acknowledged:true });
      }

      if (request.method === "POST" && path === "/v1/diagnostics") {
        const p = await authPairing(env,request,"device");
        if (!p) return unauthorized();
        const input = await bodyJson(request);
        if (!["status","log","crash","screen","feature_snapshot"].includes(input.kind) || input.payload === undefined) return json({ error:"Invalid diagnostic payload" },400);
        const payload = JSON.stringify(input.payload ?? null);
        if (new TextEncoder().encode(payload).length > 500_000) return json({ error:"Diagnostic payload too large" },413);
        const now = Date.now();
        const result = await env.DB.prepare("INSERT INTO diagnostics(pairing_id,kind,payload,created_at) VALUES (?,?,?,?)")
          .bind(p.id,input.kind,payload,now).run();
        await prune(env,p.id,input.kind,input.kind === "screen" ? 5 : 100);
        return json({ accepted:true, id:result.meta.last_row_id },202);
      }

      if (request.method === "POST" && path === "/v1/diagnostics/screen/clear") {
        const p = await authPairing(env,request,"device");
        if (!p) return unauthorized();
        await env.DB.prepare("DELETE FROM diagnostics WHERE pairing_id=? AND kind='screen'").bind(p.id).run();
        return json({ cleared:true });
      }

      if (request.method === "POST" && path === "/v1/device/disconnect") {
        const p = await authPairing(env,request,"device");
        if (!p) return unauthorized();
        await env.DB.prepare("UPDATE pairings SET status='revoked' WHERE id=?").bind(p.id).run();
        return json({ revoked:true });
      }

      if (request.method === "GET" && path === "/v1/diagnostics") {
        const p = await authPairing(env,request,"assistant");
        if (!p) return unauthorized();
        const limit = Math.min(100,Math.max(1,Number(url.searchParams.get("limit") ?? 30) || 30));
        const rows = await env.DB.prepare("SELECT id,kind,payload,created_at FROM diagnostics WHERE pairing_id=? AND kind<>'screen' ORDER BY created_at DESC LIMIT ?")
          .bind(p.id,limit).all<{id:number;kind:string;payload:string;created_at:number}>();
        return json({ pairingId:p.id, device:p.device_label, items:rows.results.map(row=>({ id:row.id,kind:row.kind,createdAt:new Date(row.created_at).toISOString(),payload:JSON.parse(row.payload) })) });
      }

      if (request.method === "GET" && path === "/v1/diagnostics/screen") {
        const p = await authPairing(env,request,"assistant");
        if (!p) return unauthorized();
        const row = await env.DB.prepare("SELECT payload,created_at FROM diagnostics WHERE pairing_id=? AND kind='screen' ORDER BY created_at DESC LIMIT 1")
          .bind(p.id).first<{payload:string;created_at:number}>();
        if (!row) return json({ error:"No screen frame has been received yet" },404);
        const payload = JSON.parse(row.payload) as {mimeType?:string;encoding?:string;frame?:string};
        if (payload.mimeType !== "image/jpeg" || payload.encoding !== "base64" || !payload.frame) return json({ error:"Latest screen frame has an unsupported format" },422);
        const binary = Uint8Array.from(atob(payload.frame), c => c.charCodeAt(0));
        return new Response(binary,{headers:{"content-type":"image/jpeg","cache-control":"no-store","x-captured-at":String(row.created_at)}});
      }

      if (request.method === "POST" && path === "/v1/dev/projects") {
        const p = await authPairing(env,request,"assistant");
        if (!p) return unauthorized();
        const input = await bodyJson(request);
        const name = typeof input.name === "string" ? input.name.trim() : "";
        const projectType = typeof input.projectType === "string" ? input.projectType.trim() : "";
        const brief = typeof input.brief === "string" ? input.brief.trim() : "";
        if (!name || name.length > 120 || !projectType || projectType.length > 40 || !brief || brief.length > 12000) {
          return json({ error:"name (1-120), projectType (1-40), and brief (1-12000) are required" },400);
        }
        const id = uuid(), now = Date.now();
        await env.DB.prepare("INSERT INTO dev_projects (id,pairing_id,name,project_type,brief,status,progress,current_stage,summary,created_at,updated_at) VALUES (?,?,?,?,?,'planned',0,'Requirements review','Project request recorded; no build has started.',?,?)")
          .bind(id,p.id,name,projectType,brief,now,now).run();
        await env.DB.prepare("INSERT INTO dev_project_events (project_id,event_type,message,details,created_at) VALUES (?,?,?,?,?)")
          .bind(id,"created","Project request recorded. Build execution has not started.",JSON.stringify({source:"assistant"}),now).run();
        return json({ projectId:id,name,projectType,status:"planned",progress:0,currentStage:"Requirements review",buildStarted:false,createdAt:new Date(now).toISOString() },201);
      }

      if (request.method === "GET" && path === "/v1/dev/projects") {
        const p = await authPairing(env,request,"assistant");
        if (!p) return unauthorized();
        const rows = await env.DB.prepare("SELECT id,name,project_type,status,progress,current_stage,summary,created_at,updated_at FROM dev_projects WHERE pairing_id=? ORDER BY updated_at DESC LIMIT 50")
          .bind(p.id).all<any>();
        return json({projects:rows.results.map(row=>({projectId:row.id,name:row.name,projectType:row.project_type,status:row.status,progress:row.progress,currentStage:row.current_stage,summary:row.summary,createdAt:new Date(row.created_at).toISOString(),updatedAt:new Date(row.updated_at).toISOString()}))});
      }

      const projectMatch = path.match(/^\/v1\/dev\/projects\/([0-9a-f-]+)$/i);
      if (request.method === "GET" && projectMatch) {
        const p = await authPairing(env,request,"assistant");
        if (!p) return unauthorized();
        const row = await env.DB.prepare("SELECT * FROM dev_projects WHERE id=? AND pairing_id=?").bind(projectMatch[1],p.id).first<any>();
        if (!row) return json({error:"Project not found"},404);
        const events = await env.DB.prepare("SELECT event_type,message,details,created_at FROM dev_project_events WHERE project_id=? ORDER BY created_at DESC LIMIT 100").bind(row.id).all<any>();
        return json({project:{projectId:row.id,name:row.name,projectType:row.project_type,brief:row.brief,status:row.status,progress:row.progress,currentStage:row.current_stage,summary:row.summary,createdAt:new Date(row.created_at).toISOString(),updatedAt:new Date(row.updated_at).toISOString()},events:events.results.map(e=>({type:e.event_type,message:e.message,details:JSON.parse(e.details),createdAt:new Date(e.created_at).toISOString()}))});
      }

      const projectUpdate = path.match(/^\/v1\/dev\/projects\/([0-9a-f-]+)$/i);
      if (request.method === "PATCH" && projectUpdate) {
        const p = await authPairing(env,request,"assistant");
        if (!p) return unauthorized();
        const row = await env.DB.prepare("SELECT id FROM dev_projects WHERE id=? AND pairing_id=?").bind(projectUpdate[1],p.id).first<{id:string}>();
        if (!row) return json({error:"Project not found"},404);
        const input = await bodyJson(request);
        const statuses = ["planned","analyzing","building","testing","blocked","completed","failed"];
        if (input.status !== undefined && !statuses.includes(input.status)) return json({error:"Invalid project status"},400);
        if (input.progress !== undefined && (!Number.isInteger(input.progress) || input.progress < 0 || input.progress > 100)) return json({error:"Progress must be an integer from 0 to 100"},400);
        if (input.currentStage !== undefined && (typeof input.currentStage !== "string" || input.currentStage.length > 120)) return json({error:"Invalid currentStage"},400);
        if (input.summary !== undefined && (typeof input.summary !== "string" || input.summary.length > 2000)) return json({error:"Invalid summary"},400);
        if (typeof input.message !== "string" || !input.message.trim() || input.message.length > 2000) return json({error:"A non-empty event message is required"},400);
        if (input.status === "completed" && input.verified !== true) return json({error:"Completion requires verified=true and a recorded verification event"},409);
        const now = Date.now();
        await env.DB.prepare("UPDATE dev_projects SET status=COALESCE(?,status),progress=COALESCE(?,progress),current_stage=COALESCE(?,current_stage),summary=COALESCE(?,summary),updated_at=? WHERE id=? AND pairing_id=?")
          .bind(input.status ?? null,input.progress ?? null,input.currentStage ?? null,input.summary ?? null,now,row.id,p.id).run();
        await env.DB.prepare("INSERT INTO dev_project_events (project_id,event_type,message,details,created_at) VALUES (?,?,?,?,?)")
          .bind(row.id,"status_update",input.message.trim(),JSON.stringify({status:input.status ?? null,progress:input.progress ?? null,verified:input.verified === true}),now).run();
        return json({updated:true,projectId:row.id,updatedAt:new Date(now).toISOString()});
      }

      const revoke = path.match(/^\/v1\/pairings\/([^/]+)\/revoke$/);
      if (request.method === "POST" && revoke) {
        const p = await authPairing(env,request,"assistant");
        if (!p) return unauthorized();
        if (revoke[1] !== p.id) return json({ error:"Pairing not found" },404);
        await env.DB.prepare("UPDATE pairings SET status='revoked' WHERE id=?").bind(p.id).run();
        return json({ revoked:true });
      }
      return json({ error:"Not found" },404);
    } catch (error) {
      const message = error instanceof Error ? error.message : "Unexpected relay error";
      return json({ error: message === "Request body too large" ? message : "Relay request failed" }, message === "Request body too large" ? 413 : 500);
    }
  }
};
