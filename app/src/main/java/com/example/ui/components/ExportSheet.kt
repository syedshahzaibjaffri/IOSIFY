package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.engine.ExportManager
import com.example.model.CanvasDocument
import com.example.model.ExportFormat
import com.example.model.ExportScale
import com.example.ui.theme.LocalIosifyColors
import com.example.viewmodel.ExportUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBottomSheet(
    document: CanvasDocument,
    defaultFormat: ExportFormat,
    defaultQuality: Int,
    exportState: ExportUiState,
    exportManager: ExportManager,
    onExport: (ExportFormat, Int, Int, Boolean, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val ext = LocalIosifyColors.current

    var selectedFormat by remember { mutableStateOf(defaultFormat) }
    var selectedScale by remember { mutableStateOf(ExportScale.X2) }
    var customWidth by remember { mutableStateOf(document.canvasWidth.toString()) }
    var customHeight by remember { mutableStateOf(document.canvasHeight.toString()) }
    var transparentBg by remember { mutableStateOf(false) }
    var quality by remember { mutableFloatStateOf(defaultQuality.toFloat()) }
    var copiedNotice by remember { mutableStateOf(false) }

    val (resolvedW, resolvedH) = remember(selectedScale, customWidth, customHeight, document) {
        when (selectedScale) {
            ExportScale.X1 -> document.canvasWidth to document.canvasHeight
            ExportScale.X2 -> (document.canvasWidth * 2).coerceAtMost(4096) to
                (document.canvasHeight * 2).coerceAtMost(4096)
            ExportScale.X3 -> (document.canvasWidth * 3).coerceAtMost(4096) to
                (document.canvasHeight * 3).coerceAtMost(4096)
            ExportScale.CUSTOM -> {
                val cw = customWidth.toIntOrNull()?.coerceIn(128, 4096) ?: document.canvasWidth
                val ch = customHeight.toIntOrNull()?.coerceIn(128, 4096) ?: document.canvasHeight
                cw to ch
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Export Design",
                style = MaterialTheme.typography.headlineMedium
            )

            // Format Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Format", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExportFormat.entries.forEach { format ->
                        FilterChip(
                            selected = selectedFormat == format,
                            onClick = {
                                selectedFormat = format
                                if (format == ExportFormat.JPG) transparentBg = false
                            },
                            label = { Text(format.name) },
                            modifier = Modifier.testTag("export_format_${format.name.lowercase()}")
                        )
                    }
                }
            }

            // Resolution Scale Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Resolution ($resolvedW × $resolvedH px)",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportScale.entries.forEach { scale ->
                        FilterChip(
                            selected = selectedScale == scale,
                            onClick = { selectedScale = scale },
                            label = { Text(scale.label.substringBefore(" ")) },
                            modifier = Modifier.testTag("export_scale_${scale.name.lowercase()}")
                        )
                    }
                }

                if (selectedScale == ExportScale.CUSTOM) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = customWidth,
                            onValueChange = { customWidth = it.filter(Char::isDigit).take(4) },
                            label = { Text("Width (px)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = customHeight,
                            onValueChange = { customHeight = it.filter(Char::isDigit).take(4) },
                            label = { Text("Height (px)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Transparency Toggle (PNG / WebP)
            if (selectedFormat != ExportFormat.JPG) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Transparent Background", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Ideal for stickers and text overlays",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = transparentBg,
                        onCheckedChange = { transparentBg = it },
                        modifier = Modifier.testTag("export_transparent_switch")
                    )
                }
            }

            // Compression Quality
            if (selectedFormat != ExportFormat.PNG) {
                LabeledSlider(
                    label = "Compression Quality",
                    valueText = "${quality.toInt()}%",
                    value = quality,
                    valueRange = 60f..100f,
                    onValueChange = { quality = it }
                )
            }

            // Export Status / Confirmation
            when (exportState) {
                is ExportUiState.Exporting -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Rendering ${resolvedW}×${resolvedH} ${selectedFormat.name}...")
                    }
                }
                is ExportUiState.Success -> {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ext.green.copy(alpha = 0.14f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = "Exported",
                                    tint = ext.green
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Saved ${exportState.output.width}×${exportState.output.height} ${exportState.output.format.name} to Gallery",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Button(
                                onClick = {
                                    val shareIntent = exportManager.createShareIntent(
                                        exportState.output.filePath,
                                        exportState.output.format
                                    )
                                    context.startActivity(Intent.createChooser(shareIntent, "Share iOSify Design"))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("export_share_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Share via Android Sheet")
                            }
                        }
                    }
                }
                is ExportUiState.Error -> {
                    Text(
                        text = exportState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                ExportUiState.Idle -> Unit
            }

            // Action Buttons
            Button(
                onClick = {
                    onExport(selectedFormat, resolvedW, resolvedH, transparentBg, quality.toInt())
                },
                enabled = exportState !is ExportUiState.Exporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("export_generate_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save to Device Gallery")
            }

            OutlinedButton(
                onClick = {
                    val allText = document.elements.mapNotNull {
                        it.text.takeIf(String::isNotBlank)
                    }.joinToString("\n\n")
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("iOSify Text", allText))
                    copiedNotice = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("export_copy_text_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (copiedNotice) "Text Copied to Clipboard" else "Copy Design Text")
            }
        }
    }
}
