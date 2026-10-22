package io.github.alanrb.luckywheel.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.alanrb.luckywheel.ui.theme.AppColors
import io.github.alanrb.luckywheel.ui.theme.AppText

/** White card with the design's hairline border and 20 dp corners. */
@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(22.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .background(AppColors.Surface, RoundedCornerShape(20.dp))
            .border(1.dp, AppColors.Border, RoundedCornerShape(20.dp))
            .padding(padding),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

@Composable
fun SectionTitle(title: String, subtitle: String? = null) {
    Column {
        Text(title, style = AppText.copy(fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.Text))
        if (subtitle != null) {
            Text(subtitle, style = AppText.copy(fontSize = 16.sp, color = AppColors.TextMuted), modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** Small uppercase card label ("XEM TRƯỚC", "TỔNG TỈ LỆ"). */
@Composable
fun CardLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = AppText.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.06.em, color = AppColors.TextLabel),
        modifier = modifier,
    )
}

val TitleStyle = AppText.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.Text)
val BodyStyle = AppText.copy(fontSize = 15.sp, lineHeight = 22.sp, color = AppColors.TextMuted)

enum class PillKind { PRIMARY, OUTLINED, BRAND_OUTLINED, TEXT, DANGER_TEXT }

/** The prototype's rounded pill buttons. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: PillKind = PillKind.PRIMARY,
    enabled: Boolean = true,
    height: Dp = 46.dp,
    fontSize: TextUnit = 16.sp,
) {
    val shape = RoundedCornerShape(percent = 50)
    val (bg, fg) = when (kind) {
        PillKind.PRIMARY -> AppColors.Primary to AppColors.OnPrimary
        PillKind.OUTLINED -> AppColors.Surface to AppColors.Text
        PillKind.BRAND_OUTLINED -> AppColors.Surface to AppColors.Primary
        PillKind.TEXT -> Color.Transparent to AppColors.TextNeutral
        PillKind.DANGER_TEXT -> Color.Transparent to AppColors.Error
    }
    val border = when (kind) {
        PillKind.OUTLINED -> Modifier.border(1.5.dp, AppColors.InputBorder, shape)
        PillKind.BRAND_OUTLINED -> Modifier.border(1.5.dp, AppColors.Primary, shape)
        else -> Modifier
    }
    val weight = if (kind == PillKind.PRIMARY || kind == PillKind.BRAND_OUTLINED || kind == PillKind.DANGER_TEXT) FontWeight.Bold else FontWeight.SemiBold
    Box(
        modifier
            .height(height)
            .alpha(if (enabled) 1f else 0.4f)
            .background(bg, shape)
            .then(border)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = AppText.copy(fontSize = fontSize, fontWeight = weight, color = fg))
    }
}

@Composable
fun Badge(text: String, background: Color, color: Color, bold: Boolean = true) {
    Text(
        text,
        style = AppText.copy(fontSize = 12.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold, color = color),
        modifier = Modifier.background(background, RoundedCornerShape(percent = 50)).padding(horizontal = 9.dp, vertical = 3.dp),
    )
}

/** The prototype's 56×34 switch. */
@Composable
fun AppSwitch(checked: Boolean) {
    val track by animateColorAsState(if (checked) AppColors.Primary else AppColors.ToggleOff, label = "track")
    val knob by animateDpAsState(if (checked) 26.dp else 4.dp, label = "knob")
    Box(Modifier.size(56.dp, 34.dp).background(track, RoundedCornerShape(17.dp))) {
        Box(
            Modifier
                .offset(x = knob, y = 4.dp)
                .size(26.dp)
                .shadow(2.dp, CircleShape)
                .background(Color.White, CircleShape),
        )
    }
}

/** A whole card that toggles a setting when tapped. */
@Composable
fun ToggleCard(title: String, body: String, checked: Boolean, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val on = if (checked) "on" else "off"
    Row(
        modifier
            .background(AppColors.Surface, RoundedCornerShape(20.dp))
            .border(1.dp, AppColors.Border, RoundedCornerShape(20.dp))
            .clickable(role = Role.Switch) { onToggle(!checked) }
            .semantics { stateDescription = on }
            .padding(horizontal = 22.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = TitleStyle)
            Text(body, style = BodyStyle, modifier = Modifier.padding(top = 3.dp))
        }
        AppSwitch(checked)
    }
}

/** Two-or-more option toggle on a sand-coloured track (colour mode, quantity mode). */
@Composable
fun Segmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    fill: Boolean = false,
    fontSize: TextUnit = 16.sp,
) {
    Row(
        modifier.background(AppColors.Segment, RoundedCornerShape(14.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEachIndexed { i, label ->
            SegmentOption(label, i == selected, { onSelect(i) }, fill, fontSize)
        }
    }
}

@Composable
private fun RowScope.SegmentOption(label: String, selected: Boolean, onClick: () -> Unit, fill: Boolean, fontSize: TextUnit) {
    Box(
        (if (fill) Modifier.weight(1f) else Modifier)
            .height(42.dp)
            .background(if (selected) AppColors.Surface else Color.Transparent, RoundedCornerShape(11.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = if (fill) 6.dp else 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = AppText.copy(fontSize = fontSize, fontWeight = FontWeight.SemiBold, color = AppColors.Text), maxLines = 1)
    }
}

val FieldLabelStyle: TextStyle = AppText.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColors.TextField)
