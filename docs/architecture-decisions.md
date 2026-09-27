# Worldrunner Android App — Architecture Decisions

Status: Accepted baseline after product discovery  
Date: 2026-09-25 (revised 2026-09-26)  
Target: A modern, English-language Android app for an independent team running game inspired by [kmspiel.de](https://www.kmspiel.de/)

## 1. Purpose

This document is the readable overview of Worldrunner's technical foundation. It records what we chose and why, and what is still open.

Domain language is defined in [`CONTEXT.md`](../CONTEXT.md). Capitalised terms here (Runner, Run, Week close, Membership, and so on) have exactly the meanings given there. Hard-to-reverse decisions have their own records in [`docs/adr/`](./adr/). Where this document and an ADR disagree, the ADR wins.

Worldrunner is an independent successor to kmspiel (see [ADR 0001](./adr/0001-independent-successor.md)). It does not reuse the kmspiel name, branding, content, user data, or interfaces.

## 2. Product understanding

The core loop:

1. A Runner logs Runs: real on-foot distance, individually, each Week.
2. Each Run's full distance counts for every Team the Runner has a Membership in that Season.
3. Teams are ranked by total distance within their League, and Route progress visualises that distance.
4. At Season rollover, Promotion and Relegation move Teams between Tiers of the League pyramid.

The primary user is a regular Runner. Their most frequent job is "log this week's distance in seconds, then see what it did for my team."

The UX principle is **record progress quickly, then understand its impact**. A returning Runner can log a Run from the first screen with very little navigation.

Profiles, training diaries, races, forums, chat, and similar kmspiel features are out of scope for the MVP (see section 11).

## 3. Settled product and platform facts

- Worldrunner has its own backend, Accounts, and game data. Existing kmspiel users start fresh.
- English is the initial UI language. Strings are externalized so more locales can be added.
- Phones are the primary form factor. Tablets and foldables remain usable through adaptive layouts.
- Minimum Android version is API 26 (Android 8.0).
- Runs are entered manually in the MVP. Health Connect import is later work.
- The server is authoritative for Seasons, Weeks, Week close, Memberships, totals, Standings, and conflict resolution.
- Rule values are server configuration, not app constants. Current values: Team size cap 10; 3 Memberships per Season; Joining cutoff at the end of Week 4; Run limits of 100 km per Run and 150 km per day; 10 Teams per League; 2 promoted and 2 relegated per League.

## 4. Decision summary

Decisions in this document use `AD-n` IDs. Standalone records in `docs/adr/` use four-digit numbers.

| ID | Decision | Status |
|---|---|---|
| AD-1 | Use a supported HTTPS JSON API behind a replaceable client boundary; never scrape HTML | Superseded by [ADR 0001](./adr/0001-independent-successor.md): the API is our own |
| AD-2 | Build a native Android app in Kotlin with Jetpack Compose and Material 3 | Accepted |
| AD-3 | Use a single-activity, feature-oriented architecture with unidirectional data flow | Accepted |
| AD-4 | Make cached reads offline-capable; queue Run writes in an idempotent outbox | Accepted |
| AD-5 | Start as a small modularized app, not a module per screen | Accepted |
| AD-6 | Use four primary destinations: Home, Teams, Standings, Profile | Proposed; validate with the first vertical slice |
| AD-7 | Keep authentication tokens out of the database and logs; minimize personal data | Accepted |
| AD-8 | Treat server rules and calculations as authoritative | Accepted |
| [0001](./adr/0001-independent-successor.md) | Independent successor to kmspiel, not a client of it | Accepted |
| [0002](./adr/0002-cloudflare-workers-d1-backend.md) | Backend on Cloudflare Workers with D1 | Accepted |
| [0003](./adr/0003-kmspiel-standings-prototype.md) | Test app shows a snapshot of real kmspiel league standings | Accepted |

## 5. Architecture

### 5.1 High-level shape

```text
Compose screens
    ↓ events                         ↑ immutable UI state
ViewModels / screen state holders
    ↓
Focused use cases (only where business orchestration is needed)
    ↓
Repositories
    ├── Room database (local source of truth for cached app data)
    ├── API client (remote authority)
    └── DataStore (small preferences only)
             ↕
     Worldrunner API (Cloudflare Workers + D1)
```

The UI observes local persisted data. Repositories refresh that data from the service and reconcile queued user actions. Screens never call network or database clients directly.

### 5.2 Android stack

- **Language:** Kotlin
- **UI:** Jetpack Compose with Material 3
- **Navigation:** Navigation Compose with type-safe destinations
- **State:** `ViewModel`, coroutines, and `StateFlow`; immutable screen state and explicit UI events
- **Dependency injection:** Hilt
- **Network:** Retrofit/OkHttp with Kotlin serialization
- **Persistence:** Room for structured cache and pending operations; DataStore for small preferences
- **Background sync:** WorkManager with network constraints and retry/backoff
- **Sign-in:** Credential Manager for Google sign-in
- **Images:** Coil, if remote avatars or team imagery are included
- **Build:** Gradle Kotlin DSL and a version catalog

Library versions are deliberately omitted here. They should be selected and pinned when the Android project is scaffolded.

### 5.3 Initial module boundaries

```text
:app                 Application shell, root navigation, DI setup
:core:model          App-facing immutable models
:core:data           Repositories, local/remote sources, sync policy
:core:designsystem   Theme and genuinely shared UI components
:feature:auth        Sign in (Google, email magic link), first-run setup
:feature:home        Season summary and fast Run logging
:feature:teams       Runner's Teams, Team details, create and join by Invite code
:feature:standings   League Standings and Route progress
:feature:profile     Display name, units, reminder, account deletion, sign out
```

Start with these boundaries only. Split networking/database modules or add feature modules when build performance, ownership, or reuse provides a concrete reason.

## 6. Data and integration decisions

### AD-1: Supported API, not HTML scraping (superseded)

The original decision listed three integration paths and was blocked on backend discovery. [ADR 0001](./adr/0001-independent-successor.md) resolved it: Worldrunner owns its backend ([ADR 0002](./adr/0002-cloudflare-workers-d1-backend.md)). The principle still applies: the app depends on an explicit, versioned API contract, and scraping or reverse engineering any third-party service is ruled out.

`Fake*Repository` implementations backed by fixtures remain the starting point, so UI work does not wait on the backend. They keep the same repository interfaces used for real integration.

The discovery questions are now answered:

- **API:** Our own HTTPS JSON API. It provides stable IDs, server timestamps, and required idempotency keys on every Run write.
- **Authentication:** Google sign-in or email magic link. No passwords are stored anywhere.
- **Rules:** Seasons, Week close, Joining cutoff, Standings, and Season rollover are defined in `CONTEXT.md`. There is no playoff.
- **Unit of entry:** A Run is one individual activity. Weekly totals are always derived.

### AD-4: Offline-capable reads and queued Run writes

The app shows previously synchronized Home, Teams, and Standings data immediately when offline. Room is the source read by the UI, and network responses update Room transactionally.

Run creates, edits, and deletes use a durable outbox:

1. Validate locally (including Run limits) and create a pending operation with a client-generated idempotency key.
2. Show the Run as **Pending**, never as silently confirmed.
3. Submit immediately when online, or schedule WorkManager.
4. Replace the pending state with the server result.
5. Surface rejected entries as **Needs attention** (for example, a write that arrives after Week close) and let the Runner edit or discard them.

The backend requires idempotency keys, so a retried submission never duplicates distance.

### Core entities

- `Account`
- `Runner`
- `Season`
- `Week`
- `Run`
- `Team`
- `Membership`
- `League`
- `Tier`
- `Standing`
- `RouteProgress`
- `PendingOperation`

API DTOs, database entities, and app/domain models remain separate. Mapping at data-source boundaries keeps a changing wire format from leaking through the app.

Distance is stored as an integer number of metres, never as a floating-point kilometre value. Display converts to km or mi according to the Runner's preference. Time is stored as ISO-8601 instants plus explicit Season/Week identifiers supplied by the server. A Run's Week comes from its local date in the device timezone at logging time.

## 7. Navigation and screen responsibility

Use a bottom navigation bar for four top-level destinations:

- **Home:** the current Season and Week, the Runner's Weekly total, Team impact, sync status, and the primary **Log run** action.
- **Teams:** the Runner's Teams first. Creating a Team and joining by Invite code are secondary flows.
- **Standings:** the League Standings with Promotion and Relegation zones, plus Route progress. The viewer's own Team is highlighted.
- **Profile:** Display name, units (km/mi), the Week reminder, help, account deletion, and sign out.

`Log run` is a short modal or focused destination reachable from Home in 3 taps or fewer. It provides numeric input in the Runner's unit, the date (limited to Weeks before Week close), clear validation, and an explicit saved/pending/needs-attention outcome.

Visibility: Standings and Route progress are visible to all signed-in Runners. A Team's roster and each member's Weekly totals are visible to teammates only. Individual Runs are visible only to their owner. Only Display names are shown, never real names.

Navigation passes stable identifiers, not whole objects. Each destination reloads its state from its repository, which supports process recreation and deep links, including Invite links.

Accessibility is part of the definition of done: scalable text, meaningful semantics, adequate touch targets, screen-reader labels, non-colour status cues, and support for reduced motion. Promotion and Relegation cannot be communicated through colour alone.

## 8. Authentication, privacy, and security

- Sign in with Google (Credential Manager) or an email magic link. The backend issues short-lived access tokens and rotating refresh tokens. No passwords exist in the system.
- Store token-encryption keys with Android Keystore-backed facilities. Never store credentials or tokens in Room, DataStore plaintext, crash reports, analytics, or logs.
- Redact network logs in all builds, and disable diagnostic HTTP bodies in release builds.
- Request no location, contacts, or health permission for the manual-entry MVP.
- If Health Connect is added later, make it an explicit opt-in integration with the minimum activity permissions and a documented duplicate-detection policy.
- Account deletion is in the MVP. It deletes the Account, its Runs, and its Display name. Distance already credited in closed Weeks stays with the Team as anonymous distance, so final Standings never change retroactively.
- The only notification is an opt-in reminder on Monday evening, sent if the Runner logged no Runs in the previous Week. It is off by default, and the app offers it after the first Run.
- Keep secrets and environment configuration outside source control. Keep development, staging, and production environments separate.

## 9. Error and sync semantics

Every data screen supports distinct loading, content, stale/offline, empty, and error states. Cached content remains visible when a refresh fails.

User-visible write states are explicit:

```text
Draft → Pending sync → Confirmed
                    ↘ Needs attention (rejected, e.g. after Week close)
```

Server responses win for totals and Standings. The client may preview the expected impact of pending Runs, but it labels that preview as provisional. Week close and the Joining cutoff use server time. Device time never determines eligibility.

## 10. Testing and delivery

Minimum test strategy:

- **Unit tests:** Run limits validation, Week/Season mapping (including Weeks at a Season boundary), unit conversion, state reducers, repository reconciliation, and API/entity/model mapping.
- **Repository tests:** a fake server plus in-memory Room, covering duplicate submission, retry, writes after Week close, and stale cache scenarios.
- **Compose tests:** each critical journey below, authentication states, bottom navigation, offline/pending status, and accessibility semantics.
- **Contract tests:** shared JSON examples and error shapes, run against both the Kotlin client and the TypeScript backend.
- **Backend tests:** Week close and Season rollover jobs, including safe re-runs.

Critical journeys, each with acceptance criteria and an end-to-end test:

1. First launch: sign in with Google or magic link, then set Display name and units.
2. Log a Run in 3 taps or fewer from Home, then see it go from Pending to Confirmed.
3. Create a Team and share its Invite code; a second Runner joins through the link.
4. View League Standings and Route progress, with the viewer's own Team highlighted.
5. Log a Run offline, reconnect, and see it sync. A Run submitted after Week close shows as Needs attention.

CI should run static analysis, unit tests, and Compose tests appropriate for pull requests. Only protected release automation should build a signed artifact. Production telemetry should be privacy-conscious and limited at first to crashes, app health, and coarse sync success/failure, subject to consent and policy requirements.

## 11. Deliberately deferred decisions

- Visual identity and final branding (the name, content, and branding must be original)
- Health Connect or device imports
- Public team directory, join requests, and moderation or report flows
- Playoffs and Route checkpoints or achievements
- Social features: profiles, diaries, races, forums, and chat
- Notifications beyond the Week reminder
- Analytics and crash reporting vendor, hosting region, data retention, and legal basis
- iOS and web clients
- Monetization, if any

## 12. Suggested first implementation slice

Build a vertical slice against fake repositories:

1. App shell, English theme, and navigation.
2. A fake signed-in Runner and the current-Season Home screen.
3. The Log run flow with validation and visible Pending/Confirmed/Needs attention states.
4. One Team detail screen and one League Standings screen showing the Run's impact.
5. Unit and Compose tests for that journey.

Stop there and validate the navigation (AD-6) and terminology before adding sign-in, Invite codes, or backend integration.

## References

- [`CONTEXT.md`](../CONTEXT.md): domain glossary
- [`docs/adr/`](./adr/): architecture decision records
- [kmspiel small handbook](https://www.kmspiel.de/handbuch/) (inspiration only)
- [Android guide to app architecture](https://developer.android.com/topic/architecture)
- [Android data layer guidance](https://developer.android.com/topic/architecture/data-layer)
- [Android offline-first guidance](https://developer.android.com/topic/architecture/data-layer/offline-first)
- [Type-safe Navigation Compose](https://developer.android.com/guide/navigation/design/type-safety)
- [Android security checklist](https://developer.android.com/privacy-and-security/security-tips)
