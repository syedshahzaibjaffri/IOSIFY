package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppLanguage
import com.example.model.ExportFormat
import com.example.model.StudioFont
import com.example.model.ThemePreference
import com.example.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "iosify_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val DEFAULT_EN_FONT = stringPreferencesKey("default_en_font")
        val DEFAULT_UR_FONT = stringPreferencesKey("default_ur_font")
        val DEFAULT_EMOJI_PACK = stringPreferencesKey("default_emoji_pack")
        val DEFAULT_EXPORT_FORMAT = stringPreferencesKey("default_export_format")
        val EXPORT_QUALITY = intPreferencesKey("export_quality")
        val AI_UPLOAD_CONSENT = booleanPreferencesKey("ai_upload_consent")
    }

    val settingsFlow: Flow<UserSettings> = context.settingsDataStore.data.map { prefs ->
        UserSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() }
                ?: ThemePreference.SYSTEM,
            appLanguage = prefs[Keys.APP_LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
                ?: AppLanguage.AUTO,
            defaultEnglishFont = prefs[Keys.DEFAULT_EN_FONT]?.let { runCatching { StudioFont.valueOf(it) }.getOrNull() }
                ?: StudioFont.INTER,
            defaultUrduFont = prefs[Keys.DEFAULT_UR_FONT]?.let { runCatching { StudioFont.valueOf(it) }.getOrNull() }
                ?: StudioFont.NOTO_NASTALIQ_URDU,
            defaultEmojiPack = prefs[Keys.DEFAULT_EMOJI_PACK] ?: "Default iOSify",
            defaultExportFormat = prefs[Keys.DEFAULT_EXPORT_FORMAT]?.let { runCatching { ExportFormat.valueOf(it) }.getOrNull() }
                ?: ExportFormat.PNG,
            exportQuality = prefs[Keys.EXPORT_QUALITY] ?: 95,
            hasConsentedToAiUpload = prefs[Keys.AI_UPLOAD_CONSENT] ?: false
        )
    }

    suspend fun setThemeMode(mode: ThemePreference) {
        context.settingsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        context.settingsDataStore.edit { it[Keys.APP_LANGUAGE] = language.name }
    }

    suspend fun setDefaultEnglishFont(font: StudioFont) {
        context.settingsDataStore.edit { it[Keys.DEFAULT_EN_FONT] = font.name }
    }

    suspend fun setDefaultUrduFont(font: StudioFont) {
        context.settingsDataStore.edit { it[Keys.DEFAULT_UR_FONT] = font.name }
    }

    suspend fun setDefaultEmojiPack(pack: String) {
        context.settingsDataStore.edit { it[Keys.DEFAULT_EMOJI_PACK] = pack }
    }

    suspend fun setDefaultExportFormat(format: ExportFormat) {
        context.settingsDataStore.edit { it[Keys.DEFAULT_EXPORT_FORMAT] = format.name }
    }

    suspend fun setExportQuality(quality: Int) {
        context.settingsDataStore.edit { it[Keys.EXPORT_QUALITY] = quality.coerceIn(50, 100) }
    }

    suspend fun setAiUploadConsent(consented: Boolean) {
        context.settingsDataStore.edit { it[Keys.AI_UPLOAD_CONSENT] = consented }
    }
}
