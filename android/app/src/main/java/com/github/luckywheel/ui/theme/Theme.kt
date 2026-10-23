package com.github.luckywheel.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ColorScheme = lightColorScheme(
    primary = AppColors.Primary,
    onPrimary = AppColors.OnPrimary,
    secondary = AppColors.Primary,
    onSecondary = AppColors.OnPrimary,
    background = AppColors.Background,
    onBackground = AppColors.Text,
    surface = AppColors.Surface,
    onSurface = AppColors.Text,
    surfaceVariant = AppColors.Segment,
    onSurfaceVariant = AppColors.TextNeutral,
    outline = AppColors.InputBorder,
    error = AppColors.Error,
)

/** The app always uses the café design palette; the customer screen is themed separately per wheel theme. */
@Composable
fun LuckyWheelTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ColorScheme, typography = Typography, content = content)
}
