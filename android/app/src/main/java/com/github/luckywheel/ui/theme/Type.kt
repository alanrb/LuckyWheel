package com.github.luckywheel.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.github.luckywheel.R

/** Be Vietnam Pro (SIL OFL 1.1) renders Vietnamese diacritics in every weight the design uses. */
val BeVietnamPro = FontFamily(
    Font(R.font.be_vietnam_pro_regular, FontWeight.Normal),
    Font(R.font.be_vietnam_pro_medium, FontWeight.Medium),
    Font(R.font.be_vietnam_pro_semibold, FontWeight.SemiBold),
    Font(R.font.be_vietnam_pro_bold, FontWeight.Bold),
    Font(R.font.be_vietnam_pro_extrabold, FontWeight.ExtraBold),
    Font(R.font.be_vietnam_pro_black, FontWeight.Black),
)

private val Base = Typography()

val Typography = Typography(
    displayLarge = Base.displayLarge.copy(fontFamily = BeVietnamPro),
    displayMedium = Base.displayMedium.copy(fontFamily = BeVietnamPro),
    displaySmall = Base.displaySmall.copy(fontFamily = BeVietnamPro),
    headlineLarge = Base.headlineLarge.copy(fontFamily = BeVietnamPro),
    headlineMedium = Base.headlineMedium.copy(fontFamily = BeVietnamPro),
    headlineSmall = Base.headlineSmall.copy(fontFamily = BeVietnamPro),
    titleLarge = Base.titleLarge.copy(fontFamily = BeVietnamPro),
    titleMedium = Base.titleMedium.copy(fontFamily = BeVietnamPro),
    titleSmall = Base.titleSmall.copy(fontFamily = BeVietnamPro),
    bodyLarge = Base.bodyLarge.copy(fontFamily = BeVietnamPro),
    bodyMedium = Base.bodyMedium.copy(fontFamily = BeVietnamPro),
    bodySmall = Base.bodySmall.copy(fontFamily = BeVietnamPro),
    labelLarge = Base.labelLarge.copy(fontFamily = BeVietnamPro),
    labelMedium = Base.labelMedium.copy(fontFamily = BeVietnamPro),
    labelSmall = Base.labelSmall.copy(fontFamily = BeVietnamPro),
)

/** Plain style in the app font, for text sized directly from the design. */
val AppText = TextStyle(fontFamily = BeVietnamPro)
