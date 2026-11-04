# NEXCRAFT Main Connector Relay

This folder contains the relay service and ChatGPT GPT Action schema for the NEXCRAFT Android launcher's Main Connector.

## What is implemented in this branch

- Five-minute, single-use pairing codes with rate-limited creation/completion.
- Separate device and assistant bearer credentials. The assistant token is returned once; device token is encrypted at rest in SQLite and delivered to the paired app after approval.
- HTTPS-only Android client configuration, with device token encrypted using Android Keystore.
- Authenticated diagnostics ingestion and assistant-side diagnostics retrieval.
- User-approved Android MediaProjection foreground service. While active, it uploads a JPEG screen snapshot about once per second and redacted status/log/crash diagnostics about every 30 seconds.
- Assistant-side latest-screen endpoint returning JPEG, plus token revocation.
- WebSocket event stream endpoint for assistant-side clients.
- Persistent SQLite storage and Render/Docker deployment configuration.

This is sampled screen sharing, not a continuous high-frame-rate video call. The user must grant Android's system screen-capture permission and can stop capture from the app or the persistent notification.

## API endpoints

- `GET /health` — health check.
- `POST /v1/pairings` — create a five-minute pairing request.
- `POST /v1/pairings/complete` — consume the pairing code and issue a one-time assistant token.
- `GET /v1/pairings/status` — device-side pairing status and one-time device-token delivery.
- `POST /v1/diagnostics` — authenticated device diagnostics/screen-frame upload.
- `GET /v1/diagnostics` — authenticated assistant diagnostics read.
- `GET /v1/diagnostics/screen` — authenticated latest JPEG frame.
- `POST /v1/device/disconnect` — revoke from the device.
- `POST /v1/pairings/:id/revoke` — revoke from the assistant.
- `WS /v1/stream` — assistant event stream; first frame must be `{"token":"..."}`.

## Security

- Deploy behind HTTPS only.
- Generate `PAIRING_SIGNING_SECRET` as at least 32 random bytes in hex.
- Pairing codes expire after five minutes and can be used once; pairing endpoints are rate-limited.
- Device and assistant credentials are different. The database stores keyed hashes; the device token's one-time handoff is AES-GCM encrypted at rest.
- Android stores its device token encrypted with a non-exportable Android Keystore key.
- CORS denies browser origins unless explicitly listed in `CORS_ORIGINS`.
- Screen frames are uploaded only while the user-approved capture service is active and the device is paired.
- Logs/crash excerpts are bounded and redact common token/password patterns before upload.
- Never put ChatGPT cookies, account passwords, or personal API keys in the relay or APK.

## Deploy

1. Push the branch to GitHub.
2. In Render, create a Blueprint from the repository's root `render.yaml`. It points to `connector-relay/` and provisions a persistent disk for SQLite.
3. Wait for the Render service to become healthy and copy its HTTPS URL.
4. In NEXCRAFT → ChatGPT Connector, enter that HTTPS URL and tap Create Pairing.
5. Import `connector-relay/openapi.yaml` into a ChatGPT GPT Action. Replace `https://YOUR-DEPLOYED-RELAY-HOST` with the real relay URL.
6. Call `pairNexcraftDevice` with the code shown in NEXCRAFT. Copy the returned one-time `assistantToken` and configure the GPT Action's API-key authentication as Bearer.
7. Return to NEXCRAFT and tap Check Pairing. The Android app receives and stores its separate device token.
8. Use `readNexcraftDiagnostics` and `viewNexcraftScreen` in the GPT Action. Call the screen action again to refresh the latest frame.

The OpenAPI file configures a GPT Action. It does not silently attach the device to an arbitrary existing ChatGPT conversation; the Action must be configured in ChatGPT after deployment.

## Local development

1. Copy `.env.example` to `.env` and set a random signing secret.
2. Install Node.js 22+ and run `npm install`.
3. Run `npm run dev`; production uses `npm run build` then `npm start`.

## Verification status

Source code has been added to the NEXCRAFT branch. The relay has not yet been deployed, the Android app has not yet been built or run on a device, and the full end-to-end ChatGPT Action pairing has not yet been tested. Do not treat this as production-ready until those checks pass.
