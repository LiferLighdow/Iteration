package com.liferlighdow.iteration.data

data class AppModel(
    val label: String,
    val packageName: String = "",
    val isHidden: Boolean = false,
    val category: Int = -1,
    val displayCategory: String = "Other",
    val isFolder: Boolean = false,
    val folderItems: List<AppModel> = emptyList(),
    val widget: WidgetModel? = null,
    val userId: Long = 0,
    val uniqueId: String = when {
        widget != null -> "widget_${widget.id}"
        else -> {
            // 注意：我們在 AppRepository 中會傳入 activityName 作為 uniqueId 的一部分
            // 格式預期為: packageName/activityName@userId
            // 這裡只是預設的 fallback 邏輯
            packageName + (if (userId > 0) "@$userId" else "")
        }
    }
) {
    val isWidget: Boolean get() = widget != null
}
