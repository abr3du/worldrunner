---
status: accepted
---

# The test app shows a snapshot of real kmspiel league standings

For a prototype built in personal contact with the kmspiel owner, the test app shows one real kmspiel league, the 4. Liga that LG Albatros Kiel plays in. This narrows [ADR 0001](./0001-independent-successor.md) for the prototype only. Worldrunner still has its own backend, Accounts, and game rules, and it still never uses kmspiel accounts or interfaces.

## What is imported

- Only team-level fields: rank, team name, season distance, and number of runners. No runner names, profiles, or per-runner distances.
- The data is a snapshot bundled with the app, in `android/core/data/src/main/resources/kmspiel/liga-4.tsv`. The app records and shows the source URL, the date the source page showed, and when the snapshot was read.
- The snapshot is shown as its own "kmspiel league" in Standings. The Runner's own Runs never change it, so it always matches the source.

## How it is refreshed

kmspiel league pages are only readable when signed in: logged-out visitors get a human check. We do not automate sign-in or get around the check, and no cookies or credentials go into the repository or CI. A person who is signed in copies the page text and runs `tools/kmspiel_standings.py`, which writes a new snapshot; the usual push to `main` then publishes a new test build.

## Consequences

- The test APK is public, so these team names and distances are public too. If the owner's agreement changes, remove the snapshot and this import path.
- Distances are copied verbatim in German notation ("4.138" is 4,138 km) and parsed by the app, which rejects anything ambiguous rather than guessing.
- kmspiel's promotion and relegation rules are not in the snapshot, so the imported league shows no zones.
