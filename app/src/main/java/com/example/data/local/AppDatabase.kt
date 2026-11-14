package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.CanvasDocument
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@Database(
    entities = [
        ProjectEntity::class,
        ExportRecordEntity::class,
        CustomEmojiEntity::class,
        EmojiUsageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun exportRecordDao(): ExportRecordDao
    abstract fun emojiDao(): EmojiDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "iosify_studio.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

object CanvasJsonSerializer {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(CanvasDocument::class.java)

    fun toJson(document: CanvasDocument): String = adapter.toJson(document)

    fun fromJson(json: String): CanvasDocument {
        return runCatching { adapter.fromJson(json) }.getOrNull() ?: CanvasDocument()
    }
}
