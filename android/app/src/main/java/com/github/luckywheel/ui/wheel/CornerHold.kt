package com.github.luckywheel.ui.wheel

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private const val HOLD_MS = 3_000L

/**
 * ST-01: holding the top-left 80×80 dp corner for 3 seconds opens the PIN prompt. Nothing is visible
 * until the hold is under way; it only works while the wheel is idle (SP-08).
 */
@Composable
fun CornerHoldZone(enabled: Boolean, darkTheme: Boolean, onTriggered: () -> Unit, modifier: Modifier = Modifier) {
    val triggered by rememberUpdatedState(onTriggered)
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Box(
        modifier
            .size(80.dp)
            .testTag(WheelTestTags.CORNER)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val ring = scope.launch {
                        progress.snapTo(0f)
                        progress.animateTo(1f, tween(HOLD_MS.toInt(), easing = LinearEasing))
                    }
                    val released = withTimeoutOrNull(HOLD_MS) {
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            val p = change.position
                            val outside = p.x < 0 || p.y < 0 || p.x > size.width || p.y > size.height
                            if (!change.pressed || outside) break
                        }
                        true
                    }
                    ring.cancel()
                    scope.launch { progress.snapTo(0f) }
                    if (released == null) triggered()
                }
            },
    ) {
        val p = progress.value
        if (p > 0.15f) {
            val strong = if (darkTheme) Color.White.copy(alpha = 0.8f) else Color(40, 25, 15).copy(alpha = 0.6f)
            val weak = if (darkTheme) Color.White.copy(alpha = 0.15f) else Color(40, 25, 15).copy(alpha = 0.12f)
            Canvas(Modifier.offset(18.dp, 18.dp).size(44.dp)) {
                val width = 8.dp.toPx()
                val inset = width / 2
                val arcSize = Size(size.width - width, size.height - width)
                drawArc(weak, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(width))
                drawArc(strong, -90f, 360f * p, false, Offset(inset, inset), arcSize, style = Stroke(width))
            }
        }
    }
}
