package com.github.luckywheel.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.luckywheel.R
import com.github.luckywheel.data.AppSettings
import com.github.luckywheel.sound.SoundEffects
import com.github.luckywheel.ui.pin.PinDots
import com.github.luckywheel.ui.pin.PinKeypad
import com.github.luckywheel.ui.pin.PinPadStyle
import com.github.luckywheel.ui.theme.AppColors
import com.github.luckywheel.ui.theme.AppText

private val NarrowSection = Modifier.widthIn(max = 720.dp)

/** SD-01: sound on/off, plus test buttons that play regardless of the toggle. */
@Composable
fun SoundSection(state: SettingsUiState, vm: SettingsViewModel, sounds: SoundEffects) {
    Column(NarrowSection, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SectionTitle(stringResource(R.string.section_sound))
        ToggleCard(
            stringResource(R.string.sound_title),
            stringResource(R.string.sound_body),
            checked = state.settings.soundEnabled,
            onToggle = vm::setSoundEnabled,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton(stringResource(R.string.test_win), onClick = { sounds.win(state.settings.themeId) }, kind = PillKind.OUTLINED)
            PillButton(stringResource(R.string.test_lose), onClick = sounds::lose, kind = PillKind.OUTLINED)
        }
    }
}

/** SD-02, SD-03: result display time and keep-screen-on. */
@Composable
fun DisplaySection(state: SettingsUiState, vm: SettingsViewModel) {
    Column(NarrowSection, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SectionTitle(stringResource(R.string.section_display))
        SettingsCard(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
            var seconds by remember(state.settings.resultDisplaySeconds) {
                mutableFloatStateOf(state.settings.resultDisplaySeconds.toFloat())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.result_time_title), style = TitleStyle)
                    Text(stringResource(R.string.result_time_body), style = BodyStyle, modifier = Modifier.padding(top = 3.dp))
                }
                Text(
                    stringResource(R.string.seconds_value, seconds.toInt()),
                    style = AppText.copy(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.Text, fontFeatureSettings = "tnum"),
                )
            }
            val range = AppSettings.RESULT_SECONDS_RANGE
            Slider(
                value = seconds,
                onValueChange = { seconds = it },
                onValueChangeFinished = { vm.setResultSeconds(seconds.toInt()) },
                valueRange = range.first.toFloat()..range.last.toFloat(),
                steps = range.last - range.first - 1,
                colors = SliderDefaults.colors(
                    thumbColor = AppColors.Primary,
                    activeTrackColor = AppColors.Primary,
                    inactiveTrackColor = AppColors.Segment,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val small = AppText.copy(fontSize = 13.sp, color = AppColors.TextLabel)
                Text(stringResource(R.string.seconds_value, range.first), style = small)
                Text(stringResource(R.string.seconds_value, range.last), style = small)
            }
        }
        ToggleCard(
            stringResource(R.string.keep_on_title),
            stringResource(R.string.keep_on_body),
            checked = state.settings.keepScreenOn,
            onToggle = vm::setKeepScreenOn,
        )
    }
}

/** ST-05: change the PIN (current, new, new again). */
@Composable
fun SecuritySection(state: SettingsUiState, vm: SettingsViewModel) {
    Column(NarrowSection, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SectionTitle(stringResource(R.string.section_security))
        val change = state.changePin
        if (change == null) {
            SettingsCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.pin_title), style = TitleStyle)
                        Text(
                            stringResource(if (state.pinChanged) R.string.pin_changed else R.string.pin_purpose),
                            style = BodyStyle,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                    PillButton(stringResource(R.string.change_pin), onClick = vm::startChangePin)
                }
            }
            Text(
                stringResource(R.string.pin_forgot_note),
                style = AppText.copy(fontSize = 15.sp, lineHeight = 22.sp, color = AppColors.TextMuted),
                modifier = Modifier.widthIn(max = 560.dp),
            )
        } else {
            SettingsCard(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                padding = PaddingValues(28.dp),
            ) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    CardLabel(stringResource(R.string.change_step, change.step + 1))
                    Text(
                        stringResource(
                            when (change.step) {
                                0 -> R.string.change_current
                                1 -> R.string.change_new
                                else -> R.string.change_confirm
                            },
                        ),
                        style = AppText.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppColors.Text),
                    )
                    PinDots(filled = change.input.length, style = PinPadStyle.LIGHT)
                    Box(Modifier.heightIn(min = 22.dp)) {
                        val error = when (change.error) {
                            ChangePinError.CURRENT_WRONG -> stringResource(R.string.err_current_wrong)
                            ChangePinError.MISMATCH -> stringResource(R.string.err_new_mismatch)
                            null -> ""
                        }
                        Text(error, style = AppText.copy(fontSize = 16.sp, color = AppColors.Error))
                    }
                    PinKeypad(vm::pressChangePin, PinPadStyle.LIGHT, keyWidth = 96.dp)
                    PillButton(stringResource(R.string.cancel), onClick = vm::cancelChangePin, kind = PillKind.TEXT, height = 44.dp)
                }
            }
        }
    }
}
