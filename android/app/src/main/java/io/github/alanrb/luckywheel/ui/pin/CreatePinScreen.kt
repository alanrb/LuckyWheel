package io.github.alanrb.luckywheel.ui.pin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.alanrb.luckywheel.LocalAppContainer
import io.github.alanrb.luckywheel.R
import io.github.alanrb.luckywheel.ui.theme.AppColors
import io.github.alanrb.luckywheel.ui.theme.AppText
import io.github.alanrb.luckywheel.ui.themes.cssLinear

const val CREATE_PIN_TAG = "createPinScreen"

/** ST-02, ST-06: first-launch PIN creation, with the note that a forgotten PIN means clearing app data. */
@Composable
fun CreatePinRoute(onDone: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: CreatePinViewModel = viewModel(factory = container.createPinViewModelFactory)
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }
    CreatePinScreen(state, vm::press)
}

@Composable
fun CreatePinScreen(state: CreatePinState, onKey: (PinKey) -> Unit) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .testTag(CREATE_PIN_TAG)
            .drawBehind { drawRect(cssLinear(160f, size, 0f to AppColors.CreatePinTop, 1f to AppColors.CreatePinBottom)) },
    ) {
        val wide = maxWidth > 900.dp
        val intro: @Composable () -> Unit = { CreatePinIntro(Modifier.widthIn(max = 560.dp)) }
        val pad: @Composable () -> Unit = {
            Column(Modifier.width(380.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(22.dp)) {
                Text(
                    text = stringResource(if (state.step == 0) R.string.create_pin_step1 else R.string.create_pin_step2),
                    style = AppText.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White),
                )
                PinDots(filled = state.input.length, style = PinPadStyle.DARK)
                Box(Modifier.heightIn(min = 26.dp), contentAlignment = Alignment.Center) {
                    if (state.mismatch) {
                        Text(
                            stringResource(R.string.create_pin_mismatch),
                            style = AppText.copy(fontSize = 18.sp, color = AppColors.PinError, textAlign = TextAlign.Center),
                        )
                    }
                }
                PinKeypad(onKey, PinPadStyle.DARK)
            }
        }
        if (wide) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 110.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(110.dp, Alignment.CenterHorizontally),
            ) {
                Box(Modifier.weight(1f)) { intro() }
                pad()
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(40.dp),
            ) {
                intro()
                pad()
            }
        }
    }
}

@Composable
private fun CreatePinIntro(modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            stringResource(R.string.create_pin_eyebrow).uppercase(),
            style = AppText.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.14.em, color = AppColors.CreatePinEyebrow),
        )
        Text(
            stringResource(R.string.create_pin_heading),
            style = AppText.copy(fontSize = 54.sp, lineHeight = 59.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.CreatePinText),
        )
        Text(
            stringResource(R.string.create_pin_body),
            style = AppText.copy(fontSize = 20.sp, lineHeight = 31.sp, color = AppColors.CreatePinBody),
            modifier = Modifier.widthIn(max = 480.dp),
        )
        Text(
            stringResource(R.string.pin_forgot_note),
            style = AppText.copy(fontSize = 17.sp, lineHeight = 26.sp, color = AppColors.CreatePinNote),
            modifier = Modifier
                .padding(top = 10.dp)
                .widthIn(max = 480.dp)
                .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                .padding(horizontal = 22.dp, vertical = 18.dp),
        )
    }
}
