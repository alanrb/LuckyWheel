package io.github.alanrb.luckywheel.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProbabilityTest {

    private fun weights(prizes: List<Prize>) =
        effectiveWheel(prizes).outcomes.map { it.prize.id to it.weightBp }

    @Test
    fun effective_noOutOfStock_keepsConfigured() {
        assertEquals(
            listOf(1L to 5000, 2L to 3000, 3L to 1500, 4L to 500),
            weights(seedSample()),
        )
    }

    @Test
    fun effective_oneOutOfStock_movesToFallback() {
        // PRD §5.2 example
        val prizes = listOf(
            prize(3, 1500, name = "Cà phê sữa đá", sortOrder = 0),
            prize(2, 3000, name = "Giảm 10%", sortOrder = 1),
            prize(4, 500, qty = 0, name = "Ly giữ nhiệt", sortOrder = 2),
            prize(1, 5000, fallback = true, sortOrder = 3),
        )
        assertEquals(listOf(3L to 1500, 2L to 3000, 1L to 5500), weights(prizes))
    }

    @Test
    fun effective_multipleOutOfStock_totalAlways10000() {
        val prizes = listOf(
            prize(1, 1000, fallback = true),
            prize(2, 2000, qty = 0),
            prize(3, 3000, qty = 0),
            prize(4, 4000, qty = 5),
        )
        val wheel = effectiveWheel(prizes)
        assertEquals(listOf(1L to 6000, 4L to 4000), weights(prizes))
        assertEquals(TOTAL_BP, wheel.outcomes.sumOf { it.weightBp })
    }

    @Test
    fun effective_fallbackZero_hidden() {
        val prizes = listOf(
            prize(1, 0, fallback = true),
            prize(2, 6000),
            prize(3, 4000, qty = 3),
        )
        assertEquals(listOf(2L to 6000, 3L to 4000), weights(prizes))
    }

    @Test
    fun spinAllowed_falseWhenAllRealUnavailable() {
        val prizes = listOf(
            prize(1, 5000, fallback = true),
            prize(2, 3000, qty = 0),
            prize(3, 2000, qty = 0),
        )
        val wheel = effectiveWheel(prizes)
        assertFalse(wheel.spinAllowed)
        assertEquals(listOf(1L to 10000), weights(prizes))
        assertTrue(effectiveWheel(seedSample()).spinAllowed)
    }

    @Test
    fun realPrizeWithZeroPercent_notDrawn() {
        val prizes = listOf(
            prize(1, 5000, fallback = true),
            prize(2, 0),
            prize(3, 5000, qty = 2),
        )
        assertEquals(listOf(1L to 5000, 3L to 5000), weights(prizes))
    }

    @Test
    fun outcomes_followSortOrder() {
        val prizes = listOf(
            prize(1, 5000, fallback = true, sortOrder = 2),
            prize(2, 3000, sortOrder = 0),
            prize(3, 2000, sortOrder = 1),
        )
        assertEquals(listOf(2L, 3L, 1L), weights(prizes).map { it.first })
    }
}
