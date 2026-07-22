package com.liferlighdow.iteration.viewmodel

import android.app.Application
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import android.content.pm.ShortcutInfo
import android.os.Process
import android.widget.Toast
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewModelScope
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.PwaActivity
import com.liferlighdow.iteration.data.AppModel
import com.liferlighdow.iteration.data.ConfigSerializer
import com.liferlighdow.iteration.ui.DockStyle
import com.liferlighdow.iteration.ui.DynamicColorGenerator
import com.liferlighdow.iteration.ui.ThemeMode
import com.liferlighdow.iteration.utils.ActionMode
import com.liferlighdow.iteration.utils.GestureAction
import com.liferlighdow.iteration.utils.IconShape
import com.liferlighdow.iteration.utils.IconStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import android.graphics.Canvas
import android.graphics.Paint
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import android.graphics.Rect
import android.graphics.RectF
import java.io.FileOutputStream

/**
 * 從 LruCache 或磁碟獲取處理後的圖示
 */
fun MainViewModel.getIcon(uniqueId: String): ImageBitmap? {
    val styleSuffix = currentStyleSuffix
    if (styleSuffix == "default") return null

    // 獲取目前日期與時間 (用於動態圖標匹配)
    val cal = java.util.Calendar.getInstance()
    val currentDay = cal.get(java.util.Calendar.DAY_OF_MONTH).toString()
    val currentHour = cal.get(java.util.Calendar.HOUR_OF_DAY)
    val currentMinute = cal.get(java.util.Calendar.MINUTE)

    // 1. 處理 Shortcut 類型的 ID
    if (uniqueId.startsWith("shortcut_")) {
        val cacheKey = "${uniqueId}_$styleSuffix"
        iconCache[cacheKey]?.let { return it }

        // 檢查磁碟快取 (Shortcut 的圖標已經在保存時轉為檔名安全格式)
        val fileSafeId = uniqueId.replace("/", "_").replace(":", "_").replace("@", "_")
        val diskCacheFile = File(processedIconCacheDir, "${fileSafeId}_$styleSuffix.png")
        if (diskCacheFile.exists()) {
            return try {
                BitmapFactory.decodeFile(diskCacheFile.absolutePath)?.asImageBitmap()?.also {
                    iconCache.put(cacheKey, it)
                }
            } catch (e: Exception) { null }
        }
        return null 
    }

    // 2. 取得基礎 ID (App 類型)
    val baseId = if (uniqueId.contains("@")) {
        val parts = uniqueId.split("@")
        val lastPart = parts.last()
        if (lastPart.length >= 10 && lastPart.toLongOrNull() != null) {
            uniqueId.substringBeforeLast("@")
        } else {
            uniqueId
        }
    } else {
        uniqueId
    }
    
    val pkgName = baseId.substringBefore("/")
    val isDynamicCalendar = _isDynamicCalendarEnabled.value && isCalendarApp(pkgName)
    val isDynamicClock = _isDynamicClockEnabled.value && isClockApp(pkgName)
    
    val cacheKey = when {
        isDynamicCalendar -> "${baseId}_${styleSuffix}_D$currentDay"
        isDynamicClock -> "${baseId}_${styleSuffix}_H${currentHour}M${currentMinute}"
        else -> "${baseId}_$styleSuffix"
    }

    // 2. 檢查 LruCache
    iconCache[cacheKey]?.let { return it }

    // 3. 檢查自定義圖示
    val fileSafeId = baseId.replace("/", "_").replace(":", "_").replace("@", "_")
    val customIconFile = File(customIconDir, "$fileSafeId.png")
    if (customIconFile.exists()) {
        return try {
            BitmapFactory.decodeFile(customIconFile.absolutePath)?.asImageBitmap()?.also {
                iconCache.put(cacheKey, it)
            }
        } catch (e: Exception) { null }
    }

    // 4. 檢查磁碟快取
    val diskCacheFile = when {
        isDynamicCalendar -> File(processedIconCacheDir, "${fileSafeId}_${styleSuffix}_D$currentDay.png")
        isDynamicClock -> File(processedIconCacheDir, "${fileSafeId}_${styleSuffix}_H${currentHour}M${currentMinute}.png")
        else -> File(processedIconCacheDir, "${fileSafeId}_$styleSuffix.png")
    }

    if (diskCacheFile.exists()) {
        return try {
            BitmapFactory.decodeFile(diskCacheFile.absolutePath)?.asImageBitmap()?.also {
                iconCache.put(cacheKey, it)
            }
        } catch (e: Exception) { null }
    }

    // 修復：如果動態圖標開啟但找不到特定時間快取，嘗試尋找原始快取
    if (isDynamicCalendar || isDynamicClock) {
        val fallbackFile = File(processedIconCacheDir, "${fileSafeId}_$styleSuffix.png")
        if (fallbackFile.exists()) {
            return try {
                BitmapFactory.decodeFile(fallbackFile.absolutePath)?.asImageBitmap()
            } catch (e: Exception) { null }
        }
    }

    return null
}

fun MainViewModel.triggerIconUpdate() {
    _iconUpdateSignal.value = System.currentTimeMillis()
}

fun MainViewModel.clearIconCache() {
    viewModelScope.launch(Dispatchers.IO) {
        iconCache.evictAll()
        processedIconCacheDir.listFiles()?.forEach { it.delete() }
        iconProcessor.clearCache()
        
        withContext(Dispatchers.Main) {
            cachedRawApps = null
            loadApps()
            
            val intent = Intent("com.liferlighdow.iteration.ACTION_REFRESH_APPS")
            intent.setPackage(getApplication<Application>().packageName)
            getApplication<Application>().sendBroadcast(intent)
        }
    }
}

fun MainViewModel.loadHiddenPackages() {
    val saved = prefs.getStringSet("hidden_apps", emptySet()) ?: emptySet()
    hiddenPackages.clear()
    hiddenPackages.addAll(saved)
}

fun MainViewModel.loadFrozenPackages() {
    val saved = prefs.getStringSet("frozen_apps", emptySet()) ?: emptySet()
    frozenPackages.clear()
    frozenPackages.addAll(saved)
}

fun MainViewModel.loadCustomLabels() {
    val saved = prefs.getString("custom_labels_json", null)
    customLabels.clear()
    if (saved != null) {
        val json = JSONObject(saved)
        json.keys().forEach { key: String ->
            // 修復：移除通用的 PWA 標籤鍵值，避免全域覆蓋
            if (key != "com.iteration.pwa") {
                customLabels[key] = json.getString(key)
            }
        }
    }
}

fun MainViewModel.loadCustomCategories() {
    val saved = prefs.getString("custom_categories_json", null)
    customCategories.clear()
    if (saved != null) {
        val json = JSONObject(saved)
        json.keys().forEach { key: String ->
            customCategories[key] = json.getString(key)
        }
    }
}

fun MainViewModel.saveCustomCategories() {
    val json = JSONObject()
    customCategories.forEach { (k, v) -> json.put(k, v) }
    prefs.edit().putString("custom_categories_json", json.toString()).apply()
}

fun MainViewModel.setAppCategory(uniqueId: String, category: String) {
    if (category.isBlank()) {
        customCategories.remove(uniqueId)
    } else {
        customCategories[uniqueId] = category
    }
    saveCustomCategories()
    loadApps()
}

fun MainViewModel.setCustomLabel(uniqueId: String, newLabel: String) {
    if (newLabel.isBlank()) {
        customLabels.remove(uniqueId)
    } else {
        customLabels[uniqueId] = newLabel
    }
    val json = JSONObject()
    customLabels.forEach { (k, v) -> json.put(k, v) }
    prefs.edit().putString("custom_labels_json", json.toString()).apply()
    loadApps()
}

fun MainViewModel.setCustomIcon(uniqueId: String, bitmap: Bitmap) {
    val fileSafeId = uniqueId.replace("/", "_").replace(":", "_").replace("@", "_")
    val file = File(customIconDir, "$fileSafeId.png")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    iconCache.remove(uniqueId)
    loadApps()
}

fun MainViewModel.resetCustomIcon(uniqueId: String) {
    val fileSafeId = uniqueId.replace("/", "_").replace(":", "_").replace("@", "_")
    val file = File(customIconDir, "$fileSafeId.png")
    if (file.exists()) file.delete()
    iconCache.remove(uniqueId)
    loadApps()
}

fun MainViewModel.toggleHiddenApp(packageName: String) {
    if (hiddenPackages.contains(packageName)) {
        hiddenPackages.remove(packageName)
    } else {
        hiddenPackages.add(packageName)
        removeAppFromHomeByPackageWithAnimation(packageName)
    }
    prefs.edit().putStringSet("hidden_apps", hiddenPackages).apply()
    clearAppIconCache(packageName)
    loadApps()
}

fun MainViewModel.toggleFreezeApp(app: AppModel, context: Context) {
    val pkg = app.packageName
    val userId = app.userId
    val isFrozen = app.isFrozen
    val mode = _actionMode.value

    viewModelScope.launch {
        val success = withContext(Dispatchers.IO) {
            if (isFrozen) {
                // Unfreeze
                when (mode) {
                    ActionMode.SHIZUKU -> {
                        executeShizukuCommandSilent(arrayOf("pm", "enable", "--user", userId.toString(), pkg))
                    }
                    ActionMode.ROOT -> {
                        executeCommandSilent(arrayOf("su", "-c", "pm enable --user $userId $pkg"))
                    }
                    else -> false
                }
            } else {
                // Freeze
                when (mode) {
                    ActionMode.SHIZUKU -> {
                        executeShizukuCommandSilent(arrayOf("pm", "disable-user", "--user", userId.toString(), pkg))
                    }
                    ActionMode.ROOT -> {
                        executeCommandSilent(arrayOf("su", "-c", "pm disable-user --user $userId $pkg"))
                    }
                    else -> false
                }
            }
        }

        if (success) {
            if (isFrozen) {
                frozenPackages.remove(pkg)
            } else {
                frozenPackages.add(pkg)
                removeAppFromHomeByPackageWithAnimation(pkg)
            }
            prefs.edit().putStringSet("frozen_apps", frozenPackages).apply()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, if (isFrozen) R.string.unfreeze_success else R.string.freeze_success, Toast.LENGTH_SHORT).show()
                loadApps()
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, R.string.action_failed_check_permission, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private fun executeShizukuCommandSilent(command: Array<String>): Boolean {
    if (!rikka.shizuku.Shizuku.pingBinder()) return false
    return try {
        val method = rikka.shizuku.Shizuku::class.java.declaredMethods.find { 
            it.name == "newProcess" && it.parameterTypes.size == 3 && it.parameterTypes[0].isArray
        }
        if (method != null) {
            method.isAccessible = true
            val process = method.invoke(null, command, null, null) as rikka.shizuku.ShizukuRemoteProcess
            process.waitFor() == 0
        } else false
    } catch (e: Exception) { false }
}

private fun executeCommandSilent(command: Array<String>): Boolean {
    return try {
        Runtime.getRuntime().exec(command).waitFor() == 0
    } catch (e: Exception) { false }
}

fun MainViewModel.removeAppFromHomeByPackage(packageName: String) {
    val currentPages = _pages.value.map { page ->
        page.filter { item ->
            if (item.isFolder) true else item.packageName != packageName
        }.map { item ->
            if (item.isFolder) {
                item.copy(folderItems = item.folderItems.filter { it.packageName != packageName })
            } else item
        }
    }.filter { it.isNotEmpty() }

    _pages.value = currentPages
    saveLayout()
}

fun MainViewModel.removeAppFromHomeByPackageWithAnimation(packageName: String) {
    val idsToAnimate = _pages.value.flatten().flatMap { 
        if (it.isFolder) it.folderItems.filter { f -> f.packageName == packageName }.map { f -> f.uniqueId }
        else if (it.packageName == packageName) listOf(it.uniqueId)
        else emptyList()
    }
    if (idsToAnimate.isNotEmpty()) {
        viewModelScope.launch {
            _removingItemIds.value += idsToAnimate
            delay(500)
            removeAppFromHomeByPackage(packageName)
            _removingItemIds.value -= idsToAnimate
        }
    } else {
        removeAppFromHomeByPackage(packageName)
    }
}

fun MainViewModel.clearAppIconCache(packageName: String) {
    processedIconCacheDir.listFiles { _, name ->
        name.startsWith("${packageName}_")
    }?.forEach { it.delete() }
    iconCache.evictAll()
}

fun MainViewModel.loadUserCategories() {
    val saved = prefs.getString("user_categories_ordered", null)
    _userCategories.value = saved?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
}

fun MainViewModel.saveUserCategories(list: List<String>) {
    prefs.edit().putString("user_categories_ordered", list.joinToString(",")).apply()
}

fun MainViewModel.addUserCategory(name: String) {
    val current = _userCategories.value.toMutableList()
    if (!current.contains(name)) {
        current.add(name)
        _userCategories.value = current
        saveUserCategories(current)
    }
}

fun MainViewModel.deleteUserCategory(name: String) {
    val current = _userCategories.value.toMutableList()
    current.remove(name)
    _userCategories.value = current
    saveUserCategories(current)

    val toRemove = customCategories.filter { it.value == name }.keys
    toRemove.forEach { customCategories.remove(it) }
    saveCustomCategories()
    loadApps()
}

fun MainViewModel.moveUserCategory(fromIndex: Int, toIndex: Int) {
    val current = _userCategories.value.toMutableList()
    if (fromIndex in current.indices && toIndex in current.indices) {
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        _userCategories.value = current
        saveUserCategories(current)
    }
}

fun MainViewModel.loadCategoryRenames() {
    val saved = prefs.getString("category_renames_json", null)
    categoryRenames.clear()
    if (saved != null) {
        val json = JSONObject(saved)
        json.keys().forEach { key: String ->
            categoryRenames[key] = json.getString(key)
        }
    }
}

fun MainViewModel.saveCategoryRenames() {
    val json = JSONObject()
    categoryRenames.forEach { (k, v) -> json.put(k, v) }
    prefs.edit().putString("category_renames_json", json.toString()).apply()
}

fun MainViewModel.renameCategory(oldName: String, newName: String) {
    if (oldName == newName || newName.isBlank()) return
    categoryRenames[oldName] = newName
    saveCategoryRenames()

    val current = _userCategories.value.toMutableList()
    val index = current.indexOf(oldName)
    if (index != -1) {
        current[index] = newName
        _userCategories.value = current
        saveUserCategories(current)
    }

    val appsToUpdate = customCategories.filter { it.value == oldName }.keys
    appsToUpdate.forEach { customCategories[it] = newName }
    if (appsToUpdate.isNotEmpty()) saveCustomCategories()
    loadApps()
}

fun MainViewModel.updateSuggestions() {
    val launchCounts = prefs.getString("launch_counts", "") ?: ""
    val topPackages = launchCounts.splitToSequence(",")
        .filter { it.contains("|") }
        .map { it.split("|") }
        .filter { it.size == 2 }
        .map { it[0] to (it[1].toIntOrNull() ?: 0) }
        .sortedByDescending { it.second }
        .take(8)
        .map { it.first }
        .toList()

    _suggestedApps.value = _allApps.value.filter { 
        topPackages.contains(it.packageName) && !it.isHidden && !it.isPrivate && !it.isFrozen 
    }
}

fun MainViewModel.logAppLaunch(packageName: String) {
    val launchCounts = prefs.getString("launch_counts", "") ?: ""
    val countsMap = launchCounts.split(",").filter { it.contains("|") }.associate {
        val parts = it.split("|")
        parts[0] to (parts[1].toIntOrNull() ?: 0)
    }.toMutableMap()

    countsMap[packageName] = (countsMap[packageName] ?: 0) + 1
    val serialized = countsMap.map { "${it.key}|${it.value}" }.joinToString(",")
    prefs.edit().putString("launch_counts", serialized).apply()
    updateSuggestions()
}

fun MainViewModel.processNewIcon(
    app: AppModel,
    currentIconPack: String,
    isThemed: Boolean,
    isExcluded: Boolean,
    themeColors: ColorScheme?,
    currentStyle: IconStyle,
    currentShape: IconShape,
    sizePx: Int,
    customBg: Int,
    customFg: Int,
    customOriginal: Boolean,
    customOriginalBg: Boolean,
    activityInfoCache: Map<UserHandle, List<LauncherActivityInfo>>,
    calendarDay: String? = null,
    clockTime: Pair<Int, Int>? = null
): ImageBitmap {
    val userManager = getApplication<Application>().getSystemService(Context.USER_SERVICE) as UserManager
    val userHandle = userManager.userProfiles.find { 
        userManager.getSerialNumberForUser(it) == app.userId 
    } ?: android.os.Process.myUserHandle()

    val rawIcon = try {
        val userActivities = activityInfoCache[userHandle] ?: emptyList()
        if (app.uniqueId.contains("/")) {
            val componentStr = app.uniqueId.substringBefore("@")
            val pkg = componentStr.substringBefore("/")
            val cls = componentStr.substringAfter("/")
            val component = android.content.ComponentName(pkg, cls)
            userActivities.find { it.componentName == component }?.getIcon(getApplication<Application>().resources.displayMetrics.densityDpi)
        } else {
            userActivities.find { it.applicationInfo.packageName == app.packageName }?.getIcon(getApplication<Application>().resources.displayMetrics.densityDpi)
        }
    } catch (e: Exception) { null }

    val finalRawIcon = rawIcon ?: try {
        getApplication<Application>().packageManager.getApplicationIcon(app.packageName)
    } catch (e: Exception) { null }

    if (isExcluded) {
        return iconProcessor.processIcon(finalRawIcon, false, null, IconStyle.STANDARD, currentShape, sizePx, customBgColor = 0, customFgColor = 0, customUseOriginal = true, customUseOriginalBg = true, userId = app.userId, calendarDay = calendarDay, clockTime = clockTime)
    }

    return if (currentIconPack.isNotEmpty()) {
        val ipIcon = iconPackManager.getIcon(app.packageName, app.uniqueId)
        if (ipIcon != null) {
            iconProcessor.processIcon(ipIcon, false, null, IconStyle.STANDARD, currentShape, sizePx, isIconPack = true, userId = app.userId, isPrivate = app.isPrivate, calendarDay = calendarDay, clockTime = clockTime)
        } else {
            iconProcessor.processIcon(finalRawIcon, false, null, IconStyle.CUSTOM, currentShape, sizePx, customBgColor = 0, customFgColor = 0, customUseOriginal = true, customUseOriginalBg = true, userId = app.userId, isPrivate = app.isPrivate, calendarDay = calendarDay, clockTime = clockTime)
        }
    } else {
        iconProcessor.processIcon(finalRawIcon, isThemed, themeColors, currentStyle, currentShape, sizePx, customBgColor = customBg, customFgColor = customFg, customUseOriginal = customOriginal, customUseOriginalBg = customOriginalBg, userId = app.userId, isPrivate = app.isPrivate, calendarDay = calendarDay, clockTime = clockTime)
    }
}

fun MainViewModel.saveIconToDisk(bitmap: ImageBitmap, file: File) {
    try {
        val b = bitmap.asAndroidBitmap()
        FileOutputStream(file).use { out ->
            b.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    } catch (e: Exception) { e.printStackTrace() }
}

fun MainViewModel.loadSettings() {
    loadHiddenPackages()
    loadFrozenPackages()
    loadCustomLabels()
    loadUserCategories()
    loadCustomCategories()
    loadCategoryRenames()
    loadWidgets()
    loadDock()
    loadPwaApps()

    val savedStyleStr = prefs.getString("icon_style", "STANDARD") ?: "STANDARD"
    val newStyle = try { IconStyle.valueOf(savedStyleStr) } catch (e: Exception) { IconStyle.STANDARD }
    val savedShapeStr = prefs.getString("icon_shape", "DEFAULT") ?: "DEFAULT"
    val newShape = try { IconShape.valueOf(savedShapeStr) } catch (e: Exception) { IconShape.DEFAULT }
    val savedLibShapeStr = prefs.getString("library_shape", "DEFAULT") ?: "DEFAULT"
    val newLibShape = try { IconShape.valueOf(savedLibShapeStr) } catch (e: Exception) { IconShape.DEFAULT }
    val newThemed = prefs.getBoolean("themed_icons", false)
    val newLiquidEnabled = prefs.getBoolean("liquid_glass_enabled", false)
    val newLiquidDockEnabled = prefs.getBoolean("liquid_glass_dock", false)
    val newLiquidHomeFolderEnabled = prefs.getBoolean("liquid_glass_home_folder", false)
    val newLiquidAppLibraryFolderEnabled = prefs.getBoolean("liquid_glass_app_library_folder", false)
    val newLiquidGlobalSearchEnabled = prefs.getBoolean("liquid_glass_global_search", false)
    val newLiquidAppLibrarySearchEnabled = prefs.getBoolean("liquid_glass_app_library_search", false)
    val newLiquidWidgetsEnabled = prefs.getBoolean("liquid_glass_widgets", false)
    val newLiquidMinusOneWidgetEnabled = prefs.getBoolean("liquid_glass_minus_one_widget", false)
    val newLiquidMinusOneSearchEnabled = prefs.getBoolean("liquid_glass_minus_one_search", false)
    val newLiquidMinusOneButtonEnabled = prefs.getBoolean("liquid_glass_minus_one_button", false)
    val newNetworkAccessEnabled = prefs.getBoolean("network_access_enabled", true)
    val newShowMinusOne = prefs.getBoolean("show_minus_one", true)
    val newShowAppLibrary = prefs.getBoolean("show_app_library", true)
    val newLiquidBlur = prefs.getFloat("liquid_glass_blur", 0f)
    val newLiquidRefractionHeight = prefs.getFloat("liquid_glass_refraction_height", 24f)
    val newLiquidRefractionAmount = prefs.getFloat("liquid_glass_refraction_amount", 48f)
    val newLiquidChromaticAberration = prefs.getBoolean("liquid_glass_chromatic_aberration", true)
    val newDoubleTapActionStr = prefs.getString("double_tap_action", "NONE") ?: "NONE"
    val newDoubleTapAction = try { GestureAction.valueOf(newDoubleTapActionStr) } catch (e: Exception) { GestureAction.NONE }
    val newIconPackPackage = prefs.getString("icon_pack_package", "") ?: ""
    val newExcluded = prefs.getStringSet("excluded_themed_packages", emptySet()) ?: emptySet()
    val newRows = prefs.getInt("desktop_rows", 0)
    val newDockStyle = try { DockStyle.valueOf(prefs.getString("dock_style", "MODERN") ?: "MODERN") } catch (e: Exception) { DockStyle.MODERN }
    val newSearchEngine = prefs.getString("search_engine_url", "https://www.google.com/search?q=") ?: "https://www.google.com/search?q="
    val newAutoAdd = prefs.getBoolean("auto_add_apps_to_home", true)
    val newShowStatusBar = prefs.getBoolean("show_status_bar", true)
    val newShowNavigationBar = prefs.getBoolean("show_navigation_bar", true)
    val newThemeMode = try {
        ThemeMode.valueOf(prefs.getString("theme_mode", "FOLLOW_SYSTEM") ?: "FOLLOW_SYSTEM")
    } catch (e: Exception) {
        ThemeMode.FOLLOW_SYSTEM
    }
    val newAmoled = prefs.getBoolean("amoled_black", false)
    val newIconSize = prefs.getInt("icon_size_px", -1)

    if (_iconStyle.value != newStyle || _isThemedIconsEnabled.value != newThemed || _iconPackPackage.value != newIconPackPackage || _iconShape.value != newShape || _libraryShape.value != newLibShape || _excludedThemedPackages.value != newExcluded || _desktopRows.value != newRows || _dockStyle.value != newDockStyle || _iconSizePx.value != newIconSize) {
        _iconStyle.value = newStyle
        _iconShape.value = newShape
        _libraryShape.value = newLibShape
        _isThemedIconsEnabled.value = newThemed
        _iconPackPackage.value = newIconPackPackage
        _excludedThemedPackages.value = newExcluded
        _desktopRows.value = newRows
        _dockStyle.value = newDockStyle
        _iconSizePx.value = newIconSize
    }

    _searchEngineUrl.value = newSearchEngine
    _autoAddAppsToHome.value = newAutoAdd
    _showStatusBar.value = newShowStatusBar
    _showNavigationBar.value = newShowNavigationBar
    _themeMode.value = newThemeMode
    _isAmoledBlack.value = newAmoled
    _isLiquidGlassEnabled.value = newLiquidEnabled
    _isLiquidGlassDockEnabled.value = newLiquidDockEnabled
    _isLiquidGlassHomeFolderEnabled.value = newLiquidHomeFolderEnabled
    _isLiquidGlassAppLibraryFolderEnabled.value = newLiquidAppLibraryFolderEnabled
    _isLiquidGlassGlobalSearchEnabled.value = newLiquidGlobalSearchEnabled
    _isLiquidGlassAppLibrarySearchEnabled.value = newLiquidAppLibrarySearchEnabled
    _isLiquidGlassWidgetsEnabled.value = newLiquidWidgetsEnabled
    _isLiquidGlassMinusOneWidgetEnabled.value = newLiquidMinusOneWidgetEnabled
    _isLiquidGlassMinusOneSearchEnabled.value = newLiquidMinusOneSearchEnabled
    _isLiquidGlassMinusOneButtonEnabled.value = newLiquidMinusOneButtonEnabled
    _isNetworkAccessEnabled.value = newNetworkAccessEnabled
    _showMinusOnePage.value = newShowMinusOne
    _showAppLibrary.value = newShowAppLibrary
    _liquidGlassBlur.value = newLiquidBlur
    _liquidGlassRefractionHeight.value = newLiquidRefractionHeight
    _liquidGlassRefractionAmount.value = newLiquidRefractionAmount
    _liquidGlassChromaticAberration.value = newLiquidChromaticAberration
    _doubleTapAction.value = newDoubleTapAction

    _swipeUpAction.value = try {
        GestureAction.valueOf(prefs.getString("swipe_up_action", "NONE") ?: "NONE")
    } catch (e: Exception) {
        GestureAction.NONE
    }
    _doubleTapApp.value = prefs.getString("double_tap_app", "") ?: ""
    _swipeUpApp.value = prefs.getString("swipe_up_app", "") ?: ""
    _swipeDownAction.value = try {
        GestureAction.valueOf(prefs.getString("swipe_down_action", "OPEN_GLOBAL_SEARCH") ?: "OPEN_GLOBAL_SEARCH")
    } catch (e: Exception) {
        GestureAction.OPEN_GLOBAL_SEARCH
    }
    _longPressAction.value = try {
        GestureAction.valueOf(prefs.getString("long_press_action", "OPEN_DESKTOP_MENU") ?: "OPEN_DESKTOP_MENU")
    } catch (e: Exception) {
        GestureAction.OPEN_DESKTOP_MENU
    }
    _swipeDownApp.value = prefs.getString("swipe_down_app", "") ?: ""
    _longPressApp.value = prefs.getString("long_press_app", "") ?: ""
    _twoFingerSwipeUpAction.value = try {
        GestureAction.valueOf(prefs.getString("two_finger_swipe_up_action", "NONE") ?: "NONE")
    } catch (e: Exception) {
        GestureAction.NONE
    }
    _twoFingerSwipeDownAction.value = try {
        GestureAction.valueOf(prefs.getString("two_finger_swipe_down_action", "OPEN_NOTIFICATIONS") ?: "OPEN_NOTIFICATIONS")
    } catch (e: Exception) {
        GestureAction.OPEN_NOTIFICATIONS
    }
    _twoFingerSwipeUpApp.value = prefs.getString("two_finger_swipe_up_app", "") ?: ""
    _twoFingerSwipeDownApp.value = prefs.getString("two_finger_swipe_down_app", "") ?: ""

    _homeMenuOptions.value = prefs.getStringSet("home_menu_options", setOf("delete_home", "uninstall", "shortcuts", "freeze")) ?: setOf("delete_home", "uninstall", "shortcuts", "freeze")
    _favoritePackages.value = prefs.getStringSet("favorite_packages", emptySet()) ?: emptySet()
}

fun MainViewModel.loadDock() {
    val saved = prefs.getString("dock_items_v2", null)
    if (saved != null) {
        try {
            val array = JSONArray(saved)
            val list = mutableListOf<AppModel>()
            for (i in 0 until array.length()) {
                ConfigSerializer.deserializeAppModel(array.getString(i))?.let { list.add(it) }
            }
            while (list.size < 4) list.add(AppModel(label = "", packageName = "", uniqueId = "empty_dock_${list.size}"))
            _dockItems.value = list.take(4)
            _dockPackageNames.value = list.take(4).map { it.packageName }
            return
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 舊版相容邏輯
    val savedOld = prefs.getString("dock_packages", null)
    val list = if (savedOld != null) {
        val decoded = savedOld.split(",").toMutableList()
        while (decoded.size < 4) decoded.add("")
        decoded.take(4)
    } else {
        List(4) { "" }
    }
    _dockPackageNames.value = list
}

fun MainViewModel.saveDock() {
    val array = JSONArray()
    _dockItems.value.forEach { array.put(ConfigSerializer.serializeAppModel(it)) }
    val pkgNames = _dockItems.value.map { it.packageName }
    prefs.edit()
        .putString("dock_items_v2", array.toString())
        .putString("dock_packages", pkgNames.joinToString(","))
        .apply()
    _dockPackageNames.value = pkgNames
}

fun MainViewModel.replaceDockApp(index: Int, app: AppModel) {
    val current = _dockItems.value.toMutableList()
    if (index in current.indices) {
        current[index] = app
        _dockItems.value = current
        saveDock()
    }
}

fun MainViewModel.removeAppFromDock(index: Int) {
    val current = _dockItems.value.toMutableList()
    if (index in current.indices) {
        current[index] = AppModel(label = "", packageName = "", uniqueId = "empty_dock_$index")
        _dockItems.value = current
        saveDock()
    }
}

fun MainViewModel.convertDockAppToFolder(index: Int) {
    val current = _dockItems.value.toMutableList()
    if (index in current.indices) {
        val app = current[index]
        if (!app.isFolder && app.packageName.isNotEmpty()) {
            val newFolder = AppModel(
                label = getApplication<Application>().getString(R.string.folder_default_name),
                isFolder = true,
                folderItems = listOf(app),
                uniqueId = "folder_${System.currentTimeMillis()}"
            )
            current[index] = newFolder
        } else if (app.packageName.isEmpty()) {
            // 如果是空的，建立一個空的資料夾
            current[index] = AppModel(
                label = getApplication<Application>().getString(R.string.folder_default_name),
                isFolder = true,
                uniqueId = "folder_${System.currentTimeMillis()}"
            )
        }
        _dockItems.value = current
        saveDock()
    }
}

@android.annotation.SuppressLint("MissingPermission")
fun MainViewModel.loadApps() {
    loadAppsJob?.cancel()

    loadAppsJob = viewModelScope.launch {
        delay(300)
        withContext(Dispatchers.Default) {
            loadSettings()
        }
        updateBlurredWallpaper()

        val isThemed = _isThemedIconsEnabled.value
        val currentStyle = _iconStyle.value
        val currentShape = _iconShape.value
        val currentIconPack = _iconPackPackage.value
        
        // 獲取目前日期與時間
        val cal = java.util.Calendar.getInstance()
        val currentDay = cal.get(java.util.Calendar.DAY_OF_MONTH).toString()
        val currentHour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = cal.get(java.util.Calendar.MINUTE)

        if (currentStyle == IconStyle.CUSTOM) {
            delay(200)
        }
        if (currentIconPack.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                iconPackManager.loadIconPack(currentIconPack)
            }
        }

        val rawApps = cachedRawApps ?: withContext(Dispatchers.IO) {
            repository.getInstalledApps(frozenPackages).also { cachedRawApps = it }
        }

        // 合併 PWA 應用程式
        val appsToProcess = rawApps + _pwaApps.value

        val themeColors = if (isThemed) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dynamicLightColorScheme(getApplication())
            } else {
                val seed = withContext(Dispatchers.IO) {
                    try {
                        val wm = WallpaperManager.getInstance(getApplication())
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                            wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)?.primaryColor?.toArgb()
                        } else {
                            wm.drawable?.toBitmap()
                                ?.let { DynamicColorGenerator.extractSeedColorFromBitmap(it) }
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                seed?.let { DynamicColorGenerator.generateColorSchemeFromSeed(it, false) }
            }
        } else null

        val colorKey = themeColors?.primary?.let {
            val argb = ((it.alpha * 255).toInt() shl 24) or
                       ((it.red * 255).toInt() shl 16) or
                       ((it.green * 255).toInt() shl 8) or
                       (it.blue * 255).toInt()
            argb.toString(16)
        } ?: "default"

        val customBg = _customIconBgColor.value
        val customFg = _customIconFgColor.value
        val customOriginal = _customIconUseOriginal.value
        val customOriginalBg = _customIconUseOriginalBg.value
        val customKey = if (currentStyle == IconStyle.CUSTOM) {
            "C_${customBg.toString(16)}_${customFg.toString(16)}_${if (customOriginal) "O" else "M"}_${if (customOriginalBg) "OB" else "CB"}"
        } else "N"

        // 根據拉條設定決定渲染解析度 (畫質)
        val density = getApplication<Application>().resources.displayMetrics.density
        val userIconSize = _iconSizePx.value
        
        // 關鍵優化：即便在預設模式下，也進行 16px 階梯對齊，以適配 fractional density
        val renderingSizePx = if (userIconSize > 0) {
            userIconSize 
        } else {
            (Math.round(62 * density / 16f) * 16).toInt().coerceIn(32, 256)
        }

        val newStyleSuffix = if (currentIconPack.isNotEmpty()) {
            "IP_V13_${currentIconPack.hashCode()}_${currentShape.name}_${currentStyle.name}_${if (isThemed) "T_$colorKey" else "N"}_${customKey}_Q$renderingSizePx"
        } else {
            "V13_${currentStyle.name}_${currentShape.name}_${if (isThemed) "T_$colorKey" else "N"}_${customKey}_Q$renderingSizePx"
        }

        if (currentStyleSuffix != newStyleSuffix) {
            currentStyleSuffix = newStyleSuffix
            iconCache.evictAll()
        }
        themeColorsCache = themeColors

        val launcherApps = getApplication<Application>().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val userManager = getApplication<Application>().getSystemService(Context.USER_SERVICE) as UserManager
        val activityInfoCache = withContext(Dispatchers.IO) {
            userManager.userProfiles.associateWith { handle ->
                launcherApps.getActivityList(null, handle)
            }
        }

        val processedApps: List<AppModel> = withContext(Dispatchers.Default) {
            val styleSuffix = currentStyleSuffix
            val semaphore = Semaphore(8)

            coroutineScope {
                appsToProcess.map { app ->
                    async {
                        semaphore.withPermit {
                            val fileSafeId = app.uniqueId.replace("/", "_").replace(":", "_").replace("@", "_")
                            val customIconFile = File(customIconDir, "$fileSafeId.png")
                            val legacyCustomIconFile = File(customIconDir, "${app.packageName}.png")
                            val diskCacheFile = File(processedIconCacheDir, "${fileSafeId}_$styleSuffix.png")
                            val cacheKey = "${app.uniqueId}_$styleSuffix"
                            val isExcluded = excludedThemedPackages.value.contains(app.packageName)
                            val isDynamicCalendar = _isDynamicCalendarEnabled.value && isCalendarApp(app.packageName)
                            val isDynamicClock = _isDynamicClockEnabled.value && isClockApp(app.packageName)
                            
                            val calendarDayToPass = if (isDynamicCalendar) currentDay else null
                            val clockTimeToPass = if (isDynamicClock) Pair(currentHour, currentMinute) else null
                            
                            // 針對動態 App，快取 Key 必須包含時間標籤
                            val finalCacheKey = when {
                                isDynamicCalendar -> "${cacheKey}_D$currentDay"
                                isDynamicClock -> "${cacheKey}_H${currentHour}M$currentMinute"
                                else -> cacheKey
                            }
                            val finalDiskFile = when {
                                isDynamicCalendar -> File(processedIconCacheDir, "${fileSafeId}_${styleSuffix}_D$currentDay.png")
                                isDynamicClock -> File(processedIconCacheDir, "${fileSafeId}_${styleSuffix}_H${currentHour}M$currentMinute.png")
                                else -> diskCacheFile
                            }

                            if (iconCache[finalCacheKey] == null) {
                                val customToLoad = if (customIconFile.exists()) customIconFile else if (legacyCustomIconFile.exists()) legacyCustomIconFile else null
                                if (customToLoad != null) {
                                    BitmapFactory.decodeFile(customToLoad.absolutePath)?.asImageBitmap()?.let {
                                        iconCache.put(finalCacheKey, it)
                                    }
                                } else if (finalDiskFile.exists()) {
                                    BitmapFactory.decodeFile(finalDiskFile.absolutePath)?.let {
                                        iconCache.put(finalCacheKey, it.asImageBitmap())
                                    }
                                } else {
                                    // 關鍵修改：區分 PWA 與一般 App 的生成邏輯
                                    val processed = if (app.isPWA) {
                                        generatePwaIcon(app, renderingSizePx)?.asImageBitmap()
                                    } else {
                                        processNewIcon(app, currentIconPack, isThemed, isExcluded, themeColors, currentStyle, currentShape, renderingSizePx, customBg, customFg, customOriginal, customOriginalBg, activityInfoCache, calendarDayToPass, clockTimeToPass)
                                    }
                                    
                                    processed?.let {
                                        saveIconToDisk(it, finalDiskFile)
                                        iconCache.put(finalCacheKey, it)
                                    }
                                }
                            }

                            val appRes = getApplication<Application>()
                            val rawCategory = customCategories[app.uniqueId] ?: customCategories[app.packageName] ?: when (app.category) {
                                0 -> appRes.getString(R.string.cat_games)
                                1 -> appRes.getString(R.string.cat_audio)
                                2 -> appRes.getString(R.string.cat_video)
                                3 -> appRes.getString(R.string.cat_imaging)
                                4 -> appRes.getString(R.string.cat_social)
                                5 -> appRes.getString(R.string.cat_news)
                                6 -> appRes.getString(R.string.cat_maps)
                                7 -> appRes.getString(R.string.cat_productivity)
                                else -> appRes.getString(R.string.cat_other)
                            }

                            val displayCategory = categoryRenames[rawCategory] ?: rawCategory
                            
                            // 標籤選取邏輯優化：確保桌面上的項目能對應到正確的自定義標籤
                            val baseId = if (app.uniqueId.contains("@")) app.uniqueId.substringBeforeLast("@") else app.uniqueId
                            
                            val finalLabel = when {
                                app.isPWA -> customLabels[baseId] ?: app.label
                                else -> customLabels[baseId] ?: customLabels[app.packageName] ?: app.label
                            }

                            app.copy(
                                label = finalLabel,
                                isHidden = hiddenPackages.contains(app.packageName) || hiddenPackages.contains(app.uniqueId),
                                displayCategory = when {
                                    app.isPWA -> "PWA Apps"
                                    else -> displayCategory
                                }
                            )
                        }
                    }
                }.awaitAll()
            }
        }

        _allApps.value = processedApps
        triggerIconUpdate()
        updateSuggestions()

        val seenApps = (prefs.getStringSet("seen_apps", null) ?: emptySet()).toMutableSet()
        val isMigration = prefs.getStringSet("seen_apps", null) == null
        val savedLayout = prefs.getString("launcher_layout_v3", null) ?: prefs.getString("launcher_layout_v2", null)

        if (savedLayout != null) {
            try {
                val pagesArray = JSONArray(savedLayout)
                val restoredPages = mutableListOf<List<AppModel>>()
                if (isMigration) {
                    seenApps.addAll(processedApps.map { it.uniqueId })
                    prefs.edit().putStringSet("seen_apps", seenApps).apply()
                }
                for (i in 0 until pagesArray.length()) {
                    val pageArray = pagesArray.getJSONArray(i)
                    val pageItems = mutableListOf<AppModel>()
                    for (j in 0 until pageArray.length()) {
                        val itemValue = pageArray.get(j)
                        val jsonStr = if (itemValue is JSONObject) itemValue.toString() else itemValue as String
                        ConfigSerializer.deserializeAppModel(jsonStr)?.let { savedApp ->
                            if (savedApp.isFolder || savedApp.isWidget || savedApp.isPWA) {
                                pageItems.add(savedApp)
                            } else if (savedApp.isShortcut && !savedApp.shortcutId.isNullOrEmpty()) {
                                // 處理 Shortcut 圖示快取
                                val currentStyle = currentStyleSuffix
                                val cacheKey = "${savedApp.uniqueId}_$currentStyle"
                                if (iconCache[cacheKey] == null) {
                                    viewModelScope.launch(Dispatchers.IO) {
                                        val info = getShortcutInfoById(savedApp.packageName, savedApp.shortcutId, savedApp.userId)
                                        info?.let { si ->
                                            getShortcutIcon(si)?.let { icon ->
                                                iconCache.put(cacheKey, icon)
                                                triggerIconUpdate()
                                            }
                                        }
                                    }
                                }
                                pageItems.add(savedApp)
                            } else {
                                val baseApp = processedApps.find { it.uniqueId == savedApp.uniqueId }
                                    ?: processedApps.find { it.packageName == savedApp.packageName && it.userId == savedApp.userId }
                                baseApp?.let {
                                    if (!it.isFrozen && !it.isPrivate) {
                                        pageItems.add(it.copy(uniqueId = savedApp.uniqueId, label = customLabels[savedApp.uniqueId] ?: customLabels[savedApp.packageName] ?: it.label, isHidden = hiddenPackages.contains(savedApp.uniqueId) || hiddenPackages.contains(savedApp.packageName)))
                                    }
                                }
                            }
                        }
                    }
                    restoredPages.add(pageItems)
                }
                val newApps = processedApps.filter { app -> !seenApps.contains(app.uniqueId) && !seenApps.contains(app.packageName) && !app.isHidden && !app.isFrozen && !app.isPrivate }
                if (newApps.isNotEmpty()) {
                    if (_autoAddAppsToHome.value) {
                        val isMassiveMigration = newApps.size > 20
                        if (!isMassiveMigration) {
                            val mutablePages = restoredPages.map { it.toMutableList() }.toMutableList()
                            newApps.forEach { app ->
                                var added = false
                                for (page in mutablePages) { if (page.size < pageSize) { page.add(app); added = true; break } }
                                if (!added) mutablePages.add(mutableListOf(app))
                                seenApps.add(app.uniqueId)
                            }
                            _pages.value = mutablePages
                            saveLayout()
                        } else { seenApps.addAll(newApps.map { it.uniqueId }) }
                        prefs.edit().putStringSet("seen_apps", seenApps).apply()
                    } else {
                        seenApps.addAll(newApps.map { it.uniqueId })
                        prefs.edit().putStringSet("seen_apps", seenApps).apply()
                        _pages.value = restoredPages
                    }
                } else { _pages.value = restoredPages }
            } catch (e: Exception) { repaginate(processedApps) }
        } else {
            repaginate(processedApps)
            seenApps.addAll(processedApps.map { it.uniqueId })
            prefs.edit().putStringSet("seen_apps", seenApps).apply()
            saveLayout()
        }
        if (_dockItems.value.isEmpty() || _dockItems.value.all { it.packageName.isEmpty() && !it.isFolder }) {
            if (_dockPackageNames.value.all { it.isEmpty() } && processedApps.isNotEmpty()) {
                val defaultDock = processedApps.take(4)
                _dockItems.value = defaultDock
                _dockPackageNames.value = defaultDock.map { it.packageName }
                saveDock()
            } else {
                // 從舊版 package names 構建
                val list = _dockPackageNames.value.map { pkg ->
                    processedApps.find { it.packageName == pkg } ?: AppModel(label = "", packageName = "", uniqueId = "empty_dock_${System.currentTimeMillis()}")
                }
                _dockItems.value = list
                saveDock()
            }
        } else {
            // 同步最新的 App 資訊 (Label 等)
            val updatedDock = _dockItems.value.map { dockApp ->
                if (dockApp.isFolder || dockApp.isPWA || dockApp.isShortcut) {
                    dockApp
                } else if (dockApp.packageName.isNotEmpty()) {
                    processedApps.find { it.packageName == dockApp.packageName && it.userId == dockApp.userId } ?: dockApp
                } else {
                    dockApp
                }
            }
            _dockItems.value = updatedDock
        }
    }
}

fun MainViewModel.repaginate(allApps: List<AppModel>) {
    if (allApps.isEmpty()) {
        _pages.value = listOf(emptyList())
    } else {
        val visibleApps = allApps.filter { !it.isHidden && !it.isFrozen && !it.isPrivate }
        _pages.value = visibleApps.chunked(pageSize)
    }
}

fun MainViewModel.launchApp(app: AppModel) {
    val launcherApps = getApplication<Application>().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val userManager = getApplication<Application>().getSystemService(Context.USER_SERVICE) as UserManager
    
    try {
        val allProfiles = userManager.userProfiles
        val userHandle = allProfiles.find { 
            userManager.getSerialNumberForUser(it) == app.userId 
        } ?: android.os.Process.myUserHandle()

        // 增加 PWA 啟動邏輯
        if (app.isPWA || app.packageName == "com.iteration.pwa" || app.uniqueId.startsWith("pwa_")) {
            val rawUrl = (app.shortcutId ?: "").trim()
            if (rawUrl.isEmpty()) {
                android.util.Log.e("Iteration", "PWA URL is blank for ${app.label}")
                return
            }
            
            try {
                // 檢查是否選擇使用 vNavi 且是否安裝
                val vNaviPackage = "com.liferlighdow.vnavi"
                val pm = getApplication<Application>().packageManager
                val isVNaviInstalled = try {
                    pm.getPackageInfo(vNaviPackage, 0)
                    true
                } catch (e: Exception) {
                    false
                }

                if (_useVNaviForPwa.value) {
                    if (isVNaviInstalled) {
                        val intent = Intent("com.liferlighdow.vnavi.action.RUN_PWA").apply {
                            setPackage(vNaviPackage)
                            putExtra("url", rawUrl)
                            putExtra("label", app.label)
                            putExtra("uniqueId", app.uniqueId)
                            putExtra("theme_color", app.pwaBgColor)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        getApplication<Application>().startActivity(intent)
                    } else {
                        // 雖然選擇了但沒裝，提示安裝，且不再自動降級使用內建，以符合使用者設定
                        _showVNaviInstallDialog.value = true
                    }
                } else {
                    // 用戶明確選擇使用內置
                    val intent = Intent(getApplication(), PwaActivity::class.java).apply {
                        putExtra("url", rawUrl)
                        putExtra("label", app.label)
                        putExtra("uniqueId", app.uniqueId)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    getApplication<Application>().startActivity(intent)
                }
                logAppLaunch(app.packageName)
            } catch (e: Exception) {
                android.util.Log.e("Iteration", "Failed to launch PWA", e)
                android.widget.Toast.makeText(getApplication(), "PWA Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
            return
        }

        // 增加 Shortcut 啟動邏輯
        if (app.isShortcut && !app.shortcutId.isNullOrEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                launcherApps.startShortcut(app.packageName, app.shortcutId, null, null, userHandle)
            }
            logAppLaunch(app.packageName)
            return
        }

        if (app.uniqueId.contains("/")) {
            val idWithoutTimestamp = if (app.uniqueId.contains("@")) {
                val lastPart = app.uniqueId.substringAfterLast("@")
                if (lastPart.length >= 10) app.uniqueId.substringBeforeLast("@") else app.uniqueId
            } else app.uniqueId
            
            val idWithoutUser = idWithoutTimestamp.substringBefore("@")
            val pkg = idWithoutUser.substringBefore("/")
            val cls = idWithoutUser.substringAfter("/")
            val component = android.content.ComponentName(pkg, cls)
            launcherApps.startMainActivity(component, userHandle, null, null)
        } else {
            val intent = getApplication<Application>().packageManager.getLaunchIntentForPackage(app.packageName)
            if (intent != null) {
                getApplication<Application>().startActivity(intent.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        }
        logAppLaunch(app.packageName)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun MainViewModel.getAppShortcuts(packageName: String, userId: Long): List<ShortcutInfo> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return emptyList()
    val launcherApps = getApplication<Application>().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val userManager = getApplication<Application>().getSystemService(Context.USER_SERVICE) as UserManager
    
    val allProfiles = userManager.userProfiles
    val userHandle = allProfiles.find { 
        userManager.getSerialNumberForUser(it) == userId 
    } ?: android.os.Process.myUserHandle()

    val query = LauncherApps.ShortcutQuery().apply {
        setPackage(packageName)
        setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or 
                     LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or 
                     LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
    }
    return try {
        launcherApps.getShortcuts(query, userHandle) ?: emptyList()
    } catch (e: SecurityException) {
        emptyList()
    }
}

fun MainViewModel.launchShortcut(packageName: String, shortcutId: String, userId: Long) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
    val launcherApps = getApplication<Application>().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val userManager = getApplication<Application>().getSystemService(Context.USER_SERVICE) as UserManager
    
    val allProfiles = userManager.userProfiles
    val userHandle = allProfiles.find { 
        userManager.getSerialNumberForUser(it) == userId 
    } ?: android.os.Process.myUserHandle()

    try {
        launcherApps.startShortcut(packageName, shortcutId, null, null, userHandle)
        logAppLaunch(packageName)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun MainViewModel.getShortcutInfoById(packageName: String, shortcutId: String, userId: Long): ShortcutInfo? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return null
    val launcherApps = getApplication<Application>().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val userManager = getApplication<Application>().getSystemService(Context.USER_SERVICE) as UserManager
    
    val allProfiles = userManager.userProfiles
    val userHandle = allProfiles.find { 
        userManager.getSerialNumberForUser(it) == userId 
    } ?: android.os.Process.myUserHandle()

    val query = LauncherApps.ShortcutQuery().apply {
        setPackage(packageName)
        setShortcutIds(listOf(shortcutId))
        setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or 
                     LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or 
                     LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
    }
    return try {
        launcherApps.getShortcuts(query, userHandle)?.firstOrNull()
    } catch (e: SecurityException) {
        null
    }
}

fun MainViewModel.getShortcutIcon(shortcut: ShortcutInfo): ImageBitmap? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return null
    val launcherApps = getApplication<Application>().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    return try {
        val density = getApplication<Application>().resources.displayMetrics.densityDpi
        launcherApps.getShortcutIconDrawable(shortcut, density)?.toBitmap()?.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}

/** PWA Maker 邏輯 **/

fun MainViewModel.loadPwaApps() {
    val saved = prefs.getString("pwa_apps_json", null)
    if (saved != null) {
        try {
            val array = JSONArray(saved)
            val list = mutableListOf<AppModel>()
            for (i in 0 until array.length()) {
                ConfigSerializer.deserializeAppModel(array.getString(i))?.let { list.add(it) }
            }
            _pwaApps.value = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun MainViewModel.savePwaApps() {
    val array = JSONArray()
    _pwaApps.value.forEach { array.put(ConfigSerializer.serializeAppModel(it)) }
    prefs.edit().putString("pwa_apps_json", array.toString()).apply()
}

fun MainViewModel.deletePWA(app: AppModel) {
    viewModelScope.launch {
        _removingItemIds.value += app.uniqueId
        delay(500)
        
        val current = _pwaApps.value.toMutableList()
        current.removeAll { it.uniqueId == app.uniqueId }
        _pwaApps.value = current
        savePwaApps()

        // 從主畫面移除
        val currentPages = _pages.value.map { page ->
            page.filter { it.uniqueId != app.uniqueId }
        }.filter { it.isNotEmpty() }
        _pages.value = currentPages
        saveLayout()
        
        // 清除圖示快取
        val fileSafeId = app.uniqueId.replace("/", "_").replace(":", "_").replace("@", "_")
        processedIconCacheDir.listFiles { _, name -> name.startsWith(fileSafeId) }?.forEach { it.delete() }
        
        loadApps()
        _removingItemIds.value -= app.uniqueId
    }
}

fun MainViewModel.createPWA(label: String, url: String, bgColor: Int) {
    val pwaApp = AppModel(
        label = label,
        packageName = "com.iteration.pwa",
        uniqueId = "pwa_${System.currentTimeMillis()}",
        shortcutId = url,
        isPWA = true,
        pwaBgColor = bgColor
    )
    
    // 保存到 PWA 列表
    val currentPwas = _pwaApps.value.toMutableList()
    currentPwas.add(pwaApp)
    _pwaApps.value = currentPwas
    savePwaApps()
    
    // 觸發應用程式清單更新，PWA 會因為 auto-add 邏輯自動出現在桌面
    loadApps()
    
    viewModelScope.launch(Dispatchers.IO) {
        loadPwaIcon(pwaApp)
    }
}

fun MainViewModel.updatePWA(uniqueId: String, newLabel: String, newUrl: String, newBgColor: Int) {
    val oldPwa = _pwaApps.value.find { it.uniqueId == uniqueId }
    val urlChanged = oldPwa?.shortcutId != newUrl
    
    val currentPwas = _pwaApps.value.map {
        if (it.uniqueId == uniqueId) {
            it.copy(label = newLabel, shortcutId = newUrl, pwaBgColor = newBgColor)
        } else it
    }
    _pwaApps.value = currentPwas
    savePwaApps()
    
    // 同步更新已在桌面上的項目 (包含資料夾內部)
    _pages.value = _pages.value.map { page ->
        page.map { app ->
            if (app.uniqueId == uniqueId || app.uniqueId.startsWith("$uniqueId@")) {
                app.copy(label = newLabel, shortcutId = newUrl, pwaBgColor = newBgColor)
            } else if (app.isFolder) {
                app.copy(folderItems = app.folderItems.map { fApp ->
                    if (fApp.uniqueId == uniqueId || fApp.uniqueId.startsWith("$uniqueId@")) {
                        fApp.copy(label = newLabel, shortcutId = newUrl, pwaBgColor = newBgColor)
                    } else fApp
                })
            } else app
        }
    }
    saveLayout()
    loadApps()
    
    // 如果 URL 改變，需要重新載入圖示 (Favicon 可能已變)
    if (urlChanged) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedApp = _pwaApps.value.find { it.uniqueId == uniqueId }
            if (updatedApp != null) loadPwaIcon(updatedApp)
        }
    }
}

suspend fun MainViewModel.loadPwaIcon(app: AppModel) {
    val styleSuffix = currentStyleSuffix
    val fileSafeId = app.uniqueId.replace("/", "_").replace(":", "_").replace("@", "_")
    val diskCacheFile = File(processedIconCacheDir, "${fileSafeId}_$styleSuffix.png")
    val cacheKey = "${app.uniqueId}_$styleSuffix"
    
    val density = getApplication<Application>().resources.displayMetrics.density
    val userIconSize = _iconSizePx.value
    
    // PWA 圖示生成同步階梯對齊邏輯
    val renderingSizePx = if (userIconSize > 0) {
        userIconSize 
    } else {
        (Math.round(62 * density / 16f) * 16).toInt().coerceIn(32, 256)
    }
    
    val bitmap = generatePwaIcon(app, renderingSizePx)
    if (bitmap != null) {
        val imageBitmap = bitmap.asImageBitmap()
        saveIconToDisk(imageBitmap, diskCacheFile)
        iconCache.put(cacheKey, imageBitmap)
        triggerIconUpdate()
    }
}

private suspend fun MainViewModel.generatePwaIcon(app: AppModel, sizePx: Int): Bitmap? {
    val url = app.shortcutId ?: return null
    val domain = try { android.net.Uri.parse(url).host ?: url } catch (e: Exception) { url }
    val faviconUrl = "https://www.google.com/s2/favicons?sz=128&domain=$domain"
    
    val loader = ImageLoader(getApplication())
    val request = ImageRequest.Builder(getApplication())
        .data(faviconUrl)
        .allowHardware(false)
        .build()
        
    val result = (loader.execute(request) as? SuccessResult)?.drawable?.toBitmap(sizePx, sizePx)
    
    val finalBitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(finalBitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    // 繪製背景
    val shape = _iconShape.value
    val mask = iconProcessor.getOrCreateMask(shape, sizePx)
    paint.color = app.pwaBgColor
    canvas.drawBitmap(mask, 0f, 0f, paint)
    
    // 繪製前景 (Favicon)
    if (result != null) {
        val padding = sizePx * 0.2f
        val srcRect = Rect(0, 0, result.width, result.height)
        val dstRect = RectF(padding, padding, sizePx - padding, sizePx - padding)
        canvas.drawBitmap(result, srcRect, dstRect, paint)
    } else {
        // Fallback: 畫一個字母
        paint.color = android.graphics.Color.WHITE
        paint.textSize = sizePx * 0.5f
        paint.textAlign = Paint.Align.CENTER
        val fontMetrics = paint.fontMetrics
        val y = (sizePx - fontMetrics.ascent - fontMetrics.descent) / 2
        canvas.drawText(app.label.take(1).uppercase(), sizePx / 2f, y, paint)
    }
    
    return finalBitmap
}
