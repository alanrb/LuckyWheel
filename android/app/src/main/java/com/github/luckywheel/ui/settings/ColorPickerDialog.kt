package com.github.luckywheel.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.luckywheel.R
import com.github.luckywheel.ui.theme.AppColors
import com.github.luckywheel.ui.theme.AppText

/** "Màu khác": any colour, picked by hue, saturation and brightness. */
@Composable
fun ColorPickerDialog(initial: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val hsv = remember(initial) { FloatArray(3).also { android.graphics.Color.colorToHSV(initial, it) } }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var saturation by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2]) }
    val color = Color.hsv(hue, saturation, value)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(31, 26, 23, (0.38f * 255).toInt()))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .widthIn(max = 440.dp)
                .shadow(30.dp, RoundedCornerShape(26.dp))
                .background(AppColors.Surface, RoundedCornerShape(26.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(56.dp).background(color, RoundedCornerShape(16.dp)).border(1.dp, Color.Black.copy(alpha = 0.15f), RoundedCornerShape(16.dp)))
                Text(
                    stringResource(R.string.color_picker_title),
                    style = AppText.copy(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.Text),
                )
            }
            ColorSlider(stringResource(R.string.color_hue), hue, 0f..360f, Color.hsv(hue, 1f, 1f)) { hue = it }
            ColorSlider(stringResource(R.string.color_saturation), saturation, 0f..1f, color) { saturation = it }
            ColorSlider(stringResource(R.string.color_brightness), value, 0f..1f, color) { value = it }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                PillButton(stringResource(R.string.cancel), onClick = onDismiss, kind = PillKind.OUTLINED, height = 48.dp)
                PillButton(stringResource(R.string.color_pick), onClick = { onPick(color.toArgb()) }, height = 48.dp)
            }
        }
    }
}

@Composable
private fun ColorSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, thumb: Color, onChange: (Float) -> Unit) {
    Column {
        Text(label, style = FieldLabelStyle)
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = thumb, activeTrackColor = AppColors.Primary, inactiveTrackColor = AppColors.Segment),
        )
    }
}
