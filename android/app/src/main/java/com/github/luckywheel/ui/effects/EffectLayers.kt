package com.github.luckywheel.ui.effects

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.github.luckywheel.ui.themes.ThemeSpec
import kotlin.math.min

/** Idle theme decorations (TH-03, TH-06); frames stop while the app is in the background (§6). */
@Composable
fun ThemeDecorations(theme: ThemeSpec, unitPx: Float, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val system = remember(theme.decoration, unitPx) { DecorationSystem(theme.decoration, w, h, unitPx) }
        SideEffect { system.resize(w, h) }
        val painter = remember { ParticlePainter() }
        var frameNanos by remember { mutableLongStateOf(0L) }
        val lifecycle = LocalLifecycleOwner.current
        LaunchedEffect(system) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                var last = -1L
                while (true) {
                    withFrameNanos { now ->
                        val dt = if (last < 0) 0f else min(0.05f, (now - last) / 1e9f)
                        last = now
                        system.update(dt)
                        frameNanos = now
                    }
                }
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            painter.drawDecorations(this, system, frameNanos / 1e9f, unitPx)
        }
    }
}

/** The result's burst: theme win effect for a real prize, a soft glow for the fallback (RS-02). */
@Composable
fun ResultEffects(theme: ThemeSpec, isReal: Boolean, revealId: Long, unitPx: Float, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val system = remember(revealId) {
            if (isReal) WinEffectSystem.win(theme.winEffect, w, h, unitPx, theme.palette)
            else WinEffectSystem.lose(w, h, unitPx, theme.glow.toArgb())
        }
        val painter = remember { ParticlePainter() }
        var frameNanos by remember { mutableLongStateOf(0L) }
        LaunchedEffect(system) {
            var last = -1L
            while (!system.isFinished) {
                withFrameNanos { now ->
                    val dt = if (last < 0) 0f else min(0.05f, (now - last) / 1e9f)
                    last = now
                    system.update(dt)
                    frameNanos = now
                }
            }
            frameNanos = -1
        }
        Canvas(Modifier.fillMaxSize()) {
            if (frameNanos != 0L) painter.drawEffects(this, system)
        }
    }
}
