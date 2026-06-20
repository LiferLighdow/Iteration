package com.liferlighdow.iteration

import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap

data class AppModel(
    val label: String,
    val packageName: String = "",
    val isHidden: Boolean = false,
    val category: Int = -1, // ApplicationInfo.category
    val displayCategory: String = "Other",
    val uniqueId: String = packageName, // 用於區分主畫面上的多個實例
    val isFolder: Boolean = false,
    val folderItems: List<AppModel> = emptyList(),
    val widget: WidgetModel? = null // 新增：支援小工具
) {
    val isWidget: Boolean get() = widget != null
}