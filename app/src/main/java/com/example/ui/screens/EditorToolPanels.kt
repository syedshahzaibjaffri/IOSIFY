package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CustomEmojiEntity
import com.example.model.BackgroundType
import com.example.model.CanvasBackgroundConfig
import com.example.model.CanvasElement
import com.example.model.EmojiCatalogItem
import com.example.model.StudioFont
import com.example.model.TextAlignMode
import com.example.model.TextDirectionMode
import com.example.model.TextEffectConfig
import com.example.model.TextFillType
import com.example.ui.components.ColorSwatchRow
import com.example.ui.components.LabeledSlider
import com.example.ui.components.StudioPaletteColors
import com.example.viewmodel.AiTipState

@Composable
fun TypographyPanel(
    selectedElement: CanvasElement?,
    onUpdateElement: ((CanvasElement) -> CanvasElement) -> Unit
) {
    if (selectedElement == null) {
        Text(
            "Tap any text or card on the canvas to edit its typography.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(8.dp)
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = selectedElement.text,
            onValueChange = { txt -> onUpdateElement { it.copy(text = txt) } },
            label = { Text("Text Content (English / اردو)") },
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("editor_text_input")
        )

        Text("Font Family", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(StudioFont.entries) { font ->
                FilterChip(
                    selected = selectedElement.font == font,
                    onClick = { onUpdateElement { it.copy(font = font) } },
                    label = { Text(font.displayName) }
                )
            }
        }

        LabeledSlider(
            label = "Font Size",
            valueText = "${selectedElement.fontSizeSp.toInt()} sp",
            value = selectedElement.fontSizeSp,
            valueRange = 12f..64f,
            onValueChange = { sz -> onUpdateElement { it.copy(fontSizeSp = sz) } }
        )

        Text("Font Weight", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(400 to "Regular", 500 to "Medium", 600 to "Semibold", 700 to "Bold").forEach { (w, label) ->
                FilterChip(
                    selected = selectedElement.fontWeight == w,
                    onClick = { onUpdateElement { it.copy(fontWeight = w) } },
                    label = { Text(label) }
                )
            }
        }

        Text("Direction & Alignment", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TextDirectionMode.entries.forEach { dir ->
                FilterChip(
                    selected = selectedElement.directionMode == dir,
                    onClick = { onUpdateElement { it.copy(directionMode = dir) } },
                    label = { Text(dir.label) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TextAlignMode.entries.forEach { align ->
                FilterChip(
                    selected = selectedElement.textAlign == align,
                    onClick = { onUpdateElement { it.copy(textAlign = align) } },
                    label = { Text(align.label) }
                )
            }
        }

        LabeledSlider(
            label = "Line Height",
            valueText = String.format("%.2fx", selectedElement.lineHeightMultiplier),
            value = selectedElement.lineHeightMultiplier,
            valueRange = 1.0f..2.4f,
            onValueChange = { lh -> onUpdateElement { it.copy(lineHeightMultiplier = lh) } }
        )
        LabeledSlider(
            label = "Letter Spacing",
            valueText = String.format("%.2f em", selectedElement.letterSpacingEm),
            value = selectedElement.letterSpacingEm,
            valueRange = -0.05f..0.25f,
            onValueChange = { ls -> onUpdateElement { it.copy(letterSpacingEm = ls) } }
        )
    }
}

@Composable
fun EffectsPanel(
    selectedElement: CanvasElement?,
    onUpdateEffect: ((TextEffectConfig) -> TextEffectConfig) -> Unit,
    onUpdateElement: ((CanvasElement) -> CanvasElement) -> Unit
) {
    if (selectedElement == null) {
        Text(
            "Select a text or card element to customize gradient fills, outlines, and drop shadows.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(8.dp)
        )
        return
    }
    val effect = selectedElement.textEffect
    val gradA = effect.gradientColors.getOrNull(0) ?: 0xFF007AFFL
    val gradB = effect.gradientColors.getOrNull(1) ?: 0xFFAF52DEL

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Text Fill Mode", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = effect.fillType == TextFillType.SOLID,
                onClick = { onUpdateEffect { it.copy(fillType = TextFillType.SOLID) } },
                label = { Text("Solid Color") },
                modifier = Modifier.testTag("effect_fill_solid")
            )
            FilterChip(
                selected = effect.fillType == TextFillType.GRADIENT,
                onClick = { onUpdateEffect { it.copy(fillType = TextFillType.GRADIENT) } },
                label = { Text("Gradient Fill") },
                modifier = Modifier.testTag("effect_fill_gradient")
            )
        }

        if (effect.fillType == TextFillType.SOLID) {
            Text("Text Color", style = MaterialTheme.typography.labelMedium)
            ColorSwatchRow(
                selectedColor = effect.solidColor,
                onColorSelected = { col -> onUpdateEffect { it.copy(solidColor = col) } }
            )
        } else {
            Text("Gradient Start Color", style = MaterialTheme.typography.labelMedium)
            ColorSwatchRow(
                selectedColor = gradA,
                onColorSelected = { col ->
                    onUpdateEffect { it.copy(gradientColors = listOf(col, gradB)) }
                }
            )
            Text("Gradient End Color", style = MaterialTheme.typography.labelMedium)
            ColorSwatchRow(
                selectedColor = gradB,
                onColorSelected = { col ->
                    onUpdateEffect { it.copy(gradientColors = listOf(gradA, col)) }
                }
            )
            LabeledSlider(
                label = "Gradient Angle",
                valueText = "${effect.gradientAngleDeg.toInt()}°",
                value = effect.gradientAngleDeg,
                valueRange = 0f..360f,
                onValueChange = { ang -> onUpdateEffect { it.copy(gradientAngleDeg = ang) } }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Text Outline (Stroke)", style = MaterialTheme.typography.titleMedium)
            Switch(
                checked = effect.outlineEnabled,
                onCheckedChange = { enabled -> onUpdateEffect { it.copy(outlineEnabled = enabled) } },
                modifier = Modifier.testTag("effect_outline_switch")
            )
        }
        if (effect.outlineEnabled) {
            ColorSwatchRow(
                selectedColor = effect.outlineColor,
                onColorSelected = { col -> onUpdateEffect { it.copy(outlineColor = col) } }
            )
            LabeledSlider(
                label = "Outline Thickness",
                valueText = String.format("%.1f dp", effect.outlineWidthDp),
                value = effect.outlineWidthDp,
                valueRange = 0.5f..10f,
                onValueChange = { w -> onUpdateEffect { it.copy(outlineWidthDp = w) } }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Drop Shadow", style = MaterialTheme.typography.titleMedium)
            Switch(
                checked = effect.shadowEnabled,
                onCheckedChange = { enabled -> onUpdateEffect { it.copy(shadowEnabled = enabled) } },
                modifier = Modifier.testTag("effect_shadow_switch")
            )
        }
        if (effect.shadowEnabled) {
            ColorSwatchRow(
                selectedColor = effect.shadowColor,
                colors = listOf(0x66000000L, 0xAA000000L, 0x88007AFFL, 0x88AF52DEL, 0x88FFFFFFL),
                onColorSelected = { col -> onUpdateEffect { it.copy(shadowColor = col) } }
            )
            LabeledSlider(
                label = "Shadow Blur",
                valueText = "${effect.shadowBlurRadius.toInt()} px",
                value = effect.shadowBlurRadius,
                valueRange = 1f..28f,
                onValueChange = { b -> onUpdateEffect { it.copy(shadowBlurRadius = b) } }
            )
            LabeledSlider(
                label = "Shadow X Offset",
                valueText = "${effect.shadowOffsetX.toInt()} px",
                value = effect.shadowOffsetX,
                valueRange = -20f..20f,
                onValueChange = { x -> onUpdateEffect { it.copy(shadowOffsetX = x) } }
            )
            LabeledSlider(
                label = "Shadow Y Offset",
                valueText = "${effect.shadowOffsetY.toInt()} px",
                value = effect.shadowOffsetY,
                valueRange = -20f..20f,
                onValueChange = { y -> onUpdateEffect { it.copy(shadowOffsetY = y) } }
            )
        }

        LabeledSlider(
            label = "Card Shadow Elevation",
            valueText = "${selectedElement.cardShadowElevationDp.toInt()} dp",
            value = selectedElement.cardShadowElevationDp,
            valueRange = 0f..28f,
            onValueChange = { elev -> onUpdateElement { it.copy(cardShadowElevationDp = elev) } }
        )
    }
}

@Composable
fun ColorsPanel(
    selectedElement: CanvasElement?,
    onUpdateElement: ((CanvasElement) -> CanvasElement) -> Unit
) {
    if (selectedElement == null) {
        Text(
            "Select a card or text element to adjust surface and border colors.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(8.dp)
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Card Surface Color", style = MaterialTheme.typography.labelLarge)
        ColorSwatchRow(
            selectedColor = selectedElement.cardBackgroundColor,
            colors = listOf(0x00000000L, 0xEBFFFFFFL, 0xCC1C1C1EL) + StudioPaletteColors,
            onColorSelected = { col -> onUpdateElement { it.copy(cardBackgroundColor = col) } }
        )

        Text("Card Border Color", style = MaterialTheme.typography.labelLarge)
        ColorSwatchRow(
            selectedColor = selectedElement.cardBorderColor,
            colors = listOf(0x00000000L, 0x33D2D2D7L, 0x66FFFFFFL, 0xFF007AFFL, 0xFFAF52DEL),
            onColorSelected = { col -> onUpdateElement { it.copy(cardBorderColor = col) } }
        )

        LabeledSlider(
            label = "Border Width",
            valueText = String.format("%.1f dp", selectedElement.cardBorderWidthDp),
            value = selectedElement.cardBorderWidthDp,
            valueRange = 0f..5f,
            onValueChange = { bw -> onUpdateElement { it.copy(cardBorderWidthDp = bw) } }
        )
    }
}

@Composable
fun EmojisPanel(
    catalog: List<EmojiCatalogItem>,
    customEmojis: List<CustomEmojiEntity>,
    onInsertIntoSelected: (String) -> Unit,
    onAddSticker: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Insert into Selected Text / Replace", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(catalog.take(28), key = { it.char }) { item ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { onInsertIntoSelected(item.char) }
                ) {
                    Text(item.char, fontSize = 24.sp, modifier = Modifier.padding(10.dp))
                }
            }
        }

        Text("Add Floating Emoji Sticker", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(catalog.drop(28).take(20), key = { "stk_${it.char}" }) { item ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { onAddSticker(item.char) }
                ) {
                    Text(item.char, fontSize = 24.sp, modifier = Modifier.padding(10.dp))
                }
            }
        }

        if (customEmojis.isNotEmpty()) {
            Text(
                "Custom Imported Packs (${customEmojis.size} active — auto-replaced in text!)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun BackgroundPanel(
    background: CanvasBackgroundConfig,
    isGeneratingBackdrop: Boolean,
    onUpdateBackground: ((CanvasBackgroundConfig) -> CanvasBackgroundConfig) -> Unit,
    onGenerateAiBackdrop: () -> Unit
) {
    val bgA = background.gradientColors.getOrNull(0) ?: 0xFFEAF3FFL
    val bgB = background.gradientColors.getOrNull(1) ?: 0xFFF3EBFFL

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                BackgroundType.SOLID to "Solid",
                BackgroundType.GRADIENT to "Gradient",
                BackgroundType.TRANSPARENT to "Transparent"
            ).forEach { (type, label) ->
                FilterChip(
                    selected = background.type == type,
                    onClick = { onUpdateBackground { it.copy(type = type) } },
                    label = { Text(label) }
                )
            }
        }

        if (background.type == BackgroundType.SOLID) {
            ColorSwatchRow(
                selectedColor = background.solidColor,
                onColorSelected = { col -> onUpdateBackground { it.copy(solidColor = col) } }
            )
        } else if (background.type == BackgroundType.GRADIENT) {
            Text("Start Gradient", style = MaterialTheme.typography.labelMedium)
            ColorSwatchRow(
                selectedColor = bgA,
                onColorSelected = { col -> onUpdateBackground { it.copy(gradientColors = listOf(col, bgB)) } }
            )
            Text("End Gradient", style = MaterialTheme.typography.labelMedium)
            ColorSwatchRow(
                selectedColor = bgB,
                onColorSelected = { col -> onUpdateBackground { it.copy(gradientColors = listOf(bgA, col)) } }
            )
            LabeledSlider(
                label = "Angle",
                valueText = "${background.gradientAngleDeg.toInt()}°",
                value = background.gradientAngleDeg,
                valueRange = 0f..360f,
                onValueChange = { ang -> onUpdateBackground { it.copy(gradientAngleDeg = ang) } }
            )
        }

        Button(
            onClick = onGenerateAiBackdrop,
            enabled = !isGeneratingBackdrop,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("editor_ai_backdrop_btn"),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (isGeneratingBackdrop) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generating iOS Wallpaper (30-60s)...")
            } else {
                Icon(Icons.Rounded.AutoAwesome, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate AI iOS Backdrop")
            }
        }
    }
}

@Composable
fun LayoutPanel(
    selectedElement: CanvasElement?,
    aiTipState: AiTipState,
    onUpdateElement: ((CanvasElement) -> CanvasElement) -> Unit,
    onAddTextElement: () -> Unit,
    onDeleteSelected: () -> Unit,
    onResetProject: () -> Unit,
    onRequestAiTip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (selectedElement != null) {
            LabeledSlider(
                label = "Corner Radius",
                valueText = "${selectedElement.cardCornerRadiusDp.toInt()} dp",
                value = selectedElement.cardCornerRadiusDp,
                valueRange = 0f..48f,
                onValueChange = { r -> onUpdateElement { it.copy(cardCornerRadiusDp = r) } }
            )
            LabeledSlider(
                label = "Card Padding",
                valueText = "${selectedElement.cardPaddingDp.toInt()} dp",
                value = selectedElement.cardPaddingDp,
                valueRange = 4f..44f,
                onValueChange = { p -> onUpdateElement { it.copy(cardPaddingDp = p) } }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onAddTextElement,
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Card")
            }
            if (selectedElement != null) {
                OutlinedButton(
                    onClick = onDeleteSelected,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remove")
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onResetProject,
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Icon(Icons.Rounded.RestartAlt, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset")
            }
            Button(
                onClick = onRequestAiTip,
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("AI Tips")
            }
        }

        when (aiTipState) {
            is AiTipState.Streaming -> Text(
                text = aiTipState.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            is AiTipState.Ready -> Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            ) {
                Text(
                    text = aiTipState.text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
            is AiTipState.Error -> Text(
                text = aiTipState.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            AiTipState.Idle -> Unit
        }
    }
}
