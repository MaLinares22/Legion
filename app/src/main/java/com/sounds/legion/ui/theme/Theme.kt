package com.sounds.legion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = LegionPurple,
    onPrimary = LegionBlack,
    primaryContainer = LegionPurpleSoft,
    onPrimaryContainer = LegionText,
    secondary = LegionNeonGreen,
    onSecondary = LegionBlack,
    tertiary = LegionElectricBlue,
    onTertiary = LegionBlack,
    background = LegionBlack,
    onBackground = LegionText,
    surface = LegionSurface,
    onSurface = LegionText,
    surfaceVariant = LegionSurfaceVariant,
    onSurfaceVariant = LegionMutedText,
    outline = LegionMutedText
)

@Composable
fun LegionTheme(content: @Composable () -> Unit) {
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}