package io.github.alanrb.luckywheel.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class PercentFormatTest {

    @Test
    fun parse_acceptsDotAndComma() {
        assertEquals(1250, parsePercentToBp("12,5"))
        assertEquals(1250, parsePercentToBp("12.5"))
        assertEquals(1, parsePercentToBp("0.01"))
        assertEquals(10_000, parsePercentToBp("100"))
        assertEquals(0, parsePercentToBp("0"))
        assertEquals(1234, parsePercentToBp(" 12,34 "))
    }

    @Test
    fun parse_rejectsInvalid() {
        assertNull(parsePercentToBp("100.01"))
        assertNull(parsePercentToBp("abc"))
        assertNull(parsePercentToBp(""))
        assertNull(parsePercentToBp("1.234"))
        assertNull(parsePercentToBp("-5"))
        assertNull(parsePercentToBp("1000"))
    }

    @Test
    fun format_trimsLikePrototype() {
        assertEquals("50%", formatPercent(5000))
        assertEquals("12,5%", formatPercent(1250))
        assertEquals("12,34%", formatPercent(1234))
        assertEquals("0,01%", formatPercent(1))
        assertEquals("0%", formatPercent(0))
    }

    @Test
    fun formatFixed_twoDecimals() {
        assertEquals("100,00%", formatPercentFixed(10_000))
        assertEquals("0,50%", formatPercentFixed(50))
        assertEquals("0,01%", formatPercentFixed(1))
    }

    @Test
    fun inputText_withoutPercentSign() {
        assertEquals("12,5", percentInputText(1250))
        assertEquals("50", percentInputText(5000))
        assertEquals("0,01", percentInputText(1))
    }

    @Test
    fun format_usesAsciiDigitsInAnyLocale() {
        val saved = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"))
            assertEquals("12,34%", formatPercent(1234))
            assertEquals("100,00%", formatPercentFixed(10_000))
        } finally {
            Locale.setDefault(saved)
        }
    }
}
