package com.example.data.repository

import com.example.model.BackgroundType
import com.example.model.CanvasBackgroundConfig
import com.example.model.CanvasDocument
import com.example.model.CanvasElement
import com.example.model.ElementKind
import com.example.model.StudioFont
import com.example.model.TextAlignMode
import com.example.model.TextDirectionMode
import com.example.model.TextEffectConfig
import com.example.model.TextFillType
import com.example.model.TextTemplatePreset
import com.example.model.TransformMode

object TemplateLibrary {

    val presets: List<TextTemplatePreset> = listOf(
        TextTemplatePreset(
            id = "messages",
            title = "Messages",
            subtitle = "Crisp iMessage-style bubble card",
            sampleText = "Assalam-o-Alaikum! ✨ Just finished styling this in iOSify.",
            isUrdu = false,
            document = CanvasDocument(
                canvasWidth = 1080,
                canvasHeight = 1080,
                background = CanvasBackgroundConfig(
                    type = BackgroundType.GRADIENT,
                    gradientColors = listOf(0xFFEAF3FFL, 0xFFF5F5F7L),
                    gradientAngleDeg = 135f
                ),
                elements = listOf(
                    CanvasElement(
                        id = "msg_bubble",
                        kind = ElementKind.CARD,
                        xFraction = 0.1f,
                        yFraction = 0.28f,
                        widthFraction = 0.8f,
                        heightFraction = 0.34f,
                        text = "Assalam-o-Alaikum! ✨\nJust styled this message with crisp iOS typography.",
                        font = StudioFont.INTER,
                        fontSizeSp = 24f,
                        fontWeight = 500,
                        textAlign = TextAlignMode.START,
                        directionMode = TextDirectionMode.AUTO,
                        textEffect = TextEffectConfig(
                            fillType = TextFillType.SOLID,
                            solidColor = 0xFFFFFFFFL
                        ),
                        cardBackgroundColor = 0xFF007AFFL,
                        cardCornerRadiusDp = 28f,
                        cardShadowElevationDp = 14f,
                        cardPaddingDp = 24f,
                        cardBorderWidthDp = 0f,
                        zIndex = 1
                    ),
                    CanvasElement(
                        id = "msg_status",
                        kind = ElementKind.TEXT,
                        xFraction = 0.52f,
                        yFraction = 0.64f,
                        widthFraction = 0.38f,
                        heightFraction = 0.08f,
                        text = "Delivered • 9:41 AM",
                        font = StudioFont.INTER,
                        fontSizeSp = 14f,
                        fontWeight = 600,
                        textAlign = TextAlignMode.END,
                        textEffect = TextEffectConfig(solidColor = 0xFF86868BL),
                        cardBackgroundColor = 0x00000000L,
                        zIndex = 2
                    )
                ),
                transformMode = TransformMode.COMPLETE_REDESIGN
            )
        ),
        TextTemplatePreset(
            id = "quotes_urdu",
            title = "Quotes",
            subtitle = "Urdu Nastaliq quote with gradient & shadow",
            sampleText = "خودی کو کر بلند اتنا کہ ہر تقدیر سے پہلے\nخدا بندے سے خود پوچھے بتا تیری رضا کیا ہے ✨",
            isUrdu = true,
            document = CanvasDocument(
                canvasWidth = 1080,
                canvasHeight = 1350,
                background = CanvasBackgroundConfig(
                    type = BackgroundType.GRADIENT,
                    gradientColors = listOf(0xFF0A1931L, 0xFF1C1C1EL, 0xFF2D1B4EL),
                    gradientAngleDeg = 140f
                ),
                elements = listOf(
                    CanvasElement(
                        id = "quote_card",
                        kind = ElementKind.CARD,
                        xFraction = 0.08f,
                        yFraction = 0.2f,
                        widthFraction = 0.84f,
                        heightFraction = 0.56f,
                        text = "خودی کو کر بلند اتنا کہ ہر تقدیر سے پہلے\nخدا بندے سے خود پوچھے بتا تیری رضا کیا ہے ✨",
                        font = StudioFont.NOTO_NASTALIQ_URDU,
                        fontSizeSp = 28f,
                        fontWeight = 700,
                        lineHeightMultiplier = 1.9f,
                        textAlign = TextAlignMode.CENTER,
                        directionMode = TextDirectionMode.RTL,
                        textEffect = TextEffectConfig(
                            fillType = TextFillType.GRADIENT,
                            solidColor = 0xFFFFFFFFL,
                            gradientColors = listOf(0xFFFFFFFFL, 0xFF8EC5FCL, 0xFFE0C3FCL),
                            gradientAngleDeg = 35f,
                            outlineEnabled = true,
                            outlineColor = 0x55000000L,
                            outlineWidthDp = 1.2f,
                            shadowEnabled = true,
                            shadowColor = 0x99000000L,
                            shadowBlurRadius = 12f,
                            shadowOffsetY = 6f
                        ),
                        cardBackgroundColor = 0xCC1C1C1EL,
                        cardCornerRadiusDp = 30f,
                        cardShadowElevationDp = 18f,
                        cardPaddingDp = 26f,
                        cardBorderWidthDp = 1.2f,
                        cardBorderColor = 0x44FFFFFFL,
                        zIndex = 1
                    ),
                    CanvasElement(
                        id = "quote_author",
                        kind = ElementKind.TEXT,
                        xFraction = 0.16f,
                        yFraction = 0.78f,
                        widthFraction = 0.68f,
                        heightFraction = 0.08f,
                        text = "Allama Iqbal • علامہ اقبال",
                        font = StudioFont.NOTO_NASKH_ARABIC,
                        fontSizeSp = 17f,
                        fontWeight = 600,
                        textAlign = TextAlignMode.CENTER,
                        directionMode = TextDirectionMode.AUTO,
                        textEffect = TextEffectConfig(solidColor = 0xFFD2D2D7L),
                        cardBackgroundColor = 0x00000000L,
                        zIndex = 2
                    )
                )
            )
        ),
        TextTemplatePreset(
            id = "captions",
            title = "Social Media Captions",
            subtitle = "Clean bilingual feed & reel card",
            sampleText = "Minimal aesthetics, maximum clarity. 🌿\nسادگی میں ہی اصل خوبصورتی ہے",
            isUrdu = true,
            document = CanvasDocument(
                canvasWidth = 1080,
                canvasHeight = 1350,
                background = CanvasBackgroundConfig(
                    type = BackgroundType.GRADIENT,
                    gradientColors = listOf(0xFFF5F5F7L, 0xFFE8F0FFL),
                    gradientAngleDeg = 120f
                ),
                elements = listOf(
                    CanvasElement(
                        id = "cap_en",
                        kind = ElementKind.CARD,
                        xFraction = 0.08f,
                        yFraction = 0.22f,
                        widthFraction = 0.84f,
                        heightFraction = 0.26f,
                        text = "Minimal aesthetics, maximum clarity. 🌿",
                        font = StudioFont.PLUS_JAKARTA,
                        fontSizeSp = 27f,
                        fontWeight = 700,
                        textAlign = TextAlignMode.CENTER,
                        directionMode = TextDirectionMode.LTR,
                        textEffect = TextEffectConfig(
                            fillType = TextFillType.GRADIENT,
                            gradientColors = listOf(0xFF007AFFL, 0xFFAF52DEL),
                            gradientAngleDeg = 20f
                        ),
                        cardBackgroundColor = 0xFFFFFFFFL,
                        cardCornerRadiusDp = 26f,
                        cardShadowElevationDp = 12f,
                        cardPaddingDp = 22f,
                        zIndex = 1
                    ),
                    CanvasElement(
                        id = "cap_ur",
                        kind = ElementKind.CARD,
                        xFraction = 0.08f,
                        yFraction = 0.52f,
                        widthFraction = 0.84f,
                        heightFraction = 0.26f,
                        text = "سادگی میں ہی اصل خوبصورتی ہے ✨",
                        font = StudioFont.NOTO_NASTALIQ_URDU,
                        fontSizeSp = 26f,
                        fontWeight = 600,
                        lineHeightMultiplier = 1.85f,
                        textAlign = TextAlignMode.CENTER,
                        directionMode = TextDirectionMode.RTL,
                        textEffect = TextEffectConfig(solidColor = 0xFF1D1D1FL),
                        cardBackgroundColor = 0xFFFFFFFFL,
                        cardCornerRadiusDp = 26f,
                        cardShadowElevationDp = 12f,
                        cardPaddingDp = 22f,
                        zIndex = 2
                    )
                )
            )
        ),
        TextTemplatePreset(
            id = "status",
            title = "Status Updates",
            subtitle = "Story & status gradient badge",
            sampleText = "Creating something new today 🎨⚡",
            isUrdu = false,
            document = CanvasDocument(
                canvasWidth = 1080,
                canvasHeight = 1920,
                background = CanvasBackgroundConfig(
                    type = BackgroundType.GRADIENT,
                    gradientColors = listOf(0xFF007AFFL, 0xFF5856D6L, 0xFFAF52DEL),
                    gradientAngleDeg = 160f
                ),
                elements = listOf(
                    CanvasElement(
                        id = "status_emoji",
                        kind = ElementKind.EMOJI,
                        xFraction = 0.36f,
                        yFraction = 0.24f,
                        widthFraction = 0.28f,
                        heightFraction = 0.16f,
                        text = "✨",
                        emojiChar = "✨",
                        zIndex = 1
                    ),
                    CanvasElement(
                        id = "status_text",
                        kind = ElementKind.TEXT,
                        xFraction = 0.08f,
                        yFraction = 0.43f,
                        widthFraction = 0.84f,
                        heightFraction = 0.24f,
                        text = "Creating something unforgettable today ⚡",
                        font = StudioFont.PLUS_JAKARTA,
                        fontSizeSp = 32f,
                        fontWeight = 700,
                        textAlign = TextAlignMode.CENTER,
                        textEffect = TextEffectConfig(
                            fillType = TextFillType.SOLID,
                            solidColor = 0xFFFFFFFFL,
                            outlineEnabled = true,
                            outlineColor = 0x44000000L,
                            outlineWidthDp = 2f,
                            shadowEnabled = true,
                            shadowColor = 0x77000000L,
                            shadowBlurRadius = 14f,
                            shadowOffsetY = 6f
                        ),
                        cardBackgroundColor = 0x00000000L,
                        zIndex = 2
                    )
                )
            )
        ),
        TextTemplatePreset(
            id = "posters",
            title = "Typography Posters",
            subtitle = "Bold outlined & gradient editorial poster",
            sampleText = "DESIGN\nBEYOND\nLIMITS",
            isUrdu = false,
            document = CanvasDocument(
                canvasWidth = 1080,
                canvasHeight = 1350,
                background = CanvasBackgroundConfig(
                    type = BackgroundType.SOLID,
                    solidColor = 0xFF000000L
                ),
                elements = listOf(
                    CanvasElement(
                        id = "poster_title",
                        kind = ElementKind.TEXT,
                        xFraction = 0.08f,
                        yFraction = 0.24f,
                        widthFraction = 0.84f,
                        heightFraction = 0.46f,
                        text = "DESIGN\nBEYOND\nLIMITS",
                        font = StudioFont.PLUS_JAKARTA,
                        fontSizeSp = 54f,
                        fontWeight = 800,
                        lineHeightMultiplier = 1.08f,
                        textAlign = TextAlignMode.CENTER,
                        textEffect = TextEffectConfig(
                            fillType = TextFillType.GRADIENT,
                            gradientColors = listOf(0xFF0A84FFL, 0xFFBF5AF2L, 0xFFFF9F0AL),
                            gradientAngleDeg = 45f,
                            outlineEnabled = true,
                            outlineColor = 0xFFFFFFFFL,
                            outlineWidthDp = 2.4f,
                            shadowEnabled = true,
                            shadowColor = 0x880A84FFL,
                            shadowBlurRadius = 18f,
                            shadowOffsetY = 8f
                        ),
                        cardBackgroundColor = 0x00000000L,
                        zIndex = 1
                    ),
                    CanvasElement(
                        id = "poster_sub",
                        kind = ElementKind.BADGE,
                        xFraction = 0.24f,
                        yFraction = 0.74f,
                        widthFraction = 0.52f,
                        heightFraction = 0.08f,
                        text = "iOSify Studio Edition",
                        font = StudioFont.INTER,
                        fontSizeSp = 16f,
                        fontWeight = 600,
                        textAlign = TextAlignMode.CENTER,
                        textEffect = TextEffectConfig(solidColor = 0xFFFFFFFFL),
                        cardBackgroundColor = 0xFF1C1C1EL,
                        cardCornerRadiusDp = 20f,
                        cardBorderWidthDp = 1f,
                        cardBorderColor = 0x44FFFFFFL,
                        zIndex = 2
                    )
                )
            )
        ),
        TextTemplatePreset(
            id = "notifications",
            title = "iOS-Style Notifications",
            subtitle = "Frosted Lock Screen banner alert",
            sampleText = "iOSify Studio • now\nYour Urdu & English design is ready to share! 💎",
            isUrdu = false,
            document = CanvasDocument(
                canvasWidth = 1080,
                canvasHeight = 1080,
                background = CanvasBackgroundConfig(
                    type = BackgroundType.GRADIENT,
                    gradientColors = listOf(0xFF1D2B64L, 0xFFF8CDDAL),
                    gradientAngleDeg = 135f
                ),
                elements = listOf(
                    CanvasElement(
                        id = "notif_card",
                        kind = ElementKind.CARD,
                        xFraction = 0.06f,
                        yFraction = 0.34f,
                        widthFraction = 0.88f,
                        heightFraction = 0.28f,
                        text = "🔔 iOSify Studio   •   now\nYour bilingual design is ready to share! 💎",
                        font = StudioFont.INTER,
                        fontSizeSp = 19f,
                        fontWeight = 600,
                        textAlign = TextAlignMode.START,
                        textEffect = TextEffectConfig(solidColor = 0xFF1D1D1FL),
                        cardBackgroundColor = 0xEBFFFFFFL,
                        cardCornerRadiusDp = 26f,
                        cardShadowElevationDp = 16f,
                        cardPaddingDp = 22f,
                        cardBorderWidthDp = 1f,
                        cardBorderColor = 0x66FFFFFFL,
                        zIndex = 1
                    )
                )
            )
        ),
        TextTemplatePreset(
            id = "chat_comp",
            title = "Chat Compositions",
            subtitle = "Two-bubble bilingual conversation",
            sampleText = "کیا حال ہے؟ ☕",
            isUrdu = true,
            document = CanvasDocument(
                canvasWidth = 1080,
                canvasHeight = 1350,
                background = CanvasBackgroundConfig(
                    type = BackgroundType.SOLID,
                    solidColor = 0xFFF5F5F7L
                ),
                elements = listOf(
                    CanvasElement(
                        id = "chat_in",
                        kind = ElementKind.CARD,
                        xFraction = 0.08f,
                        yFraction = 0.24f,
                        widthFraction = 0.68f,
                        heightFraction = 0.18f,
                        text = "السلام علیکم! نیا ڈیزائن کیسا لگا؟ ☕",
                        font = StudioFont.NOTO_NASKH_ARABIC,
                        fontSizeSp = 21f,
                        fontWeight = 600,
                        textAlign = TextAlignMode.START,
                        directionMode = TextDirectionMode.RTL,
                        textEffect = TextEffectConfig(solidColor = 0xFF1D1D1FL),
                        cardBackgroundColor = 0xFFE5E5EAL,
                        cardCornerRadiusDp = 24f,
                        cardShadowElevationDp = 4f,
                        cardPaddingDp = 18f,
                        zIndex = 1
                    ),
                    CanvasElement(
                        id = "chat_out",
                        kind = ElementKind.CARD,
                        xFraction = 0.24f,
                        yFraction = 0.48f,
                        widthFraction = 0.68f,
                        heightFraction = 0.2f,
                        text = "Absolutely stunning! Clean iOS polish ✨🔥",
                        font = StudioFont.INTER,
                        fontSizeSp = 20f,
                        fontWeight = 500,
                        textAlign = TextAlignMode.START,
                        directionMode = TextDirectionMode.LTR,
                        textEffect = TextEffectConfig(solidColor = 0xFFFFFFFFL),
                        cardBackgroundColor = 0xFF34C759L,
                        cardCornerRadiusDp = 24f,
                        cardShadowElevationDp = 8f,
                        cardPaddingDp = 18f,
                        zIndex = 2
                    )
                )
            )
        )
    )

    fun createBlankCanvasDocument(defaultFont: StudioFont): CanvasDocument {
        return CanvasDocument(
            canvasWidth = 1080,
            canvasHeight = 1350,
            background = CanvasBackgroundConfig(
                type = BackgroundType.GRADIENT,
                gradientColors = listOf(0xFFF5F5F7L, 0xFFE8F1FFL),
                gradientAngleDeg = 135f
            ),
            elements = listOf(
                CanvasElement(
                    id = "blank_title",
                    kind = ElementKind.CARD,
                    xFraction = 0.1f,
                    yFraction = 0.34f,
                    widthFraction = 0.8f,
                    heightFraction = 0.3f,
                    text = "Tap to edit your iOSify text ✨",
                    font = defaultFont,
                    fontSizeSp = 26f,
                    fontWeight = 600,
                    textAlign = TextAlignMode.CENTER,
                    directionMode = TextDirectionMode.AUTO,
                    textEffect = TextEffectConfig(solidColor = 0xFF1D1D1FL),
                    cardBackgroundColor = 0xFFFFFFFFL,
                    cardCornerRadiusDp = 24f,
                    cardShadowElevationDp = 12f,
                    cardPaddingDp = 24f,
                    zIndex = 1
                )
            )
        )
    }
}
