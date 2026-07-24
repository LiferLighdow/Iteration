package com.liferlighdow.iteration.viewmodel

import android.app.Application
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.data.AppModel
import com.liferlighdow.iteration.data.WidgetType
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * MainViewModel 的擴充檔案，專門處理桌面佈局、拖放與資料夾邏輯
 */

fun MainViewModel.prepareForDrag() {
    dragBackupPages = _pages.value
}

fun MainViewModel.handleAppDrop(
    fromId: String,
    targetId: String?,
    targetPageIndex: Int,
    targetSlotIndex: Int? = null,
    isFromLibrary: Boolean,
    dropType: MainViewModel.DropType
) {
    // 使用備份作為基準，如果沒有備份則使用當前狀態
    val basePages = dragBackupPages ?: _pages.value
    val currentPages = basePages.map { it.toMutableList() }.toMutableList()
    dragBackupPages = null // 清除備份
    
    var movingItem: AppModel? = null

    // 1. 取得移動物件 (包含從資料夾內抓取)
    if (isFromLibrary) {
        val baseApp = _allApps.value.find { it.uniqueId == fromId }
        if (baseApp?.isPrivate == true) return // 禁止從庫中將私密應用拖放至桌面
        movingItem = baseApp?.copy(uniqueId = "${fromId}@${System.currentTimeMillis()}")
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

    // 2. 處理落點
    when (dropType) {
        MainViewModel.DropType.FOLDER -> {
            if (targetId != null) {
                var success = false
                for (page in currentPages) {
                    val tIdx = page.indexOfFirst { it.uniqueId == targetId }
                    if (tIdx != -1) {
                        val targetItem = page[tIdx]
                        if (targetItem.isFolder) {
                            page[tIdx] = targetItem.copy(
                                folderItems = (targetItem.folderItems + item).sortedBy { it.label.lowercase() }
                            )
                        } else {
                            page[tIdx] = AppModel(
                                label = getApplication<Application>().getString(R.string.folder_default_name),
                                isFolder = true,
                                folderItems = listOf(targetItem, item).sortedBy { it.label.lowercase() },
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
        MainViewModel.DropType.REORDER -> {
            var success = false
            
            // 優先嘗試使用目標插槽索引進行插入，這比 ID 查找更精確 (尤其是回到原位時)
            if (targetSlotIndex != null && targetPageIndex in currentPages.indices) {
                val page = currentPages[targetPageIndex]
                page.add(targetSlotIndex.coerceIn(0, page.size), item)
                success = true
            } 
            
            // 如果插槽插入失敗，則嘗試按 ID 查找 (作為保險)
            if (!success && targetId != null) {
                for (page in currentPages) {
                    val tIdx = page.indexOfFirst { it.uniqueId == targetId }
                    if (tIdx != -1) {
                        page.add(tIdx, item)
                        success = true
                        break
                    }
                }
            }
            
            if (!success) insertAtPage(currentPages, targetPageIndex, item)
        }
    }

    reorganizeAllPages(currentPages)
}

fun MainViewModel.addAppToHome(uniqueId: String) {
    val baseApp = _allApps.value.find { it.uniqueId == uniqueId } ?: return
    if (baseApp.isPrivate) return // 禁止將私密空間應用新增至主畫面

    val newItem = baseApp.copy(uniqueId = "${uniqueId}@${System.currentTimeMillis()}")
    
    val currentPages = _pages.value.map { it.toMutableList() }.toMutableList()
    if (currentPages.isEmpty()) {
        currentPages.add(mutableListOf(newItem))
    } else {
        currentPages.last().add(newItem)
    }
    reorganizeAllPages(currentPages)
}

fun MainViewModel.addShortcutToHome(packageName: String, shortcutId: String, label: String) {
    val newItem = AppModel(
        label = label,
        packageName = packageName,
        shortcutId = shortcutId,
        uniqueId = "shortcut_${packageName}_${shortcutId}"
    )
    
    val currentPages = _pages.value.map { it.toMutableList() }.toMutableList()
    if (currentPages.isEmpty()) {
        currentPages.add(mutableListOf(newItem))
    } else {
        // 找到最後一個有空間的頁面或新增一頁
        currentPages.last().add(newItem)
    }
    _pages.value = currentPages
    saveLayout()
}

fun MainViewModel.insertAtPage(pages: MutableList<MutableList<AppModel>>, index: Int, item: AppModel) {
    val safeIndex = index.coerceIn(0, pages.size)
    if (safeIndex < pages.size) {
        pages[safeIndex].add(item)
    } else {
        pages.add(mutableListOf(item))
    }
}

fun MainViewModel.calculateUsedSlots(items: List<AppModel>): Int {
    return items.sumOf { item ->
        if (item.isWidget) {
            when (val type = item.widget?.type) {
                is WidgetType.Battery -> 4
                is WidgetType.Clock -> 4
                is WidgetType.Calendar -> if (type.isWide) 8 else 4
                is WidgetType.Photo -> if (type.isWide) 8 else 4
                is WidgetType.Music -> if (type.isWide) 8 else 4
                is WidgetType.Note -> if (type.isWide) 8 else 4
                is WidgetType.Weather -> if (type.isWide) 8 else 4
                is WidgetType.ToDoList -> if (type.isWide) 8 else 4
                is WidgetType.Stack -> 4
                is WidgetType.RSS -> 8
                is WidgetType.InfoHub -> 8
                is WidgetType.InfoHub2 -> 8
                is WidgetType.Custom -> if (type.size == "4x2") 8 else 4
                else -> 1
            }
        } else 1
    }
}

fun MainViewModel.addEmptyPage() {
    val currentPages = _pages.value.map { it.toMutableList() }.toMutableList()
    currentPages.add(mutableListOf())
    _pages.value = currentPages
    saveLayout()
}

fun MainViewModel.deletePage(index: Int) {
    val currentPages = _pages.value.map { it.toMutableList() }.toMutableList()
    if (index in currentPages.indices) {
        currentPages.removeAt(index)
        reorganizeAllPages(currentPages)
    }
}

fun MainViewModel.reorganizeAllPages(pages: MutableList<MutableList<AppModel>>) {
    val result = mutableListOf<MutableList<AppModel>>()
    var overflowItems = mutableListOf<AppModel>()

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
                pageItems.add(item)
                iterator.remove()
                break
            } else {
                break
            }
        }
        result.add(pageItems)
    }

    if (result.isEmpty()) result.add(mutableListOf())

    _pages.value = result
    saveLayout()
}

fun MainViewModel.updateFolderName(folderId: String, newName: String) {
    val currentPages = _pages.value.map { page ->
        page.map { item ->
            if (item.uniqueId == folderId) item.copy(label = newName) else item
        }
    }
    _pages.value = currentPages
    
    val currentDock = _dockItems.value.map { item ->
        if (item.uniqueId == folderId) item.copy(label = newName) else item
    }
    _dockItems.value = currentDock
    
    saveLayout()
    saveDock()
}

fun MainViewModel.createFolder(pageIndex: Int, folderName: String) {
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

fun MainViewModel.deleteFolder(folderId: String, keepIcons: Boolean = true) {
    val currentPages = _pages.value.map { it.toMutableList() }.toMutableList()
    var foundInPages = false
    outer@for (page in currentPages) {
        val idx = page.indexOfFirst { it.uniqueId == folderId }
        if (idx != -1) {
            val folder = page.removeAt(idx)
            if (keepIcons) {
                page.addAll(idx, folder.folderItems)
            }
            foundInPages = true
            break@outer
        }
    }
    if (foundInPages) {
        reorganizeAllPages(currentPages)
    }

    val currentDock = _dockItems.value.toMutableList()
    val dockIdx = currentDock.indexOfFirst { it.uniqueId == folderId }
    if (dockIdx != -1) {
        val folder = currentDock[dockIdx]
        if (keepIcons && folder.folderItems.isNotEmpty()) {
            currentDock[dockIdx] = folder.folderItems[0] // Dock 只能放一個，如果有多個也沒辦法，取第一個
        } else {
            currentDock[dockIdx] = AppModel(label = "", packageName = "", uniqueId = "empty_dock_${System.currentTimeMillis()}")
        }
        _dockItems.value = currentDock
        saveDock()
    }
}

fun MainViewModel.updateFolderApps(folderId: String, ids: List<String>) {
    val allAppsMap = _allApps.value.associateBy { it.uniqueId }
    
    val currentPages = _pages.value.map { page ->
        page.map { item ->
            if (item.uniqueId == folderId) {
                val existingMap = item.folderItems.associateBy { it.uniqueId }
                val newFolderItems = ids.mapNotNull { id ->
                    existingMap[id] ?: allAppsMap[id]?.copy(
                        uniqueId = "${id}@${System.currentTimeMillis()}"
                    )
                }.sortedBy { it.label.lowercase() }
                item.copy(folderItems = newFolderItems)
            } else item
        }
    }
    _pages.value = currentPages.map { it.toMutableList() }

    val currentDock = _dockItems.value.map { item ->
        if (item.uniqueId == folderId) {
            val existingMap = item.folderItems.associateBy { it.uniqueId }
            val newFolderItems = ids.mapNotNull { id ->
                existingMap[id] ?: allAppsMap[id]?.copy(
                    uniqueId = "${id}@${System.currentTimeMillis()}"
                )
            }.sortedBy { it.label.lowercase() }
            item.copy(folderItems = newFolderItems)
        } else item
    }
    _dockItems.value = currentDock

    saveLayout()
    saveDock()
}

fun MainViewModel.removeAppFromHome(uniqueId: String) {
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

fun MainViewModel.removeAppFromHomeWithAnimation(uniqueId: String) {
    viewModelScope.launch {
        _removingItemIds.value += uniqueId
        delay(500)
        removeAppFromHome(uniqueId)
        _removingItemIds.value -= uniqueId
    }
}

fun MainViewModel.removeAppFromFolderWithAnimation(folderId: String, appUniqueId: String) {
    viewModelScope.launch {
        _removingItemIds.value += appUniqueId
        delay(500)
        removeAppFromFolder(folderId, appUniqueId)
        _removingItemIds.value -= appUniqueId
    }
}

fun MainViewModel.removeAppFromFolder(folderId: String, appUniqueId: String) {
    val currentPages = _pages.value.map { it.toMutableList() }.toMutableList()
    var foundInPages = false
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
                        page[i] = updatedItems[0]
                    } else {
                        page[i] = item.copy(folderItems = updatedItems)
                    }
                    foundInPages = true
                    break@outer
                }
            }
        }
    }
    if (foundInPages) reorganizeAllPages(currentPages)

    val currentDock = _dockItems.value.toMutableList()
    for (i in currentDock.indices) {
        val item = currentDock[i]
        if (item.isFolder && item.uniqueId == folderId) {
            val updatedItems = item.folderItems.toMutableList()
            val removedIdx = updatedItems.indexOfFirst { it.uniqueId == appUniqueId }
            if (removedIdx != -1) {
                updatedItems.removeAt(removedIdx)
                if (updatedItems.isEmpty()) {
                    currentDock[i] = AppModel(label = "", packageName = "", uniqueId = "empty_dock_${System.currentTimeMillis()}")
                } else if (updatedItems.size == 1) {
                    currentDock[i] = updatedItems[0]
                } else {
                    currentDock[i] = item.copy(folderItems = updatedItems)
                }
                _dockItems.value = currentDock
                saveDock()
                break
            }
        }
    }
}
