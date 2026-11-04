# NEXCRAFT Main Connector Relay

This folder contains the relay service and ChatGPT GPT Action schema for the NEXCRAFT Android launcher's Main Connector.

## Current implementation

- Five-minute single-use pairing codes with rate-limited creation/completion.
- Separate device and assistant credentials. The assistant token is returned once; the device token is encrypted at rest in SQLite and delivered to the paired app after approval.
- Android stores its device token encrypted using Android Keystore.
- Authenticated diagnostics ingestion and assistant-side diagnostics retrieval.
- User-approved Android MediaProjection foreground service. While active, it uploads sampled JPEG screen frames and bounded app diagnostics.
- Assistant-side latest-screen endpoint and token revocation.
- WebSocket event stream for assistant-side clients.

This is sampled screen sharing, not a continuous high-frame-rate video call. Android system permission is required, and the user can stop capture from the app or persistent notification.

## Important hosting status

**No paid hosting is configured or deployed.** The user declined recurring hosting charges. Paid Render deployment templates have been removed. Do not deploy this SQLite relay on an ephemeral free filesystem: pairing records and credentials would be lost on restart/redeploy.

A separate Cloudflare Workers + D1 free-tier adapter is now in `cloudflare/`. It avoids the ephemeral-filesystem problem, but it still needs the account owner to create the D1 database, set a secret, deploy, and test it. There is no live relay URL yet. Free-tier daily quotas apply.

## API endpoints

- `GET /health`
- `POST /v1/pairings`
- `POST /v1/pairings/complete`
- `GET /v1/pairings/status`
- `POST /v1/pairings/:id/device-token/ack`
- `POST /v1/diagnostics`
- `GET /v1/diagnostics`
- `GET /v1/diagnostics/screen`
- `POST /v1/device/disconnect`
- `POST /v1/pairings/:id/revoke`
- `WS /v1/stream`

## Security and privacy

- Android relay URLs must use HTTPS.
- Set `PAIRING_SIGNING_SECRET` to at least 32 random bytes in hex.
- Pairing codes expire after five minutes and can be used once.
- Device and assistant credentials are different; only hashes are stored for authentication. Device token handoff is encrypted at rest.
- CORS denies browser origins unless explicitly listed in `CORS_ORIGINS`.
- Screen frames are uploaded only while user-approved capture is active and the device is paired.
- Log/crash excerpts are bounded and common credential patterns are redacted. Logs can still contain personal information; sharing must remain opt-in.
- Never put ChatGPT cookies, account passwords, or personal API keys in the relay or APK.

## ChatGPT GPT Action setup (once a persistent HTTPS host exists)

For the no-paid-hosting Cloudflare setup, follow `cloudflare/README.md` first.

1. Set the server URL in `openapi.yaml` to the real relay URL and import it into a ChatGPT GPT Action.
2. In NEXCRAFT → ChatGPT Connector, enter the relay URL and create a pairing.
3. Call `pairNexcraftDevice` with the one-time code shown in NEXCRAFT.
4. Reuse the returned `assistantToken` in the `X-Assistant-Token` header for diagnostics, screen, and revoke actions.
5. Return to NEXCRAFT and tap Check Pairing.
6. Use diagnostics/screen actions only while the user has approved sharing.

The Action does not silently attach the device to an arbitrary existing ChatGPT conversation; it must be configured separately. Treat the assistant token as a secret.

## Local development

1. Copy `.env.example` to `.env` and set a random signing secret.
2. Install Node.js 22+ and run `npm install`.
3. Run `npm run dev`; production uses `npm run build` then `npm start`.

## Verification status

The source has not yet been built or run on a device. The relay is not deployed and end-to-end GPT Action pairing has not been tested. Do not treat this as production-ready.
