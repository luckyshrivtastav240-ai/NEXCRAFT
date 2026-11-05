# NEXCRAFT Free-First Engineering Policy

This policy applies to the NEXCRAFT connector and its future development workspace.

## Non-negotiable product rules

- Free-only by default. Do not add subscriptions, paid tiers, premium locks, paid API dependencies, or upsell screens.
- Never bypass provider quotas, payment checks, account limits, access controls, or terms of service.
- Never describe a service as unlimited or permanently free unless its provider explicitly guarantees that.
- Disclose quotas, sleep/cold-start behavior, storage limits, build minutes, artifact retention, and account requirements before a user depends on a provider.
- Do not silently switch providers if doing so would expose private source code, secrets, user data, or production systems. Ask first when data leaves the user's chosen boundary.
- Keep project source exportable and avoid provider-specific lock-in wherever practical.

## Resilience design

1. **Portable project source:** Git repository remains the source of truth. Every generated project must be exportable as source code, with a dependency and environment manifest.
2. **Build adapters:** Implement a provider-neutral build interface. GitHub Actions is the first adapter for repository builds; local/self-hosted runners may be added as opt-in adapters. A provider adapter must report its own quota and failure reason.
3. **Queue and retry:** Persist jobs and their states. Retry only transient failures with bounded exponential backoff and a maximum attempt count. Never retry authentication, quota, invalid configuration, or user-code failures indefinitely.
4. **Fallback routing:** Route a job to another enabled adapter only when the project requirements are compatible and the user has authorized that adapter. Preserve logs and artifact provenance across attempts.
5. **Artifacts:** Store build outputs with checksums, build metadata, expiry/retention information, and an explicit download/export path. Do not promise permanent artifact hosting on a free tier.
6. **Preview and deployment:** Separate temporary previews from production deployments. Prefer portable static exports and user-owned destinations. Production deployment, DNS changes, data migrations, and destructive actions require explicit approval.
7. **Verification gate:** A project cannot be marked complete merely because a job was queued or a provider returned success. Record actual build exit status, test results, artifact existence, and applicable install/smoke-test evidence.
8. **Observability:** Show queued/running/succeeded/failed/blocked states, timestamps, provider, quota/rate-limit responses, logs, and next safe action. Never fabricate progress.
9. **Security:** Keep provider credentials in secret stores, never in source code or logs. Use least-privilege tokens, isolate untrusted builds, and require explicit consent for screen sharing or device access.
10. **Cost guard:** Reject any workflow that requires a paid plan or paid API unless the user explicitly changes the free-only policy later.

## Honest limits

No architecture can guarantee that third-party free services will remain free, available, or unlimited forever. NEXCRAFT should reduce dependency risk through portability, user-controlled exports, optional self-hosted execution, and documented fallback adapters—not by bypassing limits.

## Current implementation status

The current connector includes device pairing/diagnostics and a project status tracker. It does **not** yet include the provider-neutral build runner, build queue, artifact store, live preview service, automatic test/fix loop, or deployment manager. Those components must be implemented and tested before they are advertised as available.
