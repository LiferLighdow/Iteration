package com.liferlighdow.iteration

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import java.io.File
import java.io.FileOutputStream
import android.os.Build
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode = _isEditMode.asStateFlow()

    private val hiddenPackages = mutableStateSetOf<String>()
    private val customLabels = mutableMapOf<String, String>()
    private val customCategories = mutableMapOf<String, String>()
    private val _userCategories = MutableStateFlow<List<String>>(emptyList())
    val userCategories: StateFlow<List<String>> = _userCategories.asStateFlow()

    private val customIconDir = File(application.filesDir, "custom_icons").apply { mkdirs() }
    private var pageSize = 20
    private var dragBackupPages: List<List<AppModel>>? = null

    init {
        loadHiddenPackages()
        loadCustomLabels()
        loadUserCategories()
        loadCustomCategories()
        loadApps()
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
        val saved = prefs.getStringSet("user_categories", setOf("Games", "Social", "Work", "Media", "Tools")) ?: emptySet()
        _userCategories.value = saved.toList().sorted()
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
        val current = _userCategories.value.toMutableSet()
        current.add(name)
        _userCategories.value = current.toList().sorted()
        prefs.edit().putStringSet("user_categories", current).apply()
    }

    fun deleteUserCategory(name: String) {
        val current = _userCategories.value.toMutableSet()
        current.remove(name)
        _userCategories.value = current.toList().sorted()
        prefs.edit().putStringSet("user_categories", current).apply()
        
        // 清除該分類下的 App 設定
        val toRemove = customCategories.filter { it.value == name }.keys
        toRemove.forEach { customCategories.remove(it) }
        saveCustomCategories()
        loadApps()
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
        viewModelScope.launch {
            val rawApps = withContext(Dispatchers.IO) {
                repository.getInstalledApps()
            }
            
            val processedApps = withContext(Dispatchers.Default) {
                val density = getApplication<Application>().resources.displayMetrics.density
                val sizePx = (62 * density).toInt()
                
                rawApps.map { app ->
                    val customIconFile = File(customIconDir, "${app.packageName}.png")
                    
                    // 優先從快取或檔案讀取圖示
                    val cachedIcon = iconCache[app.packageName]
                    
                    val processedIcon: ImageBitmap = if (customIconFile.exists()) {
                        // 如果有自定義圖示，每次重新讀取
                        BitmapFactory.decodeFile(customIconFile.absolutePath).asImageBitmap()
                    } else if (cachedIcon != null) {
                        cachedIcon
                    } else {
                        // 否則生成新的
                        val b = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(b)
                        val icon = app.icon
                        
                        if (icon != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && icon is AdaptiveIconDrawable) {
                                val scale = 1.35f
                                val scaledSize = (sizePx * scale).toInt()
                                val offset = (sizePx - scaledSize) / 2
                                
                                // 背景與前景套用相同的縮放比例，確保它們看起來是一個整體
                                icon.background?.setBounds(offset, offset, offset + scaledSize, offset + scaledSize)
                                icon.background?.draw(canvas)

                                icon.foreground?.setBounds(offset, offset, offset + scaledSize, offset + scaledSize)
                                icon.foreground?.draw(canvas)
                            } else {
                                icon.setBounds(0, 0, sizePx, sizePx)
                                icon.draw(canvas)
                            }
                        }
                        val ib = b.asImageBitmap()
                        iconCache[app.packageName] = ib
                        ib
                    }
                    
                    val displayCategory = customCategories[app.packageName] ?: when (app.category) {
                        0 -> "Games"
                        1 -> "Audio"
                        2 -> "Video"
                        3 -> "Imaging"
                        4 -> "Social"
                        5 -> "News"
                        6 -> "Maps"
                        7 -> "Productivity"
                        else -> "Other"
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
                                    label = "Folder",
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
        val safeIndex = index.coerceIn(0, pages.size.coerceAtLeast(1) - 1)
        if (safeIndex < pages.size) {
            pages[safeIndex].add(item)
        } else {
            pages.add(mutableListOf(item))
        }
    }

    private fun reorganizeAllPages(pages: MutableList<MutableList<AppModel>>) {
        val allItems = pages.flatten()
        val newPages = allItems.chunked(pageSize).map { it.toMutableList() }.toMutableList()
        if (newPages.isEmpty()) newPages.add(mutableListOf())
        _pages.value = newPages
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

    fun createFolder(pageIndex: Int, folderName: String) {
        val currentPages = _pages.value.map { it.toMutableList() }.toMutableList()
        val targetPage = if (pageIndex < currentPages.size) currentPages[pageIndex] else mutableListOf<AppModel>().also { currentPages.add(it) }
        
        val newFolder = AppModel(
            label = folderName.ifBlank { "Folder" },
            isFolder = true,
            uniqueId = "folder_${System.currentTimeMillis()}"
        )
        targetPage.add(newFolder)
        _pages.value = currentPages
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
}
