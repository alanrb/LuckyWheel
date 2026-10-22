package io.github.alanrb.luckywheel.ui.wheel

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min

/** Velocity is measured over the last part of the drag, as in the prototype. */
private const val VELOCITY_WINDOW_MS = 100L

/**
 * Lets the customer turn the wheel with a finger (SP-02). The wheel follows the finger's angle around
 * the centre; on release [onRelease] gets the angular velocity (degrees per second, clockwise
 * positive) and the total angle dragged. Touches outside the wheel or on the hub are ignored.
 */
fun Modifier.wheelDrag(
    enabled: Boolean,
    onDrag: (deltaDeg: Float) -> Unit,
    onRelease: (velocityDegPerSec: Double, draggedDeg: Double) -> Unit,
): Modifier = pointerInput(enabled) {
    if (!enabled) return@pointerInput
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val centre = Offset(size.width / 2f, size.height / 2f)
        val radius = min(size.width, size.height) / 2f
        val distance = (down.position - centre).getDistance()
        if (distance > radius * 0.95f || distance < 10.dp.toPx()) return@awaitEachGesture
        down.consume()

        var lastAngle = angleDeg(down.position - centre)
        var dragged = 0.0
        val samples = ArrayDeque<Pair<Long, Double>>()
        while (true) {
            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) {
                val now = change.uptimeMillis
                val recent = samples.filter { now - it.first < VELOCITY_WINDOW_MS }
                val sum = recent.sumOf { it.second }
                val span = if (recent.isEmpty()) 1.0 else max(0.016, (now - recent.first().first) / 1000.0)
                onRelease(sum / span, dragged)
                break
            }
            val angle = angleDeg(change.position - centre)
            var delta = angle - lastAngle
            if (delta > 180) delta -= 360
            if (delta < -180) delta += 360
            lastAngle = angle
            dragged += abs(delta)
            samples.addLast(change.uptimeMillis to delta)
            while (samples.isNotEmpty() && change.uptimeMillis - samples.first().first >= VELOCITY_WINDOW_MS) {
                samples.removeFirst()
            }
            onDrag(delta.toFloat())
            change.consume()
        }
    }
}

/** Screen angle in degrees; with y pointing down, increasing angles run clockwise. */
private fun angleDeg(v: Offset): Double = Math.toDegrees(atan2(v.y.toDouble(), v.x.toDouble()))
