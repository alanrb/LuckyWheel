package io.github.alanrb.luckywheel.ui.wheel

import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import io.github.alanrb.luckywheel.R
import io.github.alanrb.luckywheel.domain.prefersWhiteText
import io.github.alanrb.luckywheel.ui.theme.AppColors
import io.github.alanrb.luckywheel.ui.themes.ThemeSpec
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * The wheel (WH-01..WH-06): equal slices in display order, labels along the radius, a hub, and a
 * fixed pointer at the top. [rotation] is read while drawing, so spinning only redraws.
 */
@Composable
fun WheelCanvas(
    slices: List<WheelSlice>,
    theme: ThemeSpec,
    rotation: () -> Float,
    modifier: Modifier = Modifier,
    showLabels: Boolean = true,
) {
    val context = LocalContext.current
    val minFontPx = with(LocalDensity.current) { 9.dp.toPx() }
    val renderer = remember(slices, theme, showLabels) {
        val typeface = runCatching { ResourcesCompat.getFont(context, R.font.be_vietnam_pro_bold) }.getOrNull()
        WheelRenderer(slices, theme, typeface ?: Typeface.DEFAULT_BOLD, minFontPx, showLabels)
    }
    Canvas(modifier) { renderer.draw(this, rotation()) }
}

class WheelRenderer(
    private val slices: List<WheelSlice>,
    private val theme: ThemeSpec,
    typeface: Typeface,
    private val minFontPx: Float,
    private val showLabels: Boolean,
) {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val shadowed = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        textAlign = Paint.Align.RIGHT
    }
    private val pointerPath = Path()
    private val arcBounds = RectF()

    private var cachedSize = -1f
    private var labels: List<FittedLabel> = emptyList()
    private var sheen: Shader? = null

    private fun prepare(s: Float, radius: Float) {
        if (s == cachedSize) return
        cachedSize = s
        sheen = RadialGradient(
            0f, 0f, radius, intArrayOf(0x1AFFFFFF, 0x1A000000), floatArrayOf(0.2f, 1f), Shader.TileMode.CLAMP,
        )
        val n = slices.size
        if (!showLabels || n == 0) {
            labels = emptyList()
            return
        }
        val seg = 2 * PI / n
        val half = if (n > 1) min(PI / 2, seg / 2) else PI / 2
        val maxH = min(radius * 0.5f, (2 * radius * 0.52f * sin(half) * 0.82f).toFloat())
        val maxW = radius * 0.6f
        labels = slices.map { slice ->
            fitLabel(slice.name, maxW, maxH, radius, minFontPx) { str, size ->
                text.textSize = size
                text.measureText(str)
            }
        }
    }

    fun draw(scope: DrawScope, rotationDeg: Float) = with(scope) {
        val s = min(size.width, size.height)
        val left = (size.width - s) / 2
        val top = (size.height - s) / 2
        val c = s / 2
        val ring = s * 0.022f
        val r = c - ring - s * 0.014f
        prepare(s, r)
        drawIntoCanvas { composeCanvas ->
            val canvas = composeCanvas.nativeCanvas
            canvas.save()
            canvas.translate(left, top)

            // Rim with drop shadow.
            shadowed.color = theme.rim.toArgb()
            shadowed.setShadowLayer(s * 0.04f * 0.85f, 0f, s * 0.014f, 0x4D000000)
            canvas.drawCircle(c, c, r + ring, shadowed)

            // Rotating face.
            canvas.save()
            canvas.translate(c, c)
            canvas.rotate(rotationDeg)
            val n = slices.size
            if (n == 0) {
                fill.color = AppColors.EmptyWheel.toArgb()
                canvas.drawCircle(0f, 0f, r, fill)
            }
            val segDeg = 360f / max(1, n)
            arcBounds.set(-r, -r, r, r)
            slices.forEachIndexed { i, slice ->
                fill.color = slice.color
                if (n == 1) canvas.drawCircle(0f, 0f, r, fill)
                else canvas.drawArc(arcBounds, -90f + i * segDeg, segDeg, true, fill)
            }
            if (n > 1) {
                stroke.color = 0x66FFFFFF
                stroke.strokeWidth = max(1f, s * 0.004f)
                for (i in 0 until n) {
                    canvas.save()
                    canvas.rotate(-90f + i * segDeg)
                    canvas.drawLine(0f, 0f, r, 0f, stroke)
                    canvas.restore()
                }
            }
            fill.shader = sheen
            canvas.drawCircle(0f, 0f, r, fill)
            fill.shader = null

            labels.forEachIndexed { i, label ->
                canvas.save()
                canvas.rotate(-90f + (i + 0.5f) * segDeg)
                text.textSize = label.fontSizePx
                text.color = if (prefersWhiteText(slices[i].color)) 0xFFFFFFFF.toInt() else 0xFF111111.toInt()
                val metrics = text.fontMetrics
                val centre = -(metrics.ascent + metrics.descent) / 2
                val x = r * 0.88f
                if (label.lines.size == 1) {
                    canvas.drawText(label.lines[0], x, centre, text)
                } else {
                    canvas.drawText(label.lines[0], x, -label.fontSizePx * 0.56f + centre, text)
                    canvas.drawText(label.lines[1], x, label.fontSizePx * 0.56f + centre, text)
                }
                canvas.restore()
            }
            canvas.restore()

            // Hub.
            shadowed.color = theme.hub.toArgb()
            shadowed.setShadowLayer(s * 0.02f * 0.85f, 0f, s * 0.006f, 0x4D000000)
            canvas.drawCircle(c, c, r * 0.13f, shadowed)
            stroke.color = 0x59FFFFFF
            stroke.strokeWidth = max(1f, s * 0.005f)
            canvas.drawCircle(c, c, r * 0.13f, stroke)

            // Pointer, fixed at the top and pointing into the wheel.
            val w = r * 0.085f
            val pTop = s * 0.006f
            val tip = c - r + r * 0.15f
            pointerPath.reset()
            pointerPath.moveTo(c - w, pTop + w * 0.7f)
            pointerPath.quadTo(c, pTop - w * 0.55f, c + w, pTop + w * 0.7f)
            pointerPath.lineTo(c, tip)
            pointerPath.close()
            shadowed.color = theme.pointer.toArgb()
            shadowed.setShadowLayer(s * 0.015f * 0.85f, 0f, s * 0.006f, 0x59000000)
            canvas.drawPath(pointerPath, shadowed)
            stroke.color = 0xD9FFFFFF.toInt()
            stroke.strokeWidth = max(1f, s * 0.004f)
            canvas.drawPath(pointerPath, stroke)
            fill.color = 0xE6FFFFFF.toInt()
            canvas.drawCircle(c, pTop + w * 0.62f, w * 0.32f, fill)

            canvas.restore()
        }
    }
}
