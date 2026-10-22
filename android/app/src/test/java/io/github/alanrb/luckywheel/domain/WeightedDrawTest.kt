package io.github.alanrb.luckywheel.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class WeightedDrawTest {

    private val seedOutcomes = effectiveWheel(seedSample()).outcomes

    @Test
    fun boundaries_firstAndLastValueOfEachRange() {
        val expected = mapOf(
            0 to 0, 4999 to 0,
            5000 to 1, 7999 to 1,
            8000 to 2, 9499 to 2,
            9500 to 3, 9999 to 3,
        )
        expected.forEach { (drawn, index) ->
            assertEquals("drawn=$drawn", index, outcomeIndexFor(seedOutcomes, drawn))
        }
    }

    @Test
    fun onePointZeroOneBpPrize_reachable() {
        val outcomes = effectiveWheel(
            listOf(prize(1, 9999, fallback = true), prize(2, 1)),
        ).outcomes
        assertEquals(0, outcomeIndexFor(outcomes, 9998))
        assertEquals(1, outcomeIndexFor(outcomes, 9999))
        assertEquals(1, drawOutcomeIndex(outcomes, ScriptedRandomSource(9999)))
    }

    @Test
    fun draw_usesTotalWeightAsBound() {
        val random = object : RandomSource {
            var bound = -1
            override fun nextInt(bound: Int): Int {
                this.bound = bound
                return 0
            }
        }
        drawOutcomeIndex(seedOutcomes, random)
        assertEquals(TOTAL_BP, random.bound)
    }

    @Test
    fun statistical_100kDraws_withinHalfPoint() {
        val prizes = listOf(
            prize(1, 4000, fallback = true),
            prize(2, 3000),
            prize(3, 1500),
            prize(4, 1000, qty = 0),
            prize(5, 499),
            prize(6, 1),
        )
        val outcomes = effectiveWheel(prizes).outcomes
        val random = SeededRandomSource(42)
        val counts = IntArray(outcomes.size)
        val n = 100_000
        repeat(n) { counts[drawOutcomeIndex(outcomes, random)]++ }
        outcomes.forEachIndexed { i, o ->
            val observedPct = counts[i] * 100.0 / n
            val expectedPct = o.weightBp / 100.0
            assertTrue(
                "outcome ${o.prize.id}: observed $observedPct expected $expectedPct",
                abs(observedPct - expectedPct) <= 0.5,
            )
        }
    }
}
