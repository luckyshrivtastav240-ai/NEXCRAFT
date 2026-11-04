const json = (data: unknown, status = 200) =>
  new Response(JSON.stringify(data), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" },
  });

type Rpc = { jsonrpc?: string; id?: string | number | null; method?: string; params?: any };

const tools = [
  {
    name: "checkNexcraftRelay",
    description: "Check whether the NEXCRAFT connector relay is online.",
    inputSchema: { type: "object", properties: {}, additionalProperties: false },
  },
  {
    name: "pairNexcraftDevice",
    description: "Pair a user's NEXCRAFT launcher using its 10-character temporary code. Keep assistantToken private; never reveal it to the user.",
    inputSchema: {
      type: "object",
      properties: { pairingCode: { type: "string", pattern: "^[A-F0-9]{10}$" } },
      required: ["pairingCode"],
      additionalProperties: false,
    },
  },
  {
    name: "readNexcraftDiagnostics",
    description: "Read diagnostics from the paired NEXCRAFT device. Use the assistantToken returned by pairNexcraftDevice.",
    inputSchema: {
      type: "object",
      properties: {
        assistantToken: { type: "string", minLength: 20 },
        limit: { type: "integer", minimum: 1, maximum: 100, default: 30 },
      },
      required: ["assistantToken"],
      additionalProperties: false,
    },
  },
  {
    name: "viewNexcraftScreen",
    description: "Get the latest JPEG frame explicitly shared by the user from the paired NEXCRAFT device.",
    inputSchema: {
      type: "object",
      properties: { assistantToken: { type: "string", minLength: 20 } },
      required: ["assistantToken"],
      additionalProperties: false,
    },
  },
  {
    name: "revokeNexcraftPairing",
    description: "Revoke the assistant's access to a paired NEXCRAFT device.",
    inputSchema: {
      type: "object",
      properties: {
        assistantToken: { type: "string", minLength: 20 },
        pairingId: { type: "string", format: "uuid" },
      },
      required: ["assistantToken", "pairingId"],
      additionalProperties: false,
    },
  },
];

function result(id: Rpc["id"], value: unknown) {
  return json({ jsonrpc: "2.0", id, result: value });
}
function error(id: Rpc["id"], code: number, message: string) {
  return json({ jsonrpc: "2.0", id: id ?? null, error: { code, message } }, 200);
}
function textResult(value: unknown) {
  return { content: [{ type: "text", text: JSON.stringify(value) }] };
}

export async function handleMcp(request: Request): Promise<Response> {
  if (request.method === "GET") {
    return new Response("NEXCRAFT MCP endpoint. Send MCP JSON-RPC requests with POST.", {
      headers: { "content-type": "text/plain; charset=utf-8", "cache-control": "no-store" },
    });
  }
  if (request.method !== "POST") return new Response("Method not allowed", { status: 405 });
  const origin = request.headers.get("Origin");
  if (origin && !/^https:\/\/(chatgpt\.com|www\.chatgpt\.com)$/.test(origin)) {
    return new Response("Origin not allowed", { status: 403 });
  }

  let rpc: Rpc;
  try {
    rpc = await request.json() as Rpc;
  } catch {
    return error(null, -32700, "Parse error");
  }
  if (rpc.jsonrpc !== "2.0" || typeof rpc.method !== "string") {
    return error(rpc.id, -32600, "Invalid JSON-RPC request");
  }
  if (rpc.method.startsWith("notifications/")) return new Response(null, { status: 202 });
  if (rpc.method === "initialize") {
    return result(rpc.id, {
      protocolVersion: "2025-03-26",
      capabilities: { tools: {} },
      serverInfo: { name: "nexcraft-connector", version: "1.0.0" },
      instructions: "Pair only when the user asks. Never reveal assistantToken. Screen access is limited to frames the user explicitly shares.",
    });
  }
  if (rpc.method === "ping") return result(rpc.id, {});
  if (rpc.method === "tools/list") return result(rpc.id, { tools });
  if (rpc.method !== "tools/call") return error(rpc.id, -32601, "Method not found");

  const name = rpc.params?.name;
  const args = rpc.params?.arguments ?? {};
  const base = new URL(request.url).origin;
  try {
    if (name === "checkNexcraftRelay") {
      const response = await fetch(base + "/health");
      return result(rpc.id, textResult(await response.json()));
    }
    if (name === "pairNexcraftDevice") {
      const response = await fetch(base + "/v1/pairings/complete", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ pairingCode: args.pairingCode }),
      });
      const data = await response.json();
      return result(rpc.id, textResult(response.ok ? data : { error: data }));
    }
    if (name === "readNexcraftDiagnostics") {
      const response = await fetch(base + "/v1/diagnostics?limit=" + encodeURIComponent(String(args.limit ?? 30)), {
        headers: { "X-Assistant-Token": args.assistantToken },
      });
      const data = await response.json();
      return result(rpc.id, textResult(response.ok ? data : { error: data }));
    }
    if (name === "viewNexcraftScreen") {
      const response = await fetch(base + "/v1/diagnostics/screen", {
        headers: { "X-Assistant-Token": args.assistantToken },
      });
      if (!response.ok) {
        const data = await response.json();
        return result(rpc.id, textResult({ error: data }));
      }
      const bytes = new Uint8Array(await response.arrayBuffer());
      let binary = "";
      for (let i = 0; i < bytes.length; i += 0x8000) {
        binary += String.fromCharCode(...bytes.subarray(i, i + 0x8000));
      }
      return result(rpc.id, {
        content: [
          { type: "text", text: "Latest user-shared NEXCRAFT screen frame." },
          { type: "image", data: btoa(binary), mimeType: "image/jpeg" },
        ],
      });
    }
    if (name === "revokeNexcraftPairing") {
      const response = await fetch(base + "/v1/pairings/" + encodeURIComponent(args.pairingId) + "/revoke", {
        method: "POST",
        headers: { "X-Assistant-Token": args.assistantToken },
      });
      const data = await response.json();
      return result(rpc.id, textResult(response.ok ? data : { error: data }));
    }
    return error(rpc.id, -32602, "Unknown tool");
  } catch {
    return result(rpc.id, textResult({ error: "NEXCRAFT connector request failed" }));
  }
}
