package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.CustomEmojiEntity
import com.example.data.repository.TemplateLibrary
import com.example.engine.CanvasRenderer
import com.example.engine.LanguageDetector
import com.example.model.BackgroundType
import com.example.model.CanvasBackgroundConfig
import com.example.model.CanvasDocument
import com.example.model.CanvasElement
import com.example.model.ElementKind
import com.example.model.StudioFont
import com.example.model.TextAlignMode
import com.example.model.TextDirectionMode
import com.example.model.TextEffectConfig
import com.example.model.TextFillType
import com.example.model.TextTemplatePreset
import com.example.model.UserSettings
import com.example.ui.components.ColorSwatchRow
import com.example.ui.components.InteractiveStudioCanvas
import com.example.ui.components.IosifySectionCard
import com.example.ui.components.LabeledSlider
import com.example.ui.theme.LocalIosifyColors

@Composable
fun TextStudioScreen(
    settings: UserSettings,
    renderer: CanvasRenderer,
    customEmojis: List<CustomEmojiEntity>,
    onCreateFromTemplate: (TextTemplatePreset, String?) -> Unit,
    onCreateCustomDesign: (String, CanvasDocument) -> Unit
) {
    val ext = LocalIosifyColors.current
    val customEmojiMap = remember(customEmojis) { customEmojis.associate { it.emojiChar to it.assetFilePath } }

    var textInput by remember {
        mutableStateOf("خودی کو کر بلند اتنا ✨\nCreate timeless Urdu & English typography.")
    }
    var directionMode by remember { mutableStateOf(TextDirectionMode.AUTO) }
    var selectedFont by remember { mutableStateOf(StudioFont.NOTO_NASTALIQ_URDU) }
    var useGradientFill by remember { mutableStateOf(true) }
    var solidColor by remember { mutableStateOf(0xFF1D1D1FL) }
    var gradColorA by remember { mutableStateOf(0xFF007AFFL) }
    var gradColorB by remember { mutableStateOf(0xFFAF52DEL) }
    var gradientAngle by remember { mutableFloatStateOf(40f) }
    var outlineEnabled by remember { mutableStateOf(true) }
    var outlineColor by remember { mutableStateOf(0xFFFFFFFFL) }
    var outlineWidth by remember { mutableFloatStateOf(2.5f) }
    var shadowEnabled by remember { mutableStateOf(true) }
    var shadowBlur by remember { mutableFloatStateOf(10f) }

    val detectedRtl = remember(textInput, directionMode) {
        LanguageDetector.resolveIsRtl(textInput, directionMode)
    }

    val previewDocument = remember(
        textInput,
        directionMode,
        selectedFont,
        useGradientFill,
        solidColor,
        gradColorA,
        gradColorB,
        gradientAngle,
        outlineEnabled,
        outlineColor,
        outlineWidth,
        shadowEnabled,
        shadowBlur
    ) {
        CanvasDocument(
            canvasWidth = 1080,
            canvasHeight = 900,
            background = CanvasBackgroundConfig(
                type = BackgroundType.GRADIENT,
                gradientColors = listOf(0xFFF5F5F7L, 0xFFEAF3FFL),
                gradientAngleDeg = 135f
            ),
            elements = listOf(
                CanvasElement(
                    id = "live_preview",
                    kind = ElementKind.CARD,
                    xFraction = 0.07f,
                    yFraction = 0.14f,
                    widthFraction = 0.86f,
                    heightFraction = 0.72f,
                    text = textInput.ifBlank { "Type English or Urdu text..." },
                    font = selectedFont,
                    fontSizeSp = 27f,
                    fontWeight = 700,
                    lineHeightMultiplier = if (selectedFont == StudioFont.NOTO_NASTALIQ_URDU) 1.85f else 1.4f,
                    textAlign = TextAlignMode.CENTER,
                    directionMode = directionMode,
                    textEffect = TextEffectConfig(
                        fillType = if (useGradientFill) TextFillType.GRADIENT else TextFillType.SOLID,
                        solidColor = solidColor,
                        gradientColors = listOf(gradColorA, gradColorB),
                        gradientAngleDeg = gradientAngle,
                        outlineEnabled = outlineEnabled,
                        outlineColor = outlineColor,
                        outlineWidthDp = outlineWidth,
                        shadowEnabled = shadowEnabled,
                        shadowColor = 0x66000000L,
                        shadowBlurRadius = shadowBlur,
                        shadowOffsetX = 0f,
                        shadowOffsetY = 5f
                    ),
                    cardBackgroundColor = 0xFFFFFFFFL,
                    cardCornerRadiusDp = 26f,
                    cardShadowElevationDp = 14f,
                    cardPaddingDp = 24f,
                    zIndex = 1
                )
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 820.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Text Studio", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Bilingual English & Urdu Nastaliq/Naskh typography with gradient fills, customizable outlines, and drop shadows.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Live Bilingual Canvas Preview
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                ) {
                    InteractiveStudioCanvas(
                        document = previewDocument,
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

            // Text Input + Direction + Font
            item {
                IosifySectionCard(title = "Bilingual Text & Direction") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            label = { Text("English, Urdu, or Mixed Text") },
                            minLines = 2,
                            maxLines = 5,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("text_studio_input")
                        )

                        Text(
                            text = "Active Direction: ${if (detectedRtl) "RTL (Urdu/Arabic)" else "LTR (English)"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextDirectionMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = directionMode == mode,
                                    onClick = { directionMode = mode },
                                    label = { Text(mode.label) },
                                    modifier = Modifier.testTag("dir_chip_${mode.name.lowercase()}")
                                )
                            }
                        }

                        Text("Typography Family", style = MaterialTheme.typography.titleMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(StudioFont.entries) { font ->
                                FilterChip(
                                    selected = selectedFont == font,
                                    onClick = { selectedFont = font },
                                    label = { Text(font.displayName) },
                                    modifier = Modifier.testTag("font_chip_${font.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }

            // Advanced Text Effects Quick Lab
            item {
                IosifySectionCard(title = "Advanced Text Effects") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Gradient Text Fill", style = MaterialTheme.typography.bodyLarge)
                            Switch(
                                checked = useGradientFill,
                                onCheckedChange = { useGradientFill = it },
                                modifier = Modifier.testTag("ts_gradient_switch")
                            )
                        }
                        if (useGradientFill) {
                            Text("Start Color", style = MaterialTheme.typography.labelMedium)
                            ColorSwatchRow(selectedColor = gradColorA, onColorSelected = { gradColorA = it })
                            Text("End Color", style = MaterialTheme.typography.labelMedium)
                            ColorSwatchRow(selectedColor = gradColorB, onColorSelected = { gradColorB = it })
                            LabeledSlider(
                                label = "Gradient Angle",
                                valueText = "${gradientAngle.toInt()}°",
                                value = gradientAngle,
                                valueRange = 0f..360f,
                                onValueChange = { gradientAngle = it }
                            )
                        } else {
                            ColorSwatchRow(selectedColor = solidColor, onColorSelected = { solidColor = it })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Text Outline (Stroke)", style = MaterialTheme.typography.bodyLarge)
                            Switch(
                                checked = outlineEnabled,
                                onCheckedChange = { outlineEnabled = it },
                                modifier = Modifier.testTag("ts_outline_switch")
                            )
                        }
                        if (outlineEnabled) {
                            ColorSwatchRow(selectedColor = outlineColor, onColorSelected = { outlineColor = it })
                            LabeledSlider(
                                label = "Outline Thickness",
                                valueText = "${String.format("%.1f", outlineWidth)} dp",
                                value = outlineWidth,
                                valueRange = 0.5f..10f,
                                onValueChange = { outlineWidth = it }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Adjustable Drop Shadow", style = MaterialTheme.typography.bodyLarge)
                            Switch(
                                checked = shadowEnabled,
                                onCheckedChange = { shadowEnabled = it },
                                modifier = Modifier.testTag("ts_shadow_switch")
                            )
                        }
                        if (shadowEnabled) {
                            LabeledSlider(
                                label = "Shadow Blur Radius",
                                valueText = "${shadowBlur.toInt()} px",
                                value = shadowBlur,
                                valueRange = 1f..28f,
                                onValueChange = { shadowBlur = it }
                            )
                        }

                        Button(
                            onClick = {
                                val title = textInput.lineSequence().firstOrNull()?.take(24) ?: "Text Graphic"
                                onCreateCustomDesign(title, previewDocument.copy(canvasHeight = 1350))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("ts_open_editor_btn"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.Edit, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open in Full Canvas Editor")
                        }
                    }
                }
            }

            // 7 Ready Templates
            item {
                Text(
                    text = "iOS Text Templates",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            items(TemplateLibrary.presets, key = { it.id }) { preset ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.8.dp, ext.separator.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .testTag("template_${preset.id}")
                        .clickable { onCreateFromTemplate(preset, null) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                preset.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                preset.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                preset.sampleText,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = "Use Template",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
