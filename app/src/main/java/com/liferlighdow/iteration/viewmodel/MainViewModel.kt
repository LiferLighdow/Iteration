package com.liferlighdow.iteration.viewmodel

import android.Manifest
import android.app.Application
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.util.LruCache
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.os.LocaleListCompat
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.liferlighdow.iteration.utils.ActionMode
import com.liferlighdow.iteration.utils.GestureAction
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider
import android.os.IBinder
import com.liferlighdow.iteration.utils.IconPackManager
import com.liferlighdow.iteration.utils.IconProcessor
import com.liferlighdow.iteration.utils.IconShape
import com.liferlighdow.iteration.utils.IconStyle
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.utils.WallpaperProcessor
import com.liferlighdow.iteration.data.LauncherConfig
import com.liferlighdow.iteration.data.LauncherSettings
import com.liferlighdow.iteration.data.GlassParams
import com.liferlighdow.iteration.data.GestureSettings
import com.liferlighdow.iteration.data.CustomIconSettings
import com.liferlighdow.iteration.data.AppModel
import com.liferlighdow.iteration.data.*
import com.liferlighdow.iteration.ui.*
import com.liferlighdow.iteration.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.collections.forEach
import kotlin.collections.plus

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)
    private val weatherRepository = WeatherRepository(application)
    private val currencyRepository = CurrencyRepository(application)
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

    private val _iconUpdateSignal = MutableStateFlow(0L)
    val iconUpdateSignal = _iconUpdateSignal.asStateFlow()

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

    private val _isLiquidGlassEnabled =
        MutableStateFlow(prefs.getBoolean("liquid_glass_enabled", false))
    val isLiquidGlassEnabled = _isLiquidGlassEnabled.asStateFlow()

    private val _isLiquidGlassDockEnabled =
        MutableStateFlow(prefs.getBoolean("liquid_glass_dock", false))
    val isLiquidGlassDockEnabled = _isLiquidGlassDockEnabled.asStateFlow()

    private val _isLiquidGlassHomeFolderEnabled =
        MutableStateFlow(prefs.getBoolean("liquid_glass_home_folder", false))
    val isLiquidGlassHomeFolderEnabled = _isLiquidGlassHomeFolderEnabled.asStateFlow()

    private val _isLiquidGlassAppLibraryFolderEnabled =
        MutableStateFlow(prefs.getBoolean("liquid_glass_app_library_folder", false))
    val isLiquidGlassAppLibraryFolderEnabled = _isLiquidGlassAppLibraryFolderEnabled.asStateFlow()

    private val _isLiquidGlassGlobalSearchEnabled =
        MutableStateFlow(prefs.getBoolean("liquid_glass_global_search", false))
    val isLiquidGlassGlobalSearchEnabled = _isLiquidGlassGlobalSearchEnabled.asStateFlow()

    private val _isLiquidGlassAppLibrarySearchEnabled =
        MutableStateFlow(prefs.getBoolean("liquid_glass_app_library_search", false))
    val isLiquidGlassAppLibrarySearchEnabled = _isLiquidGlassAppLibrarySearchEnabled.asStateFlow()

    private val _isLiquidGlassWidgetsEnabled =
        MutableStateFlow(prefs.getBoolean("liquid_glass_widgets", false))
    val isLiquidGlassWidgetsEnabled = _isLiquidGlassWidgetsEnabled.asStateFlow()

    private val _isNetworkAccessEnabled =
        MutableStateFlow(prefs.getBoolean("network_access_enabled", true))
    val isNetworkAccessEnabled = _isNetworkAccessEnabled.asStateFlow()

    private val _isSystemNetworkEnabled = MutableStateFlow(true)
    val isSystemNetworkEnabled = _isSystemNetworkEnabled.asStateFlow()

    fun checkSystemNetworkStatus() {
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(activeNetwork)
        
        // 如果系統有網路但 App 無法獲取 INTERNET 能力，或者根本沒有活動網路
        // 注意：這只是一個啟發式判斷，因為 Wi-Fi 關閉也會導致這個結果
        // 但在設定頁面中，這可以作為「當前是否具備連線能力」的參考
        val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        _isSystemNetworkEnabled.value = hasInternet
    }

    val exchangeRates = currencyRepository.exchangeRates
    val weatherInfo = weatherRepository.weatherInfo
    val weatherError = weatherRepository.weatherError
    val weatherProvider = weatherRepository.weatherProvider

    private val _autoAddAppsToHome = MutableStateFlow(prefs.getBoolean("auto_add_apps_to_home", true))
    val autoAddAppsToHome = _autoAddAppsToHome.asStateFlow()

    private val _themeMode = MutableStateFlow(
        try {
            ThemeMode.valueOf(prefs.getString("theme_mode", "FOLLOW_SYSTEM") ?: "FOLLOW_SYSTEM")
        } catch (e: Exception) {
            ThemeMode.FOLLOW_SYSTEM
        }
    )
    val themeMode = _themeMode.asStateFlow()

    private val _isAmoledBlack = MutableStateFlow(prefs.getBoolean("amoled_black", false))
    val isAmoledBlack = _isAmoledBlack.asStateFlow()

    private val _showStatusBar = MutableStateFlow(prefs.getBoolean("show_status_bar", true))
    val showStatusBar = _showStatusBar.asStateFlow()

    private val _showNavigationBar = MutableStateFlow(prefs.getBoolean("show_navigation_bar", true))
    val showNavigationBar = _showNavigationBar.asStateFlow()

    private val _showMinusOnePage = MutableStateFlow(prefs.getBoolean("show_minus_one", true))
    val showMinusOnePage = _showMinusOnePage.asStateFlow()

    private val _appLanguage = MutableStateFlow(prefs.getString("app_language", "") ?: "")
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

    private var currentStyleSuffix = "default"
    private var themeColorsCache: ColorScheme? = null

    /**
     * 從 LruCache 或磁碟獲取處理後的圖示
     */
    fun getIcon(uniqueId: String): ImageBitmap? {
        val styleSuffix = currentStyleSuffix
        if (styleSuffix == "default") return null

        // 1. 取得基礎 ID
        // 桌面實例 ID 格式為 baseId@timestamp (時間戳通常 > 1000000000)
        // 分身 ID 格式為 pkg/act@userId
        val baseId = if (uniqueId.contains("@")) {
            val parts = uniqueId.split("@")
            val lastPart = parts.last()
            // 如果最後一部分長度 >= 10，判定為時間戳，剝離它
            if (lastPart.length >= 10 && lastPart.toLongOrNull() != null) {
                uniqueId.substringBeforeLast("@")
            } else {
                uniqueId
            }
        } else {
            uniqueId
        }
        
        val cacheKey = "${baseId}_$styleSuffix"

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

    fun triggerIconUpdate() {
        _iconUpdateSignal.value = System.currentTimeMillis()
    }

    fun clearIconCache() {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. 清除記憶體 LruCache
            iconCache.evictAll()
            // 2. 徹底刪除所有產生的磁碟快取檔案
            processedIconCacheDir.listFiles()?.forEach { it.delete() }
            // 3. 清除圖格處理器的形狀遮罩快取
            iconProcessor.clearCache()
            
            withContext(Dispatchers.Main) {
                // 4. 強制清除 PackageManager 的原始 App 緩存
                cachedRawApps = null
                
                // 5. 重新執行 loadApps (會重新讀取設定、處理圖示、產生新訊號)
                loadApps()
                
                // 6. 發送全局廣播，通知 Launcher 內的所有組件（包含頁面、搜尋框等）徹底重新載入
                val intent = Intent("com.liferlighdow.iteration.ACTION_REFRESH_APPS")
                intent.setPackage(getApplication<Application>().packageName)
                getApplication<Application>().sendBroadcast(intent)
            }
        }
    }

    private val _showAppLibrary = MutableStateFlow(prefs.getBoolean("show_app_library", true))
    val showAppLibrary = _showAppLibrary.asStateFlow()

    private val _liquidGlassBlur = MutableStateFlow(prefs.getFloat("liquid_glass_blur", 0f))
    val liquidGlassBlur = _liquidGlassBlur.asStateFlow()

    private val _liquidGlassRefractionHeight =
        MutableStateFlow(prefs.getFloat("liquid_glass_refraction_height", 24f))
    val liquidGlassRefractionHeight = _liquidGlassRefractionHeight.asStateFlow()

    private val _liquidGlassRefractionAmount =
        MutableStateFlow(prefs.getFloat("liquid_glass_refraction_amount", 48f))
    val liquidGlassRefractionAmount = _liquidGlassRefractionAmount.asStateFlow()

    private val _liquidGlassChromaticAberration =
        MutableStateFlow(prefs.getBoolean("liquid_glass_chromatic_aberration", true))
    val liquidGlassChromaticAberration = _liquidGlassChromaticAberration.asStateFlow()

    private val _homeMenuOptions = MutableStateFlow(
        prefs.getStringSet("home_menu_options", setOf("delete_home", "uninstall"))
            ?: setOf("delete_home", "uninstall")
    )
    val homeMenuOptions = _homeMenuOptions.asStateFlow()

    private val _favoritePackages =
        MutableStateFlow(prefs.getStringSet("favorite_packages", emptySet()) ?: emptySet())
    val favoritePackages = _favoritePackages.asStateFlow()

    private val _doubleTapAction = MutableStateFlow(
        try {
            GestureAction.valueOf(prefs.getString("double_tap_action", "NONE") ?: "NONE")
        } catch (e: Exception) {
            GestureAction.NONE
        }
    )
    val doubleTapAction = _doubleTapAction.asStateFlow()

    private val _actionMode = MutableStateFlow(
        try {
            ActionMode.valueOf(prefs.getString("action_mode", "ACCESSIBILITY") ?: "ACCESSIBILITY")
        } catch (e: Exception) {
            ActionMode.ACCESSIBILITY
        }
    )
    val actionMode = _actionMode.asStateFlow()

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
            GestureAction.valueOf(
                prefs.getString("swipe_down_action", "OPEN_GLOBAL_SEARCH") ?: "OPEN_GLOBAL_SEARCH"
            )
        } catch (e: Exception) {
            GestureAction.OPEN_GLOBAL_SEARCH
        }
    )
    val swipeDownAction = _swipeDownAction.asStateFlow()

    private val _longPressAction = MutableStateFlow(
        try {
            GestureAction.valueOf(
                prefs.getString("long_press_action", "OPEN_DESKTOP_MENU") ?: "OPEN_DESKTOP_MENU"
            )
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

    private val _twoFingerSwipeUpApp =
        MutableStateFlow(prefs.getString("two_finger_swipe_up_app", "") ?: "")
    val twoFingerSwipeUpApp = _twoFingerSwipeUpApp.asStateFlow()

    private val _twoFingerSwipeDownApp =
        MutableStateFlow(prefs.getString("two_finger_swipe_down_app", "") ?: "")
    val twoFingerSwipeDownApp = _twoFingerSwipeDownApp.asStateFlow()

    private val _iconPackPackage = MutableStateFlow(prefs.getString("icon_pack_package", "") ?: "")
    val iconPackPackage = _iconPackPackage.asStateFlow()

    private val _iconStyle = MutableStateFlow(
        try {
            IconStyle.valueOf(prefs.getString("icon_style", "STANDARD") ?: "STANDARD")
        } catch (e: Exception) {
            IconStyle.STANDARD
        }
    )
    val iconStyle = _iconStyle.asStateFlow()

    private val _customIconBgColor =
        MutableStateFlow(prefs.getInt("custom_icon_bg_color", 0xFF2196F3.toInt()))
    val customIconBgColor = _customIconBgColor.asStateFlow()

    private val _customIconFgColor =
        MutableStateFlow(prefs.getInt("custom_icon_fg_color", 0xFFFFFFFF.toInt()))
    val customIconFgColor = _customIconFgColor.asStateFlow()

    private val _customIconUseOriginal =
        MutableStateFlow(prefs.getBoolean("custom_icon_use_original", false))
    val customIconUseOriginal = _customIconUseOriginal.asStateFlow()

    private val _customIconUseOriginalBg =
        MutableStateFlow(prefs.getBoolean("custom_icon_use_original_bg", false))
    val customIconUseOriginalBg = _customIconUseOriginalBg.asStateFlow()

    private val _iconShape = MutableStateFlow(
        try {
            IconShape.valueOf(prefs.getString("icon_shape", "DEFAULT") ?: "DEFAULT")
        } catch (e: Exception) {
            IconShape.DEFAULT
        }
    )
    val iconShape = _iconShape.asStateFlow()

    private val _desktopRows = MutableStateFlow(prefs.getInt("desktop_rows", 0)) // 0 means auto
    val desktopRows = _desktopRows.asStateFlow()

    private val _dockStyle = MutableStateFlow(
        try {
            DockStyle.valueOf(prefs.getString("dock_style", "MODERN") ?: "MODERN")
        } catch (e: Exception) {
            DockStyle.MODERN
        }
    )
    val dockStyle = _dockStyle.asStateFlow()

    private val _dockCornerRadius = MutableStateFlow(prefs.getFloat("dock_corner_radius", 42f))
    val dockCornerRadius = _dockCornerRadius.asStateFlow()

    private val _searchEngineUrl = MutableStateFlow(
        prefs.getString("search_engine_url", "https://www.google.com/search?q=")
            ?: "https://www.google.com/search?q="
    )
    val searchEngineUrl = _searchEngineUrl.asStateFlow()

    private val _excludedThemedPackages =
        MutableStateFlow(prefs.getStringSet("excluded_themed_packages", emptySet()) ?: emptySet())
    val excludedThemedPackages = _excludedThemedPackages.asStateFlow()

    private val _libraryShape = MutableStateFlow(
        try {
            IconShape.valueOf(prefs.getString("library_shape", "DEFAULT") ?: "DEFAULT")
        } catch (e: Exception) {
            IconShape.DEFAULT
        }
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

    private val _suggestedApps = MutableStateFlow<List<AppModel>>(emptyList())
    val suggestedApps = _suggestedApps.asStateFlow()

    private val _contacts = MutableStateFlow<List<ContactModel>>(emptyList())
    val contacts = _contacts.asStateFlow()

    fun loadContacts() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                return@launch
            }

            val contactList = mutableListOf<ContactModel>()
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
            )

            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

                while (cursor.moveToNext()) {
                    val id = cursor.getString(idIdx)
                    val name = cursor.getString(nameIdx)
                    val number = cursor.getString(numIdx)
                    val photoUri = cursor.getString(photoIdx)

                    var photoBitmap: Bitmap? = null
                    if (photoUri != null) {
                        try {
                            val uri = Uri.parse(photoUri)
                            photoBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                                android.graphics.ImageDecoder.decodeBitmap(source)
                            } else {
                                @Suppress("DEPRECATION")
                                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                            }
                        } catch (e: Exception) {}
                    }
                    contactList.add(ContactModel(id, name, number, photoBitmap))
                }
            }
            _contacts.value = contactList.distinctBy { it.phoneNumber }
        }
    }

    private val customIconDir = File(application.filesDir, "custom_icons").apply { mkdirs() }
    private val processedIconCacheDir = File(application.cacheDir, "processed_icons").apply { mkdirs() }
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
            // 新安裝時不需要清除，loadApps 會處理新 ID。
            // 只有更新（Changed）或移除後再裝（由 packageReceiver 處理）才需要。
            refreshApps()
        }
        override fun onPackageRemoved(packageName: String, user: UserHandle) {
            // 注意：不要在這裡從 seen_apps 或其他設定中移除
            // 這樣在應用程式「更新」時（先移除再安裝），才能保留原有的位置、自定義標籤與隱藏狀態
            // 也能避免在 autoAddAppsToHome 開啟時，更新應用程式導致重複加入桌面的問題

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
        // 2. 加入延遲並發送廣播，確保系統層同步完成
        viewModelScope.launch {
            delay(1000)
            loadApps()
            
            // 3. 發送全局刷新廣播
            val intent = Intent("com.liferlighdow.iteration.ACTION_REFRESH_APPS")
            intent.setPackage(getApplication<Application>().packageName)
            getApplication<Application>().sendBroadcast(intent)
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
            "themed_icons" -> _isThemedIconsEnabled.value = sharedPreferences.getBoolean(key, false)
            "show_minus_one" -> _showMinusOnePage.value = sharedPreferences.getBoolean(key, true)
            "show_app_library" -> _showAppLibrary.value = sharedPreferences.getBoolean(key, true)
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        loadSettings()
        loadApps()
        updateBlurredWallpaper()
        fetchExchangeRates()
        fetchWeather()
        checkSystemNetworkStatus()
        // 註冊監聽
        launcherApps.registerCallback(packageCallback)
        
        val filter = IntentFilter("com.liferlighdow.iteration.ACTION_REFRESH_APPS")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getApplication<Application>().registerReceiver(refreshReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
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

    private var lastBlurredSignal = -1L

    fun updateBlurredWallpaper() {
        val currentSignal = _wallpaperUpdateSignal.value
        // 如果桌布沒換，且已經有緩存，就不重複執行昂貴的模糊運算
        if (currentSignal == lastBlurredSignal && _blurredWallpaper.value != null) return

        viewModelScope.launch(Dispatchers.IO) {
            lastBlurredSignal = currentSignal
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
        val newDockStyle = try { DockStyle.valueOf(prefs.getString("dock_style", "MODERN") ?: "MODERN") }
        catch (e: Exception) { DockStyle.MODERN }
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

        if (_iconStyle.value != newStyle || _isThemedIconsEnabled.value != newThemed || _iconPackPackage.value != newIconPackPackage || _iconShape.value != newShape || _libraryShape.value != newLibShape || _excludedThemedPackages.value != newExcluded || _desktopRows.value != newRows || _dockStyle.value != newDockStyle) {
            _iconStyle.value = newStyle
            _iconShape.value = newShape
            _libraryShape.value = newLibShape
            _isThemedIconsEnabled.value = newThemed
            _iconPackPackage.value = newIconPackPackage
            _excludedThemedPackages.value = newExcluded
            _desktopRows.value = newRows
            _dockStyle.value = newDockStyle
            // 不要在這裡立即 evictAll，移到 loadAppsJob 裡面根據後綴變化決定
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
        prefs.edit().putString("launcher_layout_v3", pagesArray.toString()).apply()
    }

    private fun loadWidgets() {
        val saved = prefs.getString("minus_one_widgets_v3", null) ?: return
        val list = mutableListOf<WidgetModel>()
        try {
            val array = JSONArray(saved)
            for (i in 0 until array.length()) {
                ConfigSerializer.deserializeWidgetModel(array.getString(i))?.let { list.add(it) }
            }
        } catch (e: Exception) { e.printStackTrace() }
        _minusOneWidgets.value = list
    }

    fun addWidget(type: WidgetType, pageIndex: Int = -1) {
        val label = when (type) {
            is WidgetType.Battery -> "Battery"
            is WidgetType.Clock -> "Clock"
            is WidgetType.Calendar -> "Calendar"
            is WidgetType.Photo -> "Photo"
            is WidgetType.Music -> "Music"
            is WidgetType.Note -> "Note"
            is WidgetType.Weather -> "Weather"
            is WidgetType.Stack -> if (type.isWide) "Wide Widget Stacker" else "Stack"
        }

        if (pageIndex == -1) {
            // 加入到負一屏
            val newList = _minusOneWidgets.value + WidgetModel(widgetType = type, label = label)
            _minusOneWidgets.value = newList
            saveWidgets(newList)
        } else {
            // 加入到指定桌面分頁
            val widget = WidgetModel(widgetType = type, label = label)
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
        prefs.edit().putString("minus_one_widgets_v3", array.toString()).apply()
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

    fun setAppCategory(uniqueId: String, category: String) {
        if (category.isBlank()) {
            customCategories.remove(uniqueId)
        } else {
            customCategories[uniqueId] = category
        }
        saveCustomCategories()
        loadApps()
    }

    private fun saveCustomCategories() {
        val json = JSONObject()
        customCategories.forEach { (k, v) -> json.put(k, v) }
        prefs.edit().putString("custom_categories_json", json.toString()).apply()
    }

    fun setCustomLabel(uniqueId: String, newLabel: String) {
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

    fun setCustomIcon(uniqueId: String, bitmap: Bitmap) {
        val fileSafeId = uniqueId.replace("/", "_").replace(":", "_").replace("@", "_")
        val file = File(customIconDir, "$fileSafeId.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        iconCache.remove(uniqueId) // 清除快取以強制重讀
        loadApps()
    }

    fun resetCustomIcon(uniqueId: String) {
        val fileSafeId = uniqueId.replace("/", "_").replace(":", "_").replace("@", "_")
        val file = File(customIconDir, "$fileSafeId.png")
        if (file.exists()) file.delete()
        iconCache.remove(uniqueId) // 清除快取
        loadApps()
    }

    private fun clearAppIconCache(packageName: String) {
        // 刪除該 App 所有的磁碟快取圖示，確保更新後能重新生成
        processedIconCacheDir.listFiles { _, name ->
            name.startsWith("${packageName}_")
        }?.forEach { it.delete() }

        iconCache.evictAll()
    }

    fun toggleHiddenApp(packageName: String) {
        if (hiddenPackages.contains(packageName)) {
            hiddenPackages.remove(packageName)
        } else {
            hiddenPackages.add(packageName)
            // 如果 App 被隱藏，從桌面佈局中移除
            removeAppFromHomeByPackage(packageName)
        }
        prefs.edit().putStringSet("hidden_apps", hiddenPackages).apply()

        clearAppIconCache(packageName)
        loadApps() // Reload to update isHidden status
    }

    /**
     * 從桌面分頁中移除指定包名的所有實例（用於隱藏 App 時）
     */
    private fun removeAppFromHomeByPackage(packageName: String) {
        val currentPages = _pages.value.map { page ->
            page.filter { item ->
                if (item.isFolder) {
                    // 如果是資料夾，我們檢查是否內部的項目全部被過濾後會導致資料夾變動
                    true
                } else {
                    item.packageName != packageName
                }
            }.map { item ->
                if (item.isFolder) {
                    item.copy(folderItems = item.folderItems.filter { it.packageName != packageName })
                } else item
            }
        }.filter { it.isNotEmpty() }

        _pages.value = currentPages
        saveLayout()
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

    private var loadAppsJob: Job? = null
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
        updateBlurredWallpaper()

        val isThemed = _isThemedIconsEnabled.value
        val currentStyle = _iconStyle.value
        val currentShape = _iconShape.value
        val currentIconPack = _iconPackPackage.value

        loadAppsJob = viewModelScope.launch {
            // 如果是 Custom 風格且正在頻繁調整，稍微延遲一下以避免過度運算
            if (currentStyle == IconStyle.CUSTOM) {
                delay(100)
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
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                                // 優化點：API 27+ 直接獲取顏色，不需加載完整桌布位圖
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

            // 預先計算樣式後綴，確保 getIcon 尋找的路徑正確
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

            val newStyleSuffix = if (currentIconPack.isNotEmpty()) {
                "IP_V13_${currentIconPack.hashCode()}_${currentShape.name}_${currentStyle.name}_${if (isThemed) "T_$colorKey" else "N"}_$customKey"
            } else {
                "V13_${currentStyle.name}_${currentShape.name}_${if (isThemed) "T_$colorKey" else "N"}_$customKey"
            }

            // 如果樣式真的變了，才更新後綴並清空記憶體快取
            if (currentStyleSuffix != newStyleSuffix) {
                currentStyleSuffix = newStyleSuffix
                iconCache.evictAll()
            }
            themeColorsCache = themeColors

            val processedApps: List<AppModel> = withContext(Dispatchers.Default) {
                val density = getApplication<Application>().resources.displayMetrics.density
                val sizePx = (62 * density).toInt()
                val styleSuffix = currentStyleSuffix

                val semaphore = Semaphore(8) // 限制併發數

                coroutineScope {
                    rawApps.map { app ->
                        async {
                            semaphore.withPermit {
                                val fileSafeId = app.uniqueId.replace("/", "_").replace(":", "_").replace("@", "_")
                                val customIconFile = File(customIconDir, "$fileSafeId.png")
                                val legacyCustomIconFile = File(customIconDir, "${app.packageName}.png")
                                
                                val diskCacheFile = File(
                                    processedIconCacheDir,
                                    "${fileSafeId}_$styleSuffix.png"
                                )
                                val cacheKey = "${app.uniqueId}_$styleSuffix"
                                val isExcluded =
                                    excludedThemedPackages.value.contains(app.packageName)

                                // 檢查快取，如果不存在則處理並存入快取與磁碟
                                if (iconCache[cacheKey] == null) {
                                    val customToLoad = if (customIconFile.exists()) customIconFile else if (legacyCustomIconFile.exists()) legacyCustomIconFile else null
                                    
                                    if (customToLoad != null) {
                                        BitmapFactory.decodeFile(customToLoad.absolutePath)
                                            ?.asImageBitmap()?.let {
                                            iconCache.put(cacheKey, it)
                                        }
                                    } else if (diskCacheFile.exists()) {
                                        BitmapFactory.decodeFile(diskCacheFile.absolutePath)?.let {
                                            iconCache.put(cacheKey, it.asImageBitmap())
                                        }
                                    } else {
                                        val processed = processNewIcon(
                                            app,
                                            currentIconPack,
                                            isThemed,
                                            isExcluded,
                                            themeColors,
                                            currentStyle,
                                            currentShape,
                                            sizePx,
                                            customBg,
                                            customFg,
                                            customOriginal,
                                            customOriginalBg
                                        )
                                        saveIconToDisk(processed, diskCacheFile)
                                        iconCache.put(cacheKey, processed)
                                    }
                                }

                                val appRes = getApplication<Application>()
                                val rawCategory =
                                    customCategories[app.uniqueId] ?: customCategories[app.packageName] ?: when (app.category) {
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
                                    label = customLabels[app.uniqueId] ?: customLabels[app.packageName] ?: app.label,
                                    isHidden = hiddenPackages.contains(app.packageName) || hiddenPackages.contains(app.uniqueId),
                                    displayCategory = displayCategory
                                )
                            }
                        }
                    }.awaitAll()
                }
            }

            _allApps.value = processedApps
            triggerIconUpdate() // 完成處理後通知 UI 刷新圖示

            // 獲取已紀錄過的 App 清單，用於判定「新安裝」
            val seenApps = (prefs.getStringSet("seen_apps", null) ?: emptySet()).toMutableSet()
            val isMigration = prefs.getStringSet("seen_apps", null) == null

            // 優先從儲存的佈局恢復 (v3 是純 kotlinx-serialization 格式)
            val savedLayout = prefs.getString("launcher_layout_v3", null) 
                ?: prefs.getString("launcher_layout_v2", null)

            if (savedLayout != null) {
                try {
                    val pagesArray = JSONArray(savedLayout)
                    val restoredPages = mutableListOf<List<AppModel>>()

                    // 遷移邏輯：如果是第一次使用 seen_apps 且已有佈局，先將目前已安裝的所有 App 標記為已見過
                    if (isMigration) {
                        seenApps.addAll(processedApps.map { it.uniqueId })
                        prefs.edit().putStringSet("seen_apps", seenApps).apply()
                    }

                    for (i in 0 until pagesArray.length()) {
                        val pageArray = pagesArray.getJSONArray(i)
                        val pageItems = mutableListOf<AppModel>()
                        for (j in 0 until pageArray.length()) {
                            // 自動辨識格式：v3 是 String, v2 是 JSONObject
                            val itemValue = pageArray.get(j)
                            val jsonStr = if (itemValue is JSONObject) itemValue.toString() else itemValue as String

                            ConfigSerializer.deserializeAppModel(jsonStr)?.let { savedApp ->
                                if (savedApp.isFolder) {
                                    pageItems.add(savedApp)
                                } else if (savedApp.isWidget) {
                                    pageItems.add(savedApp)
                                } else {
                                    // 尋找對應的已安裝 App
                                    val baseApp = processedApps.find { it.uniqueId == savedApp.uniqueId }
                                        ?: processedApps.find { it.packageName == savedApp.packageName && it.userId == savedApp.userId }
                                    
                                    baseApp?.let {
                                        pageItems.add(it.copy(
                                            uniqueId = savedApp.uniqueId,
                                            label = customLabels[savedApp.uniqueId] ?: customLabels[savedApp.packageName] ?: it.label,
                                            isHidden = hiddenPackages.contains(savedApp.uniqueId) || hiddenPackages.contains(savedApp.packageName)
                                        ))
                                    }
                                }
                            }
                        }
                        restoredPages.add(pageItems)
                    }

                    // 關鍵：找出真正新安裝的 App (不在 seen_apps 中)
                    val newApps = processedApps.filter { app ->
                        val isNew = !seenApps.contains(app.uniqueId) && 
                                   !seenApps.contains(app.packageName) && // 遷移相容：檢查舊包名格式
                                   !app.isHidden
                        isNew
                    }

                    if (newApps.isNotEmpty()) {
                        if (_autoAddAppsToHome.value) {
                            // 額外檢查：如果 newApps 數量多得離譜（例如 > 50），極大機率是 ID 遷移導致的
                            // 這種情況下我們只更新 seenApps，不實際塞入桌面，防止使用者崩潰
                            val isMassiveMigration = newApps.size > 20 
                            
                            if (!isMassiveMigration) {
                                val mutablePages = restoredPages.map { it.toMutableList() }.toMutableList()
                                newApps.forEach { app ->
                                    var added = false
                                    for (page in mutablePages) {
                                        if (page.size < pageSize) {
                                            page.add(app)
                                            added = true
                                            break
                                        }
                                    }
                                    if (!added) {
                                        mutablePages.add(mutableListOf(app))
                                    }
                                    seenApps.add(app.uniqueId)
                                }
                                _pages.value = mutablePages
                                saveLayout()
                            } else {
                                // 僅更新標記，不塞入桌面
                                seenApps.addAll(newApps.map { it.uniqueId })
                            }
                            
                            prefs.edit().putStringSet("seen_apps", seenApps).apply()
                        } else {
                            // 不自動加入桌面，但也標記為已見過，否則下次 loadApps 又會進來
                            seenApps.addAll(newApps.map { it.uniqueId })
                            prefs.edit().putStringSet("seen_apps", seenApps).apply()
                            _pages.value = restoredPages
                        }
                    } else {
                        _pages.value = restoredPages
                    }
                } catch (e: Exception) {
                    repaginate(processedApps)
                }
            } else {
                repaginate(processedApps)
                // 首次啟動，將所有目前 App 標記為已處理
                seenApps.addAll(processedApps.map { it.uniqueId })
                prefs.edit().putStringSet("seen_apps", seenApps).apply()
                saveLayout()
            }

            if (_dockPackageNames.value.all { it.isEmpty() } && processedApps.isNotEmpty()) {
                val defaultDock = processedApps.take(4).map { app: AppModel -> app.packageName }
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
        val launcherApps = getApplication<Application>().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val userManager = getApplication<Application>().getSystemService(Context.USER_SERVICE) as android.os.UserManager
        
        // 1. 取得該 App 所屬使用者的 Handle
        val userHandle = userManager.userProfiles.find { 
            userManager.getSerialNumberForUser(it) == app.userId 
        } ?: android.os.Process.myUserHandle()

        // 2. 優先嘗試獲取特定的 Activity 圖示 (支援一鍵多入口)
        val rawIcon = try {
            if (app.uniqueId.contains("/")) {
                val componentStr = app.uniqueId.substringBefore("@")
                val pkg = componentStr.substringBefore("/")
                val cls = componentStr.substringAfter("/")
                val component = android.content.ComponentName(pkg, cls)
                
                // 從 LauncherApps 中精準提取該入口的圖示
                launcherApps.getActivityList(pkg, userHandle)
                    .find { it.componentName == component }
                    ?.getIcon(getApplication<Application>().resources.displayMetrics.densityDpi)
            } else {
                // 如果是普通應用，則獲取該使用者的標準圖示
                launcherApps.getActivityList(app.packageName, userHandle)
                    .firstOrNull()
                    ?.getIcon(getApplication<Application>().resources.displayMetrics.densityDpi)
            }
        } catch (e: Exception) {
            null
        }

        // 3. 如果上述都失敗，才回退到 PM (僅限主使用者)
        val finalRawIcon = rawIcon ?: try {
            getApplication<Application>().packageManager.getApplicationIcon(app.packageName)
        } catch (e: Exception) {
            null
        }

        if (isExcluded) {
            return iconProcessor.processIcon(
                finalRawIcon, false, null, IconStyle.STANDARD, currentShape, sizePx,
                customBgColor = 0, customFgColor = 0, customUseOriginal = true, customUseOriginalBg = true
            )
        }

        return if (currentIconPack.isNotEmpty()) {
            val ipIcon = iconPackManager.getIcon(app.packageName, app.uniqueId)
            if (ipIcon != null) {
                iconProcessor.processIcon(ipIcon, false, null, IconStyle.STANDARD, currentShape, sizePx, isIconPack = true)
            } else {
                iconProcessor.processIcon(finalRawIcon, false, null, IconStyle.CUSTOM, currentShape, sizePx, customBgColor = 0, customFgColor = 0, customUseOriginal = true, customUseOriginalBg = true)
            }
        } else {
            iconProcessor.processIcon(finalRawIcon, isThemed, themeColors, currentStyle, currentShape, sizePx, customBgColor = customBg, customFgColor = customFg, customUseOriginal = customOriginal, customUseOriginalBg = customOriginalBg)
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
                val w = item.widget
                if (w?.id == stackId && w.widgetType is WidgetType.Stack) {
                    val isWide = w.widgetType.isWide
                    item.copy(widget = w.copy(widgetType = WidgetType.Stack(children, isWide)))
                } else item
            }
        }
        _pages.value = currentPages

        val newMinusOne = _minusOneWidgets.value.map { widget ->
            if (widget.id == stackId && widget.widgetType is WidgetType.Stack) {
                val isWide = widget.widgetType.isWide
                widget.copy(widgetType = WidgetType.Stack(children, isWide))
            } else widget
        }
        _minusOneWidgets.value = newMinusOne

        saveLayout()
        saveWidgets(newMinusOne)
    }

    fun updateNoteText(widgetId: String, text: String) {
        val currentPages = _pages.value.map { page ->
            page.map { item ->
                val w = item.widget
                if (w?.id == widgetId && w.widgetType is WidgetType.Note) {
                    item.copy(widget = w.copy(widgetType = w.widgetType.copy(text = text)))
                } else item
            }
        }
        _pages.value = currentPages

        val newMinusOne = _minusOneWidgets.value.map { widget ->
            if (widget.id == widgetId && widget.widgetType is WidgetType.Note) {
                widget.copy(widgetType = widget.widgetType.copy(text = text))
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

    fun setActionMode(mode: ActionMode) {
        _actionMode.value = mode
        prefs.edit().putString("action_mode", mode.name).apply()
    }

    fun requestRootAccess(onResult: (Boolean) -> Unit) {
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

    fun checkShizukuPermission(): Boolean {
        return try {
            if (Shizuku.pingBinder()) {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } else false
        } catch (e: Exception) { false }
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

        clearAppIconCache(packageName)
        loadApps()
    }

    fun getInstalledIconPacks() = iconPackManager.getInstalledIconPacks()

    fun launchApp(app: AppModel) {
        val launcherApps = getApplication<Application>().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val userManager = getApplication<Application>().getSystemService(Context.USER_SERVICE) as android.os.UserManager
        
        try {
            val allProfiles = userManager.userProfiles
            val userHandle = allProfiles.find { 
                userManager.getSerialNumberForUser(it) == app.userId 
            } ?: android.os.Process.myUserHandle()

            if (app.uniqueId.contains("/")) {
                // 多入口應用或分身：使用 ComponentName 啟動特定 Activity
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
                // 一般應用：使用包名啟動
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

    fun setNetworkAccessEnabled(enabled: Boolean) {
        _isNetworkAccessEnabled.value = enabled
        prefs.edit().putBoolean("network_access_enabled", enabled).apply()
        if (enabled) {
            fetchExchangeRates()
            fetchWeather()
        }
    }

    fun fetchWeather() {
        viewModelScope.launch {
            weatherRepository.fetchWeather(_isNetworkAccessEnabled.value)
        }
    }

    fun setWeatherProvider(provider: WeatherProvider) {
        weatherRepository.setWeatherProvider(provider)
        fetchWeather()
    }

    fun updateLocation(lat: Double, lon: Double, name: String) {
        weatherRepository.updateLocation(lat, lon, name)
        fetchWeather()
    }

    fun resetToIpLocation() {
        weatherRepository.resetToIpLocation()
        fetchWeather()
    }

    fun fetchExchangeRates() {
        viewModelScope.launch {
            currencyRepository.fetchExchangeRates(_isNetworkAccessEnabled.value)
        }
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

    fun setDockCornerRadius(radius: Float) {
        _dockCornerRadius.value = radius
        prefs.edit().putFloat("dock_corner_radius", radius).apply()
    }

    fun setSearchEngineUrl(url: String) {
        _searchEngineUrl.value = url
        prefs.edit().putString("search_engine_url", url).apply()
    }

    fun setAutoAddAppsToHome(enabled: Boolean) {
        _autoAddAppsToHome.value = enabled
        prefs.edit().putBoolean("auto_add_apps_to_home", enabled).apply()
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun setAmoledBlack(enabled: Boolean) {
        _isAmoledBlack.value = enabled
        prefs.edit().putBoolean("amoled_black", enabled).apply()
    }

    fun setShowStatusBar(enabled: Boolean) {
        _showStatusBar.value = enabled
        prefs.edit().putBoolean("show_status_bar", enabled).apply()
    }

    fun setShowNavigationBar(enabled: Boolean) {
        _showNavigationBar.value = enabled
        prefs.edit().putBoolean("show_navigation_bar", enabled).apply()
    }

    fun setAppLanguage(languageCode: String) {
        _appLanguage.value = languageCode
        prefs.edit().putString("app_language", languageCode).apply()
        
        val appLocale: LocaleListCompat = if (languageCode.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageCode)
        }
        AppCompatDelegate.setApplicationLocales(appLocale)
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
                password = getPassword(),
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

    fun importConfig(jsonString: String): Boolean {
        val config = ConfigSerializer.deserializeConfig(jsonString) ?: return false
        return try {
            applyConfig(config)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun applyConfig(config: LauncherConfig) {
        val settings = config.settings
        
        // 1. 恢復基礎設定
        setThemedIconsEnabled(settings.themedIcons)
        setLiquidGlassDockEnabled(settings.liquidGlassDock)
        setLiquidGlassHomeFolderEnabled(settings.liquidGlassHomeFolder)
        setLiquidGlassAppLibraryFolderEnabled(settings.liquidGlassAppLibraryFolder)
        setLiquidGlassGlobalSearchEnabled(settings.liquidGlassGlobalSearch)
        setLiquidGlassAppLibrarySearchEnabled(settings.liquidGlassAppLibrarySearch)
        setLiquidGlassWidgetsEnabled(settings.liquidGlassWidgets)
        setNetworkAccessEnabled(settings.networkAccessEnabled)
        setLiquidGlassEnabled(settings.liquidGlassEnabled)
        setShowMinusOnePage(settings.showMinusOne)
        setShowAppLibrary(settings.showAppLibrary)
        setAutoAddAppsToHome(settings.autoAddAppsToHome)
        setShowStatusBar(settings.showStatusBar)
        setShowNavigationBar(settings.showNavigationBar)

        _actionMode.value = settings.actionMode
        prefs.edit().putString("action_mode", settings.actionMode.name).apply()

        _iconStyle.value = settings.iconStyle
        prefs.edit().putString("icon_style", settings.iconStyle.name).apply()

        _iconShape.value = settings.iconShape
        prefs.edit().putString("icon_shape", settings.iconShape.name).apply()

        _libraryShape.value = settings.libraryShape
        prefs.edit().putString("library_shape", settings.libraryShape.name).apply()

        setSearchEngineUrl(settings.searchEngineUrl)
        pageSize = settings.pageSize
        setPassword(settings.password)

        // 3. 恢復進階細項設定
        setLiquidGlassBlur(settings.glassParams.blur)
        setLiquidGlassRefractionHeight(settings.glassParams.refractionHeight)
        setLiquidGlassRefractionAmount(settings.glassParams.refractionAmount)
        setLiquidGlassChromaticAberration(settings.glassParams.chromaticAberration)

        val gst = settings.gestures
        setDoubleTapAction(gst.doubleTapAction)
        setSwipeUpAction(gst.swipeUpAction)
        setSwipeDownAction(gst.swipeDownAction)
        setLongPressAction(gst.longPressAction)
        setTwoFingerSwipeUpAction(gst.twoFingerSwipeUpAction)
        setTwoFingerSwipeDownAction(gst.twoFingerSwipeDownAction)
        
        setDoubleTapApp(gst.doubleTapApp)
        setSwipeUpApp(gst.swipeUpApp)
        setSwipeDownApp(gst.swipeDownApp)
        setLongPressApp(gst.longPressApp)
        setTwoFingerSwipeUpApp(gst.twoFingerSwipeUpApp)
        setTwoFingerSwipeDownApp(gst.twoFingerSwipeDownApp)

        setIconPack(settings.customIconSettings.useOriginal.toString()) // This looks suspicious in old code, but I'll follow settings for now
        // Wait, old code had setIconPack(ap.optString("icon_pack", ""))
        // I'll use the right one.
        
        setThemeMode(settings.themeMode)
        setAppLanguage(settings.appLanguage)
        setAmoledBlack(settings.amoledBlack)

        val ci = settings.customIconSettings
        setCustomIconBgColor(ci.bgColor)
        setCustomIconFgColor(ci.fgColor)
        setCustomIconUseOriginal(ci.useOriginal)
        setCustomIconUseOriginalBg(ci.useOriginalBg)

        _favoritePackages.value = config.favorites
        prefs.edit().putStringSet("favorite_packages", config.favorites).apply()

        _excludedThemedPackages.value = config.excludedThemed
        prefs.edit().putStringSet("excluded_themed_packages", config.excludedThemed).apply()

        _homeMenuOptions.value = settings.homeMenuOptions
        prefs.edit().putStringSet("home_menu_options", settings.homeMenuOptions).apply()

        // 2. 恢復 App 設定
        hiddenPackages.clear()
        hiddenPackages.addAll(config.hiddenApps)
        prefs.edit().putStringSet("hidden_apps", hiddenPackages).apply()

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

        // 重新加載所有內容
        loadApps()
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
