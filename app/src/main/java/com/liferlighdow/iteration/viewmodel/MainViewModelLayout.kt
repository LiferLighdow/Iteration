package com.liferlighdow.iteration.viewmodel

import android.app.Application
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.data.AppModel
import com.liferlighdow.iteration.data.WidgetType

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

fun MainViewModel.addAppToHome(uniqueId: String) {
    val baseApp = _allApps.value.find { it.uniqueId == uniqueId } ?: return
    val newItem = baseApp.copy(uniqueId = "${uniqueId}@${System.currentTimeMillis()}")
    
    val currentPages = _pages.value.map { it.toMutableList() }.toMutableList()
    if (currentPages.isEmpty()) {
        currentPages.add(mutableListOf(newItem))
    } else {
        currentPages.last().add(newItem)
    }
    reorganizeAllPages(currentPages)
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
                is WidgetType.Stack -> 4
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
    saveLayout()
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
    outer@for (page in currentPages) {
        val idx = page.indexOfFirst { it.uniqueId == folderId }
        if (idx != -1) {
            val folder = page.removeAt(idx)
            if (keepIcons) {
                page.addAll(idx, folder.folderItems)
            }
            break@outer
        }
    }
    reorganizeAllPages(currentPages)
}

fun MainViewModel.updateFolderApps(folderId: String, ids: List<String>) {
    val currentPages = _pages.value.map { page ->
        page.map { item ->
            if (item.uniqueId == folderId) {
                // 1. 建立當前所有可用 App 的對照表
                val allAppsMap = _allApps.value.associateBy { it.uniqueId }
                // 建立目前資料夾內現有應用的對照表
                val existingMap = item.folderItems.associateBy { it.uniqueId }
                
                // 2. 根據傳入的 ID 列表構建新的內容
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
    saveLayout()
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

fun MainViewModel.removeAppFromFolder(folderId: String, appUniqueId: String) {
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
                        page[i] = updatedItems[0]
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
