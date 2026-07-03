package com.codingwithsalman.voicenotes.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * "Ink & Amber" — dark-first, content-first, one confident accent.
 * Amber is reserved for meaning (primary actions, active words, played audio);
 * coral is reserved for exactly one thing: recording.
 */
object VnColors {
    // Ink (dark) surfaces
    val Ink = Color(0xFF0D0E12)
    val InkSurface = Color(0xFF14161C)
    val InkSurfaceHigh = Color(0xFF1B1E26)
    val InkOutline = Color(0xFF2A2E38)
    val InkTextPrimary = Color(0xFFECEDEF)
    val InkTextSecondary = Color(0xFF9BA0AA)

    // Paper (light) surfaces
    val Paper = Color(0xFFFAF7F1)
    val PaperSurface = Color(0xFFFFFFFF)
    val PaperSurfaceHigh = Color(0xFFF1EDE4)
    val PaperOutline = Color(0xFFE5E0D6)
    val PaperTextPrimary = Color(0xFF1A1B20)
    val PaperTextSecondary = Color(0xFF5D6068)

    // Accents
    val Amber = Color(0xFFF5A524)
    val AmberDeep = Color(0xFFB97708)
    val Coral = Color(0xFFFF5252)
    val CoralWarm = Color(0xFFFF7A59)
    val ErrorRed = Color(0xFFE5484D)
}

/** Slots the M3 scheme doesn't have: recording + waveform semantics. */
data class ExtendedColors(
    val record: Color,
    val recordGradientEnd: Color,
    val waveActive: Color,
    /** Loud-moment end of the waveform ramp; contrast-tuned per theme. */
    val waveHot: Color,
    val waveIdle: Color,
    val cardStroke: Color,
    /** Cards float on paper (light) and sit flush on ink (dark). */
    val cardElevation: androidx.compose.ui.unit.Dp,
)

val DarkExtendedColors = ExtendedColors(
    record = VnColors.Coral,
    recordGradientEnd = VnColors.CoralWarm,
    waveActive = VnColors.Amber,
    waveHot = VnColors.CoralWarm,
    waveIdle = Color(0xFF3A3F4B),
    cardStroke = Color(0xFF232733),
    cardElevation = androidx.compose.ui.unit.Dp(0f),
)

val LightExtendedColors = ExtendedColors(
    record = VnColors.Coral,
    recordGradientEnd = VnColors.CoralWarm,
    waveActive = VnColors.AmberDeep,
    waveHot = Color(0xFFD9542E), // deep coral — holds contrast on paper
    waveIdle = Color(0xFFD8D2C6),
    cardStroke = Color(0xFFE9E3D7),
    cardElevation = androidx.compose.ui.unit.Dp(2f),
)

val LocalExtendedColors = staticCompositionLocalOf { DarkExtendedColors }

internal val VnDarkColorScheme = darkColorScheme(
    primary = VnColors.Amber,
    onPrimary = VnColors.Ink,
    primaryContainer = VnColors.InkSurfaceHigh,
    onPrimaryContainer = VnColors.Amber,
    secondary = VnColors.InkTextSecondary,
    onSecondary = VnColors.Ink,
    background = VnColors.Ink,
    onBackground = VnColors.InkTextPrimary,
    surface = VnColors.InkSurface,
    onSurface = VnColors.InkTextPrimary,
    surfaceVariant = VnColors.InkSurfaceHigh,
    onSurfaceVariant = VnColors.InkTextSecondary,
    outline = VnColors.InkOutline,
    error = VnColors.ErrorRed,
    onError = VnColors.InkTextPrimary,
)

internal val VnLightColorScheme = lightColorScheme(
    primary = VnColors.AmberDeep,
    onPrimary = VnColors.PaperSurface,
    primaryContainer = VnColors.PaperSurfaceHigh,
    onPrimaryContainer = VnColors.AmberDeep,
    secondary = VnColors.PaperTextSecondary,
    onSecondary = VnColors.PaperSurface,
    background = VnColors.Paper,
    onBackground = VnColors.PaperTextPrimary,
    surface = VnColors.PaperSurface,
    onSurface = VnColors.PaperTextPrimary,
    surfaceVariant = VnColors.PaperSurfaceHigh,
    onSurfaceVariant = VnColors.PaperTextSecondary,
    outline = VnColors.PaperOutline,
    error = VnColors.ErrorRed,
    onError = VnColors.PaperSurface,
)
