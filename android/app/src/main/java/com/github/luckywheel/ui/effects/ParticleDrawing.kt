package com.github.luckywheel.ui.effects

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.github.luckywheel.ui.themes.DecorationKind
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Draws particle shapes from the design prototype (dBean, dFlower, dLantern, dBat, ...) at the origin;
 * callers translate and rotate. Holds scratch paths so frames don't allocate them.
 */
class ParticlePainter {
    private val path = Path()

    fun drawDecorations(scope: DrawScope, system: DecorationSystem, timeS: Float, unit: Float) = with(scope) {
        if (system.kind == DecorationKind.SNOW) {
            system.stars.forEach { (x, y, s, ph) ->
                val a = 0.35f + 0.45f * (0.5f + 0.5f * sin(timeS * 2 + ph))
                translate(x, y) { star(this, s, 4, 0.35f, Color(0xFFFFF6D0), a) }
            }
        }
        if (system.kind == DecorationKind.BATS) {
            val pumpkins = listOf(
                Triple(70f * unit, size.height - 55f * unit, 26f * unit),
                Triple(size.width - 80f * unit, size.height - 60f * unit, 30f * unit),
                Triple(size.width - 160f * unit, size.height - 35f * unit, 18f * unit),
            )
            pumpkins.forEachIndexed { i, (x, y, s) ->
                translate(x, y) { pumpkin(this, s, 0.6f + 0.4f * sin(timeS * 1.6f + i)) }
            }
        }
        system.particles.forEach { p ->
            var x = p.x
            var y = p.y
            when (p.type) {
                ParticleType.SNOW, ParticleType.PETAL -> x += sin(p.phase * 1.2f) * 14f * unit
                ParticleType.LANTERN, ParticleType.HEART -> x += sin(p.phase * 0.7f) * 10f * unit
                ParticleType.BAT -> y += sin(p.phase * 1.3f) * 18f * unit
                else -> Unit
            }
            translate(x, y) {
                when (p.type) {
                    ParticleType.BEAN -> rotateRad(p.rotation, Offset.Zero) { bean(this, p.size, p.alpha) }
                    ParticleType.SNOW -> drawCircle(Color.White, p.size, Offset.Zero, alpha = p.alpha)
                    ParticleType.PETAL -> rotateRad(p.rotation, Offset.Zero) { flower(this, p.size, Color(p.color), p.alpha) }
                    ParticleType.LANTERN -> rotateRad(sin(p.phase) * 0.08f, Offset.Zero) { lantern(this, p.size, p.star, p.alpha) }
                    ParticleType.HEART -> rotateRad(sin(p.phase) * 0.2f, Offset.Zero) { heart(this, p.size, Color(p.color), p.alpha) }
                    ParticleType.BAT -> bat(this, p.size, p.phase * 9, Color(p.color), null, p.alpha)
                    else -> Unit
                }
            }
        }
    }

    fun drawEffects(scope: DrawScope, system: WinEffectSystem) = with(scope) {
        system.particles.forEach { p ->
            val a = (p.life / 0.7f).coerceIn(0f, 1f) * if (p.type == ParticleType.SPARK) 0.9f else 1f
            val c = Color(p.color)
            translate(p.x, p.y) {
                when (p.type) {
                    ParticleType.CONFETTI -> rotateRad(p.rotation, Offset.Zero) {
                        scale(1f, cos(p.phase + p.age * 9), Offset.Zero) {
                            drawRect(c, Offset(-p.size / 2, -p.size / 4), Size(p.size, p.size / 2), alpha = a)
                        }
                    }
                    ParticleType.FLAKE -> rotateRad(p.rotation, Offset.Zero) { flake(this, p.size, a) }
                    ParticleType.STAR -> rotateRad(p.rotation, Offset.Zero) { star(this, p.size, 5, 0.45f, c, a) }
                    ParticleType.ENVELOPE -> rotateRad(sin(p.age * 3 + p.phase) * 0.4f, Offset.Zero) { envelope(this, p.size, a) }
                    ParticleType.SPARK -> drawCircle(
                        Brush.radialGradient(listOf(c, Color.White.copy(alpha = 0f)), Offset.Zero, p.size),
                        p.size, Offset.Zero, alpha = a,
                    )
                    ParticleType.LANTERN -> rotateRad(sin(p.age * 2 + p.phase) * 0.1f, Offset.Zero) { lantern(this, p.size, p.star, a) }
                    ParticleType.HEART -> rotateRad(p.rotation * 0.3f, Offset.Zero) { heart(this, p.size, c, a) }
                    ParticleType.PETAL -> rotateRad(p.rotation, Offset.Zero) { flower(this, p.size, c, a) }
                    ParticleType.BAT -> rotateRad(atan2(p.vy, p.vx) * 0.2f, Offset.Zero) { bat(this, p.size, p.age * 16, c, Color(0xFFFFA726), a) }
                    ParticleType.CANDY -> rotateRad(p.rotation, Offset.Zero) { candy(this, p.size, c, a) }
                    ParticleType.BEAN, ParticleType.SNOW -> Unit
                }
            }
        }
    }

    private fun bean(scope: DrawScope, s: Float, alpha: Float) = with(scope) {
        drawOval(Color(0xFF6F4E37), Offset(-1.35f * s, -s), Size(2.7f * s, 2 * s), alpha = alpha)
        path.reset()
        path.moveTo(-1.1f * s, 0f)
        path.quadraticTo(-0.3f * s, -0.45f * s, 0f, 0f)
        path.quadraticTo(0.3f * s, 0.45f * s, 1.1f * s, 0f)
        drawPath(path, Color(0xFF3E2723), alpha = alpha, style = Stroke(0.2f * s))
    }

    private fun flower(scope: DrawScope, s: Float, color: Color, alpha: Float) = with(scope) {
        repeat(5) { i ->
            rotateRad((i * 2 * PI / 5).toFloat(), Offset.Zero) {
                drawOval(color, Offset(-0.38f * s, -1.1f * s), Size(0.76f * s, 1.1f * s), alpha = alpha)
            }
        }
        drawCircle(Color(0xFFFFF3C4), 0.25f * s, Offset.Zero, alpha = alpha)
    }

    private fun heart(scope: DrawScope, s: Float, color: Color, alpha: Float) = with(scope) {
        path.reset()
        path.moveTo(0f, 0.45f * s)
        path.cubicTo(-1.15f * s, -0.3f * s, -0.5f * s, -1.05f * s, 0f, -0.4f * s)
        path.cubicTo(0.5f * s, -1.05f * s, 1.15f * s, -0.3f * s, 0f, 0.45f * s)
        path.close()
        drawPath(path, color, alpha = alpha)
    }

    private fun starPath(r: Float, points: Int, inner: Float) {
        path.reset()
        for (i in 0 until points * 2) {
            val a = -PI / 2 + i * PI / points
            val rr = if (i % 2 == 1) r * inner else r
            val x = (cos(a) * rr).toFloat()
            val y = (sin(a) * rr).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
    }

    private fun star(scope: DrawScope, r: Float, points: Int, inner: Float, color: Color, alpha: Float) = with(scope) {
        starPath(r, points, inner)
        drawPath(path, color, alpha = alpha)
    }

    private fun lantern(scope: DrawScope, s: Float, starShape: Boolean, alpha: Float) = with(scope) {
        drawCircle(
            Brush.radialGradient(listOf(Color(255, 180, 70, 115), Color(255, 180, 70, 0)), Offset.Zero, 2.4f * s),
            2.4f * s, Offset.Zero, alpha = alpha,
        )
        drawLine(Color(255, 230, 180, 128), Offset(0f, -1.05f * s), Offset(0f, -1.9f * s), strokeWidth = max(1f, 0.05f * s), alpha = alpha)
        if (starShape) {
            starPath(1.05f * s, 5, 0.5f)
            drawPath(path, Color(0xFFFFD54F), alpha = alpha)
            drawPath(path, Color(0xFFE65100), alpha = alpha, style = Stroke(0.08f * s))
            return@with
        }
        drawOval(
            Brush.horizontalGradient(listOf(Color(0xFFE65100), Color(0xFFFFB300), Color(0xFFE65100)), -s, s),
            Offset(-0.85f * s, -s), Size(1.7f * s, 2 * s), alpha = alpha,
        )
        listOf(0.45f, 0f, 0.45f).forEach { k ->
            val rx = 0.85f * s * abs(k) + 0.1f
            drawOval(Color(120, 40, 0, 89), Offset(-rx, -s), Size(2 * rx, 2 * s), alpha = alpha, style = Stroke(0.06f * s))
        }
        drawRect(Color(0xFF5D2A0A), Offset(-0.4f * s, -1.08f * s), Size(0.8f * s, 0.18f * s), alpha = alpha)
        drawRect(Color(0xFF5D2A0A), Offset(-0.4f * s, 0.9f * s), Size(0.8f * s, 0.18f * s), alpha = alpha)
        drawLine(Color(0xFFC62828), Offset(0f, 1.08f * s), Offset(0f, 1.6f * s), strokeWidth = 0.1f * s, alpha = alpha)
    }

    private fun bat(scope: DrawScope, s: Float, flap: Float, fill: Color, stroke: Color?, alpha: Float) = with(scope) {
        val f = sin(flap) * 0.45f
        path.reset()
        path.moveTo(0f, -0.15f * s)
        path.quadraticTo(0.5f * s, -(0.6f + f) * s, 1.15f * s, -(0.15f + f) * s)
        path.quadraticTo(0.85f * s, 0.02f * s, 0.62f * s, 0.08f * s)
        path.quadraticTo(0.42f * s, -0.02f * s, 0.22f * s, 0.28f * s)
        path.lineTo(0f, 0.18f * s)
        path.lineTo(-0.22f * s, 0.28f * s)
        path.quadraticTo(-0.42f * s, -0.02f * s, -0.62f * s, 0.08f * s)
        path.quadraticTo(-0.85f * s, 0.02f * s, -1.15f * s, -(0.15f + f) * s)
        path.quadraticTo(-0.5f * s, -(0.6f + f) * s, 0f, -0.15f * s)
        path.close()
        drawPath(path, fill, alpha = alpha)
        if (stroke != null) drawPath(path, stroke, alpha = alpha, style = Stroke(max(1.5f, 0.06f * s)))
        drawOval(fill, Offset(-0.16f * s, -0.26f * s), Size(0.32f * s, 0.52f * s), alpha = alpha)
    }

    private fun pumpkin(scope: DrawScope, s: Float, glow: Float) = with(scope) {
        drawCircle(
            Brush.radialGradient(listOf(Color(255, 140, 30, (0.35f * glow * 255).toInt()), Color(255, 140, 30, 0)), Offset.Zero, 2.4f * s),
            2.4f * s, Offset.Zero,
        )
        listOf(-0.5f to Color(0xFFE8630A), 0.5f to Color(0xFFE8630A), 0f to Color(0xFFFF7A1A)).forEach { (dx, c) ->
            drawOval(c, Offset(dx * s - 0.62f * s, -0.78f * s), Size(1.24f * s, 1.56f * s))
        }
        drawRect(Color(0xFF5B7F2B), Offset(-0.08f * s, -1.02f * s), Size(0.16f * s, 0.3f * s))
    }

    private fun flake(scope: DrawScope, s: Float, alpha: Float) = with(scope) {
        val w = max(1.2f, 0.14f * s)
        repeat(3) { i ->
            rotateRad((i * PI / 3).toFloat(), Offset.Zero) {
                fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
                    drawLine(Color.White, Offset(x1 * s, y1 * s), Offset(x2 * s, y2 * s), w, StrokeCap.Round, alpha = alpha)
                line(-1f, 0f, 1f, 0f)
                line(0.6f, 0f, 0.85f, -0.25f)
                line(0.6f, 0f, 0.85f, 0.25f)
                line(-0.6f, 0f, -0.85f, -0.25f)
                line(-0.6f, 0f, -0.85f, 0.25f)
            }
        }
    }

    private fun envelope(scope: DrawScope, s: Float, alpha: Float) = with(scope) {
        val w = 1.2f * s
        val h = 1.7f * s
        drawRoundRect(Color(0xFFD32F2F), Offset(-w / 2, -h / 2), Size(w, h), CornerRadius(0.12f * s), alpha = alpha)
        path.reset()
        path.moveTo(-w / 2, -h / 2)
        path.lineTo(0f, -h * 0.12f)
        path.lineTo(w / 2, -h / 2)
        path.close()
        drawPath(path, Color(0xFFB71C1C), alpha = alpha)
        drawCircle(Color(0xFFFBC02D), 0.24f * s, Offset(0f, -h * 0.08f), alpha = alpha)
    }

    private fun candy(scope: DrawScope, s: Float, color: Color, alpha: Float) = with(scope) {
        path.reset()
        path.moveTo(-0.5f * s, 0f)
        path.lineTo(-1.05f * s, -0.4f * s)
        path.lineTo(-1.05f * s, 0.4f * s)
        path.close()
        path.moveTo(0.5f * s, 0f)
        path.lineTo(1.05f * s, -0.4f * s)
        path.lineTo(1.05f * s, 0.4f * s)
        path.close()
        drawPath(path, color, alpha = alpha)
        drawCircle(color, 0.55f * s, Offset.Zero, alpha = alpha)
        drawArc(
            Color.White.copy(alpha = 0.6f), startAngle = Math.toDegrees(0.3).toFloat(), sweepAngle = Math.toDegrees(2.3).toFloat(),
            useCenter = false, topLeft = Offset(-0.3f * s, -0.3f * s), size = Size(0.6f * s, 0.6f * s),
            alpha = alpha, style = Stroke(0.14f * s),
        )
    }
}
