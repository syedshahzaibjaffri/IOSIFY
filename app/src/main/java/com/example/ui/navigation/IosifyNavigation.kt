package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.IosifyContainer
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.EmojiStudioScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ScreenshotStudioScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TextStudioScreen
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.EmojiStudioViewModel
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.ScreenshotViewModel

object Routes {
    const val HOME = "home"
    const val SCREENSHOTS = "screenshot_studio"
    const val EMOJIS = "emoji_studio"
    const val TEXT = "text_studio"
    const val SETTINGS = "settings"
    const val EDITOR = "editor/{projectId}"

    fun editorRoute(projectId: String): String = "editor/$projectId"
}

private data class BottomTabItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val bottomTabs = listOf(
    BottomTabItem(Routes.HOME, "Home", Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
    BottomTabItem(Routes.SCREENSHOTS, "Shots", Icons.Rounded.PhotoLibrary, Icons.Outlined.PhotoLibrary, "nav_screenshots"),
    BottomTabItem(Routes.EMOJIS, "Emojis", Icons.Rounded.EmojiEmotions, Icons.Outlined.EmojiEmotions, "nav_emojis"),
    BottomTabItem(Routes.TEXT, "Text", Icons.Rounded.TextFields, Icons.Outlined.TextFields, "nav_text"),
    BottomTabItem(Routes.SETTINGS, "Settings", Icons.Rounded.Settings, Icons.Outlined.Settings, "nav_settings")
)

@Composable
fun IosifyAppShell(
    container: IosifyContainer,
    mainViewModel: MainViewModel
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showNavigationChrome = bottomTabs.any { it.route == currentRoute }

    val settings by mainViewModel.settings.collectAsStateWithLifecycle()
    val allProjects by mainViewModel.allProjects.collectAsStateWithLifecycle()
    val filteredProjects by mainViewModel.filteredProjects.collectAsStateWithLifecycle()
    val recentExports by mainViewModel.recentExports.collectAsStateWithLifecycle()
    val customEmojis by mainViewModel.customEmojis.collectAsStateWithLifecycle()
    val searchQuery by mainViewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by mainViewModel.selectedFilter.collectAsStateWithLifecycle()

    val screenshotViewModel: ScreenshotViewModel = viewModel(
        factory = ScreenshotViewModel.provideFactory(
            projectRepository = container.projectRepository,
            emojiRepository = container.emojiRepository,
            settingsRepository = container.settingsRepository,
            geminiAiRepository = container.geminiAiRepository
        )
    )
    val emojiViewModel: EmojiStudioViewModel = viewModel(
        factory = EmojiStudioViewModel.provideFactory(
            emojiRepository = container.emojiRepository,
            projectRepository = container.projectRepository,
            exportManager = container.exportManager
        )
    )

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showNavigationChrome && !useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        bottomTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showNavigationChrome && useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxHeight()
                            .windowInsetsPadding(WindowInsets.statusBars)
                    ) {
                        bottomTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .then(
                            if (showNavigationChrome) Modifier.windowInsetsPadding(WindowInsets.statusBars)
                            else Modifier
                        )
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = Routes.HOME
                    ) {
                        composable(Routes.HOME) {
                            HomeScreen(
                                projects = filteredProjects,
                                recentExports = recentExports,
                                searchQuery = searchQuery,
                                selectedFilter = selectedFilter,
                                exportManager = container.exportManager,
                                onSearchChange = mainViewModel::setSearchQuery,
                                onFilterChange = mainViewModel::setHomeFilter,
                                onOpenScreenshotStudio = { navigateToTab(Routes.SCREENSHOTS) },
                                onOpenTextStudio = { navigateToTab(Routes.TEXT) },
                                onOpenEmojiStudio = { navigateToTab(Routes.EMOJIS) },
                                onCreateBlankProject = {
                                    mainViewModel.createBlankProject { id ->
                                        navController.navigate(Routes.editorRoute(id))
                                    }
                                },
                                onOpenProject = { id ->
                                    navController.navigate(Routes.editorRoute(id))
                                },
                                onRenameProject = mainViewModel::renameProject,
                                onDuplicateProject = mainViewModel::duplicateProject,
                                onToggleFavorite = mainViewModel::toggleFavorite,
                                onDeleteProject = mainViewModel::deleteProject,
                                onDeleteExport = mainViewModel::deleteExport
                            )
                        }

                        composable(Routes.SCREENSHOTS) {
                            val selectedImg by screenshotViewModel.selectedImagePath.collectAsStateWithLifecycle()
                            val selectedMode by screenshotViewModel.selectedMode.collectAsStateWithLifecycle()
                            val genBackdrop by screenshotViewModel.generateAiBackdrop.collectAsStateWithLifecycle()
                            val instruction by screenshotViewModel.customInstruction.collectAsStateWithLifecycle()
                            val aiState by screenshotViewModel.aiState.collectAsStateWithLifecycle()

                            ScreenshotStudioScreen(
                                selectedImagePath = selectedImg,
                                selectedMode = selectedMode,
                                generateAiBackdrop = genBackdrop,
                                customInstruction = instruction,
                                aiState = aiState,
                                settings = settings,
                                onImportUri = screenshotViewModel::importScreenshot,
                                onClearImage = screenshotViewModel::clearSelectedImage,
                                onSelectMode = screenshotViewModel::setTransformMode,
                                onToggleAiBackdrop = screenshotViewModel::setGenerateAiBackdrop,
                                onInstructionChange = screenshotViewModel::setCustomInstruction,
                                onGrantConsentAndTransform = {
                                    screenshotViewModel.grantConsentAndAnalyze { id ->
                                        navController.navigate(Routes.editorRoute(id))
                                    }
                                },
                                onTransform = {
                                    screenshotViewModel.analyzeAndCreateProject { id ->
                                        navController.navigate(Routes.editorRoute(id))
                                    }
                                }
                            )
                        }

                        composable(Routes.EMOJIS) {
                            val selectedCat by emojiViewModel.selectedCategory.collectAsStateWithLifecycle()
                            val emojiSearch by emojiViewModel.searchQuery.collectAsStateWithLifecycle()
                            val selectedEmojis by emojiViewModel.selectedEmojis.collectAsStateWithLifecycle()
                            val emojiUsage by emojiViewModel.emojiUsage.collectAsStateWithLifecycle()
                            val emojiExport by emojiViewModel.exportState.collectAsStateWithLifecycle()

                            EmojiStudioScreen(
                                catalog = emojiViewModel.catalog,
                                selectedCategory = selectedCat,
                                searchQuery = emojiSearch,
                                selectedEmojis = selectedEmojis,
                                customEmojis = customEmojis,
                                emojiUsage = emojiUsage,
                                exportState = emojiExport,
                                exportManager = container.exportManager,
                                onCategorySelected = emojiViewModel::selectCategory,
                                onSearchChange = emojiViewModel::updateSearch,
                                onToggleEmojiSelection = emojiViewModel::toggleEmojiInSelection,
                                onAddEmojiInput = emojiViewModel::addEmojiTextInput,
                                onClearSelection = emojiViewModel::clearSelection,
                                onToggleFavorite = emojiViewModel::toggleFavorite,
                                onImportCustomEmoji = emojiViewModel::importCustomEmoji,
                                onDeleteCustomEmoji = emojiViewModel::deleteCustomEmoji,
                                onExportEmojiPng = emojiViewModel::exportEmojiPng,
                                onCreateEmojiProject = {
                                    emojiViewModel.createEmojiArtProject { id ->
                                        navController.navigate(Routes.editorRoute(id))
                                    }
                                }
                            )
                        }

                        composable(Routes.TEXT) {
                            TextStudioScreen(
                                settings = settings,
                                renderer = container.canvasRenderer,
                                customEmojis = customEmojis,
                                onCreateFromTemplate = { preset, customTxt ->
                                    mainViewModel.createFromTemplate(preset, customTxt) { id ->
                                        navController.navigate(Routes.editorRoute(id))
                                    }
                                },
                                onCreateCustomDesign = { title, doc ->
                                    mainViewModel.createCustomTextProject(title, doc) { id ->
                                        navController.navigate(Routes.editorRoute(id))
                                    }
                                }
                            )
                        }

                        composable(Routes.SETTINGS) {
                            SettingsScreen(
                                settings = settings,
                                projectCount = allProjects.size,
                                exportCount = recentExports.size,
                                onSetThemeMode = mainViewModel::setThemeMode,
                                onSetAppLanguage = mainViewModel::setAppLanguage,
                                onSetDefaultEnglishFont = mainViewModel::setDefaultEnglishFont,
                                onSetDefaultUrduFont = mainViewModel::setDefaultUrduFont,
                                onSetDefaultEmojiPack = mainViewModel::setDefaultEmojiPack,
                                onSetDefaultExportFormat = mainViewModel::setDefaultExportFormat,
                                onSetExportQuality = mainViewModel::setExportQuality,
                                onSetAiUploadConsent = mainViewModel::setAiUploadConsent,
                                onClearHistory = mainViewModel::clearAllHistory
                            )
                        }

                        composable(
                            route = Routes.EDITOR,
                            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
                        ) { entry ->
                            val projectId = entry.arguments?.getString("projectId") ?: ""
                            val editorViewModel: EditorViewModel = viewModel(
                                key = "editor_$projectId",
                                factory = EditorViewModel.provideFactory(
                                    projectId = projectId,
                                    projectRepository = container.projectRepository,
                                    emojiRepository = container.emojiRepository,
                                    settingsRepository = container.settingsRepository,
                                    exportManager = container.exportManager,
                                    geminiAiRepository = container.geminiAiRepository
                                )
                            )
                            val editorState by editorViewModel.editorState.collectAsStateWithLifecycle()
                            val exportState by editorViewModel.exportState.collectAsStateWithLifecycle()
                            val aiTipState by editorViewModel.aiTipState.collectAsStateWithLifecycle()
                            val isGeneratingBackdrop by editorViewModel.isGeneratingBackdrop.collectAsStateWithLifecycle()

                            EditorScreen(
                                state = editorState,
                                exportState = exportState,
                                aiTipState = aiTipState,
                                isGeneratingBackdrop = isGeneratingBackdrop,
                                settings = settings,
                                customEmojis = customEmojis,
                                emojiCatalog = emojiViewModel.catalog,
                                renderer = container.canvasRenderer,
                                exportManager = container.exportManager,
                                onBack = { navController.popBackStack() },
                                onRenameProject = editorViewModel::renameProject,
                                onSaveNow = editorViewModel::saveNow,
                                onUndo = editorViewModel::undo,
                                onRedo = editorViewModel::redo,
                                onSelectElement = editorViewModel::selectElement,
                                onSetComparisonMode = editorViewModel::setComparisonMode,
                                onUpdateElement = { editorViewModel.updateSelectedElement(true, it) },
                                onUpdateTextEffect = { editorViewModel.updateSelectedTextEffect(true, it) },
                                onUpdateBackground = { editorViewModel.updateBackground(true, it) },
                                onAddTextElement = { editorViewModel.addTextElement() },
                                onInsertEmojiIntoSelected = editorViewModel::insertEmojiIntoSelectedElement,
                                onAddEmojiSticker = editorViewModel::addEmojiSticker,
                                onDeleteSelected = editorViewModel::deleteSelectedElement,
                                onResetBaseline = editorViewModel::resetToBaseline,
                                onGenerateAiBackdrop = editorViewModel::generateAiBackdropForCanvas,
                                onRequestAiTip = editorViewModel::requestAiDesignTip,
                                onDismissError = editorViewModel::dismissError,
                                onExport = editorViewModel::exportProject,
                                onResetExportState = editorViewModel::resetExportState
                            )
                        }
                    }
                }
            }
        }
    }
}
