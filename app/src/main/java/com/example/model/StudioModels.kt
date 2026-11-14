package com.example.model

import com.squareup.moshi.JsonClass

enum class ProjectType {
    SCREENSHOT,
    TEXT_CARD,
    EMOJI_ART
}

enum class TransformMode {
    PRESERVE_LAYOUT,
    COMPLETE_REDESIGN
}

enum class StudioFont(val displayName: String, val supportsUrdu: Boolean) {
    INTER("SF Sans (Inter)", false),
    PLUS_JAKARTA("SF Display (Jakarta)", false),
    NOTO_NASTALIQ_URDU("Noto Nastaliq Urdu", true),
    NOTO_NASKH_ARABIC("Noto Naskh Urdu", true),
    SYSTEM_UI("System UI", true)
}

enum class TextDirectionMode(val label: String) {
    AUTO("Auto"),
    LTR("English LTR"),
    RTL("Urdu RTL")
}

enum class TextAlignMode(val label: String) {
    START("Start"),
    CENTER("Center"),
    END("End")
}

enum class TextFillType {
    SOLID,
    GRADIENT
}

enum class BackgroundType {
    SOLID,
    GRADIENT,
    TRANSPARENT,
    IMAGE
}

enum class ElementKind {
    TEXT,
    CARD,
    EMOJI,
    BADGE
}

enum class ExportFormat(val extension: String, val mimeType: String) {
    PNG("png", "image/png"),
    JPG("jpg", "image/jpeg"),
    WEBP("webp", "image/webp")
}

enum class ExportScale(val label: String, val multiplier: Float) {
    X1("Original (1x)", 1f),
    X2("2x Retina", 2f),
    X3("3x Ultra HD", 3f),
    CUSTOM("Custom", 1f)
}

enum class ComparisonMode(val label: String) {
    TRANSFORMED("iOSified"),
    ORIGINAL("Original"),
    SIDE_BY_SIDE("Side-by-Side"),
    SLIDER("Split Slider")
}

enum class ThemePreference(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class AppLanguage(val label: String) {
    AUTO("Auto Detect"),
    ENGLISH("English"),
    URDU("اردو (Urdu)")
}

enum class EmojiCategory(val label: String) {
    RECENT("Recent"),
    FAVORITES("Favorites"),
    CUSTOM("Custom Packs"),
    SMILEYS("Smileys & Emotions"),
    PEOPLE("People & Gestures"),
    ANIMALS("Animals & Nature"),
    FOOD("Food & Drinks"),
    TRAVEL("Travel & Places"),
    ACTIVITIES("Activities"),
    OBJECTS("Objects"),
    SYMBOLS("Symbols"),
    FLAGS("Flags")
}

@JsonClass(generateAdapter = true)
data class TextEffectConfig(
    val fillType: TextFillType = TextFillType.SOLID,
    val solidColor: Long = 0xFF1D1D1FL,
    val gradientColors: List<Long> = listOf(0xFF007AFFL, 0xFFAF52DEL),
    val gradientAngleDeg: Float = 45f,
    val outlineEnabled: Boolean = false,
    val outlineColor: Long = 0xFFFFFFFFL,
    val outlineWidthDp: Float = 2.5f,
    val shadowEnabled: Boolean = false,
    val shadowColor: Long = 0x55000000L,
    val shadowBlurRadius: Float = 8f,
    val shadowOffsetX: Float = 0f,
    val shadowOffsetY: Float = 4f
)

@JsonClass(generateAdapter = true)
data class CanvasBackgroundConfig(
    val type: BackgroundType = BackgroundType.GRADIENT,
    val solidColor: Long = 0xFFF5F5F7L,
    val gradientColors: List<Long> = listOf(0xFFEAF3FFL, 0xFFF5F5F7L, 0xFFF3EBFFL),
    val gradientAngleDeg: Float = 135f,
    val imagePath: String? = null
)

@JsonClass(generateAdapter = true)
data class CanvasElement(
    val id: String,
    val kind: ElementKind,
    val xFraction: Float,
    val yFraction: Float,
    val widthFraction: Float,
    val heightFraction: Float,
    val text: String = "",
    val font: StudioFont = StudioFont.INTER,
    val fontSizeSp: Float = 20f,
    val fontWeight: Int = 600,
    val letterSpacingEm: Float = 0f,
    val lineHeightMultiplier: Float = 1.35f,
    val textAlign: TextAlignMode = TextAlignMode.START,
    val directionMode: TextDirectionMode = TextDirectionMode.AUTO,
    val textEffect: TextEffectConfig = TextEffectConfig(),
    val cardBackgroundColor: Long = 0xFFFFFFFFL,
    val cardCornerRadiusDp: Float = 22f,
    val cardShadowElevationDp: Float = 10f,
    val cardPaddingDp: Float = 18f,
    val cardBorderWidthDp: Float = 0.8f,
    val cardBorderColor: Long = 0x33D2D2D7L,
    val emojiChar: String = "",
    val zIndex: Int = 0
)

@JsonClass(generateAdapter = true)
data class CanvasDocument(
    val canvasWidth: Int = 1080,
    val canvasHeight: Int = 1350,
    val background: CanvasBackgroundConfig = CanvasBackgroundConfig(),
    val elements: List<CanvasElement> = emptyList(),
    val sourceImagePath: String? = null,
    val aiBackdropPath: String? = null,
    val transformMode: TransformMode = TransformMode.PRESERVE_LAYOUT,
    val designSummary: String = ""
)

data class TextTemplatePreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val sampleText: String,
    val isUrdu: Boolean,
    val document: CanvasDocument
)

data class EmojiCatalogItem(
    val char: String,
    val name: String,
    val category: EmojiCategory,
    val keywords: List<String> = emptyList()
)

data class UserSettings(
    val themeMode: ThemePreference = ThemePreference.SYSTEM,
    val appLanguage: AppLanguage = AppLanguage.AUTO,
    val defaultEnglishFont: StudioFont = StudioFont.INTER,
    val defaultUrduFont: StudioFont = StudioFont.NOTO_NASTALIQ_URDU,
    val defaultEmojiPack: String = "Default iOSify",
    val defaultExportFormat: ExportFormat = ExportFormat.PNG,
    val exportQuality: Int = 95,
    val hasConsentedToAiUpload: Boolean = false
)
