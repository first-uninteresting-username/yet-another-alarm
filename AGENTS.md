# yet-another-alarm

Android alarm clock: Kotlin, Jetpack Compose, Material 3 Expressive with dynamic (Material You) color. Snoozing requires answering an Open Trivia Database question.

## Scope

Build only what the spec covers; propose anything else to the maintainer before writing it. Spec: multiple alarms (time, repeat days, sound, per-alarm snooze length), user-picked sound files only (nothing bundled), trivia-gated snooze, stop behind a confirmation banner. The 5-second OpenTDB rate limit is the intended wait between questions after a wrong answer.

Dependencies stay FOSS: AndroidX, Compose, kotlinx only. Network goes through `HttpURLConnection`.

## Environment

Run everything inside devenv: `devenv shell -- ./gradlew assembleDebug lintDebug`. The SDK comes from the `android-nixpkgs` input because Compose needs compileSdk 37.2, which nixpkgs `androidenv` lacks. `buildToolsVersion` in `app/build.gradle.kts` must match `android.buildTools.version` in `devenv.nix`; the Nix SDK is read-only, so AGP cannot fetch a missing version.

A change is done when `assembleDebug` and `lintDebug` both pass with no new warnings.

## Alarm reliability invariants

- Alarm data lives in device-protected storage, and the receivers, service and `RingActivity` are `directBootAware`, so alarms ring after a reboot before unlock.
- `AlarmScheduler.sync` is the single path to `AlarmManager`; call it after every store change.
- `AlarmService` sets `ringing` before `startForeground`: `RingActivity` closes itself when `ringing` is null.
- A missing or unreadable sound falls back to vibration.
- After 3 failed OpenTDB fetches, snoozing without a question becomes available.

## Releases

Bump `versionCode` and `versionName`, then push tag `v<versionName>`. `.github/workflows/release.yml` signs with the `RELEASE_*` repository secrets and attaches the APK to a GitHub release, which Obtainium tracks.
