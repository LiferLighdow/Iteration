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

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)
    private val prefs = application.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
    
    private val _pages = MutableStateFlow<List<List<AppModel>>>(emptyList())
    val pages: StateFlow<List<List<AppModel>>> = _pages

    private val _allApps = MutableStateFlow<List<AppModel>>(emptyList())
    val allApps: StateFlow<List<AppModel>> = _allApps

    private val _dockPackageNames = MutableStateFlow(listOf<String>())
    val dockPackageNames = _dockPackageNames.asStateFlow()

    private val hiddenPackages = mutableStateSetOf<String>()
    private val customLabels = mutableMapOf<String, String>()
    private val customIconDir = File(application.filesDir, "custom_icons").apply { mkdirs() }
    private var pageSize = 20

    init {
        loadHiddenPackages()
        loadCustomLabels()
        loadApps()
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
                        
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && icon is AdaptiveIconDrawable) {
                            icon.background.setBounds(0, 0, sizePx, sizePx)
                            icon.background.draw(canvas)
                            val scale = 1.35f
                            val scaledSize = (sizePx * scale).toInt()
                            val offset = (sizePx - scaledSize) / 2
                            icon.foreground.setBounds(offset, offset, offset + scaledSize, offset + scaledSize)
                            icon.foreground.draw(canvas)
                        } else {
                            icon.setBounds(0, 0, sizePx, sizePx)
                            icon.draw(canvas)
                        }
                        val ib = b.asImageBitmap()
                        iconCache[app.packageName] = ib
                        ib
                    }
                    
                    app.copy(
                        label = customLabels[app.packageName] ?: app.label,
                        processedIcon = processedIcon,
                        isHidden = hiddenPackages.contains(app.packageName)
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
     * 移動或新增 App。
     * @param fromId 如果是移動，則是 uniqueId；如果是從 Library 新增，則是 packageName
     * @param targetId 如果是拖到另一個 App 上（交換/插入）
     * @param targetPageIndex 如果是拖到空白處或新分頁（直接加入該頁末尾）
     * @param isFromLibrary 是否是從 App Library 拖過來的（允許重複）
     */
    fun moveApp(fromId: String, targetId: String? = null, targetPageIndex: Int? = null, isFromLibrary: Boolean = false) {
        val currentPages = _pages.value.map { it.toMutableList() }.toMutableList()
        
        var movingItem: AppModel? = null
        
        if (!isFromLibrary) {
            // 移動模式：精確匹配 uniqueId
            outer@for (page in currentPages) {
                val index = page.indexOfFirst { it.uniqueId == fromId }
                if (index != -1) {
                    movingItem = page.removeAt(index)
                    break@outer
                }
            }
        }
        
        // 獲取要放置的對象
        val item = if (isFromLibrary) {
            // 新增模式：從總表複製
            val baseApp = _allApps.value.find { it.packageName == fromId }
            baseApp?.copy(uniqueId = "${fromId}_${System.currentTimeMillis()}")
        } else {
            movingItem
        } ?: return

        // 2. 判定落點
        if (targetId != null) {
            // 交換或插入到目標 App 的位置 (使用 uniqueId)
            var found = false
            currentPages.forEachIndexed { pIdx, page ->
                val iIdx = page.indexOfFirst { it.uniqueId == targetId }
                if (iIdx != -1) {
                    page.add(iIdx, item)
                    found = true
                }
            }
            if (!found) currentPages[0].add(item)
        } else if (targetPageIndex != null) {
            // 直接加入到特定分頁的末尾
            // 如果 targetPageIndex 超過現有範圍，補齊空白頁
            while (currentPages.size <= targetPageIndex) {
                currentPages.add(mutableListOf())
            }
            currentPages[targetPageIndex].add(item)
        }

        // 3. 清理：除了第一頁外，移除所有空分頁
        val finalPages = currentPages.filterIndexed { index, list -> 
            index == 0 || list.isNotEmpty() 
        }
        _pages.value = finalPages
    }

    fun updateDockApp(slotIndex: Int, packageName: String) {
        val current = _dockPackageNames.value.toMutableList()
        if (slotIndex in current.indices) {
            current[slotIndex] = packageName
            _dockPackageNames.value = current
        }
    }
}