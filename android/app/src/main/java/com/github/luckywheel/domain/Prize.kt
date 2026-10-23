package com.github.luckywheel.domain

enum class ColorMode { THEME, CUSTOM }

/**
 * A prize as configured by the owner (PRD §7.1).
 *
 * [probabilityBp] is in basis points (1 bp = 0.01%). [quantity] is the remaining count, or null
 * for unlimited. Exactly one prize is the [isFallback] ("Chúc may mắn lần sau").
 */
data class Prize(
    val id: Long,
    val name: String,
    val colorMode: ColorMode,
    val customColor: Int?,
    val probabilityBp: Int,
    val quantity: Int?,
    val isFallback: Boolean,
    val sortOrder: Int,
) {
    val isOutOfStock: Boolean get() = !isFallback && quantity == 0
}
