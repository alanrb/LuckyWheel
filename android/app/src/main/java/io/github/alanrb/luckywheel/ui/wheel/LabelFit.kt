package io.github.alanrb.luckywheel.ui.wheel

import kotlin.math.max
import kotlin.math.min

data class FittedLabel(val fontSizePx: Float, val lines: List<String>)

/**
 * Fits a prize name into a slice (WH-05), as the design prototype does: the largest size that fits on
 * one line, or on two lines split at the best word boundary; at the minimum size the overflow is
 * ellipsized. [measure] returns the text width at a font size.
 */
fun fitLabel(
    name: String,
    maxWidth: Float,
    maxHeight: Float,
    radius: Float,
    minFontPx: Float,
    measure: (String, Float) -> Float,
): FittedLabel {
    val minSize = max(minFontPx, radius * 0.045f)
    var size = min(radius * 0.115f, maxHeight * 0.9f)
    while (size >= minSize) {
        if (measure(name, size) <= maxWidth) return FittedLabel(size, listOf(name))
        if (size * 2.15f <= maxHeight) {
            bestTwoLineSplit(name, maxWidth, size, measure)?.let { return FittedLabel(size, it) }
        }
        size -= 1f
    }
    size = minSize
    val words = name.split(' ')
    var first = ""
    for (word in words) {
        val candidate = if (first.isEmpty()) word else "$first $word"
        if (measure(candidate, size) <= maxWidth) first = candidate else break
    }
    if (first.isEmpty()) return FittedLabel(size, listOf(ellipsize(name, maxWidth, size, measure)))
    val rest = name.substring(first.length).trim()
    return FittedLabel(size, if (rest.isEmpty()) listOf(first) else listOf(first, ellipsize(rest, maxWidth, size, measure)))
}

private fun bestTwoLineSplit(name: String, maxWidth: Float, size: Float, measure: (String, Float) -> Float): List<String>? {
    val words = name.split(' ')
    if (words.size < 2) return null
    var best: List<String>? = null
    var bestWidth = Float.MAX_VALUE
    for (k in 1 until words.size) {
        val a = words.subList(0, k).joinToString(" ")
        val b = words.subList(k, words.size).joinToString(" ")
        val width = max(measure(a, size), measure(b, size))
        if (width <= maxWidth && width < bestWidth) {
            bestWidth = width
            best = listOf(a, b)
        }
    }
    return best
}

private fun ellipsize(text: String, maxWidth: Float, size: Float, measure: (String, Float) -> Float): String {
    if (measure(text, size) <= maxWidth) return text
    var s = text
    while (s.length > 1 && measure("$s…", size) > maxWidth) s = s.dropLast(1)
    return s.trim() + "…"
}
