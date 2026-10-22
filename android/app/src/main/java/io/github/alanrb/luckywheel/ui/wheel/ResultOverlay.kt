package io.github.alanrb.luckywheel.ui.wheel

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.alanrb.luckywheel.R
import io.github.alanrb.luckywheel.ui.theme.AppText
import io.github.alanrb.luckywheel.ui.themes.ThemeSpec

/**
 * RS-01..RS-03: the prize name in large text over a dimmed wheel. It closes after [seconds] or on tap;
 * the bar shows the time left.
 */
@Composable
fun ResultOverlay(
    result: SpinPhase.Revealed,
    theme: ThemeSpec,
    unit: Float,
    seconds: Int,
    onDismiss: () -> Unit,
    effects: @Composable () -> Unit = {},
) {
    val dismiss by rememberUpdatedState(onDismiss)
    val remaining = remember(result.revealId) { Animatable(1f) }
    LaunchedEffect(result.revealId) {
        remaining.animateTo(0f, tween(durationMillis = seconds * 1000, easing = LinearEasing))
        dismiss()
    }

    Box(
        Modifier
            .fillMaxSize()
            .testTag(WheelTestTags.RESULT)
            .background(Color(12, 8, 6, (0.64f * 255).toInt()))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { dismiss() },
    ) {
        effects()
        Column(
            Modifier.fillMaxSize().padding(horizontal = (80 * unit).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy((18 * unit).dp, Alignment.CenterVertically),
        ) {
            if (result.isReal) {
                Text(
                    text = stringResource(R.string.result_congrats),
                    style = AppText.copy(
                        fontSize = (30 * unit).sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.glow,
                        letterSpacing = 0.03.em,
                        textAlign = TextAlign.Center,
                    ),
                )
            }
            val nameSize = when {
                !result.isReal -> 76f
                result.name.length > 16 -> 84f
                else -> 108f
            }
            Text(
                text = result.name,
                style = AppText.copy(
                    fontSize = (nameSize * unit).sp,
                    lineHeight = (nameSize * unit * 1.08f).sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineBreak = LineBreak.Heading,
                    shadow = Shadow(Color(0, 0, 0, 128), Offset(0f, 4f * unit), 30f * unit),
                ),
                modifier = Modifier.widthIn(max = (1080 * unit).dp),
            )
            Spacer(Modifier.height((12 * unit).dp))
            Box(
                Modifier
                    .size((260 * unit).dp, (6 * unit).dp)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(percent = 50)),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(remaining.value)
                        .background(Color.White, RoundedCornerShape(percent = 50)),
                )
            }
            Text(
                text = stringResource(R.string.result_tap_to_close),
                style = AppText.copy(fontSize = (22 * unit).sp, color = Color.White.copy(alpha = 0.82f)),
            )
        }
    }
}
