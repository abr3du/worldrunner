# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Worldrunner is a team running game: Runners log on-foot distance, which moves their Teams along a virtual Route and up or down Leagues each Season. The Android app lives in `android/`; there is no backend yet (planned: Cloudflare Workers + D1, ADR 0002).

## Commands

Run Gradle from `android/` (the repo root has no wrapper). Requires JDK 21 (`android/mise.toml`) and Android SDK platform 37 via `android/local.properties` (`sdk.dir=...`) or `ANDROID_HOME`. On Windows, use `./gradlew` from Git Bash or `.\gradlew.bat` from PowerShell.

```sh
./gradlew :core:model:test testDebugUnitTest   # all tests, same as CI
./gradlew lint                                 # Android lint, same as CI
./gradlew assembleDebug                        # app/build/outputs/apk/debug/app-debug.apk

# Single test class / method
./gradlew :core:model:test --tests "com.worldrunner.core.model.DistanceTest"
./gradlew :feature:home:testDebugUnitTest --tests "com.worldrunner.feature.home.LogRunJourneyTest.someMethod"
```

`:core:model` is a pure Kotlin/JVM module, so its task is `test`, not `testDebugUnitTest`. All other modules are Android modules.

All tests are JVM tests: Compose UI tests run on Robolectric (no emulator needed). `StandingsMapScreenshotTest` renders real pixels (hardware render mode set in `feature/standings/build.gradle.kts`) and writes to `feature/standings/build/outputs/screenshots/`.

## Architecture

Modules (`android/settings.gradle.kts`), using type-safe project accessors (`projects.core.data`) and the version catalog `android/gradle/libs.versions.toml`:

- `:app`: single activity, `WorldrunnerApp` owns the `NavHost` and the bottom bar of four top-level tabs (Home, Teams, Standings, Profile).
- `:feature:*`: each feature exposes a `@Serializable` route object and a `NavGraphBuilder.xxxScreen(...)` extension in `*Navigation.kt`; `:app` wires them together and passes navigation callbacks (for example `onTeamClick(teamId)`). Navigation passes IDs, never objects. Screens use Hilt `ViewModel`s exposing `StateFlow` UI state.
- `:core:data`: repository interfaces in `Repositories.kt`. `di/DataModule.kt` binds them to `Fake*Repository` implementations backed by `FakeGameStore`, an in-memory stand-in for the server that applies the server's rules (Run validation, simulated Pending to Confirmed / NeedsAttention sync after a delay). Real implementations must keep the same interfaces. `health/HealthConnectRunSource.kt` is the one real data source: it reads Runs that other apps recorded from Health Connect for the Home import button (ADR 0004).
- `:core:model`: immutable domain models and rules (distance, Week/Season calendar, Run validation, Route).
- `:core:designsystem`: theme and shared UI such as status labels.

Key rules from `docs/architecture-decisions.md`:
- Distance is an integer number of metres (`Distance`), converted to km/mi only for display.
- The server is authoritative for Weeks, Week close, totals, and Standings. Device time never decides eligibility.
- Run writes show explicit Pending / Confirmed / Needs attention states, never silent success.
- Status is never shown by colour alone (accessibility is part of done). User-visible strings go in `strings.xml`.

Standings shows a bundled snapshot of a real kmspiel league (ADR 0003): `core/data/src/main/resources/kmspiel/liga-4.tsv`, parsed by `StandingsSnapshot`. Regenerate it with `tools/kmspiel_standings.py` (instructions in its docstring). The world map outlines in `feature/standings/src/main/res/raw/world_land.txt` come from `tools/world_land.py`.

## Domain language

`CONTEXT.md` is the glossary. Use its terms exactly (Runner, Run, Week close, Membership, Standing, and so on) in code, UI text, and docs, and avoid the listed alternatives (for example no "User", "Activity", "Workout", "Division"). ADRs in `docs/adr/` override `docs/architecture-decisions.md` where they disagree.

## CI and releases

- PRs touching `android/` run `.github/workflows/android-checks.yml`: tests, lint, `assembleDebug`.
- Pushes to `main` touching `android/` run `android-test-apk.yml`, which publishes a signed debug APK as GitHub release `android-test-<versionCode>`.
- `versionCode` is the git commit count (`app/build.gradle.kts`), so builds need a git checkout.
- Test-key signing comes only from the `ANDROID_KEYSTORE_*` / `ANDROID_KEY_*` environment variables. Never commit a keystore.

Commit messages are one short, capitalised, imperative sentence without a type prefix (for example "Add an adaptive launcher icon").
