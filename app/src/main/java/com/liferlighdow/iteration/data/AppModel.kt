package com.liferlighdow.iteration.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Transient

@Serializable
data class AppModel(
    @SerialName("label") val label: String,
    @SerialName("pkg") val packageName: String = "",
    @SerialName("isHidden") val isHidden: Boolean = false,
    @SerialName("category") val category: Int = -1,
    @SerialName("displayCategory") val displayCategory: String = "Other",
    @SerialName("type") val type: String = "app", // "app", "folder", "widget"
    @SerialName("children") val folderItems: List<AppModel> = emptyList(),
    @SerialName("widget") val widget: WidgetModel? = null,
    @SerialName("userId") val userId: Long = 0,
    @SerialName("id") val uniqueId: String = ""
) {
    @Transient
    val isFolder: Boolean = type == "folder"
    
    @Transient
    val isWidget: Boolean = type == "widget" || widget != null

    /**
     * 相容性邏輯：如果 uniqueId 為空，根據內容產生預設 ID
     */
    val effectiveId: String get() = uniqueId.ifBlank {
        when {
            widget != null -> "widget_${widget.id}"
            else -> packageName + (if (userId > 0) "@$userId" else "")
        }
    }
}
