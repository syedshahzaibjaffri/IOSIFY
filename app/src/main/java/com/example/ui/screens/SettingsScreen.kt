package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.model.ExportFormat
import com.example.model.StudioFont
import com.example.model.ThemePreference
import com.example.model.UserSettings
import com.example.ui.components.IosifySectionCard
import com.example.ui.components.LabeledSlider

@Composable
fun SettingsScreen(
    settings: UserSettings,
    projectCount: Int,
    exportCount: Int,
    onSetThemeMode: (ThemePreference) -> Unit,
    onSetAppLanguage: (AppLanguage) -> Unit,
    onSetDefaultEnglishFont: (StudioFont) -> Unit,
    onSetDefaultUrduFont: (StudioFont) -> Unit,
    onSetDefaultEmojiPack: (String) -> Unit,
    onSetDefaultExportFormat: (ExportFormat) -> Unit,
    onSetExportQuality: (Int) -> Unit,
    onSetAiUploadConsent: (Boolean) -> Unit,
    onClearHistory: () -> Unit
) {
    var showClearConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Settings", style = MaterialTheme.typography.headlineMedium)
            }

            // Appearance
            item {
                IosifySectionCard(title = "Appearance & Theme") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemePreference.entries.forEach { mode ->
                            FilterChip(
                                selected = settings.themeMode == mode,
                                onClick = { onSetThemeMode(mode) },
                                label = { Text(mode.label) },
                                modifier = Modifier.testTag("settings_theme_${mode.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            // Language & Typography Defaults
            item {
                IosifySectionCard(title = "Language & Default Typography") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Studio Language Mode", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppLanguage.entries.forEach { lang ->
                                FilterChip(
                                    selected = settings.appLanguage == lang,
                                    onClick = { onSetAppLanguage(lang) },
                                    label = { Text(lang.label) }
                                )
                            }
                        }

                        Text("Default English Font", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf(StudioFont.INTER, StudioFont.PLUS_JAKARTA, StudioFont.SYSTEM_UI)) { font ->
                                FilterChip(
                                    selected = settings.defaultEnglishFont == font,
                                    onClick = { onSetDefaultEnglishFont(font) },
                                    label = { Text(font.displayName) }
                                )
                            }
                        }

                        Text("Default Urdu Font (RTL)", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf(StudioFont.NOTO_NASTALIQ_URDU, StudioFont.NOTO_NASKH_ARABIC)) { font ->
                                FilterChip(
                                    selected = settings.defaultUrduFont == font,
                                    onClick = { onSetDefaultUrduFont(font) },
                                    label = { Text(font.displayName) }
                                )
                            }
                        }
                    }
                }
            }

            // Emoji Pack & Export Defaults
            item {
                IosifySectionCard(title = "Emoji Pack & Export Defaults") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Default Emoji Pack", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Default iOSify", "Authorized iOS Pack").forEach { pack ->
                                FilterChip(
                                    selected = settings.defaultEmojiPack == pack,
                                    onClick = { onSetDefaultEmojiPack(pack) },
                                    label = { Text(pack) }
                                )
                            }
                        }

                        Text("Default Export Format", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ExportFormat.entries.forEach { fmt ->
                                FilterChip(
                                    selected = settings.defaultExportFormat == fmt,
                                    onClick = { onSetDefaultExportFormat(fmt) },
                                    label = { Text(fmt.name) }
                                )
                            }
                        }

                        LabeledSlider(
                            label = "Default Image Compression Quality",
                            valueText = "${settings.exportQuality}%",
                            value = settings.exportQuality.toFloat(),
                            valueRange = 60f..100f,
                            onValueChange = { onSetExportQuality(it.toInt()) }
                        )
                    }
                }
            }

            // Privacy & Data Controls
            item {
                IosifySectionCard(title = "Privacy & Storage") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("AI Screenshot Upload Consent", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "Allow sending selected screenshots securely to Gemini via Firebase AI Logic",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.hasConsentedToAiUpload,
                                onCheckedChange = onSetAiUploadConsent,
                                modifier = Modifier.testTag("settings_ai_consent_switch")
                            )
                        }

                        Text(
                            text = "Local Storage: $projectCount saved projects • $exportCount recorded exports",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedButton(
                            onClick = { showClearConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("settings_clear_history_btn"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.DeleteForever, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear Project & Export History")
                        }
                    }
                }
            }

            // About iOSify
            item {
                IosifySectionCard(title = "About iOSify") {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(
                            Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("iOSify Studio • v1.0", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "AI-powered iOS style studio for Android featuring Google Gemini multimodal analysis, authentic Urdu Nastaliq & Naskh RTL typography, advanced text effects, and authorized custom emoji pack support.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear All History?") },
            text = { Text("This removes all saved projects, thumbnails, and export history records from device storage.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearHistory()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Clear Everything") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
