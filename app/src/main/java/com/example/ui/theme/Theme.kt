package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val TXLDarkColorScheme = darkColorScheme(
    primary = NeonPurple,
    onPrimary = TextPrimary,
    primaryContainer = NeonPurpleDeep,
    onPrimaryContainer = TextPrimary,
    secondary = NeonPink,
    onSecondary = DeepVoid,
    secondaryContainer = ColorTokens.PinkContainer,
    onSecondaryContainer = TextPrimary,
    tertiary = NeonCyan,
    onTertiary = DeepVoid,
    background = DeepVoid,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = DeepVoid,
    surfaceContainerLow = SurfaceDark,
    surfaceContainer = SurfaceCard,
    surfaceContainerHigh = SurfaceElevated,
    outline = BorderGlow,
    outlineVariant = BorderSubtle,
    error = DangerRose,
    onError = TextPrimary
)

private object ColorTokens {
    val PinkContainer = androidx.compose.ui.graphics.Color(0xFF4A153B)
}

val TXLShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun TXLsocialTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TXLDarkColorScheme,
        typography = TXLTypography,
        shapes = TXLShapes,
        content = content
    )
}
