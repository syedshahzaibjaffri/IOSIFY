package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.R
import com.example.model.TransformMode
import com.example.model.UserSettings
import com.example.ui.components.IosifySectionCard
import com.example.ui.theme.LocalIosifyColors
import com.example.viewmodel.ScreenshotAiState
import java.io.File

@Composable
fun ScreenshotStudioScreen(
    selectedImagePath: String?,
    selectedMode: TransformMode,
    generateAiBackdrop: Boolean,
    customInstruction: String,
    aiState: ScreenshotAiState,
    settings: UserSettings,
    onImportUri: (Uri) -> Unit,
    onClearImage: () -> Unit,
    onSelectMode: (TransformMode) -> Unit,
    onToggleAiBackdrop: (Boolean) -> Unit,
    onInstructionChange: (String) -> Unit,
    onGrantConsentAndTransform: () -> Unit,
    onTransform: () -> Unit
) {
    val context = LocalContext.current
    val ext = LocalIosifyColors.current

    var showConsentDialog by remember { mutableStateOf(false) }
    var cameraNotice by remember { mutableStateOf<String?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let(onImportUri) }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            val captured = pendingCameraUri
            if (success && captured != null) {
                onImportUri(captured)
            }
        }
    )

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                val captureFile = File(context.cacheDir, "cam_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    captureFile
                )
                pendingCameraUri = uri
                cameraLauncher.launch(uri)
            } else {
                cameraNotice = "Camera permission denied. You can still select screenshots from your gallery."
            }
        }
    )

    fun launchCameraFlow() {
        cameraNotice = null
        val hasCamera = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        if (!hasCamera) {
            cameraNotice = "Camera hardware is unavailable on this device. Use Gallery Selection instead."
            return
        }
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            val captureFile = File(context.cacheDir, "cam_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                captureFile
            )
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
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
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Screenshot Studio",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Analyze English & Urdu screenshots with Gemini and convert them into crisp editable iOS designs.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Upload / Preview Card
            item {
                IosifySectionCard(title = "1. Source Screenshot (PNG, JPG, WebP)") {
                    if (selectedImagePath == null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Rounded.PhotoLibrary,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "Choose a portrait or landscape screenshot",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("screenshot_pick_gallery"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Rounded.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Gallery")
                                }
                                OutlinedButton(
                                    onClick = { launchCameraFlow() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("screenshot_capture_camera"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Rounded.CameraAlt, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Camera")
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 180.dp, max = 340.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                AsyncImage(
                                    model = File(selectedImagePath),
                                    contentDescription = "Selected Screenshot Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                                IconButton(
                                    onClick = onClearImage,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                                        .testTag("screenshot_remove_btn")
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Remove Screenshot")
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("screenshot_replace_btn"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Rounded.SwapHoriz, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Replace Image")
                                }
                            }
                        }
                    }

                    cameraNotice?.let { notice ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = notice,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ext.orange
                        )
                    }
                }
            }

            // Transformation Mode Card
            item {
                IosifySectionCard(title = "2. Transformation Mode") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ModeOptionCard(
                            title = "Preserve Original Layout",
                            description = "Keeps original element positions and text verbatim while upgrading fonts, colors, borders, spacing, and emojis.",
                            selected = selectedMode == TransformMode.PRESERVE_LAYOUT,
                            testTag = "mode_preserve_layout",
                            onClick = { onSelectMode(TransformMode.PRESERVE_LAYOUT) }
                        )
                        ModeOptionCard(
                            title = "Complete iOS Redesign",
                            description = "Reconstructs detected content into modern iOS cards with improved visual hierarchy and rounded surfaces.",
                            selected = selectedMode == TransformMode.COMPLETE_REDESIGN,
                            testTag = "mode_complete_redesign",
                            onClick = { onSelectMode(TransformMode.COMPLETE_REDESIGN) }
                        )

                        AnimatedVisibility(visible = selectedMode == TransformMode.COMPLETE_REDESIGN) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Generate AI iOS Backdrop", style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        "Uses Gemini image generation behind reconstructed cards",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = generateAiBackdrop,
                                    onCheckedChange = onToggleAiBackdrop,
                                    modifier = Modifier.testTag("toggle_ai_backdrop")
                                )
                            }
                        }

                        OutlinedTextField(
                            value = customInstruction,
                            onValueChange = onInstructionChange,
                            label = { Text("Optional Style Instructions") },
                            placeholder = { Text("e.g., Use dark OLED cards and Noto Nastaliq for Urdu lines") },
                            maxLines = 2,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("screenshot_instruction_input")
                        )
                    }
                }
            }

            // Processing / Error / Action
            item {
                when (aiState) {
                    is ScreenshotAiState.Processing -> {
                        IosifySectionCard {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = ext.purple
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(aiState.statusMessage, style = MaterialTheme.typography.titleMedium)
                                }
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                    is ScreenshotAiState.Error -> {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.ErrorOutline,
                                        contentDescription = "Error",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = aiState.message,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                OutlinedButton(
                                    onClick = onTransform,
                                    modifier = Modifier.testTag("screenshot_retry_btn")
                                ) {
                                    Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry Analysis")
                                }
                            }
                        }
                    }
                    ScreenshotAiState.Idle -> Unit
                }

                Button(
                    onClick = {
                        if (settings.hasConsentedToAiUpload) {
                            onTransform()
                        } else {
                            showConsentDialog = true
                        }
                    },
                    enabled = selectedImagePath != null && aiState !is ScreenshotAiState.Processing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("screenshot_transform_btn"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedMode == TransformMode.PRESERVE_LAYOUT) {
                            "iOSify with Preserved Layout"
                        } else {
                            "Transform with Complete iOS Redesign"
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showConsentDialog) {
        AlertDialog(
            onDismissRequest = { showConsentDialog = false },
            title = { Text(stringResource(R.string.consent_title)) },
            text = { Text(stringResource(R.string.consent_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        showConsentDialog = false
                        onGrantConsentAndTransform()
                    },
                    modifier = Modifier.testTag("consent_accept_btn")
                ) {
                    Text(stringResource(R.string.consent_accept))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConsentDialog = false }) {
                    Text(stringResource(R.string.consent_cancel))
                }
            }
        )
    }
}

@Composable
private fun ModeOptionCard(
    title: String,
    description: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(if (selected) 1.8.dp else 0.8.dp, borderColor, RoundedCornerShape(16.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.surface
            )
            .testTag(testTag)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
