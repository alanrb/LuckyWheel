package com.github.luckywheel.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.luckywheel.R
import com.github.luckywheel.domain.ThemeId
import com.github.luckywheel.ui.rememberStoredImage
import com.github.luckywheel.ui.theme.AppColors
import com.github.luckywheel.ui.theme.AppText
import com.github.luckywheel.ui.themes.ThemeSpec
import com.github.luckywheel.ui.themes.drawThemeBackground
import com.github.luckywheel.ui.themes.scrimColor
import com.github.luckywheel.ui.themes.themeSpec

fun themeCardTag(id: ThemeId) = "themeCard_${id.name}"

/** TH-01..TH-06, LG-01..LG-03: theme, logo, custom background and decorations. */
@Composable
fun AppearanceSection(state: SettingsUiState, vm: SettingsViewModel, pickerHooks: PickerHooks) {
    val settings = state.settings
    val theme = themeSpec(settings.themeId)
    val pickLogo = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        pickerHooks.onResult()
        uri?.let(vm::importLogo)
    }
    val pickBackground = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        pickerHooks.onResult()
        uri?.let(vm::importBackground)
    }
    val imagesOnly = PickVisualMediaRequest(PickVisualMedia.ImageOnly)

    Column(verticalArrangement = Arrangement.spacedBy(30.dp)) {
        SectionTitle(stringResource(R.string.section_appearance), stringResource(R.string.appearance_hint))

        BoxWithConstraints {
            val columns = if (maxWidth > 600.dp) 3 else 2
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ThemeId.entries.chunked(columns).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        row.forEach { id ->
                            ThemeCard(themeSpec(id), active = id == settings.themeId, onClick = { vm.setTheme(id) }, modifier = Modifier.weight(1f))
                        }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        val logo by rememberStoredImage(settings.logoPath)
        ImageCard(
            preview = {
                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))) {
                    Checkerboard()
                    val bitmap = logo
                    if (bitmap != null) {
                        Image(bitmap, null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(12.dp))
                    } else {
                        Text(
                            stringResource(R.string.logo_none),
                            style = AppText.copy(fontSize = 15.sp, color = AppColors.TextLabel),
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
            },
            previewHeight = 120,
            title = stringResource(R.string.logo_title),
            body = stringResource(R.string.logo_body),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton(
                    stringResource(if (settings.logoPath != null) R.string.logo_change else R.string.pick_image),
                    onClick = {
                        pickerHooks.onLaunch()
                        pickLogo.launch(imagesOnly)
                    },
                )
                if (settings.logoPath != null) {
                    PillButton(stringResource(R.string.logo_remove), onClick = vm::removeLogo, kind = PillKind.OUTLINED)
                }
            }
        }

        val background by rememberStoredImage(settings.customBackgroundPath)
        ImageCard(
            preview = {
                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))) {
                    val bitmap = background
                    if (bitmap != null) {
                        Image(bitmap, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        Box(Modifier.fillMaxSize().background(scrimColor(theme)))
                    } else {
                        Canvas(Modifier.fillMaxSize()) { drawThemeBackground(theme, unitPx = 0.3f * density, withCobwebs = true) }
                    }
                    Text(
                        stringResource(if (settings.customBackgroundPath != null) R.string.bg_label_custom else R.string.bg_label_theme),
                        style = AppText.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.Text),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                            .background(Color.White.copy(alpha = 0.88f), RoundedCornerShape(percent = 50))
                            .padding(horizontal = 9.dp, vertical = 3.dp),
                    )
                }
            },
            previewHeight = 136,
            title = stringResource(R.string.bg_title),
            body = stringResource(R.string.bg_body),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton(
                    stringResource(if (settings.customBackgroundPath != null) R.string.bg_change else R.string.pick_image),
                    onClick = {
                        pickerHooks.onLaunch()
                        pickBackground.launch(imagesOnly)
                    },
                )
                if (settings.customBackgroundPath != null) {
                    PillButton(stringResource(R.string.bg_remove), onClick = vm::removeBackground, kind = PillKind.OUTLINED)
                }
            }
            if (state.imageError) {
                Text(
                    stringResource(R.string.image_import_failed),
                    style = AppText.copy(fontSize = 15.sp, color = AppColors.Error),
                    modifier = Modifier.clickable(onClick = vm::dismissImageError),
                )
            }
        }

        ToggleCard(
            stringResource(R.string.deco_title),
            stringResource(R.string.deco_body),
            checked = settings.showThemeDecorations,
            onToggle = vm::setDecorations,
        )
    }
}

@Composable
private fun ThemeCard(theme: ThemeSpec, active: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .clip(shape)
            .background(AppColors.Surface)
            .border(if (active) 3.dp else 1.dp, if (active) AppColors.Primary else AppColors.Border, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .testTag(themeCardTag(theme.id)),
    ) {
        Box(Modifier.fillMaxWidth().height(128.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) { drawThemeBackground(theme, unitPx = 0.3f * density, withCobwebs = false) }
            MiniWheel(theme)
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(theme.nameRes),
                style = AppText.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AppColors.Text),
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            if (active) {
                Text(
                    stringResource(R.string.theme_active),
                    style = AppText.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppColors.OnPrimary),
                    modifier = Modifier.background(AppColors.Primary, RoundedCornerShape(percent = 50)).padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}

/** Thumbnail wheel: the theme's six palette colours, rim and hub. */
@Composable
private fun MiniWheel(theme: ThemeSpec) {
    Box(Modifier.size(92.dp).shadow(8.dp, CircleShape), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val seg = 360f / theme.palette.size
            theme.palette.forEachIndexed { i, c ->
                drawArc(Color(c), -90f + i * seg, seg, useCenter = true)
            }
            val rim = 5.dp.toPx()
            drawCircle(theme.rim, radius = size.minDimension / 2 - rim / 2, style = Stroke(rim))
            drawCircle(theme.hub, radius = 10.dp.toPx())
        }
    }
}

@Composable
private fun ImageCard(
    preview: @Composable () -> Unit,
    previewHeight: Int,
    title: String,
    body: String,
    actions: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints {
        val wide = maxWidth > 520.dp
        SettingsCard(modifier = Modifier.fillMaxWidth()) {
            val text: @Composable ColumnScope.() -> Unit = {
                Text(title, style = AppText.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColors.Text))
                Text(body, style = BodyStyle)
                actions()
            }
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(240.dp).height(previewHeight.dp)) { preview() }
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp), content = text)
                }
            } else {
                Box(Modifier.fillMaxWidth().height(previewHeight.dp)) { preview() }
                text()
            }
        }
    }
}

/** Transparency checkerboard behind the logo preview. */
@Composable
private fun Checkerboard() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(AppColors.Segment)
        val cell = 10.dp.toPx()
        var y = 0f
        var row = 0
        while (y < size.height) {
            var x = if (row % 2 == 0) 0f else cell
            while (x < size.width) {
                drawRect(AppColors.Checker, Offset(x, y), Size(cell, cell))
                x += cell * 2
            }
            y += cell
            row++
        }
    }
}
