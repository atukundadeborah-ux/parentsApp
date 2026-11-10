package com.example.parentapp.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * ParentsApp brand palette — a calm "care & trust" scheme built around a teal primary,
 * a muted blue secondary and a warm amber tertiary. Values are a Material 3 tonal set
 * (Material Theme Builder style) so every colour role is defined for light and dark.
 */

// ---- Light ----
internal val md_primary = Color(0xFF006A6A)
internal val md_onPrimary = Color(0xFFFFFFFF)
internal val md_primaryContainer = Color(0xFF9CF1F0)
internal val md_onPrimaryContainer = Color(0xFF002020)

internal val md_secondary = Color(0xFF4A6363)
internal val md_onSecondary = Color(0xFFFFFFFF)
internal val md_secondaryContainer = Color(0xFFCCE8E7)
internal val md_onSecondaryContainer = Color(0xFF051F1F)

internal val md_tertiary = Color(0xFF4B607C)
internal val md_onTertiary = Color(0xFFFFFFFF)
internal val md_tertiaryContainer = Color(0xFFD3E4FF)
internal val md_onTertiaryContainer = Color(0xFF041C35)

internal val md_error = Color(0xFFBA1A1A)
internal val md_onError = Color(0xFFFFFFFF)
internal val md_errorContainer = Color(0xFFFFDAD6)
internal val md_onErrorContainer = Color(0xFF410002)

internal val md_background = Color(0xFFF4FBFA)
internal val md_onBackground = Color(0xFF161D1D)
internal val md_surface = Color(0xFFF4FBFA)
internal val md_onSurface = Color(0xFF161D1D)
internal val md_surfaceVariant = Color(0xFFDAE5E3)
internal val md_onSurfaceVariant = Color(0xFF3F4948)
internal val md_outline = Color(0xFF6F7979)
internal val md_outlineVariant = Color(0xFFBEC9C8)
internal val md_surfaceTint = md_primary
internal val md_inverseSurface = Color(0xFF2B3231)
internal val md_inverseOnSurface = Color(0xFFECF2F1)
internal val md_inversePrimary = Color(0xFF4CDADA)
internal val md_scrim = Color(0xFF000000)
internal val md_surfaceDim = Color(0xFFD5DBDA)
internal val md_surfaceBright = Color(0xFFF4FBFA)
internal val md_surfaceContainerLowest = Color(0xFFFFFFFF)
internal val md_surfaceContainerLow = Color(0xFFEFF5F4)
internal val md_surfaceContainer = Color(0xFFE9EFEE)
internal val md_surfaceContainerHigh = Color(0xFFE3EAE9)
internal val md_surfaceContainerHighest = Color(0xFFDEE4E3)

// ---- Dark ----
internal val md_primary_dark = Color(0xFF4CDADA)
internal val md_onPrimary_dark = Color(0xFF003737)
internal val md_primaryContainer_dark = Color(0xFF004F4F)
internal val md_onPrimaryContainer_dark = Color(0xFF9CF1F0)

internal val md_secondary_dark = Color(0xFFB0CCCB)
internal val md_onSecondary_dark = Color(0xFF1B3535)
internal val md_secondaryContainer_dark = Color(0xFF324B4B)
internal val md_onSecondaryContainer_dark = Color(0xFFCCE8E7)

internal val md_tertiary_dark = Color(0xFFB3C8E8)
internal val md_onTertiary_dark = Color(0xFF1C314B)
internal val md_tertiaryContainer_dark = Color(0xFF334863)
internal val md_onTertiaryContainer_dark = Color(0xFFD3E4FF)

internal val md_error_dark = Color(0xFFFFB4AB)
internal val md_onError_dark = Color(0xFF690005)
internal val md_errorContainer_dark = Color(0xFF93000A)
internal val md_onErrorContainer_dark = Color(0xFFFFDAD6)

internal val md_background_dark = Color(0xFF0E1514)
internal val md_onBackground_dark = Color(0xFFDDE4E2)
internal val md_surface_dark = Color(0xFF0E1514)
internal val md_onSurface_dark = Color(0xFFDDE4E2)
internal val md_surfaceVariant_dark = Color(0xFF3F4948)
internal val md_onSurfaceVariant_dark = Color(0xFFBEC9C8)
internal val md_outline_dark = Color(0xFF889392)
internal val md_outlineVariant_dark = Color(0xFF3F4948)
internal val md_surfaceTint_dark = md_primary_dark
internal val md_inverseSurface_dark = Color(0xFFDDE4E2)
internal val md_inverseOnSurface_dark = Color(0xFF2B3231)
internal val md_inversePrimary_dark = Color(0xFF006A6A)
internal val md_scrim_dark = Color(0xFF000000)
internal val md_surfaceDim_dark = Color(0xFF0E1514)
internal val md_surfaceBright_dark = Color(0xFF343B3A)
internal val md_surfaceContainerLowest_dark = Color(0xFF090F0F)
internal val md_surfaceContainerLow_dark = Color(0xFF161D1D)
internal val md_surfaceContainer_dark = Color(0xFF1A2121)
internal val md_surfaceContainerHigh_dark = Color(0xFF252B2B)
internal val md_surfaceContainerHighest_dark = Color(0xFF303636)

/**
 * Semantic status colours used by [com.example.parentapp.core.designsystem.component.StatusBadge]
 * and reports. Exposed through [ParentsAppExtendedColors] / `LocalExtendedColors`.
 */
data class StatusColors(
    val container: Color,
    val onContainer: Color,
)

data class ParentsAppExtendedColors(
    val success: StatusColors,
    val warning: StatusColors,
    val danger: StatusColors,
    val info: StatusColors,
    val neutral: StatusColors,
)

internal val LightExtendedColors = ParentsAppExtendedColors(
    success = StatusColors(Color(0xFFB8F0C4), Color(0xFF00210E)),
    warning = StatusColors(Color(0xFFFFDEA6), Color(0xFF261900)),
    danger = StatusColors(Color(0xFFFFDAD6), Color(0xFF410002)),
    info = StatusColors(Color(0xFFD3E4FF), Color(0xFF041C35)),
    neutral = StatusColors(Color(0xFFDAE4E3), Color(0xFF191C1C)),
)

internal val DarkExtendedColors = ParentsAppExtendedColors(
    success = StatusColors(Color(0xFF1F4A2E), Color(0xFFB8F0C4)),
    warning = StatusColors(Color(0xFF4A3800), Color(0xFFFFDEA6)),
    danger = StatusColors(Color(0xFF93000A), Color(0xFFFFDAD6)),
    info = StatusColors(Color(0xFF334863), Color(0xFFD3E4FF)),
    neutral = StatusColors(Color(0xFF3F4948), Color(0xFFE0E3E2)),
)
