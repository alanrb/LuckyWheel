package io.github.alanrb.luckywheel.domain

import java.util.Locale

private val PERCENT_PATTERN = Regex("^(\\d{1,3})(?:[.,](\\d{1,2}))?$")

/** Parses "12,5" or "12.5" into basis points; null unless 0–100 with at most 2 decimals. */
fun parsePercentToBp(text: String): Int? {
    val match = PERCENT_PATTERN.matchEntire(text.trim()) ?: return null
    val whole = match.groupValues[1].toInt()
    val fraction = match.groupValues[2].padEnd(2, '0').toInt()
    val bp = whole * 100 + fraction
    return bp.takeIf { it <= TOTAL_BP }
}

/** Vietnamese decimal comma, at most one trailing zero trimmed: 1250 → "12,5". */
fun percentInputText(bp: Int): String {
    if (bp % 100 == 0) return (bp / 100).toString()
    return twoDecimals(bp).removeSuffix("0")
}

/** 5000 → "50%", 1250 → "12,5%", 1 → "0,01%". */
fun formatPercent(bp: Int): String = percentInputText(bp) + "%"

/** Always two decimals: 10000 → "100,00%". */
fun formatPercentFixed(bp: Int): String = twoDecimals(bp) + "%"

private fun twoDecimals(bp: Int): String = String.format(Locale.ROOT, "%d,%02d", bp / 100, bp % 100)
