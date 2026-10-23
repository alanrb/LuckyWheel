package com.github.luckywheel.ui.wheel

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.github.luckywheel.R
import com.github.luckywheel.domain.Clock
import com.github.luckywheel.domain.darken
import com.github.luckywheel.domain.flickStrength
import com.github.luckywheel.domain.rotationAt
import com.github.luckywheel.domain.sliceIndexUnderPointer
import com.github.luckywheel.sound.SoundEffects
import com.github.luckywheel.ui.effects.ResultEffects
import com.github.luckywheel.ui.effects.ThemeDecorations
import com.github.luckywheel.ui.rememberStoredImage
import com.github.luckywheel.ui.theme.AppText
import com.github.luckywheel.ui.themes.ThemeBackground
import com.github.luckywheel.ui.themes.ThemeSpec
import com.github.luckywheel.ui.themes.themeSpec
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

object WheelTestTags {
    const val SPIN_BUTTON = "spinButton"
    const val WHEEL = "wheel"
    const val OUT_OF_PRIZES = "outOfPrizes"
    const val RESULT = "resultOverlay"
    const val CORNER = "cornerZone"
}

/** Callbacks from the customer screen, all forwarded to [WheelViewModel]. */
class WheelActions(
    val onSpin: () -> Unit,
    val onFlick: (direction: Int, strength: Double, fromRotation: Double) -> Unit,
    val onSpinFinished: () -> Unit,
    val onDismissResult: () -> Unit,
    /** The hidden 3-second corner hold completed (ST-01). */
    val onCornerHold: () -> Unit,
)

/**
 * The customer screen (PRD §3): logo, wheel and QUAY button over the theme background, with the result
 * overlay and the out-of-prizes state. Nothing on it hints at Settings (CS-03).
 */
@Composable
fun WheelScreen(
    state: WheelUiState,
    clock: Clock,
    sounds: SoundEffects,
    actions: WheelActions,
    modifier: Modifier = Modifier,
) {
    val theme = themeSpec(state.settings.themeId)
    val background by rememberStoredImage(state.settings.customBackgroundPath)
    val logo by rememberStoredImage(state.settings.logoPath)
    val outOfPrizes = state.loaded && !state.spinAllowed
    val idle = state.phase == SpinPhase.Idle

    KeepScreenOn(state.settings.keepScreenOn)
    ResultSound(state, theme, sounds)

    val rotation = remember { mutableFloatStateOf(state.restRotation.toFloat()) }
    val dragOffset = remember { mutableFloatStateOf(0f) }
    val dragging = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var springBack by remember { mutableStateOf<Job?>(null) }
    SpinAnimation(state, clock, rotation::floatValue::set, actions.onSpinFinished)
    TickSounds(state, theme, sounds, dragging::value) { rotation.floatValue + dragOffset.floatValue }

    val springBackToRest: () -> Unit = {
        springBack = scope.launch {
            animate(dragOffset.floatValue, 0f, animationSpec = tween(420, easing = EaseOutBack)) { v, _ ->
                dragOffset.floatValue = v
            }
        }
    }
    // A spin always starts from the angle it plans from; never keep a leftover drag offset on top of it.
    LaunchedEffect(state.phase) {
        if (state.phase is SpinPhase.Spinning) {
            springBack?.cancel()
            dragOffset.floatValue = 0f
            dragging.value = false
        }
    }

    val canDrag = idle && state.loaded && !outOfPrizes
    val dragModifier = Modifier.wheelDrag(
        enabled = canDrag,
        onDrag = { delta ->
            springBack?.cancel()
            dragging.value = true
            dragOffset.floatValue += delta
        },
        onRelease = { velocity, dragged ->
            dragging.value = false
            val strength = flickStrength(velocity, dragged)
            if (strength != null) {
                // SP-02: spin in the flick's direction, starting where the finger left the wheel.
                rotation.floatValue += dragOffset.floatValue
                dragOffset.floatValue = 0f
                actions.onFlick(if (velocity >= 0) 1 else -1, strength, rotation.floatValue.toDouble())
            } else {
                // SP-03: too weak, ease back to the resting angle.
                springBackToRest()
            }
        },
        onCancel = {
            dragging.value = false
            springBackToRest()
        },
    )

    BoxWithConstraints(modifier.fillMaxSize().background(Color.Black)) {
        val layout = wheelLayout(maxWidth.value, maxHeight.value, hasLogo = state.settings.logoPath != null)
        val u = layout.unit
        val unitPx = u * LocalDensity.current.density
        ThemeBackground(theme, background, unitPx, Modifier.fillMaxSize())
        if (state.settings.showThemeDecorations) ThemeDecorations(theme, unitPx, Modifier.fillMaxSize())

        val wheel: @Composable () -> Unit = {
            WheelWithState(state, theme, layout, outOfPrizes, dragModifier) {
                rotation.floatValue + dragOffset.floatValue
            }
        }
        val button: @Composable () -> Unit = {
            // No button spin while a finger is turning the wheel (PRD §11 "tap while flicking"); checked again
            // at tap time because a second finger can land before the next frame disables the button.
            SpinButton(theme, layout, enabled = idle && state.loaded && !outOfPrizes && !dragging.value) {
                if (!dragging.value) actions.onSpin()
            }
        }
        val logoView: @Composable () -> Unit = {
            logo?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .heightIn(max = layout.logoHeightDp.dp)
                        .widthIn(max = layout.logoMaxWidthDp.dp),
                )
            }
        }

        if (layout.split) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(780f).fillMaxHeight(), contentAlignment = Alignment.Center) { wheel() }
                Column(
                    Modifier.weight(500f).fillMaxHeight().padding(end = (48 * u).dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(layout.splitColumnGapDp.dp, Alignment.CenterVertically),
                ) {
                    logoView()
                    button()
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(top = layout.paddingTopDp.dp, bottom = layout.paddingBottomDp.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(layout.gapDp.dp, Alignment.CenterVertically),
            ) {
                if (state.settings.logoPath != null) {
                    Box(Modifier.height(layout.logoHeightDp.dp), contentAlignment = Alignment.Center) { logoView() }
                }
                wheel()
                button()
            }
        }

        CornerHoldZone(
            enabled = idle && state.loaded,
            darkTheme = theme.isDark,
            onTriggered = actions.onCornerHold,
            modifier = Modifier.align(Alignment.TopStart),
        )

        val revealed = state.phase as? SpinPhase.Revealed
        if (revealed != null) {
            ResultOverlay(revealed, theme, u, state.settings.resultDisplaySeconds, actions.onDismissResult) {
                ResultEffects(theme, revealed.isReal, revealed.revealId, unitPx, Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun WheelWithState(
    state: WheelUiState,
    theme: ThemeSpec,
    layout: WheelLayout,
    outOfPrizes: Boolean,
    dragModifier: Modifier,
    rotation: () -> Float,
) {
    val description = stringResource(R.string.cd_wheel)
    Box(Modifier.size(layout.wheelDp.dp), contentAlignment = Alignment.Center) {
        WheelCanvas(
            slices = state.slices,
            theme = theme,
            rotation = rotation,
            modifier = Modifier
                .fillMaxSize()
                .testTag(WheelTestTags.WHEEL)
                .then(dragModifier)
                .semantics { contentDescription = description }
                .then(if (outOfPrizes) Modifier.greyedOut() else Modifier),
        )
        if (outOfPrizes) {
            val u = layout.unit
            Text(
                text = stringResource(R.string.out_of_prizes),
                style = AppText.copy(fontSize = (40 * u).sp, fontWeight = FontWeight.ExtraBold, color = Color.White),
                modifier = Modifier
                    .testTag(WheelTestTags.OUT_OF_PRIZES)
                    .background(Color(20, 14, 10, (0.88f * 255).toInt()), RoundedCornerShape(percent = 50))
                    .padding(horizontal = (36 * u).dp, vertical = (18 * u).dp),
            )
        }
    }
}

/** OP-01: the wheel is shown grey and faded. */
private fun Modifier.greyedOut(): Modifier = graphicsLayer {
    alpha = 0.55f
    val matrix = ColorMatrix().apply { setSaturation(0f) }
    renderEffect = RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(matrix)).asComposeRenderEffect()
}

/** Large 3-D QUAY button (SP-01), styled after the prototype. */
@Composable
private fun SpinButton(theme: ThemeSpec, layout: WheelLayout, enabled: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val u = layout.unit
    val shape = RoundedCornerShape(percent = 50)
    val edge = Color(darken(theme.button.toArgb(), 0.62))
    val lift: Dp = layout.buttonShadowDp.dp
    Box(
        Modifier
            .size(layout.buttonWidthDp.dp, layout.buttonHeightDp.dp + lift)
            .alpha(if (enabled) 1f else 0.5f)
            .testTag(WheelTestTags.SPIN_BUTTON)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        Box(
            Modifier
                .offset(y = lift)
                .size(layout.buttonWidthDp.dp, layout.buttonHeightDp.dp)
                .shadow((18 * u).dp, shape, clip = false)
                .background(edge, shape),
        )
        Box(
            Modifier
                .offset { IntOffset(0, if (pressed && enabled) (5 * u).dp.roundToPx() else 0) }
                .size(layout.buttonWidthDp.dp, layout.buttonHeightDp.dp)
                .background(theme.button, shape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.spin_button),
                style = AppText.copy(
                    fontSize = layout.buttonFontSp.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.14.em,
                    color = theme.buttonText,
                ),
            )
        }
    }
}

/**
 * Drives the rotation from the pre-decided plan and the clock, so the spin continues correctly after
 * rotation or backgrounding (PRD §5.4, §11). Frames stop while the app is not visible.
 */
@Composable
private fun SpinAnimation(state: WheelUiState, clock: Clock, setRotation: (Float) -> Unit, onFinished: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentState by rememberUpdatedState(state)
    val finished by rememberUpdatedState(onFinished)
    val phase = state.phase
    LaunchedEffect(phase, state.restRotation) {
        if (phase !is SpinPhase.Spinning) {
            setRotation(currentState.restRotation.toFloat())
            return@LaunchedEffect
        }
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                withFrameMillis { }
                val elapsed = clock.elapsedMs() - phase.startedAtMs
                setRotation(rotationAt(phase.plan, elapsed).toFloat())
                if (elapsed >= phase.plan.durationMs) {
                    finished()
                    break
                }
            }
        }
    }
}

/** SP-07: a tick each time a slice boundary passes the pointer, while spinning or dragging. */
@Composable
private fun TickSounds(
    state: WheelUiState,
    theme: ThemeSpec,
    sounds: SoundEffects,
    dragging: () -> Boolean,
    displayedRotation: () -> Float,
) {
    val current by rememberUpdatedState(state)
    LaunchedEffect(sounds, theme.id) {
        var last = -1
        snapshotFlow { displayedRotation() }.collect { r ->
            val s = current
            val n = s.slices.size
            if (n == 0) return@collect
            val index = sliceIndexUnderPointer(r.toDouble(), n)
            val moving = s.phase is SpinPhase.Spinning || dragging()
            if (last >= 0 && index != last && moving && s.settings.soundEnabled) sounds.tick(theme.id)
            last = index
        }
    }
}

/** RS-02: the theme's win sound for a real prize, a soft one for the fallback. */
@Composable
private fun ResultSound(state: WheelUiState, theme: ThemeSpec, sounds: SoundEffects) {
    val revealed = state.phase as? SpinPhase.Revealed
    val soundOn = state.settings.soundEnabled
    LaunchedEffect(revealed?.revealId) {
        if (revealed == null || !soundOn) return@LaunchedEffect
        if (revealed.isReal) sounds.win(theme.id) else sounds.lose()
    }
}

/** CS-02: keep the screen on while the wheel is visible, if the owner wants it. */
@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}
