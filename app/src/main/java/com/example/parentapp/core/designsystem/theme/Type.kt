package com.example.parentapp.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * Material 3 type scale. Uses the platform default font family (no bundled/downloadable
 * font dependency) but tightens weights and line-height alignment for a modern feel.
 */
private val defaultFontFamily = FontFamily.Default

private val lineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

val ParentsAppTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontFamily = defaultFontFamily, fontWeight = FontWeight.Normal, lineHeightStyle = lineHeightStyle),
        headlineLarge = headlineLarge.copy(fontFamily = defaultFontFamily, fontWeight = FontWeight.SemiBold, lineHeightStyle = lineHeightStyle),
        headlineMedium = headlineMedium.copy(fontFamily = defaultFontFamily, fontWeight = FontWeight.SemiBold, lineHeightStyle = lineHeightStyle),
        headlineSmall = headlineSmall.copy(fontFamily = defaultFontFamily, fontWeight = FontWeight.SemiBold, lineHeightStyle = lineHeightStyle),
        titleLarge = titleLarge.copy(fontFamily = defaultFontFamily, fontWeight = FontWeight.SemiBold, lineHeightStyle = lineHeightStyle),
        titleMedium = titleMedium.copy(fontFamily = defaultFontFamily, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp, lineHeightStyle = lineHeightStyle),
        titleSmall = titleSmall.copy(fontFamily = defaultFontFamily, fontWeight = FontWeight.Medium, lineHeightStyle = lineHeightStyle),
        bodyLarge = bodyLarge.copy(fontFamily = defaultFontFamily, lineHeightStyle = lineHeightStyle),
        bodyMedium = bodyMedium.copy(fontFamily = defaultFontFamily, lineHeightStyle = lineHeightStyle),
        bodySmall = bodySmall.copy(fontFamily = defaultFontFamily, lineHeightStyle = lineHeightStyle),
        labelLarge = labelLarge.copy(fontFamily = defaultFontFamily, fontWeight = FontWeight.Medium),
        labelMedium = labelMedium.copy(fontFamily = defaultFontFamily, fontWeight = FontWeight.Medium),
        labelSmall = labelSmall.copy(fontFamily = defaultFontFamily, fontWeight = FontWeight.Medium),
    )
}

/** A slightly heavier style for numeric emphasis in stat tiles / reports. */
val StatNumberTextStyle = TextStyle(
    fontFamily = defaultFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp,
    lineHeight = 34.sp,
)
