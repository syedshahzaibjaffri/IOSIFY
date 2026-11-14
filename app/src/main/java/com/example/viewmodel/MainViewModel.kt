package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.CustomEmojiEntity
import com.example.data.local.ExportRecordEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.SettingsRepository
import com.example.data.repository.EmojiRepository
import com.example.data.repository.ProjectRepository
import com.example.data.repository.TemplateLibrary
import com.example.model.AppLanguage
import com.example.model.CanvasDocument
import com.example.model.ExportFormat
import com.example.model.ProjectType
import com.example.model.StudioFont
import com.example.model.TextTemplatePreset
import com.example.model.ThemePreference
import com.example.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HomeFilter(val label: String) {
    ALL("All Projects"),
    FAVORITES("Favorites"),
    EXPORTS("Recent Exports")
}

class MainViewModel(
    private val projectRepository: ProjectRepository,
    private val emojiRepository: EmojiRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(HomeFilter.ALL)
    val selectedFilter: StateFlow<HomeFilter> = _selectedFilter.asStateFlow()

    val settings: StateFlow<UserSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    val allProjects: StateFlow<List<ProjectEntity>> = projectRepository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentExports: StateFlow<List<ExportRecordEntity>> = projectRepository.allExports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customEmojis: StateFlow<List<CustomEmojiEntity>> = emojiRepository.customEmojis
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredProjects: StateFlow<List<ProjectEntity>> = combine(
        allProjects,
        searchQuery,
        selectedFilter
    ) { projects, query, filter ->
        val base = when (filter) {
            HomeFilter.FAVORITES -> projects.filter { it.isFavorite }
            else -> projects
        }
        val trimmed = query.trim()
        if (trimmed.isEmpty()) base else base.filter { it.name.contains(trimmed, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun currentEmojiPathMap(): Map<String, String> =
        customEmojis.value.associate { it.emojiChar to it.assetFilePath }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setHomeFilter(filter: HomeFilter) {
        _selectedFilter.value = filter
    }

    fun createBlankProject(onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val doc = TemplateLibrary.createBlankCanvasDocument(settings.value.defaultEnglishFont)
            val created = projectRepository.createProject(
                name = "New iOSify Design",
                type = ProjectType.TEXT_CARD,
                document = doc,
                customEmojiPaths = currentEmojiPathMap()
            )
            onCreated(created.id)
        }
    }

    fun createFromTemplate(
        preset: TextTemplatePreset,
        customText: String? = null,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val doc = if (!customText.isNullOrBlank()) {
                val updatedFirst = preset.document.elements.firstOrNull()?.let { first ->
                    val hasUrdu = com.example.engine.LanguageDetector.containsUrduOrArabic(customText)
                    first.copy(
                        text = customText,
                        font = if (hasUrdu) settings.value.defaultUrduFont else first.font
                    )
                }
                if (updatedFirst != null) {
                    preset.document.copy(elements = listOf(updatedFirst) + preset.document.elements.drop(1))
                } else {
                    preset.document
                }
            } else {
                preset.document
            }
            val created = projectRepository.createProject(
                name = preset.title,
                type = ProjectType.TEXT_CARD,
                document = doc,
                customEmojiPaths = currentEmojiPathMap()
            )
            onCreated(created.id)
        }
    }

    fun createCustomTextProject(
        title: String,
        document: CanvasDocument,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val created = projectRepository.createProject(
                name = title,
                type = ProjectType.TEXT_CARD,
                document = document,
                customEmojiPaths = currentEmojiPathMap()
            )
            onCreated(created.id)
        }
    }

    fun renameProject(projectId: String, newName: String) {
        viewModelScope.launch {
            projectRepository.renameProject(projectId, newName)
        }
    }

    fun duplicateProject(projectId: String) {
        viewModelScope.launch {
            projectRepository.duplicateProject(projectId, currentEmojiPathMap())
        }
    }

    fun toggleFavorite(project: ProjectEntity) {
        viewModelScope.launch {
            projectRepository.toggleFavorite(project.id, project.isFavorite)
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            projectRepository.deleteProject(projectId)
        }
    }

    fun deleteExport(record: ExportRecordEntity) {
        viewModelScope.launch {
            projectRepository.deleteExportRecord(record)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            projectRepository.clearAllHistory()
        }
    }

    fun setThemeMode(mode: ThemePreference) = viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    fun setAppLanguage(lang: AppLanguage) = viewModelScope.launch { settingsRepository.setAppLanguage(lang) }
    fun setDefaultEnglishFont(font: StudioFont) = viewModelScope.launch { settingsRepository.setDefaultEnglishFont(font) }
    fun setDefaultUrduFont(font: StudioFont) = viewModelScope.launch { settingsRepository.setDefaultUrduFont(font) }
    fun setDefaultEmojiPack(pack: String) = viewModelScope.launch { settingsRepository.setDefaultEmojiPack(pack) }
    fun setDefaultExportFormat(fmt: ExportFormat) = viewModelScope.launch { settingsRepository.setDefaultExportFormat(fmt) }
    fun setExportQuality(quality: Int) = viewModelScope.launch { settingsRepository.setExportQuality(quality) }
    fun setAiUploadConsent(consented: Boolean) = viewModelScope.launch { settingsRepository.setAiUploadConsent(consented) }

    companion object {
        fun provideFactory(
            projectRepository: ProjectRepository,
            emojiRepository: EmojiRepository,
            settingsRepository: SettingsRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { MainViewModel(projectRepository, emojiRepository, settingsRepository) }
        }
    }
}
