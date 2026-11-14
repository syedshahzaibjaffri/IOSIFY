package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun observeFavoriteProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProject(project: ProjectEntity)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("UPDATE projects SET name = :name, updatedAt = :updatedAt WHERE id = :id")
    suspend fun renameProject(id: String, name: String, updatedAt: Long)

    @Query("UPDATE projects SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)

    @Query("DELETE FROM projects")
    suspend fun deleteAllProjects()
}

@Dao
interface ExportRecordDao {
    @Query("SELECT * FROM export_records ORDER BY exportedAt DESC")
    fun observeAllExports(): Flow<List<ExportRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExport(record: ExportRecordEntity)

    @Query("DELETE FROM export_records WHERE id = :id")
    suspend fun deleteExportById(id: String)

    @Query("DELETE FROM export_records")
    suspend fun deleteAllExports()
}

@Dao
interface EmojiDao {
    @Query("SELECT * FROM custom_emojis ORDER BY createdAt DESC")
    fun observeCustomEmojis(): Flow<List<CustomEmojiEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomEmoji(emoji: CustomEmojiEntity)

    @Query("DELETE FROM custom_emojis WHERE id = :id")
    suspend fun deleteCustomEmoji(id: String)

    @Query("SELECT * FROM emoji_usage ORDER BY lastUsedAt DESC")
    fun observeEmojiUsage(): Flow<List<EmojiUsageEntity>>

    @Query("SELECT * FROM emoji_usage WHERE emojiChar = :emojiChar LIMIT 1")
    suspend fun getUsage(emojiChar: String): EmojiUsageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUsage(usage: EmojiUsageEntity)
}
