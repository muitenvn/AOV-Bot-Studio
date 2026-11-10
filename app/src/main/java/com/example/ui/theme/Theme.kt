package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TacticalDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = ObsidianNavy,
    primaryContainer = Color(0xFF003844),
    onPrimaryContainer = ElectricCyan,
    secondary = TacticalGold,
    onSecondary = ObsidianNavy,
    secondaryContainer = Color(0xFF3E2C00),
    onSecondaryContainer = AmberHighlight,
    tertiary = EmeraldSuccess,
    onTertiary = ObsidianNavy,
    error = CrimsonDanger,
    background = ObsidianNavy,
    onBackground = SoftIceWhite,
    surface = DeepSlateBlue,
    onSurface = SoftIceWhite,
    surfaceVariant = ElevatedTacticalCard,
    onSurfaceVariant = MutedSteelGray,
    outline = Color(0xFF2E3F63)
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TacticalDarkColorScheme,
        typography = Typography,
        content = content
    )
}
