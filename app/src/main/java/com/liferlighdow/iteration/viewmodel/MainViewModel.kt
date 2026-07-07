package com.liferlighdow.iteration.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.LauncherApps
import android.graphics.BitmapFactory
import android.os.Build
import android.os.UserHandle
import android.util.LruCache
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.os.LocaleListCompat
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.liferlighdow.iteration.utils.ActionMode
import com.liferlighdow.iteration.utils.GestureAction
import com.liferlighdow.iteration.utils.IconPackManager
import com.liferlighdow.iteration.utils.IconProcessor
import com.liferlighdow.iteration.utils.IconShape
import com.liferlighdow.iteration.utils.IconStyle
import com.liferlighdow.iteration.utils.WallpaperProcessor
import com.liferlighdow.iteration.data.*
import com.liferlighdow.iteration.ui.*
import com.liferlighdow.iteration.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {
    internal val repository = AppRepository(application)
    internal val weatherRepository = WeatherRepository(application)
    internal val currencyRepository = CurrencyRepository(application)
    internal val prefs = application.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
    internal val iconProcessor = IconProcessor(application)
    internal val iconPackManager = IconPackManager(application)
    internal val wallpaperProcessor = WallpaperProcessor(application)
    internal val wallpaperFile = File(application.filesDir, "launcher_wallpaper.png")

    internal val _blurredWallpaper = MutableStateFlow<ImageBitmap?>(null)
    val blurredWallpaper = _blurredWallpaper.asStateFlow()

    internal val _rawWallpaper = MutableStateFlow<ImageBitmap?>(null)
    val rawWallpaper = _rawWallpaper.asStateFlow()

    // 用於強制更新 UI 的訊號
    internal val _wallpaperUpdateSignal = MutableStateFlow(0L)
    val wallpaperUpdateSignal = _wallpaperUpdateSignal.asStateFlow()

    internal val _iconUpdateSignal = MutableStateFlow(0L)
    val iconUpdateSignal = _iconUpdateSignal.asStateFlow()

    internal val _widgetUpdateSignal = MutableStateFlow(0L)
    val widgetUpdateSignal = _widgetUpdateSignal.asStateFlow()

    internal val _pages = MutableStateFlow<List<List<AppModel>>>(emptyList())
    val pages: StateFlow<List<List<AppModel>>> = _pages

    internal val _allApps = MutableStateFlow<List<AppModel>>(emptyList())
    val allApps: StateFlow<List<AppModel>> = _allApps

    internal val _dockPackageNames = MutableStateFlow(listOf<String>())
    val dockPackageNames = _dockPackageNames.asStateFlow()

    internal val _minusOneWidgets = MutableStateFlow<List<WidgetModel>>(emptyList())
    val minusOneWidgets: StateFlow<List<WidgetModel>> = _minusOneWidgets.asStateFlow()

    internal val _isEditMode = MutableStateFlow(false)
    val isEditMode = _isEditMode.asStateFlow()

    internal val _isThemedIconsEnabled = MutableStateFlow(prefs.getBoolean("themed_icons", false))
    val isThemedIconsEnabled = _isThemedIconsEnabled.asStateFlow()

    internal val _isLiquidGlassEnabled =
        MutableStateFlow(prefs.getBoolean("liquid_glass_enabled", false))
    val isLiquidGlassEnabled = _isLiquidGlassEnabled.asStateFlow()

    internal val _isLiquidGlassDockEnabled =
        MutableStateFlow(prefs.getBoolean("liquid_glass_dock", false))
    val isLiquidGlassDockEnabled = _isLiquidGlassDockEnabled.asStateFlow()

    internal val _isLiquidGlassHomeFolderEnabled =
        MutableStateFlow(prefs.getBoolean("liquid_glass_home_folder", false))
    val isLiquidGlassHomeFolderEnabled = _isLiquidGlassHomeFolderEnabled.asStateFlow()

    internal val _isLiquidGlassAppLibraryFolderEnabled =
        MutableStateFlow(prefs.getBoolean("liquid_glass_app_library_folder", false))
    val isLiquidGlassAppLibraryFolderEnabled = _isLiquidGlassAppLibraryFolderEnabled.asStateFlow()

    internal val _isLiquidGlassGlobalSearchEnabled =
        MutableStateFlow(prefs.getBoolean("liquid_glass_global_search", false))
    val isLiquidGlassGlobalSearchEnabled = _isLiquidGlassGlobalSearchEnabled.asStateFlow()

    internal val _isLiquidGlassAppLibrarySearchEnabled =
        MutableStateFlow(prefs.getBoolean("liquid_glass_app_library_search", false))
    val isLiquidGlassAppLibrarySearchEnabled = _isLiquidGlassAppLibrarySearchEnabled.asStateFlow()

    internal val _isLiquidGlassWidgetsEnabled =
        MutableStateFlow(prefs.getBoolean("liquid_glass_widgets", false))
    val isLiquidGlassWidgetsEnabled = _isLiquidGlassWidgetsEnabled.asStateFlow()

    internal val _isLiquidGlassMinusOneWidgetEnabled =
        MutableStateFlow(prefs.getBoolean("liquid_glass_minus_one_widget", false))
    val isLiquidGlassMinusOneWidgetEnabled = _isLiquidGlassMinusOneWidgetEnabled.asStateFlow()

    internal val _isLiquidGlassMinusOneSearchEnabled =
        MutableStateFlow(prefs.getBoolean("liquid_glass_minus_one_search", false))
    val isLiquidGlassMinusOneSearchEnabled = _isLiquidGlassMinusOneSearchEnabled.asStateFlow()

    internal val _isLiquidGlassMinusOneButtonEnabled =
        MutableStateFlow(prefs.getBoolean("liquid_glass_minus_one_button", false))
    val isLiquidGlassMinusOneButtonEnabled = _isLiquidGlassMinusOneButtonEnabled.asStateFlow()

    internal val _isNetworkAccessEnabled =
        MutableStateFlow(prefs.getBoolean("network_access_enabled", true))
    val isNetworkAccessEnabled = _isNetworkAccessEnabled.asStateFlow()

    internal val _isSystemNetworkEnabled = MutableStateFlow(true)
    val isSystemNetworkEnabled = _isSystemNetworkEnabled.asStateFlow()

    val exchangeRates = currencyRepository.exchangeRates
    val weatherInfo = weatherRepository.weatherInfo
    val weatherError = weatherRepository.weatherError
    val weatherProvider = weatherRepository.weatherProvider

    internal val _autoAddAppsToHome = MutableStateFlow(prefs.getBoolean("auto_add_apps_to_home", true))
    val autoAddAppsToHome = _autoAddAppsToHome.asStateFlow()

    internal val _themeMode = MutableStateFlow(
        try {
            ThemeMode.valueOf(prefs.getString("theme_mode", "FOLLOW_SYSTEM") ?: "FOLLOW_SYSTEM")
        } catch (e: Exception) {
            ThemeMode.FOLLOW_SYSTEM
        }
    )
    val themeMode = _themeMode.asStateFlow()

    internal val _isAmoledBlack = MutableStateFlow(prefs.getBoolean("amoled_black", false))
    val isAmoledBlack = _isAmoledBlack.asStateFlow()

    internal val _showStatusBar = MutableStateFlow(prefs.getBoolean("show_status_bar", true))
    val showStatusBar = _showStatusBar.asStateFlow()

    internal val _showNavigationBar = MutableStateFlow(prefs.getBoolean("show_navigation_bar", true))
    val showNavigationBar = _showNavigationBar.asStateFlow()

    internal val _iconCacheSize = MutableStateFlow(prefs.getInt("icon_cache_size", 250))
    val iconCacheSize = _iconCacheSize.asStateFlow()

    internal val _showMinusOnePage = MutableStateFlow(prefs.getBoolean("show_minus_one", true))
    val showMinusOnePage = _showMinusOnePage.asStateFlow()

    internal val _appLanguage = MutableStateFlow(prefs.getString("app_language", "") ?: "")
    val appLanguage = _appLanguage.asStateFlow()

    init {
        // Apply saved language on startup
        val savedLang = _appLanguage.value
        if (savedLang.isNotEmpty()) {
            try {
                val appLocale = LocaleListCompat.forLanguageTags(savedLang)
                AppCompatDelegate.setApplicationLocales(appLocale)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    internal var currentStyleSuffix = "default"
    internal var themeColorsCache: ColorScheme? = null

    internal val _showAppLibrary = MutableStateFlow(prefs.getBoolean("show_app_library", true))
    val showAppLibrary = _showAppLibrary.asStateFlow()

    internal val _liquidGlassBlur = MutableStateFlow(prefs.getFloat("liquid_glass_blur", 0f))
    val liquidGlassBlur = _liquidGlassBlur.asStateFlow()

    internal val _liquidGlassRefractionHeight =
        MutableStateFlow(prefs.getFloat("liquid_glass_refraction_height", 24f))
    val liquidGlassRefractionHeight = _liquidGlassRefractionHeight.asStateFlow()

    internal val _liquidGlassRefractionAmount =
        MutableStateFlow(prefs.getFloat("liquid_glass_refraction_amount", 48f))
    val liquidGlassRefractionAmount = _liquidGlassRefractionAmount.asStateFlow()

    internal val _liquidGlassChromaticAberration =
        MutableStateFlow(prefs.getBoolean("liquid_glass_chromatic_aberration", true))
    val liquidGlassChromaticAberration = _liquidGlassChromaticAberration.asStateFlow()

    internal val _homeMenuOptions = MutableStateFlow(
        prefs.getStringSet("home_menu_options", setOf("delete_home", "uninstall"))
            ?: setOf("delete_home", "uninstall")
    )
    val homeMenuOptions = _homeMenuOptions.asStateFlow()

    internal val _favoritePackages =
        MutableStateFlow(prefs.getStringSet("favorite_packages", emptySet()) ?: emptySet())
    val favoritePackages = _favoritePackages.asStateFlow()

    internal val _doubleTapAction = MutableStateFlow(
        try {
            GestureAction.valueOf(prefs.getString("double_tap_action", "NONE") ?: "NONE")
        } catch (e: Exception) {
            GestureAction.NONE
        }
    )
    val doubleTapAction = _doubleTapAction.asStateFlow()

    internal val _actionMode = MutableStateFlow(
        try {
            ActionMode.valueOf(prefs.getString("action_mode", "ACCESSIBILITY") ?: "ACCESSIBILITY")
        } catch (e: Exception) {
            ActionMode.ACCESSIBILITY
        }
    )
    val actionMode = _actionMode.asStateFlow()

    internal val _swipeUpAction = MutableStateFlow(
        try {
            GestureAction.valueOf(prefs.getString("swipe_up_action", "NONE") ?: "NONE")
        } catch (e: Exception) {
            GestureAction.NONE
        }
    )
    val swipeUpAction = _swipeUpAction.asStateFlow()

    internal val _doubleTapApp = MutableStateFlow(prefs.getString("double_tap_app", "") ?: "")
    val doubleTapApp = _doubleTapApp.asStateFlow()

    internal val _swipeUpApp = MutableStateFlow(prefs.getString("swipe_up_app", "") ?: "")
    val swipeUpApp = _swipeUpApp.asStateFlow()

    internal val _swipeDownAction = MutableStateFlow(
        try {
            GestureAction.valueOf(
                prefs.getString("swipe_down_action", "OPEN_GLOBAL_SEARCH") ?: "OPEN_GLOBAL_SEARCH"
            )
        } catch (e: Exception) {
            GestureAction.OPEN_GLOBAL_SEARCH
        }
    )
    val swipeDownAction = _swipeDownAction.asStateFlow()

    internal val _longPressAction = MutableStateFlow(
        try {
            GestureAction.valueOf(
                prefs.getString("long_press_action", "OPEN_DESKTOP_MENU") ?: "OPEN_DESKTOP_MENU"
            )
        } catch (e: Exception) {
            GestureAction.OPEN_DESKTOP_MENU
        }
    )
    val longPressAction = _longPressAction.asStateFlow()

    internal val _swipeDownApp = MutableStateFlow(prefs.getString("swipe_down_app", "") ?: "")
    val swipeDownApp = _swipeDownApp.asStateFlow()

    internal val _longPressApp = MutableStateFlow(prefs.getString("long_press_app", "") ?: "")
    val longPressApp = _longPressApp.asStateFlow()

    internal val _twoFingerSwipeUpAction = MutableStateFlow(
        try {
            GestureAction.valueOf(prefs.getString("two_finger_swipe_up_action", "NONE") ?: "NONE")
        } catch (e: Exception) {
            GestureAction.NONE
        }
    )
    val twoFingerSwipeUpAction = _twoFingerSwipeUpAction.asStateFlow()

    internal val _twoFingerSwipeDownAction = MutableStateFlow(
        try {
            GestureAction.valueOf(
                prefs.getString(
                    "two_finger_swipe_down_action",
                    "OPEN_NOTIFICATIONS"
                ) ?: "OPEN_NOTIFICATIONS"
            )
        } catch (e: Exception) {
            GestureAction.OPEN_NOTIFICATIONS
        }
    )
    val twoFingerSwipeDownAction = _twoFingerSwipeDownAction.asStateFlow()

    internal val _twoFingerSwipeUpApp =
        MutableStateFlow(prefs.getString("two_finger_swipe_up_app", "") ?: "")
    val twoFingerSwipeUpApp = _twoFingerSwipeUpApp.asStateFlow()

    internal val _twoFingerSwipeDownApp =
        MutableStateFlow(prefs.getString("two_finger_swipe_down_app", "") ?: "")
    val twoFingerSwipeDownApp = _twoFingerSwipeDownApp.asStateFlow()

    internal val _iconPackPackage = MutableStateFlow(prefs.getString("icon_pack_package", "") ?: "")
    val iconPackPackage = _iconPackPackage.asStateFlow()

    internal val _iconStyle = MutableStateFlow(
        try {
            IconStyle.valueOf(prefs.getString("icon_style", "STANDARD") ?: "STANDARD")
        } catch (e: Exception) {
            IconStyle.STANDARD
        }
    )
    val iconStyle = _iconStyle.asStateFlow()

    internal val _customIconBgColor =
        MutableStateFlow(prefs.getInt("custom_icon_bg_color", 0xFF2196F3.toInt()))
    val customIconBgColor = _customIconBgColor.asStateFlow()

    internal val _customIconFgColor =
        MutableStateFlow(prefs.getInt("custom_icon_fg_color", 0xFFFFFFFF.toInt()))
    val customIconFgColor = _customIconFgColor.asStateFlow()

    internal val _customIconUseOriginal =
        MutableStateFlow(prefs.getBoolean("custom_icon_use_original", false))
    val customIconUseOriginal = _customIconUseOriginal.asStateFlow()

    internal val _customIconUseOriginalBg =
        MutableStateFlow(prefs.getBoolean("custom_icon_use_original_bg", false))
    val customIconUseOriginalBg = _customIconUseOriginalBg.asStateFlow()

    internal val _iconShape = MutableStateFlow(
        try {
            IconShape.valueOf(prefs.getString("icon_shape", "DEFAULT") ?: "DEFAULT")
        } catch (e: Exception) {
            IconShape.DEFAULT
        }
    )
    val iconShape = _iconShape.asStateFlow()

    internal val _desktopRows = MutableStateFlow(prefs.getInt("desktop_rows", 0)) // 0 means auto
    val desktopRows = _desktopRows.asStateFlow()

    internal val _dockStyle = MutableStateFlow(
        try {
            DockStyle.valueOf(prefs.getString("dock_style", "MODERN") ?: "MODERN")
        } catch (e: Exception) {
            DockStyle.MODERN
        }
    )
    val dockStyle = _dockStyle.asStateFlow()

    internal val _dockCornerRadius = MutableStateFlow(prefs.getFloat("dock_corner_radius", 42f))
    val dockCornerRadius = _dockCornerRadius.asStateFlow()

    internal val _searchEngineUrl = MutableStateFlow(
        prefs.getString("search_engine_url", "https://www.google.com/search?q=")
            ?: "https://www.google.com/search?q="
    )
    val searchEngineUrl = _searchEngineUrl.asStateFlow()

    internal val _excludedThemedPackages =
        MutableStateFlow(prefs.getStringSet("excluded_themed_packages", emptySet()) ?: emptySet())
    val excludedThemedPackages = _excludedThemedPackages.asStateFlow()

    internal val _libraryShape = MutableStateFlow(
        try {
            IconShape.valueOf(prefs.getString("library_shape", "DEFAULT") ?: "DEFAULT")
        } catch (e: Exception) {
            IconShape.DEFAULT
        }
    )
    val libraryShape = _libraryShape.asStateFlow()

    internal val hiddenPackages = mutableSetOf<String>()
    internal val customLabels = mutableMapOf<String, String>()
    internal val customCategories = mutableMapOf<String, String>()
    internal val categoryRenames = mutableMapOf<String, String>()
    internal val _userCategories = MutableStateFlow<List<String>>(emptyList())
    val userCategories: StateFlow<List<String>> = _userCategories.asStateFlow()

    // --- 新增：響應式 UI 狀態 ---
    internal val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    internal val _isLibrarySearchFocused = MutableStateFlow(false)
    val isLibrarySearchFocused = _isLibrarySearchFocused.asStateFlow()

    fun setLibrarySearchFocused(focused: Boolean) {
        _isLibrarySearchFocused.value = focused
    }

    internal val _selectedCategory = MutableStateFlow("All")
    val selectedCategory = _selectedCategory.asStateFlow()

    /**
     * 優化：預先排序好的全 App 清單
     */
    private val sortedAllApps = _allApps.map { apps: List<AppModel> ->
        apps.sortedBy { it.label.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * 自動計算 App Library 的顯示清單 (高效過濾)
     */
    val filteredLibraryApps = combine(
        sortedAllApps,
        _searchQuery,
        _selectedCategory
    ) { apps: List<AppModel>, query: String, category: String ->
        if (query.isEmpty() && category == "All") return@combine apps

        apps.filter { app ->
            val matchesQuery =
                if (query.isEmpty()) true else app.label.contains(query, ignoreCase = true)
            val matchesCategory = when (category) {
                "All" -> true
                "Hidden Apps" -> app.isHidden
                else -> app.displayCategory == category
            }
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSelectedCategory(category: String) { _selectedCategory.value = category }
    // --------------------------

    internal val _suggestedApps = MutableStateFlow<List<AppModel>>(emptyList())
    val suggestedApps = _suggestedApps.asStateFlow()

    internal val _contacts = MutableStateFlow<List<ContactModel>>(emptyList())
    val contacts = _contacts.asStateFlow()

    internal val customIconDir = File(application.filesDir, "custom_icons").apply { mkdirs() }
    internal val processedIconCacheDir = File(application.cacheDir, "processed_icons").apply { mkdirs() }
    internal var pageSize = 20
    internal var dragBackupPages: List<List<AppModel>>? = null

    // --- 新增：監聽 App 安裝/卸載/更新 ---
    private val launcherApps = application.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null || intent.action != Intent.ACTION_PACKAGE_REMOVED) return
            
            val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
            if (!isReplacing) {
                val packageName = intent.data?.schemeSpecificPart ?: return
                performFullPackageCleanup(packageName)
            }
        }
    }

    private fun performFullPackageCleanup(packageName: String) {
        // 1. 從 seen_apps 中移除
        val seenApps = (prefs.getStringSet("seen_apps", emptySet()) ?: emptySet()).toMutableSet()
        val toRemoveSeen = seenApps.filter { it.startsWith("$packageName/") || it == packageName }
        if (toRemoveSeen.isNotEmpty()) {
            seenApps.removeAll(toRemoveSeen.toSet())
            prefs.edit().putStringSet("seen_apps", seenApps).apply()
        }

        // 2. 從排除名單中移除
        val excluded = (prefs.getStringSet("excluded_themed_packages", emptySet()) ?: emptySet()).toMutableSet()
        if (excluded.remove(packageName)) {
            prefs.edit().putStringSet("excluded_themed_packages", excluded).apply()
            _excludedThemedPackages.value = excluded
        }

        // 3. 從隱藏名單中移除
        if (hiddenPackages.remove(packageName)) {
            prefs.edit().putStringSet("hidden_apps", hiddenPackages).apply()
        }

        // 4. 清除自定義標籤與類別
        val labelsUpdated = customLabels.remove(packageName) != null
        if (labelsUpdated) {
            val json = JSONObject()
            customLabels.forEach { (k, v) -> json.put(k, v) }
            prefs.edit().putString("custom_labels_json", json.toString()).apply()
        }
        if (customCategories.remove(packageName) != null) {
            saveCustomCategories()
        }
    }

    private val packageCallback = object : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String, user: UserHandle) {
            refreshApps()
        }
        override fun onPackageRemoved(packageName: String, user: UserHandle) {
            clearAppIconCache(packageName)
            refreshApps()
        }
        override fun onPackageChanged(packageName: String, user: UserHandle) {
            clearAppIconCache(packageName)
            refreshApps()
        }
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
            refreshApps()
        }
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
            refreshApps()
        }

        override fun onShortcutsChanged(packageName: String, shortcuts: List<android.content.pm.ShortcutInfo>, user: UserHandle) {
            refreshApps()
        }
    }

    private fun refreshApps() {
        cachedRawApps = null // 1. 強制清空系統應用快取
        // 2. 加入延遲，確保系統層同步完成
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            loadApps()
            // 這裡不再發送廣播，避免無窮遞迴導致當機
        }
    }

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.liferlighdow.iteration.ACTION_REFRESH_APPS") {
                refreshApps()
            }
        }
    }

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        when (key) {
            "dock_style" -> {
                try {
                    _dockStyle.value = DockStyle.valueOf(sharedPreferences.getString(key, "MODERN") ?: "MODERN")
                } catch (_: Exception) {}
            }
            "dock_corner_radius" -> {
                _dockCornerRadius.value = sharedPreferences.getFloat(key, 42f)
            }
            "liquid_glass_blur" -> _liquidGlassBlur.value = sharedPreferences.getFloat(key, 0f)
            "liquid_glass_refraction_height" -> _liquidGlassRefractionHeight.value = sharedPreferences.getFloat(key, 24f)
            "liquid_glass_refraction_amount" -> _liquidGlassRefractionAmount.value = sharedPreferences.getFloat(key, 48f)
            "liquid_glass_chromatic_aberration" -> _liquidGlassChromaticAberration.value = sharedPreferences.getBoolean(key, true)
            "liquid_glass_minus_one_widget" -> _isLiquidGlassMinusOneWidgetEnabled.value = sharedPreferences.getBoolean(key, false)
            "liquid_glass_minus_one_search" -> _isLiquidGlassMinusOneSearchEnabled.value = sharedPreferences.getBoolean(key, false)
            "liquid_glass_minus_one_button" -> _isLiquidGlassMinusOneButtonEnabled.value = sharedPreferences.getBoolean(key, false)
            "themed_icons" -> _isThemedIconsEnabled.value = sharedPreferences.getBoolean(key, false)
            "show_minus_one" -> _showMinusOnePage.value = sharedPreferences.getBoolean(key, true)
            "show_app_library" -> _showAppLibrary.value = sharedPreferences.getBoolean(key, true)
            "icon_cache_size" -> {
                val newSize = sharedPreferences.getInt(key, 250)
                _iconCacheSize.value = newSize
                iconCache.resize(newSize)
            }
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        
        viewModelScope.launch {
            // 延遲載入非核心任務，避免 init 阻塞主執行緒
            loadSettings()
            delay(100)
            loadApps()
            delay(500)
            updateBlurredWallpaper()
            fetchExchangeRates()
            fetchWeather()
            checkSystemNetworkStatus()
        }

        // 註冊監聽
        launcherApps.registerCallback(packageCallback)
        
        val filter = IntentFilter("com.liferlighdow.iteration.ACTION_REFRESH_APPS")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getApplication<Application>().registerReceiver(refreshReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            getApplication<Application>().registerReceiver(refreshReceiver, filter)
        }

        // 註冊套件移除監聽（用於區分卸載與更新）
        val pkgFilter = IntentFilter(Intent.ACTION_PACKAGE_REMOVED).apply {
            addDataScheme("package")
        }
        getApplication<Application>().registerReceiver(packageReceiver, pkgFilter)
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        // 取消註冊，避免內存洩漏
        launcherApps.unregisterCallback(packageCallback)
        getApplication<Application>().unregisterReceiver(refreshReceiver)
        getApplication<Application>().unregisterReceiver(packageReceiver)
    }

    internal var lastBlurredSignal = -1L

    internal val iconCache = object : LruCache<String, ImageBitmap>(prefs.getInt("icon_cache_size", 250)) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = 1
    }

    internal var loadAppsJob: Job? = null
    internal var cachedRawApps: List<AppModel>? = null

    internal val widgetPhotoDir = File(getApplication<Application>().filesDir, "widget_photos").apply { mkdirs() }

    enum class DropType { REORDER, FOLDER }

    // 具體邏輯已移至各個 MainViewModel*.kt 擴充檔案中
    fun saveLayout() {
        val layoutArray = JSONArray()
        _pages.value.forEach { page ->
            val pageArray = JSONArray()
            page.forEach { item ->
                pageArray.put(ConfigSerializer.serializeAppModel(item))
            }
            layoutArray.put(pageArray)
        }
        prefs.edit().putString("launcher_layout_v3", layoutArray.toString()).apply()
    }

    fun getPassword(): String? = prefs.getString("password", null)

    fun setPassword(password: String?) {
        prefs.edit().putString("password", password).apply()
    }
}
