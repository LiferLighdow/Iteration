package com.liferlighdow.iteration

import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap

data class AppModel(
    val label: String,
    val packageName: String,
    val icon: Drawable,
    val processedIcon: ImageBitmap? = null, // 預處理後的滿版圖示
    val isHidden: Boolean = false,
    val category: Int = -1, // ApplicationInfo.category
    val uniqueId: String = packageName // 用於區分主畫面上的多個實例
)