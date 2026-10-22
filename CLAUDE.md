# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Café Lucky Wheel: an offline, native Android tablet app (Kotlin + Jetpack Compose, Material 3). Customers see a full-screen prize wheel. The owner configures prizes and themes in a hidden Settings area protected by a PIN.

**The spec is [docs/PRD.md](docs/PRD.md). Read it before implementing anything.** Requirement IDs such as `SP-04` and `PR-04` refer to it. `docs/Cafe Lucky Wheel (standalone).html` is a visual prototype (a self-contained bundled React page; open it in a browser). Use it as a design reference only. It is not app code.

Current state: `android/` is still the Android Studio template (`MainActivity` with a `Greeting`, plus the default theme files). No PRD features exist yet.

## Commands

The Gradle root is `android/`, not the repo root. Run every command from there.

```sh
cd android
./gradlew assembleDebug                 # build
./gradlew installDebug                  # install on a connected device or emulator
./gradlew testDebugUnitTest             # JVM unit tests (domain logic lives here)
./gradlew testDebugUnitTest --tests "io.github.alanrb.luckywheel.SomeTest"           # one class
./gradlew testDebugUnitTest --tests "io.github.alanrb.luckywheel.SomeTest.someCase"  # one method
./gradlew connectedDebugAndroidTest     # instrumented and Compose UI tests (needs a device)
./gradlew lint
```

## Build setup notes

- AGP 9.x with built-in Kotlin support. Only `org.jetbrains.kotlin.plugin.compose` is applied. Do not add `org.jetbrains.kotlin.android`.
- Dependencies and plugins go through the version catalog at `android/gradle/libs.versions.toml`.
- The Gradle daemon runs on a JDK 25 toolchain (`gradle/gradle-daemon-jvm.properties`), which the foojay resolver auto-provisions. The configuration cache is enabled.
- R8 keep rules go in `app/src/main/keepRules/` (AGP 9 layout), not `proguard-rules.pro`. Release optimization is currently disabled.
- `compileSdk`/`targetSdk` are 37 and `minSdk` is 33 (Android 13), so APIs up to 33 can be used without version checks.
- Package/namespace: `io.github.alanrb.luckywheel`.

## Architecture (target, per PRD §10)

- Single activity, Compose Navigation, MVVM.
- A **pure Kotlin `domain` package with no Android imports** holds: effective-probability calculation, the weighted draw (with an injectable random source; production uses `SecureRandom`), target-angle calculation, and validation rules. This is the core logic and must be fully unit-tested on the JVM.
- Probabilities are stored and computed as **integer basis points** (10,000 = 100%). Never use floating point for weights.
- When a prize's quantity reaches 0, its configured bp moves to the single fallback prize ("Chúc may mắn lần sau"), so the total always stays at 10,000.
- Persistence: Room for `Prize` (schema in PRD §7.1), DataStore Preferences for settings (keys in §7.2). Images are copied into app-private storage.
- Spin flow: the outcome is drawn **when the spin starts** and held in the ViewModel and `SavedStateHandle`. The animation (`Animatable` rotation, flick via `pointerInput` + `VelocityTracker`) only visualizes that outcome. Quantity is decremented **on reveal**, in one Room transaction that guards against going below 0. If the process dies mid-spin, the spin is discarded.
- Wheel, decorations and win effects are drawn with Compose `Canvas` and must hold 60 fps. Sound uses `SoundPool`. Image loading uses Coil. Images are picked with the Photo Picker, so no storage permission is needed.

## Hard constraints

- When a requirement is ambiguous, keep it simple and preserve these, in priority order:
  1. never award a prize beyond its quantity;
  2. never let animation or touch input influence the outcome;
  3. never expose Settings without the PIN.
- No `INTERNET` permission, analytics, or tracking SDKs.
- All UI text is Vietnamese and lives in string resources (no hardcoded strings), so English can be added later.
- Theme assets must be drawn in code or be CC0/royalty-free.

## Workflow

- Build milestone by milestone (PRD §13). Do not start UI work until the M1 domain and statistical tests pass.
- Put requirement IDs in commit messages and test names (e.g. `SP-04`).
- Do not add a `Co-Authored-By: Claude` trailer or any other Claude attribution to commit messages.
