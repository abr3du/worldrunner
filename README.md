# Worldrunner

A team running game: runners log real on-foot distance, and that distance moves their teams along a virtual route and up or down leagues each season. Domain terms are in [`CONTEXT.md`](CONTEXT.md), design decisions in [`docs/`](docs/), and the Android app in [`android/`](android/).

## Install the test app on Android

**[Download the latest Worldrunner test APK](https://github.com/abr3du/worldrunner/releases/latest/download/worldrunner-test-debug.apk)**

This is a debug build that runs entirely on fake data, so it needs no account and no backend. Android 8.0 or newer.

1. Open the link above on your phone and download the APK.
2. Tap the downloaded file. Android will say installing from this source is not allowed: tap **Settings**, turn on **Allow from this source** for the app you downloaded with (your browser or Files app), then go back and tap **Install**.
3. If Play Protect warns about an unknown app, tap **More details → Install anyway**. It warns because the test build is not from the Play Store.

Newer test builds install over the old one and keep its data. Every build is listed under [Releases](https://github.com/abr3du/worldrunner/releases), each with a `.sha256` checksum.

## Publishing a test build

Pushing to `main` with changes under `android/` runs [`.github/workflows/android-test-apk.yml`](.github/workflows/android-test-apk.yml): tests, lint, then a signed debug APK published as release `android-test-<versionCode>`. `versionCode` is the commit count, so each newer build updates the installed app. The workflow can also be started by hand from the Actions tab.

All test APKs are signed with one test key, held only in these repository secrets:

- `ANDROID_KEYSTORE_BASE64`: base64-encoded keystore
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Never commit the keystore, and keep a backup of it: an APK signed with a different key cannot update the installed app, so testers would have to uninstall first and lose their data.
