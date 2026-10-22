package io.github.alanrb.luckywheel.ui.wheel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.alanrb.luckywheel.LocalAppContainer

@Composable
fun WheelRoute() {
    val container = LocalAppContainer.current
    val vm: WheelViewModel = viewModel(factory = container.wheelViewModelFactory)
    val state by vm.state.collectAsStateWithLifecycle()
    val actions = remember(vm) {
        WheelActions(
            onSpin = vm::spinWithButton,
            onSpinFinished = vm::onSpinAnimationFinished,
            onDismissResult = vm::dismissResult,
        )
    }
    WheelScreen(state, container.clock, actions)
}
