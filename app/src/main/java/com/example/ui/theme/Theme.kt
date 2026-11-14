package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val IosifyLightColorScheme = lightColorScheme(
    primary = IosBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5F1FF),
    onPrimaryContainer = Color(0xFF003B80),
    secondary = IosPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E8FF),
    onSecondaryContainer = Color(0xFF4A148C),
    tertiary = IosGreen,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightPrimaryText,
    surface = LightSurface,
    onSurface = LightPrimaryText,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightSecondaryText,
    outline = LightSeparator,
    outlineVariant = Color(0xFFE5E5EA),
    error = IosRed,
    onError = Color.White
)

private val IosifyDarkColorScheme = darkColorScheme(
    primary = IosBlueDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0A3266),
    onPrimaryContainer = Color(0xFFD1E6FF),
    secondary = IosPurpleDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF3A1B54),
    onSecondaryContainer = Color(0xFFF2DAFF),
    tertiary = IosGreenDark,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = DarkPrimaryText,
    surface = DarkSurface,
    onSurface = DarkPrimaryText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkSecondaryText,
    outline = DarkSeparator,
    outlineVariant = Color(0xFF2C2C2E),
    error = IosRedDark,
    onError = Color.White
)

val IosifyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun IosifyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) IosifyDarkColorScheme else IosifyLightColorScheme
    val extended = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalIosifyColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = IosifyTypography,
            shapes = IosifyShapes,
            content = content
        )
    }
}
