package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val SalimColorScheme = lightColorScheme(
    primary = AppleBlue,
    onPrimary = PureWhite,
    primaryContainer = AppleBlueLight,
    onPrimaryContainer = AppleBlueDark,
    secondary = TextPrimary,
    onSecondary = PureWhite,
    secondaryContainer = SurfaceLayer,
    onSecondaryContainer = TextPrimary,
    tertiary = SystemOrange,
    onTertiary = PureWhite,
    background = PureWhite,
    onBackground = TextPrimary,
    surface = PureWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceLayer,
    onSurfaceVariant = TextSecondary,
    outline = BorderGray,
    outlineVariant = SurfaceLayerSecondary,
    error = SystemRed,
    onError = PureWhite
)

val SalimShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun SalimTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SalimColorScheme,
        typography = Typography,
        shapes = SalimShapes,
        content = content
    )
}

// Kept for backward compatibility if any test references MyApplicationTheme
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    SalimTheme(content = content)
}

