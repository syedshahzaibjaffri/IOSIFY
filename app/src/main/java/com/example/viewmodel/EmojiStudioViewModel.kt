package com.example.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.CustomEmojiEntity
import com.example.data.local.EmojiUsageEntity
import com.example.data.repository.EmojiRepository
import com.example.data.repository.ProjectRepository
import com.example.engine.ExportManager
import com.example.engine.ExportOutput
import com.example.model.BackgroundType
import com.example.model.CanvasBackgroundConfig
import com.example.model.CanvasDocument
import com.example.model.CanvasElement
import com.example.model.ElementKind
import com.example.model.EmojiCatalogItem
import com.example.model.EmojiCategory
import com.example.model.ProjectType
import com.example.model.StudioFont
import com.example.model.TextAlignMode
import com.example.model.TextEffectConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface EmojiExportState {
    data object Idle : EmojiExportState
    data object Exporting : EmojiExportState
    data class Success(val output: ExportOutput) : EmojiExportState
    data class Error(val message: String) : EmojiExportState
}

class EmojiStudioViewModel(
    private val emojiRepository: EmojiRepository,
    private val projectRepository: ProjectRepository,
    private val exportManager: ExportManager
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(EmojiCategory.SMILEYS)
    val selectedCategory: StateFlow<EmojiCategory> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedEmojis = MutableStateFlow<List<String>>(emptyList())
    val selectedEmojis: StateFlow<List<String>> = _selectedEmojis.asStateFlow()

    private val _exportState = MutableStateFlow<EmojiExportState>(EmojiExportState.Idle)
    val exportState: StateFlow<EmojiExportState> = _exportState.asStateFlow()

    val customEmojis: StateFlow<List<CustomEmojiEntity>> = emojiRepository.customEmojis
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val emojiUsage: StateFlow<List<EmojiUsageEntity>> = emojiRepository.emojiUsage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val catalog: List<EmojiCatalogItem> get() = emojiRepository.catalog

    fun selectCategory(category: EmojiCategory) {
        _selectedCategory.value = category
    }

    fun updateSearch(query: String) {
        _searchQuery.value = query
    }

    fun toggleEmojiInSelection(emojiChar: String) {
        val current = _selectedEmojis.value
        _selectedEmojis.value = if (current.contains(emojiChar)) {
            current - emojiChar
        } else {
            current + emojiChar
        }
        viewModelScope.launch { emojiRepository.markUsed(emojiChar) }
    }

    fun addEmojiTextInput(raw: String) {
        if (raw.isBlank()) return
        val current = _selectedEmojis.value
        _selectedEmojis.value = current + raw.trim()
    }

    fun clearSelection() {
        _selectedEmojis.value = emptyList()
    }

    fun toggleFavorite(emojiChar: String) {
        viewModelScope.launch {
            emojiRepository.toggleFavorite(emojiChar)
        }
    }

    fun importCustomEmoji(uri: Uri, emojiChar: String, shortName: String, packName: String) {
        viewModelScope.launch {
            emojiRepository.importCustomEmojiAsset(uri, emojiChar, shortName, packName)
        }
    }

    fun deleteCustomEmoji(entity: CustomEmojiEntity) {
        viewModelScope.launch {
            emojiRepository.deleteCustomEmoji(entity)
        }
    }

    fun exportEmojiPng(emojiChar: String, transparentBackground: Boolean = true) {
        viewModelScope.launch {
            _exportState.value = EmojiExportState.Exporting
            try {
                val customPath = customEmojis.value.firstOrNull { it.emojiChar == emojiChar }?.assetFilePath
                val out = exportManager.exportEmojiBitmap(
                    emojiChar = emojiChar,
                    customAssetPath = customPath,
                    sizePx = 512,
                    transparentBackground = transparentBackground
                )
                projectRepository.recordExport(
                    projectId = null,
                    projectName = "Emoji $emojiChar",
                    output = out,
                    transparentBackground = transparentBackground
                )
                _exportState.value = EmojiExportState.Success(out)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _exportState.value = EmojiExportState.Error(e.localizedMessage ?: "Emoji export failed.")
            }
        }
    }

    fun resetExportState() {
        _exportState.value = EmojiExportState.Idle
    }

    fun createEmojiArtProject(onCreated: (String) -> Unit) {
        val chars = _selectedEmojis.value.ifEmpty { listOf("✨", "🎨", "💎") }
        viewModelScope.launch {
            val combined = chars.joinToString("  ")
            val doc = CanvasDocument(
                canvasWidth = 1080,
                canvasHeight = 1080,
                background = CanvasBackgroundConfig(
                    type = BackgroundType.GRADIENT,
                    gradientColors = listOf(0xFFF5F5F7L, 0xFFEAF3FFL),
                    gradientAngleDeg = 135f
                ),
                elements = listOf(
                    CanvasElement(
                        id = "emoji_art_card",
                        kind = ElementKind.CARD,
                        xFraction = 0.1f,
                        yFraction = 0.28f,
                        widthFraction = 0.8f,
                        heightFraction = 0.44f,
                        text = combined,
                        font = StudioFont.INTER,
                        fontSizeSp = 44f,
                        fontWeight = 600,
                        textAlign = TextAlignMode.CENTER,
                        textEffect = TextEffectConfig(solidColor = 0xFF1D1D1FL),
                        cardBackgroundColor = 0xFFFFFFFFL,
                        cardCornerRadiusDp = 28f,
                        cardShadowElevationDp = 14f,
                        zIndex = 1
                    )
                )
            )
            val map = customEmojis.value.associate { it.emojiChar to it.assetFilePath }
            val project = projectRepository.createProject(
                name = "Emoji Graphic",
                type = ProjectType.EMOJI_ART,
                document = doc,
                customEmojiPaths = map
            )
            onCreated(project.id)
        }
    }

    companion object {
        fun provideFactory(
            emojiRepository: EmojiRepository,
            projectRepository: ProjectRepository,
            exportManager: ExportManager
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { EmojiStudioViewModel(emojiRepository, projectRepository, exportManager) }
        }
    }
}
