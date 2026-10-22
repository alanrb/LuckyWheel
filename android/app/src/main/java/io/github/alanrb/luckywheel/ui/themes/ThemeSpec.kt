package io.github.alanrb.luckywheel.ui.themes

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import io.github.alanrb.luckywheel.R
import io.github.alanrb.luckywheel.domain.ThemeId

enum class DecorationKind { BEANS, SNOW, PETALS, LANTERNS, HEARTS, BATS }

enum class WinEffectKind { CONFETTI, SNOW_STAR, LIXI, LANTERNS, HEARTS, BATS }

/** Everything a theme defines (TH-03). Values are taken from the design prototype. */
data class ThemeSpec(
    val id: ThemeId,
    @param:StringRes val nameRes: Int,
    /** Wheel palette as ARGB ints, shared with the colour assignment in the domain layer. */
    val palette: List<Int>,
    val isDark: Boolean,
    val hub: Color,
    val rim: Color,
    val button: Color,
    val buttonText: Color,
    val pointer: Color,
    val glow: Color,
    val decoration: DecorationKind,
    val winEffect: WinEffectKind,
)

private fun argb(hex: Long) = (0xFF000000 or hex).toInt()
private fun palette(vararg hex: Long) = hex.map(::argb)

val ThemeSpecs: Map<ThemeId, ThemeSpec> = listOf(
    ThemeSpec(
        ThemeId.DEFAULT, R.string.theme_default,
        palette(0x6F4E37, 0xC8A27A, 0xA0522D, 0xE8D5B9, 0x3E2723, 0xD9A066), isDark = false,
        hub = Color(0xFF6F4E37), rim = Color(0xFF3E2723), button = Color(0xFF6F4E37), buttonText = Color(0xFFFFF8EE),
        pointer = Color(0xFF3E2723), glow = Color(0xFFE8C38E),
        decoration = DecorationKind.BEANS, winEffect = WinEffectKind.CONFETTI,
    ),
    ThemeSpec(
        ThemeId.NOEL, R.string.theme_noel,
        palette(0xC62828, 0x2E7D32, 0xFFFFFF, 0xF9A825, 0x1B5E20, 0xE53935), isDark = true,
        hub = Color(0xFFC62828), rim = Color(0xFFF4E6C4), button = Color(0xFFC62828), buttonText = Color(0xFFFFFFFF),
        pointer = Color(0xFFF9A825), glow = Color(0xFFFBD46A),
        decoration = DecorationKind.SNOW, winEffect = WinEffectKind.SNOW_STAR,
    ),
    ThemeSpec(
        ThemeId.TET, R.string.theme_tet,
        palette(0xD32F2F, 0xFBC02D, 0xFF8F00, 0xF48FB1, 0xB71C1C, 0xFFE082), isDark = true,
        hub = Color(0xFFFBC02D), rim = Color(0xFFFFE082), button = Color(0xFFFBC02D), buttonText = Color(0xFF7A0E0E),
        pointer = Color(0xFFFFF3C4), glow = Color(0xFFFFE082),
        decoration = DecorationKind.PETALS, winEffect = WinEffectKind.LIXI,
    ),
    ThemeSpec(
        ThemeId.TRUNG_THU, R.string.theme_trung_thu,
        palette(0xFF8F00, 0xFFD54F, 0x1A237E, 0xE65100, 0xFFF8E1, 0x5C6BC0), isDark = true,
        hub = Color(0xFFFF8F00), rim = Color(0xFFFFD54F), button = Color(0xFFFFD54F), buttonText = Color(0xFF1A237E),
        pointer = Color(0xFFFFF8E1), glow = Color(0xFFFFD54F),
        decoration = DecorationKind.LANTERNS, winEffect = WinEffectKind.LANTERNS,
    ),
    ThemeSpec(
        ThemeId.LOVE, R.string.theme_love,
        palette(0xEC407A, 0xF8BBD0, 0xD81B60, 0xFFFFFF, 0xAD1457, 0xF48FB1), isDark = false,
        hub = Color(0xFFD81B60), rim = Color(0xFFAD1457), button = Color(0xFFD81B60), buttonText = Color(0xFFFFFFFF),
        pointer = Color(0xFFAD1457), glow = Color(0xFFF8BBD0),
        decoration = DecorationKind.HEARTS, winEffect = WinEffectKind.HEARTS,
    ),
    ThemeSpec(
        ThemeId.HALLOWEEN, R.string.theme_halloween,
        palette(0xFF6F00, 0x6A1B9A, 0x212121, 0x9CCC65, 0x4A148C, 0xFFA726), isDark = true,
        hub = Color(0xFFFF6F00), rim = Color(0xFFFF8F1F), button = Color(0xFFFF6F00), buttonText = Color(0xFF14081F),
        pointer = Color(0xFF9CCC65), glow = Color(0xFFFFA726),
        decoration = DecorationKind.BATS, winEffect = WinEffectKind.BATS,
    ),
).associateBy { it.id }

fun themeSpec(id: ThemeId): ThemeSpec = ThemeSpecs.getValue(id)
