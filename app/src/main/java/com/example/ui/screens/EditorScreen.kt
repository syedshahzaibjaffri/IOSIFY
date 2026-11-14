package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.local.CustomEmojiEntity
import com.example.engine.CanvasRenderer
import com.example.engine.ExportManager
import com.example.model.ComparisonMode
import com.example.model.EmojiCatalogItem
import com.example.model.ExportFormat
import com.example.model.UserSettings
import com.example.ui.components.ExportBottomSheet
import com.example.ui.components.InteractiveStudioCanvas
import com.example.ui.theme.LocalIosifyColors
import com.example.viewmodel.AiTipState
import com.example.viewmodel.EditorState
import com.example.viewmodel.ExportUiState
import java.io.File
import kotlin.math.roundToInt

private enum class EditorDockTab(val label: String, val icon: ImageVector) {
    TYPOGRAPHY("Typography", Icons.Rounded.TextFields),
    EFFECTS("Effects", Icons.Rounded.AutoAwesome),
    COLORS("Colors", Icons.Rounded.Palette),
    EMOJIS("Emojis", Icons.Rounded.EmojiEmotions),
    BACKGROUND("Background", Icons.Rounded.Wallpaper),
    LAYOUT("Layout", Icons.Rounded.Layers)
}

@Composable
fun EditorScreen(
    state: EditorState,
    exportState: ExportUiState,
    aiTipState: AiTipState,
    isGeneratingBackdrop: Boolean,
    settings: UserSettings,
    customEmojis: List<CustomEmojiEntity>,
    emojiCatalog: List<EmojiCatalogItem>,
    renderer: CanvasRenderer,
    exportManager: ExportManager,
    onBack: () -> Unit,
    onRenameProject: (String) -> Unit,
    onSaveNow: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSelectElement: (String?) -> Unit,
    onSetComparisonMode: (ComparisonMode) -> Unit,
    onUpdateElement: ((com.example.model.CanvasElement) -> com.example.model.CanvasElement) -> Unit,
    onUpdateTextEffect: ((com.example.model.TextEffectConfig) -> com.example.model.TextEffectConfig) -> Unit,
    onUpdateBackground: ((com.example.model.CanvasBackgroundConfig) -> com.example.model.CanvasBackgroundConfig) -> Unit,
    onAddTextElement: () -> Unit,
    onInsertEmojiIntoSelected: (String) -> Unit,
    onAddEmojiSticker: (String) -> Unit,
    onDeleteSelected: () -> Unit,
    onResetBaseline: () -> Unit,
    onGenerateAiBackdrop: () -> Unit,
    onRequestAiTip: () -> Unit,
    onDismissError: () -> Unit,
    onExport: (ExportFormat, Int, Int, Boolean, Int) -> Unit,
    onResetExportState: () -> Unit
) {
    val ext = LocalIosifyColors.current
    val customEmojiMap = remember(customEmojis) { customEmojis.associate { it.emojiChar to it.assetFilePath } }

    var activeTab by remember { mutableStateOf(EditorDockTab.TYPOGRAPHY) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember(state.projectName) { mutableStateOf(state.projectName) }
    var fullScreenPreview by remember { mutableStateOf(false) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var splitFraction by remember { mutableFloatStateOf(0.5f) }

    val hasSourceImage = !state.present.sourceImagePath.isNullOrBlank() &&
        File(state.present.sourceImagePath).exists()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Toolbar: Back | Project Name | Save | Undo | Redo | Export
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        onSaveNow()
                        onBack()
                    },
                    modifier = Modifier.testTag("editor_back_btn")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            renameText = state.projectName
                            showRenameDialog = true
                        }
                        .padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = state.projectName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (state.isDirty) "Saving..." else "Saved",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.isDirty) ext.orange else ext.green
                    )
                }

                IconButton(onClick = onSaveNow, modifier = Modifier.testTag("editor_save_btn")) {
                    Icon(Icons.Rounded.Save, contentDescription = "Save Progress")
                }

                IconButton(
                    onClick = onUndo,
                    enabled = state.canUndo,
                    modifier = Modifier.testTag("editor_undo_btn")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = "Undo")
                }

                IconButton(
                    onClick = onRedo,
                    enabled = state.canRedo,
                    modifier = Modifier.testTag("editor_redo_btn")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Redo, contentDescription = "Redo")
                }

                IconButton(
                    onClick = {
                        onResetExportState()
                        showExportSheet = true
                    },
                    modifier = Modifier.testTag("editor_export_btn")
                ) {
                    Icon(
                        Icons.Rounded.IosShare,
                        contentDescription = "Export Design",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Comparison & Viewport Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasSourceImage) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(ComparisonMode.entries) { mode ->
                        FilterChip(
                            selected = state.comparisonMode == mode,
                            onClick = { onSetComparisonMode(mode) },
                            label = { Text(mode.label, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            } else {
                Text(
                    text = "Pinch to zoom • Drag elements to arrange",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (zoomScale != 1f || panOffset != Offset.Zero) {
                    IconButton(
                        onClick = {
                            zoomScale = 1f
                            panOffset = Offset.Zero
                        }
                    ) {
                        Icon(Icons.Rounded.ZoomOutMap, contentDescription = "Reset Zoom")
                    }
                }
                IconButton(
                    onClick = { fullScreenPreview = true },
                    modifier = Modifier.testTag("editor_fullscreen_btn")
                ) {
                    Icon(Icons.Rounded.Fullscreen, contentDescription = "Full Screen Preview")
                }
            }
        }

        // Center Canvas Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                when (if (hasSourceImage) state.comparisonMode else ComparisonMode.TRANSFORMED) {
                    ComparisonMode.TRANSFORMED -> {
                        InteractiveStudioCanvas(
                            document = state.present,
                            renderer = renderer,
                            customEmojiPaths = customEmojiMap,
                            selectedElementId = state.selectedElementId,
                            zoomScale = zoomScale,
                            panOffset = panOffset,
                            onZoomPanChange = { z, p ->
                                zoomScale = z
                                panOffset = p
                            },
                            onSelectElement = onSelectElement,
                            onMoveElement = { _, dx, dy ->
                                onUpdateElement { el ->
                                    el.copy(
                                        xFraction = (el.xFraction + dx).coerceIn(0f, 1f - el.widthFraction),
                                        yFraction = (el.yFraction + dy).coerceIn(0f, 1f - el.heightFraction)
                                    )
                                }
                            },
                            onResizeElement = { _, dw, dh ->
                                onUpdateElement { el ->
                                    el.copy(
                                        widthFraction = (el.widthFraction + dw).coerceIn(0.12f, 1f - el.xFraction),
                                        heightFraction = (el.heightFraction + dh).coerceIn(0.06f, 1f - el.yFraction)
                                    )
                                }
                            }
                        )
                    }
                    ComparisonMode.ORIGINAL -> {
                        AsyncImage(
                            model = File(state.present.sourceImagePath!!),
                            contentDescription = "Original Screenshot",
                            modifier = Modifier
                                .fillMaxSize(0.92f)
                                .clip(RoundedCornerShape(18.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                    ComparisonMode.SIDE_BY_SIDE -> {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = File(state.present.sourceImagePath!!),
                                contentDescription = "Original Screenshot",
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(0.9f)
                                    .clip(RoundedCornerShape(14.dp)),
                                contentScale = ContentScale.Fit
                            )
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                InteractiveStudioCanvas(
                                    document = state.present,
                                    renderer = renderer,
                                    customEmojiPaths = customEmojiMap,
                                    selectedElementId = null,
                                    zoomScale = 1f,
                                    panOffset = Offset.Zero,
                                    onZoomPanChange = { _, _ -> },
                                    onSelectElement = {},
                                    onMoveElement = { _, _, _ -> },
                                    onResizeElement = { _, _, _ -> },
                                    interactive = false
                                )
                            }
                        }
                    }
                    ComparisonMode.SLIDER -> {
                        BoxWithConstraints(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            val density = LocalDensity.current
                            val totalPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
                            InteractiveStudioCanvas(
                                document = state.present,
                                renderer = renderer,
                                customEmojiPaths = customEmojiMap,
                                selectedElementId = null,
                                zoomScale = 1f,
                                panOffset = Offset.Zero,
                                onZoomPanChange = { _, _ -> },
                                onSelectElement = {},
                                onMoveElement = { _, _, _ -> },
                                onResizeElement = { _, _, _ -> },
                                interactive = false
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(splitFraction)
                                    .fillMaxHeight()
                                    .align(Alignment.CenterStart)
                                    .clipToBounds()
                            ) {
                                AsyncImage(
                                    model = File(state.present.sourceImagePath!!),
                                    contentDescription = "Original Half",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            val handleOffsetX = (splitFraction * totalPx).roundToInt()
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .offset { IntOffset(handleOffsetX - 24, 0) }
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .border(2.dp, Color.White, CircleShape)
                                    .pointerInput(totalPx) {
                                        detectHorizontalDragGestures { change, dragAmount ->
                                            change.consume()
                                            splitFraction = (splitFraction + dragAmount / totalPx).coerceIn(0.1f, 0.9f)
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }

        // Bottom Editing Dock
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(0.8.dp, ext.separator.copy(alpha = 0.45f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(EditorDockTab.entries) { tab ->
                        FilterChip(
                            selected = activeTab == tab,
                            onClick = { activeTab = tab },
                            leadingIcon = {
                                Icon(tab.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text(tab.label) },
                            modifier = Modifier.testTag("editor_tab_${tab.name.lowercase()}")
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(216.dp)
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    when (activeTab) {
                        EditorDockTab.TYPOGRAPHY -> TypographyPanel(
                            selectedElement = state.selectedElement,
                            onUpdateElement = onUpdateElement
                        )
                        EditorDockTab.EFFECTS -> EffectsPanel(
                            selectedElement = state.selectedElement,
                            onUpdateEffect = onUpdateTextEffect,
                            onUpdateElement = onUpdateElement
                        )
                        EditorDockTab.COLORS -> ColorsPanel(
                            selectedElement = state.selectedElement,
                            onUpdateElement = onUpdateElement
                        )
                        EditorDockTab.EMOJIS -> EmojisPanel(
                            catalog = emojiCatalog,
                            customEmojis = customEmojis,
                            onInsertIntoSelected = onInsertEmojiIntoSelected,
                            onAddSticker = onAddEmojiSticker
                        )
                        EditorDockTab.BACKGROUND -> BackgroundPanel(
                            background = state.present.background,
                            isGeneratingBackdrop = isGeneratingBackdrop,
                            onUpdateBackground = onUpdateBackground,
                            onGenerateAiBackdrop = onGenerateAiBackdrop
                        )
                        EditorDockTab.LAYOUT -> LayoutPanel(
                            selectedElement = state.selectedElement,
                            aiTipState = aiTipState,
                            onUpdateElement = onUpdateElement,
                            onAddTextElement = onAddTextElement,
                            onDeleteSelected = onDeleteSelected,
                            onResetProject = onResetBaseline,
                            onRequestAiTip = onRequestAiTip
                        )
                    }
                }
            }
        }
    }

    // Export Modal Bottom Sheet
    if (showExportSheet) {
        ExportBottomSheet(
            document = state.present,
            defaultFormat = settings.defaultExportFormat,
            defaultQuality = settings.exportQuality,
            exportState = exportState,
            exportManager = exportManager,
            onExport = onExport,
            onDismiss = { showExportSheet = false }
        )
    }

    // Inline Rename Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Design") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text("Project Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onRenameProject(renameText)
                    showRenameDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Error Alert
    state.errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text("Notice") },
            text = { Text(msg) },
            confirmButton = {
                Button(onClick = onDismissError) { Text("OK") }
            }
        )
    }

    // Full-Screen Preview Modal
    if (fullScreenPreview) {
        Dialog(
            onDismissRequest = { fullScreenPreview = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(16.dp)
            ) {
                InteractiveStudioCanvas(
                    document = state.present,
                    renderer = renderer,
                    customEmojiPaths = customEmojiMap,
                    selectedElementId = null,
                    zoomScale = 1f,
                    panOffset = Offset.Zero,
                    onZoomPanChange = { _, _ -> },
                    onSelectElement = {},
                    onMoveElement = { _, _, _ -> },
                    onResizeElement = { _, _, _ -> },
                    interactive = false
                )
                IconButton(
                    onClick = { fullScreenPreview = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close Full Screen", tint = Color.White)
                }
            }
        }
    }
}
