package com.liferlighdow.iteration.data

import android.graphics.drawable.Drawable

data class AppShortcut(
    val id: String,
    val label: String,
    val icon: Drawable? = null,
    val packageName: String
)