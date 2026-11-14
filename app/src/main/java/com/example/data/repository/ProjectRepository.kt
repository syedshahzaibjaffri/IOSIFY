package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.data.local.CanvasJsonSerializer
import com.example.data.local.ExportRecordDao
import com.example.data.local.ExportRecordEntity
import com.example.data.local.ProjectDao
import com.example.data.local.ProjectEntity
import com.example.engine.CanvasRenderer
import com.example.engine.ExportOutput
import com.example.model.CanvasDocument
import com.example.model.ProjectType
import com.example.model.TransformMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ProjectRepository(
    private val context: Context,
    private val projectDao: ProjectDao,
    private val exportRecordDao: ExportRecordDao,
    private val renderer: CanvasRenderer
) {

    val allProjects: Flow<List<ProjectEntity>> = projectDao.observeAllProjects()
    val favoriteProjects: Flow<List<ProjectEntity>> = projectDao.observeFavoriteProjects()
    val allExports: Flow<List<ExportRecordEntity>> = exportRecordDao.observeAllExports()

    suspend fun getProject(id: String): ProjectEntity? = projectDao.getProjectById(id)

    suspend fun createProject(
        name: String,
        type: ProjectType,
        mode: TransformMode = TransformMode.PRESERVE_LAYOUT,
        document: CanvasDocument,
        customEmojiPaths: Map<String, String> = emptyMap()
    ): ProjectEntity = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val thumbPath = renderThumbnail(id, document, customEmojiPaths)
        val entity = ProjectEntity(
            id = id,
            name = name.ifBlank { "Untitled Design" },
            type = type.name,
            mode = mode.name,
            sourceImagePath = document.sourceImagePath,
            thumbnailPath = thumbPath,
            canvasJson = CanvasJsonSerializer.toJson(document),
            isFavorite = false,
            createdAt = now,
            updatedAt = now
        )
        projectDao.upsertProject(entity)
        entity
    }

    suspend fun saveProgress(
        projectId: String,
        name: String,
        mode: TransformMode,
        document: CanvasDocument,
        customEmojiPaths: Map<String, String> = emptyMap()
    ): ProjectEntity? = withContext(Dispatchers.IO) {
        val existing = projectDao.getProjectById(projectId) ?: return@withContext null
        val thumbPath = renderThumbnail(projectId, document, customEmojiPaths) ?: existing.thumbnailPath
        val updated = existing.copy(
            name = name.ifBlank { existing.name },
            mode = mode.name,
            sourceImagePath = document.sourceImagePath ?: existing.sourceImagePath,
            thumbnailPath = thumbPath,
            canvasJson = CanvasJsonSerializer.toJson(document),
            updatedAt = System.currentTimeMillis()
        )
        projectDao.upsertProject(updated)
        updated
    }

    suspend fun renameProject(id: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isNotEmpty()) {
            projectDao.renameProject(id, trimmed, System.currentTimeMillis())
        }
    }

    suspend fun toggleFavorite(id: String, currentlyFavorite: Boolean) {
        projectDao.setFavorite(id, !currentlyFavorite)
    }

    suspend fun duplicateProject(
        sourceId: String,
        customEmojiPaths: Map<String, String> = emptyMap()
    ): ProjectEntity? = withContext(Dispatchers.IO) {
        val original = projectDao.getProjectById(sourceId) ?: return@withContext null
        val newId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val doc = CanvasJsonSerializer.fromJson(original.canvasJson)
        val newThumb = renderThumbnail(newId, doc, customEmojiPaths)
        val copy = original.copy(
            id = newId,
            name = "${original.name} Copy",
            thumbnailPath = newThumb,
            isFavorite = false,
            createdAt = now,
            updatedAt = now
        )
        projectDao.upsertProject(copy)
        copy
    }

    suspend fun deleteProject(id: String) = withContext(Dispatchers.IO) {
        val existing = projectDao.getProjectById(id)
        existing?.thumbnailPath?.let { path -> runCatching { File(path).delete() } }
        projectDao.deleteProjectById(id)
    }

    suspend fun recordExport(
        projectId: String?,
        projectName: String,
        output: ExportOutput,
        transparentBackground: Boolean
    ) {
        val record = ExportRecordEntity(
            id = UUID.randomUUID().toString(),
            projectId = projectId,
            projectName = projectName,
            filePath = output.filePath,
            mediaUri = output.mediaStoreUri,
            format = output.format.name,
            width = output.width,
            height = output.height,
            transparentBackground = transparentBackground,
            exportedAt = System.currentTimeMillis()
        )
        exportRecordDao.insertExport(record)
    }

    suspend fun deleteExportRecord(record: ExportRecordEntity) = withContext(Dispatchers.IO) {
        runCatching { File(record.filePath).delete() }
        exportRecordDao.deleteExportById(record.id)
    }

    suspend fun clearAllHistory() = withContext(Dispatchers.IO) {
        projectDao.deleteAllProjects()
        exportRecordDao.deleteAllExports()
        File(context.filesDir, "thumbnails").deleteRecursively()
        File(context.filesDir, "sources").deleteRecursively()
        File(context.filesDir, "exports").deleteRecursively()
    }

    suspend fun importSourceImage(uri: Uri): String? = withContext(Dispatchers.IO) {
        val sourcesDir = File(context.filesDir, "sources").apply { mkdirs() }
        val outFile = File(sourcesDir, "src_${System.currentTimeMillis()}.png")
        val input = context.contentResolver.openInputStream(uri) ?: return@withContext null
        val bitmap = input.use { BitmapFactory.decodeStream(it) } ?: return@withContext null
        val scaled = downscaleIfHuge(bitmap, 2048)
        FileOutputStream(outFile).use { out ->
            scaled.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        if (scaled !== bitmap) bitmap.recycle()
        scaled.recycle()
        outFile.absolutePath
    }

    suspend fun saveBitmapToInternalFile(bitmap: Bitmap, folder: String, prefix: String): String =
        withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, folder).apply { mkdirs() }
            val file = File(dir, "${prefix}_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file.absolutePath
        }

    private fun renderThumbnail(
        projectId: String,
        document: CanvasDocument,
        customEmojiPaths: Map<String, String>
    ): String? {
        return try {
            val thumbDir = File(context.filesDir, "thumbnails").apply { mkdirs() }
            val file = File(thumbDir, "thumb_$projectId.png")
            val aspect = document.canvasWidth.toFloat() / document.canvasHeight.coerceAtLeast(1).toFloat()
            val w = 360
            val h = (w / aspect).toInt().coerceIn(200, 560)
            val bmp = renderer.renderToBitmap(
                document = document,
                outputWidth = w,
                outputHeight = h,
                customEmojiPaths = customEmojiPaths,
                transparentBackground = false
            )
            FileOutputStream(file).use { out ->
                bmp.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
            bmp.recycle()
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    private fun downscaleIfHuge(source: Bitmap, maxDim: Int): Bitmap {
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
