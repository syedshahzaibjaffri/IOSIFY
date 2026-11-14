package com.example

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsRepository
import com.example.data.repository.EmojiRepository
import com.example.data.repository.GeminiAiRepository
import com.example.data.repository.ProjectRepository
import com.example.engine.CanvasRenderer
import com.example.engine.ExportManager

class IosifyContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database by lazy { AppDatabase.getInstance(appContext) }

    val canvasRenderer by lazy { CanvasRenderer(appContext) }
    val exportManager by lazy { ExportManager(appContext, canvasRenderer) }
    val settingsRepository by lazy { SettingsRepository(appContext) }
    val projectRepository by lazy {
        ProjectRepository(
            context = appContext,
            projectDao = database.projectDao(),
            exportRecordDao = database.exportRecordDao(),
            renderer = canvasRenderer
        )
    }
    val emojiRepository by lazy {
        EmojiRepository(
            context = appContext,
            emojiDao = database.emojiDao()
        )
    }
    val geminiAiRepository by lazy { GeminiAiRepository() }
}
