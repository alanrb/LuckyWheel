package io.github.alanrb.luckywheel.domain

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign

/** Spin feel (PRD SP-04, SP-05), matching the design prototype. */
object SpinTuning {
    const val MIN_TURNS = 4
    const val MAX_TURNS = 8
    const val MIN_DURATION_MS = 3500L
    const val MAX_DURATION_MS = 7000L

    /** Release velocity (degrees per second) below which a flick does not spin. */
    const val FLICK_THRESHOLD = 260.0

    /** Velocity above the threshold that maps to full strength. */
    const val FLICK_RANGE = 1600.0

    /** A drag shorter than this is treated as a tap. */
    const val MIN_DRAG_DEG = 8.0
    const val BUTTON_STRENGTH = 0.5

    /** The pointer stops inside this central fraction of the target slice. */
    const val INNER_FRACTION = 0.7
}

/**
 * Rotation is in degrees, clockwise positive. Slice `i` covers wheel angles
 * `[i * seg, (i + 1) * seg)` measured clockwise from the pointer at rotation 0.
 */
data class SpinPlan(
    val outcomeIndex: Int,
    val startRotation: Double,
    val endRotation: Double,
    val durationMs: Long,
)

private fun mod(a: Double, n: Double) = ((a % n) + n) % n

/** Strength in 0..1 for a released drag, or null when it is too weak to spin (SP-03). */
fun flickStrength(velocityDegPerSec: Double, draggedDeg: Double): Double? {
    val speed = abs(velocityDegPerSec)
    if (speed < SpinTuning.FLICK_THRESHOLD || draggedDeg <= SpinTuning.MIN_DRAG_DEG) return null
    return ((speed - SpinTuning.FLICK_THRESHOLD) / SpinTuning.FLICK_RANGE).coerceIn(0.0, 1.0)
}

/**
 * Draws the outcome and plans the animation that shows it. The outcome comes only from the weighted
 * draw; [direction] and [strength] change only how the spin looks.
 */
fun planSpin(
    outcomes: List<Outcome>,
    random: RandomSource,
    startRotation: Double,
    direction: Int,
    strength: Double,
): SpinPlan {
    val index = drawOutcomeIndex(outcomes, random)
    val seg = 360.0 / outcomes.size
    val unit = random.nextInt(1_000_001) / 1_000_000.0
    val offset = (unit - 0.5) * SpinTuning.INNER_FRACTION * seg
    val restAngle = mod(-(index * seg + seg / 2 + offset), 360.0)
    val s = strength.coerceIn(0.0, 1.0)
    val turns = (SpinTuning.MIN_TURNS + (SpinTuning.MAX_TURNS - SpinTuning.MIN_TURNS) * s).roundToInt()
    val duration = SpinTuning.MIN_DURATION_MS +
        ((SpinTuning.MAX_DURATION_MS - SpinTuning.MIN_DURATION_MS) * s).roundToInt()
    val current = mod(startRotation, 360.0)
    val end = if (direction.sign >= 0) {
        startRotation + turns * 360.0 + mod(restAngle - current, 360.0)
    } else {
        startRotation - turns * 360.0 - mod(current - restAngle, 360.0)
    }
    return SpinPlan(index, startRotation, end, duration)
}

fun sliceIndexUnderPointer(rotationDeg: Double, sliceCount: Int): Int {
    if (sliceCount <= 0) return -1
    val seg = 360.0 / sliceCount
    return floor(mod(-rotationDeg, 360.0) / seg).toInt().coerceIn(0, sliceCount - 1)
}

fun easeOutQuart(t: Double): Double = 1 - (1 - t.coerceIn(0.0, 1.0)).pow(4)

fun rotationAt(plan: SpinPlan, elapsedMs: Long): Double {
    if (elapsedMs >= plan.durationMs) return plan.endRotation
    val t = elapsedMs.coerceAtLeast(0).toDouble() / plan.durationMs
    return plan.startRotation + (plan.endRotation - plan.startRotation) * easeOutQuart(t)
}
