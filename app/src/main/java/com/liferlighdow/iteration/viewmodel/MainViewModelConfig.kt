package com.liferlighdow.iteration.viewmodel

import com.liferlighdow.iteration.data.ConfigSerializer
import com.liferlighdow.iteration.data.CustomIconSettings
import com.liferlighdow.iteration.data.GestureSettings
import com.liferlighdow.iteration.data.GlassParams
import com.liferlighdow.iteration.data.LauncherConfig
import com.liferlighdow.iteration.data.LauncherSettings
import org.json.JSONArray
import org.json.JSONObject

fun MainViewModel.exportConfig(): String {
    val config = LauncherConfig(
        settings = LauncherSettings(
            themedIcons = _isThemedIconsEnabled.value,
            liquidGlassEnabled = _isLiquidGlassEnabled.value,
            liquidGlassDock = _isLiquidGlassDockEnabled.value,
            liquidGlassHomeFolder = _isLiquidGlassHomeFolderEnabled.value,
            liquidGlassAppLibraryFolder = _isLiquidGlassAppLibraryFolderEnabled.value,
            liquidGlassGlobalSearch = _isLiquidGlassGlobalSearchEnabled.value,
            liquidGlassAppLibrarySearch = _isLiquidGlassAppLibrarySearchEnabled.value,
            liquidGlassWidgets = _isLiquidGlassWidgetsEnabled.value,
            liquidGlassMinusOneWidget = _isLiquidGlassMinusOneWidgetEnabled.value,
            liquidGlassMinusOneSearch = _isLiquidGlassMinusOneSearchEnabled.value,
            liquidGlassMinusOneButton = _isLiquidGlassMinusOneButtonEnabled.value,
            networkAccessEnabled = _isNetworkAccessEnabled.value,
            showMinusOne = _showMinusOnePage.value,
            showAppLibrary = _showAppLibrary.value,
            iconStyle = _iconStyle.value,
            iconShape = _iconShape.value,
            libraryShape = _libraryShape.value,
            searchEngineUrl = _searchEngineUrl.value,
            autoAddAppsToHome = _autoAddAppsToHome.value,
            showStatusBar = _showStatusBar.value,
            showNavigationBar = _showNavigationBar.value,
            pageSize = pageSize,
            actionMode = _actionMode.value,
            themeMode = _themeMode.value,
            appLanguage = _appLanguage.value,
            amoledBlack = _isAmoledBlack.value,
            iconPackPackage = _iconPackPackage.value,
            password = getPassword() ?: "",
            emojiWallpaperText = _emojiWallpaperText.value,
            customWallpaperColor = _customWallpaperColor.value,
            favoriteWallpaperColors = _favoriteWallpaperColors.value,
            homeMenuOptions = _homeMenuOptions.value.filterNotNull().toSet(),
            glassParams = GlassParams(
                blur = _liquidGlassBlur.value,
                refractionHeight = _liquidGlassRefractionHeight.value,
                refractionAmount = _liquidGlassRefractionAmount.value,
                chromaticAberration = _liquidGlassChromaticAberration.value
            ),
            gestures = GestureSettings(
                doubleTapAction = _doubleTapAction.value,
                doubleTapApp = _doubleTapApp.value,
                swipeUpAction = _swipeUpAction.value,
                swipeUpApp = _swipeUpApp.value,
                swipeDownAction = _swipeDownAction.value,
                swipeDownApp = _swipeDownApp.value,
                longPressAction = _longPressAction.value,
                longPressApp = _longPressApp.value,
                twoFingerSwipeUpAction = _twoFingerSwipeUpAction.value,
                twoFingerSwipeUpApp = _twoFingerSwipeUpApp.value,
                twoFingerSwipeDownAction = _twoFingerSwipeDownAction.value,
                twoFingerSwipeDownApp = _twoFingerSwipeDownApp.value
            ),
            customIconSettings = CustomIconSettings(
                bgColor = _customIconBgColor.value,
                fgColor = _customIconFgColor.value,
                useOriginal = _customIconUseOriginal.value,
                useOriginalBg = _customIconUseOriginalBg.value
            )
        ),
        layout = _pages.value,
        dock = _dockPackageNames.value,
        minusOneWidgets = _minusOneWidgets.value,
        favorites = _favoritePackages.value.filterNotNull().toSet(),
        hiddenApps = hiddenPackages,
        customLabels = customLabels,
        customCategories = customCategories,
        userCategories = _userCategories.value,
        categoryRenames = categoryRenames,
        excludedThemed = _excludedThemedPackages.value.filterNotNull().toSet(),
        launchCounts = prefs.getString("launch_counts", "") ?: ""
    )

    return ConfigSerializer.exportConfig(config)
}

fun MainViewModel.importConfig(jsonString: String): Boolean {
    val config = ConfigSerializer.deserializeConfig(jsonString) ?: return false
    return try {
        applyConfig(config)
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun MainViewModel.applyConfig(config: LauncherConfig) {
    val settings = config.settings
    
    // 1. 恢復基礎設定 - 直接操作 State 與 Prefs，避免觸發多個 loadApps
    _isThemedIconsEnabled.value = settings.themedIcons
    _isLiquidGlassEnabled.value = settings.liquidGlassEnabled
    _isLiquidGlassDockEnabled.value = settings.liquidGlassDock
    _isLiquidGlassHomeFolderEnabled.value = settings.liquidGlassHomeFolder
    _isLiquidGlassAppLibraryFolderEnabled.value = settings.liquidGlassAppLibraryFolder
    _isLiquidGlassGlobalSearchEnabled.value = settings.liquidGlassGlobalSearch
    _isLiquidGlassAppLibrarySearchEnabled.value = settings.liquidGlassAppLibrarySearch
    _isLiquidGlassWidgetsEnabled.value = settings.liquidGlassWidgets
    _isLiquidGlassMinusOneWidgetEnabled.value = settings.liquidGlassMinusOneWidget
    _isLiquidGlassMinusOneSearchEnabled.value = settings.liquidGlassMinusOneSearch
    _isLiquidGlassMinusOneButtonEnabled.value = settings.liquidGlassMinusOneButton
    _isNetworkAccessEnabled.value = settings.networkAccessEnabled
    _showMinusOnePage.value = settings.showMinusOne
    _showAppLibrary.value = settings.showAppLibrary
    _autoAddAppsToHome.value = settings.autoAddAppsToHome
    _showStatusBar.value = settings.showStatusBar
    _showNavigationBar.value = settings.showNavigationBar
    
    prefs.edit().apply {
        putBoolean("themed_icons", settings.themedIcons)
        putBoolean("liquid_glass_enabled", settings.liquidGlassEnabled)
        putBoolean("liquid_glass_dock", settings.liquidGlassDock)
        putBoolean("liquid_glass_home_folder", settings.liquidGlassHomeFolder)
        putBoolean("liquid_glass_app_library_folder", settings.liquidGlassAppLibraryFolder)
        putBoolean("liquid_glass_global_search", settings.liquidGlassGlobalSearch)
        putBoolean("liquid_glass_app_library_search", settings.liquidGlassAppLibrarySearch)
        putBoolean("liquid_glass_widgets", settings.liquidGlassWidgets)
        putBoolean("liquid_glass_minus_one_widget", settings.liquidGlassMinusOneWidget)
        putBoolean("liquid_glass_minus_one_search", settings.liquidGlassMinusOneSearch)
        putBoolean("liquid_glass_minus_one_button", settings.liquidGlassMinusOneButton)
        putBoolean("network_access_enabled", settings.networkAccessEnabled)
        putBoolean("show_minus_one", settings.showMinusOne)
        putBoolean("show_app_library", settings.showAppLibrary)
        putBoolean("auto_add_apps_to_home", settings.autoAddAppsToHome)
        putBoolean("show_status_bar", settings.showStatusBar)
        putBoolean("show_navigation_bar", settings.showNavigationBar)
        
        putString("action_mode", settings.actionMode.name)
        putString("icon_style", settings.iconStyle.name)
        putString("icon_shape", settings.iconShape.name)
        putString("library_shape", settings.libraryShape.name)
        putString("search_engine_url", settings.searchEngineUrl)
        putInt("page_size", settings.pageSize)
        putString("password", settings.password)
        
        putFloat("liquid_glass_blur", settings.glassParams.blur)
        putFloat("liquid_glass_refraction_height", settings.glassParams.refractionHeight)
        putFloat("liquid_glass_refraction_amount", settings.glassParams.refractionAmount)
        putBoolean("liquid_glass_chromatic_aberration", settings.glassParams.chromaticAberration)
        
        putString("double_tap_action", settings.gestures.doubleTapAction.name)
        putString("swipe_up_action", settings.gestures.swipeUpAction.name)
        putString("swipe_down_action", settings.gestures.swipeDownAction.name)
        putString("long_press_action", settings.gestures.longPressAction.name)
        putString("two_finger_swipe_up_action", settings.gestures.twoFingerSwipeUpAction.name)
        putString("two_finger_swipe_down_action", settings.gestures.twoFingerSwipeDownAction.name)
        
        putString("double_tap_app", settings.gestures.doubleTapApp)
        putString("swipe_up_app", settings.gestures.swipeUpApp)
        putString("swipe_down_app", settings.gestures.swipeDownApp)
        putString("long_press_app", settings.gestures.longPressApp)
        putString("two_finger_swipe_up_app", settings.gestures.twoFingerSwipeUpApp)
        putString("two_finger_swipe_down_app", settings.gestures.twoFingerSwipeDownApp)
        
        putString("icon_pack_package", settings.iconPackPackage)
        putString("theme_mode", settings.themeMode.name)
        putString("app_language", settings.appLanguage)
        putBoolean("amoled_black", settings.amoledBlack)
        
        putInt("custom_icon_bg_color", settings.customIconSettings.bgColor)
        putInt("custom_icon_fg_color", settings.customIconSettings.fgColor)
        putBoolean("custom_icon_use_original", settings.customIconSettings.useOriginal)
        putBoolean("custom_icon_use_original_bg", settings.customIconSettings.useOriginalBg)
        
        putString("emoji_wallpaper_text", settings.emojiWallpaperText)
        putInt("custom_wallpaper_color", settings.customWallpaperColor)
        putString("favorite_wallpaper_colors", settings.favoriteWallpaperColors.joinToString(","))
        
        putStringSet("favorite_packages", config.favorites)
        putStringSet("excluded_themed_packages", config.excludedThemed)
        putStringSet("home_menu_options", settings.homeMenuOptions)
        putStringSet("hidden_apps", config.hiddenApps)
    }.apply()

    _actionMode.value = settings.actionMode
    _iconStyle.value = settings.iconStyle
    _iconShape.value = settings.iconShape
    _libraryShape.value = settings.libraryShape
    _searchEngineUrl.value = settings.searchEngineUrl
    pageSize = settings.pageSize
    _themeMode.value = settings.themeMode
    _appLanguage.value = settings.appLanguage
    _isAmoledBlack.value = settings.amoledBlack
    _iconPackPackage.value = settings.iconPackPackage
    
    _liquidGlassBlur.value = settings.glassParams.blur
    _liquidGlassRefractionHeight.value = settings.glassParams.refractionHeight
    _liquidGlassRefractionAmount.value = settings.glassParams.refractionAmount
    _liquidGlassChromaticAberration.value = settings.glassParams.chromaticAberration

    _doubleTapAction.value = settings.gestures.doubleTapAction
    _swipeUpAction.value = settings.gestures.swipeUpAction
    _swipeDownAction.value = settings.gestures.swipeDownAction
    _longPressAction.value = settings.gestures.longPressAction
    _twoFingerSwipeUpAction.value = settings.gestures.twoFingerSwipeUpAction
    _twoFingerSwipeDownAction.value = settings.gestures.twoFingerSwipeDownAction
    
    _doubleTapApp.value = settings.gestures.doubleTapApp
    _swipeUpApp.value = settings.gestures.swipeUpApp
    _swipeDownApp.value = settings.gestures.swipeDownApp
    _longPressApp.value = settings.gestures.longPressApp
    _twoFingerSwipeUpApp.value = settings.gestures.twoFingerSwipeUpApp
    _twoFingerSwipeDownApp.value = settings.gestures.twoFingerSwipeDownApp

    _customIconBgColor.value = settings.customIconSettings.bgColor
    _customIconFgColor.value = settings.customIconSettings.fgColor
    _customIconUseOriginal.value = settings.customIconSettings.useOriginal
    _customIconUseOriginalBg.value = settings.customIconSettings.useOriginalBg

    _emojiWallpaperText.value = settings.emojiWallpaperText
    _customWallpaperColor.value = settings.customWallpaperColor
    _favoriteWallpaperColors.value = settings.favoriteWallpaperColors

    _favoritePackages.value = config.favorites
    _excludedThemedPackages.value = config.excludedThemed
    _homeMenuOptions.value = settings.homeMenuOptions
    
    hiddenPackages.clear()
    hiddenPackages.addAll(config.hiddenApps)

    customLabels.clear()
    customLabels.putAll(config.customLabels)
    val labelsJson = JSONObject()
    customLabels.forEach { (k, v) -> labelsJson.put(k, v) }
    prefs.edit().putString("custom_labels_json", labelsJson.toString()).apply()

    customCategories.clear()
    customCategories.putAll(config.customCategories)
    saveCustomCategories()

    _userCategories.value = config.userCategories
    saveUserCategories(config.userCategories)

    categoryRenames.clear()
    categoryRenames.putAll(config.categoryRenames)
    saveCategoryRenames()

    // 3. 恢復負一屏小工具
    _minusOneWidgets.value = config.minusOneWidgets
    saveWidgets(config.minusOneWidgets)

    // 4. 恢復佈局
    val layoutArray = JSONArray()
    config.layout.forEach { page ->
        val pageArray = JSONArray()
        page.forEach { item ->
            pageArray.put(ConfigSerializer.serializeAppModel(item))
        }
        layoutArray.put(pageArray)
    }
    prefs.edit().putString("launcher_layout_v3", layoutArray.toString()).apply()

    // 5. 恢復 Dock & Stats
    _dockPackageNames.value = config.dock
    prefs.edit().putString("dock_packages", config.dock.joinToString(",")).apply()
    prefs.edit().putString("launch_counts", config.launchCounts).apply()

    // 6. 恢復純色/Emoji 桌布 (如果有)
    if (settings.customWallpaperColor != 0) {
        val color = settings.customWallpaperColor
        val emoji = settings.emojiWallpaperText
        val bitmap = android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888).apply {
            eraseColor(color)
        }
        // 注意：這裡我們預設恢復為 Lite 模式或純色模式，因為 Full 模式的大圖不在備份中
        setEmojiWallpaperText(emoji)
        setCustomWallpaper(bitmap)
    }

    // 最後清空快取並統一加載一次
    iconCache.evictAll()
    loadApps()
}
