package com.example.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.SettingsRepository
import com.example.data.repository.EmojiRepository
import com.example.data.repository.GeminiAiRepository
import com.example.data.repository.ProjectRepository
import com.example.model.BackgroundType
import com.example.model.ProjectType
import com.example.model.TransformMode
import com.example.model.UserSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ScreenshotAiState {
    data object Idle : ScreenshotAiState
    data class Processing(val statusMessage: String) : ScreenshotAiState
    data class Error(val message: String) : ScreenshotAiState
}

class ScreenshotViewModel(
    private val projectRepository: ProjectRepository,
    private val emojiRepository: EmojiRepository,
    private val settingsRepository: SettingsRepository,
    private val geminiAiRepository: GeminiAiRepository
) : ViewModel() {

    private val _selectedImagePath = MutableStateFlow<String?>(null)
    val selectedImagePath: StateFlow<String?> = _selectedImagePath.asStateFlow()

    private val _selectedMode = MutableStateFlow(TransformMode.PRESERVE_LAYOUT)
    val selectedMode: StateFlow<TransformMode> = _selectedMode.asStateFlow()

    private val _generateAiBackdrop = MutableStateFlow(false)
    val generateAiBackdrop: StateFlow<Boolean> = _generateAiBackdrop.asStateFlow()

    private val _customInstruction = MutableStateFlow("")
    val customInstruction: StateFlow<String> = _customInstruction.asStateFlow()

    private val _aiState = MutableStateFlow<ScreenshotAiState>(ScreenshotAiState.Idle)
    val aiState: StateFlow<ScreenshotAiState> = _aiState.asStateFlow()

    val settings: StateFlow<UserSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    private val customEmojis = emojiRepository.customEmojis
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun importScreenshot(uri: Uri) {
        viewModelScope.launch {
            _aiState.value = ScreenshotAiState.Processing("Preparing screenshot...")
            try {
                val path = projectRepository.importSourceImage(uri)
                if (path != null) {
                    _selectedImagePath.value = path
                    _aiState.value = ScreenshotAiState.Idle
                } else {
                    _aiState.value = ScreenshotAiState.Error("Unsupported or unreadable image format.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiState.value = ScreenshotAiState.Error(e.localizedMessage ?: "Could not load image.")
            }
        }
    }

    fun clearSelectedImage() {
        _selectedImagePath.value = null
        _aiState.value = ScreenshotAiState.Idle
    }

    fun setTransformMode(mode: TransformMode) {
        _selectedMode.value = mode
        if (mode == TransformMode.PRESERVE_LAYOUT) {
            _generateAiBackdrop.value = false
        }
    }

    fun setGenerateAiBackdrop(enabled: Boolean) {
        _generateAiBackdrop.value = enabled
    }

    fun setCustomInstruction(instruction: String) {
        _customInstruction.value = instruction
    }

    fun grantConsentAndAnalyze(onProjectCreated: (String) -> Unit) {
        viewModelScope.launch {
            settingsRepository.setAiUploadConsent(true)
            analyzeAndCreateProject(onProjectCreated)
        }
    }

    fun analyzeAndCreateProject(onProjectCreated: (String) -> Unit) {
        val imagePath = _selectedImagePath.value ?: return
        viewModelScope.launch {
            _aiState.value = ScreenshotAiState.Processing("Analyzing text, Urdu/English script & emojis with Gemini...")
            try {
                val currentSettings = settings.value
                val result = geminiAiRepository.analyzeAndTransformScreenshot(
                    sourceImagePath = imagePath,
                    mode = _selectedMode.value,
                    preferredUrduFont = currentSettings.defaultUrduFont,
                    preferredEnglishFont = currentSettings.defaultEnglishFont,
                    customInstruction = _customInstruction.value
                )

                var finalDoc = result.document
                if (_generateAiBackdrop.value && _selectedMode.value == TransformMode.COMPLETE_REDESIGN) {
                    _aiState.value = ScreenshotAiState.Processing("Generating minimalist iOS backdrop...")
                    val backdropBmp = geminiAiRepository.generateIosBackdrop(result.designSummary)
                    if (backdropBmp != null) {
                        val backdropPath = projectRepository.saveBitmapToInternalFile(
                            bitmap = backdropBmp,
                            folder = "backdrops",
                            prefix = "ios_bg"
                        )
                        backdropBmp.recycle()
                        finalDoc = finalDoc.copy(
                            aiBackdropPath = backdropPath,
                            background = finalDoc.background.copy(
                                type = BackgroundType.IMAGE,
                                imagePath = backdropPath
                            )
                        )
                    }
                }

                val emojiMap = customEmojis.value.associate { it.emojiChar to it.assetFilePath }
                val modeTitle = if (_selectedMode.value == TransformMode.PRESERVE_LAYOUT) "iOS Polish" else "iOS Redesign"
                val created = projectRepository.createProject(
                    name = "$modeTitle ${(System.currentTimeMillis() % 1000)}",
                    type = ProjectType.SCREENSHOT,
                    mode = _selectedMode.value,
                    document = finalDoc,
                    customEmojiPaths = emojiMap
                )
                _aiState.value = ScreenshotAiState.Idle
                onProjectCreated(created.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiState.value = ScreenshotAiState.Error(
                    e.localizedMessage ?: "Gemini analysis failed. Check connection and try again."
                )
            }
        }
    }

    companion object {
        fun provideFactory(
            projectRepository: ProjectRepository,
            emojiRepository: EmojiRepository,
            settingsRepository: SettingsRepository,
            geminiAiRepository: GeminiAiRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ScreenshotViewModel(projectRepository, emojiRepository, settingsRepository, geminiAiRepository)
            }
        }
    }
}
