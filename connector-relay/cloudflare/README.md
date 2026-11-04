# NEXCRAFT Connector — Cloudflare Free-tier adapter

This Worker implements the HTTP API used by the Android connector and ChatGPT GPT Action. It replaces Express/SQLite with Cloudflare Workers + D1. The GPT Action only needs HTTP endpoints; the WebSocket endpoint in the self-hosted Node relay is not included here.

## Free-tier limits to be aware of

Cloudflare currently documents Workers Free as 100,000 requests/day and 10 ms CPU time per invocation. D1 Free currently includes 5 million rows read/day, 100,000 rows written/day, and 5 GB total storage. These are quotas, not an unlimited guarantee; requests/queries can fail when daily limits are reached. Check current Cloudflare pricing/limits before deployment.

The Android screen sampler was reduced to one uploaded frame (up to 300 KB) every 5 seconds to leave quota for diagnostics and pairing. Stopping screen sharing deletes the cached local JPEG and asks the relay to delete retained screen frames. The Worker retains only the latest five frames per device and the latest 100 diagnostics per kind.

## Setup

1. Create or sign in to a Cloudflare account and open a terminal with Node.js installed.
2. From this directory run `npm install`.
3. Authorize Wrangler in your Cloudflare account: `npx wrangler login`.
4. Create the free D1 database: `npx wrangler d1 create nexcraft-connector`.
5. Copy the returned database ID into `wrangler.toml`, replacing `REPLACE_WITH_CLOUDFLARE_D1_DATABASE_ID`.
6. Create the tables: `npm run db:init`.
7. Set the relay secret: `npx wrangler secret put PAIRING_SIGNING_SECRET`. Paste at least 32 cryptographically random characters (for example, generate 32 random bytes locally and encode as hex).
8. Test locally with `npm run dev`, then publish with `npm run deploy`.
9. Copy the Worker HTTPS URL into NEXCRAFT → ChatGPT Connector and replace the server URL in the GPT Action OpenAPI file with that same URL.

## GPT Action

Import `../openapi.yaml`. Pair with `pairNexcraftDevice`, then pass the returned `assistantToken` in the `X-Assistant-Token` header for diagnostics, screen, and revoke operations. Treat that token as a password.

## Notes

- No paid Render service or paid Cloudflare plan is required for this setup.
- Cloudflare account creation, D1 database creation, secret entry, and deployment still require the account owner to sign in and run the commands.
- Screen sharing still requires Android's system MediaProjection permission and a visible foreground notification.
- This adapter has not been deployed or end-to-end tested yet. Do not treat it as production-ready until the Worker build, D1 migration, pairing, screen upload, and revoke flows have been tested.
