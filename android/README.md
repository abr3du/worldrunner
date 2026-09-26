# Worldrunner Android

First vertical slice (see `../docs/architecture-decisions.md`, section 12): app shell, Home with Log run, Team detail, League Standings, and Profile units. All data comes from fake repositories in `:core:data` until the backend exists.

## Build

Requires JDK 21 (`mise.toml` pins it) and an Android SDK with platform 37. Point Gradle at the SDK with `local.properties` (`sdk.dir=/path/to/Android/Sdk`) or `ANDROID_HOME`.

```sh
./gradlew assembleDebug                          # app/build/outputs/apk/debug/app-debug.apk
./gradlew :core:model:test testDebugUnitTest     # unit, repository, and Robolectric Compose tests
```

To sign a local build with the shared test key (so it can update an installed test APK), export the `ANDROID_KEYSTORE_FILE`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD` variables before `assembleDebug`; without them Gradle uses your machine's debug key. Published test APKs and how to install them: [`../README.md`](../README.md#install-the-test-app-on-android).

Domain terms follow `../CONTEXT.md`.
