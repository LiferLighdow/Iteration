package com.liferlighdow.iteration.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class AppModel(
    @SerialName("l") val label: String,
    @SerialName("p") val packageName: String = "",
    @SerialName("h") val isHidden: Boolean = false,
    @SerialName("c") val category: Int = -1,
    @SerialName("dc") val displayCategory: String = "Other",
    @SerialName("f") val isFolder: Boolean = false,
    @SerialName("ch") val folderItems: List<AppModel> = emptyList(),
    @SerialName("w") val widget: WidgetModel? = null,
    @SerialName("u") val userId: Long = 0,
    @SerialName("id") val uniqueId: String = ""
) {
    val isWidget: Boolean get() = widget != null
    
    /**
     * 根據內容產生有效的 ID (Fallback 邏輯)
     */
    val effectiveId: String get() = uniqueId.ifBlank {
        when {
            widget != null -> "widget_${widget.id}"
            else -> packageName + (if (userId > 0) "@$userId" else "")
        }
    }
}
