package com.codingwithsalman.voicenotes.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun VoiceNotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) VnDarkColorScheme else VnLightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = VnTypography,
            content = content,
        )
    }
}

/** Accessor mirroring MaterialTheme.colorScheme for the extended slots. */
object VnTheme {
    val extended: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}
