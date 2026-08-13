package com.liferlighdow.iteration

import android.content.pm.ActivityInfo
import android.os.Build
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
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
            val isMaterialYouEnabled by viewModel.isMaterialYouEnabled.collectAsState()
            val seedColor by viewModel.seedColor.collectAsState()
            val showStatusBar by viewModel.showStatusBar.collectAsState()
            val showNavigationBar by viewModel.showNavigationBar.collectAsState()
            val isLightWallpaper by viewModel.isLightWallpaper.collectAsState()
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsStateWithLifecycle()

            LaunchedEffect(showStatusBar, showNavigationBar, isLightWallpaper, lifecycleState) {
                if (lifecycleState != Lifecycle.State.RESUMED) return@LaunchedEffect

                val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                
                // 根據桌布明暗設置狀態欄與導覽列圖示顏色
                windowInsetsController.isAppearanceLightStatusBars = isLightWallpaper
                windowInsetsController.isAppearanceLightNavigationBars = isLightWallpaper

                if (showStatusBar) {
                    windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
                } else {
                    windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
                }

                if (showNavigationBar) {
                    windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                    windowInsetsController.show(WindowInsetsCompat.Type.navigationBars())
                } else {
                    windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
                }
            }

            IterationTheme(
                themeMode = themeMode,
                isAmoledBlack = isAmoledBlack,
                isMaterialYouEnabled = isMaterialYouEnabled,
                seedColor = seedColor
            ) {
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
    var workshopWidgetId by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    
    BackHandler(enabled = currentPage != SettingsPage.MAIN) {
        when (currentPage) {
            SettingsPage.CHANGE_ICON -> currentPage = SettingsPage.ICON_THEME
            SettingsPage.GLOBAL_SEARCH_MANUAL -> currentPage = SettingsPage.MANUALS
            SettingsPage.ICON_ENGINE_MANUAL -> currentPage = SettingsPage.MANUALS
            SettingsPage.DOCK_MANUAL -> currentPage = SettingsPage.MANUALS
            SettingsPage.WIDGET_WORKSHOP -> currentPage = SettingsPage.WIDGET_MAKER
            SettingsPage.SEARCH_ENGINE -> currentPage = SettingsPage.SEARCH
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
            onNavigateToPwaMaker = { currentPage = SettingsPage.PWA_MANAGE },
            onNavigateToWidgetMaker = { currentPage = SettingsPage.WIDGET_MAKER },
            onNavigateToGreenify = { currentPage = SettingsPage.GREENIFY }
        )
        SettingsPage.HIDE_APPS -> HideAppsScreen(onBack = { currentPage = SettingsPage.MAIN })
        SettingsPage.GREENIFY -> GreenifyScreen(onBack = { currentPage = SettingsPage.MAIN })
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
        SettingsPage.SEARCH -> SearchSettingsScreen(
            onBack = { currentPage = SettingsPage.MAIN },
            onNavigateToSearchEngine = { currentPage = SettingsPage.SEARCH_ENGINE }
        )
        SettingsPage.SEARCH_ENGINE -> SearchEngineSettingsScreen(onBack = { currentPage = SettingsPage.SEARCH })
        SettingsPage.PERMISSIONS -> PermissionsSettingsScreen(onBack = { currentPage = SettingsPage.MAIN })
        SettingsPage.MANUALS -> ManualsScreen(
            onBack = { currentPage = SettingsPage.MAIN },
            onNavigateToGlobalSearchManual = { currentPage = SettingsPage.GLOBAL_SEARCH_MANUAL },
            onNavigateToIconEngineManual = { currentPage = SettingsPage.ICON_ENGINE_MANUAL },
            onNavigateToDockManual = { currentPage = SettingsPage.DOCK_MANUAL }
        )
        SettingsPage.GLOBAL_SEARCH_MANUAL -> GlobalSearchManualScreen(onBack = {
            currentPage = SettingsPage.MANUALS
        })
        SettingsPage.ICON_ENGINE_MANUAL -> com.liferlighdow.iteration.ui.IconEngineManualScreen(onBack = {
            currentPage = SettingsPage.MANUALS
        })
        SettingsPage.DOCK_MANUAL -> com.liferlighdow.iteration.ui.DockStyleManualScreen(onBack = {
            currentPage = SettingsPage.MANUALS
        })
        SettingsPage.LANGUAGE -> LanguageSettingsScreen(onBack = { currentPage = SettingsPage.MAIN })
        SettingsPage.ADVANCED -> AdvancedSettingsScreen(onBack = { currentPage = SettingsPage.MAIN })
        SettingsPage.PWA_MANAGE -> PwaManageScreen(
            onBack = { currentPage = SettingsPage.MAIN },
            onNavigateToPwaMaker = { currentPage = SettingsPage.PWA_MAKER }
        )
        SettingsPage.PWA_MAKER -> PwaMakerScreen(onBack = { currentPage = SettingsPage.PWA_MANAGE })
        SettingsPage.WIDGET_MAKER -> WidgetMakerScreen(
            onBack = { currentPage = SettingsPage.MAIN },
            onNavigateToWorkshop = { id: String ->
                workshopWidgetId = id
                currentPage = SettingsPage.WIDGET_WORKSHOP 
            }
        )
        SettingsPage.WIDGET_WORKSHOP -> WidgetWorkshopScreen(
            widgetId = workshopWidgetId ?: "",
            onBack = { currentPage = SettingsPage.WIDGET_MAKER }
        )
    }
}

