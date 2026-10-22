# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Café Lucky Wheel: an offline, native Android tablet app (Kotlin + Jetpack Compose, Material 3). Customers see a full-screen prize wheel. The owner configures prizes and themes in a hidden Settings area protected by a PIN.

**The spec is [docs/PRD.md](docs/PRD.md).** Requirement IDs such as `SP-04` and `PR-04` refer to it.

**The visual design is `docs/Cafe Lucky Wheel (standalone).html`.** It is a bundled React prototype and it is the source of truth for visuals, Vietnamese copy, theme values, particle shapes, sound parameters and spin feel. The markup and logic are packed inside; to read them, decode the `__bundler/template` and `__bundler/manifest` script blocks (base64 + gzip) into a scratch directory.

## Commands

The Gradle root is `android/`, not the repo root. Run every command from there.

```sh
cd android
./gradlew assembleDebug                 # build
./gradlew installDebug                  # install on a connected device or emulator
./gradlew testDebugUnitTest             # all tests: JVM + Robolectric (Room, DataStore, ViewModels, Compose UI)
./gradlew testDebugUnitTest --tests "io.github.alanrb.luckywheel.domain.SpinMathTest"           # one class
./gradlew testDebugUnitTest --tests "io.github.alanrb.luckywheel.domain.SpinMathTest.button_isMiddleOfRanges"  # one method
./gradlew lintDebug
python3 ../tools/generate_sounds.py     # regenerate res/raw/*.wav (synthesized, no third-party audio)
```

All tests, including the Compose UI tests, run on the JVM under Robolectric; `src/androidTest` is empty.

## Build setup notes

- AGP 9.x with built-in Kotlin support. Only `org.jetbrains.kotlin.plugin.compose` and KSP (for Room) are applied. Do not add `org.jetbrains.kotlin.android`.
- Kotlin is 2.2. Libraries built against Kotlin 2.4 will not compile; this is why Coil was dropped and images are decoded with `ImageDecoder`/`BitmapFactory` instead. Check the transitive Kotlin stdlib version before adding a dependency.
- Dependencies and plugins go through the version catalog at `android/gradle/libs.versions.toml`.
- The Gradle daemon (and the test JVM) runs on a JDK 25 toolchain, auto-provisioned by foojay. Robolectric needs `--add-exports java.base/jdk.internal.access`, which is set in `app/build.gradle.kts` along with a 3 GB test heap.
- Robolectric runs with `sdk=36` (`app/src/test/resources/robolectric.properties`), because targetSdk 37 is newer than Robolectric supports.
- `compileSdk`/`targetSdk` are 37 and `minSdk` is 33.
- R8 keep rules go in `app/src/main/keepRules/` (AGP 9 layout). Release optimization is disabled.
- The Room schema is exported to `app/schemas/`. Bump the version and add a migration if `PrizeEntity` changes.

## Architecture

Package `io.github.alanrb.luckywheel`. Manual DI: `AppContainer` (created in `LuckyWheelApplication`) holds the repositories, `RandomSource`, `Clock`, `SoundEffects` and the ViewModel factories. Compose code reads it via `LocalAppContainer`, and tests build their own container with fakes.

- **`domain/`** is pure Kotlin with no Android imports, and fully JVM-tested. It covers:
  - effective probabilities (`effectiveWheel`);
  - the weighted draw;
  - spin planning (`planSpin`, `rotationAt`, `sliceIndexUnderPointer`, `flickStrength`);
  - validation and percent parsing/formatting (Vietnamese decimal comma);
  - slice colour assignment;
  - PIN hashing (PBKDF2) and the lockout rule.

  Weights are integer basis points (10,000 = 100%). The out-of-stock weight moves to the single fallback prize.
- **`data/`** contains:
  - `PrizeRepository` (Room). `replaceAll` saves the Settings draft; ids ≤ 0 are new rows. `claimPrize` is the guarded decrement done on reveal.
  - `SettingsRepository` and `PinManager`, which share one Preferences DataStore. The PIN lock is stored, so it survives restarts.
  - `ImageStore`, which copies picked images, downscaled, into `filesDir/images`.
- **Spin flow** is in `ui/wheel/WheelViewModel`:
  - The outcome is drawn when the spin starts. The `SpinPlan` and its start time are kept in `SavedStateHandle`.
  - The screen computes the rotation from `Clock.elapsedMs()` each frame. It does not use an `Animatable`, so the spin survives rotation, backgrounding and process recreation.
  - `onSpinAnimationFinished()` is idempotent and claims the prize. If stock ran out, it reveals the fallback instead.
- **Customer screen** (`ui/wheel`):
  - `WheelLayout` picks a stacked or split layout scaled from the 1280×800 design (`designScale`).
  - `WheelDrawing` renders with a native canvas, and `LabelFit` ports the prototype's label fitting.
  - Gestures are flick-to-spin and the hidden 80 dp top-left corner hold. The corner opens the PIN overlay in `WheelRoute`.
- **Themes and effects**: `ui/themes/ThemeSpec` holds the prototype's six themes, and `ThemeBackground` ports its CSS gradients. In `ui/effects/`, `Particles.kt` holds the pure physics and `ParticleDrawing.kt` the shapes. Frame loops run only while the lifecycle is RESUMED.
- **Settings** (`ui/settings`) is one two-pane screen with sections and in-layout overlays rather than `Dialog` windows, which would break immersive mode. Prize edits go into a draft that is saved whole and only when valid. Settings closes itself after 2 minutes idle.
- **Navigation** has three routes (`createPin`, `wheel`, `settings`). The start route is decided once from `PinManager.hasPin`.

## Testing conventions

- Compose UI tests set `mainClock.autoAdvance = false` and give the app a `Clock` that reads `mainClock.currentTime`. With auto-advance on, the endless spin and decoration frame loops never go idle. Drive time with `advanceTimeBy` and wait on Room/DataStore with a poll-and-settle loop (see `WheelScreenTest`, `PinFlowTest`).
- ViewModel tests use `Dispatchers.setMain(Dispatchers.Unconfined)` when the code under test uses `delay`; `UnconfinedTestDispatcher` never advances it.
- Robolectric fakes text measurement by default. Layout tests that check text widths need `@GraphicsMode(NATIVE)` and `useUnmergedTree = true` (see `SettingsPhoneLayoutTest`).

## Hard constraints

- When a requirement is ambiguous, keep it simple and preserve these, in priority order:
  1. never award a prize beyond its quantity;
  2. never let animation or touch input influence the outcome;
  3. never expose Settings without the PIN.
- No `INTERNET` permission, analytics, or tracking SDKs.
- All UI text is Vietnamese and lives in `res/values/strings.xml` (no hardcoded strings), so English can be added later.
- Theme assets are drawn in code, sounds are generated by `tools/generate_sounds.py`, and the font is Be Vietnam Pro (SIL OFL, licence in `docs/licenses/`).

## Workflow

- Put requirement IDs in commit messages and test names (e.g. `SP-04`).
- Do not add a `Co-Authored-By: Claude` trailer or any other Claude attribution to commit messages.
