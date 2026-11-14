package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.CustomEmojiEntity
import com.example.data.local.EmojiUsageEntity
import com.example.engine.ExportManager
import com.example.model.EmojiCatalogItem
import com.example.model.EmojiCategory
import com.example.ui.components.IosifySectionCard
import com.example.ui.theme.LocalIosifyColors
import com.example.viewmodel.EmojiExportState
import java.io.File

@Composable
fun EmojiStudioScreen(
    catalog: List<EmojiCatalogItem>,
    selectedCategory: EmojiCategory,
    searchQuery: String,
    selectedEmojis: List<String>,
    customEmojis: List<CustomEmojiEntity>,
    emojiUsage: List<EmojiUsageEntity>,
    exportState: EmojiExportState,
    exportManager: ExportManager,
    onCategorySelected: (EmojiCategory) -> Unit,
    onSearchChange: (String) -> Unit,
    onToggleEmojiSelection: (String) -> Unit,
    onAddEmojiInput: (String) -> Unit,
    onClearSelection: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onImportCustomEmoji: (Uri, String, String, String) -> Unit,
    onDeleteCustomEmoji: (CustomEmojiEntity) -> Unit,
    onExportEmojiPng: (String, Boolean) -> Unit,
    onCreateEmojiProject: () -> Unit
) {
    val context = LocalContext.current
    val ext = LocalIosifyColors.current

    var emojiInput by remember { mutableStateOf("") }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var importEmojiChar by remember { mutableStateOf("✨") }
    var importShortName by remember { mutableStateOf("") }
    var importPackName by remember { mutableStateOf("Authorized iOS Pack") }

    val customAssetPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                pendingImportUri = uri
                importShortName = "Sticker ${(customEmojis.size + 1)}"
            }
        }
    )

    val usageMap = remember(emojiUsage) { emojiUsage.associateBy { it.emojiChar } }
    val customMap = remember(customEmojis) { customEmojis.associateBy { it.emojiChar } }

    val displayedItems = remember(catalog, selectedCategory, searchQuery, usageMap) {
        val q = searchQuery.trim()
        if (q.isNotEmpty()) {
            catalog.filter {
                it.name.contains(q, ignoreCase = true) ||
                    it.char.contains(q) ||
                    it.keywords.any { kw -> kw.contains(q, ignoreCase = true) }
            }
        } else {
            when (selectedCategory) {
                EmojiCategory.RECENT -> {
                    val recentChars = emojiUsage.filter { it.lastUsedAt > 0L }.map { it.emojiChar }
                    recentChars.mapNotNull { ch -> catalog.find { it.char == ch } }
                }
                EmojiCategory.FAVORITES -> {
                    val favChars = emojiUsage.filter { it.isFavorite }.map { it.emojiChar }.toSet()
                    catalog.filter { favChars.contains(it.char) }
                }
                EmojiCategory.CUSTOM -> emptyList()
                else -> catalog.filter { it.category == selectedCategory }
            }
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
                .widthIn(max = 820.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Emoji Studio", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Browse categories, import authorized iOS-style PNG/WebP assets with alpha transparency, or export transparent PNGs.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Emoji Input & Search
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = emojiInput,
                            onValueChange = { emojiInput = it },
                            label = { Text("Direct Emoji Input") },
                            placeholder = { Text("Paste any emoji e.g. 🫶✨") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("emoji_direct_input")
                        )
                        Button(
                            onClick = {
                                onAddEmojiInput(emojiInput)
                                emojiInput = ""
                            },
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("emoji_add_input_btn"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Add")
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = { Text("Search emojis by name or mood...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("emoji_search_input")
                    )
                }
            }

            // Multi-Emoji Selection Tray
            if (selectedEmojis.isNotEmpty()) {
                item {
                    IosifySectionCard(title = "Selected Emojis (${selectedEmojis.size})") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(selectedEmojis) { ch ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.clickable { onToggleEmojiSelection(ch) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val custom = customMap[ch]
                                            if (custom != null) {
                                                AsyncImage(
                                                    model = File(custom.assetFilePath),
                                                    contentDescription = ch,
                                                    modifier = Modifier.size(24.dp),
                                                    contentScale = ContentScale.Fit
                                                )
                                            } else {
                                                Text(ch, fontSize = 22.sp)
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(Icons.Rounded.Clear, null, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onExportEmojiPng(selectedEmojis.first(), true) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("emoji_export_png_btn"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Rounded.Download, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("PNG Export")
                                }
                                Button(
                                    onClick = onCreateEmojiProject,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("emoji_open_canvas_btn"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Canvas Art")
                                }
                            }
                            TextButton(onClick = onClearSelection) {
                                Text("Clear Selection")
                            }
                        }
                    }
                }
            }

            // Export Confirmation
            when (exportState) {
                is EmojiExportState.Success -> {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = ext.green.copy(alpha = 0.14f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Rounded.CheckCircle, null, tint = ext.green)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Exported 512×512 Transparent PNG",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val intent = exportManager.createShareIntent(
                                            exportState.output.filePath,
                                            exportState.output.format
                                        )
                                        context.startActivity(Intent.createChooser(intent, "Share Emoji PNG"))
                                    }
                                ) {
                                    Icon(Icons.Rounded.Share, contentDescription = "Share Emoji PNG")
                                }
                            }
                        }
                    }
                }
                is EmojiExportState.Error -> {
                    item {
                        Text(exportState.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                else -> Unit
            }

            // Category Browser Chips (All 9 categories + Recent/Favorites/Custom)
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(EmojiCategory.entries) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat && searchQuery.isBlank(),
                            onClick = { onCategorySelected(cat) },
                            label = { Text(cat.label) },
                            modifier = Modifier.testTag("emoji_cat_${cat.name.lowercase()}")
                        )
                    }
                }
            }

            // Custom Emoji Asset Pack Importer
            item {
                IosifySectionCard(title = "Authorized Emoji Asset Pack") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Import your authorized PNG or WebP emoji artwork. Aspect ratio and transparent alpha channels are preserved across previews and exports.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = {
                                customAssetPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("emoji_import_custom_btn"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.AddPhotoAlternate, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Import Custom PNG / WebP Emoji")
                        }

                        if (customEmojis.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(customEmojis, key = { it.id }) { custom ->
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        modifier = Modifier.clickable { onToggleEmojiSelection(custom.emojiChar) }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            AsyncImage(
                                                model = File(custom.assetFilePath),
                                                contentDescription = custom.shortName,
                                                modifier = Modifier.size(48.dp),
                                                contentScale = ContentScale.Fit
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "${custom.emojiChar} ${custom.shortName}",
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            IconButton(
                                                onClick = { onDeleteCustomEmoji(custom) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Delete,
                                                    contentDescription = "Remove Custom Emoji",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Emoji Grid
            if (selectedCategory != EmojiCategory.CUSTOM || searchQuery.isNotBlank()) {
                item {
                    if (displayedItems.isEmpty()) {
                        Text(
                            text = "No emojis match this filter yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 84.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(360.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(displayedItems, key = { it.char }) { item ->
                                val isSelected = selectedEmojis.contains(item.char)
                                val isFav = usageMap[item.char]?.isFavorite == true
                                val customAsset = customMap[item.char]
                                Card(
                                    modifier = Modifier
                                        .border(
                                            width = if (isSelected) 2.dp else 0.8.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else ext.separator.copy(alpha = 0.45f),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clip(RoundedCornerShape(16.dp))
                                        .testTag("emoji_cell_${item.name.replace(" ", "_").lowercase()}")
                                        .clickable { onToggleEmojiSelection(item.char) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Box(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                                        IconButton(
                                            onClick = { onToggleFavorite(item.char) },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isFav) Icons.Rounded.Star else Icons.Outlined.StarBorder,
                                                contentDescription = "Favorite Emoji",
                                                tint = if (isFav) ext.orange else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            if (customAsset != null) {
                                                AsyncImage(
                                                    model = File(customAsset.assetFilePath),
                                                    contentDescription = item.name,
                                                    modifier = Modifier.size(34.dp),
                                                    contentScale = ContentScale.Fit
                                                )
                                            } else {
                                                Text(item.char, fontSize = 30.sp)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = item.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // Custom Emoji Mapping Dialog
    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("Map Custom Emoji Asset") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = importEmojiChar,
                        onValueChange = { importEmojiChar = it },
                        label = { Text("Emoji Trigger (e.g. ✨ or 😀)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = importShortName,
                        onValueChange = { importShortName = it },
                        label = { Text("Asset Label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = importPackName,
                        onValueChange = { importPackName = it },
                        label = { Text("Pack Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onImportCustomEmoji(uri, importEmojiChar, importShortName, importPackName)
                        pendingImportUri = null
                    }
                ) {
                    Text("Import Asset")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) { Text("Cancel") }
            }
        )
    }
}
