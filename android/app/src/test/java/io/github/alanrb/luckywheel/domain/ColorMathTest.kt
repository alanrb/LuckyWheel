package io.github.alanrb.luckywheel.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorMathTest {

    private val palette = listOf(
        0xFF6F4E37.toInt(), 0xFFC8A27A.toInt(), 0xFFA0522D.toInt(),
        0xFFE8D5B9.toInt(), 0xFF3E2723.toInt(), 0xFFD9A066.toInt(),
    )

    private fun themeOutcomes(n: Int) = (1..n).map { Outcome(prize(it.toLong(), 100), 100) }

    @Test
    fun white_onDarkBrown() {
        assertTrue(prefersWhiteText(0xFF3E2723.toInt()))
        assertTrue(prefersWhiteText(0xFFC62828.toInt()))
    }

    @Test
    fun black_onCream() {
        assertFalse(prefersWhiteText(0xFFE8D5B9.toInt()))
        assertFalse(prefersWhiteText(0xFFFFFFFF.toInt()))
        assertFalse(prefersWhiteText(0xFFFBC02D.toInt()))
    }

    @Test
    fun darken_scalesChannels() {
        assertEquals(0xFF453022.toInt(), darken(0xFF6F4E37.toInt(), 0.62))
    }

    @Test
    fun slices_followPaletteInOrder() {
        assertEquals(palette, assignSliceColors(themeOutcomes(6), palette))
    }

    @Test
    fun slices_noAdjacentDuplicate_includingWrap() {
        for (n in 2..12) {
            val colors = assignSliceColors(themeOutcomes(n), palette)
            for (i in colors.indices) {
                assertNotEquals("n=$n i=$i", colors[i], colors[(i + 1) % n].takeIf { n > 2 || i == 0 })
            }
        }
        // 7 slices with a 6-colour palette: the last would repeat the first, so it moves on.
        assertEquals(palette[1], assignSliceColors(themeOutcomes(7), palette).last())
    }

    @Test
    fun slices_customColorKept() {
        val custom = 0xFF00897B.toInt()
        val outcomes = listOf(
            Outcome(prize(1, 100), 100),
            Outcome(prize(2, 100, colorMode = ColorMode.CUSTOM, customColor = custom), 100),
            Outcome(prize(3, 100), 100),
        )
        assertEquals(listOf(palette[0], custom, palette[1]), assignSliceColors(outcomes, palette))
    }

    @Test
    fun slices_themeSliceAfterCustomSkipsSameColour() {
        val outcomes = listOf(
            Outcome(prize(1, 100, colorMode = ColorMode.CUSTOM, customColor = palette[0]), 100),
            Outcome(prize(2, 100), 100),
            Outcome(prize(3, 100), 100),
        )
        val colors = assignSliceColors(outcomes, palette)
        assertEquals(listOf(palette[0], palette[1], palette[2]), colors)
    }
}
