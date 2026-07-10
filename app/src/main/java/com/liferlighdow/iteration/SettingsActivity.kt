package com.liferlighdow.iteration

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liferlighdow.iteration.ui.*
import com.liferlighdow.iteration.ui.settings.*
import com.liferlighdow.iteration.viewmodel.MainViewModel

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val isAmoledBlack by viewModel.isAmoledBlack.collectAsState()
            IterationTheme(themeMode = themeMode, isAmoledBlack = isAmoledBlack) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsNavigation()
                }
            }
        }
    }
}

@Composable
fun SettingsNavigation() {
    var currentPage by remember { mutableStateOf(SettingsPage.MAIN) }
    val context = LocalContext.current
    
    BackHandler(enabled = currentPage != SettingsPage.MAIN) {
        when (currentPage) {
            SettingsPage.CHANGE_ICON -> currentPage = SettingsPage.ICON_THEME
            SettingsPage.GLOBAL_SEARCH_MANUAL -> currentPage = SettingsPage.MANUALS
            SettingsPage.ICON_ENGINE_MANUAL -> currentPage = SettingsPage.MANUALS
            else -> currentPage = SettingsPage.MAIN
        }
    }

    when (currentPage) {
        SettingsPage.MAIN -> SettingsMainScreen(
            onBack = { (context as? AppCompatActivity)?.finish() },
            onNavigateToHideApps = { currentPage = SettingsPage.HIDE_APPS },
            onNavigateToRenameApps = { currentPage = SettingsPage.RENAME_APPS },
            onNavigateToAppLibrary = { currentPage = SettingsPage.APP_LIBRARY },
            onNavigateToIconTheme = { currentPage = SettingsPage.ICON_THEME },
            onNavigateToDock = { currentPage = SettingsPage.DOCK },
            onNavigateToLiquidGlass = { currentPage = SettingsPage.LIQUID_GLASS },
            onNavigateToGestures = { currentPage = SettingsPage.GESTURES },
            onNavigateToSearch = { currentPage = SettingsPage.SEARCH },
            onNavigateToPermissions = { currentPage = SettingsPage.PERMISSIONS },
            onNavigateToManuals = { currentPage = SettingsPage.MANUALS },
            onNavigateToLanguage = { currentPage = SettingsPage.LANGUAGE },
            onNavigateToAdvanced = { currentPage = SettingsPage.ADVANCED },
            onNavigateToPwaMaker = { currentPage = SettingsPage.PWA_MAKER }
        )
        SettingsPage.HIDE_APPS -> HideAppsScreen(onBack = { currentPage = SettingsPage.MAIN })
        SettingsPage.RENAME_APPS -> RenameAppsScreen(onBack = { currentPage = SettingsPage.MAIN })
        SettingsPage.CHANGE_ICON -> ChangeIconScreen(onBack = { currentPage = SettingsPage.ICON_THEME })
        SettingsPage.APP_LIBRARY -> AppLibrarySettingsScreen(onBack = { currentPage = SettingsPage.MAIN })
        SettingsPage.ICON_THEME -> IconThemeScreen(
            onBack = { currentPage = SettingsPage.MAIN },
            onNavigateToChangeIcon = { currentPage = SettingsPage.CHANGE_ICON }
        )
        SettingsPage.DOCK -> DesktopSettingsScreen(onBack = { currentPage = SettingsPage.MAIN })
        SettingsPage.LIQUID_GLASS -> LiquidGlassSettingsScreen(onBack = { currentPage = SettingsPage.MAIN })
        SettingsPage.GESTURES -> GesturesSettingsScreen(onBack = { currentPage = SettingsPage.MAIN })
        SettingsPage.SEARCH -> SearchSettingsScreen(onBack = { currentPage = SettingsPage.MAIN })
        SettingsPage.PERMISSIONS -> PermissionsSettingsScreen(onBack = { currentPage = SettingsPage.MAIN })
        SettingsPage.MANUALS -> ManualsScreen(
            onBack = { currentPage = SettingsPage.MAIN },
            onNavigateToGlobalSearchManual = { currentPage = SettingsPage.GLOBAL_SEARCH_MANUAL },
            onNavigateToIconEngineManual = { currentPage = SettingsPage.ICON_ENGINE_MANUAL }
        )
        SettingsPage.GLOBAL_SEARCH_MANUAL -> GlobalSearchManualScreen(onBack = {
            currentPage = SettingsPage.MANUALS
        })
        SettingsPage.ICON_ENGINE_MANUAL -> com.liferlighdow.iteration.ui.IconEngineManualScreen(onBack = {
            currentPage = SettingsPage.MANUALS
        })
        SettingsPage.LANGUAGE -> LanguageSettingsScreen(onBack = { currentPage = SettingsPage.MAIN })
        SettingsPage.ADVANCED -> AdvancedSettingsScreen(onBack = { currentPage = SettingsPage.MAIN })
        SettingsPage.PWA_MAKER -> PwaMakerScreen(onBack = { currentPage = SettingsPage.MAIN })
    }
}
