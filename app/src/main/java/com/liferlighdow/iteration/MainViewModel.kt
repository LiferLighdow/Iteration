package com.liferlighdow.iteration

import android.app.Application
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.core.graphics.drawable.toBitmap
import java.io.File
import java.io.FileOutputStream
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material3.dynamicLightColorScheme
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
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

    fun addWidget(type: WidgetType) {
        val label = when (type) {
            is WidgetType.Battery -> "Battery"
            is WidgetType.Clock -> "Clock"
            is WidgetType.Calendar -> "Calendar"
            is WidgetType.Photo -> "Photo"
        }
        val newList = _minusOneWidgets.value + WidgetModel(type = type, label = label)
        _minusOneWidgets.value = newList
        saveWidgets(newList)
    }

    fun removeWidget(id: String) {
        val newList = _minusOneWidgets.value.filter { it.id != id }
        _minusOneWidgets.value = newList
        saveWidgets(newList)
    }

    fun updateWidgetDisplayMode(id: String, mode: WidgetDisplayMode) {
        val newList = _minusOneWidgets.value.map {
            if (it.id == id) it.copy(displayMode = mode) else it
        }
        _minusOneWidgets.value = newList
        saveWidgets(newList)
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
        val saved = prefs.getString("custom_labels", null)
        customLabels.clear()
        saved?.split(",")?.forEach { 
            val parts = it.split("|")
            if (parts.size == 2) {
                customLabels[parts[0]] = parts[1]
            }
        }
    }

    private fun loadUserCategories() {
        val saved = prefs.getString("user_categories_ordered", "Games,Social,Work,Media,Tools")
        _userCategories.value = saved?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    private fun loadCustomCategories() {
        val saved = prefs.getString("custom_categories", null)
        customCategories.clear()
        saved?.split(",")?.forEach { 
            val parts = it.split("|")
            if (parts.size == 2) {
                customCategories[parts[0]] = parts[1]
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
        val serialized = customCategories.map { "${it.key}|${it.value}" }.joinToString(",")
        prefs.edit().putString("custom_categories", serialized).apply()
    }

    fun setCustomLabel(packageName: String, newLabel: String) {
        if (newLabel.isBlank()) {
            customLabels.remove(packageName)
        } else {
            customLabels[packageName] = newLabel
        }
        val serialized = customLabels.map { "${it.key}|${it.value}" }.joinToString(",")
        prefs.edit().putString("custom_labels", serialized).apply()
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
        }
        prefs.edit().putStringSet("hidden_apps", hiddenPackages).apply()
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

    private val iconCache = mutableMapOf<String, ImageBitmap>()

    fun loadApps() {
        // 先從儲存空間重新讀取所有設定，確保從 SettingsActivity 返回後抓到最新變更
        loadHiddenPackages()
        loadCustomLabels()
        loadUserCategories()
        loadCustomCategories()
        loadWidgets()
        updateSuggestions()
        
        val latestThemedPref = prefs.getBoolean("themed_icons", false)
        if (_isThemedIconsEnabled.value != latestThemedPref) {
            _isThemedIconsEnabled.value = latestThemedPref
            iconCache.clear() // 開關狀態改變，必須清空快取
        }

        viewModelScope.launch {
            val rawApps = withContext(Dispatchers.IO) {
                repository.getInstalledApps()
            }
            
            val isThemed = _isThemedIconsEnabled.value
            
            // 優先嘗試獲取 Android 12+ 的官方調色盤作為染色基準
            val themeColors = if (isThemed) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Android 12+ 模擬 Compose 的 LightColorScheme 作為 Icon 生成基準 (避免 Icon 太黑)
                    dynamicLightColorScheme(getApplication<Application>())
                } else {
                    // Android 12 以下，維持原有的桌布提取邏輯
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
                
                rawApps.map { app ->
                    val customIconFile = File(customIconDir, "${app.packageName}.png")
                    val cachedIcon = iconCache[app.packageName]
                    
                    val processedIcon: ImageBitmap = if (customIconFile.exists()) {
                        BitmapFactory.decodeFile(customIconFile.absolutePath).asImageBitmap()
                    } else if (cachedIcon != null && !isThemed) {
                        cachedIcon
                    } else {
                        val b = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(b)
                        val icon = app.icon
                        
                        if (icon != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && icon is AdaptiveIconDrawable) {
                                val scale = 1.4f
                                val scaledSize = (sizePx * scale).toInt()
                                val offset = (sizePx - scaledSize) / 2
                                
                                if (isThemed && themeColors != null) {
                                    // 1. 繪製背景 (使用 Primary)
                                    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                                    val bgCol = themeColors.primary
                                    bgPaint.color = Color.argb(
                                        (bgCol.alpha * 255).toInt(),
                                        (bgCol.red * 255).toInt(),
                                        (bgCol.green * 255).toInt(),
                                        (bgCol.blue * 255).toInt()
                                    )
                                    canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), bgPaint)

                                    // 2. 染色並繪製前景 (使用 OnPrimary)
                                    val fgCol = themeColors.onPrimary
                                    val iconColor = Color.argb(
                                        (fgCol.alpha * 255).toInt(),
                                        (fgCol.red * 255).toInt(),
                                        (fgCol.green * 255).toInt(),
                                        (fgCol.blue * 255).toInt()
                                    )
                                    
                                    val cm = ColorMatrix(floatArrayOf(
                                        0f, 0f, 0f, 0f, Color.red(iconColor).toFloat(),
                                        0f, 0f, 0f, 0f, Color.green(iconColor).toFloat(),
                                        0f, 0f, 0f, 0f, Color.blue(iconColor).toFloat(),
                                        0f, 0f, 0f, 1f, 0f
                                    ))
                                    
                                    val filter = ColorMatrixColorFilter(cm)

                                    // 優先使用 Android 13+ 的 Monochrome (單色) 圖層
                                    var drawn = false
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        icon.monochrome?.let { mono ->
                                            mono.colorFilter = filter
                                            mono.setBounds(offset, offset, offset + scaledSize, offset + scaledSize)
                                            mono.draw(canvas)
                                            mono.colorFilter = null
                                            drawn = true
                                        }
                                    }

                                    // 如果沒有單色圖層，則回退到前景圖層染色 (雖然效果可能不如單色圖層完美)
                                    if (!drawn) {
                                        icon.foreground?.let {
                                            it.colorFilter = filter
                                            it.setBounds(offset, offset, offset + scaledSize, offset + scaledSize)
                                            it.draw(canvas)
                                            it.colorFilter = null
                                        }
                                    }
                                } else {
                                    icon.background?.setBounds(offset, offset, offset + scaledSize, offset + scaledSize)
                                    icon.background?.draw(canvas)
                                    icon.foreground?.setBounds(offset, offset, offset + scaledSize, offset + scaledSize)
                                    icon.foreground?.draw(canvas)
                                }
                            } else {
                                if (isThemed && themeColors != null) {
                                    val fgCol = themeColors.primary
                                    val iconColor = Color.argb((fgCol.alpha * 255).toInt(), (fgCol.red * 255).toInt(), (fgCol.green * 255).toInt(), (fgCol.blue * 255).toInt())
                                    icon.setTint(iconColor)
                                }
                                icon.setBounds(0, 0, sizePx, sizePx)
                                icon.draw(canvas)
                            }
                        }
                        val ib = b.asImageBitmap()
                        if (!isThemed) iconCache[app.packageName] = ib
                        ib
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
            }
            
            _allApps.value = processedApps
            repaginate(processedApps)
            
            if (_dockPackageNames.value.isEmpty() && processedApps.isNotEmpty()) {
                _dockPackageNames.value = processedApps.take(4).map { it.packageName }
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

    private fun reorganizeAllPages(pages: MutableList<MutableList<AppModel>>) {
        val result = mutableListOf<MutableList<AppModel>>()
        var overflow = mutableListOf<AppModel>()
        
        for (page in pages) {
            val current = (page + overflow).toMutableList()
            if (current.size > pageSize) {
                result.add(current.take(pageSize).toMutableList())
                overflow = current.drop(pageSize).toMutableList()
            } else {
                // 保留分頁結構，不強制從後面的分頁往前遞補
                if (current.isNotEmpty() || result.isEmpty()) {
                    result.add(current)
                }
                overflow = mutableListOf()
            }
        }
        
        // 處理剩餘的溢出物件，建立新分頁
        while (overflow.isNotEmpty()) {
            result.add(overflow.take(pageSize).toMutableList())
            overflow = overflow.drop(pageSize).toMutableList()
        }
        
        // 移除結尾多餘的空分頁 (除了第一頁)
        while (result.size > 1 && result.last().isEmpty()) {
            result.removeAt(result.size - 1)
        }

        _pages.value = result
    }

    /**
     * 即時預覽推擠效果 (改為只用於紀錄意圖，不再修改 _pages)
     */
    fun previewPushAway(fromId: String, targetId: String?) {
        // 暫時留空或僅用於日誌，不再更新 _pages
    }


    enum class DropType { REORDER, FOLDER }

    fun updateFolderName(folderId: String, newName: String) {
        val currentPages = _pages.value.map { page ->
            page.map { item ->
                if (item.uniqueId == folderId) item.copy(label = newName) else item
            }
        }
        _pages.value = currentPages
    }


    fun updateDockApp(slotIndex: Int, packageName: String) {
        val current = _dockPackageNames.value.toMutableList()
        if (slotIndex in current.indices) {
            current[slotIndex] = packageName
            _dockPackageNames.value = current
        }
    }

    fun setEditMode(enabled: Boolean) {
        _isEditMode.value = enabled
    }

    fun setThemedIconsEnabled(enabled: Boolean) {
        _isThemedIconsEnabled.value = enabled
        prefs.edit().putBoolean("themed_icons", enabled).apply()
        iconCache.clear() // 強制清空快取以重新生成主題圖示
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
                    var updatedItems = item.folderItems.toMutableList()
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

    private fun updateSuggestions() {
        val launchCounts = prefs.getString("launch_counts", "") ?: ""
        val topPackages = launchCounts.split(",")
            .filter { it.contains("|") }
            .map { it.split("|") }
            .filter { it.size == 2 }
            .map { it[0] to (it[1].toIntOrNull() ?: 0) }
            .sortedByDescending { it.second }
            .take(4)
            .map { it.first }
        
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
