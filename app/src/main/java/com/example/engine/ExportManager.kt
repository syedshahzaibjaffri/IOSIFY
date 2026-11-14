package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.model.CanvasDocument
import com.example.model.ExportFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class ExportOutput(
    val filePath: String,
    val shareUri: Uri,
    val mediaStoreUri: String?,
    val width: Int,
    val height: Int,
    val format: ExportFormat
)

class ExportManager(
    private val context: Context,
    private val renderer: CanvasRenderer
) {

    suspend fun exportDocument(
        document: CanvasDocument,
        projectName: String,
        format: ExportFormat,
        targetWidth: Int,
        targetHeight: Int,
        transparentBackground: Boolean,
        quality: Int,
        customEmojiPaths: Map<String, String>,
        saveToGallery: Boolean = true
    ): ExportOutput = withContext(Dispatchers.IO) {
        val safeW = targetWidth.coerceIn(128, 4096)
        val safeH = targetHeight.coerceIn(128, 4096)
        val effectiveTransparent = transparentBackground && format != ExportFormat.JPG

        val bitmap = renderer.renderToBitmap(
            document = document,
            outputWidth = safeW,
            outputHeight = safeH,
            customEmojiPaths = customEmojiPaths,
            transparentBackground = effectiveTransparent
        )

        val exportsDir = File(context.filesDir, "exports").apply { mkdirs() }
        val sanitizedName = projectName
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .take(32)
            .ifBlank { "iosify_design" }
        val fileName = "${sanitizedName}_${System.currentTimeMillis()}.${format.extension}"
        val privateFile = File(exportsDir, fileName)

        val compressFormat = resolveCompressFormat(format)
        FileOutputStream(privateFile).use { out ->
            val ok = bitmap.compress(compressFormat, quality.coerceIn(50, 100), out)
            if (!ok) throw IllegalStateException("Failed to encode ${format.name} image.")
        }

        val mediaUri = if (saveToGallery) {
            saveToMediaStore(bitmap, fileName, format, compressFormat, quality)
        } else null

        bitmap.recycle()

        val shareUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            privateFile
        )

        ExportOutput(
            filePath = privateFile.absolutePath,
            shareUri = shareUri,
            mediaStoreUri = mediaUri,
            width = safeW,
            height = safeH,
            format = format
        )
    }

    suspend fun exportEmojiBitmap(
        emojiChar: String,
        customAssetPath: String?,
        sizePx: Int = 512,
        transparentBackground: Boolean = true
    ): ExportOutput = withContext(Dispatchers.IO) {
        val doc = com.example.model.CanvasDocument(
            canvasWidth = sizePx,
            canvasHeight = sizePx,
            background = com.example.model.CanvasBackgroundConfig(
                type = if (transparentBackground) com.example.model.BackgroundType.TRANSPARENT
                else com.example.model.BackgroundType.SOLID,
                solidColor = 0xFFFFFFFFL
            ),
            elements = listOf(
                com.example.model.CanvasElement(
                    id = "emoji_export",
                    kind = com.example.model.ElementKind.EMOJI,
                    xFraction = 0.08f,
                    yFraction = 0.08f,
                    widthFraction = 0.84f,
                    heightFraction = 0.84f,
                    text = emojiChar,
                    emojiChar = emojiChar
                )
            )
        )
        val map = if (customAssetPath != null) mapOf(emojiChar to customAssetPath) else emptyMap()
        exportDocument(
            document = doc,
            projectName = "emoji_${emojiChar.hashCode()}",
            format = ExportFormat.PNG,
            targetWidth = sizePx,
            targetHeight = sizePx,
            transparentBackground = transparentBackground,
            quality = 100,
            customEmojiPaths = map,
            saveToGallery = true
        )
    }

    fun createShareIntent(filePath: String, format: ExportFormat): Intent {
        val file = File(filePath)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun saveToMediaStore(
        bitmap: Bitmap,
        fileName: String,
        format: ExportFormat,
        compressFormat: Bitmap.CompressFormat,
        quality: Int
    ): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/iOSify")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return null
                resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(compressFormat, quality.coerceIn(50, 100), out)
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                uri.toString()
            } else {
                val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.apply { mkdirs() }
                if (picturesDir != null) {
                    val outFile = File(picturesDir, fileName)
                    FileOutputStream(outFile).use { out ->
                        bitmap.compress(compressFormat, quality.coerceIn(50, 100), out)
                    }
                    Uri.fromFile(outFile).toString()
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun resolveCompressFormat(format: ExportFormat): Bitmap.CompressFormat {
        return when (format) {
            ExportFormat.PNG -> Bitmap.CompressFormat.PNG
            ExportFormat.JPG -> Bitmap.CompressFormat.JPEG
            ExportFormat.WEBP -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    Bitmap.CompressFormat.WEBP
                }
            }
        }
    }
}
