package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.ExportRecordEntity
import com.example.data.local.ProjectEntity
import com.example.engine.ExportManager
import com.example.model.ExportFormat
import com.example.ui.theme.LocalIosifyColors
import com.example.viewmodel.HomeFilter
import java.io.File
import java.text.DateFormat
import java.util.Date

@Composable
fun HomeScreen(
    projects: List<ProjectEntity>,
    recentExports: List<ExportRecordEntity>,
    searchQuery: String,
    selectedFilter: HomeFilter,
    exportManager: ExportManager,
    onSearchChange: (String) -> Unit,
    onFilterChange: (HomeFilter) -> Unit,
    onOpenScreenshotStudio: () -> Unit,
    onOpenTextStudio: () -> Unit,
    onOpenEmojiStudio: () -> Unit,
    onCreateBlankProject: () -> Unit,
    onOpenProject: (String) -> Unit,
    onRenameProject: (String, String) -> Unit,
    onDuplicateProject: (String) -> Unit,
    onToggleFavorite: (ProjectEntity) -> Unit,
    onDeleteProject: (String) -> Unit,
    onDeleteExport: (ExportRecordEntity) -> Unit
) {
    val context = LocalContext.current
    val ext = LocalIosifyColors.current

    var renameTarget by remember { mutableStateOf<ProjectEntity?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<ProjectEntity?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 860.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Welcome Hero Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp)
                        .border(0.8.dp, ext.separator.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_studio_1794628333064),
                            contentDescription = "iOSify Studio Banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xD90A0A0F),
                                            Color(0x990A0A0F),
                                            Color(0x330A0A0F)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.18f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Gemini Multimodal Studio",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "iOSify Studio",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = Color.White
                                )
                                Text(
                                    text = "Transform screenshots, Urdu & English text, and emojis into crisp iOS designs.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.88f),
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }

            // Quick Actions
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            QuickActionTile(
                                title = "Upload Screenshot",
                                subtitle = "AI Redesign & Polish",
                                icon = Icons.Rounded.PhotoLibrary,
                                accent = MaterialTheme.colorScheme.primary,
                                testTag = "qa_upload_screenshot",
                                onClick = onOpenScreenshotStudio
                            )
                        }
                        item {
                            QuickActionTile(
                                title = "Create Text Design",
                                subtitle = "English & Urdu RTL",
                                icon = Icons.Rounded.TextFields,
                                accent = ext.purple,
                                testTag = "qa_create_text",
                                onClick = onOpenTextStudio
                            )
                        }
                        item {
                            QuickActionTile(
                                title = "Emoji Studio",
                                subtitle = "Packs & PNG Export",
                                icon = Icons.Rounded.EmojiEmotions,
                                accent = ext.orange,
                                testTag = "qa_emoji_studio",
                                onClick = onOpenEmojiStudio
                            )
                        }
                        item {
                            QuickActionTile(
                                title = "New Blank Canvas",
                                subtitle = "Start from Scratch",
                                icon = Icons.Rounded.Add,
                                accent = ext.green,
                                testTag = "qa_new_blank",
                                onClick = onCreateBlankProject
                            )
                        }
                    }
                }
            }

            // Search & Filter Bar
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = { Text("Search saved projects...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_search_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HomeFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { onFilterChange(filter) },
                                label = { Text(filter.label) },
                                modifier = Modifier.testTag("home_filter_${filter.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            // Recent Exports View or Shelf
            if (selectedFilter == HomeFilter.EXPORTS) {
                if (recentExports.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "No Exports Yet",
                            message = "Export a design from any studio to view and share your high-resolution PNG, JPG, or WebP files here."
                        )
                    }
                } else {
                    items(recentExports, key = { it.id }) { record ->
                        ExportRecordRow(
                            record = record,
                            onShare = {
                                val fmt = runCatching { ExportFormat.valueOf(record.format) }
                                    .getOrDefault(ExportFormat.PNG)
                                val intent = exportManager.createShareIntent(record.filePath, fmt)
                                context.startActivity(Intent.createChooser(intent, "Share Export"))
                            },
                            onDelete = { onDeleteExport(record) }
                        )
                    }
                }
            } else {
                // Recent Exports Strip on All Projects
                if (selectedFilter == HomeFilter.ALL && recentExports.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent Exports",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                TextButton(onClick = { onFilterChange(HomeFilter.EXPORTS) }) {
                                    Text("See All (${recentExports.size})")
                                }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(recentExports.take(6), key = { it.id }) { exportItem ->
                                    Card(
                                        modifier = Modifier
                                            .width(142.dp)
                                            .clickable {
                                                val fmt = runCatching { ExportFormat.valueOf(exportItem.format) }
                                                    .getOrDefault(ExportFormat.PNG)
                                                val intent = exportManager.createShareIntent(exportItem.filePath, fmt)
                                                context.startActivity(Intent.createChooser(intent, "Share Export"))
                                            },
                                        shape = RoundedCornerShape(18.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column {
                                            AsyncImage(
                                                model = File(exportItem.filePath),
                                                contentDescription = exportItem.projectName,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(92.dp),
                                                contentScale = ContentScale.Crop
                                            )
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(
                                                    text = exportItem.projectName,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${exportItem.format} • ${exportItem.width}×${exportItem.height}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Projects Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedFilter == HomeFilter.FAVORITES) "Favorited Designs" else "Your Projects",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${projects.size} saved",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (projects.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = if (selectedFilter == HomeFilter.FAVORITES) "No Favorites Yet" else "Start Creating",
                            message = if (selectedFilter == HomeFilter.FAVORITES) {
                                "Tap the star on any saved project to pin it here for quick access."
                            } else {
                                "Create a blank canvas, upload a screenshot for AI iOS styling, or pick an Urdu/English template."
                            },
                            actionLabel = if (selectedFilter == HomeFilter.ALL) "New Blank Design" else null,
                            onAction = if (selectedFilter == HomeFilter.ALL) onCreateBlankProject else null
                        )
                    }
                } else {
                    items(projects, key = { it.id }) { project ->
                        ProjectCard(
                            project = project,
                            onOpen = { onOpenProject(project.id) },
                            onToggleFavorite = { onToggleFavorite(project) },
                            onRename = {
                                renameTarget = project
                                renameText = project.name
                            },
                            onDuplicate = { onDuplicateProject(project.id) },
                            onDelete = { deleteTarget = project }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // Rename Dialog
    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename Project") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text("Project Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_project_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRenameProject(target.id, renameText)
                        renameTarget = null
                    },
                    modifier = Modifier.testTag("rename_project_confirm")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
            }
        )
    }

    // Delete Confirmation Dialog
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Project?") },
            text = { Text("\"${target.name}\" and its saved canvas progress will be removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProject(target.id)
                        deleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("delete_project_confirm")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun QuickActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val ext = LocalIosifyColors.current
    Card(
        modifier = Modifier
            .width(156.dp)
            .border(0.8.dp, ext.separator.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .testTag(testTag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = accent)
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ProjectCard(
    project: ProjectEntity,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val ext = LocalIosifyColors.current
    var menuExpanded by remember { mutableStateOf(false) }
    val dateStr = remember(project.updatedAt) {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(project.updatedAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.8.dp, ext.separator.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .testTag("project_card_${project.id}")
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!project.thumbnailPath.isNullOrBlank() && File(project.thumbnailPath).exists()) {
                    AsyncImage(
                        model = File(project.thumbnailPath),
                        contentDescription = project.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${project.type.replace("_", " ")} • $dateStr",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.testTag("favorite_btn_${project.id}")
            ) {
                Icon(
                    imageVector = if (project.isFavorite) Icons.Rounded.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Toggle Favorite",
                    tint = if (project.isFavorite) ext.orange else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.testTag("project_menu_${project.id}")
                ) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Project Actions")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Rounded.DriveFileRenameOutline, null) },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicate") },
                        leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                        onClick = {
                            menuExpanded = false
                            onDuplicate()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error)
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportRecordRow(
    record: ExportRecordEntity,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val ext = LocalIosifyColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.8.dp, ext.separator.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = File(record.filePath),
                contentDescription = record.projectName,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(record.projectName, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    "${record.format} • ${record.width}×${record.height}" +
                        if (record.transparentBackground) " • Transparent" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Rounded.Share, contentDescription = "Share Export", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = "Delete Export", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val ext = LocalIosifyColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.8.dp, ext.separator.copy(alpha = 0.5f), RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Button(onClick = onAction, shape = RoundedCornerShape(14.dp)) {
                    Text(actionLabel)
                }
            }
        }
    }
}
