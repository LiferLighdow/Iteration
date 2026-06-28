package com.liferlighdow.iteration.data

data class AppModel(
    val label: String,
    val packageName: String = "",
    val shortcutId: String? = null,
    val isHidden: Boolean = false,
    val category: Int = -1,
    val displayCategory: String = "Other",
    val isFolder: Boolean = false,
    val folderItems: List<AppModel> = emptyList(),
    val widget: WidgetModel? = null,
    val intentUri: String? = null,
    val uniqueId: String = when {
        widget != null -> "widget_${widget.id}"
        shortcutId != null -> "$packageName/shortcut/$shortcutId"
        intentUri != null -> "intent_shortcut_${intentUri.hashCode()}"
        else -> packageName
    }
) {
    val isWidget: Boolean get() = widget != null
    val isShortcut: Boolean get() = shortcutId != null || intentUri != null
}
