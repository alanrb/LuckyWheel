package io.github.alanrb.luckywheel.ui.wheel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.alanrb.luckywheel.LocalAppContainer
import io.github.alanrb.luckywheel.ui.pin.PinEntryOverlay
import io.github.alanrb.luckywheel.ui.pin.PinEntryViewModel

@Composable
fun WheelRoute(onOpenSettings: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: WheelViewModel = viewModel(factory = container.wheelViewModelFactory)
    val pinVm: PinEntryViewModel = viewModel(factory = container.pinEntryViewModelFactory)
    val state by vm.state.collectAsStateWithLifecycle()
    val pinState by pinVm.state.collectAsStateWithLifecycle()
    var pinOpen by rememberSaveable { mutableStateOf(false) }

    val actions = remember(vm, pinVm) {
        WheelActions(
            onSpin = vm::spinWithButton,
            onFlick = vm::spinWithFlick,
            onSpinFinished = vm::onSpinAnimationFinished,
            onDismissResult = vm::dismissResult,
            onCornerHold = {
                pinVm.reset()
                pinOpen = true
            },
        )
    }

    LaunchedEffect(pinState.unlocked, pinOpen) {
        if (pinOpen && pinState.unlocked) {
            pinOpen = false
            pinVm.reset()
            onOpenSettings()
        }
    }

    Box(Modifier.fillMaxSize()) {
        WheelScreen(
            state = state,
            clock = container.clock,
            sounds = container.sounds,
            actions = actions,
            modifier = if (pinOpen) Modifier.blur(14.dp) else Modifier,
        )
        if (pinOpen) {
            PinEntryOverlay(pinState, pinVm::press, onCancel = { pinOpen = false })
        }
    }
}
