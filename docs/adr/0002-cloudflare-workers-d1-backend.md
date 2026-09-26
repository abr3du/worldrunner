---
status: accepted
---

# Backend runs on Cloudflare Workers with D1

The Worldrunner backend is TypeScript on Cloudflare Workers, stores data in D1 (SQLite), and uses Cron Triggers for Week close and Season rollover. We chose this over Kotlin/Ktor with Postgres on a managed host. The main reasons are near-zero operations work and free-tier cost at MVP scale, for a backend run by one person. We accept that the backend and app no longer share a language, and that we are locked in to Cloudflare's runtime and D1's SQLite dialect and size limits.

## Consequences

- API DTOs are defined twice (TypeScript and Kotlin). Contract tests against shared JSON examples keep them in sync.
- Every Run write requires a client idempotency key. The Android outbox relies on this.
- Scheduled jobs (Week close, Season rollover, reminders) must be idempotent and safe to re-run, so a retried or manually re-run job cannot double-apply results.
