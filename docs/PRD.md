# PRD: Café Lucky Wheel (Android Tablet)

| Field | Value |
|---|---|
| Version | 1.0 (final scope for MVP) |
| Date | 2026-10-22 |
| Platform | Android tablet, native app |
| Tech | Kotlin + Jetpack Compose (Material 3) |
| Connectivity | Fully offline, no internet permission |
| Customer-facing language | Vietnamese |

---

## 1. Summary

A simple prize wheel app for a café. The owner installs it on an Android tablet. Customers see only a full-screen wheel with the café logo at the top and a **QUAY** (Spin) button. They can spin by tapping the button or by flicking the wheel with a finger. The result is random, weighted by probabilities the owner configures.

The owner opens a hidden, PIN-protected Settings menu to manage prizes (name, color, probability, quantity), choose the logo, choose a seasonal theme, and adjust a few display options. When a prize's quantity runs out, its slice disappears from the wheel and its probability moves to the "Chúc may mắn lần sau" (Better luck next time) outcome.

### 1.1 Assumptions

- Anyone in front of the tablet can spin; there are no spin credits or limits. Who gets to spin is controlled physically by the café (e.g. the tablet sits at the counter).
- Staff hand over prizes by looking at the result on screen. There are no redemption codes or win history.
- The tablet is used by one café; no accounts, sync, or multi-device support.

---

## 2. Scope

### 2.1 In scope

- Customer wheel screen with button spin and flick-to-spin.
- Weighted random outcome with per-prize quantity limits.
- Hidden Settings protected by a 4-digit PIN.
- Prize management: name, color, probability %, quantity.
- Logo shown at the top of the screen.
- Six built-in seasonal themes, chosen manually, plus an optional custom background image.
- Theme-specific sound effects with an on/off toggle.

### 2.2 Out of scope

- Screen pinning / kiosk / Lock Task mode.
- Spin credits, staff roles, cooldowns, or spin limits.
- Redemption codes, win history, reports, CSV export.
- Budget or cost tracking.
- Automatic theme switching by date.
- Backup/restore, cloud sync, multiple devices, remote management.
- English UI (strings must still be externalized so it can be added later).

---

## 3. Customer screen

### 3.1 Layout

| Area | Content |
|---|---|
| Top (center) | Café logo, if set. Auto-fits the available height (about 15–20% of screen height), keeps aspect ratio, never overlaps the wheel. If no logo is set, this area collapses and the wheel gets more space. |
| Middle | The wheel, as large as fits, with a fixed pointer at the top of the wheel pointing down. |
| Wheel center | A plain circular hub in the theme's primary color. |
| Bottom | Large **QUAY** button. |
| Background | Theme background (or custom background image) with the theme's animated decorations. |

Both portrait and landscape are supported. In landscape, the logo stays at the top, the wheel is centered, and the button sits below the wheel (or beside it if vertical space is too small).

### 3.2 Display mode

| ID | Requirement |
|---|---|
| CS-01 | The app runs full screen (immersive) with status and navigation bars hidden. The user can still swipe to reveal them; screen pinning is **not** used. |
| CS-02 | Screen stays on while the wheel screen is visible (Settings toggle, default on). |
| CS-03 | No visible link or hint to Settings anywhere on the customer screen. |

### 3.3 Wheel rendering

| ID | Requirement |
|---|---|
| WH-01 | Every visible outcome occupies exactly one slice. All slices are equal size. Slice size does **not** reflect probability. |
| WH-02 | Visible outcomes are: every prize with effective probability > 0 (see §5). Prizes with quantity 0 or probability 0% are not drawn. |
| WH-03 | Slice order follows the order of prizes in Settings. |
| WH-04 | Slice color is the prize's custom color if set; otherwise the next color from the active theme's palette, cycling in order. Adjacent slices should not share the same color (if the palette would cause this, skip to the next palette color). |
| WH-05 | Slice text is the prize name, rotated along the slice radius, auto-sized to fit, up to 2 lines, ellipsized if still too long. Text color is black or white, whichever has better contrast with the slice color. |
| WH-06 | The wheel is drawn with Compose Canvas and stays smooth at 60 fps. |

### 3.4 Spinning

The outcome is **always** decided by the weighted random draw (§5) at the moment a spin starts. The animation only visualizes that outcome. Neither the button nor the flick strength can influence which slice is chosen.

| ID | Requirement |
|---|---|
| SP-01 | **Button spin**: tapping **QUAY** starts a spin clockwise at a medium strength. |
| SP-02 | **Flick spin**: the customer can drag the wheel with a finger. While dragging, the wheel follows the finger rotationally. On release, if the angular velocity is above a threshold, a spin starts in the direction of the flick. |
| SP-03 | **Weak flick**: if the release velocity is below the threshold (or it was just a tap/small drag), no spin happens. The wheel eases back to its resting angle with a short spring animation. |
| SP-04 | **Flick strength affects only the feel**: a stronger flick gives more full rotations and a slightly longer duration. Suggested mapping: 4–8 full turns and 3.5–7 seconds total, scaled linearly by release velocity and clamped. Button spin uses the middle of these ranges. |
| SP-05 | The animation decelerates smoothly (ease-out) and stops with the pointer inside the target slice at a random position within the inner 70% of that slice, so it does not always stop dead center. |
| SP-06 | While a spin is in progress, the button is disabled and touch input on the wheel is ignored. A new spin can only start after the result has been dismissed. |
| SP-07 | Tick sound plays each time a slice boundary passes the pointer (if sound is on), naturally slowing as the wheel slows. |
| SP-08 | The Settings entry gesture is disabled during a spin. |

### 3.5 Result

| ID | Requirement |
|---|---|
| RS-01 | When the wheel stops, a result overlay shows the prize name in large text over a dimmed wheel. |
| RS-02 | A real prize triggers the theme's win effect and win sound. The fallback outcome ("Chúc may mắn lần sau") shows a softer, shorter effect and sound. |
| RS-03 | The overlay closes automatically after the configured result display time (default 10 seconds, range 3–60) or when tapped. The screen then returns to idle and a new spin is possible. |

### 3.6 Out-of-prizes state

| ID | Requirement |
|---|---|
| OP-01 | When no prize other than the fallback is available (every limited prize has quantity 0 and there are no unlimited real prizes with probability > 0), the wheel is shown greyed out, the **QUAY** button is disabled, flicking does nothing, and a message "Đã hết quà" is displayed. |
| OP-02 | The state clears automatically as soon as the owner restocks any prize in Settings. |

---

## 4. Settings (owner)

### 4.1 Access and PIN

| ID | Requirement |
|---|---|
| ST-01 | Settings opens by **long-pressing the top-left corner** of the customer screen for 3 seconds (a touch area about 80×80 dp), then entering the PIN. |
| ST-02 | On first launch, the app asks the owner to create a 4-digit PIN (entered twice) before showing the wheel. |
| ST-03 | The PIN is stored as a salted hash, never in plain text. |
| ST-04 | After 5 wrong attempts, PIN entry is locked for 30 seconds. |
| ST-05 | The PIN can be changed in Settings (requires the current PIN). |
| ST-06 | If the PIN is forgotten, the only recovery is clearing the app's data (all settings are reset). This is mentioned on the PIN creation screen. |
| ST-07 | Settings closes automatically and returns to the wheel after 2 minutes of inactivity. |

### 4.2 Settings structure

| Section | Contents |
|---|---|
| Prizes | Prize list, add/edit/delete/reorder, total % indicator |
| Appearance | Theme, logo, custom background, theme decorations toggle |
| Sound | Sound on/off |
| Display | Result display time, keep screen on |
| Security | Change PIN |

A **Back to wheel** button is always visible.

### 4.3 Prize management

Each prize has:

| Field | Rules |
|---|---|
| Name | Required, 1–24 characters. |
| Color | "Theme color" (default) or a custom color from a picker. |
| Probability | 0–100% with up to 2 decimals (0.01% resolution). Stored internally as integer basis points (1 bp = 0.01%). |
| Quantity | A non-negative integer, or "Unlimited" (Không giới hạn). Represents the **remaining** quantity. |

| ID | Requirement |
|---|---|
| PR-01 | Owner can add, edit, delete, and reorder prizes (drag handle). |
| PR-02 | Exactly one **fallback** prize always exists, created on first launch with the name "Chúc may mắn lần sau". Its quantity is always Unlimited, it cannot be deleted, and its name, color, and probability are editable. It is visually marked in the list. |
| PR-03 | The prize list shows, for each prize: color swatch, name, configured %, remaining quantity (or ∞), and a badge "Hết quà" when quantity is 0. |
| PR-04 | A live total of all probabilities is shown. **Save is only possible when the total is exactly 100.00%.** If it isn't, show how much is missing or over. |
| PR-05 | Validation: at most 12 prizes in total (including fallback); at least 2 prizes with probability > 0%; quantity must be ≥ 0. Errors shown inline in Vietnamese. |
| PR-06 | Restocking is done by editing the quantity. A prize whose quantity goes from 0 to more than 0 reappears on the wheel immediately. |
| PR-07 | The prize list shows a small preview of the wheel as it will currently look (respecting out-of-stock prizes). |
| PR-08 | On first launch, the app seeds sample prizes so the wheel is usable immediately: Chúc may mắn lần sau 50% (fallback), Giảm 10% 30% unlimited, Cà phê sữa đá 15% unlimited, Ly giữ nhiệt 5% quantity 10. |

### 4.4 Logo

| ID | Requirement |
|---|---|
| LG-01 | Owner picks a logo using the Android photo picker (no storage permission needed). PNG and JPG supported; PNG with transparency recommended. |
| LG-02 | The selected image is copied into app-private storage (downscaled to max 1024 px on the long side), so deleting the original from the gallery does not affect the app. |
| LG-03 | Owner can replace or remove the logo. |
| LG-04 | Logo appears top-center on the customer screen as described in §3.1 and does not move or animate. |

### 4.5 Themes

| ID | Requirement |
|---|---|
| TH-01 | Owner chooses one theme manually from a list with a preview thumbnail for each. Changing theme applies immediately. |
| TH-02 | Six built-in themes (see §6): Default, Christmas (Noel), Lunar New Year (Tết), Mid-Autumn (Trung Thu), Love (Valentine / 8-3 / 20-10), Halloween. |
| TH-03 | A theme defines: background, animated idle decorations, wheel color palette, hub and button colors, pointer style, win effect, and sound set. |
| TH-04 | Prizes set to "Theme color" change color when the theme changes. Prizes with a custom color keep it. |
| TH-05 | **Custom background**: owner can pick an image (photo picker) to replace the theme's background layer. The theme's palette, effects, and sounds still apply. A subtle dark/light scrim is applied automatically so the wheel and logo remain readable. Owner can remove the custom background to return to the theme background. |
| TH-06 | Toggle "Show theme decorations" (default on) to turn off the animated idle decorations, e.g. when using a busy custom background. |

### 4.6 Sound and display

| ID | Requirement |
|---|---|
| SD-01 | Sound on/off toggle (default on). Uses the device media volume. |
| SD-02 | Result display time: 3–60 seconds, default 10. |
| SD-03 | Keep screen on: default on. |

---

## 5. Probability rules

### 5.1 Definitions

- Each prize has a configured probability `p` in basis points; all prizes together total exactly 10,000 bp.
- A **real prize** is available if its quantity is Unlimited or greater than 0, and its probability is greater than 0.
- The **fallback** is always available.

### 5.2 Effective probabilities

1. Every available real prize keeps its configured probability.
2. The configured probability of every real prize with quantity 0 is **added to the fallback**.
3. The total therefore always remains exactly 10,000 bp.

Example:

| Prize | Configured | Quantity | Effective |
|---|---|---|---|
| Cà phê sữa đá | 15% | Unlimited | 15% |
| Giảm 10% | 30% | Unlimited | 30% |
| Ly giữ nhiệt | 5% | 0 | 0% (slice hidden) |
| Chúc may mắn lần sau | 50% | ∞ | 55% |

If the fallback's effective probability is 0, its slice is hidden.

### 5.3 Draw

1. If no real prize is available, spinning is disabled (§3.6).
2. Build the list of available outcomes in display order with their effective weights.
3. Draw a uniform random integer from 0 up to (but not including) 10,000 using `java.security.SecureRandom`.
4. Walk the cumulative weights; the first outcome whose cumulative weight exceeds the drawn number is the result.
5. Compute the target angle: the center region of that outcome's slice plus a random offset within the inner 70% of the slice.

Only integer arithmetic is used for weights.

### 5.4 Quantity update and app interruption

- The outcome is decided when the spin starts and held in the ViewModel (and `SavedStateHandle`) during the animation.
- The prize's quantity is decremented by 1 (if limited) **when the result is revealed**, in a single database transaction.
- If the app goes to the background during a spin (no screen pinning, so this is possible), the spin resumes or completes when the app returns, and the same pre-decided result is revealed.
- If the process is killed during a spin, the spin is discarded: nothing was revealed and quantity is unchanged.
- Because only one spin can run at a time on one device, there are no concurrent decrements. Quantity must never go below 0 (the decrement must be guarded in the transaction).

---

## 6. Theme specifications

All decorations and effects must be drawn in code (Compose Canvas, vector shapes, particles) or use bundled assets that are royalty-free / CC0. No copyrighted characters, logos, or music. Sounds are short effects (tick, win, soft "no prize"), generated or CC0, bundled in the app.

Palettes below are suggestions; the implementer may adjust for contrast and harmony.

| Theme | Wheel palette (suggested) | Background | Idle decorations | Win effect | Sound feel |
|---|---|---|---|---|---|
| **Default** | #6F4E37, #C8A27A, #A0522D, #E8D5B9, #3E2723, #D9A066 | Warm cream-to-latte gradient | Subtle floating coffee beans or steam wisps | Confetti burst | Neutral click tick, cheerful chime |
| **Christmas (Noel)** | #C62828, #2E7D32, #FFFFFF, #F9A825, #1B5E20, #E53935 | Deep green to dark red gradient | Falling snow, twinkling stars | Snowflake and star burst | Bell tick, jingle chime |
| **Lunar New Year (Tết)** | #D32F2F, #FBC02D, #FF8F00, #F48FB1, #B71C1C, #FFE082 | Red gradient with soft gold glow | Falling apricot (mai) and peach (đào) blossom petals | Red envelopes (lì xì) falling + fireworks | Drum tick, festive gong / firework pop |
| **Mid-Autumn (Trung Thu)** | #FF8F00, #FFD54F, #1A237E, #E65100, #FFF8E1, #5C6BC0 | Night-blue sky with a large glowing full moon | Floating star-shaped and round lanterns drifting upward | Lanterns rising + golden sparkles | Soft wood-block tick, warm chime |
| **Love (Valentine / 8-3 / 20-10)** | #EC407A, #F8BBD0, #D81B60, #FFFFFF, #AD1457, #F48FB1 | Soft pink gradient | Floating hearts and flower petals | Heart and petal burst | Light tick, sparkly chime |
| **Halloween** | #FF6F00, #6A1B9A, #212121, #9CCC65, #4A148C, #FFA726 | Dark purple night with a pale moon and cobweb corners | Flying bats, gently glowing pumpkins | Bats flying out + candy falling | Creaky tick, playful spooky jingle |

Additional theme rules:

- Each theme also defines hub color, button color, button text color, and pointer color.
- Idle decorations must be lightweight: at most ~60 particles on screen, paused when the app is in the background.
- Win effects last 2–4 seconds and must not hide the prize name.

---

## 7. Data model

### 7.1 Prize (Room)

| Field | Type | Notes |
|---|---|---|
| id | Long | Primary key |
| name | String | 1–24 chars |
| colorMode | Enum | THEME, CUSTOM |
| customColor | Int? | ARGB, used when CUSTOM |
| probabilityBp | Int | 0–10,000 |
| quantity | Int? | null = Unlimited; remaining count |
| isFallback | Boolean | Exactly one true |
| sortOrder | Int | Display and slice order |

### 7.2 Settings (DataStore Preferences)

| Key | Type | Default |
|---|---|---|
| pinHash, pinSalt | String | Set on first launch |
| failedPinAttempts | Int | 0 |
| pinLockedUntil | Long | 0 |
| themeId | Enum | DEFAULT |
| logoPath | String? | null |
| customBackgroundPath | String? | null |
| showThemeDecorations | Boolean | true |
| soundEnabled | Boolean | true |
| resultDisplaySeconds | Int | 10 |
| keepScreenOn | Boolean | true |

Images are stored in app-private internal storage.

---

## 8. Screens

| Screen | Purpose |
|---|---|
| First launch: Create PIN | Create and confirm 4-digit PIN; note about recovery |
| Wheel (customer) | Logo, wheel, QUAY button, result overlay, out-of-prizes state |
| PIN entry | Unlock Settings; lockout state |
| Settings home | Sections listed in §4.2 |
| Prize list | List, total %, wheel preview, add/reorder |
| Prize editor | Name, color, probability, quantity |
| Theme picker | Six themes with preview, custom background, decorations toggle |
| Logo | Pick, preview, remove |
| Change PIN | Current PIN, new PIN twice |

All UI text in Vietnamese, from string resources.

---

## 9. Non-functional requirements

| Area | Requirement |
|---|---|
| Android version | minSdk 33 (Android 13), target the latest stable SDK. |
| Devices | Tablets 8–13 inches; also usable on phones for testing. Portrait and landscape. |
| Offline | No INTERNET permission. No analytics or third-party tracking SDKs. |
| Performance | Cold start under 3 s on a mid-range tablet. Wheel and decorations at a steady 60 fps. |
| Persistence | Prizes, quantities, and settings survive app restarts and device reboots. |
| Randomness | `SecureRandom` only; outcome logic isolated and unit-tested. |
| Fonts | Must render Vietnamese diacritics correctly in all themes. |
| Accessibility | Touch targets at least 48 dp (QUAY button much larger); text contrast meets WCAG AA. |

---

## 10. Technical approach

- **Language/UI**: Kotlin, Jetpack Compose, Material 3, single activity, Compose Navigation.
- **Architecture**: MVVM. Keep a pure Kotlin `domain` package with no Android dependencies containing: effective-probability calculation, weighted draw (with injectable random source), target-angle calculation, validation rules.
- **Persistence**: Room for prizes, DataStore Preferences for settings.
- **Wheel**: Compose `Canvas` for drawing; `Animatable<Float>` for rotation; `pointerInput` with drag detection and `VelocityTracker` for flick; convert touch movement to angular change around the wheel center.
- **Full screen**: `WindowInsetsControllerCompat` to hide system bars with transient-bars-by-swipe behavior; `FLAG_KEEP_SCREEN_ON` controlled by the setting.
- **Images**: Android Photo Picker (`PickVisualMedia`), copy to internal storage, load with Coil.
- **Sound**: `SoundPool` with short bundled effects per theme.
- **Effects**: particle systems drawn on Canvas, driven by a frame clock; paused when not visible.
- **PIN hashing**: PBKDF2 (or similar) with a random salt.
- **DI**: Hilt or simple manual DI, implementer's choice.

---

## 11. Edge cases

| Case | Expected behavior |
|---|---|
| Double tap on QUAY, or tap while flicking | Only one spin starts. |
| Weak flick or accidental touch | No spin; wheel springs back. |
| App backgrounded mid-spin | On return, spin completes and shows the same pre-decided result. |
| Process killed mid-spin | Spin discarded; quantity unchanged. |
| Last unit of a prize won | Quantity becomes 0 on reveal; slice disappears for the next spin; its % moves to fallback. |
| All real prizes out of stock | "Đã hết quà" state (§3.6). |
| Owner sets fallback to 0% and nothing is out of stock | Fallback slice hidden; wheel shows only real prizes. |
| Owner edits prizes while away from the wheel | Changes apply when returning to the wheel. |
| Prize name too long for a small slice | Auto-size text, up to 2 lines, then ellipsis. |
| Logo is very wide or very tall | Fit within the logo area preserving aspect ratio. |
| Custom background makes text hard to read | Automatic scrim keeps contrast acceptable. |
| Many wrong PIN attempts | 30-second lockout after 5 attempts. |
| Rotation between portrait and landscape mid-spin | Spin continues without restarting or changing result. |

---

## 12. Testing requirements

- **Unit tests (domain)**: effective probabilities with zero, one, and multiple out-of-stock prizes; fallback absorbs exactly the right amount; total always 10,000 bp; draw boundaries (first and last value of each range); all real prizes unavailable → spin disallowed; 0.01% prize reachable; validation rules (total ≠ 100%, > 12 prizes, < 2 non-zero prizes).
- **Statistical test**: 100,000 draws with a fixed seed; each outcome's observed frequency within ±0.5 percentage points of its effective probability.
- **Persistence tests**: quantity decrement happens once per revealed spin and never below 0; restock makes the prize available again.
- **UI tests**: button spin shows a result; weak flick does not spin; settings blocked without correct PIN; out-of-prizes state appears and clears after restock.
- **Animation check**: for a sample of draws, the final wheel angle places the pointer inside the chosen slice.

---

## 13. Milestones

| # | Milestone | Done when |
|---|---|---|
| M1 | Project setup + domain logic + tests | All domain and statistical tests pass |
| M2 | Wheel screen: rendering, button spin, result overlay, out-of-prizes state | End-to-end spin with sample prizes works |
| M3 | Flick-to-spin with threshold, direction, and strength mapping | Flick and weak-flick behavior match §3.4 |
| M4 | Settings: PIN, prize CRUD with validation, quantity rules | Owner can fully configure prizes |
| M5 | Logo, themes, custom background, decorations, win effects, sounds | All six themes selectable and complete |
| M6 | Polish: landscape layout, edge cases, performance pass | Section 14 acceptance criteria pass |

---

## 14. Acceptance criteria

1. First launch requires creating a 4-digit PIN; afterwards the wheel shows the sample prizes.
2. Tapping **QUAY** or flicking the wheel strongly starts exactly one spin; a weak flick does not.
3. The wheel spins in the flick direction, and stronger flicks spin longer, but the result distribution is unaffected.
4. Over 100,000 simulated draws (fixed seed), each outcome is within ±0.5 percentage points of its effective probability.
5. A prize with quantity N is won at most N times; when it reaches 0, its slice disappears and the fallback's probability increases by exactly that prize's configured probability.
6. When every real prize is out of stock, the screen shows "Đã hết quà" and spinning is impossible until restock.
7. Settings can only be opened via the hidden corner long-press plus the correct PIN; Settings cannot be saved unless probabilities total exactly 100%.
8. The chosen logo appears top-center and persists after restart, even if the original image is deleted from the gallery.
9. All six themes can be selected and visibly change background, decorations, wheel colors, win effect, and sounds; custom colors on prizes are preserved across theme changes.
10. A custom background replaces the theme background while keeping the theme's palette, effects, and sounds.

---

## Appendix: Notes for Claude Code

- Save this file as `docs/PRD.md` and reference it from `CLAUDE.md`.
- Build milestone by milestone (§13). Do not start UI work before M1 tests pass.
- Reference requirement IDs (e.g. `SP-04`, `PR-04`) in commit messages and test names.
- When something is ambiguous, keep it simple and follow these priorities: never award a prize beyond its quantity, never let animation or touch input influence the outcome, never expose Settings without the PIN.
