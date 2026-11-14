package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.configureAppCheck
import com.example.model.ThemePreference
import com.example.ui.navigation.IosifyAppShell
import com.example.ui.theme.IosifyTheme
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before any UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        val container = IosifyContainer(applicationContext)

        setContent {
            val mainViewModel: MainViewModel = viewModel(
                factory = MainViewModel.provideFactory(
                    projectRepository = container.projectRepository,
                    emojiRepository = container.emojiRepository,
                    settingsRepository = container.settingsRepository
                )
            )
            val settings by mainViewModel.settings.collectAsStateWithLifecycle()
            val darkTheme = when (settings.themeMode) {
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
            }

            IosifyTheme(darkTheme = darkTheme) {
                IosifyAppShell(
                    container = container,
                    mainViewModel = mainViewModel
                )
            }
        }
    }
}
