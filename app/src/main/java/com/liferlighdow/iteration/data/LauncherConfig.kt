package com.liferlighdow.iteration.data

import com.liferlighdow.iteration.ui.DockStyle
import com.liferlighdow.iteration.ui.ThemeMode
import com.liferlighdow.iteration.utils.ActionMode
import com.liferlighdow.iteration.utils.GestureAction
import com.liferlighdow.iteration.utils.IconShape
import com.liferlighdow.iteration.utils.IconStyle
import kotlinx.serialization.Serializable

@Serializable
data class LauncherConfig(
    val settings: LauncherSettings = LauncherSettings(),
    val layout: List<List<AppModel>> = emptyList(),
    val dock: List<String> = emptyList(),
    val minusOneWidgets: List<WidgetModel> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val hiddenApps: Set<String> = emptySet(),
    val customLabels: Map<String, String> = emptyMap(),
    val customCategories: Map<String, String> = emptyMap(),
    val userCategories: List<String> = emptyList(),
    val categoryRenames: Map<String, String> = emptyMap(),
    val excludedThemed: Set<String> = emptySet(),
    val pwaApps: List<AppModel> = emptyList(),
    val launchCounts: String = "",
    val version: Int = 3,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class LauncherSettings(
    val themedIcons: Boolean = false,
    val liquidGlassEnabled: Boolean = false,
    val liquidGlassDock: Boolean = false,
    val liquidGlassHomeFolder: Boolean = false,
    val liquidGlassAppLibraryFolder: Boolean = false,
    val liquidGlassGlobalSearch: Boolean = false,
    val liquidGlassAppLibrarySearch: Boolean = false,
    val liquidGlassWidgets: Boolean = false,
    val liquidGlassMinusOneWidget: Boolean = false,
    val liquidGlassMinusOneSearch: Boolean = false,
    val liquidGlassMinusOneButton: Boolean = false,
    val networkAccessEnabled: Boolean = true,
    val showMinusOne: Boolean = true,
    val showAppLibrary: Boolean = true,
    val iconStyle: IconStyle = IconStyle.STANDARD,
    val iconShape: IconShape = IconShape.DEFAULT,
    val libraryShape: IconShape = IconShape.DEFAULT,
    val searchEngineUrl: String = "https://www.google.com/search?q=",
    val autoAddAppsToHome: Boolean = true,
    val showStatusBar: Boolean = true,
    val showNavigationBar: Boolean = true,
    val pageSize: Int = 20,
    val actionMode: ActionMode = ActionMode.ACCESSIBILITY,
    val themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    val appLanguage: String = "default",
    val amoledBlack: Boolean = false,
    val iconPackPackage: String = "",
    val password: String = "",
    val emojiWallpaperText: String = "",
    val customWallpaperColor: Int = 0,
    val favoriteWallpaperColors: List<Int> = emptyList(),
    val homeMenuOptions: Set<String> = setOf("delete_home", "edit", "uninstall", "shortcuts", "freeze", "hide", "app_info", "favorite"),
    val glassParams: GlassParams = GlassParams(),
    val gestures: GestureSettings = GestureSettings(),
    val customIconSettings: CustomIconSettings = CustomIconSettings()
)

@Serializable
data class GlassParams(
    val blur: Float = 0f,
    val refractionHeight: Float = 24f,
    val refractionAmount: Float = 48f,
    val chromaticAberration: Boolean = true
)

@Serializable
data class GestureSettings(
    val doubleTapAction: GestureAction = GestureAction.NONE,
    val doubleTapApp: String = "",
    val swipeUpAction: GestureAction = GestureAction.NONE,
    val swipeUpApp: String = "",
    val swipeDownAction: GestureAction = GestureAction.NONE,
    val swipeDownApp: String = "",
    val longPressAction: GestureAction = GestureAction.OPEN_DESKTOP_MENU,
    val longPressApp: String = "",
    val twoFingerSwipeUpAction: GestureAction = GestureAction.NONE,
    val twoFingerSwipeUpApp: String = "",
    val twoFingerSwipeDownAction: GestureAction = GestureAction.NONE,
    val twoFingerSwipeDownApp: String = ""
)

@Serializable
data class CustomIconSettings(
    val bgColor: Int = 0,
    val fgColor: Int = 0,
    val useOriginal: Boolean = true,
    val useOriginalBg: Boolean = true,
    val useDominantColor: Boolean = false,
    val iconPackPackage: String = ""
)
