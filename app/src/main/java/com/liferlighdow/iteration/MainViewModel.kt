package com.liferlighdow.iteration

import android.app.Application
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.AdaptiveIconDrawable
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import java.io.File
import java.io.FileOutputStream
import android.os.Build
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

    private val hiddenPackages = mutableSetOf<String>()
    private val customLabels = mutableMapOf<String, String>()
    private val customCategories = mutableMapOf<String, String>()
    private val _userCategories = MutableStateFlow<List<String>>(emptyList())
    val userCategories: StateFlow<List<String>> = _userCategories.asStateFlow()

    private val _suggestedApps = MutableStateFlow<List<AppModel>>(emptyList())
    val suggestedApps = _suggestedApps.asStateFlow()

    private val customIconDir = File(application.filesDir, "custom_icons").apply { mkdirs() }
    private val processedIconCacheDir = File(application.cacheDir, "processed_icons").apply { mkdirs() }
    private var pageSize = 20
    private var dragBackupPages: List<List<AppModel>>? = null

    init {
        loadHiddenPackages()
        loadCustomLabels()
        loadUserCategories()
        loadCustomCategories()
        loadWidgets()
        loadApps()
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
            obj.put("widget_id", w.id)
            obj.put("widget_type", when(w.type) {
                is WidgetType.Battery -> "Battery"
                is WidgetType.Clock -> "Clock"
                is WidgetType.Calendar -> "Calendar"
                is WidgetType.Photo -> "Photo"
            })
            obj.put("widget_mode", w.displayMode.name)
            if (w.type is WidgetType.Calendar) obj.put("is_wide", w.type.isWide)
            if (w.type is WidgetType.Photo) obj.put("is_wide", w.type.isWide)
        }
        return obj
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
                val wId = obj.optString("widget_id")
                val wTypeStr = obj.optString("widget_type")
                val wMode = try { WidgetDisplayMode.valueOf(obj.optString("widget_mode", "GLASS")) } catch(e: Exception) { WidgetDisplayMode.GLASS }
                val isWide = obj.optBoolean("is_wide", false)

                val wType = when(wTypeStr) {
                    "Battery" -> WidgetType.Battery
                    "Clock" -> WidgetType.Clock
                    "Calendar" -> WidgetType.Calendar(isWide)
                    "Photo" -> WidgetType.Photo(isWide)
                    else -> null
                } ?: return null
                
                AppModel(
                    label = obj.optString("label"),
                    uniqueId = obj.optString("id"),
                    widget = WidgetModel(id = wId, type = wType, label = obj.optString("label"), displayMode = wMode)
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
                val obj = array.getJSONObject(i)
                val typeStr = obj.getString("type")
                val type = when (typeStr) {
                    "Battery" -> WidgetType.Battery
                    "Clock" -> WidgetType.Clock
                    "Calendar" -> WidgetType.Calendar(obj.optBoolean("isWide", false))
                    "Photo" -> WidgetType.Photo(obj.optBoolean("isWide", false))
                    else -> null
                }
                if (type != null) {
                    list.add(WidgetModel(
                        id = obj.getString("id"),
                        type = type,
                        label = obj.getString("label"),
                        displayMode = try { WidgetDisplayMode.valueOf(obj.getString("displayMode")) } catch(e: Exception) { WidgetDisplayMode.GLASS }
                    ))
                }
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
            val obj = JSONObject()
            obj.put("id", widget.id)
            obj.put("label", widget.label)
            obj.put("displayMode", widget.displayMode.name)
            val typeStr = when (widget.type) {
                is WidgetType.Battery -> "Battery"
                is WidgetType.Clock -> "Clock"
                is WidgetType.Calendar -> {
                    obj.put("isWide", widget.type.isWide)
                    "Calendar"
                }
                is WidgetType.Photo -> {
                    obj.put("isWide", widget.type.isWide)
                    "Photo"
                }
            }
            obj.put("type", typeStr)
            array.put(obj)
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
        val saved = prefs.getString("user_categories_ordered", "Games,Social,Work,Media,Tools")
        _userCategories.value = saved?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
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

    fun getPassword(): String = prefs.getString("hidden_password", "1234") ?: "1234"

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
        if (saved != null) {
            _dockPackageNames.value = saved.split(",").filter { it.isNotBlank() }
        }
    }

    fun loadApps() {
        // 先從儲存空間重新讀取所有設定，確保從 SettingsActivity 返回後抓到最新變更
        loadHiddenPackages()
        loadCustomLabels()
        loadUserCategories()
        loadCustomCategories()
        loadWidgets()
        loadDock()
        updateSuggestions()
        
        val latestThemedPref = prefs.getBoolean("themed_icons", false)
        if (_isThemedIconsEnabled.value != latestThemedPref) {
            _isThemedIconsEnabled.value = latestThemedPref
            iconCache.evictAll() // 開關狀態改變，必須清空快取
            processedIconCacheDir.deleteRecursively()
            processedIconCacheDir.mkdirs()
        }

        viewModelScope.launch {
            val rawApps = withContext(Dispatchers.IO) {
                repository.getInstalledApps()
            }
            
            val isThemed = _isThemedIconsEnabled.value

            // 優先嘗試獲取 Android 12+ 的官方調色盤作為染色基準
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
                
                // 使用 coroutineScope 進行平行處理
                coroutineScope {
                    rawApps.map { app ->
                        async {
                            val customIconFile = File(customIconDir, "${app.packageName}.png")
                            val cachedIcon = iconCache[app.packageName]

                            // 嘗試從磁碟快取讀取 (如果不是自定義圖示)
                            val diskCacheFile = File(processedIconCacheDir, "${app.packageName}.png")
                            
                            val processedIcon: ImageBitmap = if (customIconFile.exists()) {
                                BitmapFactory.decodeFile(customIconFile.absolutePath).asImageBitmap()
                            } else cachedIcon ?: if (diskCacheFile.exists()) {
                                val bitmap = BitmapFactory.decodeFile(diskCacheFile.absolutePath).asImageBitmap()
                                iconCache.put(app.packageName, bitmap)
                                bitmap
                            } else {
                                val processedIconBitmap = iconProcessor.processIcon(app.icon, isThemed, themeColors, sizePx)

                                // 儲存到磁碟快取與記憶體快取
                                val b = processedIconBitmap.asAndroidBitmap()
                                FileOutputStream(diskCacheFile).use { out ->
                                    b.compress(Bitmap.CompressFormat.PNG, 100, out)
                                }
                                iconCache.put(app.packageName, processedIconBitmap)
                                processedIconBitmap
                            }
                            
                            val appRes = getApplication<Application>()
                            val displayCategory = customCategories[app.packageName] ?: when (app.category) {
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
            
            if (_dockPackageNames.value.isEmpty() && processedApps.isNotEmpty()) {
                val defaultDock = processedApps.take(4).map { it.packageName }
                _dockPackageNames.value = defaultDock
                prefs.edit().putString("dock_packages", defaultDock.joinToString(",")).apply()
            }
        }
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
                    is WidgetType.Battery -> 1 // 在 4 欄位佈局中佔 2 欄 = 1 個 2x2 單元
                    is WidgetType.Clock -> 1
                    is WidgetType.Calendar -> if (type.isWide) 2 else 1
                    is WidgetType.Photo -> if (type.isWide) 2 else 1
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
        val remainingItems = pages.flatten().toMutableList()

        while (remainingItems.isNotEmpty()) {
            val pageItems = mutableListOf<AppModel>()
            var used = 0
            val iterator = remainingItems.iterator()

            while (iterator.hasNext()) {
                val item = iterator.next()
                val itemSlots = calculateUsedSlots(listOf(item))
                if (used + itemSlots <= pageSize) {
                    pageItems.add(item)
                    used += itemSlots
                    iterator.remove()
                } else if (pageItems.isEmpty()) {
                    // 單個物件太大，強行放入避免無限迴圈
                    pageItems.add(item)
                    iterator.remove()
                    break
                } else {
                    // 這一頁塞不下了，停止這一頁的填充，讓剩餘物件去下一頁
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


    fun updateDockApp(slotIndex: Int, packageName: String) {
        val current = _dockPackageNames.value.toMutableList()
        if (slotIndex in current.indices) {
            current[slotIndex] = packageName
            _dockPackageNames.value = current
            prefs.edit().putString("dock_packages", current.joinToString(",")).apply()
        }
    }

    fun setEditMode(enabled: Boolean) {
        _isEditMode.value = enabled
    }

    fun setThemedIconsEnabled(enabled: Boolean) {
        _isThemedIconsEnabled.value = enabled
        prefs.edit().putBoolean("themed_icons", enabled).apply()
        iconCache.evictAll() // 強制清空快取以重新生成主題圖示
        loadApps()
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

    fun deleteFolder(folderId: String) {
        val currentPages = _pages.value.map { it.toMutableList() }.toMutableList()
        outer@for (page in currentPages) {
            val idx = page.indexOfFirst { it.uniqueId == folderId }
            if (idx != -1) {
                val folder = page.removeAt(idx)
                // 將資料夾內的 App 釋放回桌面 (可選)
                page.addAll(idx, folder.folderItems)
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
            val obj = JSONObject()
            obj.put("id", widget.id)
            obj.put("label", widget.label)
            obj.put("displayMode", widget.displayMode.name)
            obj.put("type", when(widget.type) {
                is WidgetType.Battery -> "Battery"
                is WidgetType.Clock -> "Clock"
                is WidgetType.Calendar -> "Calendar"
                is WidgetType.Photo -> "Photo"
            })
            if (widget.type is WidgetType.Calendar) obj.put("is_wide", widget.type.isWide)
            if (widget.type is WidgetType.Photo) obj.put("is_wide", widget.type.isWide)
            minusOne.put(obj)
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
            
            // 恢復負一屏小工具
            val minusOne = mutableListOf<WidgetModel>()
            val minusOneArray = root.getJSONArray("minus_one_widgets")
            for (i in 0 until minusOneArray.length()) {
                val obj = minusOneArray.getJSONObject(i)
                val typeStr = obj.getString("type")
                val isWide = obj.optBoolean("is_wide", false)
                val type = when(typeStr) {
                    "Battery" -> WidgetType.Battery
                    "Clock" -> WidgetType.Clock
                    "Calendar" -> WidgetType.Calendar(isWide)
                    "Photo" -> WidgetType.Photo(isWide)
                    else -> null
                }
                if (type != null) {
                    minusOne.add(WidgetModel(
                        id = obj.getString("id"),
                        type = type,
                        label = obj.getString("label"),
                        displayMode = WidgetDisplayMode.valueOf(obj.getString("displayMode"))
                    ))
                }
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
