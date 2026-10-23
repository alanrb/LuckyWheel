package io.github.alanrb.luckywheel.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.alanrb.luckywheel.LocalAppContainer
import io.github.alanrb.luckywheel.R
import io.github.alanrb.luckywheel.sound.SoundEffects
import io.github.alanrb.luckywheel.ui.theme.AppColors
import io.github.alanrb.luckywheel.ui.theme.AppText
import io.github.alanrb.luckywheel.ui.themes.themeSpec
import kotlinx.coroutines.delay

const val SETTINGS_TAG = "settingsScreen"
const val BACK_TO_WHEEL_TAG = "backToWheel"

/** ST-07: Settings closes by itself after this long without a touch or change. */
const val SETTINGS_IDLE_TIMEOUT_MS = 120_000L

fun settingsNavTag(section: SettingsSection) = "settingsNav_${section.name}"

@Composable
fun SettingsRoute(onExit: () -> Unit) {
    val container = LocalAppContainer.current
    val gate = container.settingsGate
    if (!gate.unlocked) {
        // Restored navigation (e.g. after the process was killed) must not skip the PIN.
        LaunchedEffect(Unit) { onExit() }
        Box(Modifier.fillMaxSize().background(AppColors.Background))
        return
    }
    val exit: () -> Unit = {
        gate.lock()
        onExit()
    }
    LockWhenLeavingApp(gate, exit)
    val vm: SettingsViewModel = viewModel(factory = container.settingsViewModelFactory)
    val state by vm.state.collectAsStateWithLifecycle()
    SettingsScreen(
        state, vm, container.sounds, exit,
        pickerHooks = PickerHooks(onLaunch = { gate.pickerOpen = true }, onResult = { gate.pickerOpen = false }),
    )
}

/** Leaving the app (Home, recents, another app) closes and locks Settings; rotating or picking a photo does not. */
@Composable
private fun LockWhenLeavingApp(gate: SettingsGate, exit: () -> Unit) {
    // The Activity's lifecycle, not the navigation entry's: entries also stop during transitions.
    val activity = LocalActivity.current
    val lifecycleOwner = (activity as? LifecycleOwner) ?: LocalLifecycleOwner.current
    val currentExit by rememberUpdatedState(exit)
    DisposableEffect(lifecycleOwner, activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && activity?.isChangingConfigurations != true && !gate.pickerOpen) {
                currentExit()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

/** Lets the Appearance section tell the gate that the system photo picker is open. */
class PickerHooks(val onLaunch: () -> Unit = {}, val onResult: () -> Unit = {})

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    vm: SettingsViewModel,
    sounds: SoundEffects,
    onExit: () -> Unit,
    pickerHooks: PickerHooks = PickerHooks(),
) {
    val exit by rememberUpdatedState(onExit)
    var touches by remember { mutableIntStateOf(0) }
    LaunchedEffect(touches, state) {
        delay(SETTINGS_IDLE_TIMEOUT_MS)
        exit()
    }
    var pickingColor by remember { mutableIntStateOf(0) }
    // Back closes what is open first; only then does it leave Settings.
    BackHandler(enabled = pickingColor > 0 || state.editor != null || state.changePin != null) {
        when {
            pickingColor > 0 -> pickingColor = 0
            state.editor != null -> vm.cancelEditor()
            else -> vm.cancelChangePin()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .testTag(SETTINGS_TAG)
            .background(AppColors.Background)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        touches++
                    }
                }
            },
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 900.dp
            val compactHeader = maxWidth < 700.dp
            Column(Modifier.fillMaxSize()) {
                Header(onExit, compact = compactHeader)
                val content: @Composable () -> Unit = {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(
                                start = if (wide) 36.dp else 16.dp,
                                end = if (wide) 36.dp else 16.dp,
                                top = if (wide) 28.dp else 20.dp,
                                bottom = if (wide) 40.dp else 32.dp,
                            ),
                    ) {
                        when (state.section) {
                            SettingsSection.PRIZES -> PrizesSection(state, themeSpec(state.settings.themeId), vm)
                            SettingsSection.APPEARANCE -> AppearanceSection(state, vm, pickerHooks)
                            SettingsSection.SOUND -> SoundSection(state, vm, sounds)
                            SettingsSection.DISPLAY -> DisplaySection(state, vm)
                            SettingsSection.SECURITY -> SecuritySection(state, vm)
                        }
                    }
                }
                if (wide) {
                    Row(Modifier.fillMaxSize()) {
                        Column(
                            Modifier.width(272.dp).fillMaxHeight().padding(horizontal = 16.dp, vertical = 22.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            SettingsSection.entries.forEach { NavItem(it, state, vm, Modifier.fillMaxWidth()) }
                        }
                        VerticalDivider(color = AppColors.Border)
                        Box(Modifier.weight(1f)) { content() }
                    }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            SettingsSection.entries.forEach { NavItem(it, state, vm, Modifier) }
                        }
                        HorizontalDivider(color = AppColors.Border)
                        content()
                    }
                }
            }
        }

        state.editor?.let { editor ->
            PrizeEditorDialog(editor, vm, onPickOtherColor = { pickingColor++ })
            if (pickingColor > 0) {
                ColorPickerDialog(
                    initial = editor.customColor,
                    onPick = { c ->
                        vm.updateEditor { it.copy(customColor = c) }
                        pickingColor = 0
                    },
                    onDismiss = { pickingColor = 0 },
                )
            }
        }
    }
}

@Composable
private fun Header(onExit: () -> Unit, compact: Boolean) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .height(if (compact) 64.dp else 76.dp)
                .background(AppColors.HeaderBackground)
                .padding(horizontal = if (compact) 16.dp else 32.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    stringResource(R.string.settings_title),
                    style = AppText.copy(fontSize = if (compact) 22.sp else 26.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.Text),
                    maxLines = 1,
                )
                if (!compact) {
                    Text(
                        stringResource(R.string.app_name),
                        style = AppText.copy(fontSize = 16.sp, color = AppColors.TextMuted),
                        modifier = Modifier.padding(bottom = 3.dp),
                        maxLines = 1,
                    )
                }
            }
            PillButton(
                stringResource(R.string.back_to_wheel),
                onClick = onExit,
                height = if (compact) 44.dp else 52.dp,
                fontSize = if (compact) 15.sp else 18.sp,
                modifier = Modifier.testTag(BACK_TO_WHEEL_TAG),
            )
        }
        HorizontalDivider(color = AppColors.Border)
    }
}

@Composable
private fun NavItem(section: SettingsSection, state: SettingsUiState, vm: SettingsViewModel, modifier: Modifier) {
    val selected = state.section == section
    val (label, sub) = when (section) {
        SettingsSection.PRIZES -> stringResource(R.string.section_prizes) to stringResource(R.string.nav_sub_prizes, state.stored.size)
        SettingsSection.APPEARANCE -> stringResource(R.string.section_appearance) to stringResource(themeSpec(state.settings.themeId).nameRes)
        SettingsSection.SOUND -> stringResource(R.string.section_sound) to
            stringResource(if (state.settings.soundEnabled) R.string.nav_sub_sound_on else R.string.nav_sub_sound_off)
        SettingsSection.DISPLAY -> stringResource(R.string.section_display) to
            stringResource(R.string.nav_sub_display, state.settings.resultDisplaySeconds)
        SettingsSection.SECURITY -> stringResource(R.string.section_security) to stringResource(R.string.nav_sub_security)
    }
    Column(
        modifier
            .background(if (selected) AppColors.NavSelected else Color.Transparent, RoundedCornerShape(14.dp))
            .clickable(role = Role.Tab) { vm.selectSection(section) }
            .testTag(settingsNavTag(section))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(label, style = AppText.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.Text))
        Text(sub, style = AppText.copy(fontSize = 14.sp, color = AppColors.TextMuted))
    }
}
