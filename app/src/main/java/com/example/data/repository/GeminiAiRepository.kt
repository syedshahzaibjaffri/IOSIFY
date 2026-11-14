package com.example.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import com.example.engine.LanguageDetector
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
import com.example.model.TransformMode
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.File
import java.util.UUID

private const val TAG = "GeminiAiRepository"

data class ScreenshotAnalysisResult(
    val document: CanvasDocument,
    val detectedEmojis: List<String>,
    val detectedLanguages: List<String>,
    val designSummary: String
)

class GeminiAiRepository {

    private val elementSchema = Schema.obj(
        properties = mapOf(
            "kind" to Schema.enumeration(listOf("TEXT", "CARD", "BADGE", "EMOJI")),
            "text" to Schema.string(description = "Exact verbatim OCR text without translating or modifying spelling"),
            "language" to Schema.enumeration(listOf("en", "ur", "mixed")),
            "xPercent" to Schema.integer(description = "Left coordinate 0 to 950"),
            "yPercent" to Schema.integer(description = "Top coordinate 0 to 950"),
            "widthPercent" to Schema.integer(description = "Width 50 to 960"),
            "heightPercent" to Schema.integer(description = "Height 40 to 900"),
            "fontSizeSp" to Schema.integer(description = "Suggested font size 12 to 48"),
            "fontWeight" to Schema.integer(description = "400, 500, 600, or 700"),
            "textColorHex" to Schema.string(description = "6-digit hex like #1D1D1F or #FFFFFF"),
            "cardColorHex" to Schema.string(description = "6-digit hex like #FFFFFF or #007AFF, or #00000000 for transparent"),
            "align" to Schema.enumeration(listOf("START", "CENTER", "END"))
        ),
        optionalProperties = listOf("cardColorHex", "align")
    )

    private val screenshotSchema = Schema.obj(
        properties = mapOf(
            "designSummary" to Schema.string(description = "Short summary of layout and iOS styling applied"),
            "backgroundColorHex" to Schema.string(description = "Primary background hex e.g. #F5F5F7 or #000000"),
            "gradientSecondaryHex" to Schema.string(description = "Secondary background gradient hex e.g. #EAF3FF"),
            "detectedLanguages" to Schema.array(Schema.string()),
            "detectedEmojis" to Schema.array(Schema.string()),
            "elements" to Schema.array(elementSchema)
        )
    )

    private val analysisModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = screenshotSchema
            }
        )
    }

    private val recommendationModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    private val backdropImageModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    suspend fun analyzeAndTransformScreenshot(
        sourceImagePath: String,
        mode: TransformMode,
        preferredUrduFont: StudioFont,
        preferredEnglishFont: StudioFont,
        customInstruction: String = ""
    ): ScreenshotAnalysisResult = withContext(Dispatchers.IO) {
        val rawBitmap = BitmapFactory.decodeFile(sourceImagePath)
            ?: throw IllegalStateException("Unable to load screenshot image from storage.")
        val scaledBitmap = scaleForAi(rawBitmap, 1536)

        val prompt = buildString {
            appendLine("Analyze this screenshot carefully for iOSify.")
            appendLine("CRITICAL RULES:")
            appendLine("1. Extract ALL visible text verbatim. Never fabricate content, never silently rewrite text, never reverse Urdu characters, and NEVER translate Urdu to Hindi.")
            appendLine("2. Detect whether each block is English (en), Urdu (ur), or mixed (mixed).")
            appendLine("3. Detect all emojis in the screenshot and list them in detectedEmojis.")
            if (mode == TransformMode.PRESERVE_LAYOUT) {
                appendLine("4. MODE = PRESERVE_ORIGINAL_LAYOUT: Keep xPercent, yPercent, widthPercent, and heightPercent faithful to the screenshot's original positions while upgrading colors, rounded card surfaces, spacing, and typography to minimalist Apple iOS style.")
            } else {
                appendLine("4. MODE = COMPLETE_IOS_REDESIGN: Reconstruct the extracted content into a clean, spacious iOS-style hierarchy with rounded #FFFFFF or #1C1C1E cards, #007AFF / #AF52DE accents, crisp borders, and balanced vertical rhythm.")
            }
            if (customInstruction.isNotBlank()) {
                appendLine("5. User instruction: $customInstruction")
            }
        }

        try {
            val response = withTimeout(60_000L) {
                analysisModel.generateContent(
                    content {
                        image(scaledBitmap)
                        text(prompt)
                    }
                )
            }
            val jsonStr = response.text
                ?: throw IllegalStateException("Gemini returned an empty analysis response.")

            parseAnalysisJson(
                jsonStr = jsonStr,
                rawWidth = rawBitmap.width,
                rawHeight = rawBitmap.height,
                sourceImagePath = sourceImagePath,
                mode = mode,
                preferredUrduFont = preferredUrduFont,
                preferredEnglishFont = preferredEnglishFont
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Screenshot analysis timed out", e)
            throw IllegalStateException("AI analysis timed out after 60 seconds. Please retry.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Screenshot analysis failed", e)
            throw e
        } finally {
            if (scaledBitmap !== rawBitmap) scaledBitmap.recycle()
            rawBitmap.recycle()
        }
    }

    suspend fun generateIosBackdrop(
        designContext: String
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val response = withTimeout(60_000L) {
                backdropImageModel.generateContent(
                    "Generate a clean minimalist Apple iOS frosted wallpaper gradient background inspired by: $designContext. Soft diffused light, smooth Apple-style mesh gradient, no text, no icons, no devices."
                )
            }
            val imagePart = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()
            imagePart?.image
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Backdrop generation timed out", e)
            throw IllegalStateException("AI backdrop generation timed out after 60 seconds.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Backdrop generation failed", e)
            throw e
        }
    }

    fun streamDesignTips(textSample: String): Flow<String> = flow {
        val acc = StringBuilder()
        try {
            withTimeout(45_000L) {
                recommendationModel.generateContentStream(
                    "In 2 short sentences, suggest an iOS-inspired color palette and typography pairing for this text (respecting Urdu RTL if present): $textSample"
                ).collect { chunk ->
                    acc.append(chunk.text.orEmpty())
                    emit(acc.toString())
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Design tip stream failed", e)
            throw e
        }
    }

    private fun parseAnalysisJson(
        jsonStr: String,
        rawWidth: Int,
        rawHeight: Int,
        sourceImagePath: String,
        mode: TransformMode,
        preferredUrduFont: StudioFont,
        preferredEnglishFont: StudioFont
    ): ScreenshotAnalysisResult {
        val root = JSONObject(jsonStr)
        val summary = root.optString("designSummary", "iOS layout transformation applied")
        val bgPrimary = parseHexColor(root.optString("backgroundColorHex", "#F5F5F7"), 0xFFF5F5F7L)
        val bgSecondary = parseHexColor(root.optString("gradientSecondaryHex", "#EAF3FF"), 0xFFEAF3FFL)

        val languages = mutableListOf<String>()
        val langArray = root.optJSONArray("detectedLanguages")
        if (langArray != null) {
            for (i in 0 until langArray.length()) {
                languages.add(langArray.optString(i))
            }
        }

        val emojis = mutableListOf<String>()
        val emojiArray = root.optJSONArray("detectedEmojis")
        if (emojiArray != null) {
            for (i in 0 until emojiArray.length()) {
                val em = emojiArray.optString(i)
                if (em.isNotBlank()) emojis.add(em)
            }
        }

        val elements = mutableListOf<CanvasElement>()
        val elementsArray = root.optJSONArray("elements")
        if (elementsArray != null) {
            for (i in 0 until elementsArray.length()) {
                val item = elementsArray.optJSONObject(i) ?: continue
                val text = item.optString("text", "")
                if (text.isBlank()) continue
                val kindStr = item.optString("kind", "CARD")
                val kind = runCatching { ElementKind.valueOf(kindStr) }.getOrDefault(ElementKind.CARD)
                val lang = item.optString("language", "en")
                val isUrdu = lang == "ur" || lang == "mixed" || LanguageDetector.containsUrduOrArabic(text)

                val x = (item.optInt("xPercent", 80) / 1000f).coerceIn(0.02f, 0.9f)
                val y = (item.optInt("yPercent", 100 + i * 140) / 1000f).coerceIn(0.02f, 0.9f)
                val w = (item.optInt("widthPercent", 840) / 1000f).coerceIn(0.12f, 0.96f)
                val h = (item.optInt("heightPercent", 140) / 1000f).coerceIn(0.06f, 0.85f)

                val textColor = parseHexColor(item.optString("textColorHex", "#1D1D1F"), 0xFF1D1D1FL)
                val cardColorRaw = item.optString("cardColorHex", "#FFFFFF")
                val cardColor = if (cardColorRaw.equals("#00000000", ignoreCase = true)) {
                    0x00000000L
                } else {
                    parseHexColor(cardColorRaw, 0xFFFFFFFFL)
                }

                val alignStr = item.optString("align", "START")
                val align = runCatching { TextAlignMode.valueOf(alignStr) }.getOrDefault(TextAlignMode.START)
                val font = if (isUrdu) preferredUrduFont else preferredEnglishFont
                val dirMode = if (isUrdu) TextDirectionMode.AUTO else TextDirectionMode.LTR

                elements.add(
                    CanvasElement(
                        id = "ai_${i}_${UUID.randomUUID().toString().take(6)}",
                        kind = kind,
                        xFraction = x,
                        yFraction = y,
                        widthFraction = w,
                        heightFraction = h,
                        text = text,
                        font = font,
                        fontSizeSp = item.optInt("fontSizeSp", 18).toFloat().coerceIn(12f, 48f),
                        fontWeight = item.optInt("fontWeight", 500),
                        lineHeightMultiplier = if (isUrdu) 1.75f else 1.35f,
                        textAlign = align,
                        directionMode = dirMode,
                        textEffect = TextEffectConfig(
                            fillType = TextFillType.SOLID,
                            solidColor = textColor
                        ),
                        cardBackgroundColor = cardColor,
                        cardCornerRadiusDp = if (kind == ElementKind.BADGE) 16f else 22f,
                        cardShadowElevationDp = if (cardColor == 0x00000000L) 0f else 10f,
                        cardPaddingDp = 16f,
                        emojiChar = if (kind == ElementKind.EMOJI) text else "",
                        zIndex = i + 1
                    )
                )
            }
        }

        val targetW = if (rawWidth >= rawHeight) 1350 else 1080
        val targetH = ((targetW.toFloat() * rawHeight) / rawWidth.coerceAtLeast(1)).toInt().coerceIn(720, 1920)

        val document = CanvasDocument(
            canvasWidth = targetW,
            canvasHeight = targetH,
            background = CanvasBackgroundConfig(
                type = BackgroundType.GRADIENT,
                solidColor = bgPrimary,
                gradientColors = listOf(bgPrimary, bgSecondary),
                gradientAngleDeg = 135f
            ),
            elements = elements,
            sourceImagePath = sourceImagePath,
            transformMode = mode,
            designSummary = summary
        )
        return ScreenshotAnalysisResult(
            document = document,
            detectedEmojis = emojis,
            detectedLanguages = languages,
            designSummary = summary
        )
    }

    private fun parseHexColor(hex: String, fallback: Long): Long {
        return try {
            val clean = hex.trim()
            val colorInt = Color.parseColor(if (clean.startsWith("#")) clean else "#$clean")
            colorInt.toLong() and 0xFFFFFFFFL
        } catch (_: Exception) {
            fallback
        }
    }

    private fun scaleForAi(source: Bitmap, maxDim: Int): Bitmap {
        val w = source.width
        val h = source.height
        if (w <= maxDim && h <= maxDim) return source
        val ratio = minOf(maxDim.toFloat() / w, maxDim.toFloat() / h)
        return Bitmap.createScaledBitmap(
            source,
            (w * ratio).toInt().coerceAtLeast(1),
            (h * ratio).toInt().coerceAtLeast(1),
            true
        )
    }
}
