package com.example.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.CanvasJsonSerializer
import com.example.data.local.CustomEmojiEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.SettingsRepository
import com.example.data.repository.EmojiRepository
import com.example.data.repository.GeminiAiRepository
import com.example.data.repository.ProjectRepository
import com.example.engine.ExportManager
import com.example.engine.ExportOutput
import com.example.model.BackgroundType
import com.example.model.CanvasBackgroundConfig
import com.example.model.CanvasDocument
import com.example.model.CanvasElement
import com.example.model.ComparisonMode
import com.example.model.ElementKind
import com.example.model.ExportFormat
import com.example.model.StudioFont
import com.example.model.TextEffectConfig
import com.example.model.TransformMode
import com.example.model.UserSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface ExportUiState {
    data object Idle : ExportUiState
    data object Exporting : ExportUiState
    data class Success(val output: ExportOutput) : ExportUiState
    data class Error(val message: String) : ExportUiState
}

sealed interface AiTipState {
    data object Idle : AiTipState
    data class Streaming(val text: String) : AiTipState
    data class Ready(val text: String) : AiTipState
    data class Error(val message: String) : AiTipState
}

data class EditorState(
    val isLoading: Boolean = true,
    val projectId: String = "",
    val projectName: String = "Untitled",
    val transformMode: TransformMode = TransformMode.PRESERVE_LAYOUT,
    val past: List<CanvasDocument> = emptyList(),
    val present: CanvasDocument = CanvasDocument(),
    val future: List<CanvasDocument> = emptyList(),
    val baseline: CanvasDocument = CanvasDocument(),
    val selectedElementId: String? = null,
    val comparisonMode: ComparisonMode = ComparisonMode.TRANSFORMED,
    val isDirty: Boolean = false,
    val lastSavedAt: Long = 0L,
    val errorMessage: String? = null
) {
    val canUndo: Boolean get() = past.isNotEmpty()
    val canRedo: Boolean get() = future.isNotEmpty()
    val selectedElement: CanvasElement?
        get() = present.elements.firstOrNull { it.id == selectedElementId }
}

class EditorViewModel(
    private val projectId: String,
    private val projectRepository: ProjectRepository,
    private val emojiRepository: EmojiRepository,
    private val settingsRepository: SettingsRepository,
    private val exportManager: ExportManager,
    private val geminiAiRepository: GeminiAiRepository
) : ViewModel() {

    private val _editorState = MutableStateFlow(EditorState(projectId = projectId))
    val editorState: StateFlow<EditorState> = _editorState.asStateFlow()

    private val _exportState = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val exportState: StateFlow<ExportUiState> = _exportState.asStateFlow()

    private val _aiTipState = MutableStateFlow<AiTipState>(AiTipState.Idle)
    val aiTipState: StateFlow<AiTipState> = _aiTipState.asStateFlow()

    private val _isGeneratingBackdrop = MutableStateFlow(false)
    val isGeneratingBackdrop: StateFlow<Boolean> = _isGeneratingBackdrop.asStateFlow()

    val settings: StateFlow<UserSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    val customEmojis: StateFlow<List<CustomEmojiEntity>> = emojiRepository.customEmojis
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var loadedEntity: ProjectEntity? = null
    private var autoSaveJob: Job? = null

    init {
        loadProject()
    }

    private fun loadProject() {
        viewModelScope.launch {
            val entity = projectRepository.getProject(projectId)
            if (entity == null) {
                _editorState.value = _editorState.value.copy(
                    isLoading = false,
                    errorMessage = "Project not found."
                )
                return@launch
            }
            loadedEntity = entity
            val doc = CanvasJsonSerializer.fromJson(entity.canvasJson)
            val mode = runCatching { TransformMode.valueOf(entity.mode) }
                .getOrDefault(TransformMode.PRESERVE_LAYOUT)
            _editorState.value = EditorState(
                isLoading = false,
                projectId = entity.id,
                projectName = entity.name,
                transformMode = mode,
                present = doc,
                baseline = doc,
                selectedElementId = doc.elements.firstOrNull()?.id,
                lastSavedAt = entity.updatedAt
            )
        }
    }

    fun selectElement(id: String?) {
        _editorState.value = _editorState.value.copy(selectedElementId = id)
    }

    fun setComparisonMode(mode: ComparisonMode) {
        _editorState.value = _editorState.value.copy(comparisonMode = mode)
    }

    fun renameProject(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        _editorState.value = _editorState.value.copy(projectName = trimmed, isDirty = true)
        scheduleAutoSave()
    }

    fun updateSelectedElement(recordUndo: Boolean = true, transform: (CanvasElement) -> CanvasElement) {
        val state = _editorState.value
        val targetId = state.selectedElementId ?: return
        val updatedElements = state.present.elements.map { el ->
            if (el.id == targetId) transform(el) else el
        }
        applyDocumentChange(state.present.copy(elements = updatedElements), recordUndo)
    }

    fun updateSelectedTextEffect(recordUndo: Boolean = true, transform: (TextEffectConfig) -> TextEffectConfig) {
        updateSelectedElement(recordUndo) { el ->
            el.copy(textEffect = transform(el.textEffect))
        }
    }

    fun updateBackground(recordUndo: Boolean = true, transform: (CanvasBackgroundConfig) -> CanvasBackgroundConfig) {
        val state = _editorState.value
        applyDocumentChange(state.present.copy(background = transform(state.present.background)), recordUndo)
    }

    fun addTextElement(initialText: String = "New iOS Text ✨") {
        val state = _editorState.value
        val newId = "el_${UUID.randomUUID().toString().take(6)}"
        val maxZ = (state.present.elements.maxOfOrNull { it.zIndex } ?: 0) + 1
        val newEl = CanvasElement(
            id = newId,
            kind = ElementKind.CARD,
            xFraction = 0.12f,
            yFraction = 0.35f,
            widthFraction = 0.76f,
            heightFraction = 0.22f,
            text = initialText,
            font = settings.value.defaultEnglishFont,
            fontSizeSp = 22f,
            fontWeight = 600,
            zIndex = maxZ
        )
        applyDocumentChange(state.present.copy(elements = state.present.elements + newEl), true)
        _editorState.value = _editorState.value.copy(selectedElementId = newId)
    }

    fun addEmojiSticker(emojiChar: String) {
        val state = _editorState.value
        val newId = "em_${UUID.randomUUID().toString().take(6)}"
        val maxZ = (state.present.elements.maxOfOrNull { it.zIndex } ?: 0) + 1
        val newEl = CanvasElement(
            id = newId,
            kind = ElementKind.EMOJI,
            xFraction = 0.38f,
            yFraction = 0.38f,
            widthFraction = 0.24f,
            heightFraction = 0.18f,
            text = emojiChar,
            emojiChar = emojiChar,
            cardBackgroundColor = 0x00000000L,
            zIndex = maxZ
        )
        applyDocumentChange(state.present.copy(elements = state.present.elements + newEl), true)
        _editorState.value = _editorState.value.copy(selectedElementId = newId)
        viewModelScope.launch { emojiRepository.markUsed(emojiChar) }
    }

    fun insertEmojiIntoSelectedElement(emojiChar: String) {
        val selected = _editorState.value.selectedElement ?: run {
            addEmojiSticker(emojiChar)
            return
        }
        if (selected.kind == ElementKind.EMOJI) {
            updateSelectedElement { it.copy(emojiChar = emojiChar, text = emojiChar) }
        } else {
            updateSelectedElement { it.copy(text = "${it.text} $emojiChar".trim()) }
        }
        viewModelScope.launch { emojiRepository.markUsed(emojiChar) }
    }

    fun deleteSelectedElement() {
        val state = _editorState.value
        val targetId = state.selectedElementId ?: return
        val remaining = state.present.elements.filterNot { it.id == targetId }
        applyDocumentChange(state.present.copy(elements = remaining), true)
        _editorState.value = _editorState.value.copy(selectedElementId = remaining.firstOrNull()?.id)
    }

    fun undo() {
        val state = _editorState.value
        if (state.past.isEmpty()) return
        val previous = state.past.last()
        val newPast = state.past.dropLast(1)
        _editorState.value = state.copy(
            past = newPast,
            present = previous,
            future = listOf(state.present) + state.future,
            isDirty = true
        )
        scheduleAutoSave()
    }

    fun redo() {
        val state = _editorState.value
        if (state.future.isEmpty()) return
        val next = state.future.first()
        val newFuture = state.future.drop(1)
        _editorState.value = state.copy(
            past = state.past + state.present,
            present = next,
            future = newFuture,
            isDirty = true
        )
        scheduleAutoSave()
    }

    fun resetToBaseline() {
        val state = _editorState.value
        applyDocumentChange(state.baseline, true)
    }

    fun saveNow() {
        autoSaveJob?.cancel()
        viewModelScope.launch {
            persistCurrentState()
        }
    }

    fun generateAiBackdropForCanvas() {
        val state = _editorState.value
        viewModelScope.launch {
            _isGeneratingBackdrop.value = true
            try {
                val contextText = state.present.elements.joinToString(" ") { it.text }.take(140)
                    .ifBlank { "Minimalist Apple studio aesthetic" }
                val bmp: Bitmap? = geminiAiRepository.generateIosBackdrop(contextText)
                if (bmp != null) {
                    val path = projectRepository.saveBitmapToInternalFile(bmp, "backdrops", "ai_bg")
                    bmp.recycle()
                    val updatedDoc = _editorState.value.present.copy(
                        aiBackdropPath = path,
                        background = _editorState.value.present.background.copy(
                            type = BackgroundType.IMAGE,
                            imagePath = path
                        )
                    )
                    applyDocumentChange(updatedDoc, true)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _editorState.value = _editorState.value.copy(
                    errorMessage = e.localizedMessage ?: "AI backdrop generation failed."
                )
            } finally {
                _isGeneratingBackdrop.value = false
            }
        }
    }

    fun requestAiDesignTip() {
        val sample = _editorState.value.present.elements.joinToString(" ") { it.text }.take(200)
            .ifBlank { "iOSify Studio typography poster" }
        viewModelScope.launch {
            _aiTipState.value = AiTipState.Streaming("Thinking...")
            try {
                var latest = ""
                geminiAiRepository.streamDesignTips(sample).collect { partial ->
                    latest = partial
                    _aiTipState.value = AiTipState.Streaming(partial)
                }
                _aiTipState.value = AiTipState.Ready(latest.ifBlank { "Use clean high-contrast typography with generous card padding." })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiTipState.value = AiTipState.Error(e.localizedMessage ?: "Could not fetch AI design tips.")
            }
        }
    }

    fun dismissError() {
        _editorState.value = _editorState.value.copy(errorMessage = null)
    }

    fun exportProject(
        format: ExportFormat,
        width: Int,
        height: Int,
        transparentBackground: Boolean,
        quality: Int
    ) {
        val state = _editorState.value
        viewModelScope.launch {
            _exportState.value = ExportUiState.Exporting
            try {
                persistCurrentState()
                val emojiPaths = customEmojis.value.associate { it.emojiChar to it.assetFilePath }
                val output = exportManager.exportDocument(
                    document = state.present,
                    projectName = state.projectName,
                    format = format,
                    targetWidth = width,
                    targetHeight = height,
                    transparentBackground = transparentBackground,
                    quality = quality,
                    customEmojiPaths = emojiPaths,
                    saveToGallery = true
                )
                projectRepository.recordExport(
                    projectId = state.projectId,
                    projectName = state.projectName,
                    output = output,
                    transparentBackground = transparentBackground && format != ExportFormat.JPG
                )
                _exportState.value = ExportUiState.Success(output)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _exportState.value = ExportUiState.Error(e.localizedMessage ?: "Export failed.")
            }
        }
    }

    fun resetExportState() {
        _exportState.value = ExportUiState.Idle
    }

    private fun applyDocumentChange(newDoc: CanvasDocument, recordUndo: Boolean) {
        val state = _editorState.value
        if (newDoc == state.present) return
        val newPast = if (recordUndo) (state.past + state.present).takeLast(30) else state.past
        _editorState.value = state.copy(
            past = newPast,
            present = newDoc,
            future = if (recordUndo) emptyList() else state.future,
            isDirty = true
        )
        scheduleAutoSave()
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(850L)
            persistCurrentState()
        }
    }

    private suspend fun persistCurrentState() {
        val state = _editorState.value
        val emojiMap = customEmojis.value.associate { it.emojiChar to it.assetFilePath }
        val updated = projectRepository.saveProgress(
            projectId = state.projectId,
            name = state.projectName,
            mode = state.transformMode,
            document = state.present,
            customEmojiPaths = emojiMap
        )
        if (updated != null) {
            _editorState.value = _editorState.value.copy(
                isDirty = false,
                lastSavedAt = updated.updatedAt
            )
        }
    }

    companion object {
        fun provideFactory(
            projectId: String,
            projectRepository: ProjectRepository,
            emojiRepository: EmojiRepository,
            settingsRepository: SettingsRepository,
            exportManager: ExportManager,
            geminiAiRepository: GeminiAiRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                EditorViewModel(
                    projectId = projectId,
                    projectRepository = projectRepository,
                    emojiRepository = emojiRepository,
                    settingsRepository = settingsRepository,
                    exportManager = exportManager,
                    geminiAiRepository = geminiAiRepository
                )
            }
        }
    }
}
