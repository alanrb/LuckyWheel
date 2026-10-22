package io.github.alanrb.luckywheel.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.alanrb.luckywheel.R
import io.github.alanrb.luckywheel.domain.ColorMode
import io.github.alanrb.luckywheel.domain.MAX_PRIZES
import io.github.alanrb.luckywheel.domain.Prize
import io.github.alanrb.luckywheel.domain.PrizeListIssue
import io.github.alanrb.luckywheel.domain.TOTAL_BP
import io.github.alanrb.luckywheel.domain.assignSliceColors
import io.github.alanrb.luckywheel.domain.effectiveWheel
import io.github.alanrb.luckywheel.domain.formatPercent
import io.github.alanrb.luckywheel.domain.formatPercentFixed
import io.github.alanrb.luckywheel.ui.theme.AppColors
import io.github.alanrb.luckywheel.ui.theme.AppText
import io.github.alanrb.luckywheel.ui.themes.ThemeSpec
import io.github.alanrb.luckywheel.ui.wheel.WheelCanvas
import io.github.alanrb.luckywheel.ui.wheel.WheelSlice

object PrizeTags {
    const val ADD = "addPrize"
    const val SAVE = "saveDraft"
    fun row(name: String) = "prizeRow_$name"
}

private val Tabular = AppText.copy(fontFeatureSettings = "tnum")

/** Column widths; narrower on phones so the name column keeps its room. */
private data class TableMetrics(val padding: Dp, val gap: Dp, val handle: Dp, val swatch: Dp, val percent: Dp, val remaining: Dp)

private val WideTable = TableMetrics(padding = 20.dp, gap = 12.dp, handle = 24.dp, swatch = 28.dp, percent = 96.dp, remaining = 120.dp)
private val CompactTable = TableMetrics(padding = 12.dp, gap = 8.dp, handle = 20.dp, swatch = 24.dp, percent = 60.dp, remaining = 76.dp)
private val HiddenSwatch = Color(0xFFE4DED6)

/** PR-01..PR-07: the prize list with live total, preview wheel and save. */
@Composable
fun PrizesSection(state: SettingsUiState, theme: ThemeSpec, vm: SettingsViewModel) {
    val preview = remember(state.draft, theme) {
        val wheel = effectiveWheel(state.draft)
        val colors = assignSliceColors(wheel.outcomes, theme.palette)
        wheel.outcomes.mapIndexed { i, o -> WheelSlice(o.prize.id, o.prize.name, colors[i], o.prize.isFallback) }
    }
    val colorOf = preview.associate { it.prizeId to it.color }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.weight(1f)) {
                SectionTitle(
                    stringResource(R.string.section_prizes),
                    stringResource(R.string.prize_count, state.draft.size, MAX_PRIZES) + " · " + stringResource(R.string.reorder_hint),
                )
            }
            PillButton(
                stringResource(R.string.add_prize),
                onClick = { vm.openEditor(null) },
                kind = PillKind.BRAND_OUTLINED,
                enabled = state.canAdd,
                height = 48.dp,
                fontSize = 17.sp,
                modifier = Modifier.testTag(PrizeTags.ADD),
            )
        }
        BoxWithConstraints {
            if (maxWidth >= 760.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.Top) {
                    PrizeTable(state.draft, colorOf, vm, Modifier.weight(1f))
                    Column(Modifier.width(300.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        PreviewCard(preview, theme)
                        TotalCard(state, vm)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    PrizeTable(state.draft, colorOf, vm, Modifier.fillMaxWidth())
                    TotalCard(state, vm)
                    PreviewCard(preview, theme)
                }
            }
        }
    }
}

@Composable
private fun PrizeTable(draft: List<Prize>, colorOf: Map<Long, Int>, vm: SettingsViewModel, modifier: Modifier) {
    val current by rememberUpdatedState(draft)
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var rowHeight by remember { mutableFloatStateOf(1f) }

    BoxWithConstraints(modifier) {
    val m = if (maxWidth < 560.dp) CompactTable else WideTable
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(20.dp)),
    ) {
        Row(
            Modifier.padding(horizontal = m.padding, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(m.gap),
        ) {
            val header = AppText.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.06.em, color = AppColors.TextLabel)
            Spacer(Modifier.width(m.handle))
            Spacer(Modifier.width(m.swatch))
            Text(stringResource(R.string.col_name).uppercase(), style = header, modifier = Modifier.weight(1f), maxLines = 1)
            Text(stringResource(R.string.col_percent).uppercase(), style = header.copy(textAlign = TextAlign.End), modifier = Modifier.width(m.percent), maxLines = 1)
            Text(stringResource(R.string.col_remaining).uppercase(), style = header.copy(textAlign = TextAlign.End), modifier = Modifier.width(m.remaining), maxLines = 1)
        }
        draft.forEach { prize ->
            key(prize.id) {
                val dragging = draggingId == prize.id
                val handle = Modifier.pointerInput(prize.id) {
                    detectDragGestures(
                        onDragStart = {
                            draggingId = prize.id
                            dragOffset = 0f
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            dragOffset += amount.y
                            val list = current
                            val from = list.indexOfFirst { it.id == prize.id }
                            if (dragOffset > rowHeight / 2 && from < list.lastIndex) {
                                vm.move(from, from + 1)
                                dragOffset -= rowHeight
                            } else if (dragOffset < -rowHeight / 2 && from > 0) {
                                vm.move(from, from - 1)
                                dragOffset += rowHeight
                            }
                        },
                        onDragEnd = { draggingId = null; dragOffset = 0f },
                        onDragCancel = { draggingId = null; dragOffset = 0f },
                    )
                }
                PrizeRow(
                    prize = prize,
                    metrics = m,
                    swatch = colorOf[prize.id]?.let(::Color)
                        ?: if (prize.colorMode == ColorMode.CUSTOM && prize.customColor != null) Color(prize.customColor) else HiddenSwatch,
                    handleModifier = handle,
                    onClick = { vm.openEditor(prize.id) },
                    modifier = Modifier
                        .zIndex(if (dragging) 1f else 0f)
                        .onSizeChanged { if (!dragging) rowHeight = it.height.toFloat() }
                        .graphicsLayer {
                            translationY = if (dragging) dragOffset else 0f
                            shadowElevation = if (dragging) 12f else 0f
                        },
                )
            }
        }
    }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrizeRow(
    prize: Prize,
    metrics: TableMetrics,
    swatch: Color,
    handleModifier: Modifier,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val outOfStock = prize.isOutOfStock
    val handleDescription = stringResource(R.string.cd_drag_handle)
    Column(modifier.background(AppColors.Surface)) {
        HorizontalDivider(color = AppColors.Divider, thickness = 1.dp)
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 70.dp)
                .clickable(onClick = onClick)
                .testTag(PrizeTags.row(prize.name))
                .padding(horizontal = metrics.padding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(metrics.gap),
        ) {
            Box(
                handleModifier.size(width = metrics.handle, height = 48.dp).semantics { contentDescription = handleDescription },
                contentAlignment = Alignment.Center,
            ) {
                Text("⠿", style = AppText.copy(fontSize = 22.sp, color = AppColors.Handle))
            }
            Box(Modifier.width(metrics.swatch)) {
                Box(
                    Modifier.size(metrics.swatch - 2.dp).background(swatch, CircleShape)
                        .border(1.dp, Color.Black.copy(alpha = 0.12f), CircleShape),
                )
            }
            Column(Modifier.weight(1f).padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    prize.name,
                    style = AppText.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Text),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (prize.isFallback) Badge(stringResource(R.string.badge_fallback), AppColors.BadgeFallback, AppColors.Primary)
                    if (outOfStock) Badge(stringResource(R.string.badge_out), AppColors.BadgeOut, AppColors.Error)
                    if (prize.probabilityBp == 0) Badge(stringResource(R.string.badge_hidden), AppColors.BadgeNeutral, AppColors.TextNeutral, bold = false)
                    if (prize.colorMode == ColorMode.CUSTOM) Badge(stringResource(R.string.badge_custom_color), AppColors.BadgeNeutral, AppColors.TextNeutral, bold = false)
                }
            }
            Text(
                formatPercent(prize.probabilityBp),
                style = Tabular.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.Text, textAlign = TextAlign.End),
                modifier = Modifier.width(metrics.percent),
                maxLines = 1,
            )
            Text(
                prize.quantity?.let { stringResource(R.string.remaining_count, it) } ?: stringResource(R.string.remaining_unlimited),
                style = Tabular.copy(fontSize = 17.sp, color = if (outOfStock) AppColors.Error else AppColors.TextNeutral, textAlign = TextAlign.End),
                modifier = Modifier.width(metrics.remaining),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PreviewCard(slices: List<WheelSlice>, theme: ThemeSpec) {
    SettingsCard(
        padding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        CardLabel(stringResource(R.string.preview_title))
        WheelCanvas(slices, theme, rotation = { 0f }, modifier = Modifier.size(240.dp).align(Alignment.CenterHorizontally))
        Text(
            stringResource(R.string.preview_note),
            style = AppText.copy(fontSize = 14.sp, color = AppColors.TextMuted, textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TotalCard(state: SettingsUiState, vm: SettingsViewModel) {
    SettingsCard(padding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        CardLabel(stringResource(R.string.total_title))
        Text(
            formatPercentFixed(state.totalBp),
            style = Tabular.copy(
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (state.totalBp == TOTAL_BP) AppColors.Success else AppColors.Error,
            ),
        )
        val issueStyle = AppText.copy(fontSize = 15.sp, lineHeight = 21.sp, color = AppColors.Error)
        state.issues.forEach { issue ->
            val text = when (issue) {
                is PrizeListIssue.TotalShort -> stringResource(R.string.issue_short, formatPercentFixed(issue.missingBp))
                is PrizeListIssue.TotalOver -> stringResource(R.string.issue_over, formatPercentFixed(issue.excessBp))
                PrizeListIssue.TooManyPrizes -> stringResource(R.string.issue_too_many)
                PrizeListIssue.TooFewNonZero -> stringResource(R.string.issue_too_few)
            }
            Text(text, style = issueStyle)
        }
        if (state.issues.isEmpty()) {
            val ok = when {
                state.justSaved && !state.dirty -> R.string.saved_ok
                state.dirty -> R.string.valid_unsaved
                else -> R.string.valid_unchanged
            }
            Text(stringResource(ok), style = AppText.copy(fontSize = 15.sp, color = AppColors.Success))
        }
        PillButton(
            stringResource(R.string.save_changes),
            onClick = vm::save,
            enabled = state.canSave,
            height = 52.dp,
            fontSize = 18.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp).testTag(PrizeTags.SAVE),
        )
        if (state.dirty) {
            PillButton(
                stringResource(R.string.discard_changes),
                onClick = vm::discard,
                kind = PillKind.TEXT,
                height = 44.dp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
