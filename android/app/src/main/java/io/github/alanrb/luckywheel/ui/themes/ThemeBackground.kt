package io.github.alanrb.luckywheel.ui.themes

import android.graphics.Matrix
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import io.github.alanrb.luckywheel.domain.ThemeId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The theme's background layer, or the owner's custom image under a readability scrim (TH-05).
 * [unitPx] is the size of one design pixel, which scales the moons and cobwebs.
 */
@Composable
fun ThemeBackground(theme: ThemeSpec, customBackground: ImageBitmap?, unitPx: Float, modifier: Modifier = Modifier) {
    Box(modifier) {
        if (customBackground != null) {
            Image(
                bitmap = customBackground,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(Modifier.fillMaxSize().background(scrimColor(theme)))
        } else {
            Canvas(Modifier.fillMaxSize()) { drawThemeBackground(theme, unitPx, withCobwebs = true) }
        }
    }
}

fun scrimColor(theme: ThemeSpec): Color =
    if (theme.isDark) Color(10, 6, 4, (0.42f * 255).toInt()) else Color(255, 250, 244, (0.42f * 255).toInt())

fun DrawScope.drawThemeBackground(theme: ThemeSpec, unitPx: Float, withCobwebs: Boolean) {
    when (theme.id) {
        ThemeId.DEFAULT -> drawRect(cssLinear(165f, size, 0f to Color(0xFFFBF5EC), 0.55f to Color(0xFFF1E2CC), 1f to Color(0xFFE2C9A8)))
        ThemeId.NOEL -> drawRect(cssLinear(165f, size, 0f to Color(0xFF0E3B22), 0.45f to Color(0xFF17331F), 1f to Color(0xFF5B1216)))
        ThemeId.TET -> drawRect(
            ellipseFarthestCorner(
                Offset(size.width * 0.5f, size.height * 0.32f), size,
                listOf(0f to Color(0xFFF6B24A), 0.38f to Color(0xFFD8352A), 0.75f to Color(0xFFA3141A), 1f to Color(0xFF6E0B10)),
            ),
        )
        ThemeId.TRUNG_THU -> {
            drawRect(cssLinear(180f, size, 0f to Color(0xFF070C33), 0.6f to Color(0xFF141C66), 1f to Color(0xFF25307E)))
            drawMoon(Offset(size.width * 0.86f, size.height * 0.19f), 62f * unitPx, 170f * unitPx, Color(0xFFFFF7DA), Color(255, 226, 140), 0.42f)
        }
        ThemeId.LOVE -> drawRect(cssLinear(165f, size, 0f to Color(0xFFFFF0F4), 0.55f to Color(0xFFFFD6E2), 1f to Color(0xFFF8BBD0)))
        ThemeId.HALLOWEEN -> {
            drawRect(cssLinear(180f, size, 0f to Color(0xFF14081F), 0.55f to Color(0xFF2A0F45), 1f to Color(0xFF3F1766)))
            drawMoon(Offset(size.width * 0.13f, size.height * 0.21f), 56f * unitPx, 150f * unitPx, Color(0xFFF3EDD6), Color(243, 237, 214), 0.26f)
            if (withCobwebs) {
                drawCobweb(Offset(size.width, 0f), (PI / 2).toFloat(), 210f * unitPx, unitPx)
                drawCobweb(Offset(0f, size.height), (-PI / 2).toFloat(), 170f * unitPx, unitPx)
            }
        }
    }
}

/** CSS `linear-gradient(<angle>deg, ...)`: 0° points up, 90° right; the line spans the box. */
fun cssLinear(angleDeg: Float, size: Size, vararg stops: Pair<Float, Color>): Brush {
    val rad = Math.toRadians(angleDeg.toDouble())
    val dir = Offset(sin(rad).toFloat(), -cos(rad).toFloat())
    val half = (abs(size.width * dir.x) + abs(size.height * dir.y)) / 2f
    val c = Offset(size.width / 2f, size.height / 2f)
    return Brush.linearGradient(*stops, start = c - dir * half, end = c + dir * half)
}

/** CSS `radial-gradient(ellipse at <center>, ...)` with the default farthest-corner size. */
private fun ellipseFarthestCorner(center: Offset, size: Size, stops: List<Pair<Float, Color>>): Brush {
    val fx = max(center.x, size.width - center.x)
    val fy = max(center.y, size.height - center.y)
    val rx = fx * sqrt(2f)
    val ry = fy * sqrt(2f)
    return object : ShaderBrush() {
        override fun createShader(size: Size): Shader =
            RadialGradient(
                center.x, center.y, ry,
                stops.map { it.second.toArgb() }.toIntArray(), stops.map { it.first }.toFloatArray(),
                Shader.TileMode.CLAMP,
            ).apply { setLocalMatrix(Matrix().apply { setScale(rx / ry, 1f, center.x, center.y) }) }
    }
}

private fun DrawScope.drawMoon(center: Offset, radius: Float, glowRadius: Float, disc: Color, glow: Color, glowAlpha: Float) {
    val inner = (radius + 2f) / glowRadius
    drawCircle(
        brush = Brush.radialGradient(
            0f to glow.copy(alpha = glowAlpha), inner to glow.copy(alpha = glowAlpha), 1f to glow.copy(alpha = 0f),
            center = center, radius = glowRadius,
        ),
        radius = glowRadius, center = center,
    )
    drawCircle(disc, radius, center)
}

/** A quarter cobweb anchored at a screen corner (Halloween). */
private fun DrawScope.drawCobweb(origin: Offset, startAngle: Float, radius: Float, unitPx: Float) {
    val color = Color(230, 225, 245, (0.24f * 255).toInt())
    val stroke = Stroke(width = 1.2f * unitPx)
    val spokes = 6
    val angles = (0..spokes).map { startAngle + it / spokes.toFloat() * (PI / 2).toFloat() }
    fun at(a: Float, r: Float) = origin + Offset(cos(a) * r, sin(a) * r)
    angles.forEach { a -> drawLine(color, origin, at(a, radius), strokeWidth = stroke.width) }
    for (k in 1..5) {
        val r = radius * k / 5.4f
        val path = Path().apply {
            moveTo(at(angles[0], r).x, at(angles[0], r).y)
            for (i in 0 until spokes) {
                val mid = (angles[i] + angles[i + 1]) / 2
                val ctrl = at(mid, r * 0.86f)
                val end = at(angles[i + 1], r)
                quadraticTo(ctrl.x, ctrl.y, end.x, end.y)
            }
        }
        drawPath(path, color, style = stroke)
    }
}
