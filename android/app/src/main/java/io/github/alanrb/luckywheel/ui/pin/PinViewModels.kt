package io.github.alanrb.luckywheel.ui.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.alanrb.luckywheel.data.PinCheck
import io.github.alanrb.luckywheel.data.PinManager
import io.github.alanrb.luckywheel.domain.PinPolicy
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface PinKey {
    data class Digit(val value: Int) : PinKey
    data object Delete : PinKey
}

/** Short pause so the fourth dot is visible before the PIN is checked, as in the prototype. */
internal const val PIN_SUBMIT_DELAY_MS = 180L

/** The input after [key], or null when the key does nothing. */
internal fun String.typed(key: PinKey): String? = when (key) {
    PinKey.Delete -> if (isEmpty()) null else dropLast(1)
    is PinKey.Digit -> if (length >= PinPolicy.LENGTH) null else this + key.value
}

data class CreatePinState(
    val step: Int = 0,
    val input: String = "",
    val mismatch: Boolean = false,
    val busy: Boolean = false,
    val done: Boolean = false,
)

/** ST-02: first launch asks for a 4-digit PIN twice before the wheel is shown. */
class CreatePinViewModel(private val pins: PinManager) : ViewModel() {
    private val _state = MutableStateFlow(CreatePinState())
    val state: StateFlow<CreatePinState> = _state.asStateFlow()
    private var first = ""

    fun press(key: PinKey) {
        val s = _state.value
        if (s.busy || s.done) return
        val input = s.input.typed(key) ?: return
        val full = input.length == PinPolicy.LENGTH
        _state.value = s.copy(input = input, mismatch = s.mismatch && key == PinKey.Delete, busy = full)
        if (full) viewModelScope.launch {
            delay(PIN_SUBMIT_DELAY_MS)
            submit(input)
        }
    }

    private suspend fun submit(pin: String) {
        _state.value = when {
            _state.value.step == 0 -> {
                first = pin
                CreatePinState(step = 1)
            }
            pin == first -> {
                pins.createPin(pin)
                CreatePinState(step = 1, input = pin, done = true)
            }
            else -> {
                first = ""
                CreatePinState(step = 0, mismatch = true)
            }
        }
    }
}

data class PinEntryState(
    val input: String = "",
    /** Set after a wrong PIN; null before any attempt. */
    val attemptsLeft: Int? = null,
    val lockedSeconds: Int = 0,
    val busy: Boolean = false,
    val unlocked: Boolean = false,
)

/** ST-01, ST-04: unlocking Settings, with the 30-second lock after 5 wrong attempts. */
class PinEntryViewModel(private val pins: PinManager) : ViewModel() {
    private val _state = MutableStateFlow(PinEntryState())
    val state: StateFlow<PinEntryState> = _state.asStateFlow()
    private var lockJob: Job? = null

    init {
        watchLock()
    }

    /** Called each time the PIN prompt opens. */
    fun reset() {
        _state.value = PinEntryState()
        watchLock()
    }

    fun press(key: PinKey) {
        val s = _state.value
        if (s.busy || s.unlocked || s.lockedSeconds > 0) return
        val input = s.input.typed(key) ?: return
        val full = input.length == PinPolicy.LENGTH
        _state.value = s.copy(input = input, attemptsLeft = if (key == PinKey.Delete) s.attemptsLeft else null, busy = full)
        if (full) viewModelScope.launch {
            delay(PIN_SUBMIT_DELAY_MS)
            when (val result = pins.check(input)) {
                PinCheck.Ok -> _state.value = PinEntryState(unlocked = true)
                is PinCheck.Wrong -> _state.value = PinEntryState(attemptsLeft = result.attemptsLeft)
                is PinCheck.Locked -> {
                    _state.value = PinEntryState(lockedSeconds = result.seconds)
                    watchLock()
                }
            }
        }
    }

    /** Keeps the lock countdown current until it expires. */
    private fun watchLock() {
        lockJob?.cancel()
        lockJob = viewModelScope.launch {
            while (true) {
                val seconds = pins.lockedSeconds()
                _state.update { it.copy(lockedSeconds = seconds) }
                if (seconds == 0) break
                delay(250)
            }
        }
    }
}
