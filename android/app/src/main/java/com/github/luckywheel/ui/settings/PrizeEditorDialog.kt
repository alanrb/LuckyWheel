package com.github.luckywheel.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.luckywheel.R
import com.github.luckywheel.domain.ColorMode
import com.github.luckywheel.domain.EditorField
import com.github.luckywheel.domain.MAX_NAME_LENGTH
import com.github.luckywheel.ui.theme.AppColors
import com.github.luckywheel.ui.theme.AppText

object EditorTags {
    const val NAME = "editorName"
    const val PERCENT = "editorPercent"
    const val QUANTITY = "editorQuantity"
    const val DONE = "editorDone"
    const val DELETE = "editorDelete"
}

/** The 12 quick colours from the prototype. */
private val Swatches = listOf(
    0xFF6F4E37, 0xFFC62828, 0xFF2E7D32, 0xFFF9A825, 0xFF1A237E, 0xFFEC407A,
    0xFF6A1B9A, 0xFFFF6F00, 0xFF00897B, 0xFF455A64, 0xFFFFFFFF, 0xFF212121,
).map { it.toInt() }

/** Add or edit one prize (PR-01, PR-02, §4.3). Changes go to the draft; the list's Save stores them. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrizeEditorDialog(editor: EditorState, vm: SettingsViewModel, onPickOtherColor: () -> Unit) {
    val errors = editor.errors
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(31, 26, 23, (0.38f * 255).toInt()))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = vm::cancelEditor)
            .imePadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .widthIn(max = 620.dp)
                .heightIn(max = 740.dp)
                .shadow(30.dp, RoundedCornerShape(26.dp))
                .background(AppColors.Surface, RoundedCornerShape(26.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(if (editor.id == null) R.string.editor_title_add else R.string.editor_title_edit),
                    style = AppText.copy(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.Text),
                )
                if (editor.isFallback) Badge(stringResource(R.string.badge_fallback), AppColors.BadgeFallback, AppColors.Primary)
            }

            // Name
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.field_name), style = FieldLabelStyle)
                    Text(
                        stringResource(R.string.name_counter, editor.name.length, MAX_NAME_LENGTH),
                        style = FieldLabelStyle.copy(fontWeight = FontWeight.Medium, color = AppColors.TextLabel),
                    )
                }
                EditorField(
                    value = editor.name,
                    onValueChange = { v -> vm.updateEditor { it.copy(name = v.take(MAX_NAME_LENGTH)) } },
                    error = EditorField.NAME in errors,
                    placeholder = stringResource(R.string.name_placeholder),
                    modifier = Modifier.fillMaxWidth().testTag(EditorTags.NAME),
                )
                ErrorLine(if (EditorField.NAME in errors) stringResource(R.string.err_name) else "")
            }

            // Colour
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.field_color), style = FieldLabelStyle)
                Segmented(
                    options = listOf(stringResource(R.string.color_theme), stringResource(R.string.color_custom)),
                    selected = if (editor.colorMode == ColorMode.THEME) 0 else 1,
                    onSelect = { i -> vm.updateEditor { it.copy(colorMode = if (i == 0) ColorMode.THEME else ColorMode.CUSTOM) } },
                )
                if (editor.colorMode == ColorMode.CUSTOM) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Swatches.forEach { c -> ColorSwatch(c, selected = c == editor.customColor) { vm.updateEditor { it.copy(customColor = c) } } }
                        Row(
                            Modifier.height(40.dp).clickable(role = Role.Button, onClick = onPickOtherColor),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ColorSwatch(editor.customColor, selected = editor.customColor !in Swatches, onClick = onPickOtherColor)
                            Text(stringResource(R.string.color_other), style = AppText.copy(fontSize = 14.sp, color = AppColors.TextNeutral))
                        }
                    }
                }
            }

            // Percent and quantity
            BoxWithConstraints {
                val twoColumns = maxWidth > 440.dp
                val percent: @Composable (Modifier) -> Unit = { m -> PercentField(editor, vm, m) }
                val quantity: @Composable (Modifier) -> Unit = { m -> QuantityField(editor, vm, m) }
                if (twoColumns) {
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        percent(Modifier.weight(1f))
                        quantity(Modifier.weight(1f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        percent(Modifier.fillMaxWidth())
                        quantity(Modifier.fillMaxWidth())
                    }
                }
            }

            HorizontalDivider(color = AppColors.Divider)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (editor.canDelete) {
                    PillButton(
                        stringResource(R.string.delete_prize),
                        onClick = { editor.id?.let(vm::deletePrize) },
                        kind = PillKind.DANGER_TEXT,
                        height = 48.dp,
                        modifier = Modifier.testTag(EditorTags.DELETE),
                    )
                }
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton(stringResource(R.string.cancel), onClick = vm::cancelEditor, kind = PillKind.OUTLINED, height = 48.dp)
                    PillButton(
                        stringResource(R.string.done),
                        onClick = vm::commitEditor,
                        enabled = errors.isEmpty(),
                        height = 48.dp,
                        modifier = Modifier.testTag(EditorTags.DONE),
                    )
                }
            }
        }
    }
}

@Composable
private fun PercentField(editor: EditorState, vm: SettingsViewModel, modifier: Modifier) {
    val error = EditorField.PERCENT in editor.errors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.field_percent), style = FieldLabelStyle)
        EditorField(
            value = editor.percentText,
            onValueChange = { v -> vm.updateEditor { it.copy(percentText = v.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(6)) } },
            error = error,
            keyboardType = KeyboardType.Decimal,
            suffix = "%",
            textStyle = InputStyle.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.fillMaxWidth().testTag(EditorTags.PERCENT),
        )
        ErrorLine(if (error) stringResource(R.string.err_percent) else "")
    }
}

@Composable
private fun QuantityField(editor: EditorState, vm: SettingsViewModel, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.field_quantity), style = FieldLabelStyle)
        val note = AppText.copy(fontSize = 14.sp, lineHeight = 19.sp, color = AppColors.TextMuted)
        if (editor.isFallback) {
            Box(
                Modifier.fillMaxWidth().height(54.dp).background(AppColors.Background, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(stringResource(R.string.qty_unlimited_fixed), style = AppText.copy(fontSize = 17.sp, color = AppColors.TextField))
            }
            Text(stringResource(R.string.fallback_qty_note), style = note)
            return@Column
        }
        Segmented(
            options = listOf(stringResource(R.string.qty_unlimited), stringResource(R.string.qty_limited)),
            selected = if (editor.unlimited) 0 else 1,
            onSelect = { i -> vm.updateEditor { it.copy(unlimited = i == 0) } },
            fill = true,
            fontSize = 15.sp,
            modifier = Modifier.fillMaxWidth(),
        )
        if (!editor.unlimited) {
            val error = EditorField.QUANTITY in editor.errors
            val current = editor.quantityText.trim().toIntOrNull() ?: 0
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StepButton("−", stringResource(R.string.cd_qty_minus)) {
                    vm.updateEditor { it.copy(quantityText = (current - 1).coerceAtLeast(0).toString()) }
                }
                EditorField(
                    value = editor.quantityText,
                    onValueChange = { v -> vm.updateEditor { it.copy(quantityText = v.filter(Char::isDigit).take(6)) } },
                    error = error,
                    keyboardType = KeyboardType.Number,
                    textStyle = InputStyle.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                    modifier = Modifier.weight(1f).testTag(EditorTags.QUANTITY),
                )
                StepButton("+", stringResource(R.string.cd_qty_plus)) {
                    vm.updateEditor { it.copy(quantityText = (current + 1).toString()) }
                }
            }
            val text = when {
                error -> stringResource(R.string.err_qty)
                editor.quantityText.trim() == "0" -> stringResource(R.string.qty_zero_note)
                else -> stringResource(R.string.qty_note)
            }
            Text(text, style = note.copy(color = if (error) AppColors.Error else AppColors.TextMuted))
        }
    }
}

private val InputStyle = AppText.copy(fontSize = 19.sp, color = AppColors.Text)

@Composable
private fun EditorField(
    value: String,
    onValueChange: (String) -> Unit,
    error: Boolean,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    suffix: String? = null,
    textStyle: TextStyle = InputStyle,
    height: Dp = 54.dp,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(AppColors.Primary),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier,
        decorationBox = { inner ->
            Row(
                Modifier
                    .height(height)
                    .border(1.5.dp, if (error) AppColors.Error else AppColors.InputBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(placeholder, style = textStyle.copy(color = AppColors.TextLabel))
                    }
                    inner()
                }
                if (suffix != null) Text(suffix, style = AppText.copy(fontSize = 19.sp, color = AppColors.TextLabel))
            }
        },
    )
}

@Composable
private fun StepButton(label: String, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(54.dp)
            .border(1.5.dp, AppColors.InputBorder, RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = AppText.copy(fontSize = 24.sp, color = AppColors.Text))
    }
}

@Composable
private fun ErrorLine(text: String) {
    Text(text, style = AppText.copy(fontSize = 14.sp, lineHeight = 19.sp, color = AppColors.Error), modifier = Modifier.heightIn(min = 18.dp))
}

@Composable
fun ColorSwatch(color: Int, selected: Boolean, onClick: () -> Unit) {
    val ring = if (selected) {
        Modifier
            .border(5.dp, AppColors.Primary, CircleShape)
            .padding(2.dp)
            .border(3.dp, Color.White, CircleShape)
    } else {
        Modifier
    }
    Box(
        Modifier
            .size(40.dp)
            .then(ring)
            .background(Color(color), CircleShape)
            .border(1.dp, Color.Black.copy(alpha = 0.15f), CircleShape)
            .clickable(role = Role.RadioButton, onClick = onClick),
    )
}
