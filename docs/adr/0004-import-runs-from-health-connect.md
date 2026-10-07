---
status: accepted
---

# Runs are imported from Health Connect, not from Strava or Garmin directly

Runners can import their recorded runs with one button on Home. The app reads them from Health Connect on the phone, where Garmin Connect, Strava, Samsung Health and others already write the sessions they record. We chose this over connecting to Strava's or Garmin's own APIs for three reasons:

- **Strava's API terms rule out our core loop.** Since November 2024, third-party apps may show a user's Strava activity data only to that user. Every Run's distance counts towards Team totals and Standings that other Runners see.
- **Garmin's API is not self-serve.** The Garmin Connect Developer Program needs an approved business application.
- **No server is needed.** Strava's OAuth flow needs a client secret, which cannot ship in the app, and the backend ([ADR 0002](./0002-cloudflare-workers-d1-backend.md)) does not exist yet. Health Connect is read on the device with the Runner's permission.

## How it works

- The app asks for read-only access to exercise sessions and distance, only when the Runner taps **Import runs**. It never writes health data and asks for nothing else, such as heart rate or routes.
- Only the Weeks not yet closed are read. Running, treadmill running, walking and hiking sessions count as Runs. A session's distance is the distance recorded by the same app during the session, and its date is the local date it started.
- Each imported Run remembers its recording, so importing again never duplicates it. Recordings that overlap in time are one run recorded by two apps (for example Garmin Connect, and Strava copying it), and only the earliest-starting one is imported.
- Imported Runs go through the same Run limits and Week close checks as Runs logged by hand. Recordings that fail them are reported as skipped.

## Consequences

- An imported run and the same run logged by hand both count: hand-logged Runs have no time of day to match on. The Runner can delete the duplicate while the Week is open.
- Health Connect only shows its permission dialog to apps that explain their data use, so the app ships a short rationale screen. A Play Store release will also need Google's Health Connect permission declaration.
- Runners whose app does not write to Health Connect (or iOS users later) still log Runs by hand. A direct Strava or Garmin connection stays possible later, but only with a backend and terms that allow sharing distance with teammates.
- Imports are manual. Background sync with WorkManager can follow once the outbox (AD-4) exists.
