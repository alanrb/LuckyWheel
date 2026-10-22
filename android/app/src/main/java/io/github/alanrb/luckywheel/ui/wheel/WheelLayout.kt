package io.github.alanrb.luckywheel.ui.wheel

import kotlin.math.max
import kotlin.math.min

/**
 * Scale from the 1280×800 design to this screen: 1.0 on a 10" tablet in either orientation, smaller
 * on phones.
 */
fun designScale(widthDp: Float, heightDp: Float): Float =
    min(max(widthDp, heightDp) / 1280f, min(widthDp, heightDp) / 800f).coerceIn(0.4f, 2f)

/** Sizes for the customer screen (PRD §3.1), in dp/sp, derived from the prototype's layouts. */
data class WheelLayout(
    /** Button (and logo) beside the wheel instead of below it. */
    val split: Boolean,
    val unit: Float,
    val wheelDp: Float,
    val paddingTopDp: Float,
    val paddingBottomDp: Float,
    val gapDp: Float,
    val logoHeightDp: Float,
    val logoMaxWidthDp: Float,
    val buttonWidthDp: Float,
    val buttonHeightDp: Float,
    val buttonFontSp: Float,
    val buttonShadowDp: Float,
    val splitColumnGapDp: Float,
)

private const val MIN_BUTTON_HEIGHT = 56f

fun wheelLayout(widthDp: Float, heightDp: Float, hasLogo: Boolean): WheelLayout {
    val u = designScale(widthDp, heightDp)
    val padTop = 24f * u
    val padBottom = 30f * u
    val gap = 18f * u
    val logoH = 120f * u
    val stackedButtonH = max(92f * u, MIN_BUTTON_HEIGHT)
    val stackedWheel = 0.95f * min(
        widthDp - 48f * u,
        heightDp - padTop - padBottom - (if (hasLogo) logoH + gap else 0f) - gap - stackedButtonH,
    )
    val splitWheel = min(widthDp * 780f / 1280f * 0.9f, heightDp * 0.875f)
    val landscape = widthDp > heightDp
    val split = landscape && (widthDp / heightDp >= 1.9f || stackedWheel < 0.6f * splitWheel)
    return if (split) {
        WheelLayout(
            split = true, unit = u, wheelDp = splitWheel,
            paddingTopDp = padTop, paddingBottomDp = padBottom, gapDp = gap,
            logoHeightDp = 200f * u, logoMaxWidthDp = 380f * u,
            buttonWidthDp = max(340f * u, 160f), buttonHeightDp = max(120f * u, MIN_BUTTON_HEIGHT),
            buttonFontSp = 52f * u, buttonShadowDp = 9f * u, splitColumnGapDp = 56f * u,
        )
    } else {
        WheelLayout(
            split = false, unit = u, wheelDp = stackedWheel,
            paddingTopDp = padTop, paddingBottomDp = padBottom, gapDp = gap,
            logoHeightDp = logoH, logoMaxWidthDp = 520f * u,
            buttonWidthDp = max(340f * u, 160f), buttonHeightDp = stackedButtonH,
            buttonFontSp = 42f * u, buttonShadowDp = 8f * u, splitColumnGapDp = 0f,
        )
    }
}
