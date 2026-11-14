package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val mode: String,
    val sourceImagePath: String? = null,
    val thumbnailPath: String? = null,
    val canvasJson: String,
    val isFavorite: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "export_records")
data class ExportRecordEntity(
    @PrimaryKey val id: String,
    val projectId: String?,
    val projectName: String,
    val filePath: String,
    val mediaUri: String? = null,
    val format: String,
    val width: Int,
    val height: Int,
    val transparentBackground: Boolean = false,
    val exportedAt: Long
)

@Entity(tableName = "custom_emojis")
data class CustomEmojiEntity(
    @PrimaryKey val id: String,
    val packName: String,
    val emojiChar: String,
    val shortName: String,
    val assetFilePath: String,
    val isFavorite: Boolean = false,
    val createdAt: Long
)

@Entity(tableName = "emoji_usage")
data class EmojiUsageEntity(
    @PrimaryKey val emojiChar: String,
    val isFavorite: Boolean = false,
    val lastUsedAt: Long = 0L
)
