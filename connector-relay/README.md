# NEXCRAFT Main Connector Relay (scaffold)

This service is the secure relay component for NEXCRAFT's Main Connector. It is intentionally separate from the Android launcher.

## Current endpoints

- `GET /health` — health check.
- `POST /v1/pairings` — create a five-minute pairing request.
- `POST /v1/pairings/complete` — consume a pairing code and issue a one-time ChatGPT-side bearer token.
- `GET /v1/pairings/status` — device-side pairing status.
- `POST /v1/diagnostics` — authenticated device diagnostics ingestion.
- `GET /v1/diagnostics` — authenticated assistant diagnostics read.
- `POST /v1/pairings/:id/revoke` — revoke the assistant pairing.
- `WS /v1/stream` — authenticated assistant event stream; first frame must be `{"token":"..."}`.

## Security

- HTTPS is required at the deployment edge; do not expose plain HTTP publicly.
- Generate `PAIRING_SIGNING_SECRET` with `openssl rand -hex 32`.
- Pairing codes expire after five minutes and are single-use.
- Bearer tokens are stored as keyed SHA-256 hashes; assistant token is returned only once.
- Device pairing secret is separate from assistant token.
- CORS is denied for browser origins unless explicitly listed in `CORS_ORIGINS`.
- Diagnostics are size-limited; device logs must be redacted by the Android client before upload.
- No API keys, ChatGPT cookies, or user account credentials belong in this service.

## Important implementation status

This is a relay API scaffold, not yet a production-ready ChatGPT connector. The device-token handoff is deliberately not enabled until its one-time encrypted delivery is implemented. The Android MediaProjection foreground service, app-side secure pairing client, reconnect lifecycle, and ChatGPT MCP tool adapter still need to be integrated and tested before this is called complete. The SQLite store must use persistent storage in deployment.

## Deploy on Render

1. Push this branch to GitHub and create a Render Blueprint using `connector-relay/render.yaml`.
2. Confirm the persistent disk is mounted at `/var/data`; do not use an ephemeral filesystem for the SQLite database.
3. After deployment, copy the HTTPS service URL into NEXCRAFT → ChatGPT Connector.
4. Import `openapi.yaml` into a ChatGPT GPT Action and replace the example server URL with the deployed HTTPS URL.
5. Create a pairing code in NEXCRAFT, then call `pairNexcraftDevice` from the Action with that code. Copy the one-time `assistantToken` from the result and configure the Action's API-key authentication as Bearer. The Android app will obtain its separate device token after you tap Check Pairing.
6. The Action can call `readNexcraftDiagnostics` and `viewNexcraftScreen`. Revoke access from the app's Disconnect control or the Action's revoke operation.

The OpenAPI file describes a GPT Action integration. It does not automatically attach this device to an arbitrary existing ChatGPT conversation. The Action must be created/configured in ChatGPT after the relay is deployed.

## Local development

1. Copy `.env.example` to `.env` and set a random signing secret.
2. Install Node.js 22+ and run `npm install`.
3. Run `npm run dev`.

Do not run this service on an untrusted public network without HTTPS and a persistent database volume.
