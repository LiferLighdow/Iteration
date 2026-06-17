package com.liferlighdow.iteration

import android.app.Application
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.AdaptiveIconDrawable
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import java.io.File
import java.io.FileOutputStream
import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)
    private val prefs = application.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
    private val iconProcessor = IconProcessor(application)
    private val iconPackManager = IconPackManager(application)
    private val wallpaperProcessor = WallpaperProcessor(application)
    private val wallpaperFile = File(application.filesDir, "launcher_wallpaper.png")

    private val _blurredWallpaper = MutableStateFlow<ImageBitmap?>(null)
    val blurredWallpaper = _blurredWallpaper.asStateFlow()

    private val _rawWallpaper = MutableStateFlow<ImageBitmap?>(null)
    val rawWallpaper = _rawWallpaper.asStateFlow()

    // 用於強制更新 UI 的訊號
    private val _wallpaperUpdateSignal = MutableStateFlow(0L)
    val wallpaperUpdateSignal = _wallpaperUpdateSignal.asStateFlow()

    internal val _pages = MutableStateFlow<List<List<AppModel>>>(emptyList())
    val pages: StateFlow<List<List<AppModel>>> = _pages

    internal val _allApps = MutableStateFlow<List<AppModel>>(emptyList())
    val allApps: StateFlow<List<AppModel>> = _allApps

    private val _dockPackageNames = MutableStateFlow(listOf<String>())
    val dockPackageNames = _dockPackageNames.asStateFlow()

    private val _minusOneWidgets = MutableStateFlow<List<WidgetModel>>(emptyList())
    val minusOneWidgets: StateFlow<List<WidgetModel>> = _minusOneWidgets.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode = _isEditMode.asStateFlow()

    private val _isThemedIconsEnabled = MutableStateFlow(prefs.getBoolean("themed_icons", false))
    val isThemedIconsEnabled = _isThemedIconsEnabled.asStateFlow()

    private val _isLiquidGlassEnabled = MutableStateFlow(prefs.getBoolean("liquid_glass_enabled", false))
    val isLiquidGlassEnabled = _isLiquidGlassEnabled.asStateFlow()

    private val _isLiquidGlassDockEnabled = MutableStateFlow(prefs.getBoolean("liquid_glass_dock", false))
    val isLiquidGlassDockEnabled = _isLiquidGlassDockEnabled.asStateFlow()

    private val _isLiquidGlassHomeFolderEnabled = MutableStateFlow(prefs.getBoolean("liquid_glass_home_folder", false))
    val isLiquidGlassHomeFolderEnabled = _isLiquidGlassHomeFolderEnabled.asStateFlow()

    private val _isLiquidGlassAppLibraryFolderEnabled = MutableStateFlow(prefs.getBoolean("liquid_glass_app_library_folder", false))
    val isLiquidGlassAppLibraryFolderEnabled = _isLiquidGlassAppLibraryFolderEnabled.asStateFlow()

    private val _isLiquidGlassGlobalSearchEnabled = MutableStateFlow(prefs.getBoolean("liquid_glass_global_search", false))
    val isLiquidGlassGlobalSearchEnabled = _isLiquidGlassGlobalSearchEnabled.asStateFlow()

    private val _isLiquidGlassAppLibrarySearchEnabled = MutableStateFlow(prefs.getBoolean("liquid_glass_app_library_search", false))
    val isLiquidGlassAppLibrarySearchEnabled = _isLiquidGlassAppLibrarySearchEnabled.asStateFlow()

    private val _isLiquidGlassWidgetsEnabled = MutableStateFlow(prefs.getBoolean("liquid_glass_widgets", false))
    val isLiquidGlassWidgetsEnabled = _isLiquidGlassWidgetsEnabled.asStateFlow()

    private val _showMinusOnePage = MutableStateFlow(prefs.getBoolean("show_minus_one", true))
    val showMinusOnePage = _showMinusOnePage.asStateFlow()

    private val _showAppLibrary = MutableStateFlow(prefs.getBoolean("show_app_library", true))
    val showAppLibrary = _showAppLibrary.asStateFlow()

    private val _liquidGlassBlur = MutableStateFlow(prefs.getFloat("liquid_glass_blur", 0f))
    val liquidGlassBlur = _liquidGlassBlur.asStateFlow()

    private val _liquidGlassRefractionHeight = MutableStateFlow(prefs.getFloat("liquid_glass_refraction_height", 24f))
    val liquidGlassRefractionHeight = _liquidGlassRefractionHeight.asStateFlow()

    private val _liquidGlassRefractionAmount = MutableStateFlow(prefs.getFloat("liquid_glass_refraction_amount", 48f))
    val liquidGlassRefractionAmount = _liquidGlassRefractionAmount.asStateFlow()

    private val _liquidGlassChromaticAberration = MutableStateFlow(prefs.getBoolean("liquid_glass_chromatic_aberration", true))
    val liquidGlassChromaticAberration = _liquidGlassChromaticAberration.asStateFlow()

    private val _homeMenuOptions = MutableStateFlow(prefs.getStringSet("home_menu_options", setOf("delete_home", "uninstall")) ?: setOf("delete_home", "uninstall"))
    val homeMenuOptions = _homeMenuOptions.asStateFlow()

    private val _favoritePackages = MutableStateFlow(prefs.getStringSet("favorite_packages", emptySet()) ?: emptySet())
    val favoritePackages = _favoritePackages.asStateFlow()

    private val _doubleTapAction = MutableStateFlow(
        try {
            GestureAction.valueOf(prefs.getString("double_tap_action", "NONE") ?: "NONE")
        } catch (e: Exception) {
            GestureAction.NONE
        }
    )
    val doubleTapAction = _doubleTapAction.asStateFlow()

    private val _swipeUpAction = MutableStateFlow(
        try {
            GestureAction.valueOf(prefs.getString("swipe_up_action", "NONE") ?: "NONE")
        } catch (e: Exception) {
            GestureAction.NONE
        }
    )
    val swipeUpAction = _swipeUpAction.asStateFlow()

    private val _doubleTapApp = MutableStateFlow(prefs.getString("double_tap_app", "") ?: "")
    val doubleTapApp = _doubleTapApp.asStateFlow()

    private val _swipeUpApp = MutableStateFlow(prefs.getString("swipe_up_app", "") ?: "")
    val swipeUpApp = _swipeUpApp.asStateFlow()

    private val _swipeDownAction = MutableStateFlow(
        try {
            GestureAction.valueOf(prefs.getString("swipe_down_action", "OPEN_GLOBAL_SEARCH") ?: "OPEN_GLOBAL_SEARCH")
        } catch (e: Exception) {
            GestureAction.OPEN_GLOBAL_SEARCH
        }
    )
    val swipeDownAction = _swipeDownAction.asStateFlow()

    private val _longPressAction = MutableStateFlow(
        try {
            GestureAction.valueOf(prefs.getString("long_press_action", "OPEN_DESKTOP_MENU") ?: "OPEN_DESKTOP_MENU")
        } catch (e: Exception) {
            GestureAction.OPEN_DESKTOP_MENU
        }
    )
    val longPressAction = _longPressAction.asStateFlow()

    private val _swipeDownApp = MutableStateFlow(prefs.getString("swipe_down_app", "") ?: "")
    val swipeDownApp = _swipeDownApp.asStateFlow()

    private val _longPressApp = MutableStateFlow(prefs.getString("long_press_app", "") ?: "")
    val longPressApp = _longPressApp.asStateFlow()

    private val _twoFingerSwipeUpAction = MutableStateFlow(
        try {
            GestureAction.valueOf(prefs.getString("two_finger_swipe_up_action", "NONE") ?: "NONE")
        } catch (e: Exception) {
            GestureAction.NONE
        }
    )
    val twoFingerSwipeUpAction = _twoFingerSwipeUpAction.asStateFlow()

    private val _twoFingerSwipeDownAction = MutableStateFlow(
        try {
            GestureAction.valueOf(prefs.getString("two_finger_swipe_down_action", "OPEN_NOTIFICATIONS") ?: "OPEN_NOTIFICATIONS")
        } catch (e: Exception) {
            GestureAction.OPEN_NOTIFICATIONS
        }
    )
    val twoFingerSwipeDownAction = _twoFingerSwipeDownAction.asStateFlow()

    private val _twoFingerSwipeUpApp = MutableStateFlow(prefs.getString("two_finger_swipe_up_app", "") ?: "")
    val twoFingerSwipeUpApp = _twoFingerSwipeUpApp.asStateFlow()

    private val _twoFingerSwipeDownApp = MutableStateFlow(prefs.getString("two_finger_swipe_down_app", "") ?: "")
    val twoFingerSwipeDownApp = _twoFingerSwipeDownApp.asStateFlow()

    private val _iconPackPackage = MutableStateFlow(prefs.getString("icon_pack_package", "") ?: "")
    val iconPackPackage = _iconPackPackage.asStateFlow()

    private val _iconStyle = MutableStateFlow(
        try { IconStyle.valueOf(prefs.getString("icon_style", "STANDARD") ?: "STANDARD") }
        catch (e: Exception) { IconStyle.STANDARD }
    )
    val iconStyle = _iconStyle.asStateFlow()

    private val _customIconBgColor = MutableStateFlow(prefs.getInt("custom_icon_bg_color", 0xFF2196F3.toInt()))
    val customIconBgColor = _customIconBgColor.asStateFlow()

    private val _customIconFgColor = MutableStateFlow(prefs.getInt("custom_icon_fg_color", 0xFFFFFFFF.toInt()))
    val customIconFgColor = _customIconFgColor.asStateFlow()

    private val _customIconUseOriginal = MutableStateFlow(prefs.getBoolean("custom_icon_use_original", false))
    val customIconUseOriginal = _customIconUseOriginal.asStateFlow()

    private val _customIconUseOriginalBg = MutableStateFlow(prefs.getBoolean("custom_icon_use_original_bg", false))
    val customIconUseOriginalBg = _customIconUseOriginalBg.asStateFlow()

    private val _iconShape = MutableStateFlow(
        try { IconShape.valueOf(prefs.getString("icon_shape", "DEFAULT") ?: "DEFAULT") }
        catch (e: Exception) { IconShape.DEFAULT }
    )
    val iconShape = _iconShape.asStateFlow()

    private val _desktopRows = MutableStateFlow(prefs.getInt("desktop_rows", 0)) // 0 means auto
    val desktopRows = _desktopRows.asStateFlow()

    private val _dockStyle = MutableStateFlow(
        try { DockStyle.valueOf(prefs.getString("dock_style", "MODERN") ?: "MODERN") }
        catch (e: Exception) { DockStyle.MODERN }
    )
    val dockStyle = _dockStyle.asStateFlow()

    private val _excludedThemedPackages = MutableStateFlow(prefs.getStringSet("excluded_themed_packages", emptySet()) ?: emptySet())
    val excludedThemedPackages = _excludedThemedPackages.asStateFlow()

    private val _libraryShape = MutableStateFlow(
        try { IconShape.valueOf(prefs.getString("library_shape", "DEFAULT") ?: "DEFAULT") }
        catch (e: Exception) { IconShape.DEFAULT }
    )
    val libraryShape = _libraryShape.asStateFlow()

    private val hiddenPackages = mutableSetOf<String>()
    private val customLabels = mutableMapOf<String, String>()
    private val customCategories = mutableMapOf<String, String>()
    private val categoryRenames = mutableMapOf<String, String>()
    private val _userCategories = MutableStateFlow<List<String>>(emptyList())
    val userCategories: StateFlow<List<String>> = _userCategories.asStateFlow()

    // --- 新增：響應式 UI 狀態 ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isLibrarySearchFocused = MutableStateFlow(false)
    val isLibrarySearchFocused = _isLibrarySearchFocused.asStateFlow()

    fun setLibrarySearchFocused(focused: Boolean) {
        _isLibrarySearchFocused.value = focused
    }

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory = _selectedCategory.asStateFlow()

    /**
     * 自動計算 App Library 的顯示清單 (無損優化)
     */
    val filteredLibraryApps = combine(_allApps, _searchQuery, _selectedCategory) { apps, query, category ->
        apps.asSequence()
            .filter { app ->
                val matchesQuery = app.label.contains(query, ignoreCase = true)
                val matchesCategory = when (category) {
                    "All" -> true
                    "Hidden Apps" -> app.isHidden
                    else -> app.displayCategory == category
                }
                matchesQuery && matchesCategory
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSelectedCategory(category: String) { _selectedCategory.value = category }
    // --------------------------

    private val _suggestedApps = MutableStateFlow<List<AppModel>>(emptyList())
    val suggestedApps = _suggestedApps.asStateFlow()

    private val customIconDir = File(application.filesDir, "custom_icons").apply { mkdirs() }
    private val processedIconCacheDir = File(application.cacheDir, "processed_icons").apply { mkdirs() }
    internal var pageSize = 20
    internal var dragBackupPages: List<List<AppModel>>? = null

    init {
        loadSettings()
        loadApps()
        updateBlurredWallpaper()
    }

    fun updateBlurredWallpaper() {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. 優先從本地儲存載入 (使用者自選)
            var result = wallpaperProcessor.loadWallpaperFromFile(wallpaperFile)

            // 2. 如果沒有自選，則嘗試從系統獲取 (降級方案)
            if (result == null) {
                result = wallpaperProcessor.extractSystemWallpaper()
            }

            if (result != null) {
                _rawWallpaper.value = result.raw
                _blurredWallpaper.value = result.blurred
            }
        }
    }

    fun setCustomWallpaper(bitmap: Bitmap, syncToSystem: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (syncToSystem) {
                    val wm = WallpaperManager.getInstance(getApplication())
                    wm.setBitmap(bitmap)
                }
                saveWallpaperToLocal(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveWallpaperToLocal(bitmap: Bitmap) {
        try {
            FileOutputStream(wallpaperFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            // 發出更新訊號
            _wallpaperUpdateSignal.value = System.currentTimeMillis()
            updateBlurredWallpaper()
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onCleared() {
        super.onCleared()
    }

    private fun loadSettings() {
        loadHiddenPackages()
        loadCustomLabels()
        loadUserCategories()
        loadCustomCategories()
        loadCategoryRenames()
        loadWidgets()
        loadDock()

        // 重新讀取所有可能在 SettingsActivity 中改變的設定
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
        val newDockStyle = try { DockStyle.valueOf(prefs.getString("dock_style", "MODERN") ?: "MODERN") }
        catch (e: Exception) { DockStyle.MODERN }

        if (_iconStyle.value != newStyle || _isThemedIconsEnabled.value != newThemed || _iconPackPackage.value != newIconPackPackage || _iconShape.value != newShape || _libraryShape.value != newLibShape || _excludedThemedPackages.value != newExcluded || _desktopRows.value != newRows || _dockStyle.value != newDockStyle) {
            _iconStyle.value = newStyle
            _iconShape.value = newShape
            _libraryShape.value = newLibShape
            _isThemedIconsEnabled.value = newThemed
            _iconPackPackage.value = newIconPackPackage
            _excludedThemedPackages.value = newExcluded
            _desktopRows.value = newRows
            _dockStyle.value = newDockStyle
            iconCache.evictAll()
        }

        _isLiquidGlassEnabled.value = newLiquidEnabled
        _isLiquidGlassDockEnabled.value = newLiquidDockEnabled
        _isLiquidGlassHomeFolderEnabled.value = newLiquidHomeFolderEnabled
        _isLiquidGlassAppLibraryFolderEnabled.value = newLiquidAppLibraryFolderEnabled
        _isLiquidGlassGlobalSearchEnabled.value = newLiquidGlobalSearchEnabled
        _isLiquidGlassAppLibrarySearchEnabled.value = newLiquidAppLibrarySearchEnabled
        _isLiquidGlassWidgetsEnabled.value = newLiquidWidgetsEnabled
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

        // 重新讀取主畫面選單與收藏設定
        _homeMenuOptions.value = prefs.getStringSet("home_menu_options", setOf("delete_home", "uninstall")) ?: setOf("delete_home", "uninstall")
        _favoritePackages.value = prefs.getStringSet("favorite_packages", emptySet()) ?: emptySet()
    }

    internal fun saveLayout() {
        val pagesArray = JSONArray()
        _pages.value.forEach { page ->
            val pageArray = JSONArray()
            page.forEach { item ->
                pageArray.put(ConfigSerializer.serializeAppModel(item))
            }
            pagesArray.put(pageArray)
        }
        prefs.edit().putString("launcher_layout_v2", pagesArray.toString()).apply()
    }

    private fun loadWidgets() {
        val saved = prefs.getString("minus_one_widgets", null)
        if (saved != null) {
            val list = mutableListOf<WidgetModel>()
            val array = JSONArray(saved)
            for (i in 0 until array.length()) {
                ConfigSerializer.deserializeWidgetModel(array.getJSONObject(i))?.let { list.add(it) }
            }
            _minusOneWidgets.value = list
        }
    }

    fun addWidget(type: WidgetType, pageIndex: Int = -1) {
        val label = when (type) {
            is WidgetType.Battery -> "Battery"
            is WidgetType.Clock -> "Clock"
            is WidgetType.Calendar -> "Calendar"
            is WidgetType.Photo -> "Photo"
            is WidgetType.Music -> "Music"
            is WidgetType.Note -> "Note"
            is WidgetType.Stack -> if (type.isWide) "Wide Widget Stacker" else "Stack"
        }

        if (pageIndex == -1) {
            // 加入到負一屏
            val newList = _minusOneWidgets.value + WidgetModel(type = type, label = label)
            _minusOneWidgets.value = newList
            saveWidgets(newList)
        } else {
            // 加入到指定桌面分頁
            val widget = WidgetModel(type = type, label = label)
            val appModel = AppModel(
                label = label,
                uniqueId = "widget_${widget.id}",
                widget = widget
            )
            val currentPages = _pages.value.map { it.toMutableList() }.toMutableList()
            insertAtPage(currentPages, pageIndex, appModel)
            reorganizeAllPages(currentPages)
        }
    }

    fun removeWidget(id: String) {
        val newList = _minusOneWidgets.value.filter { it.id != id }
        _minusOneWidgets.value = newList
        saveWidgets(newList)
    }

    fun updateWidgetDisplayMode(id: String, mode: WidgetDisplayMode) {
        // 1. 更新負一屏的小工具
        val newList = _minusOneWidgets.value.map {
            if (it.id == id) it.copy(displayMode = mode) else it
        }
        _minusOneWidgets.value = newList
        saveWidgets(newList)

        // 2. 更新桌面分頁上的小工具
        val currentPages = _pages.value.map { page ->
            page.map { item ->
                if (item.widget?.id == id) {
                    item.copy(widget = item.widget.copy(displayMode = mode))
                } else {
                    item
                }
            }
        }
        _pages.value = currentPages
    }

    private fun saveWidgets(list: List<WidgetModel>) {
        val array = JSONArray()
        list.forEach { widget ->
            array.put(ConfigSerializer.serializeWidgetModel(widget))
        }
        prefs.edit().putString("minus_one_widgets", array.toString()).apply()
    }

    // 佈局處理邏輯已移至 MainViewModelLayout.kt

    private fun loadHiddenPackages() {
        val saved = prefs.getStringSet("hidden_apps", emptySet()) ?: emptySet()
        hiddenPackages.clear()
        hiddenPackages.addAll(saved)
    }

    private fun loadCustomLabels() {
        val saved = prefs.getString("custom_labels_json", null)
        customLabels.clear()
        if (saved != null) {
            val json = JSONObject(saved)
            json.keys().forEach { key ->
                customLabels[key] = json.getString(key)
            }
        }
    }

    private fun loadUserCategories() {
        val saved = prefs.getString("user_categories_ordered", null)
        _userCategories.value = saved?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    private fun loadCategoryRenames() {
        val saved = prefs.getString("category_renames_json", null)
        categoryRenames.clear()
        if (saved != null) {
            val json = JSONObject(saved)
            json.keys().forEach { key ->
                categoryRenames[key] = json.getString(key)
            }
        }
    }

    fun renameCategory(oldName: String, newName: String) {
        if (oldName == newName || newName.isBlank()) return

        // 1. 更新重命名映射
        categoryRenames[oldName] = newName
        saveCategoryRenames()

        // 2. 更新排序清單中的名稱
        val current = _userCategories.value.toMutableList()
        val index = current.indexOf(oldName)
        if (index != -1) {
            current[index] = newName
            _userCategories.value = current
            saveUserCategories(current)
        }

        // 3. 更新所有使用了舊名稱的 customCategories
        val appsToUpdate = customCategories.filter { it.value == oldName }.keys
        appsToUpdate.forEach { customCategories[it] = newName }
        if (appsToUpdate.isNotEmpty()) saveCustomCategories()

        loadApps() // 重新載入以套用顯示名稱
    }

    private fun saveCategoryRenames() {
        val json = JSONObject()
        categoryRenames.forEach { (k, v) -> json.put(k, v) }
        prefs.edit().putString("category_renames_json", json.toString()).apply()
    }

    private fun loadCustomCategories() {
        val saved = prefs.getString("custom_categories_json", null)
        customCategories.clear()
        if (saved != null) {
            val json = JSONObject(saved)
            json.keys().forEach { key ->
                customCategories[key] = json.getString(key)
            }
        }
    }

    fun addUserCategory(name: String) {
        val current = _userCategories.value.toMutableList()
        if (!current.contains(name)) {
            current.add(name)
            _userCategories.value = current
            saveUserCategories(current)
        }
    }

    fun deleteUserCategory(name: String) {
        val current = _userCategories.value.toMutableList()
        current.remove(name)
        _userCategories.value = current
        saveUserCategories(current)

        // 清除該分類下的 App 設定
        val toRemove = customCategories.filter { it.value == name }.keys
        toRemove.forEach { customCategories.remove(it) }
        saveCustomCategories()
        loadApps()
    }

    fun moveUserCategory(fromIndex: Int, toIndex: Int) {
        val current = _userCategories.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            _userCategories.value = current
            saveUserCategories(current)
        }
    }

    private fun saveUserCategories(list: List<String>) {
        prefs.edit().putString("user_categories_ordered", list.joinToString(",")).apply()
    }

    fun setAppCategory(packageName: String, category: String) {
        if (category.isBlank()) {
            customCategories.remove(packageName)
        } else {
            customCategories[packageName] = category
        }
        saveCustomCategories()
        loadApps()
    }

    private fun saveCustomCategories() {
        val json = JSONObject()
        customCategories.forEach { (k, v) -> json.put(k, v) }
        prefs.edit().putString("custom_categories_json", json.toString()).apply()
    }

    fun setCustomLabel(packageName: String, newLabel: String) {
        if (newLabel.isBlank()) {
            customLabels.remove(packageName)
        } else {
            customLabels[packageName] = newLabel
        }
        val json = JSONObject()
        customLabels.forEach { (k, v) -> json.put(k, v) }
        prefs.edit().putString("custom_labels_json", json.toString()).apply()
        loadApps()
    }

    fun setCustomIcon(packageName: String, bitmap: Bitmap) {
        val file = File(customIconDir, "$packageName.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        iconCache.remove(packageName) // 清除快取以強制重讀
        loadApps()
    }

    fun resetCustomIcon(packageName: String) {
        val file = File(customIconDir, "$packageName.png")
        if (file.exists()) file.delete()
        iconCache.remove(packageName) // 清除快取
        loadApps()
    }

    fun toggleHiddenApp(packageName: String) {
        if (hiddenPackages.contains(packageName)) {
            hiddenPackages.remove(packageName)
        } else {
            hiddenPackages.add(packageName)
            // 如果 App 被隱藏，確保它從桌面佈局中移除 (預留)
        }
        prefs.edit().putStringSet("hidden_apps", hiddenPackages).apply()

        // 如果 App 被隱藏，從桌面佈局中移除 (已由 repaginate 處理)
        // 但如果是在 Dock 中，UI 會根據 isHidden 自動隱藏

        loadApps() // Reload to update isHidden status
    }

    fun getPassword(): String = prefs.getString("hidden_password", "") ?: ""

    fun setPassword(password: String) {
        prefs.edit().putString("hidden_password", password).apply()
    }

    fun setPageSize(size: Int) {
        if (pageSize != size) {
            pageSize = size
            repaginate(_pages.value.flatten())
        }
    }

    private fun repaginate(allApps: List<AppModel>) {
        if (allApps.isEmpty()) {
            _pages.value = listOf(emptyList())
        } else {
            // 過濾掉隱藏的 App 後再進行分頁
            val visibleApps = allApps.filter { !it.isHidden }
            _pages.value = visibleApps.chunked(pageSize)
        }
    }

    private val iconCache = object : LruCache<String, ImageBitmap>(250) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = 1
    }

    private var loadAppsJob: kotlinx.coroutines.Job? = null
    private var cachedRawApps: List<AppModel>? = null

    private fun loadDock() {
        val saved = prefs.getString("dock_packages", null)
        val list = if (saved != null) {
            val decoded = saved.split(",").toMutableList()
            // 確保始終有 4 個位置
            while (decoded.size < 4) decoded.add("")
            decoded.take(4)
        } else {
            List(4) { "" }
        }
        _dockPackageNames.value = list
    }

    fun loadApps() {
        loadAppsJob?.cancel()

        // 1. 強制重新載入設定
        loadSettings()
        updateSuggestions()
        updateBlurredWallpaper() // 更新模糊桌布

        val isThemed = _isThemedIconsEnabled.value
        val currentStyle = _iconStyle.value
        val currentShape = _iconShape.value
        val currentIconPack = _iconPackPackage.value

        loadAppsJob = viewModelScope.launch {
            // 如果是 Custom 風格且正在頻繁調整，稍微延遲一下以避免過度運算
            if (currentStyle == IconStyle.CUSTOM) {
                kotlinx.coroutines.delay(100)
            }
            if (currentIconPack.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    iconPackManager.loadIconPack(currentIconPack)
                }
            }

            val rawApps = cachedRawApps ?: withContext(Dispatchers.IO) {
                repository.getInstalledApps().also { cachedRawApps = it }
            }
            
            // 2. 獲取主題色
            val themeColors = if (isThemed) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    dynamicLightColorScheme(getApplication())
                } else {
                    val seed = withContext(Dispatchers.IO) {
                        try {
                            val wm = WallpaperManager.getInstance(getApplication())
                            wm.drawable?.toBitmap()?.let { DynamicColorGenerator.extractSeedColorFromBitmap(it) }
                        } catch (e: Exception) { null }
                    }
                    seed?.let { DynamicColorGenerator.generateColorSchemeFromSeed(it, false) }
                }
            } else null

            val processedApps = withContext(Dispatchers.Default) {
                val density = getApplication<Application>().resources.displayMetrics.density
                val sizePx = (62 * density).toInt()

                // 獲取目前的顏色特徵值，用於區分快取 (使用 ARGB 確保唯一性)
                val colorKey = themeColors?.primary?.let {
                    val argb = ((it.alpha * 255).toInt() shl 24) or
                               ((it.red * 255).toInt() shl 16) or
                               ((it.green * 255).toInt() shl 8) or
                               (it.blue * 255).toInt()
                    argb.toString(16)
                } ?: "default"

                coroutineScope {
                    rawApps.map { app ->
                        async {
                            val customIconFile = File(customIconDir, "${app.packageName}.png")

                            // 關鍵：快取檔名現在包含顏色數值，桌布一換，快取即失效
                            // V8: 加入自定義顏色支援
                            val excludedKey = if (excludedThemedPackages.value.contains(app.packageName)) "EX" else "IN"
                            val customBg = _customIconBgColor.value
                            val customFg = _customIconFgColor.value
                            val customOriginal = _customIconUseOriginal.value
                            val customOriginalBg = _customIconUseOriginalBg.value
                            val customKey = if (currentStyle == IconStyle.CUSTOM) {
                                "C_${customBg.toString(16)}_${customFg.toString(16)}_${if (customOriginal) "O" else "M"}_${if (customOriginalBg) "OB" else "CB"}"
                            } else "N"

                            val styleSuffix = if (currentIconPack.isNotEmpty()) {
                                "IP_V10_${currentIconPack.hashCode()}_${currentShape.name}_${currentStyle.name}_${if (isThemed) "T_$colorKey" else "N"}_${excludedKey}_$customKey"
                            } else {
                                "V9_${currentStyle.name}_${currentShape.name}_${if (isThemed) "T_$colorKey" else "N"}_${excludedKey}_$customKey"
                            }

                            val diskCacheFile = File(processedIconCacheDir, "${app.packageName}_$styleSuffix.png")

                            val cacheKey = "${app.packageName}_$styleSuffix"
                            val cachedIcon = iconCache[cacheKey]
                            val isExcluded = excludedThemedPackages.value.contains(app.packageName)

                            val processedIcon: ImageBitmap = if (cachedIcon != null) {
                                cachedIcon
                            } else if (customIconFile.exists()) {
                                BitmapFactory.decodeFile(customIconFile.absolutePath)?.asImageBitmap()
                                    ?: processNewIcon(app, currentIconPack, isThemed, isExcluded, themeColors, currentStyle, currentShape, sizePx, customBg, customFg, customOriginal, customOriginalBg)
                            } else if (diskCacheFile.exists()) {
                                val bitmap = BitmapFactory.decodeFile(diskCacheFile.absolutePath)
                                if (bitmap != null) {
                                    val imageBitmap = bitmap.asImageBitmap()
                                    iconCache.put(cacheKey, imageBitmap)
                                    imageBitmap
                                } else {
                                    val processed = processNewIcon(app, currentIconPack, isThemed, isExcluded, themeColors, currentStyle, currentShape, sizePx, customBg, customFg, customOriginal, customOriginalBg)
                                    saveIconToDisk(processed, diskCacheFile)
                                    iconCache.put(cacheKey, processed)
                                    processed
                                }
                            } else {
                                val processed = processNewIcon(app, currentIconPack, isThemed, isExcluded, themeColors, currentStyle, currentShape, sizePx, customBg, customFg, customOriginal, customOriginalBg)
                                saveIconToDisk(processed, diskCacheFile)
                                iconCache.put(cacheKey, processed)
                                processed
                            }

                            val appRes = getApplication<Application>()
                            val rawCategory = customCategories[app.packageName] ?: when (app.category) {
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

                            // 套用全域重命名映射
                            val displayCategory = categoryRenames[rawCategory] ?: rawCategory

                            app.copy(
                                label = customLabels[app.packageName] ?: app.label,
                                processedIcon = processedIcon,
                                isHidden = hiddenPackages.contains(app.packageName),
                                displayCategory = displayCategory
                            )
                        }
                    }.awaitAll()
                }
            }

            _allApps.value = processedApps
            // ... 後續佈局恢復邏輯保持不變
            
            // 優先從儲存的佈局恢復
            val savedLayout = prefs.getString("launcher_layout_v2", null)
            if (savedLayout != null) {
                try {
                    val pagesArray = JSONArray(savedLayout)
                    val restoredPages = mutableListOf<List<AppModel>>()
                    for (i in 0 until pagesArray.length()) {
                        val pageArray = pagesArray.getJSONArray(i)
                        val pageItems = mutableListOf<AppModel>()
                        for (j in 0 until pageArray.length()) {
                            ConfigSerializer.deserializeAppModel(
                                pageArray.getJSONObject(j),
                                processedApps,
                                customLabels,
                                hiddenPackages
                            )?.let { pageItems.add(it) }
                        }
                        restoredPages.add(pageItems)
                    }
                    _pages.value = restoredPages
                } catch (e: Exception) {
                    repaginate(processedApps)
                }
            } else {
                repaginate(processedApps)
            }

            if (_dockPackageNames.value.all { it.isEmpty() } && processedApps.isNotEmpty()) {
                val defaultDock = processedApps.take(4).map { it.packageName }
                _dockPackageNames.value = defaultDock
                prefs.edit().putString("dock_packages", defaultDock.joinToString(",")).apply()
            }
        }
    }

    private fun processNewIcon(
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
        customOriginalBg: Boolean
    ): ImageBitmap {
        return if (currentIconPack.isNotEmpty()) {
            val ipIcon = iconPackManager.getIcon(app.packageName)
            if (ipIcon != null) {
                iconProcessor.processIcon(ipIcon, false, null, IconStyle.STANDARD, currentShape, sizePx, isIconPack = true)
            } else {
                iconProcessor.processIcon(app.icon, isThemed && !isExcluded, themeColors, if (isExcluded) IconStyle.STANDARD else currentStyle, currentShape, sizePx, customBgColor = customBg, customFgColor = customFg, customUseOriginal = customOriginal, customUseOriginalBg = customOriginalBg)
            }
        } else {
            iconProcessor.processIcon(app.icon, isThemed && !isExcluded, themeColors, if (isExcluded) IconStyle.STANDARD else currentStyle, currentShape, sizePx, customBgColor = customBg, customFgColor = customFg, customUseOriginal = customOriginal, customUseOriginalBg = customOriginalBg)
        }
    }

    private fun saveIconToDisk(bitmap: ImageBitmap, file: File) {
        try {
            val b = bitmap.asAndroidBitmap()
            FileOutputStream(file).use { out ->
                b.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    /**
     * App 相關設定
     */

    enum class DropType { REORDER, FOLDER }

    fun updateStackChildren(stackId: String, children: List<WidgetModel>) {
        val currentPages = _pages.value.map { page ->
            page.map { item ->
                if (item.widget?.id == stackId && item.widget.type is WidgetType.Stack) {
                    val isWide = item.widget.type.isWide
                    item.copy(widget = item.widget.copy(type = WidgetType.Stack(children, isWide)))
                } else item
            }
        }
        _pages.value = currentPages

        val newMinusOne = _minusOneWidgets.value.map { widget ->
            if (widget.id == stackId && widget.type is WidgetType.Stack) {
                val isWide = widget.type.isWide
                widget.copy(type = WidgetType.Stack(children, isWide))
            } else widget
        }
        _minusOneWidgets.value = newMinusOne

        saveLayout()
        saveWidgets(newMinusOne)
    }

    fun updateNoteText(widgetId: String, text: String) {
        val currentPages = _pages.value.map { page ->
            page.map { item ->
                if (item.widget?.id == widgetId && item.widget.type is WidgetType.Note) {
                    item.copy(widget = item.widget.copy(type = item.widget.type.copy(text = text)))
                } else item
            }
        }
        _pages.value = currentPages

        val newMinusOne = _minusOneWidgets.value.map { widget ->
            if (widget.id == widgetId && widget.type is WidgetType.Note) {
                widget.copy(type = widget.type.copy(text = text))
            } else widget
        }
        _minusOneWidgets.value = newMinusOne

        saveLayout()
        saveWidgets(newMinusOne)
    }


    fun updateDockApp(slotIndex: Int, packageName: String) {
        val current = _dockPackageNames.value.toMutableList()
        // 確保列表長度足夠
        while (current.size <= slotIndex) {
            current.add("")
        }
        current[slotIndex] = packageName
        _dockPackageNames.value = current
        prefs.edit().putString("dock_packages", current.joinToString(",")).apply()
    }

    fun setEditMode(enabled: Boolean) {
        _isEditMode.value = enabled
    }

    fun setDoubleTapAction(action: GestureAction) {
        _doubleTapAction.value = action
        prefs.edit().putString("double_tap_action", action.name).apply()
    }

    fun setSwipeUpAction(action: GestureAction) {
        _swipeUpAction.value = action
        prefs.edit().putString("swipe_up_action", action.name).apply()
    }

    fun setDoubleTapApp(packageName: String) {
        _doubleTapApp.value = packageName
        prefs.edit().putString("double_tap_app", packageName).apply()
    }

    fun setSwipeUpApp(packageName: String) {
        _swipeUpApp.value = packageName
        prefs.edit().putString("swipe_up_app", packageName).apply()
    }

    fun setSwipeDownAction(action: GestureAction) {
        _swipeDownAction.value = action
        prefs.edit().putString("swipe_down_action", action.name).apply()
    }

    fun setLongPressAction(action: GestureAction) {
        _longPressAction.value = action
        prefs.edit().putString("long_press_action", action.name).apply()
    }

    fun setSwipeDownApp(packageName: String) {
        _swipeDownApp.value = packageName
        prefs.edit().putString("swipe_down_app", packageName).apply()
    }

    fun setLongPressApp(packageName: String) {
        _longPressApp.value = packageName
        prefs.edit().putString("long_press_app", packageName).apply()
    }

    fun setTwoFingerSwipeUpAction(action: GestureAction) {
        _twoFingerSwipeUpAction.value = action
        prefs.edit().putString("two_finger_swipe_up_action", action.name).apply()
    }

    fun setTwoFingerSwipeDownAction(action: GestureAction) {
        _twoFingerSwipeDownAction.value = action
        prefs.edit().putString("two_finger_swipe_down_action", action.name).apply()
    }

    fun resetGestures() {
        setDoubleTapAction(GestureAction.NONE)
        setSwipeUpAction(GestureAction.NONE)
        setSwipeDownAction(GestureAction.OPEN_GLOBAL_SEARCH)
        setLongPressAction(GestureAction.OPEN_DESKTOP_MENU)
        setTwoFingerSwipeUpAction(GestureAction.NONE)
        setTwoFingerSwipeDownAction(GestureAction.NONE)
    }

    fun applySuggestedGestures() {
        setDoubleTapAction(GestureAction.LOCK_SCREEN)
        setSwipeUpAction(GestureAction.OPEN_SYSTEM_SETTINGS)
        setSwipeDownAction(GestureAction.OPEN_GLOBAL_SEARCH)
        setLongPressAction(GestureAction.OPEN_DESKTOP_MENU)
        setTwoFingerSwipeUpAction(GestureAction.LAUNCHER_SETTINGS)
        setTwoFingerSwipeDownAction(GestureAction.OPEN_NOTIFICATIONS)
    }

    fun setTwoFingerSwipeUpApp(packageName: String) {
        _twoFingerSwipeUpApp.value = packageName
        prefs.edit().putString("two_finger_swipe_up_app", packageName).apply()
    }

    fun setTwoFingerSwipeDownApp(packageName: String) {
        _twoFingerSwipeDownApp.value = packageName
        prefs.edit().putString("two_finger_swipe_down_app", packageName).apply()
    }

    fun setThemedIconsEnabled(enabled: Boolean) {
        _isThemedIconsEnabled.value = enabled
        prefs.edit().putBoolean("themed_icons", enabled).apply()
        iconCache.evictAll() 
        loadApps()
    }

    fun setIconStyle(style: IconStyle) {
        _iconStyle.value = style
        prefs.edit().putString("icon_style", style.name).apply()
        iconCache.evictAll()
        loadApps()
    }

    fun setCustomIconBgColor(color: Int) {
        _customIconBgColor.value = color
        prefs.edit().putInt("custom_icon_bg_color", color).apply()
        iconCache.evictAll()
        loadApps()
    }

    fun setCustomIconFgColor(color: Int) {
        _customIconFgColor.value = color
        prefs.edit().putInt("custom_icon_fg_color", color).apply()
        iconCache.evictAll()
        loadApps()
    }

    fun setCustomIconUseOriginal(useOriginal: Boolean) {
        _customIconUseOriginal.value = useOriginal
        prefs.edit().putBoolean("custom_icon_use_original", useOriginal).apply()
        iconCache.evictAll()
        loadApps()
    }

    fun setCustomIconUseOriginalBg(useOriginalBg: Boolean) {
        _customIconUseOriginalBg.value = useOriginalBg
        prefs.edit().putBoolean("custom_icon_use_original_bg", useOriginalBg).apply()
        iconCache.evictAll()
        loadApps()
    }

    fun setIconShape(shape: IconShape) {
        _iconShape.value = shape
        prefs.edit().putString("icon_shape", shape.name).apply()
        iconCache.evictAll()
        loadApps()
    }

    fun setLibraryShape(shape: IconShape) {
        _libraryShape.value = shape
        prefs.edit().putString("library_shape", shape.name).apply()
    }

    fun setIconPack(packageName: String) {
        if (packageName.isNotEmpty()) {
            _isThemedIconsEnabled.value = false
            prefs.edit().putBoolean("themed_icons", false).apply()
        }
        _iconPackPackage.value = packageName
        prefs.edit().putString("icon_pack_package", packageName).apply()
        iconCache.evictAll()
        // 強制重新載入所有 App 以應用新的圖標包與形狀裁切
        loadApps()
    }

    fun toggleExcludedThemedApp(packageName: String) {
        val current = _excludedThemedPackages.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        _excludedThemedPackages.value = current
        prefs.edit().putStringSet("excluded_themed_packages", current).apply()
        iconCache.evictAll()
        loadApps()
    }

    fun getInstalledIconPacks() = iconPackManager.getInstalledIconPacks()

    fun setLiquidGlassEnabled(enabled: Boolean) {
        _isLiquidGlassEnabled.value = enabled
        prefs.edit().putBoolean("liquid_glass_enabled", enabled).apply()
        if (enabled) {
            updateBlurredWallpaper()
        }
    }

    fun setLiquidGlassDockEnabled(enabled: Boolean) {
        _isLiquidGlassDockEnabled.value = enabled
        prefs.edit().putBoolean("liquid_glass_dock", enabled).apply()
        if (enabled) {
            updateBlurredWallpaper() // 立即開始生成，不需要等待回到主頁
        }
    }

    fun setLiquidGlassHomeFolderEnabled(enabled: Boolean) {
        _isLiquidGlassHomeFolderEnabled.value = enabled
        prefs.edit().putBoolean("liquid_glass_home_folder", enabled).apply()
        if (enabled) updateBlurredWallpaper()
    }

    fun setLiquidGlassAppLibraryFolderEnabled(enabled: Boolean) {
        _isLiquidGlassAppLibraryFolderEnabled.value = enabled
        prefs.edit().putBoolean("liquid_glass_app_library_folder", enabled).apply()
        if (enabled) updateBlurredWallpaper()
    }

    fun setLiquidGlassGlobalSearchEnabled(enabled: Boolean) {
        _isLiquidGlassGlobalSearchEnabled.value = enabled
        prefs.edit().putBoolean("liquid_glass_global_search", enabled).apply()
        if (enabled) updateBlurredWallpaper()
    }

    fun setLiquidGlassAppLibrarySearchEnabled(enabled: Boolean) {
        _isLiquidGlassAppLibrarySearchEnabled.value = enabled
        prefs.edit().putBoolean("liquid_glass_app_library_search", enabled).apply()
        if (enabled) updateBlurredWallpaper()
    }

    fun setLiquidGlassWidgetsEnabled(enabled: Boolean) {
        _isLiquidGlassWidgetsEnabled.value = enabled
        prefs.edit().putBoolean("liquid_glass_widgets", enabled).apply()
        if (enabled) updateBlurredWallpaper()
    }

    fun setHomeMenuOption(option: String, enabled: Boolean) {
        val current = _homeMenuOptions.value.toMutableSet()
        if (enabled) current.add(option) else current.remove(option)
        _homeMenuOptions.value = current
        prefs.edit().putStringSet("home_menu_options", current).apply()
    }

    fun toggleFavoriteApp(packageName: String) {
        val current = _favoritePackages.value.toMutableSet()
        if (current.contains(packageName)) current.remove(packageName)
        else current.add(packageName)
        _favoritePackages.value = current
        prefs.edit().putStringSet("favorite_packages", current).apply()
    }

    fun setShowMinusOnePage(enabled: Boolean) {
        _showMinusOnePage.value = enabled
        prefs.edit().putBoolean("show_minus_one", enabled).apply()
    }

    fun setShowAppLibrary(enabled: Boolean) {
        _showAppLibrary.value = enabled
        prefs.edit().putBoolean("show_app_library", enabled).apply()
    }

    fun setLiquidGlassBlur(value: Float) {
        _liquidGlassBlur.value = value
        prefs.edit().putFloat("liquid_glass_blur", value).apply()
    }

    fun setLiquidGlassRefractionHeight(value: Float) {
        _liquidGlassRefractionHeight.value = value
        prefs.edit().putFloat("liquid_glass_refraction_height", value).apply()
    }

    fun setLiquidGlassRefractionAmount(value: Float) {
        _liquidGlassRefractionAmount.value = value
        prefs.edit().putFloat("liquid_glass_refraction_amount", value).apply()
    }

    fun setLiquidGlassChromaticAberration(enabled: Boolean) {
        _liquidGlassChromaticAberration.value = enabled
        prefs.edit().putBoolean("liquid_glass_chromatic_aberration", enabled).apply()
    }

    fun setDesktopRows(rows: Int) {
        _desktopRows.value = rows
        prefs.edit().putInt("desktop_rows", rows).apply()
        loadApps() // Trigger relayout
    }

    fun setDockStyle(style: DockStyle) {
        _dockStyle.value = style
        prefs.edit().putString("dock_style", style.name).apply()
    }

    fun resetLiquidGlassParams() {
        setLiquidGlassBlur(0f)
        setLiquidGlassRefractionHeight(24f)
        setLiquidGlassRefractionAmount(48f)
        setLiquidGlassChromaticAberration(true)
    }

    fun logAppLaunch(packageName: String) {
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

    fun exportConfig(): String {
        return ConfigSerializer.exportConfig(
            themedIcons = _isThemedIconsEnabled.value,
            liquidGlassDock = _isLiquidGlassDockEnabled.value,
            liquidGlassHomeFolder = _isLiquidGlassHomeFolderEnabled.value,
            liquidGlassAppLibraryFolder = _isLiquidGlassAppLibraryFolderEnabled.value,
            liquidGlassGlobalSearch = _isLiquidGlassGlobalSearchEnabled.value,
            liquidGlassAppLibrarySearch = _isLiquidGlassAppLibrarySearchEnabled.value,
            liquidGlassWidgets = _isLiquidGlassWidgetsEnabled.value,
            liquidGlassEnabled = _isLiquidGlassEnabled.value,
            showMinusOne = _showMinusOnePage.value,
            showAppLibrary = _showAppLibrary.value,
            iconStyle = _iconStyle.value.name,
            iconShape = _iconShape.value.name,
            libraryShape = _libraryShape.value.name,
            pageSize = pageSize,
            password = getPassword(),
            hiddenPackages = hiddenPackages,
            customLabels = customLabels,
            customCategories = customCategories,
            userCategories = _userCategories.value,
            categoryRenames = categoryRenames,
            pages = _pages.value,
            minusOneWidgets = _minusOneWidgets.value,
            dockPackageNames = _dockPackageNames.value,
            launchCounts = prefs.getString("launch_counts", "")
        )
    }

    fun importConfig(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            val settings = root.getJSONObject("settings")

            // 1. 恢復基礎設定
            setThemedIconsEnabled(settings.optBoolean("themed_icons", false))
            setLiquidGlassDockEnabled(settings.optBoolean("liquid_glass_dock", false))
            setLiquidGlassHomeFolderEnabled(settings.optBoolean("liquid_glass_home_folder", false))
            setLiquidGlassAppLibraryFolderEnabled(settings.optBoolean("liquid_glass_app_library_folder", false))
            setLiquidGlassGlobalSearchEnabled(settings.optBoolean("liquid_glass_global_search", false))
            setLiquidGlassAppLibrarySearchEnabled(settings.optBoolean("liquid_glass_app_library_search", false))
            setLiquidGlassWidgetsEnabled(settings.optBoolean("liquid_glass_widgets", false))
            setLiquidGlassEnabled(settings.optBoolean("liquid_glass_enabled", false))
            setShowMinusOnePage(settings.optBoolean("show_minus_one", true))
            setShowAppLibrary(settings.optBoolean("show_app_library", true))

            val savedStyle = settings.optString("icon_style", "STANDARD")
            _iconStyle.value = try { IconStyle.valueOf(savedStyle) } catch(e: Exception) { IconStyle.STANDARD }
            prefs.edit().putString("icon_style", _iconStyle.value.name).apply()

            val savedShape = settings.optString("icon_shape", "DEFAULT")
            _iconShape.value = try { IconShape.valueOf(savedShape) } catch(e: Exception) { IconShape.DEFAULT }
            prefs.edit().putString("icon_shape", _iconShape.value.name).apply()

            val savedLibShape = settings.optString("library_shape", "DEFAULT")
            _libraryShape.value = try { IconShape.valueOf(savedLibShape) } catch(e: Exception) { IconShape.DEFAULT }
            prefs.edit().putString("library_shape", _libraryShape.value.name).apply()

            pageSize = settings.optInt("page_size", 20)
            setPassword(settings.optString("hidden_password", "1234"))

            // 2. 恢復 App 設定
            hiddenPackages.clear()
            val hidden = root.getJSONArray("hidden_apps")
            for (i in 0 until hidden.length()) hiddenPackages.add(hidden.getString(i))
            prefs.edit().putStringSet("hidden_apps", hiddenPackages).apply()

            customLabels.clear()
            val labels = root.getJSONObject("custom_labels")
            labels.keys().forEach { customLabels[it] = labels.getString(it) }
            val labelsJson = JSONObject()
            customLabels.forEach { (k, v) -> labelsJson.put(k, v) }
            prefs.edit().putString("custom_labels_json", labelsJson.toString()).apply()

            customCategories.clear()
            val categories = root.getJSONObject("custom_categories")
            categories.keys().forEach { customCategories[it] = categories.getString(it) }
            saveCustomCategories()

            val userCats = mutableListOf<String>()
            val userCatsArray = root.getJSONArray("user_categories")
            for (i in 0 until userCatsArray.length()) userCats.add(userCatsArray.getString(i))
            _userCategories.value = userCats
            saveUserCategories(userCats)

            categoryRenames.clear()
            val renames = root.optJSONObject("category_renames")
            if (renames != null) {
                renames.keys().forEach { categoryRenames[it] = renames.getString(it) }
            }
            saveCategoryRenames()

            // 3. 恢復負一屏小工具
            val minusOne = mutableListOf<WidgetModel>()
            val minusOneArray = root.getJSONArray("minus_one_widgets")
            for (i in 0 until minusOneArray.length()) {
                ConfigSerializer.deserializeWidgetModel(minusOneArray.getJSONObject(i))?.let { minusOne.add(it) }
            }
            _minusOneWidgets.value = minusOne
            saveWidgets(minusOne)

            // 4. 恢復佈局 (需要等待 allApps 加載完成，所以這裡直接存入 prefs)
            val layout = root.getJSONArray("layout")
            prefs.edit().putString("launcher_layout_v2", layout.toString()).apply()

            // 5. 恢復 Dock & Stats
            val dock = root.optJSONArray("dock")
            if (dock != null) {
                val dockList = mutableListOf<String>()
                for (i in 0 until dock.length()) dockList.add(dock.getString(i))
                _dockPackageNames.value = dockList
                prefs.edit().putString("dock_packages", dockList.joinToString(",")).apply()
            }
            prefs.edit().putString("launch_counts", root.optString("launch_counts", "")).apply()

            // 重新加載所有內容
            loadApps()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun updateSuggestions() {
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

        _suggestedApps.value = _allApps.value.filter { topPackages.contains(it.packageName) && !it.isHidden }
    }

    private val widgetPhotoDir = File(getApplication<Application>().filesDir, "widget_photos").apply { mkdirs() }

    fun saveWidgetPhoto(widgetId: String, bitmap: Bitmap) {
        val file = File(widgetPhotoDir, "$widgetId.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        _minusOneWidgets.value = _minusOneWidgets.value.toList()
    }

    fun getWidgetPhoto(widgetId: String): Bitmap? {
        val file = File(widgetPhotoDir, "$widgetId.png")
        return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }
}
