package com.liferlighdow.iteration

import android.app.Application
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
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
import androidx.compose.material3.dynamicLightColorScheme
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)
    private val prefs = application.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
    private val iconProcessor = IconProcessor(application)
    private val iconPackManager = IconPackManager(application)
    
    private val _blurredWallpaper = MutableStateFlow<ImageBitmap?>(null)
    val blurredWallpaper = _blurredWallpaper.asStateFlow()

    private val _rawWallpaper = MutableStateFlow<ImageBitmap?>(null)
    val rawWallpaper = _rawWallpaper.asStateFlow()

    private val _pages = MutableStateFlow<List<List<AppModel>>>(emptyList())
    val pages: StateFlow<List<List<AppModel>>> = _pages

    private val _allApps = MutableStateFlow<List<AppModel>>(emptyList())
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

    private val _iconPackPackage = MutableStateFlow(prefs.getString("icon_pack_package", "") ?: "")
    val iconPackPackage = _iconPackPackage.asStateFlow()

    private val _iconStyle = MutableStateFlow(
        try { IconStyle.valueOf(prefs.getString("icon_style", "STANDARD") ?: "STANDARD") }
        catch (e: Exception) { IconStyle.STANDARD }
    )
    val iconStyle = _iconStyle.asStateFlow()

    private val hiddenPackages = mutableSetOf<String>()
    private val customLabels = mutableMapOf<String, String>()
    private val customCategories = mutableMapOf<String, String>()
    private val categoryRenames = mutableMapOf<String, String>()
    private val _userCategories = MutableStateFlow<List<String>>(emptyList())
    val userCategories: StateFlow<List<String>> = _userCategories.asStateFlow()

    private val _suggestedApps = MutableStateFlow<List<AppModel>>(emptyList())
    val suggestedApps = _suggestedApps.asStateFlow()

    private val customIconDir = File(application.filesDir, "custom_icons").apply { mkdirs() }
    private val processedIconCacheDir = File(application.cacheDir, "processed_icons").apply { mkdirs() }
    private var pageSize = 20
    private var dragBackupPages: List<List<AppModel>>? = null

    init {
        loadSettings()
        loadApps()
        updateBlurredWallpaper()
    }

    fun updateBlurredWallpaper() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val wm = WallpaperManager.getInstance(getApplication())
                val drawable = wm.drawable ?: return@launch
                val rawBitmap = drawable.toBitmap()
                
                val dm = getApplication<Application>().resources.displayMetrics
                val screenW = dm.widthPixels
                val screenH = dm.heightPixels
                
                // 1. 精確裁剪
                val wallpaperAspectRatio = rawBitmap.width.toFloat() / rawBitmap.height
                val screenAspectRatio = screenW.toFloat() / screenH
                
                val cropW = if (wallpaperAspectRatio > screenAspectRatio) (rawBitmap.height * screenAspectRatio).toInt() else rawBitmap.width
                val cropH = if (wallpaperAspectRatio > screenAspectRatio) rawBitmap.height else (rawBitmap.width / screenAspectRatio).toInt()
                
                val cropped = Bitmap.createBitmap(rawBitmap, (rawBitmap.width - cropW) / 2, (rawBitmap.height - cropH) / 2, cropW, cropH)
                val scaled = Bitmap.createScaledBitmap(cropped, screenW, screenH, true)
                
                // 2. 【Liquid Glass 採樣優化】：保留高清晰度
                val blurScale = 0.5f
                val bw = (screenW * blurScale).toInt().coerceAtLeast(1)
                val bh = (screenH * blurScale).toInt().coerceAtLeast(1)
                
                val small = Bitmap.createScaledBitmap(scaled, bw, bh, true)
                val blurred = Bitmap.createScaledBitmap(small, screenW, screenH, true)
                
                _rawWallpaper.value = scaled.asImageBitmap()
                _blurredWallpaper.value = blurred.asImageBitmap()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
        val newThemed = prefs.getBoolean("themed_icons", false)
        val newLiquidEnabled = prefs.getBoolean("liquid_glass_enabled", false)
        val newLiquidDockEnabled = prefs.getBoolean("liquid_glass_dock", false)
        val newLiquidHomeFolderEnabled = prefs.getBoolean("liquid_glass_home_folder", false)
        val newLiquidAppLibraryFolderEnabled = prefs.getBoolean("liquid_glass_app_library_folder", false)
        val newLiquidGlobalSearchEnabled = prefs.getBoolean("liquid_glass_global_search", false)
        val newLiquidAppLibrarySearchEnabled = prefs.getBoolean("liquid_glass_app_library_search", false)
        val newIconPackPackage = prefs.getString("icon_pack_package", "") ?: ""

        if (_iconStyle.value != newStyle || _isThemedIconsEnabled.value != newThemed || _iconPackPackage.value != newIconPackPackage) {
            _iconStyle.value = newStyle
            _isThemedIconsEnabled.value = newThemed
            _iconPackPackage.value = newIconPackPackage
            iconCache.evictAll()
        }
        
        _isLiquidGlassEnabled.value = newLiquidEnabled
        _isLiquidGlassDockEnabled.value = newLiquidDockEnabled
        _isLiquidGlassHomeFolderEnabled.value = newLiquidHomeFolderEnabled
        _isLiquidGlassAppLibraryFolderEnabled.value = newLiquidAppLibraryFolderEnabled
        _isLiquidGlassGlobalSearchEnabled.value = newLiquidGlobalSearchEnabled
        _isLiquidGlassAppLibrarySearchEnabled.value = newLiquidAppLibrarySearchEnabled
    }

    private fun saveLayout() {
        val pagesArray = JSONArray()
        _pages.value.forEach { page ->
            val pageArray = JSONArray()
            page.forEach { item ->
                pageArray.put(serializeAppModel(item))
            }
            pagesArray.put(pageArray)
        }
        prefs.edit().putString("launcher_layout_v2", pagesArray.toString()).apply()
    }

    private fun serializeAppModel(item: AppModel): JSONObject {
        val obj = JSONObject()
        obj.put("type", if (item.isFolder) "folder" else if (item.isWidget) "widget" else "app")
        obj.put("id", item.uniqueId)
        obj.put("label", item.label)
        obj.put("pkg", item.packageName)
        
        if (item.isFolder) {
            val children = JSONArray()
            item.folderItems.forEach { children.put(serializeAppModel(it)) }
            obj.put("children", children)
        }
        
        item.widget?.let { w ->
            obj.put("widget", serializeWidgetModel(w))
        }
        return obj
    }

    private fun serializeWidgetModel(w: WidgetModel): JSONObject {
        val obj = JSONObject()
        obj.put("id", w.id)
        obj.put("label", w.label)
        obj.put("displayMode", w.displayMode.name)
        obj.put("type", when (w.type) {
            is WidgetType.Battery -> "Battery"
            is WidgetType.Clock -> "Clock"
            is WidgetType.Calendar -> "Calendar"
            is WidgetType.Photo -> "Photo"
            is WidgetType.Music -> "Music"
            is WidgetType.Stack -> "Stack"
        })
        if (w.type is WidgetType.Calendar) obj.put("is_wide", w.type.isWide)
        if (w.type is WidgetType.Photo) obj.put("is_wide", w.type.isWide)
        if (w.type is WidgetType.Music) obj.put("is_wide", w.type.isWide)
        if (w.type is WidgetType.Stack) {
            val children = JSONArray()
            w.type.children.forEach { children.put(serializeWidgetModel(it)) }
            obj.put("children", children)
        }
        return obj
    }

    private fun deserializeWidgetModel(obj: JSONObject): WidgetModel? {
        val id = obj.optString("id")
        val label = obj.optString("label")
        val mode = try { WidgetDisplayMode.valueOf(obj.optString("displayMode", "GLASS")) } catch (e: Exception) { WidgetDisplayMode.GLASS }
        val typeStr = obj.optString("type")
        val isWide = obj.optBoolean("is_wide", false)

        val type: WidgetType = when (typeStr) {
            "Battery" -> WidgetType.Battery
            "Clock" -> WidgetType.Clock
            "Calendar" -> WidgetType.Calendar(isWide)
            "Photo" -> WidgetType.Photo(isWide)
            "Music" -> WidgetType.Music(isWide)
            "Stack" -> {
                val children = mutableListOf<WidgetModel>()
                val childrenArr = obj.optJSONArray("children")
                if (childrenArr != null) {
                    for (i in 0 until childrenArr.length()) {
                        deserializeWidgetModel(childrenArr.getJSONObject(i))?.let { children.add(it) }
                    }
                }
                WidgetType.Stack(children)
            }
            else -> return null
        }
        return WidgetModel(id = id, type = type, label = label, displayMode = mode)
    }

    private fun deserializeAppModel(obj: JSONObject, allInstalled: List<AppModel>): AppModel? {
        val type = obj.optString("type", "app")
        val pkg = obj.optString("pkg", "")
        
        return when (type) {
            "folder" -> {
                val children = mutableListOf<AppModel>()
                val childrenArray = obj.optJSONArray("children")
                if (childrenArray != null) {
                    for (i in 0 until childrenArray.length()) {
                        deserializeAppModel(childrenArray.getJSONObject(i), allInstalled)?.let { children.add(it) }
                    }
                }
                AppModel(
                    label = obj.optString("label", "Folder"),
                    isFolder = true,
                    uniqueId = obj.optString("id", "folder_${System.currentTimeMillis()}"),
                    folderItems = children
                )
            }
            "widget" -> {
                val widgetObj = obj.optJSONObject("widget")
                val widget = if (widgetObj != null) {
                    deserializeWidgetModel(widgetObj)
                } else {
                    // 相容舊格式
                    val wId = obj.optString("widget_id")
                    val wTypeStr = obj.optString("widget_type")
                    val wMode = try { WidgetDisplayMode.valueOf(obj.optString("widget_mode", "GLASS")) } catch(e: Exception) { WidgetDisplayMode.GLASS }
                    val isWide = obj.optBoolean("is_wide", false)
                    val wType: WidgetType? = when(wTypeStr) {
                        "Battery" -> WidgetType.Battery
                        "Clock" -> WidgetType.Clock
                        "Calendar" -> WidgetType.Calendar(isWide)
                        "Photo" -> WidgetType.Photo(isWide)
                        "Music" -> WidgetType.Music(isWide)
                        else -> null
                    }
                    if (wType == null) null
                    else WidgetModel(id = wId, type = wType, label = obj.optString("label"), displayMode = wMode)
                }
                
                if (widget == null) return null
                
                AppModel(
                    label = obj.optString("label"),
                    uniqueId = obj.optString("id"),
                    widget = widget
                )
            }
            else -> {
                val baseApp = allInstalled.find { it.packageName == pkg } ?: return null
                baseApp.copy(
                    uniqueId = obj.optString("id", pkg),
                    label = customLabels[pkg] ?: baseApp.label,
                    isHidden = hiddenPackages.contains(pkg)
                )
            }
        }
    }

    private fun loadWidgets() {
        val saved = prefs.getString("minus_one_widgets", null)
        if (saved != null) {
            val list = mutableListOf<WidgetModel>()
            val array = JSONArray(saved)
            for (i in 0 until array.length()) {
                deserializeWidgetModel(array.getJSONObject(i))?.let { list.add(it) }
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
            is WidgetType.Stack -> "Stack"
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
            array.put(serializeWidgetModel(widget))
        }
        prefs.edit().putString("minus_one_widgets", array.toString()).apply()
    }

    fun prepareForDrag() {
        dragBackupPages = _pages.value
    }

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

    private val iconCache = object : LruCache<String, ImageBitmap>(50) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = 1
    }

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
        // 1. 強制重新載入設定
        loadSettings()
        updateSuggestions()
        updateBlurredWallpaper() // 更新模糊桌布
        
        val isThemed = _isThemedIconsEnabled.value
        val currentStyle = _iconStyle.value
        val currentIconPack = _iconPackPackage.value

        viewModelScope.launch {
            if (currentIconPack.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    iconPackManager.loadIconPack(currentIconPack)
                }
            }

            val rawApps = withContext(Dispatchers.IO) {
                repository.getInstalledApps()
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
                            // V4: Icon Pack 縮放補償
                            val styleSuffix = if (currentIconPack.isNotEmpty()) {
                                "IP_V4_${currentIconPack.hashCode()}"
                            } else {
                                "V4_${currentStyle.name}_${if (isThemed) "T_$colorKey" else "N"}"
                            }
                            
                            val diskCacheFile = File(processedIconCacheDir, "${app.packageName}_$styleSuffix.png")
                            
                            val cacheKey = "${app.packageName}_$styleSuffix"
                            val cachedIcon = iconCache[cacheKey]

                            val processedIcon: ImageBitmap = if (customIconFile.exists()) {
                                BitmapFactory.decodeFile(customIconFile.absolutePath)?.asImageBitmap() 
                                    ?: iconProcessor.processIcon(app.icon, isThemed, themeColors, currentStyle, sizePx)
                            } else cachedIcon ?: if (diskCacheFile.exists()) {
                                val bitmap = BitmapFactory.decodeFile(diskCacheFile.absolutePath)
                                if (bitmap != null) {
                                    val imageBitmap = bitmap.asImageBitmap()
                                    iconCache.put(cacheKey, imageBitmap)
                                    imageBitmap
                                } else {
                                    val processed = if (currentIconPack.isNotEmpty()) {
                                        val ipIcon = iconPackManager.getIcon(app.packageName)
                                        if (ipIcon != null) {
                                            // Icon Pack 內的圖示：維持原始樣式，不套用主題色，且告知是 IconPack 以取消白底
                                            iconProcessor.processIcon(ipIcon, false, null, IconStyle.STANDARD, sizePx, isIconPack = true)
                                        } else {
                                            // Fallback App：套用使用者選定的 Iteration 樣式與主題
                                            iconProcessor.processIcon(app.icon, isThemed, themeColors, currentStyle, sizePx)
                                        }
                                    } else {
                                        iconProcessor.processIcon(app.icon, isThemed, themeColors, currentStyle, sizePx)
                                    }
                                    saveIconToDisk(processed, diskCacheFile)
                                    iconCache.put(cacheKey, processed)
                                    processed
                                }
                            } else {
                                val processedIconBitmap = if (currentIconPack.isNotEmpty()) {
                                    val ipIcon = iconPackManager.getIcon(app.packageName)
                                    if (ipIcon != null) {
                                        iconProcessor.processIcon(ipIcon, false, null, IconStyle.STANDARD, sizePx, isIconPack = true)
                                    } else {
                                        // Fallback App：套用使用者選定的 Iteration 樣式與主題
                                        iconProcessor.processIcon(app.icon, isThemed, themeColors, currentStyle, sizePx)
                                    }
                                } else {
                                    iconProcessor.processIcon(app.icon, isThemed, themeColors, currentStyle, sizePx)
                                }
                                saveIconToDisk(processedIconBitmap, diskCacheFile)
                                iconCache.put(cacheKey, processedIconBitmap)
                                processedIconBitmap
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
                            deserializeAppModel(pageArray.getJSONObject(j), processedApps)?.let { pageItems.add(it) }
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

    private fun saveIconToDisk(bitmap: ImageBitmap, file: File) {
        try {
            val b = bitmap.asAndroidBitmap()
            FileOutputStream(file).use { out ->
                b.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    /**
     * 處理 App 放置邏輯：包含推擠換位與資料夾建立
     */
    fun handleAppDrop(
        fromId: String,
        targetId: String?,
        targetPageIndex: Int,
        isFromLibrary: Boolean,
        dropType: DropType
    ) {
        // 使用備份作為基準，如果沒有備份則使用當前狀態
        val basePages = dragBackupPages ?: _pages.value
        val currentPages = basePages.map { it.toMutableList() }.toMutableList()
        dragBackupPages = null // 清除備份
        
        var movingItem: AppModel? = null

        // 1. 取得移動物件 (包含從資料夾內抓取)
        if (isFromLibrary) {
            val baseApp = _allApps.value.find { it.packageName == fromId }
            movingItem = baseApp?.copy(uniqueId = "${fromId}_${System.currentTimeMillis()}")
        } else {
            // 遞迴尋找：檢查頁面與資料夾
            outer@for (pIdx in currentPages.indices) {
                val page = currentPages[pIdx]
                // 檢查頂層
                val topIdx = page.indexOfFirst { it.uniqueId == fromId }
                if (topIdx != -1) {
                    movingItem = page.removeAt(topIdx)
                    break@outer
                }
                // 檢查資料夾內
                for (i in page.indices) {
                    val item = page[i]
                    if (item.isFolder) {
                        val fIdx = item.folderItems.indexOfFirst { it.uniqueId == fromId }
                        if (fIdx != -1) {
                            val mutableFolderItems = item.folderItems.toMutableList()
                            movingItem = mutableFolderItems.removeAt(fIdx)
                            // 更新資料夾
                            if (mutableFolderItems.isEmpty()) {
                                page.removeAt(i)
                            } else if (mutableFolderItems.size == 1) {
                                page[i] = mutableFolderItems[0] // 剩一個則拆解資料夾
                            } else {
                                page[i] = item.copy(folderItems = mutableFolderItems)
                            }
                            break@outer
                        }
                    }
                }
            }
        }
        
        val item = movingItem ?: return

        // 2. 處理落點 (其餘邏輯保持不變)
        when (dropType) {
            DropType.FOLDER -> {
                if (targetId != null) {
                    var success = false
                    for (page in currentPages) {
                        val tIdx = page.indexOfFirst { it.uniqueId == targetId }
                        if (tIdx != -1) {
                            val targetItem = page[tIdx]
                            if (targetItem.isFolder) {
                                page[tIdx] = targetItem.copy(folderItems = targetItem.folderItems + item)
                            } else {
                                page[tIdx] = AppModel(
                                    label = getApplication<Application>().getString(R.string.folder_default_name),
                                    isFolder = true,
                                    folderItems = listOf(targetItem, item),
                                    uniqueId = "folder_${System.currentTimeMillis()}"
                                )
                            }
                            success = true
                            break
                        }
                    }
                    if (!success) insertAtPage(currentPages, targetPageIndex, item)
                }
            }
            DropType.REORDER -> {
                if (targetId != null) {
                    var success = false
                    for (page in currentPages) {
                        val tIdx = page.indexOfFirst { it.uniqueId == targetId }
                        if (tIdx != -1) {
                            page.add(tIdx, item)
                            success = true
                            break
                        }
                    }
                    if (!success) insertAtPage(currentPages, targetPageIndex, item)
                } else {
                    insertAtPage(currentPages, targetPageIndex, item)
                }
            }
        }

        reorganizeAllPages(currentPages)
    }

    fun addAppToHome(packageName: String) {
        val baseApp = _allApps.value.find { it.packageName == packageName } ?: return
        val newItem = baseApp.copy(uniqueId = "${packageName}_${System.currentTimeMillis()}")
        
        val currentPages = _pages.value.map { it.toMutableList() }.toMutableList()
        // 放到最後一頁，reorganizeAllPages 會自動處理分頁
        if (currentPages.isEmpty()) {
            currentPages.add(mutableListOf(newItem))
        } else {
            currentPages.last().add(newItem)
        }
        reorganizeAllPages(currentPages)
    }


    private fun insertAtPage(pages: MutableList<MutableList<AppModel>>, index: Int, item: AppModel) {
        val safeIndex = index.coerceIn(0, pages.size)
        if (safeIndex < pages.size) {
            pages[safeIndex].add(item)
        } else {
            pages.add(mutableListOf(item))
        }
    }

    private fun calculateUsedSlots(items: List<AppModel>): Int {
        return items.sumOf { item ->
            if (item.isWidget) {
                when (val type = item.widget?.type) {
                    is WidgetType.Battery -> 4
                    is WidgetType.Clock -> 4
                    is WidgetType.Calendar -> if (type.isWide) 8 else 4
                    is WidgetType.Photo -> if (type.isWide) 8 else 4
                    is WidgetType.Music -> if (type.isWide) 8 else 4
                    is WidgetType.Stack -> 4
                    else -> 1
                }
            } else 1
        }
    }

    fun addEmptyPage() {
        val currentPages = _pages.value.map { it.toMutableList() }.toMutableList()
        currentPages.add(mutableListOf())
        _pages.value = currentPages
        saveLayout()
    }

    fun deletePage(index: Int) {
        val currentPages = _pages.value.map { it.toMutableList() }.toMutableList()
        if (index in currentPages.indices) {
            currentPages.removeAt(index)
            // 刪除後進行重整，確保不會留下連續空白頁
            reorganizeAllPages(currentPages)
        }
    }

    private fun reorganizeAllPages(pages: MutableList<MutableList<AppModel>>) {
        val result = mutableListOf<MutableList<AppModel>>()
        var overflowItems = mutableListOf<AppModel>()

        // 逐頁處理，保留原始分頁結構，僅處理溢出部分
        for (page in pages) {
            val combinedItems = (overflowItems + page).toMutableList()
            overflowItems = mutableListOf()
            
            val pageItems = mutableListOf<AppModel>()
            var used = 0
            
            for (item in combinedItems) {
                val itemSlots = calculateUsedSlots(listOf(item))
                if (used + itemSlots <= pageSize) {
                    pageItems.add(item)
                    used += itemSlots
                } else {
                    overflowItems.add(item)
                }
            }
            result.add(pageItems)
        }

        // 處理剩餘的溢出物件，放入新頁面
        while (overflowItems.isNotEmpty()) {
            val pageItems = mutableListOf<AppModel>()
            var used = 0
            val iterator = overflowItems.iterator()
            while (iterator.hasNext()) {
                val item = iterator.next()
                val itemSlots = calculateUsedSlots(listOf(item))
                if (used + itemSlots <= pageSize) {
                    pageItems.add(item)
                    used += itemSlots
                    iterator.remove()
                } else if (pageItems.isEmpty()) {
                    // 單個物件太大，強行放入
                    pageItems.add(item)
                    iterator.remove()
                    break
                } else {
                    break
                }
            }
            result.add(pageItems)
        }

        // 至少保留一頁
        if (result.isEmpty()) result.add(mutableListOf())

        _pages.value = result
        saveLayout()
    }

    enum class DropType { REORDER, FOLDER }

    fun updateFolderName(folderId: String, newName: String) {
        val currentPages = _pages.value.map { page ->
            page.map { item ->
                if (item.uniqueId == folderId) item.copy(label = newName) else item
            }
        }
        _pages.value = currentPages
        saveLayout()
    }

    fun updateStackChildren(stackId: String, children: List<WidgetModel>) {
        val currentPages = _pages.value.map { page ->
            page.map { item ->
                if (item.widget?.id == stackId) {
                    item.copy(widget = item.widget.copy(type = WidgetType.Stack(children)))
                } else item
            }
        }
        _pages.value = currentPages
        
        val newMinusOne = _minusOneWidgets.value.map { widget ->
            if (widget.id == stackId) {
                widget.copy(type = WidgetType.Stack(children))
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

    fun setIconPack(packageName: String) {
        _iconPackPackage.value = packageName
        prefs.edit().putString("icon_pack_package", packageName).apply()
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

    fun createFolder(pageIndex: Int, folderName: String) {
        val currentPages = _pages.value.map { it.toMutableList() }.toMutableList()
        val targetPage = if (pageIndex >= 0 && pageIndex < currentPages.size) {
            currentPages[pageIndex]
        } else if (currentPages.isNotEmpty()) {
            currentPages.last()
        } else {
            mutableListOf<AppModel>().also { currentPages.add(it) }
        }
        
        val newFolder = AppModel(
            label = folderName.ifBlank { getApplication<Application>().getString(R.string.folder_default_name) },
            isFolder = true,
            uniqueId = "folder_${System.currentTimeMillis()}"
        )
        targetPage.add(newFolder)
        reorganizeAllPages(currentPages)
    }

    fun deleteFolder(folderId: String, keepIcons: Boolean = true) {
        val currentPages = _pages.value.map { it.toMutableList() }.toMutableList()
        outer@for (page in currentPages) {
            val idx = page.indexOfFirst { it.uniqueId == folderId }
            if (idx != -1) {
                val folder = page.removeAt(idx)
                if (keepIcons) {
                    // 將資料夾內的 App 釋放回桌面
                    page.addAll(idx, folder.folderItems)
                }
                break@outer
            }
        }
        reorganizeAllPages(currentPages)
    }

    fun addAppsToFolder(folderId: String, packageNames: List<String>) {
        val currentPages = _pages.value.map { page ->
            page.map { item ->
                if (item.uniqueId == folderId) {
                    val updatedItems = item.folderItems.toMutableList()
                    packageNames.forEach { pkg ->
                        val appToAdd = _allApps.value.find { it.packageName == pkg }
                        if (appToAdd != null) {
                            updatedItems.add(appToAdd.copy(uniqueId = "${pkg}_${System.currentTimeMillis()}"))
                        }
                    }
                    item.copy(folderItems = updatedItems)
                } else item
            }
        }
        _pages.value = currentPages.map { it.toMutableList() }
        saveLayout()
    }

    fun removeAppFromHome(uniqueId: String) {
        val currentPages = _pages.value.map { it.toMutableList() }.toMutableList()
        var removed = false
        for (page in currentPages) {
            val index = page.indexOfFirst { it.uniqueId == uniqueId }
            if (index != -1) {
                page.removeAt(index)
                removed = true
                break
            }
        }
        if (removed) {
            reorganizeAllPages(currentPages)
        }
    }

    fun removeAppFromFolder(folderId: String, appUniqueId: String) {
        val currentPages = _pages.value.map { it.toMutableList() }.toMutableList()
        outer@for (page in currentPages) {
            for (i in page.indices) {
                val item = page[i]
                if (item.isFolder && item.uniqueId == folderId) {
                    val updatedItems = item.folderItems.toMutableList()
                    val removedIdx = updatedItems.indexOfFirst { it.uniqueId == appUniqueId }
                    if (removedIdx != -1) {
                        updatedItems.removeAt(removedIdx)
                        if (updatedItems.isEmpty()) {
                            page.removeAt(i)
                        } else if (updatedItems.size == 1) {
                            page[i] = updatedItems[0] // 剩下一個則拆解資料夾
                        } else {
                            page[i] = item.copy(folderItems = updatedItems)
                        }
                        break@outer
                    }
                }
            }
        }
        reorganizeAllPages(currentPages)
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
        val root = JSONObject()
        val settings = JSONObject()

        // 1. 基礎設定
        settings.put("themed_icons", _isThemedIconsEnabled.value)
        settings.put("liquid_glass_dock", _isLiquidGlassDockEnabled.value)
        settings.put("liquid_glass_home_folder", _isLiquidGlassHomeFolderEnabled.value)
        settings.put("liquid_glass_app_library_folder", _isLiquidGlassAppLibraryFolderEnabled.value)
        settings.put("liquid_glass_global_search", _isLiquidGlassGlobalSearchEnabled.value)
        settings.put("liquid_glass_app_library_search", _isLiquidGlassAppLibrarySearchEnabled.value)
        settings.put("liquid_glass_enabled", _isLiquidGlassEnabled.value)
        settings.put("icon_style", _iconStyle.value.name)
        settings.put("page_size", pageSize)
        settings.put("hidden_password", getPassword())

        // 2. App 相關
        val hidden = JSONArray()
        hiddenPackages.forEach { hidden.put(it) }
        root.put("hidden_apps", hidden)

        val labels = JSONObject()
        customLabels.forEach { (k, v) -> labels.put(k, v) }
        root.put("custom_labels", labels)

        val categories = JSONObject()
        customCategories.forEach { (k, v) -> categories.put(k, v) }
        root.put("custom_categories", categories)

        val userCats = JSONArray()
        _userCategories.value.forEach { userCats.put(it) }
        root.put("user_categories", userCats)

        val renames = JSONObject()
        categoryRenames.forEach { (k, v) -> renames.put(k, v) }
        root.put("category_renames", renames)
        
        // 3. 佈局
        val pagesArray = JSONArray()
        _pages.value.forEach { page ->
            val pageArray = JSONArray()
            page.forEach { pageArray.put(serializeAppModel(it)) }
            pagesArray.put(pageArray)
        }
        root.put("layout", pagesArray)
        
        // 4. 負一屏小工具
        val minusOne = JSONArray()
        _minusOneWidgets.value.forEach { widget ->
            minusOne.put(serializeWidgetModel(widget))
        }
        root.put("minus_one_widgets", minusOne)

        // 5. Dock & Stats
        root.put("dock", JSONArray(_dockPackageNames.value))
        root.put("launch_counts", prefs.getString("launch_counts", ""))

        root.put("settings", settings)
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())

        return root.toString(4)
    }

    fun importConfig(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            val settings = root.getJSONObject("settings")

            // 恢復基礎設定
            setThemedIconsEnabled(settings.optBoolean("themed_icons", false))
            setLiquidGlassDockEnabled(settings.optBoolean("liquid_glass_dock", false))
            setLiquidGlassHomeFolderEnabled(settings.optBoolean("liquid_glass_home_folder", false))
            setLiquidGlassAppLibraryFolderEnabled(settings.optBoolean("liquid_glass_app_library_folder", false))
            setLiquidGlassGlobalSearchEnabled(settings.optBoolean("liquid_glass_global_search", false))
            setLiquidGlassAppLibrarySearchEnabled(settings.optBoolean("liquid_glass_app_library_search", false))
            setLiquidGlassEnabled(settings.optBoolean("liquid_glass_enabled", false))
            val savedStyle = settings.optString("icon_style", "STANDARD")
            _iconStyle.value = try { IconStyle.valueOf(savedStyle) } catch(e: Exception) { IconStyle.STANDARD }
            prefs.edit().putString("icon_style", _iconStyle.value.name).apply()

            pageSize = settings.optInt("page_size", 20)
            setPassword(settings.optString("hidden_password", "1234"))
            
            // 恢復 App 設定
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
            
            // 恢復負一屏小工具
            val minusOne = mutableListOf<WidgetModel>()
            val minusOneArray = root.getJSONArray("minus_one_widgets")
            for (i in 0 until minusOneArray.length()) {
                deserializeWidgetModel(minusOneArray.getJSONObject(i))?.let { minusOne.add(it) }
            }
            _minusOneWidgets.value = minusOne
            saveWidgets(minusOne)
            
            // 恢復佈局 (需要等待 allApps 加載完成，所以這裡直接存入 prefs)
            val layout = root.getJSONArray("layout")
            prefs.edit().putString("launcher_layout_v2", layout.toString()).apply()

            // 恢復 Dock & Stats
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
            .take(4)
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
