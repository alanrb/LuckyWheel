package com.github.luckywheel.domain

import kotlin.math.pow
import kotlin.math.roundToInt

private fun channel(argb: Int, shift: Int) = (argb shr shift) and 0xFF

/** WCAG relative luminance of an ARGB colour. */
fun relativeLuminance(argb: Int): Double {
    fun linear(c: Int): Double {
        val v = c / 255.0
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * linear(channel(argb, 16)) + 0.7152 * linear(channel(argb, 8)) + 0.0722 * linear(channel(argb, 0))
}

/** True when white text has at least as much contrast as near-black text (WH-05). */
fun prefersWhiteText(argb: Int): Boolean {
    val l = relativeLuminance(argb)
    return 1.05 / (l + 0.05) >= (l + 0.05) / 0.05
}

fun darken(argb: Int, factor: Double): Int {
    fun scaled(shift: Int) = (channel(argb, shift) * factor).roundToInt().coerceIn(0, 255)
    return (0xFF shl 24) or (scaled(16) shl 16) or (scaled(8) shl 8) or scaled(0)
}

/**
 * Slice colours (WH-04, TH-04): custom colours are kept; theme slices take the next palette colour,
 * skipping ahead when it would match the previous slice, or the first slice for the last one.
 */
fun assignSliceColors(outcomes: List<Outcome>, palette: List<Int>): List<Int> {
    val colors = ArrayList<Int>(outcomes.size)
    var next = 0
    outcomes.forEachIndexed { i, outcome ->
        val prize = outcome.prize
        if (prize.colorMode == ColorMode.CUSTOM && prize.customColor != null) {
            colors += prize.customColor
            return@forEachIndexed
        }
        fun clashes(c: Int) = (i > 0 && c == colors[i - 1]) ||
            (i == outcomes.lastIndex && outcomes.size > 2 && c == colors[0])
        var color = palette[next % palette.size]
        var guard = 0
        while (guard < palette.size && clashes(color)) {
            next++
            color = palette[next % palette.size]
            guard++
        }
        colors += color
        next++
    }
    return colors
}
