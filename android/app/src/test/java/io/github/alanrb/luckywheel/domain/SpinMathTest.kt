package io.github.alanrb.luckywheel.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.floor

class SpinMathTest {

    private fun outcomes(n: Int) = effectiveWheel(
        (0 until n).map { i -> prize(i + 1L, if (i == 0) 10_000 - (n - 1) * 100 else 100, fallback = i == 0) },
    ).outcomes

    private fun mod(a: Double, n: Double) = ((a % n) + n) % n

    @Test
    fun pointerLandsInChosenSlice_forSampledPlans() {
        val random = SeededRandomSource(7)
        for (n in 1..12) {
            val list = outcomes(n)
            val seg = 360.0 / n
            for (dir in listOf(1, -1)) {
                var rotation = 0.0
                repeat(2_000) {
                    val plan = planSpin(list, random, rotation, dir, strength = (it % 11) / 10.0)
                    assertEquals(plan.outcomeIndex, sliceIndexUnderPointer(plan.endRotation, n))
                    // Offset from the slice centre stays inside the inner 70%.
                    val wheelAngle = mod(-plan.endRotation, 360.0)
                    val fromCentre = wheelAngle - (plan.outcomeIndex * seg + seg / 2)
                    assertTrue("n=$n offset=$fromCentre", abs(fromCentre) <= 0.35 * seg + 1e-6)
                    rotation = plan.endRotation
                }
            }
        }
    }

    @Test
    fun button_isMiddleOfRanges() {
        val plan = planSpin(outcomes(4), ScriptedRandomSource(0, 500_000), 0.0, 1, SpinTuning.BUTTON_STRENGTH)
        assertEquals(5250L, plan.durationMs)
        val turns = floor((plan.endRotation - plan.startRotation) / 360.0).toInt()
        assertEquals(6, turns)
    }

    @Test
    fun strength_mapsLinearlyToTurnsAndDuration() {
        val weak = planSpin(outcomes(4), ScriptedRandomSource(0, 500_000), 0.0, 1, 0.0)
        val strong = planSpin(outcomes(4), ScriptedRandomSource(0, 500_000), 0.0, 1, 1.0)
        assertEquals(3500L, weak.durationMs)
        assertEquals(7000L, strong.durationMs)
        assertEquals(4, floor(weak.endRotation / 360.0).toInt())
        assertEquals(8, floor(strong.endRotation / 360.0).toInt())
    }

    @Test
    fun flick_belowThreshold_noSpin() {
        assertNull(flickStrength(259.0, draggedDeg = 90.0))
        assertNull(flickStrength(-259.0, draggedDeg = 90.0))
        assertNull(flickStrength(2000.0, draggedDeg = 7.0))
        assertNotNull(flickStrength(260.0, draggedDeg = 9.0))
    }

    @Test
    fun flick_strengthClamped() {
        assertEquals(1.0, flickStrength(5000.0, 90.0)!!, 0.0)
        assertEquals(0.0, flickStrength(260.0, 90.0)!!, 0.0)
        assertEquals(0.5, flickStrength(-1060.0, 90.0)!!, 1e-9)
    }

    @Test
    fun direction_negative_rotatesBackward() {
        val plan = planSpin(outcomes(6), SeededRandomSource(3), 100.0, -1, 0.5)
        assertTrue(plan.endRotation < plan.startRotation - 6 * 360.0 + 1e-9)
        assertEquals(plan.outcomeIndex, sliceIndexUnderPointer(plan.endRotation, 6))
    }

    @Test
    fun rotationAt_endsExactly() {
        val plan = SpinPlan(outcomeIndex = 0, startRotation = 10.0, endRotation = 2000.0, durationMs = 5000)
        assertEquals(10.0, rotationAt(plan, 0), 0.0)
        assertEquals(2000.0, rotationAt(plan, 5000), 0.0)
        assertEquals(2000.0, rotationAt(plan, 99_000), 0.0)
    }

    @Test
    fun rotationAt_monotonic() {
        val plan = SpinPlan(outcomeIndex = 0, startRotation = 0.0, endRotation = -1800.0, durationMs = 4000)
        var last = rotationAt(plan, 0)
        for (t in 1..4000L step 16) {
            val r = rotationAt(plan, t)
            assertTrue(r <= last)
            last = r
        }
    }

    @Test
    fun sliceIndexUnderPointer_atRest() {
        assertEquals(0, sliceIndexUnderPointer(0.0, 4))
        assertEquals(3, sliceIndexUnderPointer(1.0, 4))   // wheel turned clockwise: last slice moves under pointer
        assertEquals(1, sliceIndexUnderPointer(-100.0, 4))
        assertEquals(0, sliceIndexUnderPointer(360.0, 4))
    }
}
