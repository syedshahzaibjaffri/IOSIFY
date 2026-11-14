package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.model.BackgroundType
import com.example.model.CanvasBackgroundConfig
import com.example.model.CanvasDocument
import com.example.model.CanvasElement
import com.example.model.ElementKind
import com.example.model.StudioFont
import com.example.model.TextAlignMode
import com.example.model.TextDirectionMode
import com.example.model.TextFillType
import java.io.File
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class CanvasRenderer(private val context: Context) {

    private val fontCache = mutableMapOf<StudioFont, Typeface>()
    private val bitmapCache = mutableMapOf<String, Bitmap>()

    fun clearBitmapCache() {
        bitmapCache.clear()
    }

    fun renderDocument(
        canvas: Canvas,
        document: CanvasDocument,
        targetWidth: Float,
        targetHeight: Float,
        customEmojiPaths: Map<String, String> = emptyMap(),
        transparentOverride: Boolean = false
    ) {
        val scale = min(
            targetWidth / document.canvasWidth.coerceAtLeast(1).toFloat(),
            targetHeight / document.canvasHeight.coerceAtLeast(1).toFloat()
        )

        if (!transparentOverride && document.background.type != BackgroundType.TRANSPARENT) {
            drawBackground(canvas, document.background, document.aiBackdropPath, targetWidth, targetHeight)
        }

        val sortedElements = document.elements.sortedBy { it.zIndex }
        for (element in sortedElements) {
            drawElement(
                canvas = canvas,
                element = element,
                canvasWidth = targetWidth,
                canvasHeight = targetHeight,
                scale = scale,
                customEmojiPaths = customEmojiPaths
            )
        }
    }

    fun renderToBitmap(
        document: CanvasDocument,
        outputWidth: Int,
        outputHeight: Int,
        customEmojiPaths: Map<String, String> = emptyMap(),
        transparentBackground: Boolean = false
    ): Bitmap {
        val safeWidth = outputWidth.coerceIn(64, 4096)
        val safeHeight = outputHeight.coerceIn(64, 4096)
        val bitmap = Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (transparentBackground || document.background.type == BackgroundType.TRANSPARENT) {
            canvas.drawColor(Color.TRANSPARENT)
        }
        renderDocument(
            canvas = canvas,
            document = document,
            targetWidth = safeWidth.toFloat(),
            targetHeight = safeHeight.toFloat(),
            customEmojiPaths = customEmojiPaths,
            transparentOverride = transparentBackground
        )
        return bitmap
    }

    private fun drawBackground(
        canvas: Canvas,
        bg: CanvasBackgroundConfig,
        aiBackdropPath: String?,
        width: Float,
        height: Float
    ) {
        val rect = RectF(0f, 0f, width, height)
        when (bg.type) {
            BackgroundType.TRANSPARENT -> Unit
            BackgroundType.SOLID -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = bg.solidColor.toInt()
                    style = Paint.Style.FILL
                }
                canvas.drawRect(rect, paint)
            }
            BackgroundType.GRADIENT -> {
                val colors = if (bg.gradientColors.size >= 2) {
                    bg.gradientColors.map { it.toInt() }.toIntArray()
                } else {
                    intArrayOf(bg.solidColor.toInt(), bg.solidColor.toInt())
                }
                val shader = createLinearGradient(width, height, bg.gradientAngleDeg, colors)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.shader = shader
                    style = Paint.Style.FILL
                }
                canvas.drawRect(rect, paint)
            }
            BackgroundType.IMAGE -> {
                val path = bg.imagePath ?: aiBackdropPath
                val bmp = path?.let { loadCachedBitmap(it) }
                if (bmp != null) {
                    drawCenterCroppedBitmap(canvas, bmp, width, height)
                } else {
                    val fallback = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = bg.solidColor.toInt()
                    }
                    canvas.drawRect(rect, fallback)
                }
            }
        }
    }

    private fun drawElement(
        canvas: Canvas,
        element: CanvasElement,
        canvasWidth: Float,
        canvasHeight: Float,
        scale: Float,
        customEmojiPaths: Map<String, String>
    ) {
        val left = element.xFraction * canvasWidth
        val top = element.yFraction * canvasHeight
        val width = max(24f, element.widthFraction * canvasWidth)
        val height = max(24f, element.heightFraction * canvasHeight)
        val bounds = RectF(left, top, left + width, top + height)

        when (element.kind) {
            ElementKind.CARD, ElementKind.BADGE -> {
                drawCardSurface(canvas, element, bounds, scale)
                if (element.text.isNotEmpty()) {
                    val pad = element.cardPaddingDp * 2.5f * scale
                    val textRect = RectF(
                        bounds.left + pad,
                        bounds.top + pad,
                        max(bounds.left + pad + 16f, bounds.right - pad),
                        max(bounds.top + pad + 16f, bounds.bottom - pad)
                    )
                    drawStyledText(canvas, element, textRect, scale, customEmojiPaths)
                }
            }
            ElementKind.TEXT -> {
                if (element.cardBackgroundColor != 0x00000000L && (element.cardBackgroundColor ushr 24) > 0L) {
                    drawCardSurface(canvas, element, bounds, scale)
                }
                val pad = element.cardPaddingDp * 2.2f * scale
                val textRect = RectF(
                    bounds.left + pad,
                    bounds.top + pad,
                    max(bounds.left + pad + 16f, bounds.right - pad),
                    max(bounds.top + pad + 16f, bounds.bottom - pad)
                )
                drawStyledText(canvas, element, textRect, scale, customEmojiPaths)
            }
            ElementKind.EMOJI -> {
                val emoji = element.emojiChar.ifEmpty { element.text }
                val customPath = customEmojiPaths[emoji]
                val customBmp = customPath?.let { loadCachedBitmap(it) }
                if (customBmp != null) {
                    drawContainedBitmap(canvas, customBmp, bounds)
                } else {
                    val emojiElement = element.copy(
                        text = emoji,
                        textAlign = TextAlignMode.CENTER,
                        fontSizeSp = (min(width, height) / (2.7f * scale)).coerceIn(22f, 180f)
                    )
                    drawStyledText(canvas, emojiElement, bounds, scale, customEmojiPaths)
                }
            }
        }
    }

    private fun drawCardSurface(
        canvas: Canvas,
        element: CanvasElement,
        bounds: RectF,
        scale: Float
    ) {
        val radius = element.cardCornerRadiusDp * 2.4f * scale
        val shadowRadius = element.cardShadowElevationDp * 2.2f * scale

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = element.cardBackgroundColor.toInt()
            style = Paint.Style.FILL
            if (shadowRadius > 0.5f) {
                setShadowLayer(shadowRadius, 0f, shadowRadius * 0.35f, Color.argb(44, 0, 0, 0))
            }
        }
        val path = Path().apply {
            addRoundRect(bounds, radius, radius, Path.Direction.CW)
        }
        canvas.drawPath(path, fillPaint)

        if (element.cardBorderWidthDp > 0f && (element.cardBorderColor ushr 24) > 0L) {
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = element.cardBorderColor.toInt()
                style = Paint.Style.STROKE
                strokeWidth = element.cardBorderWidthDp * 2.2f * scale
            }
            canvas.drawRoundRect(bounds, radius, radius, borderPaint)
        }
    }

    private fun drawStyledText(
        canvas: Canvas,
        element: CanvasElement,
        rect: RectF,
        scale: Float,
        customEmojiPaths: Map<String, String>
    ) {
        val rawText = element.text
        if (rawText.isEmpty()) return

        val availWidth = max(16, rect.width().toInt())
        val isRtl = LanguageDetector.resolveIsRtl(rawText, element.directionMode)
        val effectiveFont = resolveEffectiveFont(element.font, rawText)
        val typeface = resolveTypeface(effectiveFont, element.fontWeight)

        val fontSizePx = element.fontSizeSp * 2.65f * scale
        val basePaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            this.typeface = typeface
            this.textSize = fontSizePx
            this.letterSpacing = element.letterSpacingEm
            if (element.fontWeight >= 600 && Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                isFakeBoldText = true
            }
        }

        val styledText = buildSpannedWithCustomEmojis(
            text = rawText,
            fontSizePx = fontSizePx,
            customEmojiPaths = customEmojiPaths
        )

        val alignment = resolveAlignment(element.textAlign, isRtl)
        val lineMult = if (effectiveFont == StudioFont.NOTO_NASTALIQ_URDU) {
            max(element.lineHeightMultiplier, 1.65f)
        } else {
            element.lineHeightMultiplier
        }

        val effect = element.textEffect
        val textDirection = when (element.directionMode) {
            TextDirectionMode.RTL -> TextDirectionHeuristics.RTL
            TextDirectionMode.LTR -> TextDirectionHeuristics.LTR
            TextDirectionMode.AUTO ->
                if (isRtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.FIRSTSTRONG_LTR
        }

        fun createLayout(paint: TextPaint): StaticLayout {
            return StaticLayout.Builder
                .obtain(styledText, 0, styledText.length, paint, availWidth)
                .setAlignment(alignment)
                .setLineSpacing(0f, lineMult)
                .setIncludePad(true)
                .setTextDirection(textDirection)
                .build()
        }

        val measureLayout = createLayout(basePaint)
        val textHeight = measureLayout.height.toFloat()
        val extraTopPad = if (effectiveFont == StudioFont.NOTO_NASTALIQ_URDU) fontSizePx * 0.18f else 0f
        val startY = rect.top + max(0f, (rect.height() - textHeight) / 2f) + extraTopPad

        canvas.save()
        canvas.translate(rect.left, startY)

        // Pass 1: Drop shadow (drawn cleanly underneath stroke and gradient fill)
        if (effect.shadowEnabled) {
            val shadowPaint = TextPaint(basePaint).apply {
                color = if (effect.outlineEnabled) effect.outlineColor.toInt() else effect.solidColor.toInt()
                style = if (effect.outlineEnabled) Paint.Style.STROKE else Paint.Style.FILL
                if (effect.outlineEnabled) {
                    strokeWidth = effect.outlineWidthDp * 2.4f * scale * 2f
                    strokeJoin = Paint.Join.ROUND
                    strokeCap = Paint.Cap.ROUND
                }
                setShadowLayer(
                    (effect.shadowBlurRadius * 2.2f * scale).coerceAtLeast(0.5f),
                    effect.shadowOffsetX * 2.2f * scale,
                    effect.shadowOffsetY * 2.2f * scale,
                    effect.shadowColor.toInt()
                )
            }
            createLayout(shadowPaint).draw(canvas)
        }

        // Pass 2: Outer Stroke (2x strokeWidth drawn under Fill keeps Urdu ligatures seamless)
        if (effect.outlineEnabled && effect.outlineWidthDp > 0f) {
            val strokePaint = TextPaint(basePaint).apply {
                style = Paint.Style.STROKE
                strokeWidth = effect.outlineWidthDp * 2.4f * scale * 2f
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
                color = effect.outlineColor.toInt()
                shader = null
            }
            createLayout(strokePaint).draw(canvas)
        }

        // Pass 3: Solid or Linear Gradient Fill
        val fillPaint = TextPaint(basePaint).apply {
            style = Paint.Style.FILL
            if (effect.fillType == TextFillType.GRADIENT && effect.gradientColors.size >= 2) {
                val gradColors = effect.gradientColors.map { it.toInt() }.toIntArray()
                shader = createLinearGradient(
                    width = availWidth.toFloat(),
                    height = max(textHeight, fontSizePx),
                    angleDeg = effect.gradientAngleDeg,
                    colors = gradColors
                )
            } else {
                color = effect.solidColor.toInt()
                shader = null
            }
        }
        createLayout(fillPaint).draw(canvas)

        canvas.restore()
    }

    private fun buildSpannedWithCustomEmojis(
        text: String,
        fontSizePx: Float,
        customEmojiPaths: Map<String, String>
    ): CharSequence {
        if (customEmojiPaths.isEmpty()) return text
        val matchingKeys = customEmojiPaths.keys.filter { it.isNotEmpty() && text.contains(it) }
            .sortedByDescending { it.length }
        if (matchingKeys.isEmpty()) return text

        val builder = SpannableStringBuilder(text)
        val iconSize = (fontSizePx * 1.08f).toInt().coerceAtLeast(16)
        for (emojiKey in matchingKeys) {
            val path = customEmojiPaths[emojiKey] ?: continue
            val bmp = loadCachedBitmap(path) ?: continue
            var index = builder.indexOf(emojiKey)
            while (index >= 0) {
                val aspect = bmp.width.toFloat() / bmp.height.coerceAtLeast(1).toFloat()
                val w = if (aspect >= 1f) iconSize else (iconSize * aspect).toInt().coerceAtLeast(8)
                val h = if (aspect <= 1f) iconSize else (iconSize / aspect).toInt().coerceAtLeast(8)
                val drawable = BitmapDrawable(context.resources, bmp).apply {
                    setBounds(0, 0, w, h)
                }
                val span = ImageSpan(drawable, DynamicDrawableSpan.ALIGN_CENTER)
                builder.setSpan(span, index, index + emojiKey.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                index = builder.indexOf(emojiKey, startIndex = index + emojiKey.length)
            }
        }
        return builder
    }

    private fun resolveEffectiveFont(requested: StudioFont, text: String): StudioFont {
        val hasUrdu = LanguageDetector.containsUrduOrArabic(text)
        return if (hasUrdu && !requested.supportsUrdu) {
            StudioFont.NOTO_NASTALIQ_URDU
        } else {
            requested
        }
    }

    private fun resolveTypeface(font: StudioFont, weight: Int): Typeface {
        val base = fontCache.getOrPut(font) {
            when (font) {
                StudioFont.INTER -> ResourcesCompat.getFont(context, R.font.inter) ?: Typeface.SANS_SERIF
                StudioFont.PLUS_JAKARTA -> ResourcesCompat.getFont(context, R.font.plus_jakarta_sans) ?: Typeface.SANS_SERIF
                StudioFont.NOTO_NASTALIQ_URDU -> ResourcesCompat.getFont(context, R.font.noto_nastaliq_urdu) ?: Typeface.DEFAULT
                StudioFont.NOTO_NASKH_ARABIC -> ResourcesCompat.getFont(context, R.font.noto_naskh_arabic) ?: Typeface.DEFAULT
                StudioFont.SYSTEM_UI -> Typeface.SANS_SERIF
            }
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Typeface.create(base, weight.coerceIn(100, 900), false)
        } else {
            if (weight >= 600) Typeface.create(base, Typeface.BOLD) else base
        }
    }

    private fun resolveAlignment(alignMode: TextAlignMode, isRtl: Boolean): Layout.Alignment {
        return when (alignMode) {
            TextAlignMode.CENTER -> Layout.Alignment.ALIGN_CENTER
            TextAlignMode.START -> if (isRtl) Layout.Alignment.ALIGN_NORMAL else Layout.Alignment.ALIGN_NORMAL
            TextAlignMode.END -> Layout.Alignment.ALIGN_OPPOSITE
        }
    }

    private fun createLinearGradient(
        width: Float,
        height: Float,
        angleDeg: Float,
        colors: IntArray
    ): LinearGradient {
        val rad = Math.toRadians(angleDeg.toDouble())
        val dx = cos(rad).toFloat()
        val dy = sin(rad).toFloat()
        val cx = width / 2f
        val cy = height / 2f
        val halfSpan = (kotlin.math.abs(width * dx) + kotlin.math.abs(height * dy)) / 2f
        val x0 = cx - dx * halfSpan
        val y0 = cy - dy * halfSpan
        val x1 = cx + dx * halfSpan
        val y1 = cy + dy * halfSpan
        return LinearGradient(x0, y0, x1, y1, colors, null, Shader.TileMode.CLAMP)
    }

    private fun drawCenterCroppedBitmap(canvas: Canvas, bitmap: Bitmap, targetW: Float, targetH: Float) {
        val srcRatio = bitmap.width.toFloat() / bitmap.height.coerceAtLeast(1)
        val dstRatio = targetW / targetH.coerceAtLeast(1f)
        val destRect = RectF(0f, 0f, targetW, targetH)
        val srcRect = if (srcRatio > dstRatio) {
            val newW = (bitmap.height * dstRatio).toInt()
            val xOff = (bitmap.width - newW) / 2
            android.graphics.Rect(xOff, 0, xOff + newW, bitmap.height)
        } else {
            val newH = (bitmap.width / dstRatio).toInt()
            val yOff = (bitmap.height - newH) / 2
            android.graphics.Rect(0, yOff, bitmap.width, yOff + newH)
        }
        canvas.drawBitmap(bitmap, srcRect, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
    }

    private fun drawContainedBitmap(canvas: Canvas, bitmap: Bitmap, bounds: RectF) {
        val srcRatio = bitmap.width.toFloat() / bitmap.height.coerceAtLeast(1)
        val dstRatio = bounds.width() / bounds.height().coerceAtLeast(1f)
        val drawRect = if (srcRatio > dstRatio) {
            val h = bounds.width() / srcRatio
            val dy = (bounds.height() - h) / 2f
            RectF(bounds.left, bounds.top + dy, bounds.right, bounds.top + dy + h)
        } else {
            val w = bounds.height() * srcRatio
            val dx = (bounds.width() - w) / 2f
            RectF(bounds.left + dx, bounds.top, bounds.left + dx + w, bounds.bottom)
        }
        canvas.drawBitmap(bitmap, null, drawRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    }

    private fun loadCachedBitmap(path: String): Bitmap? {
        bitmapCache[path]?.let { if (!it.isRecycled) return it }
        val file = File(path)
        if (!file.exists()) return null
        val opts = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bmp = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return null
        bitmapCache[path] = bmp
        return bmp
    }
}
