// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The tokens behind documentation/DESIGN_SYSTEM.md. Lime is the one accent: it marks the single
 * primary action on a screen, what is selected, and what is live. Nothing else gets a colour of
 * its own.
 */
object MotoHubColors {
    val Lime = Color(0xFFC8F240)
    val Background = Color(0xFF0A0A0B)
    // Each step is about 1.16:1 above the one below - enough for a card to survive sunlight
    // without an outline - and TextSecondary still reads at 4.5:1 or better on all of them.
    val Surface = Color(0xFF1C1C1E)
    val SurfaceHigh = Color(0xFF2C2C2E)
    val SurfaceHighest = Color(0xFF363638)

    /**
     * 12% white, for controls that sit on a surface: secondary buttons, icon circles, text fields,
     * the off switch track, the grabber, neutral chips. Translucent on purpose: an opaque grey
     * vanishes on the one surface that happens to share its value (a secondary pill on a sheet was
     * 1:1), while this always reads one step lighter than whatever is under it.
     */
    val Fill = Color(0x1FFFFFFF)
    val TextPrimary = Color(0xFFF5F5F7)
    val TextSecondary = Color(0xFFA0A0A6)

    /**
     * Secondary text that sits on [Fill]: a field's label and placeholder, a neutral chip. Plain
     * TextSecondary drops to 3.6:1 on Fill over a sheet; this holds 4.6:1 there.
     */
    val TextSecondaryOnFill = Color(0xFFB6B6BC)

    /** Disabled and decorative text only - too faint for anything the rider has to read. */
    val TextTertiary = Color(0xFF6E6E74)
    /** Red for icons, dots and chips. Text uses [ErrorText]. */
    val Error = Color(0xFFFF5A52)

    /**
     * Red for text, and colorScheme.error. A destructive label sits on a Fill pill, and on a sheet
     * that pill is light enough that [Error] reads 3.1:1; this lighter red holds 4.6:1 there. It
     * is the same hue, so it still reads as red, as Material's own dark error colours do.
     */
    val ErrorText = Color(0xFFFF9994)
    val ErrorContainer = Color(0xFF2B1513)
    val Warning = Color(0xFFFFB340)
    val WarningContainer = Color(0xFF2B2111)
    val LimeContainer = Color(0xFF252A12)
}

// The per-feature colours the app used to paint its tiles with. Kept as names so the screens that
// still read them compile, but they now all resolve to the one neutral or the one accent: a list
// of options reads as a list, not a paint chart.
val MotoHubLive = MotoHubColors.Lime
val MotoHubMirror = MotoHubColors.TextPrimary
val MotoHubDashboard = MotoHubColors.TextPrimary
val MotoHubAndroidAuto = MotoHubColors.TextPrimary
val MotoHubImport = MotoHubColors.TextPrimary
val MotoHubManual = MotoHubColors.TextPrimary
val MotoHubFavorite = MotoHubColors.Warning

private val MotoHubColorScheme = darkColorScheme(
    primary = MotoHubColors.Lime,
    onPrimary = MotoHubColors.Background,
    primaryContainer = MotoHubColors.LimeContainer,
    onPrimaryContainer = MotoHubColors.Lime,
    secondary = MotoHubColors.TextSecondary,
    onSecondary = MotoHubColors.Background,
    secondaryContainer = MotoHubColors.SurfaceHighest,
    onSecondaryContainer = MotoHubColors.TextPrimary,
    tertiary = MotoHubColors.Lime,
    onTertiary = MotoHubColors.Background,
    background = MotoHubColors.Background,
    onBackground = MotoHubColors.TextPrimary,
    surface = MotoHubColors.Surface,
    onSurface = MotoHubColors.TextPrimary,
    surfaceVariant = MotoHubColors.SurfaceHigh,
    onSurfaceVariant = MotoHubColors.TextSecondary,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = MotoHubColors.Background,
    surfaceContainerLow = MotoHubColors.Surface,
    surfaceContainer = MotoHubColors.SurfaceHigh,
    surfaceContainerHigh = MotoHubColors.SurfaceHigh,
    surfaceContainerHighest = MotoHubColors.SurfaceHighest,
    inverseSurface = MotoHubColors.TextPrimary,
    inverseOnSurface = MotoHubColors.Background,
    inversePrimary = MotoHubColors.Lime,
    outline = MotoHubColors.SurfaceHighest,
    outlineVariant = MotoHubColors.SurfaceHigh,
    error = MotoHubColors.ErrorText,
    onError = MotoHubColors.Background,
    errorContainer = MotoHubColors.ErrorContainer,
    onErrorContainer = Color(0xFFFFB4AE),
    // 80%, not 60%: at 60 the lime action behind a sheet still read as an olive pill, a second
    // "do this" competing with the sheet's own.
    scrim = Color(0xCC000000)
)

private fun sans(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp
)

private val MotoHubTypography = Typography(
    displayLarge = sans(40, 46, FontWeight.Bold, -0.8),
    displayMedium = sans(34, 40, FontWeight.Bold, -0.6),
    // The screen title: 32, double the row text, the step Revolut's ladder has (28 was 1.75x).
    displaySmall = sans(32, 38, FontWeight.Bold, -0.5),
    headlineLarge = sans(26, 32, FontWeight.Bold, -0.3),
    headlineMedium = sans(22, 28, FontWeight.Bold, -0.2),
    headlineSmall = sans(20, 26, FontWeight.Bold, -0.1),
    titleLarge = sans(20, 26, FontWeight.SemiBold, -0.1),
    // Medium, not SemiBold: titles, headers and buttons all at 600 flattened the hierarchy, and
    // many OEM system fonts ship no 600, so it rendered as 500 on one phone and 700 on the next.
    titleMedium = sans(16, 22, FontWeight.Medium),
    titleSmall = sans(15, 20, FontWeight.SemiBold),
    bodyLarge = sans(16, 24, FontWeight.Normal),
    bodyMedium = sans(14, 20, FontWeight.Normal),
    bodySmall = sans(13, 18, FontWeight.Normal),
    labelLarge = sans(16, 20, FontWeight.SemiBold),
    labelMedium = sans(13, 16, FontWeight.Medium),
    labelSmall = sans(12, 16, FontWeight.Medium)
)

private val MotoHubShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MotoHubTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MotoHubColorScheme,
        typography = MotoHubTypography,
        shapes = MotoHubShapes,
        content = content
    )
}
