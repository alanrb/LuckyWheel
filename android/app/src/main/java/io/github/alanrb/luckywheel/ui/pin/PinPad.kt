package io.github.alanrb.luckywheel.ui.pin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.alanrb.luckywheel.R
import io.github.alanrb.luckywheel.domain.PinPolicy
import io.github.alanrb.luckywheel.ui.theme.AppColors
import io.github.alanrb.luckywheel.ui.theme.AppText

/** Dark keys over the café/wheel backdrop, or light keys inside Settings. */
enum class PinPadStyle { DARK, LIGHT }

fun pinKeyTag(label: String) = "pinKey_$label"

@Composable
fun PinDots(filled: Int, style: PinPadStyle) {
    val dark = style == PinPadStyle.DARK
    val dot = if (dark) 20.dp else 18.dp
    val color = if (dark) Color.White else AppColors.Primary
    Row(horizontalArrangement = Arrangement.spacedBy(if (dark) 18.dp else 16.dp)) {
        repeat(PinPolicy.LENGTH) { i ->
            Box(
                Modifier
                    .size(dot)
                    .border(2.dp, color, CircleShape)
                    .background(if (i < filled) color else Color.Transparent, CircleShape),
            )
        }
    }
}

/** 1–9, blank, 0, delete — the prototype's keypad. */
@Composable
fun PinKeypad(onKey: (PinKey) -> Unit, style: PinPadStyle, enabled: Boolean = true, keyWidth: Dp? = null) {
    val dark = style == PinPadStyle.DARK
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "del"))
    val gap = if (dark) 14.dp else 12.dp
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                row.forEach { label ->
                    val modifier = if (keyWidth != null) Modifier.width(keyWidth) else Modifier.weight(1f)
                    if (label.isEmpty()) {
                        Spacer(modifier)
                    } else {
                        PinKeyButton(label, style, enabled, modifier) {
                            onKey(if (label == "del") PinKey.Delete else PinKey.Digit(label.toInt()))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinKeyButton(label: String, style: PinPadStyle, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val dark = style == PinPadStyle.DARK
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val background = when {
        dark && pressed -> Color.White.copy(alpha = 0.26f)
        dark -> Color.White.copy(alpha = 0.10f)
        pressed -> AppColors.SegmentPressed
        else -> AppColors.Segment
    }
    val deleteDescription = stringResource(R.string.cd_pin_delete)
    Box(
        modifier
            .height(if (dark) 76.dp else 64.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .background(background, RoundedCornerShape(if (dark) 22.dp else 18.dp))
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .testTag(pinKeyTag(label))
            .semantics { if (label == "del") contentDescription = deleteDescription },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (label == "del") "⌫" else label,
            style = AppText.copy(
                fontSize = if (dark) 32.sp else 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (dark) Color.White else AppColors.Text,
            ),
        )
    }
}
