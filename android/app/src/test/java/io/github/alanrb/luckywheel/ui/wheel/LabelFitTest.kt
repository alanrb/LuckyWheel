package io.github.alanrb.luckywheel.ui.wheel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LabelFitTest {

    /** Monospace stand-in: every character is 0.6 em wide. */
    private val measure: (String, Float) -> Float = { s, size -> s.length * 0.6f * size }

    @Test
    fun shortName_singleLineLargest() {
        val fit = fitLabel("Giảm 10%", maxWidth = 180f, maxHeight = 150f, radius = 300f, minFontPx = 10f, measure = measure)
        assertEquals(listOf("Giảm 10%"), fit.lines)
        assertEquals(34.5f, fit.fontSizePx, 0.01f)
    }

    @Test
    fun twoWords_wrapsToTwoLines() {
        val fit = fitLabel("Cà phê sữa đá", maxWidth = 180f, maxHeight = 150f, radius = 300f, minFontPx = 10f, measure = measure)
        assertEquals(listOf("Cà phê", "sữa đá"), fit.lines)
        assertEquals(34.5f, fit.fontSizePx, 0.01f)
    }

    @Test
    fun longSingleWord_ellipsizedWithinWidth() {
        val name = "Siêucàphêđặcbiệtngonnhất"
        val fit = fitLabel(name, maxWidth = 180f, maxHeight = 40f, radius = 300f, minFontPx = 10f, measure = measure)
        assertEquals(1, fit.lines.size)
        assertTrue(fit.lines[0].endsWith("…"))
        assertTrue(measure(fit.lines[0], fit.fontSizePx) <= 180f)
        assertEquals(13.5f, fit.fontSizePx, 0.01f)
    }

    @Test
    fun longPhrase_inThinSlice_secondLineEllipsized() {
        val name = "Bánh mì thịt nướng đặc biệt"
        val fit = fitLabel(name, maxWidth = 100f, maxHeight = 40f, radius = 300f, minFontPx = 10f, measure = measure)
        assertTrue(fit.lines.size <= 2)
        fit.lines.forEach { assertTrue("'$it' too wide", measure(it, fit.fontSizePx) <= 100f) }
        assertTrue(fit.lines.last().endsWith("…"))
    }
}
