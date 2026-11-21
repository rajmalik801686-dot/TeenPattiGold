package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val RoyalDarkColorScheme = darkColorScheme(
    primary = RoyalGold,
    onPrimary = ObsidianNight,
    primaryContainer = RoyalGoldDeep,
    onPrimaryContainer = ObsidianNight,
    secondary = EmeraldFeltLight,
    onSecondary = TextPrimary,
    secondaryContainer = EmeraldFeltMid,
    onSecondaryContainer = RoyalGoldBright,
    tertiary = AmberBlind,
    onTertiary = ObsidianNight,
    background = ObsidianNight,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianElevated,
    onSurfaceVariant = TextSecondary,
    outline = DividerGold,
    error = CrimsonFold,
    onError = TextPrimary
)

val TeenPattiShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun TeenPattiGoldTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RoyalDarkColorScheme,
        typography = TeenPattiTypography,
        shapes = TeenPattiShapes,
        content = content
    )
}
