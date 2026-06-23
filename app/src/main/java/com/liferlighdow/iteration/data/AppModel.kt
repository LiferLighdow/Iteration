package com.liferlighdow.iteration.data

data class AppModel(
    val label: String,
    val packageName: String = "",
    val shortcutId: String? = null, // 新增：用於支援 PWA / 網頁捷徑
    val isHidden: Boolean = false,
    val category: Int = -1, // ApplicationInfo.category
    val displayCategory: String = "Other",
    val uniqueId: String = if (shortcutId != null) "$packageName/shortcut/$shortcutId" else packageName,
    val isFolder: Boolean = false,
    val folderItems: List<AppModel> = emptyList(),
    val widget: WidgetModel? = null // 新增：支援小工具
) {
    val isWidget: Boolean get() = widget != null
    val isShortcut: Boolean get() = shortcutId != null
}