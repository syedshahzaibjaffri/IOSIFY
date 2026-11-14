package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Light Palette (Apple Minimalist Silver & White)
val LightBackground = Color(0xFFF5F5F7)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEBEBF0)
val LightPrimaryText = Color(0xFF1D1D1F)
val LightSecondaryText = Color(0xFF86868B)
val LightSeparator = Color(0xFFD2D2D7)

// Dark Palette (OLED True Black & Elevated Charcoal)
val DarkBackground = Color(0xFF000000)
val DarkSurface = Color(0xFF1C1C1E)
val DarkSurfaceVariant = Color(0xFF2C2C2E)
val DarkPrimaryText = Color(0xFFFFFFFF)
val DarkSecondaryText = Color(0xFF98989D)
val DarkSeparator = Color(0xFF38383A)

// iOS System Semantic Accents
val IosBlue = Color(0xFF007AFF)
val IosBlueDark = Color(0xFF0A84FF)
val IosPurple = Color(0xFFAF52DE)
val IosPurpleDark = Color(0xFFBF5AF2)
val IosGreen = Color(0xFF34C759)
val IosGreenDark = Color(0xFF30D158)
val IosOrange = Color(0xFFFF9500)
val IosOrangeDark = Color(0xFFFF9F0A)
val IosRed = Color(0xFFFF3B30)
val IosRedDark = Color(0xFFFF453A)

@Immutable
data class IosifyExtendedColors(
    val purple: Color,
    val green: Color,
    val orange: Color,
    val red: Color,
    val separator: Color,
    val secondaryText: Color,
    val elevatedCard: Color,
    val canvasCheckeredLight: Color,
    val canvasCheckeredDark: Color
)

val LightExtendedColors = IosifyExtendedColors(
    purple = IosPurple,
    green = IosGreen,
    orange = IosOrange,
    red = IosRed,
    separator = LightSeparator,
    secondaryText = LightSecondaryText,
    elevatedCard = Color(0xFFFBFBFD),
    canvasCheckeredLight = Color(0xFFF0F0F5),
    canvasCheckeredDark = Color(0xFFE2E2EA)
)

val DarkExtendedColors = IosifyExtendedColors(
    purple = IosPurpleDark,
    green = IosGreenDark,
    orange = IosOrangeDark,
    red = IosRedDark,
    separator = DarkSeparator,
    secondaryText = DarkSecondaryText,
    elevatedCard = Color(0xFF242426),
    canvasCheckeredLight = Color(0xFF262629),
    canvasCheckeredDark = Color(0xFF1A1A1D)
)

val LocalIosifyColors = staticCompositionLocalOf { LightExtendedColors }
