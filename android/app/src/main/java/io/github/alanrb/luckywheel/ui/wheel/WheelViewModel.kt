package io.github.alanrb.luckywheel.ui.wheel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.alanrb.luckywheel.data.AppSettings
import io.github.alanrb.luckywheel.data.PrizeRepository
import io.github.alanrb.luckywheel.data.SettingsRepository
import io.github.alanrb.luckywheel.domain.Clock
import io.github.alanrb.luckywheel.domain.Outcome
import io.github.alanrb.luckywheel.domain.RandomSource
import io.github.alanrb.luckywheel.domain.SpinPlan
import io.github.alanrb.luckywheel.domain.SpinTuning
import io.github.alanrb.luckywheel.domain.assignSliceColors
import io.github.alanrb.luckywheel.domain.effectiveWheel
import io.github.alanrb.luckywheel.domain.normalizeDegrees
import io.github.alanrb.luckywheel.domain.planSpin
import io.github.alanrb.luckywheel.ui.themes.themeSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WheelSlice(val prizeId: Long, val name: String, val color: Int, val isFallback: Boolean)

sealed interface SpinPhase {
    data object Idle : SpinPhase

    /** The outcome is already decided; the animation only shows it (PRD §3.4). */
    data class Spinning(
        val plan: SpinPlan,
        val startedAtMs: Long,
        val prizeId: Long,
        val prizeName: String,
        val isFallback: Boolean,
    ) : SpinPhase

    data class Revealed(val name: String, val isReal: Boolean, val revealId: Long) : SpinPhase
}

data class WheelUiState(
    val loaded: Boolean = false,
    val slices: List<WheelSlice> = emptyList(),
    val outcomes: List<Outcome> = emptyList(),
    val spinAllowed: Boolean = false,
    val fallbackName: String = "",
    val phase: SpinPhase = SpinPhase.Idle,
    val restRotation: Double = 0.0,
    val settings: AppSettings = AppSettings(),
)

class WheelViewModel(
    private val prizes: PrizeRepository,
    settings: SettingsRepository,
    private val random: RandomSource,
    private val clock: Clock,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val phase = MutableStateFlow(restoreSpin() ?: SpinPhase.Idle)
    private val rest = MutableStateFlow(savedState.get<Double>(KEY_REST) ?: 0.0)
    private var revealing = false
    private var revealCount = 0L

    val state: StateFlow<WheelUiState> =
        combine(prizes.prizes, settings.settings, phase, rest) { list, s, ph, r ->
            val wheel = effectiveWheel(list)
            val colors = assignSliceColors(wheel.outcomes, themeSpec(s.themeId).palette)
            WheelUiState(
                loaded = true,
                slices = wheel.outcomes.mapIndexed { i, o ->
                    WheelSlice(o.prize.id, o.prize.name, colors[i], o.prize.isFallback)
                },
                outcomes = wheel.outcomes,
                spinAllowed = wheel.spinAllowed,
                fallbackName = list.firstOrNull { it.isFallback }?.name.orEmpty(),
                phase = ph,
                restRotation = r,
                settings = s,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, WheelUiState())

    /** SP-01: clockwise at medium strength. */
    fun spinWithButton() = spinWithFlick(direction = 1, strength = SpinTuning.BUTTON_STRENGTH, fromRotation = rest.value)

    /** Starts at most one spin (SP-06); the draw happens here, before any animation. */
    fun spinWithFlick(direction: Int, strength: Double, fromRotation: Double) {
        if (phase.value != SpinPhase.Idle) return
        val current = state.value
        if (!current.loaded || !current.spinAllowed || current.outcomes.isEmpty()) return
        val plan = planSpin(current.outcomes, random, fromRotation, direction, strength)
        val prize = current.outcomes[plan.outcomeIndex].prize
        val spin = SpinPhase.Spinning(plan, clock.elapsedMs(), prize.id, prize.name, prize.isFallback)
        saveSpin(spin)
        phase.value = spin
    }

    /**
     * Reveals the pre-decided result and awards it once (PRD §5.4). If a limited prize somehow has no
     * stock left, the fallback is shown instead so a prize is never awarded beyond its quantity.
     */
    fun onSpinAnimationFinished() {
        val spin = phase.value as? SpinPhase.Spinning ?: return
        if (revealing) return
        revealing = true
        viewModelScope.launch {
            val awarded = runCatching { prizes.claimPrize(spin.prizeId) }.getOrDefault(false)
            val name = if (awarded) spin.prizeName else state.value.fallbackName
            rest.value = normalizeDegrees(spin.plan.endRotation)
            savedState[KEY_REST] = rest.value
            clearSpin()
            phase.value = SpinPhase.Revealed(name, isReal = awarded && !spin.isFallback, revealId = ++revealCount)
            revealing = false
        }
    }

    fun dismissResult() {
        if (phase.value is SpinPhase.Revealed) phase.value = SpinPhase.Idle
    }

    private fun saveSpin(spin: SpinPhase.Spinning) {
        savedState[KEY_INDEX] = spin.plan.outcomeIndex
        savedState[KEY_START] = spin.plan.startRotation
        savedState[KEY_END] = spin.plan.endRotation
        savedState[KEY_DURATION] = spin.plan.durationMs
        savedState[KEY_STARTED] = spin.startedAtMs
        savedState[KEY_NAME] = spin.prizeName
        savedState[KEY_FALLBACK] = spin.isFallback
        savedState[KEY_PRIZE] = spin.prizeId
    }

    private fun clearSpin() {
        listOf(KEY_PRIZE, KEY_INDEX, KEY_START, KEY_END, KEY_DURATION, KEY_STARTED, KEY_NAME, KEY_FALLBACK)
            .forEach { savedState.remove<Any>(it) }
    }

    private fun restoreSpin(): SpinPhase.Spinning? {
        val prizeId = savedState.get<Long>(KEY_PRIZE) ?: return null
        return SpinPhase.Spinning(
            plan = SpinPlan(
                outcomeIndex = savedState[KEY_INDEX] ?: return null,
                startRotation = savedState[KEY_START] ?: return null,
                endRotation = savedState[KEY_END] ?: return null,
                durationMs = savedState[KEY_DURATION] ?: return null,
            ),
            startedAtMs = savedState[KEY_STARTED] ?: return null,
            prizeId = prizeId,
            prizeName = savedState[KEY_NAME] ?: return null,
            isFallback = savedState[KEY_FALLBACK] ?: return null,
        )
    }

    private companion object {
        const val KEY_REST = "wheel.rest"
        const val KEY_PRIZE = "spin.prizeId"
        const val KEY_INDEX = "spin.index"
        const val KEY_START = "spin.start"
        const val KEY_END = "spin.end"
        const val KEY_DURATION = "spin.duration"
        const val KEY_STARTED = "spin.startedAt"
        const val KEY_NAME = "spin.name"
        const val KEY_FALLBACK = "spin.fallback"
    }
}
