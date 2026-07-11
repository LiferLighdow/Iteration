package com.liferlighdow.iteration.viewmodel

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.viewModelScope
import com.liferlighdow.iteration.utils.ActionMode
import com.liferlighdow.iteration.utils.GestureAction
import com.liferlighdow.iteration.utils.IconShape
import com.liferlighdow.iteration.utils.IconStyle
import com.liferlighdow.iteration.ui.DockStyle
import com.liferlighdow.iteration.ui.ThemeMode
import com.liferlighdow.iteration.service.UpdateCheckWorker
import androidx.work.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

fun MainViewModel.updateDockApp(slotIndex: Int, packageName: String) {
    val current = _dockPackageNames.value.toMutableList()
    while (current.size <= slotIndex) {
        current.add("")
    }
    current[slotIndex] = packageName
    _dockPackageNames.value = current
    prefs.edit().putString("dock_packages", current.joinToString(",")).apply()
}

fun MainViewModel.setEditMode(enabled: Boolean) {
    _isEditMode.value = enabled
}

fun MainViewModel.setDoubleTapAction(action: GestureAction) {
    _doubleTapAction.value = action
    prefs.edit().putString("double_tap_action", action.name).apply()
}

fun MainViewModel.setActionMode(mode: ActionMode) {
    _actionMode.value = mode
    prefs.edit().putString("action_mode", mode.name).apply()
}

fun MainViewModel.requestRootAccess(onResult: (Boolean) -> Unit) {
    viewModelScope.launch(Dispatchers.IO) {
        val success = try {
            val process = Runtime.getRuntime().exec("su")
            process.outputStream.write("exit\n".toByteArray())
            process.outputStream.flush()
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
        withContext(Dispatchers.Main) {
            onResult(success)
        }
    }
}

fun MainViewModel.checkShizukuPermission(): Boolean {
    return try {
        if (Shizuku.pingBinder()) {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } else false
    } catch (e: Exception) { false }
}

fun MainViewModel.setSwipeUpAction(action: GestureAction) {
    _swipeUpAction.value = action
    prefs.edit().putString("swipe_up_action", action.name).apply()
}

fun MainViewModel.setDoubleTapApp(packageName: String) {
    _doubleTapApp.value = packageName
    prefs.edit().putString("double_tap_app", packageName).apply()
}

fun MainViewModel.setSwipeUpApp(packageName: String) {
    _swipeUpApp.value = packageName
    prefs.edit().putString("swipe_up_app", packageName).apply()
}

fun MainViewModel.setSwipeDownAction(action: GestureAction) {
    _swipeDownAction.value = action
    prefs.edit().putString("swipe_down_action", action.name).apply()
}

fun MainViewModel.setLongPressAction(action: GestureAction) {
    _longPressAction.value = action
    prefs.edit().putString("long_press_action", action.name).apply()
}

fun MainViewModel.setSwipeDownApp(packageName: String) {
    _swipeDownApp.value = packageName
    prefs.edit().putString("swipe_down_app", packageName).apply()
}

fun MainViewModel.setLongPressApp(packageName: String) {
    _longPressApp.value = packageName
    prefs.edit().putString("long_press_app", packageName).apply()
}

fun MainViewModel.setTwoFingerSwipeUpAction(action: GestureAction) {
    _twoFingerSwipeUpAction.value = action
    prefs.edit().putString("two_finger_swipe_up_action", action.name).apply()
}

fun MainViewModel.setTwoFingerSwipeDownAction(action: GestureAction) {
    _twoFingerSwipeDownAction.value = action
    prefs.edit().putString("two_finger_swipe_down_action", action.name).apply()
}

fun MainViewModel.resetGestures() {
    setDoubleTapAction(GestureAction.NONE)
    setSwipeUpAction(GestureAction.NONE)
    setSwipeDownAction(GestureAction.OPEN_GLOBAL_SEARCH)
    setLongPressAction(GestureAction.OPEN_DESKTOP_MENU)
    setTwoFingerSwipeUpAction(GestureAction.NONE)
    setTwoFingerSwipeDownAction(GestureAction.NONE)
}

fun MainViewModel.applySuggestedGestures() {
    setDoubleTapAction(GestureAction.LOCK_SCREEN)
    setSwipeUpAction(GestureAction.OPEN_SYSTEM_SETTINGS)
    setSwipeDownAction(GestureAction.OPEN_GLOBAL_SEARCH)
    setLongPressAction(GestureAction.OPEN_DESKTOP_MENU)
    setTwoFingerSwipeUpAction(GestureAction.LAUNCHER_SETTINGS)
    setTwoFingerSwipeDownAction(GestureAction.OPEN_NOTIFICATIONS)
}

fun MainViewModel.setTwoFingerSwipeUpApp(packageName: String) {
    _twoFingerSwipeUpApp.value = packageName
    prefs.edit().putString("two_finger_swipe_up_app", packageName).apply()
}

fun MainViewModel.setTwoFingerSwipeDownApp(packageName: String) {
    _twoFingerSwipeDownApp.value = packageName
    prefs.edit().putString("two_finger_swipe_down_app", packageName).apply()
}

fun MainViewModel.setThemedIconsEnabled(enabled: Boolean) {
    _isThemedIconsEnabled.value = enabled
    prefs.edit().putBoolean("themed_icons", enabled).apply()
    loadApps()
}

fun MainViewModel.setIconStyle(style: IconStyle) {
    _iconStyle.value = style
    prefs.edit().putString("icon_style", style.name).apply()
    loadApps()
}

fun MainViewModel.setCustomIconBgColor(color: Int) {
    _customIconBgColor.value = color
    prefs.edit().putInt("custom_icon_bg_color", color).apply()
    loadApps()
}

fun MainViewModel.setCustomIconFgColor(color: Int) {
    _customIconFgColor.value = color
    prefs.edit().putInt("custom_icon_fg_color", color).apply()
    loadApps()
}

fun MainViewModel.setCustomIconUseOriginal(useOriginal: Boolean) {
    _customIconUseOriginal.value = useOriginal
    prefs.edit().putBoolean("custom_icon_use_original", useOriginal).apply()
    loadApps()
}

fun MainViewModel.setCustomIconUseOriginalBg(useOriginalBg: Boolean) {
    _customIconUseOriginalBg.value = useOriginalBg
    prefs.edit().putBoolean("custom_icon_use_original_bg", useOriginalBg).apply()
    loadApps()
}

fun MainViewModel.setIconShape(shape: IconShape) {
    _iconShape.value = shape
    prefs.edit().putString("icon_shape", shape.name).apply()
    loadApps()
}

fun MainViewModel.setLibraryShape(shape: IconShape) {
    _libraryShape.value = shape
    prefs.edit().putString("library_shape", shape.name).apply()
}

fun MainViewModel.setIconPack(packageName: String) {
    if (packageName.isNotEmpty()) {
        _isThemedIconsEnabled.value = false
        prefs.edit().putBoolean("themed_icons", false).apply()
    }
    _iconPackPackage.value = packageName
    prefs.edit().putString("icon_pack_package", packageName).apply()
    iconCache.evictAll()
    loadApps()
}

fun MainViewModel.toggleExcludedThemedApp(packageName: String) {
    val current = _excludedThemedPackages.value.toMutableSet()
    if (current.contains(packageName)) {
        current.remove(packageName)
    } else {
        current.add(packageName)
    }
    _excludedThemedPackages.value = current
    prefs.edit().putStringSet("excluded_themed_packages", current).apply()

    clearAppIconCache(packageName)
    loadApps()
}

fun MainViewModel.getInstalledIconPacks() = iconPackManager.getInstalledIconPacks()

fun MainViewModel.setLiquidGlassEnabled(enabled: Boolean) {
    _isLiquidGlassEnabled.value = enabled
    prefs.edit().putBoolean("liquid_glass_enabled", enabled).apply()
    if (enabled) {
        updateBlurredWallpaper()
    }
}

fun MainViewModel.setLiquidGlassDockEnabled(enabled: Boolean) {
    _isLiquidGlassDockEnabled.value = enabled
    prefs.edit().putBoolean("liquid_glass_dock", enabled).apply()
    if (enabled) {
        updateBlurredWallpaper()
    }
}

fun MainViewModel.setLiquidGlassHomeFolderEnabled(enabled: Boolean) {
    _isLiquidGlassHomeFolderEnabled.value = enabled
    prefs.edit().putBoolean("liquid_glass_home_folder", enabled).apply()
    if (enabled) updateBlurredWallpaper()
}

fun MainViewModel.setLiquidGlassAppLibraryFolderEnabled(enabled: Boolean) {
    _isLiquidGlassAppLibraryFolderEnabled.value = enabled
    prefs.edit().putBoolean("liquid_glass_app_library_folder", enabled).apply()
    if (enabled) updateBlurredWallpaper()
}

fun MainViewModel.setLiquidGlassGlobalSearchEnabled(enabled: Boolean) {
    _isLiquidGlassGlobalSearchEnabled.value = enabled
    prefs.edit().putBoolean("liquid_glass_global_search", enabled).apply()
    if (enabled) updateBlurredWallpaper()
}

fun MainViewModel.setLiquidGlassAppLibrarySearchEnabled(enabled: Boolean) {
    _isLiquidGlassAppLibrarySearchEnabled.value = enabled
    prefs.edit().putBoolean("liquid_glass_app_library_search", enabled).apply()
    if (enabled) updateBlurredWallpaper()
}

fun MainViewModel.setLiquidGlassWidgetsEnabled(enabled: Boolean) {
    _isLiquidGlassWidgetsEnabled.value = enabled
    prefs.edit().putBoolean("liquid_glass_widgets", enabled).apply()
    if (enabled) updateBlurredWallpaper()
}

fun MainViewModel.setLiquidGlassMinusOneWidgetEnabled(enabled: Boolean) {
    _isLiquidGlassMinusOneWidgetEnabled.value = enabled
    prefs.edit().putBoolean("liquid_glass_minus_one_widget", enabled).apply()
    if (enabled) updateBlurredWallpaper()
}

fun MainViewModel.setLiquidGlassMinusOneSearchEnabled(enabled: Boolean) {
    _isLiquidGlassMinusOneSearchEnabled.value = enabled
    prefs.edit().putBoolean("liquid_glass_minus_one_search", enabled).apply()
    if (enabled) updateBlurredWallpaper()
}

fun MainViewModel.setLiquidGlassMinusOneButtonEnabled(enabled: Boolean) {
    _isLiquidGlassMinusOneButtonEnabled.value = enabled
    prefs.edit().putBoolean("liquid_glass_minus_one_button", enabled).apply()
    if (enabled) updateBlurredWallpaper()
}

fun MainViewModel.setHomeMenuOption(option: String, enabled: Boolean) {
    val current = _homeMenuOptions.value.toMutableSet()
    if (enabled) current.add(option) else current.remove(option)
    _homeMenuOptions.value = current
    prefs.edit().putStringSet("home_menu_options", current).apply()
}

fun MainViewModel.toggleFavoriteApp(packageName: String) {
    val current = _favoritePackages.value.toMutableSet()
    if (current.contains(packageName)) current.remove(packageName)
    else current.add(packageName)
    _favoritePackages.value = current
    prefs.edit().putStringSet("favorite_packages", current).apply()
}

fun MainViewModel.setShowMinusOnePage(enabled: Boolean) {
    _showMinusOnePage.value = enabled
    prefs.edit().putBoolean("show_minus_one", enabled).apply()
}

fun MainViewModel.setShowAppLibrary(enabled: Boolean) {
    _showAppLibrary.value = enabled
    prefs.edit().putBoolean("show_app_library", enabled).apply()
}

fun MainViewModel.setDesktopLocked(locked: Boolean) {
    _isDesktopLocked.value = locked
    prefs.edit().putBoolean("is_desktop_locked", locked).apply()
}

fun MainViewModel.setLiquidGlassBlur(value: Float) {
    _liquidGlassBlur.value = value
    prefs.edit().putFloat("liquid_glass_blur", value).apply()
}

fun MainViewModel.setLiquidGlassRefractionHeight(value: Float) {
    _liquidGlassRefractionHeight.value = value
    prefs.edit().putFloat("liquid_glass_refraction_height", value).apply()
}

fun MainViewModel.setLiquidGlassRefractionAmount(value: Float) {
    _liquidGlassRefractionAmount.value = value
    prefs.edit().putFloat("liquid_glass_refraction_amount", value).apply()
}

fun MainViewModel.setLiquidGlassChromaticAberration(enabled: Boolean) {
    _liquidGlassChromaticAberration.value = enabled
    prefs.edit().putBoolean("liquid_glass_chromatic_aberration", enabled).apply()
}

fun MainViewModel.setDesktopRows(rows: Int) {
    _desktopRows.value = rows
    prefs.edit().putInt("desktop_rows", rows).apply()
    loadApps()
}

fun MainViewModel.setDockStyle(style: DockStyle) {
    _dockStyle.value = style
    prefs.edit().putString("dock_style", style.name).apply()
}

fun MainViewModel.setDockCornerRadius(radius: Float) {
    _dockCornerRadius.value = radius
    prefs.edit().putFloat("dock_corner_radius", radius).apply()
}

fun MainViewModel.setSearchEngineUrl(url: String) {
    _searchEngineUrl.value = url
    prefs.edit().putString("search_engine_url", url).apply()
}

fun MainViewModel.setPageSize(size: Int) {
    pageSize = size
    repaginate(allApps.value)
}

fun MainViewModel.setNetworkAccessEnabled(enabled: Boolean) {
    _isNetworkAccessEnabled.value = enabled
    prefs.edit().putBoolean("network_access_enabled", enabled).apply()
    if (enabled) {
        fetchExchangeRates()
        fetchWeather()
    }
}

fun MainViewModel.setAutoAddAppsToHome(enabled: Boolean) {
    _autoAddAppsToHome.value = enabled
    prefs.edit().putBoolean("auto_add_apps_to_home", enabled).apply()
}

fun MainViewModel.setThemeMode(mode: ThemeMode) {
    _themeMode.value = mode
    prefs.edit().putString("theme_mode", mode.name).apply()
}

fun MainViewModel.setAmoledBlack(enabled: Boolean) {
    _isAmoledBlack.value = enabled
    prefs.edit().putBoolean("amoled_black", enabled).apply()
}

fun MainViewModel.setShowStatusBar(enabled: Boolean) {
    _showStatusBar.value = enabled
    prefs.edit().putBoolean("show_status_bar", enabled).apply()
}

fun MainViewModel.setShowNavigationBar(enabled: Boolean) {
    _showNavigationBar.value = enabled
    prefs.edit().putBoolean("show_navigation_bar", enabled).apply()
}

fun MainViewModel.setIconCacheSize(size: Int) {
    _iconCacheSize.value = size
    prefs.edit().putInt("icon_cache_size", size).apply()
}

fun MainViewModel.setIconSizePx(size: Int) {
    _iconSizePx.value = size
    prefs.edit().putInt("icon_size_px", size).apply()
    loadApps() // 重新載入以觸發解析度切換
}

fun MainViewModel.applyRecommendedIconSize() {
    val density = getApplication<android.app.Application>().resources.displayMetrics.density
    val rawRecommended = 62 * density
    
    // 階梯式 Guard 邏輯：
    // 1. 將建議像素對齊到 16 的倍數，這對 GPU 紋理上傳與內存對齊最友好。
    // 2. 使用四捨五入確保最接近物理像素。
    val snapped = (Math.round(rawRecommended / 16f) * 16).toInt().coerceIn(32, 256)
    
    setIconSizePx(snapped)
}

fun MainViewModel.setUpdateCheckInterval(hours: Int) {
    _updateCheckInterval.value = hours
    prefs.edit().putInt("update_check_interval", hours).apply()
    
    val workManager = WorkManager.getInstance(getApplication())
    if (hours > 0) {
        val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
            hours.toLong(), TimeUnit.HOURS
        ).setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        ).build()
        
        workManager.enqueueUniquePeriodicWork(
            "update_check",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    } else {
        workManager.cancelUniqueWork("update_check")
    }
}

fun MainViewModel.dismissUpdateDialog() {
    _newVersionAvailable.value = null
    prefs.edit().remove("new_version_available").apply()
}

fun MainViewModel.setAppLanguage(languageCode: String) {
    _appLanguage.value = languageCode
    prefs.edit().putString("app_language", languageCode).apply()
    
    val appLocale: LocaleListCompat = if (languageCode.isEmpty()) {
        LocaleListCompat.getEmptyLocaleList()
    } else {
        LocaleListCompat.forLanguageTags(languageCode)
    }
    AppCompatDelegate.setApplicationLocales(appLocale)
}

fun MainViewModel.resetLiquidGlassParams() {
    setLiquidGlassBlur(0f)
    setLiquidGlassRefractionHeight(24f)
    setLiquidGlassRefractionAmount(48f)
    setLiquidGlassChromaticAberration(true)
}

fun MainViewModel.setOfflineTranslationEnabled(enabled: Boolean) {
    _isOfflineTranslationEnabled.value = enabled
    prefs.edit().putBoolean("offline_translation_enabled", enabled).apply()
}
