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
    isFolderMerge: Boolean = false
) {
    // 使用備份作為基準，如果沒有備份則使用當前狀態
    val basePages = dragBackupPages ?: _pages.value
    val currentPages = basePages.map { it.toMutableList() }.toMutableList()
    dragBackupPages = null // 清除備份

    // 1. 自由網格模式 (Free Grid) 處理邏輯
    if (_isFreeGridEnabled.value) {
        val targetPageIdx = targetPageIndex.coerceIn(0, (currentPages.size - 1).coerceAtLeast(0))
        if (currentPages.isEmpty()) currentPages.add(mutableListOf())
        val page = currentPages[targetPageIdx]

        val columns = 4
        val rows = if (_desktopRows.value > 0) _desktopRows.value else (pageSize / 4).coerceAtLeast(6)
        val totalSlots = columns * rows
        while (page.size < totalSlots) {
            page.add(AppModel(label = "", packageName = "", uniqueId = "empty_slot_${System.currentTimeMillis()}_${page.size}"))
        }

        if (isFromLibrary) {
            val baseApp = _allApps.value.find { it.uniqueId == fromId } ?: return
            if (baseApp.isPrivate) return
            val item = baseApp.copy(uniqueId = "${fromId}@${System.currentTimeMillis()}")
            val dropIdx = targetSlotIndex?.coerceIn(0, page.size - 1) ?: page.indexOfFirst { it.packageName.isEmpty() && !it.isFolder && !it.isWidget }
            if (dropIdx != -1 && dropIdx < page.size) {
                page[dropIdx] = item
            } else {
                page.add(item)
            }
        } else {
            var fromPageIdx = -1
            var fromSlotIdx = -1
            var movingItem: AppModel? = null

            outer@ for (pIdx in currentPages.indices) {
                val p = currentPages[pIdx]
                val topIdx = p.indexOfFirst { it.uniqueId == fromId }
                if (topIdx != -1) {
                    fromPageIdx = pIdx
                    fromSlotIdx = topIdx
                    movingItem = p[topIdx]
                    break@outer
                }
                for (i in p.indices) {
                    val folder = p[i]
                    if (folder.isFolder) {
                        val fIdx = folder.folderItems.indexOfFirst { it.uniqueId == fromId }
                        if (fIdx != -1) {
                            val mutableFolderItems = folder.folderItems.toMutableList()
                            movingItem = mutableFolderItems.removeAt(fIdx)
                            if (mutableFolderItems.isEmpty()) {
                                p[i] = AppModel(label = "", packageName = "", uniqueId = "empty_slot_${System.currentTimeMillis()}_$i")
                            } else if (mutableFolderItems.size == 1) {
                                p[i] = mutableFolderItems[0]
                            } else {
                                p[i] = folder.copy(folderItems = mutableFolderItems)
                            }
                            break@outer
                        }
                    }
                }
            }

            val item = movingItem ?: return
            val dropSlotIdx = targetSlotIndex?.coerceIn(0, page.size - 1)
                ?: if (fromPageIdx == targetPageIdx && fromSlotIdx != -1) {
                    fromSlotIdx
                } else {
                    val emptyIdx = page.indexOfFirst { it.packageName.isEmpty() && !it.isFolder && !it.isWidget }
                    if (emptyIdx != -1) emptyIdx else 0
                }

            if (isFolderMerge) {
                val targetApp = page[dropSlotIdx]
                if (targetApp.packageName.isNotEmpty() || targetApp.isFolder) {
                    if (targetApp.isFolder) {
                        page[dropSlotIdx] = targetApp.copy(folderItems = targetApp.folderItems + item)
                    } else if (!targetApp.isWidget && !targetApp.isPWA) {
                        val defaultFolderName = getApplication<Application>().getString(R.string.folder_default_name)
                        val newFolder = AppModel(
                            label = defaultFolderName,
                            isFolder = true,
                            folderItems = listOf(targetApp, item),
                            uniqueId = "folder_${System.currentTimeMillis()}"
                        )
                        page[dropSlotIdx] = newFolder
                    }
                    if (fromPageIdx != -1 && fromSlotIdx != -1) {
                        currentPages[fromPageIdx][fromSlotIdx] = AppModel(label = "", packageName = "", uniqueId = "empty_slot_${System.currentTimeMillis()}_$fromSlotIdx")
                    }
                }
            } else {
                if (fromPageIdx != -1 && fromSlotIdx != -1) {
                    val targetApp = page[dropSlotIdx]
                    currentPages[fromPageIdx][fromSlotIdx] = targetApp
                    page[dropSlotIdx] = item
                } else {
                    page[dropSlotIdx] = item
                }
            }
        }

        _pages.value = currentPages
        saveLayout()
        return
    }

    // 2. 自動補位模式 (Auto-Refill Mode) 處理邏輯
    val cleanedPages = currentPages.map { page ->
        page.filter { it.packageName.isNotEmpty() || it.isFolder || it.isWidget || it.isShortcut }.toMutableList()
    }.toMutableList()

    var movingItem: AppModel? = null

    if (isFromLibrary) {
        val baseApp = _allApps.value.find { it.uniqueId == fromId }
        if (baseApp?.isPrivate == true) return
        movingItem = baseApp?.copy(uniqueId = "${fromId}@${System.currentTimeMillis()}")
    } else {
        outer@ for (pIdx in cleanedPages.indices) {
            val page = cleanedPages[pIdx]
            val topIdx = page.indexOfFirst { it.uniqueId == fromId }
            if (topIdx != -1) {
                movingItem = page.removeAt(topIdx)
                break@outer
            }
            for (i in page.indices) {
                val item = page[i]
                if (item.isFolder) {
                    val fIdx = item.folderItems.indexOfFirst { it.uniqueId == fromId }
                    if (fIdx != -1) {
                        val mutableFolderItems = item.folderItems.toMutableList()
                        movingItem = mutableFolderItems.removeAt(fIdx)
                        if (mutableFolderItems.isEmpty()) {
                            page.removeAt(i)
                        } else if (mutableFolderItems.size == 1) {
                            page[i] = mutableFolderItems[0]
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

    var success = false

    if (isFolderMerge && targetSlotIndex != null && targetPageIndex in cleanedPages.indices) {
        val page = cleanedPages[targetPageIndex]
        if (targetSlotIndex in page.indices) {
            val targetApp = page[targetSlotIndex]
            if (targetApp.isFolder) {
                page[targetSlotIndex] = targetApp.copy(folderItems = targetApp.folderItems + item)
                success = true
            } else if (!targetApp.isWidget && !targetApp.isPWA) {
                val defaultFolderName = getApplication<Application>().getString(R.string.folder_default_name)
                val newFolder = AppModel(
                    label = defaultFolderName,
                    isFolder = true,
                    folderItems = listOf(targetApp, item),
                    uniqueId = "folder_${System.currentTimeMillis()}"
                )
                page[targetSlotIndex] = newFolder
                success = true
            }
        }
    }

    if (!success && targetSlotIndex != null && targetPageIndex in cleanedPages.indices) {
        val page = cleanedPages[targetPageIndex]
        page.add(targetSlotIndex.coerceIn(0, page.size), item)
        success = true
    }

    if (!success && targetId != null) {
        for (page in cleanedPages) {
            val tIdx = page.indexOfFirst { it.uniqueId == targetId }
            if (tIdx != -1) {
                page.add(tIdx, item)
                success = true
                break
            }
        }
    }

    if (!success) insertAtPage(cleanedPages, targetPageIndex, item)

    reorganizeAllPages(cleanedPages)
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

fun MainViewModel.getWidgetDimensions(type: WidgetType): Pair<Int, Int> {
    return when (type) {
        is WidgetType.Battery -> 2 to 2
        is WidgetType.Clock -> 2 to 2
        is WidgetType.Calendar -> if (type.isWide) 4 to 2 else 2 to 2
        is WidgetType.Photo -> if (type.isWide) 4 to 2 else 2 to 2
        is WidgetType.Music -> if (type.isWide) 4 to 2 else 2 to 2
        is WidgetType.Note -> if (type.isWide) 4 to 2 else 2 to 2
        is WidgetType.Weather -> if (type.isWide) 4 to 2 else 2 to 2
        is WidgetType.ToDoList -> if (type.isWide) 4 to 2 else 2 to 2
        is WidgetType.Stack -> if (type.isWide) 4 to 2 else 2 to 2
        is WidgetType.Countdown -> 2 to 2
        is WidgetType.RSS -> 4 to 2
        is WidgetType.InfoHub -> 4 to 2
        is WidgetType.InfoHub2 -> 4 to 2
        is WidgetType.Custom -> if (type.size == "4x2") 4 to 2 else 2 to 2
    }
}

fun MainViewModel.canWidgetFitOnPage(
    existingItems: List<AppModel>,
    widgetType: WidgetType,
    columns: Int = 4,
    rows: Int = (pageSize / 4).coerceAtLeast(5)
): Boolean {
    val grid = Array(rows) { BooleanArray(columns) }

    // 將既有 App 與 Widget 填入模擬網格
    for (item in existingItems) {
        val (w, h) = if (item.isWidget && item.widget != null) {
            getWidgetDimensions(item.widget.type)
        } else {
            1 to 1
        }
        
        var placed = false
        for (r in 0..(rows - h)) {
            for (c in 0..(columns - w)) {
                var canPlace = true
                for (dr in 0 until h) {
                    for (dc in 0 until w) {
                        if (grid[r + dr][c + dc]) {
                            canPlace = false
                            break
                        }
                    }
                    if (!canPlace) break
                }
                if (canPlace) {
                    for (dr in 0 until h) {
                        for (dc in 0 until w) {
                            grid[r + dr][c + dc] = true
                        }
                    }
                    placed = true
                    break
                }
            }
            if (placed) break
        }
    }

    // 檢查新 Widget 的形狀 (寬 w, 高 h) 能否放入剩餘空位
    val (reqW, reqH) = getWidgetDimensions(widgetType)
    if (reqH > rows || reqW > columns) return false

    for (r in 0..(rows - reqH)) {
        for (c in 0..(columns - reqW)) {
            var fit = true
            for (dr in 0 until reqH) {
                for (dc in 0 until reqW) {
                    if (grid[r + dr][c + dc]) {
                        fit = false
                        break
                    }
                }
                if (!fit) break
            }
            if (fit) return true
        }
    }

    return false
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

fun MainViewModel.cleanupEmptyPages(exceptPageIndex: Int) {
    val currentPages = _pages.value.map { it.toMutableList() }.toMutableList()
    var changed = false
    
    // 從後往前遍歷，避免索引錯亂
    for (i in currentPages.indices.reversed()) {
        // 如果分頁是空的，且不是使用者正停留的分頁，且總頁數大於 1
        if (currentPages[i].isEmpty() && i != exceptPageIndex && currentPages.size > 1) {
            currentPages.removeAt(i)
            changed = true
        }
    }
    
    if (changed) {
        _pages.value = currentPages
        saveLayout()
    }
}

fun MainViewModel.reorganizeAllPages(pages: MutableList<MutableList<AppModel>>) {
    if (_isFreeGridEnabled.value) {
        val columns = 4
        val rows = if (_desktopRows.value > 0) _desktopRows.value else (pageSize / 4).coerceAtLeast(6)
        val totalSlots = columns * rows
        val paddedPages = pages.map { page ->
            val mutablePage = page.toMutableList()
            while (mutablePage.size < totalSlots) {
                mutablePage.add(AppModel(label = "", packageName = "", uniqueId = "empty_slot_${System.currentTimeMillis()}_${mutablePage.size}"))
            }
            mutablePage
        }.toMutableList()
        _pages.value = paddedPages
        saveLayout()
        return
    }
    val result = mutableListOf<MutableList<AppModel>>()
    var overflowItems = mutableListOf<AppModel>()
    val columns = 4
    val rows = (pageSize / 4).coerceAtLeast(5)

    for (page in pages) {
        val combinedItems = (overflowItems + page).toMutableList()
        overflowItems = mutableListOf()
        
        val pageItems = mutableListOf<AppModel>()
        
        for (item in combinedItems) {
            val fit = if (item.isWidget && item.widget != null) {
                canWidgetFitOnPage(pageItems, item.widget.type, columns, rows)
            } else {
                canWidgetFitOnPage(pageItems, WidgetType.Battery, columns, rows) // 1x1 app item
            }
            if (fit) {
                pageItems.add(item)
            } else {
                overflowItems.add(item)
            }
        }
        result.add(pageItems)
    }

    while (overflowItems.isNotEmpty()) {
        val pageItems = mutableListOf<AppModel>()
        val iterator = overflowItems.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            val fit = if (item.isWidget && item.widget != null) {
                canWidgetFitOnPage(pageItems, item.widget.type, columns, rows)
            } else {
                canWidgetFitOnPage(pageItems, WidgetType.Battery, columns, rows)
            }
            if (pageItems.isEmpty() || fit) {
                pageItems.add(item)
                iterator.remove()
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
                val existingMap = item.folderItems.associateBy { folderItem ->
                    val baseApp = _allApps.value.find { it.uniqueId == folderItem.uniqueId }
                        ?: _allApps.value.find { it.packageName == folderItem.packageName && it.userId == folderItem.userId }
                    baseApp?.uniqueId ?: folderItem.uniqueId
                }
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
            val existingMap = item.folderItems.associateBy { folderItem ->
                val baseApp = _allApps.value.find { it.uniqueId == folderItem.uniqueId }
                    ?: _allApps.value.find { it.packageName == folderItem.packageName && it.userId == folderItem.userId }
                baseApp?.uniqueId ?: folderItem.uniqueId
            }
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
            if (_isFreeGridEnabled.value) {
                page[index] = AppModel(label = "", packageName = "", uniqueId = "empty_slot_${System.currentTimeMillis()}_$index")
            } else {
                page.removeAt(index)
            }
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
