package io.github.alanrb.luckywheel.ui.pin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.alanrb.luckywheel.R
import io.github.alanrb.luckywheel.ui.theme.AppColors
import io.github.alanrb.luckywheel.ui.theme.AppText

const val PIN_ENTRY_TAG = "pinEntry"
const val PIN_CANCEL_TAG = "pinCancel"

/** The PIN prompt shown over the wheel after the hidden corner hold (ST-01, ST-04). */
@Composable
fun PinEntryOverlay(state: PinEntryState, onKey: (PinKey) -> Unit, onCancel: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .testTag(PIN_ENTRY_TAG)
            .background(Color(18, 12, 9, (0.8f * 255).toInt()))
            // Swallow touches so nothing behind the prompt reacts.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.width(360.dp).verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Text(
                stringResource(R.string.pin_enter_title),
                style = AppText.copy(fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White),
            )
            PinDots(filled = state.input.length, style = PinPadStyle.DARK)
            val message = when {
                state.lockedSeconds > 0 -> stringResource(R.string.pin_locked, state.lockedSeconds)
                state.attemptsLeft != null -> stringResource(R.string.pin_wrong, state.attemptsLeft)
                else -> ""
            }
            Box(Modifier.heightIn(min = 52.dp), contentAlignment = Alignment.Center) {
                Text(
                    message,
                    style = AppText.copy(fontSize = 18.sp, lineHeight = 25.sp, color = AppColors.PinError, textAlign = TextAlign.Center),
                )
            }
            PinKeypad(onKey, PinPadStyle.DARK, enabled = state.lockedSeconds == 0)
            Text(
                stringResource(R.string.cancel),
                style = AppText.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White),
                modifier = Modifier
                    .testTag(PIN_CANCEL_TAG)
                    .height(52.dp)
                    .border(1.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(26.dp))
                    .clickable(role = Role.Button, onClick = onCancel)
                    .padding(horizontal = 28.dp, vertical = 13.dp),
            )
        }
    }
}
